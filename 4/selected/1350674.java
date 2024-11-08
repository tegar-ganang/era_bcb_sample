package net.sourceforge.jwap;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;
import net.sourceforge.jwap.util.Logger;
import net.sourceforge.jwap.wsp.CWSPMethodManager;
import net.sourceforge.jwap.wsp.CWSPResult;
import net.sourceforge.jwap.wsp.CWSPSession;
import net.sourceforge.jwap.wsp.CWSPSocketAddress;
import net.sourceforge.jwap.wsp.IWSPUpperLayer2;
import net.sourceforge.jwap.wsp.pdu.CWSPHeaders;
import net.sourceforge.jwap.wtp.CWTPSocket;

/**
 * This class represents a WSP "User-Agent" which can be used for executing 
 * WSP <code>GET</code> and <code>POST</code> methods.
 * <p>
 * <h3>Example</h3>
 * <pre>
 * WAPClient client = new WAPClient("localhost", 9201);
 * Request request = new GetRequest("http://localhost/");
 * client.connect();
 * Response response = client.execute(request);
 * client.disconnect();
 * </pre>
 * @author Michel Marti
 *
 */
public class WAPClient {

    /** Default connect/disconnect timeout in milliseconds: 30000 */
    public static final long DEFAULT_CONNECT_TIMEOUT = 30000;

    /** Default execute timeout in milliseconds: 180000 */
    public static final long DEFAULT_EXEC_TIMEOUT = 180000;

    private static final Logger log = Logger.getLogger(WAPClient.class);

    private static final String USER_AGENT = "user-agent";

    static {
        Logger.initLogSystem(true);
    }

    private static final String USAGE = "Usage: WAPClient <WAP-Gateway-address[:port]> [GET/POST] [options] <URL>\n" + "   if method (GET/POST) is unspecified, GET is assumed\n\n" + "   Common options:\n" + "      -u <user-agent>   The User-Agent (defaults to jWAP/1.x)\n" + "      -o <file>         write response to file\n" + "      -l [addr]:port    local port (and address) to bind to\n" + "      -tc <timeout>     connection timeout (seconds, default=30)\n" + "      -tx <timeout>     request timeout (seconds, default=180)\n" + "      -v                show response-headers\n\n" + "   POST options:\n" + "      -c <content-type> The content-type  of the response body\n" + "      -p <file>         A file containing the post data, use '-' to read" + " the post data from standard input";

    private static final String DEFAULT_CONTENT_TYPE = "application/unknown";

    private static final String CONNECTED = "CONNECTED";

    private InetAddress gwAddr;

    private InetAddress localAddr;

    private int gwPort;

    private int localPort;

    private CWSPSession session;

    private long disconnectTimeout;

    private byte[] sessionLock = new byte[0];

    private IWSPUpperLayer2 upperLayerImpl;

    private Hashtable pendingRequests;

    private WAPClient() {
    }

    /**
     * Construct a new WAP Client
     * @param wapGateway hostname of the WAP gateway to use
     * @param port port-number
     * @throws UnknownHostException if the hostname cannot be resolved
     */
    public WAPClient(String wapGateway, int port) throws UnknownHostException {
        this(InetAddress.getByName(wapGateway), port);
    }

    /**
     * Construct a new WAP Client
     * @param wapGateway the address of the WAP gateway to use
     * @param port the WAP gateway port number
     */
    public WAPClient(InetAddress wapGateway, int port) {
        this(wapGateway, port, null, CWTPSocket.DEFAULT_PORT);
    }

    /**
     * Construct a new WAP Client
     * @param wapGateway the addresss of the WAP gateway to use
     * @param wapPort the WAP gateway port number
     * @param localAddress the local address to bind to
     * @param localPort the local port to bind to (0 to let the OS pick a free port) 
     */
    public WAPClient(InetAddress wapGateway, int wapPort, InetAddress localAddress, int localPort) {
        gwAddr = wapGateway;
        gwPort = wapPort;
        this.localAddr = localAddress;
        this.localPort = localPort;
        upperLayerImpl = new UpperLayerImpl();
        pendingRequests = new Hashtable();
    }

    /**
     * Execute a request. The client must be connected to the gateway.
     * @param request the request to execute
     * @return the response
     * @throws SocketException if a timeout occurred
     * @throws IllegalStateException if the client is not connected
     */
    public Response execute(Request request) throws SocketException, IllegalStateException {
        return execute(request, DEFAULT_EXEC_TIMEOUT);
    }

    /**
     * Execute a request. The client must be connected to the gateway.
     * @param request the request to execute
     * @param timeout timeout in milliseconds 
     * @return the response
     * @throws SocketException if a timeout occurred
     * @throws IllegalStateException if the client is not connected
     */
    public Response execute(Request request, long timeout) throws SocketException, IllegalStateException {
        CWSPMethodManager mgr = null;
        synchronized (this) {
            if (session == null) {
                throw new IllegalStateException("Not yet connected");
            }
            CWSPHeaders headers = request.getWSPHeaders();
            if (headers.getHeader("accept") == null) {
                headers.setHeader("accept", "*/*");
            }
            String uh = headers.getHeader(USER_AGENT);
            if (uh == null) {
                headers.setHeader(USER_AGENT, "jWAP/1.2");
            } else if ("".equals(uh)) {
                headers.setHeader(USER_AGENT, null);
            }
            if (request instanceof GetRequest) {
                if (log.isDebugEnabled()) {
                    log.debug("Executing GET Request for URL " + request.getURL());
                }
                mgr = session.s_get(headers, request.getURL());
            } else if (request instanceof PostRequest) {
                if (log.isDebugEnabled()) {
                    log.debug("Executing POST Request for URL " + request.getURL());
                }
                PostRequest post = (PostRequest) request;
                mgr = session.s_post(post.getWSPHeaders(), post.getRequestBody(), post.getContentType(), post.getURL());
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Waiting " + timeout + "ms for execute completion...");
        }
        Response response = (Response) waitForCompletion(mgr, timeout);
        if (response == null) {
            throw new SocketException("Timeout executing request");
        }
        return response;
    }

    /**
     * Connect to the WAP gateway. Before requests can be executed, this method
     * must be called.
     * @throws SocketException if the connection could not be established
     * @throws IllegalStateException if the client is already connected
     */
    public synchronized void connect() throws SocketException, IllegalStateException {
        connect(DEFAULT_CONNECT_TIMEOUT);
    }

    /**
     * Connect to the WAP gateway. Before requests can be executed, this method
     * must be called.
     * @param timeout timeout in milliseconds
     * @throws SocketException if the connection could not be established
     * @throws IllegalStateException if the client is already connected
     */
    public synchronized void connect(long timeout) throws SocketException, IllegalStateException {
        connect(null, timeout);
    }

    /**
     * Connect to the WAP gateway. Before requests can be executed, this method
     * must be called.
     * @param timeout timeout in milliseconds
     * @param headers WSP headers used for connect or null 
     *   objects. The headers will be encoded using the default WAP codepage. 
     * @throws SocketException if the connection could not be established
     * @throws IllegalStateException if the client is already connected
     */
    public synchronized void connect(CWSPHeaders headers, long timeout) throws SocketException, IllegalStateException {
        if (session != null) {
            throw new IllegalStateException("Already connected");
        }
        disconnectTimeout = timeout;
        pendingRequests.clear();
        if (log.isDebugEnabled()) {
            log.debug("Establishing WSP session with " + gwAddr.getHostAddress() + ":" + gwPort);
        }
        session = new CWSPSession(gwAddr, gwPort, localAddr, localPort, upperLayerImpl, false);
        session.s_connect(headers);
        Object result = waitForCompletion(sessionLock, timeout);
        if (result == null) {
            CWSPSession ts = session;
            session = null;
            try {
                ts.s_disconnect();
            } catch (Exception unknown) {
            }
            throw new SocketException("connect: Timeout occurred");
        }
        if (result != null) {
            if (result instanceof CWSPSocketAddress[]) {
                CWSPSocketAddress[] addr = (CWSPSocketAddress[]) result;
                if (addr.length > 0) {
                    gwAddr = addr[0].getAddress();
                    int p = addr[0].getPort();
                    if (p > 0) {
                        gwPort = p;
                    }
                    session = null;
                    if (log.isDebugEnabled()) {
                        log.debug("Redirect to " + gwAddr.getHostAddress() + ":" + gwPort);
                    }
                    connect(headers, timeout);
                    return;
                }
            } else if (!CONNECTED.equals(result)) {
                CWSPSession ts = session;
                session = null;
                ts.s_disconnect();
                if (result == null) {
                    throw new SocketException("Timeout while establishing connection");
                } else if (!CONNECTED.equals(result)) {
                    throw new SocketException("Connection failed.");
                }
            }
        }
        log.debug("Connection established, Session-ID: " + session.getSessionID());
    }

    /**
     * Disconnect from the WAP gateway. This releases used resources as well. 
     */
    public synchronized void disconnect() {
        if (session == null) {
            return;
        }
        log.debug("Disconnecting client...");
        CWSPSession ts = session;
        session = null;
        ts.s_disconnect();
        waitForCompletion(sessionLock, disconnectTimeout);
        session = null;
        log.debug("Client disconnected...");
    }

    /**
     * Check if the client is currently connected to the WAP gateway
     * @return true if the client is connected, false otherwise
     */
    public synchronized boolean isConnected() {
        return session != null;
    }

    /**
     * Execute a WSP GET request.
     * 
     * <pre>
     * Usage: WAPClient &lt;WAP-Gateway-address[:port]&gt; [GET/POST] [options] &lt;URL&gt;
     *   if method (GET/POST) is unspecified, GET is assumed
     * 
     *   Common options:
     *   -u <user-agent>   The User-Agent (defaults to jWAP/1.x)
     *   -o <file>         write response to file
     *   -v                show response-headers
     * 
     * POST options:
     *   -c <content-type> The content-type  of the response body
     *   -p <file>         A file containing the post data, use '-' to read the post data from standard input
     * </pre>
     */
    public static void main(String[] args) throws IOException {
        int exitCode = 0;
        if (args.length < 2) {
            System.err.println(USAGE);
            System.exit(1);
        }
        String wapGW = args[0];
        int wapPort = 9201;
        int c = wapGW.indexOf(':');
        if (c > 0) {
            wapPort = Integer.parseInt(wapGW.substring(c + 1));
            wapGW = wapGW.substring(0, c);
        }
        int argp = 1;
        String method = args[argp];
        String userAgent = null;
        String output = null;
        boolean showHeaders = false;
        String contentType = null;
        String input = null;
        String url = null;
        String locaddr = null;
        InetAddress la = null;
        int lp = CWTPSocket.DEFAULT_PORT;
        long tc = DEFAULT_CONNECT_TIMEOUT;
        long tx = DEFAULT_EXEC_TIMEOUT;
        if ("GET".equals(method) || "POST".equals(method)) {
            argp++;
        }
        try {
            while (url == null && argp < args.length) {
                String arg = args[argp++];
                if ("-u".equals(arg)) {
                    userAgent = args[argp++];
                } else if ("-o".equals(arg)) {
                    output = args[argp++];
                } else if ("-v".equals(arg)) {
                    showHeaders = true;
                } else if ("-l".equals(arg)) {
                    locaddr = args[argp++];
                } else if ("-c".equals(arg)) {
                    contentType = args[argp++];
                } else if ("-p".equals(arg)) {
                    input = args[argp++];
                } else if ("-tc".equals(arg)) {
                    tc = Integer.parseInt(args[argp++]) * 1000;
                } else if ("-tx".equals(arg)) {
                    tx = Integer.parseInt(args[argp++]) * 1000;
                } else if (arg.startsWith("-")) {
                    System.err.println(arg + ": Unknown option");
                    System.err.println(USAGE);
                    System.exit(1);
                } else {
                    url = arg;
                }
            }
        } catch (Exception unknown) {
            System.err.println(USAGE);
            System.exit(1);
        }
        if (url == null) {
            System.err.println("Error: <URL> is mandatory");
            System.err.println(USAGE);
            System.exit(1);
        }
        if (locaddr != null) {
            locaddr = locaddr.trim();
            int p = locaddr.lastIndexOf(':');
            if (p >= 0) {
                lp = Integer.parseInt(locaddr.substring(p + 1));
                locaddr = locaddr.substring(0, p);
            }
            if (!"".equals(locaddr)) {
                la = InetAddress.getByName(locaddr);
            }
        }
        WAPClient client = new WAPClient(InetAddress.getByName(wapGW), wapPort, la, lp);
        Request request = null;
        OutputStream out = null;
        if (output == null || "-".equals(output)) {
            out = System.out;
        } else {
            out = new FileOutputStream(output);
        }
        if ("POST".equals(method)) {
            if (contentType == null) {
                System.err.println("Warning: no content-type specified, assuming " + DEFAULT_CONTENT_TYPE);
                contentType = DEFAULT_CONTENT_TYPE;
            }
            byte[] postData = readPostData(input);
            PostRequest preq = new PostRequest(url);
            request = preq;
            preq.setContentType(contentType);
            preq.setRequestBody(postData);
        } else {
            request = new GetRequest(url);
        }
        try {
            CWSPHeaders hdr = null;
            if (userAgent != null) {
                hdr = new CWSPHeaders();
                hdr.setHeader(USER_AGENT, userAgent);
                request.setHeader(USER_AGENT, userAgent);
            }
            client.connect(hdr, tc);
            Response response = client.execute(request, tx);
            if (!response.isSuccess()) {
                exitCode = 2;
            }
            if (out == System.out) {
                System.out.println("");
            }
            if (showHeaders || !response.isSuccess()) {
                System.err.println("Status: " + response.getStatus() + " " + response.getStatusText());
                for (Enumeration e = response.getHeaderNames(); e.hasMoreElements(); ) {
                    String key = (String) e.nextElement();
                    for (Enumeration e2 = response.getHeaders(key); e2.hasMoreElements(); ) {
                        String val = e2.nextElement().toString();
                        System.err.println(key + ": " + val);
                    }
                }
                System.err.println();
            }
            out.write(response.getResponseBody());
        } finally {
            client.disconnect();
            if (out != null && out == System.out) {
                out.close();
            }
        }
        System.exit(exitCode);
    }

    private static byte[] readPostData(String input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = null;
        if (input == null) {
            System.out.println("Reading post-data from input stream, hit EOF when done");
            in = System.in;
        } else if ("-".equals(input)) {
            in = System.in;
        } else {
            in = new FileInputStream(input);
        }
        byte[] buf = new byte[1024];
        int read = 0;
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
        }
        in.close();
        return out.toByteArray();
    }

    private Object waitForCompletion(Object key, long timeout) {
        Object object = null;
        long startAt = 0;
        if (timeout > 0) {
            startAt = System.currentTimeMillis();
        }
        while (object == null) {
            if (timeout > 0 && (startAt + timeout) < System.currentTimeMillis()) {
                log.debug("Timeout occurred");
                break;
            }
            synchronized (pendingRequests) {
                object = pendingRequests.remove(key);
                if (object == null) {
                    try {
                        pendingRequests.wait(timeout);
                    } catch (InterruptedException e) {
                        log.warn("Interrupted");
                    }
                }
            }
        }
        return object;
    }

    private void complete(Object key, Object value) {
        synchronized (pendingRequests) {
            pendingRequests.put(key, value);
            pendingRequests.notifyAll();
        }
    }

    private class UpperLayerImpl implements IWSPUpperLayer2 {

        public void s_connect_cnf() {
            complete(sessionLock, CONNECTED);
        }

        public void s_disconnect_ind(short reason) {
            if (log.isDebugEnabled()) {
                log.debug("s_disconnect_ind(" + reason + ")");
            }
            complete(sessionLock, "DISCONNECTED: " + reason);
            session = null;
        }

        public void s_disconnect_ind(CWSPSocketAddress[] redirectInfo) {
            complete(sessionLock, redirectInfo);
        }

        public void s_methodResult_ind(CWSPResult result) {
            Response response = new Response(result);
            CWSPMethodManager mgr = result.getMethodManager();
            mgr.s_methodResult(null);
            complete(mgr, response);
        }

        public void s_suspend_ind(short reason) {
            if (log.isDebugEnabled()) {
                log.debug("s_suspend_ind(" + reason + ")");
            }
        }

        public void s_resume_cnf() {
            log.debug("s_resume_cnf()");
        }

        public void s_disconnect_ind(InetAddress[] redirectInfo) {
        }

        public void s_methodResult_ind(byte[] payload, String contentType, boolean moreData) {
        }
    }
}
