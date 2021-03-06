/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.modules.LinkSet.AddLoadToLinkSet;
import static com.github.anba.es6draft.runtime.modules.LinkSet.CreateLinkSet;
import static com.github.anba.es6draft.runtime.modules.LinkSet.LinkSetFailed;
import static com.github.anba.es6draft.runtime.modules.LinkSet.UpdateLinkSetOnLoad;
import static com.github.anba.es6draft.runtime.modules.Load.CreateLoad;
import static com.github.anba.es6draft.runtime.modules.Load.CreateLoadRequestObject;
import static com.github.anba.es6draft.runtime.objects.internal.ListIterator.FromScriptIterator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.LinkedMap;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.objects.modules.LoaderObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>15 ECMAScript Language: Modules and Scripts</h1><br>
 * <h2>15.2 Modules</h2>
 * <ul>
 * <li>15.2.4 Runtime Semantics: Module Loading
 * </ul>
 */
public final class ModuleLoading {
    private ModuleLoading() {
    }

    /**
     * 15.2.4.1 LoadModule(loader, name, options) Abstract Operation
     */
    public static ScriptObject LoadModule(ExecutionContext cx, Loader loader, Object name,
            Object options) {
        // FIXME: spec bug - source is undefined
        String source = null;
        /* step 1 (not applicable) */
        /* steps 2-3 */
        String sname = ToFlatString(cx, name);
        /* steps 4-5 */
        Object address = GetOption(cx, options, "address");
        /* steps 6-7 */
        AsyncStartLoadPartwayThrough.Step step;
        if (Type.isUndefined(address)) {
            step = AsyncStartLoadPartwayThrough.Step.Locate;
        } else {
            step = AsyncStartLoadPartwayThrough.Step.Fetch;
        }
        /* step 8 */
        ScriptObject metadata = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        /* step 9 */
        return PromiseOfStartLoadPartwayThrough(cx, step, loader, sname, metadata, source, address);
    }

    /**
     * 15.2.4.2 RequestLoad(loader, request, refererName, refererAddress) Abstract Operation
     */
    public static ScriptObject RequestLoad(ExecutionContext cx, Loader loader, String request,
            Object refererName, Object refererAddress) {
        /* steps 1-5 */
        CallNormalize f = new CallNormalize(cx.getRealm(), loader, request, refererName,
                refererAddress);
        /* step 6 */
        ScriptObject p = PromiseNew(cx, f);
        /* steps 7-8 */
        GetOrCreateLoad g = new GetOrCreateLoad(cx.getRealm(), loader);
        /* step 9 */
        return PromiseThen(cx, p, g);
    }

    /**
     * 15.2.4.2.1 CallNormalize(resolve, reject) Functions
     */
    public static final class CallNormalize extends BuiltinFunction {
        /** [[Loader]] */
        private final Loader loader;
        /** [[Request]] */
        private final String request;
        /** [[RefererName]] */
        private final Object refererName;
        /** [[RefererAddress]] */
        private final Object refererAddress;

        public CallNormalize(Realm realm, Loader loader, String request, Object refererName,
                Object refererAddress) {
            super(realm, ANONYMOUS, 2);
            this.loader = loader;
            this.request = request;
            this.refererName = refererName;
            this.refererAddress = refererAddress;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object resolve = args.length > 0 ? args[0] : UNDEFINED;
            @SuppressWarnings("unused")
            Object reject = args.length > 1 ? args[1] : UNDEFINED;
            /* step 1 */
            Loader loader = this.loader;
            /* step 2 */
            String request = this.request;
            /* step 3 */
            Object refererName = this.refererName;
            /* step 4 */
            Object refererAddress = this.refererAddress;
            /* step 5 */
            ScriptObject loaderObj = loader.getLoaderObj();
            /* step 6 */
            Object normalizeHook = Get(calleeContext, loaderObj, "normalize");
            // FIXME: missing [[Call]] check
            if (!IsCallable(normalizeHook)) {
                throw newTypeError(calleeContext, Messages.Key.NotCallable);
            }
            /* steps 7-8 */
            Object name = ((Callable) normalizeHook).call(calleeContext, loaderObj, request,
                    refererName, refererAddress);
            /* step 9 */
            assert IsCallable(resolve);
            return ((Callable) resolve).call(calleeContext, UNDEFINED, name);
        }
    }

    /**
     * 15.2.4.2.2 GetOrCreateLoad(name) Functions
     */
    public static final class GetOrCreateLoad extends BuiltinFunction {
        /** [[Loader]] */
        private final Loader loader;

        public GetOrCreateLoad(Realm realm, Loader loader) {
            super(realm, ANONYMOUS, 1);
            this.loader = loader;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object nameArg = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            Loader loader = this.loader;
            /* steps 2-3 */
            String name = ToFlatString(calleeContext, nameArg);
            /* step 4 */
            LinkedMap<String, ModuleLinkage> modules = loader.getModules();
            /* steps 5-6 */
            if (modules.containsKey(name)) {
                /* step 5 */
                ModuleLinkage existingModule = modules.get(name);
                Load load = CreateLoad(calleeContext, name);
                load.link(existingModule);
                return load;
            } else if (loader.getLoads().containsKey(name)) {
                /* step 6 */
                Load load = loader.getLoads().get(name);
                assert load.getStatus() == Load.Status.Loading
                        || load.getStatus() == Load.Status.Loaded;
                return load;
            }
            /* step 7 */
            Load load = CreateLoad(calleeContext, name);
            /* step 8 */
            loader.getLoads().put(name, load);
            /* step 9 */
            ProceedToLocate(calleeContext, loader, load);
            /* step 10 */
            return load;
        }
    }

    /**
     * 15.2.4.3 ProceedToLocate(loader, load, p) Abstract Operation
     */
    public static ScriptObject ProceedToLocate(ExecutionContext cx, Loader loader, Load load) {
        /* step 1 */
        ScriptObject p = PromiseOf(cx, UNDEFINED);
        /* steps 2-4 */
        CallLocate f = new CallLocate(cx.getRealm(), loader, load);
        /* step 5 */
        p = PromiseThen(cx, p, f);
        /* step 6 */
        return ProceedToFetch(cx, loader, load, p);
    }

    /**
     * 15.2.4.3.1 CallLocate Functions
     */
    public static final class CallLocate extends BuiltinFunction {
        /** [[Loader]] */
        private final Loader loader;
        /** [[Load]] */
        private final Load load;

        public CallLocate(Realm realm, Loader loader, Load load) {
            super(realm, ANONYMOUS, 0);
            this.loader = loader;
            this.load = load;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            /* step 1 */
            Loader loader = this.loader;
            /* step 2 */
            Load load = this.load;
            /* step 3 */
            ScriptObject loaderObj = loader.getLoaderObj();
            /* step 4-5 */
            Object hook = Get(calleeContext, loaderObj, "locate");
            /* step 6 */
            if (!IsCallable(hook)) {
                throw newTypeError(calleeContext, Messages.Key.NotCallable);
            }
            /* step 7 */
            ScriptObject obj = CreateLoadRequestObject(calleeContext, load.getNameOrNull(),
                    load.getMetadata());
            /* step 8 */
            // FIXME: spec bug - missing 'loader as thisArgument'
            return ((Callable) hook).call(calleeContext, loaderObj, obj);
        }
    }

    /**
     * 15.2.4.4 ProceedToFetch(loader, load, p) Abstract Operation
     */
    public static ScriptObject ProceedToFetch(ExecutionContext cx, Loader loader, Load load,
            ScriptObject p) {
        /* steps 1-4 */
        CallFetch f = new CallFetch(cx.getRealm(), loader, load);
        /* step 5 */
        p = PromiseThen(cx, p, f);
        /* step 6 */
        return ProceedToTranslate(cx, loader, load, p);
    }

    /**
     * 15.2.4.4.1 CallFetch(address) Functions
     */
    public static final class CallFetch extends BuiltinFunction {
        /** [[Loader]] */
        private final Loader loader;
        /** [[Load]] */
        private final Load load;

        public CallFetch(Realm realm, Loader loader, Load load) {
            super(realm, ANONYMOUS, 1);
            this.loader = loader;
            this.load = load;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object address = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            Loader loader = this.loader;
            /* step 2 */
            Load load = this.load;
            /* step 3 */
            if (load.getLinkSets().isEmpty()) {
                return UNDEFINED;
            }
            /* step 4 */
            load.setAddress(address);
            /* step 5 */
            LoaderObject loaderObj = loader.getLoaderObj();
            /* steps 6-7 */
            Object hook = Get(calleeContext, loaderObj, "fetch");
            /* step 8 */
            if (!IsCallable(hook)) {
                throw newTypeError(calleeContext, Messages.Key.NotCallable);
            }
            /* step 9 */
            ScriptObject obj = CreateLoadRequestObject(calleeContext, load.getNameOrNull(),
                    load.getMetadata(), address);
            /* step 10 */
            // FIXME: spec bug - missing 'loader as thisArgument'
            return ((Callable) hook).call(calleeContext, loaderObj, obj);
        }
    }

    /**
     * 15.2.4.5 ProceedToTranslate(loader, load, p) Abstract Operation
     */
    public static ScriptObject ProceedToTranslate(ExecutionContext cx, Loader loader, Load load,
            ScriptObject p) {
        /* step 1-3 */
        CallTranslate f1 = new CallTranslate(cx.getRealm(), loader, load);
        /* step 4 */
        p = PromiseThen(cx, p, f1);
        /* steps 5-7 */
        CallInstantiate f2 = new CallInstantiate(cx.getRealm(), loader, load);
        /* step 8 */
        p = PromiseThen(cx, p, f2);
        /* steps 9-11 */
        InstantiateSucceeded f3 = new InstantiateSucceeded(cx.getRealm(), loader, load);
        /* step 12 */
        p = PromiseThen(cx, p, f3);
        /* steps 13-14 */
        LoadFailed f4 = new LoadFailed(cx.getRealm(), load);
        /* step 15 */
        return PromiseCatch(cx, p, f4);
    }

    /**
     * 15.2.4.5.1 CallTranslate Functions
     */
    public static final class CallTranslate extends BuiltinFunction {
        /** [[Loader]] */
        private final Loader loader;
        /** [[Load]] */
        private final Load load;

        public CallTranslate(Realm realm, Loader loader, Load load) {
            super(realm, ANONYMOUS, 1);
            this.loader = loader;
            this.load = load;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object source = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            Loader loader = this.loader;
            /* step 2 */
            Load load = this.load;
            /* step 3 */
            if (load.getLinkSets().isEmpty()) {
                return UNDEFINED;
            }
            /* FIXME: missing step */
            LoaderObject loaderObj = loader.getLoaderObj();
            /* steps 4-5 */
            Object hook = Get(calleeContext, loaderObj, "translate");
            /* step 6 */
            if (!IsCallable(hook)) {
                throw newTypeError(calleeContext, Messages.Key.NotCallable);
            }
            /* step 7 */
            ScriptObject obj = CreateLoadRequestObject(calleeContext, load.getNameOrNull(),
                    load.getMetadata(), load.getAddress(), source);
            /* step 8 */
            // FIXME: spec bug - missing 'loader as thisArgument'
            return ((Callable) hook).call(calleeContext, loaderObj, obj);
        }
    }

    /**
     * 15.2.4.5.2 CallInstantiate Functions
     */
    public static final class CallInstantiate extends BuiltinFunction {
        /** [[Loader]] */
        private final Loader loader;
        /** [[Load]] */
        private final Load load;

        public CallInstantiate(Realm realm, Loader loader, Load load) {
            super(realm, ANONYMOUS, 1);
            this.loader = loader;
            this.load = load;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object source = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            Loader loader = this.loader;
            /* step 2 */
            Load load = this.load;
            /* step 3 */
            if (load.getLinkSets().isEmpty()) {
                return UNDEFINED;
            }
            String ssource = ToFlatString(calleeContext, source);// FIXME: add ToString() ?
            /* step 4 */
            load.setSource(ssource);
            /* step 5 */
            LoaderObject loaderObj = loader.getLoaderObj();
            /* steps 6-7 */
            Object hook = Get(calleeContext, loaderObj, "instantiate");
            /* step 8 */
            if (!IsCallable(hook)) {
                throw newTypeError(calleeContext, Messages.Key.NotCallable);
            }
            /* step 9 */
            ScriptObject obj = CreateLoadRequestObject(calleeContext, load.getNameOrNull(),
                    load.getMetadata(), load.getAddress(), ssource);
            /* step 10 */
            // FIXME: spec bug - missing 'loader as thisArgument'
            return ((Callable) hook).call(calleeContext, loaderObj, obj);
        }
    }

    /**
     * 15.2.4.5.3 InstantiateSucceeded(instantiateResult) Functions
     */
    public static final class InstantiateSucceeded extends BuiltinFunction {
        /** [[Loader]] */
        private final Loader loader;
        /** [[Load]] */
        private final Load load;

        public InstantiateSucceeded(Realm realm, Loader loader, Load load) {
            super(realm, ANONYMOUS, 1);
            this.loader = loader;
            this.load = load;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object instantiateResult = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            Loader loader = this.loader;
            /* step 2 */
            Load load = this.load;
            /* step 3 */
            if (load.getLinkSets().isEmpty()) {
                return UNDEFINED;
            }
            /* steps 4-6 */
            Iterable<String> depsList;
            if (Type.isUndefined(instantiateResult)) {
                /* step 4 */
                Parser parser = new Parser("", 1, calleeContext.getRealm().getOptions());
                Module parsedModule;
                try {
                    parsedModule = parser.parseModule(load.getSource());
                } catch (ParserException e) {
                    throw e.toScriptException(calleeContext);
                }
                // TODO: parsedModule as ModuleBody
                ModuleBody body = null;
                load.declarative(body);
                depsList = parsedModule.getScope().getModuleRequests();
            } else if (Type.isObject(instantiateResult)) {
                /* step 5 */
                ScriptObject instantiateResultObject = Type.objectValue(instantiateResult);
                Object deps = Get(calleeContext, instantiateResultObject, "deps");
                if (Type.isUndefined(deps)) {
                    depsList = new ArrayList<>();
                } else {
                    // TODO: convert ToString() here?
                    depsList = IterableToArray(calleeContext, deps);
                }
                Object execute = Get(calleeContext, instantiateResultObject, "execute");
                // TODO: assert callable here?
                if (!IsCallable(execute)) {
                    throw newTypeError(calleeContext, Messages.Key.NotCallable);
                }
                load.dynamic((Callable) execute);
            } else {
                /* step 6 */
                throw newTypeError(calleeContext, Messages.Key.NotObjectType);
            }
            return ProcessLoadDependencies(calleeContext, load, loader, depsList);
        }
    }

    /**
     * 15.2.4.5.4 LoadFailed Functions
     */
    public static final class LoadFailed extends BuiltinFunction {
        /** [[Load]] */
        private final Load load;

        public LoadFailed(Realm realm, Load load) {
            super(realm, ANONYMOUS, 1);
            this.load = load;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object exc = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            Load load = this.load;
            /* steps 2-4 */
            load.failed(exc);
            /* steps 5-6 */
            for (LinkSet linkSet : load.getSortedLinkSets()) {
                LinkSetFailed(calleeContext, linkSet, exc);
            }
            /* step 7 */
            assert load.getLinkSets().isEmpty();
            /* step 8 */
            return UNDEFINED;
        }
    }

    /**
     * 15.2.4.6 ProcessLoadDependencies(load, loader, depsList) Abstract Operation
     */
    public static ScriptObject ProcessLoadDependencies(ExecutionContext cx, Load load,
            Loader loader, Iterable<String> depsList) {
        /* step 1 */
        Object refererName = load.getNameOrNull();
        /* step 2 */
        load.setDependencies(new ArrayList<Load.Dependency>());
        /* step 3 */
        List<ScriptObject> loadPromises = new ArrayList<>();
        /* step 4 */
        for (String request : depsList) {
            /* step 4a */
            ScriptObject p = RequestLoad(cx, loader, request, refererName, load.getAddress());
            /* steps 4b-4d */
            AddDependencyLoad f = new AddDependencyLoad(cx.getRealm(), load, request);
            /* step 4e */
            p = PromiseThen(cx, p, f);
            /* step 4f */
            loadPromises.add(p);
        }
        /* step 5 */
        ScriptObject p = PromiseAll(cx, loadPromises);
        /* steps 6-7 */
        LoadSucceeded f = new LoadSucceeded(cx.getRealm(), load);
        /* step 8 */
        return PromiseThen(cx, p, f);
    }

    /**
     * 15.2.4.6.1 AddDependencyLoad(depLoad) Functions
     */
    public static final class AddDependencyLoad extends BuiltinFunction {
        /** [[ParentLoad]] */
        private final Load parentLoad;
        /** [[Request]] */
        private final String request;

        public AddDependencyLoad(Realm realm, Load parentLoad, String request) {
            super(realm, ANONYMOUS, 1);
            this.parentLoad = parentLoad;
            this.request = request;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            @SuppressWarnings("unused")
            ExecutionContext calleeContext = calleeContext();
            Object depLoadArg = args.length > 0 ? args[0] : null;
            assert depLoadArg instanceof Load;
            Load depLoad = (Load) depLoadArg;
            /* step 1 */
            Load parentLoad = this.parentLoad;
            /* step 2 */
            String request = this.request;
            /* step 3 */
            // FIXME: implement
            // assert parentLoad.dependencies;
            /* step 4 */
            // FIXME: [[key]] / [[value]] relation unclear
            assert depLoad.getName() != null : "unexpected anonymous dependency load";
            parentLoad.addDependency(request, depLoad.getName());
            /* step 5 */
            if (depLoad.getStatus() != Load.Status.Linked) {
                List<LinkSet> linkSets = new ArrayList<>(parentLoad.getLinkSets());
                for (LinkSet linkSet : linkSets) {
                    AddLoadToLinkSet(linkSet, depLoad);
                }
            }
            return UNDEFINED;
        }
    }

    /**
     * 15.2.4.6.2 LoadSucceeded Functions
     */
    public static final class LoadSucceeded extends BuiltinFunction {
        /** [[Load]] */
        private final Load load;

        public LoadSucceeded(Realm realm, Load load) {
            super(realm, ANONYMOUS, 0);
            this.load = load;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            /* step 1 */
            Load load = this.load;
            /* steps 2-3 */
            load.loaded();
            /* steps 4-5 */
            for (LinkSet linkSet : load.getSortedLinkSets()) {
                UpdateLinkSetOnLoad(calleeContext, linkSet, load);
            }
            return UNDEFINED;
        }
    }

    /**
     * 15.2.4.7 PromiseOfStartLoadPartwayThrough (step, loader, name, metadata, source, address)
     */
    public static ScriptObject PromiseOfStartLoadPartwayThrough(ExecutionContext cx,
            AsyncStartLoadPartwayThrough.Step step, Loader loader, String name, Object metadata,
            Object source, Object address) {
        /* step 1 */
        AsyncStartLoadPartwayThrough f = new AsyncStartLoadPartwayThrough(cx.getRealm(), step,
                loader, name, metadata, source, address);
        /* step 2 */
        return PromiseNew(cx, f);
    }

    /**
     * 15.2.4.7.1 AsyncStartLoadPartwayThrough Functions
     */
    public static final class AsyncStartLoadPartwayThrough extends BuiltinFunction {
        /** [[Step]] */
        private final Step step;
        /** [[Loader]] */
        private final Loader loader;
        /** [[ModuleName]] */
        private final String moduleName;
        /** [[ModuleMetadata]] */
        private final Object moduleMetadata;
        /** [[ModuleSource]] */
        private final Object moduleSource;
        /** [[ModuleAddress]] */
        private final Object moduleAddress;

        public enum Step {
            Locate, Fetch, Translate
        }

        public AsyncStartLoadPartwayThrough(Realm realm, Step step, Loader loader,
                String moduleName, Object moduleMetadata, Object moduleSource, Object moduleAddress) {
            super(realm, ANONYMOUS, 2);
            assert moduleName != null : "anonymous module in async-start-load";
            this.step = step;
            this.loader = loader;
            this.moduleName = moduleName;
            this.moduleMetadata = moduleMetadata;
            this.moduleSource = moduleSource;
            this.moduleAddress = moduleAddress;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object resolveArg = args.length > 0 ? args[0] : UNDEFINED;
            Object rejectArg = args.length > 1 ? args[1] : UNDEFINED;
            assert IsCallable(resolveArg) && IsCallable(rejectArg);
            Callable resolve = (Callable) resolveArg;
            @SuppressWarnings("unused")
            Callable reject = (Callable) rejectArg;
            /* step 1 (not applicable) */
            /* step 2 */
            Loader loader = this.loader;
            /* step 3 */
            String name = this.moduleName;
            /* step 4 */
            Step step = this.step;
            /* step 5 */
            @SuppressWarnings("unused")
            Object source = this.moduleSource;
            /* step 6 */
            if (loader.getModules().containsKey(name)) {
                throw newTypeError(calleeContext, Messages.Key.InternalError);
            }
            /* step 7 */
            if (loader.getLoads().containsKey(name)) {
                throw newTypeError(calleeContext, Messages.Key.InternalError);
            }
            /* steps 8-9 */
            Load load = CreateLoad(calleeContext, name, moduleMetadata);
            /* step 10 */
            LinkSet linkSet = CreateLinkSet(calleeContext, loader, load);
            /* step 11 */
            loader.getLoads().put(name, load);
            /* step 12 */
            resolve.call(calleeContext, UNDEFINED, linkSet.getDone());
            /* steps 13-15 */
            if (step == Step.Locate) {
                /* step 13 */
                return ProceedToLocate(calleeContext, loader, load);
            } else if (step == Step.Fetch) {
                /* step 14 */
                ScriptObject addressPromise = PromiseOf(calleeContext, moduleAddress);
                return ProceedToFetch(calleeContext, loader, load, addressPromise);
            } else {
                /* step 15 */
                assert step == Step.Translate;
                load.setAddress(moduleAddress);
                ScriptObject sourcePromise = PromiseOf(calleeContext, moduleSource);
                return ProceedToTranslate(calleeContext, loader, load, sourcePromise);
            }
        }
    }

    // FIXME: missing definition in spec
    private static List<String> IterableToArray(ExecutionContext cx, Object iterable) {
        Iterator<?> iterator = FromScriptIterator(cx, GetIterator(cx, iterable));
        List<String> array = new ArrayList<>();
        while (iterator.hasNext()) {
            Object value = iterator.next();
            array.add(ToFlatString(cx, value));
        }
        return array;
    }
}
