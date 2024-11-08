package syntelos.net.file;

import java.net.URL;
import java.net.URLConnection;

/**
 * 
 * @author jdp
 * @since 1.6
 */
public final class Handler extends java.net.URLStreamHandler {

    public Handler() {
        super();
    }

    protected URLConnection openConnection(URL url) throws java.io.IOException {
        return new Connection(url);
    }

    protected int getDefaultPort() {
        return 0;
    }
}
