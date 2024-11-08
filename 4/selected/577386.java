package org.opennms.netmgt.vmmgr;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import org.apache.log4j.Category;
import org.opennms.core.utils.ThreadCategory;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class NewManager {

    /**
     * The log4j category used to log debug messsages and statements.
     */
    private static final String LOG4J_CATEGORY = "OpenNMS.Manager";

    public static void main(String[] args) throws Exception {
        NewManager mgr = new NewManager();
        mgr.doMain(args);
    }

    void doMain(String[] args) throws Exception {
        ThreadCategory.setPrefix(LOG4J_CATEGORY);
        if (args.length == 0 || "start".equals(args[0])) {
            startServer();
        } else if (args.length != 0 && "stop".equals(args[0])) {
            stopServer();
        } else if (args.length != 0 && "status".equals(args[0])) {
            statusOfServer();
        }
    }

    private void startServer() throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        mx4j.log.Log.redirectTo(new mx4j.log.Log4JLogger());
        String appContext = System.getProperty("opennms.appcontext", "opennms-appContext.xml");
        FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(appContext);
    }

    private void statusOfServer() {
        invokeCmd("status");
    }

    private void stopServer() {
        invokeCmd("stop");
    }

    private void invokeCmd(String cmd) {
        Category log = ThreadCategory.getInstance(Manager.class);
        try {
            URL invoke = new URL("http://127.0.0.1:8181/invoke?objectname=OpenNMS%3AName=FastExit&operation=" + cmd);
            InputStream in = invoke.openStream();
            int ch;
            while ((ch = in.read()) != -1) System.out.write((char) ch);
            in.close();
            System.out.println("");
            System.out.flush();
        } catch (Throwable t) {
            log.error("error invoking " + cmd + " command", t);
        }
    }

    public List status() {
        System.err.println("Status Called");
        return Collections.singletonList("We are here. We are here! WE ARE HERE!");
    }

    public void stop() {
        System.err.println("Stop Called!");
    }

    public void doSystemExit() {
    }
}
