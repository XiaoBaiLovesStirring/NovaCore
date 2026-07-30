package com.novacore.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

/**
 * Module 5: 数学优化 — sin/cos查表法
 * 替换MathHelper.func_76126_a和MathHelper.func_76134_b为65536精度查表实现。
 * 消除每帧数千次Math.sin/cos的JNI调用开销（约10x加速）。
 */
public class MathOptTransformer extends NovaTransformer {

    private static final String MATH_HELPER = "net.minecraft.util.math.MathHelper";
    private static final String NOVA_MATH = "com/novacore/asm/NovaMathHelper";

    @Override
    protected String[] getTargetClasses() {
        return new String[]{MATH_HELPER};
    }

    @Override
    protected String getTransformerName() {
        return "MathOpt";
    }

    @Override
    protected byte[] doTransform(String className, String transformedName, byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = createClassWriter(cr);

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // func_76126_a = sin
                if ("func_76126_a".equals(name) && "(F)F".equals(desc)) {
                    return new MethodBodyReplacer(mv, access, name, desc) {
                        @Override
                        protected void emitBody(GeneratorAdapter ga) {
                            ga.loadArg(0);
                            ga.invokeStatic(Type.getType("L" + NOVA_MATH + ";"),
                                new Method("sin", "(F)F"));
                            ga.returnValue();
                        }
                    };
                }

                // func_76134_b = cos
                if ("func_76134_b".equals(name) && "(F)F".equals(desc)) {
                    return new MethodBodyReplacer(mv, access, name, desc) {
                        @Override
                        protected void emitBody(GeneratorAdapter ga) {
                            ga.loadArg(0);
                            ga.invokeStatic(Type.getType("L" + NOVA_MATH + ";"),
                                new Method("cos", "(F)F"));
                            ga.returnValue();
                        }
                    };
                }

                return mv;
            }
        };

        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }
}