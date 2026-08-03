package com.novacore.mixin;

import com.novacore.asm.NovaMathHelper;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

/**
 * MixinMathHelper — 数学查表优化
 * 替换: func_76126_a (sin) / func_76134_b (cos)
 */
@Mixin(MathHelper.class)
public class MixinMathHelper {

    @Unique
    private static int loadCount = 0;

    /**
     * 替换 MathHelper.func_76126_a (sin)
     */
    @Overwrite(remap = false)
    public static float func_76126_a(float value) {
        if (loadCount == 0) {
            loadCount++;
            System.out.println("[NovaCore] 数学查表优化已注入 sin/cos");
        }
        return NovaMathHelper.sin(value);
    }

    /**
     * 替换 MathHelper.func_76134_b (cos)
     */
    @Overwrite(remap = false)
    public static float func_76134_b(float value) {
        if (loadCount == 0) {
            loadCount++;
            System.out.println("[NovaCore] 数学查表优化已注入 sin/cos");
        }
        return NovaMathHelper.cos(value);
    }
}