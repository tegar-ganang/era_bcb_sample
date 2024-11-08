package ch.iserver.ace.net.impl.protocol;

import org.apache.log4j.Logger;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.InputDataStream;
import org.beepcore.beep.core.MessageMSG;
import org.beepcore.beep.transport.tcp.TCPSession;
import ch.iserver.ace.net.impl.MutableUserDetails;
import ch.iserver.ace.net.impl.NetworkServiceImpl;
import ch.iserver.ace.net.impl.RemoteUserProxyExt;
import ch.iserver.ace.net.impl.RemoteUserProxyFactory;
import ch.iserver.ace.net.impl.discovery.DiscoveryManager;
import ch.iserver.ace.net.impl.discovery.DiscoveryManagerFactory;

/**
 *
 */
public class MainRequestHandler extends AbstractRequestHandler {

    private static Logger LOG = Logger.getLogger(MainRequestHandler.class);

    private RequestFilter filter;

    private Deserializer deserializer;

    private RequestParserHandler handler;

    public MainRequestHandler(Deserializer deserializer, RequestFilter filter, RequestParserHandler handler) {
        this.deserializer = deserializer;
        this.filter = filter;
        this.handler = handler;
    }

    /**
	 * @see org.beepcore.beep.core.RequestHandler#receiveMSG(org.beepcore.beep.core.MessageMSG)
	 */
    public void receiveMSG(MessageMSG message) {
        LOG.debug("--> receiveMSG");
        try {
            Request request = null;
            if (!NetworkServiceImpl.getInstance().isStopped()) {
                InputDataStream input = message.getDataStream();
                byte[] rawData = DataStreamHelper.read(input);
                LOG.debug("received " + rawData.length + " bytes. [" + (new String(rawData)) + "]");
                deserializer.deserialize(rawData, handler);
                request = handler.getResult();
                String userid = request.getUserId();
                DiscoveryManager discoveryManager = DiscoveryManagerFactory.getDiscoveryManager(null);
                RemoteUserProxyExt user = discoveryManager.getUser(userid);
                if (user == null) {
                    LOG.debug("add new RemoteUserProxy for [" + userid + "]");
                    MutableUserDetails details = new MutableUserDetails("unknown (resolving...)");
                    user = RemoteUserProxyFactory.getInstance().createProxy(userid, details);
                    discoveryManager.addUser(user);
                }
                if (!discoveryManager.hasSessionEstablished(userid)) {
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

    public void cleanup() {
        throw new UnsupportedOperationException();
    }

    protected Logger getLogger() {
        return LOG;
    }
}
