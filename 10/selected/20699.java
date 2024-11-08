package lebah.portal.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import lebah.db.Db;
import lebah.db.DbException;
import lebah.db.SQLRenderer;
import lebah.portal.element.Module;
import lebah.portal.element.Module2;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public class PrepareModule {

    public static Vector retrieve(String usrlogin, String tab) throws DbException {
        Vector v = retrieveTemplate(usrlogin, tab);
        if (v.size() == 0) v = retrievePersonal(usrlogin, tab);
        return v;
    }

    public static Vector retrievePersonal(String usrlogin, String tab) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            sql = "SELECT m.module_id, m.module_title, m.module_class, u.module_custom_title, u.column_number " + "FROM module m, user_module u " + "WHERE m.module_id = u.module_id " + "AND u.user_login = '" + usrlogin + "' " + "AND u.tab_id = '" + tab + "' order by u.sequence";
            ResultSet rs = stmt.executeQuery(sql);
            Vector v = new Vector();
            while (rs.next()) {
                String id = rs.getString("module_id");
                String title = rs.getString("module_title");
                String mclass = rs.getString("module_class");
                String custom_title = rs.getString("module_custom_title");
                int col = rs.getInt("column_number");
                if (custom_title == null || "".equals(custom_title)) custom_title = title;
                v.addElement(new Module2(id, title, mclass, custom_title, col));
            }
            return v;
        } catch (SQLException ex) {
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static Vector retrieveTemplate(String usrlogin, String tab) throws DbException {
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
            sql = "SELECT m.module_id, m.module_title, m.module_class, u.module_custom_title, u.column_number " + "FROM module m, user_module_template u " + "WHERE m.module_id = u.module_id " + "AND u.user_login = '" + role + "' " + "AND u.tab_id = '" + tab + "' order by u.sequence";
            ResultSet rs = stmt.executeQuery(sql);
            Vector v = new Vector();
            while (rs.next()) {
                String id = rs.getString("module_id");
                String title = rs.getString("module_title");
                String mclass = rs.getString("module_class");
                String custom_title = rs.getString("module_custom_title");
                int col = rs.getInt("column_number");
                if (custom_title == null || "".equals(custom_title)) custom_title = title;
                v.addElement(new Module2(id, title, mclass, custom_title, col));
            }
            return v;
        } catch (SQLException ex) {
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void changeSequence(String usrlogin, String tab, String module, String pos) throws DbException {
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
                sql = "SELECT sequence FROM user_module WHERE module_id = '" + module + "' AND tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    sequence = rs.getInt("sequence");
                }
            }
            String module2 = "";
            if ("down".equals(pos)) {
                sql = "SELECT module_id FROM user_module WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "' AND sequence = " + Integer.toString(++sequence);
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) module2 = rs.getString("module_id");
                if (!"".equals(module2)) {
                    sql = "UPDATE user_module SET sequence = " + sequence + " WHERE module_id = '" + module + "' AND tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
                    stmt.executeUpdate(sql);
                    sql = "UPDATE user_module SET sequence = " + Integer.toString(--sequence) + " WHERE module_id = '" + module2 + "' AND tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
                    stmt.executeUpdate(sql);
                }
            } else if ("up".equals(pos)) {
                sql = "SELECT module_id FROM user_module WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "' AND sequence = " + Integer.toString(--sequence);
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) module2 = rs.getString("module_id");
                if (!"".equals(module2)) {
                    sql = "UPDATE user_module SET sequence = " + sequence + " WHERE module_id = '" + module + "' AND tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
                    stmt.executeUpdate(sql);
                    sql = "UPDATE user_module SET sequence = " + Integer.toString(++sequence) + " WHERE module_id = '" + module2 + "' AND tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
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

    public static void fixModuleSequence(String usrlogin, String tab) throws DbException {
        Db db = null;
        Connection conn = null;
        String sql = "";
        try {
            db = new Db();
            conn = db.getConnection();
            conn.setAutoCommit(false);
            Statement stmt = db.getStatement();
            boolean fix = true;
            if (fix) {
                Vector v = new Vector();
                sql = "SELECT module_id FROM user_module WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String module_id = rs.getString("module_id");
                    v.addElement(module_id);
                }
                for (int i = 0; i < v.size(); i++) {
                    String module_id = (String) v.elementAt(i);
                    sql = "UPDATE user_module SET sequence = " + Integer.toString(i + 1) + " WHERE module_id = '" + module_id + "' AND tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";
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

    public static Vector getListOfModules() throws DbException {
        return getListOfModules("");
    }

    public static Vector getListOfModules(String role) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            if (!"".equals(role)) sql = "SELECT m.module_id AS module_id, module_title, module_class, module_group FROM module m, role_module r " + "WHERE m.module_id = r.module_id AND user_role = '" + role + "' " + "ORDER BY module_title"; else sql = "SELECT module_id, module_title, module_class, module_group FROM module ORDER BY module_group, module_title";
            ResultSet rs = stmt.executeQuery(sql);
            Vector v = new Vector();
            while (rs.next()) {
                String id = rs.getString("module_id");
                String title = rs.getString("module_title");
                String klazz = rs.getString("module_class");
                String module_group = rs.getString("module_group");
                Module module = new Module(id, title, klazz);
                module.setGroupName(module_group);
                v.addElement(module);
            }
            return v;
        } catch (SQLException ex) {
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static Vector getListOfModules(String role, String usrlogin, String tab) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            Vector vu = new Vector();
            {
                sql = "SELECT module_id FROM user_module WHERE user_login = '" + usrlogin + "' AND tab_id = '" + tab + "'";
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String id = rs.getString("module_id");
                    vu.addElement(id);
                }
            }
            Vector v = new Vector();
            {
                sql = "SELECT m.module_id AS module_id, module_title, module_class, module_group FROM module m, role_module r " + "WHERE m.module_id = r.module_id AND user_role = '" + role + "' " + "ORDER BY module_group, module_title";
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String id = rs.getString("module_id");
                    String title = rs.getString("module_title");
                    String klazz = rs.getString("module_class");
                    String group = rs.getString("module_group");
                    boolean marked = vu.contains(id) ? true : false;
                    Module module = new Module2(id, title, klazz, marked);
                    module.setGroupName(group != null ? group : "");
                    v.addElement(module);
                }
            }
            return v;
        } catch (SQLException ex) {
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void saveModules(String usrlogin, String tabid, Vector allmodules) throws DbException {
        Db db = null;
        Connection conn = null;
        String sql = "";
        try {
            db = new Db();
            conn = db.getConnection();
            Statement stmt = db.getStatement();
            conn.setAutoCommit(false);
            Vector checkedModules = new Vector();
            for (int i = 0; i < allmodules.size(); i++) {
                Module2 module = (Module2) allmodules.elementAt(i);
                if (module.getMarked()) {
                    checkedModules.addElement(module.getId());
                }
            }
            Vector userModules = new Vector();
            {
                sql = "SELECT module_id FROM user_module WHERE user_login = '" + usrlogin + "' AND tab_id = '" + tabid + "'";
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String id = rs.getString("module_id");
                    userModules.addElement(id);
                }
            }
            Vector deletedModules = new Vector();
            for (int i = 0; i < userModules.size(); i++) {
                if (!checkedModules.contains((String) userModules.elementAt(i))) {
                    deletedModules.addElement((String) userModules.elementAt(i));
                }
            }
            Vector addedModules = new Vector();
            for (int i = 0; i < checkedModules.size(); i++) {
                if (!userModules.contains((String) checkedModules.elementAt(i))) {
                    addedModules.addElement((String) checkedModules.elementAt(i));
                }
            }
            for (int i = 0; i < deletedModules.size(); i++) {
                String id = (String) deletedModules.elementAt(i);
                int sequence = 0;
                sql = "SELECT sequence FROM user_module WHERE module_id = '" + id + "' AND user_login = '" + usrlogin + "' AND tab_id = '" + tabid + "' ";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) sequence = rs.getInt("sequence");
                sql = "DELETE FROM user_module WHERE module_id = '" + id + "' AND user_login = '" + usrlogin + "' AND tab_id = '" + tabid + "' ";
                stmt.executeUpdate(sql);
                sql = "UPDATE user_module SET sequence = sequence - 1 WHERE sequence > " + sequence + " AND user_login = '" + usrlogin + "' AND tab_id = '" + tabid + "' ";
                stmt.executeUpdate(sql);
            }
            {
                int maxseq = 0;
                sql = "SELECT MAX(sequence) AS seq FROM user_module WHERE user_login = '" + usrlogin + "' AND tab_id = '" + tabid + "' ";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) maxseq = rs.getInt("seq");
                for (int i = 0; i < addedModules.size(); i++) {
                    String id = (String) addedModules.elementAt(i);
                    sql = "INSERT INTO user_module (tab_id, module_id, user_login, sequence) VALUES (" + "'" + tabid + "', '" + id + "', '" + usrlogin + "', " + Integer.toString(++maxseq) + ")";
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

    public static Module getModuleById(String module_id) throws DbException {
        Module module = null;
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = null;
            {
                r = new SQLRenderer();
                r.add("module_title");
                r.add("module_class");
                r.add("module_group");
                r.add("module_description");
                r.add("module_id", module_id);
                sql = r.getSQLSelect("module");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    String module_title = rs.getString("module_title");
                    String module_class = rs.getString("module_class");
                    String module_group = rs.getString("module_group");
                    String module_description = rs.getString("module_description");
                    module = new Module(module_id, module_title, module_class);
                    module.setGroupName(module_group != null ? module_group : "");
                    module.setDescription(module_description != null ? module_description : "");
                }
            }
            if (module != null) {
                r = new SQLRenderer();
                r.add("user_role");
                r.add("module_id", module_id);
                sql = r.getSQLSelect("role_module");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    module.addRole(rs.getString("user_role"));
                }
            }
        } catch (SQLException ex) {
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
        return module;
    }

    public static void saveCustomTitles(String usrlogin, String tabid, String[] custom_titles) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            int seq = 0;
            for (int i = 0; i < custom_titles.length; i++) {
                seq++;
                r.clear();
                r.add("module_custom_title", custom_titles[i]);
                r.update("user_login", usrlogin);
                r.update("tab_id", tabid);
                r.update("sequence", seq);
                sql = r.getSQLUpdate("user_module");
                stmt.executeUpdate(sql);
            }
        } catch (SQLException ex) {
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void saveCustomTitlesAndColumnNumbers(String usrlogin, String tabid, String[] custom_titles, String[] column_numbers, String[] module_ids) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            int seq = 0;
            for (int i = 0; i < custom_titles.length; i++) {
                seq++;
                r.clear();
                r.add("module_custom_title", custom_titles[i]);
                r.add("column_number", column_numbers[i]);
                r.add("sequence", seq);
                r.update("user_login", usrlogin);
                r.update("tab_id", tabid);
                r.update("module_id", module_ids[i]);
                sql = r.getSQLUpdate("user_module");
                stmt.executeUpdate(sql);
            }
        } catch (SQLException ex) {
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static Vector retrieve2(String usrlogin, String tab) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            sql = "SELECT m.module_id, m.module_title, m.module_class " + "FROM module m, user_module u " + "WHERE m.module_id = u.module_id " + "AND u.user_login = '" + usrlogin + "' " + "AND u.tab_id = '" + tab + "' order by u.sequence";
            ResultSet rs = stmt.executeQuery(sql);
            Vector v = new Vector();
            while (rs.next()) {
                String id = rs.getString("module_id");
                String title = rs.getString("module_title");
                String mclass = rs.getString("module_class");
                v.addElement(new Module(id, title, mclass));
            }
            return v;
        } catch (SQLException ex) {
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void saveModules(String usrlogin, String tabid, String[] moduleIds, String[] custom_titles, String[] column_numbers) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            for (int i = 0; i < moduleIds.length; i++) {
                r.clear();
                r.add("module_custom_title", custom_titles[i]);
                r.add("column_number", column_numbers[i]);
                r.add("sequence", i + 1);
                r.update("module_id", moduleIds[i]);
                r.update("user_login", usrlogin);
                r.update("tab_id", tabid);
                sql = r.getSQLUpdate("user_module");
                stmt.executeUpdate(sql);
            }
        } catch (SQLException ex) {
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static Vector getListOfModules(String usrlogin, String tab) throws DbException {
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
            Vector vu = new Vector();
            {
                sql = "SELECT module_id FROM user_module WHERE user_login = '" + usrlogin + "' AND tab_id = '" + tab + "'";
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String id = rs.getString("module_id");
                    vu.addElement(id);
                }
            }
            Vector v = new Vector();
            {
                sql = "SELECT m.module_id AS module_id, module_title, module_class, module_group " + "FROM module m, role_module r " + "WHERE m.module_id = r.module_id AND user_role = '" + role + "' " + "ORDER BY module_group, module_title";
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String id = rs.getString("module_id");
                    String title = rs.getString("module_title");
                    String klazz = rs.getString("module_class");
                    String group = rs.getString("module_group");
                    boolean marked = vu.contains(id) ? true : false;
                    Module module = new Module2(id, title, klazz, marked);
                    module.setGroupName(group != null ? group : "");
                    v.addElement(module);
                }
            }
            return v;
        } catch (SQLException ex) {
            throw new DbException(ex.getMessage() + ": " + sql);
        } finally {
            if (db != null) db.close();
        }
    }
}
