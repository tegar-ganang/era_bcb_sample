package edu.ucdavis.cs.dblp.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import edu.ucdavis.cs.dblp.data.Publication;

/**
 * 
 * @author pfishero
 * @version $Id$
 */
public class WebResourceFetcher implements ResourceFetcher {

    public static final Logger logger = Logger.getLogger(WebResourceFetcher.class);

    private final HttpClient client;

    public WebResourceFetcher() {
        client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
        client.getParams().setParameter("http.socket.timeout", 10000);
    }

    @Override
    public String fetchElectronicEdition(Publication pub) {
        final String url = pub.getEe();
        HttpMethod method = null;
        String responseBody = "";
        method = new GetMethod(url);
        method.setFollowRedirects(true);
        try {
            if (StringUtils.isNotBlank(method.getURI().getScheme())) {
                InputStream is = null;
                StringWriter writer = new StringWriter();
                try {
                    client.executeMethod(method);
                    Header contentType = method.getResponseHeader("Content-Type");
                    if (contentType != null && StringUtils.isNotBlank(contentType.getValue()) && contentType.getValue().indexOf("text/html") >= 0) {
                        is = method.getResponseBodyAsStream();
                        IOUtils.copy(is, writer);
                        responseBody = writer.toString();
                    } else {
                        logger.info("ignoring non-text/html response from page: " + url + " content-type:" + contentType);
                    }
                } catch (HttpException he) {
                    logger.error("Http error connecting to '" + url + "'");
                    logger.error(he.getMessage());
                } catch (IOException ioe) {
                    logger.error("Unable to connect to '" + url + "'");
                } finally {
                    IOUtils.closeQuietly(is);
                    IOUtils.closeQuietly(writer);
                }
            }
        } catch (URIException e) {
            logger.error(e);
        } finally {
            method.releaseConnection();
        }
        return responseBody;
    }
}
