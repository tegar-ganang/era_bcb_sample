package de.jochenbrissier.backyard;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

/**
 * Channelhandler contains the channels an manages them
 * @author jochenbrissier
 *
 */
@Singleton
public class ChannelHandler {

    Log log = LogFactory.getLog(ChannelHandler.class);

    Collection<Channel> channels;

    /**
	 * adds a channel
	 * 
	 * @param cn
	 */
    Injector in;

    long channel_it = 0;

    @Inject
    public ChannelHandler(Injector in) {
        log.debug("ChannelHandler init");
        channels = new ArrayList<Channel>();
        this.in = in;
        init();
    }

    public long getNextChannelId() {
        return channel_it++;
    }

    public void addChannel(Channel cn) {
        log.debug("Add Channel: " + cn.getChannelName());
        cn.setChannelId(getNextChannelId());
        synchronized (channels) {
            channels.add(cn);
        }
    }

    /**
	 * removes a channel
	 */
    public void removeChannel(Channel cn) {
        log.debug("Remove Channel" + cn.getChannelName());
        synchronized (channels) {
            channels.remove(cn);
        }
    }

    public Channel getChannel(long id) {
        log.debug("get Chanel");
        synchronized (channels) {
            for (Channel cn : channels) {
                if (cn.getChannelId() == id) {
                    log.debug("return channel" + id);
                    return cn;
                }
            }
        }
        log.debug("cannel not found create a new one");
        Channel ch = in.getInstance(Channel.class);
        ch.setChannelName(id + "");
        ch.setChannelId(id);
        this.addChannel(ch);
        return ch;
    }

    public Channel getChannel(String name) {
        log.debug("get Chanel");
        synchronized (channels) {
            for (Channel cn : channels) {
                if (cn != null && cn.getChannelName().equals(name)) {
                    log.debug("return channel" + name);
                    return cn;
                }
            }
        }
        log.debug("cannel not found create a new one");
        Channel ch = in.getInstance(Channel.class);
        ch.setChannelName(name);
        this.addChannel(ch);
        return ch;
    }

    public void init() {
        creatMetaChannel();
    }

    private void creatMetaChannel() {
        Channel ch = in.getInstance(Channel.class);
        ch.setChannelId(0);
        ch.setChannelName("Meta");
        this.addChannel(ch);
    }
}
