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
                DebugLogger.log("OverworldManager: Starting '7 Sexy Iron' Strategy");
                sendChat(client, "§a[Ghost] Starting scan for resources...");
                transition(State.SCANNING_FOR_VILLAGE);
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

    private static void scanForVillage(MinecraftClient client) {
        // Priority 1: Iron Golem (instant iron!)
        Entity golem = WorldScanner.findIronGolem(100);
        if (golem != null) {
            targetEntity = golem;
            targetType = "IRON_GOLEM";
            int x = (int) golem.getX();
            int y = (int) golem.getY();
            int z = (int) golem.getZ();
            DebugLogger.log("FOUND: Iron Golem at (" + x + ", " + y + ", " + z + ")");
            sendChat(client, "§a§l[EYES] §r§6IRON GOLEM §fat (" + x + ", " + y + ", " + z + ") §7- Distance: "
                    + (int) Math.sqrt(golem.squaredDistanceTo(client.player)) + " blocks");
            transition(State.TARGET_FOUND);
            return;
        }

        // Priority 2: Villager (means village nearby)
        Entity villager = WorldScanner.findVillager(100);
        if (villager != null) {
            targetEntity = villager;
            targetType = "VILLAGER";
            int x = (int) villager.getX();
            int y = (int) villager.getY();
            int z = (int) villager.getZ();
            DebugLogger.log("FOUND: Villager at (" + x + ", " + y + ", " + z + ")");
            sendChat(client, "§a§l[EYES] §r§eVILLAGER §fat (" + x + ", " + y + ", " + z + ") §7- Village nearby!");
            transition(State.TARGET_FOUND);
            return;
        }

        // Priority 3: Village indicator blocks (Bell, Hay, etc.)
        WorldScanner.ScanResult village = WorldScanner.findVillageIndicator(80);
        if (village != null) {
            targetPos = village.blockPos;
            targetType = village.type;
            DebugLogger.log("FOUND: " + village.type + " at " + village.getCoords());
            sendChat(client,
                    "§a§l[EYES] §r§b" + village.type + " §fat " + village.getCoords() + " §7- Village indicator!");
            transition(State.TARGET_FOUND);
            return;
        }

        // Nothing found - move to shipwreck scan
        DebugLogger.log("Scan: No village found. Checking for shipwrecks...");
        sendChat(client, "§7[EYES] No village nearby. Scanning for shipwrecks...");
        transition(State.SCANNING_FOR_SHIPWRECK);
    }

    private static void scanForShipwreck(MinecraftClient client) {
        // Look for chests
        BlockPos chest = WorldScanner.findChest(60);
        if (chest != null) {
            targetPos = chest;
            targetType = "CHEST";
            DebugLogger.log("FOUND: Chest at (" + chest.getX() + ", " + chest.getY() + ", " + chest.getZ() + ")");
            sendChat(client,
                    "§a§l[EYES] §r§dCHEST §fat (" + chest.getX() + ", " + chest.getY() + ", " + chest.getZ() + ")");
            transition(State.TARGET_FOUND);
            return;
        }

        // Nothing found - move to cave scan
        DebugLogger.log("Scan: No chests found. Checking for iron ore...");
        sendChat(client, "§7[EYES] No chests nearby. Scanning for iron ore...");
        transition(State.SCANNING_FOR_CAVES);
    }

    private static void scanForCaves(MinecraftClient client) {
        // Look for iron ore
        BlockPos iron = WorldScanner.findIronOre(40);
        if (iron != null) {
            targetPos = iron;
            targetType = "IRON_ORE";
            DebugLogger.log("FOUND: Iron Ore at (" + iron.getX() + ", " + iron.getY() + ", " + iron.getZ() + ")");
            sendChat(client,
                    "§a§l[EYES] §r§fIRON ORE §fat (" + iron.getX() + ", " + iron.getY() + ", " + iron.getZ() + ")");
            transition(State.TARGET_FOUND);
            return;
        }

        // Nothing found anywhere - notify user
        DebugLogger.log("Scan: Nothing found. Waiting for player to move...");
        sendChat(client, "§c[EYES] Nothing found nearby. Walk around to scan new areas.");
        active = false; // Stop scanning to prevent spam
    }

    private static void sendChat(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendChatMessage(message);
        }
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
            if (client.player != null) {
                client.player.sendChatMessage("§a[Ghost] 7 Sexy Iron: §lON §r§7- Starting scan...");
            }
        } else {
            DebugLogger.log("Strategy DISABLED.");
            currentState = State.IDLE;
            if (client.player != null) {
                client.player.sendChatMessage("§c[Ghost] 7 Sexy Iron: §lOFF");
            }
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
