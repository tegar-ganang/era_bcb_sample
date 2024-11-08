package sun.awt.image;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;

public class URLImageSource extends InputStreamImageSource {

    URL url;

    URLConnection conn;

    String actualHost;

    int actualPort;

    public URLImageSource(URL u) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                java.security.Permission perm = u.openConnection().getPermission();
                if (perm != null) {
                    try {
                        sm.checkPermission(perm);
                    } catch (SecurityException se) {
                        if ((perm instanceof java.io.FilePermission) && perm.getActions().indexOf("read") != -1) {
                            sm.checkRead(perm.getName());
                        } else if ((perm instanceof java.net.SocketPermission) && perm.getActions().indexOf("connect") != -1) {
                            sm.checkConnect(u.getHost(), u.getPort());
                        } else {
                            throw se;
                        }
                    }
                }
            } catch (java.io.IOException ioe) {
                sm.checkConnect(u.getHost(), u.getPort());
            }
        }
        this.url = u;
    }

    public URLImageSource(String href) throws MalformedURLException {
        this(new URL(null, href));
    }

    public URLImageSource(URL u, URLConnection uc) {
        this(u);
        conn = uc;
    }

    public URLImageSource(URLConnection uc) {
        this(uc.getURL(), uc);
    }

    final boolean checkSecurity(Object context, boolean quiet) {
        if (actualHost != null) {
            try {
                SecurityManager security = System.getSecurityManager();
                if (security != null) {
                    if (context != null) security.checkConnect(actualHost, actualPort, context); else security.checkConnect(actualHost, actualPort);
                }
            } catch (SecurityException e) {
                if (!quiet) {
                    throw e;
                }
                return false;
            }
        }
        return true;
    }

    private synchronized URLConnection getConnection() throws IOException {
        URLConnection c;
        if (conn != null) {
            c = conn;
            conn = null;
        } else {
            c = url.openConnection();
        }
        return c;
    }

    protected ImageDecoder getDecoder() {
        InputStream is = null;
        String type = null;
        try {
            URLConnection c = getConnection();
            is = c.getInputStream();
            type = c.getContentType();
            URL u = c.getURL();
            if (u != url && (!u.getHost().equals(url.getHost()) || u.getPort() != url.getPort())) {
                if (actualHost != null && (!actualHost.equals(u.getHost()) || actualPort != u.getPort())) {
                    throw new SecurityException("image moved!");
                }
                actualHost = u.getHost();
                actualPort = u.getPort();
            }
        } catch (IOException e) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e2) {
                }
            }
            return null;
        }
        ImageDecoder id = decoderForType(is, type);
        if (id == null) {
            id = getDecoder(is);
        }
        return id;
    }
}
