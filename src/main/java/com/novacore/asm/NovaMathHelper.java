package com.novacore.asm;

import com.novacore.NovaCoreConfig;

/**
 * sin/cos查表法实现，替代MathHelper中的Math.sin/cos
 * 精度: 65536级（2^16），误差 < 0.0001
 * 性能: 查表 + 位运算，比JNI Math.sin快约10倍
 */
public final class NovaMathHelper {

    private static final int TABLE_SIZE;
    private static final float[] SIN_TABLE;
    private static final float RAD_TO_INDEX;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final float HALF_PI = (float) (Math.PI / 2.0);

    static {
        TABLE_SIZE = NovaCoreConfig.sinTableSize;
        SIN_TABLE = new float[TABLE_SIZE];
        RAD_TO_INDEX = TABLE_SIZE / TWO_PI;

        for (int i = 0; i < TABLE_SIZE; i++) {
            SIN_TABLE[i] = (float) Math.sin((double) i * TWO_PI / TABLE_SIZE);
        }
    }

    public static float sin(float value) {
        int index = (int) (value * RAD_TO_INDEX) & (TABLE_SIZE - 1);
        return SIN_TABLE[index];
    }

    public static float cos(float value) {
        return sin(value + HALF_PI);
    }

    private NovaMathHelper() {}
}