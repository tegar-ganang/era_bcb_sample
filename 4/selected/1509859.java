package com.volantis.xml.pipeline.sax.drivers.web;

import com.volantis.pipeline.localization.LocalizationFactory;
import com.volantis.shared.net.http.headers.HeaderUtils;
import com.volantis.shared.net.url.http.RuntimeHttpException;
import com.volantis.shared.time.Period;
import com.volantis.synergetics.localization.ExceptionLocalizer;
import com.volantis.synergetics.log.LogDispatcher;
import com.volantis.synergetics.performance.MonitoredTransaction;
import com.volantis.synergetics.url.URLIntrospector;
import com.volantis.synergetics.url.URLPrefixRewriteManager;
import com.volantis.synergetics.url.URLPrefixRewriteOperation;
import com.volantis.xml.pipeline.sax.XMLPipeline;
import com.volantis.xml.pipeline.sax.XMLPipelineContext;
import com.volantis.xml.pipeline.sax.XMLPipelineException;
import com.volantis.xml.pipeline.sax.XMLProcess;
import com.volantis.xml.pipeline.sax.conditioners.ContentConditioner;
import com.volantis.xml.pipeline.sax.config.XMLPipelineConfiguration;
import com.volantis.xml.pipeline.sax.convert.ConverterConfiguration;
import com.volantis.xml.pipeline.sax.convert.ConverterTuple;
import com.volantis.xml.pipeline.sax.convert.URLRewriteProcess;
import com.volantis.xml.pipeline.sax.convert.URLRewriteProcessConfiguration;
import com.volantis.xml.pipeline.sax.operation.AbstractOperationProcess;
import com.volantis.xml.pipeline.sax.performance.MonitoringConfiguration;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.helpers.XMLFilterImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The operation process for GET and POST Web Driver requests.
 */
public class HTTPRequestOperationProcess extends AbstractOperationProcess {

    /**
     * Used for logging
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(HTTPRequestOperationProcess.class);

    /**
     * Used to retrieve localized exception messages.
     */
    private static final ExceptionLocalizer exceptionLocalizer = LocalizationFactory.createExceptionLocalizer(HTTPRequestOperationProcess.class);

    /**
     * The TUPLES that are required for the URL rewriting.
     */
    private static final ConverterTuple[] TUPLES = new ConverterTuple[] { new ConverterTuple(null, "a", "href"), new ConverterTuple(null, "form", "action"), new ConverterTuple(null, "img", "src"), new ConverterTuple(null, "frame", "src"), new ConverterTuple(null, "link", "href"), new ConverterTuple(null, "xfform", "action") };

    /**
     * This value is used in a number of places. Its put here to help
     * locate it. It is the status code returned by a http server when a
     * conditional get is performed but no changes have been made.
     * The returned page will have no content.
     */
    private static final int NO_CHANGE_REPONSE = 304;

    /**
     * The type of the request for this operation.
     */
    private HTTPRequestType requestType;

    /**
     * The id for the request.
     */
    private String id;

    /**
     * The String representation of the url to use for this request.
     */
    private String urlString;

    /**
     * The string representation of the protocol to use for this request.  This
     * is extracted from {@link #urlString} and not explicitly set.
     */
    private String protocolString;

    /**
     * The version of http to use for the request.
     */
    private HTTPVersion httpVersion = HTTPVersion.HTTP_1_1;

    /**
     * This flag is used to determine whether this process silently follows
     * HTTP 302 response codes.
     */
    private Boolean followsRedirect;

    /**
     * This flag determines how errored content is handled in the pipeline.
     * If true the content is ignored and stored in an ignored content buffer
     * in the response.  If it is false the content is passed through the
     * pipeline.
     */
    private Boolean ignoreErroredContent;

    /**
     * The configuration for this operation process.
     */
    private WebDriverConfiguration configuration;

    /**
     * The input stream factory used to create alternative input streams
     * from an existing one and the value of the content encoding itself.
     */
    protected InputStreamFactory inputStreamFactory = null;

    /**
     * The timeout to be applied to connections made with the HTTP manager.
     * Measured in milliseconds. A zero or negative value means no timeout.
     */
    private Period timeout = Period.INDEFINITELY;

    /**
     * The pipeline configuration.
     */
    private XMLPipelineConfiguration pipelineConfiguration;

    /**
     * Construct a new RequestOperationProcess.
     */
    public HTTPRequestOperationProcess() {
    }

    public void setPipeline(XMLPipeline pipeline) {
        super.setPipeline(pipeline);
        XMLPipelineContext context = getPipelineContext();
        pipelineConfiguration = context.getPipelineConfiguration();
        configuration = (WebDriverConfiguration) pipelineConfiguration.retrieveConfiguration(WebDriverConfiguration.class);
    }

    public void stopProcess() throws SAXException {
        XMLPipelineContext context = getPipelineContext();
        if (!context.inErrorRecoveryMode()) {
            MonitoringConfiguration monitoringConfiguration = (MonitoringConfiguration) context.getPipelineConfiguration().retrieveConfiguration(MonitoringConfiguration.class);
            MonitoredTransaction webdTransaction = monitoringConfiguration.getTransaction("webd");
            webdTransaction.start();
            PluggableHTTPManager httpManager = ((WebDriverConfigurationImpl) configuration).getPluggableHTTPManager(protocolString, pipelineConfiguration);
            httpManager.initialize(configuration, timeout);
            try {
                context.pushBaseURI(getUrlString());
                httpManager.sendRequest(createRequestDetails(), context);
                webdTransaction.stop(MonitoredTransaction.SUCCESSFUL, getUrlString());
            } catch (HTTPException e) {
                webdTransaction.stop(MonitoredTransaction.FAILED, getUrlString());
                fatalError(new XMLPipelineException(exceptionLocalizer.format("http-request-process-failure", urlString), context.getCurrentLocator(), e));
            } catch (RuntimeHttpException e) {
                webdTransaction.stop(MonitoredTransaction.FAILED, getUrlString());
                fatalError(new XMLPipelineException(exceptionLocalizer.format("http-request-process-failure", urlString), context.getCurrentLocator(), e));
            } catch (MalformedURLException e) {
                webdTransaction.stop(MonitoredTransaction.FAILED, getUrlString());
                fatalError(new XMLPipelineException("base uri attribute is malformed", context.getCurrentLocator(), e));
            } finally {
                context.popBaseURI();
            }
        }
    }

    /**
     * Creates a {@link HTTPResponseProcessor} instance that can be used to
     * process the response of a HTTP Request
     * @return a <code>HTTPResponseProcessor</code> instance
     */
    private HTTPResponseProcessor createHTTPResponseProcessor() {
        return new HTTPResponseProcessor() {

            public void processHTTPResponse(String redirectURL, InputStream responseStream, int statusCode, String contentType, String contentEncoding) throws HTTPException {
                try {
                    processResponse(redirectURL, responseStream, statusCode, contentType, contentEncoding);
                } catch (SAXException e) {
                    XMLPipelineContext context = getPipelineContext();
                    if (context.inErrorRecoveryMode()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("WEBD encountered XML parsing exception " + "while error recovery is in progress, ignoring", e);
                        }
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("WEBD encountered XML parsing exception " + "while no error recovery is in progress, rethrowing", e);
                        }
                        throw new HTTPException(e);
                    }
                } catch (IOException e) {
                    throw new HTTPException(e);
                }
            }
        };
    }

    /**
     * Determine if a url has a resource part.
     * @param url the url represented as a String
     * @return true if url has a resource part; false otherwise.
     */
    private boolean hasResource(String url) {
        List resourceSuffixes = configuration.getContextChangingResourceSuffixes();
        boolean hasResource = false;
        int size = resourceSuffixes.size();
        for (int i = 0; i < size && !hasResource; i++) {
            hasResource = url.endsWith((String) resourceSuffixes.get(i));
        }
        return hasResource;
    }

    /**
     * Create the XML URL rewrite process and return it. The process created is
     * based on whether or not URL redirection processing is necessary or not.
     * If it isn't required this method will return null.
     *
     * @return the newly created URL rewrite process, null if not required.
     */
    private XMLProcess createURLRewriterProcess(String redirectURL) {
        XMLProcess process = null;
        if (redirectURL != null && getFollowRedirects()) {
            URLRewriteProcessConfiguration urlRewriteConfig = new URLRewriteProcessConfiguration();
            ConverterConfiguration convertConfig = urlRewriteConfig.getConverterConfiguration();
            convertConfig.setTuples(TUPLES);
            URLPrefixRewriteManager rewriteManager = urlRewriteConfig.getURLPrefixRewriteManager();
            rewriteManager.addRewritableURLPrefix(null, redirectURL, URLPrefixRewriteOperation.ADD_PREFIX);
            process = new URLRewriteProcess(urlRewriteConfig);
        }
        return process;
    }

    /**
     * Get the request type of this operation process
     * @return requestType.
     */
    public HTTPRequestType getRequestType() {
        return requestType;
    }

    /**
     * Set the request type of this operation process.
     * @param requestType The request type.
     */
    public void setRequestType(HTTPRequestType requestType) {
        this.requestType = requestType;
    }

    /**
     * Get the id for this request.
     * @return The id for this request.
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id for this request.
     * @param id The id for this request.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the String representation of the url for this request.
     * @return The String representation of the url for this request.
     */
    public String getUrlString() {
        return urlString;
    }

    /**
     * Set the String representation of the url for this request.
     * @param urlString The String representation of the url for this request.
     */
    public void setUrlString(String urlString) {
        this.urlString = urlString;
        URLIntrospector urlHandler = new URLIntrospector(urlString);
        protocolString = urlHandler.getProtocol();
    }

    /**
     * Get the version of http this request should specify.
     * @return The http version.
     */
    public HTTPVersion getHTTPVersion() {
        return httpVersion;
    }

    /**
     * Set the version of http this request should specify.
     * @param httpVersion The http version.
     */
    public void setHTTPVersion(HTTPVersion httpVersion) {
        this.httpVersion = httpVersion;
    }

    /**
     * Set the value that determines if {@link HTTPRequestOperationProcess}
     * automatically and silently follows HTTP response 302 redirects.
     *
     * @param follows If "true" then the process automatically redirects.  If
     * it is "false" then the redirect is not followed and it is up to the
     * client to perform the redirect.
     */
    public void setFollowRedirects(String follows) {
        this.followsRedirect = Boolean.valueOf(follows);
    }

    /**
     * Get the value that determines if {@link HTTPRequestOperationProcess}
     * automatically and silently follows HTTP response 302 redirects.
     *
     * This first checks whether it was specified as an attribute on the
     * markup.  If not the value is retrieved from the configuration.
     *
     * @return true if the process automatically redirects otherwise false
     * perform the redirect.
     */
    public boolean getFollowRedirects() {
        boolean result = true;
        if (followsRedirect != null) {
            result = followsRedirect.booleanValue();
        } else if (configuration != null) {
            result = configuration.getFollowRedirects();
        }
        return result;
    }

    /**
     * Set the value of the flag that determines how errored content is
     * handled. If "true" the content is ignored.  If
     * {@link WebDriverConfiguration#setIgnoreContentEnabled} has been called
     * with a true parameter then the ignored content will be stored in an
     * ignored content buffer in the response.  If it is "false" the content is
     * passed through the pipeline.
     * @param ignore The value that determines whether errored content is
     * ignored or processed.
     */
    public void setIgnoreErroredContent(String ignore) {
        ignoreErroredContent = Boolean.valueOf(ignore);
    }

    /**
     * Get the value of the flag that determines whether errored content is
     * either ignored or processed. This value may have been set explicitly on
     * the markup attributes or on the {@link WebDriverConfiguration}.  A value
     * set on the attributes takes precedence over values in the configuration.
     * As such this method will return the markup value first.  If no value
     * was set in the markup it returns the configuration value.  The
     * configuration defines a default value of true.
     * @return true if errored content should be ignored, otherwise false.
     */
    public boolean getIgnoreErroredContent() {
        boolean result;
        if (ignoreErroredContent != null) {
            result = ignoreErroredContent.booleanValue();
        } else {
            result = configuration.getIgnoreErroredContent();
        }
        return result;
    }

    /**
     * Check whether content should be ignored given the specified http
     * response status code.  The method will return true (the content should
     * be ignored) if the following conditions are met:
     * <ul>
     * <li> The status code is not 200
     * <li> The status code is not 304 (conditional get)
     * <li> The status code is not a 3XX code (redirects are handled separately)
     * <li> The markup or process configuration states that we should ignore
     *      content where the status code indicates an error.
     * </ul>
     * @param statusCode The HTTP response status code.
     * @return true if the listed conditions are met, otherwise false.
     */
    protected boolean shouldIgnoreContent(int statusCode) {
        boolean result = false;
        if ((statusCode != 200 && (statusCode < 300 || statusCode > 399) && getIgnoreErroredContent()) || statusCode == NO_CHANGE_REPONSE) {
            result = true;
        }
        return result;
    }

    /**
     * Get the input stream factory.
     *
     * @return the input stream factory.
     */
    protected InputStreamFactory getInputStreamFactory() {
        if (inputStreamFactory == null) {
            inputStreamFactory = new InputStreamFactory();
        }
        return inputStreamFactory;
    }

    /**
     * Process the response ensuring that it is conditioned as appropriate and
     * passed to an associated script if one exists.  The content may be stored
     * in the ignoredContent buffer of the response if {@link
     * WebDriverConfiguration#setIgnoreContentEnabled} has been called with a
     * true parameter AND EITHER the ignoreContent flag is true OR the content
     * type is one that we have been asked to ignore.
     *
     * @param redirectURL
     *                   if a redirect was followed this parameter will
     *                   reference the URL that was followed. Will be null if a
     *                   redirect did not occur.
     * @param response   an InputStream that can be used to retrieve the actual
     *                   response body.
     * @param statusCode the status of the response.
     * @param contentType
     *                   the content type of the response.
     * @param contentEncoding
     *                   the content encoding of the response.
     */
    protected void processResponse(String redirectURL, InputStream response, int statusCode, String contentType, String contentEncoding) throws IOException, SAXException {
        boolean ignoreContent = shouldIgnoreContent(statusCode);
        boolean ignoreThisContentType = ContentAction.IGNORE == retrieveContentAction(contentType);
        if (!ignoreContent && !ignoreThisContentType) {
            if (response != null) {
                InputStream stream = getInputStreamFactory().getInputStream(response, contentEncoding);
                PushbackInputStream pbis = new PushbackInputStream(stream);
                if (findStartDelimiter('<', pbis)) {
                    consumeResponse(redirectURL, pbis, contentType);
                }
            }
        } else {
            WebDriverResponse webdriver = retrieveWebDriverResponse();
            boolean saveIgnoredContent = ignoreContent || (ignoreThisContentType && configuration.isIgnoreContentEnabled());
            if (webdriver != null && saveIgnoredContent && response != null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[2048];
                int readBytes = response.read(buffer);
                while (readBytes != -1) {
                    out.write(buffer, 0, readBytes);
                    readBytes = response.read(buffer);
                }
                webdriver.setIgnoredContent(new ByteArrayInputStream(out.toByteArray()));
            }
        }
    }

    /**
     * Reads the pbis until the specified delimiter is found. That delimiter is
     * then pushed back onto the stream and this method returns true. If the
     * delimiter is not found then this method returns false and the push back
     * input stream will be empty.
     *
     * @param delim the delimiter to search for
     * @param pbis the PushbackInputStream to search for the delimiter
     * @return true if the delimiter was found, false otherwise
     */
    private boolean findStartDelimiter(char delim, PushbackInputStream pbis) {
        boolean result = false;
        try {
            int c = pbis.read();
            while (c != -1 && !result) {
                if (c == delim) {
                    result = true;
                    pbis.unread(c);
                } else {
                    c = pbis.read();
                }
            }
        } catch (IOException ioe) {
            result = false;
        }
        return result;
    }

    /**
     * Consume the response.
     * @param response The response.
     * @param contentType The content type of the response.
     * @throws IOException thrown by condition(..) call.
     * @throws SAXException thrown by condition(..) call.
     */
    private void consumeResponse(String redirectURL, InputStream response, String contentType) throws IOException, SAXException {
        XMLFilter responseFilter = retrieveResponseFilter(contentType);
        ContentConditioner conditioner = createContentConditioner(contentType, responseFilter);
        InputSource source = new InputSource(response);
        String charEncoding;
        charEncoding = HeaderUtils.getCharSetFromContentType(contentType);
        if (charEncoding == null) {
            charEncoding = configuration.getCharacterEncoding();
        }
        if (charEncoding != null) {
            source.setEncoding(charEncoding);
        }
        XMLProcess nextProcess = next;
        if (configuration.getResponseContainsPipelineMarkup()) {
            XMLPipeline pipeline = getPipelineContext().getPipelineFactory().createDynamicPipeline(getPipelineContext());
            XMLProcess pipelineProcess = pipeline.getPipelineProcess();
            pipelineProcess.setNextProcess(nextProcess);
            nextProcess = pipelineProcess;
        }
        if (redirectURL != null) {
            if (redirectURL.indexOf("://") == -1) {
                redirectURL = null;
            } else {
                if (hasResource(redirectURL)) {
                    int index = redirectURL.lastIndexOf('/');
                    redirectURL = redirectURL.substring(0, index);
                }
            }
        }
        XMLProcess urlRewriteProcess = createURLRewriterProcess(redirectURL);
        if (urlRewriteProcess != null) {
            urlRewriteProcess.setNextProcess(nextProcess);
            urlRewriteProcess.setPipeline(getPipeline());
            nextProcess = urlRewriteProcess;
        }
        XMLProcess cup = getPipelineContext().getPipelineFactory().createContextUpdatingProcess();
        cup.setPipeline(getPipeline());
        cup.setNextProcess(nextProcess);
        setNextProcess(cup);
        source.setSystemId(getUrlString());
        conditioner.condition(source, cup);
    }

    /**
     * Create the right kind of conditioner for the specified content type.
     * This is a factory method that may be moved to its own class in future
     * if/when conditioner creation becomes more complicated. There may need
     * to be something done about the XMLFilter also if in future the thing
     * that provides the conditioner does not know about the filter.
     * @param contentType The content type.
     * @param filter The XMLFilter for use by the created conditioner.
     * @return A conditioner that can condition the specified content type.
     */
    private ContentConditioner createContentConditioner(String contentType, XMLFilter filter) {
        ContentConditioner conditioner = null;
        if (contentType != null) {
            int pos = contentType.indexOf(';');
            if (pos != -1) {
                contentType = contentType.substring(0, pos);
            }
            WebDriverConditionerFactory factory = configuration.getWebDriverConditionerFactory(contentType);
            if (factory != null) {
                conditioner = factory.createConditioner(filter);
            }
            if (conditioner == null) {
                conditioner = HeaderUtils.createContentTypeConditioner(contentType, filter);
            }
        }
        return conditioner;
    }

    /**
     * <P>Retrieve the ContentAction for a specified contentType.</P>
     * <P>If the content type specified is null, then
     * {@link ContentAction#IGNORE} is returned.</P>
     * <P>If a content type is specified for which no ContentAction is found
     * then the following applies:
     * <ul>
     * <li>If only {@link ContentAction#CONSUME} are found then anything not
     * specified is IGNORED</li>
     * <li>IF only {@link ContentAction#IGNORE} are found then anything not
     * specified is CONSUMED</li>
     * <li>IF both are found then the content is CONSUMED either explicitly or
     * because it is not IGNORED</li>
     * </ul>
     * @param contentType The content type.
     * @return The ContentAction associated with the specified content type.
     */
    private ContentAction retrieveContentAction(String contentType) {
        ContentAction action = ContentAction.CONSUME;
        if (contentType == null) {
            action = ContentAction.IGNORE;
            if (logger.isDebugEnabled()) {
                String message = "Content type is null - ignoring content.";
                logger.debug(message);
            }
        } else {
            XMLPipelineContext context = getPipelineContext();
            Map contents = (Map) context.getProperty(Content.class);
            if (contents != null) {
                Content content = (Content) contents.get(contentType);
                if (content != null) {
                    action = content.getAction();
                } else {
                    Iterator iterator = contents.keySet().iterator();
                    boolean ignoreExists = false;
                    boolean consumeExists = false;
                    boolean found = false;
                    while (iterator.hasNext() && !found) {
                        String key = (String) iterator.next();
                        content = (Content) contents.get(key);
                        action = content.getAction();
                        if (contentType.startsWith(key)) {
                            found = true;
                        } else {
                            if (action == ContentAction.CONSUME) {
                                consumeExists = true;
                            } else if (action == ContentAction.IGNORE) {
                                ignoreExists = true;
                            }
                        }
                    }
                    if (found == false) {
                        if (ignoreExists) {
                            action = ContentAction.CONSUME;
                        } else {
                            action = ContentAction.IGNORE;
                        }
                    }
                }
            }
        }
        return action;
    }

    /**
     * If there is a Script associated with this operation for the content type
     * of the response then retreive the XMLFilter for this Script. If no Script
     * XMLFilter can be found for this operation and content type then the given
     * then adapt the consumer XMLProcess into an XMLFilter and return this.
     * @param contentType The content type.
     */
    private XMLFilter retrieveResponseFilter(String contentType) {
        XMLPipelineContext context = getPipelineContext();
        Script script = (Script) context.getProperty(Script.class);
        XMLFilter responseFilter = null;
        if (script != null) {
            responseFilter = configuration.retrieveScriptFilter(script.getRef(), contentType);
        } else {
            responseFilter = new XMLFilterImpl();
        }
        return responseFilter;
    }

    /**
     * Get the real request associated with this HTTPRequestOperationProcess
     * if there is one.
     * @return The WebDriverRequest associated with this
     * HTTPRequestOperationProcess.
     */
    private WebDriverRequest retrieveWebDriverRequest() {
        XMLPipelineContext context = getPipelineContext();
        WebDriverAccessor accessor = (WebDriverAccessor) context.getProperty(WebDriverAccessor.class);
        WebDriverRequest request = null;
        if (accessor == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No WebDriverAccessor available");
            }
        } else {
            request = accessor.getRequest(context);
        }
        return request;
    }

    /**
     * Get the real response associated with this HTTPRequestOperationProcess
     * if there is one.
     * @return The WebDriverResponse associated with this
     * HTTPRequestOperationProcess.
     */
    private WebDriverResponse retrieveWebDriverResponse() {
        XMLPipelineContext context = getPipelineContext();
        WebDriverAccessor accessor = (WebDriverAccessor) context.getProperty(WebDriverAccessor.class);
        WebDriverResponse response = null;
        if (accessor == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No WebDriverAccessor available");
            }
        } else {
            response = accessor.getResponse(context, id);
        }
        return response;
    }

    /**
     * Factory method that creates a {@link RequestDetails} instance
     * @return
     */
    private RequestDetails createRequestDetails() {
        return new RequestDetails(getUrlString(), getPipelineContext().getCurrentBaseURI().toExternalForm(), getRequestType(), retrieveWebDriverRequest(), retrieveWebDriverResponse(), getFollowRedirects(), getHTTPVersion(), (HTTPRequestPreprocessor) getPipelineContext().getProperty(HTTPRequestPreprocessor.class), (HTTPResponsePreprocessor) getPipelineContext().getProperty(HTTPResponsePreprocessor.class), createHTTPResponseProcessor());
    }

    /**
     * Allows the timeout to be specified.
     *
     * @param timeout the timeout.
     */
    public void setTimeout(Period timeout) {
        this.timeout = timeout;
    }
}
