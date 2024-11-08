package org.beepcore.beep.core;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.beepcore.beep.util.BufferSegment;
import edu.oswego.cs.dl.util.concurrent.PooledExecutor;

/**
 * ChannelImpl is a conduit for a certain kind of traffic over a session,
 * determined by the profile of the channel.  Channels are created by Session
 *
 * @author Eric Dixon
 * @author Huston Franklin
 * @author Jay Kint
 * @author Scott Pead
 * @version $Revision: 1.10 $, $Date: 2003/11/07 17:39:21 $
 *
 */
class ChannelImpl implements Channel, Runnable {

    private static final BufferSegment zeroLengthSegment = new BufferSegment(new byte[0]);

    private static final PooledExecutor callbackQueue = new PooledExecutor();

    static final int DEFAULT_WINDOW_SIZE = 4096;

    static final RequestHandler defaultHandler = new DefaultMSGHandler();

    private Log log = LogFactory.getLog(this.getClass());

    /** syntax of messages */
    private String profile;

    /** encoding of used by profile */
    private String encoding;

    /** channel number on the session */
    private String number;

    /** Used to pass data sent on the Start Channel request */
    private String startData;

    /** receiver of MSG messages */
    private RequestHandler handler;

    /** number of last message sent */
    private int lastMessageSent;

    /** sequence number for messages sent */
    private long sentSequence;

    /** sequence for messages received */
    private long recvSequence;

    /** messages waiting for replies */
    private List sentMSGQueue;

    /** MSG we've received by awaiting processing of a former MSG */
    private LinkedList recvMSGQueue;

    /** messages queued to be sent */
    private LinkedList pendingSendMessages;

    /** session this channel sends through. */
    private SessionImpl session;

    /** message that we are receiving frames */
    private LinkedList recvReplyQueue;

    private int state = STATE_INITIALIZED;

    private Frame previousFrame;

    /** size of the peer's receive buffer */
    private int peerWindowSize;

    /** size of the receive buffer */
    private int recvWindowSize;

    /** amount of the buffer in use */
    private int recvWindowUsed;

    private int recvWindowFreed;

    private Object applicationData = null;

    private boolean tuningProfile = false;

    public String toString() {
        return super.toString() + " (#" + getNumberAsString() + " " + getStateString() + " on " + session.toString() + ")";
    }

    ChannelImpl(String profile, String number, RequestHandler handler, boolean tuningReset, SessionImpl session) {
        this.profile = profile;
        this.encoding = Constants.ENCODING_DEFAULT;
        this.number = number;
        this.setRequestHandler(handler, tuningReset);
        this.session = session;
        sentSequence = 0;
        recvSequence = 0;
        lastMessageSent = 1;
        pendingSendMessages = new LinkedList();
        sentMSGQueue = Collections.synchronizedList(new LinkedList());
        recvMSGQueue = new LinkedList();
        recvReplyQueue = new LinkedList();
        state = STATE_INITIALIZED;
        recvWindowUsed = 0;
        recvWindowFreed = 0;
        recvWindowSize = DEFAULT_WINDOW_SIZE;
        peerWindowSize = DEFAULT_WINDOW_SIZE;
    }

    ChannelImpl(String profile, String number, SessionImpl session) {
        this(profile, number, defaultHandler, false, session);
    }

    static ChannelImpl createChannelZero(SessionImpl session, ReplyListener reply, RequestHandler handler) {
        ChannelImpl channel = new ChannelImpl(null, "0", handler, true, session);
        channel.sentMSGQueue.add(new MessageStatus(channel, Message.MESSAGE_TYPE_MSG, 0, null, reply));
        channel.recvMSGQueue.add(new MessageMSGImpl(channel, 0, null));
        channel.state = STATE_ACTIVE;
        return channel;
    }

    /**
     * Closes the channel.
     *
     * @throws BEEPException
     */
    public void close() throws BEEPException {
        session.closeChannel(this, BEEPError.CODE_SUCCESS, null);
    }

    /**
     * Returns application context data previously set using
     * <code>setAppData()</code>.
     *
     * @see #setAppData
     */
    public Object getAppData() {
        return this.applicationData;
    }

    /**
     * Set the application context data member for future retrieval.
     *
     * @see #getAppData
     */
    public void setAppData(Object applicationData) {
        this.applicationData = applicationData;
    }

    /**
     * Returns the receive buffer size for this channel.
     */
    public synchronized int getBufferSize() {
        return recvWindowSize;
    }

    /**
     * Returns the encoding used on this <code>Channel</code>
     * @todo look at removing this and adding the information to getProfile()
     */
    String getEncoding() {
        return encoding;
    }

    void setEncoding(String enc) {
        this.encoding = enc;
    }

    /**
     * Return the number of this <code>Channel</code>.
     *
     */
    public int getNumber() {
        return Integer.parseInt(number);
    }

    /**
     * Sets the receive buffer size for this channel.  Default size is 4K.
     *
     *
     * @param size
     *
     * @throws BEEPException
     *
     */
    public void setReceiveBufferSize(int size) throws BEEPException {
        log.debug("--> setReceiveBufferSize(" + size + ")");
        synchronized (this) {
            if ((state != STATE_ACTIVE) && (state != STATE_INITIALIZED)) {
                throw new BEEPException("Channel in a bad state.");
            }
            if (size < recvWindowUsed) {
                throw new BEEPException("New size is less than what is " + "currently in use.");
            }
            recvWindowSize = size;
            if (log.isDebugEnabled()) {
                log.debug("Buffer size for channel " + number + " set to " + recvWindowSize);
            }
            sendWindowUpdate();
        }
        log.debug("<-- setReceiveBufferSize(" + size + ")");
    }

    /**
     * Sets the <code>MessageListener</code> for this channel.
     *
     * @param ml
     * @return The previous MessageListener or null if none was set.
     */
    public MessageListener setMessageListener(MessageListener ml) {
        MessageListener tmp = getMessageListener();
        this.handler = new MessageListenerAdapter(ml);
        return tmp;
    }

    /**
     * Returns the message listener for this channel.
     */
    public MessageListener getMessageListener() {
        if (!(this.handler instanceof MessageListenerAdapter)) {
            return null;
        }
        return ((MessageListenerAdapter) this.handler).getMessageListener();
    }

    /**
     * Returns the <code>RequestHandler</code> registered with this channel.
     */
    public RequestHandler getRequestHandler() {
        RequestHandler tmp;
        if (this.handler instanceof MessageAssembler) {
            tmp = ((MessageAssembler) this.handler).getRequestHandler();
        } else {
            tmp = this.handler;
        }
        return tmp;
    }

    /**
     * Sets the MSG handler for this <code>Channel</code>.
     * 
     * @param handler <code>RequestHandler</code> to handle received MSG messages.
     * @return The previous <code>RequestHandler</code> or <code>null</code> if
     *         one wasn't set.
     */
    public RequestHandler setRequestHandler(RequestHandler handler) {
        return this.setRequestHandler(handler, false);
    }

    /**
     * Sets the MSG handler for this <code>Channel</code>.
     * 
     * @param handler <code>RequestHandler</code> to handle received MSG messages.
     * @param tuningReset flag indicating that the profile will request a
     *                    tuning reset.
     * @return The previous <code>RequestHandler</code> or <code>null</code> if
     *         one wasn't set.
     */
    public RequestHandler setRequestHandler(RequestHandler handler, boolean tuningReset) {
        RequestHandler tmp = getRequestHandler();
        this.handler = new MessageAssembler(handler);
        this.tuningProfile = tuningReset;
        return tmp;
    }

    /**
     * Returns the session for this channel.
     *
     */
    public Session getSession() {
        return this.session;
    }

    public void run() {
        log.debug("--> run()");
        MessageMSGImpl msg;
        synchronized (recvMSGQueue) {
            msg = (MessageMSGImpl) recvMSGQueue.getFirst();
            synchronized (msg) {
                msg.setNotified();
            }
        }
        log.debug("--> RequestHandler.receiveMsg(" + msg + ")");
        handler.receiveMSG(msg);
        log.debug("<-- RequestHandler.receiveMsg(msg)");
        log.debug("<-- run()");
    }

    /**
     * Sends a message of type MSG.
     *
     * @param stream Data to send in the form of <code>DataStream</code>.
     * @param replyListener A "one-shot" listener that will handle replies
     * to this sendMSG listener.
     *
     * @see OutputDataStream
     * @see MessageStatus
     *
     * @return MessageStatus
     *
     * @throws BEEPException if an error is encoutered.
     */
    public MessageStatus sendMSG(OutputDataStream stream, ReplyListener replyListener) throws BEEPException {
        MessageStatus status;
        if (state != STATE_ACTIVE && state != STATE_TUNING && state != STATE_TUNING_PENDING) {
            switch(state) {
                case STATE_INITIALIZED:
                    throw new BEEPException("Channel is uninitialised.");
                default:
                    throw new BEEPException("Channel is in an unknown state.");
            }
        }
        synchronized (this) {
            status = new MessageStatus(this, Message.MESSAGE_TYPE_MSG, lastMessageSent, stream, replyListener);
            ++lastMessageSent;
        }
        synchronized (sentMSGQueue) {
            sentMSGQueue.add(status);
        }
        sendToPeer(status);
        return status;
    }

    void abort() {
        setState(ChannelImpl.STATE_ABORTED);
    }

    void addPiggybackedMSG(PiggybackedMSG msg) throws BEEPException {
        recvMSGQueue.add(msg);
        try {
            callbackQueue.execute(this);
        } catch (InterruptedException e) {
            throw new BEEPException(e);
        }
    }

    /**
     * get the number of this <code>Channel</code> as a <code>String</code>
     *
     */
    String getNumberAsString() {
        return number;
    }

    public int getState() {
        return state;
    }

    private String getStateString() {
        switch(state) {
            case STATE_INITIALIZED:
                return "initialized";
            case STATE_STARTING:
                return "starting";
            case STATE_ACTIVE:
                return "active";
            case STATE_TUNING_PENDING:
                return "tuning pending";
            case STATE_TUNING:
                return "tuning";
            case STATE_CLOSE_PENDING:
                return "close pending";
            case STATE_CLOSING:
                return "closing";
            case STATE_CLOSED:
                return "closed";
            case STATE_ABORTED:
                return "aborted";
            default:
                return "unknown";
        }
    }

    private void receiveFrame(Frame frame) throws BEEPException {
        if (frame.getMessageType() == Message.MESSAGE_TYPE_MSG) {
            synchronized (recvMSGQueue) {
                MessageMSGImpl msg = null;
                log.debug("size recvMSGQueue [" + recvMSGQueue.size() + "]");
                if (recvMSGQueue.size() != 0) {
                    msg = (MessageMSGImpl) recvMSGQueue.getLast();
                    if (msg.getMsgno() != frame.getMsgno()) {
                        msg = null;
                    }
                }
                if (msg != null) {
                    Iterator i = frame.getPayload();
                    synchronized (msg) {
                        log.debug("add fragment to msg [" + msg + "]");
                        while (i.hasNext()) {
                            msg.getDataStream().add((BufferSegment) i.next());
                        }
                        if (frame.isLast()) {
                            log.debug("frame is last, set DataStream of msg complete");
                            msg.getDataStream().setComplete();
                        }
                        msg.setNotified();
                        if (recvMSGQueue.size() == 1) {
                            try {
                                log.debug("going to call run() from receiveFrame #1 [complete ==" + msg.getDataStream().isComplete() + "]");
                                callbackQueue.execute(this);
                            } catch (InterruptedException e) {
                                throw new BEEPException("interrupted exception #1", e);
                            }
                        }
                    }
                    return;
                }
                msg = new MessageMSGImpl(this, frame.getMsgno(), new InputDataStream(this));
                msg.setNotified();
                Iterator i = frame.getPayload();
                while (i.hasNext()) {
                    msg.getDataStream().add((BufferSegment) i.next());
                }
                recvMSGQueue.addLast(msg);
                if (frame.isLast()) {
                    msg.getDataStream().setComplete();
                }
                if (recvMSGQueue.size() == 1) {
                    try {
                        log.debug("going to call run() from receiveFrame #2 [" + msg.getDataStream().isComplete() + "]");
                        callbackQueue.execute(this);
                    } catch (InterruptedException e) {
                        throw new BEEPException("interrupted exception #2", e);
                    }
                } else {
                    log.warn("cannot call run(), recvMSGQueue size != 1 [" + recvMSGQueue.size() + "]");
                }
            }
            return;
        }
        MessageImpl m = null;
        MessageStatus mstatus;
        synchronized (sentMSGQueue) {
            Message sentMSG;
            if (sentMSGQueue.size() == 0) {
            }
            mstatus = (MessageStatus) sentMSGQueue.get(0);
            if (mstatus.getMsgno() != frame.getMsgno()) {
            }
            if ((frame.isLast() == true) && (frame.getMessageType() != Message.MESSAGE_TYPE_ANS)) {
                sentMSGQueue.remove(0);
            }
        }
        ReplyListener replyListener = mstatus.getReplyListener();
        if (replyListener == null) {
        }
        if (frame.getMessageType() == Message.MESSAGE_TYPE_NUL) {
            synchronized (recvReplyQueue) {
                if (recvReplyQueue.size() != 0) {
                    log.debug("Received NUL before last ANS");
                    session.terminate("Received NUL before last ANS");
                }
            }
            m = new MessageImpl(this, frame.getMsgno(), null, Message.MESSAGE_TYPE_NUL);
            mstatus.setMessageStatus(MessageStatus.MESSAGE_STATUS_RECEIVED_REPLY);
            if (log.isDebugEnabled()) {
                log.debug("Notifying reply listener =>" + replyListener + "for NUL message");
            }
            replyListener.receiveNUL(m);
            return;
        }
        if (frame.getMessageType() == Message.MESSAGE_TYPE_ANS) {
            synchronized (recvReplyQueue) {
                Iterator i = recvReplyQueue.iterator();
                m = null;
                while (i.hasNext()) {
                    MessageImpl tmp = (MessageImpl) i.next();
                    if (tmp.getAnsno() == frame.getAnsno()) {
                        m = tmp;
                        break;
                    }
                }
                if (m == null) {
                    m = new MessageImpl(this, frame.getMsgno(), frame.getAnsno(), new InputDataStream(this));
                    if (!frame.isLast()) {
                        recvReplyQueue.add(m);
                    }
                } else if (frame.isLast()) {
                    i.remove();
                }
            }
        } else {
            synchronized (recvReplyQueue) {
                if (recvReplyQueue.size() == 0) {
                    m = new MessageImpl(this, frame.getMsgno(), new InputDataStream(this), frame.getMessageType());
                    if (frame.isLast() == false) {
                        recvReplyQueue.add(m);
                    }
                } else {
                    m = (MessageImpl) recvReplyQueue.getFirst();
                    if (frame.isLast()) {
                        recvReplyQueue.removeFirst();
                    }
                }
                if (frame.isLast()) {
                    if (frame.getMessageType() == Message.MESSAGE_TYPE_ERR) {
                        mstatus.setMessageStatus(MessageStatus.MESSAGE_STATUS_RECEIVED_ERROR);
                    } else {
                        mstatus.setMessageStatus(MessageStatus.MESSAGE_STATUS_RECEIVED_REPLY);
                    }
                }
            }
        }
        Iterator i = frame.getPayload();
        while (i.hasNext()) {
            m.getDataStream().add((BufferSegment) i.next());
        }
        if (frame.isLast()) {
            m.getDataStream().setComplete();
        }
        synchronized (m) {
            if (m.isNotified()) {
                return;
            }
            m.setNotified();
        }
        if (log.isDebugEnabled()) {
            log.debug("Notifying reply listener.=>" + replyListener);
        }
        if (m.messageType == Message.MESSAGE_TYPE_RPY) {
            replyListener.receiveRPY(m);
        } else if (m.messageType == Message.MESSAGE_TYPE_ERR) {
            replyListener.receiveERR(m);
        } else if (m.messageType == Message.MESSAGE_TYPE_ANS) {
            replyListener.receiveANS(m);
        }
    }

    /**
     * Interface between the session.  The session receives a frame and then
     * calls this function.  The function then calls the message listener
     * via some intermediary thread functions.  The message hasn't been
     * completely received.  The data stream contained in the message will
     * block if more is expected.
     * 
     * @param frame - the frame received by the session
     * @return true iff session listener thread shall keep running
     */
    boolean postFrame(Frame frame) throws BEEPException {
        log.trace("--> Channel.postFrame");
        if (state != STATE_ACTIVE && state != STATE_TUNING && state != STATE_TUNING_PENDING) {
            throw new BEEPException("State is " + state);
        }
        validateFrame(frame);
        recvSequence += frame.getSize();
        recvWindowUsed += frame.getSize();
        if (recvWindowUsed > recvWindowSize) {
            throw new BEEPException("Channel window overflow");
        }
        log.trace("--> Channel.receiveFrame");
        receiveFrame(frame);
        log.trace("<-- Channel.receiveFrame");
        boolean result;
        if (frame.getMessageType() == Message.MESSAGE_TYPE_MSG) {
            result = !(frame.isLast() == true && tuningProfile == true);
        } else {
            result = !(frame.isLast() == true && getState() == STATE_TUNING);
        }
        log.trace("<-- Channel.postFrame [" + result + "]");
        return result;
    }

    void sendMessage(MessageStatus m) throws BEEPException {
        log.debug("--> sendMessage");
        if (state != STATE_ACTIVE && state != STATE_TUNING && state != STATE_TUNING_PENDING) {
            switch(state) {
                case STATE_INITIALIZED:
                    throw new BEEPException("Channel is uninitialised.");
                default:
                    throw new BEEPException("Channel is in an unknown state.");
            }
        }
        sendToPeer(m);
        log.debug("<-- sendMessage");
    }

    private void sendToPeer(MessageStatus status) throws BEEPException {
        log.debug("--> sendToPeer [" + status + "]");
        synchronized (pendingSendMessages) {
            pendingSendMessages.add(status);
        }
        status.getMessageData().setChannel(this);
        sendQueuedMessages();
        log.debug("<-- sendToPeer");
    }

    synchronized void sendQueuedMessages() throws BEEPException {
        log.debug("--> sendQueuedMessages()");
        while (true) {
            MessageStatus status;
            synchronized (pendingSendMessages) {
                if (pendingSendMessages.isEmpty()) {
                    log.debug("<-- sendQueuedMessages() #1");
                    return;
                }
                status = (MessageStatus) pendingSendMessages.removeFirst();
            }
            if (this.recvWindowFreed != 0) {
                sendWindowUpdate();
            }
            sendFrames(status);
            log.debug("MessageStatus == " + status.getMessageStatus());
            if (status.getMessageStatus() != MessageStatus.MESSAGE_STATUS_SENT) {
                synchronized (pendingSendMessages) {
                    pendingSendMessages.addFirst(status);
                }
                log.debug("<-- sendQueuedMessages() #2");
                return;
            }
        }
    }

    private void sendFrames(MessageStatus status) throws BEEPException {
        log.debug("--> sendFrames");
        int sessionBufferSize = session.getMaxFrameSize();
        OutputDataStream ds = status.getMessageData();
        do {
            synchronized (this) {
                Frame frame;
                frame = new Frame(status.getMessageType(), this, status.getMsgno(), false, sentSequence, 0, status.getAnsno());
                if (peerWindowSize == 0) {
                    log.debug("peerWindowSize == 0, return");
                    return;
                }
                int maxToSend = Math.min(sessionBufferSize, peerWindowSize);
                int size = 0;
                while (size < maxToSend) {
                    if (ds.availableSegment() == false) {
                        if (size == 0) {
                            if (ds.isComplete() == false) {
                                log.warn("more BufferSegments are expected... [size = " + size + ", maxToSend = " + maxToSend + ", peerWindowSize = " + peerWindowSize + "]");
                                return;
                            }
                            frame.addPayload(zeroLengthSegment);
                        }
                        break;
                    }
                    BufferSegment b = ds.getNextSegment(maxToSend - size);
                    frame.addPayload(b);
                    size += b.getLength();
                }
                if (ds.isComplete() && ds.availableSegment() == false) {
                    log.debug("frame is set as last");
                    frame.setLast();
                }
                try {
                    session.sendFrame(frame);
                } catch (BEEPException e) {
                    log.error("sendFrames", e);
                    status.setMessageStatus(MessageStatus.MESSAGE_STATUS_NOT_SENT);
                    throw e;
                }
                sentSequence += size;
                peerWindowSize -= size;
            }
        } while (ds.availableSegment() == true || ds.isComplete() == false);
        status.setMessageStatus(MessageStatus.MESSAGE_STATUS_SENT);
        if (ds.isComplete() && ds.availableSegment() == false && (status.getMessageType() == Message.MESSAGE_TYPE_RPY || status.getMessageType() == Message.MESSAGE_TYPE_ERR || status.getMessageType() == Message.MESSAGE_TYPE_NUL)) {
            MessageMSGImpl msg;
            synchronized (recvMSGQueue) {
                log.debug("sendFrames() removes msg in recvMSGQueue [" + recvMSGQueue.size() + "]");
                if (!recvMSGQueue.isEmpty()) {
                    log.debug("delete msg [" + recvMSGQueue.removeFirst() + "]");
                } else {
                    log.warn("recvMSGQueue is empty");
                }
                log.debug("recvMSGQueue [" + recvMSGQueue.size() + "]");
                if (recvMSGQueue.size() != 0) {
                    msg = (MessageMSGImpl) recvMSGQueue.getFirst();
                    synchronized (msg) {
                        msg.setNotified();
                    }
                } else {
                    msg = null;
                }
            }
            if (msg != null) {
                try {
                    log.debug("going to call run() from sendFrames [" + msg.getDataStream().isComplete() + "]");
                    callbackQueue.execute(this);
                } catch (InterruptedException e) {
                    throw new BEEPException(e);
                }
            }
        }
        log.debug("<-- sendFrames");
    }

    private void sendWindowUpdate() throws BEEPException {
        log.debug("--> sendWindowUpdate");
        if (session.updateMyReceiveBufferSize(this, recvSequence, recvWindowSize - (recvWindowUsed - recvWindowFreed))) {
            recvWindowUsed -= recvWindowFreed;
            recvWindowFreed = 0;
        }
        log.debug("<-- sendWindowUpdate");
    }

    /**
     * Method setState
     *
     *
     * @param newState
     *
     * @throws BEEPException
     *
     */
    synchronized void setState(int newState) {
        log.trace("CH" + number + " state=" + newState);
        this.state = newState;
        if (false) {
            session.terminate("Bad state transition in channel");
        }
    }

    void setProfile(String profile) {
        this.profile = profile;
    }

    /**
     * Returns the profile for this channel.
     */
    public String getProfile() {
        return this.profile;
    }

    synchronized void updatePeerReceiveBufferSize(long lastSeq, int size) {
        log.debug("\t--> updatePeerReceiveBufferSize");
        int previousPeerWindowSize = peerWindowSize;
        if (log.isDebugEnabled()) {
            log.debug("Channel.updatePeerReceiveBufferSize: size = " + size + " lastSeq = " + lastSeq + " sentSequence = " + sentSequence + " peerWindowSize = " + peerWindowSize);
        }
        peerWindowSize = size - (int) (sentSequence - lastSeq);
        log.debug("Channel.updatePeerReceiveBufferSize: New window size = " + peerWindowSize);
        if ((previousPeerWindowSize == 0) && (peerWindowSize > 0)) {
            try {
                sendQueuedMessages();
            } catch (BEEPException e) {
            }
        }
        log.debug("\t<-- updatePeerReceiveBufferSize");
    }

    private void validateFrame(Frame frame) throws BEEPException {
        synchronized (this) {
            if (previousFrame == null) {
                if (frame.getMessageType() == Message.MESSAGE_TYPE_MSG) {
                    synchronized (recvMSGQueue) {
                        ListIterator i = recvMSGQueue.listIterator(recvMSGQueue.size());
                        while (i.hasPrevious()) {
                            if (((Message) i.previous()).getMsgno() == frame.getMsgno()) {
                                throw new BEEPException("Received a frame " + "with a duplicate " + "msgno (" + frame.getMsgno() + ")");
                            }
                        }
                    }
                } else {
                    MessageStatus mstatus;
                    synchronized (sentMSGQueue) {
                        if (sentMSGQueue.size() == 0) {
                            throw new BEEPException("Received unsolicited reply");
                        }
                        mstatus = (MessageStatus) sentMSGQueue.get(0);
                    }
                    if (frame.getMsgno() != mstatus.getMsgno()) {
                        throw new BEEPException("Incorrect message number: was " + frame.getMsgno() + "; expecting " + mstatus.getMsgno());
                    }
                }
            } else {
                if (previousFrame.getMessageType() != frame.getMessageType()) {
                    throw new BEEPException("Incorrect message type: was " + frame.getMessageTypeString() + "; expecting " + previousFrame.getMessageTypeString());
                }
                if (frame.getMessageType() == Message.MESSAGE_TYPE_MSG && frame.getMsgno() != previousFrame.getMsgno()) {
                    throw new BEEPException("Incorrect message number: was " + frame.getMsgno() + "; expecting " + previousFrame.getMsgno());
                }
            }
            if (frame.getSeqno() != recvSequence) {
                throw new BEEPException("Incorrect sequence number: was " + frame.getSeqno() + "; expecting " + recvSequence);
            }
        }
        if (frame.getMessageType() != Message.MESSAGE_TYPE_MSG) {
            MessageStatus mstatus;
            synchronized (sentMSGQueue) {
                if (sentMSGQueue.size() == 0) {
                    throw new BEEPException("Received unsolicited reply");
                }
                mstatus = (MessageStatus) sentMSGQueue.get(0);
                if (mstatus.getMsgno() != frame.getMsgno()) {
                    throw new BEEPException("Received reply out of order");
                }
            }
        }
        if (frame.isLast()) {
            previousFrame = null;
        } else {
            log.debug("Frame is not last");
            previousFrame = frame;
        }
    }

    synchronized void freeReceiveBufferBytes(int size) {
        log.debug("--> freeReceiveBufferBytes(" + size + ")");
        if (log.isTraceEnabled()) {
            log.trace("Freed up " + size + " bytes on channel " + number);
        }
        recvWindowFreed += size;
        if (log.isTraceEnabled()) {
            log.trace("recvWindowUsed = " + recvWindowUsed + " recvWindowFreed = " + recvWindowFreed + " recvWindowSize = " + recvWindowSize);
        }
        if (state == ChannelImpl.STATE_ACTIVE) {
            try {
                sendWindowUpdate();
            } catch (BEEPException e) {
                log.fatal("Error updating receive buffer size", e);
            }
        }
        log.debug("<-- freeReceiveBufferBytes(" + size + ")");
    }

    /**
     * Method getAvailableWindow
     *
     *
     * @return int the amount of free buffer space
     * available.
     *
     * This is called from Session to provide a # used
     * to screen frame sizes against and enforce the
     * protocol.
     *
     */
    synchronized int getAvailableWindow() {
        return (recvWindowSize - recvWindowUsed);
    }

    /**
     * Used to set data that can be piggybacked on
     * a profile reply to a start channel request
     * (or any other scenario we choose)
     *
     * called by Channel Zero
     */
    public void setStartData(String data) {
        startData = data;
    }

    /**
     * Used to get data that can be piggybacked on
     * a profile reply to a start channel request
     * (or any other scenario we choose)
     *
     * Could be called by users, profile implementors etc.
     * to fetch data off a profile response.
     *
     * @return String the attached data, if any
     */
    public String getStartData() {
        return startData;
    }

    /**
     * Used to remove the piggyback request from the recvQue
     *
     * called by piggyBackMSG 
     */
    public void removeMSG() {
        recvMSGQueue.removeFirst();
    }

    static class MessageListenerAdapter implements RequestHandler {

        MessageListenerAdapter(MessageListener listener) {
            this.listener = listener;
        }

        public void receiveMSG(MessageMSG message) {
            try {
                listener.receiveMSG(message);
            } catch (BEEPError e) {
                try {
                    message.sendERR(e);
                } catch (BEEPException e2) {
                    log.error("Error sending ERR", e2);
                }
            } catch (AbortChannelException e) {
                try {
                    message.getChannel().close();
                } catch (BEEPException e2) {
                    log.error("Error closing channel", e2);
                }
            }
        }

        public MessageListener getMessageListener() {
            return this.listener;
        }

        private Log log = LogFactory.getLog(this.getClass());

        private MessageListener listener;
    }

    private static class DefaultMSGHandler implements RequestHandler {

        public void receiveMSG(MessageMSG message) {
            log.error("No handler registered to process MSG received on " + "channel " + message.getChannel().getNumber());
            try {
                message.sendERR(BEEPError.CODE_REQUESTED_ACTION_ABORTED, "No MSG handler registered");
            } catch (BEEPException e) {
                log.error("Error sending ERR", e);
            }
        }

        private Log log = LogFactory.getLog(this.getClass());

        private MessageListener listener;
    }
}
