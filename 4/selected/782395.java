package com.spring.rssReader.jdbc;

import com.spring.rssReader.Channel;
import com.spring.rssReader.ICategory;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.object.MappingSqlQuery;
import org.springframework.jdbc.object.SqlUpdate;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

/**
 * @author Ronald
 * Date: Dec 1, 2003
 * Time: 12:47:16 PM
 */
public class ChannelDAOJdbc extends JdbcDaoSupport implements IChannelDAO {

    private Properties queries;

    private static String __EMPTY_CHANNELS = "select channel.* from channel " + " left join item on channel.channelID=item.channelID" + " where item.itemID is null" + " and channel.remove=0" + " and isHtml=0" + " order by channel.title";

    private static String __FAVOURITE_CHANNELS = "select * from channel " + " where remove=0" + " and preferance > 0" + " and isHtml=0" + " order by channel.preferance, channel.title";

    private static String __CHANNELS_TO_READ = "select * from channel" + " where remove=0" + " and numberOfItems != numberOfRead" + " and isHtml=0" + " order by preferance desc, lastPolled desc";

    private static String __CHANNELS_NO_NEWS_ITEMS = "select * from channel" + " where remove=0" + " and numberOfItems = numberOfRead" + " and isHtml=0" + " order by preferance desc, lastPolled desc";

    private static String __GET_ALL_CHANNELS = "select * from channel" + " where remove=0 and pollsStarted<10 and isHtml=0" + " order by pollsStarted asc, lastPolled desc";

    private static String __HTML_ARTICLES = "select * from channel" + " where isHtml=1 and remove=0" + " order by channel.title";

    private static String FIND_CHANNEL_BY_ID = "select * from channel where channelID=?";

    private static String FIND_CHANNEL_BY_URL = "select * from channel where url =?";

    public static final String FIND_CHANNEL_LIKE_URL = "select * from channel where lower(url) like ?";

    public static final String FIND_CHANNEL_LIKE_TITLE = "select * from channel where lower(title) like ?";

    private static final String UPDATE_CHANNEL = "update channel " + " set url=?" + ", title=?" + ", description=?" + ", lastPolled=?" + ", created=?" + ", lastETag=?" + ", status=?" + ", numberOfItems=?" + ", numberOfRead=?" + ", remove=?" + ", lastModified=?" + ", preferance=?" + ", pollsStarted=?" + ", isHtml=?" + ", autoInsert=?" + " where channelID=?";

    private static final String INSERT_CHANNEL = "insert into channel( " + " url" + ", title" + ", description" + ", lastPolled" + ", created" + ", lastETag" + ", status" + ", numberOfItems" + ", numberOfRead" + ", remove" + ", lastModified" + ", preferance" + ", pollsStarted" + ", isHtml" + ", autoInsert" + ") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String SET_READITEMS = "update channel set numberOfRead=numberOfItems where channelID=?";

    public static final String DELETE_ITEM_FROM_CHANNEL = "delete from item where channelID=?";

    public static final String DELETE_CHANNEL = "delete from channel where channelID=?";

    private static final String __CATEGORIZED_CHANNELS = "select channel.* from channel, generalItemCategory, category" + " where channel.channelId=generalItemCategory.channelId" + " and generalItemCategory.categoryId=category.categoryId";

    private ChannelQuery listArticles = null;

    private ChannelQuery listFavouriteChannels = null;

    private ChannelQuery listToReadChannels = null;

    private ChannelQuery listNoNewsChannels = null;

    private ChannelQuery listEmptyChannels = null;

    private ChannelQuery listAllChannels = null;

    private FindChannelQueryById findChannelQueryById = null;

    private MarkAsReadQuery updateChannelAsRead = null;

    private UpdateChannel updateChannel = null;

    private FindChannelQuery findChannelQueryByUrl = null;

    private InsertChannel insertChannel = null;

    private FindChannelQuery findChannelQueryLikeUrl;

    private FindChannelQuery findChannelQueryLikeTitle;

    private DeleteChannelQuery deleteChannelQuery;

    private ChannelQuery listCategorizedChannels = null;

    private FindChannelQuery listSpecificCategorizedChannels = null;

    private Map cachedQueries = Collections.synchronizedMap(new HashMap());

    private ILastInsertDAO lastInsertDAO = null;

    public ChannelDAOJdbc() {
    }

    public ILastInsertDAO getLastInsertDAO() {
        return lastInsertDAO;
    }

    public void setLastInsertDAO(ILastInsertDAO lastInsertDAO) {
        this.lastInsertDAO = lastInsertDAO;
    }

    public void setQueries(Properties queries) {
        this.queries = queries;
    }

    protected void initDao() throws Exception {
        super.initDao();
        if (listAllChannels == null) {
            listAllChannels = new ChannelQuery(super.getDataSource(), getQuery(IChannelDAO.CHANNELS_TO_POLL, __GET_ALL_CHANNELS));
            cachedQueries.put(IChannelDAO.CHANNELS_TO_POLL, listAllChannels);
        }
        if (listArticles == null) {
            listArticles = new ChannelQuery(super.getDataSource(), getQuery(IChannelDAO.ARTICLES, __HTML_ARTICLES));
            cachedQueries.put(IChannelDAO.ARTICLES, listArticles);
        }
        if (listFavouriteChannels == null) {
            listFavouriteChannels = new ChannelQuery(super.getDataSource(), getQuery(IChannelDAO.FAVOURITES, __FAVOURITE_CHANNELS));
            cachedQueries.put(IChannelDAO.FAVOURITES, listFavouriteChannels);
        }
        if (listToReadChannels == null) {
            listToReadChannels = new ChannelQuery(super.getDataSource(), getQuery(IChannelDAO.CHANNELS_TO_READ, __CHANNELS_TO_READ));
            cachedQueries.put(IChannelDAO.CHANNELS_TO_READ, listToReadChannels);
        }
        if (listNoNewsChannels == null) {
            listNoNewsChannels = new ChannelQuery(super.getDataSource(), getQuery(IChannelDAO.NO_NEWS_CHANNELS, __CHANNELS_NO_NEWS_ITEMS));
            cachedQueries.put(IChannelDAO.NO_NEWS_CHANNELS, listNoNewsChannels);
        }
        if (listEmptyChannels == null) {
            listEmptyChannels = new ChannelQuery(super.getDataSource(), getQuery(IChannelDAO.EMPTY_CHANNELS, __EMPTY_CHANNELS));
            cachedQueries.put(IChannelDAO.EMPTY_CHANNELS, listEmptyChannels);
        }
        if (findChannelQueryById == null) {
            findChannelQueryById = new FindChannelQueryById(super.getDataSource(), FIND_CHANNEL_BY_ID);
        }
        if (findChannelQueryByUrl == null) {
            findChannelQueryByUrl = new FindChannelQuery(super.getDataSource(), FIND_CHANNEL_BY_URL);
        }
        if (findChannelQueryLikeUrl == null) {
            findChannelQueryLikeUrl = new FindChannelQuery(super.getDataSource(), FIND_CHANNEL_LIKE_URL);
        }
        if (findChannelQueryLikeTitle == null) {
            findChannelQueryLikeTitle = new FindChannelQuery(super.getDataSource(), FIND_CHANNEL_LIKE_TITLE);
        }
        if (updateChannelAsRead == null) {
            updateChannelAsRead = new MarkAsReadQuery(super.getDataSource(), SET_READITEMS);
        }
        if (updateChannel == null) {
            updateChannel = new UpdateChannel(super.getDataSource(), UPDATE_CHANNEL);
        }
        if (insertChannel == null) {
            insertChannel = new InsertChannel(super.getDataSource(), INSERT_CHANNEL);
        }
        if (deleteChannelQuery == null) {
            deleteChannelQuery = new DeleteChannelQuery(super.getDataSource(), DELETE_CHANNEL);
        }
        if (listCategorizedChannels == null) {
            listCategorizedChannels = new ChannelQuery(super.getDataSource(), getQuery(IChannelDAO.CATEGORIZED_CHANNELS, __CATEGORIZED_CHANNELS));
            cachedQueries.put(IChannelDAO.CATEGORIZED_CHANNELS, listCategorizedChannels);
        }
        if (listSpecificCategorizedChannels == null) {
            listSpecificCategorizedChannels = new FindChannelQuery(super.getDataSource(), getQuery(IChannelDAO.SPECIFIC_CATEGORIZED_CHANNELS, null));
        }
    }

    private String getQuery(String queryKey, String defaultQuery) {
        if (queries.getProperty(queryKey) != null) {
            return queries.getProperty(queryKey);
        }
        return defaultQuery;
    }

    public Channel getChannel(Long id) {
        List results = findChannelQueryById.execute(new Object[] { id });
        if (results.size() > 0) {
            return (Channel) results.get(0);
        }
        return null;
    }

    public void markAsRead(Long itemId) {
        updateChannelAsRead.update(new Object[] { itemId });
    }

    public void update(Channel channel) {
        updateChannel.update(channel);
    }

    public List findChannelsByUrl(String url) {
        return findChannelQueryByUrl.execute(url);
    }

    public void insert(Channel channel) {
        insertChannel.update(channel);
        channel.setId(lastInsertDAO.getLatestId());
    }

    /**
     * The pre-defined queries are cached
     * @param query
     * @return
     */
    public List getChannels(String query) {
        if (cachedQueries.containsKey(query)) {
            return ((ChannelQuery) cachedQueries.get(query)).execute();
        } else {
            ChannelQuery channelQuery = new ChannelQuery(super.getDataSource(), query);
            cachedQueries.put(query, channelQuery);
            return channelQuery.execute();
        }
    }

    public List findChannelsLikeUrl(String url) {
        return findChannelQueryLikeUrl.execute(url);
    }

    public List findChannelsLikeTitle(String title) {
        return findChannelQueryLikeTitle.execute(title);
    }

    public void delete(Channel channel) {
        deleteChannelQuery.update(new Object[] { channel.getId() });
    }

    public List getCategorizedChannels(ICategory category) {
        if (category.getId().longValue() == -1L) {
            return listCategorizedChannels.execute();
        } else {
            return listSpecificCategorizedChannels.execute(category.getCategoryAndChildrenIds());
        }
    }

    private static class MarkAsReadQuery extends SqlUpdate {

        public MarkAsReadQuery(DataSource ds, String sql) {
            super(ds, sql);
            declareParameter(new SqlParameter(Types.BIGINT));
            compile();
        }
    }

    private class ChannelQueryPrivate extends MappingSqlQuery {

        public ChannelQueryPrivate() {
        }

        public ChannelQueryPrivate(DataSource ds, String sql) {
            super(ds, sql);
        }

        protected Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            Channel channel = new Channel();
            channel.setCreated(rs.getLong("created"));
            channel.setDescription(rs.getString("description"));
            channel.setId(new Long(rs.getLong("channelID")));
            channel.setLastETag(rs.getString("lastETag"));
            channel.setLastModified(rs.getString("lastModified"));
            channel.setLastPolled(rs.getLong("lastPolled"));
            channel.setNumberOfItems(rs.getInt("numberOfItems"));
            channel.setNumberOfRead(rs.getInt("numberOfRead"));
            channel.setRemove(rs.getBoolean("remove"));
            channel.setStatus(rs.getInt("status"));
            channel.setPreferance(rs.getInt("preferance"));
            channel.setTitle(rs.getString("title"));
            channel.setUrl(rs.getString("url"));
            channel.setPollsStarted(rs.getInt("pollsStarted"));
            channel.setHtml(rs.getBoolean("isHtml"));
            channel.setAutoInsert(rs.getBoolean("autoInsert"));
            return channel;
        }
    }

    private class ChannelQuery extends ChannelQueryPrivate {

        public ChannelQuery(DataSource ds, String sql) {
            super(ds, sql);
            compile();
        }
    }

    private class FindChannelQueryById extends ChannelQueryPrivate {

        public FindChannelQueryById(DataSource ds, String sql) {
            super(ds, sql);
            declareParameter(new SqlParameter(Types.BIGINT));
            compile();
        }
    }

    private class FindChannelQuery extends ChannelQueryPrivate {

        public FindChannelQuery(DataSource ds, String sql) {
            super(ds, sql);
            declareParameter(new SqlParameter(Types.VARCHAR));
            compile();
        }
    }

    private static class DeleteChannelQuery extends SqlUpdate {

        public DeleteChannelQuery(DataSource ds, String sql) {
            super(ds, sql);
            declareParameter(new SqlParameter(Types.BIGINT));
        }
    }

    private static class InsertChannel extends SqlUpdate {

        public InsertChannel(DataSource ds, String sql) {
            super(ds, sql);
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.BIGINT));
            declareParameter(new SqlParameter(Types.BIGINT));
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.INTEGER));
            declareParameter(new SqlParameter(Types.INTEGER));
            declareParameter(new SqlParameter(Types.INTEGER));
            declareParameter(new SqlParameter(Types.TINYINT));
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.INTEGER));
            declareParameter(new SqlParameter(Types.INTEGER));
            declareParameter(new SqlParameter(Types.TINYINT));
            declareParameter(new SqlParameter(Types.TINYINT));
        }

        protected void update(Channel channel) {
            this.update(new Object[] { channel.getUrl(), channel.getTitle(), channel.getDescription(), new Long(channel.getLastPolled()), new Long(channel.getCreated()), channel.getLastETag(), new Integer(channel.getStatus()), new Integer(channel.getNumberOfItems()), new Integer(channel.getNumberOfRead()), Boolean.valueOf(channel.isRemove()), channel.getLastModified(), new Integer(channel.getPreferance()), new Integer(channel.getPollsStarted()), Boolean.valueOf(channel.isHtml()), Boolean.valueOf(channel.isAutoInsert()) });
        }
    }

    private static class UpdateChannel extends InsertChannel {

        public UpdateChannel(DataSource ds, String sql) {
            super(ds, sql);
            declareParameter(new SqlParameter(Types.BIGINT));
        }

        protected void update(Channel channel) {
            this.update(new Object[] { channel.getUrl(), channel.getTitle(), channel.getDescription(), new Long(channel.getLastPolled()), new Long(channel.getCreated()), channel.getLastETag(), new Integer(channel.getStatus()), new Integer(channel.getNumberOfItems()), new Integer(channel.getNumberOfRead()), Boolean.valueOf(channel.isRemove()), channel.getLastModified(), new Integer(channel.getPreferance()), new Integer(channel.getPollsStarted()), Boolean.valueOf(channel.isHtml()), Boolean.valueOf(channel.isAutoInsert()), channel.getId() });
        }
    }
}
