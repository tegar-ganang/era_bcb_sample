package com.apelon.dts.db;

import com.apelon.common.util.db.dao.GeneralDAO;
import com.apelon.common.xml.*;
import com.apelon.dts.server.DTSPermission;
import com.apelon.dts.server.PermissionException;
import com.apelon.dts.util.DTSXMLFactory;
import com.apelon.dts.db.modules.*;
import org.w3c.dom.*;
import java.sql.*;
import java.util.*;

/**
 * Add, delete, update and retrieve Association Types
 * as well as Concept, Term and Synonym Associations.
 *
 * @since DTS 3.0
 *
 * Copyright (c) 2006 Apelon, Inc. All rights reserved.
 */
public class AssociationDb extends QualifierTypeDb {

    /**
   * Use to indicate which group of SQL statements to retrieve from
   * the DAO oracle.xml or sql2k.xml file in com.apelon.dts.db.config.
   *
   * @since DTS 3.0
   *
   * Copyright (c) 2006 Apelon, Inc. All rights reserved.
   */
    private static final String TABLE_KEY = "ASSOCIATION_DB";

    private static final String COLUMN = "column";

    private static final String VALUE = "value";

    private static final String NAME = "name";

    private static final String ID = "id";

    private static final String CODE = "code";

    private static final String NAMESPACE_ID = "namespaceId";

    private static final String PURPOSE = "purpose";

    private static final String CONNECT_TYPE = "connectType";

    private static final String INVERSE_NAME = "inverseName";

    private static final String ASSOCIATION_ID = "associationId";

    private static final String ASSOCIATION_NAMESPACE_ID = "associationNamespaceId";

    private static final String ASSOCIATION_TYPES = "associationTypes";

    private static final String FROM_NAMESPACE_ID = "fromNamespaceId";

    private static final String TO_NAMESPACE_ID = "toNamespaceId";

    private static final String TO_ID = "toId";

    private static final String FROM_ID = "fromId";

    private static final String FROM_MODE = "fromMode";

    private static final String TO_MODE = "toMode";

    private static final String MODE = "mode";

    private static final String OPERATION = "operation";

    private static final String SOURCE = "source";

    private static final String TARGET = "target";

    private static final String SOURCE_NAMESPACEID = "sourceNamespaceId";

    private static final String TARGET_NAMESPACEID = "targetNamespaceId";

    private static final String ASSOCIATION = "association";

    private static final String ASSOC_NAMESPACEID = "associationNamespaceId";

    private static final String CONNECT = "connect";

    private static final String SYNONYM = "SYNONYM";

    private PreparedStatement insertSt = null;

    private PreparedStatement updateSt = null;

    private Statement qualifierStmt;

    private Map stmtMap = new HashMap();

    /**
  * Set the database connection in {@linkplain  BasicDb BasicDb}
  * and initiate by storing and preparing SQL statements.
  *
  * @param conn a java.sql.Connection for access to DTS schema.
  *
  * @since DTS 3.0
  */
    public AssociationDb(Connection conn) throws SQLException {
        super(conn);
        init();
    }

    /**
  * Call {@linkplain  BasicDb#getDAO() BasicDb.getDAO()} to get
  * a SQL statement, prepare the statement then store it
  * in a private HashMap.
  *
  * @param key a String indicating which SQL statement to get from the DAO
  *
  * @since DTS 3.0
  */
    private void storeStmt(String key) throws SQLException {
        String statement = getDAO().getStatement(TABLE_KEY, key);
        PreparedStatement pStmt = conn.prepareStatement(statement);
        stmtMap.put(key, pStmt);
    }

    private PreparedStatement getStmt(String key) throws SQLException {
        return (PreparedStatement) stmtMap.get(key);
    }

    private void init() throws SQLException {
        String statement = getDAO().getStatement(TABLE_KEY, "INSERT");
        insertSt = conn.prepareStatement(statement);
        statement = getDAO().getStatement(TABLE_KEY, "UPDATE");
        updateSt = conn.prepareStatement(statement);
        storeStmt("FIND_CONCEPT_ASSOCIATION_BY_NAME");
        storeStmt("FIND_CONCEPT_ASSOCIATION_BY_ID");
        storeStmt("FIND_CONCEPT_ASSOCIATION_BY_CODE");
        storeStmt("FIND_TERM_ASSOCIATION_BY_NAME");
        storeStmt("FIND_TERM_ASSOCIATION_BY_ID");
        storeStmt("FIND_TERM_ASSOCIATION_BY_CODE");
        qualifierStmt = conn.createStatement();
    }

    public void close() throws SQLException {
        super.close();
        if (insertSt != null) {
            insertSt.close();
        }
        if (updateSt != null) {
            updateSt.close();
        }
        Iterator iter = stmtMap.values().iterator();
        while (iter.hasNext()) {
            PreparedStatement pStmt = (PreparedStatement) iter.next();
            pStmt.close();
        }
        stmtMap.clear();
        if (qualifierStmt != null) {
            qualifierStmt.close();
        }
    }

    /**
  * Get Association Type info from the Xml Element, verify user, then get,
  * populate and execute SQL statement that creates the Association Type
  * in the database.
  *
  * @param root a Xml DOM Element holding data about the Association Type to add.
  * @param permit a DTSPermission object used to validate the user.
  *
  * @return a String of Xml holding data about the added Association Type.
  *
  * @since DTS 3.0
  */
    public String addAssociationType(Element root, DTSPermission permit) throws SQLException, XMLException, PermissionException {
        NodeList children = root.getChildNodes();
        int len = children.getLength();
        Node node = children.item(0);
        String name = getAttribute(node, NAME);
        String id = getAttribute(node, ID);
        String code = getAttribute(node, CODE);
        String namespaceId = getAttribute(node, NAMESPACE_ID);
        String connectType = getAttribute(node, CONNECT_TYPE);
        String purpose = getAttribute(node, PURPOSE);
        String inverseName = getAttribute(node, INVERSE_NAME);
        checkPermission(permit, namespaceId);
        int namespaceIdInt = Integer.parseInt(namespaceId);
        int idInt = Integer.parseInt(id);
        if (idInt == -1) {
            idInt = getNextFormId(TABLE_KEY, namespaceId);
        }
        if ((code == null) || code.equals("") || code.equals("null")) {
            code = generateCode(idInt);
        }
        insertSt.setLong(1, getGID(namespaceIdInt, idInt));
        insertSt.setInt(2, idInt);
        insertSt.setString(3, code);
        insertSt.setString(4, name);
        insertSt.setInt(5, Integer.parseInt(namespaceId));
        insertSt.setString(6, connectType);
        insertSt.setString(7, purpose);
        insertSt.setString(8, inverseName);
        insertSt.setNull(9, Types.INTEGER);
        int result = insertSt.executeUpdate();
        StringBuffer buff = new StringBuffer(200);
        appendDtd(buff, com.apelon.dts.dtd.common.DTD.ASSOCIATIONTYPE, "associationType");
        getAssociationType("ID", Integer.toString(idInt), namespaceId, buff);
        return buff.toString();
    }

    /**
  * Get Association Type info from the Xml Element, verify user, then get,
  * populate and execute SQL statement that deletes the Association Type
  * in the database.
  *
  * @param root a Xml DOM Element holding data about the Association Type to delete.
  * @param permit a DTSPermission object used to validate the user.
  *
  * @return a String of Xml holding data about the deleted Association Type.
  *
  * @since DTS 3.0
  */
    public String deleteAssociationType(Element root, DTSPermission permit) throws SQLException, XMLException, PermissionException {
        String id = root.getAttribute(ID);
        String namespaceId = root.getAttribute(NAMESPACE_ID);
        String connect = root.getAttribute(CONNECT);
        checkPermission(permit, namespaceId);
        boolean exist = isAssociationTypeUsed(Integer.parseInt(namespaceId), Integer.parseInt(id), connect);
        if (exist) {
            throw new IllegalArgumentException("any concept or term attached to type exists");
        }
        String statement = getDAO().getStatement(TABLE_KEY, "DELETE");
        statement = getDAO().getStatement(statement, 1, "'" + id + "'");
        statement += namespaceId;
        int result = keepAliveStmt.executeUpdate(statement);
        String response = null;
        if (result != 1) {
            response = getFalseResult();
        } else {
            response = getTrueResult();
        }
        return response;
    }

    /**
   * Returns whether an association type for given ID , namespace ID and connect code
   * is used in an association definition or not.
   *
   * @param namespaceId Namespace Id of Association Type
   * @param id Id of Association Type
   * @param connect Char code of the type of association
   * @return true if the Association Type is used in association defintion else false.
   * @throws SQLException for any Database Error
   */
    public boolean isAssociationTypeUsed(int namespaceId, int id, String connect) throws SQLException {
        String statement = getDAO().getStatement(TABLE_KEY, "EXIST_ASSOCIATION_TYPE_" + connect);
        long gid = this.getGID(namespaceId, id);
        statement = getDAO().getStatement(statement, 1, String.valueOf(gid));
        return exists(statement);
    }

    /**
  * Replace previous with new Association Type.
  *
  * @param root a Xml DOM Element holding info indicating the type of operation.
  * @param permit a DTSPermission object used to validate the user.
  *
  * @return a String of Xml holding data about the processed Association.
  *
  * @since DTS 3.0
  */
    public String updateAssociationType(Element root, DTSPermission permit) throws SQLException, XMLException, PermissionException {
        String oldId = root.getAttribute(ID);
        String oldNamespaceId = root.getAttribute(NAMESPACE_ID);
        checkPermission(permit, oldNamespaceId);
        NodeList children = root.getChildNodes();
        int len = children.getLength();
        Node node = children.item(0);
        String name = getAttribute(node, NAME);
        String id = getAttribute(node, ID);
        String code = getAttribute(node, CODE);
        String namespaceId = getAttribute(node, NAMESPACE_ID);
        String connectType = getAttribute(node, CONNECT_TYPE);
        String purpose = getAttribute(node, PURPOSE);
        String inverseName = getAttribute(node, INVERSE_NAME);
        checkPermission(permit, namespaceId);
        assertUpdate(oldId.equals(id), "old Id: " + oldId + " should be same as new id: " + id);
        assertUpdate(oldNamespaceId.equals(namespaceId), "old namespaceId: " + oldNamespaceId + " should be same as new namespaceId: " + namespaceId);
        updateSt.setString(1, name);
        updateSt.setString(2, connectType);
        updateSt.setString(3, purpose);
        updateSt.setString(4, inverseName);
        updateSt.setInt(5, Integer.parseInt(oldId));
        updateSt.setInt(6, Integer.parseInt(oldNamespaceId));
        int result = updateSt.executeUpdate();
        StringBuffer buff = new StringBuffer(200);
        appendDtd(buff, com.apelon.dts.dtd.common.DTD.ASSOCIATIONTYPE, "associationType");
        getAssociationType("ID", id, namespaceId, buff);
        return buff.toString();
    }

    /**
  * Get Association Type info from db by name, code or id.
  *
  * @param root a Xml DOM Element holding data about the Association Type to get.
  *
  * @return a String of Xml holding data about the retrieved Association Type.
  *
  * @since DTS 3.0
  */
    public String getAssociationType(Element root) throws SQLException, XMLException {
        String column = root.getAttribute(COLUMN);
        String value = root.getAttribute(VALUE);
        String namespaceId = root.getAttribute(NAMESPACE_ID);
        StringBuffer response = new StringBuffer(200);
        appendDtd(response, com.apelon.dts.dtd.common.DTD.ASSOCIATIONTYPE, "associationType");
        long gid = getAssociationType(column, value, namespaceId, response);
        if (gid == -1) {
            return nullDocument(com.apelon.dts.dtd.result.DTD.COMMON_RESULT);
        }
        return response.toString();
    }

    private long getAssociationType(String column, String value, String namespaceId, StringBuffer response) throws SQLException {
        return getAssociationType(keepAliveStmt, column, value, namespaceId, response, getDAO());
    }

    /**
  * Get Association Type info from db by name, code or id.
  *
  * @param stmt Statement that keeps alive until the connection is closed.
  * @param column String indicating which column to select by such as name, id or code.
  * @param value String that is the value to select by.
  * @param namespaceId String indicating the id of the namespace to select in.
  * @param response StringBuffer to hold the Xml result.
  * @param dao GeneralDAO holding SQL statements.
  *
  * @return a String of Xml holding data about the retrieved Association Type.
  *
  * @since DTS 3.0
  */
    public static long getAssociationType(Statement stmt, String column, String value, String namespaceId, StringBuffer response, GeneralDAO dao) throws SQLException {
        String statement = dao.getStatement(TABLE_KEY, "GET_TYPE_BY_" + column.toUpperCase());
        statement = dao.getStatement(statement, 1, "'" + value + "'");
        ResultSet res = stmt.executeQuery(statement + namespaceId);
        long gid = -1;
        while (res.next()) {
            gid = res.getLong(1);
            int id = res.getInt(2);
            String code = res.getString(3);
            String name = res.getString(4);
            String connectType = res.getString(5);
            String purpose = res.getString(6);
            String inverseName = res.getString(7);
            response.append(DTSXMLFactory.createAssociationTypeXML(name, id, code, Integer.parseInt(namespaceId), connectType.charAt(0), purpose.charAt(0), inverseName));
        }
        res.close();
        return gid;
    }

    /**
  * Retrieve all Association Types info from db.
  *
  * @param root a Xml DOM Element holding info indicating the type of query.
  *
  * @return a String of Xml holding data about the retrieved Association Types.
  *
  * @since DTS 3.0
  */
    public String getAssociationTypes(Element root) throws SQLException, XMLException {
        String mode = root.getAttribute(MODE);
        String statement = getDAO().getStatement(TABLE_KEY, "GET_" + mode + "_ASSOCIATION_TYPE");
        ResultSet rs = keepAliveStmt.executeQuery(statement);
        boolean hasResult = false;
        StringBuffer result = new StringBuffer(200);
        appendDtd(result, com.apelon.dts.dtd.common.DTD.ASSOCIATIONTYPE, ASSOCIATION_TYPES);
        XML.asStartTag(result, ASSOCIATION_TYPES);
        hasResult = getAssociationTypes(rs, result);
        rs.close();
        if (!hasResult) {
            return nullDocument(com.apelon.dts.dtd.result.DTD.COMMON_RESULT);
        }
        XML.asEndTag(result, ASSOCIATION_TYPES);
        return result.toString();
    }

    /**
  * Get all Association Types info from db in a given namespace.
  *
  * @param root a Xml DOM Element holding info indicating the type of query.
  *
  * @return a String of Xml holding data about the retrieved Association Types.
  *
  * @since DTS 3.0
  */
    public String getAssociationTypesInNamespace(Element root) throws SQLException, XMLException {
        String mode = root.getAttribute(MODE);
        String namespaceId = root.getAttribute(NAMESPACE_ID);
        String statement = getDAO().getStatement(TABLE_KEY, "GET_" + mode + "_ASSOCIATION_TYPE");
        statement += getDAO().getStatement(TABLE_KEY, "SET_NAMESPACE");
        statement += (" '" + namespaceId + "' ");
        ResultSet rs = keepAliveStmt.executeQuery(statement);
        boolean hasResult = false;
        StringBuffer result = new StringBuffer(200);
        appendDtd(result, com.apelon.dts.dtd.common.DTD.ASSOCIATIONTYPE, ASSOCIATION_TYPES);
        XML.asStartTag(result, ASSOCIATION_TYPES);
        hasResult = getAssociationTypes(rs, result);
        rs.close();
        if (!hasResult) {
            return nullDocument(com.apelon.dts.dtd.result.DTD.COMMON_RESULT);
        }
        XML.asEndTag(result, ASSOCIATION_TYPES);
        return result.toString();
    }

    /**
  * Retrieve all Synonym Types info from db.
  *
  * @param root a Xml DOM Element holding info indicating the type of query.
  *
  * @return a String of Xml holding data about the retrieved Association Types.
  *
  * @since DTS 3.0
  */
    public String getSynonymTypes(Element root) throws SQLException {
        String mode = root.getAttribute(MODE);
        String value = root.getAttribute(VALUE);
        String statement = getDAO().getStatement(TABLE_KEY, "GET_SYNONYMTYPES_" + mode);
        ResultSet rs = keepAliveStmt.executeQuery(statement + value);
        StringBuffer result = new StringBuffer(200);
        appendDtd(result, com.apelon.dts.dtd.common.DTD.ASSOCIATIONTYPE, ASSOCIATION_TYPES);
        XML.asStartTag(result, ASSOCIATION_TYPES);
        boolean hasResult = getAssociationTypes(rs, result);
        rs.close();
        if (!hasResult) {
            return nullDocument(com.apelon.dts.dtd.result.DTD.COMMON_RESULT);
        }
        XML.asEndTag(result, ASSOCIATION_TYPES);
        return result.toString();
    }

    static boolean getAssociationTypes(ResultSet rs, StringBuffer result) throws SQLException {
        boolean hasResult = false;
        while (rs.next()) {
            int id = rs.getInt(1);
            String code = rs.getString(2);
            String name = rs.getString(3);
            int namespaceId = rs.getInt(4);
            String connectType = rs.getString(5);
            String purpose = rs.getString(6);
            String inverseName = rs.getString(7);
            hasResult = true;
            result.append(DTSXMLFactory.createAssociationTypeXML(name, id, code, namespaceId, connectType.charAt(0), purpose.charAt(0), inverseName));
        }
        return hasResult;
    }

    static void getAssociationTypeByGID(Statement stmt, long gid, StringBuffer response, GeneralDAO dao) throws SQLException {
        String statement = dao.getStatement(TABLE_KEY, "GET_TYPE_BY_GID");
        ResultSet res = stmt.executeQuery(statement + gid);
        while (res.next()) {
            gid = res.getLong(1);
            int id = res.getInt(2);
            String code = res.getString(3);
            String name = res.getString(4);
            int namespaceId = res.getInt(5);
            String connectType = res.getString(6);
            String purpose = res.getString(7);
            String inverseName = res.getString(8);
            response.append(DTSXMLFactory.createAssociationTypeXML(name, id, code, namespaceId, connectType.charAt(0), purpose.charAt(0), inverseName));
        }
        res.close();
    }

    /**
  * Add or delete a Concept, Term or Synonym Association.
  *
  * @param root a Xml DOM Element holding info indicating the type of operation.
  * @param permit a DTSPermission object used to validate the user.
  *
  * @return a String of Xml holding data about the processed Associations.
  *
  * @since DTS 3.0
  */
    public String processAssociation(Element root, DTSPermission permit) throws SQLException, XMLException, PermissionException {
        String operation = root.getAttribute(OPERATION);
        NodeList children = root.getChildNodes();
        int len = children.getLength();
        Node node = children.item(0);
        Entry entry = getEntry(node, permit);
        if (!entry.hasResult()) {
            return getFalseResult();
        }
        if (entry.mode.equals(SYNONYM) && operation.equals("ADD")) {
            return addSynonym(entry);
        }
        if (!entry.mode.equals(SYNONYM) && operation.equals("ADD")) {
            return addConceptTermAssociation(entry, children);
        }
        if (!entry.mode.equals(SYNONYM) && operation.equals("DELETE")) {
            return deleteConceptTermAssociation(entry);
        }
        String statement = getDAO().getStatement(TABLE_KEY, operation + "_" + entry.mode + "_ASSOCIATION");
        statement = getDAO().getStatement(statement, 1, String.valueOf(entry.fromGID));
        statement = getDAO().getStatement(statement, 2, String.valueOf(entry.associationGID));
        statement = getDAO().getStatement(statement, 3, String.valueOf(entry.toGID));
        statement = getDAO().getStatement(statement, 4, "'" + String.valueOf(entry.preferredFlag) + "'");
        WFPlugin wf = entry.getWF(this);
        char attrType = (entry.mode.equals(SYNONYM)) ? WFPlugin.ATTR_SYNONYM : WFPlugin.ATTR_ASSOCIATION;
        char editType = WFPlugin.EDIT_ADD;
        if (operation.equals("UPDATE")) {
            editType = WFPlugin.EDIT_UPDATE;
        } else if (operation.equals("DELETE")) {
            editType = WFPlugin.EDIT_DELETE;
        }
        conn.setAutoCommit(false);
        int defaultLevel = conn.getTransactionIsolation();
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        int result = -1;
        try {
            result = keepAliveStmt.executeUpdate(statement);
            if (result != 1) {
                conn.rollback();
                return getFalseResult();
            }
            wf.update(entry.fromId, entry.fromNamespaceId, permit, attrType, editType);
            conn.commit();
            return getTrueResult();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setTransactionIsolation(defaultLevel);
            conn.setAutoCommit(true);
        }
    }

    private String addConceptTermAssociation(Entry entry, NodeList children) throws SQLException {
        WFPlugin wf = entry.getWF(this);
        conn.setAutoCommit(false);
        int defaultLevel = conn.getTransactionIsolation();
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        int result = -1;
        try {
            boolean success = addCTAssociation(entry, children, 1);
            if (!success) {
                conn.rollback();
                return getFalseResult();
            }
            wf.update(entry.fromId, entry.fromNamespaceId, entry.permit, WFPlugin.ATTR_ASSOCIATION, WFPlugin.EDIT_ADD);
            wf.update(entry.toId, entry.toNamespaceId, entry.permit, WFPlugin.ATTR_ASSOCIATION, WFPlugin.EDIT_ADD);
            conn.commit();
            return getTrueResult();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setTransactionIsolation(defaultLevel);
            conn.setAutoCommit(true);
        }
    }

    private boolean addCTAssociation(Entry entry, NodeList children, int index) throws SQLException {
        String sequenceName = getDAO().getStatement(TABLE_KEY, "GET_NEXT_" + entry.mode + "_ASSOCIATION");
        int assocTypeNamespaceId = entry.assocTypeNamespaceId;
        long instanceId = this.getInstanceId(sequenceName, assocTypeNamespaceId);
        String statement = getDAO().getStatement(TABLE_KEY, "ADD_" + entry.mode + "_ASSOCIATION");
        statement = getDAO().getStatement(statement, 1, String.valueOf(entry.fromGID));
        statement = getDAO().getStatement(statement, 2, String.valueOf(entry.associationGID));
        statement = getDAO().getStatement(statement, 3, String.valueOf(entry.toGID));
        statement = getDAO().getStatement(statement, 4, "'" + instanceId + "'");
        int result = -1;
        result = keepAliveStmt.executeUpdate(statement);
        if (result != 1) {
            return false;
        }
        statement = getDAO().getStatement(TABLE_KEY, "ADD_" + entry.mode + "_ASSOCIATION_QUALIFIER");
        sequenceName = getDAO().getStatement(TABLE_KEY, "GET_SEQUENCE_" + entry.mode + "_ASSOCIATION_QUALIFIER");
        boolean success = addQualifiers(statement, children, index, instanceId, sequenceName);
        return success;
    }

    private String deleteConceptTermAssociation(Entry entry) throws SQLException {
        conn.setAutoCommit(false);
        int defaultLevel = conn.getTransactionIsolation();
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        WFPlugin wf = entry.getWF(this);
        int result = -1;
        try {
            boolean success = deleteCTAssociation(entry);
            if (!success) {
                conn.rollback();
                return getFalseResult();
            }
            wf.update(entry.fromId, entry.fromNamespaceId, entry.permit, WFPlugin.ATTR_ASSOCIATION, WFPlugin.EDIT_DELETE);
            wf.update(entry.toId, entry.toNamespaceId, entry.permit, WFPlugin.ATTR_ASSOCIATION, WFPlugin.EDIT_DELETE);
            conn.commit();
            return getTrueResult();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setTransactionIsolation(defaultLevel);
            conn.setAutoCommit(true);
        }
    }

    private boolean deleteCTAssociation(Entry entry) throws SQLException {
        int result = 0;
        String statement = getDAO().getStatement(TABLE_KEY, "DELETE_" + entry.mode + "_ASSOCIATION");
        statement = getDAO().getStatement(statement, 1, String.valueOf(entry.fromGID));
        statement = getDAO().getStatement(statement, 2, String.valueOf(entry.associationGID));
        statement += entry.toGID;
        result = keepAliveStmt.executeUpdate(statement);
        if (result == 0) {
            return false;
        }
        return true;
    }

    private String addSynonym(Entry entry) throws SQLException {
        conn.setAutoCommit(false);
        int defaultLevel = conn.getTransactionIsolation();
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        int result = 0;
        String statement = "";
        try {
            if (entry.preferredFlag.equals("T")) {
                statement = getDAO().getStatement(TABLE_KEY, "SET_PREFERRED_TERM_FALSE2");
                statement = getDAO().getStatement(statement, 1, String.valueOf(entry.fromGID));
                statement += entry.associationGID;
                result = keepAliveStmt.executeUpdate(statement);
            }
            statement = getDAO().getStatement(TABLE_KEY, "ADD_SYNONYM_ASSOCIATION");
            statement = getDAO().getStatement(statement, 1, String.valueOf(entry.fromGID));
            statement = getDAO().getStatement(statement, 2, String.valueOf(entry.associationGID));
            statement = getDAO().getStatement(statement, 3, String.valueOf(entry.toGID));
            statement = getDAO().getStatement(statement, 4, "'" + String.valueOf(entry.preferredFlag) + "'");
            result = keepAliveStmt.executeUpdate(statement);
            if (result == 0) {
                conn.rollback();
                return getFalseResult();
            }
            WFPlugin wf = this.getConceptWF(entry.permit);
            wf.update(entry.fromId, entry.fromNamespaceId, entry.permit, WFPlugin.ATTR_SYNONYM, WFPlugin.EDIT_ADD);
            conn.commit();
            return getTrueResult();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setTransactionIsolation(defaultLevel);
            conn.setAutoCommit(true);
        }
    }

    private String updateSynonym(Entry oldEntry, Entry newEntry) throws SQLException {
        conn.setAutoCommit(false);
        int defaultLevel = conn.getTransactionIsolation();
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        int result = 0;
        String statement = "";
        try {
            if (newEntry.preferredFlag.equals("T")) {
                statement = getDAO().getStatement(TABLE_KEY, "SET_PREFERRED_TERM_FALSE2");
                statement = getDAO().getStatement(statement, 1, String.valueOf(newEntry.fromGID));
                statement += newEntry.associationGID;
                result = keepAliveStmt.executeUpdate(statement);
            }
            statement = getDAO().getStatement(TABLE_KEY, "UPDATE_" + oldEntry.mode + "_ASSOCIATION");
            statement = getDAO().getStatement(statement, 1, String.valueOf(newEntry.fromGID));
            statement = getDAO().getStatement(statement, 2, String.valueOf(newEntry.associationGID));
            statement = getDAO().getStatement(statement, 3, String.valueOf(newEntry.toGID));
            statement = getDAO().getStatement(statement, 4, "'" + String.valueOf(newEntry.preferredFlag) + "'");
            statement = getDAO().getStatement(statement, 5, String.valueOf(oldEntry.fromGID));
            statement = getDAO().getStatement(statement, 6, String.valueOf(oldEntry.associationGID));
            statement += oldEntry.toGID;
            result = keepAliveStmt.executeUpdate(statement);
            String response = null;
            if (result != 1) {
                conn.rollback();
                return getFalseResult();
            }
            response = getTrueResult();
            WFPlugin wf = this.getConceptWF(oldEntry.permit);
            wf.update(oldEntry.fromId, oldEntry.fromNamespaceId, oldEntry.permit, WFPlugin.ATTR_SYNONYM, WFPlugin.EDIT_UPDATE);
            conn.commit();
            return response;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setTransactionIsolation(defaultLevel);
            conn.setAutoCommit(true);
        }
    }

    /**
  * Update a Concept, Term or Synonym Association by replacing previous with new Association.
  *
  * @param root a Xml DOM Element holding info indicating the type of operation.
  * @param permit a DTSPermission object used to validate the user.
  *
  * @return a String of Xml holding data about the processed Association.
  *
  * @since DTS 3.0
  */
    public String updateAssociation(Element root, DTSPermission permit) throws SQLException, XMLException, PermissionException {
        String operation = root.getAttribute(OPERATION);
        NodeList children = root.getChildNodes();
        int len = children.getLength();
        Node node = children.item(0);
        Entry oldEntry = getEntry(node, permit);
        if (!oldEntry.hasResult()) {
            return getFalseResult();
        }
        node = children.item(1);
        Entry newEntry = getEntry(node, permit);
        if (!newEntry.hasResult()) {
            return getFalseResult();
        }
        if (oldEntry.mode.equals("SYNONYM")) {
            return updateSynonym(oldEntry, newEntry);
        }
        return updateConceptTermAssociation(root, oldEntry, newEntry);
    }

    /**
   * update concepter/term association without transaction
   * @param node
   * @param oldEntry
   * @param newEntry
   * @return
   * @throws SQLException
   */
    private boolean updateConceptTermAssociationNT(Node node, Entry oldEntry, Entry newEntry) throws SQLException {
        boolean success = deleteCTAssociation(oldEntry);
        if (!success) {
            return success;
        }
        return addCTAssociation(newEntry, node.getChildNodes(), 2);
    }

    /**
   * if term_association and concept_association needs to be updated
   * delete old one and add new one. if existing one needs to be updated,
   * then existing qualifiers needs to be update, which will be more expensive.
   * Since concept association or termassociation is deleted, attached qualifiers
   * will be deleted by the database
   *
   * @param node new association which contains qualifiers
   * @param oldEntry old association
   * @param newEntry new association
   * @throws SQLException
   * @return XML result string which contains true or false
   */
    private String updateConceptTermAssociation(Node node, Entry oldEntry, Entry newEntry) throws SQLException {
        conn.setAutoCommit(false);
        int defaultLevel = conn.getTransactionIsolation();
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        try {
            boolean success = false;
            if ((oldEntry.associationGID == newEntry.associationGID) && (oldEntry.fromGID == newEntry.fromGID) && (oldEntry.toGID == newEntry.toGID)) {
                NodeList qualifierChildren = node.getChildNodes();
                long instanceID = this.getInstanceId(oldEntry.fromGID, oldEntry.associationGID, oldEntry.toGID, oldEntry.mode + "_ASSOCIATION");
                success = updateQualifiers(TABLE_KEY, instanceID, oldEntry.mode + "_ASSOCIATION", qualifierChildren);
            } else {
                success = updateConceptTermAssociationNT(node, oldEntry, newEntry);
            }
            if (!success) {
                conn.rollback();
                return getFalseResult();
            }
            WFPlugin wf = oldEntry.getWF(this);
            wf.update(oldEntry.fromId, oldEntry.fromNamespaceId, oldEntry.permit, WFPlugin.ATTR_ASSOCIATION, WFPlugin.EDIT_UPDATE);
            conn.commit();
            return getTrueResult();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } catch (Exception e) {
            conn.rollback();
            throw new SQLException("unable to update association " + e.getMessage());
        } finally {
            conn.setTransactionIsolation(defaultLevel);
            conn.setAutoCommit(true);
        }
    }

    /**
  * Find a Concept or Term Association in the database based on name, code or id.
  *
  * @param root a Xml DOM Element holding data about the Association to find.
  *
  * @return a String of Xml holding data about the retrieved Association.
  *
  * @since DTS 3.0
  */
    public String findAssociation(Element root) throws SQLException, XMLException {
        String mode = root.getAttribute(MODE);
        boolean isConceptMode = false;
        String type = "termAssociation";
        if (mode.equals("CONCEPT")) {
            isConceptMode = true;
            type = "conceptAssociation";
        }
        String column = root.getAttribute(COLUMN).toUpperCase();
        String source = root.getAttribute(SOURCE);
        String sourceNamespaceId = root.getAttribute(SOURCE_NAMESPACEID);
        String assoication = root.getAttribute(ASSOCIATION);
        String assocNamespaceIdStr = root.getAttribute(ASSOC_NAMESPACEID);
        String target = root.getAttribute(TARGET);
        String targetNamespaceId = root.getAttribute(TARGET_NAMESPACEID);
        String key = "FIND_" + mode + "_ASSOCIATION_BY_" + column;
        PreparedStatement pStmt = getStmt(key);
        pStmt.setString(1, source);
        pStmt.setInt(2, Integer.parseInt(sourceNamespaceId));
        pStmt.setString(3, assoication);
        pStmt.setInt(4, Integer.parseInt(assocNamespaceIdStr));
        pStmt.setString(5, target);
        pStmt.setInt(6, Integer.parseInt(targetNamespaceId));
        ResultSet rs = pStmt.executeQuery();
        boolean hasResult = false;
        StringBuffer response = new StringBuffer(200);
        appendDtd(response, com.apelon.dts.dtd.common.DTD.ASSOCIATION, type);
        while (rs.next()) {
            hasResult = true;
            XML.asStartTag(response, type);
            int fromId = rs.getInt(1);
            String fromCode = rs.getString(2);
            String fromName = rs.getString(3);
            int fromNamespaceId = rs.getInt(4);
            if (mode.equals("CONCEPT")) {
                response.append(DTSXMLFactory.createDTSConceptXML(fromName, fromId, fromCode, fromNamespaceId));
            } else {
                response.append(DTSXMLFactory.createTermXML(fromName, fromId, fromCode, fromNamespaceId));
            }
            int assocTypeId = rs.getInt(5);
            String assocTypeCode = rs.getString(6);
            String assocTypeName = rs.getString(7);
            int assocNamespaceId = rs.getInt(8);
            String connect = rs.getString(9);
            String purpose = rs.getString(10);
            String inverseName = rs.getString(11);
            response.append(DTSXMLFactory.createAssociationTypeXML(assocTypeName, assocTypeId, assocTypeCode, assocNamespaceId, connect.charAt(0), purpose.charAt(0), inverseName));
            int toId = rs.getInt(12);
            String toCode = rs.getString(13);
            String toName = rs.getString(14);
            int toNamespaceId = rs.getInt(15);
            long associationIID = rs.getLong(16);
            if (mode.equals("CONCEPT")) {
                response.append(DTSXMLFactory.createDTSConceptXML(toName, toId, toCode, toNamespaceId));
            } else {
                response.append(DTSXMLFactory.createTermXML(toName, toId, toCode, toNamespaceId));
            }
            buildAssociationQualifiers(qualifierStmt, associationIID, Integer.parseInt(sourceNamespaceId), response, mode);
            XML.asEndTag(response, type);
        }
        rs.close();
        if (!hasResult) {
            return nullDocument(com.apelon.dts.dtd.result.DTD.COMMON_RESULT);
        }
        return response.toString();
    }

    /**
  * Set a Synonym to be a preferred term or not.
  *
  * @param root a Xml DOM Element holding info indicating how to set preferred term.
  * @param permit a DTSPermission object used to validate the user.
  *
  * @return a String of Xml holding data about the processed Associations.
  *
  * @since DTS 3.0
  */
    public String setPreferredTerm(Element root, DTSPermission permit) throws SQLException, XMLException, PermissionException {
        NodeList children = root.getChildNodes();
        int len = children.getLength();
        Node node = children.item(0);
        Entry entry = getEntry(node, permit);
        String statement = getDAO().getStatement(TABLE_KEY, "SET_PREFERRED_TERM_FALSE");
        statement += entry.fromGID;
        int result = keepAliveStmt.executeUpdate(statement);
        statement = getDAO().getStatement(TABLE_KEY, "SET_PREFERRED_TERM_TRUE");
        statement = getDAO().getStatement(statement, 1, String.valueOf(entry.fromGID));
        statement = getDAO().getStatement(statement, 2, String.valueOf(entry.associationGID));
        statement += entry.toGID;
        result = keepAliveStmt.executeUpdate(statement);
        String response = null;
        if (result != 1) {
            response = getFalseResult();
        } else {
            response = getTrueResult();
        }
        return response;
    }

    private Entry getEntry(Node node, DTSPermission permit) throws SQLException, PermissionException {
        Entry entry = new Entry();
        entry.permit = permit;
        String fromMode = getAttribute(node, FROM_MODE);
        String fromNamespaceId = getAttribute(node, FROM_NAMESPACE_ID);
        entry.fromNamespaceId = Integer.parseInt(fromNamespaceId);
        String fromId = getAttribute(node, FROM_ID);
        entry.fromId = Integer.parseInt(fromId);
        String associationId = getAttribute(node, ASSOCIATION_ID);
        String assocNamespaceId = getAttribute(node, ASSOCIATION_NAMESPACE_ID);
        entry.assocTypeNamespaceId = Integer.parseInt(assocNamespaceId);
        String toMode = getAttribute(node, TO_MODE);
        String toNamespaceId = getAttribute(node, TO_NAMESPACE_ID);
        entry.toNamespaceId = Integer.parseInt(toNamespaceId);
        String toId = getAttribute(node, TO_ID);
        entry.toId = Integer.parseInt(toId);
        checkPermission(permit, assocNamespaceId);
        entry.fromGID = getGID(Integer.parseInt(fromNamespaceId), Integer.parseInt(fromId));
        entry.toGID = getGID(Integer.parseInt(toNamespaceId), Integer.parseInt(toId));
        entry.associationGID = getGID(Integer.parseInt(assocNamespaceId), Integer.parseInt(associationId));
        if (!fromMode.equals(toMode)) {
            entry.mode = "SYNONYM";
            entry.preferredFlag = getAttribute(node, "preferredFlag");
        } else {
            entry.mode = fromMode;
        }
        return entry;
    }

    private long getInstanceId(long fromGID, long associationGID, long toGID, String mode) throws SQLException {
        String statement = getDAO().getStatement(TABLE_KEY, "GET_" + mode + "_INSTANCE_ID");
        statement = getDAO().getStatement(statement, 1, String.valueOf(fromGID));
        statement = getDAO().getStatement(statement, 2, String.valueOf(associationGID));
        statement += toGID;
        ResultSet rs = null;
        try {
            rs = this.keepAliveStmt.executeQuery(statement);
            rs.next();
            long instanceID = rs.getLong(1);
            return instanceID;
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
    }

    private ConceptEntry getConceptEntry(String mode, String column, String value, String namespaceId) throws SQLException {
        ConceptEntry ce = new ConceptEntry();
        String statement = getDAO().getStatement(TABLE_KEY, "FIND_" + mode + "_BY_" + column);
        statement = getDAO().getStatement(statement, 1, "'" + value + "'");
        statement += namespaceId;
        ResultSet rs = keepAliveStmt.executeQuery(statement);
        while (rs.next()) {
            ce.gid = rs.getLong(1);
            ce.id = rs.getInt(2);
            ce.name = rs.getString(3);
            ce.code = rs.getString(4);
            ce.hasResult = true;
        }
        rs.close();
        return ce;
    }

    public static String getAttribute(Node node, String name) {
        return DTSXMLFactory.getAttribute(node, name);
    }

    private static class Entry {

        long associationGID;

        long toGID;

        long fromGID;

        int assocTypeNamespaceId;

        int toId;

        int toNamespaceId;

        int fromId;

        int fromNamespaceId;

        String mode;

        String preferredFlag = "";

        private WFPlugin wf;

        private DTSPermission permit;

        boolean hasResult() {
            return ((fromGID != NO_RESULT) && (associationGID != NO_RESULT) && (toGID != NO_RESULT));
        }

        private WFPlugin getWF(QualifierTypeDb db) {
            if (mode.equals("TERM")) {
                return db.getTermWF(permit);
            }
            return db.getConceptWF(permit);
        }
    }

    private static class ConceptEntry {

        long gid;

        String name;

        int id;

        String code;

        boolean hasResult;
    }

    /**
   * Simply add "A" in front of the id.
   */
    protected String generateCode(int id) {
        return "A" + id;
    }
}
