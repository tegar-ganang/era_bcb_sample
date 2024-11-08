package com.sleepycat.je.dbi;

import static com.sleepycat.je.dbi.DbiStatDefinition.ENVIMPL_RELATCHES_REQUIRED;
import static com.sleepycat.je.dbi.DbiStatDefinition.ENV_GROUP_DESC;
import static com.sleepycat.je.dbi.DbiStatDefinition.ENV_GROUP_NAME;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.MemoryHandler;
import com.sleepycat.je.CacheMode;
import com.sleepycat.je.CacheModeStrategy;
import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentFailureException;
import com.sleepycat.je.EnvironmentLockedException;
import com.sleepycat.je.EnvironmentMutableConfig;
import com.sleepycat.je.EnvironmentNotFoundException;
import com.sleepycat.je.EnvironmentStats;
import com.sleepycat.je.ExceptionListener;
import com.sleepycat.je.LockStats;
import com.sleepycat.je.LogScanConfig;
import com.sleepycat.je.LogScanner;
import com.sleepycat.je.OperationFailureException;
import com.sleepycat.je.StatsConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.TransactionConfig;
import com.sleepycat.je.TransactionStats;
import com.sleepycat.je.VerifyConfig;
import com.sleepycat.je.TransactionStats.Active;
import com.sleepycat.je.cleaner.Cleaner;
import com.sleepycat.je.cleaner.LocalUtilizationTracker;
import com.sleepycat.je.cleaner.UtilizationProfile;
import com.sleepycat.je.cleaner.UtilizationTracker;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.evictor.Evictor;
import com.sleepycat.je.evictor.PrivateEvictor;
import com.sleepycat.je.evictor.SharedEvictor;
import com.sleepycat.je.evictor.Evictor.EvictionSource;
import com.sleepycat.je.incomp.INCompressor;
import com.sleepycat.je.latch.Latch;
import com.sleepycat.je.latch.LatchSupport;
import com.sleepycat.je.latch.SharedLatch;
import com.sleepycat.je.log.FileManager;
import com.sleepycat.je.log.LNFileReader;
import com.sleepycat.je.log.LatchedLogManager;
import com.sleepycat.je.log.LogEntryHeader;
import com.sleepycat.je.log.LogEntryType;
import com.sleepycat.je.log.LogItem;
import com.sleepycat.je.log.LogManager;
import com.sleepycat.je.log.ReplicationContext;
import com.sleepycat.je.log.SyncedLogManager;
import com.sleepycat.je.log.Trace;
import com.sleepycat.je.log.entry.LogEntry;
import com.sleepycat.je.log.entry.SingleItemEntry;
import com.sleepycat.je.recovery.Checkpointer;
import com.sleepycat.je.recovery.RecoveryInfo;
import com.sleepycat.je.recovery.RecoveryManager;
import com.sleepycat.je.recovery.VLSNRecoveryProxy;
import com.sleepycat.je.tree.BIN;
import com.sleepycat.je.tree.BINReference;
import com.sleepycat.je.tree.IN;
import com.sleepycat.je.tree.Key;
import com.sleepycat.je.tree.LN;
import com.sleepycat.je.txn.LockType;
import com.sleepycat.je.txn.LockUpgrade;
import com.sleepycat.je.txn.Locker;
import com.sleepycat.je.txn.ThreadLocker;
import com.sleepycat.je.txn.Txn;
import com.sleepycat.je.txn.TxnManager;
import com.sleepycat.je.util.DbBackup;
import com.sleepycat.je.utilint.DbLsn;
import com.sleepycat.je.utilint.ExceptionListenerUser;
import com.sleepycat.je.utilint.LoggerUtils;
import com.sleepycat.je.utilint.LongStat;
import com.sleepycat.je.utilint.StatGroup;
import com.sleepycat.je.utilint.TestHook;
import com.sleepycat.je.utilint.TestHookExecute;
import com.sleepycat.je.utilint.TracerFormatter;
import com.sleepycat.je.utilint.VLSN;

/**
 * Underlying Environment implementation. There is a single instance for any
 * database environment opened by the application.
 */
public class EnvironmentImpl implements EnvConfigObserver {

    private static final boolean TEST_NO_LOCKING_MODE = false;

    private volatile DbEnvState envState;

    private volatile boolean closing;

    private final File envHome;

    private int referenceCount;

    private boolean isTransactional;

    private boolean isNoLocking;

    private boolean isReadOnly;

    private boolean isMemOnly;

    private boolean sharedCache;

    private static boolean fairLatches;

    private static boolean useSharedLatchesForINs;

    private boolean dbEviction;

    private CacheMode cacheMode;

    private CacheModeStrategy cacheModeStrategy;

    private boolean initializedSuccessfully = false;

    protected boolean needConvert = false;

    private MemoryBudget memoryBudget;

    private static int adler32ChunkSize;

    private long lockTimeout;

    private long txnTimeout;

    protected DbTree dbMapTree;

    private long mapTreeRootLsn = DbLsn.NULL_LSN;

    private Latch mapTreeRootLatch;

    private INList inMemoryINs;

    protected DbConfigManager configManager;

    private List<EnvConfigObserver> configObservers;

    protected Logger envLogger;

    private LogManager logManager;

    private FileManager fileManager;

    private TxnManager txnManager;

    private Evictor evictor;

    private INCompressor inCompressor;

    private Checkpointer checkpointer;

    private Cleaner cleaner;

    private RecoveryInfo lastRecoveryInfo;

    private EnvironmentFailureException savedInvalidatingException;

    private TestHook<Long> cleanerBarrierHoook;

    private static boolean forcedYield = false;

    private SharedLatch triggerLatch;

    /**
     * The exception listener for this envimpl, if any has been specified.
     */
    private ExceptionListener exceptionListener = null;

    private final Set<ExceptionListenerUser> exceptionListenerUsers;

    private volatile int backgroundSleepBacklog;

    private volatile int backgroundReadLimit;

    private volatile int backgroundWriteLimit;

    private long backgroundSleepInterval;

    private int backgroundReadCount;

    private long backgroundWriteBytes;

    private TestHook<?> backgroundSleepHook;

    private final Object backgroundTrackingMutex = new Object();

    private final Object backgroundSleepMutex = new Object();

    private static int threadLocalReferenceCount = 0;

    /**
     * DbPrintLog doesn't need btree and dup comparators to function properly
     * don't require any instantiations.  This flag, if true, indicates that
     * we've been called from DbPrintLog.
     */
    private static boolean noComparators = false;

    public final EnvironmentFailureException SAVED_EFE = EnvironmentFailureException.makeJavaErrorWrapper();

    public static final boolean USE_JAVA5_ADLER32;

    private static final String DISABLE_JAVA_ADLER32_NAME = "je.disable.java.adler32";

    static {
        USE_JAVA5_ADLER32 = System.getProperty(DISABLE_JAVA_ADLER32_NAME) == null;
    }

    private static final String REGISTER_MONITOR = "JEMonitor";

    private volatile boolean isMBeanRegistered = false;

    private static final String INFO_FILES = "je.info";

    private static final int FILEHANDLER_LIMIT = 10000000;

    private static final int FILEHANDLER_COUNT = 10;

    private final ConsoleHandler consoleHandler;

    private ConsoleHandler memoryTarget;

    private final MemoryHandler memoryHandler;

    private final FileHandler fileHandler;

    private final boolean dbLoggingDisabled;

    protected final Formatter formatter;

    /**
     * Because the Android platform does not have any javax.management classes,
     * we load JEMonitor dynamically to ensure that there are no explicit
     * references to com.sleepycat.je.jmx.*.
     */
    public static interface MBeanRegistrar {

        public void doRegister(Environment env) throws Exception;

        public void doUnregister() throws Exception;
    }

    private final ArrayList<MBeanRegistrar> mBeanRegList = new ArrayList<MBeanRegistrar>();

    public static final boolean IS_DALVIK;

    static {
        IS_DALVIK = "Dalvik".equals(System.getProperty("java.vm.name"));
    }

    private final NodeSequence nodeSequence;

    private StatGroup stats;

    private LongStat relatchesRequired;

    static {
        LockUpgrade.ILLEGAL.setUpgrade(null);
        LockUpgrade.EXISTING.setUpgrade(null);
        LockUpgrade.WRITE_PROMOTE.setUpgrade(LockType.WRITE);
        LockUpgrade.RANGE_READ_IMMED.setUpgrade(LockType.RANGE_READ);
        LockUpgrade.RANGE_WRITE_IMMED.setUpgrade(LockType.RANGE_WRITE);
        LockUpgrade.RANGE_WRITE_PROMOTE.setUpgrade(LockType.RANGE_WRITE);
    }

    private final String nodeName;

    public EnvironmentImpl(File envHome, EnvironmentConfig envConfig, EnvironmentImpl sharedCacheEnv) throws EnvironmentNotFoundException, EnvironmentLockedException {
        this(envHome, envConfig, sharedCacheEnv, null);
    }

    /**
     * Create a database environment to represent the data in envHome.
     * dbHome. Properties from the je.properties file in that directory are
     * used to initialize the system wide property bag. Properties passed to
     * this method are used to influence the open itself.
     *
     * @param envHome absolute path of the database environment home directory
     * @param envConfig is the configuration to be used. It's already had
     *                  the je.properties file applied, and has been validated.
     * @param sharedCacheEnv if non-null, is another environment that is
     * sharing the cache with this environment; if null, this environment is
     * not sharing the cache or is the first environment to share the cache.
     *
     * @throws DatabaseException on all other failures
     *
     * @throws IllegalArgumentException via Environment ctor.
     */
    protected EnvironmentImpl(File envHome, EnvironmentConfig envConfig, EnvironmentImpl sharedCacheEnv, RepConfigProxy repConfigProxy) throws EnvironmentNotFoundException, EnvironmentLockedException {
        boolean success = false;
        try {
            this.envHome = envHome;
            envState = DbEnvState.INIT;
            mapTreeRootLatch = new Latch("MapTreeRoot");
            exceptionListenerUsers = new HashSet<ExceptionListenerUser>();
            stats = new StatGroup(ENV_GROUP_NAME, ENV_GROUP_DESC);
            relatchesRequired = new LongStat(stats, ENVIMPL_RELATCHES_REQUIRED);
            configManager = initConfigManager(envConfig, repConfigProxy);
            configObservers = new ArrayList<EnvConfigObserver>();
            addConfigObserver(this);
            forcedYield = configManager.getBoolean(EnvironmentParams.ENV_FORCED_YIELD);
            isTransactional = configManager.getBoolean(EnvironmentParams.ENV_INIT_TXN);
            isNoLocking = !(configManager.getBoolean(EnvironmentParams.ENV_INIT_LOCKING));
            if (isTransactional && isNoLocking) {
                if (TEST_NO_LOCKING_MODE) {
                    isNoLocking = !isTransactional;
                } else {
                    throw new IllegalArgumentException("Can't set 'je.env.isNoLocking' and " + "'je.env.isTransactional';");
                }
            }
            fairLatches = configManager.getBoolean(EnvironmentParams.ENV_FAIR_LATCHES);
            isReadOnly = configManager.getBoolean(EnvironmentParams.ENV_RDONLY);
            isMemOnly = configManager.getBoolean(EnvironmentParams.LOG_MEMORY_ONLY);
            useSharedLatchesForINs = configManager.getBoolean(EnvironmentParams.ENV_SHARED_LATCHES);
            dbEviction = configManager.getBoolean(EnvironmentParams.ENV_DB_EVICTION);
            adler32ChunkSize = configManager.getInt(EnvironmentParams.ADLER32_CHUNK_SIZE);
            sharedCache = configManager.getBoolean(EnvironmentParams.ENV_SHARED_CACHE);
            dbLoggingDisabled = !configManager.getBoolean(EnvironmentParams.JE_LOGGING_DBLOG);
            formatter = initFormatter();
            consoleHandler = new com.sleepycat.je.util.ConsoleHandler(formatter, this);
            memoryHandler = initMemoryHandler();
            fileHandler = initFileHandler();
            envLogger = LoggerUtils.getLogger(getClass());
            memoryBudget = new MemoryBudget(this, sharedCacheEnv, configManager);
            fileManager = new FileManager(this, envHome, isReadOnly);
            if (!envConfig.getAllowCreate() && !fileManager.filesExist()) {
                throw new EnvironmentNotFoundException(this, "Home directory: " + envHome);
            }
            nodeName = envConfig.getNodeName();
            if (fairLatches) {
                logManager = new LatchedLogManager(this, isReadOnly);
            } else {
                logManager = new SyncedLogManager(this, isReadOnly);
            }
            inMemoryINs = new INList(this);
            txnManager = new TxnManager(this);
            createDaemons(sharedCacheEnv);
            nodeSequence = new NodeSequence(this);
            nodeSequence.initTransientNodeId();
            dbMapTree = new DbTree(this, isReplicated());
            referenceCount = 0;
            triggerLatch = new SharedLatch("TriggerLatch");
            nodeSequence.initRealNodeId();
            success = true;
        } finally {
            if (!success) {
                clearFileManager();
                closeHandlers();
            }
        }
    }

    /**
     * Create a config manager that holds the configuration properties that
     * have been passed in. These properties are already validated, and have
     * had the proper order of precedence applied; that is, the je.properties
     * file has been applied. The configuration properties need to be available
     * before the rest of environment creation proceeds.
     *
     * This method is overridden by replication environments.
     *
     * @param envConfig is the environment configuration to use
     * @param replicationParams are the replication configurations to use. In
     * this case, the Properties bag has been extracted from the configuration
     * instance, to avoid crossing the compilation firewall.
     */
    protected DbConfigManager initConfigManager(EnvironmentConfig envConfig, RepConfigProxy unused) {
        return new DbConfigManager(envConfig);
    }

    public synchronized void finishInit(EnvironmentConfig envConfig) throws DatabaseException {
        try {
            if (!initializedSuccessfully) {
                if (configManager.getBoolean(EnvironmentParams.ENV_RECOVERY)) {
                    try {
                        RecoveryManager recoveryManager = new RecoveryManager(this);
                        lastRecoveryInfo = recoveryManager.recover(isReadOnly);
                        postRecoveryConversion();
                    } finally {
                        try {
                            logManager.flush();
                            fileManager.clear();
                        } catch (IOException e) {
                            throw new EnvironmentFailureException(this, EnvironmentFailureReason.LOG_INTEGRITY, e);
                        }
                    }
                } else {
                    isReadOnly = true;
                    noComparators = true;
                }
                lockTimeout = configManager.getDuration(EnvironmentParams.LOCK_TIMEOUT);
                txnTimeout = configManager.getDuration(EnvironmentParams.TXN_TIMEOUT);
                memoryBudget.initCacheMemoryUsage(dbMapTree.getTreeAdminMemory());
                open();
                envConfigUpdate(configManager, envConfig);
                initializedSuccessfully = true;
            }
        } catch (RuntimeException e) {
            clearFileManager();
            throw e;
        } finally {
            if (!initializedSuccessfully && sharedCache && evictor != null) {
                evictor.removeEnvironment(this);
            }
        }
    }

    public synchronized void registerMBean(Environment env) throws DatabaseException {
        if (!isMBeanRegistered) {
            if (System.getProperty(REGISTER_MONITOR) != null) {
                doRegisterMBean(getMonitorClassName(), env);
                doRegisterMBean(getDiagnosticsClassName(), env);
            }
            isMBeanRegistered = true;
        }
    }

    protected String getMonitorClassName() {
        return "com.sleepycat.je.jmx.JEMonitor";
    }

    protected String getDiagnosticsClassName() {
        return "com.sleepycat.je.jmx.JEDiagnostics";
    }

    private void doRegisterMBean(String className, Environment env) throws DatabaseException {
        try {
            Class<?> newClass = Class.forName(className);
            MBeanRegistrar mBeanReg = (MBeanRegistrar) newClass.newInstance();
            mBeanReg.doRegister(env);
            mBeanRegList.add(mBeanReg);
        } catch (Exception e) {
            throw new EnvironmentFailureException(DbInternal.getEnvironmentImpl(env), EnvironmentFailureReason.MONITOR_REGISTRATION, e);
        }
    }

    private synchronized void unregisterMBean() throws Exception {
        for (MBeanRegistrar mBeanReg : mBeanRegList) {
            mBeanReg.doUnregister();
        }
    }

    private void clearFileManager() throws DatabaseException {
        if (fileManager != null) {
            try {
                fileManager.clear();
            } catch (IOException IOE) {
            }
            try {
                fileManager.close();
            } catch (IOException IOE) {
            }
        }
    }

    /**
     * Respond to config updates.
     */
    public void envConfigUpdate(DbConfigManager mgr, EnvironmentMutableConfig newConfig) {
        backgroundReadLimit = mgr.getInt(EnvironmentParams.ENV_BACKGROUND_READ_LIMIT);
        backgroundWriteLimit = mgr.getInt(EnvironmentParams.ENV_BACKGROUND_WRITE_LIMIT);
        backgroundSleepInterval = mgr.getDuration(EnvironmentParams.ENV_BACKGROUND_SLEEP_INTERVAL);
        if (newConfig.isConfigParamSet(EnvironmentConfig.CONSOLE_LOGGING_LEVEL)) {
            Level newConsoleHandlerLevel = Level.parse(mgr.get(EnvironmentParams.JE_CONSOLE_LEVEL));
            consoleHandler.setLevel(newConsoleHandlerLevel);
        }
        if (newConfig.isConfigParamSet(EnvironmentConfig.FILE_LOGGING_LEVEL)) {
            Level newFileHandlerLevel = Level.parse(mgr.get(EnvironmentParams.JE_FILE_LEVEL));
            fileHandler.setLevel(newFileHandlerLevel);
        }
        exceptionListener = newConfig.getExceptionListener();
        for (ExceptionListenerUser u : exceptionListenerUsers) {
            u.setExceptionListener(exceptionListener);
        }
        cacheMode = newConfig.getCacheMode();
        cacheModeStrategy = newConfig.getCacheModeStrategy();
        runOrPauseDaemons(mgr);
    }

    public void registerExceptionListenerUser(ExceptionListenerUser u) {
        exceptionListenerUsers.add(u);
    }

    public boolean unregisterExceptionListenerUser(ExceptionListenerUser u) {
        return exceptionListenerUsers.remove(u);
    }

    /**
     * Read configurations for daemons, instantiate.
     */
    private void createDaemons(EnvironmentImpl sharedCacheEnv) throws DatabaseException {
        long evictorWakeupInterval = configManager.getDuration(EnvironmentParams.EVICTOR_WAKEUP_INTERVAL);
        if (sharedCacheEnv != null) {
            assert sharedCache;
            evictor = sharedCacheEnv.evictor;
        } else if (sharedCache) {
            evictor = new SharedEvictor(this, evictorWakeupInterval, "SharedEvictor");
        } else {
            evictor = new PrivateEvictor(this, evictorWakeupInterval, "Evictor");
        }
        long checkpointerWakeupTime = Checkpointer.getWakeupPeriod(configManager);
        checkpointer = new Checkpointer(this, checkpointerWakeupTime, Environment.CHECKPOINTER_NAME);
        long compressorWakeupInterval = configManager.getDuration(EnvironmentParams.COMPRESSOR_WAKEUP_INTERVAL);
        inCompressor = new INCompressor(this, compressorWakeupInterval, Environment.INCOMP_NAME);
        cleaner = new Cleaner(this, Environment.CLEANER_NAME);
    }

    /**
     * Run or pause daemons, depending on config properties.
     */
    private void runOrPauseDaemons(DbConfigManager mgr) {
        if (!isReadOnly) {
            inCompressor.runOrPause(mgr.getBoolean(EnvironmentParams.ENV_RUN_INCOMPRESSOR));
            cleaner.runOrPause(mgr.getBoolean(EnvironmentParams.ENV_RUN_CLEANER) && !isMemOnly);
            checkpointer.runOrPause(mgr.getBoolean(EnvironmentParams.ENV_RUN_CHECKPOINTER));
        }
        evictor.runOrPause(mgr.getBoolean(EnvironmentParams.ENV_RUN_EVICTOR));
    }

    /**
     * Return the incompressor. In general, don't use this directly because
     * it's easy to forget that the incompressor can be null at times (i.e
     * during the shutdown procedure. Instead, wrap the functionality within
     * this class, like lazyCompress.
     */
    public INCompressor getINCompressor() {
        return inCompressor;
    }

    /**
     * Returns the UtilizationTracker.
     */
    public UtilizationTracker getUtilizationTracker() {
        return cleaner.getUtilizationTracker();
    }

    /**
     * Returns the UtilizationProfile.
     */
    public UtilizationProfile getUtilizationProfile() {
        return cleaner.getUtilizationProfile();
    }

    /**
     * Returns the default cache mode for this environment. If the environment
     * has a null cache mode, CacheMode.DEFAULT is returned.  Null is never
     * returned.
     */
    public CacheMode getDefaultCacheMode() {
        if (cacheMode != null) {
            return cacheMode;
        }
        return CacheMode.DEFAULT;
    }

    /**
     * Returns the environment cache mode strategy.  Null may be returned.
     */
    public CacheModeStrategy getDefaultCacheModeStrategy() {
        return cacheModeStrategy;
    }

    /**
     * If a background read limit has been configured and that limit is
     * exceeded when the cumulative total is incremented by the given number of
     * reads, increment the sleep backlog to cause a sleep to occur.  Called by
     * background activities such as the cleaner after performing a file read
     * operation.
     *
     * @see #sleepAfterBackgroundIO
     */
    public void updateBackgroundReads(int nReads) {
        int limit = backgroundReadLimit;
        if (limit > 0) {
            synchronized (backgroundTrackingMutex) {
                backgroundReadCount += nReads;
                if (backgroundReadCount >= limit) {
                    backgroundSleepBacklog += 1;
                    backgroundReadCount -= limit;
                    assert backgroundReadCount >= 0;
                }
            }
        }
    }

    /**
     * If a background write limit has been configured and that limit is
     * exceeded when the given amount written is added to the cumulative total,
     * increment the sleep backlog to cause a sleep to occur.  Called by
     * background activities such as the checkpointer and evictor after
     * performing a file write operation.
     *
     * <p>The number of writes is estimated by dividing the bytes written by
     * the log buffer size.  Since the log write buffer is shared by all
     * writers, this is the best approximation possible.</p>
     *
     * @see #sleepAfterBackgroundIO
     */
    public void updateBackgroundWrites(int writeSize, int logBufferSize) {
        int limit = backgroundWriteLimit;
        if (limit > 0) {
            synchronized (backgroundTrackingMutex) {
                backgroundWriteBytes += writeSize;
                int writeCount = (int) (backgroundWriteBytes / logBufferSize);
                if (writeCount >= limit) {
                    backgroundSleepBacklog += 1;
                    backgroundWriteBytes -= (limit * logBufferSize);
                    assert backgroundWriteBytes >= 0;
                }
            }
        }
    }

    /**
     * If the sleep backlog is non-zero (set by updateBackgroundReads or
     * updateBackgroundWrites), sleep for the configured interval and decrement
     * the backlog.
     *
     * <p>If two threads call this method and the first call causes a sleep,
     * the call by the second thread will block until the first thread's sleep
     * interval is over.  When the call by the second thread is unblocked, if
     * another sleep is needed then the second thread will sleep again.  In
     * other words, when lots of sleeps are needed, background threads may
     * backup.  This is intended to give foreground threads a chance to "catch
     * up" when background threads are doing a lot of IO.</p>
     */
    public void sleepAfterBackgroundIO() {
        if (backgroundSleepBacklog > 0) {
            synchronized (backgroundSleepMutex) {
                try {
                    Thread.sleep(backgroundSleepInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                assert TestHookExecute.doHookIfSet(backgroundSleepHook);
            }
            synchronized (backgroundTrackingMutex) {
                if (backgroundSleepBacklog > 0) {
                    backgroundSleepBacklog -= 1;
                }
            }
        }
    }

    public void setBackgroundSleepHook(TestHook<?> hook) {
        backgroundSleepHook = hook;
    }

    public void setCleanerBarrierHook(TestHook<Long> hook) {
        cleanerBarrierHoook = hook;
    }

    /**
     * @throws IllegalArgumentException via Environment.scanLog.
     */
    public boolean scanLog(long startPosition, long endPosition, LogScanConfig config, LogScanner scanner) throws DatabaseException {
        DbConfigManager cm = getConfigManager();
        int readBufferSize = cm.getInt(EnvironmentParams.LOG_ITERATOR_READ_SIZE);
        long endOfLogLsn = fileManager.getNextLsn();
        boolean forwards = config.getForwards();
        LNFileReader reader = null;
        if (forwards) {
            if (endPosition > endOfLogLsn) {
                throw new IllegalArgumentException("endPosition (" + endPosition + ") is past the end of the log on a forewards scan.");
            }
            reader = new LNFileReader(this, readBufferSize, startPosition, true, endPosition, DbLsn.NULL_LSN, null, DbLsn.NULL_LSN);
        } else {
            if (startPosition > endOfLogLsn) {
                throw new IllegalArgumentException("startPosition (" + startPosition + ") is past the end of the log on a backwards scan.");
            }
            reader = new LNFileReader(this, readBufferSize, startPosition, false, endOfLogLsn, endPosition, null, DbLsn.NULL_LSN);
        }
        reader.addTargetType(LogEntryType.LOG_LN_TRANSACTIONAL);
        reader.addTargetType(LogEntryType.LOG_LN);
        reader.addTargetType(LogEntryType.LOG_DEL_DUPLN_TRANSACTIONAL);
        reader.addTargetType(LogEntryType.LOG_DEL_DUPLN);
        Map<DatabaseId, String> dbNameMap = dbMapTree.getDbNamesAndIds();
        while (reader.readNextEntry()) {
            if (reader.isLN()) {
                LN theLN = reader.getLN();
                byte[] theKey = reader.getKey();
                DatabaseId dbId = reader.getDatabaseId();
                String dbName = dbNameMap.get(dbId);
                if (DbTree.isReservedDbName(dbName)) {
                    continue;
                }
                boolean continueScanning = scanner.scanRecord(new DatabaseEntry(theKey), new DatabaseEntry(theLN.getData()), theLN.isDeleted(), dbName);
                if (!continueScanning) {
                    break;
                }
            }
        }
        return true;
    }

    /**
     * Logs the map tree root and saves the LSN.
     */
    public void logMapTreeRoot() throws DatabaseException {
        logMapTreeRoot(DbLsn.NULL_LSN);
    }

    /**
     * Logs the map tree root, but only if its current LSN is before the
     * ifBeforeLsn parameter or ifBeforeLsn is NULL_LSN.
     */
    public void logMapTreeRoot(long ifBeforeLsn) throws DatabaseException {
        mapTreeRootLatch.acquire();
        try {
            if (ifBeforeLsn == DbLsn.NULL_LSN || DbLsn.compareTo(mapTreeRootLsn, ifBeforeLsn) < 0) {
                mapTreeRootLsn = logManager.log(new SingleItemEntry(LogEntryType.LOG_ROOT, dbMapTree), ReplicationContext.NO_REPLICATE);
            }
        } finally {
            mapTreeRootLatch.release();
        }
    }

    /**
     * Force a rewrite of the map tree root if required.
     */
    public void rewriteMapTreeRoot(long cleanerTargetLsn) throws DatabaseException {
        mapTreeRootLatch.acquire();
        try {
            if (DbLsn.compareTo(cleanerTargetLsn, mapTreeRootLsn) == 0) {
                mapTreeRootLsn = logManager.log(new SingleItemEntry(LogEntryType.LOG_ROOT, dbMapTree), ReplicationContext.NO_REPLICATE);
            }
        } finally {
            mapTreeRootLatch.release();
        }
    }

    /**
     * @return the mapping tree root LSN.
     */
    public long getRootLsn() {
        return mapTreeRootLsn;
    }

    /**
     * Set the mapping tree from the log. Called during recovery.
     */
    public void readMapTreeFromLog(long rootLsn) throws DatabaseException {
        if (dbMapTree != null) {
            dbMapTree.close();
        }
        dbMapTree = (DbTree) logManager.getEntryHandleFileNotFound(rootLsn);
        if (!dbMapTree.isReplicated() && getAllowConvert()) {
            dbMapTree.setIsReplicated();
            dbMapTree.setIsConverted();
            needConvert = true;
        }
        dbMapTree.initExistingEnvironment(this);
        mapTreeRootLatch.acquire();
        try {
            mapTreeRootLsn = rootLsn;
        } finally {
            mapTreeRootLatch.release();
        }
    }

    /**
     * Tells the asynchronous IN compressor thread about a BIN with a deleted
     * entry.
     */
    public void addToCompressorQueue(BIN bin, Key deletedKey, boolean doWakeup) {
        if (inCompressor != null) {
            inCompressor.addBinKeyToQueue(bin, deletedKey, doWakeup);
        }
    }

    /**
     * Tells the asynchronous IN compressor thread about a BINReference with a
     * deleted entry.
     */
    public void addToCompressorQueue(BINReference binRef, boolean doWakeup) {
        if (inCompressor != null) {
            inCompressor.addBinRefToQueue(binRef, doWakeup);
        }
    }

    /**
     * Tells the asynchronous IN compressor thread about a collections of
     * BINReferences with deleted entries.
     */
    public void addToCompressorQueue(Collection<BINReference> binRefs, boolean doWakeup) {
        if (inCompressor != null) {
            inCompressor.addMultipleBinRefsToQueue(binRefs, doWakeup);
        }
    }

    /**
     * Do lazy compression at opportune moments.
     */
    public void lazyCompress(IN in, LocalUtilizationTracker localTracker) throws DatabaseException {
        if (inCompressor != null) {
            inCompressor.lazyCompress(in, localTracker);
        }
    }

    /**
     * Reset the logging level for specified loggers in a JE environment.
     *
     * @throws IllegalArgumentException via JEDiagnostics.OP_RESET_LOGGING
     */
    public void resetLoggingLevel(String changedLoggerName, Level level) {
        java.util.logging.LogManager loggerManager = java.util.logging.LogManager.getLogManager();
        Enumeration<String> loggers = loggerManager.getLoggerNames();
        boolean validName = false;
        while (loggers.hasMoreElements()) {
            String loggerName = loggers.nextElement();
            Logger logger = loggerManager.getLogger(loggerName);
            if ("all".equals(changedLoggerName) || loggerName.endsWith(changedLoggerName) || loggerName.endsWith(changedLoggerName + LoggerUtils.NO_ENV) || loggerName.endsWith(changedLoggerName + LoggerUtils.FIXED_PREFIX) || loggerName.startsWith(changedLoggerName)) {
                logger.setLevel(level);
                validName = true;
            }
        }
        if (!validName) {
            throw new IllegalArgumentException("The logger name parameter: " + changedLoggerName + " is invalid!");
        }
    }

    public void pushMemoryHandler() {
        if (memoryHandler.getLevel() != Level.OFF) {
            Level level = memoryTarget.getLevel();
            memoryTarget.publish(new LogRecord(level, "***************************************"));
            memoryTarget.publish(new LogRecord(level, "Start pushing out memory handler......."));
            memoryHandler.push();
            memoryHandler.flush();
            memoryTarget.publish(new LogRecord(level, "Finish pushing out memory handler......"));
            memoryTarget.publish(new LogRecord(level, "***************************************"));
        }
    }

    protected Formatter initFormatter() {
        return new TracerFormatter(getName());
    }

    private MemoryHandler initMemoryHandler() {
        String memHandlerName = com.sleepycat.je.util.MemoryHandler.class.getName();
        int memoryHandlerSize = 1000;
        String size = LoggerUtils.getLoggerProperty(memHandlerName + ".size");
        if (size != null) {
            memoryHandlerSize = Integer.valueOf(size);
        }
        memoryTarget = new ConsoleHandler();
        memoryTarget.setLevel(Level.ALL);
        memoryTarget.setFormatter(formatter);
        Level pushLevel = LoggerUtils.getPushLevel(memHandlerName);
        return new com.sleepycat.je.util.MemoryHandler(memoryTarget, memoryHandlerSize, pushLevel, formatter);
    }

    private FileHandler initFileHandler() throws DatabaseException {
        if ((envHome == null) || (!envHome.isDirectory())) {
            return null;
        }
        String handlerName = com.sleepycat.je.util.FileHandler.class.getName();
        String logFilePattern = envHome + "/" + INFO_FILES;
        int limit = FILEHANDLER_LIMIT;
        String logLimit = LoggerUtils.getLoggerProperty(handlerName + ".limit");
        if (logLimit != null) {
            limit = Integer.parseInt(logLimit);
        }
        int count = FILEHANDLER_COUNT;
        String logCount = LoggerUtils.getLoggerProperty(handlerName + ".count");
        if (logCount != null) {
            count = Integer.parseInt(logCount);
        }
        try {
            return new com.sleepycat.je.util.FileHandler(logFilePattern, limit, count, formatter, this);
        } catch (IOException e) {
            throw EnvironmentFailureException.unexpectedException("Problem creating output files in: " + logFilePattern, e);
        }
    }

    public ConsoleHandler getConsoleHandler() {
        return consoleHandler;
    }

    public MemoryHandler getMemoryHandler() {
        return memoryHandler;
    }

    public FileHandler getFileHandler() {
        return fileHandler;
    }

    private void closeHandlers() {
        if (consoleHandler != null) {
            consoleHandler.close();
        }
        if (memoryHandler != null) {
            memoryHandler.close();
        }
        if (fileHandler != null) {
            fileHandler.close();
        }
    }

    /**
     * Not much to do, mark state.
     */
    public void open() {
        envState = DbEnvState.OPEN;
    }

    /**
     * Invalidate the environment. Done when a fatal exception
     * (EnvironmentFailureException) is thrown.
     */
    public void invalidate(EnvironmentFailureException e) {
        savedInvalidatingException = e;
        envState = DbEnvState.INVALID;
        requestShutdownDaemons();
    }

    /**
     * Invalidate the environment when a Java Error is thrown.
     */
    public void invalidate(Error e) {
        if (SAVED_EFE.getCause() == null) {
            SAVED_EFE.initCause(e);
            invalidate(SAVED_EFE);
        }
    }

    /**
     * Predicate used to determine whether the EnvironmentImpl is valid.
     *
     * @return true if it's valid, false otherwise
     */
    public boolean isInvalid() {
        return (savedInvalidatingException != null);
    }

    /**
     * @return true if environment is open.
     */
    public boolean isValid() {
        return (envState == DbEnvState.OPEN);
    }

    /**
     * @return true if environment is still in init
     */
    public boolean isInInit() {
        return (envState == DbEnvState.INIT);
    }

    /**
     * @return true if close has begun, although the state may still be open.
     */
    public boolean isClosing() {
        return closing;
    }

    public boolean isClosed() {
        return (envState == DbEnvState.CLOSED);
    }

    /**
     * When a EnvironmentFailureException occurs or the environment is closed,
     * further writing can cause log corruption.
     */
    public boolean mayNotWrite() {
        return (envState == DbEnvState.INVALID) || (envState == DbEnvState.CLOSED);
    }

    public void checkIfInvalid() throws EnvironmentFailureException {
        if (envState == DbEnvState.INVALID) {
            savedInvalidatingException.setAlreadyThrown(true);
            if (savedInvalidatingException == SAVED_EFE) {
                savedInvalidatingException.fillInStackTrace();
                throw savedInvalidatingException;
            }
            throw savedInvalidatingException.wrapSelf("Environment must be closed, caused by: " + savedInvalidatingException);
        }
    }

    public void checkNotClosed() throws DatabaseException {
        if (envState == DbEnvState.CLOSED) {
            throw new IllegalStateException("Attempt to use a Environment that has been closed.");
        }
    }

    /**
     * Decrements the reference count and closes the environment when it
     * reaches zero.  A checkpoint is always performed when closing.
     */
    public void close() throws DatabaseException {
        DbEnvPool.getInstance().closeEnvironment(this, true, true);
    }

    /**
     * Decrements the reference count and closes the environment when it
     * reaches zero.  A checkpoint when closing is optional.
     */
    public void close(boolean doCheckpoint) throws DatabaseException {
        DbEnvPool.getInstance().closeEnvironment(this, doCheckpoint, true);
    }

    /**
     * Used by error handling to forcibly close an environment, and by tests to
     * close an environment to simulate a crash.  Database handles do not have
     * to be closed before calling this method.  A checkpoint is not performed.
     */
    public void abnormalClose() throws DatabaseException {
        int count = getReferenceCount();
        if (count > 1) {
            throw EnvironmentFailureException.unexpectedState(this, "Abnormal close assumes that the reference count on " + "this handle is 1, not " + count);
        }
        DbEnvPool.getInstance().closeEnvironment(this, false, false);
    }

    /**
     * Closes the environment, optionally performing a checkpoint and checking
     * for resource leaks.  This method must be called while synchronized on
     * DbEnvPool.
     *
     * @throws IllegalStateException if the environment is already closed.
     *
     * @throws EnvironmentFailureException if leaks or other problems are
     * detected while closing.
     */
    synchronized void doClose(boolean doCheckpoint, boolean doCheckLeaks) {
        StringWriter errorStringWriter = new StringWriter();
        PrintWriter errors = new PrintWriter(errorStringWriter);
        try {
            Trace.traceLazily(this, "Close of environment " + envHome + " started");
            LoggerUtils.fine(envLogger, this, "Close of environment " + envHome + " started");
            envState.checkState(DbEnvState.VALID_FOR_CLOSE, DbEnvState.CLOSED);
            setupClose(errors);
            requestShutdownDaemons();
            try {
                unregisterMBean();
            } catch (Exception e) {
                errors.append("\nException unregistering MBean: ");
                e.printStackTrace(errors);
                errors.println();
            }
            boolean checkpointHappened = false;
            if (doCheckpoint && !isReadOnly && (envState != DbEnvState.INVALID) && logManager.getLastLsnAtRecovery() != fileManager.getLastUsedLsn()) {
                CheckpointConfig ckptConfig = new CheckpointConfig();
                ckptConfig.setForce(true);
                ckptConfig.setMinimizeRecoveryTime(true);
                try {
                    invokeCheckpoint(ckptConfig, false, "close");
                } catch (DatabaseException e) {
                    errors.append("\nException performing checkpoint: ");
                    e.printStackTrace(errors);
                    errors.println();
                }
                checkpointHappened = true;
            }
            postCheckpointClose(checkpointHappened);
            LoggerUtils.fine(envLogger, this, "About to shutdown daemons for Env " + envHome);
            shutdownDaemons();
            try {
                logManager.flush();
            } catch (Exception e) {
                errors.append("\nException flushing log manager: ");
                e.printStackTrace(errors);
                errors.println();
            }
            try {
                fileManager.clear();
            } catch (Exception e) {
                errors.append("\nException clearing file manager: ");
                e.printStackTrace(errors);
                errors.println();
            }
            try {
                fileManager.close();
            } catch (Exception e) {
                errors.append("\nException closing file manager: ");
                e.printStackTrace(errors);
                errors.println();
            }
            dbMapTree.close();
            cleaner.close();
            inMemoryINs.clear();
            closeHandlers();
            if (doCheckLeaks && (envState != DbEnvState.INVALID)) {
                try {
                    checkLeaks();
                } catch (Exception e) {
                    errors.append("\nException performing validity checks: ");
                    e.printStackTrace(errors);
                    errors.println();
                }
            }
        } finally {
            envState = DbEnvState.CLOSED;
        }
        if (errorStringWriter.getBuffer().length() > 0 && savedInvalidatingException == null) {
            throw EnvironmentFailureException.unexpectedState(errorStringWriter.toString());
        }
    }

    /**
     * Release any resources from a subclass that need to be released before
     * close is called on regular environment components.
     * @throws DatabaseException
     */
    protected synchronized void setupClose(PrintWriter errors) throws DatabaseException {
    }

    /**
     * Release any resources from a subclass that need to be released after
     * the closing checkpoint.
     * @param checkpointed if true, a checkpoint as issued before the close
     * @throws DatabaseException
     */
    protected synchronized void postCheckpointClose(boolean checkpointed) throws DatabaseException {
    }

    /**
     * Convert user defined databases to replicated after doing recovery.
     *
     * @throws DatabaseException
     */
    protected void postRecoveryConversion() throws DatabaseException {
    }

    public void closeAfterInvalid() throws DatabaseException {
        DbEnvPool.getInstance().closeEnvironmentAfterInvalid(this);
    }

    /**
     * This method must be called while synchronized on DbEnvPool.
     */
    public synchronized void doCloseAfterInvalid() {
        try {
            unregisterMBean();
        } catch (Exception e) {
        }
        shutdownDaemons();
        try {
            fileManager.clear();
        } catch (Exception e) {
        }
        try {
            fileManager.close();
        } catch (Exception e) {
        }
        closeHandlers();
    }

    synchronized void incReferenceCount() {
        referenceCount++;
    }

    /**
     * Returns true if the environment should be closed.
     */
    synchronized boolean decReferenceCount() {
        return (--referenceCount <= 0);
    }

    protected synchronized int getReferenceCount() {
        return referenceCount;
    }

    public static int getThreadLocalReferenceCount() {
        return threadLocalReferenceCount;
    }

    static synchronized void incThreadLocalReferenceCount() {
        threadLocalReferenceCount++;
    }

    static synchronized void decThreadLocalReferenceCount() {
        threadLocalReferenceCount--;
    }

    public static boolean getNoComparators() {
        return noComparators;
    }

    /**
     * Debugging support. Check for leaked locks and transactions.
     */
    private void checkLeaks() throws DatabaseException {
        if (!configManager.getBoolean(EnvironmentParams.ENV_CHECK_LEAKS)) {
            return;
        }
        boolean clean = true;
        StatsConfig statsConfig = new StatsConfig();
        statsConfig.setFast(false);
        LockStats lockStat = lockStat(statsConfig);
        if (lockStat.getNTotalLocks() != 0) {
            clean = false;
            System.err.println("Problem: " + lockStat.getNTotalLocks() + " locks left");
            txnManager.getLockManager().dump();
        }
        TransactionStats txnStat = txnStat(statsConfig);
        if (txnStat.getNActive() != 0) {
            clean = false;
            System.err.println("Problem: " + txnStat.getNActive() + " txns left");
            TransactionStats.Active[] active = txnStat.getActiveTxns();
            if (active != null) {
                for (Active element : active) {
                    System.err.println(element);
                }
            }
        }
        if (LatchSupport.countLatchesHeld() > 0) {
            clean = false;
            System.err.println("Some latches held at env close.");
            LatchSupport.dumpLatchesHeld();
        }
        long memoryUsage = memoryBudget.getVariableCacheUsage();
        if (memoryUsage != 0) {
            clean = false;
            System.err.println("Local Cache Usage = " + memoryUsage);
            System.err.println(memoryBudget.loadStats());
        }
        boolean assertionsEnabled = false;
        assert assertionsEnabled = true;
        if (!clean && assertionsEnabled) {
            throw EnvironmentFailureException.unexpectedState("Lock, transaction, latch or memory " + "left behind at environment close");
        }
    }

    /**
     * Invoke a checkpoint programmatically. Note that only one checkpoint may
     * run at a time.
     */
    public boolean invokeCheckpoint(CheckpointConfig config, boolean flushAll, String invokingSource) throws DatabaseException {
        if (checkpointer != null) {
            checkpointer.doCheckpoint(config, flushAll, invokingSource);
            return true;
        }
        return false;
    }

    /**
     * Flip the log to a new file, forcing an fsync.  Return the LSN of the
     * trace record in the new file.
     */
    public long forceLogFileFlip() throws DatabaseException {
        return logManager.logForceFlip(new SingleItemEntry(LogEntryType.LOG_TRACE, new Trace("File Flip")));
    }

    /**
     * Invoke a compress programatically. Note that only one compress may run
     * at a time.
     */
    public boolean invokeCompressor() throws DatabaseException {
        if (inCompressor != null) {
            inCompressor.doCompress();
            return true;
        }
        return false;
    }

    public void invokeEvictor() throws DatabaseException {
        if (evictor != null) {
            evictor.doEvict(EvictionSource.MANUAL);
        }
    }

    /**
     * @throws UnsupportedOperationException via Environment.cleanLog.
     */
    public int invokeCleaner() throws DatabaseException {
        if (isReadOnly || isMemOnly) {
            throw new UnsupportedOperationException("Log cleaning not allowed in a read-only or memory-only " + "environment");
        }
        if (cleaner != null) {
            return cleaner.doClean(true, false);
        }
        return 0;
    }

    private void requestShutdownDaemons() {
        closing = true;
        if (inCompressor != null) {
            inCompressor.requestShutdown();
        }
        if (evictor != null && !sharedCache) {
            evictor.requestShutdown();
        }
        if (checkpointer != null) {
            checkpointer.requestShutdown();
        }
        if (cleaner != null) {
            cleaner.requestShutdown();
        }
    }

    /**
     * For unit testing -- shuts down daemons completely but leaves environment
     * usable since environment references are not nulled out.
     */
    public void stopDaemons() {
        if (inCompressor != null) {
            inCompressor.shutdown();
        }
        if (evictor != null) {
            evictor.shutdown();
        }
        if (checkpointer != null) {
            checkpointer.shutdown();
        }
        if (cleaner != null) {
            cleaner.shutdown();
        }
    }

    /**
     * Ask all daemon threads to shut down.
     */
    protected void shutdownDaemons() {
        shutdownINCompressor();
        shutdownCleaner();
        shutdownCheckpointer();
        shutdownEvictor();
    }

    void shutdownINCompressor() {
        if (inCompressor != null) {
            inCompressor.shutdown();
            inCompressor.clearEnv();
            inCompressor = null;
        }
        return;
    }

    void shutdownEvictor() {
        if (evictor != null) {
            if (sharedCache) {
                evictor.removeEnvironment(this);
            } else {
                evictor.shutdown();
                evictor.clearEnv();
                evictor = null;
            }
        }
        return;
    }

    void shutdownCheckpointer() {
        if (checkpointer != null) {
            checkpointer.shutdown();
            checkpointer.clearEnv();
            checkpointer = null;
        }
        return;
    }

    /**
     * public for unit tests.
     */
    public void shutdownCleaner() {
        if (cleaner != null) {
            cleaner.shutdown();
        }
        return;
    }

    public boolean isNoLocking() {
        return isNoLocking;
    }

    public boolean isTransactional() {
        return isTransactional;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public boolean isMemOnly() {
        return isMemOnly;
    }

    public String getNodeName() {
        return nodeName;
    }

    public static boolean getFairLatches() {
        return fairLatches;
    }

    public static boolean getSharedLatches() {
        return useSharedLatchesForINs;
    }

    /**
     * Returns whether DB/MapLN eviction is enabled.
     */
    public boolean getDbEviction() {
        return dbEviction;
    }

    public static int getAdler32ChunkSize() {
        return adler32ChunkSize;
    }

    public boolean getSharedCache() {
        return sharedCache;
    }

    /**
     * Transactional services.
     */
    public Txn txnBegin(Transaction parent, TransactionConfig txnConfig) throws DatabaseException {
        return txnManager.txnBegin(parent, txnConfig);
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public DbTree getDbTree() {
        return dbMapTree;
    }

    /**
     * Returns the config manager for the current base configuration.
     *
     * <p>The configuration can change, but changes are made by replacing the
     * config manager object with a enw one.  To use a consistent set of
     * properties, call this method once and query the returned manager
     * repeatedly for each property, rather than getting the config manager via
     * this method for each property individually.</p>
     */
    public DbConfigManager getConfigManager() {
        return configManager;
    }

    public NodeSequence getNodeSequence() {
        return nodeSequence;
    }

    /**
     * Clones the current configuration.
     */
    public EnvironmentConfig cloneConfig() {
        return configManager.getEnvironmentConfig().clone();
    }

    /**
     * Clones the current mutable configuration.
     */
    public EnvironmentMutableConfig cloneMutableConfig() {
        return DbInternal.cloneMutableConfig(configManager.getEnvironmentConfig());
    }

    /**
     * Throws an exception if an immutable property is changed.
     */
    public void checkImmutablePropsForEquality(Properties handleConfigProps) throws IllegalArgumentException {
        DbInternal.checkImmutablePropsForEquality(configManager.getEnvironmentConfig(), handleConfigProps);
    }

    /**
     * Changes the mutable config properties that are present in the given
     * config, and notifies all config observer.
     */
    public void setMutableConfig(EnvironmentMutableConfig config) throws DatabaseException {
        DbEnvPool.getInstance().setMutableConfig(this, config);
    }

    /**
     * This method must be called while synchronized on DbEnvPool.
     */
    synchronized void doSetMutableConfig(EnvironmentMutableConfig config) throws DatabaseException {
        EnvironmentConfig newConfig = configManager.getEnvironmentConfig().clone();
        DbInternal.copyMutablePropsTo(config, newConfig);
        configManager = resetConfigManager(newConfig);
        for (int i = configObservers.size() - 1; i >= 0; i -= 1) {
            EnvConfigObserver o = configObservers.get(i);
            o.envConfigUpdate(configManager, newConfig);
        }
    }

    /**
     * Make a new config manager that has all the properties needed. More
     * complicated for subclasses.
     */
    protected DbConfigManager resetConfigManager(EnvironmentConfig newConfig) {
        return new DbConfigManager(newConfig);
    }

    public ExceptionListener getExceptionListener() {
        return exceptionListener;
    }

    /**
     * Adds an observer of mutable config changes.
     */
    public synchronized void addConfigObserver(EnvConfigObserver o) {
        configObservers.add(o);
    }

    /**
     * Removes an observer of mutable config changes.
     */
    public synchronized void removeConfigObserver(EnvConfigObserver o) {
        configObservers.remove(o);
    }

    public INList getInMemoryINs() {
        return inMemoryINs;
    }

    public TxnManager getTxnManager() {
        return txnManager;
    }

    public Checkpointer getCheckpointer() {
        return checkpointer;
    }

    public Cleaner getCleaner() {
        return cleaner;
    }

    public MemoryBudget getMemoryBudget() {
        return memoryBudget;
    }

    /**
     * @return environment Logger, for use in debugging output.
     */
    public Logger getLogger() {
        return envLogger;
    }

    public boolean isDbLoggingDisabled() {
        return dbLoggingDisabled;
    }

    public boolean verify(VerifyConfig config, PrintStream out) throws DatabaseException {
        return dbMapTree.verify(config, out);
    }

    public void verifyCursors() throws DatabaseException {
        inCompressor.verifyCursors();
    }

    /**
     * Retrieve and return stat information.
     */
    public synchronized EnvironmentStats loadStats(StatsConfig config) throws DatabaseException {
        EnvironmentStats envStats = new EnvironmentStats();
        envStats.setINCompStats(inCompressor.loadStats(config));
        envStats.setCkptStats(checkpointer.loadStats(config));
        envStats.setCleanerStats(cleaner.loadStats(config));
        envStats.setLogStats(logManager.loadStats(config));
        envStats.setMBAndEvictorStats(memoryBudget.loadStats(), evictor.loadStats(config));
        envStats.setLockStats(txnManager.loadStats(config));
        envStats.setEnvImplStats(loadEnvImplStats(config));
        return envStats;
    }

    public StatGroup loadEnvImplStats(StatsConfig config) {
        return stats.cloneGroup(config.getClear());
    }

    public void incRelatchesRequired() {
        relatchesRequired.increment();
    }

    /**
     * For replicated environments only; just return true for a standalone
     * environment.
     */
    public boolean addDbBackup(DbBackup backup) {
        return true;
    }

    /**
     * For replicated environments only; do nothing for a standalone
     * environment.
     */
    public void removeDbBackup(DbBackup backup) {
    }

    /**
     * Retrieve lock statistics
     */
    public synchronized LockStats lockStat(StatsConfig config) throws DatabaseException {
        return txnManager.lockStat(config);
    }

    /**
     * Retrieve txn statistics
     */
    public synchronized TransactionStats txnStat(StatsConfig config) {
        return txnManager.txnStat(config);
    }

    public int getINCompressorQueueSize() {
        return inCompressor.getBinRefQueueSize();
    }

    /**
     * Info about the last recovery.
     */
    public RecoveryInfo getLastRecoveryInfo() {
        return lastRecoveryInfo;
    }

    /**
     * Get the environment home directory.
     */
    public File getEnvironmentHome() {
        return envHome;
    }

    /**
     * Get an environment name, for tagging onto logging and debug message.
     * Useful for multiple environments in a JVM, or for HA.
     */
    public String getName() {
        if (nodeName == null) {
            return envHome.toString();
        } else {
            return getNodeName();
        }
    }

    public long getTxnTimeout() {
        return txnTimeout;
    }

    public long getLockTimeout() {
        return lockTimeout;
    }

    public long getReplayTxnTimeout() {
        if (lockTimeout != 0) {
            return lockTimeout;
        } else {
            return 1;
        }
    }

    /**
     * Returns the shared trigger latch.
     */
    public SharedLatch getTriggerLatch() {
        return triggerLatch;
    }

    public Evictor getEvictor() {
        return evictor;
    }

    void alertEvictor() {
        if (evictor != null) {
            evictor.alert();
        }
    }

    /**
     * Performs critical eviction if necessary.  Is called before and after
     * each cursor operation.
     *
     * May be used here or by an overridden method in RepImpl to perform
     * periodic actions.  Since this method is called often by app threads, it
     * may be used as a substitute for creating an internal thread.
     *
     * WARNING: The action performed here should be as inexpensive as possible,
     * since it will impact app operation latency.  Unconditional
     * synchronization must not be performed, since that would introduce a new
     * synchronization point for all app threads.
     *
     * An overriding method must call super.criticalEviction.
     *
     * No latches are held or synchronization is in use when this method is
     * called.
     */
    public void criticalEviction(boolean backgroundIO) {
        evictor.doCriticalEviction(backgroundIO);
    }

    /**
     * Performs special eviction (eviction other than standard IN eviction)
     * for this environment.  This method is called once per eviction batch to
     * give other components an opportunity to perform eviction.  For a shared
     * cached, it is called for only one environment (in rotation) per batch.
     *
     * An overriding method must call super.specialEviction and return the sum
     * of the long value it returns and any additional amount of budgeted
     * memory that is evicted.
     *
     * No latches are held when this method is called, but it is called while
     * synchronized on the evictor.
     *
     * @return the number of bytes evicted from the JE cache.
     */
    public long specialEviction() {
        return cleaner.getUtilizationTracker().evictMemory();
    }

    /**
     * See Evictor.isCacheFull
     */
    public boolean isCacheFull() {
        return getEvictor().isCacheFull();
    }

    /**
     * See Evictor.wasCacheEverFull
     */
    public boolean wasCacheEverFull() {
        return getEvictor().wasCacheEverFull();
    }

    /**
     * For stress testing.  Should only ever be called from an assert.
     */
    public static boolean maybeForceYield() {
        if (forcedYield) {
            Thread.yield();
        }
        return true;
    }

    /**
     * Return true if this environment is part of a replication group.
     */
    public boolean isReplicated() {
        return false;
    }

    /**
     * True if ReplicationConfig set allowConvert as true. Standalone
     * environment is prohibited to do conversion, return false always.
     */
    public boolean getAllowConvert() {
        return false;
    }

    /**
     * True if this environment is converted from non-replicated to
     * replicated.
     */
    public boolean isConverted() {
        return dbMapTree.isConverted();
    }

    public boolean needConvert() {
        return needConvert;
    }

    public VLSN bumpVLSN() {
        return null;
    }

    public void decrementVLSN() {
    }

    /**
     * @throws DatabaseException from subclasses.
     */
    public VLSNRecoveryProxy getVLSNProxy() throws DatabaseException {
        return new NoopVLSNProxy();
    }

    public boolean isMaster() {
        return false;
    }

    /**
     */
    public void preRecoveryCheckpointInit(RecoveryInfo recoveryInfo) {
    }

    public void registerVLSN(LogItem logItem) {
    }

    /**
     * Adjust the vlsn index after cleaning.
     */
    public void vlsnHeadTruncate(VLSN lastVLSN, long deleteFileNum) {
    }

    /**
     * Do any work that must be done before the checkpoint end is written, as
     * as part of the checkpoint process.
     * @throws DatabaseException
     */
    public void preCheckpointEndFlush() throws DatabaseException {
    }

    /**
     * For replicated environments only; only the overridden method should
     * ever be called.
     * @throws DatabaseException from subclasses.
     */
    public Txn createReplayTxn(long txnId) {
        throw EnvironmentFailureException.unexpectedState("Should not be called on a non replicated environment");
    }

    /**
     * For replicated environments only; only the overridden method should
     * ever be called.
     * @throws DatabaseException from subclasses.
     */
    public ThreadLocker createRepThreadLocker() {
        throw EnvironmentFailureException.unexpectedState("Should not be called on a non replicated environment");
    }

    /**
     * For replicated environments only; only the overridden method should
     * ever be called.
     * @throws DatabaseException from subclasses.
     */
    public Txn createRepUserTxn(TransactionConfig config) {
        throw EnvironmentFailureException.unexpectedState("Should not be called on a non replicated environment");
    }

    /**
     * For replicated environments only; only the overridden method should
     * ever be called.
     * @throws DatabaseException from subclasses.
     */
    public Txn createRepTxn(TransactionConfig config, long mandatedId) {
        throw EnvironmentFailureException.unexpectedState("Should not be called on a non replicated environment");
    }

    /**
     * For replicated environments only; only the overridden method should
     * ever be called.
     * @throws com.sleepycat.je.rep.LockPreemptedException from subclasses.
     */
    public OperationFailureException createLockPreemptedException(Locker locker, Throwable cause) {
        throw EnvironmentFailureException.unexpectedState("Should not be called on a non replicated environment");
    }

    /**
     * For replicated environments only; only the overridden method should
     * ever be called.
     * @throws com.sleepycat.je.rep.DatabasePreemptedException from subclasses.
     */
    public OperationFailureException createDatabasePreemptedException(String msg, String dbName, Database db) {
        throw EnvironmentFailureException.unexpectedState("Should not be called on a non replicated environment");
    }

    /**
     * For replicated environments only; only the overridden method should
     * ever be called.
     * @throws com.sleepycat.je.rep.LogOverwriteException from subclasses.
     */
    public OperationFailureException createLogOverwriteException(String msg) {
        throw EnvironmentFailureException.unexpectedState("Should not be called on a non replicated environment");
    }

    /**
     * Returns the first protected file number.  All files from this file
     * (inclusive) to the end of the log will be protected from deletion.
     *
     * For replicated environments, this method should be overridden to return
     * the CBVLSN file.
     *
     * Returns -1 if all file deletion is prohibited.
     *
     * Requirement:  This method may never return a file number less that
     * (prior to) a file number returned earlier.
     */
    public long getCleanerBarrierStartFile() {
        if (cleanerBarrierHoook != null) {
            return cleanerBarrierHoook.getHookValue();
        }
        return Long.MAX_VALUE;
    }

    /**
     * Check whether this environment can be opened on an existing environment
     * directory.
     *
     * @throws UnsupportedOperationException via Environment ctor.
     */
    public void checkRulesForExistingEnv(boolean dbTreeReplicatedBit) throws UnsupportedOperationException {
        if (dbTreeReplicatedBit && (!isReadOnly())) {
            throw new UnsupportedOperationException("This environment was previously opened for replication." + " It cannot be re-opened for in read/write mode for" + " non-replicated operation.");
        }
    }

    /**
     * The VLSNRecoveryProxy is only needed for replicated environments.
     */
    private class NoopVLSNProxy implements VLSNRecoveryProxy {

        public void trackMapping(long lsn, LogEntryHeader currentEntryHeader, LogEntry targetLogEntry) {
        }
    }
}
