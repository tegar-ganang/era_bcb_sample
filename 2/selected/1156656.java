package org.orbeon.oxf.resources.oxf;

import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.resources.ResourceManagerWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {

    public static final String PROTOCOL = "oxf";

    protected URLConnection openConnection(URL url) throws IOException {
        return new URLConnection(url) {

            private String key;

            private String getKey() {
                if (key == null) {
                    String urlString = getURL().toExternalForm();
                    if (!urlString.startsWith(PROTOCOL + ":")) throw new OXFException("Presentation Server URL must start with oxf:");
                    key = urlString.substring(PROTOCOL.length() + 1, urlString.length());
                }
                return key;
            }

            public void connect() {
            }

            public InputStream getInputStream() {
                return ResourceManagerWrapper.instance().getContentAsStream(getKey());
            }

            public OutputStream getOutputStream() {
                return ResourceManagerWrapper.instance().getOutputStream(getKey());
            }

            public long getLastModified() {
                return ResourceManagerWrapper.instance().lastModified(getKey());
            }

            public int getContentLength() {
                return ResourceManagerWrapper.instance().length(getKey());
            }
        };
    }

    protected void parseURL(URL u, String spec, int start, int limit) {
        super.parseURL(u, spec, start, limit);
    }

    protected void setURL(URL u, String protocol, String host, int port, String authority, String userInfo, String path, String query, String ref) {
        super.setURL(u, protocol, null, port, authority, userInfo, host + path, query, ref);
    }
}
