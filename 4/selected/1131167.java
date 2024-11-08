package com.hp.hpl.jena.shared;

import com.hp.hpl.jena.shared.JenaException;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import java.util.*;
import org.apache.commons.logging.*;

/**
 * Lock implemenetation using a Multiple Reader, Single Writer policy.
 * All the locking work is done by the imported WriterPreferenceReadWriteLock.
 * Ths class adds:
 * <ul>
 *   <li>The same thread that acquired a lock should release it</li>
 *   <li>Lock promotion (turning read locks into write locks) is 
 *   deteched as an error</li>
 *  <ul>
 *   
 * @author      Andy Seaborne
 * @version     $Id: LockMRSW.java,v 1.3 2006/03/22 13:52:44 andy_seaborne Exp $
 */
public class LockMRSW implements Lock {

    static Log log = LogFactory.getLog(LockMRSW.class);

    Map threadStates = new HashMap();

    int threadStatesSize = threadStates.size();

    WriterPreferenceReadWriteLock mrswLock = new WriterPreferenceReadWriteLock();

    SynchronizedInt activeReadLocks = new SynchronizedInt(0);

    SynchronizedInt activeWriteLocks = new SynchronizedInt(0);

    public LockMRSW() {
        if (log.isDebugEnabled()) log.debug("Lock : " + Thread.currentThread().getName());
    }

    /** Application controlled locking - enter a critical section.
     *  Locking is reentrant so an application can have nested critical sections.
     *  Typical code:
     *  <pre>
     *  try {
     *     enterCriticalSection(Lock.READ) ;
     *     ... application code ...
     *  } finally { leaveCriticalSection() ; }
     * </pre>
     */
    public final void enterCriticalSection(boolean readLockRequested) {
        LockState state = getLockState();
        if (log.isDebugEnabled()) log.debug(Thread.currentThread().getName() + " >> enterCS: " + report(state));
        if (state.readLocks > 0 && state.writeLocks == 0 && !readLockRequested) {
            state.readLocks++;
            activeReadLocks.increment();
            if (log.isDebugEnabled()) log.debug(Thread.currentThread().getName() + " << enterCS: promotion attempt: " + report(state));
            throw new JenaException("enterCriticalSection: Write lock request while holding read lock - potential deadlock");
        }
        if (state.writeLocks > 0 && readLockRequested) readLockRequested = false;
        try {
            if (readLockRequested) {
                if (state.readLocks == 0) mrswLock.readLock().acquire();
                state.readLocks++;
                activeReadLocks.increment();
            } else {
                if (state.writeLocks == 0) mrswLock.writeLock().acquire();
                state.writeLocks++;
                activeWriteLocks.increment();
            }
        } catch (InterruptedException intEx) {
        } finally {
            if (log.isDebugEnabled()) log.debug(Thread.currentThread().getName() + " << enterCS: " + report(state));
        }
    }

    /** Application controlled locking - leave a critical section.
     *  @see #enterCriticalSection
     */
    public final void leaveCriticalSection() {
        LockState state = getLockState();
        if (log.isDebugEnabled()) log.debug(Thread.currentThread().getName() + " >> leaveCS: " + report(state));
        try {
            if (state.readLocks > 0) {
                state.readLocks--;
                activeReadLocks.decrement();
                if (state.readLocks == 0) mrswLock.readLock().release();
                state.clean();
                return;
            }
            if (state.writeLocks > 0) {
                state.writeLocks--;
                activeWriteLocks.decrement();
                if (state.writeLocks == 0) mrswLock.writeLock().release();
                state.clean();
                return;
            }
            throw new JenaException("leaveCriticalSection: No lock held (" + Thread.currentThread().getName() + ")");
        } finally {
            if (log.isDebugEnabled()) log.debug(Thread.currentThread().getName() + " << leaveCS: " + report(state));
        }
    }

    private String report(LockState state) {
        StringBuffer sb = new StringBuffer();
        sb.append("Thread R/W: ");
        sb.append(Integer.toString(state.readLocks));
        sb.append("/");
        sb.append(Integer.toString(state.writeLocks));
        sb.append(" :: Model R/W: ");
        sb.append(Integer.toString(activeReadLocks.get()));
        sb.append("/");
        sb.append(Integer.toString(activeWriteLocks.get()));
        sb.append(" (thread: ");
        sb.append(state.thread.getName());
        sb.append(")");
        return sb.toString();
    }

    synchronized LockState getLockState() {
        Thread thisThread = Thread.currentThread();
        LockState state = (LockState) threadStates.get(thisThread);
        if (state == null) {
            state = new LockState(this);
            threadStates.put(thisThread, state);
            threadStatesSize = threadStates.size();
        }
        return state;
    }

    synchronized void removeLockState(Thread thread) {
        threadStates.remove(thread);
    }

    static class LockState {

        int readLocks = 0;

        int writeLocks = 0;

        LockMRSW lock;

        Thread thread;

        LockState(LockMRSW theLock) {
            lock = theLock;
            thread = Thread.currentThread();
        }

        void clean() {
            if (lock.activeReadLocks.get() == 0 && lock.activeWriteLocks.get() == 0) {
                lock.removeLockState(thread);
            }
        }
    }
}
