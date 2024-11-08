package com.jiexplorer.filetask;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import org.apache.commons.io.FileUtils;
import com.jiexplorer.db.JIThumbnailService;

public class CopyFileTask extends FileTask {

    public CopyFileTask(final File file, final File destDir) {
        super(file, destDir);
    }

    public CopyFileTask(final List<File> list, final File file) {
        super(list, file);
    }

    @Override
    public String getOperationName() {
        return "Copy ";
    }

    public void run() {
        final File file = getDestinationFolder();
        final ListIterator listiterator = getSourceFilesList().listIterator();
        while (listiterator.hasNext() && !isCancelled()) {
            final File from = (File) listiterator.next();
            final File to = new File(file, from.getName());
            setSource(from);
            setDestination(to);
            this.listener.fileTaskProgress(this);
            if (confirmAllOverride() && copyFile(from, to)) {
                this.performed++;
            }
            setOverallProgress(getOverallProgress() + 1L);
            this.listener.fileTaskProgress(this);
        }
        this.listener.fileTaskCompleted(this);
    }

    protected boolean copyFile(final File from, final File to) {
        try {
            FileUtils.copyFile(from, to);
        } catch (final IOException e) {
            e.printStackTrace();
            return false;
        }
        JIThumbnailService.getInstance().copyFile(from, to);
        return true;
    }
}
