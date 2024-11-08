package de.goddchen.gbouncer.connection_management.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ResourceBundle;
import java.util.Vector;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.log4j.Logger;
import de.goddchen.gbouncer.common.Constants;
import de.goddchen.gbouncer.common.IServerInfo;
import de.goddchen.gbouncer.common.IUser;
import de.goddchen.gbouncer.common.rfc1459.IRCMessage;
import de.goddchen.gbouncer.configuration.IConfiguration;
import de.goddchen.gbouncer.connection_management.IConnectionManager;
import de.goddchen.gbouncer.exceptions.ConfigurationException;
import de.goddchen.gbouncer.exceptions.ConnectionManagerException;
import de.goddchen.gbouncer.exceptions.UserException;
import de.goddchen.gbouncer.identd.IIdentd;
import de.goddchen.gbouncer.plugin_system.IPluginSystem;

public class ServerConnection extends Thread implements IServerConnection {

    private static final long serialVersionUID = 1L;

    private IServerInfo info;

    private boolean isConnectionAlive;

    private IUser user;

    private IConfiguration config;

    private static Logger logger = Logger.getLogger(ServerConnection.class);

    private static ResourceBundle langRes = ResourceBundle.getBundle("lang/lang", Constants.currentLocale);

    private BufferedWriter writer;

    private ServerConnectionListener myListener;

    private Vector<String> channels;

    private Socket clientSocket;

    private IConnectionManager connMgr;

    private IPluginSystem pluginSystem;

    public ServerConnection(IServerInfo info, IUser user) {
        this.info = info;
        this.user = user;
        isConnectionAlive = false;
        channels = new Vector<String>();
        try {
            config = (IConfiguration) LocateRegistry.getRegistry().lookup("Configuration");
            connMgr = (IConnectionManager) LocateRegistry.getRegistry().lookup("ConnectionManagement");
            pluginSystem = (IPluginSystem) LocateRegistry.getRegistry().lookup("PluginSystem");
        } catch (AccessException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    public IUser getUser() throws RemoteException {
        return user;
    }

    public String getServerHostname() throws RemoteException {
        return info.getHostname();
    }

    public int getServerPort() throws RemoteException {
        return info.getPort();
    }

    public boolean isConnectionAlive() throws RemoteException {
        return isConnectionAlive;
    }

    public void setConnectionAlive(boolean alive) throws RemoteException {
        isConnectionAlive = alive;
    }

    public void startHandling() throws RemoteException {
        this.start();
    }

    @Override
    public void run() {
        try {
            logger.debug(langRes.getString("SERVERCONN_STARTING") + " (" + user.getProperty("name") + "@" + info.getHostname() + ")");
            if (config.get("ConnectOnStartup").equals("True")) {
                connectToServer();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UserException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void connectToServer() throws IOException, UnknownHostException, RemoteException, UserException, NotBoundException, AccessException {
        if (info.getHostname().startsWith("S=")) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                } };
                SSLContext sslContext = SSLContext.getInstance("SSLv3");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                clientSocket = sslSocketFactory.createSocket(InetAddress.getByName(info.getHostname().replace("S=", "")), info.getPort());
                logger.debug(langRes.getString("SERVERCONN_CREATED_SECURE_CLIENT_SOCKET") + " " + info.getHostname());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }
        } else {
            try {
                clientSocket = new Socket(InetAddress.getByName(info.getHostname()), info.getPort());
            } catch (ConnectException e) {
                logger.warn("Exception: " + e.getMessage());
            }
            logger.debug(langRes.getString("SERVERCONN_CREATED_CLIENT_SOCKET") + " " + info.getHostname());
        }
        if (clientSocket != null) {
            new ServerConnectionMonitor().start();
            logger.debug(langRes.getString("SERVERCONN_STARTED_MONITOR") + " (" + info.getHostname() + ":" + info.getPort() + ")");
            IIdentd identd = (IIdentd) LocateRegistry.getRegistry().lookup("Identd");
            identd.setIdent(user.getProperty("name"));
            identd.startIdentd();
            myListener = new ServerConnectionListener(clientSocket, user, this);
            myListener.start();
            writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            sendMessage(IRCMessage.create("", "NICK", new String[] { user.getProperty("nickname") }));
            sendMessage(IRCMessage.create("", "USER", new String[] { user.getProperty("name"), "0", "*", user.getProperty("name") }));
        } else {
            logger.error(langRes.getString("SERVERCONN_UNABLE_TO_CREATED_SOCKET") + " " + info.getHostname());
            logger.debug(langRes.getString("SERVERCONN_FAILED_TO_CONNECT") + " (" + info.getHostname() + ":" + info.getPort() + ")");
            try {
                connMgr.getClientConnectionFromUser(user).sendMessage(IRCMessage.create(null, "NOTICE", new String[] { user.getProperty("nickname") + " :" + langRes.getString("SERVERCONN_FAILED_TO_CONNECT") + " (" + info.getHostname() + ":" + info.getPort() + ")" }));
            } catch (ConnectionManagerException e1) {
                e1.printStackTrace();
            }
            try {
                Thread.sleep(Integer.valueOf(config.get("ServerRetryInterval")) * 1000);
                connectToServer();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(IRCMessage message) throws RemoteException {
        pluginSystem.passMessageToServer(message);
        try {
            if ("JOIN".equals(message.command.toUpperCase())) {
                channels.add(message.params[0]);
                logger.debug(langRes.getString("SERVERCONN_ADDING_CHANNEL") + " (" + message.params[0] + ")");
                String modifiedChannel = message.params[0];
                if (!user.getProperty("channels").contains(";" + modifiedChannel + ";")) {
                    user.setProperty("channels", user.getProperty("channels") + ";" + modifiedChannel + ";");
                }
            } else if ("PART".equals(message.command.toUpperCase())) {
                String modifiedChannel = message.params[0];
                logger.debug(langRes.getString("SERVERCONN_REMOVE_CHANNEL") + " (" + message.params[0] + ")");
                channels.remove(message.params[0]);
                user.setProperty("channels", user.getProperty("channels").replace(";" + modifiedChannel + ";", ""));
            }
            if (writer != null) {
                String line = message.toString();
                logger.trace(langRes.getString("SERVERCONN_SENDING_LINE") + " (" + line + ")");
                writer.write(line + "\r\n");
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UserException e) {
            e.printStackTrace();
        }
    }

    public IServerInfo getServerInfo() throws RemoteException {
        return info;
    }

    public void reconnect() throws RemoteException {
        disconnect();
        connect();
    }

    public void connect() throws RemoteException {
        info = user.getServer();
        try {
            if (!isConnectionAlive) {
                logger.debug(langRes.getString("SERVERCONN_CONNECTING") + " " + info.getHostname() + ":" + info.getPort());
                connectToServer();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UserException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() throws RemoteException {
        if (isConnectionAlive) {
            logger.debug(langRes.getString("SERVERCONN_DISCONNECTING") + " " + info.getHostname() + ":" + info.getPort());
            try {
                sendMessage(IRCMessage.create("", "QUIT", new String[] { user.getProperty("quitmessage") }));
                isConnectionAlive = false;
            } catch (UserException e) {
                e.printStackTrace();
            }
        }
    }

    public IRCMessage[] getUnreadMessages() throws RemoteException {
        if (myListener == null) {
            return new IRCMessage[0];
        } else {
            return myListener.getUnreadMessages();
        }
    }

    public String[] getChannels() throws RemoteException {
        return channels.toArray(new String[channels.size()]);
    }

    private class ServerConnectionMonitor extends Thread {

        @Override
        public void run() {
            while (clientSocket == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            while (clientSocket.isConnected()) {
                try {
                    Thread.sleep(30 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                logger.warn(langRes.getString("SERVERCONN_RECONNECT") + " (" + info.getHostname() + ":" + info.getPort() + ")");
                sendMessage(IRCMessage.create("", "NOTICE", new String[] { user.getProperty("nickname"), langRes.getString("SERVERCONN_RECONNECT") + " (" + info.getHostname() + ":" + info.getPort() + ")" }));
                connectToServer();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (UserException e) {
                e.printStackTrace();
            } catch (NotBoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void addChannel(String channel) throws RemoteException {
        if (!channels.contains(channel)) {
            logger.debug(langRes.getString("SERVERCONN_ADDING_CHANNEL"));
            channels.add(channel);
        }
    }
}
