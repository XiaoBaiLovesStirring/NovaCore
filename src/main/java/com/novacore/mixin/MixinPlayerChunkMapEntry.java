package com.novacore.mixin;

import com.novacore.asm.NovaChunkIO;
import net.minecraft.server.management.PlayerChunkMapEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinPlayerChunkMapEntry — 在区块更新时注入预加载调度
 * <p>
 * 目标类: net.minecraft.server.management.PlayerChunkMapEntry
 * 注入方法: update (func_187280_d) — 在方法开头调用 NovaChunkIO.schedulePreload(this)
 * </p>
 */
@Mixin(PlayerChunkMapEntry.class)
public class MixinPlayerChunkMapEntry {

    /**
     * 首次调用计数器，用于保证日志只打印一次
     */
    @Unique
    private static int loadCount = 0;

    /**
     * 注入到 PlayerChunkMapEntry.update (func_187280_d) 方法开头
     * 调用 NovaChunkIO.schedulePreload(this) 进行区块预加载调度
     */
    @Inject(method = "update", at = @At("HEAD"))
    private void onUpdate(CallbackInfo ci) {
        if (loadCount == 0) {
            loadCount++;
            System.out.println("[NovaCore] 区块预加载调度已注入");
        }
        NovaChunkIO.schedulePreload((PlayerChunkMapEntry) (Object) this);
    }
}