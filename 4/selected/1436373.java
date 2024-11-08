package eu.mpower.framework.sensor.rss.soap;

import de.nava.informa.core.ChannelExporterIF;
import de.nava.informa.core.ImageIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.exporters.RSS_2_0_Exporter;
import de.nava.informa.impl.basic.Image;
import de.nava.informa.impl.basic.Item;
import de.nava.informa.impl.basic.ItemGuid;
import de.nava.informa.utils.ItemComparator;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Grabadora
 */
public class RSS {

    private Hashtable channels;

    private String rssPath;

    public boolean addFeedItem(String titleChannel, String titleItem, String link, String description, Date date) {
        FSAChannel ch = (FSAChannel) getChannels().get(titleChannel);
        if (ch == null) {
            ch = createDeviceChannel(titleChannel, link, description);
        }
        if (ch != null) {
            try {
                if (ch.getItems().size() >= ch.getMaxNumberOfItems()) {
                    deleteLastItem(ch);
                }
                ItemIF item = new Item();
                item.setTitle(titleItem + " <date>" + date.toString() + "</date>");
                item.setCreator("FSA");
                item.setDescription(description);
                if (!link.contentEquals("")) {
                    item.setLink(new URL(link));
                }
                item.setDate(date);
                ItemGuid guid = new ItemGuid(item);
                guid.setId(date.getTime());
                guid.setLocation(date.toString());
                item.setGuid(guid);
                ch.addItem(item);
                ch.setPubDate(date);
                createRSSXML(ch);
                return true;
            } catch (MalformedURLException ex) {
                Logger.getLogger(RSS.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        return false;
    }

    public FSAChannel createDeviceChannel(String title, String link, String description) {
        if (getChannels().containsKey(title)) {
            return null;
        }
        try {
            FSAChannel channel = new FSAChannel();
            channel.setTitle(title);
            channel.setDescription(description);
            if (!link.contentEquals("")) {
                channel.setSite(new URL(link));
            }
            channel.setMaxNumberOfItems(15);
            channel.setUpdateFrequency(1);
            channel.setUpdateFrequency(1);
            channel.setTtl(2);
            getChannels().put(title, channel);
            return channel;
        } catch (MalformedURLException ex) {
            Logger.getLogger(RSS.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public void setRSSPath(String rssPath) {
        this.rssPath = rssPath;
    }

    private Hashtable getChannels() {
        if (channels == null) {
            channels = new Hashtable();
        }
        return channels;
    }

    public boolean createDeviceChannel(String title, String link, String description, String imageURL, String imageTitle, int maxNumberOfItems) {
        try {
            FSAChannel channel = new FSAChannel();
            channel.setTitle(title);
            if (!imageURL.contentEquals("")) {
                ImageIF image = new Image();
                image.setLink(new URL(imageURL));
                image.setTitle(imageTitle);
                channel.setImage(image);
            }
            channel.setDescription(description);
            if (!link.contentEquals("")) {
                channel.setSite(new URL(link));
            }
            channel.setMaxNumberOfItems(maxNumberOfItems);
            channel.setUpdateFrequency(1);
            channel.setUpdateFrequency(1);
            channel.setTtl(2);
            if (getChannels().containsKey(title)) {
                return false;
            }
            getChannels().put(title, channel);
            return true;
        } catch (MalformedURLException ex) {
            Logger.getLogger(RSS.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public boolean addFeedItem(String titleChannel, String titleItem, String link, String description, Date date, String imageURL, String imageAlt) {
        FSAChannel ch = (FSAChannel) getChannels().get(titleChannel);
        if (ch != null) {
            try {
                String image = "";
                if (!imageURL.contentEquals("")) {
                    image = "<img vspace'4' hspace='4' border='1' src='" + imageURL + "' alt='" + imageAlt + "' />";
                    image = "<img hspace=\"4\" vspace=\"4\" border=\"1\" align=\"right\" alt=\"" + imageAlt + "\" src=\"" + imageURL + "\"/>";
                }
                if (ch.getItems().size() >= ch.getMaxNumberOfItems()) {
                    deleteLastItem(ch);
                }
                ItemIF item = new Item();
                item.setTitle(titleItem);
                item.setCreator("FSA");
                item.setDescription(description + " " + image);
                if (!link.contentEquals("")) {
                    item.setLink(new URL(link));
                }
                item.setDate(date);
                ItemGuid guid = new ItemGuid(item);
                guid.setId(date.getTime());
                guid.setLocation(date.toString());
                item.setGuid(guid);
                ch.addItem(item);
                ch.setPubDate(date);
                createRSSXML(ch);
                return true;
            } catch (MalformedURLException ex) {
                Logger.getLogger(RSS.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        return false;
    }

    public static void main(String args[]) throws IOException {
        try {
            RSS feed = new RSS();
            feed.createDeviceChannel("Channel title", "http://www.google.es", "prueba", "", "", 3);
            feed.addFeedItem("Channel title", "1", "http://www.google.es", "desc 1", new Date(System.currentTimeMillis()), "", "");
            Thread.sleep(1000);
            feed.addFeedItem("Channel title", "2", "http://www.google.es", "desc 2", new Date(System.currentTimeMillis()), "", "");
            Thread.sleep(1000);
            feed.addFeedItem("Channel title", "3", "http://www.google.es", "desc 3", new Date(System.currentTimeMillis()), "", "");
            Thread.sleep(1000);
            feed.addFeedItem("Channel title", "4", "http://www.google.es", "desc 4", new Date(System.currentTimeMillis()), "", "");
            Thread.sleep(1000);
            feed.addFeedItem("Channel title", "5", "http://www.google.es", "desc 5", new Date(System.currentTimeMillis()), "", "");
            feed.createRSSXML("Channel title");
        } catch (InterruptedException ex) {
            Logger.getLogger(RSS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void deleteLastItem(FSAChannel channel) {
        Object[] items = channel.getItems().toArray();
        ItemComparator iComparator = new ItemComparator(false);
        ItemIF[] array = new ItemIF[items.length];
        ArrayList a = new ArrayList();
        for (int i = 0; i < items.length; i++) {
            array[i] = (ItemIF) items[i];
            channel.removeItem(array[i]);
        }
        java.util.Arrays.sort(array, iComparator);
        for (int i = 1; i < array.length; i++) {
            channel.addItem(array[i]);
        }
    }

    public void createRSSXML(String titleChannel) {
        FSAChannel ch = (FSAChannel) getChannels().get(titleChannel);
        try {
            ChannelExporterIF exporter = new RSS_2_0_Exporter(titleChannel + ".xml");
            exporter.write(ch);
        } catch (IOException ex) {
            Logger.getLogger(RSS.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

    public void createRSSXML(FSAChannel channel) {
        if (!rssPath.contentEquals("")) {
            try {
                ChannelExporterIF exporter = new RSS_2_0_Exporter(rssPath + "/" + channel.getTitle().replaceAll(" ", "") + ".xml");
                exporter.write(channel);
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }
        }
    }
}
