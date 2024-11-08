package com.mebigfatguy.tomailer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;

/**
 * the basic servlet that handles requests to convert web page html
 * to email html
 */
public class ToMailer extends HttpServlet {

    private static final long serialVersionUID = 6120664829482395241L;

    /**
	 * a handler for get requests, that just forwards to a Post handler.
	 * This shouldn't be used, but is here for completeness.
	 * 
	 * @param request the http request
	 * @param response the http response
	 */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doPost(request, response);
    }

    /**
	 * the main servlet processing method for converting web page html to email html
	 * 
	 * @param request the http request
	 * @param response the http response
	 */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        InputStream is = null;
        InputStream page = null;
        OutputStream os = null;
        String rootUrl = null;
        try {
            boolean isMultipart = ServletFileUpload.isMultipartContent(request);
            if (!isMultipart) {
                request.setAttribute("error", "Form isn't a multipart form");
                RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/error.jsp");
                rd.forward(request, response);
            }
            ServletFileUpload upload = new ServletFileUpload();
            String webUrl = null;
            FileItemIterator iter = upload.getItemIterator(request);
            while (iter.hasNext()) {
                FileItemStream item = iter.next();
                String name = item.getFieldName();
                if (name.equals("webpage")) {
                    is = item.openStream();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    IOUtils.copy(is, baos);
                    page = new ByteArrayInputStream(baos.toByteArray());
                } else if (name.equals("weburl")) {
                    InputStream wpIs = null;
                    try {
                        webUrl = Streams.asString(item.openStream());
                        URL u = new URL(webUrl);
                        wpIs = new BufferedInputStream(u.openStream());
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        IOUtils.copy(wpIs, baos);
                        page = new ByteArrayInputStream(baos.toByteArray());
                    } finally {
                        IOUtils.closeQuietly(wpIs);
                    }
                } else if (name.equals("rooturl")) {
                    rootUrl = Streams.asString(item.openStream());
                }
            }
            if (page == null) {
                request.setAttribute("error", "Form doesn't have an html file");
                RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/error.jsp");
                rd.forward(request, response);
            }
            ToMailerDelegate delegate = new ToMailerDelegate(page, rootUrl);
            os = new BufferedOutputStream(response.getOutputStream());
            os.write(delegate.getMailer());
            os.flush();
        } catch (Exception e) {
            streamException(request, response, e);
        } finally {
            IOUtils.closeQuietly(page);
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }

    /**
	 * convert a caught exception to a page to be displayed
	 * 
	 * @param request the http request
	 * @param response the http response
	 * @param e the exception to display
	 * 
	 * @throws IOException
	 * @throws ServletException
	 */
    private void streamException(HttpServletRequest request, HttpServletResponse response, Exception e) throws IOException, ServletException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        request.setAttribute("error", sw.toString());
        RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/error.jsp");
        rd.forward(request, response);
    }
}
