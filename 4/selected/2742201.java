package org.asteriskjava.manager.action;

/**
 * The ChangeMonitorAction changes the monitoring filename of a channel. It has
 * no effect if the channel is not monitored.<p>
 * It is implemented in <code>res/res_monitor.c</code>
 * 
 * @author srt
 * @version $Id: ChangeMonitorAction.java 938 2007-12-31 03:23:38Z srt $
 */
public class ChangeMonitorAction extends AbstractManagerAction {

    /**
     * Serializable version identifier
     */
    private static final long serialVersionUID = -798530703607827118L;

    private String channel;

    private String file;

    /**
     * Creates a new empty ChangeMonitorAction.
     */
    public ChangeMonitorAction() {
    }

    /**
     * Creates a new ChangeMonitorAction that causes monitoring data for the
     * given channel to be written to the given file(s).
     * 
     * @param channel the name of the channel that is monitored
     * @param file the (base) name of the file(s) to which the voice data is
     *            written
     * @since 0.2
     */
    public ChangeMonitorAction(String channel, String file) {
        this.channel = channel;
        this.file = file;
    }

    /**
     * Returns the name of this action, i.e. "ChangeMonitor".
     */
    @Override
    public String getAction() {
        return "ChangeMonitor";
    }

    /**
     * Returns the name of the monitored channel.
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Sets the name of the monitored channel.<p>
     * This property is mandatory.
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Returns the name of the file to which the voice data is written.
     */
    public String getFile() {
        return file;
    }

    /**
     * Sets the (base) name of the file(s) to which the voice data is written.<p>
     * This property is mandatory.
     */
    public void setFile(String file) {
        this.file = file;
    }
}
