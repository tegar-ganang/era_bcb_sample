package toxtree.ui.actions;

import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import toxTree.io.Tools;
import toxtree.data.DataModule;
import toxtree.data.ToxTreeActions;

/**
 * Saves a file 
 * @author Nina Jeliazkova
 * <b>Modified</b> 2005-10-23
 */
public class SaveFileAction extends DataModuleAction {

    String[] ext = { ".sdf", ".csv", ".txt", ".pdf" };

    String[] ext_description = { "SDF files (*.sdf)", "Comma delimited (*.csv)", "Text files (*.txt)", "PDF files (*.pdf)" };

    /**
	 * Comment for <code>serialVersionUID</code>
	 */
    private static final long serialVersionUID = -6068359897306217218L;

    /**
	 * @param module
	 */
    public SaveFileAction(DataModule module) {
        this(module, "Save");
    }

    /**
	 * @param module
	 * @param name
	 */
    public SaveFileAction(DataModule module, String name) {
        this(module, name, Tools.getImage("save.png"));
    }

    /**
	 * @param module
	 * @param name
	 * @param icon
	 */
    public SaveFileAction(DataModule module, String name, Icon icon) {
        super(module, name, icon);
        putValue(AbstractAction.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
        StringBuffer b = new StringBuffer();
        b.append("<html>");
        b.append("Saves the current set of chemical compounds into a file");
        for (int i = 0; i < ext_description.length; i++) {
            b.append("<br>");
            b.append(ext_description[i]);
        }
        b.append("</html>");
        putValue(AbstractAction.SHORT_DESCRIPTION, b.toString());
    }

    @Override
    public void run() throws Exception {
        module.getActions().allActionsEnable(true);
        File file = ToxTreeActions.selectFile(module.getActions().getFrame(), ext, ext_description, false);
        if (file != null) {
            if (file.exists()) {
                if (JOptionPane.showConfirmDialog(frame, "File " + file.getAbsolutePath() + " already exists.\nOverwrite?", "Please confirm", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION) return;
            }
            module.getActions().allActionsEnable(false);
            module.getDataContainer().saveFile(file);
        }
    }
}
