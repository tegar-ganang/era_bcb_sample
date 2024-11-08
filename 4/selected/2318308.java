package org.jampa.net.podcast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jampa.controllers.Controller;
import org.jampa.controllers.events.EventConstants;
import org.jampa.gui.translations.Messages;
import org.jampa.logging.Log;
import org.jampa.model.IPlaylist;
import org.jampa.model.podcasts.PodcastItem;
import org.jampa.net.NetworkManager;
import org.jampa.preferences.PreferenceConstants;

public class PodcastItemDownloaderJob extends Job {

    private static final int MAX_BUFFER_SIZE = Controller.getInstance().getPreferenceStore().getInt(PreferenceConstants.PODCAST_DOWNLOAD_BUFFER_SIZE);

    private static final int DISPLAY_BUFFER_SIZE = Controller.getInstance().getPreferenceStore().getInt(PreferenceConstants.PODCAST_DOWNLOAD_DISPLAY_BUFFER_SIZE);

    private static final String TEMPORARY_EXTENSION = ".tmp";

    private IPlaylist _playlist;

    private PodcastItem _audioItem;

    private String _sourceUrl;

    private String _fileName;

    private boolean _playAfterDownload;

    private BufferedInputStream bis = null;

    private BufferedOutputStream bos = null;

    private boolean _notifyUserOnError = false;

    public PodcastItemDownloaderJob(IPlaylist playlist, PodcastItem audioItem) {
        this(playlist, audioItem, Controller.getInstance().getPreferenceStore().getBoolean(PreferenceConstants.PODCAST_PLAY_AFTER_DOWNLOAD));
    }

    public PodcastItemDownloaderJob(IPlaylist playlist, PodcastItem audioItem, boolean playAfterDownload) {
        super(Messages.getString("PodcastItemDownloaderJob.JobTitle"));
        _playlist = playlist;
        _audioItem = audioItem;
        _sourceUrl = _audioItem.getUrl();
        _fileName = _audioItem.getTemporaryFileName();
        _playAfterDownload = playAfterDownload;
    }

    public PodcastItemDownloaderJob(IPlaylist playlist, PodcastItem audioItem, String destination, boolean notifyUserOnError) {
        super(Messages.getString("PodcastItemDownloaderJob.JobTitle"));
        _playlist = playlist;
        _audioItem = audioItem;
        _sourceUrl = _audioItem.getUrl();
        _fileName = destination;
        _playAfterDownload = false;
        _notifyUserOnError = notifyUserOnError;
    }

    private void processError(String message) {
        if (_notifyUserOnError) {
            Log.getInstance(PodcastItemDownloaderJob.class).warnWithUserNotification(message);
        } else {
            Log.getInstance(PodcastItemDownloaderJob.class).warn(message);
        }
    }

    private void closeDownload() {
        if (bis != null) {
            try {
                bis.close();
            } catch (IOException e) {
                processError("Error while closing binary download: IOException: " + e.getMessage() + ".");
            }
        }
        if (bos != null) {
            try {
                bos.close();
            } catch (IOException e) {
                processError("Error while closing binary download: IOException: " + e.getMessage() + ".");
            }
        }
    }

    private void finalizeDownload(String temporaryFileName) {
        File newFile = new File(_fileName);
        if (newFile.exists()) {
            newFile.delete();
        }
        File tempFile = new File(temporaryFileName);
        tempFile.renameTo(newFile);
    }

    private void deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        if (!Controller.getInstance().getDownloadController().isItemDownloading(_audioItem)) {
            Controller.getInstance().getEventController().firePodcastsUpdateChange(EventConstants.EVT_PODCAST_DOWNLOAD_BEGIN, null, _audioItem);
            monitor.beginTask(Messages.getString("PodcastItemDownloaderJob.MainTask") + " " + _audioItem.getTitle(), 1);
            String temporaryFileName = _fileName + TEMPORARY_EXTENSION;
            try {
                Log.getInstance(PodcastItemDownloaderJob.class).debug("Start download of " + _sourceUrl + " to " + _fileName);
                monitor.subTask(Messages.getString("PodcastItemDownloaderJob.Connecting") + " " + _sourceUrl);
                HttpURLConnection connection = NetworkManager.getInstance().getConnection(_sourceUrl);
                int size = connection.getContentLength();
                monitor.beginTask(Messages.getString("PodcastItemDownloaderJob.MainTask") + " " + _audioItem.getTitle(), (size / DISPLAY_BUFFER_SIZE) + 4);
                monitor.worked(1);
                if (monitor.isCanceled()) {
                    Controller.getInstance().getEventController().firePodcastsUpdateChange(EventConstants.EVT_PODCAST_DOWNLOAD_END, new Boolean(true), _audioItem);
                    return Status.CANCEL_STATUS;
                }
                monitor.subTask(Messages.getString("PodcastItemDownloaderJob.CreateFile") + " " + temporaryFileName);
                bis = new BufferedInputStream(connection.getInputStream());
                bos = new BufferedOutputStream(new FileOutputStream(temporaryFileName));
                monitor.worked(1);
                if (monitor.isCanceled()) {
                    closeDownload();
                    deleteFile(temporaryFileName);
                    Controller.getInstance().getEventController().firePodcastsUpdateChange(EventConstants.EVT_PODCAST_DOWNLOAD_END, new Boolean(true), _audioItem);
                    return Status.CANCEL_STATUS;
                }
                monitor.subTask(Messages.getString("PodcastItemDownloaderJob.Downloading") + " " + _sourceUrl);
                boolean downLoading = true;
                byte[] buffer;
                int downloaded = 0;
                int read;
                int stepRead = 0;
                while (downLoading) {
                    if (size - downloaded > MAX_BUFFER_SIZE) {
                        buffer = new byte[MAX_BUFFER_SIZE];
                    } else {
                        buffer = new byte[size - downloaded];
                    }
                    read = bis.read(buffer);
                    if (read > 0) {
                        bos.write(buffer, 0, read);
                        downloaded += read;
                        stepRead += read;
                    } else {
                        downLoading = false;
                    }
                    if (monitor.isCanceled()) {
                        closeDownload();
                        deleteFile(temporaryFileName);
                        Controller.getInstance().getEventController().firePodcastsUpdateChange(EventConstants.EVT_PODCAST_DOWNLOAD_END, new Boolean(true), _audioItem);
                        return Status.CANCEL_STATUS;
                    }
                    if (stepRead >= DISPLAY_BUFFER_SIZE) {
                        monitor.worked(1);
                        stepRead = 0;
                    }
                }
                monitor.worked(1);
            } catch (MalformedURLException e) {
                processError("Error while binary downloading: MalformedURLException.");
            } catch (IOException e) {
                processError("Error while binary downloading: IOException: " + e.getMessage() + ".");
            } finally {
                monitor.subTask(Messages.getString("PodcastItemDownloaderJob.ClosingDownload"));
                closeDownload();
                monitor.worked(1);
            }
            finalizeDownload(temporaryFileName);
            if (_playAfterDownload) {
                Controller.getInstance().getPlaylistController().playFile(_playlist, _audioItem);
            }
            Log.getInstance(PodcastItemDownloaderJob.class).debug("End of download.");
            Controller.getInstance().getEventController().firePodcastsUpdateChange(EventConstants.EVT_PODCAST_DOWNLOAD_END, new Boolean(false), _audioItem);
        } else {
            Log.getInstance(PodcastItemDownloaderJob.class).debug("Item already being downloaded: " + _audioItem.getTitle());
        }
        return Status.OK_STATUS;
    }
}
