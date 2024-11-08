package xmpp.core;

import java.util.Collection;
import java.util.HashMap;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message.Type;
import xmpp.configuration.ConnectionCredentials;
import xmpp.configuration.RoomCredentials;
import xmpp.listeners.PrivateMessageListener;
import xmpp.messaging.PrivateChatMessage;
import xmpp.messaging.PrivateMessage;
import xmpp.messaging.PublicChatMessage;
import xmpp.messaging.base.ChatMessage;
import xmpp.messaging.base.Message;
import xmpp.processing.IProcessor;

/**
 * Connection to XMPP server. Provides method of creation of {@link Room}
 * instances and stores collection of all rooms associated with this connection
 * 
 * @author tillias
 * 
 */
public class Connection implements IConnection, ITransport {

    /**
     * Creates new instance of connection with empty rooms collection
     * 
     * @param credentials
     *            Credentials which will be used to open connection
     * @param messageProcessor
     *            {@link IProcessor} concrete implementation which will be used
     *            to process {@link PrivateMessage} and
     *            {@link PrivateChatMessage} received from this connection
     * @throws NullPointerException
     *             Thrown if any argument passed to constructor is null pointer
     */
    public Connection(ConnectionCredentials credentials, IProcessor messageProcessor) throws NullPointerException {
        if (credentials == null) throw new NullPointerException("Connection credentials can't be null");
        if (messageProcessor == null) throw new NullPointerException("Message processor can't be null");
        this.credentials = credentials;
        this.messageProcessor = messageProcessor;
        rooms = new HashMap<String, IRoom>();
        conn = createXmppConnection(credentials);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation opens connection to remote xmpp server and starts
     * listening for incoming text messages. Valid packets are sent to
     * underlying {@link IProcessor} of this connection for futher processing.
     * Method blocks until connection process succeeds or fails. If already
     * connected does nothing
     */
    @Override
    public void connect() {
        if (conn == null || !isConnected()) {
            try {
                conn.connect();
                conn.login(credentials.getNick(), credentials.getPassword(), resource);
                conn.addPacketListener(new PrivateMessageListener(this, messageProcessor), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public IRoom createRoom(RoomCredentials roomCredentials) {
        IRoom result = null;
        if (isConnected()) {
            try {
                String roomName = roomCredentials.getRoomName();
                IRoom existingRoom = rooms.get(roomName);
                if (existingRoom == null) {
                    result = new Room(roomCredentials, conn, messageProcessor);
                    rooms.put(roomName, result);
                } else {
                    result = existingRoom;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation closes connection to remote xmpp server. If already
     * disconnected does nothing
     */
    @Override
    public void disconnect() {
        if (isConnected()) {
            Collection<IRoom> roomsCollection = rooms.values();
            for (IRoom room : roomsCollection) {
                room.leave();
            }
            roomsCollection.clear();
            conn.disconnect();
        }
    }

    @Override
    public boolean isConnected() {
        if (conn != null) return conn.isConnected();
        return false;
    }

    @Override
    public IRoom getRoom(String roomName) {
        return rooms.get(roomName);
    }

    @Override
    public IRoom[] getRooms() {
        Collection<IRoom> roomsCollection = rooms.values();
        return roomsCollection.toArray(new IRoom[] {});
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation uses appropriative transporting method according to
     * message type.
     */
    @Override
    public void send(Message msg) {
        if (msg != null) {
            if (msg instanceof PrivateMessage) {
                PrivateMessage privateMessage = (PrivateMessage) msg;
                sendPrivateMessage(privateMessage);
            }
            if (msg instanceof PrivateChatMessage) {
                PrivateChatMessage chatMessage = (PrivateChatMessage) msg;
                sendChatMessage(chatMessage);
            }
            if (msg instanceof PublicChatMessage) {
                PublicChatMessage chatMessage = (PublicChatMessage) msg;
                sendChatMessage(chatMessage);
            }
        }
    }

    private XMPPConnection createXmppConnection(ConnectionCredentials connectionCredentials) {
        ConnectionConfiguration config = new ConnectionConfiguration(connectionCredentials.getServer(), connectionCredentials.getPort());
        return new XMPPConnection(config);
    }

    private void sendPrivateMessage(PrivateMessage msg) {
        try {
            org.jivesoftware.smack.packet.Message outMsg = new org.jivesoftware.smack.packet.Message(msg.getRecipient().getJabberID(), Type.chat);
            outMsg.setBody(msg.getText());
            String sender = credentials.getNick() + '@' + credentials.getServer();
            outMsg.setFrom(sender);
            conn.sendPacket(outMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendChatMessage(ChatMessage msg) {
        try {
            IRoom room = getRoom(msg.getRoomName());
            room.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ConnectionCredentials credentials;

    static final String resource = "Digital";

    XMPPConnection conn;

    IProcessor messageProcessor;

    HashMap<String, IRoom> rooms;
}
