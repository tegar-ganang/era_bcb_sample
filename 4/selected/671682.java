package rabbit.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Date;
import rabbit.http.HTTPHeader;
import rabbit.util.Counter;
import rabbit.util.Logger;

/** A class to handle a connection to the Internet.
 */
public class WebConnection {

    private InetAddress ia;

    private int port;

    private boolean isProxyConnected;

    private String proxyAuth;

    private Counter counter;

    private Logger logger;

    private SocketChannel sc;

    private SelectionKey sk;

    private HTTPInputStream webis;

    private HTTPOutputStream webos;

    private Date releasedAt;

    private boolean pipelined = false;

    private boolean waiting = false;

    /** Create a new WebConnection to the given InetAddress and port.
     * @param ia the computer to connect to.
     * @param port the port number to connect to.
     */
    public WebConnection(InetAddress ia, int port, boolean isProxyConnected, String proxyAuth, Counter counter, Logger logger, NLSOHandler handler) throws IOException {
        this.ia = ia;
        this.port = port;
        this.isProxyConnected = isProxyConnected;
        this.proxyAuth = proxyAuth;
        this.counter = counter;
        this.logger = logger;
        try {
            sc = SocketChannel.open();
            Socket socket = sc.socket();
            sc.configureBlocking(false);
            SocketAddress addr = new InetSocketAddress(ia, port);
            boolean connected = sc.connect(addr);
            counter.inc("WebConnections created");
            webis = new HTTPInputStream(sc, connected, logger, handler);
            sc.configureBlocking(true);
            webos = new HTTPOutputStream(sc);
        } catch (IOException e) {
            logger.logError(Logger.WARN, "couldnt connect to the remote site (" + e.getMessage() + ")");
            throw new IOException("Could not connect to the site: " + ia + ":" + port);
        }
    }

    /** Close the connection.
     */
    public synchronized void close() {
        counter.inc("WebConnections closed");
        try {
            if (sc != null) {
                try {
                    if (webis != null) webis.close();
                    webis = null;
                    if (webos != null) webos.close();
                    webos = null;
                } catch (SocketException e) {
                    System.err.println("**** duh: " + e);
                }
            }
            sc = null;
            ia = null;
            releasedAt = null;
        } catch (IOException e) {
            logger.logError(Logger.WARN, "couldnt close WebConnection: " + e);
            e.printStackTrace();
        }
        synchronized (this) {
            notify();
            pipelined = false;
        }
    }

    public synchronized void setPipeline(boolean p) {
        pipelined = p;
    }

    public boolean getWaiting() {
        return waiting;
    }

    /** Get the InetAddress that this WebConnection is connected to.
     * @return the InetAddress.
     */
    public InetAddress getInetAddress() {
        return ia;
    }

    /** Get the port number this WebConnection is connected to.
     * @return the port number.
     */
    public int getPort() {
        return port;
    }

    /** Mark this WebConnection as released at current time.
     */
    public void setReleased() {
        setReleased(new Date());
    }

    /** Mark this WebConnection as released at given time.
     * @param d the time that this WebConnection is released.
     */
    public void setReleased(Date d) {
        try {
            webis.finish();
            webos.finish();
        } catch (IOException e) {
            logger.logError(Logger.WARN, "could not finish streams: " + e);
        }
        releasedAt = d;
        synchronized (this) {
            pipelined = false;
            if (waiting) notify();
        }
    }

    /** Get the time that this WebConnection was released.
     */
    public Date getReleasedAt() {
        return releasedAt;
    }

    protected void setSelectionKey(SelectionKey sk) {
        this.sk = sk;
    }

    protected SelectionKey getSelectionKey() {
        return sk;
    }

    protected SocketChannel getChannel() {
        return sc;
    }

    /** Get the InputStream.
     * @return an HTTPInputStream.
     */
    public synchronized HTTPInputStream getInputStream() {
        if (pipelined) {
            try {
                waiting = true;
                wait(1000);
                waiting = false;
                if (pipelined) {
                    webis.setKeepAlive(false);
                    return null;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return webis;
    }

    /** Get the OutputStream of this WebConnection.
     * @return an HTTPOutputStream.
     */
    public HTTPOutputStream getOutputStream() {
        return webos;
    }

    /** Get the keepalive value of this WebConnection.
     * @return true if this WebConnection may be reused.
     */
    public boolean getKeepAlive() {
        return webis.getKeepAlive();
    }

    /** Get the size of the data being fetched.
     * @return the number of bytes of the page or -1 if unknown.
     */
    public long dataSize() {
        return webis.dataSize();
    }

    /** Is this request chunked?
     * @return true if the last read request was chunked, false otherwise.
     */
    public boolean chunked() {
        return webis.chunked();
    }

    /** Get the last chunked page.
     * @return an InputStream to the last chunked page.
     */
    public InputStream getChunkStream() throws IOException {
        return webis.getChunkStream();
    }

    /** Write an HTTP header on this connection. 
     *  This is a convenience function to do <xmp>
     *  HTTPOutputStream hos = getOutputStream ();
     *  hos.writeHTTPHeader (header);
     *  </xmp>
     * @param header the Header to write.
     * @throws IOException if an I/O error occurs.
     */
    public void writeHTTPHeader(HTTPHeader header) throws IOException {
        if (webos == null) throw new IOException("Already closed");
        webis.setChunked(false);
        webos.writeHTTPHeader(header, isProxyConnected, proxyAuth);
    }
}
