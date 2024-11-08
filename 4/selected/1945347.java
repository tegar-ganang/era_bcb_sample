package rafaelortis.dbsmartcopy.metadataviewer;

import java.awt.Component;
import java.awt.Dialog;
import java.sql.Connection;
import java.text.MessageFormat;
import javax.swing.JComponent;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import rafaelortis.dbsmartcopy.metadataviewer.dbanalizer.DBAnalizer;
import rafaelortis.dbsmartcopy.metadataviewer.dbanalizer.TableNode;

public final class CopyDataWizardAction extends CallableSystemAction {

    private WizardDescriptor.Panel[] panels;

    @Override
    public void performAction() {
        WizardDescriptor wizardDescriptor = new WizardDescriptor(getPanels());
        ((CopyDataWizardPanel3) panels[2]).reload();
        ((CopyDataWizardPanel1) panels[0]).reload();
        wizardDescriptor.setTitleFormat(new MessageFormat("{0}"));
        wizardDescriptor.setTitle("Data Export Wizard");
        Dialog dialog = DialogDisplayer.getDefault().createDialog(wizardDescriptor);
        dialog.setVisible(true);
        dialog.toFront();
        boolean cancelled = wizardDescriptor.getValue() != WizardDescriptor.FINISH_OPTION;
        if (!cancelled) {
            try {
                TableNode table = ((CopyDataWizardPanel1) panels[0]).tableSelected();
                Connection conn = ((CopyDataWizardPanel3) panels[2]).getConnection();
                DBAnalizer analizer = new DBAnalizer(table.getMetaData().getConn());
                String filter = ((CopyDataWizardPanel2) panels[1]).getFilter();
                IOHelper.writeInfo("Begin reading data");
                analizer.readFilteredRelatedData(table.getMetaData(), null, null, filter, null, false);
                IOHelper.writeInfo("Finished reading data");
                IOHelper.writeInfo("Begin writing data");
                analizer.writeAllRelatedData(conn, table.getMetaData(), null, null);
                IOHelper.writeInfo("Finished writing data");
            } catch (Exception ex) {
                IOHelper.writeError(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Initialize panels representing individual wizard's steps and sets
     * various properties for them influencing wizard appearance.
     */
    private WizardDescriptor.Panel[] getPanels() {
        if (panels == null) {
            panels = new WizardDescriptor.Panel[] { new CopyDataWizardPanel1(), new CopyDataWizardPanel2(), new CopyDataWizardPanel3(), new CopyDataWizardPanel4(), new CopyDataWizardPanel5() };
            String[] steps = new String[panels.length];
            for (int i = 0; i < panels.length; i++) {
                Component c = panels[i].getComponent();
                steps[i] = c.getName();
                if (c instanceof JComponent) {
                    JComponent jc = (JComponent) c;
                    jc.putClientProperty("WizardPanel_contentSelectedIndex", new Integer(i));
                    jc.putClientProperty("WizardPanel_contentData", steps);
                    jc.putClientProperty("WizardPanel_autoWizardStyle", Boolean.TRUE);
                    jc.putClientProperty("WizardPanel_contentDisplayed", Boolean.TRUE);
                    jc.putClientProperty("WizardPanel_contentNumbered", Boolean.TRUE);
                }
            }
        }
        return panels;
    }

    @Override
    public String getName() {
        return "Data Export";
    }

    @Override
    public String iconResource() {
        return "rafaelortis/dbsmartcopy/metadataviewer/edit-copy.png";
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }
}
