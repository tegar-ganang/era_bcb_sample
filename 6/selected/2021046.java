package test.mx4j.tools.adaptor.security.interceptor;

import java.util.HashMap;
import java.rmi.RemoteException;
import javax.management.ObjectName;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MBeanException;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.ReflectionException;
import javax.management.MalformedObjectNameException;
import junit.framework.TestCase;
import mx4j.adaptor.rmi.jrmp.JRMPAdaptor;
import mx4j.tools.adaptor.security.UserPasswordAdaptorAuthenticator;
import mx4j.tools.adaptor.security.UserPasswordInvocationContext;
import mx4j.connector.rmi.jrmp.JRMPConnector;
import mx4j.connector.RemoteMBeanServer;
import mx4j.tools.security.password.UserPasswordAuthRequest;
import mx4j.tools.security.password.UserPasswordAuthReply;

/**
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 481 $
 */
public class SecureUserPasswordInterceptorTest extends TestCase {

    static {
        System.setProperty("java.security.policy", "=dist/test/java.policy");
        System.setProperty("java.security.auth.policy", "=dist/test/jaas.policy");
        System.setSecurityManager(new SecurityManager());
    }

    private MBeanServer m_server;

    private ObjectName m_naming;

    public SecureUserPasswordInterceptorTest(String s) {
        super(s);
    }

    protected void setUp() throws Exception {
        m_server = MBeanServerFactory.createMBeanServer();
        m_naming = new ObjectName("Naming:type=rmiregistry");
        m_server.createMBean("mx4j.tools.naming.NamingService", m_naming, null);
        m_server.invoke(m_naming, "start", null, null);
    }

    protected void tearDown() throws Exception {
        m_server.invoke(m_naming, "stop", null, null);
        m_server.unregisterMBean(m_naming);
        MBeanServerFactory.releaseMBeanServer(m_server);
    }

    public void testSuccesfulSecureCall() throws Exception {
        JRMPAdaptor adaptor = new JRMPAdaptor();
        JRMPConnector connector = new JRMPConnector();
        try {
            HashMap map = new HashMap();
            String user = "simon";
            char[] password = user.toCharArray();
            map.put(user, password);
            adaptor.setAuthenticator(new UserPasswordAdaptorAuthenticator(map));
            String jndiName = "jrmp";
            adaptor.setJNDIName(jndiName);
            adaptor.setMBeanServer(m_server);
            adaptor.start();
            connector.connect(jndiName, null);
            RemoteMBeanServer server = connector.getRemoteMBeanServer();
            UserPasswordAuthRequest request = new UserPasswordAuthRequest(user, password);
            UserPasswordAuthReply reply = (UserPasswordAuthReply) connector.login(request);
            UserPasswordInvocationContext context = new UserPasswordInvocationContext(reply);
            connector.setInvocationContext(context);
            try {
                String id = (String) server.getAttribute(new ObjectName("JMImplementation:type=MBeanServerDelegate"), "MBeanServerId");
                System.out.println("ID = " + id);
            } finally {
                connector.logout(context);
            }
        } finally {
            connector.close();
            adaptor.stop();
        }
    }

    public void testNotSuccesfulSecureCall() throws Exception {
        JRMPAdaptor adaptor = new JRMPAdaptor();
        JRMPConnector connector = new JRMPConnector();
        try {
            HashMap map = new HashMap();
            String user = "simon";
            char[] password = user.toCharArray();
            map.put(user, password);
            String anotherUser = "another";
            map.put(anotherUser, password);
            adaptor.setAuthenticator(new UserPasswordAdaptorAuthenticator(map));
            String jndiName = "jrmp";
            adaptor.setJNDIName(jndiName);
            adaptor.setMBeanServer(m_server);
            adaptor.start();
            connector.connect(jndiName, null);
            RemoteMBeanServer server = connector.getRemoteMBeanServer();
            UserPasswordAuthRequest request = new UserPasswordAuthRequest(anotherUser, password);
            UserPasswordAuthReply reply = (UserPasswordAuthReply) connector.login(request);
            UserPasswordInvocationContext context = new UserPasswordInvocationContext(reply);
            connector.setInvocationContext(context);
            try {
                String id = (String) server.getAttribute(new ObjectName("JMImplementation:type=MBeanServerDelegate"), "MBeanServerId");
                fail("User has not the right permissions");
            } catch (SecurityException x) {
            } finally {
                connector.logout(context);
            }
        } finally {
            connector.close();
            adaptor.stop();
        }
    }
}
