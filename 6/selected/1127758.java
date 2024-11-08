package ds.asterisk.incubation;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

/**
 * Sendet eine Nachricht und ermöglicht das Empfangen von Nachrichten. Wenn die
 * übertagene Nachricht == "quit" ist, wird die Verbindung beendet.
 * 
 * @author David Strohmayer
 */
public class SendMessageTest {

    private XMPPConnection connection;

    public static void main(String[] args) {
        new SendMessageTest();
    }

    public SendMessageTest() {
        connect();
        createChat();
    }

    private void connect() {
        ConnectionConfiguration config = new ConnectionConfiguration("jabber.ccc.de", 5222);
        config.setSASLAuthenticationEnabled(true);
        connection = new XMPPConnection(config);
        try {
            System.out.println("Connecting…");
            connection.connect();
            SASLAuthentication.supportSASLMechanism("PLAIN", 0);
            System.out.println("Logging in...");
            connection.login("david.strohmayer", "ramodroll", "ECLIPSE");
        } catch (XMPPException e) {
            e.printStackTrace();
        }
    }

    private void createChat() {
        ChatManager chatmanager = connection.getChatManager();
        Chat chat = chatmanager.createChat("david.strohmayer@jabber.ccc.de", new MessageListener() {

            public void processMessage(Chat chat, Message message) {
                if (message.getBody().equalsIgnoreCase("quit")) {
                    System.out.println("Disconnecting...");
                    connection.disconnect();
                } else {
                    System.out.println("Received message: " + message.getBody());
                }
            }
        });
        try {
            System.out.println("Sending Message...");
            chat.sendMessage("Zefix!");
            while (true) {
                ;
            }
        } catch (XMPPException e) {
            System.err.println("Error delivering message...");
        }
    }
}
