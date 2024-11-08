package com.spring.rssReader.jdbc;

import com.spring.rssReader.Channel;
import com.spring.rssReader.Item;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.object.SqlUpdate;
import javax.sql.DataSource;
import java.sql.Types;
import java.util.List;

/**
 * @author Ronald
 * Date: Dec 1, 2003
 * Time: 12:47:16 PM
 */
public class ItemDAOJdbc extends JdbcDaoSupport implements IItemDAO {

    private static final String MARK_ITEMS_AS_READ = "update item set articleRead=1 where channelID=?";

    private static final String REMOVE_ITEM = "update item set remove=1, description=null where itemID=?";

    private static final String GET_ITEM = "select * from item where itemID=?";

    private static final String INSERT_ITEM = "insert into item (" + "title" + ",url" + ",description" + ",comments" + ",postedDate" + ",articleRead" + ",channelID" + ",lastModified" + ",lastETag" + ",remove" + ",preferance" + ",fetched) values (?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String UPDATE_ITEM = "update item set" + " title=?" + ",url=?" + ",description=?" + ",comments=?" + ",postedDate=?" + ",articleRead=?" + ",channelID=?" + ",lastModified=?" + ",lastETag=?" + ",remove=?" + ",preferance=?" + ",fetched=?" + " where itemID=?";

    private static final String FIND_ITEM_BY_URL = "select * from item where url=? and channelID=?";

    private static final String DELETE_ITEM_FROM_CHANNEL = "delete from item where channelID=?";

    private static final String FIND_ITEMS_FROM_CHANNEL = "select * from item where channelID=? and remove=0 order by postedDate desc, itemID";

    private static final String FIND_NEW_ITEMS_FROM_CHANNEL = "select * from item where channelID=? and remove=0 and articleRead=0 order by postedDate desc, itemID";

    private RemoveItemQuery deleteItemQuery = null;

    private FindItemsQuery findItemsQuery = null;

    private RemoveItemQuery updateItemsAsRead = null;

    private InsertItemQuery insertItemQuery = null;

    private UpdateItemQuery updateItemQuery = null;

    private FindItemsByURLQuery findItemsByURLQuery = null;

    private RemoveItemQuery deleteItemsFromChannelQuery = null;

    private FindItemsQuery channelNewItemsQuery = null;

    private FindItemsQuery channelItemsQuery = null;

    private ILastInsertDAO lastInsertDAO = null;

    public ItemDAOJdbc() {
    }

    public ILastInsertDAO getLastInsertDAO() {
        return lastInsertDAO;
    }

    public void setLastInsertDAO(ILastInsertDAO lastInsertDAO) {
        this.lastInsertDAO = lastInsertDAO;
    }

    protected void initDao() throws Exception {
        super.initDao();
        if (findItemsQuery == null) {
            findItemsQuery = new FindItemsQuery(super.getDataSource(), GET_ITEM);
        }
        if (deleteItemQuery == null) {
            deleteItemQuery = new RemoveItemQuery(super.getDataSource(), REMOVE_ITEM);
        }
        if (updateItemsAsRead == null) {
            updateItemsAsRead = new RemoveItemQuery(super.getDataSource(), MARK_ITEMS_AS_READ);
        }
        if (insertItemQuery == null) {
            insertItemQuery = new InsertItemQuery(super.getDataSource(), INSERT_ITEM);
        }
        if (updateItemQuery == null) {
            updateItemQuery = new UpdateItemQuery(super.getDataSource(), UPDATE_ITEM);
        }
        if (findItemsByURLQuery == null) {
            findItemsByURLQuery = new FindItemsByURLQuery(super.getDataSource(), FIND_ITEM_BY_URL);
        }
        if (deleteItemsFromChannelQuery == null) {
            deleteItemsFromChannelQuery = new RemoveItemQuery(super.getDataSource(), DELETE_ITEM_FROM_CHANNEL);
        }
        if (channelItemsQuery == null) {
            channelItemsQuery = new FindItemsQuery(super.getDataSource(), FIND_ITEMS_FROM_CHANNEL);
        }
        if (channelNewItemsQuery == null) {
            channelNewItemsQuery = new FindItemsQuery(super.getDataSource(), FIND_NEW_ITEMS_FROM_CHANNEL);
        }
    }

    public void deleteItem(Item item) {
        deleteItemQuery.update(new Object[] { item.getId() });
    }

    public List getAllItems(Long channelID) {
        return channelItemsQuery.execute(new Object[] { channelID });
    }

    public List getNewItems(Long channelID) {
        return channelNewItemsQuery.execute(new Object[] { channelID });
    }

    public Item getItem(Long itemId) {
        List items = findItemsQuery.execute(new Object[] { itemId });
        if (items.size() > 0) {
            return (Item) items.get(0);
        }
        return null;
    }

    public void markItemsAsRead(Long id) {
        updateItemsAsRead.update(new Object[] { id });
    }

    public void insert(Item item) {
        insertItemQuery.update(item);
        item.setId(lastInsertDAO.getLatestId());
    }

    public void update(Item item) {
        updateItemQuery.update(item);
    }

    public Item findItemByUrl(String url, Long id) {
        List items = findItemsByURLQuery.execute(new Object[] { url, id });
        if (items.size() > 0) {
            return (Item) items.get(0);
        }
        return null;
    }

    public void deleteItemsFromChannel(Channel channel) {
        deleteItemsFromChannelQuery.update(new Object[] { channel.getId() });
    }

    private static class RemoveItemQuery extends SqlUpdate {

        public RemoveItemQuery(DataSource ds, String sql) {
            super(ds, sql);
            declareParameter(new SqlParameter(Types.BIGINT));
            compile();
        }
    }

    private class UpdateItemQuery extends InsertItemQuery {

        public UpdateItemQuery(DataSource ds, String sql) {
            super(ds, sql);
            declareParameter(new SqlParameter(Types.BIGINT));
            compile();
        }

        public int update(Item item) throws InvalidDataAccessApiUsageException {
            return super.update(new Object[] { item.getTitle(), item.getUrl(), item.getDescription(), item.getComments(), new Long(item.getPostedDate()), Boolean.valueOf(item.isArticleRead()), item.getChannelID(), item.getLastModified(), item.getLastETag(), Boolean.valueOf(item.isRemove()), new Integer(item.getPreferance()), Boolean.valueOf(item.isFetched()), item.getId() });
        }
    }

    private class InsertItemQuery extends SqlUpdate {

        public InsertItemQuery(DataSource ds, String sql) {
            super(ds, sql);
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.BIGINT));
            declareParameter(new SqlParameter(Types.TINYINT));
            declareParameter(new SqlParameter(Types.BIGINT));
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.TINYINT));
            declareParameter(new SqlParameter(Types.BIGINT));
            declareParameter(new SqlParameter(Types.TINYINT));
        }

        public int update(Item item) throws InvalidDataAccessApiUsageException {
            return super.update(new Object[] { item.getTitle(), item.getUrl(), item.getDescription(), item.getComments(), new Long(item.getPostedDate()), Boolean.valueOf(item.isArticleRead()), item.getChannelID(), item.getLastModified(), item.getLastETag(), Boolean.valueOf(item.isRemove()), new Integer(item.getPreferance()), Boolean.valueOf(item.isFetched()) });
        }
    }

    public static class FindItemsByURLQuery extends ItemsQuery {

        public FindItemsByURLQuery() {
        }

        public FindItemsByURLQuery(DataSource ds, String sql) {
            super(ds, sql);
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.BIGINT));
            compile();
        }
    }
}
