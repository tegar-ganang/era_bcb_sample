package org.orbeon.oxf.processor.generator;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.orbeon.oxf.cache.*;
import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.common.ValidationException;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.processor.*;
import org.orbeon.oxf.resources.ResourceManagerWrapper;
import org.orbeon.oxf.resources.URLFactory;
import org.orbeon.oxf.resources.oxf.Handler;
import org.orbeon.oxf.util.NetUtils;
import org.orbeon.oxf.xml.*;
import org.orbeon.oxf.xml.dom4j.LocationData;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xml.sax.*;
import org.xml.sax.helpers.AttributesImpl;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * Generates SAX events from a document fetched from an URL.
 * <p/>
 * NOTE: For XML content-type and encoding related questions, check out the following draft
 * document:
 * <p/>
 * http://www.faqs.org/rfcs/rfc3023.html
 * http://www.ietf.org/internet-drafts/draft-murata-kohn-lilley-xml-00.txt
 */
public class URLGenerator extends ProcessorImpl {

    private static Logger logger = Logger.getLogger(URLGenerator.class);

    private static final String DEFAULT_TEXT_ENCODING = "iso-8859-1";

    private static final boolean DEFAULT_VALIDATING = false;

    private static final boolean DEFAULT_FORCE_CONTENT_TYPE = false;

    private static final boolean DEFAULT_FORCE_ENCODING = false;

    private static final boolean DEFAULT_IGNORE_CONNECTION_ENCODING = false;

    private static final int CACHE_EXPIRATION_NO_CACHE = 0;

    private static final int CACHE_EXPIRATION_NO_EXPIRATION = -1;

    private static final int CACHE_EXPIRATION_LAST_MODIFIED = -2;

    private static final boolean DEFAULT_CACHE_USE_LOCAL_CACHE = true;

    private static final boolean DEFAULT_CACHE_ALWAYS_REVALIDATE = true;

    private static final int DEFAULT_CACHE_EXPIRATION = CACHE_EXPIRATION_LAST_MODIFIED;

    private static final String DEFAULT_TEXT_DOCUMENT_ELEMENT = "document";

    private static final String DEFAULT_BINARY_DOCUMENT_ELEMENT = "document";

    public static final String URL_NAMESPACE_URI = "http://www.orbeon.org/oxf/xml/url";

    public static final String VALIDATING_PROPERTY = "validating";

    private ConfigURIReferences localConfigURIReferences;

    public URLGenerator() {
        addInputInfo(new ProcessorInputOutputInfo(INPUT_CONFIG, URL_NAMESPACE_URI));
        addOutputInfo(new ProcessorInputOutputInfo(OUTPUT_DATA));
    }

    public URLGenerator(String url) {
        try {
            this.localConfigURIReferences = new ConfigURIReferences(new Config(URLFactory.createURL(url)));
            addOutputInfo(new ProcessorInputOutputInfo(OUTPUT_DATA));
        } catch (MalformedURLException e) {
            throw new OXFException(e);
        }
    }

    public URLGenerator(URL url) {
        this.localConfigURIReferences = new ConfigURIReferences(new Config(url));
        addOutputInfo(new ProcessorInputOutputInfo(OUTPUT_DATA));
    }

    public URLGenerator(URL url, String contentType, boolean forceContentType) {
        this.localConfigURIReferences = new ConfigURIReferences(new Config(url, contentType, forceContentType));
        addOutputInfo(new ProcessorInputOutputInfo(OUTPUT_DATA));
    }

    private static class Config {

        private URL url;

        private String contentType = ProcessorUtils.DEFAULT_CONTENT_TYPE;

        private boolean forceContentType = DEFAULT_FORCE_CONTENT_TYPE;

        private String encoding;

        private boolean forceEncoding = DEFAULT_FORCE_ENCODING;

        private boolean ignoreConnectionEncoding = DEFAULT_IGNORE_CONNECTION_ENCODING;

        private boolean validating = DEFAULT_VALIDATING;

        private Map headers;

        private boolean cacheUseLocalCache = DEFAULT_CACHE_USE_LOCAL_CACHE;

        private boolean cacheAlwaysRevalidate = DEFAULT_CACHE_ALWAYS_REVALIDATE;

        private int cacheExpiration = DEFAULT_CACHE_EXPIRATION;

        private TidyConfig tidyConfig;

        public Config(URL url) {
            this.url = url;
        }

        public Config(URL url, String contentType, boolean forceContentType) {
            this(url);
            this.forceContentType = true;
            this.contentType = contentType;
            this.forceContentType = forceContentType;
        }

        public Config(URL url, String contentType, boolean forceContentType, String encoding, boolean forceEncoding, boolean ignoreConnectionEncoding, boolean validating, Map headers, boolean cacheUseLocalCache, boolean cacheAlwaysRevalidate, int cacheExpiration, TidyConfig tidyConfig) {
            this.url = url;
            this.contentType = contentType;
            this.forceContentType = forceContentType;
            this.encoding = encoding;
            this.forceEncoding = forceEncoding;
            this.ignoreConnectionEncoding = ignoreConnectionEncoding;
            this.validating = validating;
            this.headers = headers;
            this.cacheUseLocalCache = cacheUseLocalCache;
            this.cacheAlwaysRevalidate = cacheAlwaysRevalidate;
            this.cacheExpiration = cacheExpiration;
            this.tidyConfig = tidyConfig;
        }

        public URL getURL() {
            return url;
        }

        public String getContentType() {
            return contentType;
        }

        public boolean isForceContentType() {
            return forceContentType;
        }

        public String getEncoding() {
            return encoding;
        }

        public boolean isForceEncoding() {
            return forceEncoding;
        }

        public boolean isIgnoreConnectionEncoding() {
            return ignoreConnectionEncoding;
        }

        public TidyConfig getTidyConfig() {
            return tidyConfig;
        }

        public boolean isValidating() {
            return validating;
        }

        public Map getHeaders() {
            return headers;
        }

        public boolean isCacheUseLocalCache() {
            return cacheUseLocalCache;
        }

        public String toString() {
            return "[" + getURL().toExternalForm() + "|" + getContentType() + "|" + getEncoding() + "|" + isValidating() + "|" + isForceContentType() + "|" + isForceEncoding() + "|" + isIgnoreConnectionEncoding() + "|" + tidyConfig + "]";
        }
    }

    private static class ConfigURIReferences {

        public ConfigURIReferences(Config config) {
            this.config = config;
        }

        public Config config;

        public URIReferences uriReferences;
    }

    public ProcessorOutput createOutput(String name) {
        ProcessorOutput output = new ProcessorImpl.ProcessorOutputImpl(getClass(), name) {

            public void readImpl(PipelineContext pipelineContext, ContentHandler contentHandler) {
                ConfigURIReferences configURIReferences = URLGenerator.this.localConfigURIReferences != null ? localConfigURIReferences : (ConfigURIReferences) readCacheInputAsObject(pipelineContext, getInputByName(INPUT_CONFIG), new CacheableInputReader() {

                    public Object read(PipelineContext context, ProcessorInput input) {
                        Element configElement = readInputAsDOM4J(context, input).getRootElement();
                        String url = configElement.getTextTrim();
                        if (url != null && !url.equals("")) {
                            try {
                                return new ConfigURIReferences(new Config(URLFactory.createURL(url)));
                            } catch (MalformedURLException e) {
                                throw new OXFException(e);
                            }
                        }
                        url = XPathUtils.selectStringValueNormalize(configElement, "/config/url");
                        String contentType = XPathUtils.selectStringValueNormalize(configElement, "/config/content-type");
                        boolean forceContentType = ProcessorUtils.selectBooleanValue(configElement, "/config/force-content-type", DEFAULT_FORCE_CONTENT_TYPE);
                        if (forceContentType && (contentType == null || contentType.equals(""))) throw new OXFException("The force-content-type element requires a content-type element.");
                        String encoding = XPathUtils.selectStringValueNormalize(configElement, "/config/encoding");
                        boolean forceEncoding = ProcessorUtils.selectBooleanValue(configElement, "/config/force-encoding", DEFAULT_FORCE_ENCODING);
                        boolean ignoreConnectionEncoding = ProcessorUtils.selectBooleanValue(configElement, "/config/ignore-connection-encoding", DEFAULT_IGNORE_CONNECTION_ENCODING);
                        if (forceEncoding && (encoding == null || encoding.equals(""))) throw new OXFException("The force-encoding element requires an encoding element.");
                        Map headers = new HashMap();
                        for (Iterator i = configElement.selectNodes("/config/header").iterator(); i.hasNext(); ) {
                            Element headerElement = (Element) i.next();
                            String name = headerElement.element("name").getStringValue();
                            String value = headerElement.element("value").getStringValue();
                            headers.put(name, value);
                        }
                        boolean defaultValidating = getPropertySet().getBoolean(VALIDATING_PROPERTY, DEFAULT_VALIDATING).booleanValue();
                        boolean validating = ProcessorUtils.selectBooleanValue(configElement, "/config/validating", defaultValidating);
                        boolean cacheUseLocalCache = ProcessorUtils.selectBooleanValue(configElement, "/config/cache-control/use-local-cache", DEFAULT_CACHE_USE_LOCAL_CACHE);
                        boolean cacheAlwaysRevalidate = ProcessorUtils.selectBooleanValue(configElement, "/config/cache-control/always-revalidate", DEFAULT_CACHE_ALWAYS_REVALIDATE);
                        int cacheExpiration = ProcessorUtils.selectIntValue(configElement, "/config/cache-control/expiration", DEFAULT_CACHE_EXPIRATION);
                        TidyConfig tidyConfig = new TidyConfig(XPathUtils.selectSingleNode(configElement, "/config/tidy-options"));
                        try {
                            LocationData locationData = getLocationData();
                            URL fullURL = (locationData != null && locationData.getSystemID() != null) ? URLFactory.createURL(locationData.getSystemID(), url) : URLFactory.createURL(url);
                            Config config = new Config(fullURL, contentType, forceContentType, encoding, forceEncoding, ignoreConnectionEncoding, validating, headers, cacheUseLocalCache, cacheAlwaysRevalidate, cacheExpiration, tidyConfig);
                            if (logger.isDebugEnabled()) logger.debug("Read configuration: " + config.toString());
                            return new ConfigURIReferences(config);
                        } catch (MalformedURLException e) {
                            throw new OXFException(e);
                        }
                    }
                });
                try {
                    if (configURIReferences.config.getURL() == null) throw new OXFException("Missing configuration.");
                    CacheKey key = new InternalCacheKey(URLGenerator.this, "urlDocument", configURIReferences.config.toString());
                    Object cachedResource = null;
                    if (cachedResource == null) {
                        ResourceHandler handler = null;
                        try {
                            Object validity = getValidityImpl(pipelineContext);
                            cachedResource = ObjectCache.instance().findValid(pipelineContext, key, validity);
                            if (cachedResource != null) {
                                ((SAXStore) cachedResource).replay(contentHandler);
                            } else {
                                handler = Handler.PROTOCOL.equals(configURIReferences.config.getURL().getProtocol()) ? (ResourceHandler) new OXFResourceHandler(configURIReferences.config) : (ResourceHandler) new URLResourceHandler(configURIReferences.config);
                                String contentType;
                                if (configURIReferences.config.isForceContentType()) {
                                    contentType = configURIReferences.config.getContentType();
                                } else {
                                    contentType = handler.getResourceContentType();
                                    if (contentType == null) contentType = configURIReferences.config.getContentType();
                                    if (contentType == null) contentType = ProcessorUtils.DEFAULT_CONTENT_TYPE;
                                }
                                ContentHandler output = configURIReferences.config.isCacheUseLocalCache() ? new SAXStore(contentHandler) : contentHandler;
                                if (ProcessorUtils.HTML_CONTENT_TYPE.equals(contentType)) {
                                    handler.readHTML(output);
                                    configURIReferences.uriReferences = null;
                                } else if (ProcessorUtils.isXMLContentType(contentType)) {
                                    LocalXIncludeListener localXIncludeListener = new LocalXIncludeListener();
                                    XIncludeHandler.setXIncludeListener(localXIncludeListener);
                                    try {
                                        handler.readXML(pipelineContext, output);
                                    } finally {
                                        XIncludeHandler.setXIncludeListener(null);
                                    }
                                    localXIncludeListener.updateCache(pipelineContext, configURIReferences);
                                } else if (ProcessorUtils.isTextContentType(contentType)) {
                                    handler.readText(output, contentType);
                                    configURIReferences.uriReferences = null;
                                } else {
                                    handler.readBinary(output, contentType);
                                    configURIReferences.uriReferences = null;
                                }
                                if (configURIReferences.config.isCacheUseLocalCache()) ObjectCache.instance().add(pipelineContext, key, validity, output);
                            }
                        } finally {
                            if (handler != null) handler.destroy();
                        }
                    }
                } catch (SAXParseException spe) {
                    throw new ValidationException(spe.getMessage(), new LocationData(spe));
                } catch (ValidationException e) {
                    LocationData locationData = e.getLocationData();
                    if (locationData == null || locationData.getSystemID() == null) e.setLocationData(new LocationData(configURIReferences.config.getURL().toExternalForm(), -1, -1));
                    throw e;
                } catch (OXFException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ValidationException(e, new LocationData(configURIReferences.config.getURL().toExternalForm(), -1, -1));
                }
            }

            public OutputCacheKey getKeyImpl(PipelineContext context) {
                try {
                    ConfigURIReferences configURIReferences = getConfigURIReferences(context);
                    if (configURIReferences == null) return null;
                    List keys = new ArrayList();
                    if (localConfigURIReferences == null) {
                        KeyValidity configKeyValidity = getInputKeyValidity(context, INPUT_CONFIG);
                        keys.add(configKeyValidity.key);
                    }
                    keys.add(new OutputCacheKey(this, configURIReferences.config.toString()));
                    if (configURIReferences.uriReferences != null) {
                        for (Iterator i = configURIReferences.uriReferences.references.iterator(); i.hasNext(); ) {
                            URIReference uriReference = (URIReference) i.next();
                            keys.add(new InternalCacheKey(URLGenerator.this, "urlReference", URLFactory.createURL(uriReference.context, uriReference.spec).toExternalForm()));
                        }
                    }
                    return new OutputCacheKey(this, keys);
                } catch (MalformedURLException e) {
                    throw new OXFException(e);
                }
            }

            public Object getValidityImpl(PipelineContext context) {
                try {
                    ConfigURIReferences configURIReferences = getConfigURIReferences(context);
                    if (configURIReferences == null) return null;
                    List validities = new ArrayList();
                    if (localConfigURIReferences == null) {
                        KeyValidity configKeyValidity = getInputKeyValidity(context, INPUT_CONFIG);
                        if (configKeyValidity == null) return null;
                        validities.add(configKeyValidity.validity);
                    }
                    validities.add(getHandlerValidity(configURIReferences.config.getURL()));
                    if (configURIReferences.uriReferences != null) {
                        for (Iterator i = configURIReferences.uriReferences.references.iterator(); i.hasNext(); ) {
                            URIReference uriReference = (URIReference) i.next();
                            validities.add(getHandlerValidity(URLFactory.createURL(uriReference.context, uriReference.spec)));
                        }
                    }
                    return validities;
                } catch (IOException e) {
                    throw new OXFException(e);
                }
            }

            private Object getHandlerValidity(URL url) {
                try {
                    ResourceHandler handler = Handler.PROTOCOL.equals(url.getProtocol()) ? (ResourceHandler) new OXFResourceHandler(new Config(url)) : (ResourceHandler) new URLResourceHandler(new Config(url));
                    try {
                        return handler.getValidity();
                    } finally {
                        if (handler != null) handler.destroy();
                    }
                } catch (Exception e) {
                    return null;
                }
            }

            private ConfigURIReferences getConfigURIReferences(PipelineContext context) {
                if (localConfigURIReferences != null) return localConfigURIReferences;
                KeyValidity keyValidity = getInputKeyValidity(context, INPUT_CONFIG);
                if (keyValidity == null) return null;
                ConfigURIReferences config = (ConfigURIReferences) ObjectCache.instance().findValid(context, keyValidity.key, keyValidity.validity);
                if (logger.isDebugEnabled()) {
                    if (config != null) logger.debug("Config found: " + config.toString()); else logger.debug("Config not found");
                }
                return config;
            }
        };
        addOutput(name, output);
        return output;
    }

    private interface ResourceHandler {

        public Object getValidity() throws IOException;

        public String getResourceContentType() throws IOException;

        public String getConnectionEncoding() throws IOException;

        public void destroy() throws IOException;

        public void readHTML(ContentHandler output) throws IOException;

        public void readText(ContentHandler output, String contentType) throws IOException;

        public void readXML(PipelineContext pipelineContext, ContentHandler output) throws IOException;

        public void readBinary(ContentHandler output, String contentType) throws IOException;
    }

    private static class OXFResourceHandler implements ResourceHandler {

        private Config config;

        private String resourceManagerKey;

        private InputStream inputStream;

        public OXFResourceHandler(Config config) {
            this.config = config;
        }

        public String getResourceContentType() throws IOException {
            return null;
        }

        public String getConnectionEncoding() throws IOException {
            return null;
        }

        public Object getValidity() throws IOException {
            getKey();
            if (logger.isDebugEnabled()) logger.debug("OXF Protocol: Using ResourceManager for key " + getKey());
            long result = ResourceManagerWrapper.instance().lastModified(getKey());
            return (result <= 0) ? null : new Long(result);
        }

        public void destroy() throws IOException {
            if (inputStream != null) {
                inputStream.close();
            }
        }

        private String getExternalEncoding() throws IOException {
            if (config.isForceEncoding()) return config.getEncoding();
            String connectionEncoding = getConnectionEncoding();
            if (!config.isIgnoreConnectionEncoding() && connectionEncoding != null) return connectionEncoding;
            String userEncoding = config.getEncoding();
            if (userEncoding != null) return userEncoding;
            return null;
        }

        public void readHTML(ContentHandler output) throws IOException {
            inputStream = ResourceManagerWrapper.instance().getContentAsStream(getKey());
            URLResourceHandler.readHTML(inputStream, config.getTidyConfig(), getExternalEncoding(), output);
        }

        public void readText(ContentHandler output, String contentType) throws IOException {
            inputStream = ResourceManagerWrapper.instance().getContentAsStream(getKey());
            URLResourceHandler.readText(inputStream, getExternalEncoding(), output, contentType);
        }

        public void readXML(PipelineContext pipelineContext, ContentHandler output) throws IOException {
            if (getExternalEncoding() != null) {
                inputStream = ResourceManagerWrapper.instance().getContentAsStream(getKey());
                XMLUtils.readerToSAX(new InputStreamReader(inputStream, getExternalEncoding()), config.getURL().toExternalForm(), output, config.isValidating());
            } else {
                ResourceManagerWrapper.instance().getContentAsSAX(getKey(), output);
            }
        }

        public void readBinary(ContentHandler output, String contentType) throws IOException {
            inputStream = ResourceManagerWrapper.instance().getContentAsStream(getKey());
            URLResourceHandler.readBinary(inputStream, output, contentType);
        }

        private String getKey() {
            if (resourceManagerKey == null) resourceManagerKey = config.getURL().getFile();
            return resourceManagerKey;
        }
    }

    private static class LocalXIncludeListener implements XIncludeHandler.XIncludeListener {

        private URIReferences uriReferences;

        public void inclusion(String base, String href) {
            if (uriReferences == null) uriReferences = new URIReferences();
            uriReferences.references.add(new URIReference(base, href));
        }

        public void updateCache(PipelineContext pipelineContext, ConfigURIReferences configURIReferences) {
            configURIReferences.uriReferences = uriReferences;
        }
    }

    private static class URLResourceHandler implements ResourceHandler {

        private Config config;

        private URLConnection urlConn;

        public URLResourceHandler(Config config) {
            this.config = config;
        }

        public String getResourceContentType() throws IOException {
            if ("file".equals(config.getURL().getProtocol())) return null;
            openConnection();
            return NetUtils.getContentTypeContentType(urlConn.getContentType());
        }

        public String getConnectionEncoding() throws IOException {
            if ("file".equals(config.getURL().getProtocol())) return null;
            openConnection();
            return NetUtils.getContentTypeCharset(urlConn.getContentType());
        }

        public Object getValidity() throws IOException {
            openConnection();
            long lastModified = NetUtils.getLastModified(urlConn);
            return lastModified <= 0 ? null : new Long(lastModified);
        }

        public void destroy() throws IOException {
            if (urlConn != null) {
                urlConn.getInputStream().close();
            }
        }

        private void openConnection() throws IOException {
            if (urlConn == null) {
                urlConn = config.getURL().openConnection();
                Map headers = config.getHeaders();
                if (headers != null) {
                    for (Iterator i = headers.keySet().iterator(); i.hasNext(); ) {
                        String name = (String) i.next();
                        String value = (String) config.getHeaders().get(name);
                        urlConn.setRequestProperty(name, value);
                    }
                }
            }
        }

        private String getExternalEncoding() throws IOException {
            if (config.isForceEncoding()) return config.getEncoding();
            String connectionEncoding = getConnectionEncoding();
            if (!config.isIgnoreConnectionEncoding() && connectionEncoding != null) return connectionEncoding;
            String userEncoding = config.getEncoding();
            if (userEncoding != null) return userEncoding;
            return null;
        }

        public void readHTML(ContentHandler output) throws IOException {
            openConnection();
            readHTML(urlConn.getInputStream(), config.getTidyConfig(), getExternalEncoding(), output);
        }

        public void readText(ContentHandler output, String contentType) throws IOException {
            openConnection();
            readText(urlConn.getInputStream(), getExternalEncoding(), output, contentType);
        }

        public void readBinary(ContentHandler output, String contentType) throws IOException {
            openConnection();
            readBinary(urlConn.getInputStream(), output, contentType);
        }

        public void readXML(PipelineContext pipelineContext, ContentHandler output) throws IOException {
            openConnection();
            try {
                SAXParser parser = XMLUtils.newSAXParser(config.isValidating());
                XMLReader reader = parser.getXMLReader();
                reader.setContentHandler(output);
                reader.setEntityResolver(XMLUtils.ENTITY_RESOLVER);
                reader.setErrorHandler(XMLUtils.ERROR_HANDLER);
                InputSource inputSource;
                if (getExternalEncoding() != null) {
                    inputSource = new InputSource(new InputStreamReader(urlConn.getInputStream(), getExternalEncoding()));
                } else {
                    inputSource = new InputSource(urlConn.getInputStream());
                }
                inputSource.setSystemId(config.getURL().toExternalForm());
                reader.parse(inputSource);
            } catch (SAXException e) {
                throw new OXFException(e);
            }
        }

        public static void readHTML(InputStream is, TidyConfig tidyConfig, String encoding, ContentHandler output) {
            Tidy tidy = new Tidy();
            tidy.setShowWarnings(tidyConfig.isShowWarnings());
            tidy.setQuiet(tidyConfig.isQuiet());
            tidy.setCharEncoding(TidyConfig.getTidyEncoding(encoding));
            Document document = tidy.parseDOM(is, null);
            try {
                Transformer transformer = TransformerUtils.getIdentityTransformer();
                transformer.transform(new DOMSource(document), new SAXResult(output));
            } catch (TransformerException e) {
                throw new OXFException(e);
            }
        }

        /**
         * Generate a "standard" OXF text document.
         *
         * @param is
         * @param encoding
         * @param output
         * @param contentType
         * @throws IOException
         */
        public static void readText(InputStream is, String encoding, ContentHandler output, String contentType) throws IOException {
            if (encoding == null) encoding = DEFAULT_TEXT_ENCODING;
            try {
                AttributesImpl attributes = new AttributesImpl();
                output.startPrefixMapping(XMLConstants.XSI_PREFIX, XMLConstants.XSI_URI);
                output.startPrefixMapping(XMLConstants.XSD_PREFIX, XMLConstants.XSD_URI);
                attributes.addAttribute(XMLConstants.XSI_URI, "type", "xsi:type", "CDATA", XMLConstants.XS_STRING_QNAME.getQualifiedName());
                if (contentType != null) attributes.addAttribute("", "content-type", "content-type", "CDATA", contentType);
                output.startDocument();
                output.startElement("", DEFAULT_TEXT_DOCUMENT_ELEMENT, DEFAULT_TEXT_DOCUMENT_ELEMENT, attributes);
                XMLUtils.readerToCharacters(new InputStreamReader(is, encoding), output);
                output.endElement("", DEFAULT_TEXT_DOCUMENT_ELEMENT, DEFAULT_TEXT_DOCUMENT_ELEMENT);
                output.endDocument();
            } catch (SAXException e) {
                throw new OXFException(e);
            }
        }

        /**
         * Generate a "standard" OXF binary document.
         *
         * @param is
         * @param output
         * @param contentType
         */
        public static void readBinary(InputStream is, ContentHandler output, String contentType) {
            try {
                AttributesImpl attributes = new AttributesImpl();
                output.startPrefixMapping(XMLConstants.XSI_PREFIX, XMLConstants.XSI_URI);
                output.startPrefixMapping(XMLConstants.XSD_PREFIX, XMLConstants.XSD_URI);
                attributes.addAttribute(XMLConstants.XSI_URI, "type", "xsi:type", "CDATA", XMLConstants.XS_BASE64BINARY_QNAME.getQualifiedName());
                if (contentType != null) attributes.addAttribute("", "content-type", "content-type", "CDATA", contentType);
                output.startDocument();
                output.startElement("", DEFAULT_BINARY_DOCUMENT_ELEMENT, DEFAULT_BINARY_DOCUMENT_ELEMENT, attributes);
                XMLUtils.inputStreamToBase64Characters(new BufferedInputStream(is), output);
                output.endElement("", DEFAULT_BINARY_DOCUMENT_ELEMENT, DEFAULT_BINARY_DOCUMENT_ELEMENT);
                output.endDocument();
            } catch (SAXException e) {
                throw new OXFException(e);
            }
        }
    }

    private static class URIReference {

        public URIReference(String context, String spec) {
            this.context = context;
            this.spec = spec;
        }

        public String context;

        public String spec;
    }

    private static class URIReferences {

        public List references = new ArrayList();
    }
}
