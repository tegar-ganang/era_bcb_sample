package org.tranche.flatfile;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.tranche.ConfigureTranche;
import org.tranche.TrancheServer;
import org.tranche.security.Signature;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.configuration.ConfigurationUtil;
import org.tranche.configuration.ServerModeFlag;
import org.tranche.exceptions.ChunkAlreadyExistsSecurityException;
import org.tranche.exceptions.ChunkDoesNotMatchHashException;
import org.tranche.exceptions.MetaDataIsCorruptedException;
import org.tranche.exceptions.ServerIsNotReadableException;
import org.tranche.exceptions.ServerIsNotWritableException;
import org.tranche.remote.Token;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.logs.LogUtil;
import org.tranche.logs.activity.Activity;
import org.tranche.logs.activity.ActivityLog;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataAnnotation;
import org.tranche.network.StatusTable;
import org.tranche.network.NetworkUtil;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.servers.ServerUtil;
import org.tranche.time.TimeUtil;
import org.tranche.users.User;
import org.tranche.FileEncoding;
import org.tranche.commons.TextUtil;
import org.tranche.exceptions.NoHostProvidedException;
import org.tranche.util.IOUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.util.AssertionUtil;
import org.tranche.util.TestUtil;
import org.tranche.commons.ThreadUtil;

/**
 * <p>An implementation of the TrancheServer interface that relies on the underlying operating system's filesystem.</p>
 * <p>This is the workhorse for the Tranche server, and it is very efficient in terms of speed and memory usage.</p>
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class FlatFileTrancheServer extends TrancheServer {

    private final File homeDirectory;

    private static String defaultHomeDirectory = null;

    private Configuration config;

    private DataBlockUtil dataBlockUtil;

    private HashSpanFixingThread hashSpanFixingThread;

    private TargetHashSpanThread targetHashSpanThread;

    private boolean closed = false;

    /**
     * <p>Flag if finished loading data blocks (a start-up activity).</p>
     */
    protected boolean doneLoadingDataBlocks = false;

    private long lastTimeConfigurationWasSet = -1;

    /**
     * <p>A synchronized collection of all assigned nonces.</p>
     */
    private NonceMap nonces;

    /**
     * --------------------------------------------------------------------------------
     * WHY KEEP TWO COLLECTIONS:
     *   Need order of list so can iterate using getProjectHashes, including offset 
     *   and limit. But also need logarithmic lookup.
     * 
     * DOES IT TAKE TOO MUCH MEMORY?:
     *   No. A hash is 76 bytes, and is an object in heap. Say storing n hashes.
     *   Keeping a list alone stores (76 + 8) * n bytes (since Java uses 64-bit memory
     *   references, or 8 bytes) since the list actually only stores a memory reference. 
     *   Adding the set only changes to (76 + 8 + 8) * n bytes. In other words, adds
     *   only 9.5% increase in memory reference.
     * 
     * SHOULD THIS BE STORED IN MEMORY? WHAT IF MANY PROJECTS...:
     *   Disk-backed could work, but would really slow down. 100MB of memory accomodates
     *   1,139,756 project chunks. In comparison, a 4TB server might average around 21
     *   million chunks. In other words, approximately one out of every ten meta data
     *   chunks would need to be a project chunk for this to happen--and generally, a
     *   4 TB server has more than enough memory to spare.
     * 
     *   At the moment, this shouldn't be an issue.
     * 
     * --------------------------------------------------------------------------------
     */
    private final List<BigHash> knownProjectsList = new ArrayList();

    private final Set<BigHash> knownProjectsSet = new HashSet();

    /**
     * <p>Servers will re-use cached configuration when getConfiguration called. This is the default time to cache.</p>
     * <p>The value used can be set in Configuration attributes.</p>
     */
    public static final int DEFAULT_GET_CONFIGURATION_CACHE_TIME = 1000 * 60 * 5;

    private long getNetworkStatusCount = 0;

    private final Object getNetworkStatusCountLock = new Object();

    private long getNonceCount = 0;

    private final Object getNonceCountLock = new Object();

    private long getDataHashesCount = 0;

    private final Object getDataHashesCountLock = new Object();

    private long getMetaDataHashesCount = 0;

    private final Object getMetaDataHashesCountLock = new Object();

    private long getHashesCount = 0;

    private final Object getHashesCountLock = new Object();

    private long getDataCount = 0;

    private final Object getDataCountLock = new Object();

    private long hasDataCount = 0;

    private final Object hasDataCountLock = new Object();

    private long hasMetaDataCount = 0;

    private final Object hasMetaDataCountLock = new Object();

    private long getMetaDataCount = 0;

    private final Object getMetaDataCountLock = new Object();

    private long setDataCount = 0;

    private final Object setDataCountLock = new Object();

    private long setMetaDataCount = 0;

    private final Object setMetaDataCountLock = new Object();

    private long deleteMetaDataCount = 0;

    private final Object deleteMetaDataCountLock = new Object();

    private long deleteDataCount = 0;

    private final Object deleteDataCountLock = new Object();

    private long getConfigurationCount = 0;

    private final Object getConfigurationCountLock = new Object();

    private long setConfigurationCount = 0;

    private final Object setConfigurationCountLock = new Object();

    private long getProjectHashesCount = 0;

    private final Object getProjectHashesCountLock = new Object();

    private final ActivityLog activityLog;

    private final Set<FlatFileTrancheServerListener> listeners = Collections.synchronizedSet(new HashSet<FlatFileTrancheServerListener>());

    private final Map<Collection<HashSpan>, String> hostsToUseWithOverlappingHashSpans = new HashMap<Collection<HashSpan>, String>();

    /**
     * <p>Used to perform actions on other servers.</p>
     */
    private X509Certificate authCert;

    private PrivateKey authPrivateKey;

    private String cachedHost = null;

    ;

    /**
     * <p>A simple method for determining the home directory where Tranche should both store files and
     * keep temporary files.</p>
     *
     * @return  the default home directory name
     *
     */
    public static String getDefaultHomeDir() {
        if (FlatFileTrancheServer.defaultHomeDirectory != null) {
            return FlatFileTrancheServer.defaultHomeDirectory;
        }
        String subdir1 = ".tranche";
        String subdir2 = "tranche";
        if (tryDirectory(subdir1) || tryDirectory(subdir2)) {
            return FlatFileTrancheServer.defaultHomeDirectory;
        }
        throw new RuntimeException("Can't create a temporary directory. Do you have write permissions on this computer?" + "\n" + "Can't create a persistent directory.");
    }

    /**
     * <p>Test a directory to see whether can read and write. Also, don't want multiple processes writing to same temporary subdirectory, so identify if available.</p>
     * @param subdirName
     * @return
     */
    public static boolean tryDirectory(String subdirName) {
        boolean permission = false;
        String possibleHomeDir = ConfigureTranche.get(ConfigureTranche.CATEGORY_SERVER, ConfigureTranche.PROP_SERVER_DIRECTORY);
        try {
            String userHome = System.getProperty("user.home");
            if (userHome != null) {
                possibleHomeDir = System.getProperty("user.home") + File.separator + subdirName;
            }
        } catch (Exception ex) {
            System.err.println("Exception occurred while checking system properties: " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
        File testDir = new File(possibleHomeDir, "test_permissions");
        File lockFile = null;
        RandomAccessFile lockRAF = null;
        FileChannel channel = null;
        FileLock lock = null;
        try {
            lockFile = new File(possibleHomeDir, "ffts-permissions-test.lock");
            if (!lockFile.exists()) {
                lockFile.getParentFile().mkdirs();
                lockFile.createNewFile();
            }
            lockRAF = new RandomAccessFile(lockFile, "rw");
            boolean obtainedLock = false;
            int attempt = 0;
            channel = lockRAF.getChannel();
            while (!obtainedLock && attempt < 50) {
                try {
                    lock = channel.lock();
                    obtainedLock = true;
                } catch (Exception ex) {
                    Thread.sleep(10);
                } finally {
                    attempt++;
                }
            }
            IOUtil.recursiveDelete(testDir);
            boolean success = testDir.mkdirs();
            permission = success && testDir.exists() && testDir.canRead() && testDir.canWrite();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            IOUtil.recursiveDelete(testDir);
            if (lock != null) {
                try {
                    lock.release();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            if (lockRAF != null) {
                try {
                    lockRAF.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            IOUtil.safeDelete(lockFile);
        }
        if (permission) {
            FlatFileTrancheServer.defaultHomeDirectory = possibleHomeDir;
            return true;
        } else {
            System.err.println("Could not create persistent directory at " + possibleHomeDir);
        }
        return false;
    }

    /**
     * @return  the home directory
     *
     */
    public File getHomeDirectory() {
        return homeDirectory;
    }

    /**
     * <p>Timestamp for last update of cached configuration.</p>
     */
    private long lastConfigurationTimestamp = -1;

    /**
     * <p>Get the Configuration object for this FlatFileTrancheServer.</p>
     * @return  the local configuration
     *
     */
    public Configuration getConfiguration() {
        return getConfiguration(true);
    }

    /**
     * <p>Get the Configuration object for this FlatFileTrancheServer.</p>
     * @param useCache If true, uses cache if time not elapsed to update; if false, updates unconditionally
     * @return the local configuration
     */
    protected Configuration getConfiguration(boolean useCache) {
        boolean isOkayToUseCache = (TimeUtil.getTrancheTimestamp() - lastConfigurationTimestamp < this.getConfigurationCacheTime());
        if (useCache && isOkayToUseCache) {
            return config;
        }
        synchronized (config) {
            try {
                List<String> keysToRemove = new ArrayList();
                Set<String> keys = new HashSet();
                keys.addAll(config.getValueKeys());
                for (String key : keys) {
                    if (key != null && (key.startsWith("actualBytesUsed:") || key.startsWith("actualBytesUsedOverflow:") || key.startsWith("actualPercentageUsed:"))) {
                        keysToRemove.add(key);
                    }
                }
                for (String key : keysToRemove) {
                    config.removeKeyValuePair(key);
                }
                long totalSize = 0;
                long totalSizeUsed = 0;
                boolean totalSizeOverflowed = false;
                boolean totalSizeUsedOverflowed = false;
                Set<DataDirectoryConfiguration> ddcs = new HashSet();
                ddcs.addAll(config.getDataDirectories());
                for (DataDirectoryConfiguration ddc : ddcs) {
                    config.setValue("actualBytesUsed:" + ddc.getDirectory(), Long.toString(ddc.getActualSize()));
                    config.setValue("actualBytesUsedOverflow:" + ddc.getDirectory(), String.valueOf(ddc.isOverflowBeenDetected()));
                    double percentage = 100.0 * (double) ddc.getActualSize() / (double) ddc.getSizeLimit();
                    String percentageStr = String.valueOf(percentage);
                    final int decimalPointIndex = percentageStr.indexOf(".");
                    String scientificStr = null;
                    final int scientificIndex = percentageStr.indexOf("E");
                    if (scientificIndex != -1) {
                        scientificStr = percentageStr.substring(scientificIndex);
                    }
                    if (decimalPointIndex != -1 && percentageStr.length() > decimalPointIndex + 1 + 2) {
                        String integerStr = percentageStr.substring(0, decimalPointIndex);
                        String floatStr = percentageStr.substring(decimalPointIndex + 1, decimalPointIndex + 1 + 2);
                        percentageStr = integerStr + "." + floatStr + (scientificStr != null ? scientificStr : "");
                    }
                    config.setValue("actualPercentageUsed:" + ddc.getDirectory(), percentageStr + "%");
                    long prevTotalSizeUsed = totalSizeUsed;
                    totalSizeUsed += ddc.getActualSize();
                    if (prevTotalSizeUsed > totalSizeUsed && ddc.getActualSize() >= 0) {
                        totalSizeUsedOverflowed = true;
                    }
                    long prevTotalSize = totalSize;
                    totalSize += ddc.getSizeLimit();
                    if (prevTotalSize > totalSize && ddc.getActualSize() >= 0) {
                        totalSizeOverflowed = true;
                    }
                }
                long totalSizeAvailable = totalSize - totalSizeUsed;
                boolean totalSizeAvailableOverflowed = totalSizeOverflowed || totalSizeUsedOverflowed;
                if (config.getName() == null) {
                    config.setValue(ConfigKeys.NAME, ServerUtil.getHostName());
                }
                if (config.getValue(ConfigKeys.TARGET_HASH_SPAN_THREAD_WORKING) == null) {
                    config.setValue(ConfigKeys.TARGET_HASH_SPAN_THREAD_WORKING, String.valueOf(false));
                }
                config.setValue(ConfigKeys.TOTAL_SIZE, TextUtil.formatBytes(totalSize) + (totalSizeOverflowed ? " (Value has overflowed: " + totalSize + " bytes)" : " (" + totalSize + " bytes)"));
                config.setValue(ConfigKeys.TOTAL_SIZE_UNUSED, TextUtil.formatBytes(totalSizeAvailable) + (totalSizeAvailableOverflowed ? " (Value calculated from overflowed values: " + totalSizeAvailable + " bytes)" : " (" + totalSizeAvailable + " bytes)"));
                config.setValue(ConfigKeys.TOTAL_SIZE_USED, TextUtil.formatBytes(totalSizeUsed) + (totalSizeUsedOverflowed ? " (Value has overflowed: " + totalSizeUsed + " bytes)" : " (" + totalSizeUsed + " bytes)"));
                Runtime r = Runtime.getRuntime();
                config.setValue(ConfigKeys.FREE_MEMORY, Long.toString(r.freeMemory()));
                config.setValue(ConfigKeys.TOTAL_MEMORY, Long.toString(r.totalMemory()));
                config.setValue(ConfigKeys.DATABLOCK_KNOWN_DATA, Long.toString(dataBlockUtil.dataHashes.size(true)));
                config.setValue(ConfigKeys.DATABLOCK_KNOWN_META, Long.toString(dataBlockUtil.metaDataHashes.size(true)));
                config.setValue(ConfigKeys.DATABLOCK_KNOWN_PROJECTS, Long.toString(this.knownProjectsList.size()));
                config.setValue(ConfigKeys.HASHSPANFIX_DATA_COPIED, Long.toString(this.hashSpanFixingThread.dataCopied));
                config.setValue(ConfigKeys.HASHSPANFIX_DATA_SKIPPED, Long.toString(this.hashSpanFixingThread.dataSkipped));
                config.setValue(ConfigKeys.HASHSPANFIX_DATA_DELETED, Long.toString(this.hashSpanFixingThread.dataDeleted));
                config.setValue(ConfigKeys.HASHSPANFIX_DATA_REPAIRED, Long.toString(this.hashSpanFixingThread.dataRepaired));
                config.setValue(ConfigKeys.HASHSPANFIX_DATA_NOT_REPAIRED, Long.toString(this.hashSpanFixingThread.dataNotRepaired));
                config.setValue(ConfigKeys.HASHSPANFIX_DATA_THREW_EXCEPTION, Long.toString(this.hashSpanFixingThread.dataLocalChunkThrewException));
                config.setValue(ConfigKeys.HASHSPANFIX_META_COPIED, Long.toString(this.hashSpanFixingThread.metaDataCopied));
                config.setValue(ConfigKeys.HASHSPANFIX_META_SKIPPED, Long.toString(this.hashSpanFixingThread.metaDataSkipped));
                config.setValue(ConfigKeys.HASHSPANFIX_META_DELETED, Long.toString(this.hashSpanFixingThread.metaDataDeleted));
                config.setValue(ConfigKeys.HASHSPANFIX_META_REPAIRED, Long.toString(this.hashSpanFixingThread.metaDataRepaired));
                config.setValue(ConfigKeys.HASHSPANFIX_META_NOT_REPAIRED, Long.toString(this.hashSpanFixingThread.metaDataNotRepaired));
                config.setValue(ConfigKeys.HASHSPANFIX_META_THREW_EXCEPTION, Long.toString(this.hashSpanFixingThread.metaLocalDataChunkThreadException));
                config.setValue(ConfigKeys.HASHSPANFIX_IT_COUNT, Long.toString(this.hashSpanFixingThread.iterationCount));
                config.setValue(ConfigKeys.HASHSPANFIX_SERVER_BEING_CHECKED, this.hashSpanFixingThread.serverBeingIndexed);
                config.setValue(ConfigKeys.HASHSPANFIX_REPAIR_ITS, Long.toString(this.hashSpanFixingThread.localRepairCompleteIterations));
                config.setValue(ConfigKeys.HASHSPANFIX_DELETE_ITS, Long.toString(this.hashSpanFixingThread.localDeleteCompleteIterations));
                config.setValue(ConfigKeys.HASHSPANFIX_PERCEIVED_DISK_USE_STATE, this.hashSpanFixingThread.getCurrentThreadState());
                config.setValue(ConfigKeys.HASHSPANFIX_PERFORMANCE_SCORE, String.valueOf(this.hashSpanFixingThread.getCurrentPerformanceScore()));
                config.setValue(ConfigKeys.DATABLOCK_MERGE_QUEUE_SIZE, Long.toString(dataBlockUtil.mergeQueue.size()));
                config.setValue(ConfigKeys.DATABLOCK_TOTAL_MERGED, Long.toString(dataBlockUtil.getTotalMergeDataBlock()));
                config.setValue(ConfigKeys.DATABLOCK_SUCCESS_MERGED, Long.toString(dataBlockUtil.getSuccessMergeDataBlock()));
                config.setValue(ConfigKeys.DATABLOCK_FAIL_MERGED, Long.toString(dataBlockUtil.getFailedMergeDataBlock()));
                config.setValue(ConfigKeys.BUILD_NUMBER, "@buildNumber");
                try {
                    byte flag = Byte.valueOf(config.getValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM));
                    config.setValue(ConfigKeys.SERVER_MODE_DESCRIPTION_SYSTEM, ServerModeFlag.toString(flag));
                } catch (Exception e) {
                }
                try {
                    byte flag = Byte.valueOf(config.getValue(ConfigKeys.SERVER_MODE_FLAG_ADMIN));
                    config.setValue(ConfigKeys.SERVER_MODE_DESCRIPTION_ADMIN, ServerModeFlag.toString(flag));
                } catch (Exception e) {
                }
                config.setValue(ConfigKeys.HASHSPANFIX_CURRENT_HASH_SPAN_FIXING_THREAD_ACTIVITY, this.hashSpanFixingThread.getCurrentActivity());
                config.setValue(ConfigKeys.HASHSPANFIX_TIME_SPENT_DELETING, TextUtil.formatTimeLength(this.hashSpanFixingThread.getTimeSpentDeleting()) + " (" + this.hashSpanFixingThread.getEstimatedPercentageTimeSpentDeleting() + ")");
                config.setValue(ConfigKeys.HASHSPANFIX_TIME_SPENT_DOING_NOTHING, TextUtil.formatTimeLength(this.hashSpanFixingThread.getTimeSpentDoingNothing()) + " (" + this.hashSpanFixingThread.getEstimatedPercentageTimeSpentDoingNothing() + ")");
                config.setValue(ConfigKeys.HASHSPANFIX_TIME_SPENT_DOWNLOADING, TextUtil.formatTimeLength(this.hashSpanFixingThread.getTimeSpentDownloading()) + " (" + this.hashSpanFixingThread.getEstimatedPercentageTimeSpentDownloading() + ")");
                config.setValue(ConfigKeys.HASHSPANFIX_TIME_SPENT_HEALING, TextUtil.formatTimeLength(this.hashSpanFixingThread.getTimeSpentHealing()) + " (" + this.hashSpanFixingThread.getEstimatedPercentageTimeSpentHealing() + ")");
                config.setValue(ConfigKeys.HASHSPANFIX_TIME_SPENT_BALANCING, TextUtil.formatTimeLength(this.hashSpanFixingThread.getTimeSpentBalancing()) + " (" + this.hashSpanFixingThread.getEstimatedPercentageTimeSpentBalancing() + ")");
                config.setValue(ConfigKeys.HASHSPANFIX_TOTAL_DATABLOCKS_MOVED_TO_BALANCE, String.valueOf(this.getDataBlockUtil().getTotalDataBlocksMovedWhileBalancing()));
                try {
                    config.setValue(ConfigKeys.HASHSPANFIX_INDEX_DELETE_DATA, String.valueOf(hashSpanFixingThread.getDeleteDataIndex()));
                } catch (Exception whoops) {
                    String msg = whoops.getClass().getSimpleName() + ": " + whoops.getMessage();
                    config.setValue(ConfigKeys.HASHSPANFIX_INDEX_DELETE_DATA, msg);
                }
                try {
                    config.setValue(ConfigKeys.HASHSPANFIX_INDEX_DELETE_META_DATA, String.valueOf(hashSpanFixingThread.getDeleteMetaDataIndex()));
                } catch (Exception whoops) {
                    String msg = whoops.getClass().getSimpleName() + ": " + whoops.getMessage();
                    config.setValue(ConfigKeys.HASHSPANFIX_INDEX_DELETE_META_DATA, msg);
                }
                try {
                    config.setValue(ConfigKeys.HASHSPANFIX_INDEX_DOWNLOAD_DATA, String.valueOf(hashSpanFixingThread.getDownloadDataIndex()));
                } catch (Exception whoops) {
                    String msg = whoops.getClass().getSimpleName() + ": " + whoops.getMessage();
                    config.setValue(ConfigKeys.HASHSPANFIX_INDEX_DOWNLOAD_DATA, msg);
                }
                try {
                    config.setValue(ConfigKeys.HASHSPANFIX_INDEX_DOWNLOAD_META_DATA, String.valueOf(hashSpanFixingThread.getDownloadMetaDataIndex()));
                } catch (Exception whoops) {
                    String msg = whoops.getClass().getSimpleName() + ": " + whoops.getMessage();
                    config.setValue(ConfigKeys.HASHSPANFIX_INDEX_DOWNLOAD_META_DATA, msg);
                }
                try {
                    config.setValue(ConfigKeys.HASHSPANFIX_INDEX_REPAIR_DATA, String.valueOf(hashSpanFixingThread.getRepairDataIndex()));
                } catch (Exception whoops) {
                    String msg = whoops.getClass().getSimpleName() + ": " + whoops.getMessage();
                    config.setValue(ConfigKeys.HASHSPANFIX_INDEX_REPAIR_DATA, msg);
                }
                try {
                    config.setValue(ConfigKeys.HASHSPANFIX_INDEX_REPAIR_META_DATA, String.valueOf(hashSpanFixingThread.getRepairMetaDataIndex()));
                } catch (Exception whoops) {
                    String msg = whoops.getClass().getSimpleName() + ": " + whoops.getMessage();
                    config.setValue(ConfigKeys.HASHSPANFIX_INDEX_REPAIR_META_DATA, msg);
                }
                config.setValue(ConfigKeys.CORRUPTED_DB_COUNT, String.valueOf(this.dataBlockUtil.getCorruptedDataBlockCount()));
                config.setValue(ConfigKeys.CORRUPTED_DB_DOWNLOADED_CHUNK_COUNT, String.valueOf(this.dataBlockUtil.getDownloadedChunksFromCorruptedDataBlockCount()));
                config.setValue(ConfigKeys.CORRUPTED_DB_IN_BODY_COUNT, String.valueOf(this.dataBlockUtil.getCorruptedDataBlockBodyCount()));
                config.setValue(ConfigKeys.CORRUPTED_DB_IN_HEADER_COUNT, String.valueOf(this.dataBlockUtil.getCorruptedDataBlockHeaderCount()));
                config.setValue(ConfigKeys.CORRUPTED_DB_LOST_CHUNK_COUNT, String.valueOf(this.dataBlockUtil.getLostChunksFromCorruptedDataBlockCount()));
                config.setValue(ConfigKeys.CORRUPTED_DB_SALVAGED_CHUNK_COUNT, String.valueOf(this.dataBlockUtil.getSalvagedChunksFromCorruptedDataBlockCount()));
                config.setValue(ConfigKeys.REQUESTS_COUNT_DELETE_DATA, String.valueOf(deleteDataCount));
                config.setValue(ConfigKeys.REQUESTS_COUNT_DELETE_META_DATA, String.valueOf(deleteMetaDataCount));
                config.setValue(ConfigKeys.REQUESTS_COUNT_GET_CONFIGURATION, String.valueOf(getConfigurationCount));
                config.setValue(ConfigKeys.REQUESTS_COUNT_GET_DATA, String.valueOf(getDataCount));
                config.setValue(ConfigKeys.REQUESTS_COUNT_GET_DATA_HASHES, String.valueOf(getDataHashesCount));
                config.setValue(ConfigKeys.REQUESTS_COUNT_GET_HASHES, String.valueOf(getHashesCount));
                config.setValue(ConfigKeys.REQUESTS_COUNT_GET_META_DATA, String.valueOf(getMetaDataCount));
                config.setValue(ConfigKeys.REQUESTS_COUNT_GET_META_DATA_HASHES, String.valueOf(getMetaDataHashesCount));
                config.setValue(ConfigKeys.REQUESTS_COUNT_GET_NONCE, String.valueOf(getNonceCount));
                config.setValue(ConfigKeys.REQUESTS_COUNT_GET_PROJECT_HASHES, String.valueOf(getProjectHashesCount));
                config.setValue(ConfigKeys.REQUESTS_COUNT_HAS_DATA, String.valueOf(hasDataCount));
                config.setValue(ConfigKeys.REQUESTS_COUNT_HAS_META_DATA, String.valueOf(hasMetaDataCount));
                config.setValue(ConfigKeys.REQUESTS_COUNT_SET_CONFIGURATION, String.valueOf(setConfigurationCount));
                config.setValue(ConfigKeys.REQUESTS_COUNT_SET_DATA, String.valueOf(setDataCount));
                config.setValue(ConfigKeys.REQUESTS_COUNT_SET_META_DATA, String.valueOf(setMetaDataCount));
            } finally {
                lastConfigurationTimestamp = TimeUtil.getTrancheTimestamp();
            }
        }
        return config;
    }

    /**
     * 
     * @param   dir     the directory received
     *
     */
    public FlatFileTrancheServer(File dir) {
        this(dir.getAbsolutePath());
    }

    /**
     * 
     * @param dir
     * @param log
     */
    public FlatFileTrancheServer(File dir, ActivityLog log) {
        this(dir.getAbsolutePath(), log);
    }

    /**
     * 
     * @param homeDirectoryName
     */
    public FlatFileTrancheServer(String homeDirectoryName) {
        this(homeDirectoryName, null, null);
    }

    public FlatFileTrancheServer(String homeDirectoryName, X509Certificate cert, PrivateKey key) {
        if (TestUtil.isTesting() && !TestUtil.isTestingActivityLogs()) {
            this.activityLog = null;
        } else {
            try {
                this.activityLog = new ActivityLog(new File(homeDirectoryName));
            } catch (Exception e) {
                System.err.println(e.getMessage() + " occurred while instantiating activity log: " + e.getMessage());
                e.printStackTrace(System.err);
                throw new RuntimeException(e);
            }
        }
        homeDirectory = new File(homeDirectoryName);
        homeDirectory.mkdirs();
        this.authCert = cert;
        this.authPrivateKey = key;
        init();
    }

    /**
     * 
     * @param   homeDirectoryName   the home directory name received
     */
    public FlatFileTrancheServer(String homeDirectoryName, ActivityLog log) {
        this(homeDirectoryName, log, null, null);
    }

    /**
     * 
     * @param homeDirectoryName
     * @param log
     * @param cert
     * @param key
     */
    public FlatFileTrancheServer(String homeDirectoryName, ActivityLog log, X509Certificate cert, PrivateKey key) {
        this.activityLog = log;
        homeDirectory = new File(homeDirectoryName);
        homeDirectory.mkdirs();
        this.authCert = cert;
        this.authPrivateKey = key;
        init();
    }

    /**
     * <p>Builds supporting objects and initializes configuration.</p>
     */
    private void init() {
        nonces = new NonceMap(FlatFileTrancheServer.this);
        this.dataBlockUtil = new DataBlockUtil(FlatFileTrancheServer.this);
        try {
            File configFile = new File(homeDirectory, "configuration");
            if (configFile.exists()) {
                byte[] configBytes = IOUtil.getBytes(configFile);
                config = ConfigurationUtil.read(new ByteArrayInputStream(configBytes));
            } else {
                config = new Configuration();
                config.addUser(SecurityUtil.getAdmin());
                config.addUser(SecurityUtil.getUser());
                config.addUser(SecurityUtil.getReadOnly());
                config.addUser(SecurityUtil.getWriteOnly());
                config.addUser(SecurityUtil.getAutoCert());
                File dataDirectory = new File(homeDirectory, "data");
                dataDirectory.mkdirs();
                config.addDataDirectory(new DataDirectoryConfiguration(dataDirectory.getAbsolutePath(), Long.MAX_VALUE));
            }
            if (!config.hasKey(ConfigKeys.DATABLOCK_LOG_DATA_CHUNK_DELETIONS)) {
                config.setValue(ConfigKeys.DATABLOCK_LOG_DATA_CHUNK_DELETIONS, String.valueOf(ConfigKeys.DEFAULT_LOG_DATA_CHUNK_DELETIONS));
            }
            if (!config.hasKey(ConfigKeys.DATABLOCK_LOG_META_DATA_CHUNK_DELETIONS)) {
                config.setValue(ConfigKeys.DATABLOCK_LOG_META_DATA_CHUNK_DELETIONS, String.valueOf(ConfigKeys.DEFAULT_LOG_META_DATA_CHUNK_DELETIONS));
            }
            synchronized (ConfigKeys.SERVER_MODE_FLAG_SYSTEM) {
                if (!config.hasKey(ConfigKeys.SERVER_MODE_FLAG_SYSTEM)) {
                    config.setValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM, String.valueOf(ServerModeFlag.CAN_READ_WRITE));
                }
            }
            if (!config.hasKey(ConfigKeys.SERVER_MODE_FLAG_ADMIN)) {
                config.setValue(ConfigKeys.SERVER_MODE_FLAG_ADMIN, String.valueOf(ServerModeFlag.CAN_READ_WRITE));
            }
            if (!config.hasKey(ConfigKeys.MIRROR_EVERY_UPLOAD)) {
                config.setValue(ConfigKeys.MIRROR_EVERY_UPLOAD, String.valueOf(false));
            }
            config.setValue(ConfigKeys.REMOTE_SERVER_CALLBACKS, "None");
            lastTimeConfigurationWasSet = TimeUtil.getTrancheTimestamp();
            if (this.projectFindingThread != null) {
                this.projectFindingThread.setStopped(true);
            }
            for (DataDirectoryConfiguration ddc : config.getDataDirectories()) {
                getDataBlockUtil().add(ddc, "init() was called from FlatFileTrancheServer. Just created server.");
            }
            hashSpanFixingThread = new HashSpanFixingThread(this);
            hashSpanFixingThread.setDaemon(true);
            hashSpanFixingThread.start();
            targetHashSpanThread = new TargetHashSpanThread(this);
            getTargetHashSpanThread().setDaemon(true);
            getTargetHashSpanThread().start();
            findAvailableData("FlatFileTrancheServer just instantiated: " + this.homeDirectory.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.err.flush();
            throw new RuntimeException("Can't load configuration file: " + ex.getMessage(), ex);
        }
    }

    /**
     * Helper method to load everything.
     *
     *
     */
    private ProjectFindingThread projectFindingThread = null;

    /**
     * <p>Blocking method to wait for project-finding thread to finish loading.</p>
     */
    public void waitToLoadExistingDataBlocks() {
        while (!doneLoadingDataBlocks && !closed) {
            ThreadUtil.sleep(500);
        }
    }

    /**
     * <p>Functionality to ping this server. Note this does nothing for the FlatFileTrancheServer.</p>
     * @throws java.lang.Exception
     */
    public void ping() throws Exception {
    }

    /**
     * <p>Returns the rows in the local network status that have host names that are between the startHost (inclusive) and the endHost (exclusive.)</p>
     * @param startHost
     * @param endHost
     * @return
     * @throws java.lang.Exception
     */
    public StatusTable getNetworkStatusPortion(String startHost, String endHost) throws Exception {
        synchronized (getNetworkStatusCountLock) {
            getNetworkStatusCount++;
        }
        return NetworkUtil.getStatus().getStatus(startHost, endHost);
    }

    /**
     * <p>Get a batch of hashes. Used to discover data chunks on server.</p>
     * @param   offset      the offset
     * @param   length      the length
     * @return              an array of data hashes
     * @throws  Exception   if any exception occurs
     *
     */
    public BigHash[] getDataHashes(BigInteger offset, BigInteger length) throws Exception {
        synchronized (getDataHashesCountLock) {
            getDataHashesCount++;
        }
        return getHashes(offset, length, false);
    }

    /**
     * <p>Get a batch of hashes. Used to discover meta data chunks on server.</p>
     * @param   offset      the offset
     * @param   length      the length
     * @return              an array of meta data hashes
     * @throws  Exception   if any exception occurs
     *
     */
    public BigHash[] getMetaDataHashes(BigInteger offset, BigInteger length) throws Exception {
        synchronized (getMetaDataHashesCountLock) {
            getMetaDataHashesCount++;
        }
        return getHashes(offset, length, true);
    }

    /**
     * <p>Get a batch of hashes. Used to discover data or meta data chunks on server.</p>
     * @param   offset      the offset
     * @param   length      the length
     * @param   isMetaData  the meta data flag
     * @return              an array of hashes
     * @throws  Exception   if any exception occurs
     *
     */
    private BigHash[] getHashes(BigInteger offset, BigInteger length, boolean isMetaData) throws Exception {
        synchronized (getHashesCountLock) {
            getHashesCount++;
        }
        if (offset.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0 || length.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
            throw new Exception("Server can currently only hold " + Long.MAX_VALUE + " entries.");
        }
        long o = offset.longValue();
        long l = length.longValue();
        if (isMetaData) {
            return getDataBlockUtil().getMetaDataHashes(o, l);
        } else {
            return getDataBlockUtil().getDataHashes(o, l);
        }
    }

    private static long findAvailableDataInvocationCount = 0;

    /**
     * <p>Restart the project-finding thread.</p>
     * @param reason A brief description of the reason the thread was restarted. Used to log restarts.
     */
    private void findAvailableData(String reason) {
        if (this.projectFindingThread != null) {
            this.projectFindingThread.setStopped(true);
        }
        try {
            Thread.sleep(500);
        } catch (Exception ex) {
        }
        findAvailableDataInvocationCount++;
        debugOut("Starting up project-finding thread at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()) + ". Total invocations: " + findAvailableDataInvocationCount + ". Reason: " + reason);
        this.projectFindingThread = new ProjectFindingThread(FlatFileTrancheServer.this);
        this.projectFindingThread.setDaemon(true);
        this.projectFindingThread.start();
    }

    /**
     * <p>Check to see whether server has the data chunks represented by the hashes.</p>
     * @param   hashes      the BigHash array of data chunks
     * @return              boolean array
     * @throws  Exception   if any exception occurs
     *
     */
    public boolean[] hasData(BigHash[] hashes) throws Exception {
        synchronized (hasDataCountLock) {
            hasDataCount++;
        }
        try {
            boolean[] hasList = new boolean[hashes.length];
            for (int i = 0; i < hashes.length; i++) {
                hasList[i] = getDataBlockUtil().hasData(hashes[i]);
            }
            return hasList;
        } catch (Exception e) {
            debugErr(e);
            throw e;
        }
    }

    /**
     * <p>Check to see whether server has the meta data chunks represented by the hashes.</p>
     * @param   hashes      the BigHash array of meta data
     * @return              boolean array
     * @throws  Exception   if any exception occurs
     */
    public boolean[] hasMetaData(BigHash[] hashes) throws Exception {
        synchronized (hasMetaDataCountLock) {
            hasMetaDataCount++;
        }
        try {
            boolean[] hasList = new boolean[hashes.length];
            for (int i = 0; i < hashes.length; i++) {
                hasList[i] = getDataBlockUtil().hasMetaData(hashes[i]);
            }
            return hasList;
        } catch (Exception e) {
            debugErr(e);
            throw e;
        }
    }

    /**
     * <p>Get a batch of data chunks.</p>
     * @param hashes
     * @param propagateRequest
     * @return
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper getData(BigHash[] hashes, boolean propagateRequest) throws Exception {
        synchronized (getDataCountLock) {
            getDataCount++;
        }
        byte[][] dataBytes = new byte[hashes.length][];
        Set<PropagationExceptionWrapper> exceptionSet = new HashSet<PropagationExceptionWrapper>();
        try {
            if (!canRead()) {
                throw new ServerIsNotReadableException();
            }
            for (int i = 0; i < hashes.length; i++) {
                try {
                    dataBytes[i] = getDataBlockUtil().getData(hashes[i]);
                } catch (Exception e) {
                    exceptionSet.add(new PropagationExceptionWrapper(e, getHost(), hashes[i]));
                }
            }
        } catch (Exception e) {
            exceptionSet.add(new PropagationExceptionWrapper(e, getHost()));
        }
        return new PropagationReturnWrapper(exceptionSet, dataBytes);
    }

    /**
     * <p>Get a batch of meta data chunks.</p>
     * @param hashes
     * @param propagateRequest
     * @return
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper getMetaData(BigHash[] hashes, boolean propagateRequest) throws Exception {
        synchronized (getMetaDataCountLock) {
            getMetaDataCount++;
        }
        byte[][] metaDataBytes = new byte[hashes.length][];
        HashSet<PropagationExceptionWrapper> exceptionSet = new HashSet<PropagationExceptionWrapper>();
        try {
            if (!canRead()) {
                throw new ServerIsNotReadableException();
            }
            for (int i = 0; i < hashes.length; i++) {
                try {
                    metaDataBytes[i] = getDataBlockUtil().getMetaData(hashes[i]);
                } catch (Exception e) {
                    exceptionSet.add(new PropagationExceptionWrapper(e, getHost(), hashes[i]));
                }
            }
        } catch (Exception e) {
            exceptionSet.add(new PropagationExceptionWrapper(e, getHost()));
        }
        return new PropagationReturnWrapper(exceptionSet, metaDataBytes);
    }

    /**
     * Helper method to verify signatures.
     * @param   bytes                       the bytes to update the signature
     * @param   sig                         the bytes to verify the signature
     * @param   key                         the signature key
     * @param   algorithm                   the signature algoritm
     * @return                              <code>true</code> if the signature is verified;
     *                                      <code>false</code> otherwise
     * @throws  GeneralSecurityException    if a general security exception occurs
     * @throws  IOException                 if an IO exception occurs
     */
    private boolean verifySignature(byte[] bytes, byte[] sig, PublicKey key, String algorithm) throws GeneralSecurityException, IOException {
        java.security.Signature signature = java.security.Signature.getInstance(algorithm);
        signature.initVerify(key);
        signature.update(bytes);
        return signature.verify(sig);
    }

    /**
     * Helper method to verify nonce.
     *
     * @param   nonce   a nonce byte
     * @return          <code>true</code> if the nonce byte is verified;
     *                  <code>false</code> otherwise
     *
     */
    private boolean verifyNonce(byte[] nonce) {
        if (!nonces.contains(nonce)) {
            return false;
        }
        nonces.remove(nonce);
        return true;
    }

    /**
     * Helper method to verify certificate is still valid.
     *
     * @param   cert    the certificate
     * @return          <code>true</code> if the certificate is valid;
     *                  <code>false</code> otherwise
     *
     */
    private boolean verifyCertificate(X509Certificate cert) {
        try {
            cert.checkValidity(new Date(TimeUtil.getTrancheTimestamp()));
        } catch (CertificateExpiredException e) {
            return false;
        } catch (CertificateNotYetValidException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * <p>Register a Tranche server.</p>
     * @param   url             the URL of the Tranche server (e.g. tranche://proteomecommons.org:443)
     * @throws  IOException     if an exception occurs
     */
    public void registerServer(String url) throws Exception {
    }

    /**
     * <p>Determine whether meta data specifies that this is sticky server.</p>
     * @param md
     * @return
     * @throws java.lang.Exception
     */
    protected boolean isStickyMetaDataForThisServer(MetaData md) throws Exception {
        MATCHES_STICKY_SERVER: for (String stickyServer : md.getStickyServers()) {
            if (stickyServer.equals(this.getHost())) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>Close off resources associated with this server.</p>
     *
     */
    public synchronized void close() {
        closed = true;
        this.dataBlockUtil.close();
        this.dataBlockUtil = null;
        if (this.getActivityLog() != null) {
            try {
                this.getActivityLog().close();
            } catch (Exception e) {
            }
        }
        if (this.hashSpanFixingThread != null) {
            this.hashSpanFixingThread.close();
        }
        if (this.targetHashSpanThread != null) {
            this.targetHashSpanThread.close();
        }
        notifyAll();
    }

    /**
     * <p>Serialize the Configurtation data and save to disk.</p>
     *
     */
    public void saveConfiguration() {
        try {
            File backupConfig = new File(homeDirectory, "configuration.backup");
            backupConfig.delete();
            File configFile = new File(homeDirectory, "configuration");
            configFile.renameTo(backupConfig);
            FileOutputStream fos = null;
            BufferedOutputStream bos = null;
            try {
                fos = new FileOutputStream(configFile);
                bos = new BufferedOutputStream(fos);
                ConfigurationUtil.write(config, bos);
            } catch (Exception e) {
                IOUtil.safeClose(bos);
                IOUtil.safeClose(fos);
                configFile.delete();
                backupConfig.renameTo(configFile);
            } finally {
                IOUtil.safeClose(bos);
                IOUtil.safeClose(fos);
            }
        } finally {
            lastTimeConfigurationWasSet = TimeUtil.getTrancheTimestamp();
        }
    }

    /**
     * <p>Get the DataBlockUtil associated with this FlatFileTrancheServer.</p>
     * @return  the data block utility
     *
     */
    public DataBlockUtil getDataBlockUtil() {
        return dataBlockUtil;
    }

    /**
     * <p>Get the Configuration object.</p>
     * @param   sig         the signature
     * @param   nonce       the nonce bytes
     * @return              the configuration
     * @throws  Exception   if any exception occurs
     *
     */
    public Configuration getConfiguration(Signature sig, byte[] nonce) throws Exception {
        synchronized (getConfigurationCountLock) {
            getConfigurationCount++;
        }
        boolean nonceValid = verifyNonce(nonce);
        if (!nonceValid) {
            throw new GeneralSecurityException(BAD_NONCE);
        }
        if (sig == null) {
            throw new Exception("Must pass in a valid signature.");
        }
        if (nonce == null) {
            throw new Exception("Must pass in a valid nonce.");
        }
        if (sig.getCert() == null) {
            throw new GeneralSecurityException("Signature's certificate is null.");
        }
        User user = config.getRecognizedUser(sig.getCert());
        if (user == null) {
            throw new GeneralSecurityException("User is not recognized by this server.");
        }
        if (!user.canGetConfiguration()) {
            throw new GeneralSecurityException("User can not read the server's configuration.");
        }
        boolean certIsValid = verifyCertificate(sig.getCert());
        if (!certIsValid) {
            throw new GeneralSecurityException("User certificate not valid after " + user.getCertificate().getNotAfter().toString());
        }
        boolean nonceVerify = verifySignature(nonce, sig.getBytes(), sig.getCert().getPublicKey(), sig.getAlgorithm());
        if (!nonceVerify) {
            throw new GeneralSecurityException(BAD_SIG);
        }
        return getConfiguration();
    }

    /**
     * <p>Ask whether allowed to shutdown. Does NOT shutdown the server. Used by Server to find out whether it should shut down resources or not.</p>
     * @param sig
     * @param nonce
     * @throws java.lang.Exception Any security issues.
     */
    public void requestShutdown(Signature sig, byte[] nonce) throws Exception {
        if (sig == null) {
            throw new Exception("Must pass in a valid signature.");
        }
        if (nonce == null) {
            throw new Exception("Must pass in a valid nonce.");
        }
        if (sig.getCert() == null) {
            throw new GeneralSecurityException("Signature's certificate is null.");
        }
        User user = config.getRecognizedUser(sig.getCert());
        if (user == null) {
            throw new GeneralSecurityException("User is not recognized by this server.");
        }
        if (!user.canSetConfiguration()) {
            throw new GeneralSecurityException("User can not read the server's configuration.");
        }
        boolean certIsValid = verifyCertificate(sig.getCert());
        if (!certIsValid) {
            throw new GeneralSecurityException("User certificate not valid after " + user.getCertificate().getNotAfter().toString());
        }
        boolean nonceValid = verifyNonce(nonce);
        if (!nonceValid) {
            throw new GeneralSecurityException(BAD_NONCE);
        }
        boolean nonceVerify = verifySignature(nonce, sig.getBytes(), sig.getCert().getPublicKey(), sig.getAlgorithm());
        if (!nonceVerify) {
            throw new GeneralSecurityException(BAD_SIG);
        }
        Thread t = new Thread("Shutdown Thread") {

            @Override
            public void run() {
                ThreadUtil.sleep(2000);
                close();
            }
        };
        t.setDaemon(true);
        t.start();
    }

    /**
     * <p>Set the Configuration for this FlatFileTrancheServer.</p>
     * @param c
     * @throws java.lang.Exception
     */
    public void setConfiguration(Configuration c) throws Exception {
        cachedHost = null;
        synchronized (config) {
            {
                Set<DataDirectoryConfiguration> newDataDirectories = c.getDataDirectories();
                Set<DataDirectoryConfiguration> existingDataDirectories = config.getDataDirectories();
                for (DataDirectoryConfiguration newDataDirectory : newDataDirectories) {
                    if (!existingDataDirectories.contains(newDataDirectory)) {
                        existingDataDirectories.add(newDataDirectory);
                    } else {
                        DataDirectoryConfiguration toUpdate = null;
                        for (Iterator<DataDirectoryConfiguration> it = existingDataDirectories.iterator(); it.hasNext(); ) {
                            DataDirectoryConfiguration test = it.next();
                            if (newDataDirectory.getDirectory().equals(test.getDirectory())) {
                                toUpdate = test;
                                toUpdate.setSizeLimit(newDataDirectory.getSizeLimit());
                                break;
                            }
                        }
                        if (toUpdate == null) {
                            throw new RuntimeException("Couldn't update data directory?");
                        }
                    }
                }
                for (Iterator<DataDirectoryConfiguration> it = existingDataDirectories.iterator(); it.hasNext(); ) {
                    DataDirectoryConfiguration existingDataDirectory = it.next();
                    if (!newDataDirectories.contains(existingDataDirectory)) {
                        it.remove();
                    }
                }
            }
            config.setHashSpans(new HashSet(c.getHashSpans()));
            config.setTargetHashSpans(new HashSet(c.getTargetHashSpans()));
            config.setUsers(new HashSet(c.getUsers()));
            config.setStickyProjects(new HashSet(c.getStickyProjects()));
            config.setServerConfigs(new HashSet(c.getServerConfigs()));
            {
                Set<String> newValueKeys = c.getValueKeys();
                Set<String> existingValueKeys = config.getValueKeys();
                List<String> keysToAdd = new ArrayList();
                for (String newValueKey : newValueKeys) {
                    try {
                        if (!existingValueKeys.contains(newValueKey) || !config.getValue(newValueKey).equals(c.getValue(newValueKey))) {
                            keysToAdd.add(newValueKey);
                        }
                    } catch (NullPointerException e) {
                        keysToAdd.add(newValueKey);
                    }
                }
                for (String keyToAdd : keysToAdd) {
                    String valueToAdd = c.getValue(keyToAdd);
                    if (isSavableAttributePair(keyToAdd, valueToAdd)) {
                        config.setValue(keyToAdd, valueToAdd);
                    }
                }
                List<String> keysToRemove = new ArrayList();
                for (String existingValueKey : existingValueKeys) {
                    if (!newValueKeys.contains(existingValueKey)) {
                        keysToRemove.add(existingValueKey);
                    }
                }
                for (String keyToRemove : keysToRemove) {
                    config.removeKeyValuePair(keyToRemove);
                }
            }
            saveConfiguration();
            if (NetworkUtil.getLocalServerRow() != null) {
                NetworkUtil.getLocalServerRow().update(config);
            }
            if (this.projectFindingThread != null) {
                this.projectFindingThread.setStopped(true);
            }
            debugOut("New configuration set.");
            for (DataDirectoryConfiguration ddc : c.getDataDirectories()) {
                this.dataBlockUtil.add(ddc, "A new configuration was set at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));
            }
            findAvailableData("Set Configurations");
            fireConfigurationSet(config);
        }
    }

    /**
     * <p>Set the Configuration for this FlatFileTrancheServer.</p>
     * @param   data        the data bytes
     * @param   sig         the signature
     * @param   nonce       the nonce bytes
     * @throws  Exception   if any exception occurs
     *
     */
    public void setConfiguration(byte[] data, Signature sig, byte[] nonce) throws Exception {
        synchronized (setConfigurationCountLock) {
            setConfigurationCount++;
        }
        debugOut("Setting configuration.");
        boolean nonceValid = verifyNonce(nonce);
        if (!nonceValid) {
            throw new GeneralSecurityException(BAD_NONCE);
        }
        if (sig == null) {
            throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid signature.");
        }
        if (nonce == null) {
            throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid nonce.");
        }
        if (data == null) {
            throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid nonce.");
        }
        User user = config.getRecognizedUser(sig.getCert());
        if (user == null) {
            throw new GeneralSecurityException(Token.SECURITY_ERROR + " User is not recognized by this server.");
        }
        if (!user.canSetConfiguration()) {
            throw new GeneralSecurityException(Token.SECURITY_ERROR + " User can not set the server's configuration.");
        }
        boolean certIsValid = verifyCertificate(sig.getCert());
        if (!certIsValid) {
            throw new GeneralSecurityException("User certificate not valid after " + user.getCertificate().getNotAfter().toString());
        }
        byte[] bytes = new byte[data.length + nonce.length];
        System.arraycopy(data, 0, bytes, 0, data.length);
        System.arraycopy(nonce, 0, bytes, data.length, nonce.length);
        boolean nonceVerify = verifySignature(bytes, sig.getBytes(), sig.getCert().getPublicKey(), sig.getAlgorithm());
        if (!nonceVerify) {
            throw new GeneralSecurityException(BAD_SIG);
        }
        setConfiguration(ConfigurationUtil.read(new ByteArrayInputStream(data)));
        this.findAvailableData("Configuration was remotely set.");
    }

    /**
     * <p>Check if the key-value pair should be saved along with new configuration.</p>
     * @param key
     * @param value
     * @return False if should not be saved, true otherwise.
     */
    private boolean isSavableAttributePair(String key, String value) {
        if (key.startsWith("actualBytesUsed:") || key.startsWith("acutalPercentageUsed:") || key.startsWith("actualBytesUsedOverflow:")) {
            return false;
        }
        if (!ConfigKeys.isEditable(key)) {
            return false;
        }
        return true;
    }

    /**
     * <p>Gets all of the hashes for projects that are in memory.</p>
     *
     * @param   offset      the offset
     * @param   length      the length
     * @return              an array of project hashes that are in memory
     * @throws  Exception   if any exception occurs
     *
     */
    public BigHash[] getProjectHashes(BigInteger offset, BigInteger length) throws Exception {
        synchronized (getProjectHashesCountLock) {
            getProjectHashesCount++;
        }
        synchronized (knownProjectsList) {
            if (knownProjectsList.size() < offset.longValue()) {
                return new BigHash[0];
            }
            long l = length.longValue();
            if (length.longValue() + offset.longValue() > knownProjectsList.size()) {
                l = knownProjectsList.size() - offset.longValue();
            }
            BigHash[] hashes = new BigHash[(int) l];
            for (int i = 0; i < l; i++) {
                hashes[i] = knownProjectsList.get(offset.intValue() + i);
            }
            return hashes;
        }
    }

    /**
     * <p>Return the healing thread associated with this FlatFileTrancheServer.</p>
     * @return
     */
    public HashSpanFixingThread getHashSpanFixingThread() {
        return hashSpanFixingThread;
    }

    /**
     * <p>Returns true if the server is closed down.</p>
     * @return
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * <p>Returns whether server mode flag is set to allow clients to read server's data.</p>
     * @return
     */
    public boolean canRead() {
        return getConfiguration().canRead();
    }

    /**
     * <p>Returns whether server mode flag is set to allow clients to write data to server.</p>
     * @return
     */
    public boolean canWrite() {
        return getConfiguration().canWrite();
    }

    /**
     * <p>Checks configuration to see whether this server is read-only.</p>
     * @return True if read-only, false otherwise. Default is false.
     * @deprecated There are different modes for server. Right now, there's none, read-only, write-only, and read/write.
     * @see #canRead() 
     * @see #canWrite() 
     */
    public boolean isReadOnly() throws Exception {
        return getConfiguration().canRead() && !getConfiguration().canWrite();
    }

    /**
     * <p>Timestamp of last change to Configuration.</p>
     * @return
     */
    public long getLastTimeConfigurationWasSet() {
        return lastTimeConfigurationWasSet;
    }

    /**
     * <p>Gets the time to internally cache the configuration. This is the time that must ellapse before the FlatFileTrancheServer will update the configuration that is returned by getConfiguration.</p>
     * <p>A lower time means up-to-date configurations, but substantially more work.</p>
     * @return
     */
    public int getConfigurationCacheTime() {
        try {
            return Integer.parseInt(config.getValue(ConfigKeys.GET_CONFIGURATION_CACHE_TIME));
        } catch (Exception ex) {
            config.setValue(ConfigKeys.GET_CONFIGURATION_CACHE_TIME, String.valueOf(DEFAULT_GET_CONFIGURATION_CACHE_TIME));
        }
        return FlatFileTrancheServer.DEFAULT_GET_CONFIGURATION_CACHE_TIME;
    }

    /**
     * <p>Gets the time to internally cache the configuration. This is the time that must ellapse before the FlatFileTrancheServer will update the configuration that is returned by getConfiguration.</p>
     * <p>A lower time means up-to-date configurations, but substantially more work.</p>
     * @param value
     */
    public void setConfigurationCacheTime(int value) {
        synchronized (this.config) {
            this.config.setValue(ConfigKeys.GET_CONFIGURATION_CACHE_TIME, String.valueOf(value));
        }
    }

    /**
     * <p>Returns the Tranche url (e.g., tranche://123.123.123.123:443), if set.</p>
     * @return The Tranche url, if set, or null otherwise
     */
    public String getTrancheUrl() {
        synchronized (this.config) {
            return this.config.getValue(ConfigKeys.URL);
        }
    }

    /**
     * <p>Returns the host name for this server. Prefers one set by Server, but if not found, finds by iterating the interfaces.</p>
     * @return
     */
    public String getHost() {
        if (cachedHost == null) {
            synchronized (this.config) {
                try {
                    cachedHost = this.config.getValue(ConfigKeys.SERVER_HOST_OVERRIDE);
                } catch (Exception e) {
                }
                if (cachedHost == null || cachedHost.trim().equals("")) {
                    try {
                        cachedHost = IOUtil.parseHost(this.config.getValue(ConfigKeys.URL));
                    } catch (Exception e) {
                    }
                }
                if (cachedHost == null || cachedHost.trim().equals("")) {
                    cachedHost = ServerUtil.getHostName();
                }
            }
        }
        return cachedHost;
    }

    /**
     * <p>Get all activity log entries from within a time period.</p>
     * @param startTimestamp Timestamp from which to include activities (inclusive)
     * @param finishTimestamp Timestamp to which to include activities (inclusive)
     * @param limit Limit number of activity entries to return
     * @param mask Used to match activity bytes. If all the bits in the mask are included in the activity byte, then the activity will be included. For example, if the mask is Activity.ACTION_TYPE_SET, then all logs that have this bit set (set data and set meta data) will be included. However, if Activity.SET_DATA is the mask, then only set data actions will match (since there are two bits set in the mask, making it more restrictive). 0 is least restrictive (restricts nothing) and 0xFF is most restrictive (restricts everything but itself).
     * @return
     */
    public Activity[] getActivityLogEntries(long startTimestamp, long finishTimestamp, int limit, byte mask) throws Exception {
        if (getActivityLog() != null) {
            return getActivityLog().read(startTimestamp, finishTimestamp, limit, mask).toArray(new Activity[0]);
        } else {
            throw new UnsupportedOperationException("Cannot get activity log entries in test scenario if no log was created. See TestUtil.isTestingActivityLogs() for more information.");
        }
    }

    /**
     * <p>Returns a count of all activity log entries from within a time period.</p>
     * @param startTimestamp Timestamp from which to include activities (inclusive)
     * @param stopTimestamp Timestamp to which to include activities (inclusive)
     * @param mask Used to match activity bytes. If all the bits in the mask are included in the activity byte, then the activity will be included. For example, if the mask is Activity.ACTION_TYPE_SET, then all logs that have this bit set (set data and set meta data) will be included. However, if Activity.SET_DATA is the mask, then only set data actions will match (since there are two bits set in the mask, making it more restrictive). 0 is least restrictive (restricts nothing) and 0xFF is most restrictive (restricts everything but itself).
     * @return
     */
    public int getActivityLogEntriesCount(long startTimestamp, long stopTimestamp, byte mask) throws Exception {
        if (getActivityLog() != null) {
            return getActivityLog().getActivityCount(startTimestamp, stopTimestamp, mask);
        } else {
            throw new UnsupportedOperationException("Cannot get activity log entries in test scenario if no log was created. See TestUtil.isTestingActivityLogs() for more information.");
        }
    }

    /**
     * <p>Returns the activity log.</p>
     * @return ActivityLog object, if set; otherwise, null.
     */
    public ActivityLog getActivityLog() {
        return activityLog;
    }

    /**
     * <p>Add a FlatFileTrancheServerListener.</p>
     * @param l
     * @return
     */
    public boolean addListener(FlatFileTrancheServerListener l) {
        return listeners.add(l);
    }

    /**
     * <p>Remove a FlatFileTrancheServerListener.</p>
     * @param l
     * @return
     */
    public boolean removeListener(FlatFileTrancheServerListener l) {
        return listeners.remove(l);
    }

    /**
     * <p>Clear all FlatFileTrancheServerListener objects.</p>
     */
    public void clearListeners() {
        listeners.clear();
    }

    /**
     * <p>Fired when data chunk is added.</p>
     * @param hash
     */
    public void fireDataChunkAdded(BigHash hash) {
        for (FlatFileTrancheServerListener l : listeners) {
            l.dataChunkAdded(hash);
        }
    }

    /**
     * <p>Fired when meta data chunk is added.</p>
     * @param hash
     */
    public void fireMetaDataChunkAdded(BigHash hash) {
        for (FlatFileTrancheServerListener l : listeners) {
            l.metaDataChunkAdded(hash);
        }
    }

    /**
     * <p>Fired when data chunk is deleted.</p>
     * @param hash
     */
    public void fireDataChunkDeleted(BigHash hash) {
        for (FlatFileTrancheServerListener l : listeners) {
            l.dataChunkDeleted(hash);
        }
    }

    /**
     * <p>Fired when meta data chunk is deleted.</p>
     * @param hash
     */
    public void fireMetaDataChunkDeleted(BigHash hash) {
        for (FlatFileTrancheServerListener l : listeners) {
            l.metaDataChunkDeleted(hash);
        }
    }

    /**
     * <p>Fired when configuration is set.</p>
     * @param config
     */
    public void fireConfigurationSet(Configuration config) {
        for (FlatFileTrancheServerListener l : this.listeners) {
            l.configurationSet(config);
        }
    }

    /**
     * <p>Tells the server to use the given server hosts for necessary communications with servers with overlapping hash spans.</p>
     * <p>The data structure is given the way it is to allow for ease-of-lookup.</p>
     * <p>The hosts to be used will be modified every time connections are adjusted.</p>
     * @param hostsToUseWithOverlappingHashSpans
     */
    public void setHostsToUseWithOverlappingHashSpans(Map<Collection<HashSpan>, String> hostsToUseWithOverlappingHashSpans) {
        synchronized (this.hostsToUseWithOverlappingHashSpans) {
            this.hostsToUseWithOverlappingHashSpans.clear();
            this.hostsToUseWithOverlappingHashSpans.putAll(hostsToUseWithOverlappingHashSpans);
        }
    }

    /**
     * 
     */
    protected void clearKnownProjects() {
        synchronized (knownProjectsList) {
            knownProjectsList.clear();
            knownProjectsSet.clear();
        }
    }

    /**
     * 
     * @param hash
     */
    protected void addKnownProject(BigHash hash) {
        synchronized (knownProjectsList) {
            if (!knownProjectsSet.contains(hash)) {
                knownProjectsSet.add(hash);
                knownProjectsList.add(hash);
            }
        }
    }

    /**
     * 
     * @param hash
     */
    protected void removeKnownProject(BigHash hash) {
        synchronized (knownProjectsList) {
            if (knownProjectsSet.contains(hash)) {
                knownProjectsSet.remove(hash);
                knownProjectsList.remove(hash);
            }
        }
    }

    /**
     * <p>Set data and replicate to specified hosts.</p>
     * @param hash
     * @param data
     * @param sig
     * @param hosts
     * @return
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper setData(BigHash hash, byte[] data, Signature sig, String[] hosts) throws Exception {
        if (hosts == null || hosts.length == 0) {
            throw new NoHostProvidedException();
        }
        AssertionUtil.assertNoNullValues(hash);
        AssertionUtil.assertNoNullValues(sig);
        AssertionUtil.assertNoNullValues(data);
        AssertionUtil.assertNoNullValues(hosts);
        AssertionUtil.assertNoDuplicateObjects(hosts);
        if (hosts.length != 1) {
            throw new RuntimeException("FlatFileTranche server propagation methods should only get request for one server (local host), instead: " + hosts.length);
        }
        if (!hosts[0].equals(getHost())) {
            throw new RuntimeException("FlatFileTranche server propagation method expects local host <" + this.getHost() + ">, but instead found: " + hosts[0]);
        }
        Set<PropagationExceptionWrapper> exceptions = new HashSet();
        try {
            synchronized (setDataCountLock) {
                setDataCount++;
            }
            if (!canWrite()) {
                throw new ServerIsNotWritableException();
            }
            boolean exists = false;
            try {
                if (dataBlockUtil.getData(hash) != null) {
                    exists = true;
                }
            } catch (Exception e) {
            }
            if (!exists) {
                User user = config.getRecognizedUser(sig.getCert());
                if (user == null) {
                    throw new GeneralSecurityException(Token.SECURITY_ERROR);
                }
                if (!user.canSetData()) {
                    throw new GeneralSecurityException(Token.SECURITY_ERROR);
                }
                if (!verifyCertificate(sig.getCert())) {
                    throw new GeneralSecurityException(Token.SECURITY_ERROR);
                }
                byte[] bytes = new byte[BigHash.HASH_LENGTH + data.length];
                byte[] hashBytes = hash.toByteArray();
                System.arraycopy(hashBytes, 0, bytes, 0, hashBytes.length);
                System.arraycopy(data, 0, bytes, hashBytes.length, data.length);
                if (!verifySignature(bytes, sig.getBytes(), sig.getCert().getPublicKey(), sig.getAlgorithm())) {
                    throw new GeneralSecurityException(BAD_SIG);
                }
                BigHash verifyHash = new BigHash(data);
                if (!verifyHash.equals(hash)) {
                    throw new ChunkDoesNotMatchHashException(hash, verifyHash);
                }
                if (getActivityLog() != null) {
                    getActivityLog().write(new Activity(Activity.SET_DATA, sig, hash));
                }
                getDataBlockUtil().addData(hash, data);
                fireDataChunkAdded(hash);
            }
        } catch (Exception e) {
            exceptions.add(new PropagationExceptionWrapper(e, getHost()));
        }
        return new PropagationReturnWrapper(exceptions);
    }

    /**
     * <p>Set meta data and replicate to specified hosts.</p>
     * @param merge
     * @param hash
     * @param metaData
     * @param sig
     * @param hosts
     * @return
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper setMetaData(boolean merge, BigHash hash, byte[] metaData, Signature sig, String[] hosts) throws Exception {
        if (hosts == null || hosts.length == 0) {
            throw new NoHostProvidedException();
        }
        AssertionUtil.assertNoNullValues(hash);
        AssertionUtil.assertNoNullValues(sig);
        AssertionUtil.assertNoNullValues(metaData);
        AssertionUtil.assertNoNullValues(hosts);
        AssertionUtil.assertNoDuplicateObjects(hosts);
        if (hosts.length != 1) {
            throw new RuntimeException("FlatFileTranche server propagation methods should only get request for one server (local host), instead: " + hosts.length);
        }
        if (!hosts[0].equals(getHost())) {
            throw new RuntimeException("FlatFileTranche server propagation method expects local host <" + this.getHost() + ">, but instead found: " + hosts[0]);
        }
        Set<PropagationExceptionWrapper> exceptions = new HashSet();
        try {
            synchronized (setMetaDataCountLock) {
                setMetaDataCount++;
            }
            if (!canWrite()) {
                throw new ServerIsNotWritableException();
            }
            User user = config.getRecognizedUser(sig.getCert());
            if (user == null) {
                throw new GeneralSecurityException(Token.SECURITY_ERROR);
            }
            if (!user.canSetMetaData()) {
                throw new GeneralSecurityException(Token.SECURITY_ERROR);
            }
            if (!verifyCertificate(sig.getCert())) {
                throw new GeneralSecurityException(Token.SECURITY_ERROR);
            }
            byte[] bytes = new byte[BigHash.HASH_LENGTH + metaData.length];
            byte[] hashBytes = hash.toByteArray();
            System.arraycopy(hashBytes, 0, bytes, 0, hashBytes.length);
            System.arraycopy(metaData, 0, bytes, hashBytes.length, metaData.length);
            if (!verifySignature(bytes, sig.getBytes(), sig.getCert().getPublicKey(), sig.getAlgorithm())) {
                throw new GeneralSecurityException(BAD_SIG);
            }
            MetaData existingMetaData = null;
            try {
                existingMetaData = MetaData.createFromBytes(dataBlockUtil.getMetaData(hash));
            } catch (Exception e) {
            }
            MetaData newMetaData = null;
            try {
                newMetaData = MetaData.createFromBytes(metaData);
            } catch (Exception e) {
                throw new MetaDataIsCorruptedException();
            }
            boolean writeAsIs = false;
            if (existingMetaData != null) {
                if (!merge) {
                    if (!user.canDeleteMetaData()) {
                        throw new ChunkAlreadyExistsSecurityException();
                    }
                    writeAsIs = true;
                } else {
                    MetaData mergedMetaData = existingMetaData.clone();
                    for (int i = 0; i < newMetaData.getUploaderCount(); i++) {
                        newMetaData.selectUploader(i);
                        if (!mergedMetaData.containsUploader(newMetaData.getSignature().getUserName(), newMetaData.getTimestampUploaded(), newMetaData.getRelativePathInDataSet())) {
                            mergedMetaData.addUploader(newMetaData.getSignature(), new ArrayList<FileEncoding>(newMetaData.getEncodings()), newMetaData.getProperties(), new ArrayList<MetaDataAnnotation>(newMetaData.getAnnotations()));
                            mergedMetaData.setParts(new ArrayList<BigHash>(newMetaData.getParts()));
                        }
                    }
                    byte[] mergedMetaDataBytes = mergedMetaData.toByteArray();
                    if (mergedMetaDataBytes.length > MetaData.SIZE_MAX) {
                        throw new Exception(Token.ERROR_META_DATA_TOO_BIG);
                    }
                    getDataBlockUtil().addMetaData(hash, mergedMetaDataBytes);
                }
            } else {
                writeAsIs = true;
            }
            if (writeAsIs) {
                if (metaData.length > MetaData.SIZE_MAX) {
                    throw new Exception(Token.ERROR_META_DATA_TOO_BIG);
                }
                getDataBlockUtil().addMetaData(hash, metaData);
            }
            if (getActivityLog() != null) {
                getActivityLog().write(new Activity(Activity.SET_META_DATA, sig, hash));
            }
            fireMetaDataChunkAdded(hash);
            if (existingMetaData == null) {
                try {
                    if (newMetaData.isProjectFile()) {
                        addKnownProject(hash);
                        if (isStickyMetaDataForThisServer(newMetaData)) {
                            getConfiguration().addStickyProject(hash);
                        }
                    }
                } catch (Exception e) {
                    LogUtil.logError(e);
                }
            }
        } catch (Exception e) {
            exceptions.add(new PropagationExceptionWrapper(e, getHost()));
        }
        return new PropagationReturnWrapper(exceptions);
    }

    /**
     * <p>Get batch of nonces from selected server hosts.</p>
     * @param hosts
     * @param count
     * @return
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper getNonces(String[] hosts, int count) throws Exception {
        if (hosts == null || hosts.length == 0) {
            throw new NoHostProvidedException();
        }
        AssertionUtil.assertNoNullValues(hosts);
        AssertionUtil.assertNoDuplicateObjects(hosts);
        if (hosts.length != 1) {
            throw new RuntimeException("FlatFileTranche server propagation methods should only get request for one server (local host), instead: " + hosts.length);
        }
        if (!hosts[0].equals(this.getHost())) {
            throw new RuntimeException("FlatFileTranche server propagation method expects local host <" + this.getHost() + ">, but instead found: " + hosts[0]);
        }
        Set<PropagationExceptionWrapper> exceptions = new HashSet();
        byte[][][] thisNonces = new byte[1][][];
        try {
            thisNonces[0] = new byte[count][NonceMap.NONCE_BYTES];
            for (int i = 0; i < count; i++) {
                thisNonces[0][i] = nonces.newNonce();
            }
        } catch (Exception e) {
            exceptions.add(new PropagationExceptionWrapper(e, this.getHost()));
        }
        return new PropagationReturnWrapper(exceptions, thisNonces);
    }

    /**
     * <p>Delete data chunk from selected servers.</p>
     * @param hash
     * @param sigs
     * @param nonces
     * @param hosts
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper deleteData(BigHash hash, Signature[] sigs, byte[][] nonces, String[] hosts) throws Exception {
        if (hosts == null || hosts.length == 0) {
            throw new NoHostProvidedException();
        }
        AssertionUtil.assertNoNullValues(hash);
        AssertionUtil.assertNoNullValues(sigs);
        AssertionUtil.assertNoNullValues(nonces);
        AssertionUtil.assertNoNullValues(hosts);
        AssertionUtil.assertNoDuplicateObjects(hosts);
        if (hosts.length != 1) {
            throw new RuntimeException("FlatFileTranche server propagation methods should only get request for one server (local host), instead: " + hosts.length);
        }
        if (!hosts[0].equals(this.getHost())) {
            throw new RuntimeException("FlatFileTranche server propagation method expects local host <" + this.getHost() + ">, but instead found: " + hosts[0]);
        }
        if (nonces.length != 1) {
            throw new RuntimeException("FlatFileTranche server propagation methods should only have one nonce, instead: " + nonces.length);
        }
        if (sigs.length != 1) {
            throw new RuntimeException("FlatFileTranche server propagation methods should only have one signature, instead: " + sigs.length);
        }
        Set<PropagationExceptionWrapper> exceptions = new HashSet();
        try {
            Signature sig = sigs[0];
            byte[] nonce = nonces[0];
            synchronized (deleteDataCountLock) {
                deleteDataCount++;
            }
            boolean nonceValid = verifyNonce(nonce);
            if (!nonceValid) {
                throw new GeneralSecurityException(BAD_NONCE);
            }
            if (sig == null) {
                throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid signature.");
            }
            if (hash == null) {
                throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid hash.");
            }
            if (nonce == null) {
                throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid nonce.");
            }
            User user = config.getRecognizedUser(sig.getCert());
            if (user == null) {
                throw new GeneralSecurityException(Token.SECURITY_ERROR + " User is not recognized by this server.");
            }
            if (!user.canDeleteData()) {
                throw new GeneralSecurityException(Token.SECURITY_ERROR + " User can not set data.");
            }
            boolean certIsValid = verifyCertificate(sig.getCert());
            if (!certIsValid) {
                throw new GeneralSecurityException("User certificate not valid after " + user.getCertificate().getNotAfter().toString());
            }
            byte[] hashBytes = hash.toByteArray();
            byte[] bytes = new byte[hashBytes.length + nonce.length];
            System.arraycopy(nonce, 0, bytes, 0, nonce.length);
            System.arraycopy(hashBytes, 0, bytes, nonce.length, hashBytes.length);
            if (!verifySignature(bytes, sig.getBytes(), sig.getCert().getPublicKey(), sig.getAlgorithm())) {
                throw new GeneralSecurityException(BAD_SIG);
            }
            if (getActivityLog() != null) {
                getActivityLog().write(new Activity(Activity.DELETE_DATA, sig, hash));
            }
            getDataBlockUtil().deleteData(hash, "ffts: deleteData");
            fireDataChunkDeleted(hash);
        } catch (Exception e) {
            exceptions.add(new PropagationExceptionWrapper(e, getHost()));
        }
        return new PropagationReturnWrapper(exceptions);
    }

    /**
     * 
     * @param hash
     * @param uploaderName
     * @param uploadTimestamp
     * @param relativePathInDataSet
     * @param sigs
     * @param nonces
     * @param hosts
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper deleteMetaData(BigHash hash, String uploaderName, Long uploadTimestamp, String relativePathInDataSet, Signature[] sigs, byte[][] nonces, String[] hosts) throws Exception {
        if (hosts == null || hosts.length == 0) {
            throw new NoHostProvidedException();
        }
        AssertionUtil.assertNoNullValues(hash);
        AssertionUtil.assertNoNullValues(sigs);
        AssertionUtil.assertNoNullValues(nonces);
        AssertionUtil.assertNoNullValues(hosts);
        AssertionUtil.assertNoDuplicateObjects(hosts);
        if (hosts.length != 1) {
            throw new RuntimeException("FlatFileTranche server propagation methods should only get request for one server (local host), instead: " + hosts.length);
        }
        if (!hosts[0].equals(this.getHost())) {
            throw new RuntimeException("FlatFileTranche server propagation method expects local host <" + this.getHost() + ">, but instead found: " + hosts[0]);
        }
        if (nonces.length != 1) {
            throw new RuntimeException("FlatFileTranche server propagation methods should only have one nonce, instead: " + nonces.length);
        }
        if (sigs.length != 1) {
            throw new RuntimeException("FlatFileTranche server propagation methods should only have one signature, instead: " + sigs.length);
        }
        Set<PropagationExceptionWrapper> exceptions = new HashSet();
        try {
            Signature sig = sigs[0];
            byte[] nonce = nonces[0];
            synchronized (deleteMetaDataCountLock) {
                deleteMetaDataCount++;
            }
            boolean nonceValid = verifyNonce(nonce);
            if (!nonceValid) {
                throw new GeneralSecurityException(BAD_NONCE);
            }
            if (sig == null) {
                throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid signature.");
            }
            if (hash == null) {
                throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid hash.");
            }
            if (nonce == null) {
                throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid nonce.");
            }
            User user = config.getRecognizedUser(sig.getCert());
            if (user == null) {
                throw new GeneralSecurityException(Token.SECURITY_ERROR + " User is not recognized by this server.");
            }
            if (!user.canDeleteMetaData()) {
                throw new GeneralSecurityException(Token.SECURITY_ERROR + " User can not delete meta-data.");
            }
            boolean certIsValid = verifyCertificate(sig.getCert());
            if (!certIsValid) {
                throw new GeneralSecurityException("User certificate valid time range: " + user.getCertificate().getNotBefore().toString() + " to " + user.getCertificate().getNotAfter().toString());
            }
            byte[] hashBytes = hash.toByteArray();
            byte[] bytes = new byte[hashBytes.length + nonce.length];
            System.arraycopy(nonce, 0, bytes, 0, nonce.length);
            System.arraycopy(hashBytes, 0, bytes, nonce.length, hashBytes.length);
            boolean nonceVerify = verifySignature(bytes, sig.getBytes(), sig.getCert().getPublicKey(), sig.getAlgorithm());
            if (!nonceVerify) {
                throw new GeneralSecurityException(BAD_SIG);
            }
            byte[] metaData = dataBlockUtil.getMetaData(hash);
            if (metaData == null) {
                throw new FileNotFoundException("Bytes were null. Should have thrown exception.");
            }
            MetaData existingMetaData = MetaData.createFromBytes(metaData);
            if (existingMetaData == null) {
                throw new Exception("Could not read meta data.");
            }
            boolean delete = true;
            if (uploaderName != null && uploadTimestamp != null) {
                existingMetaData.removeUploader(uploaderName, uploadTimestamp, relativePathInDataSet);
                if (existingMetaData.getUploaderCount() != 0) {
                    delete = false;
                    getDataBlockUtil().addMetaData(hash, existingMetaData.toByteArray());
                    if (getActivityLog() != null) {
                        getActivityLog().write(new Activity(Activity.REPLACE_META_DATA, sig, hash));
                    }
                    fireMetaDataChunkAdded(hash);
                }
            }
            if (delete) {
                if (existingMetaData.isProjectFile()) {
                    removeKnownProject(hash);
                    if (existingMetaData.getAllStickyServers().contains(getHost())) {
                        config.removeStickyProject(hash);
                    }
                }
                if (getActivityLog() != null) {
                    getActivityLog().write(new Activity(Activity.DELETE_META_DATA, sig, hash));
                }
                getDataBlockUtil().deleteMetaData(hash, "ffts: deleteMetaData");
                fireMetaDataChunkDeleted(hash);
            }
        } catch (Exception e) {
            exceptions.add(new PropagationExceptionWrapper(e, this.getHost()));
        }
        return new PropagationReturnWrapper(exceptions);
    }

    /**
     * <p>The thread that looks for target hash spans and injects/deletes chunks until can swap out current hash span(s) with target hash span.</p>
     * @return
     */
    public TargetHashSpanThread getTargetHashSpanThread() {
        return targetHashSpanThread;
    }

    /**
     * <p>By setting a certificate and private key to the flat file server that has write privileges on other servers, can inject data to network when needed.</p>
     * <p>It is the responsibility of the administrator starting the server to secure a signed key/certificate that can set chunks. Please contact the Tranche network administrators for more information.</p>
     * @return
     */
    public X509Certificate getAuthCert() {
        return authCert;
    }

    /**
     * <p>By setting a certificate and private key to the flat file server that has write privileges on other servers, can inject data to network when needed.</p>
     * <p>It is the responsibility of the administrator starting the server to secure a signed key/certificate that can set chunks. Please contact the Tranche network administrators for more information.</p>
     * @param authCert
     */
    public void setAuthCert(X509Certificate authCert) {
        this.authCert = authCert;
    }

    /**
     * <p>By setting a certificate and private key to the flat file server that has write privileges on other servers, can inject data to network when needed.</p>
     * <p>It is the responsibility of the administrator starting the server to secure a signed key/certificate that can set chunks. Please contact the Tranche network administrators for more information.</p>
     * @return
     */
    public PrivateKey getAuthPrivateKey() {
        return authPrivateKey;
    }

    /**
     * <p>By setting a certificate and private key to the flat file server that has write privileges on other servers, can inject data to network when needed.</p>
     * <p>It is the responsibility of the administrator starting the server to secure a signed key/certificate that can set chunks. Please contact the Tranche network administrators for more information.</p>
     * @param authPrivateKey
     */
    public void setAuthPrivateKey(PrivateKey authPrivateKey) {
        this.authPrivateKey = authPrivateKey;
    }
}
