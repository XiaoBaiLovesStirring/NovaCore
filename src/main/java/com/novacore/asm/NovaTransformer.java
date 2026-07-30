package com.novacore.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * NovaTransformer 基类 — 所有ASM转换器的统一入口
 *
 * 精简版:
 *   - 移除了方法签名预验证（verifyMethodsExist）
 *   - 仅保留 try-catch 防止转换异常导致游戏崩溃
 *   - 每个子类在 doTransform 中自行处理 ClassReader/ClassWriter
 */
public abstract class NovaTransformer implements IClassTransformer {

    /** 子类声明需要转换的目标类（MCP全限定名） */
    protected abstract String[] getTargetClasses();

    /** 子类返回转换器名称，用于日志 */
    protected abstract String getTransformerName();

    /** 子类执行实际的字节码转换 */
    protected abstract byte[] doTransform(String className, String transformedName, byte[] bytes);

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return bytes;

        for (String target : getTargetClasses()) {
            if (transformedName.equals(target) || name.equals(target)) {
                String display = transformedName != null ? transformedName : name;

                try {
                    byte[] result = doTransform(name, transformedName, bytes);
                    return result;
                } catch (Throwable e) {
                    System.err.println("[NovaCore][" + getTransformerName() + "] Failed to transform " + display + ": " + e);
                    e.printStackTrace();
                    return bytes;
                }
            }
        }
        return bytes;
    }

    // ==================== ClassWriter 工具 ====================

    /**
     * 创建安全的ClassWriter，处理getCommonSuperClass的类加载异常
     */
    protected ClassWriter createClassWriter(ClassReader cr) {
        return new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                try {
                    return super.getCommonSuperClass(type1, type2);
                } catch (Exception e) {
                    return "java/lang/Object";
                }
            }
        };
    }
}