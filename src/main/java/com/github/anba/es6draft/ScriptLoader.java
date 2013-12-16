/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static com.github.anba.es6draft.runtime.ExecutionContext.newScriptExecutionContext;

import java.util.EnumSet;

import com.github.anba.es6draft.ast.FunctionDefinition;
import com.github.anba.es6draft.ast.GeneratorDefinition;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.compiler.CompiledFunction;
import com.github.anba.es6draft.compiler.CompiledScript;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.interpreter.InterpretedScript;
import com.github.anba.es6draft.interpreter.Interpreter;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 * <h1>15 ECMAScript Language: Modules and Scripts</h1>
 * <ul>
 * <li>15.2 Script
 * </ul>
 */
public final class ScriptLoader {
    private ScriptLoader() {
    }

    /**
     * [15.2.7 Runtime Semantics: Script Evaluation]
     */
    public static Object ScriptEvaluation(Script script, Realm realm, boolean deletableBindings) {
        /* steps 1-2 */
        RuntimeInfo.ScriptBody scriptBody = script.getScriptBody();
        if (scriptBody == null)
            return null;
        /* step 3 */
        LexicalEnvironment globalEnv = realm.getGlobalEnv();
        /* steps 4-5 */
        scriptBody.globalDeclarationInstantiation(realm.defaultContext(), globalEnv, globalEnv,
                deletableBindings);
        /* steps 6-9 */
        ExecutionContext progCxt = newScriptExecutionContext(script, realm);
        ExecutionContext oldScriptContext = realm.getScriptContext();
        try {
            realm.setScriptContext(progCxt);
            /* steps 10-14 */
            Object result = script.evaluate(progCxt);
            /* step 15 */
            return result;
        } finally {
            realm.setScriptContext(oldScriptContext);
        }
    }

    /**
     * Returns an executable {@link Script} object for given
     * {@link com.github.anba.es6draft.ast.Script} AST-node. This may either be an
     * {@link InterpretedScript} or {@link CompiledScript} instance.
     */
    public static Script load(String className, com.github.anba.es6draft.ast.Script parsedScript)
            throws CompilationException {
        return load(className, parsedScript, EnumSet.noneOf(Compiler.Option.class));
    }

    /**
     * Returns an executable {@link Script} object for given
     * {@link com.github.anba.es6draft.ast.Script} AST-node. This may either be an
     * {@link InterpretedScript} or {@link CompiledScript} instance.
     */
    public static Script load(String className, com.github.anba.es6draft.ast.Script parsedScript,
            EnumSet<Compiler.Option> options) throws CompilationException {
        Script script = Interpreter.script(parsedScript);
        if (script == null) {
            script = compile(className, parsedScript, options);
        }
        return script;
    }

    /**
     * Compiles the given {@link com.github.anba.es6draft.ast.Script} to an executable
     * {@link Script} object
     */
    public static CompiledScript compile(String className,
            com.github.anba.es6draft.ast.Script parsedScript, EnumSet<Compiler.Option> options)
            throws CompilationException {
        // prepend '#' to mark generated classes, cf. ErrorPrototype
        String clazzName = "#" + className;
        Compiler compiler = new Compiler(options);
        byte[] bytes = compiler.compile(parsedScript, clazzName);
        return getScript(clazzName, bytes);
    }

    /**
     * Compiles the given {@link FunctionDefinition} to a
     * {@link com.github.anba.es6draft.runtime.internal.RuntimeInfo.Function} object
     */
    public static RuntimeInfo.Function compile(String className, FunctionDefinition function,
            EnumSet<Compiler.Option> options) throws CompilationException {
        // prepend '#' to mark generated classes, cf. ErrorPrototype
        String clazzName = "#" + className;
        Compiler compiler = new Compiler(options);
        byte[] bytes = compiler.compile(function, clazzName);
        return getFunction(clazzName, bytes);
    }

    /**
     * Compiles the given {@link GeneratorDefinition} to a
     * {@link com.github.anba.es6draft.runtime.internal.RuntimeInfo.Function} object
     */
    public static RuntimeInfo.Function compile(String className, GeneratorDefinition generator,
            EnumSet<Compiler.Option> options) throws CompilationException {
        // prepend '#' to mark generated classes, cf. ErrorPrototype
        String clazzName = "#" + className;
        Compiler compiler = new Compiler(options);
        byte[] bytes = compiler.compile(generator, clazzName);
        return getFunction(clazzName, bytes);
    }

    private static CompiledScript getScript(String className, byte[] bytes) {
        try {
            ClassLoader cl = new ByteClassLoader(className, bytes);
            Class<?> c = cl.loadClass(className);
            CompiledScript instance = (CompiledScript) c.newInstance();
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static RuntimeInfo.Function getFunction(String className, byte[] bytes) {
        try {
            ClassLoader cl = new ByteClassLoader(className, bytes);
            Class<?> c = cl.loadClass(className);
            CompiledFunction instance = (CompiledFunction) c.newInstance();
            return instance.getFunction();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ByteClassLoader extends ClassLoader {
        private final String className;
        private byte[] bytes;

        public ByteClassLoader(String className, byte[] bytes) {
            this(ClassLoader.getSystemClassLoader(), className, bytes);
        }

        public ByteClassLoader(ClassLoader parent, String className, byte[] bytes) {
            super(parent);
            this.className = className;
            this.bytes = bytes;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (className.equals(name)) {
                byte[] bytes = this.bytes;
                this.bytes = null;
                return this.defineClass(name, bytes, 0, bytes.length);
            }
            return super.findClass(name);
        }
    }
}
