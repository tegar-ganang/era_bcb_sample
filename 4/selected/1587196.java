package de.renier.vdr.channel.editor.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import de.renier.vdr.channel.ChannelCategory;
import de.renier.vdr.channel.editor.ChannelEditor;
import de.renier.vdr.channel.editor.Messages;
import de.renier.vdr.channel.editor.util.Utils;

/**
 * CreateCategoryAction
 * 
 * @author <a href="mailto:editor@renier.de">Renier Roth</a>
 */
public class CreateCategoryAction extends AbstractAction {

    private static final long serialVersionUID = -1731338871238015167L;

    public CreateCategoryAction() {
        super(Messages.getString("CreateCategoryAction.0"), new ImageIcon(OpenAction.class.getResource("/org/javalobby/icons/20x20/NewFolder.gif")));
        this.setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
        TreePath treePath = ChannelEditor.application.getChannelListingPanel().getLeadSelectionPath();
        if (treePath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            if (node.isRoot()) {
                String categoryName = JOptionPane.showInputDialog(ChannelEditor.application, Messages.getString("CreateCategoryAction.2"), Messages.getString("CreateCategoryAction.3"), JOptionPane.QUESTION_MESSAGE);
                if (!Utils.isEmpty(categoryName)) {
                    categoryName = categoryName.trim().replace(':', '|');
                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(new ChannelCategory(categoryName));
                    ChannelEditor.application.getChannelListingPanel().insertNodeInto(newNode, node, node.getChildCount());
                    ChannelEditor.application.getChannelListingPanel().treeNodeStructureChanged(node);
                    ChannelEditor.application.setModified(true);
                }
            }
        }
    }
}
