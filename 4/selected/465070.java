package de.nava.informa.impl.hibernate;

import de.nava.informa.core.ChannelSubscriptionIF;
import de.nava.informa.core.ChannelIF;

/**
 * Hibernate implementation of the ChannelSubscriptionIF interface.
 * 
 * @author Niko Schmuck (niko@nava.de)
 */
public class ChannelSubscription implements ChannelSubscriptionIF {

    private static final long serialVersionUID = -4767438264503641819L;

    private long id = -1;

    private ChannelIF channel;

    private boolean active;

    private int updateInterval;

    public ChannelSubscription() {
        this(null);
    }

    /**
   * Default constructor sets to an inactive channel (with an update
   * interval of 3 hours, used when activated).
   */
    public ChannelSubscription(ChannelIF channel) {
        this(channel, false, 3 * 60 * 60);
    }

    public ChannelSubscription(ChannelIF channel, boolean active, int interval) {
        this.channel = channel;
        this.active = active;
        this.updateInterval = interval;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ChannelIF getChannel() {
        return channel;
    }

    public void setChannel(ChannelIF channel) {
        this.channel = channel;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int interval) {
        this.updateInterval = interval;
    }
}
