package com.novacore.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

/**
 * Module 6: OpenGL管线优化 — MultiDraw批量渲染 + GL状态缓存
 *
 * 转换目标（仅客户端）:
 *   - RenderGlobal.func_174977_a(BlockRenderLayer, double, int, Entity)I → 委托
 *   - RenderGlobal.func_174970_a(Entity, double, ICamera, int, boolean)V → 注入GL状态缓存初始化
 *   - RenderGlobal.func_180447_b(float, int, double, double, double)V → 注入GL状态缓存重置
 */
public class OpenGLTransformer extends NovaTransformer {

    private static final String RENDER_GLOBAL = "net.minecraft.client.renderer.RenderGlobal";
    private static final String NOVA_GL = "com/novacore/asm/NovaGLHelper";

    private static final String BLOCK_RENDER_LAYER = "Lnet/minecraft/util/BlockRenderLayer;";
    private static final String ENTITY_TYPE = "Lnet/minecraft/entity/Entity;";
    private static final String ICAMERA_TYPE = "Lnet/minecraft/client/renderer/culling/ICamera;";

    private static final String RENDER_BLOCK_LAYER_DESC = "(" + BLOCK_RENDER_LAYER + "DI" + ENTITY_TYPE + ")I";
    private static final String SETUP_TERRAIN_DESC = "(" + ENTITY_TYPE + "D" + ICAMERA_TYPE + "IZ)V";
    private static final String RENDER_CLOUDS_DESC = "(FIDDD)V";

    @Override
    protected String[] getTargetClasses() {
        return new String[]{RENDER_GLOBAL};
    }

    @Override
    protected String getTransformerName() {
        return "OpenGL";
    }

    @Override
    protected byte[] doTransform(String className, String transformedName, byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = createClassWriter(cr);

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {

            @Override
            public void visitEnd() {
                FieldVisitor fvCache = visitField(
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                    "novaGLCache",
                    "Ljava/lang/Object;",
                    null, null);
                if (fvCache != null) {
                    fvCache.visitEnd();
                }

                FieldVisitor fvBatch = visitField(
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                    "novaBatchBuffer",
                    "Ljava/lang/Object;",
                    null, null);
                if (fvBatch != null) {
                    fvBatch.visitEnd();
                }

                super.visitEnd();
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // func_174977_a = renderBlockLayer(BlockRenderLayer, double, int, Entity) → int
                if ("func_174977_a".equals(name) && RENDER_BLOCK_LAYER_DESC.equals(desc)) {
                    return new MethodBodyReplacer(mv, access, name, desc) {
                        @Override
                        protected void emitBody(GeneratorAdapter ga) {
                            ga.loadThis();
                            ga.loadArg(0);
                            ga.loadArg(1);
                            ga.loadArg(2);
                            ga.loadArg(3);
                            ga.invokeStatic(Type.getType("L" + NOVA_GL + ";"),
                                new Method("renderBlockLayerMultiDraw",
                                    "(Lnet/minecraft/client/renderer/RenderGlobal;" +
                                    BLOCK_RENDER_LAYER + "DI" + ENTITY_TYPE + ")I"));
                            ga.returnValue();
                        }
                    };
                }

                // func_174970_a = setupTerrain(Entity, double, ICamera, int, boolean)V
                if ("func_174970_a".equals(name) && SETUP_TERRAIN_DESC.equals(desc)) {
                    return new HeadInjector(mv) {
                        @Override
                        protected void onMethodEntry() {
                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, NOVA_GL,
                                "onSetupTerrain",
                                "(Lnet/minecraft/client/renderer/RenderGlobal;)V", false);
                        }
                    };
                }

                // func_180447_b = renderClouds(float, int, double, double, double)V
                if ("func_180447_b".equals(name) && RENDER_CLOUDS_DESC.equals(desc)) {
                    return new ReturnInjector(mv) {
                        @Override
                        protected void onBeforeReturn() {
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, NOVA_GL,
                                "resetGLCache", "()V", false);
                        }
                    };
                }

                return mv;
            }
        };

        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    /* ==================== 内部工具类 ==================== */

    abstract static class HeadInjector extends MethodVisitor {
        private boolean injected = false;

        HeadInjector(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        protected abstract void onMethodEntry();

        @Override
        public void visitCode() {
            super.visitCode();
            if (!injected) {
                onMethodEntry();
                injected = true;
            }
        }
    }

    abstract static class ReturnInjector extends MethodVisitor {
        private boolean injected = false;

        ReturnInjector(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        protected abstract void onBeforeReturn();

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN && !injected) {
                onBeforeReturn();
                injected = true;
            }
            super.visitInsn(opcode);
        }
    }
}