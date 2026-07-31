package com.novacore.asm;

import com.novacore.NovaCoreConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;

import java.util.ArrayDeque;

/**
 * 极致模式：粒子限制器 — 使用 MCP 名编译，Forge 运行时自动重映射到 SRG 名
 */
public class NovaParticleLimiter {

    /**
     * 获取 ParticleManager 中所有粒子的总数
     */
    private static int getParticleCount(ParticleManager manager) {
        int count = 0;
        for (ArrayDeque<Particle>[] layer : manager.fxLayers) {
            if (layer != null) {
                for (ArrayDeque<Particle> deque : layer) {
                    if (deque != null) {
                        count += deque.size();
                    }
                }
            }
        }
        return count;
    }

    /**
     * 在 ParticleManager.spawnParticle 开头注入
     * @return true 表示应该丢弃该粒子
     */
    public static boolean shouldDiscardParticle(ParticleManager manager, Particle particle) {
        if (!NovaCoreConfig.particleLimiterEnabled) return false;
        if (particle == null) return true;

        // 雨滴粒子禁用
        if (NovaCoreConfig.disableRainParticles) {
            String className = particle.getClass().getName();
            if (className.contains("Rain") || className.contains("WaterDrop")
                || className.contains("Splash")) {
                return true;
            }
        }

        // 距离剔除
        try {
            Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
            if (viewer != null) {
                double px = particle.posX;
                double py = particle.posY;
                double pz = particle.posZ;
                double vx = viewer.posX;
                double vy = viewer.posY;
                double vz = viewer.posZ;
                double dx = px - vx;
                double dy = py - vy;
                double dz = pz - vz;
                double distSq = dx * dx + dy * dy + dz * dz;

                int cutoff = NovaCoreConfig.particleDistanceCutoff;
                if (distSq > cutoff * cutoff) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }

        // 全局粒子数量限制
        try {
            int cap = NovaCoreConfig.particleGlobalCap;
            if (getParticleCount(manager) >= cap) {
                return true;
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    /**
     * 在 ParticleManager.updateEffects 开头注入
     * 缩减已有粒子的寿命
     */
    public static void onUpdateEffects(ParticleManager manager) {
        if (!NovaCoreConfig.particleLimiterEnabled) return;
        if (!NovaCoreConfig.reduceParticleLifetime) return;

        try {
            for (ArrayDeque<Particle>[] layer : manager.fxLayers) {
                if (layer != null) {
                    for (ArrayDeque<Particle> deque : layer) {
                        if (deque != null) {
                            for (Particle p : deque) {
                                if (p == null) continue;
                                if (!p.isAlive()) continue;
                                // 加速粒子老化：每帧额外减少粒子年龄
                                p.particleAge += 2;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}