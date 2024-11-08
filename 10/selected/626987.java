package lebah.portal.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Vector;
import lebah.db.DataHelper;
import lebah.db.Db;
import lebah.db.DbException;
import lebah.db.Log;
import lebah.db.SQLRenderer;
import lebah.portal.element.PageTheme;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public class RegisterUser {

    public static void savePhoto(final String userId, final String photoFileName) throws Exception {
        new DataHelper() {

            public String doSQL() {
                SQLRenderer r = new SQLRenderer();
                return r.update("user_login", userId).add("avatar", photoFileName).getSQLUpdate("users");
            }
        }.execute();
    }

    public static boolean add(String fullname, String username, String password, String role, String style) throws Exception {
        Db db = null;
        Connection conn = null;
        String sql = "";
        try {
            db = new Db();
            conn = db.getConnection();
            Statement stmt = db.getStatement();
            conn.setAutoCommit(false);
            SQLRenderer r = new SQLRenderer();
            {
                r.add("user_login");
                r.add("user_login", username);
                sql = r.getSQLSelect("users");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next() && (username.equalsIgnoreCase(rs.getString("user_login")))) return false;
            }
            {
                r.add("user_login_alt");
                r.add("user_login_alt", username);
                sql = r.getSQLSelect("users");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next() && (username.equalsIgnoreCase(rs.getString("user_login_alt")))) return false;
            }
            {
                r.clear();
                r.add("user_login", username);
                r.add("user_password", lebah.util.PasswordService.encrypt(password));
                r.add("user_name", fullname);
                r.add("user_role", role);
                r.add("date_registered", lebah.util.DateTool.getCurrentDatetime());
                sql = r.getSQLInsert("users");
                stmt.executeUpdate(sql);
                setPageStyle(username, style);
            }
            conn.commit();
        } catch (SQLException ex) {
            try {
                conn.rollback();
            } catch (SQLException ex2) {
            }
            Log.print(ex.getMessage() + "\n" + sql);
            return false;
        } finally {
            if (db != null) db.close();
        }
        return true;
    }

    public static boolean update2(String fullname, String username, String password, String role) throws Exception {
        Db db = null;
        Connection conn = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            r.add("user_name", fullname);
            r.add("user_password", lebah.util.PasswordService.encrypt(password));
            r.add("user_role", role);
            r.update("user_login", username);
            sql = r.getSQLUpdate("users");
            int num = stmt.executeUpdate(sql);
            if (num > 0) return true; else return false;
        } catch (SQLException ex) {
            Log.print(ex.getMessage() + "\n" + sql);
        } finally {
            if (db != null) db.close();
        }
        return false;
    }

    public static boolean update(String fullname, String username, String login_alt) throws Exception {
        Db db = null;
        Connection conn = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            boolean isExist = false;
            if (!login_alt.equals(username)) {
                {
                    r.clear();
                    r.add("user_login");
                    r.add("user_login", login_alt);
                    sql = r.getSQLSelect("users");
                    ResultSet rs = stmt.executeQuery(sql);
                    if (rs.next()) isExist = true;
                }
            }
            if (!isExist) {
                r.clear();
                r.add("user_name", fullname);
                r.add("user_login_alt", login_alt);
                r.update("user_login", username);
                sql = r.getSQLUpdate("users");
                int num = stmt.executeUpdate(sql);
                if (num > 0) return true; else return false;
            }
        } catch (SQLException ex) {
            Log.print(ex.getMessage() + "\n" + sql);
        } finally {
            if (db != null) db.close();
        }
        return false;
    }

    public static boolean update(String fullname, String username, String password, String login_alt) throws Exception {
        Db db = null;
        Connection conn = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            boolean isExist = false;
            if (!login_alt.equals(username)) {
                {
                    r.clear();
                    r.add("user_login");
                    r.add("user_login", login_alt);
                    sql = r.getSQLSelect("users");
                    ResultSet rs = stmt.executeQuery(sql);
                    if (rs.next()) isExist = true;
                }
            }
            if (!isExist) {
                r.clear();
                r.add("user_name", fullname);
                r.add("user_password", lebah.util.PasswordService.encrypt(password));
                r.add("user_login_alt", login_alt);
                r.update("user_login", username);
                sql = r.getSQLUpdate("users");
                int num = stmt.executeUpdate(sql);
                if (num > 0) return true; else return false;
            }
        } catch (SQLException ex) {
            Log.print(ex.getMessage() + "\n" + sql);
        } finally {
            if (db != null) db.close();
        }
        return false;
    }

    public static boolean update(String fullname, String username, String password, String role, String login_alt) throws Exception {
        Db db = null;
        Connection conn = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            boolean isExist = false;
            if (!login_alt.equals(username)) {
                {
                    r.clear();
                    r.add("user_login");
                    r.add("user_login", login_alt);
                    sql = r.getSQLSelect("users");
                    ResultSet rs = stmt.executeQuery(sql);
                    if (rs.next()) isExist = true;
                }
            }
            if (!isExist) {
                r.clear();
                r.add("user_name", fullname);
                r.add("user_password", lebah.util.PasswordService.encrypt(password));
                r.add("user_role", role);
                r.add("user_login_alt", login_alt);
                r.update("user_login", username);
                sql = r.getSQLUpdate("users");
                int num = stmt.executeUpdate(sql);
                if (num > 0) return true; else return false;
            }
        } catch (SQLException ex) {
            Log.print(ex.getMessage() + "\n" + sql);
        } finally {
            if (db != null) db.close();
        }
        return false;
    }

    public static boolean update(String fullname, String username, String password, String role, String curRole, String style) throws Exception {
        Db db = null;
        Connection conn = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            r.add("user_name", fullname);
            r.add("user_password", lebah.util.PasswordService.encrypt(password));
            r.add("user_role", role);
            r.update("user_login", username);
            sql = r.getSQLUpdate("users");
            int num = stmt.executeUpdate(sql);
            if (!"".equals(style)) setPageStyle(username, style);
            if (num > 0) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException ex) {
            Log.print(ex.getMessage() + "\n" + sql);
        } finally {
            if (db != null) db.close();
        }
        return false;
    }

    public static void delete(String usrlogin) throws DbException {
        Db db = null;
        Connection conn = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            conn = db.getConnection();
            conn.setAutoCommit(false);
            sql = "DELETE FROM user_module WHERE user_login = '" + usrlogin + "'";
            stmt.executeUpdate(sql);
            sql = "DELETE FROM users WHERE user_login = '" + usrlogin + "'";
            stmt.executeUpdate(sql);
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

    public static void setUserModule(String[] usrlogins, String role) throws Exception {
        for (int i = 0; i < usrlogins.length; i++) {
            setUserModule(usrlogins[i], role);
        }
    }

    public static void setUserModule(String usrlogin, String role) throws Exception {
        Db db = null;
        Connection conn = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            conn = db.getConnection();
            conn.setAutoCommit(false);
            sql = "DELETE FROM tabs WHERE user_login = '" + usrlogin + "'";
            stmt.executeUpdate(sql);
            sql = "DELETE FROM user_module WHERE user_login = '" + usrlogin + "'";
            stmt.executeUpdate(sql);
            SQLRenderer r = new SQLRenderer();
            {
                r.add("tab_id");
                r.add("tab_title");
                r.add("sequence");
                r.add("display_type");
                r.add("user_login", role);
                sql = r.getSQLSelect("tab_template");
                ResultSet rs = stmt.executeQuery(sql);
                Vector tabs = new Vector();
                while (rs.next()) {
                    Hashtable h = new Hashtable();
                    h.put("tab_id", rs.getString("tab_id"));
                    h.put("tab_title", rs.getString("tab_title"));
                    h.put("sequence", new Integer(rs.getInt("sequence")));
                    h.put("display_type", rs.getString("display_type"));
                    h.put("user_login", usrlogin);
                    tabs.addElement(h);
                }
                for (int i = 0; i < tabs.size(); i++) {
                    Hashtable h = (Hashtable) tabs.elementAt(i);
                    r.clear();
                    r.add("tab_id", (String) h.get("tab_id"));
                    r.add("tab_title", (String) h.get("tab_title"));
                    r.add("sequence", ((Integer) h.get("sequence")).intValue());
                    r.add("display_type", (String) h.get("display_type"));
                    r.add("user_login", usrlogin);
                    sql = r.getSQLInsert("tabs");
                    stmt.executeUpdate(sql);
                }
            }
            {
                r.clear();
                r.add("tab_id");
                r.add("module_id");
                r.add("sequence");
                r.add("module_custom_title");
                r.add("column_number");
                r.add("user_login", role);
                sql = r.getSQLSelect("user_module_template");
                ResultSet rs = stmt.executeQuery(sql);
                Vector modules = new Vector();
                while (rs.next()) {
                    int sequence = 1;
                    String seq = rs.getString("sequence") != null ? rs.getString("sequence") : "1";
                    if (seq != null && !"".equals(seq)) sequence = Integer.parseInt(seq);
                    int colnum = 1;
                    String col = rs.getString("column_number") != null ? rs.getString("column_number") : "0";
                    if (col != null && !"".equals(col)) colnum = Integer.parseInt(col);
                    Hashtable h = new Hashtable();
                    h.put("tab_id", rs.getString("tab_id"));
                    h.put("module_id", rs.getString("module_id"));
                    h.put("sequence", new Integer(sequence));
                    h.put("module_custom_title", Db.getString(rs, "module_custom_title"));
                    h.put("column_number", new Integer(colnum));
                    h.put("user_login", usrlogin);
                    modules.addElement(h);
                }
                for (int i = 0; i < modules.size(); i++) {
                    Hashtable h = (Hashtable) modules.elementAt(i);
                    r.clear();
                    r.add("tab_id", (String) h.get("tab_id"));
                    r.add("module_id", (String) h.get("module_id"));
                    r.add("sequence", ((Integer) h.get("sequence")).intValue());
                    r.add("module_custom_title", (String) h.get("module_custom_title"));
                    r.add("column_number", ((Integer) h.get("column_number")).intValue());
                    r.add("user_login", usrlogin);
                    sql = r.getSQLInsert("user_module");
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

    public static Vector getPageStyles() throws DbException {
        Db db = null;
        Connection conn = null;
        String sql = "";
        Vector list = new Vector();
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            conn = db.getConnection();
            SQLRenderer r = new SQLRenderer();
            r.add("css_title");
            r.add("css_name");
            sql = r.getSQLSelect("page_css");
            ResultSet rs = stmt.executeQuery(sql);
            PageTheme css = null;
            while (rs.next()) {
                css = new PageTheme(rs.getString("css_name"), rs.getString("css_title"));
                list.addElement(css);
            }
        } catch (SQLException ex) {
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
        return list;
    }

    private static void setPageStyle(String usrlogin, String style) throws DbException {
        Db db = null;
        Connection conn = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            conn = db.getConnection();
            conn.setAutoCommit(false);
            sql = "delete from user_css where user_login = '" + usrlogin + "'";
            stmt.executeUpdate(sql);
            SQLRenderer r = new SQLRenderer();
            r.add("user_login", usrlogin);
            r.add("css_name", style);
            sql = r.getSQLInsert("user_css");
            stmt.executeUpdate(sql);
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

    public static void saveUserInfo(Hashtable info) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
        } finally {
            if (db != null) db.close();
        }
    }
}
