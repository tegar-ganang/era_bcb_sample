package com.flazr.util;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import org.jboss.netty.channel.ExceptionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelUtils {

    private static final Logger logger = LoggerFactory.getLogger(ChannelUtils.class);

    public static void exceptionCaught(final ExceptionEvent e) {
        if (e.getCause() instanceof ClosedChannelException) {
            logger.info("exception: {}", e);
        } else if (e.getCause() instanceof IOException) {
            logger.info("exception: {}", e.getCause().getMessage());
        } else {
            logger.warn("exception: {}", e.getCause());
        }
        if (e.getChannel().isOpen()) {
            e.getChannel().close();
        }
    }
}
