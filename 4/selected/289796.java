package org.datanucleus.util;

import org.datanucleus.ObjectManagerFactoryImpl;

/**
 * A simple read-write lock implementation. Multiple threads may lock using
 * readLock(), only one can lock using writeLock(). The caller is responsible
 * for coding a try-finally that ensures unlock() is called for every readLock()
 * and writeLock() call.
 * <p>
 * A ReadWriteLock is recursive; with one exception, a thread can re-lock an
 * object it already has locked. Multiple read locks can be acquired by the
 * same thread, as can multiple write locks. The exception however is that a
 * write lock cannot be acquired when a read lock is already held (to allow
 * this would cause deadlocks).
 * </p> 
 * <p>
 * Successive lock calls from the same thread must be matched by an
 * equal number of unlock() calls.
 * </p> 
 */
public class ReadWriteLock {

    private static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation", ObjectManagerFactoryImpl.class.getClassLoader());

    private static final int WAIT_LOG_INTERVAL = 5000;

    /** A count for each thread indicating the number of read locks it holds. */
    private ThreadLocal readLocksByThread;

    /** The number of read locks held across all threads. */
    private int readLocks;

    /** The number of write locks held (by writeLockedBy). */
    private int writeLocks;

    /** The thread holding the write lock(s), if any. */
    private Thread writeLockedBy;

    /**
     * An object holding a per-thread read-lock count.
     */
    private static class Count {

        /** count value **/
        public int value = 0;
    }

    /**
     * Constructs read-write lock.
     */
    public ReadWriteLock() {
        readLocksByThread = new ThreadLocal() {

            public Object initialValue() {
                return new Count();
            }
        };
        readLocks = 0;
        writeLocks = 0;
        writeLockedBy = null;
    }

    /**
     * Acquire a read lock.  The calling thread will be suspended until no other
     * thread holds a write lock.
     *
     * <p>If the calling thread already owns a write lock for the object a read
     * lock is immediately acquired.
     *
     * @exception InterruptedException
     *      If the thread is interrupted while attempting to acquire the lock.
     */
    public synchronized void readLock() throws InterruptedException {
        Thread me = Thread.currentThread();
        Count myReadLocks = (Count) readLocksByThread.get();
        if (writeLockedBy != me) {
            while (writeLocks > 0) {
                wait(WAIT_LOG_INTERVAL);
                if (writeLocks > 0) {
                    if (NucleusLogger.GENERAL.isDebugEnabled()) {
                        NucleusLogger.GENERAL.debug(LOCALISER.msg("030000", this), new InterruptedException());
                    }
                }
            }
        }
        ++readLocks;
        ++myReadLocks.value;
    }

    /**
     * Acquire a write lock. The calling thread will be suspended until no
     * other thread holds a read or write lock.
     *
     * <p>This method cannot be called if the thread already owns a read lock on
     * the same ReadWriteLock object, otherwise an
     * <code>IllegalStateException</code> is thrown.
     *
     * @exception IllegalStateException
     *      If the thread already holds a read lock on the same object.
     * @exception InterruptedException
     *      If the thread is interrupted while attempting to acquire the lock.
     */
    public synchronized void writeLock() throws InterruptedException {
        Thread me = Thread.currentThread();
        Count myReadLocks = (Count) readLocksByThread.get();
        if (myReadLocks.value > 0) {
            throw new IllegalStateException(LOCALISER.msg("030001"));
        }
        if (writeLockedBy != me) {
            while (writeLocks > 0 || readLocks > 0) {
                wait(WAIT_LOG_INTERVAL);
                if (writeLocks > 0 || readLocks > 0) {
                    if (NucleusLogger.GENERAL.isDebugEnabled()) {
                        NucleusLogger.GENERAL.debug(LOCALISER.msg("030002", this), new InterruptedException());
                    }
                }
            }
            writeLockedBy = me;
        }
        ++writeLocks;
    }

    /**
     * Release a read or write lock.  Must be called in a finally block after
     * acquiring a lock.
     */
    public synchronized void unlock() {
        Thread me = Thread.currentThread();
        Count myReadLocks = (Count) readLocksByThread.get();
        if (myReadLocks.value > 0) {
            --myReadLocks.value;
            --readLocks;
        } else if (writeLockedBy == me) {
            if (writeLocks > 0) {
                if (--writeLocks == 0) {
                    writeLockedBy = null;
                }
            }
        }
        notifyAll();
    }

    /**
     * Method to return this object as a String.
     * @return String version of this object.
     **/
    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append(": readLocks = ").append(readLocks).append(", writeLocks = ").append(writeLocks).append(", writeLockedBy = ").append(writeLockedBy);
        return s.toString();
    }
}
