package com.doculibre.intelligid.webservices;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

@SuppressWarnings("serial")
public abstract class BaseServiceWeb extends HttpServlet {

    protected void error(String message, HttpServletResponse response) throws ServletException, IOException {
        Element root = new Element("error").setText(message);
        Document document = new Document(root);
        writeToResponse(document, response);
    }

    protected void writeToResponse(Document document, HttpServletResponse response) throws IOException {
        Format format = Format.getPrettyFormat();
        XMLOutputter outputter = new XMLOutputter(format);
        outputter.output(document, response.getOutputStream());
    }

    protected void writeToResponse(InputStream stream, HttpServletResponse response) throws IOException {
        OutputStream output = response.getOutputStream();
        try {
            IOUtils.copy(stream, output);
        } finally {
            try {
                stream.close();
            } finally {
                output.close();
            }
        }
    }

    @Override
    protected final void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doService(request, response);
    }

    protected abstract void doService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
}
