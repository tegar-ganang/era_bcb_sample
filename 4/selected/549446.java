package org.geogrid.aist.tsukubagama.servlet.httpproxy.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import org.globus.common.ChainedIOException;
import org.globus.gsi.GSIConstants;
import org.globus.gsi.gssapi.GSSConstants;
import org.globus.gsi.gssapi.net.GssSocket;
import org.globus.gsi.gssapi.net.GssSocketFactory;
import org.globus.net.GSIURLConnection;
import org.globus.util.http.HTTPChunkedInputStream;
import org.globus.util.http.HTTPChunkedOutputStream;
import org.globus.util.http.HTTPProtocol;
import org.globus.util.http.HTTPResponseParser;
import org.gridforum.jgss.ExtendedGSSContext;
import org.gridforum.jgss.ExtendedGSSManager;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;

public class GSIHttpURLConnectionExt extends GSIURLConnection {

    public static final int PORT = 8443;

    private static final String USER_AGENT = "Java-Globus-GASS-HTTP/1.1.0";

    private static final String POST_CONTENT_TYPE = "text/xml";

    private Socket socket;

    private int port;

    private HTTPResponseParser response;

    private InputStream is;

    private OutputStream os;

    public GSIHttpURLConnectionExt(URL u) {
        super(u);
    }

    public synchronized void connect() throws IOException {
        if (this.connected) {
            return;
        } else {
            this.connected = true;
        }
        this.port = (url.getPort() == -1) ? PORT : url.getPort();
        GSSManager manager = ExtendedGSSManager.getInstance();
        ExtendedGSSContext context = null;
        try {
            context = (ExtendedGSSContext) manager.createContext(getExpectedName(), GSSConstants.MECH_OID, this.credentials, GSSContext.DEFAULT_LIFETIME);
            switch(this.delegationType) {
                case GSIConstants.DELEGATION_NONE:
                    context.requestCredDeleg(false);
                    break;
                case GSIConstants.DELEGATION_LIMITED:
                    context.requestCredDeleg(true);
                    context.setOption(GSSConstants.DELEGATION_TYPE, GSIConstants.DELEGATION_TYPE_LIMITED);
                    break;
                case GSIConstants.DELEGATION_FULL:
                    context.requestCredDeleg(true);
                    context.setOption(GSSConstants.DELEGATION_TYPE, GSIConstants.DELEGATION_TYPE_FULL);
                    break;
                default:
                    context.requestCredDeleg(true);
                    context.setOption(GSSConstants.DELEGATION_TYPE, new Integer(this.delegationType));
            }
            if (this.gssMode != null) {
                context.setOption(GSSConstants.GSS_MODE, gssMode);
            }
        } catch (GSSException e) {
            throw new ChainedIOException("Failed to init GSI context", e);
        }
        GssSocketFactory factory = GssSocketFactory.getDefault();
        socket = factory.createSocket(url.getHost(), this.port, context);
        ((GssSocket) socket).setAuthorization(authorization);
    }

    public void disconnect() {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }

    public synchronized OutputStream getOutputStream(String contenttype) throws IOException {
        if ("".equals(contenttype)) {
            contenttype = POST_CONTENT_TYPE;
        }
        if (this.is != null && this.os == null) {
            throw new ProtocolException("Cannot write output after reading input");
        }
        if (this.os == null) {
            connect();
            String header = HTTPProtocol.createPUTHeader(url.getFile(), url.getHost() + ":" + port, USER_AGENT, contenttype, -1, true);
            OutputStream wrapped = socket.getOutputStream();
            wrapped.write(header.getBytes());
            this.os = new HTTPChunkedOutputStream(wrapped);
        }
        return os;
    }

    public synchronized InputStream getInputStream() throws IOException {
        if (this.is == null) {
            connect();
            if (this.os == null) {
                OutputStream out = socket.getOutputStream();
                String msg = HTTPProtocol.createGETHeader(url.getFile(), url.getHost() + ":" + this.port, USER_AGENT);
                out.write(msg.getBytes());
                out.flush();
            } else {
                this.os.flush();
                this.os.close();
                this.os = null;
            }
            InputStream in = socket.getInputStream();
            response = new HTTPResponseParser(in);
            if (!response.isOK()) {
                throw new IOException(response.getMessage());
            }
            if (response.isChunked()) {
                is = new HTTPChunkedInputStream(in);
            } else {
                is = in;
            }
        }
        return is;
    }

    public String getHeaderField(String name) {
        if (response == null) {
            return null;
        }
        if (name.equalsIgnoreCase("content-type")) {
            return response.getContentType();
        } else if (name.equalsIgnoreCase("content-length")) {
            return String.valueOf(response.getContentLength());
        } else {
            return null;
        }
    }
}
