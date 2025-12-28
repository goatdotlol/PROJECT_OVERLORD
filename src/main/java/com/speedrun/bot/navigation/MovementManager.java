package com.speedrun.bot.navigation;

import com.speedrun.bot.input.InputSimulator;
import com.speedrun.bot.strategy.OverworldManager;
import com.speedrun.bot.utils.DebugLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.List;

public class MovementManager {

    private static List<BlockPos> currentPath = null;
    private static BlockPos currentTarget = null;
    private static int pathUpdateCooldown = 0;

    public static void tick(MinecraftClient client) {
        if (client.player == null)
            return;

        BlockPos targetPos = OverworldManager.getTargetPos();
        if (OverworldManager.getTargetEntity() != null) {
            targetPos = OverworldManager.getTargetEntity().getBlockPos();
        }

        if (targetPos == null) {
            stopMovement(client);
            return;
        }

        if (currentTarget == null || !currentTarget.equals(targetPos) || currentPath == null || currentPath.isEmpty()) {
            if (pathUpdateCooldown <= 0) {
                DebugLogger.log("[LEGS] Planning path to target...");
                currentPath = LocalPathfinder.findPath(client.player.getBlockPos(), targetPos);
                currentTarget = targetPos;
                pathUpdateCooldown = 40;
            }
        }

        if (pathUpdateCooldown > 0)
            pathUpdateCooldown--;

        if (currentPath != null && !currentPath.isEmpty()) {
            followPath(client);
        } else {
            stopMovement(client);
        }
    }

    private static void followPath(MinecraftClient client) {
        BlockPos nextNode = currentPath.get(0);
        Vec3d playerPos = client.player.getPos();
        // 1.16.1 Fix: Use manual center calculation instead of Vec3d.ofCenter
        Vec3d nextNodeVec = new Vec3d(nextNode.getX() + 0.5, nextNode.getY() + 0.5, nextNode.getZ() + 0.5);

        double distSq = playerPos.squaredDistanceTo(nextNodeVec.x, playerPos.y, nextNodeVec.z);

        if (distSq < 0.5) {
            currentPath.remove(0);
            if (currentPath.isEmpty()) {
                DebugLogger.log("[LEGS] Reached path destination.");
                stopMovement(client);
                return;
            }
            nextNode = currentPath.get(0);
            nextNodeVec = new Vec3d(nextNode.getX() + 0.5, nextNode.getY() + 0.5, nextNode.getZ() + 0.5);
        }

        InputSimulator.lookAt(nextNodeVec, 2);
        InputSimulator.setKeyState(client.options.keyForward, true);

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
