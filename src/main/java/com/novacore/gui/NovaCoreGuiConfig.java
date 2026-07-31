package com.novacore.gui;

import com.novacore.NovaCoreConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * NovaCore 双预设配置 GUI
 *
 * 提供 STANDARD / EXTREME / CUSTOM 三档预设切换，
 * 以及各模块的独立开关。
 * 通过 Forge Mods 列表 → NovaCore → Config 进入。
 */
public class NovaCoreGuiConfig extends GuiScreen {

    private static final int GUI_WIDTH = 320;
    private static final int ROW_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 20;

    // 按钮 ID 范围
    private static final int ID_PRESET_STANDARD = 100;
    private static final int ID_PRESET_EXTREME = 101;
    private static final int ID_PRESET_CUSTOM = 102;
    private static final int ID_SAVE = 200;
    private static final int ID_DONE = 201;

    // 模块按钮 ID
    private static final int ID_MODULE_BASE = 300;
    private static final int ID_MODULE_ENTITY_CULL = 300;
    private static final int ID_MODULE_LIGHTING = 301;
    private static final int ID_MODULE_CHUNK_LOAD = 302;
    private static final int ID_MODULE_MEMORY = 303;
    private static final int ID_MODULE_MATH = 304;
    private static final int ID_MODULE_OPENGL = 305;
    private static final int ID_MODULE_TICK_RATE = 306;
    private static final int ID_MODULE_PARTICLE = 307;
    private static final int ID_MODULE_RENDER_AGG = 308;

    private NovaCoreConfig.Preset currentPreset;
    private GuiButton btnStandard, btnExtreme, btnCustom;
    private final List<ModuleToggle> moduleToggles = new ArrayList<>();

    private static class ModuleToggle {
        final int id;
        final String label;
        final String category;
        boolean enabled;

        ModuleToggle(int id, String label, String category, boolean enabled) {
            this.id = id;
            this.label = label;
            this.category = category;
            this.enabled = enabled;
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        currentPreset = NovaCoreConfig.preset;
        moduleToggles.clear();

        int centerX = this.width / 2;
        int topY = 40;

        // 预设按钮
        int presetBtnWidth = 90;
        int presetBtnSpacing = 8;
        int presetStartX = centerX - (presetBtnWidth * 3 + presetBtnSpacing * 2) / 2;

        this.buttonList.add(btnStandard = new GuiButton(ID_PRESET_STANDARD,
            presetStartX, topY, presetBtnWidth, BUTTON_HEIGHT,
            colorText("STANDARD", NovaCoreConfig.Preset.STANDARD)));
        this.buttonList.add(btnExtreme = new GuiButton(ID_PRESET_EXTREME,
            presetStartX + presetBtnWidth + presetBtnSpacing, topY,
            presetBtnWidth, BUTTON_HEIGHT,
            colorText("EXTREME", NovaCoreConfig.Preset.EXTREME)));
        this.buttonList.add(btnCustom = new GuiButton(ID_PRESET_CUSTOM,
            presetStartX + (presetBtnWidth + presetBtnSpacing) * 2, topY,
            presetBtnWidth, BUTTON_HEIGHT,
            colorText("CUSTOM", NovaCoreConfig.Preset.CUSTOM)));

        topY += 36;

        // 模块开关
        int moduleBtnWidth = 70;
        int labelWidth = 140;
        int moduleStartX = centerX - (labelWidth + moduleBtnWidth + 8) / 2;

        addModuleToggle(moduleStartX, topY, ID_MODULE_ENTITY_CULL,
            "实体剔除", "标准", NovaCoreConfig.entityCullingEnabled);
        topY += ROW_HEIGHT;
        addModuleToggle(moduleStartX, topY, ID_MODULE_LIGHTING,
            "BFS光照引擎", "标准", NovaCoreConfig.lightingEnabled);
        topY += ROW_HEIGHT;
        addModuleToggle(moduleStartX, topY, ID_MODULE_CHUNK_LOAD,
            "异步区块加载", "标准", NovaCoreConfig.chunkLoadingEnabled);
        topY += ROW_HEIGHT;
        addModuleToggle(moduleStartX, topY, ID_MODULE_MEMORY,
            "内存泄漏修复", "标准", NovaCoreConfig.memoryFixEnabled);
        topY += ROW_HEIGHT;
        addModuleToggle(moduleStartX, topY, ID_MODULE_MATH,
            "数学查表优化", "标准", NovaCoreConfig.mathLookupTable);
        topY += ROW_HEIGHT;
        addModuleToggle(moduleStartX, topY, ID_MODULE_OPENGL,
            "OpenGL状态缓存", "标准", NovaCoreConfig.openglEnabled);
        topY += ROW_HEIGHT;

        // 分隔线用空行
        topY += 8;

        addModuleToggle(moduleStartX, topY, ID_MODULE_TICK_RATE,
            "Tick降频引擎", "极端", NovaCoreConfig.tickRateEnabled);
        topY += ROW_HEIGHT;
        addModuleToggle(moduleStartX, topY, ID_MODULE_PARTICLE,
            "粒子限制器", "极端", NovaCoreConfig.particleLimiterEnabled);
        topY += ROW_HEIGHT;
        addModuleToggle(moduleStartX, topY, ID_MODULE_RENDER_AGG,
            "渲染激进优化", "极端", NovaCoreConfig.renderAggressionEnabled);

        topY += 40;

        // 保存/完成按钮
        int actionBtnWidth = 100;
        int actionStartX = centerX - (actionBtnWidth * 2 + 12) / 2;
        this.buttonList.add(new GuiButton(ID_SAVE,
            actionStartX, topY, actionBtnWidth, BUTTON_HEIGHT, "保存配置"));
        this.buttonList.add(new GuiButton(ID_DONE,
            actionStartX + actionBtnWidth + 12, topY, actionBtnWidth, BUTTON_HEIGHT, "完成"));
    }

    private void addModuleToggle(int startX, int y, int id, String label, String category, boolean enabled) {
        moduleToggles.add(new ModuleToggle(id, label, category, enabled));
        this.buttonList.add(new GuiButton(id,
            startX + 150, y, 70, BUTTON_HEIGHT,
            enabled ? "§aON" : "§cOFF"));
    }

    private String colorText(String text, NovaCoreConfig.Preset preset) {
        if (currentPreset == preset) {
            return "§a§l" + text;
        }
        return text;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        int id = button.id;

        // 预设切换
        if (id == ID_PRESET_STANDARD) {
            NovaCoreConfig.setPreset(NovaCoreConfig.Preset.STANDARD);
            refreshState();
            return;
        }
        if (id == ID_PRESET_EXTREME) {
            NovaCoreConfig.setPreset(NovaCoreConfig.Preset.EXTREME);
            refreshState();
            return;
        }
        if (id == ID_PRESET_CUSTOM) {
            NovaCoreConfig.setPreset(NovaCoreConfig.Preset.CUSTOM);
            refreshState();
            return;
        }

        // 模块切换
        if (id >= ID_MODULE_BASE && id < ID_MODULE_BASE + 20) {
            toggleModule(id);
            return;
        }

        // 保存
        if (id == ID_SAVE) {
            saveAllModules();
            NovaCoreConfig.saveConfig();
            return;
        }

        // 完成
        if (id == ID_DONE) {
            saveAllModules();
            NovaCoreConfig.saveConfig();
            this.mc.displayGuiScreen(null);
            return;
        }

        super.actionPerformed(button);
    }

    private void toggleModule(int id) {
        switch (id) {
            case ID_MODULE_ENTITY_CULL:
                NovaCoreConfig.entityCullingEnabled = !NovaCoreConfig.entityCullingEnabled;
                break;
            case ID_MODULE_LIGHTING:
                NovaCoreConfig.lightingEnabled = !NovaCoreConfig.lightingEnabled;
                break;
            case ID_MODULE_CHUNK_LOAD:
                NovaCoreConfig.chunkLoadingEnabled = !NovaCoreConfig.chunkLoadingEnabled;
                break;
            case ID_MODULE_MEMORY:
                NovaCoreConfig.memoryFixEnabled = !NovaCoreConfig.memoryFixEnabled;
                break;
            case ID_MODULE_MATH:
                NovaCoreConfig.mathLookupTable = !NovaCoreConfig.mathLookupTable;
                break;
            case ID_MODULE_OPENGL:
                NovaCoreConfig.openglEnabled = !NovaCoreConfig.openglEnabled;
                break;
            case ID_MODULE_TICK_RATE:
                NovaCoreConfig.tickRateEnabled = !NovaCoreConfig.tickRateEnabled;
                break;
            case ID_MODULE_PARTICLE:
                NovaCoreConfig.particleLimiterEnabled = !NovaCoreConfig.particleLimiterEnabled;
                break;
            case ID_MODULE_RENDER_AGG:
                NovaCoreConfig.renderAggressionEnabled = !NovaCoreConfig.renderAggressionEnabled;
                break;
        }
        // 任意修改后切换到 CUSTOM
        NovaCoreConfig.preset = NovaCoreConfig.Preset.CUSTOM;
        currentPreset = NovaCoreConfig.Preset.CUSTOM;
        refreshState();
    }

    private void saveAllModules() {
        // 各模块值已在 toggleModule 中实时更新到 NovaCoreConfig
        // 这里只需确保 preset 正确
        if (currentPreset != NovaCoreConfig.Preset.CUSTOM) {
            NovaCoreConfig.setPreset(currentPreset);
        }
    }

    private void refreshState() {
        currentPreset = NovaCoreConfig.preset;
        this.buttonList.clear();
        this.moduleToggles.clear();
        initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        int centerX = this.width / 2;

        // 标题
        this.drawCenteredString(this.fontRenderer,
            "§lNovaCore 极致性能引擎", centerX, 16, 0xFFFFFF);
        this.drawCenteredString(this.fontRenderer,
            "§7配置版本: " + NovaCoreConfig.preset.name(), centerX, 28, 0xAAAAAA);

        // 模块标签
        int moduleStartX = centerX - (140 + 70 + 8) / 2;
        int labelX = moduleStartX;
        int topY = 76;

        for (ModuleToggle mt : moduleToggles) {
            String catTag = mt.category.equals("极端") ? " §5[极端]" : " §7[标准]";
            this.drawString(this.fontRenderer, mt.label + catTag, labelX, topY + 6, 0xCCCCCC);
            topY += ROW_HEIGHT;
        }

        // 底部提示
        this.drawCenteredString(this.fontRenderer,
            "§7提示: 修改任何模块开关将自动切换到 CUSTOM 模式",
            centerX, this.height - 20, 0x888888);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}