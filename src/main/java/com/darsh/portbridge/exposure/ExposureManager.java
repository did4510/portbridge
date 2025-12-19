package com.darsh.portbridge.exposure;

import com.darsh.portbridge.Config;
import org.slf4j.Logger;
import com.darsh.portbridge.PortBridge;

public class ExposureManager {
    private static final Logger LOGGER = PortBridge.LOGGER;

    private UPnPExposureService upnpService;
    private TunnelExposureService tunnelService;
    private ExposureService activeService;
    private String tunnelMode;

    public ExposureManager() {
        this.upnpService = new UPnPExposureService();
        this.tunnelService = new TunnelExposureService();
    }

    public void start(int internalPort, int externalPort) {
        tunnelMode = Config.TUNNEL_MODE.get();

        if ("FORCE".equalsIgnoreCase(tunnelMode)) {
            LOGGER.info("[PortBridge] Tunnel mode: FORCE");
            startTunnel(internalPort);
        } else if ("AUTO".equalsIgnoreCase(tunnelMode) && Config.TUNNEL_ENABLED.get()) {
            LOGGER.info("[PortBridge] Tunnel mode: AUTO");
            startUPnPWithTunnelFallback(internalPort, externalPort);
        } else {
            LOGGER.info("[PortBridge] UPnP mode");
            startUPnP(internalPort, externalPort);
        }
    }

    private void startUPnPWithTunnelFallback(int internalPort, int externalPort) {
        LOGGER.info("[PortBridge] Attempting UPnP first, tunnel as fallback");
        upnpService.start(internalPort, externalPort);

        // Check UPnP status after delay
        new Thread(() -> {
            try {
                Thread.sleep(10000); // Wait 10 seconds for UPnP to complete
                if (!upnpService.isActive()) {
                    LOGGER.info("[PortBridge] UPnP failed, falling back to tunnel");
                    startTunnel(internalPort);
                } else {
                    activeService = upnpService;
                    LOGGER.info("[PortBridge] UPnP active, skipping tunnel");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "PortBridge-FallbackCheck").start();
    }

    private void startUPnP(int internalPort, int externalPort) {
        upnpService.start(internalPort, externalPort);
        activeService = upnpService;
    }

    private void startTunnel(int internalPort) {
        if (!Config.TUNNEL_ENABLED.get()) {
            LOGGER.warn("[PortBridge] Tunnel disabled in config");
            return;
        }
        tunnelService.start(internalPort, -1);
        activeService = tunnelService;
    }

    public boolean isExposed() {
        return activeService != null && activeService.isActive();
    }

    public String getPublicAddress() {
        if (activeService != null) {
            return activeService.getPublicAddress();
        }
        return null;
    }

    public String getExposureMethod() {
        if (activeService != null) {
            return activeService.getExposureMethod();
        }
        return "NONE";
    }

    public String getLastError() {
        if (activeService != null) {
            return activeService.getLastError();
        }
        return null;
    }

    public String getDiagnostics() {
        StringBuilder sb = new StringBuilder();

        sb.append("=== PortBridge Exposure Status ===\n");
        sb.append("Exposed: ").append(isExposed()).append("\n");
        sb.append("Method: ").append(getExposureMethod()).append("\n");

        if (activeService instanceof TunnelExposureService) {
            TunnelExposureService tunnel = (TunnelExposureService) activeService;
            sb.append("Tunnel Status: ").append(tunnel.getStatus()).append("\n");
            sb.append("Session ID: ").append(tunnel.getSessionId()).append("\n");
            sb.append("Latency: ").append(tunnel.getLatency()).append("ms\n");
            sb.append("Bytes Transferred: ").append(tunnel.getBytesTransferred()).append("\n");
        }

        if (activeService != null) {
            sb.append("Public Address: ").append(getPublicAddress()).append("\n");
            if (getLastError() != null) {
                sb.append("Last Error: ").append(getLastError()).append("\n");
            }

            if (activeService instanceof UPnPExposureService) {
                UPnPExposureService upnp = (UPnPExposureService) activeService;
                sb.append("UPnP Service Type: ").append(upnp.getServiceType()).append("\n");
                sb.append("Control URL: ").append(upnp.getControlURL()).append("\n");
                sb.append("WAN IP: ").append(upnp.getLastWanIp()).append("\n");
                sb.append("Mapped Ports:\n");
                for (String info : upnp.getMappedPortsInfo()) {
                    sb.append("  - ").append(info).append("\n");
                }
            }
        }

        return sb.toString();
    }

    public void stop() {
        if (upnpService != null) {
            upnpService.stop();
        }
        if (tunnelService != null) {
            tunnelService.stop();
        }
        activeService = null;
    }

    public void shutdown() {
        if (upnpService != null) {
            upnpService.shutdown();
        }
        if (tunnelService != null) {
            tunnelService.shutdown();
        }
        activeService = null;
    }
}
