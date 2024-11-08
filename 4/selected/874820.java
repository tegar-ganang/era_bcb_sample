package rabbit.proxy;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import rabbit.cache.NCacheEntry;
import rabbit.filter.HTTPFilter;
import rabbit.filter.IPAccessFilter;
import rabbit.handler.BaseHandler;
import rabbit.handler.Handler;
import rabbit.handler.HandlerFactory;
import rabbit.handler.MultiPartHandler;
import rabbit.http.HTTPDateParser;
import rabbit.http.HTTPHeader;
import rabbit.io.HTTPInputStream;
import rabbit.io.HTTPOutputStream;
import rabbit.io.MultiOutputStream;
import rabbit.io.WebConnection;
import rabbit.meta.MetaHandler;
import rabbit.util.Coder;
import rabbit.util.Logger;
import rabbit.util.RestartableThread;

/** This is the class that handles one connection 
 *  And make sure the content is delivered to the client.
 *  It reads the request and get the data (from the cache or the web), 
 *  and filters it and sends the data to the client. If Keepalive is suitable
 *  it reads the next request.
 */
public class Connection extends RestartableThread {

    /** The proxy we are running a connection for. */
    private Proxy proxy;

    /** The current status of this connection. */
    private String status;

    private Date started = new Date();

    private SocketChannel socket = null;

    private HTTPInputStream in = null;

    private HTTPOutputStream client = null;

    private MultiOutputStream out = null;

    private boolean keepalive = true;

    private boolean meta = false;

    private int requests = 0;

    private boolean chunk = true;

    private boolean mayusecache = true;

    private boolean maycache = true;

    private boolean mayfilter = true;

    private boolean mustRevalidate = false;

    private boolean addedINM = false;

    private boolean addedIMS = false;

    /** If the user has authenticated himself */
    private String username = null;

    private String password = null;

    private String requestline = null;

    private String statuscode = null;

    private String extrainfo = null;

    private String contentlength = null;

    private static long counter = 0;

    private static Map<Class<? extends MetaHandler>, MetaHandler> metaHandlers = new HashMap<Class<? extends MetaHandler>, MetaHandler>();

    private StandardResponseHeaders responseHandler = new StandardResponseHeaders(this);

    /** Create a new Connection.
     *  You need to call setsocket on this connection to make it do stuff.
     * @param proxy the Proxy to handle connections for.
     */
    public Connection(Proxy proxy) {
        super("RabbIT2: " + counter++);
        this.proxy = proxy;
        status = "created";
    }

    /** Give this connection a socket to handle
     * @param socket the Socket that is requesting service.
     */
    public void setSocket(SocketChannel socket) {
        this.socket = socket;
        keepalive = true;
        meta = false;
        requests = 0;
        status = "setting up socket";
        if (!checkIPAccess(socket.socket())) {
            proxy.logError(Logger.WARN, "Rejecting access from " + socket.socket().getInetAddress());
            proxy.getCounter().inc("Rejected IP:s");
            status = "rejected";
            closeDown();
        } else {
            status = "starting";
            start();
        }
    }

    private void clearVariables() {
        in = null;
        out = null;
        socket = null;
        status = "idle";
        client = null;
        username = null;
        password = null;
        requestline = null;
    }

    /** Close down nicely.
     */
    private void closeDown() {
        try {
            try {
                if (socket != null && socket.isConnected() && socket.socket().isConnected() && socket.isOpen()) {
                    try {
                        socket.socket().shutdownInput();
                        socket.socket().shutdownOutput();
                    } catch (SocketException e) {
                    }
                }
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    WritableByteChannel wc = out.getChannel();
                    if (wc != null) {
                        wc.close();
                    }
                    out.close();
                }
                if (socket != null) {
                    proxy.getCounter().inc("Client sockets closed");
                    socket.socket().close();
                    socket.close();
                }
            } catch (IOException e) {
                proxy.logError(Logger.WARN, "Problems to close the sockets are usually a bad thing..." + e);
                e.printStackTrace();
            }
            clearVariables();
        } finally {
            proxy.removeConnection(this);
        }
    }

    private void returnSocket() throws IOException {
        proxy.getCounter().inc("Client sockets returned");
        SocketChannel sc = socket;
        clearVariables();
        keepalive = false;
        proxy.returnSocket(sc);
    }

    /** Check if this socket is in the possible range of ip:s being served.
     * @param sock the Socket to check.
     * @return true if the socket is allowed, false if it should be rejected.
     */
    public boolean checkIPAccess(Socket sock) {
        List<IPAccessFilter> v = proxy.getAccessFilters();
        int vsize = v.size();
        for (int i = 0; i < vsize; i++) {
            IPAccessFilter ipf = v.get(i);
            if (ipf.doIPFiltering(sock)) return true;
        }
        return false;
    }

    /** Check if this SSL request is allowed and handle it
     *  accordingly.
     * @param header the SSL request.
     */
    private void checkAndHandleSSL(HTTPHeader header) {
        TunnelHandler.HandleMode hm = TunnelHandler.HandleMode.THREADED;
        SSLHandler sslh = new SSLHandler(getProxy());
        if (sslh.sslIsAllowed(header)) {
            status = "handling SSL connection";
            hm = sslh.handleSSL(header, in, out, this);
        } else {
            HTTPHeader badresponse = responseHandler.get403();
            send(badresponse);
            setKeepalive(false);
            statuscode = badresponse.getStatusCode();
        }
        status = "logging connection";
        proxy.logConnection(this);
        if (hm == TunnelHandler.HandleMode.CHANNELED) clearVariables();
    }

    /** Resets the statuses for this connection.
     */
    private void clearStatuses() {
        status = "handling next request";
        started = new Date();
        username = null;
        password = null;
        chunk = true;
        mayusecache = true;
        maycache = true;
        mayfilter = true;
        mustRevalidate = false;
        addedINM = false;
        addedIMS = false;
        requestline = "?";
        statuscode = "200";
        extrainfo = null;
        contentlength = "-";
    }

    /** Handles the case where the request does not have a valid body.
     * @param header the request made.
     */
    private void handleBadContent(HTTPHeader header, String desc) {
        proxy.logError(Logger.DEBUG, "bad content for:\n" + header);
        doError(400, "Bad content: " + desc);
        statuscode = "400";
        status = "logging connection";
        proxy.logConnection(this);
    }

    /** Filter the request and handle it. 
     * @param header the request
     */
    private void filterAndHandleRequest(HTTPHeader header) {
        HTTPHeader badresponse = filterHTTPIn(proxy.getHTTPInFilters(), header);
        if (badresponse != null) {
            send(badresponse);
            setKeepalive(false);
            statuscode = badresponse.getStatusCode();
        } else {
            status = "Handling request";
            if (getMeta()) {
                setKeepalive(false);
                handleMeta(header);
            } else {
                handleRequest(header);
            }
        }
    }

    private void logExceptionAndSendError(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter sos = new PrintWriter(sw);
        t.printStackTrace(sos);
        proxy.logError(Logger.ERROR, sw.toString());
        HTTPHeader err = responseHandler.get500(t);
        send(err);
    }

    /** Handle the incomming requests for as long as we can keep the connection alive.
     */
    public void doWork() {
        try {
            do {
                clearStatuses();
                requests++;
                if (!setupStreams()) break;
                HTTPHeader header = null;
                try {
                    status = "reading request";
                    header = in.readHTTPHeader();
                    if (header == null) throw new IOException("failed to read header..");
                    String requestVersion = header.getHTTPVersion();
                    if (requestVersion == null) throw new IOException("read a bad header..");
                    requestVersion = requestVersion.toUpperCase();
                    header.addHeader("Via", requestVersion + " RabbIT");
                } catch (InterruptedIOException e) {
                    proxy.logError(Logger.INFO, "Keepalive timed out: " + e);
                    break;
                } catch (IOException e) {
                    proxy.logError(Logger.INFO, "" + socket.socket().getInetAddress().getHostAddress() + " disconnected when reading header\n" + e);
                    break;
                }
                requestline = header.getRequestLine();
                proxy.getCounter().inc("Requests");
                if (SSLHandler.isSSLRequest(header)) {
                    checkAndHandleSSL(header);
                    break;
                }
                String te = header.getHeader("Transfer-Encoding");
                if (te != null && te.equalsIgnoreCase("chunked")) {
                    setMayUseCache(false);
                    setMayCache(false);
                    if (!readChunkedContent(header)) {
                        handleBadContent(header, "bad chunking");
                        break;
                    }
                }
                String ct = null;
                ct = header.getHeader("Content-Type");
                if (header.getContent() == null && (ct == null || !ct.startsWith("multipart/byteranges")) && header.getHeader("Content-length") != null) {
                    setMayUseCache(false);
                    setMayCache(false);
                    if (!readContent(header)) {
                        handleBadContent(header, "bad content length");
                        break;
                    }
                }
                if (ct != null) {
                    if (ct.startsWith("multipart/byteranges")) {
                        setMayUseCache(false);
                        setMayCache(false);
                        if (!readMultipartContent(header, ct)) {
                            handleBadContent(header, "bad multipart");
                            break;
                        }
                    }
                }
                filterAndHandleRequest(header);
                status = "logging connection";
                proxy.logConnection(this);
                if (keepalive && in.available() == 0) {
                    returnSocket();
                }
            } while (keepalive);
        } catch (Throwable t) {
            logExceptionAndSendError(t);
        }
        closeDown();
    }

    /** Set up the streams used for reading request and sending data to and from the client.
     * @return true if all goes well.
     */
    protected boolean setupStreams() {
        status = "setting up streams";
        if (in != null && out != null) return true;
        try {
            in = new HTTPInputStream(socket, true, proxy, proxy.getNLSOHandler());
            client = new HTTPOutputStream(socket);
            out = new MultiOutputStream(client);
        } catch (IOException e) {
            try {
                socket.close();
                socket = null;
            } catch (IOException e2) {
                proxy.logError(Logger.FATAL, "Not being able to close the sockets will lead to system shutdown.");
                System.exit(-4711);
            }
            proxy.logError(Logger.ERROR, "Couldnt get streams from socket: " + e);
            return false;
        }
        return true;
    }

    /** If this request has a body (due to some post or so)
     *  read it in and append it to the request.
     */
    protected boolean readContent(HTTPHeader header) {
        String sl = (String) header.getHeader("Content-length");
        int contentLength = Integer.parseInt(sl.trim());
        byte v[] = new byte[contentLength];
        try {
            in.readFully(v, 0, contentLength);
            header.setContent(v);
        } catch (EOFException e) {
            proxy.logError(Logger.WARN, "Couldnt read all content");
            return false;
        } catch (IOException e) {
            proxy.logError(Logger.INFO, "Unexpected termination:" + e);
            return false;
        }
        return true;
    }

    protected boolean readChunkedContent(HTTPHeader header) {
        try {
            InputStream is = in.getChunkStream();
            StringBuilder sb = new StringBuilder(1024);
            byte[] buf = new byte[1024];
            int read;
            while ((read = is.read(buf)) > 0) {
                String s = new String(buf, 0, read);
                sb.append(s);
            }
            header.setContent(sb.toString());
        } catch (IOException e) {
            proxy.logError(Logger.INFO, "Unexpected termination:" + e);
            return false;
        }
        return true;
    }

    /** If this request has a multipart body read it and append it to the request.
     */
    protected boolean readMultipartContent(HTTPHeader header, String ct) {
        StringTokenizer st = new StringTokenizer(ct, " =\n\r\t;");
        String boundary = null;
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (s.equals("boundary") && st.hasMoreTokens()) boundary = st.nextToken();
        }
        if (boundary == null) return false;
        try {
            StringBuilder sb = new StringBuilder(200);
            String line;
            while ((line = header.readLine(in)) != null) {
                sb.append(line);
                sb.append(header.CRLF);
                if (line.startsWith("--") && line.endsWith("--")) {
                    if (line.substring(2, line.length() - 2).equals(boundary)) break;
                }
            }
            header.setContent(sb.toString());
        } catch (IOException e) {
            proxy.logError(Logger.INFO, "Unexpected termination:" + e);
            return false;
        }
        return true;
    }

    /** Filter the headers using the methods in the vector.
     * @param filters a List with Methods.
     * @param in the request or response header.
     * @return null if all is ok, a HTTPHeader if this request is blocked.
     */
    public HTTPHeader filterHTTPIn(List<HTTPFilter> filters, HTTPHeader in) {
        int fsize = filters.size();
        for (int i = 0; i < fsize; i++) {
            HTTPFilter hf = filters.get(i);
            HTTPHeader badresponse = hf.doHTTPInFiltering(socket.socket(), in, this);
            if (badresponse != null) return badresponse;
        }
        return null;
    }

    /** Filter the headers using the methods in the vector.
     * @param filters a List with Methods.
     * @param in the request or response header.
     * @return null if all is ok, a HTTPHeader if this request is blocked.
     */
    public HTTPHeader filterHTTPOut(List<HTTPFilter> filters, HTTPHeader in) {
        int fsize = filters.size();
        for (int i = 0; i < fsize; i++) {
            HTTPFilter hf = filters.get(i);
            HTTPHeader badresponse = hf.doHTTPOutFiltering(socket.socket(), in, this);
            if (badresponse != null) return badresponse;
        }
        return null;
    }

    /** Send a header to the client.
     * @param header the HTTPHeader to send.
     */
    protected void send(HTTPHeader header) {
        try {
            out.writeHTTPHeader(header);
        } catch (IOException e) {
            setKeepalive(false);
        }
    }

    boolean isWeak(String t) {
        return t.startsWith("W/");
    }

    boolean checkStrongEtag(String et, String im) {
        return !isWeak(im) && im.equals(et);
    }

    private boolean checkWeakEtag(HTTPHeader h1, HTTPHeader h2) {
        String et1 = h1.getHeader("Etag");
        String et2 = h2.getHeader("Etag");
        if (et1 == null || et2 == null) return true;
        return checkWeakEtag(et1, et2);
    }

    private boolean checkWeakEtag(String et, String im) {
        if (et == null || im == null) return false;
        if (isWeak(et)) et = et.substring(2);
        if (isWeak(im)) im = im.substring(2);
        return im.equals(et);
    }

    public HTTPHeader checkIfMatch(HTTPHeader header, RequestHandler rh) {
        NCacheEntry entry = rh.entry;
        if (entry == null) return null;
        String im = header.getHeader("If-Match");
        if (im == null) return null;
        HTTPHeader oldresp = rh.getDataHook();
        HTTPHeader expfail = checkExpectations(header, oldresp);
        if (expfail != null) return expfail;
        String et = oldresp.getHeader("Etag");
        if (!checkStrongEtag(et, im)) return responseHandler.get412();
        return null;
    }

    /** Check if the request allows us to use a "304 Not modified" response.
     * @param in the request being made.
     * @param rh the RequestHandler for this request
     */
    public HTTPHeader is304(HTTPHeader in, RequestHandler rh) {
        NCacheEntry entry = rh.entry;
        if (entry == null) return null;
        HTTPHeader oldresp = rh.getDataHook();
        HTTPHeader expfail = checkExpectations(in, oldresp);
        if (expfail != null) return expfail;
        String ifRange = in.getHeader("If-Range");
        if (ifRange != null) return null;
        String sims = in.getHeader("If-Modified-Since");
        String sums = in.getHeader("If-Unmodified-Since");
        List<String> vinm = in.getHeaders("If-None-Match");
        String et = oldresp.getHeader("Etag");
        String range = in.getHeader("Range");
        boolean mustUseStrong = range != null;
        boolean etagMatch = false;
        Date ims = null;
        Date ums = null;
        Date dm = null;
        if (sims != null) ims = HTTPDateParser.getDate(sims);
        if (sums != null) ums = HTTPDateParser.getDate(sums);
        if (ims != null || ums != null) {
            String lm = oldresp.getHeader("Last-Modified");
            if (lm == null) return ematch(etagMatch, oldresp);
            dm = HTTPDateParser.getDate(lm);
        }
        long diff;
        if (ums != null && (diff = dm.getTime() - ums.getTime()) >= 0) {
            if (mustUseStrong && diff > 60000) return responseHandler.get412(); else return responseHandler.get412();
        }
        if (et != null) {
            for (int i = 0; i < vinm.size(); i++) {
                String sinm = vinm.get(i);
                if (sinm != null && (sinm.equals("*") || ((mustUseStrong && checkStrongEtag(et, sinm)) || (!mustUseStrong && checkWeakEtag(et, sinm))))) etagMatch = true;
            }
        }
        if (sims == null) {
            return ematch(etagMatch, oldresp);
        } else {
            if (ims == null) {
                proxy.logError(Logger.INFO, "unparseable date: " + sims + " for URL: " + in.getRequestURI());
                return ematch(etagMatch, oldresp);
            }
            if (dm == null) return ematch(etagMatch, oldresp);
            if (dm.after(ims)) return null;
            if (vinm.size() < 1) {
                if (mustUseStrong) {
                    if (dm.getTime() - ims.getTime() < 60000) return null;
                }
                return responseHandler.get304(oldresp);
            }
            return ematch(etagMatch, oldresp);
        }
    }

    private HTTPHeader ematch(boolean etagMatch, HTTPHeader oldresp) {
        if (etagMatch) return responseHandler.get304(oldresp); else return null;
    }

    /** Filtering the response gave us a blocker, so send it.
     */
    private void sendBadResponse(HTTPHeader badresponse, HTTPInputStream contentStream) {
        setKeepalive(false);
        send(badresponse);
        statuscode = badresponse.getStatusCode();
        try {
            if (contentStream != null) contentStream.close();
        } catch (IOException e) {
        }
    }

    private void checkNoStore(HTTPHeader req, NCacheEntry entry) {
        if (entry == null) return;
        List<String> ccs = req.getHeaders("Cache-Control");
        for (int i = 0; i < ccs.size(); i++) {
            String cc = ccs.get(i);
            if (cc.equals("no-store")) {
                proxy.getCache().remove(entry.getKey());
            }
        }
    }

    private boolean checkMaxStale(HTTPHeader req, RequestHandler rh) {
        NCacheEntry entry = rh.entry;
        List ccs = req.getHeaders("Cache-Control");
        for (int i = 0; i < ccs.size(); i++) {
            String cc = (String) ccs.get(i);
            cc = cc.trim();
            if (cc.equals("max-stale")) {
                if (entry != null) {
                    HTTPHeader resp = rh.getDataHook();
                    ConditionalChecker ccheck = new ConditionalChecker(proxy);
                    long maxAge = ccheck.getCacheControlValue(resp, "max-age=");
                    if (maxAge >= 0) {
                        long now = System.currentTimeMillis();
                        long secs = (now - entry.getCacheTime()) / 1000;
                        long currentAge = secs;
                        String age = resp.getHeader("Age");
                        if (age != null) currentAge += Long.parseLong(age);
                        if (currentAge > maxAge) {
                            resp.addHeader("Warning", "110 RabbIT \"Response is stale\"");
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    private boolean checkMaxAge(RequestHandler rh) {
        NCacheEntry entry = rh.entry;
        if (entry == null) return false;
        HTTPHeader resp = rh.getDataHook();
        ConditionalChecker ccheck = new ConditionalChecker(proxy);
        return ccheck.checkMaxAge(this, resp, rh);
    }

    /** A container to send around less parameters.*/
    protected class RequestHandler {

        public HTTPInputStream contentStream = null;

        public HTTPHeader webheader = null;

        public NCacheEntry entry = null;

        public HTTPHeader dataHook = null;

        public HandlerFactory handlerFactory = null;

        public long size = -1;

        public WebConnection wc = null;

        public Date requestTime = null;

        public HTTPHeader getDataHook() {
            if (dataHook != null) return dataHook;
            return dataHook = getDataHook(entry);
        }

        private HTTPHeader getDataHook(NCacheEntry entry) {
            return (HTTPHeader) entry.getDataHook(proxy.getCache());
        }
    }

    private void setAge(RequestHandler rh) {
        Date now = new Date();
        String age = rh.webheader.getHeader("Age");
        String date = rh.webheader.getHeader("Date");
        Date dd = HTTPDateParser.getDate(date);
        if (dd == null) dd = now;
        long lage = 0;
        try {
            if (age != null) {
                lage = Long.parseLong(age);
            }
            long dt = Math.max((now.getTime() - dd.getTime() - proxy.getOffset()) / 1000, 0);
            long correct_age = lage + dt;
            long correct_recieved_age = Math.max(dt, lage);
            long corrected_initial_age = correct_recieved_age + dt;
            if (corrected_initial_age > 0) {
                rh.webheader.setHeader("Age", "" + corrected_initial_age);
            }
        } catch (NumberFormatException e) {
            proxy.logError(Logger.WARN, "Bad age: " + age);
        }
    }

    private int nextNonBlank(String s, int start) {
        char c;
        int len = s.length();
        while (start < len && ((c = s.charAt(start)) == ' ' || c == '\n' || c == '\r' || c == '\t')) start++;
        return start;
    }

    private int nextBlank(String s, int start) {
        char c;
        int len = s.length();
        while (start < len && !((c = s.charAt(start)) == ' ' || c == '\n' || c == '\r' || c == '\t')) start++;
        return start;
    }

    private void removeWarnings(HTTPHeader header, boolean remove1xx) {
        List warns = header.getHeaders("Warning");
        String rdate = header.getHeader("Date");
        for (int w = 0; w < warns.size(); w++) {
            String val = (String) warns.get(w);
            try {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                int start = 0;
                while (start < val.length()) {
                    int i = nextNonBlank(val, start);
                    i = nextBlank(val, i);
                    String code = val.substring(start, i);
                    int j = nextNonBlank(val, i + 1);
                    j = nextBlank(val, j);
                    String agent = val.substring(i + 1, j);
                    int k = val.indexOf('"', j);
                    int l = val.indexOf('"', k + 1);
                    String text = val.substring(k + 1, l);
                    int c = val.indexOf(',', l);
                    int m = val.indexOf('"', l + 1);
                    String date = null;
                    if (((c == -1 && m == -1) || (c < m))) {
                        start = l + 1;
                    } else {
                        int n = val.indexOf('"', m + 1);
                        date = val.substring(m + 1, n);
                        int c2 = val.indexOf(',', n + 1);
                        if (c2 != -1) start = c2; else start = n + 1;
                    }
                    char s;
                    while (start < val.length() && ((s = val.charAt(start)) == ' ' || s == ',')) start++;
                    Date d1 = null, d2 = null;
                    if (date != null) d1 = HTTPDateParser.getDate(date);
                    if (rdate != null) d2 = HTTPDateParser.getDate(rdate);
                    if ((d1 != null && !d1.equals(d2)) || (remove1xx && code.charAt(0) == '1') && !"RabbIT".equals(agent)) {
                    } else {
                        if (!first) sb.append(", ");
                        sb.append(code + " " + agent + " \"" + text + (date != null ? "\" \"" + date + "\"" : "\""));
                        first = false;
                    }
                }
                if (sb.length() != 0) header.setExistingValue(val, sb.toString()); else header.removeValue(val);
            } catch (StringIndexOutOfBoundsException e) {
                proxy.logError(Logger.WARN, "bad warning header: '" + val + "'");
            }
        }
    }

    private void updateWarnings(HTTPHeader header, HTTPHeader webheader) {
        List warnings = webheader.getHeaders("Warning");
        for (int i = 0; i < warnings.size(); i++) header.addHeader("Warning", (String) warnings.get(i));
    }

    private void updateHeader(RequestHandler rh, HTTPHeader cachedheader, String header) {
        String h = rh.webheader.getHeader(header);
        if (h != null) cachedheader.setHeader(header, h);
    }

    private void updateHeader(RequestHandler rh) {
        if (rh.entry == null) return;
        HTTPHeader cachedheader = rh.getDataHook();
        updateHeader(rh, cachedheader, "Date");
        updateHeader(rh, cachedheader, "Expires");
        updateHeader(rh, cachedheader, "Content-Location");
        List ccs = rh.webheader.getHeaders("Cache-Control");
        if (ccs.size() > 0) {
            cachedheader.removeHeader("Cache-Control");
            for (int i = 0; i < ccs.size(); i++) {
                String cc = (String) ccs.get(i);
                cachedheader.addHeader("Cache-Control", cc);
            }
        }
        List varys = rh.webheader.getHeaders("Vary");
        if (varys.size() > 0) {
            cachedheader.removeHeader("Vary");
            for (int i = 0; i < varys.size(); i++) {
                String cc = (String) varys.get(i);
                cachedheader.addHeader("Vary", cc);
            }
        }
        removeWarnings(cachedheader, true);
        updateWarnings(cachedheader, rh.webheader);
    }

    private List<RandomStream.Range> getRanges(HTTPHeader header, RequestHandler rh) {
        List ranges = header.getHeaders("Range");
        int z = ranges.size();
        if (z == 0) return null;
        List<RandomStream.Range> ret = new ArrayList<RandomStream.Range>();
        try {
            for (int i = 0; i < z; i++) {
                String rs = ((String) ranges.get(i)).trim();
                if (!rs.startsWith("bytes")) return null;
                rs = rs.substring(5);
                int j = rs.indexOf('=');
                if (j == -1) return null;
                rs = rs.substring(j + 1);
                StringTokenizer st = new StringTokenizer(rs, ",");
                while (st.hasMoreTokens()) {
                    String r = st.nextToken();
                    int d = r.indexOf('-');
                    if (d == -1) return null;
                    String s = r.substring(0, d).trim();
                    String e = r.substring(d + 1).trim();
                    long start = -1;
                    long end = -1;
                    long size = rh.entry.getSize();
                    if (s.length() > 0) {
                        start = Integer.parseInt(s);
                        if (e.length() > 0) {
                            end = Integer.parseInt(e);
                        } else {
                            end = size;
                        }
                        if (start > size) throw new IllegalArgumentException("bad range: start bigger than size");
                        if (start > end) return null;
                        if (start < 0 || end < 0) throw new IllegalArgumentException("bad range: start and end both less than zero");
                        ret.add(new RandomStream.Range(start, end));
                    } else if (e.length() > 0) {
                        start = Integer.parseInt(e);
                        if (start < 0) throw new IllegalArgumentException("bad range: start less than zero");
                        start = size - start;
                        end = size;
                        ret.add(new RandomStream.Range(start, end));
                    } else {
                        return null;
                    }
                }
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return ret;
    }

    private boolean checkConditions(HTTPHeader header, HTTPHeader webheader) {
        String inm = header.getHeader("If-None-Match");
        if (inm != null) {
            String etag = webheader.getHeader("ETag");
            if (!checkWeakEtag(inm, etag)) return false;
        }
        Date dm = null;
        String sims = header.getHeader("If-Modified-Since");
        if (sims != null) {
            Date ims = HTTPDateParser.getDate(sims);
            String lm = webheader.getHeader("Last-Modified");
            if (lm != null) {
                dm = HTTPDateParser.getDate(lm);
                if (dm.getTime() - ims.getTime() < 60000) return false;
            }
        }
        String sums = header.getHeader("If-Unmodified-Since");
        if (sums != null) {
            Date ums = HTTPDateParser.getDate(sums);
            if (dm != null) {
                if (dm.after(ums)) return false;
            } else {
                String lm = webheader.getHeader("Last-Modified");
                if (lm != null) {
                    dm = HTTPDateParser.getDate(lm);
                    if (dm.after(ums)) return false;
                }
            }
        }
        return true;
    }

    private boolean haveAllRanges(List<RandomStream.Range> ranges, RequestHandler rh, long totalSize) {
        if (rh.entry.getSize() == totalSize) return true;
        String cr = rh.webheader.getHeader("Content-Range");
        if (cr == null) return false;
        for (int i = 0; i < ranges.size(); i++) {
            RandomStream.Range r = ranges.get(i);
            long start = r.getStart();
            long end = r.getEnd();
            String t = "bytes " + start + "-" + end + "/" + totalSize;
            if (!t.equals(cr)) {
                cr = cr.substring(6);
                StringTokenizer st = new StringTokenizer(cr, "-/");
                if (st.countTokens() == 3) {
                    int rstart = Integer.parseInt(st.nextToken());
                    int rend = Integer.parseInt(st.nextToken());
                    if (rstart > start || rend < end) return false;
                }
            }
        }
        return true;
    }

    private long getTotalSize(RequestHandler rh) {
        String cr = rh.webheader.getHeader("Content-Range");
        if (cr != null) {
            int i = cr.lastIndexOf('/');
            if (i != -1) {
                return Long.parseLong(cr.substring(i + 1));
            }
        }
        String cl = rh.webheader.getHeader("Content-Length");
        if (cl != null) return Long.parseLong(cl);
        return rh.entry.getSize();
    }

    private HTTPHeader setupCachedEntry(HTTPHeader header, RequestHandler rh) throws IOException {
        String ifRange = header.getHeader("If-Range");
        boolean mayRange = true;
        rh.webheader = rh.getDataHook();
        if (ifRange != null) {
            String etag = rh.webheader.getHeader("ETag");
            if (etag != null) {
                mayRange = checkStrongEtag(etag, ifRange);
            } else {
                mayRange = false;
            }
            boolean cc = checkConditions(header, rh.webheader);
            if (mayRange && !cc) {
                rh.webheader = null;
                rh.contentStream = null;
                return null;
            } else if (!cc) {
                rh.contentStream = null;
                return null;
            }
        }
        List<RandomStream.Range> ranges = null;
        if (mayRange) {
            try {
                ranges = getRanges(header, rh);
            } catch (IllegalArgumentException e) {
                return responseHandler.get416(e);
            }
        }
        setChunking(false);
        if (ranges != null) {
            long totalSize = getTotalSize(rh);
            if (!haveAllRanges(ranges, rh, totalSize)) {
                rh.webheader = null;
                rh.contentStream = null;
                return null;
            }
            rh.contentStream = new HTTPInputStream(new RandomStream(this, rh, ranges, totalSize), proxy);
            setChunking(false);
            rh.webheader = responseHandler.get206(ifRange, rh.webheader);
            statuscode = "206";
            if (ranges.size() > 1) {
                rh.webheader.removeHeader("Content-Length");
                rh.webheader.setHeader("Content-Type", "multipart/byteranges; boundary=THIS_STRING_SEPARATES");
            } else {
                RandomStream.Range r = ranges.get(0);
                rh.webheader.setHeader("Content-Range", "bytes " + r.getStart() + "-" + r.getEnd() + "/" + totalSize);
                rh.size = (r.getEnd() - r.getStart() + 1);
                rh.webheader.setHeader("Content-Length", "" + rh.size);
            }
        } else {
            rh.contentStream = new HTTPInputStream(new CacheStream(proxy.getCache(), rh.entry, proxy.getCounter()), proxy);
            rh.size = rh.entry.getSize();
            rh.webheader.setStatusCode("200");
            rh.webheader.setReasonPhrase("OK");
        }
        String age = rh.webheader.getHeader("Age");
        long now = System.currentTimeMillis();
        long secs = (now - rh.entry.getCacheTime()) / 1000;
        if (age != null) {
            try {
                long l = Long.parseLong(age);
                secs += l;
            } catch (NumberFormatException e) {
                proxy.logError(Logger.WARN, "bad Age : '" + age + "'");
            }
        }
        rh.webheader.setHeader("Age", "" + secs);
        String ctype = rh.webheader.getHeader("Content-Type");
        if (ctype != null) rh.handlerFactory = proxy.getCacheHandlers().get(ctype);
        if (rh.handlerFactory == null || ranges != null) rh.handlerFactory = BaseHandler.getFactory();
        removeWarnings(rh.webheader, false);
        return null;
    }

    private void setupWebConnection(HTTPHeader header, RequestHandler rh) throws IOException {
        int attempts = 0;
        String method = header.getMethod().trim();
        boolean safe = true;
        do {
            attempts++;
            rh.requestTime = new Date();
            try {
                rh.wc = proxy.getWebConnection(header);
            } catch (IOException e) {
                proxy.getCounter().inc("Failed to connect socket");
                proxy.logError(Logger.WARN, "Failed to connect socket:" + e);
                continue;
            }
            synchronized (rh.wc) {
                try {
                    if (header.getContentStream() != null) header.setHeader("Transfer-Encoding", "chunked");
                    safe = rh.wc.getReleasedAt() != null || (method != null && (method.equals("GET") || method.equals("HEAD")));
                    rh.wc.writeHTTPHeader(header);
                    HTTPInputStream his = header.getContentStream();
                    if (his != null) {
                        InputStream is = his.getChunkStream();
                        chunkData(rh.wc, is);
                    }
                } catch (IOException e) {
                    proxy.getCounter().inc("WebConnections failed on write");
                    rh.wc.close();
                    rh.wc = null;
                    continue;
                }
            }
            try {
                rh.contentStream = rh.wc.getInputStream();
                if (rh.contentStream == null) {
                    proxy.getCounter().inc("Pipeline failure");
                    rh.wc.close();
                    rh.wc = null;
                    continue;
                }
                if (!header.isDot9Request()) {
                    char status = '0';
                    do {
                        rh.webheader = rh.contentStream.readHTTPHeader(true);
                        String sc = rh.webheader.getStatusCode();
                        if (sc.length() > 0 && (status = sc.charAt(0)) == '1') {
                            if (requestline.endsWith("1.1")) out.writeHTTPHeader(rh.webheader, proxy.isProxyConnected(), proxy.getProxyAuthString());
                        }
                    } while (status == '1');
                    rh.size = rh.wc.dataSize();
                    if (rh.wc.chunked()) {
                        rh.contentStream = new HTTPInputStream(rh.wc.getChunkStream(), proxy);
                    }
                    String responseVersion = rh.webheader.getResponseHTTPVersion();
                    setAge(rh);
                    removeWarnings(rh.webheader, false);
                    rh.webheader.addHeader("Via", responseVersion + " RabbIT");
                } else rh.handlerFactory = BaseHandler.getFactory();
            } catch (SocketException e) {
                proxy.getCounter().inc("WebConnections failed on read header");
                rh.wc.close();
                rh.wc = null;
                proxy.logError(Logger.DEBUG, "SocketException when reading response:" + e);
            } catch (IOException e) {
                proxy.getCounter().inc("WebConnections failed on read header");
                safe = rh.wc.getReleasedAt() != null || (method != null && (method.equals("GET") || method.equals("HEAD")));
                rh.wc.close();
                rh.wc = null;
                proxy.logError(Logger.DEBUG, "IOException when reading response:" + e);
            }
            if (rh.wc == null && safe) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                }
            }
        } while (rh.wc == null && safe && attempts < 5);
        if (rh.wc == null) throw new IOException("Unable to connect to URL: " + header.getRequestURI()); else proxy.markForPipelining(rh.wc);
    }

    private boolean partialContent(HTTPHeader request, RequestHandler rh) {
        if (rh.entry == null) return false;
        String method = request.getMethod();
        if (!method.equals("GET")) return false;
        HTTPHeader resp = rh.getDataHook();
        String realLength = resp.getHeader("RabbIT-Partial");
        return (realLength != null);
    }

    private void fillupContent(HTTPHeader request, RequestHandler rh) {
        HTTPHeader resp = rh.getDataHook();
        String realLength = resp.getHeader("RabbIT-Partial");
        long l = Long.parseLong(realLength);
        try {
            setupWebConnection(request, rh);
            String status = rh.webheader.getStatusCode();
            if (status.equals("200")) {
                setMayUseCache(false);
                proxy.releaseWebConnection(rh.wc);
                return;
            } else if (status.equals("206")) {
                proxy.logError(Logger.WARN, "Filling up partial request is not yet implemented:");
                proxy.releaseWebConnection(rh.wc);
                return;
            }
        } catch (IOException e) {
            proxy.logError(Logger.WARN, "Error filling up partial request:" + e);
            return;
        }
        setMayCache(true);
    }

    private void setMayCacheFromCC(HTTPHeader header, RequestHandler rh) {
        HTTPHeader resp = rh.webheader;
        List ccs = resp.getHeaders("Cache-Control");
        for (int i = 0; i < ccs.size(); i++) {
            String val = (String) ccs.get(i);
            if ("public".equals(val) || "must-revalidate".equals(val) || val.startsWith("s-maxage=")) {
                String auth = header.getHeader("Authorization");
                if (auth != null) {
                    maycache = true;
                    break;
                }
            }
        }
    }

    private void checkIfRange(HTTPHeader header, RequestHandler rh) {
        NCacheEntry entry = rh.entry;
        if (entry == null) return;
        String ifRange = header.getHeader("If-Range");
        if (ifRange == null) return;
        String range = header.getHeader("Range");
        if (range == null) return;
        Date d = HTTPDateParser.getDate(ifRange);
        HTTPHeader oldresp = rh.getDataHook();
        if (d != null) {
        } else {
            String etag = oldresp.getHeader("Etag");
            if (etag == null || !checkWeakEtag(etag, ifRange)) setMayUseCache(false);
        }
    }

    /** Close cache streams and return web connections.
     */
    private void closeDown(RequestHandler rh) {
        if (rh.wc != null) {
            proxy.releaseWebConnection(rh.wc);
            rh.wc = null;
        } else if (rh.entry != null) {
            try {
                FileChannel fc = rh.contentStream.getFileChannel();
                if (fc != null) {
                    fc.close();
                }
                rh.contentStream.close();
            } catch (IOException e) {
                proxy.logError(Logger.WARN, "Error closing stream:" + e);
                setKeepalive(false);
            }
            rh.entry = null;
        }
    }

    /** Handle a request by getting the datastream (from the cache or the web).
     *  After getting the handler for the mimetype, send it.
     * @param header the request made.
     */
    public void handleRequest(HTTPHeader header) {
        RequestHandler rh = new RequestHandler();
        String method = header.getMethod().trim();
        if (!method.equals("GET") && !method.equals("HEAD")) {
            proxy.getCache().remove(header);
        }
        rh.entry = proxy.getCache().getEntry(header);
        checkNoStore(header, rh.entry);
        if (!checkMaxStale(header, rh) && checkMaxAge(rh)) setMayUseCache(false);
        ConditionalChecker ccheck = new ConditionalChecker(proxy);
        boolean conditional = ccheck.checkConditional(this, header, rh);
        if (partialContent(header, rh)) {
            fillupContent(header, rh);
        }
        checkIfRange(header, rh);
        boolean mc = getMayCache();
        if (getMayUseCache()) {
            if (rh.entry != null) {
                proxy.getCounter().inc("Cache hits");
                setKeepalive(true);
                HTTPHeader resp = checkIfMatch(header, rh);
                if (resp == null) resp = is304(header, rh);
                if (resp != null) {
                    statuscode = resp.getStatusCode();
                    sendBadResponse(resp, rh.contentStream);
                    return;
                }
                setMayCache(false);
                try {
                    resp = setupCachedEntry(header, rh);
                    if (resp != null) {
                        send(resp);
                        statuscode = resp.getStatusCode();
                        closeDown(rh);
                        return;
                    }
                } catch (FileNotFoundException e) {
                    rh.contentStream = null;
                    rh.entry = null;
                } catch (IOException e) {
                    rh.contentStream = null;
                    rh.entry = null;
                }
            }
        }
        if (rh.contentStream == null) {
            maycache = mc;
            try {
                setupWebConnection(header, rh);
                setMayCacheFromCC(header, rh);
            } catch (IOException e) {
                proxy.logError(Logger.WARN, "strange error setting up web connection: " + e.toString());
                if (rh.entry != null && conditional && !mustRevalidate) {
                    setMayCache(false);
                    try {
                        setupCachedEntry(header, rh);
                        rh.webheader.addHeader("Warning", "110 RabbIT \"Response is stale\"");
                    } catch (IOException ex) {
                        doError(504, ex);
                        setKeepalive(false);
                        closeDown(rh);
                        return;
                    }
                } else {
                    doError(504, e);
                    setKeepalive(false);
                    closeDown(rh);
                    return;
                }
            }
        }
        if (!header.isDot9Request()) {
            TunnelHandler th = proxy.getTunnelHandler();
            if (th.mustTunnel(header, rh)) {
                th.handleTunnel(in, out, rh);
                return;
            }
            String status = rh.webheader.getStatusCode().trim();
            rh.entry = proxy.getCache().getEntry(header);
            checkStaleCache(header, rh);
            removeOtherStaleCaches(header, rh.webheader);
            if (status.equals("304")) updateHeader(rh);
            HTTPHeader badresponse = checkExpectations(header, rh.webheader);
            if (badresponse == null) badresponse = filterHTTPOut(proxy.getHTTPOutFilters(), rh.webheader);
            if (badresponse != null) {
                sendBadResponse(badresponse, rh.contentStream);
                closeDown(rh);
                return;
            }
            if (conditional && (rh.entry = proxy.getCache().getEntry(header)) != null && status.equals("304")) {
                HTTPHeader cachedheader = rh.getDataHook();
                proxy.releaseWebConnection(rh.wc);
                if (addedINM) header.removeHeader("If-None-Match");
                if (addedIMS) header.removeHeader("If-Modified-Since");
                if (checkWeakEtag(cachedheader, rh.webheader)) {
                    updateHeader(rh);
                    setMayCache(false);
                    try {
                        HTTPHeader res304 = is304(header, rh);
                        if (res304 != null) {
                            send(res304);
                            statuscode = res304.getStatusCode();
                            closeDown(rh);
                            return;
                        } else {
                            setupCachedEntry(header, rh);
                        }
                    } catch (FileNotFoundException e) {
                        proxy.logError(Logger.WARN, "Conditional request could not find the cached file (" + header.getRequestURI() + ", : " + e);
                    } catch (IOException e) {
                        proxy.logError(Logger.WARN, "Conditional request got IOException (" + header.getRequestURI() + ",: " + e);
                    }
                } else {
                    header.removeHeader("If-None-Match");
                    proxy.getCache().remove(header);
                    handleRequest(header);
                    closeDown(rh);
                    return;
                }
            }
            status = rh.webheader.getStatusCode().trim();
            if (status != null && status.length() > 0) {
                if (status.equals("304") || status.equals("204") || status.charAt(0) == '1') {
                    send(rh.webheader);
                    closeDown(rh);
                    return;
                }
            }
        }
        if (rh.handlerFactory == null) {
            String ct = rh.webheader.getHeader("Content-Type");
            if (ct != null) ct = ct.toLowerCase();
            if (getMayFilter() && rh.webheader != null && ct != null) rh.handlerFactory = proxy.getHandlers().get(ct.toLowerCase());
            if (rh.handlerFactory == null && ct != null && ct.startsWith("multipart/byteranges")) rh.handlerFactory = MultiPartHandler.getFactory();
            if (rh.handlerFactory == null) {
                rh.handlerFactory = BaseHandler.getFactory();
            }
        }
        Handler handler = rh.handlerFactory.getNewInstance(this, header, rh.webheader, rh.contentStream, out, getMayCache(), getMayFilter(), rh.size);
        if (handler == null) {
            doError(500, "Something fishy with that handler....");
            setKeepalive(false);
            closeDown(rh);
        } else {
            try {
                if (chunk) {
                    rh.webheader.removeHeader("Content-Length");
                    rh.webheader.setHeader("Transfer-Encoding", "chunked");
                } else {
                    if (getKeepalive()) {
                        rh.webheader.setHeader("Proxy-Connection", "Keep-Alive");
                        rh.webheader.setHeader("Connection", "Keep-Alive");
                    } else {
                        rh.webheader.setHeader("Proxy-Connection", "close");
                        rh.webheader.setHeader("Connection", "close");
                    }
                }
                if (header.isHeadOnlyRequest()) out.writeHTTPHeader(rh.webheader, proxy.isProxyConnected(), proxy.getProxyAuthString()); else handler.handle();
                if (chunk) client.finish();
                closeDown(rh);
            } catch (IOException e) {
                proxy.logError(Logger.WARN, "Error writing request:" + e);
                setKeepalive(false);
                closeDown(rh);
                return;
            }
        }
    }

    private void chunkData(WebConnection wc, InputStream is) throws IOException {
        byte[] buf = new byte[1024];
        int read;
        HTTPOutputStream os = wc.getOutputStream();
        while ((read = is.read(buf)) > 0) os.write(buf, 0, read);
        os.finish();
    }

    private void checkStaleCache(HTTPHeader header, RequestHandler rh) {
        NCacheEntry entry = rh.entry;
        if (entry == null) return;
        HTTPHeader webheader = rh.webheader;
        if (webheader.getStatusCode().trim().equals("304")) return;
        HTTPHeader cachedwebheader = rh.getDataHook();
        String sd = webheader.getHeader("Date");
        String cd = cachedwebheader.getHeader("Date");
        if (sd != null && cd != null) {
            Date d1 = HTTPDateParser.getDate(sd);
            Date d2 = HTTPDateParser.getDate(cd);
            if (d1 != null && d1.before(d2)) {
                setMayCache(false);
                return;
            }
        }
        if (webheader.getStatusCode().equals("200")) checkStaleHeader(header, webheader, cachedwebheader, "Content-Length");
        checkStaleHeader(header, webheader, cachedwebheader, "Content-MD5");
        checkStaleHeader(header, webheader, cachedwebheader, "ETag");
        checkStaleHeader(header, webheader, cachedwebheader, "Last-Modified");
    }

    private void removeCaches(HTTPHeader header, HTTPHeader webheader, String type) {
        String loc = webheader.getHeader(type);
        if (loc == null) return;
        try {
            URL u = new URL(header.getRequestURI());
            URL u2 = new URL(u, loc);
            String host1 = u.getHost();
            String host2 = u.getHost();
            if (!host1.equals(host2)) return;
            int port1 = u.getPort();
            if (port1 == -1) port1 = 80;
            int port2 = u2.getPort();
            if (port2 == -1) port2 = 80;
            if (port1 != port2) return;
            HTTPHeader h = new HTTPHeader();
            h.setRequestURI(u2.toString());
            proxy.getCache().remove(h);
        } catch (MalformedURLException e) {
            proxy.logError(Logger.WARN, "Trying to delete cached object with bad url (" + header.getRequestURI() + ", " + loc + ": " + e);
        }
    }

    private void removeCaches(HTTPHeader header, HTTPHeader webheader) {
        removeCaches(header, webheader, "Location");
        removeCaches(header, webheader, "Content-Location");
    }

    private void removeOtherStaleCaches(HTTPHeader header, HTTPHeader webheader) {
        String method = header.getMethod();
        String status = webheader.getStatusCode();
        if ((method.equals("PUT") || method.equals("POST")) && status.equals("201")) {
            removeCaches(header, webheader);
        } else if (method.equals("DELETE") && status.equals("200")) {
            removeCaches(header, webheader);
        }
    }

    private void checkStaleHeader(HTTPHeader header, HTTPHeader webheader, HTTPHeader cachedwebheader, String str) {
        String cln = webheader.getHeader(str);
        String clo = cachedwebheader.getHeader(str);
        if (clo != null) {
            if (!clo.equals(cln)) {
                proxy.getCache().remove(header);
                return;
            }
        } else {
            if (cln != null) {
                proxy.getCache().remove(header);
                return;
            }
        }
    }

    private HTTPHeader checkExpectations(HTTPHeader header, HTTPHeader webheader) {
        String exp = header.getHeader("Expect");
        if (exp == null) return null;
        if (exp.equals("100-continue")) {
            String status = webheader.getStatusCode();
            if (status.equals("200") || status.equals("304")) return null;
            return responseHandler.get417(exp);
        }
        StringTokenizer st = new StringTokenizer(exp, ";");
        while (st.hasMoreTokens()) {
            String e = st.nextToken();
            int i = e.indexOf('=');
            if (i == -1 || i == e.length() - 1) return responseHandler.get417(e);
            String type = e.substring(0, i);
            String value = e.substring(i + 1);
            if (type.equals("expect")) {
                String h = webheader.getHeader(value);
                if (h == null) return responseHandler.get417("No expected header found");
            }
        }
        return responseHandler.get417(exp);
    }

    /** Handle a meta page.
     * @param header the request being made.
     */
    public void handleMeta(HTTPHeader header) {
        proxy.getCounter().inc("Meta pages requested");
        URL url = null;
        try {
            url = new URL(header.getRequestURI());
        } catch (MalformedURLException e) {
        }
        String file = url.getFile().substring(1);
        if (file.length() == 0) file = "FileSender/";
        int index = -1;
        String args = "";
        if ((index = file.indexOf("?")) >= 0) {
            args = file.substring(index + 1);
            file = file.substring(0, index);
        }
        Properties htab = splitArgs(args);
        if ((index = file.indexOf("/")) >= 0) {
            String fc = file.substring(index + 1);
            file = file.substring(0, index);
            htab.put("argstring", fc);
        }
        String error = null;
        try {
            if (file.indexOf(".") < 0) file = "rabbit.meta." + file;
            Class<? extends MetaHandler> cls = Class.forName(file).asSubclass(MetaHandler.class);
            MetaHandler mh = null;
            synchronized (metaHandlers) {
                mh = metaHandlers.get(cls);
                if (mh == null) {
                    mh = cls.newInstance();
                    metaHandlers.put(cls, mh);
                }
            }
            mh.handle(in, out, header, htab, this);
            proxy.getCounter().inc("Meta pages handled");
        } catch (NoSuchMethodError e) {
            error = "Given metahandler doesnt have a public no-arg constructor:" + file + ", " + e;
        } catch (ClassCastException e) {
            error = "Given metapage is not a MetaHandler:" + file + ", " + e;
        } catch (ClassNotFoundException e) {
            error = "Couldnt find class:" + file + ", " + e;
        } catch (InstantiationException e) {
            error = "Couldnt instantiate metahandler:" + file + ", " + e;
        } catch (IllegalAccessException e) {
            error = "Que? metahandler access violation?:" + file + ", " + e;
        } catch (IllegalArgumentException e) {
            error = "Strange name of metapage?:" + file + ", " + e;
        }
        if (error != null) {
            proxy.logError(Logger.WARN, error);
            doError(400, error);
            return;
        }
    }

    /** splits the CGI-paramsstring into variables and values.
     *  put these values into a hashtable for easy retrival
     * @param params the CGI-querystring.
     * @return a hastable with type->value maps for the CGI-querystring
     */
    public Properties splitArgs(String params) {
        Properties htab = new Properties();
        StringTokenizer st = new StringTokenizer(params, "=&", true);
        String key = null;
        while (st.hasMoreTokens()) {
            String next = st.nextToken();
            if (next.equals("=")) {
            } else if (next.equals("&")) {
                if (key != null) {
                    htab.put(key, "");
                    key = null;
                }
            } else if (key == null) {
                key = next;
            } else {
                htab.put(key, Coder.URLdecode(next));
                key = null;
            }
        }
        return htab;
    }

    /** Send an error (400 Bad Request) to the client.
     * @param statuscode the status code of the error.
     * @param message the error message to tell the client.
     */
    public void doError(int statuscode, String message) {
        this.statuscode = "" + statuscode;
        HTTPHeader header = responseHandler.getHeader("HTTP/1.0 400 Bad Request");
        StringBuilder error = new StringBuilder(HTMLPage.getPageHeader(this, "400 Bad Request") + "Unable to handle request:<br><b>" + message + "</b></body></html>\n");
        header.setContent(error.toString());
        send(header);
    }

    /** Send an error (400 Bad Request) to the client.
     * @param statuscode the status code of the error.
     * @param e the exception to tell the client.
     */
    public void doError(int statuscode, Exception e) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter ps = new PrintWriter(new OutputStreamWriter(bos));
        e.printStackTrace(ps);
        String message = bos.toString();
        this.statuscode = "" + statuscode;
        extrainfo = (extrainfo != null ? extrainfo + e.toString() : e.toString());
        HTTPHeader header = null;
        if (statuscode == 504) header = responseHandler.get504(e, requestline); else header = responseHandler.getHeader("HTTP/1.0 400 Bad Request");
        StringBuilder error = new StringBuilder(HTMLPage.getPageHeader(this, statuscode + " " + header.getReasonPhrase()) + "Unable to handle request:<br><b>" + e.getMessage() + (header.getContent() != null ? "<br>" + header.getContent() : "") + "</b><br><xmp>" + message + "</xmp></body></html>\n");
        header.setContent(error.toString());
        send(header);
    }

    /** Get the proxy that this connection is working for.
     */
    public Proxy getProxy() {
        return proxy;
    }

    /** Get the status of this Connection
     * @return the current status.
     */
    public String getStatus() {
        return status;
    }

    /** Get the time this Connection was started.
     * @return the time this Connection started serving requests.
     */
    public Date getStarted() {
        return started;
    }

    /** Set the user name of the client.
     * @param username the username of the client.
     */
    public void setUserName(String username) {
        this.username = username;
    }

    /** Get the username of the client.
     * @return the current username of the client.
     */
    public String getUserName() {
        return username;
    }

    /** Set the password of the client.
     * @param password the password of the client.
     */
    public void setPassWord(String password) {
        this.password = password;
    }

    /** Get the password of the client.
     * @return the current password of the client.
     */
    public String getPassWord() {
        return password;
    }

    /** Set keepalive to a new value. Note that keepalive can only be
     *	promoted down. 
     * @param keepalive the new keepalive value.
     */
    public void setKeepalive(boolean keepalive) {
        this.keepalive = (this.keepalive && keepalive);
    }

    /** Get the keepalive value.
     * @return true if keepalive should be done, false otherwise.
     */
    public boolean getKeepalive() {
        return keepalive;
    }

    /** Set the chunking option.
     * @param b if true this connection should use chunking.
     */
    public void setChunking(boolean b) {
        chunk = b;
    }

    /** Get the chunking option.
     * @return if this connection is using chunking.
     */
    public boolean getChunking() {
        return chunk;
    }

    /** Set the state of this request.
     * @param meta true if this request is a metapage request, false otherwise.
     */
    public void setMeta(boolean meta) {
        this.meta = meta;
    }

    /** Get the state of this request.
     * @return true if this is a metapage request, false otherwise.
     */
    public boolean getMeta() {
        return meta;
    }

    /** Set the state of this request. This can only be promoted down..
     * @param usecache true if we may use the cache for this request, false otherwise.
     */
    public void setMayUseCache(boolean usecache) {
        mayusecache = mayusecache && usecache;
    }

    /** Get the state of this request.
     * @return true if we may use the cache for this request, false otherwise.
     */
    public boolean getMayUseCache() {
        return mayusecache;
    }

    /** Set the state of this request. This can only be promoted down.
     * @param cacheAllowed true if we may cache the response, false otherwise.
     */
    public void setMayCache(boolean cacheAllowed) {
        maycache = cacheAllowed && maycache;
    }

    /** Get the state of this request.
     * @return true if we may cache the response, false otherwise.
     */
    public boolean getMayCache() {
        return maycache;
    }

    /** Get the state of this request. This can only be promoted down.
     * @param filterAllowed true if we may filter the response, false otherwise.
     */
    public void setMayFilter(boolean filterAllowed) {
        mayfilter = filterAllowed && mayfilter;
    }

    /** Get the state of the request.
     * @return true if we may filter the response, false otherwise.
     */
    public boolean getMayFilter() {
        return mayfilter;
    }

    public void setAddedINM(boolean b) {
        addedINM = b;
    }

    public void setAddedIMS(boolean b) {
        addedIMS = b;
    }

    public void setMustRevalidate(boolean b) {
        mustRevalidate = b;
    }

    /** Get the Socket that is being served.
     * @return the requesting Socket.
     */
    public Socket getSocket() {
        return socket.socket();
    }

    /** Get the current request line
     * @return the request being handled.
     */
    public String getRequestLine() {
        return requestline;
    }

    /** Get the status code of the request.
     * @return the current status code.
     */
    public String getStatusCode() {
        return statuscode;
    }

    /** Get the extra information for this request.
     * @return the extra information if set.
     */
    public String getExtraInfo() {
        return extrainfo;
    }

    /** Set the extra information for this request.
     * @param exinfo the new extra information to set.
     */
    public void setExtraInfo(String exinfo) {
        extrainfo = exinfo;
    }

    /** Set the content length of the response.
     * @param cl the new content length.
     */
    public void setContentLength(String cl) {
        contentlength = cl;
    }

    /** Get the content length of the response.
     * @return the content length of the response if set.
     */
    public String getContentLength() {
        return contentlength;
    }

    public StandardResponseHeaders getResponseHandler() {
        return responseHandler;
    }
}
