package com.workplacesystems.queuj.utils.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.workplacesystems.queuj.utils.Callback;
import com.workplacesystems.queuj.utils.Condition;
import com.workplacesystems.queuj.utils.QueujException;
import com.workplacesystems.queuj.utils.collections.decorators.SynchronizedDecorator;

/**
 *
 * @author  dave
 */
public abstract class SyncUtils {

    private static final SyncUtils sync_utils_instance;

    static {
        SyncUtils local_sync_util;
        boolean java16 = false;
        try {
            Class.forName("java.util.ArrayDeque");
            java16 = true;
        } catch (ClassNotFoundException cnfe) {
        }
        if (java16) {
            try {
                Class<?> sync_jdk16_class = Class.forName("com.workplacesystems.queuj.utils.collections.SyncUtilsJdk16");
                local_sync_util = (SyncUtils) sync_jdk16_class.newInstance();
            } catch (Exception e) {
                new QueujException("JDK 1.6 was detected but SyncUtilsJdk16 class cannot be found.");
                local_sync_util = new SyncUtilsReentrant();
            }
        } else local_sync_util = new SyncUtilsReentrant();
        sync_utils_instance = local_sync_util;
    }

    /** Creates a new instance of SyncUtils */
    SyncUtils() {
    }

    public static final Object getLockObject(final Map<?, ?> map) {
        return getObjectToLock(map);
    }

    public static final Object getLockObject(final Collection<?> col) {
        return getObjectToLock(col);
    }

    protected static final Object getObjectToLock(final Object obj) {
        if (obj == null) return createMutex(null);
        if (obj instanceof SynchronizedDecorator) {
            SynchronizedDecorator sd = (SynchronizedDecorator) obj;
            return sd.getLockObject();
        }
        return obj;
    }

    public abstract static class SyncList {

        private final List<Object> objects_to_lock = new ArrayList<Object>();

        private final List<Object> locked = new ArrayList<Object>();

        SyncList() {
        }

        public void addObjectToLock(Object obj) {
            Object mutex = getObjectToLock(obj);
            if (!objects_to_lock.contains(mutex)) objects_to_lock.add(mutex);
        }

        void lockAll() {
            if (!locked.isEmpty()) throw new IllegalStateException("lockAll already called");
            boolean all_locked = false;
            while (!all_locked) {
                all_locked = true;
                for (Object mutex : objects_to_lock) {
                    if (tryLock(mutex)) locked.add(mutex); else {
                        unlockAll();
                        all_locked = false;
                        try {
                            Thread.sleep((long) (100L + Math.random() * 400L));
                        } catch (InterruptedException e) {
                        }
                        break;
                    }
                }
            }
        }

        abstract boolean tryLock(Object mutex);

        void unlockAll() {
            QueujException ce = null;
            for (Iterator<Object> i = locked.iterator(); i.hasNext(); ) {
                Object mutex = i.next();
                try {
                    unlock(mutex);
                } catch (Exception e) {
                    QueujException _ce = new QueujException(e);
                    if (ce == null) ce = _ce;
                }
                i.remove();
            }
            if (ce != null) throw ce;
        }

        abstract void unlock(Object mutex);
    }

    public static SyncList getNewSyncList() {
        return sync_utils_instance.getNewSyncListImpl();
    }

    public static final Object createMutex(Object suggested_mutex) {
        return sync_utils_instance.createMutexImpl(suggested_mutex);
    }

    public static final <T> T synchronizeWrite(Object mutex, Callback<T> callback) {
        return sync_utils_instance.synchronizeWriteImpl(getObjectToLock(mutex), callback, null);
    }

    public static final <T> T synchronizeWrite(Object mutex, Callback<T> callback, Callback<?> release_callback) {
        return sync_utils_instance.synchronizeWriteImpl(getObjectToLock(mutex), callback, release_callback);
    }

    public static final <T> T synchronizeRead(Object mutex, Callback<T> callback) {
        return sync_utils_instance.synchronizeReadImpl(getObjectToLock(mutex), callback, null);
    }

    public static final <T> T synchronizeRead(Object mutex, Callback<T> callback, Callback<?> release_callback) {
        return sync_utils_instance.synchronizeReadImpl(getObjectToLock(mutex), callback, release_callback);
    }

    public static <T> T synchronizeWriteThenRead(Object mutex, Callback<?> write_callback, Callback<T> read_callback) {
        return sync_utils_instance.synchronizeWriteThenReadImpl(getObjectToLock(mutex), write_callback, read_callback, null);
    }

    public static <T> T synchronizeWriteThenRead(Object mutex, Callback<?> write_callback, Callback<T> read_callback, Callback<?> release_callback) {
        return sync_utils_instance.synchronizeWriteThenReadImpl(getObjectToLock(mutex), write_callback, read_callback, release_callback);
    }

    public static <T> T synchronizeConditionalWrite(Object mutex, Condition write_condition, Callback<T> write_callback) {
        return sync_utils_instance.synchronizeConditionalWriteImpl(getObjectToLock(mutex), write_condition, write_callback, null);
    }

    public static <T> T synchronizeConditionalWrite(Object mutex, Condition write_condition, Callback<T> write_callback, Callback<?> release_callback) {
        return sync_utils_instance.synchronizeConditionalWriteImpl(getObjectToLock(mutex), write_condition, write_callback, release_callback);
    }

    public static <T> T synchronizeConditionalWriteThenRead(Object mutex, Condition write_condition, Callback<?> write_callback, Callback<T> read_callback) {
        return sync_utils_instance.synchronizeConditionalWriteThenReadImpl(getObjectToLock(mutex), write_condition, write_callback, read_callback, null);
    }

    public static <T> T synchronizeConditionalWriteThenRead(Object mutex, Condition write_condition, Callback<?> write_callback, Callback<T> read_callback, Callback<?> release_callback) {
        return sync_utils_instance.synchronizeConditionalWriteThenReadImpl(getObjectToLock(mutex), write_condition, write_callback, read_callback, release_callback);
    }

    abstract SyncList getNewSyncListImpl();

    abstract Object createMutexImpl(Object suggested_mutex);

    abstract <T> T synchronizeWriteImpl(Object mutex, Callback<T> callback, Callback<?> release_callback);

    abstract <T> T synchronizeReadImpl(Object mutex, Callback<T> callback, Callback<?> release_callback);

    abstract <T> T synchronizeWriteThenReadImpl(Object mutex, Callback<?> write_callback, Callback<T> read_callback, Callback<?> release_callback);

    abstract <T> T synchronizeConditionalWriteThenReadImpl(Object mutex, Condition write_condition, Callback<?> write_callback, Callback<T> read_callback, Callback<?> release_callback);

    abstract <T> T synchronizeConditionalWriteImpl(Object mutex, Condition write_condition, Callback<T> write_callback, Callback<?> release_callback);
}
