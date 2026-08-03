package com.novacore.mixin;

import com.novacore.asm.NovaGLHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinRenderGlobal_OpenGL — OpenGL 状态缓存优化
 * 目标: RenderGlobal.func_174970_a (setupTerrain) / func_180447_b (renderCloudsFancy)
 */
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal_OpenGL {

    @Unique
    private static boolean injected = false;

    /**
     * 注入到 RenderGlobal.func_174970_a (setupTerrain) HEAD
     */
    @Inject(method = "func_174970_a", at = @At("HEAD"), remap = false)
    private void onSetupTerrain(Entity renderViewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator, CallbackInfo ci) {
        if (!injected) {
            injected = true;
            System.out.println("[NovaCore] OpenGL 状态缓存优化已注入");
        }
        NovaGLHelper.onSetupTerrain((RenderGlobal) (Object) this);
    }

    /**
     * 注入到 RenderGlobal.func_180447_b (renderCloudsFancy) RETURN
     */
    @Inject(method = "func_180447_b", at = @At("RETURN"), remap = false)
    private void onRenderCloudsFancyReturn(CallbackInfo ci) {
        NovaGLHelper.resetGLCache();
    }
}