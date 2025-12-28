package com.speedrun.bot.perception;

import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
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

    // Shipwreck indicator blocks - waterlogged wood structures underwater
    private static final Block[] SHIPWRECK_INDICATORS = {
            // Trapdoors (all wood types used in shipwrecks)
            Blocks.OAK_TRAPDOOR,
            Blocks.SPRUCE_TRAPDOOR,
            Blocks.BIRCH_TRAPDOOR,
            Blocks.JUNGLE_TRAPDOOR,
            Blocks.ACACIA_TRAPDOOR,
            Blocks.DARK_OAK_TRAPDOOR,
            // Fences (ship railings)
            Blocks.OAK_FENCE,
            Blocks.SPRUCE_FENCE,
            Blocks.BIRCH_FENCE,
            Blocks.JUNGLE_FENCE,
            Blocks.ACACIA_FENCE,
            Blocks.DARK_OAK_FENCE,
            // Logs (ship hull)
            Blocks.OAK_LOG,
            Blocks.SPRUCE_LOG,
            Blocks.STRIPPED_OAK_LOG,
            Blocks.STRIPPED_SPRUCE_LOG,
            // Planks (ship deck)
            Blocks.OAK_PLANKS,
            Blocks.SPRUCE_PLANKS,
            Blocks.DARK_OAK_PLANKS
    };

    /**
     * Gets current biome category for smart scanning.
     */
    public static String getCurrentBiomeType(MinecraftClient client) {
        if (client.player == null || client.world == null)
            return "UNKNOWN";

        BlockPos pos = client.player.getBlockPos();
        Biome biome = client.world.getBiome(pos);
        Biome.Category category = biome.getCategory();

        return category.getName().toUpperCase();
    }

    /**
     * Check if player is in/near ocean.
     */
    public static boolean isInOcean(MinecraftClient client) {
        String biome = getCurrentBiomeType(client);
        return biome.contains("OCEAN") || biome.contains("BEACH") || biome.contains("RIVER");
    }

    /**
     * Check if player is in plains/forest (village territory).
     */
    public static boolean isInVillageTerritory(MinecraftClient client) {
        String biome = getCurrentBiomeType(client);
        return biome.contains("PLAINS") || biome.contains("SAVANNA") ||
                biome.contains("DESERT") || biome.contains("TAIGA") ||
                biome.contains("SNOWY");
    }

    /**
     * Scans for a specific block within a radius.
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
     */
    public static ScanResult findVillageIndicator(int radius) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return null;

        // Priority: Bell first (most reliable)
        BlockPos bell = findNearestBlock(Blocks.BELL, radius);
        if (bell != null) {
            return new ScanResult("BELL (Village!)", bell, null);
        }

        // Then check for any village workstation
        for (Block indicator : VILLAGE_INDICATORS) {
            BlockPos found = findNearestBlock(indicator, radius);
            if (found != null) {
                String name = indicator.toString().replace("Block{", "").replace("}", "").toUpperCase();
                return new ScanResult(name + " (Village)", found, null);
            }
        }

        return null;
    }

    /**
     * Scans for shipwreck indicators (underwater wood structures).
     * More reliable than just finding chests!
     */
    public static ScanResult findShipwreckIndicator(int radius) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return null;

        // Look for underwater wood structures (trapdoors, fences are most unique)
        for (Block indicator : SHIPWRECK_INDICATORS) {
            BlockPos found = findNearestBlock(indicator, radius);
            if (found != null) {
                // Verify it's underwater (shipwrecks are always underwater)
                BlockState aboveState = client.world.getBlockState(found.up());
                if (aboveState.getBlock() == Blocks.WATER || found.getY() < 62) {
                    String name = indicator.toString().replace("Block{", "").replace("}", "").toUpperCase();
                    return new ScanResult(name + " (Shipwreck?)", found, null);
                }
            }
        }

        // Fallback: chest underwater
        BlockPos chest = findNearestBlock(Blocks.CHEST, radius);
        if (chest != null && chest.getY() < 62) {
            return new ScanResult("CHEST (Underwater)", chest, null);
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
