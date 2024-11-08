package com.subshell.persistence.oracle;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import oracle.jdbc.driver.OraclePreparedStatement;

/**
 * Wrapper to make LOBS work with Oracle during INSERT and UPDATE
 * operations. Is not as general as it could be as it assumes that
 * each row uses a single primary key that is either a String or a
 * long integer. Has been used successfully to get the
 * storage.hibernate.HibernateDataStorageManager to work with
 * Oracle.
 *
 * @author <a href="mailto:rossi@webslingerZ.com">Chris Rossi</a>
 * @version $Revision: 1.2 $
 */
public class OracleHackedPreparedStatement extends PreparedStatementWrapper {

    protected ParsedStatement parsedStatement = null;

    protected List lobs = new LinkedList();

    protected String sql;

    public OracleHackedPreparedStatement(OraclePreparedStatement ps, String sql) {
        super(ps);
        this.sql = sql;
    }

    public OraclePreparedStatement getOraclePreparedStatement() {
        return (OraclePreparedStatement) getWrappedPreparedStatement();
    }

    public void setClob(int i, Clob x) throws SQLException {
        if (parsedStatement == null) {
            parsedStatement = new ParsedStatement(sql);
            lobs = new LinkedList();
        }
        setString(i, " ");
        lobs.add(new OracleHackedLob(parsedStatement.columns[i - 1], x));
    }

    public void setString(int i, String x) throws SQLException {
        super.setString(i, x);
    }

    private static final byte[] BLANK_BINARY = { (byte) ' ' };

    public void setBlob(int i, Blob x) throws SQLException {
        if (parsedStatement == null) {
            parsedStatement = new ParsedStatement(sql);
            lobs = new LinkedList();
        }
        super.setBytes(i, BLANK_BINARY);
        lobs.add(new OracleHackedLob(parsedStatement.columns[i - 1], x));
    }

    public ResultSet executeQuery() throws SQLException {
        ResultSet rs = super.executeQuery();
        executeLobHack();
        return rs;
    }

    public int executeUpdate() throws SQLException {
        int i = super.executeUpdate();
        executeLobHack();
        return i;
    }

    public boolean execute() throws SQLException {
        boolean b = super.execute();
        executeLobHack();
        return b;
    }

    public int[] executeBatch() throws SQLException {
        int[] i = super.executeBatch();
        executeLobHack();
        return i;
    }

    protected void executeLobHack() throws SQLException {
        try {
            if (lobs != null && lobs.size() > 0 && parsedStatement.columns != null) {
                Connection connection = getConnection();
                StringBuffer sql = new StringBuffer(100);
                sql.append("select ");
                for (Iterator i = lobs.iterator(); i.hasNext(); ) {
                    OracleHackedLob lob = (OracleHackedLob) i.next();
                    sql.append(lob.column.name);
                    if (i.hasNext()) {
                        sql.append(",");
                    }
                }
                sql.append(" from ");
                sql.append(parsedStatement.table);
                StatementColumn column = parsedStatement.getIdColumn(connection);
                sql.append(" where ");
                sql.append(column.name);
                sql.append("=?");
                PreparedStatement ps = connection.prepareStatement(sql.toString());
                Object id = parsedStatement.getId();
                if (id instanceof Long) {
                    ps.setLong(1, ((Long) id).longValue());
                } else if (id instanceof Integer) {
                    ps.setLong(1, ((Integer) id).intValue());
                } else if (id instanceof String) {
                    ps.setString(1, (String) id);
                }
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int index = 1;
                    for (Iterator i = lobs.iterator(); i.hasNext(); index++) {
                        OracleHackedLob lob = (OracleHackedLob) i.next();
                        if (lob.data instanceof Clob) {
                            oracle.sql.CLOB clob = (oracle.sql.CLOB) rs.getClob(index);
                            Writer writer = clob.getCharacterOutputStream();
                            Reader reader = ((Clob) lob.data).getCharacterStream();
                            FileUtil.copyFile(reader, writer);
                            reader.close();
                            writer.close();
                        } else if (lob.data instanceof Blob) {
                            oracle.sql.BLOB blob = (oracle.sql.BLOB) rs.getBlob(index);
                            OutputStream output = blob.getBinaryOutputStream();
                            InputStream input = ((Blob) lob.data).getBinaryStream();
                            FileUtil.copyFile(input, output);
                            input.close();
                            output.close();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
    }

    class ParsedStatement {

        String table;

        StatementColumn[] columns = null;

        StatementColumn idColumn = null;

        ParsedStatement(String sql) {
            parse(sql);
        }

        StatementColumn getIdColumn(Connection connection) throws SQLException {
            if (idColumn == null) {
                DatabaseMetaData md = connection.getMetaData();
                ResultSet pk = md.getPrimaryKeys(null, null, table.toUpperCase());
                String name = null;
                idColumn = null;
                if (pk.next()) {
                    name = pk.getString("COLUMN_NAME");
                    for (int i = 0; i < columns.length; i++) {
                        if (columns[i].name.toUpperCase().equals(name)) {
                            idColumn = columns[i];
                            break;
                        }
                    }
                }
                pk.close();
                if (name == null) {
                    throw new RuntimeException("no primary key for table " + table);
                } else if (idColumn == null) {
                    throw new RuntimeException("could not find matching column for key " + name);
                }
            }
            return idColumn;
        }

        Object getId() {
            Integer key = new Integer(idColumn.position);
            return columnData.get(key);
        }

        int leftIndex = 0;

        int rightIndex = 0;

        private void parse(String sql) {
            skipWhitespace(sql);
            String op = getKeyword(sql).toUpperCase();
            if (op.equals("INSERT")) {
                parseInsert(sql);
            } else if (op.equals("UPDATE")) {
                parseUpdate(sql);
            }
        }

        private void skipWhitespace(String sql) {
            while (Character.isWhitespace(sql.charAt(rightIndex))) {
                rightIndex++;
            }
            leftIndex = rightIndex;
        }

        private String getKeyword(String sql) {
            while (!Character.isWhitespace(sql.charAt(rightIndex))) {
                rightIndex++;
            }
            return sql.substring(leftIndex, rightIndex);
        }

        private void parseInsert(String sql) {
            List columns = new LinkedList();
            skipWhitespace(sql);
            String into = getKeyword(sql).toUpperCase();
            if (!into.equals("INTO")) {
                throw new RuntimeException("not expecting " + into);
            }
            skipWhitespace(sql);
            table = getKeyword(sql);
            skipWhitespace(sql);
            if (sql.charAt(rightIndex) != '(') {
                throw new RuntimeException("expecting (");
            }
            rightIndex++;
            leftIndex = rightIndex;
            List columnNames = new ArrayList();
            while (true) {
                skipWhitespace(sql);
                char ch = sql.charAt(rightIndex);
                while (ch != ',' && ch != ')') {
                    rightIndex++;
                    ch = sql.charAt(rightIndex);
                }
                String columnName = sql.substring(leftIndex, rightIndex).trim();
                columnNames.add(columnName);
                rightIndex++;
                leftIndex = rightIndex;
                if (ch == ')') {
                    break;
                }
            }
            skipWhitespace(sql);
            String values = getKeyword(sql).toUpperCase();
            if (!values.equals("VALUES")) {
                throw new RuntimeException("not expecting " + values);
            }
            skipWhitespace(sql);
            if (sql.charAt(rightIndex) != '(') {
                throw new RuntimeException("expecting (");
            }
            rightIndex++;
            leftIndex = rightIndex;
            int position = 1;
            int index = 0;
            while (true) {
                skipWhitespace(sql);
                char ch = sql.charAt(rightIndex);
                while (ch != ',' && ch != ')') {
                    rightIndex++;
                    ch = sql.charAt(rightIndex);
                }
                String columnValue = sql.substring(leftIndex, rightIndex).trim();
                if (columnValue.equals("?")) {
                    columns.add(new StatementColumn(position, (String) columnNames.get(index)));
                    position++;
                }
                rightIndex++;
                leftIndex = rightIndex;
                if (ch == ')') {
                    break;
                }
                index++;
            }
            this.columns = new StatementColumn[columns.size()];
            columns.toArray(this.columns);
        }

        private void parseUpdate(String sql) {
            List columns = new ArrayList(10);
            skipWhitespace(sql);
            table = getKeyword(sql);
            int position = 1;
            skipWhitespace(sql);
            String set = getKeyword(sql).toUpperCase();
            if (!set.equals("SET")) {
                throw new RuntimeException("not expecting " + set);
            }
            final int EXPECTING_COLUMN_NAME = 0;
            final int GOT_COLUMN_NAME = 1;
            final int GOT_EQUALS = 2;
            final int GOT_COLUMN_VALUE = 3;
            int state = EXPECTING_COLUMN_NAME;
            int sqlLen = sql.length();
            String columnName = null;
            while (rightIndex < sqlLen) {
                skipWhitespace(sql);
                char ch = sql.charAt(rightIndex);
                while (ch != ',' && ch != '=' && !Character.isWhitespace(ch)) {
                    rightIndex++;
                    if (rightIndex >= sqlLen) {
                        break;
                    }
                    ch = sql.charAt(rightIndex);
                }
                String next = sql.substring(leftIndex, rightIndex);
                rightIndex++;
                leftIndex = rightIndex;
                if (state == EXPECTING_COLUMN_NAME) {
                    columnName = next;
                    if (ch == '=') {
                        state = GOT_EQUALS;
                    } else {
                        state = GOT_COLUMN_NAME;
                    }
                    if (ch == ',') {
                        throw new RuntimeException("not expecting , here");
                    }
                } else if (state == GOT_COLUMN_NAME) {
                    if (ch == '=') {
                        state = GOT_EQUALS;
                    } else {
                        throw new RuntimeException("expecting =");
                    }
                } else if (state == GOT_EQUALS) {
                    if (next.equals("?")) {
                        columns.add(new StatementColumn(position, columnName));
                        position++;
                    }
                    if (ch == ',') {
                        state = EXPECTING_COLUMN_NAME;
                    } else {
                        state = GOT_COLUMN_VALUE;
                    }
                } else if (state == GOT_COLUMN_VALUE) {
                    if (ch == ',') {
                        state = EXPECTING_COLUMN_NAME;
                    } else if (next.toUpperCase().equals("WHERE")) {
                        state = EXPECTING_COLUMN_NAME;
                    } else {
                        throw new RuntimeException("not expecting " + next);
                    }
                }
            }
            this.columns = new StatementColumn[columns.size()];
            columns.toArray(this.columns);
        }
    }

    class StatementColumn {

        int position;

        String name;

        StatementColumn(int position, String name) {
            this.position = position;
            this.name = name;
        }
    }

    public static final int TYPE_CLOB = 0;

    public static final int TYPE_BLOB = 1;

    class OracleHackedLob {

        StatementColumn column;

        Object data;

        OracleHackedLob(StatementColumn column, Object data) {
            this.column = column;
            this.data = data;
        }
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        boolean temp = super.execute(sql, autoGeneratedKeys);
        executeLobHack();
        return temp;
    }

    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        boolean temp = super.execute(sql, columnIndexes);
        executeLobHack();
        return temp;
    }

    public boolean execute(String sql, String[] columnNames) throws SQLException {
        boolean temp = super.execute(sql, columnNames);
        executeLobHack();
        return temp;
    }

    public boolean execute(String sql) throws SQLException {
        boolean temp = super.execute(sql);
        executeLobHack();
        return temp;
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        ResultSet temp = super.executeQuery(sql);
        executeLobHack();
        return temp;
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        int temp = super.executeUpdate(sql, autoGeneratedKeys);
        executeLobHack();
        return temp;
    }

    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        int temp = super.executeUpdate(sql, columnIndexes);
        executeLobHack();
        return temp;
    }

    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        int temp = super.executeUpdate(sql, columnNames);
        executeLobHack();
        return temp;
    }

    public int executeUpdate(String sql) throws SQLException {
        int temp = super.executeUpdate(sql);
        executeLobHack();
        return temp;
    }
}
