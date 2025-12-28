package com.speedrun.bot.mixin;

import com.speedrun.bot.BotMain;
import com.speedrun.bot.systems.GoalEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void renderOverlay(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (!BotMain.isEnabled())
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.textRenderer == null)
            return;

        int color = 0x00FF00; // Green
        int y = 5;
        int x = client.getWindow().getScaledWidth() - 5;

        // Draw Strings (Right aligned)
        drawRightAligned(matrices, client, "§l[OVERLORD] v3.0", x, y, 0xFFD700);
        y += 10;

        // Timer
        long elapsed = System.currentTimeMillis() - BotMain.startTime;
        long sec = (elapsed / 1000) % 60;
        long min = (elapsed / (1000 * 60)) % 60;
        String timeStr = String.format("%02d:%02d", min, sec);
        drawRightAligned(matrices, client, "Time: §f" + timeStr, x, y, color);
        y += 10;

        // Goal
        drawRightAligned(matrices, client, "Goal: §b" + GoalEngine.currentState.name(), x, y, color);
        y += 10;

        // Status
        drawRightAligned(matrices, client, "Task: §7" + GoalEngine.status, x, y, color);
        y += 10;
    }

    private void drawRightAligned(MatrixStack matrices, MinecraftClient client, String text, int x, int y, int color) {
        int width = client.textRenderer.getWidth(text);
        client.textRenderer.drawWithShadow(matrices, text, x - width, y, color);
    }
}
