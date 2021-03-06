/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.Task;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.objects.ErrorObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 *
 */
public final class SimpleShellGlobalObject extends ShellGlobalObject {
    public SimpleShellGlobalObject(Realm realm, ShellConsole console, Path baseDir, Path script,
            ScriptCache scriptCache) {
        super(realm, console, baseDir, script, scriptCache);
    }

    @Override
    public void defineBuiltinProperties(ExecutionContext cx, ScriptObject object) {
        super.defineBuiltinProperties(cx, object);
        createProperties(object, this, cx, SimpleShellGlobalObject.class);
    }

    /**
     * Returns an object to allocate new instances of this class
     */
    public static ObjectAllocator<SimpleShellGlobalObject> newGlobalObjectAllocator(
            final ShellConsole console, final Path baseDir, final Path script,
            final ScriptCache scriptCache) {
        return new ObjectAllocator<SimpleShellGlobalObject>() {
            @Override
            public SimpleShellGlobalObject newInstance(Realm realm) {
                return new SimpleShellGlobalObject(realm, console, baseDir, script, scriptCache);
            }
        };
    }

    /** shell-function: {@code parseModule(source)} */
    @Function(name = "parseModule", arity = 1)
    public String parseModule(ExecutionContext cx, String source) {
        Parser parser = new Parser("<module>", 1, cx.getRealm().getOptions());
        try {
            parser.parseModule(source);
        } catch (ParserException e) {
            throw e.toScriptException(cx);
        }
        return "success";
    }

    /** shell-function: {@code compile(filename)} */
    @Function(name = "compile", arity = 1)
    public String compile(String filename) {
        try {
            scriptCache.script(filename, 1, absolutePath(Paths.get(filename)));
        } catch (ParserException | CompilationException | IOException e) {
            return "error: " + e.getMessage();
        }
        return "success";
    }

    /** shell-function: {@code loadRelativeToScript(filename)} */
    @Function(name = "loadRelativeToScript", arity = 1)
    public Object loadRelativeToScript(ExecutionContext cx, String filename) {
        return load(cx, Paths.get(filename), relativePath(Paths.get(filename)));
    }

    /** shell-function: {@code dump(object)} */
    @Function(name = "dump", arity = 1)
    public void dump(ScriptObject object) {
        String id = String.format("%s@%d", object.getClass().getSimpleName(),
                System.identityHashCode(object));
        console.print(id);
    }

    /** shell-function: {@code error()} */
    @Function(name = "error", arity = 0)
    public void error() {
        throw new AssertionError();
    }

    /** shell-function: {@code printStackTrace(object)} */
    @Function(name = "printStackTrace", arity = 1)
    public void printStackTrace(ScriptObject object) {
        if (object instanceof ErrorObject) {
            ((ErrorObject) object).getException().printStackTrace();
        }
    }

    /** shell-function: {@code nextTick(function)} */
    @Function(name = "nextTick", arity = 1)
    public void nextTick(final ExecutionContext cx, final Callable function) {
        cx.getRealm().getWorld().enqueuePromiseTask(new Task() {
            @Override
            public void execute() {
                function.call(cx, UNDEFINED);
            }
        });
    }

    /** shell-function: {@code version()} */
    @Function(name = "version", arity = 1)
    public String version() {
        return String.format("%s", getResourceInfo("/version", "<unknown version>"));
    }
}
