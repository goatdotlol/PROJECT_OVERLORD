package com.speedrun.bot.perception;

import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.tag.BlockTags;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class WorldScanner {

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

    public static String getCurrentBiomeType(MinecraftClient client) {
        if (client.player == null || client.world == null)
            return "UNKNOWN";
        Biome biome = client.world.getBiome(client.player.getBlockPos());
        return biome.getCategory().getName().toUpperCase();
    }

    public static boolean isInOcean(MinecraftClient client) {
        String biome = getCurrentBiomeType(client);
        return biome.contains("OCEAN") || biome.contains("BEACH") || biome.contains("RIVER");
    }

    public static BlockPos findNearestBlock(Block targetBlock, int radius) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return null;

        BlockPos playerPos = client.player.getBlockPos();
        BlockPos nearest = null;
        double minDstSq = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (client.world.getBlockState(pos).getBlock() == targetBlock) {
                        double dstSq = pos.getSquaredDistance(playerPos);
                        if (dstSq < minDstSq) {
                            minDstSq = dstSq;
                            nearest = pos;
                        }
                    }
                }
            }
        }
        return nearest;
    }

    /**
     * Finds the nearest log (tree).
     */
    public static BlockPos findTree(int radius) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return null;

        BlockPos playerPos = client.player.getBlockPos();
        BlockPos nearest = null;
        double minDstSq = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (client.world.getBlockState(pos).getBlock().isIn(BlockTags.LOGS)) {
                        double dstSq = pos.getSquaredDistance(playerPos);
                        if (dstSq < minDstSq) {
                            minDstSq = dstSq;
                            nearest = pos;
                        }
                    }
                }
            }
        }
        return nearest;
    }

    /**
     * Finds structure with verification.
     */
    public static ScanResult findShipwreck(int radius) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return null;

        for (Block indicator : SHIPWRECK_INDICATORS) {
            BlockPos found = findNearestBlock(indicator, radius);
            if (found != null && found.getY() < 64) {
                return new ScanResult("SHIPWRECK", found, null);
            }
        }
        return null;
    }

    /**
     * Finds village with verification.
     */
    public static ScanResult findVillage(int radius) {
        BlockPos bell = findNearestBlock(Blocks.BELL, radius);
        if (bell != null)
            return new ScanResult("VILLAGE_BELL", bell, null);

        for (Block indicator : VILLAGE_INDICATORS) {
            BlockPos found = findNearestBlock(indicator, radius);
            if (found != null && !isInOcean(MinecraftClient.getInstance())) {
                return new ScanResult("VILLAGE_PIECE", found, null);
            }
        }
        return null;
    }

    public static BlockPos findLavaPool(int radius) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return null;

        BlockPos playerPos = client.player.getBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -10; y <= 20; y++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = client.world.getBlockState(pos).getBlock();
                    if (block == Blocks.LAVA) {
                        if (client.world.getBlockState(pos.east()).getBlock() == Blocks.LAVA ||
                                client.world.getBlockState(pos.west()).getBlock() == Blocks.LAVA) {
                            return pos;
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

    public static Entity findVillager(double radius) {
        List<Entity> villagers = getNearbyEntities(EntityType.VILLAGER, radius);
        return villagers.isEmpty() ? null : villagers.get(0);
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
