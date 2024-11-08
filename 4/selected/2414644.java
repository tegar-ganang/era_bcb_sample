package net.sourceforge.processdash.tool.bridge.client;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import net.sourceforge.processdash.tool.bridge.OfflineLockStatus;
import net.sourceforge.processdash.tool.bridge.OfflineLockStatusListener;
import net.sourceforge.processdash.tool.bridge.ResourceCollectionInfo;
import net.sourceforge.processdash.tool.bridge.ResourceFilter;
import net.sourceforge.processdash.tool.bridge.ResourceFilterFactory;
import net.sourceforge.processdash.tool.bridge.ResourceListing;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollection;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.lock.AlreadyLockedException;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.LockMessage;
import net.sourceforge.processdash.util.lock.LockMessageHandler;
import net.sourceforge.processdash.util.lock.LockUncertainException;
import net.sourceforge.processdash.util.lock.OfflineLockLostException;

/**
 * Working directory implementation in which data is saved persistently to a
 * server.
 * 
 * This class can toggle between two different modes:
 * <ul>
 * 
 * <li>In regular mode, the server owns the "gold copy" of the data and the
 * local computer holds a cached, working copy. We check out a copy
 * of the data on startup, copy changes back periodically, and make certain to
 * flush all changes back to the server on shutdown. We also must ping the
 * server periodically to let it know we are still using the data; otherwise it
 * may give our lock away to another user.</li>
 * 
 * <li>In offline mode, the local computer owns the "gold copy" of the data and
 * the server holds a "best effort" backup. Changes are still copied back to the
 * server periodically in an effort to keep the server backup up-to-date, but
 * the client can disconnect from the network for extended periods of time and
 * the server will still hold the lock for this computer.</li>
 * 
 * </ul>
 */
public class BridgedWorkingDirectory extends AbstractWorkingDirectory {

    ResourceBridgeClient client;

    Worker worker;

    OfflineLockStatus offlineStatus;

    OfflineLockStatusChangeHandler offlineStatusHandler;

    Thread shutdownHook;

    private static final Logger logger = Logger.getLogger(BridgedWorkingDirectory.class.getName());

    protected BridgedWorkingDirectory(File targetDirectory, String remoteURL, FileResourceCollectionStrategy strategy, File workingDirectoryParent) {
        super(targetDirectory, remoteURL, strategy, workingDirectoryParent);
        FileResourceCollection collection = new FileResourceCollection(workingDirectory, false);
        collection.setStrategy(strategy);
        client = new ResourceBridgeClient(collection, remoteURL, strategy.getUnlockedFilter());
        client.setSourceIdentifier(getSourceIdentifier());
        try {
            initializeOfflineLockData();
        } catch (IOException ioe) {
        }
        offlineStatusHandler = new OfflineLockStatusChangeHandler();
        client.setOfflineLockStatusListener(offlineStatusHandler);
    }

    public void prepare() throws IOException {
        if (!processLock.isLocked()) throw new IllegalStateException("Process lock has not been obtained");
        if (offlineStatus == null) initializeOfflineLockData();
        if (isOfflineLockEnabled() == false) doOnlinePrepare();
    }

    private void doOnlinePrepare() throws IOException {
        doBackupImpl(workingDirectory, "startup");
        doSyncDown();
    }

    private void doSyncDown() throws IOException {
        SyncFilter filter = getSyncDownFilter();
        for (int numTries = 5; numTries-- > 0; ) if (client.syncDown(filter) == false) return;
        throw new IOException("Unable to sync down");
    }

    /**
     * @see WorkingDirectory#update()
     * 
     * @throws IllegalStateException 
     *             if the current process owns a write lock on this collection,
     *             or if it does not own a process lock
     */
    public void update() throws IOException {
        if (!processLock.isLocked()) throw new IllegalStateException("Process lock has not been obtained"); else if (worker == null && isOfflineLockEnabled() == false) doSyncDown(); else throw new IllegalStateException("update should not be called in " + "offline or read-write mode.");
    }

    public File getDirectory() {
        return workingDirectory;
    }

    public void acquireWriteLock(LockMessageHandler lockHandler, String ownerName) throws AlreadyLockedException, LockFailureException {
        if (isOfflineLockEnabled()) resumeOfflineWriteLock(ownerName); else acquireOnlineWriteLock(ownerName);
        worker = new Worker(lockHandler);
        registerShutdownHook();
    }

    private void resumeOfflineWriteLock(String ownerName) throws LockFailureException {
        try {
            client.resumeOfflineLock(ownerName);
        } catch (LockFailureException e) {
            if (syncTimestampIsRecent()) {
                try {
                    doOnlinePrepare();
                    acquireOnlineWriteLock(ownerName);
                    return;
                } catch (IOException ioe) {
                    logger.log(Level.SEVERE, "Unable to prepare bridged " + "working directory when attempting to recover " + "from a broken offline lock.", ioe);
                }
            }
            throw new OfflineLockLostException(e, getSyncTimestamp());
        }
    }

    private void acquireOnlineWriteLock(String ownerName) throws LockFailureException {
        client.acquireLock(ownerName);
    }

    public void assertWriteLock() throws LockFailureException {
        try {
            client.assertLock();
        } catch (LockUncertainException lue) {
            if (isOfflineLockEnabled() == false) throw lue;
        }
    }

    public boolean flushData() throws LockFailureException, IOException {
        try {
            for (int numTries = 5; numTries-- > 0; ) {
                if (client.syncUp() == false) {
                    if (worker != null) worker.resetFlushFrequency();
                    try {
                        client.saveDefaultExcludedFiles();
                    } catch (Exception e) {
                        logger.log(Level.FINE, "Unable to save default excluded files", e);
                    }
                    saveSyncTimestamp();
                    return true;
                }
            }
        } catch (IOException ioe) {
            if (isOfflineLockEnabled()) return true; else throw ioe;
        }
        return false;
    }

    /**
     * Enable or disable the lock for offline use.
     * 
     * @param offlineEnabled
     *            true if the lock should be enabled for offline use
     * @throws LockFailureException
     *             if a lock was not already held, if the server could not be
     *             reached, or if any other problem prevents the offline
     *             enablement state from being changed.
     */
    public void setOfflineLockEnabled(boolean offlineEnabled) throws LockFailureException {
        client.setOfflineLockEnabled(offlineEnabled);
    }

    public boolean isOfflineLockEnabled() {
        return offlineStatus == OfflineLockStatus.Enabled;
    }

    public OfflineLockStatus getOfflineLockStatus() {
        return offlineStatus;
    }

    public void addOfflineLockStatusListener(OfflineLockStatusListener l) {
        offlineStatusHandler.addListener(l);
    }

    public void removeOfflineLockStatusListener(OfflineLockStatusListener l) {
        offlineStatusHandler.removeListener(l);
    }

    public URL doBackup(String qualifier) throws IOException {
        if (worker != null) {
            try {
                URL serverResult = client.doBackup(qualifier);
                if (isOfflineLockEnabled() == false) return serverResult;
            } catch (IOException ioe) {
            }
        }
        return doBackupImpl(workingDirectory, qualifier);
    }

    public void releaseLocks() {
        if (worker != null) worker.shutDown();
        if (isOfflineLockEnabled() == false) client.releaseLock();
        if (processLock != null) processLock.releaseLock();
        unregisterShutdownHook();
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(shutdownHook = new Thread() {

            public void run() {
                shutdownHook = null;
                releaseLocks();
            }
        });
    }

    private void unregisterShutdownHook() {
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (Exception e) {
            }
            shutdownHook = null;
        }
    }

    private String getSourceIdentifier() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return null;
        }
    }

    /** read and initialize data relating to offline lock mode. */
    private void initializeOfflineLockData() throws IOException {
        client.setExtraLockData(getWorkingDirectoryGuid());
        readOfflineLockStateFromMetadata();
    }

    private String getWorkingDirectoryGuid() throws IOException {
        String result = getMetadata(DIR_GUID);
        if (!StringUtils.hasValue(result)) {
            UUID uuid = UUID.randomUUID();
            result = uuid.toString();
            setMetadata(DIR_GUID, result);
        }
        return result;
    }

    private void readOfflineLockStateFromMetadata() throws IOException {
        if (StringUtils.hasValue(getMetadata(OFFLINE_LOCK_MODE))) offlineStatus = OfflineLockStatus.Enabled; else offlineStatus = OfflineLockStatus.NotLocked;
    }

    private boolean updateLockStatusFromServer(OfflineLockStatus newStatus) {
        OfflineLockStatus oldStatus = offlineStatus;
        if (newStatus == oldStatus) return false;
        boolean oldEnabled = (oldStatus == OfflineLockStatus.Enabled);
        boolean newEnabled = (newStatus == OfflineLockStatus.Enabled);
        if (oldEnabled != newEnabled) {
            try {
                logger.info("Offline bridged lock is now " + newStatus);
                setMetadata(OFFLINE_LOCK_MODE, newEnabled ? "enabled" : null);
            } catch (IOException e) {
                logger.severe("Unable to write offline status[" + newStatus + "] to metadata.");
            }
        }
        offlineStatus = newStatus;
        return true;
    }

    @Override
    public String getModeDescriptor() {
        return (isOfflineLockEnabled() ? ", offline lock enabled" : "");
    }

    private SyncFilter getSyncDownFilter() {
        long timestamp;
        try {
            timestamp = Long.parseLong(getMetadata(SYNC_TIMESTAMP));
        } catch (Exception e) {
            return null;
        }
        return new SyncDownFilter(timestamp);
    }

    private void saveSyncTimestamp() {
        try {
            setMetadata(SYNC_TIMESTAMP, Long.toString(System.currentTimeMillis()));
        } catch (IOException ioe) {
        }
    }

    public void clearSyncTimestamp() {
        try {
            setMetadata(SYNC_TIMESTAMP, null);
        } catch (IOException ioe) {
        }
    }

    private Date getSyncTimestamp() {
        try {
            return new Date(Long.parseLong(getMetadata(SYNC_TIMESTAMP)));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean syncTimestampIsRecent() {
        try {
            String timestamp = getMetadata(SYNC_TIMESTAMP);
            if (timestamp == null) return false;
            ResourceFilter filter = ResourceFilterFactory.getForRequest(Collections.singletonMap(ResourceFilterFactory.LAST_MOD_PARAM, timestamp));
            ResourceCollectionInfo changedFiles = new ResourceListing(client.localCollection, filter);
            return changedFiles.listResourceNames().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private class SyncDownFilter implements SyncFilter {

        private long lastSyncTimestamp;

        public SyncDownFilter(long lastSyncTimestamp) {
            this.lastSyncTimestamp = lastSyncTimestamp;
        }

        /**
         * When running in bridged mode, the dashboard flushes all data to the
         * server periodically, and when it shuts down. Unfortunately, there
         * could still be scenarios in which the dashboard shuts down without
         * getting a chance to connect to the server and save changes.
         * 
         * Fortunately, many datasets are personal use datasets, used by a
         * single individual. If an individual's dashboard closes without saving
         * data, and no one else has opened the dataset in the meantime, there
         * isn't any reason why we can't recover from the earlier connectivity
         * problem and upload our locally changed files.
         * 
         * If a file was locally created/modified after our the most recent
         * connection to the server, and if the file has not been modified on
         * the server in the meantime, this class will detect that pattern and
         * ask the syncDown logic NOT to retrieve the obsolete file from the
         * server. Then, we will automatically save the locally modified file
         * to the server when we perform our next syncUp operation.
         */
        public boolean shouldSync(String name, long localTimestamp, long remoteTimestamp) {
            if (localTimestamp <= 0) {
                return true;
            }
            if (remoteTimestamp <= 0) {
                if (localTimestamp > lastSyncTimestamp) return false; else return true;
            }
            if (remoteTimestamp <= lastSyncTimestamp) return syncOnlyIfFileIsCorrupt(name); else return true;
        }

        private boolean syncOnlyIfFileIsCorrupt(String name) {
            File file = new File(workingDirectory, name);
            return strategy.isFilePossiblyCorrupt(file);
        }
    }

    private class OfflineLockStatusChangeHandler implements OfflineLockStatusListener, Runnable {

        private List<OfflineLockStatusListener> listeners = new ArrayList();

        public void addListener(OfflineLockStatusListener l) {
            listeners.add(l);
        }

        public void removeListener(OfflineLockStatusListener l) {
            listeners.remove(l);
        }

        public void setOfflineLockStatus(OfflineLockStatus status) {
            if (updateLockStatusFromServer(status) && !listeners.isEmpty()) SwingUtilities.invokeLater(this);
        }

        public void run() {
            List<OfflineLockStatusListener> toNotify = new ArrayList(listeners);
            for (OfflineLockStatusListener l : toNotify) {
                l.setOfflineLockStatus(getOfflineLockStatus());
            }
        }
    }

    private class Worker extends Thread {

        private volatile boolean isRunning;

        private LockMessageHandler lockHandler;

        private volatile int flushCountdown;

        private volatile int fullFlushCountdown;

        public Worker(LockMessageHandler lockHandler) {
            super("WorkingDirBridge.Worker(" + remoteURL + ")");
            setDaemon(true);
            this.isRunning = true;
            this.lockHandler = lockHandler;
            start();
        }

        @Override
        public void run() {
            while (isRunning) {
                try {
                    Thread.sleep(ONE_MINUTE);
                } catch (InterruptedException ie) {
                }
                if (!isRunning) break;
                try {
                    client.pingLock();
                    if (flushCountdown > 1) flushCountdown--; else {
                        resetFlushFrequency();
                        if (client.syncUp()) saveSyncTimestamp();
                        if (fullFlushCountdown > 1) {
                            fullFlushCountdown--;
                        } else {
                            client.saveDefaultExcludedFiles();
                            fullFlushCountdown = FULL_FLUSH_FREQUENCY;
                        }
                    }
                } catch (LockUncertainException lue) {
                } catch (LockFailureException lfe) {
                    sendMessage(LockMessage.LOCK_LOST_MESSAGE);
                } catch (IOException ioe) {
                } catch (Throwable e) {
                    logger.log(Level.WARNING, "Unexpected exception encountered when " + "uploading working files to server", e);
                }
            }
        }

        private void sendMessage(String message) {
            try {
                LockMessage lockMessage = new LockMessage(BridgedWorkingDirectory.this, message);
                lockHandler.handleMessage(lockMessage);
            } catch (Exception e) {
            }
        }

        public void shutDown() {
            this.isRunning = false;
        }

        public void resetFlushFrequency() {
            flushCountdown = FLUSH_FREQUENCY;
        }
    }

    private static final long ONE_MINUTE = 60 * 1000;

    private static final int FLUSH_FREQUENCY = 5;

    private static final int FULL_FLUSH_FREQUENCY = 12;

    private static final String DIR_GUID = "workingDirGUID";

    private static final String OFFLINE_LOCK_MODE = "offlineLockMode";

    private static final String SYNC_TIMESTAMP = "syncUpTimestamp";
}
