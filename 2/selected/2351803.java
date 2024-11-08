package jlibs.core.net;

import jlibs.core.io.FileUtil;
import jlibs.core.io.IOUtil;
import jlibs.core.lang.ImpossibleException;
import jlibs.core.lang.StringUtil;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * @author Santhosh Kumar T
 */
public class URLUtil {

    /**
     * Constructs <code>URI</code> from given string.
     *
     * The <code>URISyntaxException</code> is rethrown as <code>IllegalArgumentException</code>
     */
    public static URI toURI(String str) {
        return URI.create(str);
    }

    /**
     * Constructs <code>URI</code> from given <code>URL</code>.
     *
     * The <code>URISyntaxException</code> is rethrown as <code>IllegalArgumentException</code>
     */
    public static URI toURI(URL url) {
        return URI.create(url.toString());
    }

    public static URL toURL(String systemID) {
        if (StringUtil.isWhitespace(systemID)) return null;
        systemID = systemID.trim();
        try {
            return new URL(systemID);
        } catch (MalformedURLException ex) {
            return FileUtil.toURL(new File(systemID));
        }
    }

    public static String toSystemID(URL url) {
        try {
            if ("file".equals(url.getProtocol())) return new File(url.toURI()).getAbsolutePath(); else return url.toString();
        } catch (URISyntaxException ex) {
            throw new ImpossibleException(ex);
        }
    }

    public static URI resolve(String base, String child) {
        URI childURI = URI.create(child);
        if (childURI.isAbsolute()) return childURI;
        if (base == null) base = new File("temp.txt").getAbsolutePath();
        return toURI(toURL(base)).resolve(childURI);
    }

    public static URI relativize(String base, String child) {
        if (base == null) base = new File("temp.txt").getAbsolutePath();
        int slash = base.lastIndexOf('/');
        if (slash != -1) base = base.substring(0, slash + 1);
        return toURI(toURL(base)).relativize(URI.create(child));
    }

    /**
     * returns Query Parameters in specified uri as <code>Map</code>.
     * key will be param name and value wil be param value.
     *
     * @param uri       The string to be parsed into a URI
     * @param encoding  if null, <code>UTF-8</code> will be used
     *
     * @throws URISyntaxException               in case of invalid uri
     * @throws UnsupportedEncodingException     if named character encoding is not supported
     */
    public static Map<String, String> getQueryParams(String uri, String encoding) throws URISyntaxException, UnsupportedEncodingException {
        if (encoding == null) encoding = IOUtil.UTF_8.name();
        String query = new URI(uri).getRawQuery();
        Map<String, String> map = new HashMap<String, String>();
        StringTokenizer params = new StringTokenizer(query, "&;");
        while (params.hasMoreTokens()) {
            String param = params.nextToken();
            int equal = param.indexOf('=');
            String name = param.substring(0, equal);
            String value = param.substring(equal + 1);
            name = URLDecoder.decode(name, encoding);
            value = URLDecoder.decode(value, encoding);
            map.put(name, value);
        }
        return map;
    }

    public static String suggestFile(URI uri, String extension) {
        String path = uri.getPath();
        String tokens[] = StringUtil.getTokens(path, "/", true);
        String file = tokens[tokens.length - 1];
        int dot = file.indexOf(".");
        if (dot == -1) return file + '.' + extension; else return file.substring(0, dot + 1) + extension;
    }

    public static String suggestPrefix(Properties suggested, String uri) {
        return suggested.getProperty(uri, "ns");
    }

    private static SSLContext sc;

    /**
     * Creates connection to the specified url. If the protocol is <code>https</code> the connection
     * created doesn't validate any certificates.
     *
     * @param url   url to which connection has to be created
     * @param proxy proxy to be used. can be null
     * @return <code>URLConnection</code>. the connection is not yet connected
     *
     * @throws IOException if an I/O exception occurs
     */
    public static URLConnection createUnCertifiedConnection(URL url, Proxy proxy) throws IOException {
        if (sc == null) {
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, SSLUtil.DUMMY_TRUST_MANAGERS, new SecureRandom());
                URLUtil.sc = sc;
            } catch (Exception ex) {
                throw new ImpossibleException(ex);
            }
        }
        URLConnection con = proxy == null ? url.openConnection() : url.openConnection(proxy);
        if ("https".equals(url.getProtocol())) {
            HttpsURLConnection httpsCon = (HttpsURLConnection) con;
            httpsCon.setSSLSocketFactory(sc.getSocketFactory());
            httpsCon.setHostnameVerifier(new HostnameVerifier() {

                public boolean verify(String urlHostName, SSLSession session) {
                    return true;
                }
            });
        }
        return con;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(getQueryParams("http://www.google.co.in/search?hl=en&client=firefox-a&rls=org.mozilla%3Aen-US%3Aofficial&hs=Jvw&q=java%26url+encode&btnG=Search&meta=&aq=f&oq=", null));
        System.out.println(suggestPrefix(new Properties(), "urn:xxx:yyy"));
    }
}
