package org.asteriskjava.manager.action;

/**
 * The StopMonitorAction ends monitoring (recording) a channel.<p>
 * It is implemented in <code>res/res_monitor.c</code>
 * 
 * @author srt
 * @version $Id: StopMonitorAction.java 938 2007-12-31 03:23:38Z srt $
 */
public class StopMonitorAction extends AbstractManagerAction {

    /**
     * Serializable version identifier
     */
    private static final long serialVersionUID = -6316010713240389305L;

    /**
     * The name of the channel to end monitoring.
     */
    private String channel;

    /**
     * Creates a new empty StopMonitorAction.
     */
    public StopMonitorAction() {
    }

    /**
     * Creates a new StopMonitorAction that ends monitoring of the given
     * channel.
     * 
     * @param channel the name of the channel to stop monitoring.
     * @since 0.2
     */
    public StopMonitorAction(String channel) {
        this.channel = channel;
    }

    /**
     * Returns the name of this action, i.e. "StopMonitor".
     * 
     * @return the name of this action.
     */
    @Override
    public String getAction() {
        return "StopMonitor";
    }

    /**
     * Returns the name of the channel to end monitoring.
     * 
     * @return the name of the channel to end monitoring.
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Sets the name of the channel to end monitoring.<p>
     * This property is mandatory.
     * 
     * @param channel the name of the channel to end monitoring.
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }
}
