package jblip.gui.components.tree;

import java.io.Serializable;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;
import jblip.gui.data.channels.DataChannel;

public class ChannelTreeNode<T extends Serializable> extends DefaultMutableTreeNode {

    private static final long serialVersionUID = 1L;

    private static final ImageIcon DEFAULT_ICON;

    static {
        DEFAULT_ICON = new ImageIcon(ChannelTreeNode.class.getResource("/jblip/gui/resources/femto_blip.png"));
    }

    protected final DataChannel<T> channel;

    protected final ChannelTreeModel tree_model;

    public ChannelTreeNode(final ChannelTreeModel model, final DataChannel<T> data) {
        channel = data;
        tree_model = model;
    }

    public ImageIcon getIcon() {
        return DEFAULT_ICON;
    }

    public DataChannel<T> getChannel() {
        return channel;
    }

    public String getDescriptionString() {
        return null;
    }
}
