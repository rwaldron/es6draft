/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function MozillaLegacyExtensions(global) {
"use strict";

const Object = global.Object,
      Function = global.Function,
      Array = global.Array,
      String = global.String,
      Boolean = global.Boolean,
      Number = global.Number,
      Math = global.Math,
      Date = global.Date,
      RegExp = global.RegExp,
      Error = global.Error,
      TypeError = global.TypeError,
      JSON = global.JSON,
      Proxy = global.Proxy,
      Map = global.Map,
      Set = global.Set,
      WeakMap = global.WeakMap,
      Reflect = global.Reflect;

const Object_defineProperty = Object.defineProperty,
      Object_getOwnPropertyDescriptor = Object.getOwnPropertyDescriptor,
      Object_getOwnPropertyNames = Object.getOwnPropertyNames,
      Object_hasOwnProperty = Function.prototype.call.bind(Object.prototype.hasOwnProperty),
      Object_keys = Object.keys;

function Quote(s, qc = '"') {
  var r = '';
  for (var i = 0, len = s.length; i < len; ++i) {
    var c = s.charCodeAt(i);
    switch (c) {
      case 0x09: r += "\\t"; continue;
      case 0x0A: r += "\\n"; continue;
      case 0x0B: r += "\\b"; continue;
      case 0x0C: r += "\\v"; continue;
      case 0x0D: r += "\\r"; continue;
      case 0x22: r += "\\\""; continue;
      case 0x5C: r += "\\\\"; continue;
    }
    if (c == 0x27 && qc == "'") {
      r += "\\\'";
    }
    if (c < 20) {
      r += "\\x" + (c < 0x10 ? "0" : "") + c.toString(16).toUpperCase();
    } else if (c < 0x7F) {
      r += s.charAt(i);
    } else if (c < 0xFF) {
      r += "\\x" + c.toString(16).toUpperCase();
    } else {
      r += "\\u" + (c < 0x1000 ? "0" : "") + c.toString(16).toUpperCase();
    }
  }
  return qc + r + qc;
}

const ASCII_Ident = /^[_$a-zA-Z][_$a-zA-Z0-9]*$/;

function IsInt32(name) {
  return ((name | 0) >= 0 && (name | 0) <= 0x7fffffff && (name | 0) + "" == name);
}

function ToPropertyName(name) {
  if (ASCII_Ident.test(name) || IsInt32(name)) {
    return name;
  }
  return Quote(name, "'");
}

function ToSource(o) {
  switch (typeof o) {
    case 'undefined':
      return "(void 0)";
    case 'boolean':
    case 'number':
      return "" + o;
    case 'string':
      return Quote(o);
    case 'object':
    default:
      if (o !== null && typeof o.toSource == 'function') {
        return o.toSource();
      }
      return "null";
  }
}

Object.defineProperty(global, getSym("@@toStringTag"), {
  value: "global", writable: true, enumerable: false, configurable: true
});

Object.defineProperty(Object.assign(global, {
  uneval(o) {
    return ToSource(o);
  }
}), "uneval", {enumerable: false});

Object.defineProperties(Object.assign(Object.prototype, {
  __defineGetter__(name, getter) {
    var obj = (this != null ? Object(this) : global);
    Object_defineProperty(obj, name, {get: getter, enumerable: true, configurable: true});
  },
  __defineSetter__(name, setter) {
    var obj = (this != null ? Object(this) : global);
    Object_defineProperty(obj, name, {set: setter, enumerable: true, configurable: true});
  },
  __lookupGetter__(name) {
    var p = this;
    do {
      var desc = Object_getOwnPropertyDescriptor(p, name);
      if (desc && desc.get) return desc.get;
    } while ((p = p.__proto__));
  },
  __lookupSetter__(name) {
    var p = this;
    do {
      var desc = Object_getOwnPropertyDescriptor(p, name);
      if (desc && desc.set) return desc.set;
    } while ((p = p.__proto__));
  }
}), {
  __defineGetter__: {enumerable: false},
  __defineSetter__: {enumerable: false},
  __lookupGetter__: {enumerable: false},
  __lookupSetter__: {enumerable: false},
});

const wm = new WeakMap();
var depth = 0;

Object.defineProperty(Object.assign(Object.prototype, {
  toSource() {
    if (this == null) throw TypeError();
    var obj = Object(this);
    if (wm.has(obj)) {
      return "{}";
    }
    wm.set(obj, null);
    depth += 1;
    try {
      var s = "";
      var names = Object_keys(obj);
      for (var i = 0, len = names.length; i < len; ++i) {
        var name = names[i];
        var desc = Object_getOwnPropertyDescriptor(obj, name);
        if (desc == null) {
          // ignore removed properties
        } else if ('value' in desc) {
          s += ToPropertyName(name) + ":" + ToSource(desc.value);
        } else {
          if (desc.get !== void 0) {
            var fsrc = ToSource(desc.get);
            s += "get " + ToPropertyName(name) + fsrc.substr(fsrc.indexOf('('));
            if (desc.set !== void 0) s += ", ";
          }
          if (desc.set !== void 0) {
            var fsrc = ToSource(desc.set);
            s += "set " + ToPropertyName(name) + fsrc.substr(fsrc.indexOf('('));
          }
        }
        if (i + 1 < len) s += ", ";
      }
      if (depth > 1) {
        return "{" + s + "}";
      }
      return "({" + s + "})";
    } finally {
      wm.delete(obj);
      depth -= 1;
    }
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Function.prototype, {
  toSource() {
    if (this.name == "") {
      return "(" + Function.prototype.toString.call(this) + ")";
    }
    return Function.prototype.toString.call(this);
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Array.prototype, {
  toSource() {
    if (!Array.isArray(this)) throw TypeError();
    if (wm.has(this)) {
      return "[]";
    }
    wm.set(this, null);
    depth += 1;
    try {
      var s = "";
      for (var i = 0, len = this.length; i < len; ++i) {
        if (!Object_hasOwnProperty(this, i)) {
          s += ",";
          if (i + 1 < len) s += " ";
        } else {
          s += ToSource(this[i]);
          if (i + 1 < len) s += ", ";
        }
      }
      return "[" + s + "]";
    } finally {
      wm.delete(this);
      depth -= 1;
    }
  }
}), "toSource", {enumerable: false});

const Array_prototype_toString = Array.prototype.toString;

Object.defineProperty(Object.assign(Array.prototype, {
  toString() {
    if (wm.has(this)) {
      return "";
    }
    wm.set(this, null);
    try {
      return Array_prototype_toString.call(this);
    } finally {
      wm.delete(this);
    }
  }
}), "toString", {enumerable: false});

Object.defineProperties(Object.assign(Array, {
  join(array, $1, ...more) {
    return Array.prototype.join.call(array, $1, ...more);
  },
  reverse(array, ...more) {
    return Array.prototype.reverse.call(array, ...more);
  },
  sort(array, $1, ...more) {
    return Array.prototype.sort.call(array, $1, ...more);
  },
  push(array, $1, ...more) {
    if (arguments.length <= 1) {
      return Array.prototype.push.call(array);
    }
    return Array.prototype.push.call(array, ...[$1, ...more]);
  },
  pop(array, ...more) {
    return Array.prototype.pop.call(array, ...more);
  },
  shift(array, ...more) {
    return Array.prototype.shift.call(array, ...more);
  },
  unshift(array, $1, ...more) {
    if (arguments.length <= 1) {
      return Array.prototype.unshift.call(array);
    }
    return Array.prototype.unshift.call(array, ...[$1, ...more]);
  },
  splice(array, $1, $2, ...more) {
    return Array.prototype.splice.call(array, $1, $2, ...more);
  },
  concat(array, $1, ...more) {
    if (arguments.length <= 1) {
      return Array.prototype.concat.call(array);
    }
    return Array.prototype.concat.call(array, ...[$1, ...more]);
  },
  slice(array, $1, $2, ...more) {
    return Array.prototype.slice.call(array, $1, $2, ...more);
  },
  filter(array, $1, ...more) {
    return Array.prototype.filter.call(array, $1, ...more);
  },
  lastIndexOf(array, $1, ...more) {
    return Array.prototype.lastIndexOf.call(array, $1, ...more);
  },
  indexOf(array, $1, ...more) {
    return Array.prototype.indexOf.call(array, $1, ...more);
  },
  forEach(array, $1, ...more) {
    return Array.prototype.forEach.call(array, $1, ...more);
  },
  map(array, $1, ...more) {
    return Array.prototype.map.call(array, $1, ...more);
  },
  every(array, $1, ...more) {
    return Array.prototype.every.call(array, $1, ...more);
  },
  some(array, $1, ...more) {
    return Array.prototype.some.call(array, $1, ...more);
  },
  reduce(array, $1, ...more) {
    return Array.prototype.reduce.call(array, $1, ...more);
  },
  reduceRight(array, $1, ...more) {
    return Array.prototype.reduceRight.call(array, $1, ...more);
  },
}), {
  join: {enumerable: false},
  reverse: {enumerable: false},
  sort: {enumerable: false},
  push: {enumerable: false},
  pop: {enumerable: false},
  shift: {enumerable: false},
  unshift: {enumerable: false},
  splice: {enumerable: false},
  concat: {enumerable: false},
  slice: {enumerable: false},
  filter: {enumerable: false},
  lastIndexOf: {enumerable: false},
  indexOf: {enumerable: false},
  forEach: {enumerable: false},
  map: {enumerable: false},
  every: {enumerable: false},
  some: {enumerable: false},
  reduce: {enumerable: false},
  reduceRight: {enumerable: false},
});

Object.defineProperties(Object.assign(String, {
  quote(string, ...more) {
    return String.prototype.quote.call(string, ...more);
  },
  substring(string, $1, $2, ...more) {
    return String.prototype.substring.call(string, $1, $2, ...more);
  },
  toLowerCase(string, ...more) {
    return String.prototype.toLowerCase.call(string, ...more);
  },
  toUpperCase(string, ...more) {
    return String.prototype.toUpperCase.call(string, ...more);
  },
  charAt(string, $1, ...more) {
    return String.prototype.charAt.call(string, $1, ...more);
  },
  charCodeAt(string, $1, ...more) {
    return String.prototype.charCodeAt.call(string, $1, ...more);
  },
  contains(string, $1, ...more) {
    return String.prototype.contains.call(string, $1, ...more);
  },
  indexOf(string, $1, ...more) {
    return String.prototype.indexOf.call(string, $1, ...more);
  },
  lastIndexOf(string, $1, ...more) {
    return String.prototype.lastIndexOf.call(string, $1, ...more);
  },
  startsWith(string, $1, ...more) {
    return String.prototype.startsWith.call(string, $1, ...more);
  },
  endsWith(string, $1, ...more) {
    return String.prototype.endsWith.call(string, $1, ...more);
  },
  trim(string, ...more) {
    return String.prototype.trim.call(string, ...more);
  },
  trimLeft(string, ...more) {
    return String.prototype.trimLeft.call(string, ...more);
  },
  trimRight(string, ...more) {
    return String.prototype.trimRight.call(string, ...more);
  },
  toLocaleLowerCase(string, ...more) {
    return String.prototype.toLocaleLowerCase.call(string, ...more);
  },
  toLocaleUpperCase(string, ...more) {
    return String.prototype.toLocaleUpperCase.call(string, ...more);
  },
  localeCompare(string, $1, ...more) {
    return String.prototype.localeCompare.call(string, $1, ...more);
  },
  match(string, $1, ...more) {
    return String.prototype.match.call(string, $1, ...more);
  },
  search(string, $1, ...more) {
    return String.prototype.search.call(string, $1, ...more);
  },
  replace(string, $1, $2, ...more) {
    return String.prototype.replace.call(string, $1, $2, ...more);
  },
  split(string, $1, $2, ...more) {
    return String.prototype.split.call(string, $1, $2, ...more);
  },
  substr(string, $1, $2, ...more) {
    return String.prototype.substr.call(string, $1, $2, ...more);
  },
  concat(string, $1, ...more) {
    if (arguments.length <= 1) {
      return String.prototype.concat.call(string);
    }
    return String.prototype.concat.call(string, ...[$1, ...more]);
  },
  slice(string, $1, $2, ...more) {
    return String.prototype.slice.call(string, $1, $2, ...more);
  },
}), {
  quote: {enumerable: false},
  substring: {enumerable: false},
  toLowerCase: {enumerable: false},
  toUpperCase: {enumerable: false},
  charAt: {enumerable: false},
  charCodeAt: {enumerable: false},
  contains: {enumerable: false},
  indexOf: {enumerable: false},
  lastIndexOf: {enumerable: false},
  startsWith: {enumerable: false},
  endsWith: {enumerable: false},
  trim: {enumerable: false},
  trimLeft: {enumerable: false},
  trimRight: {enumerable: false},
  toLocaleLowerCase: {enumerable: false},
  toLocaleUpperCase: {enumerable: false},
  localeCompare: {enumerable: false},
  match: {enumerable: false},
  search: {enumerable: false},
  replace: {enumerable: false},
  split: {enumerable: false},
  substr: {enumerable: false},
  concat: {enumerable: false},
  slice: {enumerable: false},
});

const String_prototype_match = String.prototype.match,
      String_prototype_search = String.prototype.search,
      String_prototype_replace = String.prototype.replace;

Object.defineProperties(Object.assign(String.prototype, {
  quote() {
    return Quote(String.prototype.toString.call(this));
  },
  match(regexp, flags) {
    if (typeof regexp == 'string' && flags !== void 0) {
      regexp = new RegExp(regexp, flags);
    }
    return String_prototype_match.call(this, regexp);
  },
  search(regexp, flags) {
    if (typeof regexp == 'string' && flags !== void 0) {
      regexp = new RegExp(regexp, flags);
    }
    return String_prototype_search.call(this, regexp);
  },
  replace(searchValue, replaceValue, flags) {
    if (typeof searchValue == 'string' && flags !== void 0) {
      searchValue = new RegExp(searchValue, flags);
    }
    return String_prototype_replace.call(this, searchValue, replaceValue);
  },
}), {
  quote: {enumerable: false},
  match: {enumerable: false},
  search: {enumerable: false},
  replace: {enumerable: false},
});

Object.defineProperty(Object.assign(String.prototype, {
  toSource() {
    return "(new String(" + Quote(String.prototype.toString.call(this)) + "))";
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Boolean.prototype, {
  toSource() {
    return "(new Boolean(" + Boolean.prototype.valueOf.call(this) + "))";
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Number.prototype, {
  toSource() {
    return "(new Number(" + Number.prototype.valueOf.call(this) + "))";
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Math, {
  toSource() {
    return "Math";
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Math, {
  imul(u, v) {
    u = u >>> 0;
    v = v >>> 0;
    var u0 = u & 0xFFFF, u1 = u >>> 16,
        v0 = v & 0xFFFF, v1 = v >>> 16;
    return (((u1 * v0 + u0 * v1) << 16) + (u0 * v0)) | 0;
  }
}), "imul", {enumerable: false});

Object.defineProperty(Object.assign(Date.prototype, {
  toSource() {
    return "(new Date(" + Date.prototype.valueOf.call(this) + "))";
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(RegExp.prototype, {
  toSource() {
    return RegExp.prototype.toString.call(this);
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Error.prototype, {
  toSource() {
    return "(new " + this.name + "(" + ToSource(this.message) + "))";
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(JSON, {
  toSource() {
    return "JSON";
  }
}), "toSource", {enumerable: false});

function toProxyHandler(handler) {
  var TypeErrorThrower = () => { throw TypeError() };
  var proxyHandler = {
    getOwnPropertyDescriptor: TypeErrorThrower,
    getPropertyDescriptor: TypeErrorThrower,
    getOwnPropertyNames: TypeErrorThrower,
    getPropertyNames: TypeErrorThrower,
    defineProperty: TypeErrorThrower,
    delete: TypeErrorThrower,
    ownKeys: TypeErrorThrower,
  };

  // fundamental traps
  if ('getOwnPropertyDescriptor' in handler) {
    proxyHandler['getOwnPropertyDescriptor'] = (_, pk) => handler['getOwnPropertyDescriptor'](pk);
  }
  if (!('getOwnPropertyDescriptor' in handler) && 'getPropertyDescriptor' in handler) {
    proxyHandler['getOwnPropertyDescriptor'] = (_, pk) => handler['getPropertyDescriptor'](pk);
  }
  if ('getOwnPropertyNames' in handler) {
    proxyHandler['ownKeys'] = () => Array.from(handler['getOwnPropertyNames']()).values();
  }
  if ('defineProperty' in handler) {
    proxyHandler['defineProperty'] = (_, pk, desc) => (handler['defineProperty'](pk, desc), true);
  }
  if ('delete' in handler) {
    proxyHandler['deleteProperty'] = (_, pk) => handler['delete'](pk);
  }

  // derived traps
  if ('has' in handler) {
    proxyHandler['has'] = (_, pk) => handler['has'](pk);
  } else {
    proxyHandler['has'] = (_, pk) => !!handler['getPropertyDescriptor'](pk);
  }
  if ('hasOwn' in handler) {
    proxyHandler['hasOwn'] = (_, pk) => handler['hasOwn'](pk);
  } else {
    proxyHandler['hasOwn'] = (_, pk) => !!handler['getOwnPropertyDescriptor'](pk);
  }
  if ('get' in handler) {
    proxyHandler['get'] = (_, pk, receiver) => handler['get'](receiver, pk);
  } else {
    proxyHandler['get'] = (_, pk, receiver) => {
      // XXX: special case for 'tests/for-of/proxy-3.js'
      if (pk === iteratorSym) {
        pk = "iterator";
      }
      var desc = handler['getPropertyDescriptor'](pk);
      if (desc !== undefined && 'value' in desc) {
        return desc.value;
      }
      if (desc !== undefined && desc.get !== undefined) {
        return desc.get.call(receiver);
      }
    };
  }
  if ('set' in handler) {
    proxyHandler['set'] = (_, pk, value, receiver) => handler['set'](receiver, pk, value);
  } else {
    proxyHandler['set'] = (_, pk, value, receiver) => {
      var desc = handler['getOwnPropertyDescriptor'](pk);
      if (!desc) {
        desc = handler['getPropertyDescriptor'](pk);
        if (!desc) {
          desc = {
            writable: true, enumerable: true, configurable: true
          };
        }
      }
      if (('writable' in desc) && desc.writable) {
        handler['defineProperty'](pk, (desc.value = value, desc));
        return true;
      }
      if (!('writable' in desc) && desc.set) {
        desc.set.call(receiver, value);
        return true;
      }
      return false;
    };
  }
  if ('enumerate' in handler) {
    proxyHandler['enumerate'] = () => Array.from(handler['enumerate']()).values();
  } else if ('iterate' in handler) {
    proxyHandler['enumerate'] = () => handler['iterate']();
  } else {
    proxyHandler['enumerate'] = () => handler['getPropertyNames'].filter(
      pk => handler['getPropertyDescriptor'](pk).enumerable
    ).values();
  }
  if ('keys' in handler) {
    proxyHandler['ownKeys'] = () => Array.from(handler['keys']()).values();
  }
  return proxyHandler;
}

Object.defineProperties(Object.assign(Proxy, {
  create(handler, proto = null) {
    if (Object(handler) !== handler) throw TypeError();
    var proxyTarget = Object.create(proto);
    var proxyHandler = Object.assign({
      setPrototypeOf() { throw TypeError() }
    }, toProxyHandler(handler));
    return new Proxy(proxyTarget, proxyHandler);
  },
  createFunction(handler, callTrap, constructTrap = callTrap) {
    if (Object(handler) !== handler) throw TypeError();
    if (typeof callTrap != 'function') throw TypeError();
    if (typeof constructTrap != 'function') throw TypeError();
    var proxyTarget = function(){};
    var proxyHandler = Object.assign({
      setPrototypeOf() { throw TypeError() },
      apply(_, thisValue, args) { return callTrap.apply(thisValue, args) },
      construct(_, args) { return new constructTrap(...args) }
    }, toProxyHandler(handler));
    return new Proxy(proxyTarget, proxyHandler);
  }
}), {
  create: {enumerable: false},
  createFunction: {enumerable: false},
});

const iteratorSym = getSym("@@iterator");

Object.defineProperty(Object.prototype, iteratorSym, {
  get() { return this.iterator }
});

const nextSym = newSym("next");

function ToIterator(instance, obj, keys) {
  var iter = (
    Array.isArray(obj) && keys ? obj.map((_, k) => k) :
    Array.isArray(obj) ? obj.map((v, k) => [k, v]) :
    keys ? Object_keys(Object(obj)) :
    Object_keys(Object(obj)).map(k => [k, obj[k]])
  ).values();
  var next = iter.next.bind(iter);
  Object_defineProperty(instance, nextSym, {value: next, configurable: false});
  return new Proxy(instance, {enumerate: () => iter});
}

function IsIterator(o) {
  return Object(o) === o && Object_hasOwnProperty(o, nextSym);
}

function Iterator(obj, keys) {
  if (IsIterator(this) && this[nextSym] === null) {
    return ToIterator(this, obj, keys);
  } else {
    return new Iterator(obj, keys);
  }
}
Object.defineProperty(global, "Iterator", {
  value: Iterator,
  writable: true, enumerable: false, configurable: true
});

Object.defineProperty(Iterator, getSym("@@create"), {
  value: function() {
    var o = Object.create(Iterator.prototype);
    Object_defineProperty(o, nextSym, {value: null, configurable: true});
    return o;
  }
});

Iterator.prototype = ToIterator(Object.create(Object.prototype), []);

Object.defineProperties(Object.assign(Iterator.prototype, {
  iterator() {
    return this;
  },
  next() {
    if (!IsIterator(this)) {
      throw TypeError();
    }
    return this[nextSym]();
  }
}), {
  iterator: {enumerable: false},
  next: {enumerable: false},
});

// adjust prototype chain for built-in iterators
[[], new Map, new Set].forEach(v => v.values().__proto__.__proto__ = Iterator.prototype);

// make prototype.iterator() an own data property and remove @@iterator hook
[Map, Set].forEach(
  ctor => {
    Object.defineProperty(ctor.prototype, "iterator", {
      value: ctor.prototype[iteratorSym],
      writable: true, enumerable: false, configurable: true
    });
    delete ctor.prototype[iteratorSym];
  }
);

const ArrayPrototype_iterator = (function() {
  const arrayIterator = Array.prototype[iteratorSym];
  const throwsOnGet = new Proxy({}, {get: () => { throw TypeError() }});
  return {
    iterator() {
      return arrayIterator.call(this != null ? this : throwsOnGet);
    }
  }.iterator;
})();

// remove @@iterator on Array.prototype
delete Array.prototype[iteratorSym];

const TypedArrays = [Int8Array, Uint8Array, Uint8ClampedArray, Int16Array, Uint16Array, Int32Array, Uint32Array, Float32Array, Float64Array];

// make Strings and TypedArrays iterable
[Array, String, ...TypedArrays].forEach(
  ctor => {
    Object.defineProperty(ctor.prototype, "iterator", {
      value() { return ArrayPrototype_iterator.call(this) },
      writable: true, enumerable: false, configurable: true
    });
  }
);

// it-object
var it_custom = undefined;
const it_mapped = {
  __proto__: null,
  custom: true,
  customRdOnly: false,
};
const it_target = Object.create(Object.prototype, {
  color: {writable: true, enumerable: true, configurable: true},
  height: {writable: true, enumerable: true, configurable: true},
  width: {writable: true, enumerable: true, configurable: true},
  funny: {writable: true, enumerable: true, configurable: true},
  array: {writable: true, enumerable: true, configurable: true},
  rdonly: {writable: false, enumerable: true, configurable: true},
  custom: {writable: true, enumerable: true, configurable: true},
  customRdOnly: {writable: false, enumerable: true, configurable: true},
  customNative: {
    get() { if (this === it) return it_custom },
    set(v) { if (this === it) it_custom = v },
    enumerable: true, configurable: true
  }
});

Object.defineProperty(it_target, getSym("@@toStringTag"), {
  value: "It"
});

const it = Proxy(it_target, {
  getOwnPropertyDescriptor(t, pk) {
    if (pk in it_mapped) {
      return Object.assign(Reflect.getOwnPropertyDescriptor(t, pk), {value: it_custom});
    }
    return Reflect.getOwnPropertyDescriptor(t, pk);
  },
  defineProperty(t, pk, desc) {
    delete it_mapped[pk];
    return Reflect.defineProperty(t, pk, desc);
  },
  get(t, pk, r) {
    if (pk in it_mapped) {
      return it_custom;
    }
    return Reflect.get(t, pk, r);
  },
  set(t, pk, v, r) {
    if (pk in it_mapped) {
      if (it_mapped[pk]) {
        it_custom = v;
      }
      return it_mapped[pk];
    }
    return Reflect.set(t, pk, v, r);
  },
  deleteProperty(t, pk) {
    delete it_mapped[pk];
    return Reflect.deleteProperty(t, pk);
  },
  enumerate(t) {
    if (it.enum_fail) {
      throw Error("its enumeration failed");
    }
    return Reflect.enumerate(t);
  },
});

Object.defineProperty(global, "it", {
  value: it,
  writable: true, enumerable: false, configurable: true
});

})(this);