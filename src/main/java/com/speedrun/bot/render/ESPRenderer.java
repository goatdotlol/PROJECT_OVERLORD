package com.speedrun.bot.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.speedrun.bot.strategy.OverworldManager;
import com.speedrun.bot.strategy.AutoSpeedrunManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public class ESPRenderer {

    private static boolean enabled = true;

    public static void toggle() {
        enabled = !enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void render(MatrixStack matrices, float tickDelta) {
        if (!enabled)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return;

        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();

        // 1. Render Autonomous Target (Prioritized)
        if (AutoSpeedrunManager.isActive()) {
            renderTarget(matrices, AutoSpeedrunManager.getTargetEntity(), AutoSpeedrunManager.getTargetPos(),
                    AutoSpeedrunManager.getTargetType(), cameraPos, tickDelta, true);
        }

        // 2. Render Passive Scan Target
        if (OverworldManager.isActive()) {
            renderTarget(matrices, OverworldManager.getTargetEntity(), OverworldManager.getTargetPos(),
                    OverworldManager.getTargetType(), cameraPos, tickDelta, false);
        }
    }

    private static void renderTarget(MatrixStack matrices, Entity entity, BlockPos pos, String type, Vec3d cameraPos,
            float tickDelta, boolean isAuto) {
        if (entity != null) {
            double x = entity.prevX + (entity.getX() - entity.prevX) * tickDelta;
            double y = entity.prevY + (entity.getY() - entity.prevY) * tickDelta;
            double z = entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta;
            Box box = entity.getBoundingBox().offset(-entity.getX() + x, -entity.getY() + y, -entity.getZ() + z);
            drawBox(matrices, box, cameraPos, getColorForType(type, isAuto));
        } else if (pos != null) {
            drawBox(matrices, new Box(pos), cameraPos, getColorForType(type, isAuto));
        }
    }

    private static float[] getColorForType(String type, boolean isAuto) {
        float alpha = isAuto ? 1.0f : 0.5f; // Active target is more solid
        if (type.contains("WOOD"))
            return new float[] { 0.6f, 0.4f, 0.2f, alpha }; // Brown
        if (type.contains("GOLEM"))
            return new float[] { 1.0f, 0.5f, 0.0f, alpha };
        if (type.contains("VILLAGER"))
            return new float[] { 0.0f, 1.0f, 0.0f, alpha };
        if (type.contains("IRON"))
            return new float[] { 1.0f, 1.0f, 1.0f, alpha };
        if (type.contains("LAVA"))
            return new float[] { 1.0f, 0.2f, 0.0f, alpha };
        return new float[] { 0.0f, 1.0f, 1.0f, alpha };
    }

    private static void drawBox(MatrixStack matrices, Box box, Vec3d cameraPos, float[] color) {
        double x1 = box.minX - cameraPos.x, y1 = box.minY - cameraPos.y, z1 = box.minZ - cameraPos.z;
        double x2 = box.maxX - cameraPos.x, y2 = box.maxY - cameraPos.y, z2 = box.maxZ - cameraPos.z;

        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2.5f);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Matrix4f model = matrices.peek().getModel();

        buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
        float r = color[0], g = color[1], b = color[2], a = color[3];

        // Bottom
        vertex(buffer, model, x1, y1, z1, r, g, b, a);
        vertex(buffer, model, x2, y1, z1, r, g, b, a);
        vertex(buffer, model, x2, y1, z1, r, g, b, a);
        vertex(buffer, model, x2, y1, z2, r, g, b, a);
        vertex(buffer, model, x2, y1, z2, r, g, b, a);
        vertex(buffer, model, x1, y1, z2, r, g, b, a);
        vertex(buffer, model, x1, y1, z2, r, g, b, a);
        vertex(buffer, model, x1, y1, z1, r, g, b, a);
        // Top
        vertex(buffer, model, x1, y2, z1, r, g, b, a);
        vertex(buffer, model, x2, y2, z1, r, g, b, a);
        vertex(buffer, model, x2, y2, z1, r, g, b, a);
        vertex(buffer, model, x2, y2, z2, r, g, b, a);
        vertex(buffer, model, x2, y2, z2, r, g, b, a);
        vertex(buffer, model, x1, y2, z2, r, g, b, a);
        vertex(buffer, model, x1, y2, z2, r, g, b, a);
        vertex(buffer, model, x1, y2, z1, r, g, b, a);
        // Columns
        vertex(buffer, model, x1, y1, z1, r, g, b, a);
        vertex(buffer, model, x1, y2, z1, r, g, b, a);
        vertex(buffer, model, x2, y1, z1, r, g, b, a);
        vertex(buffer, model, x2, y2, z1, r, g, b, a);
        vertex(buffer, model, x2, y1, z2, r, g, b, a);
        vertex(buffer, model, x2, y2, z2, r, g, b, a);
        vertex(buffer, model, x1, y1, z2, r, g, b, a);
        vertex(buffer, model, x1, y2, z2, r, g, b, a);

        tessellator.draw();
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f model, double x, double y, double z, float r, float g,
            float b, float a) {
        buffer.vertex(model, (float) x, (float) y, (float) z).color(r, g, b, a).next();
    }
}
