package com.speedrun.bot.gui;

import com.speedrun.bot.utils.DebugLogger;
import com.speedrun.bot.strategy.OverworldManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
import net.minecraft.client.util.math.MatrixStack;

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
                new LiteralText("§b[SCAN NOW] Force Rescan"), (button) -> {
                    if (!OverworldManager.isActive()) {
                        OverworldManager.toggle(); // Enable if not active
                    }
                    this.client.player.sendChatMessage("§e[Ghost] Forcing rescan...");
                    this.client.openScreen(null);
                });
        this.addButton(scanButton);

        // Button: Show Status
        this.addButton(new ButtonWidget(centerX - 100, startY + 48, 200, 20,
                new LiteralText("§a[STATUS] Show Current Target"), (button) -> {
                    showStatus();
                }));

        // Button: Nether Mode (Placeholder)
        netherButton = new ButtonWidget(centerX - 100, startY + 80, 200, 20,
                new LiteralText("§7Nether Rush [NOT IMPLEMENTED]"), (button) -> {
                    if (this.client.player != null) {
                        this.client.player.sendChatMessage("§c[Ghost] Nether Rush not implemented yet!");
                    }
                });
        this.addButton(netherButton);

        // Button: End Mode (Placeholder)
        endButton = new ButtonWidget(centerX - 100, startY + 104, 200, 20,
                new LiteralText("§7End Game [NOT IMPLEMENTED]"), (button) -> {
                    if (this.client.player != null) {
                        this.client.player.sendChatMessage("§c[Ghost] End Game not implemented yet!");
                    }
                });
        this.addButton(endButton);

        // Close
        this.addButton(new ButtonWidget(centerX - 100, this.height - 40, 200, 20,
                new LiteralText("§f[X] Close"),
                (button) -> {
                    this.client.openScreen(null);
                }));
    }

    private LiteralText getOverworldButtonText() {
        boolean active = OverworldManager.isActive();
        if (active) {
            return new LiteralText("§a§l7 Sexy Iron: ON §r[Click to STOP]");
        } else {
            return new LiteralText("§c7 Sexy Iron: OFF §r[Click to START]");
        }
    }

    private void showStatus() {
        if (this.client.player == null)
            return;

        String state = OverworldManager.getState().toString();
        String targetType = OverworldManager.getTargetType();

        this.client.player.sendChatMessage("§e--- Ghost Status ---");
        this.client.player.sendChatMessage("§7State: §f" + state);
        this.client.player.sendChatMessage("§7Active: " + (OverworldManager.isActive() ? "§aYES" : "§cNO"));

        if (!targetType.isEmpty()) {
            if (OverworldManager.getTargetPos() != null) {
                var pos = OverworldManager.getTargetPos();
                this.client.player.sendChatMessage("§7Target: §6" + targetType + " §fat (" + pos.getX() + ", "
                        + pos.getY() + ", " + pos.getZ() + ")");
            } else if (OverworldManager.getTargetEntity() != null) {
                var ent = OverworldManager.getTargetEntity();
                this.client.player.sendChatMessage("§7Target: §6" + targetType + " §fat (" + (int) ent.getX() + ", "
                        + (int) ent.getY() + ", " + (int) ent.getZ() + ")");
            }
        } else {
            this.client.player.sendChatMessage("§7Target: §8None");
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        // Title
        this.drawCenteredString(matrices, this.textRenderer, "§l§6Project OVERLORD", this.width / 2, 15, 0xFFFFFF);
        this.drawCenteredString(matrices, this.textRenderer, "§7Ghost Engine v1.0", this.width / 2, 28, 0xAAAAAA);

        // Instructions
        this.drawCenteredString(matrices, this.textRenderer, "§8Press Right Shift to open this menu", this.width / 2,
                this.height - 55, 0x888888);

        super.render(matrices, mouseX, mouseY, delta);
    }
}
