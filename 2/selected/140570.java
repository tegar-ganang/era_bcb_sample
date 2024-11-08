package com.potix.web.servlet.dsp;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import com.potix.lang.D;
import com.potix.lang.Exceptions;
import com.potix.util.logging.Log;
import com.potix.util.resource.ResourceCache;
import com.potix.util.resource.Locator;
import com.potix.io.Files;
import com.potix.web.servlet.Charsets;
import com.potix.web.servlet.http.Https;
import com.potix.web.util.resource.ResourceCaches;
import com.potix.web.util.resource.ResourceLoader;
import com.potix.web.util.resource.ServletContextLocator;

/**
 * The servlet used to interpret the DSP file (Potix Dynamic Script Page).
 *
 * @author <a href="mailto:tomyeh@potix.com">tomyeh@potix.com</a>
 */
public class InterpreterServlet extends HttpServlet {

    private static final Log log = Log.lookup(InterpreterServlet.class);

    private ServletContext _ctx;

    public void init(ServletConfig config) throws ServletException {
        _ctx = config.getServletContext();
    }

    public ServletContext getServletContext() {
        return _ctx;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String path = Https.getThisServletPath(request);
        if (D.ON && log.debugable()) log.debug("Get " + path);
        final Object old = Charsets.setup(request, response);
        try {
            final Interpretation cnt = (Interpretation) ResourceCaches.get(getCache(_ctx), _ctx, path);
            if (cnt == null) {
                if (Https.isIncluded(request)) log.error("Not found: " + path);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            cnt.interpret(new ServletDSPContext(_ctx, request, response, null));
        } finally {
            Charsets.cleanup(old);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    private static final String ATTR_PAGE_CACHE = "com.potix.web.servlet.dsp.PageCache";

    private static final ResourceCache getCache(ServletContext ctx) {
        ResourceCache cache = (ResourceCache) ctx.getAttribute(ATTR_PAGE_CACHE);
        if (cache == null) {
            synchronized (InterpreterServlet.class) {
                cache = (ResourceCache) ctx.getAttribute(ATTR_PAGE_CACHE);
                if (cache == null) {
                    cache = new ResourceCache(new MyLoader(ctx), 29);
                    cache.setMaxSize(500).setLifetime(60 * 60 * 1000);
                    ctx.setAttribute(ATTR_PAGE_CACHE, cache);
                }
            }
        }
        return cache;
    }

    private static class MyLoader extends ResourceLoader {

        private final Locator _locator;

        private MyLoader(ServletContext ctx) {
            _locator = new ServletContextLocator(ctx);
        }

        protected Object parse(String path, File file) throws Exception {
            try {
                return parse0(new FileInputStream(file), Interpreter.getContentType(file.getName()));
            } catch (Exception ex) {
                if (log.debugable()) log.realCauseBriefly("Failed to parse " + file, ex); else log.error("Failed to parse " + file + "\nCause: " + Exceptions.getMessage(ex) + "\n" + Exceptions.getFirstStackTrace(ex));
                return null;
            }
        }

        protected Object parse(String path, URL url) throws Exception {
            try {
                return parse0(url.openStream(), Interpreter.getContentType(url.getPath()));
            } catch (Exception ex) {
                if (log.debugable()) log.realCauseBriefly("Failed to parse " + url, ex); else log.error("Failed to parse " + url + "\nCause: " + Exceptions.getMessage(ex));
                return null;
            }
        }

        private Object parse0(InputStream is, String ctype) throws Exception {
            if (is == null) return null;
            final String content = Files.readAll(new InputStreamReader(is, "UTF-8")).toString();
            return new Interpreter().parse(content, ctype, null, _locator);
        }
    }
}
