package jabbergame;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.logging.Logger;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;

public class ConnectionManager {

    private final XMPPConnection connection;

    private final URI jid;

    private final String chatHost;

    private static final Random random = new Random();

    private static final Logger logger = Logger.getLogger(ConnectionManager.class.getName());

    public ConnectionManager(URI connectionUri, String chatHost) throws XMPPException, URISyntaxException {
        if (!"xmpp".equals(connectionUri.getScheme())) {
            throw new IllegalArgumentException("Invalid connection URI");
        }
        this.chatHost = chatHost;
        String[] userInfo = connectionUri.getUserInfo().split(":");
        if (userInfo.length != 2) {
            throw new IllegalArgumentException("Invalid connection URI");
        }
        connection = new XMPPConnection(connectionUri.getHost());
        connection.connect();
        connection.login(userInfo[0], userInfo[1]);
        jid = new URI("xmpp://" + connection.getUser());
        if (!jid.getUserInfo().equals(userInfo[0]) || !jid.getHost().equals(connectionUri.getHost())) {
            throw new XMPPException("Bad JID");
        }
    }

    public String getUniqueChatId() {
        String id;
        do {
            id = Long.toHexString(random.nextLong()) + '@' + chatHost;
            try {
                MultiUserChat.getRoomInfo(connection, id);
                logger.info(id);
            } catch (XMPPException e) {
                break;
            }
        } while (true);
        return id;
    }

    public XMPPConnection getXMPPConnection() {
        return connection;
    }

    public URI getJid() {
        return jid;
    }

    public String getChatHost() {
        return chatHost;
    }
}
