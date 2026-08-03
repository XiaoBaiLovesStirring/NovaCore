package com.novacore.mixin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * MixinFakePlayer — 内存泄漏修复：检测并清理过期的 FakePlayer 实例
 * <p>
 * 目标类: net.minecraftforge.common.util.FakePlayer
 * 注入方法: onUpdate (func_70071_h_) — 在 HEAD 注入过期检测
 * SRG 字段映射:
 *   field_70173_aa -> ticksExisted (存活 tick 数)
 *   field_70170_p  -> world (所在世界)
 *   field_73010_i  -> playerEntities (世界中的玩家列表)
 * </p>
 */
@Mixin(FakePlayer.class)
public class MixinFakePlayer {

    /**
     * 注入标志，确保日志只打印一次
     */
    @Unique
    private static boolean injected = false;

    /**
     * 存活 tick 数 (SRG: field_70173_aa)
     * 位于 Entity 超类中
     */
    @Shadow
    private int ticksExisted;

    /**
     * 所在世界 (SRG: field_70170_p)
     * 位于 Entity 超类中
     */
    @Shadow
    public World world;

    /**
     * 注入到 FakePlayer.onUpdate (func_70071_h_) 方法 HEAD
     * <p>
     * 过期检测逻辑:
     * 如果 field_70173_aa (ticksExisted) > 12000 (10 分钟)
     * 且 field_70170_p (world) 的 field_73010_i (playerEntities) 不包含 this
     * 则调用 func_70106_y (setDead) 标记为死亡
     * </p>
     */
    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void onUpdate(CallbackInfo ci) {
        if (!injected) {
            injected = true;
            System.out.println("[NovaCore] 内存泄漏修复: FakePlayer 过期检测已注入");
        }

        // 检测条件: ticksExisted > 12000 且不在 playerEntities 列表中
        if (ticksExisted > 12000) {
            List<EntityPlayer> playerEntities = world.playerEntities;
            if (playerEntities != null && !playerEntities.contains(this)) {
                // 调用 setDead (func_70106_y) 标记为死亡
                ((FakePlayer) (Object) this).setDead();
            }
        }
    }
}