package com.oracle.truffle.sl.builtins;

import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.CachedLanguage;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.sl.SLLanguage;
import com.oracle.truffle.sl.runtime.SLContext;
import com.oracle.truffle.sl.runtime.SLObject;

@NodeInfo(shortName = "play")
public abstract class SLPlayBuiltin extends SLBuiltinNode {

    @Specialization(guards = { "obj != null", "role != null" })
    public Object newPlayer(SLObject obj, SLObject role) {
        obj.playRole(role);
        return obj;
    }
}
