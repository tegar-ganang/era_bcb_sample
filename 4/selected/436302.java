package flattree.xml.stax;

import java.io.InputStreamReader;
import java.io.StringWriter;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import flattree.xml.AbstractXMLTest;

/**
 * Test for {@link FlatStreamReader}.
 */
public class FlatStreamReaderTest extends AbstractXMLTest {

    public void test() throws Exception {
        FlatStreamReader reader = new FlatStreamReader(getRoot(), new InputStreamReader(getFlat()));
        StringWriter writer = new StringWriter();
        generate(new StAXSource(reader), new StreamResult(writer));
        assertEqual(getXML(), writer.toString());
    }
}
