package org.happycomp.radio.downloader.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import org.happycomp.radio.Playlist;
import org.happycomp.radio.PlaylistEntry;
import org.happycomp.radio.ProcessStates;
import org.happycomp.radio.Station;
import org.happycomp.radio.StopDownloadCondition;
import org.happycomp.radio.StoreStateException;
import org.happycomp.radio.downloader.Downloader;
import org.happycomp.radio.downloader.DownloadingItem;
import org.happycomp.radio.io.IOUtils;
import org.happycomp.radio.scheduler.Scheduler;
import org.happycomp.radio.utils.RESTHelper;
import com.google.inject.Inject;

public class DownloaderImpl implements Downloader {

    public static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(DownloaderImpl.class.getName());

    private Scheduler radioScheduler;

    private Lock downloaderLock;

    private List<DownloadingItem> downloadingItems;

    @Inject
    public DownloaderImpl(Scheduler radioScheduler) {
        super();
        this.downloaderLock = new ReentrantLock();
        this.downloadingItems = new ArrayList<DownloadingItem>();
        this.radioScheduler = radioScheduler;
    }

    @Override
    public Playlist getPlayList(Station st) throws IOException, IvalidPlaylistContentException {
        String playlistUrlString = st.getPlaylistUrlString();
        if (playlistUrlString.endsWith(".pls")) {
            String content = null;
            try {
                InputStream inputStream = RESTHelper.inputStream(playlistUrlString);
                content = IOUtils.readAsString(inputStream, Charset.forName("ISO8859-1"), true);
                Playlist plist = new Playlist(st);
                plist.loadFromStream(content);
                return plist;
            } catch (Exception e) {
                throw new IvalidPlaylistContentException(e, content);
            }
        } else {
            String content = null;
            try {
                Playlist plist = new Playlist(st);
                PlaylistEntry entry = new PlaylistEntry(st);
                InputStream inputStream = RESTHelper.inputStream(playlistUrlString);
                content = IOUtils.readAsString(inputStream, Charset.forName("ISO8859-1"), true);
                entry.setUrl(new URL(content.trim()));
                entry.setTitle(st.name());
                plist.addUrlToPlaylist(entry);
                return plist;
            } catch (Exception e) {
                throw new IvalidPlaylistContentException(e, content);
            }
        }
    }

    @Override
    public DownloadingItem download(Playlist playlist, String title, File folder, StopDownloadCondition condition, String uuid) throws IOException, StoreStateException {
        boolean firstIteration = true;
        Iterator<PlaylistEntry> entries = playlist.getEntries().iterator();
        DownloadingItem prevItem = null;
        File[] previousDownloadedFiles = new File[0];
        while (entries.hasNext()) {
            PlaylistEntry entry = entries.next();
            DownloadingItem item = null;
            LOGGER.info("Downloading from '" + entry.getTitle() + "'");
            InputStream is = RESTHelper.inputStream(entry.getUrl());
            boolean stopped = false;
            File nfile = null;
            try {
                nfile = createFileStream(folder, entry);
                item = new DownloadingItem(nfile, uuid.toString(), title, entry, new Date(), getPID(), condition);
                if (previousDownloadedFiles.length > 0) {
                    item.setPreviousFiles(previousDownloadedFiles);
                }
                addItem(item);
                if (prevItem != null) deletePrevItem(prevItem);
                prevItem = item;
                stopped = IOUtils.copyStreams(is, new FileOutputStream(nfile), condition);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                radioScheduler.fireException(e);
                if (!condition.isStopped()) {
                    File[] nfiles = new File[previousDownloadedFiles.length + 1];
                    System.arraycopy(previousDownloadedFiles, 0, nfiles, 0, previousDownloadedFiles.length);
                    nfiles[nfiles.length - 1] = item.getFile();
                    previousDownloadedFiles = nfiles;
                    if ((!entries.hasNext()) && (firstIteration)) {
                        firstIteration = false;
                        entries = playlist.getEntries().iterator();
                    }
                    continue;
                }
            }
            if (stopped) {
                item.setState(ProcessStates.STOPPED);
                this.radioScheduler.fireStopDownloading(item);
                return item;
            }
        }
        return null;
    }

    private void addItem(DownloadingItem item) throws StoreStateException {
        try {
            this.downloaderLock.lock();
            this.downloadingItems.add(item);
        } finally {
            this.downloaderLock.unlock();
        }
    }

    @Override
    public void removeDownloadingItem(DownloadingItem item) {
        try {
            this.downloaderLock.lock();
            this.downloadingItems.remove(item);
        } finally {
            this.downloaderLock.unlock();
        }
    }

    private void deletePrevItem(DownloadingItem prevItem) {
        try {
            this.downloaderLock.lock();
            this.downloadingItems.remove(prevItem);
        } finally {
            this.downloaderLock.unlock();
        }
    }

    private File createFileStream(File folder, PlaylistEntry entry) throws IOException {
        File nfile = new File(folder, entry.getStation().name());
        int counter = 1;
        while (nfile.exists()) {
            nfile = new File(folder, entry.getStation().name() + "_" + (counter++));
        }
        nfile.createNewFile();
        return nfile;
    }

    private static String getPID() {
        String pid = null;
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String[] split = name.split("@");
        if ((split != null) && (split.length > 1)) {
            pid = split[0];
        }
        return pid;
    }

    @Override
    public Lock getDownladerLock() {
        return this.downloaderLock;
    }

    @Override
    public DownloadingItem[] getDownloadingItems() {
        try {
            this.downloaderLock.lock();
            return (DownloadingItem[]) this.downloadingItems.toArray(new DownloadingItem[this.downloadingItems.size()]);
        } finally {
            this.downloaderLock.unlock();
        }
    }

    public void setDownloadingItems(DownloadingItem[] items) {
        try {
            this.downloaderLock.lock();
            this.downloadingItems.clear();
            this.downloadingItems.addAll(Arrays.asList(items));
        } finally {
            this.downloaderLock.unlock();
        }
    }
}
