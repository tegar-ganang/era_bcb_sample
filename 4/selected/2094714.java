package com.nubotech.gwt.oss.client.s3;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.nubotech.gwt.oss.client.AccessControlPolicy;
import com.nubotech.gwt.oss.client.ObjectHolder;
import com.nubotech.gwt.oss.client.RequestHeader;
import com.nubotech.gwt.oss.client.auth.Credential;
import com.nubotech.gwt.oss.client.util.DateUtils;
import com.nubotech.gwt.oss.client.util.LogUtil;
import com.nubotech.gwt.oss.client.util.RestEncoder;
import com.nubotech.gwt.oss.client.util.Sha1;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jonnakkerud
 */
public class AwsS3 {

    public static final String HOST_URL = "s3.amazonaws.com";

    public static final AwsS3CallbackHandler HANDLER = new DefaultCallbackHandler();

    private String bucket;

    private String host;

    private Credential credential;

    private AwsS3CallbackHandler callbackHandler;

    public AwsS3(String host, Credential c) {
        this.credential = c;
        this.host = host;
    }

    public void doPut(String resource) {
        httpRequest("PUT", resource, ObjectHolder.EMPTY, null, null);
    }

    public void doPut(String resource, ObjectHolder content) {
        httpRequest("PUT", resource, content, null, null);
    }

    public void doPut(String resource, ObjectHolder content, RequestHeader[] headers) {
        httpRequest("PUT", resource, content, headers, null);
    }

    public void doGet(String resource) {
        httpRequest("GET", resource, null, null, null);
    }

    public void doGet(String resource, Map params) {
        httpRequest("GET", resource, null, null, params);
    }

    public void doGet(String resource, RequestHeader[] headers) {
        httpRequest("GET", resource, null, headers, null);
    }

    public void doDelete(String resource) {
        httpRequest("DELETE", resource, null, null, null);
    }

    public void doHead(String resource) {
        httpRequest("HEAD", resource, null, null, null);
    }

    private Request httpRequest(String method, String resource, ObjectHolder content, RequestHeader[] headers, Map params) {
        String url = createUrl(getBucket(), resource, params);
        AwsRequestBuilder builder = new AwsRequestBuilder(method, url);
        String requestData = null;
        if (content != null) {
            if (content.getAccessPolicy() > -1) {
                builder.setHeader("x-amz-acl", accessPolicyString(content.getAccessPolicy()));
            }
            if (content.hasMetaData()) {
                Iterator itr = content.getMetaData().keySet().iterator();
                while (itr.hasNext()) {
                    String key = (String) itr.next();
                    builder.setHeader("x-amz-meta-" + key, (String) content.getMetaData().get(key));
                }
            }
            builder.setHeader("Content-Length", String.valueOf(content.getSize()));
        }
        String dateStr = DateUtils.formatHTTPDate(new Date());
        builder.setHeader("x-amz-date", dateStr);
        if (credential.isAnonymous() == false) {
            String md5 = RequestHeader.getRequestHeaderValue(RequestHeader.CONTENT_MD5, "", headers);
            String contentType = RequestHeader.getRequestHeaderValue(RequestHeader.CONTENT_TYPE, "", headers);
            String signature = createSignature(new String[] { method, md5, contentType, "" }, createCanonicalizedAmzHeaders(builder), createCanonicalizedResource(getBucket(), resource, false));
            builder.setHeader("Authorization", "AWS" + " " + credential.getAccount() + ":" + signature);
        }
        if (headers != null) {
            for (int i = 0; i < headers.length; i++) {
                builder.setHeader(headers[i]);
            }
        }
        Request request = null;
        try {
            request = builder.sendRequest(requestData, getCallbackHandler());
        } catch (RequestException e) {
            LogUtil.log(e.getLocalizedMessage());
            getCallbackHandler().onError(null, e);
        }
        return request;
    }

    protected String createCanonicalizedResource(String bucketName, String resource, boolean pathEncoding) {
        StringBuffer sb = new StringBuffer();
        if (bucketName != null) {
            sb.append("/").append(bucketName);
        }
        sb.append(resource);
        if (pathEncoding) {
            return RestEncoder.encodeUrlPath(sb.toString(), "/");
        } else {
            return URL.encode(sb.toString());
        }
    }

    protected String createSignature(String[] stringToSign, String canonicalizedAmzHeaders, String canonicalizedResource) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < stringToSign.length; i++) {
            sb.append(stringToSign[i]).append("\n");
        }
        sb.append(canonicalizedAmzHeaders).append(canonicalizedResource);
        return hmacSHA1(sb.toString(), credential.getSecretKey());
    }

    protected String createUrl(String bucketName, String resource, Map params) {
        StringBuffer sb = new StringBuffer();
        sb.append("http://");
        sb.append(getHost());
        if (bucketName != null) {
            sb.append("/").append(bucketName);
        }
        if (resource != null) {
            sb.append(resource);
        }
        if (params != null) {
            sb.append("?");
            Iterator itr = params.keySet().iterator();
            while (itr.hasNext()) {
                String key = (String) itr.next();
                sb.append(key).append("=").append(params.get(key));
                if (itr.hasNext()) {
                    sb.append("&");
                }
            }
        }
        return URL.encode(sb.toString());
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getHost() {
        if (host == null) {
            host = HOST_URL;
        }
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public AwsS3CallbackHandler getCallbackHandler() {
        if (callbackHandler == null) {
            callbackHandler = HANDLER;
        }
        return callbackHandler;
    }

    public void setCallbackHandler(AwsS3CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    public static String hmacSHA1(String data, String secret) {
        String result = Sha1.b64_hmac_sha1(secret, data) + "=";
        return result;
    }

    private String createCanonicalizedAmzHeaders(AwsRequestBuilder builder) {
        StringBuffer sb = new StringBuffer("");
        if (builder.getAmzHeaders().size() > 0) {
            Iterator itr = builder.getSortedHeaders().iterator();
            while (itr.hasNext()) {
                String key = (String) itr.next();
                String value = (String) builder.getAmzHeaders().get(key);
                sb.append(key).append(":").append(value.trim()).append("\n");
            }
        }
        return sb.toString();
    }

    public static String accessPolicyString(int policy) {
        String result = null;
        switch(policy) {
            case 0:
                result = "private";
                break;
            case 1:
                result = "public-read";
                break;
            case 2:
                result = "public-read-write";
                break;
            case 3:
                result = "authenticated-read";
                break;
        }
        return result;
    }

    static class AwsRequestBuilder extends RequestBuilder {

        private Map amzheaders = new HashMap();

        public void setHeader(RequestHeader header) {
            setHeader(header.getHeaderName(), header.getHeaderValue());
        }

        @Override
        public void setHeader(String header, String value) {
            super.setHeader(header, value);
            String canonacalizedHeader = header.toLowerCase();
            if (canonacalizedHeader.startsWith("x-amz-")) {
                amzheaders.put(canonacalizedHeader, value);
            }
        }

        public Map getAmzHeaders() {
            return amzheaders;
        }

        public List getSortedHeaders() {
            List sorted = new ArrayList(amzheaders.keySet());
            Collections.sort(sorted);
            return sorted;
        }

        /**
         * Constructor that allows a developer to override the HTTP method
         * restrictions imposed by the RequestBuilder class.  Note if you override the
         * RequestBuilder's HTTP method restrictions in this manner, your application
         * may not work correctly on Safari browsers.
         *
         * @param httpMethod any non-null, non-empty string is considered valid
         * @param url any non-null, non-empty string is considered valid
         *
         * @throws IllegalArgumentException if httpMethod or url are empty
         * @throws NullPointerException if httpMethod or url are null
         */
        public AwsRequestBuilder(String httpMethod, String url) {
            super(httpMethod, url);
        }
    }
}
