package com.speedrun.bot.strategy;

import com.speedrun.bot.utils.DebugLogger;
import com.speedrun.bot.utils.InventoryScanner;
import com.speedrun.bot.perception.WorldScanner;
import com.speedrun.bot.navigation.MovementManager;
import com.speedrun.bot.input.InteractionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;

public class AutoSpeedrunManager {

    public enum Goal {
        IDLE,
        GET_WOOD,
        CRAFT_BASIC_TOOLS,
        GET_IRON,
        GET_LAVA,
        DONE
    }

    private static Goal currentGoal = Goal.IDLE;
    private static boolean active = false;

    private static BlockPos currentTargetPos = null;
    private static Entity currentTargetEntity = null;
    private static String currentTargetType = "";

    private static int scanCooldown = 0;
    private static final int SCAN_INTERVAL = 20; // Once per second

    public static void tick(MinecraftClient client) {
        if (!active || client.player == null)
            return;

        if (scanCooldown > 0) {
            scanCooldown--;
        } else {
            scanCooldown = SCAN_INTERVAL;
            // 1. Determine current goal based on inventory
            updateGoal();

            // 2. Refresh target selection (heavy lifting)
            refreshTarget(client);
        }

        // 3. Constant logic (movement and interaction)
        executeCurrentTarget(client);
    }

    private static void updateGoal() {
        if (!InventoryScanner.hasWood() && InventoryScanner.getIronCount() < 3) {
            currentGoal = Goal.GET_WOOD;
        } else if (InventoryScanner.getIronCount() < 7) {
            currentGoal = Goal.GET_IRON;
        } else {
            currentGoal = Goal.GET_LAVA;
        }
    }

    private static void refreshTarget(MinecraftClient client) {
        switch (currentGoal) {
            case GET_WOOD:
                BlockPos tree = WorldScanner.findTree(40);
                if (tree != null) {
                    setTarget(tree, null, "WOOD_LOG");
                }
                break;
            case GET_IRON:
                Entity golem = WorldScanner.findIronGolem(80);
                if (golem != null) {
                    setTarget(null, golem, "IRON_GOLEM");
                } else {
                    WorldScanner.ScanResult village = WorldScanner.findVillage(80);
                    if (village != null) {
                        setTarget(village.blockPos, null, village.type);
                    } else {
                        BlockPos iron = WorldScanner.findIronOre(40);
                        if (iron != null) {
                            setTarget(iron, null, "IRON_ORE");
                        }
                    }
                }
                break;
            case GET_LAVA:
                BlockPos lava = WorldScanner.findLavaPool(100);
                if (lava != null) {
                    setTarget(lava, null, "LAVA_POOL");
                }
                break;
            default:
                break;
        }
    }

    private static void executeCurrentTarget(MinecraftClient client) {
        if (InteractionManager.isInteracting() || currentTargetPos == null && currentTargetEntity == null)
            return;

        BlockPos target = currentTargetPos;
        if (currentTargetEntity != null)
            target = currentTargetEntity.getBlockPos();

        double distSq = client.player.getBlockPos().getSquaredDistance(target);

        // If we are close enough to the target block, try to interact/break
        if (distSq < 5.0) {
            if (currentTargetType.equals("WOOD_LOG")) {
                InteractionManager.breakBlock(target, 60);
            } else if (currentTargetType.equals("IRON_ORE") && InventoryScanner.hasPickaxe()) {
                InteractionManager.breakBlock(target, 40);
            }
        }
    }

    private static void setTarget(BlockPos pos, Entity entity, String type) {
        currentTargetPos = pos;
        currentTargetEntity = entity;
        currentTargetType = type;
    }

    public static void start() {
        active = true;
        currentGoal = Goal.IDLE;
        scanCooldown = 0;
        DebugLogger.log("[Auto] Autonomous Speedrun ENABLED.");
    }

    public static void stop() {
        active = false;
        currentGoal = Goal.IDLE;
        DebugLogger.log("[Auto] Autonomous Speedrun DISABLED.");
    }

    public static boolean isActive() {
        return active;
    }

    public static Goal getGoal() {
        return currentGoal;
    }

    public static BlockPos getTargetPos() {
        return currentTargetPos;
    }

    public static Entity getTargetEntity() {
        return currentTargetEntity;
    }

    public static String getTargetType() {
        return currentTargetType;
    }
}
