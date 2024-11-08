package org.jwaim.core.storage.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.jwaim.core.JWAIMStatus;

/**
 * Database operations for status
 */
final class StatusOperations {

    static final void saveStatus(JWAIMStatus status, DBConnector connector) throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        Statement st = null;
        try {
            con = connector.getDB();
            con.setAutoCommit(false);
            st = con.createStatement();
            st.executeUpdate("DELETE FROM status");
            ps = con.prepareStatement("INSERT INTO status VALUES (?, ?)");
            ps.setString(1, "jwaim.status");
            ps.setBoolean(2, status.getJWAIMStatus());
            ps.addBatch();
            ps.setString(1, "logging.status");
            ps.setBoolean(2, status.getLoggingStatus());
            ps.addBatch();
            ps.setString(1, "stats.status");
            ps.setBoolean(2, status.getStatsStatus());
            ps.addBatch();
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

    static final JWAIMStatus loadStatus(DBConnector connector) throws IOException {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        boolean jwaim = false;
        boolean logging = false;
        boolean stats = false;
        try {
            con = connector.getDB();
            st = con.createStatement();
            rs = st.executeQuery("SELECT * FROM status");
            while (rs.next()) {
                String key = rs.getString("key");
                boolean status = rs.getBoolean("status");
                if ("jwaim.status".equals(key)) jwaim = status; else if ("logging.status".equals(key)) logging = status; else if ("stats.status".equals(key)) stats = status;
            }
            return new JWAIMStatus(jwaim, logging, stats);
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
    }
}
