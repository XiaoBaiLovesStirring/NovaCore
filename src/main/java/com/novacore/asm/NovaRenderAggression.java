package com.novacore.asm;

import com.novacore.NovaCoreConfig;

/**
 * NovaRenderAggression — EXTREME 模块：渲染激进优化
 * <p>
 * 控制阴影/云/天气/区块更新的渲染
 * 由 NovaCoreConfig.renderAggressionEnabled 控制开关
 * </p>
 */
public class NovaRenderAggression {

    /**
     * 判断是否禁用实体阴影
     */
    public static boolean shouldDisableShadows() {
        return NovaCoreConfig.renderAggressionEnabled && NovaCoreConfig.disableEntityShadows;
    }

    /**
     * 判断是否禁用云渲染
     */
    public static boolean shouldDisableClouds() {
        return NovaCoreConfig.renderAggressionEnabled && NovaCoreConfig.disableClouds;
    }

    /**
     * 判断是否禁用天气渲染
     */
    public static boolean shouldDisableWeather() {
        return NovaCoreConfig.renderAggressionEnabled && NovaCoreConfig.disableWeather;
    }

    /**
     * 获取实体渲染距离比例
     */
    public static double getRenderDistanceRatio() {
        if (!NovaCoreConfig.renderAggressionEnabled) return 1.0;
        return NovaCoreConfig.entityRenderDistanceRatio;
    }
}