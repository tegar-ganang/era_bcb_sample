package jblip.gui.components.tree;

import java.awt.Component;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class ChannelTree extends JTree {

    private static final long serialVersionUID = 1L;

    private static class ChannelTreeCellRenderer extends DefaultTreeCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof ChannelTreeNode<?>) {
                ChannelTreeNode<?> node = (ChannelTreeNode<?>) value;
                setIcon(node.getIcon());
                setText(node.getChannel().getName());
            }
            return this;
        }
    }

    public ChannelTree() {
        super(new ChannelTreeModel());
        setRootVisible(false);
        selectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        setEditable(false);
        setShowsRootHandles(true);
        setCellRenderer(new ChannelTreeCellRenderer());
        addTreeWillExpandListener(new TreeWillExpandListener() {

            @Override
            public void treeWillExpand(TreeExpansionEvent event) {
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                throw new ExpandVetoException(event);
            }
        });
        final ChannelTreeModel model = (ChannelTreeModel) getModel();
        for (TreeNode node : model.getRootChildren()) {
            expandPath(new TreePath(model.getPathToRoot(node)));
        }
    }
}
