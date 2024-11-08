package org.coos.messaging;

import java.util.Vector;
import org.coos.messaging.jmx.ManagedObject;
import org.coos.messaging.jmx.ManagementFactory;

/**
 * The Plugin, A holder class for all information associated with a plugin.
 *
 * @author Knut Eilif Husa, Tellu AS
 */
public class Plugin {

    public static final int DEFAULT_STARTLEVEL = 10;

    private Endpoint endpoint;

    private Vector channels = new Vector();

    private int startLevel = DEFAULT_STARTLEVEL;

    private ManagedObject managedObject = null;

    public String getName() {
        return endpoint.getName();
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
        endpoint.setPlugin(this);
    }

    public String getEndpointState() {
        if (endpoint != null) {
            return endpoint.getEndpointState();
        }
        return null;
    }

    public int getStartLevel() {
        return startLevel;
    }

    public void setStartLevel(int startLevel) {
        this.startLevel = startLevel;
    }

    public void addChannel(Channel channel) {
        channels.addElement(channel);
    }

    public Vector getChannels() {
        return channels;
    }

    public void removeChannel(Channel channel) {
        channels.removeElement(channel);
    }

    /**
     * Connects all channels. Then starts the endpoint.
     * @throws ConnectingException Exception thrown if any of the
     * channels fails in connecting
     */
    public void connect() throws ConnectingException {
        for (int i = 0; i < channels.size(); i++) {
            Channel channel = (Channel) channels.elementAt(i);
            channel.connect(endpoint);
        }
        endpoint.initializeEndpoint();
        managedObject = ManagementFactory.getPlatformManagementService().registerPlugin(this);
    }

    /**
     * Connects the provided Channel. Does NOT start the endpoint
     * @param channel the channel to connect
     * @throws ConnectingException thrown if the channel fails in connecting
     */
    public void connect(Channel channel) throws ConnectingException {
        channel.connect(endpoint);
    }

    /**
     * Shuts down a specific channel. Does not bring down the endpoint
     * @param channel
     */
    public void disconnect(Channel channel) {
        channel.disconnect();
    }

    /**
     * Shuts down all channels connected to this endpoint. Shuts down the
     * endpoint
     */
    public void disconnect() {
        endpoint.shutDownEndpoint();
        for (int i = 0; i < channels.size(); i++) {
            Channel channel = (Channel) channels.elementAt(i);
            channel.disconnect();
        }
        if (managedObject != null) {
            ManagementFactory.getPlatformManagementService().unregister(managedObject);
        }
    }

    public boolean isConnected() {
        boolean connected = true;
        for (int i = 0; i < channels.size(); i++) {
            Channel channel = (Channel) channels.elementAt(i);
            connected = connected && channel.isConnected();
        }
        return connected;
    }
}
