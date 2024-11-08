package saadadb.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.sqlite.SQLiteConfig;
import saadadb.collection.Category;
import saadadb.configuration.RelationConf;
import saadadb.exceptions.AbortException;
import saadadb.exceptions.FatalException;
import saadadb.exceptions.QueryException;
import saadadb.exceptions.SaadaException;
import saadadb.sqltable.SQLLargeQuery;
import saadadb.sqltable.SQLQuery;
import saadadb.sqltable.SQLTable;
import saadadb.util.HardwareDescriptor;
import saadadb.util.Merger;
import saadadb.util.Messenger;

public class SQLiteWrapper extends DbmsWrapper {

    private static String db_file;

    private static boolean DEV_MODE = false;

    private static boolean EXT_LOADED = false;

    private static final String suffix = ".sqlitedb";

    private static final String driver_classname = "org.sqlite.JDBC";

    /** * @version $Id: SQLiteWrapper.java 118 2012-01-06 14:33:51Z laurent.mistahl $

	 * @param server_or_driver
	 * @param port_or_url
	 * @throws ClassNotFoundException
	 */
    private SQLiteWrapper(String server_or_driver, String port_or_url) throws ClassNotFoundException {
        super(true);
        test_base = "test" + suffix;
        test_table = "TableTest";
        if (server_or_driver.startsWith(driver_classname)) {
            this.driver = server_or_driver;
            this.url = port_or_url;
            Class.forName(driver);
        } else {
            this.driver = driver_classname;
            this.url = "jdbc:sqlite:" + port_or_url;
            Class.forName(driver);
        }
        db_file = port_or_url.replace("jdbc:sqlite:", "");
        Messenger.printMsg(Messenger.TRACE, "Linked with SQLITE db file " + db_file + " by driver " + this.driver);
    }

    /**
	 * @param server_or_url
	 * @param port_or_url
	 * @return
	 * @throws ClassNotFoundException
	 */
    public static DbmsWrapper getWrapper(String server_or_url, String port_or_url) throws Exception {
        if (wrapper != null) {
            return wrapper;
        }
        SQLiteWrapper sw = new SQLiteWrapper(server_or_url, port_or_url);
        return sw;
    }

    /**
	 * returns the path of the native module containing extensions
	 * @throws Exception
	 */
    public static String getExtensionFilePath() throws Exception {
        if (DEV_MODE) {
            String root_dir = "/Users/laurentmichel/Documents/workspace/SQLITEProc/Debug/";
            root_dir += HardwareDescriptor.getArchDependentLibName("libSQLITEProc");
            if (new File(root_dir).exists()) {
                return root_dir;
            } else {
                return System.getProperty("user.home") + Database.getSepar() + HardwareDescriptor.getArchDependentLibName("libSQLITEProc");
            }
        } else {
            ClassLoader cl = SQLiteWrapper.class.getClassLoader();
            String proc_libname = HardwareDescriptor.getArchDependentLibName("libSQLITEProc");
            Messenger.printMsg(Messenger.TRACE, "Looking for native library " + proc_libname);
            InputStream in = cl.getResourceAsStream(proc_libname);
            if (in == null) {
                throw new Exception("library " + proc_libname + " not found (supposed to be in sqliteprocs.jar)");
            }
            File otmplib = File.createTempFile("sqliteprocjdbc-", ".lib");
            otmplib.deleteOnExit();
            OutputStream out = new FileOutputStream(otmplib);
            byte[] buf = new byte[1024];
            for (int len; (len = in.read(buf)) != -1; ) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            return otmplib.getAbsolutePath();
        }
    }

    protected void dropTestDB(String tmp_dir) throws SQLException {
        File f = new File(tmp_dir + Database.getSepar() + test_base);
        if (f.exists()) {
            if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "DB " + f.getAbsolutePath() + " already exists: drop it");
            f.delete();
        }
    }

    protected void createTestDB(String tmp_dir) throws Exception {
        test_base = tmp_dir + Database.getSepar() + test_base;
        if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "Create test DB " + test_base);
        createDB(test_base);
    }

    /**
	 * @return
	 * @throws SQLException 
	 */
    @Override
    public Connection getConnection(String url, String user, String password) throws Exception {
        if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "User " + user + " Connecting to " + url);
        SQLiteConfig config = new SQLiteConfig();
        config.enableLoadExtension(true);
        Connection conn = DriverManager.getConnection(url, config.toProperties());
        if (!EXT_LOADED) {
            String efp = getExtensionFilePath();
            if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "Loading extensions from " + efp);
            Statement stat = conn.createStatement();
            try {
                stat.executeUpdate("select load_extension('" + efp + "')");
            } catch (Exception e) {
                e.printStackTrace();
                if (!EXT_LOADED) {
                } else {
                    Messenger.printMsg(Messenger.WARNING, "Can not load SQLITE extension, but they seem to be already here");
                }
            }
            stat.executeUpdate("pragma cache_size=200000");
            stat.executeUpdate("pragma temp_store=FILE");
            stat.close();
            EXT_LOADED = false;
        }
        return conn;
    }

    @Override
    public String getJdbcURL(String repository, String dbname) {
        return this.getUrl() + repository + Database.getSepar() + "embeddeddb" + Database.getSepar() + dbname + suffix;
    }

    public Connection openLargeQueryConnection() throws Exception {
        return Database.get_connection();
    }

    public void closeLargeQueryConnection(Connection largeConnection) throws SQLException {
    }

    @Override
    public int getDefaultScrollMode() {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public String abortTransaction() {
        return "ROLLBACK";
    }

    @Override
    public String castToString(String token) {
        return null;
    }

    @Override
    public String[] changeColumnType(String table, String column, String type) throws Exception {
        Messenger.printMsg(Messenger.WARNING, "Changing column type may be long with SQLITE");
        DatabaseMetaData dm = Database.get_connection().getMetaData();
        ResultSet resultat = dm.getColumns(null, null, table, null);
        String select = "";
        while (resultat.next()) {
            if (select.length() > 0) {
                select += ", ";
            }
            String cn = resultat.getString("COLUMN_NAME");
            if (column.equals(cn)) {
                select += "CAST(" + cn + " AS " + type + ") AS " + column;
            } else {
                select += cn;
            }
        }
        resultat.close();
        return new String[] { this.getCreateTableFromSelectStatement("colchanger", "SELECT " + select + "  FROM " + table), "DROP TABLE  " + table, "ALTER TABLE colchanger RENAME TO " + table };
    }

    @Override
    public String[] addColumn(String table, String column, String type) throws Exception {
        return new String[] { "ALTER TABLE " + table + " ADD  COLUMN " + column + " " + type };
    }

    @Override
    public void cleanUp() throws SQLException {
    }

    @Override
    public void createDB(String dbname) throws Exception {
    }

    @Override
    public void createRelationshipTable(RelationConf relation_conf) throws SaadaException {
        String sqlCreateTable = "";
        sqlCreateTable = " oidprimary  int8, oidsecondary int8, primaryclass int4 , secondaryclass int4";
        for (String q : relation_conf.getQualifier().keySet()) {
            sqlCreateTable = sqlCreateTable + "," + q + "  double precision";
        }
        SQLTable.createTable(relation_conf.getNameRelation().toLowerCase(), sqlCreateTable, null, true);
        SQLTable.addQueryToTransaction("CREATE TRIGGER " + relation_conf.getNameRelation().toLowerCase() + "_secclass \n" + "BEFORE INSERT ON " + relation_conf.getNameRelation().toLowerCase() + " FOR EACH ROW \n" + "BEGIN \n" + "  UPDATE  " + relation_conf.getNameRelation() + "\n" + "  SET  secondaryclass = ((new.oidsecondary>>32) & 65535), primaryclass = ((new.oidprimary>>32) & 65535);\n" + "END");
    }

    @Override
    public void suspendRelationTriggger(String relationName) throws AbortException {
        SQLTable.addQueryToTransaction("DROP TRIGGER  IF EXISTS " + relationName + "_secclass");
    }

    @Override
    public void setClassColumns(String relationName) throws AbortException {
        SQLTable.addQueryToTransaction("UPDATE " + relationName + " SET primaryclass   = ((oidprimary>>32)  & 65535);");
        SQLTable.addQueryToTransaction("UPDATE " + relationName + " SET secondaryclass = ((oidsecondary>>32) & 65535);");
        SQLTable.addQueryToTransaction("CREATE TRIGGER " + relationName + "_secclass \n" + "BEFORE INSERT ON " + relationName + " FOR EACH ROW \n" + "BEGIN \n" + "  UPDATE  " + relationName + "\n" + "  SET  secondaryclass = ((new.oidsecondary>>32) & 65535), primaryclass = ((new.oidprimary>>32) & 65535);\n" + "END");
    }

    @Override
    public boolean dbExists(String repository, String dbname) {
        String dfn;
        if (repository == null || dbname.startsWith(Database.getSepar())) {
            dfn = dbname + suffix;
        } else {
            dfn = repository + Database.getSepar() + "embeddeddb" + Database.getSepar() + dbname + suffix;
        }
        return (new File(dfn)).exists();
    }

    @Override
    public void dropDB(String repository, String dbname) {
        if (dbExists(repository, dbname)) {
            String dfn;
            if (repository == null || dbname.startsWith(Database.getSepar())) {
                dfn = dbname + suffix;
            } else {
                dfn = repository + Database.getSepar() + "embeddeddb" + Database.getSepar() + dbname + suffix;
            }
            Messenger.printMsg(Messenger.TRACE, "Drop DB " + dbname);
            (new File(dfn)).delete();
        }
    }

    @Override
    public String dropTable(String table) {
        return "DROP TABLE " + table;
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
    public String getCollectionTableName(String collName, int cat) throws FatalException {
        return collName + "_" + Category.explain(cat);
    }

    @Override
    public String getCreateTempoTable(String tableName, String fmt) throws FatalException {
        return "CREATE TEMPORARY TABLE " + tableName + " " + fmt;
    }

    @Override
    public String getCreateTableFromSelectStatement(String tablename, String select) {
        return "CREATE TABLE " + tablename + " AS " + select + "";
    }

    @Override
    public String getSelectWithExcept(String main_query, String key, String sec_query) throws FatalException {
        return main_query + " " + this.getExceptStatement(key) + " " + sec_query;
    }

    @Override
    public String getDropIndexStatement(String tableName, String indexName) {
        return "DROP INDEX IF EXISTS " + indexName;
    }

    @Override
    public boolean isIndexDroppable(String indexname) {
        if (indexname.startsWith("sqlite_autoindex")) {
            if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "Index " + indexname + " can not be dropped");
            return false;
        }
        return true;
    }

    @Override
    public String getDropTempoTable(String tableName) throws FatalException {
        return "DROP TABLE IF EXISTS " + tableName;
    }

    @Override
    public String getExceptStatement(String key) {
        return " EXCEPT ";
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
                    retour.put(iname.toString(), col);
                }
            }
            resultat.close();
            return retour;
        } catch (Exception e) {
            AbortException.throwNewException(SaadaException.INTERNAL_ERROR, e);
            return null;
        }
    }

    @Override
    public String getGlobalAlias(String alias) {
        return "AS " + alias;
    }

    @Override
    public String getIndexableTextType() {
        return "text";
    }

    @Override
    public String getInsertStatement(String where, String[] fields, String[] values) {
        return "INSERT INTO " + where + " (" + Merger.getMergedArray(fields) + ") VALUES (" + Merger.getMergedArray(values) + ")";
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
    public String getPrimaryClassColumn() {
        return "primaryclass";
    }

    @Override
    public String getPrimaryRelationshipIndex(String relationName) {
        return "CREATE INDEX " + relationName.toLowerCase() + "_primoid_class ON " + relationName + " ( primaryclass )";
    }

    @Override
    protected File getProcBaseRef() throws Exception {
        return null;
    }

    @Override
    public String getRegexpOp() {
        return "REGEXP";
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
    public String getSecondaryClassColumn() {
        return "secondaryclass";
    }

    @Override
    public String getSecondaryRelationshipIndex(String relationName) {
        return "CREATE INDEX " + relationName.toLowerCase() + "_secoid_class ON " + relationName + " ( secondaryclass )";
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
                rsTables.close();
                return dm.getColumns(null, null, tableName, null);
            }
        }
        rsTables.close();
        return null;
    }

    @Override
    public String getTempoTableName(String tableName) throws FatalException {
        return tableName;
    }

    @Override
    public String getTempodbName(String dbname) {
        return dbname;
    }

    @Override
    public String getUpdateWithJoin(String tableToUpdate, String tableToJoin, String joinCriteria, String keyAlias, String[] keys, String[] values, String selectCriteria) {
        return null;
    }

    @Override
    public String grantSelectToPublic(String table) {
        return "";
    }

    @Override
    public String lockTable(String table) {
        return "";
    }

    @Override
    public String lockTables(String[] writeTable, String[] readTable) {
        return "";
    }

    @Override
    public String getInsertAutoincrementStatement() {
        return "null ";
    }

    public String getSerialToken() {
        return "INTEGER";
    }

    @Override
    public String getEscapeQuote(String val) {
        if (val == null) {
            return null;
        }
        return val.replaceAll("'", "''");
    }

    @Override
    public String getAsciiNull() {
        return "null";
    }

    @Override
    public void storeTable(String tableName, int ncols, String tableFile) throws Exception {
        if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "Loading ASCII  file " + tableFile + " in table " + tableName);
        storeTable(Database.get_connection(), tableName, ncols, tableFile);
    }

    @Override
    public String[] getStoreTable(String table_name, int ncols, String table_file) throws Exception {
        return null;
    }

    @Override
    public boolean tsvLoadNotSupported() {
        return true;
    }

    /**
	 * @param connection
	 * @param tableName
	 * @param tableFile
	 * @throws Exception
	 */
    public void storeTable(Connection connection, String tableName, int ncols, String tableFile) throws Exception {
        int nb_col = 0;
        if (ncols == -1) {
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet rsColumns = meta.getColumns(null, null, tableName.toLowerCase(), null);
            while (rsColumns.next()) nb_col++;
            rsColumns.close();
        } else {
            nb_col = ncols;
        }
        String ps = "insert into " + tableName.toLowerCase() + "  values (";
        for (int i = 0; i < nb_col; i++) {
            if (i > 0) ps += ",";
            ps += "?";
        }
        ps += ")";
        PreparedStatement prep = connection.prepareStatement(ps);
        BufferedReader br = new BufferedReader(new FileReader(tableFile));
        String str = "";
        int line = 0;
        while ((str = br.readLine()) != null) {
            line++;
            String fs[] = str.split("\\t");
            if (fs.length != nb_col) {
                QueryException.throwNewException(SaadaException.FILE_FORMAT, "Error at line " + line + " number of values (" + fs.length + ") does not match the number of columns (" + nb_col + ")");
            }
            for (int i = 0; i < nb_col; i++) {
                if ("null".equals(fs[i])) prep.setObject(i + 1, null); else prep.setObject(i + 1, fs[i]);
            }
            prep.addBatch();
            if ((line % 5000) == 0) {
                if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, "Store 5000 lines into the DB");
                prep.executeBatch();
            }
        }
        if (Messenger.debug_mode) Messenger.printMsg(Messenger.DEBUG, line + " lines stored");
        prep.executeBatch();
        prep.close();
        (new File(tableFile)).delete();
    }

    @Override
    public boolean tableExist(String searched_table) throws Exception {
        DatabaseMetaData dm = Database.get_connection().getMetaData();
        ResultSet rsTables = dm.getTables(null, null, searched_table.toLowerCase(), null);
        while (rsTables.next()) {
            String tableName = rsTables.getString("TABLE_NAME");
            if (searched_table.equalsIgnoreCase(tableName.toLowerCase())) {
                rsTables.close();
                return true;
            }
        }
        rsTables.close();
        return false;
    }

    /**
	 * @param table table to be populated
	 * @param file   datafile (TSV)
	 * @param db_file database file
	 * @return
	 * @throws Exception
	 */
    public static int importTSV(String table, String file, String db_file) throws Exception {
        return importASCIIFile(table, file, "\t", db_file);
    }

    /**
	 * @param table table to be populated
	 * @param file datafile (TSV)
	 * @param separ field separator (one char string)
	 * @param db_file database file
	 * @return
	 * @throws Exception
	 */
    static native int importASCIIFile(String table, String file, String separ, String db_file) throws Exception;

    public boolean supportAccount() {
        return false;
    }

    public void setFetchSize(Statement stmt, int size) throws SQLException {
    }

    public static void main(String[] args) {
        try {
            Database.init("Napoli");
            SQLQuery sq = new SQLQuery();
            sq.run("SELECT oidsaada FROM AIPWFI_IMAGE WHERE (isinbox(15.927, -72.016417, 0.5, 0.5, AIPWFI_IMAGE.pos_ra_csa, AIPWFI_IMAGE.pos_dec_csa) ) limit 100");
        } catch (Exception e) {
            Messenger.printStackTrace(e);
            System.err.println(e.getMessage());
        }
    }
}
