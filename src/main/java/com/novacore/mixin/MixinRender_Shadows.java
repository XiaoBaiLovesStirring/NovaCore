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
 * <p>
 * 目标类: net.minecraft.client.renderer.entity.Render
 * 注入方法: func_76976_a (doRenderShadowAndFire) — HEAD 注入，按需禁用阴影
 * 委托: NovaRenderAggression.shouldDisableShadows()
 * </p>
 */
@Mixin(Render.class)
public class MixinRender_Shadows {

    /**
     * 注入标志，确保日志只打印一次
     */
    @Unique
    private static boolean injected = false;

    /**
     * 注入到 Render.func_76976_a (doRenderShadowAndFire) 方法 HEAD
     * <p>
     * 调用 NovaRenderAggression.shouldDisableShadows() 判断是否禁用阴影渲染
     * 如果返回 true，则通过 CallbackInfo.cancel() 取消阴影渲染
     * </p>
     */
    @Inject(method = "func_76976_a", at = @At("HEAD"), cancellable = true)
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