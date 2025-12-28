package com.speedrun.bot.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.speedrun.bot.strategy.OverworldManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

/**
 * ESP Renderer - Draws boxes through walls to highlight targets.
 */
public class ESPRenderer {

    private static boolean enabled = true;

    public static void toggle() {
        enabled = !enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Called from WorldRenderMixin to draw ESP boxes.
     */
    public static void render(MatrixStack matrices, float tickDelta) {
        if (!enabled)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return;

        // Get camera position
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();

        // Draw target from OverworldManager
        Entity targetEntity = OverworldManager.getTargetEntity();
        BlockPos targetPos = OverworldManager.getTargetPos();
        String targetType = OverworldManager.getTargetType();

        if (targetEntity != null) {
            // Draw box around entity
            Box box = targetEntity.getBoundingBox();
            drawBox(matrices, box, cameraPos, getColorForType(targetType));
        } else if (targetPos != null) {
            // Draw box around block
            Box box = new Box(targetPos);
            drawBox(matrices, box, cameraPos, getColorForType(targetType));
        }
    }

    private static float[] getColorForType(String type) {
        if (type.contains("GOLEM")) {
            return new float[] { 1.0f, 0.5f, 0.0f, 0.8f }; // Orange
        } else if (type.contains("VILLAGER")) {
            return new float[] { 0.0f, 1.0f, 0.0f, 0.8f }; // Green
        } else if (type.contains("BELL") || type.contains("Village")) {
            return new float[] { 1.0f, 1.0f, 0.0f, 0.8f }; // Yellow
        } else if (type.contains("CHEST") || type.contains("Shipwreck")) {
            return new float[] { 1.0f, 0.0f, 1.0f, 0.8f }; // Magenta
        } else if (type.contains("IRON")) {
            return new float[] { 1.0f, 1.0f, 1.0f, 0.8f }; // White
        } else {
            return new float[] { 0.0f, 1.0f, 1.0f, 0.8f }; // Cyan default
        }
    }

    private static void drawBox(MatrixStack matrices, Box box, Vec3d cameraPos, float[] color) {
        // Translate box relative to camera
        double x1 = box.minX - cameraPos.x;
        double y1 = box.minY - cameraPos.y;
        double z1 = box.minZ - cameraPos.z;
        double x2 = box.maxX - cameraPos.x;
        double y2 = box.maxY - cameraPos.y;
        double z2 = box.maxZ - cameraPos.z;

        // Setup OpenGL for line rendering
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.lineWidth(2.0f);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);

        float r = color[0];
        float g = color[1];
        float b = color[2];
        float a = color[3];

        // Bottom face
        buffer.vertex(x1, y1, z1).color(r, g, b, a).next();
        buffer.vertex(x2, y1, z1).color(r, g, b, a).next();

        buffer.vertex(x2, y1, z1).color(r, g, b, a).next();
        buffer.vertex(x2, y1, z2).color(r, g, b, a).next();

        buffer.vertex(x2, y1, z2).color(r, g, b, a).next();
        buffer.vertex(x1, y1, z2).color(r, g, b, a).next();

        buffer.vertex(x1, y1, z2).color(r, g, b, a).next();
        buffer.vertex(x1, y1, z1).color(r, g, b, a).next();

        // Top face
        buffer.vertex(x1, y2, z1).color(r, g, b, a).next();
        buffer.vertex(x2, y2, z1).color(r, g, b, a).next();

        buffer.vertex(x2, y2, z1).color(r, g, b, a).next();
        buffer.vertex(x2, y2, z2).color(r, g, b, a).next();

        buffer.vertex(x2, y2, z2).color(r, g, b, a).next();
        buffer.vertex(x1, y2, z2).color(r, g, b, a).next();

        buffer.vertex(x1, y2, z2).color(r, g, b, a).next();
        buffer.vertex(x1, y2, z1).color(r, g, b, a).next();

        // Vertical edges
        buffer.vertex(x1, y1, z1).color(r, g, b, a).next();
        buffer.vertex(x1, y2, z1).color(r, g, b, a).next();

        buffer.vertex(x2, y1, z1).color(r, g, b, a).next();
        buffer.vertex(x2, y2, z1).color(r, g, b, a).next();

        buffer.vertex(x2, y1, z2).color(r, g, b, a).next();
        buffer.vertex(x2, y2, z2).color(r, g, b, a).next();

        buffer.vertex(x1, y1, z2).color(r, g, b, a).next();
        buffer.vertex(x1, y2, z2).color(r, g, b, a).next();

        tessellator.draw();

        // Restore OpenGL state
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }
}
