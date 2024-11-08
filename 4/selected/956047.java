package org.exist.storage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.exist.Database;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.collections.CollectionCache;
import org.exist.collections.CollectionConfiguration;
import org.exist.collections.CollectionConfigurationManager;
import org.exist.collections.triggers.DocumentTrigger;
import org.exist.collections.triggers.DocumentTriggerProxy;
import org.exist.collections.triggers.TriggerException;
import org.exist.config.ConfigurationDocumentTrigger;
import org.exist.config.Configurator;
import org.exist.config.annotation.ConfigurationClass;
import org.exist.config.annotation.ConfigurationFieldAsAttribute;
import org.exist.debuggee.Debuggee;
import org.exist.debuggee.DebuggeeFactory;
import org.exist.dom.SymbolTable;
import org.exist.indexing.IndexManager;
import org.exist.management.AgentFactory;
import org.exist.numbering.DLNFactory;
import org.exist.numbering.NodeIdFactory;
import org.exist.plugin.PluginsManagerImpl;
import org.exist.scheduler.Scheduler;
import org.exist.scheduler.SystemTaskJob;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.security.SecurityManager;
import org.exist.security.Subject;
import org.exist.security.internal.SecurityManagerImpl;
import org.exist.storage.btree.DBException;
import org.exist.storage.lock.DeadlockDetection;
import org.exist.storage.lock.FileLock;
import org.exist.storage.lock.Lock;
import org.exist.storage.lock.ReentrantReadWriteLock;
import org.exist.storage.sync.Sync;
import org.exist.storage.sync.SyncTask;
import org.exist.storage.txn.TransactionException;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.util.Configuration;
import org.exist.util.DatabaseConfigurationException;
import org.exist.util.ReadOnlyException;
import org.exist.util.XMLReaderObjectFactory;
import org.exist.util.XMLReaderPool;
import org.exist.xmldb.ShutdownListener;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.PerformanceStats;

@ConfigurationClass("pool")
public class BrokerPool extends Observable implements Database {

    private static final Logger LOG = Logger.getLogger(BrokerPool.class);

    private static final TreeMap<String, BrokerPool> instances = new TreeMap<String, BrokerPool>();

    /*** initializing subcomponents */
    public static final String SIGNAL_STARTUP = "startup";

    /*** ready for recovery & read-only operations */
    public static final String SIGNAL_READINESS = "readiness";

    /*** ready for writable operations */
    public static final String SIGNAL_RIDEABLE = "rideable";

    /*** running shutdown sequence */
    public static final String SIGNAL_SHUTDOWN = "shutdown";

    /**
     * The name of a default database instance for those who are too lazy to provide parameters ;-). 
     */
    public static final String DEFAULT_INSTANCE_NAME = "exist";

    public static final String CONFIGURATION_CONNECTION_ELEMENT_NAME = "db-connection";

    public static final String CONFIGURATION_POOL_ELEMENT_NAME = "pool";

    public static final String CONFIGURATION_SECURITY_ELEMENT_NAME = "security";

    public static final String CONFIGURATION_RECOVERY_ELEMENT_NAME = "recovery";

    public static final String DATA_DIR_ATTRIBUTE = "files";

    public static final String RECOVERY_ENABLED_ATTRIBUTE = "enabled";

    public static final String RECOVERY_POST_RECOVERY_CHECK = "consistency-check";

    public static final String COLLECTION_CACHE_SIZE_ATTRIBUTE = "collectionCacheSize";

    public static final String MIN_CONNECTIONS_ATTRIBUTE = "min";

    public static final String MAX_CONNECTIONS_ATTRIBUTE = "max";

    public static final String SYNC_PERIOD_ATTRIBUTE = "sync-period";

    public static final String SHUTDOWN_DELAY_ATTRIBUTE = "wait-before-shutdown";

    public static final String NODES_BUFFER_ATTRIBUTE = "nodesBuffer";

    public static final String PROPERTY_DATA_DIR = "db-connection.data-dir";

    public static final String PROPERTY_MIN_CONNECTIONS = "db-connection.pool.min";

    public static final String PROPERTY_MAX_CONNECTIONS = "db-connection.pool.max";

    public static final String PROPERTY_SYNC_PERIOD = "db-connection.pool.sync-period";

    public static final String PROPERTY_SHUTDOWN_DELAY = "wait-before-shutdown";

    public static final String PROPERTY_COLLECTION_CACHE_SIZE = "db-connection.collection-cache-size";

    public static final String DEFAULT_SECURITY_CLASS = "org.exist.security.internal.SecurityManagerImpl";

    public static final String PROPERTY_SECURITY_CLASS = "db-connection.security.class";

    public static final String PROPERTY_RECOVERY_ENABLED = "db-connection.recovery.enabled";

    public static final String PROPERTY_RECOVERY_CHECK = "db-connection.recovery.consistency-check";

    public static final String PROPERTY_SYSTEM_TASK_CONFIG = "db-connection.system-task-config";

    public static final String PROPERTY_NODES_BUFFER = "db-connection.nodes-buffer";

    public static final String PROPERTY_EXPORT_ONLY = "db-connection.emergency";

    public static final String DOC_ID_MODE_ATTRIBUTE = "doc-ids";

    public static final String DOC_ID_MODE_PROPERTY = "db-connection.doc-ids.mode";

    private static final Thread shutdownHook = new Thread() {

        /**
         * Make sure that all instances are cleanly shut down.
         */
        @Override
        public void run() {
            LOG.info("Executing shutdown thread");
            BrokerPool.stopAll(true);
        }
    };

    private static boolean registerShutdownHook = true;

    private static Observer statusObserver = null;

    public static final void setRegisterShutdownHook(boolean register) {
        registerShutdownHook = register;
    }

    /**
     * For testing only: triggers a database corruption by disabling the page caches. The effect is
     * similar to a sudden power loss or the jvm being killed. The flag is used by some
     * junit tests to test the recovery process.
     */
    public static boolean FORCE_CORRUPTION = false;

    public static final void configure(int minBrokers, int maxBrokers, Configuration config) throws EXistException, DatabaseConfigurationException {
        configure(DEFAULT_INSTANCE_NAME, minBrokers, maxBrokers, config);
    }

    public static final void configure(String instanceName, int minBrokers, int maxBrokers, Configuration config) throws EXistException {
        BrokerPool instance = instances.get(instanceName);
        if (instance == null) {
            LOG.debug("configuring database instance '" + instanceName + "'...");
            try {
                instance = new BrokerPool(instanceName, minBrokers, maxBrokers, config);
                instances.put(instanceName, instance);
                if (instances.size() == 1) {
                    if (registerShutdownHook) {
                        try {
                            Runtime.getRuntime().addShutdownHook(shutdownHook);
                            LOG.debug("shutdown hook registered");
                        } catch (IllegalArgumentException e) {
                            LOG.warn("shutdown hook already registered");
                        }
                    }
                }
            } catch (Throwable ex) {
                LOG.error("Unable to initialize database instance '" + instanceName + "': " + ex.getMessage(), ex);
            }
        } else LOG.warn("database instance '" + instanceName + "' is already configured");
    }

    public static final boolean isConfigured() {
        return isConfigured(DEFAULT_INSTANCE_NAME);
    }

    public static final boolean isConfigured(String id) {
        BrokerPool instance = instances.get(id);
        if (instance == null) return false;
        return instance.isInstanceConfigured();
    }

    /**Returns a broker pool for the default database instance.
     * @return The broker pool
     * @throws EXistException If the database instance is not available (not created, stopped or not configured)
     */
    public static final BrokerPool getInstance() throws EXistException {
        return getInstance(DEFAULT_INSTANCE_NAME);
    }

    /**Returns a broker pool for a database instance.
     * @param instanceName The name of the database instance
     * @return The broker pool
     * @throws EXistException If the instance is not available (not created, stopped or not configured)
     */
    public static final BrokerPool getInstance(String instanceName) throws EXistException {
        BrokerPool instance = instances.get(instanceName);
        if (instance != null) return instance;
        throw new EXistException("database instance '" + instanceName + "' is not available");
    }

    /** Returns an iterator over the database instances.
     * @return The iterator
     */
    public static final Iterator<BrokerPool> getInstances() {
        return instances.values().iterator();
    }

    public static final boolean isInstancesEmpty() {
        return instances.values().isEmpty();
    }

    /** Stops the default database instance. After calling this method, it is
     *  no longer configured.
     * @throws EXistException If the default database instance is not available (not created, stopped or not configured) 
     */
    public static final void stop() throws EXistException {
        stop(DEFAULT_INSTANCE_NAME);
    }

    /** Stops the given database instance. After calling this method, it is
     *  no longer configured.
     * @param id The name of the database instance
     * @throws EXistException If the database instance is not available (not created, stopped or not configured)
     */
    public static final void stop(String id) throws EXistException {
        BrokerPool instance = instances.get(id);
        if (instance == null) throw new EXistException("database instance '" + id + "' is not available");
        instance.shutdown();
    }

    /** Stops all the database instances. After calling this method, the database instances are
     *  no longer configured.
     * @param killed <code>true</code> when invoked by an exiting JVM
     */
    public static final void stopAll(boolean killed) {
        Vector<BrokerPool> tmpInstances = new Vector<BrokerPool>();
        for (BrokerPool instance : instances.values()) {
            tmpInstances.add(instance);
        }
        for (BrokerPool instance : tmpInstances) {
            if (instance.conf != null) instance.shutdown(killed);
        }
        instances.clear();
    }

    public static final void systemInfo() {
        for (BrokerPool instance : instances.values()) {
            instance.printSystemInfo();
        }
    }

    public static void registerStatusObserver(Observer observer) {
        statusObserver = observer;
        LOG.debug("registering observer: " + observer.getClass().getName());
    }

    private final int DEFAULT_MIN_BROKERS = 1;

    private final int DEFAULT_MAX_BROKERS = 15;

    public final long DEFAULT_SYNCH_PERIOD = 120000;

    public final long DEFAULT_MAX_SHUTDOWN_WAIT = 45000;

    public final int DEFAULT_COLLECTION_BUFFER_SIZE = 512;

    public static final String PROPERTY_PAGE_SIZE = "db-connection.page-size";

    public static final int DEFAULT_PAGE_SIZE = 4096;

    /**
     * <code>true</code> if the database instance is able to handle transactions. 
     */
    private boolean transactionsEnabled;

    /**
	 * The name of the database instance
	 */
    private String instanceName;

    private static final int SHUTDOWN = -1;

    private static final int INITIALIZING = 0;

    private static final int OPERATING = 1;

    private int status = INITIALIZING;

    /**
	 * The number of brokers for the database instance 
	 */
    private int brokersCount = 0;

    /**
	 * The minimal number of brokers for the database instance 
	 */
    @ConfigurationFieldAsAttribute("min")
    private int minBrokers;

    /**
	 * The maximal number of brokers for the database instance 
	 */
    @ConfigurationFieldAsAttribute("max")
    private int maxBrokers;

    /**
	 * The number of inactive brokers for the database instance 
	 */
    private Stack<DBBroker> inactiveBrokers = new Stack<DBBroker>();

    /**
	 * The number of active brokers for the database instance 
	 */
    private Map<Thread, DBBroker> activeBrokers = new IdentityHashMap<Thread, DBBroker>();

    /**
     * The configuration object for the database instance
     */
    protected Configuration conf = null;

    private boolean syncRequired = false;

    /**
	 * The kind of scheduled cache synchronization event. 
	 * One of {@link org.exist.storage.sync.Sync#MAJOR_SYNC} or {@link org.exist.storage.sync.Sync#MINOR_SYNC}
	 */
    private int syncEvent = 0;

    private boolean checkpoint = false;

    private boolean isReadOnly;

    @ConfigurationFieldAsAttribute("pageSize")
    private int pageSize;

    private FileLock dataLock;

    /**
     * The transaction manager of the database instance.
     */
    private TransactionManager transactionManager = null;

    /**
	 * Delay (in ms) for running jobs to return when the database instance shuts down.
	 */
    @ConfigurationFieldAsAttribute("wait-before-shutdown")
    private long maxShutdownWait;

    /**
	 * The scheduler for the database instance.
	 */
    @ConfigurationFieldAsAttribute("scheduler")
    private Scheduler scheduler;

    /**
     * Manages pluggable index structures. 
     */
    private IndexManager indexManager;

    /**
     * Global symbol table used to encode element and attribute qnames.
     */
    private SymbolTable symbols;

    /**
	 * Cache synchronization on the database instance.
	 */
    @ConfigurationFieldAsAttribute("sync-period")
    private long majorSyncPeriod = DEFAULT_SYNCH_PERIOD;

    private long lastMajorSync = System.currentTimeMillis();

    /**
	 * The listener that is notified when the database instance shuts down.
	 */
    private ShutdownListener shutdownListener = null;

    /**
     * The security manager of the database instance. 
     */
    private SecurityManager securityManager = null;

    /**
     * The plugin manager.
     */
    private PluginsManagerImpl pluginManager = null;

    /**
     * The global notification service used to subscribe
     * to document updates.
     */
    private NotificationService notificationService = null;

    private long nextSystemStatus = System.currentTimeMillis();

    /**
	 * The cache in which the database instance may store items.
	 */
    private DefaultCacheManager cacheManager;

    private CollectionCacheManager collectionCacheMgr;

    private long reservedMem;

    /**
	 * The pool in which the database instance's <strong>compiled</strong> XQueries are stored.
	 */
    private XQueryPool xQueryPool;

    /**
	 * The monitor in which the database instance's strong>running</strong> XQueries are managed.
	 */
    private ProcessMonitor processMonitor;

    /**
     * Global performance stats to gather function execution statistics
     * from all queries running on this database instance.
     */
    private PerformanceStats xqueryStats;

    /**
     * The global manager for accessing collection configuration files from the database instance.
     */
    private CollectionConfigurationManager collectionConfigurationManager = null;

    protected CollectionCache collectionCache;

    /**
	 * The pool in which the database instance's readers are stored.
	 */
    protected XMLReaderPool xmlReaderPool;

    private NodeIdFactory nodeFactory = new DLNFactory();

    private Lock globalXUpdateLock = new ReentrantReadWriteLock("xupdate");

    private Subject serviceModeUser = null;

    private boolean inServiceMode = false;

    private final Calendar startupTime = Calendar.getInstance();

    private BrokerWatchdog watchdog = null;

    private BrokerPool(String instanceName, int minBrokers, int maxBrokers, Configuration conf) throws EXistException, DatabaseConfigurationException {
        Integer anInteger;
        Long aLong;
        Boolean aBoolean;
        NumberFormat nf = NumberFormat.getNumberInstance();
        if (statusObserver != null) addObserver(statusObserver);
        this.instanceName = instanceName;
        this.minBrokers = DEFAULT_MIN_BROKERS;
        this.maxBrokers = DEFAULT_MAX_BROKERS;
        this.maxShutdownWait = DEFAULT_MAX_SHUTDOWN_WAIT;
        this.transactionsEnabled = true;
        this.minBrokers = minBrokers;
        this.maxBrokers = maxBrokers;
        anInteger = (Integer) conf.getProperty(PROPERTY_MIN_CONNECTIONS);
        if (anInteger != null) this.minBrokers = anInteger.intValue();
        anInteger = (Integer) conf.getProperty(PROPERTY_MAX_CONNECTIONS);
        if (anInteger != null) this.maxBrokers = anInteger.intValue();
        LOG.info("database instance '" + instanceName + "' will have between " + nf.format(this.minBrokers) + " and " + nf.format(this.maxBrokers) + " brokers");
        aLong = (Long) conf.getProperty(PROPERTY_SYNC_PERIOD);
        if (aLong != null) majorSyncPeriod = aLong.longValue();
        LOG.info("database instance '" + instanceName + "' will be synchronized every " + nf.format(majorSyncPeriod) + " ms");
        aLong = (Long) conf.getProperty(BrokerPool.PROPERTY_SHUTDOWN_DELAY);
        if (aLong != null) {
            this.maxShutdownWait = aLong.longValue();
        }
        LOG.info("database instance '" + instanceName + "' will wait  " + nf.format(this.maxShutdownWait) + " ms during shutdown");
        aBoolean = (Boolean) conf.getProperty(PROPERTY_RECOVERY_ENABLED);
        if (aBoolean != null) {
            this.transactionsEnabled = aBoolean.booleanValue();
        }
        LOG.info("database instance '" + instanceName + "' is enabled for transactions : " + this.transactionsEnabled);
        pageSize = conf.getInteger(PROPERTY_PAGE_SIZE);
        if (pageSize < 0) pageSize = DEFAULT_PAGE_SIZE;
        scheduler = new Scheduler(this, conf);
        this.isReadOnly = !canReadDataDir(conf);
        LOG.debug("isReadOnly: " + isReadOnly);
        this.conf = conf;
        initialize();
        if (majorSyncPeriod > 0) {
            SyncTask syncTask = new SyncTask();
            scheduler.createPeriodicJob(2500, new SystemTaskJob(SyncTask.getJobName(), syncTask), 2500);
        }
        if (System.getProperty("trace.brokers", "no").equals("yes")) watchdog = new BrokerWatchdog();
    }

    protected boolean canReadDataDir(Configuration conf) throws EXistException {
        String dataDir = (String) conf.getProperty(PROPERTY_DATA_DIR);
        if (dataDir == null) dataDir = "data";
        File dir = new File(dataDir);
        if (!dir.exists()) {
            try {
                LOG.info("Data directory '" + dir.getAbsolutePath() + "' does not exist. Creating one ...");
                dir.mkdirs();
            } catch (SecurityException e) {
                LOG.info("Cannot create data directory '" + dir.getAbsolutePath() + "'. Switching to read-only mode.");
                return false;
            }
        }
        conf.setProperty(PROPERTY_DATA_DIR, dataDir);
        if (!dir.canWrite()) {
            LOG.info("Cannot write to data directory: " + dir.getAbsolutePath() + ". Switching to read-only mode.");
            return false;
        }
        dataLock = new FileLock(this, dir, "dbx_dir.lck");
        try {
            boolean locked = dataLock.tryLock();
            if (!locked) {
                throw new EXistException("The database directory seems to be locked by another " + "database instance. Found a valid lock file: " + dataLock.getFile());
            }
        } catch (ReadOnlyException e) {
            LOG.info(e.getMessage() + ". Switching to read-only mode!!!");
            return false;
        }
        return true;
    }

    /**Initializes the database instance.
	 * @throws EXistException
	 */
    protected void initialize() throws EXistException, DatabaseConfigurationException {
        if (LOG.isDebugEnabled()) LOG.debug("initializing database instance '" + instanceName + "'...");
        status = INITIALIZING;
        signalSystemStatus(SIGNAL_STARTUP);
        boolean exportOnly = (Boolean) conf.getProperty(PROPERTY_EXPORT_ONLY, false);
        securityManager = new SecurityManagerImpl(this);
        cacheManager = new DefaultCacheManager(this);
        xQueryPool = new XQueryPool(conf);
        processMonitor = new ProcessMonitor(maxShutdownWait);
        xqueryStats = new PerformanceStats(this);
        xmlReaderPool = new XMLReaderPool(conf, new XMLReaderObjectFactory(this), 5, 0);
        int bufferSize = conf.getInteger(PROPERTY_COLLECTION_CACHE_SIZE);
        if (bufferSize == -1) bufferSize = DEFAULT_COLLECTION_BUFFER_SIZE;
        collectionCache = new CollectionCache(this, bufferSize, 0.0001);
        collectionCacheMgr = new CollectionCacheManager(this, collectionCache);
        Runtime rt = Runtime.getRuntime();
        long maxMem = rt.maxMemory();
        long minFree = maxMem / 5;
        reservedMem = cacheManager.getTotalMem() + collectionCacheMgr.getMaxTotal() + minFree;
        LOG.debug("Reserved memory: " + reservedMem + "; max: " + maxMem + "; min: " + minFree);
        notificationService = new NotificationService();
        transactionManager = new TransactionManager(this, new File((String) conf.getProperty(BrokerPool.PROPERTY_DATA_DIR)), isTransactional());
        try {
            transactionManager.initialize();
        } catch (ReadOnlyException e) {
            LOG.warn(e.getMessage() + ". Switching to read-only mode!!!");
            isReadOnly = true;
        }
        symbols = new SymbolTable(this, conf);
        isReadOnly = isReadOnly || !symbols.getFile().canWrite();
        indexManager = new IndexManager(this, conf);
        DBBroker broker = get(securityManager.getSystemSubject());
        try {
            if (isReadOnly()) {
                transactionManager.setEnabled(false);
            }
            boolean recovered = false;
            if (isTransactional()) {
                recovered = transactionManager.runRecovery(broker);
                if (!recovered) {
                    try {
                        if (broker.getCollection(XmldbURI.ROOT_COLLECTION_URI) == null) {
                            Txn txn = transactionManager.beginTransaction();
                            try {
                                broker.getOrCreateCollection(txn, XmldbURI.ROOT_COLLECTION_URI);
                                transactionManager.commit(txn);
                            } catch (IOException e) {
                                transactionManager.abort(txn);
                            } catch (PermissionDeniedException e) {
                                transactionManager.abort(txn);
                            } catch (TriggerException e) {
                                transactionManager.abort(txn);
                            }
                        }
                    } catch (PermissionDeniedException pde) {
                        LOG.fatal(pde.getMessage(), pde);
                    }
                }
            }
            if (!exportOnly) {
                try {
                    initialiseSystemCollections(broker);
                } catch (PermissionDeniedException pde) {
                    LOG.error(pde.getMessage(), pde);
                    throw new EXistException(pde.getMessage(), pde);
                }
            }
            status = OPERATING;
            signalSystemStatus(SIGNAL_READINESS);
            try {
                collectionConfigurationManager = new CollectionConfigurationManager(broker);
            } catch (Exception e) {
                LOG.error("Found an error while initializing database: " + e.getMessage(), e);
            }
            try {
                initialiseTriggersForSystemCollections(broker);
            } catch (PermissionDeniedException pde) {
                LOG.error(pde.getMessage(), pde);
                throw new EXistException(pde.getMessage(), pde);
            }
            securityManager.attach(this, broker);
            if (securityManager.isXACMLEnabled()) securityManager.getPDP().initializePolicyCollection();
            if (recovered) {
                try {
                    broker.repair();
                } catch (PermissionDeniedException e) {
                    LOG.warn("Error during recovery: " + e.getMessage(), e);
                }
                if (((Boolean) conf.getProperty(PROPERTY_RECOVERY_CHECK)).booleanValue()) {
                    ConsistencyCheckTask task = new ConsistencyCheckTask();
                    Properties props = new Properties();
                    props.setProperty("backup", "no");
                    props.setProperty("output", "sanity");
                    task.configure(conf, props);
                    task.execute(broker);
                }
            }
            signalSystemStatus(SIGNAL_RIDEABLE);
            try {
                Collection systemCollection = broker.getCollection(XmldbURI.SYSTEM_COLLECTION_URI);
                if (systemCollection != null) {
                    CollectionConfigurationManager manager = broker.getBrokerPool().getConfigurationManager();
                    CollectionConfiguration collConf = manager.getOrCreateCollectionConfiguration(broker, systemCollection);
                    Class c = ConfigurationDocumentTrigger.class;
                    DocumentTriggerProxy triggerProxy = new DocumentTriggerProxy((Class<DocumentTrigger>) c, systemCollection.getURI());
                    collConf.getDocumentTriggerProxies().add(triggerProxy);
                }
            } catch (PermissionDeniedException pde) {
                LOG.error(pde.getMessage(), pde);
            }
            try {
                broker.cleanUpTempResources(true);
            } catch (PermissionDeniedException pde) {
                LOG.error(pde.getMessage(), pde);
            }
            sync(broker, Sync.MAJOR_SYNC);
            instances.put(instanceName, this);
            pluginManager = new PluginsManagerImpl(this, broker);
        } finally {
            release(broker);
        }
        for (int i = 1; i < minBrokers; i++) createBroker();
        AgentFactory.getInstance().initDBInstance(this);
        if (LOG.isDebugEnabled()) LOG.debug("database instance '" + instanceName + "' initialized");
        scheduler.run();
    }

    /**
     * Initialise system collections, if it doesn't exist yet
     *
     * @param sysBroker The system broker from before the brokerpool is populated
     * @param sysCollectionUri XmldbURI of the collection to create
     * @param permissions The permissions to set on the created collection
     */
    private void initialiseSystemCollection(DBBroker sysBroker, XmldbURI sysCollectionUri, int permissions) throws EXistException, PermissionDeniedException {
        Collection collection = sysBroker.getCollection(sysCollectionUri);
        if (collection == null) {
            TransactionManager transact = getTransactionManager();
            Txn txn = transact.beginTransaction();
            try {
                collection = sysBroker.getOrCreateCollection(txn, sysCollectionUri);
                if (collection == null) throw new IOException("Could not create system collection: " + sysCollectionUri);
                collection.setPermissions(permissions);
                sysBroker.saveCollection(txn, collection);
                transact.commit(txn);
            } catch (Exception e) {
                transact.abort(txn);
                e.printStackTrace();
                String msg = "Initialisation of system collections failed: " + e.getMessage();
                LOG.error(msg, e);
                throw new EXistException(msg, e);
            }
        }
    }

    /**
     * Initialize required system collections, if they don't exist yet
     *
     * @param sysBroker - The system broker from before the brokerpool is populated
     *
     * @throws EXistException If a system collection cannot be created
     */
    private void initialiseSystemCollections(DBBroker broker) throws EXistException, PermissionDeniedException {
        initialiseSystemCollection(broker, XmldbURI.SYSTEM_COLLECTION_URI, Permission.DEFAULT_SYSTEM_COLLECTION_PERM);
        initialiseSystemCollection(broker, XmldbURI.ETC_COLLECTION_URI, Permission.DEFAULT_SYSTEM_ETC_COLLECTION_PERM);
    }

    private void initialiseTriggersForSystemCollections(DBBroker broker) throws EXistException, PermissionDeniedException {
        Collection collection = broker.getCollection(XmldbURI.ETC_COLLECTION_URI);
        if (collection != null) {
            CollectionConfigurationManager manager = getConfigurationManager();
            CollectionConfiguration collConf = manager.getOrCreateCollectionConfiguration(broker, collection);
            Class c = ConfigurationDocumentTrigger.class;
            DocumentTriggerProxy triggerProxy = new DocumentTriggerProxy((Class<DocumentTrigger>) c, collection.getURI());
            collConf.getDocumentTriggerProxies().add(triggerProxy);
        }
    }

    public long getReservedMem() {
        return reservedMem - cacheManager.getSizeInBytes();
    }

    public int getPageSize() {
        return pageSize;
    }

    private String lastSignal = "";

    public void signalSystemStatus(String signal) {
        if (!lastSignal.equals(signal) || System.currentTimeMillis() > nextSystemStatus) {
            lastSignal = signal;
            setChanged();
            notifyObservers(signal);
            nextSystemStatus = System.currentTimeMillis() + 10000;
        }
    }

    public boolean isInitializing() {
        return status == INITIALIZING;
    }

    public String getId() {
        return instanceName;
    }

    public int active() {
        return activeBrokers.size();
    }

    public Map<Thread, DBBroker> getActiveBrokers() {
        return new HashMap<Thread, DBBroker>(activeBrokers);
    }

    public int available() {
        return inactiveBrokers.size();
    }

    public int getMax() {
        return maxBrokers;
    }

    public int total() {
        return brokersCount;
    }

    /**
	 * Returns whether the database instance has been configured.
	 *
	 *@return <code>true</code> if the datbase instance is configured
	 */
    public final boolean isInstanceConfigured() {
        return conf != null;
    }

    /**
	 * Returns the configuration object for the database instance.
	 *@return The configuration
	 */
    public Configuration getConfiguration() {
        return conf;
    }

    public void registerShutdownListener(ShutdownListener listener) {
        shutdownListener = listener;
    }

    public NodeIdFactory getNodeFactory() {
        return nodeFactory;
    }

    /**
     *  Returns the database instance's security manager
     *
     *@return    The security manager
     */
    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    /** Returns the Scheduler
     * @return The scheduler
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    public SymbolTable getSymbols() {
        return symbols;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    /**
     * Returns whether transactions can be handled by the database instance.
     * 
     * @return <code>true</code> if transactions can be handled
     */
    public boolean isTransactional() {
        return !isReadOnly && transactionsEnabled;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public void setReadOnly() {
        isReadOnly = true;
    }

    public boolean isInServiceMode() {
        return inServiceMode;
    }

    public TransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    /** 
     * Returns a manager for accessing the database instance's collection configuration files.
     * @return The manager
     */
    public CollectionConfigurationManager getConfigurationManager() {
        return collectionConfigurationManager;
    }

    /**
     * Returns a cache in which the database instance's collections are stored.
     * 
     * @return The cache
     */
    public CollectionCache getCollectionsCache() {
        return collectionCache;
    }

    /**
     * Returns a cache in which the database instance's may store items.
     * 
     * @return The cache
     */
    @Override
    public DefaultCacheManager getCacheManager() {
        return cacheManager;
    }

    public CollectionCacheManager getCollectionCacheMgr() {
        return collectionCacheMgr;
    }

    /**
     * Returns the index manager which handles all additional indexes not
     * being part of the database core.
     * 
     * @return The IndexManager
     */
    public IndexManager getIndexManager() {
        return indexManager;
    }

    /**
     * Returns a pool in which the database instance's <strong>compiled</strong> XQueries are stored.
     * 
     * @return The pool
     */
    public XQueryPool getXQueryPool() {
        return xQueryPool;
    }

    /**
     * Returns a monitor in which the database instance's <strong>running</strong> XQueries are managed.
     * 
     * @return The monitor
     */
    public ProcessMonitor getProcessMonitor() {
        return processMonitor;
    }

    /**
     * Returns the global profiler used to gather execution statistics
     * from all XQueries running on this db instance.
     *
     * @return the profiler
     */
    public PerformanceStats getPerformanceStats() {
        return xqueryStats;
    }

    /**
     * Returns a pool in which the database instance's readers are stored.
     * 
     * @return The pool
	 */
    public XMLReaderPool getParserPool() {
        return xmlReaderPool;
    }

    public Lock getGlobalUpdateLock() {
        return globalXUpdateLock;
    }

    /** Creates an inactive broker for the database instance.
     * @return The broker
     * @throws EXistException
     */
    protected DBBroker createBroker() throws EXistException {
        DBBroker broker = BrokerFactory.getInstance(this, this.getConfiguration());
        inactiveBrokers.push(broker);
        brokersCount++;
        broker.setId(broker.getClass().getName() + '_' + instanceName + "_" + brokersCount);
        LOG.debug("created broker '" + broker.getId() + " for database instance '" + instanceName + "'");
        return broker;
    }

    public boolean setSubject(Subject subject) {
        DBBroker broker = activeBrokers.get(Thread.currentThread());
        if (broker != null) {
            broker.setUser(subject);
            return true;
        }
        return false;
    }

    public Subject getSubject() {
        DBBroker broker = activeBrokers.get(Thread.currentThread());
        if (broker != null) {
            return broker.getSubject();
        }
        return securityManager.getGuestSubject();
    }

    public DBBroker getActiveBroker() {
        DBBroker broker = activeBrokers.get(Thread.currentThread());
        return broker;
    }

    public DBBroker getBroker() throws EXistException {
        return get(null);
    }

    public DBBroker get(Subject user) throws EXistException {
        if (!isInstanceConfigured()) {
            throw new EXistException("database instance '" + instanceName + "' is not available");
        }
        DBBroker broker = activeBrokers.get(Thread.currentThread());
        if (broker != null) {
            broker.incReferenceCount();
            if (user != null) broker.setSubject(user);
            return broker;
        }
        while (serviceModeUser != null && user != null && !user.equals(serviceModeUser)) {
            try {
                LOG.debug("Db instance is in service mode. Waiting for db to become available again ...");
                wait();
            } catch (InterruptedException e) {
            }
        }
        synchronized (this) {
            if (inactiveBrokers.isEmpty()) {
                if (brokersCount < maxBrokers) createBroker(); else while (inactiveBrokers.isEmpty()) {
                    LOG.debug("waiting for a broker to become available");
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            broker = inactiveBrokers.pop();
            activeBrokers.put(Thread.currentThread(), broker);
            if (watchdog != null) watchdog.add(broker);
            broker.incReferenceCount();
            if (user != null) broker.setSubject(user); else broker.setSubject(securityManager.getGuestSubject());
            this.notifyAll();
            return broker;
        }
    }

    public void release(DBBroker broker) {
        if (broker == null) return;
        broker.decReferenceCount();
        if (broker.getReferenceCount() > 0) {
            return;
        }
        synchronized (this) {
            for (int i = 0; i < inactiveBrokers.size(); i++) {
                if (broker == inactiveBrokers.get(i)) {
                    LOG.error("Broker is already in the inactive list!!!");
                    return;
                }
            }
            if (activeBrokers.remove(Thread.currentThread()) == null) {
                LOG.error("release() has been called from the wrong thread for broker " + broker.getId());
                for (Object t : activeBrokers.keySet()) {
                    if (activeBrokers.get(t) == broker) {
                        activeBrokers.remove(t);
                        break;
                    }
                }
            }
            Subject lastUser = broker.getSubject();
            broker.setSubject(securityManager.getGuestSubject());
            inactiveBrokers.push(broker);
            if (watchdog != null) watchdog.remove(broker);
            if (activeBrokers.size() == 0) {
                if (syncRequired) {
                    sync(broker, syncEvent);
                    this.syncRequired = false;
                    this.checkpoint = false;
                }
                if (serviceModeUser != null && !lastUser.equals(serviceModeUser)) {
                    inServiceMode = true;
                }
            }
            this.notifyAll();
        }
    }

    public DBBroker enterServiceMode(Subject user) throws PermissionDeniedException {
        if (!user.hasDbaRole()) throw new PermissionDeniedException("Only users of group dba can switch the db to service mode");
        serviceModeUser = user;
        synchronized (this) {
            if (activeBrokers.size() != 0) {
                while (!inServiceMode) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        inServiceMode = true;
        DBBroker broker = inactiveBrokers.peek();
        checkpoint = true;
        sync(broker, Sync.MAJOR_SYNC);
        checkpoint = false;
        return broker;
    }

    public void exitServiceMode(Subject user) throws PermissionDeniedException {
        if (!user.equals(serviceModeUser)) throw new PermissionDeniedException("The db has been locked by a different user");
        serviceModeUser = null;
        inServiceMode = false;
        synchronized (this) {
            this.notifyAll();
        }
    }

    /**
	 * Reloads the security manager of the database instance. This method is 
         * called for example when the <code>users.xml</code> file has been changed.
	 * 
	 * @param broker A broker responsible for executing the job
         *
         *  TOUNDERSTAND (pb) : why do we need a broker here ? Why not get and 
         *  release one when we're done?
         *  WM: this is called from the Collection.store() methods to signal 
         *  that /db/system/users.xml has changed.
         *  A broker is already available in these methods, so we use it here.
         */
    public void reloadSecurityManager(DBBroker broker) {
    }

    public long getMajorSyncPeriod() {
        return majorSyncPeriod;
    }

    public long getLastMajorSync() {
        return lastMajorSync;
    }

    public void sync(DBBroker broker, int syncEvent) {
        broker.sync(syncEvent);
        Subject user = broker.getSubject();
        broker.setSubject(securityManager.getSystemSubject());
        if (status != SHUTDOWN) {
            try {
                broker.cleanUpTempResources();
            } catch (PermissionDeniedException pde) {
                LOG.warn("Unable to clean-up temp collection: " + pde.getMessage(), pde);
            }
        }
        if (syncEvent == Sync.MAJOR_SYNC) {
            LOG.debug("Major sync");
            try {
                if (!FORCE_CORRUPTION) transactionManager.checkpoint(checkpoint);
            } catch (TransactionException e) {
                LOG.warn(e.getMessage(), e);
            }
            cacheManager.checkCaches();
            lastMajorSync = System.currentTimeMillis();
            if (LOG.isDebugEnabled()) notificationService.debug();
        } else {
            cacheManager.checkDistribution();
        }
        broker.setSubject(user);
    }

    /**
	 * Schedules a cache synchronization for the database instance. If the database instance is idle,
	 * the cache synchronization will be run immediately. Otherwise, the task will be deffered 
	 * until all running threads have returned.
	 * @param syncEvent One of {@link org.exist.storage.sync.Sync#MINOR_SYNC} or 
         * {@link org.exist.storage.sync.Sync#MINOR_SYNC}   
	 */
    public void triggerSync(int syncEvent) {
        if (status == SHUTDOWN) return;
        LOG.debug("Triggering sync: " + syncEvent);
        synchronized (this) {
            if (inactiveBrokers.size() == brokersCount) {
                DBBroker broker = inactiveBrokers.pop();
                sync(broker, syncEvent);
                inactiveBrokers.push(broker);
                syncRequired = false;
            } else {
                this.syncEvent = syncEvent;
                syncRequired = true;
            }
        }
    }

    public void triggerSystemTask(SystemTask task) {
        transactionManager.triggerSystemTask(task);
    }

    /**
	 * Shuts downs the database instance
	 */
    public void shutdown() {
        shutdown(false);
    }

    public boolean isShuttingDown() {
        return (status == SHUTDOWN);
    }

    /**
	 * Shuts downs the database instance
	 * @param killed <code>true</code> when the JVM is (cleanly) exiting
	 */
    public void shutdown(boolean killed) {
        if (status == SHUTDOWN) return;
        LOG.info("Database is shutting down ...");
        status = SHUTDOWN;
        processMonitor.stopRunningJobs();
        java.util.concurrent.locks.Lock lock = transactionManager.getLock();
        try {
            lock.lock();
            synchronized (this) {
                lock.unlock();
                notificationService.debug();
                scheduler.shutdown(false);
                while (!scheduler.isShutdown()) {
                    try {
                        wait(250);
                    } catch (InterruptedException e) {
                    }
                    signalSystemStatus(SIGNAL_SHUTDOWN);
                }
                processMonitor.killAll(500);
                if (isTransactional()) transactionManager.getJournal().flushToLog(true, true);
                boolean hangingThreads = false;
                long waitStart = System.currentTimeMillis();
                if (activeBrokers.size() > 0) {
                    printSystemInfo();
                    LOG.info("Waiting " + maxShutdownWait + "ms for remaining threads to shut down...");
                    while (activeBrokers.size() > 0) {
                        try {
                            this.wait(1000);
                        } catch (InterruptedException e) {
                        }
                        signalSystemStatus(SIGNAL_SHUTDOWN);
                        if (maxShutdownWait > -1 && System.currentTimeMillis() - waitStart > maxShutdownWait) {
                            LOG.warn("Not all threads returned. Forcing shutdown ...");
                            hangingThreads = true;
                            break;
                        }
                    }
                }
                LOG.debug("Calling shutdown ...");
                try {
                    indexManager.shutdown();
                } catch (DBException e) {
                    LOG.warn("Error during index shutdown: " + e.getMessage(), e);
                }
                DBBroker broker = null;
                if (inactiveBrokers.isEmpty()) try {
                    broker = createBroker();
                } catch (EXistException e) {
                    LOG.warn("could not create instance for shutdown. Giving up.");
                } else broker = inactiveBrokers.peek();
                if (broker != null) {
                    broker.setUser(securityManager.getSystemSubject());
                    broker.shutdown();
                }
                collectionCacheMgr.deregisterCache(collectionCache);
                signalSystemStatus(SIGNAL_SHUTDOWN);
                if (hangingThreads) transactionManager.shutdown(false); else transactionManager.shutdown(true);
                AgentFactory.getInstance().closeDBInstance(this);
                conf = null;
                instances.remove(instanceName);
                if (!isReadOnly) dataLock.release();
                LOG.info("shutdown complete !");
                if (instances.size() == 0 && !killed) {
                    LOG.debug("removing shutdown hook");
                    try {
                        Runtime.getRuntime().removeShutdownHook(shutdownHook);
                    } catch (IllegalStateException e) {
                    }
                }
                if (shutdownListener != null) shutdownListener.shutdown(instanceName, instances.size());
            }
        } finally {
            Configurator.clear(this);
            transactionManager = null;
            collectionCache = null;
            collectionCacheMgr = null;
            xQueryPool = null;
            processMonitor = null;
            collectionConfigurationManager = null;
            notificationService = null;
            indexManager = null;
            scheduler = null;
            xmlReaderPool = null;
            shutdownListener = null;
            securityManager = null;
            notificationService = null;
        }
    }

    public BrokerWatchdog getWatchdog() {
        return watchdog;
    }

    public void triggerCheckpoint() {
        if (syncRequired) return;
        synchronized (this) {
            syncEvent = Sync.MAJOR_SYNC;
            syncRequired = true;
            checkpoint = true;
        }
    }

    private Debuggee debuggee = null;

    public Debuggee getDebuggee() {
        synchronized (this) {
            if (debuggee == null) debuggee = DebuggeeFactory.getInstance();
        }
        return debuggee;
    }

    public Calendar getStartupTime() {
        return startupTime;
    }

    public void printSystemInfo() {
        StringWriter sout = new StringWriter();
        PrintWriter writer = new PrintWriter(sout);
        writer.println("SYSTEM INFO");
        writer.format("Database instance: %s\n", getId());
        writer.println("-------------------------------------------------------------------");
        if (watchdog != null) watchdog.dump(writer);
        DeadlockDetection.debug(writer);
        String s = sout.toString();
        LOG.info(s);
        System.err.println(s);
    }
}
