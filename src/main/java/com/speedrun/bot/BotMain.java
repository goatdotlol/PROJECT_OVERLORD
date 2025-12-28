package com.speedrun.bot;

import com.speedrun.bot.systems.*;
import com.speedrun.bot.utils.DebugLogger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

public class BotMain implements ClientModInitializer {

    public static final String MOD_ID = "ghost_runner";
    private static KeyBinding toggleKey;
    private static boolean enabled = false;

    @Override
    public void onInitializeClient() {
        DebugLogger.clear();
        DebugLogger.log("Initializing Project OVERLORD v2.0 - Ghost Engine");

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ghost.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.ghost.speedrun"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null)
                return;

            // Input Handling
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                client.player.sendMessage(
                        new LiteralText("§6[OVERLORD] Autonomous Mode: " + (enabled ? "§aON" : "§cOFF")), true);
                if (!enabled) {
                    PathingControl.stop(client);
                    InteractionControl.stopBreaking(client);
                    GoalEngine.reset();
                }
            }

            if (!enabled)
                return;

            // --- GOD-TIER EXECUTION PIPELINE (v2.0) ---

            // 1. PERCEPTION: Update world cache (Time-sliced chunks)
            AsyncChunkScanner.tick(client);

            // 2. DECISION: Strategy and State Machine
            GoalEngine.tick(client);

            // 3. NAVIGATION: Path following
            PathingControl.tick(client);

            // 4. ACTION: Breaking blocks
            InteractionControl.tick(client);

            // 5. MOTION: Natural camera smoothing
            HumanoidControl.tick(client);
        });

        DebugLogger.log("Ghost Engine v2.0 successfully initialized.");
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
