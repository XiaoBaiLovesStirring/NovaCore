package com.novacore.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import java.util.Set;

/**
 * NovaCore GUI 配置工厂
 * 提供游戏内可视化配置界面，可切换 STANDARD/EXTREME/CUSTOM 预设
 */
public class NovaCoreGuiFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft minecraftInstance) {}

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new GuiConfig(parentScreen,
            ConfigElement.from(NovaCoreGuiConfig.class).getChildElements(),
            "novacore",
            false, false,
            "NovaCore - 性能优化引擎配置");
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return java.util.Collections.emptySet();
    }
}