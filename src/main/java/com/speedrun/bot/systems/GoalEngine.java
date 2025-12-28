package com.speedrun.bot.systems;

import com.speedrun.bot.utils.InventoryScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/**
 * GoalEngine - Strategic Decision Making.
 * Manages the state machine and sets targets for Pathing and Interaction.
 */
public class GoalEngine {

    public enum State {
        IDLE,
        GATHER_WOOD,
        CRAFT_PLANKS,
        CRAFT_TABLE,
        CRAFT_PICKAXE,
        GATHER_IRON
    }

    public static State currentState = State.IDLE;
    private static BlockPos currentTarget = null;

    public static void tick(MinecraftClient client) {
        if (client.player == null)
            return;

        updateState(client);

        switch (currentState) {
            case GATHER_WOOD:
                BlockPos log = AsyncChunkScanner.getNearestLog();
                if (log != null) {
                    PathingControl.setTarget(log);
                    InteractionControl.setBreakTarget(log);
                } else {
                    PathingControl.wander(client);
                }
                break;

            case CRAFT_PLANKS:
                // Triggered in GoalEngine state change, but execution remains in
                // Strategy/Crafting
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
        }
    }

    private static void updateState(MinecraftClient client) {
        // Use the existing InventoryScanner utility
        boolean hasWood = InventoryScanner.hasWood();
        boolean hasPlanks = InventoryScanner.hasPlanks();
        boolean hasPick = InventoryScanner.hasPickaxe();
        int ironCount = InventoryScanner.getIronCount();

        if (!hasPick) {
            if (!hasWood && !hasPlanks) {
                currentState = State.GATHER_WOOD;
            } else if (hasWood && !hasPlanks) {
                currentState = State.CRAFT_PLANKS;
            } else {
                currentState = State.CRAFT_PICKAXE;
            }
        } else {
            if (ironCount < 7) {
                currentState = State.GATHER_IRON;
            } else {
                currentState = State.IDLE; // Final goal for this stage
            }
        }
    }

    public static void reset() {
        currentState = State.IDLE;
        currentTarget = null;
    }
}
