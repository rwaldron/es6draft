/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1>
 * <ul>
 * <li>14.1 Function Definitions
 * </ul>
 */
public final class FunctionDeclaration extends Declaration implements FunctionDefinition {
    private FunctionScope scope;
    private BindingIdentifier identifier;
    private FormalParameterList parameters;
    private List<StatementListItem> statements;
    private StrictMode strictMode;
    private boolean superReference;
    private String headerSource, bodySource;
    private boolean syntheticNodes;

    public FunctionDeclaration(long beginPosition, long endPosition, FunctionScope scope,
            BindingIdentifier identifier, FormalParameterList parameters,
            List<StatementListItem> statements, boolean superReference, String headerSource,
            String bodySource) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.identifier = identifier;
        this.parameters = parameters;
        this.statements = statements;
        this.superReference = superReference;
        this.headerSource = headerSource;
        this.bodySource = bodySource;
    }

    @Override
    public FunctionScope getScope() {
        return scope;
    }

    @Override
    public BindingIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public String getFunctionName() {
        return identifier.getName();
    }

    @Override
    public FormalParameterList getParameters() {
        return parameters;
    }

    @Override
    public List<StatementListItem> getStatements() {
        return statements;
    }

    @Override
    public void setStatements(List<StatementListItem> statements) {
        this.statements = statements;
    }

    @Override
    public StrictMode getStrictMode() {
        return strictMode;
    }

    @Override
    public void setStrictMode(StrictMode strictMode) {
        this.strictMode = strictMode;
    }

    @Override
    public String getHeaderSource() {
        return headerSource;
    }

    @Override
    public String getBodySource() {
        return bodySource;
    }

    @Override
    public boolean isGenerator() {
        return false;
    }

    @Override
    public boolean hasSuperReference() {
        return superReference;
    }

    @Override
    public boolean isConstDeclaration() {
        return false;
    }

    @Override
    public boolean hasSyntheticNodes() {
        return syntheticNodes;
    }

    @Override
    public void setSyntheticNodes(boolean syntheticNodes) {
        this.syntheticNodes = syntheticNodes;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
