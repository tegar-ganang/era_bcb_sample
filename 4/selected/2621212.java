package org.ozoneDB.core.storage.gammaStore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ozoneDB.*;
import org.ozoneDB.core.*;
import org.ozoneDB.core.DbRemote.DbCommand;
import org.ozoneDB.core.storage.Cache;
import org.ozoneDB.core.storage.FixedSizeCache;

/**
 * TODO: override public void releaseObject(ObjectContainer)
 *
 * @author <a href="mailto:leoATmekenkampD0Tcom">Leo Mekenkamp (mind the anti sp@m)</a>
 * @version $Id: GammaTransaction.java,v 1.35 2005/12/12 19:40:24 leomekenkamp Exp $
 */
public class GammaTransaction extends Transaction implements Serializable {

    private static final Logger logger = Logger.getLogger(GammaTransaction.class.getName());

    private static final class LockingInfo implements Serializable {

        static final int LOCK_READ = 1;

        static final int LOCK_WRITE = 2;

        private static final long serialVersionUID = 0L;

        private int type;

        private ContainerLocation containerLocation;

        private ObjectID objectID;

        private boolean wipeOnCommit;

        /**
         * Write lock.
         */
        LockingInfo(ObjectID objectID, ContainerLocation prevCommitted, boolean wipeOnCommit) {
            type = LOCK_WRITE;
            containerLocation = prevCommitted;
            this.objectID = objectID;
        }

        /**
         * Read lock.
         */
        LockingInfo(ObjectID objectID) {
            type = LOCK_READ;
            this.objectID = objectID;
        }

        int getType() {
            return type;
        }

        ContainerLocation getContainerLocation() {
            return containerLocation;
        }

        int getImageSize() {
            return containerLocation.getSize();
        }

        ObjectID getObjectID() {
            return objectID;
        }

        boolean getWipeOnCommit() {
            return wipeOnCommit;
        }

        public String toString() {
            return "LockingInfo(type: " + type + "; loc: " + containerLocation + "; id: " + objectID + "; wipeOnCommit: " + wipeOnCommit;
        }
    }

    private static final long serialVersionUID = 1L;

    private static final String NAME_SEPARATOR = ".";

    private GammaStore gammaStore;

    private StorageFactory storageFactory;

    private Map containerNames;

    private List lockingInfos;

    /**
     * Keeps track of the number of lockingInfos that have been persisted.
     */
    private int lockingInfosCount = 0;

    public GammaTransaction(GammaStore gammaStore, StorageFactory storageFactory, Server server, User owner) {
        super(server, owner);
        if (logger.isLoggable(Level.FINE)) logger.fine("transaction created: " + this);
        try {
            setGammaStore(gammaStore);
            setStorageFactory(storageFactory);
            Storage storage = getStorageFactory().createStorage(logStorageName());
            storage.sync();
            storage.close();
        } catch (IOException e) {
            throw new OzoneInternalException("could not create transaction", e);
        }
        lockingInfos = new LinkedList();
    }

    private String logStorageName() {
        return taID().value() + NAME_SEPARATOR + "log";
    }

    private String lockingInfoStorageName(int index) {
        return taID().value() + NAME_SEPARATOR + index + NAME_SEPARATOR + "locks";
    }

    private String committingStorageName() {
        return taID().value() + NAME_SEPARATOR + "committing";
    }

    protected ObjectContainer acquireContainer(ObjectContainer container, int lockLevel) throws PermissionError, TransactionException, TransactionError, IOException, ObjectNotFoundException, ClassNotFoundException {
        if (lockLevel >= Lock.LEVEL_WRITE && getGammaStore().isReadOnly()) {
            throw new PermissionDeniedException("database is read only");
        }
        GammaContainer result = (GammaContainer) super.acquireContainer(container, lockLevel);
        return result;
    }

    private GammaContainer containerForName(String name) throws Exception {
        GammaContainer result = null;
        boolean searchMain = true;
        if (getContainerNames() != null) {
            searchMain = !getContainerNames().containsKey(name);
            ObjectID id = (ObjectID) getContainerNames().get(name);
            if (id != null) {
                if (logger.isLoggable(Level.FINEST)) logger.finest("object name " + name + " found in transaction, id: " + id);
                result = (GammaContainer) acquireObject(id, Lock.LEVEL_READ);
            } else {
                if (logger.isLoggable(Level.FINEST)) logger.finest("object name " + name + " removed in transaction");
            }
        }
        if (searchMain) {
            if (logger.isLoggable(Level.FINEST)) logger.finest("object name " + name + " not found in transaction");
            result = (GammaContainer) getGammaStore().containerForName(this, name);
        }
        return result;
    }

    private GammaStore getGammaStore() {
        return gammaStore;
    }

    private void setGammaStore(GammaStore gammaStore) {
        this.gammaStore = gammaStore;
    }

    /**
     * Marks the container as changed in memory, so we know that it has changed
     * and needs to be persisted.
     */
    public void afterInvoke(ObjectContainer container, int lockLevel) {
        if (lockLevel >= Lock.LEVEL_WRITE) {
            GammaContainer gammaContainer = (GammaContainer) container;
            gammaContainer.afterWriteInvoke(taID().value());
        }
    }

    /**
     * TODO: should do something with onPassivate here; see Transaction.nameObject
     */
    public void nameObject(ObjectID id, String name) throws Exception {
        ObjectContainer oldContainerWithThatName = containerForName(name);
        if (oldContainerWithThatName != null) {
            throw new PermissionDeniedException("object name '" + name + "' already exists");
        }
        ObjectContainer container = acquireObject(id, Lock.LEVEL_WRITE);
        if (container == null) {
            throw new OzoneInternalException("how can a named object be null?");
        }
        if (name == null) {
            initContainerNames().put(name, null);
        } else {
            initContainerNames().put(name, container.id());
        }
        container.setName(name);
    }

    StorageFactory getStorageFactory() {
        return storageFactory;
    }

    private void setStorageFactory(StorageFactory storageFactory) {
        this.storageFactory = storageFactory;
    }

    private Map getContainerNames() {
        return containerNames;
    }

    private Map initContainerNames() {
        if (containerNames == null) {
            containerNames = new HashMap();
        }
        return containerNames;
    }

    synchronized int getStatus() {
        return status;
    }

    synchronized void setStatus(int status) {
        this.status = status;
    }

    /**
     * Called after a container has been read without a lock and that container
     * his lastWritingTransactionID equals ours.
     */
    synchronized void isThisYours(GammaContainer container) {
        if (getStatus() < STATUS_COMMITTING) {
            if (container.getGammaLock().tryAcquire(this, Lock.LEVEL_WRITE, false) == Lock.NOT_ACQUIRED) {
                throw new OzoneInternalException("could not acquire write lock on " + container.getObjectId() + " while being asked if it is mine: " + this);
            }
            if (logger.isLoggable(Level.FINER)) logger.finer("container " + container.getObjectId() + " is mine: " + this);
        }
    }

    public void prepareCommit() throws IOException, ClassNotFoundException {
        if (logger.isLoggable(Level.FINE)) logger.fine("starting prepare commit " + this);
        setStatus(STATUS_PREPARING);
        if (!getGammaStore().isReadOnly()) {
            for (Iterator i = lockingInfosIterator(); i.hasNext(); ) {
                LockingInfo lockingInfo = (LockingInfo) i.next();
                if (lockingInfo.getType() == LockingInfo.LOCK_WRITE) {
                    if (logger.isLoggable(Level.FINEST)) logger.finest("pre-committing object " + lockingInfo.getObjectID());
                    getGammaStore().preCommit(lockingInfo.getObjectID());
                }
            }
            getGammaStore().waitForSerializer(this);
            getGammaStore().getDataFileManager().flush();
        }
        if (logger.isLoggable(Level.FINE)) logger.fine("ready preparing commit " + this);
        setStatus(STATUS_PREPARED);
    }

    public void commit() throws IOException, ClassNotFoundException {
        if (logger.isLoggable(Level.FINE)) logger.fine("committing transaction " + this);
        setStatus(STATUS_COMMITTING);
        Storage storage = getStorageFactory().createStorage(committingStorageName());
        storage.close();
        if (!getGammaStore().isReadOnly()) {
            if (getContainerNames() != null) {
                getGammaStore().updateContainerNames(getContainerNames());
            }
            for (Iterator i = lockingInfosIterator(); i.hasNext(); ) {
                LockingInfo lockingInfo = (LockingInfo) i.next();
                if (logger.isLoggable(Level.FINER)) logger.finer("committing " + (lockingInfo.getType() == LockingInfo.LOCK_READ ? "read: " : " write: ") + lockingInfo.getObjectID());
                if (lockingInfo.getType() == LockingInfo.LOCK_READ) {
                    GammaContainer container = (GammaContainer) getGammaStore().containerForID(this, lockingInfo.getObjectID());
                    if (container.getGammaLock().level(null) == Lock.LEVEL_READ) {
                        container.getGammaLock().release(this);
                    }
                } else {
                    GammaContainer container = getGammaStore().commit(lockingInfo.getObjectID(), lockingInfo.getContainerLocation(), lockingInfo.getWipeOnCommit());
                    if (container != null && container.getGammaLock().isAcquiredBy(this)) {
                        container.getGammaLock().release(this);
                    }
                }
            }
        } else {
            for (Iterator i = lockingInfosIterator(); i.hasNext(); ) {
                LockingInfo lockingInfo = (LockingInfo) i.next();
                GammaContainer container = (GammaContainer) getGammaStore().containerForID(this, lockingInfo.getObjectID());
                if (container.getGammaLock().level(this) > Lock.LEVEL_NONE) {
                    container.lock().release(this);
                }
            }
        }
        deleteLogStorage();
        deleteCommittingStorage();
        deleteLockingInfoStorage();
        setStatus(STATUS_COMMITTED);
        if (logger.isLoggable(Level.FINE)) logger.fine("committed transaction " + this);
    }

    public void abort() throws IOException, ClassNotFoundException {
        setStatus(STATUS_ABORTING);
        if (logger.isLoggable(Level.FINE)) logger.fine("aborting transaction " + this);
        for (Iterator i = lockingInfosIterator(); i.hasNext(); ) {
            LockingInfo lockingInfo = (LockingInfo) i.next();
            GammaContainer container = (GammaContainer) getGammaStore().containerForID(this, lockingInfo.getObjectID());
            if (container != null) {
                if (lockingInfo.getType() == LockingInfo.LOCK_WRITE) {
                    if (container.getGammaLock().level(this) == Lock.LEVEL_WRITE) {
                        synchronized (container) {
                            getGammaStore().abort(container, lockingInfo.getContainerLocation());
                        }
                    }
                }
                if (container.getGammaLock().level(this) != Lock.LEVEL_NONE) {
                    container.lock().release(this);
                }
            }
        }
        getGammaStore().waitForSerializer(this);
        getGammaStore().getDataFileManager().flush();
        deleteLogStorage();
        deleteLockingInfoStorage();
        setStatus(STATUS_ABORTED);
        if (logger.isLoggable(Level.FINE)) logger.fine("aborted transaction " + this);
    }

    private void deleteLogStorage() throws IOException {
        getStorageFactory().delete(logStorageName());
    }

    private void deleteCommittingStorage() throws IOException {
        getStorageFactory().delete(committingStorageName());
    }

    private void deleteLockingInfoStorage() throws IOException {
        for (int i = 0; i < lockingInfosCount; i++) {
            getStorageFactory().delete(lockingInfoStorageName(i));
        }
    }

    public OzoneProxy objectForName(String name) throws Exception {
        OzoneProxy result = null;
        GammaContainer container = containerForName(name);
        if (container != null) {
            result = container.ozoneProxy();
        }
        return result;
    }

    /**
     * Returns <code>true</code> iff this instance is younger than the
     * transaction with the given ID.
     */
    private boolean isYounger(long transactionID) {
        return taID().value() < transactionID;
    }

    /**
     * Is called by a <code>GammaLock</code> when a lock on the given container
     * has been changed. Note that this takes place before <code>afterInvoke
     * </code>, which is good because we really need the taId of the previous
     * committer of the image that has been read (not read when new object/
     * container created by this ta).
     */
    void lockUpdated(GammaContainer container) {
        int level = container.getGammaLock().level(null);
        LockingInfo lockingInfo = null;
        if (level == Lock.LEVEL_READ) {
            lockingInfo = new LockingInfo(container.getObjectId());
        } else if (level == Lock.LEVEL_WRITE) {
            boolean wipeOnCommit = isYounger(container.getLastWritingTransactionId());
            lockingInfo = new LockingInfo(container.getObjectId(), container.getGammaLock().getContainerLocation(), wipeOnCommit);
            if (logger.isLoggable(Level.FINEST)) logger.finest("lockingInfo created for " + container.getObjectId() + " : " + lockingInfo);
        }
        if (lockingInfo != null) {
            if (logger.isLoggable(Level.FINER)) logger.finer("container " + container.getObjectId() + " was updated to lock level " + level + " and added to ta " + this);
            lockingInfos.add(lockingInfo);
            if (lockingInfos.size() > 100000) {
                if (logger.isLoggable(Level.FINE)) logger.fine("writing lock infos for " + this);
                try {
                    Storage storage = getStorageFactory().createStorage(lockingInfoStorageName(lockingInfosCount++));
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    ObjectOutputStream out = new ObjectOutputStream(buf);
                    out.writeObject(lockingInfos);
                    out.close();
                    storage.write(buf.toByteArray());
                    storage.close();
                    lockingInfos.clear();
                } catch (IOException e) {
                    throw new OzoneInternalException("could not write ta locking info", e);
                }
            }
        }
    }

    /**
     * Returns a bit crooked iterator; hasNext _has_ to be called before next.
     * Iterates over *all* LockingInfos for this transaction.
     */
    private Iterator lockingInfosIterator() {
        return new Iterator() {

            private int lockingInfosIndex = 0;

            private Iterator iterator;

            private boolean nextIterator() {
                if (lockingInfosIndex > lockingInfosCount) {
                    return false;
                }
                if (lockingInfosIndex == lockingInfosCount) {
                    iterator = lockingInfos.iterator();
                } else {
                    try {
                        Storage storage = getStorageFactory().createStorage(lockingInfoStorageName(lockingInfosIndex));
                        byte[] buf = new byte[(int) storage.getLength()];
                        storage.readFully(buf);
                        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buf));
                        List readList = (List) in.readObject();
                        iterator = readList.iterator();
                        storage.close();
                    } catch (Exception e) {
                        throw new OzoneInternalException("could not retrieve ta locking info", e);
                    }
                }
                lockingInfosIndex++;
                return true;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public boolean hasNext() {
                if (iterator == null) {
                    nextIterator();
                }
                if (iterator.hasNext()) {
                    return true;
                } else if (nextIterator()) {
                    return iterator.hasNext();
                } else {
                    return false;
                }
            }

            public Object next() {
                return iterator.next();
            }
        };
    }
}
