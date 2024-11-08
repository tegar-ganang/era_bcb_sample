package org.jmule.core.protocol.donkey;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.jmule.core.ConnectionManager;
import org.jmule.util.MiscUtil;
import org.jmule.util.Convert;

/** The base class of DonkeyClientConection
 * @version $Revision: 1.1.1.1 $
 * <br>Last changed by $Author: jmartinc $ on $Date: 2005/04/22 21:44:16 $
 */
public abstract class DonkeyConnectionSkeleton implements DonkeyConnection, DonkeyPacketConstants {

    static final Logger log = Logger.getLogger(DonkeyConnectionSkeleton.class.getName());

    public DonkeyConnectionSkeleton(SocketChannel channel) {
        this.channel = channel;
        this.dContext = DonkeyProtocol.getInstance();
        init();
    }

    public DonkeyConnectionSkeleton() {
        this(null);
    }

    private void init() {
        dSendQueue = new LinkedList();
        dReceiveQueue = new LinkedList();
        packetDecoder = new DonkeyPacketReceiver(this);
        connected = false;
        doClose = false;
        transferStart = System.currentTimeMillis();
        connectTimeOut = 20;
        state = STATE_CONNECTING;
        sendSpeeds = new int[40];
        receiveSpeeds = new int[40];
        lastActivity = transferStart;
        heartBeat = transferStart;
        if (channel != null) {
            setPeerAddress((InetSocketAddress) channel.socket().getRemoteSocketAddress());
        }
    }

    /**in seconds*/
    protected int connectTimeOut;

    protected boolean connected;

    protected boolean doClose;

    private boolean closed = false;

    protected boolean isOutbound = false;

    protected DonkeyProtocol dContext;

    protected LinkedList dSendQueue;

    protected LinkedList dReceiveQueue;

    protected DonkeyPacketReceiver packetDecoder;

    protected SocketChannel channel;

    protected long lastActivity;

    protected boolean waiting = false;

    protected boolean hasOutBuffer = false;

    protected ByteBuffer outBuffer;

    protected long totalBytesSent = 0;

    protected long totalBytesReceived = 0;

    protected int thisSecBytesSent = 0;

    protected int thisSecReceived = 0;

    protected long heartBeat = 0;

    protected long transferStart = 0;

    protected int actReceiveSpeed = 0;

    protected int actSendSpeed = 0;

    protected SelectionKey selectionKey;

    protected InetSocketAddress peerAddress;

    protected int state = 0;

    static final int STATE_UNKNOWN = 0;

    static final int STATE_CONNECTING = 10;

    static final int STATE_HANDSHAKING = 20;

    static final int STATE_CONNECTED = 30;

    static final int STATE_TRANSFERING = 40;

    static final int STATE_WAITING = 50;

    static final int STATE_ENQUEUE = 60;

    static final int STATE_UPLOADING = 70;

    static final int STATE_CLOSING = 70;

    protected int[] sendSpeeds;

    protected int[] receiveSpeeds;

    protected int speedpos = 0;

    protected int numHeartBeats = 0;

    protected boolean writeSelected = false;

    public void calcSpeed() {
        long currentTime = System.currentTimeMillis();
        long millis = (currentTime - heartBeat);
        if (millis > 0) {
            sendSpeeds[speedpos] = (int) ((thisSecBytesSent * MiscUtil.TU_MillisPerSecond) / millis);
            receiveSpeeds[speedpos] = (int) ((thisSecReceived * MiscUtil.TU_MillisPerSecond) / millis);
            thisSecBytesSent = 0;
            thisSecReceived = 0;
            heartBeat = currentTime;
            speedpos++;
            if (numHeartBeats < sendSpeeds.length) numHeartBeats++;
            if (speedpos == sendSpeeds.length - 1) speedpos = 0;
        }
    }

    private int clcspd(int[] speeds, int heartbeats) {
        int nbeats = heartbeats;
        if (nbeats > speeds.length) nbeats = speeds.length;
        if (nbeats > numHeartBeats) nbeats = numHeartBeats;
        int speed = 0;
        int pos = speedpos;
        for (int i = 0; i < nbeats; i++) {
            speed += speeds[pos - i];
            if (pos - i == 0) pos += speeds.length;
        }
        if (nbeats > 0) return speed / nbeats; else return 0;
    }

    public synchronized void addInPacket(DonkeyPacket inPacket) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(getConnectionNumber() + " received packet " + Convert.byteBufferToHexString(inPacket.getBuffer(), inPacket.getBuffer().position(), Math.min(128, inPacket.getBuffer().remaining())));
        }
        dReceiveQueue.add(inPacket);
        processIncomingPacket();
    }

    public synchronized void addOutPacket(DonkeyPacket outPacket) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(getConnectionNumber() + " sending packet " + Convert.byteBufferToHexString(outPacket.getBuffer(), outPacket.getBuffer().position(), Math.min(128, outPacket.getBuffer().remaining())));
        }
        lastActivity = System.currentTimeMillis();
        dSendQueue.add(outPacket);
        if (!writeOff && selectionKey != null && !doClose) {
            if (getChannel().socket().isClosed()) {
                log.log(Level.FINE, getConnectionNumber() + " channel closed! ", new Exception());
            } else {
                try {
                    selectionKey.interestOps(SelectionKey.OP_WRITE | selectionKey.interestOps());
                } catch (java.nio.channels.CancelledKeyException cke) {
                    log.log(Level.WARNING, getConnectionNumber() + " unexpected exception: ", cke);
                }
            }
        }
    }

    public synchronized boolean hasInput() {
        return dReceiveQueue.size() > 0;
    }

    public synchronized boolean hasOutput() {
        return ((dSendQueue.size() > 0) || hasOutBuffer());
    }

    public synchronized void processIncomingPacket() {
    }

    public boolean check(int count) {
        if (doClose) close();
        if (getChannel().socket().isClosed()) {
            log.log(Level.FINEST, getConnectionNumber() + " " + (System.currentTimeMillis() - transferStart) + " socket closed!", new Exception());
            close();
            return false;
        }
        if ((count << 1) % 5 == 0) {
            calcSpeed();
            if (count % 20 == 0) {
                if (!waiting) {
                    if ((System.currentTimeMillis() - lastActivity) > (getTimeOut() * MiscUtil.TU_MillisPerSecond)) {
                        log.finest(getConnectionNumber() + " Connection timeout. State: " + state);
                        close();
                        return false;
                    }
                }
            }
        }
        if (readOn) {
            readOn = false;
            log.fine(getConnectionNumber() + " read on " + ConnectionManager.getInstance().getLoopCount());
            selectionKey.interestOps(SelectionKey.OP_READ | selectionKey.interestOps());
        }
        if (writeOn && hasOutput()) {
            writeOn = false;
            selectionKey.interestOps(SelectionKey.OP_WRITE | selectionKey.interestOps());
        }
        return (true);
    }

    private long dataOutTraffic;

    private long dataInTraffic;

    private long otherInTraffic;

    private long otherOutTraffic;

    static final int PACKETHEADER_TRAFIC = 0x10000;

    static final boolean DIRECTION_IN = true;

    static final boolean DIRECTION_OUT = false;

    public long getStatistic(int opcode, boolean direction) {
        switch(opcode) {
            case OP_SENDINGPART:
                return DIRECTION_IN ? dataInTraffic : dataOutTraffic;
        }
        return DIRECTION_IN ? otherInTraffic : otherOutTraffic;
    }

    public void addStatistic(int opcode, boolean direction, int amount) {
        if (direction) switch(opcode) {
            case OP_SENDINGPART:
                {
                    dataInTraffic += amount;
                    break;
                }
            default:
                {
                    log.fine(getConnectionNumber() + " other in traffictype " + opcode);
                    otherInTraffic += amount;
                }
        } else switch(opcode) {
            case OP_SENDINGPART:
                {
                    dataOutTraffic += amount;
                    break;
                }
            default:
                {
                    log.fine(getConnectionNumber() + " other out traffictype " + opcode);
                    otherOutTraffic += amount;
                }
        }
    }

    private DonkeyPacket outPacket = null;

    private long startOfStartuploadReq = 0;

    public static long callCounterOut = 0;

    public static long callCounterIn = 0;

    int currentOpcode = 0;

    public synchronized void processOutput() {
        callCounterOut++;
        DonkeyUpLoadLimiter.Limiter.decConnections();
        int sendBytesInWhile = DonkeyUpLoadLimiter.Limiter.getMaxBytesPerConnection();
        writeOff = sendBytesInWhile == 0;
        while ((hasOutput() || hasOutBuffer()) && getChannel().isOpen() && sendBytesInWhile > 0 && !writeOff) {
            if (!hasOutBuffer()) {
                outPacket = getNextOutPacket();
                currentOpcode = outPacket.getBuffer().remaining() > 5 ? outPacket.getCommandId() : 0;
                setOutBuffer(outPacket.getBuffer());
                setHasOutBuffer(true);
                removeOutPacket();
            }
            ByteBuffer buf = getOutBuffer();
            int limit = buf.limit();
            int maxwrite = buf.position() + sendBytesInWhile;
            buf.limit(maxwrite > limit ? limit : maxwrite);
            try {
                int nbytes = getChannel().write(buf);
                sendBytesInWhile -= nbytes;
                if (nbytes > 0) {
                    lastActivity = System.currentTimeMillis();
                    addStatistic(currentOpcode, DIRECTION_OUT, nbytes);
                }
                addSentBytesNum(nbytes);
                buf.limit(limit);
                if (buf.remaining() == 0) {
                    log.finest(getConnectionNumber() + " sent " + Convert.byteToHex(outPacket.getCommandId()));
                    setHasOutBuffer(false);
                    setOutBuffer(null);
                    outPacket.disposePacketByteBuffer();
                } else {
                    break;
                }
            } catch (IOException e) {
                log.fine(getConnectionNumber() + " Error sending...: " + e.getMessage());
                try {
                    getChannel().close();
                } catch (IOException e2) {
                }
                doClose = true;
                return;
            }
        }
        if (hasOutput()) {
            if (writeOff) {
                selectionKey.interestOps(SELECTION_MASK_WRITE_OFF & selectionKey.interestOps());
                writeOff = false;
                writeOn = true;
            }
        } else {
            if (writeOff) {
                writeOn = true;
            }
            selectionKey.interestOps(SELECTION_MASK_WRITE_OFF & selectionKey.interestOps());
        }
    }

    private boolean readOn = false;

    private boolean readOff = false;

    private boolean writeOn = false;

    private boolean writeOff = false;

    private static final int SELECTION_MASK_READ_OFF = ~SelectionKey.OP_READ;

    private static final int SELECTION_MASK_WRITE_OFF = ~SelectionKey.OP_WRITE;

    public boolean processInput() throws IOException {
        if (doClose) return !doClose;
        callCounterIn++;
        DonkeyDownLoadLimiter.Limiter.decConnections();
        int limit = DonkeyDownLoadLimiter.Limiter.getMaxBytesPerConnection();
        if (limit == 0) {
            log.fine(getConnectionNumber() + " read off " + ConnectionManager.getInstance().getLoopCount());
            selectionKey.interestOps(SELECTION_MASK_READ_OFF & selectionKey.interestOps());
            readOff = false;
            readOn = true;
            return !doClose;
        }
        int num = packetDecoder.append(limit);
        if (num > 0) {
            lastActivity = System.currentTimeMillis();
        } else if (num == 0) {
            doClose = true;
            return !doClose;
        }
        addReceivedBytesNum(num);
        log.finest(getConnectionNumber() + " bytes read: " + num + " " + limit + " " + readOff + " " + readOn + (num > 0 ? "" : " " + System.currentTimeMillis() + " " + ConnectionManager.getInstance().getLoopCount()));
        if (readOff && !doClose) {
            log.fine(getConnectionNumber() + " read off " + ConnectionManager.getInstance().getLoopCount());
            selectionKey.interestOps(SELECTION_MASK_READ_OFF & selectionKey.interestOps());
            readOff = false;
            readOn = true;
        }
        return !doClose;
    }

    /** Provide timeout for this connection.
     * @return timeout in seconds.
     */
    protected abstract int getTimeOut();

    public synchronized DonkeyPacket getNextPacket() {
        return ((DonkeyPacket) dReceiveQueue.removeFirst());
    }

    public synchronized DonkeyPacket getNextOutPacket() {
        return ((DonkeyPacket) dSendQueue.getFirst());
    }

    public synchronized DonkeyPacket removeOutPacket() {
        return ((DonkeyPacket) dSendQueue.removeFirst());
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean doClose() {
        return doClose;
    }

    public void setConnected(boolean connected) {
        if (connected && state == STATE_CONNECTING) {
            state = STATE_CONNECTED;
            setLastActivity(System.currentTimeMillis());
        }
        this.connected = connected;
    }

    public synchronized void close() {
        if (closed) {
            return;
        }
        state = STATE_CLOSING;
        closed = true;
        doClose = true;
        log.fine("close " + getConnectionNumber() + " packets in send queue: " + dSendQueue.size() + " packets in receive queue: " + dReceiveQueue.size());
        while (!dSendQueue.isEmpty()) {
            removeOutPacket().disposePacketByteBuffer();
        }
        while (!dReceiveQueue.isEmpty()) {
            getNextPacket().disposePacketByteBuffer();
        }
        this.connected = false;
        thisSecBytesSent = 0;
        thisSecReceived = 0;
        try {
            getChannel().close();
        } catch (IOException e) {
            log.warning(getConnectionNumber() + " Error closing channel: " + e.toString());
        }
        packetDecoder.cleanup();
    }

    public boolean isOutbound() {
        return isOutbound;
    }

    public void setOutbound(boolean isOutbound) {
        this.isOutbound = isOutbound;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public DonkeyPacketReceiver getPacketDecoder() {
        return packetDecoder;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    /**
	 * Returns the lastActivity.
	 * @return long
	 */
    public long getLastActivity() {
        return lastActivity;
    }

    /**
	 * Sets the lastActivity.
	 * @param lastActivity The lastActivity to set
	 */
    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }

    /**
	 * @see org.jmule.core.protocol.donkey.DonkeyConnection#addReceivedBytesNum(int)
	 */
    public void addReceivedBytesNum(int nbytes) {
        ConnectionManager.getInstance().addBytesRecievedfromInternet(nbytes);
        readOff = !DonkeyDownLoadLimiter.Limiter.canGoOnWithTransfer(nbytes);
        thisSecReceived += nbytes;
        totalBytesReceived += nbytes;
    }

    /**
	 * @see org.jmule.core.protocol.donkey.DonkeyConnection#addSentBytesNum(int)
	 */
    public void addSentBytesNum(int nbytes) {
        ConnectionManager.getInstance().addBytesSendtoInternet(nbytes);
        writeOff = (!DonkeyUpLoadLimiter.Limiter.canGoOnWithTransfer(nbytes));
        thisSecBytesSent += nbytes;
        totalBytesSent += nbytes;
    }

    /**
	 * Returns ReceiveSpeed.
	 * @return int
	 */
    public int getReceiveSpeed(int heartbeats) {
        return clcspd(receiveSpeeds, heartbeats);
    }

    /**
	 * Returns the sendSpeed.
	 * @return int
	 */
    public int getSendSpeed(int heartbeats) {
        return clcspd(sendSpeeds, heartbeats);
    }

    /**
	 * Returns the connectTimeOut.
	 * @return int
	 */
    public int getConnectTimeoutAt() {
        return connectTimeOut;
    }

    /**
	 * Sets the connectTimeOut.
	 * @param connectTimeOut The connectTimeOut to set
	 */
    public void setConnectTimeOut(int connectTimeOut) {
        this.connectTimeOut = connectTimeOut;
    }

    /**
	 * Returns the getConnectionNumber().
	 * @return int an unique value to identify this connection 
	 */
    public int getConnectionNumber() {
        return this.hashCode();
    }

    /**
	 * @see org.jmule.core.protocol.donkey.DonkeyConnection#receiveQueueSize()
	 */
    public int receiveQueueSize() {
        return dReceiveQueue.size();
    }

    /**
	 * @see org.jmule.core.protocol.donkey.DonkeyConnection#sendQueueSize()
	 */
    public int sendQueueSize() {
        return dSendQueue.size();
    }

    /**
	 * @see org.jmule.core.protocol.donkey.DonkeyConnection#getState()
	 */
    public int getState() {
        return state;
    }

    /**
	 * Returns the outBuffer.
	 * @return ByteBuffer
	 */
    public ByteBuffer getOutBuffer() {
        return outBuffer;
    }

    /**
	 * Sets the outBuffer.
	 * @param outBuffer The outBuffer to set
	 */
    public void setOutBuffer(ByteBuffer outBuffer) {
        this.outBuffer = outBuffer;
    }

    /**
	 * Returns the hasOutBuffer.
	 * @return boolean
	 */
    public boolean hasOutBuffer() {
        return hasOutBuffer;
    }

    /**
	 * Sets the hasOutBuffer.
	 * @param hasOutBuffer The hasOutBuffer to set
	 */
    public void setHasOutBuffer(boolean hasOutBuffer) {
        this.hasOutBuffer = hasOutBuffer;
    }

    /**
	 * Returns the selectionKey.
	 * @return SelectionKey
	 */
    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    /**
	 * Sets the selectionKey.
	 * @param selectionKey The selectionKey to set
	 */
    public void setSelectionKey(SelectionKey selectionKey) {
        assert this.selectionKey == null;
        this.selectionKey = selectionKey;
        if (hasOutput()) {
            this.selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }
    }

    /**
	 * Returns the peerAddress.
	 * @return InetSocketAddress
	 */
    public InetSocketAddress getPeerAddress() {
        return peerAddress;
    }

    /**
	 * Sets the peerAddress.
	 * @param peerAddress The peerAddress to set
	 */
    public void setPeerAddress(InetSocketAddress peerAddress) {
        this.peerAddress = peerAddress;
    }
}
