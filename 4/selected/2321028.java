package org.paw.gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.paw.gui.util.Io;

public class MainWindow extends JFrame implements ActionListener, TreeSelectionListener {

    private static final long serialVersionUID = 1L;

    private JToolBar toolBar;

    private JButton toolBarSaveButton, toolBarConnectButton, toolBarShowServerLogButton;

    private JButton toolBarStatusButton, toolBarStopButton, toolBarStartButton, toolBarShutdownButton;

    private JComboBox hostList;

    private LogViewer serverLogViewer = null;

    private Hashtable passHash, hostsHash;

    private ConnectionDialog connectDialog;

    private AuthDialog authDialog;

    public GuiBuilder guiBuilder;

    public AdminConnection currentConnection = null;

    private AdminConnection newConnection = null;

    private final String hostsFile = "hosts";

    private boolean saveInProgress = false;

    static ProgressDialog progressDialog;

    private String lastDirAccessed = null;

    TextEdit textEdit;

    RegExpFilterInfoPanel regExpFilterInfoPanel;

    HandlerInfoPanel handlerInfoPanel;

    FilterInfoPanel filterInfoPanel;

    AdminServerPrefs adminServerPrefs;

    ServerPrefs serverPrefs;

    EmptyPanel emptyPanel = new EmptyPanel();

    public MainWindow(String title) {
        super(title);
        passHash = new Hashtable();
        hostsHash = new Hashtable();
        currentConnection = new AdminConnection();
    }

    public void doUI() {
        guiBuilder = new GuiBuilder(this);
        guiBuilder.addWindowExitListener(this);
        guiBuilder.buildMenu();
        guiBuilder.setFrameIcon("gui_images/framelogo.png");
        toolBar = guiBuilder.createToolBar();
        toolBarSaveButton = guiBuilder.addToToolBar("Sync Configuration", "", "gui_images/sync.png");
        toolBarSaveButton.setEnabled(true);
        toolBarShowServerLogButton = guiBuilder.addToToolBar("Show Serverlog", "", "gui_images/view_log.png");
        toolBarShowServerLogButton.setEnabled(true);
        toolBar.addSeparator();
        toolBarConnectButton = guiBuilder.addToToolBar("Connect", "", "gui_images/connect.png");
        toolBarConnectButton.setEnabled(true);
        hostList = guiBuilder.createComboBox();
        hostList.addItem("New Connection");
        hostList.setMaximumSize(new Dimension(200, (int) hostList.getPreferredSize().getHeight()));
        loadConfig();
        toolBar.addSeparator();
        toolBarStatusButton = guiBuilder.addToToolBar("Server Status", "", "gui_images/info.png");
        toolBarStopButton = guiBuilder.addToToolBar("Stop Server", "", "gui_images/pause.png");
        toolBarStartButton = guiBuilder.addToToolBar("Start Server", "", "gui_images/start.png");
        toolBarShutdownButton = guiBuilder.addToToolBar("Shutdown Server", "", "gui_images/stop.png");
        disableToolBarButtons();
        guiBuilder.createSplitPane();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Sync Configuration")) {
            if (!currentConnection.isValid()) return;
            if (saveInProgress) return;
            saveInProgress = true;
            try {
                int steps = 4;
                progressDialog = new ProgressDialog(this, "Progress", true, false, 1, steps);
                progressDialog.setSize(250, 100);
                progressDialog.setLocationRelativeTo(null);
                final Runnable runnable = new Runnable() {

                    public void run() {
                        try {
                            XmlFileCreator xmlConfig = new XmlFileCreator(currentConnection);
                            String xml;
                            progressDialog.setMessage("Sendig command: " + "server stop");
                            currentConnection.doCommand("server stop");
                            progressDialog.setProgressValue(1);
                            xml = xmlConfig.getServerConfig();
                            progressDialog.setMessage("Syncing file: " + "server.xml");
                            currentConnection.uploadFile("server.xml", xml);
                            xml = xmlConfig.getFilterConfig();
                            progressDialog.setMessage("Syncing file: " + "filter.xml");
                            currentConnection.uploadFile("filter.xml", xml);
                            xml = xmlConfig.getHandlerConfig();
                            progressDialog.setMessage("Syncing file: " + "handler.xml");
                            currentConnection.uploadFile("handler.xml", xml);
                            xml = xmlConfig.getRegFilterConfig();
                            progressDialog.setMessage("Syncing file: " + "reg-filter.xml");
                            currentConnection.uploadFile("reg-filter.xml", xml);
                            progressDialog.setProgressValue(2);
                            Vector files = currentConnection.getFiles();
                            for (Enumeration f = files.elements(); f.hasMoreElements(); ) {
                                GuiFile file = (GuiFile) f.nextElement();
                                progressDialog.setMessage("Syncing file: " + file.getName());
                                currentConnection.uploadFile(file.getName(), file.getContent());
                            }
                            progressDialog.setProgressValue(3);
                            Thread.sleep(1000);
                            progressDialog.setMessage("Sendig command: " + "init");
                            currentConnection.doCommand("init");
                            progressDialog.setMessage("Sendig command: " + "server start");
                            currentConnection.doCommand("server start");
                            progressDialog.setMessage("Sendig command: " + "admin restart");
                            currentConnection.doCommand("admin restart");
                            progressDialog.setProgressValue(4);
                            Thread.sleep(1000);
                            progressDialog.dispose();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                };
                saveInProgress = false;
                Thread thread = new Thread(runnable);
                thread.start();
                progressDialog.setVisible(true);
            } catch (Exception ex) {
                System.out.println(ex);
                saveInProgress = false;
            }
        }
        if (e.getActionCommand().equals("Connect")) {
            if (hostList.getSelectedIndex() == 0) {
                showConnectionDialog();
            } else {
                String hostString = (String) hostList.getSelectedItem();
                String host = hostString.substring(0, hostString.indexOf(":"));
                String port = hostString.substring(hostString.indexOf(":") + 1);
                connectToServer(host, port);
            }
            return;
        }
        if (e.getActionCommand().equals("Show Serverlog")) {
            if (!currentConnection.isValid()) {
                return;
            }
            if (serverLogViewer == null) {
                serverLogViewer = new LogViewer("Server Logfile");
            }
            guiBuilder.clearTreeSelection();
            String serverLogContent = currentConnection.getFileFromServer("getlog", "server.log");
            serverLogViewer.setContent(serverLogContent);
            guiBuilder.updateSplitPane(serverLogViewer);
            return;
        }
        if (e.getActionCommand().equals("Clear Serverlog")) {
            if (!currentConnection.isValid()) {
                return;
            }
            boolean ret = currentConnection.doCommand("clearlog");
            String message = currentConnection.getLastMessage();
            if (ret) {
                message = message.substring(4);
                JOptionPane.showMessageDialog(this, message);
            } else {
                JOptionPane.showMessageDialog(this, "Unable to clear logfile!", "Failed", JOptionPane.WARNING_MESSAGE);
            }
        }
        if (e.getActionCommand().equals("Save Brazil Config")) {
            if (!currentConnection.isValid()) {
                return;
            }
            JFileChooser chooser;
            if (lastDirAccessed == null) {
                chooser = new JFileChooser();
            } else {
                chooser = new JFileChooser(lastDirAccessed);
            }
            chooser.setDialogTitle("Save Brazil Config");
            int ret = chooser.showSaveDialog(null);
            if (ret == JFileChooser.CANCEL_OPTION) {
                return;
            }
            lastDirAccessed = chooser.getCurrentDirectory().getPath();
            String filename = chooser.getSelectedFile().getName();
            String fullFilename = chooser.getSelectedFile().getPath();
            if (new File(fullFilename).exists()) {
                int res = JOptionPane.showConfirmDialog(this, "The file \"" + filename + "\" already exists!\n" + "Do you want to overwrite?", "File exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (res == 1) {
                    return;
                }
            }
            String brazilConfig = currentConnection.getFileFromServer("getBrazilConfig", null);
            Io.writeLocalFile(fullFilename, brazilConfig);
        }
        if (e.getActionCommand().equals("Server Status")) {
            String message = currentConnection.getFileFromServer("status", "");
            if (message != null) {
                JOptionPane.showMessageDialog(this, message);
            } else {
                JOptionPane.showMessageDialog(this, "Error while obtaining status", "Failed", JOptionPane.WARNING_MESSAGE);
            }
        }
        if (e.getActionCommand().equals("Stop Server")) {
            boolean ret = currentConnection.doCommand("server stop");
            String message = currentConnection.getLastMessage();
            if (ret) {
                message = message.substring(4);
                JOptionPane.showMessageDialog(this, message);
            } else {
                JOptionPane.showMessageDialog(this, "Error while stopping server", "Failed", JOptionPane.WARNING_MESSAGE);
            }
        }
        if (e.getActionCommand().equals("Start Server")) {
            currentConnection.doCommand("init");
            boolean ret = currentConnection.doCommand("server start");
            String message = currentConnection.getLastMessage();
            if (ret) {
                message = message.substring(4);
                JOptionPane.showMessageDialog(this, message);
            } else {
                JOptionPane.showMessageDialog(this, "Error while starting server", "Failed", JOptionPane.WARNING_MESSAGE);
            }
        }
        if (e.getActionCommand().equals("Shutdown Server")) {
            String warningText = "Do you really want to shutdown the server?\n" + "After shutdown the server has to be restarted manually!";
            int res = JOptionPane.showConfirmDialog(this, warningText, "Shutdown Server", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (res == 1) {
                return;
            }
            boolean ret = currentConnection.doCommand("shutdown");
            String message = currentConnection.getLastMessage();
            if (ret) {
                message = message.substring(4);
                JOptionPane.showMessageDialog(this, message);
            } else {
                JOptionPane.showMessageDialog(this, "Error while shutting down", "Failed", JOptionPane.WARNING_MESSAGE);
            }
        }
        if (e.getActionCommand().equals("About")) {
            AboutDialog aboutDialog = new AboutDialog("PAW Web Filter " + PawGui.class.getPackage().getImplementationVersion(), true, false);
            aboutDialog.setLocationRelativeTo(null);
            aboutDialog.setVisible(true);
        }
        if (e.getActionCommand().equals("Help")) {
            HelpFrame helpFrame = new HelpFrame("PAW Help");
            helpFrame.setVisible(true);
        }
        if (e.getActionCommand().equals("ConnectToHost")) {
            connectDialog.dispose();
            setupConnection();
            return;
        }
        if (e.getActionCommand().equals("AuthUser")) {
            String user = authDialog.getUser();
            String pass = authDialog.getPass();
            authDialog.dispose();
            authenticate(user, pass);
            return;
        }
        if (e.getActionCommand().equals("ConnectCancel")) {
            connectDialog.dispose();
            return;
        }
        if (e.getActionCommand().equals("AuthUserCancel")) {
            authDialog.dispose();
            return;
        }
        if (e.getActionCommand().equals("Exit")) {
            saveConfig();
            System.exit(0);
        }
        if (e.getActionCommand().equals("Expand tree")) {
            guiBuilder.expandTree();
            return;
        }
        if (e.getActionCommand().equals("Collapse tree")) {
            guiBuilder.collapseTree();
            ;
            return;
        }
        if (e.getActionCommand().equals("ImportHandlersAndFilters")) {
            JFileChooser chooser;
            if (lastDirAccessed == null) {
                chooser = new JFileChooser();
            } else {
                chooser = new JFileChooser(lastDirAccessed);
            }
            chooser.setFileFilter(new XMLFileFilter());
            chooser.setDialogTitle("Import");
            int ret = chooser.showOpenDialog(null);
            if (ret == JFileChooser.CANCEL_OPTION) {
                return;
            }
            lastDirAccessed = chooser.getCurrentDirectory().getPath();
            String filename = chooser.getSelectedFile().getName();
            String fullFilename = chooser.getSelectedFile().getPath();
            String xml = Io.readLocalFile(fullFilename);
            if (xml == null) {
                JOptionPane.showMessageDialog(this, "Error importing file \"" + filename + "\"", "Imported", JOptionPane.ERROR_MESSAGE);
                return;
            }
            doImport(xml, true);
        }
        if (e.getActionCommand().equals("Export")) {
            JFileChooser chooser;
            if (lastDirAccessed == null) {
                chooser = new JFileChooser();
            } else {
                chooser = new JFileChooser(lastDirAccessed);
            }
            chooser.setFileFilter(new XMLFileFilter());
            chooser.setDialogTitle("Export");
            int ret = chooser.showSaveDialog(null);
            if (ret == JFileChooser.CANCEL_OPTION) {
                return;
            }
            lastDirAccessed = chooser.getCurrentDirectory().getPath();
            String filename = chooser.getSelectedFile().getName();
            String fullFilename = chooser.getSelectedFile().getPath();
            if (new File(fullFilename).exists()) {
                int res = JOptionPane.showConfirmDialog(this, "The file \"" + filename + "\" already exists!\n" + "Do you want to overwrite?", "File exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (res == 1) {
                    return;
                }
            }
            String xml = null;
            Object obj = guiBuilder.getTree().getLastSelectedPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj;
            if (node.getUserObject() instanceof GuiRegExpFilter) {
                GuiRegExpFilter regFilter = (GuiRegExpFilter) node.getUserObject();
                Vector v = new Vector();
                v.add(regFilter);
                XmlFileCreator creator = new XmlFileCreator();
                try {
                    xml = creator.getRegFilterConfig(v);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else if (node.getUserObject() instanceof GuiFilter) {
                GuiFilter filter = (GuiFilter) node.getUserObject();
                Vector v = new Vector();
                v.add(filter);
                XmlFileCreator creator = new XmlFileCreator();
                try {
                    xml = creator.getFilterConfig(v);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else if (node.getUserObject() instanceof GuiHandler) {
                GuiHandler handler = (GuiHandler) node.getUserObject();
                Vector v = new Vector();
                v.add(handler);
                XmlFileCreator creator = new XmlFileCreator();
                try {
                    xml = creator.getHandlerConfig(v);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            Io.writeLocalFile(fullFilename, xml);
        }
        if (e.getActionCommand().equals("NewHandler")) {
            String xml = Io.readLocalFile("templates/Handler.xml");
            if (xml != null) {
                doImport(xml, false);
            }
        }
        if (e.getActionCommand().equals("NewFilter")) {
            String xml = Io.readLocalFile("templates/Filter.xml");
            if (xml != null) {
                doImport(xml, false);
            }
        }
        if (e.getActionCommand().equals("NewCustomFilter")) {
            String xml = Io.readLocalFile("templates/CustomFilter.xml");
            if (xml != null) {
                doImport(xml, false);
            }
        }
        if (e.getActionCommand().equals("NewReplaceFilter")) {
            String xml = Io.readLocalFile("templates/RegExpFilter-Replace.xml");
            if (xml != null) {
                doImport(xml, false);
            }
        }
        if (e.getActionCommand().equals("NewSizeFilter")) {
            String xml = Io.readLocalFile("templates/RegExpFilter-Size.xml");
            if (xml != null) {
                doImport(xml, false);
            }
        }
        if (e.getActionCommand().equals("DeleteNode")) {
            Object obj = guiBuilder.getTree().getLastSelectedPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj;
            if (node.getUserObject() instanceof GuiRegExpFilter) {
                GuiRegExpFilter userObj = (GuiRegExpFilter) node.getUserObject();
                int ret = JOptionPane.showConfirmDialog(this, "Delete RegExp Filter \"" + userObj.getName() + "\" ?", "Delete RegExp Filter", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (ret == 1) {
                    return;
                }
                DefaultTreeModel model = (DefaultTreeModel) guiBuilder.getTree().getModel();
                model.removeNodeFromParent(node);
                currentConnection.removeRegExpFilter(userObj);
            } else if (node.getUserObject() instanceof GuiHandler) {
                GuiHandler userObj = (GuiHandler) node.getUserObject();
                int ret = JOptionPane.showConfirmDialog(this, "Delete Handler \"" + userObj.getName() + "\" ?", "Delete Handler", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (ret == 1) {
                    return;
                }
                DefaultTreeModel model = (DefaultTreeModel) guiBuilder.getTree().getModel();
                model.removeNodeFromParent(node);
                currentConnection.removeHandler(userObj);
            } else if (node.getUserObject() instanceof GuiFilter) {
                GuiFilter userObj = (GuiFilter) node.getUserObject();
                int ret = JOptionPane.showConfirmDialog(this, "Delete Filter \"" + userObj.getName() + "\" ?", "Delete Filter", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (ret == 1) {
                    return;
                }
                DefaultTreeModel model = (DefaultTreeModel) guiBuilder.getTree().getModel();
                model.removeNodeFromParent(node);
                currentConnection.removeFilter(userObj);
            }
        }
        if (e.getActionCommand().equals("SwitchNode")) {
            Object obj = guiBuilder.getTree().getLastSelectedPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj;
            if (node.getUserObject() instanceof GuiRegExpFilter) {
                GuiRegExpFilter filter = (GuiRegExpFilter) node.getUserObject();
                if (filter.isActive()) filter.setActive(false); else filter.setActive(true);
                guiBuilder.getTree().treeDidChange();
            }
            if (node.getUserObject() instanceof GuiHandler) {
                GuiHandler handler = (GuiHandler) node.getUserObject();
                if (handler.isActive()) handler.setActive(false); else handler.setActive(true);
                guiBuilder.getTree().treeDidChange();
            }
            if (node.getUserObject() instanceof GuiFilter) {
                GuiFilter filter = (GuiFilter) node.getUserObject();
                if (filter.isActive()) filter.setActive(false); else filter.setActive(true);
                guiBuilder.getTree().treeDidChange();
            }
            return;
        }
        if (e.getActionCommand().equals("MoveNodeUp")) {
            Object obj = guiBuilder.getTree().getLastSelectedPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj;
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            int index = parent.getIndex(node);
            if (index > 0) {
                parent.insert(node, index - 1);
                DefaultTreeModel model = (DefaultTreeModel) guiBuilder.getTree().getModel();
                model.reload(parent);
                guiBuilder.getTree().treeDidChange();
                currentConnection.resyncHandlersAndFiltersWithTree(guiBuilder.getTree());
            }
            return;
        }
        if (e.getActionCommand().equals("MoveNodeDown")) {
            Object obj = guiBuilder.getTree().getLastSelectedPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj;
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            int index = parent.getIndex(node);
            int numChildren = parent.getChildCount();
            if (index != numChildren - 1) {
                parent.insert(node, index + 1);
                DefaultTreeModel model = (DefaultTreeModel) guiBuilder.getTree().getModel();
                model.reload(parent);
                currentConnection.resyncHandlersAndFiltersWithTree(guiBuilder.getTree());
            }
            return;
        }
        if (e.getActionCommand().equals("EditFilterHandler")) {
            Object obj = guiBuilder.getTree().getLastSelectedPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj;
            if (node.getUserObject() instanceof GuiRegExpFilter) {
                GuiRegExpFilter regFilter = (GuiRegExpFilter) node.getUserObject();
                Vector v = new Vector();
                v.add(regFilter);
                XmlFileCreator creator = new XmlFileCreator();
                String xml = null;
                try {
                    xml = creator.getRegFilterConfig(v);
                    TextEdit textEdit = new TextEdit(xml, node, this);
                    guiBuilder.updateSplitPane(textEdit);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else if (node.getUserObject() instanceof GuiFilter) {
                GuiFilter filter = (GuiFilter) node.getUserObject();
                Vector v = new Vector();
                v.add(filter);
                XmlFileCreator creator = new XmlFileCreator();
                String xml = null;
                try {
                    xml = creator.getFilterConfig(v);
                    TextEdit textEdit = new TextEdit(xml, node, this);
                    guiBuilder.updateSplitPane(textEdit);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else if (node.getUserObject() instanceof GuiHandler) {
                GuiHandler handler = (GuiHandler) node.getUserObject();
                Vector v = new Vector();
                v.add(handler);
                XmlFileCreator creator = new XmlFileCreator();
                String xml = null;
                try {
                    xml = creator.getHandlerConfig(v);
                    TextEdit textEdit = new TextEdit(xml, node, this);
                    guiBuilder.updateSplitPane(textEdit);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public boolean logonLocally() {
        boolean ret = false;
        String host = "localhost", user = "admin", pass = "paw";
        int port = 4444;
        currentConnection.setPeer(host, port);
        if (currentConnection.isValid()) {
            authenticate(user, pass, true);
            if (currentConnection.authValid) {
                hostList.setSelectedItem(host + ":" + port);
                ret = true;
            }
        } else ret = false;
        return ret;
    }

    private void setupConnection() {
        String host = connectDialog.getServerName();
        String port = connectDialog.getServerPort();
        connectToServer(host, port);
    }

    private void connectToServer(String host, String port) {
        int intPort = 0;
        try {
            intPort = Integer.decode(port).intValue();
        } catch (Exception nf) {
            intPort = 0;
        }
        if (intPort == 0 || host.length() == 0) {
            JOptionPane.showMessageDialog(this, "Please specify correct values");
            return;
        }
        newConnection = new AdminConnection();
        newConnection.setPeer(host, intPort);
        if (!newConnection.isValid()) {
            JOptionPane.showMessageDialog(this, "Connection failed", "Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (hostsHash.get(host + ":" + port) == null) {
            hostList.addItem(host + ":" + port);
            hostList.setSelectedItem(host + ":" + port);
            hostsHash.put(host + ":" + port, "");
        }
        if (passHash.get(host + ":" + port) == null) {
            showAuthDialog();
        } else {
            String userPass = (String) passHash.get(host + ":" + port);
            String user = userPass.substring(0, userPass.indexOf(":"));
            String pass = userPass.substring(userPass.indexOf(":") + 1);
            authenticate(user, pass);
        }
    }

    private void showConnectionDialog() {
        connectDialog = new ConnectionDialog(this, true);
        connectDialog.setButtonActionListerner(this);
        connectDialog.setLocationRelativeTo(null);
        connectDialog.setVisible(true);
    }

    private void showAuthDialog() {
        authDialog = new AuthDialog(this, true);
        authDialog.setButtonActionListerner(this);
        authDialog.setLocationRelativeTo(null);
        authDialog.setVisible(true);
        ;
    }

    private boolean loadCompleteServerConfig() {
        currentConnection.initFiles();
        if (!currentConnection.readServerConfig()) return false;
        if (!currentConnection.readHandlerConfig()) return false;
        if (!currentConnection.readFilterConfig()) return false;
        if (currentConnection.hasFilterClass("org.paw.filter.PawRegFilter")) if (!currentConnection.readRegFilterConfig()) return false;
        return true;
    }

    public void authenticate(String user, String pass) {
        authenticate(user, pass, false);
    }

    public void authenticate(String user, String pass, boolean quiet) {
        if (newConnection == null) {
            newConnection = currentConnection;
        }
        newConnection.checkAuth(user, pass);
        if (newConnection.authValid) {
            currentConnection = newConnection;
            enableToolBarButtons();
            passHash.put(currentConnection.getHost() + ":" + currentConnection.getPort(), currentConnection.getUser() + ":" + currentConnection.getPass());
            if (loadCompleteServerConfig()) {
                guiBuilder.setupTree(currentConnection.getServerConfig(), currentConnection.getAdminConfig(), currentConnection.getHandlers(), currentConnection.getFilters(), currentConnection.getReplaceFilters(), currentConnection.getSizeFilters(), currentConnection.getFiles());
            } else {
                if (!quiet) JOptionPane.showMessageDialog(this, "Could not load configuration", "Failed", JOptionPane.WARNING_MESSAGE);
            }
        } else showAuthDialog();
    }

    public boolean saveConfig() {
        try {
            BufferedWriter os = new BufferedWriter(new FileWriter(hostsFile));
            for (Enumeration e = hostsHash.keys(); e.hasMoreElements(); ) {
                String s = (String) e.nextElement();
                os.write(s + "\n");
            }
            os.close();
        } catch (IOException ex) {
            return false;
        }
        return true;
    }

    public boolean loadConfig() {
        File f = new File(hostsFile);
        if (f.canRead()) {
            try {
                BufferedReader is = new BufferedReader(new FileReader(f));
                String s = null;
                while ((s = is.readLine()) != null) {
                    if (s.startsWith("#")) continue;
                    if (hostsHash.get(s) == null) hostList.addItem(s);
                    hostsHash.put(s, "");
                }
                is.close();
            } catch (Exception ex) {
                return false;
            }
        } else return false;
        return true;
    }

    private void disableToolBarButtons() {
        toolBarStopButton.setEnabled(false);
        toolBarStatusButton.setEnabled(false);
        toolBarStartButton.setEnabled(false);
        toolBarShutdownButton.setEnabled(false);
    }

    private void enableToolBarButtons() {
        toolBarStopButton.setEnabled(true);
        toolBarStatusButton.setEnabled(true);
        toolBarStartButton.setEnabled(true);
        toolBarShutdownButton.setEnabled(true);
    }

    public void valueChanged(TreeSelectionEvent e) {
        try {
            JTree tree = (JTree) e.getSource();
            DefaultMutableTreeNode node;
            node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node == null) return;
            Object object = node.getUserObject();
            if (object instanceof GuiFile) {
                GuiFile file = (GuiFile) object;
                textEdit = new TextEdit(file.getContent(), node, this);
                guiBuilder.updateSplitPane(textEdit);
            } else if (object instanceof GuiRegExpFilter) {
                regExpFilterInfoPanel = new RegExpFilterInfoPanel(node);
                guiBuilder.updateSplitPane(regExpFilterInfoPanel);
            } else if (object instanceof GuiFilter) {
                filterInfoPanel = new FilterInfoPanel(node, currentConnection);
                guiBuilder.updateSplitPane(filterInfoPanel);
            } else if (object instanceof GuiHandler) {
                handlerInfoPanel = new HandlerInfoPanel(node, currentConnection);
                guiBuilder.updateSplitPane(handlerInfoPanel);
            } else if (object instanceof GuiAdminConfig) {
                adminServerPrefs = new AdminServerPrefs(node);
                guiBuilder.updateSplitPane(new AdminServerPrefs(node));
            } else if (object instanceof GuiServerConfig) {
                serverPrefs = new ServerPrefs(node);
                guiBuilder.updateSplitPane(serverPrefs);
            } else guiBuilder.updateSplitPane(emptyPanel);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void setBusyCursor(boolean busy) {
        Cursor cursor;
        if (busy) {
            cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        } else {
            cursor = Cursor.getDefaultCursor();
        }
        getGlassPane().setCursor(cursor);
        getGlassPane().setVisible(busy);
        setCursor(cursor);
    }

    public AdminConnection getCurrentConnection() {
        return currentConnection;
    }

    private void doImport(String xml, boolean showResult) {
        int handlersBefore = currentConnection.getHandlers().size();
        int filtersBefore = currentConnection.getFilters().size();
        int replaceFiltersBefore = currentConnection.getReplaceFilters().size();
        int sizeFiltersBefore = currentConnection.getSizeFilters().size();
        int filesBefore = currentConnection.getFiles().size();
        try {
            currentConnection.buildHandlerConfig(xml);
            currentConnection.buildFilterConfig(xml);
            currentConnection.buildRegFilterConfig(xml);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
        }
        Vector handlers = currentConnection.getHandlers();
        Vector filters = currentConnection.getFilters();
        Vector replaceFilters = currentConnection.getReplaceFilters();
        Vector sizeFilters = currentConnection.getSizeFilters();
        Vector files = currentConnection.getFiles();
        for (int i = handlersBefore; i < handlers.size(); i++) {
            GuiHandler handler = (GuiHandler) handlers.elementAt(i);
            guiBuilder.addHandler(handler);
        }
        for (int i = filtersBefore; i < filters.size(); i++) {
            GuiFilter filter = (GuiFilter) filters.elementAt(i);
            guiBuilder.addFilter(filter);
        }
        for (int i = replaceFiltersBefore; i < replaceFilters.size(); i++) {
            GuiRegExpFilter filter = (GuiRegExpFilter) replaceFilters.elementAt(i);
            guiBuilder.addReplaceFilter(filter);
        }
        for (int i = sizeFiltersBefore; i < sizeFilters.size(); i++) {
            GuiRegExpFilter filter = (GuiRegExpFilter) sizeFilters.elementAt(i);
            guiBuilder.addSizeFilter(filter);
        }
        for (int i = filesBefore; i < files.size(); i++) {
            GuiFile file = (GuiFile) files.elementAt(i);
            guiBuilder.addFile(file);
        }
        if (showResult) {
            JOptionPane.showMessageDialog(this, "Handlers:  " + (handlers.size() - handlersBefore) + "\nFilters:  " + (filters.size() - filtersBefore) + "\nReplace filters:  " + (replaceFilters.size() - replaceFiltersBefore) + "\nSize filters:  " + (sizeFilters.size() - sizeFiltersBefore) + "\nFiles:  " + (files.size() - filesBefore), "Imported Handlers/Filters", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}

class XMLFileFilter extends javax.swing.filechooser.FileFilter {

    @Override
    public boolean accept(File file) {
        if (file.toString().toLowerCase().endsWith(".xml") || file.isDirectory()) {
            return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "XML Files";
    }
}
