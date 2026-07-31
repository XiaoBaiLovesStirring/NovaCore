package com.novacore.asm;

import net.minecraft.client.renderer.RenderGlobal;

/**
 * OpenGL管线优化 — GL状态缓存
 *
 * 原版每次渲染块层时都会重复设置相同的 GL 状态。
 * 通过缓存当前 GL 状态避免冗余的 glEnable/glDisable 调用。
 */
public class NovaGLHelper {

    // GL 状态缓存标志
    private static boolean blendEnabled;
    private static boolean depthEnabled;
    private static boolean alphaEnabled;
    private static boolean stateInitialized;

    /**
     * 在 setupTerrain 开头注入 — 初始化 GL 状态缓存
     */
    public static void onSetupTerrain(RenderGlobal rg) {
        stateInitialized = false;
        blendEnabled = false;
        depthEnabled = false;
        alphaEnabled = false;
    }

    /**
     * 在 renderClouds 末尾注入 — 重置 GL 状态缓存
     */
    public static void resetGLCache() {
        stateInitialized = false;
    }
}