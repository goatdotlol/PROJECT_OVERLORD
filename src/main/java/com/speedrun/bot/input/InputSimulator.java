package com.speedrun.bot.input;

import com.speedrun.bot.utils.DebugLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.util.math.MathHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Iterator;

public class InputSimulator {
    private static final Map<KeyBinding, Integer> keyState = new HashMap<>();
    private static final Random random = new Random();
    
    // Rotation State
    private static boolean isRotating = false;
    private static float targetYaw, targetPitch;
    private static float startYaw, startPitch;
    private static int rotationTicks, currentRotationTick;

    public static void tick(MinecraftClient client) {
        // Handle Key Holding
        Iterator<Map.Entry<KeyBinding, Integer>> it = keyState.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<KeyBinding, Integer> entry = it.next();
            KeyBinding key = entry.getKey();
            int ticksLeft = entry.getValue();
            
            if (ticksLeft > 0) {
                key.setPressed(true); // Force pressed state
                entry.setValue(ticksLeft - 1);
            } else {
                key.setPressed(false); // Release
                it.remove();
            }
        }
        
        // Handle Rotation
        if (isRotating && client.player != null) {
            updateRotation(client);
        }
    }

    public static void pressKey(KeyBinding key, int durationTicks) {
        // Humanizer: Add variance to duration (+/- 2 ticks)
        int variance = random.nextInt(5) - 2; 
        int finalDuration = Math.max(1, durationTicks + variance);
        
        keyState.put(key, finalDuration);
    }
    
    public static void smoothLook(float yaw, float pitch, int durationTicks) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        startYaw = client.player.yaw;
        startPitch = client.player.pitch;
        targetYaw = yaw;
        targetPitch = pitch;
        
        // Humanizer: Add variance to speed
        int variance = random.nextInt(3);
        rotationTicks = Math.max(2, durationTicks + variance);
        
        currentRotationTick = 0;
        isRotating = true;
    }

    private static void updateRotation(MinecraftClient client) {
        if (currentRotationTick >= rotationTicks) {
            isRotating = false;
            // Snap to final to ensure precision at end
            client.player.yaw = targetYaw;
            client.player.pitch = targetPitch;
            return;
        }
        
        currentRotationTick++;
        float progress = (float) currentRotationTick / rotationTicks;
        
        // Smooth Step interpolation for natural mouse acceleration
        float smoothProgress = progress * progress * (3 - 2 * progress); 

        client.player.yaw = startYaw + (targetYaw - startYaw) * smoothProgress;
        client.player.pitch = startPitch + (targetPitch - startPitch) * smoothProgress;
    }
    
    public static void attack(MinecraftClient client) {
        if (client.options.keyAttack.isPressed()) return; // Don't spam if held
        // Instant click
        client.options.keyAttack.setPressed(true); 
        // Note: Release is handled by game or next tick?
        // Actually for attack, we usually press and release.
        // We'll queue a release next tick if needed, or just setPressed(true) is enough for 1 tick attack.
    }
    
    public static boolean isBusy() {
        return isRotating || !keyState.isEmpty();
    }
}
