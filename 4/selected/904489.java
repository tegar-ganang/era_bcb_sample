package cz.hdf.cdnavigator.gui;

import cz.hdf.cdnavigator.db.DataManagement;
import cz.hdf.config.Config;
import cz.hdf.exceptions.XMLException;
import cz.hdf.gui.GUI;
import cz.hdf.gui.HButton;
import cz.hdf.gui.HButtonNoted;
import cz.hdf.gui.HLabel;
import cz.hdf.gui.HLabelComponent;
import cz.hdf.gui.HMessageDialog;
import cz.hdf.gui.HPanel;
import cz.hdf.i18n.I18N;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JTextField;

/**
 * 
 * @author hunter
 */
public class CDDBManagementPanel extends HPanel {

    /** the top panel - changer panel */
    protected ChangerPanel changer;

    /** all CD tab XML file field */
    private JTextField atFileField;

    /** all CD tab export db button */
    private HButtonNoted atExportButton;

    /** all CD tab import db button */
    private HButtonNoted atImportButton;

    /** all CD tab export/import file dialog button */
    private JButton atFSChooserButton;

    /** all CD tab check db button */
    private HButtonNoted atCheckButton;

    /** all CD tab repair db button */
    private HButtonNoted atRepairButton;

    /** Logger. Hierarchy is set to name of this class. */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
   *
   *
   * @param _changer  
   */
    public CDDBManagementPanel(ChangerPanel _changer) {
        changer = _changer;
        HPanel atEIFilePanel = new HPanel();
        atEIFilePanel.setOpaque(false);
        atEIFilePanel.setLayout(new BorderLayout());
        atFileField = new JTextField(Config.getConfigurationDir() + Config.FS + "CDnavigatorData.xml");
        atFileField.setEnabled(false);
        atFSChooserButton = new HButton(Config.getIcon(Config.ICON_TYPE_FILE_OPEN, Config.ICON_SIZE_16));
        atFSChooserButton.setEnabled(false);
        atExportButton = new HButtonNoted("Export");
        atImportButton = new HButtonNoted("Import");
        atExportButton.setEnabled(false);
        atImportButton.setEnabled(false);
        HPanel atFileFieldPanel = new HPanel();
        atFileFieldPanel.setBorder(BorderFactory.createEmptyBorder(0, GUI.COMPONENT_HGAP, 0, GUI.COMPONENT_HGAP));
        atFileFieldPanel.setLayout(new BorderLayout());
        atFileFieldPanel.add(atFileField);
        atEIFilePanel.add(new HLabel("XML file"), BorderLayout.WEST);
        atEIFilePanel.add(atFileFieldPanel, BorderLayout.CENTER);
        atEIFilePanel.add(atFSChooserButton, BorderLayout.EAST);
        HPanel atEIButtonPanel = new HPanel();
        atEIButtonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, GUI.COMPONENT_HGAP, 0));
        atEIButtonPanel.setOpaque(false);
        atEIButtonPanel.add(atExportButton);
        atEIButtonPanel.add(atImportButton);
        HPanel atEIInnerPanel = new HPanel();
        atEIInnerPanel.setOpaque(false);
        atEIInnerPanel.setLayout(new BoxLayout(atEIInnerPanel, BoxLayout.Y_AXIS));
        atEIInnerPanel.add(atEIFilePanel);
        atEIInnerPanel.add(Box.createVerticalStrut(GUI.COMPONENT_VGAP));
        atEIInnerPanel.add(atEIButtonPanel);
        HLabelComponent atEIPanel = new HLabelComponent("DB data", atEIInnerPanel, true);
        atEIPanel.setOpaque(false);
        HPanel atStructInnerPanel = new HPanel();
        atStructInnerPanel.setLayout(new BoxLayout(atStructInnerPanel, BoxLayout.Y_AXIS));
        atCheckButton = new HButtonNoted("Check DB structure");
        atRepairButton = new HButtonNoted("Repair DB structure");
        atRepairButton.setEnabled(false);
        atCheckButton.setAlignmentX(0.5f);
        atRepairButton.setAlignmentX(0.5f);
        atStructInnerPanel.add(atCheckButton);
        atStructInnerPanel.add(Box.createVerticalStrut(GUI.COMPONENT_VGAP));
        atStructInnerPanel.add(atRepairButton);
        HLabelComponent atStructPanel = new HLabelComponent("DB structure", atStructInnerPanel, true, HLabelComponent.FILL_BOTH);
        atStructPanel.setOpaque(false);
        HPanel atXSLTInnerPanel = new HPanel();
        HLabelComponent atXSLTPanel = new HLabelComponent("Generate output", atXSLTInnerPanel, true, HLabelComponent.FILL_BOTH);
        atXSLTPanel.setOpaque(false);
        atXSLTPanel.setBorder(BorderFactory.createEmptyBorder(0, GUI.DIALOG_HGAP, GUI.DIALOG_VGAP, GUI.DIALOG_HGAP));
        HPanel atNorthPanel = new HPanel();
        atNorthPanel.setLayout(new BoxLayout(atNorthPanel, BoxLayout.X_AXIS));
        atNorthPanel.setBorder(BorderFactory.createEmptyBorder(GUI.DIALOG_VGAP, GUI.DIALOG_HGAP, GUI.PANEL_VGAP, GUI.DIALOG_HGAP));
        atNorthPanel.setOpaque(false);
        atNorthPanel.add(atEIPanel);
        atNorthPanel.add(Box.createHorizontalStrut(GUI.PANEL_HGAP));
        atNorthPanel.add(atStructPanel);
        setLayout(new BorderLayout());
        setOpaque(false);
        add(atNorthPanel, BorderLayout.NORTH);
        add(atXSLTPanel, BorderLayout.CENTER);
        atExportButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                atActionExport();
            }
        });
        atImportButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                atActionImport();
            }
        });
        atCheckButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                atCheckDBStructure();
            }
        });
        atRepairButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                atRepairDBStructure();
            }
        });
    }

    /**
   * Export data to XML file.
   */
    private void atActionExport() {
        File xmlFile = new File(atFileField.getText());
        try {
            boolean created = xmlFile.createNewFile();
            if (!created) {
                changer.setStatusText(I18N.translate("Export file already exists."));
                HMessageDialog dlg = new HMessageDialog(changer.topFrame, HMessageDialog.MESSAGE_QUESTION, I18N.translate("Export file already exists. Do you want rewrite this file?"));
                if (dlg.getResult() == HMessageDialog.RESULT_NO) {
                    return;
                }
            }
        } catch (IOException e) {
            changer.setStatusText(I18N.translate("Can not create new export file. ") + e.getLocalizedMessage());
            new HMessageDialog(changer.topFrame, HMessageDialog.MESSAGE_ERROR, I18N.translate("Can not create new export file. " + e.getLocalizedMessage()));
            return;
        }
        if (!xmlFile.canWrite()) {
            changer.setStatusText(I18N.translate("Can not write to export file."));
            new HMessageDialog(changer.topFrame, HMessageDialog.MESSAGE_ERROR, I18N.translate("Can not write to export file."));
            return;
        }
        try {
            DataManagement.getDBManager().saveDataToXML(xmlFile);
            changer.setStatusText(I18N.translate("Data successfully exported to file '" + xmlFile.getName() + "'."));
        } catch (SQLException e) {
            changer.setStatusText(I18N.translate("Can not get data from DB. " + e.getLocalizedMessage()));
            new HMessageDialog(changer.topFrame, HMessageDialog.MESSAGE_ERROR, I18N.translate("Can not get data from DB. " + e.getLocalizedMessage()));
        } catch (IOException e) {
            changer.setStatusText(I18N.translate("Can not write to export file. " + e.getLocalizedMessage()));
            new HMessageDialog(changer.topFrame, HMessageDialog.MESSAGE_ERROR, I18N.translate("Can not write to export file. " + e.getLocalizedMessage()));
        }
    }

    /**
   * Import data from XML file.
   */
    private void atActionImport() {
        File xmlFile = new File(atFileField.getText());
        if (!xmlFile.exists()) {
            changer.setStatusText(I18N.translate("Can not found XML file."));
            new HMessageDialog(changer.topFrame, HMessageDialog.MESSAGE_ERROR, I18N.translate("Can not found XML file."));
            return;
        }
        if (!xmlFile.canRead()) {
            changer.setStatusText(I18N.translate("Can not read XML file."));
            new HMessageDialog(changer.topFrame, HMessageDialog.MESSAGE_ERROR, I18N.translate("Can not read XML file."));
            return;
        }
        try {
            DataManagement.getDBManager().loadDataFromXML(xmlFile);
            changer.setStatusText(I18N.translate("Data successfully imported from file '" + xmlFile.getName() + "'."));
        } catch (SQLException e) {
            changer.setStatusText(I18N.translate("Can not set data to DB. " + e.getLocalizedMessage()));
            new HMessageDialog(changer.topFrame, HMessageDialog.MESSAGE_ERROR, I18N.translate("Can not set data to DB. " + e.getLocalizedMessage()));
        } catch (IOException e) {
            changer.setStatusText(I18N.translate("Can not read XML file. " + e.getLocalizedMessage()));
            new HMessageDialog(changer.topFrame, HMessageDialog.MESSAGE_ERROR, I18N.translate("Can not read XML file. " + e.getLocalizedMessage()));
        } catch (XMLException e) {
            changer.setStatusText(I18N.translate("Can not parse XML file. " + e.getLocalizedMessage()));
            new HMessageDialog(changer.topFrame, HMessageDialog.MESSAGE_ERROR, I18N.translate("Can not parse XML file. " + e.getLocalizedMessage()));
        }
    }

    /**
   * Check whole DB structure with DB model. Enable repair button when test
   * failed.
   */
    private void atCheckDBStructure() {
        try {
            DataManagement.getDBManager().checkDB();
            atRepairButton.setEnabled(false);
            changer.setStatusText(I18N.translate("Database structure is OK."));
            new HMessageDialog(changer.topFrame, HMessageDialog.MESSAGE_OK, I18N.translate("Database structure is OK."));
        } catch (SQLException e) {
            atRepairButton.setEnabled(true);
            changer.setStatusText(I18N.translate("Database structure is probably DEMAGED."));
            new HMessageDialog(changer.topFrame, HMessageDialog.MESSAGE_WARNING, I18N.translate("Database structure is probably DEMAGED. I recommend to backup DB (export or DB backup) and try to repair DB with 'Repair' button."));
        }
    }

    /**
   * Repair DB structure according to DB model. Disable repair button on
   * success.
   */
    private void atRepairDBStructure() {
        try {
            DataManagement.getDBManager().repairDB();
            atRepairButton.setEnabled(false);
            changer.setStatusText(I18N.translate("Database structure successfully repaired."));
            new HMessageDialog(changer.topFrame, HMessageDialog.MESSAGE_OK, I18N.translate("Database structure successfully repaired. Check your data."));
        } catch (SQLException e) {
            changer.setStatusText(I18N.translate("Can not repair DB structure."));
            new HMessageDialog(changer.topFrame, HMessageDialog.MESSAGE_ERROR, I18N.translate("Can not repair DB structure. Restore data from your backup or reinstall this program and import data from XML file."));
        }
    }
}
