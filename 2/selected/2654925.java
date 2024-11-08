package org.pustefixframework.resource.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import org.apache.log4j.Logger;
import org.pustefixframework.resource.InputStreamResource;
import org.pustefixframework.resource.URLResource;

/**
 * Resource implementation using a URL to access the resource's content.  
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class URLResourceImpl extends AbstractResource implements InputStreamResource, URLResource {

    private Logger LOG = Logger.getLogger(URLResourceImpl.class);

    private URI uri;

    private URI originalURI;

    private URL url;

    /**
     * Creates a new URL based resource.
     * 
     * @param uri is returned by {@link #getURI()}
     * @param originallyRequestedURI is returned by {@link #getOriginalURI()}
     * @param url is used to actually access the resource
     */
    public URLResourceImpl(URI uri, URI originallyRequestedURI, URL url) {
        this.originalURI = uri;
        if (originallyRequestedURI != null) {
            this.uri = originallyRequestedURI;
        } else {
            this.uri = uri;
        }
        this.url = url;
    }

    public InputStream getInputStream() throws IOException {
        return url.openStream();
    }

    public URI getOriginalURI() {
        return originalURI;
    }

    public URI[] getSupplementaryURIs() {
        return null;
    }

    public URI getURI() {
        return uri;
    }

    public URL getURL() {
        return url;
    }

    public long lastModified() {
        URLConnection con = null;
        try {
            con = url.openConnection();
            return con.getLastModified();
        } catch (IOException e) {
            return 0;
        } finally {
            if (url.getProtocol().equals("file") && con != null) {
                try {
                    con.getInputStream().close();
                } catch (IOException e) {
                    LOG.warn("Error closing URLConnection stream after modcheck.", e);
                }
            }
        }
    }
}
