package com.novacore;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import java.util.Map;

/**
 * NovaCore — Minecraft 1.12.2 极致性能引擎
 * Forge Coremod入口，注册6个ASM字节码转换器
 */
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
public class NovaCorePlugin implements IFMLLoadingPlugin {

    public NovaCorePlugin() {
        System.out.println("[NovaCore] 极致性能引擎启动，准备替换五大核心子系统...");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{
            "com.novacore.asm.MathOptTransformer",
            "com.novacore.asm.MemoryLeakTransformer",
            "com.novacore.asm.LightingTransformer",
            "com.novacore.asm.ChunkLoadingTransformer",
            "com.novacore.asm.EntityCullingTransformer",
            "com.novacore.asm.OpenGLTransformer",
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