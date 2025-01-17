package org.das2.util.filesystem;

import java.awt.EventQueue;
import java.util.logging.Level;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.NullProgressMonitor;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.*;
import java.util.logging.Logger;
import org.das2.util.Base64;
import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;
import org.das2.util.monitor.CancelledOperationException;

/**
 *
 * @author  Jeremy
 *
 * This is a refactoring of the HttpFileObject, generalized for use with FTP and HTTP file objects.  Note that
 * the HttpFileObject has not been refactored to use this.
 *
 */
public class WebFileObject extends FileObject {

    WebFileSystem wfs;

    String pathname;

    File localFile;

    Date modifiedDate;

    boolean isRoot;

    boolean isFolder;

    Map<String, String> metadata;

    /**
     * true if we know if it's a folder or not.
     */
    boolean isFolderResolved = false;

    public boolean canRead() {
        return true;
    }

    synchronized void maybeLoadMetadata() throws IOException {
        if (metadata == null) {
            metadata = wfs.protocol.getMetadata(this);
        }
    }

    public FileObject[] getChildren() throws IOException {
        if (!isFolder) {
            throw new IllegalArgumentException(toString() + "is not a folder");
        }
        String[] list = wfs.listDirectory(pathname);
        FileObject[] result = new FileObject[list.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = new WebFileObject(wfs, list[i], new Date(System.currentTimeMillis()));
        }
        return result;
    }

    public InputStream getInputStream(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        if (wfs.protocol != null) {
            FileSystem.logger.log(Level.FINE, "get inputstream from {0}", wfs.protocol);
            return wfs.protocol.getInputStream(this, monitor);
        }
        if (isFolder) {
            throw new IllegalArgumentException("is a folder");
        }
        if (!localFile.exists()) {
            File partFile = new File(localFile.toString() + ".part");
            wfs.downloadFile(pathname, localFile, partFile, monitor);
        }
        FileSystem.logger.log(Level.FINE, "read local file {0}", localFile);
        return new FileInputStream(localFile);
    }

    /**
     * 
     * @return a WebFileObject referencing the parent directory.
     */
    public FileObject getParent() {
        return new WebFileObject(wfs, wfs.getLocalName(localFile.getParentFile()), new Date(System.currentTimeMillis()));
    }

    public long getSize() {
        if (isFolder) {
            throw new IllegalArgumentException("is a folder");
        }
        try {
            maybeLoadMetadata();
        } catch (IOException ex) {
            System.err.println("unable to load metadata: " + ex);
            return localFile.length();
        }
        if (metadata.containsKey("Content-Length")) {
            return Long.parseLong(metadata.get("Content-Length"));
        } else {
            return localFile.length();
        }
    }

    public boolean isData() {
        return !this.isFolder;
    }

    public boolean isFolder() {
        if (this.isFolderResolved) {
            return this.isFolder;
        } else {
            throw new RuntimeException("IOException in constructor prevented us from resolving");
        }
    }

    public boolean isReadOnly() {
        return true;
    }

    public boolean isRoot() {
        return this.isRoot;
    }

    public java.util.Date lastModified() {
        return modifiedDate;
    }

    /**
     * returns the File that corresponds to the remote file.  This may or may
     * not exist, depending on whether it's been downloaded yet.
     */
    protected File getLocalFile() {
        return this.localFile;
    }

    public boolean exists() {
        if (localFile != null && localFile.exists()) {
            return true;
        } else {
            try {
                if (wfs.protocol != null) {
                    maybeLoadMetadata();
                    return "true".equals(metadata.get(WebProtocol.META_EXIST));
                } else {
                    Logger.getLogger("das2.filesystem").info("This implementation of WebFileObject.exists() is not optimal");
                    File partFile = new File(localFile.toString() + ".part");
                    wfs.downloadFile(pathname, localFile, partFile, new NullProgressMonitor());
                    return localFile.exists();
                }
            } catch (FileNotFoundException e) {
                return false;
            } catch (IOException e) {
                return false;
            }
        }
    }

    protected WebFileObject(WebFileSystem wfs, String pathname, Date modifiedDate) {
        this.modifiedDate = modifiedDate;
        this.wfs = wfs;
        this.pathname = pathname;
        this.isFolderResolved = false;
        if (!wfs.isAppletMode()) {
            this.localFile = new File(wfs.getLocalRoot(), pathname);
            if (FileSystem.settings().getPersistence() == FileSystemSettings.Persistence.SESSION) this.localFile.deleteOnExit();
            try {
                if (!localFile.canRead()) {
                    if (!(pathname.endsWith(".zip") || pathname.endsWith(".ZIP")) && wfs.isDirectory(pathname)) {
                        FileSystemUtil.maybeMkdirs(localFile);
                        this.isFolder = true;
                        if ("".equals(pathname)) {
                            this.isRoot = true;
                        }
                    } else {
                        this.isFolder = false;
                    }
                } else {
                    this.isFolder = localFile.isDirectory();
                    this.modifiedDate = new Date(localFile.lastModified());
                }
                this.isFolderResolved = true;
            } catch (IOException ex) {
                ex.printStackTrace();
                this.isFolderResolved = false;
            }
        }
    }

    public String toString() {
        return "[" + wfs + "]" + getNameExt();
    }

    public String getNameExt() {
        return pathname;
    }

    /**
     * return a Channel for the resource.  If the resource can be made locally available, a FileChannel is returned.
     * @param monitor
     * @return
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public java.nio.channels.ReadableByteChannel getChannel(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        InputStream in = getInputStream(monitor);
        return Channels.newChannel(in);
    }

    public File getFile(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        if (wfs.isAppletMode()) throw new SecurityException("getFile cannot be used with applets.");
        if (false) {
            if (EventQueue.isDispatchThread()) {
                System.err.println("download on event thread! " + this.getNameExt());
            }
        }
        boolean download = false;
        if (monitor == null) throw new NullPointerException("monitor may not be null");
        Date remoteDate;
        if (wfs instanceof HttpFileSystem) {
            URL url = wfs.getURL(this.getNameExt());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            String userInfo = null;
            try {
                userInfo = KeyChain.getDefault().getUserInfo(url);
            } catch (CancelledOperationException ex) {
                throw new FileSystemOfflineException("user cancelled credentials");
            }
            if (userInfo != null) {
                String encode = Base64.encodeBytes(userInfo.getBytes());
                connection.setRequestProperty("Authorization", "Basic " + encode);
            }
            try {
                connection.connect();
                remoteDate = new Date(connection.getLastModified());
            } catch (IOException ex) {
                if (!((HttpFileSystem) wfs).isOffline()) {
                    throw ex;
                } else {
                    remoteDate = new Date(0);
                }
            }
        } else {
            remoteDate = new Date(localFile.lastModified());
        }
        if (localFile.exists()) {
            Date localFileLastModified = new Date(localFile.lastModified());
            if (remoteDate.after(localFileLastModified)) {
                FileSystem.logger.log(Level.INFO, "remote file is newer than local copy of {0}, download.", this.getNameExt());
                download = true;
            }
        } else {
            download = true;
        }
        if (download) {
            try {
                FileSystem.logger.log(Level.FINE, "downloading file {0}", getNameExt());
                if (!localFile.getParentFile().exists()) {
                    FileSystemUtil.maybeMkdirs(localFile.getParentFile());
                }
                File partFile = new File(localFile.toString() + ".part");
                wfs.downloadFile(pathname, localFile, partFile, monitor);
                FileSystem.logger.log(Level.FINE, "downloaded local file has date {0}", new Date(localFile.lastModified()));
            } catch (FileNotFoundException e) {
                throw e;
            } catch (IOException ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("Forbidden")) {
                    throw ex;
                }
                if (this.wfs instanceof HttpFileSystem && !(ex instanceof InterruptedIOException)) {
                    if (this.wfs.isOffline()) {
                        ex.printStackTrace();
                        throw new FileSystem.FileSystemOfflineException("not found in local cache: " + getNameExt());
                    }
                }
                throw ex;
            } finally {
                monitor.finished();
            }
        }
        return localFile;
    }

    /**
     * returns true is the file is locally available, meaning clients can 
     * call getFile() and the readable File reference will be available in
     * interactive time.  For FileObjects from HttpFileSystem, a HEAD request
     * is made to ensure that the local file is as new as the website one.
     */
    public boolean isLocal() {
        if (wfs.isAppletMode()) return false;
        boolean download = false;
        if (localFile.exists()) {
            synchronized (wfs) {
                Date remoteDate;
                long localFileLastAccessed = wfs.getLastAccessed(this.getNameExt());
                if (System.currentTimeMillis() - localFileLastAccessed > 60000) {
                    try {
                        if (wfs instanceof HttpFileSystem) {
                            URL url = wfs.getURL(this.getNameExt());
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestMethod("HEAD");
                            connection.connect();
                            remoteDate = new Date(connection.getLastModified());
                        } else {
                            remoteDate = new Date(localFile.lastModified());
                        }
                        Date localFileLastModified = new Date(localFile.lastModified());
                        if (remoteDate.after(localFileLastModified)) {
                            FileSystem.logger.info("remote file is newer than local copy of " + this.getNameExt() + ", download.");
                            download = true;
                        }
                        wfs.markAccess(this.getNameExt());
                    } catch (IOException ex) {
                        return false;
                    }
                }
            }
        } else {
            download = true;
        }
        return !download;
    }
}
