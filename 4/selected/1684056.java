package de.renier.vdr.channel.editor.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import de.renier.vdr.channel.editor.ChannelEditor;
import de.renier.vdr.channel.editor.Messages;

/**
 * CloseAction
 * 
 * @author <a href="mailto:editor@renier.de">Renier Roth</a>
 */
public class CloseAction extends AbstractAction {

    private static final long serialVersionUID = 3062727121744152842L;

    public CloseAction() {
        super(Messages.getString("CloseAction.0"), new ImageIcon(OpenAction.class.getResource("/org/javalobby/icons/20x20/Hide.gif")));
    }

    public void actionPerformed(ActionEvent e) {
        int result = JOptionPane.OK_OPTION;
        if (ChannelEditor.application.getChannelParkingPanel().getListSize() > 0) {
            result = JOptionPane.showConfirmDialog(ChannelEditor.application, Messages.getString("CloseAction.2"), Messages.getString("CloseAction.3"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.NO_OPTION) {
                return;
            }
        }
        if (ChannelEditor.application.isModified()) {
            result = JOptionPane.showConfirmDialog(ChannelEditor.application, Messages.getString("CloseAction.4"), Messages.getString("CloseAction.5"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.NO_OPTION) {
                return;
            }
        }
        System.exit(0);
    }
}
