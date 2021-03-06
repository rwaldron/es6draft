/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.promise;

import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>25 Control Abstraction Objects</h1><br>
 * <h2>25.4 Promise Objects</h2>
 * <ul>
 * <li>25.4.6 Properties of Promise Instances
 * </ul>
 */
public final class PromiseObject extends OrdinaryObject {
    public enum Status {
        Unresolved, HasResolution, HasRejection
    }

    /** [[PromiseStatus]] */
    private Status status;

    /** [[PromiseConstructor]] */
    private Constructor constructor;

    /** [[PromiseResult]] */
    private Object result;

    /** [[PromiseResolveReactions]] */
    private List<PromiseReaction> resolveReactions;

    /** [[PromiseRejectReactions]] */
    private List<PromiseReaction> rejectReactions;

    public PromiseObject(Realm realm) {
        super(realm);
    }

    public void initialise() {
        assert status == null;
        status = PromiseObject.Status.Unresolved;
        resolveReactions = new ArrayList<>();
        rejectReactions = new ArrayList<>();
    }

    /** [[PromiseStatus]] */
    public Status getStatus() {
        return status;
    }

    /** [[PromiseConstructor]] */
    public Constructor getConstructor() {
        return constructor;
    }

    /** [[PromiseConstructor]] */
    public void setConstructor(Constructor constructor) {
        assert this.constructor == null && constructor != null;
        this.constructor = constructor;
    }

    /** [[PromiseResult]] */
    public Object getResult() {
        return result;
    }

    /** [[PromiseResolveReactions]] */
    public void addResolveReaction(PromiseReaction reaction) {
        assert status == Status.Unresolved;
        resolveReactions.add(reaction);
    }

    /** [[PromiseRejectReactions]] */
    public void addRejectReaction(PromiseReaction reaction) {
        assert status == Status.Unresolved;
        rejectReactions.add(reaction);
    }

    public List<PromiseReaction> resolve(Object resolution) {
        List<PromiseReaction> reactions = resolveReactions;
        resolve(Status.HasResolution, resolution);
        return reactions;
    }

    public List<PromiseReaction> reject(Object reason) {
        List<PromiseReaction> reactions = rejectReactions;
        resolve(Status.HasRejection, reason);
        return reactions;
    }

    private void resolve(Status status, Object result) {
        assert this.status == Status.Unresolved;
        this.result = result;
        this.resolveReactions = null;
        this.rejectReactions = null;
        this.status = status;
    }
}
