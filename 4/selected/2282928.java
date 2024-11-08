package de.renier.vdr.channel.editor.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import de.renier.vdr.channel.editor.ChannelEditor;
import de.renier.vdr.channel.editor.Messages;

/**
 * ParkAction
 * 
 * @author <a href="mailto:editor@renier.de">Renier Roth</a>
 */
public class ParkAction extends AbstractAction {

    private static final long serialVersionUID = -5545739754263312091L;

    public ParkAction() {
        super(Messages.getString("ParkAction.0"), new ImageIcon(OpenAction.class.getResource("/org/javalobby/icons/20x20/BaggageIn.gif")));
        this.setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
        TreePath[] treepaths = ChannelEditor.application.getChannelListingPanel().getSelectionPaths();
        if (treepaths != null) {
            for (int i = 0; i < treepaths.length; i++) {
                TreePath path = treepaths[i];
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (!node.isRoot()) {
                    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
                    node.removeFromParent();
                    ChannelEditor.application.getChannelParkingPanel().addElement(node);
                    ChannelEditor.application.getChannelListingPanel().treeNodeStructureChanged(parentNode);
                }
            }
            ChannelEditor.application.setModified(true);
        }
    }
}
