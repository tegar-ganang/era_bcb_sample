package net.sf.nanomvc.core.render;

import java.io.InputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

public class InputStreamRenderer implements Renderer<InputStream> {

    public static final String name = "inputStream";

    public void render(final HttpServletRequest request, final HttpServletResponse response, InputStream inputStream, final Throwable t, final String contentType, final String encoding) throws Exception {
        if (contentType != null) {
            response.setContentType(contentType);
        }
        if (encoding != null) {
            response.setCharacterEncoding(encoding);
        }
        IOUtils.copy(inputStream, response.getOutputStream());
    }

    public boolean accepts(Class<?> clazz) {
        return InputStream.class.isAssignableFrom(clazz);
    }
}
