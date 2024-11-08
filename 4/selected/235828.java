package net.disy.legato.tools.servlet;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

public class ResourceServlet extends HttpServlet {

    private static final long serialVersionUID = -8451830693210613895L;

    private static final String RESOURCE_PARAMETER_NAME = "RESOURCE";

    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final String resource = request.getParameter(RESOURCE_PARAMETER_NAME);
        if (resource == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource parameter is not specified.");
        } else {
            final InputStream is;
            if (resource.startsWith("/")) {
                is = getClass().getClassLoader().getResourceAsStream(resource.substring(1));
            } else {
                is = getClass().getResourceAsStream(resource);
            }
            if (is == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource [" + resource + "] could not be found.");
            } else {
                renderResource(request, response, is);
            }
        }
    }

    protected void renderResource(final HttpServletRequest request, final HttpServletResponse response, final InputStream is) throws IOException {
        try {
            IOUtils.copy(is, response.getOutputStream());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
