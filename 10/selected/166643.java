package database;

import gui.BaseProperties;
import gui.EditViewFocusManager;
import gui.SequenceBoxComponentLayer;
import java.awt.Component;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import common.Dialogs;
import common.IListItemIter;
import common.ProgressWrapper;
import common.Utils;
import database.DbColumnExt.DbColumnType;

public class DbCommon<Item extends IItem, Comp> {

    private final String m_itemName;

    private final String m_itemNameCapitalised;

    private final String m_itemNamePlural;

    private final String m_dbItemName;

    private String m_dbComponentName;

    private final DbTable m_itemTable;

    private final DbTable m_componentTable;

    private final String m_dbItemNameFieldColName;

    private final String m_dbItemIdFieldColName;

    private final String m_itemNameAbbrev;

    private final String m_compNameAbbrev;

    private final Class<Comp> m_compClass;

    private final Class<Item> m_itemClass;

    private final ICache<Item> m_itemCache;

    DbCommon(String itemName, String itemNameCapitalised, String itemNamePlural, String itemNameAbbrev, String compName, String compNamePlural, String compNameAbbrev, DbTable itemTable, DbTable componentTable, Class<Item> itemClass, Class<Comp> compClass, ICache<Item> itemCache) {
        m_itemName = itemName;
        m_itemNameCapitalised = itemNameCapitalised;
        m_itemNamePlural = itemNamePlural;
        m_itemNameAbbrev = itemNameAbbrev;
        m_compNameAbbrev = compNameAbbrev;
        m_itemTable = itemTable;
        m_componentTable = componentTable;
        m_itemClass = itemClass;
        m_compClass = compClass;
        m_itemCache = itemCache;
        m_dbItemName = itemTable.getTableName();
        if (componentTable != null) {
            m_dbComponentName = componentTable.getTableName();
        }
        m_dbItemNameFieldColName = itemTable.getName().getColumnName();
        m_dbItemIdFieldColName = itemTable.getPrimaryId().getColumnName();
    }

    public enum SelectionMode {

        SELECTED_MODE_NONE, SELECTED_MODE_ALL
    }

    ;

    public enum WhereMode {

        REG_EXP, WILDCARD, EXACT
    }

    ;

    void addItemCaseInsensitive(StringBuffer sqlBuff, String item) {
        if (DbTypeConstant.DB_TYPE == DbEnum.DbType.HSQL) {
            sqlBuff.append("LCASE(");
            sqlBuff.append(item);
            sqlBuff.append(")");
        } else {
            sqlBuff.append(item);
        }
    }

    void buildItemWhere(StringBuffer sqlBuff, boolean regExp, String nameSpec) {
        buildItemWhere(sqlBuff, regExp ? WhereMode.REG_EXP : WhereMode.WILDCARD, nameSpec);
    }

    void buildItemWhere(StringBuffer sqlBuff, WhereMode whereMode, String nameSpec) {
        sqlBuff.append(" FROM ");
        sqlBuff.append(m_dbItemName);
        sqlBuff.append(" WHERE ");
        switch(whereMode) {
            case REG_EXP:
                {
                    if (DbTypeConstant.DB_TYPE == DbEnum.DbType.HSQL) {
                        sqlBuff.append("REGEXP_MATCHES(");
                        sqlBuff.append(m_dbItemNameFieldColName);
                        sqlBuff.append(",'");
                        sqlBuff.append(nameSpec);
                        sqlBuff.append("')");
                    } else {
                        sqlBuff.append(m_dbItemNameFieldColName);
                        sqlBuff.append(" regexp '");
                        sqlBuff.append(nameSpec);
                        sqlBuff.append("'");
                    }
                    break;
                }
            case WILDCARD:
                {
                    addItemCaseInsensitive(sqlBuff, m_dbItemNameFieldColName);
                    sqlBuff.append(" like '");
                    sqlBuff.append(nameSpec.toLowerCase());
                    sqlBuff.append("%'");
                    break;
                }
            case EXACT:
                {
                    sqlBuff.append(m_dbItemNameFieldColName);
                    sqlBuff.append(" = '");
                    sqlBuff.append(nameSpec);
                    sqlBuff.append("'");
                    break;
                }
            default:
                {
                    throw new IllegalArgumentException("Unexpected value " + whereMode + " in buildItemWhere");
                }
        }
    }

    /**
     * @param conn
     * @return
     * @throws SQLException
     */
    int getCount(Connection conn, WhereMode whereMode, String nameSpec) throws SQLException {
        int count = 0;
        Statement statement = conn.createStatement();
        StringBuffer sqlBuff = new StringBuffer("SELECT COUNT(*) ");
        buildItemWhere(sqlBuff, whereMode, nameSpec);
        String sql = sqlBuff.toString();
        ResultSet rs = statement.executeQuery(sql);
        while (rs.next()) {
            count = rs.getInt(1);
        }
        try {
        } finally {
            statement.close();
        }
        return count;
    }

    int getId(Connection conn, WhereMode whereMode, String nameSpec, String idColName) throws SQLException {
        int id = -1;
        Statement statement = conn.createStatement();
        StringBuffer sqlBuff = new StringBuffer("SELECT ");
        sqlBuff.append(idColName);
        buildItemWhere(sqlBuff, whereMode, nameSpec);
        String sql = sqlBuff.toString();
        ResultSet rs = statement.executeQuery(sql);
        while (rs.next()) {
            id = rs.getInt(1);
        }
        try {
        } finally {
            statement.close();
        }
        return id;
    }

    interface IDeleteItemResults {

        public void setDeleted(Set deletedItems);
    }

    interface ICache<E> {

        void deleteFromCache(int cacheId);

        void updateCache(E elem, int cacheId);

        E get(String name);

        E get(int cacheId);
    }

    /**
     * @author Roo and Joey
     *
     * TODO To change the template for this generated type comment go to
     * Window - Preferences - Java - Code Style - Code Templates
     */
    class DeleteItem extends DbQuery {

        private String m_nameSpec;

        private boolean m_regExp;

        private Set m_itemIds;

        private SelectionMode m_selectionMode;

        private IDeleteItemResults m_deleteItemResults;

        private Integer m_parentId;

        /**
         * @param nameSpec
         * @param regExp
         * @param itemIds
         * @param selectionMode
         * @param deleteItemResults
         */
        public DeleteItem(String nameSpec, boolean regExp, Set itemIds, SelectionMode selectionMode, IDeleteItemResults deleteItemResults, Integer parentId, String context) {
            super("Deleting " + context + "s");
            m_nameSpec = nameSpec;
            m_regExp = regExp;
            m_itemIds = itemIds;
            m_selectionMode = selectionMode;
            m_deleteItemResults = deleteItemResults;
            if (parentId != null && parentId > 0) m_parentId = parentId;
        }

        public DeleteItem(String nameSpec, boolean regExp, Set itemIds, SelectionMode selectionMode, IDeleteItemResults deleteItemResults, String context) {
            this(nameSpec, regExp, itemIds, selectionMode, deleteItemResults, null, context);
        }

        void execute(Connection conn, Component parent, String context, ProgressMonitor progressBar, ProgressWrapper progressWrapper) throws Exception {
            Statement statement = null;
            ResultSet rs = null;
            Set deletedSet = new HashSet();
            try {
                conn = DbInit.getInstance().getConnection();
                statement = conn.createStatement();
                StringBuffer sqlBuff = new StringBuffer("SELECT ");
                sqlBuff.append(m_dbItemIdFieldColName);
                buildItemWhere(sqlBuff, m_regExp, m_nameSpec);
                if (m_parentId != null) addParentWhere(sqlBuff, m_parentId, m_itemTable);
                addAdditionalWheres(sqlBuff);
                String sql = sqlBuff.toString();
                rs = statement.executeQuery(sql);
                int itemId = -1;
                int count = 0;
                while (rs.next()) {
                    count++;
                    progressBar.setNote("Loading " + m_itemName + " " + count);
                    itemId = rs.getInt(1);
                    Integer itemID = new Integer(itemId);
                    if (m_selectionMode == SelectionMode.SELECTED_MODE_NONE && m_itemIds.contains(itemID) || m_selectionMode == SelectionMode.SELECTED_MODE_ALL && !m_itemIds.contains(itemID)) {
                        progressBar.setNote("Deleting " + m_itemName + " " + count);
                        delete(conn, itemId);
                        deletedSet.add(itemID);
                        m_itemCache.deleteFromCache(itemId);
                    }
                }
                if (itemId == -1) {
                    DbQuery.showError(parent, m_itemNameCapitalised + " not found", "No " + m_itemNamePlural + " matching criteria");
                    return;
                }
                m_deleteItemResults.setDeleted(deletedSet);
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }

        /**
         * @param conn
         * @param itemId
         * @throws SQLException
         */
        private void delete(Connection conn, int itemId) throws SQLException {
            Statement statement = null;
            try {
                conn.setAutoCommit(false);
                deleteComponents(conn, itemId);
                statement = conn.createStatement();
                StringBuffer sqlBuff = new StringBuffer("DELETE FROM ");
                sqlBuff.append(m_dbItemName);
                sqlBuff.append(" WHERE ");
                sqlBuff.append(m_dbItemIdFieldColName);
                sqlBuff.append(" = ");
                sqlBuff.append(Integer.toString(itemId));
                String sql = sqlBuff.toString();
                statement.executeUpdate(sql);
                conn.commit();
            } catch (SQLException ex) {
                try {
                    conn.rollback();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                throw ex;
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }
    }

    class SaveItem extends DbQuery {

        private boolean m_update;

        private int m_id;

        private final String m_itemNameValue;

        private final Item m_item;

        private final Comp[] m_components;

        private final boolean m_indexesChanged;

        SaveItem(int id, String itemNameValue, Item item, Comp[] components, boolean update, String context) {
            this(id, itemNameValue, item, components, update, true, context);
        }

        SaveItem(int id, String itemNameValue, Item item, Comp[] components, boolean update, boolean indexesChanged, String context) {
            super("Saving " + context);
            m_itemNameValue = itemNameValue;
            m_item = item;
            m_components = components;
            m_update = update;
            m_id = id;
            m_indexesChanged = indexesChanged;
        }

        void execute(Connection conn, Component parent, String context, final ProgressMonitor progressMonitor, ProgressWrapper progressWrapper) throws Exception {
            int noOfComponents = m_components.length;
            Statement statement = null;
            StringBuffer pmNoteBuf = new StringBuffer(m_update ? "Updating " : "Creating ");
            pmNoteBuf.append(m_itemNameAbbrev);
            pmNoteBuf.append(" ");
            pmNoteBuf.append(m_itemNameValue);
            final String pmNote = pmNoteBuf.toString();
            progressMonitor.setNote(pmNote);
            try {
                conn.setAutoCommit(false);
                int id = -1;
                if (m_update) {
                    statement = conn.createStatement();
                    String sql = getUpdateSql(noOfComponents, m_id);
                    statement.executeUpdate(sql);
                    id = m_id;
                    if (m_indexesChanged) deleteComponents(conn, id);
                } else {
                    PreparedStatement pStmt = getInsertPrepStmt(conn, noOfComponents);
                    pStmt.executeUpdate();
                    Integer res = DbCommon.getAutoGenId(parent, context, pStmt);
                    if (res == null) return;
                    id = res.intValue();
                }
                if (!m_update || m_indexesChanged) {
                    PreparedStatement insertCompPrepStmt = conn.prepareStatement(getInsertComponentPrepStmtSql());
                    for (int i = 0; i < noOfComponents; i++) {
                        createComponent(progressMonitor, m_components, pmNote, id, i, insertCompPrepStmt);
                    }
                }
                conn.commit();
                m_itemTable.getPrimaryId().setVal(m_item, id);
                m_itemCache.updateCache(m_item, id);
            } catch (SQLException ex) {
                try {
                    conn.rollback();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                throw ex;
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }

        private String getInsertComponentPrepStmtSql() {
            StringBuffer sqlBuff = new StringBuffer("INSERT INTO ");
            sqlBuff.append(m_dbComponentName);
            sqlBuff.append("(");
            boolean first = true;
            int colCount = 0;
            for (Iterator<DbColumnExt> iter = m_componentTable.getIter(0); iter.hasNext(); ) {
                DbColumnExt column = iter.next();
                if (!first) sqlBuff.append(",");
                first = false;
                sqlBuff.append(column.getColumnName());
                colCount++;
            }
            sqlBuff.append(")");
            sqlBuff.append(" VALUES (");
            first = true;
            for (int i = 0; i < colCount; i++) {
                if (!first) sqlBuff.append(",");
                first = false;
                sqlBuff.append("?");
            }
            sqlBuff.append(")");
            return sqlBuff.toString();
        }

        private String getUpdateSql(int noOfComponents, int id) {
            StringBuffer sqlBuff = new StringBuffer("UPDATE ");
            sqlBuff.append(m_dbItemName);
            sqlBuff.append(" SET ");
            sqlBuff.append(m_itemTable.getName().getColumnName());
            sqlBuff.append(" = '");
            sqlBuff.append(m_itemNameValue);
            sqlBuff.append("'");
            for (Iterator<DbColumnExt> iter = m_itemTable.getIter(2); iter.hasNext(); ) {
                DbColumnExt column = iter.next();
                sqlBuff.append(",");
                sqlBuff.append(column.getColumnName());
                sqlBuff.append("=");
                appendItemValue(sqlBuff, column, noOfComponents);
            }
            sqlBuff.append(" WHERE ");
            sqlBuff.append(m_dbItemIdFieldColName);
            sqlBuff.append(" = ");
            sqlBuff.append(id);
            return sqlBuff.toString();
        }

        /**
         * @param sqlBuff
         * @param column
         * @param noOfComponents 
         */
        private void appendItemValue(StringBuffer sqlBuff, DbColumnExt column, int noOfComponents) {
            boolean hasDelim = column.hasDelim();
            if (hasDelim) sqlBuff.append("'");
            Object obj = null;
            if (column == m_itemTable.getNoOfComponentsCol()) obj = new Integer(noOfComponents); else obj = column.getVal(m_item);
            if (obj == null && column.isNullable()) sqlBuff.append("null"); else sqlBuff.append(obj);
            if (hasDelim) sqlBuff.append("'");
        }

        private String getInsertSql(int noOfComponents) {
            StringBuffer sqlBuff = new StringBuffer("INSERT INTO ");
            sqlBuff.append(m_dbItemName);
            sqlBuff.append("(");
            boolean first = true;
            for (Iterator<DbColumnExt> iter = m_itemTable.getIter(1); iter.hasNext(); ) {
                DbColumnExt column = iter.next();
                if (!first) {
                    sqlBuff.append(",");
                }
                first = false;
                sqlBuff.append(column.getColumnName());
            }
            sqlBuff.append(") VALUES (");
            first = true;
            for (Iterator<DbColumnExt> iter = m_itemTable.getIter(1); iter.hasNext(); ) {
                DbColumnExt column = iter.next();
                if (!first) {
                    sqlBuff.append(",");
                }
                first = false;
                appendItemValue(sqlBuff, column, noOfComponents);
            }
            sqlBuff.append(")");
            return sqlBuff.toString();
        }

        private PreparedStatement getInsertPrepStmt(Connection conn, int noOfComponents) throws SQLException {
            return conn.prepareStatement(getInsertSql(noOfComponents), PreparedStatement.RETURN_GENERATED_KEYS);
        }

        private void createComponent(final ProgressMonitor progressMonitor, Comp[] components, final String pmNote, int id, int componentNdx, PreparedStatement insertCompPrepStmt) throws SQLException {
            Comp comp = components[componentNdx];
            final int dispNdx = componentNdx + 1;
            if (okToStore(comp)) {
                Runnable doFillUpdate = new Runnable() {

                    public void run() {
                        StringBuffer noteBuf = new StringBuffer(pmNote);
                        noteBuf.append(": ");
                        noteBuf.append(m_compNameAbbrev);
                        noteBuf.append(" ");
                        noteBuf.append(dispNdx);
                        progressMonitor.setNote(noteBuf.toString());
                    }
                };
                try {
                    SwingUtilities.invokeLater(doFillUpdate);
                } finally {
                }
                insertCompPrepStmt.clearParameters();
                int colNdx = 1;
                for (Iterator<DbColumnExt> iter = m_componentTable.getIter(0); iter.hasNext(); colNdx++) {
                    DbColumnExt column = iter.next();
                    Object obj = null;
                    if (column == m_componentTable.getParentId()) obj = new Integer(id); else if (column.isGetFromParent()) obj = column.getVal(m_item); else obj = column.getVal(comp);
                    insertCompPrepStmt.setObject(colNdx, obj);
                }
                insertCompPrepStmt.executeUpdate();
            }
        }

        /**
         * Overrride to change behaviour if required.
         * @param comp
         * @return
         */
        boolean okToStore(Comp comp) {
            return true;
        }
    }

    private void deleteComponents(Connection conn, int id) throws SQLException {
        if (m_dbComponentName != null) {
            Statement statement = conn.createStatement();
            StringBuffer sqlBuff = new StringBuffer("DELETE FROM ");
            sqlBuff.append(m_dbComponentName);
            sqlBuff.append(" WHERE ");
            sqlBuff.append(m_componentTable.getParentId().getColumnName());
            sqlBuff.append(" = ");
            sqlBuff.append(id);
            String sql = sqlBuff.toString();
            statement.executeUpdate(sql);
        }
    }

    public interface ILoadItemResults<Param> {

        public void setResults(Param param);
    }

    class BuildItemResult {

        private final int m_id;

        private final int m_noOfComponents;

        private final Item m_item;

        private final String m_name;

        BuildItemResult(int id, int noOfComponents, Item item, String name) {
            m_id = id;
            m_noOfComponents = noOfComponents;
            m_item = item;
            m_name = name;
        }

        /**
         * @return Returns the id.
         */
        protected int getId() {
            return m_id;
        }

        /**
         * @return Returns the item.
         */
        protected Item getItem() {
            return m_item;
        }

        /**
         * @return Returns the noOfComponents.
         */
        protected int getNoOfComponents() {
            return m_noOfComponents;
        }

        /**
         * @return Returns the name.
         */
        protected String getName() {
            return m_name;
        }
    }

    BuildItemResult buildItem(ResultSet rs) throws SQLException {
        Item item = newItem();
        DbColumnExt noOfComponentsCol = m_itemTable.getNoOfComponentsCol();
        DbColumnExt idCol = m_itemTable.getPrimaryId();
        DbColumnExt nameCol = m_itemTable.getName();
        int id = -1;
        int noOfComponents = -1;
        int colNdx = 1;
        String name = null;
        for (Iterator<DbColumnExt> iter = m_itemTable.getIter(0); iter.hasNext(); colNdx++) {
            DbColumnExt column = iter.next();
            if (column == idCol) {
                id = rs.getInt(colNdx);
                column.setVal(item, id);
            } else if (column == nameCol) {
                name = rs.getString(colNdx);
                column.setVal(item, name);
            } else if (column == noOfComponentsCol) {
                noOfComponents = rs.getInt(colNdx);
            } else {
                DbColumnType colType = column.getColumnType();
                if (colType == DbColumnExt.DbColumnType.DB_BYTE || colType == DbColumnExt.DbColumnType.DB_ENUM) {
                    column.setVal(item, rs.getByte(colNdx));
                } else {
                    column.setVal(item, rs.getObject(colNdx));
                }
            }
        }
        return new BuildItemResult(id, noOfComponents, item, name);
    }

    class LoadItem extends DbQuery {

        private final String m_name;

        private ILoadItemResults<Item> m_loadItemResults;

        private final Integer m_id;

        private boolean m_synchronous = false;

        LoadItem(String name, ILoadItemResults<Item> loadItemResults, String context) {
            this(name, loadItemResults, false, context);
        }

        LoadItem(String name, ILoadItemResults<Item> loadItemResults, boolean synchronous, String context) {
            super("Loading " + context);
            m_name = name;
            m_id = null;
            m_loadItemResults = loadItemResults;
        }

        LoadItem(Integer id, ILoadItemResults<Item> loadItemResults, boolean synchronous, String context) {
            super("Loading " + context);
            m_name = null;
            m_id = id;
            m_loadItemResults = loadItemResults;
            m_synchronous = synchronous;
        }

        void execute(Connection conn, Component parent, String context, ProgressMonitor progressBar, ProgressWrapper progressWrapper) throws Exception {
            Item item;
            if (m_name != null) item = cacheLookup(); else item = m_itemCache.get(m_id);
            if (item != null) {
                setNote(progressBar, progressWrapper, m_context + " " + item.getName());
                setNonCacheComponents(conn, parent, progressBar, item, progressWrapper);
                final Item finItem = item;
                if (m_synchronous) {
                    m_loadItemResults.setResults(item);
                } else {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            m_loadItemResults.setResults(finItem);
                        }
                    });
                }
                return;
            }
            Statement statement = null;
            ResultSet rs = null;
            try {
                conn = DbInit.getInstance().getConnection();
                statement = conn.createStatement();
                String sql;
                if (m_name != null) sql = getSelectStmtSQL(m_name); else sql = getSelectStmtSQL(m_id);
                rs = statement.executeQuery(sql);
                int id = -1;
                int noOfComponents = -1;
                String name = "";
                if (rs.next()) {
                    BuildItemResult res = buildItem(rs);
                    id = res.getId();
                    noOfComponents = res.getNoOfComponents();
                    item = res.getItem();
                    name = res.getName();
                }
                if (id == -1) {
                    StringBuffer notFoundBuf = new StringBuffer(m_itemNameCapitalised);
                    if (m_name != null) {
                        notFoundBuf.append(" '");
                        notFoundBuf.append(m_name);
                        notFoundBuf.append("'");
                    }
                    notFoundBuf.append(" not found");
                    StringBuffer invalidItemBuf = new StringBuffer("Invalid ");
                    invalidItemBuf.append(m_itemName);
                    Dialogs.showErrorMessageDialogEventDispatch(invalidItemBuf.toString(), notFoundBuf.toString());
                    return;
                }
                Comp[] components = null;
                StringBuffer noteBuf = new StringBuffer(m_context);
                noteBuf.append(" ");
                noteBuf.append(name == null ? "" : name);
                setNote(progressBar, progressWrapper, noteBuf.toString());
                if (m_componentTable != null) {
                    components = loadComponents(conn, id, noOfComponents);
                }
                item.setComponents(components);
                setAdditionalComponents(conn, parent, progressBar, item, progressWrapper);
                m_itemCache.updateCache(item, id);
                final Item finItem = item;
                if (m_synchronous) {
                    m_loadItemResults.setResults(finItem);
                } else {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            m_loadItemResults.setResults(finItem);
                        }
                    });
                }
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }

        private void setNote(ProgressMonitor progressBar, ProgressWrapper progressWrapper, String note) {
            if (progressBar != null) {
                progressBar.setNote(note);
            } else if (progressWrapper != null) {
                progressWrapper.setNote(note);
            }
        }

        protected void setNonCacheComponents(Connection conn, Component parent, ProgressMonitor progressBar, Item item, ProgressWrapper progressWrapper) throws Exception {
        }

        protected void setAdditionalComponents(Connection conn, Component parent, ProgressMonitor progressBar, Item item, ProgressWrapper progressWrapper) throws Exception {
        }

        private String getSelectStmtSQL(String name) {
            StringBuffer sqlBuff = getSelectSQL();
            sqlBuff.append(" FROM ");
            sqlBuff.append(m_dbItemName);
            sqlBuff.append(" WHERE ");
            sqlBuff.append(m_dbItemNameFieldColName);
            sqlBuff.append(" = '");
            sqlBuff.append(name);
            sqlBuff.append("'");
            return sqlBuff.toString();
        }

        private String getSelectStmtSQL(int id) {
            StringBuffer sqlBuff = getSelectSQL();
            sqlBuff.append(" FROM ");
            sqlBuff.append(m_dbItemName);
            sqlBuff.append(" WHERE ");
            sqlBuff.append(m_dbItemIdFieldColName);
            sqlBuff.append(" = ");
            sqlBuff.append(id);
            return sqlBuff.toString();
        }

        private Item cacheLookup() {
            return m_itemCache.get(m_name);
        }
    }

    /**
     * For (optional) override
     * @param components
     */
    void initialiseComponents(Comp[] components) {
    }

    public Comp[] loadComponents(Connection conn, int id, int noOfComponents) throws SQLException {
        Statement statement = null;
        ResultSet rs = null;
        try {
            statement = conn.createStatement();
            String sql = getSelectComponentSQL(id);
            rs = statement.executeQuery(sql);
            Comp[] components = (Comp[]) Array.newInstance(m_compClass, noOfComponents);
            initialiseComponents(components);
            int compNdx = 0;
            while (rs.next()) {
                Comp component = null;
                try {
                    component = m_compClass.newInstance();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                int colNdx = 1;
                DbColumnExt parentIdCol = m_componentTable.getParentId();
                for (Iterator<DbColumnExt> iter = m_componentTable.getIter(0); iter.hasNext(); colNdx++) {
                    DbColumnExt column = iter.next();
                    if (column != parentIdCol) {
                        column.setVal(component, rs.getObject(colNdx));
                    }
                }
                components[getCompNdx(compNdx, component)] = component;
                compNdx++;
            }
            return components;
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    /**
     * For (optional) override.
     * @param compNdx
     * @param component
     * @return
     */
    int getCompNdx(int compNdx, Comp component) {
        return compNdx;
    }

    private String getSelectComponentSQL(int id) {
        StringBuffer sqlBuff = new StringBuffer("SELECT ");
        boolean first = true;
        for (Iterator<DbColumnExt> iter = m_componentTable.getIter(0); iter.hasNext(); ) {
            DbColumnExt column = iter.next();
            if (!first) sqlBuff.append(",");
            first = false;
            sqlBuff.append(column.getColumnName());
            if (column.isEnum()) {
                sqlBuff.append("+0");
            }
        }
        sqlBuff.append(" FROM ");
        sqlBuff.append(m_dbComponentName);
        sqlBuff.append(" WHERE ");
        sqlBuff.append(m_componentTable.getParentId().getColumnName());
        sqlBuff.append(" = ");
        sqlBuff.append(id);
        return sqlBuff.toString();
    }

    public interface IListItemResults<I> {

        public void setNames(List<String> names);

        /**
         * @param rowCount
         */
        public void setCount(int itemCount);

        /**
         * @param harmonicIds
         */
        public void setItemIds(List<Integer> itemIds);

        public void setItems(List<I> items);

        public void setIter(IListItemIter iter);
    }

    public boolean exists(String name) throws Exception {
        DbInit instance = DbInit.getInstance();
        instance.init();
        Connection conn = instance.getConnection();
        return getCount(conn, WhereMode.EXACT, name) > 0;
    }

    public int getId(String name) throws Exception {
        DbInit instance = DbInit.getInstance();
        instance.init();
        Connection conn = instance.getConnection();
        return getId(conn, WhereMode.EXACT, name, m_itemTable.getPrimaryId().getColumnName());
    }

    class ListItem extends DbQuery {

        private String m_nameSpec;

        private boolean m_regExp;

        private boolean m_showItems;

        private int m_rowLimit;

        private IListItemResults<Item> m_listItemResults;

        private Component m_parent;

        private int m_startOfs = 0;

        private boolean m_getItemIds;

        private boolean m_getCount;

        private int m_itemCount;

        private Integer m_parentId;

        private final Set m_itemIds;

        private final SelectionMode m_selectionMode;

        private boolean m_resultsOnDispatchThread;

        /**
         * @param startOfs The startOfs to set.
         */
        public void setStartOfs(int startOfs) {
            m_startOfs = startOfs;
        }

        /**
         * @param nameSpec
         * @param regExp
         * @param showItems
         * @param rowLimit
         * @param listItemResults
         * @param context
         * @param parent
         * @param getItemIds
         */
        public ListItem(String nameSpec, boolean regExp, boolean showItems, int rowLimit, IListItemResults<Item> listItemResults, Component parent, String context, boolean getItemIds, boolean getCount) {
            this(nameSpec, regExp, showItems, rowLimit, listItemResults, parent, context, getItemIds, getCount, null, null, null, true);
        }

        public ListItem(String nameSpec, boolean regExp, boolean showItems, int rowLimit, IListItemResults<Item> listItemResults, Component parent, String context, boolean getItemIds, boolean getCount, Integer parentId, Set itemIds, SelectionMode selectionMode, boolean resultsOnDispatchThread) {
            super("Listing " + context + "s");
            m_nameSpec = nameSpec;
            m_regExp = regExp;
            m_showItems = showItems;
            m_rowLimit = rowLimit;
            m_listItemResults = listItemResults;
            m_parent = parent;
            m_getItemIds = getItemIds;
            m_getCount = getCount;
            m_itemIds = itemIds;
            m_selectionMode = selectionMode;
            m_resultsOnDispatchThread = resultsOnDispatchThread;
            if (parentId != null && parentId > 0) m_parentId = parentId;
        }

        ListItem getThis() {
            return this;
        }

        String getOrderBy() {
            return m_dbItemNameFieldColName;
        }

        void execute(Connection conn, Component parent, String context, ProgressMonitor progressBar, ProgressWrapper progressWrapper) throws Exception {
            Statement statement = null;
            ResultSet rs = null;
            final List<String> names = new ArrayList<String>();
            final List<Item> items = (m_showItems ? new ArrayList<Item>() : null);
            final List<Integer> itemIds = (m_getItemIds ? new ArrayList<Integer>() : null);
            m_itemCount = 0;
            try {
                conn = DbInit.getInstance().getConnection();
                statement = conn.createStatement();
                StringBuffer sqlBuff = getSelectSQL();
                buildItemWhere(sqlBuff, m_regExp, m_nameSpec);
                if (m_parentId != null) addParentWhere(sqlBuff, m_parentId, m_itemTable);
                addAdditionalWheres(sqlBuff);
                if (m_getCount) m_itemCount = getCount(conn, (m_regExp ? WhereMode.REG_EXP : WhereMode.WILDCARD), m_nameSpec);
                sqlBuff.append(" ORDER BY ");
                if (DbTypeConstant.DB_TYPE == DbEnum.DbType.HSQL) {
                    sqlBuff.append("UPPER(");
                }
                sqlBuff.append(getOrderBy());
                if (DbTypeConstant.DB_TYPE == DbEnum.DbType.HSQL) {
                    sqlBuff.append(")");
                }
                if (m_selectionMode == null) {
                    sqlBuff.append(" LIMIT ");
                    sqlBuff.append((m_rowLimit + 1));
                    sqlBuff.append(" OFFSET ");
                    sqlBuff.append(m_startOfs);
                }
                String sql = sqlBuff.toString();
                rs = statement.executeQuery(sql);
                Item params = null;
                int itemId = -1;
                int noOfComponents = -1;
                String name = null;
                int count = m_startOfs;
                final boolean hasEarlierItems = m_startOfs > 0;
                int targetOfs = (m_selectionMode == null) ? m_startOfs + m_rowLimit : Integer.MAX_VALUE;
                while (rs.next()) {
                    count++;
                    if (count > targetOfs) break;
                    BuildItemResult res = buildItem(rs);
                    itemId = res.getId();
                    if (m_selectionMode != null) {
                        if (!(m_selectionMode == SelectionMode.SELECTED_MODE_NONE && m_itemIds.contains(itemId) || m_selectionMode == SelectionMode.SELECTED_MODE_ALL && !m_itemIds.contains(itemId))) {
                            continue;
                        }
                    }
                    name = res.getName();
                    names.add(name);
                    StringBuffer noteBuf = new StringBuffer("Loading ");
                    noteBuf.append(m_itemName);
                    noteBuf.append(" ");
                    noteBuf.append(name);
                    if (progressBar != null) {
                        progressBar.setNote(noteBuf.toString());
                    } else if (progressWrapper != null) {
                        progressWrapper.setNote(noteBuf.toString());
                    }
                    noOfComponents = res.getNoOfComponents();
                    params = res.getItem();
                    if (m_getItemIds) {
                        itemIds.add(new Integer(itemId));
                    }
                    if (m_showItems) {
                        Item itemParams = m_itemCache.get(itemId);
                        if (itemParams == null) {
                            if (noOfComponents > 0) {
                                Comp[] components = loadComponents(conn, itemId, noOfComponents);
                                itemParams = params;
                                itemParams.setComponents(components);
                            } else {
                                itemParams = params;
                            }
                            m_itemCache.updateCache(itemParams, itemId);
                        }
                        items.add(itemParams);
                    }
                }
                final boolean hasLaterItems = count > targetOfs;
                if (itemId == -1) {
                    StringBuffer errBuf = new StringBuffer(m_itemNameCapitalised);
                    errBuf.append(" not found");
                    StringBuffer errBuf2 = new StringBuffer("No ");
                    errBuf2.append(m_itemNamePlural);
                    errBuf2.append("  matching criteria");
                    DbQuery.showError(parent, errBuf.toString(), errBuf2.toString());
                    return;
                }
                if (m_resultsOnDispatchThread) {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            setResults(names, items, itemIds, hasEarlierItems, hasLaterItems);
                        }
                    });
                } else {
                    if (progressBar != null) {
                        progressBar.close();
                    }
                    setResults(names, items, itemIds, hasEarlierItems, hasLaterItems);
                }
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }

        /**
         * 
         */
        public void start() {
            super.start(m_parent);
        }

        /**
         * @param b
         */
        public void setGetCount(boolean getCount) {
            m_getCount = getCount;
        }

        private void setResults(final List<String> names, final List<Item> items, final List<Integer> itemIds, final boolean hasEarlierItems, final boolean hasLaterItems) {
            m_listItemResults.setNames(names);
            if (m_getItemIds) m_listItemResults.setItemIds(itemIds);
            if (m_getCount) m_listItemResults.setCount(m_itemCount);
            ListItemIter iter = new ListItemIter(m_startOfs, getThis(), hasLaterItems, hasEarlierItems, m_rowLimit);
            m_listItemResults.setIter(iter);
            if (m_showItems) m_listItemResults.setItems(items);
        }
    }

    /**
     * @return
     */
    private StringBuffer getSelectSQL() {
        StringBuffer sqlBuff = new StringBuffer("SELECT ");
        boolean first = true;
        for (Iterator<DbColumnExt> iter = m_itemTable.getIter(0); iter.hasNext(); ) {
            DbColumnExt column = iter.next();
            if (!first) sqlBuff.append(",");
            first = false;
            sqlBuff.append(column.getColumnName());
            if (column.isEnum()) {
                sqlBuff.append("+0");
            }
        }
        return sqlBuff;
    }

    /**
     * @param item
     * @return
     */
    private Item newItem() {
        Item item = null;
        try {
            item = m_itemClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return item;
    }

    static void setFilenameParam(PreparedStatement pStmt, int index, String fileName) throws SQLException {
        if (fileName == null) {
            pStmt.setNull(index, Types.VARCHAR);
        } else {
            pStmt.setString(index, fileName);
        }
    }

    static void setGraphParam(PreparedStatement pStmt, int index, Integer graphId) throws SQLException {
        if (graphId == null || graphId.intValue() <= 0) {
            pStmt.setNull(index, Types.INTEGER);
        } else {
            pStmt.setInt(index, graphId.intValue());
        }
    }

    static void saveChildren(Connection conn, Component frame, JComponent parentComp, int parentDbId, boolean parentIsSequence) throws SQLException {
        int componentCount = parentComp.getComponentCount();
        for (int i = 0; i < componentCount; i++) {
            JComponent component = (JComponent) parentComp.getComponent(i);
            if (parentIsSequence && (component instanceof SequenceBoxComponentLayer)) {
                saveChildren(conn, frame, component, parentDbId, true);
                return;
            }
            BaseProperties baseProp = (BaseProperties) component.getClientProperty(EditViewFocusManager.USER_PROPERTIES);
            if (baseProp != null) {
                baseProp.save(conn, frame, i + 1, parentDbId, component, parentIsSequence);
            }
        }
    }

    static String doubleBackslashes(String soundFileName) {
        String[] parts = soundFileName.split("\\\\");
        StringBuffer buf = new StringBuffer();
        buf.append(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            buf.append("\\\\");
            buf.append(parts[i]);
        }
        return buf.toString();
    }

    static void appendString(StringBuffer sqlBuff, String s) {
        sqlBuff.append("'");
        sqlBuff.append(Utils.isBlank(s) ? "" : s);
        sqlBuff.append("'");
    }

    static String prepareString(String s) {
        return Utils.isBlank(s) ? "" : s;
    }

    static void appendFileName(StringBuffer sqlBuff, String fname) {
        if (Utils.isBlank(fname)) {
            sqlBuff.append("null");
        } else {
            String fileNameDoubledBackSlashes = doubleBackslashes(fname);
            appendString(sqlBuff, fileNameDoubledBackSlashes);
        }
    }

    public static Integer getAutoGenId(Component parent, String context, Statement statement) throws SQLException {
        ResultSet rs = statement.getGeneratedKeys();
        if (rs.next()) {
            return rs.getInt(1);
        } else {
            DbQuery.showError(parent, "Error getting auto-increment key", context);
            return null;
        }
    }

    private class ListItemIter implements IListItemIter {

        private final int m_offset;

        private final ListItem m_listDbQry;

        private final boolean m_hasNext;

        private final boolean m_hasPrev;

        private final int m_rowLimit;

        ListItemIter(int offset, ListItem listItem, boolean hasNext, boolean hasPrev, int rowLimit) {
            m_offset = offset;
            m_listDbQry = listItem;
            m_hasNext = hasNext;
            m_hasPrev = hasPrev;
            m_rowLimit = rowLimit;
            m_listDbQry.setGetCount(false);
        }

        public boolean hasNext() {
            return m_hasNext;
        }

        public boolean hasPrev() {
            return m_hasPrev;
        }

        public void next() {
            if (hasNext()) {
                m_listDbQry.setStartOfs(m_offset + m_rowLimit);
                m_listDbQry.start();
            }
        }

        public void prev() {
            if (hasPrev()) {
                m_listDbQry.setStartOfs(m_offset > m_rowLimit ? m_offset - m_rowLimit : 0);
                m_listDbQry.start();
            }
        }
    }

    static class SqlAndMode {

        public String m_sql;

        public PreparedStatement m_pStmt;

        public boolean m_isUpdate;
    }
}
