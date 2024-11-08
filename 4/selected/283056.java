package org.checksum.rss.util;

import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.FeedIF;
import de.nava.informa.impl.basic.Item;
import de.nava.informa.utils.FeedManager;
import de.nava.informa.utils.FeedManagerException;

public class RSSUtil {

    private static FeedManager feedManager = new FeedManager("hourly", 1);

    public Item[] getFeed(String uri) throws FeedManagerException {
        FeedIF feed = feedManager.addFeed(uri);
        ChannelIF channel = feed.getChannel();
        Item[] items = (Item[]) channel.getItems().toArray(new Item[0]);
        return items;
    }

    public String getChannelTitle(String uri) throws FeedManagerException {
        FeedIF feed = feedManager.getFeed(uri);
        return feed.getChannel().getTitle();
    }
}
