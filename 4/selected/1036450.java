package org.wportal.rss;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wportal.rss.parse.RSSChannel;
import org.wportal.rss.dao.ChannelDao;
import java.util.List;

/**
 * @author early
 * @version $Revision: 1.1 $
 */
public class RssUpdator {

    public static final Log logger = LogFactory.getLog(RssUpdator.class);

    private ChannelDao channelDao;

    public ChannelDao getChannelDao() {
        return channelDao;
    }

    public void setChannelDao(ChannelDao channelDao) {
        this.channelDao = channelDao;
    }

    /**
     * Update the rss channels.
     */
    public void update() {
        logger.info("Fetching RSS.");
        List feeds = channelDao.getAllChannels();
        for (int i = 0; i < feeds.size(); i++) {
            RSSChannel channel = (RSSChannel) feeds.get(i);
            try {
                channelDao.updateChannel(channel);
            } catch (Exception e) {
                logger.error("Ignore malformed URL " + channel.getFeed(), e);
            }
        }
    }
}
