package com.darsh.portbridge.tunnel;

import org.slf4j.Logger;
import com.darsh.portbridge.PortBridge;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class TunnelClient {
    private static final Logger LOGGER = PortBridge.LOGGER;
    private static final int KEEPALIVE_INTERVAL = 20000; // 20 seconds
    private static final int CONNECT_TIMEOUT = 5000; // 5 seconds
    private static final int READ_TIMEOUT = 10000; // 10 seconds

    private final String relayHost;
    private final int relayPort;
    private final TunnelSession session;
    private final ExecutorService executor;

    private Socket relaySocket;
    private InputStream relayIn;
    private OutputStream relayOut;
    private volatile boolean connected;
    private volatile boolean shouldRun;
    private long lastKeepalive;
    private long bytesTransferred;
    private String lastDisconnectReason;
    private Thread clientThread;

    public TunnelClient(String relayHost, int relayPort, TunnelSession session) {
        this.relayHost = relayHost;
        this.relayPort = relayPort;
        this.session = session;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "PortBridge-TunnelIO");
            t.setDaemon(true);
            return t;
        });
        this.connected = false;
        this.shouldRun = false;
        this.bytesTransferred = 0;
    }

    public boolean connect() {
        try {
            relaySocket = new Socket();
            relaySocket.setKeepAlive(true);
            relaySocket.setSoTimeout(READ_TIMEOUT);
            relaySocket.connect(new InetSocketAddress(relayHost, relayPort), CONNECT_TIMEOUT);

            relayIn = new BufferedInputStream(relaySocket.getInputStream());
            relayOut = new BufferedOutputStream(relaySocket.getOutputStream());

            shouldRun = true;
            connected = true;
            lastKeepalive = System.currentTimeMillis();

            // Register with relay
            if (!registerWithRelay()) {
                close("Registration failed");
                return false;
            }

            // Start keepalive and message loop
            executor.submit(this::messageLoop);
            executor.submit(this::keepaliveLoop);

            LOGGER.info("[PortBridge] Tunnel connected to relay: {}:{}", relayHost, relayPort);
            return true;
        } catch (Exception e) {
            lastDisconnectReason = e.getMessage();
            LOGGER.error("[PortBridge] Failed to connect to tunnel relay", e);
            close("Connection failed");
            return false;
        }
    }

    private boolean registerWithRelay() {
        try {
            String registrationMessage = String.format(
                "REGISTER|%s|%d|%s\n",
                session.getServerUUID(),
                session.getMinecraftPort(),
                session.getWorldName()
            );

            relayOut.write(registrationMessage.getBytes(StandardCharsets.UTF_8));
            relayOut.flush();

            // Read response
            BufferedReader reader = new BufferedReader(new InputStreamReader(relayIn, StandardCharsets.UTF_8));
            String response = reader.readLine();

            if (response != null && response.startsWith("REGISTERED|")) {
                String[] parts = response.split("\\|");
                if (parts.length >= 3) {
                    String publicHost = parts[1];
                    int publicPort = Integer.parseInt(parts[2]);
                    session.updateFromRelayResponse(publicHost, publicPort);
                    session.setActive(true);
                    LOGGER.info("[PortBridge] Tunnel registered. Public address: {}:{}", publicHost, publicPort);
                    return true;
                }
            }

            lastDisconnectReason = "Invalid registration response";
            return false;
        } catch (Exception e) {
            lastDisconnectReason = e.getMessage();
            LOGGER.error("[PortBridge] Registration error", e);
            return false;
        }
    }

    private void messageLoop() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(relayIn, StandardCharsets.UTF_8));
            String line;

            while (shouldRun && (line = reader.readLine()) != null) {
                session.recordHeartbeat();
                final String currentLine = line; // Make it effectively final for lambda

                if (currentLine.startsWith("HEARTBEAT")) {
                    // Acknowledge heartbeat
                    sendMessage("HEARTBEAT_ACK");
                } else if (currentLine.startsWith("CONNECTION|")) {
                    // Handle new connection forward
                    executor.submit(() -> handleConnection(currentLine));
                }
            }

            if (shouldRun) {
                close("Connection closed by relay");
            }
        } catch (IOException e) {
            if (shouldRun) {
                lastDisconnectReason = e.getMessage();
                close("I/O error in message loop");
            }
        }
    }

    private void handleConnection(String line) {
        try {
            String[] parts = line.split("\\|");
            if (parts.length < 2) return;

            String connId = parts[1];
            int localPort = session.getMinecraftPort();

            // Create connection to local Minecraft server
            Socket localSocket = new Socket("127.0.0.1", localPort);
            TunnelForwarder forwarder = new TunnelForwarder(connId, relaySocket, localSocket, this);
            forwarder.start();

        } catch (Exception e) {
            LOGGER.error("[PortBridge] Error handling tunnel connection", e);
        }
    }

    private void keepaliveLoop() {
        while (shouldRun) {
            try {
                long now = System.currentTimeMillis();
                if (now - lastKeepalive > KEEPALIVE_INTERVAL) {
                    sendMessage("KEEPALIVE");
                    lastKeepalive = now;
                    session.recordHeartbeat();
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private synchronized void sendMessage(String message) {
        try {
            if (relayOut != null && connected) {
                relayOut.write((message + "\n").getBytes(StandardCharsets.UTF_8));
                relayOut.flush();
            }
        } catch (IOException e) {
            LOGGER.debug("[PortBridge] Error sending message to relay", e);
            close("Send error");
        }
    }

    public void recordTransfer(int bytes) {
        this.bytesTransferred += bytes;
    }

    public void close(String reason) {
        lastDisconnectReason = reason;
        shouldRun = false;
        connected = false;

        try {
            if (relaySocket != null && !relaySocket.isClosed()) {
                relaySocket.close();
            }
        } catch (IOException e) {
            LOGGER.debug("[PortBridge] Error closing relay socket", e);
        }

        session.setActive(false);
        LOGGER.info("[PortBridge] Tunnel disconnected: {}", reason);
    }

    public boolean isConnected() {
        return connected && relaySocket != null && !relaySocket.isClosed();
    }

    public String getPublicAddress() {
        return session.getPublicAddress();
    }

    public long getBytesTransferred() {
        return bytesTransferred;
    }

    public String getLastDisconnectReason() {
        return lastDisconnectReason;
    }

    public long getLatency() {
        return session.getHeartbeatAge();
    }

    public String getSessionId() {
        return session.getSessionId();
    }

    public void shutdown() {
        close("Shutdown requested");
        executor.shutdownNow();
    }
}
