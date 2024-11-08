package ftraq.fs.local;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import ftraq.fs.*;
import ftraq.fs.exceptions.*;

/**
 * a file on the local file system.
 * 
 * @author <a href="mailto:jssauder@tfh-berlin.de">Steffen Sauder</a>
 * @version 1.0
 */
public class LocalFile implements LgFile {

    /** the absolute file name of this file */
    private String _absoluteFileName;

    /** the instance of the local file system this file belongs to */
    private LocalFileSystem _localFileSystem;

    /** the parent directory of this file */
    private LocalDirectory _parentDirectory;

    /** the time when this file's properties were updated */
    private long _timeOfLastUpdate;

    /** a String representation of this file's permissions */
    private String _permissions;

    private static ftraq.util.Logger logger = ftraq.util.Logger.getInstance(LocalFile.class);

    /**
     * create an LocalFile object with the given parent directory and file name
     * @param i_parentDirectory the parent directory of the file
     * @param i_fileName the file name (without leading path) of the file
     * @throws InvalidFileNameFailure if the file name contains invalid characters
     */
    LocalFile(LocalDirectory i_parentDirectory, String i_fileName) throws InvalidFileNameFailure {
        logger.info("a new LocalFile object with name '" + i_fileName + "' is being created in directory " + i_parentDirectory.getAbsoluteName());
        this._parentDirectory = i_parentDirectory;
        this._localFileSystem = (LocalFileSystem) i_parentDirectory.getFileSystem();
        java.io.File mappedLocalFile = new java.io.File(i_parentDirectory.getAbsoluteName(), i_fileName);
        this._absoluteFileName = mappedLocalFile.getAbsolutePath();
        this.updatePermissions();
    }

    public InputStreamWithReadLine getInputStream(boolean i_asciiMode) throws ReadFromSourceFileFailure {
        logger.info("an InputStream for File " + this.getAbsoluteName() + " was requested...");
        try {
            return new LocalFileInputStream(new java.io.FileInputStream(this.getMappedLocalFile()));
        } catch (java.io.FileNotFoundException ex) {
            throw new ReadFromSourceFileFailure(ex, this);
        } catch (RuntimeException e) {
            throw new ReadFromSourceFileFailure(e, this);
        }
    }

    public InputStreamWithReadLine getResumeInputStream(long i_indexOfFirstByte, boolean i_asciiMode) throws ReadFromSourceFileFailure, OperationNotSupportedExc {
        try {
            java.io.FileInputStream fileInputStream = new java.io.FileInputStream(this.getMappedLocalFile());
            fileInputStream.skip(i_indexOfFirstByte);
            return new LocalFileInputStream(fileInputStream);
        } catch (Exception e) {
            throw new ReadFromSourceFileFailure(e, this);
        }
    }

    public OutputStreamWithWriteLine getAppendOutputStream(long i_indexOfFirstByte, boolean i_asciiMode) throws WriteToTargetFileFailure, OperationNotSupportedExc {
        try {
            RandomAccessFileOutputStream outputStream = new RandomAccessFileOutputStream(this, i_indexOfFirstByte);
            return new LocalFileOutputStream(this, outputStream);
        } catch (Exception e) {
            throw new WriteToTargetFileFailure(e, this);
        }
    }

    public OutputStreamWithWriteLine getOutputStream(boolean i_asciiMode) throws WriteToTargetFileFailure, OperationNotSupportedExc {
        logger.info("an OutputStream to File " + this.getAbsoluteName() + " was requested...");
        try {
            return new LocalFileOutputStream(this, new java.io.FileOutputStream(this.getMappedLocalFile()));
        } catch (java.io.FileNotFoundException ex) {
            throw new WriteToTargetFileFailure(ex, this);
        } catch (RuntimeException e) {
            throw new WriteToTargetFileFailure(e, this);
        }
    }

    public void parentDirectoryHasChanged(LgDirectory i_newParent) {
        this._parentDirectory = (LocalDirectory) i_newParent;
        this._absoluteFileName = this._localFileSystem.constructAbsoluteFileName(this._parentDirectory, this.getName());
    }

    private java.io.File getMappedLocalFile() {
        return new java.io.File(this._absoluteFileName);
    }

    public boolean exists() throws RessourceAccessFailure {
        return this.getMappedLocalFile().exists();
    }

    public void delete() throws DeleteDirectoryEntryFailure, OperationNotSupportedExc {
        java.io.File mappedLocalFile = this.getMappedLocalFile();
        try {
            logger.info("deleting " + this.getAbsoluteName() + " from your local file system...");
            if (mappedLocalFile.exists() == false) {
                throw new NoSuchDirectoryEntryExc(this.getParentDirectory(), this.getName());
            }
            boolean success = mappedLocalFile.delete();
            if (success == false) {
                throw new PermissionDeniedExc(this);
            }
            this.getParentDirectory().removeChildDirectoryEntries(Lg_ListImpl.createLgDirectoryEntry_List(this));
            this.getFileSystem().removeDirectoryEntryFromCache(this.getAbsoluteName());
            logger.info("succesfully deleted " + this.getAbsoluteName() + " from your local file system...");
        } catch (Exception e) {
            throw new DeleteDirectoryEntryFailure(e, this);
        }
    }

    public void moveTo(LgDirectory i_targetDirectory) throws MoveDirectoryEntryFailure, OperationNotSupportedExc {
        logger.info("moving " + this.getAbsoluteName() + " to " + i_targetDirectory.getAbsoluteName());
        java.io.File mappedLocalFile = this.getMappedLocalFile();
        try {
            if (mappedLocalFile.exists() == false) {
                throw new NoSuchDirectoryEntryExc(this.getParentDirectory(), this.getName());
            }
            java.io.File newMappedLocalFile = new java.io.File(i_targetDirectory.getAbsoluteName(), this.getName());
            boolean success = mappedLocalFile.renameTo(newMappedLocalFile);
            if (success == false) {
                throw new PermissionDeniedExc("no success moving {0} to {1}", this, i_targetDirectory);
            }
            logger.debug("succesfully renamed file to " + newMappedLocalFile.getAbsolutePath() + ", create object...");
            ftraq.fs.LgFile newFile = this.getFileSystem().getFile(newMappedLocalFile.getCanonicalPath());
            logger.debug("created object for new file " + newFile.getAbsoluteName());
            this.getParentDirectory().removeChildDirectoryEntries(Lg_ListImpl.createLgDirectoryEntry_List(this));
            newFile.getParentDirectory().addChildDirectoryEntries(Lg_ListImpl.createLgDirectoryEntry_List(newFile));
            logger.info("sucesfully moved " + this.getAbsoluteName() + " to " + i_targetDirectory.getAbsoluteName());
            this.getFileSystem().removeDirectoryEntryFromCache(this.getAbsoluteName());
        } catch (FileSystemExc ex) {
            throw new MoveDirectoryEntryFailure(ex, this, i_targetDirectory);
        } catch (FileSystemFailure f) {
            throw new MoveDirectoryEntryFailure(f, this, i_targetDirectory);
        } catch (java.io.IOException e) {
            throw new MoveDirectoryEntryFailure(e, this, i_targetDirectory);
        } catch (RuntimeException e) {
            throw new MoveDirectoryEntryFailure(e, this, i_targetDirectory);
        }
    }

    public LgDirectory toDirectory() throws IsNoDirectoryExc {
        throw new IsNoDirectoryExc(this);
    }

    public LgFile toFile() throws IsNoFileExc {
        return this;
    }

    public String toString() {
        return this.getURL().toString();
    }

    public String getName() {
        return this.getMappedLocalFile().getName();
    }

    public String getAbsoluteName() {
        return this._absoluteFileName;
    }

    public String getURL() {
        return this._localFileSystem.getURLPrefix(false) + this.getMappedLocalFile().getAbsolutePath();
    }

    public String getURLwithPassword() {
        return this.getURL();
    }

    public LgDirectory getParentDirectory() throws HasNoParentDirectoryExc {
        logger.debug("looking for parent of file " + this.getAbsoluteName() + "...");
        if (this._parentDirectory == null) {
            throw new HasNoParentDirectoryExc(this);
        }
        logger.debug("the parent directory for file " + this.getAbsoluteName() + " is " + this._parentDirectory.getAbsoluteName());
        return this._parentDirectory;
    }

    public long getSize() throws InformationNotAvailableExc {
        if (this.getMappedLocalFile().exists() == false) {
            throw new InformationNotAvailableExc(this, "the file doesn't exist yet, no file size available");
        }
        return this.getMappedLocalFile().length();
    }

    public Date getDate() throws InformationNotAvailableExc {
        if (this.getMappedLocalFile().exists() == false) {
            throw new InformationNotAvailableExc(this, "the file doesn't exist yet, no date available");
        }
        return new Date(this.getMappedLocalFile().lastModified());
    }

    public long getTimeOfLastUpdate() {
        return this._timeOfLastUpdate;
    }

    public LgFileSystem getFileSystem() {
        return this._localFileSystem;
    }

    public void setName(String i_newName) throws RenameDirectoryEntryFailure, OperationNotSupportedExc {
        logger.info("renaming " + this.getAbsoluteName() + " to " + i_newName);
        java.io.File mappedLocalFile = this.getMappedLocalFile();
        try {
            if (mappedLocalFile.exists() == false) {
                throw new NoSuchDirectoryEntryExc(this.getParentDirectory(), i_newName);
            }
            java.io.File newMappedLocalFile = new java.io.File(this.getParentDirectory().getAbsoluteName(), i_newName);
            if (newMappedLocalFile.exists()) {
                throw new DirectoryEntryAlreadyExistsExc(this.getParentDirectory(), i_newName);
            }
            boolean success = mappedLocalFile.renameTo(newMappedLocalFile);
            if (success == false) {
                throw new PermissionDeniedExc(this.getAbsoluteName(), "failed to rename to " + i_newName);
            }
            mappedLocalFile = newMappedLocalFile;
            String oldAbsoluteName = this._absoluteFileName;
            this._absoluteFileName = mappedLocalFile.getAbsolutePath();
            this._localFileSystem.absoluteNameOfDirectoryEntryChanged(this, oldAbsoluteName);
            this.getParentDirectory().updateChildDirectoryEntries(Lg_ListImpl.createLgDirectoryEntry_List(this));
            logger.info("succesfully renamed to " + this.getAbsoluteName());
        } catch (FileSystemExc ex) {
            throw new RenameDirectoryEntryFailure(ex, this, i_newName);
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

    void updatePermissions() {
        boolean canRead = this.getMappedLocalFile().canRead();
        boolean canWrite = this.getMappedLocalFile().canWrite();
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
     * this class is an Adapter used for writing to any position of a file
     * via java.io.RandomAccessFile
     */
    class RandomAccessFileOutputStream extends java.io.OutputStream {

        /** the file that should be written into */
        private java.io.RandomAccessFile _randomAccessFile;

        RandomAccessFileOutputStream(LocalFile i_file, long i_indexOfFirstByteToRead) throws java.io.FileNotFoundException, java.io.IOException {
            this._randomAccessFile = new java.io.RandomAccessFile(i_file.getMappedLocalFile(), "rw");
            this._randomAccessFile.seek(i_indexOfFirstByteToRead);
        }

        public void write(int i_byteToWrite) throws java.io.IOException {
            this._randomAccessFile.write(i_byteToWrite);
        }

        public void write(byte[] i_bytesToWrite) throws java.io.IOException {
            this._randomAccessFile.write(i_bytesToWrite);
        }

        public void write(byte[] i_bytesToWrite, int i_offset, int i_length) throws java.io.IOException {
            this._randomAccessFile.write(i_bytesToWrite, i_offset, i_length);
        }

        public void close() throws java.io.IOException {
            this._randomAccessFile.close();
        }
    }
}
