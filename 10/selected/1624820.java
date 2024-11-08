package fido.db;

import java.sql.*;
import java.util.*;
import fido.util.FidoDataSource;

/**
 * Holds a list of ClassLinkTypes.  The list is a hashtable that maps link names to
 * ClassLinkType objects.  The list is stored in a database file.  After each
 * add, update, or removal of a ClassLinkType, the list is updated in the database.
 * @see ClassLinkType
 */
public class ClassLinkTypeTable {

    /**
	 * Used for the type of link value.  This value specifies the Fido system
	 * created the link, and a user may not modify or delete it.
	 */
    public static final int SYSTEM_LINK = 1;

    /**
	 * Used for the type of link value.  This value is the default for users
	 * adding link types.  Other users may modify or delete it.
	 */
    public static final int USER_LINK = 2;

    /**
	 * Constructs a new ClassLinkTypeList with no entries in the list.  The database
	 * reference is stored so any updates will store in the database.  Upon creation,
	 * this class will write itself to the database.  If an entry with the name
	 * ClassLinkTypeList already exists in the database, the object it points to is
	 * replaced.
	 * @param file database to store itself upon creation and any updates.
	 * @exception FidoIOException Input / Output error on creating the ClassLinkTypeList object
	 */
    public ClassLinkTypeTable() {
    }

    /**
	 * Adds or modifies a ClassLinkType.  The <i>name</i> parameter is the string
	 * used to reference the object in the list.  If an entry with this name
	 * already is in the list, the object replaced with the <i>linkType</i>
	 * object parameter.<P>
	 * The Fido system has a constraint that every link field name must be
	 * unique.  Therefore, the forward and backward fields of the ClassLinkType
	 * parameter are searched for in every other link type in the system.
	 * If any other enter contains the same field name, a DupulicateClassLinkField
	 * exception is thrown.<P>
	 * If there is not duplicate field exception, the list is not updated in the
	 * database file.<P>
	 * The forward field cannot be null.  If it is, a NullPointerException is
	 * thrown.
	 * @param linkType ClassLinkType object to add or replace an existing entry
	 * @exception FidoIOException Input / Output error saving ClassLinkTypeList
	 * @exception DuplicateLinkFieldException thrown if a field in the <i>linkType</i>
	 *            parameter already exists in another ClassLinkType.
	 * @exception NullPointerException thrown if the forwardField is null.
	 */
    public void add(String name) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                String sql = "insert into ClassLinkTypes (LinkName, LinkType) " + "values ('" + name + "', " + USER_LINK + ")";
                conn = FidoDataSource.getConnection();
                stmt = conn.createStatement();
                stmt.executeUpdate(sql);
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    /**
	 * 
	 * @param linkType ClassLinkType object to add or replace an existing entry
	 * @exception FidoIOException Input / Output error saving ClassLinkTypeList
	 */
    public boolean isSystemLink(Statement stmt, String name) throws SQLException, ClassLinkTypeNotFoundException {
        ResultSet rs = null;
        try {
            String sql = "select LinkType from ClassLinkTypes where LinkName = '" + name + "'";
            rs = stmt.executeQuery(sql);
            if (rs.next() == false) throw new ClassLinkTypeNotFoundException(name);
            int type = rs.getInt(1);
            if (type == SYSTEM_LINK) return true; else return false;
        } finally {
            if (rs != null) rs.close();
        }
    }

    /**
	 * Removes the ClassLinkType referenced by the parameters <i>name</i>.  If
	 * the list contains no entery with that name, this call does nothing.
	 * The list is only updated in the database file if the link type
	 * was acutally removed.
	 * @param name Name of the ClassLinkType to remove
	 * @exception FidoIOException Input / Output error saving ClassLinkTypeList
	 * @exception CannotDeleteSystemLinkException Attempt to delete a link
	 *            deemed as a System link.  These cannot be modified, but
	 *            ususally have another method of modifying the link.
	 */
    public void delete(String name) throws FidoDatabaseException, CannotDeleteSystemLinkException, ClassLinkTypeNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                if (isSystemLink(stmt, name) == true) throw new CannotDeleteSystemLinkException(name);
                AdjectivePrepositionTable prepTable = new AdjectivePrepositionTable();
                prepTable.deleteLinkType(stmt, name);
                ObjectLinkTable objectLinkTable = new ObjectLinkTable();
                objectLinkTable.deleteLinkType(stmt, name);
                String sql = "delete from ClassLinkTypes where LinkName = '" + name + "'";
                stmt.executeUpdate(sql);
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
	 * Tests for the existance of a link type.
	 * @param name Name of the ClassLinkType to test for
	 * @return true if the link type is in the list, false otherwise
	 */
    public boolean contains(String name) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select count(1) from ClassLinkTypes where LinkName = '" + name + "'";
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                if (rs.next() == false) throw new SQLException("No rows returned for count(1) query"); else {
                    int num = rs.getInt(1);
                    if (num == 1) return true;
                    return false;
                }
            } finally {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    /**
	 * Returns the names of all ClassLinkType entries in the list.  The names
	 * of the link types are in alphabetical order.
	 * @return list of link type names
	 */
    public Collection list() throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select a.LinkName, a.LinkType, b.Description from ClassLinkTypes a, ClassLinkTypeTypes b " + "where a.LinkType = b.LinkType order by LinkName";
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                Vector list = new Vector();
                while (rs.next() == true) {
                    ClassLinkType type = new ClassLinkType(rs.getString(1), rs.getInt(2), rs.getString(3));
                    list.add(type);
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

    public Collection listLinkNames() throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select LinkName from ClassLinkTypes where LinkType = " + USER_LINK + " order by LinkName";
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                Vector list = new Vector();
                while (rs.next() == true) list.add(rs.getString(1));
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
