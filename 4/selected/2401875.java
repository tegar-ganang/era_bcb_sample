package de.nava.informa.utils;

import java.io.File;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ChannelUpdatePeriod;
import de.nava.informa.core.FeedIF;
import de.nava.informa.impl.basic.ChannelBuilder;
import de.nava.informa.impl.basic.Feed;
import de.nava.informa.parsers.FeedParser;

/**
 * @author Jean-Guy Avelin
 */
public class TestCacheSettingsFM extends InformaTestCase {

    public TestCacheSettingsFM(String name) {
        super("TestCacheSettingsFM", name);
    }

    public void testRSS091SettingsDefault() {
        FeedManager FM = new FeedManager(ChannelUpdatePeriod.UPDATE_HOURLY, 2);
        CacheSettings cs = new CacheSettings();
        cs.setDefaultTtl(2000);
        File inpFile = new File(getDataDir(), "xmlhack-0.91.xml");
        try {
            ChannelIF channel = FeedParser.parse(new ChannelBuilder(), inpFile);
            assertEquals(2000, cs.getTtl(channel, -1L));
            String url = new Feed(channel).getLocation().toString();
            FM.addFeed(url);
            assertEquals(2000, cs.getTtl(FM.getFeed(url).getChannel(), -1L));
        } catch (Exception e) {
            System.err.println("testRSS091SettingsDefault error " + e);
            e.printStackTrace();
        }
    }

    public void testRSS091Settings() {
        FeedManager FM = new FeedManager(ChannelUpdatePeriod.UPDATE_HOURLY, 2);
        CacheSettings cs = new CacheSettings();
        cs.setDefaultTtl(2000);
        File inpFile = new File(getDataDir(), "xmlhack-0.91.xml");
        try {
            ChannelIF channel = FeedParser.parse(new ChannelBuilder(), inpFile);
            assertEquals(2000L, cs.getTtl(channel, 10000L));
            assertEquals(360000L, cs.getTtl(channel, 360000L));
            String url = new Feed(channel).getLocation().toString();
            FM.addFeed(url);
            assertEquals(2000L, cs.getTtl(FM.getFeed(url).getChannel(), 10000L));
            assertEquals(360000L, cs.getTtl(FM.getFeed(url).getChannel(), 360000L));
        } catch (Exception e) {
            System.err.println("testRSS091Settings error " + e);
        }
    }

    public void testRSS100SettingsNoUpdatePeriodInFeed() {
        FeedManager FM = new FeedManager(ChannelUpdatePeriod.UPDATE_HOURLY, 2);
        CacheSettings cs = new CacheSettings();
        cs.setDefaultTtl(2000);
        File inpFile = new File(getDataDir(), "bloggy.rdf");
        try {
            ChannelIF channel = FeedParser.parse(new ChannelBuilder(), inpFile);
            assertEquals(2000, cs.getTtl(channel, -1L));
            assertEquals(2000L, cs.getTtl(channel, 10000L));
            assertEquals(360000L, cs.getTtl(channel, 360000L));
            String url = new Feed(channel).getLocation().toString();
            FM.addFeed(url);
            assertEquals(2000, cs.getTtl(FM.getFeed(url).getChannel(), -1L));
            assertEquals(2000L, cs.getTtl(FM.getFeed(url).getChannel(), 10000L));
            assertEquals(360000L, cs.getTtl(FM.getFeed(url).getChannel(), 360000L));
        } catch (Exception e) {
            System.err.println("testRSS100SettingsNoUpdatePeriodInFeed error " + e);
            e.printStackTrace();
        }
    }

    public void testRSS100SettingsUpdatePeriodInFeed() {
        FeedManager FM = new FeedManager(ChannelUpdatePeriod.UPDATE_HOURLY, 2);
        CacheSettings cs = new CacheSettings();
        cs.setDefaultTtl(2000);
        File inpFile = new File(getDataDir(), "slashdot-010604.rdf");
        try {
            ChannelIF channel = FeedParser.parse(new ChannelBuilder(), inpFile);
            assertEquals(3600000, cs.getTtl(channel, -1L));
            assertEquals(3600000, cs.getTtl(channel, 10000L));
            assertEquals(3600005, cs.getTtl(channel, 3600005));
            String url = new Feed(channel).getLocation().toString();
            FM.addFeed(url);
            assertEquals(3600000, cs.getTtl(FM.getFeed(url).getChannel(), -1L));
            assertEquals(3600000, cs.getTtl(FM.getFeed(url).getChannel(), 10000L));
        } catch (Exception e) {
            System.err.println("testRSS100SettingsUpdatePeriodInFeed error " + e);
        }
    }

    public void testRSS200SettingsUpdatePeriodInFeed() {
        FeedManager FM = new FeedManager(ChannelUpdatePeriod.UPDATE_HOURLY, 2);
        CacheSettings cs = new CacheSettings();
        cs.setDefaultTtl(2000);
        File inpFile = new File(getDataDir(), "mobitopia.xml");
        try {
            ChannelIF channel = FeedParser.parse(new ChannelBuilder(), inpFile);
            assertEquals((60 * 60 * 1000), cs.getTtl(channel, -1L));
            assertEquals((60 * 60 * 1000), cs.getTtl(channel, 10000L));
            assertEquals((60 * 60 * 1000), cs.getTtl(channel, (30 * 60 * 1000)));
            assertEquals((120 * 60 * 1000), cs.getTtl(channel, (120 * 60 * 1000)));
            String url = new Feed(channel).getLocation().toString();
            FM.addFeed(url);
            assertEquals((60 * 60 * 1000), cs.getTtl(FM.getFeed(url).getChannel(), -1L));
            assertEquals((60 * 60 * 1000), cs.getTtl(FM.getFeed(url).getChannel(), 10000L));
        } catch (Exception e) {
            System.err.println("testRSS200SettingsUpdatePeriodInFeed error " + e);
        }
    }

    public void testFMCache091() {
        long t1, t2;
        FeedManager FM = new FeedManager(ChannelUpdatePeriod.UPDATE_HOURLY, 2);
        CacheSettings cs = new CacheSettings();
        cs.setDefaultTtl(3000);
        File inpFile = new File(getDataDir(), "xmlhack-0.91.xml");
        try {
            ChannelIF channel = FeedParser.parse(new ChannelBuilder(), inpFile);
            assertEquals(3000, cs.getTtl(channel, -1L));
            String url = new Feed(channel).getLocation().toString();
            FM.removeFeed(url);
            FM.addFeed(url);
            assertTrue("feed not in cache !!", FM.hasFeed(url));
            FeedIF feedRef1 = FM.getFeed(url);
            assertNotNull("feed read null", feedRef1);
            t1 = feedRef1.getLastUpdated().getTime();
            t2 = FM.getFeed(url).getLastUpdated().getTime();
            assertTrue("same  date for feeds read in cache " + t1, t1 == t2);
            try {
                Thread.sleep(4000);
            } catch (Exception e) {
                System.err.println("Error in Wait..." + e);
            }
            t2 = FM.getFeed(url).getLastUpdated().getTime();
            System.err.println("LAST uptd " + t1 + "," + t2);
            System.err.println("LAST uptd test (false?)" + (t1 == t2));
            assertTrue("lastUpdated must differ for RSS091 test feed", t1 != t2);
            FM.removeFeed(url);
            assertFalse("feed should be removed", FM.hasFeed(url));
            System.err.println("ori:" + FM.addFeed(url, 100).getLastUpdated().getTime());
            t1 = FM.getFeed(url).getLastUpdated().getTime();
            try {
                Thread.sleep(4000);
            } catch (Exception e) {
                System.err.println("Error in Wait..." + e);
            }
            t2 = FM.getFeed(url).getLastUpdated().getTime();
            assertTrue("lastUpdated must be the same for a 100 mins ttl" + url, t1 == t2);
        } catch (Exception e) {
            System.err.println("testFMCache091 error " + e);
            e.printStackTrace();
        }
    }

    public void testCondGet() {
        long t1, t2;
        FeedManager FM = new FeedManager(ChannelUpdatePeriod.UPDATE_HOURLY, 2);
        CacheSettings cs = new CacheSettings();
        cs.setDefaultTtl(3000);
        try {
            ChannelIF channel = FeedParser.parse(new ChannelBuilder(), "http://www.intertwingly.net/blog/index.rss");
            String url = new Feed(channel).getLocation().toString();
            FM.removeFeed(url);
            FM.addFeed(url);
            FeedIF feedRef1 = FM.getFeed(url);
            t1 = feedRef1.getLastUpdated().getTime();
            t2 = FM.getFeed(url).getLastUpdated().getTime();
            assertTrue("same  date for feeds read in cache " + t1, t1 == t2);
            try {
                Thread.sleep(4000);
            } catch (Exception e) {
                System.err.println("Error in Wait..." + e);
            }
            System.err.println("Look at the stderr for trace about cond.get");
            t2 = FM.getFeed(url).getLastUpdated().getTime();
            assertTrue("lastUpdated must be different", t1 != t2);
        } catch (Exception e) {
            System.err.println("testCondGet error " + e);
            e.printStackTrace();
        }
    }

    public void testFMCacheRSS100WithUpdatePeriod() {
        long t1, t2;
        FeedManager FM = new FeedManager(ChannelUpdatePeriod.UPDATE_HOURLY, 2);
        CacheSettings cs = new CacheSettings();
        cs.setDefaultTtl(3000);
        File inpFile = new File(getDataDir(), "slashdot-010604.rdf");
        try {
            ChannelIF channel = FeedParser.parse(new ChannelBuilder(), inpFile);
            assertEquals(60 * 60 * 1000, cs.getTtl(channel, -1L));
            String url = new Feed(channel).getLocation().toString();
            FM.removeFeed(url);
            channel = FeedParser.parse(new ChannelBuilder(), inpFile);
            url = new Feed(channel).getLocation().toString();
            FeedIF feedRef1 = FM.addFeed(url);
            t1 = feedRef1.getLastUpdated().getTime();
            try {
                Thread.sleep(4000);
            } catch (Exception e) {
                System.err.println("Error in Wait..." + e);
            }
            t2 = FM.getFeed(url).getLastUpdated().getTime();
            assertTrue("lastUpdated must be the same for this RSS1.00 test feed", (t1 == t2));
        } catch (Exception e) {
            System.err.println("testFMCacheRSS100WithUpdatePeriod error " + e);
            e.printStackTrace();
        }
    }
}
