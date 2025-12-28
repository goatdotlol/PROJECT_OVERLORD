package com.speedrun.bot.systems;

import com.speedrun.bot.utils.InventoryScanner;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/**
 * GoalEngine - Strategic Decision Making (v2.1 Logic).
 * Strict progression: Wood -> Stone -> Iron.
 */
public class GoalEngine {

    public enum State {
        IDLE,
        GATHER_LOGS,
        CRAFT_PLANKS,
        CRAFT_STICKS,
        CRAFT_TABLE,
        PLACE_TABLE,
        CRAFT_WOOD_PICK,
        GATHER_STONE,
        CRAFT_STONE_PICK,
        GATHER_IRON
    }

    public static State currentState = State.IDLE;
    private static BlockPos tablePos = null;

    public static void tick(MinecraftClient client) {
        if (client.player == null)
            return;

        updateState(client);

        switch (currentState) {
            case GATHER_LOGS:
                BlockPos log = AsyncChunkScanner.getNearestLog();
                if (log != null) {
                    PathingControl.setTarget(log);
                    InteractionControl.setBreakTarget(log);
                } else {
                    PathingControl.wander(client);
                }
                break;

            case CRAFT_PLANKS:
                CraftingControl.craftPlanks(client);
                break;

            case CRAFT_STICKS:
                CraftingControl.craftSticks(client);
                break;

            case CRAFT_TABLE:
                CraftingControl.craftTable(client);
                break;

            case PLACE_TABLE:
                // Look down and place
                HumanoidControl.lookAt(client, client.player.getBlockPos().down(), 2);
                if (client.player.getPitch() > 80) { // If looking down
                    // Select table
                    int tableSlot = findSlot(client, net.minecraft.item.Items.CRAFTING_TABLE);
                    if (tableSlot != -1) {
                        client.player.getInventory().selectedSlot = tableSlot;
                        client.interactionManager.interactItem(client.player, client.world,
                                net.minecraft.util.Hand.MAIN_HAND);
                        tablePos = client.player.getBlockPos()
                                .offset(net.minecraft.util.math.Direction.fromRotation(client.player.getYaw()));
                    }
                }
                break;

            case CRAFT_WOOD_PICK:
                // Check if near table
                BlockPos table = AsyncChunkScanner.findNearestTable(client); // We will add this helper or just check
                                                                             // radius
                if (table == null && tablePos != null
                        && client.player.squaredDistanceTo(tablePos.getX(), tablePos.getY(), tablePos.getZ()) < 16)
                    table = tablePos;

                if (table != null) {
                    InteractionControl.interactBlock(table); // Open GUI
                    CraftingControl.craftPickaxe(client);
                } else {
                    currentState = State.PLACE_TABLE;
                }
                break;

            case GATHER_STONE:
                BlockPos stone = AsyncChunkScanner.getNearestStone();
                if (stone != null) {
                    PathingControl.setTarget(stone);
                    InteractionControl.setBreakTarget(stone);
                } else {
                    // Dig Down Logic (Simple)
                    InteractionControl.setBreakTarget(client.player.getBlockPos().down());
                }
                break;

            case CRAFT_STONE_PICK:
                if (tablePos != null
                        && client.player.squaredDistanceTo(tablePos.getX(), tablePos.getY(), tablePos.getZ()) < 16) {
                    InteractionControl.interactBlock(tablePos);
                    CraftingControl.craftStonePickaxe(client);
                } else {
                    currentState = State.PLACE_TABLE;
                }
                break;

            case GATHER_IRON:
                BlockPos iron = AsyncChunkScanner.getNearestIron();
                if (iron != null) {
                    PathingControl.setTarget(iron);
                    InteractionControl.setBreakTarget(iron);
                } else {
                    PathingControl.wander(client);
                }
                break;

            default:
                break;
        }
    }

    private static void updateState(MinecraftClient client) {
        int logs = InventoryScanner.countItem(net.minecraft.item.Items.OAK_LOG); // Naive check
        boolean hasPlanks = InventoryScanner.hasPlanks();
        boolean hasTable = InventoryScanner.hasWorkbench();
        boolean hasWoodPick = InventoryScanner.hasPickaxe(); // Should check specific type
        boolean hasStonePick = false; // Need specific check

        // Strict Progression
        if (!hasWoodPick && !hasStonePick) {
            if (logs < 3 && !hasPlanks) {
                currentState = State.GATHER_LOGS;
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
            currentState = State.GATHER_IRON;
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
    }
}
