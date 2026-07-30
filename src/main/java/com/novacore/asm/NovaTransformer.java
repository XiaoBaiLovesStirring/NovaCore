package com.novacore.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashSet;
import java.util.Set;

/**
 * NovaTransformer 基类 — 所有ASM转换器的统一入口
 *
 * 安全锁机制:
 *   - 每个子类声明目标方法签名列表{@link #getRequiredMethods()}
 *   - 在doTransform之前，基类预扫描class字节码验证所有目标方法是否存在
 *   - 任何方法缺失则跳过整个转换，返回原始字节码
 *   - 防止因MCP映射变化、Forge版本差异、类结构更新导致的字节码损坏
 */
public abstract class NovaTransformer implements IClassTransformer {

    /** 子类声明需要转换的目标类（MCP全限定名） */
    protected abstract String[] getTargetClasses();

    /** 子类返回转换器名称，用于日志 */
    protected abstract String getTransformerName();

    /**
     * 子类声明需要验证的方法签名列表。
     * 返回null表示不需要验证（不推荐，无法享受安全锁保护）。
     * 每个MethodSignature包含MCP方法名和描述符。
     * 如果该类的任何方法在目标class中不存在，转换将被跳过。
     */
    protected abstract MethodSignature[] getRequiredMethods(String targetClass);

    /** 子类执行实际的字节码转换（此时所有方法已验证存在） */
    protected abstract byte[] doTransform(String className, String transformedName, byte[] bytes);

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return bytes;

        for (String target : getTargetClasses()) {
            if (transformedName.equals(target) || name.equals(target)) {
                String display = transformedName != null ? transformedName : name;

                // === 安全锁: 预扫描验证目标方法是否存在 ===
                MethodSignature[] required = getRequiredMethods(target);
                if (required != null && required.length > 0) {
                    if (!verifyMethodsExist(bytes, target, required)) {
                        // 任意方法缺失，跳过转换，返回原始字节码
                        return bytes;
                    }
                }

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

    // ==================== 安全锁实现 ====================

    /**
     * 方法签名 — 描述一个目标方法的名字和描述符
     */
    public static class MethodSignature {
        public final String name;
        public final String desc;

        public MethodSignature(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }

        /** 便捷构造: 无参无返回值方法 */
        public static MethodSignature of(String name) {
            return new MethodSignature(name, "()V");
        }

        /** 便捷构造: 指定名字和描述符 */
        public static MethodSignature of(String name, String desc) {
            return new MethodSignature(name, desc);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodSignature)) return false;
            MethodSignature that = (MethodSignature) o;
            return name.equals(that.name) && desc.equals(that.desc);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + desc.hashCode();
        }

        @Override
        public String toString() {
            return name + desc;
        }
    }

    /**
     * 预扫描class字节码，验证所有目标方法签名是否存在于类中。
     * 使用轻量级单次ClassReader扫描，收集所有方法签名后一次性比对。
     *
     * @param classBytes 原始class字节码
     * @param targetClass 目标类名（仅用于日志）
     * @param required 需要验证的方法签名列表
     * @return true 如果所有方法都存在，false 如果有任何方法缺失
     */
    private boolean verifyMethodsExist(byte[] classBytes, String targetClass, MethodSignature[] required) {
        try {
            ClassReader cr = new ClassReader(classBytes);
            final Set<MethodSignature> existing = new HashSet<>();

            cr.accept(new ClassVisitor(Opcodes.ASM5) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc,
                        String signature, String[] exceptions) {
                    existing.add(new MethodSignature(name, desc));
                    return null; // 不需要进入方法体
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            // 检查所有required方法是否存在
            boolean allFound = true;
            for (MethodSignature ms : required) {
                if (!existing.contains(ms)) {
                    System.err.println("[NovaCore][" + getTransformerName() + "] SAFETY LOCK: method '" +
                            ms.name + ms.desc + "' NOT FOUND in " + targetClass +
                            " — skipping transform (possible MCP mapping change or version mismatch)");
                    allFound = false;
                }
            }

            return allFound;
        } catch (Throwable e) {
            System.err.println("[NovaCore][" + getTransformerName() + "] SAFETY LOCK: failed to scan " + targetClass + ": " + e);
            return false; // 扫描失败也跳过转换
        }
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