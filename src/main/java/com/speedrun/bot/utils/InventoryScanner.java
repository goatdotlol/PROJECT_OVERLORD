package com.speedrun.bot.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.tag.ItemTags;

public class InventoryScanner {

    public static int countItem(Item item) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return 0;
        int count = 0;
        for (int i = 0; i < client.player.inventory.size(); i++) {
            ItemStack stack = client.player.inventory.getStack(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static boolean hasWood() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return false;
        for (int i = 0; i < client.player.inventory.size(); i++) {
            ItemStack stack = client.player.inventory.getStack(i);
            if (stack.getItem().isIn(ItemTags.LOGS))
                return true;
        }
        return false;
    }

    public static boolean hasPlanks() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return false;
        for (int i = 0; i < client.player.inventory.size(); i++) {
            ItemStack stack = client.player.inventory.getStack(i);
            if (stack.getItem().isIn(ItemTags.PLANKS))
                return true;
        }
        return false;
    }

    public static boolean hasPickaxe() {
        return countTool("pickaxe") > 0;
    }

    public static boolean hasAxe() {
        return countTool("axe") > 0;
    }

    private static int countTool(String type) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return 0;
        int count = 0;
        for (int i = 0; i < client.player.inventory.size(); i++) {
            ItemStack stack = client.player.inventory.getStack(i);
            String name = stack.getItem().toString().toLowerCase();
            if (name.contains(type))
                count++;
        }
        return count;
    }

    public static boolean hasWorkbench() {
        return countItem(Items.CRAFTING_TABLE) > 0;
    }

    public static int getIronCount() {
        return countItem(Items.IRON_INGOT);
    }
}
