package oracle.toplink.essentials.internal.identitymaps;

import java.util.*;

/**
 * <p><b>Purpose</b>: Provide the capability to not cache objects at all.
 * <p><b>Responsibilities</b>:<ul>
 * <li> Do nothing when an object is cached
 * </ul>
 *    @since TOPLink/Java 1.0
 */
public class NoIdentityMap extends IdentityMap {

    public NoIdentityMap(int size) {
        super(size);
    }

    /**
     *    locking for no identity.
     */
    public CacheKey acquire(Vector primaryKey) {
        CacheKey cacheKey = new CacheKey(primaryKey);
        cacheKey.acquire();
        return cacheKey;
    }

    /**
     * INTERNAL:
     * Used to print all the Locks in every identity map in this session.
     * The output of this method will go to log passed in as a parameter.
     */
    public void collectLocks(HashMap threadList) {
    }

    /**
     * Allow for the cache to be iterated on.
     */
    public Enumeration elements() {
        return new Vector(1).elements();
    }

    /**
     *    Return the object cached in the identity map
     *  Return null as no object is cached in the no IM.
     */
    public Object get(Vector primaryKey) {
        return null;
    }

    /**
     *    Return null since no objects are actually cached.
     */
    protected CacheKey getCacheKey(CacheKey searchKey) {
        return null;
    }

    /**
     *    @return 0 (zero)
     */
    public int getSize() {
        return 0;
    }

    /**
     * Return the number of actual objects of type myClass in the IdentityMap.
     * Recurse = true will include subclasses of myClass in the count.
     */
    public int getSize(Class myClass, boolean recurse) {
        return 0;
    }

    /**
     *    Get the write lock value from the cache key associated to the primarykey
     */
    public Object getWriteLockValue(Vector primaryKey) {
        return null;
    }

    /**
     * Allow for the cache keys to be iterated on.
     */
    public Enumeration keys() {
        return new Vector(1).elements();
    }

    /**
     * DO NOTHING.
     */
    public CacheKey put(Vector aVector, Object object, Object writeLockValue, long readTime) {
        return null;
    }

    /**
     * DO NOTHING
     */
    public void put(CacheKey key) {
        return;
    }

    /**
     * Do Nothing.
     * Return null, since no objects are cached.
     */
    public Object remove(Vector primaryKey) {
        return null;
    }

    /**
     * Do Nothing
     * Return null, since no objects are cached.
     */
    public Object remove(CacheKey searchKey) {
        return null;
    }

    public void setWriteLockValue(Vector primaryKey, Object writeLockValue) {
    }
}
