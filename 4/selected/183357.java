package org.lindenb.net.httpserver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;
import org.lindenb.io.IOUtils;
import org.lindenb.json.JSONBuilder;
import org.lindenb.json.JSONable;
import org.lindenb.lang.ResourceUtils;
import org.lindenb.util.AbstractApplication;
import org.lindenb.util.Cast;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * AbstractHttpHandler
 * @author pierre
 *
 */
public abstract class AbstractHttpHandler extends AbstractApplication implements HttpHandler {

    /** ok http code */
    public static final int SC_OK = 200;

    public static final int SC_NOT_FOUND = 404;

    public static final int SC_TEMPORARILY_UNAVAILABLE = 504;

    /** HttpServer */
    private HttpServer server;

    /** max length input */
    private long max_length_input = Long.MAX_VALUE;

    /** a basic wrapper for request parameters */
    public abstract static class Parameters implements JSONable {

        /** returns all the parameters names */
        public abstract Set<String> getParameterNames();

        /** returns all the values for a given parameter */
        public abstract List<String> getParameters(String name);

        /** return a value for a given parameter or a default value */
        public String getParameter(String name, String defaultValue) {
            List<String> params = getParameters(name);
            return params.isEmpty() ? defaultValue : params.get(0);
        }

        /** return a value for a given parameter or null */
        public String getParameter(String name) {
            return getParameter(name, null);
        }

        /** returns true if there is a parameter with this name */
        public boolean contains(String name) {
            return getParameter(name) != null;
        }

        /** return true if the given paremeters is null or is blank */
        public boolean isEmpty(String name) {
            String value = getParameter(name);
            return value == null || value.trim().length() == 0;
        }

        public Double getDouble(String name) {
            return Cast.Double.cast(getParameter(name));
        }

        public Float getFloat(String name) {
            return Cast.Float.cast(getParameter(name));
        }

        public Integer getInt(String name) {
            return Cast.Integer.cast(getParameter(name));
        }

        public Long getLong(String name) {
            return Cast.Long.cast(getParameter(name));
        }

        public Short getShort(String name) {
            return Cast.Short.cast(getParameter(name));
        }

        @Override
        public String toJSON() {
            JSONBuilder b = new JSONBuilder();
            for (String s : getParameterNames()) {
                b.put(s, getParameters(s));
            }
            return b.toString();
        }

        @Override
        public String toString() {
            return toJSON();
        }
    }

    public static class Cookie {

        private String key;

        private String value;

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }

    /**
	 * ParametersImpl
	 * @author pierre
	 *
	 */
    protected static class ParametersImpl extends Parameters {

        private Map<String, List<String>> paramMap = new HashMap<String, List<String>>();

        public Map<String, List<String>> getMap() {
            return this.paramMap;
        }

        @Override
        public Set<String> getParameterNames() {
            return new HashSet<String>(getMap().keySet());
        }

        @Override
        public List<String> getParameters(String name) {
            List<String> values = getMap().get(name);
            if (values == null) return new ArrayList<String>(0);
            return new ArrayList<String>(values);
        }

        void add(String name, String value) {
            List<String> v = this.paramMap.get(name);
            if (v == null) {
                v = new ArrayList<String>();
                this.paramMap.put(name, v);
            }
            v.add(value == null ? "" : value);
        }
    }

    private class MaxInputStream extends FilterInputStream {

        private long curr_size = 0L;

        MaxInputStream(InputStream in) {
            super(in);
        }
    }

    public AbstractHttpHandler() {
        this(null);
    }

    public AbstractHttpHandler(HttpServer server) {
        this.server = server;
    }

    public HttpServer getServer() {
        return server;
    }

    public abstract void service(HttpExchange exchange) throws IOException;

    public void init() throws IOException {
    }

    public void release() {
    }

    public void handle(HttpExchange exchange) throws IOException {
        try {
            init();
            service(exchange);
        } catch (IOException e) {
            throw e;
        } catch (Throwable e) {
            throw new IOException(e);
        } finally {
            release();
        }
    }

    public void setContentType(HttpExchange exchange, String mime) {
        exchange.getResponseHeaders().set("Content-Type", mime);
    }

    public Parameters getParameters(HttpExchange http) throws IOException {
        ParametersImpl params = new ParametersImpl();
        String query = null;
        if (http.getRequestMethod().equalsIgnoreCase("GET")) {
            query = http.getRequestURI().getRawQuery();
        } else if (http.getRequestMethod().equalsIgnoreCase("POST")) {
            InputStream in = new MaxInputStream(http.getRequestBody());
            if (in != null) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                IOUtils.copyTo(in, bytes);
                query = new String(bytes.toByteArray());
                in.close();
            }
        } else {
            throw new IOException("Method not supported " + http.getRequestMethod());
        }
        if (query != null) {
            for (String s : query.split("[&]")) {
                s = s.replace('+', ' ');
                int eq = s.indexOf('=');
                if (eq > 0) {
                    params.add(URLDecoder.decode(s.substring(0, eq), "UTF-8"), URLDecoder.decode(s.substring(eq + 1), "UTF-8"));
                }
            }
        }
        return params;
    }

    protected long getMaxLengthInput() {
        return max_length_input;
    }

    protected void log(Object o) {
        if (LOG.getLevel() == Level.OFF) return;
        try {
            throw new Exception();
        } catch (Exception e) {
            LOG.log(LOG.getLevel(), getClass().getName() + ":" + String.valueOf(o));
        }
    }

    protected void log() {
        if (LOG.getLevel() == Level.OFF) return;
        try {
            throw new Exception();
        } catch (Exception e) {
            LOG.log(LOG.getLevel(), getClass().getName());
        }
    }

    /** 
	 * e.g.: "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.7) Gecko/2009021906 Firefox/3.0.7" */
    public boolean isFirefoxClient(HttpExchange exch, int majorVers, int minorVers, int microVers) {
        String s = exch.getRequestHeaders().getFirst("User-agent");
        if (s == null) return false;
        int i = s.indexOf("Firefox/");
        if (i == -1) return false;
        return true;
    }

    protected void dumpHeader(Headers headers) {
        for (Entry<String, List<String>> o : headers.entrySet()) {
            System.err.println("+\"" + o.getKey() + "\"");
            for (String v : o.getValue()) {
                System.err.println("\t\"" + v + "\"");
            }
        }
    }

    public PrintWriter createWriter(HttpExchange exch) throws IOException {
        return new PrintWriter(new OutputStreamWriter(createOutputStream(exch)));
    }

    public void compressResponse(HttpExchange exch) {
        exch.getResponseHeaders().add("Set-Cookie", "session1=1;");
        exch.getResponseHeaders().add("Set-Cookie", "session2=2;");
        exch.getResponseHeaders().set("Pragma", "no-cache");
        String accept = exch.getRequestHeaders().getFirst("Accept-encoding");
        if (accept != null) {
            for (String s : accept.split("[,]")) {
                if (s.trim().equals("gzip")) {
                    exch.getResponseHeaders().set("Content-Encoding", "gzip");
                    break;
                }
            }
        }
    }

    public OutputStream createOutputStream(HttpExchange exch) throws IOException {
        OutputStream out = exch.getResponseBody();
        String encode = exch.getResponseHeaders().getFirst("Content-Encoding");
        if (encode != null && encode.equals("gzip")) {
            out = new GZIPOutputStream(out);
        }
        return out;
    }

    protected void echo(File file, Writer out) throws IOException {
        FileReader r = new FileReader(file);
        IOUtils.copyTo(r, out);
        r.close();
    }

    protected void echo(Class<?> clazz, String name, Writer out) throws IOException {
        Reader r = ResourceUtils.openReader(clazz, name);
        IOUtils.copyTo(r, out);
        r.close();
    }
}
