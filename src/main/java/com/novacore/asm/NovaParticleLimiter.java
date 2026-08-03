package com.novacore.asm;

import com.novacore.NovaCoreConfig;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;

/**
 * NovaParticleLimiter — EXTREME 模块：粒子限制器
 * <p>
 * 在粒子生成时按需丢弃，控制粒子总数
 * 由 NovaCoreConfig.particleLimiterEnabled 控制开关
 * </p>
 */
public class NovaParticleLimiter {

    private static int particleCount = 0;
    private static long lastResetTime = 0;

    /**
     * 判断是否应该丢弃该粒子
     */
    public static boolean shouldDiscardParticle(ParticleManager manager, Particle particle) {
        if (!NovaCoreConfig.particleLimiterEnabled) return false;

        long now = System.currentTimeMillis();
        if (now - lastResetTime > 1000) {
            particleCount = 0;
            lastResetTime = now;
        }

        particleCount++;

        // 全局粒子上限
        if (particleCount > NovaCoreConfig.particleGlobalCap) {
            return true;
        }

        // 减少粒子生命周期
        if (NovaCoreConfig.reduceParticleLifetime && particle != null) {
            particle.particleMaxAge = Math.max(1, particle.particleMaxAge / 2);
        }

        return false;
    }

    /**
     * 每帧更新粒子限制器状态
     */
    public static void onUpdateEffects(ParticleManager manager) {
        if (!NovaCoreConfig.particleLimiterEnabled) return;
    }
}