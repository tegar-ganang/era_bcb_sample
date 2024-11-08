package com.knowgate.hipergate;

import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.knowgate.debug.DebugFile;
import com.knowgate.jdc.JDCConnection;
import com.knowgate.dataobjs.DB;
import com.knowgate.dataobjs.DBBind;
import com.knowgate.dataobjs.DBSubset;

/**
 * Singleton manager for Categories Tree
 * @author Sergio Montoro Ten
 * @version 2.1
 */
public class Categories {

    public Categories() {
        iRootsCount = -1;
        sRootsNamedTables = DB.k_categories + " c, " + DB.k_cat_labels + " n," + DB.k_cat_root + " r";
        sRootsNamedFields = "c." + DB.gu_category + ", c." + DB.nm_category + ", " + DBBind.Functions.ISNULL + "(n." + DB.tr_category + ",''), c." + DB.nm_icon + ", c." + DB.nm_icon2;
        sRootsNamedFilter = "n." + DB.gu_category + "=c." + DB.gu_category + " AND c." + DB.gu_category + "=r." + DB.gu_category + " AND n." + DB.id_language + "=?";
        sChildNamedTables = DB.v_cat_tree_labels;
        sChildNamedFields = DB.gu_category + "," + DB.nm_category + "," + DB.tr_category + ", " + DB.nm_icon + ", " + DB.nm_icon2 + ", " + DB.gu_owner;
        sChildNamedFilter = DB.gu_parent_cat + "=? AND (" + DB.id_language + "=? OR " + DB.id_language + " IS NULL)";
    }

    /**
   * Clear root categories cache.
   * Root category names are loaded once and then cached into a static variable.
   * Use this method for forcing reload of categories from database on next call
   * to getRoots() or getRootsNamed().
   */
    public void clearCache() {
        oRootsLoaded = false;
    }

    /**
   * <p>Expand Category Childs into k_cat_expand table</p>
   * @param oConn Database Connection
   * @param sRootCategoryId GUID of Category to expand.
   * @throws SQLException
   */
    public static void expand(JDCConnection oConn, String sRootCategoryId) throws SQLException {
        Category oRoot = new Category(sRootCategoryId);
        oRoot.expand(oConn);
    }

    /**
   * <p>Get root category for a given Domain</p>
   * The root Category for a Domain will be the one such that nm_category=nm_domain
   * @param oConn Database Connection
   * @param iDomain Domain Numeric Identifier
   * @return Category GUID or <b>null</b> if root Category for Domain was not found.
   * @throws SQLException
   */
    public Category forDomain(JDCConnection oConn, int iDomain) throws SQLException {
        PreparedStatement oStmt;
        ResultSet oRSet;
        Category oRetVal;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin Categories.forDomain([Connection], " + String.valueOf(iDomain) + ")");
            DebugFile.incIdent();
            DebugFile.writeln("Connection.prepareStatement(SELECT " + DB.gu_category + " FROM " + DB.k_categories + " WHERE " + DB.nm_category + "=(SELECT " + DB.nm_domain + " FROM " + DB.k_domains + " WHERE " + DB.id_domain + "=?)");
        }
        oStmt = oConn.prepareStatement("SELECT " + DB.gu_category + " FROM " + DB.k_categories + " WHERE " + DB.nm_category + "=(SELECT " + DB.nm_domain + " FROM " + DB.k_domains + " WHERE " + DB.id_domain + "=?)", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        oStmt.setInt(1, iDomain);
        oRSet = oStmt.executeQuery();
        if (oRSet.next()) oRetVal = new Category(oConn, oRSet.getString(1)); else oRetVal = null;
        oRSet.close();
        oStmt.close();
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End Categories.forDomain() : " + (oRetVal == null ? "null" : "[Category]"));
        }
        return oRetVal;
    }

    /**
   * <p>Get root categories as a DBSubset.</p>
   * Root categories are those present at k_cat_root table.<br>
   * It is recommended to use this criteria instead of seeking those categories
   * not present as childs at k_cat_tree. Selecting from k_cat_root is much faster
   * than scanning the k_cat_tree table.
   * @param oConn Database Connection
   * @return A single column DBSubset containing th GUID of root categories.
   * @throws SQLException
   */
    public DBSubset getRoots(JDCConnection oConn) throws SQLException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin Categories.getRoots([Connection])");
            DebugFile.incIdent();
        }
        oRoots = new DBSubset(DB.k_cat_root, DB.gu_category, "", 10);
        iRootsCount = oRoots.load(oConn);
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End Categories.getRoots()");
        }
        return oRoots;
    }

    /**
   * Get root categories count.
   * @throws IllegalStateException If getRoots() or getRootsNamed() have not
   * been called prior to getRootsCount()
   */
    public int getRootsCount() throws IllegalStateException {
        if (-1 == iRootsCount) throw new IllegalStateException("Must call getRoots() or getRootsNamed() prior to getRootsCount()");
        return iRootsCount;
    }

    /**
   * <p>Get Root Caetgories and their names as a DBSubset</p>
   * Categories not having any translation at k_cat_labels will not be retrieved.<br>
   * Root Category Names are loaded once and then cached internally as a static object.<br>
   * Use clearCahce() method for refreshing root categories from database.
   * @param oConn Database Connection
   * @param sLanguage Language for category label retrieval.
   * @param iOrderBy Column for order by { ORDER_BY_NONE, ORDER_BY_NEUTRAL_NAME, ORDER_BY_LOCALE_NAME }
   * @return A DBSubset with the following columns:<br>
   * <table border=1 cellpadding=4>
   * <tr><td><b>gu_category</b></td><td><b>nm_category</b></td><td><b>tr_category</b></td><td><b>nm_icon</b></td><td><b>nm_icon2</b></td></tr>
   * <tr><td>Category GUID</td><td>Category Internal Name</td><td>Category Translated Label</td><td>Icon for Closed Folder</td><td>Icon for Opened Folder</td></tr>
   * </table>
   * @throws SQLException
   */
    public DBSubset getRootsNamed(JDCConnection oConn, String sLanguage, int iOrderBy) throws SQLException {
        sRootsNamedFields = "c." + DB.gu_category + ", c." + DB.nm_category + ", " + DBBind.Functions.ISNULL + "(n." + DB.tr_category + ",''), c." + DB.nm_icon + ", c." + DB.nm_icon2;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin Categories.getRootsNamed([Connection], " + sLanguage + String.valueOf(iOrderBy) + ")");
            DebugFile.incIdent();
        }
        if (!oRootsLoaded) {
            Object[] aLang = { sLanguage };
            if (iOrderBy > 0) oRootsNamed = new DBSubset(sRootsNamedTables, sRootsNamedFields, sRootsNamedFilter + " ORDER BY " + iOrderBy, 16); else oRootsNamed = new DBSubset(sRootsNamedTables, sRootsNamedFields, sRootsNamedFilter, 16);
            iRootsCount = oRootsNamed.load(oConn, aLang);
            oRootsLoaded = true;
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End Categories.getRootsNamed()");
        }
        return oRootsNamed;
    }

    /**
   * <p>Get first level childs for a given category.</p>
   * Categories not having any translation at k_cat_labels will not be retrieved.
   * @param oConn Database Connection
   * @param idParent Parent Category
   * @param sLanguage Language for label retrieval
   * @param iOrderBy Column for order by { ORDER_BY_NONE, ORDER_BY_NEUTRAL_NAME, ORDER_BY_LOCALE_NAME }
   * @return A DBSubset with the following columns:<br>
   * <table border=1 cellpadding=4>
   * <tr><td><b>gu_category</b></td><td><b>nm_category</b></td><td><b>tr_category</b></td><td><b>nm_icon</b></td><td><b>nm_icon2</b></td></tr>
   * <tr><td>Category GUID</td><td>Category Internal Name</td><td>Category Translated Label</td><td>Icon for Closed Folder</td><td>Icon for Opened Folder</td></tr>
   * </table>
   * @throws SQLException
   */
    public DBSubset getChildsNamed(JDCConnection oConn, String idParent, String sLanguage, int iOrderBy) throws SQLException {
        long lElapsed = 0;
        if (DebugFile.trace) {
            lElapsed = System.currentTimeMillis();
            DebugFile.writeln("Begin Categories.getChildsNamed([Connection], " + (idParent == null ? "null" : idParent) + "," + (sLanguage == null ? "null" : sLanguage) + "," + String.valueOf(iOrderBy) + ")");
            DebugFile.incIdent();
        }
        Object[] aParams = { idParent, sLanguage, idParent, idParent, sLanguage };
        DBSubset oChilds;
        if (iOrderBy > 0) oChilds = new DBSubset(sChildNamedTables, sChildNamedFields, sChildNamedFilter + " UNION SELECT " + "c." + DB.gu_category + ",c." + DB.nm_category + ",c." + DB.nm_category + "," + "c." + DB.nm_icon + ",c." + DB.nm_icon2 + ",c." + DB.gu_owner + " FROM " + DB.k_categories + " c, " + DB.k_cat_tree + " t WHERE c." + DB.gu_category + "=t." + DB.gu_child_cat + " AND " + "t." + DB.gu_parent_cat + "=? AND c." + DB.gu_category + " NOT IN " + "(SELECT " + DB.gu_category + " FROM " + sChildNamedTables + " WHERE " + sChildNamedFilter + ") ORDER BY " + iOrderBy, 32); else oChilds = new DBSubset(sChildNamedTables, sChildNamedFields, sChildNamedFilter + " UNION SELECT " + "c." + DB.gu_category + ",c." + DB.nm_category + ",c." + DB.nm_category + ", " + "c." + DB.nm_icon + ",c." + DB.nm_icon2 + ",c." + DB.gu_owner + " FROM " + DB.k_categories + " c, " + DB.k_cat_tree + " t WHERE c." + DB.gu_category + "=t." + DB.gu_child_cat + " AND " + "t." + DB.gu_parent_cat + "=? AND c." + DB.gu_category + " NOT IN " + "(SELECT " + DB.gu_category + " FROM " + sChildNamedTables + " WHERE " + sChildNamedFilter + ")", 32);
        int iChilds = oChilds.load(oConn, aParams);
        if (DebugFile.trace) {
            DebugFile.writeln(String.valueOf(iChilds) + " childs readed in " + String.valueOf(System.currentTimeMillis() - lElapsed) + " ms");
            DebugFile.decIdent();
            DebugFile.writeln("End Categories.getChildsNamed()");
        }
        return oChilds;
    }

    private DBSubset oRoots;

    private DBSubset oRootsNamed;

    private boolean oRootsLoaded;

    private int iRootsCount;

    private String sRootsNamedTables;

    private String sRootsNamedFields;

    private String sRootsNamedFilter;

    private String sChildNamedTables;

    private String sChildNamedFields;

    private String sChildNamedFilter;

    private String sChildNamedNoLang;

    public static final int ORDER_BY_NONE = 0;

    public static final int ORDER_BY_ID = 1;

    public static final int ORDER_BY_NEUTRAL_NAME = 2;

    public static final int ORDER_BY_LOCALE_NAME = 3;
}
