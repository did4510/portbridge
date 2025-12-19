package com.darsh.portbridge.exposure;

import com.darsh.portbridge.Config;
import com.darsh.portbridge.PublicIPResolver;
import com.darsh.portbridge.SimpleUPnP;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UPnPExposureService extends ExposureService {
    private final ScheduledExecutorService executor;
    private SimpleUPnP upnp;
    private PublicIPResolver ipResolver;
    private int currentExternalPort = -1;
    private long leaseDuration;
    private java.util.Map<Integer, Long> mappedPortsExpiry = new java.util.concurrent.ConcurrentHashMap<>();
    private String lastWanIp;

    public UPnPExposureService() {
        super("UPnP");
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PortBridge-UPnP");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public boolean start(int internalPort, int externalPort) {
        executor.submit(() -> attemptUPnP(internalPort, externalPort));
        return true;
    }

    private void attemptUPnP(int internalPort, int externalPort) {
        try {
            upnp = new SimpleUPnP();
            if (!upnp.isUPnPAvailable()) {
                setError("UPnP not available on this network");
                LOGGER.warn("[PortBridge] {}", lastError);
                return;
            }
            String localIP = getLocalIP();
            if (localIP == null) {
                setError("Could not determine local IP address");
                LOGGER.warn("[PortBridge] {}", lastError);
                return;
            }

            // Check allowed subnets
            String allowed = Config.ALLOWED_SUBNETS.get();
            if (allowed != null && !allowed.isBlank() && !isIPInAllowedSubnets(localIP, allowed)) {
                setError("Local IP " + localIP + " is not within allowed subnets: " + allowed);
                LOGGER.warn("[PortBridge] {}", lastError);
                return;
            }

            int leaseDuration = Config.LEASE_DURATION.get();

            // Build list of ports to map: main server port + additional ports from config
            class PortEntry { String name; int in; int out; String proto; boolean enabled; }

            java.util.List<PortEntry> portsToMap = new java.util.ArrayList<>();
            PortEntry main = new PortEntry();
            main.name = "minecraft";
            main.in = internalPort;
            main.out = externalPort > 0 ? externalPort : internalPort;
            main.proto = "TCP";
            main.enabled = true;
            portsToMap.add(main);

            String extra = Config.ADDITIONAL_PORTS.get();
            if (extra != null && !extra.isBlank()) {
                String[] entries = extra.split(",");
                for (String e : entries) {
                    try {
                        String[] parts = e.trim().split(":");
                        if (parts.length >= 5) {
                            PortEntry pe = new PortEntry();
                            pe.name = parts[0];
                            pe.in = Integer.parseInt(parts[1]);
                            pe.out = Integer.parseInt(parts[2]);
                            pe.proto = parts[3].toUpperCase();
                            pe.enabled = Boolean.parseBoolean(parts[4]);
                            if (pe.enabled) portsToMap.add(pe);
                        }
                    } catch (Exception ex) {
                        LOGGER.warn("[PortBridge] Invalid additionalPorts entry: {}", e);
                    }
                }
            }

            // Dry-run handling: simulate mapping without calling UPnP
            if (Config.DRY_RUN.get()) {
                ipResolver = new PublicIPResolver();
                ipResolver.getPublicIP().thenAccept(ip -> {
                    String publicIp = ip != null ? ip : "0.0.0.0";
                    // Simulate mapping for the first TCP port (minecraft)
                    PortEntry firstTcp = portsToMap.stream().filter(p -> "TCP".equalsIgnoreCase(p.proto)).findFirst().orElse(null);
                    if (firstTcp != null) {
                        setPublicAddress(publicIp + ":" + firstTcp.out);
                        active = true;
                        currentExternalPort = firstTcp.out;
                        this.leaseDuration = leaseDuration;
                        LOGGER.info("[PortBridge] DRY-RUN: Would create UPnP mappings: {}", portsToMap);
                    }
                });
                return;
            }

            // Attempt to create mappings
            boolean anySuccess = false;
            for (PortEntry pe : portsToMap) {
                boolean success = false;
                if ("TCP".equalsIgnoreCase(pe.proto)) {
                    success = upnp.openPortTCP(pe.out, pe.in, localIP, "PortBridge-Minecraft-" + pe.name, leaseDuration);
                } else {
                    // For now, treat UDP as not implemented in SimpleUPnP but attempt TCP call as placeholder
                    success = upnp.openPortTCP(pe.out, pe.in, localIP, "PortBridge-" + pe.name, leaseDuration);
                }

                if (success) {
                    anySuccess = true;
                    long expiry = leaseDuration <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + (leaseDuration * 1000L);
                    mappedPortsExpiry.put(pe.out, expiry);
                    if (Config.DEBUG_LOGGING.get()) {
                        LOGGER.debug("[PortBridge] Mapped {} {}->{} (proto={}) expiry={}ms", pe.name, pe.in, pe.out, pe.proto, expiry);
                    }
                    // Set public address from resolver for the first mapped port
                    if (currentExternalPort <= 0) {
                        currentExternalPort = pe.out;
                    }
                } else {
                    LOGGER.warn("[PortBridge] Failed to map port {} ({}->{})", pe.name, pe.in, pe.out);
                }
            }

            if (anySuccess) {
                ipResolver = new PublicIPResolver();
                ipResolver.getPublicIP().thenAccept(ip -> {
                    if (ip != null) {
                        lastWanIp = ip;
                        String address = ip + ":" + currentExternalPort;
                        setPublicAddress(address);
                        active = true;
                        this.leaseDuration = leaseDuration;

                        LOGGER.info("[PortBridge] UPnP port forwarding successful");
                        LOGGER.info("[PortBridge] Public address: {}", address);

                        // Schedule periodic refresh check
                        executor.scheduleAtFixedRate(this::refreshMapping, Math.max(60, Config.REFRESH_INTERVAL.get()), Math.max(60, Config.REFRESH_INTERVAL.get()), TimeUnit.SECONDS);
                    }
                });
            } else {
                setError("Failed to create any UPnP port mappings");
                LOGGER.warn("[PortBridge] {}", lastError);
            }
        } catch (Exception e) {
            setError("UPnP error: " + e.getMessage());
            LOGGER.error("[PortBridge] {}", lastError, e);
        }
    }

    private boolean isIPInAllowedSubnets(String ip, String allowedCsv) {
        try {
            String[] parts = allowedCsv.split(",");
            for (String p : parts) {
                p = p.trim();
                if (p.isEmpty()) continue;
                if (cidrMatches(ip, p)) return true;
            }
        } catch (Exception e) {
            LOGGER.debug("[PortBridge] Error checking allowed subnets", e);
        }
        return false;
    }

    // Simple CIDR match for IPv4
    private boolean cidrMatches(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) return false;
            long ipLong = inet4ToLong(InetAddress.getByName(ip));
            long cidrBase = inet4ToLong(InetAddress.getByName(parts[0]));
            int prefix = Integer.parseInt(parts[1]);
            long mask = prefix == 0 ? 0 : 0xffffffffL << (32 - prefix) & 0xffffffffL;
            return (ipLong & mask) == (cidrBase & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private long inet4ToLong(InetAddress inet) {
        byte[] addr = inet.getAddress();
        long r = 0;
        for (byte b : addr) {
            r = (r << 8) | (b & 0xff);
        }
        return r & 0xffffffffL;
    }

    private void refreshMapping() {
        try {
            if (!active || upnp == null) return;

            String localIP = getLocalIP();
            if (localIP == null) return;

            long now = System.currentTimeMillis();
            for (java.util.Map.Entry<Integer, Long> entry : mappedPortsExpiry.entrySet()) {
                int extPort = entry.getKey();
                long expiry = entry.getValue();
                if (expiry == Long.MAX_VALUE) continue; // indefinite

                long timeLeft = expiry - now;
                // Renew if less than 25% of lease remaining or less than 60s
                if (timeLeft < (leaseDuration * 1000L) / 4 || timeLeft < 60000) {
                    boolean ok = upnp.openPortTCP(extPort, Config.INTERNAL_PORT.get(), localIP, "PortBridge-Minecraft", (int) leaseDuration);
                    if (ok) {
                        long newExpiry = System.currentTimeMillis() + (leaseDuration * 1000L);
                        mappedPortsExpiry.put(extPort, newExpiry);
                        if (Config.DEBUG_LOGGING.get()) {
                            LOGGER.debug("[PortBridge] Renewed mapping for port {} newExpiry={}", extPort, newExpiry);
                        }
                    } else {
                        LOGGER.warn("[PortBridge] Failed to renew mapping for port {}", extPort);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("[PortBridge] Error refreshing UPnP mapping", e);
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

    @Override
    public void stop() {
        active = false;
        executor.submit(() -> {
            try {
                if (upnp != null && !mappedPortsExpiry.isEmpty()) {
                    for (Integer ext : mappedPortsExpiry.keySet()) {
                        try {
                            upnp.closePortTCP(ext);
                            if (Config.DEBUG_LOGGING.get()) {
                                LOGGER.debug("[PortBridge] Removed mapping for port {}", ext);
                            }
                        } catch (Exception ex) {
                            LOGGER.warn("[PortBridge] Error removing mapping for port {}", ext, ex);
                        }
                    }
                    mappedPortsExpiry.clear();
                    LOGGER.info("[PortBridge] UPnP port mappings removed");
                }
            } catch (Exception e) {
                LOGGER.error("[PortBridge] Error closing UPnP mapping", e);
            }
        });
    }

    // Diagnostics helpers
    public String getServiceType() {
        return upnp != null ? upnp.getServiceType() : null;
    }

    public String getControlURL() {
        return upnp != null ? upnp.getControlURL() : null;
    }

    public java.util.List<String> getMappedPortsInfo() {
        java.util.List<String> out = new java.util.ArrayList<>();
        long now = System.currentTimeMillis();
        for (java.util.Map.Entry<Integer, Long> e : mappedPortsExpiry.entrySet()) {
            long expiresIn = e.getValue() == Long.MAX_VALUE ? -1 : (e.getValue() - now) / 1000L;
            out.add(e.getKey() + " (expires in: " + (expiresIn < 0 ? "indefinite" : expiresIn + "s") + ")");
        }
        return out;
    }

    public String getLastWanIp() {
        return lastWanIp;
    }

    public void shutdown() {
        stop();
        executor.shutdownNow();
        if (ipResolver != null) {
            ipResolver.shutdown();
        }
    }
}
