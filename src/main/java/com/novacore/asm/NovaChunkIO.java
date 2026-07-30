package com.novacore.asm;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.server.management.PlayerChunkMapEntry;

/**
 * 异步区块IO桩 — 待实现
 * 将替换AnvilChunkLoader.loadChunk/chunkExists和注入PlayerChunkMapEntry.update
 */
public class NovaChunkIO {
    public static Chunk loadChunkAsync(AnvilChunkLoader loader, World world, int x, int z) {
        throw new UnsupportedOperationException("[NovaCore] NovaChunkIO not yet implemented");
    }

    public static boolean chunkExistsFast(AnvilChunkLoader loader, World world, int x, int z) {
        throw new UnsupportedOperationException("[NovaCore] NovaChunkIO not yet implemented");
    }

    public static void schedulePreload(PlayerChunkMapEntry entry) {
        throw new UnsupportedOperationException("[NovaCore] NovaChunkIO not yet implemented");
    }
}