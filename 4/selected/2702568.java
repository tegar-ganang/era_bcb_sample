package org.translationcomponent.api.impl.document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;
import org.translationcomponent.api.Document;
import org.translationcomponent.api.Parameter;
import org.translationcomponent.api.RequestHeaderAccess;
import org.translationcomponent.api.ResponseCode;
import org.translationcomponent.api.ResponseHeader;
import org.translationcomponent.api.ResponseState;
import org.translationcomponent.api.impl.response.ResponseHeaderImpl;
import org.translationcomponent.api.impl.response.ResponseStateBean;
import org.translationcomponent.api.impl.response.ResponseStateNotModified;
import org.translationcomponent.api.impl.response.ResponseStateOk;

public abstract class HttpDocument implements Document {

    private final String url;

    private Set<Parameter> parameters;

    private final String requestedCharacterEncoding;

    private boolean methodExecuted = false;

    private HttpMethodBase method;

    private ResponseState status;

    private CloseableInputStreamDecorator input;

    private List<String> forwardRequestHeaders;

    private List<String> forwardResponseHeaders;

    private final String userAgent;

    private final HttpClient httpClient;

    private RequestHeaderAccess availableHeaders;

    private String completeUrl;

    public HttpDocument(final HttpClient httpClient, final String url, final Set<Parameter> parameters, final String requestedCharacterEncoding, final String userAgent) {
        super();
        this.httpClient = httpClient;
        this.url = url;
        this.requestedCharacterEncoding = requestedCharacterEncoding;
        this.userAgent = userAgent;
        this.parameters = parameters;
    }

    public String toString() {
        return url;
    }

    protected void setMethod(final HttpMethodBase m) {
        this.method = m;
    }

    protected HttpMethodBase getMethod() {
        return this.method;
    }

    /**
	 * Executes the get.
	 * 
	 * Throws an exception on a serious I/O error.
	 * 
	 * Updates the status on regular errors like page not found.
	 * 
	 */
    public void open() throws IOException {
        createMethod();
        int code = httpClient.executeMethod(method);
        methodExecuted = true;
        switch(code) {
            case HttpServletResponse.SC_UNAUTHORIZED:
                status = new ResponseStateBean(ResponseCode.SECURITYERROR, "http.unauthorized");
                break;
            case HttpServletResponse.SC_FORBIDDEN:
                status = new ResponseStateBean(ResponseCode.SECURITYERROR, "http.forbidden");
                break;
            case HttpServletResponse.SC_NOT_FOUND:
                status = new ResponseStateBean(ResponseCode.NOTFOUND, method.getStatusText());
                break;
            case HttpServletResponse.SC_NOT_MODIFIED:
                status = ResponseStateNotModified.getInstance();
                break;
            case HttpServletResponse.SC_OK:
                status = ResponseStateOk.getInstance();
                break;
            default:
                status = new ResponseStateBean(ResponseCode.ERROR, method.getStatusText());
        }
    }

    protected abstract void createMethod();

    protected void addRequestHeaders(final String contentType) {
        for (final String[] s : this.getRequestHeaders(contentType)) {
            method.addRequestHeader(s[0], s[1]);
        }
    }

    public Collection<String[]> getRequestHeaders(String contentType) {
        Collection<String[]> list = new ArrayList<String[]>(this.forwardRequestHeaders == null ? 1 : forwardRequestHeaders.size());
        String acceptCharSet = null;
        if (this.forwardRequestHeaders != null && this.availableHeaders != null) {
            for (String name : forwardRequestHeaders) {
                String value = this.availableHeaders.getHeaderValue(name);
                if (value != null) {
                    if (name.equals("Content-Type")) {
                        if (contentType == null) {
                            contentType = value;
                        }
                    } else if (name.equals("Accept-Charset")) {
                        if (acceptCharSet == null) {
                            acceptCharSet = value;
                        }
                    } else {
                        list.add(new String[] { name, value });
                    }
                }
            }
        }
        if (contentType == null) {
            contentType = "text/html; charset=" + this.getRequestedCharacterEncoding();
        } else {
            if (!contentType.contains("charset")) {
                contentType = new StringBuilder(contentType).append("; charset=").append(this.getRequestedCharacterEncoding()).toString();
            }
        }
        list.add(new String[] { "Content-Type", contentType });
        if (acceptCharSet == null) {
            acceptCharSet = this.getRequestedCharacterEncoding();
        }
        list.add(new String[] { "Accept-Charset", acceptCharSet });
        if (userAgent != null) {
            list.add(new String[] { "User-Agent", userAgent });
        }
        return list;
    }

    public Set<ResponseHeader> getResponseHeaders() {
        if (this.method == null || !this.methodExecuted) {
            return Collections.emptySet();
        }
        if (forwardResponseHeaders == null) {
            return Collections.emptySet();
        }
        String connectionHeaderValue = null;
        Header connectionHeader = method.getResponseHeader("Connection");
        if (connectionHeader != null) {
            connectionHeaderValue = connectionHeader.getValue().toLowerCase();
            if (connectionHeaderValue.equals("keep-alive") || connectionHeaderValue.equals("close")) connectionHeaderValue = null;
        }
        Set<ResponseHeader> set = new TreeSet<ResponseHeader>();
        for (String name : forwardResponseHeaders) {
            if (connectionHeaderValue == null || !connectionHeaderValue.contains(name.toLowerCase())) {
                Header[] headers = method.getResponseHeaders(name);
                if (headers != null && headers.length != 0) {
                    String[] values = new String[headers.length];
                    for (int ii = 0; ii < headers.length; ii++) {
                        values[ii] = headers[ii].getValue();
                    }
                    set.add(new ResponseHeaderImpl(name, values));
                }
            }
        }
        return Collections.unmodifiableSet(set);
    }

    /**
	 * Is always executed after an open.
	 * 
	 * So proper coding is:
	 * 
	 * try { open() } finally { close() };
	 * 
	 * Reads the response body to its end (this is a requirement).
	 * 
	 * Releases the connection used by getMethod.
	 */
    public void close() {
        if (method != null) {
            try {
                if (methodExecuted) {
                    getReader();
                    if (input != null && !input.isClosed()) {
                        try {
                            byte[] buffer = new byte[1024];
                            while (input.read(buffer) == buffer.length) {
                            }
                        } catch (Exception e) {
                            LogFactory.getLog(this.getClass()).warn("Cannot read rest of responsebody from HttpClient. Url=" + url, e);
                        } finally {
                            try {
                                input.close();
                            } catch (Exception e) {
                                LogFactory.getLog(this.getClass()).warn("Cannot close reader from HttpClient. Url=" + url, e);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LogFactory.getLog(this.getClass()).warn("Error retrieving reader in attempting to close the request. Url=" + url, e);
            } finally {
                method.releaseConnection();
                method = null;
            }
        }
    }

    public Reader getReader() throws IOException {
        getInputStream();
        if (input == null) {
            return null;
        } else {
            return new CloseableReaderDecorator(new InputStreamReader(input, getCharacterEncoding()));
        }
    }

    public String getCharacterEncoding() throws IOException {
        if (methodExecuted && method != null) {
            return method.getResponseCharSet();
        }
        return null;
    }

    public InputStream getInputStream() throws IOException {
        if (input != null) {
            return input;
        }
        if (methodExecuted && method != null) {
            InputStream inputResponse = method.getResponseBodyAsStream();
            if (inputResponse != null) {
                input = new CloseableInputStreamDecorator(inputResponse);
            }
        }
        return input;
    }

    public ResponseState getStatus() {
        return status;
    }

    protected String getUrl() {
        return url;
    }

    protected String getRequestedCharacterEncoding() {
        return requestedCharacterEncoding;
    }

    public void addParameters(Set<Parameter> parameters) {
        if (this.parameters == null) {
            this.parameters = parameters;
        } else {
            for (Parameter p : parameters) {
                if (this.parameters.contains(p)) {
                    this.parameters.remove(p);
                }
                this.parameters.add(p);
            }
        }
    }

    public Set<Parameter> getParameters() {
        return parameters;
    }

    public void setForwardRequestHeaders(final List<String> headers) {
        this.forwardRequestHeaders = headers;
    }

    public void setForwardResponseHeaders(List<String> forwardHeaders) {
        this.forwardResponseHeaders = forwardHeaders;
    }

    public void setAvailableHeaders(RequestHeaderAccess availableHeaders) {
        this.availableHeaders = availableHeaders;
    }

    public String getText() throws IOException {
        BufferedReader reader = new BufferedReader(getReader());
        try {
            StringWriter writer = new StringWriter();
            IOUtils.copy(reader, writer);
            return writer.toString();
        } finally {
            reader.close();
        }
    }

    protected String getCookieHeaderValue(String name) {
        if (methodExecuted && method != null) {
            try {
                Header[] cookies = method.getResponseHeaders("Set-Cookie");
                name = new StringBuilder(name.length() + 2).append(' ').append(name).append('=').toString();
                for (int ii = 0; ii < cookies.length; ii++) {
                    int pos = -1;
                    if ((pos = cookies[ii].getValue().indexOf(name)) != -1) {
                        pos += name.length();
                        int endPos = cookies[ii].getValue().indexOf(pos, ';');
                        if (endPos != -1) {
                            return URLDecoder.decode(cookies[ii].getValue().substring(pos, endPos), method.getResponseCharSet());
                        }
                    }
                }
            } catch (UnsupportedEncodingException e) {
                LogFactory.getLog(this.getClass()).error(this.url, e);
            }
        }
        return null;
    }

    public String getSystemId() {
        if (completeUrl == null) {
            return url;
        }
        return completeUrl;
    }

    public String getCompleteUrl() {
        return completeUrl;
    }

    public void setCompleteUrl(String completeUrl) {
        this.completeUrl = completeUrl;
    }
}
