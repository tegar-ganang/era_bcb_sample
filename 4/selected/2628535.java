package de.haumacher.timecollect.remote.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple, tiny, nicely embeddable HTTP 1.0 server in Java
 *
 * <p> NanoHTTPD version 1.12,
 * Copyright &copy; 2001,2005-2010 Jarno Elonen (elonen@iki.fi, http://iki.fi/elonen/)
 *
 * <p><b>Features + limitations: </b><ul>
 *
 *    <li> Only one Java file </li>
 *    <li> Java 1.1 compatible </li>
 *    <li> Released as open source, Modified BSD licence </li>
 *    <li> No fixed config files, logging, authorization etc. (Implement yourself if you need them.) </li>
 *    <li> Supports parameter parsing of GET and POST methods </li>
 *    <li> Supports both dynamic content and file serving </li>
 *    <li> Never caches anything </li>
 *    <li> Doesn't limit bandwidth, request time or simultaneous connections </li>
 *    <li> Default code serves files and shows all HTTP parameters and headers</li>
 *    <li> File server supports directory listing, index.html and index.htm </li>
 *    <li> File server does the 301 redirection trick for directories without '/'</li>
 *    <li> File server supports simple skipping for files (continue download) </li>
 *    <li> File server uses current directory as a web root </li>
 *    <li> File server serves also very long files without memory overhead </li>
 *    <li> Contains a built-in list of most common mime types </li>
 *    <li> All header names are converted lowercase so they don't vary between browsers/clients </li>
 *
 * </ul>
 *
 * <p><b>Ways to use: </b><ul>
 *
 *    <li> Run as a standalone app, serves files from current directory and shows requests</li>
 *    <li> Subclass serve() and embed to your own program </li>
 *    <li> Call serveFile() from serve() with your own base directory </li>
 *
 * </ul>
 *
 * See the end of the source file for distribution license
 * (Modified BSD licence)
 */
public class NanoHTTPD implements Runnable {

    private static final Logger LOG = Logger.getLogger(NanoHTTPD.class.getName());

    /**
	 * The {@link ServerSocket} that is observed by this {@link NanoHTTPD} instance.
	 */
    private ServerSocket serverSocket;

    /**
	 * The port on which {@link #serverSocket} is bound.
	 */
    private final int tcpPort;

    /**
	 * The main thread of this {@link NanoHTTPD} instance that dispatches requests.
	 */
    private Thread acceptThread;

    /**
	 * Whether each response should be served in a separate thread.
	 */
    protected final boolean multithreaded;

    /**
	 * The {@link Handler} that processes {@link Request}s.
	 */
    protected final Handler handler;

    /**
	 * Interface that allows to plug in a request handler into {@link NanoHTTPD}.
	 */
    public interface Handler {

        /**
		 * Override this to customize the server.<p>
		 *
		 * (By default, this delegates to serveFile() and allows directory listing.)
		 *
		 * @parm uri	Percent-decoded URI without parameters, for example "/index.cgi"
		 * @parm method	"GET", "POST" etc.
		 * @parm parms	Parsed, percent decoded parameters from URI and, in case of POST, data.
		 * @parm header	Header entries, percent decoded
		 * @param source The socket that identifies the source of the request. Must only be 
		 *        used for testing socket properties.
		 * @return HTTP response, see class Response for details
		 */
        Response serve(String uri, String method, Properties header, Properties parms, Socket source);
    }

    /**
	 * {@link Handler} that dispatches to sub {@link Handler}s by URI prefix.
	 * 
	 * @author Bernhard Haumacher
	 */
    public static class DispatchingHandler implements Handler {

        /**
		 * @see #getHandlers()
		 */
        private final Map<String, Handler> handlers;

        /**
		 * Creates a {@link DispatchingHandler}.
		 * 
		 * @param handlers See {@link #getHandlers()}.
		 */
        public DispatchingHandler(Map<String, Handler> handlers) {
            this.handlers = handlers;
        }

        /**
		 * Mapping of URI prefixes to sub {@link Handler}s.
		 * 
		 * <p>
		 * The {@link Handler} serving the root is expected to be registered
		 * under the "/" key.
		 * </p>
		 */
        public Map<String, Handler> getHandlers() {
            return handlers;
        }

        @Override
        public Response serve(String uri, String method, Properties header, Properties parms, Socket source) {
            if (uri == null || uri.length() == 0 || uri.charAt(0) != '/') {
                return notFound();
            }
            String context;
            String subUri;
            int contextSeparatorIdx = uri.indexOf('/', 1);
            if (contextSeparatorIdx < 0) {
                context = uri;
                subUri = "/";
            } else {
                context = uri.substring(0, contextSeparatorIdx);
                subUri = uri.substring(contextSeparatorIdx);
            }
            Handler handler = handlers.get(context);
            if (handler == null) {
                return notFound();
            }
            return handler.serve(subUri, method, header, parms, source);
        }

        private Response notFound() {
            return new Response(HTTP_NOTFOUND, "text/plain", "Resource not found.");
        }
    }

    /**
	 * HTTP response.
	 * Return one of these from serve().
	 */
    public static class Response {

        /**
		 * Default constructor: response = HTTP_OK, data = mime = 'null'
		 */
        public Response() {
            this.status = HTTP_OK;
        }

        /**
		 * Basic constructor.
		 */
        public Response(String status, String mimeType, InputStream data) {
            this.status = status;
            this.mimeType = mimeType;
            this.data = data;
        }

        /**
		 * Convenience method that makes an InputStream out of
		 * given text.
		 */
        public Response(String status, String mimeType, String txt) {
            this.status = status;
            this.mimeType = mimeType;
            this.data = new ByteArrayInputStream(txt.getBytes());
        }

        /**
		 * Adds given line to the header.
		 */
        public void addHeader(String name, String value) {
            header.put(name, value);
        }

        /**
		 * HTTP status code after processing, e.g. "200 OK", HTTP_OK
		 */
        public String status;

        /**
		 * MIME type of content, e.g. "text/html"
		 */
        public String mimeType;

        /**
		 * Data of the response, may be null.
		 */
        public InputStream data;

        /**
		 * Headers for the HTTP response. Use addHeader()
		 * to add lines.
		 */
        public Properties header = new Properties();
    }

    /**
	 * Some HTTP response status codes
	 */
    public static final String HTTP_OK = "200 OK", HTTP_REDIRECT = "301 Moved Permanently", HTTP_FORBIDDEN = "403 Forbidden", HTTP_NOTFOUND = "404 Not Found", HTTP_BADREQUEST = "400 Bad Request", HTTP_INTERNALERROR = "500 Internal Server Error", HTTP_NOTIMPLEMENTED = "501 Not Implemented";

    /**
	 * Common mime types for dynamic content
	 */
    public static final String MIME_PLAINTEXT = "text/plain", MIME_HTML = "text/html", MIME_DEFAULT_BINARY = "application/octet-stream";

    /**
	 * Starts a HTTP server to given port.<p>
	 * Throws an IOException if the socket is already in use
	 */
    public NanoHTTPD(int port, Handler handler) throws IOException {
        this(port, handler, false);
    }

    /**
	 * Starts a HTTP server to given port.<p>
	 * Throws an IOException if the socket is already in use
	 */
    public NanoHTTPD(int port, Handler handler, boolean multithreaded) throws IOException {
        this.multithreaded = multithreaded;
        this.tcpPort = port;
        this.handler = handler;
        this.serverSocket = new ServerSocket(tcpPort);
    }

    /**
	 * Starts the main request dispatcher.
	 */
    public synchronized void start() {
        acceptThread = new Thread(this);
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    /**
	 * Stops the main request dispatcher but returns immediately.
	 * 
	 * @see #join(long)
	 */
    public synchronized void stop() throws IOException {
        if (acceptThread == null) {
            return;
        }
        acceptThread.interrupt();
        serverSocket.close();
    }

    /**
	 * Waits until the main request dispatcher terminates.
	 * 
	 * @see #stop()
	 */
    public synchronized void join(long timeout) throws InterruptedException {
        if (acceptThread == null) {
            return;
        }
        acceptThread.join(timeout);
    }

    /**
	 * Servers main loop accepting connections.
	 */
    public void run() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                try {
                    process(new Request(clientSocket));
                } catch (Throwable ex) {
                    LOG.log(Level.WARNING, "HTTP service failed.", ex);
                }
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }
        } catch (IOException ioe) {
        } finally {
            synchronized (this) {
                this.acceptThread = null;
            }
        }
    }

    /**
	 * Process a single request.
	 * 
	 * <p>
	 * Hook to customize threading behavior.
	 * </p>
	 * 
	 * @param session
	 *        The request to parse and
	 *        {@link #serve(String, String, Properties, Properties, Socket)}.
	 */
    protected void process(Request session) {
        if (multithreaded) {
            Thread t = new Thread(session);
            t.setDaemon(true);
            t.start();
        } else {
            session.run();
        }
    }

    /**
	 * Handles one session, i.e. parses the HTTP request
	 * and returns the response.
	 */
    private class Request implements Runnable {

        public Request(Socket s) {
            mySocket = s;
        }

        public void run() {
            try {
                InputStream is = mySocket.getInputStream();
                if (is == null) return;
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String inLine = in.readLine();
                if (inLine == null) return;
                StringTokenizer st = new StringTokenizer(inLine);
                if (!st.hasMoreTokens()) sendError(HTTP_BADREQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");
                String method = st.nextToken();
                if (!st.hasMoreTokens()) sendError(HTTP_BADREQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");
                String uri = st.nextToken();
                Properties parms = new Properties();
                int qmi = uri.indexOf('?');
                if (qmi >= 0) {
                    decodeParms(uri.substring(qmi + 1), parms);
                    uri = decodePercent(uri.substring(0, qmi));
                } else uri = decodePercent(uri);
                Properties header = new Properties();
                if (st.hasMoreTokens()) {
                    String line = in.readLine();
                    while ((line != null) && line.trim().length() > 0) {
                        int p = line.indexOf(':');
                        header.put(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
                        line = in.readLine();
                    }
                }
                if (method.equalsIgnoreCase("POST")) {
                    long size = 0x7FFFFFFFFFFFFFFFl;
                    String contentLength = header.getProperty("content-length");
                    if (contentLength != null) {
                        try {
                            size = Integer.parseInt(contentLength);
                        } catch (NumberFormatException ex) {
                        }
                    }
                    String postLine = "";
                    char buf[] = new char[512];
                    int read = in.read(buf);
                    while (read >= 0 && size > 0 && !postLine.endsWith("\r\n")) {
                        size -= read;
                        postLine += String.valueOf(buf, 0, read);
                        if (size > 0) read = in.read(buf);
                    }
                    postLine = postLine.trim();
                    decodeParms(postLine, parms);
                }
                Response r = handler.serve(uri, method, header, parms, mySocket);
                if (r == null) sendError(HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: Serve() returned a null response."); else sendResponse(r.status, r.mimeType, r.header, r.data);
                in.close();
            } catch (IOException ioe) {
                try {
                    sendError(HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                } catch (Throwable t) {
                }
            } catch (InterruptedException ie) {
            }
        }

        /**
		 * Decodes the percent encoding scheme. <br/>
		 * For example: "an+example%20string" -> "an example string"
		 */
        private String decodePercent(String str) throws InterruptedException {
            try {
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < str.length(); i++) {
                    char c = str.charAt(i);
                    switch(c) {
                        case '+':
                            sb.append(' ');
                            break;
                        case '%':
                            sb.append((char) Integer.parseInt(str.substring(i + 1, i + 3), 16));
                            i += 2;
                            break;
                        default:
                            sb.append(c);
                            break;
                    }
                }
                return new String(sb.toString().getBytes());
            } catch (Exception e) {
                sendError(HTTP_BADREQUEST, "BAD REQUEST: Bad percent-encoding.");
                return null;
            }
        }

        /**
		 * Decodes parameters in percent-encoded URI-format
		 * ( e.g. "name=Jack%20Daniels&pass=Single%20Malt" ) and
		 * adds them to given Properties. NOTE: this doesn't support multiple
		 * identical keys due to the simplicity of Properties -- if you need multiples,
		 * you might want to replace the Properties with a Hastable of Vectors or such.
		 */
        private void decodeParms(String parms, Properties p) throws InterruptedException {
            if (parms == null) return;
            StringTokenizer st = new StringTokenizer(parms, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                if (sep >= 0) p.put(decodePercent(e.substring(0, sep)).trim(), decodePercent(e.substring(sep + 1)));
            }
        }

        /**
		 * Returns an error message as a HTTP response and
		 * throws InterruptedException to stop furhter request processing.
		 */
        private void sendError(String status, String msg) throws InterruptedException {
            sendResponse(status, MIME_PLAINTEXT, null, new ByteArrayInputStream(msg.getBytes()));
            throw new InterruptedException();
        }

        /**
		 * Sends given response to the socket.
		 */
        private void sendResponse(String status, String mime, Properties header, InputStream data) {
            try {
                if (status == null) throw new Error("sendResponse(): Status can't be null.");
                OutputStream out = mySocket.getOutputStream();
                PrintWriter pw = new PrintWriter(out);
                pw.print("HTTP/1.0 " + status + " \r\n");
                if (mime != null) pw.print("Content-Type: " + mime + "\r\n");
                if (header == null || header.getProperty("Date") == null) pw.print("Date: " + gmtFrmt.format(new Date()) + "\r\n");
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
                    byte[] buff = new byte[2048];
                    while (true) {
                        int read = data.read(buff, 0, 2048);
                        if (read <= 0) break;
                        out.write(buff, 0, read);
                    }
                }
                out.flush();
                out.close();
                if (data != null) data.close();
            } catch (IOException ioe) {
                try {
                    mySocket.close();
                } catch (Throwable t) {
                }
            }
        }

        private Socket mySocket;
    }

    ;

    /**
	 * URL-encodes everything between "/"-characters.
	 * Encodes spaces as '%20' instead of '+'.
	 */
    private String encodeUri(String uri) {
        String newUri = "";
        StringTokenizer st = new StringTokenizer(uri, "/ ", true);
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals("/")) newUri += "/"; else if (tok.equals(" ")) newUri += "%20"; else {
                newUri += URLEncoder.encode(tok);
            }
        }
        return newUri;
    }

    /**
	 * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
	 */
    private static Hashtable theMimeTypes = new Hashtable();

    static {
        StringTokenizer st = new StringTokenizer("htm		text/html " + "html		text/html " + "txt		text/plain " + "asc		text/plain " + "gif		image/gif " + "jpg		image/jpeg " + "jpeg		image/jpeg " + "png		image/png " + "mp3		audio/mpeg " + "m3u		audio/mpeg-url " + "pdf		application/pdf " + "doc		application/msword " + "ogg		application/x-ogg " + "zip		application/octet-stream " + "exe		application/octet-stream " + "class		application/octet-stream ");
        while (st.hasMoreTokens()) theMimeTypes.put(st.nextToken(), st.nextToken());
    }

    /**
	 * GMT date formatter
	 */
    private static java.text.SimpleDateFormat gmtFrmt;

    static {
        gmtFrmt = new java.text.SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
	 * The distribution licence
	 */
    private static final String LICENCE = "Copyright (C) 2001,2005-2008 by Jarno Elonen <elonen@iki.fi>\n" + "\n" + "Redistribution and use in source and binary forms, with or without\n" + "modification, are permitted provided that the following conditions\n" + "are met:\n" + "\n" + "Redistributions of source code must retain the above copyright notice,\n" + "this list of conditions and the following disclaimer. Redistributions in\n" + "binary form must reproduce the above copyright notice, this list of\n" + "conditions and the following disclaimer in the documentation and/or other\n" + "materials provided with the distribution. The name of the author may not\n" + "be used to endorse or promote products derived from this software without\n" + "specific prior written permission. \n" + " \n" + "THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR\n" + "IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES\n" + "OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.\n" + "IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,\n" + "INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT\n" + "NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,\n" + "DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY\n" + "THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\n" + "(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE\n" + "OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.";
}
