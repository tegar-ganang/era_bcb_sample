package de.mguennewig.pobjform.tests;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/** A pseudo HTTP servlet response for tests.
 *
 * @author Michael G�nnewig
 * @see PseudoHttpServletRequest
 */
public class PseudoHttpServletResponse extends Object implements HttpServletResponse {

    private static final String CONTENT_LENGTH = "Content-Length";

    private static final String CONTENT_TYPE = "Content-Type";

    private static final String DEFAULT_ENCODING = "ISO-8859-1";

    private final SimpleDateFormat format;

    private final Map<String, List<String>> headers;

    private final ServletOutputStream out;

    private PrintWriter writer;

    private int status;

    private String message;

    private int contentLength;

    private String contentType;

    private String encoding;

    private Locale locale;

    private boolean commited;

    private PseudoHttpServletResponse(final ServletOutputStream out) {
        super();
        this.format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        this.format.setTimeZone(TimeZone.getTimeZone("GMT"));
        this.headers = new HashMap<String, List<String>>();
        this.out = out;
        this.status = HttpServletResponse.SC_OK;
        this.commited = false;
        this.contentLength = -1;
        this.locale = Locale.getDefault();
    }

    public PseudoHttpServletResponse(final File file) throws FileNotFoundException {
        this(new PseudoServletOutputStream(file));
    }

    public PseudoHttpServletResponse(final ByteArrayOutputStream out) {
        this(new PseudoServletOutputStream(out));
    }

    /** {@inheritDoc} */
    public void flushBuffer() throws IOException {
        if (writer != null) writer.flush(); else out.flush();
    }

    /** {@inheritDoc} */
    public int getBufferSize() {
        return -1;
    }

    /** {@inheritDoc} */
    public String getCharacterEncoding() {
        if (encoding == null) return DEFAULT_ENCODING;
        return encoding;
    }

    /** {@inheritDoc} */
    public final Locale getLocale() {
        return locale;
    }

    /** {@inheritDoc} */
    public ServletOutputStream getOutputStream() {
        if (writer != null) throw new IllegalStateException("getWriter already called");
        writeHeaders();
        return out;
    }

    /** {@inheritDoc} */
    public PrintWriter getWriter() {
        if (writer == null) {
            writeHeaders();
            final String lineSeparator = System.getProperty("line.separator");
            System.setProperty("line.separator", "\n");
            writer = new PrintWriter(out);
            System.setProperty("line.separator", lineSeparator);
        }
        return writer;
    }

    /** {@inheritDoc} */
    public final boolean isCommitted() {
        return commited;
    }

    /** {@inheritDoc} */
    public void reset() {
        headers.clear();
        status = SC_OK;
        contentLength = -1;
        contentType = null;
    }

    /** {@inheritDoc} */
    public void resetBuffer() {
    }

    /** {@inheritDoc} */
    public void setBufferSize(final int size) {
    }

    /** {@inheritDoc} */
    public void setContentLength(final int len) {
        contentLength = len;
    }

    /** {@inheritDoc} */
    public void setContentType(final String type) {
        contentType = type;
        if (type.indexOf(';') >= 0) {
            encoding = parseCharacterEncoding(type);
            if (encoding == null) encoding = DEFAULT_ENCODING;
        } else if (encoding != null) {
            contentType = type + ";charset=" + encoding;
        }
    }

    /** {@inheritDoc} */
    public void setLocale(final Locale locale) {
        this.locale = locale;
    }

    /** {@inheritDoc} */
    public void addCookie(final Cookie cookie) {
    }

    /** {@inheritDoc} */
    public void addDateHeader(final String name, final long date) {
        addHeader(name, format.format(new Date(date)));
    }

    /** {@inheritDoc} */
    public void addHeader(final String name, final String value) {
        if (!headers.containsKey(name)) headers.put(name, new ArrayList<String>());
        headers.get(name).add(value);
    }

    /** {@inheritDoc} */
    public void addIntHeader(final String name, final int value) {
        addHeader(name, Integer.toString(value));
    }

    /** {@inheritDoc} */
    public boolean containsHeader(final String name) {
        return headers.containsKey(name);
    }

    /** {@inheritDoc} */
    public String encodeRedirectURL(final String url) {
        return url;
    }

    /** {@inheritDoc} */
    @Deprecated
    public String encodeRedirectUrl(final String url) {
        return encodeRedirectURL(url);
    }

    /** {@inheritDoc} */
    public String encodeURL(final String url) {
        return url;
    }

    /** {@inheritDoc} */
    @Deprecated
    public String encodeUrl(final String url) {
        return encodeURL(url);
    }

    /** {@inheritDoc} */
    public void sendError(final int st) throws IOException {
        sendError(st, getStatusMessage(st));
    }

    /** {@inheritDoc} */
    public void sendError(final int st, final String msg) throws IOException {
        this.status = st;
        this.message = msg;
        resetBuffer();
        writeHeaders();
        if (writer != null) {
            writer.flush();
            writer.close();
        } else {
            out.flush();
            out.close();
        }
    }

    /** {@inheritDoc} */
    public void sendRedirect(final String location) throws IOException {
        resetBuffer();
        try {
            setStatus(SC_MOVED_TEMPORARILY);
            setHeader("Location", location);
        } catch (IllegalArgumentException e) {
            setStatus(SC_NOT_FOUND);
        }
        writeHeaders();
        if (writer != null) {
            writer.flush();
            writer.close();
        } else {
            out.flush();
            out.close();
        }
    }

    /** {@inheritDoc} */
    public void setDateHeader(final String name, long date) {
        setHeader(name, format.format(new Date(date)));
    }

    /** {@inheritDoc} */
    public void setHeader(final String name, final String value) {
        if (CONTENT_LENGTH.equalsIgnoreCase(name)) {
            int length = -1;
            try {
                length = Integer.parseInt(value);
            } catch (NumberFormatException e) {
            }
            if (length >= 0) setContentLength(length);
        } else if (CONTENT_TYPE.equalsIgnoreCase(name)) {
            setContentType(value);
        } else {
            headers.put(name, new ArrayList<String>());
            headers.get(name).add(value);
        }
    }

    /** {@inheritDoc} */
    public void setIntHeader(final String name, final int value) {
        setHeader(name, Integer.toString(value));
    }

    /** {@inheritDoc} */
    public void setStatus(final int status) {
        this.status = status;
        this.message = getStatusMessage(status);
    }

    /** {@inheritDoc} */
    @Deprecated
    public void setStatus(final int status, final String message) {
        this.status = status;
        this.message = message;
    }

    /** {@inheritDoc} */
    public final String getContentType() {
        return contentType;
    }

    /** {@inheritDoc} */
    public void setCharacterEncoding(final String encoding) {
        this.encoding = encoding;
    }

    private void writeHeaders() {
        OutputStreamWriter osw = null;
        try {
            osw = new OutputStreamWriter(out, getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            osw = new OutputStreamWriter(out);
        }
        final PrintWriter w = new PrintWriter(osw);
        w.print("HTTP/1.0 ");
        w.print(status);
        if (message != null) {
            w.print(" ");
            w.print(message);
        }
        w.print("\r\n");
        if (contentType != null) {
            w.print("Content-Type: ");
            w.print(contentType);
            w.print("\r\n");
        }
        if (contentLength >= 0) {
            w.print("Content-Length: ");
            w.print(contentLength);
            w.print("\r\n");
        }
        for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (final String value : entry.getValue()) {
                w.print(entry.getKey());
                w.print(": ");
                w.print(value);
                w.print("\r\n");
            }
        }
        w.print("\r\n");
        w.flush();
        commited = true;
    }

    protected static String getStatusMessage(final int status) {
        switch(status) {
            case SC_OK:
                return "OK";
            case SC_ACCEPTED:
                return "Accepted";
            case SC_BAD_GATEWAY:
                return "Bad Gateway";
            case SC_CONFLICT:
                return "Conflict";
            case SC_CONTINUE:
                return "Continue";
            case SC_CREATED:
                return "Created";
            case SC_EXPECTATION_FAILED:
                return "Expectation Failed";
            case SC_FORBIDDEN:
                return "Forbidden";
            case SC_GATEWAY_TIMEOUT:
                return "Gateway Timeout";
            case SC_GONE:
                return "Gone";
            case SC_HTTP_VERSION_NOT_SUPPORTED:
                return "HTTP Version Not Supported";
            case SC_INTERNAL_SERVER_ERROR:
                return "Internal Server Error";
            case SC_LENGTH_REQUIRED:
                return "Length Required";
            case SC_METHOD_NOT_ALLOWED:
                return "Method Not Allowed";
            case SC_MOVED_PERMANENTLY:
                return "Moved Permanently";
            case SC_MOVED_TEMPORARILY:
                return "Moved Temporarily";
            case SC_MULTIPLE_CHOICES:
                return "Multiple Choices";
            case SC_NO_CONTENT:
                return "No Content";
            case SC_NON_AUTHORITATIVE_INFORMATION:
                return "Non-Authoritative Information";
            case SC_NOT_ACCEPTABLE:
                return "Not Acceptable";
            case SC_NOT_FOUND:
                return "Not Found";
            case SC_NOT_IMPLEMENTED:
                return "Not Implemented";
            case SC_NOT_MODIFIED:
                return "Not Modified";
            case SC_PARTIAL_CONTENT:
                return "Partial Content";
            case SC_PAYMENT_REQUIRED:
                return "Payment Required";
            case SC_PRECONDITION_FAILED:
                return "Precondition Failed";
            case SC_PROXY_AUTHENTICATION_REQUIRED:
                return "Proxy Authentication Required";
            case SC_REQUEST_ENTITY_TOO_LARGE:
                return "Request Entity Too Large";
            case SC_REQUEST_TIMEOUT:
                return "Request Timeout";
            case SC_REQUEST_URI_TOO_LONG:
                return "Request URI Too Long";
            case SC_REQUESTED_RANGE_NOT_SATISFIABLE:
                return "Requested Range Not Satisfiable";
            case SC_RESET_CONTENT:
                return "Reset Content";
            case SC_SEE_OTHER:
                return "See Other";
            case SC_SERVICE_UNAVAILABLE:
                return "Service Unavailable";
            case SC_SWITCHING_PROTOCOLS:
                return "Switching Protocols";
            case SC_UNAUTHORIZED:
                return "Unauthorized";
            case SC_UNSUPPORTED_MEDIA_TYPE:
                return "Unsupported Media Type";
            case SC_USE_PROXY:
                return "Use Proxy";
            default:
                return "HTTP Response Status " + status;
        }
    }

    public static String parseCharacterEncoding(final String contentType) {
        if (contentType == null) return null;
        final int start = contentType.indexOf("charset=");
        if (start < 0) return null;
        String encoding = contentType.substring(start + 8);
        final int end = encoding.indexOf(';');
        if (end >= 0) encoding = encoding.substring(0, end);
        encoding = encoding.trim();
        if ((encoding.length() > 2) && encoding.startsWith("\"") && encoding.endsWith("\"")) encoding = encoding.substring(1, encoding.length() - 1);
        return encoding.trim();
    }
}
