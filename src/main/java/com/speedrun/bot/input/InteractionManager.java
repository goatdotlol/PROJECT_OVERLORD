package com.speedrun.bot.input;

import com.speedrun.bot.utils.DebugLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

/**
 * InteractionManager - Precision block breaking and item usage.
 */
public class InteractionManager {

    private static int breakingTicks = 0;
    private static BlockPos currentTarget = null;
    private static int maxBreakingTicks = 0;

    public static void tick(MinecraftClient client) {
        if (breakingTicks > 0) {
            breakingTicks--;
            if (currentTarget != null) {
                // Precision Alignment: Look at the dead center of the block
                Vec3d center = new Vec3d(currentTarget.getX() + 0.5, currentTarget.getY() + 0.5,
                        currentTarget.getZ() + 0.5);
                InputSimulator.lookAt(center, 1);

                // Only start attacking after a short look-at delay to ensure alignment
                if (maxBreakingTicks - breakingTicks > 2) {
                    InputSimulator.setKeyState(client.options.keyAttack, true);
                }
            }

            if (breakingTicks == 0) {
                InputSimulator.setKeyState(client.options.keyAttack, false);
                currentTarget = null;
            }
        }
    }

    public static void breakBlock(BlockPos pos, int approxTicks) {
        currentTarget = pos;
        breakingTicks = approxTicks;
        maxBreakingTicks = approxTicks;
        DebugLogger.log("[HANDS] Precision break started at " + pos);
    }

    public static boolean isInteracting() {
        return breakingTicks > 0;
    }

    public static void stopInteraction() {
        breakingTicks = 0;
        currentTarget = null;
        MinecraftClient client = MinecraftClient.getInstance();
        InputSimulator.setKeyState(client.options.keyAttack, false);
    }

    /**
     * Tries to use the item in hand on the current target block.
     */
    public static void useOnBlock(MinecraftClient client, BlockPos pos) {
        if (client.player == null)
            return;
        Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        InputSimulator.lookAt(center, 2);

        // Simulating right click
        InputSimulator.pressKey(client.options.keyUse, 2);
    }
}
