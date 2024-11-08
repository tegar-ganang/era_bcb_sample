package fido.db;

import java.sql.*;
import java.util.*;
import fido.util.FidoDataSource;

/**
 * 
 */
public class AttributeCategoryTable {

    /**
	 * Constructs a new AttributeList with no entries in the list.  The database
	 * reference is stored so any updates will store in the database.  Upon creation,
	 * this class will write itself to the database.  If an entry with the name
	 * AttributeList already exists in the database, the object it points to is
	 * replaced.
	 * @param file database to store itself upon creation and any updates.
	 * @exception FidoIOException Input / Output error on creating the AttributeList object
	 */
    public AttributeCategoryTable() {
    }

    /**
	 * Adds or modifies a AttributeList.  The <i>name</i> parameter is the string
	 * used to reference the object in the list.  If an entry with this name
	 * already is in the list, the object replaced with the <i>type</i>
	 * parameter.<P>
	 * @param name name of the adjective to add to the list
	 * @param linkName name of the link 
	 * @exception SQLException Input / Output error saving Attributes
	 */
    public void add(String name) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                if (contains(name) == false) {
                    String sql = "insert into AttributeCategories (CategoryName) " + "values ('" + name + "')";
                    conn = FidoDataSource.getConnection();
                    stmt = conn.createStatement();
                    stmt.executeUpdate(sql);
                }
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
	 * 
	 * 
	 * 
	 * @param name name of the adjective to add to the list
	 * @param linkName name of the link 
	 * @exception FidoIOException Input / Output error saving Attributes
	 */
    public boolean contains(String name) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select count(1) from AttributeCategories " + "where CategoryName = '" + name + "'";
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
	 * Removes the Attribute referenced by the parameters <i>name</i>.  If
	 * the list contains no entery with that name, this call does nothing.
	 * The list is only updated in the database file if the link type
	 * was acutally removed.
	 * @param name Name of the Attribute to remove
	 * @exception FidoIOException Input / Output error saving GrammarLinkList
	 */
    public void delete(String name) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                AttributeTable attribute = new AttributeTable();
                attribute.deleteAllForType(stmt, name);
                String sql = "delete from AttributeCategories " + "where CategoryName = '" + name + "'";
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
	 * Returns the names of all Attribute names in the list.  The names
	 * of the links are in alphabetical order.
	 * @return list of link names
	 */
    public Collection list() throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select CategoryName from AttributeCategories order by CategoryName";
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                Vector cats = new Vector();
                while (rs.next() == true) cats.add(rs.getString(1));
                return cats;
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
