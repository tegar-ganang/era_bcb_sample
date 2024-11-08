package de.tud.kom.nat.comm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import de.tud.kom.nat.comm.msg.IMessage;
import de.tud.kom.nat.comm.msg.StayAliveMessage;
import de.tud.kom.nat.comm.obs.IMessageTracer;
import de.tud.kom.nat.comm.obs.MessageTracer;
import de.tud.kom.nat.comm.obs.IMessageTracer.MessageState;
import de.tud.kom.nat.comm.serialization.IMessageSerializer;
import de.tud.kom.nat.comm.serialization.SerializerLoader;
import de.tud.kom.nat.comm.util.SocketFormatter;
import de.tud.kom.nat.util.Logger;

/**
 * The <tt>Commfacade</tt> implements all functionality described in
 * the interface <tt>ICommFacade</tt>.
 * <br><br>
 * This class is basically a facade to ease the work with <tt>java.nio</tt>.
 * It provides methods that create different sockets, register messages which
 * are automatically handled by the given <tt>IMessageHandler</tt> or provide
 * some possiblities to control the used bandwidth of certain sockets.
 *
 * @author Matthias Weinert
 */
public class CommFacade implements ICommFacade, IChannelManager {

    /** message processor */
    private final MessageProcessor msgProc = new MessageProcessor();

    /** socket event handler - encapsulates selector behavior */
    private SocketEventReader eventReader = new SocketEventReader();

    /** message tracer */
    private MessageTracer messageTracer = new MessageTracer();

    /** current message IO system */
    private MessageIO messageIO = new MessageIO(msgProc, eventReader, messageTracer);

    /**
	 * Creates a communication facade.
	 */
    public CommFacade() {
    }

    /**
	 * Returns the message processor.
	 * @return the message processor
	 */
    public IMessageProcessor getMessageProcessor() {
        return msgProc;
    }

    /**
	 * Returns the socket event reader.
	 * @return the SocketEventReader
	 */
    public SocketEventReader getEventReader() {
        return eventReader;
    }

    /**
	 * Opens a TCP server socket on the given port.
	 */
    public ServerSocketChannel openTCPServerSocket(int port) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.socket().bind(new InetSocketAddress(port));
        channel.configureBlocking(false);
        eventReader.registerChannel(channel, channel.validOps(), messageIO);
        return channel;
    }

    /**
	 * Opens a UDP socket which listens to the given port.
	 */
    public DatagramChannel openUDPSocket(int port) throws IOException {
        DatagramChannel udpChan = DatagramChannel.open();
        udpChan.socket().bind(new InetSocketAddress(port));
        udpChan.configureBlocking(false);
        eventReader.registerChannel(udpChan, SelectionKey.OP_READ, messageIO);
        return udpChan;
    }

    /**
	 * Sends a UDP message to the peer which is connected with this channel. This only works if the given
	 * UDP channel is connected, otherwise a <tt>IllegalStateException</tt> will be thrown.
	 * 
	 * @param chan channel to use
	 * @param msg message
	 * @throws IOException
	 */
    public void sendUDPMessage(DatagramChannel chan, IMessage msg) throws IOException {
        if (!chan.isConnected()) {
            throw new IllegalStateException("UDP Channel has to be CONNECTED to use that method!");
        }
        sendUDPMessage(chan, msg, chan.socket().getRemoteSocketAddress());
    }

    /**
	 * Sends a UDP message to the given peer, using the given channel.
	 */
    public void sendUDPMessage(DatagramChannel chan, IMessage msg, SocketAddress addr) throws IOException {
        if (chan == null || msg == null || (addr == null && !chan.isConnected())) throw new IllegalArgumentException("Channel and message must not be null. Address must not be null when channel is unconnected.");
        if (!chan.isOpen()) {
            Logger.logWarning("Channel is closed, it wont send the UDP message " + msg + " to " + addr);
            return;
        }
        if (addr == null) addr = chan.socket().getRemoteSocketAddress();
        IMessageSerializer seri = SerializerLoader.getInstance();
        byte data[] = seri.serializeMessage(msg, false);
        if (data == null) return;
        ByteBuffer bb = ByteBuffer.allocate(4 + data.length);
        bb.position(0);
        bb.asIntBuffer().put(data.length);
        bb.position(4);
        bb.put(data);
        bb.flip();
        if (!(msg instanceof StayAliveMessage)) Logger.log("UDP SEND " + chan.socket().getLocalSocketAddress() + " => " + addr + ": " + msg);
        int send = chan.send(bb, addr);
        if (send != data.length + 4) {
            Logger.logWarning("Did not send all data to " + addr + "(" + msg + ")");
        }
        getMessageTracer().setMessageState(msg.getMessageID(), MessageState.SENT);
    }

    public void sendRawUDPData(DatagramChannel channel, byte[] data) {
        messageIO.writeRaw(data, channel);
    }

    /**
	 * Send a message with a already established tcp channel.
	 * @param chan channel to use
	 * @param msg message to send
	 */
    public void sendTCPMessage(SocketChannel chan, IMessage msg) throws IOException {
        if (this.eventReader.getSelectionKey(chan) == null) throw new IllegalArgumentException("Only registered channels are allowed to send messages!");
        messageIO.writeMessage(msg, chan);
    }

    /**
	 * Sends raw data with the already established tcp channel.
	 * @param chan channel to use
	 * @param data data to send
	 */
    public void sendRawTCPData(SocketChannel chan, byte data[]) {
        messageIO.writeRaw(data, chan);
    }

    /** Starts the selection process (of every socket which is registered). */
    public void startSelectionProcess() {
        eventReader.startSelectionProgress();
    }

    /**
	 * Returns the bandwidth controller which is used to specifiy bandwidth
	 * limitations.
	 */
    public IBandwidthController getBandwidthController() {
        return messageIO;
    }

    /**
	 * Returns the message tracer which provides information about the current
	 * state of a message (queued, sent, acknowlegded).
	 */
    public IMessageTracer getMessageTracer() {
        return messageTracer;
    }

    public SelectionKey registerChannel(SelectableChannel chan) {
        return eventReader.registerChannel(chan, chan.validOps(), messageIO);
    }

    public void unregisterChannel(SelectableChannel channel) {
        eventReader.unregister(channel);
    }

    public SelectionKey setSelectedOps(SelectableChannel chan, int ops) {
        eventReader.setRegisteredOps(chan, ops);
        return eventReader.getSelectionKey(chan);
    }

    /**
	 * Here, a <tt>IRawReader</tt> is registered for a given channel. This disables
	 * the default handing of incoming data (try to creates messages). Now, the incoming
	 * data is just passed to the <tt>rawReader</tt>.<br><br>
	 * Even if a socket has a raw reader registered, it can send both messages and
	 * raw data using the corresponding methods.
	 */
    public void registerRawReader(SelectableChannel channel, IRawReader rawReader) {
        messageIO.registerRawReader(channel, rawReader);
    }

    public void shutdown() {
        eventReader.shutdown();
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
        }
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        Iterator<SelectionKey> it = eventReader.selector.keys().iterator();
        while (it.hasNext()) {
            SelectionKey key = it.next();
            SelectableChannel ch = key.channel();
            sb.append(SocketFormatter.formatChannel(ch)).append("\n");
        }
        return sb.toString();
    }

    public SelectionKey getSelectionKey(SelectableChannel chan) {
        return eventReader.getSelectionKey(chan);
    }

    public IChannelManager getChannelManager() {
        return this;
    }
}
