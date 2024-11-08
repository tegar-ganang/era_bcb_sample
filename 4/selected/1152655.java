package ftraq.fs.local;

import java.io.File;
import java.util.List;
import java.net.URL;
import java.util.Date;
import ftraq.fs.*;
import ftraq.fs.exceptions.*;

/**
 * a directory on the local file system.
 * 
 * @author <a href="mailto:jssauder@tfh-berlin.de">Steffen Sauder</a>
 * @version 1.0
 */
public class LocalDirectory implements LgDirectory {

    /** a List of all Listeners who should be informed when the content of this directory changed */
    private LgDirectoryContentChangedListener_List _directoryContentChangedListenerList;

    /** a List containg the LgDirectoryEntry-Objects of all children from this directory */
    private LgDirectoryEntry_List _directoryEntriesList;

    /** the instance of the local file system this directory belongs to */
    private LocalFileSystem _localFileSystem;

    /** the parent directory of this directory or null if it is a root directory */
    private LocalDirectory _parentDirectory;

    /** the absolute pathname of this directory */
    private String _absoluteDirectoryName;

    /** the long value of the time when this directories's listing was updated */
    private long _timeOfLastUpdate;

    /** a String representation of this directories' permissions */
    private String _permissions;

    private static ftraq.util.Logger logger = ftraq.util.Logger.getInstance(LocalDirectory.class);

    LocalDirectory(LocalDirectory i_parentDirectory, String i_fileName) throws IsNoDirectoryExc, InvalidFileNameFailure {
        logger.info("a new LocalDirectory object with name '" + i_fileName + "' is being created in " + i_parentDirectory.getAbsoluteName());
        this._localFileSystem = (LocalFileSystem) i_parentDirectory.getFileSystem();
        this._parentDirectory = i_parentDirectory;
        java.io.File mappedLocalDirectory = new java.io.File(i_parentDirectory.getAbsoluteName(), i_fileName);
        if (mappedLocalDirectory.exists() && mappedLocalDirectory.isDirectory() == false) {
            throw new IsNoDirectoryExc(i_parentDirectory, i_fileName);
        }
        this._absoluteDirectoryName = mappedLocalDirectory.getAbsolutePath();
        this.updatePermissions();
    }

    LocalDirectory(LocalFileSystem i_fileSystem, String i_rootDirectoryName) throws IsNoDirectoryExc, InvalidFileNameFailure {
        logger.info("a new LocalDirectory object with name '" + i_rootDirectoryName + "' is being created as a root directory");
        this._localFileSystem = (LocalFileSystem) i_fileSystem;
        this._parentDirectory = null;
        java.io.File mappedLocalDirectory = new java.io.File(i_rootDirectoryName);
        if (mappedLocalDirectory.isDirectory() == false) {
            throw new IsNoDirectoryExc(i_fileSystem, i_rootDirectoryName);
        }
        this._absoluteDirectoryName = mappedLocalDirectory.getAbsolutePath();
    }

    public LgDirectoryEntry getDirectoryEntry(String i_fileName) throws NoSuchDirectoryEntryExc, GetDirectoryContentFailure {
        try {
            logger.info("the directory entry named '" + i_fileName + "' in " + this.getAbsoluteName() + " is being requested...");
            this._updateDirectoryEntriesList();
            synchronized (this._directoryEntriesList) {
                java.util.Iterator it = this._directoryEntriesList.iterator();
                while (it.hasNext()) {
                    LgDirectoryEntry entry = (LgDirectoryEntry) it.next();
                    if (entry.getName().equals(i_fileName)) {
                        logger.info("returning " + entry.getAbsoluteName());
                        return entry;
                    }
                }
            }
        } catch (FileSystemFailure f) {
            throw new GetDirectoryContentFailure(f, this);
        } catch (RuntimeException e) {
            throw new GetDirectoryContentFailure(e, this);
        }
        throw new NoSuchDirectoryEntryExc(this, i_fileName);
    }

    public LgDirectory createSubDirectory(String i_directoryName) throws CreateDirectoryFailure {
        try {
            logger.info("creating subdirectory '" + i_directoryName + "' in " + this.getAbsoluteName() + "...");
            if (this._directoryEntriesList == null) {
                this._updateDirectoryEntriesList();
            }
            String newAbsoluteFileName = this._localFileSystem.constructAbsoluteFileName(this, i_directoryName);
            LocalDirectory newDirectory = (LocalDirectory) this._localFileSystem.getDirectory(newAbsoluteFileName);
            java.io.File fileObjectForNewDirectory = newDirectory._getMappedLocalDirectory();
            if (fileObjectForNewDirectory.exists()) {
                throw new DirectoryEntryAlreadyExistsExc(newDirectory);
            }
            boolean success = fileObjectForNewDirectory.mkdir();
            newDirectory.updatePermissions();
            if (success == false) {
                throw new PermissionDeniedExc(newDirectory);
            }
            this.addChildDirectoryEntries(Lg_ListImpl.createLgDirectoryEntry_List(newDirectory));
            logger.info("succesfully created subdirectory " + newDirectory.getAbsoluteName());
            return newDirectory;
        } catch (FileSystemFailure f) {
            throw new CreateDirectoryFailure(f, this, i_directoryName);
        } catch (FileSystemExc ex2) {
            throw new CreateDirectoryFailure(ex2, this, i_directoryName);
        } catch (RuntimeException e) {
            throw new CreateDirectoryFailure(e, this, i_directoryName);
        }
    }

    public LgFile createFile(String i_newFileName) throws DirectoryEntryAlreadyExistsExc, CreateFileFailure {
        try {
            if (this._directoryEntriesList == null) {
                this._updateDirectoryEntriesList();
            }
            String newAbsoluteFileName = this.getAbsoluteName() + this.getFileSystem().getPathSeparator() + i_newFileName;
            LocalFile newFile = (LocalFile) this.getFileSystem().getFile(newAbsoluteFileName);
            if (newFile.exists()) {
                throw new DirectoryEntryAlreadyExistsExc(this, i_newFileName);
            }
            return newFile;
        } catch (DirectoryEntryAlreadyExistsExc e) {
            throw e;
        } catch (Exception e) {
            throw new CreateFileFailure(e, this, i_newFileName);
        }
    }

    public void clearCachedDirectoryListing() {
        this._directoryEntriesList = null;
    }

    public void addDirectoryContentChangedListener(LgDirectoryContentChangedListener i_listener) {
        logger.info("adding content changed listener to " + this.getAbsoluteName());
        if (this._directoryContentChangedListenerList == null) {
            this._directoryContentChangedListenerList = Lg_ListImpl.createLgDirectoryContentChangedListener_List();
        }
        synchronized (this._directoryContentChangedListenerList) {
            this._directoryContentChangedListenerList.add(i_listener);
        }
    }

    public void removeDirectoryContentChangedListener(LgDirectoryContentChangedListener i_listener) {
        logger.info("removing content changed listener from " + this.getAbsoluteName());
        if (this._directoryContentChangedListenerList == null) {
            this._directoryContentChangedListenerList = Lg_ListImpl.createLgDirectoryContentChangedListener_List();
        }
        synchronized (this._directoryContentChangedListenerList) {
            this._directoryContentChangedListenerList.remove(i_listener);
            if (this._directoryContentChangedListenerList.size() == 0) {
                this._directoryContentChangedListenerList = null;
            }
        }
    }

    public void addChildDirectoryEntries(LgDirectoryEntry_List i_addedEntries) {
        if (this._directoryEntriesList == null) return;
        logger.info(i_addedEntries.size() + " directory entries were added to " + this.getAbsoluteName());
        for (int i = 0; i < i_addedEntries.size(); i++) {
            LgDirectoryEntry newEntry = (LgDirectoryEntry) i_addedEntries.get(i);
            if (this._directoryEntriesList.contains(newEntry)) {
                this._directoryEntriesList.remove(newEntry);
            }
            this._directoryEntriesList.add(newEntry);
        }
        if (this._directoryContentChangedListenerList != null) {
            java.util.Iterator it = this._directoryContentChangedListenerList.iterator();
            while (it.hasNext()) {
                LgDirectoryContentChangedListener listener = (LgDirectoryContentChangedListener) it.next();
                listener.directoryEntriesWereAdded(this, i_addedEntries, this._directoryEntriesList);
            }
        }
    }

    public void updateChildDirectoryEntries(LgDirectoryEntry_List i_changedEntries) {
        if (this._directoryEntriesList == null) return;
        logger.info(i_changedEntries.size() + " directory entries were changed in " + this.getAbsoluteName());
        if (this._directoryContentChangedListenerList != null) {
            java.util.Iterator it = this._directoryContentChangedListenerList.iterator();
            while (it.hasNext()) {
                LgDirectoryContentChangedListener listener = (LgDirectoryContentChangedListener) it.next();
                listener.directoryEntriesWereChanged(this, i_changedEntries, this._directoryEntriesList);
            }
        }
    }

    public void removeChildDirectoryEntries(LgDirectoryEntry_List i_removedEntries) {
        if (this._directoryEntriesList == null) return;
        logger.info(i_removedEntries.size() + " directory entries were removed from " + this.getAbsoluteName());
        for (int i = 0; i < i_removedEntries.size(); i++) {
            this._directoryEntriesList.remove(i_removedEntries.get(i));
        }
        if (this._directoryContentChangedListenerList != null) {
            java.util.Iterator it = this._directoryContentChangedListenerList.iterator();
            while (it.hasNext()) {
                LgDirectoryContentChangedListener listener = (LgDirectoryContentChangedListener) it.next();
                listener.directoryEntriesWereRemoved(this, i_removedEntries, this._directoryEntriesList);
            }
        }
    }

    public String getName() {
        try {
            if (this._localFileSystem.isRootDirectoryName(this.getAbsoluteName())) {
                return this.getAbsoluteName();
            }
            return this._localFileSystem.constructFileNameWithoutPath(this._absoluteDirectoryName);
        } catch (InvalidFileNameFailure f) {
            return this._getMappedLocalDirectory().getAbsolutePath();
        }
    }

    public void setName(String i_newName) throws RenameDirectoryEntryFailure, OperationNotSupportedExc {
        logger.info("renaming " + this.getAbsoluteName() + " to '" + i_newName + "'");
        java.io.File mappedLocalDirectory = this._getMappedLocalDirectory();
        try {
            if (mappedLocalDirectory.exists() == false) {
                throw new NoSuchDirectoryEntryExc(this.getParentDirectory(), i_newName);
            }
            java.io.File newMappedLocalDirectory = new java.io.File(this.getParentDirectory().getAbsoluteName(), i_newName);
            if (newMappedLocalDirectory.exists()) {
                throw new DirectoryEntryAlreadyExistsExc(this.getParentDirectory(), i_newName);
            }
            boolean success = mappedLocalDirectory.renameTo(newMappedLocalDirectory);
            if (success == false) {
                throw new PermissionDeniedExc(this.getAbsoluteName(), "failed to rename to " + i_newName);
            }
            this._absoluteDirectoryName = newMappedLocalDirectory.getAbsolutePath();
            this._localFileSystem.removeDirectoryEntryFromCache(mappedLocalDirectory.getAbsolutePath());
            this._localFileSystem.replaceDirectoryEntry(this.getAbsoluteName(), this);
            this.getParentDirectory().updateChildDirectoryEntries(Lg_ListImpl.createLgDirectoryEntry_List(this));
            if (this._directoryEntriesList != null) {
                synchronized (this._directoryEntriesList) {
                    java.util.Iterator it = this._directoryEntriesList.iterator();
                    while (it.hasNext()) {
                        LgDirectoryEntry entry = (LgDirectoryEntry) it.next();
                        entry.parentDirectoryHasChanged(this);
                    }
                }
            }
            logger.info("succesfully renamed directory to '" + this.getAbsoluteName() + "'");
        } catch (FileSystemExc e) {
            throw new RenameDirectoryEntryFailure(e, this, i_newName);
        } catch (RuntimeException e) {
            throw new RenameDirectoryEntryFailure(e, this, i_newName);
        }
    }

    public String getPermissions() throws GetPermissionsFailure, OperationNotSupportedExc {
        return this._permissions;
    }

    public void setPermissions(String i_newPermissions) throws SetPermissionsFailure, OperationNotSupportedExc {
        throw new OperationNotSupportedExc("java doesn't allow to change permissions on your local file system", this);
    }

    public String getURL() {
        return this._localFileSystem.getURLPrefix(false) + this._getMappedLocalDirectory().getAbsolutePath();
    }

    public String getAbsoluteName() {
        return this._absoluteDirectoryName;
    }

    public String getURLwithPassword() {
        return this.getURL();
    }

    public LgDirectory getParentDirectory() throws HasNoParentDirectoryExc {
        logger.debug("looking for parent of directory " + this.getAbsoluteName() + "...");
        if (this._parentDirectory == null) {
            throw new HasNoParentDirectoryExc(this);
        }
        logger.debug("the parent directory is " + this._parentDirectory.getAbsoluteName());
        return this._parentDirectory;
    }

    public long getSize() throws InformationNotAvailableExc {
        throw new InformationNotAvailableExc(this, "the size for local directories can't be calculated yet");
    }

    public Date getDate() throws InformationNotAvailableExc {
        return new Date(this._getMappedLocalDirectory().lastModified());
    }

    public long getTimeOfLastUpdate() {
        return this._timeOfLastUpdate;
    }

    public LgFileSystem getFileSystem() {
        return this._localFileSystem;
    }

    public boolean exists() throws RessourceAccessFailure {
        return this._getMappedLocalDirectory().exists();
    }

    public void delete() throws DeleteDirectoryEntryFailure, OperationNotSupportedExc {
        try {
            logger.info("trying to delete " + this.getAbsoluteName() + "...");
            java.io.File mappedLocalDirectory = this._getMappedLocalDirectory();
            if (this._directoryEntriesList == null) {
                this.getDirectoryListing();
            }
            if (mappedLocalDirectory.exists() == false) {
                throw new NoSuchDirectoryEntryExc(this.getParentDirectory(), this.getName());
            }
            if (this._directoryEntriesList.size() > 0) {
                throw new OperationNotSupportedExc("deleting of the non-empty directory {0} is not supported", this);
            }
            boolean success = mappedLocalDirectory.delete();
            if (success == false) {
                throw new PermissionDeniedExc(this);
            }
            this.getParentDirectory().removeChildDirectoryEntries(Lg_ListImpl.createLgDirectoryEntry_List(this));
            this.getFileSystem().removeDirectoryEntryFromCache(this.getAbsoluteName());
            logger.info("succesfully deleted " + this.getAbsoluteName());
        } catch (FileSystemFailure f) {
            throw new DeleteDirectoryEntryFailure(f, this);
        } catch (FileSystemExc ex) {
            throw new DeleteDirectoryEntryFailure(ex, this);
        } catch (RuntimeException e) {
            throw new DeleteDirectoryEntryFailure(e, this);
        }
    }

    public void moveTo(LgDirectory i_targetDirectory) throws MoveDirectoryEntryFailure, OperationNotSupportedExc {
        logger.info("moving " + this.getAbsoluteName() + " to " + i_targetDirectory.getAbsoluteName());
        java.io.File mappedLocalDirectory = this._getMappedLocalDirectory();
        try {
            if (mappedLocalDirectory.exists() == false) {
                throw new NoSuchDirectoryEntryExc(this.getParentDirectory(), this.getName());
            }
            if (this.equals(i_targetDirectory)) {
                throw new OperationNotSupportedExc("cannot move {0} to itself", this);
            }
            this.getParentDirectory();
            java.io.File newMappedLocalDirectory = new java.io.File(i_targetDirectory.getAbsoluteName(), this.getName());
            boolean success = mappedLocalDirectory.renameTo(newMappedLocalDirectory);
            if (success == false) {
                throw new PermissionDeniedExc("no success moving {0} to {1}, probably permission denied", this, i_targetDirectory);
            }
            logger.debug("succesfully renamed file to " + newMappedLocalDirectory.getAbsolutePath() + ", create object...");
            this.getParentDirectory().removeChildDirectoryEntries(Lg_ListImpl.createLgDirectoryEntry_List(this));
            i_targetDirectory.addChildDirectoryEntries(Lg_ListImpl.createLgDirectoryEntry_List(this));
            logger.info("succesfully moved " + this.getAbsoluteName() + " to " + i_targetDirectory.getAbsoluteName());
            this.parentDirectoryHasChanged(i_targetDirectory);
        } catch (FileSystemExc ex) {
            throw new MoveDirectoryEntryFailure(ex, this, i_targetDirectory);
        } catch (RuntimeException e) {
            throw new MoveDirectoryEntryFailure(e, this, i_targetDirectory);
        }
    }

    public void parentDirectoryHasChanged(LgDirectory i_newParent) {
        String oldAbsoluteName = this._absoluteDirectoryName;
        this._parentDirectory = (LocalDirectory) i_newParent;
        this._absoluteDirectoryName = this._localFileSystem.constructAbsoluteFileName(this._parentDirectory, this.getName());
        this._localFileSystem.absoluteNameOfDirectoryEntryChanged(this, oldAbsoluteName);
        if (this._directoryEntriesList != null) {
            synchronized (this._directoryEntriesList) {
                java.util.Iterator it = this._directoryEntriesList.iterator();
                LgDirectoryEntry nextEntry = (LgDirectoryEntry) it.next();
                if (nextEntry instanceof LgDirectory) {
                    ((LgDirectory) nextEntry).parentDirectoryHasChanged(this);
                }
            }
        }
    }

    public LgDirectoryEntry_List getDirectoryListing() throws GetDirectoryContentFailure {
        try {
            logger.info("directory content of " + this.getAbsoluteName() + " is being requested...");
            this._updateDirectoryEntriesList();
            logger.info("return directory content of " + this.getAbsoluteName() + "...");
            synchronized (this._directoryEntriesList) {
                return Lg_ListImpl.createLgDirectoryEntry_List(this._directoryEntriesList);
            }
        } catch (GetDirectoryContentFailure f) {
            throw f;
        } catch (FileSystemFailure f) {
            throw new GetDirectoryContentFailure(f, this);
        } catch (RuntimeException e) {
            throw new GetDirectoryContentFailure(e, this);
        }
    }

    public LgDirectoryEntry_List getRecursiveDirectoryListing() throws GetDirectoryContentFailure {
        logger.warn("recursing the directory " + this.getAbsoluteName());
        LgDirectoryEntry_List recursedList = Lg_ListImpl.createLgDirectoryEntry_List();
        java.util.Iterator it = this.getDirectoryListing().iterator();
        while (it.hasNext()) {
            LgDirectoryEntry nextEntry = (LgDirectoryEntry) it.next();
            logger.warn("adding " + nextEntry.getAbsoluteName());
            recursedList.add(nextEntry);
            try {
                LgDirectory subDirectory = nextEntry.toDirectory();
                recursedList.addAll(subDirectory.getRecursiveDirectoryListing());
            } catch (IsNoDirectoryExc e) {
            }
        }
        logger.warn("finished recursing " + this.getAbsoluteName());
        return recursedList;
    }

    public LgDirectory toDirectory() throws IsNoDirectoryExc {
        return this;
    }

    public ftraq.fs.LgFile toFile() throws IsNoFileExc {
        throw new IsNoFileExc(this);
    }

    public String toString() {
        return this.getURL().toString();
    }

    void updatePermissions() {
        java.io.File mappedLocalDirectory = new java.io.File(this._absoluteDirectoryName);
        boolean canRead = mappedLocalDirectory.canRead();
        boolean canWrite = mappedLocalDirectory.canWrite();
        if (canRead && canWrite) {
            this._permissions = "read/write";
        }
        if (canRead && !canWrite) {
            this._permissions = "read-only";
        }
        if (!canRead && canWrite) {
            this._permissions = "write-only";
        }
        if (!canRead && !canWrite) {
            this._permissions = "no access";
        }
    }

    /**
     * @return the java.io.File object for this local directory
     */
    private File _getMappedLocalDirectory() {
        return new java.io.File(this.getAbsoluteName());
    }

    private void _updateDirectoryEntriesList() throws GetDirectoryContentFailure {
        logger.info("updating the directory content of " + this.getAbsoluteName());
        try {
            LgDirectoryEntry_List newDirectoryEntriesList = Lg_ListImpl.createLgDirectoryEntry_List();
            java.io.File[] allChildFiles = this._getMappedLocalDirectory().listFiles();
            if (allChildFiles == null) {
                throw new PermissionDeniedExc(this.getAbsoluteName(), "failed to list directory");
            }
            for (int i = 0; i < allChildFiles.length; i++) {
                LgDirectoryEntry newEntry;
                String canonicalPath = allChildFiles[i].getCanonicalPath();
                String absolutePath = allChildFiles[i].getAbsolutePath();
                if (canonicalPath.equals(absolutePath) == false) {
                    logger.info("saw the link " + absolutePath + " in a directory listing, try to get it's object...");
                    newEntry = this._localFileSystem.getDirectoryEntry(absolutePath);
                } else {
                    if (allChildFiles[i].isDirectory()) {
                        try {
                            newEntry = this._localFileSystem.getDirectory(absolutePath);
                        } catch (IsNoDirectoryExc ex2) {
                            throw new GetDirectoryContentFailure(ex2, this);
                        } catch (GetDirectoryEntryFailure f) {
                            throw new GetDirectoryContentFailure(f, this);
                        }
                    } else {
                        try {
                            newEntry = this._localFileSystem.getFile(absolutePath);
                        } catch (IsNoFileExc ex2) {
                            throw new GetDirectoryContentFailure(ex2, this);
                        } catch (GetDirectoryEntryFailure f) {
                            throw new GetDirectoryContentFailure(f, this);
                        }
                    }
                }
                newDirectoryEntriesList.add(newEntry);
            }
            if (this._directoryEntriesList == null) {
                this._directoryEntriesList = newDirectoryEntriesList;
            } else {
                synchronized (this._directoryEntriesList) {
                    this._directoryEntriesList = newDirectoryEntriesList;
                }
            }
            this._timeOfLastUpdate = new java.util.Date().getTime();
            logger.info("succesfully updated the directory content of " + this.getAbsoluteName());
        } catch (GetDirectoryContentFailure f) {
            throw f;
        } catch (Exception e) {
            throw new GetDirectoryContentFailure(e, this);
        }
    }
}
