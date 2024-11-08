package net.sf.webwarp.util.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import net.sf.webwarp.util.types.MimeType;
import net.sf.webwarp.util.web.FolderMappingServlet;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletOutputStream;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;

public class FolderMappingServletTest {

    @Test
    public void testDoGet() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setPathInfo("/test-document.pdf");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockServletConfig config = new MockServletConfig();
        config.addInitParameter(FolderMappingServlet.MAPPED_FOLDER_KEY, "file:src/test/resources");
        FolderMappingServlet servlet = new FolderMappingServlet();
        servlet.init(config);
        servlet.doGet(request, response);
        Assert.assertArrayEquals(getBytes("/test-document.pdf"), getBytes(response));
        Assert.assertEquals(MimeType.PDF.getMimeType(), response.getContentType());
    }

    @Test(expected = ServletException.class)
    public void testNoServletConfig() throws ServletException, IOException {
        MockServletConfig config = new MockServletConfig();
        FolderMappingServlet servlet = new FolderMappingServlet();
        servlet.init(config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoPathInfo() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockServletConfig config = new MockServletConfig();
        config.addInitParameter(FolderMappingServlet.MAPPED_FOLDER_KEY, "file:src/test/resources");
        FolderMappingServlet servlet = new FolderMappingServlet();
        servlet.init(config);
        servlet.doGet(request, response);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongMimeType() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setPathInfo("/test-document.xxx");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockServletConfig config = new MockServletConfig();
        config.addInitParameter(FolderMappingServlet.MAPPED_FOLDER_KEY, "file:src/test/resources");
        FolderMappingServlet servlet = new FolderMappingServlet();
        servlet.init(config);
        servlet.doGet(request, response);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileNotExists() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setPathInfo("/notExists.pdf");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockServletConfig config = new MockServletConfig();
        config.addInitParameter(FolderMappingServlet.MAPPED_FOLDER_KEY, "file:src/test/resources");
        FolderMappingServlet servlet = new FolderMappingServlet();
        servlet.init(config);
        servlet.doGet(request, response);
    }

    private byte[] getBytes(String resource) throws IOException {
        InputStream is = HttpServletFileDownloadTest.class.getResourceAsStream(resource);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(is, out);
        IOUtils.closeQuietly(is);
        return out.toByteArray();
    }

    private byte[] getBytes(HttpServletResponse response) throws IOException {
        DelegatingServletOutputStream outputStream = (DelegatingServletOutputStream) response.getOutputStream();
        ByteArrayOutputStream stream = (ByteArrayOutputStream) outputStream.getTargetStream();
        return stream.toByteArray();
    }
}
