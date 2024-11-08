package net.fortytwo.ripple.util;

import net.fortytwo.ripple.RippleException;
import net.fortytwo.flow.rdf.SesameInputAdapter;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.openrdf.rio.RDFFormat;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;

public final class RDFHTTPUtils {

    public static RDFFormat read(final HttpMethod method, final SesameInputAdapter sa, final String baseUri, RDFFormat format) throws RippleException {
        HTTPUtils.registerMethod(method);
        InputStream body;
        HttpClient client = HTTPUtils.createClient();
        try {
            client.executeMethod(method);
            body = method.getResponseBodyAsStream();
        } catch (Throwable t) {
            throw new RippleException(t);
        }
        if (null == format) {
            format = RDFUtils.guessRdfFormat(method.getPath(), method.getResponseHeader(HTTPUtils.CONTENT_TYPE).getValue());
            if (null == format) {
                return null;
            }
        }
        RDFUtils.read(body, sa, baseUri, format);
        try {
            body.close();
        } catch (IOException e) {
            throw new RippleException(e);
        }
        method.releaseConnection();
        return format;
    }

    public static RDFFormat read(final URL url, final SesameInputAdapter sa, final String baseUri, RDFFormat format) throws RippleException {
        String urlStr = url.toString();
        if (urlStr.startsWith("jar:")) {
            if (null == format) {
                format = RDFUtils.guessRdfFormat(urlStr, null);
            }
            JarURLConnection jc;
            InputStream is;
            try {
                jc = (JarURLConnection) url.openConnection();
                is = jc.getInputStream();
            } catch (IOException e) {
                throw new RippleException(e);
            }
            RDFUtils.read(is, sa, baseUri, format);
            try {
                is.close();
            } catch (IOException e) {
                throw new RippleException(e);
            }
            return format;
        } else if (urlStr.startsWith("file:")) {
            if (null == format) {
                format = RDFUtils.guessRdfFormat(urlStr, null);
            }
            InputStream is;
            try {
                is = new FileInputStream(urlStr.substring(5));
            } catch (IOException e) {
                throw new RippleException(e);
            }
            RDFUtils.read(is, sa, baseUri, format);
            try {
                is.close();
            } catch (IOException e) {
                throw new RippleException(e);
            }
            return format;
        } else {
            HttpMethod method = HTTPUtils.createRdfGetMethod(url.toString());
            return read(method, sa, baseUri, format);
        }
    }

    public static RDFFormat read(final URL url, final SesameInputAdapter sa, final String baseUri) throws RippleException {
        return read(url, sa, baseUri, null);
    }
}
