/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertTrue
} = Assert;

// 22.1.3.7 Array.prototype.filter

function assertSameArray(array1, array2) {
  assertSame(array1.length, array2.length);
  for (let i = 0, len = array1.length; i < len; ++i) {
    assertSame(array1[i], array2[i]);
  }
}

// filter() with same realm constructor
{
  class MyArray extends Array { }
  const obj1 = {}, obj2 = {};
  let array1 = new MyArray(obj1, obj2);
  let array2 = array1.filter(() => true);

  // array1.constructor is from the same realm, filter() creates Array sub-class instances
  assertTrue(Array.isArray(array2));
  assertSame(MyArray, array2.constructor);
  assertSame(MyArray.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// filter() with different realm constructor (1)
{
  const ForeignMyArray = new Realm().eval(`
    class MyArray extends Array { }
    MyArray;
  `);
  const obj1 = {}, obj2 = {};
  let array1 = new ForeignMyArray(obj1, obj2);
  let array2 = Array.prototype.filter.call(array1, () => true);

  // array1.constructor is from a different realm, filter() creates default Array instances
  assertTrue(Array.isArray(array2));
  assertSame(Array, array2.constructor);
  assertSame(Array.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// filter() with different realm constructor (2)
{
  class MyArray extends Array { }
  const ForeignArray = new Realm().eval("Array");
  const obj1 = {}, obj2 = {};
  let array1 = new MyArray(obj1, obj2);
  let array2 = ForeignArray.prototype.filter.call(array1, () => true);

  // array1.constructor is from a different realm, filter() creates default Array instances
  assertTrue(Array.isArray(array2));
  assertSame(ForeignArray, array2.constructor);
  assertSame(ForeignArray.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// filter() with proxied constructor
{
  class MyArray extends Array { }
  const obj1 = {}, obj2 = {};
  let array1 = new MyArray(obj1, obj2);
  array1.constructor = new Proxy(array1.constructor, {});
  let array2 = array1.filter(() => true);

  // Proxy (function) objects do not have a [[Realm]] internal slot, filter() creates default Array instances
  assertTrue(Array.isArray(array2));
  assertSame(Array, array2.constructor);
  assertSame(Array.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// filter() with bound constructor
{
  class MyArray extends Array { }
  const obj1 = {}, obj2 = {};
  let array1 = new MyArray(obj1, obj2);
  array1.constructor = array1.constructor.bind(null);
  let array2 = array1.filter(() => true);

  // Bound function objects do not have a [[Realm]] internal slot, filter() creates default Array instances
  assertTrue(Array.isArray(array2));
  assertSame(Array, array2.constructor);
  assertSame(Array.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}
