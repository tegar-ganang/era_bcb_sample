package rabbit.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.SocketException;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.StringTokenizer;
import rabbit.http.GeneralHeader;
import rabbit.http.HTTPFooter;
import rabbit.http.HTTPHeader;
import rabbit.util.Logger;

/** This is an extended DataInputStream suitable for reading data from the web. 
 *  It handles chunked encoding etc.
 *
 *  If the underlying input is chunked this class will throw away any
 *  trailing footer.
 */
public class HTTPInputStream extends DataInputStream {

    private Logger logger;

    private boolean keepalive = true;

    private boolean ischunked = false;

    private long dataSize = -1;

    private HTTPHeader header = null;

    private SocketChannel socket;

    private byte[] HTTP_RESPONSE_IDENTIFIER = { (byte) 'H', (byte) 'T', (byte) 'T', (byte) 'P', (byte) '/' };

    /** An input stream suitable for reading chunked streams 
     */
    private class Chunker extends InputStream {

        private HTTPInputStream in = null;

        private byte[] buf;

        private int idx = 0;

        private int chunksize = -1;

        /** Create a new chunk handler on the underlying stream
	 * @param in the underlying stream.
	 */
        Chunker(HTTPInputStream in) throws IOException {
            this.in = in;
            getChunk();
        }

        /** read byte.
	 * @return a byte or  (int)-1 if end of stream.
	 */
        public int read() throws IOException {
            if (buf.length == 0) return -1;
            if (buf.length <= idx) {
                getChunk();
                return read();
            }
            return (buf[idx++] & 0xff);
        }

        /** Try to read up to b.length bytes.
	 * @param b the byte array to read data into.
	 * @return the number of bytes read, -1 if end of stream.
	 */
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        /** Try to read up to len bytes.
	 * @param b the byte array to read data into.
	 * @param off the offset into b where data is stored.
	 * @param len read at most this many bytes.
	 * @return the number of bytes read, -1 if end of stream.
	 */
        public int read(byte[] b, int off, int len) throws IOException {
            if (buf.length == 0) return -1;
            if (buf.length <= idx) {
                getChunk();
                return read(b, off, len);
            }
            int left = buf.length - idx;
            int toread = Math.min(left, len);
            System.arraycopy(buf, idx, b, off, toread);
            idx += toread;
            return toread;
        }

        /** Try to skip n bytes.
	 * @param n the number of bytes to skip.
	 * @return the number of bytes actually skipped.
	 */
        public long skip(long n) throws IOException {
            if (buf.length == 0) return 0;
            if (buf.length <= idx) {
                getChunk();
                return skip(n);
            }
            int left = buf.length - idx;
            if (n < left) {
                idx += n;
                return n;
            } else {
                idx = buf.length;
                return (left + skip(n - left));
            }
        }

        /** Get the number of bytes that can be read without blocking.
	 * @return the number of bytes we can read without blocking.
	 */
        public int available() throws IOException {
            if (buf.length == 0) return 0;
            if (buf.length <= idx) return 0;
            return buf.length - idx;
        }

        /** Close this stream.
	 */
        public void close() throws IOException {
            in.close();
        }

        /** Get the next chunk.
	 */
        private void getChunk() throws IOException {
            chunksize = getChunkSize();
            if (chunksize > 0) {
                buf = new byte[chunksize];
                in.readFully(buf);
                in.readCRLF();
                idx = 0;
            } else {
                buf = new byte[0];
                idx = 0;
                new HTTPFooter(in);
            }
        }
    }

    /** Create a new HTTPInputStream on the underlying stream.
     * @param is the underlying stream
     */
    public HTTPInputStream(InputStream is, Logger logger) {
        super(new BufferedInputStream(is));
        this.logger = logger;
    }

    /** Create a new HTTPInputStream on the underlying channel
     * @param socket the socket to use.
     * @param connected false if there is a asynchronous connection pending, true 
     *                  if the socket is fully connected
     */
    public HTTPInputStream(SocketChannel socket, boolean connected, Logger logger, NLSOHandler nlsoHandler) throws IOException {
        this(new NLSOInputStream(socket, connected, nlsoHandler), logger);
        this.socket = socket;
    }

    /** Read a HTTPHeader of this stream.
     *  If data is chunked the whole page will be read in so to get it 
     *  use the getChunkStream. something like this <xmp>
     *  HTTPHeader header = readHeader ();
     *  if (chunked) {
     *      contentStream = getChunkStream ();
     *  } 
     *  </xmp>
     * @return a new HTTPHeader read from the stream.
     * @throws IOException if the HTTPHeader could not be read correctly.
     */
    public HTTPHeader readHTTPHeader() throws IOException {
        return readHTTPHeader(false);
    }

    /** Read a HTTPHeader of this stream.
     *  If data is chunked the whole page will be read in so to get it 
     *  use the getChunkStream. something like this <xmp>
     *  HTTPHeader header = readHeader ();
     *  if (chunked) {
     *      contentStream = getChunkStream ();
     *  } 
     *  </xmp>
     * @param response if true a response will be read. If the underlying 
     *  stream does not start with HTTP/ a newly created http response header will 
     *  be returned and the bytes read will be pushed back onto the stream. 
     * @return a new HTTPHeader read from the stream.
     * @throws IOException if the HTTPHeader could not be read correctly.
     */
    public HTTPHeader readHTTPHeader(boolean response) throws IOException {
        header = null;
        if (response) {
            verifyResponse();
        }
        if (header == null) header = new HTTPHeader(this);
        dataSize = -1;
        String cl = header.getHeader("Content-Length");
        if (cl != null) {
            try {
                dataSize = Long.parseLong(cl);
            } catch (NumberFormatException e) {
                dataSize = -1;
            }
        }
        String con = header.getHeader("Connection");
        String pcon = header.getHeader("Proxy-Connection");
        if (con != null && con.equalsIgnoreCase("close")) setKeepAlive(false);
        if (keepalive && pcon != null && pcon.equalsIgnoreCase("close")) setKeepAlive(false);
        if (header.isResponse()) {
            if (header.getResponseHTTPVersion().equals("HTTP/1.1")) {
                String chunked = header.getHeader("Transfer-Encoding");
                setKeepAlive(true);
                ischunked = false;
                if (chunked != null && chunked.equalsIgnoreCase("chunked")) {
                    ischunked = true;
                    header.removeHeader("Content-Length");
                    dataSize = -1;
                }
            } else {
                setKeepAlive(false);
            }
        }
        if (!(dataSize > -1 || ischunked)) setKeepAlive(false);
        return header;
    }

    /** Verify that the response starts with "HTTP/" 
     *  Failure to verify response => treat all of data as content = HTTP/0.9.
     */
    protected void verifyResponse() throws IOException {
        byte[] buf = new byte[5];
        int read = read(buf);
        if (read > 4 && buf[0] == '0' && buf[1] == '\r' && buf[2] == '\n' && buf[3] == '\r' && buf[4] == '\n') {
            logger.logError(Logger.WARN, "found a last-chunk, trying to ignore it.");
            verifyResponse();
            return;
        }
        for (int i = 0; i < 5 && i < read; i++) {
            if (buf[i] != HTTP_RESPONSE_IDENTIFIER[i]) {
                logger.logError(Logger.WARN, "http response header with odd start:" + (int) buf[i] + " != " + (int) HTTP_RESPONSE_IDENTIFIER[i]);
                header = new HTTPHeader();
                header.setStatusLine("HTTP/1.1 200 OK");
                header.setHeader("Connection", "close");
                break;
            }
        }
        ByteArrayInputStream bar = new ByteArrayInputStream(buf, 0, read);
        in = new RSequenceInputStream(bar, in);
    }

    private static class RSequenceInputStream extends SequenceInputStream {

        private ByteArrayInputStream ba;

        private InputStream in;

        public RSequenceInputStream(ByteArrayInputStream ba, InputStream in) {
            super(ba, in);
            this.ba = ba;
            this.in = in;
        }

        public ByteArrayInputStream getFirst() {
            return ba;
        }

        public InputStream getSecond() {
            return in;
        }
    }

    /** Get the keep alive value.
     * @return if this stream is in keepalive mode.
     */
    public boolean getKeepAlive() {
        return keepalive;
    }

    /** Set the keep alive value to currentkeepalive & keepalive
     * @param keepalive the new keepalive value.
     */
    public void setKeepAlive(boolean keepalive) {
        this.keepalive = (this.keepalive && keepalive);
    }

    /** Get the chunked value.
     * @return true if the last read HTTPHeader was chunked.
     */
    public boolean chunked() {
        return ischunked;
    }

    /** Set the chunkin of this stream. 
     * Useful for turning off chunking on persistent connections.
     * @param b the new chunking value.
     */
    public void setChunked(boolean b) {
        this.ischunked = b;
    }

    /** Get the size of the page being read.
     * @return the size of the page being read or -1 if unknown.
     */
    public long dataSize() {
        return dataSize;
    }

    /** Get the size of the next chunk.
     * @return the size of the next chunk.
     * @throws IOException if the chunk size could not be read correctly.
     */
    protected int getChunkSize() throws IOException {
        int chunksize = 0;
        String line = GeneralHeader.readLine(this);
        if (line == null) throw new IOException("Failed to read chunk block size");
        StringTokenizer st = new StringTokenizer(line, "\t \n\r(;");
        if (st.hasMoreTokens()) {
            String hex = st.nextToken();
            try {
                chunksize = Integer.parseInt(hex, 16);
            } catch (NumberFormatException e) {
                throw new IOException("Chunk size is not a hex number: '" + line + "', '" + hex + "'.");
            }
        } else {
            throw new IOException("Chunk size is not available.");
        }
        return chunksize;
    }

    /** Read of an CR LF combination.
     * @throws IOException if the CR LF combination could not be read correctly.
     */
    public void readCRLF() throws IOException {
        int cr = read();
        int lf = read();
        if (cr != 13 && lf != 10) throw new IOException("Could not read CRLF correctly: " + cr + ", " + lf);
    }

    /** Get the chunked page.
     * @return an InputStream to the previously read chunked pagel.
     */
    public InputStream getChunkStream() throws IOException {
        return new Chunker(this);
    }

    protected void finish() {
        header = null;
    }

    public void close() throws IOException {
        header = null;
        synchronized (this) {
            SocketChannel sc = getSocketChannel();
            if (sc != null) {
                if (sc.isOpen()) {
                    try {
                        sc.socket().shutdownInput();
                    } catch (SocketException e) {
                        logger.logError(Logger.WARN, "socket.shutDownInput: " + e);
                    }
                    try {
                        sc.socket().shutdownOutput();
                    } catch (SocketException e) {
                        logger.logError(Logger.WARN, "socket.shutDownOutput: " + e);
                    }
                    try {
                        sc.socket().close();
                    } catch (SocketException e) {
                        logger.logError(Logger.WARN, "socket.socket().close: " + e);
                    }
                }
                sc.close();
            } else {
                FileChannel fc = getFileChannel();
                if (fc != null) {
                    if (fc.isOpen()) fc.close();
                }
            }
        }
        super.close();
        dataSize = -1;
    }

    /** Try to get a file channel from this stream.
     * @return a FileChannel or null if no file channel is available.
     */
    public FileChannel getFileChannel() {
        if (ischunked) return null;
        if (in instanceof FileInputStream) return ((FileInputStream) in).getChannel();
        return null;
    }

    /** Try to get a socket channel from this stream.
     * @return a SocketChanne or null if no socket channel is available.
     */
    public SocketChannel getSocketChannel() {
        if (ischunked) return null;
        if (socket != null) return socket;
        return null;
    }
}
