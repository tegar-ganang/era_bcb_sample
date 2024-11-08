package org.comoro.dao;

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
 * Access to user objects stored into relational database.
 * Singleton with initialization object.
 */
public final class UsersDAO extends AbstractDAO {

    private static final Logger log = Logger.getLogger(UsersDAO.class.getPackage().getName());

    private static UsersDAO instance;

    /**
     * Initializes dao with provided data source.
     * Non concurrent method, be aware.
     */
    public static void init(final DataSource pool) {
        if (instance == null) {
            instance = new UsersDAO(pool);
        }
        instance.testVersion(pool, "$Id: UsersDAO.java,v 1.11 2008/08/12 08:00:21 pierluiggi Exp $");
    }

    /**
     * Gets the only instance of this class.
     * @throws RuntimeException if <code>init</code> method has not been called
     * yet.
     */
    public static UsersDAO getInstance() {
        if (instance == null) {
            throw new RuntimeException("Persistence not configured yet!");
        } else return instance;
    }

    private final DataSource pool;

    private final RolesManager rolesManager;

    /**
     * Private constructor, since it's a singleton.
     */
    private UsersDAO(final DataSource pool) {
        this.pool = pool;
        this.rolesManager = RolesManager.getInstance();
    }

    /**
     * Gets user from her identity name.
     * @return null if no user with given identity exists.
     */
    public User getUserFromIdentity(final String identity) throws IOException {
        return this.getUserFromLogin(identity);
    }

    /**
     * Gets user from her login name.
     * @return null if no user with given login.
     */
    public User getUserFromLogin(final String login) throws IOException {
        try {
            Connection conn = null;
            try {
                conn = pool.getConnection();
                final PreparedStatement selectUser = conn.prepareStatement("select users.userId,users.mainRoleId,userRoles.roleId " + "from users, userRoles " + "where users.userId = userRoles.userId and " + "users.userId = ?");
                selectUser.setString(1, login);
                final ResultSet rs = selectUser.executeQuery();
                if (rs.next()) {
                    final Role mainRole = rolesManager.getRole(rs.getInt(2));
                    final Set<Role> roles = new HashSet<Role>();
                    roles.add(rolesManager.getRole(rs.getInt(3)));
                    while (rs.next()) {
                        roles.add(rolesManager.getRole(rs.getInt(3)));
                    }
                    return new User(login, roles, mainRole);
                } else return null;
            } finally {
                if (conn != null) conn.close();
            }
        } catch (final SQLException sqle) {
            log.log(Level.SEVERE, sqle.toString(), sqle);
            throw new IOException(sqle.toString());
        }
    }

    /**
     * Adds a new user to users list.
     */
    public void insertUser(final User user) throws IOException {
        try {
            Connection conn = null;
            boolean autoCommit = false;
            try {
                conn = pool.getConnection();
                autoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);
                final PreparedStatement insertUser = conn.prepareStatement("insert into users (userId, mainRoleId) values (?,?)");
                log.finest("userId= " + user.getUserId());
                insertUser.setString(1, user.getUserId());
                log.finest("mainRole= " + user.getMainRole().getId());
                insertUser.setInt(2, user.getMainRole().getId());
                insertUser.executeUpdate();
                final PreparedStatement insertRoles = conn.prepareStatement("insert into userRoles (userId, roleId) values (?,?)");
                for (final Role role : user.getRoles()) {
                    insertRoles.setString(1, user.getUserId());
                    insertRoles.setInt(2, role.getId());
                    insertRoles.executeUpdate();
                }
                conn.commit();
            } catch (Throwable t) {
                if (conn != null) conn.rollback();
                log.log(Level.SEVERE, t.toString(), t);
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

    /**
     * Updates user roles.
     */
    public void updateUser(final User user) throws IOException {
        try {
            Connection conn = null;
            boolean autoCommit = false;
            try {
                conn = pool.getConnection();
                autoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);
                final PreparedStatement updateUser = conn.prepareStatement("update users set mainRoleId=? where userId=?");
                updateUser.setInt(1, user.getMainRole().getId());
                updateUser.setString(2, user.getUserId());
                updateUser.executeUpdate();
                final PreparedStatement deleteRoles = conn.prepareStatement("delete from userRoles where userId=?");
                deleteRoles.setString(1, user.getUserId());
                deleteRoles.executeUpdate();
                final PreparedStatement insertRoles = conn.prepareStatement("insert into userRoles (userId, roleId) values (?,?)");
                for (final Role role : user.getRoles()) {
                    insertRoles.setString(1, user.getUserId());
                    insertRoles.setInt(2, role.getId());
                    insertRoles.executeUpdate();
                }
                conn.commit();
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

    /**
     * Deletes a user from the users list.
     */
    public void removeUser(final User user) throws IOException {
        try {
            Connection conn = null;
            boolean autoCommit = false;
            try {
                conn = pool.getConnection();
                autoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);
                final PreparedStatement removeUser = conn.prepareStatement("delete from users  where userId = ?");
                removeUser.setString(1, user.getUserId());
                removeUser.executeUpdate();
                final PreparedStatement deleteRoles = conn.prepareStatement("delete from userRoles where userId=?");
                deleteRoles.setString(1, user.getUserId());
                deleteRoles.executeUpdate();
                conn.commit();
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

    /**
     * Gets a list of all user's logins.
     */
    public String[] getAllLogins() throws IOException {
        try {
            final List<String> logins = new ArrayList<String>();
            Connection conn = null;
            try {
                conn = pool.getConnection();
                final PreparedStatement select = conn.prepareStatement("select userId from users");
                final ResultSet rs = select.executeQuery();
                while (rs.next()) {
                    logins.add(rs.getString(1));
                }
                return logins.toArray(new String[0]);
            } finally {
                if (conn != null) conn.close();
            }
        } catch (final SQLException sqle) {
            log.log(Level.SEVERE, sqle.toString(), sqle);
            throw new IOException(sqle.toString());
        }
    }

    /**
     * Gets a list of all user's logins with a given main role.
     */
    public String[] getAllLogins(final String roleName) throws IOException {
        try {
            Connection conn = null;
            try {
                final List<String> logins = new ArrayList<String>();
                conn = pool.getConnection();
                final PreparedStatement select = conn.prepareStatement("select users.userId from users, roles " + "where users.mainRoleId=roles.roleId " + "and roles.roleName=?");
                select.setString(1, roleName);
                final ResultSet rs = select.executeQuery();
                while (rs.next()) {
                    logins.add(rs.getString(1));
                }
                return logins.toArray(new String[0]);
            } finally {
                if (conn != null) conn.close();
            }
        } catch (final SQLException sqle) {
            log.log(Level.SEVERE, sqle.toString(), sqle);
            throw new IOException(sqle.toString());
        }
    }
}
