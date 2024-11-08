import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import fi.hip.gb.client.ui.Utils;
import fi.hip.gb.client.ui.Window;
import fi.hip.gb.client.ui.treetable.DynamicTreeTableModel;
import fi.hip.gb.client.ui.treetable.JTreeTable;
import fi.hip.gb.client.ui.treetable.RightClickAdapter;
import fi.hip.gb.client.ui.treetable.TreeTableModel;
import fi.hip.gb.core.WorkResult;
import fi.hip.gb.utils.FileUtils;

/**
 * User interface for showing remote file listings.
 * 
 * @author Juho Karppinen
 */
public class FileObserver extends JFrame {

    /** treetable model */
    private TreeTableModel ttModel;

    private static final long serialVersionUID = 1L;

    public FileObserver() {
        getContentPane().add(showWindow("/"));
    }

    public static void main(String[] args) throws Exception {
        FileObserver frame = new FileObserver();
        frame.pack();
        frame.setVisible(true);
    }

    /**
	 * Shows the tree component.
	 */
    @SuppressWarnings("serial")
    public JComponent showWindow(String initialFolder) {
        final DirectoryNode root = new DirectoryNode("remote", null);
        root.setAllowsChildren(true);
        FileFetcher ff = new FileFetcher();
        String fileList = ff.listDirectory(initialFolder, false);
        addNodes(root, fileList);
        String[] columnNames = { "Name", "Size", "Type", "Remote" };
        Class[] columnTypes = { TreeTableModel.class, Long.class, String.class, String.class };
        String[] getterMethodNames = { "getName", "getSize", "getType", "getLocation" };
        final String[] setterMethodNames = { "setName", null, null, null };
        this.ttModel = new DynamicTreeTableModel(root, columnNames, getterMethodNames, setterMethodNames, columnTypes) {

            public boolean isLeaf(Object node) {
                return ((TreeNode) node).getAllowsChildren() == false;
            }
        };
        JTreeTable treeTable = new JTreeTable(this.ttModel);
        treeTable.setDefaultRenderer(Boolean.class, new DefaultTableCellRenderer() {

            public void setValue(Object value) {
                setText((value == null) ? "" : value.toString());
            }
        });
        treeTable.setDefaultRenderer(Long.class, new DefaultTableCellRenderer() {

            public void setValue(Object value) {
                setText((value == null) ? "" : value.toString());
            }
        });
        final JTree tree = treeTable.getTree();
        tree.setRootVisible(false);
        tree.expandRow(0);
        treeTable.addMouseListener(new RightClickAdapter() {

            public void handleEvent(MouseEvent me) {
                if (me.isPopupTrigger()) {
                    TreePath[] paths = tree.getSelectionPaths();
                    if (paths == null) return;
                    DirectoryNode[] fNodes = new DirectoryNode[paths.length];
                    for (int i = 0; i < fNodes.length; i++) fNodes[i] = (DirectoryNode) paths[i].getLastPathComponent();
                    JPopupMenu popup = (fNodes[0].isRemote()) ? getRemoteMenu(fNodes[0]) : getLocalMenu(fNodes[0]);
                    popup.show((JComponent) me.getSource(), me.getX(), me.getY());
                }
            }
        });
        treeTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        treeTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        treeTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        return new JScrollPane(treeTable);
    }

    /**
	 * Saves the list of the files.
	 * 
	 * @see fi.hip.gb.mobile.Observer#saveResult(WorkResult)
	 */
    public String saveResult(WorkResult result) throws RemoteException {
        return result.firstResult().readContent();
    }

    public void addNodes(DefaultMutableTreeNode root, String directoryContent) {
        System.out.println("Adding nodes " + directoryContent);
        for (Enumeration files = new StringTokenizer(directoryContent, "\n", false); files.hasMoreElements(); ) {
            StringTokenizer cells = new StringTokenizer((String) files.nextElement(), ";", false);
            if (cells.countTokens() != 3) return;
            boolean isDirectory = Boolean.valueOf((String) cells.nextElement()).booleanValue();
            String filename = (String) cells.nextElement();
            String filesize = (String) cells.nextElement();
            DefaultMutableTreeNode subdirNode = root;
            for (Enumeration subdirs = new StringTokenizer(filename, "/", false); subdirs.hasMoreElements(); ) {
                String subdir = (String) subdirs.nextElement();
                if (filename.startsWith("/" + subdir)) {
                    subdir = "/" + subdir;
                }
                if (subdirs.hasMoreElements() == false) {
                    DefaultMutableTreeNode node = isDirectory ? new DirectoryNode(subdir, null) : new FileNode(subdir, new Long(filesize), null);
                    node.setAllowsChildren(isDirectory);
                    subdirNode.add(node);
                } else {
                    int index = subdirNode.getIndex(new DirectoryNode(subdir, null));
                    if (index == -1) {
                        DefaultMutableTreeNode newSubdir = new DirectoryNode(subdir, null);
                        newSubdir.setAllowsChildren(true);
                        subdirNode.add(newSubdir);
                        subdirNode = newSubdir;
                    } else {
                        subdirNode = (DefaultMutableTreeNode) subdirNode.getChildAt(index);
                    }
                }
            }
        }
    }

    /**
	 * Construct popup menu for local file
	 * @param fNode node containing info abou local file
	 * @return popup menu with all listeners set up
	 */
    private JPopupMenu getLocalMenu(final DirectoryNode fNode) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuitem = new JMenuItem("Save file to...");
        menuitem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                String targetFile = Utils.selectFile("Save file as", fNode.getName(), FileObserver.this);
                try {
                    FileUtils.copyFile(fNode.getLocalURL(), new File(targetFile));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        popup.add(menuitem);
        menuitem = new JMenuItem("View as text");
        menuitem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                Window window = new Window((Frame) null, "Content of file " + fNode.getName());
                JTextArea ta = new JTextArea();
                Utils.addPopupMenu(ta);
                ta.setEditable(false);
                try {
                    ta.setText(FileUtils.readFile(fNode.getLocalURL()));
                } catch (IOException ioe) {
                    ta.setText("Error: File " + fNode.getLocalURL() + " cannot be found");
                }
                window.setContent(new JScrollPane(ta));
                window.addCloseButton();
                window.showWindow();
            }
        });
        popup.add(menuitem);
        return popup;
    }

    /**
	 * Construct popup menu for remote file
	 * @param fNode node containing info about remote file
	 * @return popup menu with all listeners set up
	 */
    private JPopupMenu getRemoteMenu(final DirectoryNode fNode) {
        JPopupMenu popup = new JPopupMenu();
        ActionListener listener = new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                TreeNode parent = fNode.getParent();
                String currentPath = fNode.getName();
                while (parent.getParent() != null) {
                    currentPath = ((DirectoryNode) parent).getName() + "/" + currentPath;
                    parent = parent.getParent();
                }
                String operation = ae.getActionCommand();
                if (operation.equalsIgnoreCase(FileFetcher.UPLOAD)) {
                    try {
                        File fileToUpload = Utils.loadFile("Upload file", "", FileObserver.this);
                        if (fileToUpload == null) return;
                        FileFetcher ff = new FileFetcher();
                        ff.uploadFile(fileToUpload, currentPath + "/" + fileToUpload.getName());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    FileFetcher ff = new FileFetcher();
                    try {
                        ff.downloadFile(currentPath);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        JMenuItem menuitem = new JMenuItem("Upload file");
        menuitem.addActionListener(listener);
        menuitem.setActionCommand(FileFetcher.UPLOAD);
        popup.add(menuitem);
        popup.addSeparator();
        if (fNode.isDirectory()) {
            menuitem = new JMenuItem("Get directory listing");
            menuitem.addActionListener(listener);
            menuitem.setActionCommand(FileFetcher.LIST);
            popup.add(menuitem);
            menuitem = new JMenuItem("Download directory");
            menuitem.addActionListener(listener);
            menuitem.setActionCommand(FileFetcher.DOWNLOAD);
            popup.add(menuitem);
        } else {
            menuitem = new JMenuItem("Download file");
            menuitem.addActionListener(listener);
            menuitem.setActionCommand(FileFetcher.DOWNLOAD);
            popup.add(menuitem);
        }
        return popup;
    }

    public class FileNode extends DirectoryNode {

        /** size of the file */
        private Long filesize;

        private static final long serialVersionUID = 1L;

        /**
		 * Create new node for the tree
		 * @param filename name of the file, shown on the first column
		 * @param filesize size of the file
		 * @param location URL for the local file, or null if remote file
		 */
        public FileNode(String filename, Long filesize, URL location) {
            super(filename, location);
            this.filesize = filesize;
        }

        public Long getSize() {
            return filesize;
        }

        public String getType() {
            return "file";
        }

        public boolean isDirectory() {
            return false;
        }
    }

    public class DirectoryNode extends DefaultMutableTreeNode {

        /** name of the file, shown on the first column */
        protected String filename;

        /** URL for the local file, or null if remote file */
        private URL location;

        private static final long serialVersionUID = 1L;

        /**
		 * Create new directory node into the tree
		 * @param filename name of the directory, shown on the first column
		 * @param location URL for the local file, or null if remote file
		 */
        public DirectoryNode(String filename, URL location) {
            super(filename);
            this.filename = filename;
            this.location = location;
        }

        /**
		 * Nodes are equals if their filenames are equal
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
        public boolean equals(Object obj) {
            return this.filename.equals(((DirectoryNode) obj).getName());
        }

        /**
		 * Override to disable check for existence of child object.
		 * Enables usage of FileNode#equals(Object).
		 * 
		 * @see javax.swing.tree.DefaultMutableTreeNode#getIndex(TreeNode)
		 */
        public int getIndex(TreeNode aChild) {
            if (aChild == null) {
                throw new IllegalArgumentException("argument is null");
            }
            if (children == null) return -1;
            return children.indexOf(aChild);
        }

        public String getName() {
            return this.filename;
        }

        public void setName(String filename) {
            this.filename = filename;
        }

        public Long getSize() {
            return null;
        }

        public String getType() {
            return "directory";
        }

        public boolean isDirectory() {
            return true;
        }

        public String getLocation() {
            return this.location == null ? "remote" : "local";
        }

        public boolean isRemote() {
            return this.location == null;
        }

        public URL getLocalURL() {
            return this.location;
        }
    }
}
