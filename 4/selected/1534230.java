package com.goodow.web.logging.server;

import com.goodow.web.logging.server.servlet.ChannelPresenceSevlet;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.gwt.logging.client.TextLogFormatter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

@Singleton
public class ChannelLoggingHandler extends Handler {

    @Inject
    private static ChannelPresenceSevlet channelPresenceSevlet;

    private static int i;

    private static final Logger logger = Logger.getLogger(ChannelLoggingHandler.class.getName());

    public ChannelLoggingHandler() {
        logger.warning("init " + i);
        setFormatter(new TextLogFormatter(true));
        setLevel(Level.ALL);
    }

    @Override
    public void close() {
    }

    @Override
    public void flush() {
    }

    @Override
    public void publish(final LogRecord record) {
        if (!isSupported() || !isLoggable(record)) {
            return;
        }
        String msg = getFormatter().format(record);
        log(msg);
    }

    private boolean isSupported() {
        return channelPresenceSevlet != null && !channelPresenceSevlet.getConnectedClientIds().isEmpty();
    }

    private void log(final String message) {
        ChannelService channelService = ChannelServiceFactory.getChannelService();
        for (String clientId : channelPresenceSevlet.getConnectedClientIds()) {
            channelService.sendMessage(new ChannelMessage(clientId, message));
        }
    }
}
