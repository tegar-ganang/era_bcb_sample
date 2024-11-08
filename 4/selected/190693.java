package transport.channel;

import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import jegg.EggBase;
import jegg.impl.Dispatcher;
import jegg.impl.EggShell;

/**
 * 
 */
public class ChannelList extends EggBase {

    private static final Log LOG = LogFactory.getLog(ChannelList.class);

    private Collection channels = new HashSet();

    public ChannelList() {
        super();
    }

    public void init() {
    }

    public void handle(Object message) {
        LOG.warn("Unexpected message: " + message);
    }

    public void handle(AddChannelCommand add) {
        LOG.info("Received new channel");
        SocketChannel ch = add.getChannel();
        channels.add(ch);
        getContext().send(add);
    }

    public void handle(GetChannelCommand get) {
        int id = get.getID();
        if (LOG.isDebugEnabled()) LOG.debug("Getting channel " + id);
        SocketChannel channel = findChannel(id);
        if (null != channel) {
            getContext().respond(new GetChannelResponse(id, channel));
        } else {
            getContext().respond(new NoSuchChannelException(id));
        }
    }

    private SocketChannel findChannel(int id) {
        if (LOG.isDebugEnabled()) LOG.debug("Finding channel " + id);
        SocketChannel channel = null;
        for (Iterator it = channels.iterator(); it.hasNext(); ) {
            SocketChannel ch = (SocketChannel) it.next();
            if (ch.hashCode() == id) {
                channel = ch;
                break;
            }
        }
        return channel;
    }
}
