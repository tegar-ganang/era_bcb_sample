package com.pehrs.mailpost.wmlblog.sql;

import com.pehrs.mailpost.wmlblog.sql.Debug;
import java.sql.*;
import java.util.*;

/** 
 * @author <a href="mailto:matti.pehrs@home.se">Matti Pehrs</a>
 * @version $Id: MP_POST_CHANNEL.java,v 1.1.1.1 2004/10/19 22:46:12 mattipehrs Exp $
 */
public class MP_POST_CHANNEL extends JdbcValueObject implements java.io.Serializable {

    public static String TABLE_NAME = TableNameLookup.getTableName("blog_post_channel");

    public MP_POST_CHANNEL() {
    }

    /** varchar user_id*/
    java.lang.String userId;

    /** varchar channel_name*/
    java.lang.String channelName;

    /** text channel_display_name*/
    java.lang.String channelDisplayName;

    /** text channel_date_format*/
    java.lang.String channelDateFormat;

    /** text loc_lang*/
    java.lang.String locLang;

    /** text loc_country*/
    java.lang.String locCountry;

    /** text ftp_host*/
    java.lang.String ftpHost;

    /** text ftp_user*/
    java.lang.String ftpUser;

    /** text ftp_passwd*/
    java.lang.String ftpPasswd;

    /** text ftp_path*/
    java.lang.String ftpPath;

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

    public java.lang.String getChannelDisplayName() {
        return channelDisplayName;
    }

    public void setChannelDisplayName(java.lang.String val) {
        channelDisplayName = val;
    }

    public java.lang.String getChannelDateFormat() {
        return channelDateFormat;
    }

    public void setChannelDateFormat(java.lang.String val) {
        channelDateFormat = val;
    }

    public java.lang.String getLocLang() {
        return locLang;
    }

    public void setLocLang(java.lang.String val) {
        locLang = val;
    }

    public java.lang.String getLocCountry() {
        return locCountry;
    }

    public void setLocCountry(java.lang.String val) {
        locCountry = val;
    }

    public java.lang.String getFtpHost() {
        return ftpHost;
    }

    public void setFtpHost(java.lang.String val) {
        ftpHost = val;
    }

    public java.lang.String getFtpUser() {
        return ftpUser;
    }

    public void setFtpUser(java.lang.String val) {
        ftpUser = val;
    }

    public java.lang.String getFtpPasswd() {
        return ftpPasswd;
    }

    public void setFtpPasswd(java.lang.String val) {
        ftpPasswd = val;
    }

    public java.lang.String getFtpPath() {
        return ftpPath;
    }

    public void setFtpPath(java.lang.String val) {
        ftpPath = val;
    }

    public boolean dbSelect(Connection dbConnection) throws SQLException {
        String query = "select " + "user_id, " + "channel_name, " + "channel_display_name, " + "channel_date_format, " + "loc_lang, " + "loc_country, " + "ftp_host, " + "ftp_user, " + "ftp_passwd, " + "ftp_path " + "from " + TABLE_NAME + " " + "where " + " channel_name = ? " + "AND " + " user_id = ? " + "";
        PreparedStatement stmt = dbConnection.prepareStatement(query);
        stmt.setString(1, getChannelName());
        stmt.setString(2, getUserId());
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
        String query = "insert into " + TABLE_NAME + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = dbConnection.prepareStatement(query);
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: " + query);
        }
        stmt.setString(1, getUserId());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [1]=" + getUserId());
        }
        stmt.setString(2, getChannelName());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [2]=" + getChannelName());
        }
        stmt.setString(3, getChannelDisplayName());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [3]=" + getChannelDisplayName());
        }
        stmt.setString(4, getChannelDateFormat());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [4]=" + getChannelDateFormat());
        }
        stmt.setString(5, getLocLang());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [5]=" + getLocLang());
        }
        stmt.setString(6, getLocCountry());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [6]=" + getLocCountry());
        }
        stmt.setString(7, getFtpHost());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [7]=" + getFtpHost());
        }
        stmt.setString(8, getFtpUser());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [8]=" + getFtpUser());
        }
        stmt.setString(9, getFtpPasswd());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [9]=" + getFtpPasswd());
        }
        stmt.setString(10, getFtpPath());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [10]=" + getFtpPath());
        }
        stmt.executeUpdate();
        stmt.close();
    }

    public void dbUpdate(Connection dbConnection) throws SQLException {
        String query = "update " + TABLE_NAME + " set " + "channel_display_name = ?, " + "channel_date_format = ?, " + "loc_lang = ?, " + "loc_country = ?, " + "ftp_host = ?, " + "ftp_user = ?, " + "ftp_passwd = ?, " + "ftp_path = ? " + " where " + " channel_name = ? " + "AND " + " user_id = ? " + "";
        PreparedStatement stmt = dbConnection.prepareStatement(query);
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: " + query);
        }
        stmt.setString(1, getChannelDisplayName());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [1]=" + getChannelDisplayName());
        }
        stmt.setString(2, getChannelDateFormat());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [2]=" + getChannelDateFormat());
        }
        stmt.setString(3, getLocLang());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [3]=" + getLocLang());
        }
        stmt.setString(4, getLocCountry());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [4]=" + getLocCountry());
        }
        stmt.setString(5, getFtpHost());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [5]=" + getFtpHost());
        }
        stmt.setString(6, getFtpUser());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [6]=" + getFtpUser());
        }
        stmt.setString(7, getFtpPasswd());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [7]=" + getFtpPasswd());
        }
        stmt.setString(8, getFtpPath());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [8]=" + getFtpPath());
        }
        stmt.setString(9, getChannelName());
        stmt.setString(10, getUserId());
        stmt.executeUpdate();
        stmt.close();
    }

    public void dbDelete(Connection dbConnection) throws SQLException {
        String query = "delete from " + TABLE_NAME + " where " + " channel_name = ? " + "AND " + " user_id = ? " + "";
        PreparedStatement stmt = dbConnection.prepareStatement(query);
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: " + query);
        }
        stmt.setString(1, getChannelName());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [1]=" + getChannelName());
        }
        stmt.setString(2, getUserId());
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [2]=" + getUserId());
        }
        stmt.executeUpdate();
        stmt.close();
    }

    public boolean dbExists(Connection dbConnection) throws SQLException {
        String query = " select channel_name, user_id from " + TABLE_NAME + " where channel_name = ? AND user_id = ? ";
        PreparedStatement stmt = dbConnection.prepareStatement(query);
        stmt.setString(1, getChannelName());
        stmt.setString(2, getUserId());
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
        if (hasColumn(meta, "channel_display_name")) {
            channelDisplayName = rs.getString("channel_display_name");
        }
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: channel_display_name=" + channelDisplayName);
        }
        if (hasColumn(meta, "channel_date_format")) {
            channelDateFormat = rs.getString("channel_date_format");
        }
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: channel_date_format=" + channelDateFormat);
        }
        if (hasColumn(meta, "loc_lang")) {
            locLang = rs.getString("loc_lang");
        }
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: loc_lang=" + locLang);
        }
        if (hasColumn(meta, "loc_country")) {
            locCountry = rs.getString("loc_country");
        }
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: loc_country=" + locCountry);
        }
        if (hasColumn(meta, "ftp_host")) {
            ftpHost = rs.getString("ftp_host");
        }
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: ftp_host=" + ftpHost);
        }
        if (hasColumn(meta, "ftp_user")) {
            ftpUser = rs.getString("ftp_user");
        }
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: ftp_user=" + ftpUser);
        }
        if (hasColumn(meta, "ftp_passwd")) {
            ftpPasswd = rs.getString("ftp_passwd");
        }
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: ftp_passwd=" + ftpPasswd);
        }
        if (hasColumn(meta, "ftp_path")) {
            ftpPath = rs.getString("ftp_path");
        }
        if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(this.getClass().getName())) {
            com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: ftp_path=" + ftpPath);
        }
    }

    public static java.util.Vector selectWhere(Connection dbConnection, String whereStmt) throws SQLException {
        return selectWhere(dbConnection, whereStmt, MP_POST_CHANNEL.class);
    }

    public static java.util.Vector selectWhere(Connection dbConnection, String whereStmt, Class subClass) throws SQLException {
        try {
            Statement stmt = dbConnection.createStatement();
            String query = "select " + "user_id, " + "channel_name, " + "channel_display_name, " + "channel_date_format, " + "loc_lang, " + "loc_country, " + "ftp_host, " + "ftp_user, " + "ftp_passwd, " + "ftp_path " + "from " + TABLE_NAME + " ";
            if (whereStmt != null) {
                query += "where " + whereStmt + " ";
            }
            if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
                com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: " + query);
            }
            java.util.Vector result = new java.util.Vector();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                MP_POST_CHANNEL vo = (MP_POST_CHANNEL) subClass.newInstance();
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
        return selectWhere(dbConnection, MP_POST_CHANNEL.class, false);
    }

    public java.util.Vector selectWhere(Connection dbConnection, boolean useLike) throws SQLException {
        return selectWhere(dbConnection, MP_POST_CHANNEL.class, useLike);
    }

    public java.util.Vector selectWhere(Connection dbConnection, Class subClass) throws SQLException {
        return selectWhere(dbConnection, subClass, false);
    }

    public java.util.Vector selectWhere(Connection dbConnection, Class subClass, boolean useLike) throws SQLException {
        try {
            String match = (useLike ? "like" : "=");
            String query = "select " + "user_id, " + "channel_name, " + "channel_display_name, " + "channel_date_format, " + "loc_lang, " + "loc_country, " + "ftp_host, " + "ftp_user, " + "ftp_passwd, " + "ftp_path " + "from " + TABLE_NAME + " ";
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
            if (channelDisplayName != null) {
                if (whereStmt == null) {
                    whereStmt = "WHERE ";
                } else {
                    whereStmt += "AND ";
                }
                whereStmt += " channel_display_name " + match + " ? ";
                values.addElement(channelDisplayName);
            }
            if (channelDateFormat != null) {
                if (whereStmt == null) {
                    whereStmt = "WHERE ";
                } else {
                    whereStmt += "AND ";
                }
                whereStmt += " channel_date_format " + match + " ? ";
                values.addElement(channelDateFormat);
            }
            if (locLang != null) {
                if (whereStmt == null) {
                    whereStmt = "WHERE ";
                } else {
                    whereStmt += "AND ";
                }
                whereStmt += " loc_lang " + match + " ? ";
                values.addElement(locLang);
            }
            if (locCountry != null) {
                if (whereStmt == null) {
                    whereStmt = "WHERE ";
                } else {
                    whereStmt += "AND ";
                }
                whereStmt += " loc_country " + match + " ? ";
                values.addElement(locCountry);
            }
            if (ftpHost != null) {
                if (whereStmt == null) {
                    whereStmt = "WHERE ";
                } else {
                    whereStmt += "AND ";
                }
                whereStmt += " ftp_host " + match + " ? ";
                values.addElement(ftpHost);
            }
            if (ftpUser != null) {
                if (whereStmt == null) {
                    whereStmt = "WHERE ";
                } else {
                    whereStmt += "AND ";
                }
                whereStmt += " ftp_user " + match + " ? ";
                values.addElement(ftpUser);
            }
            if (ftpPasswd != null) {
                if (whereStmt == null) {
                    whereStmt = "WHERE ";
                } else {
                    whereStmt += "AND ";
                }
                whereStmt += " ftp_passwd " + match + " ? ";
                values.addElement(ftpPasswd);
            }
            if (ftpPath != null) {
                if (whereStmt == null) {
                    whereStmt = "WHERE ";
                } else {
                    whereStmt += "AND ";
                }
                whereStmt += " ftp_path " + match + " ? ";
                values.addElement(ftpPath);
            }
            if (whereStmt != null) {
                query += whereStmt;
            }
            if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
                com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: " + query);
            }
            int i = 1;
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            for (Enumeration en = values.elements(); en.hasMoreElements(); i++) {
                java.lang.Object val = (java.lang.Object) en.nextElement();
                stmt.setObject(i, val);
                if (com.pehrs.mailpost.wmlblog.sql.Debug.isOn(MP_POST_CHANNEL.class.getName())) {
                    com.pehrs.mailpost.wmlblog.sql.Debug.log("SQL: value [" + i + "]=" + val);
                }
            }
            java.util.Vector result = new java.util.Vector();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                MP_POST_CHANNEL vo = (MP_POST_CHANNEL) subClass.newInstance();
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
        buff.append("{MP_POST_CHANNEL: ");
        buff.append("userId");
        buff.append("=");
        buff.append(getUserId());
        buff.append(", ");
        buff.append("channelName");
        buff.append("=");
        buff.append(getChannelName());
        buff.append(", ");
        buff.append("channelDisplayName");
        buff.append("=");
        buff.append(getChannelDisplayName());
        buff.append(", ");
        buff.append("channelDateFormat");
        buff.append("=");
        buff.append(getChannelDateFormat());
        buff.append(", ");
        buff.append("locLang");
        buff.append("=");
        buff.append(getLocLang());
        buff.append(", ");
        buff.append("locCountry");
        buff.append("=");
        buff.append(getLocCountry());
        buff.append(", ");
        buff.append("ftpHost");
        buff.append("=");
        buff.append(getFtpHost());
        buff.append(", ");
        buff.append("ftpUser");
        buff.append("=");
        buff.append(getFtpUser());
        buff.append(", ");
        buff.append("ftpPasswd");
        buff.append("=");
        buff.append(getFtpPasswd());
        buff.append(", ");
        buff.append("ftpPath");
        buff.append("=");
        buff.append(getFtpPath());
        buff.append("}");
        return buff.toString();
    }
}
