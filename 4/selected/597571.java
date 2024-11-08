package com.spring.rssReader.jdbc;

import com.spring.rssReader.Channel;
import com.spring.rssReader.ICategory;
import org.springframework.orm.hibernate3.HibernateAccessor;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Properties;

/**
 * @author Ronald Date: 14-mrt-2004 Time: 20:28:41
 */
public class ChannelDAOHibernate extends HibernateDaoSupport implements IChannelDAO {

    private Properties queries;

    public Channel getChannel(Long id) {
        return (Channel) this.getHibernateTemplate().load(Channel.class, id);
    }

    public void markAsRead(Long id) {
        Channel channel = this.getChannel(id);
        channel.setNumberOfRead(channel.getNumberOfItems());
    }

    public void update(Channel channel) {
        this.getHibernateTemplate().update(channel);
    }

    public List getAllItems(Long channelID) {
        return this.getHibernateTemplate().findByNamedParam("from Item where channelID=:channelID and remove=0", "channelID", channelID);
    }

    public List getNewItems(Long channelID) {
        return this.getHibernateTemplate().findByNamedParam("from Item where channelID=:channelID and remove=0 and articleread=0", "channelID", channelID);
    }

    public List findChannelsByUrl(String url) {
        List results = this.getHibernateTemplate().findByNamedParam("from Channel where url=:url and remove=0", "url", url);
        return results;
    }

    public void insert(Channel channel) {
        int flushMode = this.getHibernateTemplate().getFlushMode();
        getHibernateTemplate().setFlushMode(HibernateAccessor.FLUSH_EAGER);
        getHibernateTemplate().save(channel);
        getHibernateTemplate().setFlushMode(flushMode);
    }

    public List getChannels(String query) {
        if (queries.getProperty(query) != null) {
            return this.getHibernateTemplate().find(queries.getProperty(query));
        } else {
            return this.getHibernateTemplate().find(query);
        }
    }

    public List findChannelsLikeUrl(String url) {
        return this.getHibernateTemplate().findByNamedParam("from Channel where url like :url and remove=0", "url", url);
    }

    public List findChannelsLikeTitle(String title) {
        return this.getHibernateTemplate().findByNamedParam("from Channel where title like :title and remove=0", "title", title);
    }

    public void delete(Channel channel) {
        this.getHibernateTemplate().delete(channel);
    }

    public void setQueries(Properties properties) {
        this.queries = properties;
    }

    public List getCategorizedChannels(ICategory category) {
        List channelObjecten = null;
        if (category == null) {
            channelObjecten = getChannels(IChannelDAO.CATEGORIZED_CHANNELS);
        } else {
            if (queries.getProperty(IChannelDAO.SPECIFIC_CATEGORIZED_CHANNELS) != null) {
                StringBuffer sb = new StringBuffer();
                sb.append(queries.getProperty(IChannelDAO.SPECIFIC_CATEGORIZED_CHANNELS));
                sb.append("(").append(category.getCategoryAndChildrenIds()).append(")");
                channelObjecten = this.getHibernateTemplate().find(sb.toString());
            }
        }
        List channels = new ArrayList(channelObjecten.size());
        for (int i = 0; i < channelObjecten.size(); i++) {
            channels.add(((Object[]) channelObjecten.get(i))[0]);
        }
        return channels;
    }
}
