package org.opennms.install;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.DatagramPacket;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.opennms.core.schema.Migration;
import org.opennms.core.schema.Migrator;
import org.opennms.core.utils.ProcessExec;
import org.opennms.netmgt.ConfigFileConstants;
import org.opennms.netmgt.config.C3P0ConnectionFactory;
import org.opennms.netmgt.config.opennmsDataSources.JdbcDataSource;
import org.opennms.netmgt.dao.db.InstallerDb;
import org.opennms.netmgt.dao.db.SimpleDataSource;
import org.opennms.netmgt.ping.Ping;
import org.opennms.protocols.icmp.IcmpSocket;
import org.springframework.util.StringUtils;

public class Installer {

    static final String s_version = "$Id: Installer.java 13609 2009-06-07 00:59:50Z rangerrick $";

    static final String LIBRARY_PROPERTY_FILE = "libraries.properties";

    String m_opennms_home = null;

    boolean m_update_database = false;

    boolean m_do_inserts = false;

    boolean m_skip_constraints = false;

    boolean m_update_iplike = false;

    boolean m_update_unicode = false;

    boolean m_do_full_vacuum = false;

    boolean m_do_vacuum = false;

    boolean m_install_webapp = false;

    boolean m_fix_constraint = false;

    boolean m_force = false;

    boolean m_ignore_not_null = false;

    boolean m_ignore_database_version = false;

    boolean m_do_not_revert = false;

    boolean m_remove_database = false;

    String m_etc_dir = "";

    String m_tomcat_conf = null;

    String m_webappdir = null;

    String m_import_dir = null;

    String m_install_servletdir = null;

    String m_library_search_path = null;

    String m_fix_constraint_name = null;

    boolean m_fix_constraint_remove_rows = false;

    protected Options options = new Options();

    protected CommandLine m_commandLine;

    private PrintStream m_out;

    private Migration m_migration = new Migration();

    private Migrator m_migrator = new Migrator();

    Properties m_properties = null;

    String m_required_options = "At least one of -d, -i, -s, -y, -C, or -T is required.";

    private InstallerDb m_installerDb = new InstallerDb();

    private static final String OPENNMS_DATA_SOURCE_NAME = "opennms";

    private static final String ADMIN_DATA_SOURCE_NAME = "opennms-admin";

    public Installer() {
        setOutputStream(System.out);
    }

    public void install(String[] argv) throws Exception {
        printHeader();
        loadProperties();
        parseArguments(argv);
        boolean doDatabase = (m_update_database || m_do_inserts || m_update_iplike || m_update_unicode || m_fix_constraint);
        if (!doDatabase && m_tomcat_conf == null && !m_install_webapp && m_library_search_path == null) {
            usage(options, m_commandLine, "Nothing to do.  Use -h for help.", null);
            System.exit(1);
        }
        if (doDatabase) {
            File cfgFile = ConfigFileConstants.getFile(ConfigFileConstants.OPENNMS_DATASOURCE_CONFIG_FILE_NAME);
            Reader fr = new FileReader(cfgFile);
            JdbcDataSource adminDsConfig = C3P0ConnectionFactory.marshalDataSourceFromConfig(fr, ADMIN_DATA_SOURCE_NAME);
            DataSource adminDs = new SimpleDataSource(adminDsConfig);
            fr.close();
            fr = new FileReader(cfgFile);
            JdbcDataSource dsConfig = C3P0ConnectionFactory.marshalDataSourceFromConfig(fr, OPENNMS_DATA_SOURCE_NAME);
            DataSource ds = new SimpleDataSource(dsConfig);
            fr.close();
            m_installerDb.setForce(m_force);
            m_installerDb.setIgnoreNotNull(m_ignore_not_null);
            m_installerDb.setNoRevert(m_do_not_revert);
            m_installerDb.setAdminDataSource(adminDs);
            m_installerDb.setPostgresOpennmsUser(dsConfig.getUserName());
            m_installerDb.setPostgresOpennmsPassword(dsConfig.getPassword());
            m_installerDb.setDataSource(ds);
            m_installerDb.setDatabaseName(dsConfig.getDatabaseName());
            m_migrator.setDataSource(ds);
            m_migrator.setAdminDataSource(adminDs);
            m_migrator.setValidateDatabaseVersion(!m_ignore_database_version);
            m_migration.setDatabaseName(dsConfig.getDatabaseName());
            m_migration.setAdminUser(adminDsConfig.getUserName());
            m_migration.setAdminPassword(adminDsConfig.getPassword());
            m_migration.setDatabaseUser(dsConfig.getUserName());
            m_migration.setDatabasePassword(dsConfig.getPassword());
            m_migration.setChangeLog("changelog.xml");
        }
        if (!Boolean.getBoolean("skip-native")) {
            String icmp_path = findLibrary("jicmp", m_library_search_path, true);
            String jrrd_path = findLibrary("jrrd", m_library_search_path, false);
            writeLibraryConfig(icmp_path, jrrd_path);
        }
        verifyFilesAndDirectories();
        if (m_install_webapp) {
            checkWebappOldOpennmsDir();
            checkServerXmlOldOpennmsContext();
        }
        if (m_update_database || m_fix_constraint) {
        }
        if (doDatabase) {
            m_installerDb.databaseCheckLanguage();
            m_installerDb.disconnect();
            m_migrator.validateDatabaseVersion();
            m_out.println(String.format("* using '%s' as the PostgreSQL user for OpenNMS", m_migration.getAdminUser()));
            m_out.println(String.format("* using '%s' as the PostgreSQL password for OpenNMS", m_migration.getAdminPassword()));
            m_out.println(String.format("* using '%s' as the PostgreSQL database name for OpenNMS", m_migration.getDatabaseName()));
        }
        if (m_update_database) {
            m_migrator.prepareDatabase(m_migration);
        }
        if (doDatabase) {
            m_installerDb.checkUnicode();
        }
        handleConfigurationChanges();
        if (m_update_database) {
            m_installerDb.databaseSetUser();
            m_installerDb.disconnect();
            m_out.println("- Migrating/creating database:");
            m_migrator.migrate(m_migration);
        }
        if (m_update_unicode) {
            m_out.println("WARNING: the -U option is deprecated, it does nothing now");
        }
        if (m_do_vacuum) {
            m_installerDb.vacuumDatabase(m_do_full_vacuum);
        }
        if (m_install_webapp) {
            installWebApp();
        }
        if (m_tomcat_conf != null) {
            updateTomcatConf();
        }
        if (m_update_iplike) {
            m_installerDb.updateIplike();
        }
        if (m_update_database && m_remove_database) {
            m_installerDb.disconnect();
            m_installerDb.databaseRemoveDB();
        }
        if (doDatabase) {
            m_installerDb.disconnect();
        }
        if (m_update_database) {
            createConfiguredFile();
        }
        m_out.println();
        m_out.println("Installer completed successfully!");
    }

    private void handleConfigurationChanges() {
        File etcDir = new File(m_opennms_home + File.separator + "etc");
        File importDir = new File(m_import_dir);
        File[] files = etcDir.listFiles(getImportFileFilter());
        if (!importDir.exists()) {
            m_out.print("- Creating imports directory (" + importDir.getAbsolutePath() + "... ");
            if (!importDir.mkdirs()) {
                m_out.println("FAILED");
                System.exit(1);
            }
            m_out.println("OK");
        }
        m_out.print("- Checking for old import files in " + etcDir.getAbsolutePath() + "... ");
        if (files.length > 0) {
            m_out.println("FOUND");
            for (File f : files) {
                String newFileName = f.getName().replace("imports-", "");
                File newFile = new File(importDir, newFileName);
                m_out.print("  - moving " + f.getName() + " to " + importDir.getPath() + "... ");
                if (f.renameTo(newFile)) {
                    m_out.println("OK");
                } else {
                    m_out.println("FAILED");
                }
            }
        } else {
            m_out.println("DONE");
        }
    }

    private FilenameFilter getImportFileFilter() {
        return new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.matches("imports-.*\\.xml");
            }
        };
    }

    public void createConfiguredFile() throws IOException {
        File f = new File(m_opennms_home + File.separator + "etc" + File.separator + "configured");
        f.createNewFile();
    }

    public void printHeader() {
        m_out.println("==============================================================================");
        m_out.println("OpenNMS Installer Version " + s_version);
        m_out.println("==============================================================================");
        m_out.println("");
        m_out.println("Configures PostgreSQL tables, users, and other miscellaneous settings.");
        m_out.println("");
    }

    public void loadProperties() throws Exception {
        m_properties = new Properties();
        m_properties.load(Installer.class.getResourceAsStream("installer.properties"));
        Properties sys = System.getProperties();
        m_properties.putAll(sys);
        m_opennms_home = fetchProperty("install.dir");
        m_etc_dir = fetchProperty("install.etc.dir");
        try {
            Properties opennmsProperties = new Properties();
            InputStream ois = new FileInputStream(m_etc_dir + File.separator + "model-importer.properties");
            opennmsProperties.load(ois);
            for (Entry<Object, Object> p : opennmsProperties.entrySet()) {
                if (!m_properties.containsKey(p.getKey())) {
                    m_properties.put(p.getKey(), p.getValue());
                }
            }
        } catch (FileNotFoundException e) {
            m_out.println("WARNING: unable to load " + m_etc_dir + File.separator + "opennms.properties");
        }
        m_install_servletdir = fetchProperty("install.servlet.dir");
        m_import_dir = fetchProperty("importer.requisition.dir");
        String soext = fetchProperty("build.soext");
        String pg_iplike_dir = m_properties.getProperty("install.postgresql.dir");
        if (pg_iplike_dir != null) {
            m_installerDb.setPostgresIpLikeLocation(pg_iplike_dir + File.separator + "iplike." + soext);
        }
        m_installerDb.setStoredProcedureDirectory(m_etc_dir);
        m_installerDb.setCreateSqlLocation(m_etc_dir + File.separator + "create.sql");
    }

    public String fetchProperty(String property) throws Exception {
        String value;
        if ((value = m_properties.getProperty(property)) == null) {
            throw new Exception("property \"" + property + "\" not set " + "from bundled installer.properties file");
        }
        return value;
    }

    public void parseArguments(String[] argv) throws Exception {
        options.addOption("h", "help", false, "this help");
        options.addOption("d", "do-database", false, "perform database actions");
        options.addOption("Z", "remove-database", false, "remove the OpenNMS database");
        options.addOption("u", "username", true, "username of the database account (default: 'opennms')");
        options.addOption("p", "password", true, "password of the database account (default: 'opennms')");
        options.addOption("a", "admin-username", true, "username of the database administrator (default: 'postgres')");
        options.addOption("A", "admin-password", true, "password of the database administrator (default: '')");
        options.addOption("D", "database-url", true, "JDBC database URL (default: jdbc:postgresql://localhost:5432/");
        options.addOption("P", "database-name", true, "name of the PostgreSQL database (default: opennms)");
        options.addOption("c", "clean-database", false, "clean existing database before creating");
        options.addOption("i", "insert-data", false, "insert (or upgrade) default data including database and XML configuration");
        options.addOption("s", "stored-procedure", false, "add the IPLIKE stored procedure if it's missing");
        options.addOption("U", "unicode", false, "upgrade the database to Unicode (deprecated, does nothing)");
        options.addOption("v", "vacuum", false, "vacuum (optimize) the database");
        options.addOption("f", "vacuum-full", false, "vacuum full the database (recovers unused disk space)");
        options.addOption("N", "ignore-not-null", false, "ignore NOT NULL constraint when transforming data");
        options.addOption("Q", "ignore-database-version", false, "disable the database version check");
        options.addOption("x", "database-debug", false, "turn on debugging for the database data transformation");
        options.addOption("R", "do-not-revert", false, "do not revert a table to the original if an error occurs");
        options.addOption("n", "skip-constraint", false, "");
        options.addOption("C", "repair-constraint", true, "fix rows that violate the specified constraint (sets key column to NULL)");
        options.addOption("X", "drop-constraint", false, "drop rows that match the constraint specified in -C, instead of fixing them");
        options.addOption("y", "do-webapp", false, "install web application (see '-w')");
        options.addOption("T", "tomcat-conf", true, "location of tomcat.conf");
        options.addOption("w", "tomcat-context", true, "location of the tomcat context (eg, conf/Catalina/localhost)");
        options.addOption("l", "library-path", true, "library search path (directories separated by '" + File.pathSeparator + "')");
        options.addOption("r", "rpm-install", false, "RPM install (deprecated)");
        CommandLineParser parser = new PosixParser();
        m_commandLine = parser.parse(options, argv);
        if (m_commandLine.hasOption("h")) {
            usage(options, m_commandLine);
            System.exit(0);
        }
        options.addOption("u", "username", true, "replaced by opennms-datasources.xml");
        options.addOption("p", "password", true, "replaced by opennms-datasources.xml");
        options.addOption("a", "admin-username", true, "replaced by opennms-datasources.xml");
        options.addOption("A", "admin-password", true, "replaced by opennms-datasources.xml");
        options.addOption("D", "database-url", true, "replaced by opennms-datasources.xml");
        options.addOption("P", "database-name", true, "replaced by opennms-datasources.xml");
        if (m_commandLine.hasOption("u") || m_commandLine.hasOption("p") || m_commandLine.hasOption("a") || m_commandLine.hasOption("A") || m_commandLine.hasOption("D") || m_commandLine.hasOption("P")) {
            usage(options, m_commandLine, "The 'u', 'p', 'a', 'A', 'D', and 'P' options have all been superceded.\nPlease edit $OPENNMS_HOME/etc/opennms-datasources.xml instead.", null);
            System.exit(1);
        }
        m_force = m_commandLine.hasOption("c");
        m_fix_constraint = m_commandLine.hasOption("C");
        m_fix_constraint_name = m_commandLine.getOptionValue("C");
        m_update_database = m_commandLine.hasOption("d");
        m_remove_database = m_commandLine.hasOption("Z");
        m_do_full_vacuum = m_commandLine.hasOption("f");
        m_do_inserts = m_commandLine.hasOption("i");
        m_library_search_path = m_commandLine.getOptionValue("l", m_library_search_path);
        m_skip_constraints = m_commandLine.hasOption("n");
        m_ignore_not_null = m_commandLine.hasOption("N");
        m_ignore_database_version = m_commandLine.hasOption("Q");
        m_do_not_revert = m_commandLine.hasOption("R");
        m_update_iplike = m_commandLine.hasOption("s");
        m_tomcat_conf = m_commandLine.getOptionValue("T", m_tomcat_conf);
        m_update_unicode = m_commandLine.hasOption("U");
        m_do_vacuum = m_commandLine.hasOption("v");
        m_webappdir = m_commandLine.getOptionValue("w", m_webappdir);
        m_installerDb.setDebug(m_commandLine.hasOption("x"));
        m_fix_constraint_remove_rows = m_commandLine.hasOption("X");
        m_install_webapp = m_commandLine.hasOption("y");
        if (m_commandLine.getArgList().size() > 0) {
            usage(options, m_commandLine, "Unknown command-line arguments: " + Arrays.toString(m_commandLine.getArgs()), null);
            System.exit(1);
        }
    }

    public void verifyFilesAndDirectories() throws FileNotFoundException {
        if (m_update_database) {
            verifyFileExists(true, m_installerDb.getStoredProcedureDirectory(), "SQL directory", "install.etc.dir property");
            verifyFileExists(false, m_installerDb.getCreateSqlLocation(), "create.sql", "install.etc.dir property");
        }
        if (m_tomcat_conf != null) {
            verifyFileExists(false, m_tomcat_conf, "Tomcat startup configuration file tomcat4.conf", "-T option");
        }
        if (m_install_webapp) {
            verifyFileExists(true, m_webappdir, "Tomcat context directory", "-w option");
            verifyFileExists(true, m_install_servletdir, "OpenNMS servlet directory", "install.servlet.dir property");
        }
    }

    public void verifyFileExists(boolean isDir, String file, String description, String option) throws FileNotFoundException {
        File f;
        if (file == null) {
            throw new FileNotFoundException("The user most provide the location of " + description + ", but this is not specified.  Use the " + option + " to specify this file.");
        }
        m_out.print("- using " + description + "... ");
        f = new File(file);
        if (!f.exists()) {
            throw new FileNotFoundException(description + " does not exist at \"" + file + "\".  Use the " + option + " to specify another location.");
        }
        if (!isDir) {
            if (!f.isFile()) {
                throw new FileNotFoundException(description + " not a file at \"" + file + "\".  Use the " + option + " to specify another file.");
            }
        } else {
            if (!f.isDirectory()) {
                throw new FileNotFoundException(description + " not a directory at \"" + file + "\".  Use the " + option + " to specify " + "another directory.");
            }
        }
        m_out.println(f.getAbsolutePath());
    }

    public void checkWebappOldOpennmsDir() throws Exception {
        File f = new File(m_webappdir + File.separator + "opennms");
        m_out.print("- Checking for old opennms webapp directory in " + f.getAbsolutePath() + "... ");
        if (f.exists()) {
            throw new Exception("Old OpenNMS web application exists: " + f.getAbsolutePath() + ".  You need to remove this " + "before continuing.");
        }
        m_out.println("OK");
    }

    public void checkServerXmlOldOpennmsContext() throws Exception {
        String search_regexp = "(?ms).*<Context\\s+path=\"/opennms\".*";
        StringBuffer b = new StringBuffer();
        File f = new File(m_webappdir + File.separator + ".." + File.separator + "conf" + File.separator + "server.xml");
        m_out.print("- Checking for old opennms context in " + f.getAbsolutePath() + "... ");
        if (!f.exists()) {
            m_out.println("DID NOT CHECK (file does not exist)");
            return;
        }
        FileReader fr = new FileReader(f);
        BufferedReader r = new BufferedReader(fr);
        String line;
        while ((line = r.readLine()) != null) {
            b.append(line);
            b.append("\n");
        }
        r.close();
        fr.close();
        if (b.toString().matches(search_regexp)) {
            throw new Exception("Old OpenNMS context found in " + f.getAbsolutePath() + ".  You must remove this context from server.xml and re-run the installer.");
        }
        m_out.println("OK");
        return;
    }

    public void installWebApp() throws Exception {
        m_out.println("- Install OpenNMS webapp... ");
        copyFile(m_install_servletdir + File.separator + "META-INF" + File.separator + "context.xml", m_webappdir + File.separator + "opennms.xml", "web application context", false);
        m_out.println("- Installing OpenNMS webapp... DONE");
    }

    public void copyFile(String source, String destination, String description, boolean recursive) throws Exception {
        File sourceFile = new File(source);
        File destinationFile = new File(destination);
        if (!sourceFile.exists()) {
            throw new Exception("source file (" + source + ") does not exist!");
        }
        if (!sourceFile.isFile()) {
            throw new Exception("source file (" + source + ") is not a file!");
        }
        if (!sourceFile.canRead()) {
            throw new Exception("source file (" + source + ") is not readable!");
        }
        if (destinationFile.exists()) {
            m_out.print("  - " + destination + " exists, removing... ");
            if (destinationFile.delete()) {
                m_out.println("REMOVED");
            } else {
                m_out.println("FAILED");
                throw new Exception("unable to delete existing file: " + sourceFile);
            }
        }
        m_out.print("  - copying " + source + " to " + destination + "... ");
        if (!destinationFile.getParentFile().exists()) {
            if (!destinationFile.getParentFile().mkdirs()) {
                throw new Exception("unable to create directory: " + destinationFile.getParent());
            }
        }
        if (!destinationFile.createNewFile()) {
            throw new Exception("unable to create file: " + destinationFile);
        }
        FileChannel from = null;
        FileChannel to = null;
        try {
            from = new FileInputStream(sourceFile).getChannel();
            to = new FileOutputStream(destinationFile).getChannel();
            to.transferFrom(from, 0, from.size());
        } catch (FileNotFoundException e) {
            throw new Exception("unable to copy " + sourceFile + " to " + destinationFile, e);
        } finally {
            if (from != null) {
                from.close();
            }
            if (to != null) {
                to.close();
            }
        }
        m_out.println("DONE");
    }

    public void installLink(String source, String destination, String description, boolean recursive) throws Exception {
        String[] cmd;
        ProcessExec e = new ProcessExec(m_out, m_out);
        if (new File(destination).exists()) {
            m_out.print("  - " + destination + " exists, removing... ");
            removeFile(destination, description, recursive);
            m_out.println("REMOVED");
        }
        m_out.print("  - creating link to " + destination + "... ");
        cmd = new String[4];
        cmd[0] = "ln";
        cmd[1] = "-sf";
        cmd[2] = source;
        cmd[3] = destination;
        if (e.exec(cmd) != 0) {
            throw new Exception("Non-zero exit value returned while " + "linking " + description + ", " + source + " into " + destination);
        }
        m_out.println("DONE");
    }

    public void updateTomcatConf() throws Exception {
        File f = new File(m_tomcat_conf);
        m_out.print("- setting tomcat4 user to 'root'... ");
        BufferedReader r = new BufferedReader(new FileReader(f));
        StringBuffer b = new StringBuffer();
        String line;
        while ((line = r.readLine()) != null) {
            if (line.startsWith("TOMCAT_USER=")) {
                b.append("TOMCAT_USER=\"root\"\n");
            } else {
                b.append(line);
                b.append("\n");
            }
        }
        r.close();
        f.renameTo(new File(m_tomcat_conf + ".before-opennms-" + System.currentTimeMillis()));
        f = new File(m_tomcat_conf);
        PrintWriter w = new PrintWriter(new FileOutputStream(f));
        w.print(b.toString());
        w.close();
        m_out.println("DONE");
    }

    public void removeFile(String destination, String description, boolean recursive) throws IOException, InterruptedException, Exception {
        String[] cmd;
        ProcessExec e = new ProcessExec(m_out, m_out);
        if (recursive) {
            cmd = new String[3];
            cmd[0] = "rm";
            cmd[1] = "-r";
            cmd[2] = destination;
        } else {
            cmd = new String[2];
            cmd[0] = "rm";
            cmd[1] = destination;
        }
        if (e.exec(cmd) != 0) {
            throw new Exception("Non-zero exit value returned while " + "removing " + description + ", " + destination + ", using \"" + StringUtils.arrayToDelimitedString(cmd, " ") + "\"");
        }
        if (new File(destination).exists()) {
            usage(options, m_commandLine, "Could not delete existing " + description + ": " + destination, null);
            System.exit(1);
        }
    }

    private void usage(Options options, CommandLine cmd) {
        usage(options, cmd, null, null);
    }

    private void usage(Options options, CommandLine cmd, String error, Exception e) {
        HelpFormatter formatter = new HelpFormatter();
        PrintWriter pw = new PrintWriter(m_out);
        if (error != null) {
            pw.println("An error occurred: " + error + "\n");
        }
        formatter.printHelp("usage: install [options]", options);
        if (e != null) {
            pw.println(e.getMessage());
            e.printStackTrace(pw);
        }
        pw.close();
    }

    public static void main(String[] argv) throws Exception {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.WARN);
        new Installer().install(argv);
    }

    public String checkServerVersion() throws IOException {
        File catalinaHome = new File(m_webappdir).getParentFile();
        String readmeVersion = getTomcatVersion(new File(catalinaHome, "README.txt"));
        String runningVersion = getTomcatVersion(new File(catalinaHome, "RUNNING.txt"));
        if (readmeVersion == null && runningVersion == null) {
            return null;
        } else if (readmeVersion != null && runningVersion != null) {
            return readmeVersion;
        } else if (readmeVersion != null && runningVersion == null) {
            return readmeVersion;
        } else {
            return runningVersion;
        }
    }

    public String getTomcatVersion(File file) throws IOException {
        if (file == null || !file.exists()) {
            return null;
        }
        Pattern p = Pattern.compile("The Tomcat (\\S+) Servlet/JSP Container");
        BufferedReader in = new BufferedReader(new FileReader(file));
        for (int i = 0; i < 5; i++) {
            String line = in.readLine();
            if (line == null) {
                in.close();
                return null;
            }
            Matcher m = p.matcher(line);
            if (m.find()) {
                in.close();
                return m.group(1);
            }
        }
        in.close();
        return null;
    }

    @SuppressWarnings("unchecked")
    public String findLibrary(String libname, String path, boolean isRequired) throws Exception {
        String fullname = System.mapLibraryName(libname);
        ArrayList<String> searchPaths = new ArrayList<String>();
        if (path != null) {
            for (String entry : path.split(File.pathSeparator)) {
                searchPaths.add(entry);
            }
        }
        try {
            File confFile = new File(m_opennms_home + File.separator + "etc" + File.separator + LIBRARY_PROPERTY_FILE);
            Properties p = new Properties();
            InputStream is = new FileInputStream(confFile);
            p.load(is);
            is.close();
            for (Enumeration e = p.keys(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                if (key.startsWith("opennms.library")) {
                    String value = p.getProperty(key);
                    value.replaceAll(File.separator + "[^" + File.separator + "]*$", "");
                    searchPaths.add(value);
                }
            }
        } catch (Exception e) {
        }
        if (System.getProperty("java.library.path") != null) {
            for (String entry : System.getProperty("java.library.path").split(File.pathSeparator)) {
                searchPaths.add(entry);
            }
        }
        if (!System.getProperty("os.name").contains("Windows")) {
            String[] defaults = { "/usr/lib/jni", "/usr/lib", "/usr/local/lib", "/opt/NMSjicmp/lib/32", "/opt/NMSjicmp/lib/64" };
            for (String entry : defaults) {
                searchPaths.add(entry);
            }
        }
        m_out.println("- searching for " + libname + ":");
        for (String dirname : searchPaths) {
            File entry = new File(dirname);
            if (entry.isFile()) {
                dirname = entry.getParent();
            }
            String fullpath = dirname + File.separator + fullname;
            if (loadLibrary(fullpath)) {
                return fullpath;
            }
        }
        if (isRequired) {
            StringBuffer buf = new StringBuffer();
            for (String pathEntry : System.getProperty("java.library.path").split(File.pathSeparator)) {
                buf.append(" ");
                buf.append(pathEntry);
            }
            throw new Exception("Failed to load the required " + libname + " library that is required at runtime.  By default, we search the Java library path:" + buf.toString() + ".  For more information, see http://www.opennms.org/index.php/" + libname);
        } else {
            m_out.println("- Failed to load the optional " + libname + " library.");
            m_out.println("  - This error is not fatal, since " + libname + " is only required for optional features.");
            m_out.println("  - For more information, see http://www.opennms.org/index.php/" + libname);
        }
        return null;
    }

    public boolean loadLibrary(String path) {
        try {
            m_out.print("  - trying to load " + path + ": ");
            System.load(path);
            m_out.println("OK");
            return true;
        } catch (UnsatisfiedLinkError ule) {
            m_out.println("NO");
        }
        return false;
    }

    public void writeLibraryConfig(String jicmp_path, String jrrd_path) throws IOException {
        Properties libraryProps = new Properties();
        if (jicmp_path != null && jicmp_path.length() != 0) {
            libraryProps.put("opennms.library.jicmp", jicmp_path);
        }
        if (jrrd_path != null && jrrd_path.length() != 0) {
            libraryProps.put("opennms.library.jrrd", jrrd_path);
        }
        File f = null;
        try {
            f = new File(m_opennms_home + File.separator + "etc" + File.separator + LIBRARY_PROPERTY_FILE);
            f.createNewFile();
            FileOutputStream os = new FileOutputStream(f);
            libraryProps.store(os, null);
        } catch (IOException e) {
            m_out.println("unable to write to " + f.getPath());
            throw e;
        }
    }

    public void pingLocalhost() throws IOException {
        String host = "127.0.0.1";
        IcmpSocket m_socket = null;
        try {
            m_socket = new IcmpSocket();
        } catch (UnsatisfiedLinkError e) {
            m_out.println("UnsatisfiedLinkError while creating an " + "IcmpSocket.  Most likely failed to load " + "libjicmp.so.  Try setting the property " + "'opennms.library.jicmp' to point at the " + "full path name of the libjicmp.so shared " + "library " + "(e.g. 'java -Dopennms.library.jicmp=/some/path/libjicmp.so ...')");
            throw e;
        } catch (NoClassDefFoundError e) {
            m_out.println("NoClassDefFoundError while creating an " + "IcmpSocket.  Most likely failed to load " + "libjicmp.so.");
            throw e;
        } catch (IOException e) {
            m_out.println("IOException while creating an " + "IcmpSocket.");
            throw e;
        }
        java.net.InetAddress addr = null;
        try {
            addr = java.net.InetAddress.getByName(host);
        } catch (java.net.UnknownHostException e) {
            m_out.println("UnknownHostException when looking up " + host + ".");
            throw e;
        }
        m_out.println("PING " + host + " (" + addr.getHostAddress() + "): 56 data bytes");
        short m_icmpId = 2;
        Ping.Stuff s = new Ping.Stuff(m_socket, m_icmpId);
        Thread t = new Thread(s);
        t.start();
        int count = 3;
        for (long attempt = 0; attempt < count; attempt++) {
            org.opennms.protocols.icmp.ICMPEchoPacket pingPkt = new org.opennms.protocols.icmp.ICMPEchoPacket(attempt);
            pingPkt.setIdentity(m_icmpId);
            pingPkt.computeChecksum();
            byte[] buf = pingPkt.toBytes();
            DatagramPacket sendPkt = new DatagramPacket(buf, buf.length, addr, 0);
            buf = null;
            pingPkt = null;
            try {
                m_socket.send(sendPkt);
            } catch (IOException e) {
                m_out.println("IOException received when sending packet.");
                throw e;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }

    public InstallerDb getInstallerDb() {
        return m_installerDb;
    }

    public void setOutputStream(PrintStream out) {
        m_out = out;
        m_installerDb.setOutputStream(m_out);
    }
}
