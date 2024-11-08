package com.tirsen.angkor.test.unit.mock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.w3c.tidy.Tidy;
import org.jdom.input.DOMBuilder;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * TODO document MockServletResponse
 *
 * <!-- $Id: MockServletResponse.java,v 1.5 2002/10/13 19:59:23 tirsen Exp $ -->
 *
 * @author $Author: tirsen $
 * @version $Revision: 1.5 $
 */
public class MockServletResponse implements HttpServletResponse {

    private static Log logger = LogFactory.getLog(MockServletResponse.class);

    private ByteArrayOutputStream output = new ByteArrayOutputStream();

    private PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(output));

    public String getCharacterEncoding() {
        throw new RuntimeException("MockServletResponse.getCharacterEncoding is not implemented yet.");
    }

    public ServletOutputStream getOutputStream() throws IOException {
        throw new RuntimeException("MockServletResponse.getOutputStream is not implemented yet.");
    }

    public PrintWriter getWriter() throws IOException {
        return printWriter;
    }

    public void setContentLength(int i) {
        throw new RuntimeException("MockServletResponse.setContentLength is not implemented yet.");
    }

    public void setContentType(String s) {
        throw new RuntimeException("MockServletResponse.setContentType is not implemented yet.");
    }

    public void setBufferSize(int i) {
        throw new RuntimeException("MockServletResponse.setBufferSize is not implemented yet.");
    }

    public int getBufferSize() {
        throw new RuntimeException("MockServletResponse.getBufferSize is not implemented yet.");
    }

    public void flushBuffer() throws IOException {
        throw new RuntimeException("MockServletResponse.flushBuffer is not implemented yet.");
    }

    public void resetBuffer() {
        throw new RuntimeException("MockServletResponse.resetBuffer is not implemented yet.");
    }

    public boolean isCommitted() {
        return false;
    }

    public void reset() {
        throw new RuntimeException("MockServletResponse.reset is not implemented yet.");
    }

    public void setLocale(Locale locale) {
        throw new RuntimeException("MockServletResponse.setLocale is not implemented yet.");
    }

    public Locale getLocale() {
        throw new RuntimeException("MockServletResponse.getLocale is not implemented yet.");
    }

    public void setStatus(int i, String s) {
        throw new RuntimeException("MockServletResponse.setStatus is not implemented yet.");
    }

    public void setStatus(int i) {
        throw new RuntimeException("MockServletResponse.setStatus is not implemented yet.");
    }

    public void addIntHeader(String s, int i) {
        throw new RuntimeException("MockServletResponse.addIntHeader is not implemented yet.");
    }

    public void setIntHeader(String s, int i) {
        throw new RuntimeException("MockServletResponse.setIntHeader is not implemented yet.");
    }

    public void addHeader(String s, String s1) {
        throw new RuntimeException("MockServletResponse.addHeader is not implemented yet.");
    }

    public void setHeader(String s, String s1) {
        throw new RuntimeException("MockServletResponse.setHeader is not implemented yet.");
    }

    public void addDateHeader(String s, long l) {
        throw new RuntimeException("MockServletResponse.addDateHeader is not implemented yet.");
    }

    public void setDateHeader(String s, long l) {
        throw new RuntimeException("MockServletResponse.setDateHeader is not implemented yet.");
    }

    public void sendRedirect(String s) throws IOException {
        throw new RuntimeException("MockServletResponse.sendRedirect is not implemented yet.");
    }

    public void sendError(int i) throws IOException {
        throw new RuntimeException("MockServletResponse.sendError is not implemented yet.");
    }

    public void sendError(int i, String s) throws IOException {
        throw new RuntimeException("MockServletResponse.sendError is not implemented yet.");
    }

    public String encodeRedirectUrl(String s) {
        throw new RuntimeException("MockServletResponse.encodeRedirectUrl is not implemented yet.");
    }

    public String encodeUrl(String s) {
        throw new RuntimeException("MockServletResponse.encodeUrl is not implemented yet.");
    }

    public String encodeRedirectURL(String s) {
        throw new RuntimeException("MockServletResponse.encodeRedirectURL is not implemented yet.");
    }

    public String encodeURL(String s) {
        throw new RuntimeException("MockServletResponse.encodeURL is not implemented yet.");
    }

    public boolean containsHeader(String s) {
        throw new RuntimeException("MockServletResponse.containsHeader is not implemented yet.");
    }

    public void addCookie(Cookie cookie) {
        throw new RuntimeException("MockServletResponse.addCookie is not implemented yet.");
    }

    public Document getResultingDocument() {
        Tidy tidy = new Tidy();
        org.w3c.dom.Document doc = tidy.parseDOM(getRenderedInputStream(), System.out);
        DOMBuilder domBuilder = new DOMBuilder();
        return new Document(domBuilder.build(doc.getDocumentElement()));
    }

    public void dumpResult() {
        try {
            InputStream renderedInputStream = getRenderedInputStream();
            int read;
            while ((read = renderedInputStream.read()) != -1) {
                System.out.write(read);
            }
        } catch (IOException ignore) {
        }
    }

    private InputStream getRenderedInputStream() {
        printWriter.flush();
        byte[] bytes = output.toByteArray();
        return new ByteArrayInputStream(bytes);
    }

    void resetStreams() {
        output = new ByteArrayOutputStream();
        printWriter = new PrintWriter(output);
    }
}
