package org.jeuron.jlightning.connection.manager;

import java.io.Serializable;
import java.nio.channels.Channel;

/**
 * Used to queue interestOps changes to the selection keys.
 *
 * @author Mike Karrys
 * @since 1.0
 * @see SysOps
 * @see AbstractConnectionManager
 */
public class SysOpsChange implements Serializable {

    private static final long serialVersionUID = -5415360590272305846L;

    private int interestOps = 0;

    private Channel channel = null;

    public SysOpsChange() {
    }

    public SysOpsChange(Channel channel, int interestOps) {
        this.channel = channel;
        this.interestOps = interestOps;
    }

    /**
     * Gets the associated {@link Channel} to change SysOps on.
     * @return channel
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Sets the associated {@link Channel} to change SysOps on.
     * @param channel
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
     * Return the interestOps to add to the selection key.
     * @return interestOps returns interestOps
     */
    public int getInterestOps() {
        return interestOps;
    }

    /**
     * Sets the interestOps to add to the selection key.
     * @param interestOps new interestOps
     */
    public void setInterestOps(int interestOps) {
        this.interestOps = interestOps;
    }
}
