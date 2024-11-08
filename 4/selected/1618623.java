package com.gerodp.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import com.gerodp.model.newspaper.Headline;
import com.gerodp.model.newspaper.Newspaper;
import com.sun.cnpi.rss.elements.Item;
import com.sun.cnpi.rss.elements.Rss;
import com.sun.cnpi.rss.parser.RssParser;
import com.sun.cnpi.rss.parser.RssParserException;
import com.sun.cnpi.rss.parser.RssParserFactory;

public class RSSUtil {

    private static RssParser parser;

    static {
        try {
            parser = RssParserFactory.createDefault();
        } catch (Throwable ex) {
            System.err.println("Initial RSSParser creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static RssParser getParser() {
        return parser;
    }

    public static void updateNewspaper(Newspaper newspaper) throws MalformedURLException, RssParserException, IOException, Exception {
        Rss rss = parser.parse(new URL(newspaper.getAddress()));
        newspaper.setName(rss.getChannel().getTitle().toString());
        Collection items = rss.getChannel().getItems();
        if (items != null && !items.isEmpty()) {
            for (Iterator i = items.iterator(); i.hasNext(); ) {
                Item item = (Item) i.next();
                Headline headline = new Headline(item.getTitle().toString(), item.getLink().toString(), item.getDescription().toString());
                newspaper.getHeadlines().getHeadlines().add(headline);
            }
        }
    }
}
