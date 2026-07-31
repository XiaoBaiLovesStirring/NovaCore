package com.novacore.asm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityWeatherEffect;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;

/**
 * 实体剔除优化 — 视锥体 + 距离 + 实体类型三重剔除
 *
 * 增强：
 *   - 距离剔除：超过 renderDist 的实体直接跳过
 *   - 类型过滤：粒子、经验球等小实体使用更激进的剔除策略
 *   - 视锥体剔除：使用扩展包围盒避免边缘闪烁
 */
public class NovaCullingHelper {

    private static ICamera currentCamera;
    private static double cameraX, cameraY, cameraZ;
    private static int renderDistance = 64;
    private static int renderDistanceSq = 4096;

    /**
     * 在 renderEntities 开头注入
     */
    public static void beginCulling(ICamera camera, Entity renderViewEntity) {
        currentCamera = camera;
        if (renderViewEntity != null) {
            cameraX = renderViewEntity.posX;
            cameraY = renderViewEntity.posY;
            cameraZ = renderViewEntity.posZ;
        }
        try {
            Minecraft mc = Minecraft.getMinecraft();
            renderDistance = mc.gameSettings.renderDistanceChunks * 16;
        } catch (Exception e) {
            renderDistance = 64;
        }
        renderDistanceSq = renderDistance * renderDistance;
    }

    /**
     * 在 renderEntities 末尾注入
     */
    public static void endCulling() {
        currentCamera = null;
    }

    /**
     * 快速剔除检查：实体是否可见
     */
    public static boolean isVisible(Entity entity, float partialTicks) {
        if (currentCamera == null) return true;
        if (entity == null || entity.isDead) return false;

        // 玩家始终可见
        if (entity instanceof EntityPlayer) return true;
        // 天气效果始终可见
        if (entity instanceof EntityWeatherEffect) return true;

        // 阶段 1: 距离剔除（快速平方距离检查）
        double dx = entity.posX - cameraX;
        double dy = entity.posY - cameraY;
        double dz = entity.posZ - cameraZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        if (entity instanceof EntityItem || entity instanceof EntityXPOrb) {
            if (distSq > 256) return false; // 16^2
        } else if (entity instanceof EntityArrow) {
            if (distSq > 1024) return false; // 32^2
        } else {
            if (distSq > renderDistanceSq) return false;
        }

        // 阶段 2: 视锥体剔除
        AxisAlignedBB bb = entity.getRenderBoundingBox();
        if (bb == null) {
            bb = entity.getEntityBoundingBox();
        }

        if (bb != null) {
            double expand = MathHelper.clamp(Math.sqrt(distSq) * 0.02, 1.0, 4.0);
            return currentCamera.isBoundingBoxInFrustum(
                bb.expand(expand, expand, expand));
        }

        return true;
    }

    public static int getRenderDistance() {
        return renderDistance;
    }
}