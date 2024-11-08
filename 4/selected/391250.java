package org.beepcore.beep.profile.sasl.otp;

import java.io.InputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.beepcore.beep.core.*;
import org.beepcore.beep.profile.sasl.*;
import org.beepcore.beep.profile.sasl.otp.algorithm.*;
import org.beepcore.beep.profile.sasl.otp.database.UserDatabase;

/**
 * This class encapsulates the state associated with
 * an ongoing OTP Authentication, and provides methods
 * to handle the exchange.
 *
 * @author Eric Dixon
 * @author Huston Franklin
 * @author Jay Kint
 * @author Scott Pead
 * @version $Revision: 1.15 $, $Date: 2003/09/13 21:17:30 $
 *
 */
class OTPAuthenticator implements RequestHandler, ReplyListener {

    static final int STATE_UNKNOWN = 0;

    static final int STATE_STARTED = 1;

    static final int STATE_ID = 2;

    static final int STATE_CHALLENGE = 3;

    static final int STATE_RESPONSE = 4;

    static final int STATE_COMPLETE = 5;

    static final int STATE_ABORT = 6;

    static final String ERR_PEER_ABORTED = "Our BEEP Peer has aborted this authentication sequence";

    static final String ERR_IDENTITY_PARSE_FAILURE = "Invalid identity information submitted for OTP Authentication";

    static final String ERR_NULL_ID = "Invalid Authentication Info Provided";

    static final String ERR_OTP_AUTH_FAILURE = "Authentication Failure: Password hash doesn't match";

    static final String ERR_OTP_STATE = "Authentication Failure: Illegal OTP State Transition";

    static final String ERR_UNEXPECTED_MESSAGE = "Unexpected SASL-OTP Message";

    static final String ERR_INIT = "Error while parsing init-hex or init-word=>";

    static final String ERR_UNKNOWN_COMMAND = "Unknown SASL OTP Command=>";

    static final String ERR_CONCURRENT = "Authentication for that user already in progress";

    private static final String ERR_SEQUENCE_ZERO = "Authentication unable to proceed because the user's SASL OTP Sequence is 0.";

    private static String COLON = ":";

    static final String EXT = "ext";

    static final String HEX = "hex:";

    static final String INIT_HEX = "init-word:";

    static final String INIT_WORD = "init-hex:";

    static final String OTP_AUTH = "OTPAuthenticator";

    static final String SPACE = " ";

    static final String WORD = "word:";

    static final char SPACE_CHAR = ' ';

    private Log log = LogFactory.getLog(this.getClass());

    private int state;

    private Algorithm algorithm;

    private Channel channel;

    private Hashtable credential;

    private SASLOTPProfile profile;

    private String authenticated, authorized, initData = null, password;

    private UserDatabase database;

    /**
     * Listener API
     *
     * All of the routines below, but prior to the Initiator API,
     * are the Listener calls
     */
    OTPAuthenticator(SASLOTPProfile otpProfile) {
        log.debug("Creating Listener OTP Authenticator");
        authenticated = null;
        authorized = null;
        credential = new Hashtable();
        database = null;
        password = null;
        profile = otpProfile;
        state = STATE_UNKNOWN;
        credential.put(SessionCredential.AUTHENTICATOR_TYPE, SASLOTPProfile.MECHANISM);
    }

    /**
     * API for both Listener and Initiator
     */
    void started(Channel ch) throws SASLException {
        log.debug("Starting OTP Authenticator");
        if (state != STATE_UNKNOWN) {
            throw new SASLException(ERR_OTP_STATE);
        }
        state = STATE_STARTED;
        channel = ch;
        channel.setRequestHandler(this);
    }

    /**
     * Listener API
     *
     * Receive IDs, respond with a Challenge or Exception
     */
    synchronized Blob receiveIDs(String data) throws SASLException {
        log.debug("OTP Authenticator Receiving IDs");
        if (state != STATE_STARTED) {
            abort(ERR_OTP_STATE);
        }
        if (log.isDebugEnabled()) {
            log.debug("Data is" + data);
        }
        int i = data.charAt(0);
        if (i == -1) {
            abort(ERR_IDENTITY_PARSE_FAILURE);
        } else if (i == 0) {
            authorized = null;
        } else {
            int index = 0;
            try {
                index = data.indexOf(0);
                authorized = data.substring(0, index);
            } catch (Exception x) {
                authorized = null;
            }
        }
        authenticated = data.substring(data.indexOf(0) + 1);
        if (!profile.validateIdentity(authenticated, this)) {
            abort(ERR_CONCURRENT);
        }
        if (authenticated == null) {
            abort(ERR_NULL_ID);
        }
        if (log.isDebugEnabled()) {
            log.debug("Fetching DB for " + authenticated);
        }
        try {
            database = SASLOTPProfile.getUserDatabase().getUser(authenticated);
            algorithm = SASLOTPProfile.getAlgorithm(database.getAlgorithmName());
        } catch (SASLException x) {
            abort(x.getMessage());
        }
        credential.put(SessionCredential.ALGORITHM, algorithm.getName());
        if ((database.getSequence()) == 0) {
            abort(ERR_SEQUENCE_ZERO);
        }
        state = STATE_ID;
        credential.put(SessionCredential.AUTHENTICATOR, authenticated);
        if (authorized == null || authorized.equals("")) {
            credential.put(SessionCredential.AUTHORIZED, authenticated);
        } else {
            credential.put(SessionCredential.AUTHORIZED, authorized);
        }
        credential.put(SessionCredential.AUTHENTICATOR_TYPE, SASLOTPProfile.MECHANISM);
        StringBuffer challenge = new StringBuffer(128);
        challenge.append(algorithm.getName());
        challenge.append(SPACE);
        challenge.append(database.getSequence());
        challenge.append(SPACE);
        challenge.append(database.getSeed().toLowerCase());
        challenge.append(SPACE);
        challenge.append(EXT);
        if (log.isDebugEnabled()) {
            log.debug("Generated Challenge=>" + challenge.toString());
        }
        try {
            return new Blob(Blob.STATUS_NONE, challenge.toString());
        } catch (Exception x) {
        }
        abort("Failed to issue SASL OTP challenge");
        return null;
    }

    /**
     * Listener API
     *
     * Receive response to challenge, figure out if it
     * works or throw an exception if it doesn't.
     * 
     * @todo must handle the init-hex and init-word responses
     * if they're there.
     */
    synchronized SessionCredential validateResponse(String response) throws SASLException {
        boolean doInit = false;
        byte responseHash[] = null;
        log.debug("OTP Authenticator validating response");
        if (state != STATE_ID) {
            abort(ERR_OTP_STATE);
        }
        if (response.indexOf(SASLOTPProfile.HEX_INIT) != -1 || response.indexOf(SASLOTPProfile.WORD_INIT) != -1) {
            return validateInitResponse(response);
        }
        if (response.indexOf(WORD) != -1) {
            response = response.substring(WORD.length());
            long l = OTPDictionary.convertWordsToHash(response);
            responseHash = SASLOTPProfile.convertLongToBytes(l);
            if (log.isDebugEnabled()) {
                log.debug("Hacked response=>" + response);
            }
        } else if (response.indexOf(HEX) != -1) {
            response = response.substring(HEX.length());
            responseHash = SASLOTPProfile.convertHexToBytes(response);
            if (log.isDebugEnabled()) {
                log.debug("Hacked response=>" + response);
            }
        } else {
            abort(ERR_UNEXPECTED_MESSAGE);
        }
        if ((database.getSequence()) == 0) {
            throw new SequenceZeroFailure();
        }
        SessionCredential cred = validateHash(responseHash);
        database.updateLastHash(SASLOTPProfile.convertBytesToHex(responseHash));
        SASLOTPProfile.getUserDatabase().updateUserDB(database);
        return cred;
    }

    private SessionCredential validateHash(byte hash[]) throws SASLException {
        if ((database.getSequence()) == 0) {
            throw new SequenceZeroFailure();
        }
        byte nextHash[] = database.getLastHash();
        byte responseHash[] = algorithm.generateHash(hash);
        if (log.isTraceEnabled()) {
            log.trace("Test====>" + SASLOTPProfile.convertBytesToHex(responseHash));
            log.trace("Control=>" + SASLOTPProfile.convertBytesToHex(nextHash));
        }
        boolean match = true;
        for (int i = 0; i < 8; i++) {
            if (nextHash[i] != responseHash[i]) match = false;
        }
        if (!match) throw new SASLException(ERR_OTP_AUTH_FAILURE);
        state = STATE_COMPLETE;
        if (credential.size() == 0) return null;
        return new SessionCredential(credential);
    }

    synchronized SessionCredential validateInitResponse(String response) throws SASLException {
        log.debug("Validating init-* response");
        String oldHashData;
        byte oldHash[] = null;
        String newHashData;
        String newParms;
        int i = response.indexOf(COLON);
        String command;
        try {
            StringTokenizer st = new StringTokenizer(response, COLON);
            command = st.nextToken();
            oldHashData = st.nextToken();
            newParms = st.nextToken();
            newHashData = st.nextToken();
            if (log.isDebugEnabled()) {
                log.debug("Command=>" + command);
                log.debug("OldHashData=>" + oldHashData);
                log.debug("newParms=>" + newParms);
                log.debug("newHashData=>" + newHashData);
            }
            Algorithm a = SASLOTPProfile.getAlgorithm(database.getAlgorithmName());
            if (SASLOTPProfile.HEX_INIT.startsWith(command)) {
                log.debug("CMD is " + SASLOTPProfile.HEX_INIT);
                oldHash = SASLOTPProfile.convertHexToBytes(oldHashData);
            } else if (SASLOTPProfile.WORD_INIT.startsWith(command)) {
                log.debug("CMD is " + SASLOTPProfile.WORD_INIT);
                long l = OTPDictionary.convertWordsToHash(oldHashData);
                oldHash = SASLOTPProfile.convertLongToBytes(l);
            } else {
                abort(ERR_UNKNOWN_COMMAND + command);
            }
            if (log.isDebugEnabled()) {
                log.debug("Retrieved from init-* oldHash=>" + SASLOTPProfile.convertBytesToHex(oldHash));
            }
            SessionCredential cred = validateHash(oldHash);
            st = new StringTokenizer(newParms);
            String algorithm = st.nextToken();
            String sequence = st.nextToken();
            String seed = st.nextToken().toLowerCase();
            if (!OTPGenerator.validateSeed(seed)) abort("Invalid Seed");
            st = new StringTokenizer(newParms);
            if (log.isDebugEnabled()) {
                log.debug("Auth=>" + authenticated);
                log.debug("Hash=>" + newHashData);
            }
            SASLOTPProfile.getUserDatabase().addUser(authenticated, algorithm, newHashData, seed, sequence);
            log.debug("Successful Authentication!");
            return cred;
        } catch (Throwable t) {
            throw new SASLException(ERR_INIT + response + t.getMessage());
        }
    }

    /**
     * Initiator API
     *
     * ALL of the routines below are the Initiator calls.
     */
    OTPAuthenticator(SASLOTPProfile otpProfile, UserDatabase db, String pwd, String authorizedId, String authenticateId) {
        init(otpProfile, db, pwd, authorizedId, authenticateId);
    }

    private void init(SASLOTPProfile otpProfile, UserDatabase db, String pwd, String authorizedId, String authenticateId) {
        log.debug("OTP Authenticator Initiator Construtor");
        authenticated = authenticateId;
        authorized = authorizedId;
        credential = new Hashtable();
        database = db;
        if (log.isDebugEnabled()) {
            log.debug("Dict.getA()" + database.getAlgorithmName());
            log.debug("Dict.getA()" + SASLOTPProfile.getAlgorithm(database.getAlgorithmName()));
        }
        algorithm = SASLOTPProfile.getAlgorithm(database.getAlgorithmName());
        profile = otpProfile;
        password = pwd;
        state = STATE_UNKNOWN;
        credential.put(SessionCredential.AUTHENTICATOR_TYPE, SASLOTPProfile.MECHANISM);
        credential.put(SessionCredential.ALGORITHM, algorithm.getName());
        credential.put(SessionCredential.AUTHENTICATOR, authenticateId);
        if (authorizedId == null || authorizedId.equals("")) {
            credential.put(SessionCredential.AUTHORIZED, authenticateId);
        } else {
            credential.put(SessionCredential.AUTHORIZED, authorizedId);
        }
    }

    OTPAuthenticator(SASLOTPProfile otpProfile, UserDatabase db, String pwd, String authorizedId, String authenticateId, String newAlgorithm, String newHash, String newSeed, String newSequence) {
        StringBuffer sb = new StringBuffer(128);
        sb.append(COLON);
        sb.append(newAlgorithm);
        sb.append(SPACE);
        sb.append(newSequence);
        sb.append(SPACE);
        sb.append(newSeed.toLowerCase());
        sb.append(COLON);
        sb.append(newHash);
        initData = sb.toString();
        init(otpProfile, db, pwd, authorizedId, authenticateId);
    }

    /**
     * Initiator API used by SASL-OTP consumers that don't use
     * the data on the startChannel option
     *
     * If it works, we should get a challenge in our receiveRPY
     * callback ;)
     */
    void sendIdentity(String authorizeId, String authenticateId) throws SASLException {
        log.debug("OTP Authenticator sending Identities");
        int limit = authenticateId.length();
        if (authorizeId != null) {
            limit += authorizeId.length();
        }
        StringBuffer temp = new StringBuffer(limit);
        if (authorizeId != null) {
            temp.append(authorizeId);
        }
        temp.append((char) 0);
        temp.append(authenticateId);
        if (log.isDebugEnabled()) {
            log.debug("AuthOTP Using=>" + temp.toString() + "<=");
        }
        Blob blob = new Blob(Blob.STATUS_NONE, temp.toString());
        if (log.isDebugEnabled()) {
            log.debug("AuthOTP Using=>" + blob.toString() + "<=");
        }
        try {
            channel.sendMSG(new StringOutputDataStream(blob.toString()), (ReplyListener) this);
        } catch (BEEPException x) {
            abort(x.getMessage());
        }
    }

    /**
     * Initiator API
     * Receive Challenge, respond with a hash.
     */
    synchronized void receiveChallenge(Blob blob) throws SASLException {
        log.debug("OTP Authenticator received Challenge");
        if (state != STATE_STARTED) {
            abortNoThrow(ERR_OTP_STATE);
        }
        if (blob.getStatus().equals(Blob.ABORT)) {
            abort(ERR_PEER_ABORTED);
        }
        String challenge = blob.getData();
        state = STATE_CHALLENGE;
        int sequence = 0;
        String seed = null, algo = null;
        if (log.isDebugEnabled()) {
            log.debug("Tokenizing=>" + challenge);
        }
        StringTokenizer st = new StringTokenizer(challenge);
        if (st.countTokens() != 4) {
            abort("Failed to understand server's Challenge" + st.countTokens());
        }
        algo = st.nextToken();
        algorithm = SASLOTPProfile.getAlgorithm(algo);
        if (algorithm == null) {
            abort("Unrecognized algorithm in server challenge");
        }
        sequence = Integer.parseInt(st.nextToken());
        seed = st.nextToken().toLowerCase();
        if (!OTPGenerator.validateSeed(seed)) abort("Invalid Seed");
        if (log.isDebugEnabled()) {
            log.debug("Algo is=>" + algo + " seed is=>" + seed + " seq=>" + sequence);
        }
        String phrase = new String(seed + password);
        password = null;
        byte response[] = null, temp[];
        temp = phrase.getBytes();
        for (int i = 0; i < sequence; i++) {
            response = algorithm.generateHash(temp);
            temp = response;
        }
        if (log.isDebugEnabled()) {
            log.debug(SASLOTPProfile.convertBytesToHex(temp));
        }
        long l = profile.convertBytesToLong(temp);
        phrase = new String(WORD + OTPDictionary.convertHashToWords(l));
        if (log.isDebugEnabled()) {
            log.debug("Prelim response is =>" + phrase + "<=");
        }
        if (initData != null) {
            StringBuffer sb = new StringBuffer(128);
            sb.append(SASLOTPProfile.HEX_INIT);
            sb.append(SASLOTPProfile.convertBytesToHex(temp));
            sb.append(initData);
            phrase = sb.toString();
            if (log.isDebugEnabled()) {
                log.debug("Produced INIT response of " + phrase);
            }
        }
        try {
            blob = new Blob(Blob.STATUS_CONTINUE, phrase);
            channel.sendMSG(new StringOutputDataStream(blob.toString()), this);
        } catch (BEEPException x) {
            throw new SASLException("Unable to send response to challenge");
        }
    }

    /**
     * Initiator API
     * Receive response to challenge, figure out if it
     * works or throw an exception if it doesn't.
     */
    synchronized SessionCredential receiveCompletion(String response) throws SASLException {
        log.debug("OTP Authenticator Completing!");
        if (state != STATE_CHALLENGE) {
            abort(ERR_OTP_STATE);
        }
        state = STATE_COMPLETE;
        return new SessionCredential(credential);
    }

    void abort(String message) throws SASLException {
        log.error("Aborting OTP Authenticator because " + message);
        state = STATE_ABORT;
        throw new SASLException(message);
    }

    /**
     * This is designed to be called by initiator methods
     * that want to abort the authentication by sending
     * a MSG with a <blob status='abort'>reason</blob> 
     * to their peer.
     */
    void abortNoThrow(String message) {
        log.error("Aborting OTP Authenticator because " + message);
        state = STATE_ABORT;
    }

    /**
     * Method receiveMSG
     * Listener API
     *
     * We receive MSGS - IDs, and extended responses (hash)
     * in response to our challenges and stuff.
     * 
     * @param Message message is the data we've received.
     * We parse it to see if it's identity information, an
     * abort, or otherwise.
     * 
     * @throws BEEPError if an ERR message is generated
     * that's relative to the BEEP protocol is encountered.
     */
    public void receiveMSG(MessageMSG message) {
        try {
            log.debug("OTP Authenticator.receiveMSG");
            String data = null;
            Blob blob = null;
            if ((state != STATE_STARTED) && (state != STATE_ID)) {
                abort(ERR_OTP_STATE);
            }
            try {
                InputStream is = message.getDataStream().getInputStream();
                int limit = is.available();
                byte buff[] = new byte[limit];
                is.read(buff);
                blob = new Blob(new String(buff));
                data = blob.getData();
            } catch (IOException x) {
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
                Blob reply = receiveIDs(data);
                try {
                    message.sendRPY(new StringOutputDataStream(reply.toString()));
                } catch (BEEPException x) {
                    throw new SASLException(x.getMessage());
                }
                return;
            }
            SessionCredential cred = null;
            cred = validateResponse(data);
            if (cred != null) {
                profile.finishListenerAuthentication(cred, channel.getSession());
                state = STATE_COMPLETE;
                if (log.isDebugEnabled()) {
                    log.debug("" + channel.getSession() + " is valid for\n" + cred.toString());
                }
                try {
                    message.sendRPY(new StringOutputDataStream(new Blob(Blob.STATUS_COMPLETE).toString()));
                    channel.setRequestHandler(null);
                } catch (BEEPException x) {
                    profile.failListenerAuthentication(channel.getSession(), authenticated);
                    abortNoThrow(x.getMessage());
                    message.getChannel().getSession().terminate(x.getMessage());
                    return;
                }
            }
        } catch (SASLException s) {
            try {
                profile.failListenerAuthentication(channel.getSession(), authenticated);
                Blob reply = new Blob(Blob.STATUS_ABORT, s.getMessage());
                message.sendRPY(new StringOutputDataStream(reply.toString()));
            } catch (BEEPException x) {
                message.getChannel().getSession().terminate(s.getMessage());
            }
        }
    }

    /**
     * Method receiveRPY
     * Initiator API
     *
     * We receive replies to our ID messages, and to our extended responses
     * Initiator API
     *
     * @param Message message is the data we've received.
     * We parse it to see if it's identity information, an
     * abort, or otherwise.
     * 
     */
    public void receiveRPY(Message message) {
        log.debug("OTP Authenticator.receiveRPY");
        Blob blob = null;
        boolean sendAbort = true;
        try {
            if ((state != STATE_STARTED) && (state != STATE_CHALLENGE) && (state != STATE_COMPLETE)) {
                sendAbort = true;
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
                log.debug("OTPAuthenticator receiveRPY got an RPY=>" + blob.getData());
                sendAbort = false;
                abort(ERR_PEER_ABORTED + blob.getData());
            }
            if (state == STATE_STARTED) {
                receiveChallenge(blob);
                return;
            } else if (blob.getStatus() != Blob.ABORT) {
                profile.finishInitiatorAuthentication(new SessionCredential(credential), channel.getSession());
                synchronized (this) {
                    this.notify();
                }
                return;
            } else {
                abort(ERR_UNKNOWN_COMMAND);
                return;
            }
        } catch (Exception x) {
            log.error(x);
            synchronized (this) {
                this.notify();
            }
            if (sendAbort) {
                try {
                    Blob a = new Blob(Blob.STATUS_ABORT, x.getMessage());
                    channel.sendMSG(new StringOutputDataStream(a.toString()), this);
                } catch (BEEPException y) {
                    message.getChannel().getSession().terminate(y.getMessage());
                }
            }
        }
    }

    /**
     * Method receiveERR
     * 
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
        log.debug("OTP Authenticator.receiveERR");
        try {
            InputStream is = message.getDataStream().getInputStream();
            int limit = is.available();
            byte buff[] = new byte[limit];
            is.read(buff);
            Blob b = new Blob(new String(buff));
            if (log.isDebugEnabled()) {
                log.debug("ERR received=>\n" + b.getData());
            }
            abort(new String(buff));
            synchronized (this) {
                this.notify();
            }
        } catch (Exception x) {
            abortNoThrow(x.getMessage());
        }
    }

    public void receiveANS(Message message) {
        message.getChannel().getSession().terminate(ERR_UNEXPECTED_MESSAGE);
    }

    public void receiveNUL(Message message) {
        message.getChannel().getSession().terminate(ERR_UNEXPECTED_MESSAGE);
    }
}
