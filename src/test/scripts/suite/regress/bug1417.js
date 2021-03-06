/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 13.3: Property Definition Evaluation for getters not updated to use DefinePropertyOrThrow
// https://bugs.ecmascript.org/show_bug.cgi?id=1417

assertThrows(() => (class { static get ["caller"](){} }), TypeError);
