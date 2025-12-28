package com.speedrun.bot.systems;

import com.speedrun.bot.utils.DebugLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.BlockHitResult;

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
            // Precise Look (Priority 2: Interaction Override)
            HumanoidControl.lookAt(client, breakTarget, 2);

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

    public static void interactBlock(MinecraftClient client, BlockPos pos) {
        if (client.player == null || client.world == null || client.interactionManager == null)
            return;

        // Look at it
        HumanoidControl.lookAt(client, pos, 2);

        // Right Click
        // We need a hit result. Raycast or fake it.
        // For 1.16.1 we can use interactionManager.interactBlock
        // We need to construct a BlockHitResult.
        Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        BlockHitResult hit = new BlockHitResult(center, Direction.UP, pos, false);

        client.interactionManager.interactBlock(client.player, client.world, Hand.MAIN_HAND, hit);
    }

    public static boolean isBusy() {
        return breakTarget != null;
    }
}
