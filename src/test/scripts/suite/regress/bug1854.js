/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 8.4.2.2: Add ToUint32() to ensure array invariant
// https://bugs.ecmascript.org/show_bug.cgi?id=1854

assertThrows(() => Array.prototype.map.call({length: 1 + Math.pow(2, 32)}, x => x), RangeError);
