package com.showdown.update;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.showdown.api.impl.ShowDownManager;
import com.showdown.log.ShowDownLog;
import com.showdown.update.ResourceUpdater;

/**
 * Job for parsing the available updates for ShowDown, allowing the user to decide
 * what files to import/merge, and then running the actual update.
 * @author Mat DeLong
 */
public class UpdateJob {

    private UpdateSite updateSite;

    private IUpdateCallback callback;

    private FindAvailableUpdatesThread findUpdatesThread;

    private boolean asynchronous;

    private boolean cancelled;

    private VersionInfo versionInfo;

    private List<UpdateFile> updateSiteFiles;

    public static void main(String[] args) {
        UpdateSite site = new UpdateSite("http://show-down.sourceforge.net/update");
        UpdateJob uj = new UpdateJob(site, null, false);
        uj.findAvailableUpdates();
    }

    /**
    * Constructor for the job. Runs asynchronously
    * @param updateSite the update site to use. Must not be null.
    * @param callback the callback to notify when update parsing is complete
    */
    public UpdateJob(UpdateSite updateSite, IUpdateCallback callback) {
        this(updateSite, callback, true);
    }

    /**
    * Constructor for the job
    * @param updateSite the update site to use. Must not be null.
    * @param callback the callback to notify when update parsing is complete
    * @param asynchronous true to run tasks in another thread, false to run in same thread.
    *              this is useful so that command line jobs could be made to run an update.
    */
    public UpdateJob(UpdateSite updateSite, IUpdateCallback callback, boolean asynchronous) {
        assert updateSite != null;
        this.updateSite = updateSite;
        this.callback = callback;
        this.asynchronous = asynchronous;
        this.cancelled = false;
    }

    /**
    * Cancel the job, free the callback
    */
    public void cancel() {
        this.cancelled = true;
        this.callback = null;
    }

    /**
    * Kicks off a thread to find all of the available updates. The IUpdateCallback will
    * be notified when this completes.
    */
    public void findAvailableUpdates() {
        findUpdatesThread = new FindAvailableUpdatesThread();
        if (asynchronous) {
            findUpdatesThread.start();
        } else {
            findUpdatesThread.run();
        }
    }

    /**
    * Downloads all of the available files for update, notifying the callback for each file.
    * This also does the actual updating, where ShowDown will be restarted.
    */
    public void downloadSelectedUpdates() {
        File tempDir = ShowDownManager.INSTANCE.getSDTempDirectory();
        tempDir = new File(tempDir, Long.toString(System.currentTimeMillis()));
        ShowDownLog.getInstance().logDebug("Downloading Update files as needed.");
        DownloadAndUpdateThread daut = new DownloadAndUpdateThread(tempDir, getFilesForDelete(), getOverwriteFiles(), getUpdateParsers(), getUpdateFeeds(), getUpdateShows());
        if (asynchronous) {
            daut.start();
        } else {
            daut.run();
        }
    }

    /**
    * Returns manager for files to delete
    * @return the filesForDelete
    */
    public UpdateFilesForDelete getFilesForDelete() {
        return findUpdatesThread == null ? null : findUpdatesThread.filesForDelete;
    }

    /**
    * Returns the UpdateOverwriteFiles object, for the user to manipulate
    * @return the overwriteFiles object
    */
    public UpdateOverwriteFiles getOverwriteFiles() {
        return findUpdatesThread == null ? null : findUpdatesThread.overwriteFiles;
    }

    /**
    * Returns the UpdateParsers object, for the user to manipulate
    * @return the updateParsers object
    */
    public UpdateParsers getUpdateParsers() {
        return findUpdatesThread == null ? null : findUpdatesThread.updateParsers;
    }

    /**
    * Returns the UpdateFeeds object, for the user to manipulate
    * @return the updateFeeds object
    */
    public UpdateFeeds getUpdateFeeds() {
        return findUpdatesThread == null ? null : findUpdatesThread.updateFeeds;
    }

    /**
    * Returns the UpdateShows object, for the user to manipulate
    * @return the updateShows object
    */
    public UpdateShows getUpdateShows() {
        return findUpdatesThread == null ? null : findUpdatesThread.updateShows;
    }

    /**
    * Thread for using the update site to find files available for update.
    */
    private class FindAvailableUpdatesThread extends Thread {

        private boolean hasUpdate = false;

        private UpdateFilesForDelete filesForDelete;

        private UpdateOverwriteFiles overwriteFiles;

        private UpdateParsers updateParsers;

        private UpdateFeeds updateFeeds;

        private UpdateShows updateShows;

        /**
       * {@inheritDoc}
       */
        public void run() {
            if (!updateSite.isSiteSupported()) {
                if (callback != null) {
                    callback.updateCheckComplete(false);
                }
                return;
            }
            try {
                versionInfo = new VersionInfo();
                updateSiteFiles = updateSite.getUpdateFiles();
                if (updateSiteFiles != null) {
                    filesForDelete = new UpdateFilesForDelete(versionInfo.getLocalFilesForDelete(updateSiteFiles));
                    overwriteFiles = new UpdateOverwriteFiles();
                    hasUpdate = hasUpdate || !filesForDelete.getPaths().isEmpty();
                    for (UpdateFile f : updateSiteFiles) {
                        if (cancelled) {
                            break;
                        }
                        long localVer = versionInfo.getLocalVersion(f);
                        if (localVer < f.getVersion()) {
                            hasUpdate = true;
                            switch(f.getType()) {
                                case EPPARSERS:
                                    updateParsers = new UpdateParsers(f);
                                    break;
                                case FEEDS:
                                    updateFeeds = new UpdateFeeds(f);
                                    break;
                                case SHOWS:
                                    updateShows = new UpdateShows(f);
                                    break;
                                default:
                                    overwriteFiles.addFile(f);
                            }
                        }
                    }
                }
            } finally {
                if (callback != null) {
                    callback.updateCheckComplete(hasUpdate);
                }
            }
        }
    }

    /**
    * Thread for doing the download and then doing the update if the download was successful
    */
    private class DownloadAndUpdateThread extends Thread {

        private File tempDir;

        private UpdateFilesForDelete filesForDelete;

        private UpdateOverwriteFiles overwriteFiles;

        private UpdateParsers updateParsers;

        private UpdateFeeds updateFeeds;

        private UpdateShows updateShows;

        /**
       * Constructor that takes in all of the required information
       * @param tempDir the temporary directory to use
       * @param filesForDelete the files for delete
       * @param overwriteFiles the files to replace without any merging
       * @param updateParsers the parser file for updating
       * @param updateFeeds the feeds file for updating
       * @param updateShows the shows file for updating
       */
        public DownloadAndUpdateThread(File tempDir, UpdateFilesForDelete filesForDelete, UpdateOverwriteFiles overwriteFiles, UpdateParsers updateParsers, UpdateFeeds updateFeeds, UpdateShows updateShows) {
            this.tempDir = tempDir;
            this.filesForDelete = filesForDelete;
            this.overwriteFiles = overwriteFiles;
            this.updateParsers = updateParsers;
            this.updateFeeds = updateFeeds;
            this.updateShows = updateShows;
        }

        /**
       * {@inheritDoc}
       */
        public void run() {
            if (cancelled) {
                return;
            }
            boolean success = true;
            Map<File, File> resourcesToMove = new HashMap<File, File>();
            List<File> deleteFiles = null;
            try {
                if (overwriteFiles != null) {
                    resourcesToMove.putAll(overwriteFiles.download(tempDir, callback));
                }
                if (cancelled) {
                    return;
                }
                if (updateParsers != null) {
                    resourcesToMove.putAll(updateParsers.download(tempDir, callback));
                }
                if (cancelled) {
                    return;
                }
                if (updateFeeds != null) {
                    resourcesToMove.putAll(updateFeeds.download(tempDir, callback));
                }
                if (cancelled) {
                    return;
                }
                if (updateShows != null) {
                    resourcesToMove.putAll(updateShows.download(tempDir, callback));
                }
                if (cancelled) {
                    return;
                }
                if (filesForDelete != null) {
                    deleteFiles = filesForDelete.getFilesForDelete();
                }
                if (cancelled) {
                    return;
                }
            } catch (Exception ex) {
                ShowDownLog.getInstance().logError(ex.getLocalizedMessage(), ex);
                success = false;
            }
            if (callback != null) {
                ShowDownLog.getInstance().logDebug("Update download complete.");
                callback.updatesDownloadComplete(success);
            }
            if (success) {
                doShowDownUpdate(deleteFiles, tempDir, resourcesToMove);
            }
        }
    }

    private static UpdateFile getUpdateFile(File file, List<UpdateFile> updateSiteFiles) {
        for (UpdateFile f : updateSiteFiles) {
            File path = new File(ShowDownManager.INSTANCE.getInstallDirectory(), f.getPath());
            if (path.equals(file)) {
                return f;
            }
        }
        return null;
    }

    private void updateVersions(Collection<File> files) {
        if (updateSite != null) {
            if (versionInfo == null) {
                versionInfo = new VersionInfo();
            }
            if (updateSiteFiles == null) {
                updateSiteFiles = updateSite.getUpdateFiles();
            }
            for (File f : files) {
                versionInfo.setLocalVersionToMatch(getUpdateFile(f, updateSiteFiles));
            }
            versionInfo.storeChanges();
        }
    }

    private void doShowDownUpdate(List<File> deleteFiles, File tempDir, Map<File, File> resourcesToMove) {
        updateVersions(resourcesToMove.values());
        if ((deleteFiles != null && !deleteFiles.isEmpty()) || (resourcesToMove != null && !resourcesToMove.isEmpty())) {
            File installDir = ShowDownManager.INSTANCE.getInstallDirectory();
            File executableFile = null;
            String executableCommand = null;
            switch(ShowDownManager.INSTANCE.getOS()) {
                case WINDOWS:
                    executableFile = new File(installDir, "showdown.jar");
                    executableCommand = "java -jar showdown.jar";
                    break;
                case LINUX:
                    executableFile = new File(installDir, "showdown.jar");
                    executableCommand = "java -jar showdown.jar";
                    break;
                case MAC:
                    executableFile = new File(installDir, "showdown.jar");
                    executableCommand = "java -jar showdown.jar";
                    break;
            }
            if (executableFile != null || executableCommand != null) {
                ResourceUpdater updater = new ResourceUpdater();
                updater.updateResources(executableFile, executableCommand, resourcesToMove, tempDir, deleteFiles);
            }
        }
    }
}
