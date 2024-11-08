package de.nava.risotto;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import de.nava.informa.core.ChannelIF;

public class ChannelTree extends JTree {

    private DefaultMutableTreeNode root;

    private DefaultTreeModel treeModel;

    private Map channelMap;

    public ChannelTree(Collection channels) {
        super();
        this.root = new DefaultMutableTreeNode("Root");
        this.treeModel = new DefaultTreeModel(root);
        channelMap = new HashMap();
        Iterator it = channels.iterator();
        int idx = 0;
        while (it.hasNext()) {
            ChannelIF channel = (ChannelIF) it.next();
            ChannelTreeNode node = new ChannelTreeNode(channel);
            treeModel.insertNodeInto(node, root, idx);
            channelMap.put(new Long(channel.getId()), node);
            idx++;
        }
        setModel(treeModel);
    }

    public DefaultMutableTreeNode getRoot() {
        return root;
    }

    public DefaultTreeModel getTreeModel() {
        return treeModel;
    }

    public ChannelTreeNode getChannelTreeNode(ChannelIF channel) {
        return (ChannelTreeNode) channelMap.get(new Long(channel.getId()));
    }
}
