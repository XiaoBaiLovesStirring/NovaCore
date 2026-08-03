package com.novacore.mixin;

import com.novacore.asm.NovaParticleLimiter;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinParticleManager — EXTREME 模块：粒子限制器
 * <p>
 * 目标类: net.minecraft.client.particle.ParticleManager
 * 注入方法:
 *   func_78873_a (addEffect/spawnParticle) — HEAD 注入，按需丢弃粒子
 *   func_78874_a (updateEffects) — HEAD 注入，更新限制器状态
 * 委托: NovaParticleLimiter
 * </p>
 */
@Mixin(ParticleManager.class)
public class MixinParticleManager {

    /**
     * 注入标志，确保日志只打印一次
     */
    @Unique
    private static boolean injected = false;

    /**
     * 注入到 ParticleManager.func_78873_a (addEffect/spawnParticle) 方法 HEAD
     * <p>
     * 调用 NovaParticleLimiter.shouldDiscardParticle(this, particle) 判断是否丢弃该粒子
     * 如果返回 true，则通过 CallbackInfo.cancel() 取消粒子的生成
     * </p>
     */
    @Inject(method = "func_78873_a", at = @At("HEAD"), cancellable = true)
    private void onAddEffect(Particle particle, CallbackInfo ci) {
        if (!injected) {
            injected = true;
            System.out.println("[NovaCore][EXTREME] 粒子限制器已注入");
        }

        if (NovaParticleLimiter.shouldDiscardParticle((ParticleManager) (Object) this, particle)) {
            ci.cancel();
        }
    }

    /**
     * 注入到 ParticleManager.func_78874_a (updateEffects) 方法 HEAD
     * <p>
     * 调用 NovaParticleLimiter.onUpdateEffects(this) 更新限制器内部状态
     * </p>
     */
    @Inject(method = "func_78874_a", at = @At("HEAD"))
    private void onUpdateEffects(CallbackInfo ci) {
        NovaParticleLimiter.onUpdateEffects((ParticleManager) (Object) this);
    }
}