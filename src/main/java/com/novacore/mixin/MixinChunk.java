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
 * <p>
 * 目标类: net.minecraft.world.chunk.Chunk
 * 注入方法: onUnload (func_76589_b) — 在 HEAD 注入 TileEntity 失效逻辑
 * 调用: func_177434_y() -> getTileEntityMap() 获取 TileEntity 映射
 * 调用: func_145845_h  -> invalidate() 逐个使 TileEntity 失效
 * </p>
 */
@Mixin(Chunk.class)
public class MixinChunk {

    /**
     * 注入标志，确保日志只打印一次
     */
    @Unique
    private static boolean injected = false;

    /**
     * 注入到 Chunk.onUnload (func_76589_b) 方法 HEAD
     * <p>
     * 清理逻辑:
     * 遍历 func_177434_y() (getTileEntityMap) 的 values，
     * 逐个调用 func_145845_h (invalidate) 使 TileEntity 失效
     * </p>
     */
    @Inject(method = "onUnload", at = @At("HEAD"))
    private void onUnload(CallbackInfo ci) {
        if (!injected) {
            injected = true;
            System.out.println("[NovaCore] 内存泄漏修复: Chunk 卸载清理已注入");
        }

        // 获取 TileEntity 映射 (func_177434_y -> getTileEntityMap)
        Chunk chunk = (Chunk) (Object) this;
        Map<?, TileEntity> tileEntityMap = chunk.getTileEntityMap();

        // 遍历并逐个调用 invalidate (func_145845_h)
        if (tileEntityMap != null) {
            for (TileEntity te : tileEntityMap.values()) {
                if (te != null) {
                    te.invalidate();
                }
            }
        }
    }
}