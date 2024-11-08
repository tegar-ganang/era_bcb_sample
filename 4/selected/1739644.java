package ch.iserver.ace.net.protocol;

import org.apache.log4j.Logger;
import org.beepcore.beep.core.AbortChannelException;
import org.beepcore.beep.core.InputDataStream;
import org.beepcore.beep.core.Message;
import org.beepcore.beep.core.ReplyListener;
import org.beepcore.beep.transport.tcp.TCPSession;
import ch.iserver.ace.net.core.NetworkProperties;
import ch.iserver.ace.net.protocol.filter.RequestFilter;
import ch.iserver.ace.util.ParameterValidator;

/**
 * Default implementation for interface <code>ReplyListener</code>.
 * ResponseListener listens for responses in a BEEP Core Channel 
 * communication.
 */
public class ResponseListener implements ReplyListener {

    private static Logger LOG = Logger.getLogger(ResponseListener.class);

    private static ResponseListener instance;

    private Deserializer deserializer;

    private ResponseParserHandler handler;

    private RequestFilter filter;

    private ResponseListener() {
        handler = new ResponseParserHandler();
    }

    public static ResponseListener getInstance() {
        if (instance == null) {
            instance = new ResponseListener();
        }
        return instance;
    }

    public void init(Deserializer deserializer, RequestFilter filter) {
        ParameterValidator.notNull("deserializer", deserializer);
        ParameterValidator.notNull("filter", filter);
        this.deserializer = deserializer;
        this.filter = filter;
    }

    /**
	 * 
	 * @see org.beepcore.beep.core.ReplyListener#receiveRPY(org.beepcore.beep.core.Message)
	 */
    public void receiveRPY(Message message) throws AbortChannelException {
        String result = "n/a";
        try {
            InputDataStream stream = message.getDataStream();
            byte[] data = DataStreamHelper.read(stream);
            result = new String(data, NetworkProperties.get(NetworkProperties.KEY_DEFAULT_ENCODING));
        } catch (Exception e) {
            LOG.error("could not read stream [" + e + "]");
        }
        LOG.debug("receiveRPY(" + result + ")");
    }

    public void receiveERR(Message message) throws AbortChannelException {
        LOG.error("receiveERR(" + message + ")");
    }

    public void receiveANS(Message message) throws AbortChannelException {
        LOG.debug("receivedANS() -> not intended!");
    }

    public void receiveNUL(Message message) throws AbortChannelException {
        Object appData = message.getChannel().getAppData();
        TCPSession session = (TCPSession) message.getChannel().getSession();
        LOG.debug("received confirmation from [" + appData + ", " + session.getSocket() + "]");
    }
}
