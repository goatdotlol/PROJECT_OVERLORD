package com.speedrun.bot;

import com.speedrun.bot.utils.DebugLogger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import com.speedrun.bot.input.InputSimulator;
import com.speedrun.bot.input.InteractionManager;
import com.speedrun.bot.strategy.OverworldManager;
import com.speedrun.bot.strategy.AutoSpeedrunManager;
import com.speedrun.bot.navigation.MovementManager;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class BotMain implements ClientModInitializer {

    private static KeyBinding configKey;

    @Override
    public void onInitializeClient() {
        DebugLogger.clear();
        DebugLogger.log("Initializing Project OVERLORD - Ghost Engine (v1.0)");

        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ghost.config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.ghost.speedrun"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null)
                return;

            // 1. Core Logic Tick
            InputSimulator.tick(client);
            InteractionManager.tick(client);

            // 2. Strategy & Goal Selection
            OverworldManager.tick(client); // Passive scanning
            AutoSpeedrunManager.tick(client); // Goal progression

            // 3. Navigation Tick (LEGS)
            MovementManager.tick(client);

            // GUI Toggle
            while (configKey.wasPressed()) {
                client.openScreen(new com.speedrun.bot.gui.ConfigScreen());
            }
        });

        DebugLogger.log("Ghost Engine Initialized Successfully.");
    }
}
