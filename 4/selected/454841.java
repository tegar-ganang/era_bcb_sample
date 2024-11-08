package com.ewansilver.raindrop.handlers;

import com.ewansilver.concurrency.Channel;
import com.ewansilver.concurrency.ChannelFactory;
import com.ewansilver.raindrop.HandlerImpl;

/**
 * A simple EventHandler that simply puts whatever it has received into a
 * Channel so that external apps can access the data.
 * 
 * @author Ewan Silver
 */
public class ChannelHandler extends HandlerImpl {

    private Channel channel;

    /**
	 * Constructor.
	 */
    public ChannelHandler() {
        this(ChannelFactory.instance().getChannel());
    }

    /**
	 * Constructor.
	 * 
	 * @param aChannel
	 *            the channel that will receive the new events.
	 */
    public ChannelHandler(Channel aChannel) {
        channel = aChannel;
    }

    public void handle(Object aTask) {
        try {
            channel.put(aTask);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
	 * The Channel onto which the handled entries will be placed onto.
	 * 
	 * @return
	 */
    public Channel getOutputChannel() {
        return channel;
    }
}
