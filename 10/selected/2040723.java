package org.opcda2out.output.database;

import java.util.logging.Level;
import org.communications.CommunicationManager.STATUS;
import org.database.DatabaseManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.opccom.da.OPCDAManager;
import org.jinterop.dcom.core.JIVariant;
import org.opcda2out.OPCItemData;
import org.opcda2out.composite.CompositeItem;
import org.opcda2out.exception.FatalInitializationException;
import org.opcda2out.exception.InitializationException;
import org.opcda2out.exception.RecoverableInitializationException;
import org.opcda2out.output.OutputWriter;
import org.opcda2out.opciteminfo.AbstractOPCItemInfo;
import org.opcda2out.opciteminfo.ArrayOPCItemInfo;
import org.opcda2out.opciteminfo.OPCItemInfo;
import org.opcda2out.output.OutputData;
import org.opcda2out.scripting.ScriptDataResult;

/**
 * The database output type writer
 *
 * @author Joao Leal
 */
public class DatabaseWriter implements OutputWriter {

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(DatabaseWriter.class.getName());

    /**
     * The output type name
     */
    public static final String NAME = "Database";

    /**
     * Maps the OPC items to the prepared statement updaters
     */
    private final Map<String, PstmtItemInfo> updaterMap = new HashMap<String, PstmtItemInfo>();

    /**
     * Maps the composite itemss to the prepared statement updaters
     */
    private final Map<CompositeItem, PsmtCompositeUpdater> updaterCompMap = new HashMap<CompositeItem, PsmtCompositeUpdater>();

    /**
     * The manager used to write to the database
     */
    private final DatabaseManager database;

    /**
     * The OPC connection manager
     */
    private final OPCDAManager opc;

    private final AbstractDatabaseStructureHandler dbStruct;

    private TableData[] tables;

    /**
     * Creates a new database output writer
     * 
     * @param database The database connection manager
     * @param opc The OPC connection manager
     * @param d The database structure handler
     */
    public DatabaseWriter(DatabaseManager database, OPCDAManager opc, AbstractDatabaseStructureHandler d) {
        if (database == null) {
            throw new NullPointerException("The database manager must not be null!");
        }
        this.database = database;
        if (opc == null) {
            throw new NullPointerException("The OPC manager must not be null!");
        }
        this.opc = opc;
        if (d == null) {
            throw new NullPointerException("The database sructure creator must not be null!");
        }
        this.dbStruct = d;
    }

    public Integer getOPCServerPoolId() {
        return dbStruct.getOPCServerPoolId();
    }

    @Override
    public boolean isReady() {
        return database.getConnectionStatus() == STATUS.CONNECTED;
    }

    @Override
    public boolean isOPCTypeSupported(int VT_Type) {
        int type = VT_Type;
        if ((VT_Type & JIVariant.VT_ARRAY) == JIVariant.VT_ARRAY) {
            type = VT_Type & ~JIVariant.VT_ARRAY;
        }
        switch(type) {
            case JIVariant.VT_UI1:
            case JIVariant.VT_I2:
            case JIVariant.VT_UI2:
            case JIVariant.VT_INT:
            case JIVariant.VT_I4:
            case JIVariant.VT_UI4:
            case JIVariant.VT_CY:
            case JIVariant.VT_I8:
            case JIVariant.VT_R4:
            case JIVariant.VT_R8:
            case JIVariant.VT_DATE:
            case JIVariant.VT_BSTR:
            case JIVariant.VT_BOOL:
                return true;
        }
        return false;
    }

    @Override
    public synchronized void initialize(AbstractOPCItemInfo[] opcItemInfo, CompositeItem[] composite, boolean saveProperties) throws InitializationException {
        for (AbstractOPCItemInfo itemInfo : opcItemInfo) {
            PstmtItemInfo updatePsmtVal;
            if (itemInfo instanceof OPCItemInfo) {
                updatePsmtVal = new PsmtItemUpdater((OPCItemInfo) itemInfo);
            } else {
                updatePsmtVal = new PsmtArrayItemUpdater((ArrayOPCItemInfo) itemInfo);
            }
            updaterMap.put(itemInfo.getOPCItem().getId(), updatePsmtVal);
        }
        for (CompositeItem compItem : composite) {
            PsmtCompositeUpdater updatePsmtVal = new PsmtCompositeUpdater(compItem);
            updaterCompMap.put(compItem, updatePsmtVal);
        }
        tables = dbStruct.prepareDatabase(database, opc, updaterMap, updaterCompMap, saveProperties);
        createPreparedStmts();
    }

    public void saveProperties() throws SQLException {
        final List<PstmtItemInfo> updaters;
        synchronized (this) {
            updaters = new ArrayList<PstmtItemInfo>(updaterMap.values());
        }
        dbStruct.saveProperties(updaters);
    }

    @Override
    public String getOutputTypeName() {
        return NAME;
    }

    @Override
    public synchronized void write2Output(OutputData data) throws Exception {
        Map<String, OPCItemData> itemDataMap = data.itemData;
        for (Entry<String, PstmtItemInfo> e : updaterMap.entrySet()) {
            e.getValue().updateVal(itemDataMap.get(e.getKey()));
        }
        Map<String, ScriptDataResult> compData = data.compData;
        for (Entry<CompositeItem, PsmtCompositeUpdater> e : updaterCompMap.entrySet()) {
            e.getValue().updateVal(compData.get(e.getKey().getName()));
        }
        try {
            for (TableData t : tables) {
                t.getStmt().executeUpdate();
            }
            database.getCon().commit();
        } catch (SQLException ex) {
            database.getCon().rollback();
            throw ex;
        }
    }

    @Override
    public synchronized void stop() {
        if (tables != null) {
            for (TableData t : tables) {
                DatabaseManager.closeStatement(t.getStmt());
            }
        }
        final Connection con = database.getCon();
        if (con != null) {
            try {
                con.setAutoCommit(true);
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        tables = null;
        updaterMap.clear();
        updaterCompMap.clear();
    }

    /**
     * Creates the SQL insert strings used to save the OPC data into the several
     * data tables in the database
     */
    private void createPreparedStmts() throws FatalInitializationException, RecoverableInitializationException {
        logger.info("Initializing: Creating the database prepared statements...");
        StringBuilder sqlInsert[] = new StringBuilder[tables.length];
        for (int i = 0; i < sqlInsert.length; i++) {
            sqlInsert[i] = new StringBuilder(100);
            sqlInsert[i].append("INSERT INTO ");
            sqlInsert[i].append(tables[i].getName());
            sqlInsert[i].append(" (t");
        }
        int[] index = new int[tables.length];
        for (int i = 0; i < index.length; i++) {
            index[i] = 0;
        }
        List<TableData> usedTables = new ArrayList<TableData>(tables.length);
        List<TableData> allTables = Arrays.asList(tables);
        List<PstmtUpdater> stmtUpdaters = new ArrayList<PstmtUpdater>(updaterMap.size() + updaterCompMap.size());
        stmtUpdaters.addAll(updaterMap.values());
        stmtUpdaters.addAll(updaterCompMap.values());
        for (PstmtUpdater updt : stmtUpdaters) {
            for (int k = 0; k < updt.getElementCount(); k++) {
                TableData table = updt.getTable(k);
                int tableIndex = allTables.indexOf(table);
                if (!usedTables.contains(table)) {
                    sqlInsert[tableIndex].append(',');
                    sqlInsert[tableIndex].append(updt.getTimeColName());
                    index[tableIndex]++;
                    updt.setTimeColStmtIndex(table, index[tableIndex]);
                    usedTables.add(table);
                }
                index[tableIndex]++;
                sqlInsert[tableIndex].append(',');
                sqlInsert[tableIndex].append(updt.getColName(k));
                updt.setElementStatementIndex(k, index[tableIndex]);
            }
            usedTables.clear();
        }
        for (int table = 0; table < tables.length; table++) {
            sqlInsert[table].append(") VALUES (CURRENT_TIMESTAMP");
            for (int i = index[table]; i > 0; i--) {
                sqlInsert[table].append(",?");
            }
            sqlInsert[table].append(")");
        }
        logger.info("Initializing: Creating the database prepared statements...");
        try {
            Connection con = database.getCon();
            con.setAutoCommit(false);
            for (int t = 0; t < tables.length; t++) {
                tables[t].setStmt(con.prepareStatement(sqlInsert[t].toString()));
                System.out.println(sqlInsert[t]);
            }
        } catch (SQLException ex) {
            throw new RecoverableInitializationException("Unable to create the prepared statements used to insert data in the database", ex);
        }
    }
}
