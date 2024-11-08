package oracle.toplink.essentials.internal.identitymaps;

import java.util.*;

/**
 * <p><b>Purpose</b>: A FullIdentityMap holds all objects stored within it for the life of the application
 * <p><b>Responsibilities</b>:<ul>
 *    <li> Guarantees identity
 * <li> Holds all cached objects indefinetly.
 * </ul>
 * @since TOPLink/Java 1.0
 */
public class FullIdentityMap extends IdentityMap {

    /** Hashtable of CacheKeys stored using their key */
    protected Hashtable cacheKeys;

    public FullIdentityMap(int size) {
        super(size);
        cacheKeys = new Hashtable(size);
    }

    /**
     * INTERNAL:
     * Clones itself.
     */
    public Object clone() {
        FullIdentityMap clone = (FullIdentityMap) super.clone();
        clone.setCacheKeys(new Hashtable(getCacheKeys().size()));
        for (Enumeration cacheKeysEnum = getCacheKeys().elements(); cacheKeysEnum.hasMoreElements(); ) {
            CacheKey key = (CacheKey) ((CacheKey) cacheKeysEnum.nextElement()).clone();
            clone.getCacheKeys().put(key, key);
        }
        return clone;
    }

    /**
     * INTERNAL:
     * Used to print all the Locks in every identity map in this session.
     * The output of this method will go to log passed in as a parameter.
     */
    public void collectLocks(HashMap threadList) {
        Iterator cacheKeyIterator = this.cacheKeys.values().iterator();
        while (cacheKeyIterator.hasNext()) {
            CacheKey cacheKey = (CacheKey) cacheKeyIterator.next();
            if (cacheKey.isAcquired()) {
                Thread activeThread = cacheKey.getMutex().getActiveThread();
                Set set = (Set) threadList.get(activeThread);
                if (set == null) {
                    set = new HashSet();
                    threadList.put(activeThread, set);
                }
                set.add(cacheKey);
            }
        }
    }

    /**
     * Allow for the cache to be iterated on.
     */
    public Enumeration elements() {
        return new IdentityMapEnumeration(this);
    }

    /**
     *    Return the object indexed in the recevier at the cache key.
     *    If now object for the key exists, return null.
     *    @return a CacheKey for the primary key or null
     */
    protected synchronized CacheKey getCacheKey(CacheKey searchKey) {
        return (CacheKey) getCacheKeys().get(searchKey);
    }

    public Hashtable getCacheKeys() {
        return cacheKeys;
    }

    /**
     * Return the number of objects in the IdentityMap.
     */
    public int getSize() {
        return cacheKeys.size();
    }

    /**
     * Return the number of actual objects of type myClass in the IdentityMap.
     * Recurse = true will include subclasses of myClass in the count.
       */
    public int getSize(Class myClass, boolean recurse) {
        int i = 0;
        Enumeration keys = getCacheKeys().keys();
        while (keys.hasMoreElements()) {
            CacheKey key = (CacheKey) keys.nextElement();
            Object obj = key.getObject();
            if (obj != null) {
                if (recurse && myClass.isInstance(obj)) {
                    i++;
                } else if (obj.getClass().equals(myClass)) {
                    i++;
                }
            }
        }
        return i;
    }

    /**
     * Allow for the cache keys to be iterated on.
     */
    public Enumeration keys() {
        return new IdentityMapKeyEnumeration(this);
    }

    /**
     * Store the object in the cache at its primary key.
     * @param primaryKey is the primary key for the object.
     * @param object is the domain object to cache.
     * @param writeLockValue is the current write lock value of object, if null the version is ignored.
     */
    public CacheKey put(Vector primaryKey, Object object, Object writeLockValue, long readTime) {
        CacheKey cacheKey = getCacheKey(primaryKey);
        if (cacheKey != null) {
            resetCacheKey(cacheKey, object, writeLockValue);
            put(cacheKey);
        } else {
            cacheKey = createCacheKey(primaryKey, object, writeLockValue, readTime);
            put(cacheKey);
        }
        return cacheKey;
    }

    /**
     * Store the object in the cache with the cache key.
     */
    protected void put(CacheKey cacheKey) {
        synchronized (this) {
            getCacheKeys().put(cacheKey, cacheKey);
        }
        cacheKey.setOwningMap(this);
    }

    /**
     * Removes the CacheKey from the Hashtable.
     * @return The object held within the CacheKey or null if no object cached for given primaryKey
     */
    public Object remove(CacheKey cacheKey) {
        if (cacheKey != null) {
            cacheKey.acquire();
            synchronized (this) {
                getCacheKeys().remove(cacheKey);
            }
            cacheKey.release();
        } else {
            return null;
        }
        return cacheKey.getObject();
    }

    public void resetCacheKey(CacheKey key, Object object, Object writeLockValue) {
        resetCacheKey(key, object, writeLockValue, 0);
    }

    public void resetCacheKey(CacheKey key, Object object, Object writeLockValue, long readTime) {
        key.acquire();
        key.setObject(object);
        key.setWriteLockValue(writeLockValue);
        key.setReadTime(readTime);
        key.release();
    }

    protected void setCacheKeys(Hashtable cacheKeys) {
        this.cacheKeys = cacheKeys;
    }
}
