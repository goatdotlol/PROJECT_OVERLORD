package com.speedrun.bot.systems;

import com.speedrun.bot.utils.DebugLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Hand;

/**
 * InteractionControl - Precision breaking and interacting.
 * Hits block centers and monitors health.
 */
public class InteractionControl {

    private static BlockPos breakTarget = null;
    private static int breakTicks = 0;

    public static void tick(MinecraftClient client) {
        if (client.player == null || breakTarget == null)
            return;

        double distSq = client.player.getBlockPos().getSquaredDistance(breakTarget);

        if (distSq < 25.0) { // Within 5 blocks
            // Precise Look
            HumanoidControl.lookAt(client, breakTarget);

            // Start Attacking
            client.options.keyAttack.setPressed(true);

            // Check if block is gone
            if (client.world.getBlockState(breakTarget).isAir()) {
                stopBreaking(client);
                AsyncChunkScanner.invalidateCache();
            }
        } else {
            client.options.keyAttack.setPressed(false);
        }
    }

    public static void setBreakTarget(BlockPos pos) {
        breakTarget = pos;
    }

    public static void stopBreaking(MinecraftClient client) {
        breakTarget = null;
        if (client.options != null) {
            client.options.keyAttack.setPressed(false);
        }
    }

    public static boolean isBusy() {
        return breakTarget != null;
    }
}
