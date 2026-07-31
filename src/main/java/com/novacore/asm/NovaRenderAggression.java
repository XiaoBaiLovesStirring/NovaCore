package com.novacore.asm;

import com.novacore.NovaCoreConfig;

/**
 * 极致模式：渲染激进优化
 *
 * 核心原理：
 *   在 EXTREME 模式下，对渲染管线进行激进优化，牺牲视觉质量换取帧率：
 *     1. 禁用天气渲染（雨、雪、雷暴）
 *     2. 禁用云层渲染
 *     3. 禁用实体阴影
 *     4. 缩减实体渲染距离（按比例缩放）
 *     5. 减少区块渲染更新频率
 */
public class NovaRenderAggression {

    /**
     * 检查是否应该禁用天气
     * 注入到 World.isRaining() 中
     */
    public static boolean shouldDisableWeather() {
        return NovaCoreConfig.renderAggressionEnabled && NovaCoreConfig.disableWeather;
    }

    /**
     * 检查是否应该禁用云层
     * 注入到 RenderGlobal.renderClouds() 中
     */
    public static boolean shouldDisableClouds() {
        return NovaCoreConfig.renderAggressionEnabled && NovaCoreConfig.disableClouds;
    }

    /**
     * 检查是否应该禁用实体阴影
     * 注入到 Render.renderShadow() 中
     */
    public static boolean shouldDisableShadows() {
        return NovaCoreConfig.renderAggressionEnabled && NovaCoreConfig.disableEntityShadows;
    }

    /**
     * 获取缩放后的实体渲染距离
     * 注入到 RenderGlobal.setupTerrain() 中
     *
     * @param originalDistance 原始渲染距离（renderDistanceChunks 的值）
     * @return 缩放后的渲染距离
     */
    public static int getScaledRenderDistance(int originalDistance) {
        if (!NovaCoreConfig.renderAggressionEnabled) return originalDistance;
        double ratio = NovaCoreConfig.entityRenderDistanceRatio;
        int scaled = (int) (originalDistance * ratio);
        return Math.max(scaled, 2); // 最小 2 个区块
    }

    /**
     * 检查是否应该减少区块更新
     * 注入到 RenderGlobal 的区块更新调度中
     *
     * @param chunkUpdateCounter 当前区块更新计数器
     * @return 是否应该跳过本次更新
     */
    public static boolean shouldSkipChunkUpdate(int chunkUpdateCounter) {
        if (!NovaCoreConfig.renderAggressionEnabled) return false;
        if (!NovaCoreConfig.reduceChunkUpdates) return false;
        // 每 3 个区块更新只执行 1 个
        return (chunkUpdateCounter % 3) != 0;
    }
}