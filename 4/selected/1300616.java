package nuts.core.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import nuts.core.io.IOUtils;
import nuts.core.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

public class HttpClientAgent {

    private static Log log = LogFactory.getLog(HttpClientAgent.class);

    public static final int GET = 1;

    public static final int POST = 2;

    private Map<String, String> requestHeader;

    private CookieStore cookieStore = new BasicCookieStore();

    private HttpResponse response;

    private InputStream responseStream;

    private byte[] responseContent;

    private String responseText;

    /**
	 * @return the header
	 */
    public Map<String, String> getRequestHeader() {
        return requestHeader;
    }

    /**
	 * @param header the header to set
	 */
    public void setRequestHeader(Map<String, String> header) {
        this.requestHeader = header;
    }

    public HttpClientAgent() {
        requestHeader = new HashMap<String, String>();
        requestHeader.put("user-agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.835.186 Safari/535.1");
        requestHeader.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        requestHeader.put("accept-language", "en,ja,zh;q=0.7,en;q=0.3");
        requestHeader.put("accept-encoding", "gzip,deflate");
        requestHeader.put("accept-charset", "utf-8,Shift_JIS,GB2312;q=0.7,*;q=0.7");
    }

    /**
	 * @return the cookieStore
	 */
    public CookieStore getCookieStore() {
        return cookieStore;
    }

    /**
	 * @param cookieStore the cookieStore to set
	 */
    public void setCookieStore(CookieStore cookieStore) {
        this.cookieStore = cookieStore;
    }

    /**
	 * @return the responseEntity
	 */
    public HttpEntity getResponseEntity() {
        return response.getEntity();
    }

    /**
	 * @return the responseStatus
	 */
    public StatusLine getResponseStatus() {
        return response.getStatusLine();
    }

    public int getResonseStatusCode() {
        return getResponseStatus().getStatusCode();
    }

    /**
	 * @return the responseStream
	 * @throws IOException 
	 */
    public InputStream getResponseStream() throws IOException {
        if (responseStream == null) {
            responseStream = getResponseEntity().getContent();
            if (responseStream != null) {
                Header ce = getResponseEntity().getContentEncoding();
                if (ce != null && "gzip".equalsIgnoreCase(ce.getValue())) {
                    responseStream = new GZIPInputStream(responseStream);
                }
            }
        }
        return responseStream;
    }

    /**
	 * @return the responseContent
	 * @throws IOException 
	 */
    public byte[] getResponseContent() throws IOException {
        if (responseContent == null) {
            InputStream is = getResponseStream();
            if (is == null) {
                responseContent = new byte[0];
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
                IOUtils.copy(is, baos);
                responseContent = baos.toByteArray();
            }
        }
        return responseContent;
    }

    /**
	 * @return the responseContentLength
	 */
    public long getResponseContentLength() {
        return response.getEntity().getContentLength();
    }

    /**
	 * @return the responseCharset
	 */
    public String getResponseCharset() {
        return EntityUtils.getContentCharSet(getResponseEntity());
    }

    /**
	 * @return the responseText
	 * @throws IOException 
	 * @throws UnsupportedEncodingException 
	 */
    public String getResponseText() throws UnsupportedEncodingException, IOException {
        if (responseText == null) {
            String charset = getResponseCharset();
            if (StringUtils.isNotEmpty(charset)) {
                responseText = new String(getResponseContent(), charset);
            } else {
                responseText = new String(getResponseContent());
            }
        }
        return responseText;
    }

    public void doGet(String uri) throws IOException {
        doRequest(uri, GET, null);
    }

    public void doGet(String uri, Map<String, String> params) throws IOException {
        doRequest(uri, GET, params);
    }

    public void doPost(String uri) throws IOException {
        doRequest(uri, POST, null);
    }

    public void doPost(String uri, Map<String, String> forms) throws IOException {
        doRequest(uri, POST, forms);
    }

    public void doRequest(String uri, String method, Map<String, String> forms) throws IOException {
        if ("get".equalsIgnoreCase(method)) {
            doRequest(uri, GET, forms);
        } else if ("post".equalsIgnoreCase(method)) {
            doRequest(uri, POST, forms);
        }
    }

    public void doRequest(String uri, int method, Map<String, String> forms) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug(uri + " ... ");
        }
        response = null;
        responseStream = null;
        responseContent = null;
        responseText = null;
        HttpClient httpclient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        List<NameValuePair> params = null;
        if (forms != null) {
            params = new ArrayList<NameValuePair>();
            for (Entry<String, String> e : forms.entrySet()) {
                params.add(new BasicNameValuePair(e.getKey(), e.getValue()));
            }
            if (log.isDebugEnabled()) {
                if (forms.size() > 0) {
                    StringBuilder msg = new StringBuilder();
                    msg.append(StringUtils.center(" Request Forms ", 78, '=')).append("\r\n");
                    for (Entry<String, String> e : forms.entrySet()) {
                        msg.append(e.getKey()).append(":").append(e.getValue()).append("\r\n");
                    }
                    log.debug(msg);
                }
            }
        }
        HttpRequestBase httpreq = null;
        switch(method) {
            case GET:
                if (params != null) {
                    httpreq = new HttpGet(uri + "?" + URLEncodedUtils.format(params, "UTF-8"));
                } else {
                    httpreq = new HttpGet(uri);
                }
                break;
            case POST:
                httpreq = new HttpPost(uri);
                ((HttpPost) httpreq).setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
                break;
        }
        if (requestHeader != null) {
            for (Entry<String, String> e : requestHeader.entrySet()) {
                httpreq.addHeader(e.getKey(), e.getValue());
            }
        }
        long st = System.currentTimeMillis();
        response = httpclient.execute(httpreq, localContext);
        long et = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            StringBuilder msg = new StringBuilder();
            msg.append(uri).append(" - ").append(response.getStatusLine()).append(" ").append(getResponseEntity().getContentLength()).append("B").append(" ").append(et - st).append("ms").append("\r\n");
            if (log.isTraceEnabled()) {
                Header[] ah = response.getAllHeaders();
                if (ah.length > 0) {
                    msg.append(StringUtils.center(" Response Header ", 78, '=')).append("\r\n");
                    for (Header h : ah) {
                        msg.append(h).append("\r\n");
                    }
                }
                msg.append(StringUtils.center(" Response Content (" + getResponseContent().length + ")", 78, '=')).append("\r\n").append(getResponseText());
            }
            log.debug(msg);
        }
    }

    public static void main(String[] args) {
        try {
            HttpClientAgent hca = new HttpClientAgent();
            System.out.print(">");
            Scanner stdin = new Scanner(System.in);
            String line;
            while ((line = stdin.nextLine()) != null) {
                line = StringUtils.strip(line);
                if ("exit".equals(line)) {
                    break;
                } else {
                    String[] ss = StringUtils.split(line);
                    if (ss.length == 1) {
                        hca.doGet(ss[0]);
                    } else if (ss.length == 2) {
                        String method = ss[0];
                        String url = ss[1];
                        hca.doRequest(url, method, null);
                    }
                }
                System.out.print(">");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
