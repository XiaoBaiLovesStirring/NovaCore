package com.novacore.mixin;

import com.novacore.asm.NovaLightEngine;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

/**
 * MixinWorld_Lighting — BFS 光照引擎替换 DFS 递归
 * 替换: func_180500_c (checkLightFor) / func_185463_a (updateLightByType)
 */
@Mixin(World.class)
public class MixinWorld_Lighting {

    @Unique
    private static int loadCount = 0;

    /**
     * 替换 World.func_180500_c (checkLightFor)
     */
    @Overwrite(remap = false)
    public boolean func_180500_c(EnumSkyBlock lightType, BlockPos pos) {
        if (loadCount == 0) {
            loadCount++;
            System.out.println("[NovaCore] BFS 光照引擎正在替换 DFS 递归");
        }
        return NovaLightEngine.checkLightFor((World) (Object) this, lightType, pos);
    }

    /**
     * 替换 World.func_185463_a (updateLightByType)
     */
    @Overwrite(remap = false)
    public void func_185463_a(EnumSkyBlock lightType, BlockPos pos) {
        if (loadCount == 0) {
            loadCount++;
            System.out.println("[NovaCore] BFS 光照引擎正在替换 DFS 递归");
        }
        NovaLightEngine.updateLightBFS((World) (Object) this, lightType, pos);
    }
}