package ch.iserver.ace.net.impl.protocol;

import java.io.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.InputDataStream;
import org.beepcore.beep.core.MessageMSG;
import org.beepcore.beep.core.RequestHandler;
import org.beepcore.beep.transport.tcp.TCPSession;
import org.beepcore.beep.util.BufferSegment;
import ch.iserver.ace.net.impl.RemoteUserProxyExt;
import ch.iserver.ace.net.impl.discovery.DiscoveryManager;
import ch.iserver.ace.net.impl.discovery.DiscoveryManagerFactory;

/**
 *
 */
public class RequestHandlerImpl implements RequestHandler {

    private static Logger LOG = Logger.getLogger(RequestHandlerImpl.class);

    private static Object MUTEX = new Object();

    private RequestFilter filter;

    private Deserializer deserializer;

    private RequestParserHandler handler;

    public RequestHandlerImpl(Deserializer deserializer, RequestFilter filter) {
        this.deserializer = deserializer;
        this.filter = filter;
        handler = new RequestParserHandler();
    }

    /**
	 * @see org.beepcore.beep.core.RequestHandler#receiveMSG(org.beepcore.beep.core.MessageMSG)
	 */
    public void receiveMSG(MessageMSG message) {
        LOG.debug("--> receiveMSG");
        InputDataStream input = message.getDataStream();
        try {
            byte[] rawData = readData(input);
            LOG.debug("received " + rawData.length + " bytes. [" + (new String(rawData)) + "]");
            Request request = null;
            synchronized (MUTEX) {
                deserializer.deserialize(rawData, handler);
                request = (Request) handler.getResult();
                String userid = request.getUserId();
                DiscoveryManager discoveryManager = DiscoveryManagerFactory.getDiscoveryManager(null);
                if (!discoveryManager.hasSessionEstablished(userid)) {
                    RemoteUserProxyExt user = discoveryManager.getUser(userid);
                    LOG.debug("create new session for [" + user.getMutableUserDetails().getUsername() + "]");
                    SessionManager manager = SessionManager.getInstance();
                    manager.createSession(user, (TCPSession) message.getChannel().getSession());
                }
            }
            request.setMessage(message);
            filter.process(request);
        } catch (Exception e) {
            LOG.error("could not process request [" + e + "]");
        }
        LOG.debug("<-- receiveMSG");
    }

    private byte[] readData(InputDataStream stream) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        do {
            BufferSegment b = stream.waitForNextSegment();
            if (b == null) {
                out.flush();
                break;
            }
            out.write(b.getData());
        } while (!stream.isComplete());
        return out.toByteArray();
    }
}
