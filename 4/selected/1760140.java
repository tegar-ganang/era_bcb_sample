package com.google.code.gwtosgi.plugins.descriptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

/**
 * @author a108600
 *
 */
public class GwtServlet extends HttpServlet {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private static final String CLASSPATH_PREFIX = "war";

    private final transient ClassLoader cl;

    private String pluginKey;

    private String prefix;

    /**
	 * 
	 */
    public GwtServlet(ClassLoader cl, String pluginKey) {
        super();
        this.cl = cl;
        this.pluginKey = pluginKey;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        prefix = config.getInitParameter(GwtModuleDescriptor.PREFIX_INIT_PARAM);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        String pluginPathInfo = pathInfo.substring(prefix.length());
        String gwtPathInfo = pluginPathInfo.substring(pluginKey.length() + 1);
        String clPath = CLASSPATH_PREFIX + gwtPathInfo;
        InputStream input = cl.getResourceAsStream(clPath);
        if (input != null) {
            try {
                OutputStream output = resp.getOutputStream();
                IOUtils.copy(input, output);
            } finally {
                input.close();
            }
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
