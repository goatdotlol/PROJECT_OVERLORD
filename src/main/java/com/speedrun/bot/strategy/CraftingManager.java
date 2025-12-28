package com.speedrun.bot.strategy;

import com.speedrun.bot.utils.DebugLogger;
import com.speedrun.bot.utils.InventoryScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/**
 * CraftingManager - Handles automatic crafting of tools and basic items.
 */
public class CraftingManager {

    private static int craftingCooldown = 0;

    public static void tick(MinecraftClient client) {
        if (craftingCooldown > 0) {
            craftingCooldown--;
            return;
        }
    }

    public static void craftInInventory(MinecraftClient client, String target) {
        if (client.player == null || craftingCooldown > 0)
            return;
        PlayerScreenHandler handler = client.player.playerScreenHandler;

        switch (target) {
            case "PLANKS":
                int logSlot = findLogSlot(client);
                if (logSlot != -1) {
                    DebugLogger.log("[CRAFT] Logs -> Planks");
                    clickSlot(client, handler.syncId, logSlot, 0, SlotActionType.PICKUP);
                    clickSlot(client, handler.syncId, 1, 0, SlotActionType.PICKUP); // Crafting grid 1
                    clickSlot(client, handler.syncId, 0, 0, SlotActionType.QUICK_MOVE); // Take output
                    craftingCooldown = 20;
                }
                break;
            case "STICKS":
                if (InventoryScanner.hasPlanks()) {
                    DebugLogger.log("[CRAFT] Planks -> Sticks");
                    int plankSlot = findPlankSlot(client);
                    clickSlot(client, handler.syncId, plankSlot, 0, SlotActionType.PICKUP);
                    clickSlot(client, handler.syncId, 1, 0, SlotActionType.PICKUP);
                    clickSlot(client, handler.syncId, 2, 0, SlotActionType.PICKUP);
                    clickSlot(client, handler.syncId, 0, 0, SlotActionType.QUICK_MOVE);
                    craftingCooldown = 20;
                }
                break;
        }
    }

    private static int findLogSlot(MinecraftClient client) {
        for (int i = 9; i < 45; i++) {
            Item item = client.player.inventory.getStack(i).getItem();
            if (item.toString().contains("log"))
                return i;
        }
        return -1;
    }

    private static int findPlankSlot(MinecraftClient client) {
        for (int i = 9; i < 45; i++) {
            Item item = client.player.inventory.getStack(i).getItem();
            if (item.toString().contains("planks"))
                return i;
        }
        return -1;
    }

    private static void clickSlot(MinecraftClient client, int syncId, int slotId, int button,
            SlotActionType actionType) {
        if (client.interactionManager == null)
            return;
        client.interactionManager.clickSlot(syncId, slotId, button, actionType, client.player);
    }
}
