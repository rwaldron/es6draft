/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertEquals
} = Assert;

loadRelativeToScript("../../lib/recorder.js");

// Object with @@unscopables but no match, property is not present, single access
{
  let fallbackCalled = 0;
  let history = [];
  let blackList = Recorder.watch({otherProperty: true}, history);
  let object = Recorder.watch({[Symbol.unscopables]: blackList}, history);
  with ({get property() { fallbackCalled += 1 }}) {
    with (object) {
      property;
    }
  }
  assertEquals([
    {name: "get", target: object, property: Symbol.unscopables, result: blackList, receiver: object},
    {name: "has", target: blackList, property: "property", result: false},
    {name: "has", target: object, property: "property", result: false},
  ], history);
  assertSame(1, fallbackCalled);
}

// Object with @@unscopables but no match, property is not present, multi access
{
  let fallbackCalled = 0;
  let history = [];
  let blackList = Recorder.watch({otherProperty: true}, history);
  let object = Recorder.watch({[Symbol.unscopables]: blackList}, history);
  with ({get property() { fallbackCalled += 1 }}) {
    with (object) {
      property;
      property;
    }
  }
  assertEquals([
    {name: "get", target: object, property: Symbol.unscopables, result: blackList, receiver: object},
    {name: "has", target: blackList, property: "property", result: false},
    {name: "has", target: object, property: "property", result: false},
    {name: "get", target: object, property: Symbol.unscopables, result: blackList, receiver: object},
    {name: "has", target: blackList, property: "property", result: false},
    {name: "has", target: object, property: "property", result: false},
  ], history);
  assertSame(2, fallbackCalled);
}

// Object with @@unscopables but no match, property is present, single access
{
  let fallbackCalled = 0;
  let getterCalled = 0;
  let history = [];
  let blackList = Recorder.watch({otherProperty: true}, history);
  let object = Recorder.watch({[Symbol.unscopables]: blackList, get property() { getterCalled += 1 }}, history);
  with ({get property() { fallbackCalled += 1 }}) {
    with (object) {
      property;
    }
  }
  assertEquals([
    {name: "get", target: object, property: Symbol.unscopables, result: blackList, receiver: object},
    {name: "has", target: blackList, property: "property", result: false},
    {name: "has", target: object, property: "property", result: true},
    {name: "has", target: object, property: "property", result: true},
    {name: "get", target: object, property: "property", result: void 0, receiver: object},
  ], history);
  assertSame(1, getterCalled);
  assertSame(0, fallbackCalled);
}

// Object with @@unscopables but no match, property is present, multi access
{
  let fallbackCalled = 0;
  let getterCalled = 0;
  let history = [];
  let blackList = Recorder.watch({otherProperty: true}, history);
  let object = Recorder.watch({[Symbol.unscopables]: blackList, get property() { getterCalled += 1 }}, history);
  with ({get property() { fallbackCalled += 1 }}) {
    with (object) {
      property;
      property;
    }
  }
  assertEquals([
    {name: "get", target: object, property: Symbol.unscopables, result: blackList, receiver: object},
    {name: "has", target: blackList, property: "property", result: false},
    {name: "has", target: object, property: "property", result: true},
    {name: "has", target: object, property: "property", result: true},
    {name: "get", target: object, property: "property", result: void 0, receiver: object},
    {name: "get", target: object, property: Symbol.unscopables, result: blackList, receiver: object},
    {name: "has", target: blackList, property: "property", result: false},
    {name: "has", target: object, property: "property", result: true},
    {name: "has", target: object, property: "property", result: true},
    {name: "get", target: object, property: "property", result: void 0, receiver: object},
  ], history);
  assertSame(2, getterCalled);
  assertSame(0, fallbackCalled);
}
