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

    @Specialization(guards = { "obj != null", "obj.roles.size() == 0" })
    public Object noRoles(SLObject obj) {
        return obj;
    }

    @Specialization(guards = { "obj != null", "obj == cachedObj" },
            assumptions = "rolesUnchanged", limit = "3")
    public Object newPlayer(SLObject obj,
                            @Cached("obj") SLObject cachedObj,
                            @Cached("obj.rolesUnchanged.getAssumption()") Assumption rolesUnchanged,
                            @CachedLanguage SLLanguage language,
                            @CachedContext(SLLanguage.class) ContextReference<SLContext> contextRef,
                            @Cached("contextRef.get().getAllocationReporter()") AllocationReporter reporter,
                            @Cached("createWrappedPlayer(language, reporter, obj)") SLPlayer player) {
        // player is created once per specialization
        return player;
    }

    protected SLPlayer createWrappedPlayer(SLLanguage language, AllocationReporter reporter, SLObject object) {
        SLPlayer player = language.createPlayer(reporter, object);
        // Create libraries for accessing the different objects (wrapped object, player, object's roles)
        // NOTE: Cached libraries are not used, since they are cannot be reused for other specializations
        // and are only used once for the specialization that calls this function
        DynamicObjectLibrary playerLibrary = DynamicObjectLibrary.getFactory().getUncached(player);
        DynamicObjectLibrary objectLibrary = DynamicObjectLibrary.getFactory().getUncached(object);
        DynamicObjectLibrary roleLibrary = DynamicObjectLibrary.getUncached();
        player.wrap(playerLibrary, objectLibrary, roleLibrary);
        return player;
    }

    @Specialization(limit = "0")
    public Object doGeneric(Object obj) {
        // We cannot wrap objects that are not SLObject since we do not know how to access their roles
        // (if they have any). So we simply return the original object
        if (obj instanceof SLObject && ((SLObject)obj).roles.size() > 0) {
            throw new RuntimeException("What?");
        }
        return obj;
    }
}
