package ch.iserver.ace.net.protocol;

import org.apache.log4j.Logger;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.InputDataStream;
import org.beepcore.beep.core.MessageMSG;
import org.beepcore.beep.core.RequestHandler;
import org.beepcore.beep.transport.tcp.TCPSession;
import ch.iserver.ace.net.core.MutableUserDetails;
import ch.iserver.ace.net.core.NetworkServiceImpl;
import ch.iserver.ace.net.core.RemoteUserProxyExt;
import ch.iserver.ace.net.core.RemoteUserProxyFactory;
import ch.iserver.ace.net.discovery.DiscoveryManager;
import ch.iserver.ace.net.discovery.DiscoveryManagerFactory;
import ch.iserver.ace.net.protocol.filter.RequestFilter;

/**
 * MainRequestHandler is the main request handler for the 
 * <code>MainConnection</code> connection to a peer.
 * It forwards the parsed <code>Request</code>'s to the 
 * given Request filter chain.
 * 
 * @see ch.iserver.ace.net.protocol.MainConnection
 * @see org.beepcore.beep.core.RequestHandler
 */
public class MainRequestHandler implements RequestHandler {

    private static Logger LOG = Logger.getLogger(MainRequestHandler.class);

    private static final String UNKNOWN_USER = "unknown (resolving...)";

    /**
	 * the request filter to send the requests
	 */
    private RequestFilter filter;

    /**
	 * the deserializer implementation for deserialization
	 */
    private Deserializer deserializer;

    /**
	 * the request parser handler to parse
	 * the message and create the Request object
	 */
    private RequestParserHandler handler;

    /**
	 * Creates a new MainRequestHandler.
	 * 
	 * @param deserializer	the deserializer to use
	 * @param filter			the request filter chain to forward requests
	 * @param handler		the request parse handler
	 */
    public MainRequestHandler(Deserializer deserializer, RequestFilter filter, RequestParserHandler handler) {
        this.deserializer = deserializer;
        this.filter = filter;
        this.handler = handler;
    }

    /**
	 * @see org.beepcore.beep.core.RequestHandler#receiveMSG(org.beepcore.beep.core.MessageMSG)
	 */
    public void receiveMSG(MessageMSG message) {
        LOG.debug("--> receiveMSG(" + message + ")");
        try {
            Request request = null;
            if (!NetworkServiceImpl.getInstance().isStopped()) {
                InputDataStream input = message.getDataStream();
                byte[] rawData = DataStreamHelper.read(input);
                LOG.debug("received " + rawData.length + " bytes. [" + (new String(rawData)) + "]");
                deserializer.deserialize(rawData, handler);
                request = handler.getResult();
                String userid = request.getUserId();
                DiscoveryManager discoveryManager = DiscoveryManagerFactory.getDiscoveryManager();
                RemoteUserProxyExt user = discoveryManager.getUser(userid);
                if (user == null) {
                    if (request.getType() != ProtocolConstants.CONCEAL) {
                        LOG.debug("add new RemoteUserProxy for [" + userid + "] [" + request.getType() + "]");
                        MutableUserDetails details = new MutableUserDetails(UNKNOWN_USER);
                        user = RemoteUserProxyFactory.getInstance().createProxy(userid, details);
                        discoveryManager.addUser(user);
                    } else {
                        LOG.debug("user null and requestType == CONCEAL, ignore request");
                        request = new RequestImpl(ProtocolConstants.NULL, null, null);
                    }
                }
                if (!discoveryManager.hasSessionEstablished(userid) && request.getType() != ProtocolConstants.NULL) {
                    LOG.debug("create RemoteUserSession for [" + user.getMutableUserDetails().getUsername() + "]");
                    SessionManager manager = SessionManager.getInstance();
                    Channel mainChannel = message.getChannel();
                    manager.createSession(user, (TCPSession) mainChannel.getSession(), mainChannel);
                }
            } else {
                request = new RequestImpl(ProtocolConstants.SHUTDOWN, null, null);
            }
            request.setMessage(message);
            filter.process(request);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("could not process request [" + e + "]");
        }
        LOG.debug("<-- receiveMSG");
    }
}
