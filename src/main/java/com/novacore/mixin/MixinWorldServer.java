package com.novacore.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;

/**
 * MixinWorldServer — 内存泄漏修复：WorldServer 保存时清理死亡实体和过期 TileEntity
 * 目标: WorldServer.func_73044_a (saveAllChunks)
 */
@Mixin(WorldServer.class)
public class MixinWorldServer {

    @Unique
    private static boolean injected = false;

    /**
     * 注入到 WorldServer.func_73044_a (saveAllChunks) RETURN
     */
    @Inject(method = "func_73044_a", at = @At("RETURN"), remap = false)
    private void onSaveAllChunks(boolean save, CallbackInfo ci) {
        if (!injected) {
            injected = true;
            System.out.println("[NovaCore] 内存泄漏修复: WorldServer 实体清理已注入");
        }

        WorldServer world = (WorldServer) (Object) this;
        
        // 清理死亡实体（直接访问 loadedEntityList 字段——Mixin 可访问私有字段）
        Iterator<Entity> it = world.loadedEntityList.iterator();
        while (it.hasNext()) {
            Entity entity = it.next();
            if (entity.isDead) {
                int chunkX = entity.chunkCoordX;
                int chunkZ = entity.chunkCoordZ;
                if (!world.getChunkProvider().chunkExists(chunkX, chunkZ)) {
                    it.remove();
                }
            }
        }

        // 清理 TileEntity 列表
        world.addedTileEntityList.clear();
        world.tileEntitiesToBeRemoved.clear();
    }
}