package de.iritgo.openmetix.core.network;

import de.iritgo.openmetix.core.action.Action;
import de.iritgo.openmetix.core.action.NetworkActionProcessorInterface;
import de.iritgo.openmetix.core.base.BaseObject;
import de.iritgo.openmetix.core.iobject.NoSuchIObjectException;
import de.iritgo.openmetix.core.logger.Log;
import de.iritgo.openmetix.core.thread.ThreadService;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * NetworkService
 *
 * @version $Id: NetworkService.java,v 1.1 2005/04/24 18:10:45 grappendorf Exp $
 */
public class NetworkService extends BaseObject {

    /** The thread service. */
    private ThreadService threadService;

    /** The socket. */
    private Socket socket;

    /** All network listeners. */
    private List networkSystemListenerList;

    /** The channel. */
    private Channel channel;

    /** All chanels. */
    private HashMap channelList;

    /** The number of channels. */
    private double numChannels;

    /** The stream organizer. */
    private StreamOrganizer defaultStreamOrganizer;

    /** All objects. */
    private List objects;

    /** The recieve action processor. */
    private NetworkActionProcessorInterface reveiveNetworkActionProcessor;

    /** The send action processor. */
    private NetworkActionProcessorInterface sendNetworkActionProcessor;

    /**
	 * Create a new NetworkService.
	 *
	 * @param threadBase ThreadService, you can get it from the Engine
	 * @param receiveActionProcessor The first action processor that receive actions
	 */
    public NetworkService(ThreadService threadBase, NetworkActionProcessorInterface reveiveNetworkActionProcessor, NetworkActionProcessorInterface sendNetworkActionProcessor) {
        this.threadService = threadBase;
        networkSystemListenerList = new LinkedList();
        channelList = new HashMap();
        numChannels = 0;
        this.reveiveNetworkActionProcessor = reveiveNetworkActionProcessor;
        this.sendNetworkActionProcessor = sendNetworkActionProcessor;
    }

    /**
	 * Add a connected channel.
	 *
	 * @param channel The channel to add.
	 */
    public void addConnectedChannel(Channel channel) {
        channel.setChannelNumber(numChannels);
        synchronized (channelList) {
            channelList.put(String.valueOf(numChannels), channel);
            reveiveNetworkActionProcessor.newChannelCreated(channel);
        }
        threadService.add(channel);
        numChannels++;
        fireConnectionEstablished(channel);
    }

    /**
	 * If an object is received, all processors will be called.
	 *
	 * @param object Most it is a action object.
	 * @param channel The channel that received the object.
	 */
    public synchronized void callReceiveNetworkActionProcessor(Action action, final Channel channel) {
        reveiveNetworkActionProcessor.perform(action, new ClientTransceiver(channel.getChannelNumber(), channel));
    }

    /**
	 * Add a network listener.
	 */
    public void addNetworkSystemListener(NetworkSystemListener listener) {
        synchronized (networkSystemListenerList) {
            networkSystemListenerList.add(listener);
        }
    }

    /**
	 * Called when a network connection was established.
	 *
	 * @param channel The network channel.
	 */
    public void fireConnectionEstablished(Channel channel) {
        for (Iterator i = networkSystemListenerList.iterator(); i.hasNext(); ) {
            ((NetworkSystemListener) i.next()).connectionEstablished(this, channel);
        }
    }

    /**
	 * Called when a network connection was terminated.
	 *
	 * @param channel The network channel.
	 */
    public void fireConnectionTerminated(Channel channel) {
        for (Iterator i = networkSystemListenerList.iterator(); i.hasNext(); ) {
            ((NetworkSystemListener) i.next()).connectionTerminated(this, channel);
        }
        reveiveNetworkActionProcessor.channelClosed(channel);
        sendNetworkActionProcessor.channelClosed(channel);
    }

    /**
	 * Called when a NoSuchIObjectException has occurred.
	 *
	 * @param channel The network channel.
	 */
    public void fireError(Channel channel, NoSuchIObjectException x) {
        for (Iterator i = networkSystemListenerList.iterator(); i.hasNext(); ) {
            ((NetworkSystemListener) i.next()).error(this, channel, x);
        }
    }

    /**
	 * Called when a SocketTimeoutException has occurred.
	 *
	 * @param channel The network channel.
	 */
    public void fireError(Channel channel, SocketTimeoutException x) {
        for (Iterator i = networkSystemListenerList.iterator(); i.hasNext(); ) {
            ((NetworkSystemListener) i.next()).error(this, channel, x);
        }
    }

    /**
	 * Called when a ClassNotFoundException has occurred.
	 *
	 * @param channel The network channel.
	 */
    public void fireError(Channel channel, ClassNotFoundException x) {
        for (Iterator i = networkSystemListenerList.iterator(); i.hasNext(); ) {
            ((NetworkSystemListener) i.next()).error(this, channel, x);
        }
    }

    /**
	 * Called when an EOFException has occurred.
	 *
	 * @param channel The network channel.
	 */
    public void fireError(Channel channel, EOFException x) {
        for (Iterator i = networkSystemListenerList.iterator(); i.hasNext(); ) {
            ((NetworkSystemListener) i.next()).error(this, channel, x);
        }
    }

    /**
	 * Called when a SocketException has occurred.
	 *
	 * @param channel The network channel.
	 */
    public void fireError(Channel channel, SocketException x) {
        for (Iterator i = networkSystemListenerList.iterator(); i.hasNext(); ) {
            ((NetworkSystemListener) i.next()).error(this, channel, x);
        }
    }

    /**
	 * Called when an IOException has occurred.
	 *
	 * @param channel The network channel.
	 */
    public void fireError(Channel channel, IOException x) {
        for (Iterator i = networkSystemListenerList.iterator(); i.hasNext(); ) {
            ((NetworkSystemListener) i.next()).error(this, channel, x);
        }
    }

    /**
	 * Connect with a Server.
	 */
    public double connect(String name, int port, ConnectObserver connectObserver) {
        Log.logDebug("network", "NetworkService.connect", "Connecting to server on port " + port);
        Channel channel = null;
        try {
            socket = null;
            int i = 0;
            while ((socket == null) && (i < 10)) {
                for (int j = 0; j < 10; ++j) {
                    try {
                        Thread.sleep(100);
                        if (connectObserver != null) {
                            connectObserver.notice();
                        }
                    } catch (InterruptedException e) {
                    }
                }
                socket = new Socket(InetAddress.getByName(name), port);
                ++i;
            }
            if (socket == null) {
                return -1;
            }
            channel = new Channel(socket, this);
            addConnectedChannel(channel);
        } catch (Exception e) {
            Log.logError("network", "NetworkService.connect", "Error while connecting to " + name + ":" + port);
            return -1;
        }
        return channel.getChannelNumber();
    }

    /**
	 * Listen on a host and port
	 */
    public void listen(String name, int port, int timeout) {
        Log.logInfo("network", "NetworkService.listen", "Listening on port:" + port);
        try {
            threadService.add(new ChannelFactory(this, name, port, timeout));
        } catch (Exception e) {
        }
    }

    /**
	 * Send an object through a channel.
	 */
    public void send(Object object, double channel) {
        Channel connectedChannel = null;
        synchronized (channelList) {
            connectedChannel = (Channel) channelList.get(String.valueOf(channel));
            if (connectedChannel == null) {
                Log.logFatal("network", "NetworkService.send", "Channel not found: " + channel);
                return;
            }
        }
        connectedChannel.send(object);
    }

    /**
	 * Send an object through all channels.
	 */
    public void sendBroadcast(Object object) {
        synchronized (channelList) {
            Iterator i = channelList.keySet().iterator();
            while (i.hasNext()) {
                ((Channel) channelList.get(i.next())).send(object);
            }
        }
    }

    /**
	 * Close a channel.
	 *
	 * @param channelNumber The channel to close.
	 */
    public void closeChannel(double channelNumber) {
        try {
            synchronized (channelList) {
                Channel connectedChannel = (Channel) channelList.get(String.valueOf(channelNumber));
                if (connectedChannel != null) {
                    connectedChannel.dispose();
                    channelList.remove(String.valueOf(channelNumber));
                    Log.logInfo("network", "NetworkService.closeChannel", "Closing channel " + channelNumber);
                }
            }
        } catch (Exception x) {
            Log.logError("network", "NetworkService.closeChannel", "Error while closing channel " + channelNumber + ": " + x.toString());
        }
    }

    /**
	 * Close all channels.
	 */
    public void closeAllChannel() {
        try {
            synchronized (channelList) {
                for (Iterator i = channelList.values().iterator(); i.hasNext(); ) {
                    Channel connectedChannel = (Channel) i.next();
                    connectedChannel.dispose();
                }
                channelList.clear();
            }
        } catch (Exception x) {
            Log.logError("network", "NetworkService.closeChannel", "Error while closing channel :" + x.toString());
        }
    }

    /**
	 * Get the number of channels.
	 *
	 * @return Numbers of channels.
	 */
    public double getNumChannels() {
        return numChannels;
    }

    /**
	 * Return the Channel of a channel number.
	 */
    public Channel getConnectedChannel(double channelNumber) {
        Channel connectedChannel = null;
        synchronized (channelList) {
            connectedChannel = (Channel) channelList.get(String.valueOf(channelNumber));
        }
        return connectedChannel;
    }

    /**
	 * Flush the buffers.
	 */
    public void flush(double channel) {
        Channel connectedChannel = null;
        synchronized (channelList) {
            connectedChannel = (Channel) channelList.get(String.valueOf(channel));
            if (connectedChannel == null) {
                Log.logFatal("network", "NetworkService.flush", "Channel not found: " + channel);
                return;
            }
        }
        connectedChannel.flush();
    }

    /**
	 * Flush the buffers.
	 */
    public void flushAll() {
        Channel connectedChannel = null;
        synchronized (channelList) {
            for (Iterator i = channelList.values().iterator(); i.hasNext(); ) {
                connectedChannel = (Channel) i.next();
                if (connectedChannel == null) {
                    Log.logFatal("network", "NetworkService.flushAll", "Channel not found: " + channel);
                    continue;
                }
                connectedChannel.flush();
            }
        }
    }

    /**
	 * The stream organizer does the low level protocol for the communication.
	 *
	 * @param s The socket.
	 */
    public StreamOrganizer getDefaultStreamOrganizer(Socket s) throws IOException {
        if (defaultStreamOrganizer != null) {
            return defaultStreamOrganizer.create(s);
        }
        return new ObjectStream(s);
    }

    /**
	 * Set the default stream organizer.
	 *
	 * @param streamOrganizer The stream organizer.
	 */
    public void setDefaultStreamOrganizer(StreamOrganizer streamOrganizer) {
        defaultStreamOrganizer = streamOrganizer;
    }

    /**
	 * Set the a new stream organizer.
	 *
	 * @param streamOrganizer The stream organizer
	 * @param channel The channel number.
	 */
    public void setStreamOrganizer(StreamOrganizer streamOrganizer, double channel) {
        Channel connectedChannel = null;
        try {
            synchronized (channelList) {
                connectedChannel = (Channel) channelList.get(String.valueOf(channel));
                if (connectedChannel == null) {
                    Log.logFatal("network", "NetworkService.setStreamOrganizer", "Channel not found: " + channel);
                    return;
                }
                connectedChannel.setStreamOrganizer(streamOrganizer);
            }
        } catch (IOException x) {
            Log.logFatal("network", "NetworkService.setStreamOrganizer", "Unable to set stream organizer for channel " + channel);
        }
    }
}
