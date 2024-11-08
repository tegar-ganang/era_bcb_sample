package jblip.gui.components;

import java.util.Observable;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeNode;
import jblip.gui.components.tree.UpdatesChannelNode;
import jblip.gui.data.channels.DataChannel;
import jblip.gui.data.channels.UpdatesDataChannel;

public class ChannelController extends Observable {

    private DataChannel<?> current_channel;

    private final TreeSelectionListener tree_selection_listener;

    public ChannelController() {
        tree_selection_listener = new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent ev) {
                final TreeNode sel = (TreeNode) ev.getNewLeadSelectionPath().getLastPathComponent();
                if (sel instanceof UpdatesChannelNode) {
                    UpdatesChannelNode node = (UpdatesChannelNode) sel;
                    System.err.println("Selected channel node: " + node.getChannel().getName());
                    final UpdatesDataChannel channel = node.getChannel();
                    setChannel(channel);
                }
            }
        };
    }

    public void setChannel(final DataChannel<?> channel) {
        if (channel.equals(current_channel)) {
            return;
        }
        current_channel = channel;
        setChanged();
        notifyObservers(channel);
    }

    public TreeSelectionListener getTreeSelectionListener() {
        return tree_selection_listener;
    }
}
