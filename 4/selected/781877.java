package supersync.tree;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author bdrake
 */
public class TreeWritter {

    /** This class keeps track of the entries in the tree.
     */
    protected class TreeWritterElt {

        public int eltLength = 0;

        public int length = 0;

        public int locationInTempFile = 0;

        public TreeWritterElt[] treeWritterLeafs = null;

        public void calculateLength() {
            length = 0;
            for (TreeWritterElt treeWritterLeaf : treeWritterLeafs) {
                treeWritterLeaf.calculateLength();
                length += treeWritterLeaf.length;
            }
        }
    }

    /** Writes the temporary tree to a file.  The temporary tree is used to generate the final tree.
     */
    protected TreeWritterElt writeTempTree(SerializableLeaf l_root, DataOutputStream l_dataOut, FileOutputStream l_outStream) throws IOException {
        TreeWritterElt foldElt = new TreeWritterElt();
        foldElt.locationInTempFile = (int) l_outStream.getChannel().position();
        SerializableLeaf[] children = null;
        if (l_root.isBranch()) {
            children = l_root.getChildren();
            if (null == children) {
                children = new SerializableLeaf[0];
            }
        }
        byte[] leafValue = l_root.getLeafValue();
        foldElt.eltLength = 4 + leafValue.length;
        l_dataOut.writeInt(foldElt.eltLength);
        l_dataOut.writeInt(l_root.isBranch() ? children.length : -1);
        l_dataOut.write(leafValue);
        l_dataOut.writeByte('\n');
        if (l_root.isBranch()) {
            foldElt.treeWritterLeafs = new TreeWritterElt[children.length];
            for (int leafIndex = 0; leafIndex < children.length; leafIndex++) {
                foldElt.treeWritterLeafs[leafIndex] = writeTempTree(children[leafIndex], l_dataOut, l_outStream);
                foldElt.length += +foldElt.treeWritterLeafs[leafIndex].eltLength + 4 + 4;
                if (-1 != foldElt.treeWritterLeafs[leafIndex].length) {
                    foldElt.length += foldElt.treeWritterLeafs[leafIndex].length;
                }
            }
        } else {
            foldElt.length = -1;
        }
        return foldElt;
    }

    /** Writes a tree file to that can later be opened and edited with a TreeFile class.
     *
     * This function will only call the getChildren() function on the leafs once.
     */
    public void writeTree(SerializableLeaf l_leaf, File l_file) throws IOException {
        File tempFile = File.createTempFile("TreeWritter", ".txt");
        FileOutputStream outStream = new FileOutputStream(tempFile);
        DataOutputStream dataOutStream = new DataOutputStream(outStream);
        TreeWritterElt baseWritter = this.writeTempTree(l_leaf, dataOutStream, outStream);
        outStream.close();
        outStream = new FileOutputStream(l_file);
        dataOutStream = new DataOutputStream(outStream);
        FileInputStream inStream = new FileInputStream(tempFile);
        DataInputStream dataInStream = new DataInputStream(inStream);
        dataOutStream.writeInt(TreeFile.CURRENT_FILE_VERSION);
        dataOutStream.writeInt(0);
        int numberOfChars = dataInStream.readInt();
        dataOutStream.writeInt(numberOfChars - 4);
        byte[] buffer = new byte[numberOfChars];
        dataInStream.read(buffer);
        dataOutStream.write(buffer);
        this.writeFinalTree(baseWritter, inStream, dataInStream, dataOutStream);
        outStream.close();
        inStream.close();
        tempFile.delete();
    }

    /** Writes the final tree to the file.
     */
    protected void writeFinalTree(TreeWritterElt l_tempTree, FileInputStream l_tempInStream, DataInputStream l_tempDataInStream, DataOutputStream l_outStream) throws IOException {
        int[] startPositionOffsets = new int[l_tempTree.treeWritterLeafs.length];
        if (0 < startPositionOffsets.length) {
            startPositionOffsets[0] = 0;
            for (int leafIndex = 1; leafIndex < startPositionOffsets.length; leafIndex++) {
                int prevLength = l_tempTree.treeWritterLeafs[leafIndex - 1].length;
                if (-1 == prevLength) {
                    prevLength = 0;
                }
                startPositionOffsets[leafIndex] = startPositionOffsets[leafIndex - 1] + prevLength;
            }
        }
        for (int leafIndex = 0; leafIndex < startPositionOffsets.length; leafIndex++) {
            l_tempInStream.getChannel().position(l_tempTree.treeWritterLeafs[leafIndex].locationInTempFile);
            if (-1 != l_tempTree.treeWritterLeafs[leafIndex].length) {
                l_outStream.writeInt(startPositionOffsets[leafIndex]);
            } else {
                l_outStream.writeInt(-1);
            }
            int numberOfChars = l_tempDataInStream.readInt();
            l_outStream.writeInt(numberOfChars - 4);
            byte[] buffer = new byte[numberOfChars];
            l_tempInStream.read(buffer);
            l_outStream.write(buffer);
        }
        for (int leafIndex = 0; leafIndex < startPositionOffsets.length; leafIndex++) {
            if (-1 != l_tempTree.treeWritterLeafs[leafIndex].length) {
                writeFinalTree(l_tempTree.treeWritterLeafs[leafIndex], l_tempInStream, l_tempDataInStream, l_outStream);
            }
        }
    }
}
