package com.novacore.mixin;

import com.novacore.asm.NovaCullingHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinRenderGlobal_EntityCulling — 实体剔除引擎：在 RenderGlobal 渲染实体时注入视锥剔除
 * <p>
 * 目标类: net.minecraft.client.renderer.RenderGlobal
 * 注入方法: renderEntities (func_180446_a) — HEAD 注入 beginCulling，RETURN 注入 endCulling
 * 委托: NovaCullingHelper.beginCulling() / NovaCullingHelper.endCulling()
 * </p>
 */
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal_EntityCulling {

    /**
     * 注入标志，确保日志只打印一次
     */
    @Unique
    private static boolean injected = false;

    /**
     * 注入到 RenderGlobal.renderEntities (func_180446_a) 方法 HEAD
     * 调用 NovaCullingHelper.beginCulling(camera) 开始剔除
     */
    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void onRenderEntitiesHead(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        if (!injected) {
            injected = true;
            System.out.println("[NovaCore] 实体剔除引擎已注入");
        }

        NovaCullingHelper.beginCulling(camera);
    }

    /**
     * 注入到 RenderGlobal.renderEntities (func_180446_a) 方法 RETURN 前
     * 调用 NovaCullingHelper.endCulling() 结束剔除
     */
    @Inject(method = "renderEntities", at = @At("RETURN"))
    private void onRenderEntitiesReturn(CallbackInfo ci) {
        NovaCullingHelper.endCulling();
    }
}