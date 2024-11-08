package com.jiexplorer.rcp.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Image;
import com.jiexplorer.rcp.model.thumbnail.IThumbnailItem;
import com.jiexplorer.rcp.srv.ImageSrv;
import com.jiexplorer.rcp.srv.ThumbnailCacheService;

public class FileNode extends AbstractResourceNode implements IFileNode {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(FileNode.class);

    public static final FileNode[] NONE = new FileNode[0];

    public void rename(final File file) {
        try {
            final File ff = getFile();
            if (!ff.renameTo(file)) {
                FileUtils.copyFile(ff, file, true);
                deleteFile(ff);
            }
            update(file);
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteDeep(final IProgressMonitor m) {
        deleteDeep(m, true);
    }

    public void deleteDeep(final IProgressMonitor m, final boolean update) {
        if (m != null) {
            m.setTaskName("Deleting " + this.getAbsolutePath());
        }
        if (update) {
            parent.removeChild(this);
        }
        deleteFile(getFile());
        if (m != null) {
            m.worked(10);
        }
    }

    private static final void deleteFile(final File file) {
        deleteFile(0, file);
    }

    private static final void deleteFile(int count, final File file) {
        if (file.exists()) {
            if (count > 4) {
                file.deleteOnExit();
            } else if (!file.delete()) {
                System.gc();
                if (!file.delete()) {
                    try {
                        Thread.sleep(100);
                    } catch (final InterruptedException e) {
                    }
                    deleteFile(++count, file);
                }
            }
        }
    }

    @Override
    protected void createChildren() {
    }

    @Override
    public boolean hasChildren() {
        return false;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    public String getCRC32() {
        return null;
    }

    public Image getThumbnail(final IThumbnailItem item) {
        try {
            final Image image = item.getCachedImage(this);
            if (image != null) {
                return image;
            }
            return ThumbnailCacheService.getInstance().getThumbnailFor(this, item);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return ImageSrv.getInstance().getImage(ImageSrv.IMAGE_FILE);
    }
}
