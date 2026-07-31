package com.novacore.asm;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.chunk.storage.RegionFileCache;
import net.minecraft.world.gen.ChunkProviderServer;

import java.io.DataInputStream;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步区块IO — 零反射版，通过 Access Transformer 直接访问 SRG 字段/方法
 *
 * AT 已打通:
 *   - AnvilChunkLoader.field_75825_d (chunkSaveLocation)
 *   - AnvilChunkLoader.func_75823_a (readChunkFromNBT)
 *   - PlayerChunkMapEntry.field_187275_c (chunk)
 *   - ChunkProviderServer.field_73247_e (chunkLoader)
 */
public class NovaChunkIO {

    private static final int MAX_CACHE_SIZE = 256;
    private static final int PRELOAD_RADIUS = 3;
    private static final float LOAD_FACTOR = 0.75f;

    private static final Map<Long, Chunk> chunkCache = new LinkedHashMap<Long, Chunk>(
        MAX_CACHE_SIZE, LOAD_FACTOR, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Chunk> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private static final Map<Long, Boolean> preloadQueue = new LinkedHashMap<Long, Boolean>(
        64, LOAD_FACTOR, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
            return size() > 128;
        }
    };

    private static final AtomicInteger preloadCount = new AtomicInteger(0);
    private static final int MAX_PRELOAD_COUNT = PRELOAD_RADIUS * PRELOAD_RADIUS * 4;

    private static final ExecutorService preloadExecutor = Executors.newFixedThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
        new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "NovaCore-ChunkIO");
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            }
        }
    );

    /**
     * 替代 AnvilChunkLoader.loadChunk(World, int, int)
     */
    public static Chunk loadChunkAsync(AnvilChunkLoader loader, World world, int x, int z) {
        long key = chunkKey(x, z);

        Chunk cached;
        synchronized (chunkCache) {
            cached = chunkCache.get(key);
        }
        if (cached != null && !cached.field_189550_d) {
            return cached;
        }

        try {
            // AT 已打通: AnvilChunkLoader.field_75825_d (chunkSaveLocation)
            File saveDir = loader.field_75825_d;
            RegionFile region = RegionFileCache.createOrLoadRegionFile(saveDir, x, z);
            DataInputStream dataIn = region.getChunkDataInputStream(x & 31, z & 31);

            if (dataIn != null) {
                try {
                    NBTTagCompound nbt = CompressedStreamTools.read(dataIn);
                    if (nbt.hasKey("Level", 10)) {
                        NBTTagCompound level = nbt.getCompoundTag("Level");
                        // AT 已打通: AnvilChunkLoader.func_75823_a (readChunkFromNBT)
                        Chunk chunk = loader.func_75823_a(world, level);
                        if (chunk != null) {
                            synchronized (chunkCache) {
                                chunkCache.put(key, chunk);
                            }
                            return chunk;
                        }
                    }
                } finally {
                    dataIn.close();
                }
            }
        } catch (Exception e) {
            // 加载失败返回 null
        }

        return null;
    }

    /**
     * 替代 AnvilChunkLoader.isChunkGeneratedAt(int, int)
     */
    public static boolean chunkExistsFast(AnvilChunkLoader loader, int x, int z) {
        long key = chunkKey(x, z);
        synchronized (chunkCache) {
            if (chunkCache.containsKey(key)) return true;
        }

        try {
            // AT 已打通: AnvilChunkLoader.field_75825_d (chunkSaveLocation)
            File saveDir = loader.field_75825_d;
            return RegionFileCache.createOrLoadRegionFile(saveDir, x, z).isChunkSaved(x & 31, z & 31);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 玩家移动时调度周围区块预加载
     */
    public static void schedulePreload(final PlayerChunkMapEntry entry) {
        // AT 已打通: PlayerChunkMapEntry.field_187275_c (chunk)
        final Chunk center = entry.field_187275_c;
        if (center == null) return;

        final int cx = center.field_76635_g;
        final int cz = center.field_76647_h;

        preloadExecutor.submit(() -> {
            try {
                AnvilChunkLoader loader = getChunkLoader(center);
                if (loader == null) return;

                for (int dx = -PRELOAD_RADIUS; dx <= PRELOAD_RADIUS; dx++) {
                    for (int dz = -PRELOAD_RADIUS; dz <= PRELOAD_RADIUS; dz++) {
                        if (dx == 0 && dz == 0) continue;
                        int dist = Math.abs(dx) + Math.abs(dz);
                        if (dist > PRELOAD_RADIUS + 1) continue;

                        long key = chunkKey(cx + dx, cz + dz);

                        synchronized (chunkCache) {
                            if (chunkCache.containsKey(key)) continue;
                        }

                        synchronized (preloadQueue) {
                            if (preloadQueue.containsKey(key)) continue;
                            if (preloadCount.get() >= MAX_PRELOAD_COUNT) continue;
                            preloadQueue.put(key, Boolean.TRUE);
                            preloadCount.incrementAndGet();
                        }

                        final int fdx = dx;
                        final int fdz = dz;
                        CompletableFuture.runAsync(() -> {
                            try {
                                // AT 已打通: AnvilChunkLoader.field_75825_d (chunkSaveLocation)
                                File saveDir = loader.field_75825_d;
                                RegionFile region = RegionFileCache.createOrLoadRegionFile(saveDir, cx + fdx, cz + fdz);
                                DataInputStream dataIn = region.getChunkDataInputStream((cx + fdx) & 31, (cz + fdz) & 31);
                                if (dataIn != null) {
                                    try {
                                        NBTTagCompound nbt = CompressedStreamTools.read(dataIn);
                                        if (nbt.hasKey("Level", 10)) {
                                            NBTTagCompound level = nbt.getCompoundTag("Level");
                                            // AT 已打通: AnvilChunkLoader.func_75823_a (readChunkFromNBT)
                                            Chunk chunk = loader.func_75823_a(center.func_177412_p(), level);
                                            if (chunk != null) {
                                                synchronized (chunkCache) {
                                                    chunkCache.put(key, chunk);
                                                }
                                            }
                                        }
                                    } finally {
                                        dataIn.close();
                                    }
                                }
                            } catch (Exception ignored) {
                            } finally {
                                preloadCount.decrementAndGet();
                                synchronized (preloadQueue) {
                                    preloadQueue.remove(key);
                                }
                            }
                        }, preloadExecutor);
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    public static void invalidateCache(int x, int z) {
        synchronized (chunkCache) {
            chunkCache.remove(chunkKey(x, z));
        }
    }

    public static void clearCache() {
        synchronized (chunkCache) {
            chunkCache.clear();
        }
        synchronized (preloadQueue) {
            preloadQueue.clear();
        }
        preloadCount.set(0);
    }

    /**
     * 从 Chunk 获取 AnvilChunkLoader — 通过 AT 直接字段访问
     * AT 已打通: ChunkProviderServer.field_73247_e (chunkLoader)
     */
    private static AnvilChunkLoader getChunkLoader(Chunk chunk) {
        try {
            World world = chunk.func_177412_p();
            if (world != null) {
                // 通过 WorldServer.getChunkProvider() 获取 ChunkProviderServer
                Object provider = ((net.minecraft.world.WorldServer) world).getChunkProvider();
                if (provider instanceof ChunkProviderServer) {
                    // AT 已打通: ChunkProviderServer.field_73247_e (chunkLoader)
                    Object loader = ((ChunkProviderServer) provider).field_73247_e;
                    if (loader instanceof AnvilChunkLoader) {
                        return (AnvilChunkLoader) loader;
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}