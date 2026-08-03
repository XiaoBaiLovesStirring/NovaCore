package com.novacore.mixin;

import com.novacore.asm.NovaChunkIO;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

/**
 * MixinAnvilChunkLoader — 将 AnvilChunkLoader 的同步区块加载替换为异步 IO
 * <p>
 * 目标类: net.minecraft.world.chunk.storage.AnvilChunkLoader
 * 替换方法: loadChunk (func_75815_a) / isChunkGeneratedAt (func_191063_a)
 * 委托: NovaChunkIO.loadChunkAsync() / NovaChunkIO.chunkExistsFast()
 * </p>
 */
@Mixin(AnvilChunkLoader.class)
public class MixinAnvilChunkLoader {

    /**
     * 首次调用计数器，用于保证日志只打印一次
     */
    @Unique
    private static int loadCount = 0;

    /**
     * 替换 AnvilChunkLoader.loadChunk (func_75815_a)
     * 委托给 NovaChunkIO.loadChunkAsync() 进行异步区块加载
     */
    @Overwrite
    public Chunk loadChunk(World world, int x, int z) {
        if (loadCount == 0) {
            loadCount++;
            System.out.println("[NovaCore] 异步区块加载已启用");
        }
        return NovaChunkIO.loadChunkAsync((AnvilChunkLoader) (Object) this, world, x, z);
    }

    /**
     * 替换 AnvilChunkLoader.isChunkGeneratedAt (func_191063_a)
     * 委托给 NovaChunkIO.chunkExistsFast() 进行快速区块存在性检查
     */
    @Overwrite
    public boolean isChunkGeneratedAt(World world, int x, int z) {
        if (loadCount == 0) {
            loadCount++;
            System.out.println("[NovaCore] 异步区块加载已启用");
        }
        return NovaChunkIO.chunkExistsFast((AnvilChunkLoader) (Object) this, world, x, z);
    }
}