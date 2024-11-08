package de.cinek.rssview;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import org.apache.commons.collections.iterators.CollatingIterator;

/**
 * @author saintedlama
 */
public class RssAggregateChannel extends AbstractChannel {

    private List channels;

    private List articles;

    private RssGroupNode group;

    private RssChannelHeader header;

    private int articlesInView;

    private static final ResourceBundle rb = ResourceBundle.getBundle("rssview");

    public RssAggregateChannel() {
        this.articles = new ArrayList();
        this.channels = new ArrayList();
    }

    public void setChannels(List channels) {
        this.channels = channels;
        List articleIterators = new ArrayList(channels.size());
        for (Iterator iter = channels.iterator(); iter.hasNext(); ) {
            Channel channel = (Channel) iter.next();
            articleIterators.add(channel.iterator());
        }
        CollatingIterator currentArticlesIter = new CollatingIterator(new ArticleDateComparator(), articleIterators);
        while (currentArticlesIter.hasNext()) {
            add((Article) currentArticlesIter.next());
        }
        updateHeader();
    }

    public void setGroup(RssGroupNode group) {
        this.group = group;
    }

    protected void updateHeader() {
        this.header = new RssChannelHeader();
        StringBuffer builder = new StringBuffer();
        builder.append(rb.getString("newest"));
        builder.append(' ');
        builder.append(getArticleCount());
        builder.append(' ');
        builder.append(rb.getString("articles_from"));
        builder.append(' ');
        for (Iterator iter = channels.iterator(); iter.hasNext(); ) {
            builder.append(((Channel) iter.next()).getName());
            if (iter.hasNext()) {
                builder.append(',');
            }
        }
        header.setDescription(builder.toString());
        header.setTitle(getName());
    }

    /**
	 * @see de.cinek.rssview.Channel#getUnread()
	 */
    public int getUnread() {
        return 0;
    }

    /**
	 * @see de.cinek.rssview.Channel#markArticlesRead()
	 */
    public void markArticlesRead() {
        for (Iterator iter = articles.iterator(); iter.hasNext(); ) {
            Article article = (Article) iter.next();
            Channel parentChannel = article.getChannel();
            if (parentChannel != null) {
                parentChannel.setRead(parentChannel.indexOf(article), true);
            }
        }
    }

    /**
	 * @see de.cinek.rssview.Channel#get(int)
	 */
    public Article get(int index) {
        return (Article) articles.get(index);
    }

    /**
	 * @see de.cinek.rssview.Channel#getArticleCount()
	 */
    public int getArticleCount() {
        return articles.size();
    }

    /**
	 * @see de.cinek.rssview.Channel#contains(de.cinek.rssview.Article)
	 */
    public boolean contains(Article article) {
        return articles.contains(article);
    }

    /**
	 * @see de.cinek.rssview.Channel#add(de.cinek.rssview.Article)
	 */
    public void add(Article article) {
        articles.add(article);
    }

    /**
	 * @see de.cinek.rssview.Channel#set(int, de.cinek.rssview.Article)
	 */
    public void set(int index, Article article) {
        articles.set(index, article);
    }

    /**
	 * @see de.cinek.rssview.Channel#remove(de.cinek.rssview.Article)
	 */
    public void remove(Article article) {
        articles.remove(article);
    }

    /**
	 * @see de.cinek.rssview.Channel#indexOf(de.cinek.rssview.Article)
	 */
    public int indexOf(Article article) {
        return articles.indexOf(article);
    }

    /**
	 * @see de.cinek.rssview.Channel#isRead(int)
	 */
    public boolean isRead(int index) {
        return get(index).isRead();
    }

    /**
	 * @see de.cinek.rssview.Channel#setRead(int, boolean)
	 */
    public void setRead(int index, boolean read) {
        Article article = get(index);
        Channel parentChannel = article.getChannel();
        if (parentChannel != null) {
            parentChannel.setRead(parentChannel.indexOf(article), read);
        }
    }

    /**
	 * @see de.cinek.rssview.Channel#getHeader()
	 */
    public ChannelHeader getHeader() {
        return header;
    }

    /**
	 * @see de.cinek.rssview.Channel#getQuery()
	 */
    public Query getQuery() {
        return null;
    }

    /**
	 * @see de.cinek.rssview.Channel#getName()
	 */
    public String getName() {
        if (group != null) {
            return group.getName();
        }
        return null;
    }

    /**
	 * @see de.cinek.rssview.Channel#getUrl()
	 */
    public String getUrl() {
        return null;
    }

    /**
	 * @see de.cinek.rssview.Channel#getArticlesInView()
	 */
    public int getArticlesInView() {
        return articlesInView;
    }

    /**
	 * @see de.cinek.rssview.Channel#getPollInterval()
	 */
    public int getPollInterval() {
        return Integer.MAX_VALUE;
    }

    /**
	 * @see de.cinek.rssview.Channel#isActive()
	 */
    public boolean isActive() {
        return false;
    }

    /**
	 * @see de.cinek.rssview.Channel#isBeepEnabled()
	 */
    public boolean isBeepEnabled() {
        return true;
    }

    /**
	 * @see de.cinek.rssview.Channel#isRememberArticlesEnabled()
	 */
    public boolean isRememberArticlesEnabled() {
        return false;
    }

    /**
	 * @see de.cinek.rssview.Channel#isViewTextEnabled()
	 */
    public boolean isViewTextEnabled() {
        return true;
    }

    /**
	 * @see de.cinek.rssview.Channel#getId()
	 */
    public int getId() {
        return 0;
    }

    /**
	 * @see de.cinek.rssview.Channel#setId(int)
	 */
    public void setId(int id) {
    }

    /**
	 * @see de.cinek.rssview.Channel#merge(de.cinek.rssview.Channel)
	 */
    public void merge(Channel channel) {
    }

    /**
	 * @see de.cinek.rssview.Channel#merge(de.cinek.rssview.Channel, boolean)
	 */
    public void merge(Channel channel, boolean purgeOld) {
    }

    /**
	 * @see de.cinek.rssview.Node#getCategory()
	 */
    public CategoryNode getCategory() {
        return group;
    }

    /**
	 * @see de.cinek.rssview.Node#setCategory(de.cinek.rssview.CategoryNode)
	 */
    public void setCategory(CategoryNode catgory) {
    }

    /**
	 * @see de.cinek.rssview.Channel#iterator()
	 */
    public Iterator iterator() {
        return articles.iterator();
    }

    public void setArticlesInView(int articlesInView) {
        this.articlesInView = articlesInView;
    }
}
