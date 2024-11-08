package src;

import com.sun.cnpi.rss.elements.Category;
import com.sun.cnpi.rss.elements.Item;
import com.sun.cnpi.rss.elements.Rss;
import com.sun.cnpi.rss.parser.RssParser;
import com.sun.cnpi.rss.parser.RssParserImpl;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

public class RssReader {

    public RssReader() {
    }

    public List verifyRss(String feed) throws Exception {
        RssParser parser = new RssParserImpl();
        Rss rss = parser.parse(new URL(feed));
        Collection items = rss.getChannel().getItems();
        List rssItem = new ArrayList();
        if (items != null && !items.isEmpty()) {
            for (Iterator i = items.iterator(); i.hasNext(); ) {
                Item item = (Item) i.next();
                rssItem.add(item.getTitle());
                rssItem.add(item.getLink());
                rssItem.add(item.getDescription());
            }
        }
        return rssItem;
    }
}
