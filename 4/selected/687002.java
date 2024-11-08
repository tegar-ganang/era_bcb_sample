package org.asteriskjava.manager.event;

/**
 * An AlarmEvent is triggered when a Zap channel enters or changes alarm state.<p>
 * It is implemented in <code>channels/chan_zap.c</code>
 * 
 * @author srt
 * @version $Id: AlarmEvent.java 938 2007-12-31 03:23:38Z srt $
 */
public class AlarmEvent extends ManagerEvent {

    /**
     * Serializable version identifier
     */
    private static final long serialVersionUID = 5235245336934457877L;

    private String alarm;

    private Integer channel;

    /**
     * @param source
     */
    public AlarmEvent(Object source) {
        super(source);
    }

    /**
     * Returns the kind of alarm that happened.<p>
     * This may be one of
     * <ul>
     * <li>Red Alarm</li>
     * <li>Yellow Alarm</li>
     * <li>Blue Alarm</li>
     * <li>Recovering</li>
     * <li>Loopback</li>
     * <li>Not Open</li>
     * </ul>
     */
    public String getAlarm() {
        return alarm;
    }

    /**
     * Sets the kind of alarm that happened.
     */
    public void setAlarm(String alarm) {
        this.alarm = alarm;
    }

    /**
     * Returns the number of the channel the alarm occured on.
     */
    public Integer getChannel() {
        return channel;
    }

    /**
     * Sets the number of the channel the alarm occured on.
     */
    public void setChannel(Integer channel) {
        this.channel = channel;
    }
}
