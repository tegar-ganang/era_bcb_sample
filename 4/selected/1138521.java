package org.kenict.repository.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map.Entry;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author martin
 *
 */
public class GetServlet extends HttpServlet {

    private static final String FEDORA_URL = "fedora-url";

    private static final long serialVersionUID = 1L;

    private String fedoraUrl;

    private final Logger logger = LoggerFactory.getLogger(GetServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String rewrittenQueryString = URLDecoder.decode(request.getRequestURI(), "UTF-8").replaceFirst("^.*?\\/(id:.*)\\/.*?$", "$1");
        logger.debug("rewrittenQueryString: " + rewrittenQueryString);
        URL rewrittenUrl = new URL(fedoraUrl + rewrittenQueryString);
        logger.debug("rewrittenUrl: " + rewrittenUrl.getProtocol() + "://" + rewrittenUrl.getHost() + ":" + rewrittenUrl.getPort() + rewrittenUrl.getFile());
        HttpURLConnection httpURLConnection = (HttpURLConnection) rewrittenUrl.openConnection();
        HttpURLConnection.setFollowRedirects(false);
        httpURLConnection.connect();
        response.setStatus(httpURLConnection.getResponseCode());
        logger.debug("[status=" + httpURLConnection.getResponseCode() + "]");
        logger.debug("[headers]");
        for (Entry<String, List<String>> header : httpURLConnection.getHeaderFields().entrySet()) {
            if (header.getKey() != null) {
                for (String value : header.getValue()) {
                    if (value != null) {
                        logger.debug(header.getKey() + ": " + value);
                        if (!header.getKey().equals("Server") && !header.getKey().equals("Transfer-Encoding")) {
                            response.addHeader(header.getKey(), value);
                        }
                    }
                }
            }
        }
        logger.debug("[/headers]");
        InputStream inputStream = httpURLConnection.getInputStream();
        OutputStream outputStream = response.getOutputStream();
        IOUtils.copy(inputStream, outputStream);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        fedoraUrl = (String) getServletContext().getAttribute(FEDORA_URL);
        if (fedoraUrl == null) {
            throw new IllegalStateException("Fedora url not set");
        }
    }
}
