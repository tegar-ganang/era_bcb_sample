package p.s;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.SocketChannel;

/**
 * The proto streamer creates a thread for each inbound socket.
 * Typically a very bad idea, but for long term streaming it's a
 * simple and effective approach.
 * 
 * The fact that the request constructor called by the {@link Server}
 * does the first read line is an aid in support of this approach.  If
 * the first read line fails, the additional thread is not
 * constructed.
 * 
 * @author jdp
 */
public class Request extends Thread {

    private static volatile long Count = 0L;

    public static final String Fcat(String a, String b) {
        if (null != a) {
            int term = (a.length() - 1);
            if (null != b) {
                if ('/' == b.charAt(0)) return (a + b); else if ('/' == a.charAt(term)) return (a + b); else return (a + '/' + b);
            } else if ('/' == a.charAt(term)) return a; else return (a + '/');
        } else if (null != b) {
            if ('/' == b.charAt(0)) return b; else return ('/' + b);
        } else return null;
    }

    public static final StringBuilder Join(String[] a, char b) {
        if (null != a) {
            StringBuilder strbuf = new StringBuilder();
            for (String s : a) {
                if (0 < strbuf.length()) strbuf.append(b);
                strbuf.append(s);
            }
            return strbuf;
        } else return null;
    }

    public static final String Camel(String string) {
        if (1 < string.length()) {
            String a = string.substring(0, 1).toUpperCase();
            String b = string.substring(1).toLowerCase();
            return (a + b);
        } else return string.toUpperCase();
    }

    public final String requestId, clientAddress;

    public final ClassLoader classLoader;

    private Server server;

    private Socket socket;

    private Input input;

    private Output output;

    private String requestline, method;

    private URI path;

    private Query query;

    private Protocol protocol;

    private Headers headers;

    private String location, locationPrefix;

    protected Request(Server server, Socket socket) throws IOException, URISyntaxException {
        this(server, socket, (socket.getInetAddress().getHostAddress()), (new Input(socket)), (Count++));
    }

    private Request(Server server, Socket socket, String cid, Input input, long rid) throws IOException, URISyntaxException {
        this(server, socket, cid, input, server.id(cid, rid), input.readLine());
    }

    private Request(Server server, Socket socket, String cid, Input input, String rid, String line) throws IOException, URISyntaxException {
        super(Group.Service, (rid + ' ' + line));
        this.server = server;
        this.socket = socket;
        this.clientAddress = cid;
        this.input = input;
        this.requestId = rid;
        this.output = new Output(socket);
        this.headers = new Headers();
        this.read(line);
        this.classLoader = Apps.Get(this);
    }

    public final void disableSocketTimeout() throws IOException {
        this.socket.setSoTimeout(0);
    }

    public final Server getServer() {
        return this.server;
    }

    public final Socket getSocket() {
        return this.socket;
    }

    public final SocketChannel getChannel() {
        return this.socket.getChannel();
    }

    public final Input getInput() {
        return this.input;
    }

    public final Output getOutput() {
        return this.output;
    }

    public final String getMethod() {
        return this.method;
    }

    public final URI getPath() {
        return this.path;
    }

    public final Query getQuery() {
        return this.query;
    }

    public final String getParameter(String name) {
        Query query = this.query;
        if (null != query) return query.get(name); else return null;
    }

    public final Protocol getProtocol() {
        return this.protocol;
    }

    public final boolean isProtocolHTTP10() {
        return this.protocol.isHTTP10();
    }

    public final boolean isProtocolHTTP11() {
        return this.protocol.isHTTP11();
    }

    public final String getHeader(String name) {
        return this.headers.valueOf(name);
    }

    public final String getHostName() {
        return this.headers.getHostName();
    }

    public final String getLocationPrefix() {
        String locationPrefix = this.locationPrefix;
        if (null == locationPrefix) {
            String host = this.headers.getHostName();
            StringBuilder prefix = new StringBuilder();
            prefix.append(this.protocol.scheme);
            prefix.append("://");
            prefix.append(host);
            if (this.protocol.isNotDefaultPort(this.server)) {
                prefix.append(':');
                prefix.append(this.server.getPort());
            }
            locationPrefix = prefix.toString();
            this.locationPrefix = locationPrefix;
        }
        return locationPrefix;
    }

    public final String getLocation() {
        String location = this.location;
        if (null == location) {
            String prefix = this.getLocationPrefix();
            location = Fcat(prefix, this.path.getPath());
            this.location = location;
        }
        return location;
    }

    public final String getLocation(String name) {
        if ('/' == name.charAt(0)) {
            String prefix = this.getLocationPrefix();
            return Fcat(prefix, name);
        } else {
            String prefix = this.getLocation();
            return Fcat(prefix, name);
        }
    }

    public Welcome getLocationWelcome() {
        Welcome html = this.getLocationFile("index.html");
        if (html.isFile()) return html; else {
            Welcome xml = this.getLocationFile("index.xml");
            if (xml.isFile()) return xml; else return html;
        }
    }

    public Welcome getLocationFile() {
        String host = this.headers.getHostName();
        if (null == host) host = "null";
        String path = this.path.getPath();
        return new Welcome(this, host, path);
    }

    public Welcome getLocationFile(String name) {
        if ('/' == name.charAt(0)) {
            String host = this.headers.getHostName();
            if (null == host) host = "null";
            return new Welcome(this, host, name);
        } else {
            String host = this.headers.getHostName();
            if (null == host) host = "null";
            String path = Fcat(this.path.getPath(), name);
            return new Welcome(this, host, path);
        }
    }

    public final long getContentLength() {
        return this.headers.getContentLength();
    }

    public final String getContentType() {
        return this.headers.getContentType();
    }

    public final boolean isContentTypeFormUrlencoded() {
        return this.headers.isContentTypeFormUrlencoded();
    }

    public final String getConnection() {
        return this.headers.getConnection();
    }

    public final void setConnectionKeepAlive() {
        this.headers.setConnectionKeepAlive();
    }

    public final boolean isConnectionKeepAlive() {
        return this.headers.isConnectionKeepAlive();
    }

    public final boolean isNotConnectionKeepAlive() {
        return this.headers.isNotConnectionKeepAlive();
    }

    public final void setConnectionClose() {
        this.headers.setConnectionClose();
    }

    public final boolean isConnectionClose() {
        return this.headers.isConnectionClose();
    }

    public final boolean isNotConnectionClose() {
        return this.headers.isNotConnectionClose();
    }

    public final long getIfModifiedSince() {
        return this.headers.getDate("If-Modified-Since");
    }

    public final Class getClassByName(String name) throws ClassNotFoundException {
        return this.classLoader.loadClass(name);
    }

    public final void classLoaderMark() {
        this.classLoader.mark();
    }

    protected final String getResponsePath() {
        String p = this.path.getPath();
        int idx1 = p.lastIndexOf('.');
        if (0 < idx1) {
            int idx0 = p.lastIndexOf('/');
            if (idx0 < idx1) return p.substring(0, idx1);
        }
        return p;
    }

    protected StringBuilder createResponsePackage() {
        return Join(this.getResponsePath().split("/"), '.');
    }

    protected Response createResponseFileGet() {
        return new File.Get();
    }

    protected Response createResponseBadRequest() {
        return Error.BadRequest.clone();
    }

    protected Response createResponse() {
        StringBuilder classname = this.createResponsePackage();
        if (null != classname) {
            classname.append('.');
            classname.append(Camel(this.method));
            try {
                Class clas = this.getClassByName(classname.toString());
                return (Response) clas.newInstance();
            } catch (ClassNotFoundException ignore) {
            } catch (InstantiationException exc) {
                this.server.log(this, exc);
                return Error.Server.clone();
            } catch (IllegalAccessException exc) {
                this.server.log(this, exc);
                return Error.Server.clone();
            }
        }
        if ("GET".equals(this.method)) return this.createResponseFileGet(); else return this.createResponseBadRequest();
    }

    public final void head(Output out) throws IOException {
        out.println(this.requestline);
        this.headers.write(out);
    }

    /**
     * Copy head for logging.
     */
    public final void head(PrintStream out) {
        out.println(this.requestline);
        this.headers.write(out);
    }

    public final void run() {
        Server server = this.server;
        Socket socket = this.socket;
        Input in = this.input;
        Output out = this.output;
        Headers headers = this.headers;
        Response response = null;
        try {
            boolean run = true;
            while (run) {
                response = this.createResponse();
                response.init(this);
                if (response.isValid()) {
                    response.head(out);
                    server.log(this, response);
                    response.tail(this, out);
                } else {
                    Response error = Error.Server.clone();
                    error.init(this);
                    error.head(out);
                    server.log(this, error);
                    error.tail(this, out);
                    throw new IOException("Invalid response in '" + response.getClass().getName() + "'.");
                }
                run = response.isConnectionKeepAlive();
                response = null;
                if (run) this.read(null);
            }
        } catch (Ignore exc) {
        } catch (IOException exc) {
            server.log(this, exc);
        } catch (URISyntaxException exc) {
            server.log(this, exc);
        } finally {
            try {
                socket.close();
            } catch (IOException ignore) {
            } finally {
                this.server = null;
                this.socket = null;
                this.input = null;
                this.output = null;
            }
        }
    }

    private void read(String line) throws IOException, URISyntaxException {
        Headers headers = this.headers;
        Input in = this.input;
        if (null == line) {
            try {
                line = in.readLine();
                if (null == line) throw new Ignore.Drop(); else headers.clear();
            } catch (java.net.SocketTimeoutException timeout) {
                throw new Ignore.Drop();
            }
        }
        String[] requestline = line.split(" ");
        if (3 == requestline.length) {
            this.requestline = line;
            this.method = requestline[0];
            this.path = new URI(requestline[1]);
            this.protocol = Protocol.Instance(requestline[2]);
            this.query = new Query(this.path);
            headers.read(in);
            headers.add("X-PS+ID", this.requestId);
        } else throw new IllegalArgumentException(line);
    }

    protected URL[] getClassPath() {
        String hostname = this.getHostName();
        if (null != hostname) {
            String string = "file:" + hostname + "/classes/";
            try {
                return new URL[] { (new URL(string)) };
            } catch (MalformedURLException exc) {
                throw new RuntimeException(string, exc);
            }
        } else {
            return new URL[0];
        }
    }
}
