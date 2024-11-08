package net.sourceforge.homekeeper.core;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.log4j.Logger;

/**
 *
 * @author  Lefteris
 */
public class GroupsManager extends javax.swing.JInternalFrame {

    private static final long serialVersionUID = 1L;

    private Logger log;

    /** Creates new form GroupsManager */
    public GroupsManager() {
        log = Logger.getLogger("net.sourceforge.homekeeper.core");
        initComponents();
        Utils.setMaxSize(txtGrpDesc, 200);
        Utils.setMaxSize(txtGrpName, 20);
        registerTableChangeCallback();
    }

    private void initComponents() {
        jLabel1 = new javax.swing.JLabel();
        btnAdd = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblGroups = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        txtGrpDesc = new javax.swing.JTextPane();
        btnOk = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        txtGrpName = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        setClosable(true);
        setIconifiable(true);
        setResizable(true);
        setTitle("Groups Manager");
        addComponentListener(new java.awt.event.ComponentAdapter() {

            public void componentShown(java.awt.event.ComponentEvent evt) {
                formComponentShown(evt);
            }
        });
        jLabel1.setText("Existing User Groups");
        btnAdd.setText("Add Group");
        btnAdd.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });
        btnDelete.setText("Delete Group");
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });
        tblGroups.setAutoCreateRowSorter(true);
        ResultSet rs = getGroups();
        tblGroups.setModel(new GroupsTableModel(rs));
        Database.close(rs);
        jScrollPane1.setViewportView(tblGroups);
        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jScrollPane3.setBorder(null);
        txtGrpDesc.setBorder(txtGrpName.getBorder());
        txtGrpDesc.setEnabled(false);
        jScrollPane3.setViewportView(txtGrpDesc);
        btnOk.setText("OK");
        btnOk.setEnabled(false);
        btnOk.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOkActionPerformed(evt);
            }
        });
        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });
        txtGrpName.setEnabled(false);
        txtGrpName.addCaretListener(new javax.swing.event.CaretListener() {

            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                txtGrpNameCaretUpdate(evt);
            }
        });
        jLabel2.setText("Group Name");
        jLabel3.setText("Group Description");
        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().addContainerGap().add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jLabel2).add(jLabel3)).add(29, 29, 29).add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(txtGrpName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 162, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 506, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))).add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup().add(512, 512, 512).add(btnOk, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 47, Short.MAX_VALUE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(btnCancel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))).addContainerGap()));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().addContainerGap().add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().add(txtGrpName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 57, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).add(jPanel1Layout.createSequentialGroup().add(jLabel2).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jLabel3))).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(btnCancel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(btnOk, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addContainerGap()));
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup().add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 543, Short.MAX_VALUE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(btnDelete, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(btnAdd, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 95, Short.MAX_VALUE))).add(jLabel1)).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(jLabel1).add(6, 6, 6).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().add(btnAdd, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(btnDelete, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE)).add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 68, Short.MAX_VALUE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addContainerGap()));
        pack();
    }

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            String sql = "DELETE FROM CORE_USER_GROUPS WHERE ID IN (";
            int[] selRows = tblGroups.getSelectedRows();
            for (int i = 0; i < selRows.length; ++i) {
                log.debug("Selected Row: " + selRows[i]);
                sql = sql + tblGroups.getModel().getValueAt(tblGroups.convertRowIndexToView(selRows[i]), 0);
                if (i < selRows.length - 1) {
                    sql = sql + ",";
                }
            }
            sql = sql + ")";
            log.debug("SQL statement for delete: \n" + sql);
            PreparedStatement pStmt = Database.getMyConnection().prepareStatement(sql);
            pStmt.executeUpdate();
            Database.getMyConnection().commit();
            Database.close(pStmt);
            MessageBox.ok("Your request has completed successfully", this);
            ResultSet rs = getGroups();
            tblGroups.setModel(new GroupsTableModel(rs));
            Database.close(rs);
        } catch (SQLException e) {
            log.error("The delete of the user group was not performed correctly: \n" + e.getMessage());
            MessageBox.ok("A problem has prevented your request from completing!", this);
        } finally {
            txtGrpName.setEnabled(false);
            txtGrpDesc.setEnabled(false);
            btnOk.setEnabled(false);
            btnCancel.requestFocus();
        }
    }

    private void txtGrpNameCaretUpdate(javax.swing.event.CaretEvent evt) {
        if ((txtGrpName.getText().length() > 0) && (txtGrpDesc.getText().length() > 0)) {
            btnOk.setEnabled(true);
        } else {
            btnOk.setEnabled(false);
        }
    }

    private void btnOkActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            int id = 0;
            String sql = "SELECT MAX(ID) as MAX_ID from CORE_USER_GROUPS";
            PreparedStatement pStmt = Database.getMyConnection().prepareStatement(sql);
            ResultSet rs = pStmt.executeQuery();
            if (rs.next()) {
                id = rs.getInt("MAX_ID") + 1;
            } else {
                id = 1;
            }
            Database.close(pStmt);
            sql = "INSERT INTO CORE_USER_GROUPS" + " (ID, GRP_NAME, GRP_DESC, DATE_INITIAL, DATE_FINAL, IND_STATUS)" + " VALUES (?, ?, ?, ?, ?, ?)";
            pStmt = Database.getMyConnection().prepareStatement(sql);
            pStmt.setInt(1, id);
            pStmt.setString(2, txtGrpName.getText());
            pStmt.setString(3, txtGrpDesc.getText());
            pStmt.setDate(4, Utils.getTodaySql());
            pStmt.setDate(5, Date.valueOf("9999-12-31"));
            pStmt.setString(6, "A");
            pStmt.executeUpdate();
            Database.getMyConnection().commit();
            Database.close(pStmt);
            MessageBox.ok("New group added successfully", this);
            rs = getGroups();
            tblGroups.setModel(new GroupsTableModel(rs));
            Database.close(rs);
        } catch (SQLException e) {
            log.error("Failed with update operation \n" + e.getMessage());
            MessageBox.ok("Failed to create the new group in the database", this);
            try {
                Database.getMyConnection().rollback();
            } catch (Exception inner) {
            }
            ;
        } catch (IllegalArgumentException e) {
            log.error("Illegal argument for the DATE_FINAL \n" + e.getMessage());
            MessageBox.ok("Failed to create the new group in the database", this);
            try {
                Database.getMyConnection().rollback();
            } catch (Exception inner) {
            }
            ;
        } finally {
            txtGrpName.setEnabled(false);
            txtGrpDesc.setEnabled(false);
            btnOk.setEnabled(false);
            btnCancel.requestFocus();
        }
    }

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {
        txtGrpName.setEnabled(true);
        txtGrpDesc.setEnabled(true);
        txtGrpName.requestFocusInWindow();
    }

    private void formComponentShown(java.awt.event.ComponentEvent evt) {
    }

    private javax.swing.JButton btnAdd;

    private javax.swing.JButton btnCancel;

    private javax.swing.JButton btnDelete;

    private javax.swing.JButton btnOk;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane3;

    private javax.swing.JTable tblGroups;

    private javax.swing.JTextPane txtGrpDesc;

    private javax.swing.JTextField txtGrpName;

    private ResultSet getGroups() {
        ResultSet rs = null;
        try {
            String sql = "SELECT * from CORE_USER_GROUPS";
            PreparedStatement pStmt = Database.getMyConnection().prepareStatement(sql);
            rs = pStmt.executeQuery();
        } catch (Exception e) {
            MessageBox.ok("Failed to select from User Groups: \n" + e.getMessage(), this);
        }
        return rs;
    }

    private void registerTableChangeCallback() {
        tblGroups.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                handleSelectionChange(e);
            }
        });
    }

    private void handleSelectionChange(ListSelectionEvent e) {
        if (tblGroups.getSelectedRowCount() == 1) {
            btnDelete.setEnabled(true);
        } else if (tblGroups.getSelectedRowCount() > 1) {
            btnDelete.setEnabled(true);
        } else {
            btnDelete.setEnabled(false);
        }
    }
}
