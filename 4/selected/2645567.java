package org.armedbear.j;

public final class ReadWriteLock {

    private int activeReaders;

    private int activeWriters;

    private int waitingReaders;

    private int waitingWriters;

    private Thread writerThread;

    private int lockCount;

    public synchronized void lockRead() throws InterruptedException {
        if (activeReaders != 0 || allowRead()) {
            ++activeReaders;
            return;
        }
        if (Thread.currentThread() == writerThread) Debug.bug();
        ++waitingReaders;
        while (!allowRead()) {
            try {
                wait();
            } catch (InterruptedException e) {
                --waitingReaders;
                throw e;
            }
        }
        --waitingReaders;
        ++activeReaders;
    }

    public synchronized void unlockRead() {
        Debug.assertTrue(activeReaders > 0);
        --activeReaders;
        notifyAll();
    }

    public synchronized void lockWrite() throws InterruptedException {
        if (writerThread != null) {
            if (Thread.currentThread() == writerThread) {
                ++lockCount;
                return;
            }
        }
        if (allowWrite()) {
            claimWriteLock();
            return;
        }
        ++waitingWriters;
        while (!allowWrite()) {
            try {
                wait();
            } catch (InterruptedException e) {
                --waitingWriters;
                throw e;
            }
        }
        --waitingWriters;
        claimWriteLock();
    }

    public synchronized void unlockWrite() {
        Debug.assertTrue(activeWriters == 1);
        Debug.assertTrue(lockCount > 0);
        Debug.assertTrue(Thread.currentThread() == writerThread);
        if (--lockCount == 0) {
            --activeWriters;
            writerThread = null;
            notifyAll();
        }
    }

    public synchronized boolean isWriteLocked() {
        Debug.assertTrue(activeWriters == 0 || activeWriters == 1);
        return activeWriters == 1;
    }

    private final boolean allowRead() {
        return waitingWriters == 0 && activeWriters == 0;
    }

    private final boolean allowWrite() {
        return activeReaders == 0 && activeWriters == 0;
    }

    private void claimWriteLock() {
        ++activeWriters;
        Debug.assertTrue(writerThread == null);
        writerThread = Thread.currentThread();
        Debug.assertTrue(lockCount == 0);
        lockCount = 1;
    }
}
