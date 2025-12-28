package com.speedrun.bot.mixin;

import com.speedrun.bot.render.ESPRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to hook into world rendering for ESP.
 */
@Mixin(GameRenderer.class)
public class WorldRenderMixin {

    @Inject(method = "renderWorld", at = @At("RETURN"))
    private void onRenderWorld(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        ESPRenderer.render(matrices, tickDelta);
    }
}
