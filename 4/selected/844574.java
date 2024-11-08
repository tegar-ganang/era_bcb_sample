package org.mortbay.cometd;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.mortbay.log.Log;
import org.mortbay.util.LazyList;
import dojox.cometd.Channel;
import dojox.cometd.Client;
import dojox.cometd.DataFilter;

/** A Bayuex Channel
 * 
 * @author gregw
 *
 */
public class ChannelImpl implements Channel {

    private AbstractBayeux _bayeux;

    private ClientImpl[] _subscribers = new ClientImpl[0];

    private DataFilter[] _dataFilters = new DataFilter[0];

    private ChannelId _id;

    private ConcurrentMap<String, ChannelImpl> _children = new ConcurrentHashMap<String, ChannelImpl>();

    private ChannelImpl _wild;

    private ChannelImpl _wildWild;

    private boolean _persistent;

    ChannelImpl(String id, AbstractBayeux bayeux) {
        _id = new ChannelId(id);
        _bayeux = bayeux;
    }

    public void addChild(ChannelImpl channel) {
        ChannelId child = channel.getChannelId();
        if (!_id.isParentOf(child)) {
            throw new IllegalArgumentException(_id + " not parent of " + child);
        }
        String next = child.getSegment(_id.depth());
        if ((child.depth() - _id.depth()) == 1) {
            ChannelImpl old = _children.putIfAbsent(next, channel);
            if (old != null) throw new IllegalArgumentException("Already Exists");
            if (ChannelId.WILD.equals(next)) _wild = channel; else if (ChannelId.WILDWILD.equals(next)) _wildWild = channel;
        } else {
            ChannelImpl branch = _children.get(next);
            branch = (ChannelImpl) _bayeux.getChannel((_id.depth() == 0 ? "/" : (_id.toString() + "/")) + next, true);
            branch.addChild(channel);
        }
    }

    /**
     * @param filter
     */
    public void addDataFilter(DataFilter filter) {
        synchronized (this) {
            _dataFilters = (DataFilter[]) LazyList.addToArray(_dataFilters, filter, null);
        }
    }

    /**
     * @return
     */
    public ChannelId getChannelId() {
        return _id;
    }

    public ChannelImpl getChild(ChannelId id) {
        String next = id.getSegment(_id.depth());
        if (next == null) return null;
        ChannelImpl channel = _children.get(next);
        if (channel == null || channel.getChannelId().depth() == id.depth()) {
            return channel;
        }
        return channel.getChild(id);
    }

    /**
     * @return
     */
    public String getId() {
        return _id.toString();
    }

    /**
     * @param client The client for which this token will be valid
     * @param subscribe True if this token may be used for subscriptions
     * @param send True if this token may be used for send
     * @param oneTime True if this token may only be used in one request batch.
     * @return A new token that can be used for subcriptions and or sending.
     */
    public String getToken(Client client, boolean subscribe, boolean send, boolean oneTime) {
        String token = Long.toString(_bayeux.getRandom(client.hashCode()), 36);
        return token;
    }

    public boolean isPersistent() {
        return _persistent;
    }

    public void publish(Client fromClient, Object data, String msgId) {
        _bayeux.publish(getChannelId(), fromClient, data, msgId);
    }

    public boolean remove() {
        return _bayeux.removeChannel(getChannelId());
    }

    /**
     * @param filter
     */
    public void removeDataFilter(DataFilter filter) {
        synchronized (this) {
            _dataFilters = (DataFilter[]) LazyList.removeFromArray(_dataFilters, filter);
        }
    }

    public void setPersistent(boolean persistent) {
        _persistent = persistent;
    }

    /**
     * @param client
     */
    public void subscribe(Client client) {
        if (!(client instanceof ClientImpl)) throw new IllegalArgumentException("Client instance not obtained from Bayeux.newClient()");
        ((ClientImpl) client).addSubscription(this);
        synchronized (this) {
            _subscribers = (ClientImpl[]) LazyList.addToArray(_subscribers, client, null);
        }
    }

    public String toString() {
        return _id.toString();
    }

    /**
     * @param client
     */
    public void unsubscribe(Client client) {
        if (!(client instanceof ClientImpl)) throw new IllegalArgumentException("Client instance not obtained from Bayeux.newClient()");
        ((ClientImpl) client).addSubscription(this);
        synchronized (this) {
            _subscribers = (ClientImpl[]) LazyList.removeFromArray(_subscribers, client);
            if (!_persistent && _subscribers.length == 0 && _children.size() == 0) remove();
        }
    }

    protected void publish(ChannelId to, Client from, Map<String, Object> msg) {
        int tail = to.depth() - _id.depth();
        Object data = msg.get(AbstractBayeux.DATA_FIELD);
        Object old = data;
        DataFilter[] filters = null;
        try {
            switch(tail) {
                case 0:
                    {
                        synchronized (this) {
                            filters = _dataFilters;
                        }
                        for (DataFilter filter : filters) data = filter.filter(from, this, data);
                    }
                    break;
                case 1:
                    if (_wild != null) {
                        synchronized (_wild) {
                            filters = _wild._dataFilters;
                        }
                        for (DataFilter filter : filters) data = filter.filter(from, this, data);
                    }
                default:
                    if (_wildWild != null) {
                        synchronized (_wildWild) {
                            filters = _wildWild._dataFilters;
                        }
                        for (DataFilter filter : filters) {
                            data = filter.filter(from, this, data);
                        }
                    }
            }
        } catch (IllegalStateException e) {
            Log.debug(e);
            return;
        }
        if (data != old) msg.put(AbstractBayeux.DATA_FIELD, data);
        boolean delivered = false;
        ClientImpl[] subscribers;
        switch(tail) {
            case 0:
                synchronized (this) {
                    subscribers = _subscribers;
                }
                for (ClientImpl client : subscribers) {
                    client.deliver(from, msg);
                    delivered = true;
                }
                break;
            case 1:
                if (_wild != null) {
                    synchronized (_wild) {
                        subscribers = _wild._subscribers;
                    }
                    for (ClientImpl client : subscribers) {
                        client.deliver(from, msg);
                        delivered = true;
                    }
                }
            default:
                {
                    if (_wildWild != null) {
                        synchronized (_wildWild) {
                            subscribers = _wildWild._subscribers;
                        }
                        for (ClientImpl client : subscribers) {
                            client.deliver(from, msg);
                            delivered = true;
                        }
                    }
                    String next = to.getSegment(_id.depth());
                    ChannelImpl channel = _children.get(next);
                    if (channel != null) {
                        if (delivered) {
                            msg = new HashMap<String, Object>(msg);
                        }
                        channel.publish(to, from, msg);
                    }
                }
        }
    }
}
