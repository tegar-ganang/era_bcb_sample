package com.volantis.mcs.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.IllegalStateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import com.volantis.synergetics.log.LogDispatcher;
import com.volantis.mcs.localization.LocalizationFactory;

public class HttpStringResponse implements HttpServletResponse {

    /** Copyright */
    private static String mark = "(c) Volantis Systems Ltd 2000.";

    private static final String RFC1123_DATE_SPEC = "EEE, dd MMM yyyy HH:mm:ss z";

    /**
     * Used for logging
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(HttpStringResponse.class);

    private List cookies;

    private Map headers;

    private String contentType;

    private String encoding;

    private PrintWriter writer;

    private StringWriter stringWriter;

    private ByteArrayOutputStream outputStream;

    private ServletOutputStream servletStream;

    private int status;

    private String statusMessage;

    /** Creates a new instance of HttpStringResponse */
    public HttpStringResponse() {
        cookies = new ArrayList();
        headers = new HashMap();
        encoding = "us-ascii";
    }

    /**
     * Add a cookie to the cookie jar
     * @param cookie  The cookie to add
     */
    public void addCookie(javax.servlet.http.Cookie cookie) {
        cookies.add(cookie);
    }

    /**
     * Add a date header
     */
    public void addDateHeader(String str, long param) {
        addHeader(str, asDateHeaderValue(param));
    }

    /**
     * Adds a response header with the given name and value.
     * This method allows response headers to have multiple values.
     **/
    public void addHeader(String name, String value) {
        synchronized (headers) {
            String key = name.toUpperCase();
            ArrayList values = (ArrayList) headers.get(key);
            if (values == null) {
                values = new ArrayList();
                headers.put(key, values);
            }
            values.add(value);
        }
    }

    /**
     * Add an integer header
     */
    public void addIntHeader(String str, int param) {
        addHeader(str, Integer.toString(param));
    }

    /**
     * Checks whether the response message header has a field with
     * the specified name.
     */
    public boolean containsHeader(String str) {
        return headers.containsKey(str);
    }

    /**
     * Encodes the specified URL for use in the
     * <code>sendRedirect</code> method or, if encoding is not needed,
     * returns the URL unchanged.  The implementation of this method
     * should include the logic to determine whether the session ID
     * needs to be encoded in the URL.  Because the rules for making
     * this determination differ from those used to decide whether to
     * encode a normal link, this method is seperate from the
     * <code>encodeUrl</code> method.
     **/
    public String encodeRedirectURL(String url) {
        return url;
    }

    /**
     * @deprecated use encodeRedirectURL()
     */
    public String encodeRedirectUrl(String str) {
        return encodeRedirectURL(str);
    }

    /**
     * Encodes the specified URL by including the session ID in it,
     * or, if encoding is not needed, returns the URL unchanged.
     * The implementation of this method should include the logic to
     * determine whether the session ID needs to be encoded in the URL.
     * For example, if the browser supports cookies, or session
     * tracking is turned off, URL encoding is unnecessary.
     **/
    public String encodeURL(String url) {
        return url;
    }

    /**
     * @deprecated use encodeURL()
     */
    public String encodeUrl(String str) {
        return encodeURL(str);
    }

    /**
     * Flush the buffer
     */
    public void flushBuffer() throws java.io.IOException {
    }

    /**
     * Return the buffer size in bytes
     */
    public int getBufferSize() {
        return 0;
    }

    /**
     * Return the current character encoding
     */
    public String getCharacterEncoding() {
        return encoding;
    }

    public void setCharacterEncoding(String encoding) {
        this.encoding = encoding;
        if (contentType == null || contentType.length() == 0) {
            contentType = "text/plain";
        }
        setHeader("Content-type", contentType + "; charset=" + encoding);
    }

    /**
     * Return the current locale
     */
    public java.util.Locale getLocale() {
        return null;
    }

    /**
     * Returns a ServletOutputStream suitable for writing binary
     * data in the response. The servlet engine does not encode the
     * binary data.
     *
     * @exception IllegalStateException if you have already called the
     * <code>getWriter</code> method
     **/
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException("Tried to create output stream; writer already exists");
        }
        if (servletStream == null) {
            outputStream = new ByteArrayOutputStream();
            servletStream = new ServletStringOutputStream(outputStream);
            if (logger.isDebugEnabled()) {
                logger.debug("Create ServletOutputStream for outputStream " + outputStream);
            }
        }
        return servletStream;
    }

    /**
     * Returns a <code>PrintWriter</code> object that you
     * can use to send character text to the client.
     * The character encoding used is the one specified
     * in the <code>charset=</code> property of the
     * {@link #setContentType} method, which you must call
     * <i>before</i> you call this method.
     *
     * <p>If necessary, the MIME type of the response is
     * modified to reflect the character encoding used.
     *
     * <p> You cannot use this method if you have already
     * called {@link #getOutputStream} for this
     * <code>ServletResponse</code> object.
     *
     * @exception UnsupportedEncodingException
     *          if the character encoding specified in
     *                        <code>setContentType</code> cannot be used
     *
     * @exception IllegalStateException
     *          if the <code>getOutputStream</code>
     *                        method has already been called for this
     *                        response object; in that case, you can't
     *                        use this method
     *
     **/
    public java.io.PrintWriter getWriter() throws java.io.IOException {
        if (servletStream != null) {
            throw new IllegalStateException("Tried to create writer; output stream already exists");
        }
        if (writer == null) {
            stringWriter = new StringWriter();
            writer = new PrintWriter(stringWriter);
            if (logger.isDebugEnabled()) {
                logger.debug("Created writer for output stream " + outputStream);
            }
        }
        return writer;
    }

    /**
     * Returns a boolean indicating if the response has been committed. A commited response has
     * already had its status code and headers written.
     **/
    public boolean isCommitted() {
        return false;
    }

    /**
     * Clears any data that exists in the buffer as well as the status code and headers.
     * If the response has been committed, this method throws an IllegalStateException.
     **/
    public void reset() {
    }

    /**
     * Clears the content of the underlying buffer in the response without clearing headers or status code.
     * If the response has been committed, this method throws an IllegalStateException.
     *
     * @since 1.3
     */
    public void resetBuffer() {
    }

    /**
     * Sends an error response to the client using the specified status
     * code and descriptive message.  If setStatus has previously been
     * called, it is reset to the error status code.  The message is
     * sent as the body of an HTML page, which is returned to the user
     * to describe the problem.  The page is sent with a default HTML
     * header; the message is enclosed in simple body tags
     * (&lt;body&gt;&lt;/body&gt;).
     **/
    public void sendError(int sc) throws IOException {
        sendError(sc, "");
    }

    /**
     * Sends an error response to the client using the specified status
     * code and descriptive message.  If setStatus has previously been
     * called, it is reset to the error status code.  The message is
     * sent as the body of an HTML page, which is returned to the user
     * to describe the problem.  The page is sent with a default HTML
     * header; the message is enclosed in simple body tags
     * (&lt;body&gt;&lt;/body&gt;).
     **/
    public void sendError(int sc, String msg) throws IOException {
        setStatus(sc);
        statusMessage = msg;
        writer = null;
        servletStream = null;
        setContentType("text/html");
        getWriter().println("<html><head><title>" + msg + "</title></head>" + "<body>" + msg + "</body></html>");
    }

    /**
     * Sends a temporary redirect response to the client using the
     * specified redirect location URL.  The URL must be absolute (for
     * example, <code><em>https://hostname/path/file.html</em></code>).
     * Relative URLs are not permitted here.
     */
    public void sendRedirect(String location) throws IOException {
        setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        setHeader("Location", location);
    }

    /**
     * Sets the preferred buffer size for the body of the response. The servlet container
     * will use a buffer at least as large as the size requested. The actual buffer size
     * used can be found using getBufferSize.
     **/
    public void setBufferSize(int size) {
    }

    public void setContentLength(int param) {
    }

    /**
     * Sets the content type of the response the server sends to
     * the client. The content type may include the type of character
     * encoding used, for example, <code>text/html; charset=ISO-8859-4</code>.
     *
     * <p>You can only use this method once, and you should call it
     * before you obtain a <code>PrintWriter</code> or
     * {@link ServletOutputStream} object to return a response.
     **/
    public void setContentType(String type) {
        String[] typeAndEncoding = parseContentTypeHeader(type);
        contentType = typeAndEncoding[0];
        if (typeAndEncoding[1] != null) {
            encoding = typeAndEncoding[1];
        } else {
            encoding = "ISO-8859-4";
        }
        setHeader("Content-type", contentType + "; charset=" + encoding);
    }

    public void setDateHeader(String str, long param) {
        addDateHeader(str, param);
    }

    public void setHeader(String str, String str1) {
        addHeader(str, str1);
    }

    public void setIntHeader(String str, int param) {
        addIntHeader(str, param);
    }

    public void setLocale(java.util.Locale locale) {
    }

    public void setStatus(int param) {
        status = param;
    }

    public void setStatus(int param, String str) {
        status = param;
        statusMessage = str;
    }

    /**
     * Return a date value as a string
     * @param date  Date in time_t format
     * @return the date a a string
     */
    private String asDateHeaderValue(long date) {
        Date value = new Date(date);
        SimpleDateFormat formatter = new SimpleDateFormat(RFC1123_DATE_SPEC);
        formatter.setTimeZone(TimeZone.getTimeZone("Greenwich Mean Time"));
        return formatter.format(value);
    }

    /**
     * Returns the content type and encoding as a pair of strings.
     * If no character set is specified, the second entry will be null.
     **/
    private String[] parseContentTypeHeader(String header) {
        String[] result = new String[] { "text/plain", null };
        StringTokenizer st = new StringTokenizer(header, ";=");
        result[0] = st.nextToken();
        while (st.hasMoreTokens()) {
            String parameter = st.nextToken();
            if (st.hasMoreTokens()) {
                String value = stripQuotes(st.nextToken());
                if (parameter.trim().equalsIgnoreCase("charset")) {
                    result[1] = value;
                }
            }
        }
        return result;
    }

    private String stripQuotes(String value) {
        if (value.startsWith("'") || value.startsWith("\"")) {
            value = value.substring(1);
        }
        if (value.endsWith("'") || value.endsWith("\"")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * Allow access to the output buffer
     */
    public String getContents() throws UnsupportedEncodingException {
        if (logger.isDebugEnabled()) {
            logger.debug("Getting contents, encoding " + encoding);
        }
        if (servletStream != null) {
            return outputStream.toString(encoding);
        } else {
            return stringWriter.toString();
        }
    }

    /** 
     * return the content type
     */
    public String getContentType() {
        return contentType;
    }
}
