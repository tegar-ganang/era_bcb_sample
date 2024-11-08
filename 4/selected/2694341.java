package com.servengine.contentmanager.rss;

import de.nava.informa.core.*;
import de.nava.informa.exporters.RSS_2_0_Exporter;
import java.net.URL;
import java.util.*;
import java.io.*;

public class Channel {

    ChannelIF channelif;

    public Channel(ChannelIF channelif) {
        this.channelif = channelif;
    }

    public Channel(String title, java.net.URL homeurl, String description, String language) {
        channelif = new de.nava.informa.impl.basic.Channel(title);
        channelif.setSite(homeurl);
        channelif.setDescription(description);
        channelif.setLanguage(language);
        channelif.setGenerator("ServEngine RSS Library. http://www.servengine.com");
    }

    public ChannelIF getChannelif() {
        return channelif;
    }

    public String getRss() throws IOException {
        Writer writer = new StringWriter();
        writeRSS(writer);
        return writer.toString();
    }

    public void writeRSS(Writer writer) throws IOException {
        new RSS_2_0_Exporter(writer, "UTF-8").write(channelif);
    }

    public List<Item> getItems() throws java.io.IOException {
        if (channelif == null || channelif.getItems() == null) return null;
        List<Item> items = new ArrayList<Item>();
        for (ItemIF item : (Collection<ItemIF>) channelif.getItems()) items.add(new Item(item));
        return items;
    }

    /**
	 * El primer elemento es el 1, siguiendo el rollito de status del forEach
	 * 
	 * @param index
	 * @return
	 */
    public Item getItem(int index) throws java.io.IOException {
        return new Item((de.nava.informa.impl.basic.Item) channelif.getItems().toArray()[index - 1]);
    }

    public void add(Item item) {
        channelif.addItem(item.getItemif());
    }

    public String getTitle() {
        return channelif == null ? null : channelif.getTitle();
    }

    public Date getPubDate() {
        return channelif.getPubDate();
    }

    public String getImgurl() throws IOException {
        Collection<Item> items = getItems();
        if (items != null) {
            Iterator<Item> it = items.iterator();
            while (it.hasNext()) {
                Item item = (Item) it.next();
                String content = item.getContent();
                if (content != null && content.indexOf("<img ") > -1) {
                    String tag = content.substring(content.indexOf("<img "), content.indexOf(">", content.indexOf("<img ")));
                    int fin = tag.indexOf("\"", tag.indexOf("http"));
                    return tag.substring(tag.indexOf("http"), fin);
                }
            }
        }
        return null;
    }

    public URL getLocation() {
        return channelif.getLocation();
    }
}
