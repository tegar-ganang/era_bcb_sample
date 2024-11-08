package org.granite.gravity.gae;

import org.granite.config.GraniteConfig;
import org.granite.config.flex.ServicesConfig;
import org.granite.gravity.Channel;
import org.granite.gravity.DefaultGravity;
import org.granite.gravity.GravityConfig;
import org.granite.gravity.Subscription;
import org.granite.util.UUIDUtil;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import flex.messaging.messages.Message;

/**
 * @author William DRAI
 * @author Franck WOLFF
 */
public class GAEGravity extends DefaultGravity {

    static final String CHANNEL_PREFIX = "org.granite.gravity.gae.channel.";

    private static MemcacheService gaeCache = MemcacheServiceFactory.getMemcacheService();

    public GAEGravity(GravityConfig gravityConfig, ServicesConfig servicesConfig, GraniteConfig graniteConfig) {
        super(gravityConfig, servicesConfig, graniteConfig);
    }

    @Override
    protected Channel createChannel() {
        Channel channel = getGravityConfig().getChannelFactory().newChannel(UUIDUtil.randomUUID());
        Expiration expiration = Expiration.byDeltaMillis((int) getGravityConfig().getChannelIdleTimeoutMillis());
        gaeCache.put(CHANNEL_PREFIX + channel.getId(), channel, expiration);
        gaeCache.put(GAEChannel.MSG_COUNT_PREFIX + channel.getId(), 0L, expiration);
        return channel;
    }

    @Override
    public Channel getChannel(String channelId) {
        if (channelId == null) return null;
        return (Channel) gaeCache.get(CHANNEL_PREFIX + channelId);
    }

    @Override
    public Channel removeChannel(String channelId) {
        if (channelId == null) return null;
        Channel channel = (Channel) gaeCache.get(CHANNEL_PREFIX + channelId);
        if (channel != null) {
            for (Subscription subscription : channel.getSubscriptions()) {
                Message message = subscription.getUnsubscribeMessage();
                handleMessage(message, true);
            }
            channel.destroy();
            gaeCache.delete(CHANNEL_PREFIX + channelId);
            gaeCache.delete(GAEChannel.MSG_COUNT_PREFIX + channelId);
        }
        return channel;
    }

    @Override
    public boolean access(String channelId) {
        return true;
    }

    @Override
    public void internalStart() {
    }

    @Override
    protected void postManage(Channel channel) {
        Expiration expiration = Expiration.byDeltaMillis((int) getGravityConfig().getChannelIdleTimeoutMillis());
        gaeCache.put(CHANNEL_PREFIX + channel.getId(), channel, expiration);
    }
}
