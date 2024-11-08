package de.tud.kom.nat.nattrav.mechanism;

import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;

/**
 * Implements all features of the interface <tt>IConnectionInfo</tt>.
 *
 * @author Matthias Weinert
 * @param <TYPE>
 */
public class ConnectionInfo<TYPE extends SelectableChannel> implements IConnectionInfo<TYPE> {

    /** The channel. */
    private TYPE channel;

    /** The description of the connection details. */
    private String description;

    /** The relay host or null, if none. */
    private InetSocketAddress relayHost;

    /** True when connection has been holepunched. */
    private boolean isHolePunched;

    /** True when connection is trivial [no technique used]. */
    private boolean isTrivial;

    /**
	 * Creates a connection info with all given information.
	 * 
	 * @param channel channel
	 * @param isTrivial trivial flag
	 * @param isHolePunched hole-punched flag
	 * @param relayHost relayhost
	 * @param description description
	 */
    public ConnectionInfo(TYPE channel, boolean isTrivial, boolean isHolePunched, InetSocketAddress relayHost, String description) {
        this.channel = channel;
        this.isTrivial = isTrivial;
        this.isHolePunched = isHolePunched;
        this.relayHost = relayHost;
        this.description = description;
    }

    public TYPE getChannel() {
        return channel;
    }

    public String getDescription() {
        return description;
    }

    public InetSocketAddress getRelayHost() {
        return relayHost;
    }

    public boolean isHolePunched() {
        return isHolePunched;
    }

    public boolean isRelayed() {
        return relayHost != null;
    }

    public boolean isTrivial() {
        return isTrivial;
    }
}
