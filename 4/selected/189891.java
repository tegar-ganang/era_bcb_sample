package org.eaiframework.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eaiframework.Channel;
import org.eaiframework.LifecycleException;
import org.eaiframework.support.AbstractChannelManager;

/**
 * 
 */
public class ChannelManagerImpl extends AbstractChannelManager {

    private static Log log = LogFactory.getLog(ChannelManagerImpl.class);

    private Map<String, Channel> channels = new HashMap<String, Channel>();

    public void createChannel(Channel channel) throws LifecycleException {
        channelFactory.createChannel(channel);
        channels.put(channel.getId(), channel);
        notifyCreate(channel);
    }

    public void destroyAllChannels() {
        for (Channel channel : channels.values()) {
            channelFactory.destroyChannel(channel);
            notifyDestroy(channel);
        }
        channels.clear();
    }

    public void destroyChannel(Channel channel) throws LifecycleException {
        channelFactory.destroyChannel(channel);
        channels.remove(channel.getId());
        notifyDestroy(channel);
    }

    public Channel getChannel(String id) {
        return channels.get(id);
    }

    public Collection<Channel> getChannels() {
        return channels.values();
    }
}
