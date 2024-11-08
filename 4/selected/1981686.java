package com.globant.google.mendoza.malbec.transport;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;
import javax.net.ServerSocketFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** A simple, tiny, nicely embeddable HTTP 1.0 server in Java
 *
 * <p><b>Features & limitations: </b><ul>
 *
 *    <li> Only one Java file </li>
 *
 *    <li> Java 1.1 compatible </li>
 *
 *    <li> Released as open source, Modified BSD licence </li>
 *
 *    <li> No fixed config files, logging, authorization etc. (Implement
 *    yourself if you need them.) </li>
 *
 *    <li> Supports parameter parsing of GET and POST methods </li>
 *
 *    <li> Supports both dynamic content and file serving </li>
 *
 *    <li> Never caches anything </li>
 *
 *    <li> Doesn't limit bandwidth, request time or simultaneous connections
 *    </li>
 *
 *    <li> Contains a built-in list of most common mime types </li>
 *
 * </ul>
 *
 * See the end of the source file for distribution license (Modified BSD
 * licence)
 */
public final class NanoHTTPD {

    /** The read buffer size, set to 2 kbytes.
   */
    private static final int READ_BUFFER_SIZE = 2048;

    /** The class logger.
   */
    private static Log log = LogFactory.getLog(NanoHTTPD.class);

    /** The logger used to log header and parameters information.
   */
    private static Log logHeader = LogFactory.getLog(NanoHTTPD.class.getName() + ".header");

    /** The logger used to log data trannfered.
   */
    private static Log logData = LogFactory.getLog(NanoHTTPD.class.getName() + ".data");

    /** The Receiver of the client message.
   */
    private Receiver receiver = null;

    /** The user name the client must use to authenticate to this server.
   */
    private String userName;

    /** The password the client must use to authenticate to this server.
   */
    private String password;

    /** The server socket the web server is listening to.
   */
    private ServerSocket serverSocket;

    /** The content type to use in the response.
   */
    private String contentType = MIME_XML;

    /** Gets the port that the server is listening on.
   *
   * @return Returns the port the server is listening on, usually the one set
   * with setPort, except when the server is started with 0 as the port number,
   * in wich case it returns the effective port used by the server.
   */
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    /** Sets the content type to use in the respones.
   *
   * The default content type is MIME_XML.
   *
   * @param theContentType the content type to use.
   */
    public void setContentType(final String theContentType) {
        contentType = theContentType;
    }

    /** Serves a request.
   *
   * @param uri  Percent-decoded URI without parameters, for example
   * "/index.cgi"
   *
   * @param method "GET", "POST" etc.
   *
   * @param parms  Parsed, percent decoded parameters from URI and, in case of
   * POST, data.
   *
   * @param header Header entries, percent decoded
   *
   * @param message The message received from the client.
   *
   * @return HTTP response, see class Response for details
   */
    private Response serve(final String uri, final String method, final Properties header, final Properties parms, final String message) {
        if (log.isTraceEnabled()) {
            log.trace("Entering serve('" + uri + "', '" + method + "...')");
        }
        logData.debug("Received message:\n" + message);
        String receivedMessage;
        if ("GET".equals(method)) {
            receivedMessage = uri;
        } else {
            receivedMessage = message;
        }
        String responseMessage;
        responseMessage = receiver.receive(uri, method, header, parms, receivedMessage);
        logData.debug("Responding with: \n" + responseMessage);
        Response response = new Response(HTTP_OK, contentType, responseMessage);
        log.trace("Leaving serve");
        return response;
    }

    /** HTTP response.
   */
    private static class Response {

        /** HTTP status code after processing, e.g. HTTP_OK.
     */
        private String status;

        /** Data of the response, may be null.
     */
        private InputStream data;

        /** Headers for the HTTP response. Use addHeader() to add lines.
     */
        private Properties header = new Properties();

        /** Creates a response to send to the client.
     *
     * This is a convenience method that makes an InputStream out of the given
     * text.
     *
     * @param responseStatus The http status.
     *
     * @param txt The data to send to the client.
     *
     * @param contentType The content type to use in the response.
     */
        public Response(final String responseStatus, final String contentType, final String txt) {
            status = responseStatus;
            data = new ByteArrayInputStream(txt.getBytes());
            addHeader("Content-Type", contentType);
        }

        /** Adds given line to the header.
     *
     * @param name The name of the header.
     *
     * @param value The value of the header.
     */
        public final void addHeader(final String name, final String value) {
            header.put(name, value);
        }

        /** Returns the http status to return to the client.
     *
     * @return Returns the status.
     */
        public final String getStatus() {
            return status;
        }

        /** Returns the data to send to the client.
     *
     * @return Returns an input stream with the data to send.
     */
        public final InputStream getData() {
            return data;
        }

        /** Returns the header to send to the client.
     *
     * @return Returns a properties object with the header to send.
     */
        public final Properties getHeader() {
            return header;
        }
    }

    /** Some HTTP response status codes.
   */
    public static final String HTTP_OK = "200 OK", HTTP_REDIRECT = "301 Moved Permanently", HTTP_UNAUTHORIZED = "401 Unauthorized", HTTP_FORBIDDEN = "403 Forbidden", HTTP_NOTFOUND = "404 Not Found", HTTP_BADREQUEST = "400 Bad Request", HTTP_INTERNALERROR = "500 Internal Server Error", HTTP_NOTIMPLEMENTED = "501 Not Implemented";

    /** Common mime types for dynamic content.
   */
    public static final String MIME_PLAINTEXT = "text/plain", MIME_HTML = "text/html", MIME_XML = "application/xml", MIME_DEFAULT_BINARY = "application/octet-stream";

    /** Creates and starts an instance of the web server.<p>
   *
   * This constructor creates a thread to listen to incoming connections and
   * returns immediately.
   *
   * @param port The port number to listen on.
   *
   * @param theReceiver The receiver of the client messages.
   *
   * @param socketFactory The socket factory used to create the ssl connection.
   *
   * @param name The name expected from the client to authenticate as. If it is
   * null, we do not expect the client to authenticate.
   *
   * @param thePassword The password expected from the client to authenticate
   * with. It must be non null if the name is not null.
   */
    public NanoHTTPD(final int port, final Receiver theReceiver, final ServerSocketFactory socketFactory, final String name, final String thePassword) {
        log.trace("Entering NanoHTTPD");
        if (name != null && thePassword == null) {
            throw new IllegalArgumentException("the password cannot be null");
        }
        receiver = theReceiver;
        userName = name;
        password = thePassword;
        if (log.isDebugEnabled()) {
            log.debug("Using port: " + port);
        }
        try {
            if (socketFactory != null) {
                serverSocket = socketFactory.createServerSocket(port);
            } else {
                serverSocket = new ServerSocket(port);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error obtaining server socket", e);
        }
        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    while (true) {
                        new HTTPSession(serverSocket.accept());
                    }
                } catch (AssertionError ae) {
                    throw ae;
                } catch (SocketException e) {
                    log.debug("Connection closed", e);
                } catch (IOException e) {
                    log.error("Error while waiting for connections", e);
                }
            }
        });
        t.setDaemon(true);
        t.setUncaughtExceptionHandler(Thread.currentThread().getUncaughtExceptionHandler());
        t.start();
        log.trace("Leaving NanoHTTPD");
    }

    /** Stops the server.
   */
    public synchronized void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error stoping server", e);
        }
    }

    /** Handles one session.
   *
   * Parses the HTTP request and returns the response.
   */
    private class HTTPSession implements Runnable {

        /** Session constructor.
     *
     * Creates a new thread to handle the request from the socket.
     *
     * @param s The socket where the request came from.
     */
        public HTTPSession(final Socket s) {
            mySocket = s;
            Thread t = new Thread(this);
            t.setDaemon(true);
            t.setUncaughtExceptionHandler(Thread.currentThread().getUncaughtExceptionHandler());
            t.start();
        }

        /** The entry point of the session thread.
     */
        public void run() {
            log.trace("Entering run");
            try {
                InputStream is = mySocket.getInputStream();
                if (is == null) {
                    log.trace("Leaving run: no data read from socket");
                    return;
                }
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String requestLine = in.readLine();
                if (log.isDebugEnabled()) {
                    log.debug("Read request: " + requestLine);
                }
                if (requestLine == null) {
                    sendError(HTTP_BADREQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");
                }
                StringTokenizer st = new StringTokenizer(requestLine);
                if (!st.hasMoreTokens()) {
                    sendError(HTTP_BADREQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");
                }
                String method = st.nextToken();
                if (!st.hasMoreTokens()) {
                    sendError(HTTP_BADREQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");
                }
                String uri = decodePercent(st.nextToken());
                Properties parms = new Properties();
                int qmi = uri.indexOf('?');
                if (qmi >= 0) {
                    decodeParms(uri.substring(qmi + 1), parms);
                    uri = decodePercent(uri.substring(0, qmi));
                }
                Properties header = new Properties();
                if (st.hasMoreTokens()) {
                    String line = in.readLine();
                    while (line != null && line.trim().length() > 0) {
                        int p = line.indexOf(':');
                        String name = line.substring(0, p).trim().toLowerCase();
                        String value = line.substring(p + 1).trim();
                        if (logHeader.isDebugEnabled()) {
                            logHeader.debug("Header: '" + name + "' = '" + value + "'");
                        }
                        header.put(name, value);
                        line = in.readLine();
                    }
                }
                if (userName != null) {
                    String authorization = header.getProperty("authorization");
                    if (log.isDebugEnabled()) {
                        log.debug("Authorization: " + authorization);
                    }
                    String encoding = new String(Base64.encodeBase64((userName + ":" + password).getBytes()));
                    String expected = "Basic " + encoding;
                    if (!expected.equals(authorization)) {
                        Properties headerToSend = new Properties();
                        headerToSend.put("WWW-Authenticate", "Basic realm=\"Checkout\"");
                        sendResponse(HTTP_UNAUTHORIZED, headerToSend, new ByteArrayInputStream("Needs Authentication".getBytes()));
                        throw new InterruptedException();
                    }
                }
                String postLine = "";
                if (method.equalsIgnoreCase("POST")) {
                    long size = Long.MAX_VALUE;
                    String contentLength = header.getProperty("content-length");
                    if (contentLength != null) {
                        try {
                            size = Integer.parseInt(contentLength);
                        } catch (NumberFormatException ex) {
                            log.warn("The client sent an invalid content length, ignoring");
                        }
                    }
                    char[] buf = new char[READ_BUFFER_SIZE];
                    StringBuffer buffer = new StringBuffer();
                    while (size > 0) {
                        int read = in.read(buf);
                        if (read == -1) {
                            break;
                        }
                        size -= read;
                        buffer.append(String.valueOf(buf, 0, read));
                    }
                    postLine = buffer.toString().trim();
                    String headerContentType = header.getProperty("content-type");
                    boolean isHtmlForm = (headerContentType != null) && headerContentType.indexOf("application/x-www-form-urlencoded") != -1;
                    if (isHtmlForm) {
                        log.debug("Read from client: " + postLine);
                        decodeParms(postLine, parms);
                    }
                }
                Response r = serve(uri, method, header, parms, postLine);
                if (r == null) {
                    sendError(HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: Serve() returned a null response.");
                } else {
                    sendResponse(r.getStatus(), r.getHeader(), r.getData());
                }
                in.close();
                receiver.received(postLine);
            } catch (IOException ioe) {
                try {
                    sendError(HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                } catch (InterruptedException ie) {
                    log.error("An error occurred, cancelling response");
                } catch (Throwable t) {
                    log.error("An error occurred sending error message", t);
                }
            } catch (InterruptedException ie) {
                log.error("An error occurred, cancelling response");
            } catch (RuntimeException e) {
                try {
                    log.error(e.getMessage(), e);
                    sendError(HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: Exception: " + e.getMessage());
                } catch (InterruptedException ie) {
                    log.error("An error occurred, cancelling response");
                } catch (Throwable t) {
                    log.error("An error occurred sending error message", t);
                }
            } catch (AssertionError ae) {
                throw ae;
            }
        }

        /** Decodes the percent encoding scheme.
     *
     * For example: "an+example%20string" to "an example string"
     *
     * @param str String to decode. Cannot be null.
     *
     * @return Returns the decoded string.
     *
     * @throws InterruptedException If an error occurs and must cancel the
     * response.
     */
        private String decodePercent(final String str) throws InterruptedException {
            final int entitySize = 2;
            final int entityBase = 16;
            try {
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < str.length(); i++) {
                    char c = str.charAt(i);
                    switch(c) {
                        case '+':
                            sb.append(' ');
                            break;
                        case '%':
                            sb.append((char) Integer.parseInt(str.substring(i + 1, i + 1 + entitySize), entityBase));
                            i += entitySize;
                            break;
                        default:
                            sb.append(c);
                            break;
                    }
                }
                return new String(sb.toString().getBytes());
            } catch (RuntimeException e) {
                sendError(HTTP_BADREQUEST, "BAD REQUEST: Bad percent-encoding.");
                return null;
            }
        }

        /** Decodes parameters in percent-encoded URI-format (e.g.
     * "name=Jack%20Daniels&pass=Single%20Malt") and adds them to given
     * Properties.
     *
     * @param parms The decoded parameters string.
     *
     * @param p The properties where to store the decoded parameters.
     *
     * @throws InterruptedException If an error occurs and must cancel the
     * response.
     */
        private void decodeParms(final String parms, final Properties p) throws InterruptedException {
            if (parms == null) {
                return;
            }
            StringTokenizer st = new StringTokenizer(parms, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                if (sep >= 0) {
                    p.put(decodePercent(e.substring(0, sep)).trim(), decodePercent(e.substring(sep + 1)));
                }
            }
        }

        /** Returns an error message as a HTTP response and immediately throws
     * InterruptedException to stop the requeste processing.
     *
     * @param status The http status to return to the client.
     *
     * @param msg The message to send to the client.
     *
     * @throws InterruptedException to stop furhter request processing.
     */
        private void sendError(final String status, final String msg) throws InterruptedException {
            Properties header = new Properties();
            header.put("Content-Type", MIME_PLAINTEXT);
            sendResponse(status, header, new ByteArrayInputStream(msg.getBytes()));
            throw new InterruptedException();
        }

        /** Sends given response to the client.
     *
     * @param status The status to send to the server, e.g.  "200 OK", HTTP_OK.
     *
     * @param header The headers to send to the client.
     *
     * @param data An input stream to read the data from.
     */
        private void sendResponse(final String status, final Properties header, final InputStream data) {
            try {
                if (status == null) {
                    throw new Error("sendResponse(): Status can't be null.");
                }
                OutputStream out = mySocket.getOutputStream();
                PrintWriter pw = new PrintWriter(out);
                pw.print("HTTP/1.0 " + status + " \r\n");
                if (header == null || header.getProperty("date") == null) {
                    pw.print("Date: " + gmtFrmt.format(new Date()) + "\r\n");
                }
                if (header != null) {
                    Enumeration e = header.keys();
                    while (e.hasMoreElements()) {
                        String key = (String) e.nextElement();
                        String value = header.getProperty(key);
                        pw.print(key + ": " + value + "\r\n");
                    }
                }
                pw.print("\r\n");
                pw.flush();
                if (data != null) {
                    byte[] buff = new byte[READ_BUFFER_SIZE];
                    while (true) {
                        int read = data.read(buff, 0, READ_BUFFER_SIZE);
                        if (read <= 0) {
                            break;
                        }
                        out.write(buff, 0, read);
                    }
                }
                out.flush();
                out.close();
                if (data != null) {
                    data.close();
                }
            } catch (IOException ioe) {
                try {
                    mySocket.close();
                } catch (Throwable t) {
                    log.error("Could not send message to client", t);
                }
            }
        }

        /** The socket this session is listening to.
     */
        private Socket mySocket;
    }

    ;

    /** GMT date formatter.
   */
    private static java.text.SimpleDateFormat gmtFrmt;

    static {
        gmtFrmt = new java.text.SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
}
