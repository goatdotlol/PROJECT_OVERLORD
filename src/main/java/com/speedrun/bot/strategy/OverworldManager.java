package com.speedrun.bot.strategy;

import com.speedrun.bot.utils.DebugLogger;
import com.speedrun.bot.perception.WorldScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public class OverworldManager {
    public enum State {
        IDLE,
        SCANNING_FOR_VILLAGE,
        NAVIGATING_TO_VILLAGE,
        HUNTING_GOLEM,
        SCANNING_FOR_SHIPWRECK,
        LOOTING_SHIPWRECK,
        SCANNING_FOR_CAVES,
        MINING_IRON,
        TARGET_FOUND // Paused state when target is found
    }

    private static State currentState = State.IDLE;
    private static boolean active = false;
    private static Entity targetEntity;
    private static BlockPos targetPos;
    private static String targetType = "";

    // Throttle scanning to prevent spam (scan every 20 ticks = 1 second)
    private static int scanCooldown = 0;
    private static final int SCAN_INTERVAL = 20;

    public static void tick(MinecraftClient client) {
        if (!active || client.player == null)
            return;

        // Throttle scanning
        if (scanCooldown > 0) {
            scanCooldown--;
            return;
        }
        scanCooldown = SCAN_INTERVAL;

        switch (currentState) {
            case IDLE:
                startScanning(client);
                break;

            case SCANNING_FOR_VILLAGE:
                scanForVillage(client);
                break;

            case SCANNING_FOR_SHIPWRECK:
                scanForShipwreck(client);
                break;

            case SCANNING_FOR_CAVES:
                scanForCaves(client);
                break;

            case TARGET_FOUND:
                // Stay paused - target was found, waiting for pathfinding implementation
                break;

            default:
                break;
        }
    }

    private static void startScanning(MinecraftClient client) {
        DebugLogger.log("OverworldManager: Starting '7 Sexy Iron' Strategy");

        // Check biome to determine priority
        String biome = WorldScanner.getCurrentBiomeType(client);
        boolean isOcean = WorldScanner.isInOcean(client);

        DebugLogger.log("[Ghost] Starting scan... (Biome: " + biome + ")");

        if (isOcean) {
            // In ocean - prioritize shipwrecks
            DebugLogger.log("[Ghost] Ocean detected! Prioritizing shipwrecks...");
            transition(State.SCANNING_FOR_SHIPWRECK);
        } else {
            // On land - prioritize villages
            transition(State.SCANNING_FOR_VILLAGE);
        }
    }

    private static void scanForVillage(MinecraftClient client) {
        // Priority 1: Iron Golem (instant iron!)
        Entity golem = WorldScanner.findIronGolem(100);
        if (golem != null) {
            foundTarget(client, golem, null, "IRON_GOLEM", "IRON GOLEM");
            return;
        }

        // Priority 2: Villager (means village nearby)
        Entity villager = WorldScanner.findVillager(100);
        if (villager != null) {
            foundTarget(client, villager, null, "VILLAGER", "VILLAGER");
            return;
        }

        // Priority 3: Village indicator blocks (Bell, Hay, etc.)
        WorldScanner.ScanResult village = WorldScanner.findVillageIndicator(80);
        if (village != null) {
            foundTarget(client, null, village.blockPos, village.type, village.type);
            return;
        }

        // Nothing found - check biome before moving on
        if (WorldScanner.isInOcean(client)) {
            DebugLogger.log("[EYES] In ocean - checking for shipwrecks...");
            transition(State.SCANNING_FOR_SHIPWRECK);
        } else {
            DebugLogger.log("[EYES] No village nearby. Checking water for shipwrecks...");
            transition(State.SCANNING_FOR_SHIPWRECK);
        }
    }

    private static void scanForShipwreck(MinecraftClient client) {
        // Use smart shipwreck detection (not just chests!)
        WorldScanner.ScanResult shipwreck = WorldScanner.findShipwreckIndicator(60);
        if (shipwreck != null) {
            foundTarget(client, null, shipwreck.blockPos, shipwreck.type, shipwreck.type);
            return;
        }

        // Nothing found - move to cave scan
        DebugLogger.log("[EYES] No shipwreck structures. Scanning for iron ore...");
        transition(State.SCANNING_FOR_CAVES);
    }

    private static void scanForCaves(MinecraftClient client) {
        // Look for iron ore
        BlockPos iron = WorldScanner.findIronOre(40);
        if (iron != null) {
            foundTarget(client, null, iron, "IRON_ORE", "IRON ORE");
            return;
        }

        // Nothing found anywhere - notify user
        DebugLogger.log("[EYES] Nothing found. Move around to scan new chunks!");
        active = false; // Stop scanning to prevent spam
    }

    private static void foundTarget(MinecraftClient client, Entity entity, BlockPos pos, String type,
            String displayName) {
        targetEntity = entity;
        targetPos = pos;
        targetType = type;

        int x, y, z;
        int distance;

        if (entity != null) {
            x = (int) entity.getX();
            y = (int) entity.getY();
            z = (int) entity.getZ();
            distance = (int) Math.sqrt(entity.squaredDistanceTo(client.player));
        } else {
            x = pos.getX();
            y = pos.getY();
            z = pos.getZ();
            distance = (int) Math.sqrt(pos.getSquaredDistance(client.player.getBlockPos()));
        }

        DebugLogger.log("FOUND: " + type + " at (" + x + ", " + y + ", " + z + ")");
        DebugLogger.log("[EYES] " + displayName + " at (" + x + ", " + y + ", " + z + ") [" + distance
                + " blocks]");

        transition(State.TARGET_FOUND);
    }

    public static void toggle() {
        active = !active;
        MinecraftClient client = MinecraftClient.getInstance();

        if (active) {
            DebugLogger.log("Strategy ENABLED: 7 Sexy Iron");
            currentState = State.IDLE; // Always reset to start fresh
            targetEntity = null;
            targetPos = null;
            targetType = "";
            scanCooldown = 0;
            DebugLogger.log("[Ghost] 7 Sexy Iron: ON");
        } else {
            DebugLogger.log("Strategy DISABLED.");
            currentState = State.IDLE;
            DebugLogger.log("[Ghost] 7 Sexy Iron: OFF");
        }
    }

    /**
     * Resume scanning after target found.
     */
    public static void rescan() {
        if (currentState == State.TARGET_FOUND) {
            currentState = State.IDLE;
            targetEntity = null;
            targetPos = null;
            targetType = "";
            scanCooldown = 0;
            active = true;
        }
    }

    private static void transition(State newState) {
        DebugLogger.log("State Transition: " + currentState + " -> " + newState);
        currentState = newState;
    }

    public static boolean isActive() {
        return active;
    }

    public static State getState() {
        return currentState;
    }

    public static Entity getTargetEntity() {
        return targetEntity;
    }

    public static BlockPos getTargetPos() {
        return targetPos;
    }

    public static String getTargetType() {
        return targetType;
    }
}
