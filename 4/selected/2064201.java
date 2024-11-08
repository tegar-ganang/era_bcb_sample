package se.issi.magnolia.module.blossom.support;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Wraps an HttpServletResponse and directs outputs from both the writer and the stream to a target Writer.
 */
public class WriterResponseWrapper extends HttpServletResponseWrapper {

    private Writer targetWriter;

    private PrintWriter exposedWriter;

    private boolean writerWasUsed = false;

    private boolean streamWasUsed = false;

    private ServletOutputStream sos = new ServletOutputStream() {

        public void write(int b) throws IOException {
            targetWriter.write(b);
        }
    };

    private int status = 200;

    public WriterResponseWrapper(HttpServletResponse response, Writer targetWriter) {
        super(response);
        this.targetWriter = targetWriter;
        this.exposedWriter = new PrintWriter(targetWriter);
    }

    public PrintWriter getWriter() {
        if (streamWasUsed) throw new IllegalStateException("getOutputStream() already called");
        writerWasUsed = true;
        return exposedWriter;
    }

    public ServletOutputStream getOutputStream() {
        if (writerWasUsed) throw new IllegalStateException("getWriter() already called");
        streamWasUsed = true;
        return sos;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        this.status = sc;
    }

    @Override
    public void sendError(int sc) throws IOException {
        this.status = sc;
    }
}
