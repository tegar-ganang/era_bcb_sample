package org.exist.util;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * A reentrant read/write lock, which allows multiple readers to acquire a lock.
 * Waiting writers are preferred.
 * 
 * This is an adapted and bug-fixed version of code taken from Apache's Turbine
 * JCS.
 *  
 */
public class MultiReadReentrantLock implements Lock {

    private static final Logger log = Logger.getLogger(MultiReadReentrantLock.class);

    /** Number of threads waiting to read. */
    private int waitingForReadLock = 0;

    /** Number of threads reading. */
    private int outstandingReadLocks = 0;

    /** The thread that has the write lock or null. */
    private Thread writeLockedThread;

    /**
     * The number of (nested) write locks that have been requested from
     * writeLockedThread.
     */
    private int outstandingWriteLocks = 0;

    /**
     * Threads waiting to get a write lock are tracked in this ArrayList to
     * ensure that write locks are issued in the same order they are requested.
     */
    private List waitingForWriteLock = null;

    /** Default constructor. */
    public MultiReadReentrantLock() {
    }

    public boolean acquire() throws LockException {
        return acquire(Lock.READ_LOCK);
    }

    public boolean acquire(int mode) throws LockException {
        switch(mode) {
            case Lock.WRITE_LOCK:
                return writeLock();
            default:
                return readLock();
        }
    }

    public boolean attempt(int mode) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Issue a read lock if there is no outstanding write lock or threads
     * waiting to get a write lock. Caller of this method must be careful to
     * avoid synchronizing the calling code so as to avoid deadlock.
     */
    private synchronized boolean readLock() throws LockException {
        if (writeLockedThread == Thread.currentThread()) {
            outstandingReadLocks++;
            return true;
        }
        waitingForReadLock++;
        while (writeLockedThread != null) {
            try {
                wait(100);
            } catch (InterruptedException e) {
                throw new LockException("Interrupted while waiting for read lock");
            }
        }
        waitingForReadLock--;
        outstandingReadLocks++;
        return true;
    }

    /**
     * Issue a write lock if there are no outstanding read or write locks.
     * Caller of this method must be careful to avoid synchronizing the calling
     * code so as to avoid deadlock.
     */
    private boolean writeLock() throws LockException {
        Thread thisThread = Thread.currentThread();
        synchronized (this) {
            if (writeLockedThread == null && outstandingReadLocks == 0) {
                writeLockedThread = Thread.currentThread();
                outstandingWriteLocks++;
                return true;
            }
            if (waitingForWriteLock == null) waitingForWriteLock = new ArrayList(3);
            waitingForWriteLock.add(thisThread);
        }
        synchronized (thisThread) {
            while (thisThread != writeLockedThread) {
                try {
                    thisThread.wait();
                } catch (InterruptedException e) {
                    throw new LockException("Interrupted");
                }
            }
            outstandingWriteLocks++;
        }
        synchronized (this) {
            int i = waitingForWriteLock.indexOf(thisThread);
            waitingForWriteLock.remove(i);
        }
        return true;
    }

    public void release() {
        release(Lock.READ_LOCK);
    }

    public void release(int mode) {
        switch(mode) {
            case Lock.WRITE_LOCK:
                releaseWrite();
                break;
            default:
                releaseRead();
                break;
        }
    }

    private synchronized void releaseWrite() {
        if (Thread.currentThread() == writeLockedThread) {
            if (outstandingWriteLocks > 0) outstandingWriteLocks--;
            if (outstandingWriteLocks > 0) {
                return;
            }
            if (outstandingReadLocks == 0 && waitingForWriteLock != null && waitingForWriteLock.size() > 0) {
                writeLockedThread = (Thread) waitingForWriteLock.get(0);
                synchronized (writeLockedThread) {
                    writeLockedThread.notify();
                }
            } else {
                writeLockedThread = null;
                if (waitingForReadLock > 0) {
                    notifyAll();
                } else {
                }
            }
        } else {
            log.warn("Illegal lock usage: thread does not hold the write lock");
            throw new IllegalStateException("Thread does not have lock");
        }
    }

    /**
     * Threads call this method to relinquish a lock that they previously got
     * from this object.
     * 
     * @throws IllegalStateException
     *                   if called when there are no outstanding locks or there is a
     *                   write lock issued to a different thread.
     */
    private synchronized void releaseRead() {
        if (outstandingReadLocks > 0) {
            outstandingReadLocks--;
            if (outstandingReadLocks == 0 && writeLockedThread == null && waitingForWriteLock != null && waitingForWriteLock.size() > 0) {
                writeLockedThread = (Thread) waitingForWriteLock.get(0);
                synchronized (writeLockedThread) {
                    writeLockedThread.notifyAll();
                }
            }
            return;
        } else throw new IllegalStateException("Attempt to release a non-existing read lock.");
    }

    public synchronized boolean isLockedForWrite() {
        return writeLockedThread != null || (waitingForWriteLock != null && waitingForWriteLock.size() > 0);
    }
}
