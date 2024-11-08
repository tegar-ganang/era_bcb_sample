package com.felees.hbnpojogen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.jvnet.inflector.Noun;
import com.felees.hbnpojogen.db.FieldObj;

/** 
 * A variety of Helper methods
 * @author wallacew
 *
 */
public class SyncUtils {

    /** Constant */
    private static final String LIB = "lib";

    /** Constant */
    private static final String LIBRARIES = "LIBRARIES";

    /** Constant */
    private static final String TOPLEVEL = "toplevel";

    /** Constant */
    private static final String PROJECTNAME = "PROJECTNAME";

    /** Constant */
    private static final String FKCOLUMN_NAME = "FKCOLUMN_NAME";

    /** Constant */
    private static final String TABLE_CAT = "TABLE_CAT";

    /** Internal constant */
    private static final String JDBC_TABLE = "TABLE";

    /** Internal constant */
    private static final String JDBC_PKTABLE_NAME = "PKTABLE_NAME";

    /** Internal constant */
    private static final String JDBC_PKTABLE_CAT = "PKTABLE_CAT";

    /** Internal constant */
    private static final String JDBC_TABLE_NAME = "TABLE_NAME";

    /** For increased speed */
    private static ConcurrentHashMap<String, CommitResults> cache = new ConcurrentHashMap<String, CommitResults>();

    /** Stupid MySql workaround (mysql is inconsistent in case handling)
     * @param dbmd connection
     * @param cat catalog
     * @param tableName tableName
     * @return the proper case of the table
     * @throws SQLException
     */
    private static String getTableNameInProperCase(final java.sql.DatabaseMetaData dbmd, String cat, String tableName) throws SQLException {
        ResultSet rs2 = dbmd.getTables(cat, null, tableName, new String[] { JDBC_TABLE });
        String result = "";
        if (rs2.next()) {
            result = rs2.getString(TABLE_CAT) + "." + rs2.getString(JDBC_TABLE_NAME);
        }
        rs2.close();
        return result;
    }

    /** Convenience method
     * @param dbmd
     * @param dbName
     * @return getTableNameInProperCase
     * @throws SQLException
     */
    private static String getTableNameInProperCase(final java.sql.DatabaseMetaData dbmd, String dbName) throws SQLException {
        return getTableNameInProperCase(dbmd, HbnPojoGen.getTableCatalog(dbName), HbnPojoGen.getTableName(dbName));
    }

    /** Parses a DB and returns a commit/delete order. 
     * @param dbmd
     * @param dbCatalogs
     * @param singleSchema
     * @param greedy
     * @return CommitResults
     * @throws SQLException
     */
    public static CommitResults getCommitOrder(final java.sql.DatabaseMetaData dbmd, final TreeSet<String> dbCatalogs, final boolean singleSchema, final boolean greedy) throws SQLException {
        CommitResults result = null;
        String checkTable;
        int tblCount = 0;
        if (singleSchema) {
            result = cache.get(dbCatalogs.iterator().next());
            if (result != null) {
                return result;
            }
        }
        result = new CommitResults();
        LinkedList<String> tables = new LinkedList<String>();
        TreeSet<String> parsedTables = new TreeSet<String>(new CaseInsensitiveComparator());
        TreeSet<String> tableDependencies = null;
        TreeMap<String, Boolean> tableDepsWithPossibleCycles = null;
        TreeSet<String> catalogs = new TreeSet<String>(new CaseInsensitiveComparator());
        catalogs.addAll(dbCatalogs);
        for (String dbCat : dbCatalogs) {
            ResultSet resultSet = dbmd.getTables(dbCat, null, "%", new String[] { JDBC_TABLE });
            while (resultSet.next()) {
                tables.add(resultSet.getString(TABLE_CAT) + "." + resultSet.getString(JDBC_TABLE_NAME));
            }
        }
        while (tables.size() > 0) {
            String tmp = tables.get(tblCount);
            String dbCatalog = tmp.substring(0, tmp.indexOf("."));
            checkTable = tmp.substring(tmp.indexOf(".") + 1);
            String checkTableFull = dbCatalog + "." + checkTable;
            tableDependencies = result.getTableDeps().get(checkTableFull);
            if (tableDependencies == null) {
                tableDependencies = new TreeSet<String>(new CaseInsensitiveComparator());
                tableDepsWithPossibleCycles = new TreeMap<String, Boolean>(new CaseInsensitiveComparator());
                result.getTableDeps().put(checkTableFull, tableDependencies);
                result.getTableDepsWithPossibleCycles().put(checkTableFull, tableDepsWithPossibleCycles);
                ResultSet importedKeys = dbmd.getImportedKeys(dbCatalog, null, checkTable);
                while (importedKeys.next()) {
                    if (singleSchema && (!importedKeys.getString(JDBC_PKTABLE_CAT).equals(dbCatalog))) {
                        continue;
                    }
                    String newDep = importedKeys.getString(JDBC_PKTABLE_CAT) + "." + importedKeys.getString(JDBC_PKTABLE_NAME);
                    String cat = newDep.substring(0, newDep.indexOf("."));
                    if (!singleSchema && !catalogs.contains(cat)) {
                        if (greedy) {
                            ResultSet rs2 = dbmd.getTables(cat, null, "%", new String[] { JDBC_TABLE });
                            while (rs2.next()) {
                                tables.add(rs2.getString(TABLE_CAT) + "." + rs2.getString(JDBC_TABLE_NAME));
                            }
                            catalogs.add(cat);
                        }
                    }
                    ResultSet fieldNames = dbmd.getColumns(dbCatalog, null, checkTable, importedKeys.getString(FKCOLUMN_NAME));
                    if (fieldNames.next()) {
                        if (!result.getTableDeps().get(checkTableFull).contains(newDep) && (!newDep.equalsIgnoreCase(checkTableFull.toUpperCase()))) {
                            if (!greedy) {
                                ResultSet resultSet = dbmd.getTables(HbnPojoGen.getTableCatalog(newDep), null, HbnPojoGen.getTableName(newDep), new String[] { JDBC_TABLE });
                                if (resultSet.next()) {
                                    String match = resultSet.getString(TABLE_CAT) + "." + resultSet.getString(JDBC_TABLE_NAME);
                                    boolean matched = false;
                                    for (String s : tables) {
                                        if (s.equalsIgnoreCase(match)) {
                                            matched = true;
                                        }
                                    }
                                    if (!matched) {
                                        tables.add(match);
                                    }
                                }
                                resultSet.close();
                            }
                            if (fieldNames.getString("IS_NULLABLE").equalsIgnoreCase("YES")) {
                                tableDepsWithPossibleCycles.put(getTableNameInProperCase(dbmd, newDep), true);
                                continue;
                            }
                            tableDepsWithPossibleCycles.put(getTableNameInProperCase(dbmd, newDep), false);
                            tableDependencies.add(newDep);
                        }
                    }
                }
            }
            if ((tableDependencies.size() == 0) || parsedTables.containsAll(tableDependencies)) {
                parsedTables.add(checkTableFull);
                boolean matched = false;
                for (String s : result.getCommitList()) {
                    if (s.equalsIgnoreCase(checkTableFull)) {
                        matched = true;
                    }
                }
                if (!matched) {
                    result.getCommitList().add(getTableNameInProperCase(dbmd, checkTableFull));
                }
                tables.remove(tblCount);
            } else {
                tblCount++;
            }
            if (tblCount > tables.size() - 1) {
                tblCount = 0;
            }
        }
        if (singleSchema) {
            cache.put(dbCatalogs.iterator().next(), result);
        }
        return result;
    }

    /** Return commit order of given database 
     * @param connection
     * @param singleSchema 
     * @param greedy 
     * @return a linked list in the right commit order
     * @throws SQLException
     */
    public static CommitResults getCommitOrder(Connection connection, boolean singleSchema, boolean greedy) throws SQLException {
        String dbCatalog = connection.getCatalog();
        DatabaseMetaData dbmd = connection.getMetaData();
        TreeSet<String> catalogs = new TreeSet<String>();
        catalogs.add(dbCatalog);
        return getCommitOrder(dbmd, catalogs, singleSchema, greedy);
    }

    /** Fetches all catalogs
     * @param connection
     * @return CommitResults
     * @throws SQLException
     */
    public static CommitResults getCompleteCommitOrder(Connection connection) throws SQLException {
        DatabaseMetaData dbmd = connection.getMetaData();
        ResultSet resultSet = dbmd.getCatalogs();
        TreeSet<String> catalogs = new TreeSet<String>();
        while (resultSet.next()) {
            String cat = resultSet.getString(TABLE_CAT);
            if (!cat.equalsIgnoreCase("mysql")) {
                catalogs.add(cat);
            }
        }
        resultSet.close();
        return getCommitOrder(dbmd, catalogs, false, true);
    }

    /** 
     * Given an enum, it returns all possible values of it. wasScrubbed will be true if enums didn't match completely 
     * @param conn 
     * @param tblName 
     * @param fieldName 
     * @param wasScrubbed 
     * @return An arraylist of enum values
     * @throws SQLException 
     **/
    public static String[] getEnumValues(final Connection conn, final String tblName, final String fieldName, Boolean[] wasScrubbed) throws SQLException {
        Statement stat = null;
        ResultSet rs = null;
        TreeSet<String> enumset = new TreeSet<String>();
        try {
            stat = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            rs = stat.executeQuery(String.format("SHOW COLUMNS FROM %s LIKE '%s'", tblName, fieldName));
            rs.next();
            String enumTypes = rs.getString("type");
            enumTypes = enumTypes.substring(enumTypes.indexOf("(") + 1, enumTypes.length() - 1);
            String[] result = enumTypes.replaceAll("'", "").split(",");
            wasScrubbed[0] = false;
            for (int i = 0; i < result.length; i++) {
                String res = cleanEnum(result[i], enumset, i, wasScrubbed);
                result[i] = res;
            }
            return result;
        } finally {
            if (stat != null) {
                stat.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Scrub the enums for those characters which cannot be allowed in the java world (eg "+", etc)
     * @param enumText
     * @param enumset
     * @param count 
     * @param wasScrubbed 
     * @return A scrubbed enum
     */
    private static String cleanEnum(final String enumText, final TreeSet<String> enumset, final int count, Boolean[] wasScrubbed) {
        String txt = "(\"" + enumText + "\")";
        String tmp = "";
        for (int i = 0; i < enumText.length(); i++) {
            String c = enumText.substring(i, i + 1);
            if (c.matches("\\w")) {
                tmp += c;
            } else {
                wasScrubbed[0] = true;
            }
        }
        if ((tmp.length() == 0) || tmp.substring(0, 1).matches("\\d")) {
            wasScrubbed[0] = true;
            tmp = "ENUM" + tmp;
        }
        if (enumset.contains(tmp)) {
            tmp = tmp + count;
            wasScrubbed[0] = true;
        } else {
            enumset.add(tmp);
        }
        return tmp + txt;
    }

    /**
     * Map the types from the DB world to the Java environment. We upscale some types since
     * we do not have unsigned types in java :-(
     * @param fieldObj 
     * @return Java type
     */
    public static String mapSQLType(FieldObj fieldObj) {
        String result = "";
        switch(fieldObj.getFieldType()) {
            case java.sql.Types.BOOLEAN:
            case java.sql.Types.BIT:
                return "Boolean";
            case java.sql.Types.TINYINT:
                return "Byte";
            case java.sql.Types.SMALLINT:
                return "Integer";
            case java.sql.Types.INTEGER:
                if (!fieldObj.isFieldTypeUnsigned()) {
                    result = "Integer";
                } else {
                    result = "Long";
                }
                break;
            case java.sql.Types.BIGINT:
                result = "Long";
                break;
            case java.sql.Types.FLOAT:
            case java.sql.Types.REAL:
            case java.sql.Types.DOUBLE:
            case java.sql.Types.NUMERIC:
                result = "Double";
                break;
            case java.sql.Types.DECIMAL:
                result = "java.math.BigDecimal";
                break;
            case java.sql.Types.CHAR:
            case java.sql.Types.VARCHAR:
            case java.sql.Types.LONGVARCHAR:
            case java.sql.Types.NCHAR:
            case java.sql.Types.NVARCHAR:
            case java.sql.Types.LONGNVARCHAR:
                result = "String";
                break;
            case java.sql.Types.BINARY:
            case java.sql.Types.VARBINARY:
            case java.sql.Types.LONGVARBINARY:
                result = "Byte[]";
                break;
            case java.sql.Types.DATE:
            case java.sql.Types.TIME:
            case java.sql.Types.TIMESTAMP:
                result = "Date";
                break;
            case java.sql.Types.ROWID:
            case java.sql.Types.NCLOB:
            case java.sql.Types.SQLXML:
            case java.sql.Types.NULL:
            case java.sql.Types.OTHER:
            case java.sql.Types.JAVA_OBJECT:
            case java.sql.Types.DISTINCT:
            case java.sql.Types.STRUCT:
            case java.sql.Types.ARRAY:
            case java.sql.Types.BLOB:
            case java.sql.Types.CLOB:
            case java.sql.Types.REF:
            case java.sql.Types.DATALINK:
                result = "Object";
                break;
            default:
                result = "Object";
                break;
        }
        return result;
    }

    /** Convenience function
     * @param s
     * @return the same string with the first character set to uppercase
     */
    public static String upfirstChar(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1, s.length());
    }

    /** Copies src file to dst file. If the dst file does not exist, it is created. Uses NIO for speed
     * @param src
     * @param dst
     * @throws Exception 
     * @throws ParseErrorException 
     * @throws ResourceNotFoundException 
     */
    public static void copyFile(File src, File dst) throws ResourceNotFoundException, ParseErrorException, Exception {
        if (src.getAbsolutePath().endsWith(".vm")) {
            copyVMFile(src, dst.getAbsolutePath().substring(0, dst.getAbsolutePath().lastIndexOf(".vm")));
        } else {
            FileInputStream fIn;
            FileOutputStream fOut;
            FileChannel fIChan, fOChan;
            long fSize;
            MappedByteBuffer mBuf;
            fIn = new FileInputStream(src);
            fOut = new FileOutputStream(dst);
            fIChan = fIn.getChannel();
            fOChan = fOut.getChannel();
            fSize = fIChan.size();
            mBuf = fIChan.map(FileChannel.MapMode.READ_ONLY, 0, fSize);
            fOChan.write(mBuf);
            fIChan.close();
            fIn.close();
            fOChan.close();
            fOut.close();
        }
    }

    /** Copies a file, applying velocity transformations if necessary
     * @param src
     * @param dst
     * @throws ResourceNotFoundException
     * @throws ParseErrorException
     * @throws Exception
     */
    public static void copyVMFile(File src, String dst) throws ResourceNotFoundException, ParseErrorException, Exception {
        VelocityEngine ve = new VelocityEngine();
        Properties p = new Properties();
        p.setProperty("file.resource.loader.path", src.getParent());
        p.setProperty("runtime.interpolate.string.literals", "true");
        ve.init(p);
        Template generalTemplate = ve.getTemplate(src.getName());
        VelocityContext context = new VelocityContext();
        context.put(PROJECTNAME, HbnPojoGen.projectName);
        context.put(TOPLEVEL, HbnPojoGen.topLevel);
        context.put(LIB, HbnPojoGen.libPath);
        PrintWriter generalTemplateWriter = new PrintWriter(new BufferedWriter(new FileWriter(dst)));
        generalTemplate.merge(context, generalTemplateWriter);
        generalTemplateWriter.close();
    }

    /** Copies all files under srcDir to dstDir. If dstDir does not exist, it will be created.
     * @param srcDir
     * @param dst
     * @throws Exception 
     * @throws ParseErrorException 
     * @throws ResourceNotFoundException 
     */
    public static void copyDirectory(File srcDir, File dst) throws ResourceNotFoundException, ParseErrorException, Exception {
        File dstDir = dst;
        if (dst.getName().equals(PROJECTNAME)) {
            dstDir = new File(dstDir.getParent() + File.separator + HbnPojoGen.projectName);
        }
        if (dst.getName().equalsIgnoreCase(TOPLEVEL)) {
            dstDir = new File(dstDir.getParent() + File.separator + HbnPojoGen.topLevel.replaceAll("\\.", "/"));
        }
        if (dst.getName().equalsIgnoreCase(LIBRARIES)) {
            dstDir = new File(dstDir.getParent() + File.separator + File.separator + HbnPojoGen.libPath);
        }
        if (srcDir.isDirectory()) {
            if (!dstDir.exists()) {
                dstDir.mkdirs();
            }
            String[] children = srcDir.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(srcDir, children[i]), new File(dstDir, children[i]));
            }
        } else {
            copyFile(srcDir, dstDir);
        }
    }

    /** Create all source folders
     * @param srcFolder
     * @param targetTestFolder
     */
    public static void createDirs(String srcFolder, String targetTestFolder) {
        new File(srcFolder + "/" + HbnPojoGen.projectName + "/model/dao/").mkdirs();
        new File(srcFolder + "/" + HbnPojoGen.projectName + "/enums/db/").mkdirs();
        new File(targetTestFolder + "/" + HbnPojoGen.projectName + "/dao/").mkdirs();
        new File(srcFolder + "/" + HbnPojoGen.projectName + "/model/obj/").mkdirs();
    }

    /** Given an input string, remove underscores and convert to Java conventions
     * @param input
     * @return Java-convention name
     */
    public static String removeUnderscores(String input) {
        StringBuffer result = new StringBuffer();
        if (HbnPojoGen.disableUnderscoreConversion) {
            result.append(input);
        } else {
            String[] tmp = input.split("_");
            for (int i = 0; i < tmp.length; i++) {
                String fragment;
                if (i == 0) {
                    fragment = tmp[i];
                } else {
                    fragment = tmp[i].substring(0, 1).toUpperCase() + tmp[i].substring(1);
                }
                result.append(fragment);
            }
        }
        return result.toString();
    }

    /** Given a word, return the english plural equivalent
     * @param input
     * @return nice english
     */
    public static String pluralize(String input) {
        String result = input;
        if (!HbnPojoGen.disableEnglishPlural) {
            String tmp = Noun.pluralOf(input.substring(0, 1).toLowerCase() + input.substring(1));
            result = input.substring(0, 1) + tmp.substring(1);
        }
        return result;
    }

    /** Results of getCommitOrder
     * @author wallacew
     *
     */
    public static final class CommitResults {

        /** Contains an ordered list as to how to commit records in a DB */
        private LinkedList<String> commitList = new LinkedList<String>();

        /** List of what each table requires as other table dependencies */
        private TreeMap<String, TreeSet<String>> tableDeps = new TreeMap<String, TreeSet<String>>(new CaseInsensitiveComparator());

        /** Like tableDeps, but holding the complete dependency list rather than just those FK marked as NotNull */
        private TreeMap<String, TreeMap<String, Boolean>> tableDepsWithPossibleCycles = new TreeMap<String, TreeMap<String, Boolean>>(new CaseInsensitiveComparator());

        /** the db cycle list */
        private TreeMap<String, LinkedList<String>> cycleList = new TreeMap<String, LinkedList<String>>(new CaseInsensitiveComparator());

        /** Recursive call. Finds cycles in the DB
         * @param start what to match
         * @param test what we're seeing now
         * @param seen what we've seen so far
         * @param result linked list of result
         */
        @SuppressWarnings("all")
        private void checkCycle(String start, String test, TreeSet<String> seen, LinkedList<String> result) {
            if (start.equalsIgnoreCase(test) && (test != null)) {
                result.add(test);
                return;
            }
            if (seen == null) {
                seen = new TreeSet<String>();
            }
            if (test == null) {
                test = start;
            }
            if (seen.contains(test)) {
                return;
            }
            seen.add(test);
            for (String check : this.tableDepsWithPossibleCycles.get(test).keySet()) {
                checkCycle(start, check, seen, result);
                if (!result.isEmpty()) {
                    result.add(test);
                    return;
                }
            }
            return;
        }

        /** 
         * Build a list of all those entries that have a cycle in the db
         */
        public void buildCycleList() {
            for (Entry<String, TreeMap<String, Boolean>> entry : this.tableDepsWithPossibleCycles.entrySet()) {
                LinkedList<String> result = new LinkedList<String>();
                checkCycle(entry.getKey(), null, null, result);
                if (!result.isEmpty()) {
                    result.removeLast();
                    this.cycleList.put(entry.getKey(), result);
                }
            }
        }

        /** Return the commit order list
         * @return the commitOrder
         */
        public final LinkedList<String> getCommitList() {
            return this.commitList;
        }

        /** Return the table dependencies
         * @return the tableDeps
         */
        public final TreeMap<String, TreeSet<String>> getTableDeps() {
            return this.tableDeps;
        }

        @Override
        public String toString() {
            return this.commitList.toString();
        }

        /** Return a list of dependencies including ones with cycles in the DB
         * @return the tableDepsWithPossibleCycles
         */
        public final TreeMap<String, TreeMap<String, Boolean>> getTableDepsWithPossibleCycles() {
            return this.tableDepsWithPossibleCycles;
        }

        /** Get a list of all tables that loop around
         * @return the cycleList
         */
        public final TreeMap<String, LinkedList<String>> getCycleList() {
            return this.cycleList;
        }
    }
}
