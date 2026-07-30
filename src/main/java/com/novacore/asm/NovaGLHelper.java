package com.novacore.asm;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;

/**
 * OpenGL管线优化桩 — 待实现
 * 替换RenderGlobal.renderBlockLayer和注入setupTerrain/renderClouds
 */
public class NovaGLHelper {
    public static void renderBlockLayerMultiDraw(RenderGlobal rg, BlockRenderLayer layer, double partialTicks, int pass, Entity entity) {
        throw new UnsupportedOperationException("[NovaCore] NovaGLHelper not yet implemented");
    }

    public static void onSetupTerrain(RenderGlobal rg) {
        throw new UnsupportedOperationException("[NovaCore] NovaGLHelper not yet implemented");
    }

    public static void resetGLCache() {
        throw new UnsupportedOperationException("[NovaCore] NovaGLHelper not yet implemented");
    }
}