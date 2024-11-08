package de.renier.vdr.channel.editor.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import de.renier.vdr.channel.editor.ChannelEditor;
import de.renier.vdr.channel.editor.Messages;
import de.renier.vdr.channel.editor.util.Utils;

/**
 * SaveAction
 * 
 * @author <a href="mailto:editor@renier.de">Renier Roth</a>
 */
public class SaveAction extends AbstractAction {

    private static final long serialVersionUID = -496038610815542654L;

    public SaveAction() {
        super(Messages.getString("SaveAction.0"), new ImageIcon(OpenAction.class.getResource("/org/javalobby/icons/20x20/Save.gif")));
        this.setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
        if (ChannelEditor.application.getChannelFile() != null && ChannelEditor.application.isModified()) {
            File saveFile = ChannelEditor.application.getChannelFile();
            try {
                FileWriter outFile = new FileWriter(saveFile);
                Utils.outputChannelTree(outFile, ChannelEditor.application.getChannelListingPanel().getRootNode());
                ChannelEditor.application.setModified(false);
            } catch (Exception ioe) {
                JOptionPane.showConfirmDialog(ChannelEditor.application, Messages.getString("SaveAction.2") + saveFile.getPath() + Messages.getString("SaveAction.3") + ioe.getMessage(), Messages.getString("SaveAction.4"), JOptionPane.CLOSED_OPTION, JOptionPane.ERROR_MESSAGE);
                ioe.printStackTrace();
            }
        } else {
            if (ChannelEditor.application.getChannelFile() == null) {
                ActionManager.getInstance().getSaveAsAction().actionPerformed(e);
            }
        }
    }
}
