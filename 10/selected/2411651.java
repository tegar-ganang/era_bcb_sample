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
import lebah.portal.element.*;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public class PrepareTab {

    public static void main(String[] args) throws Exception {
        arrangeTab("bob");
    }

    public static void arrangeTab(String usr) throws Exception {
        System.out.println("arrangeTab");
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            SQLRenderer r = new SQLRenderer();
            String role = "";
            {
                sql = r.reset().add("user_login", usr).add("user_role").getSQLSelect("users");
                ResultSet rs = db.getStatement().executeQuery(sql);
                if (rs.next()) role = rs.getString(1);
            }
            if ("".equals(role)) {
                sql = r.reset().add("user_login_alt", usr).add("user_role").add("user_login").getSQLSelect("users");
                ResultSet rs = db.getStatement().executeQuery(sql);
                if (rs.next()) {
                    role = rs.getString(1);
                    usr = rs.getString(2);
                }
            }
            Vector<Tab> vTabDisplay = new Vector<Tab>();
            {
                sql = r.reset().add("tab_id").add("tab_title").add("sequence").add("display_type").add("locked").add("user_login", usr).getSQLSelect("tab_user", "sequence");
                ResultSet rs = db.getStatement().executeQuery(sql);
                while (rs.next()) {
                    Tab tab = new Tab();
                    tab.setId(rs.getString("tab_id"));
                    tab.setTitle(rs.getString("tab_title"));
                    tab.setSequence(rs.getInt("sequence"));
                    tab.setDisplaytype(rs.getString("display_type"));
                    int locked = rs.getInt("locked");
                    tab.setLocked(locked == 1 ? true : false);
                    vTabDisplay.addElement(tab);
                }
            }
            Vector<Tab> vTabTemplate = new Vector<Tab>();
            {
                sql = r.reset().add("tab_id").add("tab_title").add("sequence").add("display_type").add("locked").add("user_login", role).getSQLSelect("tab_template", "sequence");
                ResultSet rs = db.getStatement().executeQuery(sql);
                while (rs.next()) {
                    Tab tab = new Tab();
                    tab.setId(rs.getString(1));
                    tab.setTitle(rs.getString(2));
                    tab.setSequence(rs.getInt(3));
                    tab.setDisplaytype(rs.getString(4));
                    int locked = rs.getInt("locked");
                    tab.setLocked(locked == 1 ? true : false);
                    vTabTemplate.addElement(tab);
                }
            }
            Vector<Tab> vTabPersonal = new Vector<Tab>();
            {
                sql = r.reset().add("tab_id").add("tab_title").add("sequence").add("display_type").add("user_login", usr).getSQLSelect("tabs", "sequence");
                ResultSet rs = db.getStatement().executeQuery(sql);
                while (rs.next()) {
                    Tab tab = new Tab();
                    tab.setId(rs.getString(1));
                    tab.setTitle(rs.getString(2));
                    tab.setSequence(rs.getInt(3));
                    tab.setDisplaytype(rs.getString(4));
                    tab.setLocked(false);
                    vTabPersonal.addElement(tab);
                }
            }
            for (int i = 0; i < vTabTemplate.size(); i++) {
                Tab tab = vTabTemplate.elementAt(i);
                if (!tab.isLocked()) {
                    if (vTabPersonal.indexOf(tab) < 0) {
                        addTabPersonal(usr, db, r, vTabPersonal, tab);
                    }
                }
            }
            Vector<Tab> vTabAdd = new Vector<Tab>();
            for (int i = 0; i < vTabTemplate.size(); i++) {
                Tab tab = vTabTemplate.elementAt(i);
                if (vTabDisplay.indexOf(tab) < 0) {
                    if (tab.isLocked()) vTabAdd.addElement(tab);
                }
            }
            for (int i = 0; i < vTabPersonal.size(); i++) {
                Tab tab = vTabPersonal.elementAt(i);
                if (vTabDisplay.indexOf(tab) < 0) {
                    vTabAdd.addElement(tab);
                }
            }
            for (int i = 0; i < vTabAdd.size(); i++) {
                Tab tab = vTabAdd.elementAt(i);
                sql = r.reset().add("tab_id", tab.getId()).add("tab_title", tab.getTitle()).add("user_login", usr).add("display_type", tab.getDisplaytype()).add("sequence", tab.getSequence()).add("locked", tab.isLocked() ? 1 : 0).getSQLInsert("tab_user");
                db.getStatement().executeUpdate(sql);
            }
            for (int i = 0; i < vTabDisplay.size(); i++) {
                Tab tab = vTabDisplay.elementAt(i);
                if (tab.isLocked() && vTabTemplate.indexOf(tab) < 0) {
                    sql = "delete from tab_user where tab_id = '" + tab.getId() + "' and user_login = '" + usr + "'";
                    db.getStatement().executeUpdate(sql);
                }
            }
        } finally {
            if (db != null) db.close();
        }
    }

    private static void addTabPersonal(String usr, Db db, SQLRenderer r, Vector<Tab> vTabPersonal, Tab tab) throws SQLException {
        String sql;
        vTabPersonal.addElement(tab);
        sql = r.reset().add("tab_id", tab.getId()).add("tab_title", tab.getTitle()).add("sequence", tab.getSequence()).add("display_type", tab.getDisplaytype()).add("user_login", usr).getSQLInsert("tabs");
        db.getStatement().executeUpdate(sql);
        sql = "delete from user_module where user_login = '" + usr + "' and tab_id = '" + tab.getId() + "'";
        db.getStatement().executeUpdate(sql);
        sql = "insert into user_module select * from user_module_template  where user_login = '" + usr + "' and tab_id = '" + tab.getId() + "'";
        db.getStatement().executeUpdate(sql);
    }

    public static Vector retrieve(String usrlogin) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            Vector<Tab> v = new Vector<Tab>();
            SQLRenderer r = new SQLRenderer();
            {
                r.reset();
                r.add("tab_id");
                r.add("tab_title");
                r.add("display_type");
                r.add("user_login", usrlogin);
                sql = r.getSQLSelect("tabs", "sequence");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String id = rs.getString("tab_id");
                    String title = rs.getString("tab_title");
                    String displaytype = rs.getString("display_type");
                    v.addElement(new Tab(id, title, displaytype));
                }
            }
            for (Tab tab : v) {
                if ("pulldown_menu".equals(tab.getDisplayType())) {
                    sql = r.reset().add("t.tab_id", tab.getId()).add("t.user_login", usrlogin).add("t.module_id").add("t.module_custom_title").add("m.module_title").add("m.module_class").relate("t.module_id", "m.module_id").getSQLSelect("user_module t, module m").concat(" order by t.sequence");
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

    public static Vector retrieveTemplate(String usrlogin) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            String role = "";
            {
                sql = r.reset().add("user_role").add("user_login", usrlogin).getSQLSelect("users");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    role = rs.getString(1);
                }
            }
            Vector<Tab> v = new Vector<Tab>();
            {
                r.reset();
                r.add("tab_id");
                r.add("tab_title");
                r.add("display_type");
                r.add("user_login", role);
                sql = r.getSQLSelect("tab_template", "sequence");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String id = rs.getString("tab_id");
                    String title = rs.getString("tab_title");
                    String displaytype = rs.getString("display_type");
                    v.addElement(new Tab(id, title, displaytype));
                }
            }
            for (Tab tab : v) {
                if ("pulldown_menu".equals(tab.getDisplayType())) {
                    sql = r.reset().add("t.tab_id", tab.getId()).add("t.user_login", role).add("t.module_id").add("t.module_custom_title").add("m.module_title").add("m.module_class").relate("t.module_id", "m.module_id").getSQLSelect("user_module_template t, module m").concat(" order by t.sequence");
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
                sql = "SELECT sequence FROM tabs WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    sequence = rs.getInt("sequence");
                }
            }
            String tab2 = "";
            if ("down".equals(pos)) {
                sql = "SELECT tab_id FROM tabs WHERE user_login = '" + usrlogin + "' AND sequence = " + Integer.toString(++sequence);
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) tab2 = rs.getString("tab_id");
                if (!"".equals(tab2)) {
                    sql = "UPDATE tabs SET sequence = " + sequence + " WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
                    stmt.executeUpdate(sql);
                    sql = "UPDATE tabs SET sequence = " + Integer.toString(--sequence) + " WHERE tab_id = '" + tab2 + "' AND user_login = '" + usrlogin + "'";
                    stmt.executeUpdate(sql);
                }
            } else if ("up".equals(pos)) {
                sql = "SELECT tab_id FROM tabs WHERE user_login = '" + usrlogin + "' AND sequence = " + Integer.toString(--sequence);
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) tab2 = rs.getString("tab_id");
                if (!"".equals(tab2)) {
                    sql = "UPDATE tabs SET sequence = " + sequence + " WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
                    stmt.executeUpdate(sql);
                    sql = "UPDATE tabs SET sequence = " + Integer.toString(++sequence) + " WHERE tab_id = '" + tab2 + "' AND user_login = '" + usrlogin + "'";
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
                sql = "SELECT sequence FROM tabs WHERE user_login = '" + usrlogin + "' AND sequence = 0";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) fix = true;
            }
            if (fix) {
                Vector v = new Vector();
                sql = "SELECT tab_id FROM tabs WHERE user_login = '" + usrlogin + "'";
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String tab_id = rs.getString("tab_id");
                    v.addElement(tab_id);
                }
                for (int i = 0; i < v.size(); i++) {
                    String tab_id = (String) v.elementAt(i);
                    sql = "UPDATE tabs SET sequence = " + Integer.toString(i + 1) + " WHERE tab_id = '" + tab_id + "' AND user_login = '" + usrlogin + "'";
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
            sql = r.getSQLSelect("tabs");
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
                sql = "SELECT MAX(sequence) AS seq FROM tabs WHERE user_login = '" + usrlogin + "'";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) max_seq = rs.getInt("seq");
            }
            SQLRenderer r = new SQLRenderer();
            r.add("tab_id", tabid);
            r.add("tab_title", tabtitle);
            r.add("user_login", usrlogin);
            r.add("display_type", displaytype);
            r.add("sequence", ++max_seq);
            sql = r.getSQLInsert("tabs");
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
            sql = "SELECT sequence FROM tabs WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) sequence = rs.getInt("sequence");
            sql = "DELETE FROM user_module WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";
            stmt.executeUpdate(sql);
            sql = "DELETE FROM tabs WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";
            stmt.executeUpdate(sql);
            sql = "UPDATE tabs SET sequence = sequence - 1 WHERE user_login = '" + usrlogin + "' AND sequence > " + sequence;
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
            sql = "UPDATE tabs SET tab_title = '" + title + "' WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
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
            sql = "UPDATE tabs SET tab_title = '" + title + "', display_type = '" + displaytype + "' " + "WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
            stmt.executeUpdate(sql);
        } catch (SQLException sqlex) {
            throw new DbException(sqlex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void saveTabs(String[] tabIds, String user) throws Exception {
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

    public static void changeDisplayType(String usrlogin, String tab, String displaytype) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            sql = "UPDATE tabs SET display_type = '" + displaytype + "' " + "WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
            stmt.executeUpdate(sql);
        } catch (SQLException sqlex) {
            throw new DbException(sqlex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }
}
