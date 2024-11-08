package lebah.portal.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import lebah.db.Db;
import lebah.db.DbException;
import lebah.db.Log;
import lebah.db.SQLRenderer;
import lebah.portal.element.Module2;
import lebah.portal.element.Role;
import lebah.portal.element.Tab;

/**
 * 
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.0
 */
public class UserTabDb {

    public static Vector retrieve(String usrlogin) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            Vector<Tab> v = new Vector<Tab>();
            SQLRenderer r = new SQLRenderer();
            {
                r.add("tab_id");
                r.add("tab_title");
                r.add("display_type");
                r.add("locked");
                r.add("user_login", usrlogin);
                sql = r.getSQLSelect("tab_template", "sequence");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    Tab tab = new Tab();
                    tab.setId(rs.getString("tab_id"));
                    tab.setTitle(rs.getString("tab_title"));
                    tab.setDisplaytype(rs.getString("display_type"));
                    int locked = rs.getInt("locked");
                    tab.setLocked(locked == 1 ? true : false);
                    v.addElement(tab);
                }
            }
            for (Tab tab : v) {
                if ("pulldown_menu".equals(tab.getDisplayType())) {
                    sql = r.reset().add("t.tab_id", tab.getId()).add("t.user_login", usrlogin).add("t.module_id").add("t.module_custom_title").add("m.module_title").add("m.module_class").relate("t.module_id", "m.module_id").getSQLSelect("user_module_template t, module m").concat(" order by t.sequence");
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
                sql = "SELECT sequence FROM tab_template WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    sequence = rs.getInt("sequence");
                }
            }
            String tab2 = "";
            if ("down".equals(pos)) {
                sql = "SELECT tab_id FROM tab_template WHERE user_login = '" + usrlogin + "' AND sequence = " + Integer.toString(++sequence);
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) tab2 = rs.getString("tab_id");
                if (!"".equals(tab2)) {
                    sql = "UPDATE tab_template SET sequence = " + sequence + " WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
                    stmt.executeUpdate(sql);
                    sql = "UPDATE tab_template SET sequence = " + Integer.toString(--sequence) + " WHERE tab_id = '" + tab2 + "' AND user_login = '" + usrlogin + "'";
                    stmt.executeUpdate(sql);
                }
            } else if ("up".equals(pos)) {
                sql = "SELECT tab_id FROM tab_template WHERE user_login = '" + usrlogin + "' AND sequence = " + Integer.toString(--sequence);
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) tab2 = rs.getString("tab_id");
                if (!"".equals(tab2)) {
                    sql = "UPDATE tab_template SET sequence = " + sequence + " WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
                    stmt.executeUpdate(sql);
                    sql = "UPDATE tab_template SET sequence = " + Integer.toString(++sequence) + " WHERE tab_id = '" + tab2 + "' AND user_login = '" + usrlogin + "'";
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
                sql = "SELECT sequence FROM tab_template WHERE user_login = '" + usrlogin + "' AND sequence = 0";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) fix = true;
            }
            if (fix) {
                Vector v = new Vector();
                sql = "SELECT tab_id FROM tab_template WHERE user_login = '" + usrlogin + "'";
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String tab_id = rs.getString("tab_id");
                    v.addElement(tab_id);
                }
                for (int i = 0; i < v.size(); i++) {
                    String tab_id = (String) v.elementAt(i);
                    sql = "UPDATE tab_template SET sequence = " + Integer.toString(i + 1) + " WHERE tab_id = '" + tab_id + "' AND user_login = '" + usrlogin + "'";
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
            sql = r.getSQLSelect("tab_template");
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
            tabid = lebah.db.UniqueID.getUID();
            int max_seq = 0;
            {
                sql = "SELECT MAX(sequence) AS seq FROM tab_template WHERE user_login = '" + usrlogin + "'";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) max_seq = rs.getInt("seq");
            }
            SQLRenderer r = new SQLRenderer();
            r.add("tab_id", tabid);
            r.add("tab_title", tabtitle);
            r.add("user_login", usrlogin);
            r.add("display_type", displaytype);
            r.add("sequence", ++max_seq);
            r.add("locked", 1);
            sql = r.getSQLInsert("tab_template");
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
            sql = "SELECT sequence FROM tab_template WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) sequence = rs.getInt("sequence");
            sql = "DELETE FROM user_module_template WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";
            stmt.executeUpdate(sql);
            sql = "DELETE FROM tab_template WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";
            stmt.executeUpdate(sql);
            sql = "UPDATE tab_template SET sequence = sequence - 1 WHERE user_login = '" + usrlogin + "' AND sequence > " + sequence;
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
            sql = "UPDATE tab_template SET tab_title = '" + title + "' WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
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
            sql = "UPDATE tab_template SET tab_title = '" + title + "', display_type = '" + displaytype + "' " + "WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
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
            sql = "UPDATE tab_template SET display_type = '" + displaytype + "' " + "WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
            stmt.executeUpdate(sql);
        } catch (SQLException sqlex) {
            throw new DbException(sqlex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    /**
 * This method gets all roles from table role and returns a Vector object.
 */
    public static Vector getRoles() {
        String sql = "select name, description from role order by name";
        Db database = null;
        Vector list = new Vector();
        try {
            Role obj = null;
            database = new Db();
            Statement stmt = database.getStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                obj = new Role();
                obj.setName(rs.getString("name"));
                obj.setDescription(rs.getString("description"));
                list.addElement(obj);
            }
        } catch (DbException dbex) {
            System.out.println("PrepareTemplateTab.getRoles(): DbException : " + dbex.getMessage());
        } catch (SQLException ex) {
            System.out.println("PrepareTemplateTab.getRoles(): SQLException : " + ex.getMessage());
        } finally {
            if (database != null) database.close();
        }
        return list;
    }

    public static void saveTabsOrder(String[] tabIds, String[] lockIds, String role) throws Exception {
        if (tabIds == null) return;
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            int seq = 0;
            for (String tabId : tabIds) {
                sql = "update tab_template set sequence = " + seq++ + ", locked = 0 where tab_id = '" + tabId + "' and user_login = '" + role + "'";
                db.getStatement().executeUpdate(sql);
            }
            if (lockIds != null) {
                for (String tabId : lockIds) {
                    sql = "update tab_template set locked = 1 where tab_id = '" + tabId + "' and user_login = '" + role + "'";
                    db.getStatement().executeUpdate(sql);
                }
            }
        } finally {
            if (db != null) db.close();
        }
    }

    public static void saveTabsOrderPersonal(String[] tabIds, String user) throws Exception {
        if (tabIds == null) return;
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            int seq = 0;
            for (String tabId : tabIds) {
                sql = "update tabs set sequence = " + seq++ + " where tab_id = '" + tabId + "' and user_login = '" + user + "'";
                db.getStatement().executeUpdate(sql);
            }
        } finally {
            if (db != null) db.close();
        }
    }
}
