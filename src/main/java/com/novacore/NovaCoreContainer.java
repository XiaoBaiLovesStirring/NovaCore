package com.novacore;

import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.ModMetadata;

/**
 * NovaCore 模组容器 — 通过 Coremod getModContainerClass() 注册，
 * 不添加 @Mod 注解以避免与 coremod 重复注册。
 *
 * 提供 GUI 配置工厂入口，可通过 Forge Mods 列表 → Config 进入。
 */
public class NovaCoreContainer extends DummyModContainer {

    private static ModMetadata buildMeta() {
        ModMetadata meta = new ModMetadata();
        meta.modId = "novacore";
        meta.name = "NovaCore";
        meta.version = "1.2.1";
        meta.description = "Minecraft 1.12.2 extreme performance overhaul — 9 ASM transformers for BFS lighting, async chunk loading, memory leak fixes, entity culling, OpenGL pipeline rewrite, tick rate control, particle limiting, and render aggression.";
        meta.authorList = java.util.Collections.singletonList("NovaCore Team");
        meta.url = "https://github.com/novacore";
        meta.credits = "Powered by ASM bytecode engineering";
        return meta;
    }

    public NovaCoreContainer() {
        super(buildMeta());
    }

    /**
     * 返回 GUI 配置工厂类名，使 Forge Mods 列表中出现 Config 按钮
     */
    @Override
    public String getGuiClassName() {
        return "com.novacore.gui.NovaCoreGuiFactory";
    }
}