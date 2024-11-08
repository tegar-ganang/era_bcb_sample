package com.grooveapp.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.grooveapp.GrooveApp;
import com.grooveapp.controller.Controller;
import com.grooveapp.controller.ControllerMappingPath;
import com.grooveapp.controller.GrooveInvokeException;
import com.grooveapp.controller.Request;
import com.grooveapp.controller.Result;

public class GrooveFilter implements Filter {

    protected Log log = LogFactory.getLog(getClass());

    protected GrooveApp app = null;

    protected ApplicationContext context = null;

    public void init(FilterConfig config) throws ServletException {
        context = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
        app = (GrooveApp) context.getBean("grooveapp");
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) req;
        HttpServletResponse httpResponse = (HttpServletResponse) resp;
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        if (!ignorePath(path)) {
            log.debug("URI       : " + httpRequest.getRequestURI());
            log.debug("Path      : " + path);
            ControllerMappingPath cmp = app.findController(path);
            if (cmp != null) {
                Result result = null;
                try {
                    Request request = new Request(httpRequest, httpResponse, cmp, path, context);
                    result = cmp.getMapping().invokeAction(request, cmp.getParams());
                    if (result != null) {
                        httpRequest.setAttribute(Controller.CONTROLLER_CONTEXT, cmp.getMapping().getController());
                        processResult(result, httpRequest, httpResponse, cmp.getMapping().getController());
                        return;
                    } else {
                        log.debug("Null result returned, forwarding to remainder of chain");
                    }
                } catch (GrooveInvokeException e) {
                    log.error(e);
                    e.printStackTrace();
                }
            } else {
                log.debug("ControllerMapping was not found");
            }
        } else {
            log.debug("Ignoring path: " + path);
        }
        chain.doFilter(req, resp);
    }

    protected boolean ignorePath(String path) {
        if (path.toLowerCase().endsWith(".png")) {
            return true;
        }
        if (path.toLowerCase().endsWith(".jsp")) {
            return true;
        }
        if (path.toLowerCase().endsWith(".jpg")) {
            return true;
        }
        if (path.toLowerCase().endsWith(".html")) {
            return true;
        }
        if (path.toLowerCase().endsWith(".css")) {
            return true;
        }
        if (path.toLowerCase().endsWith(".js")) {
            return true;
        }
        if (path.toLowerCase().endsWith(".ico")) {
            return true;
        }
        if (path.toLowerCase().endsWith(".gif")) {
            return true;
        }
        return false;
    }

    private void processResult(Result result, HttpServletRequest request, HttpServletResponse response, Controller controller) throws IOException, ServletException {
        if (result.isStop()) {
            return;
        } else if (result.isRedirect()) {
            log.debug("redirecting to : " + result.getValue());
            if (result.getValue().startsWith("/")) {
                response.sendRedirect(request.getContextPath() + result.getValue());
            } else {
                response.sendRedirect(result.getValue());
            }
        } else {
            if (result.getType() == Result.Type.JSP) {
                if (controller.getRequestParameters() != null) {
                    for (String key : controller.getRequestParameters().keySet()) {
                        request.setAttribute(key, controller.getRequestParameters().get(key));
                    }
                }
                if (app.getHeader() != null && controller.getIncludeGlobalHeaderFooter() && result.isDecorate()) {
                    jspRequestDispatch(app.getHeader(), request, response, false);
                }
                if (controller.getHeader() != null && result.isDecorate() && !result.isError()) {
                    jspRequestDispatch(controller.getHeader(), request, response, false);
                }
                jspRequestDispatch(result.getValue(), request, response, false);
                if (controller.getFooter() != null && result.isDecorate() && !result.isError()) {
                    jspRequestDispatch(controller.getFooter(), request, response, false);
                }
                if (app.getFooter() != null && controller.getIncludeGlobalHeaderFooter() && result.isDecorate()) {
                    jspRequestDispatch(app.getFooter(), request, response, false);
                }
            } else {
                if (result.getType() == Result.Type.HTML) {
                    response.setContentType("text/html");
                    response.getWriter().write(result.getValue());
                    response.getWriter().close();
                } else if (result.getType() == Result.Type.XML) {
                    response.setContentType("text/xml");
                    response.setCharacterEncoding(request.getCharacterEncoding());
                    if (result.getValue().indexOf("<?xml") == -1) {
                        response.getWriter().write("<?xml version=\"1.0\" encoding=\"" + request.getCharacterEncoding() + "\"?>");
                    }
                    response.getWriter().write(result.getValue());
                    response.getWriter().close();
                } else if (result.getType() == Result.Type.RESOURCE) {
                    try {
                        InputStream in = new BufferedInputStream(request.getSession().getServletContext().getResourceAsStream(result.getValue()));
                        OutputStream out = new BufferedOutputStream(response.getOutputStream());
                        if (in != null && out != null) {
                            int read = 0;
                            byte[] buffer = new byte[4096];
                            while (read > -1) {
                                read = in.read(buffer);
                                if (read > -1) {
                                    out.write(buffer, 0, read);
                                }
                            }
                            in.close();
                            out.close();
                        }
                    } catch (FileNotFoundException e) {
                        log.warn(e);
                    } catch (IOException e) {
                        log.error(e);
                    }
                } else {
                    response.setContentType("text/plain");
                    response.getWriter().write(result.getValue());
                    response.getWriter().close();
                }
            }
        }
    }

    private void jspRequestDispatch(String path, HttpServletRequest request, HttpServletResponse response, boolean commit) throws ServletException, IOException {
        log.debug("including : " + path);
        RequestDispatcher rd = request.getRequestDispatcher(path);
        if (!commit || response.isCommitted()) {
            rd.include(request, response);
        } else {
            rd.forward(request, response);
        }
    }
}
