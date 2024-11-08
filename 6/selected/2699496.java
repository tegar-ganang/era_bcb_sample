package test;

import junit.framework.Assert;
import org.jivesoftware.smack.MessageListener;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import control.base.xmpp.XMPPConnectionService;
import control.top.MessageService;

@RunWith(JMock.class)
public class MessageSenderTest {

    static MessageService service;

    static XMPPConnectionService connection;

    static Mockery context;

    @BeforeClass
    public static void messageSenderInitiation() throws Exception {
        connection = new XMPPConnectionService();
        try {
            connection.connect(TestAccount.server, TestAccount.port);
            connection.login(TestAccount.username, TestAccount.password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        service = connection.chat("gustavo.cpii.ufrj@gmail.com");
        context = new JUnit4Mockery();
    }

    @Test
    public void testSend() throws Exception {
        boolean exceptionRaised = false;
        MessageListener listener = context.mock(MessageListener.class);
        service.createChat(listener);
        try {
            service.send("Cante 'Hare Krishna' e seja feliz!");
        } catch (Exception e) {
            exceptionRaised = true;
        }
        Assert.assertFalse(exceptionRaised);
    }
}
