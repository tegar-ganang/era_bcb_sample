package ftraq.fs.local;

import ftraq.fs.*;
import ftraq.fs.exceptions.*;
import java.util.Date;

/**
 * a link on the local file system.
 * 
 * @author <a href="mailto:jssauder@tfh-berlin.de">Steffen Sauder</a>
 * @version 1.0
 */
public class LocalLink implements LgLink {

    /** the Java-File-Object for the link */
    private java.io.File _mappedLinkFileObject;

    /** the Java-File-Object for the Link target */
    private java.io.File _mappedTargetFileObject;

    /** the link target */
    private LgDirectoryEntry _linkTarget;

    /** the file system that this link belongs to */
    private LocalFileSystem _localFileSystem;

    /** the parent directory of this link */
    private LocalDirectory _parentDirectory;

    /** the date when this link was created or modified */
    private java.util.Date _linkDate;

    /** the size of this link */
    private long _linkSize = -1;

    /** a String representation of this link's permissions */
    private String _permissionsString;

    private static ftraq.util.Logger logger = ftraq.util.Logger.getInstance(LocalLink.class);

    public LocalLink(LocalDirectory i_parentDirectory, java.io.File i_linkFileObject, String i_targetAbsoluteName) {
        this._parentDirectory = i_parentDirectory;
        logger.info("creating a link to '" + i_targetAbsoluteName + "' named '" + i_linkFileObject.getAbsolutePath() + "'...");
        this._mappedLinkFileObject = i_linkFileObject;
        logger.info("relative name is " + this._mappedLinkFileObject.getName());
        this._localFileSystem = (LocalFileSystem) i_parentDirectory.getFileSystem();
        this._mappedTargetFileObject = new java.io.File(i_targetAbsoluteName);
        this.updatePermissions();
    }

    public LgDirectoryEntry getLinkTarget() {
        if (this._linkTarget == null) {
            try {
                this._linkTarget = this._localFileSystem.getDirectoryEntry(this._mappedTargetFileObject.getAbsolutePath());
            } catch (Exception e) {
                logger.error("getting the link target " + this._mappedTargetFileObject.getAbsolutePath() + " failed!", e);
                this._linkTarget = null;
            }
        }
        return this._linkTarget;
    }

    public String getName() {
        return this._mappedLinkFileObject.getName();
    }

    public void setName(String i_newName) throws RenameDirectoryEntryFailure, OperationNotSupportedExc {
        throw new OperationNotSupportedExc("renaming the link {0} is not supported yet", this);
    }

    public String getPermissions() throws GetPermissionsFailure, OperationNotSupportedExc {
        return this._permissionsString;
    }

    public void setPermissions(String i_newPermissions) throws SetPermissionsFailure, OperationNotSupportedExc {
        throw new OperationNotSupportedExc("changing the permissions of the link {0} is not supported yet", this);
    }

    public String getAbsoluteName() {
        return this._mappedLinkFileObject.getAbsolutePath();
    }

    public String getURL() {
        return this._localFileSystem.getURLPrefix(false) + this.getAbsoluteName();
    }

    public String getURLwithPassword() {
        return this.getURL();
    }

    public LgDirectory getParentDirectory() throws HasNoParentDirectoryExc {
        if (this._parentDirectory == null) {
            throw new HasNoParentDirectoryExc(this);
        }
        return this._parentDirectory;
    }

    public long getSize() throws InformationNotAvailableExc {
        return this._mappedLinkFileObject.length();
    }

    public Date getDate() throws InformationNotAvailableExc {
        return new Date(this._mappedLinkFileObject.lastModified());
    }

    public LgFileSystem getFileSystem() {
        return this._localFileSystem;
    }

    public long getTimeOfLastUpdate() {
        throw new java.lang.UnsupportedOperationException("Method getTimeOfLastUpdate() not yet implemented.");
    }

    public boolean exists() throws RessourceAccessFailure {
        return this._mappedLinkFileObject.exists();
    }

    public void delete() throws DeleteDirectoryEntryFailure, OperationNotSupportedExc {
        throw new OperationNotSupportedExc("deleting the link {0} is not supported yet", this);
    }

    public void moveTo(LgDirectory i_targetDirectory) throws MoveDirectoryEntryFailure, OperationNotSupportedExc {
        throw new OperationNotSupportedExc("moving the link {0} is not supported yet", this);
    }

    public void parentDirectoryHasChanged(LgDirectory i_newParent) {
        this._parentDirectory = (LocalDirectory) i_newParent;
        this._mappedLinkFileObject = new java.io.File(this._localFileSystem.constructAbsoluteFileName(this._parentDirectory, this.getName()));
    }

    public LgDirectory toDirectory() throws IsNoDirectoryExc {
        LgDirectoryEntry target = this.getLinkTarget();
        if (target == null) {
            throw new IsNoDirectoryExc(this);
        }
        return target.toDirectory();
    }

    public LgFile toFile() throws IsNoFileExc {
        LgDirectoryEntry target = this.getLinkTarget();
        if (target == null) {
            throw new IsNoFileExc(this);
        }
        return target.toFile();
    }

    public String toString() {
        return this._mappedLinkFileObject.getAbsolutePath();
    }

    void updatePermissions() {
        boolean canRead = this._mappedLinkFileObject.canRead();
        boolean canWrite = this._mappedLinkFileObject.canWrite();
        if (canRead && canWrite) {
            this._permissionsString = "read/write";
        }
        if (canRead && !canWrite) {
            this._permissionsString = "read-only";
        }
        if (!canRead && canWrite) {
            this._permissionsString = "write-only";
        }
        if (!canRead && !canWrite) {
            this._permissionsString = "no access";
        }
    }
}
