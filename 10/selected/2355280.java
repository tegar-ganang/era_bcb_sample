package mecca.portal.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import mecca.db.Db;
import mecca.db.DbException;
import mecca.db.Log;
import mecca.db.SQLRenderer;
import mecca.portal.element.Tab;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public class PrepareTab {

    public static Vector retrieve(String usrlogin) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            r.add("tab_id");
            r.add("tab_title");
            r.add("display_type");
            r.add("user_login", usrlogin);
            sql = r.getSQLSelect("tab", "sequence");
            ResultSet rs = stmt.executeQuery(sql);
            Vector v = new Vector();
            while (rs.next()) {
                String id = rs.getString("tab_id");
                String title = rs.getString("tab_title");
                String displaytype = rs.getString("display_type");
                v.addElement(new Tab(id, title, displaytype));
            }
            return v;
        } catch (SQLException ex) {
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void changeSequence(String usrlogin, String tab, String pos) throws DbException {
        Db db = null;
        Connection conn = null;
        String sql = "";
        try {
            db = new Db();
            conn = db.getConnection();
            conn.setAutoCommit(false);
            Statement stmt = db.getStatement();
            int sequence = 0;
            {
                sql = "SELECT sequence FROM tab WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    sequence = rs.getInt("sequence");
                }
            }
            String tab2 = "";
            if ("down".equals(pos)) {
                sql = "SELECT tab_id FROM tab WHERE user_login = '" + usrlogin + "' AND sequence = " + Integer.toString(++sequence);
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) tab2 = rs.getString("tab_id");
                if (!"".equals(tab2)) {
                    sql = "UPDATE tab SET sequence = " + sequence + " WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
                    stmt.executeUpdate(sql);
                    sql = "UPDATE tab SET sequence = " + Integer.toString(--sequence) + " WHERE tab_id = '" + tab2 + "' AND user_login = '" + usrlogin + "'";
                    stmt.executeUpdate(sql);
                }
            } else if ("up".equals(pos)) {
                sql = "SELECT tab_id FROM tab WHERE user_login = '" + usrlogin + "' AND sequence = " + Integer.toString(--sequence);
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) tab2 = rs.getString("tab_id");
                if (!"".equals(tab2)) {
                    sql = "UPDATE tab SET sequence = " + sequence + " WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
                    stmt.executeUpdate(sql);
                    sql = "UPDATE tab SET sequence = " + Integer.toString(++sequence) + " WHERE tab_id = '" + tab2 + "' AND user_login = '" + usrlogin + "'";
                    stmt.executeUpdate(sql);
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            try {
                conn.rollback();
            } catch (SQLException exr) {
            }
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void fixTabSequence(String usrlogin) throws DbException {
        Db db = null;
        Connection conn = null;
        String sql = "";
        try {
            db = new Db();
            conn = db.getConnection();
            conn.setAutoCommit(false);
            Statement stmt = db.getStatement();
            boolean fix = false;
            {
                sql = "SELECT sequence FROM tab WHERE user_login = '" + usrlogin + "' AND sequence = 0";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) fix = true;
            }
            if (fix) {
                Vector v = new Vector();
                sql = "SELECT tab_id FROM tab WHERE user_login = '" + usrlogin + "'";
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String tab_id = rs.getString("tab_id");
                    v.addElement(tab_id);
                }
                for (int i = 0; i < v.size(); i++) {
                    String tab_id = (String) v.elementAt(i);
                    sql = "UPDATE tab SET sequence = " + Integer.toString(i + 1) + " WHERE tab_id = '" + tab_id + "' AND user_login = '" + usrlogin + "'";
                    Log.print(sql);
                    stmt.executeUpdate(sql);
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            try {
                conn.rollback();
            } catch (SQLException exr) {
            }
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static Tab getTab(String usrlogin, String tabid) throws DbException {
        Tab tab = null;
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            r.add("tab_title");
            r.add("display_type");
            r.add("user_login", usrlogin);
            r.add("tab_id", tabid);
            sql = r.getSQLSelect("tab");
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                String tab_title = rs.getString("tab_title");
                String displaytype = rs.getString("display_type");
                tab = new Tab(tabid, tab_title, displaytype);
            }
        } catch (SQLException sqlex) {
            throw new DbException(sqlex.getMessage() + " : " + sql);
        } finally {
            if (db != null) db.close();
        }
        return tab;
    }

    public static boolean addNewTab(String usrlogin, String tabid, String tabtitle) throws DbException {
        return addNewTab(usrlogin, tabid, tabtitle, "");
    }

    public static boolean addNewTab(String usrlogin, String tabid, String tabtitle, String displaytype) throws DbException {
        if ("".equals(displaytype)) displaytype = "left_navigation";
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            {
                sql = "SELECT tab_id FROM tab WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) return false;
            }
            int max_seq = 0;
            {
                sql = "SELECT MAX(sequence) AS seq FROM tab WHERE user_login = '" + usrlogin + "'";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) max_seq = rs.getInt("seq");
            }
            SQLRenderer r = new SQLRenderer();
            r.add("tab_id", tabid);
            r.add("tab_title", tabtitle);
            r.add("user_login", usrlogin);
            r.add("display_type", displaytype);
            r.add("sequence", ++max_seq);
            sql = r.getSQLInsert("tab");
            stmt.executeUpdate(sql);
        } catch (SQLException sqlex) {
            throw new DbException(sqlex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
        return true;
    }

    public static void deleteTab(String usrlogin, String tabid) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            int sequence = 0;
            sql = "SELECT sequence FROM tab WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) sequence = rs.getInt("sequence");
            sql = "DELETE FROM user_module WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";
            stmt.executeUpdate(sql);
            sql = "DELETE FROM tab WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";
            stmt.executeUpdate(sql);
            sql = "UPDATE tab SET sequence = sequence - 1 WHERE user_login = '" + usrlogin + "' AND sequence > " + sequence;
            stmt.executeUpdate(sql);
        } catch (SQLException sqlex) {
            throw new DbException(sqlex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void changeTitle(String usrlogin, String tab, String title) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            sql = "UPDATE tab SET tab_title = '" + title + "' WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
            stmt.executeUpdate(sql);
        } catch (SQLException sqlex) {
            throw new DbException(sqlex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void changeTitleAndDisplayType(String usrlogin, String tab, String title, String displaytype) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            sql = "UPDATE tab SET tab_title = '" + title + "', display_type = '" + displaytype + "' " + "WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
            stmt.executeUpdate(sql);
        } catch (SQLException sqlex) {
            throw new DbException(sqlex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }
}
