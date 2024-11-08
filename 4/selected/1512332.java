package flattree.xml.sax;

import java.io.StringWriter;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import flattree.xml.AbstractXMLTest;

/**
 * Test for {@link FlatXMLReader}.
 */
public class FlatXMLReaderTest extends AbstractXMLTest {

    public void test() throws Exception {
        XMLReader reader = new FlatXMLReader(getRoot());
        StringWriter writer = new StringWriter();
        generate(new SAXSource(reader, new InputSource(getFlat())), new StreamResult(writer));
        assertEqual(getXML(), writer.toString());
    }
}
