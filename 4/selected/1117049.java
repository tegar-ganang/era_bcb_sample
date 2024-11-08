package com.simplerss.builder;

import com.simplerss.dataobject.Channel;
import com.simplerss.handler.RSSHandler;
import com.simplerss.handler.itunes.ITunesRssHandler;
import junit.framework.Assert;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import java.io.InputStream;

public class TestHelper {

    public static Channel CHANNEL;

    public static RSSHandler rssHandler;

    public static void assertStringContains(String haystack, String needle) {
        Assert.assertTrue(needle + " not found in " + haystack, haystack.indexOf(needle) != -1);
    }

    public static void setUpBasicRss() throws Exception {
        XMLReader reader = XMLReaderFactory.createXMLReader();
        rssHandler = new RSSHandler(reader);
        reader.setContentHandler(rssHandler);
        reader.parse(new InputSource(getInputStream()));
        CHANNEL = rssHandler.getChannel();
    }

    public static void setUpITunesRss() throws Exception {
        XMLReader reader = XMLReaderFactory.createXMLReader();
        rssHandler = new ITunesRssHandler(reader);
        reader.setContentHandler(rssHandler);
        reader.parse(new InputSource(getInputStream()));
        CHANNEL = rssHandler.getChannel();
    }

    protected static InputStream getInputStream() {
        return TestHelper.class.getResourceAsStream("/example-itunes-podcast-file.xml");
    }

    public static void assertBlankOrEquals(String expected, String underTest) {
        if (expected == null || expected.trim().length() == 0) {
            Assert.assertTrue(underTest == null || underTest.trim().length() == 0);
        } else {
            Assert.assertEquals(expected, underTest);
        }
    }

    public static String assertTags(Document doc, String[] expectedTags) {
        String output = new FeedBuilderUtils().getStringFromDocument(doc);
        for (int i = 0; i < expectedTags.length; i++) {
            assertStringContains(output, expectedTags[i]);
        }
        return output;
    }
}
