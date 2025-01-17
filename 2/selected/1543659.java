package org.apache.shindig.gadgets.servlet;

import static junitx.framework.StringAssert.assertContains;
import static org.easymock.EasyMock.expect;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Tests for ProxyServlet.
 *
 * Tests are trivial; real tests are in ProxyHandlerTest.
 */
public class ProxyServletTest extends ServletTestFixture {

    private static final String REQUEST_DOMAIN = "example.org";

    private static final Uri REQUEST_URL = Uri.parse("http://example.org/file");

    private static final String BASIC_SYNTAX_URL = "http://opensocial.org/proxy?foo=bar&url=" + REQUEST_URL;

    private static final String ALT_SYNTAX_URL = "http://opensocial.org/proxy/foo=bar/" + REQUEST_URL;

    private static final String RESPONSE_BODY = "Hello, world!";

    private static final String ERROR_MESSAGE = "Broken!";

    private final ProxyHandler proxyHandler = new ProxyHandler(pipeline, lockedDomainService, null);

    private final ProxyServlet servlet = new ProxyServlet();

    private final HttpRequest internalRequest = new HttpRequest(REQUEST_URL);

    private final HttpResponse internalResponse = new HttpResponse(RESPONSE_BODY);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        servlet.setProxyHandler(proxyHandler);
        expect(request.getParameter(ProxyBase.URL_PARAM)).andReturn(REQUEST_URL.toString()).anyTimes();
        expect(request.getHeader("Host")).andReturn(REQUEST_DOMAIN).anyTimes();
        expect(lockedDomainService.isSafeForOpenProxy(REQUEST_DOMAIN)).andReturn(true).anyTimes();
    }

    private void setupBasic() {
        expect(request.getRequestURI()).andReturn(BASIC_SYNTAX_URL);
    }

    private void setupAltSyntax() {
        expect(request.getRequestURI()).andReturn(ALT_SYNTAX_URL);
    }

    private void assertResponseOk(int expectedStatus, String expectedBody) {
        assertEquals(expectedStatus, recorder.getHttpStatusCode());
        assertEquals(expectedBody, recorder.getResponseAsString());
    }

    public void testDoGetNormal() throws Exception {
        setupBasic();
        expect(pipeline.execute(internalRequest)).andReturn(internalResponse);
        replay();
        servlet.doGet(request, recorder);
        assertResponseOk(HttpResponse.SC_OK, RESPONSE_BODY);
    }

    public void testDoGetHttpError() throws Exception {
        setupBasic();
        expect(pipeline.execute(internalRequest)).andReturn(HttpResponse.notFound());
        replay();
        servlet.doGet(request, recorder);
        assertResponseOk(HttpResponse.SC_NOT_FOUND, "");
    }

    public void testDoGetException() throws Exception {
        setupBasic();
        expect(pipeline.execute(internalRequest)).andThrow(new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT, ERROR_MESSAGE));
        replay();
        servlet.doGet(request, recorder);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, recorder.getHttpStatusCode());
        assertContains(ERROR_MESSAGE, recorder.getResponseAsString());
    }

    public void testDoGetAlternateSyntax() throws Exception {
        setupAltSyntax();
        expect(request.getRequestURI()).andReturn(ALT_SYNTAX_URL);
        expect(pipeline.execute(internalRequest)).andReturn(internalResponse);
        replay();
        servlet.doGet(request, recorder);
        assertResponseOk(HttpResponse.SC_OK, RESPONSE_BODY);
    }
}
