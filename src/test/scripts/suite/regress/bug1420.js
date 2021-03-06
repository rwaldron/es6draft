/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 10.2.1.1.5: SetMutableBinding allows changing the value of uninitialised bindings
// https://bugs.ecmascript.org/show_bug.cgi?id=1420

function f() {
  foo = 3;
  return;
  let foo;
}
assertThrows(f, ReferenceError);

function g() {
  "use strict";
  foo = 3;
  return;
  let foo;
}
assertThrows(g, ReferenceError);
