package org.actioncenters.cometd.cache.channel;

import java.util.List;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import org.actioncenters.core.contribution.data.IContribution;
import org.actioncenters.core.spring.ApplicationContextHelper;
import org.springframework.context.ApplicationContext;

/**
 * Channel cache controller.
 *
 * @author dougk
 */
public class ChannelCacheController {

    /** This is a utility class; hide the default constructor */
    private ChannelCacheController() {
    }

    /** The application configuration context. */
    private static ApplicationContext ac = ApplicationContextHelper.getApplicationContext("actioncenters.xml");

    /** The channel cache. */
    private static BlockingCache channelCache = (BlockingCache) (ac.getBean("channelCache"));

    static {
        channelCache.getCacheEventNotificationService().registerListener(new ChannelCacheEventListener());
    }

    /**
     * Gets the channel cache.
     *
     * @return the channelCache
     */
    public static BlockingCache getChannelCache() {
        return channelCache;
    }

    /**
     * Gets the contribution list for the given channelName.
     *
     * @param channelName the channel name
     * @return the contribution list
     */
    @SuppressWarnings("unchecked")
    public static List<IContribution> getContributionList(String channelName) {
        return (List<IContribution>) getChannelCache().get(channelName).getObjectValue();
    }

    /**
     * Checks if key is in cache.
     *
     * @param channelName the channel name
     * @return true, if key is in cache
     */
    public static boolean isKeyInCache(String channelName) {
        return getChannelCache().isKeyInCache(channelName);
    }
}
