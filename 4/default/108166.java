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
import java.util.Properties;
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
import fi.hip.gb.client.Main;
import fi.hip.gb.client.ui.Utils;
import fi.hip.gb.client.ui.Window;
import fi.hip.gb.client.ui.treetable.DynamicTreeTableModel;
import fi.hip.gb.client.ui.treetable.JTreeTable;
import fi.hip.gb.client.ui.treetable.RightClickAdapter;
import fi.hip.gb.client.ui.treetable.TreeTableModel;
import fi.hip.gb.core.JobResult;
import fi.hip.gb.core.WorkDescription;
import fi.hip.gb.core.WorkResult;
import fi.hip.gb.core.plugin.DispatchPlugin;
import fi.hip.gb.mobile.AgentApi;
import fi.hip.gb.mobile.Observer;
import fi.hip.gb.serializer.XMLSerializer;
import fi.hip.gb.utils.FileUtils;

/**
 * Builds user interface for showing mandelbrot images for the user.
 * 
 * @author Juho Karppinen
 * @version $Id: FileObserver.java 165 2005-01-31 16:03:10Z jkarppin $
 */
public class FileObserver implements Observer {

    /** agentapi */
    private AgentApi api;

    /** treetable model */
    private TreeTableModel ttModel;

    public FileObserver() {
    }

    public void init(AgentApi api) {
        this.api = api;
    }

    public static void main(String[] args) throws Exception {
    }

    public DispatchPlugin showControls() throws RemoteException {
        return null;
    }

    /**
	 * Shows the received files inside tree component.
	 * 
	 * @see fi.hip.gb.mobile.Observer#showResult(WorkResult)
	 */
    public JComponent showResult(WorkResult results) throws RemoteException {
        final DirectoryNode root = new DirectoryNode(this.api.getDescription().getServiceURL().getHost(), null);
        root.setAllowsChildren(true);
        for (Enumeration e = results.results(); e.hasMoreElements(); ) {
            JobResult result = (JobResult) e.nextElement();
            Properties flags = result.getFlags();
            boolean listing = !flags.getProperty("TYPE", "LIST").equals("DOWNLOAD");
            if (listing) {
                addNodes(root, result.readContent());
            } else {
                File file = new File(result.getFileURL().getFile());
                DefaultMutableTreeNode node = new FileNode(file.getName(), new Long(file.length()), result.getFileURL());
                node.setAllowsChildren(file.isDirectory());
                root.add(node);
            }
        }
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
                String targetFile = Utils.selectFile("Save file as", fNode.getName(), (JFrame) Main.getUI());
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
                String operation = ae.getActionCommand();
                URL[] attachments = new URL[0];
                if (operation.equalsIgnoreCase(FileFetcher.UPLOAD)) {
                    try {
                        URL file = Utils.loadFile("Upload file", "", (Frame) Main.getUI());
                        if (file == null) return;
                        attachments = new URL[] { file };
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        return;
                    }
                }
                TreeNode parent = fNode.getParent();
                String filePath = fNode.getName();
                while (parent.getParent() != null) {
                    filePath = ((DirectoryNode) parent).getName() + "/" + filePath;
                    parent = parent.getParent();
                }
                try {
                    dispatch(new String[] { filePath }, attachments, operation);
                } catch (RemoteException re) {
                    Main.errorMessage("Job dispathing failed: " + re.getMessage());
                    re.printStackTrace();
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

    /**
	 * Dispatch new job to retrieve file(s) or list another directory
	 * 
	 * @param filenames name of selected files or directories
	 * @param attachments files to be uploaded
	 * @param operation either <code>FileFetcher#DOWNLOAD</code>, 
	 * <code>FileFetcher#LIST</code> or <code>FileFetcher#UPLOAD</code>
	 * @throws RemoteException
	 */
    private void dispatch(String[] filenames, URL[] attachments, String operation) throws RemoteException {
        WorkDescription wds = new WorkDescription(this.api.getDescription());
        Properties flags = new Properties();
        flags.put("TYPE", operation);
        wds.getExecutable().setFlags(flags);
        URL[] jars = wds.getExecutable().getJarFiles();
        wds.getExecutable().attachFiles(jars);
        for (int i = 0; i < attachments.length; i++) {
            wds.getExecutable().attachFile(attachments[i]);
        }
        wds.getExecutable().getParameters().clear();
        for (int i = 0; i < filenames.length; i++) wds.getExecutable().getParameters().add(filenames[i]);
        System.out.println("WDS: " + XMLSerializer.getSerialized(wds));
        this.api.appendJob(wds);
    }

    public class FileNode extends DirectoryNode {

        /** size of the file */
        private Long filesize;

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
