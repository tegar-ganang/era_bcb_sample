package org.enerj.util;

import java.util.*;

/**
 * Implements a multiple-reader/one writer (MROW) thread lock. The class is generally intended to enhance the 
 * standard Java thread synchronization (which provides only mutual exclusion). Lock owners are identified by threads. 
 * General notes:<p>
 * <ul>
 * <li> This class does not perform deadlock detection. To
 *      prevent deadlock, multiple MROWLocks should be obtained in the same order.
 * <li> No special precedence for those waiting on the lock.  E.g., there
 *      is no FIFO (first waiter is first locker) algorithm. The Java wait/notify
 *      and synchronization mechanism governs which waiter gets the lock next.
 * <li> Locks are reentrant -- a single thread can get the same read or write lock multiple times. 
 *      Matching release requests are required to release reentered locks.
 * <li> A reader may transition to a writer while still holding the read lock, given that 
 *      there are no other read locks. When the write lock is released, the lock immediately
 *      reverts to any read locks that are still held. To effectively release a read and
 *      write lock at the same time, release the read lock first, then the write lock.
 *      This allows multiple levels of method invocation to work transparently on the
 *      same lock.
 * </ul>
 *
 * @version $Id: MROWLock.java,v 1.4 2006/05/05 13:47:14 dsyrstad Exp $
 * @author <a href="mailto:dsyrstad@ener-j.org">Dan Syrstad</a>
 */
public class MROWLock {

    /** Map of read lockers. Key is the Thread, value is number of read locks held (reentries). 
     * We didn't use ThreadLocal for this because we also need to know the total size of
     * the map. ThreadLocal also contains more overhead, which we don't need because we
     * already provide enough of our own ;-).
     */
    private IdentityHashMap mReadLockers = new IdentityHashMap();

    /** If non-null, this indicates that a write lock is held and who owns it. */
    private Thread mWriteLockOwner = null;

    /** If mWriteLockOwner is non-null, this is the number of lock reentries. */
    private int mNumWriteLockReentries = 0;

    /**
     * Construct a MROWLock with no lock initially set.
     */
    public MROWLock() {
    }

    /**
     * Acquires (or reacquires) a read lock for this Thread. Suspends the thread if a 
     * write lock is currently active.
     */
    public synchronized void acquireRead() {
        while (mWriteLockOwner != null) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        Thread thread = Thread.currentThread();
        Counter counter = (Counter) mReadLockers.get(thread);
        if (counter == null) {
            counter = new Counter(1);
            mReadLockers.put(thread, counter);
        } else {
            counter.increment();
        }
    }

    /**
     * Releases a read lock. If the lock was acquired multiple times by
     * the same Thread, the read lock is kept, but the reentry count is 
     * decremented. When the reentry count reaches zero, the lock is released.
     *
     * @throws RuntimeException if a read lock is not held by the current thread.
     */
    public synchronized void releaseRead() {
        Thread thread = Thread.currentThread();
        Counter counter = (Counter) mReadLockers.get(thread);
        if (counter == null) {
            throw new RuntimeException("Attempted to release a read lock when one is not held by this thread.");
        }
        int count = counter.decrement();
        assert count >= 0;
        if (count == 0) {
            mReadLockers.remove(thread);
        }
        notifyAll();
    }

    /**
     * Acquires (or reacquires) a write lock for this Thread. Suspends the thread if one 
     * or more read or write locks are currently active. The one exception to this rule
     * is if the current thread is the only reader, the write is granted.
     */
    public synchronized void acquireWrite() {
        Thread thread = Thread.currentThread();
        if (mWriteLockOwner == thread) {
            ++mNumWriteLockReentries;
            return;
        }
        while (true) {
            if (mWriteLockOwner == null) {
                int numReaders = mReadLockers.size();
                if (numReaders == 0 || (numReaders == 1 && mReadLockers.get(thread) != null)) {
                    break;
                }
            }
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        mWriteLockOwner = thread;
        mNumWriteLockReentries = 1;
    }

    /**
     * Releases a write lock. If the lock was acquired multiple times by
     * the same Thread, the lock is kept, but the reentry count is 
     * decremented. When the reentry count reaches zero, the lock is released.
     *
     * @throws RuntimeException if a write lock is not held by the current thread.
     */
    public synchronized void releaseWrite() {
        Thread thread = Thread.currentThread();
        if (mWriteLockOwner != thread) {
            throw new RuntimeException("Attempted to release a write lock when one is not held by this thread.");
        }
        --mNumWriteLockReentries;
        assert mNumWriteLockReentries >= 0;
        if (mNumWriteLockReentries == 0) {
            mWriteLockOwner = null;
        }
        notifyAll();
    }

    /**
     * Returns true if one or more read locks are currently held by any thread.
     * Because of concurrency issues, if this returns false it doesn't mean that
     * a subsequent call to acquireRead/Write will immediately grant a lock.
     *
     * @return as described above.
     */
    public synchronized boolean isReadLocked() {
        return mReadLockers.size() != 0;
    }

    /**
     * Returns true if one or more read locks are currently held by any thread
     * other than this one.
     * Because of concurrency issues, if this returns false it doesn't mean that
     * a subsequent call to acquireRead/Write will immediately grant a lock.
     *
     * @return as described above.
     */
    public synchronized boolean isExternallyReadLocked() {
        Thread thread = Thread.currentThread();
        int numReaders = mReadLockers.size();
        return (numReaders > 0 && (numReaders > 1 || mReadLockers.get(thread) == null));
    }

    /**
     * Returns true if a read lock is currently held by this thread.
     * Because of concurrency issues, if this returns false it doesn't mean that
     * a subsequent call to acquireRead/Write will immediately grant a lock.
     *
     * @return as described above.
     */
    public synchronized boolean isLocallyReadLocked() {
        Thread thread = Thread.currentThread();
        return mReadLockers.get(thread) != null;
    }

    /**
     * Returns true if a write lock is currently held by any thread.
     * Because of concurrency issues, if this returns false it doesn't mean that
     * a subsequent call to acquireRead/Write will immediately grant a lock.
     *
     * @return as described above.
     */
    public synchronized boolean isWriteLocked() {
        return mWriteLockOwner != null;
    }

    /**
     * Returns true if a write lock is held by a thread other than this one.
     * Because of concurrency issues, if this returns false it doesn't mean that
     * a subsequent call to acquireRead/Write will immediately grant a lock.
     *
     * @return as described above.
     */
    public synchronized boolean isExternallyWriteLocked() {
        Thread thread = Thread.currentThread();
        return (mWriteLockOwner != null && mWriteLockOwner != Thread.currentThread());
    }

    /**
     * Returns true if a write lock is currently held by this thread.
     * Because of concurrency issues, if this returns false it doesn't mean that
     * a subsequent call to acquireRead/Write will immediately grant a lock.
     *
     * @return as described above.
     */
    public synchronized boolean isLocallyWriteLocked() {
        return mWriteLockOwner == Thread.currentThread();
    }

    /**
     * Returns true if a read or write lock is currently held by any thread.
     * Because of concurrency issues, if this returns false it doesn't mean that
     * a subsequent call to acquireRead/Write will immediately grant a lock.
     *
     * @return as described above.
     */
    public synchronized boolean isLocked() {
        return isReadLocked() || isWriteLocked();
    }

    /**
     * Returns true if a read or write lock is held by a thread other than this one.
     * Because of concurrency issues, if this returns false it doesn't mean that
     * a subsequent call to acquireRead/Write will immediately grant a lock.
     *
     * @return as described above.
     */
    public synchronized boolean isExternallyLocked() {
        return isExternallyReadLocked() || isExternallyWriteLocked();
    }

    /**
     * Returns true if a read or write lock is currently held by this thread.
     * Because of concurrency issues, if this returns false it doesn't mean that
     * a subsequent call to acquireRead/Write will immediately grant a lock.
     *
     * @return as described above.
     */
    public synchronized boolean isLocallyLocked() {
        return isLocallyReadLocked() || isLocallyWriteLocked();
    }

    /**
     * Provides an object that can be incremented and decremented.
     */
    private static final class Counter {

        private int mValue;

        Counter(int anInitialValue) {
            mValue = anInitialValue;
        }

        /**
         * Increment the counter and return the new value.
         */
        int increment() {
            return ++mValue;
        }

        /**
         * Decrement the counter and return the new value.
         */
        int decrement() {
            return --mValue;
        }

        /**
         * Returns the current value.
         */
        int getValue() {
            return mValue;
        }
    }
}
