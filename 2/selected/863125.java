package org.opennms.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;

/**
 * Provides convenience methods for use the HTTP POST method.
 * 
 * @author <A HREF="mailto:larry@opennms.org">Lawrence Karnowski </A>
 * @author <A HREF="http://www.opennms.org/">OpenNMS </A>
 * 
 */
public final class HttpUtils extends Object {

    /** Private constructor so this class will not be instantiated. */
    private HttpUtils() {
    }

    /** Default buffer size for reading data. (Default is one kilobyte.) */
    public static final int DEFAULT_POST_BUFFER_SIZE = 1024;

    /**
     * Post a given <code>InputStream</code> s data to a URL.
     * 
     * @param url
     *            the <code>URL</code> to post to
     * @param dataStream
     *            an input stream containing the data to send
     * @return An <code>InputStream</a> that the programmer can read from
     * to get the HTTP server's response.
     */
    public static InputStream post(URL url, InputStream dataStream) throws IOException {
        return (post(url, dataStream, null, null, DEFAULT_POST_BUFFER_SIZE));
    }

    /**
     * Post a given <code>InputStream</code> s data to a URL using BASIC
     * authentication and the given username and password.
     * 
     * @param url
     *            the <code>URL</code> to post to
     * @param dataStream
     *            an input stream containing the data to send
     * @param username
     *            the username to use in the BASIC authentication
     * @param password
     *            the password to use in the BASIC authentication
     * @return An <code>InputStream</a> that the programmer can read from
     * to get the HTTP server's response.
     */
    public static InputStream post(URL url, InputStream dataStream, String username, String password) throws IOException {
        return (post(url, dataStream, username, password, DEFAULT_POST_BUFFER_SIZE));
    }

    /**
     * Post a given <code>InputStream</code> s data to a URL using BASIC
     * authentication, the given username and password, and a buffer size.
     * 
     * @param url
     *            the <code>URL</code> to post to
     * @param dataStream
     *            an input stream containing the data to send
     * @param username
     *            the username to use in the BASIC authentication
     * @param password
     *            the password to use in the BASIC authentication
     * @param bufSize
     *            the size of the buffer to read from <code>dataStream</code>
     *            and write to the HTTP server
     * @return An <code>InputStream</a> that the programmer can read from
     * to get the HTTP server's response.
     */
    public static InputStream post(URL url, InputStream dataStream, String username, String password, int bufSize) throws IOException {
        if (url == null || dataStream == null) {
            throw new IllegalArgumentException("Cannot take null parameters.");
        }
        if (bufSize < 1) {
            throw new IllegalArgumentException("Cannot use zero or negative buffer size.");
        }
        if (!"http".equals(url.getProtocol())) {
            throw new IllegalArgumentException("Cannot use non-HTTP URLs.");
        }
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        try {
            conn.setRequestMethod("POST");
        } catch (java.net.ProtocolException e) {
            throw new IllegalStateException("Could not set a HttpURLConnection's method to POST.");
        }
        if (username != null && password != null) {
            byte[] authBytes = (username + ":" + password).getBytes();
            String authString = new String(Base64.encodeBase64(authBytes));
            conn.setRequestProperty("Authorization", "Basic " + authString);
        }
        OutputStream ostream = conn.getOutputStream();
        byte[] b = new byte[bufSize];
        int bytesRead = dataStream.read(b, 0, bufSize);
        while (bytesRead > 0) {
            ostream.write(b, 0, bytesRead);
            bytesRead = dataStream.read(b, 0, bufSize);
        }
        ostream.close();
        return (conn.getInputStream());
    }

    /**
     * Post a given <code>Reader</code> s data to a URL using BASIC
     * authentication, the given username and password, and a buffer size.
     * 
     * @param url
     *            the <code>URL</code> to post to
     * @param dataReader
     *            an input reader containing the data to send
     * @param username
     *            the username to use in the BASIC authentication
     * @param password
     *            the password to use in the BASIC authentication
     * @param bufSize
     *            the size of the buffer to read from <code>dataStream</code>
     *            and write to the HTTP server
     * @return An <code>InputStream</a> that the programmer can read from
     * to get the HTTP server's response.
     */
    public static InputStream post(URL url, Reader dataReader, String username, String password, int bufSize) throws IOException {
        if (url == null || dataReader == null) {
            throw new IllegalArgumentException("Cannot take null parameters.");
        }
        if (bufSize < 1) {
            throw new IllegalArgumentException("Cannot use zero or negative buffer size.");
        }
        if (!"http".equals(url.getProtocol())) {
            throw new IllegalArgumentException("Cannot use non-HTTP URLs.");
        }
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        try {
            conn.setRequestMethod("POST");
        } catch (java.net.ProtocolException e) {
            throw new IllegalStateException("Could not set a HttpURLConnection's method to POST.");
        }
        if (username != null && password != null) {
            byte[] authBytes = (username + ":" + password).getBytes();
            String authString = new String(Base64.encodeBase64(authBytes));
            conn.setRequestProperty("Authorization", "Basic " + authString);
        }
        conn.setRequestProperty("Content-type", "text/xml; charset=\"utf-8\"");
        OutputStreamWriter ostream = new OutputStreamWriter(conn.getOutputStream(), "US-ASCII");
        Category log = Logger.getLogger("POSTDATALOG");
        if (log.isDebugEnabled()) {
            String nl = System.getProperty("line.separator");
            log.debug(nl + "HTTP Post: Current time: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.GregorianCalendar().getTime()));
            log.debug(nl + "Data posted:" + nl);
        }
        char[] b = new char[bufSize];
        int bytesRead = dataReader.read(b, 0, bufSize);
        if (bytesRead > 0 && log.isDebugEnabled()) log.debug(new String(b, 0, bytesRead));
        while (bytesRead > 0) {
            ostream.write(b, 0, bytesRead);
            bytesRead = dataReader.read(b, 0, bufSize);
            if (bytesRead > 0 && log.isDebugEnabled()) log.debug(new String(b, 0, bytesRead));
        }
        ostream.close();
        return (conn.getInputStream());
    }
}
