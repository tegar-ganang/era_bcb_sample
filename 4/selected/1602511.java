package com.jipes.cm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.jipes.cm.domain.DDL;
import com.jipes.cm.domain.DatabaseObject;
import com.jipes.cm.domain.DatabaseObjectType;
import com.jipes.cm.domain.DependentObject;
import com.jipes.cm.domain.DependentObjectType;
import com.jipes.cm.util.ConfigUtil;
import com.jipes.cm.util.ConnectionUtil;

/**
 * This is the main utility called for build and export.  It can be executed
 * as is with the following command:<BR>
 * java -jar jipes-cm.jar [args] ...<BR><BR>
 * 
 * The valid arguments are as follows:<BR>
 * <b>-m|-mode:</b><BR>&nbsp;
 *      Builder mode, being build (default), export, or both.<BR>
 * <b>-s|-source:</b><BR>&nbsp;
 *      Source database URL for export mode using self contained JDBC driver <BR>&nbsp;&nbsp;&nbsp;
 *          <strong>(no TNSNAMES or Oracle client required):</strong><BR>&nbsp;&nbsp;
 *        jdbc:oracle:thin:[&lt;user&gt;[/&lt;password&gt;]]@&lt;DB DNS or IP&gt;:&lt;port&gt;:&lt;SID&gt;<BR>&nbsp;
 *      Source database URL for export mode with native client required:<BR>&nbsp;&nbsp;
 *        jdbc:oracle:oci:[&lt;user&gt;[/&lt;password&gt;]]@&lt;TNS SID&gt;<BR>
 * <b>-sU|-sourceUser:</b><BR>&nbsp;
 *      The user for the source database.<BR>	
 * <b>-sP|-sourcePass:</b><BR>&nbsp;
 *      The password for the source database.<BR>	
 * <b>-t|-target:</b><BR>&nbsp;
 *      Target database URL for build:<BR>&nbsp;&nbsp;	
 *        same format as -source database URL.<BR>
 * <b>-tU|-targetUser:</b><BR>&nbsp;
 *      The user for the target database.<BR>	
 * <b>-tP|-targetPass:</b><BR>&nbsp;
 *      The password for the target database.<BR>	
 * <b>-d|-directory:</b><BR>&nbsp;
 *      Directory used to read and write DDL, default is the current directory<BR>
 * <b>-p|-properties:</b><BR>&nbsp;
 *      Parameter file used to set any of these options<BR>
 * <b>-schema:</b><BR>&nbsp;
 *      A regular expression filtering the source or target schemas.<BR>&nbsp;&nbsp;
 *        FOO|BAR       = FOO or BAR schema<BR>&nbsp;&nbsp;
 *        .*BAR         = schemas ending in BAR<BR>&nbsp;&nbsp;
 *        FOO.*         = schemas starting with FOO<BR>&nbsp;&nbsp;
 *        .*BAR.*|FOO.* = schemas containing BAR or starting with FOO<BR>		
 * <b>-schemaMap:</b><BR>&nbsp;
 *      The schema to map the selected objects to for export and/or build.<BR>&nbsp;
 *      Not compatible with -mode both option, so please export and build separately.<BR>
 * <b>-object:</b><BR>&nbsp;
 *      A regular expression filtering the source or target objects.<BR>		
 * <b>-parallel:</b><BR>&nbsp;
 *      Number of parallel sessions to use.<BR>		
 * <b>-parallelDDL:</b><BR>&nbsp;
 *      Number of parallel workers to use for DDL within each session.<BR>    
 * <b>-h|-help:</b><BR>&nbsp;
 *      Prints usage information<BR>
 *      <BR>
 * More regular expression 
 * <a href="http://www.regular-expressions.info/examplesprogrammer.html">examples</a>...<BR><BR>
 * Any of these options can also appear in a property file passed by
 * using the -p or -properties parameter or in the operating system environment
 * variables using the following keys:<BR>
 * <TABLE border="5">
 * <TR><TD><b>Property</b></TD><TD><b>OS Environment</b></TD>    <TD><b>Command Line Argument</b></TD></TR>
 * <TR><TD>MODE</TD> <TD>JIPES_CM_MODE</TD>           <TD>"-m|-mode"</TD></TR>
 * <TR><TD>SOURCE_DB_URL</TD><TD>JIPES_CM_SOURCE_DB_URL</TD>  <TD>"-s|-source"</TD></TR>
 * <TR><TD>SOURCE_DB_USER</TD><TD>JIPES_CM_SOURCE_DB_USER</TD> <TD>"-sU|-sourceUser"</TD></TR>
 * <TR><TD>SOURCE_DB_PASS</TD><TD>JIPES_CM_SOURCE_DB_PASS</TD> <TD>"-sP|-sourcePass"</TD></TR>
 * <TR><TD>TARGET_DB_URL</TD><TD>JIPES_CM_TARGET_DB_URL</TD>  <TD>"-t|-target"</TD></TR>
 * <TR><TD>TARGET_DB_USER</TD><TD>JIPES_CM_TARGET_DB_USER</TD> <TD>"-tU|-targetUser"</TD></TR>
 * <TR><TD>TARGET_DB_PASS</TD><TD>JIPES_CM_TARGET_DB_PASS</TD> <TD>"-tP|-targetPass"</TD></TR>
 * <TR><TD>BUILD_DIR</TD><TD>JIPES_CM_BUILD_DIR</TD>       <TD>"-d|-directory"</TD></TR>
 * <TR><TD>PROPERTIES</TD><TD>JIPES_CM_PROPERTIES</TD>     <TD>"-p|-properties"</TD></TR>
 * <TR><TD>SCHEMA_FILTER</TD><TD>JIPES_CM_SCHEMA_FILTER</TD>  <TD>"-schema"</TD></TR>
 * <TR><TD>SCHEMA_MAP</TD><TD>JIPES_CM_SCHEMA_MAP</TD>     <TD>"-schemaMap"</TD></TR>
 * <TR><TD>OBJECT_FILTER</TD><TD>JIPES_CM_OBJECT_FILTER</TD>  <TD>"-object"</TD></TR>
 * <TR><TD>PARALLEL</TD><TD>JIPES_CM_PARALLEL</TD>       <TD>"-parallel"</TD></TR>
 * <TR><TD>PARALLEL_DDL</TD><TD>JIPES_CM_PARALLEL_DDL</TD>       <TD>"-parallelDDL"</TD></TR> * 
 * <TR><TD>DB_PREFETCH</TD><TD>JIPES_CM_DB_PREFETCH</TD>    <TD>None</TD></TR>
 * <TR><TD>DB_UPDATE_BATCHING</TD><TD>JIPES_CM_DB_UPDATE_BATCHING</TD> <TD>None</TD></TR>
 * </TABLE>
 * <BR><BR>
 * 
 * Property file keys are equal to the values of the enumeration {@link BuildParm}.<BR><BR>
 * 
 * The properties and parameters are handled in the following order, where
 * later values replace previous values:<BR>
 * <b>1.</b> Properties from configDefaults.properties on the Java classpath<BR>
 * <b>2.</b> Operating System environment variables<BR>
 * <b>3.</b> Java Virtual Machine properties (java -d environment values)<BR>
 * <b>4.</b> Properties from the file specified in command line argument -p|-properties<BR>
 * <b>5.</b> Properties specified in the command line parameters<BR>
 * 
 * @author Matt Pouttu-Clarke
 * @version     %I%, %G%
 *
 */
public class Builder implements Runnable {

    private static final Log LOG = LogFactory.getLog(Builder.class);

    public Builder() {
        super();
    }

    private static class ExportThread extends Thread {

        String path;

        List<DatabaseObject> objects;

        int threads;

        int thread;

        public ExportThread(String path, List<DatabaseObject> objects, int threads, int thread) {
            super("ExportThread" + thread);
            this.path = path;
            this.objects = objects;
            this.threads = threads;
            this.thread = thread;
        }

        @Override
        public void run() {
            try {
                LOG.trace("Running: " + getName());
                Builder bdr = new Builder();
                Connection conn = ConnectionUtil.getSourceDbConnection();
                bdr.exportParallel(path, conn, objects, threads, thread);
                conn.close();
                LOG.trace("Done: " + getName());
            } catch (Throwable t) {
                LOG.fatal("Export thread " + getName() + " failed", t);
            }
        }
    }

    private static class BuildThread extends Thread {

        String path;

        List<DatabaseObject> objects;

        int threads;

        int thread;

        boolean postProcess;

        public BuildThread(String path, List<DatabaseObject> objects, int threads, int thread, boolean postProcess) {
            super("BuildThread" + thread);
            this.path = path;
            this.objects = objects;
            this.threads = threads;
            this.thread = thread;
            this.postProcess = postProcess;
        }

        @Override
        public void run() {
            try {
                LOG.trace("Running: " + getName());
                Builder bdr = new Builder();
                Connection conn = ConnectionUtil.getTargetDbConnection();
                bdr.buildParallel(conn, path, objects, threads, thread, postProcess);
                conn.close();
                LOG.trace("Done: " + getName());
            } catch (Throwable t) {
                LOG.fatal("Builder thread " + getName() + " failed", t);
            }
        }
    }

    /**
	 * Removes carriage return characters, which Oracle JDBC clients do not 
	 * like.
	 * @param sql
	 * @return String
	 */
    public String removeBadChars(String sql) {
        return sql.replace('\r', ' ');
    }

    /**
	 * Gets a SQL String without carriage return characters.  The sqlPath
	 * is looked up on the class path using the default class loader.
	 * By default, these SQLs are located in the sql/metadata path within
	 * the build.jar file.
	 * @param sqlPath
	 * @return SQL at path
	 */
    public String getSQL(String sqlPath) {
        return removeBadChars(ConfigUtil.getInstance().getResourceAsString(sqlPath));
    }

    /**
	 * Executes a DDL on the given Connection.  If an error was ignored, the
	 * message is returned.
	 * @param conn
	 * @param sql
	 * @return Error message string
	 */
    public String executeDDL(Connection conn, String sql) {
        if (LOG.isTraceEnabled()) LOG.trace("Executing DDL: " + sql);
        Statement s = null;
        try {
            s = conn.createStatement();
            s.executeUpdate(sql);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1917 || e.getErrorCode() == 1442) {
                if (LOG.isDebugEnabled()) LOG.debug("Error executing DDL: " + sql + "\n\tmessage: " + e.getMessage(), e);
                if (e.getMessage() != null) {
                    return e.getMessage();
                } else {
                    return "SQLException with no message";
                }
            }
            LOG.error("Error executing DDL: " + sql + "\n\tmessage: " + e.getMessage(), e);
            throw new RuntimeException("Error executing DDL: " + sql + "\n\tmessage: " + e.getMessage(), e);
        } finally {
            try {
                s.close();
            } catch (Throwable t) {
            }
        }
        if (LOG.isTraceEnabled()) LOG.trace("Done executing DDL");
        return null;
    }

    /**
	 * Gets the contents of a file as a String.
	 * @param inFile
	 * @return file contents
	 */
    public String getFileContents(File inFile) {
        if (LOG.isTraceEnabled()) LOG.trace("Getting file contents for: " + inFile.getAbsolutePath());
        try {
            if (inFile.length() > Integer.MAX_VALUE) {
                throw new RuntimeException("Maximum supported file length (bytes) is " + Integer.MAX_VALUE);
            }
            if (inFile.length() < 1L) {
                LOG.warn("Ignoring empty file at " + inFile.getAbsolutePath());
                return "";
            }
            int length = (int) inFile.length();
            FileReader in = new FileReader(inFile.getAbsolutePath());
            CharBuffer buff = CharBuffer.allocate(length);
            in.read(buff);
            buff.rewind();
            String sql = removeBadChars(buff.toString());
            if (sql == null) {
                sql = "";
            }
            if (LOG.isTraceEnabled()) LOG.trace("Got file contents: " + sql);
            in.close();
            return sql;
        } catch (Exception e) {
            LOG.fatal("Error getting file contents", e);
            throw new RuntimeException("Error getting file contents", e);
        }
    }

    /**
	 * Transforms the schema of an incoming DDL script or any other String. 
	 * @param fromSchema
	 * @param in
	 * @return transformed schema string
	 */
    public String transformSchema(String fromSchema, String in) {
        if (LOG.isDebugEnabled()) LOG.debug("fromSchema=" + fromSchema + ", in=" + in);
        String out = "";
        ConfigUtil config = ConfigUtil.getInstance();
        if (config.hasConfig(BuildParm.SCHEMA_MAP)) {
            String toSchema = config.getConfig(BuildParm.SCHEMA_MAP);
            out = RemapIdentifier.remap(fromSchema, toSchema, in);
        } else {
            out = in;
        }
        return out;
    }

    /**
	 * Checks if a set of threads is done processing.
	 * @param threads
	 * @return false if any thread is not done, else return true
	 */
    public boolean checkDone(List<Thread> threads) {
        for (Thread t : threads) {
            if (t.isAlive()) {
                return false;
            }
        }
        return true;
    }

    /**
	 * Takes a database object dependent DDL set delimited by ';' characters
	 * and splits it into individual scripts for execution.
	 * @param o
	 * @param dT
	 * @param sqls
	 */
    public void parseDependentDDL(DatabaseObject o, DependentObjectType dT, String sqls) {
        if (sqls == null) {
            if (LOG.isTraceEnabled()) LOG.trace("No dependents found");
        } else {
            StringWriter newDDL = new StringWriter(1024);
            List<DependentObject> listD = new ArrayList<DependentObject>();
            o.dependents.put(dT, listD);
            String[] split = DependentObjectType.splitDDL(dT, sqls);
            Pattern p = Pattern.compile(".*ORGANIZATION\\s+INDEX.*", Pattern.DOTALL);
            for (String sqlD : split) {
                if (o.type == DatabaseObjectType.TABLE && sqlD.matches(".*PRIMARY\\s+KEY.*") && p.matcher(o.ddl.ddlString).matches()) {
                    continue;
                }
                DependentObject dep = new DependentObject();
                dep.ddl = new DDL(transformSchema(o.schemaName, sqlD));
                listD.add(dep);
                newDDL.append(dep.ddl.ddlString);
                newDDL.append(";\n");
                if (LOG.isTraceEnabled()) LOG.trace("Added dependent " + dT + ", ddlString:\n" + dep.ddl);
            }
            o.dependentDDL.put(dT, new DDL(newDDL.toString()));
            if (LOG.isTraceEnabled()) LOG.trace("New DDL for dependent " + dT + "s, ddlString:\n" + o.dependentDDL.get(dT));
        }
    }

    /**
	 * Load any dependent objects from the specified directory into the 
	 * DatabaseObject passed.
	 * @param dir
	 * @param dbo
	 */
    public void loadDependents(File dir, DatabaseObject dbo) {
        if (!dbo.type.hasDependents) return;
        File subDir = new File(dir.getAbsolutePath() + File.separatorChar + dbo.objectName);
        if (!subDir.exists()) return;
        for (DependentObjectType d : DependentObjectType.values()) {
            File file = new File(subDir.getAbsolutePath() + File.separatorChar + d.fileName + ".sql");
            if (!file.exists()) continue;
            String sqls = getFileContents(file);
            dbo.timestamp = file.lastModified();
            parseDependentDDL(dbo, d, sqls);
        }
    }

    /**
	 * Builds a list of objects to process from a path, and adds each object
	 * to the list passed.
	 * @param file
	 * @param list
	 */
    public void buildObjectListForPath(File file, List<DatabaseObject> list) {
        if (LOG.isTraceEnabled()) LOG.trace("Getting database object list for path " + file.getAbsolutePath());
        try {
            if (!file.isDirectory()) {
                throw new RuntimeException("Input path must be a directory");
            }
            ConfigUtil config = ConfigUtil.getInstance();
            SqlFilenameFilter filter = new SqlFilenameFilter();
            Set<String> set = new TreeSet<String>(Arrays.asList(file.list()));
            for (DatabaseObjectType p : DatabaseObjectType.values()) {
                if (!set.contains(p.directoryName)) {
                    if (LOG.isDebugEnabled()) LOG.debug("Source directory does not contain a " + p + " subdirectory " + p.directoryName);
                    continue;
                }
                File subDir = new File(file.getAbsolutePath() + File.separatorChar + p.directoryName);
                File[] files = subDir.listFiles(filter);
                if (LOG.isTraceEnabled()) LOG.trace("Found " + files.length + " files ending in .sql");
                for (int x = 0; x < files.length; x++) {
                    DatabaseObject dbo = new DatabaseObject();
                    dbo.objectName = files[x].getName().toUpperCase().replace(".SQL", "");
                    if (LOG.isTraceEnabled()) LOG.trace("Got object name from file name " + dbo.objectName);
                    dbo.type = p;
                    dbo.ddl = new DDL(getFileContents(files[x]));
                    dbo.timestamp = files[x].lastModified();
                    DatabaseObject qName = DatabaseObjectType.getQualifiedName(p, dbo.ddl.ddlString);
                    dbo.schemaName = qName.schemaName;
                    if (dbo.objectName.compareToIgnoreCase(qName.objectName) != 0) {
                        throw new RuntimeException("File name and object ddlString name in the file do not match for " + files[x].getAbsolutePath());
                    }
                    if (config.hasConfig(BuildParm.SCHEMA_MAP)) {
                        if (dbo.schemaName.matches(config.getConfig(BuildParm.SCHEMA_FILTER)) && dbo.objectName.matches(config.getConfig(BuildParm.OBJECT_FILTER))) {
                            String toSchema = config.getConfig(BuildParm.SCHEMA_MAP);
                            loadDependents(subDir, dbo);
                            dbo.ddl = new DDL(RemapIdentifier.remap(dbo.schemaName, toSchema, dbo.ddl.ddlString));
                            dbo.schemaName = toSchema;
                            list.add(dbo);
                        }
                    } else if (dbo.schemaName.matches(config.getConfig(BuildParm.SCHEMA_FILTER)) && dbo.objectName.matches(config.getConfig(BuildParm.OBJECT_FILTER))) {
                        loadDependents(subDir, dbo);
                        list.add(dbo);
                    }
                }
            }
        } catch (Exception e) {
            LOG.fatal("Cannot get object list", e);
            throw new RuntimeException("Cannot get object list", e);
        }
        if (LOG.isTraceEnabled()) LOG.trace("Done getting database object list for path");
    }

    /**
	 * Returns an object list from the file system path specified, including 
	 * all DDL located under that path.  Objects selected are filtered by 
	 * the -schema and -object parameters.
	 * @param path
	 * @return list of database objects
	 */
    public List<DatabaseObject> getObjectList(String path) {
        if (LOG.isTraceEnabled()) LOG.trace("Getting database object list for input path " + path);
        try {
            List<DatabaseObject> out = new ArrayList<DatabaseObject>(1024);
            File root = new File(path);
            buildObjectListForPath(root, out);
            return Collections.unmodifiableList(out);
        } catch (Exception e) {
            LOG.fatal("Cannot get object list", e);
            throw new RuntimeException("Cannot get object list", e);
        }
    }

    /**
	 * Gets a list of objects from the specified connection, including all DDL
	 * visible via DBMS_METADATA.  Objects selected are filtered by 
	 * the -schema and -object parameters.
	 * @param conn
	 * @return list of database objects
	 */
    public List<DatabaseObject> getObjectList(Connection conn) {
        if (LOG.isTraceEnabled()) LOG.trace("Getting database object list");
        try {
            ConfigUtil config = ConfigUtil.getInstance();
            List<DatabaseObject> out = new ArrayList<DatabaseObject>(1024);
            for (DatabaseObjectType ot : DatabaseObjectType.values()) {
                String sql = getSQL("sql/metadata/" + ot + "_LIST.sql");
                sql = sql.replaceAll("#excludeSchema#", ExcludeSchema.getExcludeSchemas());
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, "^(" + config.getConfig(BuildParm.SCHEMA_FILTER) + ")$");
                ps.setString(2, "^(" + config.getConfig(BuildParm.OBJECT_FILTER) + ")$");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    DatabaseObject o = new DatabaseObject();
                    o.schemaName = rs.getString("SCHEMA_NAME");
                    o.objectName = rs.getString("OBJECT_NAME");
                    o.type = DatabaseObjectType.valueOfType(rs.getString("OBJECT_TYPE"));
                    o.timestamp = rs.getTimestamp("CHANGE_TS") != null ? rs.getTimestamp("CHANGE_TS").getTime() : 0;
                    out.add(o);
                    if (LOG.isTraceEnabled()) LOG.trace("Adding " + o.type + ' ' + o.schemaName + '.' + o.objectName + " to object list");
                }
                rs.close();
                ps.close();
            }
            return Collections.unmodifiableList(out);
        } catch (Exception e) {
            LOG.fatal("Cannot get object list", e);
            throw new RuntimeException("Cannot get object list", e);
        }
    }

    /**
	 * Gets the DDL of an object from DBMS_METADATA into the specified database
	 * object, and optionally throw an error if the DDL is not found.
	 * @param conn
	 * @param o
	 * @param throwError
	 */
    public void getDDL(Connection conn, DatabaseObject o, boolean throwError) {
        try {
            String sql = getSQL("sql/metadata/" + o.type + "_LIST.sql");
            sql = sql.replaceAll("#excludeSchema#", ExcludeSchema.getExcludeSchemas());
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, "^(" + o.schemaName + ")$");
            ps.setString(2, "^(" + o.objectName + ")$");
            ResultSet rs = ps.executeQuery();
            try {
                if (!rs.next()) {
                    if (throwError) {
                        throw new RuntimeException("No object found on DB " + conn.getMetaData().getURL() + " for " + o.schemaName + "." + o.objectName);
                    } else {
                        o.ddl = new DDL("");
                        return;
                    }
                }
            } finally {
                rs.close();
                ps.close();
            }
            o.ddl = null;
            o.dependents.clear();
            o.dependentDDL.clear();
            if (LOG.isTraceEnabled()) LOG.trace("Getting DDL for " + o.schemaName + '.' + o.objectName);
            CallableStatement call = conn.prepareCall(getSQL("sql/metadata/" + o.type + ".sql"));
            call.setString(1, o.schemaName);
            call.setString(2, o.objectName);
            call.registerOutParameter(3, Types.CLOB);
            call.registerOutParameter(4, Types.TIMESTAMP);
            call.execute();
            sql = call.getString(3);
            o.timestamp = call.getTimestamp(4) != null ? call.getTimestamp(4).getTime() : 0;
            call.close();
            if (sql == null) {
                sql = "";
            }
            o.ddl = new DDL(sql);
            if (o.type.hasDependents) {
                for (DependentObjectType dT : DependentObjectType.values()) {
                    try {
                        CallableStatement callD = conn.prepareCall(getSQL("sql/metadata/dependent/" + dT + ".sql"));
                        callD.setString(1, o.schemaName);
                        callD.setString(2, o.objectName);
                        callD.registerOutParameter(3, Types.CLOB);
                        callD.registerOutParameter(4, Types.TIMESTAMP);
                        callD.execute();
                        String sqls = callD.getString(3);
                        o.timestamp = callD.getTimestamp(4) != null ? callD.getTimestamp(4).getTime() : 0;
                        callD.close();
                        parseDependentDDL(o, dT, sqls);
                    } catch (Throwable t) {
                        String errMesg = "Cannot obtain DEPENDENT " + dT + " DDL for " + o.schemaName + "." + o.objectName;
                        LOG.fatal(errMesg, t);
                        throw new RuntimeException(errMesg, t);
                    }
                }
            }
            DatabaseObject qName = new DatabaseObject();
            if (o.ddl != null && o.ddl.ddlString.trim().length() > 0) {
                o.ddl = new DDL(transformSchema(o.schemaName, o.ddl.ddlString));
                o.schemaName = transformSchema(o.schemaName, o.schemaName);
                qName = DatabaseObjectType.getQualifiedName(o.type, o.ddl.ddlString);
                if (o.schemaName.compareToIgnoreCase(qName.schemaName) != 0) {
                    throw new RuntimeException("Object name does not match object name in the ddlString for " + o.schemaName + '.' + o.objectName + " at " + conn.getMetaData().getURL());
                }
                if (o.objectName.compareToIgnoreCase(qName.objectName) != 0) {
                    throw new RuntimeException("Object name does not match object name in the ddlString for " + o.schemaName + '.' + o.objectName + " at " + conn.getMetaData().getURL());
                }
            }
            if (throwError && (o.ddl == null || o.ddl.ddlString.trim().length() < 1)) {
                throw new RuntimeException("Could not get DDL from DBMS_METADATA for " + o.schemaName + "." + o.objectName + " at " + conn.getMetaData().getURL());
            }
            if (LOG.isTraceEnabled()) LOG.trace("Done, sql is: " + sql);
        } catch (Exception e) {
            String errMesg = "Cannot obtain object DDL for " + o.schemaName + "." + o.objectName;
            LOG.fatal(errMesg, e);
            throw new RuntimeException(errMesg, e);
        }
    }

    /**
	 * Exports all objects visible on the specified connection to the specified
	 * path on the file system.  Objects selected are filtered by 
	 * the -schema and -object parameters.
	 * @param path
	 * @param conn
	 */
    public void export(String path, Connection conn) {
        if (LOG.isInfoEnabled()) LOG.info("Exporting object scripts to path: " + path);
        ConfigUtil config = ConfigUtil.getInstance();
        List<DatabaseObject> objects = getObjectList(conn);
        int threads = config.getParallel();
        List<Thread> threadList = new ArrayList<Thread>(threads);
        for (int x = 1; x < threads; x++) {
            ExportThread t = new ExportThread(path, objects, threads, x);
            t.start();
            threadList.add(t);
        }
        Thread.yield();
        exportParallel(path, conn, objects, threads, 0);
        for (int x = 0; ; x++) {
            if (x > 1000000) {
                throw new RuntimeException("Main thread timed out waiting for completion");
            }
            if (checkDone(threadList)) break;
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                throw new RuntimeException("Main thread interrupted", e);
            }
        }
        if (LOG.isInfoEnabled()) LOG.info(BuildReport.buildReportString(objects));
        if (LOG.isInfoEnabled()) LOG.info("Exporting object scripts: done");
    }

    /**
	 * Allows export in parallel by dividing up the object list among worker
	 * threads.  The number of threads and the current thread number are used
	 * to divide the workload.
	 * @param path
	 * @param conn
	 * @param objects
	 * @param threads
	 * @param thread
	 */
    public void exportParallel(String path, Connection conn, List<DatabaseObject> objects, int threads, int thread) {
        try {
            if (threads < 1) {
                throw new IllegalArgumentException("At least one worker must be used");
            }
            DatabaseObject[] oArr = new DatabaseObject[objects.size()];
            objects.toArray(oArr);
            DatabaseObject o;
            for (int x = 0; x < oArr.length; x++) {
                if (x % threads != thread) continue;
                o = oArr[x];
                String filePath = path + File.separatorChar + o.type.directoryName;
                String fileName = filePath + File.separatorChar + o.objectName + ".sql";
                try {
                    if (LOG.isInfoEnabled()) LOG.trace("Exporting " + o.type + ": " + o.schemaName + "." + o.objectName + "\n\t\twith timestamp " + new Date(o.timestamp));
                    getDDL(conn, o, true);
                    exportDependents(filePath, o);
                    File outDir = new File(filePath);
                    outDir.mkdirs();
                    File outFile = new File(fileName);
                    if (outFile.exists()) {
                        o.status = BuildStatus.UPDATED;
                        DDL ddl = new DDL(getFileContents(outFile));
                        if (ddl.equals(o.ddl)) {
                            LOG.trace("Object is equal by DDL, skipping export " + o.objectName);
                            o.status = BuildStatus.SKIPPED_EQUAL;
                            continue;
                        }
                    } else {
                        o.status = BuildStatus.CREATED;
                    }
                    switch(o.status) {
                        case CREATED:
                        case UPDATED:
                            outFile.createNewFile();
                            FileWriter out = new FileWriter(outFile);
                            out.append(o.ddl.ddlString);
                            out.close();
                    }
                    o.ddl = null;
                    o.dependentDDL.clear();
                    o.dependents.clear();
                } catch (Throwable t) {
                    o.status = BuildStatus.ERROR;
                    o.errorMesg = t.getMessage();
                }
            }
        } catch (Exception e) {
            LOG.fatal("Cannot export object scripts", e);
            throw new RuntimeException("Cannot export object scripts", e);
        }
    }

    /**
	 * Exports the dependents of a given database object to the path specified.
	 * @param path
	 * @param o
	 */
    public void exportDependents(String path, DatabaseObject o) {
        try {
            if (o.dependents.size() > 0) {
                if (LOG.isTraceEnabled()) LOG.trace("Exporting dependents for " + o.objectName);
                String filePath = path + File.separatorChar + o.objectName;
                File outDir = new File(filePath);
                outDir.mkdirs();
                for (DependentObjectType ot : o.dependents.keySet()) {
                    String fileName = filePath + File.separatorChar + ot.fileName + ".sql";
                    BuildStatus bs = BuildStatus.NOT_PROCESSED;
                    try {
                        File outFile = new File(fileName);
                        if (outFile.exists()) {
                            bs = BuildStatus.UPDATED;
                            DDL ddlIn = new DDL(getFileContents(outFile));
                            if (ddlIn.equals(o.dependentDDL.get(ot))) {
                                LOG.trace("Dependents are equal, skipping export of " + ot + "s for " + o.objectName);
                                bs = BuildStatus.SKIPPED_EQUAL;
                                setAllStatus(o.dependents.get(ot), bs, null);
                                continue;
                            }
                        } else {
                            bs = BuildStatus.CREATED;
                        }
                        switch(bs) {
                            case CREATED:
                            case UPDATED:
                                outFile.createNewFile();
                                FileWriter out = new FileWriter(outFile);
                                out.append(o.dependentDDL.get(ot).ddlString);
                                out.close();
                        }
                        setAllStatus(o.dependents.get(ot), bs, null);
                    } catch (Exception e) {
                        LOG.error("Cannot write to file " + fileName, e);
                        setAllStatus(o.dependents.get(ot), BuildStatus.ERROR, e.getMessage());
                    }
                }
            } else {
                if (LOG.isTraceEnabled()) LOG.trace("No dependents to export for object " + o.objectName);
            }
        } catch (Exception e) {
            LOG.fatal("Cannot export object scripts", e);
            throw new RuntimeException("Cannot export object scripts", e);
        }
    }

    /**
	 * Sets the status of a set of dependent objects, since many dependent 
	 * objects may be skipped together if they are all identical.
	 * @param oDC
	 * @param s
	 * @param errorMesg
	 */
    public void setAllStatus(Collection<DependentObject> oDC, BuildStatus s, String errorMesg) {
        for (DependentObject oD : oDC) {
            oD.status = s;
            oD.errorMesg = errorMesg;
        }
    }

    /**
	 * Builds all the scripts at a given location to the specified database
	 * connection.  Scripts selected are filtered by the -schema and -object 
	 * parameters.
	 * @param conn
	 * @param path
	 */
    public void build(Connection conn, String path) {
        if (LOG.isInfoEnabled()) LOG.info("Building from scripts at path: " + path);
        ConfigUtil config = ConfigUtil.getInstance();
        List<DatabaseObject> objects = getObjectList(path);
        int threads = config.getParallel();
        List<Thread> threadList = new ArrayList<Thread>(threads);
        for (int x = 1; x < threads; x++) {
            BuildThread t = new BuildThread(path, objects, threads, x, false);
            t.start();
            threadList.add(t);
        }
        Thread.yield();
        buildParallel(conn, path, objects, threads, 0, false);
        for (int x = 0; ; x++) {
            if (x > 1000000) {
                throw new RuntimeException("Main thread timed out waiting for completion");
            }
            if (checkDone(threadList)) break;
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                throw new RuntimeException("Main thread interrupted", e);
            }
        }
        if (LOG.isInfoEnabled()) LOG.info("Main build: done, starting post processing");
        threadList = new ArrayList<Thread>(threads);
        for (int x = 1; x < threads; x++) {
            BuildThread t = new BuildThread(path, objects, threads, x, true);
            t.start();
            threadList.add(t);
        }
        Thread.yield();
        buildParallel(conn, path, objects, threads, 0, true);
        for (int x = 0; ; x++) {
            if (x > 1000000) {
                throw new RuntimeException("Main thread timed out waiting for completion");
            }
            if (checkDone(threadList)) break;
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                throw new RuntimeException("Main thread interrupted", e);
            }
        }
        if (LOG.isInfoEnabled()) LOG.info(BuildReport.buildReportString(objects));
        if (LOG.isInfoEnabled()) LOG.info("Building from scripts: done");
    }

    /**
	 * Allows build in parallel by dividing up the object list among worker
	 * threads.  The number of threads and the current thread number are used
	 * to divide the workload.  Main build processing or build post processing
	 * are run depending on the value of the postProcess parameter.
	 * @param conn
	 * @param path
	 * @param objects
	 * @param threads
	 * @param thread
	 * @param postProcess
	 */
    public void buildParallel(Connection conn, String path, List<DatabaseObject> objects, int threads, int thread, boolean postProcess) {
        try {
            StringBuilder sb = new StringBuilder(128);
            Formatter fmt = new Formatter(sb);
            DatabaseObject[] oArr = new DatabaseObject[objects.size()];
            objects.toArray(oArr);
            DatabaseObject fileObj;
            for (int x = 0; x < oArr.length; x++) {
                if (x % threads != thread) continue;
                fileObj = oArr[x];
                if (postProcess) {
                    buildDependents(conn, fileObj, fileObj.target, postProcess);
                    continue;
                }
                try {
                    if (LOG.isTraceEnabled()) LOG.trace("Building " + fileObj.type + ": " + fileObj.schemaName + "." + fileObj.objectName + "\n\t\twith timestamp " + new Date(fileObj.timestamp));
                    sb.setLength(0);
                    fileObj.target = new DatabaseObject(fileObj);
                    getDDL(conn, fileObj.target, false);
                    if (fileObj.target.ddl.equals(fileObj.ddl)) {
                        fileObj.status = BuildStatus.SKIPPED_EQUAL;
                        if (LOG.isTraceEnabled()) LOG.trace("Objects are equal by DDL, skipping build of " + fileObj.type + " named: " + fileObj.objectName);
                        buildDependents(conn, fileObj, fileObj.target, postProcess);
                        continue;
                    }
                    if (fileObj.target.ddl.compareString.length() > 0) {
                        fileObj.status = BuildStatus.UPDATED;
                        if (fileObj.type.dropStatement != null) {
                            fmt.format(fileObj.type.dropStatement, fileObj.schemaName, fileObj.objectName);
                            executeDDL(conn, sb.toString());
                        }
                    } else {
                        fileObj.status = BuildStatus.CREATED;
                    }
                    String errorMesg = executeDDL(conn, fileObj.ddl.ddlString);
                    if (errorMesg != null) {
                        fileObj.status = BuildStatus.NOT_PROCESSED;
                        fileObj.errorMesg = errorMesg;
                    }
                    buildDependents(conn, fileObj, fileObj.target, postProcess);
                } catch (Throwable t) {
                    fileObj.status = BuildStatus.ERROR;
                    fileObj.errorMesg = t.getMessage();
                }
            }
        } catch (Exception e) {
            LOG.fatal("Cannot build from scripts", e);
            throw new RuntimeException("Cannot build from scripts", e);
        }
    }

    /**
	 * Builds all the dependent objects of a particular database object to the
	 * specified database connection.  If the dependents are equal between
	 * the object in the database and the object from the file system then 
	 * the dependent objects are skipped.  This method executes the main build
	 * for the object and does not execute any post processing for the dependent
	 * objects if postProcess is false.  If postProcess is true then only post
	 * processing is executed.
	 * @param conn
	 * @param fileObj
	 * @param dbObj
	 * @param postProcess
	 */
    private void buildDependents(Connection conn, DatabaseObject fileObj, DatabaseObject dbObj, boolean postProcess) {
        try {
            if (LOG.isTraceEnabled()) LOG.trace("Building dependents for " + fileObj.objectName);
            for (DependentObjectType oDT : fileObj.dependents.keySet()) {
                if (oDT.postProcess != postProcess) {
                    continue;
                }
                List<DependentObject> oL = fileObj.dependents.get(oDT);
                List<DependentObject> doL = dbObj.dependents.get(oDT);
                for (DependentObject oD : oL) {
                    try {
                        if (doL != null && doL.contains(oD)) {
                            oD.status = BuildStatus.SKIPPED_EQUAL;
                            if (LOG.isTraceEnabled()) LOG.trace("Skipping dependent becuase it is equal for " + oD.ddl);
                            continue;
                        }
                        String errorMesg = executeDDL(conn, oD.ddl.ddlString);
                        if (errorMesg != null) {
                            oD.status = BuildStatus.NOT_PROCESSED;
                            oD.errorMesg = errorMesg;
                        } else {
                            oD.status = BuildStatus.UPDATED;
                        }
                    } catch (Throwable t) {
                        oD.status = BuildStatus.ERROR;
                        oD.errorMesg = t.getMessage();
                        LOG.error("Error updating dependent object", t);
                    }
                }
            }
            if (LOG.isTraceEnabled()) LOG.trace("Done building dependents for " + fileObj.objectName);
        } catch (Exception e) {
            LOG.fatal("Cannot build dependents from scripts", e);
            throw new RuntimeException("Cannot build dependents from scripts", e);
        }
    }

    /**
	 * Prints out the usage information to the passed print stream.
	 */
    public static void printUsage(PrintStream out) {
        out.println("\nUsage:\n");
        out.println("-m|-mode:");
        out.println("     Builder mode, being build (default), export, or both");
        out.println("-s|-source:");
        out.println("     Source database URL for export mode with self contained JDBC driver");
        out.println("         (no TNSNAMES or Oracle client required):");
        out.println("       jdbc:oracle:thin:[<user>[/<password>]]@<DB DNS or IP>:<port>:<SID>");
        out.println("     Source database URL for export mode with native client required");
        out.println("       jdbc:oracle:oci:[<user>[/<password>]]@<TNS SID>");
        out.println("-sU|-sourceUser:");
        out.println("     The user for the source database");
        out.println("-sP|-sourcePass:");
        out.println("     The password for the source database");
        out.println("-t|-target:");
        out.println("     Target database URL for build");
        out.println("       same format as source database URL");
        out.println("-tU|-targetUser:");
        out.println("     The user for the target database");
        out.println("-tP|-targetPass:");
        out.println("     The password for the target database");
        out.println("-t|-target:");
        out.println("     Target database URL for build mode");
        out.println("       same format as -source database URL");
        out.println("-d|-directory:");
        out.println("     Directory used to read and write DDL, default is <user home>/build");
        out.println("-p|-properties:");
        out.println("     Parameter file used to set any of these options");
        out.println("-schema:");
        out.println("     A regular expression filtering the source or target schemas");
        out.println("       FOO|BAR       = FOO or BAR schema");
        out.println("       .*BAR         = schemas ending in BAR");
        out.println("       FOO.*         = schemas starting with FOO");
        out.println("       .*BAR.*|FOO.* = schemas containing BAR or starting with FOO");
        out.println("-schemaMap:");
        out.println("     The schema to map the selected objects to for export and/or build.");
        out.println("     Not compatible with -mode both option, please export and build separately.");
        out.println("-object:");
        out.println("     A regular expression filtering the source or target objects");
        out.println("-parallel:");
        out.println("     Number of parallel sessions to use, default 1");
        out.println("-parallelDDL:");
        out.println("     Number of parallel workers to use for DDL within each session");
        out.println("-h|-help:");
        out.println("     This output");
        out.println("\nMore regular expression examples... ");
        out.println("http://www.regular-expressions.info/examplesprogrammer.html");
    }

    public void run() {
        try {
            if (LOG.isInfoEnabled()) LOG.info("Builder: start");
            ConfigUtil config = ConfigUtil.getInstance();
            config.validate();
            if (LOG.isInfoEnabled()) LOG.info("Builder config is: \n" + config);
            String mode = config.getConfig(BuildParm.MODE);
            if (!mode.matches("both|export|build")) {
                throw new RuntimeException("Invalid mode: " + mode);
            }
            if (mode.matches("both|export")) {
                if (config.getConfig(BuildParm.SOURCE_DB_PASS) == null) {
                    System.out.print("Enter source DB password: ");
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                        config.setConfig(BuildParm.SOURCE_DB_PASS, in.readLine());
                    } catch (Throwable t) {
                        throw new RuntimeException("Read of source DB password failed", t);
                    }
                }
                Connection sConn = ConnectionUtil.getSourceDbConnection();
                export(config.getBuildHomeDir(), sConn);
                sConn.close();
            }
            if (mode.matches("both|build")) {
                if (config.getConfig(BuildParm.TARGET_DB_PASS) == null) {
                    System.out.print("Enter target DB password: ");
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                        config.setConfig(BuildParm.TARGET_DB_PASS, in.readLine());
                    } catch (Throwable t) {
                        throw new RuntimeException("Read of target DB password failed", t);
                    }
                }
                Connection tConn = ConnectionUtil.getTargetDbConnection();
                build(tConn, config.getBuildHomeDir());
                tConn.close();
            }
        } catch (Exception e) {
            LOG.fatal("Builder: failed", e);
            throw new RuntimeException("Builder: failed", e);
        }
        if (LOG.isInfoEnabled()) LOG.info("Builder: done");
    }

    /**
	 * Main entry point method.  Calls the run() method in the current thread.
	 * @param args
	 */
    public static void main(String[] args) {
        if (args == null || args.length < 1 || args[0].matches("-h|-help")) {
            printUsage(System.out);
            return;
        }
        ConfigUtil config = ConfigUtil.getInstance();
        config.processArgs(args);
        new Builder().run();
    }
}
