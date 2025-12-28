package com.speedrun.bot.navigation;

import com.speedrun.bot.utils.DebugLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.*;

public class LocalPathfinder {

    private static class Node {
        BlockPos pos;
        Node parent;
        double gCost; // District from start
        double hCost; // Distance to end

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
        // Simple A* Search
        // Limit: 1000 nodes to prevent lag

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(Node::fCost));
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, Node> nodeMap = new HashMap<>(); // optimization to check if in openSet

        Node startNode = new Node(start, null, 0, start.getManhattanDistance(end));
        openSet.add(startNode);
        nodeMap.put(start, startNode);

        int iterations = 0;
        MinecraftClient client = MinecraftClient.getInstance();

        while (!openSet.isEmpty() && iterations < 1000) {
            iterations++;
            Node current = openSet.poll();

            if (current.pos.equals(end) || current.pos.getSquaredDistance(end) <= 2.25) {
                return retracePath(current);
            }

            closedSet.add(current.pos);

            for (Direction dir : Direction.values()) {
                // Simplified: Only strict horizontal/vertical. No diagonals.
                if (dir == Direction.UP || dir == Direction.DOWN)
                    continue; // Handle Y separately?
                // Actually basic walking includes Y changes (step up/down)

                BlockPos neighborPos = current.pos.offset(dir);

                if (closedSet.contains(neighborPos))
                    continue;
                if (!isWalkable(neighborPos, client))
                    continue;

                double newCost = current.gCost + 1; // dist is 1
                Node neighbor = nodeMap.get(neighborPos);

                if (neighbor == null || newCost < neighbor.gCost) {
                    Node n = new Node(neighborPos, current, newCost, neighborPos.getManhattanDistance(end));
                    nodeMap.put(neighborPos, n);
                    openSet.add(n);
                }
            }
        }

        DebugLogger.log("Pathfinder: No path found or timed out.");
        return null; // No path
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
        // Simplified Walkability:
        // 1. Block at pos is AIR (or passable)
        // 2. Block above pos is AIR
        // 3. Block below pos is SOLID
        return !client.world.getBlockState(pos).getMaterial().isSolid() &&
                !client.world.getBlockState(pos.up()).getMaterial().isSolid() &&
                client.world.getBlockState(pos.down()).getMaterial().isSolid();
    }
}
