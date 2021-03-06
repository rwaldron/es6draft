/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetPrototypeFromConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.HasOwnProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.*;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator.FunctionAllocate;

import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.ast.GeneratorDefinition;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.FunctionKind;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;

/**
 * <h1>25 Control Abstraction Objects</h1><br>
 * <h2>25.2 GeneratorFunction Objects</h2>
 * <ul>
 * <li>25.2.1 The GeneratorFunction Constructor
 * <li>25.2.2 Properties of the GeneratorFunction Constructor
 * </ul>
 */
public final class GeneratorFunctionConstructor extends BuiltinConstructor implements Initialisable {
    public GeneratorFunctionConstructor(Realm realm) {
        super(realm, "GeneratorFunction");
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 25.2.1.1 GeneratorFunction (p1, p2, ... , pn, body)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();

        /* steps 1-7 */
        int argCount = args.length;
        StringBuilder p = new StringBuilder();
        CharSequence bodyText;
        if (argCount == 0) {
            bodyText = "";
        } else if (argCount == 1) {
            bodyText = ToString(calleeContext, args[0]);
        } else {
            Object firstArg = args[0];
            p.append(ToString(calleeContext, firstArg));
            int k = 2;
            for (; k < argCount; ++k) {
                Object nextArg = args[k - 1];
                CharSequence nextArgString = ToString(calleeContext, nextArg);
                p.append(',').append(nextArgString);
            }
            bodyText = ToString(calleeContext, args[k - 1]);
        }

        /* steps 8-12 */
        RuntimeInfo.Function function;
        try {
            Realm realm = calleeContext.getRealm();
            Parser parser = new Parser("<GeneratorFunction>", 1, realm.getOptions());
            GeneratorDefinition generatorDef = parser.parseGenerator(p, bodyText);
            String className = calleeContext.getRealm().nextFunctionName();
            function = ScriptLoader.compile(className, generatorDef, realm.getCompilerOptions());
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(calleeContext);
        }

        /* step 12 */
        boolean strict = function.isStrict();
        /* step 13 */
        LexicalEnvironment scope = calleeContext.getRealm().getGlobalEnv();
        /* step 14 */
        Object f = thisValue;
        /* steps 15-16 */
        if (!Type.isObject(f) || !(f instanceof FunctionObject)
                || ((FunctionObject) f).getCode() != null) {
            ScriptObject proto = GetPrototypeFromConstructor(calleeContext, this,
                    Intrinsics.Generator);
            f = FunctionAllocate(calleeContext, proto, strict, FunctionKind.Normal);
        } else {
            // FIXME: this also updates uninitialised function (not generator!)
            ((FunctionObject) f).setStrict(strict);
        }
        /* step 17 */
        if (!(f instanceof OrdinaryGenerator)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        OrdinaryGenerator fn = (OrdinaryGenerator) f;
        /* steps 18-19 */
        FunctionInitialise(calleeContext, fn, FunctionKind.Normal, function, scope);
        /* step 20 */
        ScriptObject prototype = ObjectCreate(calleeContext, Intrinsics.GeneratorPrototype);
        /* step 21 */
        if (function.hasSuperReference()) {
            MakeMethod(fn, (String) null, null);
        }
        /* steps 22-23 */
        MakeConstructor(calleeContext, fn, true, prototype);
        /* step 24 */
        // FIXME: if-test missing in spec https://bugs.ecmascript.org/show_bug.cgi?id=2335
        if (!HasOwnProperty(calleeContext, fn, "name")) {
            SetFunctionName(fn, "anonymous");
        }
        /* step 25 */
        return fn;
    }

    /**
     * 25.2.1.2 new GeneratorFunction (...argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    /**
     * 25.2.2 Properties of the GeneratorFunction Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Function;

        /**
         * 25.2.2.2 GeneratorFunction.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Generator;

        /**
         * 25.2.2.1 GeneratorFunction.length
         */
        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "GeneratorFunction";

        /**
         * 25.2.2.3 GeneratorFunction[ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", arity = 0, symbol = BuiltinSymbol.create,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            ScriptObject proto = GetPrototypeFromConstructor(cx, thisValue, Intrinsics.Generator);
            /* step 4 */
            OrdinaryGenerator obj = FunctionAllocate(cx, proto, false, FunctionKind.Normal);
            /* step 5 */
            return obj;
        }
    }
}
