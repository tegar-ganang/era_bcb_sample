package com.continuent.tungsten.router.jdbc;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import com.continuent.bristlecone.benchmark.db.Column;
import com.continuent.bristlecone.benchmark.db.Table;
import com.continuent.bristlecone.benchmark.db.TableHelper;
import com.continuent.tungsten.commons.config.TungstenProperties;

/**
 * Implements a utility class to hold information required by tests and and to
 * implement common operations like creating service property files.
 * 
 * @author <a href="mailto:robert.hodges@continuent.com">Robert Hodges</a>
 * @version 1.0
 */
public class SetupHelper extends TestCase {

    private static Logger logger = Logger.getLogger(SetupHelper.class);

    private String nativeDriver;

    private String readwriteUrl;

    private String readonlyUrl;

    private String user;

    private String password;

    private File routerConf;

    private static Table tableDbtype;

    /**
     * Create and configure the helper instance.
     */
    public SetupHelper() throws Exception {
        String testPropertiesName = System.getProperty("test.properties");
        if (testPropertiesName == null) {
            testPropertiesName = "test.properties";
            logger.info("Setting test.properties file name to default: test.properties");
        }
        TungstenProperties tp = new TungstenProperties();
        File f = new File(testPropertiesName);
        if (f.canRead()) {
            logger.info("Reading test.properties file: " + testPropertiesName);
            FileInputStream fis = new FileInputStream(f);
            tp.load(fis);
            fis.close();
        } else logger.warn("Using default values for test");
        nativeDriver = tp.getString("test.native.driver", "org.apache.derby.jdbc.EmbeddedDriver", true);
        readwriteUrl = tp.getString("test.readwrite.url", "jdbc:derby:readwrite;create=true", true);
        readonlyUrl = tp.getString("test.readonly.url", "jdbc:derby:readonly;create=true", true);
        user = tp.getString("test.database.user");
        password = tp.getString("test.database.password");
        String routerHomeName = System.getProperty("router.home");
        if (routerHomeName == null) throw new Exception("Property router.home is not set");
        File routerHome = new File(routerHomeName);
        if (!routerHome.exists()) {
            logger.info("Creating missing router.home directory: " + routerHome.getAbsolutePath());
            routerHome.mkdirs();
        }
        if (!routerHome.isDirectory() || !routerHome.canRead()) throw new Exception("Directory router.home invalid or unreadable: " + routerHome.getAbsolutePath());
        routerConf = new File(routerHome, "conf");
        if (!routerConf.exists()) {
            logger.info("Creating missing router.home/conf directory: " + routerConf.getAbsolutePath());
            routerConf.mkdirs();
        }
        if (!routerConf.isDirectory() || !routerConf.canWrite()) throw new Exception("Directory router.home/conf invalid or unreadable: " + routerConf.getAbsolutePath());
        Column colDbtype = new Column();
        colDbtype.setName("dbtype");
        colDbtype.setType(Types.VARCHAR);
        colDbtype.setLength(40);
        Column[] cols = new Column[] { colDbtype };
        tableDbtype = new Table("dbtype_table", cols);
        createDbtypeTable(this.readwriteUrl, "rw");
        createDbtypeTable(this.readonlyUrl, "ro");
    }

    public String getNativeDriver() {
        return nativeDriver;
    }

    public void setNativeDriver(String nativeDriver) {
        this.nativeDriver = nativeDriver;
    }

    public String getReadwriteUrl() {
        return readwriteUrl;
    }

    public void setReadwriteUrl(String readwriteUrl) {
        this.readwriteUrl = readwriteUrl;
    }

    public String getReadonlyUrl() {
        return readonlyUrl;
    }

    public void setReadonlyUrl(String readonlyUrl) {
        this.readonlyUrl = readonlyUrl;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Creates two standard resource files named master and slave.
     */
    public void createResourceFiles() throws Exception {
        File resourcesDir = new File(routerConf, "resources");
        File datastoreDir = new File(resourcesDir, "datastore");
        if (!datastoreDir.exists()) {
            logger.info("Creating new datastore directory: " + datastoreDir.getAbsolutePath());
            datastoreDir.mkdirs();
        }
        if (!datastoreDir.isDirectory() || !datastoreDir.canWrite()) throw new Exception("Datastore directory invalid or unreadable: " + datastoreDir.getAbsolutePath());
        FileFilter filter = new FileFilter() {

            public boolean accept(File f) {
                return (f.getName().endsWith(".properties"));
            }
        };
        for (File f : datastoreDir.listFiles(filter)) {
            File f2 = new File(f.getAbsolutePath() + ".save");
            f.renameTo(f2);
            logger.info("Renamed existing property file: " + f.getName() + "->" + f2.getName());
        }
        File masterFile = new File(datastoreDir, "master.properties");
        File slaveFile = new File(datastoreDir, "slave.properties");
        TungstenProperties tp = new TungstenProperties();
        tp.setString("vendor", "some vendor");
        tp.setString("name", "master");
        tp.setString("description", "master datastore");
        tp.setString("driver", nativeDriver);
        tp.setString("url", readwriteUrl);
        tp.setString("role", "master");
        tp.setString("precedence", "1");
        tp.setString("isAvailable", "true");
        FileOutputStream fos1 = new FileOutputStream(masterFile);
        tp.store(fos1);
        fos1.close();
        tp.setString("name", "slave");
        tp.setString("description", "slave datastore");
        tp.setString("url", readonlyUrl);
        tp.setString("role", "slave");
        FileOutputStream fos2 = new FileOutputStream(slaveFile);
        tp.store(fos2);
        fos2.close();
    }

    public String selectDbtype(Connection conn) throws SQLException {
        String dbtype = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("select dbtype from dbtype_table");
            while (rs.next()) {
                dbtype = rs.getString(1);
            }
            return dbtype;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
        }
    }

    private void createDbtypeTable(String url, String dataValue) throws Exception {
        TableHelper helper = new TableHelper(url, user, password);
        helper.create(tableDbtype, true);
        Object[] values = new Object[] { dataValue };
        helper.insert(tableDbtype, values);
    }

    private void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
            }
        }
    }

    private void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
            }
        }
    }
}
