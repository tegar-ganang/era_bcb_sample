package phex.download.swarming;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimerTask;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.bushe.swing.event.annotation.EventTopicSubscriber;
import phex.common.AbstractLifeCycle;
import phex.common.AddressCounter;
import phex.common.Environment;
import phex.common.EnvironmentConstants;
import phex.common.Phex;
import phex.common.PhexVersion;
import phex.common.RunnerQueueWorker;
import phex.common.ThreadTracking;
import phex.common.URN;
import phex.common.bandwidth.BandwidthController;
import phex.common.file.FileManager;
import phex.common.file.ManagedFile;
import phex.common.file.ManagedFileException;
import phex.common.log.LogBuffer;
import phex.common.log.NLogger;
import phex.download.BufferVolumeTracker;
import phex.download.DownloadDataWriter;
import phex.download.MagnetData;
import phex.download.MemoryFile;
import phex.download.RemoteFile;
import phex.download.swarming.SWDownloadCandidate.CandidateStatus;
import phex.event.ChangeEvent;
import phex.event.ContainerEvent;
import phex.event.PhexEventService;
import phex.event.PhexEventTopics;
import phex.event.UserMessageListener;
import phex.event.ContainerEvent.Type;
import phex.msg.QueryResponseMsg;
import phex.prefs.core.DownloadPrefs;
import phex.query.DownloadCandidateSnoop;
import phex.servent.Servent;
import phex.share.SharedFilesService;
import phex.utils.FileUtils;
import phex.utils.InternalFileHandler;
import phex.utils.SubscriptionDownloader;
import phex.xml.sax.DPhex;
import phex.xml.sax.DSubElementList;
import phex.xml.sax.XMLBuilder;
import phex.xml.sax.downloads.DDownloadFile;
import phex.xml.sax.parser.downloads.DownloadListHandler;

public class SwarmingManager extends AbstractLifeCycle {

    public static final short PRIORITY_MOVE_TO_TOP = 0;

    public static final short PRIORITY_MOVE_UP = 1;

    public static final short PRIORITY_MOVE_DOWN = 2;

    public static final short PRIORITY_MOVE_TO_BOTTOM = 3;

    /**
     * Indicates if the swarming manager is shutting down or not.
     */
    private boolean isManagerShutingDown;

    /**
     * The active workers. 
     */
    private List<SWDownloadWorker> workerList;

    /**
     * The download list.
     */
    private List<SWDownloadFile> downloadList;

    /**
     * A Map that maps URNs to download files they belong to. This is for
     * performant searching by urn.
     * When accessing this object locking via the downloadList object is
     * required.
     */
    private HashMap<URN, SWDownloadFile> urnToDownloadMap;

    private AddressCounter ipDownloadCounter;

    /**
     * The temporary worker holds the only worker that is used to check if more
     * workers are required. The temporary worker waits for a valid download set
     * once a valid set is found the worker loses its temporary status and a new
     * temporary worker will be created. This is used to only hold the necessary
     * number of workers.
     */
    private SWDownloadWorker temporaryWorker;

    /**
     * The worker launcher is responsible to launch download workers and 
     * to make sure there always are enough workers available.
     */
    private DownloadWorkerLauncher workerLauncher;

    /**
     * Regularly writes buffered download data to disk.
     */
    private DownloadDataWriter dataWriter;

    private BufferVolumeTracker downloadWriteBufferTracker;

    /**
     * A instance of a background runner queue to verify downloaded data.
     */
    private RunnerQueueWorker downloadVerifyRunner;

    /**
     * Lock object to lock saving of download lists.
     */
    private static Object saveDownloadListLock = new Object();

    /**
     * Object that holds the save job instance while a save job is running. The
     * reference is null if the job is not running.
     */
    private SaveDownloadListJob saveDownloadListJob;

    /**
     * Indicates if the download list has changed since the last time it was
     * saved.
     */
    private boolean downloadListChangedSinceSave;

    /**
     * A {@link LogBuffer} shared across all candidates.
     */
    private LogBuffer candidateLogBuffer;

    private SharedFilesService sharedFilesService;

    private DownloadCandidateSnoop candidateSnoop;

    private Servent servent;

    private PhexEventService eventService;

    public SwarmingManager(Servent servent, PhexEventService eventService, SharedFilesService sharedFilesService) {
        if (servent == null) {
            throw new NullPointerException("Servent is null.");
        }
        if (eventService == null) {
            throw new NullPointerException("PhexEventService is null.");
        }
        if (sharedFilesService == null) {
            throw new NullPointerException("SharedFilesService is null.");
        }
        this.eventService = eventService;
        eventService.processAnnotations(this);
        this.servent = servent;
        this.sharedFilesService = sharedFilesService;
        downloadListChangedSinceSave = false;
        isManagerShutingDown = false;
        workerList = new ArrayList<SWDownloadWorker>(5);
        downloadList = new ArrayList<SWDownloadFile>(5);
        urnToDownloadMap = new HashMap<URN, SWDownloadFile>();
        ipDownloadCounter = new AddressCounter(DownloadPrefs.MaxDownloadsPerIP.get().intValue(), false);
        dataWriter = new DownloadDataWriter(this);
        downloadVerifyRunner = new RunnerQueueWorker(Thread.NORM_PRIORITY - 1);
        downloadWriteBufferTracker = new BufferVolumeTracker(DownloadPrefs.MaxTotalDownloadWriteBuffer.get().intValue(), dataWriter);
        if (DownloadPrefs.CandidateLogBufferSize.get().intValue() > 0) {
            candidateLogBuffer = new LogBuffer(DownloadPrefs.CandidateLogBufferSize.get().intValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doStart() {
        LoadDownloadListJob job = new LoadDownloadListJob();
        job.start();
        candidateSnoop = new DownloadCandidateSnoop(this, servent.getSecurityService());
        servent.getMessageService().addMessageSubscriber(QueryResponseMsg.class, candidateSnoop);
        workerLauncher = new DownloadWorkerLauncher();
        workerLauncher.setDaemon(true);
        workerLauncher.start();
        dataWriter.start();
        Environment.getInstance().scheduleTimerTask(new SaveDownloadListTimer(), SaveDownloadListTimer.TIMER_PERIOD, SaveDownloadListTimer.TIMER_PERIOD);
        new SubscriptionDownloader();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doStop() {
        synchronized (this) {
            isManagerShutingDown = true;
        }
        if (workerLauncher != null) {
            workerLauncher.triggerCycle();
        }
        SWDownloadWorker[] workers = new SWDownloadWorker[workerList.size()];
        workerList.toArray(workers);
        for (int i = 0; i < workers.length; i++) {
            SWDownloadWorker worker = workers[i];
            if (worker != null) {
                worker.stopWorker();
            }
        }
        for (int i = 0; i < workers.length; i++) {
            SWDownloadWorker worker = workers[i];
            if (worker != null && worker.isInsideCriticalSection()) {
                worker.waitTillFinished();
            }
        }
        if (dataWriter != null) {
            dataWriter.shutdown();
        }
        shutdownForceSaveDownloadList();
    }

    /**
     * Creates the {@link BandwidthController} for the given download file.
     * @param downloadFile
     * @return
     */
    protected BandwidthController createBandwidthControllerFor(SWDownloadFile downloadFile) {
        BandwidthController bandwidthController = new BandwidthController("DownloadFile-" + downloadFile.toString(), Long.MAX_VALUE, servent.getBandwidthService().getDownloadBandwidthController());
        bandwidthController.activateShortTransferAvg(1000, 15);
        return bandwidthController;
    }

    public DownloadDataWriter getDownloadDataWriter() {
        return dataWriter;
    }

    public BufferVolumeTracker getDownloadWriteBufferTracker() {
        return downloadWriteBufferTracker;
    }

    public RunnerQueueWorker getDownloadVerifyRunner() {
        return downloadVerifyRunner;
    }

    public MemoryFile createMemoryFile(SWDownloadFile file) {
        return new MemoryFile(file, downloadWriteBufferTracker, dataWriter, downloadVerifyRunner);
    }

    public synchronized SWDownloadFile addFileToDownload(RemoteFile remoteFile, String filename, String searchTerm) {
        SWDownloadFile downloadFile = new SWDownloadFile(filename, searchTerm, remoteFile.getFileSize(), remoteFile.getURN(), this, eventService);
        downloadFile.addDownloadCandidate(remoteFile);
        int pos;
        synchronized (downloadList) {
            pos = downloadList.size();
            downloadList.add(downloadFile);
            URN urn = downloadFile.getFileURN();
            if (urn != null) {
                urnToDownloadMap.put(urn, downloadFile);
            }
        }
        fireDownloadFileAdded(downloadFile, pos);
        downloadFile.setStatus(SWDownloadConstants.STATUS_FILE_WAITING);
        workerLauncher.triggerCycle();
        triggerSaveDownloadList(true);
        return downloadFile;
    }

    /**
     * Adds a uri for download.
     * 
     * @param uri
     * @param preventDuplicate if true a file already downloaded or shared will 
     *        not be added again, in case sha1 urn can be determined.
     * @return the download file in case the download was added, false otherwise (see preventDuplicate).
     * @throws URIException
     */
    public synchronized SWDownloadFile addFileToDownload(URI uri, boolean preventDuplicate) throws URIException {
        if (preventDuplicate) {
            MagnetData magnetData = MagnetData.parseFromURI(uri);
            if (magnetData != null) {
                URN urn = MagnetData.lookupSHA1URN(magnetData);
                if (isURNDownloaded(urn) || sharedFilesService.isURNShared(urn)) {
                    return null;
                }
            }
        }
        if (NLogger.isDebugEnabled(SwarmingManager.class)) {
            NLogger.debug(SwarmingManager.class, "Adding new download by URI: " + uri.toString());
        }
        SWDownloadFile downloadFile = new SWDownloadFile(uri, this, eventService);
        URN urn = downloadFile.getFileURN();
        int pos;
        synchronized (downloadList) {
            pos = downloadList.size();
            downloadList.add(downloadFile);
            if (urn != null) {
                urnToDownloadMap.put(urn, downloadFile);
            }
        }
        fireDownloadFileAdded(downloadFile, pos);
        downloadFile.setStatus(SWDownloadConstants.STATUS_FILE_WAITING);
        workerLauncher.triggerCycle();
        triggerSaveDownloadList(true);
        return downloadFile;
    }

    /**
     * Adds a uri with a dir relative to the default dir for download.
     * 
     * @param uri
     * @param preventDuplicate if true a file already downloaded or shared will 
     *        not be added again, in case sha1 urn can be determined.
     * @return the download file in case the download was added, false otherwise (see preventDuplicate).
     * @throws URIException
     */
    public synchronized SWDownloadFile addFileToDownload(URI uri, String relativeDownloadDir, boolean preventDuplicate) throws URIException {
        if (preventDuplicate) {
            MagnetData magnetData = MagnetData.parseFromURI(uri);
            if (magnetData != null) {
                URN urn = MagnetData.lookupSHA1URN(magnetData);
                if (isURNDownloaded(urn) || sharedFilesService.isURNShared(urn)) {
                    return null;
                }
            }
        }
        if (NLogger.isDebugEnabled(SwarmingManager.class)) {
            NLogger.debug(SwarmingManager.class, "Adding new download by URI: " + uri.toString());
        }
        SWDownloadFile downloadFile = new SWDownloadFile(uri, this, eventService);
        File destinationDir = downloadFile.getDestinationDirectory();
        String destDir;
        if (destinationDir != null) {
            destDir = downloadFile.getDestinationDirectory().toString();
        } else {
            destDir = DownloadPrefs.DestinationDirectory.get();
        }
        File destinationDirectory = new File(destDir, relativeDownloadDir);
        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdir();
        }
        downloadFile.setDestinationDirectory(destinationDirectory);
        URN urn = downloadFile.getFileURN();
        int pos;
        synchronized (downloadList) {
            pos = downloadList.size();
            downloadList.add(downloadFile);
            if (urn != null) {
                urnToDownloadMap.put(urn, downloadFile);
            }
        }
        fireDownloadFileAdded(downloadFile, pos);
        downloadFile.setStatus(SWDownloadConstants.STATUS_FILE_WAITING);
        workerLauncher.triggerCycle();
        triggerSaveDownloadList(true);
        return downloadFile;
    }

    /**
     * Removes the download file from the download list. Stops all running downloads
     * and deletes all incomplete download files.
     */
    public void removeDownloadFile(SWDownloadFile file) {
        removeDownloadFileInternal(file);
        triggerSaveDownloadList(true);
    }

    /**
     * Removes the download files from the download list. Stops all running downloads
     * and deletes all incomplete download files.
     */
    public void removeDownloadFiles(SWDownloadFile[] files) {
        for (int i = 0; i < files.length; i++) {
            removeDownloadFileInternal(files[i]);
        }
        triggerSaveDownloadList(true);
    }

    /**
     * Removes the download files from the download list. Stops all running downloads
     * and deletes all incomplete download files. Block until all possible workers
     * are stopped.
     * @param file
     */
    private void removeDownloadFileInternal(SWDownloadFile file) {
        if (!file.isFileCompletedOrMoved() && !file.isDownloadStopped()) {
            file.stopDownload();
        }
        int pos;
        synchronized (downloadList) {
            pos = downloadList.indexOf(file);
            if (pos >= 0) {
                downloadList.remove(pos);
                fireDownloadFileRemoved(file, pos);
            }
            URN urn = file.getFileURN();
            if (urn != null) {
                urnToDownloadMap.remove(urn);
            }
        }
        file.removeIncompleteDownloadFile();
    }

    public Integer getDownloadPriority(SWDownloadFile file) {
        int pos = downloadList.indexOf(file);
        if (pos >= 0) {
            return Integer.valueOf(pos);
        }
        return null;
    }

    /**
     * Updates the priorities of the download files according to the order in 
     * the given download file array.
     * @param files the download file array.
     */
    public void updateDownloadFilePriorities(SWDownloadFile[] files) {
        synchronized (downloadList) {
            for (int i = 0; i < files.length; i++) {
                int pos = downloadList.indexOf(files[i]);
                if (pos >= 0) {
                    int newPos = i;
                    if (newPos < 0 || newPos >= downloadList.size()) {
                        newPos = pos;
                    }
                    downloadList.remove(pos);
                    downloadList.add(newPos, files[i]);
                    fireDownloadFileRemoved(files[i], pos);
                    fireDownloadFileAdded(files[i], newPos);
                }
            }
        }
    }

    /**
     * Moves the download file in the hierarchy.
     * 
     * @param moveDirection The move direction. Use one of the constants 
     *        PRIORITY_MOVE_TO_TOP, PRIORITY_MOVE_UP, PRIORITY_MOVE_DOWN or
     *        PRIORITY_MOVE_TO_BOTTOM.
     * @param file The SWDownloadFile to move the priority for.
     * @return the new position.
     */
    public int moveDownloadFilePriority(SWDownloadFile file, short moveDirection) {
        synchronized (downloadList) {
            int pos = downloadList.indexOf(file);
            if (pos >= 0) {
                int newPos = pos;
                switch(moveDirection) {
                    case PRIORITY_MOVE_UP:
                        newPos--;
                        break;
                    case PRIORITY_MOVE_DOWN:
                        newPos++;
                        break;
                    case PRIORITY_MOVE_TO_TOP:
                        newPos = 0;
                        break;
                    case PRIORITY_MOVE_TO_BOTTOM:
                        newPos = downloadList.size() - 1;
                        break;
                }
                if (newPos < 0 || newPos >= downloadList.size()) {
                    return pos;
                }
                downloadList.remove(pos);
                downloadList.add(newPos, file);
                fireDownloadFileRemoved(file, pos);
                fireDownloadFileAdded(file, newPos);
                return newPos;
            }
            return pos;
        }
    }

    /**
     * Returns the count of the download files
     */
    public int getDownloadFileCount() {
        return downloadList.size();
    }

    /**
     * Returns the count of the download files with the given status.
     */
    public int getDownloadFileCount(int status) {
        int count = 0;
        synchronized (downloadList) {
            for (SWDownloadFile file : downloadList) {
                if (file.getStatus() == status) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Returns the number of download files with a active status, this is:
     * STATUS_FILE_WAITING
     * STATUS_FILE_DOWNLOADING
     * STATUS_FILE_QUEUED
     * STATUS_FILE_COMPLETED -> completed but not yet moved.
     * 
     * A not active status would then be:
     * STATUS_FILE_STOPPED
     * STATUS_FILE_COMPLETED_MOVED
     */
    private int getActiveDownloadFileCount() {
        int count = 0;
        synchronized (downloadList) {
            for (SWDownloadFile file : downloadList) {
                switch(file.getStatus()) {
                    case SWDownloadConstants.STATUS_FILE_STOPPED:
                    case SWDownloadConstants.STATUS_FILE_COMPLETED_MOVED:
                        break;
                    default:
                        count++;
                }
            }
        }
        return count;
    }

    /**
     * Returns true if a download files with a active status is available.
     * This is:
     * STATUS_FILE_WAITING
     * STATUS_FILE_DOWNLOADING
     * STATUS_FILE_QUEUED
     * STATUS_FILE_COMPLETED -> completet but not yet moved.
     * 
     * A not active status would then be:
     * STATUS_FILE_STOPPED
     * STATUS_FILE_COMPLETED_MOVED
     */
    public boolean isDownloadActive() {
        synchronized (downloadList) {
            for (SWDownloadFile file : downloadList) {
                switch(file.getStatus()) {
                    case SWDownloadConstants.STATUS_FILE_STOPPED:
                    case SWDownloadConstants.STATUS_FILE_COMPLETED_MOVED:
                        break;
                    default:
                        return true;
                }
            }
        }
        return false;
    }

    public List<SWDownloadFile> getDownloadFileListCopy() {
        return new ArrayList<SWDownloadFile>(downloadList);
    }

    /**
     * Returns a download file at the given index or null if not available.
     */
    public SWDownloadFile getDownloadFile(int index) {
        synchronized (downloadList) {
            if (index < 0 || index >= downloadList.size()) {
                return null;
            }
            return downloadList.get(index);
        }
    }

    /**
     * Returns all download files at the given indices. In case one of the
     * indices is out of bounds the returned download file array contains a 
     * null object in at the corresponding position.
     * @param indices the indices to get the download files for.
     * @return Array of SWDownloadFiles, can contain null objects.
     */
    public SWDownloadFile[] getDownloadFilesAt(int[] indices) {
        synchronized (downloadList) {
            int length = indices.length;
            SWDownloadFile[] files = new SWDownloadFile[length];
            for (int i = 0; i < length; i++) {
                if (indices[i] < 0 || indices[i] >= downloadList.size()) {
                    files[i] = null;
                } else {
                    files[i] = downloadList.get(indices[i]);
                }
            }
            return files;
        }
    }

    /**
     * Returns a download files matching the given fileSize and urn.
     * This is used to find a existing download file for new search results.
     * The additional check for fileSize is a security test to identify faulty
     * search results with faked URNs.
     * @param fileSize the required file size
     * @param matchURN the required URN we need to match.
     * @return the found SWDownloadFile or null if not found.
     */
    public SWDownloadFile getDownloadFile(long fileSize, URN matchURN) {
        synchronized (downloadList) {
            SWDownloadFile file = getDownloadFileByURN(matchURN);
            if (file != null && file.getTotalDataSize() == fileSize) {
                return file;
            }
            return null;
        }
    }

    /**
     * Returns a download file only identified by the URN. This is used to
     * service partial download requests.
     */
    public SWDownloadFile getDownloadFileByURN(URN matchURN) {
        SWDownloadFile file;
        synchronized (downloadList) {
            file = urnToDownloadMap.get(matchURN);
            return file;
        }
    }

    /**
     * Returns whether a download file with the given URN exists.
     * @return true when a download file with the given URN exists, false otherwise.
     */
    public boolean isURNDownloaded(URN matchURN) {
        if (matchURN == null) {
            return false;
        }
        synchronized (downloadList) {
            return urnToDownloadMap.containsKey(matchURN);
        }
    }

    public void releaseCandidateAddress(SWDownloadCandidate candidate) {
        ipDownloadCounter.relaseAddress(candidate.getHostAddress());
    }

    /**
     * Returns a list of completed but not yet moved download files. This method 
     * is called by a download worker to finish up completed download files.
     * @return a list with completed download files.
     */
    public synchronized List<SWDownloadFile> getCompletedDownloadFiles() {
        synchronized (downloadList) {
            List<SWDownloadFile> list = new ArrayList<SWDownloadFile>(2);
            for (SWDownloadFile downloadFile : downloadList) {
                if (downloadFile.isFileCompleted()) {
                    list.add(downloadFile);
                }
            }
            return list;
        }
    }

    /**
     * Allocated a download set. The method will block until a complete download
     * set can be obtained.
     */
    public synchronized SWDownloadSet allocateDownloadSet(SWDownloadWorker worker) {
        synchronized (downloadList) {
            SWDownloadCandidate downloadCandidate = null;
            for (SWDownloadFile downloadFile : downloadList) {
                if (!downloadFile.isAbleToBeAllocated()) {
                    continue;
                }
                boolean segmentAvailable = downloadFile.isScopeAllocateable(null, false);
                if (!segmentAvailable) {
                    continue;
                }
                ipDownloadCounter.setMaxCount(DownloadPrefs.MaxDownloadsPerIP.get().intValue());
                downloadCandidate = downloadFile.allocateDownloadCandidate(worker, ipDownloadCounter);
                if (downloadCandidate == null) {
                    continue;
                }
                boolean segmentAllocateable = downloadFile.isScopeAllocateable(downloadCandidate.getAvailableScopeList(), downloadCandidate.isAvailableScopeComplete());
                if (!segmentAllocateable) {
                    downloadFile.releaseDownloadCandidate(downloadCandidate);
                    continue;
                }
                downloadFile.incrementWorkerCount();
                SWDownloadSet set = new SWDownloadSet(servent, downloadFile, downloadCandidate);
                if (worker == temporaryWorker) {
                    unsetTemporaryWorker();
                }
                return set;
            }
        }
        return null;
    }

    public LogBuffer getCandidateLogBuffer() {
        return candidateLogBuffer;
    }

    /**
     * Notifies the download manager about a change in the download list which
     * requires a download list save.
     */
    public void notifyDownloadListChange() {
        downloadListChangedSinceSave = true;
    }

    /**
     * Triggers a save of the download list. The call is not blocking and returns
     * directly, the save process is running in parallel.
     */
    private void triggerSaveDownloadList(boolean force) {
        if (!force && !downloadListChangedSinceSave) {
            return;
        }
        NLogger.debug(SwarmingManager.class, "Trigger save download list...");
        synchronized (saveDownloadListLock) {
            if (saveDownloadListJob != null) {
                saveDownloadListJob.triggerFollowUpSave();
            } else {
                saveDownloadListJob = new SaveDownloadListJob();
                saveDownloadListJob.start();
            }
        }
    }

    /**
     * Forces a save of the download list. The call returns after the save is
     * completed. Only the shutdown routine is allowed to call this method!
     */
    private void shutdownForceSaveDownloadList() {
        NLogger.debug(SwarmingManager.class, "Force save download list...");
        synchronized (saveDownloadListLock) {
            if (saveDownloadListJob == null) {
                saveDownloadListJob = new SaveDownloadListJob();
                saveDownloadListJob.start();
            } else {
                saveDownloadListJob.triggerFollowUpSave();
            }
        }
        try {
            if (saveDownloadListJob != null) {
                try {
                    saveDownloadListJob.setPriority(Thread.MAX_PRIORITY);
                    saveDownloadListJob.join();
                } catch (NullPointerException exp) {
                }
            }
        } catch (InterruptedException exp) {
            NLogger.error(SwarmingManager.class, exp, exp);
        }
    }

    /**
     * Unsets the current temporary worker since it became active
     * and creates a new temporary worker to continue worker count requirement check.
     */
    private synchronized void unsetTemporaryWorker() {
        temporaryWorker.setTemporaryWorker(false);
        temporaryWorker = null;
        workerLauncher.triggerCycle();
    }

    /**
     * Notifys all workers that are waiting to start downloading.
     */
    public synchronized void notifyWaitingWorkers() {
        notifyAll();
    }

    public synchronized void waitForNotify() throws InterruptedException {
        wait(2000);
    }

    private int getRequiredDownloadWorkerCount() {
        return Math.min(getActiveDownloadFileCount() * DownloadPrefs.MaxWorkerPerDownload.get().intValue(), DownloadPrefs.MaxTotalDownloadWorker.get().intValue());
    }

    /**
     * Checks if there are too many workers and stops the worker if needed.
     * Returns true if worker will be stopped false otherwise.
     * Also verifies if there are enough workers available and triggers worker
     * creating if necessary.
     */
    public synchronized boolean checkToStopWorker(SWDownloadWorker worker) {
        int requiredCount = getRequiredDownloadWorkerCount();
        if (isManagerShutingDown || workerList.size() > requiredCount) {
            if (worker.isRunning()) {
                worker.stopWorker();
                workerList.remove(worker);
                if (worker.isTemporaryWorker()) {
                    temporaryWorker = null;
                }
            }
            return true;
        }
        if (Thread.interrupted()) {
            return true;
        }
        return false;
    }

    /**
     * Called from worker if it unexpectedly shuts down.
     */
    public void notifyWorkerShoutdown(SWDownloadWorker worker, boolean isExpected) {
        NLogger.debug(SwarmingManager.class, "Worker shutdown: " + worker + ", expected: " + isExpected);
        worker.stopWorker();
        workerList.remove(worker);
        if (worker.isTemporaryWorker()) {
            temporaryWorker = null;
        }
        workerLauncher.triggerCycle();
    }

    @EventTopicSubscriber(topic = PhexEventTopics.Download_File_Completed)
    public void onDownloadFileCompletedEvent(String topic, SWDownloadFile file) {
        final File destFile = file.getDestinationFile();
        if (DownloadPrefs.AutoReadoutMagmaFiles.get().booleanValue() && destFile.getName().endsWith(".magma")) {
            Environment.getInstance().executeOnThreadPool(new Runnable() {

                public void run() {
                    InternalFileHandler.magmaReadout(destFile);
                }
            }, "Readout Magma");
        }
        if (DownloadPrefs.AutoReadoutMetalinkFiles.get().booleanValue() && destFile.getName().endsWith(".metalink")) {
            Environment.getInstance().executeOnThreadPool(new Runnable() {

                public void run() {
                    InternalFileHandler.metalinkReadout(destFile);
                }
            }, "Readout Metalink");
        }
        if (DownloadPrefs.AutoReadoutRSSFiles.get().booleanValue() && destFile.getName().endsWith(".rss.xml")) {
            Environment.getInstance().executeOnThreadPool(new Runnable() {

                public void run() {
                    InternalFileHandler.rssReadout(destFile);
                }
            }, "Readout RSS");
        }
    }

    @EventTopicSubscriber(topic = PhexEventTopics.Download_Candidate)
    public void onDownloadCandidateEvent(String topic, final ContainerEvent event) {
        if (event.getType() == ContainerEvent.Type.ADDED) {
            notifyWaitingWorkers();
        }
    }

    @EventTopicSubscriber(topic = PhexEventTopics.Download_Candidate_Status)
    public void onCandidateStatusChange(String topic, final ChangeEvent event) {
        switch((CandidateStatus) event.getNewValue()) {
            case WAITING:
                notifyWaitingWorkers();
                break;
            case CONNECTING:
            case PUSH_REQUEST:
            case REMOTLY_QUEUED:
            case ALLOCATING_SEGMENT:
            case REQUESTING:
            case DOWNLOADING:
            case IGNORED:
            case RANGE_UNAVAILABLE:
            case BAD:
            case CONNECTION_FAILED:
            case BUSY:
        }
    }

    private void fireDownloadFileAdded(SWDownloadFile file, int position) {
        eventService.publish(PhexEventTopics.Download_File, new ContainerEvent(Type.ADDED, file, this, position));
    }

    private void fireDownloadFileRemoved(SWDownloadFile file, int position) {
        eventService.publish(PhexEventTopics.Download_File, new ContainerEvent(Type.REMOVED, file, this, position));
    }

    /**
     * Class is responsible for launching and stopping download worker as 
     * required.
     */
    private class DownloadWorkerLauncher extends Thread {

        public DownloadWorkerLauncher() {
            super(ThreadTracking.rootThreadGroup, "DownloadWorkerLauncher");
        }

        @Override
        public void run() {
            while (!isManagerShutingDown) {
                try {
                    createRequiredWorker();
                    waitForNextCycle();
                } catch (Throwable th) {
                    NLogger.error(SwarmingManager.class, th, th);
                }
            }
        }

        public synchronized void triggerCycle() {
            this.notify();
        }

        private synchronized void waitForNextCycle() throws InterruptedException {
            wait(2000);
        }

        public void createRequiredWorker() {
            synchronized (SwarmingManager.this) {
                int requiredCount = getRequiredDownloadWorkerCount();
                if (temporaryWorker == null && workerList.size() < requiredCount) {
                    temporaryWorker = new SWDownloadWorker(SwarmingManager.this);
                    temporaryWorker.setTemporaryWorker(true);
                    temporaryWorker.startWorker();
                    workerList.add(temporaryWorker);
                    NLogger.debug(SwarmingManager.class, "Creating new worker: " + temporaryWorker + " for a total of: " + workerList.size());
                }
            }
        }
    }

    private class LoadDownloadListJob extends Thread {

        public LoadDownloadListJob() {
            super(ThreadTracking.rootThreadGroup, "LoadDownloadListJob");
        }

        @Override
        public void run() {
            try {
                loadDownloadList();
            } catch (Throwable th) {
                NLogger.error(SwarmingManager.class, th, th);
            }
        }

        private void loadDownloadList() {
            NLogger.debug(SwarmingManager.class, "Loading download list...");
            File downloadFile = Environment.getInstance().getPhexConfigFile(EnvironmentConstants.XML_DOWNLOAD_FILE_NAME);
            File downloadFileBak = new File(downloadFile.getAbsolutePath() + ".bak");
            if (!downloadFile.exists() && !downloadFileBak.exists()) {
                NLogger.debug(SwarmingManager.class, "No download list file found.");
                return;
            }
            DPhex dPhex;
            try {
                NLogger.debug(SwarmingManager.class, "Try to load from default download list.");
                FileManager fileMgr = Phex.getFileManager();
                ManagedFile managedFile = fileMgr.getReadWriteManagedFile(downloadFile);
                dPhex = XMLBuilder.loadDPhexFromFile(managedFile);
                if (dPhex == null) {
                    NLogger.debug(SwarmingManager.class, "Try to load from backup download list.");
                    ManagedFile managedFileBak = fileMgr.getReadWriteManagedFile(downloadFileBak);
                    dPhex = XMLBuilder.loadDPhexFromFile(managedFileBak);
                }
                if (dPhex == null) {
                    NLogger.debug(SwarmingManager.class, "No download settings file found.");
                    return;
                }
                DSubElementList<DDownloadFile> dDownloadList = dPhex.getDownloadList();
                if (dDownloadList != null) {
                    loadXJBSWDownloadList(dDownloadList);
                } else {
                    NLogger.debug(SwarmingManager.class, "No SWDownloadList found.");
                }
                notifyWaitingWorkers();
            } catch (IOException exp) {
                NLogger.error(SwarmingManager.class, exp, exp);
                Environment.getInstance().fireDisplayUserMessage(UserMessageListener.DownloadSettingsLoadFailed, new String[] { exp.toString() });
                return;
            } catch (ManagedFileException exp) {
                NLogger.error(SwarmingManager.class, exp, exp);
                Environment.getInstance().fireDisplayUserMessage(UserMessageListener.DownloadSettingsLoadFailed, new String[] { exp.toString() });
                return;
            }
        }

        private void loadXJBSWDownloadList(DSubElementList<DDownloadFile> list) {
            synchronized (SwarmingManager.this) {
                synchronized (downloadList) {
                    NLogger.debug(SwarmingManager.class, "Loading SWDownload xml");
                    downloadList.clear();
                    urnToDownloadMap.clear();
                    SWDownloadFile file;
                    for (DDownloadFile dFile : list.getSubElementList()) {
                        try {
                            file = new SWDownloadFile(dFile, SwarmingManager.this, eventService);
                            int pos = downloadList.size();
                            downloadList.add(file);
                            URN urn = file.getFileURN();
                            if (urn != null) {
                                urnToDownloadMap.put(urn, file);
                            }
                            NLogger.debug(SwarmingManager.class, "Loaded SWDownloadFile: " + file);
                            fireDownloadFileAdded(file, pos);
                        } catch (Exception exp) {
                            NLogger.error(SwarmingManager.class, "Error loading a download file from XML.", exp);
                        }
                    }
                }
            }
        }
    }

    private class SaveDownloadListTimer extends TimerTask {

        public static final long TIMER_PERIOD = 1000 * 60;

        @Override
        public void run() {
            try {
                triggerSaveDownloadList(false);
            } catch (Throwable th) {
                NLogger.error(SwarmingManager.class, th, th);
            }
        }
    }

    private class SaveDownloadListJob extends Thread {

        private boolean isFollowUpSaveTriggered;

        public SaveDownloadListJob() {
            super(ThreadTracking.rootThreadGroup, "SaveDownloadListJob");
            setPriority(Thread.MIN_PRIORITY);
        }

        public void triggerFollowUpSave() {
            isFollowUpSaveTriggered = true;
        }

        /**
         * Saving of the download list is done asynchronously to make sure that there
         * will be no deadlocks happening
         */
        @Override
        public void run() {
            do {
                NLogger.debug(SwarmingManager.class, "Start saving download list...");
                downloadListChangedSinceSave = false;
                isFollowUpSaveTriggered = false;
                try {
                    DPhex dPhex = new DPhex();
                    dPhex.setPhexVersion(PhexVersion.getFullVersion());
                    DSubElementList<DDownloadFile> dList = createDDownloadList();
                    dPhex.setDownloadList(dList);
                    File downloadFile = Environment.getInstance().getPhexConfigFile(EnvironmentConstants.XML_DOWNLOAD_FILE_NAME);
                    File downloadFileBak = new File(downloadFile.getAbsolutePath() + ".bak");
                    ManagedFile managedFile = Phex.getFileManager().getReadWriteManagedFile(downloadFileBak);
                    XMLBuilder.saveToFile(managedFile, dPhex);
                    FileUtils.copyFile(downloadFileBak, downloadFile);
                } catch (ManagedFileException exp) {
                    NLogger.error(SwarmingManager.class, exp, exp);
                    Environment.getInstance().fireDisplayUserMessage(UserMessageListener.DownloadSettingsSaveFailed, new String[] { exp.toString() });
                } catch (IOException exp) {
                    NLogger.error(SwarmingManager.class, exp, exp);
                    Environment.getInstance().fireDisplayUserMessage(UserMessageListener.DownloadSettingsSaveFailed, new String[] { exp.toString() });
                }
            } while (isFollowUpSaveTriggered);
            NLogger.debug(SwarmingManager.class, "Finished saving download list...");
            synchronized (saveDownloadListLock) {
                saveDownloadListJob = null;
            }
        }

        /**
         * Creates the DElement representation of this object to serialize it 
         * into XML.
         * @return the DElement representation of this object
         */
        private DSubElementList<DDownloadFile> createDDownloadList() {
            DSubElementList<DDownloadFile> dList = new DSubElementList<DDownloadFile>(DownloadListHandler.THIS_TAG_NAME);
            synchronized (SwarmingManager.this) {
                synchronized (downloadList) {
                    List<DDownloadFile> list = dList.getSubElementList();
                    for (SWDownloadFile file : downloadList) {
                        try {
                            list.add(file.createDDownloadFile());
                        } catch (Throwable th) {
                            NLogger.error(SwarmingManager.class, th, th);
                        }
                    }
                }
            }
            return dList;
        }
    }
}
