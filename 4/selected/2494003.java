package org.hironico.dbtool2.config;

import java.awt.Dimension;
import java.awt.Window;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.hironico.gui.list.SortedListModel;
import org.hironico.util.DynamicFileLoader;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.error.ErrorInfo;

/**
 * Petite GUI pour sauver son setup de base de données...
 * @author hironico
 * @since 2.2.0
 */
public class ConnectionSetupPanel extends javax.swing.JPanel {

    private static final Logger logger = Logger.getLogger("org.hironico.dbtool2.config");

    /**
     * Action réutilisable pour l'ouverture de connexions vers les SGBD.
     * @since 2.2.0
     */
    private OpenNewConnectionAction openNewConnectionAction = new OpenNewConnectionAction(null);

    /**
     * Flag permettant d'essayer de fermer la fenetre parent lorsque l'on se connecte.
     * C'est utile lorsque le composant est embarqué dans une JDialog dans une autre appli.
     * @since 2.2.0
     */
    private boolean closeDialogWhenConnect = false;

    public static void main(String[] arg) {
        ConsoleAppender consoleAppender = new ConsoleAppender(new PatternLayout());
        Logger.getRootLogger().addAppender(consoleAppender);
        JFrame myFrame = new JFrame("Connection setup tester...");
        myFrame.getContentPane().add(new org.hironico.dbtool2.config.ConnectionSetupPanel());
        myFrame.setSize(1024, 768);
        myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        myFrame.setVisible(true);
    }

    /** Creates new form ConnectionSetupPanel */
    public ConnectionSetupPanel() {
        initComponents();
        btnConnect.setPreferredSize(new Dimension(75, 23));
        showConnectionSetups();
    }

    /**
     * Permet de savoir si on tente de fermer la fenêtre lorsqu'on se connecte.
     * @return true si on ferme la fenêtre lorsqu'on se connecte.
     * @since 2.2.0
     */
    public boolean isCloseDialogWhenConnect() {
        return closeDialogWhenConnect;
    }

    /**
     * Mettre à true pour tenter de fermer la fenêtre lorsqu'on se connecte
     * @param closeDialogWhenConnect doit etre mis à true pour tenter de fermer la fenetre lors de la connexion.
     * @since 2.2.0
     */
    public void setCloseDialogWhenConnect(boolean closeDialogWhenConnect) {
        this.closeDialogWhenConnect = closeDialogWhenConnect;
    }

    /**
     * Permet d'afficher les connexions setup deja present dans le manager.
     * @since 2.2.0
     */
    public void showConnectionSetups() {
        SortedListModel model = (SortedListModel) listSavedConnections.getModel();
        model.clear();
        for (ConnectionSetup setup : ConnectionSetupManager.getInstance().getConnectionSetupList()) {
            model.add(setup);
        }
    }

    /**
     * Permet d'obtenir les properties de connexions à partir des valeurs
     * saisies dans la table prévue à cet effet.
     * @return Properties avec les saisies utilisateur.
     * @since 2.2.0
     */
    protected Properties getConnectionProperties() {
        Properties props = new Properties();
        for (int cpt = 0; cpt < tableProperties.getRowCount(); cpt++) {
            props.setProperty((String) tableProperties.getValueAt(cpt, 0), (String) tableProperties.getValueAt(cpt, 1));
        }
        return props;
    }

    /**
     * Permet d'afficher le connection setup qui se trouve à la ligne spécifiée
     * par le paramètre row, dans la liste des setups enregistrés. Si la ligne
     * est invalide alors on vide les zones de saisies.
     * @param row numéro de ligne dans la liste des setups enregistrés.
     * @since 2.2.0
     */
    protected void showConnectionSetup(int row) {
        DefaultTableModel propertiesModel = (DefaultTableModel) tableProperties.getModel();
        while (propertiesModel.getRowCount() > 0) {
            propertiesModel.removeRow(0);
        }
        lblConnectionName.setText("???");
        txtConnectionTimeout.setText("");
        txtDriverJarFile.setText("");
        txtJdbcUrl.setText("");
        cmbClassName.setSelectedItem("");
        sqlEditorAutoExec.setSqlQuery("");
        sqlEditorProcedureText.setSqlQuery("");
        sqlEditorViewText.setSqlQuery("");
        SortedListModel listModel = (SortedListModel) listSavedConnections.getModel();
        if (row >= 0) {
            ConnectionSetup setup = (ConnectionSetup) listModel.getElementAt(row);
            lblConnectionName.setText(setup.getName());
            txtConnectionTimeout.setText(Integer.toString(setup.getLoginTimeout()));
            txtDriverJarFile.setText(setup.getDriverJarFileName());
            txtJdbcUrl.setText(setup.getUrl());
            cmbClassName.setSelectedItem(setup.getDriverClass());
            sqlEditorAutoExec.setSqlQuery(setup.getAutoExecSQLQuery());
            sqlEditorProcedureText.setSqlQuery(setup.getStoredProcedureTextSQLQuery());
            sqlEditorViewText.setSqlQuery(setup.getViewTextSQLQuery());
            Properties props = setup.getConnectionProperties();
            for (String name : props.stringPropertyNames()) {
                String[] propRow = new String[2];
                propRow[0] = name;
                propRow[1] = props.getProperty(name);
                propertiesModel.addRow(propRow);
            }
        }
    }

    /**
     * Permet de sauvegarder un setup dans celui correspondant au nom affiché
     * dans le label de titre. Si le nom est inconnu alors l'outil demande
     * s'il faut en créé un nouveau.
     * @return true si la sauvegarde est effective et false en cas de problème.
     * @since 2.2.0
     */
    protected boolean applyConnectionSetup() {
        String name = lblConnectionName.getText();
        ConnectionSetup oldConnectionSetup = null;
        SortedListModel<ConnectionSetup> model = (SortedListModel<ConnectionSetup>) listSavedConnections.getModel();
        for (int index = 0; index < model.getSize(); index++) {
            ConnectionSetup mySetup = model.getElementAt(index);
            if (mySetup.getName().equals(name)) {
                oldConnectionSetup = mySetup;
                break;
            }
        }
        if (oldConnectionSetup == null) {
            return true;
        }
        ConnectionSetup setup = getConnectionSetup();
        oldConnectionSetup.setAutoExecSQLQuery(setup.getAutoExecSQLQuery());
        oldConnectionSetup.setConnectionProperties(setup.getConnectionProperties());
        oldConnectionSetup.setDatabase(setup.getDatabase());
        oldConnectionSetup.setDriverClass(setup.getDriverClass());
        oldConnectionSetup.setDriverJarFileName(setup.getDriverJarFileName());
        oldConnectionSetup.setLoginTimeout(setup.getLoginTimeout());
        oldConnectionSetup.setPassword(setup.getPassword());
        oldConnectionSetup.setStoredProcedureTextSQLQuery(setup.getStoredProcedureTextSQLQuery());
        oldConnectionSetup.setUrl(setup.getUrl());
        oldConnectionSetup.setUser(setup.getUser());
        oldConnectionSetup.setViewTextSQLQuery(setup.getViewTextSQLQuery());
        return true;
    }

    /**
     * Permet d'obtenir le ConnectionSetup qui correcpond à la saisie utilisateur.
     * @return ConnectionSetup correctement initialisé.
     * @since 2.2.0
     */
    protected ConnectionSetup getConnectionSetup() {
        ConnectionSetup setup = new ConnectionSetup();
        Properties connectionProperties = getConnectionProperties();
        setup.setConnectionProperties(connectionProperties);
        setup.setAutoExecSQLQuery(sqlEditorAutoExec.getSqlQuery());
        setup.setConnectionProperties(connectionProperties);
        setup.setDriverClass((String) cmbClassName.getSelectedItem());
        setup.setDriverJarFileName(txtDriverJarFile.getText());
        setup.setStoredProcedureTextSQLQuery(sqlEditorProcedureText.getSqlQuery());
        setup.setUrl(txtJdbcUrl.getText());
        setup.setViewTextSQLQuery(sqlEditorViewText.getSqlQuery());
        try {
            setup.setLoginTimeout(Integer.parseInt(txtConnectionTimeout.getText()));
        } catch (NumberFormatException nfe) {
            logger.warn("Cannot format number for login timeout.");
        }
        return setup;
    }

    /**
     * Permet d'obtenir la liste des setup definis dans ce panel.
     * @return Liste de ConnectionSetup.
     * @since 2.2.0
     */
    public List<ConnectionSetup> getConnectionSetupList() {
        List<ConnectionSetup> list = new ArrayList<ConnectionSetup>();
        for (int row = 0; row < listSavedConnections.getElementCount(); row++) {
            list.add((ConnectionSetup) listSavedConnections.getElementAt(row));
        }
        return list;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        popupSavedConections = new javax.swing.JPopupMenu();
        menuSave = new javax.swing.JMenuItem();
        menuRemove = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        menuRename = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        menuDuplicate = new javax.swing.JMenuItem();
        pnlConnectionSetup = new org.jdesktop.swingx.JXPanel();
        lblDriverClass = new javax.swing.JLabel();
        lblDriverJarFile = new javax.swing.JLabel();
        txtDriverJarFile = new javax.swing.JTextField();
        btnBrowseDriverJarFile = new javax.swing.JButton();
        lblJdbcUrl = new javax.swing.JLabel();
        txtJdbcUrl = new javax.swing.JTextField();
        cmbClassName = new javax.swing.JComboBox();
        lblConnectionName = new javax.swing.JLabel();
        bannerTitle = new com.jidesoft.dialog.BannerPanel();
        tabProperties = new javax.swing.JTabbedPane();
        pnlProperties = new org.jdesktop.swingx.JXPanel();
        scrollProperties = new javax.swing.JScrollPane();
        tableProperties = new org.jdesktop.swingx.JXTable();
        pnlCommandsProperties = new org.jdesktop.swingx.JXPanel();
        btnDriverDefaultProperties = new javax.swing.JButton();
        btnAddProperty = new javax.swing.JButton();
        btnRemoveProperty = new javax.swing.JButton();
        sqlEditorAutoExec = new org.hironico.dbtool2.sqleditor.SQLDocumentEditorPanel();
        sqlEditorProcedureText = new org.hironico.dbtool2.sqleditor.SQLDocumentEditorPanel();
        sqlEditorViewText = new org.hironico.dbtool2.sqleditor.SQLDocumentEditorPanel();
        pnlMiscSetup = new org.jdesktop.swingx.JXPanel();
        lblConnectionTimeout = new javax.swing.JLabel();
        txtConnectionTimeout = new javax.swing.JTextField();
        pnlCommands = new org.jdesktop.swingx.JXPanel();
        btnImport = new javax.swing.JButton();
        btnExport = new javax.swing.JButton();
        btnAddConnection = new javax.swing.JButton();
        btnRemoveConnection = new javax.swing.JButton();
        btnSaveConnection = new javax.swing.JButton();
        btnTestConnection = new javax.swing.JButton();
        btnConnect = new javax.swing.JButton();
        lblSavedConnections = new javax.swing.JLabel();
        scrollSavedConnections = new javax.swing.JScrollPane();
        listSavedConnections = new org.jdesktop.swingx.JXList();
        menuSave.setText("Save");
        menuSave.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveConnectionActionPerformed(evt);
            }
        });
        popupSavedConections.add(menuSave);
        menuRemove.setText("Remove");
        menuRemove.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveConnectionActionPerformed(evt);
            }
        });
        popupSavedConections.add(menuRemove);
        popupSavedConections.add(jSeparator1);
        menuRename.setText("Rename");
        menuRename.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuRenameActionPerformed(evt);
            }
        });
        popupSavedConections.add(menuRename);
        popupSavedConections.add(jSeparator2);
        menuDuplicate.setText("Duplicate");
        menuDuplicate.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDuplicateActionPerformed(evt);
            }
        });
        popupSavedConections.add(menuDuplicate);
        setLayout(new java.awt.GridBagLayout());
        pnlConnectionSetup.setLayout(new java.awt.GridBagLayout());
        lblDriverClass.setText("Driver class:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        pnlConnectionSetup.add(lblDriverClass, gridBagConstraints);
        lblDriverJarFile.setText("Driver JAR file:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        pnlConnectionSetup.add(lblDriverJarFile, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        pnlConnectionSetup.add(txtDriverJarFile, gridBagConstraints);
        btnBrowseDriverJarFile.setText("...");
        btnBrowseDriverJarFile.setPreferredSize(new java.awt.Dimension(45, 23));
        btnBrowseDriverJarFile.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseDriverJarFileActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        pnlConnectionSetup.add(btnBrowseDriverJarFile, gridBagConstraints);
        lblJdbcUrl.setText("JDBC URL:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        pnlConnectionSetup.add(lblJdbcUrl, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        pnlConnectionSetup.add(txtJdbcUrl, gridBagConstraints);
        cmbClassName.setEditable(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        pnlConnectionSetup.add(cmbClassName, gridBagConstraints);
        lblConnectionName.setFont(new java.awt.Font("Tahoma", 1, 12));
        lblConnectionName.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblConnectionName.setText("???");
        lblConnectionName.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        pnlConnectionSetup.add(lblConnectionName, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.7;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        add(pnlConnectionSetup, gridBagConstraints);
        bannerTitle.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        bannerTitle.setEndColor(ConnectionSetupPanel.this.getBackground());
        bannerTitle.setMinimumSize(new java.awt.Dimension(40, 100));
        bannerTitle.setStartColor(java.awt.Color.white);
        bannerTitle.setSubtitle("Specify driver class name and provide connection details then click save to keep your settings for the next time you need it.");
        bannerTitle.setTitle("Connection setup");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        add(bannerTitle, gridBagConstraints);
        tabProperties.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        pnlProperties.setLayout(new java.awt.GridBagLayout());
        tableProperties.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {}, new String[] { "Property name", "Value" }) {

            Class[] types = new Class[] { java.lang.String.class, java.lang.String.class };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }
        });
        scrollProperties.setViewportView(tableProperties);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlProperties.add(scrollProperties, gridBagConstraints);
        pnlCommandsProperties.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        btnDriverDefaultProperties.setText("Driver default properties");
        btnDriverDefaultProperties.setPreferredSize(new java.awt.Dimension(155, 23));
        btnDriverDefaultProperties.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDriverDefaultPropertiesActionPerformed(evt);
            }
        });
        pnlCommandsProperties.add(btnDriverDefaultProperties);
        btnAddProperty.setText("Add");
        btnAddProperty.setPreferredSize(new java.awt.Dimension(75, 23));
        btnAddProperty.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddPropertyActionPerformed(evt);
            }
        });
        pnlCommandsProperties.add(btnAddProperty);
        btnRemoveProperty.setText("Remove");
        btnRemoveProperty.setPreferredSize(new java.awt.Dimension(75, 23));
        btnRemoveProperty.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemovePropertyActionPerformed(evt);
            }
        });
        pnlCommandsProperties.add(btnRemoveProperty);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        pnlProperties.add(pnlCommandsProperties, gridBagConstraints);
        tabProperties.addTab("Connection properties", pnlProperties);
        tabProperties.addTab("Auto exec", sqlEditorAutoExec);
        tabProperties.addTab("Procedure's text", sqlEditorProcedureText);
        sqlEditorViewText.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tabProperties.addTab("View's text", sqlEditorViewText);
        pnlMiscSetup.setLayout(new java.awt.GridBagLayout());
        lblConnectionTimeout.setText("Connection timeout (seconds):");
        pnlMiscSetup.add(lblConnectionTimeout, new java.awt.GridBagConstraints());
        txtConnectionTimeout.setText("15");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        pnlMiscSetup.add(txtConnectionTimeout, gridBagConstraints);
        tabProperties.addTab("Miscalenous", pnlMiscSetup);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.7;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 0, 0);
        add(tabProperties, gridBagConstraints);
        pnlCommands.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        pnlCommands.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        btnImport.setText("Import");
        btnImport.setPreferredSize(new java.awt.Dimension(75, 23));
        btnImport.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImportActionPerformed(evt);
            }
        });
        pnlCommands.add(btnImport);
        btnExport.setText("Export");
        btnExport.setPreferredSize(new java.awt.Dimension(75, 23));
        btnExport.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportActionPerformed(evt);
            }
        });
        pnlCommands.add(btnExport);
        btnAddConnection.setText("New");
        btnAddConnection.setPreferredSize(new java.awt.Dimension(75, 23));
        btnAddConnection.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddConnectionActionPerformed(evt);
            }
        });
        pnlCommands.add(btnAddConnection);
        btnRemoveConnection.setText("Remove");
        btnRemoveConnection.setPreferredSize(new java.awt.Dimension(75, 23));
        btnRemoveConnection.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveConnectionActionPerformed(evt);
            }
        });
        pnlCommands.add(btnRemoveConnection);
        btnSaveConnection.setText("Save");
        btnSaveConnection.setPreferredSize(new java.awt.Dimension(75, 23));
        btnSaveConnection.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveConnectionActionPerformed(evt);
            }
        });
        pnlCommands.add(btnSaveConnection);
        btnTestConnection.setText("Test");
        btnTestConnection.setPreferredSize(new java.awt.Dimension(75, 23));
        btnTestConnection.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTestConnectionActionPerformed(evt);
            }
        });
        pnlCommands.add(btnTestConnection);
        btnConnect.setText("Connect");
        btnConnect.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConnectActionPerformed(evt);
            }
        });
        pnlCommands.add(btnConnect);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(pnlCommands, gridBagConstraints);
        lblSavedConnections.setText("Saved connections:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(lblSavedConnections, gridBagConstraints);
        scrollSavedConnections.setPreferredSize(new java.awt.Dimension(270, 132));
        listSavedConnections.setModel(new org.hironico.gui.list.SortedListModel());
        listSavedConnections.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listSavedConnections.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                listSavedConnectionsMouseClicked(evt);
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                listSavedConnectionsMouseReleased(evt);
            }
        });
        listSavedConnections.addListSelectionListener(new javax.swing.event.ListSelectionListener() {

            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                listSavedConnectionsValueChanged(evt);
            }
        });
        scrollSavedConnections.setViewportView(listSavedConnections);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.3;
        add(scrollSavedConnections, gridBagConstraints);
    }

    private void btnSaveConnectionActionPerformed(java.awt.event.ActionEvent evt) {
        if (applyConnectionSetup()) {
            JOptionPane.showMessageDialog(ConnectionSetupPanel.this, "Connection saved !", "Yeah...", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(ConnectionSetupPanel.this, "Did not save the database setup.", "Ohoh...", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void btnTestConnectionActionPerformed(java.awt.event.ActionEvent evt) {
        ConnectionSetup setup = getConnectionSetup();
        if (setup == null) return;
        List<Exception> exceptionList = setup.test();
        if (exceptionList.isEmpty()) {
            JOptionPane.showMessageDialog(ConnectionSetupPanel.this, "It works !", "Yeah...", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JXErrorPane errorPane = new JXErrorPane();
            errorPane.setErrorInfo(new ErrorInfo("Error while testing...", "Error while testing the currently edited setup.", null, "SEVERE", exceptionList.get(0), Level.SEVERE, null));
            JXErrorPane.showDialog(ConnectionSetupPanel.this, errorPane);
        }
    }

    private void btnBrowseDriverJarFileActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File f) {
                return (f.isDirectory() || f.getName().toUpperCase().endsWith(".JAR"));
            }

            @Override
            public String getDescription() {
                return "Java Archive Files (*.jar)";
            }
        });
        int response = chooser.showOpenDialog(ConnectionSetupPanel.this);
        if (response != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File myFile = chooser.getSelectedFile();
        txtDriverJarFile.setText(myFile.getAbsolutePath());
        try {
            DynamicFileLoader.addFile(myFile);
        } catch (Exception ex) {
            logger.error("Cannot load the driver JAR dynamically into the classpath.", ex);
            JXErrorPane errorPane = new JXErrorPane();
            errorPane.setErrorInfo(new ErrorInfo("Cannot load the driver JAR dynamically into the classpath.", "Cannot load the driver JAR dynamically into the classpath.", null, "SEVERE", ex, Level.SEVERE, null));
            JXErrorPane.showDialog(ConnectionSetupPanel.this, errorPane);
            return;
        }
        cmbClassName.removeAllItems();
        try {
            JarFile jarFile = new JarFile(myFile);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if ((name.toUpperCase().indexOf("DRIVER") != -1) && (name.indexOf("$") == -1) && name.endsWith(".class")) {
                    try {
                        name = name.replaceAll("/", "\\.");
                        name = name.substring(0, name.lastIndexOf('.'));
                        logger.debug("Testing driver class: " + name);
                        Object obj = Class.forName(name).newInstance();
                        logger.debug("object class name: " + obj.getClass().getName());
                        if (obj instanceof Driver) {
                            cmbClassName.addItem(name);
                        }
                    } catch (Exception ex) {
                    }
                }
            }
        } catch (IOException ioe) {
            logger.error("Cannot read the content of the JAR file: " + myFile.getName(), ioe);
            JXErrorPane errorPane = new JXErrorPane();
            errorPane.setErrorInfo(new ErrorInfo("Cannot read the content of the JAR file: " + myFile.getName(), "Cannot read the content of the JAR file: " + myFile.getName(), null, "SEVERE", ioe, Level.SEVERE, null));
            JXErrorPane.showDialog(ConnectionSetupPanel.this, errorPane);
            return;
        }
        if (cmbClassName.getItemCount() > 0) {
            cmbClassName.setSelectedIndex(0);
        } else {
            JOptionPane.showMessageDialog(ConnectionSetupPanel.this, "This archive does not contain any Driver.", "Hey!!!", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void btnDriverDefaultPropertiesActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            ConnectionSetup setup = getConnectionSetup();
            Properties props = setup.getDefaultConnectionProperties();
            DefaultTableModel model = (DefaultTableModel) tableProperties.getModel();
            while (model.getRowCount() > 0) {
                model.removeRow(0);
            }
            for (Object key : props.keySet()) {
                String[] row = new String[2];
                row[0] = (String) key;
                row[1] = props.getProperty((String) key);
                model.addRow(row);
            }
        } catch (Exception ex) {
            logger.error("Cannot load default connection properties.", ex);
            JXErrorPane errorPane = new JXErrorPane();
            errorPane.setErrorInfo(new ErrorInfo("Cannot load default driver properties", "Cannot load default driver properties.\nInvalid URL ?", null, "SEVERE", ex, Level.SEVERE, null));
            JXErrorPane.showDialog(ConnectionSetupPanel.this, errorPane);
        }
    }

    private void btnAddPropertyActionPerformed(java.awt.event.ActionEvent evt) {
        DefaultTableModel model = (DefaultTableModel) tableProperties.getModel();
        String[] row = new String[2];
        row[0] = "";
        row[1] = "";
        model.addRow(row);
    }

    private void btnRemovePropertyActionPerformed(java.awt.event.ActionEvent evt) {
        DefaultTableModel model = (DefaultTableModel) tableProperties.getModel();
        int[] selectedRows = tableProperties.getSelectedRows();
        while (selectedRows.length > 0) {
            model.removeRow(selectedRows[0]);
            selectedRows = tableProperties.getSelectedRows();
        }
    }

    private void listSavedConnectionsValueChanged(javax.swing.event.ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }
        JXList src = (JXList) evt.getSource();
        int selectedIndex = src.getSelectedIndex();
        applyConnectionSetup();
        showConnectionSetup(selectedIndex);
    }

    private void btnRemoveConnectionActionPerformed(java.awt.event.ActionEvent evt) {
        ConnectionSetup setup = (ConnectionSetup) listSavedConnections.getSelectedValue();
        if (setup != null) {
            int confirm = JOptionPane.showConfirmDialog(ConnectionSetupPanel.this, "Are your sure you want to delete the setup named:\n" + "'" + setup.getName() + "' from the configuration ?", "Please confirm...", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            SortedListModel model = (SortedListModel) listSavedConnections.getModel();
            model.removeElement(setup);
            ConnectionSetupManager.getInstance().removeConnectionSetup(setup);
            showConnectionSetup(-1);
        }
    }

    private void btnExportActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getAbsolutePath().toUpperCase().endsWith(".XML");
            }

            @Override
            public String getDescription() {
                return "XML files";
            }
        });
        int resp = chooser.showSaveDialog(ConnectionSetupPanel.this);
        if (resp != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File xmlFile = chooser.getSelectedFile();
        if (xmlFile.exists()) {
            resp = JOptionPane.showConfirmDialog(ConnectionSetupPanel.this, "The file '" + xmlFile.getName() + "' already exists.\n" + "Overwrite ?", "Please confirm...", JOptionPane.YES_NO_OPTION);
            if (resp != JOptionPane.YES_OPTION) {
                return;
            }
        }
        try {
            ConnectionSetupManager setupManager = ConnectionSetupManager.getInstance();
            SortedListModel<ConnectionSetup> model = (SortedListModel<ConnectionSetup>) listSavedConnections.getModel();
            for (int cpt = 0; cpt < model.getSize(); cpt++) {
                setupManager.addConnectionSetup(model.getElementAt(cpt));
            }
            String xml = setupManager.toXmlString();
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(xmlFile));
            bos.write(xml.getBytes(), 0, xml.getBytes().length);
            bos.flush();
            bos.close();
            JOptionPane.showMessageDialog(ConnectionSetupPanel.this, "Sucessfully saved connection setups in '" + xmlFile.getName() + "'", "Yeah...", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            logger.error("Cannot export connection setups to xml file.", ex);
            JXErrorPane errorPane = new JXErrorPane();
            errorPane.setErrorInfo(new ErrorInfo("Error while exporting", "Cannot export connection setups to XML file.", null, "ERROR", ex, Level.SEVERE, null));
            JXErrorPane.showDialog(ConnectionSetupPanel.this, errorPane);
        }
    }

    private void btnImportActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getAbsolutePath().toUpperCase().endsWith(".XML");
            }

            @Override
            public String getDescription() {
                return "XML files";
            }
        });
        int resp = chooser.showOpenDialog(ConnectionSetupPanel.this);
        if (resp != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File xmlFile = chooser.getSelectedFile();
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(xmlFile));
            int size = bis.available();
            byte[] buffer = new byte[size];
            bis.read(buffer, 0, size);
            bis.close();
            ConnectionSetupManager.getInstance().parseXmlString(new String(buffer));
            SortedListModel model = (SortedListModel) listSavedConnections.getModel();
            model.clear();
            List<ConnectionSetup> setupList = ConnectionSetupManager.getInstance().getConnectionSetupList();
            for (ConnectionSetup setup : setupList) {
                model.add(setup);
            }
            if (setupList.size() > 0) listSavedConnections.setSelectedIndex(0);
            JOptionPane.showMessageDialog(ConnectionSetupPanel.this, "Sucessfully imported " + model.getSize() + " connection setups.", "Yeah...", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            logger.error("Cannot read the XML file.", ex);
            JXErrorPane errorPane = new JXErrorPane();
            errorPane.setErrorInfo(new ErrorInfo("Cannot read XML file.", "There wee a problem in the config file.", null, "SEVERE", ex, Level.SEVERE, null));
            JXErrorPane.showDialog(ConnectionSetupPanel.this, errorPane);
        }
    }

    private void btnConnectActionPerformed(java.awt.event.ActionEvent evt) {
        applyConnectionSetup();
        String setupName = lblConnectionName.getText();
        SortedListModel<ConnectionSetup> model = (SortedListModel<ConnectionSetup>) listSavedConnections.getModel();
        ConnectionSetup setup = null;
        for (int cpt = 0; cpt < model.getSize(); cpt++) {
            ConnectionSetup mySetup = model.getElementAt(cpt);
            if (mySetup.getName().equals(setupName)) {
                setup = mySetup;
                break;
            }
        }
        if (setup == null) return;
        DbToolConfiguration.getInstance().getConnectionSetupManager().setConnectionSetupList(getConnectionSetupList());
        openNewConnectionAction.setConnectionName(setup.getName());
        openNewConnectionAction.actionPerformed(evt);
        if (closeDialogWhenConnect) {
            Window win = SwingUtilities.getWindowAncestor(ConnectionSetupPanel.this);
            if (win != null) {
                win.dispose();
            }
        }
    }

    private void menuRenameActionPerformed(java.awt.event.ActionEvent evt) {
        ConnectionSetup selectedSetup = (ConnectionSetup) listSavedConnections.getSelectedValue();
        if (selectedSetup == null) return;
        String newName = JOptionPane.showInputDialog("Please enter the new connection setup name:");
        if ((newName == null) || "".equals(newName)) return;
        selectedSetup.setName(newName);
    }

    private void listSavedConnectionsMouseClicked(java.awt.event.MouseEvent evt) {
        if (evt.isPopupTrigger()) popupSavedConections.show(evt.getComponent(), evt.getX(), evt.getY());
    }

    private void listSavedConnectionsMouseReleased(java.awt.event.MouseEvent evt) {
        if (evt.isPopupTrigger()) popupSavedConections.show(evt.getComponent(), evt.getX(), evt.getY());
    }

    private void btnAddConnectionActionPerformed(java.awt.event.ActionEvent evt) {
        String newName = JOptionPane.showInputDialog(ConnectionSetupPanel.this, "Please enter the new connection name:", "New connection...", JOptionPane.QUESTION_MESSAGE);
        if ((newName == null) || "".equals(newName)) {
            return;
        }
        SortedListModel<ConnectionSetup> model = (SortedListModel<ConnectionSetup>) listSavedConnections.getModel();
        Iterator<ConnectionSetup> iter = model.iterator();
        while (iter.hasNext()) {
            ConnectionSetup setup = iter.next();
            if (setup.getName().equals(newName)) {
                JOptionPane.showMessageDialog(ConnectionSetupPanel.this, "There is already a connection with the name:\n'" + newName + "'", "Ohoh...", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        ConnectionSetup newSetup = new ConnectionSetup(newName);
        model.add(newSetup);
        iter = model.iterator();
        int index = 0;
        while (iter.hasNext()) {
            ConnectionSetup setup = iter.next();
            if (setup.getName().equals(newName)) {
                listSavedConnections.getSelectionModel().setSelectionInterval(index, index);
                return;
            }
            index++;
        }
    }

    private void menuDuplicateActionPerformed(java.awt.event.ActionEvent evt) {
        int index = listSavedConnections.getSelectedIndex();
        if (index < 0) return;
        String newName = JOptionPane.showInputDialog("Please enter the new connection name:");
        if ((newName == null) || "".equals(newName)) {
            return;
        }
        SortedListModel<ConnectionSetup> model = (SortedListModel<ConnectionSetup>) listSavedConnections.getModel();
        ConnectionSetup setup = model.getElementAt(index);
        ConnectionSetup newSetup = new ConnectionSetup();
        newSetup.setName(newName);
        newSetup.setAutoExecSQLQuery(setup.getAutoExecSQLQuery());
        newSetup.setConnectionProperties(setup.getConnectionProperties());
        newSetup.setDatabase(setup.getDatabase());
        newSetup.setDriverClass(setup.getDriverClass());
        newSetup.setDriverJarFileName(setup.getDriverJarFileName());
        newSetup.setLoginTimeout(setup.getLoginTimeout());
        newSetup.setPassword(setup.getPassword());
        newSetup.setStoredProcedureTextSQLQuery(setup.getStoredProcedureTextSQLQuery());
        newSetup.setUrl(setup.getUrl());
        newSetup.setUser(setup.getUser());
        newSetup.setViewTextSQLQuery(setup.getViewTextSQLQuery());
        model.add(newSetup);
        for (int cpt = 0; cpt < model.getSize(); cpt++) {
            ConnectionSetup mySetup = model.getElementAt(cpt);
            if (mySetup.getName().equals(newName)) {
                listSavedConnections.setSelectedIndex(cpt);
                showConnectionSetup(cpt);
                return;
            }
        }
    }

    private com.jidesoft.dialog.BannerPanel bannerTitle;

    private javax.swing.JButton btnAddConnection;

    private javax.swing.JButton btnAddProperty;

    private javax.swing.JButton btnBrowseDriverJarFile;

    private javax.swing.JButton btnConnect;

    private javax.swing.JButton btnDriverDefaultProperties;

    private javax.swing.JButton btnExport;

    private javax.swing.JButton btnImport;

    private javax.swing.JButton btnRemoveConnection;

    private javax.swing.JButton btnRemoveProperty;

    private javax.swing.JButton btnSaveConnection;

    private javax.swing.JButton btnTestConnection;

    private javax.swing.JComboBox cmbClassName;

    private javax.swing.JPopupMenu.Separator jSeparator1;

    private javax.swing.JPopupMenu.Separator jSeparator2;

    private javax.swing.JLabel lblConnectionName;

    private javax.swing.JLabel lblConnectionTimeout;

    private javax.swing.JLabel lblDriverClass;

    private javax.swing.JLabel lblDriverJarFile;

    private javax.swing.JLabel lblJdbcUrl;

    private javax.swing.JLabel lblSavedConnections;

    private org.jdesktop.swingx.JXList listSavedConnections;

    private javax.swing.JMenuItem menuDuplicate;

    private javax.swing.JMenuItem menuRemove;

    private javax.swing.JMenuItem menuRename;

    private javax.swing.JMenuItem menuSave;

    private org.jdesktop.swingx.JXPanel pnlCommands;

    private org.jdesktop.swingx.JXPanel pnlCommandsProperties;

    private org.jdesktop.swingx.JXPanel pnlConnectionSetup;

    private org.jdesktop.swingx.JXPanel pnlMiscSetup;

    private org.jdesktop.swingx.JXPanel pnlProperties;

    private javax.swing.JPopupMenu popupSavedConections;

    private javax.swing.JScrollPane scrollProperties;

    private javax.swing.JScrollPane scrollSavedConnections;

    private org.hironico.dbtool2.sqleditor.SQLDocumentEditorPanel sqlEditorAutoExec;

    private org.hironico.dbtool2.sqleditor.SQLDocumentEditorPanel sqlEditorProcedureText;

    private org.hironico.dbtool2.sqleditor.SQLDocumentEditorPanel sqlEditorViewText;

    private javax.swing.JTabbedPane tabProperties;

    private org.jdesktop.swingx.JXTable tableProperties;

    private javax.swing.JTextField txtConnectionTimeout;

    private javax.swing.JTextField txtDriverJarFile;

    private javax.swing.JTextField txtJdbcUrl;
}
