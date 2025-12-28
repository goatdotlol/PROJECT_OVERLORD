package com.speedrun.bot.strategy;

import com.speedrun.bot.utils.DebugLogger;
import net.minecraft.client.MinecraftClient;

public class OverworldManager {
    public enum State {
        IDLE,
        SCANNING_FOR_VILLAGE,
        NAVIGATING_TO_VILLAGE,
        HUNTING_GOLEM,
        SCANNING_FOR_SHIPWRECK,
        LOOTING_SHIPWRECK,
        SCANNING_FOR_CAVES,
        MINING_IRON
    }
    
    private static State currentState = State.IDLE;
    private static boolean active = false;
    private static net.minecraft.entity.Entity targetEntity;
    private static net.minecraft.util.math.BlockPos targetPos;

    public static void tick(MinecraftClient client) {
        if (!active || client.player == null) return;
        
        // Placeholder Logic for Phase 1 Verification
        switch (currentState) {
            case IDLE:
                DebugLogger.log("OverworldManager: Starting '7 Sexy Iron' Strategy");
                transition(State.SCANNING_FOR_VILLAGE);
                break;
                
            case SCANNING_FOR_VILLAGE:
                DebugLogger.log("Scan: Searching for Iron Golem or Village...");
                // Check for Golem first (Priority 1)
                net.minecraft.entity.Entity golem = com.speedrun.bot.perception.WorldScanner.getNearbyEntities(net.minecraft.entity.EntityType.IRON_GOLEM, 100).stream().findFirst().orElse(null);
                
                if (golem != null) {
                    DebugLogger.log("FOUND: Iron Golem at " + golem.getBlockPos());
                    targetEntity = golem;
                    transition(State.HUNTING_GOLEM);
                    return;
                }
                
                // Check for Villagers
                net.minecraft.entity.Entity villager = com.speedrun.bot.perception.WorldScanner.getNearbyEntities(net.minecraft.entity.EntityType.VILLAGER, 100).stream().findFirst().orElse(null);
                
                if (villager != null) {
                   DebugLogger.log("FOUND: Villager at " + villager.getBlockPos() + ". Assuming Village.");
                   // For now, just mark as found. In real logic, we pathfind there.
                   transition(State.NAVIGATING_TO_VILLAGE);
                   return;
                }

                DebugLogger.log("Scan Result: No Village/Golem found. Fallback to Shipwreck.");
                transition(State.SCANNING_FOR_SHIPWRECK);
                break;
                
            case SCANNING_FOR_SHIPWRECK:
                DebugLogger.log("Scan: Searching for Chests (Shipwreck/Treasure)...");
                // Look for Chests
                net.minecraft.util.math.BlockPos chest = com.speedrun.bot.perception.WorldScanner.findNearestBlock(net.minecraft.block.Blocks.CHEST, 50);
                
                if (chest != null) {
                     DebugLogger.log("FOUND: Chest at " + chest);
                     targetPos = chest;
                     transition(State.LOOTING_SHIPWRECK);
                     return;
                }
                
                DebugLogger.log("Scan Result: No Chests. Fallback to Caves.");
                transition(State.SCANNING_FOR_CAVES);
                break;
                
            case SCANNING_FOR_CAVES:
                DebugLogger.log("Scan: Looking for Iron Ore...");
                active = false; // Stop for now to prevent spam
                break;
                
             default:
                break;
        }
    }
    
    public static void toggle() {
        active = !active;
        if (active) {
            DebugLogger.log("Strategy ENABLED: 7 Sexy Iron");
            if (currentState == State.IDLE) currentState = State.IDLE; // Reset if needed
        } else {
            DebugLogger.log("Strategy DISABLED.");
            currentState = State.IDLE;
        }
        
        // Chat feedback
        MinecraftClient.getInstance().player.sendChatMessage("[Ghost] 7 Sexy Iron: " + (active ? "ON" : "OFF"));
    }
    
    private static void transition(State newState) {
        DebugLogger.log("State Transition: " + currentState + " -> " + newState);
        currentState = newState;
    }
    
    public static boolean isActive() {
        return active;
    }
}
