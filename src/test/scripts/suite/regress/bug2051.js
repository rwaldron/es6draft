/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertThrows
} = Assert;

// 24.1.4.3 ArrayBuffer.prototype.slice: Ensure newly created ArrayBuffer is big enough
// https://bugs.ecmascript.org/show_bug.cgi?id=2051

let buffer = new ArrayBuffer(10);
buffer.constructor = function(len) {
  return new ArrayBuffer(5);
};
assertThrows(() => buffer.slice(), TypeError);

buffer.constructor = function(len) {
  return new ArrayBuffer(15);
};
assertSame(15, buffer.slice().byteLength);
