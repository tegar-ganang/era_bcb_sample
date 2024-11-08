package net.sf.nanomvc.core.render;

import java.io.ByteArrayInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

public class ByteArrayRenderer implements Renderer<byte[]> {

    public static final String name = "bytes";

    private static final Class<?> byteArrayClass = new byte[0].getClass();

    public void render(final HttpServletRequest request, final HttpServletResponse response, final byte[] bytes, final Throwable t, final String contentType, final String encoding) throws Exception {
        if (contentType != null) {
            response.setContentType(contentType);
        }
        if (encoding != null) {
            response.setCharacterEncoding(encoding);
        }
        response.setContentLength(bytes.length);
        IOUtils.copy(new ByteArrayInputStream(bytes), response.getOutputStream());
    }

    public boolean accepts(Class<?> clazz) {
        return byteArrayClass.equals(clazz);
    }
}
