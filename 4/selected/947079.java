package org.ozoneDB.core;

import java.io.*;
import java.util.logging.Level;
import org.ozoneDB.*;
import org.ozoneDB.DxLib.*;
import org.ozoneDB.util.*;

/**
 * This class implements a Multiple Reader One Writer lock policy.
 *
 *
 * @author <a href="http://www.softwarebuero.de/">SMB</a>
 * @author <a href="http://www.medium.net/">Medium.net</a>
 * @version $Revision: 1.13 $Date: 2005/12/12 19:39:52 $
 */
public class MROWLock extends AbstractLock {

    protected static final long serialVersionUID = 1;

    protected static final byte subSerialVersionUID = 1;

    private int level = LEVEL_NONE;

    private SharedLock readLock;

    private ExclusiveLock writeLock;

    protected transient String debugInfo;

    public MROWLock() {
        level = LEVEL_NONE;
        readLock = new SharedLock();
        writeLock = new ExclusiveLock();
    }

    public void setDebugInfo(String debugInfo) {
        this.debugInfo = debugInfo;
    }

    /**
     * @return <code>NOT_ACQUIRED</code> if the lock could not be acquired,
     * and the previous value of the lock for the specified transaction if the
     * lock could be acquired.
     */
    public synchronized int tryAcquire(Transaction ta, int newLevel) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this + " ta.getID(): " + ta.taID() + " newLevel: " + newLevel);
        }
        if (newLevel <= LEVEL_NONE || newLevel >= LEVEL_MAX) {
            throw new IllegalArgumentException("invalid lock level value");
        }
        int result;
        if (readLock.isAcquiredBy(ta) && level >= newLevel) {
            result = level;
        } else if (level == LEVEL_NONE) {
            result = LEVEL_NONE;
            if (readLock.tryAcquire(ta, LEVEL_READ) == NOT_ACQUIRED) {
                throw new OzoneInternalException("cannot get lock on shared read lock");
            }
            if (newLevel > LEVEL_READ && writeLock.tryAcquire(ta, LEVEL_WRITE) == NOT_ACQUIRED) {
                throw new OzoneInternalException("cannot get lock on exclusive write lock that should be empty");
            }
        } else if (readLock.isAcquiredBy(ta)) {
            result = (readLock.areMultipleLockersHoldingLocks() || writeLock.tryAcquire(ta, LEVEL_WRITE) == NOT_ACQUIRED) ? NOT_ACQUIRED : level;
        } else if (newLevel == LEVEL_READ && level == LEVEL_READ) {
            result = LEVEL_NONE;
            if (readLock.tryAcquire(ta, LEVEL_READ) == NOT_ACQUIRED) {
                throw new OzoneInternalException("cannot get lock on shared read lock");
            }
        } else {
            result = NOT_ACQUIRED;
        }
        if (result != NOT_ACQUIRED && newLevel > level) {
            level = newLevel;
        }
        return result;
    }

    public synchronized void release(Transaction ta) {
        if (false && logger.isLoggable(Level.FINER)) {
            logger.finer(this + ".release(): ta.getID()=" + ta.taID());
        }
        if (writeLock.isAcquiredBy(ta)) {
            writeLock.release(ta);
            level = LEVEL_READ;
        }
        readLock.release(ta);
        if (readLock.level(null) == LEVEL_NONE) {
            level = LEVEL_NONE;
        }
    }

    public synchronized boolean isAcquiredBy(Transaction ta) {
        switch(level) {
            case LEVEL_NONE:
                return false;
            case LEVEL_READ:
                return readLock.isAcquiredBy(ta);
            default:
                return writeLock.isAcquiredBy(ta);
        }
    }

    public synchronized TransactionID getWriteLockingTransactionID() {
        return level < LEVEL_WRITE ? null : writeLock.getLocker();
    }

    public synchronized DxCollection lockerIDs() {
        switch(level) {
            case LEVEL_NONE:
                {
                    return new DxArrayBag();
                }
            default:
                {
                    return readLock.lockerIDs();
                }
        }
    }

    public synchronized int level(Transaction ta) {
        if (ta == null) {
            return level;
        } else {
            switch(level) {
                case LEVEL_NONE:
                    return LEVEL_NONE;
                case LEVEL_READ:
                    return readLock.isAcquiredBy(ta) ? level : LEVEL_NONE;
                default:
                    return writeLock.isAcquiredBy(ta) ? level : LEVEL_NONE;
            }
        }
    }

    public String toString() {
        return "MROWLock[" + debugInfo + ", writeLock: " + writeLock + ", readLock: " + readLock + ", level=" + level + "]";
    }
}
