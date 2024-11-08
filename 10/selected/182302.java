package it.simplerecords.migration;

import it.simplerecords.annotations.ItalianName;
import it.simplerecords.connection.ConnectionManager;
import it.simplerecords.exceptions.IrreversibleException;
import it.simplerecords.exceptions.RecordException;
import it.simplerecords.util.LoggableStatement;
import it.simplerecords.util.Logger;
import it.simplerecords.util.StatementBuilder;
import it.simplerecords.util.TableNameResolver;
import java.sql.Connection;
import java.sql.SQLException;
import org.json.me.JSONArray;
import org.json.me.JSONException;

public abstract class Migration {

    protected Logger log = new Logger(this.getClass());

    private boolean autoCommit = true;

    private Connection conn;

    /**
	 * Method called to perform the migration. An actual migration class must override this method and call inside it a series of
	 * createTable, alterTable...
	 * 
	 * @throws RecordException
	 *            Any error during the migration steps
	 */
    public abstract void up() throws RecordException;

    /**
	 * Method called to undo the migration. An actual migration class must override this method and call inside it a series of dropTable,
	 * alterTable...
	 * 
	 * @throws RecordException
	 *            Any error during the migration steps
	 * @throws IrreversibleException
	 *            Thrown to warn that the migration is irreversible
	 */
    public abstract void down() throws RecordException, IrreversibleException;

    public final void setAutocommit(boolean autoCommit) throws RecordException {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                conn = null;
            }
        } catch (SQLException e) {
            throw new RecordException("Error closing connection", e);
        }
        if (autoCommit) {
            this.autoCommit = true;
        } else {
            this.autoCommit = false;
            conn = ConnectionManager.getConnection();
            try {
                conn.setAutoCommit(false);
            } catch (SQLException e) {
                throw new RecordException("Error setting autoCommit false on the connection", e);
            }
        }
    }

    public final void commit() throws RecordException {
        if (!autoCommit) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.commit();
                } else {
                    throw new RecordException("Connection already closed");
                }
            } catch (SQLException e) {
                throw new RecordException("Error committing changes", e);
            }
        }
    }

    public final void rollback() throws RecordException {
        if (!autoCommit) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.rollback();
                    log.log("Rollback done");
                } else {
                    throw new RecordException("Connection already closed");
                }
            } catch (SQLException e) {
                throw new RecordException("Error reverting changes", e);
            }
        }
    }

    public final void close() throws RecordException {
        if (!autoCommit) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                } else {
                    throw new RecordException("Connection already closed");
                }
            } catch (SQLException e) {
                throw new RecordException("Error closing connection", e);
            }
        }
    }

    protected final void createTable(Table t) throws RecordException {
        createTable(t, true);
    }

    /**
	 * Method to create a table.
	 * 
	 * @param t
	 *           The Table object contatinig all the table details.
	 * @throws RecordException
	 *            Any error creating the table
	 */
    private final void createTable(Table t, boolean resolveName) throws RecordException {
        Connection conn = null;
        if (autoCommit) {
            conn = ConnectionManager.getConnection();
        } else {
            conn = this.conn;
        }
        LoggableStatement st = null;
        String tableName = null;
        if (resolveName) {
            tableName = TableNameResolver.getTableName(t.getTableName(), isItalian());
        } else {
            tableName = t.getTableName();
        }
        t.setAction(Table.ACTION_CREATE);
        try {
            if (t.isForce()) {
                String sql = "drop table if exists " + tableName;
                st = new LoggableStatement(conn, sql);
                log.log(st.getQueryString());
                st.execute();
                st.close();
            }
            StatementBuilder builder = t.toSql(resolveName, isItalian());
            st = builder.getPreparedStatement(conn);
            builder.getPreparedStatement(conn);
            log.log(st.getQueryString());
            st.execute();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback ", e1);
            }
            throw new RecordException("Error creating table " + tableName + ". A rollback was done.", e);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (autoCommit) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new RecordException("Error closing the connection", e);
            }
        }
    }

    /**
	 * Method to drop a table
	 * 
	 * @param tableName
	 *           The name of the table to be dropped
	 * @throws RecordException
	 *            Any error while dropping the table
	 */
    protected final void dropTable(String tableName) throws RecordException {
        dropTable(tableName, false, true);
    }

    protected final void dropTable(String tableName, boolean ifExists) throws RecordException {
        dropTable(tableName, ifExists, true);
    }

    protected final void dropTable(String tableName, boolean ifExists, boolean resolveTableName) throws RecordException {
        Connection conn = null;
        if (autoCommit) {
            conn = ConnectionManager.getConnection();
        } else {
            conn = this.conn;
        }
        String sql = "drop table ";
        if (ifExists) {
            sql += "if exists ";
        }
        if (resolveTableName) {
            sql += TableNameResolver.getTableName(tableName, isItalian());
        } else {
            sql += tableName;
        }
        LoggableStatement st = null;
        try {
            st = new LoggableStatement(ConnectionManager.getConnection(), sql);
            log.log(st.getQueryString());
            st.execute();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback", e1);
            }
            throw new RecordException("Error dropping table " + TableNameResolver.getTableName(tableName, isItalian()) + ". A rollback was done.", e);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (autoCommit) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new RecordException("Error closing the connection", e);
            }
        }
    }

    /**
	 * Method to rename a table
	 * 
	 * @param tableName
	 *           The current table name
	 * @param newName
	 *           The new table name
	 * @throws RecordException
	 *            Any error renaming the table
	 */
    protected final void renameTable(String tableName, String newName) throws RecordException {
        Connection conn = null;
        if (autoCommit) {
            conn = ConnectionManager.getConnection();
        } else {
            conn = this.conn;
        }
        String sql = "rename table " + TableNameResolver.getTableName(tableName, isItalian()) + " to " + newName;
        LoggableStatement st = null;
        try {
            st = new LoggableStatement(conn, sql);
            log.log(st.getQueryString());
            st.execute();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback ", e1);
            }
            throw new RecordException("Error renaming table " + TableNameResolver.getTableName(tableName, isItalian()) + ". A rollback was done.", e);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (autoCommit) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new RecordException("Error closing the connection", e);
            }
        }
    }

    /**
	 * Method to alter a column (add, drop, change).
	 * 
	 * @param tableName
	 *           The name of the table to modify
	 * @param options
	 *           A JSON array of parameters for the column modification. There are three possible configurations:
	 *           <ul>
	 *           <li>[columnName, drop] Drops the column
	 *           <li>[columnName, add, type, &lt;first, after name&gt;] Adds a new column named <i>columnName</i>, of type <i>type</i>, with
	 *           optional positioning (first or after a specified column)
	 *           <li>[columnName, rename, type, newName] Changes the specified colum (<i>columnName</i>) to the specified <i>type</i> and
	 *           <i>newName</i>
	 *           </ul>
	 * @throws RecordException
	 *            Any error during column modification
	 */
    protected final void alterColumn(String tableName, String options) throws RecordException {
        if (options.startsWith("{")) {
            options = options.substring(1);
        }
        if (options.endsWith("}")) {
            options = options.substring(0, options.length() - 1);
        }
        String columnName = null;
        String action = null;
        String type = null;
        String newName = null;
        String columnPosition = null;
        boolean first = false;
        try {
            JSONArray a = new JSONArray(options);
            columnName = a.getString(0);
            action = a.getString(1);
            if (action.equalsIgnoreCase("add")) {
                type = a.getString(2);
                if (a.length() == 4) {
                    String tmp = a.getString(3);
                    if (tmp.equalsIgnoreCase("first")) {
                        first = true;
                    } else {
                        columnPosition = tmp;
                    }
                }
            } else if (action.equalsIgnoreCase("rename")) {
                type = a.getString(2);
                newName = a.getString(3);
            }
        } catch (JSONException e) {
            throw new RecordException("Error parsing parameters", e);
        } catch (IndexOutOfBoundsException e) {
            throw new RecordException("Error parsing parameters", e);
        }
        String sql = null;
        LoggableStatement st = null;
        Connection conn = null;
        if (autoCommit) {
            conn = ConnectionManager.getConnection();
        } else {
            conn = this.conn;
        }
        if (action.equalsIgnoreCase("drop")) {
            sql = "alter table " + TableNameResolver.getTableName(tableName, isItalian()) + " drop column " + columnName;
        } else if (action.equalsIgnoreCase("add")) {
            sql = "alter table " + TableNameResolver.getTableName(tableName, isItalian()) + " add column " + columnName + " " + Column.getSQLType(type, false);
            if (columnPosition != null) {
                sql += " " + columnPosition;
            } else if (first) {
                sql += " first";
            }
        } else if (action.equalsIgnoreCase("rename")) {
            sql = "alter table " + TableNameResolver.getTableName(tableName, isItalian()) + " change column " + columnName + " " + newName + " " + Column.getSQLType(type, false);
        } else {
            try {
                if (autoCommit) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new RecordException("Alter table action " + action + " not recognized. Possible actions are add, rename, drop", new RecordException("Error closing connection.", e));
            }
            throw new RecordException("Alter table action " + action + " not recognized. Possible actions are add, rename, drop");
        }
        try {
            st = new LoggableStatement(conn, sql);
            log.log(st.getQueryString());
            st.execute();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback ", e1);
            }
            throw new RecordException("Error altering table " + TableNameResolver.getTableName(tableName, isItalian()) + ". A rollback was done.", e);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (autoCommit) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new RecordException("Error closing the connection", e);
            }
        }
    }

    protected final void insert(String tableName, Object... values) throws RecordException {
        insert(tableName, true, values);
    }

    private final void insert(String tableName, boolean resolveName, Object... values) throws RecordException {
        if (values == null || values.length == 0) {
            throw new RecordException("No values passed to the function");
        }
        String name = null;
        if (resolveName) {
            name = TableNameResolver.getTableName(tableName, isItalian());
        } else {
            name = tableName;
        }
        String sql = "insert into " + name + " values (";
        for (int i = 0; i < values.length; ++i) {
            sql += "?";
            if (i != values.length - 1) {
                sql += ", ";
            }
        }
        sql += ")";
        LoggableStatement st = null;
        Connection conn = null;
        if (autoCommit) {
            conn = ConnectionManager.getConnection();
        } else {
            conn = this.conn;
        }
        try {
            st = new LoggableStatement(conn, sql);
            for (int i = 0; i < values.length; ++i) {
                st.setObject(i + 1, values[i]);
            }
            log.log(st.getQueryString());
            st.executeUpdate();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback ", e1);
            }
            throw new RecordException("Error inserting into table " + name + ". A rollback was done.", e);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (autoCommit) {
                    conn.commit();
                    conn.close();
                }
            } catch (SQLException e) {
                throw new RecordException("Error closing the connection", e);
            }
        }
    }

    protected final void insert(String tableName, String[] columns, Object[] values) throws RecordException {
        if (columns == null || columns.length == 0) {
            throw new RecordException("No column names passed to the function");
        }
        if (values == null || values.length == 0) {
            throw new RecordException("No values passed to the function");
        }
        if (columns.length != values.length) {
            throw new RecordException("The number of column names and of values does not match");
        }
        String sql = "insert into " + TableNameResolver.getTableName(tableName, isItalian()) + " (";
        for (int i = 0; i < columns.length; ++i) {
            sql += columns[i];
            if (i != columns.length - 1) {
                sql += ", ";
            }
        }
        sql += ") values (";
        for (int i = 0; i < values.length; ++i) {
            sql += "?";
            if (i != values.length - 1) {
                sql += ", ";
            }
        }
        sql += ")";
        LoggableStatement st = null;
        Connection conn = null;
        if (autoCommit) {
            conn = ConnectionManager.getConnection();
        } else {
            conn = this.conn;
        }
        try {
            st = new LoggableStatement(conn, sql);
            for (int i = 0; i < values.length; ++i) {
                st.setObject(i + 1, values[i]);
            }
            log.log(st.getQueryString());
            st.executeUpdate();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback ", e1);
            }
            throw new RecordException("Error inserting into table " + TableNameResolver.getTableName(tableName, isItalian()) + ". A rollback was done.", e);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (autoCommit) {
                    conn.commit();
                    conn.close();
                }
            } catch (SQLException e) {
                throw new RecordException("Error closing the connection", e);
            }
        }
    }

    /**
	 * Method to create an index.
	 * 
	 * @param name
	 *           The index name
	 * @param tableName
	 *           The table the index refers to
	 * @param unique
	 *           If the index is unique
	 * @param columnNames
	 *           The column names, and optional direction (asc, desc), the index is created for
	 * @throws RecordException
	 *            Any error while creating the index
	 */
    protected final void createIndex(String name, String tableName, boolean unique, String... columnNames) throws RecordException {
        if (columnNames.length == 0) {
            throw new RecordException("An index needs at least a column name");
        }
        String sql = null;
        if (unique) {
            sql = "create unique index " + name + " on " + TableNameResolver.getTableName(tableName, isItalian());
        } else {
            sql = "create index " + name + " on " + TableNameResolver.getTableName(tableName, isItalian());
        }
        sql += " (";
        for (String c : columnNames) {
            sql += c + ", ";
        }
        sql = sql.substring(0, sql.length() - 2);
        sql += ")";
        LoggableStatement st = null;
        Connection conn = null;
        if (autoCommit) {
            conn = ConnectionManager.getConnection();
        } else {
            conn = this.conn;
        }
        try {
            st = new LoggableStatement(conn, sql);
            log.log(st.getQueryString());
            st.execute();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback ", e1);
            }
            throw new RecordException("Error creating index " + name + ". A rollback was done.", e);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (autoCommit) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new RecordException("Error closing the connection", e);
            }
        }
    }

    /**
	 * Method to delete an index
	 * 
	 * @param name
	 *           The index name
	 * @param tableName
	 *           The table the index is applied to
	 * @throws RecordException
	 *            Any error while dropping the index
	 */
    protected final void dropIndex(String name, String tableName) throws RecordException {
        dropIndex(name, tableName, false);
    }

    protected final void dropIndex(String name, String tableName, boolean ifExists) throws RecordException {
        String sql = "drop index " + name + " on " + TableNameResolver.getTableName(tableName, isItalian());
        if (ifExists) {
            throw new RecordException("Mysql does not support drop index _if exists_");
        }
        LoggableStatement st = null;
        Connection conn = null;
        if (autoCommit) {
            conn = ConnectionManager.getConnection();
        } else {
            conn = this.conn;
        }
        try {
            st = new LoggableStatement(conn, sql);
            log.log(st.getQueryString());
            st.execute();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback ", e1);
            }
            throw new RecordException("Error dropping index " + name + ". A rollback was done.", e);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (autoCommit) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new RecordException("Error closing the connection", e);
            }
        }
    }

    /**
	 * Method to execute an arbitrary SQL command
	 * 
	 * @param sql
	 *           The SQL command to execute (must follow SQL syntax)
	 * @throws RecordException
	 *            Any error executing the command
	 */
    protected final void execute(String sql) throws RecordException {
        LoggableStatement st = null;
        Connection conn = null;
        if (autoCommit) {
            conn = ConnectionManager.getConnection();
        } else {
            conn = this.conn;
        }
        try {
            st = new LoggableStatement(conn, sql);
            log.log(st.getQueryString());
            st.execute();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback ", e1);
            }
            throw new RecordException("Error running sql statement. A rollback was done.", e);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (autoCommit) {
                    conn.commit();
                    conn.close();
                }
            } catch (SQLException e) {
                throw new RecordException("Error closing the connection", e);
            }
        }
    }

    protected final void createManyToManyRelationship(String table1, String table2, String tableOptions) throws RecordException {
        String a = null, b = null;
        if (table1.compareTo(table2) <= 0) {
            a = table1;
            b = table2;
        } else {
            a = table2;
            b = table1;
        }
        a = TableNameResolver.getTableName(a, isItalian());
        b = TableNameResolver.getTableName(b, isItalian());
        Table t = new Table(a + "_" + b, tableOptions);
        t.addColumn("fk_" + a, "integer", "null : false, primary : true");
        t.addColumn("fk_" + b, "integer", "null : false, primary : true");
        createTable(t, false);
    }

    protected final void dropManyToManyRelationship(String table1, String table2) throws RecordException {
        dropManyToManyRelationship(table1, table2, false);
    }

    protected final void dropManyToManyRelationship(String table1, String table2, boolean ifExsits) throws RecordException {
        String a = null, b = null;
        if (table1.compareTo(table2) <= 0) {
            a = table1;
            b = table2;
        } else {
            a = table2;
            b = table1;
        }
        a = TableNameResolver.getTableName(a, isItalian());
        b = TableNameResolver.getTableName(b, isItalian());
        dropTable(a + "_" + b, ifExsits, false);
    }

    protected final void insertManyToManyRelationship(String table1, String table2, int ids1[], int ids2[]) throws RecordException {
        if (ids1 == null || ids1.length == 0) {
            throw new RecordException("The array of ids of table 1 must have at least a value");
        }
        if (ids2 == null || ids2.length == 0) {
            throw new RecordException("The array of ids of table 2 must have at least a value");
        }
        if (ids1.length != ids2.length) {
            throw new RecordException("The array of ids of table 1 must have the same number of ids of table 2 one");
        }
        int idsA[] = null, idsB[] = null;
        if (table1.compareTo(table2) <= 0) {
            idsA = ids1;
            idsB = ids2;
        } else {
            idsA = ids2;
            idsB = ids1;
        }
        for (int i = 0; i < idsA.length; ++i) {
            insert(TableNameResolver.getRelationshipTableName(table1, table2, isItalian()), false, idsA[i], idsB[i]);
        }
    }

    /**
	 * If called this method throws an IrreversibleException. It can be used in the douwn() method to mark a Migration as irreversible
	 * 
	 * @throws IrreversibleException
	 *            Always thrown
	 */
    protected final void irreversibleMigration() throws IrreversibleException {
        throw new IrreversibleException("This migration is irreversible.");
    }

    private boolean isItalian() {
        return this.getClass().isAnnotationPresent(ItalianName.class);
    }
}
