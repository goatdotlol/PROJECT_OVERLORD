package com.speedrun.bot.systems;

import com.speedrun.bot.utils.DebugLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class CraftingControl {

    private static int stepTimer = 0;

    /**
     * Tries to craft planks from ANY logs in the 2x2 grid.
     */
    public static void craftPlanks(MinecraftClient client) {
        if (stepTimer > 0) {
            stepTimer--;
            return;
        }

        if (client.currentScreen == null) {
            client.openScreen(new InventoryScreen(client.player));
            stepTimer = 15; // Give it time to open
            return;
        }

        // Logic: Find ANY Log -> Slot 1 -> Craft All
        int logSlot = findItemSlot(client, ItemTags.LOGS);
        if (logSlot != -1) {
            // Click Log
            click(client, logSlot, 0, SlotActionType.PICKUP);
            // Place in Grid Slot 1 (Top Left 2x2)
            click(client, 1, 0, SlotActionType.PICKUP);
            // Put remaining logs back (if stack size > 1) -> Actually, just click slot 1
            // again to drop one?
            // No, simplified speedrun usage: Just put whole stack in, craft all planks.

            // Shift-Click Output (Slot 0)
            click(client, 0, 0, SlotActionType.QUICK_MOVE);

            // Return any ingredient remainder to inventory?
            // The logs stay in the grid if we don't pick them up.
            // But we closed screen. They drop or return. Good enough.

            client.player.closeHandledScreen();
            stepTimer = 20;
        } else {
            // If we are here, we have no logs. Close screen to reset?
            client.player.closeHandledScreen();
        }
    }

    public static void craftSticks(MinecraftClient client) {
        if (stepTimer > 0) {
            stepTimer--;
            return;
        }
        if (client.currentScreen == null) {
            client.openScreen(new InventoryScreen(client.player));
            stepTimer = 15;
            return;
        }

        // Find Planks (Any)
        int plankSlot = findItemSlot(client, ItemTags.PLANKS);
        if (plankSlot != -1) {
            click(client, plankSlot, 0, SlotActionType.PICKUP);
            click(client, 1, 1, SlotActionType.PICKUP); // Place 1 top-left
            click(client, 3, 1, SlotActionType.PICKUP); // Place 1 bottom-left

            // Return holding to inv (if any left) -> optimization: Check if empty hand?
            // Just click original slot to put back
            click(client, plankSlot, 0, SlotActionType.PICKUP);

            click(client, 0, 0, SlotActionType.QUICK_MOVE); // Craft Sticks
            client.player.closeHandledScreen();
            stepTimer = 20;
        }
    }

    public static void craftTable(MinecraftClient client) {
        if (stepTimer > 0) {
            stepTimer--;
            return;
        }
        if (client.currentScreen == null) {
            client.openScreen(new InventoryScreen(client.player));
            stepTimer = 15;
            return;
        }
        // Recipe: 4 Planks
        int plankSlot = findItemSlot(client, ItemTags.PLANKS);
        if (plankSlot != -1) {
            click(client, plankSlot, 0, SlotActionType.PICKUP);
            click(client, 1, 1, SlotActionType.PICKUP);
            click(client, 2, 1, SlotActionType.PICKUP);
            click(client, 3, 1, SlotActionType.PICKUP);
            click(client, 4, 1, SlotActionType.PICKUP);

            click(client, plankSlot, 0, SlotActionType.PICKUP); // Return rest

            click(client, 0, 0, SlotActionType.PICKUP); // Craft 1 Table
            client.player.closeHandledScreen();
            stepTimer = 20;
        }
    }

    public static void craftPickaxe(MinecraftClient client) {
        if (stepTimer > 0) {
            stepTimer--;
            return;
        }
        if (!(client.currentScreen instanceof CraftingScreen)) {
            return;
        }

        int plankSlot = findItemSlot(client, ItemTags.PLANKS);
        int stickSlot = findItemSlot(client, ItemTags.WOODEN_SLABS); // Wait, Sticks are not slabs?
        // Sticks do not have a dedicated broad tag in 1.16 usually, just 'sticks'.
        // But let's fallback to Item check for sticks if TAG fails or just use
        // Items.STICK check.
        // Actually ItemTags.SIGNS? No. Items.STICK is unique enough.

        // Correct way to find sticks:
        stickSlot = findItemSlot(client, Items.STICK);

        if (plankSlot != -1 && stickSlot != -1) {
            // Planks Top Row
            click(client, plankSlot, 0, SlotActionType.PICKUP);
            click(client, 1, 1, SlotActionType.PICKUP);
            click(client, 2, 1, SlotActionType.PICKUP);
            click(client, 3, 1, SlotActionType.PICKUP);
            click(client, plankSlot, 0, SlotActionType.PICKUP); // Return

            // Sticks Middle Col
            click(client, stickSlot, 0, SlotActionType.PICKUP);
            click(client, 5, 1, SlotActionType.PICKUP);
            click(client, 8, 1, SlotActionType.PICKUP);
            click(client, stickSlot, 0, SlotActionType.PICKUP); // Return

            click(client, 0, 0, SlotActionType.QUICK_MOVE);
            client.player.closeHandledScreen();
            stepTimer = 20;
        }
    }

    public static void craftStonePickaxe(MinecraftClient client) {
        if (stepTimer > 0) {
            stepTimer--;
            return;
        }
        if (!(client.currentScreen instanceof CraftingScreen))
            return;

        int cobbleSlot = findItemSlot(client, Items.COBBLESTONE); // OR ItemTags.STONE_TOOL_MATERIALS implies
                                                                  // cobble/blackstone
        if (cobbleSlot == -1)
            cobbleSlot = findItemSlot(client, ItemTags.STONE_TOOL_MATERIALS);

        int stickSlot = findItemSlot(client, Items.STICK);

        if (cobbleSlot != -1 && stickSlot != -1) {
            click(client, cobbleSlot, 0, SlotActionType.PICKUP);
            click(client, 1, 1, SlotActionType.PICKUP);
            click(client, 2, 1, SlotActionType.PICKUP);
            click(client, 3, 1, SlotActionType.PICKUP);
            click(client, cobbleSlot, 0, SlotActionType.PICKUP);

            click(client, stickSlot, 0, SlotActionType.PICKUP);
            click(client, 5, 1, SlotActionType.PICKUP);
            click(client, 8, 1, SlotActionType.PICKUP);
            click(client, stickSlot, 0, SlotActionType.PICKUP);

            click(client, 0, 0, SlotActionType.QUICK_MOVE);
            client.player.closeHandledScreen();
            stepTimer = 20;
        }
    }

    private static void click(MinecraftClient client, int slotId, int button, SlotActionType action) {
        if (client.interactionManager != null && client.player != null) {
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, slotId, button, action,
                    client.player);
        }
    }

    private static int findItemSlot(MinecraftClient client, Item item) {
        if (client.player == null || client.player.inventory == null)
            return -1;
        for (int i = 9; i < 45; i++) {
            if (client.player.currentScreenHandler.getSlot(i).getStack().getItem() == item)
                return i;
        }
        return -1;
    }

    // Helper for Tags
    private static int findItemSlot(MinecraftClient client, Tag<Item> tag) {
        if (client.player == null || client.player.inventory == null)
            return -1;
        for (int i = 9; i < 45; i++) {
            if (tag.contains(client.player.currentScreenHandler.getSlot(i).getStack().getItem()))
                return i;
        }
        return -1;
    }
}
