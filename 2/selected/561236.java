package org.apache.shindig.gadgets.servlet;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.JsonProperty;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.FeedProcessor;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.rewrite.RequestRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.apache.shindig.protocol.BaseRequestItem;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.Service;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * An alternate implementation of the Http proxy service using the standard API dispatcher for REST
 * / JSON-RPC calls. The basic form of the request is as follows
 * ...
 * method : http.<HTTP method name>
 * params : {
 *    href : <endpoint to fetch content from>,
 *    headers : { <header-name> : [<header-value>, ...]},
 *    format : <"text", "json", "feed">
 *    body : <request body>
 *    gadget : <url of gadget spec for calling application>
 *    authz: : <none | oauth | signed>,
 *    sign_owner: <boolean, default true>
 *    sign_viewer: <boolean, default true>
 *    ...<additional auth arguments. See OAuthArguments>
 *    refreshInterval : <Integer time in seconds to force as cache TTL. Default is to use response headers>
 *    noCache : <Bypass container content cache. Default false>
 *    sanitize : <Force sanitize fetched content. Default false>
 *    summarize : <If contentType == "FEED" summarize the results. Default false>
 *    entryCount : <If contentType == "FEED" limit results to specified no of items. Default 3>
 * }
 *
 * A successful response response will have the form
 *
 * data : {
 *    status : <HTTP status code.>
 *    headers : { <header name> : [<header val1>, <header val2>, ...], ...}
 *    content : <response body>: string if 'text', JSON is 'feed' or 'json' format
 *    token : <If security token provides a renewed value.>
 *    metadata : { <metadata entry> : <metadata value>, ...}
 * }
 *
 * It's important to note that requests which generate HTTP error responses such as 500 are returned
 * in the above format. The RPC itself succeeded in these cases. If an RPC error occurred the client
 * should introspect the error message for information as to the cause.
 * 
 * TODO: send errors using "data", not plain content
 *
 * @see MakeRequestHandler
 */
@Service(name = "http")
public class HttpRequestHandler {

    static final Set<String> BAD_HEADERS = ImmutableSet.of("HOST", "ACCEPT-ENCODING");

    private final RequestPipeline requestPipeline;

    private final RequestRewriterRegistry contentRewriterRegistry;

    @Inject
    public HttpRequestHandler(RequestPipeline requestPipeline, RequestRewriterRegistry contentRewriterRegistry) {
        this.requestPipeline = requestPipeline;
        this.contentRewriterRegistry = contentRewriterRegistry;
    }

    /** Execute an HTTP GET request */
    @Operation(httpMethods = { "POST", "GET" }, path = "/get")
    public HttpApiResponse get(BaseRequestItem request) {
        HttpApiRequest httpReq = request.getTypedRequest(HttpApiRequest.class);
        assertNoBody(httpReq, "GET");
        return execute("GET", httpReq, request);
    }

    /** Execute an HTTP POST request */
    @Operation(httpMethods = "POST", path = "/post")
    public HttpApiResponse post(BaseRequestItem request) {
        HttpApiRequest httpReq = request.getTypedRequest(HttpApiRequest.class);
        return execute("POST", httpReq, request);
    }

    /** Execute an HTTP PUT request */
    @Operation(httpMethods = "POST", path = "/put")
    public HttpApiResponse put(BaseRequestItem request) {
        HttpApiRequest httpReq = request.getTypedRequest(HttpApiRequest.class);
        return execute("PUT", httpReq, request);
    }

    /** Execute an HTTP DELETE request */
    @Operation(httpMethods = "POST", path = "/delete")
    public HttpApiResponse delete(BaseRequestItem request) {
        HttpApiRequest httpReq = request.getTypedRequest(HttpApiRequest.class);
        assertNoBody(httpReq, "DELETE");
        return execute("DELETE", httpReq, request);
    }

    /** Execute an HTTP HEAD request */
    @Operation(httpMethods = { "POST", "GET" }, path = "/head")
    public HttpApiResponse head(BaseRequestItem request) {
        HttpApiRequest httpReq = request.getTypedRequest(HttpApiRequest.class);
        assertNoBody(httpReq, "HEAD");
        return execute("HEAD", httpReq, request);
    }

    private void assertNoBody(HttpApiRequest httpRequest, String method) {
        if (httpRequest.body != null) {
            throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Request body not supported for " + method);
        }
    }

    /**
   * Dispatch the request
   *
   * @param method HTTP method to execute
   * @param requestItem TODO
   */
    private HttpApiResponse execute(String method, HttpApiRequest httpApiRequest, BaseRequestItem requestItem) {
        if (httpApiRequest.href == null) {
            throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "href parameter is missing");
        }
        Uri href = normalizeUrl(httpApiRequest.href);
        try {
            HttpRequest req = new HttpRequest(href);
            req.setMethod(method);
            if (httpApiRequest.body != null) {
                req.setPostBody(httpApiRequest.body.getBytes());
            }
            for (Map.Entry<String, List<String>> header : httpApiRequest.headers.entrySet()) {
                if (!BAD_HEADERS.contains(header.getKey().trim().toUpperCase())) {
                    for (String value : header.getValue()) {
                        req.addHeader(header.getKey(), value);
                    }
                }
            }
            Uri gadgetUri = getGadgetUri(requestItem.getToken(), httpApiRequest);
            if (gadgetUri == null) {
                throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Gadget URI not specified in request");
            }
            req.setGadget(gadgetUri);
            if (httpApiRequest.authz != null) {
                req.setAuthType(AuthType.parse(httpApiRequest.authz));
            }
            if (req.getAuthType() != AuthType.NONE) {
                req.setSecurityToken(requestItem.getToken());
                Map<String, String> authSettings = getAuthSettings(requestItem);
                OAuthArguments oauthArgs = new OAuthArguments(req.getAuthType(), authSettings);
                oauthArgs.setSignOwner(httpApiRequest.signOwner);
                oauthArgs.setSignViewer(httpApiRequest.signViewer);
                req.setOAuthArguments(oauthArgs);
            }
            req.setIgnoreCache(httpApiRequest.noCache);
            req.setSanitizationRequested(httpApiRequest.sanitize);
            if (httpApiRequest.refreshInterval != null) {
                req.setCacheTtl(httpApiRequest.refreshInterval);
            }
            HttpResponse results = requestPipeline.execute(req);
            results = contentRewriterRegistry.rewriteHttpResponse(req, results);
            HttpApiResponse httpApiResponse = new HttpApiResponse(results, transformBody(httpApiRequest, results), httpApiRequest);
            if (requestItem.getToken() != null) {
                String updatedAuthToken = requestItem.getToken().getUpdatedToken();
                if (updatedAuthToken != null) {
                    httpApiResponse.token = updatedAuthToken;
                }
            }
            return httpApiResponse;
        } catch (GadgetException ge) {
            throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ge.getMessage(), ge);
        } catch (RewritingException re) {
            throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, re.getMessage(), re);
        }
    }

    /**
   * Extract all unknown keys into a map for extra auth params.
   */
    private Map<String, String> getAuthSettings(BaseRequestItem requestItem) {
        @SuppressWarnings("unchecked") Set<String> allParameters = requestItem.getTypedRequest(Map.class).keySet();
        Map<String, String> authSettings = Maps.newHashMap();
        for (String paramName : allParameters) {
            if (!HttpApiRequest.KNOWN_PARAMETERS.contains(paramName)) {
                authSettings.put(paramName, requestItem.getParameter(paramName));
            }
        }
        return authSettings;
    }

    protected Uri normalizeUrl(Uri url) {
        if (url.getScheme() == null) {
            url = new UriBuilder(url).setScheme("http").toUri();
        }
        if (url.getPath() == null || url.getPath().length() == 0) {
            url = new UriBuilder(url).setPath("/").toUri();
        }
        return url;
    }

    /** Format a response as JSON, including additional JSON inserted by chained content fetchers. */
    protected Object transformBody(HttpApiRequest request, HttpResponse results) throws GadgetException {
        String body = results.getResponseAsString();
        if ("feed".equalsIgnoreCase(request.format)) {
            return processFeed(request, body);
        } else if ("json".equalsIgnoreCase(request.format)) {
            try {
                return new JSONObject(body);
            } catch (JSONException e) {
                throw new ProtocolException(HttpServletResponse.SC_NOT_ACCEPTABLE, "Response not valid JSON", e);
            }
        }
        return body;
    }

    /** Processes a feed (RSS or Atom) using FeedProcessor. */
    protected Object processFeed(HttpApiRequest req, String responseBody) throws GadgetException {
        return new FeedProcessor().process(req.href.toString(), responseBody, req.summarize, req.entryCount);
    }

    /** Extract the gadget URL from the request or the security token */
    protected Uri getGadgetUri(SecurityToken token, HttpApiRequest httpApiRequest) {
        if (token != null && token.getAppUrl() != null) {
            return Uri.parse(token.getAppUrl());
        }
        return null;
    }

    /**
   * Simple type that represents an Http request to execute on the callers behalf
   */
    public static class HttpApiRequest {

        static final Set<String> KNOWN_PARAMETERS = ImmutableSet.of("href", "headers", "body", "gadget", "authz", "sign_owner", "sign_viewer", "format", "refreshInterval", "noCache", "sanitize", "summarize", "entryCount");

        Uri href;

        Map<String, List<String>> headers = Maps.newHashMap();

        /** POST body */
        String body;

        /** Authorization type ("none", "signed", "oauth") */
        String authz = "none";

        /** Should the request be signed by owner? */
        boolean signOwner = true;

        /** Should the request be signed by viewer? */
        boolean signViewer = true;

        String format;

        Integer refreshInterval;

        boolean noCache;

        boolean sanitize;

        boolean summarize;

        int entryCount = 3;

        public Uri getHref() {
            return href;
        }

        public void setHref(Uri url) {
            this.href = url;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, List<String>> headers) {
            this.headers = headers;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public Integer getRefreshInterval() {
            return refreshInterval;
        }

        public void setRefreshInterval(Integer refreshInterval) {
            this.refreshInterval = refreshInterval;
        }

        public boolean isNoCache() {
            return noCache;
        }

        public void setNoCache(boolean noCache) {
            this.noCache = noCache;
        }

        public boolean isSanitize() {
            return sanitize;
        }

        public void setSanitize(boolean sanitize) {
            this.sanitize = sanitize;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public String getAuthz() {
            return authz;
        }

        public void setAuthz(String authz) {
            this.authz = authz;
        }

        public boolean isSignViewer() {
            return signViewer;
        }

        @JsonProperty("sign_viewer")
        public void setSignViewer(boolean signViewer) {
            this.signViewer = signViewer;
        }

        public boolean isSignOwner() {
            return signOwner;
        }

        @JsonProperty("sign_owner")
        public void setSignOwner(boolean signOwner) {
            this.signOwner = signOwner;
        }

        public boolean isSummarize() {
            return summarize;
        }

        public void setSummarize(boolean summarize) {
            this.summarize = summarize;
        }

        public int getEntryCount() {
            return entryCount;
        }

        public void setEntryCount(int entryCount) {
            this.entryCount = entryCount;
        }
    }

    /**
   * Response to request for Http content
   */
    public static class HttpApiResponse {

        int status;

        Map<String, Collection<String>> headers;

        Object content;

        String token;

        Map<String, String> metadata;

        public HttpApiResponse(int status) {
            this.status = status;
        }

        /**
     * Construct response based on HttpResponse from fetcher
     */
        public HttpApiResponse(HttpResponse response, Object content, HttpApiRequest httpApiRequest) {
            this.status = response.getHttpStatusCode();
            this.headers = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
            if (response.getHeaders().containsKey("set-cookie")) {
                this.headers.put("set-cookie", response.getHeaders("set-cookie"));
            }
            if (response.getHeaders().containsKey("location")) {
                this.headers.put("location", response.getHeaders("location"));
            }
            this.content = content;
            this.metadata = response.getMetadata();
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public Map<String, Collection<String>> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, Collection<String>> headers) {
            this.headers = headers;
        }

        public Object getContent() {
            return content;
        }

        public void setContent(Object content) {
            this.content = content;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public Map<String, String> getMetadata() {
            if (metadata != null && metadata.isEmpty()) {
                return null;
            }
            return metadata;
        }

        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata;
        }
    }
}
