package com.speedrun.bot.perception;

import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class WorldScanner {

    // Village indicator blocks - unique to villages!
    private static final Block[] VILLAGE_INDICATORS = {
            Blocks.BELL, // Bell is UNIQUE to villages
            Blocks.HAY_BLOCK, // Common in villages
            Blocks.COMPOSTER, // Farmer workstation
            Blocks.LECTERN, // Librarian workstation
            Blocks.CARTOGRAPHY_TABLE,
            Blocks.SMITHING_TABLE,
            Blocks.FLETCHING_TABLE,
            Blocks.BREWING_STAND,
            Blocks.BARREL,
            Blocks.SMOKER,
            Blocks.BLAST_FURNACE,
            Blocks.GRINDSTONE,
            Blocks.STONECUTTER,
            Blocks.LOOM
    };

    /**
     * Scans for a specific block within a radius.
     * Uses a cubic search centered on the player.
     */
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
     * Scans for ANY village indicator blocks.
     * Returns the type of block found and its position.
     */
    public static ScanResult findVillageIndicator(int radius) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return null;

        BlockPos playerPos = client.player.getBlockPos();

        // Priority: Bell first (most reliable)
        BlockPos bell = findNearestBlock(Blocks.BELL, radius);
        if (bell != null) {
            return new ScanResult("BELL", bell, null);
        }

        // Then check for any village workstation
        for (Block indicator : VILLAGE_INDICATORS) {
            BlockPos found = findNearestBlock(indicator, radius);
            if (found != null) {
                return new ScanResult(indicator.getName().getString().toUpperCase(), found, null);
            }
        }

        return null;
    }

    /**
     * Efficiently finds nearby entities of a specific type.
     */
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

    /**
     * Find Iron Golem specifically.
     */
    public static Entity findIronGolem(double radius) {
        List<Entity> golems = getNearbyEntities(EntityType.IRON_GOLEM, radius);
        return golems.isEmpty() ? null : golems.get(0);
    }

    /**
     * Find Villager specifically.
     */
    public static Entity findVillager(double radius) {
        List<Entity> villagers = getNearbyEntities(EntityType.VILLAGER, radius);
        return villagers.isEmpty() ? null : villagers.get(0);
    }

    /**
     * Find Iron Ore.
     */
    public static BlockPos findIronOre(int radius) {
        return findNearestBlock(Blocks.IRON_ORE, radius);
    }

    /**
     * Find Chest.
     */
    public static BlockPos findChest(int radius) {
        return findNearestBlock(Blocks.CHEST, radius);
    }

    public static boolean isSolid(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null)
            return false;
        BlockState state = client.world.getBlockState(pos);
        return state.getMaterial().isSolid();
    }

    /**
     * Result holder for scans.
     */
    public static class ScanResult {
        public final String type;
        public final BlockPos blockPos;
        public final Entity entity;

        public ScanResult(String type, BlockPos blockPos, Entity entity) {
            this.type = type;
            this.blockPos = blockPos;
            this.entity = entity;
        }

        public String getCoords() {
            if (blockPos != null) {
                return "(" + blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ() + ")";
            } else if (entity != null) {
                return "(" + (int) entity.getX() + ", " + (int) entity.getY() + ", " + (int) entity.getZ() + ")";
            }
            return "(unknown)";
        }
    }
}
