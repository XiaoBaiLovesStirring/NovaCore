package com.novacore.mixin;

import com.novacore.asm.NovaTickRateHelper;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinEntity_TickRate — EXTREME 模块：实体 Tick 降频
 * <p>
 * 目标类: net.minecraft.entity.Entity
 * 注入方法: func_70071_h_ (onUpdate) — HEAD 注入，按需跳过 Tick
 * 委托: NovaTickRateHelper.shouldSkipEntityTick(this)
 * </p>
 */
@Mixin(Entity.class)
public class MixinEntity_TickRate {

    /**
     * 注入标志，确保日志只打印一次
     */
    @Unique
    private static boolean injected = false;

    /**
     * 注入到 Entity.func_70071_h_ (onUpdate) 方法 HEAD
     * <p>
     * 调用 NovaTickRateHelper.shouldSkipEntityTick(this) 判断是否跳过该实体 Tick
     * 如果返回 true，则通过 CallbackInfo.cancel() 取消本次调用
     * </p>
     */
    @Inject(method = "func_70071_h_", at = @At("HEAD"), cancellable = true)
    private void onEntityUpdate(CallbackInfo ci) {
        if (!injected) {
            injected = true;
            System.out.println("[NovaCore][EXTREME] 实体 Tick 降频已注入");
        }

        if (NovaTickRateHelper.shouldSkipEntityTick((Entity) (Object) this)) {
            ci.cancel();
        }
    }
}