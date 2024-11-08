package com.ingenta.clownbike.text;

import com.ingenta.clownbike.*;
import java.io.*;
import java.util.*;
import org.apache.xerces.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * Rewites HTML so that is uses a client stylesheet for formatting
 * the content. A few class attributes that are recognized to help supliment
 * the client class styles: These are "push", "pop", and "sub".
 *
 * Few tag attributes are copied during rewriting. IMG's src and align,
 * and A's href, name, target are copied.
 *
 * Minimization is done only for IMG, HR, and BR tags.
 *
 * NOTE the design of this class is not general purpose. If you need to add
 * substantially to it then it should be rewritten.
 */
public class HtmlFormatter extends DefaultHandler {

    private boolean _propogateExceptions = true;

    private XMLReader _reader;

    private Writer _writer;

    private String _cssClass;

    private String _urlContext;

    public boolean getPropogateExceptions() {
        return _propogateExceptions;
    }

    public void setPropogateExceptions(boolean propogateExceptions) {
        _propogateExceptions = propogateExceptions;
    }

    public String format(String input, String cssClass, String urlContext) throws SAXException, IOException {
        StringWriter output = new StringWriter(input.length() + input.length() / 3);
        format(new StringReader(input), output, cssClass, urlContext);
        return output.toString();
    }

    public void format(String input, Writer output, String cssClass, String urlContext) throws SAXException, IOException {
        format(new StringReader(input), output, cssClass, urlContext);
    }

    public void format(Reader reader, Writer writer, String cssClass, String urlContext) throws SAXException, IOException {
        try {
            _cssClass = cssClass;
            _urlContext = urlContext;
            _writer = writer;
            _reader = new org.apache.xerces.parsers.SAXParser();
            _reader.setContentHandler(this);
            _reader.setErrorHandler(this);
            _reader.parse(new InputSource(reader));
        } finally {
            _reader = null;
            _writer = null;
            _cssClass = null;
        }
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {
        try {
            String name = localName.toLowerCase();
            if (name.equals("img")) {
                _writer.write("<img");
                writeClassAttribute(attributes.getValue("align"));
                writeOptionalAttribute("src", rewriteURL(_urlContext, attributes.getValue("src")));
                writeOptionalAttribute("align", attributes.getValue("align"));
                _writer.write(" />");
            } else if (name.equals("a")) {
                _writer.write("<a");
                writeClassAttribute(attributes.getValue("class"));
                writeOptionalAttribute("href", rewriteURL(_urlContext, attributes.getValue("href")));
                writeOptionalAttribute("name", attributes.getValue("name"));
                writeOptionalAttribute("target", attributes.getValue("target"));
                _writer.write(">");
            } else if (name.equals("hr")) {
                _writer.write("<hr");
                writeClassAttribute(attributes.getValue("class"));
                _writer.write("size=\"1\" noshade=\"1\" />");
            } else if (name.equals("br")) {
                _writer.write("<br");
                writeClassAttribute(attributes.getValue("class"));
                _writer.write(" />");
            } else if (name.equals("form")) {
                _writer.write("<form");
                writeClassAttribute(attributes.getValue("class"));
                writeOptionalAttribute("action", rewriteURL(_urlContext, attributes.getValue("action")));
                writeOptionalAttribute("method", attributes.getValue("method"));
                writeOptionalAttribute("name", attributes.getValue("name"));
                writeOptionalAttribute("target", attributes.getValue("target"));
                writeOptionalAttribute("enctype", attributes.getValue("enctype"));
                _writer.write(" />");
            } else {
                _writer.write("<");
                _writer.write(name);
                writeClassAttribute(attributes.getValue("class"));
                _writer.write(">");
            }
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        try {
            String name = localName.toLowerCase();
            if (!name.equals("img") && !name.equals("hr") && !name.equals("br")) {
                _writer.write("</");
                _writer.write(name);
                _writer.write(">");
            }
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public void characters(char[] content, int start, int length) throws SAXException {
        try {
            writeEscaped(content, start, length);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public void warning(SAXParseException e) throws SAXException {
        if (_propogateExceptions) {
            throw e;
        }
        try {
            writeError("Warning", e.getMessage(), e.getLineNumber(), e.getColumnNumber());
        } catch (IOException xe) {
            throw new SAXException(xe);
        }
    }

    public void error(SAXParseException e) throws SAXException {
        if (_propogateExceptions) {
            throw e;
        }
        try {
            writeError("Error", e.getMessage(), e.getLineNumber(), e.getColumnNumber());
        } catch (IOException xe) {
            throw new SAXException(xe);
        }
    }

    public void fatalError(SAXParseException e) throws SAXException {
        if (_propogateExceptions) {
            throw e;
        }
        try {
            writeError("Fatal Error", e.getMessage(), e.getLineNumber(), e.getColumnNumber());
        } catch (IOException xe) {
            throw new SAXException(xe);
        }
    }

    protected void writeError(String kind, String message, int line, int column) throws IOException {
        _writer.write("<div");
        writeClassAttribute("error");
        _writer.write(">");
        _writer.write(kind + ": " + message + " (" + line + ":" + column + ")");
        _writer.write("</div>");
    }

    protected void writeClassAttribute(String suplimentalCssClass) throws IOException {
        _writer.write(" class=\"");
        _writer.write(_cssClass);
        if (suplimentalCssClass != null && (suplimentalCssClass.equals("push") || suplimentalCssClass.equals("pop") || suplimentalCssClass.equals("sub") || suplimentalCssClass.equals("left") || suplimentalCssClass.equals("right") || suplimentalCssClass.equals("error"))) {
            _writer.write("-");
            _writer.write(suplimentalCssClass);
        }
        _writer.write("\"");
    }

    protected void writeOptionalAttribute(String name, String value) throws IOException {
        if (value != null) {
            _writer.write(" ");
            _writer.write(name);
            _writer.write("=\"");
            writeEscaped(value);
            _writer.write("\"");
        }
    }

    protected void writeEscaped(String string) throws IOException {
        char[] content = string.toCharArray();
        writeEscaped(content, 0, content.length);
    }

    protected void writeEscaped(char[] content, int start, int length) throws IOException {
        int s = start;
        for (int i = start; i < start + length; i++) {
            if (content[i] == '&') {
                _writer.write(content, s, i - s);
                s = i + 1;
                _writer.write("&amp;");
            } else if (content[i] == '<') {
                _writer.write(content, s, i - s);
                s = i + 1;
                _writer.write("&lt;");
            } else if (content[i] == '>') {
                _writer.write(content, s, i - s);
                s = i + 1;
                _writer.write("&gt;");
            } else if (content[i] == '"') {
                _writer.write(content, s, i - s);
                s = i + 1;
                _writer.write("&quot;");
            }
        }
        _writer.write(content, s, (start + length) - s);
    }

    protected static String rewriteURL(String context, String url) throws IOException {
        if (url.startsWith("//")) {
            return context + url.substring(1);
        } else {
            return url;
        }
    }

    public static void main(String[] args) throws Exception {
        HtmlFormatter formatter = new HtmlFormatter();
        for (int i = 0; i < args.length; i++) {
            StringWriter sw = new StringWriter();
            formatter.format(new FileReader(args[i]), sw, "client", "/clownbike");
            System.out.println(sw);
        }
    }
}
