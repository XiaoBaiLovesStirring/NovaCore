package com.novacore.mixin;

import com.novacore.asm.NovaRenderAggression;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinRender_Shadows — EXTREME 模块：渲染激进优化
 * 目标: Render.func_76976_a (doRenderShadowAndFire)
 */
@Mixin(Render.class)
public class MixinRender_Shadows {

    @Unique
    private static boolean injected = false;

    /**
     * 注入到 Render.func_76976_a (doRenderShadowAndFire) HEAD
     */
    @Inject(method = "func_76976_a", at = @At("HEAD"), cancellable = true, remap = false)
    private void onDoRenderShadowAndFire(Entity entity, double x, double y, double z,
                                          float shadowAlpha, float partialTicks, CallbackInfo ci) {
        if (!injected) {
            injected = true;
            System.out.println("[NovaCore][EXTREME] 渲染激进优化已注入");
        }

        if (NovaRenderAggression.shouldDisableShadows()) {
            ci.cancel();
        }
    }
}