import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.beans.*;

public class GhinExplorer extends JFrame implements ActionListener {

    protected JTree myTree;

    protected DefaultTreeModel myTreeModel;

    protected JTextField myDisplay;

    protected MyFileChooser myFileExplorer;

    private MyToolbar toolbar;

    protected JScrollPane scrolPane;

    protected JSplitPane splitMe, splitMe2;

    protected SimpleFilter gfileFilter;

    protected JPanel myExplorerPane;

    private File gcurrentDir = new File(System.getProperty("user.dir"));

    public JPopupMenu popup;

    public JMenuItem openwithPop, opennewPop, copyPop, pastePop, renamePop, newdirPop, newfilePop, deletePop, openmspaintPop, openeditorPop, openbrowserPop, opendocumentPop, openexcelPop, openjavaPop, propertiesPop;

    protected DirView dview;

    public static final ImageIcon ICON_COMPUTER = new ImageIcon(ClassLoader.getSystemResource("gohome.png"));

    public static final ImageIcon ICON_DISK = new ImageIcon(ClassLoader.getSystemResource("manager.png"));

    public static final ImageIcon ICON_FOLDER = new ImageIcon(ClassLoader.getSystemResource("folder.png"));

    public static final ImageIcon ICON_EXPANDEDFOLDER = new ImageIcon(ClassLoader.getSystemResource("manager.png"));

    protected static String TEXT_PROGRAM = "notepad.exe";

    protected static String WEB_PROGRAM = "firefox.exe";

    protected static String IMAGE_PROGRAM = "mspaint.exe";

    protected static String SHEET_PROGRAM = "excell.exe";

    protected static String DOKU_PROGRAM = "winword.exe";

    protected static String JAVA_PROGRAM = "java.exe";

    protected static String MEDIA_PROGRAM = "audioplayer.exe";

    protected static String ZIP_PROGRAM = "ghinzipo.png";

    protected static String JAR_PROGRAM = "java.exe -jar";

    protected String[] myfilter = { "zip", "jar", "java", "txt", "exe", "doc", "xls", "html", "png" };

    protected String[] myfiltername = { "ZIP Files", "Java Archive", "Java", "Text File", "Executable", "Document", "Spreetsheet", "HTML File", "Image File" };

    private FindFileName findfilename;

    private boolean showm = false;

    private File mycopyFileName;

    private String mycopyFile;

    private String mycopyPath;

    public GhinExplorer() {
        super("Ghin Explorer Ver 1.0");
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(new IconData(ICON_COMPUTER, null, "My Computer"));
        DefaultMutableTreeNode node;
        File[] roots = File.listRoots();
        for (int k = 0; k < roots.length; k++) {
            node = new DefaultMutableTreeNode(new IconData(ICON_DISK, null, new FileNode(roots[k])));
            top.add(node);
            node.add(new DefaultMutableTreeNode(new Boolean(true)));
        }
        myTreeModel = new DefaultTreeModel(top);
        myTree = new JTree(myTreeModel);
        myTree.putClientProperty("JTree.lineStyle", "Angled");
        myTree.addTreeSelectionListener(new TreeSelectionListener() {

            public void valueChanged(TreeSelectionEvent te) {
                try {
                    TreePath tp = te.getNewLeadSelectionPath();
                    int selRow = myTree.getLeadSelectionRow();
                    String treeSelect = tp.getLastPathComponent().toString();
                    int indexHome = selRow - 1;
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Error select file directory " + ex, "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        TreeCellRenderer renderer = new IconCellRenderer();
        myTree.setCellRenderer(renderer);
        myTree.addTreeExpansionListener(new DirExpansionListener());
        myTree.addTreeSelectionListener(new DirSelectionListener());
        myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        myTree.setShowsRootHandles(true);
        myTree.setEditable(false);
        Toolkit kit = Toolkit.getDefaultToolkit();
        Image image = kit.getImage(ClassLoader.getSystemResource("logo.gif"));
        setIconImage(image);
        toolbar = new MyToolbar();
        for (int i = 0; i < toolbar.imageName.length; i++) {
            toolbar.button[i].addActionListener(this);
        }
        getContentPane().add(toolbar, BorderLayout.NORTH);
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);
        myFileExplorer = new MyFileChooser();
        for (int i = 0; i < myfiltername.length; i++) {
            gfileFilter = new SimpleFilter(myfilter[i], myfiltername[i]);
            myFileExplorer.addChoosableFileFilter(gfileFilter);
        }
        dview = new DirView();
        myFileExplorer.getAcceptAllFileFilter();
        myFileExplorer.setFileView(dview);
        myFileExplorer.setCurrentDirectory(gcurrentDir);
        myFileExplorer.setDialogTitle("File Explorer");
        myFileExplorer.setMultiSelectionEnabled(true);
        myFileExplorer.setDragEnabled(true);
        myFileExplorer.setControlButtonsAreShown(true);
        myFileExplorer.setAcceptAllFileFilterUsed(true);
        myFileExplorer.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        myFileExplorer.setPreferredSize(new Dimension(500, 100));
        PropertyChangeListener mylisterner = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName() == JFileChooser.FILE_FILTER_CHANGED_PROPERTY) {
                    return;
                } else if (e.getPropertyName() == JFileChooser.SELECTED_FILE_CHANGED_PROPERTY) {
                    try {
                        File f = myFileExplorer.getSelectedFile();
                        myDisplay.setText(myFileExplorer.getSelectedFile().getPath());
                    } catch (Exception ex) {
                    }
                    ;
                }
            }
        };
        myFileExplorer.addPropertyChangeListener(mylisterner);
        popup = new JPopupMenu();
        popup.add(openwithPop = new JMenu("Open With"));
        openwithPop.add(openmspaintPop = new JMenuItem("Drawing Editor"));
        openwithPop.add(openeditorPop = new JMenuItem("Text Editor"));
        openwithPop.add(openbrowserPop = new JMenuItem("Web Browser"));
        openwithPop.add(opendocumentPop = new JMenuItem("Word Editor"));
        openwithPop.add(openexcelPop = new JMenuItem("SpreedSheet Editor"));
        openwithPop.add(openjavaPop = new JMenuItem("Java Runtime"));
        openmspaintPop.addActionListener(new menuListener());
        openeditorPop.addActionListener(new menuListener());
        openbrowserPop.addActionListener(new menuListener());
        openmspaintPop.addActionListener(new menuListener());
        opendocumentPop.addActionListener(new menuListener());
        openjavaPop.addActionListener(new menuListener());
        openwithPop.addActionListener(new menuListener());
        popup.add(opennewPop = new JMenu("New"));
        opennewPop.add(newdirPop = new JMenuItem("New Directory"));
        opennewPop.add(newfilePop = new JMenuItem("New File"));
        newdirPop.addActionListener(new menuListener());
        newfilePop.addActionListener(new menuListener());
        popup.addSeparator();
        popup.add(copyPop = new JMenuItem("Copy"));
        copyPop.addActionListener(new menuListener());
        popup.add(pastePop = new JMenuItem("Paste"));
        pastePop.setVisible(false);
        pastePop.addActionListener(new menuListener());
        popup.add(renamePop = new JMenuItem("Rename"));
        renamePop.addActionListener(new menuListener());
        popup.add(deletePop = new JMenuItem("Delete"));
        deletePop.addActionListener(new menuListener());
        popup.addSeparator();
        popup.add(propertiesPop = new JMenuItem("Properties"));
        propertiesPop.addActionListener(new menuListener());
        myFileExplorer.setComponentPopupMenu(popup);
        ActionListener actionlist = new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                String stringChoose = ev.getActionCommand();
                if (stringChoose == "ApproveSelection") {
                    File file = myFileExplorer.getSelectedFile();
                    String dir = file.getPath();
                    String myFile = file.toString();
                    if (myFile.endsWith(".txt")) runFile(TEXT_PROGRAM, file);
                    if (myFile.endsWith(".xls") || myFile.endsWith(".odl")) runFile(SHEET_PROGRAM, file);
                    if (myFile.endsWith(".jpg") || myFile.endsWith(".png")) runFile(IMAGE_PROGRAM, file);
                    if (myFile.endsWith(".doc") || myFile.endsWith(".odt")) runFile(DOKU_PROGRAM, file);
                    if (myFile.endsWith(".html") || myFile.endsWith(".php")) runFile(WEB_PROGRAM, file);
                    if (myFile.endsWith(".java")) runFile(JAVA_PROGRAM, file);
                    if (myFile.endsWith(".jar")) runFile(JAR_PROGRAM, file);
                    if (myFile.endsWith(".zip") || myFile.endsWith(".gzip")) runFile(JAR_PROGRAM, file);
                }
                if (stringChoose == "CancelSelection") {
                }
            }
        };
        myFileExplorer.addActionListener(actionlist);
        myFileExplorer.setBackground(SystemColor.controlHighlight);
        scrolPane = new JScrollPane();
        scrolPane.setMinimumSize(new Dimension(150, 100));
        scrolPane.getViewport().add(myTree);
        myExplorerPane = new JPanel();
        myExplorerPane.setLayout(new BorderLayout());
        myExplorerPane.setDoubleBuffered(true);
        findfilename = new FindFileName(myFileExplorer);
        findfilename.setVisible(false);
        splitMe2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, findfilename, myFileExplorer);
        splitMe2.setMinimumSize(new Dimension(150, 100));
        myExplorerPane.add("Center", splitMe2);
        splitMe = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrolPane, myExplorerPane);
        splitMe.setMinimumSize(new Dimension(50, 100));
        getContentPane().add("Center", splitMe);
        myDisplay = new JTextField();
        myDisplay.setEditable(false);
        myDisplay.setFont(new java.awt.Font("Dialog", Font.BOLD, 11));
        getContentPane().add("South", myDisplay);
        WindowListener wndCloser = new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        };
        addWindowListener(wndCloser);
        Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setSize(new Dimension(screenSize.width, screenSize.height));
        setVisible(true);
    }

    protected JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();
        JMenu mFile = new JMenu("File");
        mFile.setMnemonic('f');
        JMenu mAbout = new JMenu("Help");
        mAbout.setMnemonic('h');
        JMenuItem item = new JMenuItem("About GhinExplorer 1.0");
        item.setMnemonic('A');
        ActionListener lst = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFrame frame = new JFrame("About GhinExplorer 1.0");
                ImageIcon icon = new ImageIcon(ClassLoader.getSystemResource("logo.gif"));
                JLabel label1 = new JLabel(icon);
                frame.add("North", label1);
                JLabel label2 = new JLabel("<html><li>Ghin Explorer 1.0� " + "</li><li><p>Ver# 1.0 </li>" + "<li><p>Develop by: Goen-Ghin</li><li><p>JavaGeo Technology System</li><li>" + "<p>Copyright<font size=\"2\">�</font> June 2008 @Pekanbaru Riau Indonesia</li></html>");
                label2.setFont(new Font("Tahoma", Font.PLAIN, 11));
                frame.add(label2);
                Toolkit kit = Toolkit.getDefaultToolkit();
                Image image = kit.getImage(ClassLoader.getSystemResource("logo.gif"));
                frame.setIconImage(image);
                java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                frame.setSize(new java.awt.Dimension(240, 150));
                frame.setLocation((screenSize.width - 240) / 2, (screenSize.height - 240) / 2);
                frame.setVisible(true);
            }
        };
        item.addActionListener(lst);
        mAbout.add(item);
        item = new JMenuItem("Exit");
        item.setMnemonic('x');
        lst = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.out.println("Terima kasih (thanks)");
                System.exit(0);
            }
        };
        item.addActionListener(lst);
        mFile.add(item);
        menuBar.add(mFile);
        menuBar.add(mAbout);
        return menuBar;
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == toolbar.button[0]) {
            int selRow = myTree.getLeadSelectionRow() - 1;
            if (selRow > 0) myTree.setSelectionRow(selRow); else selRow = 0;
        }
        if (ae.getSource() == toolbar.button[1]) {
            int selRow = myTree.getLeadSelectionRow() + 1;
            if (selRow > 0 || selRow <= myTree.getRowCount()) myTree.setSelectionRow(selRow); else selRow = 0;
        }
        if (ae.getSource() == toolbar.button[2]) {
            if (showm == true) {
                findfilename.setVisible(false);
                myExplorerPane.validate();
                splitMe2.validate();
                showm = false;
            } else if (showm == false) {
                findfilename.setVisible(true);
                myExplorerPane.validate();
                splitMe2.validate();
                splitMe2.resetToPreferredSizes();
                showm = true;
            }
        }
        if (ae.getSource() == toolbar.button[3]) {
            System.out.println("Terima kasih (thanks)");
            System.exit(0);
        }
    }

    public static void copy(String fromFileName, String toFileName) throws IOException {
        File fromFile = new File(fromFileName);
        File toFile = new File(toFileName);
        if (!fromFile.exists()) throw new IOException("Copy: no such source file: " + fromFileName);
        if (!fromFile.canRead()) throw new IOException("Copy: source file is unreadable: " + fromFileName);
        if (toFile.isDirectory()) toFile = new File(toFile, fromFile.getName());
        if (toFile.exists()) {
            if (!toFile.canWrite()) throw new IOException("Copy: destination file is unwriteable: " + toFileName);
            if (JOptionPane.showConfirmDialog(null, "Overwrite File ?", "Overwrite File", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) return;
        } else {
            String parent = toFile.getParent();
            if (parent == null) parent = System.getProperty("user.dir");
            File dir = new File(parent);
            if (!dir.exists()) throw new IOException("Copy: destination directory doesn't exist: " + parent);
            if (dir.isFile()) throw new IOException("Copy: destination is not a directory: " + parent);
            if (!dir.canWrite()) throw new IOException("Copy: destination directory is unwriteable: " + parent);
        }
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
                ;
            }
            if (to != null) try {
                to.close();
            } catch (IOException e) {
                ;
            }
        }
    }

    public static void copyDirectory(File sourceDir, File destDir) throws IOException {
        File[] children = sourceDir.listFiles();
        for (File sourceChild : children) {
            String name = sourceChild.getName();
            File destChild = new File(destDir, name);
            if (sourceChild.isDirectory()) {
                copyDirectory(sourceChild, destChild);
                System.out.println("df" + sourceDir);
                System.out.println("dt" + destDir);
            } else copy(sourceDir.toString(), destDir.toString());
        }
    }

    public void runFile(String runcomfile, File file) {
        try {
            String s = null;
            Runtime r = Runtime.getRuntime();
            Properties p = System.getProperties();
            Process ps = null;
            ps = r.exec(runcomfile + " " + file);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            BufferedReader stdOutput1 = new BufferedReader(new InputStreamReader(ps.getErrorStream()));
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
                if ((s = stdInput.readLine()) == null) {
                    System.out.println("");
                }
            }
            while ((s = stdOutput1.readLine()) != null) {
                System.out.println(s);
            }
            stdInput.close();
            stdOutput1.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error open file " + e, "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    DefaultMutableTreeNode getTreeNode(TreePath path) {
        return (DefaultMutableTreeNode) (path.getLastPathComponent());
    }

    FileNode getFileNode(DefaultMutableTreeNode node) {
        if (node == null) return null;
        Object obj = node.getUserObject();
        if (obj instanceof IconData) obj = ((IconData) obj).getObject();
        if (obj instanceof FileNode) return (FileNode) obj; else return null;
    }

    class menuListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand() == "Drawing Editor") {
                Thread runner1 = new Thread() {

                    public void run() {
                        try {
                            runFile(IMAGE_PROGRAM, myFileExplorer.getSelectedFile());
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(null, "Error run program file " + e, "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                        ;
                    }
                };
                runner1.start();
            }
            if (e.getActionCommand() == "Text Editor") {
                Thread runner2 = new Thread() {

                    public void run() {
                        try {
                            runFile(TEXT_PROGRAM, myFileExplorer.getSelectedFile());
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(null, "Error run program file " + e, "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                        ;
                    }
                };
                runner2.start();
            }
            if (e.getActionCommand() == "Web Browser") {
                Thread runner3 = new Thread() {

                    public void run() {
                        try {
                            runFile(WEB_PROGRAM, myFileExplorer.getSelectedFile());
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(null, "Error run program file " + e, "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                        ;
                    }
                };
                runner3.start();
            }
            if (e.getActionCommand() == "Word Editor") {
                Thread runner4 = new Thread() {

                    public void run() {
                        try {
                            runFile(DOKU_PROGRAM, myFileExplorer.getSelectedFile());
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(null, "Error run program file " + e, "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                        ;
                    }
                };
                runner4.start();
            }
            if (e.getActionCommand() == "SpreedSheet Editor") {
                Thread runner5 = new Thread() {

                    public void run() {
                        try {
                            runFile(SHEET_PROGRAM, myFileExplorer.getSelectedFile());
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(null, "Error run program file " + e, "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                        ;
                    }
                };
                runner5.start();
            }
            if (e.getActionCommand() == "Java Runtime") {
                Thread runner6 = new Thread() {

                    public void run() {
                        try {
                            runFile(JAVA_PROGRAM, myFileExplorer.getSelectedFile());
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(null, "Error run program file " + e, "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                        ;
                    }
                };
                runner6.start();
            }
            if (e.getActionCommand() == "Copy") {
                Thread runner7 = new Thread() {

                    public void run() {
                        try {
                            pastePop.setVisible(true);
                            mycopyFileName = myFileExplorer.getSelectedFile();
                            mycopyFile = myFileExplorer.getSelectedFile().getName();
                            mycopyPath = myFileExplorer.getCurrentDirectory().toString();
                            System.out.println("copy" + mycopyFile.toString());
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(null, "Error run program file " + e, "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                        ;
                    }
                };
                runner7.start();
            }
            if (e.getActionCommand() == "Delete") {
                Thread runner8 = new Thread() {

                    public void run() {
                        if (JOptionPane.showConfirmDialog(null, "Delete This File ?", "Delete File", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) myFileExplorer.getSelectedFile().delete();
                        myFileExplorer.rescanCurrentDirectory();
                    }
                };
                runner8.start();
            }
            if (e.getActionCommand() == "Paste") {
                Thread runner9 = new Thread() {

                    public void run() {
                        try {
                            pastePop.setVisible(false);
                            if (mycopyFileName.isDirectory()) copyDirectory(mycopyFileName, myFileExplorer.getCurrentDirectory()); else copy(mycopyPath + File.separatorChar + mycopyFile, myFileExplorer.getCurrentDirectory().toString() + File.separatorChar + mycopyFile);
                            myFileExplorer.rescanCurrentDirectory();
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(null, "Error to copy file/directory " + e, "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                };
                runner9.start();
            }
            if (e.getActionCommand() == "Rename") {
                Thread runner10 = new Thread() {

                    public void run() {
                        String inputValue = JOptionPane.showInputDialog("Rename File to ", myFileExplorer.getSelectedFile().getName());
                        myFileExplorer.getSelectedFile().renameTo(new File(myFileExplorer.getCurrentDirectory().toString() + File.separatorChar + inputValue));
                        myFileExplorer.rescanCurrentDirectory();
                    }
                };
                runner10.start();
            }
            if (e.getActionCommand() == "New Directory") {
                Thread runner10 = new Thread() {

                    public void run() {
                        String newDir = "NewDirectory";
                        String inputValue = JOptionPane.showInputDialog("New Directory Name", newDir);
                        boolean dirstatus;
                        if (inputValue == null) return; else if (inputValue != null) dirstatus = new File(myFileExplorer.getCurrentDirectory().toString() + File.separatorChar + inputValue).mkdir();
                        myFileExplorer.rescanCurrentDirectory();
                    }
                };
                runner10.start();
            }
            if (e.getActionCommand() == "New File") {
                Thread runner10 = new Thread() {

                    public void run() {
                        try {
                            String newFile = "NewFile";
                            String inputValue = JOptionPane.showInputDialog("New Directory Name", newFile);
                            if (inputValue == null) return;
                            File newfilename = java.io.File.createTempFile(inputValue, ".txt", myFileExplorer.getCurrentDirectory());
                            myFileExplorer.rescanCurrentDirectory();
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(null, "Error to create a file " + ex, "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                        ;
                    }
                };
                runner10.start();
            }
            if (e.getActionCommand() == "Properties") {
                Thread runner11 = new Thread() {

                    public void run() {
                        try {
                            Date d = new Date();
                            d.setTime(myFileExplorer.getSelectedFile().lastModified());
                            String cano = myFileExplorer.getSelectedFile().getCanonicalPath();
                            long size = myFileExplorer.getSelectedFile().length();
                            JOptionPane.showMessageDialog(null, "File Properties " + "\n" + "Last Acces : " + d.toString() + "\n" + "File Size :" + size + "\n" + "Location :" + cano + "\n", "Properties", JOptionPane.INFORMATION_MESSAGE);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(null, "Error to create a file " + ex, "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                        ;
                    }
                };
                runner11.start();
            }
        }
    }

    class DirExpansionListener implements TreeExpansionListener {

        public void treeExpanded(TreeExpansionEvent event) {
            final DefaultMutableTreeNode node = getTreeNode(event.getPath());
            final FileNode fnode = getFileNode(node);
            Thread runner = new Thread() {

                public void run() {
                    if (fnode != null && fnode.expand(node)) {
                        Runnable runnable = new Runnable() {

                            public void run() {
                                myTreeModel.reload(node);
                            }
                        };
                        SwingUtilities.invokeLater(runnable);
                    }
                }
            };
            runner.start();
        }

        public void treeCollapsed(TreeExpansionEvent event) {
        }
    }

    class DirSelectionListener implements TreeSelectionListener {

        public void valueChanged(TreeSelectionEvent event) {
            DefaultMutableTreeNode node = getTreeNode(event.getPath());
            FileNode fnode = getFileNode(node);
            if (fnode != null) {
                myDisplay.setText(fnode.getFile().getAbsolutePath());
                myFileExplorer.setCurrentDirectory(new File(myDisplay.getText()));
            } else myDisplay.setText("");
        }
    }

    public static void main(String argv[]) {
        new GhinExplorer();
    }
}

class IconCellRenderer extends JLabel implements TreeCellRenderer {

    protected Color m_textSelectionColor;

    protected Color m_textNonSelectionColor;

    protected Color m_bkSelectionColor;

    protected Color m_bkNonSelectionColor;

    protected Color m_borderSelectionColor;

    protected boolean m_selected;

    public IconCellRenderer() {
        super();
        m_textSelectionColor = UIManager.getColor("Tree.selectionForeground");
        m_textNonSelectionColor = UIManager.getColor("Tree.textForeground");
        m_bkSelectionColor = UIManager.getColor("Tree.selectionBackground");
        m_bkNonSelectionColor = UIManager.getColor("Tree.textBackground");
        m_borderSelectionColor = UIManager.getColor("Tree.selectionBorderColor");
        setOpaque(false);
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object obj = node.getUserObject();
        setText(obj.toString());
        if (obj instanceof Boolean) setText("Waiting ...");
        if (obj instanceof IconData) {
            IconData idata = (IconData) obj;
            if (expanded) setIcon(idata.getExpandedIcon()); else setIcon(idata.getIcon());
        } else setIcon(null);
        setFont(tree.getFont());
        setForeground(sel ? m_textSelectionColor : m_textNonSelectionColor);
        setBackground(sel ? m_bkSelectionColor : m_bkNonSelectionColor);
        m_selected = sel;
        return this;
    }

    public void paintComponent(Graphics g) {
        Color bColor = getBackground();
        Icon icon = getIcon();
        g.setColor(bColor);
        int offset = 0;
        if (icon != null && getText() != null) offset = (icon.getIconWidth() + getIconTextGap());
        g.fillRect(offset, 0, getWidth() - 1 - offset, getHeight() - 1);
        if (m_selected) {
            g.setColor(m_borderSelectionColor);
            g.drawRect(offset, 0, getWidth() - 1 - offset, getHeight() - 1);
        }
        super.paintComponent(g);
    }
}

class IconData {

    protected Icon m_icon;

    protected Icon m_expandedIcon;

    protected Object m_data;

    public IconData(Icon icon, Object data) {
        m_icon = icon;
        m_expandedIcon = null;
        m_data = data;
    }

    public IconData(Icon icon, Icon expandedIcon, Object data) {
        m_icon = icon;
        m_expandedIcon = expandedIcon;
        m_data = data;
    }

    public Icon getIcon() {
        return m_icon;
    }

    public Icon getExpandedIcon() {
        return m_expandedIcon != null ? m_expandedIcon : m_icon;
    }

    public Object getObject() {
        return m_data;
    }

    public String toString() {
        return m_data.toString();
    }
}

class MyToolbar extends JToolBar {

    public JButton[] button;

    public String[] imageName = { "mundur.png", "maju.png", "tampilkan.png", "keluar.png" };

    public String[] tipText = { "Back", "Next", "Find", "Close" };

    public MyToolbar() {
        button = new JButton[4];
        for (int i = 0; i < imageName.length; i++) {
            add(button[i] = new JButton(new ImageIcon(ClassLoader.getSystemResource(imageName[i]))));
            button[i].setToolTipText(tipText[i]);
        }
    }
}

class MyFileChooser extends JFileChooser {

    protected JDialog createDialog(Component parent) throws HeadlessException {
        JDialog dialog = super.createDialog(parent);
        dialog.setLocation(300, 200);
        dialog.setResizable(false);
        return dialog;
    }
}

class FindFileName extends JPanel {

    protected String NAME_CONTAINS = "contains";

    protected String NAME_IS = "is";

    protected String NAME_STARTS_WITH = "starts with";

    protected String NAME_ENDS_WITH = "ends with";

    protected int NAME_CONTAINS_INDEX = 0;

    protected int NAME_IS_INDEX = 1;

    protected int NAME_STARTS_WITH_INDEX = 2;

    protected int NAME_ENDS_WITH_INDEX = 3;

    protected String[] setcriteria = { NAME_CONTAINS, NAME_IS, NAME_STARTS_WITH, NAME_ENDS_WITH };

    protected JTextField nameField = null;

    protected JComboBox combo = null;

    protected JCheckBox caseCheck = null;

    protected JLabel myLabel = null;

    protected int total = 0;

    protected FileFindResults resultsPanel = null;

    protected JFileChooser filechooser = null;

    protected JLabel numberofiles = null;

    protected Thread findrunner;

    protected JProgressBar pbar;

    FindFileName(final JFileChooser chooser) {
        super();
        filechooser = chooser;
        setLayout(new BorderLayout());
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(0, 5, 2, 2));
        p.setPreferredSize(new Dimension(100, 70));
        myLabel = new JLabel("Find File", SwingConstants.RIGHT);
        p.add(myLabel);
        combo = new JComboBox(setcriteria);
        combo.setFont(new Font("Helvetica", Font.PLAIN, 10));
        combo.setPreferredSize(combo.getPreferredSize());
        p.add(combo);
        nameField = new JTextField(12);
        nameField.setFont(new Font("Helvetica", Font.PLAIN, 10));
        p.add(nameField);
        JPanel panButton = new JPanel();
        panButton.setLayout(new GridLayout(1, 2, 0, 0));
        JButton findbt = new JButton(new ImageIcon(ClassLoader.getSystemResource("filefind.png")));
        findbt.setToolTipText("Find File");
        JButton stopbt = new JButton(new ImageIcon(ClassLoader.getSystemResource("stopfind.png")));
        stopbt.setToolTipText("Stop Find File");
        panButton.add(findbt);
        panButton.add(stopbt);
        p.add(panButton);
        numberofiles = new JLabel("", SwingConstants.RIGHT);
        numberofiles.setDoubleBuffered(true);
        numberofiles.setForeground(Color.red);
        numberofiles.setFont(new Font("Helvetica", Font.PLAIN, 9));
        p.add(numberofiles, SwingConstants.RIGHT);
        p.add(new JLabel("", SwingConstants.RIGHT));
        caseCheck = new JCheckBox("ignore case sensitive", true);
        caseCheck.setForeground(Color.blue);
        caseCheck.setFont(new Font("Helvetica", Font.PLAIN, 10));
        p.add(caseCheck);
        resultsPanel = new FileFindResults();
        resultsPanel.setDoubleBuffered(true);
        ActionListener actionfindbt = new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                findrunner = new Thread() {

                    public void run() {
                        try {
                            resultsPanel.clear();
                            newFindFile();
                            runFileFind(filechooser.getCurrentDirectory());
                            Thread.currentThread().sleep(0);
                        } catch (InterruptedException e) {
                            JOptionPane.showMessageDialog(null, "Error run program file " + e, "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                };
                findrunner.start();
            }
        };
        findbt.addActionListener(actionfindbt);
        ActionListener actionstopbt = new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                findrunner.stop();
            }
        };
        stopbt.addActionListener(actionstopbt);
        add(p, BorderLayout.NORTH);
        add(resultsPanel, BorderLayout.CENTER);
        pbar = new JProgressBar();
        add(pbar, BorderLayout.SOUTH);
    }

    public void addFoundFile(File f) {
        if (resultsPanel != null) resultsPanel.append(f);
    }

    public void getChooser() {
        filechooser.setCurrentDirectory(resultsPanel.getFileSelect());
        filechooser.setSelectedFile(resultsPanel.getFileSelect());
    }

    public void showNumberofFiles(int matches, int total) {
        if (numberofiles == null) return;
        numberofiles.setText("Files Found " + String.valueOf(matches) + " of " + String.valueOf(total));
    }

    protected void newFindFile() {
        total = 0;
        pbar.setValue(0);
    }

    public void updateBar(int percent) {
        pbar.setValue(percent);
    }

    protected void runFileFind(File base) throws InterruptedException {
        File folder = null;
        boolean ignoreCase = true;
        if (base.isDirectory()) folder = base; else folder = base.getParentFile();
        int comboSelected = combo.getSelectedIndex();
        String match = nameField.getText();
        File[] files = folder.listFiles();
        pbar.setMinimum(0);
        pbar.setMaximum(files.length);
        for (int i = 0; i < files.length; i++) {
            total++;
            String filename = files[i].toString();
            if (comboSelected == NAME_CONTAINS_INDEX) {
                if (filename.toLowerCase().indexOf(match.toLowerCase()) >= 0) addFoundFile(files[i]); else if (filename.indexOf(match) >= 0) addFoundFile(files[i]);
            } else if (comboSelected == NAME_IS_INDEX) {
                if (filename.equalsIgnoreCase(match)) addFoundFile(files[i]); else if (filename.equals(match)) addFoundFile(files[i]);
            } else if (comboSelected == NAME_STARTS_WITH_INDEX) {
                if (filename.toLowerCase().startsWith(match.toLowerCase())) addFoundFile(files[i]); else if (filename.startsWith(match)) addFoundFile(files[i]);
            } else if (comboSelected == NAME_ENDS_WITH_INDEX) {
                if (filename.toLowerCase().endsWith(match.toLowerCase())) addFoundFile(files[i]); else if (filename.endsWith(match)) addFoundFile(files[i]);
            }
            showNumberofFiles(resultsPanel.getMatches(), total);
            updateBar(i);
            Thread.currentThread().sleep(1);
            if (files[i].isDirectory()) runFileFind(files[i]);
        }
    }

    class FileFindResults extends JPanel {

        protected DefaultListModel model = null;

        protected JList fileList = null;

        protected JScrollPane resultsScroller;

        protected File fileinih = null;

        FileFindResults() {
            super();
            setLayout(new BorderLayout());
            model = new DefaultListModel();
            fileList = new JList(model);
            fileList.setDoubleBuffered(true);
            fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            fileList.setCellRenderer(new FileFindResultsCellRenderer());
            resultsScroller = new JScrollPane(fileList);
            resultsScroller.setDoubleBuffered(true);
            add(resultsScroller, BorderLayout.CENTER);
            MouseListener mouseListener = new MouseAdapter() {

                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        try {
                            int index = fileList.locationToIndex(e.getPoint());
                            fileinih = (File) model.elementAt(index);
                            getChooser();
                        } catch (Throwable err) {
                        }
                    }
                }
            };
            fileList.addMouseListener(mouseListener);
        }

        public void append(File f) {
            model.addElement(f);
            invalidate();
            repaint();
        }

        public int getMatches() {
            return model.size();
        }

        public void clear() {
            if (model != null) {
                model.removeAllElements();
                invalidate();
                repaint();
            }
        }

        public File getFileSelect() {
            return fileinih;
        }

        /**
			Convenience class for rendering cells in the results list.
		*/
        class FileFindResultsCellRenderer extends JLabel implements ListCellRenderer {

            protected ImageIcon EMAK_ICON = new ImageIcon(ClassLoader.getSystemResource("folder.png"));

            protected ImageIcon TEXT_ICON = new ImageIcon(ClassLoader.getSystemResource("kviewshell.png"));

            protected ImageIcon WEB_ICON = new ImageIcon(ClassLoader.getSystemResource("mozilla.png"));

            protected ImageIcon IMAGE_ICON = new ImageIcon(ClassLoader.getSystemResource("kpaint.png"));

            protected ImageIcon SHEET_ICON = new ImageIcon(ClassLoader.getSystemResource("kspread.png"));

            protected ImageIcon DOKU_ICON = new ImageIcon(ClassLoader.getSystemResource("kword.png"));

            protected ImageIcon JAVA_ICON = new ImageIcon(ClassLoader.getSystemResource("cup.png"));

            protected ImageIcon MEDIA_ICON = new ImageIcon(ClassLoader.getSystemResource("multimedia.png"));

            protected ImageIcon RUN_ICON = new ImageIcon(ClassLoader.getSystemResource("launcher.png"));

            protected ImageIcon ZIP_ICON = new ImageIcon(ClassLoader.getSystemResource("packet.png"));

            protected ImageIcon CLASS_ICON = new ImageIcon(ClassLoader.getSystemResource("duke.png"));

            FileFindResultsCellRenderer() {
                setOpaque(true);
            }

            public Icon getIcon(File f) {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".doc")) return DOKU_ICON; else if (name.endsWith(".txt")) return TEXT_ICON; else if (name.endsWith(".xls")) return SHEET_ICON; else if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".tif")) return IMAGE_ICON; else if (name.endsWith(".jar") || name.endsWith(".java")) return JAVA_ICON; else if (name.endsWith(".html") || name.endsWith(".htm") || name.endsWith(".php")) return WEB_ICON; else if (name.endsWith(".zip") || name.endsWith(".gz")) return ZIP_ICON; else if (name.endsWith(".au") || name.endsWith(".mpeg")) return MEDIA_ICON; else if (name.endsWith(".exe") || name.endsWith(".com") || name.endsWith(".sh") || name.endsWith(".bat")) return RUN_ICON; else if (name.endsWith(".class")) return CLASS_ICON; else if (f.isFile()) return DOKU_ICON;
                if (f.isDirectory()) return EMAK_ICON;
                return null;
            }

            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (index == -1) {
                    int selected = list.getSelectedIndex();
                    if (selected == -1) return this; else index = selected;
                }
                setFont(new java.awt.Font("Dialog", Font.PLAIN, 12));
                String s = value.toString();
                setIcon(getIcon(new File(s)));
                File file = (File) model.elementAt(index);
                setText(file.getAbsolutePath());
                setDoubleBuffered(true);
                if (isSelected) {
                    setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
                    setBackground(list.getSelectionBackground());
                    setForeground(list.getSelectionForeground());
                } else {
                    setBackground(Color.white);
                    setForeground(Color.black);
                }
                return this;
            }
        }
    }
}

class DirView extends javax.swing.filechooser.FileView {

    protected static ImageIcon EMAK_ICON = new ImageIcon(ClassLoader.getSystemResource("folder.png"));

    protected static ImageIcon TEXT_ICON = new ImageIcon(ClassLoader.getSystemResource("kviewshell.png"));

    protected static ImageIcon WEB_ICON = new ImageIcon(ClassLoader.getSystemResource("mozilla.png"));

    protected static ImageIcon IMAGE_ICON = new ImageIcon(ClassLoader.getSystemResource("kpaint.png"));

    protected static ImageIcon SHEET_ICON = new ImageIcon(ClassLoader.getSystemResource("kspread.png"));

    protected static ImageIcon DOKU_ICON = new ImageIcon(ClassLoader.getSystemResource("kword.png"));

    protected static ImageIcon JAVA_ICON = new ImageIcon(ClassLoader.getSystemResource("cup.png"));

    protected static ImageIcon MEDIA_ICON = new ImageIcon(ClassLoader.getSystemResource("multimedia.png"));

    protected static ImageIcon RUN_ICON = new ImageIcon(ClassLoader.getSystemResource("launcher.png"));

    protected static ImageIcon ZIP_ICON = new ImageIcon(ClassLoader.getSystemResource("packet.png"));

    protected static ImageIcon CLASS_ICON = new ImageIcon(ClassLoader.getSystemResource("duke.png"));

    public String getName(File f) {
        String name = f.getName();
        return name.equals("") ? f.getPath() : name;
    }

    public Icon getIcon(File f) {
        String name = f.getName().toLowerCase();
        if (name.endsWith(".doc")) return DOKU_ICON; else if (name.endsWith(".txt")) return TEXT_ICON; else if (name.endsWith(".xls")) return SHEET_ICON; else if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".tif")) return IMAGE_ICON; else if (name.endsWith(".jar") || name.endsWith(".java")) return JAVA_ICON; else if (name.endsWith(".html") || name.endsWith(".htm") || name.endsWith(".php")) return WEB_ICON; else if (name.endsWith(".zip") || name.endsWith(".gz")) return ZIP_ICON; else if (name.endsWith(".au") || name.endsWith(".mpeg")) return MEDIA_ICON; else if (name.endsWith(".exe") || name.endsWith(".com") || name.endsWith(".sh") || name.endsWith(".bat")) return RUN_ICON; else if (name.endsWith(".class")) return CLASS_ICON; else if (f.isFile()) return DOKU_ICON;
        if (f.isDirectory()) return EMAK_ICON;
        return null;
    }

    public Boolean isTraversable(File f) {
        return (f.isDirectory() ? Boolean.TRUE : Boolean.FALSE);
    }
}

class SimpleFilter extends javax.swing.filechooser.FileFilter {

    private String gdescription = null;

    private String gextension = null;

    public SimpleFilter(String extension, String description) {
        gdescription = description;
        gextension = "." + extension.toLowerCase();
    }

    public String getDescription() {
        return gdescription;
    }

    public boolean accept(File f) {
        if (f == null) return false;
        if (f.isDirectory()) return true;
        return f.getName().toLowerCase().endsWith(gextension);
    }
}

class FileNode {

    protected File m_file;

    public FileNode(File file) {
        m_file = file;
    }

    public File getFile() {
        return m_file;
    }

    public String toString() {
        return m_file.getName().length() > 0 ? m_file.getName() : m_file.getPath();
    }

    public boolean expand(DefaultMutableTreeNode parent) {
        DefaultMutableTreeNode flag = (DefaultMutableTreeNode) parent.getFirstChild();
        if (flag == null) return false;
        Object obj = flag.getUserObject();
        if (!(obj instanceof Boolean)) return false;
        parent.removeAllChildren();
        File[] files = listFiles();
        if (files == null) return true;
        Vector v = new Vector();
        for (int k = 0; k < files.length; k++) {
            File f = files[k];
            if (!(f.isDirectory())) continue;
            FileNode newNode = new FileNode(f);
            boolean isAdded = false;
            for (int i = 0; i < v.size(); i++) {
                FileNode nd = (FileNode) v.elementAt(i);
                if (newNode.compareTo(nd) < 0) {
                    v.insertElementAt(newNode, i);
                    isAdded = true;
                    break;
                }
            }
            if (!isAdded) v.addElement(newNode);
        }
        for (int i = 0; i < v.size(); i++) {
            FileNode nd = (FileNode) v.elementAt(i);
            IconData idata = new IconData(GhinExplorer.ICON_FOLDER, GhinExplorer.ICON_EXPANDEDFOLDER, nd);
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(idata);
            parent.add(node);
            if (nd.hasSubDirs()) node.add(new DefaultMutableTreeNode(new Boolean(true)));
        }
        return true;
    }

    public boolean hasSubDirs() {
        File[] files = listFiles();
        if (files == null) return false;
        for (int k = 0; k < files.length; k++) {
            if (files[k].isDirectory()) return true;
        }
        return false;
    }

    public int compareTo(FileNode toCompare) {
        return m_file.getName().compareToIgnoreCase(toCompare.m_file.getName());
    }

    protected File[] listFiles() {
        if (!m_file.isDirectory()) return null;
        try {
            return m_file.listFiles();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error reading directory " + m_file.getAbsolutePath(), "Warning", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }
}
