/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.1 Primary Expressions</h2><br>
 * <h3>12.1.4 Array Initialiser</h3>
 * <ul>
 * <li>12.1.4.2 Array Comprehension
 * </ul>
 */
public final class LegacyComprehensionFor extends ComprehensionQualifier {
    private IterationKind iterationKind;
    private Binding binding;
    private Expression expression;

    public enum IterationKind {
        Enumerate, Iterate, EnumerateValues
    }

    public LegacyComprehensionFor(long beginPosition, long endPosition,
            IterationKind iterationKind, Binding binding, Expression expression) {
        super(beginPosition, endPosition);
        this.iterationKind = iterationKind;
        this.binding = binding;
        this.expression = expression;
    }

    public IterationKind getIterationKind() {
        return iterationKind;
    }

    public Binding getBinding() {
        return binding;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
