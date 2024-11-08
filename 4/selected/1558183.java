package org.elmarweber.sf.appletrailerfs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;

/**
 * Guess what?
 * 
 * @author Elmar Weber (appletrailerfs@elmarweber.org)
 */
public class HTTPUtils {

    private static final String[] USER_AGENTS = new String[] { "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.0.10) Gecko/2009042316 Firefox/3.0.10 (.NET CLR 3.5.30729)", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/525.28 (KHTML, like Gecko) Version/3.2.2 Safari/525.28.1", "Mozilla/5.0 (Macintosh; U; PPC Mac OS X 10_5_4; en-US) AppleWebKit/525.18 (KHTML, like Gecko) Version/3.1 Safari/525.13", "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.0.9) Gecko/2009042114 Ubuntu/9.04 (jaunty) Firefox/3.0.9", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.0.10) Gecko/20070216 Firefox/1.5.0.10", "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.8.0.12) Gecko/20070508 Firefox/1.5.0.12 (.NET CLR 3.5.30729)", "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.8.0.12) Gecko/20070508 Firefox/1.5.0.12", "Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US; rv:1.8.0.12) Gecko/20070508 Firefox/1.5.0.12" };

    /**
     * Downloads the contents of the specified URI.
     * 
     * @param uri
     *            the URI to download.
     * 
     * @return the content of the specifed URI as a byte array.
     */
    public static byte[] getContent(String uri) throws HttpException, IOException {
        HttpClient client = new HttpClient();
        GetMethod method = new GetMethod(uri);
        method.setRequestHeader("User-Agent", USER_AGENTS[(int) (Math.random() * USER_AGENTS.length)]);
        client.executeMethod(method);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = method.getResponseBodyAsStream();
        int read = 0;
        byte[] buffer = new byte[4096];
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        method.releaseConnection();
        return out.toByteArray();
    }

    /**
     * Downloads the contents of the specified URI.
     * 
     * @param uri
     *            the URI to download.
     * 
     * @return the content of the specifed URI as a string.
     */
    public static String getContentAsString(String uri) throws HttpException, IOException {
        return new String(getContent(uri));
    }

    /**
     * 
     * @param uri
     *            the URI to check the content length of.
     * 
     * @return the content length of the file behind the specified URI.
     */
    public static long getContentLength(String uri) throws HttpException, IOException {
        return getContentLength(uri, null);
    }

    /**
     * 
     * @param uri
     *            the URI to check the content length of.
     * @param userAgent
     *            the user agent string to use.
     * 
     * @return the content length of the file behind the specified URI.
     */
    public static long getContentLength(String uri, String userAgent) throws HttpException, IOException {
        HttpClient client = new HttpClient();
        HeadMethod method = new HeadMethod(uri);
        method.setRequestHeader("User-Agent", userAgent != null ? userAgent : USER_AGENTS[(int) (Math.random() * USER_AGENTS.length)]);
        client.executeMethod(method);
        long length = method.getResponseContentLength();
        method.releaseConnection();
        return length;
    }

    /**
     * Iterates over all links in the specified HTML document. Duplicate links
     * won't be visited again.
     * 
     * @param html
     *            the HTML to parse.
     * @param visitor
     *            a {@link LinkVisitor} that is notified for each link.
     * 
     * @throws IOException
     *             in case any error occurs while parsing the HTML page.
     */
    public static void visitLinks(String html, LinkVisitor visitor) throws IOException {
        Set<String> visited = new HashSet<String>();
        int lastIndex = html.lastIndexOf("href=");
        int index = 0;
        while (index != lastIndex) {
            index = html.indexOf("href=", index + 1);
            if (index != -1) {
                int from = html.indexOf('"', index + "href=".length()) + 1;
                int to = html.indexOf('"', from);
                if ((from != -1) && (to != -1) && (from != to)) {
                    String href = html.substring(from, to);
                    if (!visited.contains(href)) {
                        visitor.visit(href);
                        visited.add(href);
                    }
                }
            }
        }
    }
}
