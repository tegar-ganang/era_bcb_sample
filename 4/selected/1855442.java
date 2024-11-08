package org.swemas.core.servlet;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import javax.servlet.*;
import javax.servlet.http.*;
import org.swemas.core.*;
import org.swemas.core.dispatcher.IDispatchingChannel;
import org.swemas.core.kernel.IKernel;

/**
 * @author Alexey Chernov
 * 
 */
public class SwServlet extends HttpServlet {

    public void init(ServletConfig sc) throws ServletException {
        ServletContext sctxt = sc.getServletContext();
        String appPath = sctxt.getRealPath(sctxt.getContextPath());
        String kname = sc.getInitParameter("KernelClassName");
        String xcname = sc.getInitParameter("DefaultXmlChannel");
        String conclevel = sc.getInitParameter("ConcurrencyLevel");
        int clevel;
        try {
            clevel = Integer.parseInt(conclevel);
        } catch (NumberFormatException e) {
            clevel = 16;
        }
        try {
            Class<?> kclass = Class.forName(kname);
            Class<?>[] partypes = { String.class, String.class, int.class };
            Object[] params = { appPath, xcname, clevel };
            _kernel = (IKernel) kclass.getConstructor(partypes).newInstance(params);
        } catch (ClassNotFoundException e) {
            throw new ServletException(e);
        } catch (IllegalArgumentException e) {
            throw new ServletException(e);
        } catch (SecurityException e) {
            throw new ServletException(e);
        } catch (InstantiationException e) {
            throw new ServletException(e);
        } catch (IllegalAccessException e) {
            throw new ServletException(e);
        } catch (InvocationTargetException e) {
            throw new ServletException(e);
        } catch (NoSuchMethodException e) {
            throw new ServletException(e);
        }
    }

    ;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html");
        response.setCharacterEncoding("utf8");
        try {
            IDispatchingChannel d = (IDispatchingChannel) _kernel.getChannel(IDispatchingChannel.class);
            d.dispatch(request, response);
        } catch (ModuleNotFoundException e) {
            throw new ServletException(e);
        }
    }

    private IKernel _kernel;

    private static final long serialVersionUID = 1L;
}
