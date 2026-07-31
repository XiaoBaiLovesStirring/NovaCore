package com.novacore.asm;

import com.novacore.NovaCoreConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;

import java.util.Queue;

/**
 * 极致模式：粒子限制器 — 零反射版，通过 Access Transformer 直接访问 SRG 字段
 */
public class NovaParticleLimiter {

    /**
     * 获取 ParticleManager 中的粒子队列。
     * AT 已打通 field_78878_a / field_78877_a / field_78876_b 为 public，
     * 直接尝试读取，哪个非 null 就是哪个。
     */
    @SuppressWarnings("unchecked")
    private static Queue<Particle> getParticleQueue(ParticleManager manager) {
        try {
            Object q = manager.field_78878_a;
            if (q instanceof Queue) return (Queue<Particle>) q;
        } catch (NoSuchFieldError ignored) {}
        try {
            Object q = manager.field_78877_a;
            if (q instanceof Queue) return (Queue<Particle>) q;
        } catch (NoSuchFieldError ignored) {}
        try {
            Object q = manager.field_78876_b;
            if (q instanceof Queue) return (Queue<Particle>) q;
        } catch (NoSuchFieldError ignored) {}
        return null;
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
            Entity viewer = Minecraft.func_71410_x().func_175606_aa();
            if (viewer != null) {
                double px = particle.field_187126_f;
                double py = particle.field_187127_g;
                double pz = particle.field_187128_h;
                double vx = viewer.field_70165_t;
                double vy = viewer.field_70163_u;
                double vz = viewer.field_70161_v;
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
            Queue<Particle> queue = getParticleQueue(manager);
            if (queue != null) {
                int cap = NovaCoreConfig.particleGlobalCap;
                if (queue.size() >= cap) {
                    return true;
                }
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
            Queue<Particle> queue = getParticleQueue(manager);
            if (queue != null) {
                for (Particle p : queue) {
                    if (p == null) continue;
                    if (!p.isAlive()) continue;
                    // 加速粒子老化：每帧额外减少粒子年龄
                    p.field_70546_d += 2;
                }
            }
        } catch (Exception ignored) {
        }
    }
}