package com.novacore.mixin;

import com.novacore.asm.NovaChunkIO;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

/**
 * MixinAnvilChunkLoader — 异步区块加载
 * 替换: func_75815_a (loadChunk) / func_191063_a (isChunkGeneratedAt)
 */
@Mixin(AnvilChunkLoader.class)
public class MixinAnvilChunkLoader {

    @Unique
    private static int loadCount = 0;

    /**
     * 替换 AnvilChunkLoader.func_75815_a (loadChunk)
     */
    @Overwrite(remap = false)
    public Chunk func_75815_a(World world, int x, int z) {
        if (loadCount == 0) {
            loadCount++;
            System.out.println("[NovaCore] 异步区块加载已启用");
        }
        return NovaChunkIO.loadChunkAsync((AnvilChunkLoader) (Object) this, world, x, z);
    }

    /**
     * 替换 AnvilChunkLoader.func_191063_a (isChunkGeneratedAt)
     */
    @Overwrite(remap = false)
    public boolean func_191063_a(World world, int x, int z) {
        if (loadCount == 0) {
            loadCount++;
            System.out.println("[NovaCore] 异步区块加载已启用");
        }
        return NovaChunkIO.chunkExistsFast((AnvilChunkLoader) (Object) this, world, x, z);
    }
}