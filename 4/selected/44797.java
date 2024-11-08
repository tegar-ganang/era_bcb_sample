package org.asteriskjava.manager.event;

/**
 * A DNDStateEvent is triggered by the Zap channel driver when a channel enters
 * or leaves DND (do not disturb) state.<p>
 * It is implemented in <code>channels/chan_zap.c</code>.<p>
 * Available since Asterisk 1.2
 * 
 * @author srt
 * @version $Id: DndStateEvent.java 938 2007-12-31 03:23:38Z srt $
 * @since 0.2
 */
public class DndStateEvent extends ManagerEvent {

    /**
     * Serializable version identifier
     */
    static final long serialVersionUID = 5906599407896179295L;

    /**
     * The name of the channel.
     */
    private String channel;

    /**
     * The DND state of the channel.
     */
    private Boolean state;

    /**
     * Creates a new DNDStateEvent.
     * 
     * @param source
     */
    public DndStateEvent(Object source) {
        super(source);
    }

    /**
     * Returns the name of the channel. The channel name is of the form
     * "Zap/&lt;channel number&gt;".
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Sets the name of the channel.
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Returns DND state of the channel.
     * 
     * @return Boolean.TRUE if do not disturb is on, Boolean.FALSE if it is off.
     */
    public Boolean getState() {
        return state;
    }

    /**
     * Sets the DND state of the channel.
     */
    public void setState(Boolean state) {
        this.state = state;
    }
}
