/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function StringExtensions(global) {
"use strict";

const {
  Object, Function, String, RegExp,
} = global;

const {
  replace: RegExp_prototype_replace
} = RegExp.prototype;

const {
  match: String_prototype_match,
  replace: String_prototype_replace,
  search: String_prototype_search,
} = String.prototype;

const $CallFunction = Function.prototype.call.bind(Function.prototype.call);

const specialCharsRE = /[|^$\\()[\]{}.?*+]/g;

function ToFlatPattern(p) {
  return $CallFunction(RegExp_prototype_replace, specialCharsRE, p, "\\$&");
}

/*
 * Add support to specify regular expression flags
 */
Object.defineProperties(Object.assign(String.prototype, {
  match(regexp, flags = void 0) {
    if (typeof regexp == 'string' && flags !== void 0) {
      regexp = new RegExp(regexp, flags);
    }
    return $CallFunction(String_prototype_match, this, regexp);
  },
  search(regexp, flags = void 0) {
    if (typeof regexp == 'string' && flags !== void 0) {
      regexp = new RegExp(regexp, flags);
    }
    return $CallFunction(String_prototype_search, this, regexp);
  },
  replace(searchValue, replaceValue, flags = void 0) {
    if (typeof searchValue == 'string' && flags !== void 0) {
      searchValue = new RegExp(ToFlatPattern(searchValue), flags);
    }
    return $CallFunction(String_prototype_replace, this, searchValue, replaceValue);
  },
}), {
  match: {enumerable: false},
  search: {enumerable: false},
  replace: {enumerable: false},
});

})(this);
