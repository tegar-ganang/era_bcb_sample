package com.ewansilver.raindrop.nio;

import java.nio.channels.SelectableChannel;

/**
 * A <code>ServerSocketContext</code> maintains the state/context information
 * neccessary for various users to work out which ServerSocket they are dealing
 * with.
 * </p>
 * 
 * @author ewan.silver @ gmail.com
 */
public class ServerSocketContext {

    /**
	 * The task queue that events relating to this ServerSocket will be placed
	 * onto.
	 */
    private final String taskQueue;

    /**
	 * The host that this server will be listening on.
	 */
    private final String hostname;

    /**
	 * The port that this server will be listening on.
	 */
    private final int port;

    /**
	 * Specifies whether the conenctions that are accepted by this ServerSocket
	 * will be readable or not be default.
	 */
    private boolean isReadable;

    /**
	 * The SelectableChannel that the ServerSocket is listening on.
	 */
    private SelectableChannel channel;

    /**
	 * Constructor.
	 * 
	 * @param aTaskQueue
	 *            the queue to which events relating to this ServerSocket should
	 *            be delivered.
	 * @param aHostname
	 *            the hostname against which the ServerSocket should be
	 *            listening.
	 * @param aPort
	 *            the port number that the ServerSocket should listen on.
	 * @param isReadableByDefault
	 *            specifies whether newly accepted connections are readable by
	 *            default or not.
	 */
    public ServerSocketContext(String aTaskQueue, String aHostname, int aPort, boolean isReadableByDefault) {
        super();
        taskQueue = aTaskQueue;
        hostname = aHostname;
        port = aPort;
        isReadable = isReadableByDefault;
    }

    /**
	 * Constructor. All accepted connections to this Socket are readable by
	 * default.
	 * 
	 * @param aTaskQueue
	 *            the queue to which events relating to this ServerSocket should
	 *            be delivered.
	 * @param aHostname
	 *            the hostname against which the ServerSocket should be
	 *            listening.
	 * @param aPort
	 *            the port number that the ServerSocket should listen on.
	 */
    public ServerSocketContext(String aTaskQueue, String aHostname, int aPort) {
        this(aTaskQueue, aHostname, aPort, true);
    }

    /**
	 * @return Returns the hostname.
	 */
    public String getHostname() {
        return hostname;
    }

    /**
	 * @return Returns the port.
	 */
    public int getPort() {
        return port;
    }

    /**
	 * @return Returns the taskQueue.
	 */
    public String getTaskQueue() {
        return taskQueue;
    }

    /**
	 * @return Returns the channel.
	 */
    public SelectableChannel getChannel() {
        return channel;
    }

    /**
	 * @param aChannel
	 *            The channel to set.
	 */
    public void setChannel(SelectableChannel aChannel) {
        channel = aChannel;
    }

    /**
	 * Checks to see if newly accepted connections should be readable.
	 * 
	 * @return true if connections should be readable. False otherwise.
	 */
    public boolean areAcceptedConnectionsReadable() {
        return isReadable;
    }
}
