package com.novacore.mixin;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * MixinChunk — 内存泄漏修复：Chunk 卸载时清理所有 TileEntity
 * 目标: Chunk.func_76589_b (onUnload)
 */
@Mixin(Chunk.class)
public class MixinChunk {

    @Unique
    private static boolean injected = false;

    /**
     * 注入到 Chunk.func_76589_b (onUnload) HEAD
     */
    @Inject(method = "func_76589_b", at = @At("HEAD"), remap = false)
    private void onUnload(CallbackInfo ci) {
        if (!injected) {
            injected = true;
            System.out.println("[NovaCore] 内存泄漏修复: Chunk 卸载清理已注入");
        }

        Chunk chunk = (Chunk) (Object) this;
        Map<?, TileEntity> tileEntityMap = chunk.getTileEntityMap();

        if (tileEntityMap != null) {
            for (TileEntity te : tileEntityMap.values()) {
                if (te != null) {
                    te.invalidate();
                }
            }
        }
    }
}