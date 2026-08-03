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
 * MixinRenderGlobal_EntityCulling — 实体剔除引擎
 * 目标: RenderGlobal.func_180446_a (renderEntities)
 */
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal_EntityCulling {

    @Unique
    private static boolean injected = false;

    /**
     * 注入到 RenderGlobal.func_180446_a (renderEntities) HEAD
     */
    @Inject(method = "func_180446_a", at = @At("HEAD"), remap = false)
    private void onRenderEntitiesHead(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        if (!injected) {
            injected = true;
            System.out.println("[NovaCore] 实体剔除引擎已注入");
        }
        NovaCullingHelper.beginCulling(camera);
    }

    /**
     * 注入到 RenderGlobal.func_180446_a (renderEntities) RETURN
     */
    @Inject(method = "func_180446_a", at = @At("RETURN"), remap = false)
    private void onRenderEntitiesReturn(CallbackInfo ci) {
        NovaCullingHelper.endCulling();
    }
}