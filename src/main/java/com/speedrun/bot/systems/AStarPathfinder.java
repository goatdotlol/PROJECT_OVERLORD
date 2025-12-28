package com.speedrun.bot.systems;

import net.minecraft.block.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/**
 * AStarPathfinder (Baritone-Lite) - Node-based pathfinding with heuristics.
 */
public class AStarPathfinder {

    private static final int MAX_ITERATIONS = 1500;

    public static class Node {
        public BlockPos pos;
        public Node parent;
        public double g;
        public double h;

        public double f() {
            return g + h;
        }

        public Node(BlockPos pos, Node parent, double g, double h) {
            this.pos = pos;
            this.parent = parent;
            this.g = g;
            this.h = h;
        }
    }

    public static List<BlockPos> compute(BlockPos start, BlockPos target) {
        MinecraftClient client = MinecraftClient.getInstance();
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::f));
        Set<Long> closed = new HashSet<>();

        open.add(new Node(start, null, 0, start.getSquaredDistance(target)));

        int iterations = 0;
        Node bestNode = null;
        double closestDist = Double.MAX_VALUE;

        while (!open.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;
            Node current = open.poll();
            long posHash = current.pos.asLong();

            if (closed.contains(posHash))
                continue;
            closed.add(posHash);

            double distToTarget = current.pos.getSquaredDistance(target);
            if (distToTarget < 2.0) {
                return rebuildPath(current);
            }

            if (distToTarget < closestDist) {
                closestDist = distToTarget;
                bestNode = current;
            }

            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0)
                        continue;

                    // 1. FLAT MOVE
                    BlockPos neighbor = current.pos.add(x, 0, z);
                    if (isSafe(client, neighbor)) {
                        addNode(open, closed, neighbor, current, target, (x != 0 && z != 0) ? 1.414 : 1.0);
                        continue;
                    }

                    // 2. JUMP (Step up)
                    BlockPos jumpPos = current.pos.add(x, 1, z);
                    if (isSafe(client, jumpPos) && isSolid(client, current.pos.add(x, 0, z))) {
                        addNode(open, closed, jumpPos, current, target, 1.2);
                        continue;
                    }

                    // 3. DROP (Step down)
                    for (int i = 1; i <= 3; i++) {
                        BlockPos dropPos = current.pos.add(x, -i, z);
                        if (isSafe(client, dropPos) && isAir(client, current.pos.add(x, -i + 1, z))) {
                            addNode(open, closed, dropPos, current, target, 1.0 + (i * 0.5));
                            break;
                        }
                    }
                }
            }
        }

        return (bestNode != null) ? rebuildPath(bestNode) : new ArrayList<>();
    }

    private static void addNode(PriorityQueue<Node> open, Set<Long> closed, BlockPos pos, Node parent, BlockPos target,
            double costMod) {
        if (closed.contains(pos.asLong()))
            return;
        double g = parent.g + costMod;
        double h = Math.sqrt(pos.getSquaredDistance(target));
        open.add(new Node(pos, parent, g, h));
    }

    private static List<BlockPos> rebuildPath(Node node) {
        List<BlockPos> path = new ArrayList<>();
        while (node != null) {
            path.add(node.pos);
            node = node.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static boolean isSafe(MinecraftClient client, BlockPos pos) {
        return isAir(client, pos) &&
                isAir(client, pos.up()) &&
                isSolid(client, pos.down()) &&
                !isLava(client, pos.down());
    }

    private static boolean isSolid(MinecraftClient client, BlockPos pos) {
        if (client.world == null)
            return false;
        Material m = client.world.getBlockState(pos).getMaterial();
        return m.isSolid();
    }

    private static boolean isAir(MinecraftClient client, BlockPos pos) {
        if (client.world == null)
            return false;
        Material m = client.world.getBlockState(pos).getMaterial();
        return !m.isSolid() && !m.isLiquid();
    }

    private static boolean isLava(MinecraftClient client, BlockPos pos) {
        if (client.world == null)
            return false;
        return client.world.getBlockState(pos).getBlock() == net.minecraft.block.Blocks.LAVA;
    }
}
