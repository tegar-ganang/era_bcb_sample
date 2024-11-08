package com.intridea.io.vfs.provider.s3;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.provider.AbstractFileObject;
import org.apache.commons.vfs.util.MonitorOutputStream;
import org.apache.log4j.Logger;
import org.jets3t.service.S3Service;
import org.jets3t.service.ServiceException;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.acl.CanonicalGrantee;
import org.jets3t.service.acl.GrantAndPermission;
import org.jets3t.service.acl.GranteeInterface;
import org.jets3t.service.acl.GroupGrantee;
import org.jets3t.service.acl.Permission;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.model.StorageOwner;
import org.jets3t.service.utils.Mimetypes;
import com.intridea.io.vfs.operations.acl.Acl;
import com.intridea.io.vfs.operations.acl.IAclGetter;

/**
 * Implementation of the virtual S3 file system object using the Jets3t library.
 * Based on Matthias Jugel code
 * http://thinkberg.com/svn/moxo/trunk/src/main/java/com/thinkberg/moxo/
 *
 * @author Marat Komarov
 * @author Matthias L. Jugel
 */
public class S3FileObject extends AbstractFileObject {

    /**
	 * Amazon S3 service
	 */
    private final S3Service service;

    /**
	 * Amazon S3 bucket
	 */
    private S3Bucket bucket;

    /**
	 * Amazon S3 object
	 */
    private StorageObject object;

    /**
	 * True when content attached to file
	 */
    private boolean attached = false;

    /**
	 * True when content downloaded.
	 * It's an extended flag to <code>attached</code>.
	 */
    private boolean downloaded = false;

    /**
	 * Local cache of file content
	 */
    private File cacheFile;

    /**
	 * Amazon file owner. Used in ACL
	 */
    private StorageOwner fileOwner;

    /**
	 * Class logger
	 */
    private Logger logger = Logger.getLogger(S3FileObject.class);

    public S3FileObject(FileName fileName, S3FileSystem fileSystem, S3Service service, S3Bucket bucket) throws FileSystemException {
        super(fileName, fileSystem);
        this.service = service;
        this.bucket = bucket;
    }

    @Override
    protected void doAttach() throws IOException, NoSuchAlgorithmException {
        if (!this.attached) {
            try {
                this.object = this.service.getObjectDetails(this.bucket.getName(), getS3Key());
                this.logger.info(String.format("Attach file to S3 Object: %s", this.object));
            } catch (ServiceException e) {
                this.object = new S3Object(this.bucket, getS3Key());
                this.object.setLastModifiedDate(new Date());
                this.object.setContentType(null);
                this.logger.info(String.format("Attach file to S3 Object: %s", this.object));
                this.downloaded = true;
            }
            this.attached = true;
        }
    }

    @Override
    protected void doDetach() throws Exception {
        if (this.attached) {
            this.object = null;
            if (this.cacheFile != null) {
                this.cacheFile.delete();
                this.cacheFile = null;
            }
            this.downloaded = false;
            this.attached = false;
        }
    }

    @Override
    protected void doDelete() throws Exception {
        this.service.deleteObject(this.bucket, this.object.getKey());
    }

    @Override
    protected void doRename(FileObject newfile) throws Exception {
        super.doRename(newfile);
    }

    @Override
    protected void doCreateFolder() throws ServiceException {
    }

    @Override
    protected long doGetLastModifiedTime() throws Exception {
        return this.object.getLastModifiedDate().getTime();
    }

    @Override
    protected void doSetLastModifiedTime(final long modtime) throws Exception {
        this.object.setLastModifiedDate(new Date(modtime));
    }

    @Override
    protected InputStream doGetInputStream() throws Exception {
        downloadOnce();
        return Channels.newInputStream(getCacheFileChannel());
    }

    @Override
    protected OutputStream doGetOutputStream(boolean bAppend) throws Exception {
        return new S3OutputStream(Channels.newOutputStream(getCacheFileChannel()), this.service, this.object);
    }

    @Override
    protected FileType doGetType() throws Exception {
        return FileType.FILE_OR_FOLDER;
    }

    @Override
    protected String[] doListChildren() throws Exception {
        String path = getS3Key();
        if (!"".equals(path)) {
            path = path + "/";
        }
        S3Object[] children = this.service.listObjects(this.bucket.getName(), path, "/");
        String[] childrenNames = new String[children.length];
        for (int i = 0; i < children.length; i++) {
            if (!children[i].getKey().equals(path)) {
                childrenNames[i] = new File(children[i].getKey()).getName();
            }
        }
        return childrenNames;
    }

    @Override
    protected long doGetContentSize() throws Exception {
        return this.object.getContentLength();
    }

    /**
	 * Download S3 object content and save it in temporary file.
	 * Do it only if object was not already downloaded.
	 */
    private void downloadOnce() throws FileSystemException {
        if (!this.downloaded) {
            final String failedMessage = "Failed to download S3 Object %s. %s";
            final String objectPath = getName().getPath();
            try {
                S3Object obj = this.service.getObject(this.bucket.getName(), getS3Key());
                this.logger.info(String.format("Downloading S3 Object: %s", objectPath));
                InputStream is = obj.getDataInputStream();
                if (obj.getContentLength() > 0) {
                    ReadableByteChannel rbc = Channels.newChannel(is);
                    FileChannel cacheFc = getCacheFileChannel();
                    cacheFc.transferFrom(rbc, 0, obj.getContentLength());
                    cacheFc.close();
                    rbc.close();
                } else {
                    is.close();
                }
            } catch (ServiceException e) {
                throw new FileSystemException(String.format(failedMessage, objectPath, e.getMessage()), e);
            } catch (IOException e) {
                throw new FileSystemException(String.format(failedMessage, objectPath, e.getMessage()), e);
            }
            this.downloaded = true;
        }
    }

    /**
	 * Create an S3 key from a commons-vfs path. This simply strips the slash
	 * from the beginning if it exists.
	 * 
	 * @return the S3 object key
	 */
    private String getS3Key() {
        String path = getName().getPath();
        if ("".equals(path)) {
            return path;
        } else {
            return path.substring(1);
        }
    }

    /**
	 * Get or create temporary file channel for file cache
	 * @return
	 * @throws IOException
	 */
    private FileChannel getCacheFileChannel() throws IOException {
        if (this.cacheFile == null) {
            this.cacheFile = File.createTempFile("scalr.", ".s3");
        }
        return new RandomAccessFile(this.cacheFile, "rw").getChannel();
    }

    /**
	 * Returns S3 file owner.
	 * Loads it from S3 if needed.
	 */
    private StorageOwner getS3Owner() throws ServiceException {
        if (this.fileOwner == null) {
            AccessControlList s3Acl = getS3Acl();
            this.fileOwner = s3Acl.getOwner();
        }
        return this.fileOwner;
    }

    /**
	 * Get S3 ACL list
	 * 
	 * @return
	 * @throws ServiceException
	 */
    private AccessControlList getS3Acl() throws ServiceException {
        String key = getS3Key();
        return "".equals(key) ? this.service.getBucketAcl(this.bucket) : this.service.getObjectAcl(this.bucket, key);
    }

    /**
	 * Put S3 ACL list
	 * @param s3Acl
	 * @throws Exception
	 */
    private void putS3Acl(AccessControlList s3Acl) throws Exception {
        String key = getS3Key();
        if ("".equals(key)) {
            this.service.putBucketAcl(this.bucket.getName(), s3Acl);
        } else {
            doAttach();
            this.object.setAcl(s3Acl);
            this.service.putObjectAcl(this.bucket.getName(), this.object);
        }
    }

    /**
	 * Returns access control list for this file.
	 * 
	 * VFS interfaces doesn't provide interface to manage permissions. ACL can be accessed through {@link FileObject#getFileOperations()}
	 * Sample: <code>file.getFileOperations().getOperation(IAclGetter.class)</code>
	 * @see {@link FileObject#getFileOperations()}
	 * @see {@link IAclGetter}
	 * 
	 * @return Current Access control list for a file
	 * @throws FileSystemException
	 */
    public Acl getAcl() throws FileSystemException {
        Acl myAcl = new Acl();
        AccessControlList s3Acl;
        try {
            s3Acl = getS3Acl();
        } catch (ServiceException e) {
            throw new FileSystemException(e);
        }
        StorageOwner owner = s3Acl.getOwner();
        this.fileOwner = owner;
        for (GrantAndPermission item : s3Acl.getGrantAndPermissions()) {
            Permission perm = item.getPermission();
            Acl.Permission[] rights;
            if (perm.equals(Permission.PERMISSION_FULL_CONTROL)) {
                rights = Acl.Permission.values();
            } else if (perm.equals(Permission.PERMISSION_READ)) {
                rights = new Acl.Permission[1];
                rights[0] = Acl.Permission.READ;
            } else if (perm.equals(Permission.PERMISSION_WRITE)) {
                rights = new Acl.Permission[1];
                rights[0] = Acl.Permission.WRITE;
            } else {
                this.logger.error(String.format("Skip unknown permission %s", perm));
                continue;
            }
            if (item.getGrantee() instanceof GroupGrantee) {
                GroupGrantee grantee = (GroupGrantee) item.getGrantee();
                if (GroupGrantee.ALL_USERS.equals(grantee)) {
                    myAcl.allow(Acl.Group.EVERYONE, rights);
                } else if (GroupGrantee.AUTHENTICATED_USERS.equals(grantee)) {
                    myAcl.allow(Acl.Group.AUTHORIZED, rights);
                }
            } else if (item.getGrantee() instanceof CanonicalGrantee) {
                CanonicalGrantee grantee = (CanonicalGrantee) item.getGrantee();
                if (grantee.getIdentifier().equals(owner.getId())) {
                    myAcl.allow(Acl.Group.OWNER, rights);
                }
            }
        }
        return myAcl;
    }

    /**
	 * Returns access control list for this file.
	 * 
	 * VFS interfaces doesn't provide interface to manage permissions. ACL can be accessed through {@link FileObject#getFileOperations()}
	 * Sample: <code>file.getFileOperations().getOperation(IAclGetter.class)</code>
	 * @see {@link FileObject#getFileOperations()}
	 * @see {@link IAclGetter}
	 * 
	 * @param acl
	 * @throws FileSystemException
	 */
    public void setAcl(Acl acl) throws FileSystemException {
        AccessControlList s3Acl = new AccessControlList();
        StorageOwner owner;
        try {
            owner = getS3Owner();
        } catch (ServiceException e) {
            throw new FileSystemException(e);
        }
        s3Acl.setOwner(owner);
        Hashtable<Acl.Group, Acl.Permission[]> rules = acl.getRules();
        Enumeration<Acl.Group> keys = rules.keys();
        Acl.Permission[] allRights = Acl.Permission.values();
        while (keys.hasMoreElements()) {
            Acl.Group group = keys.nextElement();
            Acl.Permission[] rights = rules.get(group);
            if (rights.length == 0) {
                continue;
            }
            Permission perm;
            if (ArrayUtils.isEquals(rights, allRights)) {
                perm = Permission.PERMISSION_FULL_CONTROL;
            } else if (acl.isAllowed(group, Acl.Permission.READ)) {
                perm = Permission.PERMISSION_READ;
            } else if (acl.isAllowed(group, Acl.Permission.WRITE)) {
                perm = Permission.PERMISSION_WRITE;
            } else {
                this.logger.error(String.format("Skip unknown set of rights %s", rights.toString()));
                continue;
            }
            GranteeInterface grantee;
            if (group.equals(Acl.Group.EVERYONE)) {
                grantee = GroupGrantee.ALL_USERS;
            } else if (group.equals(Acl.Group.AUTHORIZED)) {
                grantee = GroupGrantee.AUTHENTICATED_USERS;
            } else if (group.equals(Acl.Group.OWNER)) {
                grantee = new CanonicalGrantee(owner.getId());
            } else {
                this.logger.error(String.format("Skip unknown group %s", group));
                continue;
            }
            s3Acl.grantPermission(grantee, perm);
        }
        try {
            putS3Acl(s3Acl);
        } catch (Exception e) {
            throw new FileSystemException(e);
        }
    }

    /**
	 * Special JetS3FileObject output stream.
	 * It saves all contents in temporary file, onClose sends contents to S3.
	 * 
	 * @author Marat Komarov
	 */
    private class S3OutputStream extends MonitorOutputStream {

        private S3Service service;

        private StorageObject object;

        public S3OutputStream(OutputStream out, S3Service service, StorageObject object) {
            super(out);
            this.service = service;
            this.object = object;
        }

        @Override
        protected void onClose() throws IOException {
            this.object.setContentType(Mimetypes.getInstance().getMimetype(this.object.getKey()));
            this.object.setDataInputStream(Channels.newInputStream(getCacheFileChannel()));
            try {
                this.service.putObject(this.object.getBucketName(), this.object);
            } catch (ServiceException e) {
                throw new IOException(e);
            }
        }
    }
}
