package org.opennms.secret.web;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.jrobin.core.RrdException;
import org.opennms.secret.model.GraphDefinition;
import org.opennms.secret.service.GraphRenderer;
import org.springframework.web.servlet.View;

public class GraphRendererView implements View {

    private GraphRenderer m_renderer;

    private static final String s_contentType = "image/png";

    public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType(s_contentType);
        response.setHeader("Cache-control", "no-cache");
        InputStream graphStream = getGraphStream(request);
        OutputStream out = getOutputStream(response);
        IOUtils.copy(graphStream, out);
        out.flush();
    }

    private InputStream getGraphStream(HttpServletRequest request) throws IOException, RrdException {
        Object o = request.getSession().getAttribute("graphDef");
        if (m_renderer == null) {
            throw new IllegalStateException("graph renderer has not been set with setGraphRenderer");
        }
        if (o == null) {
            throw new IllegalStateException("session has no \"graphDef\" attribute, or it is null");
        }
        if (!(o instanceof GraphDefinition)) {
            throw new IllegalStateException("\"graphDef\" session attribute is not an instance of " + GraphDefinition.class.getName());
        }
        GraphDefinition graphDef = (GraphDefinition) o;
        InputStream graphStream = m_renderer.getPNG(graphDef);
        return graphStream;
    }

    private OutputStream getOutputStream(HttpServletResponse response) throws IOException, FileNotFoundException {
        OutputStream servletOut = response.getOutputStream();
        OutputStream testOut = new FileOutputStream("/tmp/chart.png");
        TeeOutputStream out = new TeeOutputStream(servletOut, testOut);
        return out;
    }

    public void setGraphRenderer(GraphRenderer renderer) {
        m_renderer = renderer;
    }

    public String getContentType() {
        return s_contentType;
    }
}
