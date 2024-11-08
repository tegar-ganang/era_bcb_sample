package com.google.code.sagetvaddons.swl.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The SWL data store; data store for custom features and enhancements
 * @version $Id: DataStore.java 253 2009-02-07 18:38:43Z derek@battams.ca $
 */
class DataStore {

    private static final int SCHEMA_VERSION = 1;

    private static DataStore instance = null;

    static synchronized DataStore getInstance() {
        if (instance == null) {
            try {
                instance = new DataStore();
            } catch (Exception e) {
                e.printStackTrace(System.out);
                instance = null;
            }
        }
        return instance;
    }

    private static String ERR_MISSING_TABLE = "^no such table.+";

    private File dataStore;

    private Connection conn;

    private DataStore() throws ClassNotFoundException, IOException {
        Class.forName("org.sqlite.JDBC");
        dataStore = new File("swl.sqlite");
        openConnection();
        loadDDL();
        upgradeSchema();
    }

    private synchronized void openConnection() throws IOException {
        try {
            if (conn != null && !conn.isClosed()) return;
            conn = DriverManager.getConnection("jdbc:sqlite:" + dataStore.getAbsolutePath());
        } catch (SQLException e) {
            e.printStackTrace(System.out);
            throw new IOException("Error opening data store");
        }
        return;
    }

    @Override
    protected void finalize() {
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
        return;
    }

    private synchronized void loadDDL() throws IOException {
        try {
            conn.createStatement().executeQuery("SELECT * FROM non_generic_favs").close();
        } catch (SQLException e) {
            Statement stmt = null;
            if (!e.getMessage().matches(ERR_MISSING_TABLE)) {
                e.printStackTrace(System.out);
                throw new IOException("Error on initial data store read");
            }
            String[] qry = { "CREATE TABLE non_generic_favs (id INT NOT NULL PRIMARY KEY)", "CREATE TABLE ignore_chan_favs (id INT NOT NULL PRIMARY KEY, chanlist LONG VARCHAR)", "CREATE TABLE settings (var VARCHAR(32) NOT NULL, val VARCHAR(255) NOT NULL, PRIMARY KEY(var))", "INSERT INTO settings (var, val) VALUES ('schema', '1')" };
            try {
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                for (String q : qry) stmt.executeUpdate(q);
                conn.commit();
            } catch (SQLException e2) {
                try {
                    conn.rollback();
                } catch (SQLException e3) {
                    e3.printStackTrace(System.out);
                }
                e2.printStackTrace(new PrintWriter(System.out));
                throw new IOException("Error initializing data store");
            } finally {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException e4) {
                        e4.printStackTrace(System.out);
                        throw new IOException("Unable to cleanup data store resources");
                    }
                }
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e3) {
                    e3.printStackTrace(System.out);
                    throw new IOException("Unable to reset data store auto commit");
                }
            }
        }
        return;
    }

    private synchronized void upgradeSchema() throws IOException {
        Statement stmt = null;
        try {
            int i = getSchema();
            if (i < SCHEMA_VERSION) {
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                while (i < SCHEMA_VERSION) {
                    switch(i) {
                    }
                    i++;
                }
                conn.commit();
            }
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e2) {
                e2.printStackTrace(System.out);
            }
            e.printStackTrace(System.out);
            throw new IOException("Error upgrading data store");
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace(System.out);
                throw new IOException("Unable to cleanup SQL resources");
            }
        }
    }

    private synchronized int getSchema() throws IOException {
        String qry = "SELECT val FROM settings WHERE var = 'schema'";
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(qry);
            return rs.getInt(1);
        } catch (SQLException e) {
            if (!e.getMessage().matches(ERR_MISSING_TABLE)) e.printStackTrace(System.out);
            return 0;
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                e.printStackTrace(System.out);
                throw new IOException("Unable to cleanup SQL resources");
            }
        }
    }

    synchronized boolean addNonGenericFav(int favId) {
        String qry = "REPLACE INTO non_generic_favs (id) VALUES (" + favId + ")";
        Statement s = null;
        try {
            s = conn.createStatement();
            s.executeUpdate(qry);
        } catch (SQLException e) {
            e.printStackTrace(System.out);
            return false;
        } finally {
            if (s != null) try {
                s.close();
            } catch (SQLException e) {
                e.printStackTrace(System.out);
            }
        }
        return true;
    }

    synchronized boolean removeNonGenericFav(int favId) {
        String qry = "DELETE FROM non_generic_favs WHERE id = " + favId;
        Statement s = null;
        try {
            s = conn.createStatement();
            s.executeUpdate(qry);
        } catch (SQLException e) {
            e.printStackTrace(System.out);
            return false;
        } finally {
            if (s != null) try {
                s.close();
            } catch (SQLException e) {
                e.printStackTrace(System.out);
            }
        }
        return true;
    }

    synchronized Integer[] getNonGenericFavs() {
        String qry = "SELECT id FROM non_generic_favs";
        Statement s = null;
        ResultSet r = null;
        List<Integer> list = new ArrayList<Integer>();
        try {
            s = conn.createStatement();
            r = s.executeQuery(qry);
            while (r.next()) list.add(r.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        } finally {
            try {
                if (r != null) r.close();
                if (s != null) s.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return list.toArray(new Integer[0]);
    }

    synchronized boolean setIgnoreChanList(int favId, String chanList) {
        String qry = "REPLACE INTO ignore_chan_favs (id, chanlist) VALUES (?, ?)";
        PreparedStatement s = null;
        try {
            s = conn.prepareStatement(qry);
            s.setInt(1, favId);
            s.setString(2, chanList);
            s.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace(System.out);
            return false;
        } finally {
            if (s != null) try {
                s.close();
            } catch (SQLException e) {
                e.printStackTrace(System.out);
            }
        }
        return true;
    }

    synchronized boolean removeIgnoreChanList(int favId) {
        String qry = "DELETE FROM ignore_chan_favs WHERE id = " + favId;
        Statement s = null;
        try {
            s = conn.createStatement();
            s.executeUpdate(qry);
        } catch (SQLException e) {
            e.printStackTrace(System.out);
            return false;
        } finally {
            if (s != null) try {
                s.close();
            } catch (SQLException e) {
                e.printStackTrace(System.out);
            }
        }
        return true;
    }

    synchronized Map<Integer, String> getIgnoreLists() {
        String qry = "SELECT id, chanlist FROM ignore_chan_favs";
        Statement s = null;
        ResultSet r = null;
        Map<Integer, String> map = new HashMap<Integer, String>();
        try {
            s = conn.createStatement();
            r = s.executeQuery(qry);
            while (r.next()) map.put(r.getInt(1), r.getString(2));
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        } finally {
            try {
                if (r != null) r.close();
                if (s != null) s.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return map;
    }

    synchronized void setSetting(String var, String val) {
        String qry = "REPLACE INTO settings (var, val) VALUES ('" + var + "', '" + val + "')";
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(qry);
            stmt.close();
            return;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return;
    }

    synchronized String getSetting(String var) {
        String qry = "SELECT val FROM settings WHERE var = '" + var + "'";
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(qry);
            if (rs.next()) return rs.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    synchronized String getSetting(String var, String defaultVal) {
        String val = getSetting(var);
        if (val == null) return defaultVal;
        return val;
    }
}
