package com.speedrun.bot.systems;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import java.util.Comparator;

/**
 * AsyncChunkScanner - Zero-Lag Per-Tick Scanning.
 * Scans immediate area every tick, and far chunks over time.
 */
public class AsyncChunkScanner {

    private static final int SCAN_RADIUS_CHUNKS = 8;
    private static int scanIndexX = -SCAN_RADIUS_CHUNKS;
    private static int scanIndexZ = -SCAN_RADIUS_CHUNKS;

    private static BlockPos nearestLog = null;
    private static BlockPos nearestIron = null;
    private static BlockPos nearestStone = null;
    private static Entity nearestGolem = null;

    public static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null)
            return;

        BlockPos playerPos = client.player.getBlockPos();

        // Reset near caches for immediate re-scan
        if (nearestLog != null && client.world.getBlockState(nearestLog).isAir())
            nearestLog = null;
        if (nearestStone != null && client.world.getBlockState(nearestStone).isAir())
            nearestStone = null;

        // 1. FAST SCAN: 3x3 chunks every tick
        scanRegion(client, playerPos.getX() >> 4, playerPos.getZ() >> 4, 1);

        // 2. BACKGROUND SCAN: 2 chunks per tick
        for (int i = 0; i < 2; i++) {
            int cx = (playerPos.getX() >> 4) + scanIndexX;
            int cz = (playerPos.getZ() >> 4) + scanIndexZ;
            scanChunk(client, cx, cz);

            scanIndexZ++;
            if (scanIndexZ > SCAN_RADIUS_CHUNKS) {
                scanIndexZ = -SCAN_RADIUS_CHUNKS;
                scanIndexX++;
            }
            if (scanIndexX > SCAN_RADIUS_CHUNKS) {
                scanIndexX = -SCAN_RADIUS_CHUNKS;
            }
        }

        scanEntities(client);
    }

    private static void scanRegion(MinecraftClient client, int chunkX, int chunkZ, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                scanChunk(client, chunkX + x, chunkZ + z);
            }
        }
    }

    private static void scanChunk(MinecraftClient client, int cx, int cz) {
        if (!client.world.getChunkManager().isChunkLoaded(cx, cz))
            return;

        WorldChunk chunk = client.world.getChunk(cx, cz);
        BlockPos playerPos = client.player.getBlockPos();

        int pY = playerPos.getY();
        int minY = Math.max(0, pY - 10);
        int maxY = Math.min(255, pY + 15);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    BlockPos pos = new BlockPos((cx << 4) + x, y, (cz << 4) + z);
                    Block block = chunk.getBlockState(pos).getBlock();

                    double distSq = pos.getSquaredDistance(playerPos);

                    if (isLog(block)) {
                        if (nearestLog == null || distSq < nearestLog.getSquaredDistance(playerPos))
                            nearestLog = pos;
                    } else if (block == Blocks.IRON_ORE) {
                        if (nearestIron == null || distSq < nearestIron.getSquaredDistance(playerPos))
                            nearestIron = pos;
                    } else if (block == Blocks.STONE || block == Blocks.COBBLESTONE) {
                        // Prioritize exposed stone
                        boolean exposed = isExposed(chunk, x, y, z);
                        if (exposed) {
                            if (nearestStone == null || distSq < nearestStone.getSquaredDistance(playerPos))
                                nearestStone = pos;
                        }
                    }
                }
            }
        }
    }

    private static void scanEntities(MinecraftClient client) {
        nearestGolem = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity e : client.world.getEntities()) {
            if (e instanceof IronGolemEntity) {
                double d = e.squaredDistanceTo(client.player);
                if (d < closestDist) {
                    closestDist = d;
                    nearestGolem = e;
                }
            }
        }
    }

    private static boolean isExposed(WorldChunk chunk, int x, int y, int z) {
        if (y < 255) {
            // Simple check: Is the block above air?
            // Since we are inside chunk, we can check directly if y+1 is 16.
            // But simplest is to trust the world if loaded, or chunk if not.
            // Chunk relative coords are x=0..15, z=0..15.
            // Safe way:
            return !chunk.getBlockState(new BlockPos((chunk.getPos().x << 4) + x, y + 1, (chunk.getPos().z << 4) + z))
                    .getMaterial().isSolid();
        }
        return true;
    }

    private static boolean isLog(Block b) {
        return b == Blocks.OAK_LOG || b == Blocks.BIRCH_LOG || b == Blocks.SPRUCE_LOG || b == Blocks.ACACIA_LOG ||
                b == Blocks.JUNGLE_LOG || b == Blocks.DARK_OAK_LOG || b == Blocks.ACACIA_LOG;
    }

    public static BlockPos getNearestLog() {
        return nearestLog;
    }

    public static BlockPos getNearestIron() {
        return nearestIron;
    }

    public static BlockPos getNearestStone() {
        return nearestStone;
    }

    public static Entity getNearestGolem() {
        return nearestGolem;
    }

    public static BlockPos findNearestTable(MinecraftClient client) {
        if (client.world == null || client.player == null)
            return null;
        BlockPos playerPos = client.player.getBlockPos();
        int cx = playerPos.getX() >> 4;
        int cz = playerPos.getZ() >> 4;

        BlockPos found = null;
        double minDst = Double.MAX_VALUE;

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                WorldChunk chunk = client.world.getChunk(cx + x, cz + z);
                // Scan chunk for table (Simplified: scan known surface Ys)
                // In a real implementation, we'd cache this like ores.
                // For now, let's scan a small radius around player instead of full chunk.
            }
        }
        // Fallback: Local Scan
        for (int x = -5; x <= 5; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (client.world.getBlockState(pos).getBlock() == Blocks.CRAFTING_TABLE) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    public static void invalidateCache() {
        nearestLog = null;
        nearestIron = null;
        nearestStone = null;
        scanIndexX = -SCAN_RADIUS_CHUNKS;
    }
}
