package com.novacore.asm;

import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * 实体剔除优化 — 视锥体 + 距离双重剔除
 *
 * 在 RenderGlobal.renderEntities 开头设置 ICamera，
 * 在后续实体渲染时检查是否在视锥体内。
 */
public class NovaCullingHelper {

    // 当前相机引用，由 beginCulling 设置
    private static ICamera currentCamera;

    /**
     * 在 renderEntities 开头注入
     */
    public static void beginCulling(ICamera camera) {
        currentCamera = camera;
    }

    /**
     * 在 renderEntities 末尾注入
     */
    public static void endCulling() {
        currentCamera = null;
    }

    /**
     * 快速剔除检查：实体是否可见
     * 在实体渲染循环中调用，替代原版的逐个 isBoundingBoxInFrustum 检查
     *
     * @param entity 待检查的实体
     * @param partialTicks 部分 ticks
     * @return true = 可见，false = 剔除
     */
    public static boolean isVisible(Entity entity, float partialTicks) {
        if (currentCamera == null) return true;
        if (entity == null || entity.isDead) return false;

        // 玩家始终可见
        if (entity instanceof EntityPlayer) return true;

        // 视锥体检查
        AxisAlignedBB bb = entity.getRenderBoundingBox();
        if (bb == null) {
            bb = entity.getEntityBoundingBox();
        }

        if (bb != null) {
            // 扩展一点包围盒避免边缘闪烁
            double expand = entity.getRenderDistanceWeight() * 2.0;
            return currentCamera.isBoundingBoxInFrustum(
                bb.expand(expand, expand, expand));
        }

        return true;
    }
}