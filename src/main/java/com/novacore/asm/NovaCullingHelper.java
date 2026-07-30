package com.novacore.asm;

import net.minecraft.client.renderer.culling.ICamera;

/**
 * 实体剔除桩 — 待实现
 * 注入到RenderGlobal.renderEntities的开头和结尾
 */
public class NovaCullingHelper {
    public static void beginCulling(ICamera camera) {
        throw new UnsupportedOperationException("[NovaCore] NovaCullingHelper not yet implemented");
    }

    public static void endCulling() {
        throw new UnsupportedOperationException("[NovaCore] NovaCullingHelper not yet implemented");
    }
}