package org.openthinclient.nfsd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import org.apache.log4j.Logger;
import org.openthinclient.mountd.NFSExport;
import org.openthinclient.nfsd.tea.fattr;
import org.openthinclient.nfsd.tea.ftype;
import org.openthinclient.nfsd.tea.nfs_fh;
import org.openthinclient.nfsd.tea.nfspath;
import org.openthinclient.nfsd.tea.nfstime;

/**
 * @author Joerg Henne
 */
class NFSFile {

    private static final Logger logger = Logger.getLogger(NFSFile.class);

    private final nfs_fh handle;

    private final File file;

    private NFSFile parentDirectory;

    private final NFSExport export;

    private FileChannel channel;

    private boolean channelIsRW;

    private fattr attributes;

    private nfspath linkDestination;

    public long lastAccess;

    NFSFile(nfs_fh handle, File file, NFSFile parentDirectory, NFSExport export) {
        this.file = file;
        this.handle = handle;
        this.parentDirectory = parentDirectory;
        this.export = export;
        updateTimestamp();
    }

    void updateTimestamp() {
        lastAccess = System.currentTimeMillis();
    }

    synchronized FileChannel getChannel(boolean rw) throws IOException {
        updateTimestamp();
        if (null != channel) {
            if (rw && !channelIsRW) {
                if (logger.isDebugEnabled()) logger.debug("Promoting channel for " + file + " to rw.");
                synchronized (channel) {
                    channel.close();
                }
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                channel = raf.getChannel();
            }
        } else {
            if (logger.isDebugEnabled()) logger.debug("Opening channel for " + file + ". rw=" + rw);
            RandomAccessFile raf = new RandomAccessFile(file, rw ? "rw" : "r");
            channel = raf.getChannel();
            channelIsRW = rw;
            CacheCleaner.registerDirtyFile(this);
        }
        return channel;
    }

    synchronized fattr getAttributes() throws FileNotFoundException {
        if (!file.exists()) {
            if (logger.isDebugEnabled()) logger.debug("File doesn't exist (anymore?) " + file);
            throw new FileNotFoundException(file.getPath());
        }
        updateTimestamp();
        if (null == attributes) {
            if (logger.isDebugEnabled()) logger.debug("Caching attributes " + file);
            attributes = new fattr();
            attributes.mode = 0;
            attributes.uid = 1000;
            attributes.gid = 100;
            attributes.size = (int) file.length();
            attributes.type = ftype.NFNON;
            if (file.isDirectory()) {
                attributes.type = ftype.NFDIR;
                attributes.mode |= NFSConstants.MISC_STDIR;
                if (attributes.size == 0) attributes.size = 1;
            } else if (file.getName().endsWith(NFSServer.SOFTLINK_TAG)) {
                attributes.type = ftype.NFLNK;
                attributes.mode |= NFSConstants.NFSMODE_LNK;
            } else {
                attributes.type = ftype.NFREG;
                attributes.mode |= NFSConstants.MISC_STFILE;
            }
            if (file.canRead()) attributes.mode |= NFSConstants.MISC_STREAD;
            if (file.canWrite()) attributes.mode |= NFSConstants.MISC_STWRITE;
            attributes.nlink = 1;
            attributes.blocksize = 1024;
            attributes.rdev = 19;
            if (file.length() != 0) attributes.blocks = (int) (1024 / file.length());
            attributes.fsid = 10;
            attributes.fileid = PathManager.handleToInt(handle);
            attributes.atime = new nfstime(file.lastModified());
            attributes.mtime = attributes.atime;
            attributes.ctime = attributes.atime;
            CacheCleaner.registerDirtyFile(this);
        }
        return attributes;
    }

    synchronized nfspath getLinkDestination() throws IOException {
        updateTimestamp();
        if (null == linkDestination) {
            BufferedReader r = new BufferedReader(new FileReader(file));
            String linkres = r.readLine();
            r.close();
            linkDestination = new nfspath(linkres);
            CacheCleaner.registerDirtyFile(this);
        }
        return linkDestination;
    }

    File getFile() {
        return file;
    }

    void flushCache() throws IOException {
        attributes = null;
        linkDestination = null;
        try {
            if (null != channel) channel.close();
        } finally {
            channel = null;
        }
    }

    NFSExport getExport() {
        return export;
    }

    NFSFile getParentDirectory() {
        return parentDirectory;
    }

    public long getLastAccessTimestamp() {
        return lastAccess;
    }

    public boolean isChannelOpen() {
        return channel != null;
    }

    public void flushCachedAttributes() {
        attributes = null;
    }

    boolean validateHandle(nfs_fh handle) {
        for (int i = 0; i < 8; i++) if (this.handle.data[i] != handle.data[i]) return false;
        return true;
    }

    public void setParentDirectory(NFSFile parentFile) {
        this.parentDirectory = parentFile;
    }

    public nfs_fh getHandle() {
        return handle;
    }
}
