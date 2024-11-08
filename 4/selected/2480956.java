package com.workplacesystems.queuj.utils.collections;

import com.workplacesystems.queuj.utils.Callback;
import com.workplacesystems.queuj.utils.Condition;
import com.workplacesystems.queuj.utils.QueujException;

/**
 *
 * @author dave
 */
abstract class SyncUtilsLegacy extends SyncUtils {

    /**
     * Creates a new instance of SyncUtilsLegacy
     */
    SyncUtilsLegacy() {
    }

    @Override
    Object createMutexImpl(Object suggested_mutex) {
        if (suggested_mutex == null) return new Object();
        return getObjectToLock(suggested_mutex);
    }

    @Override
    <T> T synchronizeWriteImpl(Object mutex, Callback<T> callback, Callback<?> release_callback) {
        if (release_callback != null) throw new QueujException("release_callback cannot be used with legacy sync");
        synchronized (mutex) {
            return callback.action();
        }
    }

    @Override
    <T> T synchronizeReadImpl(Object mutex, Callback<T> callback, Callback<?> release_callback) {
        if (release_callback != null) throw new QueujException("release_callback cannot be used with legacy sync");
        synchronized (mutex) {
            return callback.action();
        }
    }

    @Override
    <T> T synchronizeWriteThenReadImpl(Object mutex, Callback<?> write_callback, Callback<T> read_callback, Callback<?> release_callback) {
        if (release_callback != null) throw new QueujException("release_callback cannot be used with legacy sync");
        synchronized (mutex) {
            write_callback.action();
            return read_callback.action();
        }
    }

    @Override
    <T> T synchronizeConditionalWriteImpl(Object mutex, Condition write_condition, Callback<T> write_callback, Callback<?> release_callback) {
        if (release_callback != null) throw new QueujException("release_callback cannot be used with legacy sync");
        synchronized (mutex) {
            if (write_condition.isTrue(0)) return write_callback.action();
            return null;
        }
    }

    @Override
    <T> T synchronizeConditionalWriteThenReadImpl(Object mutex, Condition write_condition, Callback<?> write_callback, Callback<T> read_callback, Callback<?> release_callback) {
        if (release_callback != null) throw new QueujException("release_callback cannot be used with legacy sync");
        synchronized (mutex) {
            if (write_condition.isTrue(0)) write_callback.action();
            return read_callback.action();
        }
    }
}
