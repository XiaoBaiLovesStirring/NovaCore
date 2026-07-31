package com.novacore.asm;

import com.novacore.NovaCoreConfig;
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
 * 实体剔除优化 — 使用 MCP 名编译，Forge 运行时自动重映射到 SRG 名
 */
public class NovaCullingHelper {

    private static ICamera currentCamera;
    private static double cameraX, cameraY, cameraZ;
    private static int renderDistance = 64;
    private static int renderDistanceSq = 4096;

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

    public static void endCulling() {
        currentCamera = null;
    }

    /**
     * 快速剔除检查：实体是否可见
     */
    public static boolean isVisible(Entity entity, float partialTicks) {
        if (currentCamera == null) return true;
        if (entity == null) return false;
        if (entity.isDead) return false;

        if (entity instanceof EntityPlayer) return true;
        if (entity instanceof EntityWeatherEffect) return true;

        // 阶段 1: 距离剔除
        double ex = entity.posX;
        double ey = entity.posY;
        double ez = entity.posZ;
        double dx = ex - cameraX;
        double dy = ey - cameraY;
        double dz = ez - cameraZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        if (entity instanceof EntityItem || entity instanceof EntityXPOrb) {
            if (distSq > 256) return false;
        } else if (entity instanceof EntityArrow) {
            if (distSq > 1024) return false;
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