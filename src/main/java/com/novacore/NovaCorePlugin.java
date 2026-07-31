package com.novacore;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import java.util.Map;

/**
 * NovaCore — Minecraft 1.12.2 极致性能引擎
 * Forge Coremod入口，注册 9 个 ASM 字节码转换器
 *
 * STANDARD 预设（6 个）: Lighting, ChunkLoading, MemoryLeak, EntityCulling, MathOpt, OpenGL
 * EXTREME 预设（+3 个）: TickRate, ParticleLimiter, RenderAggression
 */
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
public class NovaCorePlugin implements IFMLLoadingPlugin {

    public NovaCorePlugin() {
        System.out.println("[NovaCore] 极致性能引擎启动，九大核心子系统就绪...");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{
            // === STANDARD: 六大稳定器官 ===
            "com.novacore.asm.MathOptTransformer",
            "com.novacore.asm.MemoryLeakTransformer",
            "com.novacore.asm.LightingTransformer",
            "com.novacore.asm.ChunkLoadingTransformer",
            "com.novacore.asm.EntityCullingTransformer",
            "com.novacore.asm.OpenGLTransformer",
            // === EXTREME: 三大激进器官 ===
            "com.novacore.asm.TickRateTransformer",
            "com.novacore.asm.ParticleLimiterTransformer",
            "com.novacore.asm.RenderAggressionTransformer",
        };
    }

    @Override
    public String getModContainerClass() {
        return "com.novacore.NovaCoreContainer";
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}