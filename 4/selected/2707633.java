package de.kapsi.net.daap.nio;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.kapsi.net.daap.DaapConnection;
import de.kapsi.net.daap.DaapRequest;
import de.kapsi.net.daap.DaapRequestProcessor;
import de.kapsi.net.daap.DaapResponse;
import de.kapsi.net.daap.DaapResponseFactory;
import de.kapsi.net.daap.DaapSession;
import de.kapsi.net.daap.DaapUtil;
import de.kapsi.net.daap.SessionId;

/**
 * A NIO based implementation of DaapConnection.
 *
 * @author  Roger Kapsi
 */
public class DaapConnectionNIO extends DaapConnection {

    private static final Log LOG = LogFactory.getLog(DaapConnectionNIO.class);

    private static final DaapResponseFactory FACTORY = new DaapResponseFactoryNIO();

    private static final DaapRequestProcessor PROCESSOR = new DaapRequestProcessor(FACTORY);

    private SocketChannel socketChannel;

    private DaapRequestReaderNIO reader;

    private ReadableByteChannel readChannel;

    private WritableByteChannel writeChannel;

    private long timer = System.currentTimeMillis();

    /** Creates a new instance of DaapConnection */
    public DaapConnectionNIO(DaapServerNIO server, SocketChannel channel) {
        super(server);
        this.socketChannel = channel;
        this.readChannel = channel;
        this.writeChannel = channel;
        reader = new DaapRequestReaderNIO(this);
    }

    /**
     * What do you do next?
     *
     * @return
     */
    public int interrestOps() {
        if (isUndef()) {
            return SelectionKey.OP_READ;
        } else if (isDaapConnection()) {
            int op = SelectionKey.OP_READ;
            if (!writer.isEmpty()) op |= SelectionKey.OP_WRITE;
            return op;
        } else {
            return SelectionKey.OP_WRITE;
        }
    }

    /**
     *
     * @return
     */
    public SocketChannel getChannel() {
        return socketChannel;
    }

    /**
     *
     * @throws IOException
     * @return
     */
    public boolean read() throws IOException {
        if (!isAudioStream()) {
            while (true) {
                if (!processRead()) {
                    break;
                }
            }
            return true;
        }
        return false;
    }

    private boolean processRead() throws IOException {
        timer = System.currentTimeMillis();
        DaapRequest request = reader.read();
        if (request == null) return false;
        if (isUndef()) {
            defineConnection(request);
        }
        DaapResponse response = PROCESSOR.process(request);
        if (LOG.isTraceEnabled()) {
            LOG.trace(request);
            LOG.trace(response);
        }
        if (response != null) {
            writer.add(response);
        }
        return true;
    }

    private void defineConnection(DaapRequest request) throws IOException {
        if (request.isSongRequest()) {
            setConnectionType(ConnectionType.AUDIO);
            SessionId sid = request.getSessionId();
            if (((DaapServerNIO) server).isSessionIdValid(sid) == false) {
                throw new IOException("Unknown Session-ID: " + sid);
            }
            DaapConnection connection = ((DaapServerNIO) server).getDaapConnection(sid);
            if (connection == null) {
                throw new IOException("No connection associated with this Session-ID: " + sid);
            }
            DaapConnection audio = ((DaapServerNIO) server).getAudioConnection(sid);
            if (audio != null) {
                throw new IOException("Multiple audio connections not allowed: " + sid);
            }
            setProtocolVersion(connection.getProtocolVersion());
        } else if (request.isServerInfoRequest()) {
            setConnectionType(ConnectionType.DAAP);
            setProtocolVersion(DaapUtil.getProtocolVersion(request));
        } else {
            throw new IOException("Illegal first request: " + request);
        }
        if (!DaapUtil.isSupportedProtocolVersion(getProtocolVersion())) {
            throw new IOException("Unsupported Protocol Version: " + getProtocolVersion());
        }
        if (!((DaapServerNIO) server).updateConnection(this)) {
            throw new IOException("Too may connections");
        }
    }

    /**
     * Returns true if Connection type is undef or daap and timeout is exceeded.
     */
    boolean timeout() {
        return (isUndef() && System.currentTimeMillis() - timer >= TIMEOUT) || (isDaapConnection() && System.currentTimeMillis() - timer >= LIBRARY_TIMEOUT);
    }

    public void clearLibraryQueue() {
        super.clearLibraryQueue();
        timer = System.currentTimeMillis();
    }

    /**
     * 
     * @throws IOException
     */
    public void update() throws IOException {
        if (isDaapConnection() && !isLocked()) {
            DaapSession session = getSession(false);
            if (session != null) {
                SessionId sessionId = session.getSessionId();
                Integer delta = (Integer) session.getAttribute("CLIENT_REVISION");
                Integer revisionNumber = new Integer(getFirstInQueue().getRevision());
                DaapRequest request = new DaapRequest(this, sessionId, revisionNumber.intValue(), delta.intValue());
                DaapResponse response = PROCESSOR.process(request);
                if (response != null) {
                    writer.add(response);
                }
            }
        }
    }

    public void close() {
        super.close();
        reader = null;
    }

    public String toString() {
        return socketChannel.toString();
    }

    public ReadableByteChannel getReadChannel() {
        return readChannel;
    }

    public void setReadChannel(ReadableByteChannel readChannel) {
        this.readChannel = readChannel;
    }

    public WritableByteChannel getWriteChannel() {
        return writeChannel;
    }

    public void setWriteChannel(WritableByteChannel writeChannel) {
        this.writeChannel = writeChannel;
    }
}
