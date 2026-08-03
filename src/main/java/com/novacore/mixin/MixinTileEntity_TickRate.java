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
 * 目标: TileEntity.func_73660_a (update)
 */
@Mixin(TileEntity.class)
public class MixinTileEntity_TickRate {

    @Unique
    private static boolean injected = false;

    /**
     * 注入到 TileEntity.func_73660_a (update) HEAD
     */
    @Inject(method = "func_73660_a", at = @At("HEAD"), cancellable = true, remap = false)
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