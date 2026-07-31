package com.novacore.asm;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.FakePlayer;

/**
 * 内存泄漏修复 — 增强版
 *
 * 1. WorldServer 存档时清理过期缓存（实体 + TileEntity + Chunk 引用）
 * 2. FakePlayer 过期检测 — 超过 12000 ticks 且不在玩家列表中则移除
 * 3. World 卸载时彻底清理各类引用
 * 4. TileEntity 移除时清理关联引用
 */
public class NovaMemoryHelper {

    /**
     * 在 WorldServer.saveAllChunks 末尾注入
     * 清理世界存档后的过期缓存，释放内存
     */
    public static void onWorldSave(WorldServer world) {
        if (world == null) return;

        // 清理已卸载区块的实体引用
        world.loadedEntityList.removeIf(entity -> {
            if (entity == null) return true;
            if (entity.isDead) return true;
            if (entity.addedToChunk) {
                // 检查实体所在区块是否仍然加载
                int cx = ((int) entity.posX) >> 4;
                int cz = ((int) entity.posZ) >> 4;
                return !world.isBlockLoaded(
                    new net.minecraft.util.math.BlockPos(cx << 4, (int) entity.posY, cz << 4));
            }
            return false;
        });

        // 清理已卸载区块的 TileEntity 引用
        world.loadedTileEntityList.removeIf(te -> {
            if (te == null) return true;
            if (te.isInvalid()) return true;
            return !te.getWorld().isBlockLoaded(te.getPos());
        });

        // 清理 tickingTileEntities 中的无效引用
        world.tickableTileEntities.removeIf(te -> {
            if (te == null) return true;
            if (te.isInvalid()) return true;
            return !te.getWorld().isBlockLoaded(te.getPos());
        });
    }

    /**
     * 在 FakePlayer.onUpdate 开头注入
     * 检测 FakePlayer 是否已过期，避免内存泄漏
     */
    public static void checkFakePlayerStale(FakePlayer player) {
        if (player == null || player.world == null) return;

        // 超过 10 分钟（12000 ticks）且不在玩家列表中
        if (player.ticksExisted > 12000) {
            // 检查是否还有人在追踪此 FakePlayer
            if (!player.world.playerEntities.contains(player)) {
                // FakePlayer 已无人使用，标记删除
                player.setDead();
            }
        }
    }

    /**
     * 在 World 卸载时调用（通过反射或 Mixin 注入）
     * 彻底清理各类引用，防止内存泄漏
     */
    public static void onWorldUnload(WorldServer world) {
        if (world == null) return;

        // 清理所有实体
        world.loadedEntityList.clear();
        world.loadedTileEntityList.clear();
        world.tickableTileEntities.clear();

        // 清理区块缓存
        NovaChunkIO.clearCache();
    }

    /**
     * TileEntity 移除时清理关联引用
     * 在 Chunk.removeTileEntity 注入
     */
    public static void onTileEntityRemoved(TileEntity te) {
        if (te == null) return;

        // 清理 TileEntity 的 world 引用（防止 GC 无法回收）
        // 注意：不能在运行时设置 world 为 null，因为其他代码可能依赖它
        // 标记为无效即可
        if (!te.isInvalid()) {
            te.invalidate();
        }
    }

    /**
     * Chunk 卸载时清理
     */
    public static void onChunkUnload(Chunk chunk) {
        if (chunk == null) return;

        // 清理区块中的 TileEntity 引用
        if (chunk.getTileEntityMap() != null) {
            for (TileEntity te : chunk.getTileEntityMap().values()) {
                if (te != null && !te.isInvalid()) {
                    te.invalidate();
                }
            }
        }

        // 失效区块缓存
        NovaChunkIO.invalidateCache(chunk.x, chunk.z);
    }
}