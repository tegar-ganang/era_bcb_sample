package com.noelios.restlet.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import org.restlet.data.Encoding;
import org.restlet.data.Language;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.data.Tag;
import org.restlet.resource.InputRepresentation;
import org.restlet.resource.ReadableRepresentation;
import org.restlet.resource.Representation;
import org.restlet.service.ConnectorService;
import com.noelios.restlet.util.HeaderReader;

/**
 * Low-level HTTP client call.
 * 
 * @author Jerome Louvel (contact@noelios.com)
 */
public class HttpClientCall extends HttpCall {

    /** The parent HTTP client helper. */
    private HttpClientHelper helper;

    /**
     * Constructor setting the request address to the local host.
     * 
     * @param helper
     *            The parent HTTP client helper.
     * @param method
     *            The method name.
     * @param requestUri
     *            The request URI.
     */
    public HttpClientCall(HttpClientHelper helper, String method, String requestUri) {
        setLogger(helper.getLogger());
        this.helper = helper;
        setMethod(method);
        setRequestUri(requestUri);
        setClientAddress(getLocalAddress());
    }

    /**
     * Returns the HTTP client helper.
     * 
     * @return The HTTP client helper.
     */
    public HttpClientHelper getHelper() {
        return this.helper;
    }

    /**
     * Returns the local IP address or 127.0.0.1 if the resolution fails.
     * 
     * @return The local IP address or 127.0.0.1 if the resolution fails.
     */
    public static String getLocalAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    /**
     * Returns the request entity channel if it exists.
     * 
     * @return The request entity channel if it exists.
     */
    public WritableByteChannel getRequestChannel() {
        return null;
    }

    /**
     * Returns the request entity stream if it exists.
     * 
     * @return The request entity stream if it exists.
     */
    public OutputStream getRequestStream() {
        return null;
    }

    /**
     * Sends the request to the client. Commits the request line, headers and
     * optional entity and send them over the network.
     * 
     * @param request
     *            The high-level request.
     */
    public Status sendRequest(Request request) {
        Status result = null;
        try {
            Representation entity = request.isEntityAvailable() ? request.getEntity() : null;
            if (entity != null) {
                ConnectorService connectorService = getConnectorService(request);
                if (connectorService != null) connectorService.beforeSend(entity);
                OutputStream rs = getRequestStream();
                WritableByteChannel wbc = getRequestChannel();
                if (wbc != null) {
                    if (entity != null) {
                        entity.write(wbc);
                    }
                } else if (rs != null) {
                    if (entity != null) {
                        entity.write(rs);
                    }
                    rs.flush();
                }
                if (connectorService != null) connectorService.afterSend(entity);
                if (rs != null) {
                    rs.close();
                } else if (wbc != null) {
                    wbc.close();
                }
            }
            result = new Status(getStatusCode(), null, getReasonPhrase(), null);
        } catch (IOException ioe) {
            getHelper().getLogger().log(Level.FINE, "An error occured during the communication with the remote HTTP server.", ioe);
            result = new Status(Status.CONNECTOR_ERROR_COMMUNICATION, "Unable to complete the HTTP call due to a communication error with the remote server. " + ioe.getMessage());
        }
        return result;
    }

    /**
     * Returns the response channel if it exists.
     * 
     * @return The response channel if it exists.
     */
    public ReadableByteChannel getResponseChannel() {
        return null;
    }

    /**
     * Returns the response stream if it exists.
     * 
     * @return The response stream if it exists.
     */
    public InputStream getResponseStream() {
        return null;
    }

    /**
     * Returns the response entity if available. Note that no metadata is
     * associated by default, you have to manually set them from your headers.
     * 
     * @return The response entity if available.
     */
    public Representation getResponseEntity() {
        Representation result = null;
        if (getResponseStream() != null) {
            result = new InputRepresentation(getResponseStream(), null);
        } else if (getResponseChannel() != null) {
            result = new ReadableRepresentation(getResponseChannel(), null);
        } else if (getMethod().equals(Method.HEAD.getName())) {
            result = new Representation() {

                @Override
                public ReadableByteChannel getChannel() throws IOException {
                    return null;
                }

                @Override
                public InputStream getStream() throws IOException {
                    return null;
                }

                @Override
                public void write(OutputStream outputStream) throws IOException {
                }

                @Override
                public void write(WritableByteChannel writableChannel) throws IOException {
                }
            };
        }
        if (result != null) {
            for (Parameter header : getResponseHeaders()) {
                if (header.getName().equalsIgnoreCase(HttpConstants.HEADER_CONTENT_TYPE)) {
                    ContentType contentType = new ContentType(header.getValue());
                    if (contentType != null) {
                        result.setMediaType(contentType.getMediaType());
                        result.setCharacterSet(contentType.getCharacterSet());
                    }
                } else if (header.getName().equalsIgnoreCase(HttpConstants.HEADER_CONTENT_LENGTH)) {
                    result.setSize(Long.parseLong(header.getValue()));
                } else if (header.getName().equalsIgnoreCase(HttpConstants.HEADER_EXPIRES)) {
                    result.setExpirationDate(parseDate(header.getValue(), false));
                } else if (header.getName().equalsIgnoreCase(HttpConstants.HEADER_CONTENT_ENCODING)) {
                    HeaderReader hr = new HeaderReader(header.getValue());
                    String value = hr.readValue();
                    while (value != null) {
                        Encoding encoding = new Encoding(value);
                        if (!encoding.equals(Encoding.IDENTITY)) {
                            result.getEncodings().add(encoding);
                        }
                        value = hr.readValue();
                    }
                } else if (header.getName().equalsIgnoreCase(HttpConstants.HEADER_CONTENT_LANGUAGE)) {
                    HeaderReader hr = new HeaderReader(header.getValue());
                    String value = hr.readValue();
                    while (value != null) {
                        result.getLanguages().add(new Language(value));
                        value = hr.readValue();
                    }
                } else if (header.getName().equalsIgnoreCase(HttpConstants.HEADER_LAST_MODIFIED)) {
                    result.setModificationDate(parseDate(header.getValue(), false));
                } else if (header.getName().equalsIgnoreCase(HttpConstants.HEADER_ETAG)) {
                    result.setTag(Tag.parse(header.getValue()));
                } else if (header.getName().equalsIgnoreCase(HttpConstants.HEADER_CONTENT_LOCATION)) {
                    result.setIdentifier(header.getValue());
                }
            }
        }
        return result;
    }
}
