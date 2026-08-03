package com.novacore.asm;

import com.novacore.NovaCoreConfig;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

/**
 * NovaTickRateHelper — EXTREME 模块：Tick 降频引擎
 * <p>
 * 控制实体/TileEntity/世界的 Tick 频率
 * 由 NovaCoreConfig.tickRateEnabled 控制开关
 * </p>
 */
public class NovaTickRateHelper {

    private static int worldTickCounter = 0;

    /**
     * 世界 Tick 降频：每 N tick 才执行一次实体更新
     */
    public static void onWorldTick(WorldServer world) {
        if (!NovaCoreConfig.tickRateEnabled) return;
        // 世界 Tick 降频逻辑由 Mixin 层处理
        worldTickCounter++;
    }

    /**
     * 判断是否跳过该实体 Tick
     */
    public static boolean shouldSkipEntityTick(Entity entity) {
        if (!NovaCoreConfig.tickRateEnabled) return false;
        if (entity == null) return false;

        // 根据距离玩家远近决定 Tick 间隔
        // 近处: 每 tick 更新，中距离: 每 2 tick，远处: 每 4 tick
        int interval;
        if (entity.world != null && entity.world.playerEntities != null && !entity.world.playerEntities.isEmpty()) {
            net.minecraft.entity.player.EntityPlayer player = entity.world.playerEntities.get(0);
            double dist = entity.getDistance(player);
            if (dist < 32) interval = NovaCoreConfig.entityTickIntervalNear;
            else if (dist < 64) interval = NovaCoreConfig.entityTickIntervalMid;
            else interval = NovaCoreConfig.entityTickIntervalFar;
        } else {
            interval = NovaCoreConfig.entityTickIntervalFar;
        }

        return (entity.ticksExisted % interval) != 0;
    }

    /**
     * 判断是否跳过该 TileEntity 更新
     */
    public static boolean shouldSkipTileEntityTick(TileEntity te) {
        if (!NovaCoreConfig.tickRateEnabled) return false;
        if (te == null) return false;

        return (te.getWorld().getTotalWorldTime() % NovaCoreConfig.tileEntityTickInterval) != 0;
    }
}