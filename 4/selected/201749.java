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
import java.sql.Connection;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import net.cattaka.util.ExceptionHandler;
import net.cattaka.util.StringUtil;

public class TelnetSqlitePreparedStatement implements PreparedStatement {

    private Socket socket;

    private Reader reader;

    private Writer writer;

    private String sql;

    private boolean closed;

    private int updateCount;

    private ResultSet resultSet;

    private List<String> paramList;

    public TelnetSqlitePreparedStatement(Socket socket, Reader reader, Writer writer, String sql) {
        super();
        this.socket = socket;
        this.reader = reader;
        this.writer = writer;
        this.sql = sql;
        this.closed = false;
    }

    public void readByPrompt() throws IOException {
        String r;
        while ((r = TelnetSqliteConnection.readLine(reader)) != null) {
            if (r.length() > 0 && r.charAt(0) == TelnetSqliteConnection.PROMPT) {
                break;
            }
        }
    }

    public void addBatch() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void clearParameters() throws SQLException {
        paramList.clear();
    }

    public boolean execute() throws SQLException {
        return this.execute(this.sql);
    }

    public ResultSet executeQuery() throws SQLException {
        if (this.execute()) {
            return this.resultSet;
        } else {
            throw new SQLException("executeQuery failed.");
        }
    }

    public int executeUpdate() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        if (this.resultSet != null) {
            return resultSet.getMetaData();
        } else {
            return null;
        }
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        return null;
    }

    public void setArray(int paramInt, Array paramArray) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setAsciiStream(int paramInt, InputStream paramInputStream) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setAsciiStream(int paramInt1, InputStream paramInputStream, int paramInt2) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setAsciiStream(int paramInt, InputStream paramInputStream, long paramLong) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setBigDecimal(int paramInt, BigDecimal paramBigDecimal) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setBinaryStream(int paramInt, InputStream paramInputStream) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setBinaryStream(int paramInt1, InputStream paramInputStream, int paramInt2) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setBinaryStream(int paramInt, InputStream paramInputStream, long paramLong) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setBlob(int paramInt, Blob paramBlob) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setBlob(int paramInt, InputStream paramInputStream) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setBlob(int paramInt, InputStream paramInputStream, long paramLong) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setBoolean(int paramInt, boolean paramBoolean) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setByte(int paramInt, byte paramByte) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setBytes(int paramInt, byte[] paramArrayOfByte) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setCharacterStream(int paramInt, Reader paramReader) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setCharacterStream(int paramInt1, Reader paramReader, int paramInt2) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setCharacterStream(int paramInt, Reader paramReader, long paramLong) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setClob(int paramInt, Clob paramClob) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setClob(int paramInt, Reader paramReader) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setClob(int paramInt, Reader paramReader, long paramLong) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setDate(int paramInt, Date paramDate) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setDate(int paramInt, Date paramDate, Calendar paramCalendar) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setDouble(int paramInt, double paramDouble) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setFloat(int paramInt, float paramFloat) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setInt(int paramInt1, int paramInt2) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setLong(int paramInt, long paramLong) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setNCharacterStream(int paramInt, Reader paramReader) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setNCharacterStream(int paramInt, Reader paramReader, long paramLong) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setNClob(int paramInt, Reader paramReader) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setNClob(int paramInt, Reader paramReader, long paramLong) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setNString(int paramInt, String paramString) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setNull(int paramInt1, int paramInt2) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setNull(int paramInt1, int paramInt2, String paramString) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setObject(int paramInt, Object paramObject) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setObject(int paramInt1, Object paramObject, int paramInt2) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setObject(int paramInt1, Object paramObject, int paramInt2, int paramInt3) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setRef(int paramInt, Ref paramRef) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setShort(int paramInt, short paramShort) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setString(int paramInt, String paramString) throws SQLException {
        for (int i = this.paramList.size(); i < paramInt; i++) {
            this.paramList.add(0, null);
        }
        this.paramList.set(paramInt, paramString);
    }

    public void setTime(int paramInt, Time paramTime) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setTime(int paramInt, Time paramTime, Calendar paramCalendar) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setTimestamp(int paramInt, Timestamp paramTimestamp) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setTimestamp(int paramInt, Timestamp paramTimestamp, Calendar paramCalendar) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setURL(int paramInt, URL paramURL) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setUnicodeStream(int paramInt1, InputStream paramInputStream, int paramInt2) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void addBatch(String paramString) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void cancel() throws SQLException {
        try {
            this.socket.close();
        } catch (IOException e) {
        }
    }

    public void clearBatch() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void clearWarnings() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void close() throws SQLException {
        this.closed = true;
    }

    public boolean execute(String paramString) throws SQLException {
        boolean resultFlag = false;
        int rowCount = 0;
        try {
            String sql = StringUtil.replaceString(paramString, "\\", "\\\\");
            sql = StringUtil.replaceString(sql, "\n", "\\\n");
            ;
            writer.append(sql);
            writer.append('\n');
            writer.flush();
            String result = TelnetSqliteConnection.readLine(reader);
            if (result.length() > 0 && result.charAt(0) == TelnetSqliteConnection.PREFIX_SUCCESS) {
                resultFlag = true;
                try {
                    rowCount = Integer.parseInt(result.substring(1));
                } catch (NumberFormatException e) {
                    ExceptionHandler.debug(e);
                }
            } else if (result.length() > 0 && result.charAt(0) == TelnetSqliteConnection.PREFIX_ERROR) {
                readByPrompt();
                throw new SQLException(result.substring(1));
            } else {
                throw new IOException(result);
            }
        } catch (IOException e) {
            try {
                this.socket.close();
            } catch (IOException e2) {
                ExceptionHandler.debug(e);
            }
            throw new SQLException(e.getMessage());
        }
        try {
            if (resultFlag) {
                this.resultSet = TelnetSqliteResultSet.createResultSet(socket, reader, writer, rowCount);
            }
            readByPrompt();
        } catch (IOException e2) {
            try {
                this.socket.close();
            } catch (IOException e3) {
                ExceptionHandler.error(e3);
            }
        }
        return resultFlag;
    }

    public boolean execute(String paramString, int paramInt) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public boolean execute(String paramString, int[] paramArrayOfInt) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public boolean execute(String paramString, String[] paramArrayOfString) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public int[] executeBatch() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public ResultSet executeQuery(String paramString) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public int executeUpdate(String paramString) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public int executeUpdate(String paramString, int paramInt) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public int executeUpdate(String paramString, int[] paramArrayOfInt) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public int executeUpdate(String paramString, String[] paramArrayOfString) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public Connection getConnection() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public int getFetchDirection() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public int getFetchSize() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public int getMaxFieldSize() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public int getMaxRows() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public boolean getMoreResults() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public boolean getMoreResults(int paramInt) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public int getQueryTimeout() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public ResultSet getResultSet() throws SQLException {
        return this.resultSet;
    }

    public int getResultSetConcurrency() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public int getResultSetHoldability() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public int getResultSetType() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public int getUpdateCount() throws SQLException {
        return updateCount;
    }

    public SQLWarning getWarnings() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public boolean isClosed() throws SQLException {
        return this.closed;
    }

    public boolean isPoolable() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setCursorName(String paramString) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setEscapeProcessing(boolean paramBoolean) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setFetchDirection(int paramInt) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setFetchSize(int paramInt) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setMaxFieldSize(int paramInt) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setMaxRows(int paramInt) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setPoolable(boolean paramBoolean) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setQueryTimeout(int paramInt) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public boolean isWrapperFor(Class<?> paramClass) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public <T> T unwrap(Class<T> paramClass) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }
}
