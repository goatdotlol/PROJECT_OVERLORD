package com.speedrun.bot.perception;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.tag.BlockTags;
import java.util.concurrent.CompletableFuture;

/**
 * DistributedScanner - Spreads work over multiple ticks to prevent FPS drops.
 * Scans a 100-block radius without local lag spikes.
 */
public class DistributedScanner {

    private static int currentR = 1;
    private static int currentY = -5;
    private static final int CHUNKS_PER_TICK = 4; // Scan 4 shells/layers per tick

    private static BlockPos foundTree = null;
    private static BlockPos foundIron = null;
    private static BlockPos foundLava = null;
    private static BlockPos foundVillage = null;

    public static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null)
            return;

        BlockPos playerPos = client.player.getBlockPos();

        // Scan a few "shells" per tick
        for (int i = 0; i < CHUNKS_PER_TICK; i++) {
            scanShell(client, playerPos, currentR, currentY);

            // Increment Y layer
            currentY++;
            if (currentY > 15) {
                currentY = -10;
                currentR++; // Move to next radial shell
            }

            // Loop back if radius exceeds limit
            if (currentR > 80) {
                currentR = 1;
                resetResults();
            }
        }
    }

    private static void scanShell(MinecraftClient client, BlockPos center, int r, int y) {
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                // Only scan the outer perimeter of this shell
                if (Math.abs(x) != r && Math.abs(z) != r)
                    continue;

                BlockPos pos = center.add(x, y, z);
                Block block = client.world.getBlockState(pos).getBlock();

                if (foundTree == null && block.isIn(BlockTags.LOGS)) {
                    foundTree = pos;
                } else if (foundIron == null && block == Blocks.IRON_ORE) {
                    foundIron = pos;
                } else if (foundLava == null && block == Blocks.LAVA) {
                    if (client.world.getBlockState(pos.east()).getBlock() == Blocks.LAVA) {
                        foundLava = pos;
                    }
                } else if (foundVillage == null && block == Blocks.BELL) {
                    foundVillage = pos;
                }
            }
        }
    }

    private static void resetResults() {
        foundTree = null;
        foundIron = null;
        foundLava = null;
        foundVillage = null;
    }

    public static BlockPos getTree() {
        return foundTree;
    }

    public static BlockPos getIron() {
        return foundIron;
    }

    public static BlockPos getLava() {
        return foundLava;
    }

    public static BlockPos getVillage() {
        return foundVillage;
    }
}
