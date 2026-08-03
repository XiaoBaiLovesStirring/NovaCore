package com.novacore.mixin;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * NovaCore Mixin 插件 — 根据配置决定哪些 Mixin 生效
 * 提供 STANDARD / EXTREME 双预设运行时切换
 */
public class NovaMixinPlugin implements IMixinConfigPlugin {

    private static boolean initialized = false;

    @Override
    public void onLoad(String mixinPackage) {
        if (!initialized) {
            System.out.println("[NovaCore] Mixin 引擎初始化中...");
            // 加载配置
            com.novacore.NovaCoreConfig.loadConfig();
            printConfigSummary();
            initialized = true;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean enabled = com.novacore.NovaCoreConfig.enabled;

        if (!enabled) return false;

        // EXTREME 模块控制
        if (!com.novacore.NovaCoreConfig.tickRateEnabled) {
            if (mixinClassName.contains("MixinWorld_TickRate") ||
                mixinClassName.contains("MixinEntity_TickRate") ||
                mixinClassName.contains("MixinTileEntity_TickRate")) {
                System.out.println("[NovaCore] 跳过 EXTREME: " + mixinClassName);
                return false;
            }
        }

        if (!com.novacore.NovaCoreConfig.particleLimiterEnabled) {
            if (mixinClassName.contains("MixinParticleManager")) {
                System.out.println("[NovaCore] 跳过 EXTREME: " + mixinClassName);
                return false;
            }
        }

        if (!com.novacore.NovaCoreConfig.renderAggressionEnabled) {
            if (mixinClassName.contains("MixinRender_Shadows")) {
                System.out.println("[NovaCore] 跳过 EXTREME: " + mixinClassName);
                return false;
            }
        }

        // STANDARD 模块控制
        if (!com.novacore.NovaCoreConfig.lightingEnabled &&
            mixinClassName.contains("MixinWorld_Lighting")) return false;
        if (!com.novacore.NovaCoreConfig.chunkLoadingEnabled &&
            (mixinClassName.contains("MixinAnvilChunkLoader") ||
             mixinClassName.contains("MixinPlayerChunkMapEntry"))) return false;
        if (!com.novacore.NovaCoreConfig.memoryFixEnabled &&
            (mixinClassName.contains("MixinWorldServer") ||
             mixinClassName.contains("MixinFakePlayer") ||
             mixinClassName.contains("MixinChunk"))) return false;
        if (!com.novacore.NovaCoreConfig.entityCullingEnabled &&
            mixinClassName.contains("MixinRenderGlobal_EntityCulling")) return false;
        if (!com.novacore.NovaCoreConfig.mathLookupTable &&
            mixinClassName.contains("MixinMathHelper")) return false;
        if (!com.novacore.NovaCoreConfig.openglEnabled &&
            mixinClassName.contains("MixinRenderGlobal_OpenGL")) return false;

        System.out.println("[NovaCore] 加载 Mixin: " + mixinClassName);
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {
        System.out.println("[NovaCore] 注入: " + mixinClassName + " -> " + targetClassName);
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {
        System.out.println("[NovaCore] 完成: " + mixinClassName + " -> " + targetClassName);
    }

    private void printConfigSummary() {
        String preset = com.novacore.NovaCoreConfig.preset.name();
        System.out.println("[NovaCore] ╔══════════════════════════════════════╗");
        System.out.println("[NovaCore] ║     NovaCore 2.0 Mixin 引擎      ║");
        System.out.println("[NovaCore] ╠══════════════════════════════════════╣");
        System.out.println("[NovaCore] ║ 预设: " + padRight(preset, 23) + "      ║");
        System.out.println("[NovaCore] ║ 光照引擎: " + pad(com.novacore.NovaCoreConfig.lightingEnabled) + "    ║");
        System.out.println("[NovaCore] ║ 区块加载: " + pad(com.novacore.NovaCoreConfig.chunkLoadingEnabled) + "    ║");
        System.out.println("[NovaCore] ║ 内存修复: " + pad(com.novacore.NovaCoreConfig.memoryFixEnabled) + "    ║");
        System.out.println("[NovaCore] ║ 实体剔除: " + pad(com.novacore.NovaCoreConfig.entityCullingEnabled) + "    ║");
        System.out.println("[NovaCore] ║ 数学优化: " + pad(com.novacore.NovaCoreConfig.mathLookupTable) + "    ║");
        System.out.println("[NovaCore] ║ OpenGL优化: " + pad(com.novacore.NovaCoreConfig.openglEnabled) + "  ║");
        System.out.println("[NovaCore] ║ Tick降频: " + pad(com.novacore.NovaCoreConfig.tickRateEnabled) + "    ║");
        System.out.println("[NovaCore] ║ 粒子限制: " + pad(com.novacore.NovaCoreConfig.particleLimiterEnabled) + "    ║");
        System.out.println("[NovaCore] ║ 渲染激进: " + pad(com.novacore.NovaCoreConfig.renderAggressionEnabled) + "    ║");
        System.out.println("[NovaCore] ╚══════════════════════════════════════╝");
    }

    private static String pad(boolean b) { return b ? "  ✓  " : "  ✗  "; }
    private static String padRight(String s, int n) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }
}