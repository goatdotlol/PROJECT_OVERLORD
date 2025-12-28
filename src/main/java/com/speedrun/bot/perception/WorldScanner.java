package com.speedrun.bot.perception;

import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class WorldScanner {
    
    /**
     * Scans for a specific block within a radius.
     * Uses a cubic search centered on the player.
     * WARNING: Large radius (>30) will cause lag on main thread.
     */
    public static BlockPos findNearestBlock(Block targetBlock, int radius) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return null;
        
        BlockPos playerPos = client.player.getBlockPos();
        BlockPos nearest = null;
        double minDstSq = Double.MAX_VALUE;
        
        // Optimization: Spiral search would be better to return early
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
     * Efficiently finds nearby entities of a specific type.
     */
    public static List<Entity> getNearbyEntities(EntityType<?> type, double radius) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<Entity> result = new ArrayList<>();
        if (client.player == null || client.world == null) return result;
        
        // ClientWorld.getEntities() gives all loaded entities
        for (Entity entity : client.world.getEntities()) {
            if (entity.getType() == type && entity.squaredDistanceTo(client.player) <= radius * radius) {
                result.add(entity);
            }
        }
        
        // Sort by distance (closest first)
        result.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(client.player)));
        return result;
    }

    public static boolean isSolid(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;
        BlockState state = client.world.getBlockState(pos);
        return state.getMaterial().isSolid();
    }
}
