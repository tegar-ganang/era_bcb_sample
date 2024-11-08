package comm;

import java.util.HashMap;
import java.util.Map;
import main.Main;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

/**
 * Tests the chat functionality.
 *
 * @author Gaston Dombiak
 */
public class ConnectionManager {

    private static final ConnectionManager INSTANCE = new ConnectionManager();

    private Map<String, XMPPConnection> connections = new HashMap<String, XMPPConnection>();

    private String[] authorizedUsers = {};

    private String host = "talk.google.com";

    private int port = 5222;

    private String type = "gmail.com";

    int reconnectTimeout = 30000;

    private Logger logger = Logger.getLogger(ConnectionManager.class);

    private Main main = null;

    /**
     * 
     * @return an INSTANCE of the ConnectionManager singleton
     */
    public static ConnectionManager getInstance() {
        return INSTANCE;
    }

    public void setMain(Main main) {
        this.main = main;
    }

    /**
     * 
     * @param userId - the id of the bot for which the connection is created
     * @param password - the password used to login
     * @throws java.lang.Exception thrown if any errors are encountered when establishing the connection
     */
    public void getConnection(String userId, String password) throws InterruptedException {
        if (null == main) {
            logger.fatal("Must have a reference to Main in order to continue!");
            System.exit(1);
        }
        try {
            host = System.getProperty("HOST");
            port = Integer.parseInt(System.getProperty("PORT").trim());
            type = System.getProperty("TYPE");
            String authorizedUsersString = System.getProperty("AUTHORIZED_USERS");
            if (null != authorizedUsersString) authorizedUsers = authorizedUsersString.split(",");
            reconnectTimeout = Integer.parseInt(System.getProperty("RECONNECT_TIMEOUT").trim()) * 1000;
        } catch (Exception ex) {
            logger.fatal("Failed to load properties: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
        logger.info("Starting IM client");
        main.updateState(Main.State.Connected);
        ConnectionConfiguration connConfig = new ConnectionConfiguration(host, port, type);
        XMPPConnection connection = new XMPPConnection(connConfig);
        try {
            connection.connect();
            logger.info("Connected to " + connection.getHost());
        } catch (Exception ex) {
            logger.error("Failed to connect to " + connection.getHost() + "\n" + ex.getMessage());
            main.updateState(Main.State.Disconnected);
            return;
        }
        try {
            if (userId.contains("@")) userId = userId.substring(0, userId.indexOf("@"));
            connection.login(userId, password);
            logger.info("Logged in as " + connection.getUser());
            Presence presence = new Presence(Presence.Type.available);
            connection.sendPacket(presence);
        } catch (Exception ex) {
            logger.error("Failed to log in as " + connection.getUser() + "\n" + ex.getMessage());
            main.updateState(Main.State.Disconnected);
            return;
        }
        addPacketListener(connection);
        connections.put(userId, connection);
    }

    public void closeConnection(String userId) {
        XMPPConnection connection = connections.get(userId);
        if (null == connection) {
            return;
        }
        if (connection.isConnected()) {
            connection.disconnect();
        }
        main.updateState(Main.State.Disconnected);
    }

    /**
     * 
     * @param userId - checks if the user has any chat connections associated with its id
     * @return true if connection exists
     */
    public boolean isConnected(String userId) {
        XMPPConnection connection = connections.get(userId);
        if (null == connection) {
            return false;
        }
        return connection.isConnected();
    }

    private void addPacketListener(XMPPConnection connection) {
        PacketListener listener = new PacketListener() {

            public void processPacket(Packet packet) {
                if (packet instanceof Message) {
                    Message msg = (Message) packet;
                    if (msg.getType().equals(Message.Type.chat) && msg.getBody() != null) {
                        String remoteUser = msg.getFrom();
                        logger.info("Got message from " + remoteUser + ": " + msg.getBody());
                        if (authorizedUsers.length > 0) {
                            boolean authorized = false;
                            for (String user : authorizedUsers) {
                                if (user.trim().equals(remoteUser.substring(0, remoteUser.indexOf("/")))) {
                                    authorized = true;
                                }
                            }
                            if (!authorized) {
                                logger.warn(remoteUser + " is an unauthorized user\nMessage: " + msg.getBody());
                                return;
                            }
                        }
                        ExecutionThread executor = new ExecutionThread(msg);
                        executor.start();
                    }
                }
            }
        };
        connection.addPacketListener(listener, new PacketTypeFilter(Message.class));
    }

    /**
     * 
     * @param userId - the id of the bot initiating the message
     * @param targetUser - the user to whom the message will be sent
     * @param message - the message which will be sent to the targetUser
     * @throws java.lang.Exception thrown when no connection exists for the user
     */
    public synchronized void sendMessage(String userId, String targetUser, String message) {
        if (userId.contains("@")) userId = userId.substring(0, userId.indexOf("@"));
        XMPPConnection connection = connections.get(userId);
        if (null == connection) {
            logger.error("No connection exist for " + userId);
            main.updateState(Main.State.Disconnected);
            return;
        }
        Message msg = new Message(targetUser, Message.Type.chat);
        msg.setBody(message);
        connection.sendPacket(msg);
    }
}
