package com.novacore.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Module 3: 内存泄漏修复 — 零反射、零Helper、纯ASM字节码注入版
 *
 * 所有 Minecraft 字段/方法访问全部使用 ASM 字节码 + SRG 名直接注入。
 * AT 已打通所有字段为 public。
 *
 * 注入点:
 *   1. WorldServer.func_73044_a (saveAllChunks) → RETURN 前注入实体清理 + TileEntity 列表清理
 *   2. FakePlayer.func_70071_h_ (onUpdate) → 方法开头注入过期假人检测
 *   3. Chunk.func_76589_b (onUnload) → 方法开头注入 TileEntity 批量 invalidate
 */
public class MemoryLeakTransformer extends NovaTransformer {

    // ==================== 目标类 MCP 全限定名 ====================

    private static final String WORLD_SERVER = "net.minecraft.world.WorldServer";
    private static final String FAKE_PLAYER  = "net.minecraftforge.common.util.FakePlayer";
    private static final String CHUNK        = "net.minecraft.world.chunk.Chunk";

    // ==================== SRG 内部名 ====================

    private static final String WORLD_INTERNAL      = "net/minecraft/world/World";
    private static final String WORLD_SERVER_INTERNAL = "net/minecraft/world/WorldServer";
    private static final String ENTITY_INTERNAL      = "net/minecraft/entity/Entity";
    private static final String BLOCK_POS_INTERNAL   = "net/minecraft/util/math/BlockPos";
    private static final String TILE_ENTITY_INTERNAL = "net/minecraft/tileentity/TileEntity";
    private static final String CHUNK_INTERNAL       = "net/minecraft/world/chunk/Chunk";

    // ==================== SRG 字段名 ====================

    // World
    private static final String FIELD_LOADED_ENTITY_LIST    = "field_72996_f";  // List<Entity>
    private static final String FIELD_LOADED_TILE_ENTITY    = "field_147482_g";  // List<TileEntity>
    private static final String FIELD_TICKABLE_TILE_ENTITIES = "field_175730_i"; // List<TileEntity>
    private static final String FIELD_PLAYER_ENTITIES       = "field_73010_i";  // List<EntityPlayer>

    // Entity
    private static final String FIELD_IS_DEAD       = "field_70128_L";  // boolean
    private static final String FIELD_ADDED_TO_CHUNK = "field_70156_m";  // boolean
    private static final String FIELD_POS_X         = "field_70165_t";  // double
    private static final String FIELD_POS_Y         = "field_70163_u";  // double
    private static final String FIELD_POS_Z         = "field_70161_v";  // double
    private static final String FIELD_WORLD         = "field_70170_p";  // World
    private static final String FIELD_TICKS_EXISTED = "field_70173_aa"; // int

    // ==================== SRG 方法名 ====================

    // Entity
    private static final String METHOD_SET_DEAD = "func_70106_y";  // ()V

    // World
    private static final String METHOD_IS_BLOCK_LOADED = "func_175667_e";  // (LBlockPos;)Z

    // Chunk
    private static final String METHOD_GET_TILE_ENTITY_MAP = "func_177434_y";  // ()Ljava/util/Map;

    // TileEntity
    private static final String METHOD_INVALIDATE = "func_145845_h";  // ()V

    // ==================== 方法描述符 ====================

    private static final String DESC_SAVE_ALL_CHUNKS = "(ZLnet/minecraft/util/IProgressUpdate;)V";
    private static final String DESC_ON_UPDATE       = "()V";
    private static final String DESC_ON_UNLOAD       = "()V";

    private static final String DESC_LIST         = "Ljava/util/List;";
    private static final String DESC_MAP          = "Ljava/util/Map;";
    private static final String DESC_WORLD        = "Lnet/minecraft/world/World;";
    private static final String DESC_BLOCK_POS    = "Lnet/minecraft/util/math/BlockPos;";
    private static final String DESC_ENTITY       = "Lnet/minecraft/entity/Entity;";
    private static final String DESC_TILE_ENTITY  = "Lnet/minecraft/tileentity/TileEntity;";

    // ==================== Transformer 接口 ====================

    @Override
    protected String[] getTargetClasses() {
        return new String[] { WORLD_SERVER, FAKE_PLAYER, CHUNK };
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

    // ==================== WorldServer 转换 ====================

    /**
     * 在 func_73044_a (saveAllChunks) 的 RETURN 之前注入：
     *   1. 遍历 loadedEntityList，移除 dead 且在未加载区块中的实体
     *   2. 清空 loadedTileEntityList 和 tickableTileEntities
     */
    private byte[] transformWorldServer(ClassReader cr, ClassWriter cw) {
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                if ("func_73044_a".equals(name) && DESC_SAVE_ALL_CHUNKS.equals(desc)) {
                    return new ReturnInjector(mv) {
                        @Override
                        protected void onBeforeReturn() {
                            injectEntityCleanup(mv);
                            injectTileEntityListClear(mv);
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
     * 生成字节码：
     * <pre>
     * Iterator it = this.field_72996_f.iterator();
     * while (it.hasNext()) {
     *     Entity e = (Entity) it.next();
     *     if (!e.field_70128_L) continue;
     *     BlockPos bp = new BlockPos((int) e.field_70165_t, (int) e.field_70163_u, (int) e.field_70161_v);
     *     if (this.func_175667_e(bp)) continue;
     *     it.remove();
     * }
     * </pre>
     *
     * 局部变量: slot 3 = Iterator, slot 4 = Entity
     */
    private void injectEntityCleanup(MethodVisitor mv) {
        // Iterator it = this.field_72996_f.iterator();
        mv.visitVarInsn(Opcodes.ALOAD, 0);                                           // this
        mv.visitFieldInsn(Opcodes.GETFIELD, WORLD_INTERNAL, FIELD_LOADED_ENTITY_LIST, DESC_LIST);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "iterator",
                "()Ljava/util/Iterator;", true);
        int iterSlot = 3;
        mv.visitVarInsn(Opcodes.ASTORE, iterSlot);

        // while (it.hasNext())
        Label loopStart = new Label();
        mv.visitLabel(loopStart);
        mv.visitVarInsn(Opcodes.ALOAD, iterSlot);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Iterator", "hasNext",
                "()Z", true);
        Label loopEnd = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, loopEnd);

        // Entity e = (Entity) it.next();
        mv.visitVarInsn(Opcodes.ALOAD, iterSlot);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Iterator", "next",
                "()Ljava/lang/Object;", true);
        mv.visitTypeInsn(Opcodes.CHECKCAST, ENTITY_INTERNAL);
        int entitySlot = 4;
        mv.visitVarInsn(Opcodes.ASTORE, entitySlot);

        // if (!e.field_70128_L) continue;
        mv.visitVarInsn(Opcodes.ALOAD, entitySlot);
        mv.visitFieldInsn(Opcodes.GETFIELD, ENTITY_INTERNAL, FIELD_IS_DEAD, "Z");
        mv.visitJumpInsn(Opcodes.IFEQ, loopStart);

        // BlockPos bp = new BlockPos((int) e.field_70165_t, (int) e.field_70163_u, (int) e.field_70161_v);
        // if (this.func_175667_e(bp)) continue;
        mv.visitVarInsn(Opcodes.ALOAD, 0);                                           // this (World)
        mv.visitTypeInsn(Opcodes.NEW, BLOCK_POS_INTERNAL);
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, entitySlot);
        mv.visitFieldInsn(Opcodes.GETFIELD, ENTITY_INTERNAL, FIELD_POS_X, "D");
        mv.visitInsn(Opcodes.D2I);
        mv.visitVarInsn(Opcodes.ALOAD, entitySlot);
        mv.visitFieldInsn(Opcodes.GETFIELD, ENTITY_INTERNAL, FIELD_POS_Y, "D");
        mv.visitInsn(Opcodes.D2I);
        mv.visitVarInsn(Opcodes.ALOAD, entitySlot);
        mv.visitFieldInsn(Opcodes.GETFIELD, ENTITY_INTERNAL, FIELD_POS_Z, "D");
        mv.visitInsn(Opcodes.D2I);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, BLOCK_POS_INTERNAL, "<init>",
                "(III)V", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, WORLD_INTERNAL, METHOD_IS_BLOCK_LOADED,
                "(" + DESC_BLOCK_POS + ")Z", false);
        mv.visitJumpInsn(Opcodes.IFNE, loopStart);

        // it.remove();
        mv.visitVarInsn(Opcodes.ALOAD, iterSlot);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Iterator", "remove",
                "()V", true);
        mv.visitJumpInsn(Opcodes.GOTO, loopStart);

        // loop end
        mv.visitLabel(loopEnd);
    }

    /**
     * 生成字节码：清空 loadedTileEntityList 和 tickableTileEntities
     * <pre>
     * this.field_147482_g.clear();
     * this.field_175730_i.clear();
     * </pre>
     */
    private void injectTileEntityListClear(MethodVisitor mv) {
        // this.field_147482_g.clear();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, WORLD_INTERNAL, FIELD_LOADED_TILE_ENTITY, DESC_LIST);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "clear", "()V", true);

        // this.field_175730_i.clear();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, WORLD_INTERNAL, FIELD_TICKABLE_TILE_ENTITIES, DESC_LIST);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "clear", "()V", true);
    }

    // ==================== FakePlayer 转换 ====================

    /**
     * 在 func_70071_h_ (onUpdate) 方法开头注入：
     * <pre>
     * if (this.field_70173_aa > 12000 && !this.field_70170_p.field_73010_i.contains(this))
     *     this.func_70106_y();
     * </pre>
     */
    private byte[] transformFakePlayer(ClassReader cr, ClassWriter cw) {
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                if ("func_70071_h_".equals(name) && DESC_ON_UPDATE.equals(desc)) {
                    return new HeadInjector(mv) {
                        @Override
                        protected void onMethodEntry() {
                            injectFakePlayerStaleCheck(mv);
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
     * 生成字节码：
     * <pre>
     * if (this.field_70173_aa > 12000 && !this.field_70170_p.field_73010_i.contains(this))
     *     this.func_70106_y();
     * </pre>
     */
    private void injectFakePlayerStaleCheck(MethodVisitor mv) {
        // if (this.field_70173_aa <= 12000) goto end;
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, ENTITY_INTERNAL, FIELD_TICKS_EXISTED, "I");
        mv.visitIntInsn(Opcodes.SIPUSH, 12000);
        Label end = new Label();
        mv.visitJumpInsn(Opcodes.IF_ICMPLE, end);

        // if (this.field_70170_p.field_73010_i.contains(this)) goto end;
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, ENTITY_INTERNAL, FIELD_WORLD, DESC_WORLD);
        mv.visitFieldInsn(Opcodes.GETFIELD, WORLD_INTERNAL, FIELD_PLAYER_ENTITIES, DESC_LIST);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "contains",
                "(Ljava/lang/Object;)Z", true);
        mv.visitJumpInsn(Opcodes.IFNE, end);

        // this.func_70106_y();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ENTITY_INTERNAL, METHOD_SET_DEAD,
                "()V", false);

        mv.visitLabel(end);
    }

    // ==================== Chunk 转换 ====================

    /**
     * 在 func_76589_b (onUnload) 方法开头注入：
     * <pre>
     * for (TileEntity te : this.func_177434_y().values())
     *     te.func_145845_h();
     * </pre>
     */
    private byte[] transformChunk(ClassReader cr, ClassWriter cw) {
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                if ("func_76589_b".equals(name) && DESC_ON_UNLOAD.equals(desc)) {
                    return new HeadInjector(mv) {
                        @Override
                        protected void onMethodEntry() {
                            injectTileEntityInvalidate(mv);
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
     * 生成字节码：
     * <pre>
     * Iterator it = this.func_177434_y().values().iterator();
     * while (it.hasNext()) {
     *     TileEntity te = (TileEntity) it.next();
     *     te.func_145845_h();
     * }
     * </pre>
     *
     * 局部变量: slot 1 = Iterator, slot 2 = TileEntity
     */
    private void injectTileEntityInvalidate(MethodVisitor mv) {
        // Iterator it = this.func_177434_y().values().iterator();
        mv.visitVarInsn(Opcodes.ALOAD, 0);                                           // this
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CHUNK_INTERNAL,
                METHOD_GET_TILE_ENTITY_MAP, "()" + DESC_MAP, false);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "values",
                "()Ljava/util/Collection;", true);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Collection", "iterator",
                "()Ljava/util/Iterator;", true);
        int iterSlot = 1;
        mv.visitVarInsn(Opcodes.ASTORE, iterSlot);

        // while (it.hasNext())
        Label loopStart = new Label();
        mv.visitLabel(loopStart);
        mv.visitVarInsn(Opcodes.ALOAD, iterSlot);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Iterator", "hasNext",
                "()Z", true);
        Label loopEnd = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, loopEnd);

        // TileEntity te = (TileEntity) it.next();
        mv.visitVarInsn(Opcodes.ALOAD, iterSlot);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Iterator", "next",
                "()Ljava/lang/Object;", true);
        mv.visitTypeInsn(Opcodes.CHECKCAST, TILE_ENTITY_INTERNAL);
        int teSlot = 2;
        mv.visitVarInsn(Opcodes.ASTORE, teSlot);

        // te.func_145845_h();
        mv.visitVarInsn(Opcodes.ALOAD, teSlot);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, TILE_ENTITY_INTERNAL, METHOD_INVALIDATE,
                "()V", false);

        mv.visitJumpInsn(Opcodes.GOTO, loopStart);

        // loop end
        mv.visitLabel(loopEnd);
    }

    // ==================== 内部工具类 ====================

    /**
     * 在方法的第一个 RETURN 指令之前注入代码。
     * 使用 injected 标志确保只注入一次（即使方法有多个 RETURN）。
     */
    abstract static class ReturnInjector extends MethodVisitor {
        private boolean injected = false;

        ReturnInjector(MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
        }

        /** 子类实现：在 RETURN 之前生成要注入的字节码 */
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

    /**
     * 在方法体第一条指令之前注入代码。
     * 使用 injected 标志确保只注入一次。
     */
    abstract static class HeadInjector extends MethodVisitor {
        private boolean injected = false;

        HeadInjector(MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
        }

        /** 子类实现：在方法入口处生成要注入的字节码 */
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