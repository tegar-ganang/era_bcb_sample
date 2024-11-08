package com.ehsunbehravesh.mypasswords.gui;

import com.ehsunbehravesh.mypasswords.Domain;
import com.ehsunbehravesh.mypasswords.Logger;
import com.ehsunbehravesh.mypasswords.Utils;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class SettingsDialog extends JDialog {

    private JFrame parent;

    private JFileChooser saveDialog;

    public SettingsDialog(JFrame parent, boolean modal) {
        super(parent, modal);
        this.parent = parent;
        initComponents();
        createComponents();
        myInitComponents();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        pnlMain = new javax.swing.JPanel();
        jtpSettings = new javax.swing.JTabbedPane();
        pnlTab1 = new javax.swing.JPanel();
        chkLogFailedLogin = new javax.swing.JCheckBox();
        pnlTab2 = new javax.swing.JPanel();
        lblDescriptionOfRecovery = new javax.swing.JLabel();
        btnCreatePasswordRecovery = new javax.swing.JButton();
        chkTrust = new javax.swing.JCheckBox();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Settings");
        setResizable(false);
        pnlMain.setBackground(new java.awt.Color(255, 255, 255));
        jtpSettings.setBackground(new java.awt.Color(255, 255, 255));
        pnlTab1.setBackground(new java.awt.Color(255, 255, 255));
        chkLogFailedLogin.setBackground(new java.awt.Color(255, 255, 255));
        chkLogFailedLogin.setText("Log failed logins");
        chkLogFailedLogin.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkLogFailedLoginActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout pnlTab1Layout = new javax.swing.GroupLayout(pnlTab1);
        pnlTab1.setLayout(pnlTab1Layout);
        pnlTab1Layout.setHorizontalGroup(pnlTab1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlTab1Layout.createSequentialGroup().addContainerGap().addComponent(chkLogFailedLogin).addContainerGap(380, Short.MAX_VALUE)));
        pnlTab1Layout.setVerticalGroup(pnlTab1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlTab1Layout.createSequentialGroup().addContainerGap().addComponent(chkLogFailedLogin).addContainerGap(94, Short.MAX_VALUE)));
        jtpSettings.addTab("Log", pnlTab1);
        pnlTab2.setBackground(new java.awt.Color(255, 255, 255));
        lblDescriptionOfRecovery.setText("<html>By creating a <b>Password Recovery File</b>, you can recover your current password. <b>Internet Connection</b> is needed for this function.</html>");
        btnCreatePasswordRecovery.setFont(new java.awt.Font("Tahoma", 1, 11));
        btnCreatePasswordRecovery.setText("Create Password Recovery File");
        btnCreatePasswordRecovery.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreatePasswordRecoveryActionPerformed(evt);
            }
        });
        chkTrust.setText("I trust MyPasswords Server");
        javax.swing.GroupLayout pnlTab2Layout = new javax.swing.GroupLayout(pnlTab2);
        pnlTab2.setLayout(pnlTab2Layout);
        pnlTab2Layout.setHorizontalGroup(pnlTab2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlTab2Layout.createSequentialGroup().addContainerGap().addGroup(pnlTab2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(lblDescriptionOfRecovery, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 489, Short.MAX_VALUE).addGroup(pnlTab2Layout.createSequentialGroup().addComponent(chkTrust).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 89, Short.MAX_VALUE).addComponent(btnCreatePasswordRecovery))).addContainerGap()));
        pnlTab2Layout.setVerticalGroup(pnlTab2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlTab2Layout.createSequentialGroup().addContainerGap().addComponent(lblDescriptionOfRecovery, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 33, Short.MAX_VALUE).addGroup(pnlTab2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(btnCreatePasswordRecovery, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(chkTrust)).addContainerGap()));
        lblDescriptionOfRecovery.getAccessibleContext().setAccessibleName("<html>By creating a <b>Password Recovery File</b>, you can recover your current password. <b>Internet Connection</b> is needed</html>");
        jtpSettings.addTab("Password Recovery", pnlTab2);
        javax.swing.GroupLayout pnlMainLayout = new javax.swing.GroupLayout(pnlMain);
        pnlMain.setLayout(pnlMainLayout);
        pnlMainLayout.setHorizontalGroup(pnlMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jtpSettings, javax.swing.GroupLayout.Alignment.TRAILING));
        pnlMainLayout.setVerticalGroup(pnlMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jtpSettings, javax.swing.GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE));
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(pnlMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(pnlMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        pack();
    }

    private void chkLogFailedLoginActionPerformed(java.awt.event.ActionEvent evt) {
        chkLogFailedLoginActionPerform(evt);
    }

    private void btnCreatePasswordRecoveryActionPerformed(java.awt.event.ActionEvent evt) {
        btnCreatePasswordRecoveryActionPerform(evt);
    }

    private javax.swing.JButton btnCreatePasswordRecovery;

    private javax.swing.JCheckBox chkLogFailedLogin;

    private javax.swing.JCheckBox chkTrust;

    private javax.swing.JTabbedPane jtpSettings;

    private javax.swing.JLabel lblDescriptionOfRecovery;

    private javax.swing.JPanel pnlMain;

    private javax.swing.JPanel pnlTab1;

    private javax.swing.JPanel pnlTab2;

    private void createComponents() {
        saveDialog = new JFileChooser();
    }

    private void myInitComponents() {
        setComponentOrientation(parent.getComponentOrientation());
        setFont(parent.getFont());
        Utils.setCenterOfParent(parent, this);
        Domain domain = Domain.getInstance();
        String value = domain.getSetting("log_failed_logins", null);
        if (value != null) {
            int intValue = Integer.parseInt(value);
            if (intValue > 1000000) {
                chkLogFailedLogin.setSelected(false);
            } else {
                chkLogFailedLogin.setSelected(true);
            }
        } else {
            chkLogFailedLogin.setSelected(true);
        }
    }

    private void chkLogFailedLoginActionPerform(ActionEvent evt) {
        Domain domain = Domain.getInstance();
        if (chkLogFailedLogin.isSelected()) {
            domain.setSetting("log_failed_logins", Utils.randomInt(0, 1000000) + "", null);
        } else {
            domain.setSetting("log_failed_logins", Utils.randomInt(1000001, 2000000) + "", null);
        }
    }

    private void btnCreatePasswordRecoveryActionPerform(ActionEvent evt) {
        if (!chkTrust.isSelected()) {
            JOptionPane.showMessageDialog(this, "You should select trusting the server.", "Trust", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int answer = saveDialog.showSaveDialog(this);
        boolean canSave = false;
        if (answer != JFileChooser.CANCEL_OPTION) {
            File file = saveDialog.getSelectedFile();
            if (file.exists()) {
                int answer2 = JOptionPane.showConfirmDialog(this, "The file has already existed. Do you want to rewrite it?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (answer2 == JOptionPane.YES_OPTION) {
                    canSave = true;
                }
            } else {
                canSave = true;
            }
            if (canSave) {
                Domain domain = Domain.getInstance();
                FileWriter writer = null;
                try {
                    String recoveryContent = Utils.createRecoveryContent(domain.getPassword());
                    if (recoveryContent != null) {
                        writer = new FileWriter(file);
                        writer.write(recoveryContent);
                        JOptionPane.showMessageDialog(this, "Recovery file has been created successfully!", "Finish", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        throw new Exception("Recovery Content is null!");
                    }
                } catch (Exception ex) {
                    Logger.log(ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Recovery File creation failed!", "Fatal Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException ex) {
                            Logger.log(ex.getMessage());
                        }
                    }
                }
            }
        }
    }
}
