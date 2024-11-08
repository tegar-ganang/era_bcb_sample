package com.armatiek.infofuze.stream.filesystem.compression;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.io.IOUtils;
import com.armatiek.infofuze.config.Config;
import com.armatiek.infofuze.config.Definitions;
import com.armatiek.infofuze.stream.DeferredFileOutputStream;
import com.armatiek.infofuze.stream.filesystem.AbstractFile;
import com.armatiek.infofuze.stream.filesystem.FileIf;

/**
 * An abstract representation of a file entry within an archive.
 * 
 * @author Maarten Kroon
 */
public abstract class ArchiveFileEntry extends AbstractFile {

    protected DeferredFileOutputStream dfos;

    protected ArchiveEntry entry;

    protected ArchiveFile archiveFile;

    protected InputStream is;

    public ArchiveFileEntry(ArchiveEntry entry, ArchiveFile archiveFile, InputStream is) {
        this.entry = entry;
        this.archiveFile = archiveFile;
        this.is = is;
    }

    public ArchiveEntry getArchiveEntry() {
        return entry;
    }

    public FileIf getContainerFile() {
        ArchiveFile archiveFile = (ArchiveFile) getParentFile();
        FileIf wrappedFile;
        while ((wrappedFile = archiveFile.getWrappedFile()) instanceof ArchiveFileEntry) {
            ArchiveFileEntry archiveFileEntry = (ArchiveFileEntry) wrappedFile;
            archiveFile = (ArchiveFile) archiveFileEntry.getParentFile();
        }
        return wrappedFile;
    }

    @Override
    public String getName() {
        return "/" + entry.getName();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (dfos == null) {
            int deferredOutputStreamThreshold = Config.getInstance().getDeferredOutputStreamThreshold();
            dfos = new DeferredFileOutputStream(deferredOutputStreamThreshold, Definitions.PROJECT_NAME, "." + Definitions.TMP_EXTENSION);
            try {
                IOUtils.copy(is, dfos);
            } finally {
                dfos.close();
            }
        }
        return dfos.getDeferredInputStream();
    }

    @Override
    public String getParent() {
        return archiveFile.getPath();
    }

    @Override
    public FileIf getParentFile() {
        return archiveFile;
    }

    @Override
    public String getPath() {
        return archiveFile.getPath() + "!" + entry.getName();
    }

    @Override
    public boolean isDirectory() {
        return entry.isDirectory();
    }

    @Override
    public boolean isFile() {
        return !entry.isDirectory();
    }

    @Override
    public long lastModified() {
        return entry.getLastModifiedDate().getTime();
    }

    @Override
    public long length() {
        return entry.getSize();
    }

    @Override
    public Iterator<FileIf> listFiles() {
        return new ArrayList<FileIf>().iterator();
    }
}
