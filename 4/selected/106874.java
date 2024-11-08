package oracle.toplink.essentials.internal.identitymaps;

import java.util.*;

/**
 * <p><b>Purpose</b>: A WeakIdentityMap holds all objects referenced by the application only.
 * The weak identity map is similar to the full identity map except for the fact that it allows
 * full garbage collection.
 * <p><b>Responsibilities</b>:<ul>
 * <li> Guarantees identity
 * <li> Allows garbage collection
 * </ul>
 *    @since TOPLink/Java 1.0
 */
public class WeakIdentityMap extends FullIdentityMap {

    /** Keep track of a counter to amortize cleanup of dead cache keys */
    protected int cleanupCount;

    /** PERF: Keep track of a cleanup size to avoid cleanup bottleneck for large caches. */
    protected int cleanupSize;

    public WeakIdentityMap(int size) {
        super(size);
        this.cleanupCount = 0;
        this.cleanupSize = size;
    }

    /**
     * Search for any cache keys that have been garbage collected and remove them.
     * This must be done because allthough the objects held by the cache keys will garbage collect,
     * the keys themselves will not and must be cleaned up.  This is a linear opperation so
     * is amortized through the cleanupCount to occur only once per cycle avergaing to make
     * the total time still constant.
     */
    protected void cleanupDeadCacheKeys() {
        for (Enumeration keysEnum = getCacheKeys().elements(); keysEnum.hasMoreElements(); ) {
            CacheKey key = (CacheKey) keysEnum.nextElement();
            if (key.getObject() == null) {
                if (key.acquireNoWait()) {
                    try {
                        if (key.getObject() == null) {
                            getCacheKeys().remove(key);
                        }
                    } finally {
                        key.release();
                    }
                }
            }
        }
    }

    public CacheKey createCacheKey(Vector primaryKey, Object object, Object writeLockValue, long readTime) {
        return new WeakCacheKey(primaryKey, object, writeLockValue, readTime);
    }

    /**
     * Used to amortized the cleanup of dead cache keys.
     */
    protected int getCleanupCount() {
        return cleanupCount;
    }

    protected void setCleanupCount(int cleanupCount) {
        this.cleanupCount = cleanupCount;
    }

    /**
     * Used to amortized the cleanup of dead cache keys.
     */
    protected int getCleanupSize() {
        return cleanupSize;
    }

    protected void setCleanupSize(int cleanupSize) {
        this.cleanupSize = cleanupSize;
    }

    /**
     * Store the object in the cache with the cache key.
     */
    protected void put(CacheKey cacheKey) {
        synchronized (this) {
            if (getCleanupCount() > getCleanupSize()) {
                cleanupDeadCacheKeys();
                setCleanupCount(0);
                if (getSize() > getCleanupSize()) {
                    setCleanupSize(getSize());
                }
            }
            setCleanupCount(getCleanupCount() + 1);
        }
        super.put(cacheKey);
    }
}
