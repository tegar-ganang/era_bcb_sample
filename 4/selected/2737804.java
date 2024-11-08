package pspdash;

import pspdash.data.DataRepository;
import pspdash.data.DoubleData;
import pspdash.data.ImmutableDoubleData;
import pspdash.data.StringData;
import java.net.*;
import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.*;

public class TinyWebServer extends Thread {

    ServerSocket serverSocket = null;

    ServerSocket dataSocket = null;

    Vector serverThreads = new Vector();

    URL[] roots = null;

    DataRepository data = null;

    Hashtable cgiLoaderMap = new Hashtable();

    Hashtable addOnLoaderMap = new Hashtable();

    Hashtable cgiCache = new Hashtable();

    MD5 md5 = new MD5();

    boolean allowRemoteConnections = false;

    private int port;

    private String startupTimestamp, startupTimestampHeader;

    public static final String PROTOCOL = "HTTP/1.0";

    public static final String DEFAULT_TEXT_MIME_TYPE = "text/plain; charset=iso-8859-1";

    public static final String DEFAULT_BINARY_MIME_TYPE = "application/octet-stream";

    public static final String SERVER_PARSED_MIME_TYPE = "text/x-server-parsed-html";

    public static final String CGI_MIME_TYPE = "application/x-httpd-cgi";

    public static final String TIMESTAMP_HEADER = "Dash-Startup-Timestamp";

    public static final String PACKAGE_ENV_PREFIX = "Dash_Package_";

    public static final String LINK_SUFFIX = ".link";

    public static final String LINK_MIME_TYPE = "text/x-server-shortcut";

    public static final String CGI_LINK_PREFIX = "class:";

    private static final DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    private static InetAddress LOCAL_HOST_ADDR, LOOPBACK_ADDR;

    private static final Properties mimeTypes = new Properties();

    private static final Hashtable DEFAULT_ENV = new Hashtable();

    private static final String CRLF = "\r\n";

    private static final int SCAN_BUF_SIZE = 4096;

    private static final String DASH_CHARSET = "iso-8859-1";

    private static final String HEADER_CHARSET = DASH_CHARSET;

    private static String OUTPUT_CHARSET = DASH_CHARSET;

    static {
        try {
            DEFAULT_ENV.put("SERVER_SOFTWARE", "PSPDASH");
            DEFAULT_ENV.put("SERVER_NAME", "localhost");
            DEFAULT_ENV.put("GATEWAY_INTERFACE", "CGI/1.1");
            DEFAULT_ENV.put("SERVER_ADDR", "127.0.0.1");
            DEFAULT_ENV.put("PATH_INFO", "");
            DEFAULT_ENV.put("PATH_TRANSLATED", "");
            DEFAULT_ENV.put("REMOTE_HOST", "localhost");
            DEFAULT_ENV.put("REMOTE_ADDR", "127.0.0.1");
            dateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
            mimeTypes.load(TinyWebServer.class.getResourceAsStream("mime_types"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            LOCAL_HOST_ADDR = InetAddress.getLocalHost();
            LOOPBACK_ADDR = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException uhe) {
        }
    }

    private ClassLoader getParentClassLoader(String url) {
        if (!url.startsWith("jar:")) return null;
        int pos = url.lastIndexOf("!/Templates");
        if (pos == -1) return null;
        url = url.substring(0, pos + 2);
        synchronized (addOnLoaderMap) {
            ClassLoader result = (ClassLoader) addOnLoaderMap.get(url);
            if (result == null) try {
                result = new AddOnClassLoader(url);
                addOnLoaderMap.put(url, result);
            } catch (Exception e) {
            }
            return result;
        }
    }

    private class AddOnClassLoader extends ClassLoader {

        URL base;

        public AddOnClassLoader(String path) {
            super();
            init(path);
        }

        protected AddOnClassLoader(String path, ClassLoader parent) {
            super(parent);
            init(path);
        }

        private void init(String path) {
            try {
                base = new URL(path);
            } catch (Exception e) {
                base = null;
            }
        }

        protected Class findClass(String name) throws ClassNotFoundException {
            if (name.startsWith("Templates.")) throw new ClassNotFoundException(name);
            try {
                String filename = name.replace('.', '/') + ".class";
                URL classURL = new URL(base, filename);
                URLConnection conn = classURL.openConnection();
                conn.connect();
                byte[] defn = slurpContents(conn.getInputStream(), true);
                Class result = defineClass(name, defn, 0, defn.length);
                resolveClass(result);
                return result;
            } catch (Exception e) {
                throw new ClassNotFoundException(name);
            }
        }

        protected URL findResource(String name) {
            try {
                URL resourceURL = new URL(base, name);
                URLConnection conn = resourceURL.openConnection();
                conn.connect();
                return resourceURL;
            } catch (Exception e) {
                return null;
            }
        }
    }

    private class CGILoader extends AddOnClassLoader {

        public CGILoader(String path) {
            super(path);
        }

        public CGILoader(String path, ClassLoader parent) {
            super(path, parent);
        }

        public Class loadFromConnection(URLConnection conn) throws IOException, ClassFormatError {
            String className = conn.getURL().getFile();
            int beg = className.lastIndexOf('/');
            int end = className.indexOf('.', beg);
            className = className.substring(beg + 1, end);
            Class result = findLoadedClass(className);
            if (result != null) return result;
            byte[] defn = slurpContents(conn.getInputStream(), true);
            synchronized (this) {
                result = findLoadedClass(className);
                if (result != null) return result;
                result = defineClass(className, defn, 0, defn.length);
                resolveClass(result);
            }
            return result;
        }

        protected Class findClass(String name) throws ClassNotFoundException {
            if (name.indexOf('.') != -1) throw new ClassNotFoundException(name);
            return super.findClass(name);
        }
    }

    ResourcePool getCGIPool(String path) {
        return (ResourcePool) cgiCache.get(path);
    }

    private class TinyWebThread extends Thread {

        Socket clientSocket = null;

        InputStream inputStream = null;

        BufferedReader in = null;

        OutputStream outputStream = null;

        Writer headerOut = null;

        boolean isRunning = false;

        Exception exceptionEncountered = null;

        boolean headerRead = false;

        Map env = null;

        String outputCharset = OUTPUT_CHARSET;

        String uri, method, protocol, id, path, query;

        private class TinyWebThreadException extends Exception {
        }

        ;

        public TinyWebThread(Socket clientSocket) {
            try {
                this.clientSocket = clientSocket;
                this.inputStream = clientSocket.getInputStream();
                this.in = new BufferedReader(new InputStreamReader(inputStream));
                this.outputStream = clientSocket.getOutputStream();
                this.headerOut = new BufferedWriter(new OutputStreamWriter(outputStream, HEADER_CHARSET));
            } catch (IOException ioe) {
                this.inputStream = null;
            }
        }

        public TinyWebThread(String uri) {
            this.clientSocket = null;
            String request = "GET " + uri + " HTTP/1.0\r\n\r\n";
            this.inputStream = new ByteArrayInputStream(request.getBytes());
            this.in = new BufferedReader(new InputStreamReader(inputStream));
            this.outputStream = new ByteArrayOutputStream(1024);
            this.outputCharset = "UTF-8";
            try {
                this.headerOut = new BufferedWriter(new OutputStreamWriter(outputStream, HEADER_CHARSET));
            } catch (UnsupportedEncodingException e) {
            }
        }

        public byte[] getOutput() throws IOException {
            if (outputStream instanceof ByteArrayOutputStream) {
                run();
                if (exceptionEncountered instanceof IOException) throw (IOException) exceptionEncountered;
                if (exceptionEncountered != null) {
                    IOException ioe = new IOException();
                    ioe.initCause(exceptionEncountered);
                    throw ioe;
                }
                return ((ByteArrayOutputStream) outputStream).toByteArray();
            } else return null;
        }

        public void dispose() {
            close();
            inputStream = null;
            outputStream = null;
            exceptionEncountered = null;
        }

        public synchronized void close() {
            if (isRunning) this.interrupt();
            serverThreads.remove(this);
            try {
                if (headerOut != null) {
                    headerOut.flush();
                    headerOut.close();
                }
                if (in != null) in.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException ioe) {
            }
            headerOut = null;
            in = null;
            clientSocket = null;
            env = null;
        }

        public void run() {
            if (inputStream != null) {
                isRunning = true;
                try {
                    handleRequest();
                } catch (TinyWebThreadException twte) {
                    if (exceptionEncountered == null) exceptionEncountered = twte;
                }
                isRunning = false;
            }
            close();
        }

        private void handleRequest() throws TinyWebThreadException {
            String line = null;
            try {
                line = readLine(inputStream);
                StringTokenizer tok = new StringTokenizer(line, " ");
                method = tok.nextToken();
                uri = tok.nextToken();
                protocol = tok.nextToken();
                if (!"GET".equals(method) && !"POST".equals(method)) sendError(501, "Not Implemented", "Unsupported Request Method");
                parseURI(uri);
                checkIP();
                serveRequest();
            } catch (NoSuchElementException nsee) {
                sendError(400, "Bad Request", "No request found.");
            } catch (IOException ioe) {
                sendError(500, "Internal Error", "IO Exception.");
            }
        }

        private void serveRequest() throws TinyWebThreadException, IOException {
            URLConnection conn = resolveURL(path);
            String initial_mime_type = getMimeTypeFromName(conn.getURL().getFile());
            if (!Translator.isTranslating() && SERVER_PARSED_MIME_TYPE.equals(initial_mime_type)) servePreprocessedFile(conn, "text/html"); else if (CGI_MIME_TYPE.equals(initial_mime_type)) serveCGI(conn); else if (LINK_MIME_TYPE.equals(initial_mime_type)) serveLink(conn); else servePlain(conn, initial_mime_type);
        }

        /** Break the URI into hierarchy path, file path, and query string.
         *
         * The results are placed into the object-global variables
         * "id", "path", and "query".
         *
         * URIs of the following forms are recognized (all may have
         * query strings appended): <PRE>
         *     /#####/regular/path
         *     /regular/path
         *     //regular/path
         *     /hierarchy/path//regular/path
         * </PRE> */
        private void parseURI(String uri) throws TinyWebThreadException {
            int pos = uri.indexOf('?');
            if (pos != -1) {
                query = uri.substring(pos + 1);
                uri = uri.substring(0, pos);
            }
            uri = canonicalizePath(uri);
            if (uri == null || !uri.startsWith("/")) sendError(400, "Bad Request", "Bad filename.");
            pos = uri.indexOf("//");
            if (pos >= 0) {
                id = uri.substring(0, pos);
                path = uri.substring(pos + 2);
            } else try {
                pos = uri.indexOf('/', 1);
                id = uri.substring(1, pos);
                Integer.parseInt(id);
                path = uri.substring(pos + 1);
            } catch (Exception e) {
                id = "";
                path = uri.substring(1);
            }
        }

        /** Resolve an absolute URL */
        private URLConnection resolveURL(String url) throws TinyWebThreadException {
            URLConnection result = TinyWebServer.this.resolveURL(url);
            if (result == null) sendError(404, "Not Found", "File '" + url + "' not found.");
            return result;
        }

        private void parseHTTPHeaders() throws IOException {
            buildEnvironment();
            if (headerRead) return;
            String line, header;
            StringBuffer text = new StringBuffer();
            int pos;
            while (null != (line = readLine(inputStream))) {
                if (line.length() == 0) break;
                header = parseHeader(line, text).toUpperCase().replace('-', '_');
                if (header.equals("CONTENT_TYPE") || header.equals("CONTENT_LENGTH")) env.put(header, text.toString()); else env.put("HTTP_" + header, text.toString());
            }
            headerRead = true;
        }

        /** parse name=value pairs in the body of a server shortcut,
         * and add them to the query string */
        private void parseLinkParameters(BufferedReader linkContents) throws IOException {
            StringBuffer queryString = new StringBuffer();
            String param, name, val;
            int equalsPos;
            while ((param = linkContents.readLine()) != null) {
                equalsPos = param.indexOf('=');
                if (equalsPos == 0 || param.length() == 0) continue; else if (equalsPos == -1) queryString.append("&").append(URLEncoder.encode(param.trim())); else {
                    name = param.substring(0, equalsPos);
                    val = param.substring(equalsPos + 1);
                    if (val.startsWith("=")) val = val.substring(1);
                    queryString.append("&").append(URLEncoder.encode(name)).append("=").append(URLEncoder.encode(val));
                }
            }
            if (queryString.length() != 0) {
                String existingQuery = (String) env.get("QUERY_STRING");
                if (existingQuery != null) queryString.append("&").append(existingQuery);
                query = queryString.toString().substring(1);
                env.put("QUERY_STRING", query);
            }
        }

        /** Handle a server shortcut. */
        private void serveLink(URLConnection conn) throws IOException, TinyWebThreadException {
            BufferedReader linkContents = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String redirectLocation = linkContents.readLine();
            if (redirectLocation.indexOf('?') != -1 || redirectLocation.indexOf("//") != -1) sendError(500, "Internal Error", "Malformed server shortcut.");
            parseHTTPHeaders();
            parseLinkParameters(linkContents);
            linkContents.close();
            if (redirectLocation.startsWith(CGI_LINK_PREFIX)) {
                String className = redirectLocation.substring(CGI_LINK_PREFIX.length()).trim();
                serveCGI(getScript(conn, className));
            } else {
                try {
                    URL contextURL = new URL("http://unimportant/" + path);
                    URL uriURL = new URL(contextURL, redirectLocation);
                    path = uriURL.getFile().substring(1);
                } catch (IOException ioe) {
                    sendError(500, "Internal Error", "Malformed server shortcut.");
                }
                serveRequest();
            }
        }

        /** Handle a cgi-like http request. */
        private void serveCGI(URLConnection conn) throws IOException, TinyWebThreadException {
            serveCGI(getScript(conn));
        }

        /** Handle a cgi-like http request. */
        private void serveCGI(TinyCGI script) throws IOException, TinyWebThreadException {
            parseHTTPHeaders();
            OutputStream cgiOut = null;
            File tempFile = null;
            try {
                if (script == null) sendError(500, "Internal Error", "Couldn't load script.");
                if (script instanceof TinyCGIHighVolume) {
                    tempFile = File.createTempFile("cgi", null);
                    cgiOut = new FileOutputStream(tempFile);
                } else {
                    cgiOut = new ByteArrayOutputStream();
                }
                script.service(inputStream, cgiOut, env);
            } catch (Exception cgie) {
                this.exceptionEncountered = cgie;
                if (tempFile != null) tempFile.delete();
                if (clientSocket == null) {
                    return;
                } else if (cgie instanceof TinyCGIException) {
                    TinyCGIException tce = (TinyCGIException) cgie;
                    sendError(tce.getStatus(), tce.getTitle(), tce.getText(), tce.getOtherHeaders());
                } else {
                    StringWriter w = new StringWriter();
                    cgie.printStackTrace(new PrintWriter(w));
                    sendError(500, "CGI Error", "Error running script: " + "<PRE>" + w.toString() + "</PRE>");
                }
            } finally {
                if (script != null) doneWithScript(script);
            }
            InputStream cgiResults = null;
            long totalSize = 0;
            if (tempFile == null) {
                byte[] results = ((ByteArrayOutputStream) cgiOut).toByteArray();
                totalSize = results.length;
                cgiResults = new ByteArrayInputStream(results);
            } else {
                cgiOut.close();
                totalSize = tempFile.length();
                cgiResults = new FileInputStream(tempFile);
            }
            String contentType = null, statusString = "OK", line, header;
            StringBuffer otherHeaders = new StringBuffer();
            StringBuffer text = new StringBuffer();
            int status = 200;
            int headerLength = 0;
            while (true) {
                line = readLine(cgiResults, true);
                headerLength += line.length();
                if (line.charAt(0) == '\r' || line.charAt(0) == '\n') break;
                header = parseHeader(line, text);
                if (header.toUpperCase().equals("STATUS")) {
                    statusString = text.toString();
                    status = Integer.parseInt(statusString.substring(0, 3));
                    statusString = statusString.substring(4);
                } else if (header.toUpperCase().equals("CONTENT-TYPE")) contentType = text.toString(); else {
                    if (header.toUpperCase().equals("LOCATION")) status = 302;
                    otherHeaders.append(header).append(": ").append(text.toString()).append(CRLF);
                }
            }
            sendHeaders(status, statusString, contentType, totalSize - headerLength, -1, otherHeaders.toString());
            byte[] buf = new byte[2048];
            int bytesRead;
            while ((bytesRead = cgiResults.read(buf)) != -1) outputStream.write(buf, 0, bytesRead);
            outputStream.flush();
            try {
                cgiResults.close();
                if (tempFile != null) tempFile.delete();
            } catch (IOException ioe) {
            }
        }

        /** Create an environment for use by a CGI script or a server
         *  preprocessed file */
        private void buildEnvironment() {
            if (env != null) return;
            env = new HashMap(DEFAULT_ENV);
            env.put("SERVER_PROTOCOL", protocol);
            env.put("REQUEST_METHOD", method);
            env.put("PATH_INFO", id);
            if (id != null && id.startsWith("/")) {
                env.put("PATH_TRANSLATED", URLDecoder.decode(id));
                env.put("SCRIPT_PATH", id + "//" + path);
            } else {
                if (data != null) env.put("PATH_TRANSLATED", data.getPath(id));
                env.put("SCRIPT_PATH", "/" + id + "/" + path);
            }
            env.put("SCRIPT_NAME", "/" + path);
            env.put("REQUEST_URI", uri);
            env.put("QUERY_STRING", query);
            if (clientSocket != null) {
                env.put("REMOTE_PORT", Integer.toString(clientSocket.getPort()));
                InetAddress addr = clientSocket.getInetAddress();
                env.put("REMOTE_HOST", addr.getHostName());
                env.put("REMOTE_ADDR", addr.getHostAddress());
                addr = clientSocket.getLocalAddress();
                env.put("SERVER_NAME", addr.getHostName());
                env.put("SERVER_ADDR", addr.getHostAddress());
            }
            env.put(TinyCGI.TINY_WEB_SERVER, TinyWebServer.this);
        }

        private class CGIPool extends ResourcePool {

            Class cgiClass;

            CGIPool(String name, Class c) throws IllegalArgumentException {
                super(name);
                if (!TinyCGI.class.isAssignableFrom(c)) throw new IllegalArgumentException(c.getName() + " does not implement pspdash.TinyCGI");
                cgiClass = c;
            }

            protected Object createNewResource() {
                try {
                    return cgiClass.newInstance();
                } catch (Throwable t) {
                    return null;
                }
            }
        }

        /** Get an appropriate CGILoader for loading a class from the given
         * connection.
         */
        private CGILoader getLoader(URLConnection conn) {
            String path = conn.getURL().toExternalForm();
            int end = path.lastIndexOf('/');
            path = path.substring(0, end + 1);
            synchronized (cgiLoaderMap) {
                CGILoader result = (CGILoader) cgiLoaderMap.get(path);
                if (result == null) {
                    ClassLoader parent = getParentClassLoader(path);
                    if (parent == null) result = new CGILoader(path); else result = new CGILoader(path, parent);
                    cgiLoaderMap.put(path, result);
                }
                return result;
            }
        }

        /** Get a TinyCGI script for a given uri path.
         * @param conn the URLConnection to the ".class" file for the script.
         *   TinyCGI scripts must be java classes in the root package (like
         *   the servlets API).
         * @return an instantiated TinyCGI script, or null on error.
         */
        private TinyCGI getScript(URLConnection conn) {
            return getScript(conn, null);
        }

        private TinyCGI getScript(URLConnection conn, String className) {
            CGIPool pool = null;
            synchronized (cgiCache) {
                pool = (CGIPool) cgiCache.get(path);
                if (pool == null) try {
                    CGILoader cgiLoader = getLoader(conn);
                    Class clz = null;
                    if (className == null) clz = cgiLoader.loadFromConnection(conn); else clz = cgiLoader.loadClass(className);
                    pool = new CGIPool(path, clz);
                    cgiCache.put(path, pool);
                } catch (Throwable t) {
                    return null;
                }
            }
            return (TinyCGI) pool.get();
        }

        private void doneWithScript(Object script) {
            CGIPool pool = (CGIPool) cgiCache.get(path);
            if (pool != null) pool.release(script);
        }

        /** Parse an HTTP header (of the form "Header: value").
         *
         *  @param line The HTTP header line.
         *  @param value The value of the header found will be placed in
         *               this StringBuffer.
         *  @return The name of the header found.
         */
        private String parseHeader(String line, StringBuffer value) {
            int len = line.length();
            int pos = 0;
            while (pos < len && ": \t".indexOf(line.charAt(pos)) == -1) pos++;
            String result = line.substring(0, pos);
            while (pos < len && ": \t".indexOf(line.charAt(pos)) != -1) pos++;
            value.setLength(0);
            int end = line.indexOf('\r', pos);
            if (end == -1) end = line.indexOf('\n', pos);
            if (end == -1) end = line.length();
            value.append(line.substring(pos, end));
            return result;
        }

        /** Serve a plain HTTP request */
        private void servePlain(URLConnection conn, String mime_type) throws TinyWebThreadException, IOException {
            byte[] buffer = new byte[SCAN_BUF_SIZE];
            InputStream content = conn.getInputStream();
            int numBytes = -1;
            if (content == null) sendError(500, "Internal Error", "Couldn't read file.");
            boolean translate = Translator.isTranslating() && !nonTranslatedPath(path);
            boolean preprocess = SERVER_PARSED_MIME_TYPE.equals(mime_type);
            if (mime_type == null || (preprocess && translate) || mime_type.startsWith("text/")) {
                PushbackInputStream pb = new PushbackInputStream(content, SCAN_BUF_SIZE + 1);
                numBytes = pb.read(buffer);
                if (numBytes < 1) sendError(500, "Internal Error", "Couldn't read file.");
                pb.unread(buffer, 0, numBytes);
                content = pb;
                if (mime_type == null) mime_type = getDefaultMimeType(buffer, numBytes);
                if ((preprocess && translate) || mime_type.startsWith("text/")) {
                    String scanBuf = new String(buffer, 0, numBytes, DASH_CHARSET);
                    translate = translate && mime_type.startsWith("text/html") && !containsNoTranslateTag(scanBuf);
                    preprocess = preprocess || containsServerParseOverride(scanBuf);
                }
            }
            if (preprocess) {
                if (SERVER_PARSED_MIME_TYPE.equals(mime_type)) mime_type = "text/html";
                servePreprocessedFile(content, translate, mime_type);
            } else if (translate && mime_type.startsWith("text/html")) serveTranslatedFile(content, mime_type); else {
                discardHeader();
                sendHeaders(200, "OK", mime_type, conn.getContentLength(), conn.getLastModified(), null);
                while (-1 != (numBytes = content.read(buffer))) {
                    outputStream.write(buffer, 0, numBytes);
                }
                outputStream.flush();
                content.close();
            }
        }

        private boolean containsServerParseOverride(String scanBuf) {
            return (scanBuf.indexOf(SERVER_PARSE_OVERRIDE) != -1);
        }

        private static final String SERVER_PARSE_OVERRIDE = "<!--#server-parsed";

        private boolean containsNoTranslateTag(String scanBuf) {
            return (scanBuf.indexOf(NO_TRANSLATE_TAG) != -1);
        }

        private static final String NO_TRANSLATE_TAG = "<!--#do-not-translate";

        private boolean nonTranslatedPath(String path) {
            return path.startsWith("help/");
        }

        private String setMimeTypeCharset(String type, String charset) {
            int pos = type.toLowerCase().indexOf("charset=");
            if (pos == -1) return type + "; charset=" + charset;
            return type.substring(pos + 8) + charset;
        }

        /** Serve up a server-parsed html file. */
        private void servePreprocessedFile(URLConnection conn, String mimeType) throws TinyWebThreadException, IOException {
            String content = preprocessTextFile(conn.getInputStream(), false);
            byte[] bytes = content.getBytes(outputCharset);
            String contentType = setMimeTypeCharset(mimeType, outputCharset);
            sendHeaders(200, "OK", contentType, bytes.length, -1, null);
            outputStream.write(bytes);
        }

        /** Serve up a server-parsed html file. */
        private void servePreprocessedFile(InputStream in, boolean translate, String mimeType) throws TinyWebThreadException, IOException {
            String content = preprocessTextFile(in, translate);
            byte[] bytes = content.getBytes(outputCharset);
            String contentType = setMimeTypeCharset(mimeType, outputCharset);
            sendHeaders(200, "OK", contentType, bytes.length, -1, null);
            outputStream.write(bytes);
        }

        private String preprocessTextFile(InputStream in, boolean translate) throws TinyWebThreadException, IOException {
            byte[] rawContent = slurpContents(in, true);
            String content = new String(rawContent, DASH_CHARSET);
            if (translate) content = Translator.translate(content);
            parseHTTPHeaders();
            HTMLPreprocessor p = new HTMLPreprocessor(TinyWebServer.this, data, env);
            return p.preprocess(content);
        }

        private void serveTranslatedFile(InputStream content, String mime_type) throws IOException {
            discardHeader();
            Reader fileReader = new InputStreamReader(content, DASH_CHARSET);
            Reader translatedReader = Translator.translate(fileReader);
            Writer output = new OutputStreamWriter(outputStream, outputCharset);
            mime_type = setMimeTypeCharset(mime_type, outputCharset);
            sendHeaders(200, "OK", mime_type, -1, -1, null);
            char[] buf = new char[4096];
            int numChars;
            while (-1 != (numChars = translatedReader.read(buf))) {
                output.write(buf, 0, numChars);
            }
            output.flush();
            content.close();
        }

        /** read and discard the rest of the request header from inputStream */
        private void discardHeader() throws IOException {
            if (headerRead) return;
            String line;
            while (null != (line = readLine(inputStream))) if (line.length() == 0) break;
            headerRead = true;
        }

        /** ensure that requests are originating from the local machine. */
        private void checkIP() throws TinyWebThreadException, IOException {
            if (clientSocket == null) return;
            if (path.indexOf('/') == -1 || path.startsWith("Images/")) return;
            InetAddress remoteIP = clientSocket.getInetAddress();
            if (remoteIP.equals(LOOPBACK_ADDR) || remoteIP.equals(LOCAL_HOST_ADDR)) return;
            parseHTTPHeaders();
            String path = (String) env.get("PATH_TRANSLATED");
            if (path == null) path = "";
            do {
                if (checkPassword(path)) return;
                path = chopPath(path);
            } while (path != null);
            if (!allowRemoteConnections) sendErrorOrAuth(403, "Forbidden", "Not accepting " + "requests from remote IP addresses .");
        }

        private String chopPath(String path) {
            if (path == null) return null;
            int slashPos = path.lastIndexOf('/');
            if (slashPos == -1) return null; else return path.substring(0, slashPos);
        }

        private boolean checkPassword(String path) throws TinyWebThreadException {
            String dataName = path + "/_Password_";
            Object value = data.getValue(dataName);
            if (value == null) return false;
            if (value instanceof DoubleData) {
                if (0 == ((DoubleData) value).getInteger()) sendErrorOrAuth(403, "Forbidden", "Not accepting " + "requests from remote IP addresses ."); else return true;
            }
            if (value instanceof StringData) {
                String val = ((StringData) value).getString();
                sawPassword = true;
                if (getUserCredential() != null && val.indexOf(getUserCredential()) != -1) {
                    env.put("AUTH_USER", getAuthUser());
                    return true;
                }
                if (getGuestCredential() != null && val.indexOf(getGuestCredential()) != -1) {
                    env.put("AUTH_USER", "anonymous");
                    return true;
                }
            }
            return false;
        }

        private boolean sawPassword = false;

        private void sendErrorOrAuth(int status, String title, String text) throws TinyWebThreadException {
            if (sawPassword) sendError(401, "Unauthorized", "Authorization required.", "WWW-Authenticate: Basic realm=\"Process " + "Dashboard\"" + CRLF); else sendError(status, title, text, null);
        }

        private String userCredential = null;

        private String guestCredential = null;

        private String wwwUser = null;

        private String getAuthUser() {
            authenticate();
            return wwwUser;
        }

        private String getUserCredential() {
            authenticate();
            return userCredential;
        }

        private String getGuestCredential() {
            authenticate();
            return guestCredential;
        }

        private void authenticate() {
            if (wwwUser != null) return;
            String credentials = (String) env.get("HTTP_AUTHORIZATION");
            if (credentials == null) return;
            StringTokenizer tok = new StringTokenizer(credentials);
            try {
                tok.nextToken();
                credentials = Base64.decode(tok.nextToken());
            } catch (Exception e) {
                return;
            }
            int colonPos = credentials.indexOf(':');
            if (colonPos == -1) return;
            wwwUser = credentials.substring(0, colonPos);
            String wwwPassword = credentials.substring(colonPos + 1);
            String md5hash;
            synchronized (md5) {
                md5.Init();
                md5.Update(wwwPassword);
                md5hash = md5.asHex();
            }
            userCredential = wwwUser + ":" + md5hash;
            guestCredential = "*:" + md5hash;
        }

        /** Send an HTTP error page.
         *
         * @throws TinyWebThreadException automatically and unequivocally
         *    after printing the error page.  (This greatly simplifies
         *    TinyWebThread control logic.  Anytime an exception or
         *    other error is found, just call this method;  an error page
         *    will be generated, and then an exception will be thrown.
         *    TinyWebThreadExceptions are caught in only one place, at
         *    the top level run() method.)
         */
        private void sendError(int status, String title, String text) throws TinyWebThreadException {
            sendError(status, title, text, null);
        }

        private void sendError(int status, String title, String text, String otherHeaders) throws TinyWebThreadException {
            TinyWebThreadException result = new TinyWebThreadException();
            try {
                if (exceptionEncountered == null) exceptionEncountered = result;
                discardHeader();
                sendHeaders(status, title, "text/html", -1, -1, otherHeaders);
                headerOut.write("<HTML><HEAD><TITLE>" + status + " " + title + "</TITLE></HEAD>\n<BODY BGCOLOR=\"#cc9999\"><H4>" + status + " " + title + "</H4>\n" + text + "\n" + "</BODY></HTML>\n");
                headerOut.flush();
            } catch (IOException ioe) {
            }
            throw result;
        }

        private boolean headersSent = false;

        private void sendHeaders(int status, String title, String mimeType, long length, long mod, String otherHeaders) throws IOException {
            if (headersSent) return;
            headersSent = true;
            Date now = new Date();
            headerOut.write(PROTOCOL + " " + status + " " + title + CRLF);
            headerOut.write("Server: localhost" + CRLF);
            headerOut.write("Date: " + dateFormat.format(now) + CRLF);
            if (mimeType != null) headerOut.write("Content-Type: " + mimeType + CRLF);
            if (mod > 0) headerOut.write("Last-Modified: " + dateFormat.format(new Date(mod)) + CRLF);
            if (length >= 0) headerOut.write("Content-Length: " + length + CRLF);
            headerOut.write(startupTimestampHeader + CRLF);
            if (otherHeaders != null) headerOut.write(otherHeaders);
            headerOut.write("Connection: close" + CRLF + CRLF);
            headerOut.flush();
        }

        private String getMimeTypeFromName(String name) {
            int pos = name.lastIndexOf('.');
            if (pos >= 0) {
                String suffix = name.substring(pos).toLowerCase();
                if (suffix.equals(".class") && name.indexOf("/IE/") == -1 && name.indexOf("/NS/") == -1) return CGI_MIME_TYPE; else return (String) mimeTypes.get(suffix);
            } else return null;
        }

        /** Check to see if the data is text or binary, and return the
         *  appropriate default mime type. */
        private String getDefaultMimeType(byte[] buffer, int numBytes) {
            while (numBytes-- > 0) if (Character.isISOControl((char) buffer[numBytes]) && "\t\r\n\f".indexOf((char) buffer[numBytes]) == -1) return DEFAULT_BINARY_MIME_TYPE;
            return DEFAULT_TEXT_MIME_TYPE;
        }
    }

    private boolean isDirectory(URL u) {
        String urlString = u.toString();
        if (!urlString.startsWith("file:/")) return false;
        String filename = urlString.substring(5);
        filename = URLDecoder.decode(filename);
        File file = new File(filename);
        return file.isDirectory();
    }

    /** Canonicalize a path through the removal of directory changes
     * made by occurences of &quot;..&quot; and &quot;.&quot;.
     *
     * @return a canonical path, or null on error.
     */
    private static String canonicalizePath(String path) {
        if (path == null) return null;
        path = path.trim();
        int pos, beg;
        while (true) {
            if (path.startsWith("../") || path.startsWith("/../")) return null; else if (path.startsWith("./")) path = path.substring(2); else if ((pos = path.indexOf("/./")) != -1) path = path.substring(0, pos) + path.substring(pos + 2); else if (path.endsWith("/.")) path = path.substring(0, path.length() - 2); else if ((pos = path.indexOf("/../", 1)) != -1) {
                beg = path.lastIndexOf('/', pos - 1);
                if (beg == -1) path = path.substring(pos + 4); else path = path.substring(0, beg) + path.substring(pos + 3);
            } else if (path.endsWith("/..")) {
                beg = path.lastIndexOf('/', path.length() - 4);
                if (beg == -1) return null; else path = path.substring(0, beg + 1);
            } else return path;
        }
    }

    private URLConnection resolveURL(String url) {
        url = canonicalizePath(url);
        if (url == null) return null;
        URL u;
        URLConnection result;
        for (int i = 0; i < roots.length; i++) try {
            u = new URL(roots[i], url);
            if (isDirectory(u)) continue;
            result = u.openConnection();
            result.connect();
            return result;
        } catch (IOException ioe) {
        }
        if (!url.endsWith(LINK_SUFFIX)) return resolveURL(url + LINK_SUFFIX);
        return null;
    }

    /** Calculate the user credential that would work for an http
     * Authorization field.
     */
    public static String calcCredential(String user, String password) {
        return "Basic " + Base64.encode(user + ":" + password);
    }

    /** Save a password setting in the data repository.
     *
     * Adjusts the password settings for the given prefix.
     * Normally, adds the username/password pair to the password table,
     *     or changes the existing password for that username.
     * If user is null and password is non-null, discards all password
     *     information and marks this node as "forbidden".
     * If user is null and password is null, discards all password
     *     information and marks this node as "unprotected".
     */
    static void setPassword(DataRepository data, String prefix, String user, String password) {
        String dataName = data.createDataName(prefix, "_Password_");
        if (user == null) {
            DoubleData val;
            if (password == null) val = ImmutableDoubleData.TRUE; else val = ImmutableDoubleData.FALSE;
            data.putValue(dataName, val);
            return;
        }
        Object val = data.getSimpleValue(dataName);
        HashMap passwords = new HashMap();
        if (val instanceof StringData) try {
            StringTokenizer tok = new StringTokenizer(((StringData) val).format(), ";");
            while (tok.hasMoreTokens()) {
                String credential = tok.nextToken();
                int colonPos = credential.indexOf(':');
                String credUser = credential.substring(0, colonPos);
                String passHash = credential.substring(colonPos + 1);
                passwords.put(credUser, passHash);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        MD5 md5 = new MD5();
        md5.Init();
        md5.Update(password);
        passwords.put(user, md5.asHex());
        StringBuffer passwordList = new StringBuffer();
        Iterator i = passwords.entrySet().iterator();
        Map.Entry e;
        while (i.hasNext()) {
            e = (Map.Entry) i.next();
            passwordList.append(e.getKey()).append(":").append(e.getValue()).append(";");
        }
        data.putValue(dataName, StringData.create(passwordList.toString()));
    }

    /** Encode HTML entities in the given string, and return the result. */
    public static String encodeHtmlEntities(String str) {
        str = StringUtils.findAndReplace(str, "&", "&amp;");
        str = StringUtils.findAndReplace(str, "<", "&lt;");
        str = StringUtils.findAndReplace(str, ">", "&gt;");
        str = StringUtils.findAndReplace(str, "\"", "&quot;");
        return str;
    }

    public static String urlEncodePath(String path) {
        path = URLEncoder.encode(path);
        path = StringUtils.findAndReplace(path, "%2F", "/");
        path = StringUtils.findAndReplace(path, "%2f", "/");
        return path;
    }

    public static String getHostName() {
        String result = Settings.getVal("http.hostname");
        if (result == null || result.length() == 0) try {
            result = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            result = "localhost";
        }
        return result;
    }

    /** Utility routine: slurp an entire file from an InputStream. */
    public static byte[] slurpContents(InputStream in, boolean close) throws IOException {
        byte[] result = null;
        ByteArrayOutputStream slurpBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) slurpBuffer.write(buffer, 0, bytesRead);
        result = slurpBuffer.toByteArray();
        if (close) try {
            in.close();
        } catch (IOException ioe) {
        }
        return result;
    }

    /** Utility routine: readLine from an InputStream.
     *
     * This is needed because the only readLine method in the Java library
     * is in the BufferedReader class.  A BufferedReader will likely grab
     * more bytes than we necessarily want it to.
     *
     * Although this method is not performing any character encoding,
     * Hopefully we're okay because we're just parsing plaintext HTTP headers.
     */
    static String readLine(InputStream in) throws IOException {
        return readLine(in, false);
    }

    static String readLine(InputStream in, boolean keepCRLF) throws IOException {
        StringBuffer result = new StringBuffer();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') {
                if (keepCRLF) result.append((char) c);
                break;
            } else if (c == '\r') {
                if (keepCRLF) result.append((char) c);
            } else {
                result.append((char) c);
            }
        }
        return result.toString();
    }

    /** Utility routine: readLine from a byte array. The carraige return
     * and linefeed that terminate the line are returned as part of the
     * final result.
     *
     * Although this method is not performing any character encoding,
     * Hopefully we're okay because we're just parsing plaintext HTTP headers.
     */
    static String readLine(byte[] buf, int beg) throws IOException {
        int p = beg;
        while (p < buf.length && buf[p] != '\r' && buf[p] != '\n') p++;
        if (p < buf.length && buf[p] == '\r') p++;
        if (p < buf.length && buf[p] == '\n') p++;
        return new String(buf, beg, p - beg);
    }

    /** Perform an internal http request for the caller.
     *
     * @param uri the absolute uri of a resource on this server (e.g.
     *     <code>/0980/help/about.htm?foo=bar</code>)
     * @param skipHeaders if true, the generated response headers are discarded
     * @return the response generated by performing the http request.
     */
    public byte[] getRequest(String uri, boolean skipHeaders) throws IOException {
        if (internalRequestNesting > 50) throw new IOException("Infinite recursion - aborting.");
        synchronized (this) {
            internalRequestNesting++;
        }
        TinyWebThread t = new TinyWebThread(uri);
        byte[] result = null;
        try {
            result = t.getOutput();
        } finally {
            synchronized (this) {
                internalRequestNesting--;
            }
            if (t != null) t.dispose();
        }
        if (!skipHeaders) return result; else {
            int a = 0, b = 1, c = 2, d = 3;
            do {
                if (result[a] == '\r' && result[b] == '\n' && result[c] == '\r' && result[d] == '\n') break;
                a++;
                b++;
                c++;
                d++;
            } while (d < result.length);
            byte[] contents = new byte[result.length - d - 1];
            System.arraycopy(result, d + 1, contents, 0, contents.length);
            return contents;
        }
    }

    private volatile int internalRequestNesting = 0;

    /** Perform an internal http request for the caller.
     * @param context the uri of an original request within this web server
     * @param uri a uri to fetch the contents of.  If it does not begin with
     *     a slash, it will be interpreted relative to <code>context</code>.
     * @param skipHeaders if true, the generated response headers are discarded
     * @return the response generated by performing the http request.
     */
    public byte[] getRequest(String context, String uri, boolean skipHeaders) throws IOException {
        if (!uri.startsWith("/")) {
            URL contextURL = new URL("http://unimportant" + context);
            URL uriURL = new URL(contextURL, uri);
            uri = uriURL.getFile();
        }
        return getRequest(uri, skipHeaders);
    }

    /** Perform an internal http request and return raw results.
     *
     * Server-parsed HTML files are returned verbatim, and
     * cgi scripts are returned as binary streams.
     */
    public byte[] getRawRequest(String uri) throws IOException {
        try {
            if (uri.startsWith("/")) uri = uri.substring(1);
            URLConnection conn = resolveURL(uri);
            if (conn == null) return null;
            InputStream in = conn.getInputStream();
            byte[] result = slurpContents(in, true);
            return result;
        } catch (IOException ioe) {
            return null;
        }
    }

    /** Clear the classloader caches, so classes will be reloaded.
     */
    public void clearClassLoaderCaches() {
        addOnLoaderMap.clear();
        cgiLoaderMap.clear();
        cgiCache.clear();
    }

    private void writePackagesToDefaultEnv() {
        Iterator i = DEFAULT_ENV.keySet().iterator();
        while (i.hasNext()) if (((String) i.next()).startsWith(PACKAGE_ENV_PREFIX)) i.remove();
        i = TemplateLoader.getPackages().iterator();
        while (i.hasNext()) {
            DashPackage pkg = (DashPackage) i.next();
            DEFAULT_ENV.put(PACKAGE_ENV_PREFIX + pkg.id, pkg.version);
        }
    }

    /** Return the number of the port this server is listening on. */
    public int getPort() {
        return port;
    }

    /** Return the socket we opened for data connections. */
    ServerSocket getDataSocket() {
        return dataSocket;
    }

    /** Return the startup timestamp for this server. */
    public String getTimestamp() {
        return startupTimestamp;
    }

    private void init(int port, URL[] roots) throws IOException {
        this.roots = roots;
        startupTimestamp = Long.toString((new Date()).getTime());
        startupTimestampHeader = TIMESTAMP_HEADER + ": " + startupTimestamp;
        InetAddress listenAddress = null;
        if ("never".equalsIgnoreCase(Settings.getVal("http.allowRemote"))) listenAddress = LOOPBACK_ADDR;
        while (serverSocket == null) try {
            dataSocket = new ServerSocket(port - 1, 50, listenAddress);
            serverSocket = new ServerSocket(port, 50, listenAddress);
        } catch (IOException ioex) {
            if (dataSocket != null) {
                try {
                    dataSocket.close();
                } catch (IOException ioe) {
                }
                dataSocket = null;
            }
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException ioe) {
                }
                serverSocket = null;
            }
            port += 2;
        }
        this.port = port;
        DEFAULT_ENV.put("SERVER_PORT", Integer.toString(port));
        writePackagesToDefaultEnv();
        String charsetName = Settings.getVal("http.charset");
        if (charsetName != null && charsetName.length() > 0) try {
            "test".getBytes(charsetName);
            OUTPUT_CHARSET = charsetName;
            TinyCGIBase.setDefaultCharset(OUTPUT_CHARSET);
        } catch (UnsupportedEncodingException uee) {
        }
    }

    /**
     * Run a tiny web server on the given port, serving up resources
     * out of the given package within the class path.
     *
     * Serving up resources out of the classpath seems like a nice
     * idea, since it allows html pages, etc to be JAR-ed up and
     * invisible to the user.
     */
    TinyWebServer(int port, String path) throws IOException {
        if (path == null || path.length() == 0) throw new IOException("Path must be specified");
        if (path.startsWith("/")) path = path.substring(1);
        if (!path.endsWith("/")) path = path + "/";
        Enumeration e = getClass().getClassLoader().getResources(path);
        Vector v = new Vector();
        while (e.hasMoreElements()) v.addElement(e.nextElement());
        int i = v.size();
        URL[] roots = new URL[i];
        while (i-- > 0) roots[i] = (URL) v.elementAt(i);
        init(port, roots);
    }

    /**
     * Run a tiny web server on the given port, serving up files out
     * of the given directory.
     */
    TinyWebServer(String directoryToServe, int port) throws IOException {
        File rootDir = new File(directoryToServe);
        if (!rootDir.isDirectory()) throw new IOException("Not a directory: " + directoryToServe);
        URL[] roots = new URL[1];
        roots[0] = rootDir.toURL();
        init(port, roots);
    }

    /**
     * Run a tiny web server on the given port, serving up resources
     * out of the given list of template search URLs.
     */
    TinyWebServer(int port, URL[] roots) throws IOException {
        init(port, roots);
    }

    void setRoots(URL[] roots) {
        this.roots = roots;
        clearClassLoaderCaches();
        writePackagesToDefaultEnv();
    }

    void setProps(PSPProperties props) {
        if (props == null) DEFAULT_ENV.remove(TinyCGI.PSP_PROPERTIES); else DEFAULT_ENV.put(TinyCGI.PSP_PROPERTIES, props);
    }

    void setData(DataRepository data) {
        this.data = data;
        if (data == null) DEFAULT_ENV.remove(TinyCGI.DATA_REPOSITORY); else DEFAULT_ENV.put(TinyCGI.DATA_REPOSITORY, data);
    }

    void setCache(ObjectCache cache) {
        if (cache == null) DEFAULT_ENV.remove(TinyCGI.OBJECT_CACHE); else DEFAULT_ENV.put(TinyCGI.OBJECT_CACHE, cache);
    }

    void allowRemoteConnections(String setting) {
        this.allowRemoteConnections = "true".equalsIgnoreCase(setting);
    }

    private volatile boolean isRunning;

    public void run() {
        acceptRequests(serverSocket);
    }

    /** handle http requests. */
    protected void acceptRequests(ServerSocket serverSocket) {
        Socket clientSocket = null;
        TinyWebThread serverThread = null;
        if (serverSocket == null) return;
        isRunning = true;
        while (isRunning) try {
            clientSocket = serverSocket.accept();
            serverThread = new TinyWebThread(clientSocket);
            serverThreads.addElement(serverThread);
            serverThread.start();
        } catch (IOException e) {
        }
        while (serverThreads.size() > 0) {
            serverThread = (TinyWebThread) serverThreads.remove(0);
            serverThread.close();
        }
        close(serverSocket);
    }

    private class SecondaryServerSocket extends Thread {

        ServerSocket secondaryServerSocket;

        public SecondaryServerSocket(int port) throws IOException {
            InetAddress listenAddress = serverSocket.getInetAddress();
            secondaryServerSocket = new ServerSocket(port, 50, listenAddress);
            dataSocket = new ServerSocket(port - 1, 50, listenAddress);
            setDaemon(true);
        }

        public void run() {
            acceptRequests(secondaryServerSocket);
            secondaryServerSocket = null;
        }
    }

    /** Start listening for connections on an additional port */
    void addExtraPort(int port) throws IOException {
        SecondaryServerSocket s = new SecondaryServerSocket(port);
        secondaryServerSockets.add(s);
        s.start();
        this.port = port;
    }

    private Vector secondaryServerSockets = new Vector();

    /** Stop the web server. */
    public void quit() {
        isRunning = false;
        this.interrupt();
        close(this.serverSocket);
        Iterator i = secondaryServerSockets.iterator();
        while (i.hasNext()) ((Thread) i.next()).interrupt();
        this.serverSocket = null;
    }

    private synchronized void close(ServerSocket serverSocket) {
        if (serverSocket != null) try {
            serverSocket.close();
        } catch (IOException e2) {
        }
    }
}
