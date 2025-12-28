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

    public static void tick(MinecraftClient client) {
        if (!active || client.player == null)
            return;

        // 1. Determine current goal based on inventory
        updateGoal();

        // 2. Execute goal logic
        switch (currentGoal) {
            case GET_WOOD:
                executeGetWood(client);
                break;
            case GET_IRON:
                executeGetIron(client);
                break;
            case GET_LAVA:
                executeGetLava(client);
                break;
            default:
                break;
        }
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

    private static void executeGetWood(MinecraftClient client) {
        if (InteractionManager.isInteracting())
            return;

        BlockPos tree = WorldScanner.findTree(40);
        if (tree != null) {
            setTarget(tree, null, "WOOD_LOG");
            double distSq = client.player.getBlockPos().getSquaredDistance(tree);

            if (distSq < 4.0) {
                // Close enough to break
                InteractionManager.breakBlock(tree, 60); // approx 3 seconds for fist
            }
        } else {
            DebugLogger.log("[Auto] No trees in range. Travel further!");
            // Auto-travel logic could be added here
        }
    }

    private static void executeGetIron(MinecraftClient client) {
        // Priority: Golem > Village > Shipwreck > Iron Ore
        Entity golem = WorldScanner.findIronGolem(80);
        if (golem != null) {
            setTarget(null, golem, "IRON_GOLEM");
            // If close, we'd fight. MovementManager handles walking to it.
            return;
        }

        WorldScanner.ScanResult village = WorldScanner.findVillage(80);
        if (village != null) {
            setTarget(village.blockPos, null, village.type);
            return;
        }

        BlockPos iron = WorldScanner.findIronOre(40);
        if (iron != null) {
            setTarget(iron, null, "IRON_ORE");
            double distSq = client.player.getBlockPos().getSquaredDistance(iron);
            if (distSq < 4.0) {
                // Check if we have pickaxe first!
                if (InventoryScanner.hasPickaxe()) {
                    InteractionManager.breakBlock(iron, 40);
                } else {
                    DebugLogger.log("[Auto] Need a pickaxe to mine iron!");
                }
            }
        }
    }

    private static void executeGetLava(MinecraftClient client) {
        BlockPos lava = WorldScanner.findLavaPool(100);
        if (lava != null) {
            setTarget(lava, null, "LAVA_POOL");
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
