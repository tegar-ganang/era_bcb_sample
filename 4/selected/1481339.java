package de.nava.risotto;

import javax.swing.tree.DefaultMutableTreeNode;
import de.nava.informa.core.ChannelIF;

public class ChannelTreeNode extends DefaultMutableTreeNode {

    private ChannelIF channel;

    public ChannelTreeNode(ChannelIF channel) {
        super(stringify(channel));
        this.channel = channel;
    }

    public ChannelIF getChannel() {
        return channel;
    }

    public void update() {
        this.setUserObject(stringify(channel));
    }

    private static String stringify(ChannelIF channel) {
        return channel.getTitle() + " (" + channel.getItems().size() + ")";
    }
}
