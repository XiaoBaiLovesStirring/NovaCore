package com.novacore.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Module 4: 实体遮挡剔除 — 视锥体+距离+类型三重剔除
 *
 * 转换目标（仅客户端）:
 *   - RenderGlobal.func_180446_a(Entity, ICamera, float)V
 *     → 注入剔除设置与结束调用
 */
public class EntityCullingTransformer extends NovaTransformer {

    private static final String RENDER_GLOBAL = "net.minecraft.client.renderer.RenderGlobal";
    private static final String NOVA_CULLING = "com/novacore/asm/NovaCullingHelper";

    private static final String ENTITY_TYPE = "Lnet/minecraft/entity/Entity;";
    private static final String ICAMERA_TYPE = "Lnet/minecraft/client/renderer/culling/ICamera;";

    private static final String RENDER_ENTITIES_DESC = "(" + ENTITY_TYPE + ICAMERA_TYPE + "F)V";

    @Override
    protected String[] getTargetClasses() {
        return new String[]{RENDER_GLOBAL};
    }

    @Override
    protected String getTransformerName() {
        return "EntityCull";
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

                // func_180446_a = renderEntities(Entity, ICamera, float)V
                if ("func_180446_a".equals(name) && RENDER_ENTITIES_DESC.equals(desc)) {
                    return new CullingInjector(mv);
                }

                return mv;
            }
        };

        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    static class CullingInjector extends MethodVisitor {
        private boolean beginInjected = false;
        private boolean endInjected = false;

        CullingInjector(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            if (!beginInjected) {
                // beginCulling(ICamera, Entity)
                mv.visitVarInsn(Opcodes.ALOAD, 1); // ICamera (param 2)
                mv.visitVarInsn(Opcodes.ALOAD, 0); // Entity renderViewEntity (param 1)
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, NOVA_CULLING,
                    "beginCulling", "(" + ICAMERA_TYPE + ENTITY_TYPE + ")V", false);
                beginInjected = true;
            }
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN && !endInjected) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, NOVA_CULLING,
                    "endCulling", "()V", false);
                endInjected = true;
            }
            super.visitInsn(opcode);
        }
    }
}