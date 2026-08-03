package com.novacore.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;

/**
 * MixinWorldServer — 内存泄漏修复：WorldServer 保存时清理死亡实体和过期 TileEntity
 * <p>
 * 目标类: net.minecraft.world.WorldServer
 * 注入方法: saveAllChunks (func_73044_a) — 在 RETURN 前注入清理逻辑
 * SRG 字段映射:
 *   field_72996_f  -> loadedEntityList (实体列表)
 *   field_70128_L  -> isDead (实体死亡标志)
 *   field_147482_g -> addedTileEntityList (待添加 TileEntity)
 *   field_175730_i -> tileEntitiesToBeRemoved (待移除 TileEntity)
 * </p>
 */
@Mixin(WorldServer.class)
public class MixinWorldServer {

    /**
     * 注入标志，确保日志只打印一次
     */
    @Unique
    private static boolean injected = false;

    /**
     * 已加载实体列表 (SRG: field_72996_f)
     * 位于 World 超类中，通过 @Shadow 以 MCP 名称访问，refmap 负责 SRG 映射
     */
    @Shadow
    private List<Entity> loadedEntityList;

    /**
     * 待添加的 TileEntity 列表 (SRG: field_147482_g)
     */
    @Shadow
    private List<TileEntity> addedTileEntityList;

    /**
     * 待移除的 TileEntity 列表 (SRG: field_175730_i)
     */
    @Shadow
    private List<TileEntity> tileEntitiesToBeRemoved;

    /**
     * 注入到 WorldServer.saveAllChunks (func_73044_a) 方法 RETURN 前
     * <p>
     * 清理逻辑:
     * 1. 遍历 loadedEntityList，移除 isDead 且在未加载区块中的实体
     * 2. 清空 addedTileEntityList 和 tileEntitiesToBeRemoved
     * </p>
     */
    @Inject(method = "saveAllChunks", at = @At("RETURN"))
    private void onSaveAllChunks(boolean save, CallbackInfo ci) {
        if (!injected) {
            injected = true;
            System.out.println("[NovaCore] 内存泄漏修复: WorldServer 实体清理已注入");
        }

        // 实体清理：遍历 loadedEntityList (field_72996_f)
        // 移除 field_70128_L (isDead) 且在未加载区块中的实体
        WorldServer world = (WorldServer) (Object) this;
        Iterator<Entity> it = loadedEntityList.iterator();
        while (it.hasNext()) {
            Entity entity = it.next();
            if (entity.isDead) {
                // 检查实体所在区块是否已卸载
                int chunkX = entity.chunkCoordX;
                int chunkZ = entity.chunkCoordZ;
                if (!world.getChunkProvider().chunkExists(chunkX, chunkZ)) {
                    it.remove();
                }
            }
        }

        // TileEntity 清理：field_147482_g.clear() 和 field_175730_i.clear()
        addedTileEntityList.clear();
        tileEntitiesToBeRemoved.clear();
    }
}