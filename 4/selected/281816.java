package com.ewansilver.concurrency;

/**
 * <p>The ChannelFactory allows us to create <code>Channel</code>s. It is intended
 * to allow us to switch between Channels implemented by the native Java 5 concurrency
 * package and other implementations (eg the Emory backport of the java 5 concurrency package.</p>
 * <p>The default behaviour is to use the native Java 5 concurrency package.
 * </p>
 * @author ewan.silver @ gmail.com
 */
public class ChannelFactory {

    private static ChannelFactory factory;

    /**
	 * Constructor.
	 */
    protected ChannelFactory() {
        super();
    }

    /**
	 * Get the instance of the ChannelFactory.
	 * @return the ChannelFactory instance.
	 */
    public static ChannelFactory instance() {
        synchronized (ChannelFactory.class) {
            if (factory == null) factory = new ChannelFactory();
        }
        return factory;
    }

    /**
	 * Gets a new instance of a <code>Channel</code>.
	 * 
	 * @return a Channel.
	 */
    public Channel getChannel() {
        return new ChannelImpl();
    }
}
