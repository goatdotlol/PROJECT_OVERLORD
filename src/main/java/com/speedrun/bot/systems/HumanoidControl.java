package com.speedrun.bot.systems;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * HumanoidControl - PID-like smoothed rotations and motion.
 * Prevents robotic snapping and makes movement look humane.
 */
public class HumanoidControl {

    private static float targetYaw;
    private static float targetPitch;
    private static int currentPriority = 0; // 0=None, 1=Pathing, 2=Interaction

    /**
     * Set the look target with a priority level.
     * 
     * @param priority 1 for Walking, 2 for Interacting/Attacking
     */
    public static void lookAt(MinecraftClient client, BlockPos target, int priority) {
        if (client.player == null)
            return;
        if (priority < currentPriority)
            return; // Ignore lower priority requests

        currentPriority = priority;

        Vec3d eyes = client.player.getCameraPosVec(1.0f);
        Vec3d dest = new Vec3d(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        Vec3d diff = dest.subtract(eyes);

        double dist = MathHelper.sqrt(diff.x * diff.x + diff.z * diff.z);
        targetPitch = MathHelper.wrapDegrees((float) (-(MathHelper.atan2(diff.y, dist) * 57.2957763671875)));
        targetYaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(diff.z, diff.x) * 57.2957763671875) - 90.0F);
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null)
            return;

        // Reset priority for the next tick cycle
        currentPriority = 0;

        float yawDiff = MathHelper.wrapDegrees(targetYaw - client.player.yaw);
        float pitchDiff = MathHelper.wrapDegrees(targetPitch - client.player.pitch);

        // Clamp rotation speed to ~40 degrees per tick for realism
        float step = 40.0f;

        if (yawDiff > step)
            yawDiff = step;
        if (yawDiff < -step)
            yawDiff = -step;
        if (pitchDiff > step)
            pitchDiff = step;
        if (pitchDiff < -step)
            pitchDiff = -step;

        // Damped interpolation
        client.player.yaw += yawDiff * 0.7f;
        client.player.pitch += pitchDiff * 0.7f;
    }
}
