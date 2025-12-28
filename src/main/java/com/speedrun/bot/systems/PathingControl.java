package com.speedrun.bot.systems;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.List;
import java.util.Random;

/**
 * PathingControl - Orchestrates A* navigation with Human-like Camera and Smart
 * Stopping.
 */
public class PathingControl {

    private static List<BlockPos> currentPath;
    private static int pathIndex = 0;
    private static BlockPos finalDestination;
    private static final Random rng = new Random();
    private static boolean isInteractionTarget = false;

    public static void setTarget(BlockPos pos) {
        // If it's the same target, don't reset unless path is done
        if (finalDestination != null && finalDestination.equals(pos) && currentPath != null && !currentPath.isEmpty())
            return;

        finalDestination = pos;
        currentPath = null;
        isInteractionTarget = true; // Assume any target set via this is for interaction
    }

    public static void wander(MinecraftClient client) {
        if (finalDestination != null)
            return;
        double angle = rng.nextDouble() * 2 * Math.PI;
        int x = (int) (Math.cos(angle) * 20);
        int z = (int) (Math.sin(angle) * 20);
        BlockPos wanderTarget = client.player.getBlockPos().add(x, 0, z);

        // Only set if safe
        currentPath = AStarPathfinder.compute(client.player.getBlockPos(), wanderTarget);
        if (currentPath != null && !currentPath.isEmpty()) {
            finalDestination = wanderTarget;
            pathIndex = 0;
            isInteractionTarget = false; // Wandering is just walking
        }
    }

    public static List<BlockPos> getCurrentPath() {
        return currentPath;
    }

    private static int stuckTicks = 0;
    private static BlockPos lastPos = null;

    public static void tick(MinecraftClient client) {
        if (client.player == null || finalDestination == null)
            return;

        BlockPos playerPos = client.player.getBlockPos();

        // --- STUCK DETECTION ---
        if (lastPos != null && playerPos.equals(lastPos)) {
            stuckTicks++;
            if (stuckTicks > 20) { // Stuck for 1 second
                // Try simple unstuck: Jump + Random waddle
                client.options.keyJump.setPressed(true);
                if (rng.nextBoolean())
                    client.options.keyLeft.setPressed(true);
                else
                    client.options.keyRight.setPressed(true);

                if (stuckTicks > 40) { // Still stuck? Recalculate path completely
                    currentPath = null;
                    stuckTicks = 0;
                }
            }
        } else {
            stuckTicks = 0;
            lastPos = playerPos;
            client.options.keyLeft.setPressed(false);
            client.options.keyRight.setPressed(false);
        }

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

            // --- SMART STOPPING ---
            // If we are targeting a block to break (isInteractionTarget), stop early!
            if (isInteractionTarget && pathIndex >= currentPath.size() - 2) { // Allow stopping 1-2 nodes early
                double distToFinal = distance2d(client.player.getPos(), finalDestination);
                if (distToFinal < 3.5) { // Interaction range
                    stop(client);
                    return;
                }
            }

            BlockPos nextNode = currentPath.get(pathIndex);

            // --- SMOOTH CAMERA (Look Ahead) ---
            // Look at a point further down the path for stability
            int lookIndex = Math.min(pathIndex + 2, currentPath.size() - 1);
            BlockPos lookTarget = currentPath.get(lookIndex);

            // Priority 1: Navigation
            HumanoidControl.lookAt(client, lookTarget, 1);

            client.options.keyForward.setPressed(true);
            client.player.setSprinting(true);

            // Robust jumping
            if (nextNode.getY() > playerPos.getY() + 0.1 || client.player.horizontalCollision) {
                client.options.keyJump.setPressed(true);
            } else {
                client.options.keyJump.setPressed(false);
            }

            // Node Advancement
            if (distance2d(client.player.getPos(), nextNode) < 0.8) {
                pathIndex++;
            }
        } else {
            stop(client);
        }
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
