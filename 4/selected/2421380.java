package supersync.tree2;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import org.jdesktop.application.ResourceMap;

/** This class represents a tree like structure which is file backed.  This class is useful for very large tree structures that you would not want to store in memory.
 *
 * @author Brandon Drake
 */
public abstract class TreeFile {

    public static final int CURRENT_FILE_VERSION = 3;

    private static final ResourceMap RESMAP = org.jdesktop.application.Application.getInstance(supersync.SynchronizerApp.class).getContext().getResourceMap(TreeFile.class);

    protected RandomAccessFile file = null;

    protected int fileInfoLocation = 0;

    protected int fileVersion = 0;

    protected java.util.Timer saveTimer = new java.util.Timer(true);

    protected int startOfTree = 8;

    protected File treeFile = null;

    /** This timer allows automatically saving the file periodically. */
    java.util.TimerTask saveTimerTask = new java.util.TimerTask() {

        @Override
        public void run() {
            synchronized (TreeFile.this) {
                if (null != file) {
                    try {
                        file.getFD().sync();
                    } catch (IOException ex) {
                    }
                }
            }
        }
    };

    /** This function closes the file.
     */
    public synchronized void close() throws IOException {
        file.close();
        file = null;
        saveTimer.cancel();
    }

    /** This function creates a new tree file.  There should be no file open currently.
     */
    public synchronized TreeFileLeaf createNew(File l_file, EditableLeaf l_root) throws IOException {
        if (file != null) {
            throw new IOException(RESMAP.getString("message.cannotCreateFromExistingFile.text"));
        }
        if (l_file.exists()) {
            l_file.delete();
        }
        treeFile = l_file;
        file = new RandomAccessFile(l_file, "rw");
        saveTimer.schedule(saveTimerTask, 300000, 300000);
        fileVersion = CURRENT_FILE_VERSION;
        file.writeInt(CURRENT_FILE_VERSION);
        file.writeInt(0);
        file.writeByte(TreeFileLeaf.ENTRY_TYPE_FILE_LIST);
        file.writeInt(1);
        file.writeInt((int) file.getChannel().position() + 4);
        TreeFileLeaf result = new TreeFileLeaf(this);
        result.entryType = TreeFileLeaf.ENTRY_TYPE_FOLDER;
        result.positionInFile = (int) file.getChannel().position();
        result.positionOfFileList = TreeFileLeaf.NO_FILES_MARKER;
        result.leaf = l_root;
        file.writeByte(result.entryType);
        file.writeInt(result.positionOfFileList);
        byte[] leafValue = l_root.getLeafValue();
        file.writeInt(leafValue.length);
        file.write(leafValue);
        return result;
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
        close();
    }

    /** Gets a data array of file information.  This value can be used for anything.
     *
     * @return The file info or null if this file version does not support file info or if it is not set.
     */
    protected byte[] getFileInfo() throws IOException {
        if (fileInfoLocation <= 0) {
            return null;
        }
        this.file.seek(this.fileInfoLocation);
        int bytesToRead = this.file.readInt();
        if (bytesToRead < 0 || 100000 < bytesToRead) {
            throw new IOException(RESMAP.getString("message.invalidFileInfoLength.text"));
        }
        byte[] fileInfo = new byte[bytesToRead];
        file.read(fileInfo);
        return fileInfo;
    }

    /** Gets a new leaf from the specified leaf value.
     */
    protected abstract EditableLeaf getNewLeaf(byte[] l_leafValue, TreeFileLeaf l_treeFileLeaf) throws java.text.ParseException;

    /** Gets the base or root of the tree file.
     */
    public synchronized TreeFileLeaf getRoot() throws IOException {
        TreeFileLeaf[] baseFiles;
        try {
            baseFiles = this.getFileList(startOfTree);
        } catch (ParseException ex) {
            throw new IOException(RESMAP.getString("message.unableToParse.text"));
        }
        return baseFiles[0];
    }

    /** Gets the actual file where the tree file is saving data to.
     */
    public File getFile() {
        return treeFile;
    }

    /** Opens the tree file.  If the file is the wrong version an error will be thrown.
     */
    public synchronized void open(File l_file) throws IOException {
        treeFile = l_file;
        file = new RandomAccessFile(l_file, "rw");
        saveTimer.schedule(saveTimerTask, 300000, 300000);
        fileVersion = file.readInt();
        if (fileVersion < 2 || CURRENT_FILE_VERSION < fileVersion) {
            throw new IOException(RESMAP.getString("message.invalidVersion.text"));
        }
        if (2 == fileVersion) {
            startOfTree = 4;
        } else if (2 < fileVersion) {
            startOfTree = 8;
            fileInfoLocation = file.readInt();
        }
    }

    /** Gets a list of tree file leafs from the file list, from the file list at the specified location.
     */
    protected synchronized TreeFileLeaf[] getFileList(int l_locationOfList) throws IOException, ParseException {
        this.file.seek(l_locationOfList);
        byte entryType = skipToEntry();
        if (entryType != TreeFileLeaf.ENTRY_TYPE_FILE_LIST) {
            throw new IOException(RESMAP.getString("message.unableToParse.text"));
        }
        int numberOfFiles = this.file.readInt();
        int[] subFileLocations = new int[numberOfFiles];
        for (int fileIndex = 0; fileIndex < numberOfFiles; fileIndex++) {
            subFileLocations[fileIndex] = this.file.readInt();
        }
        boolean positionListUpdated = false;
        TreeFileLeaf[] result = new TreeFileLeaf[numberOfFiles];
        for (int fileIndex = 0; fileIndex < numberOfFiles; fileIndex++) {
            result[fileIndex] = this.getFile(subFileLocations[fileIndex]);
            if (subFileLocations[fileIndex] != result[fileIndex].positionInFile) {
                subFileLocations[fileIndex] = result[fileIndex].positionInFile;
                positionListUpdated = true;
            }
        }
        if (positionListUpdated) {
            this.file.seek(l_locationOfList + 1);
            for (int fileIndex = 0; fileIndex < numberOfFiles; fileIndex++) {
                this.file.writeInt(subFileLocations[fileIndex]);
            }
        }
        return result;
    }

    /** Gets a tree file leaf from the file or folder entry at the specified position.
     */
    protected synchronized TreeFileLeaf getFile(int l_position) throws IOException, ParseException {
        this.file.seek(l_position);
        byte entryType = skipToEntry();
        TreeFileLeaf result = new TreeFileLeaf(this);
        result.positionInFile = l_position;
        result.entryType = entryType;
        if (result.entryType == TreeFileLeaf.ENTRY_TYPE_FOLDER) {
            result.positionOfFileList = file.readInt();
        } else {
            result.positionOfFileList = TreeFileLeaf.FILE_MARKER;
        }
        int lengthOfEntry = file.readInt();
        byte[] entry = new byte[lengthOfEntry];
        file.read(entry);
        result.leaf = getNewLeaf(entry, result);
        return result;
    }

    /** Sets a data array of file information.  This value can be used for anything.
     */
    public void setFileInfo(byte[] l_fileInfo) throws IOException {
        if (fileVersion <= 2) {
            throw new IOException(RESMAP.getString("message.fileInfoNotSupported.text"));
        }
        boolean useExistingEntry = false;
        if (0 < fileInfoLocation) {
            this.file.seek(this.fileInfoLocation);
            int fileInfoLength = this.file.readInt();
            if (l_fileInfo.length <= fileInfoLength) {
                useExistingEntry = true;
            }
        }
        if (false == useExistingEntry) {
            this.file.seek(4);
            fileInfoLocation = (int) file.length();
            this.file.writeInt(fileInfoLocation);
        }
        this.file.seek(fileInfoLocation);
        this.file.writeInt(l_fileInfo.length);
        this.file.write(l_fileInfo);
    }

    /** This function follows any moved pointers until it gets to an entry that is not a ENTRY_TYPE_MOVED type.
     *
     * The file stream should currently be pointed at the beginning of an entry. When this function is done the stream will be pointed at the position after the entry type id.
     *
     * @return Returns the entry type found.
     */
    protected synchronized byte skipToEntry() throws IOException {
        byte entryType = file.readByte();
        while (entryType == TreeFileLeaf.ENTRY_TYPE_MOVED) {
            this.file.seek(file.readInt());
            entryType = file.readByte();
        }
        return entryType;
    }
}
