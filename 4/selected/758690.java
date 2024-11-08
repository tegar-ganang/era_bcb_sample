package fi.hip.gb.disk.transport.webdav;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.transaction.util.LoggerFacade;
import org.apache.slide.common.Service;
import org.apache.slide.common.ServiceAccessException;
import org.apache.slide.common.ServiceParameterErrorException;
import org.apache.slide.common.ServiceParameterMissingException;
import org.apache.slide.security.AccessDeniedException;
import org.apache.slide.security.UnauthenticatedException;
import org.apache.slide.simple.store.BasicWebdavStore;
import org.apache.slide.simple.store.WebdavStoreAdapter;
import org.apache.slide.simple.store.WebdavStoreBulkPropertyExtension;
import org.apache.slide.simple.store.WebdavStoreLockExtension;
import org.apache.slide.simple.store.WebdavStoreMacroCopyExtension;
import org.apache.slide.simple.store.WebdavStoreMacroDeleteExtension;
import org.apache.slide.simple.store.WebdavStoreMacroMoveExtension;
import org.apache.slide.store.util.FileHelper;
import org.apache.slide.structure.ObjectAlreadyExistsException;
import org.apache.slide.structure.ObjectNotFoundException;
import fi.hip.gb.disk.FileManager;
import fi.hip.gb.disk.conf.Config;
import fi.hip.gb.disk.info.FileInfo;

/**
 * WebDav implementation of the GB-DISK storage interface.
 * Forked from Jakarta Slide Wck reference implementation of the
 * {@link org.apache.slide.simple.store.BasicWebdavStore} to
 * support virtual view for GB-DISK.
 * <p>
 * The {@link WebdavDiskStore#WEBDAV_FILES_PATH} should match
 * the files folder defined in Domain.xml files.
 * 
 * <p>
 * WebdavFileStore needs to be deployed with implementations of at a least a
 * SecurityStore and optionally with a LockStore. A sample Domain.xml entry
 * looks like: <br>
 * 
 * <pre>
 * 
 *  
 *   
 *         &lt;store name=&quot;simple&quot;&gt;
 *             &lt;parameter name=&quot;cache-mode&quot;&gt;cluster&lt;/parameter&gt;
 *             &lt;nodestore classname=&quot;org.apache.slide.store.simple.WebdavStoreAdapter&quot;&gt;
 *                &lt;parameter name=&quot;callback-store&quot;&gt;org.apache.slide.store.simple.WebdavFileStore&lt;/parameter&gt;
 *                &lt;parameter name=&quot;rootpath&quot;&gt;c:/tmp&lt;/parameter&gt;
 *             &lt;/nodestore&gt;
 *             &lt;contentstore&gt;
 *               &lt;reference store=&quot;nodestore&quot;/&gt;
 *             &lt;/contentstore&gt;
 *             &lt;revisiondescriptorsstore&gt;
 *               &lt;reference store=&quot;nodestore&quot;/&gt;
 *             &lt;/revisiondescriptorsstore&gt;
 *             &lt;revisiondescriptorstore&gt;
 *               &lt;reference store=&quot;nodestore&quot;/&gt;
 *             &lt;/revisiondescriptorstore&gt;
 *             &lt;!-- comment this out when you want to use the locking from the memory store --&gt; 
 *             &lt;lockstore&gt;
 *               &lt;reference store=&quot;nodestore&quot;/&gt;
 *             &lt;/lockstore&gt;
 *             &lt;securitystore classname=&quot;org.apache.slide.store.mem.TransientSecurityStore&quot;/&gt;
 *             &lt;!-- uncomment this when you want to use the the locking from the memory store --&gt; 
 *             &lt;!--lockstore classname=&quot;org.apache.slide.store.mem.TransientLockStore&quot;/--&gt;
 *         &lt;/store&gt;
 *         &lt;scope match=&quot;/files&quot; store=&quot;simple&quot;/&gt;
 *    
 *   
 *  
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * Caching mode should be set to "cluster" when you want every change in the
 * file system to be immedeately displayed in Slide. This is practical as this
 * store tends to be rather fast.
 * </p>
 * 
 * <p>
 * When you import data from Domain.xml like the file folder Slide gives you no
 * hint upon what kind of object it will be. Even more it stores content even
 * for folders pointing you in the wrong direction. As this implementation
 * relies on information if the stored object will be a folder or resource with
 * content imported data from Domain.xml must be augmented with properties that
 * indicate the type. E.g. for correct creation of the files object as a folder
 * the above configuration would require an entry like
 * 
 * <pre>
 * 
 *  
 *   
 *                    &lt;objectnode classname=&quot;org.apache.slide.structure.SubjectNode&quot; uri=&quot;/files&quot;&gt;
 *                           &lt;revision&gt;
 *                              &lt;property name=&quot;resourcetype&quot;&gt;&lt;![CDATA[&lt;collection/&gt;]]&gt;&lt;/property&gt;
 *                          &lt;/revision&gt;
 *                          ....
 *    
 *   
 *  
 * </pre>
 * 
 * instead of
 * 
 * <pre>
 * 
 *  
 *   
 *                    &lt;objectnode classname=&quot;org.apache.slide.structure.SubjectNode&quot; uri=&quot;/files&quot;&gt;
 *                          ....
 *    
 *   
 *  
 * </pre>
 * 
 * </p>
 * 
 * @author Juho Karppinen
 * @see BasicWebdavStore
 * @see WebdavStoreLockExtension
 * @see WebdavStoreBulkPropertyExtension
 * @see WebdavStoreAdapter
 */
public class WebdavDiskStore implements BasicWebdavStore, WebdavStoreLockExtension, WebdavStoreBulkPropertyExtension, WebdavStoreMacroCopyExtension, WebdavStoreMacroMoveExtension, WebdavStoreMacroDeleteExtension {

    /** path under Silo-dir where WebDAV server stores its files, must match the filespath argument in Domain.xml */
    public static final String WEBDAV_FILES_PATH = "/files";

    private static final String LOCK_FILE_EXTENSION = ".lck";

    private static final String ERR_FILE_EXTENSION = ".err.txt";

    private static final String PROPERTY_FILE_PREFIX = "...___...";

    /** normal logger */
    private static Log log = LogFactory.getLog(WebdavDiskStore.class);

    private static void save(InputStream is, File file) throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
        try {
            FileHelper.copy(is, os);
        } finally {
            try {
                is.close();
            } finally {
                os.close();
            }
        }
    }

    private static Service service = null;

    public synchronized void begin(Service service, Principal principal, Object connection, LoggerFacade logger, Hashtable parameters) throws ServiceAccessException, ServiceParameterErrorException, ServiceParameterMissingException {
        if (WebdavDiskStore.service == null) {
            WebdavDiskStore.service = service;
            log.info("Webdav starting as coordinator=" + Config.isFrontEnd() + " on directory " + Config.getSiloDir());
            File storage = new File(Config.getSiloDir());
            if (storage.exists() == false) {
                if (storage.mkdirs() == false) {
                    log.error("Could not create webdav storage folder under the Silo-directory: " + storage.getPath());
                } else {
                    if (new File(storage, "private").mkdir() == false) {
                        log.error("Could not create private section for webdav-directory: " + storage.getPath());
                    }
                }
            }
            if (Config.isFrontEnd() && getDiskFile("/private") == null) {
                log.info("creating private folder in the info system");
                Config.getFileInfosys().createFolder("/private");
            }
            log.info("WebDAV local storage is " + storage.getPath() + " exists=" + storage.exists());
        }
    }

    public void checkAuthentication() throws UnauthenticatedException {
    }

    public void commit() throws ServiceAccessException {
    }

    public void rollback() throws ServiceAccessException {
        log.debug("rollback");
    }

    public void macroCopy(String sourceUri, String targetUri, boolean overwrite, boolean recursive) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException, ObjectAlreadyExistsException {
        log.info("copying " + sourceUri + " to " + targetUri);
        try {
            File fromFile = getFile(sourceUri);
            File toFile = getFile(targetUri);
            if (toFile.exists() && !overwrite) {
                throw new ObjectAlreadyExistsException(targetUri);
            }
            if (!toFile.getParentFile().exists()) {
                throw new ObjectNotFoundException(toFile.getParentFile().toString());
            }
            if (fromFile.isDirectory() && !recursive) {
                if (!toFile.exists()) {
                    toFile.mkdirs();
                }
            } else {
                FileHelper.copyRec(fromFile, toFile);
            }
            File propertyFile = getPropertyFile(sourceUri);
            File destPropertyFile = getPropertyFile(targetUri);
            if (propertyFile.exists()) FileHelper.copy(propertyFile, destPropertyFile);
        } catch (FileNotFoundException e) {
            throw new ObjectNotFoundException(targetUri);
        } catch (IOException e) {
            throw new ServiceAccessException(service, e);
        } catch (SecurityException e) {
            throw new AccessDeniedException(targetUri, e.getMessage(), "/actions/write");
        }
    }

    public void macroMove(String sourceUri, String targetUri, boolean overwrite) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException, ObjectAlreadyExistsException {
        log.info("moving " + sourceUri + " to " + targetUri);
        try {
            File fromFile = getFile(sourceUri);
            File toFile = getFile(targetUri);
            if (toFile.exists() && !overwrite) {
                throw new ObjectAlreadyExistsException(targetUri);
            }
            if (!toFile.getParentFile().exists()) {
                throw new ObjectNotFoundException(toFile.getParentFile().toString());
            }
            if (Config.isFrontEnd()) {
                Config.getFileInfosys().rename(getDiskFilePath(sourceUri), getDiskFilePath(targetUri));
            }
            renameOrMove(fromFile, toFile);
            File propertyFile = getPropertyFile(sourceUri);
            File destPropertyFile = getPropertyFile(targetUri);
            renameOrMove(propertyFile, destPropertyFile);
            File lockFile = getLockFile(sourceUri);
            File destLockFile = getLockFile(targetUri);
            renameOrMove(lockFile, destLockFile);
            File errFile = getErrFile(sourceUri);
            File destErrFile = getErrFile(targetUri);
            renameOrMove(errFile, destErrFile);
        } catch (FileNotFoundException e) {
            throw new ObjectNotFoundException(targetUri);
        } catch (IOException e) {
            throw new ServiceAccessException(service, e);
        } catch (SecurityException e) {
            throw new AccessDeniedException(targetUri, e.getMessage(), "/actions/write");
        }
    }

    protected void renameOrMove(File from, File to) throws IOException, ObjectAlreadyExistsException {
        if (from.exists()) {
            if (to.exists()) {
                boolean success = to.delete();
                if (!success) {
                    new ObjectAlreadyExistsException(to.toString());
                }
            }
            boolean success = from.renameTo(to);
            if (!success) {
                FileHelper.moveRec(from, to);
            }
        }
    }

    public void macroDelete(String targetUri) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException {
        log.info("deleting " + targetUri);
        try {
            File file = getFile(targetUri);
            if (Config.isFrontEnd()) {
                if (isErrFilename(targetUri)) {
                    file.delete();
                    return;
                }
                try {
                    FileManager fm = new FileManager();
                    fm.delete(getDiskFilePath(targetUri));
                    fm.run();
                } catch (Exception ioe) {
                    log.error(ioe.getMessage(), ioe);
                    throw new ServiceAccessException(service, "Could not delete the file " + targetUri + " : " + ioe.getMessage());
                }
            }
            FileHelper.removeRec(file);
            File propertyFile = getPropertyFile(targetUri);
            if (propertyFile.exists()) propertyFile.delete();
            File lockFile = getLockFile(targetUri);
            if (lockFile.exists()) lockFile.delete();
            File errorFile = getErrFile(targetUri);
            if (errorFile.exists()) errorFile.delete();
        } catch (SecurityException e) {
            throw new AccessDeniedException(targetUri, e.getMessage(), "/actions/write");
        }
    }

    public boolean objectExists(String uri) throws ServiceAccessException, AccessDeniedException {
        try {
            if (getFile(uri).exists()) {
                return true;
            } else if (Config.isFrontEnd()) {
                FileInfo info = getDiskFile(uri);
                if (info != null) {
                    log.debug("object " + uri + " exists on DISK isdir=" + info.isDirectory());
                    return true;
                }
            }
            log.debug("object '" + uri + "' not found");
            return false;
        } catch (SecurityException e) {
            throw new AccessDeniedException(uri, e.getMessage(), "read");
        }
    }

    public boolean isFolder(String uri) throws ServiceAccessException, AccessDeniedException {
        try {
            if (getFile(uri).exists() && getFile(uri).isDirectory()) {
                return true;
            } else if (Config.isFrontEnd()) {
                FileInfo info = getDiskFile(uri);
                if (info != null && info.isDirectory()) {
                    log.debug("folder found from DISK " + uri);
                    return true;
                }
            }
        } catch (SecurityException e) {
            throw new AccessDeniedException(uri, e.getMessage(), "read");
        }
        return false;
    }

    public boolean isResource(String uri) throws ServiceAccessException, AccessDeniedException {
        try {
            if ((getFile(uri).exists() && !getFile(uri).isDirectory())) {
                log.debug("local copy found of resource " + uri + " locally");
                return true;
            } else if (Config.isFrontEnd()) {
                FileInfo info = getDiskFile(uri);
                log.debug("resource found from DISK " + uri + " " + (info != null));
                return (info != null && info.isDirectory() == false);
            }
            return false;
        } catch (SecurityException e) {
            throw new AccessDeniedException(uri, e.getMessage(), "read");
        }
    }

    public void createFolder(String folderUri) throws ServiceAccessException, AccessDeniedException, ObjectAlreadyExistsException {
        try {
            log.info("creating folder " + folderUri);
            File folder = getFile(folderUri);
            if (!folder.mkdirs()) throw new ServiceAccessException(service, "Can not create directory " + folderUri);
            if (Config.isFrontEnd()) {
                try {
                    FileManager fm = new FileManager();
                    fm.put(getDiskFilePath(folderUri), folder);
                    new Thread(fm).start();
                } catch (IOException e) {
                    log.error("Failed to create folder: " + e.getMessage(), e);
                    throw new ServiceAccessException(service, e);
                }
            }
        } catch (SecurityException e) {
            throw new AccessDeniedException(folderUri, e.getMessage(), "create");
        }
    }

    public String[] getChildrenNames(String folderUri) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException {
        try {
            HashSet<String> childList = new HashSet<String>();
            if (Config.isFrontEnd()) {
                String remotePath = getDiskFilePath(folderUri);
                FileInfo[] infos = Config.getFileInfosys().listFiles(remotePath);
                for (int i = 0; i < infos.length; i++) {
                    String remoteFile = infos[i].getFilePath().substring(remotePath.length() + 1);
                    childList.add(remoteFile);
                }
            }
            File file = getFile(folderUri);
            if (file.isDirectory()) {
                File[] children = file.listFiles(new DiskFileFilter(true, null));
                for (File f : children) {
                    try {
                        if (f.exists()) {
                            childList.add(URLDecoder.decode(f.getName(), "ISO-8859-1"));
                        } else {
                            log.debug("file " + f.getPath() + "  not found");
                        }
                    } catch (UnsupportedEncodingException e) {
                        log.error("failed to decode url " + f.getName());
                    }
                }
            }
            return (String[]) childList.toArray(new String[childList.size()]);
        } catch (SecurityException e) {
            throw new AccessDeniedException(folderUri, e.getMessage(), "read");
        }
    }

    public static void createErrFile(File uri, Exception error) {
        File errFile = new File(uri + ERR_FILE_EXTENSION);
        try {
            log.info("writing error file to " + errFile.getPath() + " : " + error.getMessage());
            FileWriter w = new FileWriter(errFile);
            w.write("Errors for file " + uri.getName() + ":\n" + error.getMessage());
            w.flush();
            w.close();
        } catch (IOException ioe) {
            log.error("could not write error file to " + errFile.getPath() + " : " + ioe.getMessage());
        }
    }

    public void createResource(String resourceUri) throws ServiceAccessException, AccessDeniedException, ObjectAlreadyExistsException {
        try {
            log.info("creating resource " + resourceUri);
            File file = getFile(resourceUri);
            if (Config.isFrontEnd()) {
                if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            }
            if (file.exists()) throw new ObjectAlreadyExistsException(resourceUri);
            if (!file.createNewFile()) throw new ServiceAccessException(service, "Can not create file " + resourceUri);
        } catch (IOException e) {
            throw new ServiceAccessException(service, e);
        } catch (SecurityException e) {
            throw new AccessDeniedException(resourceUri, e.getMessage(), "create");
        }
    }

    public void setResourceContent(String resourceUri, InputStream content, String contentType, String characterEncoding) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException {
        try {
            File file = getFile(resourceUri);
            if (!file.exists()) throw new ObjectNotFoundException(resourceUri);
            try {
                save(content, file);
                if (Config.isFrontEnd()) {
                    FileManager fm = new FileManager();
                    fm.put(getDiskFilePath(resourceUri), file);
                    if (Config.getPutOperation() == 0) {
                        new Thread(fm).start();
                    } else {
                        fm.run();
                        if (Config.getPutOperation() == 2) {
                            file.delete();
                            getPropertyFile(resourceUri).delete();
                            getLockFile(resourceUri).delete();
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                throw new AccessDeniedException(resourceUri, e.getMessage(), "store");
            } catch (IOException e) {
                log.error("File upload failed: " + e.getMessage(), e);
                throw new ServiceAccessException(service, e);
            }
        } catch (SecurityException e) {
            throw new AccessDeniedException(resourceUri, e.getMessage(), "store");
        }
    }

    public long getResourceLength(String resourceUri) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException {
        try {
            if (Config.isFrontEnd()) {
                FileInfo info = getDiskFile(resourceUri);
                if (info != null) {
                    log.debug("length of DISK resource " + info.getFilePath() + " is " + info.getFileSize());
                    return info.getFileSize();
                }
            }
            File file = getFile(resourceUri);
            if (!file.exists()) throw new ObjectNotFoundException(resourceUri);
            return file.length();
        } catch (SecurityException e) {
            throw new AccessDeniedException(resourceUri, e.getMessage(), "read");
        }
    }

    public InputStream getResourceContent(String resourceUri) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException {
        try {
            log.info("fetching file " + resourceUri);
            File file = getFile(resourceUri);
            if (!file.exists()) {
                throw new ObjectNotFoundException(resourceUri);
            }
            InputStream in;
            try {
                in = new BufferedInputStream(new FileInputStream(file));
                return in;
            } catch (FileNotFoundException e) {
                throw new ObjectNotFoundException(resourceUri);
            }
        } catch (SecurityException e) {
            throw new AccessDeniedException(resourceUri, e.getMessage(), "read");
        }
    }

    public void removeObject(String uri) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException {
        macroDelete(uri);
    }

    public Date getLastModified(String uri) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException {
        try {
            if (Config.isFrontEnd()) {
                FileInfo info = getDiskFile(uri);
                if (info != null) {
                    log.debug("DISK resource " + info.getFilePath() + " modified " + info.getUploadTime());
                    return new Date(info.getUploadTime());
                }
            }
            File file = getFile(uri);
            if (!file.exists()) {
                throw new ObjectNotFoundException(uri);
            }
            long lastModified = file.lastModified();
            return new Date(lastModified);
        } catch (SecurityException e) {
            throw new AccessDeniedException(uri, e.getMessage(), "read");
        }
    }

    public Date getCreationDate(String uri) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException {
        return getLastModified(uri);
    }

    public Map getProperties(String uri) throws ServiceAccessException, AccessDeniedException {
        File file = getPropertyFile(uri);
        if (file.exists()) {
            return readProperties(file);
        } else if (Config.isFrontEnd()) {
            FileInfo info = getDiskFile(uri);
            if (info != null) {
                log.debug("DISK resource " + info.getFilePath() + " props " + info.getProperties());
                return info.getProperties();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void setProperties(String uri, Map properties) throws ServiceAccessException, AccessDeniedException {
        log.debug("props for " + uri + " = " + properties);
        if (Config.isFrontEnd()) {
            FileInfo info = getDiskFile(uri);
            if (info != null) {
                info.setProperties(properties);
                log.debug("DISK resource " + info.getFilePath() + " props " + info.getProperties());
            }
        }
        File file = getPropertyFile(uri);
        assureCreated(file, uri);
        Properties props = new Properties();
        props.putAll(properties);
        saveProperties(file, props, "WebDAV properties");
    }

    public void addOrUpdateProperty(String uri, String name, String value) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException {
    }

    public void removeProperty(String uri, String name) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException {
    }

    public void lockObject(String uri, String lockId, String subject, Date expiration, boolean exclusive, boolean inheritable) throws ServiceAccessException, AccessDeniedException {
        File file = getLockFile(uri);
        assureCreated(file, uri);
        Properties properties = readProperties(file);
        String lockString = expiration.getTime() + "|" + String.valueOf(exclusive) + "|" + String.valueOf(inheritable) + "|" + subject;
        properties.setProperty(lockId, lockString);
        saveProperties(file, properties, "WebDAV locks");
    }

    public void unlockObject(String uri, String lockId) throws ServiceAccessException, AccessDeniedException {
        File file = getLockFile(uri);
        if (!file.exists()) {
            throw new ServiceAccessException(service, "There nothing to unlock for " + uri);
        }
        Properties properties = readProperties(file);
        properties.remove(lockId);
        if (properties.size() != 0) {
            saveProperties(file, properties, "WebDAV locks");
        } else {
            if (!file.delete()) throw new ServiceAccessException(service, "Could not delete lock file for " + uri);
        }
    }

    @SuppressWarnings("unchecked")
    public Lock[] getLockInfo(String uri) throws ServiceAccessException, AccessDeniedException {
        File file = getLockFile(uri);
        if (!file.exists()) {
            return null;
        }
        Properties properties = readProperties(file);
        List<Lock> locks = new ArrayList<Lock>();
        Enumeration enumeration = properties.propertyNames();
        while (enumeration.hasMoreElements()) {
            String id = (String) enumeration.nextElement();
            String value = properties.getProperty(id);
            if (value == null) {
                throw new ServiceAccessException(service, "Invalid lockId " + id);
            }
            StringTokenizer tokenizer = new StringTokenizer(value, "|");
            int tokens = tokenizer.countTokens();
            if (tokens != 4) {
                throw new ServiceAccessException(service, "Invalid lock information for lockId " + id);
            }
            String dateString = tokenizer.nextToken();
            String exclusiveString = tokenizer.nextToken();
            String inheritableString = tokenizer.nextToken();
            String subject = tokenizer.nextToken();
            Date date = new Date(Long.valueOf(dateString).longValue());
            boolean exclusive = Boolean.valueOf(exclusiveString).booleanValue();
            boolean inheritable = Boolean.valueOf(inheritableString).booleanValue();
            Lock lock = new SimpleLock(id, exclusive, inheritable, date, subject);
            locks.add(lock);
        }
        Lock[] lockArray = new Lock[locks.size()];
        lockArray = (Lock[]) locks.toArray(lockArray);
        return lockArray;
    }

    /**
     * Gets the address of local webdav service.
     * Consists of Config.getTransport(), bind.address,
     * Config.getUserName(), Config.getUserCredentials(),
     * Config.getPort() and Config.getPathInfo().  
     * 
     * @return protocol://user:password@hostname:ip/path
     */
    public static String getStorageURL() {
        String hostname = "localhost";
        try {
            hostname = System.getProperty("bind.address", InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
        }
        String auth = "";
        if (Config.isFrontEnd() == false) {
            auth = Config.getUserName() + ":" + Config.getUserCredentials() + "@";
        }
        return Config.getTransport() + "://" + auth + hostname + ":" + Config.getPort() + "/" + Config.getPathInfo();
    }

    /**
     * Gets playlist out of files under the directory
     * Folder is traversed recursively.
     * 
     * @param uri uri for webdav resource
     * @return bytes stored under resource
     */
    public static StringBuffer getM3u(String uri) {
        StringBuffer sb = new StringBuffer("#EXTM3U\n");
        getM3u(uri, getStorageURL(), sb);
        return sb;
    }

    /**
     * Recursive method for listing music files from directory in M3U format.
     * @param uri folder or file to lsit
     * @param base base uri
     * @param sb result buffer
     */
    private static void getM3u(String uri, String base, StringBuffer sb) {
        File[] files = getFile(uri).listFiles(new DiskFileFilter(false, new String[] { ".mp3", ".wma" }));
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    getM3u(uri + f.getName(), base, sb);
                } else {
                    try {
                        sb.append("#EXTINF:0,").append(URLDecoder.decode(f.getName(), "ISO-8859-1")).append("\n");
                    } catch (UnsupportedEncodingException e) {
                    }
                    String path = base + getDiskFilePath(uri) + "/" + f.getName();
                    path = path.replaceAll("\\+", "%20");
                    sb.append(path).append("\n");
                }
            }
        }
    }

    /**
     * Gets the file path without webdav folder name (scope).
     * 
     * @param uri uri to the file, with or without the folder name (scope).
     * @return file object or null if doesn't exist
     */
    public static String getDiskFilePath(String uri) {
        if (uri.startsWith(WEBDAV_FILES_PATH)) return uri.substring(WEBDAV_FILES_PATH.length());
        return uri;
    }

    /**
     * Gets the file info from distributed storage.
     * @param uri uri to the file, with or without the folder name (scope).
     * @return file object or null if doesn't exist
     */
    public static FileInfo getDiskFile(String uri) {
        return Config.getFileInfosys().findFile(getDiskFilePath(uri));
    }

    /**
     * Gets the file object from local storage directory.
     * First method for searching existing file is direct match. If it doesn't
     * exists, then the folder path is travelsed from root to be able to find 
     * the maching file. Names are decoded and compared in case insensitive way, 
     * so that different encoding methods shouln't prevent finding of file from
     * the disk.
     * <p>
     * If file is not found, the new file object is returned. If {@link Config#getEncoding()}
     * is true the file name is encoded to make sure we are able to fetch it back 
     * later on every environment.
     * 
     * @param uri decoded file path
     * @return file object to the storage directory
     */
    public static File getFile(String uri) {
        File file = new File(Config.getSiloDir(), getDiskFilePath(uri));
        if (!file.exists() && !isLockFilename(file.getName()) && !isPropertyFilename(file.getName()) && !isErrFilename(file.getName())) {
            log.debug("file uri " + uri + " not found directly, searching...");
            String[] trail = getDiskFilePath(uri).split("/");
            file = new File(Config.getSiloDir());
            for (String p : trail) {
                if (p.length() == 0) continue;
                File current = new File(file, p);
                if (current.exists() == false) {
                    File[] list = file.listFiles();
                    if (list != null) {
                        try {
                            boolean match = false;
                            String simpleP = URLDecoder.decode(p, "ISO-8859-1");
                            for (File f : list) {
                                String simpleF = URLDecoder.decode(f.getName(), "ISO-8859-1");
                                if (simpleF.equalsIgnoreCase(simpleP)) {
                                    log.debug("match " + p + " = " + f.getName());
                                    current = f;
                                    match = true;
                                    break;
                                } else {
                                }
                            }
                            if (match == false && Config.getEncoding()) {
                                File encoded = new File(current.getParentFile(), URLEncoder.encode(current.getName(), "ISO-8859-1"));
                                current = encoded;
                            }
                        } catch (UnsupportedEncodingException e) {
                        }
                    }
                }
                file = current;
            }
        }
        return file;
    }

    protected File getPropertyFile(String uri) {
        String dir;
        String name;
        int lastSlash = uri.lastIndexOf('/');
        if (lastSlash != -1) {
            dir = uri.substring(0, lastSlash + 1);
            name = uri.substring(lastSlash + 1);
        } else {
            dir = "";
            name = uri;
        }
        String path = dir + PROPERTY_FILE_PREFIX + name;
        return getFile(path);
    }

    protected static boolean isPropertyFilename(String uri) {
        return uri.startsWith(PROPERTY_FILE_PREFIX);
    }

    protected File getLockFile(String uri) {
        return getFile(uri + LOCK_FILE_EXTENSION);
    }

    protected static boolean isLockFilename(String uri) {
        return uri.endsWith(LOCK_FILE_EXTENSION);
    }

    public static File getErrFile(String uri) {
        return getFile(uri + ERR_FILE_EXTENSION);
    }

    public static boolean isErrFilename(String uri) {
        return uri.endsWith(ERR_FILE_EXTENSION);
    }

    protected void assureCreated(File file, String uri) throws ServiceAccessException {
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) throw new ServiceAccessException(service, "Can not create file " + uri);
            } catch (IOException e) {
                throw new ServiceAccessException(service, e);
            }
        }
    }

    protected String getLockEntry(String uri, String lockId) throws ServiceAccessException, ObjectNotFoundException {
        File file = getLockFile(uri);
        if (!file.exists()) {
            throw new ObjectNotFoundException(uri);
        }
        Properties properties = readProperties(file);
        String value = properties.getProperty(lockId);
        if (value == null) {
            throw new ServiceAccessException(service, "Invalid lockId " + lockId);
        }
        return value;
    }

    protected void saveProperties(File file, Properties properties, String header) throws ServiceAccessException {
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(file));
            properties.store(os, header);
        } catch (FileNotFoundException e) {
            throw new ServiceAccessException(service, e);
        } catch (IOException e) {
            throw new ServiceAccessException(service, e);
        } finally {
            if (os != null) try {
                os.close();
            } catch (IOException e) {
            }
        }
    }

    protected Properties readProperties(File file) throws ServiceAccessException {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            Properties properties = new Properties();
            properties.load(is);
            return properties;
        } catch (FileNotFoundException e) {
            throw new ServiceAccessException(service, e);
        } catch (IOException e) {
            throw new ServiceAccessException(service, e);
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Straight forward reference implemenation of a lock.
     */
    public static class SimpleLock implements Lock {

        public String id;

        public boolean exclusive;

        public boolean inheritable;

        public Date expirationDate;

        public String subject;

        public SimpleLock(String id, boolean exclusive, boolean inheritable, Date expirationDate, String subject) {
            this.id = id;
            this.exclusive = exclusive;
            this.inheritable = inheritable;
            this.expirationDate = expirationDate;
            this.subject = subject;
        }

        public boolean isExclusive() {
            return exclusive;
        }

        public Date getExpirationDate() {
            return expirationDate;
        }

        public String getId() {
            return id;
        }

        public boolean isInheritable() {
            return inheritable;
        }

        public String getSubject() {
            return subject;
        }
    }

    /**
     * Filters locking, property information and error files
     * out of DISK resources.
     */
    private static class DiskFileFilter implements FileFilter {

        private boolean errors = false;

        private String[] ext = new String[0];

        /**
         * New filter
         * @param errors include errors
         * @param ext list of extensions to search
         */
        public DiskFileFilter(boolean errors, String[] ext) {
            this.errors = errors;
            if (ext != null) this.ext = ext;
        }

        public boolean accept(File pathname) {
            if (isLockFilename(pathname.getName()) || isPropertyFilename(pathname.getName())) {
                return errors && isErrFilename(pathname.getName());
            } else if (ext.length > 0 && !pathname.isDirectory()) {
                for (String e : ext) {
                    if (pathname.getName().toLowerCase().endsWith(e.toLowerCase())) return true;
                }
                return false;
            }
            return true;
        }
    }
}
