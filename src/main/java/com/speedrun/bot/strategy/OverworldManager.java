package com.speedrun.bot.strategy;

import com.speedrun.bot.utils.DebugLogger;
import com.speedrun.bot.perception.WorldScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

/**
 * OverworldManager - Purely for Scanning and ESP information.
 * Does NOT trigger autonomous movement (handled by AutoSpeedrunManager).
 */
public class OverworldManager {
    public enum State {
        IDLE,
        SCANNING
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

        unifiedScan(client);
    }

    private static void unifiedScan(MinecraftClient client) {
        Entity golem = WorldScanner.findIronGolem(100);
        if (golem != null) {
            updateInfo(golem, null, "IRON_GOLEM");
            return;
        }

        WorldScanner.ScanResult village = WorldScanner.findVillage(100);
        if (village != null) {
            updateInfo(null, village.blockPos, village.type);
            return;
        }

        WorldScanner.ScanResult shipwreck = WorldScanner.findShipwreck(80);
        if (shipwreck != null) {
            updateInfo(null, shipwreck.blockPos, shipwreck.type);
            return;
        }

        BlockPos iron = WorldScanner.findIronOre(40);
        if (iron != null) {
            updateInfo(null, iron, "IRON_ORE");
            return;
        }

        BlockPos lava = WorldScanner.findLavaPool(100);
        if (lava != null) {
            updateInfo(null, lava, "LAVA_POOL");
            return;
        }

        updateInfo(null, null, "");
    }

    private static void updateInfo(Entity entity, BlockPos pos, String type) {
        targetEntity = entity;
        targetPos = pos;
        targetType = type;
    }

    public static void toggle() {
        active = !active;
        if (active) {
            currentState = State.IDLE;
            DebugLogger.log("[Ghost] 7 Sexy Iron (Passive Scan): ENABLED");
        } else {
            active = false;
            DebugLogger.log("[Ghost] 7 Sexy Iron (Passive Scan): DISABLED");
        }
    }

    public static boolean isActive() {
        return active;
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
