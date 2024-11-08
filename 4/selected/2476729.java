package net.sf.jnclib.tp.ssh2;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import net.sf.jnclib.tp.ssh2.crai.Crai;
import net.sf.jnclib.tp.ssh2.crai.CraiCipher;
import net.sf.jnclib.tp.ssh2.crai.CraiDigest;
import net.sf.jnclib.tp.ssh2.crai.CraiException;

/**
 * A client-side SSH transport, used for initiating SSH over an existing
 * socket. Once a transport has negotiated encryption and authenticated,
 * tunnels (called {@link Channel}s) can be created across the session.
 */
public class ClientTransport extends BaseTransport {

    private class MyKexTransportInterface implements KexTransportInterface {

        public String getLocalVersion() {
            return mLocalVersion;
        }

        public String getRemoteVersion() {
            return mRemoteVersion;
        }

        public byte[] getLocalKexInit() {
            return mLocalKexInit;
        }

        public byte[] getRemoteKexInit() {
            return mRemoteKexInit;
        }

        public void registerMessageHandler(byte ptype, MessageHandler handler) {
            ClientTransport.this.registerMessageHandler(ptype, handler);
        }

        public void expectPacket(byte ptype) {
            ClientTransport.this.expectPacket(ptype);
        }

        public void expectPacket(byte ptype1, byte ptype2) {
            ClientTransport.this.expectPacket(ptype1, ptype2);
        }

        public void sendMessage(Message m) throws IOException {
            ClientTransport.this.sendMessage(m);
        }

        public PKey getServerKey() {
            return null;
        }

        public void verifyKey(byte[] hostKey, byte[] sig) throws SSHException {
            ClientTransport.this.verifyKey(hostKey, sig);
        }

        public void setKH(BigInteger k, byte[] h) {
            ClientTransport.this.setKH(k, h);
        }

        public void kexComplete() throws IOException {
            ClientTransport.this.activateOutbound();
        }

        public LogSink getLog() {
            return mLog;
        }
    }

    /**
     * Create a new SSH client session over an existing socket.  This only
     * initializes the ClientTransport object; it doesn't begin negotiating
     * the SSH session yet.  Use {@link #start} to begin a client session.
     * 
     * @param socket the (previously connected) socket to use for this session 
     * @throws IOException if there's an error fetching the input/output
     *     streams from the socket
     */
    public ClientTransport(Socket socket) throws IOException {
        super(socket);
    }

    /**
     * Negotiate a new SSH2 session as a client.  This is the first step after
     * creating a new Transport.  A separate thread is created for protocol
     * negotiation, and this method blocks (up to a specified timeout) to find
     * out if it was successful.  If negotiation failed, an exception will be
     * thrown.
     *
     * <p>If a host key is passed in, host key checking will be done and an
     * exception will be thrown if the server's host key doesn't match.  If the
     * host key is <code>null</code>, no host key checking is done, and you
     * must do it yourself using {@link #getRemoteServerKey}.
     * 
     * <p>After a successful negotiation, you will usually want to
     * authenticate, calling {@link #authPassword} or {@link #authPrivateKey}.
     * Then you may call {@link #openSession} or {@link #openChannel} to get
     * a {@link Channel} for data transfer.
     * 
     * @param hostkey the host key expected from the server, or <code>null</code>
     *     to skip host key checking
     * @param timeout_ms maximum time (in milliseconds) to wait for negotiation
     *     to finish; <code>-1</code> to wait indefinitely
     * @throws SSHException if the SSH2 negotiation fails or the host key
     *     supplied by the server is incorrect
     * @throws IOException if there was an I/O exception on the socket
     */
    public void start(PKey hostkey, int timeout_ms) throws IOException {
        detectUnsupportedCiphers();
        if (hostkey != null) {
            mSecurityOptions.setKeys(Arrays.asList(new String[] { hostkey.getSSHName() }));
        }
        mCompletionEvent = new Event();
        mActive = true;
        new Thread(new Runnable() {

            public void run() {
                mLog.debug("starting thread (client mode): " + Integer.toHexString(this.hashCode()));
                transportRun();
            }
        }, "jaramiko client feeder").start();
        if (!waitForEvent(mCompletionEvent, timeout_ms)) {
            throw new SSHException("Timeout.");
        }
        if (hostkey != null) {
            PKey skey = getRemoteServerKey();
            if (!skey.equals(hostkey)) {
                mLog.debug("Bad host key from server");
                mLog.debug("Expected: " + hostkey.getSSHName() + ": " + Util.encodeHex(hostkey.getFingerprint()));
                mLog.debug("Got     : " + skey.getSSHName() + ": " + Util.encodeHex(skey.getFingerprint()));
                throw new SSHException("Bad host key from server");
            }
            mLog.debug("Host key verified (" + skey.getSSHName() + ")");
        }
    }

    /**
     * Set a listener for banner events from the remote SSH server.  The
     * listener will only receive callback events if the server sends a
     * banner during authentication.
     * 
     * @param listener the new listener
     */
    public void setBannerListener(BannerListener listener) {
        mBannerListener = listener;
    }

    /**
     * Try to authenticate to the SSH2 server using no authentication at all.
     * This will almost always fail.  It may be useful for determining the list
     * of authentication types supported by the server, by catching the
     * {@link BadAuthenticationType} exception raised.
     * 
     * @param username the username to authenticate as
     * @param timeout_ms how long to wait for a response (in milliseconds);
     *     <code>-1</code> to wait forever
     * @return a list of auth type permissible for the next step of
     *     authentication (normally empty, meaning authentication is complete)
     * @throws BadAuthenticationType if "none" authentication isn't allowed
     *     by the server for this user
     * @throws AuthenticationFailedException if the authentication failed
     * @throws SSHException if an SSH protocol exception occurred
     * @throws IOException if an I/O exception occurred at the socket layer
     */
    public String[] authNone(String username, int timeout_ms) throws IOException {
        Event event = new Event();
        mAuthHandler = new AuthHandler(this, sCrai, mLog);
        mAuthHandler.setBannerListener(mBannerListener);
        mAuthHandler.authNone(username, event);
        return waitForAuthResponse(event, timeout_ms);
    }

    /**
     * Authenticate to the SSH2 server using a password.  The username and
     * password are sent over an encrypted link.  If the server doesn't support
     * password authentication, but does support "interactive" authentication,
     * an attempt at simple "interactive" password authentication is attempted.
     * (This is usually a symptom of a misconfigured Debian or Gentoo server.)
     * 
     * <p>This method blocks until authentication succeeds or fails.  On
     * failure, an exception is raised.  Otherwise, the method simply returns.
     * If the server requires multi-step authentication (which is very rare),
     * this method will return a list of auth types permissible for the next
     * step.  Otherwise, in the normal case, an empty list is returned.
     * 
     * @param username the username to authenticate as
     * @param password the password to authenticate with
     * @param timeout_ms how long to wait for a response (in milliseconds);
     *     <code>-1</code> to wait forever
     * @return a list of auth types permissible for the next step of
     *     authentication (normally empty, meaning authentication is complete)
     * @throws BadAuthenticationType if password authentication isn't allowed
     *     by the server for this user
     * @throws AuthenticationFailedException if the authentication failed
     * @throws SSHException if an SSH protocol exception occurred
     * @throws IOException if an I/O exception occurred at the socket layer
     */
    public String[] authPassword(String username, String password, int timeout_ms) throws IOException {
        return authPassword(username, password, true, timeout_ms);
    }

    /**
     * Authenticate to the SSH2 server using a password.  The username and
     * password are sent over an encrypted link.
     * 
     * <p>This method blocks until authentication succeeds or fails.  On
     * failure, an exception is raised.  Otherwise, the method simply returns.
     * If the server requires multi-step authentication (which is very rare),
     * this method will return a list of auth types permissible for the next
     * step.  Otherwise, in the normal case, an empty list is returned.
     * 
     * @param username the username to authenticate as
     * @param password the password to authenticate with
     * @param fallback <code>true</code> if an attempt at an automated
     *     "interactive" password auth should be made if the server doesn't
     *     support normal password auth
     * @param timeout_ms how long to wait for a response (in milliseconds);
     *     <code>-1</code> to wait forever
     * @return a list of auth types permissible for the next step of
     *     authentication (normally empty, meaning authentication is complete)
     * @throws BadAuthenticationType if password authentication isn't allowed
     *     by the server for this user
     * @throws AuthenticationFailedException if the authentication failed
     * @throws SSHException if an SSH protocol exception occurred
     * @throws IOException if an I/O exception occurred at the socket layer
     */
    public String[] authPassword(String username, final String password, boolean fallback, int timeout_ms) throws IOException {
        Event event = new Event();
        mAuthHandler = new AuthHandler(this, sCrai, mLog);
        mAuthHandler.setBannerListener(mBannerListener);
        mAuthHandler.authPassword(username, password, event);
        try {
            return waitForAuthResponse(event, timeout_ms);
        } catch (BadAuthenticationType x) {
            List allowed = Arrays.asList(x.getAllowedTypes());
            if (!allowed.contains("keyboard-interactive")) {
                fallback = false;
            }
            if (!fallback) {
                throw x;
            }
            try {
                return authInteractive(username, new InteractiveHandler() {

                    public String[] handleInteractiveRequest(InteractiveQuery query) throws SSHException {
                        if (query.prompts.length > 1) {
                            throw new SSHException("Fallback authentication failed.");
                        }
                        if (query.prompts.length == 0) {
                            return new String[0];
                        }
                        return new String[] { password };
                    }
                }, null, timeout_ms);
            } catch (SSHException ignored) {
                throw x;
            }
        }
    }

    /**
     * Authenticate to the SSH2 server using a private key.  The key is used to
     * sign data from the server, so it must be a key capable of signing (not
     * just a public key).
     * 
     * <p>This method blocks until the authentication succeeds or fails.  On
     * failure, an exception is raised.  Otherwise, the method simply returns.
     * If the server requires multi-step authentication (which is very rare),
     * this method will return a list of auth types permissible for the next
     * step.  Otherwise, in the normal case, an empty list is returned.
     * 
     * @param username the username to authenticate as
     * @param key the private key to authenticate with
     * @param timeout_ms how long to wait for a response (in milliseconds);
     *     <code>-1</code> to wait forever
     * @return a list of auth types permissible for the next step of
     *     authentication (normally empty, meaning authentication is complete)
     * @throws BadAuthenticationType if private key authentication isn't
     *     allowed by the server for this user
     * @throws AuthenticationFailedException if the authentication failed
     * @throws SSHException if an SSH protocol exception occurred
     * @throws IOException if an I/O exception occurred at the socket layer
     */
    public String[] authPrivateKey(String username, PKey key, int timeout_ms) throws IOException {
        Event event = new Event();
        mAuthHandler = new AuthHandler(this, sCrai, mLog);
        mAuthHandler.setBannerListener(mBannerListener);
        mAuthHandler.authPrivateKey(username, key, event);
        return waitForAuthResponse(event, timeout_ms);
    }

    /**
     * Authenticate to the SSH2 server interactively.  A handler is used to
     * answer arbitrary questions from the server.  On many servers, this is
     * just a dumb wrapper around PAM.
     * 
     * <p>This method blocks until the authentication succeeds or fails,
     * periodically calling the given {@link InteractiveHandler} to get
     * answers for authentication questions.  The handler may be called more
     * than once if the server continues to ask questions.
     * 
     * <p>If the server requires multi-step authentication (which is very
     * rare), this method will return a list of auth types permissible for the
     * next step.  Otherwise, in the normal case, an empty list is returned.
     * 
     * @param username the username to authenticate as
     * @param handler a handler for fielding interactive queries from the
     *     server and providing responses
     * @param submethods an optional list of requested interactive submethods
     *     (may be null)
     * @param timeout_ms how long to wait for a response (in milliseconds);
     *     <code>-1</code> to wait forever
     * @return a list of auth types permissible for the next step of
     *     authentication (normally empty, meaning authentication is complete)
     * @throws BadAuthenticationType if interactive authentication isn't
     *     allowed by the server for this user
     * @throws AuthenticationFailedException if the authentication failed
     * @throws SSHException if an SSH protocol exception occurred
     * @throws IOException if an I/O exception occurred at the socket layer
     */
    public String[] authInteractive(String username, InteractiveHandler handler, String[] submethods, int timeout_ms) throws IOException {
        Event event = new Event();
        mAuthHandler = new AuthHandler(this, sCrai, mLog);
        mAuthHandler.setBannerListener(mBannerListener);
        mAuthHandler.authInteractive(username, handler, event, submethods);
        return waitForAuthResponse(event, timeout_ms);
    }

    /**
     * Return the host key of the server (in client mode).
     * 
     * @return the public key of the remote server
     * @throws SSHException if no session is currently active
     */
    public PKey getRemoteServerKey() throws SSHException {
        if (!mActive || !mInitialKexDone) {
            throw new SSHException("No existing session");
        }
        return mHostKey;
    }

    /**
     * Request a new channel to the server.  {@link Channel}s are socket-like
     * objects used for the actual transfer of data across an SSH session.
     * You may only request a channel after negotiating encryption (using
     * {@link #start}) and authenticating.
     * 
     * <p>For most cases, you should call {@link #openSession} instead.  This
     * is a more advanced interface.
     * 
     * @param kind the kind of channel requested (usually <code>"session"</code>,
     *     <code>"forwarded-tcpip"</code>, or <code>"direct-tcpip"</code>)
     * @param parameters any parameters required by the channel request type
     *     (may be null)
     * @param timeout_ms time (in milliseconds) to wait for the operation to
     *     complete
     * @return a new {@link Channel} on success
     * @throws ChannelException if an error was returned by the server
     * @throws IOException if there was an error making the request, or it was
     *     rejected by the server
     */
    public Channel openChannel(String kind, List parameters, int timeout_ms) throws IOException {
        if (!mActive) {
            throw new SSHException("No existing session.");
        }
        Event e = null;
        int chanid = 0;
        synchronized (mLock) {
            chanid = getNextChannel();
            Message m = new Message();
            m.putByte(MessageType.CHANNEL_OPEN);
            m.putString(kind);
            m.putInt(chanid);
            m.putInt(mWindowSize);
            m.putInt(mMaxPacketSize);
            if (parameters != null) {
                m.putAll(parameters);
            }
            Channel c = getChannelForKind(chanid, kind, parameters);
            if (c == null) {
                throw new ChannelException(ChannelError.ADMINISTRATIVELY_PROHIBITED);
            }
            mChannels[chanid] = c;
            e = new Event();
            mChannelEvents[chanid] = e;
            c.setTransport(this, mLog);
            c.setWindow(mWindowSize, mMaxPacketSize);
            sendUserMessage(m, timeout_ms);
        }
        if (!waitForEvent(e, timeout_ms)) {
            throw new SSHException("Timeout.");
        }
        synchronized (mLock) {
            Channel c = mChannels[chanid];
            if (c == null) {
                IOException x = getException();
                if (x == null) {
                    x = new SSHException("Unable to open channel.");
                }
                throw x;
            }
            return c;
        }
    }

    /**
     * Request a new channel to the server, of type <code>"session"</code>.
     * This is just an alias for <code>openChannel("session", null, timeout_ms)</code>.
     * 
     * @param timeout_ms time (in milliseconds) to wait for the request to
     *     compelte
     * @return a new {@link Channel} on success
     * @throws IOException if an exception occurs while sending the request,
     *     or the request is rejected
     */
    public Channel openSession(int timeout_ms) throws IOException {
        return openChannel("session", null, timeout_ms);
    }

    /**
     * Set a crypto library provider for jaramiko.  This setting affects all
     * Transport objects (both ClientTransport and ServerTransport), present
     * and future, and usually will only need to be set once (or never).
     * The only time you really need to set this is if you're using a
     * non-standard crypto provider (like on an embedded platform).
     * 
     * @param crai the crypto provider to use
     */
    public static void setCrai(Crai crai) {
        sCrai = crai;
    }

    KexTransportInterface createKexTransportInterface() {
        return new MyKexTransportInterface();
    }

    protected final void activateInbound(CipherDescription desc, MacDescription mdesc) throws SSHException {
        try {
            CraiCipher inCipher = sCrai.getCipher(desc.mAlgorithm);
            byte[] key = computeKey((byte) 'D', desc.mKeySize);
            byte[] iv = computeKey((byte) 'B', desc.mBlockSize);
            inCipher.initDecrypt(key, iv);
            key = computeKey((byte) 'F', mdesc.mNaturalSize);
            CraiDigest inMac = null;
            if (mdesc.mName.equals("MD5")) {
                inMac = sCrai.makeMD5HMAC(key);
            } else if (mdesc.mName.equals("SHA1")) {
                inMac = sCrai.makeSHA1HMAC(key);
            } else if (mdesc.mName.equals("SHA256")) {
                inMac = sCrai.makeSHA256HMAC(key);
            }
            mPacketizer.setInboundCipher(inCipher, desc.mBlockSize, inMac, mdesc.mDigestSize);
        } catch (CraiException x) {
            throw new SSHException("Internal java error: " + x);
        }
    }

    protected final void activateOutbound(CipherDescription desc, MacDescription mdesc) throws SSHException {
        try {
            CraiCipher outCipher = sCrai.getCipher(desc.mAlgorithm);
            byte[] key = computeKey((byte) 'C', desc.mKeySize);
            byte[] iv = computeKey((byte) 'A', desc.mBlockSize);
            outCipher.initEncrypt(key, iv);
            key = computeKey((byte) 'E', mdesc.mNaturalSize);
            CraiDigest outMac = null;
            if (mdesc.mName == "MD5") {
                outMac = sCrai.makeMD5HMAC(key);
            } else if (mdesc.mName == "SHA1") {
                outMac = sCrai.makeSHA1HMAC(key);
            } else if (mdesc.mName == "SHA256") {
                outMac = sCrai.makeSHA256HMAC(key);
            }
            mPacketizer.setOutboundCipher(outCipher, desc.mBlockSize, outMac, mdesc.mDigestSize);
        } catch (CraiException x) {
            throw new SSHException("Internal java error: " + x);
        }
    }

    void parseChannelOpen(Message m) throws IOException {
        String kind = m.getString();
        int chanID = m.getInt();
        mLog.debug("Rejecting '" + kind + "' channel request from server.");
        Message mx = new Message();
        mx.putByte(MessageType.CHANNEL_OPEN_FAILURE);
        mx.putInt(chanID);
        mx.putInt(ChannelError.ADMINISTRATIVELY_PROHIBITED);
        mx.putString("");
        mx.putString("en");
        sendMessage(mx);
    }

    private String[] waitForAuthResponse(Event e, int timeout_ms) throws IOException {
        if (!waitForEvent(e, timeout_ms)) {
            throw new SSHException("Timeout.");
        }
        if (!mAuthHandler.isAuthenticated()) {
            IOException x = getException();
            if (x == null) {
                x = new AuthenticationFailedException();
            } else if (x instanceof PartialAuthentication) {
                return ((PartialAuthentication) x).getAllowedTypes();
            }
            throw x;
        }
        return new String[0];
    }

    private final void verifyKey(byte[] hostKey, byte[] sig) throws SSHException {
        PKey key = PKey.createFromData(hostKey);
        mLog.debug("Server host key: " + Util.encodeHex(key.getFingerprint()) + " sig: " + Util.encodeHex(sig));
        if (!key.verifySSHSignature(sCrai, mH, new Message(sig))) {
            throw new BadSignatureException(key.getSSHName());
        }
        mHostKey = key;
    }

    private BannerListener mBannerListener;

    private PKey mHostKey;
}
