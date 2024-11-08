package org.comoro.ext.util;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import javax.sql.DataSource;
import org.comoro.entity.Role;
import org.comoro.entity.User;
import org.comoro.management.RolesManager;

/**
 * Access to rights stored into relational database.
 * Singleton with initialization object and package visibility.
 */
final class RightsDAO {

    private static final Logger log = Logger.getLogger(RightsDAO.class.getPackage().getName());

    private static RightsDAO instance;

    /**
     * Initializes dao with provided data source.
     * Non concurrent method, be aware.
     */
    public static void init(final DataSource pool) {
        if (instance == null) {
            instance = new RightsDAO(pool);
        }
    }

    /**
     * Gets the only instance of this class.
     * @throws RuntimeException if <code>init</code> method has not been called
     * yet.
     */
    public static RightsDAO getInstance() {
        if (instance == null) {
            throw new RuntimeException("Persistence not configured yet!");
        } else return instance;
    }

    private final DataSource pool;

    private final RolesManager rolesManager;

    /**
     * Private constructor, since it's a singleton.
     */
    private RightsDAO(final DataSource pool) {
        this.pool = pool;
        this.rolesManager = RolesManager.getInstance();
    }

    /**
     * Adds a new right to rights list.
     */
    public void insertRight(final String right) throws IOException {
        try {
            Connection conn = null;
            boolean autoCommit = false;
            try {
                conn = pool.getConnection();
                autoCommit = conn.getAutoCommit();
                conn.setAutoCommit(true);
                final PreparedStatement insert = conn.prepareStatement("insert into rights (name) values (?)");
                insert.setString(1, right);
                insert.executeUpdate();
            } catch (Throwable t) {
                if (conn != null) conn.rollback();
                throw new SQLException(t.toString());
            } finally {
                if (conn != null) {
                    conn.setAutoCommit(autoCommit);
                    conn.close();
                }
            }
        } catch (final SQLException sqle) {
            log.log(Level.SEVERE, sqle.toString(), sqle);
            throw new IOException(sqle.toString());
        }
    }
}
