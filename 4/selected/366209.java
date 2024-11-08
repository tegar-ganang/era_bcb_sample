package com.volantis.mcs.servlet;

import com.volantis.mcs.localization.LocalizationFactory;
import com.volantis.mcs.utilities.Convertors;
import com.volantis.synergetics.log.LogDispatcher;
import com.volantis.shared.content.ContentStyle;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * A response wrapper which caches the content written to it.
 */
public class CachingResponseWrapper extends HttpServletResponseWrapper implements CachedContent {

    /**
     * The logger used by this class.
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(CachingResponseWrapper.class);

    private ServletOutputStreamAdapter outerBinaryStream;

    private ByteArrayOutputStream innerBinaryStream;

    private PrintWriter outerTextWriter;

    private CharArrayWriter innerTextWriter;

    /**
     * The content type of this response.
     */
    private String contentType;

    /**
     * The HTTP status code of the request.
     */
    private int status;

    /**
     * Initialises an instance of this class with the given response.
     *
     * @param response the response to wrap
     */
    public CachingResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    /**
     * Gets the ServletOutputStream used by this response wrapper. Note that
     * this wrapper is either using a ServletOutputStream or a Writer but not
     * both.
     *
     * @return the ServletOutputStream
     * @throws IOException           if there is a problem obtaining the
     *                               stream
     * @throws IllegalStateException if this response wrapper has already used
     *                               a PrintWriter (@see #getWriter()).
     */
    public ServletOutputStream getOutputStream() throws IOException, IllegalStateException {
        if (innerTextWriter != null) {
            throw new IllegalStateException("A PrintWriter is already " + "being used to write the body.");
        }
        if (innerBinaryStream == null) {
            innerBinaryStream = new ByteArrayOutputStream();
            outerBinaryStream = new ServletOutputStreamAdapter(innerBinaryStream);
        }
        return outerBinaryStream;
    }

    /**
     * Gets the PrintWriter used by this response wrapper. Note that this
     * wrapper is either using a ServletOutputStream or a Writer but not both.
     *
     * @return the PrintWriter
     * @throws IOException           if there is a problem obtaining the
     *                               writer
     * @throws IllegalStateException if this response wrapper has already used
     *                               a PrintWriter (@see #getWriter()).
     */
    public PrintWriter getWriter() throws IOException, IllegalStateException {
        if (hasBinaryContent()) {
            throw new IllegalStateException("A ServletOutputStream is already being used to " + "write the body.");
        }
        if (!hasTextContent()) {
            innerTextWriter = new CharArrayWriter();
            outerTextWriter = new PrintWriter(innerTextWriter);
        }
        return outerTextWriter;
    }

    public void flushBuffer() throws IOException {
        if (hasBinaryContent()) {
            flushBinary();
        }
        if (hasTextContent()) {
            flushText();
        }
    }

    /**
     * Return the content style of any cached content, or null if there is
     * none.
     */
    public ContentStyle getContentStyle() {
        if (hasBinaryContent()) {
            return ContentStyle.BINARY;
        }
        if (hasTextContent()) {
            return ContentStyle.TEXT;
        }
        return null;
    }

    public byte[] getAsByteArray() throws IOException {
        if (!hasBinaryContent()) {
            throw new IllegalStateException("No binary content available");
        }
        flushBinary();
        final byte[] buf = innerBinaryStream.toByteArray();
        return buf;
    }

    public char[] getAsCharArray() throws IOException {
        if (!hasTextContent()) {
            throw new IllegalStateException("No text content available");
        }
        flushText();
        final char[] buf = innerTextWriter.toCharArray();
        return buf;
    }

    /**
     * Copies the content of this response wrapper to the given servlet
     * response.  If the response does not contain any content then this
     * method does nothing.
     *
     * @param response the response to be populated from this response wrapper
     * @throws IOException if there is a problem populating the response
     */
    public void writeTo(ServletResponse response) throws IOException {
        if (hasTextContent()) {
            flushText();
            if (innerTextWriter.size() > 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Writing response wrapper content as " + innerTextWriter.size() + " characters using " + "content type " + contentType);
                }
                response.setContentType(contentType);
                innerTextWriter.writeTo(response.getWriter());
            }
        } else if (hasBinaryContent()) {
            flushBinary();
            if (innerBinaryStream.size() > 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Writing response wrapper content as " + innerBinaryStream.size() + " bytes.");
                }
                response.setContentType(contentType);
                response.setContentLength(innerBinaryStream.size());
                innerBinaryStream.writeTo(response.getOutputStream());
            }
        }
    }

    private void flushBinary() throws IOException {
        outerBinaryStream.flush();
    }

    private void flushText() throws IOException {
        if (outerTextWriter.checkError()) {
            throw new IOException("Error flushing print writer");
        }
    }

    private boolean hasTextContent() {
        return outerTextWriter != null;
    }

    private boolean hasBinaryContent() {
        return outerBinaryStream != null;
    }

    public void setContentLength(int len) {
    }

    public void setContentType(String type) {
        super.setContentType(type);
        contentType = type;
    }

    public String getContentType() {
        return contentType;
    }

    /**
     * Gets the mime type from the content type of this response.
     *
     * @return the mime type
     */
    public String getMimeTypeFromContentType() {
        return Convertors.contentTypeToMimeType(contentType);
    }

    public void setStatus(int status, String msg) {
        super.setStatus(status, msg);
        this.status = status;
    }

    public void setStatus(int status) {
        super.setStatus(status);
        this.status = status;
    }

    public void sendRedirect(String location) throws IOException {
        super.sendRedirect(location);
        status = HttpServletResponse.SC_MOVED_TEMPORARILY;
    }

    /**
     * Gets the status code of the wrapper's response.
     *
     * @return the status code
     */
    public int getStatus() {
        return status;
    }
}
