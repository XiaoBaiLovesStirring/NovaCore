package com.novacore.gui;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.Name;
import net.minecraftforge.common.config.Config.RangeInt;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.novacore.NovaCoreConfig;
import com.novacore.NovaCoreConfig.Preset;

@Config(modid = "novacore", name = "novacore", category = "")
@Config.LangKey("novacore.config.title")
public class NovaCoreGuiConfig {

    @Name("preset")
    @Comment("预设模式: STANDARD(6模块稳定) / EXTREME(+3模块极限) / CUSTOM(手动逐项)")
    public static String preset = "STANDARD";

    @Name("enabled")
    @Comment("全局开关")
    public static boolean enabled = true;

    // ============ STANDARD 模块 ============

    @Name("lightingEnabled")
    @Comment("BFS 光照引擎 — 替换 DFS 递归为 BFS 迭代，消除 StackOverflow 风险")
    public static boolean lightingEnabled = true;

    @Name("chunkLoadingEnabled")
    @Comment("异步区块加载 — 异步 IO + LRU 缓存 + 并行区域解析")
    public static boolean chunkLoadingEnabled = true;

    @Name("memoryFixEnabled")
    @Comment("内存泄漏修复 — WorldServer 实体清理 + FakePlayer 过期检测 + Chunk 卸载清理")
    public static boolean memoryFixEnabled = true;

    @Name("entityCullingEnabled")
    @Comment("实体剔除 — 视锥体 + 距离 + 实体类型三重剔除")
    public static boolean entityCullingEnabled = true;

    @Name("mathLookupTable")
    @Comment("数学查表优化 — sin/cos 65536 精度查表，消除 JNI 调用开销")
    public static boolean mathLookupTable = true;

    @Name("openglEnabled")
    @Comment("OpenGL 管线优化 — GL 状态缓存去重，减少冗余 glEnable/glDisable")
    public static boolean openglEnabled = true;

    // ============ EXTREME 模块 ============

    @Name("tickRateEnabled")
    @Comment("Tick 降频引擎 — 按距离分层降低实体/TileEntity 更新频率【EXTREME】")
    public static boolean tickRateEnabled = false;

    @Name("particleLimiterEnabled")
    @Comment("粒子限制器 — 全局粒子上限 + 距离截断 + 寿命缩减【EXTREME】")
    public static boolean particleLimiterEnabled = false;

    @Name("renderAggressionEnabled")
    @Comment("渲染激进优化 — 禁用阴影/云层/天气 + 缩减渲染距离【EXTREME】")
    public static boolean renderAggressionEnabled = false;

    // ============ 参数调优 ============

    @Name("entityRenderDistDefault")
    @Comment("实体渲染距离（格）")
    @RangeInt(min = 16, max = 256)
    public static int entityRenderDistDefault = 64;

    @Name("chunkIOThreads")
    @Comment("区块 IO 线程数")
    @RangeInt(min = 1, max = 16)
    public static int chunkIOThreads = 4;

    @Name("chunkPreloadRadius")
    @Comment("区块预加载半径")
    @RangeInt(min = 1, max = 16)
    public static int chunkPreloadRadius = 5;

    @Name("glBatchSize")
    @Comment("GL 批处理大小")
    @RangeInt(min = 64, max = 2048)
    public static int glBatchSize = 256;

    @Name("particleGlobalCap")
    @Comment("粒子全局上限【EXTREME】")
    @RangeInt(min = 50, max = 2000)
    public static int particleGlobalCap = 200;

    @Name("particleDistanceCutoff")
    @Comment("粒子距离截断【EXTREME】")
    @RangeInt(min = 4, max = 64)
    public static int particleDistanceCutoff = 16;

    @Name("disableEntityShadows")
    @Comment("禁用实体阴影【EXTREME】")
    public static boolean disableEntityShadows = false;

    @Name("disableClouds")
    @Comment("禁用云层渲染【EXTREME】")
    public static boolean disableClouds = false;

    @Name("disableWeather")
    @Comment("禁用天气效果【EXTREME】")
    public static boolean disableWeather = false;

    @Name("disableRainParticles")
    @Comment("禁用雨粒子【EXTREME】")
    public static boolean disableRainParticles = false;

    /**
     * 当 GUI 配置变更时，同步到运行时 NovaCoreConfig
     */
    public static void syncToRuntime() {
        NovaCoreConfig.preset = Preset.valueOf(preset.toUpperCase());
        NovaCoreConfig.enabled = enabled;
        NovaCoreConfig.lightingEnabled = lightingEnabled;
        NovaCoreConfig.chunkLoadingEnabled = chunkLoadingEnabled;
        NovaCoreConfig.memoryFixEnabled = memoryFixEnabled;
        NovaCoreConfig.entityCullingEnabled = entityCullingEnabled;
        NovaCoreConfig.mathLookupTable = mathLookupTable;
        NovaCoreConfig.openglEnabled = openglEnabled;
        NovaCoreConfig.tickRateEnabled = tickRateEnabled;
        NovaCoreConfig.particleLimiterEnabled = particleLimiterEnabled;
        NovaCoreConfig.renderAggressionEnabled = renderAggressionEnabled;
        NovaCoreConfig.entityRenderDistDefault = entityRenderDistDefault;
        NovaCoreConfig.chunkIOThreads = chunkIOThreads;
        NovaCoreConfig.chunkPreloadRadius = chunkPreloadRadius;
        NovaCoreConfig.glBatchSize = glBatchSize;
        NovaCoreConfig.particleGlobalCap = particleGlobalCap;
        NovaCoreConfig.particleDistanceCutoff = particleDistanceCutoff;
        NovaCoreConfig.disableEntityShadows = disableEntityShadows;
        NovaCoreConfig.disableClouds = disableClouds;
        NovaCoreConfig.disableWeather = disableWeather;
        NovaCoreConfig.disableRainParticles = disableRainParticles;
        NovaCoreConfig.applyPreset();
    }

    @Mod.EventBusSubscriber(modid = "novacore")
    public static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if ("novacore".equals(event.getModID())) {
                ConfigManager.sync("novacore", Config.Type.INSTANCE);
                syncToRuntime();
            }
        }
    }
}