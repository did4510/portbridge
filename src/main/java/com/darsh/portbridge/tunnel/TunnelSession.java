package com.darsh.portbridge.tunnel;

import org.slf4j.Logger;
import com.darsh.portbridge.PortBridge;

public class TunnelSession {
    private static final Logger LOGGER = PortBridge.LOGGER;

    private final String sessionId;
    private final String serverUUID;
    private final int minecraftPort;
    private final String worldName;
    private String publicHost;
    private int publicPort;
    private long createdTime;
    private long lastHeartbeat;
    private boolean active;

    public TunnelSession(String serverUUID, int minecraftPort, String worldName) {
        this.sessionId = generateSessionId();
        this.serverUUID = serverUUID;
        this.minecraftPort = minecraftPort;
        this.worldName = worldName;
        this.createdTime = System.currentTimeMillis();
        this.lastHeartbeat = createdTime;
        this.active = false;
    }

    private String generateSessionId() {
        return "pb-" + System.nanoTime() + "-" + (int)(Math.random() * 10000);
    }

    public void updateFromRelayResponse(String host, int port) {
        this.publicHost = host;
        this.publicPort = port;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public void recordHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getServerUUID() {
        return serverUUID;
    }

    public int getMinecraftPort() {
        return minecraftPort;
    }

    public String getWorldName() {
        return worldName;
    }

    public String getPublicHost() {
        return publicHost;
    }

    public int getPublicPort() {
        return publicPort;
    }

    public String getPublicAddress() {
        if (publicHost == null || publicPort <= 0) {
            return null;
        }
        return publicHost + ":" + publicPort;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    public long getHeartbeatAge() {
        return System.currentTimeMillis() - lastHeartbeat;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getUptime() {
        return System.currentTimeMillis() - createdTime;
    }
}
