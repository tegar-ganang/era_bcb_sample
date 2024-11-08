package lebah.portal;

import java.sql.*;
import java.util.*;
import lebah.db.Db;
import lebah.db.DbException;
import lebah.db.SQLRenderer;
import lebah.portal.element.Module2;
import lebah.portal.element.Tab;

/**
 * 
 * @author Shamsul Bahrin Abd Mutalib
  * @version 1.0
 */
public class TabDb {

    static int PERSONALIZED = 0;

    static int ROLEBASED = 1;

    public static Vector getRoleTabs(String usrlogin) throws DbException {
        return getTabs(ROLEBASED, usrlogin);
    }

    public static Vector getPersonalizedTabs(String usrlogin) throws DbException {
        return getTabs(PERSONALIZED, usrlogin);
    }

    public static Vector getTabs(int type, String usrlogin) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            Vector<Tab> v = new Vector<Tab>();
            SQLRenderer r = new SQLRenderer();
            String role = "";
            if (type == ROLEBASED) {
                {
                    sql = r.add("user_login", usrlogin).add("user_role").getSQLSelect("users");
                    ResultSet rs = db.getStatement().executeQuery(sql);
                    if (rs.next()) role = rs.getString(1);
                }
            }
            String tabName = "";
            if (type == ROLEBASED) {
                tabName = "tab_template";
                if ("anon".equals(role) || "root".equals(role)) tabName = "tab_user";
            } else {
                tabName = "tab_user";
            }
            {
                r.reset();
                r.add("tab_id");
                r.add("tab_title");
                r.add("display_type");
                r.add("sequence");
                r.add("locked");
                r.add("user_login", "tab_template".equals(tabName) ? role : usrlogin);
                sql = r.getSQLSelect(tabName, "sequence");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    Tab tab = new Tab();
                    tab.setId(rs.getString("tab_id"));
                    tab.setTitle(rs.getString("tab_title"));
                    tab.setSequence(rs.getInt("sequence"));
                    tab.setDisplaytype(rs.getString("display_type"));
                    v.addElement(tab);
                }
            }
            for (Tab tab : v) {
                if ("pulldown_menu".equals(tab.getDisplayType())) {
                    String module_table = "tab_template".equals(tabName) ? "user_module_template t" : "user_module t";
                    String usr = tab.isLocked() ? role : usrlogin;
                    sql = r.reset().add("t.tab_id", tab.getId()).add("t.user_login", usr).add("t.module_id").add("t.module_custom_title").add("m.module_title").add("m.module_class").relate("t.module_id", "m.module_id").getSQLSelect(module_table + ", module m").concat(" order by t.sequence");
                    ResultSet rs = stmt.executeQuery(sql);
                    while (rs.next()) {
                        String moduleId = rs.getString("module_id");
                        String moduleTitle = rs.getString("module_title");
                        String moduleClass = rs.getString("module_class");
                        String s = rs.getString("module_custom_title");
                        Module2 module = new Module2(moduleId, moduleTitle, moduleClass, s);
                        tab.addModule(module);
                    }
                }
            }
            return v;
        } catch (SQLException ex) {
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void saveTabs(String[] tabIds, String user) throws Exception {
        if (tabIds == null) return;
        Db db = null;
        String sql = "";
        Connection conn = null;
        try {
            db = new Db();
            conn = db.getConnection();
            conn.setAutoCommit(false);
            int seq = 0;
            for (String tabId : tabIds) {
                seq = seq + 1;
                sql = "update tab_user set sequence = " + seq + " where tab_id = '" + tabId + "' and user_login = '" + user + "'";
                db.getStatement().executeUpdate(sql);
            }
            conn.commit();
        } catch (SQLException sqex) {
            try {
                conn.rollback();
            } catch (SQLException rollex) {
            }
            throw sqex;
        } finally {
            if (db != null) db.close();
        }
    }

    public static void deleteTab(String usrlogin, String tabid) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            String role = "";
            sql = "SELECT user_role FROM users WHERE user_login = '" + usrlogin + "' ";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) role = rs.getString(1);
            int sequence = 0;
            sql = "SELECT sequence FROM tab_user WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";
            rs = stmt.executeQuery(sql);
            if (rs.next()) sequence = rs.getInt("sequence");
            sql = "DELETE FROM user_module WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";
            stmt.executeUpdate(sql);
            sql = "DELETE FROM tab_user WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";
            stmt.executeUpdate(sql);
            sql = "UPDATE tab_user SET sequence = sequence - 1 WHERE user_login = '" + usrlogin + "' AND sequence > " + sequence;
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
            sql = "UPDATE tab_user SET tab_title = '" + title + "' WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
            stmt.executeUpdate(sql);
        } catch (SQLException sqlex) {
            throw new DbException(sqlex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void changeDisplayType(String usrlogin, String tab, String displaytype) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            sql = "UPDATE tab_user SET display_type = '" + displaytype + "' " + "WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
            stmt.executeUpdate(sql);
        } catch (SQLException sqlex) {
            throw new DbException(sqlex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static boolean addNewTab(String usrlogin, String tabtitle) throws DbException {
        return addNewTab(usrlogin, tabtitle, "");
    }

    public static boolean addNewTab(String usrlogin, String tabtitle, String displaytype) throws DbException {
        if ("".equals(displaytype)) displaytype = "left_navigation";
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            String tabid = lebah.db.UniqueID.getUID();
            int max_seq = 0;
            {
                sql = "SELECT MAX(sequence) AS seq FROM tab_user WHERE user_login = '" + usrlogin + "'";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) max_seq = rs.getInt("seq");
            }
            SQLRenderer r = new SQLRenderer();
            int seq = max_seq + 1;
            sql = add("tab_user", usrlogin, tabtitle, displaytype, stmt, tabid, seq, r);
        } catch (SQLException sqlex) {
            throw new DbException(sqlex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
        return true;
    }

    private static String add(String table, String usrlogin, String tabtitle, String displaytype, Statement stmt, String tabid, int seq, SQLRenderer r) throws SQLException {
        String sql;
        r.reset();
        r.add("tab_id", tabid);
        r.add("tab_title", tabtitle);
        r.add("user_login", usrlogin);
        r.add("display_type", displaytype);
        r.add("sequence", seq);
        sql = r.getSQLInsert(table);
        stmt.executeUpdate(sql);
        return sql;
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
            sql = r.getSQLSelect("tab_user");
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

    void personalizedTab(Vector v, String id, String title) {
        Tab tab = new Tab();
        tab.setId(id);
        tab.setTitle(title);
        tab.setSequence(0);
        tab.setDisplaytype("");
        v.addElement(tab);
    }
}
