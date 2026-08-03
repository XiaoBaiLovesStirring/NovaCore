package com.novacore;

import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.ModMetadata;

/**
 * NovaCore 模组容器 — 2.0 Mixin 版
 * 注册 @Mod 以供 GUI Factory 和配置系统使用
 */
public class NovaCoreContainer extends DummyModContainer {

    private static ModMetadata buildMeta() {
        ModMetadata meta = new ModMetadata();
        meta.modId = "novacore";
        meta.name = "NovaCore";
        meta.version = "2.0.0-mixin";
        meta.description = "Minecraft 1.12.2 extreme performance overhaul — Mixin-powered BFS lighting, async chunk loading, memory leak fixes, triple-tier entity culling, GL state dedup, math lookup tables, and optional EXTREME tick/particle/render tuning.";
        meta.authorList = java.util.Collections.singletonList("NovaCore Team");
        meta.credits = "Inspired by Starlight, Pulsar, Sodium, EntityCulling, FoamFix, BetterFPS";
        meta.url = "https://github.com/XiaoBaiLovesStirring/NovaCore";
        meta.guiConfigFactoryClass = "com.novacore.gui.NovaCoreGuiFactory";
        return meta;
    }

    public NovaCoreContainer() {
        super(buildMeta());
    }
}