package edu.upenn.cis.cis555.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FileUploadServlet extends HttpServlet {

    private static final long serialVersionUID = 5834109600766204133L;

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        BufferedReader in = request.getReader();
        PrintWriter out = response.getWriter();
        out.println("<!doctype html><head><title>File Upload GET</title></head><body>");
        out.println("<p>Content Length = '" + request.getContentLength() + "'</p>");
        out.println("<p>Content Type = '" + request.getContentType() + "'</p>");
        out.println("<p>");
        for (int i = 0; i < request.getContentLength(); i++) {
            out.write(in.read());
        }
        out.println("</p>");
        out.println("</body></html>");
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        out.println("<!doctype html><head><title>File Upload GET</title></head><body>");
        out.println("<p>/fileupload GET called</p>");
        out.println("</body></html>");
    }
}
