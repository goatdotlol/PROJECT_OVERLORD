package com.speedrun.bot.gui;

import com.speedrun.bot.utils.DebugLogger;
import com.speedrun.bot.strategy.OverworldManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public class ConfigScreen extends Screen {

    private ButtonWidget overworldButton;
    private ButtonWidget netherButton;
    private ButtonWidget endButton;
    private ButtonWidget scanButton;

    public ConfigScreen() {
        super(new LiteralText("Project OVERLORD - Ghost Engine"));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int startY = this.height / 4;

        // Button: 7 Sexy Iron (Overworld Strategy)
        overworldButton = new ButtonWidget(centerX - 100, startY, 200, 20,
                getOverworldButtonText(), (button) -> {
                    OverworldManager.toggle();
                    button.setMessage(getOverworldButtonText());
                    this.client.openScreen(null); // Close GUI to start
                });
        this.addButton(overworldButton);

        // Button: Rescan Now
        scanButton = new ButtonWidget(centerX - 100, startY + 24, 200, 20,
                new LiteralText("[SCAN NOW] Force Rescan"), (button) -> {
                    if (!OverworldManager.isActive()) {
                        OverworldManager.toggle(); // Enable if not active
                    }
                    DebugLogger.log("[Ghost] Forcing rescan...");
                    this.client.openScreen(null);
                });
        this.addButton(scanButton);

        // Button: Show Status
        this.addButton(new ButtonWidget(centerX - 100, startY + 48, 200, 20,
                new LiteralText("[STATUS] Log Current Target"), (button) -> {
                    showStatus();
                }));

        // Button: Nether Mode (Placeholder)
        netherButton = new ButtonWidget(centerX - 100, startY + 80, 200, 20,
                new LiteralText("Nether Rush [NOT IMPLEMENTED]"), (button) -> {
                    DebugLogger.log("[Ghost] Nether Rush not implemented yet!");
                });
        this.addButton(netherButton);

        // Button: End Mode (Placeholder)
        endButton = new ButtonWidget(centerX - 100, startY + 104, 200, 20,
                new LiteralText("End Game [NOT IMPLEMENTED]"), (button) -> {
                    DebugLogger.log("[Ghost] End Game not implemented yet!");
                });
        this.addButton(endButton);

        // Button: Toggle ESP
        this.addButton(new ButtonWidget(centerX - 100, startY + 128, 200, 20,
                new LiteralText("[ESP] Toggle Target Highlight"), (button) -> {
                    com.speedrun.bot.render.ESPRenderer.toggle();
                    boolean on = com.speedrun.bot.render.ESPRenderer.isEnabled();
                    DebugLogger.log(on ? "[Ghost] ESP: ON" : "[Ghost] ESP: OFF");
                }));

        // Close
        this.addButton(new ButtonWidget(centerX - 100, this.height - 40, 200, 20,
                new LiteralText("[X] Close"),
                (button) -> {
                    this.client.openScreen(null);
                }));
    }

    private LiteralText getOverworldButtonText() {
        boolean active = OverworldManager.isActive();
        if (active) {
            return new LiteralText("7 Sexy Iron: ON [Click to STOP]");
        } else {
            return new LiteralText("7 Sexy Iron: OFF [Click to START]");
        }
    }

    private void showStatus() {
        if (this.client.player == null)
            return;

        String state = OverworldManager.getState().toString();
        String targetType = OverworldManager.getTargetType();

        DebugLogger.log("--- Ghost Status ---");
        DebugLogger.log("State: " + state);
        DebugLogger.log("Active: " + (OverworldManager.isActive() ? "YES" : "NO"));

        if (!targetType.isEmpty()) {
            if (OverworldManager.getTargetPos() != null) {
                BlockPos pos = OverworldManager.getTargetPos();
                DebugLogger.log("Target: " + targetType + " at (" + pos.getX() + ", "
                        + pos.getY() + ", " + pos.getZ() + ")");
            } else if (OverworldManager.getTargetEntity() != null) {
                Entity ent = OverworldManager.getTargetEntity();
                DebugLogger.log("Target: " + targetType + " at (" + (int) ent.getX() + ", "
                        + (int) ent.getY() + ", " + (int) ent.getZ() + ")");
            }
        } else {
            DebugLogger.log("Target: None");
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        // Title
        this.drawCenteredString(matrices, this.textRenderer, "Project OVERLORD", this.width / 2, 15, 0xFFFFFF);
        this.drawCenteredString(matrices, this.textRenderer, "Ghost Engine v1.0", this.width / 2, 28, 0xAAAAAA);

        // Instructions
        this.drawCenteredString(matrices, this.textRenderer, "Press Right Shift to open this menu", this.width / 2,
                this.height - 55, 0x888888);

        super.render(matrices, mouseX, mouseY, delta);
    }
}
