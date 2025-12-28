package com.speedrun.bot.mixin;

import com.speedrun.bot.render.ESPRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to hook into WorldRenderer for 3D ESP.
 * Using WorldRenderer.render is more stable for in-world object highlighting.
 */
@Mixin(WorldRenderer.class)
public class WorldRenderMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline,
            net.minecraft.client.render.Camera camera, net.minecraft.client.render.GameRenderer gameRenderer,
            net.minecraft.client.render.LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f,
            CallbackInfo ci) {
        ESPRenderer.render(matrices, tickDelta);
    }
}
