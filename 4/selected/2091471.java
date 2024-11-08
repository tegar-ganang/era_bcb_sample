package transport.packet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import transport.channel.AddChannelCommand;
import transport.channel.GetChannelCommand;
import transport.channel.GetChannelResponse;
import transport.channel.RemoveChannelCommand;
import jegg.EggBase;
import jegg.PortException;
import jegg.Port;

/**
 * PacketWriter writes to the network any packets that it receives.  The 
 * channel to write the packet is identified by the channelID associated
 * with the packet (see Packet.getChannelID()).  If the channel ID is 
 * zero, then it is assumed that a new channel should be opened to write
 * the packet to.  The remote address to connect to is assumed, in this
 * case, to be the socket address bound to the 'to' peer in the packet
 * header (see Peer.getAddress()).  When a new channel is opened, it is
 * sent to the ChannelList.
 */
public class PacketWriter extends EggBase {

    private static final Log LOG = LogFactory.getLog(PacketWriter.class);

    private static long nextSerial = System.currentTimeMillis();

    private Port channelListPort;

    private Map channels = new HashMap();

    private Map pending = new HashMap();

    public PacketWriter() {
        super();
    }

    public void init() {
        getContext().bindToPort(channelListPort);
    }

    public void handle(Port p) {
        if (LOG.isDebugEnabled()) LOG.debug("handle(" + p + ")");
        channelListPort = p;
    }

    public void handle(Object message) {
        LOG.warn("Unexpected message: " + message);
    }

    /**
     * Write a packet.
     * @param p the packet to write.
     */
    public void handle(Packet p) {
        LOG.debug("Got packet");
        p.setSerial(nextSerial++);
        int ch_id = p.getChannelID();
        SocketChannel ch = null;
        if (0 >= ch_id) {
            InetAddress ia = p.getIP();
            int port = p.getPort();
            if (null == ia) {
                LOG.error("Unable to send packet " + p.getSerial() + ":  channel ID not set");
            } else {
                try {
                    SocketChannel newChan = SocketChannel.open();
                    newChan.connect(new InetSocketAddress(ia, port));
                    channelListPort.send(getContext().createMessage(new AddChannelCommand(newChan)));
                    writePacket(p, newChan);
                } catch (IOException e) {
                    LOG.error("Unable to send packet", e);
                } catch (PortException e) {
                    LOG.error("Unable to send add-channel command", e);
                }
            }
        } else {
            Object key = new Integer(ch_id);
            ch = (SocketChannel) channels.get(key);
            if (null == ch) {
                Collection list = (Collection) pending.get(key);
                if (null == list) {
                    list = new Vector();
                    pending.put(key, list);
                }
                list.add(p);
                try {
                    channelListPort.send(getContext().createMessage(new GetChannelCommand(ch_id)));
                } catch (PortException e) {
                    LOG.error("Unable to send get-channel command", e);
                }
            } else {
                writePacket(p, ch);
            }
        }
    }

    public void handle(GetChannelResponse r) {
        SocketChannel ch = r.getChannel();
        Integer key = new Integer(ch.hashCode());
        channels.put(key, ch);
        Collection list = (Collection) pending.get(key);
        if (null != list && 0 < list.size()) {
            for (Iterator it = list.iterator(); it.hasNext(); ) {
                handle((Packet) it.next());
            }
        }
    }

    /**
     * This is the response to a 'get-channel' request
     * issued to the channel list for a packet that needs
     * to be published.
     */
    public void handle(SocketChannel ch) {
        Integer channelID = new Integer(ch.hashCode());
        List list = (List) pending.get(channelID);
        if (null != list) {
            writePackets(list, ch);
            pending.remove(channelID);
        }
        channels.put(channelID, ch);
    }

    /**
     * Handle 'remove-channel' event from channel list.
     */
    public void handle(RemoveChannelCommand rmv) {
        cleanupChannel(rmv.getID());
    }

    /**
     * Handle 'add-channel' from channel list.  These are 
     * ignored because we only cache channels as we get packets
     * to write.
     */
    public void handle(AddChannelCommand add) {
    }

    private SocketChannel openNewChannel(SocketAddress addr) throws IOException {
        SocketChannel newChannel = null;
        newChannel = SocketChannel.open();
        Socket sock = newChannel.socket();
        sock.connect(addr);
        return newChannel;
    }

    private void writePackets(List list, SocketChannel ch) {
        boolean dropall = false;
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            Packet p = (Packet) it.next();
            if (!dropall) {
                if (!writePacket(p, ch)) {
                    dropall = true;
                }
            } else {
                LOG.warn("Dropping packet " + p.getSerial() + " due to channel error");
            }
        }
        list.clear();
    }

    private boolean writePacket(Packet p, SocketChannel ch) {
        if (LOG.isDebugEnabled()) LOG.debug("Writing packet #" + p.getSerial() + " to channel " + p.getChannelID());
        try {
            byte[] data = p.toBytes();
            ByteBuffer data_buf = ByteBuffer.allocate(data.length + 4);
            data_buf.clear();
            data_buf.putInt(data.length);
            data_buf.put(data);
            data_buf.flip();
            while (data_buf.hasRemaining()) {
                LOG.debug("Writing data to channel");
                if (0 == ch.write(data_buf)) {
                    throw new IOException("TCP send buffer OVERFLOW on channel " + ch.hashCode());
                }
            }
        } catch (IOException e) {
            LOG.error("Error writing packet " + p.getSerial() + " to channel " + ch.hashCode());
            dropChannel(ch);
            return false;
        }
        return true;
    }

    private void dropChannel(SocketChannel ch) {
        int id = ch.hashCode();
        if (LOG.isInfoEnabled()) LOG.info("Dropping channel " + id);
        cleanupChannel(ch.hashCode());
        try {
            channelListPort.send(getContext().createMessage(new RemoveChannelCommand(id)));
        } catch (PortException e) {
            LOG.error("Unable to send remove-channel command", e);
        }
    }

    private void cleanupChannel(int id) {
        Integer channelID = new Integer(id);
        channels.remove(channelID);
        List list = (List) pending.get(channelID);
        if (null != list) {
            pending.remove(channelID);
            logDroppedPackets(list);
        }
    }

    private void logDroppedPackets(List list) {
        if (LOG.isInfoEnabled()) {
            for (Iterator it = list.iterator(); it.hasNext(); ) {
                Packet p = (Packet) it.next();
                LOG.info("Dropping packet: " + p);
            }
        }
    }
}
