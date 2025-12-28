package com.speedrun.bot.navigation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.*;

/**
 * LocalPathfinder - Enhanced with Diagonal support and jumping logic.
 */
public class LocalPathfinder {

    private static class Node {
        BlockPos pos;
        Node parent;
        double gCost;
        double hCost;

        public Node(BlockPos pos, Node parent, double gCost, double hCost) {
            this.pos = pos;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
        }

        public double fCost() {
            return gCost + hCost;
        }
    }

    public static List<BlockPos> findPath(BlockPos start, BlockPos end) {
        // Reject extreme distances
        if (start.getSquaredDistance(end) > 120 * 120)
            return null;

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(Node::fCost));
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, Node> nodeMap = new HashMap<>();

        openSet.add(new Node(start, null, 0, getHeuristic(start, end)));
        nodeMap.put(start, openSet.peek());

        int iterations = 0;
        MinecraftClient client = MinecraftClient.getInstance();

        while (!openSet.isEmpty() && iterations < 1500) {
            iterations++;
            Node current = openSet.poll();

            // Goal check (close enough is fine for local pathing)
            if (current.pos.getSquaredDistance(end) <= 2.25) {
                return retracePath(current);
            }

            closedSet.add(current.pos);

            // 1. Horizontal/Cardinal Neighbors
            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP || dir == Direction.DOWN)
                    continue;

                BlockPos neighbor = current.pos.offset(dir);
                checkNeighbor(client, current, neighbor, end, 1.0, openSet, closedSet, nodeMap);

                // 2. Diagonals (A bit more expensive)
                BlockPos diag = neighbor.offset(dir.rotateYClockwise());
                checkNeighbor(client, current, diag, end, 1.414, openSet, closedSet, nodeMap);
            }
        }
        return null;
    }

    private static void checkNeighbor(MinecraftClient client, Node current, BlockPos pos, BlockPos end, double cost,
            PriorityQueue<Node> openSet, Set<BlockPos> closedSet, Map<BlockPos, Node> nodeMap) {

        BlockPos finalPos = pos;

        // Step Up logic
        if (!isWalkable(finalPos, client)) {
            if (isWalkable(finalPos.up(), client)) {
                finalPos = finalPos.up();
                cost += 0.2; // Small penalty for jumping
            } else if (isWalkable(finalPos.down(), client)) {
                finalPos = finalPos.down();
            } else {
                return; // Not walkable
            }
        }

        if (closedSet.contains(finalPos))
            return;

        double newGCost = current.gCost + cost;
        Node existing = nodeMap.get(finalPos);

        if (existing == null || newGCost < existing.gCost) {
            Node n = new Node(finalPos, current, newGCost, getHeuristic(finalPos, end));
            nodeMap.put(finalPos, n);
            openSet.add(n);
        }
    }

    private static double getHeuristic(BlockPos a, BlockPos b) {
        // Octile distance for diagonal support
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        return Math.max(dx, Math.max(dy, dz));
    }

    private static List<BlockPos> retracePath(Node node) {
        List<BlockPos> path = new ArrayList<>();
        Node current = node;
        while (current != null) {
            path.add(current.pos);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static boolean isWalkable(BlockPos pos, MinecraftClient client) {
        if (client.world == null)
            return false;
        // 1.16.1 collision check
        return !client.world.getBlockState(pos).getMaterial().isSolid() &&
                !client.world.getBlockState(pos.up()).getMaterial().isSolid() &&
                client.world.getBlockState(pos.down()).getMaterial().isSolid();
    }
}
