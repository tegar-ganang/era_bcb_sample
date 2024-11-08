package ch.iserver.ace.net.protocol;

import java.net.ConnectException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.InputDataStream;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.core.ProfileRegistry;
import org.beepcore.beep.core.RequestHandler;
import org.beepcore.beep.core.Session;
import org.beepcore.beep.lib.Reply;
import org.beepcore.beep.transport.tcp.TCPSession;
import org.beepcore.beep.transport.tcp.TCPSessionCreator;
import ch.iserver.ace.FailureCodes;
import ch.iserver.ace.ServerInfo;
import ch.iserver.ace.algorithm.TimestampFactory;
import ch.iserver.ace.collaboration.Participant;
import ch.iserver.ace.net.ParticipantPort;
import ch.iserver.ace.net.SessionConnectionCallback;
import ch.iserver.ace.net.core.NetworkProperties;
import ch.iserver.ace.net.core.NetworkServiceExt;
import ch.iserver.ace.net.core.NetworkServiceImpl;
import ch.iserver.ace.net.core.RemoteDocumentProxyExt;
import ch.iserver.ace.net.core.RemoteUserProxyExt;
import ch.iserver.ace.net.discovery.DiscoveryManagerFactory;
import ch.iserver.ace.util.ParameterValidator;
import ch.iserver.ace.util.SingleThreadDomain;

/**
 * The RemoteUserSession includes the session and connection management for
 * a remote user. All connections for a particular remote user are created, 
 * maintained and released in the RemoteUserSession.
 */
public class RemoteUserSession {

    private static Logger LOG = Logger.getLogger(RemoteUserSession.class);

    /**
	 * constant for channel type <code>main</code>
	 */
    public static final String CHANNEL_MAIN = "main";

    /**
	 * constant for channel type <code>main</code> but
	 * connection set up is forced by explicit discovery
	 */
    public static final String CHANNEL_DISCOVERY = "discovery";

    /**
	 * constant for channel type <code>session</code>
	 */
    public static final String CHANNEL_SESSION = "session";

    /**
	 * constant for channel type <code>proxy</code>
	 */
    public static final String CHANNEL_PROXY = "proxy";

    /**
	 * the SingleThreadDomain to wrap all ParticipantRequestHandlers
	 */
    private static SingleThreadDomain domain = new SingleThreadDomain();

    /**
	 * the host address of the remote user
	 */
    private InetAddress host;

    /**
	 * the port of the remote user
	 */
    private int port;

    /**
	 * the TCP session with the remote user
	 */
    private TCPSession session;

    /**
	 * the MainConnection
	 */
    private MainConnection mainConnection;

    /**
	 * Maps for all Participant- / and SessionConnections
	 */
    private Map participantConnections, sessionConnections;

    /**
	 * the remote user proxy
	 */
    private RemoteUserProxyExt user;

    /**
	 * flag to indicate state
	 */
    private boolean isInitiated;

    /**
	 * flag to indicate state
	 */
    private boolean isAlive;

    /**
	 * the timestamp factory
	 */
    private TimestampFactory factory;

    /**
	 * Creates a new RemoteUserSession. No <code>TCPSession</code> to 
	 * the remote user is yet available and thus no <code>MainConnection</code>.
	 * 
	 * @param address	the address of the remote user
	 * @param port		the port
	 * @param user		the remote user proxy
	 */
    public RemoteUserSession(InetAddress address, int port, RemoteUserProxyExt user) {
        ParameterValidator.notNull("address", address);
        ParameterValidator.notNegative("port", port);
        this.host = address;
        this.port = port;
        this.user = user;
        setTCPSession(null);
        isInitiated = false;
        isAlive = true;
        participantConnections = Collections.synchronizedMap(new LinkedHashMap());
        sessionConnections = Collections.synchronizedMap(new LinkedHashMap());
    }

    /**
	 * Creates a new RemoteUserSession. The <code>TCPSession</code> has been established
	 * and the <code>MainConnection</code> initiated.
	 * 
	 * @param session		the TCPSession
	 * @param connection		the MainConnection
	 * @param user			the remote user proxy
	 */
    public RemoteUserSession(TCPSession session, MainConnection connection, RemoteUserProxyExt user) {
        ParameterValidator.notNull("session", session);
        ParameterValidator.notNull("connection", connection);
        ParameterValidator.notNull("user", user);
        this.mainConnection = connection;
        this.user = user;
        this.host = session.getSocket().getInetAddress();
        setTCPSession(session);
        isInitiated = true;
        isAlive = true;
        participantConnections = Collections.synchronizedMap(new LinkedHashMap());
        sessionConnections = Collections.synchronizedMap(new LinkedHashMap());
    }

    /**
	 * Sets the timestamp factory.
	 * 
	 * @param factory the timestamp factory to set
	 */
    public void setTimestampFactory(TimestampFactory factory) {
        this.factory = factory;
    }

    /**
	 * Gets the timestamp factory.
	 * 
	 * @return the timestamp factory
	 */
    public TimestampFactory getTimestampFactory() {
        return factory;
    }

    /**
	 * 
	 * @param isDiscovery
	 * @return
	 * @throws ConnectionException
	 */
    public synchronized MainConnection startMainConnection(boolean isDiscovery) throws ConnectionException {
        if (!isAlive) throw new ConnectionException("session has been ended");
        if (!isInitiated()) initiateTCPSession();
        if (!isDiscovery) {
            DiscoveryManagerFactory.getDiscoveryManager().setSessionEstablished(user.getId());
        }
        if (mainConnection == null) {
            Channel channel = null;
            if (isDiscovery) {
                channel = setUpChannel(CHANNEL_DISCOVERY, null, null);
            } else {
                channel = setUpChannel(CHANNEL_MAIN, null, null);
            }
            LOG.debug("main channel to [" + user.getUserDetails().getUsername() + "] started");
            mainConnection = new MainConnection(channel);
        } else {
            LOG.debug("main channel to [" + user.getUserDetails().getUsername() + "] available");
        }
        return mainConnection;
    }

    /**
	 * Gets the <code>MainConnection</code>. If the session has not been initiated, the
	 * <code>TCPSession</code> is created and then the <code>MainConnection</code> started.
	 * Otherwise, the <code>MainConnection</code> is simply returned.
	 * 
	 * If the session has been cleaned up (call to {@link #cleanup()}), a 
	 * <code>ConnectionExeption</code> is thrown.
	 * 
	 * @return the MainConnection
	 * @throws ConnectionException	if the session is shut down
	 */
    public MainConnection getMainConnection() throws ConnectionException {
        return startMainConnection(false);
    }

    /**
	 * Starts a new Channel to the remote user using this session. The channel is configured with
	 * the appropriate request handler according to the given <code>type</code>.
	 * 
	 * @param type 	the type of the channel
	 * @param port 	the ParticipantPort for the ParticipantRequestHandler (optional)
	 * @param docId 	to open a session channel a document id can be passed
	 * @return	the created Channel
	 * @throws ConnectionException	if an error occurs
	 */
    public Channel setUpChannel(String type, ParticipantConnectionImpl connection, String docId) throws ConnectionException {
        LOG.debug("--> setUpChannel(type=" + type + ")");
        Channel channel = startChannel(type, connection);
        try {
            String channelType = getChannelTypeXML(type, docId);
            byte[] data = channelType.getBytes(NetworkProperties.get(NetworkProperties.KEY_DEFAULT_ENCODING));
            OutputDataStream output = DataStreamHelper.prepare(data);
            LOG.debug("--> sendMSG() for channel type");
            Reply reply = new Reply();
            channel.sendMSG(output, reply);
            if (type.equals(CHANNEL_DISCOVERY)) {
                InputDataStream response = reply.getNextReply().getDataStream();
                byte[] rawData = DataStreamHelper.read(response);
                ResponseParserHandler handler = new ResponseParserHandler();
                DeserializerImpl.getInstance().deserialize(rawData, handler);
                Request result = handler.getResult();
                if (result.getType() == ProtocolConstants.USER_DISCOVERY) {
                    getUser().setId(result.getUserId());
                    String name = (String) result.getPayload();
                    getUser().getMutableUserDetails().setUsername(name);
                } else {
                    LOG.error("unknown response type parsed.");
                }
            } else {
                reply.getNextReply();
            }
            LOG.debug("<-- sendMSG()");
            LOG.debug("<-- setUpChannel()");
            return channel;
        } catch (Exception be) {
            LOG.error("could not start channel [" + be + "]");
            throw new ConnectionException("could not start channel");
        }
    }

    /**
	 * Starts a new Channel to the remote user using this session. The channel is configured with
	 * the appropriate request handler according to the given <code>type</code> but no message is sent to
	 * the peer.
	 * 
	 * @param type
	 * @param connection
	 * @param docId
	 * @return
	 * @throws ConnectionException
	 */
    private Channel startChannel(String type, ParticipantConnectionImpl connection) throws ConnectionException {
        try {
            RequestHandler handler = null;
            if (type == CHANNEL_MAIN) {
                handler = ProfileRegistryFactory.getMainRequestHandler();
            } else if (type == CHANNEL_SESSION) {
                CollaborationDeserializer deserializer = new CollaborationDeserializer();
                CollaborationParserHandler parserHandler = new CollaborationParserHandler();
                parserHandler.setTimestampFactory(getTimestampFactory());
                NetworkServiceExt service = NetworkServiceImpl.getInstance();
                ParticipantRequestHandler pHandler = new ParticipantRequestHandler(deserializer, getTimestampFactory(), service);
                if (connection != null) {
                    pHandler.setParticipantConnection(connection);
                    pHandler.setParticipantPort(connection.getParticipantPort());
                }
                handler = (RequestHandler) domain.wrap(pHandler, RequestHandler.class);
            } else {
                throw new IllegalStateException("unknown channel type [" + type + "]");
            }
            String uri = NetworkProperties.get(NetworkProperties.KEY_PROFILE_URI);
            Channel channel = session.startChannel(uri, handler);
            return channel;
        } catch (Exception be) {
            LOG.error("could not start channel [" + be + "]");
            throw new ConnectionException("could not start channel");
        }
    }

    /**
	 * Adds a <code>SessionConnection</code> to this RemoteUserSession.
	 * 
	 * @param docId		the id of the shared document
	 * @param collaborationChannel	the channel around which the SessionConnection will be wrapped
	 * @return	the SessionConnectionImpl object
	 */
    public SessionConnectionImpl addSessionConnection(String docId, Channel collaborationChannel) {
        LOG.debug("--> addSessionConnection() for doc [" + docId + "]");
        String username = getUser().getUserDetails().getUsername();
        SessionConnectionImpl conn = new SessionConnectionImpl(docId, collaborationChannel, ResponseListener.getInstance(), new CollaborationSerializer(), username, getUser().getId());
        sessionConnections.put(docId, conn);
        LOG.debug(sessionConnections.size() + " SessionConnection(s) with " + username);
        LOG.debug("<-- addSessionConnection()");
        return conn;
    }

    /**
	 * Removes a SessionConnection from this RemoteUserSession given the document id.
	 * 
	 * @param docId	the document id of the SessionConnection
	 * @return	the removed SessionConnectionImpl object
	 */
    public SessionConnectionImpl removeSessionConnection(String docId) {
        SessionConnectionImpl connection = (SessionConnectionImpl) sessionConnections.remove(docId);
        if (connection == null) {
            LOG.warn("SessionConnection to remove for [" + docId + "] is already removed");
        } else {
            connection.cleanup();
        }
        LOG.debug(sessionConnections.size() + " SessionConnection(s) with " + getUser().getUserDetails().getUsername() + " remain.");
        return connection;
    }

    /**
	 * Checks whether this RemoteUserSession has a SessionConnection for
	 * the given document.
	 * 
	 * @param docId	the document id to check for an available SessionConnection
	 * @return	true iff a SessionConnection is available
	 */
    public boolean hasSessionConnection(String docId) {
        return sessionConnections.containsKey(docId);
    }

    /**
	 * Gets a SessionConnection to this RemoteUserSession's user.
	 * 
	 * @param docId 	the document id to get the SessionConnection for
	 * @return	the SessionConnectionImpl object
	 */
    public SessionConnectionImpl getSessionConnection(String docId) {
        return (SessionConnectionImpl) sessionConnections.get(docId);
    }

    /**
	 * Adds a ParticipantConnection.
	 * 
	 * @param docId 	the document id the created ParticipantConnection is about
	 * @return	the created ParticipantConnectionImpl
	 */
    public ParticipantConnectionImpl addParticipantConnection(String docId) {
        LOG.debug("--> createParticipantConnection() for doc [" + docId + "] to [" + getUser().getUserDetails().getUsername() + "]");
        assert !participantConnections.containsKey(docId);
        ParticipantConnectionImpl connection = ParticipantConnectionImplFactory.getInstance().createConnection(docId, this, ResponseListener.getInstance(), new CollaborationSerializer());
        participantConnections.put(docId, connection);
        LOG.debug(participantConnections.size() + " ParticipantConnection(s) for " + getUser().getUserDetails().getUsername());
        LOG.debug("<-- createParticipantConnection()");
        return connection;
    }

    /**
	 * Removes a ParticipantConnection from this RemoteUserSession.
	 * 
	 * @param docId	the document id for which the ParticipantConnection is to be removed
	 * @return	the removed ParticipantConnectionImpl object
	 */
    public ParticipantConnectionImpl removeParticipantConnection(String docId) {
        ParticipantConnectionImpl conn = (ParticipantConnectionImpl) participantConnections.remove(docId);
        conn.cleanup();
        LOG.debug(participantConnections.size() + " ParticipantConnection(s) for " + getUser().getUserDetails().getUsername() + " remain.");
        return conn;
    }

    /**
	 * Checks whether a ParticipantConnection is available for the given
	 * document id.
	 * 
	 * @param docId	the document id to check for a ParticipantConnection
	 * @return	true iff a ParticipantConnection is available
	 */
    public boolean hasParticipantConnection(String docId) {
        return participantConnections.containsKey(docId);
    }

    /**
	 * Gets the ParticipantConnection for the given document id or null
	 * if none is available.
	 * 
	 * @param docId  the document id to get the ParticipantConnection for
	 * @return	the ParticipantConnectionImpl object for the document id
	 */
    public ParticipantConnectionImpl getParticipantConnection(String docId) {
        ParticipantConnectionImpl conn = (ParticipantConnectionImpl) participantConnections.get(docId);
        if (conn == null) {
            LOG.debug("ParticipantConnection for docId [" + docId + "] not found.");
        }
        return conn;
    }

    /**
	 * Cleans up the session. No methods may be called
	 * after a call to <code>cleanup()</code>. This method
	 * is called when the BEEP session has terminated already.
	 */
    public void cleanup() {
        LOG.debug("--> cleanup()");
        LOG.debug(participantConnections.size() + " pConnection(s)");
        Iterator iter = participantConnections.values().iterator();
        while (iter.hasNext()) {
            ParticipantConnectionImpl conn = (ParticipantConnectionImpl) iter.next();
            ParticipantPort port = conn.getParticipantPort();
            conn.cleanup();
            port.leave();
        }
        participantConnections = null;
        LOG.debug(sessionConnections.size() + " sessionConnection(s)");
        iter = sessionConnections.values().iterator();
        while (iter.hasNext()) {
            SessionConnectionImpl conn = (SessionConnectionImpl) iter.next();
            RemoteDocumentProxyExt doc = getUser().getSharedDocument(conn.getDocumentId());
            SessionConnectionCallback callback = doc.getSessionConnectionCallback();
            conn.cleanup();
            if (getUser().getId().equals(doc.getPublisher().getId())) {
                callback.sessionTerminated();
            } else {
                callback.participantLeft(conn.getParticipantId(), Participant.DISCONNECTED);
            }
        }
        sessionConnections = null;
        mainConnection = null;
        setTCPSession(null);
        user = null;
        factory = null;
        isAlive = false;
        LOG.debug("<-- cleanup()");
    }

    /**
	 * Closes the active <code>TCPSession</code> and the <code>MainConnection</code>. This method must
	 * be called when the TCPSession is still active.
	 */
    public synchronized void close() {
        LOG.debug("--> close()");
        if (session != null && session.getState() == Session.SESSION_STATE_ACTIVE) {
            try {
                mainConnection.close();
                final Thread t = Thread.currentThread();
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {

                    public void run() {
                        LOG.debug("going to interrupt [" + t.getName() + "]");
                        t.interrupt();
                        LOG.debug("interrupted.");
                    }
                }, 2000);
                LOG.debug("--> session.close()");
                session.close();
                timer.cancel();
                LOG.debug("<-- session.close()");
            } catch (Exception be) {
                LOG.warn("could not close active session [" + be + ", " + be.getMessage() + "]");
            }
        }
        user = null;
        isAlive = false;
        LOG.debug("<-- close()");
    }

    /**
	 * Checks whether this RemoteUserSession is still alive i.e. the
	 * session to the remote user is in active state.
	 * 
	 * @return true if the session is still alive
	 */
    public synchronized boolean isAlive() {
        return isAlive;
    }

    /**
	 * Determines whether this RemoteUserSession has its <code>TCPSession</code> 
	 * established with the remote user or not.
	 * 
	 * @return true if the session with the remote user is established
	 */
    public boolean isInitiated() {
        return isInitiated;
    }

    /**
	 * Gets this RemoteUserSession's user.
	 * 
	 * @return the RemoteUserProxyExt of this session
	 */
    public RemoteUserProxyExt getUser() {
        return user;
    }

    /**
	 * Gets the target host of this session.
	 * 
	 * @return the target host of this session 
	 */
    public InetAddress getHost() {
        return host;
    }

    /**
	 * Gets the port of the target host.
	 * 
	 * @return	the port of the target host
	 */
    public int getPort() {
        return port;
    }

    /**
	 * Sets the TCPSession for this RemoteUserSession.
	 * 
	 * @param session	the TCPSession to set
	 */
    private void setTCPSession(TCPSession session) {
        this.session = session;
        if (getUser() != null) getUser().setSessionEstablished((session != null));
    }

    /**
	 * Helper method to initiate the TCPSession for this 
	 * RemoteUserSession.
	 *
	 * @see TCPSession
	 * @throws ConnectionException 	if an error occurs
	 */
    private void initiateTCPSession() throws ConnectionException {
        LOG.debug("--> initiateTCPSession()");
        try {
            ProfileRegistry registry = ProfileRegistryFactory.getProfileRegistry();
            LOG.info("initiate session to " + host + ":" + port + "...");
            TCPSession newSession = TCPSessionCreator.initiate(host, port, registry);
            LOG.debug("initiated session.");
            setTCPSession(newSession);
            isInitiated = true;
        } catch (BEEPException be) {
            LOG.error("could not initiate session [" + be + "]");
            if (be.getCause() instanceof ConnectException) {
                String msg = getUser().getUserDetails().getUsername() + "[" + host + ":" + port + "]";
                NetworkServiceImpl.getInstance().getCallback().serviceFailure(FailureCodes.CONNECTION_REFUSED, msg, be);
            }
            throw new ConnectionException("session init failed [" + be.getMessage() + "]");
        }
        LOG.debug("<-- initiateTCPSession()");
    }

    /**
	 * Returns the XML message to send to the peer after initialization 
	 * of the channel.
	 * 
	 * @param type	the type of the channel
	 * @param docId	the document id (only for ParticipantConnections)
	 * @return	the XML message
	 */
    private String getChannelTypeXML(String type, String docId) {
        String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        if (type.equals(CHANNEL_MAIN)) {
            result += "<ace><channel type=\"" + type + "\"/></ace>";
        } else if (type.equals(CHANNEL_DISCOVERY)) {
            NetworkServiceImpl service = NetworkServiceImpl.getInstance();
            String userid = service.getUserId();
            String username = service.getUserDetails().getUsername();
            ServerInfo info = service.getServerInfo();
            String address = info.getAddress().getHostAddress();
            String port = Integer.toString(info.getPort());
            String user = "<user id=\"" + userid + "\" name=\"" + username + "\" address=\"" + address + "\" port=\"" + port + "\"/>";
            result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<ace><channel type=\"" + CHANNEL_MAIN + "\">" + user + "</channel></ace>";
        } else if (type.equals(CHANNEL_SESSION)) {
            String id = NetworkServiceImpl.getInstance().getUserId();
            result += "<ace><channel type=\"" + type + "\" docId=\"" + docId + "\" userid=\"" + id + "\"/></ace>";
        }
        return result;
    }
}
