package com.speedrun.bot.systems;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/**
 * PortalCastingControl - Logic for constructing a Nether Portal using Lava
 * Pool.
 */
public class PortalCastingControl {

    private static int step = 0;

    public static void tick(MinecraftClient client) {
        // TODO: Full implementation of 4-block lava casting
        // 1. Place Block
        // 2. Place Water
        // 3. Break Block
        // 4. Fill with Lava buckets
    }

    public static boolean isComplete(MinecraftClient client) {
        // Check for 2x3 Obsidian frame
        return false;
    }
}
