package com.darsh.portbridge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = PortBridge.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_PORT_FORWARDING = BUILDER
            .comment("Enable automatic port forwarding on server start")
            .define("enablePortForwarding", true);

    public static final ModConfigSpec.IntValue INTERNAL_PORT = BUILDER
            .comment("The internal port the Minecraft server is running on")
            .defineInRange("internalPort", 25565, 1, 65535);

    public static final ModConfigSpec.IntValue EXTERNAL_PORT = BUILDER
            .comment("The external port to forward to (default same as internal)")
            .defineInRange("externalPort", 25565, 1, 65535);

    public static final ModConfigSpec.IntValue LEASE_DURATION = BUILDER
            .comment("Lease duration in seconds for port mapping (0 for indefinite)")
            .defineInRange("leaseDuration", 3600, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue REFRESH_INTERVAL = BUILDER
            .comment("Interval in seconds to refresh the port mapping")
            .defineInRange("refreshInterval", 1800, 60, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue RETRY_COUNT = BUILDER
            .comment("Number of retries for port forwarding discovery and mapping")
            .defineInRange("retryCount", 3, 0, 10);

    public static final ModConfigSpec.IntValue RETRY_DELAY = BUILDER
            .comment("Delay in seconds between retries")
            .defineInRange("retryDelay", 5, 1, 60);

    public static final ModConfigSpec.BooleanValue ENABLE_PUBLIC_IP_FALLBACK = BUILDER
            .comment("Enable fallback to external service for public IP detection")
            .define("enablePublicIPFallback", false);

    public static final ModConfigSpec.ConfigValue<String> PUBLIC_IP_FALLBACK_URL = BUILDER
            .comment("URL to query for public IP fallback (e.g., https://api.ipify.org)")
            .define("publicIPFallbackURL", "https://api.ipify.org");

    public static final ModConfigSpec.BooleanValue DRY_RUN = BUILDER
            .comment("When true, do not perform real UPnP calls; simulate actions for testing")
            .define("advanced.dryRun", false);

    public static final ModConfigSpec.ConfigValue<String> ALLOWED_SUBNETS = BUILDER
            .comment("Comma-separated list of allowed subnets for UPnP attempts (CIDR). Example: 192.168.0.0/16,10.0.0.0/8")
            .define("allowedSubnets", "192.168.0.0/16,10.0.0.0/8");

    public static final ModConfigSpec.ConfigValue<String> ADDITIONAL_PORTS = BUILDER
            .comment("Additional ports to map. Comma-separated entries in the form name:internal:external:protocol:enabled. Example: rcon:25575:25575:TCP:true")
            .define("additionalPorts", "");

    public static final ModConfigSpec.ConfigValue<String> OP_BROADCAST_TEMPLATE = BUILDER
            .comment("Message template sent to operators on successful mapping. Placeholders: {public_ip} {port} {protocol} {world}")
            .define("operatorMessageTemplate", "§a[PortBridge] Public Join Address: §e{public_ip}:{port}");

    public static final ModConfigSpec.BooleanValue ENABLE_OPERATOR_BROADCAST = BUILDER
            .comment("Send join address to operators in-game")
            .define("enableOperatorBroadcast", true);

    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Enable debug logging for troubleshooting")
            .define("debugLogging", false);

    // Tunnel Configuration
    public static final ModConfigSpec.BooleanValue TUNNEL_ENABLED = BUILDER
            .comment("Enable tunnel-based port exposure (fallback or primary method)")
            .define("tunnel.enabled", true);

    public static final ModConfigSpec.ConfigValue<String> TUNNEL_MODE = BUILDER
            .comment("Tunnel mode: AUTO (UPnP with tunnel fallback) or FORCE (always use tunnel)")
            .define("tunnel.mode", "AUTO");

    public static final ModConfigSpec.ConfigValue<String> TUNNEL_RELAY_HOST = BUILDER
            .comment("Tunnel relay server hostname")
            .define("tunnel.relay.host", "relay.portbridge.net");

    public static final ModConfigSpec.IntValue TUNNEL_RELAY_PORT = BUILDER
            .comment("Tunnel relay server port")
            .defineInRange("tunnel.relay.port", 7000, 1, 65535);

    public static final ModConfigSpec.IntValue TUNNEL_RECONNECT_BASE_DELAY = BUILDER
            .comment("Base delay in seconds for tunnel reconnection attempts (exponential backoff)")
            .defineInRange("tunnel.reconnect.baseDelaySeconds", 5, 1, 60);

    public static final ModConfigSpec.IntValue TUNNEL_RECONNECT_MAX_DELAY = BUILDER
            .comment("Maximum delay in seconds for tunnel reconnection")
            .defineInRange("tunnel.reconnect.maxDelaySeconds", 120, 10, 3600);

    public static final ModConfigSpec.IntValue TUNNEL_KEEPALIVE_INTERVAL = BUILDER
            .comment("Keepalive interval in seconds for tunnel connection")
            .defineInRange("tunnel.keepAliveSeconds", 20, 5, 120);

    static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Config reloaded
        PortBridge.LOGGER.info("PortBridge config reloaded");
    }
}
