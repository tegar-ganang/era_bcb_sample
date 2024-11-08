package DCL;

import DCLPlugin.*;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.net.SocketPermission;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.*;

/**
 *
 * @author  root
 */
public class DCL_GUI extends javax.swing.JFrame {

    private JehutyAPI api;

    private VLApi vl_api;

    private Experiment exp;

    private List<DCLFrame> plugins;

    private boolean connected;

    private boolean windowed;

    private JPopupMenu leftMenu;

    private JPopupMenu input_plugins;

    private JPopupMenu output_plugins;

    private JFrame myself;

    private String selected_model, selected_block, selected_param;

    private JFileChooser fc;

    private javax.swing.JButton jButtonOfflineExp;

    class ClassEvent implements java.awt.event.ActionListener {

        private String value;

        public ClassEvent(String v) {
            value = v;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            menuItemActionPerformed(actionEvent, value);
        }
    }

    class Refresher extends Thread {

        int ms;

        JFrame ziz;

        public Refresher(JFrame obj, int ms) {
            this.ms = ms;
            this.ziz = obj;
        }

        public void run() {
            while (true) {
                this.ziz.repaint();
                try {
                    Thread.sleep(this.ms);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private String generateXML() {
        System.out.println("GenerateXML");
        String xml = "<xml>";
        if (api == null) xml += exp.toXML();
        if (!(plugins == null)) {
            for (int i = 0; i < plugins.size(); i++) if (plugins.get(i).isEnabled()) xml += plugins.get(i).generateXML();
        }
        xml += "</xml>";
        System.out.println(xml);
        return xml;
    }

    private void cleanPopupMenu() {
        input_plugins = null;
        output_plugins = null;
    }

    private void fillPopupMenu() {
        DCLPluginParser parser;
        if (api == null) {
            parser = new DCLPluginParser("<xml>" + "<plugin type=\"output\" class=\"DCLPlugin.DCLPiper\"></plugin>" + "<plugin type=\"output\" class=\"DCLPlugin.DCLDisplay\"></plugin>" + "<plugin type=\"output\" class=\"DCLPlugin.DCLGauger\"></plugin>" + "<plugin type=\"input\" class=\"DCLPlugin.DCLKnob\"></plugin>" + "<plugin type=\"input\" class=\"DCLPlugin.DCLSlider\"></plugin>" + "<plugin type=\"output\" class=\"DCLPlugin.DCLFileWriter\"></plugin>" + "<plugin type=\"output\" class=\"DCLPlugin.DCLSpyker\"></plugin>" + "<plugin type=\"output\" class=\"DCLPlugin.DCLChart\"></plugin>" + "<plugin type=\"input\" class=\"DCLPlugin.DCLWheel\"></plugin>" + "<plugin type=\"input\" class=\"DCLPlugin.DCLSwitch\"></plugin>" + "</xml>");
        } else {
            System.out.println("Fetch from network");
            parser = new DCLPluginParser(api.getPlugins());
        }
        List<String> input = parser.getList("input");
        List<String> output = parser.getList("output");
        input_plugins = new JPopupMenu("Input Plugins");
        for (int i = 0; i < input.size(); i++) {
            String classname = input.get(i);
            JMenuItem inputMenuItem = new JMenuItem(input.get(i));
            inputMenuItem.addActionListener(new ClassEvent(input.get(i)));
            input_plugins.add(inputMenuItem);
        }
        output_plugins = new JPopupMenu("Output Plugins");
        for (int i = 0; i < output.size(); i++) {
            String classname = output.get(i);
            JMenuItem outputMenuItem = new JMenuItem(output.get(i));
            outputMenuItem.addActionListener(new ClassEvent(output.get(i)));
            output_plugins.add(outputMenuItem);
        }
    }

    private void connect() {
        System.out.println("Connect");
        if (!connected) {
            jTextFieldIP.setEditable(false);
            jTextFieldPort.setEditable(false);
            jTextFieldProcess.setEditable(false);
            updateParams(false);
            updateGUI();
            connected = true;
            try {
                vl_api = new VLApi(exp.getIP(), exp.getPort(), exp.getProcess());
            } catch (Exception ex) {
                new DCLXMessage("Error", ex.getMessage());
                disconnect();
                return;
            }
            vl_api.startSimulation();
            enableModelExplorer();
            fillPopupMenu();
            if (api == null) {
                System.out.println("Debug connection");
                loadPlugins();
                jButtonOfflineExp.setVisible(false);
            } else {
                loadPlugins();
            }
        }
    }

    private void disconnect() {
        System.out.println("Disconnect");
        if (connected) {
            jTextFieldIP.setEditable(true);
            jTextFieldPort.setEditable(true);
            jTextFieldProcess.setEditable(true);
            connected = false;
            unloadPlugins();
            disableModelExplorer();
            cleanPopupMenu();
            if (vl_api != null) vl_api.stopSimulation();
            if (api == null) jButtonOfflineExp.setVisible(true);
        }
    }

    private void loadPlugins() {
        System.out.println("Load Plugins");
        if (connected) {
            if (api == null) {
                System.out.println(exp.getXML());
                plugins = new DCLComponentParser(vl_api, exp.getXML()).getList();
                refreshControlPane();
            } else {
                plugins = new DCLComponentParser(vl_api, api.loadVirtualExp()).getList();
                refreshControlPane();
            }
        }
    }

    private void unloadPlugins() {
        System.out.println("Unload Plugins");
        if (!(plugins == null)) for (int i = 0; i < plugins.size(); i++) if (plugins.get(i).isEnabled() && plugins.get(i).isRunning()) plugins.get(i).dispose();
        plugins = null;
    }

    public void dispose() {
        disconnect();
        System.out.println("Quit");
        super.dispose();
    }

    private void refreshControlPane() {
        System.out.println("Refresh Control Panel");
        if (!(plugins == null)) for (int i = 0; i < plugins.size(); i++) {
            if (plugins.get(i).isEnabled() && !plugins.get(i).isRunning()) {
                jLayeredPaneControl.add(plugins.get(i), javax.swing.JLayeredPane.DEFAULT_LAYER);
                new Thread(plugins.get(i)).start();
            }
        }
    }

    private void disableModelExplorer() {
        System.out.println("Disable Model Explorer");
        jInternalFrameME.setVisible(false);
    }

    private void enableModelExplorer() {
        System.out.println("Enable Model Explorer");
        jTree = new JTree(vl_api.getModelTree());
        jTree.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTreeMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(jTree);
        jInternalFrameME.setVisible(true);
    }

    private void addPlugin(String model, String block, String param, String classname) {
        System.out.println("Add Plugin");
        if (connected) {
            List<DCLFrame> toAdd = new DCLComponentParser(vl_api, "<xml><plugin " + "model=\"" + model + "\" " + "block=\"" + block + "\" " + "param=\"" + param + "\" " + "class=\"" + classname + "\"> </plugin></xml>").getList();
            if (plugins == null) plugins = toAdd; else plugins.addAll(toAdd);
            refreshControlPane();
        }
    }

    private void updateParams(boolean saving) {
        if (jTextFieldTitle.getText().length() > 0) exp.setTitle(jTextFieldTitle.getText());
        if (jTextFieldIP.getText().length() > 0) exp.setIP(jTextFieldIP.getText());
        if (jTextFieldPort.getText().length() > 0) exp.setPort(jTextFieldPort.getText());
        if (jTextFieldProcess.getText().length() > 0) exp.setProcess(jTextFieldProcess.getText());
        if (jTextAreaNote.getText().length() >= 0) exp.setNote(jTextAreaNote.getText());
        if (saving) exp.setXML(generateXML());
    }

    private void updateGUI() {
        jTextFieldTitle.setText(exp.getTitle());
        setTitle("DCL: " + exp.getTitle());
        jTextFieldIP.setText(exp.getIP());
        jTextFieldPort.setText(exp.getPort());
        jTextFieldProcess.setText(exp.getProcess());
        jTextAreaNote.setText(exp.getNote());
    }

    private void setStatus(String m) {
        jTextFieldStatus.setText("Status: " + m);
    }

    private void menuItemActionPerformed(java.awt.event.ActionEvent evt, String name) {
        this.addPlugin(selected_model, selected_block, selected_param, name);
        System.out.println(selected_model + "." + selected_block + "." + selected_param + "->" + name);
    }

    private void createPopupMenu(String block) {
        leftMenu = new JPopupMenu("Menu");
        if (block.equals("SCOPES")) jTree.setComponentPopupMenu(output_plugins); else jTree.setComponentPopupMenu(input_plugins);
    }

    private void OfflineExp() {
        String XMLin = null;
        BufferedReader fileIn = null;
        fc = new JFileChooser();
        int ris = fc.showOpenDialog(myself);
        if (ris == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                fileIn = new BufferedReader(new FileReader(file));
            } catch (Exception ex) {
                new DCLXMessage("Error", ex.getMessage());
                return;
            }
            try {
                XMLin = fileIn.readLine();
                DCLExpParser o = new DCLExpParser(XMLin);
                exp = o.getList().get(0);
                updateGUI();
                exp.setXML(XMLin);
            } catch (Exception ex) {
                new DCLXMessage("Error", ex.getMessage());
            }
            try {
                fileIn.close();
            } catch (Exception ex) {
                new DCLXMessage("Error", ex.getMessage());
            }
        }
    }

    public DCL_GUI(Experiment e) {
        api = null;
        plugins = null;
        connected = false;
        windowed = true;
        exp = e;
        initComponents();
        GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Point Oxy = genv.getCenterPoint();
        Rectangle pos = new Rectangle(Oxy.x - getWidth() / 2, Oxy.y - getHeight() / 2, getWidth(), getHeight());
        setBounds(pos);
        updateGUI();
        disableModelExplorer();
        createOpenButton();
        setVisible(true);
    }

    /** Creates new form DCLExperimentList */
    public DCL_GUI(Experiment e, JehutyAPI a) {
        api = a;
        exp = e;
        plugins = null;
        connected = false;
        windowed = true;
        System.out.println("DCLGUI with JehutyAPI enabled.");
        initComponents();
        GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Point Oxy = genv.getCenterPoint();
        Rectangle pos = new Rectangle(Oxy.x - getWidth() / 2, Oxy.y - getHeight() / 2, getWidth(), getHeight());
        setBounds(pos);
        updateGUI();
        disableModelExplorer();
        setVisible(true);
    }

    private void initComponents() {
        jTextFieldStatus = new javax.swing.JTextField();
        jTabbedPane = new javax.swing.JTabbedPane();
        jLayeredPaneControl = new javax.swing.JLayeredPane();
        jInternalFrameME = new javax.swing.JInternalFrame();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTree = new javax.swing.JTree();
        jLayeredPaneMain = new javax.swing.JLayeredPane();
        jTextFieldTitle = new javax.swing.JTextField();
        jLabelTitle = new javax.swing.JLabel();
        jLabelIP = new javax.swing.JLabel();
        jTextFieldIP = new javax.swing.JTextField();
        jLabelPort = new javax.swing.JLabel();
        jTextFieldPort = new javax.swing.JTextField();
        jLabelProcess = new javax.swing.JLabel();
        jTextFieldProcess = new javax.swing.JTextField();
        jLabelNote = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaNote = new javax.swing.JTextArea();
        jButtonDetectIP = new javax.swing.JButton();
        jToolBar = new javax.swing.JToolBar();
        jButtonConnect = new javax.swing.JButton();
        jButtonDisconnect = new javax.swing.JButton();
        jButtonSave = new javax.swing.JButton();
        jButtonDelete = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jButtonWindow = new javax.swing.JButton();
        jMenuBar = new javax.swing.JMenuBar();
        Experiment = new javax.swing.JMenu();
        Save = new javax.swing.JMenuItem();
        jSeparatorSD = new javax.swing.JSeparator();
        Delete = new javax.swing.JMenuItem();
        jSeparatorDQ = new javax.swing.JSeparator();
        Quit = new javax.swing.JMenuItem();
        Network = new javax.swing.JMenu();
        Connect = new javax.swing.JMenuItem();
        jSeparatorCD = new javax.swing.JSeparator();
        Disconnect = new javax.swing.JMenuItem();
        Info = new javax.swing.JMenu();
        Credits = new javax.swing.JMenuItem();
        jSeparatorCV = new javax.swing.JSeparator();
        AboutDCL = new javax.swing.JMenuItem();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Distributed Control Lab");
        jTextFieldStatus.setEditable(false);
        jTextFieldStatus.setText("Status");
        jTabbedPane.setBackground(new java.awt.Color(238, 238, 238));
        jLayeredPaneControl.setBackground(new java.awt.Color(238, 238, 238));
        jInternalFrameME.setIconifiable(true);
        jInternalFrameME.setMaximizable(true);
        jInternalFrameME.setResizable(true);
        jInternalFrameME.setTitle("Model Explorer");
        jInternalFrameME.setVisible(true);
        jTree.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTreeMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(jTree);
        org.jdesktop.layout.GroupLayout jInternalFrameMELayout = new org.jdesktop.layout.GroupLayout(jInternalFrameME.getContentPane());
        jInternalFrameME.getContentPane().setLayout(jInternalFrameMELayout);
        jInternalFrameMELayout.setHorizontalGroup(jInternalFrameMELayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE));
        jInternalFrameMELayout.setVerticalGroup(jInternalFrameMELayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 278, Short.MAX_VALUE));
        jInternalFrameME.setBounds(20, 30, 170, 310);
        jLayeredPaneControl.add(jInternalFrameME, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jTabbedPane.addTab("Control Panel", jLayeredPaneControl);
        jTextFieldTitle.setText("Titolo");
        jTextFieldTitle.setBounds(50, 30, 850, 19);
        jLayeredPaneMain.add(jTextFieldTitle, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLabelTitle.setText("Title:");
        jLabelTitle.setBounds(0, 30, 31, 15);
        jLayeredPaneMain.add(jLabelTitle, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLabelIP.setText("IP:");
        jLabelIP.setBounds(0, 50, 14, 15);
        jLayeredPaneMain.add(jLabelIP, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jTextFieldIP.setText("192.168.1.55");
        jTextFieldIP.setBounds(50, 50, 120, 19);
        jLayeredPaneMain.add(jTextFieldIP, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLabelPort.setText("Port:");
        jLabelPort.setBounds(0, 70, 28, 15);
        jLayeredPaneMain.add(jLabelPort, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jTextFieldPort.setText("8090");
        jTextFieldPort.setBounds(50, 70, 60, 19);
        jLayeredPaneMain.add(jTextFieldPort, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLabelProcess.setText("Process:");
        jLabelProcess.setBounds(660, 50, 60, 15);
        jLayeredPaneMain.add(jLabelProcess, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jTextFieldProcess.setText("cosamp");
        jTextFieldProcess.setBounds(720, 50, 180, 19);
        jLayeredPaneMain.add(jTextFieldProcess, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLabelNote.setText("Note:");
        jLabelNote.setBounds(0, 120, 32, 15);
        jLayeredPaneMain.add(jLabelNote, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jTextAreaNote.setColumns(20);
        jTextAreaNote.setRows(5);
        jScrollPane1.setViewportView(jTextAreaNote);
        jScrollPane1.setBounds(0, 140, 910, 330);
        jLayeredPaneMain.add(jScrollPane1, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jButtonDetectIP.setText("Autodetect IP");
        jButtonDetectIP.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButtonDetectIPMouseClicked(evt);
            }
        });
        jButtonDetectIP.setBounds(50, 90, 120, 25);
        jLayeredPaneMain.add(jButtonDetectIP, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jTabbedPane.addTab("Process", jLayeredPaneMain);
        jToolBar.setFloatable(false);
        jToolBar.setPreferredSize(new java.awt.Dimension(100, 40));
        jButtonConnect.setBackground(new java.awt.Color(255, 255, 255));
        jButtonConnect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/connect.png")));
        jButtonConnect.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonConnectActionPerformed(evt);
            }
        });
        jToolBar.add(jButtonConnect);
        jButtonDisconnect.setBackground(new java.awt.Color(255, 255, 255));
        jButtonDisconnect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/disconnect.png")));
        jButtonDisconnect.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDisconnectActionPerformed(evt);
            }
        });
        jToolBar.add(jButtonDisconnect);
        jButtonSave.setBackground(new java.awt.Color(255, 255, 255));
        jButtonSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/save.png")));
        jButtonSave.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveActionPerformed(evt);
            }
        });
        jToolBar.add(jButtonSave);
        jButtonDelete.setBackground(new java.awt.Color(255, 255, 255));
        jButtonDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/delete.png")));
        jButtonDelete.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteActionPerformed(evt);
            }
        });
        jToolBar.add(jButtonDelete);
        jToolBar.add(jSeparator1);
        jButtonWindow.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/window.png")));
        jButtonWindow.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonWindowActionPerformed(evt);
            }
        });
        jToolBar.add(jButtonWindow);
        Experiment.setText("Experiment");
        Save.setText("Save");
        Save.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveActionPerformed(evt);
            }
        });
        Experiment.add(Save);
        Experiment.add(jSeparatorSD);
        Delete.setText("Delete");
        Delete.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteActionPerformed(evt);
            }
        });
        Experiment.add(Delete);
        Experiment.add(jSeparatorDQ);
        Quit.setText("Quit");
        Quit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                QuitActionPerformed(evt);
            }
        });
        Experiment.add(Quit);
        jMenuBar.add(Experiment);
        Network.setText("Network");
        Connect.setText("Connect");
        Connect.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ConnectActionPerformed(evt);
            }
        });
        Network.add(Connect);
        Network.add(jSeparatorCD);
        Disconnect.setText("Disconnect");
        Disconnect.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DisconnectActionPerformed(evt);
            }
        });
        Network.add(Disconnect);
        jMenuBar.add(Network);
        Info.setText("?");
        Credits.setText("Credits");
        Credits.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CreditsActionPerformed(evt);
            }
        });
        Info.add(Credits);
        Info.add(jSeparatorCV);
        AboutDCL.setText("About DCL");
        AboutDCL.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AboutDCLActionPerformed(evt);
            }
        });
        Info.add(AboutDCL);
        jMenuBar.add(Info);
        setJMenuBar(jMenuBar);
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jTextFieldStatus, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 910, Short.MAX_VALUE).add(jToolBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 910, Short.MAX_VALUE).add(jTabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 910, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup().add(jToolBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jTabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 496, Short.MAX_VALUE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jTextFieldStatus, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)));
        pack();
    }

    private void jButtonWindowActionPerformed(java.awt.event.ActionEvent evt) {
        boolean changing = false;
        if (plugins != null && plugins.size() > 0) {
            for (int i = 0; i < plugins.size(); i++) {
                if (plugins.get(i).isEnabled() && plugins.get(i).isRunning()) {
                    changing = true;
                    if (windowed) plugins.get(i).disableWindow(); else plugins.get(i).enableWindow();
                }
            }
        }
        windowed = (changing ? !windowed : windowed);
    }

    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {
        delete();
    }

    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {
        save();
    }

    private void jButtonDisconnectActionPerformed(java.awt.event.ActionEvent evt) {
        disconnect();
    }

    private void jButtonConnectActionPerformed(java.awt.event.ActionEvent evt) {
        connect();
    }

    private void DisconnectActionPerformed(java.awt.event.ActionEvent evt) {
        disconnect();
    }

    private void ConnectActionPerformed(java.awt.event.ActionEvent evt) {
        connect();
    }

    private void jTreeMouseClicked(java.awt.event.MouseEvent evt) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) jTree.getLastSelectedPathComponent();
        if (node != null && node.isLeaf()) {
            String path = jTree.getSelectionPath().toString();
            System.out.println("AAAAAAAAAAAAAAAAAAAAA " + path);
            path = path.substring(1, path.length() - 1);
            String[] token = path.split(", ");
            for (int i = 0; i < token.length; i++) System.out.println(token[i]);
            selected_model = token[0];
            selected_block = token[1];
            for (int i = 2; i < token.length - 1; i++) selected_block += "/" + token[i];
            selected_param = token[token.length - 1];
            System.out.println(selected_model + "->" + selected_block + " -> " + selected_param);
            createPopupMenu(selected_block);
        } else jTree.setComponentPopupMenu(null);
    }

    private void jButtonDetectIPMouseClicked(java.awt.event.MouseEvent evt) {
        if (api == null) {
            System.out.println("Autodetect Action");
        } else {
            exp.setIP(api.getRemoteAddr());
            jTextFieldIP.setText(exp.getIP());
            setStatus("IP acquired");
        }
    }

    private void CreditsActionPerformed(java.awt.event.ActionEvent evt) {
        new DCLXMessage("Credits DCL", "Insert here text");
    }

    private void QuitActionPerformed(java.awt.event.ActionEvent evt) {
        dispose();
    }

    private void DeleteActionPerformed(java.awt.event.ActionEvent evt) {
        delete();
    }

    private void delete() {
        if (api == null) {
            System.out.println("Delete Action");
        } else {
            api.deleteVirtualExp();
            dispose();
        }
    }

    private void save() {
        updateParams(true);
        updateGUI();
        if (api == null) {
            fc = new JFileChooser();
            int choice = fc.showSaveDialog(myself);
            if (choice == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                if (file.exists()) {
                    String msg = "File \"" + file.getName() + "\" already exists: overwrite file?";
                    int ret = JOptionPane.showConfirmDialog(myself, msg, "save file", JOptionPane.YES_NO_OPTION);
                    if (ret != JOptionPane.YES_OPTION) {
                        JOptionPane.showMessageDialog(myself, "Experiment not saved!");
                        return;
                    }
                }
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    PrintStream ps = new PrintStream(fos);
                    ps.println(generateXML());
                    JOptionPane.showMessageDialog(myself, "Experiment succesfully saved!");
                    fos.close();
                } catch (Exception ex) {
                    new DCLXMessage("Saving Error", ex.getMessage());
                    return;
                }
            } else {
                JOptionPane.showMessageDialog(myself, "Experiment not saved!");
            }
        } else {
            api.updateVirtualExp(exp);
            setStatus("Experiment saved");
        }
    }

    private void createOpenButton() {
        jButtonOfflineExp = new javax.swing.JButton();
        jButtonOfflineExp.setText("Open offline experiment");
        jButtonOfflineExp.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                OfflineExp();
            }
        });
        jButtonOfflineExp.setBounds(190, 90, 190, 25);
        jLayeredPaneMain.add(jButtonOfflineExp, javax.swing.JLayeredPane.DEFAULT_LAYER);
    }

    private void SaveActionPerformed(java.awt.event.ActionEvent evt) {
        save();
    }

    private void AboutDCLActionPerformed(java.awt.event.ActionEvent evt) {
        new DCLXMessage("About DCL", "Insert here text");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                if (args.length > 0) new DCL_GUI(new Experiment(args[0])); else new DCL_GUI(new Experiment());
            }
        });
    }

    private javax.swing.JMenuItem AboutDCL;

    private javax.swing.JMenuItem Connect;

    private javax.swing.JMenuItem Credits;

    private javax.swing.JMenuItem Delete;

    private javax.swing.JMenuItem Disconnect;

    private javax.swing.JMenu Experiment;

    private javax.swing.JMenu Info;

    private javax.swing.JMenu Network;

    private javax.swing.JMenuItem Quit;

    private javax.swing.JMenuItem Save;

    private javax.swing.JButton jButtonConnect;

    private javax.swing.JButton jButtonDelete;

    private javax.swing.JButton jButtonDetectIP;

    private javax.swing.JButton jButtonDisconnect;

    private javax.swing.JButton jButtonSave;

    private javax.swing.JButton jButtonWindow;

    private javax.swing.JInternalFrame jInternalFrameME;

    private javax.swing.JLabel jLabelIP;

    private javax.swing.JLabel jLabelNote;

    private javax.swing.JLabel jLabelPort;

    private javax.swing.JLabel jLabelProcess;

    private javax.swing.JLabel jLabelTitle;

    private javax.swing.JLayeredPane jLayeredPaneControl;

    private javax.swing.JLayeredPane jLayeredPaneMain;

    private javax.swing.JMenuBar jMenuBar;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JSeparator jSeparator1;

    private javax.swing.JSeparator jSeparatorCD;

    private javax.swing.JSeparator jSeparatorCV;

    private javax.swing.JSeparator jSeparatorDQ;

    private javax.swing.JSeparator jSeparatorSD;

    private javax.swing.JTabbedPane jTabbedPane;

    private javax.swing.JTextArea jTextAreaNote;

    private javax.swing.JTextField jTextFieldIP;

    private javax.swing.JTextField jTextFieldPort;

    private javax.swing.JTextField jTextFieldProcess;

    private javax.swing.JTextField jTextFieldStatus;

    private javax.swing.JTextField jTextFieldTitle;

    private javax.swing.JToolBar jToolBar;

    private javax.swing.JTree jTree;
}
