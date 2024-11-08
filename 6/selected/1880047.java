package test.mx4j.tools.adaptor.security.interceptor;

import java.util.HashMap;
import java.rmi.RemoteException;
import java.net.InetAddress;
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
import mx4j.tools.adaptor.security.CerberoInvocationContext;
import mx4j.tools.adaptor.security.CerberoAdaptorAuthenticator;
import mx4j.connector.rmi.jrmp.JRMPConnector;
import mx4j.connector.RemoteMBeanServer;
import mx4j.tools.security.cerbero.ServiceTicket;
import mx4j.tools.security.cerbero.PasswordEncryptedObject;
import mx4j.tools.security.cerbero.AuthenticatorTicket;
import mx4j.tools.security.cerbero.LoginTicket;
import mx4j.tools.security.cerbero.CerberoAuthReply;
import mx4j.tools.security.cerbero.CerberoAuthRequest;

/**
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 481 $
 */
public class SecureCerberoInterceptorTest extends TestCase {

    static {
        System.setProperty("java.security.policy", "=dist/test/java.policy");
        System.setProperty("java.security.auth.policy", "=dist/test/jaas.policy");
        System.setProperty("java.security.auth.login.config", "dist/test/cerbero.jaas");
        System.setSecurityManager(new SecurityManager());
    }

    private MBeanServer m_server;

    private ObjectName m_naming;

    public SecureCerberoInterceptorTest(String s) {
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

    public void testNotSuccesfulSecureCall() throws Exception {
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
            String user = "guest";
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
