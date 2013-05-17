/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateArrayFromList;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateOwnDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.JVMNames;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.11 Error Objects</h2>
 * <ul>
 * <li>15.11.4 Properties of the Error Prototype Object
 * </ul>
 */
public class ErrorPrototype extends OrdinaryObject implements Initialisable {
    public ErrorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 15.11.4 Properties of the Error Prototype Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.11.4.1 Error.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Error;

        /**
         * 15.11.4.2 Error.prototype.name
         */
        @Value(name = "name")
        public static final String name = "Error";

        /**
         * 15.11.4.3 Error.prototype.message
         */
        @Value(name = "message")
        public static final String message = "";

        /**
         * 15.11.4.4 Error.prototype.toString ( )
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            if (!Type.isObject(thisValue)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject o = Type.objectValue(thisValue);
            Object name = Get(cx, o, "name");
            CharSequence sname = (Type.isUndefined(name) ? "Error" : ToString(cx, name));
            Object msg = Get(cx, o, "message");
            CharSequence smsg = (Type.isUndefined(msg) ? "" : ToString(cx, msg));
            if (sname.length() == 0) {
                return smsg;
            }
            if (smsg.length() == 0) {
                return sname;
            }
            return sname + ": " + smsg;
        }

        /**
         * Extension: Error.prototype.fileName
         */
        @Accessor(name = "fileName", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object get_fileName(ExecutionContext cx, Object thisValue) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            ScriptException e = ((ErrorObject) thisValue).getException();
            return getTopStackTraceElement(e).getFileName();
        }

        /**
         * Extension: Error.prototype.fileName
         */
        @Accessor(name = "fileName", type = Accessor.Type.Setter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object set_fileName(ExecutionContext cx, Object thisValue, Object value) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            CreateOwnDataProperty(cx, (ErrorObject) thisValue, "fileName", value);
            return UNDEFINED;
        }

        /**
         * Extension: Error.prototype.lineNumber
         */
        @Accessor(name = "lineNumber", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object get_lineNumber(ExecutionContext cx, Object thisValue) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            ScriptException e = ((ErrorObject) thisValue).getException();
            return getTopStackTraceElement(e).getLineNumber();
        }

        /**
         * Extension: Error.prototype.lineNumber
         */
        @Accessor(name = "lineNumber", type = Accessor.Type.Setter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object set_lineNumber(ExecutionContext cx, Object thisValue, Object value) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            CreateOwnDataProperty(cx, (ErrorObject) thisValue, "lineNumber", value);
            return UNDEFINED;
        }

        /**
         * Extension: Error.prototype.stack
         */
        @Accessor(name = "stack", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object get_stack(ExecutionContext cx, Object thisValue) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            ScriptException e = ((ErrorObject) thisValue).getException();
            return getStack(e);
        }

        /**
         * Extension: Error.prototype.stack
         */
        @Accessor(name = "stack", type = Accessor.Type.Setter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object set_stack(ExecutionContext cx, Object thisValue, Object value) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            CreateOwnDataProperty(cx, (ErrorObject) thisValue, "stack", value);
            return UNDEFINED;
        }

        /**
         * Extension: Error.prototype.stacktrace
         */
        @Accessor(name = "stacktrace", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = false))
        public static Object get_stacktrace(ExecutionContext cx, Object thisValue) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            ScriptException e = ((ErrorObject) thisValue).getException();
            return getStackTrace(cx, e);
        }
    }

    private static StackTraceElement getTopStackTraceElement(ScriptException e) {
        for (StackTraceElement element : new StackTraceElementIterable(e)) {
            return element;
        }
        return new StackTraceElement("", "", "", -1);
    }

    private static String getStack(ScriptException e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : new StackTraceElementIterable(e)) {
            sb.append(getMethodName(element)).append('@').append(element.getFileName()).append(':')
                    .append(element.getLineNumber()).append('\n');
        }
        return sb.toString();
    }

    private static ScriptObject getStackTrace(ExecutionContext cx, ScriptException e) {
        List<ScriptObject> list = new ArrayList<>();
        for (StackTraceElement element : new StackTraceElementIterable(e)) {
            OrdinaryObject elem = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            CreateOwnDataProperty(cx, elem, "methodName", getMethodName(element));
            CreateOwnDataProperty(cx, elem, "fileName", element.getFileName());
            CreateOwnDataProperty(cx, elem, "lineNumber", element.getLineNumber());
            list.add(elem);
        }
        return CreateArrayFromList(cx, list);
    }

    private static String getMethodName(StackTraceElement element) {
        String methodName = JVMNames.fromBytecodeName(element.getMethodName());
        assert methodName.charAt(0) == '!';
        int i = methodName.lastIndexOf('~');
        return methodName.substring(1, (i != -1 ? i : methodName.length()));
    }

    private static final class StackTraceElementIterable implements Iterable<StackTraceElement> {
        private Throwable throwable;

        StackTraceElementIterable(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public Iterator<StackTraceElement> iterator() {
            return new StackTraceElementIterator(throwable);
        }
    }

    private static final class StackTraceElementIterator implements Iterator<StackTraceElement> {
        private ThreadGroup threadGroup;
        private StackTraceElement[] elements;
        private int cursor = 0;
        private StackTraceElement next;

        StackTraceElementIterator(Throwable throwable) {
            this.threadGroup = Thread.currentThread().getThreadGroup();
            this.elements = throwable.getStackTrace();
        }

        private static boolean isInternalStackFrame(StackTraceElement element) {
            // filter stacktrace elements based on the encoding in CodeGenerator/ScriptLoader
            return (element.getClassName().charAt(0) == '#' && JVMNames.fromBytecodeName(
                    element.getMethodName()).charAt(0) == '!');
        }

        private static boolean isGeneratorThreadGroup(ThreadGroup threadGroup) {
            // filter thread-groups based on name per GeneratorObject
            return "generator-group".equals(threadGroup.getName());
        }

        private StackTraceElement tryNext() {
            while (elements != null) {
                while (cursor < elements.length) {
                    StackTraceElement element = elements[cursor++];
                    if (isInternalStackFrame(element)) {
                        return element;
                    }
                }
                if (isGeneratorThreadGroup(threadGroup)) {
                    ThreadGroup parent = threadGroup.getParent();
                    Thread[] threads = new Thread[1];
                    if (parent.enumerate(threads) == 1) {
                        threadGroup = parent;
                        cursor = 0;
                        elements = threads[0].getStackTrace();
                    } else {
                        elements = null;
                    }
                } else {
                    elements = null;
                }
            }
            return null;
        }

        @Override
        public boolean hasNext() {
            if (next == null) {
                next = tryNext();
            }
            return next != null;
        }

        @Override
        public StackTraceElement next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            StackTraceElement next = this.next;
            this.next = null;
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
