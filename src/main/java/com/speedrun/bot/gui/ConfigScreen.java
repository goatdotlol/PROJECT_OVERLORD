package com.speedrun.bot.gui;

import com.speedrun.bot.utils.DebugLogger;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
import net.minecraft.client.util.math.MatrixStack;

public class ConfigScreen extends Screen {

    public ConfigScreen() {
        super(new LiteralText("Project OVERLORD Configuration"));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int startY = this.height / 4;

        // Button: 7 Sexy Iron
        this.addButton(new ButtonWidget(centerX - 100, startY, 200, 20,
                new LiteralText("Strategy: '7 Sexy Iron' [TOGGLE]"), (button) -> {
                    com.speedrun.bot.strategy.OverworldManager.toggle();
                }));

        // Button: Nether Mode
        this.addButton(new ButtonWidget(centerX - 100, startY + 24, 200, 20,
                new LiteralText("Strategy: Nether Rush [OFF]"), (button) -> {
                    DebugLogger.log("GUI: User clicked Nether Rush");
                }));

        // Button: End Mode
        this.addButton(new ButtonWidget(centerX - 100, startY + 48, 200, 20,
                new LiteralText("Strategy: End Game [OFF]"), (button) -> {
                    DebugLogger.log("GUI: User clicked End Game");
                }));

        // Debug
        this.addButton(new ButtonWidget(centerX - 100, startY + 96, 200, 20, new LiteralText("Toggle Debug Overlay"),
                (button) -> {
                    DebugLogger.log("GUI: Toggled Debug Overlay");
                }));

        // Close
        this.addButton(new ButtonWidget(centerX - 100, this.height - 40, 200, 20, new LiteralText("Save & Close"),
                (button) -> {
                    this.client.openScreen(null);
                }));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.drawCenteredString(matrices, this.textRenderer, this.title.getString(), this.width / 2, 20, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
    }
}
