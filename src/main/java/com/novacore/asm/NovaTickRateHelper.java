package com.novacore.asm;

import java.util.List;

import com.novacore.NovaCoreConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

/**
 * 极致模式：Tick降频引擎 — 使用 MCP 名编译，Forge 运行时自动重映射到 SRG 名
 */
public class NovaTickRateHelper {

    private static long globalTick = 0;
    private static final int PHASE_MASK = 0xFF;

    public static void onWorldTick(WorldServer world) {
        globalTick++;
    }

    /**
     * 在每个 Entity.onUpdate() 开头注入
     * @return true 表示应该跳过本次 tick
     */
    public static boolean shouldSkipEntityTick(Entity entity) {
        if (!NovaCoreConfig.tickRateEnabled) return false;
        if (entity instanceof EntityPlayer) return false;
        if (entity.isDead) return false;

        try {
            World w = entity.world;
            if (w == null) return false;

            double ex = entity.posX;
            double ez = entity.posZ;

            double minDistSq = Double.MAX_VALUE;
            List<EntityPlayer> players = w.playerEntities;

            if (players != null && !players.isEmpty()) {
                for (EntityPlayer player : players) {
                    double px = player.posX;
                    double pz = player.posZ;
                    double dx = ex - px;
                    double dz = ez - pz;
                    double distSq = dx * dx + dz * dz;
                    if (distSq < minDistSq) {
                        minDistSq = distSq;
                    }
                }
            }

            if (minDistSq == Double.MAX_VALUE) return false;

            int interval;
            if (minDistSq < 32.0 * 32.0) {
                interval = NovaCoreConfig.entityTickIntervalNear;
            } else if (minDistSq < 64.0 * 64.0) {
                interval = NovaCoreConfig.entityTickIntervalMid;
            } else {
                interval = NovaCoreConfig.entityTickIntervalFar;
            }

            if (interval <= 1) return false;

            int id = entity.getEntityId();
            int phase = id & PHASE_MASK;
            return (globalTick + phase) % interval != 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 在每个 TileEntity.update() 调用前注入
     * @return true 表示应该跳过本次 tick
     */
    public static boolean shouldSkipTileEntityTick(TileEntity te) {
        if (!NovaCoreConfig.tickRateEnabled) return false;
        if (te.isInvalid()) return false;

        int interval = NovaCoreConfig.tileEntityTickInterval;
        if (interval <= 1) return false;

        BlockPos pos = te.getPos();
        int phase = ((pos.getX() * 31 + pos.getY() * 37 + pos.getZ() * 41) & 0x7FFFFFFF) & PHASE_MASK;
        return (globalTick + phase) % interval != 0;
    }

    /**
     * 在每个 Chunk 随机刻处理前注入
     * @return true 表示跳过该区块的随机刻
     */
    public static boolean shouldSkipRandomTick(Chunk chunk, WorldServer world) {
        if (!NovaCoreConfig.tickRateEnabled) return false;
        if (!NovaCoreConfig.skipRemoteRandomTicks) return false;

        try {
            int cx = chunk.x << 4;
            int cz = chunk.z << 4;
            double minDistSq = Double.MAX_VALUE;

            List<EntityPlayer> players = world.playerEntities;

            if (players != null) {
                for (EntityPlayer player : players) {
                    double px = player.posX;
                    double pz = player.posZ;
                    double dx = (cx + 8) - px;
                    double dz = (cz + 8) - pz;
                    double distSq = dx * dx + dz * dz;
                    if (distSq < minDistSq) {
                        minDistSq = distSq;
                    }
                }
            }

            return minDistSq > 128.0 * 128.0;
        } catch (Exception e) {
            return false;
        }
    }

    public static long getGlobalTick() {
        return globalTick;
    }
}