package org.orbeon.oxf.processor.validation;

import com.sun.msv.verifier.jarv.TheFactoryImpl;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.iso_relax.verifier.*;
import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.common.ValidationException;
import org.orbeon.oxf.processor.*;
import org.orbeon.oxf.processor.generator.DOMGenerator;
import org.orbeon.oxf.resources.URLFactory;
import org.orbeon.oxf.util.LoggerFactory;
import org.orbeon.oxf.xml.TeeContentHandler;
import org.orbeon.oxf.xml.XMLUtils;
import org.orbeon.oxf.xml.XPathUtils;
import org.orbeon.oxf.xml.dom4j.DocumentDelegate;
import org.orbeon.oxf.xml.dom4j.ElementDelegate;
import org.orbeon.oxf.xml.dom4j.LocationData;
import org.xml.sax.*;
import org.xml.sax.helpers.AttributesImpl;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class MSVValidationProcessor extends ProcessorImpl {

    private Logger logger = LoggerFactory.createLogger(MSVValidationProcessor.class);

    public static final String ORBEON_ERROR_NS = "http://orbeon.org/oxf/xml/validation";

    public static final String ORBEON_ERROR_PREFIX = "v";

    public static final String ERROR_ELEMENT = "error";

    public static final String MESSAGE_ATTRIBUTE = "message";

    public static final String SYSTEMID_ATTRIBUTE = "system-id";

    public static final String LINE_ATTRIBUTE = "line";

    public static final String COLUMN_ATTRIBUTE = "column";

    public static final String INPUT_SCHEMA = "schema";

    private String schemaId;

    public static DOMGenerator NO_DECORATION_CONFIG;

    public static DOMGenerator DECORATION_CONFIG;

    static {
        NO_DECORATION_CONFIG = new DOMGenerator(new DocumentDelegate(new ElementDelegate("config") {

            {
                add(new ElementDelegate("decorate") {

                    {
                        setText("false");
                    }
                });
            }
        }));
        DECORATION_CONFIG = new DOMGenerator(new DocumentDelegate(new ElementDelegate("config") {

            {
                add(new ElementDelegate("decorate") {

                    {
                        setText("true");
                    }
                });
            }
        }));
    }

    public MSVValidationProcessor() {
        addInputInfo(new ProcessorInputOutputInfo(INPUT_DATA));
        addInputInfo(new ProcessorInputOutputInfo(INPUT_CONFIG));
        addInputInfo(new ProcessorInputOutputInfo(INPUT_SCHEMA));
        addOutputInfo(new ProcessorInputOutputInfo(OUTPUT_DATA));
    }

    private static SAXParserFactory factory = null;

    private static synchronized SAXParserFactory getFactory() {
        if (factory == null) factory = XMLUtils.createSAXParserFactory(false);
        return factory;
    }

    /**
     * Creates a validation processor that decorate the output tree with error attribute if there is a validation error.
     * The default behaviour is to throw a ValidationException
     **/
    public MSVValidationProcessor(String schemaId) {
        this();
        this.schemaId = schemaId;
    }

    public ProcessorOutput createOutput(String name) {
        ProcessorOutput output = new ProcessorImpl.CacheableTransformerOutputImpl(getClass(), name) {

            protected void readImpl(org.orbeon.oxf.pipeline.api.PipelineContext context, final ContentHandler contentHandler) {
                try {
                    Document configDoc = readCacheInputAsDOM4J(context, INPUT_CONFIG);
                    final boolean decorateOutput = Boolean.valueOf(XPathUtils.selectStringValueNormalize(configDoc, "/config/decorate")).booleanValue();
                    Schema schema = (Schema) readCacheInputAsObject(context, getInputByName(INPUT_SCHEMA), new CacheableInputReader() {

                        public Object read(org.orbeon.oxf.pipeline.api.PipelineContext context, ProcessorInput input) {
                            try {
                                long time = 0;
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Reading Schema: " + schemaId);
                                    time = System.currentTimeMillis();
                                }
                                Document schemaDoc = readInputAsDOM4J(context, input);
                                LocationData locator = (LocationData) schemaDoc.getRootElement().getData();
                                final String schemaSystemId = (locator != null && locator.getSystemID() != null) ? locator.getSystemID() : null;
                                VerifierFactory verifierFactory = new TheFactoryImpl(getFactory());
                                verifierFactory.setEntityResolver(new EntityResolver() {

                                    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                                        URL url = URLFactory.createURL(schemaSystemId, systemId);
                                        InputSource i = new InputSource(url.openStream());
                                        i.setSystemId(url.toString());
                                        return i;
                                    }
                                });
                                InputSource is = new InputSource(new StringReader(XMLUtils.domToString(schemaDoc)));
                                is.setSystemId(schemaSystemId);
                                synchronized (MSVValidationProcessor.class) {
                                    Schema schema = verifierFactory.compileSchema(is);
                                    if (logger.isDebugEnabled()) logger.debug(schemaId + " : Schema compiled in " + (System.currentTimeMillis() - time));
                                    return schema;
                                }
                            } catch (VerifierConfigurationException vce) {
                                throw new OXFException(vce.getCauseException());
                            } catch (Exception e) {
                                throw new OXFException(e);
                            }
                        }
                    });
                    Verifier verifier = schema.newVerifier();
                    verifier.setErrorHandler(new org.xml.sax.ErrorHandler() {

                        private void generateErrorElement(ValidationException ve) throws SAXException {
                            if (decorateOutput && ve != null) {
                                String systemId = ve.getLocationData().getSystemID();
                                AttributesImpl a = new AttributesImpl();
                                a.addAttribute("", MESSAGE_ATTRIBUTE, MESSAGE_ATTRIBUTE, "CDATA", ve.getSimpleMessage());
                                a.addAttribute("", SYSTEMID_ATTRIBUTE, SYSTEMID_ATTRIBUTE, "CDATA", systemId == null ? "" : systemId);
                                a.addAttribute("", LINE_ATTRIBUTE, LINE_ATTRIBUTE, "CDATA", Integer.toString(ve.getLocationData().getLine()));
                                a.addAttribute("", COLUMN_ATTRIBUTE, COLUMN_ATTRIBUTE, "CDATA", Integer.toString(ve.getLocationData().getCol()));
                                contentHandler.startElement(ORBEON_ERROR_NS, ERROR_ELEMENT, ORBEON_ERROR_PREFIX + ":" + ERROR_ELEMENT, a);
                                contentHandler.endElement(ORBEON_ERROR_NS, ERROR_ELEMENT, ORBEON_ERROR_PREFIX + ":" + ERROR_ELEMENT);
                            } else {
                                throw ve;
                            }
                        }

                        public void error(SAXParseException exception) throws SAXException {
                            generateErrorElement(new ValidationException("Error " + exception.getMessage() + "(schema: " + schemaId + ")", new LocationData(exception)));
                        }

                        public void fatalError(SAXParseException exception) throws SAXException {
                            generateErrorElement(new ValidationException("Fatal Error " + exception.getMessage() + "(schema: " + schemaId + ")", new LocationData(exception)));
                        }

                        public void warning(SAXParseException exception) throws SAXException {
                            generateErrorElement(new ValidationException("Warning " + exception.getMessage() + "(schema: " + schemaId + ")", new LocationData(exception)));
                        }
                    });
                    VerifierHandler verifierHandler = verifier.getVerifierHandler();
                    List dest = Arrays.asList(new Object[] { verifierHandler, contentHandler });
                    long time = 0;
                    if (logger.isDebugEnabled()) {
                        time = System.currentTimeMillis();
                    }
                    readInputAsSAX(context, getInputByName(INPUT_DATA), new TeeContentHandler(dest));
                    if (logger.isDebugEnabled()) {
                        logger.debug(schemaId + " validation completed in " + (System.currentTimeMillis() - time));
                    }
                } catch (Exception e) {
                    throw new OXFException(e);
                }
            }
        };
        addOutput(name, output);
        return output;
    }
}
