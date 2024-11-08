package cn.ourpk.robot.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import net.sourceforge.rssowl.dao.NewsfeedFactoryException;
import net.sourceforge.rssowl.dao.feedparser.FeedParser;
import net.sourceforge.rssowl.model.Channel;
import net.sourceforge.rssowl.model.NewsItem;
import net.sourceforge.rssowl.util.DateParser;
import net.sourceforge.rssowl.util.shop.XMLShop;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class Rssloader {

    private String url;

    private Channel channel;

    public Rssloader(String url) {
        this.url = url;
    }

    public String getChannelTitle() {
        if (channel != null) return channel.getTitle();
        return "";
    }

    /**
	 * 
	 * @return which are sorted by date
	 */
    public NewsItem[] getItems() {
        Hashtable<String, NewsItem> items = channel.getItems();
        NewsItem[] news = (NewsItem[]) items.values().toArray(new NewsItem[0]);
        Arrays.sort(news, new Comparator<NewsItem>() {

            public int compare(NewsItem o1, NewsItem o2) {
                if (o1 == o2) return 0; else {
                    Date pub1 = DateParser.getDate(o1.getPubDate());
                    Date pub2 = DateParser.getDate(o2.getPubDate());
                    return pub2.compareTo(pub1);
                }
            }
        });
        return news;
    }

    public void load() throws IOException, JDOMException, NewsfeedFactoryException {
        URL targetUrl = new URL(url);
        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        XMLShop.setDefaultEntityResolver(builder);
        Document document = builder.build(targetUrl.openStream());
        FeedParser parser = new FeedParser(document, targetUrl.toExternalForm());
        parser.parse();
        channel = parser.getChannel();
    }
}
