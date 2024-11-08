package com.servengine.contentmanager.rss;

import javax.servlet.*;
import java.util.*;
import de.nava.informa.core.*;
import de.nava.informa.impl.basic.*;
import de.nava.informa.parsers.FeedParser;
import java.net.*;
import java.io.*;

public class RSSCache implements ServletContextListener {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(RSSCache.class.getName());

    HashMap<String, ChannelIF> channels;

    public RSSCache() {
        channels = new HashMap<String, ChannelIF>();
    }

    public void contextInitialized(ServletContextEvent sce) {
        log.info("RSSCache en el contexto de aplicación");
    }

    public void contextDestroyed(ServletContextEvent sce) {
        log.info("RSSCache sacado del contexto de aplicación.");
    }

    public ChannelIF getChannel(String url) throws MalformedURLException, IOException, UnsupportedFormatException, ParseException {
        if (channels.containsKey(url)) {
            ChannelIF channel = channels.get(url);
            if (new java.util.Date().getTime() - channel.getLastUpdated().getTime() < 60 * 60 * 1000) return channel;
        }
        try {
            ChannelIF channel = FeedParser.parse(new ChannelBuilder(), new URL(url));
            channels.put(url, channel);
            return channel;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
