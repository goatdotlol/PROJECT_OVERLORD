package com.speedrun.bot.systems;

import com.speedrun.bot.utils.DebugLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class CraftingControl {

    private static int stepTimer = 0;

    /**
     * Tries to craft oak planks from logs in the 2x2 grid.
     */
    public static void craftPlanks(MinecraftClient client) {
        if (stepTimer > 0) {
            stepTimer--;
            return;
        } // Cooldown

        if (client.currentScreen == null) {
            client.openScreen(new InventoryScreen(client.player));
            stepTimer = 10;
            return;
        }

        // Assume we have logs. Click them into slot 1 (Top Left of 2x2)
        // 2x2 Crafting slots in InventoryContainer are 1, 2, 3, 4. Output is 0.
        // Main Inv starts at 9.

        // Simple logic: Find log in inv, click it, click slot 1, shift-click output.
        // Note: Slot IDs are container specific.

        // For now, let's implement a very basic automation:
        // 1. Find Log
        // 2. Pickup
        // 3. Place in Slot 1
        // 4. Shift click Slot 0

        // (Simplified implementation for space - real implementation needs exact slot
        // IDs)
        int logSlot = findItemSlot(client, Items.OAK_LOG); // Naive helper
        if (logSlot != -1) {
            click(client, logSlot, 0, SlotActionType.PICKUP); // Pick up
            click(client, 1, 0, SlotActionType.PICKUP); // Place in grid
            click(client, 0, 0, SlotActionType.QUICK_MOVE); // Craft all
            client.player.closeHandledScreen();
            stepTimer = 20;
        }
    }

    public static void craftSticks(MinecraftClient client) {
        if (stepTimer > 0) {
            stepTimer--;
            return;
        }
        // Recipe: 2 Planks (Vertical)
        if (client.currentScreen == null) {
            client.openScreen(new InventoryScreen(client.player));
            stepTimer = 10;
            return;
        }
        // Naive: Planks in slot 1 and 3 (top left, bottom left of 2x2)
        click(client, findItemSlot(client, Items.OAK_PLANKS), 0, SlotActionType.PICKUP);
        click(client, 1, 1, SlotActionType.PICKUP); // Place 1
        click(client, 3, 1, SlotActionType.PICKUP); // Place 1
        click(client, 0, 0, SlotActionType.QUICK_MOVE); // Craft
        client.player.closeHandledScreen();
        stepTimer = 20;
    }

    public static void craftTable(MinecraftClient client) {
        if (stepTimer > 0) {
            stepTimer--;
            return;
        }
        if (client.currentScreen == null) {
            client.openScreen(new InventoryScreen(client.player));
            stepTimer = 10;
            return;
        }
        // Recipe: 4 Planks (2x2)
        int plankSlot = findItemSlot(client, Items.OAK_PLANKS);
        click(client, plankSlot, 0, SlotActionType.PICKUP);
        click(client, 1, 1, SlotActionType.PICKUP);
        click(client, 2, 1, SlotActionType.PICKUP);
        click(client, 3, 1, SlotActionType.PICKUP);
        click(client, 4, 1, SlotActionType.PICKUP);
        click(client, 0, 0, SlotActionType.PICKUP); // Craft 1
        client.player.closeHandledScreen();
        stepTimer = 20;
    }

    public static void craftPickaxe(MinecraftClient client) {
        if (stepTimer > 0) {
            stepTimer--;
            return;
        }
        // Needs Crafting Table Screen
        if (!(client.currentScreen instanceof CraftingScreen)) {
            // GoalEngine handles opening the table
            return;
        }
        // Recipe: 3 Planks top, 2 Sticks middle
        // Slots 1,2,3 (Planks), 5,8 (Sticks)
        int plankSlot = findItemSlot(client, Items.OAK_PLANKS);
        int stickSlot = findItemSlot(client, Items.STICK);

        // Place Planks
        click(client, plankSlot, 0, SlotActionType.PICKUP);
        click(client, 1, 1, SlotActionType.PICKUP);
        click(client, 2, 1, SlotActionType.PICKUP);
        click(client, 3, 1, SlotActionType.PICKUP);

        // Return remainder if any
        click(client, plankSlot, 0, SlotActionType.PICKUP);

        // Place Sticks
        click(client, stickSlot, 0, SlotActionType.PICKUP);
        click(client, 5, 1, SlotActionType.PICKUP);
        click(client, 8, 1, SlotActionType.PICKUP);

        click(client, 0, 0, SlotActionType.QUICK_MOVE);
        client.player.closeHandledScreen();
        stepTimer = 20;
    }

    public static void craftStonePickaxe(MinecraftClient client) {
        if (stepTimer > 0) {
            stepTimer--;
            return;
        }
        if (!(client.currentScreen instanceof CraftingScreen)) {
            return; // GoalEngine opens table
        }
        // Recipe: 3 Cobble top, 2 Sticks middle
        int cobbleSlot = findItemSlot(client, Items.COBBLESTONE);
        int stickSlot = findItemSlot(client, Items.STICK);

        if (cobbleSlot == -1 || stickSlot == -1)
            return;

        // Place Cobble
        click(client, cobbleSlot, 0, SlotActionType.PICKUP);
        click(client, 1, 1, SlotActionType.PICKUP);
        click(client, 2, 1, SlotActionType.PICKUP);
        click(client, 3, 1, SlotActionType.PICKUP);
        click(client, cobbleSlot, 0, SlotActionType.PICKUP);

        // Place Sticks
        click(client, stickSlot, 0, SlotActionType.PICKUP);
        click(client, 5, 1, SlotActionType.PICKUP);
        click(client, 8, 1, SlotActionType.PICKUP);

        click(client, 0, 0, SlotActionType.QUICK_MOVE);
        client.player.closeHandledScreen();
        stepTimer = 20;
    }

    private static void click(MinecraftClient client, int slotId, int button, SlotActionType action) {
        if (client.interactionManager != null && client.player != null) {
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, slotId, button, action,
                    client.player);
        }
    }

    private static int findItemSlot(MinecraftClient client, net.minecraft.item.Item item) {
        if (client.player == null)
            return -1;
        if (client.player.inventory == null)
            return -1;
        // Check main inventory slots (usually index 9 to 35, plus hotbar 0-8. In
        // Container, hotbar is 36-44 usually)
        // Accessing via player.inventory directly is safer for finding the item, but
        // for clicking we need the slot ID.
        // For PlayerScreenHandler: 0=Craft, 1-4=Craft, 5=Helm... 9-35=Store,
        // 36-44=Hotbar.
        // Assuming we are in Player Screen (Survival Inventory)

        for (int i = 9; i < 45; i++) {
            if (client.player.currentScreenHandler.getSlot(i).getStack().getItem() == item)
                return i;
        }
        return -1;
    }
}
