package net.sourceforge.jsxe.util;

import java.util.Vector;

/**
 * Implements consumer/producer locking scemantics.
 * @author Peter Graves
 * @version $Id: ReadWriteLock.java,v 1.7 2004/02/14 19:02:49 spestov Exp $
 * The lock tries to be re-entrant when called from the same thread in some
 * cases.
 * 
 * The following is ok:
 * read lock
 * read lock
 * read unlock
 * read unlock
 * 
 * write lock
 * read lock
 * read unlock
 * write unlock
 * 
 * The following is not ok:
 * 
 * read lock
 * write lock
 * write unlock
 * read unlock
 * 
 * write lock
 * write lock
 * write unlock
 * write unlock
 */
public class ReadWriteLock {

    public synchronized void readLock() {
        if (activeReaders != 0 || allowRead()) {
        }
        ++waitingReaders;
        while (!allowRead()) {
            try {
                wait();
            } catch (InterruptedException e) {
                --waitingReaders;
                Log.log(Log.ERROR, this, e);
                return;
            }
        }
        --waitingReaders;
        ++activeReaders;
        readers.addElement(Thread.currentThread());
    }

    public synchronized void readUnlock() {
        if (activeReaders == 0) {
            throw new InternalError("Unbalanced readLock()/readUnlock() calls");
        }
        --activeReaders;
        notifyAll();
    }

    public synchronized void writeLock() {
        if (writerThread != null) {
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
                Log.log(Log.ERROR, this, e);
                return;
            }
        }
        --waitingWriters;
        claimWriteLock();
    }

    public synchronized void writeUnlock() {
        if (activeWriters != 1 || lockCount <= 0) {
            throw new InternalError("Unbalanced writeLock()/writeUnlock() calls");
        }
        if (Thread.currentThread() != writerThread) {
            throw new InternalError("writeUnlock() from wrong thread");
        }
        if (--lockCount == 0) {
            --activeWriters;
            writerThread = null;
            notifyAll();
        }
    }

    public synchronized boolean isWriteLocked() {
        return activeWriters == 1;
    }

    private int activeReaders;

    private int activeWriters;

    private int waitingReaders;

    private int waitingWriters;

    private Vector readers = new Vector();

    private Thread writerThread;

    private int lockCount;

    private final boolean allowRead() {
        return (Thread.currentThread() == writerThread) || (waitingWriters == 0 && activeWriters == 0);
    }

    private final boolean allowWrite() {
        return activeReaders == 0 && activeWriters == 0;
    }

    private void claimWriteLock() {
        ++activeWriters;
        writerThread = Thread.currentThread();
        lockCount = 1;
    }
}
