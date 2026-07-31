package com.novacore.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Module 8: 粒子限制器（EXTREME预设专属）
 *
 * 转换目标（仅客户端）:
 *   1. ParticleManager.func_78873_a(Particle)V → 注入 shouldDiscardParticle 检查
 *   2. ParticleManager.func_78874_a(float)V  → 注入 onUpdateEffects 粒子寿命缩减
 */
public class ParticleLimiterTransformer extends NovaTransformer {

    private static final String PARTICLE_MANAGER = "net.minecraft.client.particle.ParticleManager";
    private static final String PARTICLE_TYPE = "Lnet/minecraft/client/particle/Particle;";
    private static final String PM_TYPE = "Lnet/minecraft/client/particle/ParticleManager;";
    private static final String NOVA_PARTICLE = "com/novacore/asm/NovaParticleLimiter";

    @Override
    protected String[] getTargetClasses() {
        return new String[]{PARTICLE_MANAGER};
    }

    @Override
    protected String getTransformerName() {
        return "ParticleLimit";
    }

    @Override
    protected byte[] doTransform(String className, String transformedName, byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = createClassWriter(cr);

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // func_78873_a = spawnParticle (addEffect)
                if ("func_78873_a".equals(name) && ("(" + PARTICLE_TYPE + ")V").equals(desc)) {
                    return new SpawnParticleInjector(mv);
                }

                // func_78874_a = updateEffects (tick)
                if ("func_78874_a".equals(name) && "(F)V".equals(desc)) {
                    return new HeadInjector(mv) {
                        @Override
                        protected void onMethodEntry() {
                            // NovaParticleLimiter.onUpdateEffects(this)
                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, NOVA_PARTICLE,
                                "onUpdateEffects", "(" + PM_TYPE + ")V", false);
                        }
                    };
                }

                return mv;
            }
        };

        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    /**
     * spawnParticle 注入器：在方法开头检查是否应该丢弃粒子
     *
     * 方法签名: spawnParticle(Particle)V
     *   local 0 = this (ParticleManager)
     *   local 1 = Particle
     */
    static class SpawnParticleInjector extends MethodVisitor {
        private boolean injected = false;

        SpawnParticleInjector(MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            if (!injected) {
                // shouldDiscardParticle(this, particle)
                mv.visitVarInsn(Opcodes.ALOAD, 0); // ParticleManager (this)
                mv.visitVarInsn(Opcodes.ALOAD, 1); // Particle
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, NOVA_PARTICLE,
                    "shouldDiscardParticle",
                    "(" + PM_TYPE + PARTICLE_TYPE + ")Z", false);

                // IFEQ continue_label (if false, don't discard)
                Label continueLabel = new Label();
                mv.visitJumpInsn(Opcodes.IFEQ, continueLabel);
                // RETURN (discard the particle)
                mv.visitInsn(Opcodes.RETURN);
                // continue_label:
                mv.visitLabel(continueLabel);
                injected = true;
            }
        }
    }

    /* ==================== 内部工具类 ==================== */

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