package net.sourceforge.traffiscope.model.xml;

import org.junit.Test;
import java.io.StringReader;
import java.io.StringWriter;

public class TraffiscopeXOTest {

    @Test
    public void testMarshall() throws Exception {
        StringWriter writer = new StringWriter();
        TraffiscopeXO.marshal(TraffiscopeHelper.unmarshallTestData(), writer);
        writer.close();
        StringReader reader = new StringReader(writer.toString());
        TraffiscopeHelper.assertEquals(TraffiscopeHelper.TESTDATA, TraffiscopeXO.unmarshal(reader));
    }

    @Test
    public void testUnmarshall() throws Exception {
        TraffiscopeHelper.assertEquals(TraffiscopeHelper.TESTDATA, TraffiscopeHelper.unmarshallTestData());
    }
}
