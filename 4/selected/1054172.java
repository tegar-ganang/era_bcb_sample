package org.exist.storage.lock;

import org.apache.log4j.Logger;
import org.exist.util.DeadlockException;
import org.exist.util.LockException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A reentrant read/write lock, which allows multiple readers to acquire a lock.
 * Waiting writers are preferred.
 * <p/>
 * This is an adapted and bug-fixed version of code taken from Apache's Turbine
 * JCS.
 */
public class MultiReadReentrantLock implements Lock {

    private static final Logger LOG = Logger.getLogger(MultiReadReentrantLock.class);

    private Object id;

    /**
     * Number of threads waiting to read.
     */
    private int waitingForReadLock = 0;

    /**
     * Number of threads reading.
     */
    private List<LockOwner> outstandingReadLocks = new ArrayList<LockOwner>(4);

    /**
     * The thread that has the write lock or null.
     */
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
    private List<WaitingThread> waitingForWriteLock = null;

    /**
     * Default constructor.
     */
    public MultiReadReentrantLock(Object id) {
        this.id = id;
    }

    public String getId() {
        return id.toString();
    }

    public boolean acquire() throws LockException {
        return acquire(Lock.READ_LOCK);
    }

    public boolean acquire(int mode) throws LockException {
        if (mode == Lock.NO_LOCK) {
            LOG.warn("acquired with no lock !");
            return true;
        }
        switch(mode) {
            case Lock.WRITE_LOCK:
                return writeLock(true);
            default:
                return readLock(true);
        }
    }

    public boolean attempt(int mode) {
        try {
            switch(mode) {
                case Lock.WRITE_LOCK:
                    return writeLock(false);
                default:
                    return readLock(false);
            }
        } catch (LockException e) {
            return false;
        }
    }

    /**
     * Issue a read lock if there is no outstanding write lock or threads
     * waiting to get a write lock. Caller of this method must be careful to
     * avoid synchronizing the calling code so as to avoid deadlock.
    * @param waitIfNecessary whether to wait if the lock is not available right away
     */
    private synchronized boolean readLock(boolean waitIfNecessary) throws LockException {
        final Thread thisThread = Thread.currentThread();
        if (writeLockedThread == thisThread) {
            outstandingReadLocks.add(new LockOwner(thisThread));
            return true;
        }
        deadlockCheck();
        waitingForReadLock++;
        if (writeLockedThread != null) {
            if (!waitIfNecessary) return false;
            WaitingThread waiter = new WaitingThread(thisThread, this, this, Lock.READ_LOCK);
            DeadlockDetection.addResourceWaiter(thisThread, waiter);
            while (writeLockedThread != null) {
                waiter.doWait();
            }
            DeadlockDetection.clearResourceWaiter(thisThread);
        }
        waitingForReadLock--;
        outstandingReadLocks.add(new LockOwner(thisThread));
        return true;
    }

    /**
     * Issue a write lock if there are no outstanding read or write locks.
     * Caller of this method must be careful to avoid synchronizing the calling
     * code so as to avoid deadlock.
    * @param waitIfNecessary whether to wait if the lock is not available right away
     */
    private boolean writeLock(boolean waitIfNecessary) throws LockException {
        Thread thisThread = Thread.currentThread();
        WaitingThread waiter;
        synchronized (this) {
            if (writeLockedThread == thisThread) {
                outstandingWriteLocks++;
                return true;
            }
            if (writeLockedThread == null && grantWriteLock()) {
                writeLockedThread = thisThread;
                outstandingWriteLocks++;
                return true;
            }
            if (!waitIfNecessary) return false;
            deadlockCheck();
            if (waitingForWriteLock == null) waitingForWriteLock = new ArrayList<WaitingThread>(3);
            waiter = new WaitingThread(thisThread, thisThread, this, Lock.WRITE_LOCK);
            addWaitingWrite(waiter);
            DeadlockDetection.addResourceWaiter(thisThread, waiter);
        }
        List<WaitingThread> deadlockedThreads = null;
        LockException exceptionCaught = null;
        synchronized (thisThread) {
            if (thisThread != writeLockedThread) {
                while (thisThread != writeLockedThread && deadlockedThreads == null) {
                    if (LockOwner.DEBUG) {
                        StringBuffer buf = new StringBuffer("Waiting for write: ");
                        for (int i = 0; i < waitingForWriteLock.size(); i++) {
                            buf.append(' ');
                            buf.append((waitingForWriteLock.get(i)).getThread().getName());
                        }
                        LOG.debug(buf.toString());
                        debugReadLocks("WAIT");
                    }
                    deadlockedThreads = checkForDeadlock(thisThread);
                    if (deadlockedThreads == null) {
                        try {
                            waiter.doWait();
                        } catch (LockException e) {
                            exceptionCaught = e;
                            break;
                        }
                    }
                }
            }
            if (deadlockedThreads == null && exceptionCaught == null) outstandingWriteLocks++;
        }
        synchronized (this) {
            DeadlockDetection.clearResourceWaiter(thisThread);
            removeWaitingWrite(waiter);
        }
        if (exceptionCaught != null) throw exceptionCaught;
        if (deadlockedThreads != null) {
            for (WaitingThread wt : deadlockedThreads) {
                wt.signalDeadlock();
            }
            throw new DeadlockException();
        }
        return true;
    }

    private void addWaitingWrite(WaitingThread waiter) {
        waitingForWriteLock.add(waiter);
    }

    private void removeWaitingWrite(WaitingThread waiter) {
        for (int i = 0; i < waitingForWriteLock.size(); i++) {
            WaitingThread next = waitingForWriteLock.get(i);
            if (next.getThread() == waiter.getThread()) {
                waitingForWriteLock.remove(i);
                break;
            }
        }
    }

    public void release() {
        release(Lock.READ_LOCK);
    }

    public void release(int mode) {
        switch(mode) {
            case Lock.NO_LOCK:
                break;
            case Lock.WRITE_LOCK:
                releaseWrite(1);
                break;
            default:
                releaseRead(1);
                break;
        }
    }

    public void release(int mode, int count) {
        switch(mode) {
            case Lock.WRITE_LOCK:
                releaseWrite(count);
                break;
            default:
                releaseRead(count);
                break;
        }
    }

    private synchronized void releaseWrite(int count) {
        if (Thread.currentThread() == writeLockedThread) {
            if (outstandingWriteLocks > 0) outstandingWriteLocks -= count;
            if (outstandingWriteLocks > 0) {
                return;
            }
            if (grantWriteLockAfterRead()) {
                WaitingThread waiter = waitingForWriteLock.get(0);
                removeWaitingWrite(waiter);
                DeadlockDetection.clearResourceWaiter(waiter.getThread());
                writeLockedThread = waiter.getThread();
                synchronized (writeLockedThread) {
                    writeLockedThread.notifyAll();
                }
            } else {
                writeLockedThread = null;
                if (waitingForReadLock > 0) {
                    notifyAll();
                }
            }
        } else {
            LOG.warn("Possible lock problem: a thread released a write lock it didn't hold. Either the " + "thread was interrupted or it never acquired the lock.", new Throwable());
        }
    }

    /**
     * Threads call this method to relinquish a lock that they previously got
     * from this object.
     *
     * @throws IllegalStateException if called when there are no outstanding locks or there is a
     * write lock issued to a different thread.
     */
    private synchronized void releaseRead(int count) {
        if (!outstandingReadLocks.isEmpty()) {
            removeReadLock(count);
            if (writeLockedThread == null && grantWriteLockAfterRead()) {
                WaitingThread waiter = waitingForWriteLock.get(0);
                removeWaitingWrite(waiter);
                DeadlockDetection.clearResourceWaiter(waiter.getThread());
                writeLockedThread = waiter.getThread();
                synchronized (writeLockedThread) {
                    writeLockedThread.notifyAll();
                }
            }
            return;
        } else {
            LOG.warn("Possible lock problem: thread " + Thread.currentThread().getName() + " released a read lock it didn't hold. Either the " + "thread was interrupted or it never acquired the lock. " + "Write lock: " + (writeLockedThread != null ? writeLockedThread.getName() : "null"), new Throwable());
            if (LockOwner.DEBUG) debugReadLocks("ILLEGAL RELEASE");
        }
    }

    public synchronized boolean isLockedForWrite() {
        return writeLockedThread != null || (waitingForWriteLock != null && waitingForWriteLock.size() > 0);
    }

    public synchronized boolean hasLock() {
        return !outstandingReadLocks.isEmpty() || isLockedForWrite();
    }

    public synchronized boolean isLockedForRead(Thread owner) {
        for (int i = outstandingReadLocks.size() - 1; i > -1; i--) {
            if ((outstandingReadLocks.get(i)).getOwner() == owner) return true;
        }
        return false;
    }

    private void removeReadLock(int count) {
        Object owner = Thread.currentThread();
        for (int i = outstandingReadLocks.size() - 1; i > -1 && count > 0; i--) {
            LockOwner current = outstandingReadLocks.get(i);
            if (current.getOwner() == owner) {
                outstandingReadLocks.remove(i);
                --count;
            }
        }
    }

    private void deadlockCheck() throws DeadlockException {
        for (LockOwner next : outstandingReadLocks) {
            Lock lock = DeadlockDetection.isWaitingFor(next.getOwner());
            if (lock != null) {
                lock.wakeUp();
            }
        }
    }

    /**
     * Detect circular wait on different resources: thread A has a write lock on
     * resource R1; thread B has a write lock on resource R2; thread A tries to
     * acquire lock on R2; thread B now tries to acquire lock on R1. Solution:
     * suspend existing write lock of thread A and grant it to B.
     *
     * @return true if the write lock should be granted to the current thread
     */
    private List<WaitingThread> checkForDeadlock(Thread waiter) {
        ArrayList<WaitingThread> waiters = new ArrayList<WaitingThread>(10);
        if (DeadlockDetection.wouldDeadlock(waiter, writeLockedThread, waiters)) {
            LOG.warn("Potential deadlock detected on lock " + getId() + "; killing threads: " + waiters.size());
            return waiters.size() > 0 ? waiters : null;
        }
        return null;
    }

    /**
     * Check if a write lock can be granted, either because there are no
     * read locks, the read lock belongs to the current thread and can be
     * upgraded or the thread which holds the lock is blocked by another
     * lock held by the current thread.
     *
     * @return true if the write lock can be granted
     */
    private boolean grantWriteLock() {
        if (outstandingReadLocks.isEmpty()) {
            return true;
        }
        Thread waiter = Thread.currentThread();
        for (LockOwner next : outstandingReadLocks) {
            if (next.getOwner() != waiter) {
                if (!DeadlockDetection.isBlockedBy(waiter, next.getOwner())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check if a write lock can be granted, either because there are no
     * read locks or the read lock belongs to the current thread and can be
     * upgraded. This method is called whenever a lock is released.
     *
     * @return true if the write lock can be granted
     */
    private boolean grantWriteLockAfterRead() {
        if (waitingForWriteLock != null && waitingForWriteLock.size() > 0) {
            final int size = outstandingReadLocks.size();
            if (size > 0) {
                WaitingThread waiter = waitingForWriteLock.get(0);
                return isCompatible(waiter.getThread());
            }
            return true;
        }
        return false;
    }

    /**
     * Check if the specified thread has a read lock on the resource.
     *
     * @param owner the thread
     * @return true if owner has a read lock
     */
    private boolean hasReadLock(Thread owner) {
        for (LockOwner next : outstandingReadLocks) {
            if (next.getOwner() == owner) return true;
        }
        return false;
    }

    public Thread getWriteLockedThread() {
        return writeLockedThread;
    }

    /**
     * Check if the specified thread holds either a write or a read lock
     * on the resource.
     *
     * @param owner the thread
     * @return true if owner has a lock
     */
    public boolean hasLock(Thread owner) {
        if (writeLockedThread == owner) return true;
        return hasReadLock(owner);
    }

    public void wakeUp() {
    }

    /**
     * Check if the pending request for a write lock is compatible
     * with existing read locks and other write requests. A lock request is
     * compatible with another lock request if: (a) it belongs to the same thread,
     * (b) it belongs to a different thread, but this thread is also waiting for a write lock.
     *
     * @param waiting
     * @return true if the lock request is compatible with all other requests and the
     * lock can be granted.
     */
    private boolean isCompatible(Thread waiting) {
        for (LockOwner next : outstandingReadLocks) {
            if (next.getOwner() != waiting) {
                if (!DeadlockDetection.isBlockedBy(waiting, next.getOwner())) {
                    return false;
                }
            }
        }
        return true;
    }

    public synchronized LockInfo getLockInfo() {
        LockInfo info;
        String[] readers = new String[0];
        if (outstandingReadLocks != null) {
            readers = new String[outstandingReadLocks.size()];
            for (int i = 0; i < outstandingReadLocks.size(); i++) {
                LockOwner owner = outstandingReadLocks.get(i);
                readers[i] = owner.getOwner().getName();
            }
        }
        if (writeLockedThread != null) {
            info = new LockInfo(LockInfo.RESOURCE_LOCK, LockInfo.WRITE_LOCK, getId(), new String[] { writeLockedThread.getName() });
            info.setReadLocks(readers);
        } else {
            info = new LockInfo(LockInfo.RESOURCE_LOCK, LockInfo.READ_LOCK, getId(), readers);
        }
        if (waitingForWriteLock != null) {
            String waitingForWrite[] = new String[waitingForWriteLock.size()];
            for (int i = 0; i < waitingForWriteLock.size(); i++) {
                waitingForWrite[i] = waitingForWriteLock.get(i).getThread().getName();
            }
            info.setWaitingForWrite(waitingForWrite);
        }
        return info;
    }

    private void debugReadLocks(String msg) {
        for (LockOwner owner : outstandingReadLocks) {
            LOG.debug(msg + ": " + owner.getOwner(), owner.getStack());
        }
    }

    @Override
    public void debug(PrintStream out) {
        getLockInfo().debug(out);
    }
}
