/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse, assertUndefined, assertDataProperty
} = Assert;

// 6.1.7.3, 9.4.3: String exotic objects can violate [[GetOwnProperty]] invariant
// https://bugs.ecmascript.org/show_bug.cgi?id=2488

// Create uninitialised string object and define its "length" property
let str = String[Symbol.create]();
Reflect.defineProperty(str, "length", {value: 1, writable: false, enumerable: false, configurable: false});

// Make string object non-extensible, observe "0" property
Reflect.preventExtensions(str);
assertFalse(Reflect.isExtensible(str));
assertUndefined(Reflect.getOwnPropertyDescriptor(str, "0"));

// Initialise string object and retrieve "0" property
String.call(str, "A");
assertDataProperty(str, "0", {value: "A", writable: false, enumerable: true, configurable: false});
