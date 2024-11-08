package net.sf.jnclib.tp.ssh2;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sf.jnclib.tp.ssh2.crai.Crai;
import net.sf.jnclib.tp.ssh2.crai.CraiCipher;
import net.sf.jnclib.tp.ssh2.crai.CraiDigest;
import net.sf.jnclib.tp.ssh2.crai.CraiException;

/**
 * A server-side SSH transport, used for initiating SSH over an existing
 * socket. Once a transport has negotiated encryption, the client will usually
 * authenticate and then request {@link Channel}s.
 */
public class ServerTransport extends BaseTransport {

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
            ServerTransport.this.registerMessageHandler(ptype, handler);
        }

        public void expectPacket(byte ptype) {
            ServerTransport.this.expectPacket(ptype);
        }

        public void expectPacket(byte ptype1, byte ptype2) {
            ServerTransport.this.expectPacket(ptype1, ptype2);
        }

        public void sendMessage(Message m) throws IOException {
            ServerTransport.this.sendMessage(m);
        }

        public PKey getServerKey() {
            return ServerTransport.this.getServerKey();
        }

        public void verifyKey(byte[] hostKey, byte[] sig) throws SSHException {
            throw new SSHException("internal jaramiko error");
        }

        public void setKH(BigInteger k, byte[] h) {
            ServerTransport.this.setKH(k, h);
        }

        public void kexComplete() throws IOException {
            ServerTransport.this.activateOutbound();
        }

        public LogSink getLog() {
            return mLog;
        }
    }

    public ServerTransport(Socket socket) throws IOException {
        super(socket);
        mServerAcceptLock = new Object();
        mServerAccepts = new ArrayList();
        mServerKeyMap = new HashMap();
    }

    /**
     * Negotiate a new SSH2 session as a server.  This is the first step after
     * creating a new Transport.  A separate thread is created for protocol
     * negotiation, and this method blocks (up to a specified timeout) to find
     * out if it was successful.  If negotiation failed, an exception will be
     * thrown.
     *
     * <p>After a successful negotiation, the client will usually try to
     * authenticate and open one or more {@link Channel}s.  Methods in
     * {@link ServerInterface} will be called to handle the authentication and
     * check permissions.  If everything succeeds, newly-opened channels will
     * appear via the {@link #accept} method.
     * 
     * @param server a callback object used for authentication and permission
     *     checking
     * @param timeout_ms maximum time (in milliseconds) to wait for negotiation
     *     to finish; <code>-1</code> to wait indefinitely
     * @throws SSHException if the SSH2 negotiation fails
     * @throws IOException if there was an I/O exception on the socket
     */
    public void start(ServerInterface server, int timeout_ms) throws IOException {
        detectUnsupportedCiphers();
        mServer = server;
        mCompletionEvent = new Event();
        mActive = true;
        new Thread(new Runnable() {

            public void run() {
                mLog.debug("starting thread (server mode): " + Integer.toHexString(this.hashCode()));
                transportRun();
            }
        }, "jaramiko server feeder").start();
        if (!waitForEvent(mCompletionEvent, timeout_ms)) {
            throw new SSHException("Timeout.");
        }
    }

    /**
     * Set a banner to be sent during authentication in server mode.  This
     * method should be called before {@link #start} in order to
     * guarantee that it gets sent.
     *  
     * @param banner the authentication banner to advertise
     */
    public void setServerBanner(String banner) {
        mBanner = banner;
    }

    /**
     * Add a host key to the list of keys used for server mode.  The host key
     * is used to sign certain packets during the SSH2 negotiation, so that
     * the client can trust that we are who we say we are.  Because this is
     * used for signing, the key must contain private key info, not just the
     * public half.
     * 
     * <p>Only one key of each type (RSA or DSS) is kept.  If more than one
     * key type is set, the client gets to choose which type it prefers.
     * 
     * @param key the host key to add
     */
    public void addServerKey(PKey key) {
        mServerKeyMap.put(key.getSSHName(), key);
    }

    /**
     * Return the active host key.  After negotiating with the
     * client, this method will return the negotiated host key.  If only one
     * type of host key was set with {@link #addServerKey}, that's the only key
     * that will ever be returned.  But in cases where you have set more than
     * one type of host key (for example, an RSA key and a DSS key), the key
     * type will be negotiated by the client, and this method will return the
     * key of the type agreed on.  If the host key has not been negotiated
     * yet, null is returned.
     * 
     * @return the host key being used for this session
     */
    public PKey getServerKey() {
        return mServerKey;
    }

    /**
     * Return the next channel opened by the client over this transport.  If
     * no channel is opened before the given timeout, or the transport is
     * closed, null is returned.
     * 
     * @param timeout_ms time (in milliseconds) to wait for a channel, or 0
     *     to wait forever
     * @return a new Channel opened by the client
     */
    public Channel accept(int timeout_ms) {
        synchronized (mServerAcceptLock) {
            if (mServerAccepts.size() > 0) {
                return (Channel) mServerAccepts.remove(0);
            }
            try {
                mServerAcceptLock.wait(timeout_ms);
            } catch (InterruptedException x) {
                Thread.currentThread().interrupt();
            }
            if (!mActive) {
                return null;
            }
            if (mServerAccepts.size() > 0) {
                return (Channel) mServerAccepts.remove(0);
            }
            return null;
        }
    }

    public void close() {
        mServer.notifyClose();
        super.close();
        synchronized (mServerAcceptLock) {
            mServerAcceptLock.notifyAll();
        }
    }

    /**
     * Set a crypto library provider for jaramiko.  This setting affects all
     * Transport objects (both ClientTransport and ServerTransport), present
     * and future, and usually will only need to be set once (or never).
     * The only time you really need to set this is if you're using a
     * non-standard crypto provider (like on an embedded platform).
     * 
     * <p>If no crypto provider is set, jaramiko will attempt to use JCE,
     * which comes standard with java 1.4 and up.
     * 
     * @param crai the crypto provider to use
     */
    public static void setCrai(Crai crai) {
        sCrai = crai;
    }

    KexTransportInterface createKexTransportInterface() {
        return new MyKexTransportInterface();
    }

    String filter(List clientPrefs, List serverPrefs) {
        return super.filter(serverPrefs, clientPrefs);
    }

    protected final void activateInbound(CipherDescription desc, MacDescription mdesc) throws SSHException {
        try {
            CraiCipher inCipher = sCrai.getCipher(desc.mAlgorithm);
            byte[] key = computeKey((byte) 'C', desc.mKeySize);
            byte[] iv = computeKey((byte) 'A', desc.mBlockSize);
            inCipher.initDecrypt(key, iv);
            key = computeKey((byte) 'E', mdesc.mNaturalSize);
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
            byte[] key = computeKey((byte) 'D', desc.mKeySize);
            byte[] iv = computeKey((byte) 'B', desc.mBlockSize);
            outCipher.initEncrypt(key, iv);
            key = computeKey((byte) 'F', mdesc.mNaturalSize);
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

    void sendKexInitHook() {
        List keyTypes = mSecurityOptions.getKeys();
        for (Iterator i = keyTypes.iterator(); i.hasNext(); ) {
            String keyType = (String) i.next();
            if (!mServerKeyMap.containsKey(keyType)) {
                i.remove();
            }
        }
        mSecurityOptions.setKeys(keyTypes);
        if (getModulusPack().size() == 0) {
            List kexTypes = mSecurityOptions.getKex();
            kexTypes.remove("diffie-hellman-group-exchange-sha1");
            mSecurityOptions.setKex(kexTypes);
        }
    }

    void parseNewKeysHook() {
        if (mAuthHandler == null) {
            mAuthHandler = new AuthHandler(this, sCrai, mLog);
            mAuthHandler.useServerMode(mServer, mBanner);
        }
    }

    void kexInitHook() throws SSHException {
        mServerKey = (PKey) mServerKeyMap.get(mDescription.mServerKeyType);
        if (mServerKey == null) {
            throw new SSHException("Incompatible SSH peer (can't match requested host key type");
        }
        String temp = mDescription.mLocalCipherName;
        mDescription.mLocalCipherName = mDescription.mRemoteCipherName;
        mDescription.mRemoteCipherName = temp;
        CipherDescription tempd = mDescription.mLocalCipher;
        mDescription.mLocalCipher = mDescription.mRemoteCipher;
        mDescription.mRemoteCipher = tempd;
        temp = mDescription.mLocalMacAlgorithm;
        mDescription.mLocalMacAlgorithm = mDescription.mRemoteMacAlgorithm;
        mDescription.mRemoteMacAlgorithm = temp;
        MacDescription tempd2 = mDescription.mLocalMac;
        mDescription.mLocalMac = mDescription.mRemoteMac;
        mDescription.mRemoteMac = tempd2;
        temp = mDescription.mLocalCompression;
        mDescription.mLocalCompression = mDescription.mRemoteCompression;
        mDescription.mRemoteCompression = temp;
    }

    List checkGlobalRequest(String kind, Message m) {
        return mServer.checkGlobalRequest(kind, m);
    }

    void parseChannelOpen(Message m) throws IOException {
        String kind = m.getString();
        int reason = ChannelError.SUCCESS;
        int chanID = m.getInt();
        int initialWindowSize = m.getInt();
        int maxPacketSize = m.getInt();
        boolean reject = false;
        int myChanID = 0;
        Channel c = null;
        synchronized (mLock) {
            myChanID = getNextChannel();
            c = getChannelForKind(myChanID, kind, m);
            mChannels[myChanID] = c;
        }
        reason = mServer.checkChannelRequest(kind, myChanID);
        if (reason != ChannelError.SUCCESS) {
            mLog.debug("Rejecting '" + kind + "' channel request from client.");
            reject = true;
        }
        if (reject) {
            if (c != null) {
                synchronized (mLock) {
                    mChannels[myChanID] = null;
                }
            }
            Message mx = new Message();
            mx.putByte(MessageType.CHANNEL_OPEN_FAILURE);
            mx.putInt(chanID);
            mx.putInt(reason);
            mx.putString("");
            mx.putString("en");
            sendMessage(mx);
            return;
        }
        synchronized (mLock) {
            c.setTransport(this, mLog);
            c.setWindow(mWindowSize, mMaxPacketSize);
            c.setRemoteChannel(chanID, initialWindowSize, maxPacketSize);
            c.setServer(mServer);
        }
        Message mx = new Message();
        mx.putByte(MessageType.CHANNEL_OPEN_SUCCESS);
        mx.putInt(chanID);
        mx.putInt(myChanID);
        mx.putInt(mWindowSize);
        mx.putInt(mMaxPacketSize);
        sendMessage(mx);
        mLog.notice("Secsh channel " + myChanID + " opened.");
        synchronized (mServerAcceptLock) {
            mServerAccepts.add(c);
            mServerAcceptLock.notify();
        }
    }

    private ServerInterface mServer;

    private Map mServerKeyMap;

    private PKey mServerKey;

    private String mBanner;

    private Object mServerAcceptLock;

    private List mServerAccepts;
}
