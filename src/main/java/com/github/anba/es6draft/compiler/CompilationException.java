/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.InternalException;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;

/**
 * Exception for compilation errors
 */
@SuppressWarnings("serial")
public final class CompilationException extends InternalException {
    public CompilationException(String message) {
        super(message);
    }

    @Override
    public ScriptException toScriptException(ExecutionContext cx) {
        return Errors.newInternalError(cx, Messages.Key.InternalError, getMessage());
    }
}
