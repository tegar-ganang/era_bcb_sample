package org.dctmvfs.vfs.provider.dctm.client.impl.delegating;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.auth.StaticUserAuthenticator;
import org.apache.commons.vfs.impl.DefaultFileSystemConfigBuilder;
import org.dctmvfs.vfs.provider.dctm.client.DctmClient;
import org.dctmvfs.vfs.provider.dctm.client.DctmClientConnectionException;
import org.dctmvfs.vfs.provider.dctm.client.DctmClientException;
import org.dctmvfs.vfs.provider.dctm.client.DctmFile;
import org.dctmvfs.vfs.provider.dctm.client.DctmSessionManager;
import org.dctmvfs.vfs.provider.dctm.client.content.ContentCreator;
import org.dctmvfs.vfs.provider.dctm.client.content.ContentExporter;
import org.dctmvfs.vfs.provider.dctm.client.content.ContentUpdater;
import org.dctmvfs.vfs.provider.dctm.client.operations.Operations;
import org.dctmvfs.vfs.provider.dctm.client.operations.impl.DummyOperationsImpl;

/**
 * DctmClient implementation that uses another VFS implementation as its basis
 * @author kleij - at - users.sourceforge.net
 *
 */
public class DelegatingDctmClientImpl implements DctmClient {

    private static final Log log = LogFactory.getLog(DelegatingDctmClientImpl.class);

    private boolean isConnected = false;

    private String vfsRootPath;

    private String vfsType;

    private FileObject vfsRootFile = null;

    protected boolean readOnly = false;

    protected Operations operations = new DummyOperationsImpl();

    public DelegatingDctmClientImpl() {
        log.info("Created " + this.getClass().getName());
    }

    public void connect(char[] docbase, char[] username, char[] password, char[] domain) throws DctmClientConnectionException {
        String vfsRoot = this.vfsRootPath.replaceAll("\\$\\{docbase\\}", String.valueOf(docbase)).replaceAll("\\$\\{user\\}", String.valueOf(username)).replaceAll("\\$\\{password\\}", String.valueOf(password));
        try {
            String domainString = null;
            if (domain != null) {
                domainString = String.valueOf(domain);
            }
            StaticUserAuthenticator auth = new StaticUserAuthenticator(domainString, String.valueOf(username), String.valueOf(password));
            FileSystemOptions opts = new FileSystemOptions();
            DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
            FileObject virtualRootFile = VFS.getManager().resolveFile(vfsRoot, opts);
            if (vfsType.equals("virtual")) {
                this.vfsRootFile = VFS.getManager().createVirtualFileSystem(virtualRootFile);
            } else if (vfsType.equals("layered")) {
                this.vfsRootFile = VFS.getManager().createFileSystem(virtualRootFile);
            } else {
                this.vfsRootFile = virtualRootFile;
            }
            if (!this.vfsRootFile.exists()) {
                throw new DctmClientConnectionException("VFS root path " + vfsRoot + " does not exist");
            }
        } catch (FileSystemException e) {
            throw new DctmClientConnectionException(e);
        }
        log.info("Connected to docbase " + String.valueOf(docbase) + " with user " + String.valueOf(username));
        this.isConnected = true;
    }

    public void disconnect() {
        if (this.isConnected) {
            try {
                VFS.getManager().closeFileSystem(this.vfsRootFile.getFileSystem());
            } catch (FileSystemException e) {
                log.error("Error closing filesystem", e);
            }
            log.info("Disconnected");
            this.isConnected = false;
        }
    }

    public DctmFile findFile(String absPath) throws DctmClientException {
        try {
            FileObject file = this.vfsRootFile.resolveFile(absPath);
            if (file.exists()) {
                return new DelegatingDctmFileImpl(this, absPath, file);
            } else {
                return null;
            }
        } catch (FileSystemException e) {
            throw new DctmClientException(e);
        }
    }

    public DctmFile findFileWithId(String id) throws DctmClientException {
        String path = id.substring(0, id.lastIndexOf("-id"));
        return findFile(path);
    }

    public DctmFile[] getCabinets() throws DctmClientException {
        try {
            FileObject[] children = this.vfsRootFile.getChildren();
            DctmFile[] files = new DctmFile[children.length];
            for (int i = 0; i < children.length; i++) {
                files[i] = new DelegatingDctmFileImpl(this, children[i].getName().getPathDecoded(), children[i]);
            }
            return files;
        } catch (FileSystemException e) {
            throw new DctmClientException(e);
        }
    }

    public DctmFile[] listChildren(DctmFile parent) throws DctmClientException {
        try {
            DelegatingDctmFileImpl parentFile = (DelegatingDctmFileImpl) parent;
            FileObject[] children = parentFile.file.getChildren();
            DctmFile[] files = new DctmFile[children.length];
            for (int i = 0; i < children.length; i++) {
                files[i] = new DelegatingDctmFileImpl(this, children[i].getName().getPathDecoded(), children[i]);
            }
            return files;
        } catch (FileSystemException e) {
            throw new DctmClientException("Error getting children for object " + parent, e);
        }
    }

    public void createNewContent(String name, String location, File tmpFile) throws DctmClientException {
        if (this.isReadOnly()) {
            throw new DctmClientException("Writing is not allowed");
        }
        try {
            DelegatingDctmFileImpl parent = (DelegatingDctmFileImpl) this.findFile(location);
            FileObject newFile = parent.file.resolveFile(name);
            OutputStream outStream = newFile.getContent().getOutputStream();
            writeFileToStream(outStream, tmpFile);
        } catch (Exception e) {
            throw new DctmClientException("Error updating content " + name + " at " + location, e);
        }
    }

    public DctmFile createNewFolder(String name, String location) throws DctmClientException {
        if (this.isReadOnly()) {
            throw new DctmClientException("Writing is not allowed");
        }
        try {
            DelegatingDctmFileImpl parent = (DelegatingDctmFileImpl) this.findFile(location);
            FileObject newFile = parent.file.resolveFile(name);
            newFile.createFolder();
            return new DelegatingDctmFileImpl(this, newFile.getName().getPathDecoded(), newFile);
        } catch (FileSystemException e) {
            throw new DctmClientException("Error creating new folder " + name + " at " + location, e);
        }
    }

    public String resolvePath(String path) {
        return path;
    }

    public void setVfsRootPath(String rootPath) {
        this.vfsRootPath = rootPath;
    }

    public void setVfsType(String vfsType) {
        this.vfsType = vfsType;
    }

    public void setSessionManager(DctmSessionManager sessionManager) {
    }

    public DctmSessionManager getSessionManager() {
        return null;
    }

    public void setContentCreator(ContentCreator creator) {
    }

    public void setContentExporter(ContentExporter exporter) {
    }

    public void setContentUpdater(ContentUpdater updater) {
    }

    protected void writeFileToStream(OutputStream outStream, File tmpFile) throws IOException {
        FileInputStream inStream = null;
        try {
            inStream = new FileInputStream(tmpFile);
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int read = -1;
            while ((read = inStream.read(buffer)) > -1) {
                outStream.write(buffer, 0, read);
            }
        } finally {
            try {
                if (outStream != null) outStream.close();
            } catch (IOException e) {
                log.warn("Error closing outputstream", e);
            }
            try {
                if (inStream != null) inStream.close();
            } catch (IOException e) {
                log.warn("Error closing inputstream", e);
            }
        }
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Operations getOperations() {
        return this.operations;
    }

    public void setOperations(Operations operations) {
        this.operations = operations;
    }
}
