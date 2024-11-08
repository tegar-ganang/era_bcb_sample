package org.progeeks.repository;

import java.io.*;
import java.nio.channels.*;
import org.progeeks.util.log.Log;

/**
 * A locking manager that provides basic distributed locking
 * among several processes assuming that all processes are
 * willing to access repository items in a WORM (Write Once, Read
 * Many) manner.  The next repository item id is maintained in
 * a file in the root of the repository.  This file is locked
 * by each process only when it needs a new item id.  This
 * locking manager is not safe to use with the meta-class
 * cache.
 *
 * @version     $Revision: 1.4 $
 * @author      Paul Wisneskey
 *
 */
public class WormSharedLockManager implements LockManager {

    static final Log log = Log.getLog();

    /**
     * Default time in milliseconds between locking attempt tries.
     */
    public static final int DEFAULT_RETRY_TIME = 100;

    /**
     * File used to save the id number to assign to the next
     * added item.
     */
    protected static final String ID_FILE = "repository.id";

    /**
     * Time in milliseconds between locking attempt tries.
     */
    private int retryTime;

    /**
     * File containing the next repository item identifier to assign.
     */
    private RandomAccessFile idFile;

    /**
     * Object used for synchronizing access to the item id
     * generation.
     */
    private Object itemIdLock = new Object();

    private long lockIdSalt;

    private long nextLockId = 0;

    /**
     * Sets the time in milliseconds to wait between attempts to obtain
     * the id lock.
     */
    public void setRetryTime(int retryTime) {
        this.retryTime = retryTime;
    }

    /**
     * Returns the time in millseconds to wait between attempts to obtain
     * the id lock.
     */
    public int getRetryTime() {
        return retryTime;
    }

    /**
     * Initializes the repository, verifying that the id file is
     * present.
     */
    public void initialize(File repositoryDir) {
        File baseIdFile = new File(repositoryDir, ID_FILE);
        if (!baseIdFile.exists()) {
            throw new RuntimeException("Repository id file not found.");
        }
        try {
            idFile = new RandomAccessFile(baseIdFile, "rwd");
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to open repository id file.", e);
        }
        lockIdSalt = System.currentTimeMillis();
    }

    /**
     * Creates the repository.  Note that nothing is done to ensure
     * that another process is not also trying to create the
     * repository at the same time.
     */
    public void createRepository(File repositoryDir) {
        File idFile = new File(repositoryDir, ID_FILE);
        RandomAccessFile idWriter = null;
        try {
            idWriter = new RandomAccessFile(idFile, "rwd");
            idWriter.writeInt(0);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create repository id file.", e);
        } finally {
            if (idWriter != null) {
                try {
                    idWriter.close();
                } catch (IOException e) {
                    log.warn("Failed to close repository id file.");
                }
            }
        }
    }

    /**
     * Terminates the locking manager.
     */
    public void terminate() {
    }

    /**
     * Returns the next item id, locking the id file for the read and write
     * so that the id can be guaranteed unique between all the processes
     * using the repository.
     */
    public int getNextItemId() {
        int nextItemId;
        FileChannel lockChannel = idFile.getChannel();
        FileLock repositoryLock = null;
        while (repositoryLock == null && !Thread.currentThread().isInterrupted()) {
            try {
                repositoryLock = lockChannel.tryLock();
            } catch (IOException e) {
            }
            if (repositoryLock == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.warn("Interrupted waiting to obtain repository id lock.");
                    continue;
                }
            }
        }
        if (repositoryLock == null) {
            throw new RuntimeException("Failed to obtain repository id lock.");
        }
        try {
            idFile.seek(0);
            nextItemId = idFile.readInt();
            idFile.seek(0);
            idFile.writeInt(nextItemId + 1);
        } catch (IOException e) {
            repositoryLock = null;
            throw new RuntimeException("Failed to read/write next repository item id.", e);
        } finally {
            try {
                repositoryLock.release();
            } catch (IOException ne) {
                log.warn("Error occurred releasing lock after id read/write.", ne);
            }
        }
        return nextItemId;
    }

    /**
     * Locks an item in the repository.  Since this locking manager assumes
     * a write once, read many policy, all locks are approved since they all
     * should be shared read locks.  Any write locks are assumed to be for item
     * deletions - or if not, for updates that are coordinated externally.
     */
    public Lock lockItem(RepositoryItem item, boolean writeLock) {
        return new WormLock(item.getId(), lockIdSalt, nextLockId++);
    }

    /**
     * Releases a lock on an item on the repository.  Since we don't really
     * assign locks, this does nothing under the covers.
     */
    public void releaseLock(Lock lock) {
    }

    /**
     * Locks an item in the repository.  Since this locking manager assumes
     * a write once, read many policy, all locks are approved since they all
     * should be shared read locks.  Any write locks are assumed to be for item
     * deletions - or if not, for updates that are coordinated externally.
     */
    public Lock lockItem(String itemId, boolean writeLock) {
        return new WormLock(itemId, lockIdSalt, nextLockId++);
    }

    /**
     * Implementation of the Lock for handing to callers.
     */
    private static class WormLock implements Lock {

        private static final long serialVersionUID = 42L;

        private String itemId;

        private long salt;

        private long id;

        public WormLock(String itemId, long salt, long id) {
            this.itemId = itemId;
            this.salt = salt;
            this.id = id;
        }

        public String getItemId() {
            return itemId;
        }

        public boolean equals(Object object) {
            if (!(object instanceof WormLock)) {
                return false;
            }
            WormLock otherLock = (WormLock) object;
            return (this.salt == otherLock.salt) && (this.id == otherLock.id);
        }

        public int hashCode() {
            long value = salt + id;
            return (int) (value ^ (value >>> 32));
        }
    }
}
