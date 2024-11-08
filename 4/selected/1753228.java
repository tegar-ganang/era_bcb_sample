package net.sf.jnclib.tp.ssh2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import net.sf.jnclib.tp.ssh2.crai.*;

abstract class BaseTransport implements Transport {

    /**
     * Create a new SSH session over an existing socket.  This only
     * initializes the Transport object; it doesn't begin negotiating the
     * SSH ession yet.  Use {@link #startClient} to begin a client session,
     * or {@link #startServer} to begin a server session.
     * 
     * @param socket the (previously connected) socket to use for this session 
     * @throws IOException if there's an error fetching the input/output
     *     streams from the socket
     */
    public BaseTransport(Socket socket) throws IOException {
        if (sCrai == null) {
            try {
                sCrai = (Crai) Class.forName("net.sf.jnclib.tp.ssh2.crai.CraiImpl").newInstance();
            } catch (Throwable t) {
                throw new RuntimeException("Unable to load default Crai: " + t);
            }
        }
        mActive = false;
        mInKex = false;
        mClearToSend = new Event();
        mLog = new NullLog();
        mSocket = socket;
        mInStream = mSocket.getInputStream();
        mOutStream = mSocket.getOutputStream();
        mSecurityOptions = new SecurityOptions(KNOWN_CIPHERS, KNOWN_MACS, KNOWN_KEYS, KNOWN_KEX, KNOWN_COMPRESSIONS);
        mChannels = new Channel[16];
        mChannelEvents = new Event[16];
        mSocket.setSoTimeout(100);
        mPacketizer = new Packetizer(mInStream, mOutStream, sCrai.getPRNG());
        mExpectedPacket1 = 0;
        mExpectedPacket2 = 0;
        mInitialKexDone = false;
        mLocalVersion = "SSH-" + PROTO_ID + "-" + CLIENT_ID;
        mRemoteVersion = null;
        mMessageHandlers = new HashMap();
        mChannelFactoryMap = new HashMap();
        mChannelFactoryMap.put("session", new Channel.Factory());
    }

    public void setLog(LogSink logger) {
        mLog = logger;
        mPacketizer.setLog(logger);
    }

    public void setDumpPackets(boolean dump) {
        mPacketizer.setDumpPackets(dump);
    }

    /**
     * Set the timeout for receiving the SSH banner from the remote server.
     * By default, this library will wait 15 seconds for a banner, to account
     * for high-latency links. You may make this timeout shorter or longer,
     * if you call this method before starting the transport.
     * 
     * @param seconds number of seconds to wait for the SSH banner when
     *     initiating handshaking
     */
    public void setBannerTimeout(int seconds) {
        mInitialBannerTimeout = seconds * 1000;
    }

    /**
     * Set the window size to be used on new channels. This is the amount of
     * un-acked data that can be in transit at once, so for high-latency
     * links, larger values may be better. This will also affect the size
     * of retransmit buffers.
     * 
     * @param size window size (in bytes) to use for new channels
     */
    public void setWindowSize(int size) {
        mWindowSize = size;
    }

    /**
     * Return the current window size used for new channels.
     * (see {@link #setWindowSize(int)})
     * 
     * @return window size (in bytes) being used for new channels
     */
    public int getWindowSize() {
        return mWindowSize;
    }

    /**
     * Set the size of the largest SSH packet we will send. By default, we
     * will use the largest packet size allowed by the protocol spec, but some
     * servers will allow larger sizes. Use this with care: some servers will
     * disconnect if they receive a packet that's "too large". You usually
     * won't need to change this.
     * 
     * @param size the new maximum packet size, in bytes
     */
    public void setMaxPacketSize(int size) {
        mMaxPacketSize = size;
    }

    /**
     * Return the current maximum packet size for the SSH protocol.
     * (see {@link #setMaxPacketSize(int)})
     * 
     * @return the maximum packet size, in bytes
     */
    public int getMaxPacketSize() {
        return mMaxPacketSize;
    }

    public SecurityOptions getSecurityOptions() {
        return mSecurityOptions;
    }

    public boolean isAuthenticated() {
        return mActive && (mAuthHandler != null) && mAuthHandler.isAuthenticated();
    }

    public String getUsername() {
        if (!mActive || (mAuthHandler == null)) {
            return null;
        }
        return mAuthHandler.getUsername();
    }

    public void setKeepAlive(int interval_ms) {
        mPacketizer.setKeepAlive(interval_ms, new KeepAliveHandler() {

            public void keepAliveEvent() {
                try {
                    sendGlobalRequest("keepalive@lag.net", null, -1);
                } catch (IOException x) {
                }
            }
        });
    }

    /**
     * Turn on/off compression.  This will only have an affect before starting
     * the transport.  By default, compression is off since it negatively
     * affects interactive sessions.
     *
     * @param compress true to ask the remote client/server to compress
     *     trafic; false to refuse compression
     */
    public void useCompression(boolean compress) {
        if (compress) {
            mSecurityOptions.setCompressions(Arrays.asList(KNOWN_COMPRESSIONS));
        } else {
            mSecurityOptions.setCompressions(Arrays.asList(new String[] { "none" }));
        }
    }

    public void renegotiateKeys(int timeout_ms) throws IOException {
        mCompletionEvent = new Event();
        sendKexInit();
        if (!waitForEvent(mCompletionEvent, timeout_ms)) {
            close();
            throw new SSHException("Timeout during key renegotiation.");
        }
    }

    public void sendIgnore(int bytes, int timeout_ms) throws IOException {
        Message m = new Message();
        m.putByte(MessageType.IGNORE);
        if (bytes <= 0) {
            byte[] b = new byte[1];
            sCrai.getPRNG().getBytes(b);
            bytes = (b[0] % 32) + 10;
        }
        byte[] data = new byte[bytes];
        sCrai.getPRNG().getBytes(data);
        m.putBytes(data);
        sendUserMessage(m, timeout_ms);
    }

    public Message sendGlobalRequest(String requestName, List parameters, int timeout_ms) throws IOException {
        if (timeout_ms > 0) {
            mCompletionEvent = new Event();
        }
        Message m = new Message();
        m.putByte(MessageType.GLOBAL_REQUEST);
        m.putString(requestName);
        m.putBoolean(timeout_ms > 0);
        if (parameters != null) {
            m.putAll(parameters);
        }
        mLog.debug("Sending global request '" + requestName + "'");
        sendUserMessage(m, timeout_ms);
        if (timeout_ms <= 0) {
            return null;
        }
        if (!waitForEvent(mCompletionEvent, timeout_ms)) {
            return null;
        }
        return mGlobalResponse;
    }

    public void close() {
        Channel[] chans;
        synchronized (mLock) {
            mActive = false;
            mPacketizer.close();
            chans = mChannels;
            mChannels = new Channel[16];
        }
        for (int i = 0; i < chans.length; i++) {
            if (chans[i] != null) {
                chans[i].unlink();
            }
        }
    }

    /**
     * Return an object containing negotiated parameters of this SSH
     * transport. These parameters include the MAC algorithm, encryption,
     * and compression in both directions. This object will be null until
     * a transport is initiated.
     * 
     * @return the negotiated parameters
     */
    public TransportDescription getDescription() {
        return mDescription;
    }

    void registerMessageHandler(byte ptype, MessageHandler handler) {
        mMessageHandlers.put(new Byte(ptype), handler);
    }

    void expectPacket(byte ptype) {
        mExpectedPacket1 = ptype;
    }

    void expectPacket(byte ptype1, byte ptype2) {
        mExpectedPacket1 = ptype1;
        mExpectedPacket2 = ptype2;
    }

    void saveException(IOException x) {
        synchronized (mLock) {
            mSavedException = x;
        }
    }

    IOException getException() {
        synchronized (mLock) {
            IOException x = mSavedException;
            mSavedException = null;
            return x;
        }
    }

    void sendMessage(Message m) throws IOException {
        mPacketizer.write(m);
        if (mPacketizer.needRekey() && !mInKex) {
            sendKexInit();
        }
    }

    final void setKH(BigInteger k, byte[] h) {
        mK = k;
        mH = h;
        if (mSessionID == null) {
            mSessionID = h;
        }
    }

    final byte[] computeKey(byte id, int nbytes) {
        byte[] out = new byte[nbytes];
        int sofar = 0;
        CraiDigest sha = mKexEngine.getKDFDigest();
        while (sofar < nbytes) {
            Message m = new Message();
            m.putMPZ(mK);
            m.putBytes(mH);
            if (sofar == 0) {
                m.putByte(id);
                m.putBytes(mSessionID);
            } else {
                m.putBytes(out, 0, sofar);
            }
            sha.reset();
            byte[] d = m.toByteArray();
            sha.update(d, 0, d.length);
            byte[] digest = sha.finish();
            if (sofar + digest.length > nbytes) {
                System.arraycopy(digest, 0, out, sofar, nbytes - sofar);
                sofar = nbytes;
            } else {
                System.arraycopy(digest, 0, out, sofar, digest.length);
                sofar += digest.length;
            }
        }
        return out;
    }

    void activateInbound() throws SSHException {
        CipherDescription desc = mDescription.mRemoteCipher;
        MacDescription mdesc = mDescription.mRemoteMac;
        activateInbound(desc, mdesc);
    }

    protected abstract void activateInbound(CipherDescription desc, MacDescription mdesc) throws SSHException;

    final void activateOutbound() throws IOException {
        Message m = new Message();
        m.putByte(MessageType.NEW_KEYS);
        sendMessage(m);
        CipherDescription desc = mDescription.mLocalCipher;
        MacDescription mdesc = mDescription.mLocalMac;
        activateOutbound(desc, mdesc);
        if (!mPacketizer.needRekey()) {
            mInKex = false;
        }
        mExpectedPacket1 = MessageType.NEW_KEYS;
    }

    protected abstract void activateOutbound(CipherDescription desc, MacDescription mdesc) throws SSHException;

    void authTrigger() {
    }

    void sendUserMessage(Message m, int timeout_ms) throws IOException {
        while (true) {
            synchronized (mClearToSend) {
                if (mClearToSend.isSet()) {
                    sendMessage(m);
                    return;
                }
            }
            if (!waitForEvent(mClearToSend, timeout_ms)) {
                return;
            }
        }
    }

    boolean isActive() {
        return mActive;
    }

    static Crai getCrai() {
        return sCrai;
    }

    static ModulusPack getModulusPack() {
        synchronized (BaseTransport.class) {
            if (sModulusPack == null) {
                sModulusPack = new ModulusPack();
                sModulusPack.readStandardResource();
            }
            return sModulusPack;
        }
    }

    byte[] getSessionID() {
        return mSessionID;
    }

    private void checkBanner() throws IOException {
        String line = null;
        for (int i = 0; i < 5; i++) {
            int timeout = BANNER_TIMEOUT;
            if (i == 0) {
                timeout = mInitialBannerTimeout;
            }
            try {
                line = mPacketizer.readline(timeout);
            } catch (InterruptedIOException x) {
                throw new SSHException("Timeout waiting for SSH protocol banner");
            }
            if (line == null) {
                throw new SSHException("Error reading SSH protocol banner");
            }
            if (line.startsWith("SSH-")) {
                break;
            }
            mLog.debug("Banner: " + line);
        }
        if (!line.startsWith("SSH-")) {
            throw new SSHException("Indecipherable protocol version '" + line + "'");
        }
        mRemoteVersion = line;
        int i = line.indexOf(' ');
        if (i > 0) {
            line = line.substring(0, i);
        }
        String[] segs = Util.splitString(line, "-", 3);
        if (segs.length < 3) {
            throw new SSHException("Invalid SSH banner");
        }
        String version = segs[1];
        String client = segs[2];
        if (!version.equals("1.99") && !version.equals("2.0")) {
            throw new SSHException("Incompatible version (" + version + " instead of 2.0)");
        }
        mLog.notice("Connected (version " + version + ", client " + client + ")");
    }

    private void sendKexInit() throws IOException {
        sendKexInitHook();
        synchronized (mClearToSend) {
            mClearToSend.clear();
        }
        byte[] rand = new byte[16];
        sCrai.getPRNG().getBytes(rand);
        Message m = new Message();
        m.putByte(MessageType.KEX_INIT);
        m.putBytes(rand);
        m.putList(mSecurityOptions.getKex());
        m.putList(mSecurityOptions.getKeys());
        m.putList(mSecurityOptions.getCiphers());
        m.putList(mSecurityOptions.getCiphers());
        m.putList(mSecurityOptions.getDigests());
        m.putList(mSecurityOptions.getDigests());
        m.putList(mSecurityOptions.getCompressions());
        m.putList(mSecurityOptions.getCompressions());
        m.putString("");
        m.putString("");
        m.putBoolean(false);
        m.putInt(0);
        mLocalKexInit = m.toByteArray();
        mInKex = true;
        sendMessage(m);
    }

    void sendKexInitHook() {
    }

    String filter(List localPrefs, List remotePrefs) {
        for (Iterator i = localPrefs.iterator(); i.hasNext(); ) {
            String c = (String) i.next();
            if (remotePrefs.contains(c)) {
                return c;
            }
        }
        return null;
    }

    boolean waitForEvent(Event e, int timeout_ms) throws IOException {
        long deadline = System.currentTimeMillis() + timeout_ms;
        while (!e.isSet()) {
            try {
                int span = (timeout_ms >= 0) ? (int) (deadline - System.currentTimeMillis()) : 100;
                if (span < 0) {
                    return false;
                }
                if (span > 100) {
                    span = 100;
                }
                if (span > 0) {
                    e.waitFor(span);
                }
            } catch (InterruptedException x) {
                Thread.currentThread().interrupt();
                return false;
            }
            if (!mActive) {
                IOException x = getException();
                if (x == null) {
                    x = new SSHException("Transport closed.");
                }
                throw x;
            }
        }
        return true;
    }

    void transportRun0() {
        try {
            mPacketizer.writeline(mLocalVersion + "\r\n");
            checkBanner();
            sendKexInit();
            mExpectedPacket1 = MessageType.KEX_INIT;
            while (mActive) {
                if (mPacketizer.needRekey() && !mInKex) {
                    sendKexInit();
                }
                Message m = null;
                try {
                    m = mPacketizer.read();
                } catch (NeedRekeyException x) {
                    continue;
                }
                if (m == null) {
                    break;
                }
                byte ptype = m.getByte();
                switch(ptype) {
                    case MessageType.IGNORE:
                        continue;
                    case MessageType.DISCONNECT:
                        parseDisconnect(m);
                        mActive = false;
                        mPacketizer.close();
                        continue;
                    case MessageType.DEBUG:
                        parseDebug(m);
                        continue;
                }
                if (mExpectedPacket1 != 0) {
                    if ((ptype != mExpectedPacket1) && (ptype != mExpectedPacket2)) {
                        if (mExpectedPacket2 != 0) {
                            throw new SSHException("Expecting packet " + MessageType.getDescription(mExpectedPacket1) + " or " + MessageType.getDescription(mExpectedPacket2) + ", got " + MessageType.getDescription(ptype));
                        } else {
                            throw new SSHException("Expecting packet " + MessageType.getDescription(mExpectedPacket1) + ", got " + MessageType.getDescription(ptype));
                        }
                    }
                    mExpectedPacket1 = 0;
                    mExpectedPacket2 = 0;
                }
                if (!parsePacket(ptype, m)) {
                    mLog.warning("Oops, unhandled packet type " + MessageType.getDescription(ptype));
                    Message resp = new Message();
                    resp.putByte(MessageType.UNIMPLEMENTED);
                    resp.putInt(m.getSequence());
                    sendMessage(resp);
                }
            }
        } catch (SSHException x) {
            mLog.error("Exception: " + x);
            logStackTrace(x);
            saveException(x);
        } catch (IOException x) {
            mLog.error("I/O exception in feeder thread: " + x);
            saveException(x);
        }
        for (int i = 0; i < mChannels.length; i++) {
            if (mChannels[i] != null) {
                mChannels[i].unlink();
            }
        }
        if (mActive) {
            mActive = false;
            mPacketizer.close();
            if (mCompletionEvent != null) {
                mCompletionEvent.set();
            }
            if (mAuthHandler != null) {
                mAuthHandler.abort();
            }
            for (int i = 0; i < mChannelEvents.length; i++) {
                if (mChannelEvents[i] != null) {
                    mChannelEvents[i].set();
                }
            }
        }
        try {
            mSocket.close();
        } catch (Exception x) {
        }
    }

    void transportRun() {
        try {
            transportRun0();
        } catch (Throwable t) {
            mLog.error("Exception from feeder thread! " + t);
            StringWriter buffer = new StringWriter();
            t.printStackTrace(new PrintWriter(buffer));
            mLog.debug(buffer.toString());
        } finally {
            mLog.debug("Feeder thread terminating.");
            this.close();
        }
    }

    private boolean parsePacket(byte ptype, Message m) throws IOException {
        MessageHandler handler = (MessageHandler) mMessageHandlers.get(new Byte(ptype));
        if (handler != null) {
            return handler.handleMessage(ptype, m);
        }
        if ((ptype >= MessageType.CHANNEL_WINDOW_ADJUST) && (ptype <= MessageType.CHANNEL_FAILURE)) {
            int chanID = m.getInt();
            Channel c = null;
            if (chanID < mChannels.length) {
                c = mChannels[chanID];
            }
            if (c != null) {
                return c.handleMessage(ptype, m);
            } else {
                mLog.debug("Channel request for unknown channel " + chanID + " (ignored, sending close)");
                try {
                    Message msg = new Message();
                    msg.putByte(MessageType.CHANNEL_CLOSE);
                    msg.putInt(chanID);
                    sendUserMessage(msg, -1);
                } catch (IOException x) {
                    mLog.debug("I/O exception while sending close: " + x.getMessage());
                }
                return true;
            }
        }
        switch(ptype) {
            case MessageType.NEW_KEYS:
                parseNewKeys();
                return true;
            case MessageType.GLOBAL_REQUEST:
                parseGlobalRequest(m);
                return true;
            case MessageType.REQUEST_SUCCESS:
                parseRequestSuccess(m);
                return true;
            case MessageType.REQUEST_FAILURE:
                parseRequestFailure(m);
                return true;
            case MessageType.CHANNEL_OPEN_SUCCESS:
                parseChannelOpenSuccess(m);
                return true;
            case MessageType.CHANNEL_OPEN_FAILURE:
                parseChannelOpenFailure(m);
                return true;
            case MessageType.CHANNEL_OPEN:
                parseChannelOpen(m);
                return true;
            case MessageType.KEX_INIT:
                parseKexInit(m);
                return true;
            case MessageType.KEX_0:
            case MessageType.KEX_1:
            case MessageType.KEX_2:
            case MessageType.KEX_3:
            case MessageType.KEX_4:
                return handleKex(ptype, m);
        }
        return false;
    }

    private void parseDisconnect(Message m) {
        int code = m.getInt();
        String desc = m.getString();
        mLog.notice("Disconnect (code " + code + "): " + desc);
    }

    private void parseDebug(Message m) {
        m.getBoolean();
        String text = m.getString();
        mLog.debug("Debug msg: " + Util.safeString(text));
    }

    private void parseNewKeys() throws SSHException {
        mLog.debug("Switch to new keys...");
        activateInbound();
        mLocalKexInit = null;
        mRemoteKexInit = null;
        mKexEngine = null;
        mK = null;
        parseNewKeysHook();
        if (!mInitialKexDone) {
            mInitialKexDone = true;
        }
        if (mCompletionEvent != null) {
            mCompletionEvent.set();
        }
        if (!mPacketizer.needRekey()) {
            mInKex = false;
        }
        synchronized (mClearToSend) {
            mClearToSend.set();
        }
    }

    void parseNewKeysHook() {
    }

    private void parseGlobalRequest(Message m) throws IOException {
        String kind = m.getString();
        boolean wantReply = m.getBoolean();
        mLog.debug("Received global request '" + kind + "'");
        List response = checkGlobalRequest(kind, m);
        if (wantReply) {
            Message mx = new Message();
            if (response != null) {
                mx.putByte(MessageType.REQUEST_SUCCESS);
                mx.putAll(response);
            } else {
                mx.putByte(MessageType.REQUEST_FAILURE);
            }
            sendMessage(mx);
        }
    }

    List checkGlobalRequest(String kind, Message m) {
        return null;
    }

    void kexInitHook() throws SSHException {
    }

    abstract KexTransportInterface createKexTransportInterface();

    private void parseKexInit(Message m) throws IOException {
        synchronized (mClearToSend) {
            mClearToSend.clear();
        }
        if (mLocalKexInit == null) {
            sendKexInit();
        }
        m.getBytes(16);
        List kexAlgorithmList = m.getList();
        List serverKeyAlgorithmList = m.getList();
        List clientEncryptAlgorithmList = m.getList();
        List serverEncryptAlgorithmList = m.getList();
        List clientMacAlgorithmList = m.getList();
        List serverMacAlgorithmList = m.getList();
        List clientCompressAlgorithmList = m.getList();
        List serverCompressAlgorithmList = m.getList();
        m.getList();
        m.getList();
        m.getBoolean();
        m.getInt();
        String agreedLocalCompression = filter(mSecurityOptions.getCompressions(), clientCompressAlgorithmList);
        String agreedRemoteCompression = filter(mSecurityOptions.getCompressions(), serverCompressAlgorithmList);
        if ((agreedLocalCompression == null) || (agreedRemoteCompression == null)) {
            throw new SSHException("Incompatible SSH peer (no acceptable compression)");
        }
        String agreedKex = filter(mSecurityOptions.getKex(), kexAlgorithmList);
        if (agreedKex == null) {
            throw new SSHException("Incompatible SSH peer (no acceptable kex algorithm)");
        }
        String agreedServerKey = filter(mSecurityOptions.getKeys(), serverKeyAlgorithmList);
        if (agreedServerKey == null) {
            throw new SSHException("Incompatible SSH peer (no acceptable host key)");
        }
        String agreedLocalCipher = filter(mSecurityOptions.getCiphers(), clientEncryptAlgorithmList);
        String agreedRemoteCipher = filter(mSecurityOptions.getCiphers(), serverEncryptAlgorithmList);
        if ((agreedLocalCipher == null) || (agreedRemoteCipher == null)) {
            throw new SSHException("Incompatible SSH peer (no acceptable ciphers)");
        }
        String agreedLocalMac = filter(mSecurityOptions.getDigests(), clientMacAlgorithmList);
        String agreedRemoteMac = filter(mSecurityOptions.getDigests(), serverMacAlgorithmList);
        if ((agreedLocalMac == null) || (agreedRemoteMac == null)) {
            throw new SSHException("Incompatible SSH peer (no accpetable macs)");
        }
        TransportDescription d = mDescription = new TransportDescription();
        d.mKexName = agreedKex;
        d.mServerKeyType = agreedServerKey;
        d.mLocalCipherName = agreedLocalCipher;
        d.mLocalCipher = (CipherDescription) sCipherMap.get(agreedLocalCipher);
        d.mRemoteCipherName = agreedRemoteCipher;
        d.mRemoteCipher = (CipherDescription) sCipherMap.get(agreedRemoteCipher);
        d.mLocalMacAlgorithm = agreedLocalMac;
        d.mLocalMac = (MacDescription) sMacMap.get(agreedLocalMac);
        d.mRemoteMacAlgorithm = agreedRemoteMac;
        d.mRemoteMac = (MacDescription) sMacMap.get(agreedRemoteMac);
        d.mLocalCompression = agreedLocalCompression;
        d.mRemoteCompression = agreedRemoteCompression;
        kexInitHook();
        mLog.debug(d.toString());
        byte[] data = m.toByteArray();
        mRemoteKexInit = new byte[m.getPosition()];
        System.arraycopy(data, 0, mRemoteKexInit, 0, m.getPosition());
        Class kexClass = (Class) sKexMap.get(agreedKex);
        if (kexClass == null) {
            throw new SSHException("Oops!  Negotiated kex " + agreedKex + " which I don't implement");
        }
        try {
            mKexEngine = (Kex) kexClass.newInstance();
        } catch (Exception x) {
            throw new SSHException("Internal java error: " + x);
        }
        mKexEngine.startKex(createKexTransportInterface(), sCrai);
    }

    private boolean handleKex(byte ptype, Message m) throws IOException {
        if (mKexEngine != null) {
            return mKexEngine.handleMessage(ptype, m);
        }
        return false;
    }

    private void parseRequestSuccess(Message m) throws IOException {
        mLog.debug("Global request successful.");
        mGlobalResponse = m;
        if (mCompletionEvent != null) {
            mCompletionEvent.set();
        }
    }

    private void parseRequestFailure(Message m) throws IOException {
        mLog.debug("Global request denied.");
        mGlobalResponse = null;
        if (mCompletionEvent != null) {
            mCompletionEvent.set();
        }
    }

    private void parseChannelOpenSuccess(Message m) {
        int chanID = m.getInt();
        int serverChanID = m.getInt();
        int serverWindowSize = m.getInt();
        int serverMaxPacketSize = m.getInt();
        synchronized (mLock) {
            Channel c = mChannels[chanID];
            if (c == null) {
                mLog.warning("Success for unrequested channel! [??]");
                return;
            }
            c.setRemoteChannel(serverChanID, serverWindowSize, serverMaxPacketSize);
            mLog.notice("Secsh channel " + chanID + " opened.");
            if (mChannelEvents[chanID] != null) {
                mChannelEvents[chanID].set();
                mChannelEvents[chanID] = null;
            }
        }
    }

    private void parseChannelOpenFailure(Message m) {
        int chanID = m.getInt();
        int reason = m.getInt();
        String reasonStr = m.getString();
        m.getString();
        String reasonText = ChannelError.getDescription(reason);
        mLog.notice("Secsh channel " + chanID + " open FAILED: " + reasonStr + ": " + reasonText);
        synchronized (mLock) {
            saveException(new ChannelException(reason));
            mChannels[chanID] = null;
            if (mChannelEvents[chanID] != null) {
                mChannelEvents[chanID].set();
                mChannelEvents[chanID] = null;
            }
        }
    }

    abstract void parseChannelOpen(Message m) throws IOException;

    public void registerChannelKind(String kind, ChannelFactory factory) {
        mChannelFactoryMap.put(kind, factory);
    }

    Channel getChannelForKind(int chanid, String kind, Message params) {
        ChannelFactory factory = (ChannelFactory) mChannelFactoryMap.get(kind);
        if (factory == null) {
            mLog.notice("Cannot find a ChannelFactory for the channel kind '" + kind + "'; using default Channel");
            factory = new Channel.Factory();
        }
        return factory.createChannel(kind, chanid, params);
    }

    Channel getChannelForKind(int chanid, String kind, List params) {
        ChannelFactory factory = (ChannelFactory) mChannelFactoryMap.get(kind);
        if (factory == null) {
            mLog.notice("Cannot find a ChannelFactory for the channel kind '" + kind + "'; using default Channel");
            factory = new Channel.Factory();
        }
        return factory.createChannel(kind, chanid, params);
    }

    int getNextChannel() {
        for (int i = 0; i < mChannels.length; i++) {
            if (mChannels[i] == null) {
                return i;
            }
        }
        int old = mChannels.length;
        Channel[] nc = new Channel[old * 2];
        System.arraycopy(mChannels, 0, nc, 0, old);
        mChannels = nc;
        Event[] ne = new Event[old * 2];
        System.arraycopy(mChannelEvents, 0, ne, 0, old);
        mChannelEvents = ne;
        return old;
    }

    protected void unlinkChannel(int chanID) {
        synchronized (mLock) {
            mChannels[chanID] = null;
        }
    }

    private void logStackTrace(Exception x) {
        String[] s = Util.getStackTrace(x);
        for (int i = 0; i < s.length; i++) {
            mLog.debug(s[i]);
        }
    }

    void detectUnsupportedCiphers() {
        if (sCheckedCiphers) {
            return;
        }
        boolean giveAdvice = false;
        synchronized (BaseTransport.class) {
            for (Iterator i = sCipherMap.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry) i.next();
                String name = (String) entry.getKey();
                CipherDescription desc = (CipherDescription) entry.getValue();
                if (desc != null) {
                    try {
                        CraiCipher cipher = sCrai.getCipher(desc.mAlgorithm);
                        cipher.initEncrypt(new byte[desc.mKeySize], new byte[desc.mBlockSize]);
                    } catch (CraiException x) {
                        mLog.notice("Turning off unsupported encryption: " + name);
                        if (desc.mKeySize > 16) {
                            giveAdvice = true;
                        }
                        i.remove();
                    }
                }
            }
            sCheckedCiphers = true;
        }
        if (giveAdvice) {
            mLog.notice("Your java installation lacks support for 256-bit encryption.  " + "This is due to a poor choice of defaults in Sun's java.  To fix it, " + "visit: <http://java.sun.com/j2se/1.4.2/download.html> and download " + "the \"unlimited strength\" files at the bottom of the page, under " + "\"other downloads\".");
        }
    }

    private static final String PROTO_ID = "2.0";

    private static final String CLIENT_ID = "jsshlib_0.1.20080815";

    private static final int BANNER_TIMEOUT = 5000;

    private static final int DEFAULT_WINDOW_SIZE = 384 * 1024;

    private static final int DEFAULT_MAX_PACKET_SIZE = 34816;

    private static Map sCipherMap = new LinkedHashMap();

    private static Map sMacMap = new HashMap();

    private static Map sKeyMap = new HashMap();

    private static Map sKexMap = new LinkedHashMap();

    private static Map sCompressMap = new LinkedHashMap();

    private static volatile boolean sCheckedCiphers = false;

    static Crai sCrai = null;

    static ModulusPack sModulusPack = null;

    static {
        sCipherMap.put("aes256-ctr", new CipherDescription(CraiCipherAlgorithm.AES_CTR, 32, 16));
        sCipherMap.put("aes128-ctr", new CipherDescription(CraiCipherAlgorithm.AES_CTR, 16, 16));
        sCipherMap.put("aes192-ctr", new CipherDescription(CraiCipherAlgorithm.AES_CTR, 24, 16));
        sCipherMap.put("blowfish-ctr", new CipherDescription(CraiCipherAlgorithm.BLOWFISH_CTR, 16, 8));
        sCipherMap.put("aes256-cbc", new CipherDescription(CraiCipherAlgorithm.AES_CBC, 32, 16));
        sCipherMap.put("aes128-cbc", new CipherDescription(CraiCipherAlgorithm.AES_CBC, 16, 16));
        sCipherMap.put("aes192-cbc", new CipherDescription(CraiCipherAlgorithm.AES_CBC, 24, 16));
        sCipherMap.put("blowfish-cbc", new CipherDescription(CraiCipherAlgorithm.BLOWFISH_CBC, 16, 8));
        sCipherMap.put("3des-ctr", new CipherDescription(CraiCipherAlgorithm.DES3_CTR, 24, 8));
        sCipherMap.put("3des-cbc", new CipherDescription(CraiCipherAlgorithm.DES3_CBC, 24, 8));
        sCipherMap.put("none", new CipherDescription(CraiCipherAlgorithm.NONE, 16, 16));
        sMacMap.put("hmac-sha1", new MacDescription("SHA1", 20, 20));
        sMacMap.put("hmac-md5", new MacDescription("MD5", 16, 16));
        sKeyMap.put("ssh-rsa", RSAKey.class);
        sKeyMap.put("ssh-dss", DSSKey.class);
        sKexMap.put("diffie-hellman-group-exchange-sha256", KexGex256.class);
        sKexMap.put("diffie-hellman-group-exchange-sha1", KexGex.class);
        sKexMap.put("diffie-hellman-group14-sha1", KexGroup14.class);
        sKexMap.put("diffie-hellman-group1-sha1", KexGroup1.class);
        sCompressMap.put("none", null);
    }

    private final String[] KNOWN_CIPHERS = (String[]) sCipherMap.keySet().toArray(new String[] {});

    private final String[] KNOWN_MACS = (String[]) sMacMap.keySet().toArray(new String[] {});

    private final String[] KNOWN_KEYS = (String[]) sKeyMap.keySet().toArray(new String[] {});

    private final String[] KNOWN_KEX = (String[]) sKexMap.keySet().toArray(new String[] {});

    private final String[] KNOWN_COMPRESSIONS = { "none" };

    int mWindowSize = DEFAULT_WINDOW_SIZE;

    int mMaxPacketSize = DEFAULT_MAX_PACKET_SIZE;

    private int mInitialBannerTimeout = 15000;

    private Socket mSocket;

    public Socket getSocket() {
        return mSocket;
    }

    public void setSocket(Socket socket) {
        mSocket = socket;
    }

    private InputStream mInStream;

    private OutputStream mOutStream;

    SecurityOptions mSecurityOptions;

    Packetizer mPacketizer;

    private Kex mKexEngine;

    protected TransportDescription mDescription = null;

    String mLocalVersion;

    String mRemoteVersion;

    byte[] mLocalKexInit;

    byte[] mRemoteKexInit;

    byte mExpectedPacket1;

    byte mExpectedPacket2;

    boolean mInKex;

    boolean mInitialKexDone;

    byte[] mSessionID;

    BigInteger mK;

    byte[] mH;

    Object mLock = new Object();

    Channel[] mChannels;

    Event[] mChannelEvents;

    boolean mActive;

    Event mCompletionEvent;

    private Event mClearToSend;

    LogSink mLog;

    private IOException mSavedException;

    AuthHandler mAuthHandler;

    private Message mGlobalResponse;

    private Map mMessageHandlers;

    private Map mChannelFactoryMap;
}
