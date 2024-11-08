package fido.db;

import java.sql.*;
import java.util.*;
import fido.util.FidoDataSource;

/**
 * 
 */
public class UserTable {

    /**
	 * 
	 */
    public UserTable() {
    }

    private boolean contains(Statement stmt, String user) throws SQLException {
        ResultSet rs = null;
        try {
            String sql = "select count(1) from Principals where PrincipalId = '" + user + "'";
            rs = stmt.executeQuery(sql);
            if (rs.next() == false) throw new SQLException("No rows returned for count(1) query"); else {
                int num = rs.getInt(1);
                if (num == 1) return true;
                return false;
            }
        } finally {
            if (rs != null) rs.close();
        }
    }

    /**
	 * 
	 */
    public void add(String user, String pass, boolean admin, boolean developer) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                String sql;
                if (contains(stmt, user) == true) {
                    sql = "update Principals set Password = '" + pass + "' " + " where PrincipalId = '" + user + "'";
                } else {
                    sql = "insert into Principals (PrincipalId, Password) " + " values ('" + user + "', '" + pass + "')";
                }
                stmt.executeUpdate(sql);
                updateRoles(stmt, user, admin, developer);
                conn.commit();
            } catch (SQLException e) {
                if (conn != null) conn.rollback();
                throw e;
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    public void add(String user, boolean admin, boolean developer) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                updateRoles(stmt, user, admin, developer);
                conn.commit();
            } catch (SQLException e) {
                if (conn != null) conn.rollback();
                throw e;
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    private void updateRoles(Statement stmt, String user, boolean admin, boolean developer) throws SQLException {
        stmt.executeUpdate("delete from Roles where PrincipalId = '" + user + "'");
        if (admin == true) {
            stmt.executeUpdate("insert into Roles (PrincipalId, Role, RoleGroup) " + "values ('" + user + "', 'admin', 'Roles')");
        }
        if (developer == true) {
            stmt.executeUpdate("insert into Roles (PrincipalId, Role, RoleGroup) " + "values ('" + user + "', 'developer', 'Roles')");
        }
        stmt.executeUpdate("insert into Roles (PrincipalId, Role, RoleGroup) " + "values ('" + user + "', 'caller_" + user + "', 'CallerPrincipal')");
    }

    /**
	 * 
	 */
    public void delete(String user) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                stmt.executeUpdate("delete from Principals where PrincipalId = '" + user + "'");
                stmt.executeUpdate("delete from Roles where PrincipalId = '" + user + "'");
                conn.commit();
            } catch (SQLException e) {
                if (conn != null) conn.rollback();
                throw e;
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    /**
	 * Returns the GrammarLink type referrenced by the parameter <i>name</i>.
	 * @param type The type of link referrenced by <i>name</i>
	 * @return type of link, null if none found
	 */
    public Collection list() throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select a.PrincipalId, b.Role " + "from Principals a, Roles b " + "where b.RoleGroup = 'Roles' and a.PrincipalId = b.PrincipalId " + "order by a.PrincipalId";
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                User item = null;
                Vector list = new Vector();
                while (rs.next() == true) {
                    String user = rs.getString(1);
                    if ((item == null) || (item.getUsername().equals(user) == false)) {
                        item = new User(user);
                        list.add(item);
                    }
                    String role = rs.getString(2);
                    if (role.equals("admin") == true) item.setAdmin(); else if (role.equals("developer") == true) item.setDeveloper();
                }
                return list;
            } finally {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }
}
