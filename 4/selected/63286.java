package com.volantis.xml.pipeline.sax.drivers.web;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XMLAssert;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.CharArrayWriter;
import java.io.CharArrayReader;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import com.volantis.xml.xml.serialize.OutputFormat;
import com.volantis.xml.xml.serialize.XMLSerializer;
import com.volantis.xml.pipeline.sax.drivers.web.conditioners.HTMLResponseConditioner;
import com.volantis.xml.pipeline.sax.XMLHandlerAdapter;
import com.volantis.xml.pipeline.sax.XMLProcessImpl;
import com.volantis.xml.pipeline.sax.XMLProcess;
import com.volantis.xml.utilities.sax.XMLReaderFactory;

/**
 * This JUnit test class tests HTMLResponseConditioner
 */
public class HTMLResponseConditionerTestCase extends XMLTestCase {

    /**
     * The DOM factory class to use
     */
    public static final String DOM_FACTORY_CLASS = "com.volantis.xml.xerces.jaxp.DocumentBuilderFactoryImpl";

    /**
     * The SAX factory class to use
     */
    public static final String SAX_FACTORY_CLASS = "com.volantis.xml.xerces.jaxp.SAXParserFactoryImpl";

    /**
     * The Transformer factory class to use
     */
    public static final String TRANSFORMER_FACTORY_CLASS = "com.volantis.xml.xalan.processor.TransformerFactoryImpl";

    /**
     * The package that the class being tested belongs to.
     */
    protected String packagePath;

    /**
     * Create a new HTMLResponseConditionerTestCase instance
     * @param name the name of the test.
     */
    public HTMLResponseConditionerTestCase(String name) {
        super(name);
        packagePath = getClass().getPackage().getName().replace('.', '/');
    }

    protected void setUp() throws Exception {
        super.setUp();
        XMLUnit.setControlParser(DOM_FACTORY_CLASS);
        XMLUnit.setTestParser(DOM_FACTORY_CLASS);
        XMLUnit.setSAXParserFactory(SAX_FACTORY_CLASS);
        XMLUnit.setTransformerFactory(TRANSFORMER_FACTORY_CLASS);
        XMLUnit.setIgnoreWhitespace(false);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        XMLUnit.setIgnoreWhitespace(true);
    }

    public void testQuoteNbsp() throws Exception {
        doTestCondition("QuoteNbsp.input.xml", "QuoteNbsp.expected.xml");
    }

    /**
     * Condition the specified input file to check that it matches the
     * specified output.
     * <p/>
     * Both the input and expected file are treated as a resource relative to
     * the current package.
     * @param input           The relative path to the input file
     * @param expected        The relative path to the expected file
     * @throws Exception if problem encountered
     */
    public void doTestCondition(String input, String expected) throws Exception {
        URL inputURL = getResourceURL(packagePath + '/' + input);
        URL expectedURL = getResourceURL(packagePath + '/' + expected);
        URLConnection inputConnection = inputURL.openConnection();
        InputStream inputStream = inputConnection.getInputStream();
        URLConnection expectedConnection = expectedURL.openConnection();
        InputStream expectedStream = expectedConnection.getInputStream();
        doTestCondition(inputStream, expectedStream);
    }

    /**
     * XMLProcess the input through the pipeline and check to see whether
     * it matches the expected output.
     * @param input The reader for the input XML
     * @param expected The reader for the expected XML
     */
    public void doTestCondition(InputStream input, InputStream expected) throws Exception {
        CharArrayWriter outputWriter = new CharArrayWriter();
        OutputFormat format = new OutputFormat();
        format.setPreserveSpace(true);
        format.setOmitXMLDeclaration(true);
        XMLSerializer serializer = new XMLSerializer(format);
        serializer.setOutputCharStream(outputWriter);
        ContentHandler sHandler = serializer.asContentHandler();
        XMLHandlerAdapter adapter = new XMLHandlerAdapter();
        adapter.setContentHandler(sHandler);
        XMLReader parser = XMLReaderFactory.createXMLReader(false);
        XMLFilterImpl xmlFilter = new XMLFilterImpl(parser);
        HTMLResponseConditioner conditioner = new HTMLResponseConditioner(xmlFilter);
        XMLProcessImpl xmlProcess = new XMLProcessImpl() {

            public void setDocumentLocator(Locator locator) {
                XMLProcess consumer = getConsumerProcess();
                if (null != consumer) {
                    consumer.setDocumentLocator(locator);
                }
            }

            public void startDocument() throws SAXException {
                XMLProcess consumer = getConsumerProcess();
                if (null != consumer) {
                    consumer.startDocument();
                }
            }

            public void endDocument() throws SAXException {
                XMLProcess consumer = getConsumerProcess();
                if (null != consumer) {
                    consumer.endDocument();
                }
            }

            public void startPrefixMapping(String prefix, String uri) throws SAXException {
                XMLProcess consumer = getConsumerProcess();
                if (null != consumer) {
                    consumer.startPrefixMapping(prefix, uri);
                }
            }

            public void endPrefixMapping(String prefix) throws SAXException {
                XMLProcess consumer = getConsumerProcess();
                if (null != consumer) {
                    consumer.endPrefixMapping(prefix);
                }
            }
        };
        xmlProcess.setNextProcess(adapter);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int readBytes = input.read(buffer);
        while (readBytes != -1) {
            out.write(buffer, 0, readBytes);
            readBytes = input.read(buffer);
        }
        InputSource inputSource = new InputSource(new ByteArrayInputStream(out.toByteArray()));
        conditioner.condition(inputSource, xmlProcess);
        outputWriter.flush();
        char[] outputCharacters = outputWriter.toCharArray();
        String charsetName = "ISO-8859-1";
        XMLAssert.assertXMLEqual(new InputStreamReader(expected, charsetName), new CharArrayReader(outputCharacters));
    }

    /**
     * Returns a URL to a given named resource.
     * @param resource the resource to look for.
     * @return the URL to the resource
     */
    protected URL getResourceURL(String resource) {
        ClassLoader cl;
        URL url = null;
        cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            url = cl.getResource(resource);
        }
        if (url == null) {
            cl = getClass().getClassLoader();
            url = cl.getResource(resource);
        }
        if (url == null) {
            cl = ClassLoader.getSystemClassLoader();
            url = cl.getResource(resource);
        }
        return url;
    }
}
