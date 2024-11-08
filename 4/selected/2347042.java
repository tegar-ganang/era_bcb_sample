package net.sf.jzeno.servletfilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;
import net.sf.jzeno.exception.InternalServerException;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * ServletFilter that transparently performs delta compression on the response.
 */
public class TokenCompressionFilter implements Filter {

    protected static Logger log = Logger.getLogger(TokenCompressionFilter.class);

    private static final String PREVIOUSRESPONSE = "system.compressionfilter.previousresponse";

    private static final String CACHE_KEY = "system.compressionfilter.cache";

    private static final String CACHE_TIMESTAMPS_KEY = "system.compressionfilter.cache.timestamps";

    private static final String CACHE_SIZE_KEY = "system.compressionfilter.cache.size";

    private static final int CACHE_MAX_SIZE = 10000;

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (res instanceof HttpServletResponse && req instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;
            String agent = request.getHeader("User-Agent");
            if (agent.indexOf("Firefox") != -1 || agent.indexOf("Gecko") != -1) {
                TokenCompressionResponseWrapper wrappedResponse = new TokenCompressionResponseWrapper(response, true, request.getSession());
                chain.doFilter(req, wrappedResponse);
                wrappedResponse.finishResponse();
            } else {
                chain.doFilter(req, res);
            }
        }
    }

    public void destroy() {
    }

    public void init(FilterConfig arg0) throws ServletException {
    }

    public static class SplitStream extends OutputStream {

        protected static Logger log = Logger.getLogger(SplitStream.class);

        private OutputStream output1;

        private OutputStream output2;

        public SplitStream(OutputStream output1, OutputStream output2) {
            this.output1 = output1;
            this.output2 = output2;
        }

        /**
         * @see java.io.OutputStream#close()
         */
        public void close() throws IOException {
            output1.close();
            output2.close();
        }

        /**
         * @see java.io.OutputStream#flush()
         */
        public void flush() throws IOException {
            output1.flush();
            output2.flush();
        }

        /**
         * @see java.io.OutputStream#write(byte[], int, int)
         */
        public void write(byte[] b, int off, int len) throws IOException {
            output1.write(b, off, len);
            output2.write(b, off, len);
        }

        /**
         * @see java.io.OutputStream#write(byte[])
         */
        public void write(byte[] b) throws IOException {
            output1.write(b);
            output2.write(b);
        }

        /**
         * @see java.io.OutputStream#write(int)
         */
        public void write(int b) throws IOException {
            output1.write(b);
            output2.write(b);
        }
    }

    /**
     * Class that wraps a GZIPOutputStream to allow the output stream to be
     * GZIpped.
     */
    public static class TokenCompressionResponseStream extends ServletOutputStream {

        private SplitStream splitStream;

        private ByteArrayOutputStream byteArrayOutputStream;

        private OutputStream toBrowserStream;

        private TokenCompressionStream TokenCompressionStream;

        private boolean closed = false;

        private HttpSession session;

        public TokenCompressionResponseStream(HttpServletResponse response, boolean compress, HttpSession session) throws IOException {
            this.session = session;
            toBrowserStream = response.getOutputStream();
            if (compress) {
                byteArrayOutputStream = new ByteArrayOutputStream();
                TokenCompressionStream = new TokenCompressionStream(toBrowserStream, session);
                splitStream = new SplitStream(byteArrayOutputStream, TokenCompressionStream);
            } else {
                byteArrayOutputStream = new ByteArrayOutputStream();
                splitStream = new SplitStream(byteArrayOutputStream, toBrowserStream);
            }
        }

        public void close() throws IOException {
            if (closed) {
                throw new IOException("This output stream has already been closed");
            } else {
                splitStream.flush();
                splitStream.close();
                closed = true;
                if (session != null) {
                    session.setAttribute(PREVIOUSRESPONSE, byteArrayOutputStream.toByteArray());
                }
            }
        }

        public void flush() throws IOException {
            if (closed) {
                throw new IOException("Cannot flush a closed output stream");
            } else {
                splitStream.flush();
                return;
            }
        }

        public void write(int i) throws IOException {
            if (closed) {
                throw new IOException("Cannot write to a closed output stream");
            } else {
                splitStream.write((byte) i);
                return;
            }
        }

        public void write(byte abyte0[]) throws IOException {
            write(abyte0, 0, abyte0.length);
        }

        public void write(byte abyte0[], int i, int j) throws IOException {
            if (closed) {
                throw new IOException("Cannot write to a closed output stream");
            } else {
                splitStream.write(abyte0, i, j);
                return;
            }
        }

        public boolean closed() {
            return closed;
        }

        public void reset() {
        }
    }

    /**
     * Class that wraps a HttpServletResponse so that it can be GZIPped.
     */
    public static class TokenCompressionResponseWrapper extends HttpServletResponseWrapper {

        protected HttpServletResponse response = null;

        protected ServletOutputStream stream = null;

        protected PrintWriter writer = null;

        private HttpSession session = null;

        private boolean compress = false;

        public TokenCompressionResponseWrapper(HttpServletResponse response, boolean compress, HttpSession session) {
            super(response);
            this.response = response;
            this.session = session;
            this.compress = compress;
        }

        public ServletOutputStream createOutputStream() throws IOException {
            return new TokenCompressionResponseStream(response, compress, session);
        }

        public void finishResponse() {
            try {
                if (writer != null) writer.close(); else if (stream != null) stream.close();
            } catch (IOException ioexception) {
            }
        }

        public void flushBuffer() throws IOException {
            stream.flush();
        }

        public ServletOutputStream getOutputStream() throws IOException {
            if (writer != null) throw new IllegalStateException("getWriter() has already been called!");
            if (stream == null) stream = createOutputStream();
            return stream;
        }

        public PrintWriter getWriter() throws IOException {
            if (writer != null) return writer;
            if (stream != null) {
                throw new IllegalStateException("getOutputStream() has already been called!");
            } else {
                stream = createOutputStream();
                writer = new PrintWriter(new OutputStreamWriter(stream, "UTF-8"));
                return writer;
            }
        }

        public void setContentLength(int i) {
        }
    }

    public static class TokenCompressionStream extends OutputStream {

        protected static Logger log = Logger.getLogger(TokenCompressionStream.class);

        private HttpSession session = null;

        private OutputStream backingStream;

        private StringBuffer databuffer = new StringBuffer();

        public TokenCompressionStream(OutputStream backingStream, HttpSession session) {
            this.backingStream = backingStream;
            this.session = session;
            try {
                writeToBackingStreamNoReplace("<script language=\"JavaScript\">\n");
                writeToBackingStream("var r=parent.scriptframe.cache_concat(");
            } catch (IOException e) {
                throw new InternalServerException("Unable to write start of script to backing stream.");
            }
        }

        /**
         * The cache maps a string onto an index.
         * 
         * @return
         */
        private Map getCache() {
            Map ret = null;
            ret = (Map) session.getAttribute(CACHE_KEY);
            if (ret == null) {
                ret = new HashMap();
                session.setAttribute(CACHE_KEY, ret);
                session.setAttribute(CACHE_SIZE_KEY, new Integer(0));
            }
            return ret;
        }

        /**
         * For the index of a cached string, a last used timestamp is kept
         * 
         * @return
         */
        private Date[] getCacheTimestamps() {
            Date[] ret = null;
            ret = (Date[]) session.getAttribute(CACHE_TIMESTAMPS_KEY);
            if (ret == null) {
                ret = new Date[CACHE_MAX_SIZE];
                session.setAttribute(CACHE_TIMESTAMPS_KEY, ret);
            }
            return ret;
        }

        /**
         * Determines the last index used in the cache.
         * 
         * @return
         */
        private int getCacheSize() {
            int ret = 0;
            Integer i = (Integer) session.getAttribute(CACHE_SIZE_KEY);
            if (i == null) {
                session.setAttribute(CACHE_SIZE_KEY, new Integer(0));
            } else {
                ret = i.intValue();
            }
            return ret;
        }

        /**
         * Determines the index of this string in the cache. If the string does
         * not occur in the cache, -1 is returned. If the string is found it's
         * timestamp is updated.
         * 
         * @param line
         * @return
         */
        private int findString(String line) {
            int ret = -1;
            Map cache = getCache();
            Date[] timestamps = getCacheTimestamps();
            if (cache.containsKey(line)) {
                Integer index = (Integer) cache.get(line);
                ret = index.intValue();
                timestamps[ret] = new Date();
            }
            return ret;
        }

        /**
         * Adds the given string to the cache. It assumes that the string is not
         * allready in the cache. It returns the index used to store the new
         * key. When the string is added to the cache, or replaces an existing
         * index, then the timstamp is updated.
         * 
         * @param line
         */
        private int addStringToCache(String line) {
            int ret = -1;
            Map cache = getCache();
            Date[] timestamps = getCacheTimestamps();
            int cacheSize = getCacheSize();
            if (cache.containsKey(line)) {
                throw new InternalServerException("The cache allready contains <" + line + "> !");
            }
            if (cacheSize < CACHE_MAX_SIZE) {
                cache.put(line, new Integer(cacheSize));
                timestamps[cacheSize] = new Date();
                ret = cacheSize;
                cacheSize++;
                session.setAttribute(CACHE_SIZE_KEY, new Integer(cacheSize));
            } else {
                Date oldest = null;
                int oldestIndex = -1;
                for (int i = 0; i < timestamps.length; i++) {
                    Date current = timestamps[i];
                    if (oldest == null || current.before(oldest)) {
                        oldest = current;
                        oldestIndex = i;
                    }
                }
                cache.put(new Integer(oldestIndex), line);
                timestamps[cacheSize] = new Date();
                ret = oldestIndex;
            }
            return ret;
        }

        /**
         * Scan the databuffer for complete lines.
         *  
         */
        private void processData() throws IOException {
            int index = databuffer.indexOf("\n");
            String line = null;
            while (index != -1) {
                line = databuffer.substring(0, index + 1);
                int bufferIndex = findString(line);
                if (bufferIndex == -1) {
                    bufferIndex = addStringToCache(line);
                    writeToBackingStream("" + (-bufferIndex) + ",\"" + StringEscapeUtils.escapeJavaScript(line) + "\",");
                } else {
                    writeToBackingStream("" + bufferIndex + ",");
                }
                databuffer.replace(0, index + 1, "");
                index = databuffer.indexOf("\n");
            }
        }

        /**
         * Writes text to the backing stream, and splits script element to avoid
         * interpretation by the browser.
         * 
         * @param data
         * @throws IOException
         */
        private void writeToBackingStream(String data) throws IOException {
            data = StringUtils.replace(data, "</script", "</scr\" + \"ipt");
            data = StringUtils.replace(data, "<script", "<scr\" + \"ipt ");
            backingStream.write(data.getBytes());
        }

        private void writeToBackingStreamNoReplace(String data) throws IOException {
            backingStream.write(data.getBytes());
        }

        public void close() throws IOException {
            write("\n".getBytes());
            writeToBackingStreamNoReplace("10000);\n");
            writeToBackingStreamNoReplace("parent.scriptframe.replaceHTML(r);\n");
            writeToBackingStreamNoReplace("</script>\n");
            backingStream.close();
        }

        public void flush() throws IOException {
            backingStream.flush();
        }

        public void write(byte[] b, int off, int len) throws IOException {
            String data = new String(b, off, len);
            databuffer.append(data);
            processData();
        }

        public void write(byte[] b) throws IOException {
            String data = new String(b);
            databuffer.append(data);
            processData();
        }

        public void write(int b) throws IOException {
            databuffer.append((char) b);
            processData();
        }
    }
}
