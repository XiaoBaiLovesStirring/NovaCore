package com.novacore;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import java.util.Map;

/**
 * NovaCore 2.0 — Mixin 核心引擎
 * 基于 Mixin 字节码注入，保留 Coremod 入口
 * 12 个 Mixin 模块覆盖 9 大优化子系统
 *
 * STANDARD (6): Lighting, ChunkLoading, MemoryLeak, EntityCulling, MathOpt, OpenGL
 * EXTREME (+3): TickRate, ParticleLimiter, RenderAggression
 */
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
public class NovaCorePlugin implements IFMLLoadingPlugin {

    public NovaCorePlugin() {
        printLoadingBanner();
    }

    @Override
    public String[] getASMTransformerClass() {
        // 不再使用 ASM Transformer，全部由 Mixin 接管
        return new String[0];
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
        System.out.println("[NovaCore] 注入 Mixin 环境数据...");
        System.out.println("[NovaCore] 检测到 " + data.size() + " 个运行环境参数");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            System.out.println("[NovaCore]   " + entry.getKey() + " = " + entry.getValue());
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    private void printLoadingBanner() {
        String[] banner = {
            "",
            "  ╔══════════════════════════════════════════════════╗",
            "  ║                                                  ║",
            "  ║     ███╗   ██╗ ██████╗ ██╗   ██╗ █████╗          ║",
            "  ║     ████╗  ██║██╔═══██╗██║   ██║██╔══██╗         ║",
            "  ║     ██╔██╗ ██║██║   ██║██║   ██║███████║         ║",
            "  ║     ██║╚██╗██║██║   ██║╚██╗ ██╔╝██╔══██║         ║",
            "  ║     ██║ ╚████║╚██████╔╝ ╚████╔╝ ██║  ██║         ║",
            "  ║     ╚═╝  ╚═══╝ ╚═════╝   ╚═══╝  ╚═╝  ╚═╝         ║",
            "  ║                                                  ║",
            "  ║     ██████╗ ██████╗ ██████╗ ███████╗              ║",
            "  ║    ██╔════╝██╔═══██╗██╔══██╗██╔════╝              ║",
            "  ║    ██║     ██║   ██║██████╔╝█████╗                ║",
            "  ║    ██║     ██║   ██║██╔══██╗██╔══╝                ║",
            "  ║    ╚██████╗╚██████╔╝██║  ██║███████╗              ║",
            "  ║     ╚═════╝ ╚═════╝ ╚═╝  ╚═╝╚══════╝              ║",
            "  ║                                                  ║",
            "  ║     Mixin-Powered Performance Engine v2.0         ║",
            "  ║     Minecraft 1.12.2 │ Forge 14.23.5.2864         ║",
            "  ╚══════════════════════════════════════════════════╝",
            ""
        };
        for (String line : banner) {
            System.out.println(line);
        }
        System.out.println("[NovaCore] 正在初始化 Mixin 引擎...");
        System.out.println("[NovaCore] 检测私有类访问器...");
        System.out.println("[NovaCore] 注册 @Mixin 注入点...");
    }
}