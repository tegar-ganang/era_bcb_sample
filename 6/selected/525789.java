package com.googlecode.xmpplib;

import java.io.IOException;
import junit.framework.Assert;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.junit.Ignore;
import org.junit.Test;
import com.googlecode.xmpplib.provider.impl.SimpleAuthenticationController;

public class SmackTest {

    private static final boolean ENABLE_SMACK_DEBUGGER = Boolean.parseBoolean(System.getProperty("enableSmackDebugger"));

    private static final long SLEEP_AFTER_EACH_TEST = ENABLE_SMACK_DEBUGGER ? 10000L : 0L;

    private static int port = 10000;

    protected XmppFactory createXmppFactory() {
        XmppFactory xmppFactory = new DebugXmppFactory();
        SimpleAuthenticationController authenticationController = (SimpleAuthenticationController) xmppFactory.createAuthenticationController();
        authenticationController.addUser("unittestusername", "unittestpassword");
        return xmppFactory;
    }

    protected XmppServer createXmppServer() throws IOException {
        XmppServer xmppServer = new XmppServer(createXmppFactory());
        xmppServer.setBindIP("127.0.0.1");
        xmppServer.setBindPort(port);
        xmppServer.start();
        return xmppServer;
    }

    protected ConnectionConfiguration createConfiguration() {
        SmackConfiguration.setKeepAliveInterval(1000);
        SmackConfiguration.setPacketReplyTimeout(1000);
        ConnectionConfiguration configuration = new ConnectionConfiguration("127.0.0.1", port, "buenocode.com");
        configuration.setDebuggerEnabled(ENABLE_SMACK_DEBUGGER);
        configuration.setReconnectionAllowed(false);
        configuration.setRosterLoadedAtLogin(false);
        configuration.setSendPresence(false);
        return configuration;
    }

    @Test
    public void testConnection() throws Exception {
        XmppServer xmppServer = createXmppServer();
        try {
            ConnectionConfiguration configuration = createConfiguration();
            configuration.setSASLAuthenticationEnabled(true);
            configuration.setSecurityMode(SecurityMode.enabled);
            XMPPConnection connection = new XMPPConnection(configuration);
            connection.connect();
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            Thread.sleep(SLEEP_AFTER_EACH_TEST);
            xmppServer.shutdown();
            port++;
        }
    }

    @Test
    @Ignore("Not implemented yet")
    public void testLoginWithoutSasl() throws Exception {
        XmppServer xmppServer = createXmppServer();
        try {
            ConnectionConfiguration configuration = createConfiguration();
            configuration.setSASLAuthenticationEnabled(false);
            configuration.setSecurityMode(SecurityMode.enabled);
            XMPPConnection connection = new XMPPConnection(configuration);
            connection.connect();
            connection.login("unittestusername", "unittestpassword");
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            Thread.sleep(SLEEP_AFTER_EACH_TEST);
            xmppServer.shutdown();
            port++;
        }
    }

    @Test
    @Ignore("Not implemented yet")
    public void testWrongLoginWithoutSasl() throws Exception {
        XmppServer xmppServer = createXmppServer();
        try {
            ConnectionConfiguration configuration = createConfiguration();
            configuration.setSASLAuthenticationEnabled(false);
            configuration.setSecurityMode(SecurityMode.enabled);
            XMPPConnection connection = new XMPPConnection(configuration);
            connection.connect();
            connection.login("unittestusername", "wrongpassword");
            Assert.fail("Login with wrong password must fail.");
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            Thread.sleep(SLEEP_AFTER_EACH_TEST);
            xmppServer.shutdown();
            port++;
        }
    }

    @Test
    public void testAnonymousLogin() throws Exception {
        XmppServer xmppServer = createXmppServer();
        xmppServer.setSaslAnonymous(true);
        xmppServer.setSaslPlain(false);
        xmppServer.setSaslCramMd5(false);
        xmppServer.setSaslDigestMd5(false);
        try {
            ConnectionConfiguration configuration = createConfiguration();
            configuration.setSASLAuthenticationEnabled(true);
            configuration.setSecurityMode(SecurityMode.enabled);
            XMPPConnection connection = new XMPPConnection(configuration);
            connection.connect();
            connection.login("unittestusername", "unittestpassword");
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            Thread.sleep(SLEEP_AFTER_EACH_TEST);
            xmppServer.shutdown();
            port++;
        }
    }

    @Test
    @Ignore("Not implemented yet")
    public void testAnonymousWithIsNotAllowed() throws Exception {
        XmppServer xmppServer = createXmppServer();
        xmppServer.setSaslAnonymous(true);
        xmppServer.setSaslPlain(false);
        xmppServer.setSaslCramMd5(false);
        xmppServer.setSaslDigestMd5(false);
        try {
            ConnectionConfiguration configuration = createConfiguration();
            configuration.setSASLAuthenticationEnabled(true);
            configuration.setSecurityMode(SecurityMode.enabled);
            XMPPConnection connection = new XMPPConnection(configuration);
            connection.connect();
            try {
                connection.login("unittestusername", "wrongpassword");
                Assert.fail("Login with wrong password must fail.");
            } catch (XMPPException e) {
                if (!e.toString().equals("SASL authentication failed using mechanism DIGEST-MD5: ")) {
                    throw e;
                }
            }
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            Thread.sleep(SLEEP_AFTER_EACH_TEST);
            xmppServer.shutdown();
            port++;
        }
    }

    @Test
    public void testPlainLogin() throws Exception {
        XmppServer xmppServer = createXmppServer();
        xmppServer.setSaslAnonymous(false);
        xmppServer.setSaslPlain(true);
        xmppServer.setSaslCramMd5(false);
        xmppServer.setSaslDigestMd5(false);
        try {
            ConnectionConfiguration configuration = createConfiguration();
            configuration.setSASLAuthenticationEnabled(true);
            configuration.setSecurityMode(SecurityMode.enabled);
            XMPPConnection connection = new XMPPConnection(configuration);
            connection.connect();
            connection.login("unittestusername", "unittestpassword");
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            Thread.sleep(SLEEP_AFTER_EACH_TEST);
            xmppServer.shutdown();
            port++;
        }
    }

    @Test
    public void testPlainWrongLogin() throws Exception {
        XmppServer xmppServer = createXmppServer();
        xmppServer.setSaslAnonymous(false);
        xmppServer.setSaslPlain(true);
        xmppServer.setSaslCramMd5(false);
        xmppServer.setSaslDigestMd5(false);
        try {
            ConnectionConfiguration configuration = createConfiguration();
            configuration.setSASLAuthenticationEnabled(true);
            configuration.setSecurityMode(SecurityMode.enabled);
            XMPPConnection connection = new XMPPConnection(configuration);
            connection.connect();
            try {
                connection.login("unittestusername", "wrongpassword");
                Assert.fail("Login with wrong password must fail.");
            } catch (XMPPException e) {
                if (!e.toString().equals("SASL authentication failed using mechanism PLAIN: ")) {
                    throw e;
                }
            }
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            Thread.sleep(SLEEP_AFTER_EACH_TEST);
            xmppServer.shutdown();
            port++;
        }
    }

    @Test
    public void testCramMd5Login() throws Exception {
        XmppServer xmppServer = createXmppServer();
        xmppServer.setSaslAnonymous(false);
        xmppServer.setSaslPlain(false);
        xmppServer.setSaslCramMd5(true);
        xmppServer.setSaslDigestMd5(false);
        try {
            ConnectionConfiguration configuration = createConfiguration();
            configuration.setSASLAuthenticationEnabled(true);
            configuration.setSecurityMode(SecurityMode.enabled);
            XMPPConnection connection = new XMPPConnection(configuration);
            connection.connect();
            connection.login("unittestusername", "unittestpassword");
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            Thread.sleep(SLEEP_AFTER_EACH_TEST);
            xmppServer.shutdown();
            port++;
        }
    }

    @Test
    public void testCramMd5WrongLogin() throws Exception {
        XmppServer xmppServer = createXmppServer();
        xmppServer.setSaslAnonymous(false);
        xmppServer.setSaslPlain(false);
        xmppServer.setSaslCramMd5(true);
        xmppServer.setSaslDigestMd5(false);
        try {
            ConnectionConfiguration configuration = createConfiguration();
            configuration.setSASLAuthenticationEnabled(true);
            configuration.setSecurityMode(SecurityMode.enabled);
            XMPPConnection connection = new XMPPConnection(configuration);
            connection.connect();
            try {
                connection.login("unittestusername", "wrongpassword");
                Assert.fail("Login with wrong password must fail.");
            } catch (XMPPException e) {
                if (!e.toString().equals("SASL authentication failed using mechanism CRAM-MD5: ")) {
                    throw e;
                }
            }
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            Thread.sleep(SLEEP_AFTER_EACH_TEST);
            xmppServer.shutdown();
            port++;
        }
    }

    @Test
    public void testDigestMd5Login() throws Exception {
        XmppServer xmppServer = createXmppServer();
        xmppServer.setSaslAnonymous(false);
        xmppServer.setSaslPlain(false);
        xmppServer.setSaslCramMd5(false);
        xmppServer.setSaslDigestMd5(true);
        try {
            ConnectionConfiguration configuration = createConfiguration();
            configuration.setSASLAuthenticationEnabled(true);
            configuration.setSecurityMode(SecurityMode.enabled);
            XMPPConnection connection = new XMPPConnection(configuration);
            connection.connect();
            connection.login("unittestusername", "unittestpassword");
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            Thread.sleep(SLEEP_AFTER_EACH_TEST);
            xmppServer.shutdown();
            port++;
        }
    }

    @Test
    public void testDigestMd5WrongLogin() throws Exception {
        XmppServer xmppServer = createXmppServer();
        xmppServer.setSaslAnonymous(false);
        xmppServer.setSaslPlain(false);
        xmppServer.setSaslCramMd5(false);
        xmppServer.setSaslDigestMd5(true);
        try {
            ConnectionConfiguration configuration = createConfiguration();
            configuration.setSASLAuthenticationEnabled(true);
            configuration.setSecurityMode(SecurityMode.enabled);
            XMPPConnection connection = new XMPPConnection(configuration);
            connection.connect();
            try {
                connection.login("unittestusername", "wrongpassword");
                Assert.fail("Login with wrong password must fail.");
            } catch (XMPPException e) {
                if (!e.toString().equals("SASL authentication failed using mechanism DIGEST-MD5: ")) {
                    throw e;
                }
            }
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            Thread.sleep(SLEEP_AFTER_EACH_TEST);
            xmppServer.shutdown();
            port++;
        }
    }

    @Test
    public void testSimpleTextMessage() throws Exception {
        XmppServer xmppServer = createXmppServer();
        try {
            ConnectionConfiguration configuration = createConfiguration();
            configuration.setSASLAuthenticationEnabled(true);
            configuration.setSecurityMode(SecurityMode.enabled);
            XMPPConnection connection = new XMPPConnection(configuration);
            connection.connect();
            connection.login("unittestusername", "unittestpassword");
            Message message = new Message();
            message.setType(Type.normal);
            message.setThread("thread");
            message.setFrom("from");
            message.setTo("to");
            message.setSubject("Thread subject");
            message.setBody("Just a simple text message.");
            System.out.println(message.toXML());
            connection.sendPacket(message);
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            Thread.sleep(SLEEP_AFTER_EACH_TEST);
            xmppServer.shutdown();
            port++;
        }
    }

    @Ignore
    public void testRoster() throws Exception {
        XmppServer xmppServer = createXmppServer();
        try {
            ConnectionConfiguration configuration = createConfiguration();
            configuration.setSASLAuthenticationEnabled(true);
            configuration.setSecurityMode(SecurityMode.enabled);
            XMPPConnection connection = new XMPPConnection(configuration);
            connection.connect();
            connection.login("unittestusername", "unittestpassword");
            connection.getRoster();
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            Thread.sleep(SLEEP_AFTER_EACH_TEST);
            xmppServer.shutdown();
            port++;
        }
    }
}
