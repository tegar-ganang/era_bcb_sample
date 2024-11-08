package ajim;

import java.util.*;
import org.jivesoftware.smack.*;

/**
 *
 * @author Kris
 */
public class Model {

    private XMPPConnection connection;

    private String uzenet;

    public void login(String userName, String password) throws XMPPException {
        ConnectionConfiguration config = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
        connection = new XMPPConnection(config);
        connection.connect();
        connection.login(userName, password, "AJIM");
    }

    public void sendMessage(String message, String to, MessageListener msgl) throws XMPPException {
        Chat chat = connection.getChatManager().createChat(to, msgl);
        chat.sendMessage(message);
    }

    public Roster getRoster() {
        return connection.getRoster();
    }

    public void disconnect() {
        connection.disconnect();
    }

    public String getUzenet() {
        return uzenet;
    }
}
