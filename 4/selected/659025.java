package fr.soleil.hdbtdbArchivingApi.ArchivingApi.AttributesManagement.HdbAttributeInsert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import fr.esrf.Tango.AttrWriteType;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.GetConf;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.AttributesManagement.AdtAptAttributes.IAdtAptAttributes;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.DataBaseManagement.DbCommands.ConnectionCommands;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.DataBaseManagement.DbConnection.ConnectionFactory;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.DataBaseManagement.DbConnection.IDBConnection;
import fr.soleil.hdbtdbArchivingApi.ArchivingTools.Tools.ArchivingException;

/**
 * @author AYADI
 * 
 */
public class HDBMySqlAttributeInsert extends HdbAttributeInsert {

    private static final String HDB_ARCHIVER = "HdbArchiver";

    private static final int DEFAULT_BULK_SIZE = 1;

    private int insertBulkSize = DEFAULT_BULK_SIZE;

    private static final String BULK_SIZE_PROP_NAME = "InsertBulkSize";

    private static final long DEFAULT_BULK_TIME = TimeUnit.SECONDS.toMillis(10);

    private long insertBulkTime = DEFAULT_BULK_TIME;

    private static final String BULK_TIME_PROP_NAME = "InsertBulkTime";

    private static int MAX_STRING_SIZE = 255;

    private IDBConnection dbConn;

    private final Map<String, ScalarGroupInserter> statements = Collections.synchronizedMap(new HashMap<String, ScalarGroupInserter>());

    private final Map<String, SpectrumGroupInserter> spectrumStatements = Collections.synchronizedMap(new HashMap<String, SpectrumGroupInserter>());

    /**
     * Manage bulk insertion for scalar attributes. Configured by tango class
     * properties of HdbArchiver
     * 
     * @author ABEILLE
     * 
     */
    private class ScalarGroupInserter {

        private class InsertData {

            public InsertData(final Object value, final int dataType, final int writable, final Timestamp timestamp) {
                super();
                this.value = value;
                this.dataType = dataType;
                this.writable = writable;
                this.timestamp = timestamp;
            }

            private final Object value;

            private final int dataType;

            private final int writable;

            private final Timestamp timestamp;

            public Object getValue() {
                return value;
            }

            public int getDataType() {
                return dataType;
            }

            public int getWritable() {
                return writable;
            }

            public Timestamp getTimestamp() {
                return timestamp;
            }
        }

        private int currentBulkSize = 0;

        private long currentBulkStartTime = 0;

        private final String query;

        private StringBuilder queryBuffer;

        private final List<InsertData> dataToInsert = new ArrayList<InsertData>();

        public ScalarGroupInserter(final String query) throws ArchivingException, SQLException {
            this.query = query;
            queryBuffer = new StringBuilder(query);
        }

        public void addScalarValue(final Object value, final int dataType, final int writable, final Timestamp timestamp) throws SQLException, ArchivingException {
            if (currentBulkSize > 0) {
                if (writable == AttrWriteType._READ_WRITE) {
                    queryBuffer.append(", (?, ?, ?)");
                } else {
                    queryBuffer.append(", (?, ?)");
                }
            }
            dataToInsert.add(new InsertData(value, dataType, writable, timestamp));
            long currentBulkTime = 0;
            if (currentBulkStartTime == 0) {
                currentBulkStartTime = timestamp.getTime();
            } else {
                currentBulkTime = timestamp.getTime() - currentBulkStartTime;
            }
            currentBulkSize++;
            if (currentBulkSize >= insertBulkSize || currentBulkTime >= insertBulkTime) {
                Connection conn = null;
                PreparedStatement stmt = null;
                try {
                    conn = dbConn.getConnection();
                    stmt = conn.prepareStatement(queryBuffer.toString());
                    int i = 1;
                    for (final InsertData data : dataToInsert) {
                        stmt.setTimestamp(i++, data.getTimestamp());
                        ConnectionCommands.prepareSmtScalar(stmt, data.getDataType(), data.getWritable(), data.getValue(), i++);
                        if (writable == AttrWriteType._READ_WRITE) {
                            i++;
                        }
                    }
                    stmt.execute();
                } finally {
                    currentBulkStartTime = 0;
                    currentBulkSize = 0;
                    dataToInsert.clear();
                    queryBuffer = new StringBuilder(query);
                    ConnectionCommands.close(stmt);
                    dbConn.closeConnection(conn);
                }
            }
        }
    }

    private class SpectrumGroupInserter {

        private class InsertData {

            public InsertData(final StringBuffer readValue, final StringBuffer writeValue, final int dimX, final Timestamp timestamp) {
                super();
                this.readValue = readValue;
                this.writeValue = writeValue;
                this.dimX = dimX;
                this.timestamp = timestamp;
            }

            private final StringBuffer readValue;

            private final StringBuffer writeValue;

            public StringBuffer getWriteValue() {
                return writeValue;
            }

            private final int dimX;

            private final Timestamp timestamp;

            public StringBuffer getReadValue() {
                return readValue;
            }

            public int getDimX() {
                return dimX;
            }

            public Timestamp getTimestamp() {
                return timestamp;
            }
        }

        private int currentBulkSize = 0;

        private long currentBulkStartTime = 0;

        private final String query;

        private StringBuilder queryBuffer;

        private final List<InsertData> dataToInsert = new ArrayList<InsertData>();

        public SpectrumGroupInserter(final String query) throws ArchivingException, SQLException {
            this.query = query;
            queryBuffer = new StringBuilder(query);
        }

        private void addSpectrum(final StringBuffer readValue, final StringBuffer writeValue, final int dimX, final Timestamp timestamp, final boolean isReadWrite) throws ArchivingException, SQLException {
            dataToInsert.add(new InsertData(readValue, writeValue, dimX, timestamp));
            long currentBulkTime = 0;
            if (currentBulkStartTime == 0) {
                currentBulkStartTime = timestamp.getTime();
            } else {
                currentBulkTime = timestamp.getTime() - currentBulkStartTime;
            }
            currentBulkSize++;
            if (currentBulkSize >= insertBulkSize || currentBulkTime >= insertBulkTime) {
                Connection conn = null;
                PreparedStatement stmt = null;
                try {
                    conn = dbConn.getConnection();
                    stmt = conn.prepareStatement(queryBuffer.toString());
                    int i = 1;
                    for (final InsertData data : dataToInsert) {
                        stmt.setTimestamp(i++, data.getTimestamp());
                        stmt.setInt(i++, data.getDimX());
                        if (data.getReadValue() == null) {
                            stmt.setNull(i++, java.sql.Types.BLOB);
                        } else {
                            stmt.setString(i++, data.getReadValue().toString());
                        }
                        if (isReadWrite) {
                            if (data.getWriteValue() == null) {
                                stmt.setNull(i++, java.sql.Types.BLOB);
                            } else {
                                stmt.setString(i++, data.getWriteValue().toString());
                            }
                        }
                    }
                    stmt.execute();
                } finally {
                    currentBulkStartTime = 0;
                    currentBulkSize = 0;
                    dataToInsert.clear();
                    queryBuffer = new StringBuilder(query);
                    ConnectionCommands.close(stmt);
                    dbConn.closeConnection(conn);
                }
            }
        }

        public void addSpectrumROValue(final StringBuffer value, final int dimX, final Timestamp timestamp) throws SQLException, ArchivingException {
            if (currentBulkSize == 0) {
                queryBuffer.append("(?, ?, ?)");
            } else {
                queryBuffer.append(", (?, ?, ?)");
            }
            addSpectrum(value, null, dimX, timestamp, false);
        }

        public void addSpectrumRWValue(final StringBuffer readValue, final StringBuffer writeValue, final int dimX, final Timestamp timestamp) throws SQLException, ArchivingException {
            if (currentBulkSize == 0) {
                queryBuffer.append("(?, ?, ?, ?)");
            } else {
                queryBuffer.append(" ,(?, ?, ?, ?)");
            }
            addSpectrum(readValue, writeValue, dimX, timestamp, true);
        }
    }

    /**
     * @param con
     * @param ut
     * @param at
     * @throws ArchivingException
     */
    public HDBMySqlAttributeInsert(final int type, final IAdtAptAttributes at, final Logger logger) throws ArchivingException {
        super(type, at, logger);
        try {
            dbConn = ConnectionFactory.getInstance(archType);
            insertBulkSize = Math.abs(Integer.parseInt(GetConf.readStringInDB(HDB_ARCHIVER, BULK_SIZE_PROP_NAME)));
            insertBulkTime = Math.abs(Integer.parseInt(GetConf.readStringInDB(HDB_ARCHIVER, BULK_TIME_PROP_NAME)));
        } catch (final NumberFormatException e) {
        }
    }

    @Override
    protected void insert_ImageData_RO_DataBase(final StringBuffer query, final StringBuffer tableName, final StringBuffer tableFields, final int dimX, final int dimY, final Timestamp timeSt, final Double[][] dvalue, final StringBuffer valueStr, final String attributeName) throws ArchivingException {
        final IDBConnection dbConn = ConnectionFactory.getInstance(archType);
        if (dbConn == null) {
            return;
        }
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        query.append("INSERT INTO ").append(tableName).append(" (").append(tableFields).append(")").append(" VALUES (?, ?, ?, ?)");
        try {
            conn = dbConn.getConnection();
            preparedStatement = conn.prepareStatement(query.toString());
            preparedStatement.setTimestamp(1, timeSt);
            preparedStatement.setInt(2, dimX);
            preparedStatement.setInt(3, dimY);
            if (dvalue == null) {
                preparedStatement.setNull(4, java.sql.Types.BLOB);
            } else {
                preparedStatement.setString(4, valueStr.toString());
            }
            preparedStatement.executeUpdate();
        } catch (final SQLException e) {
            throw new ArchivingException(e);
        } finally {
            ConnectionCommands.close(preparedStatement);
            dbConn.closeConnection(conn);
        }
    }

    @Override
    protected void insert_SpectrumData_RO_DataBase(final String tableName, final StringBuffer tableFields, final int dim_x, final Timestamp timeSt, final StringBuffer valueStr, final String att_name) throws ArchivingException {
        final StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ").append(tableName).append(" (").append(tableFields).append(")").append(" VALUES ");
        try {
            if (spectrumStatements.containsKey(tableName)) {
                spectrumStatements.get(tableName).addSpectrumROValue(valueStr, dim_x, timeSt);
            } else {
                final SpectrumGroupInserter inserter = new SpectrumGroupInserter(query.toString());
                spectrumStatements.put(tableName, inserter);
                inserter.addSpectrumROValue(valueStr, dim_x, timeSt);
            }
        } catch (final SQLException e) {
            throw new ArchivingException(e);
        }
    }

    @Override
    protected void insert_SpectrumData_RW_DataBase(final String tableName, final StringBuffer tableFields, final int dimX, final Timestamp timeSt, final StringBuffer valueWriteStr, final StringBuffer valueReadStr, final String attributeName) throws ArchivingException {
        final IDBConnection dbConn = ConnectionFactory.getInstance(archType);
        if (dbConn == null) {
            return;
        }
        final Connection conn = null;
        final PreparedStatement preparedStatement = null;
        final StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ").append(tableName).append(" (").append(tableFields).append(")").append(" VALUES");
        try {
            if (spectrumStatements.containsKey(tableName)) {
                spectrumStatements.get(tableName).addSpectrumRWValue(valueReadStr, valueWriteStr, dimX, timeSt);
            } else {
                final SpectrumGroupInserter inserter = new SpectrumGroupInserter(query.toString());
                spectrumStatements.put(tableName, inserter);
                inserter.addSpectrumRWValue(valueReadStr, valueWriteStr, dimX, timeSt);
            }
        } catch (final SQLException e) {
            throw new ArchivingException(e);
        } finally {
            ConnectionCommands.close(preparedStatement);
            dbConn.closeConnection(conn);
        }
    }

    @Override
    protected synchronized void insertScalarDataInDB(final String tableName, final StringBuffer query, final String attributeName, final int writable, final Timestamp timeSt, final Object value, final int dataType) throws ArchivingException {
        try {
            Object dataToInsert = value;
            if (value instanceof String) {
                final String string = (String) value;
                if (string.length() > MAX_STRING_SIZE) {
                    dataToInsert = string.substring(0, MAX_STRING_SIZE);
                }
            }
            if (statements.containsKey(tableName)) {
                statements.get(tableName).addScalarValue(dataToInsert, dataType, writable, timeSt);
            } else {
                final ScalarGroupInserter inserter = new ScalarGroupInserter(query.toString());
                statements.put(tableName, inserter);
                inserter.addScalarValue(dataToInsert, dataType, writable, timeSt);
            }
        } catch (final SQLException e) {
            logger.error("SqlException has been raised insertScalarData()");
            throw new ArchivingException(e);
        }
    }
}
