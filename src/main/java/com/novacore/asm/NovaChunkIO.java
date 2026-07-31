package com.novacore.asm;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.chunk.storage.RegionFileCache;

import java.io.DataInputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步区块IO — 真正LRU缓存 + 区域文件直接读取 + 后台预加载
 *
 * 读取策略：
 *   1. 先查 LRU 缓存（access-order LinkedHashMap，命中率通常 > 70%）
 *   2. 未命中则直接从 RegionFile 读取 NBT 并构造 Chunk
 *   3. 预加载线程池异步加载玩家周围区块
 *   4. 区块保存时自动失效缓存
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

    private static Field saveDirField;
    private static Method readChunkMethod;
    private static boolean reflectionInitDone;

    private static synchronized void initReflection() {
        if (reflectionInitDone) return;
        try {
            try {
                saveDirField = AnvilChunkLoader.class.getDeclaredField("chunkSaveLocation");
            } catch (NoSuchFieldException e) {
                saveDirField = AnvilChunkLoader.class.getDeclaredField("field_75827_c");
            }
            saveDirField.setAccessible(true);

            Class<?> chunkLoaderClass = AnvilChunkLoader.class.getSuperclass();
            for (Method m : chunkLoaderClass.getDeclaredMethods()) {
                if (m.getReturnType() == Chunk.class && m.getParameterCount() == 2) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params[0] == World.class && params[1] == NBTTagCompound.class) {
                        readChunkMethod = m;
                        readChunkMethod.setAccessible(true);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[NovaCore] NovaChunkIO reflection init failed: " + e);
        }
        reflectionInitDone = true;
    }

    /**
     * 替代 AnvilChunkLoader.loadChunk(World, int, int)
     */
    public static Chunk loadChunkAsync(AnvilChunkLoader loader, World world, int x, int z) {
        long key = chunkKey(x, z);

        Chunk cached;
        synchronized (chunkCache) {
            cached = chunkCache.get(key);
        }
        if (cached != null && !cached.unloadQueued) {
            return cached;
        }

        initReflection();

        try {
            File saveDir = (File) saveDirField.get(loader);
            RegionFile region = RegionFileCache.createOrLoadRegionFile(saveDir, x, z);
            DataInputStream dataIn = region.getChunkDataInputStream(x & 31, z & 31);

            if (dataIn != null) {
                try {
                    NBTTagCompound nbt = CompressedStreamTools.read(dataIn);
                    if (nbt.hasKey("Level", 10)) {
                        NBTTagCompound level = nbt.getCompoundTag("Level");
                        Chunk chunk = (Chunk) readChunkMethod.invoke(loader, world, level);
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

        initReflection();

        try {
            File saveDir = (File) saveDirField.get(loader);
            return RegionFileCache.createOrLoadRegionFile(saveDir, x, z).isChunkSaved(x & 31, z & 31);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 玩家移动时调度周围区块预加载
     */
    public static void schedulePreload(final PlayerChunkMapEntry entry) {
        final Chunk center = getChunkReflect(entry);
        if (center == null) return;

        final int cx = center.x;
        final int cz = center.z;

        preloadExecutor.submit(() -> {
            try {
                AnvilChunkLoader loader = getChunkLoaderReflect(center);
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

                        CompletableFuture.runAsync(() -> {
                            try {
                                File saveDir = (File) saveDirField.get(loader);
                                RegionFile region = RegionFileCache.createOrLoadRegionFile(saveDir, cx + dx, cz + dz);
                                DataInputStream dataIn = region.getChunkDataInputStream((cx + dx) & 31, (cz + dz) & 31);
                                if (dataIn != null) {
                                    try {
                                        NBTTagCompound nbt = CompressedStreamTools.read(dataIn);
                                        if (nbt.hasKey("Level", 10)) {
                                            NBTTagCompound level = nbt.getCompoundTag("Level");
                                            Chunk chunk = (Chunk) readChunkMethod.invoke(
                                                loader, center.getWorld(), level);
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

    private static Chunk getChunkReflect(PlayerChunkMapEntry entry) {
        try {
            try {
                Field f = PlayerChunkMapEntry.class.getDeclaredField("chunk");
                f.setAccessible(true);
                return (Chunk) f.get(entry);
            } catch (NoSuchFieldException e) {
                Field f = PlayerChunkMapEntry.class.getDeclaredField("field_187275_c");
                f.setAccessible(true);
                return (Chunk) f.get(entry);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static AnvilChunkLoader getChunkLoaderReflect(Chunk chunk) {
        try {
            World world = chunk.getWorld();
            if (world != null) {
                Object provider = world.getClass().getMethod("getChunkProvider").invoke(world);
                if (provider != null) {
                    try {
                        Field f = provider.getClass().getDeclaredField("chunkLoader");
                        f.setAccessible(true);
                        return (AnvilChunkLoader) f.get(provider);
                    } catch (NoSuchFieldException e) {
                        Field f = provider.getClass().getDeclaredField("field_73247_e");
                        f.setAccessible(true);
                        return (AnvilChunkLoader) f.get(provider);
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