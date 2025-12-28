package com.speedrun.bot.systems;

import com.speedrun.bot.utils.InventoryScanner;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;

/**
 * GoalEngine - Strategic Decision Making (v2.1 Logic).
 * Strict progression: Wood -> Stone -> Iron.
 */
public class GoalEngine {

    public enum State {
        IDLE,
        GATHER_LOGS,
        COLLECT_DROPS,
        CRAFT_PLANKS,
        CRAFT_STICKS,
        CRAFT_TABLE,
        PLACE_TABLE,
        CRAFT_WOOD_PICK,
        GATHER_STONE,
        CRAFT_STONE_PICK,
        GATHER_IRON,
        HUNT_GOLEM
    }

    public static State currentState = State.IDLE;
    public static String status = "Waiting...";
    private static BlockPos tablePos = null;
    private static long lastInteractionTime = 0; // Cooldown

    public static void tick(MinecraftClient client) {
        if (client.player == null)
            return;

        updateState(client);

        // Global Cooldown check to prevent spam
        if (System.currentTimeMillis() - lastInteractionTime < 200) {
            return; // Wait 200ms between heavy actions
        }

        switch (currentState) {
            case GATHER_LOGS:
                status = "Finding Logs";
                BlockPos log = AsyncChunkScanner.getNearestLog();
                if (log != null) {
                    PathingControl.setTarget(log);
                    InteractionControl.setBreakTarget(log);
                    status = "Chopping Log at " + log.toShortString();
                } else {
                    status = "Wandering (Searching for Wood)";
                    PathingControl.wander(client);
                }
                break;

            case CRAFT_PLANKS:
                status = "Crafting Planks";
                CraftingControl.craftPlanks(client);
                break;

            case CRAFT_STICKS:
                status = "Crafting Sticks";
                CraftingControl.craftSticks(client);
                break;

            case CRAFT_TABLE:
                status = "Crafting Table";
                CraftingControl.craftTable(client);
                break;

            case CRAFT_WOOD_PICK:
                status = "Crafting Wood Pickaxe";

                // 1. If we are already in the GUI, just craft!
                if (client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.CraftingScreen) {
                    status = "Crafting Pick (GUI Open)";
                    CraftingControl.craftPickaxe(client);
                    return; // IMPORTANT: accurate return to avoid re-interaction
                }

                // 2. If valid table is nearby, open it
                BlockPos table = AsyncChunkScanner.findNearestTable(client);
                if (table == null && tablePos != null
                        && client.player.squaredDistanceTo(tablePos.getX(), tablePos.getY(), tablePos.getZ()) < 25) {
                    // Fallback: use memory
                    table = tablePos;
                }

                if (table != null) {
                    // Move to table if too far
                    if (Math.sqrt(client.player.squaredDistanceTo(table.getX(), table.getY(), table.getZ())) > 4) {
                        PathingControl.setTarget(table);
                        status = "Moving to Table";
                    } else {
                        // Close enough: Interact
                        InteractionControl.interactBlock(client, table);
                        PathingControl.stop(client);
                    }
                } else {
                    status = "Lost Table. Re-placing.";
                    currentState = State.PLACE_TABLE;
                }
                break;

            case PLACE_TABLE:
                status = "Placing Table";

                // Smart Placement: Find a valid spot (Air block with Solid block below)
                BlockPos targetPlace = findPlacementSpot(client);

                if (targetPlace == null) {
                    status = "No valid spot to place table!";
                    tablePos = client.player.getBlockPos(); // Desperate fail-safe
                    return;
                }

                PathingControl.stop(client); // Stop moving to place
                HumanoidControl.lookAt(client, targetPlace, 2);

                // Relaxed Pitch Check (45 degrees is enough to hit the block)
                double lookError = Math.abs(client.player.pitch - 90);
                // Just swing if we are looking somewhat down or at the block
                if (true) { // removed strict check, just spam it if we are close
                    int tableSlot = findSlot(client, net.minecraft.item.Items.CRAFTING_TABLE);
                    if (tableSlot != -1) {
                        client.player.inventory.selectedSlot = tableSlot;
                        // Force Look at center
                        HumanoidControl.lookAt(client, targetPlace, 2);

                        // Interact
                        client.interactionManager.interactItem(client.player, client.world,
                                net.minecraft.util.Hand.MAIN_HAND);
                        client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);

                        // Assume success for speed
                        tablePos = targetPlace;
                        currentState = State.CRAFT_WOOD_PICK; // Move on immediately
                    }
                }
                break;

            case GATHER_STONE:
                status = "Mining Stone";
                BlockPos stone = AsyncChunkScanner.getNearestStone();
                if (stone != null) {
                    PathingControl.setTarget(stone);
                    InteractionControl.setBreakTarget(stone);
                    status = "Mining Stone at " + stone.toShortString();
                } else {
                    status = "Digging Shaft (No Surface Stone)";
                    InteractionControl.setBreakTarget(client.player.getBlockPos().down());
                }
                break;

            case CRAFT_STONE_PICK:
                status = "Crafting Stone Pickaxe";
                if (tablePos != null
                        && client.player.squaredDistanceTo(tablePos.getX(), tablePos.getY(), tablePos.getZ()) < 16) {
                    InteractionControl.interactBlock(client, tablePos);
                    CraftingControl.craftStonePickaxe(client);
                } else {
                    status = "Need Table for Stone Pick";
                    currentState = State.PLACE_TABLE;
                }
                break;

            case HUNT_GOLEM:
                status = "Fighting Golem";
                net.minecraft.entity.Entity golem = AsyncChunkScanner.getNearestGolem();
                if (golem != null && golem.isAlive()) {
                    CombatControl.fightGolem(client, golem);
                } else {
                    status = "Golem dead/lost";
                    currentState = State.IDLE; // Re-evaluate
                }
                break;

            case GATHER_IRON:
                status = "Mining Iron";
                BlockPos iron = AsyncChunkScanner.getNearestIron();
                if (iron != null) {
                    PathingControl.setTarget(iron);
                    InteractionControl.setBreakTarget(iron);
                } else {
                    PathingControl.wander(client);
                    status = "Searching for Iron...";
                }
                break;

            default:
                status = "Idle";
                break;
        }
    }

    private static void updateState(MinecraftClient client) {
        int logs = InventoryScanner.countLogs(); // Robust tag-based check
        boolean hasPlanks = InventoryScanner.hasPlanks();
        boolean hasTable = InventoryScanner.hasWorkbench();
        boolean hasWoodPick = InventoryScanner.hasPickaxe(); // Should check specific type
        boolean hasStonePick = false; // Need specific check

        // Strict Progression
        if (!hasWoodPick && !hasStonePick) {
            if (logs < 3 && !hasPlanks) {
                // If we have logs on ground nearby, pick them up!
                if (AsyncChunkScanner.getNearestItem() != null
                        && AsyncChunkScanner.getNearestItem().squaredDistanceTo(client.player) < 100) {
                    currentState = State.COLLECT_DROPS;
                } else {
                    currentState = State.GATHER_LOGS;
                }
            } else if (logs >= 1 && !hasPlanks) {
                currentState = State.CRAFT_PLANKS;
            } else if (hasPlanks && !hasTable) {
                currentState = State.CRAFT_TABLE;
            } else {
                currentState = State.CRAFT_WOOD_PICK;
            }
        } else if (hasWoodPick && !hasStonePick) {
            if (InventoryScanner.countItem(net.minecraft.item.Items.COBBLESTONE) < 3) {
                currentState = State.GATHER_STONE;
            } else {
                currentState = State.CRAFT_STONE_PICK;
            }
        } else {
            // Check for Iron Golem
            if (AsyncChunkScanner.getNearestGolem() != null) {
                currentState = State.HUNT_GOLEM;
            } else {
                currentState = State.GATHER_IRON;
            }
        }
    }

    private static BlockPos findPlacementSpot(MinecraftClient client) {
        BlockPos playerPos = client.player.getBlockPos();
        // Check 3x3 around player
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                // Try to place at feet level or 1 block away
                BlockPos target = playerPos.add(x, 0, z);
                // Must be AIR and have SOLID below
                if (client.world.getBlockState(target).getMaterial().isReplaceable() &&
                        client.world.getBlockState(target.down()).getMaterial().isSolid()) {
                    return target;
                }
            }
        }
        return playerPos.down(); // Fallback
    }

    private static int findSlot(MinecraftClient client, net.minecraft.item.Item item) {
        if (client.player == null)
            return -1;
        for (int i = 0; i < 9; i++) {
            if (client.player.inventory.getStack(i).getItem() == item)
                return i;
        }
        return -1;
    }

    public static void reset() {
        currentState = State.IDLE;
        tablePos = null;
        status = "Reset";
    }
}
