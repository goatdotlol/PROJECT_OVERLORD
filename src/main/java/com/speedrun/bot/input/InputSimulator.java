package com.speedrun.bot.input;

import com.speedrun.bot.utils.DebugLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class InputSimulator {
    private static final Map<KeyBinding, Integer> timedState = new HashMap<>(); // Ticks left
    private static final Map<KeyBinding, Boolean> activeState = new HashMap<>(); // Always pressed
    private static final Random random = new Random();

    // Rotation State
    private static boolean isRotating = false;
    private static float targetYaw, targetPitch;
    private static float startYaw, startPitch;
    private static int rotationTicks, currentRotationTick;

    public static void tick(MinecraftClient client) {
        // Handle Timed Strokes
        timedState.entrySet().removeIf(entry -> {
            int ticksLeft = entry.getValue();
            if (ticksLeft > 0) {
                entry.getKey().setPressed(true);
                entry.setValue(ticksLeft - 1);
                return false;
            } else {
                return true; // Finished, but activeState might keep it pressed
            }
        });

        // Handle Active States (continuous holding)
        for (Map.Entry<KeyBinding, Boolean> entry : activeState.entrySet()) {
            if (entry.getValue()) {
                entry.getKey().setPressed(true);
            }
        }

        // Handle Smooth Rotation
        if (isRotating && client.player != null) {
            updateRotation(client);
        }
    }

    public static void setKeyState(KeyBinding key, boolean pressed) {
        activeState.put(key, pressed);
        key.setPressed(pressed);
    }

    public static void pressKey(KeyBinding key, int durationTicks) {
        int variance = random.nextInt(3) - 1;
        timedState.put(key, Math.max(1, durationTicks + variance));
    }

    public static void lookAt(Vec3d target, int durationTicks) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;

        Vec3d diff = target.subtract(client.player.getCameraPosVec(1.0f));
        double diffX = diff.x;
        double diffY = diff.y;
        double diffZ = diff.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float pitch = (float) MathHelper.wrapDegrees(-Math.toDegrees(Math.atan2(diffY, diffXZ)));

        smoothLook(yaw, pitch, durationTicks);
    }

    public static void smoothLook(float yaw, float pitch, int durationTicks) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;

        startYaw = client.player.yaw;
        startPitch = client.player.pitch;
        targetYaw = yaw;
        targetPitch = pitch;
        rotationTicks = Math.max(1, durationTicks);
        currentRotationTick = 0;
        isRotating = true;
    }

    private static void updateRotation(MinecraftClient client) {
        if (currentRotationTick >= rotationTicks) {
            isRotating = false;
            client.player.yaw = targetYaw;
            client.player.pitch = targetPitch;
            return;
        }

        currentRotationTick++;
        float progress = (float) currentRotationTick / rotationTicks;
        float smoothProgress = progress * progress * (3 - 2 * progress);

        client.player.yaw = startYaw + MathHelper.wrapDegrees(targetYaw - startYaw) * smoothProgress;
        client.player.pitch = startPitch + (targetPitch - startPitch) * smoothProgress;
    }

    public static void attack() {
        MinecraftClient client = MinecraftClient.getInstance();
        pressKey(client.options.keyAttack, 1);
    }

    public static boolean isBusy() {
        return isRotating || !timedState.isEmpty();
    }
}
