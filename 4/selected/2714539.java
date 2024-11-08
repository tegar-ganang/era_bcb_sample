package com.simplerss.test;

import com.simplerss.handler.RSSHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import java.io.IOException;
import java.io.InputStream;

public class SampleRSSParser {

    /**
   * @param args
   */
    public static void main(String[] args) throws Exception {
        XMLReader reader = XMLReaderFactory.createXMLReader();
        RSSHandler rssHandler = new RSSHandler(reader);
        reader.setContentHandler(rssHandler);
        reader.parse(new InputSource(getInputStream()));
        System.out.println(rssHandler.getChannel());
    }

    private static InputStream getInputStream() throws IOException {
        InputStream inputStream = SampleRSSParser.class.getResourceAsStream("/example-itunes-podcast-file.xml");
        return inputStream;
    }
}
