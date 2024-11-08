package de.goddchen.gbouncer.connection_management.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ResourceBundle;
import java.util.Vector;
import org.apache.log4j.Logger;
import de.goddchen.gbouncer.common.Constants;
import de.goddchen.gbouncer.common.IUser;
import de.goddchen.gbouncer.common.rfc1459.IRCMessage;
import de.goddchen.gbouncer.connection_management.IConnectionManager;
import de.goddchen.gbouncer.connection_management.server.IServerConnection;
import de.goddchen.gbouncer.exceptions.ConnectionManagerException;
import de.goddchen.gbouncer.exceptions.UserManagementException;
import de.goddchen.gbouncer.plugin_system.IPluginSystem;
import de.goddchen.gbouncer.user_management.IUserManagement;

public class ClientConnection extends Thread implements IClientConnection {

    private ClientConnectionProperties props;

    private static Logger logger = Logger.getLogger(ClientConnection.class);

    private static ResourceBundle langRes = ResourceBundle.getBundle("lang/lang", Constants.currentLocale);

    private BufferedWriter writer;

    private IUserManagement usrMgr;

    private IConnectionManager connMgr;

    private IPluginSystem pluginSystem;

    private Socket clientSocket;

    private Vector<String> serverNames;

    public ClientConnection(Socket clientSocket) {
        this.clientSocket = clientSocket;
        serverNames = new Vector<String>();
        props = new ClientConnectionProperties();
        try {
            writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            usrMgr = (IUserManagement) LocateRegistry.getRegistry().lookup("UserManagement");
            connMgr = (IConnectionManager) LocateRegistry.getRegistry().lookup("ConnectionManagement");
            pluginSystem = (IPluginSystem) LocateRegistry.getRegistry().lookup("PluginSystem");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        logger.debug(langRes.getString("CLIENTCONN_STARTING"));
        new ClientConnectionListener(clientSocket, props).start();
        props.waitTillConnected();
        logger.debug(langRes.getString("CLIENTCONN_USER_AND_NICK_SENT"));
        logger.debug(langRes.getString("CLIENTCONN_SENDING_REPLIES"));
        try {
            sendMessage(IRCMessage.create(props.getServerHost(), "001", new String[] { props.getNickname(), langRes.getString("CLIENTCONN_WELCOME_TO_BOUNCER") + " " + props.getNickname() + "!" + props.getUsername() + "@" + props.getClientHost() }));
            sendMessage(IRCMessage.create(props.getServerHost(), "002", new String[] { props.getNickname(), langRes.getString("CLIENTCONN_YOUR_HOST_IS") + " " + props.getServerHost() + ", " + langRes.getString("CLIENTCONN_RUNNING_VERSION") + " " + Constants.VERSION }));
            sendMessage(IRCMessage.create(props.getServerHost(), "003", new String[] { props.getNickname(), langRes.getString("CLIENTCONN_CREATED") }));
            sendMessage(IRCMessage.create(props.getServerHost(), "004", new String[] { props.getNickname(), "gBouncer " + Constants.VERSION }));
            logger.debug(langRes.getString("CLIENTCONN_SENDING_MOTD"));
            sendMessage(IRCMessage.create(props.getServerHost(), "375", new String[] { props.getNickname(), "- " + props.getServerHost() + " Message of the day - " }));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        try {
            FileInputStream fis = new FileInputStream("motd.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                sendMessage(IRCMessage.create(props.getServerHost(), "372", new String[] { props.getNickname(), "- " + line }));
            }
            reader.close();
        } catch (FileNotFoundException e) {
            logger.warn(langRes.getString("CLIENTCONN_MOTD_NOT_FOUND"));
            try {
                sendMessage(IRCMessage.create(props.getServerHost(), "372", new String[] { props.getNickname(), "- " + langRes.getString("CLIENTCONN_MOTD_NOT_FOUND") }));
            } catch (RemoteException ex) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            sendMessage(IRCMessage.create(props.getServerHost(), "376", new String[] { props.getNickname(), "End of /MOTD command" }));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        try {
            IServerConnection serverConn = connMgr.getServerConnenctionFromUser(usrMgr.getUser(props.getUsername()));
            if (serverConn != null) {
                IRCMessage[] unreadMessages = serverConn.getUnreadMessages();
                if (unreadMessages.length > 0) {
                    logger.debug(langRes.getString("CLIENTCONN_SENDING_UNREAD"));
                    sendMessage(IRCMessage.create(props.getServerHost(), "PRIVMSG", new String[] { props.getNickname(), langRes.getString("CLIENTCONN_UNREAD_MESSAGES") + ":" }));
                    for (IRCMessage message : unreadMessages) {
                        sendMessage(IRCMessage.create(props.getServerHost(), "PRIVMSG", new String[] { props.getNickname(), message.prefix + " -> " + message.params[1] }));
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (ConnectionManagerException e) {
            e.printStackTrace();
        } catch (UserManagementException e) {
            e.printStackTrace();
        }
        try {
            IServerConnection serverConn = connMgr.getServerConnenctionFromUser(usrMgr.getUser(props.getUsername()));
            if (serverConn != null) {
                for (String channel : serverConn.getChannels()) {
                    String modifiedChannel = channel;
                    logger.debug(langRes.getString("CLIENTCONN_REJOIN_CHANNEL") + " (" + modifiedChannel + ")");
                    sendMessage(IRCMessage.create(props.getNickname() + "!" + props.getUsername() + "@" + props.getClientHost(), "JOIN", new String[] { modifiedChannel }));
                    serverConn.sendMessage(IRCMessage.create("", "TOPIC", new String[] { channel }));
                    serverConn.sendMessage(IRCMessage.create("", "NAMES", new String[] { channel }));
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (ConnectionManagerException e) {
            e.printStackTrace();
        } catch (UserManagementException e) {
            e.printStackTrace();
        }
        new PingPongThread().start();
    }

    public void sendLine(String line) throws RemoteException {
        if (line != null) {
            pluginSystem.passMessageToClient(IRCMessage.parse(line));
            line = line.replace("%SERVERHOST%", props.getServerHost());
            logger.trace(langRes.getString("CLIENTCONN_SENDING_LINE") + " (" + line + ")");
            try {
                writer.write(line + "\r\n");
                writer.flush();
            } catch (IOException e) {
                logger.warn("Exception: " + e.getMessage());
                logger.warn(langRes.getString("CLIENTCONN_CONN_DOWN"));
                logger.warn(langRes.getString("CLIENTCONN_DROPPING") + " (" + line + ")");
                props.setConnected(false);
            }
        }
    }

    public IUser getUser() throws RemoteException {
        if (props.isConnected()) {
            try {
                return usrMgr.getUser(props.getUsername());
            } catch (RemoteException e) {
                e.printStackTrace();
                return null;
            } catch (UserManagementException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    public String getClientHostname() throws RemoteException {
        return props.getClientHost();
    }

    public boolean isConnectionAlive() throws RemoteException {
        return props.isConnected();
    }

    public void startHandling() throws RemoteException {
        this.start();
    }

    private class PingPongThread extends Thread {

        @Override
        public void run() {
            while (props.isConnected()) {
                try {
                    sendLine("PING :" + props.getServerHost());
                    Thread.sleep(30 * 1000);
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void sendMessage(IRCMessage message) throws RemoteException {
        pluginSystem.passMessageToClient(message);
        try {
            if ("001".equals(message.command)) {
                serverNames.add(message.prefix);
            }
            if (serverNames.contains(message.prefix)) {
                message.prefix = props.getServerHost();
            }
            logger.trace(langRes.getString("CLIENTCONN_SENDING_LINE") + " (" + message + ")");
            writer.write(message.toString() + "\r\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
