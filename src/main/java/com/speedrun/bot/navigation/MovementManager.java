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
        // If target is an entity, update the target position to its location
        if (OverworldManager.getTargetEntity() != null) {
            targetPos = OverworldManager.getTargetEntity().getBlockPos();
        }

        if (targetPos == null) {
            stopMovement(client);
            return;
        }

        // 1. Check if we need a new path
        if (currentTarget == null || !currentTarget.equals(targetPos) || currentPath == null || currentPath.isEmpty()) {
            if (pathUpdateCooldown <= 0) {
                DebugLogger.log("[LEGS] Planning path to target...");
                currentPath = LocalPathfinder.findPath(client.player.getBlockPos(), targetPos);
                currentTarget = targetPos;
                pathUpdateCooldown = 40; // Don't spam pathfinding
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
        Vec3d nextNodeVec = Vec3d.ofCenter(nextNode);

        double distSq = playerPos.squaredDistanceTo(nextNodeVec.x, playerPos.y, nextNodeVec.z);

        // 1. Progress to next node if close enough (horizontal distance)
        if (distSq < 0.5) {
            currentPath.remove(0);
            if (currentPath.isEmpty()) {
                DebugLogger.log("[LEGS] Reached path destination.");
                stopMovement(client);
                return;
            }
            nextNode = currentPath.get(0);
            nextNodeVec = Vec3d.ofCenter(nextNode);
        }

        // 2. Look at the node
        InputSimulator.lookAt(nextNodeVec, 2);

        // 3. Move forward
        InputSimulator.setKeyState(client.options.keyForward, true);

        // 4. Jump if needed (simplified)
        // If node is higher than player or there's a block in front
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
