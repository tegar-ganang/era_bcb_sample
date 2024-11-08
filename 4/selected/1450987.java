package net.sf.miniportal;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;
import javax.portlet.PortletURL;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class RenderResponseImpl extends PortletResponseImpl implements RenderResponse {

    private final OutputStream out;

    private final HttpServletRequest request;

    private final HttpServletResponse response;

    private final PortletConfigImpl portletConfig;

    private static final int initialBufferSize = 1024;

    private boolean committed = false;

    private int bufferSize = initialBufferSize;

    private String contentType;

    private PrintWriter writer;

    private BufferedOutputStream buffer;

    RenderResponseImpl(HttpServletRequest request, HttpServletResponse response, OutputStream out, PortletConfigImpl config) {
        super(response);
        this.request = request;
        this.response = response;
        this.portletConfig = config;
        this.out = out;
        this.writer = null;
    }

    public String getContentType() {
        return contentType;
    }

    public PortletURL createRenderURL() {
        return createURL(false);
    }

    public PortletURL createActionURL() {
        return createURL(true);
    }

    private PortletURL createURL(boolean actionURL) {
        log.trace(getClass() + ".createURL() called");
        String prefix;
        if (request.getPathInfo() != null) {
            int index = request.getRequestURL().lastIndexOf(request.getPathInfo());
            prefix = request.getRequestURL().substring(0, index);
        } else {
            prefix = request.getRequestURL().toString();
        }
        PortletURLImpl result = new PortletURLImpl(prefix, portletConfig, request.isSecure(), actionURL);
        try {
            result.setWindowState(PortletContainer.getWindowState(request, portletConfig.getPortletName()));
            result.setPortletMode(PortletContainer.getPortletMode(request, portletConfig.getPortletName()));
            result.setSecure(request.isSecure());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public String getNamespace() {
        return portletConfig.getNamespace();
    }

    public void setTitle(String arg0) {
    }

    public void setContentType(String arg0) {
        this.contentType = arg0;
    }

    public String getCharacterEncoding() {
        return response.getCharacterEncoding();
    }

    public Locale getLocale() {
        return response.getLocale();
    }

    public void setBufferSize(int arg0) {
        if (buffer != null) throw new IllegalStateException();
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void flushBuffer() throws IOException {
        if (log.isTraceEnabled()) log.trace("Flushing buffer.");
        if (writer != null) writer.flush(); else buffer.flush();
        if (log.isTraceEnabled()) log.trace("Response committed: " + isCommitted());
    }

    public void resetBuffer() {
        if (isCommitted()) throw new IllegalStateException();
        if (buffer == null) return;
        buffer = new BufferedOutputStream(out);
        if (writer != null) {
            writer = new PrintWriter(new OutputStreamWriter(buffer));
        }
    }

    public boolean isCommitted() {
        return committed;
    }

    public void reset() {
        resetBuffer();
        this.contentType = null;
        this.bufferSize = 1024;
    }

    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            if (buffer != null) {
                throw new IllegalStateException("Portlet output stream has already been requested.");
            } else {
                writer = new PrintWriter(new OutputStreamWriter(getPortletOutputStream()));
            }
        }
        return writer;
    }

    public OutputStream getPortletOutputStream() throws IOException {
        if (writer != null) throw new IllegalStateException("Portlet writer has already been requested.");
        if (buffer == null) {
            buffer = new CommitableBufferedOutputStream(out, bufferSize);
        }
        return buffer;
    }

    private class CommitableBufferedOutputStream extends BufferedOutputStream {

        public CommitableBufferedOutputStream(OutputStream out) {
            super(out);
            this.out = out;
        }

        public CommitableBufferedOutputStream(OutputStream out, int size) {
            super(out, size);
            this.out = out;
        }

        public synchronized void flush() throws IOException {
            super.flush();
            committed = true;
        }
    }
}
