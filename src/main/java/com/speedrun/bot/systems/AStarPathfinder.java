package com.speedrun.bot.systems;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/**
 * AStarPathfinder (Baritone-Lite) - Node-based pathfinding with heuristics.
 */
public class AStarPathfinder {

    private static final int MAX_ITERATIONS = 3000;

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

                    // 1. FLAT MOVE (Walk/Swim)
                    BlockPos neighbor = current.pos.add(x, 0, z);
                    if (canMoveTo(client, neighbor)) {
                        double cost = (x != 0 && z != 0) ? 1.414 : 1.0;
                        if (isWater(client, neighbor))
                            cost *= 1.5; // Swimming matches slower
                        addNode(open, closed, neighbor, current, target, cost);
                    }

                    // 2. JUMP (Step up 1 block)
                    BlockPos jumpPos = current.pos.add(x, 1, z);
                    if (canMoveTo(client, jumpPos) && isSolid(client, current.pos.add(x, 0, z))) { // Ensure we are
                                                                                                   // jumping onto
                                                                                                   // something
                        addNode(open, closed, jumpPos, current, target, 1.2);
                    }

                    // 3. DROP (Step down)
                    for (int i = 1; i <= 3; i++) {
                        BlockPos dropPos = current.pos.add(x, -i, z);
                        if (canMoveTo(client, dropPos) && isAir(client, current.pos.add(x, -i + 1, z))) { // Check head
                                                                                                          // clearance
                                                                                                          // for drop
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

    // --- Safety & Material Checks ---

    private static boolean canMoveTo(MinecraftClient client, BlockPos pos) {
        // Must be safe to stand in (Air or Water) AND safe to stand ON (Solid below or
        // Water below)
        // AND Head must be safe (Air/Water)
        return isSafeBody(client, pos) && isSafeBody(client, pos.up()) &&
                (isSolid(client, pos.down()) || isWater(client, pos.down())); // Swim support
    }

    private static boolean isSafeBody(MinecraftClient client, BlockPos pos) {
        if (client.world == null)
            return false;
        Material m = client.world.getBlockState(pos).getMaterial();
        // Safe if Air, Water, Grass (tall grass), etc.
        // Unsafe if Solid, Lava, Fire, Magma
        if (m.isSolid())
            return false;
        Block b = client.world.getBlockState(pos).getBlock();
        if (b == net.minecraft.block.Blocks.LAVA || b == net.minecraft.block.Blocks.FIRE
                || b == net.minecraft.block.Blocks.MAGMA_BLOCK)
            return false;
        return true;
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
        return client.world.getBlockState(pos).isAir();
    }

    private static boolean isWater(MinecraftClient client, BlockPos pos) {
        if (client.world == null)
            return false;
        return client.world.getBlockState(pos).getMaterial() == Material.WATER;
    }
}
