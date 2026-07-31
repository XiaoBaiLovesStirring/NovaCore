package com.novacore.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

/**
 * 方法体替换器 — 清除原方法体并写入新实现
 * <p>
 * 用法: 在ClassVisitor.visitMethod中返回子类实例，实现emitBody()写入替换代码。
 * 工作原理:
 *   1. visitCode() 被调用时，先调用 super.visitCode() 开始方法
 *   2. 调用 emitBody() 写入替换指令（由子类实现）
 *   3. 设置 replaced=true，阻止原方法体所有指令输出
 *   4. visitMaxs/visitEnd 正常传递给ClassWriter
 * <p>
 * 要求: ClassWriter必须使用 COMPUTE_MAXS | COMPUTE_FRAMES，
 *        ClassReader.accept必须传入 SKIP_FRAMES
 */
abstract class MethodBodyReplacer extends GeneratorAdapter {

    private boolean replaced = false;

    MethodBodyReplacer(MethodVisitor mv, int access, String name, String desc) {
        super(Opcodes.ASM9, mv, access, name, desc);
    }

    /** 子类实现此方法，使用GeneratorAdapter API写入新的方法体 */
    protected abstract void emitBody(GeneratorAdapter ga);

    @Override
    public void visitCode() {
        super.visitCode();
        emitBody(this);
        replaced = true;
    }

    // ===== 阻止原方法体指令输出 =====

    @Override
    public void visitInsn(int opcode) {
        if (!replaced) super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        if (!replaced) super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        if (!replaced) super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (!replaced) super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (!replaced) super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (!replaced) super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if (!replaced) super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        if (!replaced) super.visitLdcInsn(value);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        if (!replaced) super.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        if (!replaced) super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        if (!replaced) super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        if (!replaced) super.visitMultiANewArrayInsn(desc, dims);
    }

    @Override
    public void visitLabel(Label label) {
        if (!replaced) super.visitLabel(label);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        // 替换方法始终屏蔽行号调试信息
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        // 替换方法始终屏蔽局部变量调试信息
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        // COMPUTE_MAXS 会重新计算，传0即可
        super.visitMaxs(0, 0);
    }
}