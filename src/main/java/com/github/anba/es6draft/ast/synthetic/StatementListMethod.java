/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.synthetic;

import java.util.List;

import com.github.anba.es6draft.ast.NodeVisitor;
import com.github.anba.es6draft.ast.Statement;
import com.github.anba.es6draft.ast.StatementListItem;

/**
 * List of {@link StatementListItem}s as an external Java method
 */
public final class StatementListMethod extends Statement {
    private List<StatementListItem> statements;

    public StatementListMethod(List<StatementListItem> statements) {
        super(first(statements).getBeginPosition(), last(statements).getEndPosition());
        this.statements = statements;
    }

    public List<StatementListItem> getStatements() {
        return statements;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }

    private static StatementListItem first(List<StatementListItem> elements) {
        assert !elements.isEmpty();
        return elements.get(0);
    }

    private static StatementListItem last(List<StatementListItem> elements) {
        assert !elements.isEmpty();
        return elements.get(elements.size() - 1);
    }
}
