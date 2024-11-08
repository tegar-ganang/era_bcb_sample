package net.nutss.stunt;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import net.nutss.*;
import static net.nutss.Control.log;
import static net.nutss.stunt.STUNTMessage.Stage.*;

/**
 * Implements the STUNT TCP NAT Traversal protocol. The class operates in two
 * modes -- passive and active, abbreviated PE and AE respectively. PE and AE
 * coordinate over a signaling channel. A passive endpoint waits for an active
 * endpoint to initiate a connection. The STUNT state machine is as follows.
 * 
 * <p>
 * Hole Punching:
 * <ol>
 * <li>AE: Registers with directory server
 * <li>AE: Connects to STUNT responders #1 and #2 from the local port (AIP) and
 * learns the external IP address and ports.
 * <li>AE: listen() on AIP, sends INVITE to PE with predicted port (AEP)
 * <li>PE: connect() to AEP, Connects to STUNT responders #1, #2 from local
 * port (PIP)
 * <li>PE: if connect() to AEP succeeds, jump to verification below
 * <li>PE: listen() on PIP, sends ACCEPT to AE with predicted port (PEP)
 * <li>AE: connect() to PEP, wait for timeout
 * <li>AE: if connect() to PEP succeeds, jump to verification below
 * <li>AE: On timeout, listen() on AIP, sends CALLBACK to PE
 * <li>PE: connect() to AEP, wait for timeout
 * <li>PE: if connect() to AEP succeeds, jump to verification below
 * <li>PE: On timeout, sends SWAP to AE
 * <li>AE: sends SWAP to PE, switches role and becomes PE
 * <li>PE: switches role and becomes AE
 * </ol>
 * 
 * <p>
 * Verification:
 * <ol>
 * <li>AE, PE: write random nonce
 * <li>AE, PE: read peer's nonce, XOR with shared secret, write it back to the
 * socket
 * <li>AE, PE: verify nonce read from socket is original nonce XOR shared
 * secret
 * <li>AE, PE: if verification fails, jump back to SWAP states above
 * </ol>
 * 
 * @author Saikat Guha
 * @version %I%, %G%
 */
class STUNTCont implements Continuation {

    /**
	 * Magic bits for verifying STUNT connecctions
	 */
    private static final int STUNT_MAGIC = 0x5ca1ab1e;

    /**
	 * Retry timer for resending signaling messages, and minor TCP timeouts in
	 * milliseconds.
	 */
    private static final int T_RETRY = 4000;

    /**
	 * Long retry timer, mostly for TCP related timeouts in milliseconds.
	 */
    private static final int T_EXPIRE = 16000;

    /**
	 * Short timer for preempting TCP in milliseconds.
	 */
    private static final int T_SHORT = 2000;

    /**
	 * Number of times to try the connection. Each try involves one run of the
	 * STUNT protocol as an Active Endpoint and one run as a Passive Endpoint.
	 */
    private static final int MAX_ATTEMPTS = 1;

    /**
	 * Exception to hand to user function.
	 */
    private Exception reason = null;

    /**
	 * Whether operating as an Active Endpoint or not.
	 */
    private boolean isActive = false;

    /**
	 * States for STUNT state machine. No blocking or lengthy operations in any
	 * state. State transisions: <img src="doc-files/STUNTCont.State-1.gif">
	 */
    private enum State {

        /** Start state for Active Endpoint (AE) */
        A_START(0), /**
		 * AE: Awaiting response from STUNT Responder 1. Registration succeeded,
		 * connect() to responder 1 initialized.
		 */
        A_ST1(T_EXPIRE, 3), /**
		 * AE: Awaiting response from STUNT Responder 2. STUNT Responder 1
		 * responded, connect() to responder 2 initialized.
		 */
        A_ST2(T_EXPIRE, 3), A_INV_SENT(T_EXPIRE, 6), /**
		 * AE: Awaiting direct connect or timeout. Remote endpoint sent its
		 * external port prediction. direct connect to it initiated in case its
		 * NAT is EF=I. Connection will timeout if not.
		 */
        A_INV_ACKD(T_SHORT), /**
		 * AE: Awaiting direct connect. Connect attempt timeout, but hole has
		 * been punched. Remote endpoint should be able to connect now. Send
		 * CALLBACK request.
		 */
        A_CB_SENT(T_RETRY, T_EXPIRE / T_RETRY), /**
		 * AE: Awaiting remote nonce. Direct connect succeeded. Sent nonce for
		 * verification stage. Awaiting remote endpoints nonce for echo service.
		 */
        A_ECHO(T_RETRY), /**
		 * AE: Awaiting nonce response. Remote nonce received, transformed and
		 * echoed back.
		 */
        A_DONE(T_RETRY), /**
		 * AE: Awaiting invite. Last attempt was aborted. Assumed role of
		 * passive endpoint. SWAP request sent.
		 */
        A_SWAP(T_RETRY, 2 * T_EXPIRE / T_RETRY), /** Starting state for Passive Endpoint (PE) */
        P_START(0), /**
		 * PE: Awaiting direct connect or response from STUNT Responder 1.
		 * INVITE with remote port prediction received. direct connect()
		 * started, connect() to responder 1 initialized.
		 */
        P_ST1(T_EXPIRE, 3), /**
		 * PE: Awaiting response from STUNT Responder 2. STUNT Responder 1
		 * responded, connect() to responder 2 initialized.
		 */
        P_ST2(T_EXPIRE, 3), /**
		 * PE: Awaiting direct connect or CALLBACK. STUNT responder 2 responded;
		 * port prediction completed. Remote endpoint has been ACCEPT'ed. Server
		 * socket listening incase remote endpoint can connect directly i.e. NAT
		 * is EF=I. Otherwise, expecting remote endpoint to request a CALLBACK
		 */
        P_INV_ACKD(T_EXPIRE, 6), /**
		 * PE: Awaiting direct connect. Remote endpoint requested CALLBACK.
		 * connect() to remote endpoint started
		 */
        P_CB_RCVD(T_EXPIRE), /**
		 * PE: Awaiting remote nonce. Direct connect succeeded. Sent nonce for
		 * verification stage. Awaiting remote endpoints nonce for echo service.
		 */
        P_ECHO(T_RETRY), /**
		 * PE: Awaiting nonce response. Remote nonce received, transformed and
		 * echoed back.
		 */
        P_DONE(T_RETRY), /**
		 * PE: Awaiting SWAP. Last attempt was aborted. Assumed role of active
		 * endpoint. SWAP request sent. Waiting for remote endpoint to
		 * acknowledge it has swapped roles.
		 */
        P_SWAP(T_RETRY, 2 * T_EXPIRE / T_RETRY), /**
		 * STUNT protocol terminated. If verification succeeded, then socket is
		 * connected. Otherwise, attempt was aborted either due to an
		 * unrecoverable error, or after trying the connection attempt a number
		 * of times and giving up.
		 */
        STOP(-1);

        /**
		 * Timeout between retries for a given state in milliseconds.
		 */
        private final int timeout;

        /**
		 * Number of times the last signaling message should be resent when a
		 * state timeout expires.
		 */
        private final int retries;

        /**
		 * Creates a new state with the given timeout and retries counter
		 * 
		 * @param timeout
		 *            timeout between retries
		 * @param retries
		 *            number of retries before aborting
		 */
        State(int timeout, int retries) {
            this.timeout = timeout;
            this.retries = retries;
        }

        /**
		 * Creates a new state with the given timer and a default retry counter
		 * of 1.
		 * 
		 * @param timeout
		 *            timeout between retries in milliseconds
		 */
        State(int timeout) {
            this(timeout, 1);
        }

        /**
		 * Returns the timeout for the state.
		 * 
		 * @return timeout in milliseconds
		 */
        int getTimeout() {
            return timeout;
        }

        /**
		 * Returns the number of retries for the state.
		 * 
		 * @return number of retries
		 */
        int getRetries() {
            return retries;
        }
    }

    ;

    /**
	 * Current state of the local endpoint.
	 */
    private State mState;

    private int iTryed = 0;

    /**
	 * Number of times the last signalig packet has been transmitted.
	 */
    private int tries;

    /**
	 * ID for the local endpoint. Usually userdefined as a UID or email address
	 * or some sort.
	 */
    private URI id;

    /**
	 * ID for the remote endpoint. Usually userdefined as a UID or email address
	 * or some sort.
	 */
    URI dst;

    /**
	 * signaling channel
	 */
    private SignalingContext sig;

    /**
	 * Addresses of STUNT responders. Responders echo back the source IP address
	 * and port of a TCP connection made to them as observed by them (i.e.
	 * external NAT address and port).
	 */
    private SocketAddress[] responders;

    /**
	 * Signaling packet used to communicate with remote endpoint.
	 */
    private SignalingMessage msg;

    /**
	 * TCP socket used during connection establishment.
	 */
    private STUNTSocket asock;

    /**
	 * TCP STUNTSocket used during connection establishment.
	 */
    private STUNTSocket bsock;

    /**
	 * TCP STUNTSocket for the final established connection.
	 */
    private STUNTSocket fsock;

    /**
	 * IP address and port responses from the two STUNT servers. Used for
	 * predicting the next NAT'ed address and port.
	 */
    private InetSocketAddress[] eaddrs = new InetSocketAddress[2];

    /**
	 * Local address corresponding to the predicted external address.
	 */
    private SocketAddress iaddr;

    /**
	 * TCP server STUNTSocket to accept incoming connections.
	 */
    private STUNTServerSocket psock;

    /**
	 * STUNT protocol data to be send to remote host. Sent as the payload of the
	 * signaling packet.
	 */
    private STUNTMessage stuntState = new STUNTMessage();

    /**
	 * STUNT protocol data from the remote host. Received from the payload of
	 * the signaling packet.
	 */
    private STUNTMessage peerState;

    /**
	 * Random nonce used for verification of full-duplex'ness of established TCP
	 * stream.
	 */
    private int nonce;

    /**
	 * Number of times the endpoint has switched to of from Active mode. A full
	 * connection attempt requires two swaps. The number of attempts is governed
	 * by MAX_ATTEMPTS.
	 */
    private int swapped;

    /**
	 * Buffer to write an integer during connection verification.
	 */
    ByteBuffer outbuf = ByteBuffer.allocate(4);

    /**
	 * Buffer to read an integer during connection verification.
	 */
    ByteBuffer inbuf = ByteBuffer.allocateDirect(4);

    /**
	 * Whether or not a verified TCP connection has been established between the
	 * two endpoints.
	 */
    boolean established;

    /**
	 * Constructs an unconnected STUNT endpoint.
	 * 
	 * @param dst
	 *            ID of remote endpoint
	 * @param id
	 *            IP of local endpoint
	 * @param context
	 *            signaling channel
	 * @param responders
	 *            IP addresses and ports of STUNT responders
	 */
    STUNTCont(URI dst, URI id, SignalingContext context, SocketAddress[] responders) {
        this.sig = context;
        this.dst = dst;
        this.id = id;
        this.responders = responders;
    }

    /**
	 * Initializes a STUNT endpoint.
	 * 
	 * @param msg
	 *            If null, endpoint becomes an Active Endpoint. Otherwise,
	 *            message must be a STUNT INVITE message
	 * @return signaling message to match against
	 */
    public SignalingMessage init(SignalingMessage msg) {
        if (msg == null) {
            setState(State.A_START);
            this.msg = sig.createMessage(id, dst, stuntState);
            isActive = true;
            return this.msg;
        } else {
            setState(State.P_START);
            peerState = (STUNTMessage) msg.getPayload();
            this.msg = sig.createReply(msg, stuntState);
            isActive = false;
            return this.msg;
        }
    }

    /**
	 * Resends the last signaling packet unless the retry limit of the current
	 * state has been exceeded.
	 * 
	 * @return true if the packet was sent, false otherwise
	 */
    private boolean resend() throws IOException {
        if (tries >= mState.getRetries()) return false;
        tries++;
        sig.send(msg);
        return true;
    }

    /**
	 * Sends a signaling packet. Resets the retry counter.
	 * 
	 * @param payload
	 *            payload to send
	 */
    private void send(STUNTMessage payload) throws IOException {
        tries = 0;
        msg = sig.setMessage(msg, payload);
        resend();
    }

    /**
	 * Initiates a non-blocking connect on a TCP socket.
	 * 
	 * @param local
	 *            Local IP address and port to bind to. null to bind to any
	 *            available address.
	 * @param remote
	 *            Remote IP address and port to connect to.
	 * @param sel
	 *            Selector that will receive connect() completion events.
	 * @return a newly created SocketChannel set to non-blocking connect to
	 *         given destination
	 * @throws Exception
	 *             if something goes wrong
	 */
    private STUNTSocket getNBConnSock(SocketAddress local, SocketAddress remote, STUNTSelector sel, boolean bConnectAsEvent) {
        try {
            Socket sock = new Socket();
            sock.setReuseAddress(true);
            if (local != null) sock.bind(local);
            STUNTSocket s = new STUNTSocket(sock, this, sel, remote, bConnectAsEvent);
            s.start();
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * Initiates a non-blocking accept on a TCP server socket.
	 * 
	 * @param local
	 *            Local IP address and port to bind to.
	 * @param sel
	 *            Selector that will receive accept() events.
	 * @return a newly created ServerrSocketChannel set to non-blocking accept
	 *         incoming connections
	 * @throws Exception
	 *             if something goes wrong
	 */
    private STUNTServerSocket getNBLstnSock(SocketAddress local, STUNTSelector sel) throws Exception {
        ServerSocket sock = new ServerSocket();
        sock.setReuseAddress(true);
        sock.bind(local);
        STUNTServerSocket sv = new STUNTServerSocket(sock, this, sel);
        sv.start();
        return sv;
    }

    /**
	 * Predicts the external address. Prediction is based on responses from
	 * STUNT responders. If two back-to-back TCP streams to two STUNT responders
	 * received external mapping (A1,P1), (A2,P2) then predicted mapping is
	 * (A1,P2+(P2-P1)).
	 * 
	 * @return predicted external mapping
	 */
    private InetSocketAddress predict() {
        return new InetSocketAddress(eaddrs[0].getAddress(), 2 * eaddrs[1].getPort() - eaddrs[0].getPort());
    }

    /**
	 * Reads one Object from the TCP stream and closes it
	 * 
	 * @param sock
	 *            TCP stream to read from
	 * @return object read
	 * @throws Exception
	 *             if something bad happens
	 */
    private Object readOne(STUNTSocket sock) throws Exception {
        Object o = new ObjectInputStream(sock.is).readObject();
        sock.close();
        return o;
    }

    /**
	 * Reads one integer from a TCP stream
	 * 
	 * @param sock
	 *            stream
	 * @return integer read
	 * @throws Exception
	 *             if something bad happens
	 */
    private int readInt(STUNTSocket sock) throws Exception {
        try {
            return sock.readInt();
        } catch (Exception e) {
            abort(e);
            return -1;
        }
    }

    /**
	 * Writes one integer to a TCP stream
	 * 
	 * @param sock
	 *            stream
	 * @param val
	 *            integer to write
	 * @throws Exception
	 *             if something bad happens
	 */
    private void writeInt(STUNTSocket sock, int val) throws Exception {
        try {
            sock.writeInt(val);
        } catch (Exception e) {
            abort(e);
        }
    }

    /**
	 * Begins verification process of a established TCP stream.
	 * 
	 * @param fsock
	 *            Stream to verify
	 * @param sel
	 *            selector that will receive read() events
	 * @throws Exception
	 *             if something goes wrong
	 */
    private void verifyConnected(STUNTSocket fsock) throws Exception {
        nonce = (int) (Math.random() * Integer.MAX_VALUE);
        writeInt(fsock, nonce);
    }

    /**
	 * Resets the endpoint state.
	 * 
	 * @param sel
	 *            Selector where keys for various sockets were registered.
	 */
    private void reset() {
        closeSocket(ASOCK + BSOCK + PSOCK + FSOCK);
        peerState = null;
    }

    /**
	 * Abort the connection setup.
	 * 
	 * @param e
	 *            Exception to report to user
	 * @throws Exception
	 */
    private void abortFatal(Exception e) throws Exception {
        reason = e;
        log.throwing("STUNTCont", "abortFatal", e);
        throw new Exception("STUNT attempt aborted.");
    }

    /**
	 * Processes an error during the connection setup. It may result in the
	 * connection attempt being restarted with swapped roles, or may abort the
	 * connection attempt if it has been retried enough times.
	 * 
	 * @param e
	 *            Exception to report to user
	 * @throws Exception
	 */
    private void abort(Exception e) throws Exception {
        reason = e;
        log.throwing("STUNTCont", "abort", e);
        throw new IllegalStateException("STUNT attempt aborted.");
    }

    /**
	 * Processes a timeout event.
	 * 
	 * @param sel
	 *            Selector where STUNTSocket keys are registered
	 * @throws Exception
	 *             if something bad happens
	 */
    private void setState(State st) {
        if (st != mState) {
            iTryed = 0;
            mState = st;
        }
        iTryed++;
    }

    private void handleTimeout(STUNTSelector sel) throws Exception {
        switch(mState) {
            case A_ST1:
                if (iTryed > mState.getRetries()) {
                    abort(new SocketTimeoutException("TCP connect to stunt signal server failed!"));
                    break;
                }
            case A_START:
                closeSocket(ASOCK);
                asock = getNBConnSock(null, responders[0], sel, false);
                setState(State.A_ST1);
                log.finest("Attempting first STUNT lookup.");
                break;
            case P_ST2:
            case A_ST2:
                if (iTryed > mState.getRetries()) {
                    abort(new SocketTimeoutException("TCP connect to stunt signal server failed!"));
                    break;
                }
                closeSocket(ASOCK);
                asock = getNBConnSock(iaddr, responders[1], sel, false);
                setState(mState);
                break;
            case P_ST1:
                if (iTryed > mState.getRetries()) {
                    abort(new SocketTimeoutException("TCP connect to stunt signal server failed!"));
                    break;
                }
            case P_START:
                closeSocket(ASOCK + BSOCK);
                bsock = getNBConnSock(null, peerState.pred, sel, true);
                asock = getNBConnSock(null, responders[0], sel, false);
                setState(State.P_ST1);
                log.finest("Passive client attemting first STUNT lookup and direct connect.");
                break;
            case A_INV_SENT:
                if (!resend()) abort(new SocketTimeoutException("Connect Timeout"));
                break;
            case P_CB_RCVD:
                abort(new SocketTimeoutException("Connect Timeout"));
                break;
            case P_INV_ACKD:
            case A_CB_SENT:
            case A_SWAP:
            case P_SWAP:
                if (!resend()) abort(new SocketTimeoutException("Listen Timeout"));
                break;
            case A_INV_ACKD:
                closeSocket(BSOCK + PSOCK);
                psock = getNBLstnSock(iaddr, sel);
                setState(State.A_CB_SENT);
                send(stuntState.callback());
                log.finest("Direct connect timed out. Sending callback request.");
                break;
            default:
                log.fine("Unhandled timeout in state: " + mState);
                break;
        }
    }

    /**
	 * Processes a STUNTSocket event.
	 * 
	 * @param key
	 *            Key for the event to handle
	 * @param sel
	 *            Selector where STUNTSocket keys are registered
	 * @throws Exception
	 *             If something bad happens
	 */
    void handleSocketOp(Object obj, STUNTSelector sel) throws Exception {
        STUNTSocket so = null;
        STUNTServerSocket ss = null;
        if (obj instanceof STUNTSocket) so = (STUNTSocket) obj;
        if (obj instanceof STUNTServerSocket) ss = (STUNTServerSocket) obj;
        if (so != null && so == bsock) {
            if (bsock.isConnected()) {
                fsock = bsock;
                bsock = null;
                closeSocket(ASOCK);
                verifyConnected(fsock);
                setState(isActive ? State.A_ECHO : State.P_ECHO);
                log.finest("Direct connect succeeded. Verifying.");
                return;
            }
        }
        if (ss != null && ss == psock) {
            fsock = psock.accept();
            if (fsock != null) {
                closeSocket(PSOCK);
                verifyConnected(fsock);
                setState(isActive ? State.A_ECHO : State.P_ECHO);
                log.finest("Accepted direct connect, verifying.");
            }
        }
        switch(mState) {
            case P_ST1:
            case P_ST2:
            case A_INV_ACKD:
            case P_CB_RCVD:
                if (so != null && so == bsock) {
                    if (!bsock.isConnected()) {
                        closeSocket(BSOCK);
                        log.finest("Direct connect failed.");
                        if (mState == State.P_CB_RCVD) abort(new SocketTimeoutException("Connect Timeout"));
                    }
                    return;
                }
        }
        switch(mState) {
            case P_ST1:
            case A_ST1:
                if (so != null && so == asock) {
                    if (asock.isConnected()) {
                        iaddr = asock.s.getLocalSocketAddress();
                        eaddrs[0] = (InetSocketAddress) readOne(asock);
                        asock = getNBConnSock(iaddr, responders[1], sel, false);
                        setState(isActive ? State.A_ST2 : State.P_ST2);
                        log.finest("First STUNT lookup successful(" + iaddr + "->" + eaddrs[0] + "), attemting second STUNT lookup.");
                    } else {
                        abort(new UnknownHostException("Failed contacting STUNT responder #1"));
                    }
                }
                break;
            case P_ST2:
            case A_ST2:
                if (so != null && so == asock) {
                    if (asock.isConnected()) {
                        eaddrs[1] = (InetSocketAddress) readOne(asock);
                        asock = null;
                        psock = getNBLstnSock(iaddr, sel);
                        log.finest("Second STUNT lookup successful(" + iaddr + "->" + eaddrs[1] + ").");
                        if (isActive) {
                            setState(State.A_INV_SENT);
                            send(stuntState.invite(predict()));
                            log.finest("Active client inviting passive, listening for direct connect.");
                        } else {
                            setState(State.P_INV_ACKD);
                            send(stuntState.accept(predict()));
                            log.finest("Passive client accepting inviting, listening for direct connect.");
                        }
                    } else {
                        abort(new UnknownHostException("Failed contacting STUNT responder #2"));
                    }
                }
                break;
            case P_ECHO:
            case A_ECHO:
                if (so != null && so == fsock) {
                    writeInt(fsock, readInt(fsock) ^ STUNT_MAGIC ^ id.hashCode());
                    setState(isActive ? State.A_DONE : State.P_DONE);
                    log.finest("Echoed peer's half-pipe check, checking half-pipe.");
                }
                break;
            case P_DONE:
            case A_DONE:
                if (so != null && so == fsock) {
                    if ((readInt(fsock) ^ STUNT_MAGIC ^ dst.hashCode()) == nonce) {
                        setState(State.STOP);
                        established = true;
                        fsock.stopSelect();
                        log.finest("Connection is full-pipe and verified. Success!!!");
                    } else {
                        abort(new ProtocolException("Connection verification failed."));
                    }
                }
                break;
            default:
                log.fine("Unhandled STUNTSocket event: " + obj.toString());
                if (so != null) {
                    so.close();
                    if (so == fsock) fsock = null; else if (so == asock) asock = null; else if (so == bsock) bsock = null;
                }
                if (ss != null) {
                    ss.close();
                    if (ss == psock) psock = null;
                }
                break;
        }
    }

    static final int BSOCK = 1;

    static final int PSOCK = 2;

    static final int FSOCK = 4;

    static final int ASOCK = 8;

    void closeSocket(int iType) {
        try {
            if ((iType & BSOCK) > 0 && bsock != null) {
                bsock.close();
                bsock = null;
            }
            if ((iType & ASOCK) > 0 && asock != null) {
                asock.close();
                asock = null;
            }
            if ((iType & FSOCK) > 0 && fsock != null) {
                fsock.close();
                fsock = null;
            }
            if ((iType & PSOCK) > 0 && psock != null) {
                psock.close();
                psock = null;
            }
        } catch (Exception e) {
        }
    }

    /**
	 * Handles a signaling message receive event.
	 * 
	 * @param msg
	 *            Signaling message received
	 * @param sel
	 *            Selector where STUNTSocket keys are registered
	 * @throws Exception
	 *             if something bad happens
	 */
    void handleProxyMsg(SignalingMessage msg, STUNTSelector sel) throws Exception {
        STUNTMessage msgState = msg.hasPayload() ? (STUNTMessage) msg.getPayload() : null;
        if (msgState != null && msgState.stage == STUNT_ABORT) {
            reset();
            setState(State.STOP);
            abortFatal(new SocketException("Peer aborted connection attempt."));
            return;
        }
        if (msgState != null && msgState.stage == STUNT_SWAP) {
            switch(mState) {
                case A_SWAP:
                case P_SWAP:
                    break;
                default:
                    reset();
                    if (++swapped > 2 * MAX_ATTEMPTS - 1) {
                        send(stuntState.abort());
                        setState(State.STOP);
                    } else {
                        send(stuntState.swap());
                        setState(isActive ? State.A_SWAP : State.P_SWAP);
                    }
                    break;
            }
        }
        switch(mState) {
            case A_INV_SENT:
            case P_INV_ACKD:
                if (msgState != null && msgState.stage == (isActive ? STUNT_ACCEPT : STUNT_CALLBACK)) {
                    closeSocket(BSOCK + PSOCK);
                    if (msgState.pred != null) peerState = msgState;
                    bsock = getNBConnSock(iaddr, peerState.pred, sel, true);
                    setState(isActive ? State.A_INV_ACKD : State.P_CB_RCVD);
                    log.finest("Passive client accepted invite, trying direct connect.");
                } else if (msg.isError()) {
                    abortFatal(new UnknownHostException("Unknown ID"));
                }
                break;
            case P_SWAP:
                if (msgState != null && msgState.stage == STUNT_SWAP) {
                    asock = getNBConnSock(null, responders[0], sel, false);
                    setState(State.A_ST1);
                    isActive = true;
                    log.finest("Attempting connction as active client.");
                }
                break;
            case A_SWAP:
                if (msgState != null && msgState.stage == STUNT_INVITE) {
                    setState(State.P_START);
                    peerState = msgState;
                    isActive = false;
                    log.finest("Attempting connection as passive client.");
                }
                break;
            default:
                log.fine("Unhandled message: " + msg);
        }
    }

    public int step(Object o, Dispatch d, STUNTSelector sel) {
        log.entering("STUNTCont", "step", o);
        if (mState == State.STOP) {
            new Exception().printStackTrace();
        }
        try {
            if (o instanceof SignalingMessage) handleProxyMsg((SignalingMessage) o, sel); else if (o instanceof STUNTSocket || o instanceof STUNTServerSocket) handleSocketOp(o, sel); else handleTimeout(sel);
        } catch (IllegalStateException e) {
            log.throwing("STUNTCont", "step", e);
            reset();
            if (++swapped > 2 * MAX_ATTEMPTS - 1) {
                try {
                    send(stuntState.abort());
                } catch (IOException i) {
                }
                setState(State.STOP);
            } else {
                try {
                    send(stuntState.swap());
                } catch (IOException i) {
                }
                setState(isActive ? State.A_SWAP : State.P_SWAP);
            }
        } catch (Exception e) {
            log.throwing("STUNTCont", "step", e);
            setState(State.STOP);
            try {
                send(stuntState.abort());
            } catch (IOException i) {
            }
            reset();
        }
        log.exiting("STUNTCont", "step", mState + "(" + mState.getTimeout() + ":" + tries + "/" + mState.getRetries() + ")");
        return mState.getTimeout();
    }

    public void cancel(Dispatch d) {
        if (established) {
            try {
                fsock.stopSelect();
                d.sel.remove(fsock);
                d.callback(this, fsock);
                fsock = null;
            } catch (Exception e) {
                log.throwing("STUNTCont", "cancel", e);
            }
        } else {
            d.callback(this, reason);
        }
        reset();
    }
}
