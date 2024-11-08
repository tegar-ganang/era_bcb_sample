package com.simplerss.handler.itunes;

import com.simplerss.dataobject.Channel;
import com.simplerss.dataobject.itunes.ITunesCategory;
import com.simplerss.dataobject.itunes.ITunesChannel;
import com.simplerss.dataobject.itunes.ITunesOwner;
import com.simplerss.handler.RSSHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import java.net.MalformedURLException;
import java.net.URL;

public class ITunesRssHandler extends RSSHandler {

    protected ITunesOwnerHandler ownerHandler = null;

    protected ITunesCategoryHandler categoryHandler = null;

    protected ITunesItemHandler itemHandler = null;

    public ITunesRssHandler(XMLReader reader) {
        super(reader);
        ownerHandler = new ITunesOwnerHandler(this);
        categoryHandler = new ITunesCategoryHandler(this);
        itemHandler = new ITunesItemHandler(this);
    }

    public void startElement(String uri, String name, String qName, Attributes atts) throws SAXException {
        String tag = qName.toLowerCase();
        if ("itunes:image".equals(tag)) {
            try {
                ((ITunesChannel) channel).setItunesImage(new URL(atts.getValue("href")));
            } catch (MalformedURLException e) {
            }
        } else if ("itunes:owner".equals(tag)) {
            ownerHandler.startHandlingEvents(tag, atts);
        } else if ("itunes:category".equals(tag)) {
            categoryHandler.startHandlingEvents(tag, atts);
        } else if ("item".equals(tag)) {
            itemHandler.startHandlingEvents(tag, atts);
        } else {
            super.startElement(uri, name, qName, atts);
        }
    }

    public void setAttribute(String aName, Object aObject) throws SAXException {
        if ("itunes:owner".equals(aName)) {
            ((ITunesChannel) channel).setOwner((ITunesOwner) aObject);
        } else if ("itunes:category".equals(aName)) {
            ((ITunesChannel) channel).addCategory((ITunesCategory) aObject);
        } else {
            super.setAttribute(aName, aObject);
        }
    }

    public void endElement(String uri, String name, String qName) throws SAXException {
        String tag = qName.toLowerCase();
        if ("itunes:keywords".equals(tag)) {
            ((ITunesChannel) channel).setKeywords(mText);
        } else if ("itunes:subtitle".equals(tag)) {
            ((ITunesChannel) channel).setSummary(mText);
        } else if ("itunes:summary".equals(tag)) {
            ((ITunesChannel) channel).setSummary(mText);
        } else if ("itunes:author".equals(tag)) {
            ((ITunesChannel) channel).setAuthor(mText);
        } else if ("itunes:author".equals(tag)) {
            ((ITunesChannel) channel).setAuthor(mText);
        } else if ("itunes:block".equals(tag)) {
            ((ITunesChannel) channel).setBlock(mText.toLowerCase().startsWith("y"));
        } else if ("itunes:explicit".equals(tag)) {
            ((ITunesChannel) channel).setExplicit(mText.toLowerCase().startsWith("y") || mText.toLowerCase().startsWith("e"));
        } else {
            super.endElement(uri, name, qName);
        }
    }

    protected Channel createChannel() {
        return new ITunesChannel();
    }

    public ITunesChannel getITunesChannel() {
        return (ITunesChannel) super.getChannel();
    }
}
