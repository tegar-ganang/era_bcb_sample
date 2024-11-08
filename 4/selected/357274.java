package proxyservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import management.impl.ProxyClientPartStatistics;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import com.sun.net.httpserver.HttpExchange;
import configuration.ConfigurationSingleton;
import converter.Converter;
import converter.image.ImageConverter;
import converter.pdf.PdfConverter;
import converter.poi.DocConverter;
import converter.poi.PptConverter;
import converter.poi.VsdConverter;
import converter.poi.XlsConverter;

/**
 * Provides the part of the proxy-server that fetches and converts the web-content.
 * @author Peter Falkensteiner
 */
public class ProxyClientPart extends Thread {

    /** Converter instances */
    private static final List<Converter> converters = Arrays.asList((Converter) new ImageConverter(), (Converter) new PdfConverter(), (Converter) new DocConverter(), (Converter) new PptConverter(), (Converter) new VsdConverter(), (Converter) new XlsConverter());

    /** HTTP-Client from the Apache "HTTPClient"-Framework */
    private HttpClient httpclient = new DefaultHttpClient();

    /** URI of the HTTP-content to fetch */
    private URI uri;

    /** HTTP-Exchange-conversation from the server-part of the proxy */
    private HttpExchange exchange;

    /** Java-Logging handler. */
    private static Logger logger = Logger.getLogger("proxyservice.ProxyClientPart");

    /** MBean-Statistics */
    private static final ProxyClientPartStatistics statistics = new ProxyClientPartStatistics();

    /**
	 * Create the URI for the content that has to be fetched.
	 * @param host HTTP-Host requested
	 * @param path HTTP-"GET"-Path
	 * @param param HTTP-Parameters
	 * @param exchange HTTP-Exchange-conversation from the server-part
	 * @throws URISyntaxException
	 */
    public ProxyClientPart(String host, String path, String param, HttpExchange exchange) throws URISyntaxException {
        this.exchange = exchange;
        uri = URIUtils.createURI("http", host, -1, path, param, null);
    }

    /**
	 * Fetch the content from the external HTTP-Server.
	 * If the content is applicable for mitigation-strategies, the 
	 * conversion- or replacement-algorithms are executed. 
	 */
    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        HttpGet httpget = new HttpGet(uri);
        HttpResponse httpResp;
        try {
            httpResp = httpclient.execute(httpget);
        } catch (ClientProtocolException e) {
            if (logger.isLoggable(Level.WARNING)) logger.warning(e.getMessage());
            statistics.incrementProcessingErrors();
            return;
        } catch (IOException e) {
            if (logger.isLoggable(Level.WARNING)) logger.warning(e.getMessage());
            statistics.incrementProcessingErrors();
            return;
        }
        HttpEntity entity = httpResp.getEntity();
        try {
            if (convertContent(entity)) {
                long processingTime = System.currentTimeMillis() - startTime;
                statistics.incrementProcessedRequests();
                statistics.incrementTotalProcessingTime(processingTime);
                return;
            }
        } catch (IOException e) {
            if (logger.isLoggable(Level.WARNING)) logger.warning(e.getMessage());
            statistics.incrementProcessingErrors();
            return;
        }
        try {
            for (Header header : httpResp.getAllHeaders()) exchange.getResponseHeaders().add(header.getName(), header.getValue());
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
        } catch (IOException e) {
            if (logger.isLoggable(Level.WARNING)) logger.warning(e.getMessage());
            statistics.incrementProcessingErrors();
            return;
        }
        try {
            passThroughContent(entity);
        } catch (IOException e) {
            if (logger.isLoggable(Level.WARNING)) logger.warning(ConfigurationSingleton.getInstance().getConfiguration().getString("proxyservice.ProxyClientPart.ClientSendFailed") + "\n" + e.getMessage());
            statistics.incrementProcessingErrors();
            return;
        }
        long processingTime = System.currentTimeMillis() - startTime;
        statistics.incrementProcessedRequests();
        statistics.incrementTotalProcessingTime(processingTime);
    }

    /**
	 * Passes through the content without conversion.
	 * @param entity the fetched HTTP-content
	 * @throws IOException
	 */
    private void passThroughContent(HttpEntity entity) throws IOException {
        int c = 0;
        InputStream content = entity.getContent();
        OutputStream responseBody = exchange.getResponseBody();
        while ((c = content.read()) != -1) responseBody.write(c);
        responseBody.close();
    }

    /**
	 * Converts the given Content (if there is a converter for the content-type)
	 * and passes it to the web-client.
	 * @param entity the fetched HTTP-content
	 * @param startTime the time the request-handling started
	 * @throws IOException 
	 */
    private boolean convertContent(HttpEntity entity) throws IOException {
        String contentType = entity.getContentType().getValue();
        for (Converter converter : converters) {
            if (converter.isApplicable(contentType)) {
                exchange.getResponseHeaders().set("Content", converter.getTargetFormat(contentType));
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                OutputStream responseBody = exchange.getResponseBody();
                converter.convert(contentType, entity.getContent(), responseBody);
                responseBody.close();
                return true;
            }
        }
        return false;
    }
}
