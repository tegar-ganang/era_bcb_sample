package com.nogoodatcoding.folder2feed;

import com.nogoodatcoding.commons.Commons;
import com.nogoodatcoding.commons.CommonsIO;
import com.nogoodatcoding.commons.ConfigurableEndsWithFileFilter;
import com.nogoodatcoding.commons.FileDateComparator;
import com.nogoodatcoding.folder2feed.interfaces.FileMonitor;
import com.nogoodatcoding.folder2feed.interfaces.FileMonitorStateChangeEvent;
import com.nogoodatcoding.folder2feed.interfaces.FileMonitorStateListener;
import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndCategoryImpl;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;

/**
 *
 * Monitors a specified location for updates.
 *
 * @author no.good.at.coding
 *
 */
public class FileMonitorImpl implements FileMonitor {

    private static Logger log_ = Logger.getLogger(FileMonitorImpl.class);

    private static ResourceBundle messages_ = ResourceBundle.getBundle("com.nogoodatcoding.folder2feed.messages.Messages_FileMonitor");

    private static short treeTraversalInterval_ = Settings.getTreeTraversalInterval();

    private long retryInterval_ = 3600000;

    private boolean finish_ = false;

    private String pathToProperties_ = null;

    private String pathToMonitor_ = null;

    private File toMonitor_ = null;

    private Properties feedProperties_ = null;

    private FileMonitorThread fileMonitorThread_ = null;

    private FileMonitorState state_ = FileMonitorState.STOPPED;

    private ArrayList<FileMonitorStateListener> fileMonitorStateListeners_ = new ArrayList<FileMonitorStateListener>(3);

    /**
     *
     * Convenience constructor for calling
     * {@link FileMonitorImpl#FileMonitorImpl(java.lang.String, boolean)
     * with {@code false} i.e. will not start the thread
     *
     * @param pathToProperties The location of the properties file that contains
     *                         the settings for the feed to be generated
     *
     * @throws java.io.IOException
     *
     * @see FileMonitorImpl#FileMonitorImpl(java.lang.String, boolean)
     *
     */
    public FileMonitorImpl(String pathToProperties) throws IOException {
        this(pathToProperties, false);
    }

    /**
     *
     * Constructor for {@code FileMonitorImpl}
     *
     * @param pathToProperties The location of the properties file that contains
     *                         the settings for the feed to be generated
     *
     * @param startMonitoring If true, will create and start the thread; else
     *                        will not
     *
     * @throws java.io.IOException If there are any issues while loading the
     *                             properties
     *
     * @see FileMonitorImpl#FileMonitorImpl(java.lang.String)
     *
     */
    public FileMonitorImpl(String pathToProperties, boolean startMonitoring) throws IOException {
        pathToProperties_ = pathToProperties;
        feedProperties_ = CommonsIO.loadProperties(pathToProperties);
        if (startMonitoring) {
            this.startMonitor();
        }
    }

    /**
     *
     * Returns the path to the properties file for this thread
     *
     * @return The path to the properties file for this thread
     *
     */
    public String getPathToProperties() {
        return pathToProperties_;
    }

    /**
     *
     * Returns the {@code Properties} object for this thread
     *
     * @return The {@code Properties} object for this {@code FileMonitor}
     *
     */
    public Properties getProperties() {
        return feedProperties_;
    }

    /**
     *
     * Calls {@code loadValues()} and then creates and starts the
     * {@code FileMonitorThread} for this {@code FileMonitor}.
     *
     * @throws java.io.IOException
     *
     * @see FileMonitorImpl#loadValues()
     *
     */
    public void startMonitor() throws IOException {
        loadValues();
        fileMonitorThread_ = new FileMonitorThread();
        fileMonitorThread_.start();
    }

    /**
     * 
     * Adds the listener and also sends a {@code FileMonitorStateChangeEvent}
     * immediately (with the current state as the both, the previous as well as
     * current state).
     * 
     * @param listener The {@code FileMonitorStateLister} to be added for this
     * {@code FileMonitor}
     * 
     * @see FileMonitorStateChangeEvent
     * 
     */
    public void addFileMonitorStateChangeListener(FileMonitorStateListener listener) {
        fileMonitorStateListeners_.add(listener);
        listener.fileMonitorStateChanged(new FileMonitorStateChangeEventImpl(state_, state_, this));
    }

    /**
     *
     * Returns the {@code File} that is being monitored
     *
     * @return The {@code File} being monitored
     *
     */
    public File getMonitoredFile() {
        return toMonitor_;
    }

    /**
     *
     * Changes the {@code Properties} associated with this {@code FileMonitor}.
     * Also stops the current thread and re-starts it with the new values.
     *
     * @param feedProperties The new {@code Properties} for this feed
     *
     * @param pathToProperties Optional; new path for the properties file. Pass
     *                         a {@code null} if it's not to be changed. But if
     *                         different, it should be given otherwise the
     *                         properties will not get persisted properly
     *
     */
    public void setProperties(Properties feedProperties, String pathToProperties) {
        stopMonitor();
        while (getState() != FileMonitorState.STOPPED) {
        }
        ;
        pathToProperties_ = pathToProperties;
        this.feedProperties_ = feedProperties;
    }

    /**
     *
     * Stops the {@code FileMonitorThread} by first calling {@code finish()} on
     * it (to raise the flag) and then interrupting it, in case it is sleeping
     *
     */
    public void stopMonitor() {
        if (fileMonitorThread_ != null) {
            fileMonitorThread_.finish();
            fileMonitorThread_.interrupt();
        }
    }

    /**
     *
     * Returns the current state of the {@code FileMonitorThread}
     *
     * @return The current state of the {@code FileMonitorThread}
     */
    public FileMonitorState getState() {
        return state_;
    }

    /**
     *
     * Saves the current state of the {@code FileMonitorThread} in a variable
     * Also triggers the {@code stateChanged()} method
     *
     * @param state The current {@code FileMonitorState}
     *
     */
    private void setState(FileMonitorState state) {
        FileMonitorStateChangeEvent event = new FileMonitorStateChangeEventImpl(this.state_, state, this);
        synchronized (this.state_) {
            this.state_ = state;
            stateChanged(event);
        }
    }

    /**
     *
     * Loads the properties values
     *
     * @throws java.io.IOException
     *
     */
    private void loadValues() throws IOException {
        stopMonitor();
        toMonitor_ = new File(feedProperties_.getProperty("feed.settings.pathToMonitor"));
        FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.log.debug.property.pathToMonitor") + toMonitor_.getPath());
    }

    /**
     *
     * Notifies all listeners of the change in {@code FileMonitorState}
     *
     * @param event The {@code FileMonitorStateChangeEvent} indicate the state
     *              change
     */
    private void stateChanged(FileMonitorStateChangeEvent event) {
        for (FileMonitorStateListener listener : fileMonitorStateListeners_) {
            listener.fileMonitorStateChanged(event);
        }
    }

    /**
     * The actual thread that does the monitoring
     */
    private class FileMonitorThread extends Thread {

        private Object[] threadMessageArguments_ = null;

        private MessageFormat threadSleepingMessageFormatter_ = null;

        private long oldestInList_ = 0;

        private FileMonitorState fileMonitorThreadState_ = FileMonitorState.STOPPED;

        @Override
        public void run() {
            try {
                finish_ = false;
                setFileMonitorThreadState(FileMonitorState.WAITING);
                pathToMonitor_ = toMonitor_.getAbsolutePath();
                super.setName("FileMonitor_for_#" + pathToMonitor_ + "#");
                while (!toMonitor_.canRead() && !Settings.isAppExit() && !finish_) {
                    FileMonitorImpl.log_.warn(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.warn.invalidPathToMonitor") + toMonitor_.getPath());
                    if (Settings.isSingleRun()) {
                        FileMonitorImpl.log_.warn(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.warn.noRetry") + toMonitor_.getPath());
                        finish();
                    } else {
                        try {
                            FileMonitorImpl.log_.info(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.info.thread.retrying") + retryInterval_);
                            setFileMonitorThreadState(FileMonitorState.WAITING);
                            sleep(retryInterval_);
                            setFileMonitorThreadState(FileMonitorState.RUNNING);
                        } catch (InterruptedException e) {
                            FileMonitorImpl.log_.error(getName() + FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.error.thread.interrupted"));
                            setFileMonitorThreadState(FileMonitorState.STOPPED);
                        }
                    }
                }
                if (!Settings.isAppExit() && !finish_) {
                    setFileMonitorThreadState(FileMonitorState.RUNNING);
                    FileMonitorImpl.log_.info(this.getName() + FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.info.thread.started"));
                    boolean excludeSubFolders = false;
                    boolean includeHidden = false;
                    short toInclude = -1;
                    int maxItems = 0;
                    int listSizeAfterLastRun = 0;
                    int numberOfNodesInDirectoryTree = 0;
                    long newestInList = 0;
                    long tempOldestInList = 0;
                    long tempNewestInList = 0;
                    long updateInterval = 0;
                    String feedTitle = null;
                    String feedDescription = null;
                    String feedLink = null;
                    String feedType = null;
                    String feedPath = null;
                    String[] arrayOfCategories = null;
                    FileFilter fileFilter = null;
                    List<File> treeList = null;
                    SyndFeed feed = null;
                    threadMessageArguments_ = new Object[2];
                    threadMessageArguments_[0] = this.getName();
                    threadSleepingMessageFormatter_ = new MessageFormat("");
                    threadSleepingMessageFormatter_.setLocale(Locale.getDefault());
                    threadSleepingMessageFormatter_.applyPattern(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.thread.sleepingFor"));
                    MessageFormat threadNextRunMessageFormatter = new MessageFormat("");
                    threadNextRunMessageFormatter.setLocale(Locale.getDefault());
                    threadNextRunMessageFormatter.applyPattern(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.info.thread.nextRun"));
                    feedType = feedProperties_.getProperty("feed.settings.feedType");
                    feedTitle = feedProperties_.getProperty("feed.title");
                    feedDescription = feedProperties_.getProperty("feed.description");
                    feedLink = feedProperties_.getProperty("feed.link");
                    feedPath = feedProperties_.getProperty("feed.settings.pathToXML");
                    toInclude = Utils.whatToInclude(feedProperties_.getProperty("feed.settings.include"));
                    excludeSubFolders = Boolean.parseBoolean(feedProperties_.getProperty("feed.settings.excludeSubFolders"));
                    includeHidden = Boolean.parseBoolean(feedProperties_.getProperty("feed.settings.includeHidden"));
                    FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.splittingCategories"));
                    arrayOfCategories = (feedProperties_.getProperty("feed.tags")).split("\\s*,\\s*");
                    maxItems = Integer.parseInt(feedProperties_.getProperty("feed.settings.maxItems"));
                    updateInterval = Long.parseLong(feedProperties_.getProperty("feed.settings.updateInterval"));
                    fileFilter = ConfigurableEndsWithFileFilter.getInstance(feedProperties_.getProperty("feed.settings.fileTypes"));
                    FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.property.feedType") + feedType);
                    FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.property.feedTitle") + feedTitle);
                    FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.property.feedDescription") + feedDescription);
                    FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.property.feedLink") + feedLink);
                    FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.property.feedPath") + feedPath);
                    FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.property.toInclude") + ((toInclude == Utils.FILES_ONLY) ? "FILES_ONLY" : toInclude == Utils.FOLDERS_ONLY ? "FOLDERS_ONLY" : "FILES_AND_FOLDERS"));
                    FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.property.excludeSubFolders") + excludeSubFolders);
                    FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.property.includeHidden") + includeHidden);
                    FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.property.maxItems") + maxItems);
                    FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.property.updateInterval") + updateInterval);
                    FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.property.fileTypes") + feedProperties_.getProperty("feed.settings.fileTypes"));
                    feed = new SyndFeedImpl();
                    feed.setFeedType(feedType);
                    feed.setTitle(feedTitle);
                    feed.setDescription(feedDescription);
                    feed.setLink(feedLink);
                    feedType = null;
                    feedDescription = null;
                    feedLink = null;
                    treeList = new ArrayList<File>();
                    while (!(Settings.isAppExit() | finish_)) {
                        numberOfNodesInDirectoryTree = getTreeList(toMonitor_, treeList, fileFilter, toInclude, excludeSubFolders, includeHidden);
                        if (numberOfNodesInDirectoryTree > 0) {
                            if (treeList.size() < maxItems && numberOfNodesInDirectoryTree >= maxItems) {
                                FileMonitorImpl.log_.info("fileMonitor.fileMonitorThread.log.info.forceCheck");
                                forceCheck();
                                numberOfNodesInDirectoryTree = getTreeList(toMonitor_, treeList, fileFilter, toInclude, excludeSubFolders, includeHidden);
                            }
                            Collections.sort(treeList, new FileDateComparator());
                            Commons.trim(treeList, maxItems);
                            tempOldestInList = Commons.getLatestTimestamp((File) treeList.get(treeList.size() - 1));
                            tempNewestInList = Commons.getLatestTimestamp((File) treeList.get(0));
                            if ((tempOldestInList - oldestInList_) > 1 || (tempNewestInList - newestInList) > 0 || (listSizeAfterLastRun - treeList.size()) > 0) {
                                FileMonitorImpl.log_.info(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.info.writePending"));
                                feed.setEntries(createEntries(treeList, arrayOfCategories));
                                try {
                                    CommonsIO.writeFeed(feedPath, feed);
                                    FileMonitorImpl.log_.info(feedPath + FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.info.feedXMLWritten"));
                                    feed.setEntries(null);
                                } catch (IOException e) {
                                    FileMonitorImpl.log_.error(FileMonitorImpl.messages_.getString("fileMonitor.log.error.ioExceptionWhileWritingToFile") + e);
                                    e.printStackTrace();
                                } catch (FeedException e) {
                                    FileMonitorImpl.log_.error(FileMonitorImpl.messages_.getString("fileMonitor.log.error.feedExceptionWhileWritingToFile") + e);
                                    e.printStackTrace();
                                }
                            } else {
                                FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.noNewItems"));
                            }
                            oldestInList_ = tempOldestInList - 1;
                            newestInList = tempNewestInList;
                        }
                        listSizeAfterLastRun = treeList.size();
                        treeList.clear();
                        System.gc();
                        if (Settings.isSingleRun()) {
                            break;
                        }
                        try {
                            threadMessageArguments_[1] = updateInterval;
                            FileMonitorImpl.log_.debug(threadSleepingMessageFormatter_.format(threadMessageArguments_));
                            threadMessageArguments_[1] = (new Date()).getTime() + updateInterval;
                            FileMonitorImpl.log_.info(threadNextRunMessageFormatter.format(threadMessageArguments_));
                            setFileMonitorThreadState(FileMonitorState.SLEEPING);
                            sleep(updateInterval);
                            FileMonitorImpl.log_.debug(this.getName() + FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.thread.wokenUp"));
                        } catch (InterruptedException e) {
                            FileMonitorImpl.log_.error(this.getName() + FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.error.thread.interrupted"));
                            e.printStackTrace();
                        } finally {
                            setFileMonitorThreadState(FileMonitorState.RUNNING);
                        }
                    }
                    FileMonitorImpl.log_.info(this.getName() + FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.info.thread.exiting"));
                    setFileMonitorThreadState(FileMonitorState.STOPPED);
                } else {
                    setFileMonitorThreadState(FileMonitorState.STOPPED);
                }
            } catch (Exception e) {
                setFileMonitorThreadState(FileMonitorState.ERROR);
            }
        }

        /**
         *
         * Recursively iterates over the directory tree to check the files (and
         * folders if enabled). The timestamps of the nodes are compared with the
         * {@code oldestInList} timestamp (or 0 if it's a forced check) and only
         * newer items are kept; older ones are discarded.
         *
         * @param fileToCheck The File to be checked. If directory, it's children
         *                    will be checked recursively if {@code toInclude}
         *                    allows it
         * @param treeList The {@code List} to which the {@code File}s are to be
         *                 added
         * @param fileFilter A {@link FileFilter} to be applied to the contents of a
         *                   folder to get children
         * @param toInclude Indicates what is to be included: files, folders or both
         * @param excludeSubFolders Indicates whether only the 'root' location is to
         *                          be monitored or even subfolders
         * @param includeHidden Indicates if hidden files and folders are to be
         *                      included or not
         * @return Number of nodes in the tree that satisfy the criteria (file type,
         *         hidden etc)
         *
         */
        private int getTreeList(File fileToCheck, List<File> treeList, FileFilter fileFilter, short toInclude, boolean excludeSubFolders, boolean includeHidden) {
            boolean isNewer = false;
            int numberOfNodes = 0;
            FileMonitorImpl.log_.debug(fileToCheck.getPath() + FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.currentNode"));
            File[] listOfChildren = null;
            isNewer = Commons.isNewer(fileToCheck, oldestInList_);
            if (isNewer || fileToCheck.isDirectory()) {
                if (fileToCheck.isDirectory()) {
                    FileMonitorImpl.log_.debug(fileToCheck.getPath() + FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.isFolder"));
                    if (isNewer && toInclude >= Utils.FOLDERS_ONLY) {
                        FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.foldersIncluded"));
                        if (!fileToCheck.isHidden() || includeHidden) {
                            FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.considerItem") + fileToCheck.getName());
                            treeList.add(fileToCheck);
                            numberOfNodes++;
                        }
                    }
                    listOfChildren = fileToCheck.listFiles(fileFilter);
                    for (File currentChild : listOfChildren) {
                        if (!currentChild.isDirectory() || !excludeSubFolders) {
                            numberOfNodes += getTreeList(currentChild, treeList, fileFilter, toInclude, excludeSubFolders, includeHidden);
                        }
                    }
                } else if (toInclude == Utils.FILES_ONLY || toInclude == Utils.FILES_AND_FOLDERS) {
                    if (!fileToCheck.isHidden() || includeHidden) {
                        FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.considerItem") + fileToCheck.getName());
                        treeList.add(fileToCheck);
                        numberOfNodes++;
                    }
                }
                if (FileMonitorImpl.treeTraversalInterval_ > 0) {
                    try {
                        threadMessageArguments_[1] = FileMonitorImpl.treeTraversalInterval_;
                        FileMonitorImpl.log_.debug(threadSleepingMessageFormatter_.format(threadMessageArguments_));
                        setFileMonitorThreadState(FileMonitorState.SLEEPING);
                        sleep(FileMonitorImpl.treeTraversalInterval_);
                        FileMonitorImpl.log_.debug(this.getName() + FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.thread.wokenUp"));
                    } catch (InterruptedException e) {
                        FileMonitorImpl.log_.error(this.getName() + FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.error.thread.interrupted"));
                        e.printStackTrace();
                    } finally {
                        setFileMonitorThreadState(FileMonitorState.RUNNING);
                    }
                }
            }
            return numberOfNodes;
        }

        /**
         *
         * Creates a List of feed entries from the {@code List} of {@code File}s
         * given. Sets the following attributes:<ul>
         * <li>Title           - File name</li>
         * <li>Link            - File path</li>
         * <li>Published Date  - Modified date for non-Windows systems or the higher
         *                       of the {@link File#lastModified()} and created date
         *                       for Windows</li>
         * <li>Category        - The tags for the feed</li>
         * <li>Description     - The feed item description.</li></ul>
         *
         * @param filesForEntries The {@code List} of {@code File}s from which the
         *                        feed entries will be created
         *
         * @param arrayOfCategories A {@code String} array of categories that should be set
         *                          for the entries
         *
         * @return The {@code List} of feed entries created
         *
         */
        private List<SyndEntry> createEntries(List<File> filesForEntries, String[] arrayOfCategories) throws NullPointerException {
            if (filesForEntries == null) throw new NullPointerException(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.exception.nullPointerException.listOfFiles"));
            List listOfEntries = new ArrayList<SyndEntry>();
            SyndEntry entry = null;
            SyndContent description = null;
            ArrayList<SyndCategory> categories = null;
            SyndCategory categoryImpl = null;
            String descriptionString = null;
            for (File currentFile : filesForEntries) {
                entry = new SyndEntryImpl();
                FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.property.currentFileName") + currentFile.getName());
                FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.property.currentFilePath") + currentFile.getPath());
                FileMonitorImpl.log_.debug(FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.log.debug.property.currentFileLastModified") + currentFile.lastModified());
                entry.setTitle(currentFile.getName());
                entry.setLink(currentFile.getPath());
                entry.setPublishedDate(new Date(currentFile.lastModified()));
                categories = new ArrayList<SyndCategory>();
                for (String category : arrayOfCategories) {
                    categoryImpl = new SyndCategoryImpl();
                    categoryImpl.setName(category);
                    categories.add(categoryImpl);
                }
                entry.setCategories(categories);
                descriptionString = FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.format.entryLocation") + "<a href=\"" + currentFile.getPath() + "\">" + currentFile.getPath() + "</a><br />" + FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.format.entryParent") + "<a href=\"" + currentFile.getParent() + "\">" + currentFile.getParent() + "</a><br />" + FileMonitorImpl.messages_.getString("fileMonitor.fileMonitorThread.format.entryMonitoredLocation") + "<a href=\"" + pathToMonitor_ + "\">" + pathToMonitor_ + "</a><br />";
                description = new SyndContentImpl();
                description.setType("text/html");
                description.setValue(descriptionString);
                entry.setDescription(description);
                listOfEntries.add(entry);
            }
            return listOfEntries;
        }

        /**
         *
         * Indicates to this thread that it should stop running
         * The thread checks for the flag at specific synchronization
         * points and may not stop running immediately on calling
         *
         * Use this to stop the current thread only. If you want to
         * indicate a global shutdown i.e. want all threads to finish,
         * use the flag in the Settings class
         *
         * @see com.nogoodatcoding.folder2feed.Settings#setAppExit(boolean)
         *
         */
        public void finish() {
            finish_ = true;
        }

        /**
         *
         * Forces a check the next time the thread runs
         * Use this when the thread seems to be skipping over
         * newer files or if the feed has fewer than {@code maxItems}
         * even when the monitored location contains more items
         *
         * Internally, this sets the comparsion timestamp to 0
         * so that all nodes appears new
         *
         */
        public void forceCheck() {
            oldestInList_ = 0;
        }

        /**
         *
         * Saves the current state of the {@code FileMonitorThread} in a variable
         * Also calls {@code setState()} on the {@code FileMonitorImpl}
         *
         * @param state The current {@code FileMonitorState}
         *
         */
        private void setFileMonitorThreadState(FileMonitorState state) {
            synchronized (fileMonitorThreadState_) {
                fileMonitorThreadState_ = state;
            }
            setState(state);
        }
    }
}
