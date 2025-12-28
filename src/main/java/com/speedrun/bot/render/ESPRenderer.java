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
 * Fixed to use stable camera offsets to prevent "floating" with camera
 * movement.
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
     * Called from WorldRenderMixin with the current view matrices.
     */
    public static void render(MatrixStack matrices, float tickDelta) {
        if (!enabled)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return;

        // Get stable camera position for this frame
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();

        // Target from managers
        Entity targetEntity = OverworldManager.getTargetEntity();
        BlockPos targetPos = OverworldManager.getTargetPos();
        String targetType = OverworldManager.getTargetType();

        if (targetEntity != null) {
            // Predict position with tickDelta for smooth tracking
            double x = targetEntity.prevX + (targetEntity.getX() - targetEntity.prevX) * tickDelta;
            double y = targetEntity.prevY + (targetEntity.getY() - targetEntity.prevY) * tickDelta;
            double z = targetEntity.prevZ + (targetEntity.getZ() - targetEntity.prevZ) * tickDelta;

            Box box = targetEntity.getBoundingBox().offset(-targetEntity.getX() + x, -targetEntity.getY() + y,
                    -targetEntity.getZ() + z);
            drawBox(matrices, box, cameraPos, getColorForType(targetType));
        } else if (targetPos != null) {
            Box box = new Box(targetPos);
            drawBox(matrices, box, cameraPos, getColorForType(targetType));
        }
    }

    private static float[] getColorForType(String type) {
        if (type.contains("GOLEM"))
            return new float[] { 1.0f, 0.5f, 0.0f, 1.0f }; // Orange
        if (type.contains("VILLAGER"))
            return new float[] { 0.0f, 1.0f, 0.0f, 1.0f }; // Green
        if (type.contains("BELL") || type.contains("VILLAGE"))
            return new float[] { 1.0f, 1.0f, 0.0f, 1.0f }; // Yellow
        if (type.contains("SHIPWRECK"))
            return new float[] { 1.0f, 0.0f, 1.0f, 1.0f }; // Magenta
        if (type.contains("IRON"))
            return new float[] { 1.0f, 1.0f, 1.0f, 1.0f }; // White
        if (type.contains("LAVA"))
            return new float[] { 1.0f, 0.2f, 0.0f, 1.0f }; // Red-Orange
        return new float[] { 0.0f, 1.0f, 1.0f, 1.0f }; // Cyan default
    }

    private static void drawBox(MatrixStack matrices, Box box, Vec3d cameraPos, float[] color) {
        // Translation relative to camera
        double x1 = box.minX - cameraPos.x;
        double y1 = box.minY - cameraPos.y;
        double z1 = box.minZ - cameraPos.z;
        double x2 = box.maxX - cameraPos.x;
        double y2 = box.maxY - cameraPos.y;
        double z2 = box.maxZ - cameraPos.z;

        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2.5f);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // Use current matrix stack translation
        matrices.push();

        buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);

        float r = color[0];
        float g = color[1];
        float b = color[2];
        float a = color[3];

        // Draw 12 lines of the cube
        // Bottom
        line(buffer, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(buffer, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(buffer, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(buffer, x1, y1, z2, x1, y1, z1, r, g, b, a);
        // Top
        line(buffer, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(buffer, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(buffer, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(buffer, x1, y2, z2, x1, y2, z1, r, g, b, a);
        // Columns
        line(buffer, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(buffer, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(buffer, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(buffer, x1, y1, z2, x1, y2, z2, r, g, b, a);

        tessellator.draw();

        matrices.pop();

        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private static void line(BufferBuilder buffer, double x1, double y1, double z1, double x2, double y2, double z2,
            float r, float g, float b, float a) {
        buffer.vertex(x1, y1, z1).color(r, g, b, a).next();
        buffer.vertex(x2, y2, z2).color(r, g, b, a).next();
    }
}
