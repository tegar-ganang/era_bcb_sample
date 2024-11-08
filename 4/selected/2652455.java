package com.wwwc.index.ejb.directory;

import java.sql.*;
import javax.sql.*;
import java.util.*;
import java.math.*;
import javax.ejb.*;
import javax.naming.*;
import com.wwwc.index.servlet.DirectoryDetails;

public class DirectoryBean implements SessionBean {

    private Connection con;

    public void ejbCreate() throws CreateException {
    }

    public void ejbRemove() {
    }

    public void ejbActivate() {
    }

    public void ejbPassivate() {
    }

    public void setSessionContext(SessionContext sc) {
    }

    public int tableExists(String table_name) {
        int found = 0;
        try {
            makeConnection();
            String query = "SELECT TABLE_NAME FROM SYSTEM_TABLES WHERE TABLE_NAME='" + table_name + "';";
            PreparedStatement prepStmt = con.prepareStatement(query);
            ResultSet rs = prepStmt.executeQuery();
            while (rs.next()) {
                if ((rs.getString(1)).equals(table_name)) {
                    found = 1;
                }
            }
            rs.close();
            prepStmt.close();
            releaseConnection();
        } catch (Exception e) {
            found = -1;
            System.out.println("DirectoryBean:tableExists:Error:" + e.getMessage());
        }
        return found;
    }

    public String createDirectoryTable(String table_name) {
        StringBuffer sbf = new StringBuffer();
        sbf.append("CREATE TABLE " + table_name + "(");
        sbf.append("ID VARCHAR(100) PRIMARY KEY,");
        sbf.append("TYPE INTEGER,");
        sbf.append("NAME VARCHAR_IGNORECASE(100),");
        sbf.append("LINK_CONTEXT VARCHAR(100),");
        sbf.append("LINK VARCHAR(100),");
        sbf.append("PARENT_ID VARCHAR(100),");
        sbf.append("GROUP_ID VARCHAR(100),");
        sbf.append("GROUP_NAME VARCHAR(100),");
        sbf.append("DIRECTORY_LEVEL INTEGER,");
        sbf.append("DIRECTORY_AGE INTEGER,");
        sbf.append("PREVIEW_LEVEL INTEGER,");
        sbf.append("PREVIEW_AGE INTEGER,");
        sbf.append("READ_LEVEL INTEGER,");
        sbf.append("READ_AGE INTEGER,");
        sbf.append("WRITE_LEVEL INTEGER,");
        sbf.append("WRITE_AGE INTEGER,");
        sbf.append("MANAGERS VARCHAR_IGNORECASE(500),");
        sbf.append("ADMINS VARCHAR_IGNORECASE(500));");
        String query = sbf.toString();
        try {
            makeConnection();
            PreparedStatement prepStmt = con.prepareStatement(query);
            ResultSet rs = prepStmt.executeQuery();
            rs.close();
            prepStmt.close();
            releaseConnection();
        } catch (Exception e) {
            return (e.getMessage());
        }
        return "Excuting :" + query + "....\n Result:OK";
    }

    public int insertDefaultDirectoryValue(String table_name, String context, int cid, String cname, String admin) {
        PreparedStatement prepStmt = null;
        try {
            StringBuffer sbf = new StringBuffer();
            sbf.append("INSERT INTO " + table_name + "(");
            sbf.append("ID,");
            sbf.append("TYPE,");
            sbf.append("NAME,");
            sbf.append("LINK_CONTEXT,");
            sbf.append("LINK,");
            sbf.append("PARENT_ID,");
            sbf.append("GROUP_ID,");
            sbf.append("GROUP_NAME,");
            sbf.append("DIRECTORY_LEVEL,");
            sbf.append("DIRECTORY_AGE,");
            sbf.append("PREVIEW_LEVEL,");
            sbf.append("PREVIEW_AGE,");
            sbf.append("READ_LEVEL,");
            sbf.append("READ_AGE,");
            sbf.append("WRITE_LEVEL,");
            sbf.append("WRITE_AGE,");
            sbf.append("MANAGERS,");
            sbf.append("ADMINS)");
            sbf.append("VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
            makeConnection();
            prepStmt = con.prepareStatement(sbf.toString());
            prepStmt.setString(1, "1");
            prepStmt.setInt(2, 0);
            prepStmt.setString(3, "Admin");
            prepStmt.setString(4, context);
            prepStmt.setString(5, "/AdminServlet?table_name=" + table_name);
            prepStmt.setString(6, "0");
            prepStmt.setString(7, cid + "");
            prepStmt.setString(8, cname);
            prepStmt.setInt(9, 99);
            prepStmt.setInt(10, 0);
            prepStmt.setInt(11, 99);
            prepStmt.setInt(12, 0);
            prepStmt.setInt(13, 99);
            prepStmt.setInt(14, 0);
            prepStmt.setInt(15, 99);
            prepStmt.setInt(16, 0);
            prepStmt.setString(17, "");
            prepStmt.setString(18, admin);
            prepStmt.executeUpdate();
            prepStmt.setString(1, "1-10");
            prepStmt.setInt(2, 1);
            prepStmt.setString(3, "Language");
            prepStmt.setString(4, context);
            prepStmt.setString(5, "/AdminServlet?table_name=" + table_name);
            prepStmt.setString(6, "1");
            prepStmt.setString(7, cid + "");
            prepStmt.setString(8, cname);
            prepStmt.setInt(9, 99);
            prepStmt.setInt(10, 0);
            prepStmt.setInt(11, 99);
            prepStmt.setInt(12, 0);
            prepStmt.setInt(13, 99);
            prepStmt.setInt(14, 0);
            prepStmt.setInt(15, 99);
            prepStmt.setInt(16, 0);
            prepStmt.setString(17, "");
            prepStmt.setString(18, admin);
            prepStmt.executeUpdate();
            prepStmt.setString(1, "1-20");
            prepStmt.setInt(2, 1);
            prepStmt.setString(3, "Category");
            prepStmt.setString(4, context);
            prepStmt.setString(5, "/AdminServlet?table_name=" + table_name);
            prepStmt.setString(6, "1");
            prepStmt.setString(7, cid + "");
            prepStmt.setString(8, cname);
            prepStmt.setInt(9, 99);
            prepStmt.setInt(10, 0);
            prepStmt.setInt(11, 99);
            prepStmt.setInt(12, 0);
            prepStmt.setInt(13, 99);
            prepStmt.setInt(14, 0);
            prepStmt.setInt(15, 99);
            prepStmt.setInt(16, 0);
            prepStmt.setString(17, "");
            prepStmt.setString(18, admin);
            prepStmt.executeUpdate();
            prepStmt.close();
            releaseConnection();
        } catch (Exception e) {
            System.out.println("CategoryBean:insertDefaultDirectoryValue:Error:" + e);
            return 0;
        }
        return 1;
    }

    public Vector getDirectoryList(String table_name, int level) {
        if (tableExists(table_name) != 1) {
            return null;
        }
        Vector v = null;
        DirectoryDetails dd = null;
        try {
            makeConnection();
            String query = "SELECT ID, TYPE, NAME, LINK_CONTEXT, LINK, PARENT_ID," + "GROUP_ID, GROUP_NAME, DIRECTORY_LEVEL, DIRECTORY_AGE, PREVIEW_LEVEL, PREVIEW_AGE, READ_LEVEL, READ_AGE," + "WRITE_LEVEL, WRITE_AGE, MANAGERS, ADMINS FROM ? WHERE PREVIEW_LEVEL <= ? ORDER BY ID;";
            v = new Vector();
            PreparedStatement prepStmt = con.prepareStatement(query);
            prepStmt.setString(1, table_name);
            prepStmt.setInt(2, level);
            ResultSet rs = prepStmt.executeQuery();
            while (rs.next()) {
                dd = new DirectoryDetails();
                dd.setId(rs.getString(1));
                dd.setPositionX(rs.getString(1));
                dd.setType(rs.getInt(2));
                dd.setName(rs.getString(3));
                dd.setLinkContext(rs.getString(4));
                dd.setLink(rs.getString(5));
                dd.setParentId(rs.getString(6));
                dd.setGroupId(rs.getString(7));
                dd.setGroupName(rs.getString(8));
                dd.setDirectoryLevel(rs.getInt(9));
                dd.setDirectoryAge(rs.getInt(10));
                dd.setPreviewLevel(rs.getInt(11));
                dd.setPreviewAge(rs.getInt(12));
                dd.setReadLevel(rs.getInt(13));
                dd.setReadAge(rs.getInt(14));
                dd.setWriteLevel(rs.getInt(15));
                dd.setWriteAge(rs.getInt(16));
                dd.setManagers(rs.getString(17));
                dd.setAdmins(rs.getString(18));
                v.add(dd);
            }
            rs.close();
            prepStmt.close();
            releaseConnection();
        } catch (Exception e) {
            System.out.println("EJB:DirectoryBean:getDirectoryList:error:" + e.getMessage());
        }
        return v;
    }

    public int updateDirectoryDetails(String table_name, String dir_id, String managers, String admins) {
        int rs = 0;
        if (table_name == null || dir_id == null) {
            return 0;
        }
        try {
            makeConnection();
            String query = "UPDATE ? SET MANAGERS=?, ADMINS=? WHERE ID=?;";
            PreparedStatement prepStmt = con.prepareStatement(query);
            prepStmt.setString(1, table_name);
            prepStmt.setString(2, managers);
            prepStmt.setString(3, admins);
            prepStmt.setString(4, dir_id);
            rs = prepStmt.executeUpdate();
            prepStmt.close();
            releaseConnection();
        } catch (Exception e) {
            releaseConnection();
            System.out.println("EJB:DirectoryBean: updateDirectoryDetails error:" + e.getMessage());
            rs = -1;
        }
        return rs;
    }

    public int updateDirectoryDetails(String table_name, String dir_id, String dir_name, String dir_type, String dir_level, String dir_age, String pre_level, String pre_age, String read_level, String read_age, String write_level, String write_age, String managers, String admins) {
        int rs = 0;
        if (table_name == null || dir_id == null) {
            return 0;
        }
        if (managers == null) {
            managers = "";
        }
        if (admins == null) {
            admins = "";
        }
        try {
            makeConnection();
            String query = "UPDATE ? SET TYPE=?, NAME=?, DIRECTORY_LEVEL=?, DIRECTORY_AGE=?," + " PREVIEW_LEVEL=?, PREVIEW_AGE=?, READ_LEVEL=?, READ_AGE=?, WRITE_LEVEL=?, WRITE_AGE=?," + " MANAGERS=?, ADMINS=? WHERE ID=?;";
            PreparedStatement prepStmt = con.prepareStatement(query);
            prepStmt.setString(1, table_name);
            prepStmt.setString(2, dir_type);
            prepStmt.setString(3, dir_name);
            prepStmt.setString(4, dir_level);
            prepStmt.setString(5, dir_age);
            prepStmt.setString(6, pre_level);
            prepStmt.setString(7, pre_age);
            prepStmt.setString(8, read_level);
            prepStmt.setString(9, read_age);
            prepStmt.setString(10, write_level);
            prepStmt.setString(11, write_age);
            prepStmt.setString(12, managers);
            prepStmt.setString(13, admins);
            prepStmt.setString(14, dir_id);
            rs = prepStmt.executeUpdate();
            prepStmt.close();
            releaseConnection();
        } catch (Exception e) {
            releaseConnection();
            System.out.println("EJB:DirectoryBean: updateDirectoryDetails error:" + e.getMessage());
            rs = -1;
        }
        return rs;
    }

    private void makeConnection() {
        try {
            InitialContext ic = new InitialContext();
            DataSource ds = (DataSource) ic.lookup("java:comp/env/jdbc/DefaultDS");
            con = ds.getConnection();
        } catch (Exception e) {
            releaseConnection();
            System.out.println("EJB:DirectoryBean: makeConnection error:" + e.getMessage());
        }
    }

    private void releaseConnection() {
        try {
            con.close();
        } catch (SQLException e) {
            System.out.println("DirectoryBean: Reaease connect error: " + e.getMessage());
        }
    }
}
