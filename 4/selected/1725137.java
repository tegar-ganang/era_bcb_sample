package com.migniot.streamy.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.impl.SocketHttpClientConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Logger;

/**
 * A transparent proxy {@link Servlet}.
 *
 * This {@link Servlet} is a <b>minimalist</b> proxy {@link Servlet}. It has
 * been implemented with simplicity and compliance in mind and should handle
 * most web browsing common cases. When the {@link Servlet} does not know how to
 * handle a request it always tries to FAIL properly by issuing a 502 HTTP code
 * when possible and closing all streams otherwise.
 *
 * As of http://www.ietf.org/rfc/rfc2616.txt, paragraph 1.3, this servlet
 * implements a transparent proxy - without persistent connection support.
 *
 * Do <b>NOT</b> use this code on the following cases :
 * <ul>
 * <li>Security is a key issue</li>
 * <li>Proxy authentication is a key issue</li>
 * <li>Proxy chaining is a key issue</li>
 * <li>Error recovering is a key issue</li>
 * <li>SSL, FTP, Socks, IPC are used</li>
 * </ul>
 */
@SuppressWarnings("serial")
public class ProxyServlet extends HttpServlet {

    /**
	 * The logger.
	 */
    private static Logger LOGGER = Logger.getLogger(ProxyServlet.class);

    /**
	 * The logger.
	 */
    private static Logger HEADERS_LOGGER = Logger.getLogger("headers");

    /**
	 * Ignored headers.
	 *
	 * As of http://www.w3.org/Protocols/rfc2616/rfc2616-sec8.html, paragraph
	 * 8.1.3, the proxy MUST treat client and server persistent connections
	 * separately. A proxy server MUST NOT establish a persistent connection
	 * with a HTTP/1.0 client.
	 *
	 * By design this proxy servlet NEVER establish persistent connection,
	 * neither with the server nor with the client, resulting in a loss of speed
	 * from a client point of view but in a strict compliance to both RFC 1945
	 * and RFC 2616.
	 *
	 * As of http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html#sec13.5.1,
	 * these headers are hop-by-hop headers, targeted to this proxy servlet and
	 * must NOT be sent to the next intermediary in the HTTP/1.1 conversation.
	 */
    public static final Set<String> IGNORED_HEADERS = new HashSet<String>(Arrays.asList(new String[] { "Connection", "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization", "TE", "Trailer", "Transfer-Encoding", "Upgrade", "Proxy-Connection" }));

    /**
	 * {@inheritDoc}
	 */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String method = req.getMethod();
        if ("CONNECT".equals(method)) {
            doConnect(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doProxy(req, resp, false);
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doProxy(req, resp, true);
    }

    /**
	 * Proxy the request.
	 *
	 * @param request
	 *            An {@link HttpServletRequest} object that contains the request
	 *            the client has made to this {@link Servlet}
	 * @param response
	 *            An {@link HttpServletResponse} object that contains the
	 *            response this {@link Servlet} sends to the client
	 * @param hasContent
	 *            If true, forward the request content to the target server
	 * @throws IOException
	 *             Upon stream manipulation failure or remote server failure
	 */
    protected void doProxy(HttpServletRequest request, HttpServletResponse response, boolean hasContent) throws IOException {
        String method = request.getMethod();
        String query = request.getQueryString();
        StringBuilder uri = new StringBuilder(request.getRequestURI());
        if (query != null) {
            uri.append("?").append(query);
        }
        HttpRequest proxyRequest = new BasicHttpRequest(method, uri.toString(), HttpVersion.HTTP_1_1);
        for (Enumeration<?> enumeration = request.getHeaderNames(); enumeration.hasMoreElements(); ) {
            String name = (String) enumeration.nextElement();
            String value = request.getHeader(name);
            if (IGNORED_HEADERS.contains(name)) {
                HEADERS_LOGGER.debug(MessageFormat.format("Ignored header name = [{0}], value = [{1}]", name, String.valueOf(value)));
            } else {
                proxyRequest.setHeader(name, value);
                HEADERS_LOGGER.debug(MessageFormat.format("Header name = [{0}], value = [{1}]", name, String.valueOf(value)));
            }
        }
        proxyRequest.setHeader("Connection", "close");
        addViaHeader(proxyRequest);
        String server = request.getServerName();
        int port = request.getServerPort();
        Socket proxySocket = new Socket(request.getServerName(), request.getServerPort());
        HttpParams proxyParameters = new BasicHttpParams();
        proxyParameters.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        DefaultHttpClientConnection proxyConnection = new DefaultHttpClientConnection();
        proxyConnection.bind(proxySocket, proxyParameters);
        HEADERS_LOGGER.debug(MessageFormat.format("Target server = [{0}], port = [{1}]", server, port));
        try {
            HEADERS_LOGGER.debug("Sending request headers");
            proxyConnection.sendRequestHeader(proxyRequest);
            HEADERS_LOGGER.debug("Sent");
        } catch (HttpException he) {
            StringBuilder message = new StringBuilder("Proxy connection failed for url = [").append(request.getRequestURL()).append("]");
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            LOGGER.error(message.toString(), he);
            closeQuietly(proxyConnection, proxySocket);
            throw new IOException(message.toString());
        }
        ServletInputStream input = request.getInputStream();
        if (hasContent) {
            BasicHttpEntityEnclosingRequest container = new BasicHttpEntityEnclosingRequest(method, uri.toString());
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(input);
            container.setEntity(entity);
            try {
                proxyConnection.sendRequestEntity(container);
            } catch (HttpException he) {
                StringBuilder message = new StringBuilder("Proxy body post failed for url = [").append(request.getRequestURL()).append("]");
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                LOGGER.error(message.toString(), he);
                closeQuietly(proxyConnection, proxySocket, input);
                throw new IOException(message.toString());
            }
        }
        proxyConnection.flush();
        input.close();
        HttpResponse proxyResponse = null;
        try {
            HEADERS_LOGGER.debug("Receiving response headers");
            proxyResponse = proxyConnection.receiveResponseHeader();
            HEADERS_LOGGER.debug("Received headers");
        } catch (HttpException he) {
            StringBuilder message = new StringBuilder("Proxy response headers reception failed for url = [").append(request.getRequestURL()).append("]");
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            LOGGER.error(message.toString(), he);
            closeQuietly(proxyConnection, proxySocket);
            throw new IOException(message.toString());
        }
        HEADERS_LOGGER.debug(MessageFormat.format("Response status line = [{0}]", proxyResponse.getStatusLine()));
        try {
            HEADERS_LOGGER.debug("Receiving response body");
            proxyConnection.receiveResponseEntity(proxyResponse);
            HEADERS_LOGGER.debug("Received body");
        } catch (HttpException he) {
            StringBuilder message = new StringBuilder("Proxy response body reception failed for url = [").append(request.getRequestURL()).append("]");
            LOGGER.error(message.toString(), he);
            closeQuietly(proxyConnection, proxySocket);
            throw new IOException(message.toString());
        }
        response.setStatus(proxyResponse.getStatusLine().getStatusCode());
        for (Header header : proxyResponse.getAllHeaders()) {
            String name = header.getName();
            String value = header.getValue();
            if (IGNORED_HEADERS.contains(name)) {
                HEADERS_LOGGER.debug(MessageFormat.format("Ignored response header name = [{0}], value = [{1}]", name, String.valueOf(value)));
            } else {
                response.setHeader(name, value);
                HEADERS_LOGGER.debug(MessageFormat.format("Response header name = [{0}], value = [{1}]", name, String.valueOf(value)));
            }
        }
        response.setHeader("Proxy-Connection", "close");
        response.setHeader("Connection", "close");
        HttpEntity proxyEntity = proxyResponse.getEntity();
        if (proxyEntity != null) {
            HEADERS_LOGGER.debug("Copying response body");
            try {
                streamResponse(request, response, proxyRequest, proxyResponse);
            } finally {
                closeQuietly(response.getOutputStream(), proxyConnection, proxySocket);
            }
            HEADERS_LOGGER.debug("Copied");
        }
        LOGGER.info(new StringBuilder(proxyResponse.getStatusLine().toString()).append(" ").append(method).append(" ").append(request.getRequestURL()).toString());
    }

    /**
	 * Transparently proxy the request.
	 * 
	 * @param request
	 *            An {@link HttpServletRequest} object that contains the request
	 *            the client has made to this {@link Servlet}
	 * @param response
	 *            An {@link HttpServletResponse} object that contains the
	 *            response this {@link Servlet} sends to the client
	 * @throws IOException
	 *             Upon CONNECT client misusage or server connection failure
	 * @throws ServletException
	 *             When tunnel mode aborts abruptely
	 */
    protected void doConnect(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String hostname = request.getServerName();
        String uri = request.getRequestURI();
        String portString = uri.substring(uri.lastIndexOf(":") + 1);
        int port = 0;
        try {
            port = Integer.parseInt(portString);
        } catch (NumberFormatException nfe) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            String message = new StringBuilder("HTTP/1.1 502 Malformed authority CONNECT ").append(uri).toString();
            LOGGER.error(message, nfe);
            throw new IOException(message);
        }
        HashMap<String, String> currentHeaders = new HashMap<String, String>();
        for (Enumeration<?> enumeration = request.getHeaderNames(); enumeration.hasMoreElements(); ) {
            String name = (String) enumeration.nextElement();
            String value = request.getHeader(name);
            LOGGER.debug(MessageFormat.format("\tCONNECT header name = [{0}], value = [{1}]", name, String.valueOf(value)));
            currentHeaders.put(name, value);
        }
        Socket proxySocket = null;
        try {
            proxySocket = new Socket(hostname, port);
        } catch (UnknownHostException uhe) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            String message = new StringBuilder("HTTP/1.1 502 Unknown host CONNECT ").append(uri).toString();
            LOGGER.error(message, uhe);
            throw new IOException(message);
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.flushBuffer();
        LOGGER.info(new StringBuilder("HTTP/1.1 200 OK CONNECT ").append(hostname).append(":").append(port).toString());
        final ServletInputStream clientInput = request.getInputStream();
        final ServletOutputStream clientOutput = response.getOutputStream();
        final InputStream serverInput = proxySocket.getInputStream();
        final OutputStream serverOutput = proxySocket.getOutputStream();
        byte[] buffer = new byte[2048];
        int ttl = 2048;
        while (ttl > 0) {
            boolean alive = false;
            int i = serverInput.available();
            int j = clientInput.available();
            if (i > 0) {
                i = Math.min(i, buffer.length);
                int n = serverInput.read(buffer, 0, i);
                clientOutput.write(buffer, 0, n);
                alive = true;
            }
            if (j > 0) {
                j = Math.min(j, buffer.length);
                int n = clientInput.read(buffer, 0, j);
                serverOutput.write(buffer, 0, n);
                alive = true;
            }
            if (alive) {
                ttl = 2048;
            } else {
                try {
                    clientOutput.flush();
                    serverOutput.flush();
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    ttl = 0;
                    throw new ServletException("Tunnel mode stopped for hostname = [" + hostname + "] and port = [" + port + "]", e);
                }
                ttl--;
            }
        }
    }

    /**
	 * Close streams quietly.
	 *
	 * @param conversations The conversations
	 */
    protected void closeQuietly(Object... conversations) {
        for (Object conversation : conversations) {
            if (conversation instanceof InputStream) {
                IOUtils.closeQuietly((InputStream) conversation);
            } else if (conversation instanceof OutputStream) {
                IOUtils.closeQuietly((OutputStream) conversation);
            } else if (conversation instanceof SocketHttpClientConnection) {
                try {
                    ((SocketHttpClientConnection) conversation).close();
                } catch (IOException ioe) {
                }
            } else if (conversation instanceof Socket) {
                try {
                    ((Socket) conversation).close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    /**
	 * Stream the proxy response body to the user agent response output stream.
	 *
	 * SubClasses SHOULD override this method both for metadata gathering and
	 * for stream backup.
	 *
	 * @param request
	 *            The user agent request
	 * @param response
	 *            The user agent response
	 * @param proxyResponse
	 *            The proxy request
	 * @param proxyRequest
	 *            The server response
	 * @return An output stream
	 * @throws IOException
	 *             Upon streaming failure
	 */
    protected void streamResponse(HttpServletRequest request, HttpServletResponse response, HttpRequest proxyRequest, HttpResponse proxyResponse) throws IOException {
        InputStream input = proxyResponse.getEntity().getContent();
        ServletOutputStream output = response.getOutputStream();
        IOUtils.copy(input, output);
    }

    /**
	 * Add the mandatory Via header.
	 *
	 * As of http://www.ietf.org/rfc/rfc2616.txt, paragraph 14.45 a compliant
	 * proxy MUST use the Via header to indicate its presence between the user
	 * agent and the server.
	 *
	 * @param proxyRequest
	 *            The proxy request
	 */
    protected void addViaHeader(HttpRequest proxyRequest) {
        proxyRequest.addHeader("Via", "1.1 ProxyServlet");
    }
}
