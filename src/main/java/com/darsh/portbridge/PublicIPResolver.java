package com.darsh.portbridge;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class PublicIPResolver {
    private static final Logger LOGGER = PortBridge.LOGGER;
    private static final Pattern IP_PATTERN = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "PortBridge-IPResolver");
        t.setDaemon(true);
        return t;
    });

    public CompletableFuture<String> getPublicIP() {
        CompletableFuture<String> future = new CompletableFuture<>();

        executor.submit(() -> {
            try {
                String ip = null;

                // Try UPnP first
                SimpleUPnP upnp = new SimpleUPnP();
                if (upnp.isUPnPAvailable()) {
                    ip = upnp.getExternalIP();
                    if (isValidIP(ip)) {
                        if (Config.DEBUG_LOGGING.get()) {
                            LOGGER.debug("[PortBridge] Public IP from UPnP: {}", ip);
                        }
                        future.complete(ip);
                        return;
                    }
                }

                // Fallback to external service
                if (Config.ENABLE_PUBLIC_IP_FALLBACK.get()) {
                    ip = getPublicIPFromService();
                    if (isValidIP(ip)) {
                        if (Config.DEBUG_LOGGING.get()) {
                            LOGGER.debug("[PortBridge] Public IP from service: {}", ip);
                        }
                        future.complete(ip);
                        return;
                    }
                }

                future.complete(null); // No IP found
            } catch (Exception e) {
                LOGGER.error("[PortBridge] Error getting public IP", e);
                future.complete(null);
            }
        });

        return future;
    }

    private String getPublicIPFromService() {
        try {
            URL url = new URL(Config.PUBLIC_IP_FALLBACK_URL.get());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    line = line.trim();
                    if (isValidIP(line)) {
                        return line;
                    }
                }
            }
        } catch (Exception e) {
            if (Config.DEBUG_LOGGING.get()) {
                LOGGER.debug("[PortBridge] Failed to get IP from service: {}", e.getMessage());
            }
        }
        return null;
    }

    private boolean isValidIP(String ip) {
        return ip != null && IP_PATTERN.matcher(ip).matches();
    }

    public void shutdown() {
        executor.shutdown();
    }
}