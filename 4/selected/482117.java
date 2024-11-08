package com.kokesoft.easywebdav;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.webdav.lib.Ace;
import org.apache.webdav.lib.Privilege;
import org.apache.webdav.lib.Property;
import org.apache.webdav.lib.PropertyName;
import org.apache.webdav.lib.ResponseEntity;
import org.apache.webdav.lib.WebdavResource;
import org.apache.webdav.lib.methods.DepthSupport;
import org.apache.webdav.lib.properties.AclProperty;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.IContributorResourceAdapter;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.jdom.output.EscapeStrategy;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import sun.security.action.GetIntegerAction;
import com.kokesoft.easywebdav.core.WebDAVFile;
import com.kokesoft.easywebdav.core.WebDAVResource;
import com.kokesoft.easywebdav.core.WebDAVTools;
import com.kokesoft.easywebdav.ui.ACLData;
import com.kokesoft.easywebdav.ui.PropertiesData;
import com.kokesoft.easywebdav.ui.VersionsData;

public class WebDAVFileStore extends FileStore {

    private static Logger logger = Logger.getLogger(Activator.PLUGIN_ID);

    private HttpURL uri;

    private String name;

    private String displayName;

    private long contentLength;

    private boolean collection;

    private WebDAVFileInfo finfo;

    private WebDAVFileStore parent;

    boolean root;

    String error = null;

    boolean invalidAuth = false;

    boolean invalidServer = false;

    private PropertiesData properties;

    private ACLData acl;

    private VersionsData versions;

    protected static String REFRESH_CONTENTS = "Refresh to see the contents";

    private static Thread thread;

    private static long startTime;

    private static boolean renameDisplayName = true;

    public static long maxTime = 5000;

    protected static Hashtable cache = new Hashtable();

    private static Vector roots = new Vector();

    private static Stack results = new Stack();

    public static List getRoots() {
        return roots;
    }

    public static String escapeXML(String str) {
        str = str.replaceAll("&", "&amp;");
        str = str.replaceAll("<", "&lt;");
        return str;
    }

    public static String escapeXMLAttr(String str) {
        str = str.replaceAll("&", "&amp;");
        str = str.replaceAll("<", "&lt;");
        str = str.replaceAll("\"", "&quot;");
        return str;
    }

    public static String unescapeXML(String str) {
        str = str.replaceAll("&amp;", "&");
        str = str.replaceAll("&lt;", "<");
        return str;
    }

    public static String unescapeXMLAttr(String str) {
        str = str.replaceAll("&amp;", "&");
        str = str.replaceAll("&lt;", "<");
        str = str.replaceAll("&quot;", "\"");
        return str;
    }

    public String getNoAuthUri() {
        if (this.uri == null) return null;
        String uri = this.uri.toString();
        uri = uri.replaceFirst("http://", "webdav://");
        if (isCollection() && !uri.endsWith("/")) uri += "/";
        return uri;
    }

    public String getUser() {
        try {
            return uri.getUser();
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) invalidAuth = true; else logger.log(Level.WARNING, e.toString(), e);
        } catch (URIException e) {
            logger.log(Level.WARNING, e.toString(), e);
        }
        return "";
    }

    public String getPassword() {
        try {
            return uri.getPassword();
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) invalidAuth = true; else logger.log(Level.WARNING, e.toString(), e);
        } catch (URIException e) {
            logger.log(Level.WARNING, e.toString(), e);
        }
        return "";
    }

    public String getUri() {
        if (this.uri == null) return null;
        String uri = this.uri.toString();
        uri = uri.replaceFirst("http://", "webdav://");
        try {
            if (this.uri.getUserinfo() != null && this.uri.getUserinfo().length() != 0) {
                uri = uri.substring(0, 9) + this.uri.getUserinfo() + "@" + uri.substring(9);
            }
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) invalidAuth = true; else logger.log(Level.WARNING, e.toString(), e);
        } catch (URIException e) {
            logger.log(Level.SEVERE, e.toString());
        }
        if (isCollection() && !uri.endsWith("/")) uri += "/";
        return uri;
    }

    public void setUri(String uri) throws HttpException, IOException {
        setUri(uri, true);
    }

    public void setUri(String uri, boolean getInfo) throws HttpException, IOException {
        try {
            String url = uri.replaceFirst("webdav://", "http://");
            this.uri = new HttpURL(url);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        try {
            if (getInfo) {
                finfo = new WebDAVFileInfo(uri);
                setCollection(finfo.isDirectory());
            }
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) invalidAuth = true; else if (e.getReasonCode() == 404) {
                finfo = new WebDAVFileInfo();
                finfo.setExists(false);
                setCollection(false);
            } else throw e;
        } catch (URIException e) {
            logger.log(Level.SEVERE, e.toString());
        } catch (IOException e) {
            throw e;
        }
    }

    public boolean isRoot() {
        return root;
    }

    public boolean isCollection() {
        return collection;
    }

    public void setCollection(boolean collection) {
        this.collection = collection;
    }

    public String getError() {
        return error;
    }

    private WebDAVFileStore() {
    }

    public void refresh() throws HttpException, IOException {
        if (uri == null) return;
        logger.log(Level.FINEST, uri.toString());
        try {
            String uri = getUri();
            removeCached(uri);
            this.uri = null;
            properties = null;
            acl = null;
            versions = null;
            finfo = null;
            WebDAVFileInfo.refresh(uri);
            setUri(uri);
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) invalidAuth = true; else throw e;
        }
    }

    public static WebDAVFileStore create(String uri) throws IOException {
        return create(uri, false, true);
    }

    public static WebDAVFileStore create(String uri, boolean isCollection, boolean getInfo) throws IOException {
        logger.log(Level.FINEST, uri);
        if (cache.containsKey(uri)) {
            WebDAVFileStore store = (WebDAVFileStore) cache.get(uri);
            logger.log(Level.FINEST, "cached: " + uri + " " + store.isRoot());
            try {
                store.refresh();
            } catch (HttpException e) {
                if (e.getReasonCode() == 401) store.invalidAuth = true; else throw e;
            } catch (ConnectException e) {
                store.invalidServer = true;
            } catch (UnknownHostException e) {
                store.invalidServer = true;
            }
            return store;
        }
        WebDAVFileStore store = new WebDAVFileStore();
        try {
            store.setUri(uri, getInfo);
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) store.invalidAuth = true; else throw e;
        } catch (ConnectException e) {
            store.invalidServer = true;
        } catch (UnknownHostException e) {
            store.invalidServer = true;
        }
        if (!getInfo) store.setCollection(isCollection);
        cache.put(uri, store);
        store.root = roots.contains(uri);
        return store;
    }

    public static void removeCached(String uri) {
        logger.log(Level.FINEST, "" + uri);
        Vector toberemoved = new Vector();
        for (Enumeration i = cache.keys(); i.hasMoreElements(); ) {
            String nuri = (String) i.nextElement();
            if (nuri.startsWith(uri) || nuri.equals(uri)) toberemoved.add(nuri);
        }
        for (Iterator i = toberemoved.iterator(); i.hasNext(); ) {
            Object key = i.next();
            cache.remove(key);
        }
    }

    public IFileInfo[] childInfos(int options, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "");
        WebDAVFileInfo finfo;
        IFileInfo[] children;
        IFileInfo[] empty = {};
        Date date = new Date();
        if (WebDAVFileStore.thread == Thread.currentThread()) {
            if (date.getTime() > (startTime + maxTime)) {
                logger.log(Level.FINEST, "childInfos: cancelled too long");
                children = new WebDAVFileInfo[1];
                finfo = new WebDAVFileInfo();
                finfo.setDirectory(false);
                finfo.setExists(true);
                finfo.setName(REFRESH_CONTENTS);
                children[0] = finfo;
                return children;
            }
        } else {
            WebDAVFileStore.thread = Thread.currentThread();
            startTime = date.getTime();
        }
        try {
            logger.log(Level.FINEST, "childInfos: " + options);
            WebdavResource resource = new WebdavResource(uri);
            resource.setDebug(1000);
            Vector vector = resource.listBasic();
            resource.close();
            String baseUri = getUri();
            logger.log(Level.FINEST, "childInfos.baseUri: " + baseUri);
            if (baseUri.charAt(baseUri.length() - 1) == '/') baseUri = baseUri.substring(0, baseUri.length() - 1);
            children = new WebDAVFileInfo[vector.size()];
            logger.log(Level.FINEST, "childInfos.length: " + vector.size());
            for (int c = 0; c < vector.size(); c++) {
                String[] data = (String[]) vector.get(c);
                finfo = new WebDAVFileInfo();
                finfo.setDisplayName(data[0]);
                finfo.setLength(Long.parseLong(data[1], 10));
                if ("COLLECTION".equals(data[2])) {
                    finfo.setDirectory(true);
                    finfo.setContentType((IContentType) null);
                } else {
                    finfo.setDirectory(false);
                    finfo.setContentType(data[2]);
                }
                finfo.setLastModified(DateFormat.getDateTimeInstance().parse(data[3]).getTime());
                finfo.internalSetUri(baseUri + "/" + data[4]);
                finfo.setExists(true);
                children[c] = finfo;
            }
        } catch (HttpException e) {
            children = empty;
            if (e.getReasonCode() == 401) invalidAuth = true; else logger.log(Level.WARNING, e.toString() + " (" + e.getReasonCode() + ")");
        } catch (IOException e) {
            children = empty;
            logger.log(Level.WARNING, e.toString());
        } catch (ParseException e) {
            children = empty;
            logger.log(Level.WARNING, e.toString());
        }
        return children;
    }

    public IFileStore[] childStores(int options, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "");
        WebDAVFileStore store;
        IFileStore[] children;
        IFileStore[] empty = {};
        String baseUri = getUri();
        if (baseUri.charAt(baseUri.length() - 1) == '/') baseUri = baseUri.substring(0, baseUri.length() - 1);
        try {
            logger.log(Level.FINEST, "baseUri: " + baseUri + "; " + options);
            WebdavResource resource = new WebdavResource(uri);
            resource.setDebug(1000);
            Vector resources = resource.listBasic();
            resource.close();
            children = new WebDAVFileStore[resources.size()];
            logger.log(Level.FINEST, "childStore.length: " + resources.size());
            for (int c = 0; c < children.length; c++) {
                String[] data = (String[]) resources.get(c);
                logger.log(Level.FINEST, "name: " + data[4]);
                WebDAVFileInfo wdfinfo = new WebDAVFileInfo(baseUri + "/" + data[4], data[0], "COLLECTION".equals(data[2]), "COLLECTION".equals(data[2]) ? null : data[2], Long.parseLong(data[1], 10));
                store = create(baseUri + "/" + data[4], wdfinfo.isDirectory(), false);
                store.name = data[4];
                store.parent = this;
                store.displayName = data[0];
                store.contentLength = Long.parseLong(data[1], 10);
                children[c] = store;
            }
        } catch (HttpException e) {
            children = empty;
            if (e.getReasonCode() == 401) invalidAuth = true; else logger.log(Level.WARNING, e.toString() + " (" + e.getReasonCode() + ")");
        } catch (URIException e) {
            children = empty;
            logger.log(Level.WARNING, e.toString());
        } catch (IOException e) {
            children = empty;
            logger.log(Level.WARNING, e.toString());
        }
        return children;
    }

    public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "");
        String[] children = {};
        if (uri != null) {
            try {
                logger.log(Level.FINEST, "childNames: " + options);
                WebdavResource resource = new WebdavResource(uri);
                resource.setDebug(1000);
                children = resource.list();
                resource.close();
                logger.log(Level.FINEST, "children.length: " + children.length);
            } catch (HttpException e) {
                if (e.getReasonCode() == 401) invalidAuth = true; else {
                    e.printStackTrace();
                    logger.log(Level.WARNING, e.toString() + " (" + e.getReasonCode() + ")");
                }
            } catch (IOException e) {
                e.printStackTrace();
                logger.log(Level.WARNING, e.toString());
            }
        }
        return children;
    }

    public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "");
        if (savingThread != null) {
            try {
                savingThread.join(1000);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "");
            }
        }
        try {
            if (finfo == null) finfo = new WebDAVFileInfo(getUri());
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) {
                invalidAuth = true;
                throw new CoreException(Activator.createErrorStatus(e));
            } else if (e.getReasonCode() == 404) {
                WebDAVFileInfo wdfinfo = new WebDAVFileInfo();
                wdfinfo.setExists(false);
                setCollection(false);
                finfo = wdfinfo;
            } else {
                invalidServer = true;
                throw new CoreException(Activator.createErrorStatus(e));
            }
        } catch (IOException e) {
            throw new CoreException(Activator.createErrorStatus(e));
        }
        return finfo;
    }

    public IFileStore getChild(String name) {
        try {
            logger.log(Level.FINEST, "");
            WebDAVFileStore result = new WebDAVFileStore();
            String newuri = getUri();
            if (newuri.charAt(newuri.length() - 1) == '/') newuri += name; else newuri += "/" + name;
            result.setUri(newuri);
            return result;
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) invalidAuth = true; else {
                e.printStackTrace();
                logger.log(Level.SEVERE, e.toString() + " (" + e.getReasonCode() + ")", e);
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, e.toString(), e);
        }
        return null;
    }

    public String getName() {
        if (name == null) {
            if (uri == null) {
                return null;
            }
            if (isRoot()) {
                name = uri.toString();
            } else {
                String parenturi = getUri();
                if (parenturi.charAt(parenturi.length() - 1) == '/') parenturi = parenturi.substring(parenturi.lastIndexOf('/', parenturi.length() - 2) + 1, parenturi.length() - 1); else parenturi = parenturi.substring(parenturi.lastIndexOf('/', parenturi.length() - 1) + 1);
                name = parenturi;
            }
        }
        logger.log(Level.FINEST, "" + name);
        return name;
    }

    public String getDisplayName() {
        if (displayName == null) {
            displayName = getName();
            PropertiesData props = getProperties();
            if (props != null) {
                Property property = props.getProperty("displayname", "DAV:");
                if (property != null) displayName = property.getPropertyAsString();
            }
        }
        logger.log(Level.FINEST, "" + displayName);
        return displayName;
    }

    public long getContentLength() {
        if (contentLength == -1) {
            PropertiesData props = getProperties();
            if (props != null) {
                Property property = props.getProperty("getcontentlength", "DAV:");
                if (property != null) contentLength = Long.parseLong(property.getPropertyAsString(), 10);
            }
        }
        logger.log(Level.FINEST, "" + contentLength);
        return contentLength;
    }

    public String getExtension() {
        String name = getName();
        if (name.lastIndexOf('.') != -1) return name.substring(name.lastIndexOf('.') + 1);
        return "";
    }

    public IFileStore getParent() {
        logger.log(Level.FINEST, "");
        if (uri == null) return null;
        if (isRoot() || roots.contains(getUri())) return null;
        if (parent != null) return parent;
        try {
            String upath = uri.getPath();
            if (upath.split("/").length > 2) {
                String parenturi = getUri();
                if (parenturi.charAt(parenturi.length() - 1) == '/') parenturi = parenturi.substring(0, parenturi.lastIndexOf('/', parenturi.length() - 2) + 1); else parenturi = parenturi.substring(0, parenturi.lastIndexOf('/', parenturi.length() - 1) + 1);
                logger.log(Level.FINEST, "parenturi: " + parenturi + " (" + uri.getPath() + ")");
                WebDAVFileStore result = create(parenturi);
                parent = result;
                return result;
            }
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) invalidAuth = true; else e.printStackTrace();
        } catch (URIException e) {
            e.printStackTrace();
        } catch (ConnectException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public WebDAVFileStore getRoot() {
        WebDAVFileStore r = this;
        while (r != null && !r.isRoot()) r = (WebDAVFileStore) r.getParent();
        return r;
    }

    public IFileSystem getFileSystem() {
        logger.log(Level.FINEST, "");
        try {
            return EFS.getFileSystem("webdav");
        } catch (CoreException e) {
            e.printStackTrace();
            return super.getFileSystem();
        }
    }

    public IFile getFile() {
        logger.log(Level.FINEST, "");
        String tpath = "";
        logger.log(Level.FINEST, "this: " + toURI() + " (" + isRoot() + ")");
        WebDAVFileStore root = getRoot();
        logger.log(Level.FINEST, "tpath " + tpath + "; root: " + root.getUri() + " (" + root.isRoot() + ")");
        IFile file = (IFile) WebDAVResource.create(this);
        return file;
    }

    protected void copyDirectory(IFileInfo sourceInfo, IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        internalMove(destination, (options & IResource.REPLACE) != 0, false, monitor);
    }

    public boolean exists() {
        if (finfo != null) return finfo.exists();
        return fetchInfo().exists();
    }

    private void internalMove(IFileStore destination, boolean overwrite, boolean deleteOriginal, IProgressMonitor monitor) throws CoreException {
        try {
            if (destination instanceof WebDAVFileStore) {
                WebDAVFileStore dstwdfs = (WebDAVFileStore) destination;
                logger.log(Level.FINEST, "" + getUri() + " -> " + dstwdfs.getUri() + "; move: " + deleteOriginal);
                String name = uri.getName();
                if (name.length() == 0) {
                    String[] names = uri.getPath().split("/");
                    for (int i = names.length - 1; i >= 0; i--) if (names[i].length() != 0) {
                        name = names[i];
                        break;
                    }
                }
                String absDestUri = dstwdfs.getUri();
                if (dstwdfs.isCollection()) absDestUri = WebDAVTools.join(dstwdfs.getUri(), name);
                logger.log(Level.FINEST, "destUri: " + absDestUri);
                WebDAVFileStore destStore = WebDAVFileStore.create(absDestUri);
                destStore.refresh();
                if (destStore.exists()) {
                    if (equals(destStore)) throw new CoreException(Activator.createErrorStatus(0, "The source and the destination are the same resource!", null));
                    if (overwrite) destStore.delete(0, monitor); else throw new CoreException(Activator.createErrorStatus(Activator.WILL_I_REPLACE, "", null));
                }
                logger.log(Level.FINEST, "Roots: " + dstwdfs.getRoot() + "; " + getRoot());
                logger.log(Level.FINEST, "Roots.equals: " + getRoot().equals(dstwdfs.getRoot()));
                if (getRoot().equals(dstwdfs.getRoot())) {
                    try {
                        String destUri = WebDAVTools.join(dstwdfs.toURI().getPath(), uri.getName());
                        WebdavResource r = new WebdavResource(new HttpURL(uri, ""));
                        if (deleteOriginal) {
                            if (!r.moveMethod(destUri)) logger.log(Level.FINEST, "Could not move: " + r.getStatusMessage()); else {
                                cache.remove(getUri());
                            }
                        } else {
                            if (!r.copyMethod(destUri)) logger.log(Level.FINEST, "Could not copy: " + r.getStatusMessage());
                        }
                    } catch (HttpException e) {
                        logger.log(Level.SEVERE, e.toString() + " " + getUri() + " (" + e.getReasonCode() + ")");
                        e.printStackTrace();
                        if (e.getReasonCode() == 401) invalidAuth = true; else throw new CoreException(Activator.createErrorStatus(e));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        if (isCollection()) {
                            IFileStore[] children = childStores(0, monitor);
                            if (children != null) {
                                dstwdfs.createCollection(getName());
                                String basePath = WebDAVTools.join(dstwdfs.getUri(), getName());
                                WebDAVFileStore destChild = create(basePath, true, false);
                                for (int c = 0; c < children.length; c++) {
                                    try {
                                        ((WebDAVFileStore) children[c]).internalMove(destChild, overwrite, deleteOriginal, monitor);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } else {
                            InputStream is = new BufferedInputStream(openInputStream(0, monitor));
                            File tmpfile = File.createTempFile("easywebdav", "");
                            OutputStream os = new BufferedOutputStream(new FileOutputStream(tmpfile));
                            byte[] buffer = new byte[1024];
                            while (true) {
                                int read = is.read(buffer);
                                if (read <= 0) break;
                                os.write(buffer, 0, read);
                            }
                            is.close();
                            os.close();
                            dstwdfs.createFile(getName(), tmpfile);
                            tmpfile.delete();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (URIException e) {
            e.printStackTrace();
            throw new CoreException(Activator.createErrorStatus(e));
        } catch (IOException e) {
            e.printStackTrace();
            throw new CoreException(Activator.createErrorStatus(e));
        }
    }

    public void copy(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        internalMove(destination, (options & IResource.REPLACE) != 0, false, monitor);
    }

    protected void copyFile(IFileInfo sourceInfo, IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        internalMove(destination, (options & IResource.REPLACE) != 0, false, monitor);
    }

    public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        WebDAVFileStore r = null;
        if (isCollection()) {
            String basename = name = "new-collection";
            int c = 1;
            while (getChild(name) != null) {
                name = basename + "-" + c;
                c++;
            }
            r = createCollection(name);
        }
        return r;
    }

    public void move(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "Hi!");
        internalMove(destination, (options & IResource.REPLACE) != 0, true, monitor);
    }

    public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "");
        if (uri == null) return new ByteArrayInputStream("".getBytes());
        try {
            WebdavResource resource = new WebdavResource(uri);
            return resource.getMethodData();
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) invalidAuth = true; else logger.log(Level.SEVERE, e.toString() + " (" + e.getReasonCode() + ")");
            return new ByteArrayInputStream(e.toString().getBytes());
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, e.toString());
            return new ByteArrayInputStream(e.toString().getBytes());
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString());
            return new ByteArrayInputStream(e.toString().getBytes());
        }
    }

    public URI toURI() {
        try {
            return new URI(getUri());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void delete(int options, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "");
        try {
            WebdavResource r = new WebdavResource(uri);
            if (!r.deleteMethod()) logger.log(Level.SEVERE, "The resource could not be deleted: " + r.getStatusMessage()); else {
                refresh();
            }
        } catch (HttpException e) {
            logger.log(Level.SEVERE, e.toString() + " " + getUri() + " (" + e.getReasonCode() + ")");
            if (e.getReasonCode() == 401) invalidAuth = true; else throw new CoreException(Activator.createErrorStatus(0, e.toString() + " (" + e.getReasonCode() + ")", e));
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString());
            throw new CoreException(Activator.createErrorStatus(0, e.toString(), e));
        }
    }

    private Thread savingThread = null;

    private String savingThreadError = null;

    public OutputStream openOutputStream(int options, IProgressMonitor monitor, long contentlength) throws CoreException {
        try {
            logger.log(Level.FINEST, "");
            PipedOutputStream os = new PipedOutputStream();
            final InputStream is = new PipedInputStream(os);
            final WebDAVFileStore finalThis = this;
            final long thislength = contentlength;
            savingThread = new Thread() {

                public void run() {
                    try {
                        synchronized (this) {
                            try {
                                wait(1000);
                            } catch (InterruptedException e) {
                                logger.log(Level.SEVERE, e.toString());
                            }
                        }
                        logger.log(Level.FINEST, "Opening resource");
                        WebdavResource resource = new WebdavResource(uri);
                        resource.putMethod(is, thislength);
                        resource.close();
                        logger.log(Level.FINEST, "Closing resource: status " + resource.getStatusCode());
                        if (resource.getStatusCode() < 200 || resource.getStatusCode() >= 300) {
                            finalThis.savingThreadError = "Could not save the document " + finalThis.getName() + ": error code " + resource.getStatusCode();
                            logger.log(Level.SEVERE, finalThis.savingThreadError);
                        } else {
                            WebDAVFileInfo.cache.remove(getUri());
                            finalThis.savingThreadError = null;
                        }
                    } catch (HttpException e) {
                        if (e.getReasonCode() == 401) invalidAuth = true; else {
                            finalThis.savingThreadError = e.toString() + " (" + e.getReasonCode() + ")";
                            logger.log(Level.SEVERE, finalThis.savingThreadError);
                        }
                    } catch (IOException e) {
                        finalThis.savingThreadError = e.toString();
                        logger.log(Level.SEVERE, finalThis.savingThreadError);
                    }
                    synchronized (this) {
                        notifyAll();
                    }
                    finalThis.savingThread = null;
                }
            };
            savingThread.start();
            return os;
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, e.toString());
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) invalidAuth = true; else logger.log(Level.SEVERE, e.toString() + " (" + e.getReasonCode() + ")");
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString());
        }
        return null;
    }

    public Thread getSavingThread() {
        return savingThread;
    }

    public String getSavingThreadError() {
        return savingThreadError;
    }

    public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
        return openOutputStream(options, monitor, -1);
    }

    public void putInfo(IFileInfo info, int options, IProgressMonitor monitor) throws CoreException {
        logger.log(Level.FINEST, "");
        if (savingThread != null) {
            try {
                savingThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.log(Level.FINEST, "putInfo");
        if (uri == null) return;
        try {
            WebdavResource resource = new WebdavResource(uri);
            Hashtable properties = new Hashtable();
            properties.put("getlastmodified", "" + info.getLastModified());
            logger.log(Level.FINEST, "" + info.getLastModified());
            properties.put("getcontentlength", "" + info.getLength());
            resource.proppatchMethod(properties, true);
            if (WebDAVFileInfo.cache.containsKey(getUri())) {
                WebDAVFileInfo finfo = (WebDAVFileInfo) WebDAVFileInfo.cache.get(getUri());
                finfo.setLastModified(info.getLastModified());
                finfo.setLength(info.getLength());
                finfo.setTime(new Date().getTime() + WebDAVFileInfo.CACHE_TIMEOUT);
            }
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) invalidAuth = true; else logger.log(Level.SEVERE, e.toString() + " (" + e.getReasonCode() + ")");
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString());
        }
    }

    protected void getWebDAVMetaInfo() {
        logger.log(Level.FINEST, "");
        if (uri == null) return;
        try {
            WebdavResource resource = new WebdavResource(uri);
            resource.setDebug(1000);
            properties = new PropertiesData(this);
            Enumeration renum = resource.propfindMethod(DepthSupport.DEPTH_0);
            if (renum != null) {
                while (renum.hasMoreElements()) {
                    ResponseEntity r = (ResponseEntity) renum.nextElement();
                    Enumeration penum = r.getProperties();
                    while (penum.hasMoreElements()) {
                        Property property = (Property) penum.nextElement();
                        logger.log(Level.FINEST, "property: " + property.getName());
                        if ("displayname".equals(property.getLocalName()) && "DAV:".equals(property.getNamespaceURI())) {
                            displayName = property.getPropertyAsString();
                        }
                        if ("getcontentlength".equals(property.getLocalName()) && "DAV:".equals(property.getNamespaceURI())) {
                            contentLength = Long.parseLong(property.getPropertyAsString(), 10);
                        }
                        properties.addProperty(property);
                    }
                }
            }
            acl = new ACLData();
            AclProperty aclProp = resource.aclfindMethod();
            if (aclProp != null) {
                Ace[] aces = aclProp.getAces();
                for (int i = 0; i < aces.length; i++) {
                    acl.addAce(aces[i]);
                }
            }
            versions = new VersionsData();
            resource.close();
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) invalidAuth = true; else logger.log(Level.SEVERE, e.toString() + " " + getUri() + " (" + e.getReasonCode() + ")");
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString());
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, e.toString());
        }
    }

    public boolean patchProperty(String name, String ns, String value) {
        logger.log(Level.FINEST, "");
        try {
            WebdavResource resource = new WebdavResource(uri);
            resource.setDebug(10);
            PropertyName pn = new PropertyName(ns, name);
            boolean r = resource.proppatchMethod(pn, value != null ? escapeXML(value) : "", value != null);
            resource.close();
            return r;
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) invalidAuth = true; else logger.log(Level.SEVERE, e.toString() + " (" + e.getReasonCode() + ")");
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString());
        }
        return false;
    }

    public boolean deleteProperty(String name, String ns) {
        return patchProperty(name, ns, null);
    }

    protected void simplifyPrivileges(Ace acea, Ace aceb) {
        for (Enumeration a = acea.enumeratePrivileges(); a.hasMoreElements(); ) {
            Privilege pa = (Privilege) a.nextElement();
            aceb.removePrivilege(pa);
        }
        for (Enumeration b = aceb.enumeratePrivileges(); b.hasMoreElements(); ) {
            Privilege pb = (Privilege) b.nextElement();
            acea.addPrivilege(pb);
        }
    }

    protected Ace[] simplifyAces(Ace[] aces) {
        List acesList = new Vector(Arrays.asList(aces));
        for (int i = 0; i < acesList.size(); i++) {
            Ace acea = (Ace) acesList.get(i);
            for (int j = i + 1; j < acesList.size(); j++) {
                Ace aceb = (Ace) acesList.get(j);
                if (acea.getPrincipal().equals(aceb.getPrincipal()) && acea.isNegative() == aceb.isNegative()) {
                    simplifyPrivileges(acea, aceb);
                    acesList.remove(j);
                    j--;
                }
            }
        }
        Ace[] naces = new Ace[acesList.size()];
        acesList.toArray(naces);
        return naces;
    }

    protected Ace[] modifyAces(Ace[] aces, String principal, String namespace, String name, boolean deny, boolean remove) {
        boolean tobeadded = true;
        for (int c = 0; c < aces.length; c++) {
            if (aces[c].getPrincipal().equals(principal)) {
                for (Enumeration privileges = aces[c].enumeratePrivileges(); privileges.hasMoreElements(); ) {
                    Privilege p = (Privilege) privileges.nextElement();
                    if (p.getNamespace().equals(namespace) && p.getName().equals(name)) {
                        if (aces[c].isNegative() != deny) {
                            if (deny) {
                                aces[c].removePrivilege(p);
                                tobeadded = false;
                            }
                        } else {
                            if (remove) aces[c].removePrivilege(p);
                            tobeadded = false;
                        }
                        break;
                    }
                }
                if (tobeadded) {
                    if (deny == aces[c].isNegative()) {
                        Privilege p = new Privilege(namespace, name, null);
                        aces[c].addPrivilege(p);
                        tobeadded = false;
                        break;
                    }
                }
            }
        }
        if (tobeadded) {
            Ace newAce = new Ace(principal, deny, false, false, null);
            Privilege p = new Privilege(namespace, name, null);
            newAce.addPrivilege(p);
            Ace[] newAces = new Ace[aces.length + 1];
            System.arraycopy(aces, 0, newAces, 0, aces.length);
            newAces[aces.length] = newAce;
            aces = newAces;
        }
        return aces;
    }

    public boolean modifyACL(String principal, String namespace, String name, boolean deny, boolean remove) {
        try {
            WebdavResource resource = new WebdavResource(uri);
            AclProperty aclProp = resource.aclfindMethod();
            Ace[] aces = aclProp.getAces();
            aces = simplifyAces(aces);
            aces = modifyAces(aces, principal, namespace, name, deny, remove);
            resource.setDebug(1000);
            boolean result = resource.aclMethod(uri.getPath(), aces);
            if (!result) logger.log(Level.SEVERE, "ACL failed: " + resource.getStatusMessage() + " (" + resource.getStatusCode() + ")");
            return result;
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) invalidAuth = true; else logger.log(Level.SEVERE, e.toString() + " (" + e.getReasonCode() + ")");
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString());
        }
        return false;
    }

    public boolean modifyACL(ACLData acl) {
        try {
            WebdavResource resource = new WebdavResource(uri);
            AclProperty aclProp = resource.aclfindMethod();
            Ace[] aces = aclProp.getAces();
            aces = simplifyAces(aces);
            boolean result = resource.aclMethod("", aces);
            if (!result) logger.log(Level.SEVERE, "ACL failed: " + resource.getStatusMessage() + " (" + resource.getStatusCode() + ")");
            return result;
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) invalidAuth = true; else logger.log(Level.SEVERE, e.toString() + " (" + e.getReasonCode() + ")");
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString());
        }
        return false;
    }

    public WebDAVFileStore createCollection(String name) {
        try {
            WebdavResource resource = new WebdavResource(uri);
            if (!resource.mkcolMethod(WebDAVTools.join(uri.getPath(), name))) {
                logger.log(Level.SEVERE, "The collection " + name + " could not be created at " + uri.getPath() + ": " + resource.getStatusMessage());
                return null;
            }
            WebDAVFileStore nwdfs = WebDAVFileStore.create(WebDAVTools.join(getUri(), name));
            return nwdfs;
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) invalidAuth = true; else logger.log(Level.SEVERE, e.toString() + " (" + e.getReasonCode() + ")");
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString());
        }
        return null;
    }

    /**
	 * Don't use it with FileInputStream: throws 411 error (length required). Use createFile(String name, File file) instead
	 * 
	 * @param name may be null: the name is obtained from the uri of the object
	 * @param is
	 * @return the new WebDAVFileStore created or null in error
	 */
    public WebDAVFileStore createFile(String name, InputStream is, long length) {
        logger.log(Level.FINEST, "" + name + " in " + getUri());
        try {
            WebdavResource resource = new WebdavResource(uri);
            if (is != null) {
                resource.setDebug(1000);
                if (!resource.putMethod(WebDAVTools.join(uri.getPath(), name), is, length)) {
                    logger.log(Level.SEVERE, "The file could not be created: " + resource.getStatusMessage());
                    return null;
                }
            } else {
                if (!resource.putMethod(WebDAVTools.join(uri.getPath(), name))) {
                    logger.log(Level.SEVERE, "The file could not be created: " + resource.getStatusMessage());
                    return null;
                }
            }
            WebDAVFileStore nwdfs = WebDAVFileStore.create(WebDAVTools.join(getUri(), name));
            return nwdfs;
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) invalidAuth = true; else logger.log(Level.SEVERE, e.toString() + " (" + e.getReasonCode() + ")");
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString());
        }
        return null;
    }

    public WebDAVFileStore createFile(String name, File file) {
        logger.log(Level.FINEST, "" + name + " in " + getUri());
        try {
            WebdavResource resource = new WebdavResource(uri);
            if (file != null) {
                resource.setDebug(1000);
                if (!resource.putMethod(WebDAVTools.join(uri.getPath(), name), file)) {
                    logger.log(Level.SEVERE, "The file could not be created: " + resource.getStatusMessage());
                    return null;
                }
            } else {
                if (!resource.putMethod(WebDAVTools.join(uri.getPath(), name))) {
                    logger.log(Level.SEVERE, "The file could not be created: " + resource.getStatusMessage());
                    return null;
                }
            }
            WebDAVFileStore nwdfs = WebDAVFileStore.create(WebDAVTools.join(getUri(), name));
            return nwdfs;
        } catch (HttpException e) {
            if (e.getReasonCode() == 401) invalidAuth = true; else logger.log(Level.SEVERE, e.toString() + " (" + e.getReasonCode() + ")");
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString());
        }
        return null;
    }

    public Object getAdapter(Class adapter) {
        if (adapter == IResource.class) {
            return WebDAVResource.create(this);
        } else if (adapter == IContributorResourceAdapter.class) {
            return WebDAVResource.create(this);
        } else if (adapter == ResourceMapping.class) {
            return WebDAVResourceMapping.create();
        } else {
            logger.log(Level.FINEST, "");
            logger.log(Level.FINEST, "  " + adapter.getName());
        }
        return super.getAdapter(adapter);
    }

    public PropertiesData getProperties() {
        logger.log(Level.FINEST, "");
        if (properties == null) getWebDAVMetaInfo();
        return properties;
    }

    public ACLData getACL() {
        logger.log(Level.FINEST, "");
        if (acl == null) getWebDAVMetaInfo();
        return acl;
    }

    public VersionsData getVersions() {
        logger.log(Level.FINEST, "");
        if (versions == null) getWebDAVMetaInfo();
        return versions;
    }

    public boolean equals(Object obj) {
        if (obj instanceof WebDAVFileStore && getUri().equals(((WebDAVFileStore) obj).getUri())) return true;
        return false;
    }

    public String toString() {
        return getName();
    }

    public boolean isValidAuth() {
        return !invalidAuth;
    }

    public boolean isValidServer() {
        return !invalidServer;
    }

    protected String getPropertiesAsXml() {
        String r = "";
        return r;
    }

    static List forbiddenProps = new Vector();

    {
        forbiddenProps.add("DAV::supportedlock");
        forbiddenProps.add("DAV::lockdiscovery");
        forbiddenProps.add("DAV::getetag");
        forbiddenProps.add("DAV::resourcetype");
    }

    public boolean exportResource(String destPath, IProgressMonitor monitor) throws CoreException {
        File fpdir = new File(destPath, ".properties");
        if (!fpdir.exists()) fpdir.mkdir();
        if (collection) destPath = new File(destPath, getName()).getAbsolutePath();
        File f = new File(destPath);
        if (!f.exists()) f.mkdirs();
        try {
            PropertiesData pd = getProperties();
            int numprops = pd.getPropertiesCount();
            if (numprops > 0) {
                Hashtable ns = new Hashtable();
                for (int c = 0; c < numprops; c++) {
                    Property p = pd.getProperty(c);
                    if (p.getNamespaceURI() != null && p.getNamespaceURI().length() != 0 && !ns.containsKey(p.getNamespaceURI())) {
                        String namespacePrefix = p.getName();
                        if (namespacePrefix.indexOf(":") != -1) namespacePrefix = namespacePrefix.substring(0, namespacePrefix.indexOf(":")); else namespacePrefix = "ns" + (ns.size() + 1);
                        ns.put(p.getNamespaceURI(), namespacePrefix);
                    }
                }
                File pf = new File(fpdir, getName());
                Writer w = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(pf)), "utf-8");
                w.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
                w.write("<properties");
                for (Enumeration e = ns.keys(); e.hasMoreElements(); ) {
                    Object key = e.nextElement();
                    w.write(" xmlns:" + ns.get(key) + "=\"" + key + "\"");
                }
                w.write(">\n");
                for (int c = 0; c < numprops; c++) {
                    Property p = pd.getProperty(c);
                    String pn = (p.getNamespaceURI() != null && p.getNamespaceURI().length() != 0 ? p.getNamespaceURI() + ":" : "") + p.getLocalName();
                    if (forbiddenProps.contains(pn)) continue;
                    if (p.getNamespaceURI() != null && p.getNamespaceURI().length() != 0) w.write("  <" + ns.get(p.getNamespaceURI()) + ":" + p.getLocalName() + ">"); else w.write("  <" + p.getLocalName() + ">");
                    w.write(p.getPropertyAsString());
                    if (p.getNamespaceURI() != null && p.getNamespaceURI().length() != 0) w.write("</" + ns.get(p.getNamespaceURI()) + ":" + p.getLocalName() + ">\n"); else w.write("</" + p.getLocalName() + ">\n");
                }
                w.write("</properties>\n");
                w.close();
            }
            if (collection) {
                IFileStore[] stores = childStores(0, monitor);
                for (int c = 0; c < stores.length; c++) {
                    if (!((WebDAVFileStore) stores[c]).exportResource(destPath, monitor)) return false;
                }
            } else {
                File cf = new File(f, getName());
                OutputStream os = new BufferedOutputStream(new FileOutputStream(cf));
                InputStream is = openInputStream(0, monitor);
                byte[] buffer = new byte[4096];
                while (true) {
                    int r = is.read(buffer);
                    if (r <= 0) break;
                    os.write(buffer, 0, r);
                }
                is.close();
                os.close();
            }
            return true;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new CoreException(Activator.createErrorStatus(e));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new CoreException(Activator.createErrorStatus(e));
        } catch (IOException e) {
            e.printStackTrace();
            throw new CoreException(Activator.createErrorStatus(e));
        } catch (Exception e) {
            e.printStackTrace();
            throw new CoreException(Activator.createErrorStatus(e));
        }
    }

    protected static class PropertiesHandler extends DefaultHandler {

        Hashtable properties = new Hashtable();

        String[] property = { null, null };

        int depth = 0;

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            depth++;
            if (depth == 1) return;
            if (uri == null) uri = "";
            if (property[0] == null) {
                property[0] = (uri.length() != 0 ? uri + ":" : "") + localName;
                property[1] = "";
            } else {
                property[1] += "<" + (uri.length() != 0 ? uri + ":" : "") + localName;
                for (int c = 0; c < attributes.getLength(); c++) {
                    uri = attributes.getURI(c);
                    if (uri == null) uri = "";
                    property[1] += " " + (uri.length() != 0 ? uri + ":" : "") + attributes.getLocalName(c) + "=\"" + attributes.getValue(c) + "\"";
                }
                property[1] += ">";
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            depth--;
            if (depth == 0) return;
            if (uri == null) uri = "";
            String rname = (uri.length() != 0 ? uri + ":" : "") + localName;
            if (property[0].equals(rname)) {
                if (!forbiddenProps.contains(property[0])) {
                    if (property[0].lastIndexOf(':') != -1) {
                        PropertyName pn = new PropertyName(property[0].substring(0, property[0].lastIndexOf(':')), property[0].substring(property[0].lastIndexOf(':') + 1));
                        properties.put(pn, property[1]);
                    } else properties.put(property[0], property[1]);
                } else logger.log(Level.FINEST, "Forbidden property: " + property[0]);
                property[0] = null;
                property[1] = null;
            } else {
                property[1] += "</" + (uri.length() != 0 ? uri + ":" : "") + localName + ">";
            }
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            if (property[1] != null) property[1] += new String(ch, start, length);
        }
    }

    public boolean importResource(String srcPath, IProgressMonitor monitor) throws CoreException {
        File f = new File(srcPath);
        if (!f.exists()) return false;
        try {
            if (f.isDirectory()) {
                if (collection && !getName().equals(f.getName())) {
                    return createCollection(f.getName()).importResource(srcPath, monitor);
                }
                if (!exists()) ((WebDAVFileStore) getParent()).createCollection(getName());
                if (!collection) return false;
                String[] children = f.list();
                for (int i = 0; i < children.length; i++) {
                    if (children[i].equals(".properties")) continue;
                    WebDAVFileStore childStore = WebDAVFileStore.create(WebDAVTools.join(getUri(), children[i]), false, false);
                    childStore.importResource(new File(srcPath, children[i]).toString(), monitor);
                }
            } else {
                File cf = new File(srcPath);
                InputStream is = new BufferedInputStream(new FileInputStream(cf));
                ((WebDAVFileStore) getParent()).createFile(getName(), is, cf.length());
                is.close();
            }
            File pfile = new File(new File(f.getParent(), ".properties"), f.getName());
            if (pfile.exists()) {
                XMLReader reader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
                InputStream fis = new BufferedInputStream(new FileInputStream(pfile));
                InputSource is = new InputSource(fis);
                PropertiesHandler ph = new PropertiesHandler();
                reader.setContentHandler(ph);
                reader.parse(is);
                fis.close();
                WebdavResource wr = new WebdavResource(uri);
                wr.setDebug(1000);
                logger.log(Level.FINEST, "patching multiple properties");
                if (!wr.proppatchMethod(uri.getPath(), ph.properties, true)) logger.log(Level.SEVERE, "Property patch failed: " + wr.getStatusMessage() + " (" + wr.getStatusCode() + ")");
                wr.close();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            throw new CoreException(Activator.createErrorStatus(e));
        } catch (SAXException e) {
            e.printStackTrace();
            throw new CoreException(Activator.createErrorStatus(e));
        } catch (Exception e) {
            e.printStackTrace();
            throw new CoreException(Activator.createErrorStatus(e));
        }
    }

    public boolean rename(String newName, IProgressMonitor monitor) throws CoreException {
        try {
            WebdavResource wr = new WebdavResource(uri);
            boolean setDisplayNameToo = wr.getName().equals(wr.getDisplayName());
            String tmpuri = uri.getPath();
            if (tmpuri.lastIndexOf('.') != -1 && tmpuri.lastIndexOf('.') > tmpuri.lastIndexOf('/')) {
                tmpuri = tmpuri.substring(0, tmpuri.lastIndexOf('.')) + Math.floor(Math.random() * 1000) + tmpuri.substring(tmpuri.lastIndexOf('.'));
            } else tmpuri += Math.floor(Math.random() * 1000);
            if (!wr.moveMethod(tmpuri)) {
                logger.log(Level.SEVERE, "Error moving " + getUri() + " to " + tmpuri + ": " + wr.getStatusMessage() + " (" + wr.getStatusCode() + ")");
                return false;
            }
            uri.setPath(tmpuri);
            wr = new WebdavResource(uri);
            String destinationUri = ((WebDAVFileStore) getParent()).uri.getPath() + "/" + newName;
            if (!wr.moveMethod(destinationUri)) {
                logger.log(Level.SEVERE, "Error moving " + tmpuri + " to " + destinationUri + ": " + wr.getStatusMessage() + " (" + wr.getStatusCode() + ")");
                return false;
            }
            if (renameDisplayName && setDisplayNameToo) {
                PropertyName pn = new PropertyName("DAV:", "displayname");
                wr.proppatchMethod(pn, newName, true);
            }
            return true;
        } catch (HttpException e) {
            e.printStackTrace();
            throw new CoreException(Activator.createErrorStatus(e));
        } catch (IOException e) {
            e.printStackTrace();
            throw new CoreException(Activator.createErrorStatus(e));
        } catch (Exception e) {
            e.printStackTrace();
            throw new CoreException(Activator.createErrorStatus(e));
        }
    }
}
