package com.speedrun.bot.navigation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.*;

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
        if (start.getSquaredDistance(end) > 100 * 100)
            return null; // Too far

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(Node::fCost));
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, Node> nodeMap = new HashMap<>();

        openSet.add(new Node(start, null, 0, getHeuristic(start, end)));
        nodeMap.put(start, openSet.peek());

        int iterations = 0;
        MinecraftClient client = MinecraftClient.getInstance();

        while (!openSet.isEmpty() && iterations < 800) { // Reduced limit for better FPS
            iterations++;
            Node current = openSet.poll();

            if (current.pos.getSquaredDistance(end) <= 2.25) {
                return retracePath(current);
            }

            closedSet.add(current.pos);

            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP || dir == Direction.DOWN)
                    continue;

                BlockPos neighborPos = current.pos.offset(dir);

                // Handle Step Up/Down (1 block)
                if (!isWalkable(neighborPos, client)) {
                    if (isWalkable(neighborPos.up(), client)) {
                        neighborPos = neighborPos.up();
                    } else if (isWalkable(neighborPos.down(), client)) {
                        neighborPos = neighborPos.down();
                    } else {
                        continue;
                    }
                }

                if (closedSet.contains(neighborPos))
                    continue;

                double newCost = current.gCost + 1;
                Node neighbor = nodeMap.get(neighborPos);

                if (neighbor == null || newCost < neighbor.gCost) {
                    Node n = new Node(neighborPos, current, newCost, getHeuristic(neighborPos, end));
                    nodeMap.put(neighborPos, n);
                    openSet.add(n);
                }
            }
        }
        return null;
    }

    private static double getHeuristic(BlockPos a, BlockPos b) {
        return Math.sqrt(a.getSquaredDistance(b));
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
        return !client.world.getBlockState(pos).getMaterial().isSolid() &&
                !client.world.getBlockState(pos.up()).getMaterial().isSolid() &&
                client.world.getBlockState(pos.down()).getMaterial().isSolid();
    }
}
