package org.beepcore.beep.profile.sasl.anonymous;

import java.io.InputStream;
import java.io.IOException;
import java.util.Hashtable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.beepcore.beep.core.*;
import org.beepcore.beep.profile.sasl.*;

/**
 * This class encapsulates the state associated with
 * an ongoing SASL-Anonymous Authentication, and
 * provides methods to handle the exchange.  The
 * AnonymousAuthenticator provides inter-message
 * state for the exchange, which is normally
 * quite simple, and can in fact be handled complete
 * in the start channel exchange.  This isn't mandatory
 * however, and so this class has been provided to
 * support that non-piggybacked start channel case.
 *
 *
 * @author Eric Dixon
 * @author Huston Franklin
 * @author Jay Kint
 * @author Scott Pead
 * @version $Revision: 1.15 $, $Date: 2003/06/10 18:59:22 $
 *
 */
class AnonymousAuthenticator implements RequestHandler, ReplyListener {

    public static final int STATE_UNKNOWN = 0;

    public static final int STATE_STARTED = 1;

    public static final int STATE_ID = 2;

    public static final int STATE_COMPLETE = 3;

    public static final int STATE_ABORT = 4;

    public static final String ERR_ANON_STATE = "Illegal state transition";

    public static final String ERR_PEER_ABORTED = "Our BEEP Peer has aborted this authentication sequence";

    public static final String ERR_IDENTITY_PARSE_FAILURE = "Invalid identity information submitted for Anonymous Authentication";

    public static final String ERR_UNEXPECTED_MESSAGE = "Unexpected SASL-Anonymous Message";

    private Log log = LogFactory.getLog(this.getClass());

    private int state;

    private Channel channel;

    private Hashtable credential;

    private SASLAnonymousProfile profile;

    private String authenticated;

    /**
     * AnonymouAuthenticator is the constructor used by the Listener.
     * It means someone has started a SASL anon channel and hasn't
     * yet authenticated and this object has been constructed to
     * track that.
     *
     * @param SASLAnonymousProfile the instance of the profile used
     * in the authentication.
     *
     * @throws SASLException
     *
     */
    AnonymousAuthenticator(SASLAnonymousProfile anonymousProfile) {
        log.debug("Creating Listener ANONYMOUS Authenticator");
        credential = new Hashtable();
        profile = anonymousProfile;
        state = STATE_UNKNOWN;
        credential.put(SessionCredential.AUTHENTICATOR_TYPE, SASLAnonymousProfile.MECHANISM);
    }

    /**
     * Method started is called when the channel has been
     * 'started', and basically modifies the authenticators
     * state appropriately (including setting the Authenticator
     * as the replyListener for the Channel used).
     *
     * @param Channel is used to set the data member, so we
     * know what channel is used for this authentication.
     *
     * @throws SASLException
     *
     */
    void started(Channel ch) throws SASLException {
        log.debug("Starting Anonymous Authenticator");
        if (state != STATE_UNKNOWN) {
            throw new SASLException(ERR_ANON_STATE);
        }
        state = STATE_STARTED;
        ch.setRequestHandler(this);
        channel = ch;
    }

    /**
     * Method receiveID is called when the Initiator of the
     * authentication sends its information (identity).
     * @todo make the inbound parameter a blob instead..or
     * does that break piggybacking?..no, it shouldn't, the
     * piggybacked authentication can deal with it and just
     * catch the exception.
     *
     * @param String data, the user's information
     * @param Channel
     *
     * @throws SASLException
     *
     */
    synchronized Blob receiveID(String data) throws SASLException {
        log.debug("Anonymous Authenticator Receiving ID");
        if (state != STATE_STARTED) {
            abort(ERR_ANON_STATE);
        }
        if (data == null) {
            abort(ERR_IDENTITY_PARSE_FAILURE);
        }
        state = STATE_ID;
        credential.put(SessionCredential.AUTHENTICATOR, data);
        credential.put(SessionCredential.AUTHENTICATOR_TYPE, SASLAnonymousProfile.MECHANISM);
        try {
            return new Blob(Blob.STATUS_COMPLETE);
        } catch (Exception x) {
        }
        abort("Failed to complete SASL Anonymous authentication");
        return null;
    }

    /**
     * Initiator API used by SASL-ANON consumers that don't use
     * the data on the startChannel option
     *
     * If it works, we should get a challenge in our receiveRPY
     * callback ;)
     */
    void sendIdentity(String authenticateId) throws SASLException {
        log.debug("Anonymous Authenticator sending Identity");
        if (authenticateId == null) throw new SASLException(ERR_IDENTITY_PARSE_FAILURE);
        if (log.isDebugEnabled()) {
            log.debug("Using=>" + authenticateId + "<=");
        }
        Blob blob = new Blob(Blob.STATUS_NONE, authenticateId);
        if (log.isDebugEnabled()) {
            log.debug("Using=>" + blob.toString() + "<=");
        }
        try {
            credential.put(SessionCredential.AUTHENTICATOR, authenticateId);
            channel.sendMSG(new StringOutputDataStream(blob.toString()), this);
        } catch (Exception x) {
            abort(x.getMessage());
        }
        state = STATE_ID;
    }

    /**
     * Initiator API
     * Receive response to challenge, figure out if it
     * works or throw an exception if it doesn't.
     */
    synchronized SessionCredential receiveCompletion(String response) throws SASLException {
        log.debug("Anonymous Authenticator Completing!");
        if (state != STATE_ID) {
            abort(ERR_ANON_STATE);
        }
        state = STATE_COMPLETE;
        return new SessionCredential(credential);
    }

    /**
     * Cheat here, if we don't want to send anything back, then
     * we don't do a damn thing...just abort.
     *
     * The params are a bit complex.  The reply boolean indicates
     * whether or not to send a reply or a message.
     *
     * The channel parameter is non-null if we are to send ANYTHING
     * AT ALL.  If it's null, we don't send.  This is kind of
     * kludgey.
     * @todo make it cleaner.
     */
    void abort(String msg) throws SASLException {
        log.debug("Aborting Anonymous Authenticator");
        log.debug(msg);
        state = STATE_ABORT;
        throw new SASLException(msg);
    }

    void abortNoThrow(String msg) {
        log.debug("Aborting Anonymous Authenticator");
        log.debug(msg);
        state = STATE_ABORT;
    }

    /**
     * Method receiveMSG
     * Listener API
     *
     * We receive MSGS - IDs and stuff.
     *
     * @param Message message is the data we've received.
     * We parse it to see if it's identity information, an
     * abort, or otherwise.
     *
     * @throws BEEPError if an ERR message is generated
     */
    public void receiveMSG(MessageMSG message) {
        try {
            log.debug("Anonymous Authenticator.receiveMSG");
            String data = null;
            Blob blob = null;
            if (state != STATE_STARTED) {
                abort(ERR_ANON_STATE);
            }
            try {
                InputStream is = message.getDataStream().getInputStream();
                int limit = is.available();
                byte buff[] = new byte[limit];
                is.read(buff);
                blob = new Blob(new String(buff));
                data = blob.getData();
            } catch (IOException x) {
                log.error("", x);
                abort(x.getMessage());
            }
            if (log.isDebugEnabled()) {
                log.debug("MSG DATA=>" + data);
            }
            String status = blob.getStatus();
            if ((status != null) && status.equals(SASLProfile.SASL_STATUS_ABORT)) {
                abort(ERR_PEER_ABORTED);
            }
            if (state == STATE_STARTED) {
                try {
                    Blob reply = receiveID(data);
                    message.sendRPY(new StringOutputDataStream(reply.toString()));
                } catch (BEEPException x) {
                    abort(x.getMessage());
                }
                profile.finishListenerAuthentication(new SessionCredential(credential), channel.getSession());
            }
        } catch (SASLException s) {
            try {
                Blob reply = new Blob(Blob.STATUS_ABORT, s.getMessage());
                message.sendRPY(new StringOutputDataStream(reply.toString()));
            } catch (BEEPException t) {
                message.getChannel().getSession().terminate(t.getMessage());
            }
        }
    }

    /**
     * Method receiveRPY
     * Initiator API
     *
     * We receive replies to our ID messages
     *
     * @param Message message is the data we've received.
     * We parse it to see if it's identity information, an
     * abort, or otherwise.
     *
     */
    public void receiveRPY(Message message) {
        log.debug("Anonymous Authenticator.receiveRPY");
        Blob blob = null;
        boolean sendAbort = true;
        try {
            if (state != STATE_ID) {
                abort(ERR_ANON_STATE);
            }
            try {
                InputStream is = message.getDataStream().getInputStream();
                int limit = is.available();
                byte buff[] = new byte[limit];
                is.read(buff);
                blob = new Blob(new String(buff));
            } catch (IOException x) {
                abort(x.getMessage());
            }
            String status = blob.getStatus();
            if ((status != null) && status.equals(SASLProfile.SASL_STATUS_ABORT)) {
                log.debug("Anonymous Authenticator receiveRPY=>" + blob.getData());
                sendAbort = false;
                abort(ERR_PEER_ABORTED);
            }
            if (!status.equals(Blob.ABORT)) {
                profile.finishInitiatorAuthentication(new SessionCredential(credential), channel.getSession());
                synchronized (this) {
                    this.notify();
                }
                return;
            } else {
                abort(blob.getData());
            }
        } catch (SASLException x) {
            log.error(x);
            synchronized (this) {
                this.notify();
            }
            try {
                if (sendAbort) {
                    Blob reply = new Blob(Blob.STATUS_ABORT, x.getMessage());
                    channel.sendMSG(new StringOutputDataStream(blob.toString()), this);
                }
            } catch (Exception q) {
                message.getChannel().getSession().terminate(q.getMessage());
            }
        }
    }

    /**
     * Method receiveERR
     * Initiator API
     *
     * Generally we get this if our challenge fails or
     * our authenticate identity is unacceptable or the
     * hash we use isn't up to snuff etc.
     *
     * @param Message message is the data we've received.
     * We parse it to see if it's identity information, an
     * abort, or otherwise.
     *
     */
    public void receiveERR(Message message) {
        log.debug("Anonymous Authenticator.receiveERR");
        try {
            InputStream is = message.getDataStream().getInputStream();
            int limit = is.available();
            byte buff[] = new byte[limit];
            is.read(buff);
            if (log.isDebugEnabled()) {
                log.debug("SASL-Anonymous Authentication ERR received=>\n" + new String(buff));
            }
            abortNoThrow(new String(buff));
            synchronized (this) {
                this.notify();
            }
        } catch (Exception x) {
            message.getChannel().getSession().terminate(x.getMessage());
        }
    }

    /**
     * Method receiveANS
     * This method should never be called
     *
     * @param Message message is the data we've received.
     * We parse it to see if it's identity information, an
     * abort, or otherwise.
     *
     */
    public void receiveANS(Message message) {
        message.getChannel().getSession().terminate(ERR_UNEXPECTED_MESSAGE);
    }

    /**
     * Method receiveNUL
     * This method should never be called
     *
     * @param Message message is the data we've received.
     * We parse it to see if it's identity information, an
     * abort, or otherwise.
     *
     */
    public void receiveNUL(Message message) {
        message.getChannel().getSession().terminate(ERR_UNEXPECTED_MESSAGE);
    }
}
