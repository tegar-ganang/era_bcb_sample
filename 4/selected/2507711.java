package org.progeeks.repository;

import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.channels.*;
import java.util.*;
import org.progeeks.util.log.Log;

/**
 * Default implementation of repository lock manager that permits
 * only one instance of the repository to be used at a time.  Maintains
 * the next repository item id to be assigned in a file in the root
 * repository directory.  This locking manager is safe to use with
 * the meta-class cache.
 *
 * @version     $Revision: 1.3 $
 * @author      Paul Wisneskey
 *
 */
public class DefaultLockManager implements LockManager {

    static final Log log = Log.getLog();

    /**
     * File used to save the id number to assign to the next
     * added item.
     */
    protected static final String ID_FILE = "repository.id";

    /**
     * File containing the next repository item identifier to assign.
     */
    private RandomAccessFile idFile;

    /**
     * File lock used to lock the repository.
     */
    private FileLock repositoryLock;

    /**
     * Id number used to generated the id of the next repository item
     * that is created.  Using an int means that the largest item id
     * string generated will be 8 characters long (if the hexidecimal
     * value of the integer is used).
     */
    private int nextItemId;

    /**
     * Object used for synchronizing access to the item id
     * generation.
     */
    private Object itemIdLock = new Object();

    /**
     * Map used to track the various read/write locks on items in
     * the repository.
     */
    private Map itemLockMap = new HashMap();

    private long lockIdSalt;

    private long nextLockId = 0;

    /**
     * Initializes the repository, locking its id file to ensure that
     * only one process has access to the repository.
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
        FileChannel lockChannel = idFile.getChannel();
        try {
            repositoryLock = lockChannel.tryLock();
        } catch (IOException e) {
            throw new RuntimeException("Failed to obtain lock on repository.", e);
        }
        if (repositoryLock == null) {
            throw new RuntimeException("Failed to obtain lock on repository; repository is already locked.");
        }
        try {
            idFile.seek(0);
            nextItemId = idFile.readInt();
        } catch (IOException e) {
            try {
                repositoryLock.release();
            } catch (IOException ne) {
                log.warn("Error occurred releasing lock after id read failure.", ne);
            }
            repositoryLock = null;
            throw new RuntimeException("Failed to read next repository item id.", e);
        }
        lockIdSalt = System.currentTimeMillis();
    }

    public void terminate() {
        synchronized (itemLockMap) {
            if (repositoryLock == null) {
                throw new RuntimeException("Repository already closed.");
            }
            try {
                repositoryLock.release();
            } catch (IOException e) {
                log.warn("Error during release of repository lock.", e);
            }
            repositoryLock = null;
            if (!itemLockMap.isEmpty()) {
                log.warn("Repository closed when some items were still locked.");
            }
            itemLockMap.clear();
        }
    }

    /**
     * Returns the next item id.
     */
    public int getNextItemId() {
        synchronized (itemIdLock) {
            int newId = nextItemId++;
            try {
                idFile.seek(0);
                idFile.writeInt(nextItemId);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write next item id to id file.", e);
            }
            return newId;
        }
    }

    /**
     * Lock the given item for reading and/or writing.
     */
    public Lock lockItem(RepositoryItem item, boolean writeLock) {
        return lockItem(item.getId(), writeLock);
    }

    /**
     * Lock the given itemId for reading and/or writing.
     */
    public Lock lockItem(String itemId, boolean writeLock) {
        synchronized (itemLockMap) {
            if (repositoryLock == null) {
                throw new RuntimeException("Repository closed.");
            }
            LockMapEntry entry = getLockMapEntry(itemId, true);
            entry.checkLocks();
            Lock lock = new DefaultLock(itemId, lockIdSalt, nextLockId++);
            if (writeLock) {
                if (entry.isLocked()) {
                    throw new RepositoryLockException("Failed to obtain write lock; item already locked.");
                }
                entry.lockForWriting(lock);
            } else {
                if (entry.isWriteLocked()) {
                    throw new RepositoryLockException("Failed to obtain read lock; item is write locked.");
                }
                entry.lockForReading(lock);
            }
            return lock;
        }
    }

    public void releaseLock(Lock lock) {
        if (!(lock instanceof DefaultLock)) {
            throw new RuntimeException("Invalid lock object type.");
        }
        synchronized (itemLockMap) {
            LockMapEntry entry = getLockMapEntry(lock.getItemId(), false);
            if (entry == null) {
                log.error("Failed to find lock map entry for item being released: itemId=" + lock.getItemId());
                return;
            }
            entry.removeLock(lock);
            entry.checkLocks();
            if (!entry.isLocked()) {
                itemLockMap.remove(lock.getItemId());
            }
        }
    }

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
     * Returns the lock map entry for the given item id, optionally creating it if
     * missing.  This method assumes the synchronization lock on the item lock map
     * is already held by the caller.
     */
    private LockMapEntry getLockMapEntry(String itemId, boolean createIfMissing) {
        LockMapEntry entry = (LockMapEntry) itemLockMap.get(itemId);
        if (entry == null && createIfMissing) {
            entry = new LockMapEntry(itemId);
            itemLockMap.put(itemId, entry);
        }
        return entry;
    }

    /**
     * Implementation of the Lock for handing to callers.
     */
    private static class DefaultLock implements Lock {

        private static final long serialVersionUID = 42L;

        private String itemId;

        private long salt;

        private long id;

        public DefaultLock(String itemId, long salt, long id) {
            this.itemId = itemId;
            this.salt = salt;
            this.id = id;
        }

        public String getItemId() {
            return itemId;
        }

        public boolean equals(Object object) {
            if (!(object instanceof DefaultLock)) {
                return false;
            }
            DefaultLock otherLock = (DefaultLock) object;
            return (this.salt == otherLock.salt) && (this.id == otherLock.id);
        }

        public int hashCode() {
            long value = salt + id;
            return (int) (value ^ (value >>> 32));
        }
    }

    /**
     * Class used to hold the locking information for a single item in the
     * repository.
     */
    private static class LockMapEntry {

        private String itemId;

        private Set readLocks = new HashSet();

        private WeakReference writeLock = null;

        public LockMapEntry(String itemId) {
            this.itemId = itemId;
        }

        /**
         * Checks the current locks on an item to see if any of them need to
         * be cleared because the items handles that have them have been
         * leaked (i.e. reclaimed by the garbage collector without having
         * been explicitly released).
         */
        public void checkLocks() {
            if (writeLock != null && writeLock.get() == null) {
                log.error("Clearing write lock on leaked repository item id=" + itemId);
                writeLock = null;
            }
            for (Iterator iterator = readLocks.iterator(); iterator.hasNext(); ) {
                WeakReference ref = (WeakReference) iterator.next();
                if (ref.get() == null) {
                    log.warn("Clearing read lock on leaked repository item id=" + itemId);
                    iterator.remove();
                }
            }
        }

        /**
         * Returns true if the item has been locked for writing.  Does not attempt
         * to check to see if the write lock is stale.
         */
        public boolean isWriteLocked() {
            return (writeLock != null);
        }

        /**
         * Returns true if the item has been locked for reading at least once.  Does
         * not attempt to see if any of the read locks are stale.
         */
        public boolean isReadLocked() {
            return (!readLocks.isEmpty());
        }

        /**
         * Returns true if the item is locked either for reading or writing.  Does not
         * attempt to see if any of the locks are stale.
         */
        public boolean isLocked() {
            return isReadLocked() || isWriteLocked();
        }

        /**
         * Removes the lock that is being held by the supplied item handle if one
         * exists.  Does nothing if no lock for the item handle could be found.
         */
        public void removeLock(Lock lock) {
            if (writeLock != null && writeLock.get().equals(lock)) {
                writeLock = null;
                return;
            }
            for (Iterator iterator = readLocks.iterator(); iterator.hasNext(); ) {
                WeakReference ref = (WeakReference) iterator.next();
                if (ref.get() == lock) {
                    iterator.remove();
                    return;
                }
            }
        }

        /**
         * Locks the repository item for writing.
         */
        public void lockForWriting(Lock lock) {
            if (writeLock != null) {
                throw new RepositoryLockException("Item is already locked for writing.");
            }
            writeLock = new WeakReference(lock);
        }

        /**
         * Locks the repository item for reading. No check is made to see if the
         * lock instance was already used for a lock.
         */
        public void lockForReading(Lock lock) {
            readLocks.add(new WeakReference(lock));
        }
    }
}
