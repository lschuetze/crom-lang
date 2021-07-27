package com.oracle.truffle.sl.builtins;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.CachedLanguage;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.sl.SLLanguage;
import com.oracle.truffle.sl.runtime.*;

@NodeInfo(shortName = "newPlayer")
public abstract class SLAsPlayerBuiltin extends SLBuiltinNode {

    @Specialization(guards = { "obj != null", "obj == cachedObj" }, assumptions = "rolesUnchanged", limit = "3")
    public Object newPlayer(SLObject obj,
                            @Cached("obj") SLObject cachedObj,
                            @Cached("obj.rolesUnchanged.getAssumption()") Assumption rolesUnchanged,
                            @CachedLanguage SLLanguage language,
                            @CachedContext(SLLanguage.class) ContextReference<SLContext> contextRef,
                            @Cached("contextRef.get().getAllocationReporter()") AllocationReporter reporter,
                            @CachedLibrary("obj") DynamicObjectLibrary objLib,
                            @CachedLibrary(limit = "3") DynamicObjectLibrary roleLib,
                            @Cached("createPlayer(language, reporter, obj, objLib, roleLib)") SLPlayer player) {
        return player;
    }

    protected SLPlayer createPlayer(SLLanguage language, AllocationReporter reporter, SLObject object,
                                    DynamicObjectLibrary objectLibrary,
                                    DynamicObjectLibrary roleLibrary) {
        SLPlayer player = language.createPlayer(reporter, object);
        // TODO: Check if playerLibrary can be cached somehow?
        // (I doubt it, since it is only needed once when the player is created, and then never again, but still...)
        DynamicObjectLibrary playerLibrary = DynamicObjectLibrary.getUncached();
        player.wrap(playerLibrary, objectLibrary, roleLibrary);
        return player;
    }
}
