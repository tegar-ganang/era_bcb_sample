package gnu.java.net.protocol.gcjlib;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {

    protected URLConnection openConnection(URL url) throws IOException {
        return new Connection(url);
    }
}
