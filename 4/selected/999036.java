package org.opengts.util;

import java.net.*;
import javax.net.ssl.*;
import javax.net.*;

public abstract class AbstractClientPacketHandler implements ClientPacketHandler {

    public static final int PACKET_LEN_ASCII_LINE_TERMINATOR = ServerSocketThread.PACKET_LEN_ASCII_LINE_TERMINATOR;

    public static final int PACKET_LEN_END_OF_STREAM = ServerSocketThread.PACKET_LEN_END_OF_STREAM;

    private InetAddress inetAddr = null;

    private boolean isTCP = true;

    private boolean isTextPackets = false;

    protected boolean isTextPackets() {
        return this.isTextPackets;
    }

    private ServerSocketThread.SessionInfo sessionInfo = null;

    public void setSessionInfo(ServerSocketThread.SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    public ServerSocketThread.SessionInfo getSessionInfo() {
        return this.sessionInfo;
    }

    public int getLocalPort() {
        return (this.sessionInfo != null) ? this.sessionInfo.getLocalPort() : -1;
    }

    public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText) {
        this.inetAddr = inetAddr;
        this.isTCP = isTCP;
        this.isTextPackets = isText;
    }

    public byte[] getInitialPacket() throws Exception {
        return null;
    }

    public byte[] getFinalPacket(boolean hasError) throws Exception {
        return null;
    }

    public InetAddress getInetAddress() {
        return this.inetAddr;
    }

    public String getHostAddress() {
        String ipAddr = (this.inetAddr != null) ? this.inetAddr.getHostAddress() : null;
        return ipAddr;
    }

    public int getResponsePort() {
        return 0;
    }

    public int getActualPacketLength(byte packet[], int packetLen) {
        return this.isTextPackets ? PACKET_LEN_ASCII_LINE_TERMINATOR : packetLen;
    }

    public abstract byte[] getHandlePacket(byte cmd[]) throws Exception;

    public boolean terminateSession() {
        return true;
    }

    public void sessionTerminated(Throwable err, long readCount, long writeCount) {
    }
}
