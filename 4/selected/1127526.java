package org.wportal.rss.dao;

import org.wportal.testutil.JspWikiDatabaseTestCase;
import org.wportal.core.ContextManager;
import org.wportal.rss.parse.RSSChannel;

/**
 * User: SimonLei
 * Date: 2004-11-13
 * Time: 21:14:22
 * $Id: ChannelDaoTest.java,v 1.2 2004/11/14 10:00:19 simon_lei Exp $
 */
public class ChannelDaoTest extends JspWikiDatabaseTestCase {

    public ChannelDaoTest() {
        super("OneRssChannel0Item");
    }

    ChannelDao channelDao = (ChannelDao) ContextManager.getBean("channelDao");

    public void testStoreChannel() throws Exception {
        assertEquals(1, channelDao.getChannelCount());
        RSSChannel channel = new RSSChannel();
        String feed = "http://www.theserverside.com/rss/theserverside-1.0.rdf";
        channel.setFeed(feed);
        channelDao.saveChannel(channel);
        assertEquals(2, channelDao.getChannelCount());
        channel = channelDao.findChannelByFeed(feed);
        assertNotNull(channel);
    }

    public void testStoreSameUrlChannel() throws Exception {
        try {
            RSSChannel channel = new RSSChannel();
            channel.setFeed("http://blogsite.3322.org:8080/jspwiki/rss.rdf");
            channelDao.saveChannel(channel);
        } catch (Exception e) {
            return;
        }
        fail("Should throw exception");
    }

    public void testRSSParser() throws Exception {
        channelDao.addChannel(ChannelDaoTest.class.getResource("/dbfiles/tss-rdf.xml"), "http://www.theserverside.com/rss/theserverside-1.0.rdf");
    }
}
