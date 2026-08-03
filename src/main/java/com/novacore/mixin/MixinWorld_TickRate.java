package com.novacore.mixin;

import com.novacore.asm.NovaTickRateHelper;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinWorld_TickRate — EXTREME 模块：Tick 降频引擎
 * <p>
 * 目标类: net.minecraft.world.WorldServer
 * 注入方法: func_72839_b (updateEntities) — HEAD 注入降频逻辑
 * 委托: NovaTickRateHelper.onWorldTick(this)
 * </p>
 */
@Mixin(WorldServer.class)
public class MixinWorld_TickRate {

    /**
     * 注入标志，确保日志只打印一次
     */
    @Unique
    private static boolean injected = false;

    /**
     * 注入到 WorldServer.func_72839_b (updateEntities) 方法 HEAD
     * <p>
     * 调用 NovaTickRateHelper.onWorldTick(this) 控制世界 Tick 频率
     * </p>
     */
    @Inject(method = "func_72839_b", at = @At("HEAD"))
    private void onUpdateEntities(CallbackInfo ci) {
        if (!injected) {
            injected = true;
            System.out.println("[NovaCore][EXTREME] Tick 降频引擎已注入");
        }

        NovaTickRateHelper.onWorldTick((WorldServer) (Object) this);
    }
}