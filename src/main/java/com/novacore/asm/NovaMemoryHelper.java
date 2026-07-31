package com.novacore.asm;

import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

/**
 * 内存泄漏修复
 *
 * 1. WorldServer 存档时清理过期缓存
 * 2. FakePlayer 过期检测 — 超过 12000 ticks 且不在玩家列表中则移除
 */
public class NovaMemoryHelper {

    /**
     * 在 WorldServer.saveAllChunks 末尾注入
     * 清理世界存档后的过期缓存，释放内存
     */
    public static void onWorldSave(WorldServer world) {
        // 清理该世界的缓存实体引用
        // 原版在某些情况下不会清理已卸载区块的实体引用
        world.loadedEntityList.removeIf(entity -> {
            // 如果实体所在的区块已卸载，则移除引用
            if (entity != null && entity.addedToChunk) {
                return !entity.world.isBlockLoaded(entity.getPosition());
            }
            return false;
        });

        // 定期清理 TileEntity 缓存
        world.loadedTileEntityList.removeIf(te -> {
            if (te != null && !te.isInvalid()) {
                return !te.getWorld().isBlockLoaded(te.getPos());
            }
            return false;
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
}