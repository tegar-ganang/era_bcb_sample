package transport.packet;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import transport.channel.AddChannelCommand;
import transport.channel.ChannelUtils;
import transport.channel.RemoveChannelCommand;
import jegg.EggBase;
import jegg.PortException;
import jegg.UnableToInitializeException;
import jegg.Port;

/**
 * 
 */
public class PacketReader extends EggBase {

    private static final Log LOG = LogFactory.getLog(PacketReader.class);

    private static final ReadPacketsInternalCommand READ_PACKETS = new ReadPacketsInternalCommand();

    private static final long MAX_BLOCK_MSEC = 500;

    private List readyChannels = new Vector();

    private List packetList = new Vector();

    private Selector selector;

    private Port channelListPort;

    public PacketReader() {
        super();
    }

    public void init() throws UnableToInitializeException {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new UnableToInitializeException("PacketReader", e);
        }
        try {
            LOG.debug("Starting to read packets");
            getContext().getPort().send(new ReadPacketsInternalCommand());
        } catch (PortException e1) {
            LOG.error("Unable to initiate packet reading", e1);
            throw new UnableToInitializeException(e1);
        }
    }

    public void handle(Port p) {
        if (LOG.isDebugEnabled()) LOG.debug("handle(" + p + ")");
        channelListPort = p;
        getContext().bindToPort(channelListPort);
    }

    public void handle(Object message) {
        LOG.warn("Unexpected message: " + message);
    }

    /**
     * Accept new socket channel to read packets from.
     * @param ch new channel.
     */
    public void handle(AddChannelCommand add) {
        LOG.info("Got new channel");
        SocketChannel ch = add.getChannel();
        int before = selector.keys().size();
        try {
            ch.configureBlocking(false);
            SelectionKey key = ch.register(selector, SelectionKey.OP_READ);
            key.attach(ch);
        } catch (ClosedChannelException e) {
            getContext().respond(e);
        } catch (IOException e) {
            getContext().respond(e);
        }
        int after = selector.keys().size();
        if (0 == before && 0 < after) {
            try {
                getPort().send(getContext().createMessage(READ_PACKETS));
            } catch (PortException e1) {
                LOG.error("Failed to send READ_PACKETS message", e1);
            }
        }
    }

    /**
     * Handle 'remove channel' event from channel list.
     * @param rmv event holding ID of channel to remove.
     */
    public void handle(RemoveChannelCommand rmv) {
        int id = rmv.getID();
        Set keys = selector.keys();
        for (Iterator it = keys.iterator(); it.hasNext(); ) {
            SelectionKey key = (SelectionKey) it.next();
            SocketChannel ch = (SocketChannel) key.attachment();
            if (ch.hashCode() == id) {
                key.cancel();
                it.remove();
                break;
            }
        }
    }

    /**
     * ReadPackets is used to trigger a check of the
     * channels for packets.
     * @param rp event used to trigger packet read.
     */
    public void handle(ReadPacketsInternalCommand rp) {
        LOG.debug("handle(ReadPacketsInternalCommand");
        if (0 == selector.keys().size()) {
            LOG.debug("No connected clients");
            return;
        }
        try {
            int n = selector.select(MAX_BLOCK_MSEC);
            if (0 < n) {
                readPackets();
            }
        } catch (IOException e) {
            LOG.error("Error in 'select'", e);
        }
        if (0 < selector.keys().size()) {
            try {
                getPort().send(getContext().getCurrentMessage());
            } catch (PortException e1) {
                LOG.error("Failed to send read-packets internal command", e1);
            }
        }
    }

    /**
     * Read packets from channels with data on them.
     */
    private void readPackets() {
        getReadyChannels();
        readReadyChannels();
        publishPackets();
    }

    /**
     * Make list of channels that are ready to be read.
     */
    private void getReadyChannels() {
        readyChannels.clear();
        Set ready = selector.selectedKeys();
        if (0 < ready.size()) {
            for (Iterator it = ready.iterator(); it.hasNext(); ) {
                SelectionKey key = (SelectionKey) it.next();
                readyChannels.add(key.attachment());
                key.cancel();
                it.remove();
            }
            try {
                selector.selectNow();
            } catch (IOException t) {
            }
        }
    }

    /**
     * Read the channels that are ready to be read.
     */
    private void readReadyChannels() {
        packetList.clear();
        for (Iterator it = readyChannels.iterator(); it.hasNext(); ) {
            SocketChannel ch = (SocketChannel) it.next();
            Packet p = null;
            try {
                LOG.debug("Reading channel");
                p = ChannelUtils.readChannel(ch);
            } catch (IOException e) {
                LOG.error("Failed to read channel - dropping: ", e);
                dropChannel(ch);
                continue;
            }
            if (null != p) {
                packetList.add(p);
            }
            try {
                ch.configureBlocking(false);
                SelectionKey key = ch.register(selector, SelectionKey.OP_READ);
                key.attach(ch);
            } catch (ClosedChannelException e) {
                LOG.debug("Channel is closed - dropping: ");
                dropChannel(ch);
            } catch (IOException e) {
                LOG.debug("Channel has error - dropping: ");
                dropChannel(ch);
            }
        }
    }

    /**
     * Drop a channel.  Sends a message to the channel list.
     * @param ch the channel to drop.
     */
    private void dropChannel(SocketChannel ch) {
        try {
            channelListPort.send(getContext().createMessage(new RemoveChannelCommand(ch.hashCode())));
        } catch (PortException e) {
            LOG.error("Failed to send remove-channel command to channel list", e);
        }
    }

    private void publishPackets() {
        if (LOG.isDebugEnabled()) LOG.debug("Publishing " + packetList.size() + " packets");
        if (0 < packetList.size()) {
            for (Iterator it = packetList.iterator(); it.hasNext(); ) {
                Packet p = (Packet) it.next();
                getContext().send(p);
            }
        }
    }
}
