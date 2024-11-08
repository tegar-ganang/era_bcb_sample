package net.sasuke.firstapp.filters;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class FirstFilter implements Filter {

    @Override
    public void destroy() {
        System.out.println("Destroying the filter..." + this.getClass().getName());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        System.out.println("Filtering request in..." + this.getClass().getName());
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (httpRequest.getHeader("Accept-Encoding").indexOf("gzip") != -1) {
            HttpServletResponse wrappedResponse = new MyWrappedResponse(httpResponse);
            chain.doFilter(request, wrappedResponse);
            wrappedResponse.setHeader("Content-Encoding", "gzip");
            if (((MyWrappedResponse) wrappedResponse).gzipServletOs != null) ((MyWrappedResponse) wrappedResponse).gzipServletOs.finish();
        } else {
            System.out.println("gzip not supported, no encoding for you boya!");
            chain.doFilter(request, response);
        }
        System.out.println("Filtering response in..." + this.getClass().getName());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("Initting the filter..." + this.getClass().getName());
    }
}

class MyWrappedResponse extends HttpServletResponseWrapper {

    Object streamUsed;

    GzipServletOutputStream gzipServletOs;

    PrintWriter pw;

    ServletResponse response;

    public MyWrappedResponse(HttpServletResponse response) throws IOException {
        super(response);
        this.response = response;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (streamUsed != null && streamUsed == pw) {
            throw new IllegalStateException("Printwriter already obtained");
        }
        if (gzipServletOs == null) {
            gzipServletOs = new GzipServletOutputStream(response.getOutputStream());
            streamUsed = gzipServletOs;
        }
        return gzipServletOs;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (streamUsed != null && streamUsed == gzipServletOs) {
            throw new IllegalStateException("Outputstream already obtained");
        }
        if (pw == null) {
            gzipServletOs = new GzipServletOutputStream(response.getOutputStream());
            OutputStreamWriter osw = new OutputStreamWriter(gzipServletOs, response.getCharacterEncoding());
            pw = new PrintWriter(osw);
            streamUsed = pw;
        }
        return pw;
    }

    @Override
    public void setContentLength(int len) {
    }

    public GzipServletOutputStream getGzipServletOutputStream() {
        return gzipServletOs;
    }
}

class GzipServletOutputStream extends ServletOutputStream {

    GZIPOutputStream gzipOstream;

    public GzipServletOutputStream(ServletOutputStream sos) throws IOException {
        gzipOstream = new GZIPOutputStream(sos);
    }

    @Override
    public void write(int b) throws IOException {
        gzipOstream.write(b);
    }

    public void finish() throws IOException {
        gzipOstream.finish();
    }
}
