/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newDeclarativeEnvironment;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newFunctionEnvironment;
import static java.util.Objects.requireNonNull;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.ThisMode;

/**
 * <h1>8 Executable Code and Execution Contexts</h1>
 * <ul>
 * <li>8.3 Execution Contexts
 * </ul>
 */
public final class ExecutionContext {
    private final Realm realm;
    private final LexicalEnvironment varEnv;
    private LexicalEnvironment lexEnv;
    private final Script script;
    private final FunctionObject function;
    private GeneratorObject generator = null;

    private ExecutionContext(Realm realm, LexicalEnvironment varEnv, LexicalEnvironment lexEnv,
            Script script, FunctionObject function) {
        this.realm = realm;
        this.varEnv = varEnv;
        this.lexEnv = lexEnv;
        this.script = script;
        this.function = function;
    }

    public Realm getRealm() {
        return realm;
    }

    public LexicalEnvironment getLexicalEnvironment() {
        return lexEnv;
    }

    public LexicalEnvironment getVariableEnvironment() {
        return varEnv;
    }

    public ScriptObject getIntrinsic(Intrinsics id) {
        return realm.getIntrinsic(id);
    }

    public Script getCurrentScript() {
        return script;
    }

    public FunctionObject getCurrentFunction() {
        return function;
    }

    public GeneratorObject getCurrentGenerator() {
        assert generator != null;
        return generator;
    }

    public void setCurrentGenerator(GeneratorObject generator) {
        assert this.generator == null;
        this.generator = requireNonNull(generator);
    }

    // Helper
    public void pushLexicalEnvironment(LexicalEnvironment lexEnv) {
        assert lexEnv.getOuter() == this.lexEnv;
        this.lexEnv = lexEnv;
    }

    // Helper
    public void popLexicalEnvironment() {
        this.lexEnv = lexEnv.getOuter();
    }

    // Helper
    public void restoreLexicalEnvironment(LexicalEnvironment lexEnv) {
        this.lexEnv = lexEnv;
    }

    /**
     * <div>
     * <ul>
     * <li>15 ECMAScript Language: Modules and Scripts
     * <ul>
     * <li>15.1 Script
     * <ul>
     * <li>15.1.2 Runtime Semantics</div>
     * <p>
     * Runtime Semantics: Script Evaluation
     */
    public static ExecutionContext newScriptExecutionContext(Realm realm, Script script) {
        /* steps 3-6 */
        return new ExecutionContext(realm, realm.getGlobalEnv(), realm.getGlobalEnv(), script, null);
    }

    /**
     * <div>
     * <ul>
     * <li>15 ECMAScript Language: Modules and Scripts
     * <ul>
     * <li>15.2 Modules
     * <ul>
     * <li>15.2.6 Runtime Semantics: Module Evaluation</div>
     * <p>
     * 15.2.6.2 EnsureEvaluated(mod, seen, loader) Abstract Operation
     */
    public static ExecutionContext newModuleExecutionContext(Realm realm, LexicalEnvironment env) {
        /* steps 8-11 */
        return new ExecutionContext(realm, env, env, null, null);
    }

    /**
     * <div>
     * <ul>
     * <li>18 The Global Object
     * <ul>
     * <li>18.2 Function Properties of the Global Object</div>
     * <p>
     * 18.2.1 eval (x)
     */
    public static ExecutionContext newEvalExecutionContext(ExecutionContext callerContext,
            LexicalEnvironment varEnv, LexicalEnvironment lexEnv) {
        /* steps 17-20 */
        return new ExecutionContext(callerContext.realm, varEnv, lexEnv, callerContext.script,
                callerContext.function);
    }

    /**
     * <div>
     * <ul>
     * <li>9 Ordinary and Exotic Objects Behaviours
     * <ul>
     * <li>9.2 ECMAScript Function Objects</div>
     * <p>
     * 9.2.4 [[Call]] (thisArgument, argumentsList)
     */
    public static ExecutionContext newFunctionExecutionContext(ExecutionContext callerContext,
            FunctionObject f, Object thisArgument) {
        /* 9.1.15.1, steps 4-12 */
        Realm calleeRealm = f.getRealm();
        ThisMode thisMode = f.getThisMode();
        LexicalEnvironment localEnv;
        if (thisMode == ThisMode.Lexical) {
            localEnv = newDeclarativeEnvironment(f.getEnvironment());
        } else {
            Object thisValue;
            if (thisMode == ThisMode.Strict) {
                thisValue = thisArgument;
            } else {
                assert thisArgument != null;
                switch (Type.of(thisArgument)) {
                case Undefined:
                case Null:
                    thisValue = calleeRealm.getGlobalThis();
                    break;
                case Boolean:
                case Number:
                case String:
                case Symbol:
                    // FIXME: spec bug - https://bugs.ecmascript.org/show_bug.cgi?id=2484
                    thisValue = ToObject(callerContext, thisArgument);
                    break;
                case Object:
                default:
                    thisValue = thisArgument;
                    break;
                }
            }
            localEnv = newFunctionEnvironment(callerContext, f, thisValue);
        }
        return new ExecutionContext(calleeRealm, localEnv, localEnv, null, f);
    }

    /**
     * Combined {@link #resolveBinding(String, boolean)} with
     * {@link Reference#GetValue(Object, ExecutionContext)} internal method
     */
    public Object resolveBindingValue(String name, boolean strict) {
        return LexicalEnvironment.getIdentifierValueOrThrow(lexEnv, name, strict);
    }

    /**
     * 8.3.1 ResolveBinding(name)
     */
    public Reference<EnvironmentRecord, String> resolveBinding(String name, boolean strict) {
        /* steps 1-3 */
        return LexicalEnvironment.getIdentifierReference(lexEnv, name, strict);
    }

    /**
     * 8.3.2 GetThisEnvironment
     */
    public EnvironmentRecord getThisEnvironment() {
        /* step 1 */
        LexicalEnvironment lex = lexEnv;
        /* step 2 */
        for (;;) {
            EnvironmentRecord envRec = lex.getEnvRec();
            boolean exists = envRec.hasThisBinding();
            if (exists) {
                return envRec;
            }
            lex = lex.getOuter();
        }
    }

    /**
     * 8.3.3 ResolveThisBinding
     */
    public Object resolveThisBinding() {
        /* step 1 */
        EnvironmentRecord env = getThisEnvironment();
        /* step 2 */
        return env.getThisBinding();
    }

    /**
     * 8.3.4 GetGlobalObject
     */
    public GlobalObject getGlobalObject() {
        /* steps 1-2 */
        Realm currentRealm = realm;
        /* step 3 */
        return currentRealm.getGlobalThis();
    }
}
