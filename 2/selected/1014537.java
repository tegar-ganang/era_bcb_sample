package DE.FhG.IGD.io;

import DE.FhG.IGD.util.URL;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLConnection;

/**
 * This is an <code>OutputStream</code> wrapper that tries to open a stream
 * to the specified {@link URL}.
 * <p>Besides the definitions found in
 * <a href="http://sunsite.auc.dk/RFC/rfc/rfc1738.html" target="blank_">RFC 1738</a>,
 * &quot;<tt>socket</tt>&quot; will be accepted as an additional protocol
 * identifier for the URL of a TCP socket (provided by
 * {@link java.net.ServerSocket} or the like), e.g.
 * <tt>socket://192.168.0.1:30000</tt>
 * <p>This class fully supports output to <code>URL</code>s pointing to a
 * file. All other protocols depend on the output stream handler provided
 * by {@link URLConnection} and will make the constructor throw an exception
 * in case output is not supported.
 *
 * @author Matthias Pressfreund
 * @version $Id: URLOutputStream.java 1416 2004-07-02 13:11:50Z jpeters $
 */
public class URLOutputStream extends OutputStream {

    /**
     * The wrapped output stream
     */
    protected OutputStream os_;

    /**
     * Creates a <code>URLOutputStream</code> which tries to connect to
     * the given <code>URL</code>.
     *
     * @param url The destination <code>URL</code>
     * @exception IOException
     *   in case there is no output stream handler provided for the
     *   given <code>URL</code>
     * @exception NullPointerException
     *   if the given <code>URL</code> is <code>null</code>
     */
    public URLOutputStream(URL url) throws IOException, NullPointerException {
        super();
        URLConnection urlc;
        String protocol;
        if (url == null) {
            throw new NullPointerException("url");
        }
        protocol = url.getProtocol();
        if (protocol.equals("file")) {
            os_ = new FileOutputStream(url.getPath(), true);
        } else if (protocol.equals("socket")) {
            os_ = new Socket(url.getHost(), url.getPort()).getOutputStream();
        } else {
            urlc = new java.net.URL(protocol, url.getHost(), url.getPort(), url.getPath()).openConnection();
            urlc.setDoOutput(true);
            os_ = urlc.getOutputStream();
        }
    }

    /**
     * Calls the corresponding <code>write()</code> method on the
     * wrapped <code>OutputStream</code>.
     *
     * @see OutputStream#write(int)
     */
    public void write(int b) throws IOException {
        os_.write(b);
    }

    /**
     * Calls the corresponding <code>write()</code> method on the
     * wrapped <code>OutputStream</code>.
     *
     * @see OutputStream#write(byte[])
     */
    public void write(byte[] b) throws IOException {
        os_.write(b);
    }

    /**
     * Calls the corresponding <code>write()</code> method on the
     * wrapped <code>OutputStream</code>.
     *
     * @see OutputStream#write(byte[],int,int)
     */
    public void write(byte[] b, int off, int len) throws IOException {
        os_.write(b, off, len);
    }

    /**
     * Calls <code>flush()</code> on the wrapped <code>OutputStream</code>.
     *
     * @see OutputStream#flush
     */
    public void flush() throws IOException {
        os_.flush();
    }

    /**
     * Calls <code>close()</code> on the wrapped <code>OutputStream</code>.
     *
     * @see OutputStream#close
     */
    public void close() throws IOException {
        os_.close();
    }
}
