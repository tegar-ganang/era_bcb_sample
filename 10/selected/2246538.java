package com.apelon.dts.db;

import com.apelon.common.xml.XML;
import com.apelon.common.xml.XMLException;
import com.apelon.dts.client.attribute.DTSRoleType;
import com.apelon.dts.client.attribute.Kind;
import com.apelon.dts.common.DTSDataLimits;
import com.apelon.dts.common.DTSValidationException;
import com.apelon.dts.common.DTSTransferObject;
import com.apelon.dts.server.DTSPermission;
import com.apelon.dts.server.PermissionException;
import com.apelon.dts.util.DTSUtil;
import com.apelon.dts.util.DTSXMLFactory;
import com.apelon.beans.apelmsg.ApelMsgHandler;
import org.w3c.dom.Element;
import java.sql.*;
import java.util.ArrayList;

/**
 * Execute Property Type, Role Type and Index queries.
 *
 * @since DTS 3.0
 *
 * Copyright (c) 2007 Apelon, Inc. All rights reserved.
 */
public class ConceptDb extends DTSConceptDb {

    private static final String TABLE_KEY = "CONCEPT_DB";

    private static final String ROLE_TYPE_TABLE_KEY = "ROLE_TYPE_DB";

    /**
  * Constructor that takes a database connection object and simply calls
  * ConceptDb(Connection newConn, boolean init_p) with init_p set to true.
  *
  * @param newConn java.sql.Connection object to access DTS tables.
  */
    public ConceptDb(Connection newConn) throws SQLException {
        this(newConn, true);
    }

    /**
  * Constructor that takes a database connection object and an option
  * to load additional attribute tables.
  *
  * @param newConn java.sql.Connection object to access DTS tables.
  * @param init_p true to load properties, range and role tables
  */
    public ConceptDb(Connection newConn, boolean init_p) throws SQLException {
        super(newConn);
        init();
    }

    /**
  * Close database statements and connection objects.
  */
    public void close() throws SQLException {
        super.close();
    }

    private void init() throws SQLException {
    }

    /**
   * Adds a Role Type to writable Ontylog Extension namespace.
   *
   * @param entry Role Type entry
   * @param permit DTS Permission
   * @return true if Role Type entry is added, else false
   * @throws DTSValidationException If DTS Validation fails
   * @throws SQLException For Db Errors
   * @throws PermissionException If there is no permission to write to Namespace of the Role Type
   *
   * @see com.apelon.dts.client.concept.BaseOntylogConceptQuery#addRoleType(com.apelon.dts.client.attribute.DTSRoleType)
   * @since DTS 3.4.2
   */
    public boolean addRoleType(RoleTypeEntry entry, DTSPermission permit) throws DTSValidationException, SQLException, PermissionException {
        checkPermission(permit, String.valueOf(entry.getNamespaceId()));
        int linkedNamespaceId = getLinkedNamespaceId(entry.namespaceId);
        if (linkedNamespaceId == -1) {
            throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0027", new String[] { String.valueOf(entry.namespaceId) }));
        }
        if (entry.domainKindNamespaceId != entry.namespaceId && entry.domainKindNamespaceId != linkedNamespaceId) {
            throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0028"));
        }
        long domainGid = getGID(entry.domainKindNamespaceId, entry.domainKindId);
        if (entry.rangeKindNamespaceId != entry.namespaceId && entry.rangeKindNamespaceId != linkedNamespaceId) {
            throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0029"));
        }
        long rangeGid = getGID(entry.rangeKindNamespaceId, entry.rangeKindId);
        long rightIdentityGid = 0;
        if (entry.rightIdentityId != 0) {
            if (entry.rightIdentityNamespaceId != entry.namespaceId && entry.rightIdentityNamespaceId != linkedNamespaceId) {
                throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0030"));
            }
            if (entry.id != -1) {
                if (entry.rightIdentityNamespaceId == entry.namespaceId && entry.rightIdentityId == entry.id) {
                    throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0035"));
                }
            }
            rightIdentityGid = getGID(entry.rightIdentityNamespaceId, entry.rightIdentityId);
            DBRoleTypeEntry riEntry = fetchDBRoleTypeEntry(rightIdentityGid);
            if (riEntry == null) {
                throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0039", new String[] { "Right Identity Role Type", String.valueOf(entry.rightIdentityNamespaceId), String.valueOf(entry.rightIdentityId) }));
            }
            if (riEntry.domainGid != riEntry.rangeGid) {
                throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0036"));
            }
            if (riEntry.domainGid != rangeGid) {
                throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0037"));
            }
        }
        long parentGid = 0;
        if (entry.parentId != 0) {
            if (entry.parentNamespaceId != entry.namespaceId && entry.parentNamespaceId != linkedNamespaceId) {
                throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0031"));
            }
            if (entry.id != -1) {
                if (entry.parentNamespaceId == entry.namespaceId && entry.parentId == entry.id) {
                    throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0032"));
                }
            }
            parentGid = getGID(entry.parentNamespaceId, entry.parentId);
            DBRoleTypeEntry pEntry = fetchDBRoleTypeEntry(parentGid);
            if (pEntry == null) {
                throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0039", new String[] { "Parent Role Type", String.valueOf(entry.parentNamespaceId), String.valueOf(entry.parentId) }));
            }
            if ((pEntry.domainGid != domainGid) || pEntry.rangeGid != rangeGid) {
                throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0038"));
            }
        }
        if (entry.id == -1) {
            entry.id = getNextFormId(ROLE_TYPE_TABLE_KEY, String.valueOf(entry.namespaceId));
        }
        if ((entry.code == null) || entry.code.equals("")) {
            entry.code = generateRoleTypeCode(entry.id);
        }
        entry.name = DTSUtil.checkValue("Role Type Name", entry.name, DTSDataLimits.LEN_NAME);
        entry.code = DTSUtil.checkValue("Role Type Code", entry.code, DTSDataLimits.LEN_CODE);
        if (isRoleTypeExist(entry.name, linkedNamespaceId)) {
            throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0033", new String[] { entry.name }));
        }
        String sql = getDAO().getStatement(ROLE_TYPE_TABLE_KEY, "INSERT");
        PreparedStatement pstmt = null;
        int result;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, getGID(entry.namespaceId, entry.id));
            pstmt.setInt(2, entry.id);
            pstmt.setString(3, entry.code);
            pstmt.setString(4, entry.name);
            pstmt.setInt(5, entry.namespaceId);
            pstmt.setLong(6, domainGid);
            pstmt.setLong(7, rangeGid);
            if (rightIdentityGid == 0) {
                pstmt.setNull(8, Types.INTEGER);
            } else {
                pstmt.setLong(8, rightIdentityGid);
            }
            pstmt.setNull(9, Types.INTEGER);
            if (parentGid == 0) {
                pstmt.setNull(10, Types.INTEGER);
            } else {
                pstmt.setLong(10, parentGid);
            }
            result = pstmt.executeUpdate();
        } finally {
            closeStatement(pstmt);
        }
        return (result == 1);
    }

    protected String generateRoleTypeCode(int id) {
        return "R" + id;
    }

    /**
   * Deletes a Role Type in writable namespace.
   *
   * @param id ID of Role Type
   * @param namespaceId Namespace ID of Role Type
   * @param removeReferencesInRoleTypes Check references in other role types
   * @param permit DTS Permission
   * @return true if Role Type entry is deleted, else false
   * @throws DTSValidationException If DTS Validation fails
   * @throws SQLException For Db Errors
   * @throws PermissionException If there is no permission to write to Namespace of the Role Type
   *
   * @see com.apelon.dts.client.concept.BaseOntylogConceptQuery#deleteRoleType(com.apelon.dts.client.attribute.DTSRoleType, boolean)
   * @since DTS 3.4.2
   */
    public boolean deleteRoleType(int id, int namespaceId, boolean removeReferencesInRoleTypes, DTSPermission permit) throws SQLException, PermissionException, DTSValidationException {
        checkPermission(permit, String.valueOf(namespaceId));
        boolean exist = isRoleTypeUsed(namespaceId, id);
        if (exist) {
            throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0034"));
        }
        if (!removeReferencesInRoleTypes) {
            StringBuffer msgBuf = new StringBuffer();
            DTSTransferObject[] objects = fetchRightIdentityReferences(namespaceId, id);
            if (objects.length > 0) {
                msgBuf.append("Role Type is Right Identity in one or more Role Types.");
            }
            objects = fetchParentReferences(namespaceId, id);
            if (objects.length > 0) {
                if (msgBuf.length() > 0) {
                    msgBuf.append("\n");
                }
                msgBuf.append("Role Type is Parent of one or more Role Types.");
            }
            if (msgBuf.length() > 0) {
                throw new DTSValidationException(msgBuf.toString());
            }
        }
        String sqlRightId = getDAO().getStatement(ROLE_TYPE_TABLE_KEY, "DELETE_RIGHT_IDENTITY_REF");
        String sqlParent = getDAO().getStatement(ROLE_TYPE_TABLE_KEY, "DELETE_PARENT_REF");
        String sql = getDAO().getStatement(ROLE_TYPE_TABLE_KEY, "DELETE");
        PreparedStatement pstmt = null;
        boolean success = false;
        long typeGid = getGID(namespaceId, id);
        conn.setAutoCommit(false);
        int defaultLevel = conn.getTransactionIsolation();
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        try {
            pstmt = conn.prepareStatement(sqlRightId);
            pstmt.setLong(1, typeGid);
            pstmt.executeUpdate();
            pstmt.close();
            pstmt = conn.prepareStatement(sqlParent);
            pstmt.setLong(1, typeGid);
            pstmt.executeUpdate();
            pstmt.close();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, typeGid);
            int count = pstmt.executeUpdate();
            success = (count == 1);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setTransactionIsolation(defaultLevel);
            conn.setAutoCommit(true);
            closeStatement(pstmt);
        }
        return success;
    }

    /**
   * Updates a Role Type in writable Ontylog Extension namespace.
   *
   * @param id ID of Role Type to update
   * @param namespaceId Namespace ID of Role Type to update
   * @param entry Role Type entry with the new values
   * @param permit DTS Permission
   * @return true if Role Type entry is added, else false
   * @throws DTSValidationException If DTS Validation fails
   * @throws SQLException For Db Errors
   * @throws PermissionException If there is no permission to write to Namespace of the Role Type
   *
   * @see com.apelon.dts.client.concept.BaseOntylogConceptQuery#updateRoleType(com.apelon.dts.client.attribute.DTSRoleType, com.apelon.dts.client.attribute.DTSRoleType) )
   * @since DTS 3.4.2
   */
    public boolean updateRoleType(int id, int namespaceId, RoleTypeEntry entry, DTSPermission permit) throws SQLException, PermissionException, DTSValidationException {
        checkPermission(permit, String.valueOf(namespaceId));
        boolean exist = isRoleTypeUsed(namespaceId, id);
        int linkedNamespaceId = getLinkedNamespaceId(namespaceId);
        if (linkedNamespaceId == -1) {
            throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0027", new String[] { String.valueOf(entry.namespaceId) }));
        }
        DBRoleTypeEntry currEntry = fetchDBRoleTypeEntry(getGID(namespaceId, id));
        long domainGid = currEntry.domainGid;
        long rangeGid = currEntry.rangeGid;
        long rightIdentityGid = 0;
        long parentGid = 0;
        if (!exist) {
            if (entry.domainKindNamespaceId != 0) {
                if (entry.domainKindNamespaceId != namespaceId && entry.domainKindNamespaceId != linkedNamespaceId) {
                    throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0028"));
                }
                domainGid = getGID(entry.domainKindNamespaceId, entry.domainKindId);
            }
            if (entry.rangeKindNamespaceId != 0) {
                if (entry.rangeKindNamespaceId != namespaceId && entry.rangeKindNamespaceId != linkedNamespaceId) {
                    throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0029"));
                }
                rangeGid = getGID(entry.rangeKindNamespaceId, entry.rangeKindId);
            }
            if (entry.rightIdentityId != 0) {
                if (entry.rightIdentityNamespaceId != namespaceId && entry.rightIdentityNamespaceId != linkedNamespaceId) {
                    throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0030"));
                }
                if (entry.rightIdentityNamespaceId == namespaceId && entry.rightIdentityId == id) {
                    throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0035"));
                }
                rightIdentityGid = getGID(entry.rightIdentityNamespaceId, entry.rightIdentityId);
                DBRoleTypeEntry riEntry = fetchDBRoleTypeEntry(rightIdentityGid);
                if (riEntry == null) {
                    throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0039", new String[] { "Right Identity Role Type", String.valueOf(entry.rightIdentityNamespaceId), String.valueOf(entry.rightIdentityId) }));
                }
                if (riEntry.domainGid != riEntry.rangeGid) {
                    throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0036"));
                }
                if (riEntry.domainGid != rangeGid) {
                    throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0037"));
                }
            }
            if (entry.parentId != 0) {
                if (entry.parentNamespaceId != namespaceId && entry.parentNamespaceId != linkedNamespaceId) {
                    throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0031"));
                }
                if (entry.id != -1) {
                    if (entry.parentNamespaceId == namespaceId && entry.parentId == id) {
                        throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0032"));
                    }
                }
                parentGid = getGID(entry.parentNamespaceId, entry.parentId);
                DBRoleTypeEntry pEntry = fetchDBRoleTypeEntry(parentGid);
                if (pEntry == null) {
                    throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0039", new String[] { "Parent Role Type", String.valueOf(entry.parentNamespaceId), String.valueOf(entry.parentId) }));
                }
                if ((pEntry.domainGid != domainGid) || pEntry.rangeGid != rangeGid) {
                    throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0038"));
                }
            }
        }
        if (entry.name != null) {
            entry.name = DTSUtil.checkValue("Role Type Name", entry.name, DTSDataLimits.LEN_NAME);
            if (isRoleTypeExist(entry.name, linkedNamespaceId)) {
                throw new DTSValidationException(ApelMsgHandler.getInstance().getMsg("DTS-0033", new String[] { entry.name }));
            }
        }
        String sql = getDAO().getStatement(ROLE_TYPE_TABLE_KEY, "SELECT_FOR_UPDATE");
        PreparedStatement pstmt = null;
        boolean success = false;
        try {
            pstmt = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            pstmt.setInt(1, id);
            pstmt.setInt(2, namespaceId);
            ResultSet rset = pstmt.executeQuery();
            if (rset.next()) {
                if (entry.name != null) {
                    rset.updateString(1, entry.name);
                }
                if (!exist) {
                    rset.updateLong(2, domainGid);
                    rset.updateLong(3, rangeGid);
                    if (rightIdentityGid != 0) {
                        rset.updateLong(4, rightIdentityGid);
                    } else {
                        rset.updateNull(4);
                    }
                    if (parentGid != 0) {
                        rset.updateLong(5, parentGid);
                    } else {
                        rset.updateNull(5);
                    }
                }
                rset.updateRow();
                success = true;
            }
        } finally {
            closeStatement(pstmt);
        }
        return success;
    }

    /**
   * Determines whether a Role Type exists for the given name and namespace ID.
   * @param name Role Type name
   * @param namespaceId Namespace ID
   * @return boolean true if the Role Type exists else false
   * @throws SQLException
   * @since DTS 3.4.2
   */
    protected boolean isRoleTypeExist(String name, int namespaceId) throws SQLException {
        String statement = getDAO().getStatement(ROLE_TYPE_TABLE_KEY, "EXIST_ROLE_TYPE_LINKED_NAMESPACE");
        statement = getDAO().getStatement(statement, 1, name);
        statement = getDAO().getStatement(statement, 2, String.valueOf(namespaceId));
        return exists(statement);
    }

    /**
   * Returns whether a role type for given ID and namespace ID
   * is used in a role definition or not.
   *
   * @param namespaceId Namespace Id of Role Type
   * @param id Id of Role Type
   * @return true if the Role Type is used in role defintion else false.
   * @throws SQLException for any Database Error
   *
   * @see com.apelon.dts.client.concept.BaseOntylogConceptQuery#isRoleTypeUsed(com.apelon.dts.client.attribute.DTSRoleType)
   * @since DTS 3.4.2
   */
    public boolean isRoleTypeUsed(int namespaceId, int id) throws SQLException {
        String statement = getDAO().getStatement(ROLE_TYPE_TABLE_KEY, "EXIST_ROLE_TYPE");
        long gid = this.getGID(namespaceId, id);
        statement = getDAO().getStatement(statement, 1, String.valueOf(gid));
        return exists(statement);
    }

    public DTSTransferObject[] fetchRightIdentityReferences(int namespaceId, int id) throws SQLException {
        String sql = getDAO().getStatement(ROLE_TYPE_TABLE_KEY, "GET_RIGHT_IDENTITY_REF");
        return fetchRoleTypeReferences(sql, namespaceId, id);
    }

    public DTSTransferObject[] fetchParentReferences(int namespaceId, int id) throws SQLException {
        String sql = getDAO().getStatement(ROLE_TYPE_TABLE_KEY, "GET_PARENT_REF");
        return fetchRoleTypeReferences(sql, namespaceId, id);
    }

    private DTSTransferObject[] fetchRoleTypeReferences(String sql, int namespaceId, int id) throws SQLException {
        long gid = getGID(namespaceId, id);
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setLong(1, gid);
        ResultSet rset = pstmt.executeQuery();
        ArrayList list = new ArrayList();
        while (rset.next()) {
            list.add(new DTSTransferObject(rset.getInt(1), rset.getString(2), rset.getString(3), rset.getInt(4)));
        }
        pstmt.close();
        return (DTSTransferObject[]) list.toArray(new DTSTransferObject[0]);
    }

    /**
   * Returns the linked namespace id for the given ontylog extension namespace
   * from the DTS_NAMESPACE_LINKAGE table
   * @param extensionNamespace
   * @return int Namespace ID if found, else -1
   * @throws SQLException
   * @since DTS 3.4.2
   */
    protected int getLinkedNamespaceId(int extensionNamespace) throws SQLException {
        int linkedNamespaceId = -1;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(getDAO().getStatement(ROLE_TYPE_TABLE_KEY, "GET_LINKED_NAMESPACE"));
            ps.setInt(1, extensionNamespace);
            rs = ps.executeQuery();
            while (rs.next()) {
                linkedNamespaceId = rs.getInt(1);
            }
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
        return linkedNamespaceId;
    }

    /**
  * Get query info from Xml Element, get DAO and
  * execute Property Type, Role Type and Index queries.
  *
  * @param root a Xml DOM Element holding data about the type of fetch to execute.
  *
  * @return a String of Xml holding data about the fetched objects.
  *
  * @since DTS 3.0
  */
    public String executeFetchType(Element root) throws SQLException, XMLException {
        Element target = ((Element) root.getFirstChild());
        String value = null;
        if (target.hasChildNodes()) {
            value = target.getFirstChild().getNodeValue();
        }
        String value_type = target.getAttribute("value").toString().toUpperCase();
        String target_name = target.getTagName().toString().toUpperCase();
        String namespaceId = target.getAttribute("namespaceId");
        boolean one = true;
        String key = "EXECUTE_FETCH_" + target_name;
        String statement = getDAO().getStatement(TABLE_KEY, key);
        String whereStatement = getDAO().getStatement(TABLE_KEY, key + "_" + value_type);
        whereStatement = getDAO().getStatement(whereStatement, 1, "'" + value + "'");
        whereStatement += namespaceId;
        String orderBy = getDAO().getStatement(TABLE_KEY, key + "_ORDER_BY");
        statement += " " + whereStatement + " " + orderBy;
        if (value_type.equals("ALL")) {
            one = false;
        }
        if (target_name.equals("ROLETYPE")) {
            if (value_type.equals("NAMESPACEID")) {
                one = false;
            }
            return executeRoleQuery(statement, one);
        }
        if (target_name.equals("PROPERTYTYPE")) {
            return executePropertyQuery(statement, one);
        }
        throw new XMLException("unrecognized target", target);
    }

    /**
  * Called by executeFetchType if taget name is "NAMESPACEID" to execute the
  * sql and create the Xml result.
  */
    protected String executeRoleQuery(String sql, boolean one) throws SQLException {
        String roleTypesElem = "roleTypes";
        String roleTypeElem = "roleType";
        StringBuffer result = new StringBuffer();
        if (!one) {
            appendDtd(result, com.apelon.dts.dtd.common.DTD.ROLETYPE, roleTypesElem);
            XML.asStartTag(result, roleTypesElem);
        } else {
            appendDtd(result, com.apelon.dts.dtd.common.DTD.ROLETYPE, roleTypeElem);
        }
        Statement stmt = conn.createStatement();
        ResultSet res = stmt.executeQuery(sql);
        if (!res.next()) {
            res.close();
            stmt.close();
            return nullDocument(com.apelon.dts.dtd.result.DTD.COMMON_RESULT);
        } else {
            do {
                String name = res.getString(1);
                int id = res.getInt(2);
                String code = res.getString(3);
                int namespaceId = res.getInt(4);
                long domain_kind_gid = res.getLong(5);
                long range_kind_gid = res.getLong(6);
                long right_identity_gid = res.getLong(7);
                long parent_gid = res.getLong(8);
                Kind dk = getKind(domain_kind_gid);
                Kind rk = getKind(range_kind_gid);
                DTSRoleType rt = getRoleType(right_identity_gid);
                DTSRoleType pr = getRoleType(parent_gid);
                result.append(DTSXMLFactory.createRoleTypeCompleteXML(name, id, code, namespaceId, dk.getName(), dk.getId(), dk.getCode(), dk.getNamespaceId(), dk.isReference(), rk.getName(), rk.getId(), rk.getCode(), rk.getNamespaceId(), rk.isReference(), rt, pr, false));
                if (one) {
                    break;
                }
            } while (res.next());
        }
        if (!one) {
            XML.asEndTag(result, roleTypesElem);
        }
        res.close();
        stmt.close();
        return result.toString();
    }

    /**
  * Called by executeFetchType if taget name is "PROPERTYTYPE" to execute the
  * sql and create the Xml result.
  */
    protected String executePropertyQuery(String sql, boolean one) throws SQLException {
        String propTypeElem = "propertyType";
        String propTypesElem = "propertyTypes";
        StringBuffer result = new StringBuffer();
        if (one) {
            appendDtd(result, com.apelon.dts.dtd.result.DTD.CONCEPT_RESULT, propTypeElem);
        } else {
            appendDtd(result, com.apelon.dts.dtd.result.DTD.CONCEPT_RESULT, propTypesElem);
        }
        if (!one) {
            XML.asStartTag(result, propTypesElem);
        }
        Statement stmt = conn.createStatement();
        ResultSet res = stmt.executeQuery(sql);
        if (!res.next()) {
            res.close();
            stmt.close();
            return nullDocument(com.apelon.dts.dtd.result.DTD.COMMON_RESULT);
        } else {
            do {
                String name = res.getString(1);
                int id = res.getInt(2);
                String code = res.getString(3);
                result.append(DTSXMLFactory.createPropertyTypeXML(name, id, code, 1, ATTACH_CONCEPT.charAt(0), INDEXABLE.charAt(0), false));
                if (one) {
                    break;
                }
            } while (res.next());
        }
        if (!one) {
            XML.asEndTag(result, propTypesElem);
        }
        res.close();
        stmt.close();
        return result.toString();
    }

    protected DBRoleTypeEntry fetchDBRoleTypeEntry(long gid) throws SQLException {
        String sql = getDAO().getStatement("ROLE_TYPE_DB", "SELECT_BY_GID");
        sql += gid;
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery(sql);
        DBRoleTypeEntry entry = null;
        while (rset.next()) {
            entry = new DBRoleTypeEntry();
            entry.gid = gid;
            entry.domainGid = rset.getLong(1);
            entry.rangeGid = rset.getLong(2);
            entry.rightIdentityGid = rset.getLong(3);
            entry.parentGid = rset.getLong(4);
        }
        stmt.close();
        return entry;
    }

    public static class DBRoleTypeEntry {

        private long gid;

        private long domainGid;

        private long rangeGid;

        private long rightIdentityGid;

        private long parentGid;

        public long getGid() {
            return gid;
        }

        public void setGid(long gid) {
            this.gid = gid;
        }

        public long getDomainGid() {
            return domainGid;
        }

        public void setDomainGid(long domainGid) {
            this.domainGid = domainGid;
        }

        public long getRangeGid() {
            return rangeGid;
        }

        public void setRangeGid(long rangeGid) {
            this.rangeGid = rangeGid;
        }

        public long getRightIdentityGid() {
            return rightIdentityGid;
        }

        public void setRightIdentityGid(long rightIdentityGid) {
            this.rightIdentityGid = rightIdentityGid;
        }

        public long getParentGid() {
            return parentGid;
        }

        public void setParentGid(long parentGid) {
            this.parentGid = parentGid;
        }
    }

    public static class RoleTypeEntry {

        private int id;

        private String code;

        private String name;

        private int namespaceId;

        private int domainKindId;

        private int domainKindNamespaceId;

        private int rangeKindId;

        private int rangeKindNamespaceId;

        private int rightIdentityId;

        private int rightIdentityNamespaceId;

        private int parentId;

        private int parentNamespaceId;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getNamespaceId() {
            return namespaceId;
        }

        public void setNamespaceId(int namespaceId) {
            this.namespaceId = namespaceId;
        }

        public int getDomainKindId() {
            return domainKindId;
        }

        public void setDomainKindId(int domainKindId) {
            this.domainKindId = domainKindId;
        }

        public int getDomainKindNamespaceId() {
            return domainKindNamespaceId;
        }

        public void setDomainKindNamespaceId(int domainKindNamespaceId) {
            this.domainKindNamespaceId = domainKindNamespaceId;
        }

        public int getRangeKindId() {
            return rangeKindId;
        }

        public void setRangeKindId(int rangeKindId) {
            this.rangeKindId = rangeKindId;
        }

        public int getRangeKindNamespaceId() {
            return rangeKindNamespaceId;
        }

        public void setRangeKindNamespaceId(int rangeKindNamespaceId) {
            this.rangeKindNamespaceId = rangeKindNamespaceId;
        }

        public int getRightIdentityId() {
            return rightIdentityId;
        }

        public void setRightIdentityId(int rightIdentityId) {
            this.rightIdentityId = rightIdentityId;
        }

        public int getRightIdentityNamespaceId() {
            return rightIdentityNamespaceId;
        }

        public void setRightIdentityNamespaceId(int rightIdentityNamespaceId) {
            this.rightIdentityNamespaceId = rightIdentityNamespaceId;
        }

        public int getParentId() {
            return parentId;
        }

        public void setParentId(int parentId) {
            this.parentId = parentId;
        }

        public int getParentNamespaceId() {
            return parentNamespaceId;
        }

        public void setParentNamespaceId(int parentNamespaceId) {
            this.parentNamespaceId = parentNamespaceId;
        }
    }
}
