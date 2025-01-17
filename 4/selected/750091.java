package org.orbeon.oxf.processor.serializer;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.pipeline.api.ExternalContext;
import org.orbeon.oxf.processor.CacheableInputReader;
import org.orbeon.oxf.processor.ProcessorInput;
import org.orbeon.oxf.processor.ProcessorInputOutputInfo;
import org.orbeon.oxf.processor.serializer.store.ResultStore;
import org.orbeon.oxf.processor.serializer.store.ResultStoreOutputStream;
import org.orbeon.oxf.util.LoggerFactory;
import org.orbeon.oxf.xml.TransformerUtils;
import org.orbeon.oxf.xml.XPathUtils;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

/**
 * FIXME: This should be merged with the HTTP serializers. There is way too much code duplication
 * between the two code bases.
 */
public class FileSerializer extends CachedSerializer {

    private static Logger logger = LoggerFactory.createLogger(FileSerializer.class);

    public static final String FILE_SERIALIZER_CONFIG_NAMESPACE_URI = "http://orbeon.org/oxf/xml/file-serializer-config";

    private static final String XML_CONTENT_TYPE = "text/xml";

    private static final String HTML_CONTENT_TYPE = "text/html";

    private static final String TEXT_CONTENT_TYPE = "text/plain";

    private static final String DEFAULT_CONTENT_TYPE = XML_CONTENT_TYPE;

    private static final String XML_METHOD = "xml";

    private static final String HTML_METHOD = "html";

    private static final String TEXT_METHOD = "text";

    public static final String DEFAULT_XML_VERSION = "1.0";

    public static final String DEFAULT_ENCODING = TransformerUtils.DEFAULT_OUTPUT_ENCODING;

    public static final boolean DEFAULT_INDENT = true;

    public static final int DEFAULT_INDENT_AMOUNT = 1;

    public static final String DIRECTORY_PROPERTY = "directory";

    public FileSerializer() {
        addInputInfo(new ProcessorInputOutputInfo(INPUT_CONFIG, FILE_SERIALIZER_CONFIG_NAMESPACE_URI));
        addInputInfo(new ProcessorInputOutputInfo(INPUT_DATA));
    }

    private static class Config {

        private String contentType;

        private String method;

        private String version;

        private String publicDoctype;

        private String systemDoctype;

        private String encoding;

        private boolean indent;

        private int indentAmount;

        private String directory;

        private String file;

        private boolean omitXMLDeclaration;

        private Boolean standalone;

        boolean cacheUseLocalCache;

        public Config(Document document) {
            directory = XPathUtils.selectStringValueNormalize(document, "/config/directory");
            file = XPathUtils.selectStringValueNormalize(document, "/config/file");
            contentType = XPathUtils.selectStringValueNormalize(document, "/config/content-type");
            if (contentType == null) contentType = DEFAULT_CONTENT_TYPE;
            if (HTML_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
                method = HTML_METHOD;
            } else if (XML_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
                method = XML_METHOD;
            } else if (TEXT_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
                method = TEXT_METHOD;
            } else throw new OXFException("Invalid content-type" + contentType);
            version = XPathUtils.selectStringValueNormalize(document, "/config/version");
            if (version == null) {
                if (method.equals(XML_METHOD)) version = DEFAULT_XML_VERSION; else if (method.equals(HTML_METHOD)) version = null;
            }
            publicDoctype = XPathUtils.selectStringValueNormalize(document, "/config/public-doctype");
            systemDoctype = XPathUtils.selectStringValueNormalize(document, "/config/system-doctype");
            encoding = XPathUtils.selectStringValueNormalize(document, "/config/encoding");
            if (encoding == null) {
                encoding = DEFAULT_ENCODING;
            }
            String indentString = XPathUtils.selectStringValueNormalize(document, "/config/indent");
            if (indentString == null) indent = DEFAULT_INDENT; else indent = new Boolean(indentString).booleanValue();
            Integer indentAmountInteger = XPathUtils.selectIntegerValue(document, "/config/indent-amount");
            if (indentAmountInteger == null) indentAmount = DEFAULT_INDENT_AMOUNT; else indentAmount = indentAmountInteger.intValue();
            String omitXMLDeclarationString = XPathUtils.selectStringValueNormalize(document, "/config/omit-xml-declaration");
            if (omitXMLDeclarationString != null) omitXMLDeclaration = new Boolean(omitXMLDeclarationString).booleanValue();
            String standaloneString = XPathUtils.selectStringValueNormalize(document, "/config/standalone");
            if (standaloneString != null) standalone = new Boolean(standaloneString);
            String cacheUseLocalCacheString = XPathUtils.selectStringValueNormalize(document, "/config/cache-control/use-local-cache");
            if (cacheUseLocalCacheString == null) cacheUseLocalCache = DEFAULT_CACHE_USE_LOCAL_CACHE; else cacheUseLocalCache = new Boolean(cacheUseLocalCacheString).booleanValue();
        }

        public String getContentType() {
            return contentType;
        }

        public String getMethod() {
            return method;
        }

        public String getVersion() {
            return version;
        }

        public String getPublicDoctype() {
            return publicDoctype;
        }

        public String getSystemDoctype() {
            return systemDoctype;
        }

        public String getEncoding() {
            return encoding;
        }

        public boolean isOmitXMLDeclaration() {
            return omitXMLDeclaration;
        }

        public Boolean isStandalone() {
            return standalone;
        }

        public boolean isIndent() {
            return indent;
        }

        public int getIndentAmount() {
            return indentAmount;
        }

        public String getDirectory() {
            return directory;
        }

        public String getFile() {
            return file;
        }
    }

    public void start(PipelineContext context) {
        try {
            final Config config = (Config) readCacheInputAsObject(context, getInputByName(INPUT_CONFIG), new CacheableInputReader() {

                public Object read(PipelineContext context, ProcessorInput input) {
                    return new Config(readInputAsDOM4J(context, input));
                }
            });
            ProcessorInput dataInput = getInputByName(INPUT_DATA);
            File file;
            String directoryProperty = getPropertySet().getString(DIRECTORY_PROPERTY);
            if (directoryProperty == null && config.getDirectory() == null) {
                file = new File(config.getFile());
            } else {
                File baseDirectory = directoryProperty != null ? new File(directoryProperty) : new File(config.getDirectory());
                if (!baseDirectory.isDirectory() || !baseDirectory.canWrite()) throw new OXFException("Directory '" + baseDirectory + "' is not a directory or is not writeable.");
                file = new File(baseDirectory, config.getFile());
            }
            if (file.exists() && file.canWrite()) file.delete();
            if (!file.createNewFile()) throw new OXFException("Can't create file: " + file);
            final OutputStream fileOutputStream = new FileOutputStream(file);
            context.addContextListener(new PipelineContext.ContextListenerAdapter() {

                public void contextDestroyed(boolean success) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        throw new OXFException(e);
                    }
                }
            });
            if (config.cacheUseLocalCache) {
                final boolean[] read = new boolean[1];
                ResultStore filter = (ResultStore) readCacheInputAsObject(context, dataInput, new CacheableInputReader() {

                    public Object read(PipelineContext context, ProcessorInput input) {
                        read[0] = true;
                        if (logger.isDebugEnabled()) logger.debug("Output not cached");
                        try {
                            ResultStoreOutputStream resultStoreOutputStream = new ResultStoreOutputStream(fileOutputStream);
                            readInput(context, null, input, config, resultStoreOutputStream);
                            resultStoreOutputStream.close();
                            return resultStoreOutputStream;
                        } catch (IOException e) {
                            throw new OXFException(e);
                        }
                    }
                });
                if (!read[0]) {
                    if (logger.isDebugEnabled()) logger.debug("Serializer output cached");
                    filter.replay(fileOutputStream);
                }
            } else {
                readInput(context, null, dataInput, config, fileOutputStream);
                fileOutputStream.close();
            }
        } catch (Exception e) {
            throw new OXFException(e);
        }
    }

    protected void readInput(PipelineContext context, ExternalContext.Response response, ProcessorInput input, Object _config, OutputStream outputStream) {
        FileSerializer.Config config = (FileSerializer.Config) _config;
        Writer writer = getWriter(outputStream, config);
        TransformerHandler identity = TransformerUtils.getIdentityTransformerHandler();
        TransformerUtils.applyOutputProperties(identity.getTransformer(), config.getMethod(), config.getVersion(), config.getPublicDoctype(), config.getSystemDoctype(), config.getEncoding(), config.isOmitXMLDeclaration(), config.isStandalone(), config.isIndent(), config.getIndentAmount());
        identity.setResult(new StreamResult(writer));
        readInputAsSAX(context, input, new SerializerContentHandler(identity, writer, getPropertySet().getBoolean("serialize-xml-11", false).booleanValue()));
    }

    protected Writer getWriter(OutputStream outputStream, Config config) {
        try {
            return new OutputStreamWriter(outputStream, config.encoding);
        } catch (UnsupportedEncodingException e) {
            throw new OXFException(e);
        }
    }
}
