package com.novacore;

import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.ModMetadata;

/**
 * NovaCore 模组容器 — 通过 Coremod getModContainerClass() 注册，
 * 不添加 @Mod 注解以避免与 coremod 重复注册。
 */
public class NovaCoreContainer extends DummyModContainer {

    private static ModMetadata buildMeta() {
        ModMetadata meta = new ModMetadata();
        meta.modId = "novacore";
        meta.name = "NovaCore";
        meta.version = "1.0.0-alpha";
        meta.description = "Minecraft 1.12.2 extreme performance overhaul — ASM-based BFS lighting, async chunk loading, memory leak fixes, entity culling, and OpenGL pipeline rewrite.";
        meta.authorList = java.util.Collections.singletonList("NovaCore Team");
        return meta;
    }

    public NovaCoreContainer() {
        super(buildMeta());
    }
}