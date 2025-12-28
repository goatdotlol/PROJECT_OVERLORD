package com.speedrun.bot.gui;

import com.speedrun.bot.utils.DebugLogger;
import com.speedrun.bot.strategy.OverworldManager;
import com.speedrun.bot.strategy.AutoSpeedrunManager;
import com.speedrun.bot.utils.InventoryScanner;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public class ConfigScreen extends Screen {

    private ButtonWidget passiveButton;
    private ButtonWidget autoButton;
    private ButtonWidget espButton;

    public ConfigScreen() {
        super(new LiteralText("Project OVERLORD - Ghost Engine"));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int startY = this.height / 4;

        // Button: Passive Scan
        passiveButton = new ButtonWidget(centerX - 100, startY, 200, 20,
                getPassiveButtonText(), (button) -> {
                    OverworldManager.toggle();
                    button.setMessage(getPassiveButtonText());
                });
        this.addButton(passiveButton);

        // Button: Auto Speedrun (Autonomous Mode)
        autoButton = new ButtonWidget(centerX - 100, startY + 24, 200, 20,
                getAutoButtonText(), (button) -> {
                    if (AutoSpeedrunManager.isActive()) {
                        AutoSpeedrunManager.stop();
                    } else {
                        AutoSpeedrunManager.start();
                    }
                    button.setMessage(getAutoButtonText());
                    this.client.openScreen(null); // Close to start
                });
        this.addButton(autoButton);

        // Button: Show Status
        this.addButton(new ButtonWidget(centerX - 100, startY + 54, 200, 20,
                new LiteralText("[STATUS] Log Info to Console"), (button) -> {
                    showStatus();
                }));

        // Button: Toggle ESP
        espButton = new ButtonWidget(centerX - 100, startY + 84, 200, 20,
                new LiteralText("[ESP] Toggle Highlighting"), (button) -> {
                    com.speedrun.bot.render.ESPRenderer.toggle();
                    DebugLogger.log("ESP: " + (com.speedrun.bot.render.ESPRenderer.isEnabled() ? "ON" : "OFF"));
                });
        this.addButton(espButton);

        // Close
        this.addButton(new ButtonWidget(centerX - 100, this.height - 40, 200, 20,
                new LiteralText("[X] Close"), (button) -> this.client.openScreen(null)));
    }

    private LiteralText getPassiveButtonText() {
        return new LiteralText("Passive Scan: " + (OverworldManager.isActive() ? "ON" : "OFF"));
    }

    private LiteralText getAutoButtonText() {
        return new LiteralText("Auto Speedrun: " + (AutoSpeedrunManager.isActive() ? "ENABLED" : "DISABLED"));
    }

    private void showStatus() {
        DebugLogger.log("--- Ghost Status ---");
        DebugLogger.log("Goal: " + AutoSpeedrunManager.getGoal());
        DebugLogger.log("Iron: " + InventoryScanner.getIronCount());
        DebugLogger.log("Wood: " + (InventoryScanner.hasWood() ? "YES" : "NO"));

        String target = AutoSpeedrunManager.getTargetType();
        if (target.isEmpty())
            target = OverworldManager.getTargetType();
        DebugLogger.log("Target: " + (target.isEmpty() ? "None" : target));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.drawCenteredString(matrices, this.textRenderer, "Project OVERLORD", this.width / 2, 15, 0xFFFFFF);
        this.drawCenteredString(matrices, this.textRenderer, "Ghost Engine v1.0", this.width / 2, 28, 0xAAAAAA);
        super.render(matrices, mouseX, mouseY, delta);
    }
}
