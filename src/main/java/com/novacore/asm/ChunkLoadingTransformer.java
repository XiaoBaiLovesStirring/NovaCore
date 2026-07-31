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
 * Module 2: 异步区块加载 — 替换同步IO为异步预加载+并行解析
 *
 * 转换目标:
 *   - AnvilChunkLoader.func_75815_a(World, int, int)Chunk → 委托NovaChunkIO
 *   - AnvilChunkLoader.func_191063_a(int, int)Z → 委托NovaChunkIO
 *   - PlayerChunkMapEntry.func_187280_d()V → 注入异步预加载调度
 */
public class ChunkLoadingTransformer extends NovaTransformer {

    private static final String ANVIL_CHUNK_LOADER = "net.minecraft.world.chunk.storage.AnvilChunkLoader";
    private static final String PLAYER_CHUNK_MAP_ENTRY = "net.minecraft.server.management.PlayerChunkMapEntry";
    private static final String NOVA_CHUNK_IO = "com/novacore/asm/NovaChunkIO";

    private static final String WORLD_TYPE = "Lnet/minecraft/world/World;";
    private static final String CHUNK_TYPE = "Lnet/minecraft/world/chunk/Chunk;";

    private static final String ANVIL_TYPE = "Lnet/minecraft/world/chunk/storage/AnvilChunkLoader;";
    private static final String ENTRY_TYPE = "Lnet/minecraft/server/management/PlayerChunkMapEntry;";

    @Override
    protected String[] getTargetClasses() {
        return new String[]{ANVIL_CHUNK_LOADER, PLAYER_CHUNK_MAP_ENTRY};
    }

    @Override
    protected String getTransformerName() {
        return "ChunkLoad";
    }

    @Override
    protected byte[] doTransform(String className, String transformedName, byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = createClassWriter(cr);

        if (ANVIL_CHUNK_LOADER.equals(transformedName)) {
            return transformAnvilChunkLoader(cr, cw);
        }
        if (PLAYER_CHUNK_MAP_ENTRY.equals(transformedName)) {
            return transformPlayerChunkMapEntry(cr, cw);
        }
        return bytes;
    }

    private byte[] transformAnvilChunkLoader(ClassReader cr, ClassWriter cw) {
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // func_75815_a = loadChunk(World, int, int)Chunk
                if ("func_75815_a".equals(name) &&
                        ("(" + WORLD_TYPE + "II)" + CHUNK_TYPE).equals(desc)) {
                    return new MethodBodyReplacer(mv, access, name, desc) {
                        @Override
                        protected void emitBody(GeneratorAdapter ga) {
                            // loadChunkAsync(AnvilChunkLoader, World, int, int)
                            ga.loadThis();
                            ga.loadArg(0);
                            ga.loadArg(1);
                            ga.loadArg(2);
                            ga.invokeStatic(Type.getType("L" + NOVA_CHUNK_IO + ";"),
                                new Method("loadChunkAsync",
                                    "(" + ANVIL_TYPE + WORLD_TYPE + "II)" + CHUNK_TYPE));
                            ga.returnValue();
                        }
                    };
                }

                // func_191063_a = isChunkGeneratedAt(int, int)Z
                if ("func_191063_a".equals(name) && "(II)Z".equals(desc)) {
                    return new MethodBodyReplacer(mv, access, name, desc) {
                        @Override
                        protected void emitBody(GeneratorAdapter ga) {
                            // chunkExistsFast(AnvilChunkLoader, int, int)
                            ga.loadThis();
                            ga.loadArg(0);
                            ga.loadArg(1);
                            ga.invokeStatic(Type.getType("L" + NOVA_CHUNK_IO + ";"),
                                new Method("chunkExistsFast",
                                    "(" + ANVIL_TYPE + "II)Z"));
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

    private byte[] transformPlayerChunkMapEntry(ClassReader cr, ClassWriter cw) {
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // func_187280_d = update()V
                if ("func_187280_d".equals(name) && "()V".equals(desc)) {
                    return new HeadInjector(mv) {
                        @Override
                        protected void onMethodEntry() {
                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, NOVA_CHUNK_IO,
                                "schedulePreload",
                                "(" + ENTRY_TYPE + ")V", false);
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
}