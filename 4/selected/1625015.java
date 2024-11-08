package consciouscode.bonsai.components;

import consciouscode.bonsai.channels.BasicChannel;
import consciouscode.bonsai.channels.Channel;
import consciouscode.bonsai.channels.ChannelEvent;
import consciouscode.bonsai.channels.ChannelListener;
import java.util.List;
import javax.swing.AbstractListModel;

/**
   A Swing ListModel that uses a list elements in a channel.
*/
public class BListModel extends AbstractListModel implements ChannelListener {

    /**
       Identifies the {@link Channel} holding the current list elements.
    */
    public static final String CHANNEL_ELEMENTS = "elements";

    public BListModel() {
        this(new BasicChannel());
    }

    public BListModel(Channel elements) {
        myElementsChannel = elements;
        prepareChannels();
    }

    public List<?> getElements() {
        return (List<?>) myElementsChannel.getValue();
    }

    public void setElements(List<?> elements) {
        myElementsChannel.setValue(elements);
    }

    public Channel getChannel(String name) {
        return (name.equals(CHANNEL_ELEMENTS) ? myElementsChannel : null);
    }

    public int getSize() {
        List<?> elements = (List<?>) myElementsChannel.getValue();
        return (elements != null ? elements.size() : 0);
    }

    public Object getElementAt(int i) {
        List<?> elements = (List<?>) myElementsChannel.getValue();
        return (elements != null ? elements.get(i) : null);
    }

    public void channelUpdate(ChannelEvent event) {
        Channel source = event.getChannel();
        if (source == myElementsChannel) {
            List<?> oldList = (List<?>) event.getOldValue();
            List<?> newList = (List<?>) event.getNewValue();
            int oldSize = (oldList != null ? oldList.size() : 0);
            int newSize = (newList != null ? newList.size() : 0);
            if (newSize < oldSize) {
                fireIntervalRemoved(this, newSize, oldSize - 1);
                if (newSize != 0) {
                    fireContentsChanged(this, 0, newSize - 1);
                }
            } else if (oldSize < newSize) {
                fireIntervalAdded(this, oldSize, newSize - 1);
                if (oldSize != 0) {
                    fireContentsChanged(this, 0, oldSize - 1);
                }
            } else {
                if (newSize != 0) {
                    fireContentsChanged(this, 0, newSize - 1);
                }
            }
        }
    }

    protected void prepareChannels() {
        myElementsChannel.addChannelListener(this);
    }

    private Channel myElementsChannel;
}
