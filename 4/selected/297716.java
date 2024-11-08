package org.asteriskjava.manager.event;

/**
 * Abstract base class for monitoring events.<p>
 * Monitoring events are implemented in <code>res/res_monitor.c</code>
 *
 * @author srt
 * @version $Id: AbstractMonitorEvent.java 1059 2008-05-20 01:09:56Z srt $
 * @since 1.0.0
 */
public abstract class AbstractMonitorEvent extends ManagerEvent {

    private String channel;

    private String uniqueId;

    /**
     * @param source
     */
    protected AbstractMonitorEvent(Object source) {
        super(source);
    }

    /**
     * Returns the name of the channel.
     *
     * @return the name of the channel.
     */
    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Returns the unique id of the channel.
     *
     * @return the unique id of the channel.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
}
