package com.novacore.mixin;

import com.novacore.asm.NovaTickRateHelper;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinTileEntity_TickRate — EXTREME 模块：TileEntity Tick 降频
 * <p>
 * 目标类: net.minecraft.tileentity.TileEntity
 * 注入方法: func_73660_a (update) — HEAD 注入，按需跳过 Tick
 * 委托: NovaTickRateHelper.shouldSkipTileEntityTick(this)
 * </p>
 */
@Mixin(TileEntity.class)
public class MixinTileEntity_TickRate {

    /**
     * 注入标志，确保日志只打印一次
     */
    @Unique
    private static boolean injected = false;

    /**
     * 注入到 TileEntity.func_73660_a (update) 方法 HEAD
     * <p>
     * 调用 NovaTickRateHelper.shouldSkipTileEntityTick(this) 判断是否跳过该 TileEntity 更新
     * 如果返回 true，则通过 CallbackInfo.cancel() 取消本次调用
     * </p>
     */
    @Inject(method = "func_73660_a", at = @At("HEAD"), cancellable = true)
    private void onTileEntityUpdate(CallbackInfo ci) {
        if (!injected) {
            injected = true;
            System.out.println("[NovaCore][EXTREME] TileEntity Tick 降频已注入");
        }

        if (NovaTickRateHelper.shouldSkipTileEntityTick((TileEntity) (Object) this)) {
            ci.cancel();
        }
    }
}