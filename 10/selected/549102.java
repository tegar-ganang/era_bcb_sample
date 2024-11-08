package com.bs.xdbms.xmldb.modules;

import org.xmldb.api.base.*;
import org.xmldb.api.modules.CollectionManagementService;
import com.bs.xdbms.xmldb.base.ServiceImpl;
import com.bs.xdbms.xmldb.base.CollectionImpl;
import com.bs.xdbms.util.*;
import com.bs.xdbms.persistence.OIDManager;
import java.sql.*;
import java.util.Vector;

/**
 * CollectionManagementService is a <code>Service</code> that enables the basic
 * management of collections within a database. The functionality provided is
 * very basic because collection management varies widely among databases. This
 * service simply provides functionality for those databases that are able
 * to implement this basic functionality.
 */
public final class CollectionManagementServiceImpl extends ServiceImpl implements CollectionManagementService {

    public String getName() {
        return "CollectionManagementService";
    }

    /**
    * Creates a new <code>Collection</code> in the database. The default 
    * configuration of the database is determined by the implementer. The 
    * new <code>Collection</code> will be created relative to the <code>
    * Collection</code> from which the <code>CollectionManagementService</code>
    * was retrieved.
    *
    * @param name The name of the collection to create.
    * @return The created <code>Collection</code> instance.
    * @exception XMLDBException with expected error codes.<br />
    *  <code>ErrorCodes.VENDOR_ERROR</code> for any vendor
    *  specific errors that occur.<br />
    */
    public Collection createCollection(String name) throws XMLDBException {
        Connection conn = null;
        Collection coll = null;
        try {
            conn = ConnectionManager.createConnection();
            conn.setAutoCommit(false);
            String sql = "INSERT INTO XDB_COLLECTION (XDB_COLLECTION_OID,NAME,PARENT) VALUES (?,?,?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            long oid = getNewCollectionOID(conn);
            long parentOID = getParentCollectionOID();
            pstmt.setLong(1, oid);
            pstmt.setString(2, name);
            pstmt.setLong(3, parentOID);
            pstmt.executeUpdate();
            pstmt.close();
            coll = new CollectionImpl(name, oid, collection);
            coll.setProperty(Constants.XDB_COLLECTION_ABSOLUTE_PATH, collection.getProperty(Constants.XDB_COLLECTION_ABSOLUTE_PATH) + "/" + name);
            conn.commit();
            conn.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (java.sql.SQLException se) {
            se.printStackTrace();
        }
        return coll;
    }

    private long getNewCollectionOID(Connection conn) throws SQLException {
        OIDManager oidManager = new OIDManager(conn);
        return oidManager.getUniqueOID("collection");
    }

    private long getParentCollectionOID() throws XMLDBException {
        return stringToLong(collection.getProperty(Constants.XDB_COLLECTION_OID));
    }

    private String longToString(long l) {
        return (new Long(l)).toString();
    }

    private long stringToLong(String s) {
        return (new Long(s)).longValue();
    }

    /**
    * Removes a named <code>Collection</code> from the system. The 
    * name for the <code>Collection</code> to remove is relative to the <code>
    * Collection</code> from which the <code>CollectionManagementService</code>
    * was retrieved.
    *
    * @param name The name of the collection to remove.
    * @exception XMLDBException with expected error codes.<br />
    *  <code>ErrorCodes.VENDOR_ERROR</code> for any vendor
    *  specific errors that occur.<br />
    */
    public void removeCollection(String name) throws XMLDBException {
        Connection conn = null;
        Collection coll = null;
        try {
            conn = ConnectionManager.createConnection();
            conn.setAutoCommit(false);
            String sql = "SELECT XDB_COLLECTION_OID FROM XDB_COLLECTION ";
            sql += "WHERE NAME = ? AND PARENT = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            long parentOID = stringToLong(collection.getProperty(Constants.XDB_COLLECTION_OID));
            pstmt.setString(1, name);
            pstmt.setLong(2, parentOID);
            ResultSet rs = pstmt.executeQuery();
            long oid = -1;
            while (rs.next()) oid = rs.getLong(1);
            pstmt.close();
            removeCollection(oid, conn);
            conn.commit();
            conn.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (java.sql.SQLException se) {
            try {
                conn.rollback();
            } catch (java.sql.SQLException se2) {
                se2.printStackTrace();
            }
            se.printStackTrace();
        }
    }

    private void removeCollection(long oid, Connection conn) throws XMLDBException {
        try {
            String sql = "DELETE FROM X_DOCUMENT WHERE X_DOCUMENT.XDB_COLLECTION_OID = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, oid);
            pstmt.executeUpdate();
            pstmt.close();
            sql = "DELETE FROM XDB_COLLECTION WHERE XDB_COLLECTION.XDB_COLLECTION_OID = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, oid);
            pstmt.executeUpdate();
            pstmt.close();
            removeChildCollection(oid, conn);
        } catch (java.sql.SQLException se) {
            try {
                conn.rollback();
            } catch (java.sql.SQLException se2) {
                se2.printStackTrace();
            }
            se.printStackTrace();
        }
    }

    private void removeChildCollection(long parentOID, Connection conn) throws XMLDBException {
        try {
            String sql = "SELECT XDB_COLLECTION_OID FROM XDB_COLLECTION WHERE PARENT = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, parentOID);
            ResultSet rs = pstmt.executeQuery();
            long oid = -1;
            Vector v = new Vector();
            while (rs.next()) {
                oid = rs.getLong(1);
                v.addElement(longToString(oid));
            }
            pstmt.close();
            for (int i = 0; i < v.size(); i++) removeCollection(stringToLong((String) v.elementAt(i)), conn);
        } catch (java.sql.SQLException se) {
            try {
                conn.rollback();
            } catch (java.sql.SQLException se2) {
                se2.printStackTrace();
            }
            se.printStackTrace();
        }
    }
}
