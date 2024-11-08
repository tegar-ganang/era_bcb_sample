package org.asteriskjava.manager.event;

/**
 * A MusicOnHoldEvent is triggered when music on hold starts or stops on a channel.<p>
 * It is implemented in <code>res/res_musiconhold.c</code>.<p>
 * Available since Asterisk 1.6
 *
 * @author srt
 * @version $Id: MusicOnHoldEvent.java 1135 2008-08-18 13:46:59Z srt $
 * @since 1.0.0
 */
public class MusicOnHoldEvent extends ManagerEvent {

    private static final long serialVersionUID = 0L;

    public static final String STATE_START = "Start";

    public static final String STATE_STOP = "Stop";

    private String channel;

    private String uniqueId;

    private String state;

    public MusicOnHoldEvent(Object source) {
        super(source);
    }

    /**
     * Returns the name of the channel.
     *
     * @return channel the name of the channel.
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Sets the name of the channel.
     *
     * @param channel the name of the channel.
     */
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

    /**
     * Sets the unique id of the channel.
     *
     * @param uniqueId the unique id of the channel.
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * Returns the state. This is either "Start" or "Stop" depending on whether music on hold
     * started or stopped on the channel.
     *
     * @return "Start" if music on hold started or "Stop" if music on hold stopped on the channel.
     * @see #STATE_START
     * @see #STATE_STOP
     * @see #isStart()
     * @see #isStop()
     */
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    /**
     * Returns whether this is a start event.
     *
     * @return <code>true</code> if this a start event, <code>false</code> otherwise.
     */
    public boolean isStart() {
        return STATE_START.equals(state);
    }

    /**
     * Returns whether this is a stop event.
     *
     * @return <code>true</code> if this an stop event, <code>false</code> otherwise.
     */
    public boolean isStop() {
        return STATE_STOP.equals(state);
    }
}
