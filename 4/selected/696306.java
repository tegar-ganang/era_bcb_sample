package net.disy.legato.tools.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.io.IOUtils;

public class ProxyServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final List<String> allowedUrls = new LinkedList<String>();

    @Override
    public void init() throws ServletException {
        String allowedUrlsParameter = getServletConfig().getInitParameter("allowedUrls");
        if (allowedUrlsParameter != null) {
            String[] urls = allowedUrlsParameter.split(",");
            for (String url : urls) {
                String trimmedUrl = url.trim();
                if (isValidUrl(trimmedUrl)) {
                    allowedUrls.add(trimmedUrl);
                }
            }
        }
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        final String url = req.getParameter("url");
        if (!isAllowed(url)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        final HttpClient client = new HttpClient();
        final GetMethod method = new GetMethod(url);
        method.setFollowRedirects(true);
        try {
            final int statusCode = client.executeMethod(method);
            if (statusCode != -1) {
                final String contents = method.getResponseBodyAsString();
                resp.getOutputStream().print(contents);
            }
        } finally {
            method.releaseConnection();
        }
    }

    private boolean isValidUrl(String urlString) {
        try {
            URI uri = new URI(urlString);
            if (uri.getHost() == null || "".equals(uri.getHost())) {
                return false;
            }
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private boolean isAllowed(String urlString) {
        try {
            URI uri = new URI(urlString);
            String startUrl = uri.normalize().toString();
            for (String allowedPrefix : allowedUrls) {
                if (startUrl.startsWith(allowedPrefix)) {
                    return true;
                }
            }
            return false;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        final String url = req.getParameter("url");
        if (!isAllowed(url)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        final HttpClient client = new HttpClient();
        client.getParams().setVersion(HttpVersion.HTTP_1_0);
        final PostMethod method = new PostMethod(url);
        method.getParams().setVersion(HttpVersion.HTTP_1_0);
        method.setFollowRedirects(false);
        final RequestEntity entity = new InputStreamRequestEntity(req.getInputStream());
        method.setRequestEntity(entity);
        try {
            final int statusCode = client.executeMethod(method);
            if (statusCode != -1) {
                InputStream is = null;
                ServletOutputStream os = null;
                try {
                    is = method.getResponseBodyAsStream();
                    try {
                        os = resp.getOutputStream();
                        IOUtils.copy(is, os);
                    } finally {
                        if (os != null) {
                            try {
                                os.flush();
                            } catch (IOException ignored) {
                            }
                        }
                    }
                } catch (IOException ioex) {
                    final String message = ioex.getMessage();
                    if (!"chunked stream ended unexpectedly".equals(message)) {
                        throw ioex;
                    }
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        } finally {
            method.releaseConnection();
        }
    }
}
