package com.novacore.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Module 9: 渲染激进优化（EXTREME预设专属）
 *
 * 转换目标（仅客户端）:
 *   1. World.func_72896_J()Z                         → 禁用天气
 *   2. RenderGlobal.func_180447_b(FIDDD)V            → 禁用云层
 *   3. Render.func_76976_a(Entity,DDDFF)V             → 禁用实体阴影
 *   4. RenderGlobal.func_174970_a(Entity,D,ICamera,IZ)V → 缩减实体渲染距离
 */
public class RenderAggressionTransformer extends NovaTransformer {

    private static final String WORLD = "net.minecraft.world.World";
    private static final String RENDER_GLOBAL = "net.minecraft.client.renderer.RenderGlobal";
    private static final String RENDER = "net.minecraft.client.renderer.entity.Render";
    private static final String NOVA_RA = "com/novacore/asm/NovaRenderAggression";

    private static final String ENTITY_TYPE = "Lnet/minecraft/entity/Entity;";
    private static final String ICAMERA_TYPE = "Lnet/minecraft/client/renderer/culling/ICamera;";

    @Override
    protected String[] getTargetClasses() {
        return new String[]{WORLD, RENDER_GLOBAL, RENDER};
    }

    @Override
    protected String getTransformerName() {
        return "RenderAgg";
    }

    @Override
    protected byte[] doTransform(String className, String transformedName, byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = createClassWriter(cr);

        if (WORLD.equals(transformedName)) {
            return transformWorld(cr, cw);
        }
        if (RENDER_GLOBAL.equals(transformedName)) {
            return transformRenderGlobal(cr, cw);
        }
        if (RENDER.equals(transformedName)) {
            return transformRender(cr, cw);
        }
        return bytes;
    }

    /* ==================== World: 禁用天气 ==================== */

    private byte[] transformWorld(ClassReader cr, ClassWriter cw) {
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // func_72896_J = isRaining()Z
                if ("func_72896_J".equals(name) && "()Z".equals(desc)) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        private boolean injected = false;

                        @Override
                        public void visitCode() {
                            super.visitCode();
                            if (!injected) {
                                // if (NovaRenderAggression.shouldDisableWeather()) return false;
                                mv.visitMethodInsn(Opcodes.INVOKESTATIC, NOVA_RA,
                                    "shouldDisableWeather", "()Z", false);
                                Label continueLabel = new Label();
                                mv.visitJumpInsn(Opcodes.IFEQ, continueLabel);
                                // return false
                                mv.visitInsn(Opcodes.ICONST_0);
                                mv.visitInsn(Opcodes.IRETURN);
                                mv.visitLabel(continueLabel);
                                injected = true;
                            }
                        }
                    };
                }
                return mv;
            }
        };

        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    /* ==================== RenderGlobal: 禁用云层 + 缩减实体渲染距离 ==================== */

    private byte[] transformRenderGlobal(ClassReader cr, ClassWriter cw) {
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // func_180447_b = renderClouds(float, int, double, double, double)V
                if ("func_180447_b".equals(name) && "(FIDDD)V".equals(desc)) {
                    return new ConditionalReturnVoidInjector(mv,
                        NOVA_RA, "shouldDisableClouds", "()Z");
                }

                return mv;
            }
        };

        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    /* ==================== Render: 禁用实体阴影 ==================== */

    private byte[] transformRender(ClassReader cr, ClassWriter cw) {
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // func_76976_a = doRenderShadowAndFire(Entity, double, double, double, float, float)V
                if ("func_76976_a".equals(name) &&
                    ("(" + ENTITY_TYPE + "DDDFF)V").equals(desc)) {
                    return new ConditionalReturnVoidInjector(mv,
                        NOVA_RA, "shouldDisableShadows", "()Z");
                }
                return mv;
            }
        };

        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    /* ==================== 内部工具类 ==================== */

    /**
     * 条件返回注入器（void 方法）
     * 如果 check 方法返回 true，则直接 RETURN
     */
    static class ConditionalReturnVoidInjector extends MethodVisitor {
        private boolean injected = false;
        private final String owner;
        private final String methodName;
        private final String methodDesc;

        ConditionalReturnVoidInjector(MethodVisitor mv, String owner, String methodName, String methodDesc) {
            super(Opcodes.ASM9, mv);
            this.owner = owner;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            if (!injected) {
                // INVOKESTATIC check()Z
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, methodName, methodDesc, false);
                // IFEQ continue_label (if false, continue)
                Label continueLabel = new Label();
                mv.visitJumpInsn(Opcodes.IFEQ, continueLabel);
                // RETURN
                mv.visitInsn(Opcodes.RETURN);
                // continue_label:
                mv.visitLabel(continueLabel);
                injected = true;
            }
        }
    }
}