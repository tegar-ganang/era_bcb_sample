package com.goodcodeisbeautiful.syndic8.rss20;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import com.goodcodeisbeautiful.syndic8.util.Syndic8Util;
import com.goodcodeisbeautiful.test.util.CommonTestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

public class Rss20FactoryTest extends CommonTestCase {

    public static Test suite() {
        return new TestSuite(Rss20FactoryTest.class);
    }

    private Rss20Factory m_factory;

    protected List getSetupFilenames() {
        ArrayList files = new ArrayList();
        files.add("simple-rss20.xml");
        files.add("example-rss20-2.xml");
        files.add("example-rss20-3.xml");
        return files;
    }

    protected void setUp() throws Exception {
        super.setUp();
        m_factory = new Rss20Factory();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        m_factory = null;
    }

    public void testNewInstance() throws Exception {
        FileInputStream in = null;
        try {
            Rss20 rss20 = m_factory.newInstance(new FileInputStream(getWorkDir() + File.separator + "simple-rss20.xml"));
            assertNotNull(rss20.getChannel());
            assertEquals("Simple Feed Title", rss20.getChannel().getTitle());
            assertEquals("http://archtea.sourceforge.net/", rss20.getChannel().getLink());
            assertEquals("Simple Feed Description", rss20.getChannel().getDescription());
            assertNotNull(rss20.getChannel().getItems());
            assertEquals(1, rss20.getChannel().getItems().length);
            assertEquals("Simple Description in item", rss20.getChannel().getItems()[0].getDescription());
        } finally {
            if (in != null) in.close();
        }
    }

    public void testNewInstance2() throws Exception {
        FileInputStream in = null;
        try {
            Rss20 rss20 = m_factory.newInstance(new FileInputStream(getWorkDir() + File.separator + "example-rss20-2.xml"));
            assertNotNull(rss20.getChannel());
            assertEquals("Channel Title", rss20.getChannel().getTitle());
            assertEquals("http://archtea.sourceforge.net/", rss20.getChannel().getLink());
            assertEquals("Channel Description", rss20.getChannel().getDescription());
            assertEquals("ja-JP", rss20.getChannel().getLanguage());
            assertEquals("Copyright 2006, Hiroki Ata", rss20.getChannel().getCopyright());
            assertEquals("editor@localhost", rss20.getChannel().getManagingEditor());
            assertEquals("webmaster@localhost", rss20.getChannel().getWebMaster());
            assertEquals(" ", rss20.getChannel().getRating());
            assertEquals(Syndic8Util.parseRFC2822Date("Wed, 25 Jan 2006 21:20:19 GMT"), rss20.getChannel().getPubDate());
            assertEquals(Syndic8Util.parseRFC2822Date("Wed, 25 Jan 2006 20:19:18 PST"), rss20.getChannel().getLastBuildDate());
            assertEquals("http://archtea.sourceforge.net/docs", rss20.getChannel().getDocs());
            String[] skipDays = rss20.getChannel().getSkipDays();
            assertEquals(2, skipDays.length);
            assertEquals("Sunday", skipDays[0]);
            assertEquals("Friday", skipDays[1]);
            int[] hours = rss20.getChannel().getSkipHours();
            assertEquals(3, hours.length);
            assertEquals(8, hours[0]);
            assertEquals(10, hours[1]);
            assertEquals(13, hours[2]);
            assertEquals("http://localhost/category", rss20.getChannel().getCategory().getDomain());
            assertEquals("Business", rss20.getChannel().getCategory().getCategory());
            assertEquals("generatorName", rss20.getChannel().getGenerator());
            assertEquals("40", rss20.getChannel().getTtl());
            assertEquals("http://localhost/cloud", rss20.getChannel().getCloud().getDomain());
            assertEquals("80", rss20.getChannel().getCloud().getPort());
            assertEquals("/RPC2", rss20.getChannel().getCloud().getPath());
            assertEquals("pleaseNotify", rss20.getChannel().getCloud().getRegisterProcedure());
            assertEquals("XML-RPC", rss20.getChannel().getCloud().getProtocol());
            assertEquals("Image Title", rss20.getChannel().getImage().getTitle());
            assertEquals("http://archtea.sourceforge.net/images/logo.gif", rss20.getChannel().getImage().getUrl());
            assertEquals("http://archtea.sourceforge.net/image", rss20.getChannel().getImage().getLink());
            assertEquals("88", rss20.getChannel().getImage().getWidth());
            assertEquals("31", rss20.getChannel().getImage().getHeight());
            assertEquals("Image Description", rss20.getChannel().getImage().getDescription());
            assertNotNull(rss20.getChannel().getItems());
            assertEquals(2, rss20.getChannel().getItems().length);
            Rss20Item item = rss20.getChannel().getItems()[0];
            assertEquals("The first item", item.getTitle());
            assertEquals("http://archtea.sourceforge.net/first/link", item.getLink());
            assertEquals("This is a description for the first item", item.getDescription());
            assertEquals("http://archtea.sourceforge.net/first/source", item.getSource().getUrl());
            assertEquals("The first source", item.getSource().getSource());
            assertEquals("http://archtea.sourceforge.net/first/enclosure", item.getEnclosure().getUrl());
            assertEquals(284, item.getEnclosure().getLength());
            assertEquals("audio/mpeg", item.getEnclosure().getType());
            assertEquals("http://archtea.sourceforge.net/second/category", item.getCategory().getDomain());
            assertEquals("Business/Publishing", item.getCategory().getCategory());
            assertEquals("Comments for the first item", item.getComment());
            assertEquals("first@author (first name)", item.getAuthor());
            assertEquals(Syndic8Util.parseRFC2822Date("Thu, 24 Jan 2006 23:24:25 GMT"), item.getPubDate());
            assertEquals("http://archtea.sourceforge.net/first/guid/1", item.getGuid().getGuid());
            assertEquals(true, item.getGuid().isPermalink());
            item = rss20.getChannel().getItems()[1];
            assertEquals("The Second item", item.getTitle());
            assertEquals("http://archtea.sourceforge.net/second/link", item.getLink());
            assertEquals("This is a description for the second item", item.getDescription());
            assertEquals("http://archtea.sourceforge.net/second/source", item.getSource().getUrl());
            assertEquals("The second source", item.getSource().getSource());
            assertEquals("http://archtea.sourceforge.net/second/enclosure", item.getEnclosure().getUrl());
            assertEquals(28, item.getEnclosure().getLength());
            assertEquals("audio/mpeg", item.getEnclosure().getType());
            assertEquals("http://archtea.sourceforge.net/second/category", item.getCategory().getDomain());
            assertEquals("Publisher/Notification", item.getCategory().getCategory());
            assertEquals("Comments for the second item", item.getComment());
            assertEquals("second@author (second name)", item.getAuthor());
            assertEquals(Syndic8Util.parseRFC2822Date("Mon, 23 Jan 2006 22:23:24 GMT"), item.getPubDate());
            assertEquals("http://archtea.sourceforge.net/second/guid/1", item.getGuid().getGuid());
            assertEquals(true, item.getGuid().isPermalink());
        } finally {
            if (in != null) in.close();
        }
    }

    public void testNewInstanceUnknown() throws Exception {
        FileInputStream in = null;
        try {
            Rss20 rss20 = m_factory.newInstance(new FileInputStream(getWorkDir() + File.separator + "example-rss20-3.xml"));
            assertNotNull(rss20.getChannel());
            assertEquals("Simple Feed Title", rss20.getChannel().getTitle());
            assertEquals("http://archtea.sourceforge.net/", rss20.getChannel().getLink());
            assertEquals("Simple Feed Description", rss20.getChannel().getDescription());
            assertNotNull(rss20.getChannel().getItems());
            assertEquals(1, rss20.getChannel().getItems().length);
            assertEquals("Item title", rss20.getChannel().getItems()[0].getTitle());
            assertEquals("Simple Description in item", rss20.getChannel().getItems()[0].getDescription());
        } finally {
            if (in != null) in.close();
        }
    }
}
