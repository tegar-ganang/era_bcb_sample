package jblip.gui.components.tree;

import jblip.gui.data.channels.UpdatesDataChannel;
import jblip.resources.Update;

public class UpdatesChannelNode extends ChannelTreeNode<Update> {

    private static final long serialVersionUID = 1L;

    private int unread_count;

    public UpdatesChannelNode(final ChannelTreeModel model, final UpdatesDataChannel data) {
        super(model, data);
    }

    public synchronized int getUnreadCount() {
        return unread_count;
    }

    public synchronized void markAsRead() {
        unread_count = 0;
    }

    @Override
    public UpdatesDataChannel getChannel() {
        return (UpdatesDataChannel) super.getChannel();
    }

    @Override
    public synchronized String getDescriptionString() {
        return String.format("%d nieprzeczytanych wiadomości", unread_count);
    }
}
