package com.apelon.dts.db.subset;

import com.apelon.common.log4j.Categories;
import com.apelon.common.sql.SQL;
import com.apelon.common.util.GidGenerator;
import com.apelon.common.util.db.dao.GeneralDAO;
import com.apelon.common.util.graph.SubDagExtractor;
import com.apelon.common.xml.XMLException;
import com.apelon.dts.common.*;
import com.apelon.dts.common.subset.PreviewOptions;
import com.apelon.dts.common.subset.Subset;
import com.apelon.dts.common.subset.SubsetDescriptor;
import com.apelon.dts.common.subset.SubsetUpdate;
import com.apelon.dts.db.DTSConceptDb;
import com.apelon.dts.db.QuerySession;
import com.apelon.dts.db.config.SubsetDBConfig;
import com.apelon.dts.db.dao.SubsetDAOFactory;
import com.apelon.dts.db.dao.SubsetGeneralDAO;
import com.apelon.dts.db.subset.expression.DirectSupPlusBuilder;
import com.apelon.dts.db.subset.expression.ExpressionBuilder;
import com.apelon.dts.db.subset.expression.SETreeNodeConstructor;
import com.apelon.dts.db.subset.expression.Utilities;
import com.apelon.dts.server.DTSPermission;
import com.apelon.dts.server.DTSUsers;
import com.apelon.dts.server.PermissionException;
import com.apelon.graph.dag.Dag;
import org.w3c.dom.Element;
import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/**
 * <p>Title: SubsetDb </p>
 * <p>Description:  DB layer class to handle all subset related queries and updates</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: Apelon Inc. </p>
 * @author Apelon Inc.
 * @version DTS 3.4.0
 * @since 3.4.0
 */
public class SubsetDb extends DTSConceptDb {

    static HashMap subsetIdBuildMap = new HashMap();

    private static final String TABLE_KEY = "SUBSET_DB";

    private static SubsetDBConfig dbConfig;

    static {
        dbConfig = new SubsetDBConfig();
    }

    private GeneralDAO subsetDAO;

    protected Statement keepAliveStmt = null;

    public SubsetDb(Connection conn) throws SQLException {
        super(conn);
        init();
    }

    public static void loadDAO(String type) {
        SubsetDAOFactory.getDAOFactory(type, dbConfig);
    }

    public SubsetGeneralDAO getSubsetGeneralDao() {
        return (SubsetGeneralDAO) this.subsetDAO;
    }

    public void close() throws SQLException {
        super.close();
        this.closeStatements(new Statement[] { keepAliveStmt, addSubsetPs, delSubsetPs, updateSubsetNamePs, getSubsetPs, addSubsetRefPs, addSubsetHierDataPs, addSubsetHierPs, getConceptSubsetPs, updateSubStmt, updateSubsetLoadDateStmt, getNspType, getLinkNsp });
    }

    private void init() throws SQLException {
        getDAO(this.fSqlTarget);
        keepAliveStmt = conn.createStatement();
        prepare();
    }

    /**
   * Prepare the KB before running queries and update.
   * For ex: stored procs are needed for the SQL2k for
   * the MINUS and INTERSECT operations
   * @throws SQLException
   */
    private void prepare() throws SQLException {
        if (SQL.getConnType(conn).equals(SQL.SQL2K)) {
            boolean minusSpPresent = false;
            boolean intersectSpPresent = false;
            String minusSp = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, "MINUS_SP_NAME");
            String intersectSp = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, "INTERSECT_SP_NAME");
            String checkSp = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, CHECK_SP);
            checkSp = subsetDAO.getStatement(checkSp, 1, minusSp);
            checkSp = subsetDAO.getStatement(checkSp, 2, intersectSp);
            ResultSet rs = this.keepAliveStmt.executeQuery(checkSp);
            while (rs.next()) {
                String name = rs.getString(1);
                if (name.equals(minusSp)) {
                    minusSpPresent = true;
                } else if (name.equals(intersectSp)) {
                    intersectSpPresent = true;
                }
            }
            if (!minusSpPresent) {
                String createMinusSp = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, "CREATE_MINUS_SP");
                this.keepAliveStmt.executeUpdate(createMinusSp);
                Categories.dataDb().debug("Stored proc " + minusSp + " successfully created.");
            }
            if (!intersectSpPresent) {
                String createIntersectSp = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, "CREATE_INTERSECT_SP");
                this.keepAliveStmt.executeUpdate(createIntersectSp);
                Categories.dataDb().debug("Stored proc " + intersectSp + " successfully created.");
            }
            rs.close();
        }
    }

    private void getDAO(String type) {
        subsetDAO = SubsetDAOFactory.getDAOFactory(type, dbConfig);
    }

    public Subset[] getSubsets(DataTypeFilter filter, SubsetDescriptor descriptor) throws SQLException, IOException {
        ArrayList list = new ArrayList();
        String getAllSubsQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_ALL_SUBSETS);
        if (filter != null) {
            String filterBy = filter.getFilterBy();
            if (filterBy.equals(DataTypeFilter.FILTER_BY_NAMESPACE)) {
                int namespaceId = filter.getNamespaceId();
                if (namespaceId == DataTypeFilter.ALL_NAMESPACE) {
                    getAllSubsQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_ALL_SUBSETS);
                } else {
                    getAllSubsQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_ALL_SUBSETS_WITH_NAMESPACE_ID);
                    getAllSubsQuery = getDAO().getStatement(getAllSubsQuery, 1, String.valueOf(namespaceId));
                }
            } else if (filterBy.equals(DataTypeFilter.FILTER_BY_NAME_MATCHING)) {
                getAllSubsQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_ALL_SUBSETS_WITH_NAME_MATCH);
                String namePattern = filter.getNamePattern();
                namePattern = Utilities.escapeSQL(namePattern);
                namePattern = namePattern.toUpperCase();
                if (namePattern.indexOf("*") >= 0) {
                    namePattern = namePattern.replace('*', '%');
                    getAllSubsQuery = subsetDAO.getStatement(getAllSubsQuery, 2, " LIKE ");
                } else {
                    getAllSubsQuery = subsetDAO.getStatement(getAllSubsQuery, 2, " = ");
                }
                getAllSubsQuery = subsetDAO.getStatement(getAllSubsQuery, 1, namePattern);
            }
        }
        ResultSet rs = this.keepAliveStmt.executeQuery(getAllSubsQuery);
        try {
            while (rs.next()) {
                int subset_id = rs.getInt(1);
                String name = rs.getString(2);
                Timestamp created_date = rs.getTimestamp(3);
                String created_by = rs.getString(4);
                Timestamp modified_date = rs.getTimestamp(5);
                String modified_by = rs.getString(6);
                Timestamp dataLoad_date = rs.getTimestamp(7);
                String description = rs.getString(8);
                Subset si = new Subset();
                si.setId(subset_id);
                si.setName(name);
                if (descriptor.isFetchExpression()) {
                    String expr = this.getSubsetGeneralDao().getSubsetExpression(this.conn, subset_id);
                    si.setExpression(expr);
                }
                si.setCreatedTime(created_date.getTime());
                si.setCreatedBy(created_by);
                si.setModifiedTime(modified_date.getTime());
                si.setModifiedBy(modified_by);
                si.setDataCreatedTime(dataLoad_date.getTime());
                si.setDescription(description);
                if (descriptor.isFetchConceptCount()) {
                    int conceptCount = getSubsetConceptsCount(subset_id);
                    si.setConceptCount(conceptCount);
                }
                list.add(si);
            }
        } finally {
            rs.close();
        }
        Subset[] array = new Subset[list.size()];
        list.toArray(array);
        return array;
    }

    public Subset addSubset(String user, Subset item) throws SQLException, DTSValidationException {
        long newSubsetId = getSubsetIdFromSeq(this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, "GET_SUBSET_SEQ_ID_NAME"));
        return this.addSubset(user, item, (int) newSubsetId, true);
    }

    public Subset addSubset(String user, Subset item, int newSubsetId, boolean persistToDb) throws SQLException, DTSValidationException {
        int origTransLevel = Utilities.beginTransaction(this.conn);
        try {
            if (this.addSubsetPs == null) {
                String addSubsStmt = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, ADD_SUBSET);
                this.addSubsetPs = this.conn.prepareStatement(addSubsStmt);
            }
            item.setCreatedBy(user);
            item.setModifiedBy(user);
            int count = this.getSubsetGeneralDao().addSubset(addSubsetPs, newSubsetId, item);
            long beg = System.currentTimeMillis();
            if (count > 0) {
                this.getSubsetGeneralDao().insertSubsetExpression(this.conn, item.getId(), item.getExpression());
                DefaultMutableTreeNode node = SETreeNodeConstructor.getSETreeNode(item.getExpression());
                Dag dag = TreeDagTranslator.translateTreeToDag(node);
                saveSubsetRefInfo(item, dag);
                long end = System.currentTimeMillis();
                Categories.dataDb().debug(" Subset added in " + (end - beg) / 1000.00 + " secs");
                this.conn.commit();
                return item;
            } else return null;
        } catch (Exception e) {
            this.conn.rollback();
            throw new SQLException(e.getMessage());
        } finally {
            Utilities.endTransaction(this.conn, origTransLevel);
        }
    }

    /**
   * Constructs a preview for the subset.
   * (Internally it adds a subset with a -ive id, computes the results and returns results for it.
   *  After computation it deletes the temporary preview subset.)
   */
    public DTSTransferObject[] previewSubset(String user, String subsetExpr, PreviewOptions options) throws IOException, SQLException, DTSValidationException {
        int previewLimit = options.getLimit();
        long newSubsetId = getSubsetIdFromSeq();
        int previewId = (int) newSubsetId * -1;
        long time = System.currentTimeMillis();
        Subset item = new Subset();
        item.setId(previewId);
        item.setExpression(subsetExpr);
        item.setName("Preview" + previewId);
        item.setDataCreatedTime(time);
        item.setModifiedTime(time);
        ArrayList results = this.getPreviewResults(item, false);
        DTSTransferObject[] cons = new DTSTransferObject[0];
        if (results != null && results.size() > 0) {
            cons = getConceptInfo(results, previewLimit);
            Categories.dataDb().debug("Retrieved [" + cons.length + "] concepts for subset preview [" + previewId + "]");
        }
        return cons;
    }

    /**
   * This gets the diff between the preview expression results and the concepts in the given subset.
   */
    public DTSTransferObject[] previewDifferences(String user, String subsetExpr, int subsetId, PreviewOptions options) throws SQLException, DTSValidationException, PermissionException {
        long newSubsetId = getSubsetIdFromSeq();
        int previewId = (int) newSubsetId * -1;
        long time = System.currentTimeMillis();
        Subset item = new Subset();
        item.setId(previewId);
        item.setExpression(subsetExpr);
        item.setName("Preview" + previewId);
        item.setDataCreatedTime(time);
        item.setModifiedTime(time);
        DTSTransferObject[] cons = new DTSTransferObject[0];
        this.addSubset(user, item, previewId, true);
        this.getPreviewResults(item, true);
        String connType = SQL.getConnType(this.conn);
        if (connType.equals(SQL.ORACLE)) {
            cons = this.previewSubsetDiffORA(previewId, subsetId, options);
        } else if (connType.equals(SQL.SQL2K) || connType.equals(SQL.CACHE)) {
            cons = this.previewSubsetDiffSQL2K(previewId, subsetId, options);
        }
        Categories.dataDb().debug("Got [" + cons.length + "] differences [" + previewId + "]");
        this.deleteSubset(previewId, null);
        return cons;
    }

    private DTSTransferObject[] previewSubsetDiffORA(int subsetId1, int subsetId2, PreviewOptions options) throws SQLException {
        Statement getSubsDiffStmt = null;
        try {
            String getSubsDiffQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_SUBSETS_DIFF);
            if (options.getPreviewType() == PreviewOptions.DIFF_ADDITIONS) {
                getSubsDiffQuery = this.subsetDAO.getStatement(getSubsDiffQuery, 1, String.valueOf(subsetId1));
                getSubsDiffQuery = this.subsetDAO.getStatement(getSubsDiffQuery, 2, String.valueOf(subsetId2));
            } else if (options.getPreviewType() == PreviewOptions.DIFF_DELETIONS) {
                getSubsDiffQuery = this.subsetDAO.getStatement(getSubsDiffQuery, 1, String.valueOf(subsetId2));
                getSubsDiffQuery = this.subsetDAO.getStatement(getSubsDiffQuery, 2, String.valueOf(subsetId1));
            } else {
                throw new IllegalArgumentException("Only PreviewOptions.DIFF_ADDITIONS and PreviewOptions.DIFF_DELETIONS are supported for previewing differences.");
            }
            getSubsDiffQuery = this.subsetDAO.getStatement(getSubsDiffQuery, 3, String.valueOf(options.getLimit()));
            getSubsDiffStmt = this.conn.createStatement();
            ResultSet rs = getSubsDiffStmt.executeQuery(getSubsDiffQuery);
            Categories.dataDb().debug("Preview SQL [" + getSubsDiffQuery + "]");
            ArrayList al = new ArrayList(20);
            while (rs.next()) {
                int concept_id = rs.getInt(1);
                String concept_code = rs.getString(2);
                String concept_name = rs.getString(3);
                int namespaceId = rs.getInt(4);
                DTSTransferObject dto = new DTSTransferObject(concept_id, concept_code, concept_name, namespaceId);
                al.add(dto);
            }
            rs.close();
            DTSTransferObject[] dtoa = new DTSTransferObject[al.size()];
            dtoa = (DTSTransferObject[]) al.toArray(dtoa);
            return dtoa;
        } finally {
            if (getSubsDiffStmt != null) {
                getSubsDiffStmt.close();
            }
        }
    }

    private DTSTransferObject[] previewSubsetDiffSQL2K(int subsetId1, int subsetId2, PreviewOptions options) throws SQLException {
        Statement getSubsDiffStmt = null;
        try {
            String getSubsDiffQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_SUBSETS_DIFF);
            if (options.getPreviewType() == PreviewOptions.DIFF_ADDITIONS) {
                getSubsDiffQuery = this.subsetDAO.getStatement(getSubsDiffQuery, 1, String.valueOf(subsetId1));
                getSubsDiffQuery = this.subsetDAO.getStatement(getSubsDiffQuery, 2, String.valueOf(subsetId2));
            } else if (options.getPreviewType() == PreviewOptions.DIFF_DELETIONS) {
                getSubsDiffQuery = this.subsetDAO.getStatement(getSubsDiffQuery, 1, String.valueOf(subsetId2));
                getSubsDiffQuery = this.subsetDAO.getStatement(getSubsDiffQuery, 2, String.valueOf(subsetId1));
            } else {
                throw new IllegalArgumentException("Only PreviewOptions.DIFF_ADDITIONS and PreviewOptions.DIFF_DELETIONS are supported for previewing differences.");
            }
            getSubsDiffStmt = this.conn.createStatement();
            getSubsDiffStmt.setMaxRows(options.getLimit());
            ResultSet rs = getSubsDiffStmt.executeQuery(getSubsDiffQuery);
            Categories.dataDb().debug("Preview SQL [" + getSubsDiffQuery + "]");
            ArrayList gidArray = new ArrayList(20);
            while (rs.next()) {
                long concept_gid = rs.getLong(1);
                gidArray.add(new Long(concept_gid));
            }
            rs.close();
            DTSTransferObject[] dtoa = this.getConceptInfo(gidArray, options.getLimit());
            return dtoa;
        } finally {
            if (getSubsDiffStmt != null) {
                getSubsDiffStmt.close();
            }
        }
    }

    private DTSTransferObject[] previewSubsetDiffSQL2K_test(int subsetId1, int subsetId2, PreviewOptions options) throws SQLException {
        Statement getSubsDiffStmt = null;
        try {
            String spName = "dts_sp_MINUS";
            String param1 = "SELECT s.concept_gid\n" + "              FROM dts_subset_concept s\n" + "              WHERE\n" + "              s.subset_id = ?1?";
            String param2 = "SELECT s.concept_gid\n" + "              FROM dts_subset_concept s\n" + "              WHERE\n" + "              s.subset_id = ?2?";
            if (options.getPreviewType() == PreviewOptions.DIFF_ADDITIONS) {
                param1 = this.subsetDAO.getStatement(param1, 1, String.valueOf(subsetId1));
                param2 = this.subsetDAO.getStatement(param2, 2, String.valueOf(subsetId2));
            } else if (options.getPreviewType() == PreviewOptions.DIFF_DELETIONS) {
                param1 = this.subsetDAO.getStatement(param1, 1, String.valueOf(subsetId2));
                param2 = this.subsetDAO.getStatement(param2, 2, String.valueOf(subsetId1));
            } else {
                throw new IllegalArgumentException("Only PreviewOptions.DIFF_ADDITIONS and PreviewOptions.DIFF_DELETIONS are supported for previewing differences.");
            }
            ArrayList gidArray = runStoreProc(spName, param1, param2, options);
            DTSTransferObject[] dtoa = this.getConceptInfo(gidArray, options.getLimit());
            return dtoa;
        } finally {
            if (getSubsDiffStmt != null) {
                getSubsDiffStmt.close();
            }
        }
    }

    private ArrayList runStoreProc(String spName, String query1, String query2, PreviewOptions options) throws SQLException {
        CallableStatement cs = null;
        Categories.dataDb().debug("Running " + spName + " with following 2 statements \n " + " [ " + query1 + "] \n" + " [ " + query2 + "] ");
        try {
            cs = conn.prepareCall("{? = call " + spName + " (?,?)}");
            cs.setString(2, query1);
            cs.setString(3, query2);
            cs.registerOutParameter(1, Types.ARRAY);
            cs.setMaxRows(options.getLimit());
            ArrayList cons = new ArrayList(10);
            ResultSet rs = cs.executeQuery();
            while (rs.next()) {
                long conGid = rs.getLong(1);
                cons.add(new Long(conGid));
            }
            return cons;
        } finally {
            if (cs != null) {
                cs.close();
            }
        }
    }

    private ArrayList getPreviewResults(Subset item, boolean save) throws SQLException, DTSValidationException {
        try {
            DefaultMutableTreeNode node = SETreeNodeConstructor.getSETreeNode(item.getExpression());
            Dag dag = TreeDagTranslator.translateTreeToDag(node);
            ExpressionBuilder eb = new ExpressionBuilder(this.conn, this.subsetDAO);
            ArrayList results = eb.buildExpression(item, dag, save);
            return results;
        } catch (DTSValidationException dtsvex) {
            throw dtsvex;
        } catch (Exception iox) {
            throw new SQLException(iox.getMessage());
        }
    }

    private DTSTransferObject[] getConceptInfo(ArrayList conGidArray, int previewLimit) throws SQLException {
        int maxDBlimit = 1000;
        int processed = 0;
        StringBuffer gidStr = new StringBuffer(1000);
        ArrayList finalConList = new ArrayList(previewLimit);
        int totalConceptsToProcess = (conGidArray.size() > previewLimit ? previewLimit : conGidArray.size());
        while (totalConceptsToProcess > 0) {
            if (totalConceptsToProcess > maxDBlimit) {
                totalConceptsToProcess = totalConceptsToProcess - maxDBlimit;
                gidStr = new StringBuffer(1000);
                for (int l = 0; l < maxDBlimit; l++) {
                    if (l < (maxDBlimit - 1)) {
                        gidStr.append(conGidArray.get(processed++) + ", ");
                    } else {
                        gidStr.append(conGidArray.get(processed++) + "");
                    }
                }
                getConceptsFromGids(finalConList, gidStr);
            } else {
                gidStr = new StringBuffer(1000);
                for (int l = 0; l < totalConceptsToProcess; l++) {
                    if (l < (totalConceptsToProcess - 1)) {
                        gidStr.append(conGidArray.get(processed++) + ", ");
                    } else {
                        gidStr.append(conGidArray.get(processed++) + "");
                    }
                }
                getConceptsFromGids(finalConList, gidStr);
                totalConceptsToProcess = 0;
            }
        }
        DTSTransferObject[] dtoa = new DTSTransferObject[finalConList.size()];
        dtoa = (DTSTransferObject[]) finalConList.toArray(dtoa);
        return dtoa;
    }

    private void getConceptsFromGids(ArrayList list, StringBuffer gidStr) throws SQLException {
        String query = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_CONCEPTS_FROM_GIDS);
        query = subsetDAO.getStatement(query, 1, gidStr.toString());
        Statement getConStmt = null;
        try {
            getConStmt = this.conn.createStatement();
            ResultSet rs = getConStmt.executeQuery(query);
            while (rs.next()) {
                int concept_id = rs.getInt(1);
                String concept_code = rs.getString(2);
                String concept_name = rs.getString(3);
                int namespaceId = rs.getInt(4);
                DTSTransferObject dto = new DTSTransferObject(concept_id, concept_code, concept_name, namespaceId);
                list.add(dto);
            }
            rs.close();
        } finally {
            if (getConStmt != null) {
                getConStmt.close();
            }
        }
    }

    /**
   * Computes the expression the stores the concepts of the subset tables
   *
   * @param item
   * @param dag
   * @param persistToDb
   * @return
   * @throws SQLException
   * @throws DTSValidationException
   */
    private ArrayList saveSubsetConcepts(Subset item, Dag dag, boolean persistToDb) throws SQLException, DTSValidationException {
        ExpressionBuilder eb = new ExpressionBuilder(this.conn, this.subsetDAO);
        ArrayList results = eb.buildExpression(item, dag, persistToDb);
        return results;
    }

    /**
   * Saves the subset info to the dts_subset_ref table
   *
   * @param item
   * @param dag
   * @throws SQLException
   * @throws DTSValidationException
   */
    private void saveSubsetRefInfo(Subset item, Dag dag) throws SQLException, DTSValidationException {
        ExpressionBuilder eb = new ExpressionBuilder(this.conn, this.subsetDAO);
        eb.parseExpression(dag);
        deleteSubsetRefInfo(item.getId(), SUBSET_REF_NAMESPACE_ITEM_TYPE, DELETE_SUBSET_REF);
        ArrayList namespaces = eb.getNamespaces();
        for (int i = 0; i < namespaces.size(); i++) {
            int namespaceId = ((Integer) namespaces.get(i)).intValue();
            this.addSubsetRefInfo(item.getId(), namespaceId, SUBSET_REF_NAMESPACE_ITEM_TYPE);
        }
        Categories.dataDb().debug("**** Subset [" + item.getName() + "] ref. info refreshed. ****");
    }

    private void addSubsetRefInfo(int subsetId, int refItemId, String refItemType) throws SQLException {
        if (this.addSubsetRefPs == null) {
            String refInsertQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, ADD_SUBSET_REF);
            this.addSubsetRefPs = this.conn.prepareStatement(refInsertQuery);
        }
        addSubsetRefPs.setInt(1, subsetId);
        addSubsetRefPs.setString(2, refItemType);
        addSubsetRefPs.setInt(3, refItemId);
        addSubsetRefPs.executeUpdate();
    }

    public boolean refreshSubsetHeirarchyOld(int subsetId) throws SQLException {
        long[] subsetGids = getSubsetConceptGids(subsetId);
        int[] namespaces = getSubsetNamespaces(subsetId);
        boolean hierarchyRefreshed = false;
        if (namespaces != null && (namespaces.length > 0)) {
            for (int n = 0; n < namespaces.length; n++) {
                int namespaceId = namespaces[n];
                ArrayList gids = getDirectSups(namespaceId);
                if (gids != null && gids.size() > 0) {
                    ArrayList hierGids = computeDag(gids, subsetGids);
                    addSubsetHeirarchyData(subsetId, namespaceId, hierGids);
                    if (hierGids.size() > 0) {
                        hierarchyRefreshed = true;
                    }
                }
            }
        }
        updateSubsetLoadDate(subsetId, System.currentTimeMillis());
        return hierarchyRefreshed;
    }

    /**
   * This method refreshed the subset heirarchy in the following manner:
   * - Gets the list of subset concepts
   * - Gets the list of entries in DIRECT_SUPS_PLUS for the linked namespace
   * - Computes the heirachy using the SubDagExtractor
   * - Adds the results of (3) into the DTS_SUBSET_HEIRARCHY_DATA
   * @param subsetId
   * @return true if the heirarchy data was successfully added
   * @throws SQLException
   */
    public boolean build(int subsetId, DTSPermission permit) throws IOException, SQLException, DTSValidationException, PermissionException {
        boolean hierarchyRefreshed = false;
        long beg = System.currentTimeMillis();
        Subset item = this.getSubset(subsetId);
        boolean buildOwnerFlag = false;
        try {
            synchronized (subsetIdBuildMap) {
                Long buildStart = (Long) subsetIdBuildMap.get(new Integer(subsetId));
                if (buildStart != null) {
                    String msg = "Subset [" + item.getName() + "] is being built by another process since \n" + SimpleDateFormat.getDateTimeInstance().format(new Date(buildStart.longValue()));
                    Categories.dataDb().info(msg + "\nThis request for Subset build cannot be completed.");
                    throw new DTSValidationException(msg + "\nPlease try again after some time.\n");
                } else {
                    Categories.dataDb().debug("*** Adding subset[" + item.getName() + "] to the cache list...");
                    subsetIdBuildMap.put(new Integer(subsetId), new Long(System.currentTimeMillis()));
                    buildOwnerFlag = true;
                }
            }
            hierarchyRefreshed = this.buildSubset(item);
        } finally {
            synchronized (subsetIdBuildMap) {
                if (buildOwnerFlag) {
                    Categories.dataDb().debug("*** Removing subset[" + item.getName() + "] from the cache list...");
                    subsetIdBuildMap.remove(new Integer(subsetId));
                }
            }
        }
        long end = System.currentTimeMillis();
        Categories.dataDb().debug("Subset [" + subsetId + "] hierarchy refreshed in [" + (end - beg) / 1000.0 + "] secs.");
        return hierarchyRefreshed;
    }

    private boolean buildSubset(Subset item) throws IOException, SQLException, DTSValidationException {
        boolean hierarchyRefreshed = false;
        int subsetId = item.getId();
        validateExpression(item.getExpression());
        DefaultMutableTreeNode node = SETreeNodeConstructor.getSETreeNode(item.getExpression());
        Dag dag = TreeDagTranslator.translateTreeToDag(node);
        updateSubsetLoadDate(subsetId, 0);
        saveSubsetConcepts(item, dag, true);
        long[] subsetGids = getSubsetConceptGids(subsetId);
        int[] namespaces = getSubsetNamespaces(subsetId);
        int namespaceId = -1;
        int baseNamespaceId = -1;
        String namespaceType = "";
        ArrayList gids = new ArrayList(100);
        if (namespaces != null && (namespaces.length > 0)) {
            if (namespaces.length == 2) {
                String nspType = getNamespaceType(namespaces[0]);
                if (nspType.equalsIgnoreCase("E")) {
                    namespaceId = namespaces[0];
                    baseNamespaceId = namespaces[1];
                } else {
                    namespaceId = namespaces[1];
                    baseNamespaceId = namespaces[0];
                }
                namespaceType = "E";
                gids = getDirectSups(namespaceId);
                ArrayList gids2 = new ArrayList(200);
                gids2 = getDirectSups(baseNamespaceId);
                for (int i = 0; i < gids2.size(); i++) {
                    long[] gidArr2 = (long[]) gids2.get(i);
                    gids.add(gidArr2);
                }
            } else if (namespaces.length == 1) {
                namespaceId = namespaces[0];
                namespaceType = getNamespaceType(namespaces[0]);
                gids = getDirectSups(namespaceId);
                if (namespaceType.equalsIgnoreCase("E")) {
                    baseNamespaceId = getLinkedNamespace(namespaces[0]);
                    ArrayList gids2 = new ArrayList(200);
                    gids2 = getDirectSups(baseNamespaceId);
                    for (int i = 0; i < gids2.size(); i++) {
                        long[] gidArr2 = (long[]) gids2.get(i);
                        gids.add(gidArr2);
                    }
                }
            }
            deleteSubsetInfo(subsetId, DELETE_SUBSET_HIERARCHY);
            if (gids != null && gids.size() > 0) {
                ArrayList hierGids = computeDag(gids, subsetGids);
                addSubsetHeirarchyData(subsetId, namespaceId, hierGids);
                if (hierGids.size() > 0) {
                    hierarchyRefreshed = true;
                }
                if (namespaceType.equalsIgnoreCase("E")) {
                    DirectSupPlusBuilder dspb = new DirectSupPlusBuilder(this.conn, namespaceId, baseNamespaceId);
                    dspb.buildSups();
                }
            } else {
                hierarchyRefreshed = true;
            }
        }
        updateSubsetLoadDate(subsetId, System.currentTimeMillis());
        return hierarchyRefreshed;
    }

    private void validateExpression(String expression) throws IOException, SQLException, DTSValidationException {
        DefaultMutableTreeNode node = SETreeNodeConstructor.getSETreeNode(expression);
        Dag dag = TreeDagTranslator.translateTreeToDag(node);
        ExpressionBuilder eb = new ExpressionBuilder(this.conn, this.subsetDAO);
        eb.validateExpression(dag);
    }

    private long[] getSubsetConceptGids(int subsetId) throws SQLException {
        PreparedStatement getSubConsStmt = null;
        try {
            String getSubConQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_SUBSET_CONCEPTS_GID);
            getSubConsStmt = this.conn.prepareStatement(getSubConQuery);
            getSubConsStmt.setInt(1, subsetId);
            ResultSet rs = getSubConsStmt.executeQuery();
            ArrayList al = new ArrayList();
            while (rs.next()) {
                long conceptGid = rs.getLong(1);
                al.add(new Long(conceptGid));
            }
            rs.close();
            long[] gids = new long[al.size()];
            for (int i = 0; i < al.size(); i++) {
                gids[i] = ((Long) al.get(i)).longValue();
            }
            return gids;
        } finally {
            if (getSubConsStmt != null) {
                getSubConsStmt.close();
            }
        }
    }

    public int[] getSubsetNamespaces(String user, int subsetId) throws SQLException {
        return this.getSubsetNamespaces(subsetId);
    }

    private int[] getSubsetNamespaces(int subsetId) throws SQLException {
        PreparedStatement getSubRefStmt = null;
        try {
            String getSubRefQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_SUBSET_REF_ITEM_ID);
            getSubRefStmt = this.conn.prepareStatement(getSubRefQuery);
            getSubRefStmt.setInt(1, subsetId);
            getSubRefStmt.setString(2, SUBSET_REF_NAMESPACE_ITEM_TYPE);
            ResultSet rs = getSubRefStmt.executeQuery();
            ArrayList al = new ArrayList();
            while (rs.next()) {
                int namespaceId = rs.getInt(1);
                al.add(new Integer(namespaceId));
            }
            rs.close();
            int[] nsps = new int[al.size()];
            for (int i = 0; i < al.size(); i++) {
                nsps[i] = ((Integer) al.get(i)).intValue();
            }
            return nsps;
        } finally {
            if (getSubRefStmt != null) {
                getSubRefStmt.close();
            }
        }
    }

    private ArrayList getDirectSups(int namespaceId) throws SQLException {
        PreparedStatement getSubConsStmt = null;
        try {
            Categories.dataDb().debug("Getting DTS_DIRECT_SUPS data now....");
            long beg = System.currentTimeMillis();
            String getSubConQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_DIRECT_SUPS);
            getSubConQuery = subsetDAO.getStatement(getSubConQuery, 1, String.valueOf(namespaceId));
            Categories.dataDb().debug("Querying DTS_DIRECT_SUPS : \n" + getSubConQuery);
            getSubConsStmt = this.conn.prepareStatement(getSubConQuery);
            ResultSet rs = getSubConsStmt.executeQuery();
            ArrayList al = new ArrayList();
            int counter = 0;
            while (rs.next()) {
                long con = rs.getLong(1);
                long sup = rs.getLong(2);
                long[] entry = new long[2];
                entry[0] = con;
                entry[1] = sup;
                al.add(entry);
                if ((counter++ % 5000) == 0) {
                    Categories.dataDb().debug("Retreived " + counter + " rows ....");
                }
            }
            rs.close();
            long end = System.currentTimeMillis();
            Categories.dataDb().debug("Retreived " + counter + " rows in " + (end - beg) / 1000 + " secs");
            return al;
        } finally {
            if (getSubConsStmt != null) {
                getSubConsStmt.close();
            }
        }
    }

    private ArrayList getExtDirectSups(int extNamespaceId, int baseNamespaceId) throws SQLException {
        PreparedStatement getSubConsStmt = null;
        try {
            Categories.dataDb().debug("Getting extension DTS_DIRECT_SUPS data now....");
            long beg = System.currentTimeMillis();
            String getSubConQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_EXTENSION_DIRECT_SUPS);
            getSubConQuery = subsetDAO.getStatement(getSubConQuery, 1, String.valueOf(extNamespaceId));
            getSubConQuery = subsetDAO.getStatement(getSubConQuery, 2, String.valueOf(baseNamespaceId));
            long[] limits = new long[2];
            limits = GidGenerator.getNamespaceMaxAndMinGID(extNamespaceId, false);
            getSubConQuery = subsetDAO.getStatement(getSubConQuery, 5, String.valueOf(limits[0]));
            getSubConQuery = subsetDAO.getStatement(getSubConQuery, 6, String.valueOf(limits[1]));
            limits = new long[2];
            limits = GidGenerator.getNamespaceMaxAndMinGID(baseNamespaceId, false);
            getSubConQuery = subsetDAO.getStatement(getSubConQuery, 3, String.valueOf(limits[0]));
            getSubConQuery = subsetDAO.getStatement(getSubConQuery, 4, String.valueOf(limits[1]));
            Categories.dataDb().debug("Querying extension DTS_DIRECT_SUPS : \n" + getSubConQuery);
            getSubConsStmt = this.conn.prepareStatement(getSubConQuery);
            ResultSet rs = getSubConsStmt.executeQuery();
            ArrayList al = new ArrayList();
            int counter = 0;
            while (rs.next()) {
                long con = rs.getLong(1);
                long sup = rs.getLong(2);
                long[] entry = new long[2];
                entry[0] = con;
                entry[1] = sup;
                al.add(entry);
                if ((counter++ % 5000) == 0) {
                    Categories.dataDb().debug("Retreived " + counter + " rows ....");
                }
            }
            rs.close();
            long end = System.currentTimeMillis();
            Categories.dataDb().debug("Retreived " + counter + " rows in " + (end - beg) / 1000 + " secs");
            return al;
        } finally {
            if (getSubConsStmt != null) {
                getSubConsStmt.close();
            }
        }
    }

    private ArrayList computeDag(ArrayList gids, long[] subsetGids) {
        Categories.dataDb().debug("Computing dag now ....");
        long beg = System.currentTimeMillis();
        SubDagExtractor sde = new SubDagExtractor(gids, subsetGids);
        ArrayList hierGids = sde.extractDag();
        long end = System.currentTimeMillis();
        Categories.dataDb().debug("Computed dag [size=" + hierGids.size() + "] in " + (end - beg) / 1000 + " secs");
        return hierGids;
    }

    private void addSubsetHeirarchyData(int subsetId, int namespaceId, ArrayList gids) throws SQLException {
        if (this.addSubsetHierPs == null) {
            String addSubsStmt = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, ADD_SUBSET_HIERARCHY);
            this.addSubsetHierPs = this.conn.prepareStatement(addSubsStmt);
        }
        String version = getLastestVersion(namespaceId);
        int subHierId = getSubsetIdFromSeq(this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, "GET_SUBSET_HIER_SEQ_ID_NAME"));
        addSubsetHierPs.setInt(1, subHierId);
        addSubsetHierPs.setInt(2, subsetId);
        addSubsetHierPs.setInt(3, namespaceId);
        addSubsetHierPs.setString(4, version);
        addSubsetHierPs.executeUpdate();
        if (this.addSubsetHierDataPs == null) {
            String addSubsStmt = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, ADD_SUBSET_HIERARCHY_DATA);
            this.addSubsetHierDataPs = this.conn.prepareStatement(addSubsStmt);
            int batchSize = 1000;
            if (SQL.getConnType(conn).equals(SQL.ORACLE)) {
                ((oracle.jdbc.driver.OraclePreparedStatement) addSubsetHierDataPs).setExecuteBatch(batchSize);
            }
        }
        long beg = System.currentTimeMillis();
        for (int i = 0; i < gids.size(); i++) {
            long[] consup = (long[]) gids.get(i);
            long con = consup[0];
            long sup = consup[1];
            addSubsetHierDataPs.setLong(1, subHierId);
            addSubsetHierDataPs.setLong(2, con);
            addSubsetHierDataPs.setLong(3, sup);
            addSubsetHierDataPs.executeUpdate();
        }
        this.conn.commit();
        long end = System.currentTimeMillis();
        Categories.dataDb().debug("Added subset hierarchy info [size=" + gids.size() + "] in " + (end - beg) / 1000 + " secs");
    }

    private boolean deleteSubsetRefInfo(int subsetId, String refItemType, String statement) throws SQLException {
        PreparedStatement ps = null;
        try {
            String deleteStmt = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, statement);
            ps = this.conn.prepareStatement(deleteStmt);
            ps.setInt(1, subsetId);
            ps.setString(2, refItemType);
            int row = ps.executeUpdate();
            if (row > 0) {
                return true;
            } else {
                return false;
            }
        } finally {
            if (ps != null) {
                ps.close();
            }
            this.conn.commit();
        }
    }

    private boolean deleteSubsetInfo(int subsetId, String statement) throws SQLException {
        PreparedStatement ps = null;
        try {
            String deleteStmt = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, statement);
            ps = this.conn.prepareStatement(deleteStmt);
            ps.setInt(1, subsetId);
            int row = ps.executeUpdate();
            if (row > 0) {
                return true;
            } else {
                return false;
            }
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    public Subset getSubset(int subsetId) throws SQLException {
        if (this.getSubsetPs == null) {
            String getSubsetQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_SUBSET);
            this.getSubsetPs = this.conn.prepareStatement(getSubsetQuery);
        }
        getSubsetPs.setInt(1, subsetId);
        return fetchSubset(getSubsetPs);
    }

    public Subset getSubset(String name) throws SQLException {
        if (this.getSubsetByNamePs == null) {
            String getSubsetQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_SUBSET_BY_NAME);
            this.getSubsetByNamePs = this.conn.prepareStatement(getSubsetQuery);
        }
        getSubsetByNamePs.setString(1, name);
        return fetchSubset(getSubsetByNamePs);
    }

    private Subset fetchSubset(PreparedStatement ps) throws SQLException {
        ResultSet rs = ps.executeQuery();
        Subset si = null;
        while (rs.next()) {
            int subset_id = rs.getInt(1);
            String name = rs.getString(2);
            String exp = getSubsetGeneralDao().getSubsetExpression(this.conn, subset_id);
            Timestamp created_date = rs.getTimestamp(3);
            String created_by = rs.getString(4);
            Timestamp modified_date = rs.getTimestamp(5);
            String modified_by = rs.getString(6);
            Timestamp dataLoad_date = rs.getTimestamp(7);
            String description = rs.getString(8);
            si = new Subset();
            si.setId(subset_id);
            si.setName(name);
            si.setExpression(exp);
            si.setCreatedTime(created_date.getTime());
            si.setCreatedBy(created_by);
            si.setModifiedTime(modified_date.getTime());
            si.setModifiedBy(modified_by);
            si.setDataCreatedTime(dataLoad_date.getTime());
            si.setDescription(description);
            int conceptCount = this.getSubsetConceptsCount(subset_id);
            si.setConceptCount(conceptCount);
        }
        rs.close();
        return si;
    }

    public boolean updateSubsetName(int subsetId, String newName, DTSPermission permit) throws SQLException, PermissionException {
        if (this.updateSubsetNamePs == null) {
            String updateSubsStmt = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, UPDATE_SUBSET_NAME);
            this.updateSubsetNamePs = this.conn.prepareStatement(updateSubsStmt);
        }
        updateSubsetNamePs.setString(1, newName);
        updateSubsetNamePs.setInt(2, subsetId);
        int count = updateSubsetNamePs.executeUpdate();
        return (count > 0 ? true : false);
    }

    public DTSTransferObject[] getSubsetConcepts(int subsetId) throws SQLException {
        return getSubsetConcepts(subsetId, new QuerySession());
    }

    public String getSubsetConcepts(int subsetId, Element asd) throws SQLException, XMLException {
        PreparedStatement getSubConsStmt = null;
        try {
            String getSubConQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_SUBSET_CONCEPTS);
            getSubConsStmt = this.conn.prepareStatement(getSubConQuery);
            getSubConsStmt.setInt(1, subsetId);
            ResultSet rs = getSubConsStmt.executeQuery();
            return executeConceptQuery(rs, asd, false, true, -1);
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        } finally {
            if (getSubConsStmt != null) {
                getSubConsStmt.close();
            }
        }
    }

    public DTSTransferObject[] getSubsetConcepts(int subsetId, QuerySession qs) throws SQLException {
        PreparedStatement getSubConsStmt = null;
        try {
            String getSubConQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_SUBSET_CONCEPTS);
            getSubConsStmt = this.conn.prepareStatement(getSubConQuery);
            getSubConsStmt.setInt(1, subsetId);
            qs.startExecute(getSubConsStmt);
            ResultSet rs = getSubConsStmt.executeQuery();
            ArrayList al = new ArrayList(20);
            while (rs.next()) {
                String concept_name = rs.getString(2);
                int concept_id = rs.getInt(3);
                String concept_code = rs.getString(4);
                int namespaceId = rs.getInt(5);
                DTSTransferObject dto = new DTSTransferObject(concept_id, concept_code, concept_name, namespaceId);
                al.add(dto);
            }
            rs.close();
            DTSTransferObject[] dtoa = new DTSTransferObject[al.size()];
            dtoa = (DTSTransferObject[]) al.toArray(dtoa);
            return dtoa;
        } finally {
            qs.endExecute();
            if (getSubConsStmt != null) {
                getSubConsStmt.close();
            }
        }
    }

    public int getSubsetConceptsCount(int subsetId) throws SQLException {
        int count = 0;
        PreparedStatement getSubConsStmt = null;
        try {
            String getSubsetQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_SUBSET_CONCEPTS_COUNT);
            getSubConsStmt = this.conn.prepareStatement(getSubsetQuery);
            getSubConsStmt.setInt(1, subsetId);
            ResultSet rs = getSubConsStmt.executeQuery();
            while (rs.next()) {
                count = rs.getInt(1);
            }
            rs.close();
        } finally {
            if (getSubConsStmt != null) {
                getSubConsStmt.close();
            }
        }
        return count;
    }

    public Subset[] getConceptSubsets(int conceptId, int namespaceId) throws SQLException, IOException {
        ArrayList list = new ArrayList();
        if (this.getConceptSubsetPs == null) {
            String getConSubsStmt = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_CONCEPT_SUBSETS);
            this.getConceptSubsetPs = this.conn.prepareStatement(getConSubsStmt);
        }
        getConceptSubsetPs.setInt(1, conceptId);
        getConceptSubsetPs.setInt(2, namespaceId);
        ResultSet rs = this.getConceptSubsetPs.executeQuery();
        while (rs.next()) {
            int subset_id = rs.getInt(1);
            String name = rs.getString(2);
            Subset si = new Subset();
            si.setId(subset_id);
            si.setName(name);
            list.add(si);
        }
        rs.close();
        Subset[] array = new Subset[list.size()];
        list.toArray(array);
        return array;
    }

    public boolean updateSubsetExpression(int subsetId, String user, String expr, DTSPermission permit) throws SQLException, PermissionException {
        int origTransLevel = Utilities.beginTransaction(this.conn);
        try {
            Subset item = getSubset(subsetId);
            DefaultMutableTreeNode node = SETreeNodeConstructor.getSETreeNode(expr);
            Dag dag = TreeDagTranslator.translateTreeToDag(node);
            long id = this.getSubsetGeneralDao().insertSubsetExpression(this.conn, subsetId, expr);
            updateSubsetModInfo(subsetId, user, System.currentTimeMillis());
            saveSubsetRefInfo(item, dag);
            this.conn.commit();
            return (id > 0 ? true : false);
        } catch (Exception ex) {
            this.conn.rollback();
            throw new SQLException(ex.getMessage());
        } finally {
            Utilities.endTransaction(this.conn, origTransLevel);
        }
    }

    /**
   * This exports a given subset (its concepts) into the provided namespace.
   * The user supplied namespace should be empty and already saved in the
   * KB else the export doesnt work.
   *
   * Assumption here is that since the concepts come from a single namespace (for now)
   * their ids, codes, names will be unique so there is no need to do any id/code/name translation.
   */
    public boolean exportSubset(String user, int subsetId, int namespaceId, QuerySession qs, DTSPermission permit) throws SQLException, DTSValidationException, PermissionException {
        checkPermission(permit, String.valueOf(namespaceId));
        boolean exported = false;
        PreparedStatement exportSubsetNspStmt = null;
        if (!checkNamespacePresent(namespaceId)) {
            throw new DTSValidationException("Namespace [" + namespaceId + "] not present in the knowledgebase for the subset [ " + subsetId + "] to be exported to that namespace. Please create one and retry exporting subset.");
        }
        String assocSeqName = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_ASSOC_SEQ_NAME);
        int origTransLevel = Utilities.beginTransaction(this.conn);
        long beg = System.currentTimeMillis();
        try {
            HashMap assocMap = new HashMap(100);
            int count = 0;
            int assocId = 1;
            long assocGid = GidGenerator.getGID(namespaceId, assocId);
            deleteNamespaceRelated(namespaceId, assocGid, qs);
            qs.check();
            qs.setStatus("Fetching Subset Concepts ...");
            DTSTransferObject[] subsetCons = this.getSubsetConcepts(subsetId, qs);
            qs.check();
            String exportNspQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, EXPORT_SUBSET_NSP_STMT);
            exportSubsetNspStmt = this.conn.prepareStatement(exportNspQuery);
            int smallBatchSize = 1000;
            if (SQL.getConnType(conn).equals(SQL.ORACLE)) {
                ((oracle.jdbc.driver.OraclePreparedStatement) exportSubsetNspStmt).setExecuteBatch(smallBatchSize);
            }
            addSubsetAssocType(namespaceId, assocId, assocGid);
            qs.check();
            qs.setStatus("Exporting Subset Concepts to Namespace ...");
            qs.startExecute(exportSubsetNspStmt);
            for (int i = 0; i < subsetCons.length; i++) {
                DTSTransferObject dt = subsetCons[i];
                int origConNamespaceId = dt.getNamespaceId();
                long origConceptGid = GidGenerator.getGID(origConNamespaceId, dt.getId());
                int conceptId = i + 1;
                long conceptGid = GidGenerator.getGID(namespaceId, conceptId);
                String conceptCode = "C" + conceptId;
                exportSubsetNspStmt.setLong(1, conceptGid);
                exportSubsetNspStmt.setInt(2, conceptId);
                exportSubsetNspStmt.setString(3, conceptCode);
                exportSubsetNspStmt.setString(4, dt.getName());
                exportSubsetNspStmt.setInt(5, namespaceId);
                exportSubsetNspStmt.setString(6, dt.getName().toUpperCase());
                exportSubsetNspStmt.executeUpdate();
                if (assocGid > 0) {
                    Long fromCon = new Long(conceptGid);
                    if (assocMap.containsKey(fromCon)) {
                        Vector toConVec = (Vector) assocMap.get(fromCon);
                        toConVec.addElement(new Long(origConceptGid));
                        assocMap.put(fromCon, toConVec);
                    } else {
                        Vector toConVec = new Vector(2);
                        toConVec.addElement(new Long(origConceptGid));
                        assocMap.put(fromCon, toConVec);
                    }
                }
                if ((++count % 1000) == 0) {
                    Categories.dataDb().debug("Exported [" + count + "] concepts to namespace [" + namespaceId + "]  for subset_id [" + subsetId + "]...");
                }
            }
            Categories.dataDb().debug("Exported successfully [" + count + "] concepts to namespace [" + namespaceId + "]  for subset_id [" + subsetId + "].");
            qs.endExecute();
            qs.check();
            this.conn.commit();
            qs.setStatus("Adding Concept Associations ...");
            this.addAssociations(assocMap, assocGid, assocSeqName, qs);
            qs.check();
            this.conn.commit();
            qs.setStatus("Exported [" + count + "] concepts to namespace [" + namespaceId + "]");
            exported = true;
            long end = System.currentTimeMillis();
            Categories.dataDb().debug("Exported [" + count + "] concepts to namespace [" + namespaceId + "] done in " + (end - beg) / 1000 + " secs");
            return exported;
        } catch (SQLException sqle) {
            Categories.dataDb().error("Problem exporting subset_id [" + subsetId + "] to namespace [" + namespaceId + "] : " + sqle.getMessage());
            this.conn.rollback();
            throw sqle;
        } finally {
            Utilities.endTransaction(this.conn, origTransLevel);
            if (exportSubsetNspStmt != null) {
                exportSubsetNspStmt.close();
            }
        }
    }

    private void deleteNamespaceRelated(int namespaceId, long associationGid, QuerySession qs) throws SQLException {
        Categories.dataDb().debug("Cleaning up Namespace[" + namespaceId + "]...");
        Statement deleteStmt = null;
        long beg = System.currentTimeMillis();
        try {
            String deleteAssocsStmtQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, DELETE_CONCEPT_ASSOCIATIONS);
            deleteAssocsStmtQuery = this.subsetDAO.getStatement(deleteAssocsStmtQuery, 1, String.valueOf(associationGid));
            deleteStmt = this.conn.createStatement();
            qs.startExecute(deleteStmt);
            int rows = deleteStmt.executeUpdate(deleteAssocsStmtQuery);
            long end = System.currentTimeMillis();
            qs.endExecute();
            Categories.dataDb().debug("Before exporting deleted [" + rows + "] concept associations from namespace [" + namespaceId + "] in " + (end - beg) / 1000 + " secs");
            beg = System.currentTimeMillis();
            String deleteAssocTypeStmtQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, DELETE_ASSOCIATION_TYPE);
            deleteAssocTypeStmtQuery = this.subsetDAO.getStatement(deleteAssocTypeStmtQuery, 1, String.valueOf(namespaceId));
            deleteAssocTypeStmtQuery = this.subsetDAO.getStatement(deleteAssocTypeStmtQuery, 2, String.valueOf(associationGid));
            deleteStmt = this.conn.createStatement();
            qs.startExecute(deleteStmt);
            rows = deleteStmt.executeUpdate(deleteAssocTypeStmtQuery);
            qs.endExecute();
            end = System.currentTimeMillis();
            Categories.dataDb().debug("Before exporting deleted [" + rows + "] association type from namespace [" + namespaceId + "] in " + (end - beg) / 1000 + " secs");
            qs.setStatus("Deleting Concepts in the namespace ...");
            beg = System.currentTimeMillis();
            String deleteConsStmtQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, DELETE_CONCEPTS);
            deleteConsStmtQuery = this.subsetDAO.getStatement(deleteConsStmtQuery, 1, String.valueOf(namespaceId));
            deleteStmt = this.conn.createStatement();
            qs.startExecute(deleteStmt);
            rows = deleteStmt.executeUpdate(deleteConsStmtQuery);
            qs.endExecute();
            end = System.currentTimeMillis();
            Categories.dataDb().debug("Before exporting deleted [" + rows + "] concepts from namespace [" + namespaceId + "] in " + (end - beg) / 1000 + " secs");
        } finally {
            qs.endExecute();
            if (deleteStmt != null) {
                deleteStmt.close();
            }
        }
    }

    private long addSubsetAssocType(int namespaceId, int assocId, long assocGid) throws SQLException {
        PreparedStatement addAssocTypeStmt = null;
        try {
            String addAssocTypeStmtQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, ADD_ASSOCIATION_TYPE);
            addAssocTypeStmt = this.conn.prepareStatement(addAssocTypeStmtQuery);
            String assocName = "Subset Mapping [" + namespaceId + "]";
            String assocCode = "C" + assocId;
            addAssocTypeStmt.setLong(1, assocGid);
            addAssocTypeStmt.setInt(2, assocId);
            addAssocTypeStmt.setString(3, assocCode);
            addAssocTypeStmt.setString(4, assocName);
            addAssocTypeStmt.setInt(5, namespaceId);
            addAssocTypeStmt.setString(6, "C");
            addAssocTypeStmt.setString(7, "M");
            addAssocTypeStmt.setString(8, assocName);
            int rows = addAssocTypeStmt.executeUpdate();
            if (rows < 1) {
                Categories.dataDb().error("Problem adding Association Type [" + assocName + "]");
            }
        } finally {
            if (addAssocTypeStmt != null) {
                addAssocTypeStmt.close();
            }
        }
        return assocGid;
    }

    private void addAssociations(HashMap assocMap, long associationGid, String assocSeqName, QuerySession qs) throws SQLException {
        Categories.dataDb().debug("Adding associations ...");
        if (addAssociationStmt == null) {
            String addAssocStmt = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, ADD_ASSOCIATIONS);
            addAssociationStmt = this.conn.prepareStatement(addAssocStmt);
            int smallBatchSize = 1000;
            if (SQL.getConnType(conn).equals(SQL.ORACLE)) {
                ((oracle.jdbc.driver.OraclePreparedStatement) addAssociationStmt).setExecuteBatch(smallBatchSize);
            }
        }
        qs.startExecute(addAssociationStmt);
        int count = 0;
        Iterator keys = assocMap.keySet().iterator();
        while (keys.hasNext()) {
            Long fromCon = (Long) keys.next();
            Vector toCons = (Vector) assocMap.get(fromCon);
            for (int i = 0; i < toCons.size(); i++) {
                Long toCon = (Long) toCons.elementAt(i);
                addAssociationStmt.setLong(1, fromCon.longValue());
                addAssociationStmt.setLong(2, associationGid);
                addAssociationStmt.setLong(3, toCon.longValue());
                if (assocSeqName != null && assocSeqName.length() > 0) {
                    long iid = this.getInstanceId(assocSeqName, IIDGenerator.getNamespaceIdFromGid(associationGid));
                    addAssociationStmt.setLong(4, iid);
                }
                addAssociationStmt.executeUpdate();
                if ((++count % 1000) == 0) {
                    Categories.dataDb().debug("Added [" + count + "] associations...");
                }
            }
        }
        qs.endExecute();
        Categories.dataDb().debug("Added [" + count + "] associations...");
    }

    private void addAssociation(long fromConceptGid, long associationGid, long toConceptGid, String assocSeqName) throws SQLException {
        if (addAssociationStmt == null) {
            String addAssocStmt = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, ADD_ASSOCIATIONS);
            addAssociationStmt = this.conn.prepareStatement(addAssocStmt);
        }
        addAssociationStmt.setLong(1, fromConceptGid);
        addAssociationStmt.setLong(2, associationGid);
        addAssociationStmt.setLong(3, toConceptGid);
        if (assocSeqName != null && assocSeqName.length() > 0) {
            long iid = this.getInstanceId(assocSeqName, IIDGenerator.getNamespaceIdFromGid(associationGid));
            addAssociationStmt.setLong(4, iid);
        }
        addAssociationStmt.executeUpdate();
    }

    private ArrayList addPropertyTypes(DTSPropTypeObject[] propTypes, int namespaceId) throws SQLException {
        PreparedStatement addPropTypeStmt = null;
        ArrayList added = new ArrayList();
        try {
            String addPropTypeStmtQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, ADD_PROPERTY_TYPE);
            addPropTypeStmt = this.conn.prepareStatement(addPropTypeStmtQuery);
            for (int i = 0; i < propTypes.length; i++) {
                DTSPropTypeObject pt = propTypes[i];
                long propGid = GidGenerator.getGID(namespaceId, pt.getId());
                addPropTypeStmt.setLong(1, propGid);
                addPropTypeStmt.setInt(2, pt.getId());
                addPropTypeStmt.setString(3, pt.getCode());
                addPropTypeStmt.setString(4, pt.getName());
                addPropTypeStmt.setInt(5, namespaceId);
                String val = pt.getValueSize();
                if (val != null) {
                    addPropTypeStmt.setString(6, val);
                } else {
                    addPropTypeStmt.setNull(6, Types.VARCHAR);
                }
                val = pt.getAttachesTo();
                if (val != null) {
                    addPropTypeStmt.setString(7, val);
                } else {
                    addPropTypeStmt.setNull(7, Types.VARCHAR);
                }
                val = pt.getContainsIndex();
                if (val != null) {
                    addPropTypeStmt.setString(8, val);
                } else {
                    addPropTypeStmt.setNull(8, Types.VARCHAR);
                }
                int rows = addPropTypeStmt.executeUpdate();
                if (rows < 1) {
                    Categories.dataDb().error("Problem adding Property Type [" + pt.getName() + "]");
                } else {
                    added.add(pt);
                }
            }
            this.conn.commit();
        } finally {
            if (addPropTypeStmt != null) {
                addPropTypeStmt.close();
            }
        }
        return added;
    }

    private void addPropertyValues(DTSPropTypeObject[] propTypes, int subsetId, int namespaceId) throws SQLException {
        PreparedStatement getIndexPropStmt = null;
        PreparedStatement getSearchPropStmt = null;
        PreparedStatement addIndexPropStmt = null;
        PreparedStatement addSearchPropStmt = null;
        String indexSeqName = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_INDEX_SEQ_NAME);
        String searchSeqName = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_SEARCH_SEQ_NAME);
        ;
        try {
            for (int i = 0; i < propTypes.length; i++) {
                DTSPropTypeObject pt = propTypes[i];
                String size = pt.getValueSize();
                if (!size.equalsIgnoreCase("B")) {
                    if (size.equalsIgnoreCase("I")) {
                        if (getIndexPropStmt == null) {
                            String addIndexPropQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_SUBSET_CONCEPT_INDEX_PROPERTY);
                            getIndexPropStmt = this.conn.prepareStatement(addIndexPropQuery);
                        }
                        if (addIndexPropStmt == null) {
                            String addIndexPropQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, ADD_INDEX_PROPERTY_VALUE);
                            addIndexPropStmt = this.conn.prepareStatement(addIndexPropQuery);
                        }
                        addProperties(pt, subsetId, namespaceId, getIndexPropStmt, addIndexPropStmt, indexSeqName);
                    } else {
                        if (getSearchPropStmt == null) {
                            String addSearchPropQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_SUBSET_CONCEPT_SEARCH_PROPERTY);
                            getSearchPropStmt = this.conn.prepareStatement(addSearchPropQuery);
                        }
                        if (addSearchPropStmt == null) {
                            String addSearchPropQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, ADD_SEARCH_PROPERTY_VALUE);
                            addSearchPropStmt = this.conn.prepareStatement(addSearchPropQuery);
                        }
                        addProperties(pt, subsetId, namespaceId, getSearchPropStmt, addSearchPropStmt, searchSeqName);
                    }
                } else {
                    Categories.dataDb().warn("Skipping Big Property Type [" + pt.getName() + "]");
                }
            }
            this.conn.commit();
        } finally {
            this.closeStatements(new Statement[] { getIndexPropStmt, getSearchPropStmt, addIndexPropStmt, addSearchPropStmt });
        }
    }

    private void addProperties(DTSPropTypeObject pt, int subsetId, int namespaceId, PreparedStatement getPropStmt, PreparedStatement addPropStmt, String indexSeqName) throws SQLException {
        long propGid = GidGenerator.getGID(pt.getNamespaceId(), pt.getId());
        getPropStmt.setLong(1, propGid);
        getPropStmt.setInt(2, subsetId);
        ResultSet rs = getPropStmt.executeQuery();
        int count = 0;
        while (rs.next()) {
            int conId = rs.getInt(1);
            String propValue = rs.getString(2);
            long conGid = GidGenerator.getGID(namespaceId, conId);
            long newPropGid = GidGenerator.getGID(namespaceId, pt.getId());
            addPropStmt.setLong(1, conGid);
            addPropStmt.setLong(2, newPropGid);
            addPropStmt.setString(3, propValue);
            addPropStmt.setString(4, propValue);
            if (indexSeqName != null && indexSeqName.length() > 0) {
                long iid = this.getInstanceId(indexSeqName, namespaceId);
                addPropStmt.setLong(5, iid);
            }
            addPropStmt.executeUpdate();
            if ((++count % 200) == 0) {
                this.conn.commit();
            }
        }
        rs.close();
        this.conn.commit();
    }

    private boolean updateSubsetLoadDate(int subsetId, long loadTime) throws SQLException {
        if (updateSubsetLoadDateStmt == null) {
            String updateSubLoadDateoStmt = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, UPDATE_SUBSET_LOAD_DATE);
            updateSubsetLoadDateStmt = this.conn.prepareStatement(updateSubLoadDateoStmt);
        }
        updateSubsetLoadDateStmt.setTimestamp(1, new Timestamp(loadTime));
        updateSubsetLoadDateStmt.setInt(2, subsetId);
        int rows = updateSubsetLoadDateStmt.executeUpdate();
        return (rows > 0 ? true : false);
    }

    private boolean updateSubsetModInfo(int subsetId, String modifiedBy, long modifiedTime) throws SQLException {
        if (updateSubStmt == null) {
            String updateSubModInfoStmt = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, UPDATE_SUBSET_MOD_INFO);
            updateSubStmt = this.conn.prepareStatement(updateSubModInfoStmt);
        }
        updateSubStmt.setString(1, modifiedBy);
        updateSubStmt.setTimestamp(2, new Timestamp(modifiedTime));
        updateSubStmt.setInt(3, subsetId);
        int rows = updateSubStmt.executeUpdate();
        return (rows > 0 ? true : false);
    }

    private boolean checkNamespacePresent(int namespaceId) throws SQLException {
        boolean namespaceFound = false;
        Statement checkNspStmt = null;
        try {
            String checkNspQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, CHECK_NAMESPACE);
            checkNspQuery = this.subsetDAO.getStatement(checkNspQuery, 1, String.valueOf(namespaceId));
            checkNspStmt = this.conn.createStatement();
            ResultSet rs = checkNspStmt.executeQuery(checkNspQuery);
            while (rs.next()) {
                namespaceFound = true;
            }
            rs.close();
        } finally {
            if (checkNspStmt != null) {
                checkNspStmt.close();
            }
        }
        return namespaceFound;
    }

    public boolean deleteSubset(int subsetId, DTSPermission permit) throws SQLException, PermissionException {
        if (this.delSubsetPs == null) {
            String delSubsStmt = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, DELETE_SUBSET);
            this.delSubsetPs = this.conn.prepareStatement(delSubsStmt);
        }
        this.delSubsetPs.setInt(1, subsetId);
        int count = this.delSubsetPs.executeUpdate();
        return (count > 0 ? true : false);
    }

    public boolean updateSubset(String user, SubsetUpdate updateItem, DTSPermission permit) throws SQLException, DTSValidationException, PermissionException {
        Subset oldItem = this.getSubset(updateItem.getId());
        long time = oldItem.getModifiedTime();
        boolean exprModified = false;
        if (updateItem.getExpression() != null) {
            if (!oldItem.getExpression().equals(updateItem.getExpression())) {
                time = System.currentTimeMillis();
                exprModified = true;
            }
        }
        String name = oldItem.getName();
        if (updateItem.getName() != null) {
            name = updateItem.getName();
        }
        String desc = oldItem.getDescription();
        if (updateItem.getDescription() != null) {
            desc = updateItem.getDescription();
        }
        if (this.updateSubsetStmt == null) {
            String updateSubsStmt = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, UPDATE_SUBSET);
            this.updateSubsetStmt = this.conn.prepareStatement(updateSubsStmt);
        }
        updateSubsetStmt.setString(1, user);
        updateSubsetStmt.setTimestamp(2, new Timestamp(time));
        updateSubsetStmt.setString(3, name);
        updateSubsetStmt.setString(4, desc);
        updateSubsetStmt.setInt(5, updateItem.getId());
        int count = this.updateSubsetStmt.executeUpdate();
        if (exprModified) {
            this.updateSubsetExpression(updateItem.getId(), user, updateItem.getExpression(), permit);
        }
        return (count > 0 ? true : false);
    }

    private int getSubsetIdFromSeq() throws SQLException {
        int id = -1;
        if (getSubsetIdSeq == null) {
            getSubsetIdSeq = this.conn.createStatement();
        }
        String connType = SQL.getConnType(this.conn);
        if (connType.equals(SQL.ORACLE)) {
            ResultSet rs = getSubsetIdSeq.executeQuery(this.subsetDAO.getStatement(TABLE_KEY, SUBSET_ID_SEQUENCE));
            while (rs.next()) {
                id = rs.getInt(1);
            }
            rs.close();
        } else if (connType.equals(SQL.SQL2K) || connType.equals(SQL.CACHE)) {
            id = (int) this.getInstanceId(DTS_SUBSET_SEQ_NAME, 1);
        }
        return id;
    }

    private int getSubsetIdFromSeq(String seqName) throws SQLException {
        return (int) this.getInstanceId(seqName, 1);
    }

    private String getLastestVersion(int nameSpaceId) throws SQLException {
        java.sql.Date latestDate = null;
        String latestVersion = "";
        Statement st = null;
        try {
            st = this.conn.createStatement();
            String query = "select name, release_date from DTS_VERSION where namespace_id = " + nameSpaceId + " AND name NOT LIKE 'NOT_RETIRED'";
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                String version = rs.getString(1);
                java.sql.Date date = rs.getDate(2);
                if (latestDate == null) {
                    latestVersion = version;
                    latestDate = date;
                } else {
                    int result = latestDate.compareTo(date);
                    if (result < 0) {
                        latestVersion = version;
                        latestDate = date;
                    }
                }
            }
        } finally {
            if (st != null) {
                st.close();
            }
        }
        Categories.dataDb().debug("Found latest version for namespaceId[" + nameSpaceId + "] = " + latestVersion);
        return latestVersion;
    }

    private String getNamespaceType(int namespaceId) throws SQLException {
        String namespaceType = "";
        if (getNspType == null) {
            String getNspTypeQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_NAMESPACE_TYPE);
            getNspType = this.conn.prepareStatement(getNspTypeQuery);
        }
        ResultSet rs = null;
        try {
            getNspType.setInt(1, namespaceId);
            rs = getNspType.executeQuery();
            while (rs.next()) {
                namespaceType = rs.getString(1);
            }
            rs.close();
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
        return namespaceType;
    }

    private int getLinkedNamespace(int extNamespaceId) throws SQLException {
        int baseNamespace = -1;
        if (getLinkNsp == null) {
            String getLinkNspQuery = this.subsetDAO.getStatement(SubsetDb.TABLE_KEY, GET_LINK_NAMESPACE);
            getLinkNsp = this.conn.prepareStatement(getLinkNspQuery);
        }
        ResultSet rs = null;
        try {
            getLinkNsp.setInt(1, extNamespaceId);
            rs = getLinkNsp.executeQuery();
            while (rs.next()) {
                baseNamespace = rs.getInt(1);
            }
            rs.close();
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
        return baseNamespace;
    }

    /**
   * Checks permission for the subset. Here since a subset is linked to namespace(s), a user is therefore
   * granted edit permissions on a subset if it has write permission on the subset linked namespaces.
   * @param permit
   * @param subsetId
   * @throws SQLException
   * @throws PermissionException
   */
    protected void checkSubsetPermission(DTSPermission permit, int subsetId) throws SQLException, PermissionException {
        int[] namespaceIds = getSubsetNamespaces(subsetId);
        for (int n = 0; n < namespaceIds.length; n++) {
            int namespaceId = namespaceIds[n];
            if (permit != null) {
                String accessType = permit.getAccessType();
                if (accessType.equals(DTSUsers.READ_ACCESS_TYPE)) {
                    throw new PermissionException("Sorry, you have read only permission!");
                } else if (accessType.equals(DTSUsers.INVALID_ACCESS_TYPE)) {
                    throw new PermissionException("WARNING: Invalid user!");
                } else {
                    Vector stored_namespace_ids = permit.getNamespaceIds();
                    boolean isPermitted = stored_namespace_ids.contains(String.valueOf(namespaceId));
                    if (!isPermitted) {
                        throw new PermissionException("Sorry, you don't have permission to update the subset '" + namespaceId + "' ");
                    }
                }
            } else {
                assertWrite(namespaceId);
            }
        }
    }

    private static final String GET_ALL_SUBSETS = "GET_ALL_SUBSETS";

    private static final String GET_ALL_SUBSETS_WITH_NAMESPACE_ID = "GET_ALL_SUBSETS_WITH_NAMESPACE_ID";

    private static final String GET_ALL_SUBSETS_WITH_NAME_MATCH = "GET_ALL_SUBSETS_WITH_NAME_MATCH";

    private static final String GET_SUBSET = "GET_SUBSET";

    private static final String GET_SUBSET_BY_NAME = "GET_SUBSET_BY_NAME";

    private static final String GET_SUBSET_CONCEPTS = "GET_SUBSET_CONCEPTS";

    private static final String GET_SUBSET_CONCEPTS_COUNT = "GET_SUBSET_CONCEPTS_COUNT";

    private static final String ADD_SUBSET = "ADD_SUBSET";

    private static final String ADD_SUBSET_REF = "ADD_SUBSET_REF";

    private static final String DELETE_SUBSET = "DELETE_SUBSET";

    private static final String UPDATE_SUBSET_NAME = "UPDATE_SUBSET_NAME";

    private static final String GET_DIRECT_SUPS = "GET_DIRECT_SUPS";

    private static final String GET_SUBSET_REF_ITEM_ID = "GET_SUBSET_REF_ITEM_ID";

    private static final String GET_SUBSET_CONCEPTS_GID = "GET_SUBSET_CONCEPTS_GID";

    private static final String ADD_SUBSET_HIERARCHY_DATA = "ADD_SUBSET_HIERARCHY_DATA";

    private static final String ADD_SUBSET_HIERARCHY = "ADD_SUBSET_HIERARCHY";

    private static final String DELETE_SUBSET_HIERARCHY = "DELETE_SUBSET_HIERARCHY";

    private static final String DELETE_SUBSET_REF = "DELETE_SUBSET_REF";

    private static final String CHECK_SP = "CHECK_SP";

    private static final String GET_CONCEPTS_FROM_GIDS = "GET_CONCEPTS_FROM_GIDS";

    private static final String GET_SUBSETS_DIFF = "GET_SUBSETS_DIFF";

    private static final String GET_CONCEPT_SUBSETS = "GET_CONCEPT_SUBSETS";

    private static final String SUBSET_ID_SEQUENCE = "GET_SUBSET_SEQ_ID";

    private static final String EXPORT_SUBSET_NSP_STMT = "EXPORT_SUBSET_NSP_STMT";

    private static final String CHECK_NAMESPACE = "CHECK_NAMESPACE";

    private static final String UPDATE_SUBSET_MOD_INFO = "UPDATE_SUBSET_MOD_INFO";

    private static final String ADD_PROPERTY_TYPE = "ADD_PROPERTY_TYPE";

    private static final String GET_SUBSET_CONCEPT_INDEX_PROPERTY = "GET_SUBSET_CONCEPT_INDEX_PROPERTY";

    private static final String GET_SUBSET_CONCEPT_SEARCH_PROPERTY = "GET_SUBSET_CONCEPT_SEARCH_PROPERTY";

    private static final String ADD_INDEX_PROPERTY_VALUE = "ADD_INDEX_PROPERTY_VALUE";

    private static final String ADD_SEARCH_PROPERTY_VALUE = "ADD_SEARCH_PROPERTY_VALUE";

    private static final String GET_INDEX_SEQ_NAME = "GET_INDEX_SEQ_NAME";

    private static final String GET_SEARCH_SEQ_NAME = "GET_SEARCH_SEQ_NAME";

    private static final String ADD_ASSOCIATION_TYPE = "ADD_ASSOCIATION_TYPE";

    private static final String ADD_ASSOCIATIONS = "ADD_ASSOCIATIONS";

    private static final String GET_ASSOC_SEQ_NAME = "GET_ASSOC_SEQ_NAME";

    private static final String DELETE_ASSOCIATION_TYPE = "DELETE_ASSOCIATION_TYPE";

    private static final String DELETE_CONCEPTS = "DELETE_CONCEPTS";

    private static final String DELETE_CONCEPT_ASSOCIATIONS = "DELETE_CONCEPT_ASSOCIATIONS";

    private static final String UPDATE_SUBSET = "UPDATE_SUBSET";

    private static final String UPDATE_SUBSET_LOAD_DATE = "UPDATE_SUBSET_LOAD_DATE";

    private static final String GET_NAMESPACE_TYPE = "GET_NAMESPACE_TYPE";

    private static final String GET_EXTENSION_DIRECT_SUPS = "GET_EXTENSION_DIRECT_SUPS";

    private static final String GET_LINK_NAMESPACE = "GET_LINK_NAMESPACE";

    private PreparedStatement addSubsetPs = null;

    private PreparedStatement addSubsetRefPs = null;

    private PreparedStatement addSubsetHierDataPs = null;

    private PreparedStatement addSubsetHierPs = null;

    private PreparedStatement delSubsetPs = null;

    private PreparedStatement getSubsetPs = null;

    private PreparedStatement getSubsetByNamePs = null;

    private PreparedStatement getConceptSubsetPs = null;

    private PreparedStatement updateSubsetNamePs = null;

    private PreparedStatement updateSubStmt = null;

    private PreparedStatement addAssociationStmt = null;

    private Statement getSubsetIdSeq = null;

    private PreparedStatement updateSubsetStmt = null;

    private PreparedStatement updateSubsetLoadDateStmt = null;

    private PreparedStatement getNspType = null;

    private PreparedStatement getLinkNsp = null;

    private static final String SUBSET_REF_NAMESPACE_ITEM_TYPE = "NS";

    private static final String DTS_SUBSET_SEQ_NAME = "dts_subset_id_seq";

    private static HashMap directSupsEntries = new HashMap();
}
