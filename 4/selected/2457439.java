package org.archive.crawler.framework;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import org.apache.commons.httpclient.URIException;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.admin.StatisticsTracker;
import org.archive.crawler.datamodel.Checkpoint;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.ServerCache;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.event.CrawlURIDispositionListener;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.crawler.io.LocalErrorFormatter;
import org.archive.crawler.io.RuntimeErrorFormatter;
import org.archive.crawler.io.StatisticsLogFormatter;
import org.archive.crawler.io.UriErrorFormatter;
import org.archive.crawler.io.UriProcessingFormatter;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.crawler.util.CheckpointUtils;
import org.archive.io.GenerationFileHandler;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.ArchiveUtils;
import org.archive.util.CachedBdbMap;
import org.archive.util.FileUtils;
import org.archive.util.Reporter;
import org.archive.util.bdbje.EnhancedEnvironment;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.utilint.DbLsn;

/**
 * CrawlController collects all the classes which cooperate to
 * perform a crawl and provides a high-level interface to the
 * running crawl.
 *
 * As the "global context" for a crawl, subcomponents will
 * often reach each other through the CrawlController.
 *
 * @author Gordon Mohr
 */
public class CrawlController implements Serializable, Reporter {

    private static final long serialVersionUID = ArchiveUtils.classnameBasedUID(CrawlController.class, 1);

    /**
     * Messages from the crawlcontroller.
     *
     * They appear on console.
     */
    private static final Logger LOGGER = Logger.getLogger(CrawlController.class.getName());

    /** abbrieviation label for config files in manifest */
    public static final char MANIFEST_CONFIG_FILE = 'C';

    /** abbrieviation label for report files in manifest */
    public static final char MANIFEST_REPORT_FILE = 'R';

    /** abbrieviation label for log files in manifest */
    public static final char MANIFEST_LOG_FILE = 'L';

    private static final String LOGNAME_PROGRESS_STATISTICS = "progress-statistics";

    private static final String LOGNAME_URI_ERRORS = "uri-errors";

    private static final String LOGNAME_RUNTIME_ERRORS = "runtime-errors";

    private static final String LOGNAME_LOCAL_ERRORS = "local-errors";

    private static final String LOGNAME_CRAWL = "crawl";

    private transient CrawlOrder order;

    private transient CrawlScope scope;

    private transient ProcessorChainList processorChains;

    private transient Frontier frontier;

    private transient ToePool toePool;

    private transient ServerCache serverCache;

    private transient SettingsHandler settingsHandler;

    private transient volatile boolean singleThreadMode = false;

    private transient ReentrantLock singleThreadLock = null;

    private transient LinkedList<char[]> reserveMemory;

    private static final int RESERVE_BLOCKS = 1;

    private static final int RESERVE_BLOCK_SIZE = 6 * 2 ^ 20;

    /**
     * Crawl exit status.
     */
    private transient String sExit;

    private static final Object NASCENT = "NASCENT".intern();

    private static final Object RUNNING = "RUNNING".intern();

    private static final Object PAUSED = "PAUSED".intern();

    private static final Object PAUSING = "PAUSING".intern();

    private static final Object CHECKPOINTING = "CHECKPOINTING".intern();

    private static final Object STOPPING = "STOPPING".intern();

    private static final Object FINISHED = "FINISHED".intern();

    private static final Object STARTED = "STARTED".intern();

    private static final Object PREPARING = "PREPARING".intern();

    private transient Object state = NASCENT;

    private transient File disk;

    private transient File logsDisk;

    /**
     * For temp files representing state of crawler (eg queues)
     */
    private transient File stateDisk;

    /**
     * For discardable temp files (eg fetch buffers).
     */
    private transient File scratchDisk;

    /**
     * Directory that holds checkpoint.
     */
    private transient File checkpointsDisk;

    /**
     * Checkpointer.
     * Knows if checkpoint in progress and what name of checkpoint is.  Also runs
     * checkpoints.
     */
    private Checkpointer checkpointer;

    /**
     * Gets set to checkpoint we're in recovering if in checkpoint recover
     * mode.  Gets setup by {@link #getCheckpointRecover()}.
     */
    private transient Checkpoint checkpointRecover = null;

    private long maxBytes;

    private long maxDocument;

    private long maxTime;

    /**
     * A manifest of all files used/created during this crawl. Written to file
     * at the end of the crawl (the absolutely last thing done).
     */
    private StringBuffer manifest;

    /**
     * Record of fileHandlers established for loggers,
     * assisting file rotation.
     */
    private transient Map<Logger, FileHandler> fileHandlers;

    /** suffix to use on active logs */
    public static final String CURRENT_LOG_SUFFIX = ".log";

    /**
     * Crawl progress logger.
     *
     * No exceptions.  Logs summary result of each url processing.
     */
    public transient Logger uriProcessing;

    /**
     * This logger contains unexpected runtime errors.
     *
     * Would contain errors trying to set up a job or failures inside
     * processors that they are not prepared to recover from.
     */
    public transient Logger runtimeErrors;

    /**
     * This logger is for job-scoped logging, specifically errors which
     * happen and are handled within a particular processor.
     *
     * Examples would be socket timeouts, exceptions thrown by extractors, etc.
     */
    public transient Logger localErrors;

    /**
     * Special log for URI format problems, wherever they may occur.
     */
    public transient Logger uriErrors;

    /**
     * Statistics tracker writes here at regular intervals.
     */
    private transient Logger progressStats;

    /**
     * Logger to hold job summary report.
     *
     * Large state reports made at infrequent intervals (e.g. job ending) go
     * here.
     */
    public transient Logger reports;

    protected StatisticsTracking statistics = null;

    /**
     * List of crawl status listeners.
     *
     * All iterations need to synchronize on this object if they're to avoid
     * concurrent modification exceptions.
     * See {@link java.util.Collections#synchronizedList(List)}.
     */
    private transient List<CrawlStatusListener> registeredCrawlStatusListeners = Collections.synchronizedList(new ArrayList<CrawlStatusListener>());

    private transient CrawlURIDispositionListener registeredCrawlURIDispositionListener;

    protected transient ArrayList<CrawlURIDispositionListener> registeredCrawlURIDispositionListeners;

    private transient EnhancedEnvironment bdbEnvironment = null;

    /**
     * Keep a list of all BigMap instance made -- shouldn't be many -- so that
     * we can checkpoint.
     */
    private transient Map<String, CachedBdbMap<?, ?>> bigmaps = null;

    /**
     * Default constructor
     */
    public CrawlController() {
        super();
    }

    /**
     * Starting from nothing, set up CrawlController and associated
     * classes to be ready for a first crawl.
     *
     * @param sH Settings handler.
     * @throws InitializationException
     */
    public void initialize(SettingsHandler sH) throws InitializationException {
        sendCrawlStateChangeEvent(PREPARING, CrawlJob.STATUS_PREPARING);
        this.singleThreadLock = new ReentrantLock();
        this.settingsHandler = sH;
        this.order = settingsHandler.getOrder();
        this.order.setController(this);
        this.bigmaps = new Hashtable<String, CachedBdbMap<?, ?>>();
        sExit = "";
        this.manifest = new StringBuffer();
        String onFailMessage = "";
        try {
            onFailMessage = "You must set the User-Agent and From HTTP" + " header values to acceptable strings. \n" + " User-Agent: [software-name](+[info-url])[misc]\n" + " From: [email-address]\n";
            order.checkUserAgentAndFrom();
            onFailMessage = "Unable to setup disk";
            if (disk == null) {
                setupDisk();
            }
            onFailMessage = "Unable to create log file(s)";
            setupLogs();
            onFailMessage = "Unable to test/run checkpoint recover";
            this.checkpointRecover = getCheckpointRecover();
            if (this.checkpointRecover == null) {
                this.checkpointer = new Checkpointer(this, this.checkpointsDisk);
            } else {
                setupCheckpointRecover();
            }
            onFailMessage = "Unable to setup bdb environment.";
            setupBdb();
            onFailMessage = "Unable to setup statistics";
            setupStatTracking();
            onFailMessage = "Unable to setup crawl modules";
            setupCrawlModules();
        } catch (Exception e) {
            String tmp = "On crawl: " + settingsHandler.getSettingsObject(null).getName() + " " + onFailMessage;
            LOGGER.log(Level.SEVERE, tmp, e);
            throw new InitializationException(tmp, e);
        }
        Lookup.getDefaultCache(DClass.IN).setMaxEntries(1);
        setupToePool();
        setThresholds();
        reserveMemory = new LinkedList<char[]>();
        for (int i = 1; i < RESERVE_BLOCKS; i++) {
            reserveMemory.add(new char[RESERVE_BLOCK_SIZE]);
        }
    }

    /**
     * Does setup of checkpoint recover.
     * Copies bdb log files into state dir.
     * @throws IOException
     */
    protected void setupCheckpointRecover() throws IOException {
        long started = System.currentTimeMillis();
        ;
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Starting recovery setup -- copying into place " + "bdbje log files -- for checkpoint named " + this.checkpointRecover.getDisplayName());
        }
        this.checkpointer.recover(this);
        this.progressStats.info("CHECKPOINT RECOVER " + this.checkpointRecover.getDisplayName());
        File bdbSubDir = CheckpointUtils.getBdbSubDirectory(this.checkpointRecover.getDirectory());
        FileUtils.copyFiles(bdbSubDir, CheckpointUtils.getJeLogsFilter(), getStateDisk(), true, false);
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Finished recovery setup for checkpoint named " + this.checkpointRecover.getDisplayName() + " in " + (System.currentTimeMillis() - started) + "ms.");
        }
    }

    protected boolean getCheckpointCopyBdbjeLogs() {
        return ((Boolean) this.order.getUncheckedAttribute(null, CrawlOrder.ATTR_CHECKPOINT_COPY_BDBJE_LOGS)).booleanValue();
    }

    private void setupBdb() throws FatalConfigurationException, AttributeNotFoundException {
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        int bdbCachePercent = ((Integer) this.order.getAttribute(null, CrawlOrder.ATTR_BDB_CACHE_PERCENT)).intValue();
        if (bdbCachePercent > 0) {
            envConfig.setCachePercent(bdbCachePercent);
        }
        envConfig.setLockTimeout(5000000);
        if (LOGGER.isLoggable(Level.FINEST)) {
            envConfig.setConfigParam("java.util.logging.level", "SEVERE");
            envConfig.setConfigParam("java.util.logging.level.evictor", "SEVERE");
            envConfig.setConfigParam("java.util.logging.ConsoleHandler.on", "true");
        }
        if (!getCheckpointCopyBdbjeLogs()) {
            envConfig.setConfigParam("je.cleaner.expunge", "false");
        }
        try {
            this.bdbEnvironment = new EnhancedEnvironment(getStateDisk(), envConfig);
            if (LOGGER.isLoggable(Level.FINE)) {
                envConfig = bdbEnvironment.getConfig();
                LOGGER.fine("BdbConfiguration: Cache percentage " + envConfig.getCachePercent() + ", cache size " + envConfig.getCacheSize());
            }
        } catch (DatabaseException e) {
            e.printStackTrace();
            throw new FatalConfigurationException(e.getMessage());
        }
    }

    /**
     * @return the shared EnhancedEnvironment
     */
    public EnhancedEnvironment getBdbEnvironment() {
        return this.bdbEnvironment;
    }

    /**
     * @deprecated use EnhancedEnvironment's getClassCatalog() instead
     */
    public StoredClassCatalog getClassCatalog() {
        return this.bdbEnvironment.getClassCatalog();
    }

    /**
     * Register for CrawlStatus events.
     *
     * @param cl a class implementing the CrawlStatusListener interface
     *
     * @see CrawlStatusListener
     */
    public void addCrawlStatusListener(CrawlStatusListener cl) {
        synchronized (this.registeredCrawlStatusListeners) {
            this.registeredCrawlStatusListeners.add(cl);
        }
    }

    /**
     * Register for CrawlURIDisposition events.
     *
     * @param cl a class implementing the CrawlURIDispostionListener interface
     *
     * @see CrawlURIDispositionListener
     */
    public void addCrawlURIDispositionListener(CrawlURIDispositionListener cl) {
        registeredCrawlURIDispositionListener = null;
        if (registeredCrawlURIDispositionListeners == null) {
            registeredCrawlURIDispositionListener = cl;
            registeredCrawlURIDispositionListeners = new ArrayList<CrawlURIDispositionListener>(1);
        }
        registeredCrawlURIDispositionListeners.add(cl);
    }

    /**
     * Allows an external class to raise a CrawlURIDispostion
     * crawledURISuccessful event that will be broadcast to all listeners that
     * have registered with the CrawlController.
     *
     * @param curi - The CrawlURI that will be sent with the event notification.
     *
     * @see CrawlURIDispositionListener#crawledURISuccessful(CrawlURI)
     */
    public void fireCrawledURISuccessfulEvent(CrawlURI curi) {
        if (registeredCrawlURIDispositionListener != null) {
            registeredCrawlURIDispositionListener.crawledURISuccessful(curi);
        } else {
            if (registeredCrawlURIDispositionListeners != null && registeredCrawlURIDispositionListeners.size() > 0) {
                Iterator it = registeredCrawlURIDispositionListeners.iterator();
                while (it.hasNext()) {
                    ((CrawlURIDispositionListener) it.next()).crawledURISuccessful(curi);
                }
            }
        }
    }

    /**
     * Allows an external class to raise a CrawlURIDispostion
     * crawledURINeedRetry event that will be broadcast to all listeners that
     * have registered with the CrawlController.
     *
     * @param curi - The CrawlURI that will be sent with the event notification.
     *
     * @see CrawlURIDispositionListener#crawledURINeedRetry(CrawlURI)
     */
    public void fireCrawledURINeedRetryEvent(CrawlURI curi) {
        if (registeredCrawlURIDispositionListener != null) {
            registeredCrawlURIDispositionListener.crawledURINeedRetry(curi);
            return;
        }
        if (registeredCrawlURIDispositionListeners != null && registeredCrawlURIDispositionListeners.size() > 0) {
            for (Iterator i = registeredCrawlURIDispositionListeners.iterator(); i.hasNext(); ) {
                ((CrawlURIDispositionListener) i.next()).crawledURINeedRetry(curi);
            }
        }
    }

    /**
     * Allows an external class to raise a CrawlURIDispostion
     * crawledURIDisregard event that will be broadcast to all listeners that
     * have registered with the CrawlController.
     * 
     * @param curi -
     *            The CrawlURI that will be sent with the event notification.
     * 
     * @see CrawlURIDispositionListener#crawledURIDisregard(CrawlURI)
     */
    public void fireCrawledURIDisregardEvent(CrawlURI curi) {
        if (registeredCrawlURIDispositionListener != null) {
            registeredCrawlURIDispositionListener.crawledURIDisregard(curi);
        } else {
            if (registeredCrawlURIDispositionListeners != null && registeredCrawlURIDispositionListeners.size() > 0) {
                Iterator it = registeredCrawlURIDispositionListeners.iterator();
                while (it.hasNext()) {
                    ((CrawlURIDispositionListener) it.next()).crawledURIDisregard(curi);
                }
            }
        }
    }

    /**
     * Allows an external class to raise a CrawlURIDispostion crawledURIFailure event
     * that will be broadcast to all listeners that have registered with the CrawlController.
     *
     * @param curi - The CrawlURI that will be sent with the event notification.
     *
     * @see CrawlURIDispositionListener#crawledURIFailure(CrawlURI)
     */
    public void fireCrawledURIFailureEvent(CrawlURI curi) {
        if (registeredCrawlURIDispositionListener != null) {
            registeredCrawlURIDispositionListener.crawledURIFailure(curi);
        } else {
            if (registeredCrawlURIDispositionListeners != null && registeredCrawlURIDispositionListeners.size() > 0) {
                Iterator it = registeredCrawlURIDispositionListeners.iterator();
                while (it.hasNext()) {
                    ((CrawlURIDispositionListener) it.next()).crawledURIFailure(curi);
                }
            }
        }
    }

    private void setupCrawlModules() throws FatalConfigurationException, AttributeNotFoundException, MBeanException, ReflectionException {
        if (scope == null) {
            scope = (CrawlScope) order.getAttribute(CrawlScope.ATTR_NAME);
            scope.initialize(this);
        }
        try {
            this.serverCache = new ServerCache(this);
        } catch (Exception e) {
            throw new FatalConfigurationException("Unable to" + " initialize frontier (Failed setup of ServerCache) " + e);
        }
        if (this.frontier == null) {
            this.frontier = (Frontier) order.getAttribute(Frontier.ATTR_NAME);
            try {
                frontier.initialize(this);
                frontier.pause();
                if (!isCheckpointRecover()) {
                    runFrontierRecover((String) order.getAttribute(CrawlOrder.ATTR_RECOVER_PATH));
                }
            } catch (IOException e) {
                throw new FatalConfigurationException("unable to initialize frontier: " + e);
            }
        }
        if (processorChains == null) {
            processorChains = new ProcessorChainList(order);
        }
    }

    protected void runFrontierRecover(String recoverPath) throws AttributeNotFoundException, MBeanException, ReflectionException, FatalConfigurationException {
        if (recoverPath == null || recoverPath.length() <= 0) {
            return;
        }
        File f = new File(recoverPath);
        if (!f.exists()) {
            LOGGER.severe("Recover file does not exist " + recoverPath);
            return;
        }
        if (!f.isFile()) {
            return;
        }
        boolean retainFailures = ((Boolean) order.getAttribute(CrawlOrder.ATTR_RECOVER_RETAIN_FAILURES)).booleanValue();
        try {
            frontier.importRecoverLog(recoverPath, retainFailures);
        } catch (IOException e) {
            e.printStackTrace();
            throw (FatalConfigurationException) new FatalConfigurationException("Recover.log " + recoverPath + " problem: " + e).initCause(e);
        }
    }

    private void setupDisk() throws AttributeNotFoundException {
        String diskPath = (String) order.getAttribute(null, CrawlOrder.ATTR_DISK_PATH);
        this.disk = getSettingsHandler().getPathRelativeToWorkingDirectory(diskPath);
        this.disk.mkdirs();
        this.logsDisk = getSettingsDir(CrawlOrder.ATTR_LOGS_PATH);
        this.checkpointsDisk = getSettingsDir(CrawlOrder.ATTR_CHECKPOINTS_PATH);
        this.stateDisk = getSettingsDir(CrawlOrder.ATTR_STATE_PATH);
        this.scratchDisk = getSettingsDir(CrawlOrder.ATTR_SCRATCH_PATH);
    }

    /**
     * @return The logging directory or null if problem reading the settings.
     */
    public File getLogsDir() {
        File f = null;
        try {
            f = getSettingsDir(CrawlOrder.ATTR_LOGS_PATH);
        } catch (AttributeNotFoundException e) {
            LOGGER.severe("Failed get of logs directory: " + e.getMessage());
        }
        return f;
    }

    /**
     * Return fullpath to the directory named by <code>key</code>
     * in settings.
     * If directory does not exist, it and all intermediary dirs
     * will be created.
     * @param key Key to use going to settings.
     * @return Full path to directory named by <code>key</code>.
     * @throws AttributeNotFoundException
     */
    public File getSettingsDir(String key) throws AttributeNotFoundException {
        String path = (String) order.getAttribute(null, key);
        File f = new File(path);
        if (!f.isAbsolute()) {
            f = new File(disk.getPath(), path);
        }
        if (!f.exists()) {
            f.mkdirs();
        }
        return f;
    }

    /**
     * Setup the statistics tracker.
     * The statistics object must be created before modules can use it.
     * Do it here now so that when modules retrieve the object from the
     * controller during initialization (which some do), its in place.
     * @throws InvalidAttributeValueException
     * @throws FatalConfigurationException
     */
    private void setupStatTracking() throws InvalidAttributeValueException, FatalConfigurationException {
        MapType loggers = order.getLoggers();
        final String cstName = "crawl-statistics";
        if (loggers.isEmpty(null)) {
            if (!isCheckpointRecover() && this.statistics == null) {
                this.statistics = new StatisticsTracker(cstName);
            }
            loggers.addElement(null, (StatisticsTracker) this.statistics);
        }
        if (isCheckpointRecover()) {
            restoreStatisticsTracker(loggers, cstName);
        }
        for (Iterator it = loggers.iterator(null); it.hasNext(); ) {
            StatisticsTracking tracker = (StatisticsTracking) it.next();
            tracker.initialize(this);
            if (this.statistics == null) {
                this.statistics = tracker;
            }
        }
    }

    protected void restoreStatisticsTracker(MapType loggers, String replaceName) throws FatalConfigurationException {
        try {
            loggers.removeElement(loggers.globalSettings(), replaceName);
            loggers.addElement(loggers.globalSettings(), (StatisticsTracker) this.statistics);
        } catch (Exception e) {
            throw convertToFatalConfigurationException(e);
        }
    }

    protected FatalConfigurationException convertToFatalConfigurationException(Exception e) {
        FatalConfigurationException fce = new FatalConfigurationException("Converted exception: " + e.getMessage());
        fce.setStackTrace(e.getStackTrace());
        return fce;
    }

    private void setupLogs() throws IOException {
        String logsPath = logsDisk.getAbsolutePath() + File.separatorChar;
        uriProcessing = Logger.getLogger(LOGNAME_CRAWL + "." + logsPath);
        runtimeErrors = Logger.getLogger(LOGNAME_RUNTIME_ERRORS + "." + logsPath);
        localErrors = Logger.getLogger(LOGNAME_LOCAL_ERRORS + "." + logsPath);
        uriErrors = Logger.getLogger(LOGNAME_URI_ERRORS + "." + logsPath);
        progressStats = Logger.getLogger(LOGNAME_PROGRESS_STATISTICS + "." + logsPath);
        this.fileHandlers = new HashMap<Logger, FileHandler>();
        setupLogFile(uriProcessing, logsPath + LOGNAME_CRAWL + CURRENT_LOG_SUFFIX, new UriProcessingFormatter(), true);
        setupLogFile(runtimeErrors, logsPath + LOGNAME_RUNTIME_ERRORS + CURRENT_LOG_SUFFIX, new RuntimeErrorFormatter(), true);
        setupLogFile(localErrors, logsPath + LOGNAME_LOCAL_ERRORS + CURRENT_LOG_SUFFIX, new LocalErrorFormatter(), true);
        setupLogFile(uriErrors, logsPath + LOGNAME_URI_ERRORS + CURRENT_LOG_SUFFIX, new UriErrorFormatter(), true);
        setupLogFile(progressStats, logsPath + LOGNAME_PROGRESS_STATISTICS + CURRENT_LOG_SUFFIX, new StatisticsLogFormatter(), true);
    }

    private void setupLogFile(Logger logger, String filename, Formatter f, boolean shouldManifest) throws IOException, SecurityException {
        GenerationFileHandler fh = new GenerationFileHandler(filename, true, shouldManifest);
        fh.setFormatter(f);
        logger.addHandler(fh);
        addToManifest(filename, MANIFEST_LOG_FILE, shouldManifest);
        logger.setUseParentHandlers(false);
        this.fileHandlers.put(logger, fh);
    }

    protected void rotateLogFiles(String generationSuffix) throws IOException {
        if (this.state != PAUSED && this.state != CHECKPOINTING) {
            throw new IllegalStateException("Pause crawl before requesting " + "log rotation.");
        }
        for (Iterator i = fileHandlers.keySet().iterator(); i.hasNext(); ) {
            Logger l = (Logger) i.next();
            GenerationFileHandler gfh = (GenerationFileHandler) fileHandlers.get(l);
            GenerationFileHandler newGfh = gfh.rotate(generationSuffix, CURRENT_LOG_SUFFIX);
            if (gfh.shouldManifest()) {
                addToManifest((String) newGfh.getFilenameSeries().get(1), MANIFEST_LOG_FILE, newGfh.shouldManifest());
            }
            l.removeHandler(gfh);
            l.addHandler(newGfh);
            fileHandlers.put(l, newGfh);
        }
    }

    /**
     * Close all log files and remove handlers from loggers.
     */
    public void closeLogFiles() {
        for (Iterator i = fileHandlers.keySet().iterator(); i.hasNext(); ) {
            Logger l = (Logger) i.next();
            GenerationFileHandler gfh = (GenerationFileHandler) fileHandlers.get(l);
            gfh.close();
            l.removeHandler(gfh);
        }
    }

    /**
     * Sets the values for max bytes, docs and time based on crawl order. 
     */
    private void setThresholds() {
        try {
            maxBytes = ((Long) order.getAttribute(CrawlOrder.ATTR_MAX_BYTES_DOWNLOAD)).longValue();
        } catch (Exception e) {
            maxBytes = 0;
        }
        try {
            maxDocument = ((Long) order.getAttribute(CrawlOrder.ATTR_MAX_DOCUMENT_DOWNLOAD)).longValue();
        } catch (Exception e) {
            maxDocument = 0;
        }
        try {
            maxTime = ((Long) order.getAttribute(CrawlOrder.ATTR_MAX_TIME_SEC)).longValue();
        } catch (Exception e) {
            maxTime = 0;
        }
    }

    /**
     * @return Object this controller is using to track crawl statistics
     */
    public StatisticsTracking getStatistics() {
        return statistics == null ? new StatisticsTracker("crawl-statistics") : this.statistics;
    }

    /**
     * Send crawl change event to all listeners.
     * @param newState State change we're to tell listeners' about.
     * @param message Message on state change.
     * @see #sendCheckpointEvent(File) for special case event sending
     * telling listeners to checkpoint.
     */
    protected void sendCrawlStateChangeEvent(Object newState, String message) {
        synchronized (this.registeredCrawlStatusListeners) {
            this.state = newState;
            for (Iterator i = this.registeredCrawlStatusListeners.iterator(); i.hasNext(); ) {
                CrawlStatusListener l = (CrawlStatusListener) i.next();
                if (newState.equals(PAUSED)) {
                    l.crawlPaused(message);
                } else if (newState.equals(RUNNING)) {
                    l.crawlResuming(message);
                } else if (newState.equals(PAUSING)) {
                    l.crawlPausing(message);
                } else if (newState.equals(STARTED)) {
                    l.crawlStarted(message);
                } else if (newState.equals(STOPPING)) {
                    l.crawlEnding(message);
                } else if (newState.equals(FINISHED)) {
                    l.crawlEnded(message);
                } else if (newState.equals(PREPARING)) {
                    l.crawlResuming(message);
                } else {
                    throw new RuntimeException("Unknown state: " + newState);
                }
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Sent " + newState + " to " + l);
                }
            }
            LOGGER.fine("Sent " + newState);
        }
    }

    /**
     * Send the checkpoint event.
     * Has its own method apart from
     * {@link #sendCrawlStateChangeEvent(Object, String)} because checkpointing
     * throws an Exception (Didn't want to have to wrap all of the
     * sendCrawlStateChangeEvent in try/catches).
     * @param checkpointDir Where to write checkpoint state to.
     * @throws Exception
     */
    protected void sendCheckpointEvent(File checkpointDir) throws Exception {
        synchronized (this.registeredCrawlStatusListeners) {
            if (this.state != PAUSED) {
                throw new IllegalStateException("Crawler must be completly " + "paused before checkpointing can start");
            }
            this.state = CHECKPOINTING;
            for (Iterator i = this.registeredCrawlStatusListeners.iterator(); i.hasNext(); ) {
                CrawlStatusListener l = (CrawlStatusListener) i.next();
                l.crawlCheckpoint(checkpointDir);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Sent " + CHECKPOINTING + " to " + l);
                }
            }
            LOGGER.fine("Sent " + CHECKPOINTING);
        }
    }

    /**
     * Operator requested crawl begin
     */
    public void requestCrawlStart() {
        runProcessorInitialTasks();
        sendCrawlStateChangeEvent(STARTED, CrawlJob.STATUS_PENDING);
        String jobState;
        state = RUNNING;
        jobState = CrawlJob.STATUS_RUNNING;
        sendCrawlStateChangeEvent(this.state, jobState);
        this.sExit = CrawlJob.STATUS_FINISHED_ABNORMAL;
        Thread statLogger = new Thread(statistics);
        statLogger.setName("StatLogger");
        statLogger.start();
        frontier.start();
    }

    /**
     * Called when the last toethread exits.
     */
    protected void completeStop() {
        LOGGER.fine("Entered complete stop.");
        runProcessorFinalTasks();
        sendCrawlStateChangeEvent(FINISHED, this.sExit);
        synchronized (this.registeredCrawlStatusListeners) {
            this.registeredCrawlStatusListeners.removeAll(this.registeredCrawlStatusListeners);
            this.registeredCrawlStatusListeners = null;
        }
        closeLogFiles();
        this.fileHandlers = null;
        this.uriErrors = null;
        this.uriProcessing = null;
        this.localErrors = null;
        this.runtimeErrors = null;
        this.progressStats = null;
        this.reports = null;
        this.manifest = null;
        this.statistics = null;
        this.frontier = null;
        this.disk = null;
        this.scratchDisk = null;
        this.order = null;
        this.scope = null;
        if (this.settingsHandler != null) {
            this.settingsHandler.cleanup();
        }
        this.settingsHandler = null;
        this.reserveMemory = null;
        this.processorChains = null;
        if (this.serverCache != null) {
            this.serverCache.cleanup();
            this.serverCache = null;
        }
        if (this.checkpointer != null) {
            this.checkpointer.cleanup();
            this.checkpointer = null;
        }
        if (this.bdbEnvironment != null) {
            try {
                this.bdbEnvironment.sync();
                this.bdbEnvironment.close();
            } catch (DatabaseException e) {
                e.printStackTrace();
            }
            this.bdbEnvironment = null;
        }
        this.bigmaps = null;
        if (this.toePool != null) {
            this.toePool.cleanup();
        }
        this.toePool = null;
        LOGGER.fine("Finished crawl.");
    }

    synchronized void completePause() {
        notifyAll();
        sendCrawlStateChangeEvent(PAUSED, CrawlJob.STATUS_PAUSED);
    }

    private boolean shouldContinueCrawling() {
        if (frontier.isEmpty()) {
            this.sExit = CrawlJob.STATUS_FINISHED;
            return false;
        }
        if (maxBytes > 0 && frontier.totalBytesWritten() >= maxBytes) {
            sExit = CrawlJob.STATUS_FINISHED_DATA_LIMIT;
            return false;
        } else if (maxDocument > 0 && frontier.succeededFetchCount() >= maxDocument) {
            this.sExit = CrawlJob.STATUS_FINISHED_DOCUMENT_LIMIT;
            return false;
        } else if (maxTime > 0 && statistics.crawlDuration() >= maxTime * 1000) {
            this.sExit = CrawlJob.STATUS_FINISHED_TIME_LIMIT;
            return false;
        }
        return state == RUNNING;
    }

    /**
     * Request a checkpoint.
     * Sets a checkpointing thread running.
     * @throws IllegalStateException Thrown if crawl is not in paused state
     * (Crawl must be first paused before checkpointing).
     */
    public synchronized void requestCrawlCheckpoint() throws IllegalStateException {
        if (this.checkpointer == null) {
            return;
        }
        if (this.checkpointer.isCheckpointing()) {
            throw new IllegalStateException("Checkpoint already running.");
        }
        this.checkpointer.checkpoint();
    }

    /**
     * @return True if checkpointing.
     */
    public boolean isCheckpointing() {
        return this.state == CHECKPOINTING;
    }

    /**
     * Run checkpointing.
     * CrawlController takes care of managing the checkpointing/serializing
     * of bdb, the StatisticsTracker, and the CheckpointContext.  Other
     * modules that want to revive themselves on checkpoint recovery need to
     * save state during their {@link CrawlStatusListener#crawlCheckpoint(File)}
     * invocation and then in their #initialize if a module,
     * or in their #initialTask if a processor, check with the CrawlController
     * if its checkpoint recovery. If it is, read in their old state from the
     * pointed to  checkpoint directory.
     * <p>Default access only to be called by Checkpointer.
     * @throws Exception
     */
    void checkpoint() throws Exception {
        sendCheckpointEvent(this.checkpointer.getCheckpointInProgressDirectory());
        LOGGER.fine("Rotating log files.");
        rotateLogFiles(CURRENT_LOG_SUFFIX + "." + this.checkpointer.getNextCheckpointName());
        LOGGER.fine("BigMaps.");
        checkpointBigMaps(this.checkpointer.getCheckpointInProgressDirectory());
        LOGGER.fine("Bdb environment.");
        checkpointBdb(this.checkpointer.getCheckpointInProgressDirectory());
        LOGGER.fine("Copying settings.");
        copySettings(this.checkpointer.getCheckpointInProgressDirectory());
        CheckpointUtils.writeObjectToFile(this, this.checkpointer.getCheckpointInProgressDirectory());
    }

    /**
     * Copy off the settings.
     * @param checkpointDir Directory to write checkpoint to.
     * @throws IOException 
     */
    protected void copySettings(final File checkpointDir) throws IOException {
        final List files = this.settingsHandler.getListOfAllFiles();
        boolean copiedSettingsDir = false;
        final File settingsDir = new File(this.disk, "settings");
        for (final Iterator i = files.iterator(); i.hasNext(); ) {
            File f = new File((String) i.next());
            if (f.getAbsolutePath().startsWith(settingsDir.getAbsolutePath())) {
                if (copiedSettingsDir) {
                    continue;
                }
                copiedSettingsDir = true;
                FileUtils.copyFiles(settingsDir, new File(checkpointDir, settingsDir.getName()));
                continue;
            }
            FileUtils.copyFiles(f, f.isDirectory() ? checkpointDir : new File(checkpointDir, f.getName()));
        }
    }

    /**
     * Checkpoint bdb.
     * I used do a call to log cleaning as suggested in je-2.0 javadoc but takes
     * way too much time (20minutes for a crawl of 1million items). Assume
     * cleaner is keeping up. Below was log cleaning loop .
     * <pre>int totalCleaned = 0;
     * for (int cleaned = 0; (cleaned = this.bdbEnvironment.cleanLog()) != 0;
     *  totalCleaned += cleaned) {
     *      LOGGER.fine("Cleaned " + cleaned + " log files.");
     * }
     * </pre>
     * <p>I also used to do a sync. But, from Mark Hayes, sync and checkpoint
     * are effectively same thing only sync is not configurable.  He suggests
     * doing one or the other:
     * <p>MS: Reading code, Environment.sync() is a checkpoint.  Looks like
     * I don't need to call a checkpoint after calling a sync?
     * <p>MH: Right, they're almost the same thing -- just do one or the other,
     * not both.  With the new API, you'll need to do a checkpoint not a
     * sync, because the sync() method has no config parameter.  Don't worry
     * -- it's fine to do a checkpoint even though you're not using.
     * @param checkpointDir Directory to write checkpoint to.
     * @throws DatabaseException 
     * @throws IOException 
     * @throws RuntimeException Thrown if failed setup of new bdb environment.
     */
    protected void checkpointBdb(File checkpointDir) throws DatabaseException, IOException, RuntimeException {
        EnvironmentConfig envConfig = this.bdbEnvironment.getConfig();
        final List bkgrdThreads = Arrays.asList(new String[] { "je.env.runCheckpointer", "je.env.runCleaner", "je.env.runINCompressor" });
        try {
            setBdbjeBkgrdThreads(envConfig, bkgrdThreads, "false");
            CheckpointConfig chkptConfig = new CheckpointConfig();
            chkptConfig.setForce(true);
            chkptConfig.setMinimizeRecoveryTime(true);
            this.bdbEnvironment.checkpoint(chkptConfig);
            LOGGER.fine("Finished bdb checkpoint.");
            EnvironmentImpl envImpl = DbInternal.envGetEnvironmentImpl(this.bdbEnvironment);
            long firstFileInNextSet = DbLsn.getFileNumber(envImpl.forceLogFileFlip());
            final String lastBdbCheckpointLog = getBdbLogFileName(firstFileInNextSet - 1);
            processBdbLogs(checkpointDir, lastBdbCheckpointLog);
            LOGGER.fine("Finished processing bdb log files.");
        } finally {
            setBdbjeBkgrdThreads(envConfig, bkgrdThreads, "true");
        }
    }

    protected void processBdbLogs(final File checkpointDir, final String lastBdbCheckpointLog) throws IOException {
        File bdbDir = CheckpointUtils.getBdbSubDirectory(checkpointDir);
        if (!bdbDir.exists()) {
            bdbDir.mkdir();
        }
        PrintWriter pw = new PrintWriter(new FileOutputStream(new File(checkpointDir, "bdbje-logs-manifest.txt")));
        try {
            boolean pastLastLogFile = false;
            Set<String> srcFilenames = null;
            final boolean copyFiles = getCheckpointCopyBdbjeLogs();
            do {
                FilenameFilter filter = CheckpointUtils.getJeLogsFilter();
                srcFilenames = new HashSet<String>(Arrays.asList(getStateDisk().list(filter)));
                List tgtFilenames = Arrays.asList(bdbDir.list(filter));
                if (tgtFilenames != null && tgtFilenames.size() > 0) {
                    srcFilenames.removeAll(tgtFilenames);
                }
                if (srcFilenames.size() > 0) {
                    srcFilenames = new TreeSet<String>(srcFilenames);
                    int count = 0;
                    for (final Iterator i = srcFilenames.iterator(); i.hasNext() && !pastLastLogFile; ) {
                        String name = (String) i.next();
                        if (copyFiles) {
                            FileUtils.copyFiles(new File(getStateDisk(), name), new File(bdbDir, name));
                        }
                        pw.println(name);
                        if (name.equals(lastBdbCheckpointLog)) {
                            pastLastLogFile = true;
                        }
                        count++;
                    }
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Copied " + count);
                    }
                }
            } while (!pastLastLogFile && srcFilenames != null && srcFilenames.size() > 0);
        } finally {
            pw.close();
        }
    }

    protected String getBdbLogFileName(final long index) {
        String lastBdbLogFileHex = Long.toHexString(index);
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < (8 - lastBdbLogFileHex.length()); i++) {
            buffer.append('0');
        }
        buffer.append(lastBdbLogFileHex);
        buffer.append(".jdb");
        return buffer.toString();
    }

    protected void setBdbjeBkgrdThreads(final EnvironmentConfig config, final List threads, final String setting) {
        for (final Iterator i = threads.iterator(); i.hasNext(); ) {
            config.setConfigParam((String) i.next(), setting);
        }
    }

    /**
     * Get recover checkpoint.
     * Returns null if we're NOT in recover mode.
     * Looks at ATTR_RECOVER_PATH and if its a directory, assumes checkpoint
     * recover. If checkpoint mode, returns Checkpoint instance if
     * checkpoint was VALID (else null).
     * @return Checkpoint instance if we're in recover checkpoint
     * mode and the pointed-to checkpoint was valid.
     * @see #isCheckpointRecover()
     */
    public synchronized Checkpoint getCheckpointRecover() {
        if (this.checkpointRecover != null) {
            return this.checkpointRecover;
        }
        return getCheckpointRecover(this.order);
    }

    public static Checkpoint getCheckpointRecover(final CrawlOrder order) {
        String path = (String) order.getUncheckedAttribute(null, CrawlOrder.ATTR_RECOVER_PATH);
        if (path == null || path.length() <= 0) {
            return null;
        }
        File rp = new File(path);
        Checkpoint result = null;
        if (rp.exists() && rp.isDirectory()) {
            Checkpoint cp = new Checkpoint(rp);
            if (cp.isValid()) {
                result = cp;
            }
        }
        return result;
    }

    public static boolean isCheckpointRecover(final CrawlOrder order) {
        return getCheckpointRecover(order) != null;
    }

    /**
     * @return True if we're in checkpoint recover mode. Call
     * {@link #getCheckpointRecover()} to get at Checkpoint instance
     * that has info on checkpoint directory being recovered from.
     */
    public boolean isCheckpointRecover() {
        return this.checkpointRecover != null;
    }

    /**
     * Operator requested for crawl to stop.
     */
    public synchronized void requestCrawlStop() {
        requestCrawlStop(CrawlJob.STATUS_ABORTED);
    }

    /**
     * Operator requested for crawl to stop.
     * @param message 
     */
    public synchronized void requestCrawlStop(String message) {
        if (state == STOPPING || state == FINISHED) {
            return;
        }
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null.");
        }
        this.sExit = message;
        beginCrawlStop();
    }

    /**
     * Start the process of stopping the crawl. 
     */
    public void beginCrawlStop() {
        LOGGER.fine("Started.");
        sendCrawlStateChangeEvent(STOPPING, this.sExit);
        if (this.frontier != null) {
            this.frontier.terminate();
            this.frontier.unpause();
        }
        LOGGER.fine("Finished.");
    }

    /**
     * Stop the crawl temporarly.
     */
    public synchronized void requestCrawlPause() {
        if (state == PAUSING || state == PAUSED) {
            return;
        }
        sExit = CrawlJob.STATUS_WAITING_FOR_PAUSE;
        frontier.pause();
        sendCrawlStateChangeEvent(PAUSING, this.sExit);
        if (toePool.getActiveToeCount() == 0) {
            completePause();
        }
    }

    /**
     * Tell if the controller is paused
     * @return true if paused
     */
    public boolean isPaused() {
        return state == PAUSED;
    }

    public boolean isPausing() {
        return state == PAUSING;
    }

    public boolean isRunning() {
        return state == RUNNING;
    }

    /**
     * Resume crawl from paused state
     */
    public synchronized void requestCrawlResume() {
        if (state != PAUSING && state != PAUSED && state != CHECKPOINTING) {
            return;
        }
        multiThreadMode();
        frontier.unpause();
        LOGGER.fine("Crawl resumed.");
        sendCrawlStateChangeEvent(RUNNING, CrawlJob.STATUS_RUNNING);
    }

    /**
     * @return Active toe thread count.
     */
    public int getActiveToeCount() {
        if (toePool == null) {
            return 0;
        }
        return toePool.getActiveToeCount();
    }

    private void setupToePool() {
        toePool = new ToePool(this);
        toePool.setSize(order.getMaxToes());
    }

    /**
     * @return The order file instance.
     */
    public CrawlOrder getOrder() {
        return order;
    }

    /**
     * @return The server cache instance.
     */
    public ServerCache getServerCache() {
        return serverCache;
    }

    /**
     * @param o
     */
    public void setOrder(CrawlOrder o) {
        order = o;
    }

    /**
     * @return The frontier.
     */
    public Frontier getFrontier() {
        return frontier;
    }

    /**
     * @return This crawl scope.
     */
    public CrawlScope getScope() {
        return scope;
    }

    /** Get the list of processor chains.
     *
     * @return the list of processor chains.
     */
    public ProcessorChainList getProcessorChainList() {
        return processorChains;
    }

    /** Get the first processor chain.
     *
     * @return the first processor chain.
     */
    public ProcessorChain getFirstProcessorChain() {
        return processorChains.getFirstChain();
    }

    /** Get the postprocessor chain.
     *
     * @return the postprocessor chain.
     */
    public ProcessorChain getPostprocessorChain() {
        return processorChains.getLastChain();
    }

    /**
     * Get the 'working' directory of the current crawl.
     * @return the 'working' directory of the current crawl.
     */
    public File getDisk() {
        return disk;
    }

    /**
     * @return Scratch disk location.
     */
    public File getScratchDisk() {
        return scratchDisk;
    }

    /**
     * @return State disk location.
     */
    public File getStateDisk() {
        return stateDisk;
    }

    /**
     * @return The number of ToeThreads
     *
     * @see ToePool#getToeCount()
     */
    public int getToeCount() {
        return this.toePool == null ? 0 : this.toePool.getToeCount();
    }

    /**
     * @return The ToePool
     */
    public ToePool getToePool() {
        return toePool;
    }

    /**
	 * @return toepool one-line report
	 */
    public String oneLineReportThreads() {
        return toePool.singleLineReport();
    }

    /**
     * While many settings will update automatically when the SettingsHandler is
     * modified, some settings need to be explicitly changed to reflect new
     * settings. This includes, number of toe threads and seeds.
     */
    public void kickUpdate() {
        toePool.setSize(order.getMaxToes());
        this.scope.kickUpdate();
        this.frontier.kickUpdate();
        this.processorChains.kickUpdate();
        setThresholds();
    }

    /**
     * @return The settings handler.
     */
    public SettingsHandler getSettingsHandler() {
        return settingsHandler;
    }

    /**
     * This method iterates through processor chains to run processors' initial
     * tasks.
     *
     */
    private void runProcessorInitialTasks() {
        for (Iterator ic = processorChains.iterator(); ic.hasNext(); ) {
            for (Iterator ip = ((ProcessorChain) ic.next()).iterator(); ip.hasNext(); ) {
                ((Processor) ip.next()).initialTasks();
            }
        }
    }

    /**
     * This method iterates through processor chains to run processors' final
     * tasks.
     *
     */
    private void runProcessorFinalTasks() {
        for (Iterator ic = processorChains.iterator(); ic.hasNext(); ) {
            for (Iterator ip = ((ProcessorChain) ic.next()).iterator(); ip.hasNext(); ) {
                ((Processor) ip.next()).finalTasks();
            }
        }
    }

    /**
     * Kills a thread. For details see
     * {@link org.archive.crawler.framework.ToePool#killThread(int, boolean)
     * ToePool.killThread(int, boolean)}.
     * @param threadNumber Thread to kill.
     * @param replace Should thread be replaced.
     * @see org.archive.crawler.framework.ToePool#killThread(int, boolean)
     */
    public void killThread(int threadNumber, boolean replace) {
        toePool.killThread(threadNumber, replace);
    }

    /**
     * Add a file to the manifest of files used/generated by the current
     * crawl.
     * 
     * TODO: Its possible for a file to be added twice if reports are
     * force generated midcrawl.  Fix.
     *
     * @param file The filename (with absolute path) of the file to add
     * @param type The type of the file
     * @param bundle Should the file be included in a typical bundling of
     *           crawler files.
     *
     * @see #MANIFEST_CONFIG_FILE
     * @see #MANIFEST_LOG_FILE
     * @see #MANIFEST_REPORT_FILE
     */
    public void addToManifest(String file, char type, boolean bundle) {
        manifest.append(type + (bundle ? "+" : "-") + " " + file + "\n");
    }

    /**
     * Evaluate if the crawl should stop because it is finished.
     */
    public void checkFinish() {
        if (atFinish()) {
            beginCrawlStop();
        }
    }

    /**
     * Evaluate if the crawl should stop because it is finished,
     * without actually stopping the crawl.
     * 
     * @return true if crawl is at a finish-possible state
     */
    public boolean atFinish() {
        return state == RUNNING && !shouldContinueCrawling();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.registeredCrawlStatusListeners = Collections.synchronizedList(new ArrayList<CrawlStatusListener>());
        singleThreadMode = false;
    }

    /**
     * Go to single thread mode, where only one ToeThread may
     * proceed at a time. Also acquires the single lock, so 
     * no further threads will proceed past an 
     * acquireContinuePermission. Caller mush be sure to release
     * lock to allow other threads to proceed one at a time. 
     */
    public void singleThreadMode() {
        this.singleThreadLock.lock();
        singleThreadMode = true;
    }

    /**
     * Go to back to regular multi thread mode, where all
     * ToeThreads may proceed at once
     */
    public void multiThreadMode() {
        this.singleThreadLock.lock();
        singleThreadMode = false;
        while (this.singleThreadLock.isHeldByCurrentThread()) {
            this.singleThreadLock.unlock();
        }
    }

    /**
     * Proceed only if allowed, giving CrawlController a chance
     * to enforce single-thread mode.
     */
    public void acquireContinuePermission() {
        if (singleThreadMode) {
            this.singleThreadLock.lock();
            if (!singleThreadMode) {
                while (this.singleThreadLock.isHeldByCurrentThread()) {
                    this.singleThreadLock.unlock();
                }
            }
        }
    }

    /**
     * Relinquish continue permission at end of processing (allowing
     * another thread to proceed if in single-thread mode). 
     */
    public void releaseContinuePermission() {
        if (singleThreadMode) {
            while (this.singleThreadLock.isHeldByCurrentThread()) {
                this.singleThreadLock.unlock();
            }
        }
    }

    public void freeReserveMemory() {
        if (!reserveMemory.isEmpty()) {
            reserveMemory.removeLast();
            System.gc();
        }
    }

    /**
     * Note that a ToeThread reached paused condition, possibly
     * completing the crawl-pause. 
     */
    public synchronized void toePaused() {
        releaseContinuePermission();
        if (state == PAUSING && toePool.getActiveToeCount() == 0) {
            completePause();
        }
    }

    /**
     * Note that a ToeThread ended, possibly completing the crawl-stop. 
     */
    public synchronized void toeEnded() {
        if (state == STOPPING && toePool.getActiveToeCount() == 0) {
            completeStop();
        }
    }

    /**
     * Add order file contents to manifest.
     * Write configuration files and any files managed by CrawlController to
     * it - files managed by other classes, excluding the settings framework,
     * are responsible for adding their files to the manifest themselves.
     * by calling addToManifest.
     * Call before writing out reports.
     */
    public void addOrderToManifest() {
        for (Iterator it = getSettingsHandler().getListOfAllFiles().iterator(); it.hasNext(); ) {
            addToManifest((String) it.next(), CrawlController.MANIFEST_CONFIG_FILE, true);
        }
    }

    /**
     * Log a URIException from deep inside other components to the crawl's
     * shared log. 
     * 
     * @param e URIException encountered
     * @param u CrawlURI where problem occurred
     * @param l String which could not be interpreted as URI without exception
     */
    public void logUriError(URIException e, UURI u, CharSequence l) {
        if (e.getReasonCode() == UURIFactory.IGNORED_SCHEME) {
            return;
        }
        Object[] array = { u, l };
        uriErrors.log(Level.INFO, e.getMessage(), array);
    }

    public static final String PROCESSORS_REPORT = "processors";

    public static final String MANIFEST_REPORT = "manifest";

    protected static final String[] REPORTS = { PROCESSORS_REPORT, MANIFEST_REPORT };

    public String[] getReports() {
        return REPORTS;
    }

    public void reportTo(PrintWriter writer) {
        reportTo(null, writer);
    }

    public String singleLineReport() {
        return ArchiveUtils.singleLineReport(this);
    }

    public void reportTo(String name, PrintWriter writer) {
        if (PROCESSORS_REPORT.equals(name)) {
            reportProcessorsTo(writer);
            return;
        } else if (MANIFEST_REPORT.equals(name)) {
            reportManifestTo(writer);
            return;
        } else if (name != null) {
            writer.println("requested report unknown: " + name);
        }
        singleLineReportTo(writer);
    }

    /**
     * @param writer Where to write report to.
     */
    protected void reportManifestTo(PrintWriter writer) {
        writer.print(manifest.toString());
    }

    /**
     * Compiles and returns a human readable report on the active processors.
     * @param writer Where to write to.
     * @see org.archive.crawler.framework.Processor#report()
     */
    protected void reportProcessorsTo(PrintWriter writer) {
        writer.print("Processors report - " + ArchiveUtils.get12DigitDate() + "\n");
        writer.print("  Job being crawled:    " + getOrder().getCrawlOrderName() + "\n");
        writer.print("  Number of Processors: " + processorChains.processorCount() + "\n");
        writer.print("  NOTE: Some processors may not return a report!\n\n");
        for (Iterator ic = processorChains.iterator(); ic.hasNext(); ) {
            for (Iterator ip = ((ProcessorChain) ic.next()).iterator(); ip.hasNext(); ) {
                writer.print(((Processor) ip.next()).report());
            }
        }
    }

    public void singleLineReportTo(PrintWriter writer) {
        writer.write("[Crawl Controller]\n");
    }

    public String singleLineLegend() {
        return "nothingYet";
    }

    /**
     * Call this method to get instance of the crawler BigMap implementation.
     * A "BigMap" is a Map that knows how to manage ever-growing sets of
     * key/value pairs. If we're in a checkpoint recovery, this method will
     * manage reinstantiation of checkpointed bigmaps.
     * @param dbName Name to give any associated database.  Also used
     * as part of name serializing out bigmap.  Needs to be unique to a crawl.
     * @param keyClass Class of keys we'll be using.
     * @param valueClass Class of values we'll be using.
     * @return Map that knows how to carry large sets of key/value pairs or
     * if none available, returns instance of HashMap.
     * @throws Exception
     */
    public <K, V> Map<K, V> getBigMap(final String dbName, final Class<? super K> keyClass, final Class<? super V> valueClass) throws Exception {
        CachedBdbMap<K, V> result = new CachedBdbMap<K, V>(dbName);
        if (isCheckpointRecover()) {
            File baseDir = getCheckpointRecover().getDirectory();
            @SuppressWarnings("unchecked") CachedBdbMap<K, V> temp = CheckpointUtils.readObjectFromFile(result.getClass(), dbName, baseDir);
            result = temp;
        }
        result.initialize(getBdbEnvironment(), keyClass, valueClass, getBdbEnvironment().getClassCatalog());
        this.bigmaps.put(dbName, result);
        return result;
    }

    protected void checkpointBigMaps(final File cpDir) throws Exception {
        for (final Iterator i = this.bigmaps.keySet().iterator(); i.hasNext(); ) {
            Object key = i.next();
            Object obj = this.bigmaps.get(key);
            ((CachedBdbMap) obj).sync();
            CheckpointUtils.writeObjectToFile(obj, (String) key, cpDir);
        }
    }

    /**
     * Called whenever progress statistics logging event.
     * @param e Progress statistics event.
     */
    public void progressStatisticsEvent(final EventObject e) {
    }

    /**
     * Log to the progress statistics log.
     * @param msg Message to write the progress statistics log.
     */
    public void logProgressStatistics(final String msg) {
        this.progressStats.info(msg);
    }

    /**
     * @return CrawlController state.
     */
    public Object getState() {
        return this.state;
    }

    public File getCheckpointsDisk() {
        return this.checkpointsDisk;
    }
}
