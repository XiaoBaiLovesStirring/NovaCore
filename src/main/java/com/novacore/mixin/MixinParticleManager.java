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
 * 目标: ParticleManager.func_78873_a (addEffect) / func_78874_a (updateEffects)
 */
@Mixin(ParticleManager.class)
public class MixinParticleManager {

    @Unique
    private static boolean injected = false;

    /**
     * 注入到 ParticleManager.func_78873_a (addEffect/spawnParticle) HEAD
     */
    @Inject(method = "func_78873_a", at = @At("HEAD"), cancellable = true, remap = false)
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
     * 注入到 ParticleManager.func_78874_a (updateEffects) HEAD
     */
    @Inject(method = "func_78874_a", at = @At("HEAD"), remap = false)
    private void onUpdateEffects(CallbackInfo ci) {
        NovaParticleLimiter.onUpdateEffects((ParticleManager) (Object) this);
    }
}