package com.kni.etl.ketl.transformation;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import com.kni.etl.EngineConstants;
import com.kni.etl.SharedCounter;
import com.kni.etl.dbutils.DatabaseColumnDefinition;
import com.kni.etl.dbutils.JDBCItemHelper;
import com.kni.etl.dbutils.JDBCStatementWrapper;
import com.kni.etl.dbutils.PrePostSQL;
import com.kni.etl.dbutils.ResourcePool;
import com.kni.etl.dbutils.StatementManager;
import com.kni.etl.dbutils.StatementWrapper;
import com.kni.etl.ketl.DBConnection;
import com.kni.etl.ketl.ETLInPort;
import com.kni.etl.ketl.ETLOutPort;
import com.kni.etl.ketl.ETLPort;
import com.kni.etl.ketl.ETLStep;
import com.kni.etl.ketl.KETLJob;
import com.kni.etl.ketl.exceptions.KETLError;
import com.kni.etl.ketl.exceptions.KETLThreadException;
import com.kni.etl.ketl.exceptions.KETLTransformException;
import com.kni.etl.ketl.lookup.LookupCreatorImpl;
import com.kni.etl.ketl.lookup.PersistentMap;
import com.kni.etl.ketl.smp.BatchManager;
import com.kni.etl.ketl.smp.ETLThreadManager;
import com.kni.etl.ketl.smp.TransformBatchManager;
import com.kni.etl.stringtools.NumberFormatter;
import com.kni.etl.util.XMLHelper;

/**
 * The Class DimensionTransformation.
 */
public class DimensionTransformation extends ETLTransformation implements DBConnection, TransformBatchManager, PrePostSQL, LookupCreatorImpl {

    @Override
    protected String getVersion() {
        return "$LastChangedRevision: 526 $";
    }

    /**
	 * The Class DimensionETLInPort.
	 */
    class DimensionETLInPort extends ETLInPort {

        /** The column. */
        DatabaseColumnDefinition mColumn = null;

        /** The effective date. */
        boolean sk = false, insert = false, update = false, compare = false, isColumn = false, effectiveDate = false;

        /** The sk col index. */
        int skColIndex = -1;

        /**
		 * Instantiates a new dimension ETL in port.
		 * 
		 * @param esOwningStep
		 *            the es owning step
		 * @param esSrcStep
		 *            the es src step
		 */
        public DimensionETLInPort(ETLStep esOwningStep, ETLStep esSrcStep) {
            super(esOwningStep, esSrcStep);
        }

        @Override
        public int initialize(Node xmlNode) throws ClassNotFoundException, KETLThreadException {
            int res = super.initialize(xmlNode);
            if (res != 0) return res;
            DatabaseColumnDefinition dcdNewColumn;
            dcdNewColumn = new DatabaseColumnDefinition(xmlNode, "", 0);
            NamedNodeMap attr = xmlNode.getAttributes();
            dcdNewColumn.setColumnName(this.getPortName());
            dcdNewColumn.setAlternateInsertValue(XMLHelper.getAttributeAsString(attr, DimensionTransformation.ALTERNATE_INSERT_VALUE, null));
            dcdNewColumn.setAlternateUpdateValue(XMLHelper.getAttributeAsString(attr, DimensionTransformation.ALTERNATE_UPDATE_VALUE, null));
            this.effectiveDate = XMLHelper.getAttributeAsBoolean(attr, DimensionTransformation.EFFECTIVE_DATE_ATTRIB, false);
            if (this.effectiveDate && ((Element) xmlNode).hasAttribute("DATATYPE") == false) ((Element) xmlNode).setAttribute("DATATYPE", "DATE");
            if (this.effectiveDate) {
                if (DimensionTransformation.this.effectiveDatePort == null) DimensionTransformation.this.effectiveDatePort = this; else throw new KETLThreadException("Only one effective date port is allowed", this);
            }
            int skIdx = XMLHelper.getAttributeAsInt(attr, DimensionTransformation.SK_ATTRIB, -1);
            if (skIdx != -1) {
                if (skIdx < 1) throw new KETLThreadException("Port " + this.mesStep.getName() + "." + this.getPortName() + " KEY order starts at 1, invalid value of " + skIdx, this);
                dcdNewColumn.setProperty(DatabaseColumnDefinition.SRC_UNIQUE_KEY);
                this.sk = true;
                this.skColIndex = skIdx - 1;
                DimensionTransformation.this.mSKColCount++;
            }
            if (XMLHelper.getAttributeAsBoolean(attr, DimensionTransformation.INSERT_ATTRIB, false)) {
                dcdNewColumn.setProperty(DatabaseColumnDefinition.INSERT_COLUMN);
                this.insert = true;
            }
            if (XMLHelper.getAttributeAsBoolean(attr, DimensionTransformation.UPDATE_ATTRIB, false)) {
                dcdNewColumn.setProperty(DatabaseColumnDefinition.UPDATE_COLUMN);
                this.update = true;
            }
            if (XMLHelper.getAttributeAsBoolean(attr, DimensionTransformation.COMPARE_ATTRIB, false)) {
                dcdNewColumn.setProperty(DatabaseColumnDefinition.UPDATE_TRIGGER_COLUMN);
                this.compare = true;
            }
            if (this.sk || this.insert || this.update || this.compare) this.mColumn = dcdNewColumn;
            return 0;
        }
    }

    /**
	 * The Class DimensionETLOutPort.
	 */
    class DimensionETLOutPort extends ETLOutPort {

        /** The out col index. */
        int outColIndex = -1;

        /** The pk. */
        boolean pk = false;

        /** The expiration_dt. */
        boolean expiration_dt = false;

        /** The dirty flag. */
        boolean dirtyFlag = false;

        /**
		 * Instantiates a new dimension ETL out port.
		 * 
		 * @param esOwningStep
		 *            the es owning step
		 * @param esSrcStep
		 *            the es src step
		 */
        public DimensionETLOutPort(ETLStep esOwningStep, ETLStep esSrcStep) {
            super(esOwningStep, esSrcStep);
        }

        @Override
        public String generateCode(int portReferenceIndex) throws KETLThreadException {
            if (this.pk) {
                return this.getCodeGenerationReferenceObject() + "[" + DimensionTransformation.this.getUsedPortIndex(DimensionTransformation.this.pkPort) + "] =  ((" + this.mesStep.getClass().getCanonicalName() + ")this.getOwner()).getPK(pInputRecords);";
            } else return super.generateCode(portReferenceIndex);
        }

        @Override
        public ETLPort getAssociatedInPort() throws KETLThreadException {
            if (this.pk) return null;
            return super.getAssociatedInPort();
        }

        @Override
        public String getCode() throws KETLThreadException {
            if (this.pk) return "";
            return super.getCode();
        }

        @Override
        public int initialize(Node xmlConfig) throws ClassNotFoundException, KETLThreadException {
            this.pk = XMLHelper.getAttributeAsBoolean(xmlConfig.getAttributes(), DimensionTransformation.PK_ATTRIB, false);
            this.expiration_dt = XMLHelper.getAttributeAsBoolean(xmlConfig.getAttributes(), DimensionTransformation.EXPIRATION_DATE_ATTRIB, false);
            this.dirtyFlag = XMLHelper.getAttributeAsBoolean(xmlConfig.getAttributes(), DimensionTransformation.DIRTY_FLAG_ATTRIB, false);
            if (this.pk && ((Element) xmlConfig).hasAttribute("DATATYPE") == false) ((Element) xmlConfig).setAttribute("DATATYPE", Integer.class.getCanonicalName());
            if (this.expiration_dt && ((Element) xmlConfig).hasAttribute("DATATYPE") == false) ((Element) xmlConfig).setAttribute("DATATYPE", "DATE");
            if (this.dirtyFlag && ((Element) xmlConfig).hasAttribute("DATATYPE") == false) ((Element) xmlConfig).setAttribute("DATATYPE", "BOOLEAN");
            if (this.pk) {
                this.outColIndex = DimensionTransformation.this.mPKColCount++;
            }
            int res = super.initialize(xmlConfig);
            if (res != 0) return res;
            if (this.pk && DimensionTransformation.this.pkPort == null) {
                DimensionTransformation.this.pkPort = this;
            } else if (this.pk && DimensionTransformation.this.pkPort != null) {
                throw new KETLThreadException("Only one primary key port is allowed", this);
            }
            if (this.dirtyFlag) {
                if (DimensionTransformation.this.dirtyFlagPort == null) DimensionTransformation.this.dirtyFlagPort = this; else throw new KETLThreadException("Only one dirty flag port is allowed", this);
            }
            if (this.expiration_dt) {
                if (DimensionTransformation.this.expirationDatePort == null) DimensionTransformation.this.expirationDatePort = this; else throw new KETLThreadException("Only one expiration date port is allowed", this);
            }
            return 0;
        }
    }

    /** The Constant ALTERNATE_INSERT_VALUE. */
    public static final String ALTERNATE_INSERT_VALUE = "ALTERNATE_INSERT_VALUE";

    /** The Constant ALTERNATE_UPDATE_VALUE. */
    public static final String ALTERNATE_UPDATE_VALUE = "ALTERNATE_UPDATE_VALUE";

    /** The Constant BATCH_ATTRIB. */
    public static final String BATCH_ATTRIB = "BATCHDATA";

    /** The Constant COMMITSIZE_ATTRIB. */
    private static final String COMMITSIZE_ATTRIB = "COMMITSIZE";

    /** The Constant COMPARE_ATTRIB. */
    public static final String COMPARE_ATTRIB = "COMPARE";

    /** The Constant HANDLER_ATTRIB. */
    public static final String HANDLER_ATTRIB = "HANDLER";

    /** The Constant INSERT_ATTRIB. */
    public static final String INSERT_ATTRIB = "INSERT";

    /** The Constant KEY_TABLE_ATTRIB. */
    private static final String KEY_TABLE_ATTRIB = "KEYTABLE";

    /** The Constant SCD_ATTRIB. */
    private static final String SCD_ATTRIB = "SCD";

    /** The Constant KEY_TABLE_ONLY. */
    private static final int KEY_TABLE_ONLY = 1;

    /** The Constant LOAD_KEY_TABLE_DIMENSION. */
    private static final int LOAD_KEY_TABLE_DIMENSION = 0;

    /** The Constant LOWER_CASE. */
    private static final int LOWER_CASE = 0;

    /** The Constant MAXTRANSACTIONSIZE_ATTRIB. */
    public static final String MAXTRANSACTIONSIZE_ATTRIB = "MAXTRANSACTIONSIZE";

    /** The Constant MIXED_CASE. */
    private static final int MIXED_CASE = 2;

    /** The Constant PK_ATTRIB. */
    public static final String PK_ATTRIB = "PK";

    /** The Constant EFFECTIVE_DATE_ATTRIB. */
    public static final String EFFECTIVE_DATE_ATTRIB = "EFFECTIVEDATE";

    /** The Constant DIRTY_FLAG_ATTRIB. */
    public static final String DIRTY_FLAG_ATTRIB = "DIRTYFLAG";

    /** The Constant EXPIRATION_DATE_ATTRIB. */
    public static final String EXPIRATION_DATE_ATTRIB = "EXPIRATIONDATE";

    /** The Constant SCHEMA_ATTRIB. */
    private static final String SCHEMA_ATTRIB = "SCHEMA";

    /** The Constant SK_ATTRIB. */
    public static final String SK_ATTRIB = "SK";

    /** The Constant TABLE_ATTRIB. */
    private static final String TABLE_ATTRIB = "TABLE";

    /** The Constant TABLE_ONLY. */
    private static final int TABLE_ONLY = 2;

    /** The Constant UPDATE_ATTRIB. */
    public static final String UPDATE_ATTRIB = "UPDATE";

    /** The Constant UPPER_CASE. */
    private static final int UPPER_CASE = 1;

    /** The cache persistence. */
    private int cachePersistence;

    /** The fire pre batch. */
    private boolean firePreBatch;

    /** The id quote. */
    private String idQuote;

    /** The id quote enabled. */
    private boolean idQuoteEnabled;

    /** The jdbc helper. */
    private JDBCItemHelper jdbcHelper;

    /** The lookup pending load. */
    private boolean lookupPendingLoad = true;

    /** The max char length. */
    private int maxCharLength;

    /** The batch counter. */
    private int mBatchCounter;

    /** The batch data. */
    private boolean mBatchData = true;

    /** The batch log. */
    ArrayList mBatchLog = new ArrayList();

    /** The mb reinit on error. */
    private boolean mbReinitOnError;

    /** The cache persistence ID. */
    private Integer mCachePersistenceID;

    /** The cache size. */
    private int mCacheSize;

    /** The mc DB connection. */
    private Connection mcDBConnection;

    /** The DB case. */
    private int mDBCase = -1;

    /** The failed batch elements. */
    private final Set mFailedBatchElements = new HashSet();

    /** The mi commit size. */
    private int miCommitSize;

    /** The mi field population order. */
    private ETLPort[] miFieldPopulationOrder;

    /** The mi insert count. */
    private int miInsertCount;

    /** The mi mode. */
    private int miMode;

    /** The incremental commit. */
    private boolean mIncrementalCommit;

    /** The mi retry batch. */
    private int miRetryBatch;

    /** The key source. */
    private SharedCounter mKeySource;

    /** The lookup. */
    private PersistentMap mLookup;

    /** The ms all columns. */
    private String msAllColumns;

    /** The ms in batch SQL statement. */
    private String msInBatchSQLStatement;

    /** The ms insert values. */
    private String msInsertValues;

    /** The PK col count. */
    public int mSKColCount = 0, mPKColCount = 0;

    /** The ms key table all columns. */
    private String msKeyTableAllColumns;

    /** The ms key table insert values. */
    private String msKeyTableInsertValues;

    /** The mstr key table name. */
    private String mstrKeyTableName;

    /** The mstr primary key columns. */
    private String mstrPrimaryKeyColumns;

    /** The mstr schema name. */
    private String mstrSchemaName;

    /** The mstr source key columns. */
    private String mstrSourceKeyColumns;

    /** The mstr table name. */
    private String mstrTableName;

    /** The used connections. */
    private final List mUsedConnections = new ArrayList();

    /** The expiration date port. */
    DimensionETLOutPort pkPort = null, dirtyFlagPort = null, expirationDatePort = null;

    /** The effective date port. */
    DimensionETLInPort effectiveDatePort = null;

    /** The record num batch start. */
    int recordNumBatchStart;

    /** The sk data. */
    Object[] skData;

    /** The sk indx. */
    private int[] skIndx;

    /** The stmt. */
    private StatementWrapper stmt;

    /** The str driver class. */
    private String strDriverClass = null;

    /** The str password. */
    private String strPassword = null;

    /** The str pre SQL. */
    private String strPreSQL = null;

    /** The str URL. */
    private String strURL = null;

    /** The str user name. */
    private String strUserName = null;

    /** The supports release savepoint. */
    private boolean supportsReleaseSavepoint;

    /** The supports set savepoint. */
    private boolean supportsSetSavepoint;

    /** The allow insert. */
    private boolean mAllowInsert;

    /**
	 * Instantiates a new dimension transformation.
	 * 
	 * @param pXMLConfig
	 *            the XML config
	 * @param pPartitionID
	 *            the partition ID
	 * @param pPartition
	 *            the partition
	 * @param pThreadManager
	 *            the thread manager
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public DimensionTransformation(Node pXMLConfig, int pPartitionID, int pPartition, ETLThreadManager pThreadManager) throws KETLThreadException {
        super(pXMLConfig, pPartitionID, pPartition, pThreadManager);
    }

    /**
	 * Builds the in batch SQL.
	 * 
	 * @return the string
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    protected String buildInBatchSQL() throws KETLThreadException {
        boolean multiStatement = this.msKeyTableAllColumns != null && this.msAllColumns != null;
        String template = null;
        if (this.msKeyTableAllColumns != null) {
            template = this.getStepTemplate(this.getGroup(), multiStatement ? "MULTIINSERT" : "INSERT", true);
            template = EngineConstants.replaceParameterV2(template, "TABLENAME", this.mstrKeyTableName);
            template = EngineConstants.replaceParameterV2(template, "SCHEMANAME", this.mstrSchemaName);
            template = EngineConstants.replaceParameterV2(template, "DESTINATIONCOLUMNS", this.getKeyTableAllColumns());
            template = EngineConstants.replaceParameterV2(template, "VALUES", this.getKeyTableInsertValues());
            template = EngineConstants.replaceParameterV2(template, "NAMEDVALUES", this.getKeyTableAllColumns());
        }
        if (this.msAllColumns != null) {
            template = multiStatement ? template + (this.getStepTemplate(this.getGroup(), "STATEMENTSEPERATOR", true) == null ? "" : this.getStepTemplate(this.getGroup(), "STATEMENTSEPERATOR", true)) : template;
            template = template + this.getStepTemplate(this.getGroup(), multiStatement ? "MULTIINSERT" : "INSERT", true);
            template = EngineConstants.replaceParameterV2(template, "TABLENAME", this.mstrTableName);
            template = EngineConstants.replaceParameterV2(template, "SCHEMANAME", this.mstrSchemaName);
            template = EngineConstants.replaceParameterV2(template, "DESTINATIONCOLUMNS", this.getAllColumns());
            template = EngineConstants.replaceParameterV2(template, "VALUES", this.getInsertValues());
            template = EngineConstants.replaceParameterV2(template, "NAMEDVALUES", this.getAllColumns());
        }
        if (multiStatement) {
            String wrapper = this.getStepTemplate(this.getGroup(), "MULTIINSERTWRAPPER", true);
            template = EngineConstants.replaceParameterV2(wrapper, "STATEMENT", template);
            template = EngineConstants.replaceParameterV2(template, "VALUES", this.getInsertValues());
        }
        return template;
    }

    /**
	 * Clear batch log batch.
	 */
    private void clearBatchLogBatch() {
        this.mBatchLog.clear();
        this.mFailedBatchElements.clear();
    }

    @Override
    protected void close(boolean success, boolean jobFailed) {
        try {
            if (this.lookupLocked) {
                ((KETLJob) this.getJobExecutor().getCurrentETLJob()).releaseLookupWriteLock(this.getName(), this);
            }
            if (this.mcDBConnection != null && this.mIncrementalCommit == false && success == false && this.getRecordsProcessed() > 0) {
                this.mcDBConnection.rollback();
            }
        } catch (SQLException e) {
            ResourcePool.LogException(e, this);
        }
        try {
            if (this.stmt != null) this.stmt.close();
        } catch (SQLException e) {
            ResourcePool.LogException(e, this);
        }
        if (this.mcDBConnection != null) ResourcePool.releaseConnection(this.mcDBConnection);
        if (this.cachePersistence == EngineConstants.JOB_PERSISTENCE) {
            ((KETLJob) this.getJobExecutor().getCurrentETLJob()).deleteLookup(this.getName());
        }
    }

    /** The lookup locked. */
    private boolean lookupLocked = false;

    @Override
    public int complete() throws KETLThreadException {
        int res = super.complete();
        if (res < 0) ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "Error during final batch, see previous messages"); else {
            try {
                this.executePostStatements();
            } catch (Exception e) {
                ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "Running post load " + e.getMessage());
                res = -6;
            }
        }
        if (this.lookupLocked) {
            ((KETLJob) this.getJobExecutor().getCurrentETLJob()).releaseLookupWriteLock(this.getName(), this);
        }
        return res;
    }

    /**
	 * Creates the new surrogate key.
	 * 
	 * @param pInputRecords
	 *            the input records
	 * 
	 * @return the integer
	 * 
	 * @throws KETLTransformException
	 *             the KETL transform exception
	 */
    private final Integer createNewSurrogateKey(Object[] pInputRecords) throws KETLTransformException {
        Integer data = this.mKeySource.increment(1);
        try {
            for (int i = 0; i < this.miFieldPopulationOrder.length; i++) {
                ETLPort idx = this.miFieldPopulationOrder[i];
                if (idx == this.pkPort) this.stmt.setParameterFromClass(i + 1, Integer.class, data, this.maxCharLength, this.pkPort.getXMLConfig()); else {
                    ETLInPort inport = (ETLInPort) idx;
                    this.stmt.setParameterFromClass(i + 1, inport.getPortClass(), inport.isConstant() ? inport.getConstantValue() : pInputRecords[inport.getSourcePortIndex()], this.maxCharLength, inport.getXMLConfig());
                }
            }
            if (this.mBatchData) {
                this.stmt.addBatch();
                this.logBatch(pInputRecords);
                this.mBatchCounter++;
            } else {
                this.stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new KETLTransformException(e);
        }
        try {
            this.putKeyArrayDataArray(pInputRecords, data);
        } catch (Error e) {
            throw new KETLTransformException(e.getMessage());
        }
        return data;
    }

    public void executePostBatchStatements() throws SQLException {
        StatementManager.executeStatements(this, this, "POSTBATCHSQL");
    }

    public void executePostStatements() throws SQLException {
        StatementManager.executeStatements(this, this, "POSTSQL", StatementManager.END);
    }

    public void executePreBatchStatements() throws SQLException {
        StatementManager.executeStatements(this, this, "PREBATCHSQL");
    }

    public void executePreStatements() throws SQLException {
        StatementManager.executeStatements(this, this, "PRESQL", StatementManager.START);
    }

    public Object[][] finishBatch(Object[][] data, int len) throws KETLTransformException {
        int result = 0;
        try {
            if (this.mBatchData && (this.mBatchCounter >= this.miCommitSize || (len == BatchManager.LASTBATCH && this.mBatchCounter > 0))) {
                boolean errorsOccured = false;
                Savepoint savepoint = null;
                try {
                    if (this.supportsSetSavepoint) {
                        savepoint = this.mcDBConnection.setSavepoint();
                    }
                    Exception e1 = null;
                    int[] res = null;
                    try {
                        res = this.stmt.executeBatch();
                        if (this.supportsReleaseSavepoint && savepoint != null) {
                            this.mcDBConnection.releaseSavepoint(savepoint);
                        }
                    } catch (BatchUpdateException e) {
                        if (savepoint != null) this.mcDBConnection.rollback(savepoint); else res = e.getUpdateCounts();
                        e1 = e;
                        errorsOccured = true;
                    }
                    if (errorsOccured && res == null) {
                        for (int i = 0; i < this.mBatchLog.size(); i++) {
                            if (this.miRetryBatch == 0) this.incrementErrorCount(e1 == null ? new KETLTransformException("Failed to submit record " + (i + 1 + this.miInsertCount)) : new KETLTransformException(e1), (Object[]) this.mBatchLog.get(i), i + 1 + this.miInsertCount); else this.mFailedBatchElements.add(i);
                        }
                    } else {
                        int rLen = res.length;
                        for (int i = 0; i < rLen; i++) {
                            if (res[i] == Statement.EXECUTE_FAILED) {
                                this.mFailedBatchElements.add(i);
                                if (this.miRetryBatch == 0) this.incrementErrorCount(e1 == null ? new KETLTransformException("Failed to submit record " + (i + 1 + this.miInsertCount)) : new KETLTransformException(e1), (Object[]) this.mBatchLog.get(rLen), i + 1 + this.miInsertCount);
                            } else {
                                result += res[i] >= 0 ? res[i] : 1;
                            }
                        }
                    }
                } catch (SQLException e) {
                    throw new KETLTransformException(e);
                }
                if (errorsOccured && this.miRetryBatch > 0) {
                    result = this.retryBatch();
                }
                this.clearBatchLogBatch();
                this.miInsertCount += this.mBatchCounter;
                this.mBatchCounter = 0;
                if (this.mIncrementalCommit) this.mcDBConnection.commit();
                this.executePostBatchStatements();
                this.firePreBatch = true;
            } else if (this.mBatchData == false) {
                if (this.mIncrementalCommit) this.mcDBConnection.commit();
            }
        } catch (SQLException e) {
            throw new KETLTransformException(e);
        }
        return data;
    }

    /**
	 * Gets the all columns.
	 * 
	 * @return the all columns
	 */
    String getAllColumns() {
        return this.msAllColumns;
    }

    public Connection getConnection() throws SQLException, ClassNotFoundException {
        return this.mcDBConnection;
    }

    /**
	 * Gets the insert values.
	 * 
	 * @return the insert values
	 */
    String getInsertValues() {
        return this.msInsertValues;
    }

    /**
	 * Gets the key table all columns.
	 * 
	 * @return the key table all columns
	 */
    String getKeyTableAllColumns() {
        return this.msKeyTableAllColumns;
    }

    /**
	 * Gets the key table insert values.
	 * 
	 * @return the key table insert values
	 */
    String getKeyTableInsertValues() {
        return this.msKeyTableInsertValues;
    }

    public PersistentMap getLookup() {
        Class[] types = new Class[this.mSKColCount];
        Class[] values = new Class[this.mPKColCount];
        String[] valueFields = new String[this.mPKColCount];
        for (ETLInPort element : this.mInPorts) {
            DimensionETLInPort port = (DimensionETLInPort) element;
            if (port.skColIndex != -1) types[port.skColIndex] = port.getPortClass();
        }
        for (ETLOutPort element : this.mOutPorts) {
            DimensionETLOutPort port = (DimensionETLOutPort) element;
            if (port.outColIndex != -1) {
                values[port.outColIndex] = port.getPortClass();
                valueFields[port.outColIndex] = port.mstrName;
            }
        }
        String lookupClass = XMLHelper.getAttributeAsString(this.getXMLConfig().getAttributes(), "LOOKUPCLASS", EngineConstants.getDefaultLookupClass());
        try {
            return EngineConstants.getInstanceOfPersistantMap(lookupClass, this.getName(), this.mCacheSize, this.mCachePersistenceID, EngineConstants.CACHE_PATH, types, values, valueFields, this.cachePersistence == EngineConstants.JOB_PERSISTENCE ? true : false);
        } catch (Throwable e) {
            ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.WARNING_MESSAGE, "Lookup cache creation failed, trying again, check stack trace");
            e.printStackTrace();
            try {
                return EngineConstants.getInstanceOfPersistantMap(lookupClass, this.getName(), this.mCacheSize, this.mCachePersistenceID, EngineConstants.CACHE_PATH, types, values, valueFields, this.cachePersistence == EngineConstants.JOB_PERSISTENCE ? true : false);
            } catch (Throwable e1) {
                e1.printStackTrace();
                throw new KETLError("LOOKUPCLASS " + lookupClass + " could not be found: " + e.getMessage(), e);
            }
        }
    }

    @Override
    protected ETLInPort getNewInPort(ETLStep srcStep) {
        return new DimensionETLInPort(this, srcStep);
    }

    @Override
    protected ETLOutPort getNewOutPort(ETLStep srcStep) {
        return new DimensionETLOutPort(this, srcStep);
    }

    /**
	 * Gets the PK.
	 * 
	 * @param pInputRecords
	 *            the input records
	 * 
	 * @return the PK
	 * 
	 * @throws KETLTransformException
	 *             the KETL transform exception
	 */
    public final Integer getPK(Object[] pInputRecords) throws KETLTransformException {
        Integer res;
        try {
            res = this.getSurrogateKey(pInputRecords);
        } catch (Error e) {
            throw new KETLTransformException(e.getMessage());
        }
        if (res == null && this.mAllowInsert) return this.createNewSurrogateKey(pInputRecords);
        return res;
    }

    /**
	 * Gets the primary key columns.
	 * 
	 * @return the primary key columns
	 */
    private String getPrimaryKeyColumns() {
        return this.idQuote + this.pkPort.mstrName + this.idQuote;
    }

    /**
	 * Gets the source key columns.
	 * 
	 * @return the source key columns
	 */
    private String getSourceKeyColumns() {
        String res = null;
        for (ETLInPort element : this.mInPorts) {
            if (((DimensionETLInPort) element).sk) {
                res = (res == null ? ((DimensionETLInPort) element).mColumn.getColumnName(this.idQuote, this.mDBCase) : res + "," + ((DimensionETLInPort) element).mColumn.getColumnName(this.idQuote, this.mDBCase));
            }
        }
        return res;
    }

    /**
	 * Gets the surrogate key.
	 * 
	 * @param pInputRecords
	 *            the input records
	 * 
	 * @return the surrogate key
	 */
    private final Integer getSurrogateKey(Object[] pInputRecords) {
        for (int i = 0; i < this.mSKColCount; i++) this.skData[i] = pInputRecords[this.skIndx[i]];
        Object res = this.mLookup.get(this.skData, null);
        if (this.debug()) ResourcePool.LogMessage(this, ResourcePool.DEBUG_MESSAGE, "getSurrogateKey:In->" + java.util.Arrays.toString(this.skData) + ", Out->" + res);
        return (Integer) res;
    }

    /** The SCD. */
    private int mSCD;

    /** The purge cache. */
    private boolean purgeCache;

    private Properties mDatabaseProperties;

    private Properties getDatabaseProperties() {
        return this.mDatabaseProperties;
    }

    private void setDatabaseProperties(Map<String, Object> parameterListValues) throws Exception {
        this.mDatabaseProperties = JDBCItemHelper.getProperties(parameterListValues);
    }

    @Override
    protected int initialize(Node xmlConfig) throws KETLThreadException {
        int res = super.initialize(xmlConfig);
        if (res != 0) return res;
        this.mKeySource = this.getJobExecutor().getCurrentETLJob().getCounter(this.getName(), this.pkPort == null ? int.class : this.pkPort.getPortClass());
        this.strUserName = this.getParameterValue(0, DBConnection.USER_ATTRIB);
        this.strPassword = this.getParameterValue(0, DBConnection.PASSWORD_ATTRIB);
        this.strURL = this.getParameterValue(0, DBConnection.URL_ATTRIB);
        this.strDriverClass = this.getParameterValue(0, DBConnection.DRIVER_ATTRIB);
        this.strPreSQL = this.getParameterValue(0, DBConnection.PRESQL_ATTRIB);
        try {
            this.setDatabaseProperties(this.getParameterListValues(0));
        } catch (Exception e1) {
            throw new KETLThreadException(e1, this);
        }
        NamedNodeMap nmAttrs = xmlConfig.getAttributes();
        int minSize = NumberFormatter.convertToBytes(EngineConstants.getDefaultCacheSize());
        this.cachePersistence = EngineConstants.JOB_PERSISTENCE;
        this.mSCD = XMLHelper.getAttributeAsInt(xmlConfig.getAttributes(), DimensionTransformation.SCD_ATTRIB, 1);
        this.purgeCache = XMLHelper.getAttributeAsBoolean(xmlConfig.getAttributes(), "PURGECACHE", true);
        ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Slowly changing dimension mode = " + this.mSCD);
        String tmp = XMLHelper.getAttributeAsString(xmlConfig.getAttributes(), "PERSISTENCE", null);
        if (tmp == null || tmp.equalsIgnoreCase("JOB")) {
            this.mCachePersistenceID = ((Long) this.getJobExecutionID()).intValue();
            this.cachePersistence = EngineConstants.JOB_PERSISTENCE;
        } else if (tmp.equalsIgnoreCase("LOAD")) {
            this.mCachePersistenceID = this.mkjExecutor.getCurrentETLJob().getLoadID();
            this.cachePersistence = EngineConstants.LOAD_PERSISTENCE;
        } else if (tmp.equalsIgnoreCase("STATIC")) {
            this.cachePersistence = EngineConstants.STATIC_PERSISTENCE;
            this.mCachePersistenceID = null;
        } else throw new KETLThreadException("PERSISTENCE has to be either JOB,LOAD or STATIC", this);
        this.mCacheSize = NumberFormatter.convertToBytes(XMLHelper.getAttributeAsString(xmlConfig.getAttributes(), "CACHESIZE", null));
        if (this.mCacheSize == -1) this.mCacheSize = minSize;
        if (this.mCacheSize < minSize) {
            ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Cache cannot be less than 64kb, defaulting to 64kb");
            this.mCacheSize = minSize;
        }
        if (this.mSKColCount > 6) {
            ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Currently lookups are limited to no more than 4 keys, unless you use a an array object to represent the compound key");
        } else if (this.mSKColCount < 1) throw new KETLThreadException("Source key has not been specified e.g. SK=\"1\"", this);
        if (this.purgeCache) ((KETLJob) this.getJobExecutor().getCurrentETLJob()).deleteLookup(this.getName());
        this.mLookup = ((KETLJob) this.getJobExecutor().getCurrentETLJob()).registerLookupWriteLock(this.getName(), this, this.cachePersistence);
        this.lookupLocked = true;
        try {
            this.mcDBConnection = ResourcePool.getConnection(this.strDriverClass, this.strURL, this.strUserName, this.strPassword, this.strPreSQL, true, this.getDatabaseProperties());
            this.mUsedConnections.add(this.mcDBConnection);
            DatabaseMetaData md = this.mcDBConnection.getMetaData();
            this.setGroup(EngineConstants.cleanseDatabaseName(md.getDatabaseProductName()));
            this.maxCharLength = md.getMaxCharLiteralLength();
            this.supportsSetSavepoint = md.supportsSavepoints();
            this.mBatchData = XMLHelper.getAttributeAsBoolean(nmAttrs, DimensionTransformation.BATCH_ATTRIB, this.mBatchData);
            this.idQuoteEnabled = XMLHelper.getAttributeAsBoolean(nmAttrs, "IDQUOTE", false);
            String hdl = XMLHelper.getAttributeAsString(nmAttrs, DimensionTransformation.HANDLER_ATTRIB, null);
            this.jdbcHelper = this.instantiateHelper(hdl);
            if (md.storesUpperCaseIdentifiers()) {
                this.mDBCase = DimensionTransformation.UPPER_CASE;
            } else if (md.storesLowerCaseIdentifiers()) {
                this.mDBCase = DimensionTransformation.LOWER_CASE;
            } else if (md.storesMixedCaseIdentifiers()) {
                this.mDBCase = DimensionTransformation.MIXED_CASE;
            }
            this.mstrTableName = this.setDBCase(XMLHelper.getAttributeAsString(nmAttrs, DimensionTransformation.TABLE_ATTRIB, null));
            this.mstrSchemaName = this.setDBCase(XMLHelper.getAttributeAsString(nmAttrs, DimensionTransformation.SCHEMA_ATTRIB, null));
            boolean namedValueList = false;
            tmp = XMLHelper.getAttributeAsString(nmAttrs, "MODE", "BOTH");
            if (tmp.equalsIgnoreCase("BOTH")) {
                this.miMode = DimensionTransformation.LOAD_KEY_TABLE_DIMENSION;
                namedValueList = Boolean.parseBoolean(this.getStepTemplate(this.getGroup(), "NAMEDVALUELIST", true));
            } else if (tmp.equalsIgnoreCase("KEYTABLEONLY")) {
                this.miMode = DimensionTransformation.KEY_TABLE_ONLY;
            } else if (tmp.equalsIgnoreCase("TABLEONLY")) this.miMode = DimensionTransformation.TABLE_ONLY; else throw new KETLThreadException("Invalid MODE, valid values are BOTH (default), KEYTABLEONLY and TABLEONLY", this);
            if (this.idQuoteEnabled) {
                this.idQuote = md.getIdentifierQuoteString();
                if (this.idQuote == null || this.idQuote.equals(" ")) this.idQuote = "";
            } else {
                this.idQuote = "";
            }
            ResultSet rsDBResultSet = md.getTables(null, this.mstrSchemaName, this.mstrTableName, null);
            boolean tableFound = false;
            while (rsDBResultSet.next()) {
                tableFound = true;
                if (this.mstrSchemaName == null) this.mstrSchemaName = rsDBResultSet.getString("TABLE_SCHEM");
            }
            rsDBResultSet.close();
            if (tableFound == false) {
                throw new KETLThreadException("Dimension table does not exists, or could not be found - " + this.mstrSchemaName + "." + this.mstrTableName, this);
            }
            StringBuffer allColumns = new StringBuffer();
            StringBuffer insertValues = new StringBuffer();
            ArrayList fieldPopulationOrder = new ArrayList();
            int cnt;
            if (this.miMode == DimensionTransformation.LOAD_KEY_TABLE_DIMENSION || this.miMode == DimensionTransformation.KEY_TABLE_ONLY) {
                this.mstrKeyTableName = this.setDBCase(XMLHelper.getAttributeAsString(nmAttrs, DimensionTransformation.KEY_TABLE_ATTRIB, this.mstrTableName + "_KEY"));
                cnt = 0;
                StringBuilder keyTableAllColumns = new StringBuilder();
                StringBuilder keyTableInsertValues = new StringBuilder();
                if (this.pkPort != null) {
                    keyTableAllColumns.append(this.idQuote + this.pkPort.mstrName + this.idQuote);
                    keyTableInsertValues.append('?');
                    if (namedValueList == false) fieldPopulationOrder.add(this.pkPort);
                    cnt++;
                }
                for (ETLInPort element : this.mInPorts) {
                    DimensionETLInPort port = (DimensionETLInPort) element;
                    if (port.mColumn != null && port.sk) {
                        if (cnt > 0) {
                            keyTableAllColumns.append(',');
                            keyTableInsertValues.append(',');
                        }
                        keyTableAllColumns.append(port.mColumn.getColumnName(this.idQuote, this.mDBCase));
                        keyTableInsertValues.append('?');
                        if (namedValueList == false) fieldPopulationOrder.add(port);
                        cnt++;
                    }
                }
                this.setKeyTableAllColumns(keyTableAllColumns.toString());
                this.setKeyTableInsertValues(keyTableInsertValues.toString());
            }
            if (this.miMode == DimensionTransformation.LOAD_KEY_TABLE_DIMENSION || this.miMode == DimensionTransformation.TABLE_ONLY) {
                cnt = 0;
                if (this.pkPort != null) {
                    allColumns.append(this.idQuote + this.pkPort.mstrName + this.idQuote);
                    if (namedValueList) insertValues.append("? as " + this.idQuote + this.pkPort.mstrName + this.idQuote); else insertValues.append('?');
                    fieldPopulationOrder.add(this.pkPort);
                    cnt++;
                }
                for (ETLInPort element : this.mInPorts) {
                    DimensionETLInPort port = (DimensionETLInPort) element;
                    if (port.mColumn != null) {
                        if (cnt > 0) {
                            allColumns.append(',');
                            insertValues.append(',');
                        }
                        allColumns.append(port.mColumn.getColumnName(this.idQuote, this.mDBCase));
                        if (namedValueList) insertValues.append("? as " + port.mColumn.getColumnName(this.idQuote, this.mDBCase)); else insertValues.append('?');
                        fieldPopulationOrder.add(port);
                        cnt++;
                    }
                }
                this.setAllColumns(allColumns.toString());
                this.setInsertValues(insertValues.toString());
            }
            this.miFieldPopulationOrder = new ETLPort[fieldPopulationOrder.size()];
            fieldPopulationOrder.toArray(this.miFieldPopulationOrder);
            this.msInBatchSQLStatement = this.buildInBatchSQL();
            this.stmt = this.prepareStatementWrapper(this.mcDBConnection, this.msInBatchSQLStatement, this.jdbcHelper);
            if (this.supportsSetSavepoint) {
                Savepoint sPoint = null;
                try {
                    sPoint = this.mcDBConnection.setSavepoint();
                } catch (SQLException e) {
                    this.supportsSetSavepoint = false;
                }
                if (sPoint != null) {
                    try {
                        this.mcDBConnection.releaseSavepoint(sPoint);
                        this.supportsReleaseSavepoint = true;
                    } catch (SQLException e) {
                        this.supportsReleaseSavepoint = false;
                    }
                }
            }
            this.miRetryBatch = XMLHelper.getAttributeAsInt(nmAttrs, "RETRYBATCH", 1);
            this.mbReinitOnError = XMLHelper.getAttributeAsBoolean(nmAttrs, "RECONNECTONERROR", true);
            this.mIncrementalCommit = XMLHelper.getAttributeAsBoolean(nmAttrs, "INCREMENTALCOMMIT", true);
            if (this.mIncrementalCommit == false && this.supportsSetSavepoint == false) {
                throw new KETLThreadException("Incremental commit cannot be disabled for database's that do not support savepoints", this);
            }
        } catch (Exception e) {
            ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, e.toString());
            return -1;
        }
        this.mAllowInsert = XMLHelper.getAttributeAsBoolean(nmAttrs, "INSERT", true);
        this.miCommitSize = XMLHelper.getAttributeAsInt(nmAttrs, DimensionTransformation.COMMITSIZE_ATTRIB, this.batchSize);
        this.mstrPrimaryKeyColumns = this.getPrimaryKeyColumns();
        this.mstrSourceKeyColumns = this.getSourceKeyColumns();
        try {
            if (this.miMode == DimensionTransformation.LOAD_KEY_TABLE_DIMENSION || this.miMode == DimensionTransformation.KEY_TABLE_ONLY) {
                this.prepareForKeyTable();
            }
            this.executePreStatements();
        } catch (SQLException e) {
            throw new KETLThreadException(e, this);
        }
        this.skData = new Object[this.mSKColCount];
        this.skIndx = new int[this.mSKColCount];
        for (ETLInPort element : this.mInPorts) if (((DimensionETLInPort) element).skColIndex != -1) this.skIndx[((DimensionETLInPort) element).skColIndex] = element.getSourcePortIndex();
        return 0;
    }

    public Object[][] initializeBatch(Object[][] data, int len) throws KETLTransformException {
        try {
            if (this.lookupPendingLoad) {
                this.lookupPendingLoad = false;
                this.seedLookup();
            }
            if (this.firePreBatch && this.mBatchData) {
                this.executePreBatchStatements();
                this.recordNumBatchStart = this.getRecordsProcessed();
                this.firePreBatch = false;
            }
        } catch (Exception e) {
            throw new KETLTransformException(e);
        }
        return data;
    }

    /**
	 * Instantiate helper.
	 * 
	 * @param hdl
	 *            the hdl
	 * 
	 * @return the JDBC item helper
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    protected JDBCItemHelper instantiateHelper(String hdl) throws KETLThreadException {
        if (hdl == null) return new JDBCItemHelper(); else {
            try {
                Class cl = Class.forName(hdl);
                return (JDBCItemHelper) cl.newInstance();
            } catch (Exception e) {
                throw new KETLThreadException("HANDLER class not found", e, this);
            }
        }
    }

    /**
	 * Log batch.
	 * 
	 * @param inputRecords
	 *            the input records
	 */
    private void logBatch(Object[] inputRecords) {
        this.mBatchLog.add(inputRecords);
    }

    /**
	 * Prepare for key table.
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    private void prepareForKeyTable() throws KETLThreadException {
        String template = null;
        synchronized (this.mKeySource) {
            try {
                Statement mStmt = this.mcDBConnection.createStatement();
                template = this.getStepTemplate(this.getGroup(), "CHECKFORKEYTABLE", true);
                template = EngineConstants.replaceParameterV2(template, "TABLENAME", this.mstrKeyTableName);
                template = EngineConstants.replaceParameterV2(template, "SCHEMANAME", this.mstrSchemaName);
                boolean exists = false;
                try {
                    ResultSet rs = mStmt.executeQuery(template);
                    while (rs.next()) {
                        exists = true;
                    }
                    rs.close();
                } catch (Exception e) {
                    mStmt.getConnection().rollback();
                }
                if (exists == false) {
                    template = this.getStepTemplate(this.getGroup(), "CREATEKEYTABLE", true);
                    template = EngineConstants.replaceParameterV2(template, "KEYTABLENAME", this.mstrKeyTableName);
                    template = EngineConstants.replaceParameterV2(template, "TABLENAME", this.mstrTableName);
                    template = EngineConstants.replaceParameterV2(template, "SCHEMANAME", this.mstrSchemaName);
                    template = EngineConstants.replaceParameterV2(template, "PK_COLUMNS", this.mstrPrimaryKeyColumns);
                    template = EngineConstants.replaceParameterV2(template, "SK_COLUMNS", this.mstrSourceKeyColumns);
                    mStmt.executeUpdate(template);
                    template = this.getStepTemplate(this.getGroup(), "CREATEKEYTABLEPKINDEX", true);
                    template = EngineConstants.replaceParameterV2(template, "COLUMNS", this.mstrPrimaryKeyColumns);
                    template = EngineConstants.replaceParameterV2(template, "TABLENAME", this.mstrKeyTableName);
                    template = EngineConstants.replaceParameterV2(template, "SCHEMANAME", this.mstrSchemaName);
                    mStmt.executeUpdate(template);
                    template = this.getStepTemplate(this.getGroup(), "CREATEKEYTABLESKINDEX", true);
                    template = EngineConstants.replaceParameterV2(template, "COLUMNS", this.mstrSourceKeyColumns);
                    template = EngineConstants.replaceParameterV2(template, "TABLENAME", this.mstrKeyTableName);
                    template = EngineConstants.replaceParameterV2(template, "SCHEMANAME", this.mstrSchemaName);
                    mStmt.executeUpdate(template);
                }
            } catch (SQLException e) {
                throw new KETLThreadException("Error executing statement " + template, e, this);
            }
        }
    }

    /**
	 * Prepare statement wrapper.
	 * 
	 * @param Connection
	 *            the connection
	 * @param sql
	 *            the sql
	 * @param jdbcHelper
	 *            the jdbc helper
	 * 
	 * @return the statement wrapper
	 * 
	 * @throws SQLException
	 *             the SQL exception
	 */
    StatementWrapper prepareStatementWrapper(Connection Connection, String sql, JDBCItemHelper jdbcHelper) throws SQLException {
        return JDBCStatementWrapper.prepareStatement(Connection, sql, jdbcHelper);
    }

    /**
	 * Put key array data array.
	 * 
	 * @param o
	 *            the o
	 * @param data
	 *            the data
	 */
    private void putKeyArrayDataArray(Object[] o, Integer data) {
        Object[] elements = new Object[this.mSKColCount];
        for (int i = 0; i < this.mSKColCount; i++) {
            elements[i] = o[this.skIndx[i]];
        }
        ResourcePool.LogMessage(this, ResourcePool.DEBUG_MESSAGE, "putKeyArrayDataArray:" + java.util.Arrays.toString(elements));
        this.mLookup.put(elements, new Object[] { data });
    }

    /**
	 * Retry batch.
	 * 
	 * @return the int
	 * 
	 * @throws KETLTransformException
	 *             the KETL transform exception
	 */
    private int retryBatch() throws KETLTransformException {
        ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Retrying records in batch, to identify invalid records");
        int result = 0;
        for (int r = 0; r < this.miRetryBatch; r++) {
            int errorCount = 0, submitted = 0;
            try {
                this.stmt.close();
                this.stmt = this.prepareStatementWrapper(this.mcDBConnection, this.msInBatchSQLStatement, this.jdbcHelper);
            } catch (SQLException e) {
                throw new KETLTransformException(e);
            }
            for (int x = 0; x < this.mBatchLog.size(); x++) {
                Object[] record = (Object[]) this.mBatchLog.get(x);
                if (this.mFailedBatchElements.contains(x)) {
                    try {
                        if (this.mbReinitOnError) {
                            this.stmt.close();
                            this.stmt = this.prepareStatementWrapper(this.mcDBConnection, this.msInBatchSQLStatement, this.jdbcHelper);
                        }
                        for (int i = 0; i < this.miFieldPopulationOrder.length; i++) {
                            ETLPort idx = this.miFieldPopulationOrder[i];
                            if (idx == this.pkPort) {
                                try {
                                    this.stmt.setParameterFromClass(i + 1, Integer.class, this.getSurrogateKey(record), this.maxCharLength, this.pkPort.getXMLConfig());
                                } catch (Error e) {
                                    throw new KETLTransformException(e.getMessage());
                                }
                            } else {
                                ETLInPort inport = (ETLInPort) idx;
                                this.stmt.setParameterFromClass(i + 1, inport.getPortClass(), inport.isConstant() ? inport.getConstantValue() : record[inport.getSourcePortIndex()], this.maxCharLength, inport.getXMLConfig());
                            }
                        }
                        submitted++;
                        this.stmt.executeUpdate();
                        result++;
                        this.mFailedBatchElements.remove(x);
                        if (this.mIncrementalCommit) this.mcDBConnection.commit();
                    } catch (SQLException e) {
                        errorCount++;
                        if (r == this.miRetryBatch - 1) this.incrementErrorCount(new KETLTransformException("Record " + (this.miInsertCount + x + 1) + " failed to submit, " + e.toString(), e), record, this.miInsertCount + x + 1);
                        try {
                            this.stmt.close();
                            this.stmt = this.prepareStatementWrapper(this.mcDBConnection, this.msInBatchSQLStatement, this.jdbcHelper);
                        } catch (SQLException e1) {
                            throw new KETLTransformException(e1);
                        }
                    }
                }
            }
            ResourcePool.LogMessage(this, ResourcePool.WARNING_MESSAGE, "Batch retry attempt " + (r + 1) + " of " + this.miRetryBatch + ", Records resubmitted: " + submitted + ", errors: " + errorCount);
        }
        return result;
    }

    /**
	 * Seed lookup.
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    protected void seedLookup() throws KETLThreadException {
        this.setWaiting("lookup to seed");
        String template = this.getStepTemplate(this.getGroup(), "SEEDLOOKUP", true);
        if (this.msKeyTableAllColumns != null) {
            template = EngineConstants.replaceParameterV2(template, "TABLENAME", this.mstrKeyTableName);
            template = EngineConstants.replaceParameterV2(template, "SCHEMANAME", this.mstrSchemaName);
        }
        if (this.msAllColumns != null) {
            template = EngineConstants.replaceParameterV2(template, "TABLENAME", this.mstrTableName);
            template = EngineConstants.replaceParameterV2(template, "SCHEMANAME", this.mstrSchemaName);
        }
        template = EngineConstants.replaceParameterV2(template, "PKCOLUMNS", this.getPrimaryKeyColumns());
        template = EngineConstants.replaceParameterV2(template, "SKCOLUMNS", this.getSourceKeyColumns());
        template = EngineConstants.replaceParameterV2(template, "PARTITIONS", Integer.toString(this.partitions));
        template = EngineConstants.replaceParameterV2(template, "PARTITIONID", Integer.toString(this.partitionID));
        Statement mStmt = null;
        try {
            mStmt = this.mcDBConnection.createStatement();
            ResultSet rs = mStmt.executeQuery(template);
            Object[] key = new Object[this.mInPorts.length];
            int x = 0;
            while (rs.next()) {
                for (int i = 0; i < this.mSKColCount; i++) {
                    Object data = this.jdbcHelper.getObjectFromResultSet(rs, i + 2, this.getExpectedInputDataTypes()[this.skIndx[i]], this.maxCharLength);
                    if (data == null) throw new KETLTransformException("NULL values are not allowed in the key table, check table " + this.mstrKeyTableName + " for errors");
                    key[this.skIndx[i]] = data;
                }
                Integer sk = rs.getInt(1);
                if (sk > this.mKeySource.value()) {
                    this.mKeySource.set(sk);
                }
                try {
                    this.putKeyArrayDataArray(key, sk);
                } catch (Error e) {
                    throw new KETLTransformException(e);
                }
                if (++x % 20000 == 0) this.setWaiting("lookup to seed. " + x + " records loaded");
            }
            rs.close();
            this.mLookup.commit(true);
            int size = this.mLookup.size();
            if (size != x) throw new KETLThreadException("Cache has failed load all elements, cache corruption, size = " + size + ", expected size = " + x, this); else {
                ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Cache loaded with " + x + " elements");
            }
            this.setWaiting("maximum surrogate value");
            String maxStatement = "select max(${PKCOLUMNS}) from ${SCHEMANAME}.${TABLENAME}";
            if (this.mstrKeyTableName != null) {
                template = EngineConstants.replaceParameterV2(maxStatement, "TABLENAME", this.mstrKeyTableName);
                template = EngineConstants.replaceParameterV2(template, "SCHEMANAME", this.mstrSchemaName);
                template = EngineConstants.replaceParameterV2(template, "PKCOLUMNS", this.getPrimaryKeyColumns());
                rs = mStmt.executeQuery(template);
                while (rs.next()) {
                    Integer sk = rs.getInt(1);
                    if (sk > this.mKeySource.value()) {
                        this.mKeySource.set(sk);
                    }
                }
                rs.close();
            }
            if (this.mstrTableName != null) {
                template = EngineConstants.replaceParameterV2(maxStatement, "TABLENAME", this.mstrTableName);
                template = EngineConstants.replaceParameterV2(template, "SCHEMANAME", this.mstrSchemaName);
                template = EngineConstants.replaceParameterV2(template, "PKCOLUMNS", this.getPrimaryKeyColumns());
                rs = mStmt.executeQuery(template);
                while (rs.next()) {
                    Integer sk = rs.getInt(1);
                    if (sk > this.mKeySource.value()) {
                        this.mKeySource.set(sk);
                    }
                }
                rs.close();
            }
            mStmt.close();
            this.setWaiting(null);
        } catch (Exception e) {
            if (mStmt != null) try {
                mStmt.close();
            } catch (Exception e1) {
            }
            throw new KETLThreadException(e, this);
        }
    }

    /**
	 * Sets the all columns.
	 * 
	 * @param msAllColumns
	 *            the new all columns
	 */
    void setAllColumns(String msAllColumns) {
        this.msAllColumns = msAllColumns;
    }

    /**
	 * Sets the DB case.
	 * 
	 * @param pStr
	 *            the str
	 * 
	 * @return the string
	 */
    private String setDBCase(String pStr) {
        if (pStr == null) return null;
        switch(this.mDBCase) {
            case LOWER_CASE:
                return pStr.toLowerCase();
            case MIXED_CASE:
                return pStr;
            case UPPER_CASE:
                return pStr.toUpperCase();
            case -1:
                return pStr;
        }
        return pStr;
    }

    /**
	 * Sets the insert values.
	 * 
	 * @param msInsertValues
	 *            the new insert values
	 */
    void setInsertValues(String msInsertValues) {
        this.msInsertValues = msInsertValues;
    }

    /**
	 * Sets the key table all columns.
	 * 
	 * @param msAllColumns
	 *            the new key table all columns
	 */
    void setKeyTableAllColumns(String msAllColumns) {
        this.msKeyTableAllColumns = msAllColumns;
    }

    /**
	 * Sets the key table insert values.
	 * 
	 * @param msInsertValues
	 *            the new key table insert values
	 */
    void setKeyTableInsertValues(String msInsertValues) {
        this.msKeyTableInsertValues = msInsertValues;
    }

    public PersistentMap swichToReadOnlyMode() {
        this.mLookup.switchToReadOnlyMode();
        return this.mLookup;
    }
}
