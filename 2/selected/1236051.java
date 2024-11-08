package se.vgr.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

/**
 * Utilities for dealing with urls.
 * 
 */
public class HTTPUtils {

    public static String encode(String value) {
        try {
            return URLEncoder.encode(value, "utf-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String decode(String value) {
        try {
            return URLDecoder.decode(value, "utf-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
    public static HttpResponse basicAuthRequest(String url, String username, String password, DefaultHttpClient client) throws HttpUtilsException {
        HttpGet get = new HttpGet(url);
        client.getCredentialsProvider().setCredentials(new AuthScope(null, 443), new UsernamePasswordCredentials(username, password));
        BasicHttpContext localcontext = new BasicHttpContext();
        BasicScheme basicAuth = new BasicScheme();
        localcontext.setAttribute("preemptive-auth", basicAuth);
        client.addRequestInterceptor(new PreemptiveAuth(), 0);
        HttpResponse response;
        try {
            response = client.execute(get, localcontext);
        } catch (ClientProtocolException e) {
            throw new HttpUtilsException("Invalid http protocol", e);
        } catch (IOException e) {
            throw new HttpUtilsException(e.getMessage(), e);
        }
        return response;
    }

    /** */
    public static HttpResponse makeRequest(String url, String token, DefaultHttpClient client) throws Exception {
        HttpGet get = new HttpGet(url);
        get.addHeader("X-TrackerToken", token);
        return client.execute(get);
    }

    /** */
    public static HttpResponse makePostXML(String url, String token, DefaultHttpClient client, String xml) throws Exception {
        HttpPost post = new HttpPost(url);
        StringEntity e = new StringEntity(xml, "utf-8");
        e.setContentType("application/xml");
        post.addHeader("X-TrackerToken", token);
        post.addHeader("Content-type", "application/xml");
        post.setEntity(e);
        return client.execute(post);
    }

    /**
	 * @param url
	 * @param token
	 * @param httpclient
	 * @param list
	 * @return
	 * @throws Exception
	 */
    public static HttpResponse makePostAttachments(String url, String token, DefaultHttpClient httpclient, File aFile, String aFileName) throws Exception {
        HttpPost httppost = new HttpPost(url);
        httppost.addHeader("X-TrackerToken", token);
        httppost.removeHeaders("Connection");
        MultipartEntity mpEntity = new MultipartEntity();
        File f = aFile;
        if (aFileName != null && aFileName.length() > 0) {
            File newFile = new File(f.getParentFile(), aFileName);
            boolean renameSuccess = f.renameTo(newFile);
            if (renameSuccess) {
                f = newFile;
            } else {
                newFile = new File(f.getParentFile(), "" + (System.currentTimeMillis() % 1000000) + aFileName);
                renameSuccess = f.renameTo(newFile);
                if (renameSuccess) {
                    f = newFile;
                }
            }
        }
        ContentBody cbFile = new FileBody(f, "binary/octet-stream");
        mpEntity.addPart("Filedata", cbFile);
        httppost.setEntity(mpEntity);
        HttpResponse response = httpclient.execute(httppost);
        return response;
    }

    /** */
    static class PreemptiveAuth implements HttpRequestInterceptor {

        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
            if (authState.getAuthScheme() == null) {
                AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (authScheme != null) {
                    Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()));
                    if (creds == null) {
                        throw new HttpException("No credentials for preemptive authentication");
                    }
                    authState.setAuthScheme(authScheme);
                    authState.setCredentials(creds);
                }
            }
        }
    }
}
