package com.speedrun.bot.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.speedrun.bot.systems.AsyncChunkScanner;
import com.speedrun.bot.systems.InteractionControl;
import com.speedrun.bot.systems.PathingControl;
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
import java.util.List;

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

        // 0. Render Item Drops (Yellow) - Priority
        Entity drop = AsyncChunkScanner.getNearestItem();
        if (drop != null) {
            drawBox(matrices, drop.getBoundingBox(), cameraPos, new float[] { 1.0f, 1.0f, 0.0f, 0.8f });
        }

        // 1. Render Break Target (Red)
        BlockPos breakTarget = InteractionControl.getBreakTarget();
        if (breakTarget != null) {
            drawBox(matrices, new Box(breakTarget), cameraPos, new float[] { 1.0f, 0.0f, 0.0f, 0.5f });
        }

        // 2. Render Path (Blue Line)
        List<BlockPos> path = PathingControl.getCurrentPath();
        if (path != null && !path.isEmpty()) {
            drawPath(matrices, path, cameraPos, new float[] { 0.0f, 0.5f, 1.0f, 1.0f });
        }

        // 3. Render Golem (Iron Color)
        Entity golem = AsyncChunkScanner.getNearestGolem();
        if (golem != null && golem.isAlive()) {
            drawBox(matrices, golem.getBoundingBox(), cameraPos, new float[] { 0.8f, 0.8f, 0.8f, 0.5f });
        }

        // 4. Render Iron Ore (White Box) - Optional if debug needed
        BlockPos iron = AsyncChunkScanner.getNearestIron();
        if (iron != null) {
            drawBox(matrices, new Box(iron), cameraPos, new float[] { 0.9f, 0.6f, 0.5f, 0.3f });
        }
    }

    private static void drawPath(MatrixStack matrices, List<BlockPos> path, Vec3d cameraPos, float[] color) {
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(3.0f);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Matrix4f model = matrices.peek().getModel();

        buffer.begin(GL11.GL_LINE_STRIP, VertexFormats.POSITION_COLOR);

        for (BlockPos pos : path) {
            buffer.vertex(model, (float) (pos.getX() + 0.5 - cameraPos.x), (float) (pos.getY() + 0.5 - cameraPos.y),
                    (float) (pos.getZ() + 0.5 - cameraPos.z))
                    .color(color[0], color[1], color[2], color[3]).next();
        }

        tessellator.draw();
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
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
