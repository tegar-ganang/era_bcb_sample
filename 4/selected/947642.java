package com.ewansilver.raindrop.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Hashtable;
import java.util.Map;
import com.ewansilver.nio.DataHolder;
import com.ewansilver.raindrop.HandlerImpl;

/**
 * This is the base class for all packet parsing routines. It expects to receive
 * DataReceiver objects and builds up a collection of data for each connection.
 * 
 * @author Ewan Silver
 */
public class PacketParsingEventHandler extends HandlerImpl {

    private Map connections;

    /**
	 * Constructor.
	 *
	 */
    public PacketParsingEventHandler() {
        super();
        connections = new Hashtable();
    }

    /**
     * Get the OutputStream for the DataHolder.
	 * @param aDataReceiver
	 * @return a ByteArrayOutputStream.
	 * @throws IOException
	 */
    protected ByteArrayOutputStream getStream(DataHolder aDataReceiver) throws IOException {
        ByteArrayOutputStream stream = (ByteArrayOutputStream) connections.get(aDataReceiver.getChannel());
        if (stream == null) {
            stream = new ByteArrayOutputStream();
            updateStream(aDataReceiver.getChannel(), stream);
        }
        stream.write(aDataReceiver.getData());
        return stream;
    }

    /**
	 * Update stream.
	 * 
	 * @param aChannel
	 * @param aStream
	 */
    protected void updateStream(SocketChannel aChannel, ByteArrayOutputStream aStream) {
        connections.put(aChannel, aStream);
    }

    public void handle(Object aTask) {
        DataHolder dataReceiver = (DataHolder) aTask;
        try {
            ByteArrayOutputStream stream = getStream(dataReceiver);
            parse(stream.toByteArray(), dataReceiver.getChannel());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Parse the date.
	 * @param someData the data to be parsed.
	 * @param aChannel the socket channel that any response should be put on.
	 */
    protected void parse(byte[] someData, SocketChannel aChannel) {
        System.out.println("NIOInputStageEventHandler parse");
        System.out.println(new String(someData));
    }
}
