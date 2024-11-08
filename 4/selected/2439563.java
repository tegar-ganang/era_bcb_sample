package saadadb.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import saadadb.collection.Category;
import saadadb.configuration.RelationConf;
import saadadb.exceptions.AbortException;
import saadadb.exceptions.FatalException;
import saadadb.exceptions.SaadaException;
import saadadb.newdatabase.NewSaadaDB;
import saadadb.newdatabase.NewSaadaDBTool;
import saadadb.sqltable.SQLQuery;
import saadadb.sqltable.SQLTable;
import saadadb.util.Messenger;

public class MysqlWrapper extends DbmsWrapper {

    private final TreeSet<String> recorded_tmptbl = new TreeSet<String>();

    /** * @version $Id: MysqlWrapper.java 378 2012-05-02 16:09:53Z laurent.mistahl $

	 * @param server_or_driver
	 * @param port_or_url
	 * @throws ClassNotFoundException
	 */
    private MysqlWrapper(String server_or_driver, String port_or_url) throws ClassNotFoundException {
        super(false);
        test_base = "testbasededonnees";
        test_table = "TableTest";
        if (server_or_driver.startsWith("com.mysql.jdbc.Driver")) {
            this.driver = server_or_driver;
            this.url = port_or_url;
        } else {
            driver = "com.mysql.jdbc.Driver";
            url = "jdbc:mysql:";
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
        return new MysqlWrapper(server_or_url, port_or_url);
    }

    @Override
    public void createDB(String dbname) throws Exception {
        Messenger.printMsg(Messenger.TRACE, "Create database <" + dbname + "> at " + url);
        Connection admin_connection = getConnection(url + "mysql", admin.getName(), admin.getPassword());
        Statement stmt = admin_connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        try {
            stmt.executeUpdate("CREATE DATABASE " + dbname);
            if (reader != null && !reader.getName().equals(admin.getName())) {
                stmt.executeUpdate("grant select on " + dbname + ".* to " + reader.getName());
            }
            stmt.executeUpdate("CREATE DATABASE " + getTempodbName(dbname));
            if (reader != null && !reader.getName().equals(admin.getName())) {
                stmt.executeUpdate("grant SELECT, INSERT, UPDATE, DELETE, DROP, ALTER, CREATE, LOCK TABLES on " + getTempodbName(dbname) + ".* to " + reader.getName());
            }
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
        Connection admin_connection = DriverManager.getConnection(url + "mysql", admin.getName(), admin.getPassword());
        Statement stmt = admin_connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        try {
            stmt.executeUpdate("DROP DATABASE " + dbname + "");
            stmt.executeUpdate("DROP DATABASE " + getTempodbName(dbname) + "");
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
    public String getOptions() {
        return "?autoReconnect=true";
    }

    @Override
    public String[] getStoreTable(String table_name, int ncols, String table_file) throws Exception {
        return new String[] { "ALTER TABLE " + table_name + " DISABLE KEYS", "LOAD DATA INFILE '" + table_file.replaceAll("\\\\", "\\\\\\\\") + "' INTO TABLE " + table_name, "ALTER TABLE " + table_name + " ENABLE KEYS" };
    }

    @Override
    public String abortTransaction() {
        return "ROLLBACK";
    }

    @Override
    public String lockTable(String table) {
        return "";
    }

    @Override
    public String grantSelectToPublic(String table_name) {
        if (this.reader != null) {
            return "GRANT select ON TABLE " + table_name + " TO '" + this.reader.getName() + "'";
        } else {
            return "";
        }
    }

    @Override
    public String lockTables(String[] write_table, String[] read_table) throws Exception {
        String wt = "";
        if (write_table != null) {
            for (int i = 0; i < write_table.length; i++) {
                String tn = write_table[i].trim();
                if (tn.length() == 0) {
                    continue;
                }
                if (i > 0) {
                    wt += ", ";
                }
                wt += tn + " WRITE";
            }
        }
        String rt = "";
        if (read_table != null) {
            for (int i = 0; i < read_table.length; i++) {
                String tn = read_table[i].trim();
                if (tn.length() == 0) {
                    continue;
                }
                if (i > 0) {
                    rt += ", ";
                }
                rt += tn + " READ";
            }
        }
        return "";
    }

    @Override
    public String dropTable(String table) {
        return "DROP TABLE " + table;
    }

    /**
	 * On Mysql, text type can neither be indexed nor used as primary key. So varchar(255) is used despite of 
	 * the limitation length 
	 * @return
	 */
    @Override
    public String getIndexableTextType() {
        return "varchar(255) binary";
    }

    @Override
    public String getBooleanAsString(boolean val) {
        if (val) {
            return "1";
        } else {
            return "0";
        }
    }

    public boolean getBooleanValue(Object rsval) {
        if ("1".equalsIgnoreCase(rsval.toString())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getUpdateWithJoin(String table_to_update, String table_to_join, String join_criteria, String key_alias, String[] keys, String[] values, String select_criteria) {
        String set_to_update = "";
        String ka = "";
        if (key_alias != null && key_alias.length() > 0) {
            ka = key_alias + ".";
        }
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                set_to_update += ", ";
            }
            set_to_update += ka + keys[i] + " = " + values[i];
        }
        return "UPDATE " + table_to_update + "  JOIN " + table_to_join + " ON " + join_criteria + " SET " + set_to_update + " WHERE " + select_criteria;
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
            return "double precision";
        } else if (typeJava.indexOf("String") >= 0) {
            return "varchar(255)";
        } else if (typeJava.indexOf("Date") >= 0) {
            return "date";
        } else if (typeJava.equals("float")) {
            return "float4";
        } else if (typeJava.equals("byte") || typeJava.equals("class java.lang.Float")) {
            return "smallint";
        }
        FatalException.throwNewException(SaadaException.UNSUPPORTED_TYPE, "Cannot convert " + typeJava + " JAVA type");
        return "";
    }

    @Override
    public String getJavaTypeFromSQL(String typeSQL) throws FatalException {
        if (typeSQL.equalsIgnoreCase("smallint") || typeSQL.equalsIgnoreCase("int2")) {
            return "short";
        } else if (typeSQL.equalsIgnoreCase("int8") || typeSQL.equalsIgnoreCase("bigint")) {
            return "long";
        } else if (typeSQL.equalsIgnoreCase("int") || typeSQL.equalsIgnoreCase("int4")) {
            return "int";
        } else if (typeSQL.equalsIgnoreCase("tinyint")) {
            return "boolean";
        } else if (typeSQL.equalsIgnoreCase("char") || typeSQL.equalsIgnoreCase("character") || typeSQL.equalsIgnoreCase("character(1)")) {
            return "char";
        } else if (typeSQL.equalsIgnoreCase("boolean")) {
            return "boolean";
        } else if (typeSQL.equalsIgnoreCase("double precision") || typeSQL.equalsIgnoreCase("float8") || typeSQL.equalsIgnoreCase("double") || typeSQL.equalsIgnoreCase("decimal") || typeSQL.equalsIgnoreCase("numeric")) {
            return "double";
        } else if (typeSQL.equalsIgnoreCase("text") || typeSQL.startsWith("character(") || typeSQL.equalsIgnoreCase("varchar") || typeSQL.equalsIgnoreCase("varbinary")) {
            return "String";
        } else if (typeSQL.equalsIgnoreCase("date")) {
            return "Date";
        } else if (typeSQL.equalsIgnoreCase("float") || typeSQL.equalsIgnoreCase("float4")) {
            return "float";
        } else {
            return "String";
        }
    }

    @Override
    public String[] getUserTables() {
        return new String[] { "mysql.user", "mysql.tables_priv" };
    }

    @Override
    public String unlockTables() {
        return "UNLOCK TABLES";
    }

    @Override
    public String getRegexpOp() {
        return "REGEXP";
    }

    @Override
    public String getInsertStatement(String where, String[] fields, String[] values) {
        String retour = "INSERT " + where + " SET ";
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                retour += ", ";
            }
            retour += fields[i] + " = " + values[i];
        }
        return retour;
    }

    @Override
    public void createRelationshipTable(RelationConf relation_conf) throws SaadaException {
        String sqlCreateTable = "";
        sqlCreateTable = " oidprimary  int8, oidsecondary int8, primaryclass int4 , secondaryclass int4";
        for (String q : relation_conf.getQualifier().keySet()) {
            sqlCreateTable = sqlCreateTable + "," + q + "  double precision";
        }
        SQLTable.createTable(relation_conf.getNameRelation(), sqlCreateTable, null, true);
        SQLTable.addQueryToTransaction("CREATE TRIGGER " + relation_conf.getNameRelation() + "_secclass  \n" + "BEFORE INSERT ON " + relation_conf.getNameRelation() + "\n" + "FOR EACH ROW \n" + "SET NEW.secondaryclass = ((new.oidsecondary>>32) & 65535), NEW.primaryclass = ((new.oidprimary>>32) & 65535)");
    }

    @Override
    public void suspendRelationTriggger(String relationName) throws AbortException {
        SQLTable.addQueryToTransaction("DROP TRIGGER " + relationName + "_secclass");
    }

    @Override
    public void setClassColumns(String relationName) throws AbortException {
        SQLTable.addQueryToTransaction("UPDATE " + relationName + " SET primaryclass   = ((oidprimary>>32)  & 65535);");
        SQLTable.addQueryToTransaction("UPDATE " + relationName + " SET secondaryclass = ((oidsecondary>>32) & 65535);");
        SQLTable.addQueryToTransaction("CREATE TRIGGER " + relationName + "_secclass  \n" + "BEFORE INSERT ON " + relationName + "\n" + "FOR EACH ROW \n" + "SET NEW.secondaryclass = ((new.oidsecondary>>32) & 65535), NEW.primaryclass = ((new.oidprimary>>32) & 65535)");
    }

    @Override
    public String getSecondaryRelationshipIndex(String relationName) {
        return "CREATE INDEX " + relationName.toLowerCase() + "_secoid_class ON " + relationName + " ( secondaryclass )";
    }

    @Override
    public String getPrimaryRelationshipIndex(String relationName) {
        return "CREATE INDEX " + relationName.toLowerCase() + "_primoid_class ON " + relationName + " ( primaryclass )";
    }

    @Override
    public String getPrimaryClassColumn() {
        return "primaryclass";
    }

    @Override
    public String getSecondaryClassColumn() {
        return "secondaryclass";
    }

    @Override
    public boolean tableExist(String searched_table) throws Exception {
        String[] sc = searched_table.split("\\.");
        DatabaseMetaData dm = Database.get_connection().getMetaData();
        ;
        ResultSet rsTables;
        if (sc.length == 1) {
            rsTables = dm.getTables(null, null, searched_table, null);
            while (rsTables.next()) {
                String tableName = rsTables.getString("TABLE_NAME");
                if (searched_table.equalsIgnoreCase(tableName)) {
                    return true;
                }
            }
        } else {
            rsTables = dm.getTables(sc[0], null, sc[1], null);
            while (rsTables.next()) {
                String tableName = rsTables.getString("TABLE_NAME");
                if (sc[1].equalsIgnoreCase(tableName)) {
                    return true;
                }
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
        ResultSet rsTables;
        String[] sc = searched_table.split("\\.");
        if (sc.length == 1) {
            rsTables = dm.getTables(null, null, searched_table, null);
            while (rsTables.next()) {
                String tableName = rsTables.getString("TABLE_NAME");
                if (searched_table.equalsIgnoreCase(tableName)) {
                    return dm.getColumns(null, null, tableName, null);
                }
            }
            return null;
        } else {
            rsTables = dm.getTables(sc[0], null, sc[1], null);
            while (rsTables.next()) {
                String tableName = rsTables.getString("TABLE_NAME");
                if (sc[1].equalsIgnoreCase(tableName)) {
                    return dm.getColumns(sc[0], null, sc[1], null);
                }
            }
            return null;
        }
    }

    @Override
    public Map<String, String> getExistingIndex(String searched_table) throws FatalException {
        try {
            if (!tableExist(searched_table)) {
                if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "Table <" + searched_table + "> does not exist");
                return null;
            }
            DatabaseMetaData dm = Database.get_connection().getMetaData();
            ResultSet resultat;
            String[] sc = searched_table.split("\\.");
            if (sc.length == 1) {
                resultat = dm.getIndexInfo(null, null, searched_table, false, false);
                HashMap<String, String> retour = new HashMap<String, String>();
                while (resultat.next()) {
                    String col = resultat.getObject("COLUMN_NAME").toString();
                    String iname = resultat.getObject("INDEX_NAME").toString();
                    if (iname != null && col != null && !iname.equals("PRIMARY")) {
                        retour.put(iname.toString(), col);
                    }
                }
                return retour;
            } else {
                resultat = dm.getIndexInfo(sc[0], null, sc[1], false, false);
                HashMap<String, String> retour = new HashMap<String, String>();
                while (resultat.next()) {
                    String col = resultat.getObject("COLUMN_NAME").toString();
                    String iname = resultat.getObject("INDEX_NAME").toString();
                    if (iname != null && col != null && !iname.equals("PRIMARY")) {
                        retour.put(iname.toString(), col);
                    }
                }
                return retour;
            }
        } catch (Exception e) {
            Messenger.printStackTrace(e);
            AbortException.throwNewException(SaadaException.INTERNAL_ERROR, e);
            return null;
        }
    }

    @Override
    public String getExceptStatement(String key) {
        return " WHERE " + key + " NOT IN ";
    }

    @Override
    public String getDropIndexStatement(String table_name, String index_name) {
        return "DROP INDEX " + index_name + " ON " + table_name;
    }

    @Override
    public String getCollectionTableName(String coll_name, int cat) throws FatalException {
        return coll_name + "_" + Category.explain(cat).toLowerCase();
    }

    @Override
    public String getGlobalAlias(String alias) {
        return "";
    }

    @Override
    public String castToString(String token) {
        return token;
    }

    @Override
    public String getTempodbName(String dbname) {
        return dbname + "_tempo";
    }

    @Override
    public String getTempoTableName(String table_name) throws FatalException {
        return Database.getTempodbName() + "." + table_name;
    }

    @Override
    public String getCreateTempoTable(String table_name, String fmt) throws FatalException {
        recorded_tmptbl.add(table_name);
        return "CREATE TABLE " + Database.getTempodbName() + "." + table_name + " " + fmt;
    }

    @Override
    public String getDropTempoTable(String table_name) throws FatalException {
        return "DROP TABLE IF EXISTS " + Database.getTempodbName() + "." + table_name;
    }

    @Override
    public String[] changeColumnType(String table, String column, String type) {
        return new String[] { "ALTER TABLE " + table + " MODIFY  " + column + " " + type };
    }

    @Override
    public String[] addColumn(String table, String column, String type) {
        return new String[] { "ALTER TABLE " + table + " ADD  COLUMN " + column + " " + type };
    }

    @Override
    public Set<String> getReferencedTempTable() {
        return recorded_tmptbl;
    }

    @Override
    protected File getProcBaseRef() throws Exception {
        String base_dir = System.getProperty("user.home") + Database.getSepar() + "workspace" + Database.getSepar() + Database.getName() + Database.getSepar() + "sqlprocs" + Database.getSepar() + "mysql";
        if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "Look for SQL procs in " + base_dir);
        File bf = new File(base_dir);
        if (!(bf.exists() && bf.isDirectory())) {
            base_dir = Database.getRoot_dir() + Database.getSepar() + "sqlprocs" + Database.getSepar() + "mysql";
            if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "Look for SQL procs in " + base_dir);
            bf = new File(base_dir);
            if (!(bf.exists() && bf.isDirectory())) {
                base_dir = NewSaadaDB.SAADA_HOME + Database.getSepar() + "dbtemplate" + Database.getSepar() + "sqlprocs" + Database.getSepar() + "mysql";
                if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "Look for SQL procs in " + base_dir);
                Messenger.printMsg(Messenger.TRACE, "No SQL procedure found try SAADA install dir " + base_dir);
                bf = new File(base_dir);
                if (!(bf.exists() && bf.isDirectory())) {
                    base_dir = NewSaadaDBTool.saada_home + Database.getSepar() + "dbtemplate" + Database.getSepar() + "sqlprocs" + Database.getSepar() + "mysql";
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

    protected String[] removeProc() throws Exception {
        SQLQuery sq = new SQLQuery("SHOW FUNCTION STATUS WHERE Db = '" + Database.getConnector().getDbname() + "'");
        ResultSet rs = sq.run();
        ArrayList<String> retour = new ArrayList<String>();
        while (rs.next()) {
            retour.add("DROP FUNCTION " + rs.getString("Name"));
        }
        return retour.toArray(new String[0]);
    }

    public static void main(String[] args) {
        try {
            Database.init("BENCH2_0_MSQL");
            SQLTable.beginTransaction();
            SQLTable.addQueryToTransaction("CREATE TABLE BENCH2_0_MSQL_tempo.x AS SELECT * from saadadb");
            SQLTable.commitTransaction();
        } catch (Exception e) {
            Messenger.printStackTrace(e);
            System.err.println(e.getMessage());
        }
    }
}
