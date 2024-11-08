package com.armatiek.infofuze.stream.filesystem.compression.bzip2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import com.armatiek.infofuze.config.Config;
import com.armatiek.infofuze.config.Definitions;
import com.armatiek.infofuze.stream.DeferredFileOutputStream;
import com.armatiek.infofuze.stream.filesystem.FileIf;
import com.armatiek.infofuze.stream.filesystem.ProxyFile;
import com.armatiek.infofuze.stream.filesystem.compression.ArchiveFile;

/**
 * An representation of a file entry within a BZIP2 archive file.
 * 
 * @author Maarten Kroon
 */
public class BZip2ArchiveFileEntry extends ProxyFile {

    protected DeferredFileOutputStream dfos;

    protected ArchiveFile archiveFile;

    protected InputStream is;

    public BZip2ArchiveFileEntry(ArchiveFile archiveFile, InputStream is) {
        super(archiveFile);
        this.is = is;
    }

    @Override
    public String getName() {
        return FilenameUtils.removeExtension(proxy.getName());
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
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isFile() {
        return true;
    }

    @Override
    public Iterator<FileIf> listFiles() {
        return null;
    }
}
