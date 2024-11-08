package vavi.net.im.protocol.oscar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import vavi.net.im.Buddy;
import vavi.net.im.Group;
import vavi.net.im.Message;
import vavi.net.im.MessageComponent;
import vavi.net.im.TextComponent;
import vavi.net.im.event.IMAdapter;
import vavi.net.im.event.IMEvent;
import vavi.net.im.event.IMListener;
import vavi.net.im.event.IMSupport;
import vavi.net.im.event.IMEvent.IMEventName;
import vavi.net.im.event.IMEvent.Name;
import vavi.net.im.protocol.Connection;
import vavi.net.im.protocol.DirectConnection;
import vavi.net.im.protocol.HttpConnection;
import vavi.net.im.protocol.SocksConnection;
import vavi.net.im.protocol.oscar.command.AckServerRateInfo;
import vavi.net.im.protocol.oscar.command.AddBuddyCommand;
import vavi.net.im.protocol.oscar.command.BOSLoginCommand;
import vavi.net.im.protocol.oscar.command.BaseCommand;
import vavi.net.im.protocol.oscar.command.ClientFamilyVersionCommand;
import vavi.net.im.protocol.oscar.command.ClientICMB;
import vavi.net.im.protocol.oscar.command.ClientReadyCommand;
import vavi.net.im.protocol.oscar.command.ClientSetICBMParmsCommand;
import vavi.net.im.protocol.oscar.command.ClientSetLocationInfoCommand;
import vavi.net.im.protocol.oscar.command.ClientSetStatusCommand;
import vavi.net.im.protocol.oscar.command.Command;
import vavi.net.im.protocol.oscar.command.DeleteBuddyCommand;
import vavi.net.im.protocol.oscar.command.IgnoreBuddyCommand;
import vavi.net.im.protocol.oscar.command.LoginCommand;
import vavi.net.im.protocol.oscar.command.RequestICBMParmCommand;
import vavi.net.im.protocol.oscar.command.RequestServerRateCommand;
import vavi.net.im.protocol.oscar.command.TypingNotificationCommand;
import vavi.net.im.protocol.oscar.command.UnignoreBuddyCommand;
import vavi.net.im.protocol.oscar.snac.SNACConstants;

/**
 * OscarConnection.
 * 
 * @author mikem
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 */
public class OscarConnection {

    /** */
    private static Logger log = Logger.getLogger(OscarConnection.class.getName());

    /** */
    public enum Status {

        DISCONNECTED, /** sending client ident */
        CONNECTING, /** disconnecting from authentication server */
        DISCONNECTING, /** next we connect to the BOS server wtih cookie */
        BOS_CONNECTING, /** */
        BOS_CONNECTED, /** */
        BOS_SERVICE_LIST, /** */
        PROTOCOL_NEGOTIATION, /** */
        ACK_CONNECT_RATES, /** */
        CLIENT_READY
    }

    /** This thread handles all outgoing commands to AIM. */
    private WriterThread writer;

    /** This is a buffer for holding all outgoing commands. Access to this buffer must be synchronized. */
    private List<Command> writeBuffer;

    /** internal state */
    private Status status;

    /** connection type */
    private Properties props;

    /** users screen name*/
    private String screenName;

    /** connection object */
    private Connection connection;

    /** */
    private IMSupport listeners;

    /** */
    private boolean quit = false;

    /** */
    private int sequenceNumber = 11;

    /** */
    public Status getStatus() {
        return status;
    }

    /** */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Default Constructor
     */
    public OscarConnection(IMSupport listeners) {
        status = Status.DISCONNECTED;
        log.info("status: " + status);
        writeBuffer = new ArrayList<Command>();
        this.listeners = listeners;
        listeners.addIMListener(defaultIMListener);
    }

    /** */
    public void finalize() {
        quit = true;
    }

    /**
     * Sends the initial login request to AIM
     * @param host host name of the server to connect to
     * @param port port number to connect to
     * @param props "username", "password", "connection", ...
     */
    public void connect(String host, int port, Properties props) throws IOException {
        this.screenName = props.getProperty("username");
        String password = props.getProperty("password");
        this.props = props;
        connectToHost(host, port, Status.CONNECTING);
        OscarEventHandler eventHandler = new OscarEventHandler(this);
        eventHandler.setIMListeners(listeners);
        eventHandler.start();
        writer = new WriterThread(connection, writeBuffer);
        writer.start();
        sendToWriterThread(new LoginCommand(screenName, password));
    }

    private void sendToWriterThread(Command command) {
        synchronized (writeBuffer) {
            writeBuffer.add(command);
            writeBuffer.notify();
        }
    }

    /** */
    public boolean isRunning() {
        return !quit;
    }

    /**
     * Reconnect to host - used during the OSCAR authentication processing
     * @param hostName
     * @param port
     */
    public void reconnect(String hostName, int port) throws IOException {
        log.info("disconnecting from auth server/connecting to BOS server...");
        disconnect();
        connectToHost(hostName, port, Status.BOS_CONNECTING);
        writer = new WriterThread(connection, writeBuffer);
        writer.start();
    }

    /**
     * Cleans up the connection info in the handler
     */
    public void disconnect() throws IOException {
        status = Status.DISCONNECTING;
        log.info("status: " + status);
        writer.stopWriting();
        connection.close();
        status = Status.DISCONNECTED;
        log.info("status: " + status);
    }

    /**
     * Connects to a server
     */
    private void connectToHost(String host, int port, Status newState) throws IOException {
        log.info("creating connection to host " + host + " port " + port);
        status = newState;
        log.info("status: " + status);
        String connection = props.getProperty("connection");
        if (connection.equals("DIRECT")) {
            this.connection = new DirectConnection(host, port);
        } else if (connection.equals("SOCKS4")) {
            String proxyHost = props.getProperty("ServerName");
            int proxyPort = Integer.parseInt(props.getProperty("ServerPort"));
            this.connection = new SocksConnection(proxyHost, proxyPort, host, port);
        } else if (connection.equals("SOCKS5")) {
            String proxyHost = props.getProperty("ServerName");
            int proxyPort = Integer.parseInt(props.getProperty("ServerPort"));
            String proxyUsername = props.getProperty("Username");
            String proxyPassword = props.getProperty("Password");
            this.connection = new SocksConnection(proxyHost, proxyPort, proxyUsername, proxyPassword, host, port);
        } else if (connection.equals("HTTP")) {
            String proxyHost = props.getProperty("ServerName");
            int proxyPort = Integer.parseInt(props.getProperty("ServerPort"));
            String proxyUsername = props.getProperty("Username");
            String proxyPassword = props.getProperty("Password");
            if ((proxyUsername == null) || (proxyPassword == null)) {
                this.connection = new HttpConnection(proxyHost, proxyPort, host, port);
            } else {
                this.connection = new HttpConnection(proxyHost, proxyPort, proxyUsername, proxyPassword, host, port);
            }
        }
    }

    /**
     * Send a buddy an instant message
     * 
     * @param buddy Buddy
     * @param message Message
     */
    public void sendInstantMessage(Buddy buddy, Message message) {
        StringBuilder sb = new StringBuilder();
        for (MessageComponent comp : message.getComponents()) {
            if (comp instanceof TextComponent) {
                sb.append(((TextComponent) comp).getSequence());
            }
        }
        sendToWriterThread(new ClientICMB(sequenceNumber++, buddy.getUsername(), sb.toString()));
    }

    /**
     * Add a buddy to the Buddy List
     * @param buddy Buddy to add to Buddy List
     */
    public void addToBuddyList(Buddy buddy) {
        sendToWriterThread(new AddBuddyCommand(sequenceNumber++, buddy));
    }

    /**
     * Delete a buddy from the Buddy List
     * @param buddy to delete from Buddy List
     */
    public void deleteFromBuddyList(Buddy buddy) {
        sendToWriterThread(new DeleteBuddyCommand(sequenceNumber++, buddy));
    }

    /**
     * Ignore a buddy
     * @param buddy Buddy to ignore
     */
    public void ignoreBuddy(Buddy buddy) {
        sendToWriterThread(new IgnoreBuddyCommand(sequenceNumber++, buddy));
    }

    /**
     * Unignore a buddy
     * @param buddy Buddy to unignore
     */
    public void unIgnoreBuddy(Buddy buddy) {
        sendToWriterThread(new UnignoreBuddyCommand(sequenceNumber++, buddy));
    }

    /**
     * Send typing started notification to a Buddy
     * @param buddy Buddy that you are typing a message to.
     */
    public void typingStarted(Buddy buddy) {
        sendToWriterThread(new TypingNotificationCommand(sequenceNumber++, buddy, SNACConstants.MTN_TYPING_STARTED));
    }

    /**
     * Send typing stopped notification to a Buddy
     * @param buddy Buddy that you stopped typing a message to.
     */
    public void typingStopped(Buddy buddy) {
        sendToWriterThread(new TypingNotificationCommand(sequenceNumber++, buddy, SNACConstants.MTN_TYPING_FINISHED));
    }

    /** */
    public void setListeners(IMSupport listeners) {
        this.listeners = listeners;
    }

    /** */
    public void changeStatus(Integer statusFlag) {
        sendToWriterThread(new ClientSetLocationInfoCommand(sequenceNumber++, statusFlag.intValue()));
    }

    /** */
    private IMListener defaultIMListener = new IMAdapter() {

        /** */
        public void eventHappened(IMEvent event) throws IOException {
            Name name = event.getName();
            if (name instanceof OscarEventName) {
                OscarEventName eventName = (OscarEventName) name;
                switch(eventName) {
                    case buddyListReceived:
                        {
                            @SuppressWarnings("unchecked") Map<String, List<String>> map = (Map<String, List<String>>) event.getArguments()[0];
                            OscarConnection.this.buddyListReceived(map);
                        }
                        break;
                    case reconnect:
                        {
                            String server = (String) event.getArguments()[0];
                            int port = (Integer) event.getArguments()[1];
                            byte[] cookie = (byte[]) event.getArguments()[2];
                            reconnect(server, port, cookie);
                        }
                        break;
                    case bosConnecting:
                        {
                            bosConnecting();
                        }
                        break;
                    case bosConnected:
                        {
                            bosConnected();
                        }
                        break;
                    case ackConnectRates:
                        {
                            ackConnectRates();
                        }
                        break;
                    case bosServiceList:
                        {
                            bosServiceList();
                        }
                        break;
                    case protocolNegotiation:
                        {
                            protocolNegotiation();
                        }
                        break;
                    case clientReady:
                        {
                            int flags = (Integer) event.getArguments()[0];
                            byte[] snacData = (byte[]) event.getArguments()[1];
                            clientReady(flags, snacData);
                        }
                        break;
                }
            }
        }
    };

    /**
     * Build list of Buddy objects to past to the listener.  We call this
     * as the last part of login.
     */
    private void buddyListReceived(Map<String, List<String>> map) throws IOException {
        List<Buddy> buddyList = new ArrayList<Buddy>();
        if (map != null) {
            for (String groupName : map.keySet()) {
                Group group = new Group();
                group.setName(groupName);
                for (String buddyName : map.get(groupName)) {
                    Buddy buddy = new Buddy(buddyName);
                    buddyList.add(buddy);
                    group.addBuddy(buddy);
                }
            }
        }
        Buddy[] buddies = new Buddy[buddyList.size()];
        buddies = buddyList.toArray(buddies);
        listeners.eventHappened(new IMEvent(this, IMEventName.buddyListReceived, (Object) buddies));
    }

    /** */
    private void reconnect(String server, int port, byte[] cookie) throws IOException {
        reconnect(server, port);
        status = Status.BOS_CONNECTING;
        log.info("status: " + status);
        sendToWriterThread(new BOSLoginCommand(cookie));
    }

    /** */
    private void bosConnecting() throws IOException {
        status = Status.BOS_CONNECTED;
        log.info("status: " + status);
    }

    /** */
    private void bosConnected() throws IOException {
        status = Status.BOS_SERVICE_LIST;
        log.info("status: " + status);
        sendToWriterThread(new ClientFamilyVersionCommand());
    }

    /** */
    private void ackConnectRates() throws IOException {
        sendToWriterThread(new BaseCommand(8, SNACConstants.SNAC_FAMILY_SERVER_STORED_INFO, SNACConstants.ACTIVATE_SERVER_SSI));
        sendToWriterThread(new ClientSetStatusCommand(9, 0));
        sendToWriterThread(new ClientReadyCommand());
        status = Status.CLIENT_READY;
        log.info("status: " + status);
        sendToWriterThread(new RequestICBMParmCommand(sequenceNumber++));
    }

    /** */
    private void bosServiceList() throws IOException {
        status = Status.PROTOCOL_NEGOTIATION;
        log.info("status: " + status);
        sendToWriterThread(new RequestServerRateCommand());
    }

    /** */
    private void protocolNegotiation() throws IOException {
        status = Status.ACK_CONNECT_RATES;
        log.info("status: " + status);
        sendToWriterThread(new AckServerRateInfo());
        sendToWriterThread(new ClientSetLocationInfoCommand());
        sendToWriterThread(new BaseCommand(7, SNACConstants.SNAC_FAMILY_SERVER_STORED_INFO, SNACConstants.REQUEST_CONTACT_LIST));
    }

    /** */
    private void clientReady(int flags, byte[] snacData) throws IOException {
        sendToWriterThread(new ClientSetICBMParmsCommand(sequenceNumber++, flags, snacData));
    }
}
