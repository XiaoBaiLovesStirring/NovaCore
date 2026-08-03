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
 * 目标: WorldServer.func_72839_b (updateEntities)
 */
@Mixin(WorldServer.class)
public class MixinWorld_TickRate {

    @Unique
    private static boolean injected = false;

    /**
     * 注入到 WorldServer.func_72839_b (updateEntities) HEAD
     */
    @Inject(method = "func_72839_b", at = @At("HEAD"), remap = false)
    private void onUpdateEntities(CallbackInfo ci) {
        if (!injected) {
            injected = true;
            System.out.println("[NovaCore][EXTREME] Tick 降频引擎已注入");
        }

        NovaTickRateHelper.onWorldTick((WorldServer) (Object) this);
    }
}