package oracle.toplink.essentials.internal.identitymaps;

import java.util.*;
import oracle.toplink.essentials.exceptions.*;

/**
 * <p><b>Purpose</b>: A fixed size LRU cache<p>
 *    Using a linked list as well as the hashtable from the superclass a LRU cache is maintained.
 * When a get is executed the LRU list is updated and when a new object is inserted the object
 *    at the start of the list is deleted (provided the maxSize has been reached).
 * <p><b>Responsibilities</b>:<ul>
  *    <li> Guarantees identity through primary key values
 * <li> Keeps the LRU linked list updated.
 * </ul>
 * @since TOPLink/Java 1.0
 */
public class CacheIdentityMap extends FullIdentityMap {

    /** Provide handles on the linked list */
    protected LinkedCacheKey first;

    /** Provide handles on the linked list */
    protected LinkedCacheKey last;

    /**
     * Initialize newly instantiated CacheIdentityMap.
     * @param size int The size of the Cache
     */
    public CacheIdentityMap(int size) {
        super(size);
        this.first = new LinkedCacheKey(new Vector(2), null, null, 0);
        this.last = new LinkedCacheKey(new Vector(2), null, null, 0);
        this.first.setNext(this.last);
        this.last.setPrevious(this.first);
    }

    public CacheKey createCacheKey(Vector primaryKey, Object object, Object writeLockValue, long readTime) {
        return new LinkedCacheKey(primaryKey, object, writeLockValue, readTime);
    }

    /**
     * Reduces the size of the receiver down to the maxSize removing objects from the
     * start of the linked list.
     */
    protected void ensureFixedSize() {
        synchronized (this.first) {
            while (getMaxSize() > 0 && getSize() > getMaxSize()) {
                remove(last.getPrevious());
            }
        }
    }

    /**
     *    Access the object within the table for the given primaryKey.
     *    Move the accessed key to the top of the order keys linked list to maintain LRU.
     *    @param aVector is the primary key for the object to search for.
     *    @return The LinkedCacheKey or null if none found for primaryKey
     */
    protected CacheKey getCacheKey(Vector primaryKeys) {
        LinkedCacheKey cacheKey = (LinkedCacheKey) super.getCacheKey(primaryKeys);
        if (cacheKey != null) {
            synchronized (this.first) {
                removeLink(cacheKey);
                insertLink(cacheKey);
            }
        }
        return cacheKey;
    }

    /**
     *    Insert a new element into the linked list of LinkedCacheKeys.
     *    New elements (Recently Used) are added at the end (last).
     *    @return The added LinkedCacheKey
     */
    protected LinkedCacheKey insertLink(LinkedCacheKey key) {
        if (key == null) {
            return key;
        }
        synchronized (this.first) {
            this.first.getNext().setPrevious(key);
            key.setNext(this.first.getNext());
            key.setPrevious(this.first);
            this.first.setNext(key);
        }
        return key;
    }

    /**
     *  Store the object in the identity map with the linked cache key
     */
    protected void put(CacheKey cacheKey) {
        super.put(cacheKey);
        insertLink((LinkedCacheKey) cacheKey);
        ensureFixedSize();
    }

    /**
     * Remove the LinkedCacheKey from the cache as well as from the linked list.
     * @return The LinkedCacheKey to be removed
     */
    public Object remove(CacheKey key) {
        super.remove(key);
        if (key == null) {
            Class cacheItemClass = null;
            if (!getCacheKeys().isEmpty()) {
                CacheKey aKey = (CacheKey) getCacheKeys().keys().nextElement();
                if ((aKey != null) && (aKey.getObject() != null)) {
                    cacheItemClass = aKey.getObject().getClass();
                }
            }
            throw ValidationException.nullCacheKeyFoundOnRemoval(this, cacheItemClass);
        }
        return removeLink((LinkedCacheKey) key).getObject();
    }

    /**
     * Remove the LinkedCacheKey from the linked list.
     * @return The removed LinkedCacheKey
     */
    protected LinkedCacheKey removeLink(LinkedCacheKey key) {
        if (key == null) {
            return key;
        }
        synchronized (this.first) {
            if (key.getPrevious() == null || key.getNext() == null) {
                return key;
            }
            key.getPrevious().setNext(key.getNext());
            key.getNext().setPrevious(key.getPrevious());
            key.setNext(null);
            key.setPrevious(null);
        }
        return key;
    }

    /**
     * INTERNAL:
     *        This method will be used to update the max cache size, any objects exceeding the max cache size will
     * be remove from the cache. Please note that this does not remove the object from the identityMap, except in
     * the case of the CacheIdentityMap.
     */
    public synchronized void updateMaxSize(int maxSize) {
        setMaxSize(maxSize);
        ensureFixedSize();
    }
}
