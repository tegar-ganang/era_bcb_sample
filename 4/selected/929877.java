package de.mguennewig.pobjform.html;

import java.io.*;
import java.util.*;
import javax.servlet.http.*;

/**
 * The class <code>MultipartRequest</code> allows servlets to process file
 * uploads.
 *
 * <p>Formally speaking, it supports requests with
 * <code>multipart/form-data</code> content type. This content type is used to
 * submit forms that has the <code>multipart/form-data</code> encoding type,
 * which is used to upload files and is not directly supported by the Servlet
 * Specification.</p>
 *
 * <p>The class <code>MultipartRequest</code> takes an
 * <code>HttpServletRequest</code>, parses it extracting any parameters and
 * files and exposes them through an API. Notice that the class
 * <code>MultipartRequest</code> supports regular requests as well so that
 * it is possible to process any request using a single API.</p>
 *
 * <p>File parameters are passed as {@link MultipartRequest.File} objects,
 * which encapsulates the file's properties and contents. Regular parameters
 * are passed as <code>String</code> objects.</p>
 *
 * <p>Notice that the class <code>MultipartRequest</code> supports a simplified
 * version of MIME entity headers, specifically it does not support character
 * escaping, header wrapping, comments nor any extensions.
 * Finally, it does not support <code>multipart/mixed</code> parts, which are
 * used to send multiple files as a single parameter, and it assumes that the
 * request is well formed, so no error checking is performed.</p>
 */
public class MultipartRequest extends Object {

    private static final String DEFAULT_ENCODING = "ISO-8859-1";

    private static final char CR = 13;

    private static final char LF = 10;

    private static final long TIMEOUT = 30 * 60 * 1000;

    private static final int GRANULARITY = 128;

    private static int nextId;

    private static final Map<Integer, MultipartRequest> objects = new HashMap<Integer, MultipartRequest>();

    private final int id;

    private long expiration;

    private HttpServletRequest request;

    private final Map<String, List<Object>> parameters;

    private int total;

    private int processed;

    private String file;

    private int internalCount;

    private int unreadCount;

    /**
   * The class <code>MultipartRequest.File</code> encapsulates uploaded files.
   *
   * <p>Objects of this class are the values of file parameters.  This
   * implementation saves the data as temporary files in the directory
   * specified by the system property <code>java.io.tmpdir</code>.</p>
   */
    public static final class File {

        private final String name;

        private java.io.File file;

        private String type;

        private InputStream input;

        private OutputStream output;

        /** Creates a new <code>MultipartRequest.File</code> object.
     *
     * @param name original file name
     * @throws IOException if an error occurs while creating the temporary file
     */
        File(final String name) throws IOException {
            super();
            this.name = getBasename(name);
            this.type = "";
            this.file = java.io.File.createTempFile("mrf", null);
        }

        /** Returns the base name of a filename.
     *
     * <p>Some browsers like IE under Windows send the full path to the server,
     * and this method can be used to cut off any path informations.</p>
     */
        private static String getBasename(final String filename) {
            String base = filename;
            int pos = base.lastIndexOf('\\');
            if (pos != -1) base = base.substring(pos + 1);
            pos = base.lastIndexOf('/');
            if (pos != -1) base = base.substring(pos + 1);
            return base;
        }

        /** Gets an input stream to read the contents of this object.
     *
     * <p>The input stream returned by a previous call to
     * {@link #getInputStream()}, if any, is automatically closed.</p>
     *
     * @return an input stream to read this object's contents
     * @throws IOException if an error occurs while opening the input stream
     */
        public InputStream getInputStream() throws IOException {
            if (input != null) input.close();
            input = new BufferedInputStream(new FileInputStream(file));
            return input;
        }

        /** Gets the length of this file.
     *
     * @return The length of this file
     */
        public long getLength() {
            return file.length();
        }

        /** Gets the original file name, as sent by the request.
     *
     * <p>Notice that the file name depends on the client's platform.</p>
     *
     * @return The original file name
     */
        public String getName() {
            return name;
        }

        /** Gets an output stream to write to this object.
     *
     * <p>The output stream returned by a previous call to
     * {@link #getOutputStream()}, if any, is automatically closed.</p>
     *
     * @return an output stream to write to this object
     * @throws IOException if an error occurs while opening the output stream
     */
        OutputStream getOutputStream() throws IOException {
            if (output != null) output.close();
            output = new BufferedOutputStream(new FileOutputStream(file));
            return output;
        }

        /** Gets the MIME type of the file, as sent by the client.
     *
     * <p>Notice that, since MIME types are case insensitive, the type is
     * always returned in lower case.</p>
     *
     * @return the MIME type of the file or an empty string if the type
     *   is not known
     */
        public String getType() {
            return type;
        }

        /** Releases any resources held by this class.
     *
     * <p>After calling this object is not valid anymore.</p>
     *
     * @throws IOException if an error occurs while closing any opened streams
     *   or deleting the temporary file
     */
        void release() throws IOException {
            if (file == null) return;
            if (input != null) input.close();
            if (output != null) output.close();
            if (!file.delete()) throw new IOException("Failed to release file: " + file);
            file = null;
        }

        /** Sets the type of this file.
     *
     * <p>The type must be specified according MIME standards.</p>
     *
     * @param type Type of the file
     */
        void setType(final String type) {
            if (type == null) this.type = ""; else this.type = type.toLowerCase();
        }
    }

    /**
   * Background thread to release expired <code>MultipartRequest</code>
   * objects. The class <code>ReleaseThread</code> represents a thread that
   * runs in the background periodically releasing objects that expired.
   */
    private static class ReleaseThread extends Thread {

        ReleaseThread() {
            super();
        }

        /**
     * Releases expired <code>MultipartRequest</code> objects. Periodically
     * looks for expired objects and automatically releases them.
     */
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    sleep(300000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                releaseExpired();
            }
        }
    }

    static {
        new ReleaseThread().start();
    }

    /**
   * Copies all parameters from a request to this
   * <code>MultipartRequest</code>.
   *
   * @param req Request from which to copy the parameters
   */
    private void copyParameters(final HttpServletRequest req) {
        final Enumeration<?> e = req.getParameterNames();
        while (e.hasMoreElements()) {
            final String name = (String) e.nextElement();
            final Object[] values = req.getParameterValues(name);
            parameters.put(name, Arrays.asList(values));
        }
    }

    /** Parses a MIME header.
   *
   * <p>The header is returned as a <code>Map</code> where the keys are the
   * header's and parameters' names and the values are the corresponding
   * header's body (less any parameters) and parameters' values.
   * The header's and parameters' names are converted to lower case since they
   * are case insensitive.</p>
   *
   * @param header The header to parse
   *
   * @return a <code>Map</code> with the header and any parameters
   */
    private Map<String, String> parseHeader(String header) {
        final HashMap<String, String> map = new HashMap<String, String>();
        if (header == null) header = "";
        while (!"".equals(header)) {
            int delimiter = header.indexOf(';');
            final String token;
            if (delimiter == -1) {
                token = header.trim();
                header = "";
            } else {
                token = header.substring(0, delimiter).trim();
                header = header.substring(delimiter + 1);
            }
            delimiter = token.indexOf('=');
            if (delimiter == -1) delimiter = token.indexOf(':');
            if (delimiter != -1) {
                final String key = token.substring(0, delimiter).trim().toLowerCase();
                String value = token.substring(delimiter + 1).trim();
                if (value.length() > 0 && value.charAt(0) == '"') value = value.substring(1, value.length() - 1);
                map.put(key, value);
            }
        }
        return map;
    }

    /** Parses a request, populating this <code>MultipartRequest</code>.
   *
   * <p>If the content type of the request is <code>multipart/form-data</code>,
   * parses it, extracting any parameters and files, populating this object.
   * </p>
   *
   * @param req Request to parse
   * @throws IOException if an error occurs while reading the request, writing
   *   to temporary files, or if the pushback buffer is too small
   */
    private void parseRequest(final HttpServletRequest req) throws IOException {
        String encoding = req.getCharacterEncoding();
        Map<String, String> map = parseHeader(req.getHeader("content-type"));
        String boundary = map.get("boundary");
        if (boundary == null || boundary.length() == 0) return;
        boundary = "" + CR + LF + "--" + boundary;
        if (encoding == null) encoding = DEFAULT_ENCODING;
        final PushbackInputStream input = new PushbackInputStream(new BufferedInputStream(req.getInputStream()), 128);
        unread(LF, input);
        unread(CR, input);
        int c;
        do {
            c = read(input, boundary);
        } while (c != -1);
        while (c != -2) {
            String header = null;
            String type = null;
            String name = null;
            OutputStream out = null;
            File f = null;
            while (!"".equals(header)) {
                header = readLine(input);
                map = parseHeader(header);
                if (map.containsKey("content-disposition")) {
                    name = map.get("name");
                    if (map.containsKey("filename")) {
                        f = new File(map.get("filename"));
                        setFile(f.getName());
                        putParameter(name, f);
                        out = f.getOutputStream();
                    } else out = new ByteArrayOutputStream();
                } else if (map.containsKey("content-type")) {
                    type = map.get("content-type");
                    if (map.containsKey("charset")) encoding = map.get("charset"); else encoding = DEFAULT_ENCODING;
                }
            }
            if (f != null) f.setType(type);
            if (out == null) throw new IOException("Failed to parse the request header");
            while ((c = read(input, boundary)) >= 0) out.write(c);
            out.close();
            if (f == null) putParameter(name, ((ByteArrayOutputStream) out).toString(encoding));
        }
    }

    /** Saves a parameter and its value into the parameter map.
   *
   * <p>Notice that the values are always saved as a <code>List</code>. If the
   * parameter already exists in the parameter map, adds the new value to its
   * <code>List</code>, otherwise creates a new one to hold the value.</p>
   *
   * @param name Name of the parameter
   * @param value Value of the parameter
   */
    private void putParameter(final String name, final Object value) {
        List<Object> values = parameters.get(name);
        if (values == null) {
            values = new ArrayList<Object>();
            parameters.put(name, values);
        }
        values.add(value);
    }

    /**
   * Reads a byte from the request's body and updates the number of processed
   * bytes.
   *
   * <p>Notice that the counter is updated only when the actual number of bytes
   * processed is a multiple of a <i>granularity factor</i>, in order to
   * improve performance.  The method also takes into account any bytes in the
   * pushback buffer.</p>
   *
   * @param input Input stream to the request's body
   * @throws IOException if an error occurs while reading the request
   */
    private int read(final PushbackInputStream input) throws IOException {
        if (unreadCount <= 0) {
            internalCount++;
            if (internalCount % GRANULARITY == 0) setProcessed(internalCount);
        } else unreadCount--;
        return input.read();
    }

    /** Reads a character from the request's body.
   *
   * <p>The method automatically detects, consumes and reports boundaries.
   * Notice that the boundary passed must include the preceding
   * <code>CRLF</code> and the two dashes.</p>
   *
   * @param input Request's body
   * @param boundary Boundary that delimits entities
   * @return The character read from the request's body, -1 if a boundary was
   *   detected or -2 if the ending boundary was detected
   * @throws IOException if an error occurs while reading the request or if the
   *   pushback buffer is too small
   */
    private int read(final PushbackInputStream input, final String boundary) throws IOException {
        final StringBuilder buffer = new StringBuilder();
        int index = -1;
        int c;
        do {
            c = read(input);
            if (c == -1) throw new EOFException();
            buffer.append((char) c);
            index++;
        } while ((buffer.length() < boundary.length()) && (c == boundary.charAt(index)));
        if (c == boundary.charAt(index)) {
            int type = -1;
            c = read(input);
            if (c == '-') type = -2;
            if (c == -1) throw new EOFException();
            while ((c = read(input)) != LF) {
                if (c == -1) throw new EOFException();
            }
            return type;
        } else {
            while (index >= 0) {
                unread(buffer.charAt(index), input);
                index--;
            }
            return read(input);
        }
    }

    /** Reads a line from the request's body, skipping the terminating CRLF.
   *
   * @param input Request's body
   * @return The line read from the request's body
   * @throws IOException if an error occurs while reading the request
   */
    private String readLine(final PushbackInputStream input) throws IOException {
        final StringBuilder line = new StringBuilder();
        int c;
        while ((c = read(input)) != CR) {
            if (c == -1) throw new EOFException();
            line.append((char) c);
        }
        c = read(input);
        if (c == -1) throw new EOFException();
        return line.toString();
    }

    /** Releases all expired <code>MultipartRequest</code> objects.
   *
   * <p>The method <code>releaseExpired()</code> tests all objects that were
   * note released yet and automatically release those that are expired.</p>
   */
    static void releaseExpired() {
        final long time = System.currentTimeMillis();
        final MultipartRequest[] array;
        synchronized (objects) {
            final Collection<MultipartRequest> values = objects.values();
            array = values.toArray(new MultipartRequest[values.size()]);
        }
        for (int i = 0; i < array.length; i++) {
            if (time > array[i].expiration) {
                try {
                    array[i].release();
                } catch (IOException e) {
                }
            }
        }
    }

    /** Sets the file being uploaded.
   *
   * @param file File being uploaded
   */
    private synchronized void setFile(final String file) {
        this.file = file;
    }

    /** Sets the number of bytes already processed.
   *
   * @param processed Bytes already processed
   */
    private synchronized void setProcessed(final int processed) {
        this.processed = processed;
    }

    /** Sets the total number of bytes of the request.
   *
   * @param total Total number of bytes
   */
    private synchronized void setTotal(final int total) {
        this.total = total;
    }

    /** Pushes a byte back into the request's body input stream.
   *
   * <p>The method updates a count of pushed back bytes in order to take them
   * into account when updating the number of bytes processed.</p>
   *
   * @param b Byte to push back
   * @param input Request's body input stream
   * @throws IOException if the pushback buffer is too small
   */
    private void unread(final int b, final PushbackInputStream input) throws IOException {
        unreadCount++;
        input.unread(b);
    }

    /**
   * Creates a new, empty <code>MultipartRequest</code> with a default
   * expiration date.
   */
    public MultipartRequest() {
        synchronized (objects) {
            id = nextId++;
            objects.put(Integer.valueOf(id), this);
        }
        expiration = System.currentTimeMillis() + TIMEOUT;
        parameters = new HashMap<String, List<Object>>();
        setFile("");
    }

    /**
   * Gets a <code>MultipartRequest</code> object given its <code>id</code>.
   *
   * @param id Object <code>ID</code>
   * @return The object with the given <code>ID</code> or <code>null</code>
   *   if it doesn't exist or if it was released
   */
    public static MultipartRequest get(final int id) {
        synchronized (objects) {
            return objects.get(Integer.valueOf(id));
        }
    }

    /** Gets the expiration of this <code>MultipartRequest</code> object.
   *
   * @return The expiration of this object
   */
    public final long getExpiration() {
        return expiration;
    }

    /** Gets the file being uploaded.
   *
   * @return The name of the file being uploaded or an empty string if there
   *   is no file being uploaded
   */
    public synchronized String getFile() {
        return file;
    }

    /**
   * Convenient method that returns the value of a
   * {@link MultipartRequest.File} parameter.
   *
   * <p>If the parameter has multiple values, returns just the first one. Use
   * the method {@link #getParameterValues(String)} to get all values.</p>
   *
   * @param name Name of the desired parameter
   * @return The value of the given parameter, casted to a
   *   {@link MultipartRequest.File}, or <code>null</code> if the parameter
   *   does not exist
   */
    public final File getFileParameter(final String name) {
        return (File) getParameter(name);
    }

    /** Gets the <code>id</code> of this <code>MultipartRequest</code>.
   *
   * <p>Each object is guaranteed to have a unique <code>id</code>.</p>
   *
   * @return The <code>id</code> of this object
   */
    public final int getId() {
        return id;
    }

    /**
   * Returns the value of a given parameter, or <code>null</code> if the
   * parameter doesn't exist.
   *
   * <p>The value of the parameter is a <code>String</code> or a
   * {@link MultipartRequest.File} object. If the parameter has multiple
   * values, returns just the first one.  Use the method
   * {@link #getParameterValues(String)} to get all values.</p>
   *
   * @param name Name of the desired parameter
   * @return The value of the given parameter
   */
    public Object getParameter(final String name) {
        final List<Object> values = parameters.get(name);
        if (values == null) return null;
        return values.get(0);
    }

    /**
   * Returns an <code>Iterator</code> that iterates over the names of the
   * parameters contained in this <code>MultipartRequest</code>.
   *
   * <p>The names of the parameters are <code>String</code> objects.</p>
   *
   * @return The names of the parameters, as an <code>Iterator</code>
   */
    public Iterator<String> getParameterNames() {
        return Collections.unmodifiableSet(parameters.keySet()).iterator();
    }

    /**
   * Returns an array of objects containing all of the values the given
   * request parameter has, or <code>null</code> if the parameter does not
   * exist.
   *
   * <p>The values of the parameters are <code>String</code> or
   * {@link MultipartRequest.File} objects.</p>
   *
   * @param name Name of the parameter desired
   *
   * @return The values of the requested parameter
   */
    public Object[] getParameterValues(final String name) {
        final List<Object> values = parameters.get(name);
        if (values != null) return values.toArray();
        return null;
    }

    /** Gets the number of bytes of the request's body already processed.
   *
   * <p>This method can be called by another thread.</p>
   *
   * @return Number of bytes already processed
   */
    public final synchronized int getProcessed() {
        return processed;
    }

    /** Gets the request corresponding to this <code>MultipartRequest</code>.
   *
   * @return The original <code>HttpServletRequest</code>
   */
    public final HttpServletRequest getRequest() {
        return request;
    }

    /**
   * Convenient method that returns the value of a <code>String</code>
   * parameter.
   *
   * <p>If the parameter has multiple values, returns just the first one.  Use
   * the method {@link #getParameterValues(String)} to get all values.</p>
   *
   * @param name Name of the desired parameter
   *
   * @return The value of the given parameter, casted to a <code>String</code>,
   *   or <code>null</code> if the parameter does not exist
   */
    public String getStringParameter(final String name) {
        return (String) getParameter(name);
    }

    /** Gets the total number of bytes of the request.
   *
   * @return The total number of bytes
   */
    public final synchronized int getTotal() {
        return total;
    }

    /**
   * Releases this <code>MultipartRequest</code> object and all of its
   * parameters.
   *
   * <p>This method should be called when this object is not needed anymore.
   * If this object is not explicitly released before its expiration, it will
   * be automatically released when it expires.</p>
   *
   * @throws IOException if an error occurs while releasing the temporary files
   */
    public void release() throws IOException {
        try {
            for (final List<Object> values : parameters.values()) {
                for (int i = 0; i < values.size(); i++) {
                    final Object obj = values.get(i);
                    if (obj instanceof File) ((File) obj).release();
                }
            }
        } finally {
            synchronized (objects) {
                objects.remove(Integer.valueOf(getId()));
            }
        }
    }

    /**
   * Sets the expiration date for this <code>MultipartRequest</code>.
   *
   * <p>The expiration date is specified using the same base time as the one
   * used by the method <code>System.currentTimeMillis()</code>.</p>
   *
   * @param expiration Expiration date
   */
    public void setExpiration(final long expiration) {
        this.expiration = expiration;
    }

    /** Sets a request for this <code>MultipartRequest</code>.
   *
   * <p>Parses the request and populates this object with the parameters from
   * it.  Notice that <code>MultipartRequest</code> are meant to be used only
   * once, that is, they can process just one request.</p>
   *
   * @param request Client's request
   * @throws IOException if an error occurs while processing the request
   */
    public void setRequest(final HttpServletRequest request) throws IOException {
        this.request = request;
        setTotal(request.getContentLength());
        copyParameters(request);
        parseRequest(request);
    }
}
