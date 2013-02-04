/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.DefaultCodeGenerator.tailCall;
import static com.github.anba.es6draft.semantics.StaticSemantics.TemplateStrings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.MethodGenerator.Register;
import com.github.anba.es6draft.runtime.internal.ImmediateFuture;
import com.github.anba.es6draft.runtime.internal.SourceCompressor;

/**
 * 
 */
class CodeGenerator {
    private static final boolean INCLUDE_SOURCE = true;
    private static final Future<String> NO_SOURCE = new ImmediateFuture<>(null);

    private final ClassWriter cw;
    private final String className;
    private ExecutorService sourceCompressor;

    private StatementGenerator stmtgen = new StatementGenerator(this);
    private ExpressionGenerator exprgen = new ExpressionGenerator(this);
    private PropertyGenerator propgen = new PropertyGenerator(this);

    CodeGenerator(ClassWriter cw, String className) {
        this.cw = cw;
        this.className = className;
        if (INCLUDE_SOURCE) {
            this.sourceCompressor = Executors.newFixedThreadPool(1);
        }
    }

    String getClassName() {
        return className;
    }

    void close() {
        if (INCLUDE_SOURCE) {
            sourceCompressor.shutdown();
        }
        sourceCompressor = null;
    }

    private Future<String> compressed(String source) {
        if (INCLUDE_SOURCE) {
            return sourceCompressor.submit(SourceCompressor.compress(source));
        } else {
            return NO_SOURCE;
        }
    }

    MethodVisitor publicStaticMethod(String methodName, String methodDescriptor) {
        int access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
        String signature = null;
        String[] exceptions = null;
        return cw.visitMethod(access, methodName, methodDescriptor, signature, exceptions);
    }

    InstructionVisitor publicStaticMethod(String methodName, Type methodDescriptor) {
        MethodVisitor mv = publicStaticMethod(methodName, methodDescriptor.getInternalName());
        return new InstructionVisitor(mv, methodName, methodDescriptor);
    }

    // template strings
    private Map<TemplateLiteral, String> templateKeys = new HashMap<>();

    private String templateKey(TemplateLiteral template) {
        String key = templateKeys.get(template);
        if (key == null) {
            templateKeys.put(template, key = UUID.randomUUID().toString());
        }
        return key;
    }

    // method names
    private Map<Node, String> methodNames = new HashMap<>(32);
    private AtomicInteger methodCounter = new AtomicInteger(0);

    private final int nextMethodInt() {
        return methodCounter.incrementAndGet();
    }

    private final boolean __isCompiled(Node node) {
        return methodNames.containsKey(node);
    }

    private final String __methodName(Node node, String defaultValue) {
        String n = methodNames.get(node);
        if (n == null) {
            n = node.accept(FunctionName.INSTANCE, defaultValue) + "_" + nextMethodInt();
            methodNames.put(node, n);
        }
        return n;
    }

    private final String methodName(TemplateLiteral node) {
        return __methodName(node, "template");
    }

    final String methodName(GeneratorComprehension node) {
        return __methodName(node, "gencompr");
    }

    final String methodName(FunctionNode node) {
        return __methodName(node, "anonymous");
    }

    private final boolean isCompiled(TemplateLiteral node) {
        return __isCompiled(node);
    }

    private final boolean isCompiled(GeneratorComprehension node) {
        return __isCompiled(node);
    }

    private final boolean isCompiled(FunctionNode node) {
        return __isCompiled(node);
    }

    /**
     * [11.1.9] Runtime Semantics: GetTemplateCallSite Abstract Operation
     */
    void GetTemplateCallSite(TemplateLiteral node, MethodGenerator mv) {
        assert isCompiled(node);
        String methodName = methodName(node);
        String desc = Type.getMethodDescriptor(Types.String_);

        // GetTemplateCallSite
        mv.aconst(templateKey(node));
        mv.invokeStaticMH(className, methodName, desc);
        mv.load(Register.ExecutionContext);
        mv.invoke(Methods.ScriptRuntime_GetTemplateCallSite);
    }

    void compile(TemplateLiteral node) {
        if (!isCompiled(node)) {
            String name = methodName(node);
            Type desc = Type.getMethodType(Types.String_);
            InstructionVisitor body = publicStaticMethod(name, desc);
            body.lineInfo(node.getLine());
            body.begin();

            List<TemplateCharacters> strings = TemplateStrings(node);
            body.newarray(strings.size() * 2, Types.String);
            for (int i = 0, size = strings.size(); i < size; ++i) {
                TemplateCharacters e = strings.get(i);
                int index = i << 1;
                body.astore(index, e.getValue(), Types.String);
                body.astore(index + 1, e.getRawValue(), Types.String);
            }

            body.areturn();
            body.end();
        }
    }

    void compile(Script node) {
        // initialisation method
        new GlobalDeclarationInstantiationGenerator(this).generate(node);

        // TODO: only generate eval-script-init when requested
        new EvalDeclarationInstantiationGenerator(this).generate(node);

        // runtime method
        if (node.getStatements().size() < STATEMENTS_THRESHOLD) {
            singleScript(node);
        } else {
            multiScript(node);
        }

        // runtime-info method
        new RuntimeInfoGenerator(this).runtimeInfo(node);
    }

    private static final int STATEMENTS_THRESHOLD = 300;

    private void singleScript(Script node) {
        StatementMethodGenerator mv = new ScriptMethodGenerator(this, node);
        mv.lineInfo(node);
        mv.begin();

        for (StatementListItem stmt : node.getStatements()) {
            statement(stmt, mv);
        }

        mv.loadCompletionValue();
        mv.areturn();
        mv.end();
    }

    private void multiScript(Script node) {
        // split script into several parts to pass all test262 test cases
        String desc = ScriptChunkMethodGenerator.methodDescriptor.getInternalName();

        List<StatementListItem> statements = node.getStatements();
        int num = statements.size();
        int index = 0;
        for (int end = 0, start = 0; end < num; start += STATEMENTS_THRESHOLD, ++index) {
            end = Math.min(start + STATEMENTS_THRESHOLD, num);
            StatementMethodGenerator mv = new ScriptChunkMethodGenerator(this, node, index);
            mv.lineInfo(statements.get(start));
            mv.begin();

            for (StatementListItem stmt : statements.subList(start, end)) {
                statement(stmt, mv);
            }

            mv.loadCompletionValue();
            mv.areturn();
            mv.end();
        }

        StatementMethodGenerator mv = new ScriptMethodGenerator(this, node);
        mv.lineInfo(node);
        mv.begin();

        for (int i = 0; i < index; ++i) {
            mv.load(Register.ExecutionContext);
            mv.loadCompletionValue();
            mv.invokestatic(getClassName(), "script_" + i, desc);
            mv.storeCompletionValue();
        }

        mv.loadCompletionValue();
        mv.areturn();
        mv.end();
    }

    void compile(GeneratorComprehension node, MethodGenerator mv) {
        if (!isCompiled(node)) {
            MethodGenerator body = new GeneratorComprehensionMethodGenerator(this, node, mv);
            body.lineInfo(node);
            body.begin();

            new GeneratorComprehensionGenerator(this).visit(node, body);

            body.get(Fields.Undefined_UNDEFINED);
            body.areturn();
            body.end();
        }
    }

    void compile(FunctionNode node) {
        if (!isCompiled(node)) {
            Future<String> source = compressed(node.getSource());

            // initialisation method
            new FunctionDeclarationInstantiationGenerator(this).generate(node);

            // runtime method
            if (node instanceof ArrowFunction && (((ArrowFunction) node).getExpression() != null)) {
                conciseFunctionBody((ArrowFunction) node);
            } else {
                functionBody(node);
            }

            // runtime-info method
            new RuntimeInfoGenerator(this).runtimeInfo(node, source);
        }
    }

    private void conciseFunctionBody(ArrowFunction node) {
        MethodGenerator body = new ArrowFunctionMethodGenerator(this, node);
        body.lineInfo(node);
        body.begin();

        // call expression in concise function is always in tail-call position
        tailCall(node.getExpression(), body);

        ValType type = expression(node.getExpression(), body);
        body.toBoxed(type);
        invokeGetValue(node.getExpression(), body);

        body.areturn();
        body.end();
    }

    private void functionBody(FunctionNode node) {
        StatementMethodGenerator body = new FunctionMethodGenerator(this, node);
        body.lineInfo(node);
        body.begin();

        for (StatementListItem stmt : node.getStatements()) {
            statement(stmt, body);
        }

        body.mark(body.returnLabel());
        body.loadCompletionValue();
        body.areturn();
        body.end();
    }

    private void invokeGetValue(Expression node, MethodGenerator mv) {
        if (node.accept(IsReference.INSTANCE, null)) {
            mv.load(Register.Realm);
            mv.invoke(Methods.Reference_GetValue);
        }
    }

    /* ----------------------------------------------------------------------------------------- */

    ValType expression(Expression node, MethodGenerator mv) {
        return node.accept(exprgen, mv);
    }

    void propertyDefinition(PropertyDefinition node, MethodGenerator mv) {
        node.accept(propgen, mv);
    }

    void statement(StatementListItem node, StatementMethodGenerator mv) {
        node.accept(stmtgen, mv);
    }

    /* ----------------------------------------------------------------------------------------- */

    private abstract static class StatementMethodGeneratorImpl extends StatementMethodGenerator {
        private static final int COMPLETION_SLOT = 1;
        private static final Type COMPLETION_TYPE = Types.Object;

        private final boolean initCompletionValue;

        protected StatementMethodGeneratorImpl(CodeGenerator codegen, String methodName,
                Type methodDescriptor, boolean strict, boolean global, boolean completionValue,
                boolean initCompletionValue) {
            super(codegen.publicStaticMethod(methodName, methodDescriptor.getInternalName()),
                    methodName, methodDescriptor, strict, global, completionValue);
            this.initCompletionValue = initCompletionValue;
            reserveFixedSlot(COMPLETION_SLOT, COMPLETION_TYPE);
        }

        @Override
        void storeCompletionValue() {
            store(COMPLETION_SLOT, COMPLETION_TYPE);
        }

        @Override
        void loadCompletionValue() {
            load(COMPLETION_SLOT, COMPLETION_TYPE);
        }

        @Override
        public void begin() {
            super.begin();
            if (initCompletionValue) {
                get(Fields.Undefined_UNDEFINED);
                storeCompletionValue();
            }
            load(Register.ExecutionContext);
            invoke(Methods.ExecutionContext_getRealm);
            store(Register.Realm);
        }

        @Override
        protected int var(Register reg) {
            switch (reg) {
            case ExecutionContext:
                return 0;
                // 1 = completion slot
            case Realm:
                return 2;
            default:
                assert false : reg;
                return -1;
            }
        }
    }

    private abstract static class ExpressionMethodGeneratorImpl extends MethodGenerator {
        protected ExpressionMethodGeneratorImpl(CodeGenerator codegen, String methodName,
                Type methodDescriptor, boolean strict, boolean global) {
            super(codegen.publicStaticMethod(methodName, methodDescriptor.getInternalName()),
                    methodName, methodDescriptor, strict, global, false);
        }

        @Override
        public void begin() {
            super.begin();
            load(Register.ExecutionContext);
            invoke(Methods.ExecutionContext_getRealm);
            store(Register.Realm);
        }

        @Override
        protected int var(Register reg) {
            switch (reg) {
            case ExecutionContext:
                return 0;
            case Realm:
                return 1;
            default:
                assert false : reg;
                return -1;
            }
        }
    }

    private static class ScriptMethodGenerator extends StatementMethodGeneratorImpl {
        static final String methodName = "script";
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext);

        private ScriptMethodGenerator(CodeGenerator codegen, Script node) {
            super(codegen, methodName, methodDescriptor, node.isStrict(), node.isGlobal(), true,
                    true);
        }
    }

    private static class ScriptChunkMethodGenerator extends StatementMethodGeneratorImpl {
        static final String methodName = "script_";
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext, Types.Object);

        private ScriptChunkMethodGenerator(CodeGenerator codegen, Script node, int index) {
            super(codegen, methodName + index, methodDescriptor, node.isStrict(), node.isGlobal(),
                    true, false);
        }
    }

    private static class FunctionMethodGenerator extends StatementMethodGeneratorImpl {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext);

        private FunctionMethodGenerator(CodeGenerator codegen, FunctionNode node) {
            super(codegen, codegen.methodName(node), methodDescriptor, node.isStrict(), false,
                    false, true);
        }
    }

    private static class ArrowFunctionMethodGenerator extends ExpressionMethodGeneratorImpl {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext);

        private ArrowFunctionMethodGenerator(CodeGenerator codegen, ArrowFunction node) {
            super(codegen, codegen.methodName(node), methodDescriptor, node.isStrict(), false);
        }
    }

    private static class GeneratorComprehensionMethodGenerator extends
            ExpressionMethodGeneratorImpl {
        static final Type methodDescriptor = Type.getMethodType(Types.Object,
                Types.ExecutionContext);

        private GeneratorComprehensionMethodGenerator(CodeGenerator codegen,
                GeneratorComprehension node, MethodGenerator parent) {
            super(codegen, codegen.methodName(node), methodDescriptor, parent.isStrict(), parent
                    .isGlobal());
        }
    }
}
