package example.pGrid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import pgrid.Constants;
import pgrid.Properties;

public class TestPGTree {

    /**
	 * @param args
	 */
    TreeNode mTreeRoot = null;

    public static final String LINE_SEPERATOR = System.getProperty("line.separator");

    public TreeNode loadTreeFromFile(BufferedReader reader, String filename) {
        String prefix;
        try {
            prefix = reader.readLine();
        } catch (IOException e) {
            Constants.LOGGER.log(Level.WARNING, "Could not read/write PGrid Tree initialization file '" + filename + "'!", e);
            return null;
        }
        if (prefix.equals("")) return null;
        TreeNode leftChild = loadTreeFromFile(reader, filename);
        TreeNode rightChild = loadTreeFromFile(reader, filename);
        int leaves = (leftChild == null ? 0 : leftChild.getLeavesCount());
        leaves += (rightChild == null ? 0 : rightChild.getLeavesCount());
        int leftDepth = (leftChild == null ? 0 : leftChild.getDepth());
        int rightDepth = (rightChild == null ? 0 : rightChild.getDepth());
        int depth = Math.max(leftDepth, rightDepth);
        int nodes = 1;
        nodes += (leftChild == null ? 0 : leftChild.getNodesCount());
        nodes += (rightChild == null ? 0 : rightChild.getNodesCount());
        return new TreeNode(prefix, leftChild, rightChild, leaves, depth, nodes);
    }

    public BufferedReader read() throws FileNotFoundException {
        FileInputStream in = new FileInputStream("/home/tomasz/P2P/P-Grid/P-Grid/resources/PGridTree.ini");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        System.out.println("das PGridTree.ini wurde gefunden ");
        return reader;
    }

    public void createKeys(String filename, TreeNode mTreeRoot) {
        File file = new File(filename);
        try {
            FileWriter fWriter = new FileWriter(file);
            createKeys(fWriter, mTreeRoot, filename, "");
            fWriter.close();
        } catch (IOException e) {
            Constants.LOGGER.log(Level.WARNING, "Could not write to file '" + filename + "'!", e);
        }
    }

    void createKeys(FileWriter writer, TreeNode node, String filename, String key) {
        try {
            TreeNode left = node.getLeftChild();
            TreeNode right = node.getRightChild();
            if (left != null) createKeys(writer, left, filename, key + "0");
            if (right != null) createKeys(writer, right, filename, key + "1");
            if ((left == null) && (right == null)) writer.write(node.getPrefix() + " " + key + LINE_SEPERATOR);
        } catch (IOException e) {
            Constants.LOGGER.log(Level.WARNING, "Could not write to file '" + filename + "'!", e);
        }
    }

    private String findKey(String query, TreeNode node) {
        if ((node.getLeftChild() == null) && (node.getRightChild() == null)) return "";
        if (node.getPrefix().compareTo(query) > 0) return ("0" + findKey(query, node.getLeftChild())); else return ("1" + findKey(query, node.getRightChild()));
    }
}
