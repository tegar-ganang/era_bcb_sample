package com.volantis.mps.bms.impl.remote;

import com.volantis.mps.bms.Failures;
import com.volantis.mps.bms.MessageService;
import com.volantis.mps.bms.MessageServiceException;
import com.volantis.mps.bms.SendRequest;
import com.volantis.mps.bms.impl.ser.ModelParser;
import com.volantis.mps.bms.impl.ser.ModelParserFactory;
import com.volantis.mps.localization.LocalizationFactory;
import com.volantis.synergetics.io.IOUtils;
import com.volantis.synergetics.log.LogDispatcher;
import com.volantis.synergetics.localization.ExceptionLocalizer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Remote client to web service. This is expected to use JiBX / ANOther to
 * create an HTTP request body of XML, etc.
 */
public class RemoteMessageService implements MessageService {

    private static final LogDispatcher LOGGER = LocalizationFactory.createLogger(RemoteMessageService.class);

    /**
     * Used to retrieve localized exception messages.
     */
    private static final ExceptionLocalizer EXCEPTION_LOCALIZER = LocalizationFactory.createExceptionLocalizer(RemoteMessageService.class);

    private String endpoint;

    /**
     * Public constructor used by reflective creation process.
     *
     * @param endpoint
     * @see com.volantis.mps.bms.impl.DefaultMessageServiceFactory
     */
    public RemoteMessageService(String endpoint) {
        if (null == endpoint) {
            throw new IllegalArgumentException("endpoint cannot be null");
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Endpoint: " + endpoint);
        }
        this.endpoint = endpoint;
    }

    public Failures process(SendRequest sendRequest) throws MessageServiceException {
        if (sendRequest == null) {
            throw new IllegalArgumentException(EXCEPTION_LOCALIZER.format("argument-is-null", "SendRequest"));
        }
        ModelParser parser = ModelParserFactory.getDefaultInstance().createModelParser();
        InputStream inputXML = getObjectsAsXMLInputStream(parser, sendRequest);
        return doRequest(inputXML, parser);
    }

    /**
     * Return an XML InputStream representing the SendRequest.
     *
     * @param parser      a ModelParser - not null.
     * @param sendRequest a SendRequest - not null.
     * @return an InputStream - not null.
     */
    private InputStream getObjectsAsXMLInputStream(ModelParser parser, SendRequest sendRequest) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        parser.write(sendRequest, new BufferedOutputStream(out));
        final ByteArrayInputStream result = new ByteArrayInputStream(out.toByteArray());
        IOUtils.closeQuietly(out);
        return result;
    }

    /**
     * Return the Failures obtained by opening a connection to the service and
     * making a remote call using the provided inputXML.
     *
     * @param inputXML an InputStream of XML that will be used for the request
     *                 body.
     * @param parser   a ModelParser - not null.
     * @return a Failures implementation.
     * @throws MessageServiceException if there was a problem.
     */
    private Failures doRequest(InputStream inputXML, ModelParser parser) throws MessageServiceException {
        HttpURLConnection connection = openConnection();
        doPOST(connection, inputXML);
        Failures result = readResponse(connection, parser);
        connection.disconnect();
        return result;
    }

    /**
     * Return the Failures returned by the remote call.
     *
     * @param connection an HttpURLConnection
     * @param parser     a ModelParser
     * @return a Failures implementation - not null.
     * @throws MessageServiceException if there was a problem.
     */
    private Failures readResponse(HttpURLConnection connection, ModelParser parser) throws MessageServiceException {
        InputStream readIn = getResponseInputStream(connection);
        Failures result = parser.readFailures(readIn);
        IOUtils.closeQuietly(readIn);
        return result;
    }

    /**
     * Return the InputStream from the connection ready for reading.
     *
     * @param connection a HttpURLConnection
     * @return an InputStream
     * @throws MessageServiceException if unable to get the InputStream.
     */
    private InputStream getResponseInputStream(HttpURLConnection connection) throws MessageServiceException {
        try {
            return new BufferedInputStream(connection.getInputStream());
        } catch (IOException e) {
            throw new MessageServiceException(e.getMessage(), e);
        }
    }

    /**
     * Do the POST to the HTTP-POX web service.
     *
     * @param connection an HttpURLConnection.
     * @param inputXML   an InputStream which is the request body. This will
     *                   get closed as part of the HTTP request.
     * @throws MessageServiceException if there was a problem.
     */
    private void doPOST(HttpURLConnection connection, InputStream inputXML) throws MessageServiceException {
        try {
            OutputStream requestStream = new BufferedOutputStream(connection.getOutputStream());
            IOUtils.copyAndClose(inputXML, requestStream);
            connection.connect();
        } catch (IOException e) {
            throw new MessageServiceException(e.getMessage(), e);
        }
    }

    /**
     * Return an HttpUrlConnection ready for a POST request, as returned by the
     * {@link java.net.URL#openConnection()} call.
     *
     * @return an HttpURLConnection
     * @throws MessageServiceException if there was a problem opening the
     *                                 connection.
     */
    private HttpURLConnection openConnection() throws MessageServiceException {
        try {
            final HttpURLConnection result = (HttpURLConnection) getURL().openConnection();
            result.setRequestMethod("POST");
            result.setDoOutput(true);
            return result;
        } catch (IOException e) {
            throw new MessageServiceException(e.getMessage(), e);
        }
    }

    /**
     * Return a URL based on the endpoint.
     *
     * @return a URL.
     * @throws MessageServiceException if the URL was invalid.
     */
    private URL getURL() throws MessageServiceException {
        try {
            return new URL(this.endpoint);
        } catch (MalformedURLException e) {
            throw new MessageServiceException(e.getMessage(), e);
        }
    }
}
