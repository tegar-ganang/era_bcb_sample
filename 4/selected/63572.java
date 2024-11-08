package sun.net.www.protocol.http;

import java.net.URL;
import java.net.URLConnection;
import java.net.ProtocolException;
import java.net.PasswordAuthentication;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.io.*;
import java.util.Date;
import java.util.Map;
import java.util.Locale;
import java.util.StringTokenizer;
import sun.net.*;
import sun.net.www.*;
import sun.net.www.http.HttpClient;
import sun.net.www.http.PosterOutputStream;
import sun.net.www.http.ChunkedInputStream;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.net.MalformedURLException;

/**
 * A class to represent an HTTP connection to a remote object.
 */
public class HttpURLConnection extends java.net.HttpURLConnection {

    static final String version;

    public static final String userAgent;

    static final int defaultmaxRedirects = 20;

    static final int maxRedirects;

    static final boolean validateProxy;

    static final boolean validateServer;

    static {
        maxRedirects = ((Integer) java.security.AccessController.doPrivileged(new sun.security.action.GetIntegerAction("http.maxRedirects", defaultmaxRedirects))).intValue();
        version = (String) java.security.AccessController.doPrivileged(new sun.security.action.GetPropertyAction("java.version"));
        String agent = (String) java.security.AccessController.doPrivileged(new sun.security.action.GetPropertyAction("http.agent"));
        if (agent == null) {
            agent = "Java/" + version;
        } else {
            agent = agent + " Java/" + version;
        }
        userAgent = agent;
        validateProxy = ((Boolean) java.security.AccessController.doPrivileged(new sun.security.action.GetBooleanAction("http.auth.digest.validateProxy"))).booleanValue();
        validateServer = ((Boolean) java.security.AccessController.doPrivileged(new sun.security.action.GetBooleanAction("http.auth.digest.validateServer"))).booleanValue();
    }

    static final String httpVersion = "HTTP/1.1";

    static final String acceptString = "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2";

    private static final String[] EXCLUDE_HEADERS = { "Proxy-Authorization", "Authorization" };

    protected HttpClient http;

    protected Handler handler;

    protected PrintStream ps = null;

    private static HttpAuthenticator defaultAuth;

    private MessageHeader requests;

    String domain;

    DigestAuthentication.Parameters digestparams;

    AuthenticationInfo currentProxyCredentials = null;

    AuthenticationInfo currentServerCredentials = null;

    boolean needToCheck = true;

    private boolean doingNTLM2ndStage = false;

    private boolean doingNTLMp2ndStage = false;

    Object authObj;

    protected ProgressEntry pe;

    private MessageHeader responses;

    private InputStream inputStream = null;

    private PosterOutputStream poster = null;

    private boolean setRequests = false;

    private boolean failedOnce = false;

    private Exception rememberedException = null;

    private HttpClient reuseClient = null;

    private static PasswordAuthentication privilegedRequestPasswordAuthentication(final String host, final InetAddress addr, final int port, final String protocol, final String prompt, final String scheme) {
        return (PasswordAuthentication) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {

            public Object run() {
                return Authenticator.requestPasswordAuthentication(host, addr, port, protocol, prompt, scheme);
            }
        });
    }

    private void checkMessageHeader(String key, String value) {
        char LF = '\n';
        int index = key.indexOf(LF);
        if (index != -1) {
            throw new IllegalArgumentException("Illegal character(s) in message header field: " + key);
        } else {
            if (value == null) {
                return;
            }
            index = value.indexOf(LF);
            while (index != -1) {
                index++;
                if (index < value.length()) {
                    char c = value.charAt(index);
                    if ((c == ' ') || (c == '\t')) {
                        index = value.indexOf(LF, index);
                        continue;
                    }
                }
                throw new IllegalArgumentException("Illegal character(s) in message header value: " + value);
            }
        }
    }

    private void writeRequests() throws IOException {
        if (!setRequests) {
            if (!failedOnce) requests.prepend(method + " " + http.getURLFile() + " " + httpVersion, null);
            if (!getUseCaches()) {
                requests.setIfNotSet("Cache-Control", "no-cache");
                requests.setIfNotSet("Pragma", "no-cache");
            }
            requests.setIfNotSet("User-Agent", userAgent);
            int port = url.getPort();
            String host = url.getHost();
            if (port != -1 && port != 80) {
                host += ":" + String.valueOf(port);
            }
            requests.setIfNotSet("Host", host);
            requests.setIfNotSet("Accept", acceptString);
            if (!failedOnce && http.getHttpKeepAliveSet()) {
                if (http.usingProxy) {
                    requests.setIfNotSet("Proxy-Connection", "keep-alive");
                } else {
                    requests.setIfNotSet("Connection", "keep-alive");
                }
            }
            if (http.usingProxy) {
                setPreemptiveProxyAuthentication(requests);
            }
            long modTime = getIfModifiedSince();
            if (modTime != 0) {
                Date date = new Date(modTime);
                SimpleDateFormat fo = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
                fo.setTimeZone(TimeZone.getTimeZone("GMT"));
                requests.setIfNotSet("If-Modified-Since", fo.format(date));
            }
            AuthenticationInfo sauth = AuthenticationInfo.getServerAuth(url);
            if (sauth != null && sauth.supportsPreemptiveAuthorization()) {
                requests.setIfNotSet(sauth.getHeaderName(), sauth.getHeaderValue(url, method));
                currentServerCredentials = sauth;
            }
            if (poster != null) {
                synchronized (poster) {
                    poster.close();
                    if (!method.equals("PUT")) {
                        String type = "application/x-www-form-urlencoded";
                        requests.setIfNotSet("Content-Type", type);
                    }
                    requests.set("Content-Length", String.valueOf(poster.size()));
                }
            }
            setRequests = true;
        }
        http.writeRequests(requests, poster);
        if (ps.checkError()) {
            String proxyHost = http.getProxyHostUsed();
            int proxyPort = http.getProxyPortUsed();
            disconnectInternal();
            if (failedOnce) {
                throw new IOException("Error writing to server");
            } else {
                failedOnce = true;
                if (proxyHost != null) {
                    setProxiedClient(url, proxyHost, proxyPort);
                } else {
                    setNewClient(url);
                }
                ps = (PrintStream) http.getOutputStream();
                connected = true;
                responses = new MessageHeader();
                setRequests = false;
                writeRequests();
            }
        }
    }

    /**
     * Create a new HttpClient object, bypassing the cache of
     * HTTP client objects/connections.
     *
     * @param url	the URL being accessed
     */
    protected void setNewClient(URL url) throws IOException {
        setNewClient(url, false);
    }

    /**
     * Obtain a HttpsClient object. Use the cached copy if specified. 
     *
     * @param url       the URL being accessed
     * @param useCache  whether the cached connection should be used
     *        if present
     */
    protected void setNewClient(URL url, boolean useCache) throws IOException {
        http = HttpClient.New(url, useCache);
    }

    /**
     * Create a new HttpClient object, set up so that it uses
     * per-instance proxying to the given HTTP proxy.  This
     * bypasses the cache of HTTP client objects/connections.
     *
     * @param url	the URL being accessed
     * @param proxyHost	the proxy host to use
     * @param proxyPort	the proxy port to use
     */
    protected void setProxiedClient(URL url, String proxyHost, int proxyPort) throws IOException {
        setProxiedClient(url, proxyHost, proxyPort, false);
    }

    /**
     * Obtain a HttpClient object, set up so that it uses per-instance
     * proxying to the given HTTP proxy. Use the cached copy of HTTP
     * client objects/connections if specified.
     *
     * @param url       the URL being accessed
     * @param proxyHost the proxy host to use
     * @param proxyPort the proxy port to use
     * @param useCache  whether the cached connection should be used
     *        if present
     */
    protected void setProxiedClient(URL url, String proxyHost, int proxyPort, boolean useCache) throws IOException {
        proxiedConnect(url, proxyHost, proxyPort, useCache);
    }

    protected void proxiedConnect(URL url, String proxyHost, int proxyPort, boolean useCache) throws IOException {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkConnect(proxyHost, proxyPort);
        }
        http = HttpClient.New(url, proxyHost, proxyPort, useCache);
    }

    protected HttpURLConnection(URL u, Handler handler) throws IOException {
        super(u);
        requests = new MessageHeader();
        responses = new MessageHeader();
        this.handler = handler;
    }

    /** this constructor is used by other protocol handlers such as ftp
        that want to use http to fetch urls on their behalf. */
    public HttpURLConnection(URL u, String host, int port) throws IOException {
        this(u, new Handler(host, port));
    }

    /** 
     * @deprecated.  Use java.net.Authenticator.setDefault() instead.
     */
    public static void setDefaultAuthenticator(HttpAuthenticator a) {
        defaultAuth = a;
    }

    /**
     * opens a stream allowing redirects only to the same host.
     */
    public static InputStream openConnectionCheckRedirects(URLConnection c) throws IOException {
        boolean redir;
        int redirects = 0;
        InputStream in = null;
        do {
            if (c instanceof HttpURLConnection) {
                ((HttpURLConnection) c).setInstanceFollowRedirects(false);
            }
            in = c.getInputStream();
            redir = false;
            if (c instanceof HttpURLConnection) {
                HttpURLConnection http = (HttpURLConnection) c;
                int stat = http.getResponseCode();
                if (stat >= 300 && stat <= 307 && stat != 306 && stat != HttpURLConnection.HTTP_NOT_MODIFIED) {
                    URL base = http.getURL();
                    String loc = http.getHeaderField("Location");
                    URL target = null;
                    if (loc != null) {
                        target = new URL(base, loc);
                    }
                    http.disconnect();
                    if (target == null || !base.getProtocol().equals(target.getProtocol()) || base.getPort() != target.getPort() || !hostsEqual(base, target) || redirects >= 5) {
                        throw new SecurityException("illegal URL redirect");
                    }
                    redir = true;
                    c = target.openConnection();
                    redirects++;
                }
            }
        } while (redir);
        return in;
    }

    private static boolean hostsEqual(URL u1, URL u2) {
        final String h1 = u1.getHost();
        final String h2 = u2.getHost();
        if (h1 == null) {
            return h2 == null;
        } else if (h2 == null) {
            return false;
        } else if (h1.equalsIgnoreCase(h2)) {
            return true;
        }
        final boolean result[] = { false };
        java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {

            public Object run() {
                try {
                    InetAddress a1 = InetAddress.getByName(h1);
                    InetAddress a2 = InetAddress.getByName(h2);
                    result[0] = a1.equals(a2);
                } catch (UnknownHostException e) {
                } catch (SecurityException e) {
                }
                return null;
            }
        });
        return result[0];
    }

    public void connect() throws IOException {
        plainConnect();
    }

    private boolean checkReuseConnection() {
        if (connected) {
            return true;
        }
        if (reuseClient != null) {
            http = reuseClient;
            http.reuse = false;
            reuseClient = null;
            connected = true;
            return true;
        }
        return false;
    }

    protected void plainConnect() throws IOException {
        if (connected) {
            return;
        }
        try {
            if ("http".equals(url.getProtocol()) && !failedOnce) {
                http = HttpClient.New(url);
            } else {
                http = new HttpClient(url, handler.proxy, handler.proxyPort);
            }
            ps = (PrintStream) http.getOutputStream();
        } catch (IOException e) {
            throw e;
        }
        connected = true;
    }

    public synchronized OutputStream getOutputStream() throws IOException {
        try {
            if (!doOutput) {
                throw new ProtocolException("cannot write to a URLConnection" + " if doOutput=false - call setDoOutput(true)");
            }
            if (method.equals("GET")) {
                method = "POST";
            }
            if (!"POST".equals(method) && !"PUT".equals(method) && "http".equals(url.getProtocol())) {
                throw new ProtocolException("HTTP method " + method + " doesn't support output");
            }
            if (inputStream != null) {
                throw new ProtocolException("Cannot write output after reading input.");
            }
            if (!checkReuseConnection()) connect();
            ps = (PrintStream) http.getOutputStream();
            if (poster == null) poster = new PosterOutputStream();
            return poster;
        } catch (RuntimeException e) {
            disconnectInternal();
            throw e;
        } catch (IOException e) {
            disconnectInternal();
            throw e;
        }
    }

    public synchronized InputStream getInputStream() throws IOException {
        if (!doInput) {
            throw new ProtocolException("Cannot read from URLConnection" + " if doInput=false (call setDoInput(true))");
        }
        if (rememberedException != null) {
            if (rememberedException instanceof RuntimeException) throw new RuntimeException(rememberedException); else {
                IOException exception;
                try {
                    exception = new IOException();
                    exception.initCause(rememberedException);
                } catch (Exception t) {
                    exception = (IOException) rememberedException;
                }
                throw exception;
            }
        }
        if (inputStream != null) {
            return inputStream;
        }
        int redirects = 0;
        int respCode = 0;
        AuthenticationInfo serverAuthentication = null;
        AuthenticationInfo proxyAuthentication = null;
        AuthenticationHeader srvHdr = null;
        try {
            do {
                pe = new ProgressEntry(url.getFile(), null);
                ProgressData.pdata.register(pe);
                if (!checkReuseConnection()) connect();
                ps = (PrintStream) http.getOutputStream();
                writeRequests();
                http.parseHTTP(responses, pe);
                inputStream = new HttpInputStream(http.getInputStream());
                respCode = getResponseCode();
                if (respCode == HTTP_PROXY_AUTH) {
                    AuthenticationHeader authhdr = new AuthenticationHeader("Proxy-Authenticate", responses);
                    if (!doingNTLMp2ndStage) {
                        proxyAuthentication = resetProxyAuthentication(proxyAuthentication, authhdr);
                        if (proxyAuthentication != null) {
                            redirects++;
                            disconnectInternal();
                            continue;
                        }
                    } else {
                        String raw = responses.findValue("Proxy-Authenticate");
                        reset();
                        if (!proxyAuthentication.setHeaders(this, authhdr.headerParser(), raw)) {
                            disconnectInternal();
                            throw new IOException("Authentication failure");
                        }
                        if (serverAuthentication != null && srvHdr != null && !serverAuthentication.setHeaders(this, srvHdr.headerParser(), raw)) {
                            disconnectInternal();
                            throw new IOException("Authentication failure");
                        }
                        authObj = null;
                        doingNTLMp2ndStage = false;
                        continue;
                    }
                }
                if (proxyAuthentication != null) {
                    proxyAuthentication.addToCache();
                }
                if (respCode == HTTP_UNAUTHORIZED) {
                    srvHdr = new AuthenticationHeader("WWW-Authenticate", responses);
                    String raw = srvHdr.raw();
                    if (!doingNTLM2ndStage) {
                        if (serverAuthentication != null) {
                            if (serverAuthentication.isAuthorizationStale(raw)) {
                                disconnectInternal();
                                redirects++;
                                requests.set(serverAuthentication.getHeaderName(), serverAuthentication.getHeaderValue(url, method));
                                currentServerCredentials = serverAuthentication;
                                continue;
                            } else {
                                serverAuthentication.removeFromCache();
                            }
                        }
                        serverAuthentication = getServerAuthentication(srvHdr);
                        currentServerCredentials = serverAuthentication;
                        if (serverAuthentication != null) {
                            disconnectInternal();
                            redirects++;
                            continue;
                        }
                    } else {
                        reset();
                        if (!serverAuthentication.setHeaders(this, null, raw)) {
                            disconnectInternal();
                            throw new IOException("Authentication failure");
                        }
                        doingNTLM2ndStage = false;
                        authObj = null;
                        continue;
                    }
                }
                if (serverAuthentication != null) {
                    if (!(serverAuthentication instanceof DigestAuthentication) || (domain == null)) {
                        if (serverAuthentication instanceof BasicAuthentication) {
                            String npath = AuthenticationInfo.reducePath(url.getPath());
                            String opath = serverAuthentication.path;
                            if (!opath.startsWith(npath) || npath.length() >= opath.length()) {
                                npath = BasicAuthentication.getRootPath(opath, npath);
                            }
                            BasicAuthentication a = (BasicAuthentication) serverAuthentication.clone();
                            serverAuthentication.removeFromCache();
                            a.path = npath;
                            serverAuthentication = a;
                        }
                        serverAuthentication.addToCache();
                    } else {
                        DigestAuthentication srv = (DigestAuthentication) serverAuthentication;
                        StringTokenizer tok = new StringTokenizer(domain, " ");
                        String realm = srv.realm;
                        PasswordAuthentication pw = srv.pw;
                        digestparams = srv.params;
                        while (tok.hasMoreTokens()) {
                            String path = tok.nextToken();
                            try {
                                URL u = new URL(url, path);
                                DigestAuthentication d = new DigestAuthentication(false, u, realm, "Digest", pw, digestparams);
                                d.addToCache();
                            } catch (Exception e) {
                            }
                        }
                    }
                }
                if (respCode == HTTP_OK) {
                    checkResponseCredentials(false);
                } else {
                    needToCheck = false;
                }
                if (followRedirect()) {
                    redirects++;
                    continue;
                }
                int cl = -1;
                try {
                    cl = Integer.parseInt(responses.findValue("content-length"));
                } catch (Exception exc) {
                }
                ;
                if (method.equals("HEAD") || method.equals("TRACE") || cl == 0 || respCode == HTTP_NOT_MODIFIED || respCode == HTTP_NO_CONTENT) {
                    if (pe != null) {
                        ProgressData.pdata.unregister(pe);
                    }
                    http.finished();
                    http = null;
                    inputStream = new EmptyInputStream();
                    if (respCode < 400) {
                        connected = false;
                        return inputStream;
                    }
                }
                if (respCode >= 400) {
                    if (respCode == 404 || respCode == 410) {
                        throw new FileNotFoundException(url.toString());
                    } else {
                        throw new java.io.IOException("Server returned HTTP" + " response code: " + respCode + " for URL: " + url.toString());
                    }
                }
                return inputStream;
            } while (redirects < maxRedirects);
            throw new ProtocolException("Server redirected too many " + " times (" + redirects + ")");
        } catch (RuntimeException e) {
            disconnectInternal();
            rememberedException = e;
            throw e;
        } catch (IOException e) {
            rememberedException = e;
            throw e;
        } finally {
            if (respCode == HTTP_PROXY_AUTH && proxyAuthentication != null) {
                proxyAuthentication.endAuthRequest();
            } else if (respCode == HTTP_UNAUTHORIZED && serverAuthentication != null) {
                serverAuthentication.endAuthRequest();
            }
        }
    }

    public InputStream getErrorStream() {
        if (connected && responseCode >= 400) {
            if (inputStream != null) {
                return inputStream;
            }
        }
        return null;
    }

    /**
     * set or reset proxy authentication info in request headers
     * after receiving a 407 error. In the case of NTLM however,
     * receiving a 407 is normal and we just skip the stale check
     * because ntlm does not support this feature.
     */
    private AuthenticationInfo resetProxyAuthentication(AuthenticationInfo proxyAuthentication, AuthenticationHeader auth) {
        if (proxyAuthentication != null) {
            String raw = auth.raw();
            if (proxyAuthentication.isAuthorizationStale(raw)) {
                requests.set(proxyAuthentication.getHeaderName(), proxyAuthentication.getHeaderValue(url, method));
                currentProxyCredentials = proxyAuthentication;
                return proxyAuthentication;
            } else {
                proxyAuthentication.removeFromCache();
            }
        }
        proxyAuthentication = getHttpProxyAuthentication(auth);
        currentProxyCredentials = proxyAuthentication;
        return proxyAuthentication;
    }

    /**
     * establish a tunnel through proxy server
     */
    protected synchronized void doTunneling() throws IOException {
        int retryTunnel = 0;
        String statusLine = "";
        int respCode = 0;
        AuthenticationInfo proxyAuthentication = null;
        String proxyHost = null;
        int proxyPort = -1;
        try {
            do {
                if (!checkReuseConnection()) {
                    proxiedConnect(url, proxyHost, proxyPort, false);
                }
                sendCONNECTRequest();
                responses.reset();
                http.parseHTTP(responses, new ProgressEntry(url.getFile(), null));
                statusLine = responses.getValue(0);
                StringTokenizer st = new StringTokenizer(statusLine);
                st.nextToken();
                respCode = Integer.parseInt(st.nextToken().trim());
                if (respCode == HTTP_PROXY_AUTH) {
                    AuthenticationHeader authhdr = new AuthenticationHeader("Proxy-Authenticate", responses);
                    if (!doingNTLMp2ndStage) {
                        proxyAuthentication = resetProxyAuthentication(proxyAuthentication, authhdr);
                        if (proxyAuthentication != null) {
                            proxyHost = http.getProxyHostUsed();
                            proxyPort = http.getProxyPortUsed();
                            disconnectInternal();
                            retryTunnel++;
                            continue;
                        }
                    } else {
                        String raw = responses.findValue("Proxy-Authenticate");
                        reset();
                        if (!proxyAuthentication.setHeaders(this, authhdr.headerParser(), raw)) {
                            proxyHost = http.getProxyHostUsed();
                            proxyPort = http.getProxyPortUsed();
                            disconnectInternal();
                            throw new IOException("Authentication failure");
                        }
                        authObj = null;
                        doingNTLMp2ndStage = false;
                        continue;
                    }
                }
                if (proxyAuthentication != null) {
                    proxyAuthentication.addToCache();
                }
                if (respCode == HTTP_OK) {
                    break;
                }
                disconnectInternal();
                break;
            } while (retryTunnel < maxRedirects);
            if (retryTunnel >= maxRedirects || (respCode != HTTP_OK)) {
                throw new IOException("Unable to tunnel through proxy." + " Proxy returns \"" + statusLine + "\"");
            }
        } finally {
            if (respCode == HTTP_PROXY_AUTH && proxyAuthentication != null) {
                proxyAuthentication.endAuthRequest();
            }
        }
        int i;
        if ((i = requests.getKey("Proxy-authorization")) >= 0) requests.set(i, null, null);
        responses.reset();
    }

    /**
     * send a CONNECT request for establishing a tunnel to proxy server
     */
    private void sendCONNECTRequest() throws IOException {
        int port = url.getPort();
        if (port == -1) {
            port = url.getDefaultPort();
        }
        requests.prepend("CONNECT " + url.getHost() + ":" + port + " " + httpVersion, null);
        requests.setIfNotSet("User-Agent", userAgent);
        String host = url.getHost();
        if (port != -1 && port != 80) {
            host += ":" + String.valueOf(port);
        }
        requests.setIfNotSet("Host", host);
        requests.setIfNotSet("Accept", acceptString);
        setPreemptiveProxyAuthentication(requests);
        http.writeRequests(requests, null);
        requests.set(0, null, null);
    }

    /**
     * Sets pre-emptive proxy authentication in header
     */
    private void setPreemptiveProxyAuthentication(MessageHeader requests) {
        AuthenticationInfo pauth = AuthenticationInfo.getProxyAuth(http.getProxyHostUsed(), http.getProxyPortUsed());
        if (pauth != null && pauth.supportsPreemptiveAuthorization()) {
            requests.setIfNotSet(pauth.getHeaderName(), pauth.getHeaderValue(url, method));
            currentProxyCredentials = pauth;
        }
    }

    /**
     * Gets the authentication for an HTTP proxy, and applies it to
     * the connection.
     */
    private AuthenticationInfo getHttpProxyAuthentication(AuthenticationHeader authhdr) {
        AuthenticationInfo ret = null;
        String raw = authhdr.raw();
        String host = http.getProxyHostUsed();
        int port = http.getProxyPortUsed();
        if (host != null && authhdr.isPresent()) {
            HeaderParser p = authhdr.headerParser();
            String realm = p.findValue("realm");
            String scheme = authhdr.scheme();
            char schemeID;
            if ("basic".equalsIgnoreCase(scheme)) {
                schemeID = BasicAuthentication.BASIC_AUTH;
            } else if ("digest".equalsIgnoreCase(scheme)) {
                schemeID = DigestAuthentication.DIGEST_AUTH;
            } else {
                schemeID = 0;
            }
            if (realm == null) realm = "";
            ret = AuthenticationInfo.getProxyAuth(host, port, realm, schemeID);
            if (ret == null) {
                if (schemeID == BasicAuthentication.BASIC_AUTH) {
                    InetAddress addr = null;
                    try {
                        final String finalHost = host;
                        addr = (InetAddress) java.security.AccessController.doPrivileged(new java.security.PrivilegedExceptionAction() {

                            public Object run() throws java.net.UnknownHostException {
                                return InetAddress.getByName(finalHost);
                            }
                        });
                    } catch (java.security.PrivilegedActionException ignored) {
                    }
                    PasswordAuthentication a = privilegedRequestPasswordAuthentication(host, addr, port, "http", realm, scheme);
                    if (a != null) {
                        ret = new BasicAuthentication(true, host, port, realm, a);
                    }
                } else if (schemeID == DigestAuthentication.DIGEST_AUTH) {
                    PasswordAuthentication a = privilegedRequestPasswordAuthentication(host, null, port, url.getProtocol(), realm, scheme);
                    if (a != null) {
                        DigestAuthentication.Parameters params = new DigestAuthentication.Parameters();
                        ret = new DigestAuthentication(true, host, port, realm, scheme, a, params);
                    }
                }
            }
            if (ret == null && defaultAuth != null && defaultAuth.schemeSupported(scheme)) {
                try {
                    URL u = new URL("http", host, port, "/");
                    String a = defaultAuth.authString(u, scheme, realm);
                    if (a != null) {
                        ret = new BasicAuthentication(true, host, port, realm, a);
                    }
                } catch (java.net.MalformedURLException ignored) {
                }
            }
            if (ret != null) {
                if (!ret.setHeaders(this, p, raw)) {
                    ret = null;
                }
            }
        }
        return ret;
    }

    /**
     * Gets the authentication for an HTTP server, and applies it to
     * the connection.
     */
    private AuthenticationInfo getServerAuthentication(AuthenticationHeader authhdr) {
        AuthenticationInfo ret = null;
        String raw = authhdr.raw();
        if (authhdr.isPresent()) {
            HeaderParser p = authhdr.headerParser();
            String realm = p.findValue("realm");
            String scheme = authhdr.scheme();
            char schemeID;
            if ("basic".equalsIgnoreCase(scheme)) {
                schemeID = BasicAuthentication.BASIC_AUTH;
            } else if ("digest".equalsIgnoreCase(scheme)) {
                schemeID = DigestAuthentication.DIGEST_AUTH;
            } else {
                schemeID = 0;
            }
            domain = p.findValue("domain");
            if (realm == null) realm = "";
            ret = AuthenticationInfo.getServerAuth(url, realm, schemeID);
            InetAddress addr = null;
            if (ret == null) {
                try {
                    addr = InetAddress.getByName(url.getHost());
                } catch (java.net.UnknownHostException ignored) {
                }
            }
            int port = url.getPort();
            if (port == -1) {
                port = url.getDefaultPort();
            }
            if (ret == null) {
                if (schemeID == BasicAuthentication.BASIC_AUTH) {
                    PasswordAuthentication a = privilegedRequestPasswordAuthentication(url.getHost(), addr, port, url.getProtocol(), realm, scheme);
                    if (a != null) {
                        ret = new BasicAuthentication(false, url, realm, a);
                    }
                }
                if (schemeID == DigestAuthentication.DIGEST_AUTH) {
                    PasswordAuthentication a = privilegedRequestPasswordAuthentication(url.getHost(), addr, port, url.getProtocol(), realm, scheme);
                    if (a != null) {
                        digestparams = new DigestAuthentication.Parameters();
                        ret = new DigestAuthentication(false, url, realm, scheme, a, digestparams);
                    }
                }
            }
            if (ret == null && defaultAuth != null && defaultAuth.schemeSupported(scheme)) {
                String a = defaultAuth.authString(url, scheme, realm);
                if (a != null) {
                    ret = new BasicAuthentication(false, url, realm, a);
                }
            }
            if (ret != null) {
                if (!ret.setHeaders(this, p, raw)) {
                    ret = null;
                }
            }
        }
        return ret;
    }

    private void checkResponseCredentials(boolean inClose) throws IOException {
        try {
            if (!needToCheck) return;
            if (validateProxy && currentProxyCredentials != null) {
                String raw = responses.findValue("Proxy-Authentication-Info");
                if (inClose || (raw != null)) {
                    currentProxyCredentials.checkResponse(raw, method, url);
                    currentProxyCredentials = null;
                }
            }
            if (validateServer && currentServerCredentials != null) {
                String raw = responses.findValue("Authentication-Info");
                if (inClose || (raw != null)) {
                    currentServerCredentials.checkResponse(raw, method, url);
                    currentServerCredentials = null;
                }
            }
            if ((currentServerCredentials == null) && (currentProxyCredentials == null)) {
                needToCheck = false;
            }
        } catch (IOException e) {
            disconnectInternal();
            connected = false;
            throw e;
        }
    }

    private boolean followRedirect() throws IOException {
        if (!getInstanceFollowRedirects()) {
            return false;
        }
        int stat = getResponseCode();
        if (stat < 300 || stat > 307 || stat == 306 || stat == HTTP_NOT_MODIFIED) {
            return false;
        }
        String loc = getHeaderField("Location");
        if (loc == null) {
            return false;
        }
        URL locUrl;
        try {
            locUrl = new URL(loc);
            if (!url.getProtocol().equalsIgnoreCase(locUrl.getProtocol())) {
                return false;
            }
        } catch (MalformedURLException mue) {
            locUrl = new URL(url, loc);
        }
        disconnectInternal();
        responses = new MessageHeader();
        if (stat == HTTP_USE_PROXY) {
            setProxiedClient(url, locUrl.getHost(), locUrl.getPort());
            requests.set(0, method + " " + http.getURLFile() + " " + httpVersion, null);
            connected = true;
        } else {
            url = locUrl;
            if (method.equals("POST") && !Boolean.getBoolean("http.strictPostRedirect") && (stat != 307)) {
                requests = new MessageHeader();
                setRequests = false;
                setRequestMethod("GET");
                poster = null;
                if (!checkReuseConnection()) connect();
            } else {
                if (!checkReuseConnection()) connect();
                requests.set(0, method + " " + http.getURLFile() + " " + httpVersion, null);
                requests.set("Host", url.getHost() + ((url.getPort() == -1 || url.getPort() == 80) ? "" : ":" + String.valueOf(url.getPort())));
            }
        }
        return true;
    }

    byte[] cdata = new byte[128];

    /**
     * Reset (without disconnecting the TCP conn) in order to do another transaction with this instance
     */
    private void reset() throws IOException {
        http.reuse = true;
        reuseClient = http;
        InputStream is = http.getInputStream();
        try {
            if ((is instanceof ChunkedInputStream) || (is instanceof MeteredStream)) {
                while (is.read(cdata) > 0) {
                }
            } else {
                int cl = 0, n = 0;
                try {
                    cl = Integer.parseInt(responses.findValue("Content-Length"));
                } catch (Exception e) {
                }
                for (int i = 0; i < cl; ) {
                    if ((n = is.read(cdata)) == -1) {
                        break;
                    } else {
                        i += n;
                    }
                }
            }
        } catch (IOException e) {
            http.reuse = false;
            reuseClient = null;
            disconnectInternal();
            return;
        }
        try {
            if (is instanceof MeteredStream) {
                is.close();
            }
        } catch (IOException e) {
        }
        responseCode = -1;
        responses = new MessageHeader();
        connected = false;
    }

    /**
     * Disconnect from the server (for internal use)
     */
    private void disconnectInternal() {
        responseCode = -1;
        if (pe != null) {
            ProgressData.pdata.unregister(pe);
        }
        if (http != null) {
            http.closeServer();
            http = null;
            connected = false;
        }
    }

    /**
     * Disconnect from the server (public API)
     */
    public void disconnect() {
        responseCode = -1;
        if (pe != null) {
            ProgressData.pdata.unregister(pe);
        }
        if (http != null) {
            if (inputStream != null) {
                HttpClient hc = http;
                boolean ka = hc.isKeepingAlive();
                try {
                    inputStream.close();
                } catch (IOException ioe) {
                }
                if (ka) {
                    hc.closeIdleConnection();
                }
            } else {
                http.closeServer();
            }
            http = null;
            connected = false;
        }
    }

    public boolean usingProxy() {
        if (http != null) {
            return (http.getProxyHostUsed() != null);
        }
        return false;
    }

    /**
     * Gets a header field by name. Returns null if not known.
     * @param name the name of the header field
     */
    public String getHeaderField(String name) {
        try {
            getInputStream();
        } catch (IOException e) {
        }
        return responses.findValue(name);
    }

    /**
     * Returns an unmodifiable Map of the header fields.
     * The Map keys are Strings that represent the
     * response-header field names. Each Map value is an
     * unmodifiable List of Strings that represents 
     * the corresponding field values.
     *
     * @return a Map of header fields
     * @since 1.4
     */
    public Map getHeaderFields() {
        try {
            getInputStream();
        } catch (IOException e) {
        }
        return responses.getHeaders();
    }

    /**
     * Gets a header field by index. Returns null if not known.
     * @param n the index of the header field
     */
    public String getHeaderField(int n) {
        try {
            getInputStream();
        } catch (IOException e) {
        }
        return responses.getValue(n);
    }

    /**
     * Gets a header field by index. Returns null if not known.
     * @param n the index of the header field
     */
    public String getHeaderFieldKey(int n) {
        try {
            getInputStream();
        } catch (IOException e) {
        }
        return responses.getKey(n);
    }

    /**
     * Sets request property. If a property with the key already
     * exists, overwrite its value with the new value.
     * @param value the value to be set
     */
    public void setRequestProperty(String key, String value) {
        super.setRequestProperty(key, value);
        checkMessageHeader(key, value);
        requests.set(key, value);
    }

    /**
     * Adds a general request property specified by a
     * key-value pair.  This method will not overwrite
     * existing values associated with the same key.
     *
     * @param   key     the keyword by which the request is known
     *                  (e.g., "<code>accept</code>").
     * @param   value  the value associated with it.
     * @see #getRequestProperties(java.lang.String)
     * @since 1.4
     */
    public void addRequestProperty(String key, String value) {
        super.addRequestProperty(key, value);
        checkMessageHeader(key, value);
        requests.add(key, value);
    }

    void setAuthenticationProperty(String key, String value) {
        checkMessageHeader(key, value);
        requests.set(key, value);
    }

    public String getRequestProperty(String key) {
        if (key != null) {
            for (int i = 0; i < EXCLUDE_HEADERS.length; i++) {
                if (key.equalsIgnoreCase(EXCLUDE_HEADERS[i])) {
                    return null;
                }
            }
        }
        return requests.findValue(key);
    }

    /**
     * Returns an unmodifiable Map of general request
     * properties for this connection. The Map keys
     * are Strings that represent the request-header
     * field names. Each Map value is a unmodifiable List 
     * of Strings that represents the corresponding 
     * field values.
     *
     * @return  a Map of the general request properties for this connection.
     * @throws IllegalStateException if already connected
     * @since 1.4
     */
    public Map getRequestProperties() {
        if (connected) throw new IllegalStateException("Already connected");
        return requests.getHeaders(EXCLUDE_HEADERS);
    }

    public void finalize() {
    }

    String getMethod() {
        return method;
    }

    class HttpInputStream extends FilterInputStream {

        public HttpInputStream(InputStream is) {
            super(is);
        }

        public void close() throws IOException {
            try {
                super.close();
            } finally {
                HttpURLConnection.this.http = null;
                checkResponseCredentials(true);
            }
        }
    }
}

/** An input stream that just returns EOF.  This is for
 * HTTP URLConnections that are KeepAlive && use the
 * HEAD method - i.e., stream not dead, but nothing to be read.
 */
class EmptyInputStream extends InputStream {

    public int available() {
        return 0;
    }

    public int read() {
        return -1;
    }
}
