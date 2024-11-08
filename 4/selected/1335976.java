package org.lindenb.lib.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * @author lindenb
 *
 */
public class HttpResponse extends HttpBase {

    private HttpRequest request = null;

    private InputStream from_server;

    HttpResponse(HttpRequest request) {
        super();
        this.request = request;
        this.from_server = null;
    }

    public HttpRequest getHttpRequest() {
        return this.request;
    }

    /**
	 * getInputStream
	 * @return
	 */
    public InputStream getInputStream() {
        return this.from_server;
    }

    /**
	 * getInputStream
	 * @return
	 */
    public void setInputStream(InputStream in) {
        this.from_server = in;
    }

    public void write(Writer out) throws IOException {
        InputStream in = getInputStream();
        byte buffer[] = new byte[4096];
        int n_read;
        while ((n_read = in.read(buffer)) != -1) {
            out.write(new String(buffer, 0, n_read));
        }
    }

    public void write(OutputStream out) throws IOException {
        InputStream in = getInputStream();
        byte buffer[] = new byte[4096];
        int n_read;
        while ((n_read = in.read(buffer)) != -1) {
            out.write(buffer, 0, n_read);
        }
    }

    public void print(PrintWriter out) {
        try {
            write(out);
        } catch (IOException error) {
            error.printStackTrace();
        }
    }
}
