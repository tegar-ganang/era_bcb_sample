package com.sin.server;

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.appengine.api.channel.ChannelPresence;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class Channel_connected extends HttpServlet {

    private static final Logger log = Logger.getLogger(Channel_connected.class.getName());

    private Injector injector;

    private Channel_connectedManager channel_connectedManager;

    private String id;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ChannelService channelService = ChannelServiceFactory.getChannelService();
        ChannelPresence presence = channelService.parsePresence(req);
        id = presence.clientId();
        injector = Guice.createInjector(new GuiceModule());
        channel_connectedManager = injector.getInstance(Channel_connectedManager.class);
        if (!channel_connectedManager.clientConnected(id)) {
            log.warning("client " + id + " not registered as connected ??!!");
        }
    }
}
