package hanasu.p2p;

import hanasu.encryption.HashPassword;
import hanasu.encryption.RSA;
import hanasu.p2p.UserAccount.OnlineStatus;
import hanasu.tools.ByteBufferUtilities;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import net.jxta.peer.PeerID;

/**
 * @author Marc Miltenberger
 * Handles the connection to the server and performs user account related actions.
 */
public final class ServerConnection {

    private static OnlineStatus lastOnlineStatus;

    /**
	 * The server will listen on this port.
	 */
    public static final int ServerPortListen = 8080;

    /**
	 * The client will connect to this port. 
	 */
    public static final int ServerPortConnect = 80;

    public static final String ServerRelayHostname = "hanasuserver.ffm-mtk.de";

    public static final String ServerHostname = "hanasuserver.ffm-mtk.de";

    /**
	 * The unique id used for communication with the server 
	 */
    public static final String ServerMessageGUID = "884bdaa2-351d-46ee-a82a-15622526d2a6";

    private static boolean connected = false;

    private static Connection connection;

    private static ArrayList<Message> lastMessages = new ArrayList<Message>();

    public enum PasswordChangeResults {

        CHANGESUCCESSFULL, CREDENTIALSWRONG, SERVERCONNECTIONERROR, UNKNOWNERROR
    }

    public enum RegisterResults {

        REGISTERSUCCESSFULL, USERNAMETAKEN, SERVERCONNECTIONERROR, UNKNOWNERROR
    }

    public enum LoginResults {

        LOGINSUCCESSFULL, CREDENTIALSWRONG, SERVERCONNECTIONERROR, UNKNOWNERROR
    }

    public enum UpdateAccountResults {

        UPDATESUCCESSFULL, NOSUFFICIENTRIGHTS, SERVERCONNECTIONERROR, UNKNOWNERROR
    }

    public enum LostPasswordResults {

        MAILSENT, NOMAILADDRESSPROVIDED, NOTFOUND, UNKNOWNERROR, RESETSUCCESSFULLY, WRONGCODE
    }

    static HashMap<String, P2PConnection> connections = new HashMap<String, P2PConnection>();

    static IServerConnection serverConnectionListener;

    static String loginUsername;

    private static String loginPassword;

    /**
	 * Reads all bytes in a stream and returns them
	 * @param is the stream
	 * @return  the byte array containing the stream data
	 * @throws IOException
	 */
    private static byte[] getBytesFromStream(InputStream is) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 512];
        while (true) {
            int read = is.read(buffer);
            if (read == -1) break;
            stream.write(buffer, 0, read);
        }
        is.close();
        return stream.toByteArray();
    }

    /**
	 * Establishes a connection to the default server
	 * @throws Exception 
	 */
    public static void establishConnection() throws Exception {
        if (ServerConnection.serverConnectionListener == null) throw new Exception("Set the server connection listener FIRST!");
        if (connected) return;
        System.out.println("Connecting to server");
        int timeOutMs = 5000;
        int retryTimes = 3;
        RSA rsa = new RSA();
        rsa.generateKey(true);
        System.out.println("Generated RSA key");
        final IConnection cconnection = new IConnection() {

            @Override
            public void messageReceived(Message msg) {
                handleMessageReceived(msg);
            }

            @Override
            public void connectionEstablished(Connection connection) {
                RSAPublicKey b2 = (RSAPublicKey) connection.getForeignPublicKey();
                RSAPublicKey expected = null;
                try {
                    byte[] bPublicKey = getBytesFromStream(ServerConnection.class.getResourceAsStream("hanasukey.public"));
                    expected = (RSAPublicKey) RSA.getPublicKey(bPublicKey);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    return;
                }
                if (!b2.equals(expected)) {
                    if (connection != null) {
                        String connectedWithUsername;
                        try {
                            connectedWithUsername = ServerConnection.getUsername(connection.getEndpoint().getPeerID());
                        } catch (Exception e1) {
                            System.err.println("Corresponding Username to peer id not found");
                            e1.printStackTrace();
                            return;
                        }
                        P2PConnection p2pc;
                        p2pc = connections.get(connectedWithUsername);
                        if (p2pc == null) p2pc = new P2PConnection();
                        try {
                            p2pc.connectionEstablished(connection);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                }
                System.out.println("Connection to server established");
                ServerConnection.connection = connection;
                connected = true;
            }

            @Override
            public synchronized void connectionClosed(Connection connection) {
                if (connection == ServerConnection.connection) {
                    simulateConnectionTimeout();
                }
            }

            @Override
            public void connectionFailed(Connection connection) {
            }
        };
        System.out.println("Creating new network");
        new Network(cconnection, false);
        while (!connected) {
            long started = System.currentTimeMillis();
            while (started + timeOutMs > System.currentTimeMillis() && !connected) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (!connected) {
                if (connection != null) connection.close();
                retryTimes--;
                if (retryTimes == 0) break;
            }
        }
    }

    /**
	 * Sets the server connection listener.
	 * This should be done BEFORE establishConnection is called.
	 * @param serverConnectionListener the used listener
	 */
    public static void setServerConnectionListener(IServerConnection serverConnectionListener) {
        if (ServerConnection.serverConnectionListener == null) ServerConnection.serverConnectionListener = serverConnectionListener;
    }

    /**
	 * Handles a message from the server 
	 * @param msg the message
	 */
    private static void handleMessageReceived(Message msg) {
        if (msg.getGuidIntendedReceiver().equals(ServerMessageGUID)) {
            switch(msg.getMessageType()) {
                case 7:
                    ByteBuffer cbuffer = ByteBuffer.wrap(msg.getMessage());
                    final String username = ByteBufferUtilities.getStringFromByteBuffer(cbuffer);
                    final String peerid = ByteBufferUtilities.getStringFromByteBuffer(cbuffer);
                    System.out.println("Received contact data for " + username);
                    Thread thrConnect = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                System.out.println("Start connecting to " + username + ", " + peerid);
                                Network.connect(peerid);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                    thrConnect.setName("Connect to " + username);
                    thrConnect.setDaemon(true);
                    thrConnect.start();
                    break;
                case 10:
                    try {
                        UserAccount account = UserAccount.loadUserAccount(msg.getMessage());
                        if (loginUsername != null && account.getUsername().toLowerCase().equals(loginUsername.toLowerCase())) loginUsername = account.getUsername();
                        serverConnectionListener.notifyAccountChange(account);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                case 12:
                    ByteBuffer buffer = ByteBuffer.wrap(msg.getMessage());
                    String username2 = ByteBufferUtilities.getStringFromByteBuffer(buffer);
                    String message = ByteBufferUtilities.getStringFromByteBuffer(buffer);
                    serverConnectionListener.youHaveBeenAdded(username2, message);
                    return;
                case 15:
                    buffer = ByteBuffer.wrap(msg.getMessage());
                    long registeredUsers = buffer.getLong();
                    long onlineUsers = buffer.getLong();
                    serverConnectionListener.updatedStatistics(registeredUsers, onlineUsers);
                    return;
            }
        }
        synchronized (lastMessages) {
            lastMessages.add(msg);
        }
    }

    /**
	 * Waits for a response from the server
	 * @param messageType the message type of the raw message
	 * @param timeoutMs the time out
	 * @return the message
	 * @throws TimeoutException
	 */
    private static Message waitForServerResponse(byte messageType, int timeoutMs) throws TimeoutException {
        long started = System.currentTimeMillis();
        while (true) {
            synchronized (lastMessages) {
                for (Message msg : lastMessages) {
                    if (msg.getMessageType() == messageType) {
                        lastMessages.remove(msg);
                        return msg;
                    }
                }
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (System.currentTimeMillis() - started > timeoutMs) throw new TimeoutException("Time out");
        }
    }

    /**
	 * Registers at the server with the specified username and password
	 * @param username the username
	 * @param password the password
	 * @return results
	 */
    public static RegisterResults register(String username, String password) {
        String hashedPw;
        try {
            hashedPw = HashPassword.unsecureHashConstantSalt(password);
        } catch (NoSuchAlgorithmException e2) {
            e2.printStackTrace();
            return RegisterResults.UNKNOWNERROR;
        } catch (UnsupportedEncodingException e2) {
            e2.printStackTrace();
            return RegisterResults.UNKNOWNERROR;
        }
        ByteBuffer bytebuffer = ByteBuffer.allocate(ByteBufferUtilities.getSizeForString(username) + ByteBufferUtilities.getSizeForString(hashedPw));
        ByteBufferUtilities.putString(bytebuffer, username);
        ByteBufferUtilities.putString(bytebuffer, hashedPw);
        Message msg = new Message(bytebuffer.array(), (byte) 0, ServerMessageGUID);
        try {
            connection.sendMessage(msg);
            Message results = waitForServerResponse((byte) 0, 8000);
            int result = results.getMessage()[0];
            switch(result) {
                case 0:
                    return RegisterResults.REGISTERSUCCESSFULL;
                case 1:
                    return RegisterResults.USERNAMETAKEN;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return RegisterResults.SERVERCONNECTIONERROR;
        }
        return RegisterResults.UNKNOWNERROR;
    }

    /**
	 * Tries to login using an username and a password.
	 * Call getUsername() if the login was successfull!
	 * 
	 * @param username the user name
	 * @param password the password
	 * @param onlineStatus your new online status
	 * @return results
	 */
    public static LoginResults login(String username, String password, OnlineStatus onlineStatus) {
        String hashedPw;
        try {
            hashedPw = HashPassword.unsecureHashConstantSalt(password);
            return loginWithHashedPassword(username, hashedPw, onlineStatus);
        } catch (NoSuchAlgorithmException e2) {
            e2.printStackTrace();
            return LoginResults.UNKNOWNERROR;
        } catch (UnsupportedEncodingException e2) {
            e2.printStackTrace();
            return LoginResults.UNKNOWNERROR;
        }
    }

    /**
	 * Tries to login using an username and an already hashed password.
	 * Call getUsername() if the login was successfull!
	 * 
	 * @param username the user name
	 * @param password the password
	 * @param onlineStatus your new online status
	 * @return results
	 */
    public static LoginResults loginWithHashedPassword(String username, String hashedPw, OnlineStatus onlineStatus) {
        ByteBuffer bytebuffer = ByteBuffer.allocate(ByteBufferUtilities.getSizeForString(username) + ByteBufferUtilities.getSizeForString(hashedPw) + 1);
        try {
            bytebuffer.put(UserAccount.convertOnlineStatusToByte(onlineStatus));
        } catch (Exception e1) {
            e1.printStackTrace();
            return LoginResults.UNKNOWNERROR;
        }
        ByteBufferUtilities.putString(bytebuffer, username);
        ByteBufferUtilities.putString(bytebuffer, hashedPw);
        Message msg = new Message(bytebuffer.array(), (byte) 1, ServerMessageGUID);
        try {
            Message results = null;
            for (int i = 0; i <= 3; i++) {
                try {
                    connection.sendMessage(msg);
                    results = waitForServerResponse((byte) 1, 4000);
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            ByteBuffer wrapped = ByteBuffer.wrap(results.getMessage());
            int result = wrapped.get();
            switch(result) {
                case 0:
                    username = ByteBufferUtilities.getStringFromByteBuffer(wrapped);
                    loginUsername = username;
                    loginPassword = hashedPw;
                    lastOnlineStatus = onlineStatus;
                    return LoginResults.LOGINSUCCESSFULL;
                case 1:
                    return LoginResults.CREDENTIALSWRONG;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return LoginResults.SERVERCONNECTIONERROR;
        }
        return LoginResults.UNKNOWNERROR;
    }

    /**
	 * Returns the logged in username (with the right case). This should be called after Login
	 * @return the username
	 */
    public static String getUsername() {
        return loginUsername;
    }

    /**
	 * Change the password
	 * 
	 * @param username 	  the username, whose password should be changed 
	 * @param oldPassword the old password
	 * @param newPassword the new password
	 * @return results
	 */
    public static PasswordChangeResults changePassword(String username, String oldPassword, String newPassword) {
        String oldHashedPw, newHashedPw;
        try {
            oldHashedPw = HashPassword.unsecureHashConstantSalt(oldPassword);
            newHashedPw = HashPassword.unsecureHashConstantSalt(newPassword);
        } catch (NoSuchAlgorithmException e2) {
            e2.printStackTrace();
            return PasswordChangeResults.UNKNOWNERROR;
        } catch (UnsupportedEncodingException e2) {
            e2.printStackTrace();
            return PasswordChangeResults.UNKNOWNERROR;
        }
        ByteBuffer bytebuffer = ByteBuffer.allocate(ByteBufferUtilities.getSizeForString(username) + ByteBufferUtilities.getSizeForString(oldHashedPw) + ByteBufferUtilities.getSizeForString(newHashedPw));
        ByteBufferUtilities.putString(bytebuffer, username);
        ByteBufferUtilities.putString(bytebuffer, oldHashedPw);
        ByteBufferUtilities.putString(bytebuffer, newHashedPw);
        Message msg = new Message(bytebuffer.array(), (byte) 2, ServerMessageGUID);
        try {
            connection.sendMessage(msg);
            Message results = waitForServerResponse((byte) 2, 8000);
            int result = results.getMessage()[0];
            switch(result) {
                case 0:
                    return PasswordChangeResults.CHANGESUCCESSFULL;
                case 1:
                    return PasswordChangeResults.CREDENTIALSWRONG;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return PasswordChangeResults.SERVERCONNECTIONERROR;
        }
        return PasswordChangeResults.UNKNOWNERROR;
    }

    /**
	 * Updates your account
	 * @param account your account
	 * @return results
	 */
    public static UpdateAccountResults updateAccountData(UserAccount account) {
        try {
            Message msg = new Message(account.serializeUserAccount(), (byte) 3, ServerMessageGUID);
            connection.sendMessage(msg);
            Message results = waitForServerResponse((byte) 3, 8000);
            int result = results.getMessage()[0];
            switch(result) {
                case 0:
                    lastOnlineStatus = account.getOnlineStatus();
                    return UpdateAccountResults.UPDATESUCCESSFULL;
                case 1:
                    return UpdateAccountResults.NOSUFFICIENTRIGHTS;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return UpdateAccountResults.SERVERCONNECTIONERROR;
        }
        return UpdateAccountResults.UNKNOWNERROR;
    }

    /**
	 * Retrieve an account of someone
	 * @param username the user name
	 * @return the user account
	 * @throws Exception
	 */
    public static UserAccount retrieveAccount(String username) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(ByteBufferUtilities.getSizeForString(username));
        ByteBufferUtilities.putString(byteBuffer, username);
        Message msg = new Message(byteBuffer.array(), (byte) 4, ServerMessageGUID);
        connection.sendMessage(msg);
        Message results = waitForServerResponse((byte) 4, 8000);
        if (results.getMessage().length == 1 && results.getMessage()[0] == 1) throw new Exception("No sufficient rights to retrieve account.");
        return UserAccount.loadUserAccount(results.getMessage());
    }

    /**
	 * Retrieve the contact list
	 * @throws Exception
	 */
    public static ContactList retrieveContactList() throws Exception {
        Message msg = new Message(new byte[0], (byte) 5, ServerMessageGUID);
        connection.sendMessage(msg);
        Message results = waitForServerResponse((byte) 5, 8000);
        if (results.getMessage().length == 1 && results.getMessage()[0] == 1) throw new Exception("No sufficient rights to retrieve account.");
        return ContactList.deserializeContactList(results.getMessage());
    }

    /**
	 * Requests the status of all contact list members
	 * @throws Exception
	 */
    public static void requestContactListOnlineStatus() throws Exception {
        Message msg = new Message(new byte[0], (byte) 10, ServerMessageGUID);
        connection.sendMessage(msg);
    }

    /**
	 * Updates the contact list
	 * @param contactList the new contact list
	 * @return true if the update process has been completed successfully
	 * @throws Exception
	 */
    public static boolean updateContactList(ContactList contactList) throws Exception {
        Message msg = new Message(contactList.serializeContactList(), (byte) 6, ServerMessageGUID);
        connection.sendMessage(msg);
        Message results = waitForServerResponse((byte) 6, 8000);
        if (results.getMessage().length == 1 && results.getMessage()[0] == 1) throw new Exception("No sufficient rights to retrieve account.");
        return msg.getMessage().length > 0 && msg.getMessage()[0] == 0;
    }

    /**
	 * Returns the public key of a user. It requests the key from the server.
	 * @param username the username
	 * @return the public key
	 * @throws Exception
	 */
    public static PublicKey getPublicKey(String username) throws Exception {
        Message msg = new Message(username.getBytes("UTF-8"), (byte) 9, ServerMessageGUID);
        connection.sendMessage(msg);
        Message results = waitForServerResponse((byte) 9, 8000);
        if (results.getMessage().length == 1 && results.getMessage()[0] == 1) throw new Exception("Failed retrieve public key.");
        return RSA.getPublicKey(results.getMessage());
    }

    /**
	 * Returns the corresponding user name to a peer id
	 * @param id the peer id
	 * @return the corresponding user name
	 * @throws Exception
	 */
    static String getUsername(PeerID id) throws Exception {
        String str = id.toURI().toString();
        Message msg = new Message(str.getBytes("UTF-8"), (byte) 8, ServerMessageGUID);
        connection.sendMessage(msg);
        Message results = waitForServerResponse((byte) 8, 16000);
        if (results.getMessage().length == 1 && results.getMessage()[0] == 1) throw new Exception("Failed retrieve username.");
        return new String(results.getMessage(), "UTF-8");
    }

    /**
	 * Establishes a secure peer-to-peer connection to a user.
	 * WARNING: This method will block until a connection could be established. There is no timeout.
	 * @param  username the username
	 * @return the resulting connection
	 * @throws Exception
	 */
    public static synchronized P2PConnection establishConnection(String username) throws Exception {
        synchronized (connections) {
            P2PConnection p2p = connections.get(username);
            if (p2p != null && !p2p.reliableConnection.isClosed && !p2p.reliableConnection.getSocket().isClosed() && connection.getSocket().isConnected()) {
                System.out.println("Returning existing connection to " + username);
                return p2p;
            }
        }
        Message msg = new Message(username.getBytes("UTF-8"), (byte) 7, ServerMessageGUID);
        connection.sendMessage(msg);
        int c = 0;
        while (true) {
            P2PConnection p2p;
            synchronized (connections) {
                p2p = connections.get(username);
            }
            if (p2p != null && p2p.hasValidatedPublicKey) return p2p;
            Thread.sleep(50);
            c++;
            if (c >= 300) {
                connection.sendMessage(msg);
                c = 0;
            }
        }
    }

    /**
	 * Changes the status
	 * @param newStatus the new status
	 */
    public static void changeStatus(OnlineStatus newStatus) {
        try {
            UserAccount account = retrieveAccount(loginUsername);
            account.setOnlineStatus(newStatus);
            updateAccountData(account);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Searches for a contact
	 * @param keyword the keyword
	 * @return the list of resulting user names
	 */
    public static String[] searchContact(String keyword) {
        ByteBuffer bytebuffer = ByteBuffer.allocate(ByteBufferUtilities.getSizeForString(keyword));
        ByteBufferUtilities.putString(bytebuffer, keyword);
        Message msg = new Message(bytebuffer.array(), (byte) 11, ServerMessageGUID);
        try {
            connection.sendMessage(msg);
            Message results = waitForServerResponse((byte) 11, 8000);
            if (results.getMessage().length == 0) return new String[0];
            ByteBuffer result = ByteBuffer.wrap(results.getMessage());
            String[] searchResults = new String[result.getInt()];
            for (int i = 0; i < searchResults.length; i++) searchResults[i] = ByteBufferUtilities.getStringFromByteBuffer(result);
            return searchResults;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String[0];
    }

    /**
	 * Call this function when you add someone 
	 * @param usernameAdded the username of the added contact
	 * @param message the welcome message
	 */
    public static void addedSomeone(String usernameAdded, String message) {
        ByteBuffer bytebuffer = ByteBuffer.allocate(ByteBufferUtilities.getSizeForString(usernameAdded) + ByteBufferUtilities.getSizeForString(message));
        ByteBufferUtilities.putString(bytebuffer, usernameAdded);
        ByteBufferUtilities.putString(bytebuffer, message);
        Message msg = new Message(bytebuffer.array(), (byte) 12, ServerMessageGUID);
        try {
            connection.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Closes the connection to the server
	 */
    public static void close() {
        connected = false;
        if (connection != null) connection.close();
        Network.end();
    }

    /**
	 * Changes the public message.
	 * @param message the new public message
	 */
    public static void changePublicMessage(String message) {
        try {
            UserAccount account = retrieveAccount(loginUsername);
            account.setPublicMessage(message);
            updateAccountData(account);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Tries to provide access to an account with a lost password
	 * @param usernameOrMail the username or mail of the lost account
	 * @return the result - should be MAILSENT
	 */
    public static LostPasswordResults lostPassword(String usernameOrMail) {
        Message msg = new Message(ByteBufferUtilities.getByteArrayFromString(usernameOrMail), (byte) 13, ServerMessageGUID);
        try {
            connection.sendMessage(msg);
            Message results = waitForServerResponse((byte) 13, 8000);
            switch(results.getMessage()[0]) {
                case 0:
                    return LostPasswordResults.MAILSENT;
                case 1:
                    return LostPasswordResults.NOTFOUND;
                case 2:
                    return LostPasswordResults.NOMAILADDRESSPROVIDED;
                default:
                    return LostPasswordResults.UNKNOWNERROR;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return LostPasswordResults.UNKNOWNERROR;
    }

    /**
	 * Tries to provide access to an account with a lost password.
	 * Call lostPassword first.
	 * The user has to enter a code.
	 * @param code the given code
	 * @param newPassword the new password
	 * @return the result - should be RESETSUCCESSFULLY
	 */
    public static LostPasswordResults lostPasswordHasCode(String code, String newPassword) {
        String newPasswordHash;
        try {
            newPasswordHash = HashPassword.unsecureHashConstantSalt(newPassword);
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
            return LostPasswordResults.UNKNOWNERROR;
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
            return LostPasswordResults.UNKNOWNERROR;
        }
        ByteBuffer bytebuffer = ByteBuffer.allocate(ByteBufferUtilities.getSizeForString(code) + ByteBufferUtilities.getSizeForString(newPasswordHash));
        ByteBufferUtilities.putString(bytebuffer, code);
        ByteBufferUtilities.putString(bytebuffer, newPasswordHash);
        Message msg = new Message(bytebuffer.array(), (byte) 14, ServerMessageGUID);
        try {
            connection.sendMessage(msg);
            Message results = waitForServerResponse((byte) 14, 8000);
            switch(results.getMessage()[0]) {
                case 0:
                    return LostPasswordResults.RESETSUCCESSFULLY;
                case 1:
                    return LostPasswordResults.WRONGCODE;
                default:
                    return LostPasswordResults.UNKNOWNERROR;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return LostPasswordResults.UNKNOWNERROR;
    }

    /**
	 * Returns true if there is a connection to the server
	 * @return true if there is a connection to the server
	 */
    public static boolean isConnected() {
        return connected;
    }

    /**
	 * Logs the user out
	 * @return whether the log out was successful
	 */
    public static boolean logout() {
        changeStatus(OnlineStatus.OFFLINE);
        Message msg = new Message(new byte[0], (byte) 16, ServerMessageGUID);
        try {
            connection.sendMessage(msg);
            Message results = waitForServerResponse((byte) 16, 8000);
            if (results.getMessage().length >= 0) return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connected;
    }

    /**
	 * Returns an already established connection (or null)
	 * @param username the username
	 * @return the established connection
	 */
    public static P2PConnection returnEstablishedConnection(String username) {
        synchronized (connections) {
            P2PConnection p2p = connections.get(username);
            if (p2p != null && !p2p.reliableConnection.isClosed && !p2p.reliableConnection.getSocket().isClosed() && connection.getSocket().isConnected()) return p2p;
        }
        return null;
    }

    public static void simulateConnectionTimeout() {
        if (connected) {
            System.out.println("Lost connection to server");
            connection = null;
            connected = false;
            serverConnectionListener.lostConnectionToServer(false);
            while (!connected || connection == null) {
                try {
                    establishConnection();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                if (loginUsername != null) loginWithHashedPassword(loginUsername, loginPassword, lastOnlineStatus);
            }
            serverConnectionListener.lostConnectionToServer(true);
            System.out.println("Reestablished connection to server");
        }
    }
}
