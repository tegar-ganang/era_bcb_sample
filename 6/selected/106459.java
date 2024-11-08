package test.mx4j.tools.adaptor.security.interceptor;

import java.net.InetAddress;
import java.security.Security;
import java.util.Arrays;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import junit.framework.TestCase;
import mx4j.adaptor.rmi.jrmp.JRMPAdaptor;
import mx4j.connector.RemoteMBeanServer;
import mx4j.connector.rmi.jrmp.JRMPConnector;
import mx4j.tools.security.cerbero.AuthenticatorTicket;
import mx4j.tools.security.cerbero.CerberoAuthReply;
import mx4j.tools.security.cerbero.CerberoAuthRequest;
import mx4j.tools.security.cerbero.LoginTicket;
import mx4j.tools.security.cerbero.PasswordEncryptedObject;
import mx4j.tools.security.cerbero.ServiceTicket;
import mx4j.tools.adaptor.security.CerberoAdaptorAuthenticator;
import mx4j.tools.adaptor.security.CerberoInvocationContext;

/**
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 481 $
 */
public class CerberoInterceptorTest extends TestCase {

    static {
        System.setProperty("java.security.auth.login.config", "dist/test/cerbero.jaas");
        try {
            Thread.currentThread().getContextClassLoader().loadClass("java.util.logging.Logger");
        } catch (ClassNotFoundException x) {
            Security.addProvider(new com.sun.crypto.provider.SunJCE());
        }
    }

    private MBeanServer m_server;

    private ObjectName m_naming;

    public CerberoInterceptorTest(String s) {
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

    public void testBadUserBadAddressRequest() throws Exception {
        JRMPAdaptor adaptor = new JRMPAdaptor();
        JRMPConnector connector = new JRMPConnector();
        try {
            adaptor.setAuthenticator(new CerberoAdaptorAuthenticator());
            String jndiName = "jrmp";
            adaptor.setJNDIName(jndiName);
            adaptor.setMBeanServer(m_server);
            adaptor.start();
            connector.connect(jndiName, null);
            RemoteMBeanServer server = connector.getRemoteMBeanServer();
            CerberoAuthRequest badRequest = new CerberoAuthRequest(null, null);
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
            adaptor.setAuthenticator(new CerberoAdaptorAuthenticator());
            String jndiName = "jrmp";
            adaptor.setJNDIName(jndiName);
            adaptor.setMBeanServer(m_server);
            adaptor.start();
            connector.connect(jndiName, null);
            RemoteMBeanServer server = connector.getRemoteMBeanServer();
            CerberoAuthRequest badRequest = new CerberoAuthRequest(null, InetAddress.getLocalHost().getHostAddress());
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

    public void testBadAddressRequest() throws Exception {
        JRMPAdaptor adaptor = new JRMPAdaptor();
        JRMPConnector connector = new JRMPConnector();
        try {
            adaptor.setAuthenticator(new CerberoAdaptorAuthenticator());
            String jndiName = "jrmp";
            adaptor.setJNDIName(jndiName);
            adaptor.setMBeanServer(m_server);
            adaptor.start();
            connector.connect(jndiName, null);
            RemoteMBeanServer server = connector.getRemoteMBeanServer();
            String user = "simon";
            CerberoAuthRequest request = new CerberoAuthRequest(user, null);
            char[] password = user.toCharArray();
            CerberoAuthReply reply = (CerberoAuthReply) connector.login(request);
            PasswordEncryptedObject ticket = reply.getLoginTicket();
            LoginTicket login = (LoginTicket) ticket.decrypt(password);
            char[] key = login.getKey();
            AuthenticatorTicket auth = new AuthenticatorTicket(request.getUser(), request.getAddress(), 15 * 1000);
            PasswordEncryptedObject authenticator = new PasswordEncryptedObject(auth, key);
            ServiceTicket serviceTicket = new ServiceTicket(authenticator, login.getGrantingTicket());
            CerberoInvocationContext context = new CerberoInvocationContext(serviceTicket);
            try {
                connector.logout(context);
                fail("Should not be able to logout, address is null");
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
            adaptor.setAuthenticator(new CerberoAdaptorAuthenticator());
            String jndiName = "jrmp";
            adaptor.setJNDIName(jndiName);
            adaptor.setMBeanServer(m_server);
            adaptor.start();
            connector.connect(jndiName, null);
            RemoteMBeanServer server = connector.getRemoteMBeanServer();
            String user = "simon";
            String address = InetAddress.getLocalHost().getHostAddress();
            char[] password = user.toCharArray();
            CerberoAuthRequest request = new CerberoAuthRequest(user, address);
            CerberoAuthReply reply = (CerberoAuthReply) connector.login(request);
            PasswordEncryptedObject ticket = reply.getLoginTicket();
            LoginTicket login = (LoginTicket) ticket.decrypt(password);
            char[] key = login.getKey();
            AuthenticatorTicket auth = new AuthenticatorTicket(null, address, 15 * 1000);
            PasswordEncryptedObject authenticator = new PasswordEncryptedObject(auth, key);
            ServiceTicket serviceTicket = new ServiceTicket(authenticator, login.getGrantingTicket());
            CerberoInvocationContext context = new CerberoInvocationContext(serviceTicket);
            connector.setInvocationContext(context);
            try {
                server.getAttribute(new ObjectName("JMImplementation:type=MBeanServerDelegate"), "MBeanServerId");
                fail("Should not be able to call with bad user invocation context");
            } catch (SecurityException x) {
            } finally {
                try {
                    connector.logout(context);
                    fail("Should not be able to logout");
                } catch (SecurityException x) {
                }
                auth = new AuthenticatorTicket(user, address, 15 * 1000);
                authenticator = new PasswordEncryptedObject(auth, key);
                serviceTicket = new ServiceTicket(authenticator, login.getGrantingTicket());
                context = new CerberoInvocationContext(serviceTicket);
                connector.logout(context);
            }
        } finally {
            connector.close();
            adaptor.stop();
        }
    }

    public void testBadAddressContext() throws Exception {
        JRMPAdaptor adaptor = new JRMPAdaptor();
        JRMPConnector connector = new JRMPConnector();
        try {
            adaptor.setAuthenticator(new CerberoAdaptorAuthenticator());
            String jndiName = "jrmp";
            adaptor.setJNDIName(jndiName);
            adaptor.setMBeanServer(m_server);
            adaptor.start();
            connector.connect(jndiName, null);
            RemoteMBeanServer server = connector.getRemoteMBeanServer();
            String user = "simon";
            String address = InetAddress.getLocalHost().getHostAddress();
            char[] password = user.toCharArray();
            CerberoAuthRequest request = new CerberoAuthRequest(user, address);
            CerberoAuthReply reply = (CerberoAuthReply) connector.login(request);
            PasswordEncryptedObject ticket = reply.getLoginTicket();
            LoginTicket login = (LoginTicket) ticket.decrypt(password);
            char[] key = login.getKey();
            AuthenticatorTicket auth = new AuthenticatorTicket(user, null, 15 * 1000);
            PasswordEncryptedObject authenticator = new PasswordEncryptedObject(auth, key);
            ServiceTicket serviceTicket = new ServiceTicket(authenticator, login.getGrantingTicket());
            CerberoInvocationContext context = new CerberoInvocationContext(serviceTicket);
            connector.setInvocationContext(context);
            try {
                server.getAttribute(new ObjectName("JMImplementation:type=MBeanServerDelegate"), "MBeanServerId");
                fail("Should not be able to call with bad user invocation context");
            } catch (SecurityException x) {
            } finally {
                auth = new AuthenticatorTicket(user, address, 15 * 1000);
                authenticator = new PasswordEncryptedObject(auth, key);
                serviceTicket = new ServiceTicket(authenticator, login.getGrantingTicket());
                context = new CerberoInvocationContext(serviceTicket);
                connector.logout(context);
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
            adaptor.setAuthenticator(new CerberoAdaptorAuthenticator());
            String jndiName = "jrmp";
            adaptor.setJNDIName(jndiName);
            adaptor.setMBeanServer(m_server);
            adaptor.start();
            connector.connect(jndiName, null);
            RemoteMBeanServer server = connector.getRemoteMBeanServer();
            String user = "simon";
            String address = InetAddress.getLocalHost().getHostAddress();
            char[] password = user.toCharArray();
            CerberoAuthRequest request = new CerberoAuthRequest(user, address);
            CerberoAuthReply reply = (CerberoAuthReply) connector.login(request);
            PasswordEncryptedObject ticket = reply.getLoginTicket();
            LoginTicket login = (LoginTicket) ticket.decrypt(password);
            char[] key = login.getKey();
            AuthenticatorTicket auth = new AuthenticatorTicket(user, address, 15 * 1000);
            PasswordEncryptedObject authenticator = new PasswordEncryptedObject(auth, key);
            ServiceTicket serviceTicket = new ServiceTicket(authenticator, login.getGrantingTicket());
            CerberoInvocationContext context = new CerberoInvocationContext(serviceTicket);
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
            adaptor.setAuthenticator(new CerberoAdaptorAuthenticator());
            String jndiName = "jrmp";
            adaptor.setJNDIName(jndiName);
            adaptor.setMBeanServer(m_server);
            adaptor.start();
            connector.connect(jndiName, null);
            RemoteMBeanServer server = connector.getRemoteMBeanServer();
            String user = "simon";
            String address = InetAddress.getLocalHost().getHostAddress();
            char[] password = user.toCharArray();
            CerberoAuthRequest request = new CerberoAuthRequest(user, address);
            CerberoAuthReply reply = (CerberoAuthReply) connector.login(request);
            PasswordEncryptedObject ticket = reply.getLoginTicket();
            LoginTicket login = (LoginTicket) ticket.decrypt(password);
            char[] key = login.getKey();
            AuthenticatorTicket auth = new AuthenticatorTicket(user, address, 15 * 1000);
            PasswordEncryptedObject authenticator = new PasswordEncryptedObject(auth, key);
            ServiceTicket serviceTicket = new ServiceTicket(authenticator, login.getGrantingTicket());
            CerberoInvocationContext context = new CerberoInvocationContext(serviceTicket);
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

    public void testTwoLoginOneLogout() throws Exception {
        JRMPAdaptor adaptor = new JRMPAdaptor();
        JRMPConnector connector = new JRMPConnector();
        try {
            adaptor.setAuthenticator(new CerberoAdaptorAuthenticator());
            String jndiName = "jrmp";
            adaptor.setJNDIName(jndiName);
            adaptor.setMBeanServer(m_server);
            adaptor.start();
            connector.connect(jndiName, null);
            String user = "simon";
            String address = InetAddress.getLocalHost().getHostAddress();
            char[] password = user.toCharArray();
            CerberoAuthRequest request = new CerberoAuthRequest(user, address);
            CerberoAuthReply reply = (CerberoAuthReply) connector.login(request);
            String user2 = "cerbero";
            char[] password2 = user2.toCharArray();
            CerberoAuthRequest request2 = new CerberoAuthRequest(user2, address);
            CerberoAuthReply reply2 = (CerberoAuthReply) connector.login(request2);
            PasswordEncryptedObject ticket2 = reply2.getLoginTicket();
            LoginTicket login2 = (LoginTicket) ticket2.decrypt(password2);
            char[] key2 = login2.getKey();
            AuthenticatorTicket auth2 = new AuthenticatorTicket(user2, address, 15 * 1000);
            PasswordEncryptedObject authenticator2 = new PasswordEncryptedObject(auth2, key2);
            ServiceTicket serviceTicket2 = new ServiceTicket(authenticator2, login2.getGrantingTicket());
            CerberoInvocationContext context2 = new CerberoInvocationContext(serviceTicket2);
            connector.logout(context2);
            RemoteMBeanServer server = connector.getRemoteMBeanServer();
            PasswordEncryptedObject ticket = reply.getLoginTicket();
            LoginTicket login = (LoginTicket) ticket.decrypt(password);
            char[] key = login.getKey();
            AuthenticatorTicket auth = new AuthenticatorTicket(user, address, 15 * 1000);
            PasswordEncryptedObject authenticator = new PasswordEncryptedObject(auth, key);
            ServiceTicket serviceTicket = new ServiceTicket(authenticator, login.getGrantingTicket());
            CerberoInvocationContext context = new CerberoInvocationContext(serviceTicket);
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
