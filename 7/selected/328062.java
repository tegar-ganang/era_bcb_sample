package library.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import library.enums.Library;
import library.utils._Properties;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DataSourceConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.postgresql.ds.PGSimpleDataSource;

public class PostgresPooledDatabase extends AbstractLibraryDatabase {

    protected AbstractLibraryDatabase.Process process;

    protected DataSource source;

    protected Map<String, SessionFactory> factories;

    private Properties props;

    private final String databaseName;

    private final String password;

    private final String regconfig;

    private final String user;

    {
        props = new _Properties("/database-connection.properties");
        databaseName = props.getProperty("databaseName");
        password = props.getProperty("password");
        regconfig = props.getProperty("regconfig");
        user = props.getProperty("user");
        PGSimpleDataSource dsource = new PGSimpleDataSource();
        dsource.setDatabaseName(databaseName);
        dsource.setUser(user);
        dsource.setPassword(password);
        ConnectionFactory cfact = new DataSourceConnectionFactory(dsource);
        GenericObjectPool gopool = new GenericObjectPool();
        gopool.setMaxActive(-1);
        gopool.setMaxIdle(-1);
        gopool.setMinEvictableIdleTimeMillis(-1);
        new PoolableConnectionFactory(cfact, gopool, null, null, false, true);
        source = new PoolingDataSource(gopool);
        process = new AbstractLibraryDatabase.Process();
        ThreadLocalDataSource.set(source);
        System.clearProperty("hibernate.dialect");
        System.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        System.clearProperty("hibernate.library");
        System.setProperty("hibernate.library", Library.READERS_INDEX.name());
        SessionFactory readerSession = new Configuration().configure().buildSessionFactory();
        System.clearProperty("hibernate.library");
        System.setProperty("hibernate.library", Library.STAFF_INDEX.name());
        SessionFactory staffSession = new Configuration().configure().buildSessionFactory();
        factories = new HashMap<String, SessionFactory>();
        factories.put(Library.READERS_INDEX.name(), readerSession);
        factories.put(Library.STAFF_INDEX.name(), staffSession);
        factories = Collections.unmodifiableMap(factories);
    }

    @Override
    protected Map<String, SessionFactory> getSessionFactories() {
        return factories;
    }

    @Override
    protected Connection getConnection(Library library) throws SQLException {
        Connection conn = source.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("SET search_path TO " + library.getDatabaseName());
        stmt.close();
        conn.setAutoCommit(true);
        return conn;
    }

    @Override
    protected Library getLibrary(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery("SHOW search_path");
        if (rset.next()) {
            String lib = rset.getString("search_path");
            Library library = Library.getValue(lib);
            return library;
        } else {
            throw new SQLException("Error while getting library: no schema fetched!");
        }
    }

    @Override
    protected AbstractLibraryDatabase.Process getProcessSingleton() {
        return process;
    }

    @Override
    protected void setLibrary(Library library, Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        String command = "SET search_path TO " + library.getDatabaseName();
        stmt.execute(command);
        stmt.close();
    }

    @Override
    public Map<Integer, Integer[]> prune() throws SQLException {
        Connection conn = source.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("VACUUM ANALYZE");
        Map<Integer, Integer[]> result = super.prune();
        stmt.execute("VACUUM ANALYZE");
        return result;
    }

    protected class Process extends AbstractLibraryDatabase.Process {

        @Override
        protected String[] searchForWordsInFields(String[] words, String adapter, String columnID, String column, String table, Class<?>... typeOfValue) {
            String whereClause = "";
            for (@SuppressWarnings("unused") String word : words) {
                if (!whereClause.equals("")) {
                    whereClause += " OR ";
                }
                if ((typeOfValue.length > 0) && (Enum.class.isAssignableFrom(typeOfValue[0]))) {
                    whereClause += column + "=?";
                } else {
                    whereClause += "(to_tsvector('" + regconfig + "', lower(" + column + ")) @@ to_tsquery( lower(?) ))";
                }
            }
            String[] result;
            if (words.length > 0) {
                result = new String[words.length + 1];
                result[0] = "SELECT " + COLUMN_DOCUMENT_ID + " FROM (SELECT " + columnID + " FROM " + table + " WHERE " + whereClause + ") AS alias0 JOIN " + adapter + " USING(" + columnID + ")";
                for (int i = 1; i < result.length; i++) {
                    result[i] = words[i - 1];
                }
            } else {
                result = new String[0];
            }
            return result;
        }

        @Override
        protected String[] searchForWordsInFields(String[] words, String adapter, String specificColumnID, String generalColumnID, String columnLast, String columnFirst, String table) {
            String whereClause = "";
            for (@SuppressWarnings("unused") String word : words) {
                if (!whereClause.equals("")) {
                    whereClause += " OR ";
                }
                whereClause += "(to_tsvector('" + regconfig + "', lower(" + columnLast + ")) @@ to_tsquery( lower(?) )) OR " + "(to_tsvector('" + regconfig + "', lower(" + columnFirst + ")) @@ to_tsquery( lower(?) ))";
            }
            String[] result;
            if (words.length > 0) {
                result = new String[2 * words.length + 1];
                result[0] = "SELECT " + COLUMN_DOCUMENT_ID + " FROM (SELECT " + generalColumnID + " FROM " + table + " WHERE " + whereClause + ") AS alias0 JOIN " + adapter + " ON(" + specificColumnID + "=" + generalColumnID + ")";
                for (int i = 1; i < result.length; i = i + 2) {
                    result[i] = result[i + 1] = words[(i - 1) / 2];
                }
            } else {
                result = new String[0];
            }
            return result;
        }
    }
}
