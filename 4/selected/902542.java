package com.jiexplorer.filetask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import org.apache.commons.io.FileUtils;
import com.jiexplorer.db.JIThumbnailService;
import com.jiexplorer.gui.preferences.JIPreferences;
import com.jiexplorer.util.JIUtility;

public class RenameFileTask extends CopyFileTask {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(RenameFileTask.class);

    private String prefix;

    private int padding;

    private int radix = 10;

    private String startAt;

    private int orderIndex;

    private String formatString;

    private boolean bySequence = true;

    private static final int SORT_BY_PATH = 4;

    private static final int SORT_BY_DATE = 3;

    private static final int SORT_BY_SIZE = 2;

    private static final int SORT_BY_TYPE = 1;

    private static final int SORT_BY_NAME = 0;

    private static final Vector<Comparator<File>> comparators = new Vector<Comparator<File>>(5);

    static {
        comparators.add(SORT_BY_NAME, new FileSortName());
        comparators.add(SORT_BY_TYPE, new FileSortType());
        comparators.add(SORT_BY_SIZE, new FileSortSize());
        comparators.add(SORT_BY_DATE, new FileSortDate());
        comparators.add(SORT_BY_PATH, new FileSortPath());
    }

    ;

    public RenameFileTask(final File from, final File to) {
        super(from, to);
    }

    public RenameFileTask(final List<File> list, final File to) {
        super(list, to);
    }

    @Override
    public String getOperationName() {
        return "Rename ";
    }

    @Override
    public void run() {
        if (isBySequence()) {
            bySequence();
        } else {
            byDate();
        }
    }

    public void bySequence() {
        try {
            Collections.sort(getSourceFilesList(), comparators.elementAt(this.orderIndex));
            ListIterator listiterator = getSourceFilesList().listIterator();
            final Vector<String> fileCleanUp = new Vector<String>();
            int sequence = Integer.parseInt(this.startAt, this.radix);
            while (listiterator.hasNext() && !isCancelled()) {
                final char[] increment = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
                File from = (File) listiterator.next();
                log.debug("From: " + from.getPath());
                final String suffix = JIUtility.suffix(from.getName());
                final String seqStr = JIUtility.getNumericPaddedString(sequence++, this.radix, this.padding);
                File to;
                int cnt = -1;
                do {
                    if (cnt < 0) {
                        to = new File(from.getParent(), this.prefix + seqStr + (suffix.length() > 0 ? "." + suffix : ""));
                    } else {
                        to = new File(from.getParent(), this.prefix + seqStr + increment[cnt] + (suffix.length() > 0 ? "." + suffix : ""));
                    }
                    ++cnt;
                } while (to.exists() && (cnt < increment.length));
                log.debug("To: " + to.getPath());
                setSource(from);
                setDestination(to);
                this.listener.fileTaskProgress(this);
                final String fromStr = from.getAbsolutePath();
                if (confirmAllOverride() && moveFile(from, to)) {
                    this.performed++;
                    from = null;
                    fileCleanUp.add(fromStr);
                }
                setOverallProgress(getOverallProgress() + 1L);
                this.listener.fileTaskProgress(this);
            }
            listiterator = null;
            for (final String remove : fileCleanUp) {
                final File removeFile = new File(remove);
                log.debug(removeFile.getPath() + " exists = " + removeFile.exists());
                if (removeFile.exists()) {
                    JIUtility.deleteFile(removeFile);
                }
            }
            this.listener.fileTaskCompleted(this);
        } catch (final NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public void byDate() {
        ListIterator listiterator = getSourceFilesList().listIterator();
        final Vector<String> fileCleanUp = new Vector<String>();
        final SimpleDateFormat format = new SimpleDateFormat(this.formatString);
        while (listiterator.hasNext() && !isCancelled()) {
            final char[] increment = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
            File from = (File) listiterator.next();
            log.debug("From: " + from.getPath());
            final String suffix = JIUtility.suffix(from.getName());
            final long datelong = from.lastModified();
            final Date date = new Date(datelong);
            final String dateStr = format.format(date);
            File to;
            int cnt = -1;
            do {
                if (cnt < 0) {
                    to = new File(from.getParent(), this.prefix + dateStr + (suffix.length() > 0 ? "." + suffix : ""));
                } else {
                    to = new File(from.getParent(), this.prefix + dateStr + increment[cnt] + (suffix.length() > 0 ? "." + suffix : ""));
                }
                ++cnt;
            } while (to.exists() && (cnt < increment.length));
            setSource(from);
            setDestination(to);
            log.debug("From: " + from.getPath() + "  To: " + to.getPath());
            this.listener.fileTaskProgress(this);
            final String cleanup = from.getAbsolutePath();
            if (confirmAllOverride() && moveFile(from, to)) {
                this.performed++;
                from = null;
                fileCleanUp.add(cleanup);
            }
            setOverallProgress(getOverallProgress() + 1L);
            this.listener.fileTaskProgress(this);
        }
        listiterator = null;
        for (final String remove : fileCleanUp) {
            final File removeFile = new File(remove);
            log.debug(removeFile.getPath() + " exists = " + removeFile.exists());
            if (removeFile.exists()) {
                JIUtility.deleteFile(removeFile);
            }
        }
        this.listener.fileTaskCompleted(this);
    }

    protected boolean moveFile(final File from, final File to) {
        boolean bool = false;
        if (confirmDelete(from)) {
            JIThumbnailService.getInstance().copyFile(from, to);
            JIThumbnailService.getInstance().removeFile(from);
            if (from.renameTo(to)) {
                log.debug("from.renameTo(to) == true");
                bool = true;
                JIUtility.deleteFile(from);
            } else {
                try {
                    FileUtils.copyFile(from, to);
                } catch (final IOException e) {
                    e.printStackTrace();
                    return false;
                }
                bool = true;
                log.debug("Files.copy(from, to) == true");
                JIUtility.deleteFile(from);
                log.debug(from.getPath() + " exists = " + from.exists());
            }
        }
        return bool;
    }

    /**
	 * @return the bySequence
	 */
    public final synchronized boolean isBySequence() {
        return this.bySequence;
    }

    /**
	 * @param bySequence the bySequence to set
	 */
    public final synchronized void setBySequence(final boolean bySequence) {
        this.bySequence = bySequence;
    }

    /**
	 * @param orderIndex the orderIndex to set
	 */
    public final synchronized void setOrderIndex(final int orderIndex) {
        this.orderIndex = orderIndex;
    }

    /**
	 * @param padding the padding to set
	 */
    public final synchronized void setPadding(final int padding) {
        this.padding = padding;
    }

    /**
	 * @param prefix the prefix to set
	 */
    public final synchronized void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    /**
	 * @param radix the radix to set
	 */
    public final synchronized void setRadix(final int radix) {
        this.radix = radix;
    }

    /**
	 * @param startAt the startAt to set
	 */
    public final synchronized void setStartAt(final String startAt) {
        this.startAt = startAt;
    }

    /**
	 * @return the format
	 */
    public final String getFormatString() {
        return this.formatString;
    }

    /**
	 * @param format the format to set
	 */
    public final void setFormatString(final String format) {
        this.formatString = format;
    }
}

class FileSortDate implements Comparator<File> {

    public int compare(final File a, final File b) {
        if (a == null) {
            return (JIPreferences.getInstance().isThumbnailSortDesend()) ? -1 : 1;
        }
        if (b == null) {
            return (JIPreferences.getInstance().isThumbnailSortDesend()) ? 1 : -1;
        }
        final int result = (!JIPreferences.getInstance().isThumbnailSortDesend()) ? (int) (a.lastModified() - b.lastModified()) : (int) (b.lastModified() - a.lastModified());
        return (result != 0) ? result : ((!JIPreferences.getInstance().isThumbnailSortDesend()) ? a.getAbsolutePath().compareTo(b.getAbsolutePath()) : b.getAbsolutePath().compareTo(a.getAbsolutePath()));
    }
}

class FileSortSize implements Comparator<File> {

    public int compare(final File a, final File b) {
        if (a == null) {
            return (JIPreferences.getInstance().isThumbnailSortDesend()) ? -1 : 1;
        }
        if (b == null) {
            return (JIPreferences.getInstance().isThumbnailSortDesend()) ? 1 : -1;
        }
        final int result = (!JIPreferences.getInstance().isThumbnailSortDesend()) ? (int) (a.length() - b.length()) : (int) (b.length() - a.length());
        return (result != 0) ? result : ((!JIPreferences.getInstance().isThumbnailSortDesend()) ? a.getAbsolutePath().compareTo(b.getAbsolutePath()) : b.getAbsolutePath().compareTo(a.getAbsolutePath()));
    }
}

class FileSortType implements Comparator<File> {

    public int compare(final File a, final File b) {
        if (a == null) {
            return (JIPreferences.getInstance().isThumbnailSortDesend()) ? -1 : 1;
        }
        if (b == null) {
            return (JIPreferences.getInstance().isThumbnailSortDesend()) ? 1 : -1;
        }
        return (!JIPreferences.getInstance().isThumbnailSortDesend()) ? a.getName().compareToIgnoreCase(b.getName()) : b.getName().compareToIgnoreCase(a.getName());
    }
}

class FileSortName implements Comparator<File> {

    public int compare(final File a, final File b) {
        if (a == null) {
            return (JIPreferences.getInstance().isThumbnailSortDesend()) ? -1 : 1;
        }
        if (b == null) {
            return (JIPreferences.getInstance().isThumbnailSortDesend()) ? 1 : -1;
        }
        return (!JIPreferences.getInstance().isThumbnailSortDesend()) ? a.getName().compareToIgnoreCase(b.getName()) : b.getName().compareToIgnoreCase(a.getName());
    }
}

class FileSortPath implements Comparator<File> {

    public int compare(final File a, final File b) {
        if (a == null) {
            return (JIPreferences.getInstance().isThumbnailSortDesend()) ? -1 : 1;
        }
        if (b == null) {
            return (JIPreferences.getInstance().isThumbnailSortDesend()) ? 1 : -1;
        }
        return (!JIPreferences.getInstance().isThumbnailSortDesend()) ? a.getAbsolutePath().compareTo(b.getAbsolutePath()) : b.getAbsolutePath().compareTo(a.getAbsolutePath());
    }
}
