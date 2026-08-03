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
 * MixinRenderGlobal_OpenGL — OpenGL 状态缓存优化：管理地形渲染和云渲染的 GL 状态
 * <p>
 * 目标类: net.minecraft.client.renderer.RenderGlobal
 * 注入方法:
 *   setupTerrain (func_174970_a) — HEAD 注入 onSetupTerrain
 *   renderCloudsFancy (func_180447_b) — RETURN 注入 resetGLCache
 * 委托: NovaGLHelper.onSetupTerrain() / NovaGLHelper.resetGLCache()
 * </p>
 */
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal_OpenGL {

    /**
     * 注入标志，确保日志只打印一次
     */
    @Unique
    private static boolean injected = false;

    /**
     * 注入到 RenderGlobal.setupTerrain (func_174970_a) 方法 HEAD
     * 调用 NovaGLHelper.onSetupTerrain(this) 初始化 GL 状态
     */
    @Inject(method = "setupTerrain", at = @At("HEAD"))
    private void onSetupTerrain(Entity renderViewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator, CallbackInfo ci) {
        if (!injected) {
            injected = true;
            System.out.println("[NovaCore] OpenGL 状态缓存优化已注入");
        }

        NovaGLHelper.onSetupTerrain((RenderGlobal) (Object) this);
    }

    /**
     * 注入到 RenderGlobal.renderCloudsFancy (func_180447_b) 方法 RETURN 前
     * 调用 NovaGLHelper.resetGLCache() 重置 GL 缓存状态
     */
    @Inject(method = "renderCloudsFancy", at = @At("RETURN"))
    private void onRenderCloudsFancyReturn(CallbackInfo ci) {
        NovaGLHelper.resetGLCache();
    }
}