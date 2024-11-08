package com.servengine.contentmanager.rss;

import de.nava.informa.impl.basic.*;
import de.nava.informa.core.*;
import de.nava.informa.parsers.FeedParser;
import java.net.URL;

public class Item {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(Item.class.getName());

    ItemIF item;

    String wfwCommentRSS;

    public Item(ItemIF item) throws java.io.IOException {
        this.item = item;
        wfwCommentRSS = item.getElementValue("wfw:commentRSS");
    }

    public Item(Channel channel, String title, String description, URL link, String content) {
        item = new de.nava.informa.impl.basic.Item(channel.getChannelif(), title, description, link);
    }

    public boolean equals(Object o) {
        return ((Item) o).getItemif().getId() == item.getId();
    }

    public ItemIF getItemif() {
        return item;
    }

    public long getId() {
        return item.getId();
    }

    public String getDescription() {
        return item.getDescription();
    }

    public String getSubject() {
        return item.getSubject();
    }

    public String getContent() {
        String content = item.getElementValue("content:content");
        return content == null ? item.getElementValue("content:encoded") : content;
    }

    public java.net.URL getLink() {
        return item.getLink();
    }

    public String getTitle() {
        return item.getTitle();
    }

    public java.util.Date getDate() {
        return item.getDate();
    }

    public java.util.Collection<Item> getComments() throws java.io.IOException {
        try {
            if (wfwCommentRSS != null) return new Channel(FeedParser.parse(new ChannelBuilder(), item.getElementValue("wfw:commentRSS"))).getItems();
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
