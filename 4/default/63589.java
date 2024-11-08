import java.awt.*;
import java.util.*;
import java.io.*;
import java.util.zip.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.filechooser.*;
import symantec.itools.multimedia.ImageViewer;
import com.symantec.itools.javax.swing.icons.ImageIcon;
import com.symantec.itools.javax.swing.borders.BevelBorder;
import com.symantec.itools.javax.swing.JToolBarSeparator;
import com.symantec.itools.javax.swing.borders.EtchedBorder;
import com.symantec.itools.javax.swing.borders.SoftBevelBorder;

public class InstallKit extends javax.swing.JFrame {

    private Image mainIcon;

    protected JScrollPane ikScrollLeft;

    protected JTree ikTree;

    protected JPanel ikBlankPanel = new TitlePanel();

    private myTreeListener treeListener;

    private String currentProject, currentDir;

    private DefaultMutableTreeNode currentNode = null;

    /**
     * Config object that holds the configuration information for
     * this project.
     * 
     * @see EditorConfig
     */
    private EditorConfig myConfig;

    /**
     * Property page form for file objects.
     */
    protected FileProperties ikFileProperties;

    /**
     * Property page form for directory objects.
     */
    protected DirProperties ikDirProperties;

    /**
     * Property page form for settings objects.
     */
    protected SettingProperties ikSettingProperties;

    /**
     * Property page form for CGI objects.
     */
    protected CGIProperties ikCGIProperties;

    public InstallKit() {
        setJMenuBar(ikMenu);
        getContentPane().setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(new java.awt.Color(204, 204, 204));
        setSize(649, 451);
        setVisible(false);
        ikMainPanel.setLayout(new BorderLayout(0, 0));
        getContentPane().add(BorderLayout.CENTER, ikMainPanel);
        ikMainPanel.setBounds(0, 0, 649, 433);
        ikToolBar.setAlignmentY(0.08F);
        ikMainPanel.add(BorderLayout.NORTH, ikToolBar);
        ikToolBar.setBounds(0, 0, 649, 33);
        newProject.setDefaultCapable(false);
        newProject.setToolTipText("New Project");
        newProject.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        ikToolBar.add(newProject);
        newProject.setBounds(16, 4, 93, 27);
        openProject.setDefaultCapable(false);
        openProject.setToolTipText("Open Project");
        openProject.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        ikToolBar.add(openProject);
        openProject.setBounds(109, 4, 93, 27);
        saveProject.setDefaultCapable(false);
        saveProject.setToolTipText("Save Project");
        saveProject.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        ikToolBar.add(saveProject);
        saveProject.setBounds(202, 4, 93, 27);
        ikToolBar.add(JToolBarSeparator1);
        JToolBarSeparator1.setBounds(295, 2, 10, 5);
        runProject.setDefaultCapable(false);
        runProject.setToolTipText("Run Installation");
        runProject.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        ikToolBar.add(runProject);
        runProject.setBounds(305, 4, 93, 27);
        exportDir.setDefaultCapable(false);
        exportDir.setToolTipText("Deploy Installation As Directory");
        exportDir.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        ikToolBar.add(exportDir);
        exportDir.setBounds(398, 4, 93, 27);
        exportPackage.setDefaultCapable(false);
        exportPackage.setToolTipText("Deploy Installation As Zip");
        exportPackage.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        ikToolBar.add(exportPackage);
        exportPackage.setBounds(491, 4, 93, 27);
        exportWeb.setDefaultCapable(false);
        exportWeb.setToolTipText("Deploy Installation For The Web");
        exportWeb.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        ikToolBar.add(exportWeb);
        exportWeb.setBounds(0, 0, 35, 40);
        ikToolBar.add(JToolBarSeparator3);
        JToolBarSeparator3.setBounds(584, 2, 10, 5);
        newFile.setDefaultCapable(false);
        newFile.setToolTipText("Add a New File");
        newFile.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        ikToolBar.add(newFile);
        newFile.setBounds(594, 4, 93, 27);
        newSetting.setDefaultCapable(false);
        newSetting.setToolTipText("Add a New Setting");
        newSetting.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        ikToolBar.add(newSetting);
        newSetting.setBounds(687, 4, 93, 27);
        newDir.setDefaultCapable(false);
        newDir.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        ikToolBar.add(newDir);
        newDir.setBounds(780, 4, 93, 27);
        newCgi.setDefaultCapable(false);
        newCgi.setToolTipText("Add a Server Task");
        newCgi.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        ikToolBar.add(newCgi);
        newCgi.setBounds(873, 4, 93, 27);
        ikToolBar.add(JToolBarSeparator4);
        JToolBarSeparator4.setBounds(966, 2, 10, 5);
        delete.setDefaultCapable(false);
        delete.setToolTipText("Delete");
        delete.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        ikToolBar.add(delete);
        delete.setBounds(976, 4, 93, 27);
        ikToolBar.add(JToolBarSeparator2);
        JToolBarSeparator2.setBounds(1069, 2, 10, 5);
        about.setDefaultCapable(false);
        about.setToolTipText("About Install Manager");
        about.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        ikToolBar.add(about);
        about.setBounds(1079, 4, 93, 27);
        ikSplitPane.setOpaque(true);
        ikMainPanel.add(BorderLayout.CENTER, ikSplitPane);
        ikSplitPane.setBounds(0, 33, 649, 400);
        ikStatusBar.setFloatable(false);
        ikStatusBar.setAlignmentY(0.466667F);
        getContentPane().add(BorderLayout.SOUTH, ikStatusBar);
        ikStatusBar.setBounds(0, 433, 649, 18);
        ikStatusText.setText("� Copyright Governor Technology, 1998");
        ikStatusBar.add(ikStatusText);
        ikStatusText.setForeground(java.awt.Color.black);
        ikStatusText.setFont(new Font("Dialog", Font.PLAIN, 11));
        ikStatusText.setBounds(2, 2, 195, 14);
        fileMenu.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        fileMenu.setText("File");
        fileMenu.setActionCommand("File");
        fileMenu.setMnemonic((int) 'F');
        ikMenu.add(fileMenu);
        newItem.setText("New Project");
        newItem.setActionCommand("New");
        newItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.Event.CTRL_MASK));
        newItem.setMnemonic((int) 'N');
        fileMenu.add(newItem);
        openItem.setText("Open Project...");
        openItem.setActionCommand("Open...");
        openItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.Event.CTRL_MASK));
        openItem.setMnemonic((int) 'O');
        fileMenu.add(openItem);
        saveItem.setText("Save Project");
        saveItem.setActionCommand("Save");
        saveItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.Event.CTRL_MASK));
        saveItem.setMnemonic((int) 'S');
        fileMenu.add(saveItem);
        saveAsItem.setText("Save Project As...");
        saveAsItem.setActionCommand("Save As...");
        saveAsItem.setMnemonic((int) 'A');
        fileMenu.add(saveAsItem);
        fileMenu.add(JSeparator3);
        deployItem.setText("Deploy Project to Directory...");
        deployItem.setActionCommand("Deploy Project to Directory...");
        fileMenu.add(deployItem);
        packageItem.setText("Deploy Project as Zip...");
        packageItem.setActionCommand("Export Package...");
        fileMenu.add(packageItem);
        webItem.setText("Deploy Project as Web Installer...");
        webItem.setActionCommand("Deploy Project as Web Installer...");
        fileMenu.add(webItem);
        fileMenu.add(JSeparator1);
        runItem.setText("Run Installer");
        runItem.setActionCommand("Run Installer");
        runItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.Event.CTRL_MASK));
        runItem.setMnemonic((int) 'R');
        fileMenu.add(runItem);
        fileMenu.add(JSeparator2);
        exitItem.setText("Exit");
        exitItem.setActionCommand("Exit");
        exitItem.setMnemonic((int) 'X');
        fileMenu.add(exitItem);
        insertMenu.setText("Insert");
        insertMenu.setActionCommand("Insert");
        insertMenu.setMnemonic((int) 'I');
        ikMenu.add(insertMenu);
        insertFile.setText("New File");
        insertFile.setActionCommand("New File");
        insertFile.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.Event.CTRL_MASK));
        insertFile.setMnemonic((int) 'F');
        insertMenu.add(insertFile);
        insertSetting.setText("New Setting");
        insertSetting.setActionCommand("New Setting");
        insertSetting.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.Event.CTRL_MASK));
        insertSetting.setMnemonic((int) 'S');
        insertMenu.add(insertSetting);
        insertDir.setText("New Directory");
        insertDir.setActionCommand("New Server Task");
        insertDir.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.Event.CTRL_MASK));
        insertMenu.add(insertDir);
        insertCGI.setText("New Server Task");
        insertCGI.setActionCommand("New Server Task");
        insertCGI.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.Event.CTRL_MASK));
        insertMenu.add(insertCGI);
        helpMenu.setText("Help");
        helpMenu.setActionCommand("Help");
        helpMenu.setMnemonic((int) 'H');
        ikMenu.add(helpMenu);
        helpItem.setText("Help...");
        helpItem.setActionCommand("Help...");
        helpMenu.add(helpItem);
        helpMenu.add(JSeparator4);
        aboutItem.setText("About...");
        aboutItem.setActionCommand("About...");
        aboutItem.setMnemonic((int) 'A');
        helpMenu.add(aboutItem);
        try {
            newIcon.setImageLocation(symantec.itools.net.RelativeURL.getURL("images/new.gif"));
        } catch (java.net.MalformedURLException error) {
        }
        try {
            openIcon.setImageLocation(symantec.itools.net.RelativeURL.getURL("images/open.gif"));
        } catch (java.net.MalformedURLException error) {
        }
        try {
            saveIcon.setImageLocation(symantec.itools.net.RelativeURL.getURL("images/save.gif"));
        } catch (java.net.MalformedURLException error) {
        }
        try {
            addFileIcon.setImageLocation(symantec.itools.net.RelativeURL.getURL("images/addfile.gif"));
        } catch (java.net.MalformedURLException error) {
        }
        try {
            addSettingIcon.setImageLocation(symantec.itools.net.RelativeURL.getURL("images/addsetting.gif"));
        } catch (java.net.MalformedURLException error) {
        }
        try {
            addCgiIcon.setImageLocation(symantec.itools.net.RelativeURL.getURL("images/addcgi.gif"));
        } catch (java.net.MalformedURLException error) {
        }
        try {
            addDirIcon.setImageLocation(symantec.itools.net.RelativeURL.getURL("images/adddir.gif"));
        } catch (java.net.MalformedURLException error) {
        }
        try {
            deleteIcon.setImageLocation(symantec.itools.net.RelativeURL.getURL("images/delete.gif"));
        } catch (java.net.MalformedURLException error) {
        }
        try {
            aboutIcon.setImageLocation(symantec.itools.net.RelativeURL.getURL("images/about.gif"));
        } catch (java.net.MalformedURLException error) {
        }
        try {
            runIcon.setImageLocation(symantec.itools.net.RelativeURL.getURL("images/run.gif"));
        } catch (java.net.MalformedURLException error) {
        }
        try {
            packageIcon.setImageLocation(symantec.itools.net.RelativeURL.getURL("images/package.gif"));
        } catch (java.net.MalformedURLException error) {
        }
        try {
            deployIcon.setImageLocation(symantec.itools.net.RelativeURL.getURL("images/directory.gif"));
        } catch (java.net.MalformedURLException error) {
        }
        try {
            webIcon.setImageLocation(symantec.itools.net.RelativeURL.getURL("images/webinstall.gif"));
        } catch (java.net.MalformedURLException error) {
        }
        delete.setIcon(deleteIcon);
        insertCGI.setIcon(addCgiIcon);
        newProject.setIcon(newIcon);
        openItem.setIcon(openIcon);
        openProject.setIcon(openIcon);
        saveProject.setIcon(saveIcon);
        runProject.setIcon(runIcon);
        newDir.setIcon(addDirIcon);
        exportWeb.setIcon(webIcon);
        deployItem.setIcon(deployIcon);
        newCgi.setIcon(addCgiIcon);
        insertFile.setIcon(addFileIcon);
        packageItem.setIcon(packageIcon);
        newItem.setIcon(newIcon);
        insertSetting.setIcon(addSettingIcon);
        insertDir.setIcon(addDirIcon);
        about.setIcon(aboutIcon);
        exportDir.setIcon(deployIcon);
        newSetting.setIcon(addSettingIcon);
        runItem.setIcon(runIcon);
        webItem.setIcon(webIcon);
        exportPackage.setIcon(packageIcon);
        newFile.setIcon(addFileIcon);
        saveItem.setIcon(saveIcon);
        SymWindow aSymWindow = new SymWindow();
        this.addWindowListener(aSymWindow);
        SymAction lSymAction = new SymAction();
        openProject.addActionListener(lSymAction);
        newProject.addActionListener(lSymAction);
        openItem.addActionListener(lSymAction);
        newItem.addActionListener(lSymAction);
        exitItem.addActionListener(lSymAction);
        aboutItem.addActionListener(lSymAction);
        about.addActionListener(lSymAction);
        newFile.addActionListener(lSymAction);
        delete.addActionListener(lSymAction);
        insertFile.addActionListener(lSymAction);
        insertSetting.addActionListener(lSymAction);
        saveProject.addActionListener(lSymAction);
        runItem.addActionListener(lSymAction);
        newSetting.addActionListener(lSymAction);
        runProject.addActionListener(lSymAction);
        packageItem.addActionListener(lSymAction);
        exportPackage.addActionListener(lSymAction);
        deployItem.addActionListener(lSymAction);
        exportDir.addActionListener(lSymAction);
        insertCGI.addActionListener(lSymAction);
        newCgi.addActionListener(lSymAction);
        saveItem.addActionListener(lSymAction);
        newDir.addActionListener(lSymAction);
        insertDir.addActionListener(lSymAction);
        helpItem.addActionListener(lSymAction);
        exportWeb.addActionListener(lSymAction);
        Dimension minSize = new Dimension(100, 200);
        ikFileProperties = new FileProperties();
        ikFileProperties.setMinimumSize(minSize);
        ikSettingProperties = new SettingProperties(this);
        ikSettingProperties.setMinimumSize(minSize);
        ikDirProperties = new DirProperties(this);
        ikDirProperties.setMinimumSize(minSize);
        ikCGIProperties = new CGIProperties(this);
        ikCGIProperties.setMinimumSize(minSize);
        setTitle("Web Application Install Kit");
        Toolkit tk = getToolkit();
        mainIcon = tk.getImage("images/main.gif");
        setIconImage(mainIcon);
        center(tk);
        ikScrollLeft = new JScrollPane();
        ikScrollLeft.setViewportView(ikTree = new JTree());
        ikScrollLeft.setMinimumSize(minSize);
        ikSplitPane.setLeftComponent(ikScrollLeft);
        ikTree.setRootVisible(false);
        ikTree.setShowsRootHandles(true);
        treeListener = new myTreeListener();
        ikTree.addTreeSelectionListener(treeListener);
        ikSplitPane.setDividerLocation(180);
        newProject();
    }

    /**
	 * Class constructor. Creates all of the user interface elements
	 * for this application and initialises the splitter panes.
	 * 
	 * @param sTitle
	 */
    public InstallKit(String sTitle) {
        this();
        setTitle(sTitle);
    }

    public static void main(String args[]) {
        InstallKit k = new InstallKit();
        k.setVisible(true);
    }

    boolean frameSizeAdjusted = false;

    public void addNotify() {
        Dimension size = getSize();
        super.addNotify();
        if (frameSizeAdjusted) return;
        frameSizeAdjusted = true;
        Insets insets = getInsets();
        javax.swing.JMenuBar menuBar = getRootPane().getJMenuBar();
        int menuBarHeight = 0;
        if (menuBar != null) menuBarHeight = menuBar.getPreferredSize().height;
        setSize(insets.left + insets.right + size.width, insets.top + insets.bottom + size.height + menuBarHeight);
    }

    javax.swing.JPanel ikMainPanel = new javax.swing.JPanel();

    javax.swing.JToolBar ikToolBar = new javax.swing.JToolBar();

    javax.swing.JButton newProject = new javax.swing.JButton();

    javax.swing.JButton openProject = new javax.swing.JButton();

    javax.swing.JButton saveProject = new javax.swing.JButton();

    com.symantec.itools.javax.swing.JToolBarSeparator JToolBarSeparator1 = new com.symantec.itools.javax.swing.JToolBarSeparator();

    javax.swing.JButton runProject = new javax.swing.JButton();

    javax.swing.JButton exportDir = new javax.swing.JButton();

    javax.swing.JButton exportPackage = new javax.swing.JButton();

    javax.swing.JButton exportWeb = new javax.swing.JButton();

    com.symantec.itools.javax.swing.JToolBarSeparator JToolBarSeparator3 = new com.symantec.itools.javax.swing.JToolBarSeparator();

    javax.swing.JButton newFile = new javax.swing.JButton();

    javax.swing.JButton newSetting = new javax.swing.JButton();

    javax.swing.JButton newDir = new javax.swing.JButton();

    javax.swing.JButton newCgi = new javax.swing.JButton();

    com.symantec.itools.javax.swing.JToolBarSeparator JToolBarSeparator4 = new com.symantec.itools.javax.swing.JToolBarSeparator();

    javax.swing.JButton delete = new javax.swing.JButton();

    com.symantec.itools.javax.swing.JToolBarSeparator JToolBarSeparator2 = new com.symantec.itools.javax.swing.JToolBarSeparator();

    javax.swing.JButton about = new javax.swing.JButton();

    javax.swing.JSplitPane ikSplitPane = new javax.swing.JSplitPane();

    javax.swing.JToolBar ikStatusBar = new javax.swing.JToolBar();

    javax.swing.JLabel ikStatusText = new javax.swing.JLabel();

    javax.swing.JMenuBar ikMenu = new javax.swing.JMenuBar();

    javax.swing.JMenu fileMenu = new javax.swing.JMenu();

    javax.swing.JMenuItem newItem = new javax.swing.JMenuItem();

    javax.swing.JMenuItem openItem = new javax.swing.JMenuItem();

    javax.swing.JMenuItem saveItem = new javax.swing.JMenuItem();

    javax.swing.JMenuItem saveAsItem = new javax.swing.JMenuItem();

    javax.swing.JSeparator JSeparator3 = new javax.swing.JSeparator();

    javax.swing.JMenuItem deployItem = new javax.swing.JMenuItem();

    javax.swing.JMenuItem packageItem = new javax.swing.JMenuItem();

    javax.swing.JMenuItem webItem = new javax.swing.JMenuItem();

    javax.swing.JSeparator JSeparator1 = new javax.swing.JSeparator();

    javax.swing.JMenuItem runItem = new javax.swing.JMenuItem();

    javax.swing.JSeparator JSeparator2 = new javax.swing.JSeparator();

    javax.swing.JMenuItem exitItem = new javax.swing.JMenuItem();

    javax.swing.JMenu insertMenu = new javax.swing.JMenu();

    javax.swing.JMenuItem insertFile = new javax.swing.JMenuItem();

    javax.swing.JMenuItem insertSetting = new javax.swing.JMenuItem();

    javax.swing.JMenuItem insertDir = new javax.swing.JMenuItem();

    javax.swing.JMenuItem insertCGI = new javax.swing.JMenuItem();

    javax.swing.JMenu helpMenu = new javax.swing.JMenu();

    javax.swing.JMenuItem helpItem = new javax.swing.JMenuItem();

    javax.swing.JSeparator JSeparator4 = new javax.swing.JSeparator();

    javax.swing.JMenuItem aboutItem = new javax.swing.JMenuItem();

    com.symantec.itools.javax.swing.icons.ImageIcon newIcon = new com.symantec.itools.javax.swing.icons.ImageIcon();

    com.symantec.itools.javax.swing.icons.ImageIcon openIcon = new com.symantec.itools.javax.swing.icons.ImageIcon();

    com.symantec.itools.javax.swing.icons.ImageIcon saveIcon = new com.symantec.itools.javax.swing.icons.ImageIcon();

    com.symantec.itools.javax.swing.icons.ImageIcon addFileIcon = new com.symantec.itools.javax.swing.icons.ImageIcon();

    com.symantec.itools.javax.swing.icons.ImageIcon addSettingIcon = new com.symantec.itools.javax.swing.icons.ImageIcon();

    com.symantec.itools.javax.swing.icons.ImageIcon addCgiIcon = new com.symantec.itools.javax.swing.icons.ImageIcon();

    com.symantec.itools.javax.swing.icons.ImageIcon addDirIcon = new com.symantec.itools.javax.swing.icons.ImageIcon();

    com.symantec.itools.javax.swing.icons.ImageIcon deleteIcon = new com.symantec.itools.javax.swing.icons.ImageIcon();

    com.symantec.itools.javax.swing.icons.ImageIcon aboutIcon = new com.symantec.itools.javax.swing.icons.ImageIcon();

    com.symantec.itools.javax.swing.icons.ImageIcon runIcon = new com.symantec.itools.javax.swing.icons.ImageIcon();

    com.symantec.itools.javax.swing.icons.ImageIcon packageIcon = new com.symantec.itools.javax.swing.icons.ImageIcon();

    com.symantec.itools.javax.swing.icons.ImageIcon deployIcon = new com.symantec.itools.javax.swing.icons.ImageIcon();

    com.symantec.itools.javax.swing.icons.ImageIcon webIcon = new com.symantec.itools.javax.swing.icons.ImageIcon();

    com.symantec.itools.javax.swing.borders.EtchedBorder etchedBorder1 = new com.symantec.itools.javax.swing.borders.EtchedBorder();

    class SymWindow extends java.awt.event.WindowAdapter {

        public void windowClosing(java.awt.event.WindowEvent event) {
            Object object = event.getSource();
            if (object == InstallKit.this) InstallKit_windowClosing(event);
        }
    }

    public void center(Toolkit tk) {
        Dimension scr = tk.getScreenSize();
        int px = scr.width / 2 - getSize().width / 2;
        int py = scr.height / 2 - getSize().height / 2;
        setLocation(px, py - 20);
    }

    class SymAction implements java.awt.event.ActionListener {

        public void actionPerformed(java.awt.event.ActionEvent event) {
            Object object = event.getSource();
            if (object == openProject) openProject_actionPerformed(event); else if (object == newProject) newProject_actionPerformed(event); else if (object == openItem) openProject_actionPerformed(event); else if (object == newItem) newProject_actionPerformed(event); else if (object == exitItem) exitItem_actionPerformed(event); else if (object == aboutItem) about_actionPerformed(event);
            if (object == about) about_actionPerformed(event); else if (object == newFile) newFile_actionPerformed(event); else if (object == delete) delete_actionPerformed(event); else if (object == insertFile) newFile_actionPerformed(event); else if (object == insertSetting) newSetting_actionPerformed(event); else if (object == saveProject) saveProject_actionPerformed(event); else if (object == runItem) runProject_actionPerformed(event); else if (object == newSetting) newSetting_actionPerformed(event); else if (object == runProject) runProject_actionPerformed(event); else if (object == packageItem) exportPackage_actionPerformed(event); else if (object == exportPackage) exportPackage_actionPerformed(event); else if (object == deployItem) exportDir_actionPerformed(event); else if (object == exportDir) exportDir_actionPerformed(event); else if (object == insertCGI) newCGI_actionPerformed(event); else if (object == newCgi) newCGI_actionPerformed(event); else if (object == saveItem) saveProject_actionPerformed(event); else if (object == newDir) newDir_actionPerformed(event); else if (object == insertDir) newDir_actionPerformed(event); else if (object == helpItem) help_actionPerformed(event); else if (object == exportWeb) exportWeb_actionPerformed(event);
        }
    }

    /**
	 * TreeListener for the project tree view. Handles selection and
	 * renaming of project objects, displaying the appropriate
	 * properties form in the right hand pane.
	 */
    class myTreeListener implements javax.swing.event.TreeSelectionListener {

        public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
            if (e.getOldLeadSelectionPath() != null && e.getOldLeadSelectionPath().getPathCount() > 2) comittProperties((DefaultMutableTreeNode) e.getOldLeadSelectionPath().getPathComponent(2));
            if (e.getNewLeadSelectionPath() != null && e.getNewLeadSelectionPath().getPathCount() > 2) {
                DefaultMutableTreeNode tn = (DefaultMutableTreeNode) e.getNewLeadSelectionPath().getPathComponent(1);
                currentNode = (DefaultMutableTreeNode) e.getNewLeadSelectionPath().getPathComponent(2);
                String category = (String) tn.getUserObject();
                if (category.equals("Files")) ikSplitPane.setRightComponent(ikFileProperties); else if (category.equals("Settings")) ikSplitPane.setRightComponent(ikSettingProperties); else if (category.equals("Directories")) ikSplitPane.setRightComponent(ikDirProperties); else if (category.equals("Server Tasks")) ikSplitPane.setRightComponent(ikCGIProperties); else ikSplitPane.setRightComponent(ikBlankPanel);
                displayProperties(currentNode);
            } else {
                currentNode = null;
                ikSplitPane.setRightComponent(ikBlankPanel);
            }
            ikSplitPane.setDividerLocation(180);
            ikSplitPane.repaint();
        }
    }

    /**
	 * Handler for window closing event.
	 * 
	 * @param event The event that triggered this handler.
	 */
    void InstallKit_windowClosing(java.awt.event.WindowEvent event) {
        System.exit(0);
    }

    /**
	 * Handler for open project menu item. Displays an open dialog
	 * box and opens the project file selected by the user.
	 * 
	 * @param event The event that triggered this handler.
	 * @see openProject
	 */
    void openProject_actionPerformed(java.awt.event.ActionEvent event) {
        JFileChooser chooser = new JFileChooser("projects/");
        chooser.setFileFilter(new InstallKitFileFilter("ikp", "Install Kit Projects"));
        chooser.setFileSelectionMode(chooser.FILES_ONLY);
        chooser.setDialogTitle("Open Project");
        int ret = chooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            String filename = chooser.getSelectedFile().getPath();
            openProject(filename);
        }
    }

    /**
     * Opens a project file and loads project details into the
     * project view.
     * 
     * @param filename Path name of project file to load.
     */
    void openProject(String filename) {
        currentProject = filename;
        myConfig = new EditorConfig(filename);
        Vector fileList = myConfig.getFiles();
        Vector settingList = myConfig.getSettings();
        Vector cgiList = myConfig.getServerTasks();
        Vector dirList = myConfig.getDirectories();
        Vector treeData = new Vector();
        DefaultMutableTreeNode files, settings, cgis, dirs;
        DefaultMutableTreeNode tree = new DefaultMutableTreeNode();
        tree.add((files = new DefaultMutableTreeNode("Files", true)));
        for (int i = 0; i < fileList.size(); i++) files.add(new DefaultMutableTreeNode(fileList.elementAt(i), false));
        tree.add((settings = new DefaultMutableTreeNode("Settings", true)));
        for (int i = 0; i < settingList.size(); i++) settings.add(new DefaultMutableTreeNode(settingList.elementAt(i), false));
        tree.add((dirs = new DefaultMutableTreeNode("Directories", true)));
        for (int i = 0; i < dirList.size(); i++) dirs.add(new DefaultMutableTreeNode(dirList.elementAt(i), false));
        tree.add((cgis = new DefaultMutableTreeNode("Server Tasks", true)));
        for (int i = 0; i < cgiList.size(); i++) cgis.add(new DefaultMutableTreeNode(cgiList.elementAt(i), false));
        DefaultTreeModel model = new DefaultTreeModel(tree, true);
        ikTree.removeTreeSelectionListener(treeListener);
        ikTree.setModel(model);
        ikTree.addTreeSelectionListener(treeListener);
        for (int i = 0; i < ikTree.getRowCount(); i++) ikTree.expandRow(i);
    }

    /**
	 * Handler for project new menu item. Deletes all entries from
	 * the project view and starts a fresh project.
	 * 
	 * @param event The event that triggered this handler.
	 * @see newProject
	 */
    void newProject_actionPerformed(java.awt.event.ActionEvent event) {
        newProject();
    }

    /**
	 * Starts a new project. Clears out current project details.
	 */
    void newProject() {
        currentProject = null;
        ikSplitPane.setRightComponent(ikBlankPanel);
        ikSplitPane.validate();
        ikSplitPane.repaint();
        myConfig = new EditorConfig();
        DefaultMutableTreeNode tree = new DefaultMutableTreeNode();
        tree.add(new DefaultMutableTreeNode("Files", true));
        tree.add(new DefaultMutableTreeNode("Settings", true));
        tree.add(new DefaultMutableTreeNode("Directories", true));
        tree.add(new DefaultMutableTreeNode("Server Tasks", true));
        TreeModel model = new DefaultTreeModel(tree, true);
        ikTree.removeTreeSelectionListener(treeListener);
        ikTree.setModel(model);
        ikTree.addTreeSelectionListener(treeListener);
        ikScrollLeft.setMinimumSize(new Dimension(0, 100));
        ikScrollLeft.setPreferredSize(new Dimension(190, 100));
        for (int i = 0; i < ikTree.getRowCount(); i++) ikTree.expandRow(i);
    }

    /**
	 * Handler for file exit menu item. Quits the application.
	 * 
	 * @param event The event that triggered this handler.
	 */
    void exitItem_actionPerformed(java.awt.event.ActionEvent event) {
        System.exit(0);
    }

    /**
	 * Handler for about menu item. Displays the about dialog
	 * box and waits for the user to click OK before continuing.
	 * 
	 * @param event The event that triggered this handler.
	 */
    void about_actionPerformed(java.awt.event.ActionEvent event) {
        try {
            (new AboutDialog()).setVisible(true);
        } catch (Exception e) {
        }
    }

    /**
	 * Comitts current property sheet values to Config object.
	 * 
	 * @see myConfig
	 */
    public void comittProperties() {
        if (currentNode != null) comittProperties(currentNode);
    }

    /**
	 * Comitts property sheet values to Config object for a given
	 * tree node.
	 * 
	 * @param node The tree node representing the object to comitt
	 */
    public void comittProperties(DefaultMutableTreeNode node) {
        if (ikSplitPane.getRightComponent() == ikFileProperties) {
            InstallFile file = (InstallFile) node.getUserObject();
            file.destination = ikFileProperties.getDestination();
            file.permissions = ikFileProperties.getPermissions();
            file.process = ikFileProperties.getProcess();
            file.type = ikFileProperties.getType();
            file.action = ikFileProperties.getAction();
        } else if (ikSplitPane.getRightComponent() == ikSettingProperties) {
            InstallSetting set = (InstallSetting) node.getUserObject();
            set.name = ikSettingProperties.getName();
            set.replace = ikSettingProperties.getReplace();
            set.def = ikSettingProperties.getDefault();
            set.help = ikSettingProperties.getHelp();
            ((DefaultTreeModel) ikTree.getModel()).nodeChanged(node);
        } else if (ikSplitPane.getRightComponent() == ikDirProperties) {
            InstallDirectory dir = (InstallDirectory) node.getUserObject();
            dir.name = ikDirProperties.getDestination();
            dir.permissions = ikDirProperties.getPermissions();
            ((DefaultTreeModel) ikTree.getModel()).nodeChanged(node);
        } else if (ikSplitPane.getRightComponent() == ikCGIProperties) {
            InstallCGI cgi = (InstallCGI) node.getUserObject();
            cgi.name = ikCGIProperties.getName();
            cgi.url = ikCGIProperties.getURL();
            cgi.isPost = ikCGIProperties.isPost();
            cgi.parameterNames = ikCGIProperties.getParameterNames();
            cgi.parameterValues = ikCGIProperties.getParameterValues();
            ((DefaultTreeModel) ikTree.getModel()).nodeChanged(node);
        }
    }

    public void displayProperties(DefaultMutableTreeNode node) {
        if (ikSplitPane.getRightComponent() == ikFileProperties) {
            InstallFile file = (InstallFile) node.getUserObject();
            ikFileProperties.setDestination(file.destination);
            ikFileProperties.setPermissions(file.permissions);
            ikFileProperties.setProcess(file.process);
            ikFileProperties.setType(file.type);
            ikFileProperties.setAction(file.action);
        } else if (ikSplitPane.getRightComponent() == ikSettingProperties) {
            InstallSetting set = (InstallSetting) node.getUserObject();
            ikSettingProperties.setName(set.name);
            ikSettingProperties.setReplace(set.replace);
            ikSettingProperties.setDefault(set.def);
            ikSettingProperties.setHelp(set.help);
        } else if (ikSplitPane.getRightComponent() == ikDirProperties) {
            InstallDirectory dir = (InstallDirectory) node.getUserObject();
            ikDirProperties.setDestination(dir.name);
            ikDirProperties.setPermissions(dir.permissions);
        } else if (ikSplitPane.getRightComponent() == ikCGIProperties) {
            InstallCGI cgi = (InstallCGI) node.getUserObject();
            ikCGIProperties.setName(cgi.name);
            ikCGIProperties.setURL(cgi.url);
            ikCGIProperties.setPost(cgi.isPost);
            ikCGIProperties.setParameterNames(cgi.parameterNames);
            ikCGIProperties.setParameterValues(cgi.parameterValues);
        }
    }

    /**
	 * Handler for insert file menu item. Creates a new file object
	 * in the left hand pane and displays the file properties sheet
	 * for it in the right hand pane.
	 * 
	 * @param event The event that triggered this handler.
	 */
    void newFile_actionPerformed(java.awt.event.ActionEvent event) {
        JFileChooser chooser = new JFileChooser(currentDir);
        chooser.setDialogTitle("Insert New File");
        chooser.setFileSelectionMode(chooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        int ret = chooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            currentDir = chooser.getCurrentDirectory().getPath();
            File[] files = chooser.getSelectedFiles();
            if (files == null || files.length == 0) {
                files = new File[1];
                files[0] = chooser.getSelectedFile();
            }
            for (int i = 0; i < files.length; i++) {
                String filename = files[i].getPath();
                InstallFile fileObj = myConfig.CreateNewFile(filename);
                fileObj.permissions = 644;
                fileObj.destination = files[i].getName();
                fileObj.action = InstallFile.UPDATE;
                fileObj.type = InstallFile.ASCII;
                DefaultMutableTreeNode file = new DefaultMutableTreeNode(fileObj, false);
                DefaultTreeModel model = (DefaultTreeModel) ikTree.getModel();
                model.insertNodeInto(file, (MutableTreeNode) model.getChild(model.getRoot(), 0), 0);
            }
        }
    }

    /**
	 * Handler for insert setting menu item. Creates a new setting
	 * object in the left hand pane and displays the setting
	 * properties sheet for it in the right hand pane.
	 * 
	 * @param event The event that triggered this handler.
	 */
    void newSetting_actionPerformed(java.awt.event.ActionEvent event) {
        InstallSetting settingObj = myConfig.CreateNewSetting("New Setting");
        DefaultMutableTreeNode setting = new DefaultMutableTreeNode(settingObj, false);
        DefaultTreeModel model = (DefaultTreeModel) ikTree.getModel();
        model.insertNodeInto(setting, (MutableTreeNode) model.getChild(model.getRoot(), 1), 0);
    }

    /**
	 * Deletes the currently selected node in the project tree
	 * view.
	 * 
	 * @param event The event that triggered this handler.
	 */
    void delete_actionPerformed(java.awt.event.ActionEvent event) {
        if (ikTree.getSelectionPath().getPathCount() > 2) {
            DefaultTreeModel model = (DefaultTreeModel) ikTree.getModel();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) ikTree.getSelectionPath().getPathComponent(2);
            ikTree.removeTreeSelectionListener(treeListener);
            Object obj = node.getUserObject();
            if (node != null) model.removeNodeFromParent(node);
            ikTree.addTreeSelectionListener(treeListener);
            if (obj.getClass().getName().equals("InstallFile")) myConfig.removeFile((InstallFile) obj); else if (obj.getClass().getName().equals("InstallSetting")) myConfig.removeSetting((InstallSetting) obj);
            ikSplitPane.setRightComponent(ikBlankPanel);
            ikSplitPane.validate();
            ikSplitPane.repaint();
        }
    }

    /**
     * Saves the current project under its existing filename
     *
     */
    void saveCurrentProject() {
        try {
            comittProperties();
            myConfig.save(currentProject);
        } catch (IOException e) {
            System.out.println("Could not save project " + e);
        }
    }

    /**
	 * Handler for save project menu item. Displays a file save
	 * dialog box and saves the project settings in the selected
	 * project file
	 * 
	 * @param event The event that triggered this handler.
	 */
    void saveProject_actionPerformed(java.awt.event.ActionEvent event) {
        JFileChooser chooser = new JFileChooser("projects/");
        chooser.setFileFilter(new InstallKitFileFilter("ikp", "Install Kit Projects"));
        chooser.setFileSelectionMode(chooser.FILES_ONLY);
        chooser.setDialogTitle("Save Project");
        int ret = chooser.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            try {
                String filename = chooser.getSelectedFile().getPath();
                currentProject = filename;
                comittProperties();
                myConfig.save(filename);
            } catch (IOException ioe) {
                try {
                    MessageBox mb = new MessageBox();
                    mb.label.setText("There was an error while saving the project: " + ioe);
                    mb.show();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
	 * Handler for run project menu item. Saves the current project
	 * if not already saved and then runs the installer application
	 * on the saved project file.
	 * 
	 * @param event The event that triggered this handler.
	 */
    void runProject_actionPerformed(java.awt.event.ActionEvent event) {
        if (currentProject == null) saveProject_actionPerformed(event); else saveCurrentProject();
        if (currentProject != null) {
            comittProperties();
            GovInstaller i = new GovInstaller(currentProject);
            Toolkit tk = getToolkit();
            Dimension scr = tk.getScreenSize();
            int px = scr.width / 2 - i.window_cx / 2;
            int py = scr.height / 2 - i.window_cy / 2;
            i.resize(i.window_cx, i.window_cy);
            i.move(px, py);
            i.show();
        }
    }

    /**
	 * Handler for the export package menu item. Displays a file
	 * save dialog box and exports the current project files to
	 * a zipped installation package.
	 * 
	 * @param event The event that triggered this handler.
	 */
    void exportPackage_actionPerformed(java.awt.event.ActionEvent event) {
        JFileChooser chooser = new JFileChooser("");
        chooser.setFileFilter(new InstallKitFileFilter("zip", "Zip Files"));
        chooser.setFileSelectionMode(chooser.FILES_ONLY);
        chooser.setDialogTitle("Deploy Project as Zip Package");
        int ret = chooser.showDialog(this, "Deploy");
        if (ret == JFileChooser.APPROVE_OPTION) exportProject(chooser.getSelectedFile().getPath());
    }

    /**
	 * Exports the current project to the specified file.
	 * 
	 * @param filename The path name of the zip file to export.
	 */
    void exportProject(String filename) {
        if (currentProject != null) {
            try {
                InstallFile file;
                comittProperties();
                FileOutputStream o = new FileOutputStream(filename);
                GovZipOutputStream zip = new GovZipOutputStream(o);
                myConfig.save("bin/install.xml", true);
                zip.writeFile("bin/install.xml", "install.xml");
                zip.writeFile("bin/Installer.jar", "Installer.jar");
                zip.writeFile("bin/Install.exe", "Install.exe");
                zip.writeFile("bin/icon.gif", "icon.gif");
                zip.writeFile("bin/main.gif", "main.gif");
                zip.writeFile("bin/setting.gif", "setting.gif");
                zip.writeFile("bin/settingh.gif", "settingh.gif");
                Vector fileList = myConfig.getFiles();
                for (int i = 0; i < fileList.size(); i++) {
                    file = (InstallFile) fileList.elementAt(i);
                    zip.writeFile(file.localFile);
                }
                zip.close();
            } catch (IOException ioe) {
                try {
                    MessageBox mb = new MessageBox();
                    mb.label.setText("There was an error while deploying the project: " + ioe);
                    mb.show();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
	 * Handler for the export directory menu item. Displays a file
	 * save dialog box and exports the current project installation
	 * files to the selected directory.
	 * 
	 * @param event The event that triggered this handler.
	 */
    void exportDir_actionPerformed(java.awt.event.ActionEvent event) {
        JFileChooser chooser = new JFileChooser("");
        chooser.setFileSelectionMode(chooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Deploy Project as Directory");
        int ret = chooser.showDialog(this, "Deploy");
        if (ret == JFileChooser.APPROVE_OPTION) exportDir(chooser.getSelectedFile().getPath());
    }

    /**
	 * Exports project installation files to the specified
	 * directory.
	 * 
	 * @param dirName The path of the directory to export to.
	 * @see HelpWindow
	 */
    void exportDir(String dirName) {
        if (currentProject != null) {
            try {
                InstallFile file;
                String fileName;
                int index;
                comittProperties();
                myConfig.save(fs("bin/install.xml"), true);
                copyFile(fs("bin/install.xml"), dirName + fs("/install.xml"));
                copyFile(fs("bin/Installer.jar"), dirName + fs("/Installer.jar"));
                copyFile(fs("bin/Install.exe"), dirName + fs("/Install.exe"));
                copyFile(fs("bin/icon.gif"), dirName + fs("/icon.gif"));
                copyFile(fs("bin/main.gif"), dirName + fs("/main.gif"));
                copyFile(fs("bin/setting.gif"), dirName + fs("/setting.gif"));
                copyFile(fs("bin/settingh.gif"), dirName + fs("/settingh.gif"));
                Vector fileList = myConfig.getFiles();
                for (int i = 0; i < fileList.size(); i++) {
                    file = (InstallFile) fileList.elementAt(i);
                    index = file.localFile.lastIndexOf(File.separatorChar);
                    fileName = file.localFile.substring(index);
                    copyFile(file.localFile, dirName + fileName);
                }
            } catch (IOException ioe) {
                try {
                    MessageBox mb = new MessageBox();
                    mb.label.setText("There was an error while deploying the project: " + ioe);
                    mb.show();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
	 * Handler for the export web menu item. Displays a file
	 * save dialog box and exports web based installation files
	 * to the selected directory.
	 * 
	 * @param event The event that triggered this handler.
	 */
    void exportWeb_actionPerformed(java.awt.event.ActionEvent event) {
        JFileChooser chooser = new JFileChooser("");
        chooser.setFileSelectionMode(chooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Deploy Project as Web Installer");
        int ret = chooser.showDialog(this, "Deploy");
        if (ret == JFileChooser.APPROVE_OPTION) exportWeb(chooser.getSelectedFile().getPath());
    }

    /**
     * Converts all foward-slash characters in a path string to
     * the correct separator for the current platform.
     *
     * @param s The string to convert
     * @return The converted string
     */
    String fs(String s) {
        return s.replace('/', File.separatorChar);
    }

    /**
	 * Exports installation files for a web based installation
	 * to the specified directory.
	 * 
	 * @param dirName Path name of the directory to export to.
	 */
    void exportWeb(String dirName) {
        if (currentProject != null) {
            try {
                InstallFile file;
                String fileName;
                int index;
                comittProperties();
                myConfig.save(fs("bin/install.xml"), true);
                new File(dirName + fs("/webinstall/")).mkdir();
                copyFile(fs("bin/install.xml"), dirName + fs("/webinstall/install.xml"));
                copyDir(fs("bin/webinstall/"), dirName + File.separatorChar);
                Vector fileList = myConfig.getFiles();
                for (int i = 0; i < fileList.size(); i++) {
                    file = (InstallFile) fileList.elementAt(i);
                    index = file.localFile.lastIndexOf(File.separatorChar);
                    fileName = file.localFile.substring(index);
                    copyFile(file.localFile, dirName + fs("/webinstall") + fileName);
                }
            } catch (IOException ioe) {
                try {
                    MessageBox mb = new MessageBox();
                    mb.label.setText("There was an error while deploying the project: " + ioe);
                    mb.show();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Recursively copies files and sub-directories from one
     * path to another.
     * 
     * @param src The path of the source directory.
     * @param dest The path of the destination directory.
     * @exception java.io.IOException
     */
    void copyDir(String src, String dest) throws IOException {
        File srcPath = new File(src);
        String[] files = srcPath.list();
        for (int i = 0; i < files.length; i++) {
            if (files[i].charAt(0) == '.') continue;
            File test = new File(src + files[i] + File.separatorChar);
            if (test.isDirectory()) {
                new File(dest + files[i] + File.separatorChar).mkdir();
                copyDir(src + files[i] + File.separatorChar, dest + files[i] + File.separatorChar);
            } else copyFile(src + files[i], dest + files[i]);
        }
    }

    /**
	 * Copies a single file from one path to another.
	 * 
	 * @param src The path of the source file.
	 * @param dest The path of the desitination file.
	 * @exception java.io.IOException
	 */
    void copyFile(String src, String dest) throws IOException {
        int amount;
        byte[] buffer = new byte[4096];
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dest);
        while ((amount = in.read(buffer)) != -1) out.write(buffer, 0, amount);
        in.close();
        out.close();
    }

    /**
	 * Handler for the new CGI menu item. Creates a new CGI node
	 * in the project tree pane and displays the CGI properties
	 * form in the right hand pane.
	 * 
	 * @param event The event that triggered this handler.
	 */
    void newCGI_actionPerformed(java.awt.event.ActionEvent event) {
        if (!GovInstaller.DEMO_VERSION) {
            InstallCGI cgiObj = myConfig.CreateNewCGI("New Server Task");
            DefaultMutableTreeNode cgi = new DefaultMutableTreeNode(cgiObj, false);
            DefaultTreeModel model = (DefaultTreeModel) ikTree.getModel();
            model.insertNodeInto(cgi, (MutableTreeNode) model.getChild(model.getRoot(), 3), 0);
        } else {
            LimitedDialog d = new LimitedDialog();
            d.setVisible(true);
        }
    }

    /**
	 * Handler for the new directory menu item. Creates a new
	 * directory object in the project tree pane and displays the
	 * directory properties form in the right hand pane.
	 * 
	 * @param event The event that triggered this handler.
	 */
    void newDir_actionPerformed(java.awt.event.ActionEvent event) {
        if (!GovInstaller.DEMO_VERSION) {
            InstallDirectory dirObj = myConfig.CreateNewDirectory("New Directory");
            dirObj.permissions = 777;
            DefaultMutableTreeNode file = new DefaultMutableTreeNode(dirObj, false);
            DefaultTreeModel model = (DefaultTreeModel) ikTree.getModel();
            model.insertNodeInto(file, (MutableTreeNode) model.getChild(model.getRoot(), 2), 0);
        } else {
            LimitedDialog d = new LimitedDialog();
            d.setVisible(true);
        }
    }

    /**
	 * Handler for the help menu item. Displays a help window with
	 * documentation for the application in HTML format.
	 * 
	 * @param event The event that triggered this handler.
	 */
    void help_actionPerformed(java.awt.event.ActionEvent event) {
        HtmlHelpWindow h = new HtmlHelpWindow();
        h.show();
    }
}
