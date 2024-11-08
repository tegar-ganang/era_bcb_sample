package org.aacc.administrationpanel.campaigns;

import org.aacc.administrationpanel.AdministrationPanelView;
import org.aacc.administrationpanel.LoginUser;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author  Fernando
 */
public class CampaignCreate extends javax.swing.JFrame {

    /** Creates new form CampaignCreate */
    public CampaignCreate() {
        initComponents();
        optTypeAgents.setSelected(true);
        txtCampaignName.setText("");
    }

    private void initComponents() {
        grpCampaignType = new javax.swing.ButtonGroup();
        lblCampaignName = new javax.swing.JLabel();
        txtCampaignName = new javax.swing.JTextField();
        btnOk = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        optTypeAgents = new javax.swing.JRadioButton();
        optTypeIVR = new javax.swing.JRadioButton();
        jLabel2 = new javax.swing.JLabel();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.aacc.administrationpanel.AdministrationPanelApp.class).getContext().getResourceMap(CampaignCreate.class);
        setTitle(resourceMap.getString("Form.title"));
        setName("Form");
        lblCampaignName.setText(resourceMap.getString("lblCampaignName.text"));
        lblCampaignName.setName("lblCampaignName");
        txtCampaignName.setText(resourceMap.getString("txtCampaignName.text"));
        txtCampaignName.setName("txtCampaignName");
        btnOk.setText(resourceMap.getString("btnOk.text"));
        btnOk.setName("btnOk");
        btnOk.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOkActionPerformed(evt);
            }
        });
        btnCancel.setText(resourceMap.getString("btnCancel.text"));
        btnCancel.setName("btnCancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });
        grpCampaignType.add(optTypeAgents);
        optTypeAgents.setText(resourceMap.getString("optTypeAgents.text"));
        optTypeAgents.setName("optTypeAgents");
        grpCampaignType.add(optTypeIVR);
        optTypeIVR.setText(resourceMap.getString("optTypeIVR.text"));
        optTypeIVR.setName("optTypeIVR");
        jLabel2.setText(resourceMap.getString("jLabel2.text"));
        jLabel2.setName("jLabel2");
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lblCampaignName).addComponent(btnOk).addComponent(jLabel2)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(optTypeIVR).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(txtCampaignName, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(btnCancel, javax.swing.GroupLayout.Alignment.TRAILING).addComponent(optTypeAgents))).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblCampaignName).addComponent(txtCampaignName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addGap(3, 3, 3).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(optTypeAgents).addComponent(jLabel2)).addGap(2, 2, 2).addComponent(optTypeIVR).addGap(18, 18, 18).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btnCancel).addComponent(btnOk)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        pack();
    }

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {
        dispose();
    }

    /**
 * Creates a new campaign, and sets the user who created it as campaign admin
 * 
 * @param evt
 */
    private void btnOkActionPerformed(java.awt.event.ActionEvent evt) {
        if (validateData()) {
            LoginUser me = AdministrationPanelView.getMe();
            Connection dbConnection = null;
            try {
                DriverManager.registerDriver(new com.mysql.jdbc.Driver());
                dbConnection = DriverManager.getConnection(me.getSqlReportsURL(), me.getSqlReportsUser(), me.getSqlReportsPassword());
                dbConnection.setAutoCommit(false);
                dbConnection.setSavepoint();
                String sql = "INSERT INTO campaigns (type, name, dateCreated, createdBy) VALUES (?, ?, ?, ?)";
                PreparedStatement statement = dbConnection.prepareStatement(sql);
                statement.setByte(1, (optTypeAgents.isSelected()) ? CampaignStatics.CAMP_TYPE_AGENT : CampaignStatics.CAMP_TYPE_IVR);
                statement.setString(2, txtCampaignName.getText());
                statement.setTimestamp(3, new Timestamp(Calendar.getInstance().getTime().getTime()));
                statement.setLong(4, me.getId());
                statement.executeUpdate();
                ResultSet rs = statement.getGeneratedKeys();
                rs.next();
                long campaignId = rs.getLong(1);
                sql = "INSERT INTO usercampaigns (userid, campaignid, role) VALUES (?, ?, ?)";
                statement = dbConnection.prepareStatement(sql);
                statement.setLong(1, me.getId());
                statement.setLong(2, campaignId);
                statement.setString(3, "admin");
                statement.executeUpdate();
                dbConnection.commit();
                dbConnection.close();
                CampaignAdmin ca = new CampaignAdmin();
                ca.setCampaign(txtCampaignName.getText());
                ca.setVisible(true);
                dispose();
            } catch (SQLException ex) {
                try {
                    dbConnection.rollback();
                } catch (SQLException ex1) {
                    Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, null, ex1);
                }
                JOptionPane.showMessageDialog(this.getRootPane(), ex.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Validates data entered by the user
     * @return True if data is valid
     */
    private boolean validateData() {
        if (!CampaignStatics.validCampaignName(txtCampaignName.getText())) {
            JOptionPane.showMessageDialog(this.getRootPane(), "Invalid campaign name. Only numbers and letters, no spaces.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new CampaignCreate().setVisible(true);
            }
        });
    }

    private javax.swing.JButton btnCancel;

    private javax.swing.JButton btnOk;

    private javax.swing.ButtonGroup grpCampaignType;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel lblCampaignName;

    private javax.swing.JRadioButton optTypeAgents;

    private javax.swing.JRadioButton optTypeIVR;

    private javax.swing.JTextField txtCampaignName;
}
