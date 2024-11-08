package test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import com.lutris.appserver.server.*;
import com.lutris.util.Config;
import com.lutris.util.ConfigFile;
import com.lutris.util.ConfigException;
import com.lutris.appserver.server.StandardApplication;
import com.lutris.logging.StandardLogger;
import hambo.util.*;
import hambo.svc.*;
import hambo.messaging.Messaging;

/**
 * An Enhydra application to run the JUnit tests.
 */
class TestApplication extends StandardApplication {

    private static TestApplication singleton = null;

    public static TestApplication getApplication() throws HamboException {
        if (singleton == null) {
            Properties prop = new Properties();
            prop.put("enhydra.config", "enhydratest.conf");
            prop.put("enhydra.logfile", "test.log");
            prop.put("loadOrder", "log database");
            prop.put("service.log.class", "hambo.svc.log.LogServiceManager");
            prop.put("service.log.factory", "hambo.svc.log.enhydra.EnhydraLogServiceFactory");
            prop.put("service.log.id", "our/log");
            prop.put("service.database.class", "hambo.svc.database.DBServiceManager");
            prop.put("service.database.factory", "hambo.svc.database.enhydra.EnhydraDBServiceFactory");
            prop.put("service.database.id", "our/db");
            singleton = new TestApplication(prop);
            Properties mprop = new Properties();
            mprop.put("Manager.class", "hambo.messaging.hambo_db.Manager");
            mprop.put("ms.config", "ms.conf");
            Messaging.init(mprop);
        }
        return singleton;
    }

    public TestApplication(Properties prop) throws HamboException {
        try {
            String configLocation = (String) prop.get("enhydra.config");
            setConfig((new ConfigFile(new File(configLocation))).getConfig());
            String logLocation = (String) prop.get("enhydra.logfile");
            File logFile = new File(logLocation);
            String[] levels = { "HAMBO_ERROR", "HAMBO_INFO", "HAMBO_DEBUG1", "HAMBO_DEBUG2", "HAMBO_DEBUG3" };
            StandardLogger logger = new StandardLogger(true);
            logger.configure(logFile, new String[] {}, levels);
            setLogChannel(logger.getChannel(""));
            startup(getConfig());
            register();
            ServiceManagerLoader loader = new ServiceManagerLoader(prop);
            loader.loadServices();
        } catch (IOException err) {
            throw new HamboException("I/O error in startup", err);
        } catch (ConfigException err) {
            throw new HamboException("Config error in startup", err);
        } catch (ApplicationException err) {
            throw new HamboException("Application error in startup", err);
        }
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    /**
     * Register the current thread with the application of this service. This
     * <em>must</em> be called before each thread uses the service system.
     */
    public void register() {
        Enhydra.register(this);
    }

    /**
     * Register the current thread with the application of this service.  This
     * <em>must</em> be called before a registered thread terminates.
     */
    public static void unRegister() {
        Enhydra.unRegister();
    }
}
