package ch.iserver.ace.net.protocol;

import org.apache.log4j.Logger;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.InputDataStream;
import org.beepcore.beep.core.MessageMSG;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.core.RequestHandler;
import org.beepcore.beep.transport.tcp.TCPSession;
import ch.iserver.ace.net.core.NetworkProperties;
import ch.iserver.ace.net.core.NetworkServiceImpl;
import ch.iserver.ace.net.core.RemoteUserProxyExt;
import ch.iserver.ace.net.discovery.DiscoveryManagerFactory;
import ch.iserver.ace.net.protocol.RequestImpl.DocumentInfo;

/**
 * This is the default request handler. It is used as the first request handler
 * set to all channels. Its main purpose is to parse the first message sent over
 * the channel and to determine and set the correct RequestHandler for the channel, being
 * either <code>MainConnection</code> or <code>SessionRequestHandler</code>.
 * 
 * @see org.beepcore.beep.core.RequestHandler
 */
public class DefaultRequestHandler implements RequestHandler {

    private static Logger LOG = Logger.getLogger(DefaultRequestHandler.class);

    /**
	 * static object for synchronization
	 */
    private static Object MUTEX = new Object();

    /**
	 * the main handler instance
	 */
    private RequestHandler mainHandler;

    /**
	 * the deserializer to deserialize the messages
	 */
    private Deserializer deserializer;

    /**
	 * the parser handler to parse the messages
	 */
    private ParserHandler handler;

    /**
	 * Creates a new DefaultRequestHandler.
	 * 
	 * @param mainHandler		the main handler
	 * @param deserializer	the deserializer implementation
	 * @param handler		a ParserHandler to create the Request object
	 */
    public DefaultRequestHandler(RequestHandler mainHandler, Deserializer deserializer, ParserHandler handler) {
        this.mainHandler = mainHandler;
        this.deserializer = deserializer;
        this.handler = handler;
    }

    /**
	 * @see org.beepcore.beep.core.RequestHandler#receiveMSG(org.beepcore.beep.core.MessageMSG)
	 */
    public void receiveMSG(MessageMSG message) {
        LOG.debug("--> receiveMSG(" + message + ")");
        try {
            boolean isDiscovery = false;
            RequestHandler requestHandler = null;
            RemoteUserProxyExt proxy = null;
            Channel channel = null;
            if (!NetworkServiceImpl.getInstance().isStopped()) {
                InputDataStream input = message.getDataStream();
                byte[] rawData = DataStreamHelper.read(input);
                Request response = null;
                synchronized (MUTEX) {
                    deserializer.deserialize(rawData, handler);
                    response = handler.getResult();
                }
                channel = message.getChannel();
                int type = response.getType();
                if (type == ProtocolConstants.CHANNEL_MAIN) {
                    requestHandler = mainHandler;
                    proxy = (RemoteUserProxyExt) response.getPayload();
                    isDiscovery = (proxy != null);
                } else if (type == ProtocolConstants.CHANNEL_SESSION) {
                    DocumentInfo info = (DocumentInfo) response.getPayload();
                    requestHandler = SessionRequestHandlerFactory.getInstance().createHandler(info);
                } else {
                    LOG.warn("unkown channel type, use main as default");
                    requestHandler = mainHandler;
                }
                channel.setRequestHandler(requestHandler);
            }
            try {
                if (isDiscovery) {
                    LOG.debug("discovered new user: " + proxy);
                    processDiscoveredUser(message, channel, proxy);
                } else {
                    OutputDataStream os = new OutputDataStream();
                    os.setComplete();
                    message.sendRPY(os);
                }
            } catch (Exception e) {
                LOG.error("could not send confirmation [" + e + ", " + e.getMessage() + "]");
            }
            cleanup();
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("could not process request [" + e + "]");
        }
        LOG.debug("<-- receiveMSG()");
    }

    /**
	 * Sends a reply with this user's coordinates and adds the user to this ACE editor instance.
	 * 
	 * @param message	the message
	 * @param channel	the channel
	 * @param proxy		the user to send the reply
	 * @throws Exception	if an exception occurs
	 */
    private void processDiscoveredUser(MessageMSG message, Channel channel, RemoteUserProxyExt proxy) throws Exception {
        String id = NetworkServiceImpl.getInstance().getUserId();
        String name = NetworkServiceImpl.getInstance().getUserDetails().getUsername();
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ace><response><user id=\"" + id + "\" name=\"" + name + "\"/></response></ace>";
        byte[] data = xml.getBytes(NetworkProperties.get(NetworkProperties.KEY_DEFAULT_ENCODING));
        OutputDataStream output = DataStreamHelper.prepare(data);
        message.sendRPY(output);
        proxy.setDNSSDdiscovered(false);
        DiscoveryManagerFactory.getDiscoveryManager().addUser(proxy);
        SessionManager.getInstance().createSession(proxy, (TCPSession) channel.getSession(), channel);
    }

    /**
	 * Cleans up this DefaultRequestHandler and releases
	 * its resources.
	 */
    public void cleanup() {
        mainHandler = null;
        deserializer = null;
        handler = null;
    }
}
