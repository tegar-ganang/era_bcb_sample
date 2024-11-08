package oracle.toplink.essentials.internal.identitymaps;

import java.util.*;

/**
 *    <p><b>Purpose</b>: Provides the capability to insert CacheKeys into a Linked List.
 *    <p><b>Responsibilities</b>:<ul>
 *    <li> Provide same cabailities as superclass.
 *    <li> Maintain within linked list.
 *    </ul>
 *    @see CacheIdentityMap
 *    @since TOPLink/Java 1.0
 */
public class LinkedCacheKey extends CacheKey {

    /** Handle on previos element in cache */
    protected LinkedCacheKey previous;

    /** Handle on next element in cache */
    protected LinkedCacheKey next;

    /**
     *    Initialize the newly allocated instance of this class.
     *    @param object is the domain object.
     *    @param writeLockValue is the write lock value number.
     */
    public LinkedCacheKey(Vector primaryKey, Object object, Object writeLockValue, long readTime) {
        super(primaryKey, object, writeLockValue, readTime);
    }

    public LinkedCacheKey getNext() {
        return next;
    }

    public LinkedCacheKey getPrevious() {
        return previous;
    }

    public void setNext(LinkedCacheKey next) {
        this.next = next;
    }

    public void setPrevious(LinkedCacheKey previous) {
        this.previous = previous;
    }
}
