package org.openqa.selenium.server;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;
import org.apache.commons.logging.*;
import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.mortbay.jetty.*;
import org.mortbay.log.LogFactory;
import org.mortbay.util.*;
import org.mortbay.util.URI;
import org.openqa.selenium.server.browserlaunchers.*;
import org.openqa.selenium.server.commands.CaptureNetworkTrafficCommand;
import org.openqa.selenium.server.commands.AddCustomRequestHeaderCommand;
import cybervillains.ca.*;

/**
 * Proxy request handler. A HTTP/1.1 Proxy. This implementation uses the JVMs URL implementation to
 * make proxy requests.
 * <p/>
 * The HttpTunnel mechanism is also used to implement the CONNECT method.
 *
 * @author Greg Wilkins (gregw)
 * @author giacof@tiscali.it (chained proxy)
 * @version $Id: ProxyHandler.java,v 1.34 2005/10/05 13:32:59 gregwilkins Exp $
 */
public class ProxyHandler extends AbstractHttpHandler {

    private static Log log = LogFactory.getLog(ProxyHandler.class);

    protected Set<String> _proxyHostsWhiteList;

    protected Set<String> _proxyHostsBlackList;

    protected int _tunnelTimeoutMs = 250;

    private boolean _anonymous = false;

    private transient boolean _chained = false;

    private final Map<String, SslRelay> _sslMap = new LinkedHashMap<String, SslRelay>();

    @SuppressWarnings("unused")
    private String sslKeystorePath;

    private boolean useCyberVillains = true;

    private boolean trustAllSSLCertificates = false;

    private final String dontInjectRegex;

    private final String debugURL;

    private final boolean proxyInjectionMode;

    private final boolean forceProxyChain;

    private boolean fakeCertsGenerated;

    private Object shutdownLock;

    /**
     * Map of leg by leg headers (not end to end). Should be a set, but more efficient string map is
     * used instead.
     */
    protected StringMap _DontProxyHeaders = new StringMap();

    {
        Object o = new Object();
        _DontProxyHeaders.setIgnoreCase(true);
        _DontProxyHeaders.put(HttpFields.__ProxyConnection, o);
        _DontProxyHeaders.put(HttpFields.__Connection, o);
        _DontProxyHeaders.put(HttpFields.__KeepAlive, o);
        _DontProxyHeaders.put(HttpFields.__TransferEncoding, o);
        _DontProxyHeaders.put(HttpFields.__TE, o);
        _DontProxyHeaders.put(HttpFields.__Trailer, o);
        _DontProxyHeaders.put(HttpFields.__Upgrade, o);
    }

    /**
     * Map of leg by leg headers (not end to end). Should be a set, but more efficient string map is
     * used instead.
     */
    protected StringMap _ProxyAuthHeaders = new StringMap();

    {
        Object o = new Object();
        _ProxyAuthHeaders.put(HttpFields.__ProxyAuthorization, o);
        _ProxyAuthHeaders.put(HttpFields.__ProxyAuthenticate, o);
    }

    /**
     * Map of allows schemes to proxy Should be a set, but more efficient string map is used
     * instead.
     */
    protected StringMap _ProxySchemes = new StringMap();

    {
        Object o = new Object();
        _ProxySchemes.setIgnoreCase(true);
        _ProxySchemes.put(HttpMessage.__SCHEME, o);
        _ProxySchemes.put(HttpMessage.__SSL_SCHEME, o);
        _ProxySchemes.put("ftp", o);
    }

    /**
     * Set of allowed CONNECT ports.
     */
    protected HashSet<Integer> _allowedConnectPorts = new HashSet<Integer>();

    {
        _allowedConnectPorts.add(80);
        _allowedConnectPorts.add(RemoteControlConfiguration.getDefaultPort());
        _allowedConnectPorts.add(8000);
        _allowedConnectPorts.add(8080);
        _allowedConnectPorts.add(8888);
        _allowedConnectPorts.add(443);
        _allowedConnectPorts.add(8443);
    }

    public ProxyHandler(boolean trustAllSSLCertificates, String dontInjectRegex, String debugURL, boolean proxyInjectionMode, boolean forceProxyChain) {
        super();
        this.trustAllSSLCertificates = trustAllSSLCertificates;
        this.dontInjectRegex = dontInjectRegex;
        this.debugURL = debugURL;
        this.proxyInjectionMode = proxyInjectionMode;
        this.forceProxyChain = forceProxyChain;
    }

    public void start() throws Exception {
        _chained = System.getProperty("http.proxyHost") != null || forceProxyChain;
        super.start();
    }

    /**
     * Get proxy host white list.
     *
     * @return Array of hostnames and IPs that are proxied, or an empty array if all hosts are
     *         proxied.
     */
    public String[] getProxyHostsWhiteList() {
        if (_proxyHostsWhiteList == null || _proxyHostsWhiteList.size() == 0) return new String[0];
        String[] hosts = new String[_proxyHostsWhiteList.size()];
        hosts = _proxyHostsWhiteList.toArray(hosts);
        return hosts;
    }

    /**
     * Set proxy host white list.
     *
     * @param hosts Array of hostnames and IPs that are proxied, or null if all hosts are proxied.
     */
    public void setProxyHostsWhiteList(String[] hosts) {
        if (hosts == null || hosts.length == 0) _proxyHostsWhiteList = null; else {
            _proxyHostsWhiteList = new HashSet<String>();
            for (int i = 0; i < hosts.length; i++) {
                String host = hosts[i];
                if (host != null && host.trim().length() > 0) _proxyHostsWhiteList.add(host);
            }
        }
    }

    /**
     * Get proxy host black list.
     *
     * @return Array of hostnames and IPs that are NOT proxied.
     */
    public String[] getProxyHostsBlackList() {
        if (_proxyHostsBlackList == null || _proxyHostsBlackList.size() == 0) return new String[0];
        String[] hosts = new String[_proxyHostsBlackList.size()];
        hosts = _proxyHostsBlackList.toArray(hosts);
        return hosts;
    }

    /**
     * Set proxy host black list.
     *
     * @param hosts Array of hostnames and IPs that are NOT proxied.
     */
    public void setProxyHostsBlackList(String[] hosts) {
        if (hosts == null || hosts.length == 0) _proxyHostsBlackList = null; else {
            _proxyHostsBlackList = new HashSet<String>();
            for (int i = 0; i < hosts.length; i++) {
                String host = hosts[i];
                if (host != null && host.trim().length() > 0) _proxyHostsBlackList.add(host);
            }
        }
    }

    public int getTunnelTimeoutMs() {
        return _tunnelTimeoutMs;
    }

    /**
     * Tunnel timeout. IE on win2000 has connections issues with normal timeout handling. This
     * timeout should be set to a low value that will expire to allow IE to see the end of the
     * tunnel connection.
     */
    public void setTunnelTimeoutMs(int ms) {
        _tunnelTimeoutMs = ms;
    }

    public void handle(String pathInContext, String pathParams, HttpRequest request, HttpResponse response) throws HttpException, IOException {
        URI uri = request.getURI();
        if (HttpRequest.__CONNECT.equalsIgnoreCase(request.getMethod())) {
            response.setField(HttpFields.__Connection, "close");
            handleConnect(pathInContext, pathParams, request, response);
            return;
        }
        try {
            if ("True".equals(response.getAttribute("NotFound"))) {
                response.removeAttribute("NotFound");
                sendNotFound(response);
                return;
            }
            URL url = isProxied(uri);
            if (url == null) {
                if (isForbidden(uri)) sendForbid(request, response, uri);
                return;
            }
            if (isSeleniumUrl(url.toString())) {
                request.setHandled(false);
                return;
            }
            proxyPlainTextRequest(url, pathInContext, pathParams, request, response);
        } catch (Exception e) {
            log.debug("Could not proxy " + uri, e);
            LogSupport.ignore(log, e);
            if (!response.isCommitted()) response.sendError(HttpResponse.__400_Bad_Request, "Could not proxy " + uri + "\n" + e);
        }
    }

    private boolean isSeleniumUrl(String url) {
        int slashSlash = url.indexOf("//");
        if (slashSlash == -1) {
            return false;
        }
        int nextSlash = url.indexOf("/", slashSlash + 2);
        if (nextSlash == -1) {
            return false;
        }
        int seleniumServer = url.indexOf("/selenium-server/");
        if (seleniumServer == -1) {
            return false;
        }
        return seleniumServer == nextSlash;
    }

    protected long proxyPlainTextRequest(URL url, String pathInContext, String pathParams, HttpRequest request, HttpResponse response) throws IOException {
        CaptureNetworkTrafficCommand.Entry entry = new CaptureNetworkTrafficCommand.Entry(request.getMethod(), url.toString());
        entry.addRequestHeaders(request);
        if (log.isDebugEnabled()) log.debug("PROXY URL=" + url);
        URLConnection connection = url.openConnection();
        connection.setAllowUserInteraction(false);
        if (proxyInjectionMode) {
            adjustRequestForProxyInjection(request, connection);
        }
        HttpURLConnection http = null;
        if (connection instanceof HttpURLConnection) {
            http = (HttpURLConnection) connection;
            http.setRequestMethod(request.getMethod());
            http.setInstanceFollowRedirects(false);
            if (trustAllSSLCertificates && connection instanceof HttpsURLConnection) {
                TrustEverythingSSLTrustManager.trustAllSSLCertificates((HttpsURLConnection) connection);
            }
        }
        String connectionHdr = request.getField(HttpFields.__Connection);
        if (connectionHdr != null && (connectionHdr.equalsIgnoreCase(HttpFields.__KeepAlive) || connectionHdr.equalsIgnoreCase(HttpFields.__Close))) connectionHdr = null;
        boolean xForwardedFor = false;
        boolean isGet = "GET".equals(request.getMethod());
        boolean hasContent = false;
        Enumeration enm = request.getFieldNames();
        while (enm.hasMoreElements()) {
            String hdr = (String) enm.nextElement();
            if (_DontProxyHeaders.containsKey(hdr) || !_chained && _ProxyAuthHeaders.containsKey(hdr)) continue;
            if (connectionHdr != null && connectionHdr.indexOf(hdr) >= 0) continue;
            if (!isGet && HttpFields.__ContentType.equals(hdr)) hasContent = true;
            Enumeration vals = request.getFieldValues(hdr);
            while (vals.hasMoreElements()) {
                String val = (String) vals.nextElement();
                if (val != null) {
                    if ("Referer".equals(hdr) && (-1 != val.indexOf("/selenium-server/"))) {
                        continue;
                    }
                    if (!isGet && HttpFields.__ContentLength.equals(hdr) && Integer.parseInt(val) > 0) {
                        hasContent = true;
                    }
                    connection.addRequestProperty(hdr, val);
                    xForwardedFor |= HttpFields.__XForwardedFor.equalsIgnoreCase(hdr);
                }
            }
        }
        Map<String, String> customRequestHeaders = AddCustomRequestHeaderCommand.getHeaders();
        for (Map.Entry<String, String> e : customRequestHeaders.entrySet()) {
            connection.addRequestProperty(e.getKey(), e.getValue());
            entry.addRequestHeader(e.getKey(), e.getValue());
        }
        if (!_anonymous) connection.setRequestProperty("Via", "1.1 (jetty)");
        if (!xForwardedFor) connection.addRequestProperty(HttpFields.__XForwardedFor, request.getRemoteAddr());
        String cache_control = request.getField(HttpFields.__CacheControl);
        if (cache_control != null && (cache_control.indexOf("no-cache") >= 0 || cache_control.indexOf("no-store") >= 0)) connection.setUseCaches(false);
        customizeConnection(pathInContext, pathParams, request, connection);
        try {
            connection.setDoInput(true);
            InputStream in = request.getInputStream();
            if (hasContent) {
                connection.setDoOutput(true);
                IO.copy(in, connection.getOutputStream());
            }
            connection.connect();
        } catch (Exception e) {
            LogSupport.ignore(log, e);
        }
        InputStream proxy_in = null;
        int code = -1;
        if (http != null) {
            proxy_in = http.getErrorStream();
            try {
                code = http.getResponseCode();
            } catch (SSLHandshakeException e) {
                throw new RuntimeException("Couldn't establish SSL handshake.  Try using trustAllSSLCertificates.\n" + e.getLocalizedMessage(), e);
            }
            response.setStatus(code);
            response.setReason(http.getResponseMessage());
            String contentType = http.getContentType();
            if (log.isDebugEnabled()) {
                log.debug("Content-Type is: " + contentType);
            }
        }
        if (proxy_in == null) {
            try {
                proxy_in = connection.getInputStream();
            } catch (Exception e) {
                LogSupport.ignore(log, e);
                proxy_in = http.getErrorStream();
            }
        }
        response.removeField(HttpFields.__Date);
        response.removeField(HttpFields.__Server);
        int h = 0;
        String hdr = connection.getHeaderFieldKey(h);
        String val = connection.getHeaderField(h);
        while (hdr != null || val != null) {
            if (hdr != null && val != null && !_DontProxyHeaders.containsKey(hdr) && (_chained || !_ProxyAuthHeaders.containsKey(hdr))) response.addField(hdr, val);
            h++;
            hdr = connection.getHeaderFieldKey(h);
            val = connection.getHeaderField(h);
        }
        if (!_anonymous) response.setField("Via", "1.1 (jetty)");
        response.removeField(HttpFields.__ETag);
        response.removeField(HttpFields.__LastModified);
        long bytesCopied = -1;
        request.setHandled(true);
        if (proxy_in != null) {
            boolean injectableResponse = http.getResponseCode() == HttpURLConnection.HTTP_OK || (http.getResponseCode() >= 400 && http.getResponseCode() < 600);
            if (proxyInjectionMode && injectableResponse) {
                if (shouldInject(request.getPath())) {
                    bytesCopied = InjectionHelper.injectJavaScript(request, response, proxy_in, response.getOutputStream(), debugURL);
                } else {
                    bytesCopied = ModifiedIO.copy(proxy_in, response.getOutputStream());
                }
            } else {
                bytesCopied = ModifiedIO.copy(proxy_in, response.getOutputStream());
            }
        }
        entry.finish(code, bytesCopied);
        entry.addResponseHeader(response);
        CaptureNetworkTrafficCommand.capture(entry);
        return bytesCopied;
    }

    public boolean shouldInject(String path) {
        if (dontInjectRegex == null) {
            return true;
        }
        return !path.matches(dontInjectRegex);
    }

    private void adjustRequestForProxyInjection(HttpRequest request, URLConnection connection) {
        request.setState(HttpMessage.__MSG_EDITABLE);
        if (request.containsField("If-Modified-Since")) {
            request.removeField("If-Modified-Since");
            request.removeField("If-None-Match");
            connection.setUseCaches(false);
        }
        request.removeField("Accept-Encoding");
        request.setState(HttpMessage.__MSG_RECEIVED);
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        HttpContext httpContext = new HttpContext();
        httpContext.setContextPath("/");
        ProxyHandler proxy = new ProxyHandler(true, "", "", false, false);
        proxy.useCyberVillains = false;
        httpContext.addHandler(proxy);
        server.addContext(httpContext);
        SocketListener listener = new SocketListener();
        listener.setPort(4444);
        server.addListener(listener);
        server.start();
    }

    public synchronized void generateSSLCertsForLoggingHosts(HttpServer server) {
        if (fakeCertsGenerated) return;
        log.info("Creating 16 fake SSL servers for browser side logging");
        for (int i = 1; i <= 16; i++) {
            String uri = i + ".selenium.doesnotexist:443";
            try {
                getSslRelayOrCreateNew(new URI(uri), new InetAddrPort(443), server);
            } catch (Exception e) {
                log.error("Could not pre-create logging SSL relay for " + uri, e);
            }
        }
        fakeCertsGenerated = true;
    }

    public void handleConnect(String pathInContext, String pathParams, HttpRequest request, HttpResponse response) throws HttpException, IOException {
        URI uri = request.getURI();
        try {
            if (log.isDebugEnabled()) {
                log.debug("CONNECT: " + uri);
            }
            InetAddrPort addrPort;
            if (uri.toString().endsWith(".selenium.doesnotexist:443")) {
                addrPort = new InetAddrPort(443);
            } else {
                addrPort = new InetAddrPort(uri.toString());
            }
            if (isForbidden(HttpMessage.__SSL_SCHEME, addrPort.getHost(), addrPort.getPort(), false)) {
                sendForbid(request, response, uri);
            } else {
                HttpConnection http_connection = request.getHttpConnection();
                http_connection.forceClose();
                HttpServer server = http_connection.getHttpServer();
                SslRelay listener = getSslRelayOrCreateNew(uri, addrPort, server);
                int port = listener.getPort();
                int timeoutMs = 30000;
                Object maybesocket = http_connection.getConnection();
                if (maybesocket instanceof Socket) {
                    Socket s = (Socket) maybesocket;
                    timeoutMs = s.getSoTimeout();
                }
                HttpTunnel tunnel = newHttpTunnel(request, response, InetAddress.getLocalHost(), port, timeoutMs);
                if (tunnel != null) {
                    if (_tunnelTimeoutMs > 0) {
                        tunnel.getSocket().setSoTimeout(_tunnelTimeoutMs);
                        if (maybesocket instanceof Socket) {
                            Socket s = (Socket) maybesocket;
                            s.setSoTimeout(_tunnelTimeoutMs);
                        }
                    }
                    tunnel.setTimeoutMs(timeoutMs);
                    customizeConnection(pathInContext, pathParams, request, tunnel.getSocket());
                    request.getHttpConnection().setHttpTunnel(tunnel);
                    response.setStatus(HttpResponse.__200_OK);
                    response.setContentLength(0);
                }
                request.setHandled(true);
            }
        } catch (Exception e) {
            log.debug("error during handleConnect", e);
            response.sendError(HttpResponse.__500_Internal_Server_Error, e.toString());
        }
    }

    private SslRelay getSslRelayOrCreateNew(URI uri, InetAddrPort addrPort, HttpServer server) throws Exception {
        SslRelay listener;
        synchronized (_sslMap) {
            listener = _sslMap.get(uri.toString());
            if (listener == null) {
                String host = new URL("https://" + uri.toString()).getHost();
                listener = new SslRelay(addrPort);
                if (useCyberVillains) {
                    wireUpSslWithCyberVilliansCA(host, listener);
                } else {
                    wireUpSslWithRemoteService(host, listener);
                }
                listener.setPassword("password");
                listener.setKeyPassword("password");
                server.addListener(listener);
                synchronized (shutdownLock) {
                    try {
                        if (server.isStarted()) {
                            listener.start();
                        } else {
                            throw new RuntimeException("Can't start SslRelay: server is not started (perhaps it was just shut down?)");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
                _sslMap.put(uri.toString(), listener);
            }
        }
        return listener;
    }

    private void wireUpSslWithRemoteService(String host, SslRelay listener) throws IOException {
        File keystore = File.createTempFile("selenium-rc-" + host, "keystore");
        String urlString = "http://dangerous-certificate-authority.openqa.org/genkey.jsp?padding=" + _sslMap.size() + "&domain=" + host;
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();
        conn.connect();
        InputStream is = conn.getInputStream();
        byte[] buffer = new byte[1024];
        int length;
        FileOutputStream fos = new FileOutputStream(keystore);
        while ((length = is.read(buffer)) != -1) {
            fos.write(buffer, 0, length);
        }
        fos.close();
        is.close();
        listener.setKeystore(keystore.getAbsolutePath());
        listener.setNukeDirOrFile(keystore);
    }

    private void wireUpSslWithCyberVilliansCA(String host, SslRelay listener) {
        try {
            File root = File.createTempFile("seleniumSslSupport", host);
            root.delete();
            root.mkdirs();
            ResourceExtractor.extractResourcePath(getClass(), "/sslSupport", root);
            KeyStoreManager mgr = new KeyStoreManager(root);
            mgr.getCertificateByHostname(host);
            mgr.getKeyStore().deleteEntry(KeyStoreManager._caPrivKeyAlias);
            mgr.persist();
            listener.setKeystore(new File(root, "cybervillainsCA.jks").getAbsolutePath());
            listener.setNukeDirOrFile(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected HttpTunnel newHttpTunnel(HttpRequest request, HttpResponse response, InetAddress iaddr, int port, int timeoutMS) throws IOException {
        try {
            Socket socket = null;
            InputStream in = null;
            String chained_proxy_host = System.getProperty("http.proxyHost");
            if (chained_proxy_host == null) {
                socket = new Socket(iaddr, port);
                socket.setSoTimeout(timeoutMS);
                socket.setTcpNoDelay(true);
            } else {
                int chained_proxy_port = Integer.getInteger("http.proxyPort", 8888).intValue();
                Socket chain_socket = new Socket(chained_proxy_host, chained_proxy_port);
                chain_socket.setSoTimeout(timeoutMS);
                chain_socket.setTcpNoDelay(true);
                if (log.isDebugEnabled()) log.debug("chain proxy socket=" + chain_socket);
                LineInput line_in = new LineInput(chain_socket.getInputStream());
                byte[] connect = request.toString().getBytes(org.mortbay.util.StringUtil.__ISO_8859_1);
                chain_socket.getOutputStream().write(connect);
                String chain_response_line = line_in.readLine();
                HttpFields chain_response = new HttpFields();
                chain_response.read(line_in);
                int space0 = chain_response_line.indexOf(' ');
                if (space0 > 0 && space0 + 1 < chain_response_line.length()) {
                    int space1 = chain_response_line.indexOf(' ', space0 + 1);
                    if (space1 > space0) {
                        int code = Integer.parseInt(chain_response_line.substring(space0 + 1, space1));
                        if (code >= 200 && code < 300) {
                            socket = chain_socket;
                            in = line_in;
                        } else {
                            Enumeration iter = chain_response.getFieldNames();
                            while (iter.hasMoreElements()) {
                                String name = (String) iter.nextElement();
                                if (!_DontProxyHeaders.containsKey(name)) {
                                    Enumeration values = chain_response.getValues(name);
                                    while (values.hasMoreElements()) {
                                        String value = (String) values.nextElement();
                                        response.setField(name, value);
                                    }
                                }
                            }
                            response.sendError(code);
                            if (!chain_socket.isClosed()) chain_socket.close();
                        }
                    }
                }
            }
            if (socket == null) return null;
            return new HttpTunnel(socket, in, null);
        } catch (IOException e) {
            log.debug(e);
            response.sendError(HttpResponse.__400_Bad_Request);
            return null;
        }
    }

    /**
     * Customize proxy Socket connection for CONNECT. Method to allow derived handlers to customize
     * the tunnel sockets.
     */
    protected void customizeConnection(String pathInContext, String pathParams, HttpRequest request, Socket socket) {
    }

    /**
     * Customize proxy URL connection. Method to allow derived handlers to customize the connection.
     */
    protected void customizeConnection(String pathInContext, String pathParams, HttpRequest request, URLConnection connection) {
    }

    /**
     * Is URL Proxied. Method to allow derived handlers to select which URIs are proxied and to
     * where.
     *
     * @param uri The requested URI, which should include a scheme, host and port.
     * @return The URL to proxy to, or null if the passed URI should not be proxied. The default
     *         implementation returns the passed uri if isForbidden() returns true.
     */
    protected URL isProxied(URI uri) throws MalformedURLException {
        if (isForbidden(uri)) return null;
        return new URL(uri.toString());
    }

    /**
     * Is URL Forbidden.
     *
     * @return True if the URL is not forbidden. Calls isForbidden(scheme,host,port,true);
     */
    protected boolean isForbidden(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        return isForbidden(scheme, host, port, true);
    }

    /**
     * Is scheme,host & port Forbidden.
     *
     * @param scheme           A scheme that mast be in the proxySchemes StringMap.
     * @param host             A host that must pass the white and black lists
     * @param port             A port that must in the allowedConnectPorts Set
     * @param openNonPrivPorts If true ports greater than 1024 are allowed.
     * @return True if the request to the scheme,host and port is not forbidden.
     */
    protected boolean isForbidden(String scheme, String host, int port, boolean openNonPrivPorts) {
        if (false) {
            if (port > 0 && !_allowedConnectPorts.contains(new Integer(port))) {
                if (!openNonPrivPorts || port <= 1024) return true;
            }
        }
        if (scheme == null || !_ProxySchemes.containsKey(scheme)) return true;
        if (_proxyHostsWhiteList != null && !_proxyHostsWhiteList.contains(host)) return true;
        return _proxyHostsBlackList != null && _proxyHostsBlackList.contains(host);
    }

    /**
     * Send Forbidden. Method called to send forbidden response. Default implementation calls
     * sendError(403)
     */
    protected void sendForbid(HttpRequest request, HttpResponse response, URI uri) throws IOException {
        response.sendError(HttpResponse.__403_Forbidden, "Forbidden for Proxy");
    }

    /**
     * Send not found. Method called to send not found response. Default implementation calls
     * sendError(404)
     */
    protected void sendNotFound(HttpResponse response) throws IOException {
        response.sendError(HttpResponse.__404_Not_Found, "Not found");
    }

    /**
     * @return Returns the anonymous.
     */
    public boolean isAnonymous() {
        return _anonymous;
    }

    /**
     * @param anonymous The anonymous to set.
     */
    public void setAnonymous(boolean anonymous) {
        _anonymous = anonymous;
    }

    public void setSslKeystorePath(String sslKeystorePath) {
        this.sslKeystorePath = sslKeystorePath;
    }

    public void setShutdownLock(Object shutdownLock) {
        this.shutdownLock = shutdownLock;
    }

    private static class SslRelay extends SslListener {

        InetAddrPort _addr;

        File nukeDirOrFile;

        SslRelay(InetAddrPort addr) {
            _addr = addr;
        }

        public void setNukeDirOrFile(File nukeDirOrFile) {
            this.nukeDirOrFile = nukeDirOrFile;
        }

        protected void customizeRequest(Socket socket, HttpRequest request) {
            super.customizeRequest(socket, request);
            URI uri = request.getURI();
            uri.setScheme("https");
            uri.setHost(_addr.getHost());
            uri.setPort(_addr.getPort());
        }

        public void stop() throws InterruptedException {
            super.stop();
            if (nukeDirOrFile != null) {
                if (nukeDirOrFile.isDirectory()) {
                    LauncherUtils.recursivelyDeleteDir(nukeDirOrFile);
                } else {
                    nukeDirOrFile.delete();
                }
            }
        }
    }
}
