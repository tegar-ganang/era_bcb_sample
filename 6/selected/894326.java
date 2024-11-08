package crony.services.xmpp.gtalk;

import crony.services.UserPresence;
import crony.services.xmpp.gtalk.listeners.GtalkPacketListener;
import crony.ui.main_frames.AccountsManagerFrame;
import crony.ui.main_frames.MainFrame;
import javax.swing.JOptionPane;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;

/**
 *
 * @author Abhishek
 */
public class Gtalk {

    private XMPPConnection connection;

    private Roster roster;

    private GtalkService gService;

    private String username;

    private String jid;

    private GtalkChatManager chatManager;

    public Gtalk() {
    }

    public boolean connect(String _username, String password) {
        System.out.println("* * Connecting to server * *");
        XMPPConnection.DEBUG_ENABLED = true;
        ConnectionConfiguration config = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
        config.setCompressionEnabled(true);
        config.setSASLAuthenticationEnabled(false);
        connection = new XMPPConnection(config);
        try {
            connection.connect();
        } catch (XMPPException ex) {
            JOptionPane.showMessageDialog(AccountsManagerFrame.getInstance(), "remote-server-timeout(504) Could not connect to talk.google.com:5222\n" + ex.getMessage(), "Server Not Responding", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            connection.login(_username, password, "CronyIM");
        } catch (XMPPException ex) {
            if (ex.getMessage().equals("forbidden(403) Username or password not correct.")) {
                JOptionPane.showMessageDialog(AccountsManagerFrame.getInstance(), "Invalid User Name or password", "Error Loggin in", JOptionPane.ERROR_MESSAGE);
                return false;
            } else System.out.println(ex);
        }
        chatManager = new GtalkChatManager(connection.getChatManager(), jid);
        connection.addPacketListener(chatManager, new MessageTypeFilter(Message.Type.chat));
        connection.addConnectionListener(new ServerConnectionListener());
        connection.addPacketListener(new GtalkPacketListener(), new PacketTypeFilter(IQ.class));
        roster = connection.getRoster();
        roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
        if (connection.isConnected()) {
            System.out.println("* * Connected * *");
            jid = StringUtils.parseBareAddress(connection.getUser());
            username = StringUtils.parseName(jid);
            gService = new GtalkService(username);
            Thread gServiceThread = new Thread(gService, "Gtalk Service Thread");
            gServiceThread.start();
            return true;
        } else return false;
    }

    public void changePresence(UserPresence av, String newStatMsg) {
        gService.changePresence(av, newStatMsg);
    }

    void sendPacket(Packet p) {
        connection.sendPacket(p);
    }

    XMPPConnection getConnection() {
        return connection;
    }

    Roster getRoster() {
        return roster;
    }

    public String getUsername() {
        return username;
    }

    String getJID() {
        return jid;
    }

    GtalkService getInstanceOfGtalkService() {
        return gService;
    }

    public void disconnect() {
        connection.disconnect();
        MainFrame.getInstance().clearGtalkTree();
        System.out.println("\n* * * *DISCONNECTED FROM THE SERVER * * * *");
    }

    public void initiateChat(String toUser) {
        chatManager.initiateChat(toUser);
    }
}

class ServerConnectionListener implements ConnectionListener {

    @Override
    public void connectionClosed() {
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void reconnectingIn(int seconds) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void reconnectionSuccessful() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void reconnectionFailed(Exception e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
