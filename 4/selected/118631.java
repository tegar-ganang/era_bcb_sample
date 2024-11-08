package de.jtdev.jfilenotify.polling;

import de.jtdev.jfilenotify.AbstractListenerGroup;
import de.jtdev.jfilenotify.FileNotifyConstants;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Listeners that listen to the same file or directory will be grouped by this 
 * class to prevent unessesary polling.
 * 
 * @author Tobias Oelgarte
 */
public class ListenerGroup extends AbstractListenerGroup implements Comparable {

    private LinkedList fileStatsList = null;

    private FileStats singleStats = null;

    private boolean watchesDirectory;

    public ListenerGroup(File cannonicalFile) {
        super(cannonicalFile);
        watchesDirectory = cannonicalFile.isDirectory();
    }

    private final void updateFileStats(FileStats s, File f, boolean storeFile, boolean storeDirectory, boolean storeLastModified, boolean storeAttributes) {
        if (storeFile) {
            s.file = f;
        }
        if (storeDirectory) {
            s.isDirectory = f.isDirectory();
        }
        if (storeLastModified) {
            s.lastModified = f.lastModified();
        }
        if (storeAttributes) {
            s.readable = f.canRead();
            s.writeable = f.canWrite();
            s.hidden = f.isHidden();
        }
    }

    private final boolean attributeHasChanged(FileStats s, File f) {
        return f.canRead() != s.readable || f.canWrite() != s.writeable || f.isHidden() != s.hidden;
    }

    /**
	 * This method compares the stored outline of the directory/file with the 
	 * current version and returns the events that are needed to inform 
	 * listeners about any modification. The outline itself is updated to the 
	 * current version and can be used for the next comparision. The events are 
	 * stored in the given events list. It returns false if the file/directory
	 * itself was deleted, and the Listenergroup is no longer valid, after this 
	 * operation.
	 */
    public boolean compareChanges(List events) {
        if (!canonicalFile.exists()) {
            events.add(new PollingEvent(FileNotifyConstants.SELF_DELETED, canonicalFile));
            return false;
        }
        if (!watchesDirectory) {
            if (canonicalFile.isDirectory()) {
                events.add(new PollingEvent(FileNotifyConstants.SELF_DELETED, canonicalFile));
                return false;
            }
            if (singleStats == null) {
                singleStats = new FileStats();
                updateFileStats(singleStats, canonicalFile, false, false, true, true);
            } else {
                if (singleStats.lastModified != canonicalFile.lastModified()) {
                    events.add(new PollingEvent(FileNotifyConstants.MODIFIED_EVENT, canonicalFile));
                    updateFileStats(singleStats, canonicalFile, false, false, true, false);
                }
                if (attributeHasChanged(singleStats, canonicalFile)) {
                    events.add(new PollingEvent(FileNotifyConstants.ATTRIBUTES_CHANGED_EVENT, canonicalFile));
                    updateFileStats(singleStats, canonicalFile, false, false, false, true);
                }
            }
        } else {
            if (!canonicalFile.isDirectory()) {
                events.add(new PollingEvent(FileNotifyConstants.SELF_DELETED, canonicalFile));
                return false;
            }
            if (fileStatsList == null) {
                singleStats = new FileStats();
                updateFileStats(singleStats, canonicalFile, true, false, true, true);
                fileStatsList = new LinkedList();
                File[] files = canonicalFile.listFiles();
                for (int i = 0; i < files.length; i++) {
                    File f = files[i];
                    FileStats stats = new FileStats();
                    updateFileStats(stats, f, true, true, true, true);
                    fileStatsList.add(stats);
                }
            } else {
                File[] files = canonicalFile.listFiles();
                LinkedList statsCopyList = new LinkedList(fileStatsList);
                for (int i = 0; i < files.length; i++) {
                    File f = files[i];
                    Iterator iter = statsCopyList.iterator();
                    boolean found = false;
                    while (iter.hasNext()) {
                        FileStats stats = (FileStats) iter.next();
                        if (f.equals(stats.file)) {
                            if (f.isDirectory() != stats.isDirectory) {
                                events.add(new PollingEvent(FileNotifyConstants.SUBFILE_DELETED_EVENT, f));
                                events.add(new PollingEvent(FileNotifyConstants.SUBFILE_CREATED_EVENT, f));
                                updateFileStats(stats, f, false, true, true, true);
                            } else {
                                if (!stats.isDirectory && f.lastModified() != stats.lastModified) {
                                    events.add(new PollingEvent(FileNotifyConstants.MODIFIED_EVENT, f));
                                    updateFileStats(stats, f, false, false, true, false);
                                }
                                if (attributeHasChanged(stats, f)) {
                                    events.add(new PollingEvent(FileNotifyConstants.ATTRIBUTES_CHANGED_EVENT, f));
                                    updateFileStats(stats, f, false, false, false, true);
                                }
                            }
                            iter.remove();
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        events.add(new PollingEvent(FileNotifyConstants.SUBFILE_CREATED_EVENT, f));
                        FileStats newStats = new FileStats();
                        updateFileStats(newStats, f, true, true, true, true);
                        fileStatsList.add(newStats);
                    }
                }
                Iterator iter = statsCopyList.iterator();
                while (iter.hasNext()) {
                    FileStats s = (FileStats) iter.next();
                    events.add(new PollingEvent(FileNotifyConstants.SUBFILE_DELETED_EVENT, s.file));
                    fileStatsList.remove(s);
                }
            }
        }
        return true;
    }

    public int compareTo(Object obj) {
        ListenerGroup group = (ListenerGroup) obj;
        return canonicalFile.compareTo(group.canonicalFile);
    }

    public boolean equals(Object obj) {
        if (obj instanceof ListenerGroup) {
            ListenerGroup g = (ListenerGroup) obj;
            return canonicalFile.equals(g.canonicalFile);
        }
        return false;
    }
}
