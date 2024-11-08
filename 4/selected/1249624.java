package de.rentoudu.chat.server.guice;

import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.inject.Provider;

/**
 * Injection helper for a {@link ChannelService}.
 * 
 * @author Florian Sauter
 */
public class ChannelServiceProvider implements Provider<ChannelService> {

    @Override
    public ChannelService get() {
        return ChannelServiceFactory.getChannelService();
    }
}
