package chipchat;

import java.util.HashMap;

/**
 * @author Mr.Lee
 */
public final class ChipChat {

    /** Instance of this object. */
    private static ChipChat instance;

    /**
	 * Get instance.
	 * @return Instance
	 */
    public static ChipChat getInstance() {
        if (instance == null) {
            makeInstance();
        }
        return instance;
    }

    /**
	 * Make just one instance.
	 */
    static synchronized void makeInstance() {
        if (instance == null) {
            instance = new ChipChat();
            Server.initialize();
        }
    }

    /**
	 * Private constuctor.
	 */
    private ChipChat() {
    }

    /** Channels */
    private HashMap channels = new HashMap();

    /**
	 * Get or make channel.
	 * @param name Name of channel.
	 * @return channel
	 */
    public Channel getChannel(final String name) {
        Channel channel;
        synchronized (channels) {
            channel = (Channel) channels.get(name);
            if (channel == null) {
                channel = new Channel();
                channels.put(name, channel);
            }
        }
        return channel;
    }
}
