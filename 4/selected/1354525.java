package de.renier.vdr.channel.editor.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import de.renier.vdr.channel.ChannelElement;
import de.renier.vdr.channel.editor.ChannelEditor;
import de.renier.vdr.channel.editor.Messages;

/**
 * UnparkAction
 * 
 * @author <a href="mailto:editor@renier.de">Renier Roth</a>
 */
public class UnparkAction extends AbstractAction {

    private static final long serialVersionUID = 934394659505600120L;

    public UnparkAction() {
        super(Messages.getString("UnparkAction.0"), new ImageIcon(OpenAction.class.getResource("/org/javalobby/icons/20x20/BaggageOut.gif")));
        this.setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
        TreePath targetPath = ChannelEditor.application.getChannelListingPanel().getLeadSelectionPath();
        if (targetPath != null) {
            DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) targetPath.getLastPathComponent();
            ChannelElement channelElementTarget = (ChannelElement) targetNode.getUserObject();
            Object[] sources = ChannelEditor.application.getChannelParkingPanel().getSelectedOrAllElements();
            if (sources != null) {
                for (int i = 0; i < sources.length; i++) {
                    DefaultMutableTreeNode sourceNode = (DefaultMutableTreeNode) sources[i];
                    ChannelElement channelElementSource = (ChannelElement) sourceNode.getUserObject();
                    if (channelElementSource.isRadioOrTelevisionOrService() && channelElementTarget.isCategory()) {
                        ChannelEditor.application.getChannelListingPanel().insertNodeInto(sourceNode, targetNode, targetNode.getChildCount());
                    } else {
                        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) targetNode.getParent();
                        int pos = parentNode.getIndex(targetNode);
                        ChannelEditor.application.getChannelListingPanel().insertNodeInto(sourceNode, parentNode, pos);
                    }
                    ChannelEditor.application.getChannelParkingPanel().removeElement(sourceNode);
                }
                ChannelEditor.application.setModified(true);
            }
        }
    }
}
