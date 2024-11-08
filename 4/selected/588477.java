package com.jiexplorer.filetask;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import org.apache.commons.io.FileUtils;
import com.jiexplorer.db.JIThumbnailService;

public class MoveFileTask extends CopyFileTask {

    public MoveFileTask(final File from, final File to) {
        super(from, to);
    }

    public MoveFileTask(final List<File> list, final File file) {
        super(list, file);
    }

    @Override
    public String getOperationName() {
        return "Move ";
    }

    @Override
    public void run() {
        final File file = getDestinationFolder();
        final ListIterator listiterator = getSourceFilesList().listIterator();
        while (listiterator.hasNext() && !isCancelled()) {
            final File from = (File) listiterator.next();
            final File to = new File(file, from.getName());
            setSource(from);
            setDestination(to);
            this.listener.fileTaskProgress(this);
            if (confirmAllOverride() && moveFile(from, to)) {
                this.performed++;
            }
            setOverallProgress(getOverallProgress() + 1L);
            this.listener.fileTaskProgress(this);
        }
        this.listener.fileTaskCompleted(this);
    }

    protected boolean moveFile(final File from, final File to) {
        boolean bool = false;
        if (confirmDelete(from)) {
            JIThumbnailService.getInstance().copyFile(from, to);
            JIThumbnailService.getInstance().removeFile(from);
            if (to.canWrite() && from.renameTo(to)) {
                bool = true;
            } else {
                try {
                    FileUtils.copyFile(from, to);
                } catch (final IOException e) {
                    e.printStackTrace();
                    return false;
                }
                bool = from.delete();
            }
        }
        return bool;
    }
}
