package jmri.jmrit.operations.setup;

import java.awt.GridBagLayout;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import jmri.jmrit.operations.OperationsFrame;
import jmri.jmrit.operations.rollingstock.cars.CarManagerXml;
import jmri.jmrit.operations.rollingstock.engines.EngineManagerXml;
import jmri.jmrit.operations.trains.TrainManager;
import jmri.jmrit.operations.trains.TrainManagerXml;

/**
 * Frame for backing up operation files
 * 
 * @author Dan Boudreau Copyright (C) 2008, 2011
 * @version $Revision: 1.8 $
 */
public class BackupFrame extends OperationsFrame {

    static final ResourceBundle rb = ResourceBundle.getBundle("jmri.jmrit.operations.setup.JmritOperationsSetupBundle");

    javax.swing.JLabel textBackup = new javax.swing.JLabel();

    javax.swing.JButton backupButton = new javax.swing.JButton();

    javax.swing.JTextField backupTextField = new javax.swing.JTextField(20);

    Backup backup = new Backup();

    public BackupFrame() {
        super(ResourceBundle.getBundle("jmri.jmrit.operations.setup.JmritOperationsSetupBundle").getString("TitleOperationsBackup"));
    }

    public void initComponents() {
        textBackup.setText(rb.getString("BackupFiles"));
        backupButton.setText(rb.getString("Backup"));
        backupTextField.setText(backup.getDirectoryName());
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        addItem(panel, textBackup, 0, 1);
        addItem(panel, backupTextField, 0, 2);
        addItem(panel, backupButton, 0, 3);
        getContentPane().add(panel);
        addButtonAction(backupButton);
        addHelpMenu("package.jmri.jmrit.operations.Operations_BackupRestore", true);
        pack();
        if (getHeight() < 150) setSize(300, getHeight() + 50);
        setVisible(true);
    }

    public void buttonActionPerformed(java.awt.event.ActionEvent ae) {
        if (ae.getSource() == backupButton) {
            log.debug("backup button activated");
            if (CarManagerXml.instance().isDirty() || EngineManagerXml.instance().isDirty() || TrainManagerXml.instance().isDirty()) {
                if (JOptionPane.showConfirmDialog(this, rb.getString("OperationsFilesModified"), rb.getString("SaveOperationFiles"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    TrainManager.instance().save();
                }
            }
            if (backup.checkDirectoryExists(backupTextField.getText())) {
                if (JOptionPane.showConfirmDialog(this, MessageFormat.format(rb.getString("DirectoryAreadyExists"), new Object[] { backupTextField.getText() }), rb.getString("OverwriteBackupDirectory"), JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
            }
            boolean success = backup.backupFiles(backupTextField.getText());
            if (success) {
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Could not backup operation files", "Backup failed!", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BackupFrame.class.getName());
}
