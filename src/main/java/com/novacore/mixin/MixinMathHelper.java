package com.novacore.mixin;

import com.novacore.asm.NovaMathHelper;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

/**
 * MixinMathHelper — 用 NovaMathHelper 的查表实现替换 MathHelper 的 sin/cos
 * <p>
 * 目标类: net.minecraft.util.math.MathHelper
 * 精度: 65536 级查表 (2^16)，误差 &lt; 0.0001
 * 性能: 查表 + 位运算，比 JNI Math.sin 快约 10 倍
 * </p>
 */
@Mixin(MathHelper.class)
public class MixinMathHelper {

    /**
     * 首次调用计数器，用于保证日志只打印一次
     */
    @Unique
    private static int loadCount = 0;

    /**
     * 替换 MathHelper.sin (func_76126_a)
     * 使用 NovaMathHelper 的 65536 精度查表实现
     */
    @Overwrite
    public static float sin(float value) {
        if (loadCount == 0) {
            loadCount++;
            System.out.println("[NovaCore] 数学查表优化已注入 sin/cos");
        }
        return NovaMathHelper.sin(value);
    }

    /**
     * 替换 MathHelper.cos (func_76134_b)
     * 使用 NovaMathHelper 的查表实现 (通过 sin(value + PI/2) 计算)
     */
    @Overwrite
    public static float cos(float value) {
        if (loadCount == 0) {
            loadCount++;
            System.out.println("[NovaCore] 数学查表优化已注入 sin/cos");
        }
        return NovaMathHelper.cos(value);
    }
}