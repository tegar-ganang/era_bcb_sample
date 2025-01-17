package org.apache.shindig.gadgets.servlet;

import static junitx.framework.StringAssert.assertContains;
import static junitx.framework.StringAssert.assertStartsWith;
import static org.easymock.EasyMock.expect;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Collections;
import java.util.Enumeration;
import javax.servlet.http.HttpServletResponse;

/**
 * Tests for MakeRequestServlet.
 *
 * Tests are trivial; real tests are in MakeRequestHandlerTest.
 */
public class MakeRequestServletTest extends ServletTestFixture {

    private static final Uri REQUEST_URL = Uri.parse("http://example.org/file");

    private static final String RESPONSE_BODY = "Hello, world!";

    private static final String ERROR_MESSAGE = "Broken!";

    private static final Enumeration<String> EMPTY_ENUM = Collections.enumeration(Collections.<String>emptyList());

    private final MakeRequestServlet servlet = new MakeRequestServlet();

    private final MakeRequestHandler handler = new MakeRequestHandler(pipeline, null);

    private final HttpRequest internalRequest = new HttpRequest(REQUEST_URL);

    private final HttpResponse internalResponse = new HttpResponse(RESPONSE_BODY);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        servlet.setMakeRequestHandler(handler);
        expect(request.getHeaderNames()).andReturn(EMPTY_ENUM).anyTimes();
        expect(request.getParameter(MakeRequestHandler.METHOD_PARAM)).andReturn("GET").anyTimes();
        expect(request.getParameter(ProxyBase.URL_PARAM)).andReturn(REQUEST_URL.toString()).anyTimes();
    }

    private void setupGet() {
        expect(request.getMethod()).andReturn("GET").anyTimes();
    }

    private void setupPost() {
        expect(request.getMethod()).andReturn("POST").anyTimes();
    }

    private void assertResponseOk(int expectedStatus, String expectedBody) throws JSONException {
        if (recorder.getHttpStatusCode() == HttpServletResponse.SC_OK) {
            String body = recorder.getResponseAsString();
            assertStartsWith(MakeRequestHandler.UNPARSEABLE_CRUFT, body);
            body = body.substring(MakeRequestHandler.UNPARSEABLE_CRUFT.length());
            JSONObject object = new JSONObject(body);
            object = object.getJSONObject(REQUEST_URL.toString());
            assertEquals(expectedStatus, object.getInt("rc"));
            assertEquals(expectedBody, object.getString("body"));
        } else {
            fail("Invalid response for request.");
        }
    }

    public void testDoGetNormal() throws Exception {
        setupGet();
        expect(pipeline.execute(internalRequest)).andReturn(internalResponse);
        replay();
        servlet.doGet(request, recorder);
        assertResponseOk(HttpResponse.SC_OK, RESPONSE_BODY);
    }

    public void testDoGetHttpError() throws Exception {
        setupGet();
        expect(pipeline.execute(internalRequest)).andReturn(HttpResponse.notFound());
        replay();
        servlet.doGet(request, recorder);
        assertResponseOk(HttpResponse.SC_NOT_FOUND, "");
    }

    public void testDoGetException() throws Exception {
        setupGet();
        expect(pipeline.execute(internalRequest)).andThrow(new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT, ERROR_MESSAGE));
        replay();
        servlet.doGet(request, recorder);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, recorder.getHttpStatusCode());
        assertContains(ERROR_MESSAGE, recorder.getResponseAsString());
    }

    public void testDoPostNormal() throws Exception {
        setupPost();
        expect(pipeline.execute(internalRequest)).andReturn(internalResponse);
        replay();
        servlet.doPost(request, recorder);
        assertResponseOk(HttpResponse.SC_OK, RESPONSE_BODY);
    }

    public void testDoPostHttpError() throws Exception {
        setupPost();
        expect(pipeline.execute(internalRequest)).andReturn(HttpResponse.notFound());
        replay();
        servlet.doGet(request, recorder);
        assertResponseOk(HttpResponse.SC_NOT_FOUND, "");
    }

    public void testDoPostException() throws Exception {
        setupPost();
        expect(pipeline.execute(internalRequest)).andThrow(new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT, ERROR_MESSAGE));
        replay();
        servlet.doPost(request, recorder);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, recorder.getHttpStatusCode());
        assertContains(ERROR_MESSAGE, recorder.getResponseAsString());
    }
}
