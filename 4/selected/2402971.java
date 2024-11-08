package at.filemonkey.model;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListDataListener;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import at.filemonkey.model.renderer.LocalCellRenderer;

public class LocalDirectoryNavigator extends DirectoryNavigator {

    /**
	 * Set Basic var's
	 */
    protected String root;

    protected File[] roots = File.listRoots();

    protected int dirSize = 0;

    protected File tmpFile;

    protected File[] fileArray = new File[0];

    protected File tmpFile2;

    /**
	 * Constructor's
	 * @throws IOException 
	 */
    public LocalDirectoryNavigator() throws IOException {
        getRoot();
        reList();
    }

    public LocalDirectoryNavigator(String startPath) throws IOException {
        root = startPath;
        reList();
    }

    /**
	 * This Method filters file.list
	 * @param file
	 * @return
	 */
    public static boolean isLink(File file) {
        try {
            if (!file.exists()) return true; else {
                String cnnpath = file.getCanonicalPath();
                String abspath = file.getAbsolutePath();
                return !abspath.equals(cnnpath);
            }
        } catch (IOException ex) {
            System.err.println(ex);
            return true;
        }
    }

    /**
	 * Creating a new FIlefilter
	 */
    protected FileFilter filter = new FileFilter() {

        public boolean accept(File file) {
            if (file.getName().contains(".")) return false;
            return file.isDirectory();
        }
    };

    /**
	 * reList()
	 * to update Jlist after copy
	 */
    @Override
    public void reList() throws IOException {
        tmpFile = new File(getCurrentPath());
        dirSize = tmpFile.listFiles().length;
        cd(getCurrentPath());
    }

    /**
	 * reListParent(File _path)
	 * to Update JList delete funktion
	 */
    public void reListParent(File _path) throws IOException {
        tmpFile = new File(_path.getParent());
        dirSize = tmpFile.listFiles().length;
        cd(tmpFile.getAbsolutePath());
    }

    /**
	 * Methode cd(_dir)
	 * To change working directory
	 */
    @Override
    public boolean cd(String _dir) throws IOException {
        boolean check = false;
        tmpFile = new File(_dir);
        if (tmpFile.exists()) {
            fileArray = tmpFile.listFiles();
            check = true;
        } else check = false;
        return check;
    }

    /**
	 * getCurrentPath() returns the current working DIrectory
	 */
    @Override
    public String getCurrentPath() {
        File tmpFile2;
        if (tmpFile == null) {
            tmpFile2 = (File) getRoot();
        } else tmpFile2 = tmpFile;
        return tmpFile2.getAbsolutePath().toString() + File.separator;
    }

    /**
	 * up() provides to go back to Parent directory
	 */
    @Override
    public boolean up() throws IOException {
        tmpFile = new File(getCurrentPath());
        if (tmpFile.exists() && (!tmpFile.getAbsoluteFile().equals(new File(root).getAbsoluteFile()))) {
            System.out.println(tmpFile.getAbsolutePath());
            System.out.println(root);
            cd(tmpFile.getParent().toString());
            return true;
        } else return false;
    }

    public Object getChild(Object parent, int index) {
        File directory = (File) parent;
        if (directory.isDirectory()) {
            File[] children = directory.listFiles(filter);
            return children[index];
        } else {
            return 0;
        }
    }

    public int getChildCount(Object parent) {
        File fileSysEntity = (File) parent;
        if (fileSysEntity.isDirectory()) {
            File[] children = fileSysEntity.listFiles(filter);
            return children.length;
        } else {
            return 0;
        }
    }

    public int getIndexOfChild(Object parent, Object child) {
        File directory = (File) parent;
        File fileSysEntity = (File) child;
        if (fileSysEntity.isDirectory()) {
            String[] children = directory.list();
            int result = -1;
            for (int i = 0; i < children.length; ++i) {
                if (fileSysEntity.getName().equals(children[i])) {
                    result = i;
                    break;
                }
            }
            return result;
        } else return 0;
    }

    @Override
    public Object getElementAt(int index) {
        return fileArray[index];
    }

    @Override
    public int getSize() {
        return fileArray.length;
    }

    /**
	 * to delete a marked file
	 */
    @Override
    public boolean delete(String file) throws IOException {
        tmpFile = new File(getCurrentPath() + file);
        if (tmpFile.exists()) {
            tmpFile.delete();
            reListParent(tmpFile);
            return true;
        } else return false;
    }

    public Object getRoot() {
        File rootFile = null;
        if (isWindows()) {
            rootFile = new File("C:\\");
        } else if (isMac()) {
            rootFile = roots[0];
        } else if (isUnix()) {
            rootFile = roots[0];
        }
        root = rootFile.getAbsolutePath();
        return rootFile;
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("win") >= 0);
    }

    private static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("mac") >= 0);
    }

    private static boolean isUnix() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);
    }

    public boolean isLeaf(Object node) {
        return ((File) node).isFile();
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        String value = (String) newValue;
        if (newValue != null) {
            node.setUserObject(newValue);
        } else {
            node.setUserObject(value);
        }
    }

    @Override
    public String getFilename(Object file) {
        File _file;
        _file = (File) file;
        return _file.getName();
    }

    /**
	 * Copy to current Directory
	 * Reads input File and Filename and creates a new File in current Dir
	 * 
	 * @param _copyFile, _fileName
	 */
    public void copyToCurrentDir(File _copyFile, String _fileName) throws IOException {
        File outputFile = new File(getCurrentPath() + File.separator + _fileName);
        FileReader in;
        FileWriter out;
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }
        in = new FileReader(_copyFile);
        out = new FileWriter(outputFile);
        int c;
        while ((c = in.read()) != -1) out.write(c);
        in.close();
        out.close();
        reList();
    }

    /**
	 * createFolder(String name) creates a new Folder in currentDir with name
	 * @args name:String
	 */
    @Override
    public boolean createFolder(String name) throws IOException {
        tmpFile = new File(getCurrentPath() + name);
        tmpFile.mkdir();
        reList();
        return true;
    }

    /**
	 * rename(String oldName, String newName) renames a File or a Directory
	 * @args oldName:String, newName:String
	 */
    @Override
    public boolean rename(String oldName, String newName) throws IOException {
        String dirPath = getCurrentPath();
        tmpFile = new File(dirPath + oldName);
        File toTmpFile = new File(dirPath + newName);
        boolean success = tmpFile.renameTo(toTmpFile);
        reListParent(tmpFile);
        if (success) {
            System.out.println("Rename: " + success);
            return true;
        } else {
            System.out.println("Rename: Failed");
            return false;
        }
    }

    /**
	 * This Mehtode is used for FTPDirectoryNavigator
	 */
    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public ListCellRenderer getListCellRenderer() {
        return new LocalCellRenderer();
    }

    @Override
    public TreeCellRenderer getTreeCellRenderer() {
        return new LocalCellRenderer();
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
    }

    @Override
    public void addListDataListener(ListDataListener l) {
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
    }

    @Override
    public String getName() {
        return "Local:";
    }
}
