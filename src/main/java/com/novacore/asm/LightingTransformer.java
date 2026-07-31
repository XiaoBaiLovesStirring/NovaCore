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
 * Module 1: BFS光照引擎 — 替换DFS递归为BFS迭代
 *
 * 转换目标:
 *   - World.func_180500_c(EnumSkyBlock, BlockPos)Z → 委托NovaLightEngine
 *   - World.func_185463_a(EnumSkyBlock, BlockPos)V → 委托NovaLightEngine
 *
 * 注意: func_185463_a (updateLightByType) 可能在某些Forge版本中不存在
 */
public class LightingTransformer extends NovaTransformer {

    private static final String WORLD = "net.minecraft.world.World";
    private static final String NOVA_LIGHT = "com/novacore/asm/NovaLightEngine";

    private static final String ENUM_SKY = "Lnet/minecraft/world/EnumSkyBlock;";
    private static final String BLOCK_POS = "Lnet/minecraft/util/math/BlockPos;";
    private static final String WORLD_TYPE = "Lnet/minecraft/world/World;";

    private static final String CHECK_LIGHT_DESC = "(" + ENUM_SKY + BLOCK_POS + ")Z";
    private static final String UPDATE_LIGHT_DESC = "(" + ENUM_SKY + BLOCK_POS + ")V";

    @Override
    protected String[] getTargetClasses() {
        return new String[]{WORLD};
    }

    @Override
    protected String getTransformerName() {
        return "Lighting";
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

                // func_180500_c = checkLightFor
                if ("func_180500_c".equals(name) && CHECK_LIGHT_DESC.equals(desc)) {
                    return new MethodBodyReplacer(mv, access, name, desc) {
                        @Override
                        protected void emitBody(GeneratorAdapter ga) {
                            ga.loadThis();
                            ga.loadArg(0);
                            ga.loadArg(1);
                            ga.invokeStatic(Type.getType("L" + NOVA_LIGHT + ";"),
                                new Method("checkLightFor",
                                    "(" + WORLD_TYPE + ENUM_SKY + BLOCK_POS + ")Z"));
                            ga.returnValue();
                        }
                    };
                }

                // func_185463_a = updateLightByType (may not exist in all Forge builds)
                if ("func_185463_a".equals(name) && UPDATE_LIGHT_DESC.equals(desc)) {
                    return new MethodBodyReplacer(mv, access, name, desc) {
                        @Override
                        protected void emitBody(GeneratorAdapter ga) {
                            ga.loadThis();
                            ga.loadArg(0);
                            ga.loadArg(1);
                            ga.invokeStatic(Type.getType("L" + NOVA_LIGHT + ";"),
                                new Method("updateLightBFS",
                                    "(" + WORLD_TYPE + ENUM_SKY + BLOCK_POS + ")V"));
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