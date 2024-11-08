package database;

import gui.Track0Properties;
import java.awt.Component;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import preferences.Preference;
import xml.Composition;
import xml.Composition.CompositionEntity;
import common.IListItemIter;
import common.ProgressWrapper;
import database.DbTrack.PropertiesAndId;

public class DbComposition extends DbCommon<CompositionParams, TrackParams> {

    private static final String CONTEXT = "composition";

    static DbTable s_compositionTable = new DbTable("composition");

    static {
        s_compositionTable.addColumn(new DbColumn<CompositionParams>("composition_id", DbColumn.DbColumnType.DB_INT, false, new DbColumn.ISetColValue<CompositionParams>() {

            public void setVal(CompositionParams params, Object value) {
                params.setCompostionId((Integer) value);
            }
        }, new DbColumn.IGetColValue<CompositionParams>() {

            public Object getVal(CompositionParams params) {
                return params.getCompositionId();
            }
        }), DbTable.FieldType.PRIMARY_ID);
        s_compositionTable.addColumn(new DbColumn<CompositionParams>("name", DbColumn.DbColumnType.DB_STRING, false, new DbColumn.ISetColValue<CompositionParams>() {

            public void setVal(CompositionParams params, Object value) {
                params.setName((String) value);
            }
        }, new DbColumn.IGetColValue<CompositionParams>() {

            public Object getVal(CompositionParams params) {
                return params.getName();
            }
        }), DbTable.FieldType.NAME);
        s_compositionTable.addColumn(new DbColumn<CompositionParams>("track_zero_fname", DbColumn.DbColumnType.DB_STRING, false, new DbColumn.ISetColValue<CompositionParams>() {

            public void setVal(CompositionParams params, Object value) {
                params.setTrack0fname((String) value);
            }
        }, new DbColumn.IGetColValue<CompositionParams>() {

            public Object getVal(CompositionParams params) {
                return params.getTrack0fname();
            }
        }), DbTable.FieldType.NORMAL);
        s_compositionTable.addColumn(new DbColumn<CompositionParams>("calculation_freq", DbColumn.DbColumnType.DB_SHORT, false, new DbColumn.ISetColValue<CompositionParams>() {

            public void setVal(CompositionParams params, Object value) {
                params.setCalcFreq((Integer) value);
            }
        }, new DbColumn.IGetColValue<CompositionParams>() {

            public Object getVal(CompositionParams params) {
                return params.getCalcFreq();
            }
        }), DbTable.FieldType.NORMAL);
        s_compositionTable.addColumn(new DbColumn<CompositionParams>("use_midi_pitches", DbColumn.DbColumnType.DB_BOOLEAN, false, new DbColumn.ISetColValue<CompositionParams>() {

            public void setVal(CompositionParams params, Object value) {
                params.setUseMidiPitches((Boolean) value);
            }
        }, new DbColumn.IGetColValue<CompositionParams>() {

            public Object getVal(CompositionParams params) {
                return params.isUseMidiPitches();
            }
        }), DbTable.FieldType.NORMAL);
    }

    private DbComposition() {
        super("composition", "Composition", "compositions", "co", "component", "components", "cpnt", s_compositionTable, null, CompositionParams.class, TrackParams.class, new ICache<CompositionParams>() {

            public void deleteFromCache(int cacheId) {
                s_compostionCache.delete(cacheId);
            }

            public void updateCache(CompositionParams elem, int cacheId) {
                s_compostionCache.update(cacheId, elem);
            }

            public CompositionParams get(String name) {
                return s_compostionCache.get(name);
            }

            public CompositionParams get(int cacheId) {
                return s_compostionCache.get(cacheId);
            }
        });
    }

    private static final DbComposition s_composition = new DbComposition();

    static class CompositionCache extends Cache<Integer, CompositionParams> {

        CompositionCache() {
            super(Preference.getCacheSize(DbComposition.class));
        }

        /**
         * Returns id corresponding to <i>name</i>, or -1 if not in cache.
         */
        public int getId(String name) {
            CompositionParams params = get(name);
            return params == null ? -1 : params.getCompositionId();
        }

        public CompositionParams getParams(Integer id) {
            return get(id);
        }
    }

    private static CompositionCache s_compostionCache = new CompositionCache();

    private PreparedStatement m_pstmtInsert;

    private PreparedStatement m_pstmtUpdate;

    class DeleteComposition extends DbQuery {

        private final int m_compositionId;

        DeleteComposition(int compositionId) {
            super("Deleting " + CONTEXT);
            m_compositionId = compositionId;
        }

        @Override
        void execute(Connection conn, Component parent, String context, ProgressMonitor progressBar, ProgressWrapper progressWrapper) throws Exception {
            Statement statement = null;
            try {
                conn.setAutoCommit(false);
                statement = conn.createStatement();
                String deleteSql = getDeleteSql(m_compositionId);
                statement.executeUpdate(deleteSql);
                conn.commit();
                s_compostionCache.delete(new Integer(m_compositionId));
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

        private String getDeleteSql(int compositionId) {
            StringBuffer buf = new StringBuffer("DELETE FROM composition WHERE composition_id = ");
            buf.append(compositionId);
            return buf.toString();
        }
    }

    public static void deleteSynchronously(Component parent, int compositionId) throws Exception {
        DeleteComposition deleteComposition = s_composition.new DeleteComposition(compositionId);
        deleteComposition.runSynchronously(parent, null);
    }

    public static void delete(JFrame frame) {
        DeleteComposition deleteComposition = s_composition.new DeleteComposition(Track0Properties.getLoadedComposition());
        deleteComposition.start(frame);
    }

    class SaveComposition extends DbQuery {

        private final Component m_parent;

        private final CompositionEntity m_compEntity;

        private int m_compositionId;

        public SaveComposition(Component parent, Composition.CompositionEntity compEntity) {
            super("Saving " + CONTEXT);
            m_parent = parent;
            m_compEntity = compEntity;
            m_compositionId = (compEntity == null ? Track0Properties.getLoadedComposition() : Track0Properties.NO_COMPOSITION_LOADED);
        }

        @Override
        void execute(Connection conn, Component parent, String context, ProgressMonitor progressBar, ProgressWrapper progressWrapper) throws Exception {
            try {
                conn.setAutoCommit(false);
                DbCommon.SqlAndMode pStmtInfo = getCompositionPrepStmt(conn);
                PreparedStatement pStmt = pStmtInfo.m_pStmt;
                pStmt.executeUpdate();
                if (pStmtInfo.m_isUpdate) {
                    pStmt.clearParameters();
                } else {
                    Integer compositionId = DbCommon.getAutoGenId(parent, context, m_pstmtInsert);
                    pStmt.clearParameters();
                    if (compositionId == null) return;
                    m_compositionId = compositionId.intValue();
                }
                List<PropertiesAndId> propList = null;
                if (m_compEntity == null) {
                    propList = DbTrack.saveTracks(m_parent, conn, m_compositionId);
                } else {
                    DbTrack.saveTrack0(m_parent, conn, m_compositionId, m_compEntity);
                }
                conn.commit();
                if (m_compEntity == null) {
                    DbTrack.updateProperties(propList, m_compositionId);
                    Track0Properties.setLoadedComposition(m_compositionId);
                    s_compostionCache.update(new Integer(m_compositionId), new CompositionParams(m_compositionId, Track0Properties.getCompositionName(), Track0Properties.getSoundFile(), Track0Properties.getFrequency().getVal(), Track0Properties.useMidiPitches()));
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            Track0Properties.markCompositionAsUnedited(true);
                        }
                    });
                } else {
                    m_compEntity.setCompositionId(m_compositionId);
                }
            } catch (SQLException ex) {
                try {
                    conn.rollback();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                throw ex;
            }
        }

        private DbCommon.SqlAndMode getCompositionPrepStmt(Connection conn) throws SQLException {
            SqlAndMode res = new SqlAndMode();
            if (res.m_isUpdate = (m_compositionId != Track0Properties.NO_COMPOSITION_LOADED)) {
                if (m_pstmtUpdate == null) {
                    m_pstmtUpdate = conn.prepareStatement("UPDATE composition SET track_zero_fname = ?," + "name = ?," + "calculation_freq = ?," + "use_midi_pitches = ?" + " WHERE composition_id = ? ");
                }
                res.m_pStmt = m_pstmtUpdate;
                int ndx = setCompositionPreparedParams(m_pstmtUpdate, 1);
                m_pstmtUpdate.setInt(ndx++, m_compositionId);
            } else {
                if (m_pstmtInsert == null) {
                    m_pstmtInsert = conn.prepareStatement("INSERT INTO composition" + "(track_zero_fname, name, calculation_freq, use_midi_pitches) " + "VALUES (?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
                }
                res.m_pStmt = m_pstmtInsert;
                setCompositionPreparedParams(m_pstmtInsert, 1);
            }
            return res;
        }

        private int setCompositionPreparedParams(PreparedStatement pStmt, int startOfs) throws SQLException {
            int i = startOfs;
            if (m_compEntity == null) {
                DbCommon.setFilenameParam(pStmt, i++, Track0Properties.getSoundFile());
                pStmt.setString(i++, DbCommon.prepareString(Track0Properties.getCompositionName()));
                pStmt.setInt(i++, Track0Properties.getFrequency().getVal());
                pStmt.setBoolean(i++, Track0Properties.useMidiPitches());
            } else {
                DbCommon.setFilenameParam(pStmt, i++, null);
                pStmt.setString(i++, m_compEntity.getCompositionName());
                pStmt.setInt(i++, m_compEntity.getCalcFreq().getVal());
                pStmt.setBoolean(i++, m_compEntity.isUseMidiPitches());
            }
            return i;
        }
    }

    public static void save(JFrame frame) throws Exception {
        SaveComposition saveComposition = s_composition.new SaveComposition(frame, null);
        saveComposition.start(frame);
    }

    public static void saveSynchronously(Component parent, Composition.CompositionEntity compEntity) throws Exception {
        SaveComposition saveComposition = s_composition.new SaveComposition(parent, compEntity);
        saveComposition.runSynchronously(parent, null);
    }

    private class ListComposition extends ListItem {

        /**
         * @param nameSpec
         * @param regExp
         * @param showCompositions
         * @param rowLimit
         * @param listCompositionResults
         * @param context
         * @param parent
         * @param getCompositionIds
         */
        public ListComposition(String nameSpec, boolean regExp, boolean showCompositions, int rowLimit, final IListCompositionResults listCompositionResults, Component parent, boolean getCompositionIds, boolean getCount, Set itemIds, SelectionMode selectionMode, boolean resultsOnDispatchThread) {
            super(nameSpec, regExp, showCompositions, rowLimit, new IListItemResults<CompositionParams>() {

                public void setNames(List<String> names) {
                    listCompositionResults.setNames(names);
                }

                public void setCount(int itemCount) {
                    listCompositionResults.setCount(itemCount);
                }

                public void setItemIds(List<Integer> itemIds) {
                    listCompositionResults.setCompositionIds(itemIds);
                }

                public void setItems(List<CompositionParams> items) {
                    listCompositionResults.setCompositions(items);
                }

                public void setIter(IListItemIter iter) {
                    listCompositionResults.setIter(iter);
                }
            }, parent, CONTEXT, getCompositionIds, getCount, null, itemIds, selectionMode, resultsOnDispatchThread);
        }
    }

    public interface IListCompositionIter extends IListItemIter {
    }

    public interface IListCompositionResults {

        public void setNames(List names);

        /**
         * @param rowCount
         */
        public void setCount(int compositionCount);

        /**
         * @param compositionIds
         */
        public void setCompositionIds(List compositionIds);

        public void setCompositions(List<CompositionParams> compositions);

        public void setIter(IListItemIter iter);
    }

    public static void list(Component parent, String nameSpec, boolean regExp, boolean showCompositions, int rowLimit, IListCompositionResults listCompositionResults, boolean getCompositionIds, Set itemIds, SelectionMode selectionMode, boolean resultsOnDispatchThread) {
        ListComposition listDbQry = s_composition.new ListComposition(nameSpec, regExp, showCompositions, rowLimit, listCompositionResults, parent, getCompositionIds, true, itemIds, selectionMode, resultsOnDispatchThread);
        listDbQry.start(parent);
    }

    public static void listSynchronously(Component parent, String nameSpec, boolean regExp, boolean showCompositions, int rowLimit, IListCompositionResults listCompositionResults, boolean getCompositionIds, Set itemIds, SelectionMode selectionMode, ProgressWrapper progressWrapper) throws Exception {
        ListComposition listDbQry = s_composition.new ListComposition(nameSpec, regExp, showCompositions, rowLimit, listCompositionResults, parent, getCompositionIds, true, itemIds, selectionMode, false);
        listDbQry.runSynchronously2(parent, progressWrapper);
    }

    public static boolean exist(String name) throws Exception {
        return s_composition.exists(name);
    }

    /**
     *Get Id corresponding to <name>, or -1 if a record corresponding to
     *name cannot be found.
     */
    public static int getID(String name) throws Exception {
        int idFromCache = s_compostionCache.getId(name);
        return idFromCache == -1 ? s_composition.getId(name) : idFromCache;
    }

    public interface ILoadCompositionResults {

        public void setResults(CompositionParams params);
    }

    private class LoadComposition extends LoadItem {

        LoadComposition(String name, final ILoadCompositionResults loadCompositionResults, final Component parent, final boolean synchronous) {
            super(name, new ILoadItemResults<CompositionParams>() {

                public void setResults(final CompositionParams param) {
                    loadCompositionResults.setResults(param);
                }
            }, CONTEXT);
        }

        public LoadComposition(Integer id, final ILoadCompositionResults loadCompositionResults, final Component parent, final boolean synchronous) {
            super(id, new ILoadItemResults<CompositionParams>() {

                public void setResults(final CompositionParams param) {
                    loadCompositionResults.setResults(param);
                }
            }, synchronous, CONTEXT);
        }

        @Override
        protected void setAdditionalComponents(Connection conn, Component parent, ProgressMonitor progressBar, CompositionParams params, ProgressWrapper progressWrapper) throws Exception {
            params.setTrackParams(DbTrack.load(conn, parent, progressBar, params.getCompositionId(), progressWrapper));
        }

        @Override
        protected void setNonCacheComponents(Connection conn, Component parent, ProgressMonitor progressBar, CompositionParams params, ProgressWrapper progressWrapper) throws Exception {
            params.setTrackParams(DbTrack.load(conn, parent, progressBar, params.getCompositionId(), progressWrapper));
        }
    }

    public static void load(Component parent, String name, ILoadCompositionResults loadCompositionResults) {
        LoadComposition loadDbQry = s_composition.new LoadComposition(name, loadCompositionResults, parent, false);
        loadDbQry.start(parent);
    }

    public static void load(Component parent, Integer id, ILoadCompositionResults loadCompositionResults) {
        LoadComposition loadDbQry = s_composition.new LoadComposition(id, loadCompositionResults, parent, false);
        loadDbQry.start(parent);
    }

    public static void loadSynchronously(Component parent, Integer id, ILoadCompositionResults loadCompositionResults, ProgressWrapper progressWrapper) throws Exception {
        LoadComposition loadDbQry = s_composition.new LoadComposition(id, loadCompositionResults, parent, true);
        loadDbQry.runSynchronously2(parent, progressWrapper);
    }
}
