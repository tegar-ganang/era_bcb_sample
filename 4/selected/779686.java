package com.pehrs.mailpost.wmlblog.sql;

import com.pehrs.mailpost.wmlblog.sql.Debug;
import java.sql.*;
import java.util.*;

/** 
 * @author <a href="mailto:matti.pehrs@home.se">Matti Pehrs</a>
 * @version $Id: MP_POST.java,v 1.1.1.1 2004/10/19 22:46:12 mattipehrs Exp $
 */
public class MP_POST extends JdbcValueObject implements java.io.Serializable {

    public static String TABLE_NAME = TableNameLookup.getTableName("blog_post");

    public MP_POST() {
    }

    /** varchar user_id*/
    java.lang.String userId;

    /** varchar channel_name*/
    java.lang.String channelName;

    /** text post_email*/
    java.lang.String postEmail;

    /** timestamp post_date*/
    java.sql.Timestamp postDate;

    /** text title*/
    java.lang.String title;

    /** text description*/
    java.lang.String description;

    /** text image_mime_type*/
    java.lang.String imageMimeType;

    public java.lang.String getUserId() {
        return userId;
    }

    public void setUserId(java.lang.String val) {
        userId = val;
    }

    public java.lang.String getChannelName() {
        return channelName;
    }

    public void setChannelName(java.lang.String val) {
        channelName = val;
    }

    public java.lang.String getPostEmail() {
        return postEmail;
    }

    public void setPostEmail(java.lang.String val) {
        postEmail = val;
    }

    public String getPostDateAsString() {
        if (postDate == null) {
            return null;
        }
        return df.format(postDate);
    }

    public void setPostDate(String val) throws java.text.ParseException {
        if (val == null) {
            postDate = null;
        } else {
            postDate = new java.sql.Timestamp(df.parse(val).getTime());
        }
    }

    public java.sql.Timestamp getPostDate() {
        return postDate;
    }

    public void setPostDate(java.sql.Timestamp val) {
        postDate = val;
    }

    public java.lang.String getTitle() {
        return title;
    }

    public void setTitle(java.lang.String val) {
        title = val;
    }

    public java.lang.String getDescription() {
        return description;
    }

    public void setDescription(java.lang.String val) {
        description = val;
    }

    public java.lang.String getImageMimeType() {
        return imageMimeType;
    }

    public void setImageMimeType(java.lang.String val) {
        imageMimeType = val;
    }

    public boolean dbSelect(Connection dbConnection) throws SQLException {
        String query = "select " + "user_id, " + "channel_name, " + "post_email, " + "post_date, " + "title, " + "description, " + "image_mime_type " + "from " + TABLE_NAME + " " + "where " + " channel_name = ? " + "AND " + " post_date = ? " + "AND " + " user_id = ? " + "";
        PreparedStatement stmt = dbConnection.prepareStatement(query);
        stmt.setString(1, getChannelName());
        stmt.setTimestamp(2, getPostDate());
        stmt.setString(3, getUserId());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: " + query);
        }
        ResultSet rs = stmt.executeQuery();
        boolean hasNext = rs.next();
        if (!hasNext) {
            stmt.close();
            return false;
        }
        setValues(rs);
        stmt.close();
        return true;
    }

    public void dbInsert(Connection dbConnection) throws SQLException {
        String query = "insert into " + TABLE_NAME + " values (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = dbConnection.prepareStatement(query);
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: " + query);
        }
        stmt.setString(1, getUserId());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [1]=" + getUserId());
        }
        stmt.setString(2, getChannelName());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [2]=" + getChannelName());
        }
        stmt.setString(3, getPostEmail());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [3]=" + getPostEmail());
        }
        stmt.setTimestamp(4, getPostDate());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [4]=" + getPostDate());
        }
        stmt.setString(5, getTitle());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [5]=" + getTitle());
        }
        stmt.setString(6, getDescription());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [6]=" + getDescription());
        }
        stmt.setString(7, getImageMimeType());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [7]=" + getImageMimeType());
        }
        stmt.executeUpdate();
        stmt.close();
    }

    public void dbUpdate(Connection dbConnection) throws SQLException {
        String query = "update " + TABLE_NAME + " set " + "post_email = ?, " + "title = ?, " + "description = ?, " + "image_mime_type = ? " + " where " + " channel_name = ? " + "AND " + " post_date = ? " + "AND " + " user_id = ? " + "";
        PreparedStatement stmt = dbConnection.prepareStatement(query);
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: " + query);
        }
        stmt.setString(1, getPostEmail());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [1]=" + getPostEmail());
        }
        stmt.setString(2, getTitle());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [2]=" + getTitle());
        }
        stmt.setString(3, getDescription());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [3]=" + getDescription());
        }
        stmt.setString(4, getImageMimeType());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [4]=" + getImageMimeType());
        }
        stmt.setString(5, getChannelName());
        stmt.setTimestamp(6, getPostDate());
        stmt.setString(7, getUserId());
        stmt.executeUpdate();
        stmt.close();
    }

    public void dbDelete(Connection dbConnection) throws SQLException {
        String query = "delete from " + TABLE_NAME + " where " + " channel_name = ? " + "AND " + " post_date = ? " + "AND " + " user_id = ? " + "";
        PreparedStatement stmt = dbConnection.prepareStatement(query);
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: " + query);
        }
        stmt.setString(1, getChannelName());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [1]=" + getChannelName());
        }
        stmt.setTimestamp(2, getPostDate());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [2]=" + getPostDate());
        }
        stmt.setString(3, getUserId());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [3]=" + getUserId());
        }
        stmt.executeUpdate();
        stmt.close();
    }

    public boolean dbExists(Connection dbConnection) throws SQLException {
        String query = " select channel_name, post_date, user_id from " + TABLE_NAME + " where channel_name = ? AND post_date = ? AND user_id = ? ";
        PreparedStatement stmt = dbConnection.prepareStatement(query);
        stmt.setString(1, getChannelName());
        stmt.setTimestamp(2, getPostDate());
        stmt.setString(3, getUserId());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: " + query);
        }
        ResultSet rs = stmt.executeQuery();
        int num = 0;
        while (rs.next()) {
            num++;
        }
        stmt.close();
        return num != 0;
    }

    /** Note: Result values have to be in declared order */
    public void setValues(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        if (hasColumn(meta, "user_id")) {
            userId = rs.getString("user_id");
        }
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: user_id=" + userId);
        }
        if (hasColumn(meta, "channel_name")) {
            channelName = rs.getString("channel_name");
        }
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: channel_name=" + channelName);
        }
        if (hasColumn(meta, "post_email")) {
            postEmail = rs.getString("post_email");
        }
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: post_email=" + postEmail);
        }
        if (hasColumn(meta, "post_date")) {
            postDate = rs.getTimestamp("post_date");
        }
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: post_date=" + postDate);
        }
        if (hasColumn(meta, "title")) {
            title = rs.getString("title");
        }
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: title=" + title);
        }
        if (hasColumn(meta, "description")) {
            description = rs.getString("description");
        }
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: description=" + description);
        }
        if (hasColumn(meta, "image_mime_type")) {
            imageMimeType = rs.getString("image_mime_type");
        }
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: image_mime_type=" + imageMimeType);
        }
    }

    public static java.util.Vector selectWhere(Connection dbConnection, String whereStmt) throws SQLException {
        return selectWhere(dbConnection, whereStmt, MP_POST.class);
    }

    public static java.util.Vector selectWhere(Connection dbConnection, String whereStmt, Class subClass) throws SQLException {
        try {
            Statement stmt = dbConnection.createStatement();
            String query = "select " + "user_id, " + "channel_name, " + "post_email, " + "post_date, " + "title, " + "description, " + "image_mime_type " + "from " + TABLE_NAME + " ";
            if (whereStmt != null) {
                query += "where " + whereStmt + " ";
            }
            if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST.class.getName())) {
                com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: " + query);
            }
            java.util.Vector result = new java.util.Vector();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                MP_POST vo = (MP_POST) subClass.newInstance();
                vo.setValues(rs);
                result.addElement(vo);
            }
            stmt.close();
            return result;
        } catch (InstantiationException ex) {
            throw new SQLException("InstantiationException: " + ex.getMessage());
        } catch (IllegalAccessException ex) {
            throw new SQLException("IllegalAccessException: " + ex.getMessage());
        }
    }

    public java.util.Vector selectWhere(Connection dbConnection) throws SQLException {
        return selectWhere(dbConnection, MP_POST.class, false);
    }

    public java.util.Vector selectWhere(Connection dbConnection, boolean useLike) throws SQLException {
        return selectWhere(dbConnection, MP_POST.class, useLike);
    }

    public java.util.Vector selectWhere(Connection dbConnection, Class subClass) throws SQLException {
        return selectWhere(dbConnection, subClass, false);
    }

    public java.util.Vector selectWhere(Connection dbConnection, Class subClass, boolean useLike) throws SQLException {
        try {
            String match = (useLike ? "like" : "=");
            String query = "select " + "user_id, " + "channel_name, " + "post_email, " + "post_date, " + "title, " + "description, " + "image_mime_type " + "from " + TABLE_NAME + " ";
            String whereStmt = null;
            Vector values = new Vector();
            if (userId != null) {
                if (whereStmt == null) {
                    whereStmt = "WHERE ";
                } else {
                    whereStmt += "AND ";
                }
                whereStmt += " user_id " + match + " ? ";
                values.addElement(userId);
            }
            if (channelName != null) {
                if (whereStmt == null) {
                    whereStmt = "WHERE ";
                } else {
                    whereStmt += "AND ";
                }
                whereStmt += " channel_name " + match + " ? ";
                values.addElement(channelName);
            }
            if (postEmail != null) {
                if (whereStmt == null) {
                    whereStmt = "WHERE ";
                } else {
                    whereStmt += "AND ";
                }
                whereStmt += " post_email " + match + " ? ";
                values.addElement(postEmail);
            }
            if (postDate != null) {
                if (whereStmt == null) {
                    whereStmt = "WHERE ";
                } else {
                    whereStmt += "AND ";
                }
                whereStmt += " post_date " + match + " ? ";
                values.addElement(postDate);
            }
            if (title != null) {
                if (whereStmt == null) {
                    whereStmt = "WHERE ";
                } else {
                    whereStmt += "AND ";
                }
                whereStmt += " title " + match + " ? ";
                values.addElement(title);
            }
            if (description != null) {
                if (whereStmt == null) {
                    whereStmt = "WHERE ";
                } else {
                    whereStmt += "AND ";
                }
                whereStmt += " description " + match + " ? ";
                values.addElement(description);
            }
            if (imageMimeType != null) {
                if (whereStmt == null) {
                    whereStmt = "WHERE ";
                } else {
                    whereStmt += "AND ";
                }
                whereStmt += " image_mime_type " + match + " ? ";
                values.addElement(imageMimeType);
            }
            if (whereStmt != null) {
                query += whereStmt;
            }
            if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST.class.getName())) {
                com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: " + query);
            }
            int i = 1;
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            for (Enumeration en = values.elements(); en.hasMoreElements(); i++) {
                java.lang.Object val = (java.lang.Object) en.nextElement();
                stmt.setObject(i, val);
                if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST.class.getName())) {
                    com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [" + i + "]=" + val);
                }
            }
            java.util.Vector result = new java.util.Vector();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                MP_POST vo = (MP_POST) subClass.newInstance();
                vo.setValues(rs);
                result.addElement(vo);
            }
            stmt.close();
            return result;
        } catch (InstantiationException ex) {
            throw new SQLException("InstantiationException: " + ex.getMessage());
        } catch (IllegalAccessException ex) {
            throw new SQLException("IllegalAccessException: " + ex.getMessage());
        }
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("{MP_POST: ");
        buff.append("userId");
        buff.append("=");
        buff.append(getUserId());
        buff.append(", ");
        buff.append("channelName");
        buff.append("=");
        buff.append(getChannelName());
        buff.append(", ");
        buff.append("postEmail");
        buff.append("=");
        buff.append(getPostEmail());
        buff.append(", ");
        buff.append("postDate");
        buff.append("=");
        buff.append(getPostDate());
        buff.append(", ");
        buff.append("title");
        buff.append("=");
        buff.append(getTitle());
        buff.append(", ");
        buff.append("description");
        buff.append("=");
        buff.append(getDescription());
        buff.append(", ");
        buff.append("imageMimeType");
        buff.append("=");
        buff.append(getImageMimeType());
        buff.append("}");
        return buff.toString();
    }
}
