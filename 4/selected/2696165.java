package com.ewansilver.nio;

import java.nio.channels.SocketChannel;

/**
 * A simple wrapper class to hold the bytes that have been received and a
 * pointer to the Channnel that they came from.
 * 
 * @author ewan
 */
public class DataHolder {

    private final SocketChannel channel;

    private final byte[] data;

    /**
	 * Constructor
	 * 
	 * @param aChannel the Channel that the data came from.
	 * @param someData the data that has arrived.
	 */
    public DataHolder(SocketChannel aChannel, byte[] someData) {
        channel = aChannel;
        data = someData;
    }

    /**
	 * The SocketChannel that delivered the data.
	 * 
	 * @return the SocketChannel
	 */
    public SocketChannel getChannel() {
        return channel;
    }

    /**
	 * The data that has arrived.
	 * 
	 * @return the new data.
	 */
    public byte[] getData() {
        return data;
    }
}
