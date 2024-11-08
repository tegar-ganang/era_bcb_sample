package net.cattaka.rdbassistant.driver.telnetsqlite.jdbc;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import net.cattaka.util.ExceptionHandler;

public class TelnetSqliteConnection implements Connection {

    public static final char PREFIX_SUCCESS = 'S';

    public static final char PREFIX_ERROR = 'E';

    public static final char SEPALATOR = ',';

    public static final char PROMPT = '$';

    public static final char RESULT_HEADER = '+';

    public static final char RESULT_DATA = '-';

    public static final char COMMAND_SPECIAL = '.';

    public static final String CHARSET = "UTF-8";

    private boolean closed;

    private String hostname;

    private int port;

    private String database;

    private Socket socket;

    private Reader reader;

    private Writer writer;

    public TelnetSqliteConnection(String hostname, int port, String database) {
        super();
        this.hostname = hostname;
        this.port = port;
        this.database = database;
        this.closed = false;
    }

    public void prepareConection() throws SQLException {
        if (socket != null && !socket.isClosed()) {
            return;
        }
        if (this.closed) {
            throw new SQLException("Socket is closed");
        }
        try {
            this.socket = new Socket(hostname, port);
            this.reader = new InputStreamReader(socket.getInputStream(), CHARSET);
            this.writer = new OutputStreamWriter(socket.getOutputStream(), CHARSET);
            this.writer.write(database + '\n');
            this.writer.flush();
            String line = readLine(this.reader);
            if (line.length() > 0 && line.charAt(0) == PREFIX_SUCCESS) {
                String line2 = readLine(this.reader);
                if (line2.length() > 0 && line2.charAt(0) == PROMPT) {
                } else {
                    throw new IOException("Prompt could not received : " + line2);
                }
            } else if (line.length() > 0 && line.charAt(0) == PREFIX_ERROR) {
                throw new IOException(line.substring(1));
            } else {
                throw new IOException(line);
            }
        } catch (IOException e) {
            if (this.socket != null) {
                try {
                    this.socket.close();
                } catch (IOException e2) {
                    ExceptionHandler.debug(e2);
                }
            }
            this.socket = null;
            this.reader = null;
            this.writer = null;
            throw new SQLException(e.getMessage());
        }
    }

    public void clearWarnings() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void close() throws SQLException {
        try {
            writer.append(COMMAND_SPECIAL);
            writer.append("quit");
            writer.flush();
            this.socket.close();
        } catch (IOException e2) {
        }
        this.closed = true;
        this.socket = null;
        this.reader = null;
        this.writer = null;
    }

    public void write(String arg) throws SQLException {
        try {
            writer.append(arg);
            writer.append('\n');
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException e2) {
            }
            throw new SQLException(e.getMessage());
        }
    }

    public void commit() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public Statement createStatement() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public boolean getAutoCommit() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public String getCatalog() throws SQLException {
        return "main";
    }

    public Properties getClientInfo() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public String getClientInfo(String name) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public int getHoldability() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public int getTransactionIsolation() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public SQLWarning getWarnings() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public boolean isClosed() throws SQLException {
        return this.closed;
    }

    public boolean isReadOnly() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public boolean isValid(int timeout) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public String nativeSQL(String sql) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return this.prepareStatement(sql);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        prepareConection();
        PreparedStatement stmt = new TelnetSqlitePreparedStatement(socket, reader, writer, sql);
        return stmt;
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void rollback() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setCatalog(String catalog) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setHoldability(int holdability) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public Savepoint setSavepoint() throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setTransactionIsolation(int level) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public <T> T unwrap(Class<T> arg0) throws SQLException {
        throw new SQLException("Not implemented yet.");
    }

    public static String readLine(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        int r;
        boolean escaped = false;
        while ((r = reader.read()) != -1) {
            if (escaped) {
                sb.append((char) r);
                escaped = false;
            } else {
                if (r == '\\') {
                    escaped = true;
                } else if (r == '\r') {
                } else if (r == '\n') {
                    return sb.toString();
                } else {
                    sb.append((char) r);
                }
            }
        }
        return sb.toString();
    }
}
