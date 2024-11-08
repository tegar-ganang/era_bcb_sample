package de.renier.vdr.channel.editor.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import de.renier.vdr.channel.Channel;
import de.renier.vdr.channel.ChannelElement;
import de.renier.vdr.channel.editor.ChannelEditor;
import de.renier.vdr.channel.editor.CreateChannelDialog;
import de.renier.vdr.channel.editor.Messages;

/**
 * CreateChannelAction
 * 
 * @author <a href="mailto:editor@renier.de">Renier Roth</a>
 */
public class CreateChannelAction extends AbstractAction {

    private static final long serialVersionUID = 5450151829301164987L;

    public CreateChannelAction() {
        super(Messages.getString("CreateChannelAction.0"), new ImageIcon(OpenAction.class.getResource("/org/javalobby/icons/20x20/New.gif")));
    }

    public void actionPerformed(ActionEvent e) {
        TreePath treePath = ChannelEditor.application.getChannelListingPanel().getLeadSelectionPath();
        DefaultMutableTreeNode node = null;
        int insertPosition = 0;
        if (treePath == null) {
            node = ChannelEditor.application.getChannelListingPanel().getRootNode();
        } else {
            node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        }
        if (!node.isRoot() && !((ChannelElement) node.getUserObject()).isCategory()) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            insertPosition = parent.getIndex(node);
            node = parent;
        }
        CreateChannelDialog createChannelDlg = new CreateChannelDialog(ChannelEditor.application);
        int result = createChannelDlg.showDialog();
        if (result == CreateChannelDialog.RESULT_CREATE) {
            Channel channel = createChannelDlg.getChannel();
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(channel);
            ChannelEditor.application.getChannelListingPanel().insertNodeInto(newNode, node, insertPosition);
            ChannelEditor.application.getChannelListingPanel().treeNodeStructureChanged(node);
            ChannelEditor.application.setModified(true);
        }
    }
}
