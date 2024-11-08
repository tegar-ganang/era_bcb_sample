package com.bitgate.util.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import com.bitgate.util.debug.Debug;
import com.bitgate.util.memory.Objects;

/**
 * This class creates a wrapper around a blocking and non-blocking socket, so that data (secure or otherwise) can be sent
 * to the connected client without having to know the type of connection that is active.  This class is here to provide
 * a pseudo non-blocking interface to SSL client connections, as Java 1.4.x does not handle SSL in a non-blocking way.
 *
 * @author Kenji Hollis &lt;kenji@nuklees.com&gt;
 * @version $Id: //depot/nuklees/util/socket/SocketWrapper.java#13 $
 */
public class SocketWrapper implements ByteChannel {

    private Socket s;

    private InputStream is;

    private OutputStream os;

    private ReadableByteChannel rbc;

    private WritableByteChannel wbc;

    /**
     * Constructor - wraps the specified socket.
     *
     * @param socket The socket to wrap.
     * @throws IOException on any errors.
     */
    public SocketWrapper(Socket socket) throws IOException {
        this.s = socket;
        this.is = this.s.getInputStream();
        this.os = this.s.getOutputStream();
        this.rbc = Channels.newChannel(this.is);
        this.wbc = Channels.newChannel(this.os);
    }

    /**
     * Reads data into a destination <code>ByteBuffer</code> object.
     *
     * @param dst The destination to read into.
     * @return <code>int</code> containing the result.
     */
    public int read(ByteBuffer dst) throws IOException {
        int y = checkConnection();
        if (y <= 0) {
            return y;
        }
        dst.put((byte) y);
        return rbc.read(dst);
    }

    /**
     * Checks whether or not the connection is active.  For non-blocking connections, this method cheats a little with the
     * TCP stack.  It does so by setting the socket timeout to 25 milliseconds, and reading the result.  If the result
     * times out, no data is read, and the timeout is restored.  However, if the connection drops, it catches the IO
     * Exception, and indicates the error on the read.  Otherwise, it returns the data read from the socket.
     *
     * @return <code>int</code> containing the data read from the socket, 0 if timed out, and -1 on errors.
     * @throws IOException on any errors.
     */
    protected int checkConnection() throws IOException {
        int y = s.getSoTimeout();
        int p = -1;
        s.setSoTimeout(25);
        try {
            p = is.read();
        } catch (SocketTimeoutException e) {
            p = 0;
        } catch (IOException e) {
            p = -1;
        }
        if (p != -1) {
            s.setSoTimeout(y);
        }
        return p;
    }

    /**
     * Sends a buffer of data to the connected client.
     *
     * @param src <code>ByteBuffer</code> object to send.
     * @throws IOException on any errors.
     */
    public int write(ByteBuffer src) throws IOException {
        int x, y = s.getSendBufferSize(), z = 0;
        int expectedWrite;
        if (src.remaining() == 0) {
            Debug.debug("Returning 0 remaining.");
            return 0;
        }
        if (src.hasArray()) {
            byte p[] = src.array();
            ByteBuffer buf;
            buf = ByteBuffer.allocateDirect(y);
            os.flush();
            for (x = 0; x < p.length; x += y) {
                if (p.length - x < y) {
                    buf.put(p, x, p.length - x);
                    expectedWrite = p.length - x;
                } else {
                    buf.put(p, x, y);
                    expectedWrite = y;
                }
                if (!s.isConnected()) {
                    break;
                }
                buf.flip();
                z = wbc.write(buf);
                if (z < expectedWrite) {
                    break;
                }
                buf.clear();
            }
            try {
                Objects.free(buf);
            } catch (Exception e) {
                Debug.debug("Unable to free allocated buffer memory: " + e.getMessage());
            }
            int pLength = p.length;
            p = null;
            buf = null;
            if (x > pLength) {
                Debug.debug("Returning " + pLength + " bytes sent.");
                return pLength;
            } else if (x == 0) {
                return -1;
            } else {
                Debug.debug("Returning " + (x + z) + " bytes sent.");
                return x + z;
            }
        } else {
            int totalBytes = src.remaining();
            int bufRemaining = totalBytes;
            int bytesSent = 0;
            while (bufRemaining > 0) {
                int bWritten = wbc.write(src);
                bytesSent += bWritten;
                bufRemaining -= bWritten;
            }
            return totalBytes;
        }
    }

    /**
     * Closes the active connection.
     */
    public void close() {
        try {
            is.close();
            os.close();
            s.close();
        } catch (Exception e) {
        }
    }

    /**
     * Returns whether or not the currently connected client is still active.
     *
     * @return <code>true</code> if still connected, <code>false</code> otherwise.
     */
    public boolean isOpen() {
        return s.isConnected();
    }

    /**
     * Returns the <code>SocketChannel</code> object.
     *
     * @return <code>SocketChannel</code> object.
     */
    public SocketChannel getChannel() {
        return s.getChannel();
    }

    /**
     * Returns the currently connected <code>Socket</code> object.
     *
     * @return <code>Socket</code> object.
     */
    public Socket getSocket() {
        return s;
    }
}
