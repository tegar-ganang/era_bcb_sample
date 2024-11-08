package cn.shining365.webclips.tidy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import cn.shining365.webclips.common.Utils;

public class Quicktidy extends Tidy {

    public Quicktidy() {
        super(false, false, false);
    }

    /**返回不经修改的原始页面*/
    @Override
    protected String getRawPage(String url) throws IOException {
        HttpClient httpClient = new HttpClient();
        String proxyHost = config.getString("proxy.host"), proxyPortString = config.getString("proxy.port");
        if (proxyHost != null && proxyPortString != null) {
            int proxyPort = -1;
            try {
                proxyPort = Integer.parseInt(proxyPortString);
            } catch (NumberFormatException e) {
            }
            if (proxyPort != -1) {
                httpClient.getHostConfiguration().setProxy(proxyHost, proxyPort);
            }
        }
        GetMethod urlGet = new GetMethod(url);
        urlGet.setRequestHeader("Accept-Encoding", "");
        urlGet.setRequestHeader("User-Agent", "Mozilla/5.0");
        int retCode;
        if ((retCode = httpClient.executeMethod(urlGet)) != HttpStatus.SC_OK) {
            throw new RuntimeException("Unexpected HTTP code: " + retCode);
        }
        String encoding = null;
        Header contentType = urlGet.getResponseHeader("Content-Type");
        if (contentType != null) {
            String contentTypeString = contentType.toString();
            int i = contentTypeString.indexOf("charset=");
            if (i != -1) {
                encoding = contentTypeString.substring(i + "charset=".length()).trim();
            }
        }
        boolean gzipped = false;
        Header contentEncoding = urlGet.getResponseHeader("Content-Encoding");
        if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
            gzipped = true;
        }
        byte[] htmlData;
        try {
            InputStream in = gzipped ? new GZIPInputStream(urlGet.getResponseBodyAsStream()) : urlGet.getResponseBodyAsStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(in, out);
            htmlData = out.toByteArray();
            in.close();
        } finally {
            urlGet.releaseConnection();
        }
        if (encoding == null) {
            Matcher m = Pattern.compile("(?i)<meta[^>]*charset=(([^\"]+\")|(\"[^\"]+\"))").matcher(new String(htmlData));
            if (m.find()) {
                encoding = m.group(1).trim().replace("\"", "");
            }
        }
        if (encoding == null) {
            encoding = "UTF-8";
        }
        return new String(htmlData, encoding);
    }

    private static final Configuration config = Utils.getConfiguration();
}
