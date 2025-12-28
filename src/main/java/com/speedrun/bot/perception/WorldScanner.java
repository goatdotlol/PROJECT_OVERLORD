package com.speedrun.bot.perception;

import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.tag.BlockTags;
import java.util.*;

/**
 * WorldScanner - Optimized with Caching and Radial Search.
 * Fixes the severe lag by preventing 500k block loops every tick.
 */
public class WorldScanner {

    // Cache for slow-changing targets
    private static final Map<String, BlockPos> blockCache = new HashMap<>();
    private static long lastCacheFlush = 0;
    private static final long CACHE_EXPIRY = 2000; // 2 seconds

    // Village indicator blocks
    private static final Block[] VILLAGE_INDICATORS = {
            Blocks.BELL, Blocks.HAY_BLOCK, Blocks.COMPOSTER, Blocks.LECTERN,
            Blocks.CARTOGRAPHY_TABLE, Blocks.SMITHING_TABLE, Blocks.FLETCHING_TABLE,
            Blocks.BREWING_STAND, Blocks.BARREL, Blocks.SMOKER, Blocks.BLAST_FURNACE,
            Blocks.GRINDSTONE, Blocks.STONECUTTER, Blocks.LOOM
    };

    // Shipwreck indicators
    private static final Block[] SHIPWRECK_INDICATORS = {
            Blocks.OAK_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR, Blocks.BIRCH_TRAPDOOR,
            Blocks.JUNGLE_TRAPDOOR, Blocks.ACACIA_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR,
            Blocks.OAK_FENCE, Blocks.SPRUCE_FENCE, Blocks.BIRCH_FENCE,
            Blocks.JUNGLE_FENCE, Blocks.ACACIA_FENCE, Blocks.DARK_OAK_FENCE
    };

    public static void clearCache() {
        blockCache.clear();
    }

    private static boolean isCacheValid(String key) {
        if (System.currentTimeMillis() - lastCacheFlush > CACHE_EXPIRY) {
            blockCache.clear();
            lastCacheFlush = System.currentTimeMillis();
            return false;
        }
        return blockCache.containsKey(key);
    }

    public static BlockPos findNearestBlock(Block targetBlock, int radius) {
        String cacheKey = "BLOCK_" + targetBlock.getTranslationKey();
        if (isCacheValid(cacheKey))
            return blockCache.get(cacheKey);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return null;

        BlockPos playerPos = client.player.getBlockPos();
        // Radial search (outwards) is better but simple cube with early exit is usually
        // enough if we limit Y
        for (int r = 1; r <= radius; r++) {
            for (int y = -10; y <= 10; y++) { // Limit Y range drastically
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        if (Math.abs(x) != r && Math.abs(z) != r)
                            continue; // Only check the "shell"

                        BlockPos pos = playerPos.add(x, y, z);
                        if (client.world.getBlockState(pos).getBlock() == targetBlock) {
                            blockCache.put(cacheKey, pos);
                            return pos;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static BlockPos findTree(int radius) {
        String cacheKey = "TREE";
        if (isCacheValid(cacheKey))
            return blockCache.get(cacheKey);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return null;

        BlockPos playerPos = client.player.getBlockPos();
        // Limit search for trees to surface-ish levels
        for (int r = 1; r <= radius; r++) {
            for (int y = -5; y <= 15; y++) {
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        if (Math.abs(x) != r && Math.abs(z) != r)
                            continue;

                        BlockPos pos = playerPos.add(x, y, z);
                        if (client.world.getBlockState(pos).getBlock().isIn(BlockTags.LOGS)) {
                            blockCache.put(cacheKey, pos);
                            return pos;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static ScanResult findShipwreck(int radius) {
        for (Block indicator : SHIPWRECK_INDICATORS) {
            BlockPos found = findNearestBlock(indicator, radius);
            if (found != null && found.getY() < 64) {
                return new ScanResult("SHIPWRECK", found, null);
            }
        }
        return null;
    }

    public static ScanResult findVillage(int radius) {
        BlockPos bell = findNearestBlock(Blocks.BELL, radius);
        if (bell != null)
            return new ScanResult("VILLAGE_BELL", bell, null);

        for (Block indicator : VILLAGE_INDICATORS) {
            BlockPos found = findNearestBlock(indicator, radius);
            if (found != null) {
                return new ScanResult("VILLAGE_PIECE", found, null);
            }
        }
        return null;
    }

    public static BlockPos findLavaPool(int radius) {
        String cacheKey = "LAVA";
        if (isCacheValid(cacheKey))
            return blockCache.get(cacheKey);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return null;

        BlockPos playerPos = client.player.getBlockPos();
        for (int r = 1; r <= radius; r++) {
            for (int y = -10; y <= 10; y++) {
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        if (Math.abs(x) != r && Math.abs(z) != r)
                            continue;

                        BlockPos pos = playerPos.add(x, y, z);
                        if (client.world.getBlockState(pos).getBlock() == Blocks.LAVA) {
                            if (client.world.getBlockState(pos.east()).getBlock() == Blocks.LAVA) {
                                blockCache.put(cacheKey, pos);
                                return pos;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static List<Entity> getNearbyEntities(EntityType<?> type, double radius) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<Entity> result = new ArrayList<>();
        if (client.player == null || client.world == null)
            return result;
        for (Entity entity : client.world.getEntities()) {
            if (entity.getType() == type && entity.squaredDistanceTo(client.player) <= radius * radius) {
                result.add(entity);
            }
        }
        result.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(client.player)));
        return result;
    }

    public static Entity findIronGolem(double radius) {
        List<Entity> golems = getNearbyEntities(EntityType.IRON_GOLEM, radius);
        return golems.isEmpty() ? null : golems.get(0);
    }

    public static BlockPos findIronOre(int radius) {
        return findNearestBlock(Blocks.IRON_ORE, radius);
    }

    public static class ScanResult {
        public final String type;
        public final BlockPos blockPos;
        public final Entity entity;

        public ScanResult(String type, BlockPos blockPos, Entity entity) {
            this.type = type;
            this.blockPos = blockPos;
            this.entity = entity;
        }
    }
}
