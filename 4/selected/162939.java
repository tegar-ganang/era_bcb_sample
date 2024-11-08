package rss;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import org.apache.commons.io.IOUtils;
import podcastbyphone.client.Podcast;

public class RSS {

    private Channel channel;

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel c) {
        this.channel = c;
    }

    public Enclosure getFirstEnclosure() {
        Channel c = this.getChannel();
        if (c != null) {
            List<Item> items = c.getItems();
            if (items.size() > 0) {
                Enclosure enc = items.get(0).getEnclosure();
                return enc;
            }
        }
        return null;
    }

    public Item getFirstItem() {
        Channel c = this.getChannel();
        if (c != null) {
            List<Item> items = c.getItems();
            return items.get(0);
        }
        return null;
    }

    public static RSS loadFromUrl(String rssUrl) {
        com.thoughtworks.xstream.XStream xstream = XStreamFactory.createXStream();
        try {
            URL u = new URL(rssUrl);
            URLConnection conn = u.openConnection();
            InputStream istream = conn.getInputStream();
            String xml = IOUtils.toString(istream);
            System.out.println(xml);
            return (RSS) xstream.fromXML(xml);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) {
        String url = Podcast.NPR_TECHNOLOGY.getUrl();
        System.out.println(url);
        RSS rss = RSS.loadFromUrl(url);
        System.out.println(rss);
        Item item = rss.getFirstItem();
        System.out.println("duration: " + item.getDuration());
        System.out.println("duration in seconds: " + item.getDurationInSeconds());
        System.out.println(rss.getFirstEnclosure().getUrl());
    }
}
