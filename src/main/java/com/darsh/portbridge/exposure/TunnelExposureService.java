package com.darsh.portbridge.exposure;

import com.darsh.portbridge.Config;
import com.darsh.portbridge.tunnel.TunnelClient;
import com.darsh.portbridge.tunnel.TunnelSession;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TunnelExposureService extends ExposureService {
    private final ScheduledExecutorService executor;
    private TunnelClient tunnelClient;
    private TunnelSession session;
    private int reconnectAttempts;
    private long baseDelayMs;
    private long maxDelayMs;

    public TunnelExposureService() {
        super("TUNNEL");
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PortBridge-Tunnel");
            t.setDaemon(true);
            return t;
        });
        this.reconnectAttempts = 0;
        this.baseDelayMs = 5000;
        this.maxDelayMs = 120000;
    }

    @Override
    public boolean start(int internalPort, int externalPort) {
        executor.submit(() -> attemptTunnelConnection(internalPort));
        return true;
    }

    private void attemptTunnelConnection(int internalPort) {
        try {
            String relayHost = Config.TUNNEL_RELAY_HOST.get();
            int relayPort = Config.TUNNEL_RELAY_PORT.get();

            String serverUUID = UUID.randomUUID().toString();
            String worldName = "world";

            session = new TunnelSession(serverUUID, internalPort, worldName);
            tunnelClient = new TunnelClient(relayHost, relayPort, session);

            if (tunnelClient.connect()) {
                active = true;
                reconnectAttempts = 0;
                setPublicAddress(tunnelClient.getPublicAddress());
                LOGGER.info("[PortBridge] Tunnel connection established");
                LOGGER.info("[PortBridge] Public address: {}", publicAddress);

                // Start monitoring for disconnects
                executor.scheduleWithFixedDelay(this::monitorConnection, 10, 10, TimeUnit.SECONDS);
            } else {
                setError("Failed to connect to tunnel relay");
                scheduleReconnect(internalPort);
            }
        } catch (Exception e) {
            setError("Tunnel error: " + e.getMessage());
            LOGGER.error("[PortBridge] {}", lastError, e);
            scheduleReconnect(internalPort);
        }
    }

    private void monitorConnection(int internalPort) {
        if (!active) return;

        if (!tunnelClient.isConnected()) {
            LOGGER.warn("[PortBridge] Tunnel disconnected. Reason: {}", tunnelClient.getLastDisconnectReason());
            active = false;
            scheduleReconnect(internalPort);
        }
    }

    private void monitorConnection() {
        if (active && tunnelClient != null && !tunnelClient.isConnected()) {
            LOGGER.warn("[PortBridge] Tunnel disconnected. Reason: {}", tunnelClient.getLastDisconnectReason());
            active = false;
        }
    }

    private void scheduleReconnect(int internalPort) {
        reconnectAttempts++;
        long delay = Math.min(baseDelayMs * (long) Math.pow(2, reconnectAttempts - 1), maxDelayMs);
        LOGGER.info("[PortBridge] Scheduling tunnel reconnect in {} ms (attempt {})", delay, reconnectAttempts);
        executor.schedule(() -> attemptTunnelConnection(internalPort), delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        active = false;
        if (tunnelClient != null) {
            tunnelClient.close("Shutdown requested");
        }
    }

    public long getLatency() {
        if (tunnelClient != null) {
            return tunnelClient.getLatency();
        }
        return -1;
    }

    public long getBytesTransferred() {
        if (tunnelClient != null) {
            return tunnelClient.getBytesTransferred();
        }
        return 0;
    }

    public String getSessionId() {
        if (session != null) {
            return session.getSessionId();
        }
        return "N/A";
    }

    public String getStatus() {
        if (!active) {
            return "DISCONNECTED";
        }
        if (tunnelClient != null && tunnelClient.isConnected()) {
            return "CONNECTED";
        }
        return "RECONNECTING";
    }

    public void shutdown() {
        stop();
        if (tunnelClient != null) {
            tunnelClient.shutdown();
        }
        executor.shutdownNow();
    }
}
