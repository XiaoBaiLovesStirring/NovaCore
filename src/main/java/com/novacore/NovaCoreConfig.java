package com.novacore;

public class NovaCoreConfig {
    // 全局开关
    public static boolean enabled = true;

    // 调试
    public static boolean dumpAsm = Boolean.getBoolean("novacore.dumpAsm");
    public static boolean verifyAsm = Boolean.getBoolean("novacore.verifyAsm");
    public static boolean logTransforms = Boolean.getBoolean("novacore.logTransforms");

    // Module 1: 光照引擎
    public static boolean lightingEnabled = true;
    public static int lightThreadCount = Integer.getInteger("novacore.lightThreads", 1);
    public static boolean sendChunksWithoutLight = false;

    // Module 2: 区块加载
    public static boolean chunkLoadingEnabled = true;
    public static int chunkIOThreads = Integer.getInteger("novacore.chunkIOThreads", 4);
    public static int chunkParseThreads = Integer.getInteger("novacore.chunkParseThreads", 2);
    public static int chunkPreloadRadius = Integer.getInteger("novacore.chunkPreloadRadius", 5);
    public static int chunkPreloadTicksAhead = Integer.getInteger("novacore.chunkPreloadTicksAhead", 5);

    // Module 3: 内存修复
    public static boolean memoryFixEnabled = true;
    public static boolean fixWorldUnloadLeak = true;
    public static boolean fixFakePlayerLeak = true;
    public static boolean zeroAllocHotPaths = true;
    public static int blockPosPoolSize = Integer.getInteger("novacore.blockPosPoolSize", 1024);

    // Module 4: 实体剔除
    public static boolean entityCullingEnabled = true;
    public static boolean useOcclusionQuery = true;
    public static int entityInstanceThreshold = Integer.getInteger("novacore.entityInstanceThreshold", 8);
    public static int entityRenderDistDefault = 64;

    // Module 5: 数学优化
    public static boolean mathLookupTable = true;
    public static int sinTableSize = Integer.getInteger("novacore.sinTableSize", 65536);

    // Module 6: OpenGL优化
    public static boolean openglEnabled = true;
    public static boolean useMultiDraw = true;
    public static boolean cacheGlState = true;
    public static int glBatchSize = Integer.getInteger("novacore.glBatchSize", 256);

    // 自适应调度
    public static double speedThresholdFast = 8.0;
    public static double speedThresholdExtreme = 20.0;
}