/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import java.util.Locale;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.InternalException;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;

/**
 * {@link RuntimeException} subclass for parser exceptions
 */
@SuppressWarnings("serial")
public class ParserException extends InternalException {
    public enum ExceptionType {
        SyntaxError, ReferenceError
    }

    private final ExceptionType type;
    private final String file;
    private final int line, column;
    private final Messages.Key messageKey;
    private final String[] messageArguments;

    public ParserException(ExceptionType type, String file, int line, int column,
            Messages.Key messageKey, String... args) {
        super(messageKey.name());
        this.type = type;
        this.file = file;
        this.line = line;
        this.column = column;
        this.messageKey = messageKey;
        this.messageArguments = args;
    }

    @Override
    public String getMessage() {
        String message = type.toString() + ": " + getFormattedMessage();
        if (line != -1 && column != -1) {
            message += " (line " + line + ", column " + column + ")";
        } else if (line != -1) {
            message += " (line " + line + ")";
        }
        return message;
    }

    public ExceptionType getType() {
        return type;
    }

    public String getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getFormattedMessage() {
        return getFormattedMessage(Locale.getDefault());
    }

    public String getFormattedMessage(Locale locale) {
        return Messages.create(locale).getMessage(messageKey, messageArguments);
    }

    @Override
    public ScriptException toScriptException(ExecutionContext cx) {
        String message = cx.getRealm().message(messageKey, messageArguments);
        if (type == ExceptionType.ReferenceError) {
            return Errors.newReferenceError(cx, message, getFile(), getLine(), getColumn());
        }
        return Errors.newSyntaxError(cx, message, getFile(), getLine(), getColumn());
    }
}
