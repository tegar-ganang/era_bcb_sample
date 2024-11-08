package rabbit.io;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import rabbit.http.HTTPFooter;
import rabbit.http.HTTPHeader;
import rabbit.util.Coder;

/** This is an extended DataOutputstream suitable for writing HTTP data.
 *  This class handles sending data in chunked manner, 
 *  set the &quot;Transfer-Encoding&quot; header to &quot;chunked&quot; 
 *  in the HTTPHeader before sending it to specify it.
 *  If you are sending data in chunked mode you must end it correctly.
 *  There is two ways to do that: 
 *  Use the finish method (will use an empty footer) or 
 *  Manually send a zero sized block followed by the HTTPFooter. Something like this:
 *  <xmp>
 *          // now send the file
 *	    while ((read = fis.read (buf)) > 0) {
 *		os.write (buf, 0, read);
 *	    }
 *          // finish up.
 *          if (sendingchunked) {
 *	        os.write (buf, 0, 0);
 *	        HTTPFooter footer = new HTTPFooter ();
 *	        os.writeFooter (footer);
 *          }
 *  </xmp>
 */
public class HTTPOutputStream extends DataOutputStream {

    private OutputStream is;

    private SocketChannel socket;

    private boolean ischunked;

    private byte[] onebuf = new byte[1];

    /** Create a new HTTPOutputStream on the underlying stream.
     * @param is the underlying stream.
     */
    public HTTPOutputStream(OutputStream is) {
        this(is, false);
    }

    /** Create a new HTTPOutputStream on the underlying channel.
     * @param socket the socket to use.
     */
    public HTTPOutputStream(SocketChannel socket) throws IOException {
        this(socket.socket().getOutputStream(), false);
        this.socket = socket;
    }

    /** Create a new HTTPOutputStream on the underlying stream.
     * @param is the underlying stream.
     */
    public HTTPOutputStream(OutputStream is, boolean ischunked) {
        super(is);
        this.is = is;
        this.ischunked = ischunked;
    }

    /** Check if this stream is chunking.
     * @return true if this stream is in chunk mode.
     */
    public boolean isChunking() {
        return ischunked;
    }

    /** Write a HTTPHeader on this stream.
     * @param header the HTTPHeader to write.
     * @throws IOException if the header could not be written correctly.
     */
    public void writeHTTPHeader(HTTPHeader header) throws IOException {
        writeHTTPHeader(header, false, null);
    }

    /** Write a HTTPHeader on this stream.
     * @param header the HTTPHeader to write.
     * @param proxyConnected true if this connection is connected to another proxy.
     * @param proxyAuth the proxy authentication for the next proxy in the proxy chain.
     * @throws IOException if the header could not be written correctly.
     */
    public void writeHTTPHeader(HTTPHeader header, boolean proxyConnected, String proxyAuth) throws IOException {
        String requri = header.getRequestURI();
        try {
            if (header.isRequest()) {
                if (!header.isSecure()) {
                    URL url = new URL(requri);
                    if (!proxyConnected) {
                        header.setRequestURI(url.getFile());
                    } else {
                        String auth = proxyAuth;
                        if (auth != null && !auth.equals("")) {
                            auth = "Basic " + Coder.uuencode(auth);
                            header.setHeader("Proxy-authorization", auth);
                        }
                    }
                }
            }
            String chunked = header.getHeader("Transfer-Encoding");
            if (chunked != null && chunked.equalsIgnoreCase("chunked")) ischunked = true; else ischunked = false;
            byte[] b = header.toString().getBytes();
            is.write(b);
            if ((b = header.getContentArr()) != null) is.write(b);
        } catch (IOException e) {
            header.setRequestURI(requri);
            throw e;
        }
        header.setRequestURI(requri);
    }

    /** Write a byte to the underlying stream.
     *  This methods is not synchronized, synchronize if needed.
     * @param b the byte to write.
     * @throws IOException if the underlying write does.
     */
    public void write(int b) throws IOException {
        onebuf[0] = (byte) (b & 0xff);
        write(onebuf, 0, 1);
    }

    /** Write a byte array to the underlying stream.
     * @param buf the byte array to write.
     * @throws IOException if the underlying write does.
     */
    public void write(byte[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    /** Write a byte array to the underlying stream.
     * @param buf the byte array to write.
     * @param off the starting offset in the array.
     * @param len the number of bytes to write.
     * @throws IOException if the underlying write does.
     */
    public void write(byte[] buf, int off, int len) throws IOException {
        if (ischunked) {
            String chunksize = Long.toHexString(len);
            is.write(chunksize.getBytes());
            crlf(is);
            if (len > 0) {
                is.write(buf, off, len);
                crlf(is);
            }
        } else {
            is.write(buf, off, len);
        }
    }

    protected void crlf(OutputStream is) throws IOException {
        is.write(13);
        is.write(10);
    }

    /** Write a HTTPFooter on this stream. If you are sending data in 
     *  chunked manner you should always end them with writing a zero sized
     *  array followed by the footer (can be empty).
     * @param footer the HTTPFooter to write.
     */
    public void writeFooter(HTTPFooter footer) throws IOException {
        is.write(footer.toString().getBytes());
    }

    /** This will make sure the stream is finished.
     *  For chunked streams this method will send an empty block 
     *  Followed by an emtyp HTTPFooter.
     */
    public void finish() throws IOException {
        if (ischunked) {
            write(onebuf, 0, 0);
            HTTPFooter footer = new HTTPFooter();
            writeFooter(footer);
        }
    }

    public void close() throws IOException {
        if (socket != null) {
            if (socket.isOpen()) {
                socket.socket().shutdownInput();
                socket.socket().shutdownOutput();
                socket.socket().close();
            }
        }
        WritableByteChannel wbc = getChannel();
        if (wbc != null) if (wbc.isOpen()) wbc.close();
        super.close();
    }

    public WritableByteChannel getChannel() {
        if (socket != null) return socket;
        if (is instanceof FileOutputStream) return ((FileOutputStream) is).getChannel();
        return null;
    }
}
