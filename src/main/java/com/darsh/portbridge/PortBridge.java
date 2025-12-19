package com.darsh.portbridge;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.darsh.portbridge.exposure.ExposureManager;

@Mod(PortBridge.MODID)
@EventBusSubscriber(modid = PortBridge.MODID)
public class PortBridge {
    public static final String MODID = "portbridge";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static PortForwardingManager portForwardingManager;
    private static PublicIPResolver publicIPResolver;
    private static ExposureManager exposureManager;
    private static String publicAddress;
    private static MinecraftServer server;

    public PortBridge(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        CommandHandler.setInstance(this);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        server = event.getServer();
        LOGGER.info("[PortBridge] Server started, initializing exposure methods");

        exposureManager = new ExposureManager();
        exposureManager.start(Config.INTERNAL_PORT.get(), Config.EXTERNAL_PORT.get());

        // Wait for exposure to complete
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait 5 seconds
                printPublicAddress();
                if (Config.ENABLE_OPERATOR_BROADCAST.get()) {
                    broadcastToOperators();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "PortBridge-Init").start();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (exposureManager != null) {
            exposureManager.shutdown();
        }
    }

    private static void printPublicAddress() {
        if (exposureManager != null && exposureManager.isExposed()) {
            String address = exposureManager.getPublicAddress();
            String method = exposureManager.getExposureMethod();
            publicAddress = address;

            LOGGER.info("[PortBridge] ========================================");
            LOGGER.info("[PortBridge] Server is publicly accessible!");
            LOGGER.info("[PortBridge] Method: {}", method);
            LOGGER.info("[PortBridge] Address: {}", address);
            LOGGER.info("[PortBridge] ========================================");
        } else {
            LOGGER.warn("[PortBridge] ========================================");
            LOGGER.warn("[PortBridge] Port exposure failed or unavailable");
            if (exposureManager != null && exposureManager.getLastError() != null) {
                LOGGER.warn("[PortBridge] Reason: {}", exposureManager.getLastError());
            }
            LOGGER.warn("[PortBridge] Manual port forwarding may be required");
            LOGGER.warn("[PortBridge] ========================================");
        }
    }

    private static void broadcastToOperators() {
        if (server == null || exposureManager == null || !exposureManager.isExposed()) return;

        String address = exposureManager.getPublicAddress();
        String method = exposureManager.getExposureMethod();
        String publicIp = address != null && address.contains(":") ? address.split(":")[0] : "";
        String port = address != null && address.contains(":") ? address.split(":")[1] : "";

        String template = Config.OP_BROADCAST_TEMPLATE.get();
        String msgText = template.replace("{public_ip}", publicIp)
                .replace("{port}", port)
                .replace("{protocol}", "TCP")
                .replace("{world}", server.getWorldData().getLevelName());

        Component message = Component.literal(msgText);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (server.getPlayerList().isOp(player.getGameProfile())) {
                player.sendSystemMessage(message);
            }
        }
    }

    // Methods for commands
    public PortForwardingManager getPortForwardingManager() {
        return portForwardingManager;
    }

    public String getPublicAddress() {
        return publicAddress;
    }

    public ExposureManager getExposureManager() {
        return exposureManager;
    }

    public void enablePortForwarding() {
        if (exposureManager != null) {
            exposureManager.start(Config.INTERNAL_PORT.get(), Config.EXTERNAL_PORT.get());
        }
    }

    public void disablePortForwarding() {
        if (exposureManager != null) {
            exposureManager.stop();
        }
    }

    public void reloadConfig() {
        // Config is automatically reloaded via event
    }
}
