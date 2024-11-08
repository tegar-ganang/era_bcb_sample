package net.sourceforge.thinfeeder.model.dao;

import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.thinfeeder.model.Database;
import net.sourceforge.thinfeeder.util.Utils;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.impl.basic.Item;

/**
 * @author fabianofranz@users.sourceforge.net
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DAOItem {

    public static ItemIF getItem(long id) throws Exception {
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT ITEM_ID, CHANNEL_ID, TITLE, DESCRIPTION, UNREAD, LINK, CREATOR, SUBJECT, DATE, FOUND, GUID, COMMENTS, SOURCE, ENCLOSURE ");
        sql.append("FROM ITEMS WHERE ITEM_ID = ?");
        Connection con = Database.getInstance().getConnection();
        PreparedStatement stmt = con.prepareStatement(sql.toString());
        stmt.setLong(1, id);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            ItemIF item = new Item();
            item.setId(rs.getLong("ITEM_ID"));
            item.setChannel(DAOChannel.getChannel(rs.getLong("CHANNEL_ID")));
            item.setTitle(rs.getString("TITLE"));
            item.setDescription(rs.getString("DESCRIPTION"));
            item.setUnRead(rs.getInt("UNREAD") != 0);
            item.setLink(rs.getObject("LINK") == null ? null : (URL) Utils.deserialize(rs.getBytes("LINK")));
            item.setCreator(rs.getString("CREATOR"));
            item.setSubject(rs.getString("SUBJECT"));
            item.setDate(rs.getDate("DATE"));
            item.setFound(rs.getDate("FOUND"));
            item.setGuid(rs.getObject("GUID") == null ? null : DAOItemGuid.getItemGuid(rs.getInt("GUID"), item));
            item.setComments(rs.getObject("COMMENTS") == null ? null : (URL) Utils.deserialize(rs.getBytes("COMMENTS")));
            item.setSource(rs.getObject("SOURCE") == null ? null : DAOItemSource.getItemSource(rs.getInt("SOURCE"), item));
            item.setEnclosure(rs.getObject("ENCLOSURE") == null ? null : DAOItemEnclosure.getItemEnclosure(rs.getInt("SOURCE"), item));
            rs.close();
            stmt.close();
            return item;
        } else return null;
    }

    public static List getItems(ChannelIF channel, String order) throws Exception {
        if (channel == null) return null;
        List r = new ArrayList();
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT ITEM_ID, CHANNEL_ID, TITLE, DESCRIPTION, UNREAD, LINK, CREATOR, SUBJECT, DATE, FOUND, GUID, COMMENTS, SOURCE, ENCLOSURE, TIMESTAMP ");
        sql.append("FROM ITEMS WHERE CHANNEL_ID = ?");
        if (order != null) {
            sql.append(" ORDER BY ");
            sql.append(order);
        }
        Connection con = Database.getInstance().getConnection();
        PreparedStatement stmt = con.prepareStatement(sql.toString());
        stmt.setLong(1, channel.getId());
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            ItemIF item = new Item();
            item.setId(rs.getLong("ITEM_ID"));
            item.setChannel(channel);
            item.setTitle(rs.getString("TITLE"));
            item.setDescription(rs.getString("DESCRIPTION"));
            item.setUnRead(rs.getInt("UNREAD") != 0);
            item.setLink(rs.getObject("LINK") == null ? null : (URL) Utils.deserialize(rs.getBytes("LINK")));
            item.setCreator(rs.getString("CREATOR"));
            item.setSubject(rs.getString("SUBJECT"));
            item.setDate(rs.getDate("DATE"));
            item.setFound(rs.getDate("FOUND"));
            item.setGuid(rs.getObject("GUID") == null ? null : DAOItemGuid.getItemGuid(rs.getInt("GUID"), item));
            item.setComments(rs.getObject("COMMENTS") == null ? null : (URL) Utils.deserialize(rs.getBytes("COMMENTS")));
            item.setSource(rs.getObject("SOURCE") == null ? null : DAOItemSource.getItemSource(rs.getInt("SOURCE"), item));
            item.setEnclosure(rs.getObject("ENCLOSURE") == null ? null : DAOItemEnclosure.getItemEnclosure(rs.getInt("SOURCE"), item));
            r.add(item);
        }
        rs.close();
        stmt.close();
        return r;
    }

    public static List getItemsByDateDesc(ChannelIF channel) throws Exception {
        return DAOItem.getItems(channel, "DATE DESC");
    }

    public static List getItemsByIdDesc(ChannelIF channel) throws Exception {
        return DAOItem.getItems(channel, "ITEM_ID DESC");
    }

    public static List getItemsByTimestampDesc(ChannelIF channel) throws Exception {
        return DAOItem.getItems(channel, "TIMESTAMP DESC");
    }

    public static void updateItem(ItemIF item) throws Exception {
        StringBuffer sql = new StringBuffer();
        sql.append("UPDATE ITEMS SET ");
        sql.append("CHANNEL_ID = ?, ");
        sql.append("TITLE = ?, ");
        sql.append("DESCRIPTION = ?, ");
        sql.append("UNREAD = ?, ");
        sql.append("LINK = ?, ");
        sql.append("CREATOR = ?, ");
        sql.append("SUBJECT = ?, ");
        sql.append("DATE = ?, ");
        sql.append("FOUND = ?, ");
        sql.append("GUID = ?, ");
        sql.append("COMMENTS = ?, ");
        sql.append("SOURCE = ?, ");
        sql.append("ENCLOSURE = ? ");
        sql.append("WHERE ITEM_ID = ? ");
        Connection con = Database.getInstance().getConnection();
        PreparedStatement stmt = con.prepareStatement(sql.toString());
        stmt.setLong(1, item.getChannel().getId());
        stmt.setString(2, Utils.stripToSafeDatabaseString(item.getTitle()));
        stmt.setString(3, Utils.stripToSafeDatabaseString(item.getDescription()));
        stmt.setInt(4, item.getUnRead() ? 1 : 0);
        stmt.setBytes(5, item.getLink() == null ? null : Utils.serialize(item.getLink()));
        stmt.setString(6, Utils.stripToSafeDatabaseString(item.getCreator()));
        stmt.setString(7, Utils.stripToSafeDatabaseString(item.getSubject()));
        stmt.setDate(8, item.getDate() == null ? null : new Date(item.getDate().getTime()));
        stmt.setDate(9, item.getFound() == null ? null : new Date(item.getFound().getTime()));
        stmt.setObject(10, null);
        stmt.setBytes(11, item.getComments() == null ? null : Utils.serialize(item.getComments()));
        stmt.setObject(12, null);
        stmt.setObject(13, null);
        stmt.setLong(14, item.getId());
        stmt.executeUpdate();
        stmt.close();
    }

    public static void addItem(ItemIF item) throws Exception {
        StringBuffer sql = new StringBuffer();
        sql.append("INSERT INTO ITEMS ");
        sql.append("(ITEM_ID, CHANNEL_ID, TITLE, DESCRIPTION, UNREAD, LINK, CREATOR, SUBJECT, DATE, FOUND, GUID, COMMENTS, SOURCE, ENCLOSURE) ");
        sql.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        Connection con = Database.getInstance().getConnection();
        PreparedStatement stmt = con.prepareStatement(sql.toString());
        stmt.setLong(1, item.getId());
        stmt.setLong(2, item.getChannel().getId());
        stmt.setString(3, Utils.stripToSafeDatabaseString(item.getTitle()));
        stmt.setString(4, Utils.stripToSafeDatabaseString(item.getDescription()));
        stmt.setInt(5, item.getUnRead() ? 1 : 0);
        stmt.setBytes(6, item.getLink() == null ? null : Utils.serialize(item.getLink()));
        stmt.setString(7, Utils.stripToSafeDatabaseString(item.getCreator()));
        stmt.setString(8, Utils.stripToSafeDatabaseString(item.getSubject()));
        stmt.setDate(9, item.getDate() == null ? null : new Date(item.getDate().getTime()));
        stmt.setDate(10, item.getFound() == null ? null : new Date(item.getFound().getTime()));
        stmt.setObject(11, null);
        stmt.setBytes(12, item.getComments() == null ? null : Utils.serialize(item.getComments()));
        stmt.setObject(13, null);
        stmt.setObject(14, null);
        stmt.executeUpdate();
        stmt.close();
    }

    public static void removeItem(ItemIF item) throws Exception {
        StringBuffer sql = new StringBuffer();
        sql.append("DELETE FROM ITEMS ");
        sql.append("WHERE ITEM_ID = ?");
        Connection con = Database.getInstance().getConnection();
        PreparedStatement stmt = con.prepareStatement(sql.toString());
        stmt.setLong(1, item.getId());
        stmt.executeUpdate();
        stmt.close();
    }

    public static void markUnreadItems(long[] ids, boolean unread) throws Exception {
        StringBuffer sql = new StringBuffer();
        sql.append("UPDATE ITEMS SET ");
        sql.append("UNREAD = ? ");
        sql.append("WHERE ITEM_ID IN ( ");
        for (int i = 0; i < ids.length; i++) {
            sql.append(ids[i]);
            if ((ids.length - 1) != i) sql.append(", ");
        }
        sql.append(" )");
        Connection con = Database.getInstance().getConnection();
        PreparedStatement stmt = con.prepareStatement(sql.toString());
        stmt.setInt(1, unread ? 1 : 0);
        stmt.executeUpdate();
        stmt.close();
    }
}
