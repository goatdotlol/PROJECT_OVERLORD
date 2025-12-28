package com.speedrun.bot.systems;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.List;
import java.util.Random;

/**
 * PathingControl - Orchestrates A* navigation and movement inputs.
 */
public class PathingControl {

    private static List<BlockPos> currentPath;
    private static int pathIndex = 0;
    private static BlockPos finalDestination;
    private static final Random rng = new Random();

    public static void setTarget(BlockPos pos) {
        if (finalDestination != null && finalDestination.equals(pos) && currentPath != null)
            return;
        finalDestination = pos;
        currentPath = null;
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || finalDestination == null)
            return;

        BlockPos playerPos = client.player.getBlockPos();

        // 1. Path Calculation (A*)
        if (currentPath == null || currentPath.isEmpty()) {
            currentPath = AStarPathfinder.compute(playerPos, finalDestination);
            pathIndex = 0;
            if (currentPath == null || currentPath.isEmpty()) {
                finalDestination = null; // Unreachable
                return;
            }
        }

        // 2. Execution
        if (pathIndex < currentPath.size()) {
            BlockPos nextNode = currentPath.get(pathIndex);

            // Movement Inputs
            HumanoidControl.lookAt(client, nextNode);
            client.options.keyForward.setPressed(true);

            // Robust jumping logic
            if (nextNode.getY() > playerPos.getY() + 0.1 || client.player.horizontalCollision) {
                client.options.keyJump.setPressed(true);
            } else {
                client.options.keyJump.setPressed(false);
            }

            // Node Advancement
            if (distance2d(client.player.getPos(), nextNode) < 0.7) {
                pathIndex++;
            }
        } else {
            stop(client);
        }
    }

    public static void wander(MinecraftClient client) {
        if (finalDestination != null)
            return;
        double angle = rng.nextDouble() * 2 * Math.PI;
        int x = (int) (Math.cos(angle) * 30);
        int z = (int) (Math.sin(angle) * 30);
        setTarget(client.player.getBlockPos().add(x, 0, z));
    }

    public static void stop(MinecraftClient client) {
        if (client.options != null) {
            client.options.keyForward.setPressed(false);
            client.options.keyJump.setPressed(false);
        }
        finalDestination = null;
        currentPath = null;
    }

    private static double distance2d(Vec3d v, BlockPos p) {
        double dX = v.x - (p.getX() + 0.5);
        double dZ = v.z - (p.getZ() + 0.5);
        return Math.sqrt(dX * dX + dZ * dZ);
    }
}
