package be.roam.drest.xml.rss;

import java.util.List;

/**
 * Rss 2.0 feed object.
 * 
 * @author Kevin Wetzels
 * @version 1.0
 */
public abstract class Rss20Feed<ChannelType extends Rss20Channel<? extends Rss20Item>> {

    private List<ChannelType> channelList;

    /**
     * @return the channelList
     */
    public List<ChannelType> getChannelList() {
        return channelList;
    }

    /**
     * @param channelList the channelList to set
     */
    public void setChannelList(List<ChannelType> channelList) {
        this.channelList = channelList;
    }

    public abstract void addChannel(ChannelType channel);
}
