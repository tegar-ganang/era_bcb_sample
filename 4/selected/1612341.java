package com.stephenduncanjr.xpathreplacement;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

/**
 * Test for XPathReplacement.
 * 
 * @author stephen.duncan (Stephen C. Duncan Jr.
 *         &lt;stephen.duncan@gmail.com&gt;)
 * @since 1.0
 */
public class XPathReplacemetTest {

    /** Original value. */
    private static final String REPLACED_VALUE = "english";

    /** New value. */
    private static final String REPLACEMENT_VALUE = "german";

    /** Test attribute input. */
    private static final String TEST_ATTRIBUTE_XML_INPUT = "<country language=\"english\" />";

    /** Test with namespace input. */
    private static final String TEST_NAMESPACE_XML_INPUT = "<country xmlns=\"http://example.com/country\"><language>english</language></country>";

    /** Test input. */
    private static final String TEST_XML_INPUT = "<country><language>english</language></country>";

    /** Reader for input. */
    private Reader reader;

    /** Writer for output. */
    private Writer writer;

    /**
	 * Sets up for tests.
	 */
    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        this.writer = new StringWriter();
    }

    /**
	 * Test replaceXPath.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws XPathExpressionException
	 * @throws TransformerException
	 * @throws TransformerFactoryConfigurationError
	 */
    @Test(groups = "unit")
    public void testXPathReplacement() throws XPathExpressionException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException {
        this.reader = new StringReader(TEST_XML_INPUT);
        String xpath = "/country/language/text()";
        XPathReplacement.replace(xpath, REPLACEMENT_VALUE, this.reader, this.writer);
        String result = this.writer.toString();
        assertTrue(result.contains(REPLACEMENT_VALUE));
        assertFalse(result.contains(REPLACED_VALUE));
    }

    /**
	 * Test replaceXPath.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws XPathExpressionException
	 * @throws TransformerException
	 * @throws TransformerFactoryConfigurationError
	 */
    @Test(groups = "unit")
    public void testXPathReplacementAttribute() throws XPathExpressionException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException {
        this.reader = new StringReader(TEST_ATTRIBUTE_XML_INPUT);
        String xpath = "/country/@language";
        XPathReplacement.replace(xpath, REPLACEMENT_VALUE, this.reader, this.writer);
        String result = this.writer.toString();
        assertTrue(result.contains(REPLACEMENT_VALUE));
        assertFalse(result.contains(REPLACED_VALUE));
    }

    /**
	 * Test replaceXPath with namespaces.
	 * 
	 * @throws TransformerException
	 * @throws TransformerFactoryConfigurationError
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws XPathExpressionException
	 */
    @Test(groups = "unit")
    public void testXPathReplacementNamespace() throws XPathExpressionException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException {
        this.reader = new StringReader(TEST_NAMESPACE_XML_INPUT);
        String xpath = "/pref:country/pref:language/text()";
        Map<String, String> prefixToNamespace = new HashMap<String, String>();
        prefixToNamespace.put("pref", "http://example.com/country");
        XPathReplacement.replace(xpath, prefixToNamespace, REPLACEMENT_VALUE, this.reader, this.writer);
        String result = this.writer.toString();
        assertTrue(result.contains(REPLACEMENT_VALUE));
        assertFalse(result.contains(REPLACED_VALUE));
    }
}
