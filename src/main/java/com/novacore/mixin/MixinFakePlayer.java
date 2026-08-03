package com.novacore.mixin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * MixinFakePlayer — 内存泄漏修复：检测并清理过期的 FakePlayer 实例
 * 目标: FakePlayer.func_70071_h_ (onUpdate)
 */
@Mixin(FakePlayer.class)
public class MixinFakePlayer {

    @Unique
    private static boolean injected = false;

    /**
     * 注入到 FakePlayer.func_70071_h_ (onUpdate) HEAD
     */
    @Inject(method = "func_70071_h_", at = @At("HEAD"), remap = false)
    private void onUpdate(CallbackInfo ci) {
        if (!injected) {
            injected = true;
            System.out.println("[NovaCore] 内存泄漏修复: FakePlayer 过期检测已注入");
        }

        FakePlayer self = (FakePlayer) (Object) this;
        
        // ticksExisted > 12000 (10分钟) 且不在 playerEntities 列表中
        if (self.ticksExisted > 12000) {
            List<EntityPlayer> playerEntities = self.world.playerEntities;
            if (playerEntities != null && !playerEntities.contains(self)) {
                self.setDead();
            }
        }
    }
}