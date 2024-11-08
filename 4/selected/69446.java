package it.unibo.cs.ndiff.ui.gui;

import it.unibo.cs.ndiff.ui.OperationsHandler;
import it.unibo.cs.ndiff.ui.Parameters;
import it.unibo.cs.ndiff.ui.i18n.MessageHandler;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

public class MainGui extends AbstractMainGui {

    private static final long serialVersionUID = 1L;

    private static final String BUNDLE_PROPERTY = "mainGUI";

    private MessageHandler messages = null;

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                MainGui application = new MainGui();
                application.setVisible(true);
            }
        });
    }

    public MainGui() {
        super();
        messages = new MessageHandler(BUNDLE_PROPERTY);
        initialize();
        pack();
    }

    private void initialize() {
        this.setTitle(messages.getString("MainGui.MAIN_TITLE"));
        fileSelectorPanel1.setBorder(BorderFactory.createTitledBorder(null, messages.getString("MainGui.ORIGINAL_FILE"), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)));
        fileSelectorPanel2.setBorder(BorderFactory.createTitledBorder(null, messages.getString("MainGui.MODIFIED_FILE"), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)));
        fileSelectorPanel3.setBorder(BorderFactory.createTitledBorder(null, messages.getString("MainGui.OUTPUT_FILE"), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)));
        fileSelectorXSLpanel.setBorder(BorderFactory.createTitledBorder(null, messages.getString("MainGui.XSL_FILE"), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)));
        useXSLcheck.setText(messages.getString("MainGui.USE_XSL"));
        okButton.setText(messages.getString("MainGui.START_OPERATION"));
    }

    @Override
    void changeOperationStatus() {
        switch(operationsPanel.getSelectedOperation()) {
            case 1:
            case 3:
                fileSelectorPanel1.setTitle("Original file:");
                fileSelectorPanel2.setTitle("Modified file:");
                break;
            case 2:
                fileSelectorPanel1.setTitle("Original file:");
                fileSelectorPanel2.setTitle("Delta file:");
                break;
            default:
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        fileSelectorPanel1.setEnabled(enabled);
        fileSelectorPanel2.setEnabled(enabled);
        fileSelectorPanel3.setEnabled(enabled);
        useXSLcheck.setEnabled(enabled);
        if (useXSLcheck.isSelected()) {
            fileSelectorXSLpanel.setEnabled(enabled);
        } else {
            fileSelectorXSLpanel.setEnabled(false);
        }
        operationsPanel.setEnabled(enabled);
        okButton.setEnabled(enabled);
    }

    @Override
    protected void startOperation() {
        setEnabled(false);
        doOperation();
        setEnabled(true);
    }

    private void doOperation() {
        Parameters params = new Parameters();
        params.setMerge(operationsPanel.isMerge());
        params.setDiff(operationsPanel.isDiff());
        params.setStdout(false);
        params.setXslt(false);
        File tempFile;
        tempFile = fileSelectorPanel1.getSelectedFile();
        if (tempFile.canRead() && tempFile.isFile()) {
            params.setOriginalPath(fileSelectorPanel1.getSelectedStringFile());
        } else {
            JOptionPane.showMessageDialog((this), "File: \"" + fileSelectorPanel1.getSelectedStringFile() + "\" not exists or is not readable", "JNDiff Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        tempFile = fileSelectorPanel2.getSelectedFile();
        if (tempFile.canRead() && tempFile.isFile()) {
            if (operationsPanel.isDiff()) {
                params.setModifiedPath(fileSelectorPanel2.getSelectedStringFile());
            } else {
                params.setDeltaPath(fileSelectorPanel2.getSelectedStringFile());
            }
        } else {
            JOptionPane.showMessageDialog((this), "File: \"" + fileSelectorPanel2.getSelectedStringFile() + "\" not exists or is not readable", "JNDiff Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        tempFile = fileSelectorPanel3.getSelectedFile();
        if (tempFile.getParentFile().isDirectory() && tempFile.getParentFile().canWrite()) {
            if (tempFile.isFile() && tempFile.canWrite()) {
                int overWriteDecision = JOptionPane.showConfirmDialog(this, "File \"" + tempFile.getAbsolutePath() + "\"\n" + "Already exists.\n" + "Do you overwrite?", "JNDiff warning:", JOptionPane.YES_NO_OPTION);
                switch(overWriteDecision) {
                    case 0:
                        break;
                    case 1:
                    default:
                        return;
                }
            }
            if (operationsPanel.isMerge()) {
                params.setMarkupPath(fileSelectorPanel3.getSelectedStringFile());
            } else {
                params.setDeltaPath(fileSelectorPanel3.getSelectedStringFile());
            }
        }
        try {
            OperationsHandler.doOperation(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void xslCheckChange() {
        fileSelectorXSLpanel.setEnabled(useXSLcheck.isSelected());
    }
}
