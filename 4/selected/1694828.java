package com.simplerss;

import com.simplerss.dataobject.Channel;
import com.simplerss.handler.RSSHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class FeedFactory {

    private FeedFactory instance;

    private FeedFactory() {
        this.instance = new FeedFactory();
    }

    public FeedFactory getInstance() {
        return instance;
    }

    public Channel getRssFeed(String feedUrl) {
        try {
            return getRssFeed(new URL(feedUrl));
        } catch (Exception e) {
            return null;
        }
    }

    public Channel getRssFeed(URL feedUrl) {
        try {
            Channel channel = getRssFeed(feedUrl.openStream());
            if (channel != null) {
                channel.setOriginatingUrl(feedUrl);
            }
            return channel;
        } catch (Exception e) {
            return null;
        }
    }

    public Channel getRssFeed(InputStream inputStream) {
        Channel theFeed = null;
        try {
            theFeed = retrieveChannel(inputStream);
        } catch (Exception e) {
        }
        return theFeed;
    }

    private Channel retrieveChannel(InputStream inputStream) throws SAXException, IOException {
        XMLReader reader = XMLReaderFactory.createXMLReader();
        RSSHandler rssHandler = new RSSHandler(reader);
        reader.setContentHandler(rssHandler);
        reader.parse(new InputSource(inputStream));
        return rssHandler.getChannel();
    }
}
