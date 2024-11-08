package net.jxta.impl.util;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.MessageFilterListener;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Instances of this clas can be registered with an EndpointService
 * to gather statistics about what kind of messages pass through it.
 * <p/>
 * This class is not MT-safe, so make sure you plug it only
 * into one endpoint service.
 *
 * @see net.jxta.endpoint.EndpointService#addIncomingMessageFilterListener
 */
public class EndpointServiceStatsFilter implements MessageFilterListener {

    long lastMessageTime;

    Hashtable channelTrafficTable;

    Hashtable sourceCountTable;

    Hashtable destCountTable;

    public EndpointServiceStatsFilter() {
        channelTrafficTable = new Hashtable();
        sourceCountTable = new Hashtable();
        destCountTable = new Hashtable();
    }

    /**
     * This method is called by the EndpointService to give us a chance
     * to look at the message before it is dispatched to any listeners.
     */
    public Message filterMessage(Message msg, EndpointAddress source, EndpointAddress dest) {
        Message.ElementIterator e = msg.getMessageElements();
        MessageElement el;
        String namespace;
        String name;
        while (e.hasNext()) {
            el = e.next();
            namespace = e.getNamespace();
            name = el.getElementName();
            incrementCount(channelTrafficTable, source.getProtocolName() + "://" + source.getProtocolAddress() + "/" + namespace, (int) el.getByteLength());
            incrementCount(channelTrafficTable, source.getProtocolName() + "://" + source.getProtocolAddress() + "/" + name, (int) el.getByteLength());
        }
        if (source != null) {
            incrementCount(sourceCountTable, source, 1);
        }
        if (dest != null) {
            incrementCount(destCountTable, dest, 1);
        }
        lastMessageTime = System.currentTimeMillis();
        return msg;
    }

    /**
     * Get the time we last saw a message.
     *
     * @return time last message was received, in milliseconds,
     *         since Jan. 1, 1970.
     */
    public long getLastMessageTime() {
        return lastMessageTime;
    }

    /**
     * Get the number of messages seen with a given message element
     * namespace or full message element name.  (Both are referred
     * to as "channel" here because filters and listeners are
     * dispatched by the EndpointService based on message element
     * namespaces or fully name.)
     */
    public long getTrafficOnChannel(String channel) {
        return getCount(channelTrafficTable, channel);
    }

    public Enumeration getChannelNames() {
        return channelTrafficTable.keys();
    }

    /**
     * Get the number of messages received from a given address.
     */
    public long getMessageCountFrom(EndpointAddress addr) {
        return getCount(sourceCountTable, addr);
    }

    /**
     * Get the number of messages we've seen that were adderssed
     * to a given address.
     */
    public long getMessageCountTo(EndpointAddress addr) {
        return getCount(destCountTable, addr);
    }

    private long getCount(Hashtable table, Object key) {
        Counter counter = (Counter) table.get(key);
        return counter == null ? -1 : counter.value;
    }

    private void incrementCount(Hashtable table, Object key, int incr) {
        Counter counter = (Counter) table.get(key);
        if (counter == null) {
            counter = new Counter();
            table.put(key, counter);
        }
        counter.value += incr;
    }

    private static final class Counter {

        long value;
    }
}
