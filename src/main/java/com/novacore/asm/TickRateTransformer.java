package com.novacore.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Module 7: Tick降频引擎（EXTREME预设专属）
 *
 * 转换目标:
 *   1. WorldServer.func_72839_b()V → 注入 onWorldTick 全局计数器
 *   2. Entity.func_70071_h_()V    → 注入 shouldSkipEntityTick 检查
 *   3. TileEntity.func_73660_a()V → 注入 shouldSkipTileEntityTick 检查
 */
public class TickRateTransformer extends NovaTransformer {

    private static final String WORLD_SERVER = "net.minecraft.world.WorldServer";
    private static final String ENTITY = "net.minecraft.entity.Entity";
    private static final String TILE_ENTITY = "net.minecraft.tileentity.TileEntity";
    private static final String NOVA_TICK = "com/novacore/asm/NovaTickRateHelper";

    private static final String ENTITY_TYPE = "Lnet/minecraft/entity/Entity;";
    private static final String TILE_ENTITY_TYPE = "Lnet/minecraft/tileentity/TileEntity;";
    private static final String WORLD_SERVER_TYPE = "Lnet/minecraft/world/WorldServer;";

    @Override
    protected String[] getTargetClasses() {
        return new String[]{WORLD_SERVER, ENTITY, TILE_ENTITY};
    }

    @Override
    protected String getTransformerName() {
        return "TickRate";
    }

    @Override
    protected byte[] doTransform(String className, String transformedName, byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = createClassWriter(cr);

        if (WORLD_SERVER.equals(transformedName)) {
            return transformWorldServer(cr, cw);
        }
        if (ENTITY.equals(transformedName)) {
            return transformEntity(cr, cw);
        }
        if (TILE_ENTITY.equals(transformedName)) {
            return transformTileEntity(cr, cw);
        }
        return bytes;
    }

    /* ==================== WorldServer ==================== */

    private byte[] transformWorldServer(ClassReader cr, ClassWriter cw) {
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // func_72839_b = updateEntities()V
                if ("func_72839_b".equals(name) && "()V".equals(desc)) {
                    return new HeadInjector(mv) {
                        @Override
                        protected void onMethodEntry() {
                            // NovaTickRateHelper.onWorldTick(this)
                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, NOVA_TICK,
                                "onWorldTick", "(" + WORLD_SERVER_TYPE + ")V", false);
                        }
                    };
                }
                return mv;
            }
        };

        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    /* ==================== Entity ==================== */

    private byte[] transformEntity(ClassReader cr, ClassWriter cw) {
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // func_70071_h_ = onUpdate()V
                if ("func_70071_h_".equals(name) && "()V".equals(desc)) {
                    return new ConditionalReturnInjector(mv,
                        NOVA_TICK, "shouldSkipEntityTick",
                        "(" + ENTITY_TYPE + ")Z");
                }
                return mv;
            }
        };

        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    /* ==================== TileEntity ==================== */

    private byte[] transformTileEntity(ClassReader cr, ClassWriter cw) {
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // func_73660_a = update()V
                if ("func_73660_a".equals(name) && "()V".equals(desc)) {
                    return new ConditionalReturnInjector(mv,
                        NOVA_TICK, "shouldSkipTileEntityTick",
                        "(" + TILE_ENTITY_TYPE + ")Z");
                }
                return mv;
            }
        };

        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    /* ==================== 内部工具类 ==================== */

    /**
     * 方法开头注入，可选条件返回
     * 如果 shouldSkip 方法返回 true，则直接 RETURN
     */
    static class ConditionalReturnInjector extends MethodVisitor {
        private boolean injected = false;
        private final String owner;
        private final String methodName;
        private final String methodDesc;

        ConditionalReturnInjector(MethodVisitor mv, String owner, String methodName, String methodDesc) {
            super(Opcodes.ASM9, mv);
            this.owner = owner;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            if (!injected) {
                // ALOAD 0 (this)
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                // INVOKESTATIC shouldSkipXxx(this)Z
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, methodName, methodDesc, false);
                // IFEQ continue_label (if false, continue)
                Label continueLabel = new Label();
                mv.visitJumpInsn(Opcodes.IFEQ, continueLabel);
                // RETURN (skip)
                mv.visitInsn(Opcodes.RETURN);
                // continue_label:
                mv.visitLabel(continueLabel);
                injected = true;
            }
        }
    }

    /**
     * 方法开头注入（无条件，仅执行回调）
     */
    abstract static class HeadInjector extends MethodVisitor {
        private boolean injected = false;

        HeadInjector(MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
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
}