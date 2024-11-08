package org.kablink.teaming.ssfs.server.impl;

import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import javax.activation.FileTypeMap;
import org.kablink.teaming.dao.CoreDao;
import org.kablink.teaming.module.binder.impl.WriteEntryDataException;
import org.kablink.teaming.ssfs.AlreadyExistsException;
import org.kablink.teaming.ssfs.CrossContextConstants;
import org.kablink.teaming.ssfs.LockException;
import org.kablink.teaming.ssfs.NoAccessException;
import org.kablink.teaming.ssfs.NoSuchObjectException;
import org.kablink.teaming.ssfs.TypeMismatchException;
import org.kablink.teaming.ssfs.server.KablinkFileSystem;
import org.kablink.teaming.util.AbstractAllModulesInjected;

public class KablinkFileSystemImpl extends AbstractAllModulesInjected implements KablinkFileSystem {

    private KablinkFileSystemInternal ssfsInt;

    private KablinkFileSystemLibrary ssfsLib;

    public KablinkFileSystemImpl() {
        ssfsInt = new KablinkFileSystemInternal(this);
        ssfsLib = new KablinkFileSystemLibrary(this);
    }

    public void setMimeTypes(FileTypeMap mimeTypes) {
        ssfsInt.setMimeTypes(mimeTypes);
        ssfsLib.setMimeTypes(mimeTypes);
    }

    public void setCoreDao(CoreDao coreDao) {
        ssfsInt.setCoreDao(coreDao);
        ssfsLib.setCoreDao(coreDao);
    }

    public void createResource(Map uri) throws NoAccessException, AlreadyExistsException, TypeMismatchException {
        if (isInternal(uri)) ssfsInt.createResource(uri); else ssfsLib.createResource(uri);
    }

    public void setResource(Map uri, InputStream content) throws NoAccessException, NoSuchObjectException, TypeMismatchException {
        if (isInternal(uri)) ssfsInt.setResource(uri, content); else ssfsLib.setResource(uri, content);
    }

    public void createAndSetResource(Map uri, InputStream content) throws NoAccessException, AlreadyExistsException, TypeMismatchException {
        if (isInternal(uri)) ssfsInt.createAndSetResource(uri, content); else ssfsLib.createAndSetResource(uri, content);
    }

    public void createDirectory(Map uri) throws NoAccessException, AlreadyExistsException, TypeMismatchException {
        if (isInternal(uri)) ssfsInt.createDirectory(uri); else ssfsLib.createDirectory(uri);
    }

    public InputStream getResource(Map uri) throws NoAccessException, NoSuchObjectException, TypeMismatchException {
        if (isInternal(uri)) return ssfsInt.getResource(uri); else return ssfsLib.getResource(uri);
    }

    public void removeObject(Map uri) throws NoAccessException, NoSuchObjectException {
        if (isInternal(uri)) ssfsInt.removeObject(uri); else ssfsLib.removeObject(uri);
    }

    public String[] getChildrenNames(Map uri) throws NoAccessException, NoSuchObjectException {
        if (isInternal(uri)) return ssfsInt.getChildrenNames(uri); else return ssfsLib.getChildrenNames(uri);
    }

    public Map getProperties(Map uri) throws NoAccessException, NoSuchObjectException {
        if (isInternal(uri)) return ssfsInt.getProperties(uri); else return ssfsLib.getProperties(uri);
    }

    public void lockResource(Map uri, String lockId, String lockSubject, Date lockExpirationDate, String lockOwnerInfo) throws NoAccessException, NoSuchObjectException, LockException, TypeMismatchException {
        if (isInternal(uri)) ssfsInt.lockResource(uri, lockId, lockSubject, lockExpirationDate, lockOwnerInfo); else ssfsLib.lockResource(uri, lockId, lockSubject, lockExpirationDate, lockOwnerInfo);
    }

    public void unlockResource(Map uri, String lockId) throws NoAccessException, NoSuchObjectException, TypeMismatchException {
        if (isInternal(uri)) ssfsInt.unlockResource(uri, lockId); else ssfsLib.unlockResource(uri, lockId);
    }

    public void copyObject(Map sourceUri, Map targetUri, boolean overwrite, boolean recursive) throws NoAccessException, NoSuchObjectException, AlreadyExistsException, TypeMismatchException {
        if (isInternal(sourceUri)) ssfsInt.copyObject(sourceUri, targetUri, overwrite, recursive); else ssfsLib.copyObject(sourceUri, targetUri, overwrite, recursive);
    }

    public void moveObject(Map sourceUri, Map targetUri, boolean overwrite) throws NoAccessException, NoSuchObjectException, AlreadyExistsException, TypeMismatchException, WriteEntryDataException {
        if (isInternal(sourceUri)) ssfsInt.moveObject(sourceUri, targetUri, overwrite); else ssfsLib.moveObject(sourceUri, targetUri, overwrite);
    }

    private boolean isInternal(Map uri) {
        if (((String) uri.get(CrossContextConstants.URI_TYPE)).equals(CrossContextConstants.URI_TYPE_INTERNAL)) return true; else return false;
    }
}
