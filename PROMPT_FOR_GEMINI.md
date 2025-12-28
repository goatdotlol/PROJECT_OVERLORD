# PROMPT FOR GEMINI 3 PRO THINKING

## 🎯 OBJECTIVE
Fully overhaul "Project OVERLORD," a Minecraft 1.16.1 (Fabric) autonomous speedrun bot, to achieve **Baritone-level performance and intelligence** without using external libraries.

## 🧱 CONSTRAINTS
- **Pure Vanilla**: Use only Minecraft 1.16.1 client-side APIs and Reflection.
- **Natural Simulation**: Movement must look humane (smooth turns, Gauss-humanized delays).
- **Environment**: Java 8, Fabric Loader 0.18.4.
- **Performance**: Must NOT lag the game (Internal Server should stay at 20TPS, Client at high FPS).

## 🛑 CURRENT CRITICAL ISSUES
1. **Scanning Lag**: Synchronous searching in a 100-block radius causes "Can't keep up" server lag and 0.1 FPS spikes.
2. **Perception Slowness**: The "Distributed Scanner" spreads work but is too slow to find targets in real-time (takes ~100s to find a tree 10 blocks away).
3. **Wacky Pathfinding**: A* logic gets stuck on fences, small hills, and 1-block gaps. It fails to reach targets 20 blocks away.
4. **Poor Intelligence**: The bot "wanders" even when targets are highlighted by ESP. It fails to transition from gathering logs to crafting tools.
5. **Interaction Alignment**: Swinging at air; fails to hit the center of blocks reliably.

## 🛠️ YOUR TASK
Redesign the core modules to solve these issues. Provide full code for:
1. **High-Efficiency Scanner**: Concurrent or multi-tick scanning of a 100-block radius without stutter. Priority: Golems > Villages > Logs > Iron > Lava.
2. **Baritone-Lite Pathfinder**: An optimized A* search that handles diagonal movement, parkour (leaping 1-block gaps), and robust step-up/down logic.
3. **Goal-Oriented State Machine**: Wood -> Crafting (Planks/Table/Pickaxe) -> Iron -> Lava. Wander ONLY if 100% nothing is in range.
4. **Precision Interaction**: Pixel-perfect alignment and realistic attack timing.

---

## 💾 CURRENT CODEBASE

### 1. BotMain.java
```java
package com.speedrun.bot;

import com.speedrun.bot.utils.DebugLogger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import com.speedrun.bot.input.InputSimulator;
import com.speedrun.bot.input.InteractionManager;
import com.speedrun.bot.strategy.OverworldManager;
import com.speedrun.bot.strategy.AutoSpeedrunManager;
import com.speedrun.bot.perception.DistributedScanner;
import com.speedrun.bot.navigation.MovementManager;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class BotMain implements ClientModInitializer {

    private static KeyBinding configKey;

    @Override
    public void onInitializeClient() {
        DebugLogger.clear();
        DebugLogger.log("Initializing Project OVERLORD - Ghost Engine (v1.0)");

        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ghost.config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.ghost.speedrun"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null)
                return;

            // 1. Core Logic Tick
            InputSimulator.tick(client);
            InteractionManager.tick(client);

            // 2. Perception Engine (Zero Lag)
            DistributedScanner.tick(client);

            // 3. Strategy & Goal Selection
            OverworldManager.tick(client); // Passive scanning
            AutoSpeedrunManager.tick(client); // Goal progression

            // 4. Navigation Tick (LEGS)
            MovementManager.tick(client);

            // GUI Toggle
            while (configKey.wasPressed()) {
                client.openScreen(new com.speedrun.bot.gui.ConfigScreen());
            }
        });

        DebugLogger.log("Ghost Engine Initialized Successfully.");
    }
}
```

### 2. WorldScanner.java (Legacy/Helper Scanner)
```java
package com.speedrun.bot.perception;

import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.tag.BlockTags;
import java.util.*;

public class WorldScanner {
    
    // Cache for slow-changing targets
    private static final Map<String, BlockPos> blockCache = new HashMap<>();
    private static long lastCacheFlush = 0;
    private static final long CACHE_EXPIRY = 2000;

    private static final Block[] VILLAGE_INDICATORS = {
        Blocks.BELL, Blocks.HAY_BLOCK, Blocks.COMPOSTER, Blocks.LECTERN,
        Blocks.CARTOGRAPHY_TABLE, Blocks.SMITHING_TABLE, Blocks.FLETCHING_TABLE,
        Blocks.BREWING_STAND, Blocks.BARREL, Blocks.SMOKER, Blocks.BLAST_FURNACE,
        Blocks.GRINDSTONE, Blocks.STONECUTTER, Blocks.LOOM
    };
    
    private static final Block[] SHIPWRECK_INDICATORS = {
        Blocks.OAK_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR, Blocks.BIRCH_TRAPDOOR,
        Blocks.JUNGLE_TRAPDOOR, Blocks.ACACIA_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR,
        Blocks.OAK_FENCE, Blocks.SPRUCE_FENCE, Blocks.BIRCH_FENCE,
        Blocks.JUNGLE_FENCE, Blocks.ACACIA_FENCE, Blocks.DARK_OAK_FENCE
    };

    public static BlockPos findNearestBlock(Block targetBlock, int radius) {
        String cacheKey = "BLOCK_" + targetBlock.getTranslationKey();
        if (isCacheValid(cacheKey)) return blockCache.get(cacheKey);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return null;
        
        BlockPos playerPos = client.player.getBlockPos();
        for (int r = 1; r <= radius; r++) {
            for (int y = -10; y <= 10; y++) {
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        if (Math.abs(x) != r && Math.abs(z) != r) continue;
                        BlockPos pos = playerPos.add(x, y, z);
                        if (client.world.getBlockState(pos).getBlock() == targetBlock) {
                            blockCache.put(cacheKey, pos);
                            return pos;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static BlockPos findTree(int radius) {
        String cacheKey = "TREE";
        if (isCacheValid(cacheKey)) return blockCache.get(cacheKey);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return null;
        
        BlockPos playerPos = client.player.getBlockPos();
        for (int r = 1; r <= radius; r++) {
            for (int y = -5; y <= 15; y++) { 
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        if (Math.abs(x) != r && Math.abs(z) != r) continue;
                        BlockPos pos = playerPos.add(x, y, z);
                        if (client.world.getBlockState(pos).getBlock().isIn(BlockTags.LOGS)) {
                            blockCache.put(cacheKey, pos);
                            return pos;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static ScanResult findShipwreck(int radius) {
        for (Block indicator : SHIPWRECK_INDICATORS) {
            BlockPos found = findNearestBlock(indicator, radius);
            if (found != null && found.getY() < 64) {
                return new ScanResult("SHIPWRECK", found, null);
            }
        }
        return null;
    }

    public static ScanResult findVillage(int radius) {
        BlockPos bell = findNearestBlock(Blocks.BELL, radius);
        if (bell != null) return new ScanResult("VILLAGE_BELL", bell, null);
        for (Block indicator : VILLAGE_INDICATORS) {
            BlockPos found = findNearestBlock(indicator, radius);
            if (found != null) return new ScanResult("VILLAGE_PIECE", found, null);
        }
        return null;
    }

    public static BlockPos findLavaPool(int radius) {
        String cacheKey = "LAVA";
        if (isCacheValid(cacheKey)) return blockCache.get(cacheKey);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return null;
        
        BlockPos playerPos = client.player.getBlockPos();
        for (int r = 1; r <= radius; r++) {
            for (int y = -10; y <= 10; y++) {
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        if (Math.abs(x) != r && Math.abs(z) != r) continue;
                        BlockPos pos = playerPos.add(x, y, z);
                        if (client.world.getBlockState(pos).getBlock() == Blocks.LAVA) {
                            if (client.world.getBlockState(pos.east()).getBlock() == Blocks.LAVA) {
                                blockCache.put(cacheKey, pos);
                                return pos;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static Entity findIronGolem(double radius) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return null;
        for (Entity entity : client.world.getEntities()) {
            if (entity.getType() == EntityType.IRON_GOLEM && entity.squaredDistanceTo(client.player) <= radius * radius) {
                return entity;
            }
        }
        return null;
    }

    private static boolean isCacheValid(String key) {
        if (System.currentTimeMillis() - lastCacheFlush > CACHE_EXPIRY) {
            blockCache.clear();
            lastCacheFlush = System.currentTimeMillis();
            return false;
        }
        return blockCache.containsKey(key);
    }

    public static class ScanResult {
        public final String type;
        public final BlockPos blockPos;
        public final Entity entity;
        public ScanResult(String type, BlockPos blockPos, Entity entity) {
            this.type = type;
            this.blockPos = blockPos;
            this.entity = entity;
        }
    }
}
```

### 3. DistributedScanner.java
```java
package com.speedrun.bot.perception;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.tag.BlockTags;

public class DistributedScanner {

    private static int currentR = 1;
    private static int currentY = -5;
    private static final int CHUNKS_PER_TICK = 4;
    
    private static BlockPos foundTree = null;
    private static BlockPos foundIron = null;
    private static BlockPos foundLava = null;
    private static BlockPos foundVillage = null;

    public static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        BlockPos playerPos = client.player.getBlockPos();
        
        for (int i = 0; i < CHUNKS_PER_TICK; i++) {
            scanShell(client, playerPos, currentR, currentY);
            currentY++;
            if (currentY > 15) {
                currentY = -10;
                currentR++;
            }
            if (currentR > 80) {
                currentR = 1;
                resetResults();
            }
        }
    }

    private static void scanShell(MinecraftClient client, BlockPos center, int r, int y) {
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (Math.abs(x) != r && Math.abs(z) != r) continue;
                BlockPos pos = center.add(x, y, z);
                Block block = client.world.getBlockState(pos).getBlock();

                if (foundTree == null && block.isIn(BlockTags.LOGS)) foundTree = pos;
                else if (foundIron == null && block == Blocks.IRON_ORE) foundIron = pos;
                else if (foundLava == null && block == Blocks.LAVA) {
                    if (client.world.getBlockState(pos.east()).getBlock() == Blocks.LAVA) foundLava = pos;
                } else if (foundVillage == null && block == Blocks.BELL) foundVillage = pos;
            }
        }
    }

    private static void resetResults() {
        foundTree = null; foundIron = null; foundLava = null; foundVillage = null;
    }

    public static BlockPos getTree() { return foundTree; }
    public static BlockPos getIron() { return foundIron; }
    public static BlockPos getLava() { return foundLava; }
    public static BlockPos getVillage() { return foundVillage; }
}
```

### 4. LocalPathfinder.java
```java
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
            this.pos = pos; this.parent = parent; this.gCost = gCost; this.hCost = hCost;
        }
        public double fCost() { return gCost + hCost; }
    }

    public static List<BlockPos> findPath(BlockPos start, BlockPos end) {
        if (start.getSquaredDistance(end) > 120 * 120) return null;
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
            if (current.pos.getSquaredDistance(end) <= 2.25) return retracePath(current);
            closedSet.add(current.pos);

            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP || dir == Direction.DOWN) continue;
                BlockPos neighbor = current.pos.offset(dir);
                checkNeighbor(client, current, neighbor, end, 1.0, openSet, closedSet, nodeMap);
                BlockPos diag = neighbor.offset(dir.rotateYClockwise());
                checkNeighbor(client, current, diag, end, 1.414, openSet, closedSet, nodeMap);
            }
        }
        return null;
    }

    private static void checkNeighbor(MinecraftClient client, Node current, BlockPos pos, BlockPos end, double cost, 
                                     PriorityQueue<Node> openSet, Set<BlockPos> closedSet, Map<BlockPos, Node> nodeMap) {
        BlockPos finalPos = pos;
        if (!isWalkable(finalPos, client)) {
            if (isWalkable(finalPos.up(), client)) { finalPos = finalPos.up(); cost += 0.2; }
            else if (isWalkable(finalPos.down(), client)) { finalPos = finalPos.down(); }
            else return;
        }
        if (closedSet.contains(finalPos)) return;
        double newGCost = current.gCost + cost;
        Node existing = nodeMap.get(finalPos);
        if (existing == null || newGCost < existing.gCost) {
            Node n = new Node(finalPos, current, newGCost, getHeuristic(finalPos, end));
            nodeMap.put(finalPos, n);
            openSet.add(n);
        }
    }

    private static double getHeuristic(BlockPos a, BlockPos b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.max(Math.abs(a.getY() - b.getY()), Math.abs(a.getZ() - b.getZ())));
    }

    private static List<BlockPos> retracePath(Node node) {
        List<BlockPos> path = new ArrayList<>();
        Node current = node;
        while (current != null) { path.add(current.pos); current = current.parent; }
        Collections.reverse(path);
        return path;
    }

    private static boolean isWalkable(BlockPos pos, MinecraftClient client) {
        if (client.world == null) return false;
        return !client.world.getBlockState(pos).getMaterial().isSolid() &&
               !client.world.getBlockState(pos.up()).getMaterial().isSolid() &&
               client.world.getBlockState(pos.down()).getMaterial().isSolid();
    }
}
```

### 5. MovementManager.java
```java
package com.speedrun.bot.navigation;

import com.speedrun.bot.input.InputSimulator;
import com.speedrun.bot.strategy.AutoSpeedrunManager;
import com.speedrun.bot.input.InteractionManager;
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
        if (client.player == null) return;

        BlockPos targetPos = null;
        Entity targetEntity = null;

        if (AutoSpeedrunManager.isActive()) {
            targetPos = AutoSpeedrunManager.getTargetPos();
            targetEntity = AutoSpeedrunManager.getTargetEntity();
        }

        if (targetEntity != null) targetPos = targetEntity.getBlockPos();
        if (targetPos == null) { stopMovement(client); return; }
        if (InteractionManager.isInteracting()) { InputSimulator.setKeyState(client.options.keyForward, false); return; }

        if (currentTarget == null || !currentTarget.equals(targetPos) || currentPath == null || currentPath.isEmpty()) {
            if (pathUpdateCooldown <= 0) {
                currentPath = LocalPathfinder.findPath(client.player.getBlockPos(), targetPos);
                currentTarget = targetPos;
                pathUpdateCooldown = 40;
            }
        }
        if (pathUpdateCooldown > 0) pathUpdateCooldown--;
        if (currentPath != null && !currentPath.isEmpty()) followPath(client);
        else stopMovement(client);
    }

    private static void followPath(MinecraftClient client) {
        BlockPos nextNode = currentPath.get(0);
        Vec3d nextNodeVec = new Vec3d(nextNode.getX() + 0.5, nextNode.getY() + 0.5, nextNode.getZ() + 0.5);
        double distSq = client.player.getPos().squaredDistanceTo(nextNodeVec.x, client.player.getY(), nextNodeVec.z);

        if (distSq < 0.8) {
            currentPath.remove(0);
            if (currentPath.isEmpty()) { stopMovement(client); return; }
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
        currentPath = null; currentTarget = null;
    }
}
```

### 6. AutoSpeedrunManager.java
```java
package com.speedrun.bot.strategy;

import com.speedrun.bot.utils.DebugLogger;
import com.speedrun.bot.utils.InventoryScanner;
import com.speedrun.bot.perception.DistributedScanner;
import com.speedrun.bot.input.InteractionManager;
import com.speedrun.bot.input.InputSimulator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import java.util.Random;

public class AutoSpeedrunManager {

    public enum Goal { IDLE, GET_WOOD, CRAFT_TOOLS, GET_IRON, GET_LAVA, WANDER }
    private static Goal currentGoal = Goal.IDLE;
    private static boolean active = false;
    private static BlockPos currentTargetPos = null;
    private static Entity currentTargetEntity = null;
    private static String currentTargetType = "";
    private static int wanderCooldown = 0;
    private static final Random random = new Random();

    public static void tick(MinecraftClient client) {
        if (!active || client.player == null) return;
        DistributedScanner.tick(client);
        updateGoal();
        updateTargetSelection(client);
        executeTarget(client);
    }

    private static void updateGoal() {
        if (!InventoryScanner.hasWood() && InventoryScanner.getIronCount() < 3) currentGoal = Goal.GET_WOOD;
        else if (InventoryScanner.hasWood() && !InventoryScanner.hasPickaxe()) currentGoal = Goal.CRAFT_TOOLS;
        else if (InventoryScanner.getIronCount() < 7) currentGoal = Goal.GET_IRON;
        else currentGoal = Goal.GET_LAVA;
    }

    private static void updateTargetSelection(MinecraftClient client) {
        if (InteractionManager.isInteracting()) return;
        BlockPos newTarget = null; String type = "";

        switch (currentGoal) {
            case GET_WOOD: newTarget = DistributedScanner.getTree(); type = "WOOD_LOG"; break;
            case GET_IRON: newTarget = DistributedScanner.getIron(); type = "IRON_ORE"; break;
            case GET_LAVA: newTarget = DistributedScanner.getLava(); type = "LAVA_POOL"; break;
        }

        if (newTarget == null && currentGoal != Goal.CRAFT_TOOLS) handleWandering(client);
        else { setTarget(newTarget, null, type); wanderCooldown = 0; }
    }

    private static void handleWandering(MinecraftClient client) {
        if (wanderCooldown <= 0) {
            float yaw = client.player.yaw + (random.nextFloat() * 90 - 45);
            double rad = Math.toRadians(yaw);
            BlockPos wanderPos = client.player.getBlockPos().add(-Math.sin(rad) * 30, 0, Math.cos(rad) * 30);
            setTarget(wanderPos, null, "WANDER_POINT");
            wanderCooldown = 200;
        } else wanderCooldown--;
    }

    private static void executeTarget(MinecraftClient client) {
        if (currentGoal == Goal.CRAFT_TOOLS) { CraftingManager.craftInInventory(client, "PLANKS"); return; }
        if (InteractionManager.isInteracting() || currentTargetPos == null) return;
        if (client.player.getBlockPos().getSquaredDistance(currentTargetPos) < 4.5) {
            if (currentTargetType.equals("WOOD_LOG")) InteractionManager.breakBlock(currentTargetPos, 80);
            else if (currentTargetType.equals("IRON_ORE") && InventoryScanner.hasPickaxe()) InteractionManager.breakBlock(currentTargetPos, 50);
        }
    }

    private static void setTarget(BlockPos pos, Entity entity, String type) {
        currentTargetPos = pos; currentTargetEntity = entity; currentTargetType = type;
    }

    public static void start() { active = true; DebugLogger.log("[Auto] Autonomous Play ENABLED."); }
    public static void stop() { active = false; InteractionManager.stopInteraction(); }
    public static BlockPos getTargetPos() { return currentTargetPos; }
    public static Entity getTargetEntity() { return currentTargetEntity; }
    public static boolean isActive() { return active; }
}
```

### 7. Other Files
*For brevity, assume InteractionManager handles precision alignment, CraftingManager handles basic inventory clicks, and InputSimulator provides GAUSS-humanized smooth rotations.*

---

## ⚡ FINAL INSTRUCTION
Provide the **optimized redesigns** now. Focus on a superior `Scanner` that hits 100 blocks at 60FPS and a `Pathfinder` that doesn't get stuck.
