package com.apelon.dts.db;

import com.apelon.common.sql.SQL;
import com.apelon.common.xml.XML;
import com.apelon.common.xml.XMLException;
import com.apelon.dts.server.DTSPermission;
import com.apelon.dts.server.DTSUsers;
import com.apelon.dts.util.DTSXMLFactory;
import com.apelon.dts.util.DTSUtil;
import com.apelon.dts.common.DTSValidationException;
import com.apelon.dts.common.DTSDataLimits;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Adds, deletes, updates and finds Namespaces and Authorities.
 *
 * @since DTS 3.0
 *
 * Copyright (c) 2006 Apelon, Inc. All rights reserved.
 */
public class NameSpaceDb extends BasicDb {

    private static final String TABLE_KEY = "NAMESPACE_DB";

    private static final String COLUMN = "column";

    private static final String VALUE = "value";

    private static final String NAME = "name";

    private static final String ID = "id";

    private static final String CODE = "code";

    private static final String TYPE = "type";

    private static final String AUTHORITY_ID = "authorityId";

    private static final String REFERENCED_BY = "referencedBy";

    private static final String WRITABLE = "writable";

    private static final String DESCRIPTION = "description";

    private static Integer namespaceLock = new Integer(0);

    private static Map namespaceCache = new HashMap();

    private PreparedStatement insertSt = null;

    private PreparedStatement updateSt = null;

    private Statement createConceptSeqSt = null;

    private Statement createTermSeqSt = null;

    private PreparedStatement insertAuthSt = null;

    private Statement findNamespaceSt = null;

    private Statement dropConceptSeqSt = null;

    private Statement dropTermSeqSt = null;

    private String trueResult;

    private String falseResult;

    protected Statement keepAliveStmt = null;

    private PreparedStatement getExtendStmt = null;

    /**
  * Set the database connection in {@linkplain  BasicDb BasicDb}
  * and initiate by storing and preparing SQL statements.
  *
  * @param conn a java.sql.Connection for access to DTS schema.
  *
  * @since DTS 3.0
  */
    public NameSpaceDb(Connection conn) throws SQLException {
        super(conn);
        init();
    }

    private static void addCache(Integer id, String name, String local, String writable, String type) {
        synchronized (namespaceLock) {
            NamespaceEntry entry = new NamespaceEntry(id.intValue(), name, local, writable, type);
            namespaceCache.put(id, entry);
        }
    }

    private static void deleteCache(Integer id) {
        synchronized (namespaceLock) {
            namespaceCache.remove(id);
        }
    }

    private static void updateCache(Integer id, String writable) {
        synchronized (namespaceLock) {
            NamespaceEntry entry = (NamespaceEntry) namespaceCache.get(id);
            entry.writable = getBoolean(writable);
        }
    }

    static boolean isWritable(int id, BasicDb db) {
        synchronized (namespaceLock) {
            NamespaceEntry entry = (NamespaceEntry) namespaceCache.get(new Integer(id));
            if (entry == null) {
                try {
                    refillCache(db);
                } catch (SQLException e) {
                    return false;
                }
                entry = (NamespaceEntry) namespaceCache.get(new Integer(id));
            }
            if (entry == null) {
                throw new IllegalArgumentException("namespaceId: " + id + " does not exist");
            }
            return entry.writable;
        }
    }

    static NamespaceEntry getNamesapceEntry(int id, BasicDb db) {
        synchronized (namespaceLock) {
            NamespaceEntry entry = (NamespaceEntry) namespaceCache.get(new Integer(id));
            if (entry == null) {
                try {
                    refillCache(db);
                } catch (SQLException e) {
                    return null;
                }
                entry = (NamespaceEntry) namespaceCache.get(new Integer(id));
            }
            return entry;
        }
    }

    static boolean isLocal(int id, BasicDb db) {
        synchronized (namespaceLock) {
            NamespaceEntry entry = (NamespaceEntry) namespaceCache.get(new Integer(id));
            if (entry == null) {
                try {
                    refillCache(db);
                } catch (SQLException e) {
                    return false;
                }
                entry = (NamespaceEntry) namespaceCache.get(new Integer(id));
            }
            if (entry == null) {
                throw new IllegalArgumentException("namespaceId: " + id + " does not exist");
            }
            return entry.local;
        }
    }

    private void init() throws SQLException {
        String statement = getDAO().getStatement(TABLE_KEY, "INSERT");
        insertSt = conn.prepareStatement(statement);
        statement = getDAO().getStatement(TABLE_KEY, "INSERT_AUTH");
        insertAuthSt = conn.prepareStatement(statement);
        statement = getDAO().getStatement(TABLE_KEY, "UPDATE");
        updateSt = conn.prepareStatement(statement);
        createConceptSeqSt = conn.createStatement();
        createTermSeqSt = conn.createStatement();
        findNamespaceSt = conn.createStatement();
        dropConceptSeqSt = conn.createStatement();
        dropTermSeqSt = conn.createStatement();
        keepAliveStmt = conn.createStatement();
        String getStatement = getDAO().getStatement(TABLE_KEY, "GET_EXTENDING_NAMESPACES");
        getExtendStmt = conn.prepareStatement(getStatement);
        refillCache(this);
    }

    private static void refillCache(BasicDb db) throws SQLException {
        Statement stmt = null;
        synchronized (namespaceLock) {
            try {
                stmt = db.conn.createStatement();
                String statement = db.getDAO().getStatement(TABLE_KEY, "GET_NAMESPACE");
                statement += (" " + db.getDAO().getStatement(TABLE_KEY, "ORDER_NAMESPACE_BY"));
                ResultSet res = stmt.executeQuery(statement);
                while (res.next()) {
                    int id = res.getInt(1);
                    String name = res.getString(2);
                    String code = res.getString(3);
                    String referencedBy = res.getString(4);
                    int authorityId = res.getInt(5);
                    String isLocal = res.getString(6);
                    String isWritable = res.getString(7);
                    String hasSemanticType = res.getString(8);
                    String namespaceType = res.getString(9);
                    addCache(new Integer(id), name, isLocal, isWritable, namespaceType);
                }
                res.close();
                stmt.close();
            } catch (SQLException e) {
                throw e;
            } finally {
                if (stmt != null) {
                    stmt.close();
                }
            }
        }
    }

    public void close() throws SQLException {
        closeStatement(insertSt);
        closeStatement(createConceptSeqSt);
        closeStatement(createTermSeqSt);
        closeStatement(insertAuthSt);
        closeStatement(updateSt);
        updateSt = null;
        closeStatement(findNamespaceSt);
        closeStatement(dropConceptSeqSt);
        closeStatement(dropTermSeqSt);
        closeStatement(keepAliveStmt);
        closeStatement(getExtendStmt);
    }

    /**
  * Determine Namespace to add as indicated in the Element object, execute
  * statement that creates the Namespace.
  *
  * @param root a Xml DOM Element holding data about the Namespace to add.
  *
  * @return a String of Xml holding data about the added Namespace.
  *
  * @since DTS 3.0
  */
    public String addNamespace(Element root) throws Exception {
        NodeList children = root.getChildNodes();
        int len = children.getLength();
        Node node = children.item(0);
        String name = getAttribute(node, NAME);
        String id = getAttribute(node, ID);
        String code = getAttribute(node, CODE);
        String type = getAttribute(node, TYPE);
        String authority_id = getAttribute(node, AUTHORITY_ID);
        String referenced_by = getAttribute(node, REFERENCED_BY);
        String local = getAttribute(node, "local");
        String writable = getAttribute(node, "writable");
        String semanticType = getAttribute(node, "semanticType");
        name = DTSUtil.checkValue("Namespace Name", name, DTSDataLimits.LEN_NAME);
        code = DTSUtil.checkValue("Namespace Code", code, DTSDataLimits.LEN_CODE);
        insertSt.setInt(1, Integer.parseInt(id));
        insertSt.setString(2, name);
        insertSt.setString(3, code);
        insertSt.setString(4, referenced_by);
        insertSt.setString(5, authority_id);
        insertSt.setString(6, local);
        insertSt.setString(7, writable);
        insertSt.setString(8, semanticType);
        insertSt.setString(9, type);
        conn.setAutoCommit(false);
        try {
            int result = insertSt.executeUpdate();
            if (result != 1) {
                conn.rollback();
                return getFalseResult();
            }
            if (type.equals("E")) {
                if (!local.equals("T")) {
                    throw new DTSValidationException("Namespace of Ontylog Extension type should be a local.");
                }
                String linkedNSId = getAttribute(node, "linkedNSId");
                if (linkedNSId == null) {
                    throw new DTSValidationException("Linked namespace data is not found." + "Linked namespace is required to create an Ontylog Extension local namespace.");
                }
                boolean linkageAdded = addNamespaceLinkage(Integer.parseInt(id), Integer.parseInt(linkedNSId));
                if (!linkageAdded) {
                    conn.rollback();
                    return getFalseResult();
                }
            }
            addCache(new Integer(id), name, local, writable, type);
            conn.commit();
            return getTrueResult();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private boolean addNamespaceLinkage(int nsId, int subsNSId) throws SQLException {
        NamespaceEntry entry = getNamesapceEntry(subsNSId, this);
        if (!entry.type.equals("O")) {
            throw new RuntimeException("Cannot link namespace: Namespace " + entry.name + " is not an Ontylog Namespace");
        }
        String subsVersion = getLatestVersion(subsNSId);
        String sql = getDAO().getStatement(TABLE_KEY, "INSERT_LINKAGE");
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, nsId);
        ps.setInt(2, subsNSId);
        ps.setString(3, subsVersion);
        int result = ps.executeUpdate();
        ps.close();
        if (result != 1) {
            return false;
        }
        sql = getDAO().getStatement(TABLE_KEY, "INSERT_CLASSIFY_MONITOR");
        ps = conn.prepareStatement(sql);
        ps.setInt(1, nsId);
        result = ps.executeUpdate();
        ps.close();
        if (result != 1) {
            return false;
        }
        return true;
    }

    private String getLatestVersion(int namespaceId) throws SQLException {
        String version = null;
        String sql = getDAO().getStatement(TABLE_KEY, "GET_LATEST_VERSION");
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, namespaceId);
        ps.setInt(2, namespaceId);
        ResultSet res = ps.executeQuery();
        while (res.next()) {
            version = res.getString(1);
        }
        res.close();
        ps.close();
        return version;
    }

    /**
  * Determine Namespace to delete as indicated in the Element object,
  * run statement that deletes the Namespace.
  *
  * @param root a Xml DOM Element holding data about the Namespace to delete.
  *
  * @return a String of Xml holding data about the deleted Namespace.
  *
  * @since DTS 3.0
  */
    public String deleteNamespace(Element root) throws SQLException, XMLException {
        String column = root.getAttribute(COLUMN);
        String value = root.getAttribute(VALUE);
        return deleteNamespace(column, value);
    }

    /**
  * Retrieve Namespace info from the Xml Element, then populate and
  * execute SQL statement that updates the Namespace.
  *
  * @param root a Xml DOM Element holding data about the Namespace to update.
  *
  * @return a String of Xml holding data about the updated Namespace.
  *
  * @since DTS 3.0
  */
    public String updateNamespace(Element root) throws SQLException, XMLException {
        NodeList children = root.getChildNodes();
        int len = children.getLength();
        Node node = children.item(0);
        String oldId = root.getAttribute(ID);
        String name = getAttribute(node, NAME);
        String code = getAttribute(node, CODE);
        String type = getAttribute(node, TYPE);
        String authority_id = getAttribute(node, AUTHORITY_ID);
        String referenced_by = getAttribute(node, REFERENCED_BY);
        String local = getAttribute(node, "local");
        String writable = getAttribute(node, "writable");
        String semanticType = getAttribute(node, "semanticType");
        name = DTSUtil.checkValue("Namespace Name", name, DTSDataLimits.LEN_NAME);
        code = DTSUtil.checkValue("Namespace Code", code, DTSDataLimits.LEN_CODE);
        updateSt.setString(1, name);
        updateSt.setString(2, code);
        updateSt.setString(3, referenced_by);
        updateSt.setString(4, authority_id);
        updateSt.setString(5, local);
        updateSt.setString(6, writable);
        updateSt.setString(7, semanticType);
        updateSt.setString(8, type);
        updateSt.setString(9, oldId);
        int result = updateSt.executeUpdate();
        String response = null;
        if (result != 0) {
            addCache(new Integer(oldId), name, local, writable, type);
            response = getTrueResult();
        } else {
            response = getFalseResult();
        }
        return response;
    }

    /**
  * Get data about all Namespaces and their Authorities.
  *
  * @param root a Xml DOM Element holding data about the operation to execute.
  *
  * @return a String of Xml holding data about the Namespaces.
  *
  * @since DTS 3.0
  */
    public String getNamespaces(Element root, DTSPermission permit) throws SQLException, XMLException {
        StringBuffer result = new StringBuffer(500);
        String statement = getDAO().getStatement(TABLE_KEY, "GET_NAMESPACE");
        statement += (" " + getDAO().getStatement(TABLE_KEY, "ORDER_NAMESPACE_BY"));
        ResultSet res = keepAliveStmt.executeQuery(statement);
        appendDtd(result, com.apelon.dts.dtd.result.DTD.NAMESPACE_RESULT, "namespaces");
        XML.asStartTag(result, "namespaces");
        findNamespace(res, result, permit);
        XML.asEndTag(result, "namespaces");
        res.close();
        return result.toString();
    }

    /**
  * Retrieve Authority info from the Xml Element, then get,
  * populate and execute SQL statement that creates the Authority
  * in the database.
  *
  * @param root a Xml DOM Element holding data about the Authority to add.
  *
  * @return a String of Xml holding data about the added Authority.
  *
  * @since DTS 3.0
  */
    public String addAuthority(Element root) throws SQLException, XMLException {
        NodeList children = root.getChildNodes();
        int len = children.getLength();
        Node node = children.item(0);
        String id = getAttribute(node, ID);
        String description = getAttribute(node, DESCRIPTION);
        description = DTSUtil.checkValue("Authority Description", description, DTSDataLimits.LEN_DESC_AUTHORITY);
        insertAuthSt.setInt(1, Integer.parseInt(id));
        insertAuthSt.setString(2, description);
        int result = insertAuthSt.executeUpdate();
        String response = null;
        if (result != 1) {
            response = getFalseResult();
        } else {
            response = getTrueResult();
        }
        return response;
    }

    /**
  * Get Authority info from the Xml Element, then get,
  * populate and execute SQL statement that deletes the Authority
  * in the database.
  *
  * @param root a Xml DOM Element holding data about the Authority to delete.
  *
  * @return a String of Xml holding data about the deleted Authority.
  *
  * @since DTS 3.0
  */
    public String deleteAuthority(Element root) throws SQLException, XMLException {
        String column = root.getAttribute(COLUMN);
        String value = root.getAttribute(VALUE);
        return deleteAuthority(column, value);
    }

    /**
  * Get information about all Authorities in the database.
  *
  * @param root a Xml DOM Element holding data about the operation to execute.
  *
  * @return a String of Xml holding data about the Authorities.
  *
  * @since DTS 3.0
  */
    public String getAuthorities(Element root) throws SQLException, XMLException {
        StringBuffer result = new StringBuffer(500);
        String statement = getDAO().getStatement(TABLE_KEY, "GET_AUTHORITY");
        ResultSet res = keepAliveStmt.executeQuery(statement);
        appendDtd(result, com.apelon.dts.dtd.result.DTD.NAMESPACE_RESULT, "authorities");
        XML.asStartTag(result, "authorities");
        while (res.next()) {
            int id = res.getInt(1);
            String description = res.getString(2);
            result.append(DTSXMLFactory.createAuthorityXML(description, id));
        }
        XML.asEndTag(result, "authorities");
        res.close();
        return result.toString();
    }

    /**
  * Retrieve data about a given namespace such as version number and release date.
  *
  * @param root a Xml DOM Element holding data about the operation to execute.
  *
  * @return a String of Xml holding data about the content's version.
  *
  * @since DTS 3.0
  */
    public String getContentVersions(Element root) throws SQLException, XMLException {
        StringBuffer result = new StringBuffer(500);
        String statement = getDAO().getStatement(TABLE_KEY, "GET_CONTENTVERSION");
        String namespaceId = root.getAttribute(ID);
        ResultSet res = keepAliveStmt.executeQuery(statement + namespaceId);
        appendDtd(result, com.apelon.dts.dtd.result.DTD.NAMESPACE_RESULT, "contentversions");
        XML.asStartTag(result, "contentversions");
        while (res.next()) {
            int id = res.getInt(1);
            String code = res.getString(2);
            String name = res.getString(3);
            java.sql.Date date = res.getDate(4);
            result.append(DTSXMLFactory.createContentVersion(name, id, code, Integer.parseInt(namespaceId), date));
        }
        XML.asEndTag(result, "contentversions");
        res.close();
        return result.toString();
    }

    private String deleteAuthority(String column, String value) throws SQLException {
        String statement = getDAO().getStatement(TABLE_KEY, "DELETE_AUTH_BY_" + column.toUpperCase());
        int result = keepAliveStmt.executeUpdate(statement + "'" + value + "'");
        String response = null;
        if (result != 0) {
            response = getTrueResult();
        } else {
            response = getFalseResult();
        }
        return response;
    }

    /**
  * Find Namespace in the database as indicated in the Element object.
  *
  * @param root a Xml DOM Element holding data about the Namespace to find.
  *
  * @return a String of Xml holding data about the deleted Namespace.
  *
  * @since DTS 3.0
  */
    public String findNamespace(Element root, DTSPermission permit) throws SQLException, XMLException {
        String column = root.getAttribute(COLUMN);
        String value = root.getAttribute(VALUE);
        return findNamespace(column, value, permit);
    }

    private String findNamespace(String column, String value, DTSPermission permit) throws SQLException {
        StringBuffer response = new StringBuffer(200);
        appendDtd(response, com.apelon.dts.dtd.common.DTD.NAMESPACE, "namespace");
        String statement = getDAO().getStatement(TABLE_KEY, "GET_NAMESPACE");
        statement += (" " + getDAO().getStatement(TABLE_KEY, "FIND_NAMESPACE_BY_" + column.toUpperCase()));
        value = SQL.escapeSingleQoute(value);
        statement += ("'" + value + "'");
        ResultSet res = keepAliveStmt.executeQuery(statement);
        boolean hasResult = findNamespace(res, response, permit);
        res.close();
        if (!hasResult) {
            return nullDocument(com.apelon.dts.dtd.result.DTD.COMMON_RESULT);
        }
        return response.toString();
    }

    private String findNamespaceID(String column, String value) throws SQLException {
        String statement = getDAO().getStatement(TABLE_KEY, "GET_NAMESPACE");
        statement += (" " + getDAO().getStatement(TABLE_KEY, "FIND_NAMESPACE_BY_" + column.toUpperCase()));
        statement += ("'" + value + "'");
        ResultSet res = findNamespaceSt.executeQuery(statement);
        int id = -1;
        while (res.next()) {
            id = res.getInt(1);
        }
        String idString = "";
        if (id != -1) {
            idString = Integer.toString(id);
        }
        return idString;
    }

    protected boolean findNamespace(ResultSet res, StringBuffer response, DTSPermission permit) throws SQLException {
        boolean hasResult = false;
        String sql = getDAO().getStatement(TABLE_KEY, "GET_LINKED_NAMESPACE_ID");
        PreparedStatement ps = conn.prepareStatement(sql);
        while (res.next()) {
            int id = res.getInt(1);
            String name = res.getString(2);
            String code = res.getString(3);
            String referencedBy = res.getString(4);
            int authorityId = res.getInt(5);
            String isLocal = res.getString(6);
            String isWritable = res.getString(7);
            String hasSemanticType = res.getString(8);
            String namespaceType = res.getString(9);
            String authDesc = res.getString(10);
            int lnsId = -1;
            if (namespaceType.equals("E")) {
                lnsId = getLinkedNamespaceId(ps, id);
            }
            boolean writable = getBoolean(isWritable) && hasPermission(id, permit);
            response.append(DTSXMLFactory.createNamespaceXML(name, id, code, referencedBy, authDesc, authorityId, getBoolean(isLocal), writable, getBoolean(hasSemanticType), namespaceType.charAt(0), lnsId));
            hasResult = true;
        }
        ps.close();
        return hasResult;
    }

    private int getLinkedNamespaceId(PreparedStatement ps, int nsId) throws SQLException {
        int lnsId = -1;
        ps.setInt(1, nsId);
        ResultSet rset = ps.executeQuery();
        if (rset.next()) {
            lnsId = rset.getInt(1);
        }
        rset.close();
        return lnsId;
    }

    static boolean getBoolean(String value) {
        if (value.equals("T")) {
            return true;
        }
        return false;
    }

    private String deleteNamespace(String column, String value) throws SQLException {
        String namespaceId = findNamespaceID(column, value);
        String statement = getDAO().getStatement(TABLE_KEY, "DELETE_BY_" + column.toUpperCase());
        int result = keepAliveStmt.executeUpdate(statement + "'" + value + "'");
        String response = null;
        if (result != 0) {
            response = getTrueResult();
            deleteImportStatus(namespaceId);
            if (column.equals(ID)) {
                deleteCache(new Integer(value));
            }
        } else {
            response = getFalseResult();
        }
        String seqStatement = getDAO().getStatement(TABLE_KEY, "DROP_CONCEPT_SEQ");
        if (seqStatement.length() > 0) {
            SQL.dropSequence(conn, seqStatement + namespaceId);
        }
        seqStatement = getDAO().getStatement(TABLE_KEY, "DROP_TERM_SEQ");
        if (seqStatement.length() > 0) {
            SQL.dropSequence(conn, seqStatement + namespaceId);
        }
        return response;
    }

    /**
   * Delete the import status record for the given namespace.
   * @param namespaceId the namespace should be deleted from the dts_import_status table
   * @throws SQLException
   * @since DTS 3.4
   */
    private void deleteImportStatus(String namespaceId) throws SQLException {
        String statement = getDAO().getStatement(TABLE_KEY, "DELETE_IMPORT_STATUS") + namespaceId;
        Statement stmt = conn.createStatement();
        try {
            stmt.executeUpdate(statement);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    /**
  * Check if a user has permission to write to a certain namespace.
  *
  * @param root an Xml Element containing the given user and namespace information.
  * @param permit a DTSPermission object used to validate the user.
  *
  * @return a Xml String indicating if the user has permission to write to the namespace.
  *
  * @since DTS 3.0
  */
    public String hasPermission(Element root, DTSPermission permit) throws SQLException, XMLException {
        if (permit == null) {
            return getTrueResult();
        }
        String accessType = permit.getAccessType();
        if (accessType.equals(DTSUsers.WRITE_ACCESS_TYPE)) {
            String namespaceId = root.getAttribute(ID);
            Vector stored_namespace_ids = permit.getNamespaceIds();
            boolean isPermitted = stored_namespace_ids.contains(namespaceId);
            if (isPermitted) {
                return getTrueResult();
            }
        }
        return getFalseResult();
    }

    public boolean hasPermission(int namespaceId, DTSPermission permit) {
        if (permit == null) {
            return true;
        }
        String accessType = permit.getAccessType();
        if (accessType.equals(DTSUsers.WRITE_ACCESS_TYPE)) {
            String namespaceIdStr = String.valueOf(namespaceId);
            Vector stored_namespace_ids = permit.getNamespaceIds();
            boolean isPermitted = stored_namespace_ids.contains(namespaceIdStr);
            if (isPermitted) {
                return true;
            }
        }
        return false;
    }

    /**
  * Fetch extending namespaces that are in the dts_namespace_linkage table.
  *
  * @param namespaceId an int that is the namespace to get the linked namespaces.
  * @param permit a DTSPermission object used to validate the user.
  *
  * @return an Integer array that contains the ids of the extending namespaces.
  *
  * @since DTS 3.0
  */
    public Integer[] getExtendingNamespaces(int namespaceId, DTSPermission permit) throws SQLException, XMLException {
        getExtendStmt.setInt(1, namespaceId);
        ResultSet rs = getExtendStmt.executeQuery();
        ArrayList al = new ArrayList();
        if (rs != null) {
            while (rs.next()) {
                int nid = rs.getInt(1);
                al.add(new Integer(nid));
            }
            rs.close();
        }
        Integer[] na = new Integer[al.size()];
        na = (Integer[]) al.toArray(na);
        return na;
    }

    static class NamespaceEntry {

        String name;

        int id;

        boolean local;

        boolean writable;

        String type;

        public NamespaceEntry(int id, String name, String local, String writable, String type) {
            this.name = name;
            this.id = id;
            this.local = NameSpaceDb.getBoolean(local);
            this.writable = NameSpaceDb.getBoolean(writable);
            this.type = type;
        }
    }
}
