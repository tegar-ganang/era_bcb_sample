package org.ozoneDB.core.storage.gammaStore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ozoneDB.ObjectNotFoundException;
import org.ozoneDB.OzoneCompatible;
import org.ozoneDB.PermissionDeniedException;
import org.ozoneDB.DxLib.DxBag;
import org.ozoneDB.DxLib.DxHashSet;
import org.ozoneDB.DxLib.DxIterator;
import org.ozoneDB.DxLib.DxSet;
import org.ozoneDB.OzoneInternalException;
import org.ozoneDB.core.AbstractServerComponent;
import org.ozoneDB.core.Env;
import org.ozoneDB.core.GarbageCollector;
import org.ozoneDB.core.Lock;
import org.ozoneDB.core.ObjectContainer;
import org.ozoneDB.core.ObjectID;
import org.ozoneDB.core.Permissions;
import org.ozoneDB.core.PropertyConfigurableFactory;
import org.ozoneDB.core.PropertyInfo;
import org.ozoneDB.core.Server;
import org.ozoneDB.core.ServerComponent;
import org.ozoneDB.core.StoreManager;
import org.ozoneDB.core.Transaction;
import org.ozoneDB.core.TransactionID;
import org.ozoneDB.core.User;
import org.ozoneDB.core.storage.Cache;
import org.ozoneDB.core.storage.FixedSizeDelayCache;
import org.ozoneDB.core.storage.GZIPStreamFactory;
import org.ozoneDB.core.storage.GcListeningDelayCache;
import org.ozoneDB.core.storage.StreamFactory;
import org.ozoneDB.core.storage.TrimmingCache;
import org.ozoneDB.core.storage.VoidCache;
import org.ozoneDB.core.storage.SoftReferenceCache;
import org.ozoneDB.core.storage.WeakReferenceCache;
import org.ozoneDB.util.EnhProperties;

public class GammaStore extends AbstractServerComponent implements StoreManager {

    private static final int STAGE_SERIALIZE = 0;

    private static final int STAGE_STORE = 1;

    private static final class SizeMonitor {

        private long size;

        private long max;

        public SizeMonitor(long max) {
            this.max = max;
        }

        private boolean aboveMax() {
            return size > max;
        }

        public synchronized void checkSize() {
            while (aboveMax()) {
                if (log.isLoggable(Level.FINER)) log.finer("waiting because size == " + this.size + " > " + max);
                try {
                    wait();
                    if (log.isLoggable(Level.FINER)) log.finer("probably continuing; size == " + this.size);
                } catch (InterruptedException ignore) {
                }
            }
        }

        public synchronized void add(long size) {
            this.size += size;
        }

        public synchronized void remove(long size) {
            boolean wasAboveMax = aboveMax();
            this.size -= size;
            if (wasAboveMax && !aboveMax()) {
                notifyAll();
            }
            assert size >= 0;
        }
    }

    private SizeMonitor sizeMonitor = new SizeMonitor(500000);

    private final class WaitTask extends ExecPipeline.Task {

        private boolean readyWaiting = false;

        WaitTask(GammaTransaction transaction) {
            super(getContainerPersister(), transaction);
        }

        protected void aborted() {
            throw new IllegalStateException("wait task cannot be aborted");
        }

        protected void execute() {
            readyWaiting = getStage() == STAGE_STORE;
            if (readyWaiting) {
                synchronized (this) {
                    notifyAll();
                }
            }
        }

        public synchronized void waitUntilStageStore() {
            while (!readyWaiting) {
                try {
                    wait();
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

    private final class PersistContainerTask extends ExecPipeline.Task {

        private GammaContainer container;

        private byte[] containerImage;

        private byte[] lockImage;

        private boolean lockChanged;

        private int memSize;

        PersistContainerTask(GammaContainer container) {
            super(getContainerPersister(), container.getObjectId());
            this.container = container;
        }

        GammaContainer getGammaContainer() {
            return container;
        }

        protected void serialize() {
            try {
                assert !isReadOnly();
                synchronized (container) {
                    if (container.isWiped() || container.isAborted()) {
                        container.getGammaLock().setMemChanged(false);
                        container.setMemChanged(false);
                    } else if (container.isMemChanged()) {
                        assert container.getGammaLock().level(null) == Lock.LEVEL_WRITE : "expected write (" + Lock.LEVEL_WRITE + "), got " + container.getGammaLock().level(null) + " on " + container.getObjectId();
                        if (log.isLoggable(Level.FINEST)) log.finest("container " + container.id() + " has changed in memory");
                        ByteArrayOutputStream buf = new ByteArrayOutputStream();
                        GammaObjectOutputStream out;
                        if (getServer().getEncodeDecodeStreamFactory() != null) {
                            OutputStream encode = getServer().getEncodeDecodeStreamFactory().createOutputStream(buf);
                            out = new GammaObjectOutputStream(encode, getObjectStreamClasses());
                        } else {
                            out = new GammaObjectOutputStream(buf, getObjectStreamClasses());
                        }
                        if (log.isLoggable(Level.FINEST)) log.finest("serializing container " + container.id());
                        container.setMemChanged(false);
                        out.writeObject(container);
                        out.close();
                        containerImage = buf.toByteArray();
                        memSize = containerImage.length;
                    } else if (container.getGammaLock().isMemChanged()) {
                        lockChanged = true;
                        container.getGammaLock().setMemChanged(false);
                        if (container.getGammaLock().level(null) == Lock.LEVEL_READ) {
                            assert container.getGammaLock().getContainerLocation() != null : "readlock without containerlocation: " + container.getObjectId();
                            assert (container.getGammaLock().getOwnLocation() == null && container.getGammaLock().getContainerLocation().equals(getIndexManager().getContainerLocation(container.id().value()))) || (container.getGammaLock().getOwnLocation() != null && container.getGammaLock().getOwnLocation().equals(getIndexManager().getContainerLocation(container.id().value())));
                            ByteArrayOutputStream buf = new ByteArrayOutputStream();
                            GammaObjectOutputStream out;
                            if (getServer().getEncodeDecodeStreamFactory() != null) {
                                OutputStream encode = getServer().getEncodeDecodeStreamFactory().createOutputStream(buf);
                                out = new GammaObjectOutputStream(encode, getObjectStreamClasses());
                            } else {
                                out = new GammaObjectOutputStream(buf, getObjectStreamClasses());
                            }
                            if (log.isLoggable(Level.FINEST)) log.finest("serializing lock " + container.id() + " : " + container.getGammaLock());
                            out.writeObject(container.getGammaLock());
                            out.close();
                            lockImage = buf.toByteArray();
                            memSize = lockImage.length;
                        }
                    } else {
                    }
                }
            } catch (IOException e) {
                throw new OzoneInternalException("could not serialize container " + container.getObjectId());
            }
        }

        /**
         * See package.html if you like to try to understand whats going on here.
         */
        protected void store() {
            synchronized (container) {
                GammaLock lock = container.getGammaLock();
                if (containerImage != null) {
                    if (lock.firstWrite()) {
                        if (lock.getOwnLocation() != null) {
                            if (log.isLoggable(Level.FINEST)) log.finest("(now writelock) free image lock " + container.id() + " on " + lock.getOwnLocation());
                            free(lock.getOwnLocation());
                            lock.setOwnLocation(null);
                        }
                    } else {
                        if (log.isLoggable(Level.FINEST)) log.finest("free container location " + container.id() + " on " + lock.getContainerLocation());
                        free(lock.getContainerLocation(), true);
                    }
                    ContainerLocation containerLocation = rawPersist(containerImage);
                    if (log.isLoggable(Level.FINEST)) log.finest("written container " + container.id() + " to " + containerLocation);
                    lock.setContainerLocation(containerLocation);
                    getIndexManager().putContainerLocation(container.getObjectId().value(), containerLocation);
                } else if (lockChanged) {
                    if (lockImage != null) {
                        if (lock.getOwnLocation() == null) {
                            ContainerLocation lockLocation = rawPersist(lockImage);
                            if (log.isLoggable(Level.FINEST)) log.finest("written lock (lockers>=1, 'f'ts) " + container.id() + " to " + lockLocation);
                            lock.setOwnLocation(lockLocation);
                            getIndexManager().putContainerLocation(container.getObjectId().value(), lockLocation);
                        } else {
                            if (log.isLoggable(Level.FINEST)) log.finest("(readlock 0) free image lock " + container.id() + " on " + lock.getOwnLocation());
                            free(lock.getOwnLocation());
                            ContainerLocation lockLocation = rawPersist(lockImage);
                            if (log.isLoggable(Level.FINEST)) log.finest("written lock (lockers>=1, n-th ts) " + container.id() + " to " + lockLocation);
                            lock.setOwnLocation(lockLocation);
                            getIndexManager().putContainerLocation(container.getObjectId().value(), lockLocation);
                        }
                    } else {
                        if (lock.getOwnLocation() != null) {
                            if (log.isLoggable(Level.FINEST)) log.finest("(readlock 1) free image lock " + container.id() + " on " + lock.getOwnLocation());
                            free(lock.getOwnLocation());
                            lock.setOwnLocation(null);
                            if (log.isLoggable(Level.FINEST)) log.finest("setting index from lock back to container for " + container.id() + " to " + lock.getContainerLocation());
                            getIndexManager().putContainerLocation(container.getObjectId().value(), lock.getContainerLocation());
                        } else {
                            assert lock.getContainerLocation().equals(getIndexManager().getContainerLocation(container.getObjectId().value()));
                        }
                    }
                }
            }
        }

        public void aborted() {
            synchronized (container) {
                if (containerImage != null) {
                    container.setMemChanged(true);
                } else {
                    hasBecomeMemChanged(container);
                }
            }
            removeMemSize();
        }

        private synchronized void removeMemSize() {
            sizeMonitor.remove(memSize);
            memSize = 0;
        }

        public void execute() {
            switch(getStage()) {
                case STAGE_SERIALIZE:
                    sizeMonitor.checkSize();
                    serialize();
                    removeMemSize();
                    break;
                case STAGE_STORE:
                    try {
                        store();
                    } finally {
                        removeMemSize();
                    }
                    break;
            }
        }

        protected void finalize() {
            if (memSize > 0) {
                System.out.println("\n\n##########" + getKey() + " memSize " + memSize + "\n\n");
            }
        }
    }

    private static final Logger log = Logger.getLogger(GammaStore.class.getName());

    public static final String GAMMASTORE_BASE = Server.CONFIG_OZONE_BASE + ".gammaStore";

    private static final String INDEXMANAGER_BASE = GAMMASTORE_BASE + ".indexManager";

    private static final String DATAFILEMANAGER_BASE = GAMMASTORE_BASE + ".dataFileManager";

    private static final String OBJECT_STREAM_CLASSES_BASE = GAMMASTORE_BASE + ".objectStreamClasses";

    public static final PropertyInfo DIRECTORY = new PropertyInfo(GAMMASTORE_BASE + ".directory", String.class, "main database directory, must be an absolute path" + " (note: the value of this property is always overridden by the directory" + " specified when starting the server, in other words: do not use this property)", new String[] { "/var/my_ozone_db/ (*nix absolute path)", "c:\\my_ozone_db\\ (windows absolute path)" });

    private static final String TX_STORAGE_FACTORY_KEY = ".txStorageFactory";

    private static Properties txStorageFactoryDefaultProperties = new Properties();

    static {
        txStorageFactoryDefaultProperties.setProperty(TX_STORAGE_FACTORY_KEY + FileStreamStorageFactory.DIRECTORY.getKey(), EnhProperties.PRE_REPLACE + DIRECTORY.getKey() + EnhProperties.POST_REPLACE + "tx/");
    }

    public static final PropertyInfo TXSTORAGEFACTORY = new PropertyInfo(TX_STORAGE_FACTORY_KEY, StorageFactory.class, "transaction data directory", new String[] { "/var/my_ozone_db/tx/ (*nix absolute path)", "c:\\my_ozone_db\\tx\\ (windows absolute path)" }, FileStreamStorageFactory.class.getName(), txStorageFactoryDefaultProperties);

    private final PropertyInfo STORAGEFACTORY = new PropertyInfo(GAMMASTORE_BASE + ".storageFactory", StorageFactory.class, "factory for creating object containers", new String[] { FileStreamStorageFactory.class.getName() }, FileStreamStorageFactory.class.getName(), new Properties());

    private static final PropertyInfo NUMCONFIGS = new PropertyInfo(GAMMASTORE_BASE + ".numConfigs", Integer.TYPE, "number of configurations (must currently be 1)", new String[] { "1" }, "1");

    private static final PropertyInfo READ_ONLY = new PropertyInfo(GAMMASTORE_BASE + ".readOnly", Boolean.TYPE, "read only state: if set to 'true', then no writing whatsoever takes" + " place on the database files; you might even have 2 database" + " servers using the same database files (though this is not good" + " practice)", new String[] { "true", "false" }, "false");

    private static final String PROP_STREAMFACTORY = ".streamFactory";

    private static final String DIRTY_CONTAINER_CACHE_KEY = ".dirtyContainerCache";

    private static Properties dirtyContainerCacheDefaultProperties = new Properties();

    static {
        dirtyContainerCacheDefaultProperties.setProperty(DIRTY_CONTAINER_CACHE_KEY + GcListeningDelayCache.DELAY.getKey(), Integer.toString(0));
        dirtyContainerCacheDefaultProperties.setProperty(DIRTY_CONTAINER_CACHE_KEY + GcListeningDelayCache.HIGH_THRESHOLD.getKey(), "P0.35");
        dirtyContainerCacheDefaultProperties.setProperty(DIRTY_CONTAINER_CACHE_KEY + GcListeningDelayCache.LOW_THRESHOLD.getKey(), Integer.toString(0));
        dirtyContainerCacheDefaultProperties.setProperty(DIRTY_CONTAINER_CACHE_KEY + GcListeningDelayCache.THREAD_NAME.getKey(), "dirty container cache trimmer");
    }

    public static final PropertyInfo DIRTYCONTAINERCACHE = new PropertyInfo(DIRTY_CONTAINER_CACHE_KEY, TrimmingCache.class, "cache for caching index nodes that have to be written to disk", new String[] { FixedSizeDelayCache.class.getName() }, GcListeningDelayCache.class.getName(), dirtyContainerCacheDefaultProperties);

    private static Properties objectStreamClassesDefaultProperties = new Properties();

    static {
        objectStreamClassesDefaultProperties.setProperty(ObjectStreamClasses.DIRECTORY.getKey(), EnhProperties.PRE_REPLACE + DIRECTORY.getKey() + EnhProperties.POST_REPLACE + "classDesc/");
    }

    public static final PropertyInfo OBJECT_STREAM_CLASSES = new PropertyInfo("", ObjectStreamClasses.class, "external location for storing ObjectStreamClass instances outside of an ObjectOutputStream", new String[] { ObjectStreamClasses.class.getName() }, ObjectStreamClasses.class.getName(), objectStreamClassesDefaultProperties);

    private static final String CONTAINERMAPFILENAME = "names";

    private static final String SHUTDOWNFILENAME = "shutdown";

    private StreamFactory[] streamFactories;

    private IndexManager indexManager;

    private DataFileManager dataFileManager;

    private FreeSpaceManager freeSpaceManager;

    private EnhProperties properties;

    private Map containerNames;

    /**
     * The garbage collector. It should be notified in the event
     * <ul><li>that a formerly unnamed object receives a name.</li>
     * <li>that an object is freshly created</li>
     * </ul>
     */
    private GarbageCollector garbageCollector;

    private StorageFactory txStorageFactory;

    private ObjectStreamClasses objectStreamClasses;

    /**
     * holds all dirty containers
     */
    private TrimmingCache dirtyContainerCache;

    /**
     * Holds all containers, dirty an not dirty.
     */
    private Cache backupContainerCache;

    /**
     * Used exclusively in <code>containerForId</code> to keep track of objects
     * that are in the process of being read. Contains <code>ObjectId</code>s.
     */
    private final Collection readingObjectIds = new HashSet();

    private ExecPipeline containerPersister;

    private boolean readOnly;

    /**
     * While not yet supported there is a possiblility to have multiple
     * configurations. These could be used to for instance save instances of a
     * specific class to a different disk because of performance reasons
     * Because different configurations allow for different streams to be used
     * during (de)serialization, this could also be used to encrypt only certain
     * objects.
     */
    public GammaStore(Server server) {
        super(server);
        this.properties = server.getConfig();
        getProperties().setTranslating(true);
        readOnly = Boolean.valueOf(getProperties().getProperty(READ_ONLY.getKey(), READ_ONLY.getDefaultValue())).booleanValue();
        containerPersister = new ExecPipeline(2, "container persister");
        Properties defaultStoreProps = getDefaultProperties();
        for (Enumeration e = defaultStoreProps.propertyNames(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            if (!getProperties().containsKey(key)) {
                getProperties().setProperty(key, defaultStoreProps.getProperty(key));
            }
        }
        getProperties().setProperty(DIRECTORY.getKey(), server.getPath());
        setGarbageCollector(getServer().getGarbageCollector());
        indexManager = (IndexManager) PropertyConfigurableFactory.create(IndexManager.class, getProperties(), INDEXMANAGER_BASE);
        dirtyContainerCache = ((TrimmingCache) PropertyConfigurableFactory.create(Cache.class, getProperties(), GAMMASTORE_BASE, DIRTYCONTAINERCACHE));
        getDirtyContainerCache().setTrimHandler(new TrimmingCache.TrimHandler() {

            public void trimming(Object key, Object value) {
                GammaContainer container = (GammaContainer) value;
                if (log.isLoggable(Level.FINER)) log.finer("container " + container.id() + " is trimmed from dirty cache, put into persister");
                getContainerPersister().put(new PersistContainerTask(container));
            }
        });
        backupContainerCache = new SoftReferenceCache();
    }

    private TrimmingCache getDirtyContainerCache() {
        return dirtyContainerCache;
    }

    private Cache getBackupContainerCache() {
        return backupContainerCache;
    }

    void abortObjectId(GammaTransaction ta, ObjectID objectId) throws IOException, ClassNotFoundException {
        throw new UnsupportedOperationException("should be handled in GammaTransaction");
    }

    /** Force the Store to make a guess which objects are used together with the
     * container with the specified id.
     * @param id The ObjectID if the container.
     *
     */
    public DxBag clusterOfID(ObjectID id) throws Exception {
        throw new RuntimeException("should not be called");
    }

    private Object retrieveObject(ContainerLocation location) throws ClassNotFoundException, IOException {
        if (log.isLoggable(Level.FINEST)) log.finest("reading object from location " + location);
        byte[] data = getDataFileManager().retrieveData(location);
        location.setSize(data.length);
        InputStream buf = new ByteArrayInputStream(data);
        GammaObjectInputStream in;
        if (getServer().getEncodeDecodeStreamFactory() != null) {
            InputStream decode = getServer().getEncodeDecodeStreamFactory().createInputStream(buf);
            in = new GammaObjectInputStream(decode, getObjectStreamClasses());
        } else {
            in = new GammaObjectInputStream(buf, getObjectStreamClasses());
        }
        in.setDatabase(getServer().getDatabase());
        return in.readObject();
    }

    /**
     * <p>Makes sure a container will be serialized and written to storage.
     * <code>waitForSerializer</code> needs to be called (once) after calling
     * this method (multiple times) to make sure all writing has taken place.
     * </p>
     *
     * <p>Needs only be called for write-locked containers.</p>
     */
    public void preCommit(ObjectID objectID) throws IOException {
        if (log.isLoggable(Level.FINEST)) log.finest("precommitting object " + objectID);
        GammaContainer container = getFromCaches(objectID);
        if (container != null) {
            synchronized (container) {
                if (container.isMemChanged()) {
                    getContainerPersister().put(new PersistContainerTask(container));
                }
            }
        }
    }

    /**
     * Used to 'synchronize' the write actions in the persister with the
     * current thread. Serializers handle tasks in the order they are put in,
     * so we insert a task that notifies this thread if that tasks <code>store
     * </code> method has been called; this means that all previous tasks <code>
     * store</code> methods have been called, or an exception has occured. To
     * handle this last case caller should always iterate the persisters
     * store and serialize exceptions.
     */
    void waitForSerializer(GammaTransaction transaction) {
        WaitTask wait = new WaitTask(transaction);
        getContainerPersister().put(wait);
        wait.waitUntilStageStore();
    }

    /**
     * Fixates a container. Needs only to be called for write-locked containers.
     * Should be called only by a transaction that has already
     * committed and is cleaning up, or a transaction that 'knows' its commit
     * phase will succeed, for there is no turning back on old data after a
     * call to this method.
     */
    GammaContainer commit(ObjectID objectID, ContainerLocation free, boolean wipe) {
        if (log.isLoggable(Level.FINEST)) log.finest("committing object " + objectID);
        GammaContainer container = getFromCaches(objectID);
        if (container != null) {
            synchronized (container) {
                assert !container.isMemChanged() : "cannot fixate in memory changed container " + container.id();
                if (container.isDeleted()) {
                    String name = container.name();
                    if (name != null) {
                        removeContainerName(name);
                    }
                    container.setWiped();
                    if (container.getGammaLock().getContainerLocation() != null) {
                        getIndexManager().removeContainerLocation(container.getObjectId().value());
                    }
                }
            }
        }
        if (free != null) {
            free(free, wipe);
        }
        return container;
    }

    /**
     * Rolls back changes on a container; removes uncommitted (new) image.
     */
    void abort(GammaContainer container, ContainerLocation committedLocation) {
        if (isReadOnly()) {
            return;
        }
        if (log.isLoggable(Level.FINEST)) log.finest("aborting object " + container.id());
        ContainerLocation location = container.getGammaLock().getContainerLocation();
        if (location != null) {
            free(location, true);
        }
        if (committedLocation == null) {
            if (getIndexManager().getContainerLocation(container.getObjectId().value()) != null) {
                getIndexManager().removeContainerLocation(container.getObjectId().value());
            }
        } else {
            getIndexManager().putContainerLocation(container.getObjectId().value(), committedLocation);
        }
        container.setAborted();
    }

    private void free(ContainerLocation location) {
        free(location, false);
    }

    private void free(ContainerLocation location, boolean wipe) {
        checkReadOnly("cannot free on read only store; this should never happen");
        if (wipe) {
            getDataFileManager().wipeData(location, location.getSize());
        }
        getFreeSpaceManager().registerFreeSpace(location, location.getSize());
    }

    /**
     * Writes a block of data to an empty location.
     *
     * @param rawData block to be written
     *
     * @return location to a previously empty block that now contains <code>
     * rawData</code>
     */
    private ContainerLocation rawPersist(byte[] rawData) {
        checkReadOnly("cannot write on read only store; this should never happen");
        int totalSize = rawData.length + DataFileManager.getDataHeaderSize();
        ContainerLocation result;
        FreeSpace freeSpace;
        synchronized (getDataFileManager()) {
            freeSpace = getFreeSpaceManager().findFreeSpace(totalSize);
            if (freeSpace == null) {
                if (log.isLoggable(Level.FINEST)) log.finest("no free space: creating new data file");
                int dataFileId = getDataFileManager().createDataFile();
                freeSpace = new FreeSpace(dataFileId, 0, getDataFileManager().getMaxDataFileSize());
            }
            FreeSpace newFreeSpace = getFreeSpaceManager().divide(freeSpace, totalSize);
            if (newFreeSpace != null) {
                if (log.isLoggable(Level.FINEST)) log.finest("free space was split into " + freeSpace + " and " + newFreeSpace);
                ContainerLocation loc = new ContainerLocation(newFreeSpace.getDataFileId(), newFreeSpace.getLocation());
                getDataFileManager().wipeData(loc, newFreeSpace.getSize());
                getFreeSpaceManager().registerFreeSpace(newFreeSpace);
            }
        }
        result = new ContainerLocation(freeSpace.getDataFileId(), freeSpace.getLocation(), freeSpace.getSize());
        if (log.isLoggable(Level.FINEST)) log.finest("storing data in " + freeSpace);
        getDataFileManager().storeData(result, rawData, freeSpace.getSize());
        return result;
    }

    private GammaContainer getFromCaches(ObjectID id) {
        GammaContainer result = (GammaContainer) getBackupContainerCache().get(id);
        if (result != null) {
            if (result.isWiped() || result.isAborted()) {
                result = null;
            } else {
                if (result.isMemChanged() || result.getGammaLock().isMemChanged()) {
                    getDirtyContainerCache().get(id);
                    getContainerPersister().abort(id);
                }
            }
        }
        return result;
    }

    /**
     * @param container container to be put gammastore caches
     */
    private void putInCaches(GammaContainer container) {
        assert getFromCaches(container.getObjectId()) == null : "already in some cache: " + container.getObjectId();
        getBackupContainerCache().put(container.getObjectId(), container);
        if (!isReadOnly() && container.isMemChanged()) {
            getDirtyContainerCache().put(container.getObjectId(), container);
        }
    }

    /**
     * Is called by a <code>GammaContainer</code> when goes through transition
     * from <code>isMemChanged() == false</code> to </code>isMemChanged() ==
     * true</code>.
     */
    void hasBecomeMemChanged(GammaContainer container) {
        if (!isReadOnly()) {
            getDirtyContainerCache().put(container.getObjectId(), container);
        }
    }

    /**
     * When a container is retrieved through a cache, it is automatically
     * touched. This leaves nothing to do here for gamma.
     */
    public void touch(GammaContainer container) {
    }

    public ObjectContainer containerForID(Transaction ta, ObjectID id) throws ObjectNotFoundException, IOException, ClassNotFoundException {
        GammaContainer result = getFromCaches(id);
        boolean readContainer = false;
        if (result == null) {
            synchronized (readingObjectIds) {
                while (readingObjectIds.contains(id)) {
                    try {
                        if (log.isLoggable(Level.FINER)) log.finer("waiting until another thread retrieves object " + id + "; being read are: " + readingObjectIds);
                        readingObjectIds.wait();
                    } catch (InterruptedException ignore) {
                    }
                }
                result = getFromCaches(id);
                if (result == null) {
                    if (log.isLoggable(Level.FINER)) log.finer("object " + id + " not found in caches; putting id into readingObjectIds collection; already there: " + readingObjectIds);
                    readContainer = true;
                    readingObjectIds.add(id);
                } else {
                    if (log.isLoggable(Level.FINEST)) log.finest("object " + id + " found in caches");
                }
            }
        }
        try {
            if (result == null) {
                memCheck();
                ContainerLocation location = getIndexManager().getContainerLocation(id.value());
                if (location == null) {
                    if (log.isLoggable(Level.FINER)) log.finer("index has no location for object with id " + id);
                    throw new ObjectNotFoundException("no such object id: " + id);
                } else {
                    if (log.isLoggable(Level.FINER)) log.finer("retrieving object with id " + id);
                    Object read = retrieveObject(location);
                    if (read == null) {
                        throw new OzoneInternalException("index manager reports object " + id + " at " + location + ", but that location returns a null");
                    } else {
                        GammaLock lock;
                        if (read instanceof GammaLock) {
                            lock = (GammaLock) read;
                            lock.setOwnLocation(location);
                            if (log.isLoggable(Level.FINEST)) log.finest("retrieved a lock for " + id + ": " + lock);
                            location = lock.getContainerLocation();
                            assert location != null : "lock was written without reference to a container: " + id;
                            result = (GammaContainer) retrieveObject(location);
                            result.initTransients(id, this, lock, getServer());
                            lock.initTransients(result);
                        } else {
                            if (log.isLoggable(Level.FINEST)) log.finest("retrieved a container (without lock) for " + id);
                            result = (GammaContainer) read;
                            lock = new GammaLock(result, location);
                            result.initTransients(id, this, lock, getServer());
                            TransactionID taID = new TransactionID(result.getLastWritingTransactionId());
                            GammaTransaction lastWriter = (GammaTransaction) getServer().getTransactionManager().taForID(taID);
                            if (lastWriter != null) {
                                lastWriter.isThisYours(result);
                            } else {
                                if (log.isLoggable(Level.FINE)) log.fine("last writer " + result.getLastWritingTransactionId() + " for " + id + " not in transaction manager");
                            }
                            result.initTransients(id, this, lock, getServer());
                        }
                        putInCaches(result);
                    }
                }
            }
        } finally {
            if (readContainer) {
                synchronized (readingObjectIds) {
                    readingObjectIds.remove(id);
                    if (log.isLoggable(Level.FINER)) log.finer("removing " + id + " from readingObjectIds collection; still there: " + readingObjectIds);
                    readingObjectIds.notifyAll();
                }
            }
        }
        return result;
    }

    /** @param name The object name to search for.
     * @param ta
     * @return The object container for the name or null.
     *
     */
    public ObjectContainer containerForName(Transaction ta, String name) throws Exception {
        GammaTransaction gammaTransaction = (GammaTransaction) ta;
        ObjectID id = (ObjectID) getContainerNames().get(name);
        ObjectContainer result = null;
        if (id != null) {
            if (log.isLoggable(Level.FINER)) log.finer("object name '" + name + "' gives id " + id);
            result = gammaTransaction.acquireObject(id, Lock.LEVEL_READ);
        } else {
            if (log.isLoggable(Level.FINER)) log.finer("no id for object name '" + name + "'");
        }
        return result;
    }

    /** @param ta
     * @param container
     * @param name
     *
     */
    public void nameContainer(Transaction ta, ObjectContainer container, String name) throws PermissionDeniedException {
        throw new UnsupportedOperationException("should be handled in GammaTransaction");
    }

    /** Creates a new object container and initializes it with the specified
     * target object. The new container is immediatly accessible from the calling
     * transaction via containerByID but it is not joined to this transaction.
     * It needs to be joined and committed afterwards.
     *
     * @param ta
     * @param target
     * @param objectId
     * @param permissions
     * @param lockLevel
     * @return An container-proxy for the created container.
     *
     */
    public ObjectContainer newContainerAndLock(Transaction ta, OzoneCompatible target, ObjectID objectId, Permissions permissions, int lockLevel) throws Exception {
        if (isReadOnly()) {
            throw new PermissionDeniedException("cannot create object in read only database");
        }
        memCheck();
        GammaContainer result = new GammaContainer(objectId, this, getServer(), (GammaTransaction) ta, target);
        putInCaches(result);
        if (log.isLoggable(Level.FINEST)) log.finest("created new container " + result);
        getGarbageCollector().notifyNewObjectContainer(result);
        return result;
    }

    public Object newTransactionData() {
        return null;
    }

    public DxIterator objectIDIterator() {
        return null;
    }

    /** @param ta the running transaction
     * @return a String array of the all object names defined
     *
     */
    public DxSet objectNames(Transaction ta) {
        DxSet result = new DxHashSet();
        for (Iterator i = getContainerNames().keySet().iterator(); i.hasNext(); ) {
            result.add(i.next());
        }
        return result;
    }

    public void reportNamedObjectsToGarbageCollector() {
        synchronized (this) {
            for (Iterator i = getContainerNames().values().iterator(); i.hasNext(); ) {
                ObjectID id = (ObjectID) i.next();
                if (id != null) {
                    garbageCollector.notifyNamedObject(id);
                }
            }
        }
    }

    public void shutdown() {
        indexManager.shutdown();
        dataFileManager.flush();
        freeSpaceManager.shutdown();
        File shutdownFile = new File(getDirectory(), SHUTDOWNFILENAME);
        try {
            if (!isReadOnly()) {
                if (!shutdownFile.createNewFile()) {
                    throw new OzoneInternalException("cannot create shutdown file " + shutdownFile);
                }
            }
        } catch (IOException e) {
            throw new OzoneInternalException("cannot create shutdown file " + shutdownFile, e);
        }
    }

    public void startup() {
        File shutdownFile = new File(getDirectory(), SHUTDOWNFILENAME);
        boolean fullInit = !shutdownFile.exists();
        if (fullInit) {
            checkReadOnly("cannot start incorrectly shutdown database in readonly mode");
        }
        try {
            if (fullInit) {
                createContainerNames(null);
                commitContainerNames();
            } else {
                if (!isReadOnly()) {
                    if (!shutdownFile.delete()) {
                        throw new IOException("could not delete shutdown file " + shutdownFile);
                    }
                }
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(new File(getDirectory(), CONTAINERMAPFILENAME)));
                createContainerNames(in);
                log.info(getContainerNames().size() + " named object" + (getContainerNames().size() == 1 ? "" : "s") + " found");
                in.close();
            }
            indexManager.startup(false);
            dataFileManager = (DataFileManager) PropertyConfigurableFactory.create(DataFileManager.class, getProperties(), DATAFILEMANAGER_BASE);
            freeSpaceManager = new FreeSpaceManager(getProperties(), GAMMASTORE_BASE, fullInit);
            setObjectStreamClasses((ObjectStreamClasses) PropertyConfigurableFactory.create(ObjectStreamClasses.class, getProperties(), OBJECT_STREAM_CLASSES_BASE, OBJECT_STREAM_CLASSES));
            getObjectStreamClasses().startup(fullInit);
        } catch (Exception e) {
            throw new OzoneInternalException("could not startup", e);
        }
        int numConfigs = Integer.parseInt(getProperties().getProperty(NUMCONFIGS.getKey()));
        if (numConfigs != 1) {
            throw new OzoneInternalException("currently only 1 config supported");
        }
        streamFactories = new StreamFactory[numConfigs];
        txStorageFactory = (StorageFactory) PropertyConfigurableFactory.create(StorageFactory.class, getProperties(), GAMMASTORE_BASE, TXSTORAGEFACTORY);
    }

    private void commitContainerNames() throws IOException {
        checkReadOnly("cannot commit container names on read only store; this should never happen");
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(getDirectory(), CONTAINERMAPFILENAME)));
        out.writeObject(getContainerNames());
        out.close();
    }

    /** 
     * TODO: why this method and not use state? Should gamma implement this at all?
     */
    public void updateLockLevel(Transaction ta, ObjectContainer container) throws IOException {
    }

    public Transaction createTransaction(User user) {
        return new GammaTransaction(this, getTxStorageFactory(), getServer(), user);
    }

    private StorageFactory getTxStorageFactory() {
        return txStorageFactory;
    }

    /**
     * Contains a mapping from <code>String</code> with container (object) names
     * to <code>ObjectID</code>. The returned map is thread safe (synchronized).
     */
    Map getContainerNames() {
        return containerNames;
    }

    private void removeContainerName(String name) {
        synchronized (getContainerNames()) {
            getContainerNames().remove(name);
        }
    }

    void updateContainerNames(Map newContainerNames) throws IOException {
        synchronized (getContainerNames()) {
            for (Iterator i = newContainerNames.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry) i.next();
                String name = (String) entry.getKey();
                ObjectID id = (ObjectID) entry.getValue();
                if (id == null) {
                    if (log.isLoggable(Level.FINEST)) log.finest("removing name '" + name + "'");
                    getContainerNames().remove(name);
                } else {
                    if (log.isLoggable(Level.FINEST)) {
                        ObjectID previousId = (ObjectID) getContainerNames().get(name);
                        if (previousId == null) {
                            log.finest("inserting name '" + name + "' with id " + id);
                        } else {
                            log.finest("overwriting id " + previousId + " for '" + name + "' with id " + id);
                        }
                    }
                    getContainerNames().put(name, id);
                }
            }
            commitContainerNames();
        }
    }

    private void createContainerNames(ObjectInputStream in) throws IOException, ClassNotFoundException {
        containerNames = in == null ? Collections.synchronizedMap(new HashMap()) : (Map) in.readObject();
    }

    private GarbageCollector getGarbageCollector() {
        return garbageCollector;
    }

    private void setGarbageCollector(GarbageCollector garbageCollector) {
        this.garbageCollector = garbageCollector;
    }

    private IndexManager getIndexManager() {
        return indexManager;
    }

    DataFileManager getDataFileManager() {
        return dataFileManager;
    }

    private FreeSpaceManager getFreeSpaceManager() {
        return freeSpaceManager;
    }

    private EnhProperties getProperties() {
        return properties;
    }

    private File getDirectory() {
        return new File(getProperties().getProperty(DIRECTORY.getKey()));
    }

    public Properties getDefaultProperties() {
        Properties result = new Properties();
        result.setProperty(NUMCONFIGS.getKey(), NUMCONFIGS.getDefaultValue());
        return result;
    }

    public void createDatabase() {
        indexManager.startup(true);
        indexManager.shutdown();
    }

    public boolean needsEnvMemCalc() {
        return false;
    }

    public ObjectStreamClasses getObjectStreamClasses() {
        return objectStreamClasses;
    }

    private void setObjectStreamClasses(ObjectStreamClasses objectStreamClasses) {
        this.objectStreamClasses = objectStreamClasses;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Throws IllegalStateException if database is read only.
     */
    public void checkReadOnly(String msg) {
        if (isReadOnly()) {
            throw new IllegalStateException(msg);
        }
    }

    /**
     * Throws IllegalStateException if database is read only.
     */
    public void checkReadOnly() {
        checkReadOnly("database is read only");
    }

    ExecPipeline getContainerPersister() {
        return containerPersister;
    }

    private void memCheck() {
        if (Runtime.getRuntime().freeMemory() < Runtime.getRuntime().totalMemory() / 20) {
            try {
                if (log.isLoggable(Level.FINE)) log.fine("short on memory, container persister size: " + getContainerPersister().size());
                Thread.sleep(1 + (getContainerPersister().size() / 50));
            } catch (InterruptedException ignore) {
            }
        }
    }
}
