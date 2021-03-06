/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.modules;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.modules.Loader;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.3 Loader Objects</h2>
 * <ul>
 * <li>26.3.4 Properties of %Loader% Instances
 * </ul>
 */
public final class LoaderObject extends OrdinaryObject {
    /** [[Loader]] */
    private Loader loader;

    public LoaderObject(Realm realm) {
        super(realm);
    }

    /** [[Loader]] */
    public Loader getLoader() {
        return loader;
    }

    /** [[Loader]] */
    public void setLoader(Loader loader) {
        assert this.loader == null && loader != null : "LoaderObject already initialised";
        this.loader = loader;
    }
}
