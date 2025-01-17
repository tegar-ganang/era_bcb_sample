package com.android.mms.transaction;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.HttpConnectionParams;
import com.android.mms.MmsConfig;
import com.android.mms.LogTag;
import com.android.mms.R;
import com.android.mms.ui.MessagingPreferenceActivity;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class HttpUtils {

    private static final String TAG = LogTag.TRANSACTION;

    private static final boolean DEBUG = false;

    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    public static final int HTTP_POST_METHOD = 1;

    public static final int HTTP_GET_METHOD = 2;

    private static final String HDR_VALUE_ACCEPT_LANGUAGE;

    static {
        HDR_VALUE_ACCEPT_LANGUAGE = getHttpAcceptLanguage();
    }

    private static String mUserAgent;

    private static final String HDR_KEY_ACCEPT = "Accept";

    private static final String HDR_KEY_ACCEPT_LANGUAGE = "Accept-Language";

    private static final String HDR_VALUE_ACCEPT = "*/*, application/vnd.wap.mms-message, application/vnd.wap.sic";

    private HttpUtils() {
    }

    /**
     * A helper method to send or retrieve data through HTTP protocol.
     *
     * @param token The token to identify the sending progress.
     * @param url The URL used in a GET request. Null when the method is
     *         HTTP_POST_METHOD.
     * @param pdu The data to be POST. Null when the method is HTTP_GET_METHOD.
     * @param method HTTP_POST_METHOD or HTTP_GET_METHOD.
     * @return A byte array which contains the response data.
     *         If an HTTP error code is returned, an IOException will be thrown.
     * @throws IOException if any error occurred on network interface or
     *         an HTTP error code(&gt;=400) returned from the server.
     */
    protected static byte[] httpConnection(Context context, long token, String url, byte[] pdu, int method, boolean isProxySet, String proxyHost, int proxyPort) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("URL must not be null.");
        }
        if (LOCAL_LOGV) {
            Log.v(TAG, "httpConnection: params list");
            Log.v(TAG, "\ttoken\t\t= " + token);
            Log.v(TAG, "\turl\t\t= " + url);
            Log.v(TAG, "\tUser-Agent\t\t=" + mUserAgent);
            Log.v(TAG, "\tmethod\t\t= " + ((method == HTTP_POST_METHOD) ? "POST" : ((method == HTTP_GET_METHOD) ? "GET" : "UNKNOWN")));
            Log.v(TAG, "\tisProxySet\t= " + isProxySet);
            Log.v(TAG, "\tproxyHost\t= " + proxyHost);
            Log.v(TAG, "\tproxyPort\t= " + proxyPort);
        }
        AndroidHttpClient client = null;
        try {
            URI hostUrl = new URI(url);
            HttpHost target = new HttpHost(hostUrl.getHost(), hostUrl.getPort(), HttpHost.DEFAULT_SCHEME_NAME);
            client = createHttpClient(context);
            HttpRequest req = null;
            switch(method) {
                case HTTP_POST_METHOD:
                    ProgressCallbackEntity entity = new ProgressCallbackEntity(context, token, pdu);
                    entity.setContentType("application/vnd.wap.mms-message");
                    HttpPost post = new HttpPost(url);
                    post.setEntity(entity);
                    req = post;
                    break;
                case HTTP_GET_METHOD:
                    req = new HttpGet(url);
                    break;
                default:
                    Log.e(TAG, "Unknown HTTP method: " + method + ". Must be one of POST[" + HTTP_POST_METHOD + "] or GET[" + HTTP_GET_METHOD + "].");
                    return null;
            }
            HttpParams params = client.getParams();
            if (isProxySet) {
                ConnRouteParams.setDefaultProxy(params, new HttpHost(proxyHost, proxyPort));
            }
            req.setParams(params);
            req.addHeader(HDR_KEY_ACCEPT, HDR_VALUE_ACCEPT);
            {
                String xWapProfileTagName = MmsConfig.getUaProfTagName();
                String xWapProfileUrl = MmsConfig.getUaProfUrl();
                if (xWapProfileUrl != null) {
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.d(LogTag.TRANSACTION, "[HttpUtils] httpConn: xWapProfUrl=" + xWapProfileUrl);
                    }
                    req.addHeader(xWapProfileTagName, xWapProfileUrl);
                }
            }
            String extraHttpParams = MmsConfig.getHttpParams();
            if (extraHttpParams != null) {
                String line1Number = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
                String line1Key = MmsConfig.getHttpParamsLine1Key();
                String paramList[] = extraHttpParams.split("\\|");
                for (String paramPair : paramList) {
                    String splitPair[] = paramPair.split(":", 2);
                    if (splitPair.length == 2) {
                        String name = splitPair[0].trim();
                        String value = splitPair[1].trim();
                        if (line1Key != null) {
                            value = value.replace(line1Key, line1Number);
                        }
                        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
                            req.addHeader(name, value);
                        }
                    }
                }
            }
            req.addHeader(HDR_KEY_ACCEPT_LANGUAGE, HDR_VALUE_ACCEPT_LANGUAGE);
            HttpResponse response = client.execute(target, req);
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != 200) {
                throw new IOException("HTTP error: " + status.getReasonPhrase());
            }
            HttpEntity entity = response.getEntity();
            byte[] body = null;
            if (entity != null) {
                try {
                    if (entity.getContentLength() > 0) {
                        body = new byte[(int) entity.getContentLength()];
                        DataInputStream dis = new DataInputStream(entity.getContent());
                        try {
                            dis.readFully(body);
                        } finally {
                            try {
                                dis.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Error closing input stream: " + e.getMessage());
                            }
                        }
                    }
                } finally {
                    if (entity != null) {
                        entity.consumeContent();
                    }
                }
            }
            return body;
        } catch (URISyntaxException e) {
            handleHttpConnectionException(e, url);
        } catch (IllegalStateException e) {
            handleHttpConnectionException(e, url);
        } catch (IllegalArgumentException e) {
            handleHttpConnectionException(e, url);
        } catch (SocketException e) {
            handleHttpConnectionException(e, url);
        } catch (Exception e) {
            handleHttpConnectionException(e, url);
        } finally {
            if (client != null) {
                client.close();
            }
        }
        return null;
    }

    private static void handleHttpConnectionException(Exception exception, String url) throws IOException {
        Log.e(TAG, "Url: " + url + "\n" + exception.getMessage());
        IOException e = new IOException(exception.getMessage());
        e.initCause(exception);
        throw e;
    }

    private static AndroidHttpClient createHttpClient(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mUserAgent = prefs.getString(MessagingPreferenceActivity.USER_AGENT, MmsConfig.getUserAgent());
        if (mUserAgent == null || mUserAgent.equals("") || mUserAgent.equals("default")) {
            mUserAgent = MmsConfig.getUserAgent();
        } else if (mUserAgent.equals("custom")) {
            mUserAgent = prefs.getString(MessagingPreferenceActivity.USER_AGENT_CUSTOM, MmsConfig.getUserAgent());
        }
        AndroidHttpClient client = AndroidHttpClient.newInstance(mUserAgent);
        HttpParams params = client.getParams();
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        int soTimeout = MmsConfig.getHttpSocketTimeout();
        if (Log.isLoggable(LogTag.TRANSACTION, Log.DEBUG)) {
            Log.d(TAG, "[HttpUtils] createHttpClient w/ socket timeout " + soTimeout + " ms, " + ", UA=" + mUserAgent);
        }
        HttpConnectionParams.setSoTimeout(params, soTimeout);
        return client;
    }

    /**
     * Return the Accept-Language header.  Use the current locale plus
     * US if we are in a different locale than US.
     */
    private static String getHttpAcceptLanguage() {
        Locale locale = Locale.getDefault();
        StringBuilder builder = new StringBuilder();
        addLocaleToHttpAcceptLanguage(builder, locale);
        if (!locale.equals(Locale.US)) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            addLocaleToHttpAcceptLanguage(builder, Locale.US);
        }
        return builder.toString();
    }

    private static void addLocaleToHttpAcceptLanguage(StringBuilder builder, Locale locale) {
        String language = locale.getLanguage();
        if (language != null) {
            builder.append(language);
            String country = locale.getCountry();
            if (country != null) {
                builder.append("-");
                builder.append(country);
            }
        }
    }
}
