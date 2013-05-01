/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticBoundFunction.BoundFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.ExoticBoundFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.3 Function Objects</h2>
 * <ul>
 * <li>15.3.4 Properties of the Function Prototype Object
 * <li>15.3.5 Properties of Function Instances
 * </ul>
 */
public class FunctionPrototype extends BuiltinFunction implements Initialisable {
    private static final int MAX_ARGUMENTS = 0x10000;

    public FunctionPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * Returns the number of maximal supported arguments in {@code Function.prototype.apply}
     */
    public static final int getMaxArguments() {
        return MAX_ARGUMENTS;
    }

    /**
     * [[Call]]
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        return UNDEFINED;
    }

    /**
     * 15.3.4 Properties of the Function Prototype Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "FunctionPrototype";

        /**
         * 15.3.4.1 Function.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Function;

        /**
         * 15.3.4.2 Function.prototype.toString ( )
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            if (!IsCallable(thisValue)) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            return ((Callable) thisValue).toSource();
        }

        /**
         * 15.3.4.3 Function.prototype.apply (thisArg, argArray)
         */
        @Function(name = "apply", arity = 2)
        public static Object apply(ExecutionContext cx, Object thisValue, Object thisArg,
                Object argArray) {
            if (!IsCallable(thisValue)) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            Callable func = (Callable) thisValue;
            if (Type.isUndefinedOrNull(argArray)) {
                return func.call(cx, thisArg);
            }
            if (!Type.isObject(argArray)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject argarray = Type.objectValue(argArray);
            Object len = Get(cx, argarray, "length");
            long n = ToUint32(cx, len);
            if (n > MAX_ARGUMENTS) {
                throw throwRangeError(cx, Messages.Key.FunctionTooManyArguments);
            }
            Object[] argList = new Object[(int) n];
            for (int index = 0; index < n; ++index) {
                String indexName = ToString(index);
                Object nextArg = Get(cx, argarray, indexName);
                argList[index] = nextArg;
            }
            return func.call(cx, thisArg, argList);
        }

        /**
         * 15.3.4.4 Function.prototype.call (thisArg [ , arg1 [ , arg2, ... ] ] )
         */
        @Function(name = "call", arity = 1)
        public static Object call(ExecutionContext cx, Object thisValue, Object thisArg,
                Object... args) {
            if (!IsCallable(thisValue)) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            Callable func = (Callable) thisValue;
            return func.call(cx, thisArg, args);
        }

        /**
         * 15.3.4.5 Function.prototype.bind (thisArg [, arg1 [, arg2, ...]])
         */
        @Function(name = "bind", arity = 1)
        public static Object bind(ExecutionContext cx, Object thisValue, Object thisArg,
                Object... args) {
            if (!IsCallable(thisValue)) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            Callable target = (Callable) thisValue;
            ExoticBoundFunction f = BoundFunctionCreate(cx, target, thisArg, args);
            int l;
            if (target instanceof OrdinaryFunction || target instanceof OrdinaryGenerator
                    || target instanceof BuiltinFunction || target instanceof ExoticBoundFunction) {
                Object targetLen = Get(cx, target, "length");
                l = (int) Math.max(0, ToInteger(cx, targetLen) - args.length);
            } else {
                l = 0;
            }
            f.defineOwnProperty(cx, "length", new PropertyDescriptor(l, false, false, false));
            AddRestrictedFunctionProperties(cx, f);
            return f;
        }

        /**
         * 15.3.4.6 Function.prototype[ @@create ] ( )
         */
        @Function(name = "@@create", arity = 0, symbol = BuiltinSymbol.create)
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.ObjectPrototype);
        }

        /**
         * 15.3.4.7 Function.prototype[@@hasInstance] (V)
         */
        @Function(name = "@@hasInstance", arity = 1, symbol = BuiltinSymbol.hasInstance)
        public static Object hasInstance(ExecutionContext cx, Object thisValue, Object v) {
            // FIXME: spec bug? make writable=configurable=(enumerable)=false to prevent exposing
            // bound functions
            return OrdinaryHasInstance(cx, thisValue, v);
        }
    }
}
