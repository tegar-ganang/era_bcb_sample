package org.kablink.teaming.ssfs.wck;

import java.io.InputStream;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import org.apache.commons.transaction.util.LoggerFacade;
import org.apache.slide.common.Service;
import org.apache.slide.common.ServiceAccessException;
import org.apache.slide.common.ServiceParameterErrorException;
import org.apache.slide.common.ServiceParameterMissingException;
import org.apache.slide.lock.ObjectLockedException;
import org.apache.slide.security.AccessDeniedException;
import org.apache.slide.security.UnauthenticatedException;
import org.apache.slide.simple.store.BasicWebdavStore;
import org.apache.slide.simple.store.WebdavStoreBulkPropertyExtension;
import org.apache.slide.simple.store.WebdavStoreLockExtension;
import org.apache.slide.simple.store.WebdavStoreMacroCopyExtension;
import org.apache.slide.simple.store.WebdavStoreMacroDeleteExtension;
import org.apache.slide.simple.store.WebdavStoreMacroMoveExtension;
import org.apache.slide.structure.ObjectAlreadyExistsException;
import org.apache.slide.structure.ObjectNotFoundException;
import org.kablink.teaming.asmodule.zonecontext.ZoneContextHolder;
import org.kablink.teaming.ssfs.AlreadyExistsException;
import org.kablink.teaming.ssfs.LockException;
import org.kablink.teaming.ssfs.NoAccessException;
import org.kablink.teaming.ssfs.NoSuchObjectException;
import org.kablink.teaming.ssfs.TypeMismatchException;
import static org.kablink.teaming.ssfs.CrossContextConstants.*;

public class WebdavKablink implements BasicWebdavStore, WebdavStoreBulkPropertyExtension, WebdavStoreLockExtension, WebdavStoreMacroCopyExtension, WebdavStoreMacroMoveExtension, WebdavStoreMacroDeleteExtension {

    private static final String URI_SYNTACTIC_TYPE = "synType";

    private static final Integer URI_SYNTACTIC_TYPE_FOLDER = new Integer(1);

    private static final Integer URI_SYNTACTIC_TYPE_FILE = new Integer(2);

    private static final Integer URI_SYNTACTIC_TYPE_EITHER = new Integer(3);

    private Service service;

    private LoggerFacade logger;

    private String serverName;

    private String userName;

    private CCClient client;

    public void begin(Service service, Principal principal, Object connection, LoggerFacade logger, Hashtable parameters) throws ServiceAccessException, ServiceParameterErrorException, ServiceParameterMissingException {
        this.service = service;
        this.logger = logger;
        if (connection != null) {
            this.serverName = ZoneContextHolder.getServerName();
            this.userName = (String) connection;
        }
        this.client = new CCClient(serverName, userName);
    }

    public void checkAuthentication() throws UnauthenticatedException {
    }

    public void commit() throws ServiceAccessException {
    }

    public void rollback() throws ServiceAccessException {
    }

    public boolean objectExists(String uri) throws ServiceAccessException, AccessDeniedException, ObjectLockedException {
        try {
            Map m = parseUri(uri);
            return (!objectInfo(uri, m).equals(OBJECT_INFO_NON_EXISTING));
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "read");
        } catch (NoAccessException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "read");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        }
    }

    public boolean isFolder(String uri) throws ServiceAccessException, AccessDeniedException, ObjectLockedException {
        try {
            Map m = parseUri(uri);
            return (objectInfo(uri, m).equals(OBJECT_INFO_DIRECTORY));
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "read");
        } catch (NoAccessException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "read");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        }
    }

    public boolean isResource(String uri) throws ServiceAccessException, AccessDeniedException, ObjectLockedException {
        try {
            Map m = parseUri(uri);
            return (objectInfo(uri, m).equals(OBJECT_INFO_FILE));
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "read");
        } catch (NoAccessException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "read");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        }
    }

    public void createFolder(String uri) throws ServiceAccessException, AccessDeniedException, ObjectAlreadyExistsException, ObjectLockedException {
        try {
            Map m = parseUri(uri);
            if (representsAbstractFolder(m)) throw new ObjectAlreadyExistsException(uri); else if (URI_TYPE_INTERNAL.equals(m.get(URI_TYPE))) throw new AccessDeniedException(uri, "Creating folder is not supported for internal uri", "create"); else client.createFolder(uri, m);
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "create");
        } catch (NoAccessException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "create");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        } catch (AlreadyExistsException e) {
            throw new ObjectAlreadyExistsException(uri);
        } catch (TypeMismatchException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage());
        }
    }

    public void createResource(String uri) throws ServiceAccessException, AccessDeniedException, ObjectAlreadyExistsException, ObjectLockedException {
        try {
            Map m = parseUri(uri);
            if (getUriSyntacticType(m) == URI_SYNTACTIC_TYPE_FOLDER) throw new ServiceAccessException(service, "The position refers to a folder"); else client.createResource(uri, m);
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "create");
        } catch (NoAccessException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "create");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        } catch (AlreadyExistsException e) {
            throw new ObjectAlreadyExistsException(uri);
        } catch (TypeMismatchException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage());
        }
    }

    public void setResourceContent(String uri, InputStream content, String contentType, String characterEncoding) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException, ObjectLockedException {
        try {
            Map m = parseUri(uri);
            if (getUriSyntacticType(m) == URI_SYNTACTIC_TYPE_FOLDER) throw new ObjectNotFoundException(uri); else client.setResource(uri, m, content);
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "store");
        } catch (NoAccessException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "store");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        } catch (NoSuchObjectException e) {
            throw new ObjectNotFoundException(uri);
        } catch (TypeMismatchException e) {
            throw new ObjectNotFoundException(uri);
        }
    }

    public void createAndSetResource(String uri, InputStream content, String contentType, String characterEncoding) throws ServiceAccessException, AccessDeniedException, ObjectAlreadyExistsException, ObjectLockedException {
        try {
            Map m = parseUri(uri);
            if (getUriSyntacticType(m) == URI_SYNTACTIC_TYPE_FOLDER) throw new ServiceAccessException(service, "The position refers to a folder"); else client.createAndSetResource(uri, m, content);
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "create");
        } catch (NoAccessException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "create");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        } catch (AlreadyExistsException e) {
            throw new ObjectAlreadyExistsException(uri);
        } catch (TypeMismatchException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage());
        }
    }

    public Date getLastModified(String uri) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException, ObjectLockedException {
        try {
            Map m = parseUri(uri);
            if (representsAbstractFolder(m)) return new Date(0); else return client.getLastModified(uri, m);
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "read");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        } catch (NoAccessException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "read");
        } catch (NoSuchObjectException e) {
            throw new ObjectNotFoundException(uri);
        }
    }

    public Date getCreationDate(String uri) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException, ObjectLockedException {
        try {
            Map m = parseUri(uri);
            if (representsAbstractFolder(m)) return new Date(0); else return client.getCreationDate(uri, m);
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "read");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        } catch (NoAccessException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "read");
        } catch (NoSuchObjectException e) {
            throw new ObjectNotFoundException(uri);
        }
    }

    public String[] getChildrenNames(String folderUri) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException, ObjectLockedException {
        try {
            Map m = parseUri(folderUri);
            if (filesOnly(m)) {
                return new String[] { URI_TYPE_INTERNAL, URI_TYPE_LIBRARY };
            } else if (getUriSyntacticType(m) == URI_SYNTACTIC_TYPE_FILE) {
                return null;
            } else {
                return client.getChildrenNames(folderUri, m);
            }
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(folderUri, e.getLocalizedMessage(), "read");
        } catch (NoAccessException e) {
            throw new AccessDeniedException(folderUri, e.getLocalizedMessage(), "read");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        } catch (NoSuchObjectException e) {
            throw new ObjectNotFoundException(folderUri);
        } catch (TypeMismatchException e) {
            return null;
        }
    }

    public InputStream getResourceContent(String uri) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException, ObjectLockedException {
        try {
            Map m = parseUri(uri);
            if (getUriSyntacticType(m) == URI_SYNTACTIC_TYPE_FOLDER) throw new ObjectNotFoundException(uri); else return client.getResource(uri, m);
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "read");
        } catch (NoAccessException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "read");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        } catch (NoSuchObjectException e) {
            throw new ObjectNotFoundException(uri);
        } catch (TypeMismatchException e) {
            throw new ObjectNotFoundException(uri);
        }
    }

    public long getResourceLength(String uri) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException, ObjectLockedException {
        try {
            Map m = parseUri(uri);
            if (getUriSyntacticType(m) == URI_SYNTACTIC_TYPE_FOLDER) throw new ObjectNotFoundException(uri); else return client.getResourceLength(uri, m);
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "read");
        } catch (NoAccessException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "read");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        } catch (NoSuchObjectException e) {
            throw new ObjectNotFoundException(uri);
        } catch (TypeMismatchException e) {
            throw new ObjectNotFoundException(uri);
        }
    }

    public void removeObject(String uri) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException, ObjectLockedException {
        try {
            Map m = parseUri(uri);
            if (representsAbstractFolder(m)) {
                throw new AccessDeniedException(uri, "Can not remove the folder", "delete");
            } else {
                if (URI_TYPE_INTERNAL.equals(m.get(URI_TYPE))) {
                    if (getUriSyntacticType(m) == URI_SYNTACTIC_TYPE_FOLDER) throw new AccessDeniedException(uri, "Removing folder is not supported for internal uri", "delete"); else client.removeObject(uri, m);
                } else {
                    client.removeObject(uri, m);
                }
            }
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "delete");
        } catch (NoAccessException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "delete");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        } catch (NoSuchObjectException e) {
            throw new ObjectNotFoundException(uri);
        }
    }

    public Map getProperties(String uri) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException, ObjectLockedException {
        try {
            Map m = parseUri(uri);
            if (representsAbstractFolder(m)) {
                return null;
            } else {
                return client.getDAVProperties(uri, m);
            }
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "read");
        } catch (NoAccessException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "read");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        }
    }

    public void setProperties(String uri, Map properties) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException, ObjectLockedException {
    }

    public void lockObject(String uri, String lockId, String subject, Date expiration, boolean exclusive, boolean inheritable, String owner) throws ServiceAccessException, AccessDeniedException {
        if (!exclusive) throw new AccessDeniedException(uri, "Shared lock is not supported", "lock");
        try {
            Map m = parseUri(uri);
            if (getUriSyntacticType(m) == URI_SYNTACTIC_TYPE_FOLDER) throw new AccessDeniedException(uri, "Locking of folder is not supported", "lock"); else client.lockResource(uri, m, new SimpleLock(lockId, subject, expiration, owner));
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "lock");
        } catch (NoAccessException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "lock");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        } catch (NoSuchObjectException e) {
            throw new AccessDeniedException(uri, "Null-resource locking (locking of non-existing object) is not supported", "lock");
        } catch (LockException e) {
            throw new AccessDeniedException(uri, "Failed to lock the resource", "lock");
        } catch (TypeMismatchException e) {
            throw new AccessDeniedException(uri, "Locking of folder is not supported", "lock");
        }
    }

    public void unlockObject(String uri, String lockId) throws ServiceAccessException, AccessDeniedException {
        try {
            Map m = parseUri(uri);
            if (getUriSyntacticType(m) != URI_SYNTACTIC_TYPE_FOLDER) client.unlockResource(uri, m, lockId);
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "lock");
        } catch (NoAccessException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "lock");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        } catch (NoSuchObjectException e) {
        } catch (TypeMismatchException e) {
        }
    }

    public Lock[] getLockInfo(String uri) throws ServiceAccessException, AccessDeniedException {
        try {
            Map m = parseUri(uri);
            if (getUriSyntacticType(m) == URI_SYNTACTIC_TYPE_FOLDER) return null; else return client.getLockInfo(uri, m);
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "lock");
        } catch (NoAccessException e) {
            throw new AccessDeniedException(uri, e.getLocalizedMessage(), "lock");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        } catch (NoSuchObjectException e) {
            return null;
        } catch (TypeMismatchException e) {
            return null;
        }
    }

    public void macroDelete(String targetUri) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException, ObjectLockedException {
        Map tm = null;
        try {
            tm = parseUri(targetUri);
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(targetUri, e.getLocalizedMessage(), "/actions/write");
        }
        try {
            client.removeObject(targetUri, tm);
        } catch (NoAccessException e) {
            throw new AccessDeniedException(targetUri, e.getLocalizedMessage(), "/actions/write");
        } catch (NoSuchObjectException e) {
            throw new ObjectNotFoundException(targetUri);
        }
    }

    public void macroCopy(String sourceUri, String targetUri, boolean overwrite, boolean recursive) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException, ObjectAlreadyExistsException, ObjectLockedException {
        Map sm = null;
        Map tm = null;
        try {
            sm = parseUri(sourceUri);
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(sourceUri, e.getLocalizedMessage(), "/actions/write");
        }
        try {
            tm = parseUri(targetUri);
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(targetUri, e.getLocalizedMessage(), "/actions/write");
        }
        try {
            client.copyObject(sourceUri, sm, targetUri, tm, overwrite, recursive);
        } catch (NoAccessException e) {
            throw new AccessDeniedException(targetUri, e.getLocalizedMessage(), "/actions/write");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        } catch (NoSuchObjectException e) {
            throw new ObjectNotFoundException(sourceUri);
        } catch (AlreadyExistsException e) {
            throw new ObjectAlreadyExistsException(targetUri);
        } catch (TypeMismatchException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage());
        }
    }

    public void macroMove(String sourceUri, String targetUri, boolean overwrite) throws ServiceAccessException, AccessDeniedException, ObjectNotFoundException, ObjectAlreadyExistsException, ObjectLockedException {
        Map sm = null;
        Map tm = null;
        try {
            sm = parseUri(sourceUri);
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(sourceUri, e.getLocalizedMessage(), "/actions/write");
        }
        try {
            tm = parseUri(targetUri);
        } catch (ZoneMismatchException e) {
            throw new AccessDeniedException(targetUri, e.getLocalizedMessage(), "/actions/write");
        }
        try {
            client.moveObject(sourceUri, sm, targetUri, tm, overwrite);
        } catch (NoAccessException e) {
            throw new AccessDeniedException(targetUri, e.getLocalizedMessage(), "/actions/write");
        } catch (CCClientException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage(), e.isWarning());
        } catch (NoSuchObjectException e) {
            throw new ObjectNotFoundException(sourceUri);
        } catch (AlreadyExistsException e) {
            throw new ObjectAlreadyExistsException(targetUri);
        } catch (TypeMismatchException e) {
            throw new ServiceAccessException(service, e.getLocalizedMessage());
        }
    }

    private Map returnMap(Map map, Integer uriSyntacticType) {
        map.put(URI_SYNTACTIC_TYPE, uriSyntacticType);
        return map;
    }

    /**
	 * Returns a map containing the result of parsing the uri. 
	 * If uri structural validation fails, it returns <code>null</code>.
	 * If uri's zone validation fails against user credential, it throws 
	 * <code>ZoneMismatchException</code>.
	 * 
	 * @param uri
	 * @return
	 */
    private Map parseUri(String uri) throws ZoneMismatchException {
        if (uri.startsWith(Util.URI_DELIM)) uri = uri.substring(1);
        String[] u = uri.split(Util.URI_DELIM);
        if (!u[0].equals("files")) return null;
        Map map = new HashMap();
        map.put(URI_ORIGINAL, uri);
        if (u.length == 1) return returnMap(map, URI_SYNTACTIC_TYPE_FOLDER);
        String type = u[1];
        if (!type.equals(URI_TYPE_INTERNAL) && !type.equals(URI_TYPE_LIBRARY)) return null;
        map.put(URI_TYPE, type);
        if (u.length == 2) return returnMap(map, URI_SYNTACTIC_TYPE_FOLDER);
        if (type.equals(URI_TYPE_INTERNAL)) {
            try {
                map.put(URI_BINDER_ID, Long.valueOf(u[2]));
            } catch (NumberFormatException e) {
                return null;
            }
            if (u.length == 3) return returnMap(map, URI_SYNTACTIC_TYPE_FOLDER);
            map.put(URI_ENTRY_ID, Long.valueOf(u[3]));
            if (u.length == 4) return returnMap(map, URI_SYNTACTIC_TYPE_FOLDER);
            String itemType = u[4];
            if (!itemType.equals(URI_ITEM_TYPE_LIBRARY) && !itemType.equals(URI_ITEM_TYPE_FILE) && !itemType.equals(URI_ITEM_TYPE_GRAPHIC) && !itemType.equals(URI_ITEM_TYPE_ATTACH)) return null;
            map.put(URI_ITEM_TYPE, itemType);
            if (u.length == 5) return returnMap(map, URI_SYNTACTIC_TYPE_FOLDER);
            if (itemType.equals(URI_ITEM_TYPE_LIBRARY)) {
                map.put(URI_FILEPATH, makeFilepath(u, 5));
                return returnMap(map, URI_SYNTACTIC_TYPE_FILE);
            } else if (itemType.equals(URI_ITEM_TYPE_ATTACH)) {
                map.put(URI_REPOS_NAME, u[5]);
                if (u.length == 6) return returnMap(map, URI_SYNTACTIC_TYPE_FOLDER);
                map.put(URI_FILEPATH, makeFilepath(u, 6));
                return returnMap(map, URI_SYNTACTIC_TYPE_FILE);
            } else {
                map.put(URI_ELEMNAME, u[5]);
                if (u.length == 6) return returnMap(map, URI_SYNTACTIC_TYPE_FOLDER);
                map.put(URI_FILEPATH, makeFilepath(u, 6));
                return returnMap(map, URI_SYNTACTIC_TYPE_FILE);
            }
        } else {
            String libpath = makeLibpath(u, 2);
            map.put(URI_LIBPATH, libpath);
            return returnMap(map, URI_SYNTACTIC_TYPE_EITHER);
        }
    }

    private String makeLibpath(String[] sa, int startIndex) {
        StringBuffer sb = new StringBuffer();
        for (int i = startIndex; i < sa.length; i++) {
            sb.append(Util.URI_DELIM).append(sa[i]);
        }
        String s = sb.toString();
        if (s.endsWith(Util.URI_DELIM)) s = s.substring(0, s.length() - 1);
        return s;
    }

    private String objectInfo(String uri, Map m) throws NoAccessException, CCClientException {
        if (m == null) return OBJECT_INFO_NON_EXISTING; else if (representsAbstractFolder(m)) return OBJECT_INFO_DIRECTORY; else return client.objectInfo(uri, m);
    }

    /**
	 * Returns whether the URI represents an abstract folder or not.
	 * Abstract folder is one of the following:
	 * <p>
	 *	/files
	 *  /files/internal
	 *  /files/library
	 * 
	 * @param m
	 * @return
	 */
    private boolean representsAbstractFolder(Map m) {
        return (m.size() <= 3);
    }

    private boolean filesOnly(Map m) {
        return (m.size() == 2);
    }

    private boolean uptoUriTypeOnly(Map m) {
        return (m.size() == 3);
    }

    private Integer getUriSyntacticType(Map m) {
        return (Integer) m.get(URI_SYNTACTIC_TYPE);
    }

    private String makeFilepath(String[] input, int startIndex) {
        StringBuffer sb = new StringBuffer();
        for (int i = startIndex; i < input.length; i++) {
            if (i > startIndex) sb.append("/");
            sb.append(input[i]);
        }
        return sb.toString();
    }
}
