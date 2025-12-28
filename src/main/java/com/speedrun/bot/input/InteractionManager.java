package com.speedrun.bot.input;

import com.speedrun.bot.utils.DebugLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class InteractionManager {

    private static int breakingTicks = 0;
    private static BlockPos currentTarget = null;

    public static void tick(MinecraftClient client) {
        if (breakingTicks > 0) {
            breakingTicks--;
            if (currentTarget != null) {
                // Keep looking and attacking
                InputSimulator.lookAt(Vec3d.ofCenter(currentTarget), 1);
                InputSimulator.setKeyState(client.options.keyAttack, true);
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
        DebugLogger.log("[HANDS] Breaking block at " + pos);
    }

    public static boolean isInteracting() {
        return breakingTicks > 0;
    }

    /**
     * Tries to use the item in hand on the current target block.
     */
    public static void useOnBlock(MinecraftClient client, BlockPos pos) {
        if (client.player == null || client.interactionManager == null)
            return;
        InputSimulator.lookAt(Vec3d.ofCenter(pos), 2);
        // Note: In 1.16.1, we'd use interactionManager.interactBlock
        // For simplicity in prototype, we just press useKey
        InputSimulator.pressKey(client.options.keyUse, 2);
    }
}
