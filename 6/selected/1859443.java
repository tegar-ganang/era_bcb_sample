package test;

import junit.framework.Assert;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import control.base.xmpp.XMPPConnectionService;
import control.top.AccountService;
import control.top.PresenceListener;
import control.top.PresenceService;

@RunWith(JMock.class)
public class AccountManagerServiceTest {

    static Mockery context;

    static AccountService service;

    static XMPPConnectionService connection;

    @BeforeClass
    public static void accountInitiation() throws Exception {
        context = new JUnit4Mockery();
        connection = new XMPPConnectionService();
        connection.connect("jabber.org");
        service = connection.getAccountManagerService();
    }

    @Test
    public void changePasswordTest() throws Exception {
        connection.login("accountManager@jabber.org", "test");
        service.changePassword("password");
        connection.disconnect();
        connection.connect("jabber.org");
        service = connection.getAccountManagerService();
        final PresenceListener listener = context.mock(PresenceListener.class);
        try {
            PresenceService presence = connection.login("accountManager@jabber.org", "password");
            presence.addPresenceListener(listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
        context.checking(new Expectations() {

            {
                allowing(listener).updateEvent(with(any(String.class)), with(any(String.class)));
            }
        });
        Assert.assertTrue(connection.isAuthenticated());
        service.changePassword("test");
        connection.disconnect();
        connection.connect("jabber.org");
        service = connection.getAccountManagerService();
        try {
            PresenceService presence = connection.login("accountManager@jabber.org", "test");
            presence.addPresenceListener(listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
        context.checking(new Expectations() {

            {
                allowing(listener).updateEvent(with(any(String.class)), with(any(String.class)));
            }
        });
        Assert.assertTrue(connection.isAuthenticated());
    }
}
