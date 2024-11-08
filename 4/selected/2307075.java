package syntelos.tools;

import alto.io.Principal;
import alto.io.u.Bbuf;
import alto.io.u.Chbuf;
import alto.io.u.Objmap;
import alto.lang.Date;
import alto.lang.Header;
import alto.lang.HttpMessage;
import alto.lang.Type;
import alto.sys.Options;
import alto.sys.Thread;
import syntelos.net.http.Connection;
import syntelos.net.http.Request;
import syntelos.net.http.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;

/**
 * 
 * @author jdp
 * @since 1.6
 */
public class Client extends syntelos.sys.Init {

    public static final void Usage(java.io.PrintStream out) {
        out.println("Usage");
        out.println();
        out.println("    Client  [-H<name>:<value>]  <method>  <url>  [file]* ");
        out.println();
        out.println("Description");
        out.println();
        out.println("    Use a case insensitive HTTP method call to the argument URL.");
        out.println();
        out.println("    When present (and method not HEAD or GET), write the contents of the argument ");
        out.println("    file to the HTTP request entity body with content type, time and length ");
        out.println("    headers.");
        out.println();
        out.println("    Optionally define one or more case sensitive headers by name and value.");
        out.println();
        out.println("    Optionally repeat for multiple source files.");
        out.println();
        out.println("    When one or more files are present, the URL has no path or path '/'.");
        out.println("    Compose a URL for each file by concatenating the URL with the argument");
        out.println("    file path. ");
        out.println();
        out.println("    The header 'Overwrite' is a boolean that can cause the client to not employ the ");
        out.println("    Conditional PUT headers, as in '-HOverwrite:true'. ");
        out.println();
        out.println("Authentication");
        out.println();
        out.println("    The tool options '--username' and '--password' enable the use of a PFX keypair");
        out.println("    file in the working directory.  The environment variables 'SYNTELOS_USERNAME' ");
        out.println("    'SYNTELOS_PASSWORD' are equivalent.");
        out.println();
        out.println("    The tool options '--pfx' and '--password' enable the use of a PFX keypair file");
        out.println("    from another directory.  The environment variables 'SYNTELOS_PFX' ");
        out.println("    'SYNTELOS_PASSWORD' are equivalent.");
        out.println();
        out.println("    Also, any combination of command line arguments and environment variables will");
        out.println("    produce equivalent results.");
        out.println();
        out.println("    For example");
        out.println();
        out.println("    Define an environment variable named 'SYNTELOS_PFX' with an unambiguous path");
        out.println("    expression pointing to the PFX file produced using the Keys tool (or equivalent).");
        out.println("    Use this with the command line '--password pw' argument to enable client");
        out.println("    authentication.");
        out.println();
        System.exit(1);
    }

    public static final void Authenticate(HttpMessage user, HttpURLConnection connection) {
        if (connection instanceof Connection) {
            Request request = ((Connection) connection).getRequest();
            if (null == request) throw new java.lang.IllegalStateException("disconnected"); else {
                Principal.Authentic principal = null;
                try {
                    String pfxIn = user.getHeaderString("PFX-File");
                    if (null != pfxIn) {
                        String pfxPa = user.getHeaderString("PFX-Password");
                        if (null != pfxPa) try {
                            principal = new syntelos.net.Keys(pfxIn, pfxPa);
                        } catch (java.security.cert.CertificateException exc) {
                            throw new alto.sys.Error.State(pfxIn, exc);
                        } else throw new alto.sys.Error.State("Missing 'PFX-Password'.");
                    } else principal = user.authenticate();
                    if (request.authSign(principal)) Thread.Get().resetContext(request); else throw new alto.sys.Error.State("Authentication failed.");
                } catch (java.io.IOException exc) {
                    throw new alto.sys.Error.State(exc);
                }
            }
        } else throw new IllegalArgumentException(connection.getClass().getName());
    }

    public static final boolean UrlValidMultisource(URL target) {
        if (null == target) return false; else return UrlValidMultisource(target.getPath());
    }

    public static final boolean UrlValidMultisource(String path) {
        return (null == path || 1 > path.length() || "/".equals(path));
    }

    public static final boolean MethodValidSource(String method) {
        return (!("GET".equals(method) || "HEAD".equals(method)));
    }

    public static final boolean OptionOverwrite(HttpMessage headers) {
        if (null != headers) {
            if (headers.getHeaderBool("Overwrite")) return true; else if (headers.hasHeader("Conditional")) {
                if (headers.getHeaderBool("Conditional")) return false; else return true;
            }
        }
        return false;
    }

    private static final Objmap Excludes = new Objmap();

    static {
        Excludes.put("Overwrite", Boolean.TRUE);
        Excludes.put("Conditional", Boolean.TRUE);
        Excludes.put("PFX-File", Boolean.TRUE);
        Excludes.put("PFX-Password", Boolean.TRUE);
        Excludes.put("SAuth", Boolean.TRUE);
        Excludes.put("SAuth-UID", Boolean.TRUE);
    }

    public static final boolean Include(Header header) {
        return (!Excludes.containsKey(header.getName()));
    }

    public static final URL Call(String method, URL target, HttpMessage headers, File source, OutputStream dst) throws java.io.IOException {
        if (null != source) {
            String path = target.getPath();
            if (UrlValidMultisource(path)) {
                String querystring = target.getQuery();
                if (null != querystring) {
                    path = Chbuf.cat(source.getPath(), "?", querystring);
                    target = new URL(Chbuf.fcat(target.toExternalForm(), path));
                } else target = new URL(Chbuf.fcat(target.toExternalForm(), source.getPath()));
            }
        }
        HttpURLConnection http = (HttpURLConnection) target.openConnection();
        try {
            http.setRequestMethod(method);
            if (null != headers) {
                for (int cc = 0, count = headers.countHeaders(); cc < count; cc++) {
                    Header header = headers.getHeader(cc);
                    if (Include(header)) http.setRequestProperty(header.getName(), header.getValue());
                }
            }
            if (null != source && MethodValidSource(method)) {
                http.setDoInput(true);
                Bbuf content = new Bbuf(source);
                int contentLength = content.length();
                if (0 < contentLength) {
                    if (headers.hasNotContentType()) {
                        alto.lang.Type contentType = Type.Tools.Of(source.getName());
                        if (null != contentType) http.setRequestProperty("Content-Type", contentType.toString());
                    }
                    boolean overwrite = OptionOverwrite(headers);
                    long last = source.lastModified();
                    if (0L < last) {
                        String mod = Date.ToString(last);
                        http.setRequestProperty("Last-Modified", mod);
                        String etag = alto.sys.File.ETag(source.length(), last);
                        http.setRequestProperty("ETag", etag);
                        if (!overwrite) {
                            http.setRequestProperty("If-Unmodified-Since", mod);
                            http.setRequestProperty("If-None-Match", etag);
                        }
                    }
                    http.setRequestProperty("Content-Length", String.valueOf(contentLength));
                    OutputStream out = http.getOutputStream();
                    try {
                        content.writeTo(out);
                    } finally {
                        out.close();
                    }
                }
            }
            Authenticate(headers, http);
            http.connect();
            int status = http.getResponseCode();
            int contentLength = http.getContentLength();
            if (0 < contentLength && (!"HEAD".equalsIgnoreCase(method))) {
                InputStream in = (InputStream) http.getInputStream();
                if (null == dst) dst = System.out;
                byte[] iob = new byte[0x100];
                int read, remainder = contentLength, bufl = 0x100;
                while (0 < (read = in.read(iob, 0, bufl))) {
                    dst.write(iob, 0, read);
                    remainder -= read;
                    if (remainder < bufl) {
                        if (1 > remainder) break; else bufl = remainder;
                    }
                }
                if (0 < remainder) throw new java.io.IOException("Read '" + remainder + "' bytes short of '" + contentLength + "' from '" + status + " " + http.getResponseMessage() + "' @ " + target);
            }
            switch(status) {
                case 200:
                case 201:
                case 204:
                case 304:
                    return target;
                default:
                    throw new java.io.IOException(status + " " + http.getResponseMessage() + " @ " + target);
            }
        } finally {
            http.disconnect();
        }
    }

    public static final void main(java.lang.String[] argv) {
        if (Thread.In()) {
            if (SInitClient()) {
                int argvlen = argv.length;
                if (1 < argvlen) {
                    try {
                        boolean once = true;
                        HttpMessage headers = null;
                        String arg = null, method = null;
                        URL target = null, sent;
                        File source = null;
                        int argvidx = 0;
                        try {
                            Options options = Options.Instance;
                            if (options.hasPassword()) {
                                if (options.hasUsername()) {
                                    headers = new syntelos.lang.HttpMessage();
                                    headers.setHeader("PFX-File", options.getUsername() + ".pfx");
                                    headers.setHeader("PFX-Password", options.getPassword());
                                } else if (options.hasPfx()) {
                                    headers = new syntelos.lang.HttpMessage();
                                    java.io.File pfx = options.getPfx();
                                    if (pfx.isFile()) {
                                        headers.setHeader("PFX-File", pfx);
                                        headers.setHeader("PFX-Password", options.getPassword());
                                    } else {
                                        System.err.println("Error, PFX File not found: " + pfx.getAbsolutePath());
                                        System.exit(1);
                                    }
                                } else {
                                    System.err.println("Error, Found password without username or pfx.");
                                    System.exit(1);
                                }
                            }
                            for (; argvidx < argvlen; ) {
                                arg = argv[argvidx++];
                                if (arg.startsWith("-H")) {
                                    if (null == headers) headers = new syntelos.lang.HttpMessage();
                                    if (2 < arg.length()) {
                                        String line = arg.substring(2);
                                        Header header = new Header(line);
                                        headers.setHeader(header);
                                    } else {
                                        Usage(System.err);
                                    }
                                } else if (null == method) method = arg.toUpperCase(); else if (null == target) target = new URL(arg); else if (null == source) source = new File(arg); else if (UrlValidMultisource(target)) {
                                    if (once) {
                                        sent = Call(method, target, headers, source, null);
                                        System.err.println(method + ' ' + sent.toString());
                                        once = false;
                                    }
                                    source = new File(arg);
                                    sent = Call(method, target, headers, source, null);
                                    System.err.println(method + ' ' + sent.toString());
                                } else Usage(System.err);
                            }
                            if (once) {
                                if (null != source && "GET".equalsIgnoreCase(method)) {
                                    FileOutputStream dst = new FileOutputStream(source);
                                    try {
                                        sent = Call(method, target, headers, source, dst);
                                    } finally {
                                        dst.close();
                                    }
                                } else sent = Call(method, target, headers, source, null);
                                System.err.println(method + ' ' + sent.toString());
                            }
                            System.exit(0);
                        } catch (MalformedURLException exc) {
                            Usage(System.err);
                        }
                    } catch (IOException exc) {
                        exc.printStackTrace();
                        System.err.println(exc.getMessage());
                        System.exit(1);
                    }
                } else {
                    Usage(System.err);
                }
            } else {
                System.err.println(FailedSInitClient);
                System.exit(1);
            }
        } else {
            new Main(Client.class, argv).start();
        }
    }
}
