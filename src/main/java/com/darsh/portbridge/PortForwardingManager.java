package com.darsh.portbridge;

import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PortForwardingManager {
    private static final Logger LOGGER = PortBridge.LOGGER;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "PortBridge-UPnP");
        t.setDaemon(true);
        return t;
    });

    private boolean isActive = false;
    private String lastError = null;
    private int externalPort = -1;
    private long leaseDuration;

    public void startPortForwarding() {
        if (!Config.ENABLE_PORT_FORWARDING.get()) {
            LOGGER.info("[PortBridge] Port forwarding disabled in config");
            return;
        }

        executor.submit(this::attemptPortForwarding);
    }

    private void attemptPortForwarding() {
        try {
            SimpleUPnP upnp = new SimpleUPnP();
            if (!upnp.isUPnPAvailable()) {
                lastError = "UPnP not available on this network";
                LOGGER.warn("[PortBridge] {}", lastError);
                return;
            }

            if (!upnp.isMappedTCP(Config.EXTERNAL_PORT.get())) {
                String localIP = getLocalIP();
                if (localIP == null) {
                    lastError = "Could not determine local IP address";
                    LOGGER.warn("[PortBridge] {}", lastError);
                    return;
                }

                boolean success = upnp.openPortTCP(Config.EXTERNAL_PORT.get(), Config.INTERNAL_PORT.get(), localIP, "Minecraft Server", Config.LEASE_DURATION.get());
                if (success) {
                    isActive = true;
                    externalPort = Config.EXTERNAL_PORT.get();
                    leaseDuration = Config.LEASE_DURATION.get();
                    LOGGER.info("[PortBridge] Port forwarding active");
                    LOGGER.info("[PortBridge] External port: {}", externalPort);

                    // Schedule refresh if lease duration is set
                    if (leaseDuration > 0) {
                        long refreshInterval = Math.max(leaseDuration / 2, Config.REFRESH_INTERVAL.get());
                        executor.scheduleAtFixedRate(this::refreshMapping, refreshInterval, refreshInterval, TimeUnit.SECONDS);
                    }
                } else {
                    lastError = "Failed to create port mapping";
                    LOGGER.warn("[PortBridge] {}", lastError);
                }
            } else {
                isActive = true;
                externalPort = Config.EXTERNAL_PORT.get();
                LOGGER.info("[PortBridge] Port already forwarded");
            }
        } catch (Exception e) {
            lastError = "Exception during port forwarding: " + e.getMessage();
            LOGGER.error("[PortBridge] {}", lastError, e);
        }
    }

    private String getLocalIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("[PortBridge] Error getting local IP", e);
        }
        return null;
    }

    private void refreshMapping() {
        try {
            SimpleUPnP upnp = new SimpleUPnP();
            if (isActive && upnp.isMappedTCP(externalPort)) {
                // Refresh by re-mapping
                String localIP = getLocalIP();
                if (localIP != null) {
                    upnp.openPortTCP(externalPort, Config.INTERNAL_PORT.get(), localIP, "Minecraft Server", (int) leaseDuration);
                    if (Config.DEBUG_LOGGING.get()) {
                        LOGGER.debug("[PortBridge] Refreshed port mapping");
                    }
                }
            } else {
                // Try to re-establish
                attemptPortForwarding();
            }
        } catch (Exception e) {
            LOGGER.error("[PortBridge] Error refreshing port mapping", e);
        }
    }

    public void stopPortForwarding() {
        executor.submit(() -> {
            try {
                SimpleUPnP upnp = new SimpleUPnP();
                if (isActive && upnp.isMappedTCP(externalPort)) {
                    upnp.closePortTCP(externalPort);
                    LOGGER.info("[PortBridge] Port forwarding removed");
                }
            } catch (Exception e) {
                LOGGER.error("[PortBridge] Error removing port forwarding", e);
            } finally {
                isActive = false;
                executor.shutdown();
            }
        });
    }

    public boolean isActive() {
        return isActive;
    }

    public int getExternalPort() {
        return externalPort;
    }

    public String getLastError() {
        return lastError;
    }

    public void retry() {
        lastError = null;
        executor.submit(this::attemptPortForwarding);
    }
}