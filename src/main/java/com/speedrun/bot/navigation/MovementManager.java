package com.speedrun.bot.navigation;

import com.speedrun.bot.input.InputSimulator;
import com.speedrun.bot.strategy.OverworldManager;
import com.speedrun.bot.strategy.AutoSpeedrunManager;
import com.speedrun.bot.input.InteractionManager;
import com.speedrun.bot.utils.DebugLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.Entity;
import java.util.List;

public class MovementManager {

    private static List<BlockPos> currentPath = null;
    private static BlockPos currentTarget = null;
    private static int pathUpdateCooldown = 0;

    public static void tick(MinecraftClient client) {
        if (client.player == null)
            return;

        // Use AutoSpeedrunManager target if active, otherwise use OverworldManager
        // target (only if we want to move manually)
        BlockPos targetPos = null;
        Entity targetEntity = null;

        if (AutoSpeedrunManager.isActive()) {
            targetPos = AutoSpeedrunManager.getTargetPos();
            targetEntity = AutoSpeedrunManager.getTargetEntity();
        }

        if (targetEntity != null) {
            targetPos = targetEntity.getBlockPos();
        }

        if (targetPos == null) {
            stopMovement(client);
            return;
        }

        // 0. If interacting (breaking block), stop moving
        if (InteractionManager.isInteracting()) {
            InputSimulator.setKeyState(client.options.keyForward, false);
            return;
        }

        // 1. Check if we need a new path
        if (currentTarget == null || !currentTarget.equals(targetPos) || currentPath == null || currentPath.isEmpty()) {
            if (pathUpdateCooldown <= 0) {
                currentPath = LocalPathfinder.findPath(client.player.getBlockPos(), targetPos);
                currentTarget = targetPos;
                pathUpdateCooldown = 40;
            }
        }

        if (pathUpdateCooldown > 0)
            pathUpdateCooldown--;

        // 2. Follow the path
        if (currentPath != null && !currentPath.isEmpty()) {
            followPath(client);
        } else {
            stopMovement(client);
        }
    }

    private static void followPath(MinecraftClient client) {
        BlockPos nextNode = currentPath.get(0);
        Vec3d playerPos = client.player.getPos();
        Vec3d nextNodeVec = new Vec3d(nextNode.getX() + 0.5, nextNode.getY() + 0.5, nextNode.getZ() + 0.5);

        double distSq = playerPos.squaredDistanceTo(nextNodeVec.x, playerPos.y, nextNodeVec.z);

        if (distSq < 0.8) { // Slightly larger radius for smooth flow
            currentPath.remove(0);
            if (currentPath.isEmpty()) {
                stopMovement(client);
                return;
            }
            nextNode = currentPath.get(0);
            nextNodeVec = new Vec3d(nextNode.getX() + 0.5, nextNode.getY() + 0.5, nextNode.getZ() + 0.5);
        }

        InputSimulator.lookAt(nextNodeVec, 2);
        InputSimulator.setKeyState(client.options.keyForward, true);

        // Jump if node is higher OR if we are stuck
        if (nextNode.getY() > client.player.getY() + 0.5 || client.player.horizontalCollision) {
            InputSimulator.pressKey(client.options.keyJump, 1);
        }
    }

    private static void stopMovement(MinecraftClient client) {
        InputSimulator.setKeyState(client.options.keyForward, false);
        currentPath = null;
        currentTarget = null;
    }
}
