package charismata.broadcast;

import java.util.ArrayList;
import java.util.List;

public class Channel {

    private List<ChannelEntry> broadcastList;

    private List<ChannelEntry> listenerList;

    private String channelName = null;

    private boolean hasListener;

    private boolean hasBroadcaster;

    public Channel(String channelName) {
        this.channelName = channelName;
        broadcastList = new ArrayList();
        listenerList = new ArrayList();
    }

    public boolean hasListener() {
        return hasListener;
    }

    public void setHasListener(boolean hasListener) {
        this.hasListener = hasListener;
    }

    public boolean hasBroadcaster() {
        return hasBroadcaster;
    }

    public void setHasBroadcaster(boolean hasBroadcaster) {
        this.hasBroadcaster = hasBroadcaster;
    }

    public void broadcast(BroadcastInfo bi) {
        for (ChannelEntry channelEntry : listenerList) {
            Container container = channelEntry.getContainer();
            channelEntry.getChannelProgram().listen(container, this, bi);
        }
    }

    public void removeListener(Container container, ChannelProgram channelProgram) {
        boolean removed = listenerList.remove(channelProgram);
        if (listenerList.size() == 0) {
            hasListener = false;
            unHookContainer();
        }
    }

    public ChannelEntry addListener(Container container, ChannelProgram channelProgram) {
        ChannelEntry channelEntry = new ChannelEntry(container, channelProgram);
        listenerList.add(channelEntry);
        if (listenerList.size() == 1) {
            if (hasBroadcaster) {
                hookContainer();
            }
        }
        hasListener = true;
        return channelEntry;
    }

    public ChannelEntry addBroadcaster(Container container, ChannelProgram channelProgram) {
        ChannelEntry channelEntry = new ChannelEntry(container, channelProgram);
        broadcastList.add(channelEntry);
        if (hasListener) {
            channelProgram.hook(container, this);
        }
        hasBroadcaster = true;
        return channelEntry;
    }

    public void removeBroadcaster(Container container, ChannelProgram channelProgram) {
        broadcastList.remove(channelProgram);
        channelProgram.unhook(container, this);
        if (broadcastList.size() == 0) {
            hasBroadcaster = false;
        }
    }

    private void unHookContainer() {
        for (ChannelEntry iteChannelEntry : broadcastList) {
            iteChannelEntry.getChannelProgram().unhook(iteChannelEntry.getContainer(), this);
        }
    }

    private void hookContainer() {
        for (ChannelEntry iteChannelEntry : broadcastList) {
            iteChannelEntry.getChannelProgram().hook(iteChannelEntry.getContainer(), this);
        }
    }

    public String getChannelName() {
        return channelName;
    }
}
