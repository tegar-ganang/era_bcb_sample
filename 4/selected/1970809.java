package org.wportal.rss.dao;

import org.wportal.rss.parse.RSSChannel;
import org.wportal.rss.parse.RSSParser;
import org.wportal.rss.parse.ParseException;
import org.wportal.rss.parse.RSSItem;
import org.springframework.orm.hibernate.support.HibernateDaoSupport;
import java.util.List;
import java.util.Iterator;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * User: SimonLei
 * Date: 2004-11-13
 * Time: 21:03:53
 * $Id: ChannelDaoImpl.java,v 1.2 2004/11/14 10:00:19 simon_lei Exp $
 */
public class ChannelDaoImpl extends HibernateDaoSupport implements ChannelDao {

    private RssItemDao rssItemDao;

    public RssItemDao getRssItemDao() {
        return rssItemDao;
    }

    public void setRssItemDao(RssItemDao rssItemDao) {
        this.rssItemDao = rssItemDao;
    }

    public RSSChannel findChannelByFeed(String feed) {
        List list = getHibernateTemplate().find("from RSSChannel channel where channel.feed = ?", feed);
        if (list.size() > 0) return (RSSChannel) list.get(0);
        return null;
    }

    public int getChannelCount() {
        List list = getHibernateTemplate().find("select count(*) from RSSChannel channel");
        if (list.size() > 0) return (Integer) list.get(0);
        return 0;
    }

    public void saveChannel(RSSChannel channel) {
        getHibernateTemplate().save(channel);
    }

    public RSSChannel addChannel(String feed) throws MalformedURLException, ParseException {
        return addChannel(new URL(feed), feed);
    }

    public RSSChannel addChannel(URL feedURL, String feed) throws ParseException {
        RSSChannel chan = RSSParser.parse(feedURL);
        chan.setFeed(feed);
        getHibernateTemplate().save(chan);
        updateItems(chan);
        return chan;
    }

    private void updateItems(RSSChannel chan) {
        for (Iterator i = chan.getItems().iterator(); i.hasNext(); ) {
            RSSItem item = (RSSItem) i.next();
            System.out.println("item.getLink() = " + item.getLink());
            rssItemDao.addItem(item);
        }
    }

    public List getAllChannels() {
        return getHibernateTemplate().find("from RSSChannel channel");
    }

    public void updateChannel(RSSChannel channel) throws MalformedURLException, ParseException {
        RSSChannel newChannel = RSSParser.parse(new URL(channel.getFeed()));
        newChannel.setFeed(channel.getFeed());
        newChannel.setId(channel.getId());
        getHibernateTemplate().update(newChannel);
        updateItems(newChannel);
    }
}
