package com.retain;

import java.io.IOException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

/**
 * <p>Subclass of the Apache {@link DefaultHttpClient} that is configured with
 * reasonable default settings and registered schemes for Android, and
 * also lets the user add {@link HttpRequestInterceptor} classes.
 * Don't create this directly, use the {@link #newInstance} factory method.</p>
 * <p/>
 * <p>This client processes cookies but does not retain them by default.
 * To retain cookies, simply add a cookie store to the HttpContext:
 * <pre>context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);</pre>
 * </p>
 */
public final class AndroidHttpClient implements HttpClient {

    /**
   * Set if HTTP requests are blocked from being executed on this thread
   */
    private static final ThreadLocal<Boolean> sThreadBlocked = new ThreadLocal<Boolean>();

    /**
   * Interceptor throws an exception if the executing thread is blocked
   */
    private static final HttpRequestInterceptor sThreadCheckInterceptor = new HttpRequestInterceptor() {

        public void process(HttpRequest request, HttpContext context) {
            if (Boolean.TRUE.equals(sThreadBlocked.get())) {
                throw new RuntimeException("This thread forbids HTTP requests");
            }
        }
    };

    /**
   * Create a new HttpClient with reasonable defaults (which you can update).
   *
   * @param userAgent to report in your HTTP requests.
   * @return AndroidHttpClient for you to use for all your requests.
   */
    private Context mActivity;

    private String mUrl;

    public static AndroidHttpClient newInstance(Context act, String url, String userAgent) {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setStaleCheckingEnabled(params, false);
        HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);
        HttpConnectionParams.setSoTimeout(params, 20 * 1000);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
        HttpClientParams.setRedirecting(params, true);
        HttpProtocolParams.setUserAgent(params, userAgent);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        ClientConnectionManager manager = new ThreadSafeClientConnManager(params, schemeRegistry);
        return new AndroidHttpClient(act, url, manager, params);
    }

    private final HttpClient delegate;

    private AndroidHttpClient(Context act, String url, ClientConnectionManager ccm, HttpParams params) {
        mActivity = act;
        mUrl = url;
        this.delegate = new DefaultHttpClient(ccm, params) {

            @Override
            protected BasicHttpProcessor createHttpProcessor() {
                BasicHttpProcessor processor = super.createHttpProcessor();
                processor.addRequestInterceptor(sThreadCheckInterceptor);
                return processor;
            }

            @Override
            protected HttpContext createHttpContext() {
                HttpContext context = new BasicHttpContext();
                context.setAttribute(ClientContext.AUTHSCHEME_REGISTRY, getAuthSchemes());
                context.setAttribute(ClientContext.COOKIESPEC_REGISTRY, getCookieSpecs());
                context.setAttribute(ClientContext.CREDS_PROVIDER, getCredentialsProvider());
                CookieSyncManager syncr = CookieSyncManager.createInstance(mActivity);
                syncr.startSync();
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setAcceptCookie(true);
                String cookie = cookieManager.getCookie(mUrl);
                if (cookie != null) {
                    Log.d("HttpClient", "Cookie=" + cookie);
                    CookieStore store = new BasicCookieStore();
                    String[] parts = cookie.split(";");
                    for (int i = 0; i < parts.length; i++) {
                        String[] pieces = parts[i].split("=");
                        if (pieces.length == 2) {
                            Cookie c = new BasicClientCookie(pieces[0], pieces[1]);
                            store.addCookie(c);
                        }
                    }
                    context.setAttribute(ClientContext.COOKIE_STORE, store);
                    if (parts.length == 0) Log.e("HttpClient", "Bad cookie value=" + cookie);
                }
                syncr.stopSync();
                return context;
            }
        };
    }

    /**
   * Release resources associated with this client.  You must call this,
   * or significant resources (sockets and memory) may be leaked.
   */
    public void close() {
        getConnectionManager().shutdown();
    }

    public HttpParams getParams() {
        return delegate.getParams();
    }

    public ClientConnectionManager getConnectionManager() {
        return delegate.getConnectionManager();
    }

    public HttpResponse execute(HttpUriRequest request) throws IOException {
        return delegate.execute(request);
    }

    public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException {
        return delegate.execute(request, context);
    }

    public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException {
        return delegate.execute(target, request);
    }

    public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
        return delegate.execute(target, request, context);
    }

    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException {
        return delegate.execute(request, responseHandler);
    }

    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException {
        return delegate.execute(request, responseHandler, context);
    }

    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException {
        return delegate.execute(target, request, responseHandler);
    }

    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException {
        return delegate.execute(target, request, responseHandler, context);
    }
}
