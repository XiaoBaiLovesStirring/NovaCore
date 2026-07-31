package com.novacore.asm;

import net.minecraft.client.renderer.RenderGlobal;

/**
 * OpenGL管线优化 — GL状态缓存 + 批量绘制提示
 *
 * 原版每次渲染块层时都会重复设置相同的 GL 状态。
 * 通过缓存当前 GL 状态避免冗余的 glEnable/glDisable 调用。
 *
 * 增强：
 *   - 跟踪更多 GL 状态：blend, depth, alpha, cull, texture, light
 *   - 批量绘制计数器：当连续渲染相同状态时跳过 GL 调用
 *   - 脏标记模式：仅在状态变化时执行 GL 调用
 */
public class NovaGLHelper {

    // GL 状态缓存 — 使用位掩码高效存储
    private static final int FLAG_BLEND     = 1 << 0;
    private static final int FLAG_DEPTH     = 1 << 1;
    private static final int FLAG_ALPHA     = 1 << 2;
    private static final int FLAG_CULL      = 1 << 3;
    private static final int FLAG_TEXTURE2D = 1 << 4;
    private static final int FLAG_LIGHTING  = 1 << 5;
    private static final int FLAG_FOG       = 1 << 6;

    private static int currentState = 0;
    private static int dirtyFlags = 0;
    private static int batchCounter = 0;
    private static boolean stateInitialized = false;

    // 帧统计
    private static int stateChangesSkipped = 0;
    private static int totalDrawCalls = 0;

    /**
     * 在 setupTerrain 开头注入 — 初始化 GL 状态缓存
     */
    public static void onSetupTerrain(RenderGlobal rg) {
        if (!stateInitialized) {
            stateInitialized = true;
        }
        currentState = 0;
        dirtyFlags = 0;
        batchCounter = 0;
        stateChangesSkipped = 0;
        totalDrawCalls = 0;
    }

    /**
     * 在 renderClouds 末尾注入 — 重置 GL 状态缓存
     */
    public static void resetGLCache() {
        stateInitialized = false;
        currentState = 0;
        dirtyFlags = 0;
    }

    /**
     * 记录批量绘制 — 在每次绘制调用前调用
     * 如果 GL 状态未变化，返回 true 表示可以跳过状态设置
     */
    public static boolean onBatchDraw(int requiredState) {
        totalDrawCalls++;
        batchCounter++;

        if (!stateInitialized) {
            // 首次调用，必须设置状态
            currentState = requiredState;
            dirtyFlags = 0;
            return false;
        }

        if (currentState == requiredState) {
            // 状态未变化，跳过 GL 调用
            stateChangesSkipped++;
            return true;
        }

        // 状态变化，标记脏位
        dirtyFlags = currentState ^ requiredState;
        currentState = requiredState;
        return false;
    }

    /**
     * 获取当前 GL 状态位掩码
     */
    public static int getCurrentState() {
        return currentState;
    }

    /**
     * 获取脏标记（哪些状态位发生了变化）
     */
    public static int getDirtyFlags() {
        return dirtyFlags;
    }

    /**
     * 获取帧统计信息（调试用）
     */
    public static int getStateChangesSkipped() {
        return stateChangesSkipped;
    }

    public static int getTotalDrawCalls() {
        return totalDrawCalls;
    }

    // 常用状态组合常量
    public static final int STATE_OPAQUE    = FLAG_DEPTH | FLAG_CULL | FLAG_TEXTURE2D | FLAG_FOG;
    public static final int STATE_CUTOUT    = FLAG_DEPTH | FLAG_CULL | FLAG_TEXTURE2D | FLAG_ALPHA | FLAG_FOG;
    public static final int STATE_TRANSLUCENT = FLAG_DEPTH | FLAG_BLEND | FLAG_TEXTURE2D | FLAG_ALPHA | FLAG_FOG;
}