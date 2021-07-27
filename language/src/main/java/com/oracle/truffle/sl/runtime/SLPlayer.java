package com.oracle.truffle.sl.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Fallback;
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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@SuppressWarnings("static-method")
@ExportLibrary(InteropLibrary.class)
public final class SLPlayer extends DynamicObject implements TruffleObject {
    protected static final int CACHE_LIMIT = 3;

    protected final SLObject wrapped;

    public SLPlayer(Shape shape, SLObject toWrap) {
        super(shape);
        wrapped = toWrap;
    }

    public void wrap(DynamicObjectLibrary thisLibrary,
                     DynamicObjectLibrary wrapLibrary,
                     DynamicObjectLibrary roleLibrary) {
        for (Object key : wrapLibrary.getKeyArray(wrapped)) {
            thisLibrary.put(this, key, null);
        }
        // Map property names to the roles that contain the property
        // (Note: The current implementation of `writeMember` disallows directly mapping to the value)
        for (SLObject role : wrapped.roles) {
            for (Object key : roleLibrary.getKeyArray(role)) {
                thisLibrary.put(this, key, role);
            }
        }
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
        static TriState doSLObject(SLPlayer receiver, SLPlayer other) {
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
        return SLType.OBJECT;
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
    void removeMember(String member) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal,
                    @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
        // TODO: Check if update is needed
        return new PlayerKeys(objectLibrary.getKeyArray(this));
    }

    @ExportMessage(name = "isMemberReadable")
    @ExportMessage(name = "isMemberModifiable")
    @ExportMessage(name = "isMemberRemovable")
    boolean existsMember(String member,
                    @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
        // TODO: Check if update is needed
        return objectLibrary.containsKey(this, member);
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

    /**
     * {@link DynamicObjectLibrary} provides the polymorphic inline cache for reading properties.
     */
    @ExportMessage
    Object readMember(String name,
                      @CachedLibrary("this") DynamicObjectLibrary objectLibrary,
                      // NOTE: Cannot use @CachedLibrary("wrapped"), because:
                      // "@ExportMessage annotated nodes must only refer to static cache initializer methods or fields"
                      @CachedLibrary(limit = "CACHE_LIMIT") DynamicObjectLibrary wrappedLibrary,
                      @CachedLibrary(limit = "CACHE_LIMIT") DynamicObjectLibrary roleLibrary) throws UnknownIdentifierException {
        Object target = objectLibrary.getOrDefault(this, name, null);
        Object result = null;
        if (target instanceof SLObject) {
            /* Delegate to target role */
            result = roleLibrary.getOrDefault((SLObject) target, name, null);
        } else {
            /* Fall back to wrapped original object */
            result = wrappedLibrary.getOrDefault(wrapped, name, null);
        }

        if (result == null) {
            /* Property does not exist. */
            throw UnknownIdentifierException.create(name);
        }
        return result;
    }

    /**
     * {@link DynamicObjectLibrary} provides the polymorphic inline cache for writing properties.
     */
    @ExportMessage
    void writeMember(String name, Object value,
                     @CachedLibrary("this") DynamicObjectLibrary objectLibrary,
                     // NOTE: Cannot use @CachedLibrary("wrapped"), because:
                     // "@ExportMessage annotated nodes must only refer to static cache initializer methods or fields"
                     @CachedLibrary(limit = "CACHE_LIMIT") DynamicObjectLibrary wrappedLibrary,
                     @CachedLibrary(limit = "CACHE_LIMIT") DynamicObjectLibrary roleLibrary) {
        Object target = objectLibrary.getOrDefault(this, name, null);
        if (target instanceof SLObject) {
            /* Delegate to target role */
            roleLibrary.put((SLObject) target, name, value);
        }
        /* Fall back to wrapped original object */
        wrappedLibrary.put(wrapped, name, value);
    }
}
