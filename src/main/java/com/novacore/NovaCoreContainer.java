package com.novacore;

import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModMetadata;

/**
 * NovaCore 模组容器
 * 使用 DummyModContainer(ModMetadata) 构造器直接传入metadata，
 * 避免依赖不存在的 setMetadata() 方法。
 */
@Mod(modid = "novacore", name = "NovaCore", version = "1.0.0-alpha", acceptableRemoteVersions = "*")
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