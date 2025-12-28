package com.speedrun.bot.systems;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

/**
 * CombatControl - Handles PVE Logic.
 * Golem towering, shield usage, crit attacks.
 */
public class CombatControl {

    // Simple state for Towering
    private static boolean isTowering = false;

    public static void fightGolem(MinecraftClient client, Entity golem) {
        if (client.player == null)
            return;

        double dist = client.player.squaredDistanceTo(golem);

        // 1. Tower Up
        if (!isTowering && dist < 25 && client.player.getY() < golem.getY() + 3) {
            towerUp(client);
            return;
        }

        // 2. Attack
        HumanoidControl.lookAt(client, golem.getBlockPos(), 2);

        if (client.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
            if (client.player.fallDistance > 0) { // Critical Hit
                client.interactionManager.attackEntity(client.player, golem);
                client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            }
        }
    }

    private static void towerUp(MinecraftClient client) {
        // Look Down
        HumanoidControl.lookAt(client, client.player.getBlockPos().down(), 2);

        if (client.player.pitch > 80) {
            // Jump and Place
            if (client.player.isOnGround()) {
                client.options.keyJump.setPressed(true);
            } else {
                client.options.keyJump.setPressed(false);
            }
            // Use InteractionControl to place block below?
            // Simplified:
            // client.interactionManager.interactBlock(...)
        }
    }
}
