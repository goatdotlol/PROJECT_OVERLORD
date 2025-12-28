package com.speedrun.bot;

import com.speedrun.bot.utils.DebugLogger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import com.speedrun.bot.input.InputSimulator;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class BotMain implements ClientModInitializer {

    private static KeyBinding configKey;

    @Override
    public void onInitializeClient() {
        DebugLogger.clear();
        DebugLogger.log("Initializing Project OVERLORD - Ghost Engine (v1.0)");

        // Register KeyBinding: Right Shift
        // Uses Fabric API KeyBindingHelper
        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ghost.config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.ghost.speedrun"));

        // Register Client Tick Event
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Safety check: Don't run logic if not in-game
            if (client.player == null || client.world == null)
                return;

            // Input Handling
            InputSimulator.tick(client);

            // Strategy Execution
            com.speedrun.bot.strategy.OverworldManager.tick(client);

            while (configKey.wasPressed()) {
                DebugLogger.log("Input: Right Shift Pressed. Opening Config.");
                client.openScreen(new com.speedrun.bot.gui.ConfigScreen());
            }
        });

        DebugLogger.log("Ghost Engine Initialized Successfully.");
    }
}
