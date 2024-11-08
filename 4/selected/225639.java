package com.simplerss.builder;

import com.simplerss.builder.FeedBuilder;
import com.simplerss.dataobject.Channel;
import com.simplerss.handler.RSSHandler;
import com.simplerss.builder.TestHelper;
import junit.framework.TestCase;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import java.io.InputStream;
import java.io.StringReader;

public class RssFeedBuilder_UT extends TestCase {

    private FeedBuilder builder;

    public Channel channel;

    protected void setUp() throws Exception {
        channel = setUpBasicRss();
        builder = new FeedBuilder();
    }

    public void testNamespaces() throws Exception {
        String basicDocument = builder.renderFeed(channel);
        assertTrue(basicDocument.indexOf("<rss version=\"2.0\"") != -1);
        assertEquals(-1, basicDocument.indexOf("xmlns"));
    }

    public void testBasicChannelRoundTrip() throws Exception {
        String basicDocument = builder.renderFeed(channel);
        Channel parsed = parseBasicRss(basicDocument);
        assertEquals(channel.getCategory(), parsed.getCategory());
        assertEquals(channel.getCopyright(), parsed.getCopyright());
        assertEquals(channel.getDescription(), parsed.getDescription());
        assertEquals(channel.getDocs(), parsed.getDocs());
        assertEquals(channel.getGenerator(), parsed.getGenerator());
        assertEquals(channel.getLanguage(), parsed.getLanguage());
        assertEquals(channel.getLink(), parsed.getLink());
        assertEquals(channel.getManagingEditor(), parsed.getManagingEditor());
        TestHelper.assertBlankOrEquals(channel.getRating(), parsed.getRating());
        assertEquals(channel.getTitle(), parsed.getTitle());
        assertEquals(channel.getTtl(), parsed.getTtl());
        assertEquals(channel.getWebMaster(), parsed.getWebMaster());
        assertEquals(channel.getLastBuildDate(), parsed.getLastBuildDate());
        assertEquals(channel.getPubDate(), parsed.getPubDate());
    }

    public void testChannelImageFieldsRoundTrip() throws Exception {
        String basicDocument = builder.renderFeed(channel);
        Channel parsed = parseBasicRss(basicDocument);
        assertNotNull(channel.getImage());
        assertNotNull(parsed.getImage());
        assertEquals("", parsed.getImage().getDescription());
        assertEquals(channel.getImage().getHeight(), parsed.getImage().getHeight());
        assertEquals(channel.getImage().getLink(), parsed.getImage().getLink());
        assertEquals(channel.getImage().getTitle(), parsed.getImage().getTitle());
        assertEquals(channel.getImage().getUrl(), parsed.getImage().getUrl());
        assertEquals(channel.getImage().getWidth(), parsed.getImage().getWidth());
    }

    public Channel parseBasicRss(String generatedFeed) throws Exception {
        XMLReader reader = XMLReaderFactory.createXMLReader();
        RSSHandler rssHandler = new RSSHandler(reader);
        reader.setContentHandler(rssHandler);
        reader.parse(new InputSource(new StringReader(generatedFeed)));
        return rssHandler.getChannel();
    }

    public Channel setUpBasicRss() throws Exception {
        XMLReader reader = XMLReaderFactory.createXMLReader();
        RSSHandler rssHandler = new RSSHandler(reader);
        reader.setContentHandler(rssHandler);
        reader.parse(new InputSource(getInputStream()));
        return rssHandler.getChannel();
    }

    protected static InputStream getInputStream() {
        return TestHelper.class.getResourceAsStream("/example-itunes-podcast-file.xml");
    }
}
