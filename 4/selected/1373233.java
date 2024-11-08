package net.sourceforge.fo3d.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.IOUtils;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.datatypes.URISpecification;

public class IOUtil {

    /**
     * Private constructor since this is an utility class.
     */
    private IOUtil() {
    }

    /**
     * Returns an InputStream object for the given URI
     * 
     * @param ua FOP UserAgent
     * @param uri String referencing a valid file
     * @return InputStream object
     * @throws IOException
     */
    protected static InputStream getURLInputStream(FOUserAgent ua, String uri) throws IOException {
        uri = URISpecification.getURL(uri);
        Source src = ua.resolveURI(uri);
        if (src == null) {
            throw new IOException("File not found: " + uri);
        }
        InputStream in = null;
        if (src instanceof StreamSource) {
            in = ((StreamSource) src).getInputStream();
        } else {
            in = new URL(src.getSystemId()).openStream();
        }
        return in;
    }

    /**
     * Reads the content of a file given by <code>uri</code> and writes it
     * to the given output stream <code>out</code>.
     * 
     * @param ua UserAgent for resolving the given URI
     * @param uri URI referencing a file
     * @param output Output stream
     * @throws IOException
     */
    public static void readFile(FOUserAgent ua, String uri, OutputStream output) throws IOException {
        InputStream in = getURLInputStream(ua, uri);
        try {
            IOUtils.copy(in, output);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Reads the content of a file given by <code>uri</code> and writes it to
     * the <code>output</code>.
     * If encoding is null, the default encoding depending on the platform will
     * be used.
     * 
     * @param ua UserAgent for resolving the given URI
     * @param uri URI referencing a file
     * @param output Writer to the output
     * @param encoding Encoding
     * @throws IOException
     */
    public static void readFile(FOUserAgent ua, String uri, Writer output, String encoding) throws IOException {
        InputStream in = getURLInputStream(ua, uri);
        try {
            StringWriter writer = new StringWriter();
            IOUtils.copy(in, writer, encoding);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Reads the content of a file given by <code>uri</code> and writes it to
     * the <code>output</code>. (Assuming the default encoding depending
     * on the platform)
     * 
     * @param ua UserAgent for resolving the given URI
     * @param uri URI referencing a file
     * @param output Writer to the output
     * @throws IOException
     */
    public static void readFile(FOUserAgent ua, String uri, Writer output) throws IOException {
        readFile(ua, uri, output, null);
    }
}
