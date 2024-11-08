package ch.iserver.ace.net.protocol;

import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.ReplyListener;
import ch.iserver.ace.CaretUpdateMessage;
import ch.iserver.ace.FailureCodes;
import ch.iserver.ace.algorithm.Request;
import ch.iserver.ace.algorithm.Timestamp;
import ch.iserver.ace.net.ParticipantConnection;
import ch.iserver.ace.net.ParticipantPort;
import ch.iserver.ace.net.PortableDocument;
import ch.iserver.ace.net.RemoteUserProxy;
import ch.iserver.ace.net.core.NetworkServiceImpl;
import ch.iserver.ace.net.protocol.RequestImpl.DocumentInfo;
import ch.iserver.ace.net.protocol.filter.RequestFilter;

/**
 * The <code>ParticipantConnectionImpl</code> represents a connection from the publisher of a document
 * to a participant.
 * This connection does not establish its channel until <code>joinAccepted(ParticipantPort)</code> is 
 * invoked.
 *
 */
public class ParticipantConnectionImpl extends AbstractConnection implements ParticipantConnection {

    /**
	 * the remote user sesson
	 */
    private RemoteUserSession session;

    /**
	 * the request filter chain
	 */
    private RequestFilter filter;

    /**
	 * joinAccepted flag
	 */
    private boolean joinAccepted;

    /**
	 * the serializer to serialize messages
	 */
    private Serializer serializer;

    /**
	 * the participant id
	 */
    private int participantId = -1;

    /**
	 * the document id
	 */
    private String docId;

    /**
	 * flags to indicate state
	 */
    private boolean isKicked, hasLeft;

    /**
	 * the user name
	 */
    private String username;

    /**
	 * the participant port to communicate to the upper layer
	 */
    private ParticipantPort port;

    /**
	 * the receive channel (only incoming messages)
	 */
    private Channel incoming;

    /**
	 * Creates a new ParticipantConnectionImpl.
	 * 
	 * @param docId			the document id
	 * @param session		the remote user session
	 * @param listener		the reply listener
	 * @param serializer		the serializer
	 * @param filter			the request filter chain to process outgoing requests
	 */
    public ParticipantConnectionImpl(String docId, RemoteUserSession session, ReplyListener listener, Serializer serializer, RequestFilter filter) {
        super(null);
        setState(STATE_INITIALIZED);
        this.docId = docId;
        this.session = session;
        joinAccepted = false;
        this.serializer = serializer;
        this.filter = filter;
        setReplyListener(listener);
        super.LOG = Logger.getLogger(ParticipantConnectionImpl.class);
        isKicked = false;
        setHasLeft(false);
        username = session.getUser().getUserDetails().getUsername();
    }

    /**
	 * Gets the document id.
	 * 
	 * @return the document id
	 */
    public String getDocumentId() {
        return docId;
    }

    /**
	 * Gets the participant id.
	 * 
	 * @return the participant id
	 */
    public int getParticipantId() {
        return participantId;
    }

    /**
	 * Gets the ParticipantPort.
	 * 
	 * @return the participant port
	 */
    public ParticipantPort getParticipantPort() {
        return port;
    }

    /**
	 * Sets the hasLeft flag. This is used to prevent the publisher from 
	 * sending a 'terminatedSession' message.
	 * 
	 * @param value	the value to set
	 */
    public void setHasLeft(boolean value) {
        LOG.debug("setHasLeft(" + value + ")");
        hasLeft = value;
    }

    /**
	 * Returns if the participant has left.
	 * 
	 * @return	true iff the participant has left
	 */
    public boolean hasLeft() {
        return hasLeft;
    }

    /**
	 * {@inheritDoc}
	 */
    public void cleanup() {
        LOG.debug("--> cleanup()");
        session = null;
        serializer = null;
        port = null;
        setReplyListener(null);
        Channel channel = getChannel();
        if (channel != null) {
            channel.setRequestHandler(null);
        }
        Thread t = getSendingThread();
        if (t != null) {
            LOG.debug("interrupt sending thread [" + t.getName() + "]");
            t.interrupt();
        }
        setChannel(null);
        if (incoming != null) {
            incoming.setRequestHandler(null);
            incoming = null;
        }
        setState(STATE_CLOSED);
        LOG.debug("<-- cleanup()");
    }

    /**
	 * {@inheritDoc}
	 */
    public void recover() throws RecoveryException {
        throw new RecoveryException();
    }

    /**
	 * {@inheritDoc}
	 */
    public void setParticipantId(int participantId) {
        LOG.debug("setParticipantId(" + participantId + ")");
        this.participantId = participantId;
    }

    /**
	 * {@inheritDoc}
	 */
    public void joinAccepted(ParticipantPort port) {
        LOG.info("--> joinAccepted()");
        joinAccepted = true;
        this.port = port;
        try {
            LOG.debug("initiate incoming and outgoing channel to peer " + session.getHost());
            Channel outgoing = session.setUpChannel(RemoteUserSession.CHANNEL_SESSION, null, getDocumentId());
            setChannel(outgoing);
            incoming = session.setUpChannel(RemoteUserSession.CHANNEL_SESSION, this, getDocumentId());
            LOG.debug("done.");
            setState(STATE_ACTIVE);
        } catch (ConnectionException ce) {
            NetworkServiceImpl.getInstance().getCallback().serviceFailure(FailureCodes.CHANNEL_FAILURE, getUser().getUserDetails().getUsername(), ce);
        }
        LOG.info("<-- joinAccepted()");
    }

    /**
	 * {@inheritDoc}
	 */
    public void joinRejected(int code) {
        LOG.info("--> joinRejected(" + code + ")");
        DocumentInfo info = new DocumentInfo(docId, null, null);
        info.setData(Integer.toString(code));
        ch.iserver.ace.net.protocol.Request request = new RequestImpl(ProtocolConstants.JOIN_REJECTED, session.getUser().getId(), info);
        filter.process(request);
        executeCleanup();
        LOG.info("<-- joinRejected()");
    }

    /**
	 * {@inheritDoc}
	 */
    public RemoteUserProxy getUser() {
        return session.getUser();
    }

    /**
	 * {@inheritDoc}
	 */
    public void sendDocument(PortableDocument document) {
        if (joinAccepted) {
            LOG.info("--> sendDocument()");
            if (getState() == STATE_ACTIVE) {
                byte[] data = null;
                try {
                    DocumentInfo info = new DocumentInfo(docId, getParticipantId());
                    data = serializer.createResponse(ProtocolConstants.JOIN_DOCUMENT, info, document);
                } catch (SerializeException se) {
                    LOG.error("could not serialize document [" + se.getMessage() + "]");
                }
                Channel outgoing = getChannel();
                setChannel(incoming);
                sendToPeer(data);
                setChannel(outgoing);
            } else {
                LOG.warn("cannot send Document, connection is in state " + getStateString());
            }
            LOG.info("<-- sendDocument()");
        } else {
            throw new IllegalStateException("cannot send document before join is accepted.");
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public void sendRequest(int participantId, Request request) {
        LOG.info("--> sendRequest(" + participantId + ", " + request + ")");
        if (getState() == STATE_ACTIVE) {
            byte[] data = null;
            try {
                data = serializer.createSessionMessage(ProtocolConstants.REQUEST, request, Integer.toString(participantId));
            } catch (SerializeException se) {
                LOG.error("could not serialize message [" + se.getMessage() + "]");
            }
            sendToPeer(data);
        } else {
            LOG.warn("cannot send Acknowledge, connection is in state " + getStateString());
        }
        LOG.info("<-- sendRequest()");
    }

    /**
	 * {@inheritDoc}
	 */
    public void sendCaretUpdateMessage(int participantId, CaretUpdateMessage message) {
        LOG.info("--> sendCaretUpdateMessage(" + participantId + ", " + message + ")");
        if (getState() == STATE_ACTIVE) {
            byte[] data = null;
            try {
                data = serializer.createSessionMessage(ProtocolConstants.CARET_UPDATE, message, Integer.toString(participantId));
            } catch (SerializeException se) {
                LOG.error("could not serialize message [" + se.getMessage() + "]");
            }
            sendToPeer(data);
        } else {
            LOG.warn("cannot send CaretUpdateMessage, connection is in state " + getStateString());
        }
        LOG.info("<-- sendCaretUpdateMessage()");
    }

    /**
	 * {@inheritDoc}
	 */
    public void sendAcknowledge(int siteId, Timestamp timestamp) {
        LOG.info("--> sendAcknowledge(" + siteId + ", " + timestamp + ")");
        if (getState() == STATE_ACTIVE) {
            byte[] data = null;
            try {
                data = serializer.createSessionMessage(ProtocolConstants.ACKNOWLEDGE, timestamp, Integer.toString(siteId));
            } catch (SerializeException se) {
                LOG.error("could not serialize message [" + se.getMessage() + "]");
            }
            sendToPeer(data);
        } else {
            LOG.warn("cannot send Acknowledge, connection is in state " + getStateString());
        }
        LOG.info("<-- sendAcknowledge()");
    }

    /**
	 * {@inheritDoc}
	 */
    public void sendParticipantJoined(int participantId, RemoteUserProxy proxy) {
        LOG.info("--> sendParticipantJoined(" + participantId + ", " + proxy + ")");
        if (getState() == STATE_ACTIVE) {
            byte[] data = null;
            try {
                data = serializer.createSessionMessage(ProtocolConstants.PARTICIPANT_JOINED, proxy, Integer.toString(participantId));
            } catch (SerializeException se) {
                LOG.error("could not serialize message [" + se.getMessage() + "]");
            }
            sendToPeer(data);
        } else {
            LOG.warn("cannot send participantJoined, connection is in state " + getStateString());
        }
        LOG.info("--> sendParticipantJoined()");
    }

    /**
	 * {@inheritDoc}
	 */
    public void sendParticipantLeft(int participantId, int reason) {
        LOG.info("--> sendParticipantLeft(" + participantId + ", " + reason + ")");
        if (getState() == STATE_ACTIVE) {
            byte[] data = null;
            try {
                data = serializer.createSessionMessage(ProtocolConstants.PARTICIPANT_LEFT, Integer.toString(reason), Integer.toString(participantId));
            } catch (SerializeException se) {
                LOG.error("could not serialize message [" + se.getMessage() + "]");
            }
            sendToPeer(data);
        } else {
            LOG.warn("cannot send participantLeft, connection is in state " + getStateString());
        }
        LOG.info("<-- sendParticipantLeft()");
    }

    /**
	 * {@inheritDoc}
	 */
    public void sendKicked() {
        LOG.info("--> sendKicked()");
        isKicked = true;
        if (getState() == STATE_ACTIVE) {
            byte[] data = null;
            try {
                data = serializer.createNotification(ProtocolConstants.KICKED, docId);
            } catch (SerializeException se) {
                LOG.error("could not serialize message [" + se.getMessage() + "]");
            }
            sendToPeer(data);
        } else {
            LOG.warn("do not send kicked, connection is in state " + getStateString());
        }
        LOG.info("<-- sendKicked()");
    }

    /**
	 * {@inheritDoc}
	 */
    public void close() {
        LOG.info("--> close(" + getParticipantId() + ", " + username + ")");
        if (getState() == STATE_ACTIVE) {
            try {
                if (!isKicked && !hasLeft()) {
                    sendSessionTerminated();
                }
                final Thread t = Thread.currentThread();
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {

                    public void run() {
                        LOG.debug("going to interrupt [" + t.getName() + "]");
                        t.interrupt();
                        LOG.debug("interrupted.");
                    }
                }, 1500);
                LOG.debug("--> channel.close()");
                getChannel().close();
                timer.cancel();
                LOG.debug("<-- channel.close()");
            } catch (BEEPException be) {
                LOG.warn("could not close channel [" + be.getMessage() + "]");
            }
            executeCleanup();
        } else {
            LOG.warn("cannot close, connection is in state " + getStateString());
        }
        LOG.info("<-- close()");
    }

    /**
	 * Helper method to send a terminatedSession message.
	 *
	 */
    private void sendSessionTerminated() {
        byte[] data = null;
        try {
            data = serializer.createSessionMessage(ProtocolConstants.SESSION_TERMINATED, null, null);
        } catch (SerializeException se) {
            LOG.error("could not serialize message [" + se.getMessage() + "]");
        }
        sendToPeer(data);
    }

    /**
	 * Private helper method to send data to the peer.
	 * 
	 * @param data 	the data to be sent
	 */
    private void sendToPeer(byte[] data) {
        try {
            send(data, username, getReplyListener());
        } catch (ProtocolException pe) {
            LOG.error("protocol exception [" + pe.getMessage() + "]");
            executeCleanup();
            throw new NetworkException("could not send message to '" + username + "' [" + pe.getMessage() + "]");
        }
    }

    /**
	 * Clean up participant resources.
	 */
    private void executeCleanup() {
        if (docId != null && session != null) {
            ParticipantCleanup cleanup = new ParticipantCleanup(docId, session.getUser().getId());
            cleanup.execute();
        } else {
            LOG.warn("cannot cleanup, docId and/or session null [" + docId + "] [" + session + "]");
        }
    }
}
