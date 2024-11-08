package eu.popeye.ui;

import eu.popeye.application.ApplicationFramework;
import eu.popeye.middleware.dataSharing.SharedSpace;
import eu.popeye.middleware.dataSharing.SharedSpaceEvent;
import eu.popeye.middleware.dataSharing.SharedSpaceObserver;
import eu.popeye.middleware.dataSharing.SharingMode;
import eu.popeye.middleware.dataSharing.dataSharingExceptions.CouldNotGetException;
import eu.popeye.middleware.dataSharing.dataSharingExceptions.CouldNotGetLastVersionException;
import eu.popeye.middleware.dataSharing.dataSharingExceptions.DataAlreadyExistException;
import eu.popeye.middleware.dataSharing.dataSharingExceptions.DataDoesNotExistException;
import eu.popeye.middleware.dataSharing.dataSharingExceptions.InvalidPathException;
import eu.popeye.middleware.dataSharing.sharedDataImpls.SharedFile;
import eu.popeye.middleware.groupmanagement.management.WorkgroupManager;
import eu.popeye.middleware.groupmanagement.membership.Member;
import eu.popeye.middleware.pluginmanagement.plugin.PlugInDescriptor;
import eu.popeye.middleware.pluginmanagement.plugin.PlugManager;
import eu.popeye.middleware.pluginmanagement.plugin.Plugin;
import eu.popeye.middleware.pluginmanagement.runtime.data.IPlugData;
import eu.popeye.middleware.pluginmanagement.runtime.data.PlugDataManager;
import eu.popeye.middleware.pluginmanagement.ui.PlugDataWin;
import eu.popeye.middleware.workspacemanagement.Session;
import eu.popeye.middleware.workspacemanagement.SessionBaseInfo;
import eu.popeye.middleware.workspacemanagement.SessionInvitation;
import eu.popeye.middleware.workspacemanagement.Workspace;
import eu.popeye.middleware.workspacemanagement.WorkspaceListener;
import eu.popeye.middleware.workspacemanagement.WorkspaceManagementImpl;
import eu.popeye.networkabstraction.communication.basic.util.ExampleFileFilter;
import eu.popeye.networkabstraction.communication.basic.util.FileUtils;
import eu.popeye.networkabstraction.communication.basic.util.PopeyeException;
import eu.popeye.ui.laf.InnerDesktopUI;
import eu.popeye.ui.laf.ListTabPaneUI;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.JInternalFrame;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import org.jdom.Document;
import org.jdom.Element;
import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;

/**
 * The UI panel controlled by a Workspace.
 * It holds the full interface for the Workspace Explorer, as defined in
 * Popeye D6.2 Deliverable.
 * @author Paolo Gianrossi &lt;paolo.gianrossi@softeco.it&gt;
 */
public class MainViewPanel extends javax.swing.JPanel implements WorkspaceListener {

    /**
     * The main application framework.
     */
    private Workspace ws;

    private DefaultMutableTreeNode plugins;

    private DefaultMutableTreeNode availablePlugins;

    private DefaultMutableTreeNode pluginsFromFS;

    private JTree tree;

    private TreeSelectionListener tsl;

    private DefaultMutableTreeNode availableWSPlugins;

    private PlugManager pluginManager;

    /**
     * dati relativi alle finestre
     */
    private PlugDataWin plugDataWin;

    private PlugManager pm;

    private WorkgroupManager wgManager = null;

    /**
     * riferimenti contenenti le strutture dati
     */
    private PlugDataManager plugDataManager;

    private static final String PLUGIN_DIRECTORY = "downloadedPlugins";

    /**
     * Creates a new MainViewPanel form
     * @param ws Workspace bound to this pane
     */
    public MainViewPanel(Workspace ws) {
        initComponents();
        this.leftTabPane.setUI(new ListTabPaneUI());
        this.virtualDesktopPane.setUI(new InnerDesktopUI());
        PropertyChangeListener maxinMaxout = new MaxinMaxout();
        this.ws = ws;
        initializePluginStuff();
        constructPluginTree();
        java.awt.Component[] c = this.plugDataWin.getMainPanel().getComponents();
        for (int i = 0; i < c.length; i++) {
            System.err.println("INDEX = " + i);
            System.err.println((Container) c[i]);
            JInternalFrame fr = new javax.swing.JInternalFrame();
            fr.setClosable(true);
            fr.setIconifiable(true);
            fr.setMaximizable(true);
            fr.setResizable(true);
            fr.setVisible(true);
            fr.addPropertyChangeListener(JInternalFrame.IS_MAXIMUM_PROPERTY, maxinMaxout);
            fr.addPropertyChangeListener(JInternalFrame.IS_ICON_PROPERTY, maxinMaxout);
            fr.addPropertyChangeListener(JInternalFrame.IS_CLOSED_PROPERTY, maxinMaxout);
            org.jdesktop.layout.GroupLayout jInternalFrame1Layout = new org.jdesktop.layout.GroupLayout(fr.getContentPane());
            fr.getContentPane().setLayout(jInternalFrame1Layout);
            jInternalFrame1Layout.setHorizontalGroup(jInternalFrame1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(0, 320, Short.MAX_VALUE));
            jInternalFrame1Layout.setVerticalGroup(jInternalFrame1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(0, 168, Short.MAX_VALUE));
            virtualDesktopPane.add(fr, javax.swing.JLayeredPane.DEFAULT_LAYER);
            fr.setContentPane((Container) c[i]);
            fr.setSize(new Dimension(550, 450));
            fr.setLocation(100, 100);
            fr.show();
            fr.addInternalFrameListener((Plugin) c[i]);
        }
        WorkspaceManagementImpl.getInstance().registerForWorkspaceEvents(ws, this);
        this.initMembersList();
        this.initSessionList();
    }

    private void initMembersList() {
        DefaultListModel mdl = new DefaultListModel();
        List<Member> memb = this.ws.getGroup().getMembers();
        Iterator<Member> i = memb.iterator();
        for (; i.hasNext(); ) {
            mdl.addElement(i.next().getUsername());
        }
        this.usersList.setModel(mdl);
        this.usersList.validate();
    }

    /**
     * Display open plugin windows in "cascading" style
     */
    public void cascadePluginFrames() {
        int x = 0;
        int y = 0;
        int width = 3 * virtualDesktopPane.getWidth() / 4;
        int height = 3 * virtualDesktopPane.getHeight() / 4;
        for (JInternalFrame frame : virtualDesktopPane.getAllFrames()) {
            if (!frame.isIcon()) {
                try {
                    int frameDistance = frame.getHeight() - frame.getContentPane().getHeight();
                    frame.setMaximum(false);
                    frame.reshape(x, y, width, height);
                    x += frameDistance;
                    y += frameDistance;
                    if (x + frame.getWidth() > virtualDesktopPane.getWidth()) x = 0;
                    if (y + frame.getHeight() > virtualDesktopPane.getHeight()) y = 0;
                } catch (PropertyVetoException ex) {
                }
            }
        }
    }

    private void initSessionList() {
        DefaultListModel mdl = new DefaultListModel();
        Collection<Session> sess = this.ws.getSessions();
        Iterator<Session> i = sess.iterator();
        for (; i.hasNext(); ) {
            mdl.addElement(i.next().getSessionID());
        }
        this.sessionList.setModel(mdl);
        this.sessionList.validate();
    }

    private void initComponents() {
        javax.swing.JPopupMenu desktopMenu;
        usersPopupMnu = new javax.swing.JPopupMenu();
        msgToAllMenu = new javax.swing.JMenuItem();
        desktopMenu = new javax.swing.JPopupMenu();
        fullscreenMenu = new javax.swing.JCheckBoxMenuItem();
        cascadeMenu = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        closeWSMenu = new javax.swing.JMenuItem();
        leftTabPane = new javax.swing.JTabbedPane();
        usersTab = new javax.swing.JScrollPane();
        usersList = new javax.swing.JList();
        sessionsTab = new javax.swing.JScrollPane();
        sessionList = new javax.swing.JList();
        prefsTab = new javax.swing.JScrollPane();
        wsPreferenceTree = new javax.swing.JTree();
        virtualDesktopPane = new javax.swing.JDesktopPane();
        msgToAllMenu.setText("Send message to all users...");
        msgToAllMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                msgToAllMenuActionPerformed(evt);
            }
        });
        usersPopupMnu.add(msgToAllMenu);
        fullscreenMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F11, java.awt.event.InputEvent.CTRL_MASK));
        fullscreenMenu.setText("Fullscreen Mode");
        fullscreenMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fullscreenMenuActionPerformed(evt);
            }
        });
        desktopMenu.add(fullscreenMenu);
        cascadeMenu.setText("Cascade Windows");
        cascadeMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cascadeMenuActionPerformed(evt);
            }
        });
        desktopMenu.add(cascadeMenu);
        desktopMenu.add(jSeparator1);
        closeWSMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_MASK));
        closeWSMenu.setText("Close Workspace");
        closeWSMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeWSMenuActionPerformed(evt);
            }
        });
        desktopMenu.add(closeWSMenu);
        setOpaque(false);
        leftTabPane.setForeground(new java.awt.Color(99, 82, 222));
        usersTab.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 15, 15, 15));
        usersTab.setOpaque(false);
        usersList.setComponentPopupMenu(usersPopupMnu);
        usersList.setForeground(new java.awt.Color(99, 82, 222));
        usersList.setModel(new javax.swing.AbstractListModel() {

            String[] strings = { "Aristoteles", "Euclides", "Plato", "Pythagoras", "Socrates" };

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        usersTab.setViewportView(usersList);
        leftTabPane.addTab("", new javax.swing.ImageIcon(getClass().getResource("/eu/popeye/resources/user_32x32.gif")), usersTab, "Connected Users");
        sessionsTab.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 15, 15, 15));
        sessionsTab.setOpaque(false);
        sessionList.setForeground(new java.awt.Color(99, 82, 222));
        sessionList.setModel(new javax.swing.AbstractListModel() {

            String[] strings = { "Create New...", "Chat with Euclides" };

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        sessionsTab.setViewportView(sessionList);
        leftTabPane.addTab("", new javax.swing.ImageIcon(getClass().getResource("/eu/popeye/resources/service_32x32.gif")), sessionsTab, "Active Sessions");
        prefsTab.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 15, 15, 15));
        prefsTab.setOpaque(false);
        wsPreferenceTree.setForeground(new java.awt.Color(99, 82, 222));
        prefsTab.setViewportView(wsPreferenceTree);
        leftTabPane.addTab("", new javax.swing.ImageIcon(getClass().getResource("/eu/popeye/resources/tool_32x32.gif")), prefsTab, "Workspace Preferences");
        virtualDesktopPane.setBackground(new java.awt.Color(0, 102, 153));
        virtualDesktopPane.setComponentPopupMenu(desktopMenu);
        virtualDesktopPane.setDoubleBuffered(true);
        virtualDesktopPane.setOpaque(false);
        virtualDesktopPane.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                virtualDesktopPaneMouseClicked(evt);
            }
        });
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(leftTabPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 201, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(virtualDesktopPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 533, Short.MAX_VALUE).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup().addContainerGap().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING).add(leftTabPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 493, Short.MAX_VALUE).add(virtualDesktopPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 493, Short.MAX_VALUE)).addContainerGap()));
    }

    private void closeWSMenuActionPerformed(java.awt.event.ActionEvent evt) {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to logout from Workspace " + ws.getWorkspaceName() + "?", "Popeye - Workspace", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            ApplicationFramework.getInstance().logoutFromWs(ws.getWorkspaceName());
            ApplicationFramework.getInstance().getMainframe().removeWSTab(this.ws.getWorkspaceName());
        }
    }

    private void cascadeMenuActionPerformed(java.awt.event.ActionEvent evt) {
        this.cascadePluginFrames();
    }

    private void fullscreenMenuActionPerformed(java.awt.event.ActionEvent evt) {
        ApplicationFramework.getInstance().getMainframe().setFullscreenMode(((JCheckBoxMenuItem) evt.getSource()).getState());
    }

    private void virtualDesktopPaneMouseClicked(java.awt.event.MouseEvent evt) {
        Rectangle messageSpot = new Rectangle(this.virtualDesktopPane.getWidth() - 38, this.virtualDesktopPane.getHeight() - 38, 32, 32);
        if (messageSpot.contains(evt.getX(), evt.getY())) {
            evt.consume();
            ApplicationFramework.getInstance().getMainframe().getGlassPane().setVisible(true);
            ((MessagesGlassPane) ApplicationFramework.getInstance().getMainframe().getGlassPane()).showMessages(true);
            ApplicationFramework.getInstance().getMainframe().repaint();
        }
    }

    /**
     * Event listener for the Send Message to All User popup menu item
     * @param evt The event firing the method
     */
    private void msgToAllMenuActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private javax.swing.JMenuItem cascadeMenu;

    private javax.swing.JMenuItem closeWSMenu;

    private javax.swing.JCheckBoxMenuItem fullscreenMenu;

    private javax.swing.JSeparator jSeparator1;

    private javax.swing.JTabbedPane leftTabPane;

    private javax.swing.JMenuItem msgToAllMenu;

    private javax.swing.JScrollPane prefsTab;

    private javax.swing.JList sessionList;

    private javax.swing.JScrollPane sessionsTab;

    private javax.swing.JList usersList;

    private javax.swing.JPopupMenu usersPopupMnu;

    private javax.swing.JScrollPane usersTab;

    private javax.swing.JDesktopPane virtualDesktopPane;

    private javax.swing.JTree wsPreferenceTree;

    /**
     * The property change listener implementation for
     * "full maximization" of plugin internal windows as
     * in D6.2 §4.3.2.2.
     *
     * XXX Not sure this should be here.. Most probably
     * will be moved to the PluginIternalFrame as soon as we get there.
     */
    private class MaxinMaxout implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
            JInternalFrame jInternalFrame1 = (JInternalFrame) propertyChangeEvent.getSource();
            boolean v = !jInternalFrame1.isMaximum() || jInternalFrame1.isIcon() || jInternalFrame1.isClosed();
            leftTabPane.setVisible(v);
            ApplicationFramework.getInstance().getMainframe().getGlassPane().setVisible(v);
        }
    }

    private void initializePluginStuff() {
        try {
            UIManager.setLookAndFeel(new Plastic3DLookAndFeel());
        } catch (UnsupportedLookAndFeelException e1) {
            e1.printStackTrace();
        }
        plugDataWin = new PlugDataWin(virtualDesktopPane, this.leftTabPane);
        plugDataManager = new PlugDataManager();
        FileUtils.createDirectory(".", PLUGIN_DIRECTORY);
        try {
            pm = new PlugManager(plugDataWin, plugDataManager, ws);
            setPluginManager(pm);
            getPluginManager().start();
            if (false) throw new PopeyeException(new Throwable());
        } catch (PopeyeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Initialization completed.");
        plugDataWin.setPlugManager(getPluginManager());
        plugDataManager.setPlugManager(getPluginManager());
        getPluginManager().init();
        pm.start();
        for (int i = 0; i < plugDataManager.getPluginsList().size(); i++) {
            ((IPlugData) (plugDataManager.getPluginsList().get(i))).activateListeners();
        }
    }

    private class PluginTreeChange implements SharedSpaceObserver {

        public void treeStructureChanged(SharedSpaceEvent event) {
            if (event.type == event.DATA_ADDED) {
                boolean found = false;
                if (event.path.startsWith("/plugin/")) {
                    for (int i = 0; i < availableWSPlugins.getChildCount(); i++) {
                        System.err.println(((DefaultMutableTreeNode) availableWSPlugins.getChildAt(i)).toString());
                        if (((DefaultMutableTreeNode) availableWSPlugins.getChildAt(i)).toString().equals(event.path.substring(8))) {
                            found = true;
                            i = availableWSPlugins.getChildCount();
                        }
                    }
                    if (!found) {
                        DefaultMutableTreeNode child = new DefaultMutableTreeNode(event.path.substring(8));
                        availableWSPlugins.add(child);
                        tree.updateUI();
                    }
                }
                found = false;
            }
        }
    }

    private class TreePluginSelection implements TreeSelectionListener {

        public void valueChanged(TreeSelectionEvent e) {
            if (!((e.getPath().toString()).equals("[Plugins]"))) {
                if ((e.getPath().getParentPath().toString()).equals("[Plugins, Local Available Plugins]")) {
                    System.err.println((DefaultMutableTreeNode) tree.getLastSelectedPathComponent());
                    PlugInDescriptor pd = pm.getPluginDescriptor(((DefaultMutableTreeNode) tree.getLastSelectedPathComponent()).toString());
                    try {
                        pm.instantiatePlugin(pd, plugDataWin.getMainPanel(), plugDataWin.getPlugManager().getWorkspace().getGroup().getWorkgroupName());
                    } catch (PopeyeException e1) {
                        e1.printStackTrace();
                    }
                } else if ((e.getPath().getParentPath().toString()).equals("[Plugins, Workspace Available Plugins]")) {
                    System.err.println((DefaultMutableTreeNode) tree.getLastSelectedPathComponent());
                    SharedSpace ss = plugDataWin.getPlugManager().getWorkspace().getSharedSpace();
                    SharedFile sd = null;
                    try {
                        sd = (SharedFile) ss.accessRead("/plugin/" + (DefaultMutableTreeNode) tree.getLastSelectedPathComponent());
                    } catch (DataDoesNotExistException e1) {
                        e1.printStackTrace();
                    } catch (CouldNotGetException e1) {
                        e1.printStackTrace();
                    } catch (CouldNotGetLastVersionException e1) {
                        e1.printStackTrace();
                    }
                    sd.save(System.getProperty("user.dir") + File.separator + PLUGIN_DIRECTORY + File.separator + (DefaultMutableTreeNode) tree.getLastSelectedPathComponent());
                    File plug = new File(System.getProperty("user.dir") + File.separator + PLUGIN_DIRECTORY + File.separator + (DefaultMutableTreeNode) tree.getLastSelectedPathComponent());
                    PlugInDescriptor pd = null;
                    ;
                    try {
                        pd = pm.installPlugin(plug);
                    } catch (PopeyeException e1) {
                        e1.printStackTrace();
                    }
                    try {
                        pm.instantiatePlugin(pd, plugDataWin.getMainPanel(), plugDataWin.getPlugManager().getWorkspace().getGroup().getWorkgroupName());
                    } catch (PopeyeException e1) {
                        e1.printStackTrace();
                    }
                } else if ((e.getPath().toString()).equals("[Plugins, Install plugins from File System]")) {
                    JFileChooser chooser = new JFileChooser();
                    ExampleFileFilter filter = new ExampleFileFilter();
                    filter.addExtension("jar");
                    filter.setDescription("JAR files");
                    chooser.setFileFilter(filter);
                    int returnVal = chooser.showOpenDialog(null);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        System.out.println("You chose to open this file: " + chooser.getSelectedFile().getAbsoluteFile());
                        installPluginFromFS(chooser.getSelectedFile().getAbsoluteFile());
                    }
                }
            }
        }
    }

    private void installPluginFromFS(File file) {
        PluginFrameworkDialogs dialogs = new PluginFrameworkDialogsSwing(this);
        String filename = file.getName();
        File destFile = new File("." + File.separator + PLUGIN_DIRECTORY + File.separator + filename);
        if (destFile.exists()) {
            if (dialogs != null) if (!dialogs.overwriteFile(filename)) {
                return;
            }
        }
        try {
            FileUtils.copyFile(file, destFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        PlugInDescriptor selectedPd = null;
        ;
        try {
            selectedPd = pluginManager.installPlugin(destFile);
        } catch (PopeyeException e) {
            dialogs.showErrorMessage(e.getMessage());
        }
        try {
            pluginManager.instantiatePlugin(selectedPd, plugDataWin.getMainPanel(), plugDataWin.getPlugManager().getWorkspace().getGroup().getWorkgroupName());
        } catch (PopeyeException e) {
            dialogs.showErrorMessage(e.getMessage());
            dialogs.showErrorMessage(e.getStackTrace().toString());
        }
    }

    /**
     * Builds the tree of plugins to be displayed in the "Plugins" pane.
     */
    public void constructPluginTree() {
        plugins = new DefaultMutableTreeNode("Plugins");
        availablePlugins = new DefaultMutableTreeNode("Local Available Plugins");
        availableWSPlugins = new DefaultMutableTreeNode("Workspace Available Plugins");
        pluginsFromFS = new DefaultMutableTreeNode("Install plugins from File System");
        tree = new JTree(plugins);
        tsl = new TreePluginSelection();
        tree.addTreeSelectionListener(tsl);
        for (int i = 0; i < plugDataWin.getPlugManager().getPluginInFeature().length; i++) {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(plugDataWin.getPlugManager().getPluginInFeature()[i].getIdentifier());
            availablePlugins.add(child);
        }
        plugins.add(availablePlugins);
        Workspace ws = plugDataWin.getPlugManager().getWorkspace();
        PluginTreeChange pluginTreeChange = new PluginTreeChange();
        ws.getSharedSpace().registerForNotification(pluginTreeChange);
        try {
            if (!ws.getSharedSpace().exists("/plugin")) ws.getSharedSpace().shareData(null, "/plugin", SharingMode.readOnly, true);
        } catch (DataAlreadyExistException e1) {
        } catch (InvalidPathException e1) {
        }
        for (int i = 0; i < plugDataWin.getPlugManager().getPluginInFeature().length; i++) {
            SharedFile sf = new SharedFile();
            sf.retrieve(plugDataWin.getPlugManager().getPluginInFeature()[i].getDirectory() + File.separator + plugDataWin.getPlugManager().getPluginInFeature()[i].getJarName());
            try {
                ws.getSharedSpace().shareData(sf, "/plugin/" + plugDataWin.getPlugManager().getPluginInFeature()[i].getJarName(), SharingMode.readOnly, false);
            } catch (DataAlreadyExistException e) {
            } catch (InvalidPathException e) {
            }
        }
        Document doc = ws.getSharedSpace().listData();
        Element element = doc.getRootElement();
        List elementList = element.getChildren();
        Iterator listIterator = elementList.iterator();
        while (listIterator.hasNext()) {
            Element currentElement = (Element) listIterator.next();
            String label = currentElement.getAttribute("alias").getValue();
            if (label.equals("plugin")) {
                List listChild = currentElement.getChildren();
                Iterator listIteratorChild = listChild.iterator();
                while (listIteratorChild.hasNext()) {
                    Element elementChild = (Element) listIteratorChild.next();
                    boolean found = false;
                    for (int i = 0; i < availableWSPlugins.getChildCount(); i++) {
                        System.err.println(((DefaultMutableTreeNode) availableWSPlugins.getChildAt(i)).toString());
                        if (((DefaultMutableTreeNode) availableWSPlugins.getChildAt(i)).toString().equals(elementChild.getAttribute("alias").getValue())) {
                            found = true;
                            i = availableWSPlugins.getChildCount();
                        }
                    }
                    if (!found) {
                        DefaultMutableTreeNode child = new DefaultMutableTreeNode(elementChild.getAttribute("alias").getValue());
                        availableWSPlugins.add(child);
                    }
                }
            }
        }
        tree.updateUI();
        boolean found = false;
        for (int j = 0; j < plugDataWin.getPlugManager().getPluginInFeature().length; j++) {
            for (int i = 0; i < availableWSPlugins.getChildCount(); i++) {
                if (((DefaultMutableTreeNode) availableWSPlugins.getChildAt(i)).toString().equals(plugDataWin.getPlugManager().getPluginInFeature()[j].getJarName())) {
                    found = true;
                    i = availableWSPlugins.getChildCount();
                }
            }
            if (!found) {
                DefaultMutableTreeNode child = new DefaultMutableTreeNode(plugDataWin.getPlugManager().getPluginInFeature()[j].getJarName());
                availableWSPlugins.add(child);
            }
            found = false;
        }
        plugins.add(availableWSPlugins);
        plugins.add(pluginsFromFS);
        tree.updateUI();
        prefsTab.setViewportView(tree);
    }

    /**
     * Sets the plugin manager.
     * @param pluginManager the plugin manager to bind
     */
    public void setPluginManager(PlugManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    /**
     * gets the plugin manager
     * @return the plugin manager bound to the contained workspace.
     */
    public PlugManager getPluginManager() {
        return pluginManager;
    }

    /**
     * gets the plugDataWin
     * @return the plugdatawin bound to the workspace contained in this panel.
     */
    public PlugDataWin getPlugDataWin() {
        return this.plugDataWin;
    }

    public void onWorkspaceJoined(String workspaceName, Member m) {
        ((DefaultListModel) this.usersList.getModel()).addElement(m.getUsername());
    }

    public void onWorkspaceLeft(String workspaceName, Member m) {
        ((DefaultListModel) this.usersList.getModel()).removeElement(m.getUsername());
    }

    public void onWorkspaceQuit(String workspaceName, Member m) {
    }

    public void onWorkspaceTerminated(String workspaceName) {
        ApplicationFramework.getInstance().getMainframe().getWorkspaceTabPane().remove(this);
    }

    public void onWorkspaceProfileValueChanged() {
    }

    public void onWorkspaceProfileUpdated() {
    }

    public void onSessionCreated(SessionBaseInfo info) {
    }

    public void onSessionJoined(Session session, Member m) {
    }

    public void onSessionQuit(Session session, Member m) {
    }

    public void onSessionTerminated(Session session) {
    }

    public void onSessionProfileValueChanged(Session session) {
    }

    public void onSessionProfileUpdated(Session session) {
    }

    public void onSessionInvitationReceived(SessionInvitation invitation) {
    }

    public void onSessionInvitationAccepted(SessionInvitation invitation) {
    }

    public void onSessionInvitationRefused(SessionInvitation invitation) {
    }

    public void onSessionInvitationAccepted() {
    }

    public void onSessionInvitationReceived() {
    }

    public void onSessionInvitationRefused() {
    }

    public void onSessionProfileUpdated() {
    }

    public void onSessionProfileValueChanged() {
    }

    public void onWorkspaceQuit() {
    }

    public void onWorkspaceTerminated() {
    }
}
