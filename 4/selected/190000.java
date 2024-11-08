package com.csam.jar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Nathan Crause <ncrause at clarkesolomou.com>
 */
public class Pack200Aware extends HttpServlet {

    private static final String JAR_MIME_TYPE = "application/x-java-archive";

    public static final String ACCEPT_ENCODING = "accept-encoding";

    public static final String CONTENT_TYPE = "content-type";

    public static final String CONTENT_ENCODING = "content-encoding";

    public static final String LAST_MODIFIED = "Last-Modified";

    public static final String CONTENT_LENGTH = "Content-Length";

    public static final String GZIP_ENCODING = "gzip";

    public static final String PACK200_GZIP_ENCODING = "pack200-gzip";

    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response, boolean sendBody) throws ServletException, IOException {
        OutputStream out = response.getOutputStream();
        ServletContext context = getServletContext();
        String acceptEncoding = request.getHeader(ACCEPT_ENCODING);
        String requestURI = request.getRequestURI();
        String pathInfo = request.getPathInfo();
        String pathTranslated = request.getPathTranslated();
        Pattern resourcePattern = Pattern.compile(request.getContextPath() + "(.+)");
        Matcher resourceMatcher = resourcePattern.matcher(requestURI);
        String resourceName = resourceMatcher.find() ? resourceMatcher.group(1) : null;
        String realPath = context.getRealPath(requestURI);
        File jarFile = new File(context.getRealPath(resourceName));
        File pack200File = new File(context.getRealPath(resourceName + ".pack"));
        File pack200GzipFile = new File(context.getRealPath(resourceName + ".pack.gz"));
        File gzipFile = new File(context.getRealPath(resourceName + ".gz"));
        context.log("----- BEGIN HEADERS -----");
        for (Enumeration<String> headers = request.getHeaderNames(); headers.hasMoreElements(); ) {
            String header = headers.nextElement();
            context.log(header + ": " + request.getHeader(header));
        }
        context.log("----- BEGIN RELEVANT HEADERS -----");
        context.log("Accept-Encoding: " + acceptEncoding);
        context.log("URI: " + requestURI);
        context.log("Path Info: " + pathInfo);
        context.log("Path Translated: " + pathTranslated);
        context.log("Resource Name: " + resourceName);
        context.log("Real Path: " + realPath);
        context.log("----- FILE INFO -----");
        context.log("JAR: " + jarFile + " (exists? " + jarFile.exists() + ")");
        context.log("Pack: " + pack200File + " (exists? " + pack200File.exists() + ")");
        context.log("Pack-GZip: " + pack200GzipFile + " (exists? " + pack200GzipFile.exists() + ")");
        context.log("GZip: " + gzipFile + " (exists? " + gzipFile.exists() + ")");
        response.setHeader(CONTENT_TYPE, JAR_MIME_TYPE);
        if (acceptEncoding != null && acceptEncoding.contains(PACK200_GZIP_ENCODING) && pack200GzipFile.exists() && pack200GzipFile.canRead()) {
            context.log("Serving up " + pack200GzipFile.getName());
            prepFile(response, pack200GzipFile, PACK200_GZIP_ENCODING);
            if (sendBody) sendFile(out, pack200GzipFile);
        } else if (acceptEncoding != null && acceptEncoding.contains(GZIP_ENCODING) && gzipFile.exists() && gzipFile.canRead()) {
            context.log("Serving up " + gzipFile.getName());
            prepFile(response, gzipFile, GZIP_ENCODING);
            if (sendBody) sendFile(out, gzipFile);
        } else if (jarFile.exists() && jarFile.canRead()) {
            context.log("Serving up " + jarFile);
            prepFile(response, jarFile, null);
            if (sendBody) sendFile(out, jarFile);
        } else {
            context.log("Not such JAR found: '" + resourceName + "' or insufficient permissions");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        context.log("----- END -----");
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp, false);
    }

    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response, true);
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response, true);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }

    private void sendFile(OutputStream out, File file) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            byte[] buffer = new byte[1024];
            int bytes;
            while ((bytes = in.read(buffer)) != -1) out.write(buffer, 0, bytes);
        } finally {
            in.close();
        }
    }

    private void prepFile(HttpServletResponse response, File file, String contentEncoding) {
        response.setHeader(CONTENT_ENCODING, contentEncoding);
        response.setDateHeader(LAST_MODIFIED, file.lastModified());
        response.setDateHeader(CONTENT_LENGTH, file.length());
    }
}
