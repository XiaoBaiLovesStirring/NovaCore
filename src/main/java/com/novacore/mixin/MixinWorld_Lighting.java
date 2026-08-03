package com.novacore.mixin;

import com.novacore.asm.NovaLightEngine;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

/**
 * MixinWorld_Lighting — 将 World 的 DFS 递归光照更新替换为 BFS 光照引擎
 * <p>
 * 目标类: net.minecraft.world.World
 * 替换方法: checkLightFor (func_180500_c) / updateLightByType (func_185463_a)
 * 委托: NovaLightEngine.checkLightFor() / NovaLightEngine.updateLightBFS()
 * </p>
 */
@Mixin(World.class)
public class MixinWorld_Lighting {

    /**
     * 首次调用计数器，用于保证日志只打印一次
     */
    @Unique
    private static int loadCount = 0;

    /**
     * 替换 World.checkLightFor (func_180500_c)
     * 委托给 NovaLightEngine.checkLightFor() 进行 BFS 光照检查
     */
    @Overwrite
    public boolean checkLightFor(EnumSkyBlock lightType, BlockPos pos) {
        if (loadCount == 0) {
            loadCount++;
            System.out.println("[NovaCore] BFS 光照引擎正在替换 DFS 递归");
        }
        return NovaLightEngine.checkLightFor((World) (Object) this, lightType, pos);
    }

    /**
     * 替换 World.updateLightByType (func_185463_a)
     * 委托给 NovaLightEngine.updateLightBFS() 进行 BFS 光照更新
     */
    @Overwrite
    public void updateLightByType(EnumSkyBlock lightType, BlockPos pos) {
        if (loadCount == 0) {
            loadCount++;
            System.out.println("[NovaCore] BFS 光照引擎正在替换 DFS 递归");
        }
        NovaLightEngine.updateLightBFS((World) (Object) this, lightType, pos);
    }
}