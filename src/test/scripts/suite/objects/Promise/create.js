/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertBuiltinFunction,
} = Assert;


/* Promise [ @@create ] ( ) */

assertBuiltinFunction(Promise[Symbol.create], "[Symbol.create]", 0);

