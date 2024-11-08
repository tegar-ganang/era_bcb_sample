package de.excrawler.distributed;

import java.io.*;
import java.util.Properties;
import javax.swing.tree.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import java.nio.channels.FileChannel;

/**
 *
 * @author Yves Hoppe
 */
public class clientServerBox extends javax.swing.JDialog {

    public String[][] servers = null;

    public int currentServer;

    public javax.swing.tree.DefaultTreeModel m_model;

    /** Creates new form clientServerBox */
    public clientServerBox(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

    public class ServerInfo {

        public String ServerName;

        public int ServerId;

        public ServerInfo(String Server, int id) {
            ServerName = Server;
            ServerId = id;
        }

        public String toString() {
            return ServerName;
        }

        public int getServerId() {
            return ServerId;
        }
    }

    public void changeServerName(int id, String newName) {
        try {
            Properties serverFile = new Properties();
            serverFile.load(new FileInputStream("servers" + File.separatorChar + "server_" + id));
            if (ClientConfig.DEBUG == 1) {
                System.out.println("ID: " + id);
                System.out.println("Old Servername: " + serverFile.getProperty("SERVER_NAME"));
            }
            serverFile.setProperty("SERVER_NAME", newName);
            FileOutputStream out = new FileOutputStream("servers" + File.separatorChar + "server_" + id);
            serverFile.store(out, "---Changed ServerName---");
            if (ClientConfig.DEBUG == 1) {
                System.out.println("New Servername: " + serverFile.getProperty("SERVER_NAME"));
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class MyTreeModelListener implements TreeModelListener {

        public void treeNodesChanged(TreeModelEvent e) {
            DefaultMutableTreeNode node;
            node = (DefaultMutableTreeNode) (e.getTreePath().getLastPathComponent());
            try {
                int index = e.getChildIndices()[0];
                node = (DefaultMutableTreeNode) (node.getChildAt(index));
            } catch (NullPointerException exc) {
            }
            if (ClientConfig.DEBUG == 1) {
                System.out.println("The user has finished editing the node.");
                System.out.println("New value: " + node.getUserObject());
            }
            Object nodeInfo = node.getUserObject();
            changeServerName((e.getChildIndices()[0] + 1), node.getUserObject().toString());
            getServer(0);
        }

        public void treeNodesInserted(TreeModelEvent e) {
        }

        public void treeNodesRemoved(TreeModelEvent e) {
        }

        public void treeStructureChanged(TreeModelEvent e) {
        }
    }

    public void deleteServer(int id) {
        if (id != 0) {
            File delfile = new File("servers" + File.separatorChar + "server_" + id);
            if (delfile.exists()) {
                String renameFile = "servers" + File.separatorChar + "server_" + id + ".old";
                delfile.renameTo(new File(renameFile));
                try {
                    Properties delFile = new Properties();
                    delFile.load(new FileInputStream("servers" + File.separatorChar + "server_" + id + ".old"));
                    delFile.setProperty("VISIBLE", "false");
                    FileOutputStream out = new FileOutputStream("servers" + File.separatorChar + "server_" + id + ".old");
                    delFile.store(out, "--DELETED SERVERFILE (SET VISIBLE=FALSE)--");
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            getServer(0);
        }
    }

    public void copyServer(int id) throws Exception {
        File in = new File("servers" + File.separatorChar + "server_" + id);
        File serversDir = new File("servers" + File.separatorChar);
        int newNumber = serversDir.listFiles().length + 1;
        System.out.println("New File Number: " + newNumber);
        File out = new File("servers" + File.separatorChar + "server_" + newNumber);
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
        getServer(newNumber - 1);
    }

    public void switchServer(int id) {
        field_serveradress.setText(servers[id][1]);
        field_serverport.setText(servers[id][2]);
        combo_servertyp.setSelectedIndex(Integer.valueOf(servers[id][3]));
        field_nickname.setText(servers[id][4]);
        field_username.setText(servers[id][5]);
        field_password.setText(servers[id][6]);
        field_comments.setText(servers[id][7]);
        field_loadsitecount.setText(servers[id][10]);
        field_savesitecount.setText(servers[id][11]);
        currentServer = id;
    }

    public void saveServer(int id) {
        String newAddress = field_serveradress.getText();
        String newPort = field_serverport.getText();
        String newType = "0";
        String placeType = combo_servertyp.getSelectedItem().toString();
        String newNick = field_nickname.getText();
        String newUser = field_username.getText();
        String newPass = field_password.getText();
        String newComments = field_comments.getText();
        String newSiteLoad = field_loadsitecount.getText();
        String newSiteSave = field_savesitecount.getText();
        if (!placeType.equalsIgnoreCase("normal")) {
            newType = "1";
        }
        try {
            Properties saveFile = new Properties();
            saveFile.load(new FileInputStream("servers" + File.separatorChar + "server_" + id));
            saveFile.setProperty("SERVER_ADDRESS", newAddress);
            saveFile.setProperty("SERVER_PORT", newPort);
            saveFile.setProperty("SERVER_TYPE", newType);
            saveFile.setProperty("NICKNAME", newNick);
            saveFile.setProperty("USERNAME", newUser);
            saveFile.setProperty("PASSWORD", newPass);
            saveFile.setProperty("COMMENTS", newComments);
            saveFile.setProperty("SITELOAD", newSiteLoad);
            saveFile.setProperty("SITESAVE", newSiteSave);
            FileOutputStream out = new FileOutputStream("servers" + File.separatorChar + "server_" + id);
            saveFile.store(out, "--UPDATED SERVERFILE--");
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getServer(int selected) {
        tree_server.removeAll();
        javax.swing.tree.DefaultMutableTreeNode main = new javax.swing.tree.DefaultMutableTreeNode("Own Servers");
        m_model = new javax.swing.tree.DefaultTreeModel(main);
        File serversDir = new File("servers" + File.separatorChar);
        File[] serverArray = serversDir.listFiles();
        servers = new String[serverArray.length][12];
        int a = 0;
        if (serverArray != null) {
            for (int i = 0; i < serverArray.length; i++) {
                if (serverArray[i].isFile() && serverArray[i] != null) {
                    try {
                        Properties cFile = new Properties();
                        cFile.load(new FileInputStream(serverArray[i].getAbsoluteFile()));
                        servers[a][0] = cFile.getProperty("SERVER_NAME", "Ex-Crawler");
                        servers[a][1] = cFile.getProperty("SERVER_ADDRESS", "");
                        servers[a][2] = cFile.getProperty("SERVER_PORT", "11001");
                        servers[a][3] = cFile.getProperty("SERVER_TYPE", "Normal");
                        servers[a][4] = cFile.getProperty("NICKNAME", "");
                        servers[a][5] = cFile.getProperty("USERNAME", "");
                        servers[a][6] = cFile.getProperty("PASSWORD", "");
                        servers[a][7] = cFile.getProperty("COMMENTS", "");
                        servers[a][8] = cFile.getProperty("DEFAULT", "");
                        servers[a][9] = cFile.getProperty("VISIBLE", "true");
                        servers[a][10] = cFile.getProperty("SITELOAD", "");
                        servers[a][11] = cFile.getProperty("SITESAVE", "");
                        a++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        javax.swing.tree.DefaultMutableTreeNode subCurrent = null;
        if (servers != null) {
            for (int i = 0; i < servers.length; i++) {
                if (selected == i && selected != 0 && selected != -1) {
                    subCurrent = new javax.swing.tree.DefaultMutableTreeNode(new ServerInfo(servers[i][0], i));
                    main.add(subCurrent);
                } else {
                    if (servers[i][9].equalsIgnoreCase("true")) {
                        javax.swing.tree.DefaultMutableTreeNode sub = new javax.swing.tree.DefaultMutableTreeNode(new ServerInfo(servers[i][0], i));
                        main.add(sub);
                    }
                }
                if (servers[i][8].equalsIgnoreCase("true")) {
                    field_serveradress.setText(servers[i][1]);
                    field_serverport.setText(servers[i][2]);
                    combo_servertyp.setSelectedIndex(Integer.valueOf(servers[i][3]));
                    field_nickname.setText(servers[i][4]);
                    field_username.setText(servers[i][5]);
                    field_password.setText(servers[i][6]);
                    field_comments.setText(servers[i][7]);
                    field_loadsitecount.setText(servers[i][10]);
                    field_savesitecount.setText(servers[i][11]);
                }
            }
        }
        tree_server.setEditable(true);
        DefaultTreeModel treeModel = new DefaultTreeModel(main);
        treeModel.addTreeModelListener(new MyTreeModelListener());
        tree_server.setModel(treeModel);
        if (subCurrent != null) {
            TreeNode[] nodes = m_model.getPathToRoot(subCurrent);
            TreePath path = new TreePath(nodes);
            if (ClientConfig.DEBUG == 1) System.out.println("Path: " + path);
            tree_server.scrollPathToVisible(path);
            tree_server.setSelectionPath(path);
            tree_server.startEditingAtPath(path);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tree_server = new javax.swing.JTree();
        btn_newserver = new javax.swing.JButton();
        btn_rename = new javax.swing.JButton();
        btn_delete = new javax.swing.JButton();
        btn_copy = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        field_comments = new javax.swing.JTextArea();
        jLabel6 = new javax.swing.JLabel();
        field_username = new javax.swing.JTextField();
        combo_servertyp = new javax.swing.JComboBox();
        jLabel7 = new javax.swing.JLabel();
        field_serverport = new javax.swing.JTextField();
        field_serveradress = new javax.swing.JTextField();
        field_password = new javax.swing.JTextField();
        field_nickname = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        field_loadsitecount = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        field_savesitecount = new javax.swing.JTextField();
        jSeparator3 = new javax.swing.JSeparator();
        btn_save = new javax.swing.JButton();
        btn_connect = new javax.swing.JButton();
        btn_cancel = new javax.swing.JButton();
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(de.excrawler.distributed.client.class).getContext().getResourceMap(clientServerBox.class);
        setTitle(resourceMap.getString("Form.title"));
        setName("Form");
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel1.border.title")));
        jPanel1.setName("jPanel1");
        jScrollPane1.setName("jScrollPane1");
        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("Own Servers");
        javax.swing.tree.DefaultMutableTreeNode treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Ex-Crawler");
        treeNode1.add(treeNode2);
        tree_server.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        tree_server.setToolTipText(resourceMap.getString("tree_server.toolTipText"));
        tree_server.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        tree_server.setEditable(true);
        tree_server.setName("tree_server");
        tree_server.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {

            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                tree_serverValueChanged(evt);
            }
        });
        tree_server.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                tree_serverPropertyChange(evt);
            }
        });
        jScrollPane1.setViewportView(tree_server);
        btn_newserver.setText(resourceMap.getString("btn_newserver.text"));
        btn_newserver.setName("btn_newserver");
        btn_newserver.setPreferredSize(new java.awt.Dimension(110, 27));
        btn_newserver.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_newserverActionPerformed(evt);
            }
        });
        btn_rename.setText(resourceMap.getString("btn_rename.text"));
        btn_rename.setName("btn_rename");
        btn_rename.setPreferredSize(new java.awt.Dimension(110, 27));
        btn_rename.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_renameActionPerformed(evt);
            }
        });
        btn_delete.setText(resourceMap.getString("btn_delete.text"));
        btn_delete.setName("btn_delete");
        btn_delete.setPreferredSize(new java.awt.Dimension(110, 27));
        btn_delete.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_deleteActionPerformed(evt);
            }
        });
        btn_copy.setText(resourceMap.getString("btn_copy.text"));
        btn_copy.setName("btn_copy");
        btn_copy.setPreferredSize(new java.awt.Dimension(110, 27));
        btn_copy.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_copyActionPerformed(evt);
            }
        });
        jSeparator1.setName("jSeparator1");
        jTabbedPane1.setName("jTabbedPane1");
        jPanel2.setName("jPanel2");
        jLabel1.setText(resourceMap.getString("jLabel1.text"));
        jLabel1.setName("jLabel1");
        jLabel2.setText(resourceMap.getString("jLabel2.text"));
        jLabel2.setName("jLabel2");
        jLabel3.setText(resourceMap.getString("jLabel3.text"));
        jLabel3.setName("jLabel3");
        jLabel4.setText(resourceMap.getString("jLabel4.text"));
        jLabel4.setName("jLabel4");
        jSeparator2.setName("jSeparator2");
        jLabel5.setText(resourceMap.getString("jLabel5.text"));
        jLabel5.setName("jLabel5");
        jScrollPane2.setName("jScrollPane2");
        field_comments.setColumns(20);
        field_comments.setRows(5);
        field_comments.setName("field_comments");
        jScrollPane2.setViewportView(field_comments);
        jLabel6.setText(resourceMap.getString("jLabel6.text"));
        jLabel6.setName("jLabel6");
        field_username.setText(resourceMap.getString("field_username.text"));
        field_username.setName("field_username");
        field_username.setPreferredSize(new java.awt.Dimension(130, 25));
        combo_servertyp.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Normal", "Encrypted" }));
        combo_servertyp.setName("combo_servertyp");
        jLabel7.setText(resourceMap.getString("jLabel7.text"));
        jLabel7.setName("jLabel7");
        field_serverport.setText(resourceMap.getString("field_serverport.text"));
        field_serverport.setName("field_serverport");
        field_serveradress.setName("field_serveradress");
        field_serveradress.setPreferredSize(new java.awt.Dimension(130, 25));
        field_password.setName("field_password");
        field_password.setPreferredSize(new java.awt.Dimension(130, 25));
        field_nickname.setName("field_nickname");
        field_nickname.setPreferredSize(new java.awt.Dimension(130, 25));
        jLabel8.setText(resourceMap.getString("jLabel8.text"));
        jLabel8.setName("jLabel8");
        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE).addGroup(jPanel2Layout.createSequentialGroup().addContainerGap().addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel5).addGroup(jPanel2Layout.createSequentialGroup().addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel3).addComponent(jLabel4)).addGap(32, 32, 32).addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(field_password, javax.swing.GroupLayout.DEFAULT_SIZE, 294, Short.MAX_VALUE).addComponent(field_username, javax.swing.GroupLayout.DEFAULT_SIZE, 294, Short.MAX_VALUE)))).addContainerGap()).addGroup(jPanel2Layout.createSequentialGroup().addContainerGap().addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 394, Short.MAX_VALUE).addContainerGap()).addGroup(jPanel2Layout.createSequentialGroup().addContainerGap().addComponent(jLabel6).addContainerGap(336, Short.MAX_VALUE)).addGroup(jPanel2Layout.createSequentialGroup().addContainerGap().addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel1).addComponent(jLabel2).addComponent(jLabel8)).addGap(29, 29, 29).addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel2Layout.createSequentialGroup().addComponent(field_serveradress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(18, 18, 18).addComponent(jLabel7).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 51, Short.MAX_VALUE).addComponent(field_serverport, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)).addComponent(combo_servertyp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(field_nickname, javax.swing.GroupLayout.DEFAULT_SIZE, 293, Short.MAX_VALUE)).addContainerGap()));
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel2Layout.createSequentialGroup().addGap(22, 22, 22).addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel1).addComponent(field_serveradress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(field_serverport, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel7)).addGap(7, 7, 7).addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel2).addComponent(combo_servertyp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addGap(12, 12, 12).addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel8).addComponent(field_nickname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabel5).addGap(11, 11, 11).addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel3).addComponent(field_username, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addGap(7, 7, 7).addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(field_password, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel4)).addGap(11, 11, 11).addComponent(jLabel6).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(22, Short.MAX_VALUE)));
        jTabbedPane1.addTab(resourceMap.getString("jPanel2.TabConstraints.tabTitle"), jPanel2);
        jPanel3.setName("jPanel3");
        jLabel9.setText(resourceMap.getString("jLabel9.text"));
        jLabel9.setName("jLabel9");
        jLabel10.setText(resourceMap.getString("jLabel10.text"));
        jLabel10.setName("jLabel10");
        field_loadsitecount.setText(resourceMap.getString("field_loadsitecount.text"));
        field_loadsitecount.setName("field_loadsitecount");
        field_loadsitecount.setPreferredSize(new java.awt.Dimension(100, 25));
        jLabel11.setText(resourceMap.getString("jLabel11.text"));
        jLabel11.setName("jLabel11");
        field_savesitecount.setName("field_savesitecount");
        field_savesitecount.setPreferredSize(new java.awt.Dimension(100, 25));
        jSeparator3.setName("jSeparator3");
        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addContainerGap().addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel9).addGroup(jPanel3Layout.createSequentialGroup().addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel10).addComponent(jLabel11)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 85, Short.MAX_VALUE).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(field_loadsitecount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(field_savesitecount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))).addContainerGap(82, Short.MAX_VALUE)).addGroup(jPanel3Layout.createSequentialGroup().addComponent(jSeparator3, javax.swing.GroupLayout.DEFAULT_SIZE, 394, Short.MAX_VALUE).addContainerGap()))));
        jPanel3Layout.setVerticalGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addContainerGap().addComponent(jLabel9).addGap(18, 18, 18).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel10).addComponent(field_loadsitecount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel11).addComponent(field_savesitecount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(217, Short.MAX_VALUE)));
        jTabbedPane1.addTab(resourceMap.getString("jPanel3.TabConstraints.tabTitle"), jPanel3);
        btn_save.setText(resourceMap.getString("btn_save.text"));
        btn_save.setName("btn_save");
        btn_save.setPreferredSize(new java.awt.Dimension(100, 27));
        btn_save.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_saveActionPerformed(evt);
            }
        });
        btn_connect.setText(resourceMap.getString("btn_connect.text"));
        btn_connect.setName("btn_connect");
        btn_connect.setPreferredSize(new java.awt.Dimension(100, 27));
        btn_connect.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_connectActionPerformed(evt);
            }
        });
        btn_cancel.setText(resourceMap.getString("btn_cancel.text"));
        btn_cancel.setName("btn_cancel");
        btn_cancel.setPreferredSize(new java.awt.Dimension(100, 27));
        btn_cancel.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_cancelActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup().addComponent(btn_newserver, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btn_rename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))).addGroup(jPanel1Layout.createSequentialGroup().addComponent(btn_delete, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btn_copy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))).addGap(18, 18, 18).addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE).addContainerGap()).addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 698, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup().addContainerGap(257, Short.MAX_VALUE).addComponent(btn_connect, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(18, 18, 18).addComponent(btn_save, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(18, 18, 18).addComponent(btn_cancel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(105, 105, 105)));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btn_newserver, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(btn_rename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btn_delete, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(btn_copy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))).addComponent(jTabbedPane1)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btn_connect, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(btn_save, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(btn_cancel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap()));
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE));
        getAccessibleContext().setAccessibleName(resourceMap.getString("Form.AccessibleContext.accessibleName"));
        getAccessibleContext().setAccessibleDescription(resourceMap.getString("Form.AccessibleContext.accessibleDescription"));
        pack();
    }

    private void btn_newserverActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            File serversDir = new File("servers/");
            int number = serversDir.listFiles().length + 1;
            File newServer = new File("servers" + File.separatorChar + "server_" + number);
            if (newServer.createNewFile()) {
                String text = "# Server Template for Ex-Crawler Distributed Client\n" + "# Copyright (C) 2010 Yves Hoppe\n" + "# http://www.ex-crawler.de - ex-crawler.sourceforge.net\n" + "\n" + "SERVER_NAME=New Server(" + (serversDir.listFiles().length + 1) + ")\n" + "SERVER_ADDRESS=\n" + "SERVER_PORT\n" + "\n" + "# 0 = normal, 1 = crypted\n" + "\n" + "SERVER_TYPE=0\n" + "\n" + "NICKNAME=\n" + "USERNAME=\n" + "PASSWORD=\n" + "\n" + "COMMENTS=\n" + "\n" + "DEFAULT=false\n" + "VISIBLE=true\n" + "\n" + "# Advanced Settings\n" + "\n" + "SITELOAD=200\n" + "SITESAVE=200";
                BufferedWriter writeServer = new BufferedWriter(new FileWriter(newServer));
                writeServer.write(text);
                writeServer.close();
                getServer(number - 1);
            } else {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void btn_renameActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.tree.DefaultMutableTreeNode node = (javax.swing.tree.DefaultMutableTreeNode) tree_server.getLastSelectedPathComponent();
        if (node == null) return;
        if (node.isLeaf()) {
            TreeNode[] nodes = m_model.getPathToRoot(node);
            TreePath path = new TreePath(nodes);
            if (ClientConfig.DEBUG == 1) System.out.println("Path: " + path);
            tree_server.scrollPathToVisible(path);
            tree_server.setSelectionPath(path);
            tree_server.startEditingAtPath(path);
        }
    }

    private void btn_deleteActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.tree.DefaultMutableTreeNode node = (javax.swing.tree.DefaultMutableTreeNode) tree_server.getLastSelectedPathComponent();
        if (node == null) return;
        Object nodeInfo = node.getUserObject();
        if (node.isLeaf()) {
            ServerInfo Server = (ServerInfo) nodeInfo;
            deleteServer(Server.ServerId + 1);
        }
    }

    private void btn_copyActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.tree.DefaultMutableTreeNode node = (javax.swing.tree.DefaultMutableTreeNode) tree_server.getLastSelectedPathComponent();
        if (node == null) return;
        Object nodeInfo = node.getUserObject();
        if (node.isLeaf()) {
            ServerInfo Server = (ServerInfo) nodeInfo;
            try {
                copyServer(Server.ServerId + 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void formWindowOpened(java.awt.event.WindowEvent evt) {
        getServer(0);
    }

    private void tree_serverValueChanged(javax.swing.event.TreeSelectionEvent evt) {
        javax.swing.tree.DefaultMutableTreeNode node = (javax.swing.tree.DefaultMutableTreeNode) tree_server.getLastSelectedPathComponent();
        if (node == null) return;
        Object nodeInfo = node.getUserObject();
        if (node.isLeaf()) {
            ServerInfo Server = (ServerInfo) nodeInfo;
            switchServer(Server.ServerId);
        }
    }

    private void tree_serverPropertyChange(java.beans.PropertyChangeEvent evt) {
    }

    private void btn_cancelActionPerformed(java.awt.event.ActionEvent evt) {
        clientServerBox.this.setVisible(false);
    }

    private void btn_saveActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.tree.DefaultMutableTreeNode node = (javax.swing.tree.DefaultMutableTreeNode) tree_server.getLastSelectedPathComponent();
        if (node == null) return;
        Object nodeInfo = node.getUserObject();
        if (node.isLeaf()) {
            ServerInfo Server = (ServerInfo) nodeInfo;
            saveServer(Server.ServerId + 1);
        }
        clientServerBox.this.setVisible(false);
    }

    private void btn_connectActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.tree.DefaultMutableTreeNode node = (javax.swing.tree.DefaultMutableTreeNode) tree_server.getLastSelectedPathComponent();
        if (node == null) return;
        Object nodeInfo = node.getUserObject();
        if (node.isLeaf()) {
            ServerInfo Server = (ServerInfo) nodeInfo;
            saveServer(Server.ServerId + 1);
        }
        clientServerBox.this.setVisible(false);
    }

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                clientServerBox dialog = new clientServerBox(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {

                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    private javax.swing.JButton btn_cancel;

    private javax.swing.JButton btn_connect;

    private javax.swing.JButton btn_copy;

    private javax.swing.JButton btn_delete;

    private javax.swing.JButton btn_newserver;

    private javax.swing.JButton btn_rename;

    private javax.swing.JButton btn_save;

    private javax.swing.JComboBox combo_servertyp;

    private javax.swing.JTextArea field_comments;

    private javax.swing.JTextField field_loadsitecount;

    private javax.swing.JTextField field_nickname;

    private javax.swing.JTextField field_password;

    private javax.swing.JTextField field_savesitecount;

    private javax.swing.JTextField field_serveradress;

    private javax.swing.JTextField field_serverport;

    private javax.swing.JTextField field_username;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel10;

    private javax.swing.JLabel jLabel11;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JLabel jLabel7;

    private javax.swing.JLabel jLabel8;

    private javax.swing.JLabel jLabel9;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JSeparator jSeparator1;

    private javax.swing.JSeparator jSeparator2;

    private javax.swing.JSeparator jSeparator3;

    private javax.swing.JTabbedPane jTabbedPane1;

    public javax.swing.JTree tree_server;
}
