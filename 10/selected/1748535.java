package lebah.portal.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import lebah.db.Db;
import lebah.db.DbException;
import lebah.db.SQLRenderer;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public class RegisterModule {

    public static boolean add(String module_id, String module_title, String module_class, String module_group, String module_description) throws DbException {
        return add(module_id, module_title, module_class, module_group, module_description, null);
    }

    public static boolean add(String module_id, String module_title, String module_class, String module_group, String module_description, String[] roles) throws DbException {
        Db db = null;
        Connection conn = null;
        String sql = "";
        try {
            db = new Db();
            conn = db.getConnection();
            Statement stmt = db.getStatement();
            conn.setAutoCommit(false);
            SQLRenderer r = new SQLRenderer();
            r.add("module_id");
            r.add("module_id", module_id);
            sql = r.getSQLSelect("module");
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next() && (module_id.equalsIgnoreCase(rs.getString("module_id")))) return false;
            r = new SQLRenderer();
            r.add("module_id", module_id);
            r.add("module_title", module_title);
            r.add("module_class", module_class);
            r.add("module_group", module_group.toUpperCase());
            r.add("module_description", module_description);
            sql = r.getSQLInsert("module");
            stmt.executeUpdate(sql);
            if (roles != null) {
                sql = "DELETE FROM role_module WHERE module_id = '" + module_id + "'";
                stmt.executeUpdate(sql);
                for (int i = 0; i < roles.length; i++) {
                    sql = "INSERT INTO role_module (module_id, user_role) VALUES ('" + module_id + "', '" + roles[i] + "')";
                    stmt.executeUpdate(sql);
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            try {
                conn.rollback();
            } catch (SQLException ex2) {
            }
            return false;
        } finally {
            if (db != null) db.close();
        }
        return true;
    }

    public static boolean update(String module_id, String module_title, String module_class, String module_group, String module_description) throws DbException {
        return update(module_id, module_title, module_class, module_group, module_description, null);
    }

    public static boolean update(String module_id, String module_title, String module_class, String module_group, String module_description, String[] roles) throws DbException {
        Db db = null;
        Connection conn = null;
        String sql = "";
        try {
            db = new Db();
            conn = db.getConnection();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            conn.setAutoCommit(false);
            r.add("module_title", module_title);
            r.add("module_class", module_class);
            r.add("module_group", module_group.toUpperCase());
            r.add("module_description", module_description);
            r.update("module_id", module_id);
            sql = r.getSQLUpdate("module");
            stmt.executeUpdate(sql);
            if (roles != null) {
                sql = "DELETE FROM role_module WHERE module_id = '" + module_id + "'";
                stmt.executeUpdate(sql);
                for (int i = 0; i < roles.length; i++) {
                    sql = "INSERT INTO role_module (module_id, user_role) VALUES ('" + module_id + "', '" + roles[i] + "')";
                    stmt.executeUpdate(sql);
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            try {
                conn.rollback();
            } catch (SQLException ex2) {
            }
            return false;
        } finally {
            if (db != null) db.close();
        }
        return true;
    }

    public static void delete(String module_id) throws DbException {
        Db db = null;
        Connection conn = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            conn = db.getConnection();
            conn.setAutoCommit(false);
            sql = "DELETE FROM role_module WHERE module_id = '" + module_id + "'";
            stmt.executeUpdate(sql);
            sql = "DELETE FROM user_module WHERE module_id = '" + module_id + "'";
            stmt.executeUpdate(sql);
            sql = "DELETE FROM role_module WHERE module_id = '" + module_id + "'";
            stmt.executeUpdate(sql);
            sql = "DELETE FROM module WHERE module_id = '" + module_id + "'";
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

    public static void assignRoles(String module_id, String[] roles) throws DbException {
        Db db = null;
        Connection conn = null;
        String sql = "";
        try {
            db = new Db();
            conn = db.getConnection();
            Statement stmt = db.getStatement();
            conn.setAutoCommit(false);
            sql = "DELETE FROM role_module WHERE module_id = '" + module_id + "'";
            stmt.executeUpdate(sql);
            for (int i = 0; i < roles.length; i++) {
                sql = "INSERT INTO role_module (module_id, user_role) VALUES ('" + module_id + "', '" + roles[i] + "')";
                stmt.executeUpdate(sql);
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

    public static void updateHtmlLocation(String module_id, String html_location) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            boolean found = false;
            {
                sql = "SELECT module_id FROM module_htmlcontainer WHERE module_id = '" + module_id + "' ";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) found = true;
            }
            if (found) sql = "UPDATE module_htmlcontainer SET html_url = '" + html_location + "' WHERE module_id = '" + module_id + "' "; else sql = "INSERT INTO module_htmlcontainer (module_id, html_url) VALUES ('" + module_id + "', '" + html_location + "')";
            stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void updateRSSLocation(String module_id, String rss) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            boolean found = false;
            {
                sql = "SELECT module_id FROM rss_module WHERE module_id = '" + module_id + "' ";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) found = true;
            }
            if (found) sql = "UPDATE rss_module SET rss_source = '" + rss + "' WHERE module_id = '" + module_id + "' "; else sql = "INSERT INTO rss_module (module_id, rss_source) VALUES ('" + module_id + "', '" + rss + "')";
            stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void updateXMLData(String module_id, String xml, String xsl) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            boolean found = false;
            {
                sql = "SELECT module_id FROM xml_module WHERE module_id = '" + module_id + "' ";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) found = true;
            }
            if (found) sql = "UPDATE xml_module SET xml = '" + xml + "', xsl = '" + xsl + "' WHERE module_id = '" + module_id + "' "; else sql = "INSERT INTO xml_module (module_id, xml, xsl) VALUES ('" + module_id + "', '" + xml + "', '" + xsl + "')";
            stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void updateAttributeData(String module_id, String[] attributes, String[] values) throws DbException {
        if (attributes == null) {
            System.out.println("Error: attributes is null");
            return;
        }
        if (values == null) {
            System.out.println("Error: values is null");
            return;
        }
        Db db = null;
        String sql = "";
        Connection conn = null;
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            conn = db.getConnection();
            conn.setAutoCommit(false);
            SQLRenderer r = new SQLRenderer();
            sql = "DELETE FROM attr_module_data WHERE module_id = '" + module_id + "'";
            stmt.executeUpdate(sql);
            sql = "INSERT INTO attr_module_data ";
            for (int i = 0; i < attributes.length; i++) {
                r.clear();
                r.add("module_id", module_id);
                r.add("attribute_name", attributes[i]);
                r.add("attribute_value", values[i]);
                sql = r.getSQLInsert("attr_module_data");
                stmt.executeUpdate(sql);
            }
            conn.commit();
        } catch (SQLException ex) {
            try {
                conn.rollback();
            } catch (SQLException rex) {
            }
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void addUserModule(String tab_id, String user_login, String module_id, String module_custom_title, int sequence, int colNum) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            SQLRenderer r = new SQLRenderer();
            if (sequence < 0) {
                r.add("MAX(sequence) AS seq");
                r.add("tab_id", tab_id);
                r.add("user_login", user_login);
                sql = r.getSQLSelect("user_module");
                ResultSet rs = db.getStatement().executeQuery(sql);
                if (rs.next()) {
                    sequence = rs.getInt("seq") + 1;
                }
            }
            {
                r.clear();
                r.add("tab_id", tab_id);
                r.add("user_login", user_login);
                r.add("module_id", module_id);
                r.add("module_custom_title", module_custom_title);
                r.add("sequence", sequence);
                r.add("column_number", colNum);
                sql = r.getSQLInsert("user_module");
                db.getStatement().executeUpdate(sql);
            }
        } finally {
            if (db != null) db.close();
        }
    }

    public static void deleteUserModule(String tab_id, String module_id, String user_login) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            SQLRenderer r = new SQLRenderer();
            r.add("tab_id", tab_id);
            r.add("module_id", module_id);
            r.add("user_login", user_login);
            sql = r.getSQLDelete("user_module");
            db.getStatement().executeUpdate(sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void deleteAnonHtmlModule(String tab_id, String module_id, String user_login) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            SQLRenderer r = new SQLRenderer();
            {
                r.clear();
                r.add("module_id", module_id);
                sql = r.getSQLDelete("module");
                db.getStatement().executeUpdate(sql);
            }
            {
                r.clear();
                r.add("module_id", module_id);
                sql = r.getSQLDelete("module_htmlcontainer");
                db.getStatement().executeUpdate(sql);
            }
            {
                r.clear();
                r.add("tab_id", tab_id);
                r.add("module_id", module_id);
                r.add("user_login", user_login);
                sql = r.getSQLDelete("user_module");
                db.getStatement().executeUpdate(sql);
            }
        } finally {
            if (db != null) db.close();
        }
    }
}
