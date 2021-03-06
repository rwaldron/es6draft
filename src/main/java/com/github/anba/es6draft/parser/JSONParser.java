/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import com.github.anba.es6draft.parser.ParserException.ExceptionType;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>24 Structured Data</h1><br>
 * <h2>24.3 The JSON Object</h2><br>
 * <h3>24.3.1 The JSON Grammar</h3>
 * <ul>
 * <li>24.3.1.2 The JSON Syntactic Grammar
 * </ul>
 */
public final class JSONParser {
    private static final boolean DEBUG = false;

    private boolean parseCalled = false;
    private JSONTokenStream ts;
    private ExecutionContext cx;

    public JSONParser(ExecutionContext cx, CharSequence source) {
        this.cx = cx;
        ts = new JSONTokenStream(new TokenStreamInput(source));
    }

    private void reportParseError(Messages.Key messageKey, String... args) {
        throw new ParserException(ExceptionType.SyntaxError, "<json>", 1, 1, messageKey, args);
    }

    /**
     * Returns the current token in the token-stream
     */
    private Token token() {
        return ts.currentToken();
    }

    /**
     * Consumes the current token in the token-stream and advances the stream to the next token
     */
    private void consume(Token tok) {
        if (tok != token())
            reportParseError(Messages.Key.UnexpectedToken, token().toString(), tok.toString());
        Token next = ts.nextToken();
        if (DEBUG)
            System.out.printf("consume(%s) -> %s\n", tok, next);
    }

    public Object parse() throws ParserException {
        if (parseCalled)
            throw new IllegalStateException();
        parseCalled = true;
        return jsonText();
    }

    /* ***************************************************************************************** */

    /**
     * <pre>
     * JSONText :   
     *      JSONValue
     * </pre>
     */
    private Object jsonText() {
        Object value = jsonValue();
        consume(Token.EOF);
        return value;
    }

    /**
     * <pre>
     * JSONValue : 
     *      JSONNullLiteral
     *      JSONBooleanLiteral
     *      JSONObject
     *      JSONArray
     *      JSONString
     *      JSONNumber
     * </pre>
     */
    private Object jsonValue() {
        Token tok = token();
        switch (tok) {
        case NULL:
            consume(tok);
            return NULL;
        case FALSE:
            consume(tok);
            return Boolean.FALSE;
        case TRUE:
            consume(tok);
            return Boolean.TRUE;
        case STRING:
            consume(tok);
            return ts.getString();
        case NUMBER:
            consume(tok);
            return ts.getNumber();
        case LC:
            return jsonObject();
        case LB:
            return jsonArray();
        default:
            reportParseError(Messages.Key.InvalidToken, tok.toString());
            return null;
        }
    }

    /**
     * <pre>
     * JSONObject :
     *      { }
     *      { JSONMemberList }
     * JSONMemberList :
     *      JSONMember 
     *      JSONMemberList , JSONMember
     * </pre>
     */
    private ScriptObject jsonObject() {
        ScriptObject object = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        consume(Token.LC);
        if (token() != Token.RC) {
            jsonMember(object);
            while (token() != Token.RC) {
                consume(Token.COMMA);
                jsonMember(object);
            }
        }
        consume(Token.RC);
        return object;
    }

    /**
     * <pre>
     * JSONMember :
     *      JSONString : JSONValue
     * </pre>
     */
    private void jsonMember(ScriptObject object) {
        consume(Token.STRING);
        String name = ts.getString();
        consume(Token.COLON);
        Object value = jsonValue();
        object.defineOwnProperty(cx, name, new PropertyDescriptor(value, true, true, true));
    }

    /**
     * <pre>
     * JSONArray :
     *      [ ]
     *      [ JSONElementList ]
     * JSONElementList :
     *      JSONValue
     *      JSONElementList , JSONValue
     * </pre>
     */
    private Object jsonArray() {
        ScriptObject array = ArrayCreate(cx, 0);
        consume(Token.LB);
        if (token() != Token.RB) {
            long index = 0;
            Object value = jsonValue();
            array.defineOwnProperty(cx, ToString(index++), new PropertyDescriptor(value, true,
                    true, true));
            while (token() != Token.RB) {
                consume(Token.COMMA);
                value = jsonValue();
                array.defineOwnProperty(cx, ToString(index++), new PropertyDescriptor(value, true,
                        true, true));
            }
        }
        consume(Token.RB);
        return array;
    }
}
