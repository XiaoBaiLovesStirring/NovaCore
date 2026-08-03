package com.novacore.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
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
 * 目标: WorldServer.func_73044_a (saveAllChunks)
 */
@Mixin(WorldServer.class)
public class MixinWorldServer {

    @Unique
    private static boolean injected = false;

    /**
     * 已加载实体列表 (SRG: field_72996_f) — 在 World 父类中定义
     */
    @Shadow(remap = false)
    private List<Entity> field_72996_f;

    /**
     * 待添加 TileEntity (SRG: field_147482_g) — 在 World 父类中定义
     */
    @Shadow(remap = false)
    private List<TileEntity> field_147482_g;

    /**
     * 待移除 TileEntity (SRG: field_175730_i) — 在 World 父类中定义
     */
    @Shadow(remap = false)
    private List<TileEntity> field_175730_i;

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

        // 清理死亡实体
        Iterator<Entity> it = field_72996_f.iterator();
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
        field_147482_g.clear();
        field_175730_i.clear();
    }
}