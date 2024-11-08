package net.sf.jmyspaceiml;

import net.sf.jmyspaceiml.packet.InstantMessage;
import net.sf.jmyspaceiml.packet.MediaMessage;
import net.sf.jmyspaceiml.packet.StatusMessage;
import net.sf.jmyspaceiml.packet.ProfileMessage;
import net.sf.jmyspaceiml.packet.ActionMessage;
import net.sf.jmyspaceiml.packet.ErrorMessage;
import net.sf.jmyspaceiml.packet.PacketType;
import junit.framework.TestCase;

public class TestMSIMConnection extends TestCase {

    private static final String GOOD_EMAIL = "";

    private static final String GOOD_PASSWORD = "";

    public void testGoodLogin() {
        MSIMConnection connection = new MSIMConnection();
        connection.connect();
        connection.login(GOOD_EMAIL, GOOD_PASSWORD);
        assertTrue(connection.isConnected());
        connection.disconnect();
        assertFalse(connection.isConnected());
    }

    public void testBadpassword() {
        MSIMConnection connection = new MSIMConnection();
        try {
            connection.connect();
            connection.login(GOOD_EMAIL, "badpassword");
        } catch (MSIMException e) {
            assertEquals("260", e.getMessage());
        }
        assertFalse(connection.isConnected());
    }

    public void testInvalidEmail() {
        MSIMConnection connection = new MSIMConnection();
        try {
            connection.connect();
            connection.login("rtg54@hotmail", GOOD_PASSWORD);
        } catch (MSIMException e) {
            assertEquals("260", e.getMessage());
        }
        assertFalse(connection.isConnected());
    }

    public void testSendMessage() {
        MSIMConnection connection = new MSIMConnection();
        connection.connect();
        connection.login(GOOD_EMAIL, GOOD_PASSWORD);
        assertTrue(connection.isConnected());
        InstantMessage message = new InstantMessage();
        message.setTo("6221");
        message.setBody("boo");
        connection.sendPacket(message);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        connection.disconnect();
        assertFalse(connection.isConnected());
    }

    public void testMessageListenerOne() {
        MSIMConnection connection = new MSIMConnection();
        MessageListener listener = new MessageListener() {

            public void processIncomingMessage(InstantMessage message) {
                assertEquals(PacketType.INSTANT_MESSAGE, message.getType());
                assertEquals("jack", message.getTo());
                assertEquals("boo", message.getBody());
            }

            public void processIncomingMessage(ProfileMessage message) {
            }

            public void processIncomingMessage(StatusMessage message) {
            }

            public void processIncomingMessage(ActionMessage message) {
            }

            public void processIncomingMessage(MediaMessage message) {
            }

            public void processIncomingMessage(ErrorMessage message) {
            }
        };
        assertEquals(0, connection.getMessageListeners().size());
        connection.addMessageListener(listener);
        assertEquals(1, connection.getMessageListeners().size());
        InstantMessage message = new InstantMessage();
        message.setTo("jack");
        message.setBody("boo");
        connection.invokeMessageListener(message);
        connection.removeMessageListener(listener);
        assertEquals(0, connection.getMessageListeners().size());
    }

    public void testMessageListenerTwo() {
        MSIMConnection connection = new MSIMConnection();
        assertEquals(0, connection.getMessageListeners().size());
        MessageListener listener = null;
        connection.addMessageListener(listener);
        assertEquals(0, connection.getMessageListeners().size());
    }

    public void testGetRoster() {
        MSIMConnection connection = new MSIMConnection();
        connection.connect();
        connection.login(GOOD_EMAIL, GOOD_PASSWORD);
        assertTrue(connection.isConnected());
        connection.getContactManager().getContacts();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        connection.disconnect();
        assertFalse(connection.isConnected());
    }
}
