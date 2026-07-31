package com.novacore.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Module 3: 内存泄漏修复 — 增强版
 *
 * 注入点:
 *   1. WorldServer.func_73044_a → 末尾注入 onWorldSave
 *   2. FakePlayer.func_70071_h_ → 开头注入 checkFakePlayerStale
 *   3. WorldServer.func_73041_k → 末尾注入 onWorldUnload（世界卸载清理）
 *   4. Chunk.func_76589_b → 开头注入 onChunkUnload（区块卸载清理）
 */
public class MemoryLeakTransformer extends NovaTransformer {

    private static final String WORLD_SERVER = "net.minecraft.world.WorldServer";
    private static final String FAKE_PLAYER = "net.minecraftforge.common.util.FakePlayer";
    private static final String CHUNK = "net.minecraft.world.chunk.Chunk";
    private static final String NOVA_MEM = "com/novacore/asm/NovaMemoryHelper";

    @Override
    protected String[] getTargetClasses() {
        return new String[]{WORLD_SERVER, FAKE_PLAYER, CHUNK};
    }

    @Override
    protected String getTransformerName() {
        return "MemLeak";
    }

    @Override
    protected byte[] doTransform(String className, String transformedName, byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = createClassWriter(cr);

        if (WORLD_SERVER.equals(transformedName)) {
            return transformWorldServer(cr, cw);
        }
        if (FAKE_PLAYER.equals(transformedName)) {
            return transformFakePlayer(cr, cw);
        }
        if (CHUNK.equals(transformedName)) {
            return transformChunk(cr, cw);
        }
        return bytes;
    }

    private byte[] transformWorldServer(ClassReader cr, ClassWriter cw) {
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // func_73044_a = saveAllChunks(boolean, IProgressUpdate) → 存档后清理
                if ("func_73044_a".equals(name) && "(ZLnet/minecraft/util/IProgressUpdate;)V".equals(desc)) {
                    return new ReturnInjector(mv) {
                        @Override
                        protected void onBeforeReturn() {
                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, NOVA_MEM,
                                "onWorldSave", "(Lnet/minecraft/world/WorldServer;)V", false);
                        }
                    };
                }

                // func_73041_k = tick()V → 清理检测（世界卸载前）
                // 在 tick 方法末尾注入清理，捕获世界卸载场景
                if ("func_73041_k".equals(name) && "()V".equals(desc)) {
                    return new ReturnInjector(mv) {
                        @Override
                        protected void onBeforeReturn() {
                            // 轻量级清理：只清理已死亡实体
                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, NOVA_MEM,
                                "onWorldSave", "(Lnet/minecraft/world/WorldServer;)V", false);
                        }
                    };
                }

                return mv;
            }
        };

        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    private byte[] transformFakePlayer(ClassReader cr, ClassWriter cw) {
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // func_70071_h_ = onUpdate()V
                if ("func_70071_h_".equals(name) && "()V".equals(desc)) {
                    return new HeadInjector(mv) {
                        @Override
                        protected void onMethodEntry() {
                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, NOVA_MEM,
                                "checkFakePlayerStale",
                                "(Lnet/minecraftforge/common/util/FakePlayer;)V", false);
                        }
                    };
                }

                return mv;
            }
        };

        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    private byte[] transformChunk(ClassReader cr, ClassWriter cw) {
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // func_76589_b = onUnload()V → 区块卸载时清理
                if ("func_76589_b".equals(name) && "()V".equals(desc)) {
                    return new HeadInjector(mv) {
                        @Override
                        protected void onMethodEntry() {
                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, NOVA_MEM,
                                "onChunkUnload", "(Lnet/minecraft/world/chunk/Chunk;)V", false);
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
}