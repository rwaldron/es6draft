/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// [8.3.19.2.1]: Add assertion to test value for @@create is callable
// https://bugs.ecmascript.org/show_bug.cgi?id=1206

let f1 = Object.defineProperty(function(){}, Symbol.create, {
  value: null
});
assertThrows(() => new f1, TypeError);

let f2 = Object.defineProperty(function(){}, Symbol.create, {
  value() {
    return null;
  }
});
assertThrows(() => new f2, TypeError);
