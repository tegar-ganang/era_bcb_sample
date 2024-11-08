package com.sleepycat.je.cleaner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sleepycat.je.CacheMode;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.EnvironmentFailureException;
import com.sleepycat.je.EnvironmentMutableConfig;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.TransactionConfig;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.dbi.CursorImpl;
import com.sleepycat.je.dbi.DatabaseId;
import com.sleepycat.je.dbi.DatabaseImpl;
import com.sleepycat.je.dbi.DbConfigManager;
import com.sleepycat.je.dbi.DbTree;
import com.sleepycat.je.dbi.EnvConfigObserver;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.dbi.MemoryBudget;
import com.sleepycat.je.dbi.CursorImpl.SearchMode;
import com.sleepycat.je.log.LogEntryType;
import com.sleepycat.je.log.LogManager;
import com.sleepycat.je.log.ReplicationContext;
import com.sleepycat.je.log.entry.LNLogEntry;
import com.sleepycat.je.tree.BIN;
import com.sleepycat.je.tree.FileSummaryLN;
import com.sleepycat.je.tree.MapLN;
import com.sleepycat.je.tree.Tree;
import com.sleepycat.je.tree.TreeLocation;
import com.sleepycat.je.txn.BasicLocker;
import com.sleepycat.je.txn.LockType;
import com.sleepycat.je.txn.Locker;
import com.sleepycat.je.txn.Txn;
import com.sleepycat.je.utilint.DbLsn;
import com.sleepycat.je.utilint.LoggerUtils;

/**
 * The UP tracks utilization summary information for all log files.
 *
 * <p>Unlike the UtilizationTracker, the UP is not accessed under the log write
 * latch and is instead synchronized on itself for protecting the cache.  It is
 * not accessed during the primary data access path, except for when flushing
 * (writing) file summary LNs.  This occurs in the following cases:
 * <ol>
 * <li>The summary information is flushed at the end of a checkpoint.  This
 * allows tracking to occur in memory in between checkpoints, and replayed
 * during recovery.</li>
 * <li>When committing the truncateDatabase and removeDatabase operations, the
 * summary information is flushed because detail tracking for those operations
 * is not replayed during recovery</li>
 * <li>The evictor will ask the UtilizationTracker to flush the largest summary
 * if the memory taken by the tracker exeeds its budget.</li>
 * </ol>
 *
 * <p>The cache is populated by the RecoveryManager just before performing the
 * initial checkpoint.  The UP must be open and populated in order to respond
 * to requests to flush summaries and to evict tracked detail, even if the
 * cleaner is disabled.</p>
 *
 * <p>WARNING: While synchronized on this object, eviction is not permitted.
 * If it were, this could cause deadlocks because the order of locking would be
 * the UP object and then the evictor.  During normal eviction the order is to
 * first lock the evictor and then the UP, when evicting tracked detail.</p>
 *
 * <p>The methods in this class synchronize to protect the cached summary
 * information.  Some methods also access the UP database.  However, because
 * eviction must not occur while synchronized, UP database access is not
 * performed while synchronized except in one case: when inserting a new
 * summary record.  In that case we disallow eviction during the database
 * operation.</p>
 */
public class UtilizationProfile implements EnvConfigObserver {

    private final EnvironmentImpl env;

    private final UtilizationTracker tracker;

    private DatabaseImpl fileSummaryDb;

    private SortedMap<Long, FileSummary> fileSummaryMap;

    private boolean cachePopulated;

    private final boolean rmwFixEnabled;

    private final FilesToMigrate filesToMigrate;

    /**
     * Minimum overall utilization threshold that triggers cleaning.  Is
     * non-private for unit tests.
     */
    int minUtilization;

    /**
     * Minimum utilization threshold for an individual log file that triggers
     * cleaning.  Is non-private for unit tests.
     */
    int minFileUtilization;

    /**
     * Minumum age to qualify for cleaning.  If the first active LSN file is 5
     * and the mininum age is 2, file 4 won't qualify but file 3 will.  Must be
     * greater than zero because we never clean the first active LSN file.  Is
     * non-private for unit tests.
     */
    int minAge;

    private final Logger logger;

    /**
     * Creates an empty UP.
     */
    public UtilizationProfile(EnvironmentImpl env, UtilizationTracker tracker) {
        this.env = env;
        this.tracker = tracker;
        fileSummaryMap = new TreeMap<Long, FileSummary>();
        filesToMigrate = new FilesToMigrate();
        rmwFixEnabled = env.getConfigManager().getBoolean(EnvironmentParams.CLEANER_RMW_FIX);
        logger = LoggerUtils.getLogger(getClass());
        envConfigUpdate(env.getConfigManager(), null);
        env.addConfigObserver(this);
    }

    /**
     * Process notifications of mutable property changes.
     */
    public void envConfigUpdate(DbConfigManager cm, EnvironmentMutableConfig ignore) {
        minAge = cm.getInt(EnvironmentParams.CLEANER_MIN_AGE);
        minUtilization = cm.getInt(EnvironmentParams.CLEANER_MIN_UTILIZATION);
        minFileUtilization = cm.getInt(EnvironmentParams.CLEANER_MIN_FILE_UTILIZATION);
    }

    /**
     * @see EnvironmentParams#CLEANER_RMW_FIX
     * @see FileSummaryLN#postFetchInit
     */
    public boolean isRMWFixEnabled() {
        return rmwFixEnabled;
    }

    /**
     * Returns the number of files in the profile.
     */
    synchronized int getNumberOfFiles() {
        return fileSummaryMap.size();
    }

    /**
     * Returns an approximation of the total log size.  Used for stats.
     */
    long getTotalLogSize() {
        long size = 0;
        synchronized (this) {
            for (FileSummary summary : fileSummaryMap.values()) {
                size += summary.totalSize;
            }
        }
        for (TrackedFileSummary summary : tracker.getTrackedFiles()) {
            size += summary.totalSize;
        }
        return size;
    }

    /**
     * Returns the cheapest file to clean from the given list of files.  This
     * method is used to select the first file to be cleaned in the batch of
     * to-be-cleaned files.
     */
    synchronized Long getCheapestFileToClean(SortedSet<Long> files) {
        if (files.size() == 1) {
            return files.first();
        }
        assert cachePopulated;
        Long bestFile = null;
        int bestCost = Integer.MAX_VALUE;
        final SortedMap<Long, FileSummary> currentFileSummaryMap = getFileSummaryMap(true);
        for (Iterator<Long> iter = files.iterator(); iter.hasNext(); ) {
            Long file = iter.next();
            FileSummary summary = currentFileSummaryMap.get(file);
            if (summary == null) {
                return file;
            }
            int thisCost = summary.getNonObsoleteCount();
            if (bestFile == null || thisCost < bestCost) {
                bestFile = file;
                bestCost = thisCost;
            }
        }
        return bestFile;
    }

    /**
     * Returns the best file that qualifies for cleaning, or null if no file
     * qualifies.
     *
     * @param fileSelector is used to determine valid cleaning candidates.
     *
     * @param forceCleaning is true to always select a file, even if its
     * utilization is above the minimum utilization threshold.
     *
     * @param lowUtilizationFiles is a returned set of files that are below the
     * minimum utilization threshold.
     */
    Long getBestFileForCleaning(FileSelector fileSelector, boolean forceCleaning, Set<Long> lowUtilizationFiles, boolean isBacklog) throws DatabaseException {
        long minProtectedFile = env.getCleanerBarrierStartFile();
        if (minProtectedFile == -1) {
            return null;
        }
        synchronized (this) {
            if (lowUtilizationFiles != null) {
                lowUtilizationFiles.clear();
            }
            assert cachePopulated;
            SortedMap<Long, FileSummary> currentFileSummaryMap = getFileSummaryMap(true);
            if (currentFileSummaryMap.size() == 0) {
                return null;
            }
            final int useMinUtilization = minUtilization;
            final int useMinFileUtilization = minFileUtilization;
            final int useMinAge = minAge;
            long firstActiveFile = currentFileSummaryMap.lastKey().longValue();
            long firstActiveTxnLsn = env.getTxnManager().getFirstActiveLsn();
            if (firstActiveTxnLsn != DbLsn.NULL_LSN) {
                long firstActiveTxnFile = DbLsn.getFileNumber(firstActiveTxnLsn);
                if (firstActiveFile > firstActiveTxnFile) {
                    firstActiveFile = firstActiveTxnFile;
                }
            }
            long lastFileToClean = firstActiveFile - useMinAge;
            Iterator<Map.Entry<Long, FileSummary>> iter = currentFileSummaryMap.entrySet().iterator();
            Long bestFile = null;
            int bestUtilization = 101;
            long totalSize = 0;
            long totalObsoleteSize = 0;
            while (iter.hasNext()) {
                Map.Entry<Long, FileSummary> entry = iter.next();
                Long file = entry.getKey();
                long fileNum = file.longValue();
                if (fileNum >= minProtectedFile) {
                    continue;
                }
                FileSummary summary = entry.getValue();
                int obsoleteSize = summary.getObsoleteSize();
                if (fileSelector.isFileCleaningInProgress(file)) {
                    totalSize += summary.totalSize - obsoleteSize;
                    totalObsoleteSize += estimateUPObsoleteSize(summary);
                    continue;
                }
                totalSize += summary.totalSize;
                totalObsoleteSize += obsoleteSize;
                if (fileNum > lastFileToClean) {
                    continue;
                }
                int thisUtilization = utilization(obsoleteSize, summary.totalSize);
                if (bestFile == null || thisUtilization < bestUtilization) {
                    bestFile = file;
                    bestUtilization = thisUtilization;
                }
                if (lowUtilizationFiles != null && thisUtilization < useMinUtilization) {
                    lowUtilizationFiles.add(file);
                }
            }
            int totalUtilization = utilization(totalObsoleteSize, totalSize);
            if (totalUtilization < useMinUtilization || bestUtilization < useMinFileUtilization) {
                return bestFile;
            } else if (!isBacklog && filesToMigrate.hasNext(currentFileSummaryMap)) {
                return filesToMigrate.next(currentFileSummaryMap);
            } else if (forceCleaning) {
                return bestFile;
            } else {
                return null;
            }
        }
    }

    /**
     * Calculate the utilization percentage.
     */
    public static int utilization(long obsoleteSize, long totalSize) {
        if (totalSize != 0) {
            return (int) (((totalSize - obsoleteSize) * 100) / totalSize);
        } else {
            return 0;
        }
    }

    /**
     * Estimate the log size that will be made obsolete when a log file is
     * deleted and we delete its UP records.
     *
     * Note that we do not count the space taken by the deleted FileSummaryLN
     * records written during log file deletion.  These add the same amount to
     * the total log size and the obsolete log size, and therefore have a small
     * impact on total utilization.
     */
    private int estimateUPObsoleteSize(FileSummary summary) {
        if (true) {
            return 0;
        }
        final int OVERHEAD = 75;
        int OFFSETS_PER_LN = 1000;
        int BYTES_PER_LN = OVERHEAD + (OFFSETS_PER_LN * 2);
        int totalNodes = summary.totalLNCount + summary.totalINCount;
        int logEntries = (totalNodes / OFFSETS_PER_LN) + 1;
        return logEntries * BYTES_PER_LN;
    }

    /**
     * Gets the base summary from the cached map.  Add the tracked summary, if
     * one exists, to the base summary.  Sets all entries obsolete, if the file
     * is in the migrateFiles set.
     */
    private synchronized FileSummary getFileSummary(Long file) {
        FileSummary summary = fileSummaryMap.get(file);
        TrackedFileSummary trackedSummary = tracker.getTrackedFile(file);
        if (trackedSummary != null) {
            FileSummary totals = new FileSummary();
            totals.add(summary);
            totals.add(trackedSummary);
            summary = totals;
        }
        return summary;
    }

    /**
     * Count the given locally tracked info as obsolete and then log the file
     * and database info..
     */
    public void flushLocalTracker(LocalUtilizationTracker localTracker) throws DatabaseException {
        env.getLogManager().transferToUtilizationTracker(localTracker);
        flushFileUtilization(localTracker.getTrackedFiles());
        flushDbUtilization(localTracker);
    }

    /**
     * Flush a FileSummaryLN node for each TrackedFileSummary that is currently
     * active in the given tracker.
     */
    public void flushFileUtilization(Collection<TrackedFileSummary> activeFiles) throws DatabaseException {
        if (!DbInternal.getCheckpointUP(env.getConfigManager().getEnvironmentConfig())) {
            return;
        }
        for (TrackedFileSummary activeFile : activeFiles) {
            long fileNum = activeFile.getFileNumber();
            TrackedFileSummary tfs = tracker.getTrackedFile(fileNum);
            if (tfs != null) {
                flushFileSummary(tfs);
            }
        }
    }

    /**
     * Flush a MapLN for each database that has dirty utilization in the given
     * tracker.
     */
    private void flushDbUtilization(LocalUtilizationTracker localTracker) throws DatabaseException {
        if (!DbInternal.getCheckpointUP(env.getConfigManager().getEnvironmentConfig())) {
            return;
        }
        Iterator<Object> dbs = localTracker.getTrackedDbs().iterator();
        while (dbs.hasNext()) {
            DatabaseImpl db = (DatabaseImpl) dbs.next();
            if (!db.isDeleted() && db.isDirtyUtilization()) {
                env.getDbTree().modifyDbRoot(db);
            }
        }
    }

    /**
     * Returns a copy of the current file summary map, optionally including
     * tracked summary information, for use by the DbSpace utility and by unit
     * tests.  The returned map's key is a Long file number and its value is a
     * FileSummary.
     */
    public synchronized SortedMap<Long, FileSummary> getFileSummaryMap(boolean includeTrackedFiles) {
        assert cachePopulated;
        if (includeTrackedFiles) {
            TreeMap<Long, FileSummary> map = new TreeMap<Long, FileSummary>();
            for (Long file : fileSummaryMap.keySet()) {
                FileSummary summary = getFileSummary(file);
                map.put(file, summary);
            }
            for (TrackedFileSummary summary : tracker.getTrackedFiles()) {
                Long fileNum = Long.valueOf(summary.getFileNumber());
                if (!map.containsKey(fileNum)) {
                    map.put(fileNum, summary);
                }
            }
            return map;
        } else {
            return new TreeMap<Long, FileSummary>(fileSummaryMap);
        }
    }

    /**
     * Clears the cache of file summary info.  The cache starts out unpopulated
     * and is populated on the first call to getBestFileForCleaning.
     */
    public synchronized void clearCache() {
        int memorySize = fileSummaryMap.size() * MemoryBudget.UTILIZATION_PROFILE_ENTRY;
        MemoryBudget mb = env.getMemoryBudget();
        mb.updateAdminMemoryUsage(0 - memorySize);
        fileSummaryMap = new TreeMap<Long, FileSummary>();
        cachePopulated = false;
    }

    /**
     * Removes a file from the utilization database and the profile, after it
     * has been deleted by the cleaner.
     */
    void removeFile(Long fileNum, Set<DatabaseId> databases) throws DatabaseException {
        synchronized (this) {
            assert cachePopulated;
            FileSummary oldSummary = fileSummaryMap.remove(fileNum);
            if (oldSummary != null) {
                MemoryBudget mb = env.getMemoryBudget();
                mb.updateAdminMemoryUsage(0 - MemoryBudget.UTILIZATION_PROFILE_ENTRY);
            }
        }
        deleteFileSummary(fileNum, databases);
    }

    /**
     * Deletes all FileSummaryLNs for the file and updates all MapLNs to remove
     * the DbFileSummary for the file.  This method performs eviction and is
     * not synchronized.
     */
    private void deleteFileSummary(final Long fileNum, Set<DatabaseId> databases) throws DatabaseException {
        final LogManager logManager = env.getLogManager();
        final DbTree dbTree = env.getDbTree();
        DatabaseImpl idDatabase = dbTree.getDb(DbTree.ID_DB_ID);
        DatabaseImpl nameDatabase = dbTree.getDb(DbTree.NAME_DB_ID);
        boolean logRoot = false;
        if (logManager.removeDbFileSummary(idDatabase, fileNum)) {
            logRoot = true;
        }
        if (logManager.removeDbFileSummary(nameDatabase, fileNum)) {
            logRoot = true;
        }
        if (logRoot) {
            env.logMapTreeRoot();
        }
        if (databases != null) {
            for (DatabaseId dbId : databases) {
                if (!dbId.equals(DbTree.ID_DB_ID) && !dbId.equals(DbTree.NAME_DB_ID)) {
                    DatabaseImpl db = dbTree.getDb(dbId);
                    try {
                        if (db != null && logManager.removeDbFileSummary(db, fileNum)) {
                            dbTree.modifyDbRoot(db);
                        }
                    } finally {
                        dbTree.releaseDb(db);
                    }
                }
            }
        } else {
            CursorImpl.traverseDbWithCursor(idDatabase, LockType.NONE, true, new CursorImpl.WithCursor() {

                public boolean withCursor(CursorImpl cursor, DatabaseEntry key, DatabaseEntry data) throws DatabaseException {
                    MapLN mapLN = (MapLN) cursor.getCurrentLN(LockType.NONE);
                    if (mapLN != null) {
                        DatabaseImpl db = mapLN.getDatabase();
                        if (logManager.removeDbFileSummary(db, fileNum)) {
                            dbTree.modifyDbRoot(db, DbLsn.NULL_LSN, false);
                        }
                    }
                    return true;
                }
            });
        }
        Locker locker = null;
        CursorImpl cursor = null;
        boolean clearedTrackedFile = false;
        try {
            locker = BasicLocker.createBasicLocker(env, false);
            cursor = new CursorImpl(fileSummaryDb, locker);
            cursor.setAllowEviction(true);
            DatabaseEntry keyEntry = new DatabaseEntry();
            DatabaseEntry dataEntry = new DatabaseEntry();
            long fileNumVal = fileNum.longValue();
            OperationStatus status = OperationStatus.SUCCESS;
            if (getFirstFSLN(cursor, fileNumVal, keyEntry, dataEntry, LockType.WRITE)) {
                status = OperationStatus.SUCCESS;
            } else {
                status = OperationStatus.NOTFOUND;
            }
            while (status == OperationStatus.SUCCESS) {
                env.criticalEviction(true);
                FileSummaryLN ln = (FileSummaryLN) cursor.getCurrentLN(LockType.NONE);
                if (ln != null) {
                    if (fileNumVal != ln.getFileNumber(keyEntry.getData())) {
                        break;
                    }
                    TrackedFileSummary tfs = tracker.getTrackedFile(fileNumVal);
                    if (tfs != null) {
                        ln.setTrackedSummary(tfs);
                        clearedTrackedFile = true;
                    }
                    cursor.latchBIN();
                    cursor.delete(ReplicationContext.NO_REPLICATE);
                }
                status = cursor.getNext(keyEntry, dataEntry, LockType.WRITE, true, false);
            }
        } finally {
            if (cursor != null) {
                cursor.releaseBINs();
                cursor.close();
            }
            if (locker != null) {
                locker.operationEnd();
            }
        }
        if (!clearedTrackedFile) {
            TrackedFileSummary tfs = tracker.getTrackedFile(fileNum);
            if (tfs != null) {
                env.getLogManager().removeTrackedFile(tfs);
            }
        }
    }

    /**
     * Updates and stores the FileSummary for a given tracked file, if flushing
     * of the summary is allowed.
     */
    public void flushFileSummary(TrackedFileSummary tfs) throws DatabaseException {
        if (tfs.getAllowFlush()) {
            putFileSummary(tfs);
        }
    }

    /**
     * Updates and stores the FileSummary for a given tracked file.  This
     * method is synchronized and may not perform eviction.
     */
    private synchronized PackedOffsets putFileSummary(TrackedFileSummary tfs) throws DatabaseException {
        if (env.isReadOnly()) {
            throw EnvironmentFailureException.unexpectedState("Cannot write file summary in a read-only environment");
        }
        if (tfs.isEmpty()) {
            return null;
        }
        if (!cachePopulated) {
            return null;
        }
        long fileNum = tfs.getFileNumber();
        Long fileNumLong = Long.valueOf(fileNum);
        FileSummary summary = fileSummaryMap.get(fileNumLong);
        if (summary == null) {
            if (!fileSummaryMap.isEmpty() && fileNum < fileSummaryMap.lastKey() && !env.getFileManager().isFileValid(fileNum)) {
                env.getLogManager().removeTrackedFile(tfs);
                return null;
            }
            summary = new FileSummary();
        }
        FileSummary tmp = new FileSummary();
        tmp.add(summary);
        tmp.add(tfs);
        int sequence = tmp.getEntriesCounted();
        FileSummaryLN ln = new FileSummaryLN(env, summary);
        ln.setTrackedSummary(tfs);
        insertFileSummary(ln, fileNum, sequence);
        summary = ln.getBaseSummary();
        if (fileSummaryMap.put(fileNumLong, summary) == null) {
            MemoryBudget mb = env.getMemoryBudget();
            mb.updateAdminMemoryUsage(MemoryBudget.UTILIZATION_PROFILE_ENTRY);
        }
        return ln.getObsoleteOffsets();
    }

    /**
     * Returns the stored/packed obsolete offsets and the tracked obsolete
     * offsets for the given file.  The tracked summary object returned can be
     * used to test for obsolete offsets that are being added during cleaning
     * by other threads participating in lazy migration.  The caller must call
     * TrackedFileSummary.setAllowFlush(true) when cleaning is complete.
     * This method performs eviction and is not synchronized.
     * @param logUpdate if true, log any updates to the utilization profile. If
     * false, only retrieve the new information.
     */
    TrackedFileSummary getObsoleteDetail(Long fileNum, PackedOffsets packedOffsets, boolean logUpdate) throws DatabaseException {
        if (!env.getCleaner().trackDetail) {
            return null;
        }
        assert cachePopulated;
        long fileNumVal = fileNum.longValue();
        List<long[]> list = new ArrayList<long[]>();
        TrackedFileSummary tfs = env.getLogManager().getUnflushableTrackedSummary(fileNumVal);
        Locker locker = null;
        CursorImpl cursor = null;
        try {
            locker = BasicLocker.createBasicLocker(env, false);
            cursor = new CursorImpl(fileSummaryDb, locker);
            cursor.setAllowEviction(true);
            DatabaseEntry keyEntry = new DatabaseEntry();
            DatabaseEntry dataEntry = new DatabaseEntry();
            OperationStatus status = OperationStatus.SUCCESS;
            if (!getFirstFSLN(cursor, fileNumVal, keyEntry, dataEntry, LockType.NONE)) {
                status = OperationStatus.NOTFOUND;
            }
            while (status == OperationStatus.SUCCESS) {
                env.criticalEviction(true);
                FileSummaryLN ln = (FileSummaryLN) cursor.getCurrentLN(LockType.NONE);
                if (ln != null) {
                    if (fileNumVal != ln.getFileNumber(keyEntry.getData())) {
                        break;
                    }
                    PackedOffsets offsets = ln.getObsoleteOffsets();
                    if (offsets != null) {
                        list.add(offsets.toArray());
                    }
                    cursor.evict();
                }
                status = cursor.getNext(keyEntry, dataEntry, LockType.NONE, true, false);
            }
        } finally {
            if (cursor != null) {
                cursor.releaseBINs();
                cursor.close();
            }
            if (locker != null) {
                locker.operationEnd();
            }
        }
        if (!tfs.isEmpty()) {
            PackedOffsets offsets = null;
            if (logUpdate) {
                offsets = putFileSummary(tfs);
                if (offsets != null) {
                    list.add(offsets.toArray());
                }
            } else {
                long[] offsetList = tfs.getObsoleteOffsets();
                if (offsetList != null) {
                    list.add(offsetList);
                }
            }
        }
        int size = 0;
        for (int i = 0; i < list.size(); i += 1) {
            long[] a = list.get(i);
            size += a.length;
        }
        long[] offsets = new long[size];
        int index = 0;
        for (int i = 0; i < list.size(); i += 1) {
            long[] a = list.get(i);
            System.arraycopy(a, 0, offsets, index, a.length);
            index += a.length;
        }
        assert index == offsets.length;
        packedOffsets.pack(offsets);
        return tfs;
    }

    /**
     * Populate the profile for file selection.  This method performs eviction
     * and is not synchronized.  It must be called before recovery is complete
     * so that synchronization is unnecessary.  It must be called before the
     * recovery checkpoint so that the checkpoint can flush file summary
     * information.
     */
    public boolean populateCache() throws DatabaseException {
        assert !cachePopulated;
        if (!openFileSummaryDatabase()) {
            return false;
        }
        int oldMemorySize = fileSummaryMap.size() * MemoryBudget.UTILIZATION_PROFILE_ENTRY;
        Long[] existingFiles = env.getFileManager().getAllFileNumbers();
        Locker locker = null;
        CursorImpl cursor = null;
        try {
            locker = BasicLocker.createBasicLocker(env, false);
            cursor = new CursorImpl(fileSummaryDb, locker);
            cursor.setAllowEviction(true);
            DatabaseEntry keyEntry = new DatabaseEntry();
            DatabaseEntry dataEntry = new DatabaseEntry();
            if (cursor.positionFirstOrLast(true, null)) {
                OperationStatus status = cursor.getCurrentAlreadyLatched(keyEntry, dataEntry, LockType.NONE, true);
                if (status != OperationStatus.SUCCESS) {
                    status = cursor.getNext(keyEntry, dataEntry, LockType.NONE, true, false);
                }
                while (status == OperationStatus.SUCCESS) {
                    env.criticalEviction(false);
                    FileSummaryLN ln = (FileSummaryLN) cursor.getCurrentLN(LockType.NONE);
                    if (ln == null) {
                        status = cursor.getNext(keyEntry, dataEntry, LockType.NONE, true, false);
                        continue;
                    }
                    byte[] keyBytes = keyEntry.getData();
                    boolean isOldVersion = ln.hasStringKey(keyBytes);
                    long fileNum = ln.getFileNumber(keyBytes);
                    Long fileNumLong = Long.valueOf(fileNum);
                    if (Arrays.binarySearch(existingFiles, fileNumLong) >= 0) {
                        FileSummary summary = ln.getBaseSummary();
                        fileSummaryMap.put(fileNumLong, summary);
                        if (isOldVersion && !env.isReadOnly()) {
                            insertFileSummary(ln, fileNum, 0);
                            cursor.latchBIN();
                            cursor.delete(ReplicationContext.NO_REPLICATE);
                        } else {
                            cursor.evict();
                        }
                    } else {
                        fileSummaryMap.remove(fileNumLong);
                        if (!env.isReadOnly()) {
                            if (isOldVersion) {
                                cursor.latchBIN();
                                cursor.delete(ReplicationContext.NO_REPLICATE);
                            } else {
                                deleteFileSummary(fileNumLong, null);
                            }
                        }
                    }
                    if (isOldVersion) {
                        status = cursor.getNext(keyEntry, dataEntry, LockType.NONE, true, false);
                    } else {
                        if (!getFirstFSLN(cursor, fileNum + 1, keyEntry, dataEntry, LockType.NONE)) {
                            status = OperationStatus.NOTFOUND;
                        }
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.releaseBINs();
                cursor.close();
            }
            if (locker != null) {
                locker.operationEnd();
            }
            int newMemorySize = fileSummaryMap.size() * MemoryBudget.UTILIZATION_PROFILE_ENTRY;
            MemoryBudget mb = env.getMemoryBudget();
            mb.updateAdminMemoryUsage(newMemorySize - oldMemorySize);
        }
        cachePopulated = true;
        return true;
    }

    /**
     * Positions at the most recent LN for the given file number.
     */
    private boolean getFirstFSLN(CursorImpl cursor, long fileNum, DatabaseEntry keyEntry, DatabaseEntry dataEntry, LockType lockType) throws DatabaseException {
        byte[] keyBytes = FileSummaryLN.makePartialKey(fileNum);
        keyEntry.setData(keyBytes);
        int result = cursor.searchAndPosition(keyEntry, dataEntry, SearchMode.SET_RANGE, lockType);
        if ((result & CursorImpl.FOUND) == 0) {
            return false;
        }
        boolean exactKeyMatch = ((result & CursorImpl.EXACT_KEY) != 0);
        if (exactKeyMatch && cursor.getCurrentAlreadyLatched(keyEntry, dataEntry, lockType, true) != OperationStatus.KEYEMPTY) {
            return true;
        }
        cursor.evict(!exactKeyMatch);
        OperationStatus status = cursor.getNext(keyEntry, dataEntry, lockType, true, !exactKeyMatch);
        return status == OperationStatus.SUCCESS;
    }

    /**
     * If the file summary db is already open, return, otherwise attempt to
     * open it.  If the environment is read-only and the database doesn't
     * exist, return false.  If the environment is read-write the database will
     * be created if it doesn't exist.
     */
    private boolean openFileSummaryDatabase() throws DatabaseException {
        if (fileSummaryDb != null) {
            return true;
        }
        DbTree dbTree = env.getDbTree();
        Locker autoTxn = null;
        boolean operationOk = false;
        try {
            autoTxn = Txn.createLocalAutoTxn(env, new TransactionConfig());
            DatabaseImpl db = dbTree.getDb(autoTxn, DbTree.UTILIZATION_DB_NAME, null);
            if (db == null) {
                if (env.isReadOnly()) {
                    return false;
                }
                DatabaseConfig dbConfig = new DatabaseConfig();
                DbInternal.setReplicated(dbConfig, false);
                db = dbTree.createInternalDb(autoTxn, DbTree.UTILIZATION_DB_NAME, dbConfig);
            }
            fileSummaryDb = db;
            operationOk = true;
            return true;
        } finally {
            if (autoTxn != null) {
                autoTxn.operationEnd(operationOk);
            }
        }
    }

    /**
     * For unit testing.
     */
    public DatabaseImpl getFileSummaryDb() {
        return fileSummaryDb;
    }

    /**
     * Insert the given LN with the given key values.  This method is
     * synchronized and may not perform eviction.
     * 
     * Is public only for unit testing.
     */
    public synchronized boolean insertFileSummary(FileSummaryLN ln, long fileNum, int sequence) throws DatabaseException {
        byte[] keyBytes = FileSummaryLN.makeFullKey(fileNum, sequence);
        Locker locker = null;
        CursorImpl cursor = null;
        try {
            locker = BasicLocker.createBasicLocker(env, false);
            cursor = new CursorImpl(fileSummaryDb, locker);
            OperationStatus status = cursor.putLN(keyBytes, ln, null, false, ReplicationContext.NO_REPLICATE);
            if (status == OperationStatus.KEYEXIST) {
                LoggerUtils.traceAndLog(logger, env, Level.SEVERE, "Cleaner duplicate key sequence file=0x" + Long.toHexString(fileNum) + " sequence=0x" + Long.toHexString(sequence));
                return false;
            }
            BIN bin = cursor.latchBIN();
            ln.addExtraMarshaledMemorySize(bin);
            cursor.releaseBIN();
            cursor.evict();
            return true;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (locker != null) {
                locker.operationEnd();
            }
        }
    }

    /**
     * Checks that all FSLN offsets are indeed obsolete.  Assumes that the
     * system is quiesent (does not lock LNs).  This method is not synchronized
     * (because it doesn't access fileSummaryMap) and eviction is allowed.
     *
     * @return true if no verification failures.
     */
    public boolean verifyFileSummaryDatabase() throws DatabaseException {
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry data = new DatabaseEntry();
        openFileSummaryDatabase();
        Locker locker = null;
        CursorImpl cursor = null;
        boolean ok = true;
        try {
            locker = BasicLocker.createBasicLocker(env, false);
            cursor = new CursorImpl(fileSummaryDb, locker);
            cursor.setAllowEviction(true);
            if (cursor.positionFirstOrLast(true, null)) {
                OperationStatus status = cursor.getCurrentAlreadyLatched(key, data, LockType.NONE, true);
                while (status == OperationStatus.SUCCESS) {
                    env.criticalEviction(true);
                    FileSummaryLN ln = (FileSummaryLN) cursor.getCurrentLN(LockType.NONE);
                    if (ln != null) {
                        long fileNumVal = ln.getFileNumber(key.getData());
                        PackedOffsets offsets = ln.getObsoleteOffsets();
                        if (offsets != null) {
                            long[] vals = offsets.toArray();
                            for (int i = 0; i < vals.length; i++) {
                                long lsn = DbLsn.makeLsn(fileNumVal, vals[i]);
                                if (!verifyLsnIsObsolete(lsn)) {
                                    ok = false;
                                }
                            }
                        }
                        cursor.evict();
                        status = cursor.getNext(key, data, LockType.NONE, true, false);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (locker != null) {
                locker.operationEnd();
            }
        }
        return ok;
    }

    private boolean verifyLsnIsObsolete(long lsn) throws DatabaseException {
        Object o = env.getLogManager().getLogEntryHandleFileNotFound(lsn);
        if (!(o instanceof LNLogEntry)) {
            return true;
        }
        LNLogEntry entry = (LNLogEntry) o;
        if (entry.getLN().isDeleted()) {
            return true;
        }
        DatabaseId dbId = entry.getDbId();
        DatabaseImpl db = env.getDbTree().getDb(dbId);
        BIN bin = null;
        try {
            if (db == null || db.isDeleted()) {
                return true;
            }
            Tree tree = db.getTree();
            TreeLocation location = new TreeLocation();
            boolean parentFound = tree.getParentBINForChildLN(location, entry.getKey(), entry.getDupKey(), entry.getLN(), false, true, false, CacheMode.UNCHANGED);
            bin = location.bin;
            int index = location.index;
            if (!parentFound) {
                return true;
            }
            if (bin.isEntryKnownDeleted(index)) {
                return true;
            }
            if (bin.getLsn(index) != lsn) {
                return true;
            }
            System.err.println("lsn " + DbLsn.getNoFormatString(lsn) + " was found in tree.");
            return false;
        } finally {
            env.getDbTree().releaseDb(db);
            if (bin != null) {
                bin.releaseLatch();
            }
        }
    }

    /**
     * Update memory budgets when this profile is closed and will never be
     * accessed again.
     */
    void close() {
        clearCache();
        if (fileSummaryDb != null) {
            fileSummaryDb.releaseTreeAdminMemory();
        }
    }

    /**
     * Iterator over files that should be migrated by cleaning them, even if
     * they don't need to be cleaned for other reasons.
     *
     * Files are migrated either because they are named in the
     * CLEANER_FORCE_CLEAN_FILES parameter or their log version is prior to the
     * CLEANER_UPGRADE_TO_LOG_VERSION parameter.
     *
     * An iterator is used rather than finding the entire set at startup to
     * avoid opening a large number of files to examine their log version.  For
     * example, if all files are being migrated in a very large data set, this
     * would involve opening a very large number of files in order to read
     * their header.  This could significantly delay application startup.
     *
     * Because we don't have the entire set at startup, we can't select the
     * lowest utilization file from the set to clean next.  Inteaad we iterate
     * in file number order to increase the odds of cleaning lower utilization
     * files first.
     */
    private class FilesToMigrate {

        /**
         * An array of pairs of file numbers, where each pair is a range of
         * files to be force cleaned.  Index i is the from value and i+1 is the
         * to value, both inclusive.
         */
        private long[] forceCleanFiles;

        /** Log version to upgrade to, or zero if none. */
        private int upgradeToVersion;

        /** Whether to continue checking the log version. */
        private boolean checkLogVersion;

        /** Whether hasNext() has prepared a valid nextFile. */
        private boolean nextAvailable;

        /** File to return; set by hasNext() and returned by next(). */
        private long nextFile;

        FilesToMigrate() {
            String forceCleanProp = env.getConfigManager().get(EnvironmentParams.CLEANER_FORCE_CLEAN_FILES);
            parseForceCleanFiles(forceCleanProp);
            upgradeToVersion = env.getConfigManager().getInt(EnvironmentParams.CLEANER_UPGRADE_TO_LOG_VERSION);
            if (upgradeToVersion == -1) {
                upgradeToVersion = LogEntryType.LOG_VERSION;
            }
            checkLogVersion = (upgradeToVersion != 0);
            nextAvailable = false;
            nextFile = -1;
        }

        /**
         * Returns whether there are more files to be migrated.  Must be called
         * while synchronized on the UtilizationProfile.
         */
        boolean hasNext(SortedMap<Long, FileSummary> currentFileSummaryMap) throws DatabaseException {
            if (nextAvailable) {
                return true;
            }
            long foundFile = -1;
            for (long file : currentFileSummaryMap.tailMap(nextFile + 1).keySet()) {
                if (isForceCleanFile(file)) {
                    foundFile = file;
                    break;
                } else if (checkLogVersion) {
                    try {
                        int logVersion = env.getFileManager().getFileLogVersion(file);
                        if (logVersion < upgradeToVersion) {
                            foundFile = file;
                            break;
                        } else {
                            checkLogVersion = false;
                        }
                    } catch (DatabaseException e) {
                        nextFile = file;
                        throw e;
                    }
                }
            }
            if (foundFile != -1) {
                nextFile = foundFile;
                nextAvailable = true;
                return true;
            } else {
                return false;
            }
        }

        /**
         * Returns the next file file to be migrated.  Must be called while
         * synchronized on the UtilizationProfile.
         */
        long next(SortedMap<Long, FileSummary> currentFileSummaryMap) throws NoSuchElementException, DatabaseException {
            if (hasNext(currentFileSummaryMap)) {
                nextAvailable = false;
                return nextFile;
            } else {
                throw new NoSuchElementException();
            }
        }

        /**
         * Returns whether the given file is in the forceCleanFiles set.
         */
        private boolean isForceCleanFile(long file) {
            if (forceCleanFiles != null) {
                for (int i = 0; i < forceCleanFiles.length; i += 2) {
                    long from = forceCleanFiles[i];
                    long to = forceCleanFiles[i + 1];
                    if (file >= from && file <= to) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Parses the je.cleaner.forceCleanFiles property value and initializes
         * the forceCleanFiles field.
         *
         * @throws IllegalArgumentException via Environment ctor and
         * setMutableConfig.
         */
        private void parseForceCleanFiles(String propValue) throws IllegalArgumentException {
            if (propValue == null || propValue.length() == 0) {
                forceCleanFiles = null;
            } else {
                String errPrefix = "Error in " + EnvironmentParams.CLEANER_FORCE_CLEAN_FILES.getName() + "=" + propValue + ": ";
                StringTokenizer tokens = new StringTokenizer(propValue, ",-", true);
                List<Long> list = new ArrayList<Long>();
                while (tokens.hasMoreTokens()) {
                    String fromStr = tokens.nextToken();
                    long fromNum;
                    try {
                        fromNum = Long.parseLong(fromStr, 16);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(errPrefix + "Invalid hex file number: " + fromStr);
                    }
                    long toNum = -1;
                    if (tokens.hasMoreTokens()) {
                        String delim = tokens.nextToken();
                        if (",".equals(delim)) {
                            toNum = fromNum;
                        } else if ("-".equals(delim)) {
                            if (tokens.hasMoreTokens()) {
                                String toStr = tokens.nextToken();
                                try {
                                    toNum = Long.parseLong(toStr, 16);
                                } catch (NumberFormatException e) {
                                    throw new IllegalArgumentException(errPrefix + "Invalid hex file number: " + toStr);
                                }
                            } else {
                                throw new IllegalArgumentException(errPrefix + "Expected file number: " + delim);
                            }
                        } else {
                            throw new IllegalArgumentException(errPrefix + "Expected '-' or ',': " + delim);
                        }
                    } else {
                        toNum = fromNum;
                    }
                    assert toNum != -1;
                    list.add(Long.valueOf(fromNum));
                    list.add(Long.valueOf(toNum));
                }
                forceCleanFiles = new long[list.size()];
                for (int i = 0; i < forceCleanFiles.length; i += 1) {
                    forceCleanFiles[i] = list.get(i).longValue();
                }
            }
        }
    }
}
