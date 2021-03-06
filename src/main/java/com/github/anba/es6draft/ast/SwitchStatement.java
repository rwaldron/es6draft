/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.11 The switch Statement
 * </ul>
 */
public final class SwitchStatement extends BreakableStatement implements ScopedNode {
    private BlockScope scope;
    private EnumSet<Abrupt> abrupt;
    private Set<String> labelSet;
    private Expression expression;
    private List<SwitchClause> clauses;

    public SwitchStatement(long beginPosition, long endPosition, BlockScope scope,
            EnumSet<Abrupt> abrupt, Set<String> labelSet, Expression expression,
            List<SwitchClause> clauses) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.abrupt = abrupt;
        this.labelSet = labelSet;
        this.expression = expression;
        this.clauses = clauses;
    }

    @Override
    public BlockScope getScope() {
        return scope;
    }

    @Override
    public EnumSet<Abrupt> getAbrupt() {
        return abrupt;
    }

    @Override
    public Set<String> getLabelSet() {
        return labelSet;
    }

    public Expression getExpression() {
        return expression;
    }

    public List<SwitchClause> getClauses() {
        return clauses;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
