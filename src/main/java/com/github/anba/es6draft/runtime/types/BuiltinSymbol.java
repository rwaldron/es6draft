/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import java.util.EnumMap;

/**
 * <h1>6 ECMAScript Data Types and Values</h1><br>
 * <h2>6.1.5 The Symbol Type</h2>
 * <ul>
 * <li>6.1.5.1 Well-Known Symbols
 * </ul>
 */
public enum BuiltinSymbol {
    NONE, //

    /**
     * &#64;&#64;create
     */
    create,

    /**
     * &#64;&#64;hasInstance
     */
    hasInstance,

    /**
     * &#64;&#64;isConcatSpreadable
     */
    isConcatSpreadable,

    /**
     * &#64;&#64;isRegExp
     */
    isRegExp,

    /**
     * &#64;&#64;iterator
     */
    iterator,

    /**
     * &#64;&#64;toPrimitive
     */
    toPrimitive,

    /**
     * &#64;&#64;toStringTag
     */
    toStringTag,

    /**
     * &#64;&#64;unscopables
     */
    unscopables,

    ;

    /**
     * Returns a {@link Symbol} object for this {@link BuiltinSymbol}
     */
    public final Symbol get() {
        assert this != NONE;
        return symbols.get(this);
    }

    private static final EnumMap<BuiltinSymbol, Symbol> symbols;
    static {
        EnumMap<BuiltinSymbol, Symbol> map = new EnumMap<>(BuiltinSymbol.class);
        for (BuiltinSymbol builtin : values()) {
            if (builtin != NONE) {
                String name = "Symbol." + builtin.name();
                map.put(builtin, new Symbol(name));
            }
        }
        symbols = map;
    }
}
