package googletalk;

import javax.swing.JOptionPane;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.packet.Presence.Type;

public class Communicator {

    public static final String XMPP_SERVER = "talk.google.com";

    public static final String XMPP_HOST_NAME = "gmail.com";

    public static final String XMPP_SERVICE_NAME = "gmail.com";

    public static final int PACKET_REPLY_TIMEOUT = 500, DEFAULT_XMPP_SERVER_PORT = 5222;

    XMPPConnection conn;

    Roster buddyList;

    public static String canonicalizeUsername(String username) {
        if (!username.contains("@")) {
            username += "@" + XMPP_SERVICE_NAME;
        }
        return username;
    }

    public Communicator(String username, String password) throws XMPPException {
        this(XMPP_SERVER, DEFAULT_XMPP_SERVER_PORT, username, password);
    }

    public Communicator(String serverAddress, Integer serverPort, String username, String password) throws XMPPException {
        username = canonicalizeUsername(username);
        SmackConfiguration.setPacketReplyTimeout(PACKET_REPLY_TIMEOUT);
        ConnectionConfiguration config = new ConnectionConfiguration(serverAddress, serverPort != null ? serverPort : DEFAULT_XMPP_SERVER_PORT, XMPP_HOST_NAME, ProxyInfo.forDefaultProxy());
        conn = new XMPPConnection(config);
        conn.connect();
        System.out.println("Connected to " + serverAddress + ":" + serverPort);
        conn.login(username, password);
        System.out.println("Logged in as " + username);
        setStatus(true, "ON");
    }

    public void setStatus(boolean available, String status) {
        Presence presence = new Presence(available ? Type.available : Type.unavailable);
        presence.setStatus(status);
        conn.sendPacket(presence);
    }

    public void destroy() throws Exception {
        conn.disconnect();
    }

    public boolean sendMessage(String msgText, String to) throws XMPPException {
        to = canonicalizeUsername(to);
        ChatManager mgr = conn.getChatManager();
        Chat chat = mgr.createChat(to, new MessageListener() {

            public void processMessage(Chat chat, Message msg) {
                System.out.println(msg.getBody());
            }
        });
        Message msg = new Message(msgText, Message.Type.chat);
        chat.sendMessage(msg);
        return true;
    }

    public static void main(String args[]) {
        try {
            Communicator comm = new Communicator("fcube.server", "dnlwm2008");
            comm.sendMessage("Hello. ����", "ppjjuu");
            JOptionPane.showMessageDialog(null, "Close this when you want to quit");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
