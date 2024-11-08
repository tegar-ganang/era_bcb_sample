package de.renier.vdr.channel.editor.actions;

import java.awt.event.ActionEvent;
import java.util.Enumeration;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import de.renier.vdr.channel.ChannelElement;
import de.renier.vdr.channel.editor.ChannelEditor;
import de.renier.vdr.channel.editor.Messages;
import de.renier.vdr.channel.editor.util.Utils;

/**
 * MultiRenameAction
 * 
 * @author <a href="mailto:editor@renier.de">Renier Roth</a>
 */
public class MultiRenameAction extends AbstractAction {

    private static final long serialVersionUID = -1060977673989725823L;

    public MultiRenameAction() {
        super(Messages.getString("MultiRenameAction.0"), new ImageIcon(OpenAction.class.getResource("/org/javalobby/icons/20x20/DocumentDraw.gif")));
        this.setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
        TreePath treePath = ChannelEditor.application.getChannelListingPanel().getLeadSelectionPath();
        if (treePath != null) {
            String namePrefix = JOptionPane.showInputDialog(ChannelEditor.application, Messages.getString("MultiRenameAction.2"), Messages.getString("MultiRenameAction.3"), JOptionPane.QUESTION_MESSAGE);
            if (!Utils.isEmpty(namePrefix)) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                if (node.isRoot()) {
                    Enumeration enumer = node.children();
                    while (enumer.hasMoreElements()) {
                        doRename((DefaultMutableTreeNode) enumer.nextElement(), namePrefix);
                    }
                    doRename(node, namePrefix);
                } else {
                    doRename(node, namePrefix);
                }
                ChannelEditor.application.getChannelListingPanel().treeNodeStructureChanged(node);
            }
        }
    }

    /**
   * @param node
   */
    private void doRename(DefaultMutableTreeNode node, String prefix) {
        Enumeration enumer = node.children();
        while (enumer.hasMoreElements()) {
            DefaultMutableTreeNode renameNode = (DefaultMutableTreeNode) enumer.nextElement();
            ChannelElement channelElement = (ChannelElement) renameNode.getUserObject();
            if (channelElement.isRadioOrTelevisionOrService()) {
                channelElement.setName(prefix + channelElement.getName());
            }
        }
    }
}
