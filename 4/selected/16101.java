package org.jwebsocket.tcp;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import org.apache.log4j.Logger;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.api.WebSocketEngine;
import org.jwebsocket.api.WebSocketPacket;
import org.jwebsocket.async.IOFuture;
import org.jwebsocket.config.JWebSocketCommonConstants;
import org.jwebsocket.connectors.BaseConnector;
import org.jwebsocket.kit.CloseReason;
import org.jwebsocket.kit.RawPacket;
import org.jwebsocket.kit.WebSocketProtocolHandler;
import org.jwebsocket.logging.Logging;

/**
 * Implementation of the jWebSocket TCP socket connector.
 *
 * @author aschulze
 * @author jang
 */
public class TCPConnector extends BaseConnector {

    private static Logger mLog = Logging.getLogger(TCPConnector.class);

    private InputStream mIn = null;

    private OutputStream mOut = null;

    private Socket mClientSocket = null;

    private boolean mIsRunning = false;

    private CloseReason mCloseReason = CloseReason.TIMEOUT;

    /**
	 * creates a new TCP connector for the passed engine using the passed client
	 * socket. Usually connectors are instantiated by their engine only, not by
	 * the application.
	 *
	 * @param aEngine
	 * @param aClientSocket
	 */
    public TCPConnector(WebSocketEngine aEngine, Socket aClientSocket) {
        super(aEngine);
        mClientSocket = aClientSocket;
        try {
            mIn = mClientSocket.getInputStream();
            mOut = new PrintStream(mClientSocket.getOutputStream(), true, "UTF-8");
        } catch (Exception lEx) {
            mLog.error(lEx.getClass().getSimpleName() + " instantiating " + getClass().getSimpleName() + ": " + lEx.getMessage());
        }
    }

    @Override
    public void startConnector() {
        int lPort = -1;
        int lTimeout = -1;
        try {
            lPort = mClientSocket.getPort();
            lTimeout = mClientSocket.getSoTimeout();
        } catch (Exception lEx) {
        }
        if (mLog.isDebugEnabled()) {
            mLog.debug("Starting TCP connector on port " + lPort + " with timeout " + (lTimeout > 0 ? lTimeout + "ms" : "infinite") + "");
        }
        ClientProcessor lClientProc = new ClientProcessor(this);
        Thread lClientThread = new Thread(lClientProc);
        lClientThread.start();
        if (mLog.isInfoEnabled()) {
            mLog.info("Started TCP connector on port " + lPort + " with timeout " + (lTimeout > 0 ? lTimeout + "ms" : "infinite") + "");
        }
    }

    @Override
    public void stopConnector(CloseReason aCloseReason) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Stopping TCP connector (" + aCloseReason.name() + ")...");
        }
        int lPort = mClientSocket.getPort();
        mCloseReason = aCloseReason;
        mIsRunning = false;
        if (!isHixieDraft()) {
            WebSocketPacket lClose = new RawPacket("BYE");
            lClose.setFrameType(RawPacket.FRAMETYPE_CLOSE);
            sendPacket(lClose);
        }
        try {
            mIn.close();
            if (mLog.isInfoEnabled()) {
                mLog.info("Stopped TCP connector (" + aCloseReason.name() + ") on port " + lPort + ".");
            }
        } catch (IOException lEx) {
            if (mLog.isDebugEnabled()) {
                mLog.info(lEx.getClass().getSimpleName() + " while stopping TCP connector (" + aCloseReason.name() + ") on port " + lPort + ": " + lEx.getMessage());
            }
        }
    }

    @Override
    public void processPacket(WebSocketPacket aDataPacket) {
        getEngine().processPacket(this, aDataPacket);
    }

    @Override
    public synchronized void sendPacket(WebSocketPacket aDataPacket) {
        try {
            if (isHixieDraft()) {
                sendHixie(aDataPacket);
            } else {
                sendHybi(aDataPacket);
            }
            mOut.flush();
        } catch (IOException lEx) {
            mLog.error(lEx.getClass().getSimpleName() + " sending data packet: " + lEx.getMessage());
        }
    }

    @Override
    public IOFuture sendPacketAsync(WebSocketPacket aDataPacket) {
        throw new UnsupportedOperationException("Underlying connector:" + getClass().getName() + " doesn't support asynchronous send operation");
    }

    private class ClientProcessor implements Runnable {

        private WebSocketConnector mConnector = null;

        /**
		 * Creates the new socket listener thread for this connector.
		 *
		 * @param aConnector
		 */
        public ClientProcessor(WebSocketConnector aConnector) {
            mConnector = aConnector;
        }

        @Override
        public void run() {
            WebSocketEngine lEngine = getEngine();
            ByteArrayOutputStream lBuff = new ByteArrayOutputStream();
            try {
                mIsRunning = true;
                lEngine.connectorStarted(mConnector);
                if (isHixieDraft()) {
                    readHixie(lBuff, lEngine);
                } else {
                    readHybi(lBuff, lEngine);
                }
                lEngine.connectorStopped(mConnector, mCloseReason);
                mIn.close();
                mOut.close();
                mClientSocket.close();
            } catch (Exception lEx) {
                mLog.error("(close) " + lEx.getClass().getSimpleName() + ": " + lEx.getMessage());
            }
        }

        private void readHixie(ByteArrayOutputStream aBuff, WebSocketEngine aEngine) throws IOException {
            while (mIsRunning) {
                try {
                    int lIn = mIn.read();
                    if (lIn == 0x00) {
                        aBuff.reset();
                    } else if (lIn == 0xFF) {
                        RawPacket lPacket = new RawPacket(aBuff.toByteArray());
                        try {
                            aEngine.processPacket(mConnector, lPacket);
                        } catch (Exception lEx) {
                            mLog.error(lEx.getClass().getSimpleName() + " in processPacket of connector " + mConnector.getClass().getSimpleName() + ": " + lEx.getMessage());
                        }
                        aBuff.reset();
                    } else if (lIn < 0) {
                        mCloseReason = CloseReason.CLIENT;
                        mIsRunning = false;
                    } else {
                        aBuff.write(lIn);
                    }
                } catch (SocketTimeoutException lEx) {
                    mLog.error("(timeout) " + lEx.getClass().getSimpleName() + ": " + lEx.getMessage());
                    mCloseReason = CloseReason.TIMEOUT;
                    mIsRunning = false;
                } catch (Exception lEx) {
                    mLog.error("(other) " + lEx.getClass().getSimpleName() + ": " + lEx.getMessage());
                    mCloseReason = CloseReason.SERVER;
                    mIsRunning = false;
                }
            }
        }

        /**
		 *  One message may consist of one or more (fragmented message) protocol packets.
		 *  The spec is currently unclear whether control packets (ping, pong, close) may
		 *  be intermingled with fragmented packets of another message. For now I've
		 *  decided to not implement such packets 'swapping', and therefore reading fails
		 *  miserably if a client sends control packets during fragmented message read.
		 *  TODO: follow next spec drafts and add support for control packets inside fragmented message if needed.
		 *  <p>
		 *  Structure of packets conforms to the following scheme (copied from spec):
		 *  </p>
		 *  <pre>
		 *  0                   1                   2                   3
		 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
		 * +-+-+-+-+-------+-+-------------+-------------------------------+
		 * |M|R|R|R| opcode|R| Payload len |    Extended payload length    |
		 * |O|S|S|S|  (4)  |S|     (7)     |             (16/63)           |
		 * |R|V|V|V|       |V|             |   (if payload len==126/127)   |
		 * |E|1|2|3|       |4|             |                               |
		 * +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
		 * |     Extended payload length continued, if payload len == 127  |
		 * + - - - - - - - - - - - - - - - +-------------------------------+
		 * |                               |         Extension data        |
		 * +-------------------------------+ - - - - - - - - - - - - - - - +
		 * :                                                               :
		 * +---------------------------------------------------------------+
		 * :                       Application data                        :
		 * +---------------------------------------------------------------+
		 * </pre>
		 * RSVx bits are ignored (reserved for future use).
		 * TODO: add support for extension data, when extensions will be defined in the specs.
		 *
		 * <p>
		 * Read section 4.2 of the spec for detailed explanation.
		 * </p>
		 */
        private void readHybi(ByteArrayOutputStream aBuff, WebSocketEngine aEngine) throws IOException {
            int lPacketType;
            DataInputStream lDis = new DataInputStream(mIn);
            while (mIsRunning) {
                try {
                    int lFlags = lDis.read();
                    boolean lFragmented = (0x01 & lFlags) == 0x01;
                    int lType = lFlags >> 4;
                    lPacketType = WebSocketProtocolHandler.toRawPacketType(lType);
                    if (lPacketType == -1) {
                        mLog.trace("Dropping packet with unknown type: " + lType);
                    } else {
                        long lPayloadLen = mIn.read() >> 1;
                        if (lPayloadLen == 126) {
                            lPayloadLen = lDis.readUnsignedShort();
                        } else if (lPayloadLen == 127) {
                            lPayloadLen = lDis.readLong();
                        }
                        if (lPayloadLen > 0) {
                            while (lPayloadLen-- > 0) {
                                aBuff.write(lDis.read());
                            }
                        }
                        if (!lFragmented) {
                            if (lPacketType == RawPacket.FRAMETYPE_PING) {
                                WebSocketPacket lPong = new RawPacket(aBuff.toByteArray());
                                lPong.setFrameType(RawPacket.FRAMETYPE_PONG);
                                sendPacket(lPong);
                            } else if (lPacketType == RawPacket.FRAMETYPE_CLOSE) {
                                mCloseReason = CloseReason.CLIENT;
                                mIsRunning = false;
                                WebSocketPacket lClose = new RawPacket(aBuff.toByteArray());
                                lClose.setFrameType(RawPacket.FRAMETYPE_CLOSE);
                                sendPacket(lClose);
                            }
                            WebSocketPacket lPacket = new RawPacket(aBuff.toByteArray());
                            lPacket.setFrameType(lPacketType);
                            try {
                                aEngine.processPacket(mConnector, lPacket);
                            } catch (Exception lEx) {
                                mLog.error(lEx.getClass().getSimpleName() + " in processPacket of connector " + mConnector.getClass().getSimpleName() + ": " + lEx.getMessage());
                            }
                            aBuff.reset();
                        }
                    }
                } catch (SocketTimeoutException lEx) {
                    mLog.error("(timeout) " + lEx.getClass().getSimpleName() + ": " + lEx.getMessage());
                    mCloseReason = CloseReason.TIMEOUT;
                    mIsRunning = false;
                } catch (Exception lEx) {
                    mLog.error("(other) " + lEx.getClass().getSimpleName() + ": " + lEx.getMessage());
                    mCloseReason = CloseReason.SERVER;
                    mIsRunning = false;
                }
            }
        }
    }

    @Override
    public String generateUID() {
        String lUID = mClientSocket.getInetAddress().getHostAddress() + "@" + mClientSocket.getPort();
        return lUID;
    }

    @Override
    public int getRemotePort() {
        return mClientSocket.getPort();
    }

    @Override
    public InetAddress getRemoteHost() {
        return mClientSocket.getInetAddress();
    }

    @Override
    public String toString() {
        String lRes = getId() + " (" + getRemoteHost().getHostAddress() + ":" + getRemotePort();
        String lUsername = getUsername();
        if (lUsername != null) {
            lRes += ", " + lUsername;
        }
        return lRes + ")";
    }

    private void sendHixie(WebSocketPacket aDataPacket) throws IOException {
        if (aDataPacket.getFrameType() == RawPacket.FRAMETYPE_BINARY) {
            mOut.write(0xFF);
            byte[] lBA = aDataPacket.getByteArray();
            mOut.write(lBA.length);
            mOut.write(lBA);
        } else {
            mOut.write(0x00);
            mOut.write(aDataPacket.getByteArray());
            mOut.write(0xFF);
        }
    }

    private void sendHybi(WebSocketPacket aDataPacket) throws IOException {
        byte[] lPacket = WebSocketProtocolHandler.toProtocolPacket(aDataPacket);
        mOut.write(lPacket);
    }

    private boolean isHixieDraft() {
        return JWebSocketCommonConstants.WS_DRAFT_DEFAULT.equals(getHeader().getDraft());
    }
}
