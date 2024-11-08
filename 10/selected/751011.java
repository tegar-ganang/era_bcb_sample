package org.jwaim.core.storage.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.jwaim.core.ModuleCommunicationLine;
import org.jwaim.core.interfaces.Module;
import org.jwaim.core.logger.JWAIMLogger;
import org.jwaim.core.util.DynamicClassLoading;

/**
 * Database operations for modules 
 */
final class ModuleOperations {

    static final void addAvailableModule(Module module, DBConnector connector) throws IOException {
        String type = "pre";
        String className = module.getClass().getName();
        if (module.isPreModule()) type = "pre"; else if (module.isPostModule()) type = "post"; else if (module.isExceptionModule()) type = "exception"; else throw new IllegalArgumentException("Module must be of a known type.");
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = connector.getDB();
            ps = con.prepareStatement("INSERT INTO available_module VALUES (?, ?)");
            ps.setString(1, className);
            ps.setString(2, type);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ignore) {
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ignore) {
                }
            }
        }
    }

    static final <T extends Module> List<T> loadModules(Class<T> classFilter, String where, DBConnector connector, ModuleCommunicationLine communicationLine, JWAIMLogger logger) throws IOException {
        List<T> ret = new LinkedList<T>();
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        ResultSet rs2 = null;
        Map<Integer, String> tmpMap = new HashMap<Integer, String>();
        try {
            con = connector.getDB();
            st = con.createStatement();
            rs = st.executeQuery("SELECT * FROM instance " + where);
            while (rs.next()) {
                int id = rs.getInt("id");
                String className = rs.getString("class_name");
                tmpMap.put(id, className);
            }
            rs.close();
            for (Map.Entry<Integer, String> me : tmpMap.entrySet()) {
                int id = me.getKey();
                String className = me.getValue();
                rs2 = st.executeQuery("SELECT * FROM instance_property where instance_id=" + id);
                Properties props = new Properties();
                while (rs2.next()) {
                    String key = rs2.getString("key");
                    String value = rs2.getString("value");
                    props.setProperty(key, value);
                }
                rs2.close();
                Module mod = null;
                try {
                    mod = (Module) DynamicClassLoading.instantiateModule(className, communicationLine, logger);
                } catch (Exception e) {
                    logger.logException(null, "Module class no longer available. Skip all such modules: " + className);
                    continue;
                }
                mod.init(props);
                ret.add(classFilter.cast(mod));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        } finally {
            if (rs2 != null) {
                try {
                    rs2.close();
                } catch (SQLException ignore) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ignore) {
                }
            }
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException ignore) {
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ignore) {
                }
            }
        }
        DatabaseUtils.sortInstancedByPriority(ret);
        return ret;
    }

    static final void removeAvailableModule(Module removedModule, DBConnector connector) throws IOException {
        String className = removedModule.getClass().getName();
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = connector.getDB();
            ps = con.prepareStatement("DELETE FROM available_module where class_name=?");
            ps.setString(1, className);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ignore) {
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ignore) {
                }
            }
        }
    }

    static final void removeModuleInstance(Module module, DBConnector connector) throws IOException {
        Collection<String> queries = new LinkedList<String>();
        queries.add("DELETE FROM instance where id=" + module.getId());
        queries.add("DELETE FROM instance_property where instance_id=" + module.getId());
        DatabaseUtils.executeUpdate(queries, connector);
    }

    static final void saveModule(Module module, DBConnector connector) throws IOException {
        String type = "pre";
        if (module.isPreModule()) type = "pre"; else if (module.isPostModule()) type = "post"; else if (module.isExceptionModule()) type = "exception"; else throw new IllegalArgumentException("Module must be of a known type.");
        Properties props = module.getState();
        Connection con = null;
        PreparedStatement ps = null;
        Statement st = null;
        try {
            con = connector.getDB();
            con.setAutoCommit(false);
            st = con.createStatement();
            st.executeUpdate("DELETE FROM instance where id=" + module.getId());
            st.executeUpdate("DELETE FROM instance_property where instance_id=" + module.getId());
            ps = con.prepareStatement("INSERT INTO instance VALUES (?, ?, ?, ?)");
            ps.setInt(1, module.getId());
            ps.setBoolean(2, module.getActive());
            ps.setString(3, module.getClass().getName());
            ps.setString(4, type);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("INSERT INTO instance_property values(?, ?, ?)");
            for (Enumeration<Object> keys = props.keys(); keys.hasMoreElements(); ) {
                String key = (String) keys.nextElement();
                String value = props.getProperty(key);
                ps.setInt(1, module.getId());
                ps.setString(2, key);
                ps.setString(3, value);
                ps.addBatch();
            }
            ps.executeBatch();
            con.commit();
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            throw new IOException(e.getMessage());
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException ignore) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ignore) {
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ignore) {
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    static final <T extends Module> Map<String, Class<? extends T>> getAvailableClasses(String type, JWAIMLogger logger, DBConnector connector) throws IOException {
        Map<String, Class<? extends T>> ret = new HashMap<String, Class<? extends T>>();
        if (type == null) return ret;
        Connection con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = connector.getDB();
            st = con.prepareStatement("select class_name from available_module where module_type=?");
            st.setString(1, type);
            rs = st.executeQuery();
            while (rs.next()) {
                String s = rs.getString("class_name");
                try {
                    Class<T> c = (Class<T>) Class.forName(s);
                    ret.put(s, c);
                } catch (ClassNotFoundException e) {
                    logger.logException(e, "Previously used class (" + s + ") was removed. Its functionality will no loger be available.");
                }
            }
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ignore) {
                }
            }
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException ignore) {
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ignore) {
                }
            }
        }
        return ret;
    }
}
