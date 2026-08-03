package com.novacore.mixin;

import com.novacore.asm.NovaChunkIO;
import net.minecraft.server.management.PlayerChunkMapEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinPlayerChunkMapEntry — 区块预加载调度
 * 目标: PlayerChunkMapEntry.func_187280_d (update)
 */
@Mixin(PlayerChunkMapEntry.class)
public class MixinPlayerChunkMapEntry {

    @Unique
    private static int loadCount = 0;

    /**
     * 注入到 PlayerChunkMapEntry.func_187280_d (update) HEAD
     */
    @Inject(method = "func_187280_d", at = @At("HEAD"), remap = false)
    private void onUpdate(CallbackInfo ci) {
        if (loadCount == 0) {
            loadCount++;
            System.out.println("[NovaCore] 区块预加载调度已注入");
        }
        NovaChunkIO.schedulePreload((PlayerChunkMapEntry) (Object) this);
    }
}