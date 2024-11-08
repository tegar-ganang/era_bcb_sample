package net.sourceforge.nconfigurations;

import net.sourceforge.nconfigurations.convert.ConvertStrategy;
import net.sourceforge.nconfigurations.convert.StandardConvertStrategy;
import net.sourceforge.nconfigurations.util.ClasspathResource;
import net.sourceforge.nconfigurations.util.ConnectionProvider;
import net.sourceforge.nconfigurations.util.DriverManagerConnectionProvider;
import net.sourceforge.nconfigurations.util.ByteArrayResource;
import net.sourceforge.nconfigurations.util.Resource;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.text.NumberFormat;

/**
 * @author Petr Novotník
 */
@Test(sequential = true)
public class DbConfigurationTest {

    private static final int N_TRIES_MEASURE_BUILDING = 20;

    private static final String T_UNAME = "_global_";

    private ConnectionProvider connProvider;

    @BeforeClass
    public void setupClass() throws ClassNotFoundException {
        connProvider = new DriverManagerConnectionProvider.Builder().setDriverClass("org.hsqldb.jdbcDriver").setJdbcUrl("jdbc:hsqldb:mem:" + DbConfigurationTest.class.getName()).setUsername("sa").setPassword("").build();
    }

    @AfterClass
    public void tearDownClass() throws SQLException {
        Connection con = connProvider.getConnection();
        Statement stmt = con.createStatement();
        stmt.execute("shutdown");
        stmt.close();
        con.close();
    }

    @Test
    @Parameters({ "dataset-resource-path" })
    public void populateAndRetrieve(final String resourcePath) throws Exception {
        final List<TypedProperty> testData = Collections.unmodifiableList(loadData(resourcePath));
        createDBStructure();
        loadDataIntoDB(testData);
        final Resource resource = toByteArrayResource(new ClasspathResource(resourcePath));
        final ConvertStrategy cStrategy = StandardConvertStrategy.getInstance();
        final ConfigurationTable table = new ConfigurationTable.Builder().setBundleColumnName("_username").build();
        final ConfigurationBuilder dbConfigBuilder = new TableBasedConfigurationBuilder(connProvider, table, cStrategy, "_global_");
        final ConfigurationBuilder tpConfigBuilder = new TypedPropertiesConfigurationBuilder(resource, cStrategy);
        long startTime, endTime;
        startTime = System.nanoTime();
        final Configuration dbConfig = dbConfigBuilder.buildConfiguration(T_UNAME);
        endTime = System.nanoTime();
        System.out.println("building table-based configuration took: " + (endTime - startTime) + " nanos");
        startTime = System.nanoTime();
        final Configuration tpConfig = tpConfigBuilder.buildConfiguration(T_UNAME);
        endTime = System.nanoTime();
        System.out.println("building typed-properties-resource configuration took: " + (endTime - startTime) + " nanos");
        Assert.assertEquals(dbConfig.toProperties().size(), tpConfig.toProperties().size());
        Assert.assertEquals(dbConfig.toProperties(), tpConfig.toProperties());
        for (final TypedProperty tprop : testData) {
            final Key<Object> key = KeyFactory.keyFrom(tprop.getKey(), Object.class);
            Assert.assertNotNull(dbConfig.getValue(key));
            Assert.assertNotNull(tpConfig.getValue(key));
            Assert.assertEquals(dbConfig.getValue(key), tpConfig.getValue(key));
            Assert.assertEquals(tpConfig.getValue(key), dbConfig.getValue(key));
        }
    }

    private void measurePerformance(final ConfigurationBuilder builder, final String name, final int ntries) throws BuilderException {
        long sum = 0;
        for (int i = 0; i < ntries; i++) {
            final long startTime, endTime;
            startTime = System.nanoTime();
            final Configuration objConfig = builder.buildConfiguration(name);
            Assert.assertNotNull(objConfig);
            endTime = System.nanoTime();
            final long ellapsed = (endTime - startTime);
            System.out.println("ellapsed (" + name + "): " + ellapsed);
            sum += ellapsed;
        }
        final NumberFormat nf = NumberFormat.getInstance();
        nf.setGroupingUsed(true);
        System.out.println("building configuration '" + name + "' " + ntries + " times took in average: " + nf.format(sum / ntries) + " nanos");
    }

    private static ByteArrayResource toByteArrayResource(final Resource resource) throws IOException {
        final InputStream is = resource.openAsStream();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] tmp = new byte[8192];
            int read;
            while ((read = is.read(tmp)) != -1) {
                baos.write(tmp, 0, read);
            }
            return new ByteArrayResource(baos.toByteArray(), resource.getName());
        } finally {
            is.close();
        }
    }

    private void dumpDB() throws SQLException {
        Connection con = connProvider.getConnection();
        try {
            Statement stmt = con.createStatement();
            ResultSet rset = stmt.executeQuery("select _key, _type, _username, _value from configurations");
            while (rset.next()) {
                System.out.format("[key: %s; type: %s; username: %s; value: %s%n", rset.getString(1), rset.getString(2), rset.getString(3), rset.getString(4));
            }
            try {
                rset.close();
            } catch (SQLException e) {
            }
            try {
                stmt.close();
            } catch (SQLException e) {
            }
        } finally {
            try {
                con.close();
            } catch (SQLException e) {
            }
        }
    }

    private void createDBStructure() throws SQLException {
        Connection con = connProvider.getConnection();
        try {
            con.setAutoCommit(false);
            Statement stmt = con.createStatement();
            stmt.execute("create table configurations (" + "_key varchar(256) not null," + "_value varchar(1000)," + "_type varchar(32) null," + "_username varchar(256) not null)");
            con.commit();
        } finally {
            con.close();
        }
    }

    private void loadDataIntoDB(List<TypedProperty> initialContent) throws SQLException {
        final Connection con = connProvider.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("insert into configurations" + " (_key, _type, _value, _username)" + " values (?, ?, ?, ?)");
            for (final TypedProperty tprop : initialContent) {
                pstmt.setString(1, tprop.getKey());
                pstmt.setString(2, tprop.getType());
                pstmt.setString(3, tprop.getValue());
                pstmt.setString(4, T_UNAME);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            con.commit();
        } finally {
            con.close();
        }
    }

    private List<TypedProperty> loadData(final String resourcePath) throws Exception {
        final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        Assert.assertNotNull(is);
        try {
            return TypedPropertiesConfigurationBuilder.loadFromXML(is);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
    }
}
