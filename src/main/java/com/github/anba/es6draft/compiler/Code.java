/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.CodeSizeEvaluator;

import com.github.anba.es6draft.compiler.ConstantPoolMethodVisitor.ConstantPool;
import com.github.anba.es6draft.compiler.ConstantPoolMethodVisitor.ExternConstantPool;
import com.github.anba.es6draft.compiler.ConstantPoolMethodVisitor.InlineConstantPool;

/**
 * Class encapsulating generated bytecode
 */
final class Code {
    private static final boolean EVALUATE_SIZE = false;
    private static final int METHOD_LIMIT = 1 << 12;

    private final List<ClassCode> classes = new ArrayList<>();
    private final ClassCode mainClass;
    private ExternConstantPool extern = null;
    private ClassCode currentClass;

    Code(String className, String superClassName, String fileName, String sourceMap) {
        mainClass = newMainClass(this, className, superClassName, fileName, sourceMap);
        classes.add(mainClass);
        setCurrentClass(mainClass);
    }

    private void setCurrentClass(ClassCode currentClass) {
        this.currentClass = currentClass;
    }

    private ClassCode requestClassForMethod() {
        if (currentClass.methodCount() >= METHOD_LIMIT) {
            setCurrentClass(newClass(new InlineConstantPool(this)));
        }
        return currentClass;
    }

    private static ClassCode newMainClass(Code code, String className, String superClassName,
            String fileName, String sourceMap) {
        ConstantPool constantPool = new InlineConstantPool(code);
        return newClass(constantPool, className, superClassName, fileName, sourceMap);
    }

    private static ClassCode newClass(ConstantPool constantPool, String className) {
        return newClass(constantPool, className, "java/lang/Object", null, null);
    }

    private static ClassCode newClass(ConstantPool constantPool, String className,
            String superClassName, String fileName, String sourceMap) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER | Opcodes.ACC_FINAL,
                className, null, superClassName, null);
        cw.visitSource(fileName, sourceMap);

        return new ClassCode(constantPool, className, cw);
    }

    /**
     * Returns the list of generated {@link ClassCode} objects
     */
    List<ClassCode> getClasses() {
        return classes;
    }

    /**
     * Returns the shared extern constant pool instance
     */
    ConstantPool getExternConstantPool() {
        if (extern == null) {
            extern = new ExternConstantPool(this);
        }
        return extern;
    }

    /**
     * Adds a new class
     */
    ClassCode newClass(ConstantPool constantPool) {
        String className = mainClass.className + '~' + classes.size();
        ClassCode classCode = newClass(constantPool, className);
        classes.add(classCode);
        return classCode;
    }

    /**
     * Add a new method to main class module
     */
    MethodCode newMainMethod(int access, String methodName, String methodDescriptor) {
        return mainClass.newMethod(access, methodName, methodDescriptor, null, null);
    }

    /**
     * Add a new method to a class module
     */
    MethodCode newMethod(int access, String methodName, String methodDescriptor) {
        return requestClassForMethod().newMethod(access, methodName, methodDescriptor, null, null);
    }

    /**
     * Class representing method code
     */
    static final class MethodCode {
        final ClassCode classCode;
        final int access;
        final String methodName;
        final String methodDescriptor;
        final MethodVisitor methodVisitor;

        MethodCode(ClassCode classCode, int access, String methodName, String methodDescriptor,
                MethodVisitor methodVisitor) {
            this.classCode = classCode;
            this.access = access;
            this.methodName = methodName;
            this.methodDescriptor = methodDescriptor;
            this.methodVisitor = methodVisitor;
        }
    }

    /**
     * Class representing class code
     */
    static final class ClassCode {
        private int methodCount = 0;
        final ConstantPool constantPool;
        final String className;
        final ClassWriter classWriter;

        ClassCode(ConstantPool constantPool, String className, ClassWriter classWriter) {
            this.constantPool = constantPool;
            this.className = className;
            this.classWriter = classWriter;
        }

        int methodCount() {
            return methodCount;
        }

        byte[] toByteArray() {
            constantPool.close();
            classWriter.visitEnd();
            return classWriter.toByteArray();
        }

        MethodCode newMethod(int access, String methodName, String methodDescriptor,
                String signature, String[] exceptions) {
            methodCount += 1;
            MethodVisitor mv = classWriter.visitMethod(access, methodName, methodDescriptor,
                    signature, exceptions);
            if (constantPool instanceof InlineConstantPool) {
                mv = new ConstantPoolMethodVisitor(mv, (InlineConstantPool) constantPool);
            }
            if (EVALUATE_SIZE) {
                mv = new $CodeSizeEvaluator(methodName, mv);
            }
            return new MethodCode(this, access, methodName, methodDescriptor, mv);
        }
    }

    private static final class $CodeSizeEvaluator extends CodeSizeEvaluator {
        private final String methodName;

        $CodeSizeEvaluator(String methodName, MethodVisitor mv) {
            super(Opcodes.ASM4, mv);
            this.methodName = methodName;
        }

        @Override
        public void visitEnd() {
            System.out.printf("%s: [%d, %d]\n", methodName, getMinSize(), getMaxSize());
            super.visitEnd();
        }
    }
}
