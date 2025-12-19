package com.darsh.portbridge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = PortBridge.MODID)
public class CommandHandler {
    private static PortBridge instance;

    public static void setInstance(PortBridge mod) {
        instance = mod;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("portbridge")
                .requires(source -> source.hasPermission(2)) // Operator level
                .then(Commands.literal("status").executes(CommandHandler::status))
                .then(Commands.literal("diag").executes(CommandHandler::diag))
                .then(Commands.literal("retry").executes(CommandHandler::retry))
                .then(Commands.literal("enable").executes(CommandHandler::enable))
                .then(Commands.literal("disable").executes(CommandHandler::disable))
                .then(Commands.literal("reload").executes(CommandHandler::reload)));
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        if (instance == null) {
            CommandSourceStack source = context.getSource();
            source.sendSuccess(() -> Component.literal("PortBridge: Mod instance not available"), false);
            return 0;
        }

        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6=== PortBridge Status ===§r"), false);

        var exposureManager = instance.getExposureManager();
        if (exposureManager == null) {
            source.sendSuccess(() -> Component.literal("  Status: Exposure manager not initialized"), false);
            return 1;
        }

        boolean exposed = exposureManager.isExposed();
        String method = exposureManager.getExposureMethod();
        String address = exposureManager.getPublicAddress();

        source.sendSuccess(() -> Component.literal("  Exposed: " + (exposed ? "§aYes§r" : "§cNo§r")), false);
        source.sendSuccess(() -> Component.literal("  Method: " + method), false);
        if (address != null) {
            source.sendSuccess(() -> Component.literal("  Address: §e" + address + "§r"), false);
        }
        if (exposureManager.getLastError() != null) {
            source.sendSuccess(() -> Component.literal("  Error: §c" + exposureManager.getLastError() + "§r"), false);
        }

        return 1;
    }

    private static int diag(CommandContext<CommandSourceStack> context) {
        if (instance == null) {
            CommandSourceStack source = context.getSource();
            source.sendSuccess(() -> Component.literal("PortBridge: Mod instance not available"), false);
            return 0;
        }

        CommandSourceStack source = context.getSource();
        var exposureManager = instance.getExposureManager();

        if (exposureManager != null) {
            String diagnostics = exposureManager.getDiagnostics();
            for (String line : diagnostics.split("\n")) {
                source.sendSuccess(() -> Component.literal(line), false);
            }
        } else {
            source.sendSuccess(() -> Component.literal("Exposure manager not initialized"), false);
        }

        return 1;
    }

    private static int retry(CommandContext<CommandSourceStack> context) {
        if (instance == null) return 0;

        CommandSourceStack source = context.getSource();
        var exposureManager = instance.getExposureManager();
        if (exposureManager != null) {
            exposureManager.start(Config.INTERNAL_PORT.get(), Config.EXTERNAL_PORT.get());
            source.sendSuccess(() -> Component.literal("Retrying exposure methods..."), false);
        }
        return 1;
    }

    private static int enable(CommandContext<CommandSourceStack> context) {
        if (instance == null) return 0;

        CommandSourceStack source = context.getSource();
        instance.enablePortForwarding();
        source.sendSuccess(() -> Component.literal("Port forwarding enabled"), false);
        return 1;
    }

    private static int disable(CommandContext<CommandSourceStack> context) {
        if (instance == null) return 0;

        CommandSourceStack source = context.getSource();
        instance.disablePortForwarding();
        source.sendSuccess(() -> Component.literal("Port exposure disabled"), false);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        if (instance == null) return 0;

        CommandSourceStack source = context.getSource();
        instance.reloadConfig();
        source.sendSuccess(() -> Component.literal("Config reloaded"), false);
        return 1;
    }
}