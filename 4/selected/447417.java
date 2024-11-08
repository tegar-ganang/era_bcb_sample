package net.woodstock.rockapi.jsp.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import net.woodstock.rockapi.utils.FileUtils;
import net.woodstock.rockapi.utils.IOUtils;

public abstract class ResponseUtils {

    public static final String DOWNLOAD_CONTENT_TYPE = "application/force-download";

    public static final String HTML_CONTENT_TYPE = "text/html";

    public static final String TEXT_CONTENT_TYPE = "text/plain";

    public static final String XML_CONTENT_TYPE = "text/xml";

    private ResponseUtils() {
    }

    public static void redirect(PageContext context, String url) throws ServletException, IOException {
        RequestDispatcher d = context.getServletContext().getRequestDispatcher(url);
        d.forward(context.getRequest(), context.getResponse());
    }

    public static void downloadFile(HttpServletResponse response, String file) throws IOException {
        response.setContentType(FileUtils.getContentType(file));
        response.setContentLength(FileUtils.getContentLength(file));
        response.setHeader("Content-type", ResponseUtils.DOWNLOAD_CONTENT_TYPE);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + FileUtils.getFileName(file) + "\"");
        response.setHeader("Content-Length", Integer.toString(FileUtils.getContentLength(file)));
        InputStream input = new FileInputStream(file);
        OutputStream output = response.getOutputStream();
        IOUtils.copy(input, output, true);
    }

    public static void copyFile(HttpServletResponse response, String file) throws IOException {
        response.setContentType(FileUtils.getContentType(file));
        response.setContentLength(FileUtils.getContentLength(file));
        response.setHeader("Content-Length", Integer.toString(FileUtils.getContentLength(file)));
        InputStream input = new FileInputStream(file);
        OutputStream output = response.getOutputStream();
        IOUtils.copy(input, output, true);
    }
}
