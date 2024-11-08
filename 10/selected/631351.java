package com.apelon.dts.db;

import com.apelon.common.util.Timer;
import com.apelon.common.xml.XML;
import com.apelon.common.xml.XMLException;
import com.apelon.dts.common.DTSDataLimits;
import com.apelon.dts.common.DTSValidationException;
import com.apelon.dts.db.modules.WFPlugin;
import com.apelon.dts.server.DTSPermission;
import com.apelon.dts.server.PermissionException;
import com.apelon.dts.util.DTSUtil;
import com.apelon.dts.util.DTSXMLFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles addition, deletion and updates to Terms and their Properties.
 *
 * @since DTS 3.0
 *
 * Copyright (c) 2006 Apelon, Inc. All rights reserved.
 */
public class TermDb extends PropertyTypeDb {

    static final String TABLE_KEY = "TERM_DB";

    private static final String COLUMN = "column";

    private static final String VALUE = "value";

    private static final String NAME = "name";

    private static final String ID = "id";

    private static final String CODE = "code";

    private static final String NAMESPACE_ID = "namespaceId";

    private static final String PROPERTY_NAME = "name";

    private static final String PROPERTY_VALUE = "value";

    private static final String MODE = "mode";

    private static final String MATCH_LIMIT = "matchLimit";

    private static final boolean PERFORMANCE_DEBUG = false;

    private static final int ALL_NAMESPACES = -1;

    private PreparedStatement insertSt = null;

    private PreparedStatement updateSt = null;

    private Timer timer;

    /**
  * Set the database connection in {@linkplain  BasicDb BasicDb}
  * and initiate by storing and preparing SQL statements.
  *
  * @param conn a java.sql.Connection for access to DTS schema.
  *
  * @since DTS 3.0
  */
    public TermDb(Connection conn) throws SQLException {
        this(conn, true);
    }

    /**
  * Set the database connection in {@linkplain  BasicDb BasicDb}
  * and an option to initiate by storing and preparing SQL statements.
  *
  * @param conn java.sql.Connection object to access match tables.
  * @param doesInit true to initiate by storing and preparing SQL statements.
  *
  * @since DTS 3.0
  */
    public TermDb(Connection conn, boolean doesInit) throws SQLException {
        super(conn);
        if (PERFORMANCE_DEBUG) {
            timer = new Timer();
        }
        if (doesInit) {
            init();
        }
    }

    private void init() throws SQLException {
        String statement = getDAO().getStatement(TABLE_KEY, "INSERT");
        insertSt = conn.prepareStatement(statement);
        statement = getDAO().getStatement(TABLE_KEY, "UPDATE");
        updateSt = conn.prepareStatement(statement);
    }

    public void close() throws SQLException {
        super.close();
        this.closeStatements(new Statement[] { insertSt, updateSt });
    }

    /**
  * Get term info from the Xml Element, verify user, then get,
  * populate and execute SQL statement that creates the term
  * in the database.
  *
  * @param root a Xml DOM Element holding data about the term to add.
  * @param permit a DTSPermission object used to validate the user.
  *
  * @return a String of Xml holding data about the added term.
  *
  * @since DTS 3.0
  */
    public String addTerm(Element root, DTSPermission permit) throws SQLException, XMLException, PermissionException {
        NodeList children = root.getChildNodes();
        int len = children.getLength();
        Node node = children.item(0);
        String name = getAttribute(node, NAME);
        String id = getAttribute(node, ID);
        String code = getAttribute(node, CODE);
        String namespaceId = getAttribute(node, NAMESPACE_ID);
        checkPermission(permit, namespaceId);
        int namespaceIdInt = Integer.parseInt(namespaceId);
        int idInt = Integer.parseInt(id);
        if (idInt == -1) {
            idInt = getItemSeq("T", namespaceIdInt);
        }
        if ((code == null) || code.equals("") || code.equals("null")) {
            code = generateCode(idInt);
        }
        name = DTSUtil.checkValue("Term Name", name, DTSDataLimits.LEN_NAME_TERM);
        code = DTSUtil.checkValue("Term Code", code, DTSDataLimits.LEN_CODE);
        insertSt.setLong(1, getGID(namespaceIdInt, idInt));
        insertSt.setInt(2, idInt);
        insertSt.setString(3, code);
        insertSt.setString(4, name);
        insertSt.setInt(5, namespaceIdInt);
        insertSt.setString(6, name);
        insertSt.setNull(7, Types.INTEGER);
        boolean success = doTransaction(null, insertSt, idInt, namespaceIdInt, permit, WFPlugin.ATTR_ITEM, WFPlugin.EDIT_ADD);
        return findTerm("ID", Integer.toString(idInt), Integer.toString(namespaceIdInt), null);
    }

    protected String generateCode(int id) {
        return "T" + id;
    }

    /**
  * Extract term info from Xml then verify user and execute the SQL
  * that deletes the term in the database.
  *
  * @param root a Xml DOM Element holding data about the term to delete.
  * @param permit a DTSPermission object used to validate the user.
  *
  * @return a String of Xml holding data about the deleted term.
  *
  * @since DTS 3.0
  */
    public String deleteTerm(Element root, DTSPermission permit) throws SQLException, XMLException, PermissionException {
        String column = root.getAttribute(COLUMN);
        String value = root.getAttribute(VALUE);
        String namespaceId = root.getAttribute(NAMESPACE_ID);
        checkPermission(permit, namespaceId);
        return deleteTerm(column, value, namespaceId, permit);
    }

    /**
  * Get term info from the Xml Element, verify user, then get,
  * populate and execute SQL statement that updates the term
  * in the database.
  *
  * @param root a Xml DOM Element holding data about the term to update.
  * @param permit a DTSPermission object used to validate the user.
  *
  * @return a String of Xml holding data about the updated term.
  *
  * @since DTS 3.0
  */
    public String updateTerm(Element root, DTSPermission permit) throws SQLException, XMLException, PermissionException, DTSValidationException {
        String id = root.getAttribute(ID);
        String namespaceId = root.getAttribute(NAMESPACE_ID);
        checkPermission(permit, namespaceId);
        NodeList children = root.getChildNodes();
        int len = children.getLength();
        Node node = children.item(0);
        String newName = getAttribute(node, NAME);
        String newIdStr = getAttribute(node, ID);
        String newCode = getAttribute(node, CODE);
        String newNamespaceIdStr = getAttribute(node, NAMESPACE_ID);
        checkPermission(permit, newNamespaceIdStr);
        int oldId = Integer.parseInt(id);
        int oldNamespaceId = Integer.parseInt(namespaceId);
        int newNamespaceId = Integer.parseInt(newNamespaceIdStr);
        int newId = Integer.parseInt(newIdStr);
        assertUpdate(newId == oldId, "old Id: " + oldId + " should be same as new id: " + newId);
        assertUpdate(newNamespaceId == oldNamespaceId, "old namespaceId: " + oldNamespaceId + " should be same as new namespaceId: " + newNamespaceId);
        newName = DTSUtil.checkValue("Term Name", newName, DTSDataLimits.LEN_NAME_TERM);
        newCode = DTSUtil.checkValue("Term Code", newCode, DTSDataLimits.LEN_CODE);
        updateSt.setString(1, newName);
        updateSt.setString(2, newCode);
        updateSt.setString(3, newName);
        updateSt.setInt(4, oldId);
        updateSt.setInt(5, oldNamespaceId);
        boolean success = doTransaction(null, updateSt, newId, newNamespaceId, permit, WFPlugin.ATTR_ITEM, WFPlugin.EDIT_UPDATE);
        if (!success) {
            boolean termExists = termExists(ID, id, namespaceId);
            if (!termExists) {
                throw new DTSValidationException("Unable to update term: id=" + id + ", namespaceId=" + namespaceId + ". The term doesn't exist in knowledgebase.");
            }
        }
        return findTerm("ID", newIdStr, newNamespaceIdStr, null);
    }

    /**
  * Get <code>Term</code> objects with the specified name and namespace ID.
  * Since a term name may not be unique within a namespace, it's possible to
  * have more than one term returned.
  *
  * @param root a Xml DOM Element holding data about the term(s) to retrieve.
  *
  * @return a String of Xml holding data about fetched term(s).
  *
  * @since DTS 3.0
  */
    public String findTerm(Element root) throws SQLException, XMLException {
        String column = root.getAttribute(COLUMN);
        String value = root.getAttribute(VALUE);
        String namespaceId = root.getAttribute(NAMESPACE_ID);
        Element asd = (Element) root.getFirstChild();
        return findTerm(column, value, namespaceId, asd);
    }

    /**
  * Call PropertyTypeDb.addProperty(Element root, String mode, DTSPermission permit)
  * in "TERM" mode.
  *
  * @param root a Xml DOM Element holding data about the Property to add.
  * @param permit a DTSPermission object used to validate the user.
  *
  * @return a String of Xml holding data about the added Property.
  *
  * @since DTS 3.0
  */
    public String addProperty(Element root, DTSPermission permit) throws SQLException, XMLException, PermissionException {
        return addProperty(root, "TERM", permit);
    }

    /**
  * Use PropertyTypeDb.deleteProperty(Element root, String mode, DTSPermission permit)
  * in "TERM" mode.
  *
  * @param root a Xml DOM Element holding data about the Property to delete.
  * @param permit a DTSPermission object used to validate the user.
  *
  * @return a String of Xml holding data about the deleted Property.
  *
  * @since DTS 3.0
  */
    public String deleteProperty(Element root, DTSPermission permit) throws SQLException, XMLException, PermissionException {
        return deleteProperty(root, "TERM", permit);
    }

    /**
  * Call PropertyTypeDb.updateProperty(Element root, String mode, DTSPermission permit)
  * in "TERM" mode.
  *
  * @param root a Xml DOM Element holding data about the Property to update.
  * @param permit a DTSPermission object used to validate the user.
  *
  * @return a String of Xml holding data about the updated Property.
  *
  * @since DTS 3.0
  */
    public String updateProperty(Element root, DTSPermission permit) throws SQLException, XMLException, PermissionException {
        return updateProperty(root, "TERM", permit);
    }

    boolean buildTerm(ResultSet rs, int limit, Element propertySet, Element termAssocSet, Element inverseTermAssocSet, Element synonymSet, StringBuffer result) throws SQLException {
        boolean hasResult = false;
        while (rs.next()) {
            long gid = rs.getLong(1);
            int id = rs.getInt(2);
            String code = rs.getString(3);
            String name = rs.getString(4);
            int namespaceId = rs.getInt(5);
            buildTerm(gid, name, id, code, namespaceId, limit, propertySet, termAssocSet, inverseTermAssocSet, synonymSet, result, false);
            hasResult = true;
        }
        return hasResult;
    }

    private void buildTerm(long gid, String name, int id, String code, int namespaceId, int limit, Element propertySet, Element termAssocSet, Element inverseTermAssocSet, Element synonymSet, StringBuffer result, boolean wfplugin) throws SQLException {
        StringBuffer response = new StringBuffer(1000);
        populateProperties(limit, propertySet, gid, namespaceId, response, wfplugin);
        int termAssocCount = populateTermAssociations(limit, termAssocSet, gid, namespaceId, response, "NORMAL", wfplugin);
        int inverseTermAssocCount = populateTermAssociations(limit, inverseTermAssocSet, gid, namespaceId, response, "INVERSE", wfplugin);
        String termRefXML = DTSXMLFactory.createTermXML(name, id, code, namespaceId);
        int synonymCount = populateSynonyms(limit, synonymSet, gid, namespaceId, termRefXML, response, wfplugin);
        XML.asEndTag(response, "term");
        result.append(DTSXMLFactory.createTermXML(name, id, code, namespaceId, termAssocCount, inverseTermAssocCount, synonymCount, false));
        result.append(response);
    }

    /**
  * Generate a new GID and build the Xml for a new term.
  *
  * @param name a String that is the name of the term.
  * @param id an int that is the id of the term.
  * @param code a String that is the code of the term.
  * @param namespaceId a int that is the id of the namespace.
  *
  * @return a String of Xml holding data about the term.
  *
  * @since DTS 3.0
  */
    public String getFullTermInformation(String name, int id, String code, int namespaceId) throws SQLException {
        long gid = getGID(namespaceId, id);
        StringBuffer result = new StringBuffer(11000);
        appendDtd(result, com.apelon.dts.dtd.result.DTD.TERM_RESULT, "term");
        buildTerm(gid, name, id, code, namespaceId, 100000, null, null, null, null, result, true);
        return result.toString();
    }

    private String findTerm(String column, String value, String namespaceId, Element asd) throws SQLException {
        String statement = getDAO().getStatement(TABLE_KEY, "FIND_BY_" + column.toUpperCase());
        statement = getDAO().getStatement(statement, 1, "'" + value + "'");
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(statement + namespaceId);
            String response = findTerms(rs, asd);
            rs.close();
            return response;
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }

    /**
   * given term gid, pouplates the synonym
   * @param propSet
   * @param gid cooncept gid
   * @param response
   */
    private void populateProperties(int limit, Element propSet, long gid, int namespaceId, StringBuffer response) throws SQLException {
        populateProperties(limit, propSet, gid, namespaceId, response, false);
    }

    private void populateProperties(int limit, Element propSet, long gid, int namespaceId, StringBuffer response, boolean wfplugin) throws SQLException {
        if (!wfplugin && propSet == null) {
            return;
        }
        if (PERFORMANCE_DEBUG) {
            timer.start();
        }
        String allAttr = (propSet == null) ? null : propSet.getAttribute("all");
        boolean all_attrs = wfplugin || ((allAttr != null) && allAttr.toString().equals(trueAttribute()));
        String elements = "properties";
        String statement = null;
        if (all_attrs) {
            statement = getDAO().getStatement(PropertyTypeDb.TABLE_KEY, "GET_TERM_PROPERTY_ALL");
            statement = getDAO().getStatement(statement, 1, String.valueOf(gid));
            statement = getDAO().getStatement(statement, 2, String.valueOf(limit));
            buildProperties(limit, statement, namespaceId, response, false);
        } else {
            List termList = new ArrayList();
            getPropertyValueIds(propSet, termList);
            buildProperties(limit, gid, namespaceId, response, termList, "TERM");
        }
        if (PERFORMANCE_DEBUG) {
            timer.stop();
            System.out.println("populating properties: " + timer.read());
        }
    }

    private void getPropertyValueIds(Element asd, List termList) throws SQLException {
        long gids[] = getValueIds(asd);
        for (int i = 0; i < gids.length; i++) {
            Long value = new Long(gids[i]);
            PropertyTypeEntry entry = (PropertyTypeEntry) getPropertyTypeEntry(value);
            if (entry == null) {
                return;
            }
            termList.add(value);
        }
    }

    protected void buildQualifiers(int limit, long propertyIID, int termNamespaceId, StringBuffer response, char valueSize) throws SQLException {
        buildQualifiers(limit, propertyIID, termNamespaceId, response, "TERM");
    }

    private String deleteTerm(String column, String value, String namespaceId, DTSPermission permit) throws SQLException {
        boolean termExists = termExists(column, value, namespaceId);
        if (termExists) {
            String statement = getDAO().getStatement(TABLE_KEY, "DELETE_BY_" + column.toUpperCase());
            statement = getDAO().getStatement(statement, 1, "'" + value + "'");
            boolean success = doTransaction(statement + namespaceId, keepAliveStmt, Integer.parseInt(value), Integer.parseInt(namespaceId), permit, WFPlugin.ATTR_ITEM, WFPlugin.EDIT_DELETE);
            termExists = termExists(column, value, namespaceId);
        }
        String response = null;
        if (termExists) {
            response = getFalseResult();
        } else {
            response = getTrueResult();
        }
        return response;
    }

    private boolean termExists(String column, String value, String namespaceId) throws SQLException {
        boolean found = false;
        ResultSet gids = null;
        String statement = getDAO().getStatement(TABLE_KEY, "FIND_BY_" + column.toUpperCase());
        statement = getDAO().getStatement(statement, 1, "'" + value + "'");
        try {
            gids = keepAliveStmt.executeQuery(statement + namespaceId);
            while (gids.next()) {
                found = true;
                break;
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            if (gids != null) {
                gids.close();
            }
        }
        return found;
    }

    /**
  * Fetch the properties for the given <code>Term</code>.
  *
  * @param root a Xml DOM Element holding data about the term for
  * which to get properties.
  *
  * @return a String of Xml holding data about fetched properties.
  *
  * @since DTS 3.0
  */
    public String fetchProperties(Element root) throws SQLException, XMLException {
        String id = root.getAttribute(ID);
        String namespaceId = root.getAttribute(NAMESPACE_ID);
        long termGID = getGID(Integer.parseInt(namespaceId), Integer.parseInt(id));
        StringBuffer result = new StringBuffer(200);
        appendDtd(result, com.apelon.dts.dtd.common.DTD.PROPERTY, "properties");
        XML.asStartTag(result, "properties");
        String statement = getDAO().getStatement(TABLE_KEY, "FIND_ALL_PROPERTY_GID");
        boolean hasResult = buildProperties(100, statement + termGID, Integer.parseInt(namespaceId), result, false);
        if (!hasResult) {
            return nullDocument(com.apelon.dts.dtd.result.DTD.COMMON_RESULT);
        }
        XML.asEndTag(result, "properties");
        return result.toString();
    }

    /**
  * Fetch all Property Types or those in a given namespace.
  *
  * @param root a Xml DOM Element holding data about Property Types to retrieve.
  *
  * @return a String of Xml holding data about fetched Property Types.
  *
  * @since DTS 3.0
  */
    public String getPropertyType(Element root) throws SQLException, XMLException {
        String mode = root.getAttribute(MODE);
        String value = root.getAttribute(VALUE);
        String statement = getDAO().getStatement(TABLE_KEY, "GET_PROPERTY_TYPE_" + mode);
        ResultSet rs = keepAliveStmt.executeQuery(statement + value);
        StringBuffer result = new StringBuffer(200);
        appendDtd(result, com.apelon.dts.dtd.common.DTD.PROPERTY, "propertyTypes");
        XML.asStartTag(result, "propertyTypes");
        boolean hasResult = false;
        while (rs.next()) {
            String name = rs.getString(1);
            int id = rs.getInt(2);
            String code = rs.getString(3);
            int namespaceId = rs.getInt(4);
            String valueSize = rs.getString(5);
            String attachesTo = rs.getString(6);
            String isWordSearchable = rs.getString(7);
            boolean bValue = true;
            if (isWordSearchable.equals("F")) {
                bValue = false;
            }
            result.append(DTSXMLFactory.createPropertyTypeXML(name, id, code, namespaceId, valueSize.charAt(0), attachesTo.charAt(0), bValue));
            hasResult = true;
        }
        rs.close();
        if (!hasResult) {
            return nullDocument(com.apelon.dts.dtd.result.DTD.COMMON_RESULT);
        }
        XML.asEndTag(result, "propertyTypes");
        return result.toString();
    }

    /**
   * Builds the search string by substituting wild card * with % and escaping special characters
   * @param pattern the search pattern
   * @return the sql search pattern
   */
    private String buildSearchPattern(String pattern) {
        String string_pattern = pattern.toUpperCase();
        if ((string_pattern.indexOf("%") >= 0) || (string_pattern.indexOf("_") >= 0) || (string_pattern.indexOf("\\") >= 0)) {
            string_pattern = sqlEscape(string_pattern, "%_\\");
        }
        string_pattern = getDTSDAO().sqlLeftBracketEscape(string_pattern);
        if (string_pattern.indexOf("*") >= 0) {
            string_pattern = string_pattern.replace('*', '%');
        }
        return string_pattern;
    }

    /**
   * Finds Term by given Name, Property, Association or Inverse Association criteria
   * @param root Search Criteria
   * @return
   * @throws SQLException
   * @throws XMLException
   */
    public String searchTerm(Element root) throws SQLException, XMLException {
        String mode = getAttribute(root, MODE);
        if (mode == null) {
            return findTermsByName(root);
        }
        return findTermsByAttribute(root);
    }

    /**
   * Finds Term by given Name criteria
   * @param root Search Criteria
   * @return
   * @throws SQLException
   * @throws XMLException
   */
    public String findTermsByName(Element root) throws SQLException, XMLException {
        String limit = root.getAttribute(MATCH_LIMIT);
        String namespaceIdStr = root.getAttribute(NAMESPACE_ID);
        int namespaceId = Integer.parseInt(namespaceIdStr);
        String value = root.getAttribute(VALUE);
        NodeList children = root.getChildNodes();
        Element asd = (Element) children.item(0);
        String postfix = "";
        if (namespaceId == ALL_NAMESPACES) {
            postfix = "_ALL";
        }
        String statement = getDAO().getStatement(TABLE_KEY, "SEARCH_TERM" + postfix);
        statement = getDAO().getStatement(statement, 1, limit);
        statement = getDAO().getStatement(statement, 2, buildSearchPattern(value));
        if (namespaceId != ALL_NAMESPACES) {
            statement = getDAO().getStatement(statement, 3, String.valueOf(namespaceId));
        }
        return findTerms(statement, asd);
    }

    /**
   * Finds Term by given Property, Association or Inverse Association criteria
   * @param root Search Criteria
   * @return
   * @throws SQLException
   * @throws XMLException
   */
    public String findTermsByAttribute(Element root) throws SQLException, XMLException {
        NodeList children = root.getChildNodes();
        String limit = root.getAttribute(MATCH_LIMIT);
        String namespaceIdStr = root.getAttribute(NAMESPACE_ID);
        int namespaceId = Integer.parseInt(namespaceIdStr);
        String value = root.getAttribute(VALUE);
        String mode = root.getAttribute(MODE);
        Element asd = (Element) children.item(0);
        Element type = (Element) children.item(1);
        String typeId = type.getAttribute(ID);
        String typeNamespaceId = type.getAttribute(NAMESPACE_ID);
        long gid = this.getGID(Integer.parseInt(typeNamespaceId), Integer.parseInt(typeId));
        String statement = "";
        if (namespaceId == ALL_NAMESPACES) {
            statement = getDAO().getStatement(TABLE_KEY, "SEARCH_BY_" + mode + "_IN_ALL_NS");
        } else {
            statement = getDAO().getStatement(TABLE_KEY, "SEARCH_BY_" + mode);
            statement = getDAO().getStatement(statement, 4, String.valueOf(namespaceId));
        }
        statement = getDAO().getStatement(statement, 1, String.valueOf(limit));
        statement = getDAO().getStatement(statement, 2, buildSearchPattern(value));
        statement = getDAO().getStatement(statement, 3, String.valueOf(gid));
        return findTerms(statement, asd);
    }

    private String findTerms(String statement, Element asd) throws SQLException, XMLException {
        Statement st = null;
        st = conn.createStatement();
        try {
            ResultSet rs = st.executeQuery(statement);
            String response = findTerms(rs, asd);
            rs.close();
            return response;
        } catch (SQLException e) {
            throw e;
        } finally {
            if (st != null) {
                st.close();
            }
        }
    }

    /**
   * given term gid, pouplate term association
   * @param termAssocSet
   * @param gid cooncept gid
   * @param namespaceId subscription namespaceId
   * @param response
   * @param mode normal or inverse
   * @return total number of avaliable termassociation
   */
    private int populateTermAssociations(int limit, Element termAssocSet, long gid, int namespaceId, StringBuffer response, String mode) throws SQLException {
        return populateTermAssociations(limit, termAssocSet, gid, namespaceId, response, mode, false);
    }

    private int populateTermAssociations(int limit, Element termAssocSet, long gid, int namespaceId, StringBuffer response, String mode, boolean wfplugin) throws SQLException {
        if (!wfplugin && termAssocSet == null) {
            return 0;
        }
        if (PERFORMANCE_DEBUG) {
            timer.start();
        }
        String allAttr = (termAssocSet == null) ? null : termAssocSet.getAttribute("all");
        boolean all_attrs = wfplugin || ((allAttr != null) && allAttr.toString().equals(trueAttribute()));
        String element = "termAssociation";
        String attribute = "";
        if (mode.equals("INVERSE")) {
            attribute = " mode= 'INVERSE'";
        }
        String statement = null;
        String queryKey = "";
        String condition = "";
        if (all_attrs) {
            queryKey = "GET_" + mode + "_TERM_ASSOCIATION_ALL";
            statement = getDAO().getStatement(TABLE_KEY, queryKey);
            statement = getDAO().getStatement(statement, 1, String.valueOf(gid));
            statement = getDAO().getStatement(statement, 2, String.valueOf(limit));
        } else {
            long[] termAssocGIDs = getValueIds(termAssocSet);
            if (termAssocGIDs.length == 0) {
                return 0;
            }
            queryKey = "GET_" + mode + "_TERM_ASSOCIATION";
            statement = getDAO().getStatement(TABLE_KEY, queryKey);
            for (int i = 0; i < (termAssocGIDs.length - 1); i++) {
                condition += (termAssocGIDs[i] + "'");
            }
            condition += termAssocGIDs[termAssocGIDs.length - 1];
            statement = getDAO().getStatement(statement, 1, String.valueOf(gid));
            statement = getDAO().getStatement(statement, 2, condition);
            statement = getDAO().getStatement(statement, 3, String.valueOf(limit));
        }
        ResultSet rs = keepAliveStmt.executeQuery(statement);
        int resultCount = 0;
        while (rs.next()) {
            int assocTypeId = rs.getInt(1);
            String assocTypeCode = rs.getString(2);
            String assocTypeName = rs.getString(3);
            int assocNamespaceId = rs.getInt(4);
            String connect = rs.getString(5);
            String purpose = rs.getString(6);
            String inverseName = rs.getString(7);
            boolean isLocalAdd = super.isLocalAddition(namespaceId, assocNamespaceId);
            XML.asStartTag(response, element + attribute + " isLocalAddition ='" + isLocalAdd + "'");
            response.append(DTSXMLFactory.createAssociationTypeXML(assocTypeName, assocTypeId, assocTypeCode, assocNamespaceId, connect.charAt(0), purpose.charAt(0), inverseName));
            int toTermId = rs.getInt(8);
            String toTermCode = rs.getString(9);
            String toTermName = rs.getString(10);
            int toNamespaceId = rs.getInt(11);
            long termAssociationIID = rs.getLong(12);
            response.append(DTSXMLFactory.createTermXML(toTermName, toTermId, toTermCode, toNamespaceId));
            buildAssociationQualifiers(qualifierStmt, termAssociationIID, namespaceId, response, "TERM");
            XML.asEndTag(response, element);
            resultCount++;
        }
        rs.close();
        int assocCount = 0;
        if (resultCount == limit) {
            assocCount = getAvailableCount(TABLE_KEY, queryKey, String.valueOf(gid), condition, all_attrs, resultCount);
        }
        if (PERFORMANCE_DEBUG) {
            timer.stop();
            System.out.println("populating " + mode + " termassociation: " + timer.read());
        }
        return assocCount;
    }

    /**
   * given term gid, pouplates the synonym
   *
   * @param synonymSet
   * @param gid        term gid
   * @param response
   */
    private int populateSynonyms(int limit, Element synonymSet, long gid, int subscriptionNamespaceId, String termRefXML, StringBuffer response, boolean wfplugin) throws SQLException {
        if (!wfplugin && synonymSet == null) {
            return 0;
        }
        if (PERFORMANCE_DEBUG) {
            timer.start();
        }
        String allAttr = (synonymSet == null) ? null : synonymSet.getAttribute("all");
        boolean all_attrs = wfplugin || ((allAttr != null) && allAttr.toString().equals(trueAttribute()));
        String synonym = "synonym";
        String statement = null;
        String queryKey = "";
        String condition = "";
        if (all_attrs) {
            queryKey = "GET_SYNONYM_ALL";
            statement = getDAO().getStatement(TABLE_KEY, queryKey);
            statement = getDAO().getStatement(statement, 1, String.valueOf(gid));
            statement = getDAO().getStatement(statement, 2, String.valueOf(limit));
        } else {
            long[] synonymGIDs = getValueIds(synonymSet);
            if (synonymGIDs.length == 0) {
                return 0;
            }
            queryKey = "GET_SYNONYM";
            statement = getDAO().getStatement(TABLE_KEY, queryKey);
            for (int i = 0; i < (synonymGIDs.length - 1); i++) {
                condition += (synonymGIDs[i] + ",");
            }
            condition += synonymGIDs[synonymGIDs.length - 1];
            statement = getDAO().getStatement(statement, 1, String.valueOf(gid));
            statement = getDAO().getStatement(statement, 2, condition);
            statement = getDAO().getStatement(statement, 3, String.valueOf(limit));
        }
        if (PERFORMANCE_DEBUG) {
            System.out.println(statement);
        }
        ResultSet rs = keepAliveStmt.executeQuery(statement);
        int resultCount = 0;
        while (rs.next()) {
            String flag = rs.getString(1);
            int assocTypeId = rs.getInt(2);
            String assocTypeCode = rs.getString(3);
            String assocTypeName = rs.getString(4);
            int assocNamespaceId = rs.getInt(5);
            String connect = rs.getString(6);
            String purpose = rs.getString(7);
            String inverseName = rs.getString(8);
            int conId = rs.getInt(9);
            String conCode = rs.getString(10);
            String conName = rs.getString(11);
            int conNamespaceId = rs.getInt(12);
            boolean isLocalAdd = isLocalAddition(subscriptionNamespaceId, assocNamespaceId);
            XML.asStartTag(response, synonym + " preferredFlag= '" + flag + "' isLocalAddition= '" + isLocalAdd + "'");
            response.append(DTSXMLFactory.createDTSConceptXML(conName, conId, conCode, conNamespaceId));
            response.append(DTSXMLFactory.createAssociationTypeXML(assocTypeName, assocTypeId, assocTypeCode, assocNamespaceId, connect.charAt(0), purpose.charAt(0), inverseName));
            response.append(termRefXML);
            XML.asEndTag(response, synonym);
            resultCount++;
        }
        rs.close();
        if (PERFORMANCE_DEBUG) {
            timer.stop();
            System.out.println("populating synonym: " + timer.read());
        }
        int synonymCount = 0;
        if (resultCount == limit) {
            synonymCount = getAvailableCount(TABLE_KEY, queryKey, String.valueOf(gid), condition, all_attrs, resultCount);
        }
        return synonymCount;
    }

    private String findTerms(ResultSet rs, Element asd) throws SQLException {
        Element termAssocSet = null;
        Element inverseTermAssocSet = null;
        Element propertySet = null;
        Element synonymSet = null;
        int limit = -1;
        if (asd != null) {
            int length = asd.getChildNodes().getLength();
            limit = Integer.parseInt(asd.getAttribute("limit"));
            for (int i = 0; i < length; i++) {
                Node node = asd.getChildNodes().item(i);
                if (node.getNodeName().equals("propertySet")) {
                    propertySet = (Element) node;
                    continue;
                }
                if (node.getNodeName().equals("termAssociationSet")) {
                    termAssocSet = (Element) node;
                    continue;
                }
                if (node.getNodeName().equals("inverseTermAssocSet")) {
                    inverseTermAssocSet = (Element) node;
                    continue;
                }
                if (node.getNodeName().equals("synonymSet")) {
                    synonymSet = (Element) node;
                    continue;
                }
            }
        }
        StringBuffer result = new StringBuffer(200);
        appendDtd(result, com.apelon.dts.dtd.result.DTD.TERM_RESULT, "terms");
        XML.asStartTag(result, "terms");
        boolean hasResult = buildTerm(rs, limit, propertySet, termAssocSet, inverseTermAssocSet, synonymSet, result);
        if (!hasResult) {
            return nullDocument(com.apelon.dts.dtd.result.DTD.COMMON_RESULT);
        }
        XML.asEndTag(result, "terms");
        return result.toString();
    }

    public static String getAttribute(Node node, String name) {
        return DTSXMLFactory.getAttribute(node, name);
    }

    private boolean doTransaction(String statement, Statement st, int id, int namespaceId, DTSPermission permit, char attrType, char editType) throws SQLException {
        int defaultLevel = conn.getTransactionIsolation();
        conn.setAutoCommit(false);
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        boolean success = false;
        int result = 0;
        try {
            if (editType == WFPlugin.EDIT_DELETE) {
                getTermWF(permit).update(id, namespaceId, permit, attrType, editType);
            }
            if (statement == null) {
                result = ((PreparedStatement) st).executeUpdate();
            } else {
                result = st.executeUpdate(statement);
            }
            success = (result == 1) ? true : false;
            if (!success) {
                conn.rollback();
                return success;
            }
            if (editType != WFPlugin.EDIT_DELETE) {
                getTermWF(permit).update(id, namespaceId, permit, attrType, editType);
            }
            conn.commit();
            return success;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } catch (Exception e) {
            conn.rollback();
            throw new SQLException("unable to update concept: id " + id + " namespaceId: " + namespaceId + " edit mode:" + editType);
        } finally {
            conn.setTransactionIsolation(defaultLevel);
            conn.setAutoCommit(true);
        }
    }
}
