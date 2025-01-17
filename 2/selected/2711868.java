package net.sourceforge.filebot.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import net.sourceforge.tuned.ByteBufferOutputStream;

public final class WebRequest {

    public static Document getHtmlDocument(URL url) throws IOException, SAXException {
        return getHtmlDocument(url.openConnection());
    }

    public static Document getHtmlDocument(URLConnection connection) throws IOException, SAXException {
        return getHtmlDocument(getReader(connection));
    }

    public static Reader getReader(URLConnection connection) throws IOException {
        try {
            connection.addRequestProperty("Accept-Encoding", "gzip,deflate");
            connection.addRequestProperty("Accept-Charset", "UTF-8,ISO-8859-1");
        } catch (IllegalStateException e) {
        }
        Charset charset = getCharset(connection.getContentType());
        String encoding = connection.getContentEncoding();
        InputStream inputStream = connection.getInputStream();
        if ("gzip".equalsIgnoreCase(encoding)) inputStream = new GZIPInputStream(inputStream); else if ("deflate".equalsIgnoreCase(encoding)) {
            inputStream = new InflaterInputStream(inputStream, new Inflater(true));
        }
        return new InputStreamReader(inputStream, charset);
    }

    public static Document getHtmlDocument(Reader reader) throws SAXException, IOException {
        DOMParser parser = new DOMParser();
        parser.setFeature("http://xml.org/sax/features/namespaces", false);
        parser.parse(new InputSource(reader));
        return parser.getDocument();
    }

    public static Document getHtmlDocument(String html) throws SAXException, IOException {
        return getHtmlDocument(new StringReader(html));
    }

    public static Document getDocument(URL url) throws IOException, SAXException {
        return getDocument(url.openConnection());
    }

    public static Document getDocument(URLConnection connection) throws IOException, SAXException {
        return getDocument(new InputSource(getReader(connection)));
    }

    public static Document getDocument(String xml) throws IOException, SAXException {
        return getDocument(new InputSource(new StringReader(xml)));
    }

    public static Document getDocument(InputSource source) throws IOException, SAXException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setFeature("http://xml.org/sax/features/namespaces", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            return factory.newDocumentBuilder().parse(source);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static ByteBuffer fetch(URL resource) throws IOException {
        return fetch(resource, 0, null);
    }

    public static ByteBuffer fetchIfModified(URL resource, long ifModifiedSince) throws IOException {
        return fetch(resource, ifModifiedSince, null);
    }

    public static ByteBuffer fetch(URL url, long ifModifiedSince, Map<String, String> requestParameters) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setIfModifiedSince(ifModifiedSince);
        if (requestParameters != null) {
            for (Entry<String, String> parameter : requestParameters.entrySet()) {
                connection.addRequestProperty(parameter.getKey(), parameter.getValue());
            }
        }
        int contentLength = connection.getContentLength();
        InputStream in = connection.getInputStream();
        ByteBufferOutputStream buffer = new ByteBufferOutputStream(contentLength >= 0 ? contentLength : 4 * 1024);
        try {
            buffer.transferFully(in);
        } catch (IOException e) {
            if (contentLength >= 0) {
                throw e;
            }
        } finally {
            in.close();
        }
        if (contentLength < 0 && buffer.getByteBuffer().remaining() == 0) return null;
        return buffer.getByteBuffer();
    }

    public static ByteBuffer post(HttpURLConnection connection, Map<String, ?> parameters) throws IOException {
        return post(connection, encodeParameters(parameters).getBytes("UTF-8"), "application/x-www-form-urlencoded");
    }

    public static ByteBuffer post(HttpURLConnection connection, byte[] postData, String contentType) throws IOException {
        connection.addRequestProperty("Content-Length", String.valueOf(postData.length));
        connection.addRequestProperty("Content-Type", contentType);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        OutputStream out = connection.getOutputStream();
        out.write(postData);
        out.close();
        int contentLength = connection.getContentLength();
        String encoding = connection.getContentEncoding();
        InputStream in = connection.getInputStream();
        if ("gzip".equalsIgnoreCase(encoding)) in = new GZIPInputStream(in); else if ("deflate".equalsIgnoreCase(encoding)) {
            in = new InflaterInputStream(in, new Inflater(true));
        }
        ByteBufferOutputStream buffer = new ByteBufferOutputStream(contentLength >= 0 ? contentLength : 32 * 1024);
        try {
            buffer.transferFully(in);
        } catch (IOException e) {
            if (contentLength >= 0) {
                throw e;
            }
        } finally {
            in.close();
        }
        return buffer.getByteBuffer();
    }

    private static Charset getCharset(String contentType) {
        if (contentType != null) {
            Matcher matcher = Pattern.compile("charset=(\\p{Graph}+)").matcher(contentType);
            if (matcher.find()) {
                try {
                    return Charset.forName(matcher.group(1));
                } catch (IllegalArgumentException e) {
                    Logger.getLogger(WebRequest.class.getName()).log(Level.WARNING, e.getMessage());
                }
            }
            if (contentType.equals("text/html")) {
                return Charset.forName("ISO-8859-1");
            }
        }
        return Charset.forName("UTF-8");
    }

    public static String encodeParameters(Map<String, ?> parameters) {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, ?> entry : parameters.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey());
            if (entry.getValue() != null) {
                sb.append("=");
                sb.append(encode(entry.getValue().toString()));
            }
        }
        return sb.toString();
    }

    public static String encode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static SSLSocketFactory createIgnoreCertificateSocketFactory() {
        TrustManager trustAnyCertificate = new X509TrustManager() {

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        };
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[] { trustAnyCertificate }, new SecureRandom());
            return sc.getSocketFactory();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * Dummy constructor to prevent instantiation.
	 */
    private WebRequest() {
        throw new UnsupportedOperationException();
    }
}
