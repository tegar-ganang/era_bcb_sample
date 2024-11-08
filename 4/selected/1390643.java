package net.sf.babble.plugins.dcc;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import net.sf.babble.Connection;
import net.sf.babble.ReplyCode;
import net.sf.babble.plugins.dcc.events.FileTransferCompletedEvent;
import net.sf.babble.plugins.dcc.events.FileTransferInterruptedEvent;
import net.sf.babble.plugins.dcc.events.FileTransferProgressEvent;
import net.sf.babble.plugins.dcc.events.FileTransferStartedEvent;
import net.sf.babble.plugins.dcc.events.FileTransferTimeoutEvent;

/**
 *
 * @author  speakmon
 */
public final class DccFileSession {

    /** Holds value of property turboMode. */
    private boolean turboMode;

    /** Holds value of property listenInetAddress. */
    private InetAddress listenInetAddress;

    /** Holds value of property ID. */
    private String ID;

    /** Holds value of property userInfo. */
    private DccUserInfo userInfo;

    /** Holds value of property bufferSize. */
    private int bufferSize;

    /** Holds value of property file. */
    private DccFileInfo file;

    /** Holds value of property listenPort. */
    private int listenPort;

    private boolean waitingOnAccept;

    private ServerSocketChannel ssChannel;

    private Socket socket;

    private SocketChannel socketChannel;

    /** Holds value of property lastActivity. */
    private long lastActivity;

    private final DccEventManager eventManager = DccEventManager.getInstance();

    /** Creates a new instance of DccChanneledFileSession */
    private DccFileSession(DccUserInfo userInfo, DccFileInfo file, int bufferSize, int listenPort, String sessionId) {
        this.userInfo = userInfo;
        this.file = file;
        this.bufferSize = bufferSize;
        this.listenPort = listenPort;
        ID = sessionId;
    }

    public void interrupted() {
        cleanup();
        FileTransferInterruptedEvent event = new FileTransferInterruptedEvent(this);
        eventManager.fireFileTransferInterruptedEventHandlers(event);
    }

    public synchronized void stop() {
        cleanup();
        FileTransferInterruptedEvent event = new FileTransferInterruptedEvent(this);
        eventManager.fireFileTransferInterruptedEventHandlers(event);
    }

    public static void get(Connection connection, String nick, String fileName, boolean turbo) {
        StringBuilder buf = new StringBuilder(512);
        buf.append("PRIVMSG ");
        buf.append(nick);
        buf.append(" :DCC GET ");
        buf.append(fileName);
        buf.append(turbo ? " T" : "");
        buf.append("\n");
        connection.getSender().raw(buf.toString());
    }

    public static DccFileSession send(DccUserInfo userInfo, String listenInetAddress, int listenPort, DccFileInfo file, int bufferSize, boolean turboMode) {
        DccFileSession session = null;
        DccFileSessionManager sessionManager = DccFileSessionManager.getDefaultInstance();
        if (sessionManager.containsSession("S" + listenPort)) {
            throw new IllegalArgumentException("Already listening on port " + listenPort);
        }
        try {
            session = new DccFileSession(userInfo, file, bufferSize, listenPort, "S" + listenPort);
            session.setTurboMode(turboMode);
            session.setListenInetAddress(InetAddress.getByName(listenInetAddress));
            sessionManager.addSession(session);
            file.openForRead();
            session.startListener();
            session.dccSend();
            file.gotoReadPosition();
            session.signalTransferStart();
            if (turboMode) {
                session.startUpload(false);
            } else {
                session.startUpload(true);
            }
            return session;
        } catch (Exception e) {
            if (session != null) {
                sessionManager.removeSession(session);
            }
            return null;
        }
    }

    private void signalTransferStart() {
        resetActivityTimer();
        FileTransferStartedEvent event = new FileTransferStartedEvent(this);
        eventManager.fireFileTransferStartedEventHandlers(event);
    }

    private void startListener() {
        try {
            InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), listenPort);
            ssChannel = ServerSocketChannel.open();
            ssChannel.socket().bind(socketAddress);
        } catch (Exception e) {
            interrupted();
        }
    }

    public static DccFileSession receive(DccUserInfo userInfo, String listenInetAddress, int listenPort, DccFileInfo file, int bufferSize, boolean turbo) throws Exception {
        DccFileSessionManager sessionManager = DccFileSessionManager.getDefaultInstance();
        if (sessionManager.containsSession("C" + userInfo.getPort())) {
            throw new IllegalArgumentException("already listening on port " + userInfo.getPort());
        }
        DccFileSession session = null;
        try {
            session = new DccFileSession(userInfo, file, (64 * 1024), userInfo.getPort(), "C" + userInfo.getPort());
            session.turboMode = turbo;
            session.setListenInetAddress(InetAddress.getByName(listenInetAddress));
            file.openForWrite();
            sessionManager.addSession(session);
            if (session.file.shouldResume()) {
                session.waitingOnAccept = true;
                session.file.setResumeToFileSize();
                session.sendResume();
            } else {
                session.startDownload();
            }
            return session;
        } catch (Exception e) {
            if (session != null) {
                sessionManager.removeSession(session);
            }
            throw e;
        }
    }

    private void sendAccept() {
        StringBuilder buffer = new StringBuilder(512);
        buffer.append("PRIVMSG ");
        buffer.append(userInfo.getNick());
        buffer.append(" :DCC ACCEPT ");
        buffer.append(file.getName());
        buffer.append(" ");
        buffer.append(listenPort);
        buffer.append(" ");
        buffer.append(file.getFileStartingPosition());
        buffer.append("\n");
        userInfo.getConnection().getSender().raw(buffer.toString());
    }

    private void sendResume() {
        StringBuilder buffer = new StringBuilder(512);
        buffer.append("PRIVMSG ");
        buffer.append(userInfo.getNick());
        buffer.append(" :DCC RESUME ");
        buffer.append(file.getName());
        buffer.append(" ");
        buffer.append(listenPort);
        buffer.append(" ");
        buffer.append(file.getFileStartingPosition());
        buffer.append("\n");
        userInfo.getConnection().getSender().raw(buffer.toString());
    }

    private void dccSend() {
        StringBuilder buffer = new StringBuilder(512);
        buffer.append("PRIVMSG");
        buffer.append(userInfo.getNick());
        buffer.append(" :DCC SEND ");
        buffer.append(file.getName());
        buffer.append(" ");
        buffer.append(DccUtil.inetAddressToLong(getListenInetAddress()));
        buffer.append(" ");
        buffer.append(listenPort);
        buffer.append(" ");
        buffer.append(file.getCompleteFileSize());
        buffer.append(turboMode ? " T" : "");
        buffer.append("\n");
        userInfo.getConnection().getSender().raw(buffer.toString());
    }

    private void startDownload() {
        try {
            socketChannel = SocketChannel.open();
            FileChannel fileChannel = file.getChannel();
            socketChannel.connect(new InetSocketAddress(listenInetAddress, listenPort));
            ByteBuffer fileBuffer = file.getFileByteBuffer();
            ByteBuffer ackBuffer = ByteBuffer.allocate(8);
            int bytesRead = 0;
            while (!file.allBytesTransferred()) {
                bytesRead = socketChannel.read(fileBuffer);
                if (bytesRead == 0) {
                    interrupted();
                    return;
                }
                fileBuffer.flip();
                fileChannel.write(fileBuffer);
                if (fileBuffer.hasRemaining()) {
                    fileBuffer.compact();
                } else {
                    fileBuffer.clear();
                }
                resetActivityTimer();
                addBytesProcessed(bytesRead);
                if (!turboMode) {
                    ackBuffer.putLong(file.currentFilePosition());
                    ackBuffer.flip();
                    socketChannel.write(ackBuffer);
                }
            }
            finished();
        } catch (Exception e) {
            if (e.getMessage().indexOf("refused") > 0) {
                userInfo.getConnection().getListener().error(ReplyCode.BABBLE_DCC_CONNECTION_REFUSED, "Connection refused by remote user.");
            } else {
                userInfo.getConnection().getListener().error(ReplyCode.BABBLE_CONNECTION_FAILED, "Unknown socket error:" + e.getMessage());
            }
            interrupted();
        }
    }

    private void startUpload(boolean turboMode) {
        try {
            ByteBuffer buffer = file.getFileByteBuffer();
            ByteBuffer ackBuffer = ByteBuffer.allocate(4);
            if (!turboMode) {
            }
            FileChannel fileChannel = file.getChannel();
            socketChannel = ssChannel.accept();
            if (socketChannel.isConnectionPending()) {
                socketChannel.finishConnect();
            }
            fileChannel.force(false);
            int bytesRead = 0;
            while ((bytesRead = fileChannel.read(buffer)) > 0) {
                buffer.flip();
                int bytesWritten = socketChannel.write(buffer);
                if (buffer.hasRemaining()) {
                    buffer.compact();
                } else {
                    buffer.clear();
                }
                if (!turboMode) {
                    socketChannel.read(ackBuffer);
                }
                resetActivityTimer();
                addBytesProcessed(bytesRead);
            }
            if (!turboMode) {
                ackBuffer.flip();
                byte[] bytes = new byte[4];
                ackBuffer.get(bytes);
                while (!file.acksFinished(DccUtil.dccBytesToLong(bytes))) {
                    socketChannel.read(ackBuffer);
                }
            }
            finished();
        } catch (Exception e) {
            interrupted();
        }
    }

    private void resetActivityTimer() {
        lastActivity = System.currentTimeMillis();
    }

    private void addBytesProcessed(int bytesRead) {
        file.addBytesTransferred(bytesRead);
        FileTransferProgressEvent event = new FileTransferProgressEvent(this);
        event.setBytesSent(bytesRead);
    }

    private void finished() {
        cleanup();
        FileTransferCompletedEvent event = new FileTransferCompletedEvent(this);
        eventManager.fireFileTransferCompletedEventHandlers(event);
    }

    protected void timedOut() {
        if (waitingOnAccept) {
            waitingOnAccept = false;
            startDownload();
        } else {
            FileTransferTimeoutEvent event = new FileTransferTimeoutEvent(this);
            eventManager.fireFileTransferTimeoutEventHandlers(event);
            cleanup();
        }
    }

    private void cleanup() {
        DccFileSessionManager.getDefaultInstance().removeSession(this);
        try {
            if (ssChannel != null) {
                ssChannel.close();
            }
            if (socketChannel != null) {
                socketChannel.close();
            }
            if (socket != null) {
                socket.close();
            }
            file.close();
        } catch (IOException ioe) {
        }
    }

    protected synchronized void onDccAcceptReceived(long position) throws IOException {
        if (!waitingOnAccept) {
            return;
        }
        waitingOnAccept = false;
        if (!file.acceptPositionMatches(position)) {
            userInfo.getConnection().getListener().error(ReplyCode.BABBLE_BAD_DCC_ACCEPT_VALUE, "asked to start at " + file.getFileStartingPosition() + " but was sent " + position);
            interrupted();
            return;
        }
        resetActivityTimer();
        file.setResumeToFileSize();
        file.gotoWritePosition();
    }

    protected synchronized void onDccResumeRequest(long resumePosition) {
        resetActivityTimer();
        if (file.getBytesTransferred() == 0) {
            if (file.isResumePositionValid(resumePosition)) {
                file.setResumePosition(resumePosition);
                sendAccept();
            } else {
                userInfo.getConnection().getListener().error(ReplyCode.BABBLE_BAD_RESUME_POSITION, toString() + " sent an invalid resume position.");
                cleanup();
            }
        }
    }

    /** Getter for property turboMode.
     * @return Value of property turboMode.
     *
     */
    public boolean isTurboMode() {
        return this.turboMode;
    }

    /** Setter for property turboMode.
     * @param turboMode New value of property turboMode.
     *
     */
    private void setTurboMode(boolean turboMode) {
        this.turboMode = turboMode;
    }

    /** Getter for property userInfo.
     * @return Value of property userInfo.
     *
     */
    public DccUserInfo getUserInfo() {
        return this.userInfo;
    }

    /** Setter for property userInfo.
     * @param userInfo New value of property userInfo.
     *
     */
    public void setUserInfo(DccUserInfo userInfo) {
        this.userInfo = userInfo;
    }

    /** Getter for property bufferSize.
     * @return Value of property bufferSize.
     *
     */
    public int getBufferSize() {
        return this.bufferSize;
    }

    /** Getter for property file.
     * @return Value of property file.
     *
     */
    public DccFileInfo getFile() {
        return this.file;
    }

    /** Getter for property listenPort.
     * @return Value of property listenPort.
     *
     */
    public int getListenPort() {
        return this.listenPort;
    }

    /** Getter for property listenInetAddress.
     * @return Value of property listenInetAddress.
     *
     */
    public InetAddress getListenInetAddress() {
        return this.listenInetAddress;
    }

    /** Setter for property listenInetAddress.
     * @param listenInetAddress New value of property listenInetAddress.
     *
     */
    private void setListenInetAddress(InetAddress listenInetAddress) {
        this.listenInetAddress = listenInetAddress;
    }

    /** Getter for property ID.
     * @return Value of property ID.
     *
     */
    public String getID() {
        return this.ID;
    }

    /** Setter for property ID.
     * @param ID New value of property ID.
     *
     */
    public void setID(String ID) {
        this.ID = ID;
    }

    /** Getter for property lastActivity.
     * @return Value of property lastActivity.
     *
     */
    public long getLastActivity() {
        return this.lastActivity;
    }

    /** Setter for property lastActivity.
     * @param lastActivity New value of property lastActivity.
     *
     */
    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }
}
