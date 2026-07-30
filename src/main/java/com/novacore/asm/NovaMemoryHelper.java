package com.novacore.asm;

import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

/**
 * 内存泄漏修复桩 — 待实现
 * 注入到WorldServer.onWorldSave和FakePlayer.onUpdate
 */
public class NovaMemoryHelper {
    public static void onWorldSave(WorldServer world) {
        throw new UnsupportedOperationException("[NovaCore] NovaMemoryHelper not yet implemented");
    }

    public static void checkFakePlayerStale(FakePlayer player) {
        throw new UnsupportedOperationException("[NovaCore] NovaMemoryHelper not yet implemented");
    }
}