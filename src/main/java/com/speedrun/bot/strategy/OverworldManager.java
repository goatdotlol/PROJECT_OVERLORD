package com.speedrun.bot.strategy;

import com.speedrun.bot.utils.DebugLogger;
import com.speedrun.bot.perception.WorldScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public class OverworldManager {
    public enum State {
        IDLE,
        SCANNING, // Unified scanning for all targets
        NETHER_RUSH, // Searching for Lava pools
        TARGET_FOUND // Target identified, waiting for movement
    }

    private static State currentState = State.IDLE;
    private static boolean active = false;
    private static Entity targetEntity;
    private static BlockPos targetPos;
    private static String targetType = "";

    private static int scanCooldown = 0;
    private static final int SCAN_INTERVAL = 20;

    public static void tick(MinecraftClient client) {
        if (!active || client.player == null)
            return;

        if (scanCooldown > 0) {
            scanCooldown--;
            return;
        }
        scanCooldown = SCAN_INTERVAL;

        switch (currentState) {
            case IDLE:
                currentState = State.SCANNING;
                DebugLogger.log("[Ghost] Strategy Started: SCANNING...");
                break;

            case SCANNING:
                unifiedScan(client);
                break;

            case NETHER_RUSH:
                scanForLava(client);
                break;

            case TARGET_FOUND:
                // TODO: LEGS movement would initiate here
                break;
        }
    }

    /**
     * Scans for everything at once, prioritizing based on SPEEDRUN efficiency.
     */
    private static void unifiedScan(MinecraftClient client) {
        // Priority 1: Iron Golem (fastest iron)
        Entity golem = WorldScanner.findIronGolem(100);
        if (golem != null) {
            targetFound(client, golem, null, "IRON_GOLEM");
            return;
        }

        // Priority 2: Village (indicators / villagers)
        WorldScanner.ScanResult village = WorldScanner.findVillage(100);
        if (village != null) {
            targetFound(client, null, village.blockPos, village.type);
            return;
        }

        // Priority 3: Shipwreck (only structure-verified ones)
        WorldScanner.ScanResult shipwreck = WorldScanner.findShipwreck(80);
        if (shipwreck != null) {
            targetFound(client, null, shipwreck.blockPos, shipwreck.type);
            return;
        }

        // Priority 4: Surface Iron Ore
        BlockPos iron = WorldScanner.findIronOre(40);
        if (iron != null) {
            targetFound(client, null, iron, "IRON_ORE");
            return;
        }

        DebugLogger.log("[Ghost] Scanning... No high-value structures in range.");
    }

    /**
     * "Nether Rush" state: Scans for Lava pools.
     */
    private static void scanForLava(MinecraftClient client) {
        BlockPos lava = WorldScanner.findLavaPool(100);
        if (lava != null) {
            targetFound(client, null, lava, "LAVA_POOL");
            return;
        }
        DebugLogger.log("[Nether] Searching for Lava Pools...");
    }

    private static void targetFound(MinecraftClient client, Entity entity, BlockPos pos, String type) {
        targetEntity = entity;
        targetPos = pos;
        targetType = type;

        int x = (entity != null) ? (int) entity.getX() : pos.getX();
        int y = (entity != null) ? (int) entity.getY() : pos.getY();
        int z = (entity != null) ? (int) entity.getZ() : pos.getZ();

        DebugLogger.log("[FOUND] " + type + " at (" + x + ", " + y + ", " + z + ")");
        currentState = State.TARGET_FOUND;
    }

    public static void toggle() {
        active = !active;
        if (active) {
            currentState = State.IDLE;
            DebugLogger.log("[Ghost] 7 Sexy Iron: ENABLED");
        } else {
            active = false;
            DebugLogger.log("[Ghost] 7 Sexy Iron: DISABLED");
        }
    }

    public static void startNetherRush() {
        active = true;
        currentState = State.NETHER_RUSH;
        DebugLogger.log("[Ghost] Nether Rush: ENABLED");
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
