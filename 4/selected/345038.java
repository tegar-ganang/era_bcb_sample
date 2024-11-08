package net.admin4j.ui.filters;

import java.io.ByteArrayOutputStream;
import junit.framework.Assert;
import net.admin4j.deps.commons.io.IOUtils;
import net.admin4j.deps.commons.lang3.ArrayUtils;
import net.admin4j.ui.MockResponse;
import net.admin4j.util.notify.NotifierTestingMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPerformanceTimeStampingFilter extends BaseFilterTestSupport {

    private PerformanceTimeStampingFilter filter;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.mockConfig.addInitParameter("notifier", NotifierTestingMock.class.getName());
        filter = new PerformanceTimeStampingFilter();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testHtml() throws Exception {
        filter.init(this.mockConfig);
        MockFilterChain mockChain = new MockFilterChain();
        mockChain.setContentType("text/html");
        mockChain.setOutputData("<html><body></body></html>".getBytes());
        MockResponse mockResponse = new MockResponse();
        filter.doFilter(this.mockRequest, mockResponse, mockChain);
        Assert.assertTrue("Time stamp content type", "text/html".equals(mockResponse.getContentType()));
        String responseStr = new String(mockResponse.getMockServletOutputStream().getBytes());
        Assert.assertTrue("Time stamp present", responseStr.indexOf("server side time:") > 0);
    }

    @Test
    public void testOther() throws Exception {
        filter.init(this.mockConfig);
        ByteArrayOutputStream jpg = new ByteArrayOutputStream();
        IOUtils.copy(this.getClass().getResourceAsStream("Buffalo-Theory.jpg"), jpg);
        MockFilterChain mockChain = new MockFilterChain();
        mockChain.setContentType("image/jpg");
        mockChain.setOutputData(jpg.toByteArray());
        MockResponse mockResponse = new MockResponse();
        filter.doFilter(this.mockRequest, mockResponse, mockChain);
        Assert.assertTrue("Time stamp content type", "image/jpg".equals(mockResponse.getContentType()));
        Assert.assertTrue("OutputStream as original", ArrayUtils.isEquals(jpg.toByteArray(), mockResponse.getMockServletOutputStream().getBytes()));
    }
}
