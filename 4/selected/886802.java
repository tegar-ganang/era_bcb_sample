package net.webassembletool.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import net.webassembletool.ResourceContext;
import net.webassembletool.authentication.AuthenticationHandler;
import net.webassembletool.cache.Rfc2616;
import net.webassembletool.filter.Filter;
import net.webassembletool.output.Output;
import net.webassembletool.resource.Resource;
import net.webassembletool.resource.ResourceUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;

/**
 * Resource implementation pointing to a resource on an external application.
 * 
 * @author Francois-Xavier Bonnet
 * @author Nicolas Richeton
 */
public class HttpResource extends Resource {

    private static final Log LOG = LogFactory.getLog(HttpResource.class);

    private HttpClientResponse httpClientResponse;

    private final ResourceContext target;

    private final String url;

    public HttpResource(HttpClient httpClient, ResourceContext resourceContext, Map<String, String> validators) throws IOException {
        this.target = resourceContext;
        this.url = ResourceUtils.getHttpUrlWithQueryString(resourceContext);
        HttpContext httpContext = null;
        if (resourceContext.getUserContext() != null) {
            httpContext = resourceContext.getUserContext().getHttpContext();
        }
        HttpServletRequest originalRequest = resourceContext.getOriginalRequest();
        AuthenticationHandler authenticationHandler = resourceContext.getDriver().getAuthenticationHandler();
        boolean proxy = resourceContext.isProxy();
        boolean preserveHost = resourceContext.isPreserveHost();
        HttpClientRequest httpClientRequest = new HttpClientRequest(url, originalRequest, proxy, preserveHost);
        if (validators != null) {
            for (Entry<String, String> header : validators.entrySet()) {
                LOG.debug("Adding validator: " + header.getKey() + ": " + header.getValue());
                httpClientRequest.addHeader(header.getKey(), header.getValue());
            }
        }
        authenticationHandler.preRequest(httpClientRequest, resourceContext);
        Filter filter = resourceContext.getDriver().getFilter();
        if (filter != null) {
            filter.preRequest(httpClientRequest, resourceContext);
        }
        httpClientResponse = httpClientRequest.execute(httpClient, httpContext);
        while (authenticationHandler.needsNewRequest(httpClientResponse, resourceContext)) {
            httpClientResponse.finish();
            httpClientRequest = new HttpClientRequest(url, originalRequest, proxy, preserveHost);
            authenticationHandler.preRequest(httpClientRequest, resourceContext);
            if (filter != null) {
                filter.preRequest(httpClientRequest, resourceContext);
            }
            httpClientResponse = httpClientRequest.execute(httpClient, httpContext);
        }
        if (isError()) {
            LOG.warn("Problem retrieving URL: " + url + ": " + httpClientResponse.getStatusCode() + " " + httpClientResponse.getStatusText());
        }
    }

    @Override
    public void render(Output output) throws IOException {
        output.setStatus(httpClientResponse.getStatusCode(), httpClientResponse.getStatusText());
        Rfc2616.copyHeaders(this, output);
        Filter filter = target.getDriver().getFilter();
        if (filter != null) {
            filter.postRequest(httpClientResponse, output, target);
        }
        String location = httpClientResponse.getHeader("Location");
        if (location != null) {
            location = rewriteLocation(location);
            location = removeSessionId(location);
            output.addHeader("Location", location);
        }
        String charset = httpClientResponse.getContentCharset();
        if (charset != null) {
            output.setCharsetName(charset);
        }
        try {
            output.open();
            if (httpClientResponse.getException() != null) {
                output.write(httpClientResponse.getStatusText());
            } else {
                removeSessionId(httpClientResponse.openStream(), output);
            }
        } finally {
            output.close();
        }
    }

    private String rewriteLocation(String location) {
        HttpServletRequest request = target.getOriginalRequest();
        String originalBase = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + request.getServletPath();
        if (request.getPathInfo() != null) {
            originalBase += request.getPathInfo();
        }
        int pos = originalBase.lastIndexOf(target.getRelUrl());
        String driverBaseUrl = target.getDriver().getBaseURL();
        if (pos >= 0) {
            originalBase = originalBase.substring(0, pos);
            if (originalBase.charAt(originalBase.length() - 1) != '/' && driverBaseUrl.charAt(driverBaseUrl.length() - 1) == '/') {
                originalBase += "/";
            }
        }
        return location.replaceFirst(driverBaseUrl, originalBase);
    }

    private void removeSessionId(InputStream inputStream, Output output) throws IOException {
        String jsessionid = RewriteUtils.getSessionId(target);
        boolean textContentType = ResourceUtils.isTextContentType(httpClientResponse.getHeader("Content-Type"));
        if (jsessionid == null || !textContentType) {
            IOUtils.copy(inputStream, output.getOutputStream());
        } else {
            String charset = httpClientResponse.getContentCharset();
            if (charset == null) {
                charset = "ISO-8859-1";
            }
            String content = IOUtils.toString(inputStream, charset);
            content = removeSessionId(jsessionid, content);
            if (output.getHeader("Content-length") != null) {
                output.setHeader("Content-length", Integer.toString(content.length()));
            }
            OutputStream outputStream = output.getOutputStream();
            IOUtils.write(content, outputStream, charset);
        }
        inputStream.close();
    }

    private String removeSessionId(String src) {
        String sessionId = RewriteUtils.getSessionId(target);
        return removeSessionId(sessionId, src);
    }

    private String removeSessionId(String sessionId, String src) {
        if (sessionId == null) {
            return src;
        } else {
            return RewriteUtils.removeSessionId(sessionId, src);
        }
    }

    @Override
    public void release() {
        httpClientResponse.finish();
    }

    @Override
    public int getStatusCode() {
        return httpClientResponse.getStatusCode();
    }

    /**
	 * @see java.lang.Object#toString()
	 */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(target.getOriginalRequest().getMethod());
        result.append(" ");
        result.append(ResourceUtils.getHttpUrlWithQueryString(target));
        result.append("\n");
        if (target.getUserContext() != null) {
            result.append(target.getUserContext().toString());
        }
        return result.toString();
    }

    @Override
    public String getHeader(String name) {
        return httpClientResponse.getHeader(name);
    }
}
