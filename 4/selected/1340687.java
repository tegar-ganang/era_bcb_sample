package com.sitescape.team.ssfs.wck;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import com.sitescape.team.ssfs.AlreadyExistsException;
import com.sitescape.team.ssfs.CrossContextConstants;
import com.sitescape.team.ssfs.LockException;
import com.sitescape.team.ssfs.NoAccessException;
import com.sitescape.team.ssfs.NoSuchObjectException;
import com.sitescape.team.ssfs.TypeMismatchException;
import org.apache.slide.simple.store.WebdavStoreLockExtension.Lock;

public class CCClient {

    private static final CCClientCallback defaultCallback = new DefaultCCClientCallback();

    private String zoneName;

    private String userName;

    private Map<String, Object> cache;

    public CCClient(String zoneName, String userName) {
        this.zoneName = zoneName;
        this.userName = userName;
        this.cache = new HashMap<String, Object>();
    }

    public String objectInfo(String objUri, Map uri) throws CCClientException, NoAccessException {
        try {
            Map props = getPropertiesCached(objUri, uri);
            return (String) props.get(CrossContextConstants.OBJECT_INFO);
        } catch (NoSuchObjectException e) {
            return CrossContextConstants.OBJECT_INFO_NON_EXISTING;
        }
    }

    public void createFolder(String folderUri, Map uri) throws CCClientException, NoAccessException, AlreadyExistsException, TypeMismatchException {
        cache.remove(folderUri);
        CCExecutionTemplate.execute(zoneName, userName, uri, CrossContextConstants.OPERATION_CREATE_FOLDER, defaultCallback);
    }

    public void createResource(String resourceUri, Map uri) throws CCClientException, NoAccessException, AlreadyExistsException, TypeMismatchException {
        cache.remove(resourceUri);
        CCExecutionTemplate.execute(zoneName, userName, uri, CrossContextConstants.OPERATION_CREATE_RESOURCE, defaultCallback);
    }

    public void createAndSetResource(String resourceUri, Map uri, final InputStream content) throws CCClientException, NoAccessException, AlreadyExistsException, TypeMismatchException {
        cache.remove(resourceUri);
        CCExecutionTemplate.execute(zoneName, userName, uri, CrossContextConstants.OPERATION_CREATE_SET_RESOURCE, new CCClientCallback() {

            public void additionalInput(HttpServletRequest req) {
                req.setAttribute(CrossContextConstants.INPUT_STREAM, content);
            }
        });
    }

    public void setResource(String resourceUri, Map uri, final InputStream content) throws CCClientException, NoAccessException, NoSuchObjectException, TypeMismatchException {
        cache.remove(resourceUri);
        CCExecutionTemplate.execute(zoneName, userName, uri, CrossContextConstants.OPERATION_SET_RESOURCE, new CCClientCallback() {

            public void additionalInput(HttpServletRequest req) {
                req.setAttribute(CrossContextConstants.INPUT_STREAM, content);
            }
        });
    }

    public InputStream getResource(String resourceUri, Map uri) throws CCClientException, NoAccessException, NoSuchObjectException, TypeMismatchException {
        InputStream returnObj = (InputStream) CCExecutionTemplate.execute(zoneName, userName, uri, CrossContextConstants.OPERATION_GET_RESOURCE, defaultCallback);
        return returnObj;
    }

    public long getResourceLength(String resourceUri, Map uri) throws CCClientException, NoAccessException, NoSuchObjectException, TypeMismatchException {
        Map props = getPropertiesCached(resourceUri, uri);
        Long length = (Long) props.get(CrossContextConstants.DAV_PROPERTIES_GET_CONTENT_LENGTH);
        return length.longValue();
    }

    public void removeObject(String objUri, Map uri) throws CCClientException, NoAccessException, NoSuchObjectException {
        cache.remove(objUri);
        CCExecutionTemplate.execute(zoneName, userName, uri, CrossContextConstants.OPERATION_REMOVE_OBJECT, defaultCallback);
    }

    public Date getLastModified(String objUri, Map uri) throws CCClientException, NoAccessException, NoSuchObjectException {
        Map props = getPropertiesCached(objUri, uri);
        return (Date) props.get(CrossContextConstants.DAV_PROPERTIES_GET_LAST_MODIFIED);
    }

    public Date getCreationDate(String objUri, Map uri) throws CCClientException, NoAccessException, NoSuchObjectException {
        Map props = getPropertiesCached(objUri, uri);
        return (Date) props.get(CrossContextConstants.DAV_PROPERTIES_CREATION_DATE);
    }

    /**
	 * If the object doesn't represent a folder, it returns <code>null</code>.
	 * <p>
	 * Note: Unlike other methods, this method returns <code>null</code> instead
	 * of throwing <code>TypeMismatchException</code>, if the referenced object
	 * is not a folder. This discrepency is there merely to make WCK happy.  
	 * 
	 * @param resourceUri
	 * @param uri
	 * @return
	 * @throws CCClientException
	 * @throws NoAccessException
	 * @throws NoSuchObjectException
	 */
    public String[] getChildrenNames(String objUri, Map uri) throws CCClientException, NoAccessException, NoSuchObjectException {
        String[] returnObj = (String[]) CCExecutionTemplate.execute(zoneName, userName, uri, CrossContextConstants.OPERATION_GET_CHILDREN_NAMES, defaultCallback);
        return returnObj;
    }

    public Map getDAVProperties(String objUri, Map uri) throws CCClientException, NoAccessException, NoSuchObjectException {
        Map props = getPropertiesCached(objUri, uri);
        Map<String, String> result = new HashMap<String, String>();
        for (Iterator it = props.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            String key = (String) entry.getKey();
            Object val = entry.getValue();
            if (val instanceof String && key.startsWith(CrossContextConstants.DAV_PROPERTIES_NAMESPACE)) result.put(key, (String) val);
        }
        return result;
    }

    /**
	 * Locks the specified resource/file. If the resource is currently unlocked, 
	 * it creates a lock under the user's name. If the resource is currently locked
	 * by the same user AND the lock id matches, the lock is updated/extended 
	 * with the new expiration date. If the resource is currently locked by
	 * the same user BUT the lock id does not match, it throws <code>LockException</code>. 
	 * If locked by another user, it throws <code>LockException</code> (that is, 
	 * when accessed through WebDAV, the two situations are not distinguished). 
	 * 
	 * @param resourceUri
	 * @param uri
	 * @param lock
	 * @throws CCClientException if unexpected error occurs
	 * @throws NoAccessException if the user has no write permission to the specified resource
	 * @throws NoSuchObjectException if the specified resource does not exist
	 * @throws LockException if fails to lock the resource
	 */
    public void lockResource(String resourceUri, Map uri, final Lock lock) throws CCClientException, NoAccessException, NoSuchObjectException, LockException, TypeMismatchException {
        cache.remove(resourceUri);
        CCExecutionTemplate.execute(zoneName, userName, uri, CrossContextConstants.OPERATION_LOCK_RESOURCE, new CCClientCallback() {

            public void additionalInput(HttpServletRequest req) {
                req.setAttribute(CrossContextConstants.LOCK_PROPERTIES_ID, lock.getId());
                req.setAttribute(CrossContextConstants.LOCK_PROPERTIES_SUBJECT, lock.getSubject());
                req.setAttribute(CrossContextConstants.LOCK_PROPERTIES_EXPIRATION_DATE, lock.getExpirationDate());
                req.setAttribute(CrossContextConstants.LOCK_PROPERTIES_OWNER_INFO, lock.getOwner());
            }
        });
    }

    /**
	 * Unlocks the specified resource/file given the id of the existing lock. 
	 * If the resource is currently locked with the specified id, it unlocks
	 * it. In all other conditions, it is noop and returns silently.  
	 */
    public void unlockResource(String resourceUri, Map uri, final String lockId) throws CCClientException, NoAccessException, NoSuchObjectException, TypeMismatchException {
        cache.remove(resourceUri);
        CCExecutionTemplate.execute(zoneName, userName, uri, CrossContextConstants.OPERATION_UNLOCK_RESOURCE, new CCClientCallback() {

            public void additionalInput(HttpServletRequest req) {
                req.setAttribute(CrossContextConstants.LOCK_PROPERTIES_ID, lockId);
            }
        });
    }

    /**
	 * Returns lock information about the resource/file.
	 * 
	 * @param resourceUri
	 * @param uri
	 * @return
	 * @throws CCClientException
	 * @throws NoAccessException
	 * @throws NoSuchObjectException
	 */
    public Lock[] getLockInfo(String resourceUri, Map uri) throws CCClientException, NoAccessException, NoSuchObjectException, TypeMismatchException {
        Map props = getPropertiesCached(resourceUri, uri);
        String lockId = (String) props.get(CrossContextConstants.LOCK_PROPERTIES_ID);
        if (lockId != null) {
            String lockSubject = (String) props.get(CrossContextConstants.LOCK_PROPERTIES_SUBJECT);
            Date lockExpirationDate = (Date) props.get(CrossContextConstants.LOCK_PROPERTIES_EXPIRATION_DATE);
            String lockOwner = (String) props.get(CrossContextConstants.LOCK_PROPERTIES_OWNER_INFO);
            Lock lock = new SimpleLock(lockId, lockSubject, lockExpirationDate, lockOwner);
            return new Lock[] { lock };
        } else {
            return null;
        }
    }

    public void copyObject(String sourceUri, Map sourceMap, String targetUri, Map targetMap, final boolean overwrite, final boolean recursive) throws CCClientException, NoAccessException, NoSuchObjectException, AlreadyExistsException, TypeMismatchException {
        cache.remove(targetUri);
        CCExecutionTemplate.execute(zoneName, userName, sourceMap, targetMap, CrossContextConstants.OPERATION_COPY_OBJECT, new CCClientCallback() {

            public void additionalInput(HttpServletRequest req) {
                req.setAttribute(CrossContextConstants.OVERWRITE, Boolean.valueOf(overwrite));
                req.setAttribute(CrossContextConstants.RECURSIVE, Boolean.valueOf(recursive));
            }
        });
    }

    public void moveObject(String sourceUri, Map sourceMap, String targetUri, Map targetMap, final boolean overwrite) throws CCClientException, NoAccessException, NoSuchObjectException, AlreadyExistsException, TypeMismatchException {
        cache.remove(sourceUri);
        cache.remove(targetUri);
        CCExecutionTemplate.execute(zoneName, userName, sourceMap, targetMap, CrossContextConstants.OPERATION_MOVE_OBJECT, new CCClientCallback() {

            public void additionalInput(HttpServletRequest req) {
                req.setAttribute(CrossContextConstants.OVERWRITE, Boolean.valueOf(overwrite));
            }
        });
    }

    private Map getPropertiesCached(String objUri, Map uri) throws CCClientException, NoAccessException, NoSuchObjectException {
        Object value = cache.get(objUri);
        if (value == null) {
            try {
                Map props = (Map) CCExecutionTemplate.execute(zoneName, userName, uri, CrossContextConstants.OPERATION_GET_PROPERTIES, defaultCallback);
                cache.put(objUri, props);
                return props;
            } catch (NoAccessException e) {
                cache.put(objUri, e);
                throw e;
            } catch (NoSuchObjectException e) {
                cache.put(objUri, e);
                throw e;
            }
        } else if (value instanceof Map) {
            return (Map) value;
        } else if (value instanceof NoAccessException) {
            throw (NoAccessException) value;
        } else if (value instanceof NoSuchObjectException) {
            throw (NoSuchObjectException) value;
        } else {
            throw new CCClientException("Invalid cache entry type: " + value.getClass());
        }
    }

    static class DefaultCCClientCallback implements CCClientCallback {

        public void additionalInput(HttpServletRequest req) {
        }
    }
}
