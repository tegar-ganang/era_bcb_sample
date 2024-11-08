package gloodb.utils;

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of reentrant read / write lock which doesn't deadlock when upgrading a read lock into a write lock.
 * Instead an exception is thrown.
 *
 */
public class SafeReentrantReadWriteLock implements Serializable {

    private static final long serialVersionUID = -4351491406397150666L;

    private final ReentrantReadWriteLock lock;

    /**
	 * Creates a lock instance.
	 */
    public SafeReentrantReadWriteLock() {
        this.lock = new ReentrantReadWriteLock();
    }

    /**
	 * Creates a lock instance.
	 * @param fair Lock fairness (true = fair).
	 */
    public SafeReentrantReadWriteLock(boolean fair) {
        this.lock = new ReentrantReadWriteLock(fair);
    }

    /**
	 * Read locks this instance.
	 */
    public void readLock() {
        this.lock.readLock().lock();
    }

    /**
	 * Read unlocks this instance.
	 */
    public void readUnlock() {
        this.lock.readLock().unlock();
    }

    /**
	 * Write locks this instance. Throws an exception if the current thread has the lock read locked already.
	 */
    public void writeLock() {
        if (this.lock.getReadHoldCount() > 0) {
            throw new DeadLockException("The application is attempting to write lock during a read only operation and will deadlock.\n" + "This problem can occur when trying to write from a read only query. Use unguarded or update queries instead. ");
        }
        this.lock.writeLock().lock();
    }

    /**
	 * Write unlocks this instance.
	 */
    public void writeUnlock() {
        if (this.lock.getReadHoldCount() > 0) return;
        this.lock.writeLock().unlock();
    }

    /**
	 * Returns true if the lock is in use.
	 * @return True if the lock is in use.
	 */
    public boolean isInUse() {
        return this.lock.getQueueLength() > 0 || this.lock.getReadLockCount() > 0 || this.lock.isWriteLocked();
    }

    /**
	 * Returns true if this instance is read locked from current thread.
	 * @return True if this instance is read locked from current thread.
	 */
    public boolean isReadLockedByCurrentThread() {
        return this.lock.getReadHoldCount() > 0;
    }

    /**
	 * Returns true if this instance is write locked from current thread.
	 * @return True if this instance is write locked from current thread.
	 */
    public boolean isWriteLockedByCurrentThread() {
        return this.lock.isWriteLockedByCurrentThread();
    }
}
