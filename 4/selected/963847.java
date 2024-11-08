package net.sf.webwarp.util.web;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServletResponse;
import net.sf.webwarp.util.types.MimeType;
import net.sf.webwarp.util.web.HttpServletFileDownload;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletOutputStream;
import org.springframework.mock.web.MockHttpServletResponse;

public class HttpServletFileDownloadTest {

    @Test
    public void testDownloadFileHttpServletResponseFile() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpServletFileDownload.downloadFile(response, new File("src/test/resources/test-document.pdf"), MimeType.PDF.getMimeType(), true);
        Assert.assertArrayEquals(getBytes("/test-document.pdf"), getBytes(response));
        Assert.assertEquals("attachment; filename=\"test-document.pdf\"", response.getHeader("Content-disposition"));
        Assert.assertEquals(MimeType.PDF.getMimeType(), response.getContentType());
        Assert.assertEquals(getBytes("/test-document.pdf").length, response.getContentLength());
    }

    @Test
    public void testDownloadFileHttpServletResponseInputStream() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpServletFileDownload.downloadFile(response, HttpServletFileDownloadTest.class.getResourceAsStream("/test-document.pdf"), "test-document.pdf", MimeType.PDF.getMimeType(), true);
        Assert.assertArrayEquals(getBytes("/test-document.pdf"), getBytes(response));
        Assert.assertEquals("attachment; filename=\"test-document.pdf\"", response.getHeader("Content-disposition"));
        Assert.assertEquals(MimeType.PDF.getMimeType(), response.getContentType());
        Assert.assertEquals(getBytes("/test-document.pdf").length, response.getContentLength());
    }

    @Test
    public void testDownloadFileHttpServletResponseByteArray() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpServletFileDownload.downloadFile(response, getBytes("/test-document.pdf"), "test-document.pdf", MimeType.PDF.getMimeType(), true);
        Assert.assertArrayEquals(getBytes("/test-document.pdf"), getBytes(response));
        Assert.assertEquals("attachment; filename=\"test-document.pdf\"", response.getHeader("Content-disposition"));
        Assert.assertEquals(MimeType.PDF.getMimeType(), response.getContentType());
        Assert.assertEquals(getBytes("/test-document.pdf").length, response.getContentLength());
    }

    private byte[] getBytes(String resource) throws IOException {
        InputStream is = HttpServletFileDownloadTest.class.getResourceAsStream(resource);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(is, out);
        return out.toByteArray();
    }

    private byte[] getBytes(HttpServletResponse response) throws IOException {
        DelegatingServletOutputStream outputStream = (DelegatingServletOutputStream) response.getOutputStream();
        ByteArrayOutputStream stream = (ByteArrayOutputStream) outputStream.getTargetStream();
        return stream.toByteArray();
    }
}
