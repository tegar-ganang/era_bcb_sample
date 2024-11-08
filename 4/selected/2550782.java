package com.mattgarner.jaddas.node.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import com.mattgarner.jaddas.common.ColumnType;
import com.mattgarner.jaddas.common.ResultSet;
import com.mattgarner.jaddas.common.ResultSetHeader;
import com.mattgarner.jaddas.common.util.ByteConverter;
import com.mattgarner.jaddas.dataset.DataSetPartition;
import com.mattgarner.jaddas.node.LocalDataProvider;
import com.mattgarner.jaddas.util.MRUCache;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryDatabase;

public class LocalTable {

    private DataSetPartition dsPartition;

    private TableHeader header;

    private int columnCount;

    public static final String STRING_ENCODING = "UTF-8";

    private LocalDataProvider dataProvider;

    private Database dbLocalTableData;

    private SecondaryDatabase dbLocalTableDataUsingPrimaryKey;

    private MRUCache<Object, Integer> cachePrimaryKeyToTupleID;

    public LocalTable(DataSetPartition dsPartition, String dataStore, String dataName) throws TableException {
        this.dsPartition = dsPartition;
        this.header = dsPartition.getParentDataSet().getDataTableHeader();
        if (this.header == null) {
            throw new TableException("TableHeader cannot be null.");
        }
        this.columnCount = header.getColumnCount();
        this.dataProvider = LocalDataProvider.getInstance();
        this.dbLocalTableData = dataProvider.openDatabase(dataStore, dataName, true);
        this.dbLocalTableDataUsingPrimaryKey = dataProvider.openSecondaryDatabase(dbLocalTableData);
        this.cachePrimaryKeyToTupleID = new MRUCache<Object, Integer>(500);
    }

    public final TableHeader getTableHeader() {
        return header;
    }

    public final byte[] getValue(byte[] rowBytes, int col) throws TableException {
        ByteArrayInputStream bais = new ByteArrayInputStream(rowBytes);
        DataInputStream buffer = new DataInputStream(bais);
        int colIndex = 0;
        try {
            buffer.skip(2);
            while (colIndex < columnCount) {
                colIndex = buffer.readUnsignedByte();
                int fieldLength = buffer.readInt();
                if (fieldLength != -1) {
                    if (colIndex == col) {
                        byte[] fieldData = new byte[fieldLength];
                        buffer.read(fieldData, 0, fieldLength);
                        return fieldData;
                    } else {
                        buffer.skip(fieldLength);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw new TableException("getValue[byte]: Row data binary error: " + e.getMessage());
        }
    }

    public final byte[] getValueByPrimaryKey(Object primaryKey, int col) throws TableException {
        if (col >= columnCount) {
            throw new TableException("getValue: Column index (" + col + ") out-of-bounds.");
        }
        byte[] rowBytes;
        try {
            if (primaryKey instanceof String) {
                rowBytes = dataProvider.getBytes(dbLocalTableDataUsingPrimaryKey, primaryKey.toString(), LockMode.READ_UNCOMMITTED);
            } else if (primaryKey instanceof Integer) {
                rowBytes = dataProvider.getBytes(dbLocalTableDataUsingPrimaryKey, (Integer) primaryKey, LockMode.READ_UNCOMMITTED);
            } else if (primaryKey instanceof Short) {
                rowBytes = dataProvider.getBytes(dbLocalTableDataUsingPrimaryKey, (Short) primaryKey, LockMode.READ_UNCOMMITTED);
            } else if (primaryKey instanceof Long) {
                rowBytes = dataProvider.getBytes(dbLocalTableDataUsingPrimaryKey, (Long) primaryKey, LockMode.READ_UNCOMMITTED);
            } else if (primaryKey instanceof Byte[]) {
                rowBytes = dataProvider.getBytes(dbLocalTableDataUsingPrimaryKey, (byte[]) primaryKey, LockMode.READ_UNCOMMITTED);
            } else {
                throw new TableException("getValue: invalid primary key");
            }
        } catch (DatabaseException e) {
            throw new TableException("DatabaseException: " + e.getMessage());
        }
        return this.getValue(rowBytes, col);
    }

    public final String getStringValueByPrimaryKey(Object primaryKey, int col) throws TableException {
        byte[] fieldData = getValueByPrimaryKey(primaryKey, col);
        if (header.getColumnType(col) == ColumnType.COLUMN_TYPE_INT) {
            int value = getIntValueByPrimaryKey(primaryKey, col);
            return "" + value;
        } else if (header.getColumnBasicType(col) != ColumnType.COLUMN_TYPE_VARCHAR) {
            throw new TableException("getValue[String]: cannot get string from non-string column.");
        }
        try {
            return ByteConverter.convertToString(fieldData, STRING_ENCODING);
        } catch (IOException e) {
            throw new TableException("getValue: " + e.getMessage());
        }
    }

    public final String getStringValue(byte[] rowData, int col) throws TableException {
        byte[] fieldData = getValue(rowData, col);
        if (header.getColumnType(col) == ColumnType.COLUMN_TYPE_INT) {
            int value = getIntValue(rowData, col);
            return "" + value;
        } else if (header.getColumnBasicType(col) != ColumnType.COLUMN_TYPE_VARCHAR) {
            throw new TableException("getValue[String]: cannot get string from non-string column.");
        }
        try {
            return ByteConverter.convertToString(fieldData, STRING_ENCODING);
        } catch (IOException e) {
            throw new TableException("getValue: " + e.getMessage());
        }
    }

    public final int getIntValueByPrimaryKey(Object primaryKey, int col) throws TableException {
        try {
            return ByteConverter.bytesToInt(getValueByPrimaryKey(primaryKey, col));
        } catch (IOException e) {
            throw new TableException("getValue[int]: exception: " + e.getMessage());
        }
    }

    public final int getIntValue(byte[] rowBytes, int col) throws TableException {
        try {
            return ByteConverter.bytesToInt(getValue(rowBytes, col));
        } catch (IOException e) {
            throw new TableException("getValue[int]: exception: " + e.getMessage());
        }
    }

    public final short getShortValueByPrimaryKey(Object primaryKey, int col) throws TableException {
        try {
            return ByteConverter.bytesToShort(getValueByPrimaryKey(primaryKey, col));
        } catch (IOException e) {
            throw new TableException("getShortValueByPrimaryKey: " + e.getMessage());
        }
    }

    public final short getShortValue(byte[] rowBytes, int col) throws TableException {
        try {
            return ByteConverter.bytesToShort(getValue(rowBytes, col));
        } catch (IOException e) {
            throw new TableException("getValue[short]: exception: " + e.getMessage());
        }
    }

    public final long getLongValueByPrimaryKey(Object primaryKey, int col) throws TableException {
        try {
            return ByteConverter.bytesToLong(getValueByPrimaryKey(primaryKey, col));
        } catch (IOException e) {
            throw new TableException("getLongValueByPrimaryKey: " + e.getMessage());
        }
    }

    public final long getLongValue(byte[] rowBytes, int col) throws TableException {
        try {
            return ByteConverter.bytesToLong(getValue(rowBytes, col));
        } catch (IOException e) {
            throw new TableException("getValue[long]: exception: " + e.getMessage());
        }
    }

    public final TupleOperationStatus setValueByPrimaryKey(Object primaryKey, int col, byte[] data) throws TableException {
        if (col >= columnCount) {
            throw new TableException("setValue: Column index (" + col + ") out-of-bounds.");
        }
        byte[] rowBytes;
        try {
            if (primaryKey instanceof String) {
                rowBytes = dataProvider.getBytes(dbLocalTableDataUsingPrimaryKey, primaryKey.toString(), LockMode.READ_UNCOMMITTED);
            } else if (primaryKey instanceof Integer) {
                rowBytes = dataProvider.getBytes(dbLocalTableDataUsingPrimaryKey, (Integer) primaryKey, LockMode.READ_UNCOMMITTED);
            } else if (primaryKey instanceof Short) {
                rowBytes = dataProvider.getBytes(dbLocalTableDataUsingPrimaryKey, (Short) primaryKey, LockMode.READ_UNCOMMITTED);
            } else if (primaryKey instanceof Long) {
                rowBytes = dataProvider.getBytes(dbLocalTableDataUsingPrimaryKey, (Long) primaryKey, LockMode.READ_UNCOMMITTED);
            } else if (primaryKey instanceof Byte[]) {
                rowBytes = dataProvider.getBytes(dbLocalTableDataUsingPrimaryKey, (byte[]) primaryKey, LockMode.READ_UNCOMMITTED);
            } else {
                throw new TableException("setValue: invalid primary key");
            }
        } catch (Exception e) {
            throw new TableException("Exception: " + e.getMessage());
        }
        if (rowBytes == null) {
            rowBytes = initializeNewRow();
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(rowBytes);
        DataInputStream buffer = new DataInputStream(bais);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream rowOutputBuffer = new DataOutputStream(baos);
        int colIndex = 0;
        try {
            rowOutputBuffer.writeByte(buffer.readByte());
            rowOutputBuffer.writeByte(buffer.readByte());
            while (colIndex < columnCount) {
                colIndex = buffer.readUnsignedByte();
                rowOutputBuffer.writeByte(colIndex);
                int fieldLength = buffer.readInt();
                if (colIndex == col) {
                    rowOutputBuffer.writeInt(data.length);
                } else {
                    rowOutputBuffer.writeInt(fieldLength);
                }
                if (colIndex == col) {
                    if (fieldLength > 0) {
                        buffer.skip(fieldLength);
                    }
                    rowOutputBuffer.write(data);
                } else {
                    if (fieldLength > 0) {
                        byte[] fieldData = new byte[fieldLength];
                        buffer.read(fieldData, 0, fieldLength);
                        rowOutputBuffer.write(fieldData);
                    }
                }
                colIndex++;
            }
            rowOutputBuffer.flush();
            try {
                return setSingleRowData(primaryKey, baos.toByteArray());
            } catch (Exception e) {
                throw new TableException("DatabaseException: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new TableException("setValue[data]: Row data binary error: " + e.getMessage());
        }
    }

    public final TupleOperationStatus setValueByPrimaryKey(Object primaryKey, int col, Integer value) throws TableException {
        byte colType = header.getColumnBasicType(col);
        if (colType != ColumnType.COLUMN_TYPE_INT) {
            throw new TableException("setValue: Tried to set int on non-integer column (" + col + ").");
        }
        try {
            return setValueByPrimaryKey(primaryKey, col, ByteConverter.convertToBytes(value));
        } catch (IOException e) {
            throw new TableException("setValue[int]: Row data binary error: " + e.getMessage());
        }
    }

    public final TupleOperationStatus setValueByPrimaryKey(Object primaryKey, int col, Short value) throws TableException {
        byte colType = header.getColumnBasicType(col);
        if (colType != ColumnType.COLUMN_TYPE_SHORT) {
            throw new TableException("setValue: Tried to set short on non-short column (" + col + ").");
        }
        try {
            return setValueByPrimaryKey(primaryKey, col, ByteConverter.convertToBytes(value));
        } catch (IOException e) {
            throw new TableException("setValue[short]: Row data binary error: " + e.getMessage());
        }
    }

    public final TupleOperationStatus setValueByPrimaryKey(Object primaryKey, int col, Long value) throws TableException {
        byte colType = header.getColumnBasicType(col);
        if (colType != ColumnType.COLUMN_TYPE_LONG) {
            throw new TableException("setValue: Tried to set long on non-long column (" + col + ").");
        }
        try {
            return setValueByPrimaryKey(primaryKey, col, ByteConverter.convertToBytes(value));
        } catch (IOException e) {
            throw new TableException("setValue[long]: Row data binary error: " + e.getMessage());
        }
    }

    public final TupleOperationStatus setValueByPrimaryKey(Object primaryKey, int col, String value) throws TableException {
        if (value == null) {
            value = "" + '\000';
        }
        byte colType = header.getColumnBasicType(col);
        if (colType != ColumnType.COLUMN_TYPE_VARCHAR) {
            throw new TableException("setValue: Tried to set string on non-string column.");
        }
        try {
            return setValueByPrimaryKey(primaryKey, col, ByteConverter.convertToBytes(value));
        } catch (IOException e) {
            throw new TableException("setValue[String]: Row data binary error: " + e.getMessage());
        }
    }

    public final TupleOperationStatus setValueByPrimaryKey(Object primaryKey, int col, Object data) throws TableException {
        String dataClass = data.getClass().toString();
        try {
            if (dataClass.contains("String")) {
                return setValueByPrimaryKey(primaryKey, col, data.toString());
            } else if (dataClass.contains("int") || dataClass.contains("Int")) {
                return setValueByPrimaryKey(primaryKey, col, (Integer) data);
            } else if (dataClass.contains("short") || dataClass.contains("Short")) {
                return setValueByPrimaryKey(primaryKey, col, (Short) data);
            } else if (dataClass.contains("long") || dataClass.contains("Long")) {
                return setValueByPrimaryKey(primaryKey, col, (Long) data);
            } else if (dataClass.contains("byte")) {
                return setValueByPrimaryKey(primaryKey, col, (byte[]) data);
            } else {
                throw new TableException("setValue: invalid data class");
            }
        } catch (Exception e) {
            throw new TableException("Exception: " + e.getMessage());
        }
    }

    public final byte[] initializeNewRow() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream buffer = new DataOutputStream(baos);
        try {
            buffer.writeByte(0);
            buffer.writeByte(header.getPrimaryKey());
            for (int a = 0; a < columnCount; a++) {
                buffer.writeByte(a);
                buffer.writeInt(-1);
            }
            baos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public final byte[] getSingleRowData(Object primaryKey) throws TableException {
        byte[] rowBytes = new byte[0];
        try {
            rowBytes = dataProvider.getBytes(dbLocalTableDataUsingPrimaryKey, ByteConverter.convertToBytes(primaryKey), LockMode.READ_UNCOMMITTED);
        } catch (DatabaseException e) {
            throw new TableException("DatabaseException: " + e.getMessage());
        } catch (Exception e) {
            throw new TableException("Exception: " + e.getMessage());
        }
        return rowBytes;
    }

    public final TupleOperationStatus setSingleRowData(Object primaryKey, byte[] rowData) throws TableException {
        TupleOperationStatus tos = new TupleOperationStatus();
        try {
            byte[] keyBytes = ByteConverter.convertToBytes(primaryKey);
            Integer tupleID = cachePrimaryKeyToTupleID.get(primaryKey);
            if (tupleID == null) {
                ResultPair resultPair = dataProvider.getResultPair(dbLocalTableDataUsingPrimaryKey, keyBytes, LockMode.READ_UNCOMMITTED);
                if (resultPair.getKey() != null) {
                    tupleID = ByteConverter.bytesToInt(resultPair.getKey());
                }
            }
            if (tupleID == null) {
                tupleID = dsPartition.getNextTupleID();
            }
            cachePrimaryKeyToTupleID.put(primaryKey, tupleID);
            tos.affectedTupleID = tupleID;
            tos.dbOperationStatus = dataProvider.put(dbLocalTableData, tupleID, rowData);
            return tos;
        } catch (DatabaseException e) {
            throw new TableException("DatabaseException: " + e.getMessage());
        } catch (Exception e) {
            throw new TableException("Exception: " + e.getMessage());
        }
    }

    public final ResultSet getFullTableDataResultSet(String stringEncoding) throws TableException {
        try {
            ResultSetHeader rsHeader = header.convertToResultSetHeader(stringEncoding);
            ResultSet resultSet = new ResultSet(rsHeader);
            Cursor cursor = dbLocalTableData.openCursor(null, null);
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry data = new DatabaseEntry();
            int rowCounter = 0;
            while (cursor.getNext(key, data, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                for (int b = 0; b < header.getColumnCount(); b++) {
                    if (header.getColumnBasicType(b) == ColumnType.COLUMN_TYPE_INT) {
                        resultSet.setValue(rowCounter, b, getIntValue(data.getData(), b));
                    } else if (header.getColumnBasicType(b) == ColumnType.COLUMN_TYPE_SHORT) {
                        resultSet.setValue(rowCounter, b, getShortValue(data.getData(), b));
                    } else if (header.getColumnBasicType(b) == ColumnType.COLUMN_TYPE_VARCHAR) {
                        resultSet.setValue(rowCounter, b, getStringValue(data.getData(), b));
                    } else if (header.getColumnBasicType(b) == ColumnType.COLUMN_TYPE_BINARY) {
                        resultSet.setValue(rowCounter, b, getValue(data.getData(), b));
                    }
                }
                rowCounter++;
            }
            cursor.close();
            return resultSet;
        } catch (Exception e) {
            throw new TableException("LocalTableException: " + e.getMessage());
        }
    }

    public final Cursor getNewDatabaseCursor() throws DatabaseException {
        return dbLocalTableData.openCursor(null, null);
    }

    public final int addResultSetRows(ResultSet resultSet, boolean replace) throws TableException {
        int addedRowCount = 0;
        if (!checkCompatability(resultSet)) {
            throw new TableException("addResultSetRows: incompatible ResultSet");
        }
        try {
            int rsRowCount = resultSet.getRowCount();
            int priKeyColumn = header.getPrimaryKey();
            for (int a = 0; a < rsRowCount; a++) {
                byte[] priKeyData = resultSet.getValue(a, priKeyColumn);
                if (replace) {
                    setSingleRowData(priKeyData, resultSet.getSingleRowData(a));
                } else {
                    byte[] existingRow = getSingleRowData(priKeyData);
                    if (existingRow == null) {
                        setSingleRowData(priKeyData, resultSet.getSingleRowData(a));
                    } else {
                        throw new TableException("addResultSetRows: duplicate Primary Key at row " + a);
                    }
                }
            }
        } catch (Exception e) {
            throw new TableException("addResultSetRows: " + e.getMessage());
        }
        return addedRowCount;
    }

    public final boolean checkCompatability(ResultSet resultSet) {
        String hdrColString = header.getColumnTypesString();
        String rsColString = resultSet.getHeader().getColumnTypesString();
        if (hdrColString.matches(rsColString)) {
            return true;
        } else {
            return false;
        }
    }

    public final TableHeader getHeader() {
        return header;
    }

    public final long getFullRowCount() throws DatabaseException {
        return dbLocalTableData.count();
    }
}
