package com.oracle.truffle.sl.runtime;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.utilities.TriState;
import com.oracle.truffle.sl.SLLanguage;

import java.util.*;

@SuppressWarnings("static-method")
@ExportLibrary(InteropLibrary.class)
public final class SLPlayer implements TruffleObject {
    protected static final int CACHE_LIMIT = 3;

    protected final SLObject wrapped;

    public SLPlayer(SLObject toWrap) {
        wrapped = toWrap;
    }

    @ExportMessage
    boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    Class<? extends TruffleLanguage<?>> getLanguage() {
        return SLLanguage.class;
    }

    @ExportMessage
    @SuppressWarnings("unused")
    static final class IsIdenticalOrUndefined {
        @Specialization
        static TriState doSLPlayer(SLPlayer receiver, SLPlayer other) {
            return TriState.valueOf(receiver == other);
        }

        @Fallback
        static TriState doOther(SLPlayer receiver, Object other) {
            return TriState.UNDEFINED;
        }
    }

    @ExportMessage
    @TruffleBoundary
    int identityHashCode() {
        return System.identityHashCode(this);
    }

    @ExportMessage
    boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    Object getMetaObject() {
        return SLType.PLAYER;
    }

    @ExportMessage
    @TruffleBoundary
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return "Player";
    }

    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    boolean isMemberRemovable(String member) {
        return false;
    }

    @ExportMessage
    void removeMember(String member) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal,
                      @CachedLibrary(limit="CACHE_LIMIT") DynamicObjectLibrary roleLibrary,
                      @CachedLibrary("this.wrapped") DynamicObjectLibrary wrappedLibrary)
            throws UnsupportedMessageException {
        LinkedHashSet<Object> allKeys = new LinkedHashSet<>();
        allKeys.addAll(Arrays.asList(wrappedLibrary.getKeyArray(wrapped)));
        for (SLObject role : wrapped.roles) {
            allKeys.addAll(Arrays.asList(roleLibrary.getKeyArray(role)));
        }
        return new PlayerKeys(allKeys.toArray());
    }

    @ExportMessage(name = "isMemberReadable")
    @ExportMessage(name = "isMemberModifiable")
    boolean existsMember(String member,
                         @CachedLibrary(limit="CACHE_LIMIT") InteropLibrary roleLibrary) {
        return lookupTarget(member, roleLibrary) != null;
    }

    @ExportMessage
    boolean isMemberInsertable(String member,
                    @CachedLibrary("this") InteropLibrary receivers) {
        return !receivers.isMemberExisting(this, member);
    }

    @ExportLibrary(InteropLibrary.class)
    static final class PlayerKeys implements TruffleObject {

        private final Object[] keys;

        PlayerKeys(Object[] keys) {
            this.keys = keys;
        }

        @ExportMessage
        Object readArrayElement(long index) throws InvalidArrayIndexException {
            if (!isArrayElementReadable(index)) {
                throw InvalidArrayIndexException.create(index);
            }
            return keys[(int) index];
        }

        @ExportMessage
        boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        long getArraySize() {
            return keys.length;
        }

        @ExportMessage
        boolean isArrayElementReadable(long index) {
            return index >= 0 && index < keys.length;
        }
    }

    Object lookupTarget(String name, InteropLibrary roleLibrary) {
        for (int i = wrapped.roles.length - 1; i >= 0; i--) {
            Object role = wrapped.roles[i];
            if (roleLibrary.isMemberExisting(role, name)) {
                return role;
            }
        }
        return wrapped;
    }

    @ExportMessage
    abstract static class ReadMember {
        @Specialization(guards = "name == cachedName", assumptions = "rolesUnchanged")
        protected static Object doCached(SLPlayer player, String name,
                                         @Cached("name") String cachedName,
                                         @Cached("player.wrapped.rolesUnchanged.getAssumption()") Assumption rolesUnchanged,
                                         @CachedLibrary(limit = "CACHE_LIMIT") InteropLibrary roleLibrary,
                                         @Cached("player.lookupTarget(name, roleLibrary)") Object target,
                                         @CachedLibrary("target") InteropLibrary targetLibrary)
                throws UnsupportedMessageException, UnknownIdentifierException {
            return targetLibrary.readMember(target, name);
        }

        @Specialization(replaces = "doCached")
        protected static Object doUncached(SLPlayer player, String name,
                                           @CachedLibrary(limit = "3") InteropLibrary targetLibrary)
                throws UnsupportedMessageException, UnknownIdentifierException {
            Object target = player.lookupTarget(name, targetLibrary);
            return targetLibrary.readMember(target, name);
        }
    }

    /**
     * {@link DynamicObjectLibrary} provides the polymorphic inline cache for writing properties.
     */
    @ExportMessage
    void writeMember(String name, Object value,
                     @CachedLibrary("this.wrapped") DynamicObjectLibrary wrappedLibrary,
                     @CachedLibrary(limit = "CACHE_LIMIT") InteropLibrary roleLibrary)
            throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        Object target = lookupTarget(name, roleLibrary);
        if (target instanceof SLObject) {
            /* Delegate to target role */
            roleLibrary.writeMember(target, name, value);
        }
        /* Fall back to wrapped original object */
        wrappedLibrary.put(wrapped, name, value);
    }
}
