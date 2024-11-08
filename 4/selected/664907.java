package net.sf.s34j.rest.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.TreeMap;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import net.sf.practicalxml.OutputUtil;
import net.sf.practicalxml.ParseUtil;
import net.sf.s34j.S3TransportException;
import net.sf.s34j.defines.S3CannedAcl;
import net.sf.s34j.rest.S3Factory;

/**
 *  Manages a single S3 request. Requests are constructed around an HTTP
 *  operation and optional bucket and key names, modified using a builder
 *  approach, and then executed. When a query is executed, its results
 *  (if any) are immediately processed, depending on response content
 *  type, as a String, XML <code>Document</code>, or <code>byte[]</code>.
 */
public class S3Request {

    private static ThreadLocal<DateFormat> _dateFormat = new ThreadLocal<DateFormat>();

    private Log _logger = LogFactory.getLog(this.getClass());

    private S3Factory _factory;

    private HttpRequestMethod _method;

    private String _bucket;

    private String _key;

    private TreeMap<String, String> _params = new TreeMap<String, String>();

    private TreeMap<String, String> _amazonHeaders = new TreeMap<String, String>();

    private RequestEntity _content;

    private String _contentType = "";

    private String _contentMD5 = "";

    private String _timestamp;

    private int _status;

    private OutputStream _out;

    private Object _result;

    private String _resultContentType;

    public S3Request(S3Factory factory, HttpRequestMethod method, String bucket, String key) {
        _factory = factory;
        _method = method;
        _bucket = bucket;
        _key = urlEncode(key);
    }

    /**
     *  Sets an optional parameter on the request URL: the parameter is only
     *  set if the passed value is not <code>null</code>.
     */
    public S3Request setOptionalParameter(String name, Object value) {
        if (value != null) _params.put(name, String.valueOf(value));
        return this;
    }

    /**
     *  Sets a "canned" ACL on this request, overwriting any previous ACL.
     *  Only valid for PUT requests (but we don't throw if you call for a
     *  different request). May be called with <code>null</code>, clearing
     *  any prior ACL setting.
     */
    public S3Request setCannedAcl(S3CannedAcl acl) {
        if (acl == null) _amazonHeaders.remove(S3Headers.X_AMZ_ACL); else _amazonHeaders.put(S3Headers.X_AMZ_ACL, acl.toString());
        return this;
    }

    /**
     *  Attaches body content to the request. This may be called only once for
     *  a single request; if called multiple times, subsequent calls overwrite
     *  former.
     */
    public S3Request setContent(byte[] data, String contentType) {
        _content = new ByteArrayRequestEntity(data, contentType);
        _contentType = contentType;
        return this;
    }

    /**
     *  Attaches body content to the request. This may be called only once for
     *  a single request; if called multiple times, subsequent calls overwrite
     *  former.
     *  <p>
     *  <strong>Warning:</strong>
     *  The entire stream will be buffered prior to the request being sent;
     *  Amazon does not support chunked content. If you have a large stream,
     *  you should manually buffer it as a file.
     */
    public S3Request setContent(InputStream data, String contentType) {
        _content = new InputStreamRequestEntity(data, contentType);
        _contentType = contentType;
        return this;
    }

    /**
     *  Attaches body content to the request. This may be called only once for
     *  a single request; if called multiple times, subsequent calls overwrite
     *  former.
     */
    public S3Request setContent(File file, String contentType) {
        _content = new FileRequestEntity(file, contentType);
        _contentType = contentType;
        return this;
    }

    /**
     *  Sets an output stream for the request content. Content will be written
     *  to this stream without any interpretation, and subsequent calls to
     *  {@link #getResult} will return <code>null</code>.
     *  <p>
     *  This stream will remain open after executing the request.
     */
    public S3Request setOutputStream(OutputStream out) {
        _out = out;
        return this;
    }

    /**
     *  Returns the URL that would be used for this request.
     *  <p>
     *  Note: this method reconstructs the URL on each call; it does not cache
     *        the results.
     */
    public String getUrl() {
        String url = "http://";
        if (_bucket != null) url += _bucket + ".";
        url += _factory.getEndpoint() + "/";
        if (_key != null) url += _key;
        return addParameters(url);
    }

    /**
     *  Executes this request and processes the body (if any).
     *
     *  @return The HTTP status code for this request execution.
     */
    protected int execute() {
        setTimestamp();
        HttpMethod request = buildRequest();
        try {
            _status = _factory.getHttpClient().executeMethod(request);
            processBody(request);
            optionallyLog();
            return _status;
        } catch (IOException ee) {
            throw new S3TransportException(ee);
        } finally {
            request.releaseConnection();
        }
    }

    /**
     *  Returns the status code for an executed request. Will return 0 if the
     *  request has not yet been executed. This method is not normally called;
     *  instead using the value returned by {@link #execute}.
     */
    public int getStatus() {
        return _status;
    }

    /**
     *  Returns the result object. The caller is responsible for casting as
     *  appropriate.
     */
    public Object getResult() {
        return _result;
    }

    /**
     *  Returns the <code>Content-Type</code> value (if any) from the response.
     *  If the header was not set in the response, returns an empty string; if
     *  the request has not yet been executed, returns <code>null</code>.
     */
    public String getResponseContentType() {
        return _resultContentType;
    }

    private void setTimestamp() {
        DateFormat format = _dateFormat.get();
        if (format == null) {
            format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZZ");
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            _dateFormat.set(format);
        }
        _timestamp = format.format(new Date());
    }

    private HttpMethod buildRequest() {
        HttpMethod request = null;
        switch(_method) {
            case GET:
                request = new GetMethod(getUrl());
                break;
            case PUT:
                request = new PutMethod(getUrl());
                if (_content != null) {
                    PutMethod put = (PutMethod) request;
                    put.setRequestEntity(_content);
                }
                break;
            case DELETE:
                request = new DeleteMethod(getUrl());
                break;
            default:
                throw new UnsupportedOperationException("no requests use " + _method);
        }
        request.setRequestHeader(HttpHeaders.DATE, _timestamp);
        request.setRequestHeader(HttpHeaders.AUTHORIZATION, buildAWSAuthString(buildStringToSign()));
        for (String header : _amazonHeaders.keySet()) {
            request.setRequestHeader(header, _amazonHeaders.get(header));
        }
        return request;
    }

    private String addParameters(String url) {
        if (_params.size() == 0) return url;
        url += "?";
        Iterator<String> parmItx = _params.keySet().iterator();
        while (parmItx.hasNext()) {
            String name = parmItx.next();
            String value = _params.get(name);
            try {
                name = URLEncoder.encode(name, "UTF-8");
                value = URLEncoder.encode(value, "UTF-8");
            } catch (UnsupportedEncodingException ee) {
                throw new S3TransportException("JDK doesn't know UTF-8!", ee);
            }
            url += name + "=" + value;
            if (parmItx.hasNext()) url += "&";
        }
        return url;
    }

    private String buildStringToSign() {
        return _method + "\n" + _contentMD5 + "\n" + _contentType + "\n" + _timestamp + "\n" + buildAuthAmzHeaders() + buildAuthResource();
    }

    private String buildAuthAmzHeaders() {
        String result = "";
        for (String header : _amazonHeaders.keySet()) {
            result += header + ":" + _amazonHeaders.get(header) + "\n";
        }
        return result;
    }

    private String buildAuthResource() {
        if (_bucket == null) return "/";
        String resource = "/" + _bucket + "/";
        if (_key != null) resource += _key;
        return resource;
    }

    private String buildAWSAuthString(String stringToSign) {
        return "AWS " + _factory.getPublicKey() + ":" + _factory.signAndEncode(stringToSign);
    }

    private void processBody(HttpMethod request) throws IOException {
        InputStream in = request.getResponseBodyAsStream();
        if (in == null) return;
        Header contentTypeHdr = request.getResponseHeader(HttpHeaders.CONTENT_TYPE);
        _resultContentType = (contentTypeHdr != null) ? contentTypeHdr.getValue() : "";
        if (_out != null) {
            IOUtils.copy(in, _out);
            _out.flush();
        } else if (_resultContentType.startsWith(MimeTypes.TEXT)) {
            _result = IOUtils.toString(in, "UTF-8");
        } else if (_resultContentType.startsWith(MimeTypes.XML) || _resultContentType.startsWith(MimeTypes.DEPRECATED_XML)) {
            _result = ParseUtil.parse(new InputSource(in));
        } else {
            _result = IOUtils.toByteArray(in);
        }
    }

    private void optionallyLog() {
        if (!_logger.isDebugEnabled()) return;
        String logMsg = _method + " " + _bucket + "/" + _key + " = " + _status;
        if ((_status / 100 != 2) && (_result instanceof Document)) logMsg += "\n" + OutputUtil.indentedString((Document) _result, 4);
        _logger.debug(logMsg);
    }

    /**
     *  A wrapper around the JDK's URLEncoder that (1) replaces the checked
     *  exception (which should never happen) with a runtime exception, and
     *  (2) encodes spaces as "%20", not "+".
     */
    public static String urlEncode(String src) {
        if (src == null) return "";
        try {
            String encoded = URLEncoder.encode(src, "UTF-8");
            if (encoded.indexOf('+') >= 0) encoded = encoded.replace((CharSequence) "+", (CharSequence) "%20");
            return encoded;
        } catch (UnsupportedEncodingException ee) {
            throw new RuntimeException("this JVM doesn't support UTF-8!", ee);
        }
    }
}
