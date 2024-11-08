package com.continuent.tungsten.router.utils;

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
import com.continuent.tungsten.router.config.ClusterPolicyManagerConfiguration;
import com.continuent.tungsten.router.config.ConfigurationConstants;
import com.continuent.tungsten.router.config.ResourceTypes;
import com.continuent.tungsten.router.config.RouterConfiguration;
import com.continuent.tungsten.router.config.ClusterConfiguration;

/**
 * Implements a utility class to hold information required by tests and and to
 * implement common operations like creating service property files.
 * 
 * @author <a href="mailto:robert.hodges@continuent.com">Robert Hodges</a>
 * @version 1.0
 */
public class SetupHelper extends TestCase {

    private static Logger logger = Logger.getLogger(SetupHelper.class);

    public static final String DEFAULT_SERVICE_NAME = "smoke-service";

    private String dataServiceName = null;

    private String nativeDriver;

    private String readwriteUrl;

    private String readonlyUrl;

    private String user;

    private String password;

    private String clusterHomeName = null;

    private String testHostName = null;

    private ClusterConfiguration clusterConfig = null;

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
        dataServiceName = tp.getString("test.database.serviceName", DEFAULT_SERVICE_NAME, true);
        clusterHomeName = System.getProperty("cluster.home");
        if (clusterHomeName == null) throw new Exception("Property cluster.home is not set");
        createDefaultConfiguration("smoke-service");
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

    public String getDataServiceName() {
        return dataServiceName;
    }

    public void setDataServiceName(String serviceName) {
        this.dataServiceName = serviceName;
    }

    /**
     * Creates two standard resource files named master and slave.
     * 
     * @param serviceName TODO
     */
    public void createDefaultConfiguration(String serviceName) throws Exception {
        clusterConfig = new ClusterConfiguration();
        clusterConfig.createDefaultConfiguration();
        clusterConfig.createServiceConfigDirs(serviceName);
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

    public void createServiceDataSources(String rwURL, String roURL) throws Exception {
        File dataSourceDir = new File(clusterConfig.getResourceConfigDirName(clusterHomeName, dataServiceName, ResourceTypes.DATASOURCE));
        if (!dataSourceDir.exists()) {
            logger.info("Creating new datasource directory: " + dataSourceDir.getAbsolutePath());
            dataSourceDir.mkdirs();
        }
        if (!dataSourceDir.isDirectory() || !dataSourceDir.canWrite()) {
            throw new Exception("DataSource config directory invalid or unreadable: " + dataSourceDir.getAbsolutePath());
        }
        FileFilter filter = new FileFilter() {

            public boolean accept(File f) {
                return (f.getName().endsWith(".properties"));
            }
        };
        for (File f : dataSourceDir.listFiles(filter)) {
            File f2 = new File(f.getAbsolutePath() + ".save");
            f.renameTo(f2);
            logger.info("Renamed existing property file: " + f.getName() + "->" + f2.getName());
        }
        File masterFile = new File(dataSourceDir, "master.properties");
        File slaveFile = new File(dataSourceDir, "slave.properties");
        TungstenProperties tp = new TungstenProperties();
        tp.setString("vendor", "some vendor");
        tp.setString("name", "master");
        tp.setString("description", "master datastore");
        tp.setString("driver", nativeDriver);
        tp.setString("url", rwURL);
        tp.setString("role", "master");
        tp.setString("precedence", "1");
        tp.setString("isAvailable", "true");
        FileOutputStream fos1 = new FileOutputStream(masterFile);
        logger.info("Writing out a datastore configuration to '" + masterFile.getAbsolutePath() + "'");
        logger.info("router.properties contains:" + tp);
        tp.store(fos1);
        fos1.close();
        tp.setString("name", "slave");
        tp.setString("description", "slave datastore");
        tp.setString("url", roURL);
        tp.setString("role", "slave");
        FileOutputStream fos2 = new FileOutputStream(slaveFile);
        logger.info("Writing out a datastore configuration to '" + slaveFile.getAbsolutePath() + "'");
        logger.info("router.properties contains:" + tp);
        tp.store(fos2);
        fos2.close();
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

    public String getRouterURL(String qos) {
        return String.format("jdbc:t-router://%s/mydb?qos=%s", getDataServiceName(), qos);
    }
}
