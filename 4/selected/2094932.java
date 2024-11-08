package sc.fgrid.gui;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import sc.fgrid.client.InstanceProxy;
import sc.fgrid.script.AccessEnum;
import sc.fgrid.script.DisplayType;
import sc.fgrid.types.FilesClientType;
import sc.fgrid.types.VariableAttribute;
import sc.fgrid.types.VariableValue;

/**
 *
 * @author  stoll
 */
public class VarFilesPanel extends VarPanel {

    private static final long serialVersionUID = 1L;

    VariableAttribute attrib;

    private InstanceProxy instanceProxy;

    private MainFrame gui;

    private InstancePanel instancePanel;

    /** Creates new form VarFilesPanel */
    public VarFilesPanel(VariableAttribute attrib, InstanceProxy instanceProxy, MainFrame gui, InstancePanel instancePanel) {
        this.attrib = attrib;
        this.instanceProxy = instanceProxy;
        this.gui = gui;
        this.instancePanel = instancePanel;
        initComponents();
        DisplayType display = attrib.getDisplay();
        AccessEnum access = attrib.getAccess();
        boolean writable = (access == AccessEnum.INOUT);
        jButton_upload.setEnabled(writable);
        jLabel1.setText(attrib.getDescription());
        this.setToolTipText("Tool tip text");
    }

    /** */
    @Override
    public VariableValue getValue() {
        VariableValue value = new VariableValue();
        value.setName(attrib.getName());
        value.setDataType(attrib.getType());
        String status = this.getStatus();
        String clientFileName = this.getClientFileName();
        FilesClientType files = new FilesClientType();
        files.setStatus(status);
        files.setClientFileName(clientFileName);
        value.setFilesValue(files);
        return value;
    }

    @Override
    public void update(VariableValue value) {
        FilesClientType filesClientType = value.getFilesValue();
        String status = filesClientType.getStatus();
        String clientFileName = filesClientType.getClientFileName();
        this.setClientFileName(clientFileName);
        this.setStatus(status);
    }

    void setClientFileName(String fileName) {
        jTextField_clientFileName.setText(fileName);
        this.validate();
    }

    String getClientFileName() {
        return jTextField_clientFileName.getText();
    }

    void setStatus(String status) {
        jTextField_status.setText(status);
        this.validate();
    }

    String getStatus() {
        return jTextField_status.getText();
    }

    private void initComponents() {
        jTextField_clientFileName = new javax.swing.JTextField();
        jButton_selectFile = new javax.swing.JButton();
        jButton_upload = new javax.swing.JButton();
        jButton_download = new javax.swing.JButton();
        jTextField_status = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jTextField_clientFileName.setText("text");
        jButton_selectFile.setText("Select File");
        jButton_selectFile.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_selectFileActionPerformed(evt);
            }
        });
        jButton_upload.setText("upload");
        jButton_upload.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_uploadActionPerformed(evt);
            }
        });
        jButton_download.setText("download");
        jButton_download.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_downloadActionPerformed(evt);
            }
        });
        jTextField_status.setEditable(false);
        jLabel1.setText("jLabel1");
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jLabel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 403, Short.MAX_VALUE).add(jTextField_clientFileName, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 403, Short.MAX_VALUE).add(layout.createSequentialGroup().add(jButton_selectFile).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jButton_upload).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jButton_download).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jTextField_status, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE))).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(jLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jTextField_clientFileName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jButton_selectFile).add(jButton_upload).add(jButton_download)).add(jTextField_status, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
    }

    private void jButton_uploadActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            instancePanel.panel2Variables();
            instanceProxy.upload(attrib.getName());
        } catch (Exception ex) {
            gui.handleException(ex);
        }
    }

    private void jButton_downloadActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            instancePanel.panel2Variables();
            String clientFileName = jTextField_clientFileName.getText();
            File clientFile = new File(clientFileName);
            if (clientFile.exists()) {
                String message = "File " + clientFile.getAbsolutePath() + " already exists!\n" + "Overwrite it?";
                int n = JOptionPane.showConfirmDialog(this, message, "Overwrite file?", JOptionPane.YES_NO_OPTION);
                if (n != 0) {
                    return;
                }
            }
            instanceProxy.download(attrib.getName());
        } catch (Exception ex) {
            gui.handleException(ex);
        }
    }

    private void jButton_selectFileActionPerformed(java.awt.event.ActionEvent evt) {
        String clientFileString = jTextField_clientFileName.getText();
        File oldfile = new File(clientFileString);
        JFileChooser fileChooser = new JFileChooser(oldfile.getParent());
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File choosed = fileChooser.getSelectedFile();
            String abspath = choosed.getAbsolutePath();
            jTextField_clientFileName.setText(abspath);
            this.validate();
        }
    }

    private javax.swing.JButton jButton_download;

    private javax.swing.JButton jButton_selectFile;

    private javax.swing.JButton jButton_upload;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JTextField jTextField_clientFileName;

    private javax.swing.JTextField jTextField_status;
}
