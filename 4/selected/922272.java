package hambo.svc.testdatabase;

import java.io.*;
import java.sql.*;
import java.util.*;
import com.lutris.util.*;
import com.lutris.logging.*;
import com.lutris.appserver.server.*;
import junit.framework.*;
import hambo.svc.*;
import hambo.svc.log.*;
import hambo.svc.database.*;

public class DBTest extends TestCase {

    private static boolean isInitiated = false;

    public DBTest(String name) {
        super(name);
        if (!isInitiated) {
            isInitiated = true;
            try {
                MyApplication app = new MyApplication();
                File logFile = new File("test.log");
                String[] levels = { "HAMBO_ERROR", "HAMBO_INFO", "HAMBO_DEBUG1", "HAMBO_DEBUG2", "HAMBO_DEBUG3" };
                StandardLogger logger = new StandardLogger(true);
                logger.configure(logFile, new String[] {}, levels);
                app.setConfig((new ConfigFile(new File("test/Test.conf"))).getConfig());
                app.setLogChannel(logger.getChannel(""));
                app.startup(app.getConfig());
                Enhydra.register(app);
                Properties prop = new Properties();
                prop.put("loadOrder", "log database");
                prop.put("service.log.class", "hambo.svc.log.LogServiceManager");
                prop.put("service.log.factory", "hambo.svc.log.enhydra.EnhydraLogServiceFactory");
                prop.put("service.log.id", "our/log");
                prop.put("service.database.class", "hambo.svc.database.DBServiceManager");
                prop.put("service.database.factory", "hambo.svc.database.enhydra.EnhydraDBServiceFactory");
                prop.put("service.database.id", "our/db");
                ServiceManagerLoader loader = new ServiceManagerLoader(prop);
                loader.loadServices();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void setUp() {
    }

    public void testDB() {
        try {
            DBService svc = (DBService) ServiceManager.lookupService("database", DBService.class, new ClientIdentity("DBTest"));
            DBConnection conn = svc.allocateConnection();
            ResultSet rs2 = conn.executeQuery("select * from sysuser where userid='svante'");
            while (rs2.next()) {
                System.out.println(rs2.getString("userid"));
            }
            conn.reset();
            conn.release();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new DBTest("testDB"));
        return suite;
    }
}

class MyApplication extends StandardApplication {

    public void setConfig(Config config) {
        this.config = config;
    }
}
