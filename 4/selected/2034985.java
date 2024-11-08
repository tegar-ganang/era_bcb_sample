package edu.yale.csgp.vitapad.util;

import java.util.Vector;
import edu.yale.csgp.vitapad.util.logging.ILogger;
import edu.yale.csgp.vitapad.util.logging.LoggerController;

/**
 * DOCUMENT ME!
 * 
 * @author Matt Holford
 */
public class ReadWriteLock {

    private static final ILogger _log = LoggerController.createLogger(ReadWriteLock.class);

    private Thread writerThread;

    private int activeWriters;

    private int waitingWriters;

    private int activeReaders;

    private int lockCount;

    private int waitingReaders;

    private Vector readers = new Vector();

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

    public synchronized boolean isWriteLocked() {
        return activeWriters == 1;
    }

    public synchronized void readLock() {
        if (activeReaders != 0 || allowRead()) {
            ++activeReaders;
            return;
        }
        ++waitingReaders;
        while (!allowRead()) {
            try {
                wait();
            } catch (InterruptedException e) {
                --waitingReaders;
                _log.error("ReadLock interrupted", e);
                return;
            }
        }
        --waitingReaders;
        ++activeReaders;
        readers.addElement(Thread.currentThread());
    }

    public synchronized void readUnlock() {
        if (activeReaders == 0) throw new InternalError("Unbalanced readLock()/readUnlock() calls");
        --activeReaders;
        notifyAll();
    }

    public synchronized void writeLock() {
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
                _log.error("Write Lock interrupted", e);
            }
        }
        --waitingWriters;
        claimWriteLock();
    }

    public synchronized void writeUnlock() {
        if (activeWriters != 1 || lockCount == 0) throw new InternalError("Unbalanced writeLock()/writeUnlock() calls");
        if (Thread.currentThread() != writerThread) throw new InternalError("calling writeUnlock() from wrong thread");
        if (--lockCount == 0) {
            --activeWriters;
            writerThread = null;
            notifyAll();
        }
    }
}
