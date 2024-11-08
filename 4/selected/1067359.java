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
 * Fetches content from the locally downloaded cache of web resources.
 * 
 * @author pfishero
 */
public class LocalCacheResourceFetcher implements ResourceFetcher {

    private static final Logger logger = Logger.getLogger(LocalCacheResourceFetcher.class);

    private String baseURL;

    private final HttpClient client;

    public LocalCacheResourceFetcher() {
        client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
        client.getParams().setParameter("http.socket.timeout", 10000);
    }

    /**
	 * @return the baseURL
	 */
    public String getBaseURL() {
        return baseURL;
    }

    /**
	 * @param baseURL the baseURL to set
	 */
    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    @Override
    public String fetchElectronicEdition(Publication pub) {
        final String url = baseURL + pub.getKey() + ".html";
        logger.info("fetching pub ee from local cache at: " + url);
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
