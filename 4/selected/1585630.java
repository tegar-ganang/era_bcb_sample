package com.goodow.web.logging.server.servlet;

import com.google.appengine.api.channel.ChannelPresence;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class ChannelPresenceSevlet extends HttpServlet {

    private Set<String> connectedClientIds = new LinkedHashSet<String>();

    private final Logger logger;

    @Inject
    ChannelPresenceSevlet(final Logger logger) {
        this.logger = logger;
    }

    public Set<String> getConnectedClientIds() {
        return connectedClientIds;
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        ChannelService channelService = ChannelServiceFactory.getChannelService();
        ChannelPresence presence = channelService.parsePresence(req);
        if (presence.isConnected()) {
            connectedClientIds.add(presence.clientId());
        } else {
            connectedClientIds.remove(presence.clientId());
        }
    }
}
