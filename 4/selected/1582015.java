package org.ffck.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author srecinto
 *
 */
public class Servlet extends HttpServlet {

    private static final long serialVersionUID = 7260045528613530636L;

    private static final String modify = calcModify();

    private String customResourcePath;

    private static final String calcModify() {
        Date mod = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(mod);
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        customResourcePath = config.getInitParameter("customResourcePath");
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ClassLoader cl = this.getClass().getClassLoader();
        String uri = request.getRequestURI();
        String path = uri.substring(uri.indexOf(Utils.FCK_FACES_RESOURCE_PREFIX) + Utils.FCK_FACES_RESOURCE_PREFIX.length() + 1);
        if (getCustomResourcePath() != null) {
            this.getServletContext().getRequestDispatcher(getCustomResourcePath() + path).forward(request, response);
        } else {
            if (uri.endsWith(".jsf")) {
                response.setContentType("text/html;");
            } else {
                response.setHeader("Cache-Control", "public");
                response.setHeader("Last-Modified", modify);
            }
            if (uri.endsWith(".css")) {
                response.setContentType("text/css;");
            } else if (uri.endsWith(".js")) {
                response.setContentType("text/javascript;");
            } else if (uri.endsWith(".gif")) {
                response.setContentType("image/gif;");
            }
            InputStream is = cl.getResourceAsStream(path);
            if (is == null) return;
            OutputStream out = response.getOutputStream();
            byte[] buffer = new byte[2048];
            BufferedInputStream bis = new BufferedInputStream(is);
            int read = 0;
            read = bis.read(buffer);
            while (read != -1) {
                out.write(buffer, 0, read);
                read = bis.read(buffer);
            }
            bis.close();
            out.flush();
            out.close();
        }
    }

    public String getCustomResourcePath() {
        return customResourcePath;
    }

    public void setCustomResourcePath(String customResourcePath) {
        this.customResourcePath = customResourcePath;
    }
}
