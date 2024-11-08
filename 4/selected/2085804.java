package org.eaiframework.config.mock;

import org.eaiframework.Channel;
import org.eaiframework.ChannelManager;
import org.eaiframework.config.Configuration;
import org.eaiframework.config.ConfigurationException;

/**
 * 
 */
public class MockChannelsConfiguration implements Configuration {

    private ChannelManager channelManager;

    public void configure() throws ConfigurationException {
        Channel channel = new Channel();
        channel.setId("c1");
        channel.setName("c1");
        channel.setType("queue");
        channelManager.createChannel(channel);
    }

    public void destroy() throws ConfigurationException {
        channelManager.destroyAllChannels();
    }

    /**
	 * @return the channelManager
	 */
    public ChannelManager getChannelManager() {
        return channelManager;
    }

    /**
	 * @param channelManager the channelManager to set
	 */
    public void setChannelManager(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }
}
