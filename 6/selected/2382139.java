package amoeba.net;

import java.util.HashMap;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import amoeba.data.Consts;
import amoeba.data.DataFinder;
import amoeba.data.StorageGraph;
import amoeba.data.StorageIdentity;
import amoeba.ui.ConversationWindow;
import amoeba.util.AmoebaExceptionHandler;

/**
 * A class to manage a single connection, and it's associated chats. 
 * 
 * @author Shawn Lassiter
 * 
 */
public class ConnectionManager implements DataFinder.RosterSource, DataFinder.ConnectionManagerSource {

    private ConnectionConfiguration _config;

    private XMPPConnection _connection;

    private Roster _roster;

    private ChatManager _chatmanager;

    private AmoebaMessageListener _listener;

    private HashMap<String, Chat> _chats;

    private StorageIdentity _user;

    private String _server;

    private int _port;

    /**
	 * 
	 * @param user a {@link StorageIdentity} representing the user account. The new ConnectionManager stores a reference to this object. 
	 * @param port - the port on which the server accepts connections

	 */
    public ConnectionManager(StorageIdentity user, int port) {
        _user = user;
        _chats = new HashMap<String, Chat>();
        _server = user.get_server();
        _port = port;
    }

    public ConnectionManager() {
        _user = DataFinder.get_current_user();
        _chats = new HashMap<String, Chat>();
        _server = _user.get_server();
        _port = Consts.DEFAULT_PORT;
    }

    public void sendGraph(StorageGraph graph, String jid) {
        try {
            Chat chat = _chats.get(jid);
            if (chat == null) {
                chat = createChat(jid);
            }
            Message m = new Message();
            GraphExtension ext = new GraphExtension(graph);
            m.addBody("english", "testing");
            m.addExtension(ext);
            chat.sendMessage(m);
        } catch (XMPPException e) {
            AmoebaExceptionHandler.handle(e);
        }
    }

    /**
	 * Connect to the XMPP server
	 */
    public void connect() throws XMPPException {
        _config = new ConnectionConfiguration(_server, _port);
        _connection = new XMPPConnection(_config);
        _connection.connect();
        _connection.login(_user.get_nickname(), _user.get_password());
        set_roster(_connection.getRoster());
        _chatmanager = _connection.getChatManager();
        createMessageListener();
    }

    /**
	 * Disconnect from the XMPP Server
	 */
    public void disconnect() {
        _connection.disconnect();
        _config = null;
        _chatmanager = null;
        _config = null;
    }

    /**
	 * returns true if the connection is talking to a server
	 */
    @Override
    public boolean isConnected() {
        if (_connection == null) return false;
        return _connection.isConnected();
    }

    public Chat createChat(String jid) {
        Chat chat = _chatmanager.createChat(jid, _listener);
        _chats.put(jid, chat);
        return chat;
    }

    public void createMessageListener() {
        PacketTypeFilter filter = new PacketTypeFilter(Message.class);
        AmoebaMessageListener listener = new AmoebaMessageListener();
        _connection.addPacketListener(listener, filter);
    }

    /**
	 * Creates a new user on the server based on the StorageIdentity given.
	 * @param user - A StorageIdentity that defines the user. 
	 * @return - true if successful 
	 */
    public boolean createUser(StorageIdentity user) {
        return createUser(user.get_server(), user.get_nickname(), user.get_password());
    }

    /**
	 * Creates a new user on the server based on information given 
	 * @param server - the server that the user should be created on
	 * @param nick - user nick name
	 * @param password - password for the new user
	 * @return true if successful 
	 */
    public boolean createUser(String server, String nick, String password) {
        XMPPConnection conn = new XMPPConnection(server);
        try {
            conn.connect();
            AccountManager aMgr = new AccountManager(conn);
            if (!aMgr.supportsAccountCreation()) return false;
            aMgr.createAccount(nick, password);
        } catch (XMPPException e) {
            e.printStackTrace();
            return false;
        } finally {
            conn.disconnect();
        }
        return true;
    }

    /**
	 * Deletes a user from the server
	 * @param user - the user to delete
	 * @return true if successful
	 */
    public boolean deleteUser(StorageIdentity user) {
        return deleteUser(user.get_server(), user.get_jid(), user.get_password());
    }

    public boolean deleteUser(String server, String jid, String password) {
        XMPPConnection conn = new XMPPConnection(server);
        try {
            conn.connect();
            conn.login(jid, password);
            AccountManager aMgr = new AccountManager(conn);
            aMgr.deleteAccount();
            conn.disconnect();
        } catch (XMPPException e2) {
            return false;
        } finally {
            conn.disconnect();
        }
        return true;
    }

    /**
	 * Handles incoming chat graphs
	 */
    @Override
    public void incomingGraph(StorageGraph graph, String user) {
        ConversationWindow.haveConversationWith(user, graph);
    }

    public void set_roster(Roster _roster) {
        this._roster = _roster;
    }

    public Roster get_roster() {
        return _roster;
    }

    public StorageIdentity get_user() {
        return _user;
    }

    public void set_user(StorageIdentity user) {
        this._user = user;
    }

    public String get_server() {
        return _server;
    }

    public void set_server(String server) {
        this._server = server;
    }

    public int get_port() {
        return _port;
    }

    public void set_port(int port) {
        this._port = port;
    }
}
