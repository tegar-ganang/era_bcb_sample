package rabbit.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import rabbit.http.HTTPHeader;

/** A class to handle automatic writing to several streams simultanius.
 */
public class MultiOutputStream extends OutputStream {

    /** how should exceptions be handled?
     *  by closing the stream causing it
     */
    public static final int CLOSE_CURRENT = 1;

    /** Or by respawning the exception up 
     */
    public static final int RESPAWN_EXCEPTION = 2;

    private int mode = CLOSE_CURRENT;

    private List<OutputStream> streams = new ArrayList<OutputStream>();

    /** Creates a new MultiOutputStream with no 
     *  connected OutputStreams 
     */
    public MultiOutputStream() {
    }

    /** Creates a new MultiOutputStream with one connected OutputStream
     * @param os the OutputStream to connect to
     */
    public MultiOutputStream(OutputStream os) {
        addOutputStream(os);
    }

    /** Connects one more OutputStream.
     * @param os the new stream to connect
     */
    public void addOutputStream(OutputStream os) {
        streams.add(os);
    }

    /** Disconnects one of the underlaying streams.
     * @param os the stream to disconnect.
     */
    public void removeOutputStream(OutputStream os) {
        streams.remove(os);
    }

    /** Check if a stream is still being written to. 
     * @param os the stream to check for.
     */
    public boolean containsStream(OutputStream os) {
        return streams.indexOf(os) != -1;
    }

    /** Sets the mode of this stream.
     * @param i one of CLOSE_CURRENT(normal) and RESPAWN_EXCEPTION.
     */
    public void setMode(int i) {
        mode = i;
    }

    /** Write a byte to this stream.
     * @param b the byte to write.
     * @throws IOException if the underlying stream does. 
     */
    public void write(int b) throws IOException {
        int ssize = streams.size();
        for (int i = 0; i < ssize; i++) {
            OutputStream os = streams.get(i);
            try {
                os.write(b);
            } catch (IOException e) {
                switch(mode) {
                    case RESPAWN_EXCEPTION:
                        throw e;
                    default:
                        removeOutputStream(os);
                        ssize--;
                }
            }
        }
    }

    /** Write a byte array to this stream.
     * @param b the byte array to write.
     * @throws IOException if the underlying stream does. 
     */
    public void write(byte[] b) throws IOException {
        int ssize = streams.size();
        for (int i = 0; i < ssize; i++) {
            OutputStream os = streams.get(i);
            try {
                os.write(b);
            } catch (IOException e) {
                switch(mode) {
                    case RESPAWN_EXCEPTION:
                        throw e;
                    default:
                        removeOutputStream(os);
                        ssize--;
                }
            }
        }
    }

    /** Write a byte array to this stream.
     * @param b the byte to write.
     * @param off the starting offset.
     * @param len the number of bytes to write.
     * @throws IOException if the underlying stream does. 
     */
    public void write(byte[] b, int off, int len) throws IOException {
        int ssize = streams.size();
        for (int i = 0; i < ssize; i++) {
            OutputStream os = streams.get(i);
            try {
                os.write(b, off, len);
            } catch (IOException e) {
                switch(mode) {
                    case RESPAWN_EXCEPTION:
                        throw e;
                    default:
                        removeOutputStream(os);
                        ssize--;
                }
            }
        }
    }

    /** Write a HTTPHeader on this stream.
     *  This is the same as <code>writeHTTPHeader (header, false, null)</code>
     * @param header the HTTPHeader to write.
     * @throws IOException if the header could not be written correctly.
     */
    public void writeHTTPHeader(HTTPHeader header) throws IOException {
        writeHTTPHeader(header, false, null);
    }

    /** Write a HTTPHeader on this stream.
     * @param header the HTTPHeader to write.
     * @param proxyConnected true if this connection is connected to another proxy.
     * @param proxyAuth the proxy authentication token to use. 
     * @throws IOException if the header could not be written correctly.
     */
    public void writeHTTPHeader(HTTPHeader header, boolean proxyConnected, String proxyAuth) throws IOException {
        byte[] b = null;
        int ssize = streams.size();
        for (int i = 0; i < ssize; i++) {
            OutputStream os = streams.get(i);
            try {
                if (os instanceof HTTPOutputStream) {
                    HTTPOutputStream hos = (HTTPOutputStream) os;
                    hos.writeHTTPHeader(header, proxyConnected, proxyAuth);
                } else {
                    if (b == null) b = header.toString().getBytes();
                    os.write(b);
                }
            } catch (IOException e) {
                switch(mode) {
                    case RESPAWN_EXCEPTION:
                        throw e;
                    default:
                        removeOutputStream(os);
                        ssize--;
                }
            }
        }
    }

    /** Write any pending data.
     * @throws IOException if the underlying stream does. 
     */
    public void flush() throws IOException {
        int ssize = streams.size();
        for (int i = 0; i < ssize; i++) {
            OutputStream os = streams.get(i);
            try {
                os.flush();
            } catch (IOException e) {
                switch(mode) {
                    case RESPAWN_EXCEPTION:
                        throw e;
                    default:
                        removeOutputStream(os);
                        ssize--;
                }
            }
        }
    }

    /** Close this stream.
     * @throws IOException if the underlying stream does. 
     */
    public void close() throws IOException {
        int ssize = streams.size();
        for (int i = 0; i < ssize; i++) {
            OutputStream os = streams.get(i);
            try {
                os.close();
            } catch (IOException e) {
                switch(mode) {
                    case RESPAWN_EXCEPTION:
                        throw e;
                    default:
                        removeOutputStream(os);
                        ssize--;
                }
            }
        }
    }

    /** Get a writable channel for this stream.
     * @return a channel or null if channel support is not available.
     */
    public WritableByteChannel getChannel() {
        if (streams == null || streams.size() != 1) return null;
        OutputStream os = streams.get(0);
        if (os instanceof FileOutputStream) return ((FileOutputStream) os).getChannel();
        if (os instanceof MultiOutputStream) return ((MultiOutputStream) os).getChannel();
        if (os instanceof HTTPOutputStream) return ((HTTPOutputStream) os).getChannel();
        if (os instanceof MaxSizeOutputStream) return null;
        System.err.println("*** Unknown outputstream type: " + os.getClass().getName());
        return null;
    }
}
