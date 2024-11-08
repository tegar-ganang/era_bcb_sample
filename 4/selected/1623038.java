package soapdust;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.xml.parsers.ParserConfigurationException;
import junit.framework.TestCase;
import org.xml.sax.SAXException;

public class ClientResponseManagementTest extends TestCase {

    private Client client;

    @Override
    protected void setUp() throws Exception {
        Client.activeTraceMode(false);
        client = new Client();
        client.setWsdlUrl("file:test/soapdust/test.wsdl");
    }

    public void testThrowsFaultExceptionInCaseOfFaultResponse() throws IOException, MalformedResponseException, MalformedWsdlException {
        client.setEndPoint("test:status:500;file:test/soapdust/response-with-fault.xml");
        try {
            client.call("testOperation1");
            fail("FaultResponseException expected");
        } catch (FaultResponseException faultException) {
            assertEquals(500, faultException.responseCode);
            ComposedValue fault = faultException.fault;
            assertEquals("soapenv:Server.userException", fault.getStringValue("faultcode"));
            assertEquals("com.atlassian.jira.rpc.exception.RemoteAuthenticationException: Invalid username or password.", fault.getStringValue("faultstring"));
        }
    }

    public void testParseBodyWithHref() throws IOException, SAXException, ParserConfigurationException, FaultResponseException, MalformedResponseException {
        client.setEndPoint("test:file:test/soapdust/response-with-href.xml");
        ComposedValue result = client.call("testOperation1");
        ComposedValue return0 = result.getComposedValue("getIssuesFromFilterResponse").getComposedValue("getIssuesFromFilterReturn").getComposedValue("getIssuesFromFilterReturn");
        ComposedValue return1 = result.getComposedValue("getIssuesFromFilterResponse").getComposedValue("getIssuesFromFilterReturn").getComposedValue("getIssuesFromFilterReturn1");
        assertEquals("This is test 1", return0.getStringValue("summary"));
        assertEquals("This is test 2", return1.getStringValue("summary"));
        assertSame(return0.getComposedValue("affectsVersions"), return1.getComposedValue("affectsVersions"));
    }

    public void testEmptyNodeResultInNullStringOrComposedValueAndDoesNotFail() throws IOException, SAXException, ParserConfigurationException, FaultResponseException, MalformedResponseException {
        client.setEndPoint("test:file:test/soapdust/response-with-empty-nodes.xml");
        ComposedValue result = client.call("testOperation1");
        ComposedValue version = result.getComposedValue("getIssuesFromFilterResponse").getComposedValue("getIssuesFromFilterReturn").getComposedValue("affectsVersions");
        assertNull(version.getStringValue("name"));
        assertNull(version.getComposedValue("name"));
    }

    public void testUnhandledttpStatusThrowsMalformedResponseException() throws FaultResponseException, IOException {
        client.setEndPoint("test:status:153;file:test/soapdust/response-with-href.xml");
        try {
            client.call("testOperation1");
            fail("MalformedResponseException expected");
        } catch (MalformedResponseException e) {
            assertEquals(153, e.responseCode);
        }
    }

    public void testUnhandledHttpStatusStoresReceivedDataInException() throws IOException, FaultResponseException {
        client.setEndPoint("test:status:153;file:test/soapdust/response-with-href.xml");
        try {
            client.call("testOperation1");
            fail("MalformedResponseException expected");
        } catch (MalformedResponseException e) {
            assertTrue(Arrays.equals(readFile("test/soapdust/response-with-href.xml"), e.response));
        }
    }

    public void testParsingMalformedResponseThrowsException() throws IOException, FaultResponseException, MalformedWsdlException {
        Client.activeTraceMode(false);
        client.setEndPoint("test:file:test/soapdust/response-malformed.xml");
        try {
            client.call("testOperation1");
            fail("MalformedResponseException expected");
        } catch (MalformedResponseException e) {
            assertNull(e.response);
        }
    }

    public void testParsingMalformedFaultResponseThrowsException() throws IOException, FaultResponseException, MalformedWsdlException {
        Client.activeTraceMode(false);
        client.setEndPoint("test:status:500;file:test/soapdust/response-malformed.xml");
        try {
            client.call("testOperation1");
            fail("MalformedResponseException expected");
        } catch (MalformedResponseException e) {
            assertNull(e.response);
        }
    }

    public void testParsingMalformedResponseStoresReceivedDataInExceptionWhenTraceModeActivated() throws IOException, FaultResponseException, MalformedWsdlException {
        Client.activeTraceMode(true);
        client.setEndPoint("test:file:test/soapdust/response-malformed.xml");
        try {
            client.call("testOperation1");
            fail("MalformedResponseException expected");
        } catch (MalformedResponseException e) {
            assertTrue(Arrays.equals(readFile("test/soapdust/response-malformed.xml"), e.response));
        }
    }

    public void testParsingMalformedFaultResponseStoresReceivedDataInExceptionWhenTraceModeActivated() throws IOException, FaultResponseException, MalformedWsdlException {
        Client.activeTraceMode(true);
        client.setEndPoint("test:status:500;file:test/soapdust/response-malformed.xml");
        try {
            client.call("testOperation1");
            fail("MalformedResponseException expected");
        } catch (MalformedResponseException e) {
            assertTrue(Arrays.equals(readFile("test/soapdust/response-malformed.xml"), e.response));
        }
    }

    private byte[] readFile(String file) throws IOException {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        FileInputStream in = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        for (int read = in.read(buffer, 0, buffer.length); read != -1; read = in.read(buffer, 0, buffer.length)) {
            content.write(buffer, 0, read);
        }
        return content.toByteArray();
    }
}
