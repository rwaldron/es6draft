/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1>
 */
public abstract class Expression extends AstNode {
    private int parentheses = 0;

    protected Expression(long beginPosition, long endPosition) {
        super(beginPosition, endPosition);
    }

    /**
     * Returns {@code true} if this expression is enclosed in parentheses
     */
    public boolean isParenthesised() {
        return parentheses != 0;
    }

    /**
     * Adds one more layer of parentheses around this expression
     */
    public void addParentheses() {
        parentheses += 1;
    }

    /**
     * Returns the number of parentheses enclosing this expression
     */
    public int getParentheses() {
        return parentheses;
    }

    /**
     * Returns a {@link Expression} instance representing this node as a value-expression, i.e. an
     * expression which will never yield Reference values. May throw an
     * {@link IllegalStateException} if the operation is not valid for the requested node.
     */
    public Expression asValue() {
        return this;
    }
}
