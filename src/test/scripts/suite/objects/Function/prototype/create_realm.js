/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 19.2.4.6 Function.prototype[ @@create ] ( )

{
  const foreignRealm = new Realm();
  const create = Function.prototype[Symbol.create];

  // foreign constructor function whose .prototype is not an object
  const foreignConstructor = foreignRealm.eval("function F(){}; F");
  foreignConstructor.prototype = null;

  // GetPrototypeFromConstructor() retrieves the foreign realm's intrinsic %ObjectPrototype%
  let obj1 = create.call(foreignConstructor);
  assertSame(foreignRealm.global.Object.prototype, Object.getPrototypeOf(obj1));


  // foreign bound constructor function whose .prototype is not an object
  const foreignBoundConstructor = foreignRealm.eval("function F(){}; F.bind(null)");
  foreignBoundConstructor.prototype = null;

  // GetPrototypeFromConstructor() uses the current realm's intrinsic %ObjectPrototype%, because
  // bound functions have no [[Realm]] internal slot
  let obj2 = create.call(foreignBoundConstructor);
  assertSame(Object.prototype, Object.getPrototypeOf(obj2));


  // foreign proxy constructor function whose .prototype is not an object
  const foreignProxyConstructor = foreignRealm.eval("function F(){}; new Proxy(F, {})");
  foreignProxyConstructor.prototype = null;

  // GetPrototypeFromConstructor() uses the current realm's intrinsic %ObjectPrototype%, because
  // proxy functions have no [[Realm]] internal slot
  let obj3 = create.call(foreignProxyConstructor);
  assertSame(Object.prototype, Object.getPrototypeOf(obj3));
}
