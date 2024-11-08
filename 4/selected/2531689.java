package flex.messaging.log;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Theis class wraps the servlet request so we can capture the body bytes
 * and log them if an unexpected error occurs in the request. 
 */
public class LoggingHttpServletRequestWrapper extends HttpServletRequestWrapper {

    /**
     * Body data
     */
    private byte[] _bodyInfoData;

    /**
     * Constructor
     */
    public LoggingHttpServletRequestWrapper(HttpServletRequest parent) {
        super(parent);
    }

    /**
     * Read the body and store it
     *
     * @throws IOException if there is a problem reading the request data
     */
    public void doReadBody() throws IOException {
        int length = super.getContentLength();
        if (length > 0) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(length);
            InputStream in = super.getInputStream();
            byte[] bodyByte = new byte[length];
            int remain = length;
            while (remain > 0) {
                int readLen = in.read(bodyByte);
                if (readLen <= 0) {
                    break;
                }
                out.write(bodyByte, 0, readLen);
                remain -= readLen;
            }
            this._bodyInfoData = out.toByteArray();
            out.close();
        }
    }

    /**
     * Returns an input stream backed by the saved body data.
     */
    public ServletInputStream getInputStream() throws IOException {
        if (this._bodyInfoData != null) {
            return new ExServletInputStream(new ByteArrayInputStream(this._bodyInfoData));
        }
        return super.getInputStream();
    }

    /**
     * An extension of the <tt>ServletInputStream</tt> that is backed by an input stream
     * provided at construction.
     * <p>
     * Used to allow the servlet request wrapper to return a stream backed by the already consumed body data.</p>
     */
    private static class ExServletInputStream extends ServletInputStream {

        /**
         * InputStream Object
         */
        private InputStream _inputStream;

        /**
         * Constructor
         */
        ExServletInputStream(InputStream inputStream) {
            this._inputStream = inputStream;
        }

        public int readLine(byte[] b, int off, int len) throws IOException {
            throw new UnsupportedOperationException("This method is not extended");
        }

        public int read(byte[] b, int off, int len) throws IOException {
            return this._inputStream.read(b, off, len);
        }

        public int read(byte[] b) throws IOException {
            return this._inputStream.read(b);
        }

        public void mark(int readlimit) {
            this._inputStream.mark(readlimit);
        }

        public long skip(long n) throws IOException {
            return this._inputStream.skip(n);
        }

        public void reset() throws IOException {
            this._inputStream.reset();
        }

        public int read() throws IOException {
            return this._inputStream.read();
        }

        public boolean markSupported() {
            return _inputStream.markSupported();
        }

        public void close() throws IOException {
            this._inputStream.close();
        }

        public int available() throws IOException {
            return this._inputStream.available();
        }
    }
}
