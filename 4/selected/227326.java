package uk.ekiwi.mq;

import javax.swing.table.TableModel;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;

/**
 *
 * @author  Bryce
 */
public class FrmPreferences extends javax.swing.JInternalFrame implements FileAccess {

    private Preferences preferences = null;

    static final String columnNames[] = { "QueueManager", "Host", "Port", "Channel" };

    /** Creates new form FrmPreferences */
    public FrmPreferences(Preferences preferences) {
        this.preferences = preferences;
        initComponents();
        refresh();
    }

    public void refresh() {
        txtDirectory.setText(preferences.getDefaultDir());
        txtMaxMsgRetrieveCount.setText(Integer.toString(preferences.getMaxMsgRetrieveCount()));
        loadTable();
    }

    private void loadTable() {
        ArrayList<QueueManager> queueManagers = null;
        queueManagers = preferences.getQueueManagers();
        int size = queueManagers.size();
        TableModel model = new DefaultTableModel(columnNames, size);
        for (int i = 0; i < size; i++) {
            QueueManager qm = queueManagers.get(i);
            model.setValueAt(qm.getQManager(), i, 0);
            model.setValueAt(qm.getHostName(), i, 1);
            model.setValueAt(qm.getPort(), i, 2);
            model.setValueAt(qm.getChannel(), i, 3);
        }
        tblQMs.setModel(model);
        tblQMs.repaint();
    }

    private boolean isQMValid() {
        if (this.txtQM.getText().length() > 0 && this.txtHost.getText().length() > 0 && this.txtPort.getText().length() > 0 && this.txtChannel.getText().length() > 0) {
            return true;
        }
        return false;
    }

    private void initComponents() {
        btnClose = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        pnlQMs = new javax.swing.JPanel();
        pnlQMTbl = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblQMs = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        txtQM = new javax.swing.JTextField();
        txtHost = new javax.swing.JTextField();
        txtPort = new javax.swing.JTextField();
        txtChannel = new javax.swing.JTextField();
        btnAdd = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        pnlMisc = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtDirectory = new javax.swing.JTextField();
        btnBrowse = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        txtMaxMsgRetrieveCount = new javax.swing.JTextField();
        btnSave = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        lblStatus = new javax.swing.JLabel();
        setClosable(true);
        setResizable(true);
        setTitle("Preferences");
        btnClose.setText("Close");
        btnClose.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });
        pnlQMTbl.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        tblQMs.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { { null, null, null, null }, { null, null, null, null }, { null, null, null, null }, { null, null, null, null } }, new String[] { "Title 1", "Title 2", "Title 3", "Title 4" }));
        tblQMs.addInputMethodListener(new java.awt.event.InputMethodListener() {

            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
                tblQMsInputMethodTextChanged(evt);
            }

            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {
            }
        });
        tblQMs.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                tblQMsKeyPressed(evt);
            }
        });
        tblQMs.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblQMsMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tblQMs);
        org.jdesktop.layout.GroupLayout pnlQMTblLayout = new org.jdesktop.layout.GroupLayout(pnlQMTbl);
        pnlQMTbl.setLayout(pnlQMTblLayout);
        pnlQMTblLayout.setHorizontalGroup(pnlQMTblLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 426, Short.MAX_VALUE));
        pnlQMTblLayout.setVerticalGroup(pnlQMTblLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 146, Short.MAX_VALUE));
        jLabel2.setText("Queue Manager:");
        jLabel3.setText("Host");
        jLabel4.setText("Port:");
        jLabel5.setText("Channel:");
        txtQM.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtQMKeyReleased(evt);
            }
        });
        txtHost.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtHostKeyReleased(evt);
            }
        });
        txtPort.setInputVerifier(new IsIntegerValidator(this, txtPort, "Field must be numeric."));
        txtPort.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtPortKeyReleased(evt);
            }
        });
        txtChannel.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtChannelKeyReleased(evt);
            }
        });
        btnAdd.setText("Add");
        btnAdd.setEnabled(false);
        btnAdd.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });
        btnDelete.setText("Delete");
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });
        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().addContainerGap().add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jLabel2).add(jLabel3).add(jLabel4).add(jLabel5)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false).add(txtHost).add(txtQM, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE).add(txtChannel).add(txtPort, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 54, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 96, Short.MAX_VALUE).add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false).add(btnAdd, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(btnDelete, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().addContainerGap().add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel2).add(txtQM, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(btnAdd)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel3).add(txtHost, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(btnDelete)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel4).add(txtPort, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel5).add(txtChannel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addContainerGap(19, Short.MAX_VALUE)));
        org.jdesktop.layout.GroupLayout pnlQMsLayout = new org.jdesktop.layout.GroupLayout(pnlQMs);
        pnlQMs.setLayout(pnlQMsLayout);
        pnlQMsLayout.setHorizontalGroup(pnlQMsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, pnlQMsLayout.createSequentialGroup().addContainerGap().add(pnlQMsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING).add(org.jdesktop.layout.GroupLayout.LEADING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.LEADING, pnlQMTbl, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addContainerGap()));
        pnlQMsLayout.setVerticalGroup(pnlQMsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(pnlQMsLayout.createSequentialGroup().addContainerGap().add(pnlQMTbl, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jTabbedPane1.addTab("Queue Managers", pnlQMs);
        pnlMisc.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLabel1.setText("Default Directory:");
        btnBrowse.setText("Browse");
        btnBrowse.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseActionPerformed(evt);
            }
        });
        jLabel6.setText("Max Msg Retrieve Count:");
        txtMaxMsgRetrieveCount.setText("0");
        txtMaxMsgRetrieveCount.setInputVerifier(new IsIntegerValidator(this, txtMaxMsgRetrieveCount, "Field must be numeric."));
        org.jdesktop.layout.GroupLayout pnlMiscLayout = new org.jdesktop.layout.GroupLayout(pnlMisc);
        pnlMisc.setLayout(pnlMiscLayout);
        pnlMiscLayout.setHorizontalGroup(pnlMiscLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(pnlMiscLayout.createSequentialGroup().addContainerGap().add(pnlMiscLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(pnlMiscLayout.createSequentialGroup().add(jLabel6).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(txtMaxMsgRetrieveCount, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 85, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).add(pnlMiscLayout.createSequentialGroup().add(jLabel1).add(56, 56, 56).add(txtDirectory, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(btnBrowse))).addContainerGap()));
        pnlMiscLayout.setVerticalGroup(pnlMiscLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(pnlMiscLayout.createSequentialGroup().addContainerGap().add(pnlMiscLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel1).add(btnBrowse).add(txtDirectory, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(pnlMiscLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(txtMaxMsgRetrieveCount, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addContainerGap(232, Short.MAX_VALUE)));
        jTabbedPane1.addTab("Misc", pnlMisc);
        btnSave.setText("Save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        jPanel2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel2Layout.createSequentialGroup().add(18, 18, 18).add(lblStatus, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 178, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addContainerGap(283, Short.MAX_VALUE)));
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, lblStatus, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 22, Short.MAX_VALUE));
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().add(jTabbedPane1).addContainerGap()).add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup().add(btnSave).add(20, 20, 20).add(btnClose).add(177, 177, 177)))).add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(jTabbedPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 330, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(btnClose).add(btnSave)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 33, Short.MAX_VALUE).add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)));
        pack();
    }

    private void txtHostKeyReleased(java.awt.event.KeyEvent evt) {
        if (isQMValid()) {
            btnAdd.setEnabled(true);
        } else btnAdd.setEnabled(false);
    }

    private void txtChannelKeyReleased(java.awt.event.KeyEvent evt) {
        if (isQMValid()) {
            btnAdd.setEnabled(true);
        } else btnAdd.setEnabled(false);
    }

    private void txtPortKeyReleased(java.awt.event.KeyEvent evt) {
        if (isQMValid()) {
            btnAdd.setEnabled(true);
        } else btnAdd.setEnabled(false);
    }

    private void txtQMKeyReleased(java.awt.event.KeyEvent evt) {
        if (isQMValid()) {
            btnAdd.setEnabled(true);
        } else btnAdd.setEnabled(false);
    }

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {
        DefaultTableModel model = (DefaultTableModel) tblQMs.getModel();
        String[] rowData = { txtQM.getText(), txtHost.getText(), txtPort.getText(), txtChannel.getText() };
        model.addRow(rowData);
        tblQMs.setModel(model);
    }

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {
        DefaultTableModel model = (DefaultTableModel) tblQMs.getModel();
        model.removeRow(tblQMs.getSelectedRow());
        tblQMs.setModel(model);
        clearTextFields();
    }

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            ArrayList<QueueManager> qms = new ArrayList<QueueManager>();
            for (int i = 0; i < tblQMs.getRowCount(); i++) {
                QueueManager qm = new QueueManager();
                qm.setQManager(tblQMs.getValueAt(i, 0).toString());
                qm.setHostName(tblQMs.getValueAt(i, 1).toString());
                qm.setPort(tblQMs.getValueAt(i, 2).toString());
                qm.setChannel(tblQMs.getValueAt(i, 3).toString());
                qms.add(qm);
            }
            preferences.setQueueManagers(qms);
            preferences.setDefaultDir(this.txtDirectory.getText());
            preferences.setMaxMsgRetrieveCount(Integer.parseInt(this.txtMaxMsgRetrieveCount.getText()));
            preferences.persist();
            lblStatus.setText("Saved");
        } catch (Exception e) {
            FrmError f = new FrmError(e);
            this.getParent().add(f);
            f.setVisible(true);
        }
    }

    private void tblQMsKeyPressed(java.awt.event.KeyEvent evt) {
        if (tblQMs.getSelectedRow() >= 0) populateTextFields();
    }

    private void clearTextFields() {
        txtQM.setText("");
        txtHost.setText("");
        txtPort.setText("");
        txtChannel.setText("");
    }

    private void populateTextFields() {
        txtQM.setText(tblQMs.getValueAt(tblQMs.getSelectedRow(), 0).toString());
        txtHost.setText(tblQMs.getValueAt(tblQMs.getSelectedRow(), 1).toString());
        txtPort.setText(tblQMs.getValueAt(tblQMs.getSelectedRow(), 2).toString());
        txtChannel.setText(tblQMs.getValueAt(tblQMs.getSelectedRow(), 3).toString());
    }

    private void tblQMsInputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
    }

    private void tblQMsMouseClicked(java.awt.event.MouseEvent evt) {
        btnDelete.setEnabled(true);
        if (tblQMs.getSelectedRow() >= 0) populateTextFields();
    }

    private void btnBrowseActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            FrmFileAccess f = new FrmFileAccess(this);
            f.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
            this.getParent().add(f);
            f.setVisible(true);
        } catch (Exception e) {
            FrmError f = new FrmError(e);
            this.getParent().add(f);
            f.setVisible(true);
        }
    }

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    private void save() {
    }

    public void setSelectedFile(String dir) {
        txtDirectory.setText(dir);
    }

    private javax.swing.JButton btnAdd;

    private javax.swing.JButton btnBrowse;

    private javax.swing.JButton btnClose;

    private javax.swing.JButton btnDelete;

    private javax.swing.JButton btnSave;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JTabbedPane jTabbedPane1;

    private javax.swing.JLabel lblStatus;

    private javax.swing.JPanel pnlMisc;

    private javax.swing.JPanel pnlQMTbl;

    private javax.swing.JPanel pnlQMs;

    private javax.swing.JTable tblQMs;

    private javax.swing.JTextField txtChannel;

    private javax.swing.JTextField txtDirectory;

    private javax.swing.JTextField txtHost;

    private javax.swing.JTextField txtMaxMsgRetrieveCount;

    private javax.swing.JTextField txtPort;

    private javax.swing.JTextField txtQM;
}
