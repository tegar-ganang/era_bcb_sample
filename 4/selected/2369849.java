package com.kokesoft.easywebdav.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.IDescriptionProvider;
import com.kokesoft.easywebdav.Activator;
import com.kokesoft.easywebdav.WebDAVFileStore;

public class WebDAVFile extends WebDAVResource implements IFile {

    private static Logger logger = Logger.getLogger(Activator.PLUGIN_ID);

    IContentDescription description;

    String charset;

    protected WebDAVFile() {
        super();
    }

    protected WebDAVFile(WebDAVFileStore store) {
        super(store);
    }

    public void appendContents(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
    }

    public void appendContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
    }

    public void create(InputStream source, boolean force, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
    }

    public void create(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
    }

    public void createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
    }

    public void createLink(URI location, int updateFlags, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
    }

    public void delete(boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
    }

    public String getCharset() throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        return getCharset(false);
    }

    public String getCharset(boolean checkImplicit) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        if (charset != null) return charset;
        if (checkImplicit) {
            IContentDescription description = getContentDescription();
            return description.getCharset();
        }
        return null;
    }

    public String getCharsetFor(Reader reader) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        try {
            IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
            IContentDescription description;
            description = contentTypeManager.getDescriptionFor(reader, getName(), IContentDescription.ALL);
            return description.getCharset();
        } catch (IOException e) {
            throw new CoreException(Activator.createErrorStatus(e));
        }
    }

    public IContentDescription getContentDescription() throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        if (description != null) return description;
        try {
            IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
            InputStream is = getStore().openInputStream(0, null);
            description = contentTypeManager.getDescriptionFor(is, getName(), IContentDescription.ALL);
            is.close();
            return description;
        } catch (IOException e) {
            throw new CoreException(Activator.createErrorStatus(e));
        }
    }

    public InputStream getContents() throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        return getStore().openInputStream(0, null);
    }

    public InputStream getContents(boolean force) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        return getStore().openInputStream(0, null);
    }

    public int getEncoding() throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        if (getCharset().equals("UTF-8")) return IFile.ENCODING_UTF_8;
        return IFile.ENCODING_UNKNOWN;
    }

    public IFileState[] getHistory(IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        IFileState[] empty = {};
        return empty;
    }

    public void move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
    }

    public void setCharset(String newCharset) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        setCharset(newCharset, null);
    }

    public void setCharset(String newCharset, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        this.charset = newCharset;
    }

    public void setContents(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        setContents(source, (updateFlags & IFile.FORCE) != 0, (updateFlags & IFile.KEEP_HISTORY) != 0, monitor);
    }

    public void setContents(IFileState source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        setContents(source.getContents(), updateFlags, monitor);
    }

    public void setContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        try {
            logger.log(Level.FINEST, "Hi!");
            OutputStream os = getStore().openOutputStream(0, null);
            byte[] buffer = new byte[1024];
            while (true) {
                int read = source.read(buffer);
                if (read != 0) os.write(buffer, 0, read); else break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new CoreException(Activator.createErrorStatus(e));
        }
    }

    public void setContents(IFileState source, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        setContents(source.getContents(), force, keepHistory, monitor);
    }

    public int getType() {
        logger.log(Level.FINEST, "WebDAVResource.getType");
        return FILE;
    }

    public Object getAdapter(Class adapter) {
        logger.log(Level.FINEST, adapter.getName() + " (" + getStore().getUri() + ")");
        if (IFile.class == adapter) return this;
        return super.getAdapter(adapter);
    }
}
