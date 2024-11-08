package org.rdv.ui;

/**
 * @author Jason P. Hanley
 */
public class ChannelSelectionEvent {

    String channelName;

    int children;

    boolean root;

    public ChannelSelectionEvent(String channelName, int children) {
        this(channelName, children, false);
    }

    public ChannelSelectionEvent(String channelName, int children, boolean root) {
        this.channelName = channelName;
        this.children = children;
        this.root = root;
    }

    public String getChannelName() {
        return channelName;
    }

    public int getChildren() {
        return children;
    }

    public boolean isRoot() {
        return root;
    }
}
