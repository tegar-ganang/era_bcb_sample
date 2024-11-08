package net.cattaka.rdbassistant.driver.telnetsqlite.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.Socket;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Ref;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import net.cattaka.util.StringUtil;

public class TelnetSqliteResultSet implements java.sql.ResultSet {

    private TelnetSqliteResultSetMetaData metaData;

    private int columnCount;

    private int rowCount;

    private ArrayList<ArrayList<String>> resultRow;

    private int currentRow;

    private TelnetSqliteResultSet(ArrayList<ArrayList<String>> resultRow, TelnetSqliteResultSetMetaData metaData, int rowCount, int columnCount) {
        super();
        this.resultRow = resultRow;
        this.metaData = metaData;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.currentRow = -1;
    }

    public static TelnetSqliteResultSet createResultSet(Socket socket, Reader reader, Writer writer, int rowCount) throws IOException {
        String columnNameListArray = TelnetSqliteConnection.readLine(reader);
        TelnetSqliteResultSetMetaData metaData = null;
        int columnCount = 0;
        ArrayList<ArrayList<String>> resultRow = new ArrayList<ArrayList<String>>();
        if (columnNameListArray.length() > 0 && columnNameListArray.charAt(0) == TelnetSqliteConnection.RESULT_HEADER) {
            String[] columnNameList = StringUtil.split(columnNameListArray.substring(1), ',', '"');
            int[] columnTypeList = new int[columnNameList.length];
            for (int i = 0; i < columnTypeList.length; i++) {
                columnTypeList[i] = Types.VARCHAR;
            }
            columnCount = columnNameList.length;
            while (true) {
                String line = TelnetSqliteConnection.readLine(reader);
                if (line.length() == 0) {
                    break;
                } else if (line.charAt(0) == TelnetSqliteConnection.RESULT_DATA) {
                    String[] csv = split(line.substring(1), ',', '"');
                    ArrayList<String> row = new ArrayList<String>();
                    for (String str : csv) {
                        row.add(str);
                    }
                    while (row.size() < columnCount) {
                        row.add(null);
                    }
                    resultRow.add(row);
                } else {
                    throw new IOException(line);
                }
            }
            metaData = new TelnetSqliteResultSetMetaData(columnNameList, columnTypeList);
        } else if (columnNameListArray.length() == 0) {
            return null;
        } else {
            throw new IOException(columnNameListArray);
        }
        return new TelnetSqliteResultSet(resultRow, metaData, rowCount, columnCount);
    }

    public boolean absolute(int paramInt) throws SQLException {
        return false;
    }

    public void afterLast() throws SQLException {
    }

    public void beforeFirst() throws SQLException {
    }

    public void cancelRowUpdates() throws SQLException {
    }

    public void clearWarnings() throws SQLException {
    }

    public void close() throws SQLException {
    }

    public void deleteRow() throws SQLException {
    }

    public int findColumn(String paramString) throws SQLException {
        return 0;
    }

    public boolean first() throws SQLException {
        return false;
    }

    public Array getArray(int paramInt) throws SQLException {
        return null;
    }

    public Array getArray(String paramString) throws SQLException {
        return null;
    }

    public InputStream getAsciiStream(int paramInt) throws SQLException {
        return null;
    }

    public InputStream getAsciiStream(String paramString) throws SQLException {
        return null;
    }

    public BigDecimal getBigDecimal(int paramInt1, int paramInt2) throws SQLException {
        return null;
    }

    public BigDecimal getBigDecimal(int paramInt) throws SQLException {
        return null;
    }

    public BigDecimal getBigDecimal(String paramString, int paramInt) throws SQLException {
        return null;
    }

    public BigDecimal getBigDecimal(String paramString) throws SQLException {
        return null;
    }

    public InputStream getBinaryStream(int paramInt) throws SQLException {
        return null;
    }

    public InputStream getBinaryStream(String paramString) throws SQLException {
        return null;
    }

    public Blob getBlob(int paramInt) throws SQLException {
        return null;
    }

    public Blob getBlob(String paramString) throws SQLException {
        return null;
    }

    public boolean getBoolean(int paramInt) throws SQLException {
        return false;
    }

    public boolean getBoolean(String paramString) throws SQLException {
        return false;
    }

    public byte getByte(int paramInt) throws SQLException {
        return 0;
    }

    public byte getByte(String paramString) throws SQLException {
        return 0;
    }

    public byte[] getBytes(int paramInt) throws SQLException {
        return null;
    }

    public byte[] getBytes(String paramString) throws SQLException {
        return null;
    }

    public Reader getCharacterStream(int paramInt) throws SQLException {
        return null;
    }

    public Reader getCharacterStream(String paramString) throws SQLException {
        return null;
    }

    public Clob getClob(int paramInt) throws SQLException {
        return null;
    }

    public Clob getClob(String paramString) throws SQLException {
        return null;
    }

    public int getConcurrency() throws SQLException {
        return 0;
    }

    public String getCursorName() throws SQLException {
        return null;
    }

    public Date getDate(int paramInt, Calendar paramCalendar) throws SQLException {
        return null;
    }

    public Date getDate(int paramInt) throws SQLException {
        return null;
    }

    public Date getDate(String paramString, Calendar paramCalendar) throws SQLException {
        return null;
    }

    public Date getDate(String paramString) throws SQLException {
        return null;
    }

    public double getDouble(int paramInt) throws SQLException {
        return 0;
    }

    public double getDouble(String paramString) throws SQLException {
        return 0;
    }

    public int getFetchDirection() throws SQLException {
        return 0;
    }

    public int getFetchSize() throws SQLException {
        return 0;
    }

    public float getFloat(int paramInt) throws SQLException {
        return 0;
    }

    public float getFloat(String paramString) throws SQLException {
        return 0;
    }

    public int getHoldability() throws SQLException {
        return 0;
    }

    public int getInt(int paramInt) throws SQLException {
        return 0;
    }

    public int getInt(String paramString) throws SQLException {
        return 0;
    }

    public long getLong(int paramInt) throws SQLException {
        return 0;
    }

    public long getLong(String paramString) throws SQLException {
        return 0;
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return this.metaData;
    }

    public Reader getNCharacterStream(int paramInt) throws SQLException {
        return null;
    }

    public Reader getNCharacterStream(String paramString) throws SQLException {
        return null;
    }

    public String getNString(int paramInt) throws SQLException {
        return null;
    }

    public String getNString(String paramString) throws SQLException {
        return null;
    }

    public Object getObject(int paramInt, Map<String, Class<?>> paramMap) throws SQLException {
        return null;
    }

    public Object getObject(int paramInt) throws SQLException {
        return null;
    }

    public Object getObject(String paramString, Map<String, Class<?>> paramMap) throws SQLException {
        return null;
    }

    public Object getObject(String paramString) throws SQLException {
        return null;
    }

    public Ref getRef(int paramInt) throws SQLException {
        return null;
    }

    public Ref getRef(String paramString) throws SQLException {
        return null;
    }

    public int getRow() throws SQLException {
        return 0;
    }

    public short getShort(int paramInt) throws SQLException {
        return 0;
    }

    public short getShort(String paramString) throws SQLException {
        return 0;
    }

    public Statement getStatement() throws SQLException {
        return null;
    }

    public String getString(int paramInt) throws SQLException {
        return (0 < paramInt && paramInt <= columnCount) ? resultRow.get(currentRow).get(paramInt - 1) : null;
    }

    public String getString(String paramString) throws SQLException {
        int paramInt = metaData.getColumnIndex(paramString);
        return getString(paramInt);
    }

    public Time getTime(int paramInt, Calendar paramCalendar) throws SQLException {
        return null;
    }

    public Time getTime(int paramInt) throws SQLException {
        return null;
    }

    public Time getTime(String paramString, Calendar paramCalendar) throws SQLException {
        return null;
    }

    public Time getTime(String paramString) throws SQLException {
        return null;
    }

    public Timestamp getTimestamp(int paramInt, Calendar paramCalendar) throws SQLException {
        return null;
    }

    public Timestamp getTimestamp(int paramInt) throws SQLException {
        return null;
    }

    public Timestamp getTimestamp(String paramString, Calendar paramCalendar) throws SQLException {
        return null;
    }

    public Timestamp getTimestamp(String paramString) throws SQLException {
        return null;
    }

    public int getType() throws SQLException {
        return 0;
    }

    public InputStream getUnicodeStream(int paramInt) throws SQLException {
        return null;
    }

    public InputStream getUnicodeStream(String paramString) throws SQLException {
        return null;
    }

    public URL getURL(int paramInt) throws SQLException {
        return null;
    }

    public URL getURL(String paramString) throws SQLException {
        return null;
    }

    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    public void insertRow() throws SQLException {
    }

    public boolean isAfterLast() throws SQLException {
        return false;
    }

    public boolean isBeforeFirst() throws SQLException {
        return false;
    }

    public boolean isClosed() throws SQLException {
        return false;
    }

    public boolean isFirst() throws SQLException {
        return false;
    }

    public boolean isLast() throws SQLException {
        return false;
    }

    public boolean last() throws SQLException {
        return false;
    }

    public void moveToCurrentRow() throws SQLException {
    }

    public void moveToInsertRow() throws SQLException {
    }

    public boolean next() throws SQLException {
        currentRow++;
        return (currentRow < this.rowCount);
    }

    public boolean previous() throws SQLException {
        return false;
    }

    public void refreshRow() throws SQLException {
    }

    public boolean relative(int paramInt) throws SQLException {
        return false;
    }

    public boolean rowDeleted() throws SQLException {
        return false;
    }

    public boolean rowInserted() throws SQLException {
        return false;
    }

    public boolean rowUpdated() throws SQLException {
        return false;
    }

    public void setFetchDirection(int paramInt) throws SQLException {
    }

    public void setFetchSize(int paramInt) throws SQLException {
    }

    public void updateArray(int paramInt, Array paramArray) throws SQLException {
    }

    public void updateArray(String paramString, Array paramArray) throws SQLException {
    }

    public void updateAsciiStream(int paramInt1, InputStream paramInputStream, int paramInt2) throws SQLException {
    }

    public void updateAsciiStream(int paramInt, InputStream paramInputStream, long paramLong) throws SQLException {
    }

    public void updateAsciiStream(int paramInt, InputStream paramInputStream) throws SQLException {
    }

    public void updateAsciiStream(String paramString, InputStream paramInputStream, int paramInt) throws SQLException {
    }

    public void updateAsciiStream(String paramString, InputStream paramInputStream, long paramLong) throws SQLException {
    }

    public void updateAsciiStream(String paramString, InputStream paramInputStream) throws SQLException {
    }

    public void updateBigDecimal(int paramInt, BigDecimal paramBigDecimal) throws SQLException {
    }

    public void updateBigDecimal(String paramString, BigDecimal paramBigDecimal) throws SQLException {
    }

    public void updateBinaryStream(int paramInt1, InputStream paramInputStream, int paramInt2) throws SQLException {
    }

    public void updateBinaryStream(int paramInt, InputStream paramInputStream, long paramLong) throws SQLException {
    }

    public void updateBinaryStream(int paramInt, InputStream paramInputStream) throws SQLException {
    }

    public void updateBinaryStream(String paramString, InputStream paramInputStream, int paramInt) throws SQLException {
    }

    public void updateBinaryStream(String paramString, InputStream paramInputStream, long paramLong) throws SQLException {
    }

    public void updateBinaryStream(String paramString, InputStream paramInputStream) throws SQLException {
    }

    public void updateBlob(int paramInt, Blob paramBlob) throws SQLException {
    }

    public void updateBlob(int paramInt, InputStream paramInputStream, long paramLong) throws SQLException {
    }

    public void updateBlob(int paramInt, InputStream paramInputStream) throws SQLException {
    }

    public void updateBlob(String paramString, Blob paramBlob) throws SQLException {
    }

    public void updateBlob(String paramString, InputStream paramInputStream, long paramLong) throws SQLException {
    }

    public void updateBlob(String paramString, InputStream paramInputStream) throws SQLException {
    }

    public void updateBoolean(int paramInt, boolean paramBoolean) throws SQLException {
    }

    public void updateBoolean(String paramString, boolean paramBoolean) throws SQLException {
    }

    public void updateByte(int paramInt, byte paramByte) throws SQLException {
    }

    public void updateByte(String paramString, byte paramByte) throws SQLException {
    }

    public void updateBytes(int paramInt, byte[] paramArrayOfByte) throws SQLException {
    }

    public void updateBytes(String paramString, byte[] paramArrayOfByte) throws SQLException {
    }

    public void updateCharacterStream(int paramInt1, Reader paramReader, int paramInt2) throws SQLException {
    }

    public void updateCharacterStream(int paramInt, Reader paramReader, long paramLong) throws SQLException {
    }

    public void updateCharacterStream(int paramInt, Reader paramReader) throws SQLException {
    }

    public void updateCharacterStream(String paramString, Reader paramReader, int paramInt) throws SQLException {
    }

    public void updateCharacterStream(String paramString, Reader paramReader, long paramLong) throws SQLException {
    }

    public void updateCharacterStream(String paramString, Reader paramReader) throws SQLException {
    }

    public void updateClob(int paramInt, Clob paramClob) throws SQLException {
    }

    public void updateClob(int paramInt, Reader paramReader, long paramLong) throws SQLException {
    }

    public void updateClob(int paramInt, Reader paramReader) throws SQLException {
    }

    public void updateClob(String paramString, Clob paramClob) throws SQLException {
    }

    public void updateClob(String paramString, Reader paramReader, long paramLong) throws SQLException {
    }

    public void updateClob(String paramString, Reader paramReader) throws SQLException {
    }

    public void updateDate(int paramInt, Date paramDate) throws SQLException {
    }

    public void updateDate(String paramString, Date paramDate) throws SQLException {
    }

    public void updateDouble(int paramInt, double paramDouble) throws SQLException {
    }

    public void updateDouble(String paramString, double paramDouble) throws SQLException {
    }

    public void updateFloat(int paramInt, float paramFloat) throws SQLException {
    }

    public void updateFloat(String paramString, float paramFloat) throws SQLException {
    }

    public void updateInt(int paramInt1, int paramInt2) throws SQLException {
    }

    public void updateInt(String paramString, int paramInt) throws SQLException {
    }

    public void updateLong(int paramInt, long paramLong) throws SQLException {
    }

    public void updateLong(String paramString, long paramLong) throws SQLException {
    }

    public void updateNCharacterStream(int paramInt, Reader paramReader, long paramLong) throws SQLException {
    }

    public void updateNCharacterStream(int paramInt, Reader paramReader) throws SQLException {
    }

    public void updateNCharacterStream(String paramString, Reader paramReader, long paramLong) throws SQLException {
    }

    public void updateNCharacterStream(String paramString, Reader paramReader) throws SQLException {
    }

    public void updateNClob(int paramInt, Reader paramReader, long paramLong) throws SQLException {
    }

    public void updateNClob(int paramInt, Reader paramReader) throws SQLException {
    }

    public void updateNClob(String paramString, Reader paramReader, long paramLong) throws SQLException {
    }

    public void updateNClob(String paramString, Reader paramReader) throws SQLException {
    }

    public void updateNString(int paramInt, String paramString) throws SQLException {
    }

    public void updateNString(String paramString1, String paramString2) throws SQLException {
    }

    public void updateNull(int paramInt) throws SQLException {
    }

    public void updateNull(String paramString) throws SQLException {
    }

    public void updateObject(int paramInt1, Object paramObject, int paramInt2) throws SQLException {
    }

    public void updateObject(int paramInt, Object paramObject) throws SQLException {
    }

    public void updateObject(String paramString, Object paramObject, int paramInt) throws SQLException {
    }

    public void updateObject(String paramString, Object paramObject) throws SQLException {
    }

    public void updateRef(int paramInt, Ref paramRef) throws SQLException {
    }

    public void updateRef(String paramString, Ref paramRef) throws SQLException {
    }

    public void updateRow() throws SQLException {
    }

    public void updateShort(int paramInt, short paramShort) throws SQLException {
    }

    public void updateShort(String paramString, short paramShort) throws SQLException {
    }

    public void updateString(int paramInt, String paramString) throws SQLException {
    }

    public void updateString(String paramString1, String paramString2) throws SQLException {
    }

    public void updateTime(int paramInt, Time paramTime) throws SQLException {
    }

    public void updateTime(String paramString, Time paramTime) throws SQLException {
    }

    public void updateTimestamp(int paramInt, Timestamp paramTimestamp) throws SQLException {
    }

    public void updateTimestamp(String paramString, Timestamp paramTimestamp) throws SQLException {
    }

    public boolean wasNull() throws SQLException {
        return false;
    }

    public boolean isWrapperFor(Class<?> paramClass) throws SQLException {
        return false;
    }

    public <T> T unwrap(Class<T> paramClass) throws SQLException {
        return null;
    }

    public static String[] split(String src, char delim, char bracket) {
        ArrayList<String> result = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        int mode = 0;
        for (int i = 0; i < src.length(); i++) {
            char ch = src.charAt(i);
            switch(mode) {
                case 0:
                    if (ch == delim) {
                        result.add(null);
                        sb.delete(0, sb.length());
                    } else if (ch == bracket) {
                        mode = 1;
                    } else {
                        sb.append(ch);
                        mode = 3;
                    }
                    break;
                case 1:
                    if (ch == delim) {
                        sb.append(ch);
                    } else if (ch == bracket) {
                        mode = 2;
                    } else {
                        sb.append(ch);
                    }
                    break;
                case 2:
                    if (ch == delim) {
                        result.add(sb.toString());
                        sb.delete(0, sb.length());
                        mode = 0;
                    } else if (ch == bracket) {
                        sb.append(ch);
                        mode = 1;
                    } else {
                        sb.append(delim);
                        sb.append(ch);
                        mode = 1;
                    }
                    break;
                case 3:
                    if (ch == delim) {
                        result.add(sb.toString());
                        sb.delete(0, sb.length());
                        mode = 0;
                    } else if (ch == bracket) {
                        sb.append(ch);
                    } else {
                        sb.append(ch);
                    }
                    break;
                default:
                    throw new RuntimeException("ERROR");
            }
        }
        if (mode != 0) {
            result.add(sb.toString());
        }
        return result.toArray(new String[result.size()]);
    }
}
