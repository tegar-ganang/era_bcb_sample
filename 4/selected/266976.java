package saadadb.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import saadadb.collection.Category;
import saadadb.command.ArgsParser;
import saadadb.configuration.RelationConf;
import saadadb.exceptions.AbortException;
import saadadb.exceptions.FatalException;
import saadadb.exceptions.SaadaException;
import saadadb.newdatabase.NewSaadaDB;
import saadadb.newdatabase.NewSaadaDBTool;
import saadadb.sqltable.SQLQuery;
import saadadb.sqltable.SQLTable;
import saadadb.util.Merger;
import saadadb.util.Messenger;

/**
 * @author laurentmichel
 * * @version $Id: PostgresWrapper.java 329 2012-04-05 09:38:29Z laurent.mistahl $

 */
public class PostgresWrapper extends DbmsWrapper {

    private PostgresWrapper(String server_or_driver, String port_or_url) throws ClassNotFoundException {
        super(false);
        test_base = "testbasededonnees";
        test_table = "TableTest";
        if (server_or_driver.startsWith("org.postgresql.Driver")) {
            this.driver = server_or_driver;
            this.url = port_or_url;
        } else {
            driver = "org.postgresql.Driver";
            url = "jdbc:postgresql:";
            Class.forName(driver);
            if (server_or_driver != null && server_or_driver.length() > 0) {
                url += "//" + server_or_driver;
                if (port_or_url != null && port_or_url.length() > 0) {
                    url += ":" + port_or_url;
                }
                url += "/";
            }
        }
    }

    public static DbmsWrapper getWrapper(String server_or_url, String port_or_url) throws ClassNotFoundException {
        if (wrapper != null) {
            return wrapper;
        }
        return new PostgresWrapper(server_or_url, port_or_url);
    }

    @Override
    public void createDB(String dbname) throws SQLException, FatalException {
        Messenger.printMsg(Messenger.TRACE, "Create database <" + dbname + "> at " + url);
        Connection admin_connection = DriverManager.getConnection(url + "template1", admin.getName(), admin.getPassword());
        Statement stmt = admin_connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        try {
            stmt.executeUpdate("CREATE DATABASE \"" + dbname + "\"");
            admin_connection.close();
        } catch (SQLException e) {
            Messenger.printStackTrace(e);
            if (admin_connection != null) admin_connection.close();
            FatalException.throwNewException(SaadaException.DB_ERROR, e.getMessage());
        }
    }

    @Override
    public boolean dbExists(String repository, String dbname) {
        try {
            Connection admin_connection = DriverManager.getConnection(url + dbname, admin.getName(), admin.getPassword());
            admin_connection.close();
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    @Override
    public void dropDB(String repository, String dbname) throws SQLException {
        Messenger.printMsg(Messenger.TRACE, "Remove database <" + dbname + ">");
        Connection admin_connection = DriverManager.getConnection(url + "template1", admin.getName(), admin.getPassword());
        Statement stmt = admin_connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        try {
            stmt.executeUpdate("DROP DATABASE \"" + dbname + "\"");
            admin_connection.close();
        } catch (SQLException e) {
            admin_connection.close();
            throw new SQLException(e.getMessage());
        }
    }

    @Override
    public void cleanUp() throws SQLException {
        this.dropDB(null, test_base);
    }

    @Override
    public String[] getStoreTable(String table_name, int ncols, String table_file) throws Exception {
        return new String[] { lockTable(table_name), "COPY " + table_name + " FROM '" + table_file.replaceAll("\\\\", "\\\\\\\\") + "'" };
    }

    @Override
    public String abortTransaction() {
        return "ABORT";
    }

    @Override
    public String lockTable(String table) {
        return "";
    }

    @Override
    public String lockTables(String[] write_table, String[] read_table) {
        String wt = Merger.getFilteredAndMergedArray(write_table);
        String rt = Merger.getFilteredAndMergedArray(read_table);
        if (wt.length() > 0 && rt.length() > 0) {
            return "";
        } else if (wt.length() > 0 && rt.length() == 0) {
            return "";
        } else if (wt.length() == 0 && rt.length() > 0) {
            return "";
        } else {
            return "";
        }
    }

    @Override
    public String dropTable(String table) {
        return "DROP TABLE " + table;
    }

    @Override
    public String grantSelectToPublic(String table_name) {
        return "GRANT select ON TABLE " + table_name + " TO PUBLIC";
    }

    /**
	 * On psql, text type can  be indexed or used as primary key. 
	 * We take it because it has no length limitation
	 * @return
	 */
    @Override
    public String getIndexableTextType() {
        return "text";
    }

    @Override
    public String getBooleanAsString(boolean val) {
        if (val) {
            return "true";
        } else {
            return "false";
        }
    }

    public boolean getBooleanValue(Object rsval) {
        if ("true".equalsIgnoreCase(rsval.toString())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getSQLTypeFromJava(String typeJava) throws FatalException {
        if (typeJava.equals("short")) {
            return "int2";
        } else if (typeJava.equals("class java.lang.Long") || typeJava.equals("long")) {
            return "int8";
        } else if (typeJava.equals("class java.lang.Integer") || typeJava.equals("int")) {
            return "int4";
        } else if (typeJava.equals("class java.lang.Byte")) {
            return "smallint";
        } else if (typeJava.equals("class java.lang.Character")) {
            return "Character";
        } else if (typeJava.equals("char")) {
            return "character(1)";
        } else if (typeJava.equals("boolean")) {
            return "boolean";
        } else if (typeJava.equals("class java.lang.Double") || typeJava.equals("double")) {
            return "float8";
        } else if (typeJava.indexOf("String") >= 0) {
            return "text";
        } else if (typeJava.indexOf("Date") >= 0) {
            return "date";
        } else if (typeJava.equals("float") || typeJava.equals("class java.lang.Float")) {
            return "float4";
        } else if (typeJava.equals("byte")) {
            return "smallint";
        }
        FatalException.throwNewException(SaadaException.UNSUPPORTED_TYPE, "Cannot convert " + typeJava + " JAVA type");
        return "";
    }

    @Override
    public String getJavaTypeFromSQL(String typeSQL) throws FatalException {
        if (typeSQL.equals("int2")) {
            return "short";
        } else if (typeSQL.equals("int8")) {
            return "long";
        } else if (typeSQL.equals("int") || typeSQL.equals("int4")) {
            return "int";
        } else if (typeSQL.equals("smallint")) {
            return "byte";
        } else if (typeSQL.equals("character") || typeSQL.equals("character(1)") || typeSQL.equals("bpchar")) {
            return "char";
        } else if (typeSQL.equals("bool")) {
            return "boolean";
        } else if (typeSQL.equals("float8") || typeSQL.equals("numeric")) {
            return "double";
        } else if (typeSQL.equals("text") || typeSQL.startsWith("character(")) {
            return "String";
        } else if (typeSQL.equals("date")) {
            return "Date";
        } else if (typeSQL.equals("float4")) {
            return "float";
        } else {
            return "String";
        }
    }

    @Override
    public String getUpdateWithJoin(String table_to_update, String table_to_join, String join_criteria, String key_alias, String[] keys, String[] values, String select_criteria) {
        String set_to_update = "";
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                set_to_update += ", ";
            }
            set_to_update += keys[i] + " = " + values[i];
        }
        return "UPDATE " + table_to_update + " SET " + set_to_update + " FROM " + table_to_join + " WHERE " + join_criteria + " AND " + select_criteria;
    }

    @Override
    public String getRegexpOp() {
        return "~";
    }

    @Override
    public String getInsertStatement(String where, String[] fields, String[] values) {
        return "INSERT INTO " + where + " (" + Merger.getMergedArray(fields) + ") VALUES (" + Merger.getMergedArray(values) + ")";
    }

    @Override
    public void createRelationshipTable(RelationConf relation_conf) throws SaadaException {
        String sqlCreateTable = "";
        sqlCreateTable = " oidprimary  int8, oidsecondary int8 ";
        for (String q : relation_conf.getQualifier().keySet()) {
            sqlCreateTable = sqlCreateTable + "," + q + "  double precision";
        }
        SQLTable.createTable(relation_conf.getNameRelation().toLowerCase(), sqlCreateTable, null, true);
    }

    @Override
    public void setClassColumns(String relationName) throws AbortException {
    }

    @Override
    public void suspendRelationTriggger(String relationName) throws AbortException {
    }

    @Override
    public String getSecondaryRelationshipIndex(String relationName) {
        return "CREATE INDEX " + relationName.toLowerCase() + "_secoid_class ON " + relationName + " ( ((oidsecondary>>32) & 65535::bigint) )";
    }

    @Override
    public String getPrimaryRelationshipIndex(String relationName) {
        return "CREATE INDEX " + relationName.toLowerCase() + "_primoid_class ON " + relationName + " ( ((oidprimary>>32) & 65535::bigint) )";
    }

    @Override
    public String getPrimaryClassColumn() {
        return "((oidprimary>>32) & 65535::bigint)";
    }

    @Override
    public String getSecondaryClassColumn() {
        return "((oidsecondary>>32) & 65535::bigint)";
    }

    @Override
    public boolean tableExist(String searched_table) throws Exception {
        DatabaseMetaData dm = Database.get_connection().getMetaData();
        ResultSet rsTables = dm.getTables(null, "public", searched_table.toLowerCase(), null);
        while (rsTables.next()) {
            String tableName = rsTables.getString("TABLE_NAME");
            if (searched_table.equalsIgnoreCase(tableName.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ResultSet getTableColumns(String searched_table) throws Exception {
        if (!tableExist(searched_table)) {
            if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "Table <" + searched_table + "> does not exist");
            return null;
        }
        DatabaseMetaData dm = Database.get_connection().getMetaData();
        ResultSet rsTables = dm.getTables(null, null, searched_table.toLowerCase(), null);
        while (rsTables.next()) {
            String tableName = rsTables.getString("TABLE_NAME");
            if (searched_table.equalsIgnoreCase(tableName.toLowerCase())) {
                return dm.getColumns(null, null, tableName, null);
            }
        }
        return null;
    }

    @Override
    public Map<String, String> getExistingIndex(String searched_table) throws FatalException {
        try {
            if (!tableExist(searched_table)) {
                if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "Table <" + searched_table + "> does not exist");
                return null;
            }
            DatabaseMetaData dm = Database.get_connection().getMetaData();
            ResultSet resultat = dm.getIndexInfo(null, null, searched_table.toLowerCase(), false, false);
            HashMap<String, String> retour = new HashMap<String, String>();
            while (resultat.next()) {
                String col = resultat.getObject("COLUMN_NAME").toString();
                String iname = resultat.getObject("INDEX_NAME").toString();
                if (iname != null && col != null) {
                    retour.put(iname, col);
                }
            }
            return retour;
        } catch (Exception e) {
            AbortException.throwNewException(SaadaException.INTERNAL_ERROR, e);
            return null;
        }
    }

    @Override
    public String getExceptStatement(String key) {
        return " EXCEPT ";
    }

    @Override
    public String getDropIndexStatement(String table_name, String index_name) {
        return "DROP INDEX " + index_name;
    }

    @Override
    public String getCollectionTableName(String coll_name, int cat) throws FatalException {
        return coll_name + "_" + Category.explain(cat);
    }

    @Override
    public String getGlobalAlias(String alias) {
        return "AS " + alias;
    }

    @Override
    public String castToString(String token) {
        return token + "::text";
    }

    @Override
    public String getTempodbName(String dbname) {
        return dbname;
    }

    @Override
    public String getTempoTableName(String table_name) throws FatalException {
        return table_name;
    }

    @Override
    public String getCreateTempoTable(String table_name, String fmt) throws FatalException {
        return "CREATE TEMPORARY TABLE " + table_name + " " + fmt + " ON COMMIT DROP";
    }

    @Override
    public String[] changeColumnType(String table, String column, String type) {
        return new String[] { "ALTER TABLE " + table + " ALTER COLUMN  " + column + " TYPE " + type };
    }

    @Override
    public String[] addColumn(String table, String column, String type) {
        return new String[] { "ALTER TABLE " + table + " ADD COLUMN  " + column + "  " + type };
    }

    @Override
    public String getDropTempoTable(String table_name) throws FatalException {
        return "";
    }

    public String dropProcedure(String proc_name) {
        return "DROP FUNCTION " + proc_name;
    }

    /**
	 * @throws Exception
	 */
    @Override
    protected void installLanguage() throws Exception {
        boolean dontforgettoopentransaction = false;
        if (SQLTable.isTransactionOpen()) {
            dontforgettoopentransaction = true;
        } else {
            SQLTable.beginTransaction();
        }
        SQLTable.addQueryToTransaction("CREATE OR REPLACE FUNCTION make_plpgsql() \n" + " RETURNS VOID \n" + " 	AS $$ CREATE LANGUAGE plpgsql $$ LANGUAGE SQL;\n");
        SQLTable.commitTransaction();
        SQLQuery sq = new SQLQuery("SELECT \n" + " CASE \n" + " WHEN EXISTS( \n" + "     SELECT 1 \n" + "     FROM pg_catalog.pg_language \n" + "    WHERE lanname='plpgsql' \n" + " ) \n" + " THEN NULL \n" + " ELSE make_plpgsql() END; \n");
        sq.run();
        sq.close();
        SQLTable.beginTransaction();
        SQLTable.addQueryToTransaction("DROP FUNCTION make_plpgsql();");
    }

    protected void installLanguage(Statement stmt) throws Exception {
        stmt.executeUpdate("CREATE OR REPLACE FUNCTION make_plpgsql() \n" + " RETURNS VOID \n" + " 	AS $$ CREATE LANGUAGE plpgsql $$ LANGUAGE SQL;\n");
        stmt.execute("SELECT \n" + " CASE \n" + " WHEN EXISTS( \n" + "     SELECT 1 \n" + "     FROM pg_catalog.pg_language \n" + "    WHERE lanname='plpgsql' \n" + " ) \n" + " THEN NULL \n" + " ELSE make_plpgsql() END; \n");
        stmt.executeUpdate("DROP FUNCTION make_plpgsql();");
    }

    @Override
    protected File getProcBaseRef() throws Exception {
        String base_dir = System.getProperty("user.home") + Database.getSepar() + "workspace" + Database.getSepar() + Database.getName() + Database.getSepar() + "sqlprocs" + Database.getSepar() + "postgresql";
        if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "Look for SQL procs in " + base_dir);
        File bf = new File(base_dir);
        if (!(bf.exists() && bf.isDirectory())) {
            base_dir = Database.getRoot_dir() + Database.getSepar() + "sqlprocs" + Database.getSepar() + "postgresql";
            if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "Look for SQL procs in " + base_dir);
            bf = new File(base_dir);
            if (!(bf.exists() && bf.isDirectory())) {
                base_dir = NewSaadaDB.SAADA_HOME + Database.getSepar() + "dbtemplate" + Database.getSepar() + "sqlprocs" + Database.getSepar() + "postgresql";
                if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "Look for SQL procs in " + base_dir);
                Messenger.printMsg(Messenger.TRACE, "No SQL procedure found try SAADA install dir " + base_dir);
                bf = new File(base_dir);
                if (!(bf.exists() && bf.isDirectory())) {
                    base_dir = NewSaadaDBTool.saada_home + Database.getSepar() + "dbtemplate" + Database.getSepar() + "sqlprocs" + Database.getSepar() + "postgresql";
                    if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "Look for SQL procs in " + base_dir);
                    Messenger.printMsg(Messenger.TRACE, "No SQL procedure found try SAADA install dir " + base_dir);
                    bf = new File(base_dir);
                    if (!(bf.exists() && bf.isDirectory())) {
                        FatalException.throwNewException(SaadaException.FILE_ACCESS, "Can not access SQL procedure directory");
                    }
                }
            }
        }
        return bf;
    }

    public static void main(String[] args) {
        try {
            ArgsParser ap = new ArgsParser(args);
            Database.init(ap.getDBName());
            Database.getConnector().setAdminMode(ap.getPassword());
            Database.getWrapper().loadSQLProcedures();
            SQLTable.commitTransaction();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
