package com.spring.rssReader.jdbc;

import com.spring.rssReader.Channel;
import com.spring.rssReader.Item;
import org.springframework.orm.hibernate3.HibernateAccessor;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import java.util.Iterator;
import java.util.List;

/**
 * @author Ronald Date: 22-mrt-2004 Time: 21:26:40
 */
public class ItemDAOHibernate extends HibernateDaoSupport implements IItemDAO {

    public void deleteItem(Item item) {
        if (item != null) {
            Channel channel = (Channel) this.getHibernateTemplate().load(Channel.class, item.getChannelID());
            channel.getItems().remove(item);
            this.getHibernateTemplate().save(channel);
        }
    }

    public Item getItem(Long itemId) {
        return (Item) this.getHibernateTemplate().load(Item.class, itemId);
    }

    public void markItemsAsRead(Long id) {
        Channel channel = (Channel) this.getHibernateTemplate().load(Channel.class, id);
        if (channel != null) {
            Iterator it = channel.getItems().iterator();
            while (it.hasNext()) {
                Item item = (Item) it.next();
                item.setArticleRead(true);
            }
        }
    }

    /**
	 * This method should do nothing, since an item is inserted by adding it to the parent and then updating the parent.
	 * This already happens in the channel controller. However, to insert this item in the lucene index, the item must
	 * have an id.
	 * @param item
	 */
    public void insert(Item item) {
        int flushMode = this.getHibernateTemplate().getFlushMode();
        getHibernateTemplate().setFlushMode(HibernateAccessor.FLUSH_EAGER);
        getHibernateTemplate().save(item);
        getHibernateTemplate().setFlushMode(flushMode);
    }

    public void update(Item item) {
        this.getHibernateTemplate().save(item);
    }

    public Item findItemByUrl(String url, Long id) {
        List results = this.getHibernateTemplate().findByNamedParam("from com.spring.rssReader.Item where url=:url and channelID=:channelID", new String[] { "url", "channelID" }, new Object[] { url, id });
        if (results.size() >= 1) {
            return (Item) results.get(0);
        }
        return null;
    }

    public void deleteItemsFromChannel(Channel channel) {
        List items = channel.getItems();
        for (int i = 0; i < items.size(); i++) {
            Item item = (Item) items.get(i);
            this.getHibernateTemplate().delete(item);
        }
    }

    public List getAllItems(Long channelID) {
        return this.getHibernateTemplate().findByNamedParam("from Item where channelID=:channelID and remove=0" + " order by postedDate desc, itemID", "channelID", channelID);
    }

    public List getNewItems(Long channelID) {
        return this.getHibernateTemplate().findByNamedParam("from Item where channelID=:channelID" + " and articleRead=0 and remove=0 order by postedDate desc, itemID", "channelID", channelID);
    }
}
