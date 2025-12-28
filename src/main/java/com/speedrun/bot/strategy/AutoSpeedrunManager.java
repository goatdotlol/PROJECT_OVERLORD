package com.speedrun.bot.strategy;

import com.speedrun.bot.utils.DebugLogger;
import com.speedrun.bot.utils.InventoryScanner;
import com.speedrun.bot.perception.DistributedScanner;
import com.speedrun.bot.navigation.MovementManager;
import com.speedrun.bot.input.InteractionManager;
import com.speedrun.bot.input.InputSimulator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class AutoSpeedrunManager {

    public enum Goal {
        IDLE,
        GET_WOOD,
        CRAFT_TOOLS,
        GET_IRON,
        GET_LAVA,
        WANDER
    }

    private static Goal currentGoal = Goal.IDLE;
    private static boolean active = false;

    private static BlockPos currentTargetPos = null;
    private static Entity currentTargetEntity = null;
    private static String currentTargetType = "";

    private static int wanderCooldown = 0;
    private static final Random random = new Random();

    public static void tick(MinecraftClient client) {
        if (!active || client.player == null)
            return;

        // 1. Tick Perception Engine (Zero Lag)
        DistributedScanner.tick(client);

        // 2. Decide overall goal
        updateGoal();

        // 3. Update target based on goal
        updateTargetSelection(client);

        // 4. Execution logic (Movement/Interaction)
        executeTarget(client);
    }

    private static void updateGoal() {
        if (!InventoryScanner.hasWood() && InventoryScanner.getIronCount() < 3) {
            currentGoal = Goal.GET_WOOD;
        } else if (InventoryScanner.hasWood() && !InventoryScanner.hasPickaxe()) {
            currentGoal = Goal.CRAFT_TOOLS;
        } else if (InventoryScanner.getIronCount() < 7) {
            currentGoal = Goal.GET_IRON;
        } else {
            currentGoal = Goal.GET_LAVA;
        }
    }

    private static void updateTargetSelection(MinecraftClient client) {
        if (InteractionManager.isInteracting())
            return;

        BlockPos newTarget = null;
        Entity newEntity = null;
        String type = "";

        switch (currentGoal) {
            case GET_WOOD:
                newTarget = DistributedScanner.getTree();
                type = "WOOD_LOG";
                break;
            case GET_IRON:
                newTarget = DistributedScanner.getIron();
                type = "IRON_ORE";
                // Add Village/Golem checking here later
                break;
            case GET_LAVA:
                newTarget = DistributedScanner.getLava();
                type = "LAVA_POOL";
                break;
            case CRAFT_TOOLS:
                // No physical target, but we might need to stand still
                break;
        }

        // If no target found, ENTER WANDER MODE
        if (newTarget == null && newEntity == null && currentGoal != Goal.CRAFT_TOOLS) {
            handleWandering(client);
        } else {
            setTarget(newTarget, newEntity, type);
            wanderCooldown = 0; // Found target, stop wandering
        }
    }

    private static void handleWandering(MinecraftClient client) {
        if (wanderCooldown <= 0) {
            DebugLogger.log("[Auto] No target in range. Wandering to explore...");
            // Choose a point 30 blocks away in a random-ish forward direction
            float yaw = client.player.yaw + (random.nextFloat() * 90 - 45);
            double rad = Math.toRadians(yaw);
            double x = -Math.sin(rad) * 30;
            double z = Math.cos(rad) * 30;
            BlockPos wanderPos = client.player.getBlockPos().add(x, 0, z);
            setTarget(wanderPos, null, "WANDER_POINT");
            wanderCooldown = 200; // Wander for 10 seconds or until target found
        } else {
            wanderCooldown--;
        }
    }

    private static void executeTarget(MinecraftClient client) {
        if (currentGoal == Goal.CRAFT_TOOLS) {
            CraftingManager.craftInInventory(client, "PLANKS");
            return;
        }

        if (InteractionManager.isInteracting() || currentTargetPos == null)
            return;

        double distSq = client.player.getBlockPos().getSquaredDistance(currentTargetPos);

        if (distSq < 4.5) { // Within 2 blocks
            if (currentTargetType.equals("WOOD_LOG")) {
                InteractionManager.breakBlock(currentTargetPos, 80);
            } else if (currentTargetType.equals("IRON_ORE") && InventoryScanner.hasPickaxe()) {
                InteractionManager.breakBlock(currentTargetPos, 50);
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
        DistributedScanner.tick(MinecraftClient.getInstance()); // Initial pulse
        DebugLogger.log("[Auto] Autonomous Play ENABLED.");
    }

    public static void stop() {
        active = false;
        InteractionManager.stopInteraction();
        DebugLogger.log("[Auto] Autonomous Play DISABLED.");
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
