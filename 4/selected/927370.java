package com.ek.mitapp.ui.wizard.panels;

import java.util.Map;
import javax.swing.*;
import org.netbeans.spi.wizard.WizardController;
import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.debug.FormDebugPanel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.ek.mitapp.ui.wizard.MitAppWizardStarter;
import com.ek.mitapp.ui.wizard.MitDataDescriptor;

/**
 * TODO: Class description.
 * <br>
 * Id: $Id: StorePanel.java 1669 2006-04-11 20:46:33Z dhirwinjr $
 *
 * @author dhirwinjr
 */
public class StorePanel extends AbstractWizardPanel {

    /**
	 * New file name.
	 */
    public static final String KEY_OVERWRITE_DATA = "overwriteData";

    /**
	 * Panel's description text.
	 */
    private final String descText = "<html>Click the <b>Next</b> button to save the data in the mitigation prioritization Excel file.</html>";

    /**
	 * Check boxes
	 */
    private JCheckBox overwriteData_cb;

    /**
	 * Default constructor.
	 * 
	 * @param controller
	 * @param wizardData
	 */
    public StorePanel(WizardController controller, Map wizardData) {
        super(wizardData, controller);
        initComponents();
    }

    /**
	 * Initialize the object's components.
	 */
    private void initComponents() {
        overwriteData_cb = new JCheckBox("Overwrite existing data in mitigation prioritization Excel spreadsheet");
        overwriteData_cb.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (overwriteData_cb.isSelected()) wizardData.put(KEY_OVERWRITE_DATA, Boolean.TRUE); else wizardData.put(KEY_OVERWRITE_DATA, Boolean.FALSE);
            }
        });
        wizardData.put(KEY_OVERWRITE_DATA, Boolean.FALSE);
        if (!(Boolean) wizardData.get(FileSelectionPanel.KEY_APPENDING_DATA)) {
            overwriteData_cb.setEnabled(true);
        }
    }

    /**
	 * @see com.ek.mitapp.ui.wizard.panels.JExcelPanel#createPanel()
	 */
    public JPanel createPanel() {
        FormLayout panelLayout = new FormLayout("4dlu, l:p, 4dlu", "4dlu, p, 4dlu, p, 4dlu");
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(panelLayout);
        builder.addLabel(descText, cc.xy(2, 2));
        builder.add(overwriteData_cb, cc.xy(2, 4));
        return builder.getPanel();
    }
}
