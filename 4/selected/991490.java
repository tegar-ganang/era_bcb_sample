package com.jradar.reader;

import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.core.ParseException;
import de.nava.informa.impl.basic.ChannelBuilder;
import de.nava.informa.parsers.FeedParser;
import java.util.List;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;
import java.net.URL;
import java.io.IOException;
import com.jradar.service.TagtheService;

/**
 * Objects of this class present a wrapper which is used to create an rss channel populated
 * with content. Also added some useful methods not presented in ChannelIF.
 * @author Alexander Kirin
 */
public class RssReader {

    private String rssHttp;

    private ChannelIF channel;

    private boolean isTagEnabled;

    public RssReader(String http, boolean isTagEnabled) {
        this.isTagEnabled = isTagEnabled;
        rssHttp = http;
        try {
            URL url = new URL(rssHttp);
            channel = FeedParser.parse(new ChannelBuilder(), url);
            if (isTagEnabled) {
                TagtheService tagServ = new TagtheService(null, TagtheService.TEXT_PARAM);
                tagServ.appendTags(channel);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public RssReader(String http) {
        this(http, false);
    }

    /**
     * parses the rss channel and returns links to item content
     *
     * @return
     */
    public List<URL> getItemsSourceLinks() {
        List<URL> itemLinks = null;
        Set<ItemIF> items = null;
        items = channel.getItems();
        Iterator<ItemIF> it = items.iterator();
        if (items.size() > 0) {
            itemLinks = new ArrayList();
        }
        while (it.hasNext()) {
            ItemIF item = it.next();
            itemLinks.add(item.getLink());
        }
        return itemLinks;
    }

    public String getRssHttp() {
        return rssHttp;
    }

    public ChannelIF getChannel() {
        return channel;
    }
}
