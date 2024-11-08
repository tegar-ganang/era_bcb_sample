package test.mx4j.tools.adaptor.security.interceptor;

import java.util.HashMap;
import java.rmi.RemoteException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
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
public class UserPasswordInterceptorTest extends TestCase {

    private MBeanServer m_server;

    private ObjectName m_naming;

    public UserPasswordInterceptorTest(String s) {
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

    public void testBadUserBadPasswordRequest() throws Exception {
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
            UserPasswordAuthRequest badRequest = new UserPasswordAuthRequest(null, null);
            try {
                connector.login(badRequest);
                fail("Should not login with null user");
            } catch (SecurityException x) {
            }
        } finally {
            connector.close();
            adaptor.stop();
        }
    }

    public void testBadUserRequest() throws Exception {
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
            UserPasswordAuthRequest badRequest = new UserPasswordAuthRequest(null, password);
            try {
                connector.login(badRequest);
                fail("Should not login with null user");
            } catch (SecurityException x) {
            }
        } finally {
            connector.close();
            adaptor.stop();
        }
    }

    public void testBadPasswordRequest() throws Exception {
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
            UserPasswordAuthRequest badRequest = new UserPasswordAuthRequest(user, null);
            try {
                connector.login(badRequest);
                fail("Should not login with null password");
            } catch (SecurityException x) {
            }
        } finally {
            connector.close();
            adaptor.stop();
        }
    }

    public void testBadUserContext() throws Exception {
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
            UserPasswordAuthReply reply1 = (UserPasswordAuthReply) connector.login(request);
            UserPasswordInvocationContext context1 = new UserPasswordInvocationContext(reply1);
            UserPasswordAuthReply reply2 = new UserPasswordAuthReply(null, password);
            UserPasswordInvocationContext context2 = new UserPasswordInvocationContext(reply2);
            connector.setInvocationContext(context2);
            try {
                server.getAttribute(new ObjectName("JMImplementation:type=MBeanServerDelegate"), "MBeanServerId");
                fail("Should not be able to call with bad user invocation context");
            } catch (SecurityException x) {
            } finally {
                try {
                    connector.logout(context2);
                    fail("Should not be able to logout");
                } catch (SecurityException x) {
                }
                connector.logout(context1);
            }
        } finally {
            connector.close();
            adaptor.stop();
        }
    }

    public void testBadPasswordContext() throws Exception {
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
            UserPasswordAuthReply reply1 = (UserPasswordAuthReply) connector.login(request);
            UserPasswordInvocationContext context1 = new UserPasswordInvocationContext(reply1);
            UserPasswordAuthReply reply2 = new UserPasswordAuthReply(user, null);
            UserPasswordInvocationContext context2 = new UserPasswordInvocationContext(reply2);
            connector.setInvocationContext(context2);
            try {
                server.getAttribute(new ObjectName("JMImplementation:type=MBeanServerDelegate"), "MBeanServerId");
                fail("Should not be able to call with bad password invocation context");
            } catch (SecurityException x) {
            } finally {
                try {
                    connector.logout(context2);
                    fail("Should not be able to logout");
                } catch (SecurityException c) {
                }
                connector.logout(context1);
            }
        } finally {
            connector.close();
            adaptor.stop();
        }
    }

    public void testLoggedOutCall() throws Exception {
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
            connector.logout(context);
            try {
                server.getAttribute(new ObjectName("JMImplementation:type=MBeanServerDelegate"), "MBeanServerId");
                fail("Should not be able to call after logging out");
            } catch (SecurityException x) {
            }
        } finally {
            connector.close();
            adaptor.stop();
        }
    }

    public void testCall() throws Exception {
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
                server.getAttribute(new ObjectName("JMImplementation:type=MBeanServerDelegate"), "MBeanServerId");
            } finally {
                connector.logout(context);
            }
        } finally {
            connector.close();
            adaptor.stop();
        }
    }
}
