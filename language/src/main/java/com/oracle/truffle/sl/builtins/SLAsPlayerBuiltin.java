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

    @Specialization(guards = "obj != null")
    public Object newPlayer(SLObject obj,
                            @CachedLanguage SLLanguage language,
                            @CachedContext(SLLanguage.class) ContextReference<SLContext> contextRef,
                            @Cached("contextRef.get().getAllocationReporter()") AllocationReporter reporter) {
        // player is created once per specialization
        return language.createPlayer(reporter, obj);
    }

    @Specialization
    public Object doGeneric(Object obj) {
        // We cannot wrap objects that are not SLObject since we do not know how to access their roles
        // (if they have any). So we simply return the original object
        if (obj instanceof SLObject) {
            throw new RuntimeException("What?");
        }
        return obj;
    }
}
