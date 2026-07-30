package com.novacore.asm;

import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

/**
 * BFS光照引擎桩 — 待实现
 * 将替换World.checkLightFor和updateLightByType的默认实现
 */
public class NovaLightEngine {
    public static boolean checkLightFor(World world, EnumSkyBlock type, BlockPos pos) {
        throw new UnsupportedOperationException("[NovaCore] NovaLightEngine not yet implemented");
    }

    public static void updateLightBFS(World world, EnumSkyBlock type, BlockPos pos) {
        throw new UnsupportedOperationException("[NovaCore] NovaLightEngine not yet implemented");
    }
}