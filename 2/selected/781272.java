package com.googlecode.batchfb.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>For building and executing requests.</p>
 * 
 * @author Jeff Schnitzer
 */
public class RequestBuilder {

    /** */
    private static final Logger log = Logger.getLogger(RequestBuilder.class.getName());

    /** Supported methods */
    public static enum HttpMethod {

        GET, POST, DELETE
    }

    /** Returned by request execution */
    public static interface HttpResponse {

        /** The http response code */
        int getResponseCode() throws IOException;

        /** The body content of the response */
        InputStream getContentStream() throws IOException;
    }

    /** Used as a param value when user submits binary attachments */
    public static class BinaryAttachment {

        InputStream data;

        String contentType;

        String filename;

        BinaryAttachment(InputStream data, String contentType, String filename) {
            this.data = data;
            this.contentType = contentType;
            this.filename = filename;
        }
    }

    /** */
    String baseURL;

    Map<String, Object> params = new LinkedHashMap<String, Object>();

    Map<String, String> headers = new LinkedHashMap<String, String>();

    HttpMethod method;

    boolean hasBinaryAttachments;

    int timeout;

    int retries;

    /**
	 * Construct from a base URL like http://api.facebook.com/. It should not have any query parameters or a ?.
	 */
    public RequestBuilder(String url, HttpMethod method) {
        this(url, method, 0);
    }

    /**
	 * Construct from a base URL like http://api.facebook.com/. It should not have any query parameters or a ?.
	 */
    public RequestBuilder(String url, HttpMethod method, int timeout) {
        this(url, method, timeout, 0);
    }

    /**
	 * Construct from a base URL like http://api.facebook.com/. It should not have any query parameters or a ?.
	 */
    public RequestBuilder(String url, HttpMethod method, int timeout, int retries) {
        this.baseURL = url;
        this.method = method;
        this.timeout = timeout;
        this.retries = retries;
    }

    /**
	 * Adds a parameter, urlencoding both the name and value
	 */
    public void addParam(String name, String value) {
        this.params.put(name, value);
    }

    /**
	 * Adds a binary attachment. Request method must be POST; causes the type to be multipart/form-data
	 */
    public void addParam(String name, InputStream stream, String contentType, String filename) {
        if (this.method != HttpMethod.POST) throw new IllegalStateException("May only add binary attachment to POST, not to " + this.method);
        this.params.put(name, new BinaryAttachment(stream, contentType, filename));
        this.hasBinaryAttachments = true;
    }

    /**
	 * Adds a header.  Value is not encoded in any particular way.
	 */
    public void addHeader(String name, String value) {
        this.headers.put(name, value);
    }

    /**
	 * Set a connection/read timeout, or 0 for no timeout.
	 */
    public void setTimeout(int millis) {
        this.timeout = millis;
    }

    /**
	 * @return the URL + the queryString as if for a GET request
	 */
    public String toString() {
        if (this.params.isEmpty()) return this.baseURL; else return this.baseURL + '?' + this.createQueryString();
    }

    /**
	 * Execute the request, providing the result in the response object - which might be an async wrapper.
	 */
    public HttpResponse execute() throws IOException {
        return this.execute(this.method, this.baseURL);
    }

    /**
	 * Execute given the specified http method, retrying up to the allowed number of retries.
	 * @postURL is the url to use if this is a POST request; ignored otherwise.
	 */
    protected HttpResponse execute(final HttpMethod meth, String postURL) throws IOException {
        final String url = (meth == HttpMethod.POST) ? postURL : this.toString();
        if (log.isLoggable(Level.FINER)) log.finer(meth + "ing: " + url);
        return RequestExecutor.instance().execute(this.retries, new RequestSetup() {

            public void setup(RequestDefinition req) throws IOException {
                req.init(meth, url);
                for (Map.Entry<String, String> header : headers.entrySet()) req.setHeader(header.getKey(), header.getValue());
                if (timeout > 0) req.setTimeout(timeout);
                if (meth == HttpMethod.POST && !params.isEmpty()) {
                    if (!hasBinaryAttachments) {
                        String queryString = createQueryString();
                        if (log.isLoggable(Level.FINER)) log.finer("POST data is: " + queryString);
                        req.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
                        req.setContent(queryString.getBytes("utf-8"));
                    } else {
                        log.finer("POST contains binary data, sending multipart/form-data");
                        MultipartWriter writer = new MultipartWriter(req);
                        writer.write(params);
                    }
                }
            }
        });
    }

    /**
	 * Creates a string representing the current query string, or an empty string if there are no parameters. Will not work if there are binary
	 * attachments!
	 */
    protected String createQueryString() {
        assert !this.hasBinaryAttachments;
        if (this.params.isEmpty()) return "";
        StringBuilder bld = null;
        for (Map.Entry<String, Object> param : this.params.entrySet()) {
            if (bld == null) bld = new StringBuilder(); else bld.append('&');
            bld.append(StringUtils.urlEncode(param.getKey()));
            bld.append('=');
            bld.append(StringUtils.urlEncode(param.getValue().toString()));
        }
        return bld.toString();
    }
}
