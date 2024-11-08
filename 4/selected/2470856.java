package net.sourceforge.mythtvj.mythtvprotocol;

import java.sql.*;

/**
 * This class defines (or better represents) one channel from mythtv. 
 * It is a stripped down implementation of the channel table in the mythtv database. 
 * Only the most importend Information are used.
 
 * @author jjwin2k
 */
public class Channel {

    private int chanid;

    private int channum;

    private Source source;

    private String callsign;

    private String name;

    private boolean visible;

    private ChannelIcon channelIcon = null;

    /**
     * Creates a Channel-object without a associated channelIcon.
     * @param chanid The (unique) ID of the channel (NOT the channel number)
     * @param channum The channel number under which this channel is found.
     * @param source The source on which the channel can be tuned. 
     * @param callsign The callsign of the channel.
     * @param name The name of the channel.
     * @param visible Should the channel be visible. 
     */
    public Channel(int chanid, int channum, Source source, String callsign, String name, boolean visible) {
        this.chanid = chanid;
        this.channum = channum;
        this.source = source;
        this.callsign = callsign;
        this.name = name;
        this.visible = visible;
    }

    /**
     * Creates a Channel-object with a associated channelIcon.
     * @param chanid The (unique) ID of the channel (NOT the channel number)
     * @param channum The channel number under which this channel is found.
     * @param source The source on which the channel can be tuned. 
     * @param callsign The callsign of the channel.
     * @param name The name of the channel.
     * @param visible Should the channel be visible. 
     * @param channelIcon the Icon of this channel
     */
    public Channel(int chanid, int channum, Source source, String callsign, String name, boolean visible, ChannelIcon channelIcon) {
        this.chanid = chanid;
        this.channum = channum;
        this.source = source;
        this.callsign = callsign;
        this.name = name;
        this.visible = visible;
        this.channelIcon = channelIcon;
    }

    public int getChanid() {
        return chanid;
    }

    public void setChanid(int chanid) {
        this.chanid = chanid;
    }

    public int getChannum() {
        return channum;
    }

    public void setChannum(int channum) {
        this.channum = channum;
    }

    public String getCallsign() {
        return callsign;
    }

    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Source getSource() {
        return source;
    }

    /**
     * 
     * @return null - if the channelIcon wasn't set at the cunstrucion time. 
     *         else the channelIcon
     */
    public ChannelIcon getChannelIcon() {
        return channelIcon;
    }

    public void setSourceid(Source source) {
        this.source = source;
    }

    /**
     * Checks the passed Object for equality. 
     * 
     * @param o
     * @return true If the passed object is also a instance of Channel and both objects have the same chanid 
     * or if both objects have the same source and the same callsign and channum. 
     * @return false else
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!o.getClass().equals(getClass())) return false;
        Channel that = (Channel) o;
        return (this.chanid == that.chanid) || (this.source.equals(that.source) && (this.channum == that.channum && this.callsign.equals(that.callsign)));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + this.chanid;
        hash = 79 * hash + this.channum;
        hash = 79 * hash + (this.source != null ? this.source.hashCode() : 0);
        hash = 79 * hash + (this.callsign != null ? this.callsign.hashCode() : 0);
        hash = 79 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 79 * hash + (this.visible ? 1 : 0);
        return hash;
    }
}
