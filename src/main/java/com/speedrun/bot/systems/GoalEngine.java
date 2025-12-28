package com.speedrun.bot.systems;

import com.speedrun.bot.utils.InventoryScanner;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

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

    public static void tick(MinecraftClient client) {
        if (client.player == null)
            return;

        updateState(client);

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

            case PLACE_TABLE:
                status = "Placing Table";
                // Look down and place
                HumanoidControl.lookAt(client, client.player.getBlockPos().down(), 2);
                if (client.player.pitch > 80) { // If looking down
                    // Select table
                    int tableSlot = findSlot(client, net.minecraft.item.Items.CRAFTING_TABLE);
                    if (tableSlot != -1) {
                        client.player.inventory.selectedSlot = tableSlot;
                        client.interactionManager.interactItem(client.player, client.world,
                                net.minecraft.util.Hand.MAIN_HAND);

                        // Prediction: It will be at offset
                        BlockPos predicted = client.player.getBlockPos()
                                .offset(net.minecraft.util.math.Direction.fromRotation(client.player.yaw));
                        // Verify next tick
                        tablePos = predicted;
                    }
                }
                break;

            case CRAFT_WOOD_PICK:
                status = "Crafting Wood Pickaxe";
                // Check if near table
                BlockPos table = AsyncChunkScanner.findNearestTable(client);
                // Fallback to predicted position if scan fails (it might take a second to
                // update)
                if (table == null && tablePos != null
                        && client.player.squaredDistanceTo(tablePos.getX(), tablePos.getY(), tablePos.getZ()) < 16) {
                    if (client.world.getBlockState(tablePos).getBlock() == Blocks.CRAFTING_TABLE) {
                        table = tablePos;
                    }
                }

                if (table != null) {
                    InteractionControl.interactBlock(client, table); // Open GUI
                    CraftingControl.craftPickaxe(client);
                } else {
                    status = "Lost Table? Re-placing.";
                    currentState = State.PLACE_TABLE;
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
