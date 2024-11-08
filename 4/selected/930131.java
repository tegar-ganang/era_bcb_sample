package supersync.tree2;

import java.io.IOException;
import java.text.ParseException;

/** This class is a leaf in a tree file.  The leaf contains the element that we saved.  It also contains information about child leafs and where this leaf is stored in the tree file.
 *
 * @author Brandon Drake
 */
public class TreeFileLeaf<TLeaf extends EditableLeaf> {

    protected static final int FILE_MARKER = -1;

    protected static final int NO_FILES_MARKER = -2;

    protected static final byte ENTRY_TYPE_FILE = 1;

    protected static final byte ENTRY_TYPE_FOLDER = 2;

    protected static final byte ENTRY_TYPE_FILE_LIST = 3;

    protected static final byte ENTRY_TYPE_MOVED = 4;

    protected byte entryType = 0;

    protected int positionInFile = 0;

    protected int positionOfFileList = FILE_MARKER;

    protected final TreeFile treeFile;

    protected TLeaf leaf = null;

    /** Returns the children TreeFileLeafs for the current directory.  Returns null if this is not a directory.
     */
    public synchronized TreeFileLeaf[] getChildren() throws IOException {
        if (FILE_MARKER == positionOfFileList) {
            return null;
        } else if (NO_FILES_MARKER == positionOfFileList) {
            return new TreeFileLeaf[0];
        }
        synchronized (treeFile) {
            try {
                return treeFile.getFileList(positionOfFileList);
            } catch (ParseException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
        }
    }

    /** Gets the tree file that this leaf is stored in.
     */
    public synchronized TreeFile getTreeFile() {
        return this.treeFile;
    }

    /** Gets the leaf for this entry in the tree file.
     */
    public synchronized TLeaf getLeaf() {
        return leaf;
    }

    /** Gets the number of children in this leaf.
     */
    public int getChildCount() throws IOException {
        synchronized (treeFile) {
            treeFile.file.seek(this.positionOfFileList);
            treeFile.skipToEntry();
            return treeFile.file.readInt();
        }
    }

    /** Returns true if the current entry is a directory and it has at least one child.
     */
    public synchronized boolean hasChildren() {
        return this.positionOfFileList != FILE_MARKER && this.positionOfFileList != NO_FILES_MARKER;
    }

    /** Returns true if the current entry can have children.
     */
    public synchronized boolean isBranch() {
        return entryType == ENTRY_TYPE_FOLDER;
    }

    /** Saves the leaf back to the file.
     */
    public synchronized void saveLeafValue() throws IOException {
        synchronized (treeFile) {
            treeFile.file.seek(this.positionInFile + 1);
            if (this.isBranch()) {
                treeFile.file.skipBytes(4);
            }
            int lengthOfExistingEntry = treeFile.file.readInt();
            byte[] leafValue = leaf.getLeafValue();
            if (leafValue.length <= lengthOfExistingEntry) {
                treeFile.file.seek(this.treeFile.file.getChannel().position() - 4);
                treeFile.file.writeInt(leafValue.length);
                treeFile.file.write(leafValue);
            } else {
                treeFile.file.seek(this.positionInFile);
                treeFile.file.writeInt(ENTRY_TYPE_MOVED);
                this.positionInFile = (int) treeFile.file.length();
                treeFile.file.writeInt(this.positionInFile);
                treeFile.file.seek(this.positionInFile);
                treeFile.file.writeByte(this.entryType);
                if (this.isBranch()) {
                    treeFile.file.writeInt(this.positionOfFileList);
                }
                treeFile.file.writeInt(leafValue.length);
                treeFile.file.write(leafValue);
            }
        }
    }

    /** This function sets the children of this leaf.  If the leaf already has children set those will be overwritten.
     *
     * Any branches in the list will be created with no children.  After the function returns you can set the children of the individual files.
     */
    public TreeFileLeaf[] setChildren(EditableLeaf[] l_children) throws IOException {
        synchronized (treeFile) {
            treeFile.file.seek(treeFile.file.length());
            TreeFileLeaf[] result = new TreeFileLeaf[l_children.length];
            if (l_children.length != 0) {
                for (int fileIndex = 0; fileIndex < l_children.length; fileIndex++) {
                    result[fileIndex] = new TreeFileLeaf(this.treeFile);
                    result[fileIndex].positionInFile = (int) treeFile.file.getChannel().position();
                    result[fileIndex].leaf = l_children[fileIndex];
                    byte[] leafValue = l_children[fileIndex].getLeafValue();
                    if (l_children[fileIndex].isBranch()) {
                        result[fileIndex].entryType = TreeFileLeaf.ENTRY_TYPE_FOLDER;
                        result[fileIndex].positionOfFileList = NO_FILES_MARKER;
                        treeFile.file.writeByte(result[fileIndex].entryType);
                        treeFile.file.writeInt(NO_FILES_MARKER);
                    } else {
                        result[fileIndex].entryType = TreeFileLeaf.ENTRY_TYPE_FILE;
                        result[fileIndex].positionOfFileList = FILE_MARKER;
                        treeFile.file.writeByte(result[fileIndex].entryType);
                    }
                    treeFile.file.writeInt(leafValue.length);
                    treeFile.file.write(leafValue);
                }
                this.positionOfFileList = (int) treeFile.file.getChannel().position();
                treeFile.file.writeByte(TreeFileLeaf.ENTRY_TYPE_FILE_LIST);
                treeFile.file.writeInt(l_children.length);
                for (int fileIndex = 0; fileIndex < l_children.length; fileIndex++) {
                    treeFile.file.writeInt(result[fileIndex].positionInFile);
                }
            } else {
                this.positionOfFileList = NO_FILES_MARKER;
            }
            treeFile.file.seek(this.positionInFile + 1);
            treeFile.file.writeInt(this.positionOfFileList);
            return result;
        }
    }

    /** Constructor.
     */
    public TreeFileLeaf(TreeFile l_treeFile) {
        this.treeFile = l_treeFile;
    }
}
