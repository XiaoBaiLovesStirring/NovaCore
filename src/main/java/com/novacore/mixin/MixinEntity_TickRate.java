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
 * 目标: Entity.func_70071_h_ (onUpdate)
 */
@Mixin(Entity.class)
public class MixinEntity_TickRate {

    @Unique
    private static boolean injected = false;

    /**
     * 注入到 Entity.func_70071_h_ (onUpdate) HEAD
     */
    @Inject(method = "func_70071_h_", at = @At("HEAD"), cancellable = true, remap = false)
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