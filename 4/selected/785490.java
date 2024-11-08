package org.beepcore.beep.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 */
class MessageAssembler implements RequestHandler {

    private Log log = LogFactory.getLog(this.getClass());

    /**
	 * RequestHandler to which the assembled messages will be forwarded.
	 */
    private RequestHandler handler;

    private int msgCount = 1;

    /**
	 * The message which is being assembled
	 */
    private MessageMSG currMsg;

    public MessageAssembler(RequestHandler handler) {
        this.handler = handler;
    }

    /**
	 * {@inheritDoc}
	 */
    public synchronized void receiveMSG(MessageMSG message) {
        log.debug("--> receiveMsg(" + message + ", " + message.getMessageType() + ", " + msgCount + ")");
        InputDataStream stream = message.getDataStream();
        if (currMsg == null) {
            currMsg = new MessageMSGImpl((ChannelImpl) message.getChannel(), message.getMsgno(), new InputDataStream());
        }
        if (stream.availableSegment()) {
            log.debug("--> add data");
            currMsg.getDataStream().add(stream.getNextSegment());
            log.debug("<-- add data");
        }
        if (stream.isComplete() && !stream.availableSegment()) {
            log.debug("message complete, pass to actual handler [" + currMsg.getDataStream().available() + " bytes]");
            if (!currMsg.getDataStream().isComplete()) {
                currMsg.getDataStream().setComplete();
            }
            handler.receiveMSG(currMsg);
            currMsg = null;
        }
        log.debug("<-- receiveMsg(" + msgCount + ")");
        ++msgCount;
    }

    public RequestHandler getRequestHandler() {
        return handler;
    }
}
